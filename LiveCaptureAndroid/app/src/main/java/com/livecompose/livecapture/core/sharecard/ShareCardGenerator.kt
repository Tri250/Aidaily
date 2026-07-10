package com.livecompose.livecapture.core.sharecard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 分享卡片元数据
 *
 * 对标 iOS ShareCardGenerator.generate 的入参，包含品牌标题、副标题（作者/署名）、
 * 拍摄日期与 EXIF 拍摄参数，用于在卡片上渲染完整信息。
 *
 * @property title 主标题（品牌名），默认 "秒简相机"
 * @property subtitle 副标题（作者/署名），优先级高于 [date]；为空时回退显示日期
 * @property date 拍摄日期时间戳（毫秒），用于生成中文日期文本
 * @property detectionMethod 构图检测方法
 * @property iso 感光度
 * @property shutterSpeed 快门速度（秒）
 * @property aperture 光圈值
 * @property imageWidth 图片宽度（像素）
 * @property imageHeight 图片高度（像素）
 */
data class ShareCardMetadata(
    val title: String = "秒简相机",
    val subtitle: String? = null,
    val date: Long? = null,
    val detectionMethod: String? = null,
    val iso: Float? = null,
    val shutterSpeed: Double? = null,
    val aperture: Double? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
)

/**
 * 分享卡片生成器
 *
 * 使用 [Bitmap] + [Canvas] + [Paint] 绘制分享卡片，替代 iOS UIGraphicsImageRenderer。
 * 支持 4 种风格：极简 / 胶片 / 杂志 / 拍立得，与 iOS 视觉效果保持一致。
 *
 * 绘制在 [Dispatchers.Default] 调度器上执行，避免阻塞主线程。
 * 卡片基准尺寸为 1080×1440（3:4），与 iOS 完全对齐。
 */
object ShareCardGenerator {

    private const val TAG = "ShareCardGenerator"

    // 卡片基准尺寸（与 iOS 对齐：1080 × 1440，3:4）
    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1440

    /** 原图最大边长，超过则等比缩小，避免内存压力 */
    private const val MAX_PHOTO_DIMENSION = 1920

    /** 应用上下文，用于加载 logo 资源；未初始化时跳过 logo 绘制 */
    private var appContext: Context? = null

    /**
     * 初始化（用于加载 logo 资源）。
     * 应在 Application.onCreate() 中调用；未初始化时卡片仅省略 logo，其余正常绘制。
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - 公共 API

    /**
     * 生成分享卡片（核心方法）。
     *
     * @param image 原始照片
     * @param style 卡片样式
     * @param title 主标题（品牌名）
     * @param author 副标题（作者/署名）
     * @return 生成的卡片 Bitmap
     */
    suspend fun generateCard(
        image: Bitmap,
        style: ShareCardStyle,
        title: String,
        author: String,
    ): Bitmap = generateCard(image, style, ShareCardMetadata(title = title, subtitle = author))

    /**
     * 生成分享卡片（完整元数据版本，支持拍摄日期与 EXIF 参数）。
     */
    suspend fun generateCard(
        image: Bitmap,
        style: ShareCardStyle,
        metadata: ShareCardMetadata,
    ): Bitmap = withContext(Dispatchers.Default) {
        val photo = scaledPhoto(image) ?: return@withContext image
        try {
            when (style.cardTheme) {
                ShareCardStyle.CardTheme.MINIMAL -> generateMinimal(photo, style, metadata)
                ShareCardStyle.CardTheme.FILM -> generateFilm(photo, style, metadata)
                ShareCardStyle.CardTheme.MAGAZINE -> generateMagazine(photo, style, metadata)
                ShareCardStyle.CardTheme.POLAROID -> generatePolaroid(photo, style, metadata)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "生成分享卡片失败 style=${style.id}", e)
            photo
        }
    }

