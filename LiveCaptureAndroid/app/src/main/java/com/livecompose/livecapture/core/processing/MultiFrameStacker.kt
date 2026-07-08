package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 多帧合成降噪处理器
 * 通过对齐+堆栈平均多帧图像来降低噪点
 */
class MultiFrameStacker {

    /**
     * 帧对齐结果
     */
    data class AlignmentResult(
        val offsetX: Int,
        val offsetY: Int,
        val confidence: Float
    )

    /**
     * 帧对齐：使用相位相关法估计偏移量
     */
    fun alignFrames(reference: Bitmap, target: Bitmap): AlignmentResult {
        // 缩小到 1/4 加速计算
        val scale = 0.25f
        val refSmall = Bitmap.createScaledBitmap(reference,
            (reference.width * scale).toInt(), (reference.height * scale).toInt(), true)
        val tgtSmall = Bitmap.createScaledBitmap(target,
            (target.width * scale).toInt(), (target.height * scale).toInt(), true)

        val refPixels = IntArray(refSmall.width * refSmall.height)
        val tgtPixels = IntArray(tgtSmall.width * tgtSmall.height)
        refSmall.getPixels(refPixels, 0, refSmall.width, 0, 0, refSmall.width, refSmall.height)
        tgtSmall.getPixels(tgtPixels, 0, tgtSmall.width, 0, 0, tgtSmall.width, tgtSmall.height)

        // 转为灰度
        val refGray = refPixels.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }.toIntArray()
        val tgtGray = tgtPixels.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }.toIntArray()

        // 块匹配搜索最佳偏移
        val searchRange = 16
        val blockSize = 32
        var bestDx = 0
        var bestDy = 0
        var bestScore = Float.MAX_VALUE

        val cx = refSmall.width / 2
        val cy = refSmall.height / 2
        val halfBlock = blockSize / 2

        for (dy in -searchRange..searchRange) {
            for (dx in -searchRange..searchRange) {
                var sad = 0L
                var count = 0
                for (by in -halfBlock until halfBlock) {
                    for (bx in -halfBlock until halfBlock) {
                        val rx = (cx + bx).coerceIn(0, refSmall.width - 1)
                        val ry = (cy + by).coerceIn(0, refSmall.height - 1)
                        val tx = (cx + bx + dx).coerceIn(0, refSmall.width - 1)
                        val ty = (cy + by + dy).coerceIn(0, refSmall.height - 1)
                        sad += abs(refGray[ry * refSmall.width + rx] - tgtGray[ty * refSmall.width + tx])
                        count++
                    }
                }
                val score = sad.toFloat() / count
                if (score < bestScore) {
                    bestScore = score
                    bestDx = dx
                    bestDy = dy
                }
            }
        }

        refSmall.recycle()
        tgtSmall.recycle()

        val confidence = 1f - (bestScore / 255f).coerceIn(0f, 1f)
        // 偏移量需要按缩放比还原
        return AlignmentResult(
            offsetX = (bestDx / scale).roundToInt(),
            offsetY = (bestDy / scale).roundToInt(),
            confidence = confidence
        )
    }

    /**
     * 多帧堆栈平均
     * 对齐后逐像素平均，降低随机噪点
     */
    suspend fun stackFrames(
        reference: Bitmap,
        frames: List<Bitmap>,
        onProgress: (Float) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) return@withContext reference

        val width = reference.width
        val height = reference.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val totalFrames = frames.size + 1 // reference + 其他帧
        val refPixels = IntArray(width * height)
        reference.getPixels(refPixels, 0, width, 0, 0, width, height)

        // 对齐所有帧
        val alignedFrames = frames.mapIndexed { index, frame ->
            val alignment = alignFrames(reference, frame)
            onProgress((index + 1).toFloat() / frames.size * 0.5f)
            Triple(frame, alignment.offsetX, alignment.offsetY)
        }

        // 逐像素平均
        val outputPixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sumR = 0L
                var sumG = 0L
                var sumB = 0L
                var count = 0

                // 参考帧
                val refPixel = refPixels[y * width + x]
                sumR += (refPixel shr 16) and 0xFF
                sumG += (refPixel shr 8) and 0xFF
                sumB += refPixel and 0xFF
                count++

                // 其他帧
                for ((frame, dx, dy) in alignedFrames) {
                    val fx = x + dx
                    val fy = y + dy
                    if (fx in 0 until frame.width && fy in 0 until frame.height) {
                        val pixel = frame.getPixel(fx, fy)
                        sumR += (pixel shr 16) and 0xFF
                        sumG += (pixel shr 8) and 0xFF
                        sumB += pixel and 0xFF
                        count++
                    }
                }

                val r = (sumR / count).coerceIn(0, 255)
                val g = (sumG / count).coerceIn(0, 255)
                val b = (sumB / count).coerceIn(0, 255)
                outputPixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }

            if (y % 50 == 0) {
                onProgress(0.5f + 0.5f * y / height)
            }
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        onProgress(1f)
        output
    }
}
