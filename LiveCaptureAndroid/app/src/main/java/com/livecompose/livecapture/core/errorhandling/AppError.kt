package com.livecompose.livecapture.core.errorhandling

import java.util.UUID

/**
 * 错误类型分类
 *
 * 用于对 [AppError] 进行归类，决定弹窗标题与图标。
 */
enum class ErrorCategory {
    CAMERA,
    STORAGE,
    NETWORK,
    PROCESSING,
    PERMISSION,
    UNKNOWN;

    /** 用于在弹窗中展示的中文名称 */
    val displayName: String
        get() = when (this) {
            CAMERA -> "相机错误"
            STORAGE -> "存储错误"
            NETWORK -> "网络错误"
            PROCESSING -> "处理错误"
            PERMISSION -> "权限错误"
            UNKNOWN -> "未知错误"
        }
}

/** 相机错误码 */
enum class CameraErrorCode {
    CAMERA_UNAVAILABLE,
    CAPTURE_FAILED,
    FOCUS_FAILED,
    FLASH_FAILED,
    UNKNOWN
}

/** 存储错误码 */
enum class StorageErrorCode {
    DISK_FULL,
    SAVE_FAILED,
    FILE_NOT_FOUND,
    READ_FAILED,
    UNKNOWN
}

/** 网络错误码 */
enum class NetworkErrorCode {
    NO_CONNECTION,
    TIMEOUT,
    SERVER_ERROR,
    UNKNOWN
}

/** 处理错误码 */
enum class ProcessingErrorCode {
    FILTER_FAILED,
    EDIT_FAILED,
    EXPORT_FAILED,
    UNKNOWN
}

/**
 * 应用程序统一错误类型
 *
 * 对应 iOS 端 AppError 枚举。使用密封类对错误进行归类，
 * 每个子类型携带中文描述 [localizedDescription] 与恢复建议 [recoverySuggestion]，
 * 并通过 [category] 决定弹窗展示与上报分类。
 *
 * @param message 原始错误信息
 */
sealed class AppError(open val message: String) {

    /** 唯一标识（UUID 自动生成） */
    abstract val id: String

    /** 中文错误描述 */
    abstract val localizedDescription: String

    /** 恢复建议 */
    abstract val recoverySuggestion: String

    /** 错误类型分类 */
    abstract val category: ErrorCategory

    // MARK: - 相机错误

    /** 相机错误 */
    data class CameraError(
        override val message: String,
        val code: CameraErrorCode = CameraErrorCode.UNKNOWN
    ) : AppError(message) {
        override val id: String = UUID.randomUUID().toString()
        override val category: ErrorCategory = ErrorCategory.CAMERA

        override val localizedDescription: String = when (code) {
            CameraErrorCode.CAMERA_UNAVAILABLE -> "相机不可用"
            CameraErrorCode.CAPTURE_FAILED -> "拍照失败"
            CameraErrorCode.FOCUS_FAILED -> "对焦失败"
            CameraErrorCode.FLASH_FAILED -> "闪光灯失败"
            CameraErrorCode.UNKNOWN -> "相机错误：$message"
        }

        override val recoverySuggestion: String = when (code) {
            CameraErrorCode.CAMERA_UNAVAILABLE ->
                "请检查是否有其他应用正在使用相机，或尝试重启设备"
            CameraErrorCode.CAPTURE_FAILED ->
                "请检查相机是否正常运行，尝试切换镜头或重启应用"
            CameraErrorCode.FOCUS_FAILED ->
                "请清洁镜头并稍后重试，或尝试手动对焦"
            CameraErrorCode.FLASH_FAILED ->
                "请检查闪光灯是否被其他应用占用，或关闭闪光灯后重试"
            CameraErrorCode.UNKNOWN ->
                "请尝试重启应用，如果问题持续请检查设备摄像头是否正常"
        }
    }

    // MARK: - 存储错误

