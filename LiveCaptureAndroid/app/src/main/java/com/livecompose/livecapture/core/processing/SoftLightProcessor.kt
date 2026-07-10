package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp

/**
 * 柔光处理器
 *
 * 模拟柔光镜效果，用于人像柔化和梦幻氛围营造。
 * 支持两种模式：
 * - SOFT: 柔和柔光，轻微高光溢出
 * - DREAMY: 梦幻柔光，强烈光晕扩散
 *
 * 原理：对原图做高斯模糊，然后与原图按一定比例混合
 */
class SoftLightProcessor {

    enum class SoftLightMode {
        NONE, SOFT, DREAMY
    }

    /**
     * 应用柔光效果
     *
     * @param bitmap 原始图像
     * @param mode 柔光模式
     * @param intensity 强度 0.0~1.0
     * @return 柔光后的 Bitmap
     */
    suspend fun apply(
        bitmap: Bitmap,
        mode: SoftLightMode = SoftLightMode.NONE,
        intensity: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (mode == SoftLightMode.NONE || intensity <= 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height

        // 根据模式选择模糊半径
        val blurRadius = when (mode) {
            SoftLightMode.SOFT -> 8f * intensity
            SoftLightMode.DREAMY -> 16f * intensity
            SoftLightMode.NONE -> 0f
        }

        // 高斯模糊
        val blurred = gaussianBlur(bitmap, blurRadius.coerceAtLeast(1f))

        // 混合原图与模糊图
        val blendFactor = when (mode) {
            SoftLightMode.SOFT -> 0.25f * intensity
            SoftLightMode.DREAMY -> 0.45f * intensity
            SoftLightMode.NONE -> 0f
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        // 绘制原图
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // 叠加模糊图
        paint.alpha = (blendFactor * 255f).toInt().coerceIn(0, 255)
        canvas.drawBitmap(blurred, 0f, 0f, paint)

        blurred.recycle()
        output
    }

    private fun gaussianBlur(bitmap: Bitmap, sigma: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val radius = (sigma * 2f).toInt().coerceAtLeast(1)
        val kernel = createGaussianKernel(sigma, radius)

        // 分离通道
        val r = FloatArray(pixels.size) { ((pixels[it] shr 16) and 0xFF) / 255f }
        val g = FloatArray(pixels.size) { ((pixels[it] shr 8) and 0xFF) / 255f }
        val b = FloatArray(pixels.size) { (pixels[it] and 0xFF) / 255f }

        // 水平模糊
        val tempR = FloatArray(pixels.size)
        val tempG = FloatArray(pixels.size)
        val tempB = FloatArray(pixels.size)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var sr = 0f; var sg = 0f; var sb = 0f; var wsum = 0f
                for (dx in -radius..radius) {
                    val sx = (x + dx).coerceIn(0, width - 1)
                    val w = kernel[dx + radius]
                    val idx = y * width + sx
                    sr += r[idx] * w; sg += g[idx] * w; sb += b[idx] * w
                    wsum += w
                }
                val idx = y * width + x
                tempR[idx] = sr / wsum
                tempG[idx] = sg / wsum
                tempB[idx] = sb / wsum
            }
        }

        // 垂直模糊
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(pixels.size)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var sr = 0f; var sg = 0f; var sb = 0f; var wsum = 0f
                for (dy in -radius..radius) {
                    val sy = (y + dy).coerceIn(0, height - 1)
                    val w = kernel[dy + radius]
                    val idx = sy * width + x
                    sr += tempR[idx] * w; sg += tempG[idx] * w; sb += tempB[idx] * w
                    wsum += w
                }
                val ri = (sr / wsum * 255f).toInt().coerceIn(0, 255)
                val gi = (sg / wsum * 255f).toInt().coerceIn(0, 255)
                val bi = (sb / wsum * 255f).toInt().coerceIn(0, 255)
                outPixels[y * width + x] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }

        result.setPixels(outPixels, 0, width, 0, 0, width, height)
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
        for (i in kernel.indices) {
            kernel[i] /= sum
        }
        return kernel
    }
}