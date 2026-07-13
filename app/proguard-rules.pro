# ProGuard rules for LiveCapture v1.5.9
# Android 15/16 兼容 — 防止 R8 过度混淆导致启动崩溃

# ===== 通用属性保留 =====
-keepattributes *Annotation*, Signature, Exception, InnerClasses, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes EnclosingMethod

# ===== Kotlin =====
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.coroutines.Continuation
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ===== Hilt / Dagger DI (关键：缺失会导致启动时 DI 失败崩溃) =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.internal.aggregatedroot.** { *; }
-keep,allowobfuscation,allowshrinking class dagger.hilt.android.internal.managers.** { *; }
-keep class androidx.hilt.** { *; }

# Hilt 生成的组件必须保留
-keep class * extends dagger.hilt.android.components.ActivityComponent { *; }
-keep class * extends dagger.hilt.android.components.ActivityRetainedComponent { *; }
-keep class * extends dagger.hilt.android.components.ServiceComponent { *; }
-keep class * extends dagger.hilt.android.components.ViewComponent { *; }
-keep class * extends dagger.hilt.android.components.ViewModelComponent { *; }
-keep class * extends dagger.hilt.android.components.FragmentComponent { *; }
-keep class * extends dagger.hilt.components.SingletonComponent { *; }

# Hilt ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Hilt Module
-keep class * extends dagger.Module { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @javax.inject.Singleton class * { *; }
-keep @javax.inject.Inject class * { *; }

# 保留 @AndroidEntryPoint 注解的类
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# 保留 Application 类及其构造函数
-keep class com.livecompose.livecapture.LiveCaptureApp { *; }
-keep class com.livecompose.livecapture.MainActivity { *; }

# ===== Dagger 生成的类 =====
-keep class dagger.** { *; }
-keep class * implements dagger.internal.Factory { *; }
-keep class * implements dagger.MembersInjector { *; }
-dontwarn dagger.**

# ===== Compose (关键：缺失会导致 Compose 渲染崩溃) =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.** {
    <init>(...);
    *** Companion;
}
-keep class androidx.compose.runtime.** { *; }

# ===== CameraX =====
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class androidx.camera.camera2.interop.** { *; }
-keep class androidx.concurrent.futures.** { *; }

# ===== Navigation Compose =====
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ===== DataStore Preferences =====
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ===== Coil (图片加载) =====
-keep class coil.** { *; }
-dontwarn coil.**
-keep class kotlinx.coroutines.** { *; }

# ===== Lifecycle / ViewModel =====
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
-keep class * implements androidx.lifecycle.ViewModelProvider$Factory { *; }
-keep class * implements androidx.lifecycle.GeneratedAdapter { *; }

# ===== Activity / AppCompat =====
-keep class androidx.activity.** { *; }
-dontwarn androidx.activity.**
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**

# ===== TensorFlow Lite =====
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-keep class org.tensorflow.lite.nnapi.** { *; }

# ===== EXIF =====
-keep class androidx.exifinterface.** { *; }
-dontwarn androidx.exifinterface.**

# ===== 枚举 =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== Serializable / Parcelable =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===== 项目自身关键类 (防止被 R8 优化掉) =====
-keep class com.livecompose.livecapture.** { *; }
-keepclassmembers class com.livecompose.livecapture.** {
    <init>(...);
    *** Companion;
}
-keep class com.livecompose.livecapture.core.design.LiveCaptureTheme { *; }
-keep class com.livecompose.livecapture.presentation.MainTabViewKt { *; }
-keep class com.livecompose.livecapture.di.AppModule { *; }

# 保留 Composable 函数
-keepclassmembers class com.livecompose.livecapture.presentation.** {
    @androidx.compose.runtime.Composable <methods>;
}

# ===== 保留资源 (防止 shrinkResources 误删) =====
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ===== 保留 native 方法 =====
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===== 移除调试日志 (Release 构建) =====
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# ===== 通用安全规则 =====
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }