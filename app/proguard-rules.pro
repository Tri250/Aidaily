# ProGuard rules for LiveCapture

# ===== 通用属性保留 =====
-keepattributes *Annotation*, Signature, Exception, InnerClasses, SourceFile, LineNumberTable

# ===== TensorFlow Lite =====
# TFLite Interpreter  native 方法反射调用
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

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

# ===== 移除调试日志 (Release 构建) =====
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
