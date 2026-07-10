package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 色彩平衡处理器
 *
 * 支持两种色彩调整：
 * - warm_cool: 冷暖色温 (蓝←→黄, -100~+100)
 * - cyan_magenta: 青品色调 (青←→品, -100~+100)
 *
 * 使用 Kelvin 色温映射 + 色调偏移实现。
 *
 * 对应 OMaster 预设参数:
 * - warm_cool (-80~+100)
 * - cyan_magenta (-39~+28)
 */
class ColorBalanceProcessor {

    /**
     * 应用色彩平衡
     *
     * @param bitmap 原始图像
     * @param warmCool 冷暖色温 -100~+100 (负=冷/蓝, 正=暖/黄)
     * @param cyanMagenta 青品色调 -100~+100 (负=青, 正=品)
     * @return 调整后的 Bitmap
     */
    suspend fun apply(
        bitmap: Bitmap,
        warmCool: Float = 0f,
        cyanMagenta: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (warmCool == 0f && cyanMagenta == 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        // 归一化
        val wcStrength = (warmCool / 100f).coerceIn(-1f, 1f)
        val cmStrength = (cyanMagenta / 100f).coerceIn(-1f, 1f)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF).toFloat()
            val g = ((p shr 8) and 0xFF).toFloat()
            val b = (p and 0xFF).toFloat()

            var adjustedR = r
            var adjustedG = g
            var adjustedB = b

            // 冷暖色温调整
            if (wcStrength != 0f) {
                if (wcStrength > 0f) {
                    // 暖色：增加 R 和 G（= 黄），减少 B
                    adjustedR = (r + wcStrength * 30f).coerceIn(0f, 255f)
                    adjustedG = (g + wcStrength * 15f).coerceIn(0f, 255f)
                    adjustedB = (b - wcStrength * 30f).coerceIn(0f, 255f)
                } else {
                    // 冷色：增加 B，减少 R 和 G
                    val coldStrength = -wcStrength
                    adjustedR = (r - coldStrength * 25f).coerceIn(0f, 255f)
                    adjustedG = (g - coldStrength * 10f).coerceIn(0f, 255f)
                    adjustedB = (b + coldStrength * 35f).coerceIn(0f, 255f)
                }
            }

            // 青品色调调整
            if (cmStrength != 0f) {
                if (cmStrength > 0f) {
                    // 品色：增加 R 和 B
                    adjustedR = (adjustedR + cmStrength * 20f).coerceIn(0f, 255f)
                    adjustedB = (adjustedB + cmStrength * 20f).coerceIn(0f, 255f)
                    adjustedG = (adjustedG - cmStrength * 15f).coerceIn(0f, 255f)
                } else {
                    // 青色：增加 G 和 B
                    val cyanStrength = -cmStrength
                    adjustedG = (adjustedG + cyanStrength * 20f).coerceIn(0f, 255f)
                    adjustedB = (adjustedB + cyanStrength * 20f).coerceIn(0f, 255f)
                    adjustedR = (adjustedR - cyanStrength * 15f).coerceIn(0f, 255f)
                }
            }

            val ri = adjustedR.toInt()
            val gi = adjustedG.toInt()
            val bi = adjustedB.toInt()
            outPixels[i] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        output
    }
}