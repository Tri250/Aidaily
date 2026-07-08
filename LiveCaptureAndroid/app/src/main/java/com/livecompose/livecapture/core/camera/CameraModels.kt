package com.livecompose.livecapture.core.camera

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