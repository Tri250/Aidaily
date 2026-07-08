# ============================================================
# LiveCaptureAndroid ProGuard / R8 Rules
# ============================================================

# ---- 通用 keepattributes（序列化/注解/反射兼容） ----
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions
-keepattributes LineNumberTable
-keepattributes SourceFile
-renamesourcefileattribute SourceFile

# ---- Kotlin 数据类（保留 copy/componentN 方法） ----
-keepclassmembers class com.livecompose.livecapture.** {
    *** copy(...);
    <init>(...);
}
-keepclassmembers class com.livecompose.livecapture.** {
    *** component*();
}
-keepclassmembers class com.livecompose.livecapture.** implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

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

# ---- Gson（序列化专用 keep） ----
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
# 保留序列化相关数据类
-keep class com.livecompose.livecapture.core.storage.PhotoRecord { *; }
-keep class com.livecompose.livecapture.core.lut.LutRecipe { *; }
-keep class com.livecompose.livecapture.core.lut.LutPreset { *; }
-keep class com.livecompose.livecapture.core.lut.ColorRecipeParams { *; }
-keep class com.livecompose.livecapture.core.lut.LchColorAdjustment { *; }
-keep class com.livecompose.livecapture.core.community.CommunityModels$* { *; }
-keep class com.livecompose.livecapture.core.frame.WatermarkInfo { *; }
-keep class com.livecompose.livecapture.core.frame.FrameInfo { *; }
-keep class com.livecompose.livecapture.core.sharecard.ShareCardStyle { *; }
-keep class com.livecompose.livecapture.core.sharecard.ShareCardMetadata { *; }
-keep class com.livecompose.livecapture.core.portrait.BeautySettings { *; }
-keep class com.livecompose.livecapture.core.portrait.PortraitModels$* { *; }
-keep class com.livecompose.livecapture.core.composition.CompositionModels$* { *; }
-keep class com.livecompose.livecapture.core.intelligence.SceneModels$* { *; }
-keep class com.livecompose.livecapture.core.video.VideoModels$* { *; }
-keep class com.livecompose.livecapture.core.camera.CameraModels$* { *; }
-keep class com.livecompose.livecapture.core.errorhandling.AppError { *; }
-keep class com.livecompose.livecapture.core.errorhandling.LoggedError { *; }

# ---- DataStore ----
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

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

# ---- BuildConfig（渠道标识等） ----
-keep class com.livecompose.livecapture.BuildConfig { *; }

# ---- 移除 debug 日志 ----
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