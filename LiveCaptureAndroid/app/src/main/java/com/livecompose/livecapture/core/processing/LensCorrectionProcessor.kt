package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 镜头校正处理器
 * 支持畸变校正、色差校正、暗角校正
 */
class LensCorrectionProcessor {

    /**
     * 校正桶形/枕形畸变
     *
     * 使用径向畸变模型: r_corrected = r * (1 + k1*r^2 + k2*r^4 + k3*r^6)
     * - k1 > 0: 枕形畸变校正
     * - k1 < 0: 桶形畸变校正
     *
     * @param bitmap 原始图像
     * @param k1 一阶径向畸变系数
     * @param k2 二阶径向畸变系数
     * @param k3 三阶径向畸变系数
     * @return 校正后的 Bitmap
     */
    suspend fun correctDistortion(
        bitmap: Bitmap,
        k1: Float = 0f,
        k2: Float = 0f,
        k3: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (k1 == 0f && k2 == 0f && k3 == 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        // 预计算查找表以减少重复计算
        val lutX = FloatArray(width * height)
        val lutY = FloatArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / maxRadius
                val dy = (y - cy) / maxRadius
                val r2 = dx * dx + dy * dy
                val r = sqrt(r2)

                // 径向畸变模型（反向映射）
                val factor = 1f + k1 * r2 + k2 * r2 * r2 + k3 * r2 * r2 * r2
                val correctedDx = dx * factor
                val correctedDy = dy * factor

                val srcX = (correctedDx * maxRadius + cx).toInt()
                val srcY = (correctedDy * maxRadius + cy).toInt()

                lutX[y * width + x] = srcX.toFloat()
                lutY[y * width + x] = srcY.toFloat()
            }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val srcXf = lutX[idx]
                val srcYf = lutY[idx]

                // 双线性插值
                val x0 = srcXf.toInt().coerceIn(0, width - 2)
                val y0 = srcYf.toInt().coerceIn(0, height - 2)
                val x1 = x0 + 1
                val y1 = y0 + 1
                val fx = srcXf - x0
                val fy = srcYf - y0

                val p00 = pixels[y0 * width + x0]
                val p10 = pixels[y0 * width + x1]
                val p01 = pixels[y1 * width + x0]
                val p11 = pixels[y1 * width + x1]

                val a00 = (p00 shr 24) and 0xFF
                val r00 = (p00 shr 16) and 0xFF
                val g00 = (p00 shr 8) and 0xFF
                val b00 = p00 and 0xFF
                val a10 = (p10 shr 24) and 0xFF
                val r10 = (p10 shr 16) and 0xFF
                val g10 = (p10 shr 8) and 0xFF
                val b10 = p10 and 0xFF
                val a01 = (p01 shr 24) and 0xFF
                val r01 = (p01 shr 16) and 0xFF
                val g01 = (p01 shr 8) and 0xFF
                val b01 = p01 and 0xFF
                val a11 = (p11 shr 24) and 0xFF
                val r11 = (p11 shr 16) and 0xFF
                val g11 = (p11 shr 8) and 0xFF
                val b11 = p11 and 0xFF

                val w00 = (1 - fx) * (1 - fy)
                val w10 = fx * (1 - fy)
                val w01 = (1 - fx) * fy
                val w11 = fx * fy

                val a = (a00 * w00 + a10 * w10 + a01 * w01 + a11 * w11).toInt().coerceIn(0, 255)
                val r = (r00 * w00 + r10 * w10 + r01 * w01 + r11 * w11).toInt().coerceIn(0, 255)
                val g = (g00 * w00 + g10 * w10 + g01 * w01 + g11 * w11).toInt().coerceIn(0, 255)
                val b = (b00 * w00 + b10 * w10 + b01 * w01 + b11 * w11).toInt().coerceIn(0, 255)

                outputPixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * 校正横向色差（Lateral Chromatic Aberration）
     * 对红/蓝通道进行缩放以对齐绿通道
     *
     * @param bitmap 原始图像
     * @param redScale 红色通道缩放系数 (1.0 = 不变)
     * @param blueScale 蓝色通道缩放系数 (1.0 = 不变)
     * @return 校正后的 Bitmap
     */
    suspend fun correctChromaticAberration(
        bitmap: Bitmap,
        redScale: Float = 1.0f,
        blueScale: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (redScale == 1.0f && blueScale == 1.0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val cx = width / 2f
        val cy = height / 2f
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val pixel = pixels[idx]

                val g = (pixel shr 8) and 0xFF
                val a = (pixel shr 24) and 0xFF

                // 红色通道偏移
                val dx = x - cx
                val dy = y - cy
                val redSrcX = (cx + dx * redScale).toInt().coerceIn(0, width - 1)
                val redSrcY = (cy + dy * redScale).toInt().coerceIn(0, height - 1)
                val redPixel = pixels[redSrcY * width + redSrcX]
                val r = (redPixel shr 16) and 0xFF

                // 蓝色通道偏移
                val blueSrcX = (cx + dx * blueScale).toInt().coerceIn(0, width - 1)
                val blueSrcY = (cy + dy * blueScale).toInt().coerceIn(0, height - 1)
                val bluePixel = pixels[blueSrcY * width + blueSrcX]
                val b = bluePixel and 0xFF

                outputPixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * 校正暗角（去除镜头暗角，与添加暗角相反）
     * 通过中心提亮来补偿边缘衰减
     *
     * @param bitmap 原始图像
     * @param vignetteStrength 暗角强度 (0~1, 0 = 无暗角, 1 = 强暗角)
     * @param falloff 衰减速度 (1~5, 越大衰减越快)
     * @return 校正后的 Bitmap
     */
    suspend fun correctVignette(
        bitmap: Bitmap,
        vignetteStrength: Float = 0.3f,
        falloff: Float = 2.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (vignetteStrength <= 0f) return@withContext bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val pixel = pixels[idx]

                val dx = (x - cx) / maxRadius
                val dy = (y - cy) / maxRadius
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceIn(0f, 1f)

                // 暗角补偿: 边缘越暗，补偿越多
                // vignette = 1 - strength * dist^falloff
                // correction = 1 / vignette = 1 / (1 - strength * dist^falloff)
                val vignette = 1f - vignetteStrength * dist.pow(falloff)
                val correction = 1f / maxOf(vignette, 0.1f)

                val r = ((pixel shr 16) and 0xFF) / 255f * correction
                val g = ((pixel shr 8) and 0xFF) / 255f * correction
                val b = (pixel and 0xFF) / 255f * correction

                val outRi = (r.coerceIn(0f, 1f) * 255f).toInt()
                val outGi = (g.coerceIn(0f, 1f) * 255f).toInt()
                val outBi = (b.coerceIn(0f, 1f) * 255f).toInt()
                outputPixels[idx] = (0xFF shl 24) or (outRi shl 16) or (outGi shl 8) or outBi
            }
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }
}