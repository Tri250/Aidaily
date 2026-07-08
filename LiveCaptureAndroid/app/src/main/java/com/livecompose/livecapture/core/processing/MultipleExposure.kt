package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 多重曝光合成器
 * 将多张照片叠加合成一张多重曝光效果
 */
class MultipleExposure {

    /**
     * 合成模式
     */
    enum class BlendMode(val displayName: String) {
        AVERAGE("平均"),
        ADDITIVE("叠加增亮"),
        SCREEN("滤色"),
        MULTIPLY("正片叠底"),
        LIGHTEN("变亮"),
        OVERLAY("叠加")
    }

    /**
     * 合成多张图像
     *
     * @param bitmaps 参与合成的图像列表
     * @param blendMode 合成模式
     * @param opacity 每层的透明度 (0~1)
     * @param onProgress 进度回调
     */
    suspend fun blend(
        bitmaps: List<Bitmap>,
        blendMode: BlendMode = BlendMode.SCREEN,
        opacity: Float = 0.5f,
        onProgress: (Float) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        if (bitmaps.isEmpty()) throw IllegalArgumentException("至少需要 1 张图片")
        if (bitmaps.size == 1) return@withContext bitmaps[0]

        val base = bitmaps[0]
        val width = base.width
        val height = base.height
        var result = base.copy(Bitmap.Config.ARGB_8888, true)

        for (layerIdx in 1 until bitmaps.size) {
            val layer = bitmaps[layerIdx]
            // 将图层缩放到相同尺寸
            val scaledLayer = if (layer.width != width || layer.height != height) {
                Bitmap.createScaledBitmap(layer, width, height, true)
            } else {
                layer
            }

            val previousResult = result
            result = blendTwoBitmaps(result, scaledLayer, blendMode, opacity)
            // 回收旧的中间结果 Bitmap
            previousResult.recycle()
            onProgress(layerIdx.toFloat() / bitmaps.size)

            if (scaledLayer !== layer) scaledLayer.recycle()
        }

        result
    }

    private fun blendTwoBitmaps(
        base: Bitmap,
        layer: Bitmap,
        mode: BlendMode,
        opacity: Float
    ): Bitmap {
        val width = base.width
        val height = base.height
        val basePixels = IntArray(width * height)
        val layerPixels = IntArray(width * height)
        base.getPixels(basePixels, 0, width, 0, 0, width, height)
        layer.getPixels(layerPixels, 0, width, 0, 0, width, height)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        for (i in basePixels.indices) {
            val bp = basePixels[i]
            val lp = layerPixels[i]

            val bR = ((bp shr 16) and 0xFF) / 255f
            val bG = ((bp shr 8) and 0xFF) / 255f
            val bB = (bp and 0xFF) / 255f

            val lR = ((lp shr 16) and 0xFF) / 255f
            val lG = ((lp shr 8) and 0xFF) / 255f
            val lB = (lp and 0xFF) / 255f

            val (r, g, b) = when (mode) {
                BlendMode.AVERAGE -> Triple(
                    (bR + lR) / 2f, (bG + lG) / 2f, (bB + lB) / 2f
                )
                BlendMode.ADDITIVE -> Triple(
                    (bR + lR).coerceIn(0f, 1f),
                    (bG + lG).coerceIn(0f, 1f),
                    (bB + lB).coerceIn(0f, 1f)
                )
                BlendMode.SCREEN -> Triple(
                    1f - (1f - bR) * (1f - lR),
                    1f - (1f - bG) * (1f - lG),
                    1f - (1f - bB) * (1f - lB)
                )
                BlendMode.MULTIPLY -> Triple(
                    bR * lR, bG * lG, bB * lB
                )
                BlendMode.LIGHTEN -> Triple(
                    maxOf(bR, lR), maxOf(bG, lG), maxOf(bB, lB)
                )
                BlendMode.OVERLAY -> Triple(
                    overlayValue(bR, lR), overlayValue(bG, lG), overlayValue(bB, lB)
                )
            }

            val outR = (r * 255f).toInt().coerceIn(0, 255)
            val outG = (g * 255f).toInt().coerceIn(0, 255)
            val outB = (b * 255f).toInt().coerceIn(0, 255)
            outputPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun overlayValue(base: Float, layer: Float): Float {
        return if (base < 0.5f) {
            2f * base * layer
        } else {
            1f - 2f * (1f - base) * (1f - layer)
        }
    }
}
