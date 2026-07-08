package com.livecompose.livecapture.core.logger

import android.util.Log

/**
 * 统一日志管理器
 * 
 * 生产环境自动禁用所有日志输出，开发环境保持完整日志。
 * 替代项目中所有直接使用 android.util.Log 的调用。
 * 
 * 使用方式：
 *   AppLogger.d(TAG, "调试信息")
 *   AppLogger.i(TAG, "关键流程")
 *   AppLogger.w(TAG, "警告信息")
 *   AppLogger.e(TAG, "错误信息", throwable)
 */
object AppLogger {

    /**
     * 是否启用日志输出。
     * 通过 BuildConfig.DEBUG 控制：debug 构建启用，release 构建禁用。
     * 可在特殊场景下通过 [setEnabled] 强制开启（如内部测试版本）。
     */
    private var enabled: Boolean = try {
        BuildConfig.DEBUG
    } catch (_: Exception) {
        true
    }

    private var forceEnabled: Boolean = false

    /**
     * 强制启用日志（用于内部测试版本）
     */
    fun setEnabled(enabled: Boolean) {
        forceEnabled = enabled
    }

    private val isEnabled: Boolean get() = enabled || forceEnabled

    fun d(tag: String, message: String) {
        if (isEnabled) Log.d(tag, message)
    }

    fun d(tag: String, message: String, throwable: Throwable) {
        if (isEnabled) Log.d(tag, message, throwable)
    }

    fun i(tag: String, message: String) {
        if (isEnabled) Log.i(tag, message)
    }

    fun i(tag: String, message: String, throwable: Throwable) {
        if (isEnabled) Log.i(tag, message, throwable)
    }

    fun w(tag: String, message: String) {
        if (isEnabled) Log.w(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable) {
        if (isEnabled) Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String) {
        // 错误日志在生产环境持久化到本地 CrashHandler
        if (isEnabled) Log.e(tag, message)
        CrashLogWriter.write(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        if (isEnabled) Log.e(tag, message, throwable)
        CrashLogWriter.write(tag, "$message: ${throwable.stackTraceToString()}")
    }

    /**
     * 关键流程日志（生产环境也输出，仅用于启动/崩溃等极少场景）
     */
    fun critical(tag: String, message: String) {
        Log.i("LiveCapture", "[$tag] $message")
    }
}

/**
 * 崩溃日志持久化写入器
 * 生产环境错误日志写入本地文件，配合 Bugly 上报
 */
private object CrashLogWriter {
    private const val MAX_LOG_LINES = 200
    private val buffer = ArrayDeque<String>(MAX_LOG_LINES)

    @Synchronized
    fun write(tag: String, message: String) {
        val entry = "${System.currentTimeMillis()}|$tag|$message"
        if (buffer.size >= MAX_LOG_LINES) {
            buffer.removeFirst()
        }
        buffer.addLast(entry)
    }

    fun getRecentLogs(): List<String> = buffer.toList()
}