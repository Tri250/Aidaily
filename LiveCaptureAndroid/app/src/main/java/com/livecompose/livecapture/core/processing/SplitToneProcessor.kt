package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

/**
 * 色调分离处理器
 * 分别对高光和阴影区域着色，模拟 Lightroom 的 Split Toning 效果
 */
class SplitToneProcessor {

    /**
     * 应用色调分离效果
     *
     * @param bitmap 原始图像
     * @param highlightColor 高光区域着色 (ARGB, 仅使用 RGB 通道)
     * @param shadowColor 阴影区域着色 (ARGB, 仅使用 RGB 通道)
     * @param balance 平衡值 (-100 到 100, 负值偏阴影、正值偏高光、0 为平衡)
     * @return 着色后的 Bitmap
     */
    suspend fun applySplitTone(
        bitmap: Bitmap,
        highlightColor: Int = 0xFFFFCC00L.toInt(),
        shadowColor: Int = 0xFF0066CCL.toInt(),
        balance: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val highlightR = ((highlightColor shr 16) and 0xFF) / 255f
        val highlightG = ((highlightColor shr 8) and 0xFF) / 255f
        val highlightB = (highlightColor and 0xFF) / 255f

        val shadowR = ((shadowColor shr 16) and 0xFF) / 255f
        val shadowG = ((shadowColor shr 8) and 0xFF) / 255f
        val shadowB = (shadowColor and 0xFF) / 255f

        // 将 balance 从 [-100, 100] 映射到 [0, 1]
        val balanceFactor = (balance / 100f + 1f) / 2f

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            // 计算亮度
            val luminance = 0.299f * r + 0.587f * g + 0.114f * b

            // 高光权重: 亮度 > 50% 时生效
            val highlightWeight = if (luminance > 0.5f) {
                ((luminance - 0.5f) / 0.5f).coerceIn(0f, 1f)
            } else 0f

            // 阴影权重: 亮度 < 50% 时生效
            val shadowWeight = if (luminance < 0.5f) {
                ((0.5f - luminance) / 0.5f).coerceIn(0f, 1f)
            } else 0f

            // 平衡混合: 在中间调区域平滑过渡
            val adjustedHighlightWeight = highlightWeight * balanceFactor
            val adjustedShadowWeight = shadowWeight * (1f - balanceFactor)

            // 对像素应用着色（乘法混合）
            val mixedHighlightWeight = adjustedHighlightWeight * 0.5f
            val mixedShadowWeight = adjustedShadowWeight * 0.5f

            val outR = (r * (1f - mixedHighlightWeight - mixedShadowWeight) +
                    r * highlightR * mixedHighlightWeight +
                    r * shadowR * mixedShadowWeight).coerceIn(0f, 1f)
            val outG = (g * (1f - mixedHighlightWeight - mixedShadowWeight) +
                    g * highlightG * mixedHighlightWeight +
                    g * shadowG * mixedShadowWeight).coerceIn(0f, 1f)
            val outB = (b * (1f - mixedHighlightWeight - mixedShadowWeight) +
                    b * highlightB * mixedHighlightWeight +
                    b * shadowB * mixedShadowWeight).coerceIn(0f, 1f)

            val outRi = (outR * 255f).toInt()
            val outGi = (outG * 255f).toInt()
            val outBi = (outB * 255f).toInt()
            outputPixels[i] = (0xFF shl 24) or (outRi shl 16) or (outGi shl 8) or outBi
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }
}