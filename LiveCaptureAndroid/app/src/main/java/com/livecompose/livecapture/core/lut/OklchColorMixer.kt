package com.livecompose.livecapture.core.lut

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * OKLCH 颜色混色器
 * 在 Oklab/OKLCH 感知均匀色彩空间中执行 9 通道颜色调整
 * 参考 PhotonCamera PreviewColorShaderModules LCH 混色器实现
 */
object OklchColorMixer {

    // 8 个色相频带中心角（弧度）
    private val HUE_CENTERS_RAD = LchColorAdjustment.HUE_CENTERS.map {
        Math.toRadians(it.toDouble()).toFloat()
    }.toFloatArray()

    // 彩度门控参数
    private const val CHROMA_GATE_LOW = 0.005f
    private const val CHROMA_GATE_HIGH = 0.03f

    // Density 效果参数
    private const val CHROMA_BIAS = 0.35f
    private const val DENSITY_K = 1.85f

    // 皮肤检测范围 (CIE-Lab)
    private val SKIN_RANGES = listOf(
        // (L_min, L_max, h_min, h_max, C_min, C_max, weight)
        floatArrayOf(20f, 45f, -0.1f, 1.2f, 8f, 40f, 1.0f),
        floatArrayOf(35f, 55f, 0.0f, 1.4f, 10f, 45f, 1.0f),
        floatArrayOf(45f, 65f, 0.1f, 1.5f, 10f, 50f, 0.8f),
        floatArrayOf(55f, 75f, 0.2f, 1.6f, 8f, 42f, 0.67f),
        floatArrayOf(65f, 85f, 0.3f, 1.6f, 7f, 35f, 0.5f),
        floatArrayOf(75f, 95f, 0.4f, 1.5f, 5f, 28f, 0.33f),
        // 扩展范围（深肤色/浅肤色过渡）
        floatArrayOf(30f, 50f, -0.2f, 1.4f, 12f, 48f, 0.67f),
        floatArrayOf(50f, 70f, 0.0f, 1.65f, 9f, 45f, 0.5f),
        floatArrayOf(70f, 100f, 0.5f, 1.4f, 5f, 25f, 0.33f)
    )

