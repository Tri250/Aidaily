# ============================================================
# LiveCaptureAndroid ProGuard Rules (2026 正式版)
# ============================================================

# ---- 基础优化 ----
-optimizations !code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes Exceptions

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

# ---- Gson (序列化/反序列化) ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.livecompose.livecapture.core.storage.** { *; }
-keep class com.livecompose.livecapture.core.lut.** { *; }
-keep class com.livecompose.livecapture.core.state.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- DataStore ----
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# ---- CameraX ----
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---- Compose (2026 关键规则) ----
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.graphics.** { *; }
-keepclassmembers class androidx.compose.runtime.internal.ComposableLambdaImpl {
    <methods>;
}

# ---- Navigation ----
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.navigation.NavArgs { *; }

# ---- Coroutines ----
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---- Coil ----
-keep class coil.** { *; }
-dontwarn coil.**

# ---- ViewModel ----
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ---- Parcelable ----
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# ---- Keep data classes for serialization ----
-keep class com.livecompose.livecapture.** { *; }

# ---- Remove logging in release ----
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ---- Bugly (崩溃上报) ----
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

# ---- 2026 新增：反射安全 ----
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---- 2026 新增：Kotlin 反射 ----
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ---- 2026 新增：R8 完整模式优化 ----
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*