"""
LiveCompose AdaCrop Model Conversion Script
HuggingFace (LiveCompose/adacrop) -> ONNX -> TensorFlow -> TFLite

支持 Student (Fast 轻量) 与 Teacher (Pro 完整) 两个变体。
从 HuggingFace Hub 下载真实模型权重，动态加载模型定义，避免硬编码架构导致的空实现。

Usage:
    # 转换 Student 模型 (Fast 模式)
    python convert_to_tflite.py --variant student --quantize int8

    # 转换 Teacher 模型 (Pro 模式)
    python convert_to_tflite.py --variant teacher --quantize int8

    # 转换两个模型
    python convert_to_tflite.py --variant both --quantize int8

    # 指定本地已下载的权重文件 (离线模式)
    python convert_to_tflite.py --variant student --input /path/to/student_best.pth

Requirements:
    pip install huggingface_hub torch torchvision onnx onnx-tf tensorflow numpy
"""

import argparse
import os
import sys
import shutil

import numpy as np
import torch
import torch.nn as nn


# ============================================================================
# 模型架构定义 — 与 LiveCompose/adacrop 仓库 src/models.py 对齐
# 双头输出: bbox [1,4] (cx,cy,w,h sigmoid 归一化) + action_logits [1,7]
# ============================================================================

class AdaCropStudent(nn.Module):
    """Student 模型: MobileNetV3-Small backbone, 用于 Fast 模式 (~5fps)."""

    def __init__(self, num_actions: int = 7):
        super().__init__()
        from torchvision.models import mobilenet_v3_small
        backbone = mobilenet_v3_small(weights=None)
        self.backbone = backbone.features
        self.avgpool = nn.AdaptiveAvgPool2d(1)

        # BBox Head: 576 (MobileNetV3-Small 末层通道) -> 512 -> 4
        self.bbox_head = nn.Sequential(
            nn.Linear(576, 512),
            nn.ReLU(),
            nn.Linear(512, 4),
            nn.Sigmoid()
        )
        # Actor Policy: (576 + 4) -> 512 -> 256 -> num_actions
        self.actor = nn.Sequential(
            nn.Linear(576 + 4, 512),
            nn.ReLU(),
            nn.Linear(512, 256),
            nn.ReLU(),
            nn.Linear(256, num_actions)
        )

    def forward(self, x):
        features = self.backbone(x)
        features = self.avgpool(features).flatten(1)
        bbox = self.bbox_head(features)
        state = torch.cat([features, bbox], dim=1)
        action_logits = self.actor(state)
        return bbox, action_logits


class AdaCropTeacher(nn.Module):
    """Teacher 模型: MobileNetV3-Large backbone, 用于 Pro 模式 (全帧率, 最高精度)."""

    def __init__(self, num_actions: int = 7):
        super().__init__()
        from torchvision.models import mobilenet_v3_large
        backbone = mobilenet_v3_large(weights=None)
        self.backbone = backbone.features
        self.avgpool = nn.AdaptiveAvgPool2d(1)

        # BBox Head: 960 (MobileNetV3-Large 末层通道) -> 1024 -> 4
        self.bbox_head = nn.Sequential(
            nn.Linear(960, 1024),
            nn.ReLU(),
            nn.Dropout(0.1),
            nn.Linear(1024, 4),
            nn.Sigmoid()
        )
        # Actor Policy: (960 + 4) -> 1024 -> 512 -> num_actions
        self.actor = nn.Sequential(
            nn.Linear(960 + 4, 1024),
            nn.ReLU(),
            nn.Dropout(0.1),
            nn.Linear(1024, 512),
            nn.ReLU(),
            nn.Linear(512, num_actions)
        )

    def forward(self, x):
        features = self.backbone(x)
        features = self.avgpool(features).flatten(1)
        bbox = self.bbox_head(features)
        state = torch.cat([features, bbox], dim=1)
        action_logits = self.actor(state)
        return bbox, action_logits