    /** 存储错误 */
    data class StorageError(
        override val message: String,
        val code: StorageErrorCode = StorageErrorCode.UNKNOWN
    ) : AppError(message) {
        override val id: String = UUID.randomUUID().toString()
        override val category: ErrorCategory = ErrorCategory.STORAGE

        override val localizedDescription: String = when (code) {
            StorageErrorCode.DISK_FULL -> "存储空间不足"
            StorageErrorCode.SAVE_FAILED -> "保存失败：$message"
            StorageErrorCode.FILE_NOT_FOUND -> "文件未找到：$message"
            StorageErrorCode.READ_FAILED -> "读取失败：$message"
            StorageErrorCode.UNKNOWN -> "存储错误：$message"
        }

        override val recoverySuggestion: String = when (code) {
            StorageErrorCode.DISK_FULL ->
                "设备存储空间不足，请清理一些不必要的文件后重试"
            StorageErrorCode.SAVE_FAILED ->
                "请检查设备存储空间是否充足，尝试清理一些不需要的照片"
            StorageErrorCode.FILE_NOT_FOUND ->
                "文件可能已被删除或移动，请尝试刷新列表"
            StorageErrorCode.READ_FAILED ->
                "数据可能已损坏，请尝试重新打开应用"
            StorageErrorCode.UNKNOWN ->
                "请检查设备存储权限与空间是否充足"
        }
    }

    // MARK: - 网络错误

    /** 网络错误 */
    data class NetworkError(
        override val message: String,
        val code: NetworkErrorCode = NetworkErrorCode.UNKNOWN
    ) : AppError(message) {
        override val id: String = UUID.randomUUID().toString()
        override val category: ErrorCategory = ErrorCategory.NETWORK

        override val localizedDescription: String = when (code) {
            NetworkErrorCode.NO_CONNECTION -> "网络不可用"
            NetworkErrorCode.TIMEOUT -> "网络请求超时"
            NetworkErrorCode.SERVER_ERROR -> "服务器错误：$message"
            NetworkErrorCode.UNKNOWN -> "网络错误：$message"
        }

        override val recoverySuggestion: String = when (code) {
            NetworkErrorCode.NO_CONNECTION ->
                "请检查网络连接，确保 Wi-Fi 或蜂窝数据已开启"
            NetworkErrorCode.TIMEOUT ->
                "网络响应较慢，请检查网络状态后重试"
            NetworkErrorCode.SERVER_ERROR ->
                "服务器暂时不可用，请稍后重试"
            NetworkErrorCode.UNKNOWN ->
                "请检查网络连接后重试"
        }
    }

    // MARK: - 处理错误

    /** 图像/视频处理错误 */
    data class ProcessingError(
        override val message: String,
        val code: ProcessingErrorCode = ProcessingErrorCode.UNKNOWN
    ) : AppError(message) {
        override val id: String = UUID.randomUUID().toString()
        override val category: ErrorCategory = ErrorCategory.PROCESSING

        override val localizedDescription: String = when (code) {
            ProcessingErrorCode.FILTER_FAILED -> "滤镜处理失败"
            ProcessingErrorCode.EDIT_FAILED -> "图像编辑失败：$message"
            ProcessingErrorCode.EXPORT_FAILED -> "导出失败"
            ProcessingErrorCode.UNKNOWN -> "处理失败：$message"
        }

        override val recoverySuggestion: String = when (code) {
            ProcessingErrorCode.FILTER_FAILED ->
                "滤镜处理失败，请尝试选择其他滤镜"
            ProcessingErrorCode.EDIT_FAILED ->
                "图像编辑失败，请尝试使用其他编辑工具"
            ProcessingErrorCode.EXPORT_FAILED ->
                "导出失败，请检查存储空间是否充足"
            ProcessingErrorCode.UNKNOWN ->
                "处理时间过长或资源不足，请尝试缩小图片尺寸后重试"
        }
    }

    // MARK: - 权限错误

    /** 权限错误 */
    data class PermissionError(
        override val message: String,
        val permission: String = ""
    ) : AppError(message) {
        override val id: String = UUID.randomUUID().toString()
        override val category: ErrorCategory = ErrorCategory.PERMISSION

        override val localizedDescription: String =
            if (permission.isBlank()) "权限被拒绝：$message"
            else "权限被拒绝：$permission"

        override val recoverySuggestion: String =
            "请在「设置」>「应用管理」>「LiveCapture」>「权限」中开启相应权限"
    }

    // MARK: - 未知错误

    /** 未知错误 */
    data class UnknownError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(message) {
        override val id: String = UUID.randomUUID().toString()
        override val category: ErrorCategory = ErrorCategory.UNKNOWN

        override val localizedDescription: String = "未知错误：$message"

        override val recoverySuggestion: String =
            "请尝试重启应用，如果问题持续请联系技术支持"
    }
}
