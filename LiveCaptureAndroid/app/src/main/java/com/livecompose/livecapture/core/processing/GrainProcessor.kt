package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * 颗粒处理器
 *
 * 模拟胶片颗粒效果，增强照片的胶片质感。
 * 支持颗粒强度和颗粒大小两个参数。
 *
 * 对应 OMaster 预设参数:
 * - grain (0~+10)
 * - grain_size (0~+10)
 */
class GrainProcessor {

    private val random = Random(System.currentTimeMillis())

    /**
     * 应用颗粒效果
     *
     * @param bitmap 原始图像
     * @param intensity 颗粒强度 0~10
     * @param size 颗粒大小 0~10
     * @return 添加颗粒后的 Bitmap
     */
    suspend fun apply(
        bitmap: Bitmap,
        intensity: Float = 0f,
        size: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        val clampedIntensity = (intensity / 10f).coerceIn(0f, 1f)
        if (clampedIntensity < 0.001f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        // 颗粒大小映射到像素半径
        val grainRadius = (size / 10f * 3f).toInt().coerceAtLeast(1)
        val grainDiameter = grainRadius * 2 + 1

        // 生成颗粒噪声图（降低分辨率以匹配颗粒大小）
        val noiseGridX = (width / grainDiameter).coerceAtLeast(1)
        val noiseGridY = (height / grainDiameter).coerceAtLeast(1)
        val noiseGrid = FloatArray(noiseGridX * noiseGridY)

        for (i in noiseGrid.indices) {
            noiseGrid[i] = (random.nextFloat() - 0.5f) * 2f // -1~1
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x

                // 双线性插值获取当前像素位置的噪声值
                val gridX = x.toFloat() / grainDiameter
                val gridY = y.toFloat() / grainDiameter
                val noiseValue = bilinearSample(noiseGrid, noiseGridX, noiseGridY, gridX, gridY)

                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                // 将噪声叠加到 RGB（亮度感知：暗部加亮颗粒，亮部增加暗颗粒）
                val grainEffect = noiseValue * clampedIntensity * 80f
                val adjustedR = (r + grainEffect).toInt().coerceIn(0, 255)
                val adjustedG = (g + grainEffect).toInt().coerceIn(0, 255)
                val adjustedB = (b + grainEffect).toInt().coerceIn(0, 255)

                outPixels[i] = (0xFF shl 24) or (adjustedR shl 16) or (adjustedG shl 8) or adjustedB
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        output
    }

    private fun bilinearSample(
        grid: FloatArray,
        gridWidth: Int,
        gridHeight: Int,
        x: Float,
        y: Float
    ): Float {
        val x0 = x.toInt().coerceIn(0, gridWidth - 1)
        val y0 = y.toInt().coerceIn(0, gridHeight - 1)
        val x1 = (x0 + 1).coerceIn(0, gridWidth - 1)
        val y1 = (y0 + 1).coerceIn(0, gridHeight - 1)

        val fx = x - x0
        val fy = y - y0

        val v00 = grid[y0 * gridWidth + x0]
        val v10 = grid[y0 * gridWidth + x1]
        val v01 = grid[y1 * gridWidth + x0]
        val v11 = grid[y1 * gridWidth + x1]

        return v00 * (1 - fx) * (1 - fy) +
                v10 * fx * (1 - fy) +
                v01 * (1 - fx) * fy +
                v11 * fx * fy
    }
}