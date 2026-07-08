package com.livecompose.livecapture.core.editing

import android.graphics.Bitmap
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * HSL 颜色通道
 *
 * 对应 iOS 端 HSLChannel，提供 8 个独立可调的颜色通道。
 * @property name 中文名称
 * @property index 对应 hue/saturation/lightness 数组的索引（0~7）
 * @property hueCenter 该颜色在 HSV 色相环上的中心角度（0~360）
 * @property argb 用于 UI 显示的代表色（ARGB int）
 */
enum class HslChannel(val name: String, val index: Int, val hueCenter: Float, val argb: Int) {
    RED("红", 0, 0f, 0xFFFF1A1A.toInt()),
    ORANGE("橙", 1, 30f, 0xFFFF8000.toInt()),
    YELLOW("黄", 2, 60f, 0xFFFFFF00.toInt()),
    GREEN("绿", 3, 120f, 0xFF00CC33.toInt()),
    CYAN("青", 4, 180f, 0xFF00CCCC.toInt()),
    BLUE("蓝", 5, 240f, 0xFF1A4DFF.toInt()),
    PURPLE("紫", 6, 270f, 0xFF991AFF.toInt()),
    MAGENTA("洋红", 7, 300f, 0xFFFF1A99.toInt());

    companion object {
        /** 所有通道的色相中心角度，用于加权计算 */
        val hueCenters: FloatArray = FloatArray(entries.size) { entries[it].hueCenter }
    }
}

/**
 * HSL 调整参数
 *
 * 对应 iOS 端 viewModel.applyHSL 接收的三个长度为 8 的数组。
 * - hue：色相偏移，范围 -0.5~0.5（归一化，1.0 对应 360°）
 * - saturation：饱和度调整，范围 -1~1
 * - lightness：明度调整，范围 -1~1
 *
 * @property hue 长度为 8 的色相偏移数组
 * @property saturation 长度为 8 的饱和度调整数组
 * @property lightness 长度为 8 的明度调整数组
 */
data class HslParams(
    val hue: FloatArray = FloatArray(8),
    val saturation: FloatArray = FloatArray(8),
    val lightness: FloatArray = FloatArray(8)
) {
    /** 所有值均为 0 时视为默认（无调整） */
    val isDefault: Boolean
        get() = hue.all { it == 0f } &&
                saturation.all { it == 0f } &&
                lightness.all { it == 0f }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HslParams) return false
        return hue.contentEquals(other.hue) &&
                saturation.contentEquals(other.saturation) &&
                lightness.contentEquals(other.lightness)
    }

    override fun hashCode(): Int {
        var result = hue.contentHashCode()
        result = 31 * result + saturation.contentHashCode()
        result = 31 * result + lightness.contentHashCode()
        return result
    }
}

/**
 * HSL 颜色处理器
 *
 * 对应 iOS 端 HSLAdjustView 的 HSL 应用逻辑，支持对 8 种颜色独立调整色相/饱和度/明度。
 *
 * ## 处理流程
 * 1. 每个像素 RGB→HSV 转换
 * 2. 根据色相计算对 8 个颜色通道的归一化权重（三角形衰减，60° 半宽）
 * 3. 加权累加各通道的色相/饱和度/明度调整量
 * 4. HSV→RGB 转回，写回像素
 *
 * ## 注意
 * iOS 端使用 HSL 命名，实际调整在 HSV 空间完成（Android Color.RGBToHSV），
 * 视觉效果与 iOS 保持一致。
 */
class HslProcessor {

    companion object {
        private const val TAG = "HslProcessor"
        /** 权重衰减半宽（度），超出该距离的通道权重为 0 */
        private const val WEIGHT_HALF_WIDTH = 60f
    }

