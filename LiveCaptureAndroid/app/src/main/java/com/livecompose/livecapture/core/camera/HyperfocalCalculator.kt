package com.livecompose.livecapture.core.camera

import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 超焦距计算器
 * 风景拍摄使用超焦距，使前后景都清晰
 */
object HyperfocalCalculator {

    // 常见传感器尺寸 (mm × mm)
    data class SensorSize(val width: Float, val height: Float, val cropFactor: Float)

    val COMMON_SENSORS = mapOf(
        "full_frame" to SensorSize(36f, 24f, 1.0f),
        "aps_c_nikon" to SensorSize(23.5f, 15.6f, 1.5f),
        "aps_c_canon" to SensorSize(22.3f, 14.9f, 1.6f),
        "m43" to SensorSize(17.3f, 13f, 2.0f),
        "one_inch" to SensorSize(13.2f, 8.8f, 2.7f),
        "phone_main" to SensorSize(7.6f, 5.7f, 4.7f),    // 手机主摄
        "phone_ultra" to SensorSize(9.6f, 7.2f, 3.75f),  // 手机超广角
        "phone_tele" to SensorSize(5.6f, 4.2f, 6.4f)      // 手机长焦
    )

    /**
     * 计算超焦距
     *
     * H = (f² / (N × c)) + f
     * 其中：
     *   f = 焦距 (mm)
     *   N = 光圈值 (f-number)
     *   c = 弥散圆直径 (mm)，通常为传感器对角线 / 1500
     *
     * @param focalLengthMm 焦距 (mm)
     * @param aperture 光圈值 (如 1.8, 2.8, 5.6)
     * @param sensorKey 传感器类型键名
     * @return 超焦距距离（米）
     */
    fun calculate(
        focalLengthMm: Float,
        aperture: Float,
        sensorKey: String = "phone_main"
    ): HyperfocalResult {
        val sensor = COMMON_SENSORS[sensorKey] ?: COMMON_SENSORS["phone_main"]!!
        return calculate(focalLengthMm, aperture, sensor)
    }

    fun calculate(
        focalLengthMm: Float,
        aperture: Float,
        sensor: SensorSize
    ): HyperfocalResult {
        val coc = circleOfConfusion(sensor)  // mm
        val f = focalLengthMm
        val N = aperture.coerceAtLeast(0.7f)

        // 超焦距 (mm)
        val hyperfocalMm = (f * f) / (N * coc) + f
        val hyperfocalM = hyperfocalMm / 1000f

        // 近景深限 = H × (s - f) / (H + s - 2f)
        // 对于 s = H (对焦在超焦距)，近景深限 = H/2
        val nearLimitM = hyperfocalM / 2f

        // 远景深限 = 无穷远（对焦在超焦距时）
        val farLimitM = Float.POSITIVE_INFINITY

        return HyperfocalResult(
            hyperfocalDistanceM = hyperfocalM,
            nearLimitM = nearLimitM,
            farLimitM = farLimitM,
            focalLengthMm = focalLengthMm,
            aperture = aperture,
            cocMm = coc,
            displayText = formatDistance(hyperfocalM),
            nearDisplayText = formatDistance(nearLimitM)
        )
    }

    /**
     * 计算弥散圆直径
     * 通常为传感器对角线 / 1500
     */
    private fun circleOfConfusion(sensor: SensorSize): Float {
        val diagonal = kotlin.math.sqrt(sensor.width * sensor.width + sensor.height * sensor.height)
        return diagonal / 1500f
    }

    /**
     * 根据变焦因子估算焦距
     * 手机主摄通常约 24mm 等效焦距
     */
    fun estimateFocalLength(zoomFactor: Float, baseFocalLengthMm: Float = 24f): Float {
        return baseFocalLengthMm * zoomFactor
    }

    private fun formatDistance(meters: Float): String {
        return when {
            meters < 1f -> "${(meters * 100).roundToInt()}cm"
            meters < 100f -> "${(meters * 10).roundToInt() / 10f}m"
            else -> "${meters.roundToInt()}m"
        }
    }
}

data class HyperfocalResult(
    val hyperfocalDistanceM: Float,   // 超焦距 (米)
    val nearLimitM: Float,            // 近景深限 (米)
    val farLimitM: Float,             // 远景深限 (米)
    val focalLengthMm: Float,         // 焦距 (mm)
    val aperture: Float,              // 光圈
    val cocMm: Float,                 // 弥散圆直径 (mm)
    val displayText: String,          // 超焦距显示文本
    val nearDisplayText: String       // 近景深限显示文本
)
