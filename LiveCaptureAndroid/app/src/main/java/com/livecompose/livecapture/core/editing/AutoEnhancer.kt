package com.livecompose.livecapture.core.editing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.portrait.PortraitImageUtils

/**
 * 直方图分析结果
 *
 * 对应 iOS 端 HistogramData，记录图像直方图的统计量。
 *
 * @property min 最小像素值（0-255）
 * @property max 最大像素值（0-255）
 * @property mean 平均像素值（0-255）
 * @property median 中位数像素值（0-255）
 * @property isUnderexposed 是否曝光不足
 * @property isOverexposed 是否过曝
 * @property avgR 红通道平均值
 * @property avgG 绿通道平均值
 * @property avgB 蓝通道平均值
 */
data class HistogramData(
    val min: Float,
    val max: Float,
    val mean: Float,
    val median: Float,
    val isUnderexposed: Boolean,
    val isOverexposed: Boolean,
    val avgR: Float,
    val avgG: Float,
    val avgB: Float
)

/**
 * 自动增强器
 *
 * 对应 iOS 端 AutoEnhancer.swift，基于图像直方图分析的自适应增强，
 * 自动执行白平衡、色阶、曝光补偿和锐化。
 *
 * ## 处理流程
 * 1. [analyzeHistogram] 分析像素分布
 * 2. [autoWhiteBalance] 灰度世界假设法自动白平衡
 * 3. [autoLevels] 直方图拉伸实现自动色阶
 * 4. [smartExposure] 智能曝光补偿
 * 5. [adaptiveSharpening] 自适应锐化
 */
class AutoEnhancer {

    companion object {
        private const val TAG = "AutoEnhancer"
        private const val HISTOGRAM_BINS = 256
    }

    /**
     * 一键自动增强图像
     *
     * @param image 输入 Bitmap
     * @return 增强后的 Bitmap，失败返回原图
     */
    fun autoEnhance(image: Bitmap): Bitmap {
        return try {
            val histogram = analyzeHistogram(image)
            var output = autoWhiteBalance(image, histogram)
            output = autoLevels(output, histogram)
            output = smartExposure(output, histogram)
            output = adaptiveSharpening(output, histogram)
            output
        } catch (e: Exception) {
            AppLogger.e(TAG, "自动增强失败", e)
            image
        }
    }

    /** 别名，与 [autoEnhance] 等价 */
    fun autoEnhanceBitmap(image: Bitmap): Bitmap = autoEnhance(image)

    /**
     * 分析图像直方图
     *
     * 使用 [Bitmap.getPixels] 手动统计 R/G/B 三通道 256 桶直方图，
     * 并计算最小值、最大值、平均值、中位数等统计量。
     *
     * @param image 输入 Bitmap
     * @return 直方图分析结果
     */
    fun analyzeHistogram(image: Bitmap): HistogramData {
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) {
            return defaultHistogram()
        }

        // 降采样以加速（大图统计无需全像素）
        val maxSamples = 256 * 256
        val sampleStep = if (width * height > maxSamples) {
            Math.ceil(Math.sqrt((width * height).toDouble() / maxSamples)).toInt().coerceAtLeast(1)
        } else 1

