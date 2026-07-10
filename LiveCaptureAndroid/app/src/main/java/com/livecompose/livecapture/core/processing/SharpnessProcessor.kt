package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp

/**
 * 锐度处理器
 *
 * 使用 USM (Unsharp Mask) 算法：
 * 1. 对原图做高斯模糊得到模糊图
 * 2. 原图 - 模糊图 = 细节遮罩
 * 3. 原图 + 细节遮罩 × 强度 = 锐化结果
 *
 * 对应 OMaster 预设参数: sharpness (0~15)
 */
class SharpnessProcessor {

    /**
     * 应用 USM 锐化
     *
     * @param bitmap 原始图像
     * @param strength 锐化强度 0~15 (对应 OMaster 预设)
     * @param radius 锐化半径 0.5~3.0 (默认 1.0)
     * @return 锐化后的 Bitmap
     */
    suspend fun apply(
        bitmap: Bitmap,
        strength: Float = 0f,
        radius: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        val clampedStrength = (strength / 15f).coerceIn(0f, 1f)
        if (clampedStrength <= 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height

        // 提取像素
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 分离 RGB 通道
        val r = FloatArray(width * height)
        val g = FloatArray(width * height)
        val b = FloatArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            r[i] = ((p shr 16) and 0xFF) / 255f
            g[i] = ((p shr 8) and 0xFF) / 255f
            b[i] = (p and 0xFF) / 255f
        }

        // 高斯模糊（使用亮度通道以减少计算量）
        val blurred = gaussianBlur(r, width, height, radius)

        // 应用 USM: output = original + (original - blurred) * strength
        val amount = clampedStrength * 1.5f
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val detailR = (r[i] - blurred[i]) * amount
            val detailG = (g[i] - gaussianBlurSingle(g, width, height, i, radius)) * amount
            val detailB = (b[i] - gaussianBlurSingle(b, width, height, i, radius)) * amount

            // 阈值抑制：忽略微小细节（抗噪）
            val threshold = 0.01f
            val finalR = if (kotlin.math.abs(detailR) > threshold) (r[i] + detailR).coerceIn(0f, 1f) else r[i]
            val finalG = if (kotlin.math.abs(detailG) > threshold) (g[i] + detailG).coerceIn(0f, 1f) else g[i]
            val finalB = if (kotlin.math.abs(detailB) > threshold) (b[i] + detailB).coerceIn(0f, 1f) else b[i]

            val ri = (finalR * 255f).toInt()
            val gi = (finalG * 255f).toInt()
            val bi = (finalB * 255f).toInt()
            outPixels[i] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * 全通道高斯模糊
     */
    private fun gaussianBlur(data: FloatArray, width: Int, height: Int, sigma: Float): FloatArray {
        val result = data.copyOf()
        val kernelRadius = (sigma * 2f).toInt().coerceAtLeast(1)
        val kernel = createGaussianKernel(sigma, kernelRadius)

        // 水平方向
        val temp = FloatArray(result.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var weightSum = 0f
                for (dx in -kernelRadius..kernelRadius) {
                    val sx = (x + dx).coerceIn(0, width - 1)
                    val w = kernel[dx + kernelRadius]
                    sum += result[y * width + sx] * w
                    weightSum += w
                }
                temp[y * width + x] = sum / weightSum
            }
        }
        // 垂直方向
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sum = 0f
                var weightSum = 0f
                for (dy in -kernelRadius..kernelRadius) {
                    val sy = (y + dy).coerceIn(0, height - 1)
                    val w = kernel[dy + kernelRadius]
                    sum += temp[sy * width + x] * w
                    weightSum += w
                }
                result[y * width + x] = sum / weightSum
            }
        }
        return result
    }

    /**
     * 单像素高斯模糊（用于节省内存的场景）
     */
    private fun gaussianBlurSingle(data: FloatArray, width: Int, height: Int, index: Int, sigma: Float): Float {
        val x = index % width
        val y = index / width
        val kernelRadius = (sigma * 2f).toInt().coerceAtLeast(1)
        val kernel = createGaussianKernel(sigma, kernelRadius)

        // 水平方向的一行
        val row = FloatArray(width)
        for (cx in 0 until width) {
            var sum = 0f
            var weightSum = 0f
            for (dx in -kernelRadius..kernelRadius) {
                val sx = (cx + dx).coerceIn(0, width - 1)
                val w = kernel[dx + kernelRadius]
                sum += data[y * width + sx] * w
                weightSum += w
            }
            row[cx] = sum / weightSum
        }

        // 垂直方向取当前像素
        var sum = 0f
        var weightSum = 0f
        for (dy in -kernelRadius..kernelRadius) {
            val sy = (y + dy).coerceIn(0, height - 1)
            val w = kernel[dy + kernelRadius]
            sum += (if (sy == y) row[x] else {
                // 对相邻行也做水平模糊
                var hSum = 0f
                var hWSum = 0f
                for (dx in -kernelRadius..kernelRadius) {
                    val sx = (x + dx).coerceIn(0, width - 1)
                    val hw = kernel[dx + kernelRadius]
                    hSum += data[sy * width + sx] * hw
                    hWSum += hw
                }
                hSum / hWSum
            }) * w
            weightSum += w
        }
        return sum / weightSum
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