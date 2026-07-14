package com.livecompose.livecapture.core.storage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印服务
 * 负责为 Bitmap 添加水印，支持文字水印、时间戳水印和位置水印
 */
@Singleton
class WatermarkService @Inject constructor() {

    companion object {
        private const val TAG = "WatermarkService"

        // 默认水印文字
        private const val DEFAULT_WATERMARK_TEXT = "构妙 LiveCapture"

        // 水印配置
        private const val WATERMARK_PADDING_DP = 16f      // 水印与边缘的内边距
        private const val WATERMARK_TEXT_SIZE_SP = 14f    // 主水印文字大小
        private const val WATERMARK_TIMESTAMP_SIZE_SP = 12f // 时间戳文字大小
        private const val WATERMARK_LOCATION_SIZE_SP = 10f  // 位置信息文字大小
        private const val WATERMARK_LINE_SPACING = 8f     // 行间距
        private const val WATERMARK_SHADOW_RADIUS = 4f    // 阴影半径
        private const val WATERMARK_ALPHA = 200           // 水印透明度 (0-255)
    }

    /**
     * 为 Bitmap 添加水印
     *
     * @param bitmap 原始 Bitmap
     * @param watermarkText 水印文字，默认为 "构妙 LiveCapture"
     * @param showTimestamp 是否显示时间戳
     * @param locationInfo 位置信息（可选），格式如 "北京市朝阳区"
     * @return 添加水印后的 Bitmap
     */
    fun addWatermark(
        bitmap: Bitmap,
        watermarkText: String = DEFAULT_WATERMARK_TEXT,
        showTimestamp: Boolean = true,
        locationInfo: String? = null
    ): Bitmap {
        // 创建可变的 Bitmap 副本
        val watermarkedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(watermarkedBitmap)

        // 计算缩放比例，根据图片宽度调整字体大小
        val scaleFactor = bitmap.width / 1080f.coerceAtLeast(1f)
        val baseTextSize = WATERMARK_TEXT_SIZE_SP * scaleFactor * 2.5f
        val timestampSize = WATERMARK_TIMESTAMP_SIZE_SP * scaleFactor * 2.5f
        val locationSize = WATERMARK_LOCATION_SIZE_SP * scaleFactor * 2.5f
        val padding = WATERMARK_PADDING_DP * scaleFactor * 2.5f
        val lineSpacing = WATERMARK_LINE_SPACING * scaleFactor * 2.5f

        // 准备水印文字列表
        val watermarkLines = mutableListOf<String>()
        watermarkLines.add(watermarkText)

        if (showTimestamp) {
            val timestamp = formatTimestamp(System.currentTimeMillis())
            watermarkLines.add(timestamp)
        }

        if (!locationInfo.isNullOrEmpty()) {
            watermarkLines.add(locationInfo)
        }

        // 绘制水印背景（半透明矩形，增强可读性）
        val totalHeight = calculateTotalHeight(watermarkLines, baseTextSize, timestampSize, locationSize, lineSpacing)
        val maxWidth = calculateMaxWidth(canvas, watermarkLines, baseTextSize, timestampSize, locationSize)

        val backgroundRectWidth = maxWidth + padding * 2
        val backgroundRectHeight = totalHeight + padding * 2

        val backgroundPaint = Paint().apply {
            color = Color.parseColor("#80000000") // 半透明黑色
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val rectLeft = bitmap.width - backgroundRectWidth - padding
        val rectTop = bitmap.height - backgroundRectHeight - padding
        val rectRight = bitmap.width - padding
        val rectBottom = bitmap.height - padding

        canvas.drawRoundRect(
            rectLeft,
            rectTop,
            rectRight,
            rectBottom,
            padding / 2,
            padding / 2,
            backgroundPaint
        )

        // 绘制水印文字
        var currentY = rectBottom - padding

        // 从下往上绘制
        val reversedLines = watermarkLines.reversed()
        for ((index, line) in reversedLines.withIndex()) {
            val textSize = when {
                index == reversedLines.size - 1 -> baseTextSize
                index == reversedLines.size - 2 && showTimestamp -> timestampSize
                else -> locationSize
            }

            val paint = createWatermarkPaint(textSize)

            // 计算文字位置（右对齐）
            val textWidth = paint.measureText(line)
            val textX = bitmap.width - padding - textWidth - padding

            canvas.drawText(line, textX, currentY, paint)
            currentY -= (textSize + lineSpacing)
        }

        return watermarkedBitmap
    }

    /**
     * 创建水印画笔
     * 使用中文友好的字体和样式
     */
    private fun createWatermarkPaint(textSize: Float): Paint {
        return Paint().apply {
            // 设置抗锯齿
            isAntiAlias = true
            isFilterBitmap = true

            // 设置文字大小
            this.textSize = textSize

            // 设置颜色（白色，带透明度）
            color = Color.WHITE
            alpha = WATERMARK_ALPHA

            // 设置字体样式 - 使用粗体增强可读性
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            // 设置文字阴影，增强对比度
            setShadowLayer(
                WATERMARK_SHADOW_RADIUS,
                1f,
                1f,
                Color.parseColor("#60000000")
            )

            // 设置文字对齐方式
            textAlign = Paint.Align.LEFT

            // 设置下划线和删除线
            isUnderlineText = false
            isStrikeThruText = false
        }
    }

    /**
     * 计算水印总高度
     */
    private fun calculateTotalHeight(
        lines: List<String>,
        baseSize: Float,
        timestampSize: Float,
        locationSize: Float,
        lineSpacing: Float
    ): Float {
        var totalHeight = 0f
        for ((index, _) in lines.withIndex()) {
            val textSize = when {
                index == 0 -> baseSize
                index == 1 -> timestampSize
                else -> locationSize
            }
            totalHeight += textSize
            if (index < lines.size - 1) {
                totalHeight += lineSpacing
            }
        }
        return totalHeight
    }

    /**
     * 计算最大文字宽度
     */
    private fun calculateMaxWidth(
        canvas: Canvas,
        lines: List<String>,
        baseSize: Float,
        timestampSize: Float,
        locationSize: Float
    ): Float {
        var maxWidth = 0f
        for ((index, line) in lines.withIndex()) {
            val textSize = when {
                index == 0 -> baseSize
                index == 1 -> timestampSize
                else -> locationSize
            }
            val paint = Paint().apply {
                this.textSize = textSize
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val width = paint.measureText(line)
            if (width > maxWidth) {
                maxWidth = width
            }
        }
        return maxWidth
    }

    /**
     * 格式化时间戳
     * 使用中文友好的日期时间格式
     */
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to format timestamp", e)
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        }
    }

