package com.livecompose.livecapture.core.editing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Random

/**
 * 图像扩展器
 *
 * 对应 iOS 端 ImageExpander.swift，基于边缘镜像和内容感知的图像外扩填充。
 *
 * ## 算法原理
 * 1. 创建更大的画布，将原图放在中心
 * 2. 对每个扩展方向，截取边缘条带并镜像翻转
 * 3. 对镜像条带应用渐变淡出遮罩
 * 4. 在接缝处混合模糊
 * 5. 添加噪点使扩展区域看起来自然
 *
 * @param context 上下文（用于初始化 RenderScript）
 */
class ImageExpander {

    companion object {
        private const val TAG = "ImageExpander"
        private const val SEAM_WIDTH = 15
    }

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** 处理进度（0.0 - 1.0） */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    /**
     * 扩展方向
     */
    enum class ExpansionDirection {
        ALL,
        HORIZONTAL,
        VERTICAL,
        UP,
        DOWN,
        LEFT,
        RIGHT;

        /** 显示名称 */
        val displayName: String
            get() = when (this) {
                ALL -> "全部"
                HORIZONTAL -> "水平"
                VERTICAL -> "垂直"
                UP -> "向上"
                DOWN -> "向下"
                LEFT -> "向左"
                RIGHT -> "向右"
            }
    }

    /**
     * 向外扩展图像，自动填充新增区域
     *
     * @param image 输入 Bitmap
     * @param expandBy 每边扩展像素数
     * @param direction 扩展方向
     * @return 扩展后的 Bitmap，失败返回原图
     */
    suspend fun expandImage(
        image: Bitmap,
        expandBy: Int,
        direction: ExpansionDirection = ExpansionDirection.ALL
    ): Bitmap = withContext(Dispatchers.Default) {
        if (image.width <= 0 || image.height <= 0 || expandBy <= 0) {
            return@withContext image
        }

        _isProcessing.value = true
        _progress.value = 0f

        try {
            val result = processExpansion(image, expandBy, direction)
            _progress.value = 1.0f
            result
        } catch (e: Exception) {
            AppLogger.e(TAG, "图像扩展失败", e)
            image
        } finally {
            _isProcessing.value = false
        }
    }

    /**
     * 执行扩展处理
     */
    private suspend fun processExpansion(
        image: Bitmap,
        expandBy: Int,
        direction: ExpansionDirection
    ): Bitmap {
        val origWidth = image.width
        val origHeight = image.height

        // 1. 计算各方向扩展量
        var topExpand = 0
        var bottomExpand = 0
        var leftExpand = 0
        var rightExpand = 0

        when (direction) {
            ExpansionDirection.ALL -> {
                topExpand = expandBy; bottomExpand = expandBy
                leftExpand = expandBy; rightExpand = expandBy
            }
            ExpansionDirection.HORIZONTAL -> {
                leftExpand = expandBy; rightExpand = expandBy
            }
            ExpansionDirection.VERTICAL -> {
                topExpand = expandBy; bottomExpand = expandBy
            }
            ExpansionDirection.UP -> topExpand = expandBy
            ExpansionDirection.DOWN -> bottomExpand = expandBy
            ExpansionDirection.LEFT -> leftExpand = expandBy
            ExpansionDirection.RIGHT -> rightExpand = expandBy
        }

        // 2. 新画布尺寸
        val newWidth = origWidth + leftExpand + rightExpand
        val newHeight = origHeight + topExpand + bottomExpand

        // 原图在新画布中的位置（Android 坐标系：原点左上）
        val imageLeft = leftExpand
        val imageTop = topExpand
        val imagePlacement = RectF(
            imageLeft.toFloat(),
            imageTop.toFloat(),
            (imageLeft + origWidth).toFloat(),
            (imageTop + origHeight).toFloat()
        )

        // 3. 创建画布并绘制原图
        val canvas = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvasCanvas = Canvas(canvas)
        canvasCanvas.drawColor(Color.BLACK)
        canvasCanvas.drawBitmap(image, imageLeft.toFloat(), imageTop.toFloat(), null)

        _progress.value = 0.2f

        // 4. 填充各方向的扩展区域（边缘镜像）
        var stepCount = 0
        val totalSteps = (if (topExpand > 0) 1 else 0) + (if (bottomExpand > 0) 1 else 0) +
            (if (leftExpand > 0) 1 else 0) + (if (rightExpand > 0) 1 else 0)
        if (totalSteps == 0) return canvas

        if (topExpand > 0) {
            val topEdge = mirrorEdge(image, ExpansionDirection.UP, topExpand, origWidth, origHeight, imagePlacement)
            canvasCanvas.drawBitmap(topEdge, imageLeft.toFloat(), (imageTop - topExpand).toFloat(), null)
            topEdge.recycle()
            stepCount++
            _progress.value = 0.2f + 0.6f * stepCount / totalSteps
        }
        if (bottomExpand > 0) {
            val bottomEdge = mirrorEdge(image, ExpansionDirection.DOWN, bottomExpand, origWidth, origHeight, imagePlacement)
            canvasCanvas.drawBitmap(bottomEdge, imageLeft.toFloat(), (imageTop + origHeight).toFloat(), null)
            bottomEdge.recycle()
            stepCount++
            _progress.value = 0.2f + 0.6f * stepCount / totalSteps
        }
        if (leftExpand > 0) {
            val leftEdge = mirrorEdge(image, ExpansionDirection.LEFT, leftExpand, origWidth, origHeight, imagePlacement)
            canvasCanvas.drawBitmap(leftEdge, (imageLeft - leftExpand).toFloat(), imageTop.toFloat(), null)
            leftEdge.recycle()
            stepCount++
            _progress.value = 0.2f + 0.6f * stepCount / totalSteps
        }
        if (rightExpand > 0) {
            val rightEdge = mirrorEdge(image, ExpansionDirection.RIGHT, rightExpand, origWidth, origHeight, imagePlacement)
            canvasCanvas.drawBitmap(rightEdge, (imageLeft + origWidth).toFloat(), imageTop.toFloat(), null)
            rightEdge.recycle()
            stepCount++
            _progress.value = 0.2f + 0.6f * stepCount / totalSteps
        }

        // 5. 填充角落区域
        if (topExpand > 0 && leftExpand > 0) {
            fillCorner(canvas, Corner.TOP_LEFT, leftExpand, topExpand, imagePlacement)
        }
        if (topExpand > 0 && rightExpand > 0) {
            fillCorner(canvas, Corner.TOP_RIGHT, rightExpand, topExpand, imagePlacement)
        }
        if (bottomExpand > 0 && leftExpand > 0) {
            fillCorner(canvas, Corner.BOTTOM_LEFT, leftExpand, bottomExpand, imagePlacement)
        }
        if (bottomExpand > 0 && rightExpand > 0) {
            fillCorner(canvas, Corner.BOTTOM_RIGHT, rightExpand, bottomExpand, imagePlacement)
        }

        _progress.value = 0.85f

        // 6. 接缝处模糊混合
        blendSeams(canvas, imagePlacement)

        _progress.value = 0.95f

        // 7. 添加扩展区域噪点
        addExpansionNoise(canvas, imagePlacement)

        return canvas
    }