        val rCounts = IntArray(HISTOGRAM_BINS)
        val gCounts = IntArray(HISTOGRAM_BINS)
        val bCounts = IntArray(HISTOGRAM_BINS)

        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalSamples = 0
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val c = pixels[y * width + x]
                rCounts[Color.red(c)]++
                gCounts[Color.green(c)]++
                bCounts[Color.blue(c)]++
                totalSamples++
                x += sampleStep
            }
            y += sampleStep
        }

        if (totalSamples == 0) return defaultHistogram()

        val avgR = computeAverageChannel(rCounts, totalSamples)
        val avgG = computeAverageChannel(gCounts, totalSamples)
        val avgB = computeAverageChannel(bCounts, totalSamples)

        // 合并三通道为总亮度直方图
        val totalCounts = IntArray(HISTOGRAM_BINS)
        for (i in 0 until HISTOGRAM_BINS) {
            totalCounts[i] = rCounts[i] + gCounts[i] + bCounts[i]
        }

        // 最小非零值
        var minVal = 0
        for (i in 0 until HISTOGRAM_BINS) {
            if (totalCounts[i] > 0) { minVal = i; break }
        }

        // 最大非零值
        var maxVal = 255
        for (i in HISTOGRAM_BINS - 1 downTo 0) {
            if (totalCounts[i] > 0) { maxVal = i; break }
        }

        // 平均值
        var sum = 0L
        for (i in 0 until HISTOGRAM_BINS) {
            sum += i.toLong() * totalCounts[i]
        }
        val totalPixels = totalSamples * 3L
        val mean = if (totalPixels > 0) sum.toFloat() / totalPixels else 128f

        // 中位数
        val halfCount = totalPixels / 2
        var cumulative = 0L
        var medianVal = 128
        for (i in 0 until HISTOGRAM_BINS) {
            cumulative += totalCounts[i]
            if (cumulative >= halfCount) { medianVal = i; break }
        }

        val isUnderexposed = mean < 85f
        val isOverexposed = mean > 170f

        return HistogramData(
            min = minVal.toFloat(),
            max = maxVal.toFloat(),
            mean = mean,
            median = medianVal.toFloat(),
            isUnderexposed = isUnderexposed,
            isOverexposed = isOverexposed,
            avgR = avgR,
            avgG = avgG,
            avgB = avgB
        )
    }

    /** 默认直方图（图像无效时返回） */
    private fun defaultHistogram(): HistogramData {
        return HistogramData(
            min = 0f, max = 255f, mean = 128f, median = 128f,
            isUnderexposed = false, isOverexposed = false,
            avgR = 128f, avgG = 128f, avgB = 128f
        )
    }

    /** 计算单通道平均值 */
    private fun computeAverageChannel(counts: IntArray, totalSamples: Int): Float {
        var sum = 0L
        for (i in counts.indices) {
            sum += i.toLong() * counts[i]
        }
        return if (totalSamples > 0) sum.toFloat() / totalSamples else 128f
    }

    /**
     * 自动白平衡（灰度世界假设法）
     *
     * 假设图像所有颜色的平均值应为灰色，据此计算各通道增益并应用 ColorMatrix。
     *
     * @param image 输入 Bitmap
     * @param histogram 直方图分析结果
     * @return 白平衡后的 Bitmap
     */
    fun autoWhiteBalance(image: Bitmap, histogram: HistogramData): Bitmap {
        val avgGray = (histogram.avgR + histogram.avgG + histogram.avgB) / 3.0f
        if (avgGray <= 0f) return image

        var rGain = avgGray / histogram.avgR.coerceAtLeast(1f)
        var gGain = avgGray / histogram.avgG.coerceAtLeast(1f)
        var bGain = avgGray / histogram.avgB.coerceAtLeast(1f)

        // 限制增益范围，避免过度修正
        rGain = rGain.coerceIn(0.5f, 2.0f)
        gGain = gGain.coerceIn(0.5f, 2.0f)
        bGain = bGain.coerceIn(0.5f, 2.0f)

        val rBias = (1.0f - rGain) * 0.5f
        val gBias = (1.0f - gGain) * 0.5f
        val bBias = (1.0f - bGain) * 0.5f

        // ColorMatrix 4x5：[a,b,c,d,e, f,g,h,i,j, k,l,m,n,o, p,q,r,s,t]
        // R' = rGain*R + 0*G + 0*B + 0*A + rBias*255
        val matrix = ColorMatrix(floatArrayOf(
            rGain, 0f, 0f, 0f, rBias * 255f,
            0f, gGain, 0f, 0f, gBias * 255f,
            0f, 0f, bGain, 0f, bBias * 255f,
            0f, 0f, 0f, 1f, 0f
        ))

        return applyColorMatrix(image, matrix)
    }

    /**
     * 自动色阶（直方图拉伸）
     *
     * 将 [HistogramData.min, HistogramData.max] 范围映射到 [0, 255]。
     *
     * @param image 输入 Bitmap
     * @param histogram 直方图分析结果
     * @return 色阶调整后的 Bitmap
     */
    fun autoLevels(image: Bitmap, histogram: HistogramData): Bitmap {
        val minVal = histogram.min
        val maxVal = histogram.max

        // 如果直方图范围已经足够宽，不需要拉伸
        if (maxVal <= minVal || (maxVal - minVal) >= 200f) return image

        val scale = 255.0f / (maxVal - minVal)
        val contrastAdjust = (scale / 255.0f - 1.0f) * 0.5f
        val brightnessAdjust = (-minVal * scale / 255.0f) * 0.5f

        val contrast = 1.0f + contrastAdjust
        val brightness = brightnessAdjust

        return PortraitImageUtils.adjustColorControls(
            image,
            contrast = contrast,
            saturation = 1.0f,
            brightness = brightness
        )
    }

    /**
     * 智能曝光补偿
     *
     * 根据直方图平均值判断曝光不足或过曝，应用相应的 EV 调整。
     *
     * @param image 输入 Bitmap
     * @param histogram 直方图分析结果
     * @return 曝光调整后的 Bitmap
     */
    fun smartExposure(image: Bitmap, histogram: HistogramData): Bitmap {
        var ev = 0f

        if (histogram.isUnderexposed) {
            // 曝光不足：提升曝光，最多 +1.5 EV
            ev = (85.0f - histogram.mean) / 85.0f * 1.5f
            ev = ev.coerceAtMost(1.5f)
        } else if (histogram.isOverexposed) {
            // 过曝：降低曝光，最多 -1.0 EV
            ev = (170.0f - histogram.mean) / 85.0f * 1.0f
            ev = ev.coerceAtLeast(-1.0f)
        }

        if (Math.abs(ev) <= 0.05f) return image

        return PortraitImageUtils.adjustExposure(image, ev)
    }

    /**
     * 自适应锐化（根据图像对比度调整锐化强度）
     *
     * 使用 Unsharp Mask（反锐化掩模）算法：result = original + amount * (original - blurred)
     *
     * @param image 输入 Bitmap
     * @param histogram 直方图分析结果
     * @return 锐化后的 Bitmap
     */
    fun adaptiveSharpening(image: Bitmap, histogram: HistogramData): Bitmap {
        val contrast = (histogram.max - histogram.min) / 255.0f
        val sharpnessAmount: Float = when {
            contrast < 0.3f -> 0.6f   // 低对比度 → 强锐化
            contrast < 0.6f -> 0.4f   // 中等对比度 → 中等锐化
            else -> 0.2f              // 高对比度 → 弱锐化
        }

        return unsharpMask(image, sharpnessAmount, 2)
    }

    /**
     * Unsharp Mask 锐化
     *
     * @param image 输入
     * @param amount 锐化强度（0.0 - 1.0）
     * @param radius 模糊半径
     * @return 锐化后的 Bitmap
     */
    private fun unsharpMask(image: Bitmap, amount: Float, radius: Int): Bitmap {
        if (amount <= 0f) return image
        val width = image.width
        val height = image.height

        // 模糊图像
        val blurred = boxBlur(image, radius)

        val origPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        image.getPixels(origPixels, 0, width, 0, 0, width, height)
        blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val resultPixels = IntArray(width * height)

        val invAmount = 1.0f - amount
        for (i in origPixels.indices) {
            val oc = origPixels[i]
            val bc = blurPixels[i]
            val r = (Color.red(oc) * invAmount + (Color.red(oc) + (Color.red(oc) - Color.red(bc))) * amount)
                .toInt().coerceIn(0, 255)
            val g = (Color.green(oc) * invAmount + (Color.green(oc) + (Color.green(oc) - Color.green(bc))) * amount)
                .toInt().coerceIn(0, 255)
            val b = (Color.blue(oc) * invAmount + (Color.blue(oc) + (Color.blue(oc) - Color.blue(bc))) * amount)
                .toInt().coerceIn(0, 255)
            resultPixels[i] = Color.argb(Color.alpha(oc), r, g, b)
        }
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)

        if (blurred !== image) blurred.recycle()
        return result
    }

    /**
     * 应用 ColorMatrix 到 Bitmap
     */
    private fun applyColorMatrix(image: Bitmap, matrix: ColorMatrix): Bitmap {
        val width = image.width
        val height = image.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(image, 0f, 0f, paint)
        return result
    }

    /**
     * 盒式模糊（近似高斯）
     */
    private fun boxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        val temp = pixels.copyOf()
        val r = radius.coerceAtLeast(1)

        // 水平方向
        for (y in 0 until height) {
            for (x in 0 until width) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (kx in -r..r) {
                    val px = (x + kx).coerceIn(0, width - 1)
                    val c = temp[y * width + px]
                    rSum += Color.red(c); gSum += Color.green(c); bSum += Color.blue(c)
                    count++
                }
                pixels[y * width + x] = Color.rgb(rSum / count, gSum / count, bSum / count)
            }
        }
        // 垂直方向
        temp.copyInto(pixels)
        for (x in 0 until width) {
            for (y in 0 until height) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (ky in -r..r) {
                    val py = (y + ky).coerceIn(0, height - 1)
                    val c = pixels[py * width + x]
                    rSum += Color.red(c); gSum += Color.green(c); bSum += Color.blue(c)
                    count++
                }
                temp[y * width + x] = Color.rgb(rSum / count, gSum / count, bSum / count)
            }
        }
        result.setPixels(temp, 0, width, 0, 0, width, height)
        return result
    }
}
