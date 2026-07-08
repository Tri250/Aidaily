package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 去雾处理器
 * 基于暗通道先验 (Dark Channel Prior) 算法实现图像去雾
 */
class DehazeProcessor {

    /**
     * 去雾处理
     *
     * @param bitmap 原始有雾图像
     * @param strength 去雾强度 0~100
     * @return 去雾后的 Bitmap
     */
    suspend fun dehaze(
        bitmap: Bitmap,
        strength: Float = 50f
    ): Bitmap = withContext(Dispatchers.Default) {
        val clampedStrength = strength.coerceIn(0f, 100f) / 100f
        if (clampedStrength <= 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 提取 RGB 通道
        val rChannel = FloatArray(width * height)
        val gChannel = FloatArray(width * height)
        val bChannel = FloatArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            rChannel[i] = ((pixel shr 16) and 0xFF) / 255f
            gChannel[i] = ((pixel shr 8) and 0xFF) / 255f
            bChannel[i] = (pixel and 0xFF) / 255f
        }

        // 步骤 1: 计算暗通道
        val patchRadius = 7
        val darkChannel = computeDarkChannel(rChannel, gChannel, bChannel, width, height, patchRadius)

        // 步骤 2: 估算大气光值 A
        val atmosphericLight = estimateAtmosphericLight(rChannel, gChannel, bChannel, darkChannel, width, height)

        // 步骤 3: 估算透射率图
        val omega = 0.95f // 保留少量雾使图像看起来自然
        val transmission = computeTransmission(
            rChannel, gChannel, bChannel,
            atmosphericLight, width, height, patchRadius, omega
        )

        // 步骤 4: 引导滤波平滑透射率图（简化版：盒式模糊）
        val smoothTransmission = boxBlur(transmission, width, height, 5)

        // 步骤 5: 恢复无雾图像
        val t0 = 0.1f // 透射率下限，防止除零
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        val aR = atmosphericLight[0]
        val aG = atmosphericLight[1]
        val aB = atmosphericLight[2]

        for (i in pixels.indices) {
            val t = maxOf(smoothTransmission[i], t0)

            // 根据强度插值: 0 = 原图, 1 = 完全去雾
            var dehazeR = ((rChannel[i] - aR) / t + aR).coerceIn(0f, 1f)
            var dehazeG = ((gChannel[i] - aG) / t + aG).coerceIn(0f, 1f)
            var dehazeB = ((bChannel[i] - aB) / t + aB).coerceIn(0f, 1f)

            // 混合原图和去雾结果
            dehazeR = rChannel[i] + (dehazeR - rChannel[i]) * clampedStrength
            dehazeG = gChannel[i] + (dehazeG - gChannel[i]) * clampedStrength
            dehazeB = bChannel[i] + (dehazeB - bChannel[i]) * clampedStrength

            val outRi = (dehazeR.coerceIn(0f, 1f) * 255f).toInt()
            val outGi = (dehazeG.coerceIn(0f, 1f) * 255f).toInt()
            val outBi = (dehazeB.coerceIn(0f, 1f) * 255f).toInt()
            outputPixels[i] = (0xFF shl 24) or (outRi shl 16) or (outGi shl 8) or outBi
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * 计算暗通道
     * 对每个像素，取其邻域中 RGB 三个通道的最小值
     */
    private fun computeDarkChannel(
        r: FloatArray, g: FloatArray, b: FloatArray,
        width: Int, height: Int, radius: Int
    ): FloatArray {
        val dark = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var minVal = 1f
                val yStart = maxOf(0, y - radius)
                val yEnd = minOf(height - 1, y + radius)
                val xStart = maxOf(0, x - radius)
                val xEnd = minOf(width - 1, x + radius)
                for (ny in yStart..yEnd) {
                    for (nx in xStart..xEnd) {
                        val idx = ny * width + nx
                        val localMin = minOf(r[idx], g[idx], b[idx])
                        if (localMin < minVal) minVal = localMin
                    }
                }
                dark[y * width + x] = minVal
            }
        }
        return dark
    }