    /**
     * 镜像图像边缘生成扩展条带
     *
     * @param image 原图
     * @param edge 边缘方向
     * @param width 扩展宽度
     * @param origWidth 原图宽度
     * @param origHeight 原图高度
     * @param imagePlacement 原图在新画布中的位置
     * @return 镜像翻转并应用渐变淡出的边缘条带
     */
    private fun mirrorEdge(
        image: Bitmap,
        edge: ExpansionDirection,
        width: Int,
        origWidth: Int,
        origHeight: Int,
        imagePlacement: RectF
    ): Bitmap {
        val strip: Bitmap
        val matrix = Matrix()

        when (edge) {
            ExpansionDirection.UP -> {
                // 截取原图顶部条带（高度 width）
                strip = Bitmap.createBitmap(image, 0, 0, origWidth, width.coerceAtMost(origHeight))
                // 垂直翻转
                matrix.preScale(1f, -1f)
            }
            ExpansionDirection.DOWN -> {
                strip = Bitmap.createBitmap(image, 0, (origHeight - width).coerceAtLeast(0), origWidth, width.coerceAtMost(origHeight))
                matrix.preScale(1f, -1f)
            }
            ExpansionDirection.LEFT -> {
                strip = Bitmap.createBitmap(image, 0, 0, width.coerceAtMost(origWidth), origHeight)
                // 水平翻转
                matrix.preScale(-1f, 1f)
            }
            ExpansionDirection.RIGHT -> {
                strip = Bitmap.createBitmap(image, (origWidth - width).coerceAtLeast(0), 0, width.coerceAtMost(origWidth), origHeight)
                matrix.preScale(-1f, 1f)
            }
            else -> return Bitmap.createBitmap(width, origHeight, Bitmap.Config.ARGB_8888)
        }

        val flipped = Bitmap.createBitmap(strip, 0, 0, strip.width, strip.height, matrix, true)
        if (flipped !== strip) strip.recycle()

        // 应用渐变淡出（从接缝处向外渐变透明）
        return applyFadeMask(flipped, edge, width)
    }

