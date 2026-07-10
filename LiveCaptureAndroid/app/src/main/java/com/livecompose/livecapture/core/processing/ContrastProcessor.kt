package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

/**
 * 对比度处理器
 *
 * 支持三种对比度调整：
 * - global: 全局对比度（S 曲线）
 * - highlight: 高光对比度（仅影响亮度 > 50% 的区域）
 * - shadow: 阴影对比度（仅影响亮度 < 50% 的区域）
 *
 * 对应 OMaster 预设参数:
 * - contrast (-100~+100)
 * - contrast_highlight (-100~+100)
 * - contrast_shadow (-100~+100)
 */
class ContrastProcessor {

    /**
     * 应用对比度调整
     *
     * @param bitmap 原始图像
     * @param global 全局对比度 -100~+100
     * @param highlight 高光对比度 -100~+100
     * @param shadow 阴影对比度 -100~+100
     * @return 调整后的 Bitmap
     */
    suspend fun apply(
        bitmap: Bitmap,
        global: Float = 0f,
        highlight: Float = 0f,
        shadow: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (global == 0f && highlight == 0f && shadow == 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            // 计算亮度
            val luminance = 0.299f * r + 0.587f * g + 0.114f * b

            // 全局对比度 (S 曲线)
            var adjustedLuminance = luminance
            if (global != 0f) {
                val contrastFactor = (global / 100f).coerceIn(-1f, 1f)
                adjustedLuminance = applyScurve(luminance, contrastFactor)
            }

            // 高光对比度
            if (highlight != 0f && luminance > 0.5f) {
                val hFactor = (highlight / 100f).coerceIn(-1f, 1f)
                val hWeight = ((luminance - 0.5f) / 0.5f).coerceIn(0f, 1f)
                val hAdjusted = applyScurve(luminance, hFactor)
                adjustedLuminance = luminance + (hAdjusted - luminance) * hWeight
            }

            // 阴影对比度
            if (shadow != 0f && luminance < 0.5f) {
                val sFactor = (shadow / 100f).coerceIn(-1f, 1f)
                val sWeight = ((0.5f - luminance) / 0.5f).coerceIn(0f, 1f)
                val sAdjusted = applyScurve(luminance, sFactor)
                adjustedLuminance = luminance + (sAdjusted - luminance) * sWeight
            }

            // 将亮度变化应用到 RGB
            val luminanceDelta = adjustedLuminance - luminance
            val finalR = (r + luminanceDelta).coerceIn(0f, 1f)
            val finalG = (g + luminanceDelta).coerceIn(0f, 1f)
            val finalB = (b + luminanceDelta).coerceIn(0f, 1f)

            val ri = (finalR * 255f).toInt()
            val gi = (finalG * 255f).toInt()
            val bi = (finalB * 255f).toInt()
            outPixels[i] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * S 曲线对比度调整
     *
     * 正对比度：暗部更暗，亮部更亮（S 型）
     * 负对比度：暗部变亮，亮部变暗（反 S 型）
     *
     * @param value 归一化亮度 0~1
     * @param amount 对比度强度 -1~+1
     */
    private fun applyScurve(value: Float, amount: Float): Float {
        if (amount == 0f) return value

        // 使用三次多项式模拟 S 曲线
        val centered = value - 0.5f
        return if (amount > 0f) {
            // 正对比度：增强极值
            val sign = if (centered > 0f) 1f else -1f
            (0.5f + sign * (kotlin.math.abs(centered) * 2f).toDouble().pow((1f / (1f + amount * 0.5f)).toDouble()).toFloat() * 0.5f).coerceIn(0f, 1f)
        } else {
            // 负对比度：向中间调靠拢
            val absAmount = kotlin.math.abs(amount)
            (0.5f + centered * (1f - absAmount * 0.6f)).coerceIn(0f, 1f)
        }
    }
}