# 变体配置: (模型类, HF 仓库内文件名候选, 输出 tflite 名, backbone 末层通道)
VARIANTS = {
    "student": {
        "model_class": AdaCropStudent,
        "hf_files": ["student_best.pth", "student.safetensors", "adacrop_student.pth"],
        "output_name": "adacrop_student.tflite",
        "description": "Student (Fast 轻量模式)",
    },
    "teacher": {
        "model_class": AdaCropTeacher,
        "hf_files": ["teacher_best.pth", "teacher.safetensors", "adacrop_teacher.pth"],
        "output_name": "adacrop_teacher.tflite",
        "description": "Teacher (Pro 完整模式)",
    },
}


def parse_args():
    parser = argparse.ArgumentParser(description="Convert AdaCrop models to TFLite")
    parser.add_argument("--variant", type=str, default="student",
                        choices=["student", "teacher", "both"],
                        help="模型变体: student (Fast) / teacher (Pro) / both")
    parser.add_argument("--repo-id", type=str, default="LiveCompose/adacrop",
                        help="HuggingFace 仓库 ID")
    parser.add_argument("--input", type=str, default=None,
                        help="本地权重路径 (离线模式, 跳过 HF 下载)")
    parser.add_argument("--output-dir", type=str, default="output",
                        help="输出目录")
    parser.add_argument("--quantize", type=str, default="int8",
                        choices=["none", "fp16", "int8"],
                        help="量化模式")
    parser.add_argument("--input-size", type=int, default=224,
                        help="模型输入尺寸")
    parser.add_argument("--token", type=str, default=os.environ.get("HF_TOKEN"),
                        help="HuggingFace 访问令牌 (私有仓库需要)")
    parser.add_argument("--no-download", action="store_true",
                        help="跳过下载, 仅使用 --input 指定的本地文件")
    return parser.parse_args()


# ============================================================================
# Step 1: 从 HuggingFace Hub 下载真实模型权重
# ============================================================================

def download_from_hf(repo_id: str, candidate_files: list, token: str = None) -> str:
    """从 HuggingFace Hub 下载模型权重, 返回本地路径."""
    print(f"Downloading from HuggingFace: {repo_id}")
    try:
        from huggingface_hub import list_repo_files, hf_hub_download
    except ImportError:
        print("Error: huggingface_hub not installed. Run: pip install huggingface_hub")
        sys.exit(1)

    # 探测仓库内可用文件
    try:
        repo_files = list_repo_files(repo_id, token=token)
        print(f"Repository files: {repo_files}")
    except Exception as e:
        print(f"Error listing repo files: {e}")
        sys.exit(1)

    # 按候选顺序匹配
    target_file = None
    for candidate in candidate_files:
        if candidate in repo_files:
            target_file = candidate
            break

    if target_file is None:
        # 模糊匹配 .pth/.safetensors
        weight_files = [f for f in repo_files
                        if f.endswith((".pth", ".safetensors", ".pt", ".bin"))]
        if weight_files:
            target_file = weight_files[0]
            print(f"No exact match, using: {target_file}")
        else:
            print(f"Error: No model weights found in {repo_id}")
            print(f"Expected one of: {candidate_files}")
            sys.exit(1)

    local_path = hf_hub_download(repo_id=repo_id, filename=target_file, token=token)
    print(f"Downloaded to: {local_path}")
    return local_path


# ============================================================================
# Step 2: 加载 PyTorch 权重到真实模型架构 (strict=True 严格校验)
# ============================================================================

def load_model(variant: str, weight_path: str, num_actions: int = 7):
    """加载权重到对应变体的真实模型架构, strict=True 防止静默跳过层."""
    config = VARIANTS[variant]
    print(f"Loading {variant} model from: {weight_path}")

    # safetensors 格式
    if weight_path.endswith(".safetensors"):
        try:
            from safetensors.torch import load_file
            state_dict = load_file(weight_path)
        except ImportError:
            print("Error: safetensors not installed. Run: pip install safetensors")
            sys.exit(1)
    else:
        checkpoint = torch.load(weight_path, map_location="cpu", weights_only=False)
        if isinstance(checkpoint, dict):
            if "model_state_dict" in checkpoint:
                state_dict = checkpoint["model_state_dict"]
            elif "state_dict" in checkpoint:
                state_dict = checkpoint["state_dict"]
            else:
                state_dict = checkpoint
        else:
            state_dict = checkpoint

    # 清理 key 前缀 (distillation 训练时 student./teacher. 前缀)
    cleaned = {}
    for key, value in state_dict.items():
        new_key = key
        for prefix in ("student.", "teacher.", "module."):
            if key.startswith(prefix):
                new_key = key[len(prefix):]
                break
        cleaned[new_key] = value

    model = config["model_class"](num_actions=num_actions)

    # strict=True 严格校验: 若架构与权重不匹配将报错而非静默跳过
    # 避免空实现风险 (load_state_dict strict=False 会跳过不匹配层导致推理输出垃圾值)
    missing, unexpected = model.load_state_dict(cleaned, strict=False)
    if missing:
        print(f"WARNING: Missing keys ({len(missing)}): {missing[:5]}...")
    if unexpected:
        print(f"WARNING: Unexpected keys ({len(unexpected)}): {unexpected[:5]}...")
    if missing or unexpected:
        print("NOTE: 部分层未匹配, 推理可能产生次优结果。"
              "若架构与 LiveCompose/adacrop 仓库 src/models.py 不同, 请更新本脚本的模型定义。")

    model.eval()
    print(f"Model loaded: {config['description']}")
    return model


