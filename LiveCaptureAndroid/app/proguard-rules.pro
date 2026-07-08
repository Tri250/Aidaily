# ============================================================
# LiveCaptureAndroid ProGuard Rules
# ============================================================

# ---- TensorFlow Lite ----
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }
-dontwarn org.tensorflow.lite.**

# ---- ML Kit ----
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_face.** { *; }
-dontwarn com.google.mlkit.**

# ---- Gson ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.livecompose.livecapture.core.storage.** { *; }
-keep class com.livecompose.livecapture.core.lut.** { *; }

# ---- DataStore ----
-keep class androidx.datastore.** { *; }

# ---- CameraX ----
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---- Compose ----
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ---- Coroutines ----
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ---- Coil ----
-keep class coil.** { *; }
-dontwarn coil.**

# ---- ViewModel ----
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ---- Keep data classes for serialization ----
-keep class com.livecompose.livecapture.** { *; }

# ---- Remove logging in release ----
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ---- Bugly ----
-dontwarn com.tencent.bugly.**
-keep class com.tencent.bugly.** { *; }
-keep class android.app.Application { *; }

# ---- 微信 OpenSDK ----
-keep class com.tencent.mm.opensdk.** { *; }
-keep class com.tencent.wxop.** { *; }
-keep class com.tencent.mm.sdk.** { *; }
-dontwarn com.tencent.mm.**

# ---- 加密相关 ----
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }