package com.livecompose.livecapture.core.lut

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * OKLCH 色彩空间转换器
 * 基于 Björn Ottosson 2020 年提出的 Oklab 感知均匀色彩空间
 * 参考 PhotonCamera OklchConverter 实现
 */
object OklchConverter {

    // sRGB → Linear sRGB (IEC 61966-2-1 去伽马)
    fun srgbToLinear(c: Float): Float =
        if (c <= 0.04045f) c / 12.92f
        else ((c + 0.055f) / 1.055f).pow(2.4f)

    // Linear sRGB → sRGB (伽马编码)
    fun linearToSrgb(c: Float): Float =
        if (c <= 0.0031308f) 12.92f * c
        else 1.055f * c.pow(1.0f / 2.4f) - 0.055f

    // Linear sRGB → Oklab
    fun linearRgbToOklab(r: Float, g: Float, b: Float): FloatArray {
        // Step 1: Linear RGB → LMS (矩阵 M1)
        val l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
        val m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
        val s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b

        // Step 2: 立方根非线性压缩
        val l_ = if (l > 0f) cbrt(l) else -cbrt(-l)
        val m_ = if (m > 0f) cbrt(m) else -cbrt(-m)
        val s_ = if (s > 0f) cbrt(s) else -cbrt(-s)

        // Step 3: LMS → Oklab (矩阵 M2)
        val L = 0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_
        val a = 1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_
        val bOut = 0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_

        return floatArrayOf(L, a, bOut)
    }

    // Oklab → Linear sRGB
    fun oklabToLinearRgb(L: Float, a: Float, b: Float): FloatArray {
        // Step 1: Oklab → LMS'
        val l_ = L + 0.3963377774f * a + 0.2158037573f * b
        val m_ = L - 0.1055613458f * a - 0.0638541728f * b
        val s_ = L - 0.0894841775f * a - 1.2914855480f * b

        // Step 2: 立方还原
        val l = l_ * l_ * l_
        val m = m_ * m_ * m_
        val s = s_ * s_ * s_

        // Step 3: LMS → Linear RGB (逆矩阵 M1⁻¹)
        val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
        val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
        val bOut = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s

        return floatArrayOf(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), bOut.coerceIn(0f, 1f))
    }

    // Oklab → OKLCH (极坐标转换)
    fun oklabToOklch(L: Float, a: Float, b: Float): FloatArray {
        val C = sqrt(a * a + b * b)
        val H = Math.toDegrees(atan2(b.toDouble(), a.toDouble())).toFloat().let {
            if (it < 0) it + 360f else it
        }
        return floatArrayOf(L, C, H)
    }

    // OKLCH → Oklab (直角坐标还原)
    fun oklchToOklab(L: Float, C: Float, H: Float): FloatArray {
        val hRad = Math.toRadians(H.toDouble())
        val a = C * kotlin.math.cos(hRad).toFloat()
        val b = kotlin.math.sin(hRad).toFloat() * C
        return floatArrayOf(L, a, b)
    }

    // 便捷方法: sRGB [0,1]³ → OKLCH
    fun srgbToOklch(r: Float, g: Float, b: Float): FloatArray {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)
        val lab = linearRgbToOklab(linR, linG, linB)
        return oklabToOklch(lab[0], lab[1], lab[2])
    }

    // 便捷方法: OKLCH → sRGB [0,1]³
    fun oklchToSrgb(L: Float, C: Float, H: Float): FloatArray {
        val lab = oklchToOklab(L, C, H)
        val rgb = oklabToLinearRgb(lab[0], lab[1], lab[2])
        return floatArrayOf(
            linearToSrgb(rgb[0]),
            linearToSrgb(rgb[1]),
            linearToSrgb(rgb[2])
        )
    }
}
