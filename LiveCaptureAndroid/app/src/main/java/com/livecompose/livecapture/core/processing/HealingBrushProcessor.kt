package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 修复画笔处理器
 * 支持污点修复和克隆印章功能
 */
class HealingBrushProcessor {

    /**
     * 污点修复
     * 从源区域取样像素，通过加权平均混合到目标区域
     *
     * @param bitmap 原始图像（会被修改并返回新副本）
     * @param sourceX 取样源中心 X 坐标
     * @param sourceY 取样源中心 Y 坐标
     * @param targetX 修复目标中心 X 坐标
     * @param targetY 修复目标中心 Y 坐标
     * @param brushRadius 画笔半径（像素）
     * @return 修复后的新 Bitmap
     */
    suspend fun healSpot(
        bitmap: Bitmap,
        sourceX: Float,
        sourceY: Float,
        targetX: Float,
        targetY: Float,
        brushRadius: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = pixels.copyOf()

        val radius = brushRadius.coerceAtLeast(1f)
        val radiusSq = radius * radius
        val offsetX = targetX - sourceX
        val offsetY = targetY - sourceY

        for (dy in (-radius).toInt()..radius.toInt()) {
            for (dx in (-radius).toInt()..radius.toInt()) {
                val distSq = (dx * dx + dy * dy).toFloat()
                if (distSq > radiusSq) continue

                val tx = (targetX + dx).toInt().coerceIn(0, width - 1)
                val ty = (targetY + dy).toInt().coerceIn(0, height - 1)
                val ti = ty * width + tx

                // 源像素位置
                val sx = (sourceX + dx).toInt().coerceIn(0, width - 1)
                val sy = (sourceY + dy).toInt().coerceIn(0, height - 1)
                val si = sy * width + sx

                // 加权平均混合（泊松融合简化版）
                // 权重 = 1 - dist/radius, 越靠近中心越强
                val weight = (1f - sqrt(distSq) / radius).coerceIn(0f, 1f)

                val srcPixel = pixels[si]
                val dstPixel = pixels[ti]

                val srcR = (srcPixel shr 16) and 0xFF
                val srcG = (srcPixel shr 8) and 0xFF
                val srcB = srcPixel and 0xFF

                val dstR = (dstPixel shr 16) and 0xFF
                val dstG = (dstPixel shr 8) and 0xFF
                val dstB = dstPixel and 0xFF

                // 污点修复: 保留目标区域的亮度，混合源区域的纹理
                val srcLum = 0.299f * srcR + 0.587f * srcG + 0.114f * srcB
                val dstLum = 0.299f * dstR + 0.587f * dstG + 0.114f * dstB

                val mixedR = (dstR + (srcR - srcLum) * weight).coerceIn(0f, 255f).toInt()
                val mixedG = (dstG + (srcG - srcLum) * weight).coerceIn(0f, 255f).toInt()
                val mixedB = (dstB + (srcB - srcLum) * weight).coerceIn(0f, 255f).toInt()

                outputPixels[ti] = (0xFF shl 24) or (mixedR shl 16) or (mixedG shl 8) or mixedB
            }
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * 克隆印章
     * 直接复制源区域像素到目标区域，边缘使用加权平均混合
     *
     * @param bitmap 原始图像（会被修改并返回新副本）
     * @param sourceX 取样源中心 X 坐标
     * @param sourceY 取样源中心 Y 坐标
     * @param targetX 绘制目标中心 X 坐标
     * @param targetY 绘制目标中心 Y 坐标
     * @param brushRadius 画笔半径（像素）
     * @return 克隆后的新 Bitmap
     */
    suspend fun cloneStamp(
        bitmap: Bitmap,
        sourceX: Float,
        sourceY: Float,
        targetX: Float,
        targetY: Float,
        brushRadius: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = pixels.copyOf()

        val radius = brushRadius.coerceAtLeast(1f)
        val radiusSq = radius * radius

        for (dy in (-radius).toInt()..radius.toInt()) {
            for (dx in (-radius).toInt()..radius.toInt()) {
                val distSq = (dx * dx + dy * dy).toFloat()
                if (distSq > radiusSq) continue

                val tx = (targetX + dx).toInt().coerceIn(0, width - 1)
                val ty = (targetY + dy).toInt().coerceIn(0, height - 1)
                val ti = ty * width + tx

                val sx = (sourceX + dx).toInt().coerceIn(0, width - 1)
                val sy = (sourceY + dy).toInt().coerceIn(0, height - 1)
                val si = sy * width + sx

                // 边缘羽化: 在画笔边缘使用加权平均混合
                val dist = sqrt(distSq)
                val featherStart = radius * 0.7f
                val featherWeight = if (dist > featherStart) {
                    ((radius - dist) / (radius - featherStart)).coerceIn(0f, 1f)
                } else 1f

                val srcPixel = pixels[si]
                val dstPixel = pixels[ti]

                val srcR = (srcPixel shr 16) and 0xFF
                val srcG = (srcPixel shr 8) and 0xFF
                val srcB = srcPixel and 0xFF

                val dstR = (dstPixel shr 16) and 0xFF
                val dstG = (dstPixel shr 8) and 0xFF
                val dstB = dstPixel and 0xFF

                val mixedR = (srcR * featherWeight + dstR * (1f - featherWeight)).toInt()
                val mixedG = (srcG * featherWeight + dstG * (1f - featherWeight)).toInt()
                val mixedB = (srcB * featherWeight + dstB * (1f - featherWeight)).toInt()

                outputPixels[ti] = (0xFF shl 24) or (mixedR shl 16) or (mixedG shl 8) or mixedB
            }
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }
}