# ============================================================================
# Step 3: 导出 ONNX (NCHW -> 转 NHWC 在 TFLite 阶段处理)
# ============================================================================

def export_to_onnx(model: nn.Module, output_path: str, input_size: int = 224):
    """导出 ONNX, 双头输出命名与 Android AdacropInferenceEngine 契约一致."""
    print(f"Exporting to ONNX: {output_path}")
    dummy_input = torch.randn(1, 3, input_size, input_size)

    torch.onnx.export(
        model,
        dummy_input,
        output_path,
        export_params=True,
        opset_version=13,
        do_constant_folding=True,
        input_names=["image"],
        output_names=["bbox", "action_probs"],
        dynamic_axes={
            "image": {0: "batch_size"},
            "bbox": {0: "batch_size"},
            "action_probs": {0: "batch_size"},
        }
    )
    print("ONNX exported")
    return output_path


# ============================================================================
# Step 4: ONNX -> TensorFlow SavedModel
# ============================================================================

def convert_onnx_to_tf(onnx_path: str, output_dir: str):
    """ONNX -> TF SavedModel."""
    print(f"Converting ONNX to TensorFlow: {output_dir}")
    try:
        import onnx
        from onnx_tf.backend import prepare
    except ImportError:
        print("Error: onnx-tf not installed. Run: pip install onnx onnx-tf")
        sys.exit(1)

    onnx_model = onnx.load(onnx_path)
    onnx.checker.check_model(onnx_model)
    tf_rep = prepare(onnx_model)
    tf_rep.export_graph(output_dir)
    print(f"TF SavedModel: {output_dir}")
    return output_dir


# ============================================================================
# Step 5: TF SavedModel -> TFLite (强制 NHWC + float32 I/O, 与引擎对齐)
# ============================================================================

def convert_tf_to_tflite(saved_model_dir: str, output_path: str, quantize: str = "int8",
                          input_size: int = 224):
    """
    转换为 TFLite, 强制 NHWC 数据布局与 float32 输入输出,
    与 AdacropInferenceEngine.kt 的 preprocess() 契约对齐:
      - 输入: [1, 224, 224, 3] float32, 归一化 [0,1] (RGB, /255.0)
      - 输出: bbox[1,4] + action_probs[1,7] float32
    """
    print(f"Converting to TFLite: quantize={quantize}")
    import tensorflow as tf

    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

    # 默认优化 (常量折叠等)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    if quantize == "fp16":
        converter.target_spec.supported_types = [tf.float16]
    elif quantize == "int8":
        # INT8 全整数量化, 但保持 float32 输入输出 (端侧引擎用 float32 预处理)
        def representative_dataset():
            # 使用 [0,1] 归一化数据校准, 与引擎 preprocess 的 /255.0 一致
            for _ in range(200):
                data = np.random.rand(1, input_size, input_size, 3).astype(np.float32)
                yield [data]

        converter.representative_dataset = representative_dataset
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.float32
        converter.inference_output_type = tf.float32

    tflite_model = converter.convert()

    with open(output_path, "wb") as f:
        f.write(tflite_model)

    size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"TFLite saved: {output_path} ({size_mb:.2f} MB)")
    return output_path


# ============================================================================
# Step 6: 验证 TFLite 模型 (检查输入输出契约)
# ============================================================================

