# LiveCapture ProGuard/R8 规则 - v1.5.9 全量保护
# ============================================================================
# 核心原则：Release 构建必须保留所有框架生成的代码和反射使用的类
# ============================================================================

# === Hilt / Dagger ===
# Hilt 生成的组件和模块类在编译时生成，R8 必须保留
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class dagger.hilt.android.internal.modules.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.scopes.ActivityScoped class * { *; }
-keep @dagger.hilt.android.scopes.FragmentScoped class * { *; }
-keep @dagger.hilt.android.scopes.ViewScoped class * { *; }
-keep @dagger.hilt.android.scopes.ViewModelScoped class * { *; }
-keep @javax.inject.Singleton class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
# Hilt 生成的 EntryPoint 接口
-keep class * extends dagger.hilt.internal.GeneratedEntryPoint { *; }
# Application 类（Hilt 生成 Hilt_ 前缀的子类）
-keep class com.livecompose.livecapture.LiveCaptureApp { *; }
-keep class com.livecompose.livecapture.Hilt_* { *; }

# === DataStore / Preferences ===
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences$Key {
    <init>(...);
}
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.core.DataMigration { *; }
-dontwarn androidx.datastore.**

# === CameraX ===
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }
-dontwarn androidx.camera.core.**
-dontwarn androidx.camera.camera2.**

# === Compose ===
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
# Compose Preview 不需要在 release 中保留，但避免错误
-dontwarn androidx.compose.ui.tooling.preview.Preview

# === Kotlinx Coroutines ===
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# === Kotlin 标准库 ===
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# === TensorFlow Lite ===
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.lite.**

# === Kotlinx Serialization (如有使用) ===
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}

# === AndroidX Lifecycle ===
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }

# === Navigation Compose ===
-keep class androidx.navigation.** { *; }
-keep class androidx.hilt.navigation.compose.** { *; }

# === LiveCompose 应用类 ===
# 保留所有应用业务类，防止 R8 误删
-keep class com.livecompose.livecapture.** { *; }
-keepclassmembers class com.livecompose.livecapture.** { *; }

# === JNI / Native ===
-keep class * {
    native <methods>;
}

# === 枚举 ===
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# === Parcelable ===
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# === Serializable ===
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# === 反射调用 ===
-keepclassmembers class * {
    public <init>(android.content.Context);
}

# === R8 优化提示 ===
# 不要混淆日志标签（方便生产环境排查）
-keepnames class com.livecompose.livecapture.core.diagnostics.SelfChecker
-keepnames class com.livecompose.livecapture.core.diagnostics.CrashHandler
