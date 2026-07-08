package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * HDR 融合处理器
 * 多曝光融合保留高光细节
 */
class HdrFusion {

    companion object {
        private const val TAG = "HdrFusion"
        private const val SIGMA_SQ = 0.2f * 0.2f // 高斯权重参数
    }

    /**
     * 融合多帧不同曝光的图像
     * 使用 Mertens 曝光融合算法
     */
    suspend fun fuse(
        frames: List<Bitmap>,
        exposures: List<Float> = frames.map { 1f },
        onProgress: (Float) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        var output: Bitmap? = null
        try {
            if (frames.size < 2) return@withContext frames.firstOrNull()
                ?: throw IllegalArgumentException("至少需要 2 帧图像")

            require(frames.size == exposures.size) { "帧数与曝光数不匹配" }

            val width = frames[0].width
            val height = frames[0].height
            output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // 提取所有帧的像素
            val allPixels = frames.map { frame ->
                val pixels = IntArray(width * height)
                frame.getPixels(pixels, 0, width, 0, 0, width, height)
                pixels
            }

            val outputPixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    var sumR = 0f
                    var sumG = 0f
                    var sumB = 0f
                    var sumWeight = 0f

                    for (f in frames.indices) {
                        val pixel = allPixels[f][idx]
                        val r = ((pixel shr 16) and 0xFF) / 255f
                        val g = ((pixel shr 8) and 0xFF) / 255f
                        val b = (pixel and 0xFF) / 255f
                        val lum = 0.299f * r + 0.587f * g + 0.114f * b

                        // === Mertens 权重 ===

                        // 1. 对比度权重 (与周围像素的差异)
                        val contrastWeight = 1f // 简化：使用亮度值本身

                        // 2. 饱和度权重
                        val mean = (r + g + b) / 3f
                        val satVal = sqrt(((r - mean).pow(2) + (g - mean).pow(2) + (b - mean).pow(2)) / 3f)
                        val saturationWeight = satVal + 0.01f

                        // 3. 良好曝光权重（高斯权重，峰值在 0.5 处）
                        val exposureWeight = exp(-((lum - 0.5f).pow(2)) / (2 * SIGMA_SQ)) + 0.001f

                        val weight = contrastWeight * saturationWeight * exposureWeight * exposures[f]

                        sumR += r * weight
                        sumG += g * weight
                        sumB += b * weight
                        sumWeight += weight
                    }

                    if (sumWeight > 0f) {
                        val r = (sumR / sumWeight * 255f).toInt().coerceIn(0, 255)
                        val g = (sumG / sumWeight * 255f).toInt().coerceIn(0, 255)
                        val b = (sumB / sumWeight * 255f).toInt().coerceIn(0, 255)
                        outputPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    } else {
                        outputPixels[idx] = allPixels[0][idx]
                    }
                }

                if (y % 50 == 0) onProgress(y.toFloat() / height)
            }

            output.setPixels(outputPixels, 0, width, 0, 0, width, height)
            onProgress(1f)
            output
        } catch (e: OutOfMemoryError) {
            output?.recycle()
            throw RuntimeException("HDR 融合内存不足，请尝试降低图像分辨率", e)
        }
    }

    /**
     * 生成多曝光帧序列
     * 从单帧图像通过亮度映射生成不同曝光的版本
     */
    fun generateExposureBrackets(source: Bitmap, evOffsets: List<Float> = listOf(-1f, 0f, 1f)): List<Bitmap> {
        val width = source.width
        val height = source.height
        val sourcePixels = IntArray(width * height)
        source.getPixels(sourcePixels, 0, width, 0, 0, width, height)

        return evOffsets.map { ev ->
            val factor = 2f.pow(ev)
            val bracket = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val bracketPixels = IntArray(width * height)

            for (i in sourcePixels.indices) {
                val pixel = sourcePixels[i]
                val r = (((pixel shr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
                val g = (((pixel shr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
                val b = ((pixel and 0xFF) * factor).toInt().coerceIn(0, 255)
                bracketPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }

            bracket.setPixels(bracketPixels, 0, width, 0, 0, width, height)
            bracket
        }
    }
}
