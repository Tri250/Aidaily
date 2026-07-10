package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp

/**
 * 清晰度处理器
 *
 * 通过增强中频带（中间调）局部对比度来提升图像清晰度。
 * 与锐度不同，清晰度不增强边缘，而是增强纹理和中间调细节。
 *
 * 算法：使用大半径 USM 提取中频带细节，然后叠加回原图。
 *
 * 对应 OMaster 预设参数: clarity (-10~+10)
 */
class ClarityProcessor {

    /**
     * 应用清晰度调整
     *
     * @param bitmap 原始图像
     * @param strength 清晰度强度 -10~+10
     * @return 调整后的 Bitmap
     */
    suspend fun apply(
        bitmap: Bitmap,
        strength: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        val clampedStrength = (strength / 10f).coerceIn(-1f, 1f)
        if (abs(clampedStrength) < 0.001f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 提取亮度通道
        val luminance = FloatArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            luminance[i] = (0.299f * ((p shr 16) and 0xFF) +
                    0.587f * ((p shr 8) and 0xFF) +
                    0.114f * (p and 0xFF)) / 255f
        }

        // 大半径高斯模糊提取低频
        val lowFreq = gaussianBlur(luminance, width, height, 15f)

        // 小半径高斯模糊提取中频
        val midFreq = gaussianBlur(luminance, width, height, 3f)

        // 中频细节 = 中频模糊 - 低频模糊
        val detail = FloatArray(width * height)
        for (i in detail.indices) {
            detail[i] = midFreq[i] - lowFreq[i]
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            // 将中频细节叠加到 RGB 通道
            val adjust = detail[i] * clampedStrength * 0.8f
            val outR = (r + adjust).coerceIn(0f, 1f)
            val outG = (g + adjust).coerceIn(0f, 1f)
            val outB = (b + adjust).coerceIn(0f, 1f)

            val ri = (outR * 255f).toInt()
            val gi = (outG * 255f).toInt()
            val bi = (outB * 255f).toInt()
            outPixels[i] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        output
    }

    private fun gaussianBlur(data: FloatArray, width: Int, height: Int, sigma: Float): FloatArray {
        val result = data.copyOf()
        val radius = (sigma * 1.5f).toInt().coerceAtLeast(1)
        val kernel = createGaussianKernel(sigma, radius)

        // 水平
        val temp = FloatArray(result.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var wSum = 0f
                for (dx in -radius..radius) {
                    val sx = (x + dx).coerceIn(0, width - 1)
                    val w = kernel[dx + radius]
                    sum += result[y * width + sx] * w
                    wSum += w
                }
                temp[y * width + x] = sum / wSum
            }
        }
        // 垂直
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sum = 0f
                var wSum = 0f
                for (dy in -radius..radius) {
                    val sy = (y + dy).coerceIn(0, height - 1)
                    val w = kernel[dy + radius]
                    sum += temp[sy * width + x] * w
                    wSum += w
                }
                result[y * width + x] = sum / wSum
            }
        }
        return result
    }

    private fun createGaussianKernel(sigma: Float, radius: Int): FloatArray {
        val size = radius * 2 + 1
        val kernel = FloatArray(size)
        val twoSigmaSq = 2f * sigma * sigma
        var sum = 0f
        for (i in -radius..radius) {
            kernel[i + radius] = exp(-(i * i).toFloat() / twoSigmaSq)
            sum += kernel[i + radius]
        }
        for (i in kernel.indices) kernel[i] /= sum
        return kernel
    }
}