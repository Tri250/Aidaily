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
     * 使用分离式最小值滤波 O(W×H)，替代暴力滑动窗口 O(W×H×R²)
     * 先水平方向取最小值，再垂直方向取最小值
     */
    private fun computeDarkChannel(
        r: FloatArray, g: FloatArray, b: FloatArray,
        width: Int, height: Int, radius: Int
    ): FloatArray {
        // 第一步：逐像素取 RGB 最小值
        val minRgb = FloatArray(width * height)
        for (i in minRgb.indices) {
            minRgb[i] = minOf(r[i], g[i], b[i])
        }

        // 第二步：水平方向最小值滤波
        val horizontal = FloatArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            // 用单调双端队列实现 O(W) 水平最小值滤波
            val deque = java.util.ArrayDeque<Int>()
            for (x in 0 until width + radius) {
                val clampedX = x.coerceIn(0, width - 1)
                val val_ = minRgb[rowOffset + clampedX]
                // 从队尾移除大于当前值的元素
                while (deque.isNotEmpty() && minRgb[rowOffset + deque.last()] >= val_) {
                    deque.removeLast()
                }
                deque.addLast(clampedX)
                // 移除超出窗口的元素
                val windowLeft = x - radius * 2
                while (deque.isNotEmpty() && deque.first() < windowLeft.coerceIn(0, width - 1).also { if (it > deque.first()) deque.removeFirst() }) {
                    deque.removeFirst()
                }
                if (deque.isNotEmpty() && deque.first() < (x - radius * 2).coerceIn(0, width - 1)) {
                    deque.removeFirst()
                }
                // 输出当前窗口最小值
                if (x >= radius) {
                    val outX = x - radius
                    if (outX < width) {
                        horizontal[rowOffset + outX] = minRgb[rowOffset + deque.first()]
                    }
                }
            }
        }

        // 第三步：垂直方向最小值滤波
        val dark = FloatArray(width * height)
        for (x in 0 until width) {
            val deque = java.util.ArrayDeque<Int>()
            for (y in 0 until height + radius) {
                val clampedY = y.coerceIn(0, height - 1)
                val val_ = horizontal[clampedY * width + x]
                while (deque.isNotEmpty() && horizontal[deque.last() * width + x] >= val_) {
                    deque.removeLast()
                }
                deque.addLast(clampedY)
                val windowTop = y - radius * 2
                if (deque.isNotEmpty() && deque.first() < windowTop.coerceIn(0, height - 1)) {
                    deque.removeFirst()
                }
                if (y >= radius) {
                    val outY = y - radius
                    if (outY < height) {
                        dark[outY * width + x] = horizontal[deque.first() * width + x]
                    }
                }
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
     * 使用分离式最小值滤波优化，避免 O(W×H×R²)
     */
    private fun computeTransmission(
        r: FloatArray, g: FloatArray, b: FloatArray,
        atmosphericLight: FloatArray, width: Int, height: Int,
        radius: Int, omega: Float
    ): FloatArray {
        val aR = atmosphericLight[0]
        val aG = atmosphericLight[1]
        val aB = atmosphericLight[2]

        // 归一化后取 RGB 最小值
        val normalized = FloatArray(width * height)
        for (i in normalized.indices) {
            val nR = r[i] / maxOf(aR, 0.001f)
            val nG = g[i] / maxOf(aG, 0.001f)
            val nB = b[i] / maxOf(aB, 0.001f)
            normalized[i] = minOf(nR, nG, nB)
        }

        // 分离式最小值滤波（与 computeDarkChannel 相同算法）
        val darkChannel = computeDarkChannelFromNormalized(normalized, width, height, radius)

        val transmission = FloatArray(width * height)
        for (i in transmission.indices) {
            transmission[i] = 1f - omega * darkChannel[i]
        }
        return transmission
    }

    /**
     * 对已归一化的单通道数据进行分离式最小值滤波
     */
    private fun computeDarkChannelFromNormalized(
        data: FloatArray, width: Int, height: Int, radius: Int
    ): FloatArray {
        // 水平方向最小值滤波
        val horizontal = FloatArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            var minVal = Float.MAX_VALUE
            // 初始化窗口 [0, 2*radius]
            for (x in 0..minOf(2 * radius, width - 1)) {
                if (data[rowOffset + x] < minVal) minVal = data[rowOffset + x]
            }
            horizontal[rowOffset] = minVal
            // 滑动窗口
            for (x in 1 until width) {
                val leftOut = (x - radius - 1).coerceIn(0, width - 1)
                val rightIn = (x + radius).coerceIn(0, width - 1)
                // 简单滑动：如果离开的值等于当前最小值，需要重新扫描窗口
                if (data[rowOffset + leftOut] == minVal && leftOut != rightIn) {
                    minVal = Float.MAX_VALUE
                    val ws = maxOf(0, x - radius)
                    val we = minOf(width - 1, x + radius)
                    for (wx in ws..we) {
                        if (data[rowOffset + wx] < minVal) minVal = data[rowOffset + wx]
                    }
                } else if (data[rowOffset + rightIn] < minVal) {
                    minVal = data[rowOffset + rightIn]
                }
                horizontal[rowOffset + x] = minVal
            }
        }

        // 垂直方向最小值滤波
        val result = FloatArray(width * height)
        for (x in 0 until width) {
            var minVal = Float.MAX_VALUE
            for (y in 0..minOf(2 * radius, height - 1)) {
                if (horizontal[y * width + x] < minVal) minVal = horizontal[y * width + x]
            }
            result[x] = minVal
            for (y in 1 until height) {
                val topOut = (y - radius - 1).coerceIn(0, height - 1)
                val botIn = (y + radius).coerceIn(0, height - 1)
                if (horizontal[topOut * width + x] == minVal && topOut != botIn) {
                    minVal = Float.MAX_VALUE
                    val ys = maxOf(0, y - radius)
                    val ye = minOf(height - 1, y + radius)
                    for (wy in ys..ye) {
                        if (horizontal[wy * width + x] < minVal) minVal = horizontal[wy * width + x]
                    }
                } else if (horizontal[botIn * width + x] < minVal) {
                    minVal = horizontal[botIn * width + x]
                }
                result[y * width + x] = minVal
            }
        }

        return result
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