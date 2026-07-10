# Model Conversion Guide

## Prerequisites

```bash
pip install torch torchvision onnx onnx-tf tensorflow
```

## Quick Start

```bash
# Convert with INT8 quantization (recommended for mobile)
python convert_to_tflite.py \
    --input ../LiveCompose/distillation/runs/student_best.pth \
    --output adacrop_student_int8.tflite \
    --quantize int8

# Or FP16 quantization
python convert_to_tflite.py \
    --input ../LiveCompose/distillation/runs/student_best.pth \
    --output adacrop_student_fp16.tflite \
    --quantize fp16

# No quantization
python convert_to_tflite.py \
    --input ../LiveCompose/distillation/runs/student_best.pth \
    --output adacrop_student.tflite \
    --quantize none
```

## Copy to Android Project

```bash
cp adacrop_student_int8.tflite ../app/src/main/assets/
```

## Expected Output Sizes

| Quantization | Expected Size | Inference Speed |
|-------------|---------------|-----------------|
| None (FP32) | ~15-20 MB | Baseline |
| FP16 | ~8-10 MB | 1.5-2x faster |
| INT8 | ~4-6 MB | 2-4x faster |

## Troubleshooting

### ONNX-TF Import Error
If `onnx-tf` fails to import, try:
```bash
pip install onnx-tf==1.10.0
```

### Model Architecture Mismatch
If the student model architecture differs from the default MobileNetV3-Small,
modify the `StudentModel` class in `convert_to_tflite.py` to match your
actual model definition from `LiveCompose/Adacrop/src/models.py`.
