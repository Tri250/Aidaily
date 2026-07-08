package com.livecompose.livecapture.core.errorhandling

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 已记录的错误条目
 *
 * 用于 [AppErrorHandler.errorHistory]，包含错误实例、时间戳及格式化后的时间字符串。
 * 对应 iOS 端 LoggedError 结构体。
 *
 * @param error 错误实例
 * @param timestamp 发生时间戳（毫秒）
 * @param formattedTimestamp 由时间戳格式化后的字符串（yyyy-MM-dd HH:mm:ss）
 */
data class LoggedError(
    val error: AppError,
    val timestamp: Long,
    val formattedTimestamp: String
) {
    /**
     * 便捷构造：仅传入错误与时间戳，[formattedTimestamp] 由时间戳自动计算
     */
    constructor(error: AppError, timestamp: Long) : this(
        error = error,
        timestamp = timestamp,
        formattedTimestamp = FORMATTER.format(Date(timestamp))
    )

    companion object {
        private val FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
}
