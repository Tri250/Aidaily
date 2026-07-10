# AdaCrop 模型转换指南

将 HuggingFace `LiveCompose/adacrop` 仓库的 PyTorch 模型转换为 Android 平台 TFLite 格式。

## 支持的模型变体

| 变体 | 用途 | 架构 | 预期体积 (INT8) | 模式 |
|------|------|------|-----------------|------|
| **Student** | Fast 模式 (默认) | MobileNetV3-Small | 4-6 MB | ~5fps 节电 |
| **Teacher** | Pro 模式 (高精度) | MobileNetV3-Large | 8-12 MB | 全帧率 |

两个模型输入输出契约一致，`AdacropInferenceEngine` 支持运行时动态切换。

## 前置依赖

```bash
pip install huggingface_hub torch torchvision onnx onnx-tf tensorflow numpy safetensors
```

## 快速开始

### 1. 从 HuggingFace 下载并转换 (推荐)

```bash
# 仅转换 Student 模型 (Fast 模式)
python convert_to_tflite.py --variant student --quantize int8

# 仅转换 Teacher 模型 (Pro 模式)
python convert_to_tflite.py --variant teacher --quantize int8

# 同时转换两个模型
python convert_to_tflite.py --variant both --quantize int8
```

私有仓库需设置访问令牌:
```bash
export HF_TOKEN=your_token_here
python convert_to_tflite.py --variant both --quantize int8 --token $HF_TOKEN
```

### 2. 离线模式 (已下载权重)

```bash
python convert_to_tflite.py --variant student \
    --input /path/to/student_best.pth \
    --output-dir output --quantize int8 --no-download
```

### 3. 复制到 Android 项目

```bash
cp output/adacrop_student.tflite ../app/src/main/assets/
cp output/adacrop_teacher.tflite ../app/src/main/assets/
```

## 量化模式对比

| 量化 | 体积 | 推理速度 | 精度损失 | 适用场景 |
|------|------|----------|----------|----------|
| none (FP32) | ~15-20 MB | 基准 | 无 | 调试/精度验证 |
| fp16 | ~8-10 MB | 1.5-2x | 极小 | 平衡 (默认推荐 Teacher) |
| int8 | ~4-6 MB | 2-4x | 轻微 | 移动端生产 (默认推荐 Student) |

## 输入输出契约 (与 AdacropInferenceEngine.kt 对齐)

```
输入: image  [1, 224, 224, 3] float32, 归一化 [0,1] (RGB / 255.0), NHWC 布局
输出: bbox         [1, 4] float32  (cx, cy, w, h, sigmoid 归一化 0~1)
      action_probs [1, 7] float32  (LEFT, RIGHT, UP, DOWN, ZOOM_IN, ZOOM_OUT, STOP)
```

转换脚本在 `verify_tflite_model()` 阶段会自动校验此契约。

## 架构定义

模型架构定义在 `convert_to_tflite.py` 的 `AdaCropStudent` / `AdaCropTeacher` 类中，
应与 `LiveCompose/adacrop` 仓库 `src/models.py` 保持一致。

若 HF 上的真实模型架构与本脚本定义不同，`load_model()` 会通过 `missing`/`unexpected` keys
警告提示。此时需更新本脚本的模型定义以匹配真实架构，避免 `strict=False` 静默跳过层导致
推理输出垃圾值 (空实现风险)。

## 故障排查

### onnx-tf 导入错误
```bash
pip install onnx-tf==1.10.0
```

### HuggingFace 连接失败
```bash
# 使用镜像或代理
export HF_ENDPOINT=https://hf-mirror.com
python convert_to_tflite.py --variant both
```

### INT8 量化精度损失过大
改用 fp16 量化，或提供真实校准数据集替换 `representative_dataset()` 中的随机数据。
