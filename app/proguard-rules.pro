# ProGuard rules for LiveCapture (2026 Release)

# ===== 应用核心 =====
-keep class com.livecompose.livecapture.** { *; }
-keepattributes *Annotation*, Signature, Exception, InnerClasses, SourceFile, LineNumberTable

# ===== TensorFlow Lite =====
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
# TFLite 模型推理通过反射调用，禁止混淆 Interpreter 相关类
-dontwarn org.tensorflow.lite.**

# ===== Hilt / Dagger =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# ===== CameraX =====
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ===== Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== DataStore =====
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ===== AndroidX / Compose =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

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