    /**
     * 估算大气光值
     * 取暗通道中最亮的 0.1% 像素对应原图中的最大值
     */
    private fun estimateAtmosphericLight(
        r: FloatArray, g: FloatArray, b: FloatArray,
        darkChannel: FloatArray, width: Int, height: Int
    ): FloatArray {
        val totalPixels = width * height
        val numBrightest = maxOf(1, totalPixels / 1000) // 0.1%

        // 创建索引数组并按暗通道亮度排序
        val indices = (0 until totalPixels).toList()
            .sortedByDescending { darkChannel[it] }
            .take(numBrightest)

        var maxR = 0f
        var maxG = 0f
        var maxB = 0f
        for (idx in indices) {
            val luminance = 0.299f * r[idx] + 0.587f * g[idx] + 0.114f * b[idx]
            if (luminance > 0.299f * maxR + 0.587f * maxG + 0.114f * maxB) {
                maxR = r[idx]
                maxG = g[idx]
                maxB = b[idx]
            }
        }

        if (maxR <= 0f && maxG <= 0f && maxB <= 0f) {
            maxR = 1f
            maxG = 1f
            maxB = 1f
        }

        return floatArrayOf(maxR, maxG, maxB)
    }

    /**
     * 计算透射率图
     */
    private fun computeTransmission(
        r: FloatArray, g: FloatArray, b: FloatArray,
        atmosphericLight: FloatArray, width: Int, height: Int,
        radius: Int, omega: Float
    ): FloatArray {
        val aR = atmosphericLight[0]
        val aG = atmosphericLight[1]
        val aB = atmosphericLight[2]

        val transmission = FloatArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var minVal = 1f
                val yStart = maxOf(0, y - radius)
                val yEnd = minOf(height - 1, y + radius)
                val xStart = maxOf(0, x - radius)
                val xEnd = minOf(width - 1, x + radius)

                for (ny in yStart..yEnd) {
                    for (nx in xStart..xEnd) {
                        val idx = ny * width + nx
                        val normalizedR = r[idx] / maxOf(aR, 0.001f)
                        val normalizedG = g[idx] / maxOf(aG, 0.001f)
                        val normalizedB = b[idx] / maxOf(aB, 0.001f)
                        val localMin = minOf(normalizedR, normalizedG, normalizedB)
                        if (localMin < minVal) minVal = localMin
                    }
                }

                transmission[y * width + x] = 1f - omega * minVal
            }
        }

        return transmission
    }

    /**
     * 盒式模糊（用于平滑透射率图）
     */
    private fun boxBlur(data: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        var result = data.copyOf()
        // 水平模糊
        val temp = FloatArray(result.size)
        val diameter = radius * 2 + 1
        for (y in 0 until height) {
            var sum = 0f
            for (dx in -radius..radius) {
                val sx = dx.coerceIn(0, width - 1)
                sum += result[y * width + sx]
            }
            for (x in 0 until width) {
                temp[y * width + x] = sum / diameter
                val leftX = (x - radius).coerceIn(0, width - 1)
                val rightX = (x + radius + 1).coerceIn(0, width - 1)
                sum -= result[y * width + leftX]
                sum += result[y * width + rightX]
            }
        }
        // 垂直模糊
        for (x in 0 until width) {
            var sum = 0f
            for (dy in -radius..radius) {
                val sy = dy.coerceIn(0, height - 1)
                sum += temp[sy * width + x]
            }
            for (y in 0 until height) {
                result[y * width + x] = sum / diameter
                val topY = (y - radius).coerceIn(0, height - 1)
                val bottomY = (y + radius + 1).coerceIn(0, height - 1)
                sum -= temp[topY * width + x]
                sum += temp[bottomY * width + x]
            }
        }
        return result
    }
}