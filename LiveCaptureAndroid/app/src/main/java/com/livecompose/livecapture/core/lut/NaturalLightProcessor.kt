package com.livecompose.livecapture.core.lut

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 自然光效果处理器
 * 使用 RAW 渲染管线风格处理预览和 JPG 输出
 * 不使用 DCP 胶片模拟，提供自然柔和的色彩表现
 */
class NaturalLightProcessor {

    /**
     * 应用自然光效果
     * 特点：
     * 1. 柔和的高光过渡
     * 2. 温暖的肤色表现
     * 3. 细腻的阴影层次
     */
    suspend fun apply(
        source: Bitmap,
        warmth: Float = 3f,       // 色温偏移 -20 ~ +20
        shadowLift: Float = 0.05f, // 阴影提亮 0 ~ 0.2
        highlightRollOff: Float = 0.15f, // 高光过渡 0 ~ 0.3
        onProgress: (Float) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val outputPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = ((pixel shr 16) and 0xFF) / 255f
            var g = ((pixel shr 8) and 0xFF) / 255f
            var b = (pixel and 0xFF) / 255f

            // 1. 色温偏移（温暖自然光）
            if (warmth != 0f) {
                val shift = warmth / 100f
                r = (r + shift * 0.04f).coerceIn(0f, 1f)
                g = (g + shift * 0.01f).coerceIn(0f, 1f)
                b = (b - shift * 0.03f).coerceIn(0f, 1f)
            }

            // 2. 高光过渡 (Filmic Tone Mapping 简化版)
            // 使用 S 曲线平滑高光
            if (highlightRollOff > 0f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > 0.7f) {
                    val excess = (lum - 0.7f) / 0.3f
                    val rolloff = 1f - excess * highlightRollOff
                    val factor = (1f - rolloff * (1f - (1f - excess).pow(2f)))
                    r = (r * (1f - excess) + r * factor * excess).coerceIn(0f, 1f)
                    g = (g * (1f - excess) + g * factor * excess).coerceIn(0f, 1f)
                    b = (b * (1f - excess) + b * factor * excess).coerceIn(0f, 1f)
                }
            }

            // 3. 阴影提亮
            if (shadowLift > 0f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val shadowMask = (0.3f - lum).coerceIn(0f, 0.3f) / 0.3f
                r = (r + shadowLift * shadowMask).coerceIn(0f, 1f)
                g = (g + shadowLift * shadowMask).coerceIn(0f, 1f)
                b = (b + shadowLift * shadowMask).coerceIn(0f, 1f)
            }

            // 4. 轻微的胶片曲线（S 曲线中段增强）
            r = applySCurve(r, 0.05f)
            g = applySCurve(g, 0.05f)
            b = applySCurve(b, 0.05f)

            val outR = (r * 255f).roundToInt().coerceIn(0, 255)
            val outG = (g * 255f).roundToInt().coerceIn(0, 255)
            val outB = (b * 255f).roundToInt().coerceIn(0, 255)
            outputPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB

            if (i % (width * 30) == 0) onProgress(i.toFloat() / pixels.size)
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outputPixels, 0, width, 0, 0, width, height)
        onProgress(1f)
        result
    }

    /** S 曲线：中间调增强，高光阴影柔化 */
    private fun applySCurve(value: Float, strength: Float): Float {
        // 简单 S 曲线：使用 sigmoid 函数
        val centered = value * 2f - 1f
        val curved = centered + centered.pow(3f) * strength
        return ((curved + 1f) / 2f).coerceIn(0f, 1f)
    }
}
