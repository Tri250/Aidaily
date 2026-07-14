import os
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

os.makedirs("/workspace/app/src/main/assets", exist_ok=True)

def build_student_model():
    """Student: 轻量模型, 输入 [1,224,224,3] -> bbox[1,4] + action[1,7]"""
    inputs = keras.Input(shape=(224, 224, 3), name="image")
    x = layers.Conv2D(16, 3, strides=2, activation="relu", padding="same")(inputs)
    x = layers.MaxPooling2D()(x)
    x = layers.Conv2D(32, 3, strides=2, activation="relu", padding="same")(x)
    x = layers.MaxPooling2D()(x)
    x = layers.Conv2D(64, 3, strides=2, activation="relu", padding="same")(x)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(128, activation="relu")(x)

    bbox = layers.Dense(4, activation="sigmoid", name="bbox")(x)
    action = layers.Dense(7, name="action_probs")(x)

    return keras.Model(inputs=inputs, outputs=[bbox, action])

def build_teacher_model():
    """Teacher: 稍大模型, 同样的输入输出契约"""
    inputs = keras.Input(shape=(224, 224, 3), name="image")
    x = layers.Conv2D(32, 3, strides=2, activation="relu", padding="same")(inputs)
    x = layers.MaxPooling2D()(x)
    x = layers.Conv2D(64, 3, strides=2, activation="relu", padding="same")(x)
    x = layers.MaxPooling2D()(x)
    x = layers.Conv2D(128, 3, strides=2, activation="relu", padding="same")(x)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(256, activation="relu")(x)
    x = layers.Dropout(0.1)(x)

    bbox = layers.Dense(4, activation="sigmoid", name="bbox")(x)
    action = layers.Dense(7, name="action_probs")(x)

    return keras.Model(inputs=inputs, outputs=[bbox, action])

def convert_and_verify(model, output_path, name):
    print(f"Converting {name} -> {output_path}")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    with open(output_path, "wb") as f:
        f.write(tflite_model)
    size_kb = os.path.getsize(output_path) / 1024
    print(f"Saved {output_path} ({size_kb:.1f} KB)")

    # Verify
    interpreter = tf.lite.Interpreter(model_path=output_path)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()
    out = interpreter.get_output_details()
    print(f"  Input:  {inp[0]['shape']} {inp[0]['dtype']}")
    for i, o in enumerate(out):
        print(f"  Output[{i}]: {o['shape']} {o['dtype']}")

    # Quick inference test
    test_data = np.random.rand(1, 224, 224, 3).astype(np.float32)
    interpreter.set_tensor(inp[0]['index'], test_data)
    interpreter.invoke()
    print(f"  Inference OK")
    print()

student = build_student_model()
convert_and_verify(student, "/workspace/app/src/main/assets/adacrop_student.tflite", "Student")

teacher = build_teacher_model()
convert_and_verify(teacher, "/workspace/app/src/main/assets/adacrop_teacher.tflite", "Teacher")

print("All TFLite models generated successfully.")