    /**
     * 应用渐变淡出掩码（从接缝处向外渐变透明）
     */
    private fun applyFadeMask(strip: Bitmap, edge: ExpansionDirection, width: Int): Bitmap {
        val w = strip.width
        val h = strip.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 先绘制条带
        canvas.drawBitmap(strip, 0f, 0f, null)

        // 创建渐变遮罩（透明→黑色，表示淡出区域）
        val shader: Shader = when (edge) {
            ExpansionDirection.UP -> {
                // 顶部扩展：接缝在底部（y=h），远处在顶部（y=0）
                LinearGradient(0f, h.toFloat(), 0f, 0f, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
            }
            ExpansionDirection.DOWN -> {
                // 底部扩展：接缝在顶部（y=0），远处在底部（y=h）
                LinearGradient(0f, 0f, 0f, h.toFloat(), Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
            }
            ExpansionDirection.LEFT -> {
                // 左侧扩展：接缝在右侧（x=w），远处在左侧（x=0）
                LinearGradient(w.toFloat(), 0f, 0f, 0f, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
            }
            ExpansionDirection.RIGHT -> {
                // 右侧扩展：接缝在左侧（x=0），远处在右侧（x=w）
                LinearGradient(0f, 0f, w.toFloat(), 0f, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
            }
            else -> return strip
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        strip.recycle()
        return result
    }

    /**
     * 角落枚举
     */
    private enum class Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    /**
     * 填充角落区域（双向扩展的交汇处）
     */
    private fun fillCorner(
        canvas: Bitmap,
        corner: Corner,
        sizeW: Int,
        sizeH: Int,
        imagePlacement: RectF
    ) {
        // 角落中心点（在原图边缘上）
        val centerX: Float
        val centerY: Float
        when (corner) {
            Corner.TOP_LEFT -> {
                centerX = imagePlacement.left
                centerY = imagePlacement.top
            }
            Corner.TOP_RIGHT -> {
                centerX = imagePlacement.right
                centerY = imagePlacement.top
            }
            Corner.BOTTOM_LEFT -> {
                centerX = imagePlacement.left
                centerY = imagePlacement.bottom
            }
            Corner.BOTTOM_RIGHT -> {
                centerX = imagePlacement.right
                centerY = imagePlacement.bottom
            }
        }

        // 采样角落附近颜色
        val sampleColor = sampleCornerColor(canvas, corner, imagePlacement)

        // 创建径向渐变填充角落
        val radius = Math.max(sizeW, sizeH) * 1.5f
        val shader = RadialGradient(
            centerX, centerY, radius,
            sampleColor, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        val canvasCanvas = Canvas(canvas)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }

        // 角落矩形区域
        val cornerRect = when (corner) {
            Corner.TOP_LEFT -> RectF(0f, 0f, sizeW.toFloat(), sizeH.toFloat())
            Corner.TOP_RIGHT -> RectF((canvas.width - sizeW).toFloat(), 0f, canvas.width.toFloat(), sizeH.toFloat())
            Corner.BOTTOM_LEFT -> RectF(0f, (canvas.height - sizeH).toFloat(), sizeW.toFloat(), canvas.height.toFloat())
            Corner.BOTTOM_RIGHT -> RectF((canvas.width - sizeW).toFloat(), (canvas.height - sizeH).toFloat(), canvas.width.toFloat(), canvas.height.toFloat())
        }
        canvasCanvas.drawRect(cornerRect, paint)
    }

    /**
     * 采样角落附近的颜色
     */
    private fun sampleCornerColor(canvas: Bitmap, corner: Corner, imagePlacement: RectF): Int {
        val sampleSize = 10
        val sampleX: Int
        val sampleY: Int
        when (corner) {
            Corner.TOP_LEFT -> {
                sampleX = imagePlacement.left.toInt().coerceAtLeast(0)
                sampleY = imagePlacement.top.toInt().coerceAtLeast(0)
            }
            Corner.TOP_RIGHT -> {
                sampleX = (imagePlacement.right - sampleSize).toInt().coerceAtLeast(0)
                sampleY = imagePlacement.top.toInt().coerceAtLeast(0)
            }
            Corner.BOTTOM_LEFT -> {
                sampleX = imagePlacement.left.toInt().coerceAtLeast(0)
                sampleY = (imagePlacement.bottom - sampleSize).toInt().coerceAtLeast(0)
            }
            Corner.BOTTOM_RIGHT -> {
                sampleX = (imagePlacement.right - sampleSize).toInt().coerceAtLeast(0)
                sampleY = (imagePlacement.bottom - sampleSize).toInt().coerceAtLeast(0)
            }
        }

        var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
        val maxX = (sampleX + sampleSize).coerceAtMost(canvas.width)
        val maxY = (sampleY + sampleSize).coerceAtMost(canvas.height)
        for (y in sampleY until maxY) {
            for (x in sampleX until maxX) {
                val c = canvas.getPixel(x, y)
                rSum += Color.red(c); gSum += Color.green(c); bSum += Color.blue(c)
                count++
            }
        }
        return if (count > 0) Color.rgb(rSum / count, gSum / count, bSum / count) else Color.GRAY
    }

    /**
     * 在接缝处进行模糊混合
     */
    private fun blendSeams(canvas: Bitmap, imagePlacement: RectF) {
        val width = canvas.width
        val height = canvas.height

        // 创建接缝掩码（接缝处为白色=需模糊，其他为黑色）
        val seamMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(seamMask)
        maskCanvas.drawColor(Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val seamHalf = SEAM_WIDTH / 2f

        // 顶部接缝
        if (imagePlacement.top > 0) {
            maskCanvas.drawRect(
                imagePlacement.left,
                (imagePlacement.top - seamHalf).coerceAtLeast(0f),
                imagePlacement.right,
                (imagePlacement.top + seamHalf),
                paint
            )
        }
        // 底部接缝
        if (imagePlacement.bottom < height) {
            maskCanvas.drawRect(
                imagePlacement.left,
                (imagePlacement.bottom - seamHalf),
                imagePlacement.right,
                (imagePlacement.bottom + seamHalf).coerceAtMost(height.toFloat()),
                paint
            )
        }
        // 左侧接缝
        if (imagePlacement.left > 0) {
            maskCanvas.drawRect(
                (imagePlacement.left - seamHalf).coerceAtLeast(0f),
                imagePlacement.top,
                (imagePlacement.left + seamHalf),
                imagePlacement.bottom,
                paint
            )
        }
        // 右侧接缝
        if (imagePlacement.right < width) {
            maskCanvas.drawRect(
                (imagePlacement.right - seamHalf),
                imagePlacement.top,
                (imagePlacement.right + seamHalf).coerceAtMost(width.toFloat()),
                imagePlacement.bottom,
                paint
            )
        }

        // 对画布做模糊
        val blurred = boxBlur(canvas, 4)

        // 在接缝区域用模糊结果替换
        val canvasPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        canvas.getPixels(canvasPixels, 0, width, 0, 0, width, height)
        blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)
        seamMask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(width * height)
        for (i in canvasPixels.indices) {
            val maskAlpha = Color.red(maskPixels[i]) / 255f
            val oc = canvasPixels[i]
            val bc = blurPixels[i]
            val r = (Color.red(oc) * (1 - maskAlpha) + Color.red(bc) * maskAlpha).toInt().coerceIn(0, 255)
            val g = (Color.green(oc) * (1 - maskAlpha) + Color.green(bc) * maskAlpha).toInt().coerceIn(0, 255)
            val b = (Color.blue(oc) * (1 - maskAlpha) + Color.blue(bc) * maskAlpha).toInt().coerceIn(0, 255)
            resultPixels[i] = Color.argb(255, r, g, b)
        }
        canvas.setPixels(resultPixels, 0, width, 0, 0, width, height)

        blurred.recycle()
        seamMask.recycle()
    }

    /**
     * 在扩展区域添加噪点
     */
    private fun addExpansionNoise(canvas: Bitmap, imagePlacement: RectF) {
        val width = canvas.width
        val height = canvas.height

        // 创建扩展区域掩码（扩展区域=白色，原图区域=黑色）
        val expansionMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(expansionMask)
        maskCanvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK }
        maskCanvas.drawRect(imagePlacement, paint)

        // 生成噪点图
        val noise = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val noisePixels = IntArray(width * height)
        val random = Random()
        val noiseAmount = 20
        for (i in noisePixels.indices) {
            val n = 128 + random.nextInt(noiseAmount * 2) - noiseAmount
            noisePixels[i] = Color.argb(80, n, n, n)
        }
        noise.setPixels(noisePixels, 0, width, 0, 0, width, height)

        // 只在扩展区域混合噪点
        val canvasPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        canvas.getPixels(canvasPixels, 0, width, 0, 0, width, height)
        expansionMask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(width * height)
        for (i in canvasPixels.indices) {
            val maskStrength = Color.red(maskPixels[i]) / 255f * 0.5f
            val oc = canvasPixels[i]
            val nc = noisePixels[i]
            val r = (Color.red(oc) * (1 - maskStrength) + Color.red(nc) * maskStrength).toInt().coerceIn(0, 255)
            val g = (Color.green(oc) * (1 - maskStrength) + Color.green(nc) * maskStrength).toInt().coerceIn(0, 255)
            val b = (Color.blue(oc) * (1 - maskStrength) + Color.blue(nc) * maskStrength).toInt().coerceIn(0, 255)
            resultPixels[i] = Color.argb(255, r, g, b)
        }
        canvas.setPixels(resultPixels, 0, width, 0, 0, width, height)

        noise.recycle()
        expansionMask.recycle()
    }

    /**
     * 盒式模糊
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