    /**
     * 为 Bitmap 添加水印（简化版本，仅添加默认水印）
     *
     * @param bitmap 原始 Bitmap
     * @return 添加水印后的 Bitmap
     */
    fun addWatermarkSimple(bitmap: Bitmap): Bitmap {
        return addWatermark(
            bitmap = bitmap,
            watermarkText = DEFAULT_WATERMARK_TEXT,
            showTimestamp = true,
            locationInfo = null
        )
    }

    /**
     * 为 Bitmap 添加完整水印（包含所有信息）
     *
     * @param bitmap 原始 Bitmap
     * @param locationInfo 位置信息
     * @return 添加水印后的 Bitmap
     */
    fun addWatermarkFull(
        bitmap: Bitmap,
        locationInfo: String?
    ): Bitmap {
        return addWatermark(
            bitmap = bitmap,
            watermarkText = DEFAULT_WATERMARK_TEXT,
            showTimestamp = true,
            locationInfo = locationInfo
        )
    }

    /**
     * 为 Bitmap 添加自定义水印
     *
     * @param bitmap 原始 Bitmap
     * @param customText 自定义水印文字
     * @param showTimestamp 是否显示时间戳
     * @return 添加水印后的 Bitmap
     */
    fun addCustomWatermark(
        bitmap: Bitmap,
        customText: String,
        showTimestamp: Boolean = false
    ): Bitmap {
        return addWatermark(
            bitmap = bitmap,
            watermarkText = customText,
            showTimestamp = showTimestamp,
            locationInfo = null
        )
    }
}