    /**
     * 应用 HSL 调整到 Bitmap
     *
     * @param bitmap 输入 Bitmap
     * @param params HSL 参数
     * @return 调整后的 Bitmap；参数为默认时返回新拷贝
     */
    suspend fun process(bitmap: Bitmap, params: HslParams): Bitmap = withContext(Dispatchers.Default) {
        if (params.isDefault) {
            return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val hueCenters = HslChannel.hueCenters
            val hueShifts = params.hue
            val satShifts = params.saturation
            val lightShifts = params.lightness
            val channelCount = hueCenters.size

            // 预分配复用缓冲区，避免热循环中频繁分配
            val hsv = FloatArray(3)
            val outRgb = IntArray(3)
            val weights = FloatArray(channelCount)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f

                rgbToHsv(r, g, b, hsv)
                val h = hsv[0] // 0~360
                val s = hsv[1] // 0~1
                val v = hsv[2] // 0~1

                // 近乎无色彩的像素跳过色相/饱和度调整（仍可应用轻微明度）
                if (s < 0.01f) {
                    hsvToRgb(h, s, v, outRgb)
                    pixels[i] = (0xFF shl 24) or
                            (outRgb[0] shl 16) or (outRgb[1] shl 8) or outRgb[2]
                    continue
                }

                // 计算各通道权重并归一化（复用预分配的 weights 数组）
                var weightSum = 0f
                for (c in 0 until channelCount) {
                    val d = circularDistance(h, hueCenters[c])
                    val w = if (d < WEIGHT_HALF_WIDTH) (1f - d / WEIGHT_HALF_WIDTH) else 0f
                    weights[c] = w
                    weightSum += w
                }
                if (weightSum > 0f) {
                    for (c in 0 until channelCount) weights[c] /= weightSum
                }

                // 加权累加调整量
                var hueShiftDeg = 0f
                var satMul = 1f
                var lightMul = 1f
                for (c in 0 until channelCount) {
                    val w = weights[c]
                    if (w <= 0f) continue
                    hueShiftDeg += w * hueShifts[c] * 360f
                    satMul += w * satShifts[c]
                    lightMul += w * lightShifts[c]
                }

                val newH = ((h + hueShiftDeg) % 360f + 360f) % 360f
                val newS = (s * satMul).coerceIn(0f, 1f)
                val newV = (v * lightMul).coerceIn(0f, 1f)

                hsvToRgb(newH, newS, newV, outRgb)
                pixels[i] = (0xFF shl 24) or
                        (outRgb[0] shl 16) or (outRgb[1] shl 8) or outRgb[2]
            }

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
        } catch (e: OutOfMemoryError) {
            AppLogger.e(TAG, "HSL 处理内存不足", e)
            throw RuntimeException("HSL 处理内存不足，请尝试降低图像分辨率", e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "HSL 处理失败", e)
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * 计算两个色相在色相环上的最短角度距离（0~180）
     */
    private fun circularDistance(a: Float, b: Float): Float {
        var d = abs(a - b) % 360f
        if (d > 180f) d = 360f - d
        return d
    }

    /**
     * RGB（0~1）转 HSV
     * @param hsv 输出数组：[h(0~360), s(0~1), v(0~1)]
     */
    private fun rgbToHsv(r: Float, g: Float, b: Float, hsv: FloatArray) {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        hsv[2] = max
        hsv[1] = if (max > 0f) delta / max else 0f
        hsv[0] = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        if (hsv[0] < 0f) hsv[0] += 360f
    }

    /**
     * HSV 转 RGB（0~255 整数）
     *
     * 将结果写入 [out] 复用缓冲区，避免热循环中分配新数组造成 GC 压力。
     *
     * @param out 长度为 3 的输出数组：[r, g, b]
     */
    private fun hsvToRgb(h: Float, s: Float, v: Float, out: IntArray) {
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val r1: Float
        val g1: Float
        val b1: Float
        when {
            h < 60f -> { r1 = c; g1 = x; b1 = 0f }
            h < 120f -> { r1 = x; g1 = c; b1 = 0f }
            h < 180f -> { r1 = 0f; g1 = c; b1 = x }
            h < 240f -> { r1 = 0f; g1 = x; b1 = c }
            h < 300f -> { r1 = x; g1 = 0f; b1 = c }
            else -> { r1 = c; g1 = 0f; b1 = x }
        }
        out[0] = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        out[1] = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        out[2] = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
    }
}
