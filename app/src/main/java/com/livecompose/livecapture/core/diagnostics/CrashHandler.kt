package com.livecompose.livecapture.core.diagnostics

import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常处理器
 *
 * 功能：
 * - 捕获所有未处理异常，防止静默崩溃
 * - 将崩溃堆栈写入应用私有目录 crash_logs 文件
 * - 保留原始默认处理器，崩溃后仍执行系统默认行为
 * - 在下次启动时可通过 SelfChecker 检测到上次崩溃日志
 */
class CrashHandler(
    private val crashLogDir: File,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashHandler"
        private const val MAX_CRASH_LOG_FILES = 5

        /**
         * 注册全局崩溃处理器
         * @param crashLogDir 崩溃日志存储目录
         */
        fun register(crashLogDir: File) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler is CrashHandler) {
                // 已经注册过了
                return
            }
            val handler = CrashHandler(crashLogDir, currentHandler)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            Log.i(TAG, "全局崩溃处理器已注册")
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 将堆栈写入日志文件
            writeCrashLog(thread, throwable)
        } catch (e: Exception) {
            // 写入日志本身失败，不阻塞崩溃处理
            Log.e(TAG, "无法写入崩溃日志", e)
        }

        // 调用原始默认处理器（系统行为：显示崩溃对话框 / 重启应用）
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun writeCrashLog(thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val logFile = File(crashLogDir, "crash_$timestamp.txt")

        // 确保目录存在
        crashLogDir.mkdirs()

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("=== LiveCapture Crash Report ===")
        pw.println("Time: $timestamp")
        pw.println("Thread: ${thread.name} (id=${thread.id})")
        pw.println("Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        pw.println("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        pw.println()
        throwable.printStackTrace(pw)
        pw.flush()

        logFile.writeText(sw.toString())

        // 清理旧日志，只保留最近 N 个
        cleanupOldLogs()
    }

    private fun cleanupOldLogs() {
        val logFiles = crashLogDir.listFiles { file ->
            file.name.startsWith("crash_") && file.name.endsWith(".txt")
        }?.sortedByDescending { it.lastModified() } ?: return

        if (logFiles.size > MAX_CRASH_LOG_FILES) {
            logFiles.drop(MAX_CRASH_LOG_FILES).forEach { it.delete() }
        }
    }
}