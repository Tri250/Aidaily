package com.livecompose.livecapture.core.frame

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        // 如果水印产生了新 Bitmap，回收原始 output
        if (withWatermark !== output) {
            output.recycle()
        }

        // 时间戳
        if (addTimestamp || frame.id == "timestamp") {
            val withTimestamp = applyTimestamp(withWatermark)
            // 如果时间戳产生了新 Bitmap，回收 withWatermark
            if (withTimestamp !== withWatermark) {
                withWatermark.recycle()
            }
            return withTimestamp
        }

        return withWatermark
    }

    /**
     * 仅应用水印（文字水印 + 图片水印）
     */
    private fun applyWatermark(bitmap: Bitmap, watermarkInfo: WatermarkInfo): Bitmap {
        if (!watermarkInfo.isEnabled) return bitmap

        val hasText = watermarkInfo.text.isNotBlank()
        val hasLogo = watermarkInfo.logoBitmapPath != null
        if (!hasText && !hasLogo) return bitmap

        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // 绘制文字水印
        if (hasText) {
            drawTextWatermark(canvas, bitmap, watermarkInfo)
        }

        // 绘制图片水印
        if (hasLogo) {
            drawLogoWatermark(canvas, bitmap, watermarkInfo)
        }

        return output
    }

    /**
     * 绘制文字水印
     */
    private fun drawTextWatermark(canvas: Canvas, bitmap: Bitmap, watermarkInfo: WatermarkInfo) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = watermarkInfo.textColor
            textSize = watermarkInfo.textSize * bitmap.density
            typeface = if (watermarkInfo.fontName == "ds-digital") {
                try {
                    Typeface.MONOSPACE
                } catch (_: Exception) {
                    Typeface.DEFAULT_BOLD
                }
            } else {
                Typeface.DEFAULT
            }
            textAlign = watermarkInfo.positionX.toPaintAlign()
            alpha = (watermarkInfo.alpha * 255).toInt()
        }

        val x = when (watermarkInfo.positionX) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.CENTER_LEFT, WatermarkPosition.BOTTOM_LEFT -> watermarkInfo.marginDp * bitmap.density
            WatermarkPosition.TOP_CENTER, WatermarkPosition.CENTER_CENTER, WatermarkPosition.BOTTOM_CENTER -> bitmap.width / 2f
            else -> bitmap.width - watermarkInfo.marginDp * bitmap.density
        }

        val y = watermarkInfo.positionY * bitmap.height

        canvas.save()
        canvas.rotate(watermarkInfo.rotationDegrees, x, y)
        canvas.drawText(watermarkInfo.text, x, y, paint)
        canvas.restore()
    }

    /**
     * 绘制图片水印
     */
    private fun drawLogoWatermark(canvas: Canvas, bitmap: Bitmap, watermarkInfo: WatermarkInfo) {
        val path = watermarkInfo.logoBitmapPath ?: return
        val logoBitmap = try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            return
        } ?: return

        // 计算水印大小: 相对于图片宽度
        val logoWidth = (bitmap.width * watermarkInfo.logoScale).toInt().coerceAtLeast(1)
        val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
        val logoHeight = (logoWidth / aspectRatio).toInt().coerceAtLeast(1)

        val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoWidth, logoHeight, true)
        if (scaledLogo !== logoBitmap && !logoBitmap.isRecycled) {
            logoBitmap.recycle()
        }

        // 计算位置
        val marginPx = watermarkInfo.marginDp * bitmap.density
        val x = when (watermarkInfo.positionX) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.CENTER_LEFT, WatermarkPosition.BOTTOM_LEFT -> marginPx
            WatermarkPosition.TOP_CENTER, WatermarkPosition.CENTER_CENTER, WatermarkPosition.BOTTOM_CENTER -> (bitmap.width - logoWidth) / 2f
            else -> bitmap.width - logoWidth - marginPx
        }
        val y = watermarkInfo.positionY * bitmap.height - logoHeight / 2f

        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (watermarkInfo.logoAlpha * 255).toInt()
        }

        canvas.save()
        canvas.rotate(watermarkInfo.rotationDegrees, x + logoWidth / 2f, y + logoHeight / 2f)
        canvas.drawBitmap(scaledLogo, x, y, logoPaint)
        canvas.restore()
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
        val deviceText = "LiveCompose"
        canvas.drawText(deviceText, bitmap.width - 90f * bitmap.density, bitmap.height - 8f * bitmap.density, textPaint)

        return output
    }
}