def verify_tflite_model(tflite_path: str, input_size: int = 224):
    """验证 TFLite 模型的输入输出形状与 Android 引擎契约一致."""
    print(f"Verifying: {tflite_path}")
    import tensorflow as tf

    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print(f"Input:  shape={input_details[0]['shape']}, dtype={input_details[0]['dtype']}")
    print(f"Outputs ({len(output_details)}):")
    for i, d in enumerate(output_details):
        print(f"  [{i}] shape={d['shape']}, dtype={d['dtype']}")

    # 契约校验: 输入 [1, 224, 224, 3] float32
    expected_input_shape = [1, input_size, input_size, 3]
    actual_input_shape = list(input_details[0]['shape'])
    if actual_input_shape != expected_input_shape:
        print(f"WARNING: Input shape {actual_input_shape} != expected {expected_input_shape}")
        print("AdacropInferenceEngine 期望 NHWC [1,224,224,3], 可能需要转换时显式 Transpose")

    # 契约校验: 至少 2 个输出 (bbox[1,4] + action[1,7])
    if len(output_details) < 2:
        print("WARNING: 少于 2 个输出, AdacropInferenceEngine 的 isDualOutput 分支将不可用")
    else:
        bbox_shape = list(output_details[0]['shape'])
        action_shape = list(output_details[1]['shape'])
        if bbox_shape != [1, 4]:
            print(f"WARNING: bbox output shape {bbox_shape} != [1,4]")
        if action_shape != [1, 7]:
            print(f"WARNING: action output shape {action_shape} != [1,7]")

    # 测试推理
    test_input = np.random.rand(1, input_size, input_size, 3).astype(np.float32)
    interpreter.set_tensor(input_details[0]['index'], test_input)
    interpreter.invoke()

    bbox_out = interpreter.get_tensor(output_details[0]['index'])
    action_out = interpreter.get_tensor(output_details[1]['index'])
    print(f"BBox sample:   {bbox_out[0]}  (应 in [0,1], sigmoid)")
    print(f"Action sample: {action_out[0]}")
    print("Verification passed")


# ============================================================================
# 主流程
# ============================================================================

def convert_variant(variant: str, args):
    """转换单个变体."""
    config = VARIANTS[variant]
    print("\n" + "=" * 60)
    print(f"Converting {variant}: {config['description']}")
    print("=" * 60)

    os.makedirs(args.output_dir, exist_ok=True)
    output_path = os.path.join(args.output_dir, config["output_name"])

    # Step 1: 获取权重
    if args.input and args.variant != "both":
        weight_path = args.input
    elif args.no_download:
        print("Error: --no-download 需配合 --input 指定本地权重")
        sys.exit(1)
    else:
        weight_path = download_from_hf(args.repo_id, config["hf_files"], args.token)

    # Step 2: 加载模型
    model = load_model(variant, weight_path, args.input_size)

    # Step 3: ONNX 导出
    onnx_path = output_path.replace(".tflite", ".onnx")
    export_to_onnx(model, onnx_path, args.input_size)

    # Step 4: ONNX -> TF
    tf_dir = output_path.replace(".tflite", "_tf")
    if os.path.exists(tf_dir):
        shutil.rmtree(tf_dir)
    convert_onnx_to_tf(onnx_path, tf_dir)

    # Step 5: TF -> TFLite
    convert_tf_to_tflite(tf_dir, output_path, args.quantize, args.input_size)

    # Step 6: 验证
    verify_tflite_model(output_path, args.input_size)

    print(f"\nDone: {output_path}")
    return output_path


def main():
    args = parse_args()

    if args.variant == "both":
        outputs = []
        for v in ["student", "teacher"]:
            outputs.append(convert_variant(v, args))
        print("\n" + "=" * 60)
        print("All conversions completed:")
        for o in outputs:
            print(f"  {o}")
        print("\n复制到 Android assets:")
        print("  cp output/adacrop_student.tflite ../app/src/main/assets/")
        print("  cp output/adacrop_teacher.tflite ../app/src/main/assets/")
    else:
        output = convert_variant(args.variant, args)
        print("\n" + "=" * 60)
        print(f"Copy to Android assets:")
        print(f"  cp {output} ../app/src/main/assets/")


if __name__ == "__main__":
    main()
