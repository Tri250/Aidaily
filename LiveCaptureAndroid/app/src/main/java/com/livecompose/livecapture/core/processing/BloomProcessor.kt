package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Bloom/柔光效果处理器
 * 高光扩散滤镜 (HDF)，模拟黑柔/白柔滤镜
 */
class BloomProcessor {

    /**
     * 应用 Bloom 效果
     * 原理：提取高光区域 → 模糊 → 与原图叠加
     *
     * @param source 原始图像
     * @param intensity 强度 0~1
     * @param threshold 高光阈值 0~1 (高于此值的像素参与扩散)
     * @param radius 扩散半径（像素）
     * @param onProgress 进度回调
     */
    suspend fun applyBloom(
        source: Bitmap,
        intensity: Float = 0.5f,
        threshold: Float = 0.7f,
        radius: Int = 8,
        onProgress: (Float) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        var output: Bitmap? = null
        try {
            if (intensity <= 0f) return@withContext source

            val width = source.width
            val height = source.height
            val pixels = IntArray(width * height)
            source.getPixels(pixels, 0, width, 0, 0, width, height)

            // 步骤 1: 提取高光
            val highlightPixels = FloatArray(width * height * 3)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                val highlightFactor = if (lum > threshold) {
                    ((lum - threshold) / (1f - threshold)).coerceIn(0f, 1f)
                } else 0f

                highlightPixels[i * 3] = r * highlightFactor
                highlightPixels[i * 3 + 1] = g * highlightFactor
                highlightPixels[i * 3 + 2] = b * highlightFactor
            }
            onProgress(0.3f)

            // 步骤 2: 简易盒式模糊 (多次传递近似高斯模糊)
            val blurred = boxBlur3Pass(highlightPixels, width, height, radius)
            onProgress(0.8f)

            // 步骤 3: 叠加到原图
            val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            output = outputBitmap
            val outputPixels = IntArray(width * height)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f

                val bloomR = r + blurred[i * 3] * intensity
                val bloomG = g + blurred[i * 3 + 1] * intensity
                val bloomB = b + blurred[i * 3 + 2] * intensity

                val outR = (bloomR.coerceIn(0f, 1f) * 255f).toInt()
                val outG = (bloomG.coerceIn(0f, 1f) * 255f).toInt()
                val outB = (bloomB.coerceIn(0f, 1f) * 255f).toInt()

                outputPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }

            outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
            onProgress(1f)
            outputBitmap
        } catch (e: OutOfMemoryError) {
            output?.recycle()
            throw RuntimeException("Bloom 效果处理内存不足，请尝试降低图像分辨率", e)
        } catch (e: Exception) {
            output?.recycle()
            throw e
        }
    }

    /**
     * 应用柔光效果
     * 全画面柔化+降低对比度，模拟白柔滤镜
     */
    suspend fun applySoftLight(
        source: Bitmap,
        intensity: Float = 0.3f,
        onProgress: (Float) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        var output: Bitmap? = null
        try {
            if (intensity <= 0f) return@withContext source

            val width = source.width
            val height = source.height
            val pixels = IntArray(width * height)
            source.getPixels(pixels, 0, width, 0, 0, width, height)

            // 提取 RGB
            val rgb = FloatArray(width * height * 3)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                rgb[i * 3] = ((pixel shr 16) and 0xFF) / 255f
                rgb[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255f
                rgb[i * 3 + 2] = (pixel and 0xFF) / 255f
            }

            // 模糊
            val blurred = boxBlur3Pass(rgb, width, height, 12)
            onProgress(0.7f)

            // Screen 混合：result = 1 - (1 - a) * (1 - b)
            val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            output = outputBitmap
            val outputPixels = IntArray(width * height)

            for (i in pixels.indices) {
                val r = rgb[i * 3]
                val g = rgb[i * 3 + 1]
                val b = rgb[i * 3 + 2]
                val br = blurred[i * 3]
                val bg = blurred[i * 3 + 1]
                val bb = blurred[i * 3 + 2]

                // Screen blend
                val mixedR = r * (1f - intensity) + (1f - (1f - r) * (1f - br)) * intensity
                val mixedG = g * (1f - intensity) + (1f - (1f - g) * (1f - bg)) * intensity
                val mixedB = b * (1f - intensity) + (1f - (1f - b) * (1f - bb)) * intensity

                val outR = (mixedR.coerceIn(0f, 1f) * 255f).toInt()
                val outG = (mixedG.coerceIn(0f, 1f) * 255f).toInt()
                val outB = (mixedB.coerceIn(0f, 1f) * 255f).toInt()
                outputPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }

            outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
            onProgress(1f)
            outputBitmap
        } catch (e: OutOfMemoryError) {
            output?.recycle()
            throw RuntimeException("柔光效果处理内存不足", e)
        } catch (e: Exception) {
            output?.recycle()
            throw e
        }
    }

    /**
     * 三次盒式模糊（近似高斯模糊）
     */
    private fun boxBlur3Pass(data: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        var current = data.copyOf()
        current = boxBlurH(current, width, height, radius)
        current = boxBlurV(current, width, height, radius)
        current = boxBlurH(current, width, height, radius)
        current = boxBlurV(current, width, height, radius)
        current = boxBlurH(current, width, height, radius / 2)
        current = boxBlurV(current, width, height, radius / 2)
        return current
    }

    private fun boxBlurH(data: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        val output = FloatArray(data.size)
        val channels = 3
        val diameter = radius * 2 + 1

        for (y in 0 until height) {
            for (c in 0 until channels) {
                var sum = 0f
                // 初始化窗口
                for (dx in -radius..radius) {
                    val sx = (dx).coerceIn(0, width - 1)
                    sum += data[(y * width + sx) * channels + c]
                }

                for (x in 0 until width) {
                    output[(y * width + x) * channels + c] = sum / diameter

                    // 滑动窗口：减去左边，加上右边
                    val leftX = (x - radius - 1).coerceIn(0, width - 1)
                    val rightX = (x + radius + 1).coerceIn(0, width - 1)
                    sum -= data[(y * width + leftX) * channels + c]
                    sum += data[(y * width + rightX) * channels + c]
                }
            }
        }
        return output
    }

    private fun boxBlurV(data: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        val output = FloatArray(data.size)
        val channels = 3
        val diameter = radius * 2 + 1

        for (x in 0 until width) {
            for (c in 0 until channels) {
                var sum = 0f
                for (dy in -radius..radius) {
                    val sy = dy.coerceIn(0, height - 1)
                    sum += data[(sy * width + x) * channels + c]
                }

                for (y in 0 until height) {
                    output[(y * width + x) * channels + c] = sum / diameter

                    val topY = (y - radius - 1).coerceIn(0, height - 1)
                    val bottomY = (y + radius + 1).coerceIn(0, height - 1)
                    sum -= data[(topY * width + x) * channels + c]
                    sum += data[(bottomY * width + x) * channels + c]
                }
            }
        }
        return output
    }
}