    /**
     * 对 Bitmap 应用 OKLCH 混色器
     * @param bitmap 输入图像
     * @param adjustment 9 通道 LCH 调整参数
     * @param density 色彩密度（Vibrance），0 = 无效果
     * @return 处理后的 Bitmap
     */
    fun applyMixer(
        bitmap: Bitmap,
        adjustment: LchColorAdjustment,
        density: Float = 0f
    ): Bitmap {
        if (!adjustment.hasAnyAdjustment && density == 0f) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val result = IntArray(pixels.size)

        for (i in pixels.indices) {
            val argb = pixels[i]
            val a = (argb shr 24) and 0xFF
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f

            val mixed = applyMixerToPixel(r, g, b, adjustment, density)

            val outR = (mixed[0] * 255f).toInt().coerceIn(0, 255)
            val outG = (mixed[1] * 255f).toInt().coerceIn(0, 255)
            val outB = (mixed[2] * 255f).toInt().coerceIn(0, 255)

            result[i] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * 对单个像素应用 OKLCH 混色器
     * 返回 [R, G, B] 在 [0, 1] 范围
     */
    fun applyMixerToPixel(
        r: Float, g: Float, b: Float,
        adjustment: LchColorAdjustment,
        density: Float = 0f
    ): FloatArray {
        // sRGB → Linear → Oklab
        val linR = OklchConverter.srgbToLinear(r)
        val linG = OklchConverter.srgbToLinear(g)
        val linB = OklchConverter.srgbToLinear(b)
        val lab = OklchConverter.linearRgbToOklab(linR, linG, linB)

        var L = lab[0]
        var aVal = lab[1]
        var bVal = lab[2]

        // 提取极坐标
        var chroma = sqrt(aVal * aVal + bVal * bVal)
        var hue = atan2(bVal.toDouble(), aVal.toDouble()).toFloat()

        // --- Density / Vibrance 效果 ---
        if (density != 0f) {
            val densityScale = max(0f, 1f + density * CHROMA_BIAS)
            val newChroma = chroma * densityScale
            val newLightness = (L * exp(-DENSITY_K * density * chroma)).coerceIn(0f, 1f)
            chroma = newChroma
            L = newLightness
        }

        // --- LCH 混色器 ---
        if (adjustment.hasAnyAdjustment) {
            var hueShift = 0f
            var chromaScale = 1f
            var lightnessShift = 0f
            var totalWeight = 0f

            // 8 个标准色相频带
            val bandWeights = FloatArray(8)
            for (i in 0 until 8) {
                bandWeights[i] = fullCoverageBandWeight(hue, HUE_CENTERS_RAD[i], chroma)
                totalWeight += bandWeights[i]
            }

            // 归一化加权
            if (totalWeight > 0.0001f) {
                for (i in 0 until 8) {
                    val w = bandWeights[i] / totalWeight
                    val channel = adjustment.getChannel(i + 1) // index 1-8
                    hueShift += channel[0] * w * Math.toRadians(20.0).toFloat()
                    chromaScale += channel[1] * w
                    lightnessShift += channel[2] * w * 0.15f
                }

                // 彩度门控
                val chromaGate = smoothStep(CHROMA_GATE_LOW, CHROMA_GATE_HIGH, chroma)
                hueShift *= chromaGate
                chromaScale = mix(1f, chromaScale, chromaGate)
                lightnessShift *= chromaGate
            }

            // 皮肤保护通道
            val skinWeight = skinBandWeight(linR, linG, linB)
            if (skinWeight > 0.0001f) {
                val skinCh = adjustment.getChannel(0)
                hueShift += skinCh[0] * skinWeight * Math.toRadians(10.0).toFloat()
                chromaScale += skinCh[1] * skinWeight
                lightnessShift += skinCh[2] * skinWeight * 0.08f
            }

            // 应用变换
            hue += hueShift
            chroma = (chroma * chromaScale).coerceAtLeast(0f)
            L = (L + lightnessShift).coerceIn(0f, 1f)
        }

        // OKLCH → Oklab → Linear → sRGB
        aVal = chroma * cos(hue.toDouble()).toFloat()
        bVal = chroma * sin(hue.toDouble()).toFloat()

        val rgb = OklchConverter.oklabToLinearRgb(L, aVal, bVal)
        return floatArrayOf(
            OklchConverter.linearToSrgb(rgb[0]),
            OklchConverter.linearToSrgb(rgb[1]),
            OklchConverter.linearToSrgb(rgb[2])
        )
    }

    /**
     * 全覆盖带权重函数
     */
    private fun fullCoverageBandWeight(hue: Float, center: Float, chroma: Float): Float {
        val dist = abs(wrapAngle(hue - center))
        val hueWeight = smoothStep(Math.toRadians(85.0).toFloat(), 0f, dist)
        val chromaWeight = smoothStep(CHROMA_GATE_LOW, CHROMA_GATE_HIGH, chroma)
        return hueWeight * chromaWeight
    }

    /**
     * 皮肤分类器权重
     */
    private fun skinBandWeight(r: Float, g: Float, b: Float): Float {
        // Linear RGB → XYZ → CIE-Lab
        val x = 0.4124564f * r + 0.3575761f * g + 0.1804375f * b
        val y = 0.2126729f * r + 0.7151522f * g + 0.0721750f * b
        val z = 0.0193339f * r + 0.1191920f * g + 0.9503041f * b

        // XYZ → Lab (D65 白点)
        val xn = 0.95047f; val yn = 1.0f; val zn = 1.08883f
        fun f(t: Float) = if (t > 0.008856f) cbrt(t.toDouble()).toFloat() else 7.787f * t + 16f / 116f
        val L = 116f * f(y / yn) - 16f
        val aVal = 500f * (f(x / xn) - f(y / yn))
        val bVal = 200f * (f(y / yn) - f(z / zn))

        val c = sqrt(aVal * aVal + bVal * bVal)
        val h = atan2(bVal.toDouble(), aVal.toDouble()).toFloat()

        var maxWeight = 0f
        for (range in SKIN_RANGES) {
            if (L >= range[0] && L <= range[1] &&
                h >= range[2] && h <= range[3] &&
                c >= range[4] && c <= range[5]) {
                maxWeight = max(maxWeight, range[6])
            }
        }
        return maxWeight
    }

    private fun wrapAngle(a: Float): Float {
        var angle = a
        while (angle > Math.PI.toFloat()) angle -= 2f * Math.PI.toFloat()
        while (angle < -Math.PI.toFloat()) angle += 2f * Math.PI.toFloat()
        return angle
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun mix(a: Float, b: Float, t: Float): Float = a * (1f - t) + b * t

    private fun cbrt(x: Double): Double = if (x >= 0) x.pow(1.0/3.0) else -(-x).pow(1.0/3.0)
}