    /**
     * 生成样式选择器用的小尺寸预览缩略图。
     *
     * 实现方式与 iOS 一致：先生成完整卡片，再等比缩放到目标尺寸。
     *
     * @param size 目标尺寸（宽, 高），默认 160 × 213
     */
    suspend fun generatePreview(
        image: Bitmap,
        style: ShareCardStyle,
        size: Pair<Int, Int> = 160 to 213,
    ): Bitmap = withContext(Dispatchers.Default) {
        val fullCard = generateCard(image, style, ShareCardMetadata())
        val target = Bitmap.createBitmap(size.first, size.second, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawBitmap(
            fullCard,
            null,
            RectF(0f, 0f, size.first.toFloat(), size.second.toFloat()),
            paint,
        )
        target
    }

    // MARK: - 风格：极简 Minimal

    private fun generateMinimal(
        photo: Bitmap,
        style: ShareCardStyle,
        metadata: ShareCardMetadata,
    ): Bitmap {
        val card = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(card)
        val cardRect = RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat())

        // 白色圆角背景
        fillRoundedRect(canvas, cardRect, style.cardCornerRadius, style.backgroundColor)

        // 照片区域（aspect-fit，居中）
        val photoRect = aspectFitPhotoRect(
            photo, style.horizontalPadding, style.topPadding,
            maxPhotoHeight = CARD_HEIGHT - style.topPadding - style.bottomReserved,
        )

        // 照片白色衬底
        fillRoundedRect(canvas, photoRect, style.photoCornerRadius, Color.WHITE)
        // 裁剪绘制照片
        drawClippedBitmap(canvas, photo, photoRect, style.photoCornerRadius)
        // 照片描边
        strokeRoundedRect(canvas, photoRect, style.photoCornerRadius, grayColor(0.88f), 1f)

        // 底部品牌区
        val bottomY = photoRect.bottom + 36f
        drawLogo(canvas, topY = bottomY, size = 56f)

        val titleY = bottomY + 64f
        drawCenteredText(canvas, metadata.title, CARD_WIDTH / 2f, titleY, 34f, style.titleColor, bold = true)
        val subtitle = resolveSubtitle(metadata)
        if (subtitle.isNotEmpty()) {
            drawCenteredText(canvas, subtitle, CARD_WIDTH / 2f, titleY + 44f, 22f, style.subtitleColor)
        }

        val paramsY = titleY + 44f + 42f
        drawParams(canvas, metadata, CARD_WIDTH / 2f, paramsY, style.paramColor, style.horizontalPadding)

        drawBottomLine(canvas, paramsY + 48f, grayColor(0.8f))

        return card
    }

    // MARK: - 风格：胶片 Film

    private fun generateFilm(
        photo: Bitmap,
        style: ShareCardStyle,
        metadata: ShareCardMetadata,
    ): Bitmap {
        val card = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(card)
        val cardRect = RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat())

        // 深色背景
        fillRoundedRect(canvas, cardRect, style.cardCornerRadius, style.backgroundColor)

        // 两侧齿孔装饰
        val filmBorder = 40f
        val sprocketRadius = 6f
        val sprocketSpacing = 40f
        var y = filmBorder + 20f
        while (y < CARD_HEIGHT - filmBorder - 20f) {
            val leftRect = RectF(
                filmBorder / 2f - sprocketRadius, y - sprocketRadius,
                filmBorder / 2f + sprocketRadius, y + sprocketRadius,
            )
            val rightRect = RectF(
                CARD_WIDTH - filmBorder / 2f - sprocketRadius, y - sprocketRadius,
                CARD_WIDTH - filmBorder / 2f + sprocketRadius, y + sprocketRadius,
            )
            fillRoundedRect(canvas, leftRect, sprocketRadius, grayColor(0.25f))
            fillRoundedRect(canvas, rightRect, sprocketRadius, grayColor(0.25f))
            y += sprocketSpacing
        }

        // 照片区域
        val photoAreaWidth = CARD_WIDTH - filmBorder * 2f
        val photoAreaHeight = CARD_HEIGHT - filmBorder * 2f - 200f
        val photoRect = aspectFitPhotoRect(
            photo, insetHorizontal = (CARD_WIDTH - photoAreaWidth) / 2f,
            insetTop = filmBorder, maxPhotoHeight = photoAreaHeight,
            centerX = CARD_WIDTH / 2f,
        )

        // 白色衬底（向外扩展 8）
        val backing = RectF(
            photoRect.left - 8f, photoRect.top - 8f,
            photoRect.right + 8f, photoRect.bottom + 8f,
        )
        fillRoundedRect(canvas, backing, 4f, Color.WHITE)

