"""
LiveCompose AdaCrop Model Conversion Script
PyTorch (MobileNetV3 Student) -> ONNX -> TensorFlow -> TFLite

Usage:
    python convert_to_tflite.py --input student_best.pth --output adacrop_student.tflite

Requirements:
    pip install torch torchvision onnx onnx-tf tensorflow
"""

import argparse
import os
import sys

import numpy as np
import torch
import torch.nn as nn


def parse_args():
    parser = argparse.ArgumentParser(description="Convert AdaCrop Student model to TFLite")
    parser.add_argument("--input", type=str, required=True, help="Path to PyTorch .pth checkpoint")
    parser.add_argument("--output", type=str, default="adacrop_student.tflite", help="Output TFLite path")
    parser.add_argument("--quantize", type=str, default="int8", choices=["none", "fp16", "int8"],
                        help="Quantization mode")
    parser.add_argument("--input-size", type=int, default=224, help="Model input size")
    return parser.parse_args()


def load_pytorch_model(checkpoint_path: str):
    """Load PyTorch student model from checkpoint."""
    print(f"Loading PyTorch model from: {checkpoint_path}")
    checkpoint = torch.load(checkpoint_path, map_location="cpu")

    # The checkpoint may contain the full state_dict or just model weights
    if "model_state_dict" in checkpoint:
        state_dict = checkpoint["model_state_dict"]
    elif "state_dict" in checkpoint:
        state_dict = checkpoint["state_dict"]
    else:
        state_dict = checkpoint

    # Extract student model weights (remove 'student.' prefix if present)
    student_state_dict = {}
    for key, value in state_dict.items():
        if key.startswith("student."):
            student_state_dict[key[8:]] = value
        else:
            student_state_dict[key] = value

    print(f"Loaded {len(student_state_dict)} parameters")
    return student_state_dict


def export_to_onnx(model_state_dict, output_path: str, input_size: int = 224):
    """Export PyTorch model to ONNX format."""
    print(f"Exporting to ONNX: {output_path}")

    # Create a dummy model class for export
    # This is a simplified MobileNetV3-based architecture matching the student model
    class StudentModel(nn.Module):
        def __init__(self):
            super().__init__()
            from torchvision.models import mobilenet_v3_small
            backbone = mobilenet_v3_small(pretrained=False)
            self.backbone = backbone.features
            self.avgpool = nn.AdaptiveAvgPool2d(1)

            # BBox Head: 2048 -> 512 -> 4
            self.bbox_head = nn.Sequential(
                nn.Linear(576, 512),
                nn.ReLU(),
                nn.Linear(512, 4),
                nn.Sigmoid()  # Output [cx, cy, w, h] in [0, 1]
            )

            # Actor Policy: 2048+4 -> 1024 -> 512 -> 7
            self.actor = nn.Sequential(
                nn.Linear(576 + 4, 512),
                nn.ReLU(),
                nn.Linear(512, 256),
                nn.ReLU(),
                nn.Linear(256, 7)
            )

        def forward(self, x):
            features = self.backbone(x)
            features = self.avgpool(features).flatten(1)

            # Stage 1: BBox Head
            bbox = self.bbox_head(features)

            # Stage 2: Actor Policy (using bbox as additional state)
            state = torch.cat([features, bbox], dim=1)
            action_logits = self.actor(state)

            return bbox, action_logits

    model = StudentModel()
    model.load_state_dict(model_state_dict, strict=False)
    model.eval()

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
            "action_probs": {0: "batch_size"}
        }
    )
    print(f"ONNX model exported successfully")
    return output_path


def convert_onnx_to_tf(onnx_path: str, output_dir: str):
    """Convert ONNX model to TensorFlow SavedModel."""
    print(f"Converting ONNX to TensorFlow: {output_dir}")

    try:
        import onnx
        from onnx_tf.backend import prepare

        onnx_model = onnx.load(onnx_path)
        onnx.checker.check_model(onnx_model)

        tf_rep = prepare(onnx_model)
        tf_rep.export_graph(output_dir)
        print(f"TensorFlow SavedModel exported to: {output_dir}")
        return output_dir
    except ImportError:
        print("Error: onnx-tf not installed. Run: pip install onnx-tf")
        sys.exit(1)


def convert_tf_to_tflite(saved_model_dir: str, output_path: str, quantize: str = "int8"):
    """Convert TensorFlow SavedModel to TFLite with optional quantization."""
    print(f"Converting to TFLite with quantization={quantize}")

    import tensorflow as tf

    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

    if quantize == "fp16":
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
    elif quantize == "int8":
        converter.optimizations = [tf.lite.Optimize.DEFAULT]

        def representative_dataset():
            for _ in range(100):
                data = np.random.rand(1, 224, 224, 3).astype(np.float32)
                yield [data]

        converter.representative_dataset = representative_dataset
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        # 保持 float32 输入/输出，端侧预处理使用 float32，避免类型不匹配
        converter.inference_input_type = tf.float32
        converter.inference_output_type = tf.float32

    tflite_model = converter.convert()

    with open(output_path, "wb") as f:
        f.write(tflite_model)

    model_size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"TFLite model saved: {output_path} ({model_size_mb:.2f} MB)")
    return output_path


def verify_tflite_model(tflite_path: str, input_size: int = 224):
    """Verify the converted TFLite model."""
    print(f"Verifying TFLite model: {tflite_path}")

    import tensorflow as tf

    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print(f"Input shape: {input_details[0]['shape']}")
    print(f"Output shapes: {[d['shape'] for d in output_details]}")

    # Test inference - 使用 float32 输入（端侧 AdacropInferenceEngine 使用 float32）
    test_input = np.random.rand(1, input_size, input_size, 3).astype(np.float32)

    interpreter.set_tensor(input_details[0]['index'], test_input)
    interpreter.invoke()

    bbox_output = interpreter.get_tensor(output_details[0]['index'])
    action_output = interpreter.get_tensor(output_details[1]['index'])

    print(f"BBox output: {bbox_output}")
    print(f"Action output: {action_output}")
    print("TFLite model verification passed!")


def main():
    args = parse_args()

    # Create output directory
    os.makedirs(os.path.dirname(args.output) if os.path.dirname(args.output) else ".", exist_ok=True)

    # Step 1: Load PyTorch model
    state_dict = load_pytorch_model(args.input)

    # Step 2: Export to ONNX
    onnx_path = args.output.replace(".tflite", ".onnx")
    export_to_onnx(state_dict, onnx_path, args.input_size)

    # Step 3: Convert ONNX to TensorFlow
    tf_dir = args.output.replace(".tflite", "_tf")
    convert_onnx_to_tf(onnx_path, tf_dir)

    # Step 4: Convert TensorFlow to TFLite
    convert_tf_to_tflite(tf_dir, args.output, args.quantize)

    # Step 5: Verify
    verify_tflite_model(args.output, args.input_size)

    print("\n" + "=" * 50)
    print("Conversion completed successfully!")
    print(f"Output: {args.output}")
    print("\nNext steps:")
    print(f"1. Copy {args.output} to Android app: app/src/main/assets/")
    print("2. Build and run the Android app")
    print("=" * 50)


if __name__ == "__main__":
    main()
