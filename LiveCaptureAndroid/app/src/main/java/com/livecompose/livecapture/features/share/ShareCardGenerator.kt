package com.livecompose.livecapture.features.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 分享卡片生成器
 * 对标 iOS ShareCardGenerator：生成精美的照片分享卡片
 *
 * 卡片规格：
 * - 尺寸：1080 × 1440（3:4 比例）
 * - 圆角：24dp
 * - 白底 + 照片区域灰色边框
 *
 * 内容布局：
 * 1. 照片区域（居中留白）
 * 2. App Logo（透明玻璃质感）
 * 3. 标题：构妙 · LiveCompose
 * 4. 拍摄日期
 * 5. 参数行：检测引擎 · ISO · 快门 · 光圈 · 分辨率
 * 6. 底部分隔线
 */
object ShareCardGenerator {

    private const val cardWidth = 1080
    private const val cardAspectRatio = 3.0f / 4.0f
    private val cardHeight get() = (cardWidth / cardAspectRatio).toInt()
    private const val cornerRadius = 24f
    private const val photoInsetHorizontal = 80f
    private const val photoInsetVertical = 72f
    private const val topPadding = 120f
    private const val bottomReserved = 300f
    private const val maxPhotoDimension = 1920f

    /**
     * 生成分享卡片
     *
     * @param photo 原始照片
     * @param logo 品牌 Logo（可选）
     * @param date 拍摄日期
     * @param detectionMethod 检测引擎名称
     * @param iso ISO 值
     * @param shutterSpeed 快门速度
     * @param aperture 光圈值
     * @param imageWidth 图片宽度
     * @param imageHeight 图片高度
     */
    fun generate(
        photo: Bitmap,
        logo: Bitmap? = null,
        date: Date = Date(),
        detectionMethod: String? = null,
        iso: Float? = null,
        shutterSpeed: Double? = null,
        aperture: Double? = null,
        imageWidth: Int? = null,
        imageHeight: Int? = null
    ): Bitmap {
        val scaledPhoto = scalePhoto(photo)
        val cardWidthF = cardWidth.toFloat()
        val cardHeightF = cardHeight.toFloat()

        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 白色背景 + 圆角裁剪
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val bgPath = Path().apply {
            addRoundRect(RectF(0f, 0f, cardWidthF, cardHeightF), cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.drawPath(bgPath, bgPaint)

        // 照片区域
        val photoAreaWidth = cardWidthF - photoInsetHorizontal * 2
        val photoAspect = scaledPhoto.width.toFloat() / scaledPhoto.height.toFloat()
        var drawWidth = photoAreaWidth
        var drawHeight = drawWidth / photoAspect
        val maxPhotoHeight = cardHeightF - topPadding - bottomReserved
        if (drawHeight > maxPhotoHeight) {
            drawHeight = maxPhotoHeight
            drawWidth = drawHeight * photoAspect
        }
        val photoRect = RectF(
            (cardWidthF - drawWidth) / 2,
            topPadding + (maxPhotoHeight - drawHeight) / 2,
            (cardWidthF - drawWidth) / 2 + drawWidth,
            topPadding + (maxPhotoHeight - drawHeight) / 2 + drawHeight
        )

        // 照片底板（白色 + 灰色边框）
        val photoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val photoBgPath = Path().apply {
            addRoundRect(photoRect, 8f, 8f, Path.Direction.CW)
        }
        canvas.drawPath(photoBgPath, photoBgPaint)

        // 裁剪绘制照片
        canvas.save()
        canvas.clipPath(photoBgPath)
        val photoDrawRect = RectF(photoRect)
        canvas.drawBitmap(scaledPhoto, null, photoDrawRect, null)
        canvas.restore()

        // 照片边框
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(255, 224, 224, 224)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawPath(photoBgPath, borderPaint)

        // 底部水印区域
        val bottomY = photoRect.bottom + 36f

        // Logo
        if (logo != null) {
            val logoSize = 56f
            val logoRect = RectF((cardWidthF - logoSize) / 2, bottomY, (cardWidthF + logoSize) / 2, bottomY + logoSize)
            canvas.drawBitmap(logo, null, logoRect, null)
        }

        // 标题
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
        }
        val titleText = "构妙 · LiveCompose"
        val titleWidth = titlePaint.measureText(titleText)
        val titleY = bottomY + 64f
        canvas.drawText(titleText, (cardWidthF - titleWidth) / 2, titleY + titlePaint.textSize, titlePaint)

        // 日期
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(102, 0, 0, 0) // 40% black
            textSize = 22f
            typeface = Typeface.DEFAULT
        }
        val dateStr = formattedDate(date)
        val dateWidth = datePaint.measureText(dateStr)
        val dateY = titleY + titlePaint.textSize + 8f
        canvas.drawText(dateStr, (cardWidthF - dateWidth) / 2, dateY + datePaint.textSize, datePaint)

        // 参数行
        val paramsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(128, 0, 0, 0) // 50% black
            textSize = 20f
            typeface = Typeface.DEFAULT
        }
        val paramsY = dateY + datePaint.textSize + 14f
        val paramParts = mutableListOf<String>()
        if (detectionMethod != null) paramParts.add(detectionMethod)
        if (iso != null) paramParts.add("ISO ${iso.toInt()}")
        if (shutterSpeed != null) paramParts.add(shutterDisplay(shutterSpeed))
        if (aperture != null) paramParts.add("f/${"%.1f".format(aperture)}")
        if (imageWidth != null && imageHeight != null) paramParts.add("${imageWidth}×${imageHeight}")
        val paramsText = paramParts.joinToString("  ·  ")
        val paramsWidth = paramsPaint.measureText(paramsText)
        val drawX = ((cardWidthF - paramsWidth) / 2).coerceAtLeast(photoInsetHorizontal)
        canvas.drawText(paramsText, drawX, paramsY + paramsPaint.textSize, paramsPaint)

        // 底部分隔线
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(204, 0, 0, 0) // 80% black
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val lineY = paramsY + paramsPaint.textSize + 22f
        canvas.drawLine(cardWidthF * 0.25f, lineY, cardWidthF * 0.75f, lineY, linePaint)

        return bitmap
    }

    private fun scalePhoto(photo: Bitmap): Bitmap {
        val maxDim = maxOf(photo.width, photo.height).toFloat()
        if (maxDim <= maxPhotoDimension) return photo
        val ratio = maxPhotoDimension / maxDim
        val newWidth = (photo.width * ratio).toInt()
        val newHeight = (photo.height * ratio).toInt()
        return Bitmap.createScaledBitmap(photo, newWidth, newHeight, true)
    }

    private fun formattedDate(date: Date): String {
        val formatter = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
        return formatter.format(date)
    }

    private fun shutterDisplay(speed: Double): String {
        return if (speed >= 1) "${speed.toInt()}s"
        else "1/${(1.0 / speed).toInt()}s"
    }
}