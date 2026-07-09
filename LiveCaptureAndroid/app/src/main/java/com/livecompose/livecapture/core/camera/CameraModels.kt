package com.livecompose.livecapture.core.camera

import kotlin.math.roundToInt

/**
 * 镜头类型
 */
enum class LensKind(
    val approximateFocalLength: Int,
    val opticalZoomFactor: Float,
    val displayName: String
) {
    ULTRA_WIDE(13, 0.5f, "0.5×"),
    WIDE(24, 1.0f, "1×"),
    TELEPHOTO(77, 3.0f, "3×"),
    FRONT(24, 1.0f, "1×")
}

/**
 * 变焦预设
 */
data class ZoomPreset(
    val lens: LensKind,
    val zoomFactor: Float,
    val focalLength: Int,
    val style: PresetStyle = PresetStyle.SECONDARY
) {
    enum class PresetStyle { PRIMARY, SECONDARY }

    val label: String
        get() {
            val rounded = (zoomFactor * 10).roundToInt() / 10f
            return if (rounded == rounded.toInt().toFloat()) "${rounded.toInt()}×"
            else "%.1f×".format(rounded)
        }

    val focalLengthLabel: String get() = "${focalLength}mm"
}

/**
 * 变焦状态
 */
data class ZoomState(
    val currentFactor: Float = 1.0f,
    val displayedFactor: Float = 1.0f,
    val focalLength: Int = 24,
    val activeLens: LensKind = LensKind.WIDE,
    val isContinuous: Boolean = false
)

/**
 * 相机错误
 */
sealed class CameraError : Exception() {
    data object CameraUnavailable : CameraError()
    data object CannotAddInput : CameraError()
    data object CannotAddOutput : CameraError()
    data object PhotoDataMissing : CameraError()
    data object SaveFailed : CameraError()
    data object NotAuthorized : CameraError()
}

/**
 * 相机错误类型（用于 UI 状态）
 */
enum class CameraErrorType {
    PERMISSION_DENIED,
    CAMERA_IN_USE,
    NO_CAMERA_HARDWARE,
    CAMERA_DISCONNECTED,
    SESSION_CONFIG_FAILED,
    UNKNOWN
}

/**
 * 闪光灯模式
 */
enum class FlashMode(val displayName: String) {
    OFF("关闭"),
    AUTO("自动"),
    ON("打开"),
    TORCH("常亮")
}

/**
 * 对焦状态
 */
enum class FocusState {
    IDLE,
    FOCUSING,
    FOCUSED,
    FAILED
}

/**
 * 拍摄比例
 */
enum class AspectRatio(val displayName: String, val ratio: Float) {
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_9_16("9:16", 9f / 16f),
    RATIO_1_1("1:1", 1f),
    RATIO_FULL("全屏", 0f) // 0 = 跟随屏幕
}