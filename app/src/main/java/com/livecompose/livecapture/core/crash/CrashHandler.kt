package com.livecompose.livecapture.core.crash

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局崩溃捕获与日志记录
 *
 * 注意: 此类中的中文字符串仅用于本地崩溃日志文件写入与 Log 输出，非 UI 展示文案，无需国际化。
 */
@Singleton
class CrashHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashHandler"
        private const val CRASH_DIR = "crash_logs"
        private const val MAX_LOG_FILES = 10
        private const val CRASH_FLAG_FILE = "last_crash_flag"
    }

    data class CrashLogEntry(
        val timestamp: Long,
        val message: String,
        val stackTrace: String
    )

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private val crashDir = File(context.filesDir, CRASH_DIR)
    private var hadRecentCrash = false

    fun init() {
        // Check if the previous session ended with a crash (flag file exists)
        val flagFile = File(context.filesDir, CRASH_FLAG_FILE)
        hadRecentCrash = flagFile.exists()
        if (hadRecentCrash) {
            flagFile.delete()
            Log.i(TAG, "检测到上次运行崩溃")
        }
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.i(TAG, "CrashHandler 已安装")
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashLog(throwable)
            setCrashFlag()
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃日志失败", e)
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashLog(throwable: Throwable) {
        if (!crashDir.exists()) crashDir.mkdirs()

        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(timestamp))
        val logFile = File(crashDir, "crash_$dateStr.log")

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }

        val logContent = buildString {
            appendLine("=== 崩溃日志 ===")
            appendLine("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))}")
            appendLine()
            appendLine("== 设备信息 ==")
            appendLine("设备型号: ${Build.MODEL}")
            appendLine("制造商: ${Build.MANUFACTURER}")
            appendLine("Android 版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine()
            appendLine("== 应用信息 ==")
            appendLine("版本: $versionName ($versionCode)")
            appendLine("包名: ${context.packageName}")
            appendLine()
            appendLine("== 堆栈追踪 ==")
            appendLine(throwable.message ?: "null")
            appendLine()
            appendLine(getStackTraceString(throwable))
        }

        logFile.writeText(logContent)
        trimOldLogs()
        Log.i(TAG, "崩溃日志已保存: ${logFile.name}")
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sb = StringBuilder()
        sb.appendLine("${throwable.javaClass.name}: ${throwable.message ?: ""}")
        for (element in throwable.stackTrace) {
            sb.appendLine("    at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
        }
        var cause = throwable.cause
        while (cause != null) {
            sb.appendLine("Caused by: ${cause.javaClass.name}: ${cause.message ?: ""}")
            for (element in cause.stackTrace) {
                sb.appendLine("    at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
            }
            cause = cause.cause
        }
        return sb.toString()
    }

    private fun trimOldLogs() {
        val files = crashDir.listFiles()
            ?.filter { it.name.startsWith("crash_") && it.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (files.size > MAX_LOG_FILES) {
            files.drop(MAX_LOG_FILES).forEach { file ->
                file.delete()
                Log.i(TAG, "删除旧崩溃日志: ${file.name}")
            }
        }
    }

    private fun setCrashFlag() {
        val flagFile = File(context.filesDir, CRASH_FLAG_FILE)
        flagFile.writeText(System.currentTimeMillis().toString())
    }

    fun hasRecentCrash(): Boolean = hadRecentCrash

    fun getCrashLogs(): List<CrashLogEntry> {
        if (!crashDir.exists()) return emptyList()

        return crashDir.listFiles()
            ?.filter { it.name.startsWith("crash_") && it.name.endsWith(".log") }
            ?.map { file ->
                parseCrashLog(file)
            }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    private fun parseCrashLog(file: File): CrashLogEntry {
        val content = file.readText()
        val timestamp = file.name.removePrefix("crash_").removeSuffix(".log")
            .let { dateStr ->
                try {
                    SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).parse(dateStr)?.time
                        ?: file.lastModified()
                } catch (_: Exception) {
                    file.lastModified()
                }
            }

        val message = content.lines()
            .firstOrNull { it.startsWith("时间:") }
            ?.removePrefix("时间:")
            ?.trim()
            ?: "Unknown"

        val stackTraceStart = content.indexOf("== 堆栈追踪 ==")
        val stackTrace = if (stackTraceStart >= 0) {
            content.substring(stackTraceStart + "== 堆栈追踪 ==".length).trim()
        } else {
            content.trim()
        }

        return CrashLogEntry(
            timestamp = timestamp,
            message = message,
            stackTrace = stackTrace
        )
    }

    fun clearCrashLogs() {
        if (crashDir.exists()) {
            crashDir.listFiles()?.forEach { it.delete() }
        }
        hadRecentCrash = false
        Log.i(TAG, "崩溃日志已清除")
    }
}
