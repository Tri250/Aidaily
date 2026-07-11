# ProGuard rules for LiveCapture (2026 Release)
# 策略: 精准保留必要类，其余全部混淆/优化/移除

# ===== 保留属性 (堆栈追踪、注解、泛型) =====
-keepattributes *Annotation*, Signature, Exception, InnerClasses, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes EnclosingMethod

# ===== 应用核心 =====
# Hilt 入口点 (Application 和 Activity 必须保留)
-keep class com.livecompose.livecapture.LiveCaptureApp { *; }
-keep class com.livecompose.livecapture.MainActivity { *; }

# Hilt 生成的 Dagger 组件/模块
-keep class com.livecompose.livecapture.di.** { *; }

# DI 注入目标 (被 Hilt @Inject 构造的类)
-keep class com.livecompose.livecapture.core.camera.CameraManager { *; }
-keep class com.livecompose.livecapture.core.detection.AdacropInferenceEngine { *; }
-keep class com.livecompose.livecapture.core.detection.CompositionResult { *; }
-keep class com.livecompose.livecapture.core.detection.CompositionResult$ActionType { *; }
-keep class com.livecompose.livecapture.core.motion.BoxCenterManager { *; }
-keep class com.livecompose.livecapture.core.motion.MotionStabilityMonitor { *; }
-keep class com.livecompose.livecapture.core.diagnostics.SelfChecker { *; }
-keep class com.livecompose.livecapture.core.diagnostics.SelfChecker$CheckItem { *; }
-keep class com.livecompose.livecapture.core.diagnostics.SelfChecker$CheckStatus { *; }
-keep class com.livecompose.livecapture.core.permission.PermissionManager { *; }
-keep class com.livecompose.livecapture.core.settings.SettingsRepository { *; }
-keep class com.livecompose.livecapture.core.storage.PhotoStorageService { *; }
-keep class com.livecompose.livecapture.core.storage.PhotoRecord { *; }
-keep class com.livecompose.livecapture.core.storage.CropRegion { *; }
-keep class com.livecompose.livecapture.core.storage.ExifData { *; }

# ViewModel (Hilt 注入 + Compose Navigation)
-keep class com.livecompose.livecapture.presentation.**ViewModel { *; }
-keep class com.livecompose.livecapture.presentation.MainTabView$Screen { *; }

# DataStore 序列化 (Preferences)
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ===== TensorFlow Lite =====
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }
-keep class org.tensorflow.lite.support.** { *; }

# ===== Hilt / Dagger =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-dontwarn dagger.hilt.**

# ===== CameraX =====
-keep class androidx.camera.** { *; }

# ===== Compose =====
-keep class androidx.compose.** { *; }

# ===== Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

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

# ===== 移除日志 (Release 构建) =====
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}