package com.livecompose.livecapture.core.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 相框渲染器
 * 将照片渲染上相框和水印，输出最终图片
 */
object FrameRenderer {

    /**
     * 应用相框和水印到照片
     *
     * @param sourceBitmap 原始照片
     * @param frameInfo 相框信息（null 则不应用）
     * @param watermarkInfo 水印信息
     * @param addTimestamp 是否添加时间戳
     * @return 渲染后的最终 Bitmap
     */
    fun applyFrameAndWatermark(
        sourceBitmap: Bitmap,
        frameInfo: FrameInfo?,
        watermarkInfo: WatermarkInfo = WatermarkInfo.EMPTY,
        addTimestamp: Boolean = false
    ): Bitmap {
        val frame = frameInfo ?: return applyWatermark(sourceBitmap, watermarkInfo)

        val srcW = sourceBitmap.width.toFloat()
        val srcH = sourceBitmap.height.toFloat()
        val borderW = srcW * frame.borderWidthPercent
        val innerPad = srcW * frame.innerPaddingPercent
        val totalBorder = borderW + innerPad

        // 计算输出尺寸（原图 + 双倍边框）
        val outW = (srcW + totalBorder * 2).toInt()
        val outH = (srcH + totalBorder * 2).toInt()

        val output = Bitmap.createBitmap(outW.coerceAtLeast(1), outH.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 绘制背景色
        canvas.drawColor(frame.backgroundColor.toInt())

        // 绘制原图
        val destRect = RectF(totalBorder, totalBorder, totalBorder + srcW, totalBorder + srcH)
        canvas.drawBitmap(sourceBitmap, null, destRect, null)

        // 绘制边框线
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = frame.borderColor.toInt()
            strokeWidth = borderW.coerceAtLeast(1f)
            style = Paint.Style.STROKE
        }
        val frameRect = RectF(totalBorder / 2, totalBorder / 2, outW - totalBorder / 2, outH - totalBorder / 2)
        canvas.drawRect(frameRect, borderPaint)

        // 徕卡特殊处理：绘制红色圆点
        if (frame.id == "leica") {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                style = Paint.Style.FILL
            }
            canvas.drawCircle(totalBorder + srcW - 20f, totalBorder + 20f, 8f, dotPaint)
        }

        // 应用水印
        val withWatermark = applyWatermark(output, watermarkInfo)

        // 时间戳
        if (addTimestamp || frame.id == "timestamp") {
            return applyTimestamp(withWatermark)
        }

        return withWatermark
    }

    /**
     * 仅应用水印
     */
    private fun applyWatermark(bitmap: Bitmap, watermarkInfo: WatermarkInfo): Bitmap {
        if (!watermarkInfo.isEnabled || watermarkInfo.text.isBlank()) return bitmap

        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val typeface = if (watermarkInfo.fontName == "ds-digital") {
            try {
                Typeface.createFromAsset(android.app.Application::class.java.getClassLoader()?.load("assets")?.let {
                    // In actual usage, context would be passed; fallback to default
                    null
                }) ?: Typeface.MONOSPACE
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
        } else {
            Typeface.DEFAULT
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = watermarkInfo.textColor
            textSize = watermarkInfo.textSize * bitmap.density
            typeface = this.typeface
            textAlign = watermarkInfo.positionX.toPaintAlign()
            alpha = (watermarkInfo.alpha * 255).toInt()
        }

        // 计算 X 坐标
        val x = when (watermarkInfo.positionX) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.CENTER_LEFT, WatermarkPosition.BOTTOM_LEFT -> watermarkInfo.marginDp * bitmap.density
            WatermarkPosition.TOP_CENTER, WatermarkPosition.CENTER_CENTER, WatermarkPosition.BOTTOM_CENTER -> bitmap.width / 2f
            else -> bitmap.width - watermarkInfo.marginDp * bitmap.density
        }

        // 计算 Y 坐标
        val y = watermarkInfo.positionY * bitmap.height

        // 保存并旋转画布
        canvas.save()
        canvas.rotate(watermarkInfo.rotationDegrees, x, y)
        canvas.drawText(watermarkInfo.text, x, y, paint)
        canvas.restore()

        return output
    }

    /**
     * 添加时间戳
     */
    private fun applyTimestamp(bitmap: Bitmap): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // 底部时间戳背景条
        val barHeight = 28f * bitmap.density
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FF6B00")
            alpha = 200
        }
        canvas.drawRect(0f, bitmap.height - barHeight, bitmap.width.toFloat(), bitmap.height.toFloat(), bgPaint)

        // 时间文字
        val timeText = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f * bitmap.density
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText(timeText, 16f * bitmap.density, bitmap.height - 8f * bitmap.density, textPaint)

        // 设备标识
        deviceText = "LiveCompose"
        canvas.drawText(deviceText, bitmap.width - 90f * bitmap.density, bitmap.height - 8f * bitmap.density, textPaint)

        return output
    }
}
