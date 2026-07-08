package com.livecompose.livecapture.core

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

private val Context.crashDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "crash_handler"
)

/**
 * 全局崩溃处理器
 * 实现 Thread.UncaughtExceptionHandler
 * 捕获未处理异常，写入日志文件
 * 在下次启动时检测上次是否崩溃，提示用户恢复
 */
class CrashHandler private constructor(private val context: Context) :
    Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val store = context.crashDataStore

    /**
     * 最近一次崩溃信息
     */
    data class CrashInfo(
        val timestamp: Long,
        val exceptionMessage: String,
        val stackTrace: String,
        val threadName: String,
        val deviceInfo: String
    ) {
        val formattedTime: String
            get() {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                return sdf.format(Date(timestamp))
            }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 保存崩溃信息（包含崩溃标记）
        saveCrashInfo(thread, throwable)

        // 写入日志文件
        writeCrashLog(thread, throwable)

        // 传递给默认处理器
        defaultHandler?.uncaughtException(thread, throwable)
    }

    /**
     * 保存崩溃信息到 DataStore
     * 使用带超时的 runBlocking 防止死锁
     */
    private fun saveCrashInfo(thread: Thread, throwable: Throwable) {
        try {
            val crashInfo = CrashInfo(
                timestamp = System.currentTimeMillis(),
                exceptionMessage = throwable.message ?: "Unknown error",
                stackTrace = getStackTraceString(throwable),
                threadName = thread.name,
                deviceInfo = buildDeviceInfo()
            )

            runBlocking {
                withTimeoutOrNull(2000L) {
                    store.edit { preferences ->
                        preferences[CRASH_TIMESTAMP_KEY] = crashInfo.timestamp
                        preferences[CRASH_MESSAGE_KEY] = crashInfo.exceptionMessage
                        preferences[CRASH_STACK_KEY] = crashInfo.stackTrace
                        preferences[CRASH_THREAD_KEY] = crashInfo.threadName
                        preferences[CRASH_DEVICE_KEY] = crashInfo.deviceInfo
                        preferences[CRASH_OCCURRED_KEY] = true
                    }
                }
            }
        } catch (_: Exception) {
            // 保存崩溃信息失败不影响主流程
        }
    }

    /**
     * 写入崩溃日志文件
     */
    private fun writeCrashLog(thread: Thread, throwable: Throwable) {
        try {
            val logDir = getLogDirectory()
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "crash_${dateFormat.format(Date())}.log"
            val logFile = File(logDir, fileName)

            FileWriter(logFile).use { writer ->
                writer.write("========================================\n")
                writer.write("CRASH REPORT\n")
                writer.write("========================================\n")
                writer.write("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}\n")
                writer.write("Thread: ${thread.name}\n")
                writer.write("Device: ${buildDeviceInfo()}\n")
                writer.write("----------------------------------------\n")
                writer.write("Exception: ${throwable.javaClass.name}\n")
                writer.write("Message: ${throwable.message ?: "N/A"}\n")
                writer.write("----------------------------------------\n")
                writer.write("Stack Trace:\n")
                writer.write(getStackTraceString(throwable))
                writer.write("\n========================================\n")

                // 记录设备信息
                writer.write("App Version: ${getAppVersion()}\n")
                writer.write("OS Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
                writer.write("Manufacturer: ${Build.MANUFACTURER}\n")
                writer.write("Model: ${Build.MODEL}\n")
                writer.write("========================================\n")
            }
        } catch (_: Exception) {
            // 日志写入失败不影响主流程
        }
    }

    /**
     * 检查上次是否崩溃
     */
    fun wasLastSessionCrashed(): Boolean {
        return try {
            runBlocking {
                val preferences = store.data.first()
                preferences[CRASH_OCCURRED_KEY] ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取上次崩溃信息
     */
    fun getLastCrashInfo(): CrashInfo? {
        return try {
            runBlocking {
                val preferences = store.data.first()
                val occurred = preferences[CRASH_OCCURRED_KEY] ?: false
                if (!occurred) return@runBlocking null

                CrashInfo(
                    timestamp = preferences[CRASH_TIMESTAMP_KEY] ?: 0L,
                    exceptionMessage = preferences[CRASH_MESSAGE_KEY] ?: "Unknown",
                    stackTrace = preferences[CRASH_STACK_KEY] ?: "",
                    threadName = preferences[CRASH_THREAD_KEY] ?: "unknown",
                    deviceInfo = preferences[CRASH_DEVICE_KEY] ?: "unknown"
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 清除崩溃状态
     */
    fun clearCrashState() {
        try {
            runBlocking {
                store.edit { preferences ->
                    preferences.remove(CRASH_OCCURRED_KEY)
                    preferences.remove(CRASH_TIMESTAMP_KEY)
                    preferences.remove(CRASH_MESSAGE_KEY)
                    preferences.remove(CRASH_STACK_KEY)
                    preferences.remove(CRASH_THREAD_KEY)
                    preferences.remove(CRASH_DEVICE_KEY)
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 获取所有崩溃日志文件
     */
    fun getCrashLogFiles(): List<File> {
        val logDir = getLogDirectory()
        return if (logDir.exists()) {
            logDir.listFiles { file -> file.name.startsWith("crash_") && file.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 清理旧崩溃日志（保留最近 N 个）
     */
    fun cleanOldCrashLogs(keepCount: Int = 10) {
        val logs = getCrashLogFiles()
        if (logs.size > keepCount) {
            logs.drop(keepCount).forEach { it.delete() }
        }
    }

    private fun getLogDirectory(): File {
        val externalDir = context.getExternalFilesDir(null)
        return if (externalDir != null) {
            File(externalDir, "crash_logs")
        } else {
            File(context.filesDir, "crash_logs")
        }
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.flush()
        return stringWriter.toString()
    }

    private fun buildDeviceInfo(): String {
        return "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}), " +
                "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    companion object {
        private var instance: CrashHandler? = null

        // DataStore keys
        private val CRASH_OCCURRED_KEY = booleanPreferencesKey("crash_occurred")
        private val CRASH_TIMESTAMP_KEY = longPreferencesKey("crash_timestamp")
        private val CRASH_MESSAGE_KEY = stringPreferencesKey("crash_message")
        private val CRASH_STACK_KEY = stringPreferencesKey("crash_stack")
        private val CRASH_THREAD_KEY = stringPreferencesKey("crash_thread")
        private val CRASH_DEVICE_KEY = stringPreferencesKey("crash_device")

        /**
         * 初始化全局崩溃处理器
         * 应在 Application.onCreate() 中调用
         */
        fun initialize(context: Context): CrashHandler {
            if (instance == null) {
                instance = CrashHandler(context.applicationContext)
                Thread.setDefaultUncaughtExceptionHandler(instance)
            }
            return instance!!
        }

        /**
         * 获取崩溃处理器实例
         */
        fun getInstance(): CrashHandler? = instance
    }
}