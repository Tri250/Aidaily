# ProGuard rules for LiveCapture
-keep class com.livecompose.livecapture.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes *Annotation*, Signature, Exception, InnerClasses

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
