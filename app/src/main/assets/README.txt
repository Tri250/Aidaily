LiveCapture Android - 模型文件目录

放置转换后的 TFLite 模型文件:
  adacrop_student.tflite  - Student 模型 (Fast 模式, MobileNetV3-Small, ~4-6MB)
  adacrop_teacher.tflite  - Teacher 模型 (Pro 模式, MobileNetV3-Large, ~8-12MB)

模型获取方式:
1. 从 Hugging Face 下载: https://huggingface.co/LiveCompose/adacrop
2. 或使用 /model_conversion/ 目录下的脚本自行转换 (需 PyTorch 模型权重)

转换方法见 /model_conversion/README.md

注意:
- 至少需放入 adacrop_student.tflite, 否则 App 进入降级模式 (默认构图)
- Pro 模式 (Teacher) 缺失时自动回退到 Student
- 两个模型输入输出契约一致: 输入 [1,224,224,3] float32 [0,1], 输出 bbox[1,4]+action[1,7]
