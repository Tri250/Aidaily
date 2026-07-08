package com.livecompose.livecapture.core.errorhandling

import android.hardware.camera2.CameraAccessException
import androidx.lifecycle.ViewModel
import com.livecompose.livecapture.core.CrashHandler
import com.livecompose.livecapture.core.bugly.BuglyManager
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 全局错误处理器
 *
 * 对应 iOS 端 AppErrorHandler。基于 [ViewModel] 实现生命周期感知，
 * 暴露 [StateFlow] 供 UI 订阅错误状态，并集成 [BuglyManager] 进行崩溃上报、
 * 集成 [CrashHandler] 检测上次启动是否崩溃。
 *
 * ## 暴露状态
 * - [currentError] 当前需要展示的错误
 * - [showErrorAlert] 是否展示错误弹窗
 * - [errorHistory] 错误历史记录（最多 50 条）
 */
class AppErrorHandler : ViewModel() {

    companion object {
        private const val TAG = "AppErrorHandler"
        /** 错误历史最大条数 */
        private const val MAX_HISTORY_COUNT = 50
    }

    // MARK: - UI 状态

    private val _currentError = MutableStateFlow<AppError?>(null)
    /** 当前需要展示的错误 */
    val currentError: StateFlow<AppError?> = _currentError.asStateFlow()

    private val _showErrorAlert = MutableStateFlow(false)
    /** 是否展示错误弹窗 */
    val showErrorAlert: StateFlow<Boolean> = _showErrorAlert.asStateFlow()

    private val _errorHistory = MutableStateFlow<List<LoggedError>>(emptyList())
    /** 错误历史记录（最新在前，最多 [MAX_HISTORY_COUNT] 条） */
    val errorHistory: StateFlow<List<LoggedError>> = _errorHistory.asStateFlow()

    // MARK: - 错误处理

    /**
     * 处理并展示错误
     *
     * 记录日志、上报 Bugly、写入历史，并触发弹窗展示。
     *
     * @param error AppError 实例
     */
    fun handle(error: AppError) {
        AppLogger.e(TAG, "[${error.category.displayName}] ${error.localizedDescription}")
        reportToBugly(error)
        addToHistory(error)
        _currentError.value = error
        _showErrorAlert.value = true
    }

    /**
     * 处理系统 [Throwable]，自动映射为 [AppError] 后处理
     */
    fun handle(throwable: Throwable) {
        handle(mapToAppError(throwable))
    }

    /**
     * 静默记录错误（不展示弹窗）
     *
     * 记录日志、上报 Bugly、写入历史，但不修改 [currentError] / [showErrorAlert]。
     */
    fun logSilently(error: AppError) {
        AppLogger.e(TAG, "[${error.category.displayName}] ${error.localizedDescription}")
        reportToBugly(error)
        addToHistory(error)
    }

    /** 关闭错误弹窗 */
    fun dismissError() {
        _currentError.value = null
        _showErrorAlert.value = false
    }

    /** 清空错误历史 */
    fun clearHistory() {
        _errorHistory.value = emptyList()
    }

    /**
     * 检查上次启动是否崩溃，若崩溃则作为静默错误处理
     *
     * 检测到崩溃后创建 [AppError.UnknownError] 并静默记录（上报 + 写入历史，不弹窗），
     * 随后清除崩溃状态以避免重复上报。应在合适的时机（如主界面就绪后）调用，
     * 避免在主线程阻塞。
     */
    fun checkLastCrash() {
        val crashHandler = CrashHandler.getInstance() ?: return
        if (!crashHandler.wasLastSessionCrashed()) return

        val crashInfo = crashHandler.getLastCrashInfo() ?: return
        val reason = crashInfo.exceptionMessage.ifBlank { "Unknown crash" }
        AppLogger.e(TAG, "检测到上次启动发生崩溃: ${crashInfo.formattedTime}")

        logSilently(AppError.UnknownError(message = "上次崩溃: $reason"))

        crashHandler.clearCrashState()
    }

    // MARK: - 私有方法

    /**
     * 将系统 [Throwable] 映射为 [AppError]
     */
    private fun mapToAppError(throwable: Throwable): AppError {
        return when (throwable) {
            is FileNotFoundException -> AppError.StorageError(
                message = throwable.message ?: "文件未找到",
                code = StorageErrorCode.FILE_NOT_FOUND
            )

            is SocketTimeoutException -> AppError.NetworkError(
                message = throwable.message ?: "网络超时",
                code = NetworkErrorCode.TIMEOUT
            )

            is ConnectException, is UnknownHostException -> AppError.NetworkError(
                message = throwable.message ?: "网络不可用",
                code = NetworkErrorCode.NO_CONNECTION
            )

            is IOException -> {
                val message = throwable.message ?: "IO 异常"
                if (isNetworkRelated(throwable)) {
                    AppError.NetworkError(message = message, code = NetworkErrorCode.UNKNOWN)
                } else {
                    AppError.StorageError(message = message, code = StorageErrorCode.UNKNOWN)
                }
            }

            is CameraAccessException -> AppError.CameraError(
                message = throwable.message ?: "相机访问异常",
                code = CameraErrorCode.CAMERA_UNAVAILABLE
            )

            is SecurityException -> AppError.PermissionError(
                message = throwable.message ?: "权限被拒绝"
            )

            is OutOfMemoryError -> AppError.ProcessingError(
                message = throwable.message ?: "内存不足",
                code = ProcessingErrorCode.UNKNOWN
            )

            else -> AppError.UnknownError(
                message = throwable.message ?: throwable.javaClass.simpleName,
                cause = throwable
            )
        }
    }

    /**
     * 判断 [IOException] 是否为网络相关
     */
    private fun isNetworkRelated(throwable: IOException): Boolean {
        val name = throwable.javaClass.name
        return name.contains("Socket", ignoreCase = true) ||
            name.contains("Net", ignoreCase = true) ||
            name.contains("Url", ignoreCase = true) ||
            name.contains("Http", ignoreCase = true) ||
            name.contains("Ssl", ignoreCase = true)
    }

    /**
     * 上报到 Bugly
     *
     * 通过自定义数据携带错误分类，通过异常对象携带分类名称与中文描述。
     * [AppError.UnknownError] 携带的原始 cause 会一并上报，便于追踪根因。
     */
    private fun reportToBugly(error: AppError) {
        BuglyManager.putCustomData("error_category", error.category.name)
        val reportMessage = "[${error.category.displayName}] ${error.localizedDescription}"
        val cause = (error as? AppError.UnknownError)?.cause
        val exception = if (cause != null) Exception(reportMessage, cause) else Exception(reportMessage)
        BuglyManager.postException(exception)
    }

    /**
     * 写入错误历史（最新在前，超过上限自动裁剪）
     */
    private fun addToHistory(error: AppError) {
        val logged = LoggedError(error = error, timestamp = System.currentTimeMillis())
        _errorHistory.update { history ->
            (listOf(logged) + history).take(MAX_HISTORY_COUNT)
        }
    }
}
