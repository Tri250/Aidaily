package com.livecompose.livecapture.core.portrait

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

/**
 * 人像图像处理工具集
 *
 * 提供 Gaussian 模糊、掩码生成、掩码混合等通用图像处理能力，
 * 对应 iOS 端 CoreImage 的 CIFilter 链式处理能力。
 *
 * ## 技术栈
 * - RenderScript ScriptIntrinsicBlur：硬件加速高斯模糊
 * - Canvas + Paint + PorterDuffXfermode：掩码混合
 * - ColorMatrix：颜色调整（曝光、饱和度、色温）
 */
internal object PortraitImageUtils {

    /**
     * 高斯模糊（使用 RenderScript 硬件加速，需先调用 [initRenderScript]）
     *
     * @param bitmap 输入 Bitmap
     * @param radius 模糊半径（0-25）
     * @return 模糊后的 Bitmap
     */
    fun gaussianBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val clampedRadius = radius.coerceIn(0f, 25f)
        if (clampedRadius <= 0f) return bitmap
        val rs = rsContext ?: return gaussianBlurFallback(bitmap, clampedRadius)
        return try {
            val input = Allocation.createFromBitmap(rs, bitmap)
            val output = Allocation.createTyped(rs, input.type)
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            script.setRadius(clampedRadius)
            script.setInput(input)
            script.forEach(output)
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            output.copyTo(result)
            input.destroy()
            output.destroy()
            script.destroy()
            result
        } catch (e: Exception) {
            gaussianBlurFallback(bitmap, clampedRadius)
        }
    }

    /** RenderScript 上下文（需在初始化时设置） */
    private var rsContext: RenderScript? = null

    /**
     * 初始化 RenderScript（必须在主线程调用，使用应用 Context）
     */
    fun initRenderScript(context: android.content.Context) {
        if (rsContext == null) {
            rsContext = RenderScript.create(context.applicationContext)
        }
    }

    /**
     * 纯 Java 实现的高斯模糊（回退方案，性能较低）
     */
    private fun gaussianBlurFallback(bitmap: Bitmap, radius: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        // 简化的盒式模糊（近似高斯）
        val r = radius.toInt().coerceAtLeast(1)
        val temp = pixels.copyOf()

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

    /**
     * 创建椭圆形掩码 Bitmap
     *
     * @param width 画布宽度
     * @param height 画布高度
     * @param rects 椭圆区域列表（图像坐标系）
     * @param blurSigma 掩码边缘羽化半径
     * @return 掩码 Bitmap（白色=选中区域，黑色=其他）
     */
    fun createOvalMask(
        width: Int,
        height: Int,
        rects: List<RectF>,
        blurSigma: Float = 15f
    ): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.WHITE
        for (rect in rects) {
            canvas.drawOval(rect, paint)
        }
        return if (blurSigma > 0f) gaussianBlur(mask, blurSigma) else mask
    }

    /**
     * 使用掩码混合两个 Bitmap
     *
     * @param foreground 前景图（掩码白色区域显示）
     * @param background 背景图（掩码黑色区域显示）
     * @param mask 掩码（灰度图）
     * @return 混合后的 Bitmap
     */
    fun blendWithMask(foreground: Bitmap, background: Bitmap, mask: Bitmap): Bitmap {
        val width = background.width
        val height = background.height
        // 将前景缩放到背景尺寸
        val scaledForeground = if (foreground.width == width && foreground.height == height) {
            foreground
        } else {
            Bitmap.createScaledBitmap(foreground, width, height, true)
        }
        val scaledMask = if (mask.width == width && mask.height == height) {
            mask
        } else {
            Bitmap.createScaledBitmap(mask, width, height, true)
        }

        val result = scaledForeground.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(scaledMask, 0f, 0f, paint)

        // 绘制背景，使用 SRC_ATOP 让背景在透明区域显示
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        // 实际上需要在结果上叠加：先画背景，再画前景
        // 重新实现：先画背景，再用 mask 画前景
        val finalResult = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalResult)
        val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        finalCanvas.drawBitmap(background, 0f, 0f, bgPaint)

        // 在背景上绘制前景，使用 mask 控制透明度
        val fgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        // 先把前景处理成带 mask 的图
        val maskedFg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskedCanvas = Canvas(maskedFg)
        maskedCanvas.drawBitmap(scaledForeground, 0f, 0f, bgPaint)
        maskedCanvas.drawBitmap(scaledMask, 0f, 0f, fgPaint)

        // 将带 mask 的前景叠加到背景
        val overlayPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        finalCanvas.drawBitmap(maskedFg, 0f, 0f, overlayPaint)

        return finalResult
    }

    /**
     * 调整曝光（EV）
     *
     * @param bitmap 输入
     * @param ev 曝光补偿值（-2 到 +2）
     * @return 调整后的 Bitmap
     */
    fun adjustExposure(bitmap: Bitmap, ev: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        // EV 调整通过亮度乘数实现
        val factor = Math.pow(2.0, ev.toDouble()).toFloat()
        val matrix = android.graphics.ColorMatrix().apply {
            setScale(factor, factor, factor, 1f)
        }
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    /**
     * 调整对比度和饱和度
     *
     * @param bitmap 输入
     * @param contrast 对比度（1.0=原始）
     * @param saturation 饱和度（1.0=原始，0=灰度）
     * @param brightness 亮度（0=原始，正值提亮，负值压暗）
     * @return 调整后的 Bitmap
     */
    fun adjustColorControls(
        bitmap: Bitmap,
        contrast: Float = 1f,
        saturation: Float = 1f,
        brightness: Float = 0f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        // 对比度矩阵
        val contrastMatrix = android.graphics.ColorMatrix().apply {
            val scale = contrast
            val translate = (-0.5f * scale + 0.5f) * 255f + brightness * 255f
            setScale(scale, scale, scale, 1f)
            postConcat(android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, translate,
                0f, 1f, 0f, 0f, translate,
                0f, 0f, 1f, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        // 饱和度矩阵
        val saturationMatrix = android.graphics.ColorMatrix().apply {
            setSaturation(saturation)
        }
        contrastMatrix.postConcat(saturationMatrix)

        paint.colorFilter = android.graphics.ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    /**
     * 色温调整
     *
     * @param bitmap 输入
     * @param temperature 色温偏移（负值=冷色，正值=暖色）
     * @return 调整后的 Bitmap
     */
    fun adjustTemperature(bitmap: Bitmap, temperature: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        // 暖色：增加红，减少蓝；冷色：减少红，增加蓝
        val redAdjust = 1f + temperature * 0.1f
        val blueAdjust = 1f - temperature * 0.1f
        val matrix = android.graphics.ColorMatrix().apply {
            setScale(redAdjust, 1f, blueAdjust, 1f)
        }
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    /**
     * 释放 RenderScript 资源
     */
    fun release() {
        rsContext?.apply {
            try { destroy() } catch (_: Exception) {}
        }
        rsContext = null
    }
}
