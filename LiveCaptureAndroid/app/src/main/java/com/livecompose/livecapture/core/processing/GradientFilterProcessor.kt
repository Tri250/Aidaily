package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 渐变滤镜处理器
 * 模拟 GND（中灰渐变镜）效果和径向渐变效果
 */
class GradientFilterProcessor {

    /**
     * 应用线性渐变滤镜
     *
     * @param bitmap 原始图像
     * @param angle 渐变角度（度），0度为从左到右的水平渐变
     * @param intensity 渐变强度 0~1
     * @param exposure 曝光调整 (-1 到 +1, 0 = 不变)
     * @param contrast 对比度调整 (-1 到 +1, 0 = 不变)
     * @param saturation 饱和度调整 (-1 到 +1, 0 = 不变)
     * @return 处理后的 Bitmap
     */
    suspend fun applyLinearGradient(
        bitmap: Bitmap,
        angle: Float = 90f,
        intensity: Float = 0.5f,
        exposure: Float = -0.5f,
        contrast: Float = 0f,
        saturation: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (intensity <= 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val angleRad = Math.toRadians(angle.toDouble())
        val dirX = cos(angleRad).toFloat()
        val dirY = sin(angleRad).toFloat()

        // 计算投影范围
        var minProj = Float.MAX_VALUE
        var maxProj = Float.MIN_VALUE
        for (y in 0 until height) {
            for (x in 0 until width) {
                val proj = x * dirX + y * dirY
                if (proj < minProj) minProj = proj
                if (proj > maxProj) maxProj = proj
            }
        }
        val projRange = maxProj - minProj
        if (projRange <= 0f) return@withContext bitmap

        // 预计算渐变查找表
        val gradientLut = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val proj = x * dirX + y * dirY
                // 渐变: 0(最暗) 到 1(最亮)
                gradientLut[y * width + x] = ((proj - minProj) / projRange).coerceIn(0f, 1f)
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = ((pixel shr 16) and 0xFF) / 255f
            var g = ((pixel shr 8) and 0xFF) / 255f
            var b = (pixel and 0xFF) / 255f

            // 渐变因子: 1 = 完全应用调整, 0 = 不调整
            val gradFactor = gradientLut[i] * intensity

            if (gradFactor > 0.001f) {
                // 曝光调整
                val exposureFactor = 1f + exposure * gradFactor
                r *= exposureFactor
                g *= exposureFactor
                b *= exposureFactor

                // 对比度调整
                if (abs(contrast) > 0.001f) {
                    val contrastFactor = 1f + contrast * gradFactor
                    r = ((r - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
                    g = ((g - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
                    b = ((b - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
                }

                // 饱和度调整
                if (abs(saturation) > 0.001f) {
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    val satFactor = 1f + saturation * gradFactor
                    r = (lum + (r - lum) * satFactor).coerceIn(0f, 1f)
                    g = (lum + (g - lum) * satFactor).coerceIn(0f, 1f)
                    b = (lum + (b - lum) * satFactor).coerceIn(0f, 1f)
                }
            }

            r = r.coerceIn(0f, 1f)
            g = g.coerceIn(0f, 1f)
            b = b.coerceIn(0f, 1f)

            val outRi = (r * 255f).toInt()
            val outGi = (g * 255f).toInt()
            val outBi = (b * 255f).toInt()
            outputPixels[i] = (0xFF shl 24) or (outRi shl 16) or (outGi shl 8) or outBi
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * 应用径向渐变滤镜
     * 模拟局部提亮/压暗效果（类似暗角但中心可调）
     *
     * @param bitmap 原始图像
     * @param centerX 渐变中心 X 位置 (0~1, 默认 0.5 即中心)
     * @param centerY 渐变中心 Y 位置 (0~1, 默认 0.5 即中心)
     * @param intensity 渐变强度 0~1
     * @param exposure 曝光调整 (-1 到 +1)
     * @param contrast 对比度调整 (-1 到 +1)
     * @param saturation 饱和度调整 (-1 到 +1)
     * @return 处理后的 Bitmap
     */
    suspend fun applyRadialGradient(
        bitmap: Bitmap,
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
        intensity: Float = 0.5f,
        exposure: Float = -0.5f,
        contrast: Float = 0f,
        saturation: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (intensity <= 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val cx = centerX * width
        val cy = centerY * height

        // 最大距离（从中心到最远角）
        val maxDist = maxOf(
            sqrt((cx * cx + cy * cy).toDouble()),
            sqrt(((width - cx) * (width - cx) + cy * cy).toDouble()),
            sqrt((cx * cx + (height - cy) * (height - cy)).toDouble()),
            sqrt(((width - cx) * (width - cx) + (height - cy) * (height - cy)).toDouble())
        ).toFloat()

        if (maxDist <= 0f) return@withContext bitmap

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val pixel = pixels[idx]

                var r = ((pixel shr 16) and 0xFF) / 255f
                var g = ((pixel shr 8) and 0xFF) / 255f
                var b = (pixel and 0xFF) / 255f

                // 距离因子: 0 = 中心, 1 = 最远
                val dist = sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble()).toFloat()
                val gradFactor = (dist / maxDist).coerceIn(0f, 1f) * intensity

                if (gradFactor > 0.001f) {
                    // 曝光
                    val exposureFactor = 1f + exposure * gradFactor
                    r *= exposureFactor
                    g *= exposureFactor
                    b *= exposureFactor

                    // 对比度
                    if (abs(contrast) > 0.001f) {
                        val contrastFactor = 1f + contrast * gradFactor
                        r = ((r - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
                        g = ((g - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
                        b = ((b - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
                    }

                    // 饱和度
                    if (abs(saturation) > 0.001f) {
                        val lum = 0.299f * r + 0.587f * g + 0.114f * b
                        val satFactor = 1f + saturation * gradFactor
                        r = (lum + (r - lum) * satFactor).coerceIn(0f, 1f)
                        g = (lum + (g - lum) * satFactor).coerceIn(0f, 1f)
                        b = (lum + (b - lum) * satFactor).coerceIn(0f, 1f)
                    }
                }

                r = r.coerceIn(0f, 1f)
                g = g.coerceIn(0f, 1f)
                b = b.coerceIn(0f, 1f)

                val outRi = (r * 255f).toInt()
                val outGi = (g * 255f).toInt()
                val outBi = (b * 255f).toInt()
                outputPixels[idx] = (0xFF shl 24) or (outRi shl 16) or (outGi shl 8) or outBi
            }
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }
}