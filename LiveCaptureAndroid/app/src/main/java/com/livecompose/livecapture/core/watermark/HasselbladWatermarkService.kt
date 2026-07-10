package com.livecompose.livecapture.core.watermark

import android.graphics.*
import android.graphics.Paint.Align
import kotlin.math.roundToInt

/**
 * 哈苏风格水印服务
 *
 * 对标 OPPO Find X9 哈苏大师水印，包含：
 * 1. 哈苏标志性橙色"H"logo
 * 2. 相机型号 + 哈苏联名标识
 * 3. 拍摄参数条（焦距/光圈/快门/ISO）
 * 4. 日期 + 地理位置
 * 5. 底部白边留白（哈苏经典风格）
 */
class HasselbladWatermarkService {

    companion object {
        // 哈苏品牌色
        private const val HASSELBLAD_ORANGE = 0xFFE45A00.toInt()
        private const val HASSELBLAD_GREY = 0xFF8A8A8A.toInt()
        private const val HASSELBLAD_WHITE = 0xFFFFFFFF.toInt()
        private const val HASSELBLAD_DARK = 0xFF1A1A1A.toInt()

        private const val BOTTOM_BAR_HEIGHT_RATIO = 0.08f
        private const val WHITE_BORDER_RATIO = 0.03f
    }

    data class WatermarkConfig(
        val cameraModel: String = "MiaoJian",
        val hasselbladBranding: String = " | Hasselblad",
        val focalLength: String = "24mm",
        val aperture: String = "f/1.8",
        val shutterSpeed: String = "1/100",
        val iso: String = "ISO 100",
        val dateTime: String = "",
        val location: String = "",
        val enableFrame: Boolean = true,
        val enableParams: Boolean = true,
        val enableLocation: Boolean = true
    )

    /**
     * 对照片应用哈苏风格水印，返回带水印的新 Bitmap
     */
    fun applyWatermark(source: Bitmap, config: WatermarkConfig): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        val bottomBarHeight = (sourceHeight * BOTTOM_BAR_HEIGHT_RATIO).roundToInt()
        val whiteBorder = (sourceHeight * WHITE_BORDER_RATIO).roundToInt()
        val totalWidth = sourceWidth + whiteBorder * 2
        val totalHeight = sourceHeight + whiteBorder + bottomBarHeight

        val result = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. 绘制白色边框
        val borderPaint = Paint().apply {
            color = HASSELBLAD_WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, totalWidth.toFloat(), totalHeight.toFloat(), borderPaint)

        // 2. 绘制照片（带白边内偏移）
        canvas.drawBitmap(source, whiteBorder.toFloat(), whiteBorder.toFloat(), null)

        // 3. 绘制底部信息栏
        val barTop = sourceHeight + whiteBorder
        drawBottomBar(canvas, totalWidth, barTop, bottomBarHeight.toFloat(), config)

        // 4. 绘制哈苏 H logo
        drawHasselbladLogo(canvas, totalWidth.toFloat(), barTop.toFloat(), bottomBarHeight.toFloat())

        return result
    }

    /**
     * 直接对照片叠加水印（无白边，适合嵌入已有图片）
     */
    fun overlayWatermark(source: Bitmap, config: WatermarkConfig): Bitmap {
        val canvas = Canvas(source)
        val bottomBarHeight = (source.height * 0.06f).roundToInt()
        val barTop = (source.height - bottomBarHeight).toFloat()

        // 半透明渐变背景
        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, barTop - 20f,
                0f, source.height.toFloat(),
                intArrayOf(
                    Color.argb(0, 0, 0, 0),
                    Color.argb(180, 0, 0, 0)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, barTop - 20f, source.width.toFloat(), source.height.toFloat(), gradientPaint)

        drawHasselbladLogo(canvas, source.width.toFloat(), barTop, bottomBarHeight.toFloat())
        drawBottomBarText(canvas, source.width.toFloat(), barTop, bottomBarHeight.toFloat(), config)

        return source
    }

    private fun drawBottomBar(
        canvas: Canvas,
        width: Int,
        barTop: Int,
        barHeight: Float,
        config: WatermarkConfig
    ) {
        // 底部灰底
        val barBgPaint = Paint().apply {
            color = HASSELBLAD_DARK
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, barTop.toFloat(), width.toFloat(), barTop + barHeight, barBgPaint)

        drawHasselbladLogo(canvas, width.toFloat(), barTop.toFloat(), barHeight)
        drawBottomBarText(canvas, width.toFloat(), barTop.toFloat(), barHeight, config)
    }

    private fun drawHasselbladLogo(canvas: Canvas, totalWidth: Float, barTop: Float, barHeight: Float) {
        val logoPaint = Paint().apply {
            color = HASSELBLAD_ORANGE
            textSize = barHeight * 0.55f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Align.LEFT
        }

        val logoX = 16f
        val logoY = barTop + barHeight * 0.7f
        canvas.drawText("H", logoX, logoY, logoPaint)

        // 品牌名
        val brandPaint = Paint().apply {
            color = HASSELBLAD_WHITE
            textSize = barHeight * 0.28f
            isAntiAlias = true
            typeface = Typeface.DEFAULT
            textAlign = Align.LEFT
        }
        val brandX = logoX + barHeight * 0.42f
        canvas.drawText("HASSELBLAD", brandX, logoY - barHeight * 0.08f, brandPaint)

        // 手机型号
        val modelPaint = Paint().apply {
            color = HASSELBLAD_GREY
            textSize = barHeight * 0.22f
            isAntiAlias = true
            textAlign = Align.LEFT
        }
        canvas.drawText("Shot on MiaoJian", brandX, logoY + barHeight * 0.22f, modelPaint)
    }

    private fun drawBottomBarText(
        canvas: Canvas,
        totalWidth: Float,
        barTop: Float,
        barHeight: Float,
        config: WatermarkConfig
    ) {
        if (!config.enableParams) return

        val lineHeight = barHeight * 0.3f
        val line1Y = barTop + barHeight * 0.35f
        val line2Y = barTop + barHeight * 0.7f

        // 第一行：焦距 | 光圈 | 快门 | ISO
        val paramsText = "${config.focalLength}  ${config.aperture}  ${config.shutterSpeed}  ${config.iso}"
        val paramsPaint = Paint().apply {
            color = HASSELBLAD_WHITE
            textSize = barHeight * 0.24f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textAlign = Align.RIGHT
        }
        canvas.drawText(paramsText, totalWidth - 16f, line1Y, paramsPaint)

        // 第二行：日期 | 位置
        val infoParts = mutableListOf<String>()
        if (config.dateTime.isNotEmpty()) infoParts.add(config.dateTime)
        if (config.enableLocation && config.location.isNotEmpty()) infoParts.add(config.location)
        if (infoParts.isNotEmpty()) {
            val infoPaint = Paint().apply {
                color = HASSELBLAD_GREY
                textSize = barHeight * 0.2f
                isAntiAlias = true
                textAlign = Align.RIGHT
            }
            canvas.drawText(infoParts.joinToString("  "), totalWidth - 16f, line2Y, infoPaint)
        }
    }
}