        // 照片（圆角 2）
        drawClippedBitmap(canvas, photo, photoRect, 2f)

        // 底部信息
        val infoY = photoRect.bottom + 24f
        drawCenteredText(canvas, metadata.title, CARD_WIDTH / 2f, infoY, 28f, style.titleColor, bold = true)
        val subtitle = resolveSubtitle(metadata)
        if (subtitle.isNotEmpty()) {
            drawCenteredText(canvas, subtitle, CARD_WIDTH / 2f, infoY + 36f, 18f, style.subtitleColor)
        }
        val paramsY = infoY + 36f + 32f
        drawParams(canvas, metadata, CARD_WIDTH / 2f, paramsY, style.paramColor, style.horizontalPadding)

        return card
    }

    // MARK: - 风格：杂志 Magazine

    private fun generateMagazine(
        photo: Bitmap,
        style: ShareCardStyle,
        metadata: ShareCardMetadata,
    ): Bitmap {
        val card = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(card)
        val cardRect = RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat())

        // 暖米色背景
        fillRoundedRect(canvas, cardRect, style.cardCornerRadius, style.backgroundColor)

        // 顶部红色横条
        fillRect(canvas, RectF(0f, 0f, CARD_WIDTH.toFloat(), 8f), style.accentColor)

        // 页眉 "秒简"（Georgia 风格衬线粗体）
        val headerTf = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val headerText = "秒简"
        val headerPaint = textPaint(48f, style.titleColor, typeface = headerTf)
        val headerWidth = headerPaint.measureText(headerText)
        drawTextAtTopLeft(canvas, headerText, (CARD_WIDTH - headerWidth) / 2f, 40f, headerPaint)
        val headerHeight = headerPaint.fontMetrics.descent - headerPaint.fontMetrics.ascent

        // 副标题 "MIAOJIAN"，字距加宽
        val subText = "MIAOJIAN"
        val subPaint = textPaint(16f, style.subtitleColor).apply { letterSpacing = 0.25f /* ≈ 4px @ 16sp */ }
        val subWidth = subPaint.measureText(subText)
        drawTextAtTopLeft(canvas, subText, (CARD_WIDTH - subWidth) / 2f, 40f + headerHeight + 4f, subPaint)
        val subHeight = subPaint.fontMetrics.descent - subPaint.fontMetrics.ascent

        // 页眉下方装饰线
        val lineY = 40f + headerHeight + subHeight + 14f
        drawHorizontalLine(canvas, CARD_WIDTH * 0.35f, CARD_WIDTH * 0.65f, lineY, style.accentColor, 1.5f)

        // 照片区域
        val headerHeightLayout = 200f
        val photoAreaWidth = CARD_WIDTH - 60f
        val photoAreaHeight = CARD_HEIGHT - headerHeightLayout - 200f
        val photoRect = aspectFitPhotoRect(
            photo, insetHorizontal = (CARD_WIDTH - photoAreaWidth) / 2f,
            insetTop = headerHeightLayout, maxPhotoHeight = photoAreaHeight,
            centerX = CARD_WIDTH / 2f,
        )

        drawClippedBitmap(canvas, photo, photoRect, 4f)
        strokeRoundedRect(canvas, photoRect, 4f, grayColor(0.85f), 1f)

        // 底部信息（日期使用衬线字体）
        val infoY = photoRect.bottom + 24f
        val subtitle = resolveSubtitle(metadata)
        if (subtitle.isNotEmpty()) {
            val dateTf = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            drawCenteredText(canvas, subtitle, CARD_WIDTH / 2f, infoY, 18f, style.subtitleColor, typeface = dateTf)
        }
        val paramsY = infoY + 30f
        drawParams(canvas, metadata, CARD_WIDTH / 2f, paramsY, style.paramColor, style.horizontalPadding)

        // 底部 logo
        drawLogo(canvas, topY = CARD_HEIGHT - 70f, size = 40f)

        return card
    }

    // MARK: - 风格：拍立得 Polaroid

    private fun generatePolaroid(
        photo: Bitmap,
        style: ShareCardStyle,
        metadata: ShareCardMetadata,
    ): Bitmap {
        val card = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(card)
        val cardRect = RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat())

        // 白色相纸边框
        fillRoundedRect(canvas, cardRect, 8f, style.backgroundColor)

        val borderTop = 60f
        val borderSide = 50f
        val borderBottom = 100f
        val photoAreaWidth = CARD_WIDTH - borderSide * 2f
        val photoAreaHeight = CARD_HEIGHT - borderTop - borderBottom
        val photoRect = aspectFitPhotoRect(
            photo, insetHorizontal = borderSide, insetTop = borderTop,
            maxPhotoHeight = photoAreaHeight, centerX = CARD_WIDTH / 2f,
        )

        // 照片背后阴影（黑色 8% 透明，向外扩展 6）
        val shadowRect = RectF(
            photoRect.left - 6f, photoRect.top - 6f,
            photoRect.right + 6f, photoRect.bottom + 6f,
        )
        fillRoundedRect(canvas, shadowRect, 2f, (0x14000000)) // 0.08 alpha

        // 照片
        drawClippedBitmap(canvas, photo, photoRect, 2f)
        strokeRoundedRect(canvas, photoRect, 2f, grayColor(0.85f), 0.5f)

        // 底部手写感品牌区
        val bottomAreaY = CARD_HEIGHT - borderBottom + 12f
        drawCenteredText(canvas, metadata.title, CARD_WIDTH / 2f, bottomAreaY, 26f, style.titleColor, bold = true)
        val subtitle = resolveSubtitle(metadata)
        if (subtitle.isNotEmpty()) {
            drawCenteredText(canvas, subtitle, CARD_WIDTH / 2f, bottomAreaY + 32f, 16f, style.subtitleColor)
        }

        return card
    }

    // MARK: - 绘制辅助

    /**
     * 计算照片 aspect-fit 后的绘制矩形（保持宽高比，居中）。
     */
    private fun aspectFitPhotoRect(
        photo: Bitmap,
        insetHorizontal: Float,
        insetTop: Float,
        maxPhotoHeight: Float,
        centerX: Float = CARD_WIDTH / 2f,
    ): RectF {
        val photoAreaWidth = CARD_WIDTH - insetHorizontal * 2f
        val photoAspect = photo.width.toFloat() / photo.height.toFloat()
        var drawWidth = photoAreaWidth
        var drawHeight = drawWidth / photoAspect
        if (drawHeight > maxPhotoHeight) {
            drawHeight = maxPhotoHeight
            drawWidth = drawHeight * photoAspect
        }
        val left = centerX - drawWidth / 2f
        val top = insetTop + (maxPhotoHeight - drawHeight) / 2f
        return RectF(left, top, left + drawWidth, top + drawHeight)
    }

    /** 填充圆角矩形 */
    private fun fillRoundedRect(canvas: Canvas, rect: RectF, radius: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    /** 填充矩形 */
    private fun fillRect(canvas: Canvas, rect: RectF, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRect(rect, paint)
    }

    /** 圆角矩形描边 */
    private fun strokeRoundedRect(canvas: Canvas, rect: RectF, radius: Float, color: Int, strokeWidth: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    /** 裁剪到圆角矩形后绘制位图 */
    private fun drawClippedBitmap(canvas: Canvas, bitmap: Bitmap, rect: RectF, radius: Float) {
        val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawBitmap(bitmap, null, rect, paint)
        canvas.restore()
    }

    /**
     * 居中绘制文本（[centerX],[centerY] 为文本视觉中心）。
     */
    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        typeface: Typeface? = null,
    ) {
        val paint = textPaint(size, color, bold, typeface)
        val textWidth = paint.measureText(text)
        val fm = paint.fontMetrics
        val baseline = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, centerX - textWidth / 2f, baseline, paint)
    }

    /** 以 (x, y) 为文本左上角绘制 */
    private fun drawTextAtTopLeft(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        val baseline = y - paint.fontMetrics.ascent
        canvas.drawText(text, x, baseline, paint)
    }

    /**
     * 绘制拍摄参数行（检测方法 · ISO · 快门 · 光圈 · 尺寸），居中对齐。
     * 与 iOS drawParams 逻辑一致，使用 "  ·  " 作为分隔符。
     */
    private fun drawParams(
        canvas: Canvas,
        metadata: ShareCardMetadata,
        centerX: Float,
        centerY: Float,
        color: Int,
        horizontalPadding: Float,
    ) {
        val parts = mutableListOf<String>()
        metadata.detectionMethod?.let { parts.add(it) }
        metadata.iso?.let { parts.add("ISO ${it.toInt()}") }
        metadata.shutterSpeed?.let { parts.add(shutterDisplay(it)) }
        metadata.aperture?.let { parts.add("f/${"%.1f".format(it)}") }
        metadata.imageWidth?.let { w ->
            metadata.imageHeight?.let { h -> parts.add("${w}×${h}") }
        }
        if (parts.isEmpty()) return

        val text = parts.joinToString("  ·  ")
        val paint = textPaint(20f, color)
        val textWidth = paint.measureText(text)
        val maxWidth = CARD_WIDTH - horizontalPadding * 2f
        val drawWidth = minOf(textWidth, maxWidth)
        val x = maxOf(centerX - drawWidth / 2f, horizontalPadding)
        val fm = paint.fontMetrics
        val baseline = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, x, baseline, paint)
    }

    /** 绘制底部装饰横线 */
    private fun drawBottomLine(canvas: Canvas, y: Float, color: Int) {
        drawHorizontalLine(canvas, CARD_WIDTH * 0.25f, CARD_WIDTH * 0.75f, y, color, 1f)
    }

    private fun drawHorizontalLine(canvas: Canvas, startX: Float, stopX: Float, y: Float, color: Int, strokeWidth: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawLine(startX, y, stopX, y, paint)
    }

    /** 居中绘制 logo（[topY] 为顶部坐标） */
    private fun drawLogo(canvas: Canvas, topY: Float, size: Float) {
        val logo = loadLogo() ?: return
        val left = (CARD_WIDTH - size) / 2f
        val dst = RectF(left, topY, left + size, topY + size)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawBitmap(logo, null, dst, paint)
    }

    /** 构建文本画笔 */
    private fun textPaint(size: Float, color: Int, bold: Boolean = false, typeface: Typeface? = null): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            isFakeBoldText = bold
            this.typeface = typeface ?: if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    // MARK: - 资源与格式化

    /** 等比缩小原图，最大边不超过 [MAX_PHOTO_DIMENSION] */
    private fun scaledPhoto(photo: Bitmap): Bitmap? {
        val w = photo.width
        val h = photo.height
        if (w <= 0 || h <= 0) return null
        val maxDim = maxOf(w, h)
        if (maxDim <= MAX_PHOTO_DIMENSION) return photo
        val ratio = MAX_PHOTO_DIMENSION.toFloat() / maxDim
        return Bitmap.createScaledBitmap(photo, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    /** 加载 logo 资源（按名称查找，缺失时返回 null） */
    private fun loadLogo(): Bitmap? {
        val ctx = appContext ?: return null
        return try {
            val resId = ctx.resources.getIdentifier("logo_glass_livecompose", "drawable", ctx.packageName)
            if (resId != 0) BitmapFactory.decodeResource(ctx.resources, resId) else null
        } catch (e: Exception) {
            AppLogger.w(TAG, "加载 logo 资源失败", e)
            null
        }
    }

    /** 解析副标题：优先使用 [ShareCardMetadata.subtitle]，否则格式化日期 */
    private fun resolveSubtitle(metadata: ShareCardMetadata): String =
        metadata.subtitle?.takeIf { it.isNotBlank() } ?: metadata.date?.let { formattedDate(it) } ?: ""

    /** 日期格式化（中文，与 iOS 一致：yyyy年M月d日 HH:mm） */
    private val dateFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
    }

    private fun formattedDate(timestamp: Long): String =
        dateFormatter.get()?.format(Date(timestamp)) ?: ""

    /** 快门速度显示：≥1s 显示 "Ns"，否则显示 "1/Ns" */
    private fun shutterDisplay(speed: Double): String =
        if (speed >= 1.0) "${speed.toInt()}s" else "1/${(1.0 / speed).toInt()}s"
}

/**
 * 生成指定灰度级别（0..1）的不透明灰色 ARGB 值。
 */
private fun grayColor(level: Float): Int {
    val v = (level.coerceIn(0f, 1f) * 255f).toInt()
    return (0xFF shl 24) or (v shl 16) or (v shl 8) or v
}
