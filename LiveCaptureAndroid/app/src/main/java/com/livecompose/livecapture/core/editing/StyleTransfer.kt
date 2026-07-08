package com.livecompose.livecapture.core.editing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.portrait.PortraitImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

/**
 * 风格迁移器
 *
 * 对应 iOS 端 StyleTransfer.swift，基于滤镜链的艺术风格转换。
 * 每种风格是多个图像处理操作的特定组合，通过 [intensity] 控制风格强度。
 *
 * ## 支持的风格
 * - WATERCOLOR: 水彩画（模糊 + 边缘强化 + 纸纹）
 * - OIL_PAINTING: 油画（像素化 + 中值滤波 + 发光）
 * - SKETCH: 素描（灰度 + 边缘 + 反相）
 * - COMIC_BOOK: 漫画（色调分离 + 边缘 + 半色调）
 * - PIXEL_ART: 像素艺术（强像素化 + 色调分离）
 * - VINTAGE: 复古胶片（棕褐色 + 暗角 + 噪点）
 * - NEON: 霓虹灯（边缘 + 发光 + 色彩映射）
 * - PENCIL: 铅笔素描（灰度 + 边缘 + 柔化）
 */
class StyleTransfer {

    companion object {
        private const val TAG = "StyleTransfer"
    }

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /**
     * 艺术风格
     */
    enum class ArtStyle {
        WATERCOLOR,
        OIL_PAINTING,
        SKETCH,
        COMIC_BOOK,
        PIXEL_ART,
        VINTAGE,
        NEON,
        PENCIL;

        /** 显示名称 */
        val displayName: String
            get() = when (this) {
                WATERCOLOR -> "水彩画"
                OIL_PAINTING -> "油画"
                SKETCH -> "素描"
                COMIC_BOOK -> "漫画"
                PIXEL_ART -> "像素艺术"
                VINTAGE -> "复古"
                NEON -> "霓虹灯"
                PENCIL -> "铅笔"
            }
    }

    /**
     * 应用艺术风格转换
     *
     * @param image 输入 Bitmap
     * @param style 目标艺术风格
     * @param intensity 风格强度（0.0 = 原图，1.0 = 完全风格化）
     * @return 风格化后的 Bitmap，失败返回原图
     */
    fun applyStyle(image: Bitmap, style: ArtStyle, intensity: Float = 0.7f): Bitmap {
        _isProcessing.value = true
        try {
            val clampedIntensity = intensity.coerceIn(0f, 1f)
            if (clampedIntensity <= 0f) return image

            val styled: Bitmap = when (style) {
                ArtStyle.WATERCOLOR -> applyWatercolor(image)
                ArtStyle.OIL_PAINTING -> applyOilPainting(image)
                ArtStyle.SKETCH -> applySketch(image)
                ArtStyle.COMIC_BOOK -> applyComicBook(image)
                ArtStyle.PIXEL_ART -> applyPixelArt(image)
                ArtStyle.VINTAGE -> applyVintage(image)
                ArtStyle.NEON -> applyNeon(image)
                ArtStyle.PENCIL -> applyPencil(image)
            }

            // 根据强度混合原图和风格化图像
            return if (clampedIntensity >= 1.0f) {
                styled
            } else {
                blendImages(image, styled, clampedIntensity)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "风格迁移失败: $style", e)
            return image
        } finally {
            _isProcessing.value = false
        }
    }

    // MARK: - 水彩画效果

    /**
     * 水彩画：模糊 + 边缘强化 + 颜色柔化 + 纸纹
     */
    private fun applyWatercolor(image: Bitmap): Bitmap {
        // 1. 轻微高斯模糊模拟水彩渲染
        val blurred = boxBlur(image, 3)

        // 2. 提取边缘（Sobel）
        val edges = sobelEdgeDetect(blurred, 3)

        // 3. 将边缘叠加到模糊图像上（正片叠底）
        val edgeBlended = multiplyBlend(edges, blurred)

        // 4. 调整颜色使水彩感更强
        var result = PortraitImageUtils.adjustColorControls(
            edgeBlended,
            saturation = 1.2f,
            contrast = 0.95f,
            brightness = 0.05f
        )

        // 5. 添加纸纹噪点
        result = addNoise(result, 0.05f)

        if (blurred !== image) blurred.recycle()
        edges.recycle()
        edgeBlended.recycle()

        return result
    }

    // MARK: - 油画效果

    /**
     * 油画：像素化 + 中值滤波 + 发光效果
     */
    private fun applyOilPainting(image: Bitmap): Bitmap {
        // 1. 轻微像素化模拟油画笔触
        val pixellated = pixelate(image, 4)

        // 2. 中值滤波平滑色块
        val smoothed = medianFilter(pixellated, 3)

        // 3. 发光效果
        var result = bloom(smoothed, 10, 0.5f)

        // 4. 增强对比度和饱和度
        result = PortraitImageUtils.adjustColorControls(
            result,
            saturation = 1.3f,
            contrast = 1.15f,
            brightness = 0f
        )

        // 5. 增加锐度突出笔触
        result = unsharpMask(result, 0.3f, 2)

        pixellated.recycle()
        smoothed.recycle()

        return result
    }

    // MARK: - 素描效果

    /**
     * 素描：单色 + 边缘强化 + 反相
     */
    private fun applySketch(image: Bitmap): Bitmap {
        // 1. 转换为灰度并增强对比度
        val grayscale = PortraitImageUtils.adjustColorControls(
            image,
            saturation = 0f,
            contrast = 1.1f,
            brightness = 0f
        )

        // 2. 提取边缘
        val edges = sobelEdgeDetect(grayscale, 2)

        // 3. 反相颜色（白底黑线）
        val inverted = invertColors(edges)

        // 4. 调整亮度和对比度
        var result = PortraitImageUtils.adjustColorControls(
            inverted,
            brightness = 0.1f,
            contrast = 1.3f,
            saturation = 0f
        )

        // 5. 轻微模糊使线条柔和
        result = boxBlur(result, 1)

        grayscale.recycle()
        edges.recycle()
        inverted.recycle()

        return result
    }

    // MARK: - 漫画书效果

    /**
     * 漫画书：色调分离 + 边缘线条 + 半色调网点
     */
    private fun applyComicBook(image: Bitmap): Bitmap {
        // 1. 色调分离
        val posterized = posterize(image, 6)

        // 2. 提取边缘
        val edges = sobelEdgeDetect(image, 2)

        // 3. 边缘叠加（正片叠底）
        val edgeBlended = multiplyBlend(edges, posterized)

        // 4. 增强色彩
        var result = PortraitImageUtils.adjustColorControls(
            edgeBlended,
            saturation = 1.5f,
            contrast = 1.2f,
            brightness = 0.05f
        )

        // 5. 半色调网点效果
        result = halftone(result, 4)

        posterized.recycle()
        edges.recycle()
        edgeBlended.recycle()

        return result
    }

    // MARK: - 像素艺术效果

    /**
     * 像素艺术：像素化 + 色调分离 + 锐化
     */
    private fun applyPixelArt(image: Bitmap): Bitmap {
        // 1. 强像素化
        val pixellated = pixelate(image, 8)

        // 2. 色调分离
        var result = posterize(pixellated, 5)

        // 3. 增强对比度
        result = PortraitImageUtils.adjustColorControls(
            result,
            contrast = 1.3f,
            saturation = 1.2f,
            brightness = 0f
        )

        // 4. 锐化像素边缘
        result = unsharpMask(result, 0.5f, 1)

        pixellated.recycle()

        return result
    }

    // MARK: - 复古胶片效果

    /**
     * 复古胶片：棕褐色 + 暗角 + 噪点 + 褪色
     */
    private fun applyVintage(image: Bitmap): Bitmap {
        // 1. 棕褐色调
        var result = sepia(image, 0.7f)

        // 2. 褪色效果
        result = PortraitImageUtils.adjustColorControls(
            result,
            saturation = 0.6f,
            contrast = 0.85f,
            brightness = 0.05f
        )

        // 3. 暖色调偏移
        result = PortraitImageUtils.adjustTemperature(result, 0.3f)

        // 4. 暗角效果
        result = vignette(result, 0.8f)

        // 5. 胶片噪点
        result = addNoise(result, 0.08f)

        // 6. 轻微模糊模拟老镜头
        result = boxBlur(result, 1)

        return result
    }

    // MARK: - 霓虹灯效果

    /**
     * 霓虹灯：边缘提取 + 反相 + 色彩映射 + 发光
     */
    private fun applyNeon(image: Bitmap): Bitmap {
        // 1. 提取边缘
        val edges = sobelEdgeDetect(image, 2)

        // 2. 反相
        val inverted = invertColors(edges)

        // 3. 色彩矩阵：将线条转换为霓虹色（青/紫）
        val neonized = applyNeonColorMatrix(inverted)

        // 4. 发光效果
        var result = bloom(neonized, 5, 1.5f)

        // 5. 暗化原图作为背景
        val darkened = PortraitImageUtils.adjustExposure(image, -1.5f)
        val desaturated = PortraitImageUtils.adjustColorControls(
            darkened,
            saturation = 0.3f,
            contrast = 0.8f,
            brightness = 0f
        )

        // 6. 叠加霓虹效果
        result = screenBlend(result, desaturated)

        // 7. 增加对比度
        result = PortraitImageUtils.adjustColorControls(
            result,
            saturation = 1.4f,
            contrast = 1.3f,
            brightness = 0f
        )

        edges.recycle()
        inverted.recycle()
        neonized.recycle()
        darkened.recycle()
        desaturated.recycle()

        return result
    }

    // MARK: - 铅笔素描效果

    /**
     * 铅笔素描：灰度 + 边缘 + 柔化
     */
    private fun applyPencil(image: Bitmap): Bitmap {
        // 1. 灰度转换
        val grayscale = PortraitImageUtils.adjustColorControls(
            image,
            saturation = 0f,
            contrast = 1.05f,
            brightness = 0f
        )

        // 2. 边缘检测（较轻）
        val edges = sobelEdgeDetect(grayscale, 2)

        // 3. 反相
        val inverted = invertColors(edges)

        // 4. 柔化模糊
        var result = boxBlur(inverted, 1)

        // 5. 降低对比度使笔触柔和
        result = PortraitImageUtils.adjustColorControls(
            result,
            contrast = 0.9f,
            saturation = 0f,
            brightness = 0.05f
        )

        grayscale.recycle()
        edges.recycle()
        inverted.recycle()

        return result
    }

    // MARK: - 图像处理工具方法

    /**
     * 像素化（块平均）
     *
     * @param bitmap 输入
     * @param blockSize 块大小
     */
    internal fun pixelate(bitmap: Bitmap, blockSize: Int): Bitmap {
        if (blockSize <= 1) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (by in 0 until height step blockSize) {
            for (bx in 0 until width step blockSize) {
                // 计算块内平均颜色
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                val yEnd = (by + blockSize).coerceAtMost(height)
                val xEnd = (bx + blockSize).coerceAtMost(width)
                for (y in by until yEnd) {
                    for (x in bx until xEnd) {
                        val c = pixels[y * width + x]
                        rSum += Color.red(c); gSum += Color.green(c); bSum += Color.blue(c)
                        count++
                    }
                }
                val avgColor = if (count > 0) Color.rgb(rSum / count, gSum / count, bSum / count) else Color.BLACK
                // 填充块
                for (y in by until yEnd) {
                    for (x in bx until xEnd) {
                        pixels[y * width + x] = avgColor
                    }
                }
            }
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 色调分离（量化每通道为 N 级）
     *
     * @param bitmap 输入
     * @param levels 量化级数
     */
    internal fun posterize(bitmap: Bitmap, levels: Int): Bitmap {
        if (levels <= 0 || levels >= 255) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val step = 255f / (levels - 1)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (Math.round(Color.red(c) / step) * step).toInt().coerceIn(0, 255)
            val g = (Math.round(Color.green(c) / step) * step).toInt().coerceIn(0, 255)
            val b = (Math.round(Color.blue(c) / step) * step).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(c), r, g, b)
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Sobel 边缘检测
     *
     * @param bitmap 输入
     * @param radius 检测半径
     */
    internal fun sobelEdgeDetect(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 转灰度
        val gray = IntArray(width * height)
        for (i in pixels.indices) {
            val c = pixels[i]
            gray[i] = (Color.red(c) * 0.299 + Color.green(c) * 0.587 + Color.blue(c) * 0.114).toInt()
        }

        val output = IntArray(width * height)
        val r = radius.coerceAtLeast(1)
        // Sobel 算子
        val gx = intArrayOf(-1, 0, 1, -2, 0, 2, -1, 0, 1)
        val gy = intArrayOf(-1, -2, -1, 0, 0, 0, 1, 2, 1)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sumX = 0
                var sumY = 0
                var ki = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val g = gray[(y + dy) * width + (x + dx)]
                        sumX += g * gx[ki]
                        sumY += g * gy[ki]
                        ki++
                    }
                }
                val magnitude = Math.sqrt((sumX * sumX + sumY * sumY).toDouble()).toInt().coerceIn(0, 255)
                output[y * width + x] = Color.argb(255, magnitude, magnitude, magnitude)
            }
        }
        // 边缘像素填充黑色
        for (x in 0 until width) {
            output[x] = Color.BLACK
            output[(height - 1) * width + x] = Color.BLACK
        }
        for (y in 0 until height) {
            output[y * width] = Color.BLACK
            output[y * width + width - 1] = Color.BLACK
        }
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 棕褐色调
     *
     * @param bitmap 输入
     * @param intensity 强度（0.0 - 1.0）
     */
    internal fun sepia(bitmap: Bitmap, intensity: Float): Bitmap {
        val matrix = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sepiaBitmap = applyColorMatrix(bitmap, matrix)
        if (intensity >= 1.0f) return sepiaBitmap
        return blendImages(bitmap, sepiaBitmap, intensity)
    }

    /**
     * 添加噪点
     *
     * @param bitmap 输入
     * @param intensity 噪点强度（0.0 - 1.0）
     */
    internal fun addNoise(bitmap: Bitmap, intensity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        val random = Random()
        val amount = (intensity * 255).toInt()

        for (i in pixels.indices) {
            val c = pixels[i]
            val noise = random.nextInt(amount * 2) - amount
            val r = (Color.red(c) + noise).coerceIn(0, 255)
            val g = (Color.green(c) + noise).coerceIn(0, 255)
            val b = (Color.blue(c) + noise).coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(c), r, g, b)
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 暗角效果
     *
     * @param bitmap 输入
     * @param intensity 强度（0.0 - 1.0）
     */
    internal fun vignette(bitmap: Bitmap, intensity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.max(width, height) / 2f * 1.2f
        val shader = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb((intensity * 200).toInt(), 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return result
    }

    /**
     * 中值滤波
     *
     * @param bitmap 输入
     * @param kernelSize 核大小（奇数）
     */
    internal fun medianFilter(bitmap: Bitmap, kernelSize: Int): Bitmap {
        if (kernelSize <= 1) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)
        val half = kernelSize / 2

        for (y in 0 until height) {
            for (x in 0 until width) {
                val rList = ArrayList<Int>()
                val gList = ArrayList<Int>()
                val bList = ArrayList<Int>()
                for (dy in -half..half) {
                    for (dx in -half..half) {
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val c = pixels[ny * width + nx]
                        rList.add(Color.red(c))
                        gList.add(Color.green(c))
                        bList.add(Color.blue(c))
                    }
                }
                rList.sort(); gList.sort(); bList.sort()
                val mid = rList.size / 2
                output[y * width + x] = Color.argb(
                    Color.alpha(pixels[y * width + x]),
                    rList[mid], gList[mid], bList[mid]
                )
            }
        }
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 发光效果（模糊 + 屏幕混合）
     *
     * @param bitmap 输入
     * @param radius 模糊半径
     * @param intensity 发光强度
     */
    internal fun bloom(bitmap: Bitmap, radius: Int, intensity: Float): Bitmap {
        val blurred = boxBlur(bitmap, radius)
        val result = screenBlend(blurred, bitmap, intensity)
        if (blurred !== bitmap) blurred.recycle()
        return result
    }

    /**
     * 颜色反相
     */
    internal fun invertColors(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrix(bitmap, matrix)
    }

    /**
     * 半色调网点效果
     *
     * @param bitmap 输入
     * @param dotSize 网点大小
     */
    internal fun halftone(bitmap: Bitmap, dotSize: Int): Bitmap {
        if (dotSize <= 0) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }

        for (by in 0 until height step dotSize) {
            for (bx in 0 until width step dotSize) {
                // 计算块平均亮度
                var lumSum = 0; var count = 0
                val yEnd = (by + dotSize).coerceAtMost(height)
                val xEnd = (bx + dotSize).coerceAtMost(width)
                for (y in by until yEnd) {
                    for (x in bx until xEnd) {
                        val c = pixels[y * width + x]
                        lumSum += (Color.red(c) * 0.299 + Color.green(c) * 0.587 + Color.blue(c) * 0.114).toInt()
                        count++
                    }
                }
                val avgLum = if (count > 0) lumSum / count else 0
                // 亮度越低网点越大
                val dotRadius = (dotSize / 2f * (1f - avgLum / 255f)).coerceAtLeast(0f)
                if (dotRadius > 0.5f) {
                    canvas.drawCircle(
                        (bx + dotSize / 2f),
                        (by + dotSize / 2f),
                        dotRadius,
                        paint
                    )
                }
            }
        }
        return result
    }

    // MARK: - 混合模式

    /**
     * 正片叠底混合
     */
    private fun multiplyBlend(top: Bitmap, bottom: Bitmap): Bitmap {
        val width = bottom.width
        val height = bottom.height
        val scaledTop = if (top.width == width && top.height == height) top
            else Bitmap.createScaledBitmap(top, width, height, true)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bottom, 0f, 0f, null)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
        canvas.drawBitmap(scaledTop, 0f, 0f, paint)
        if (scaledTop !== top) scaledTop.recycle()
        return result
    }

    /**
     * 屏幕混合
     */
    private fun screenBlend(top: Bitmap, bottom: Bitmap, intensity: Float = 1.0f): Bitmap {
        val width = bottom.width
        val height = bottom.height
        val scaledTop = if (top.width == width && top.height == height) top
            else Bitmap.createScaledBitmap(top, width, height, true)

        val screenResult = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(screenResult)
        canvas.drawBitmap(bottom, 0f, 0f, null)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        canvas.drawBitmap(scaledTop, 0f, 0f, paint)
        if (scaledTop !== top) scaledTop.recycle()

        if (intensity >= 1.0f) return screenResult
        return blendImages(bottom, screenResult, intensity)
    }

    /**
     * 按强度混合两图（线性插值）
     *
     * @param original 原图（intensity=0 时返回）
     * @param styled 风格化图（intensity=1 时返回）
     * @param intensity 混合强度
     */
    private fun blendImages(original: Bitmap, styled: Bitmap, intensity: Float): Bitmap {
        val width = original.width
        val height = original.height
        val scaledStyled = if (styled.width == width && styled.height == height) styled
            else Bitmap.createScaledBitmap(styled, width, height, true)

        val origPixels = IntArray(width * height)
        val styledPixels = IntArray(width * height)
        original.getPixels(origPixels, 0, width, 0, 0, width, height)
        scaledStyled.getPixels(styledPixels, 0, width, 0, 0, width, height)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val resultPixels = IntArray(width * height)
        val inv = 1.0f - intensity

        for (i in origPixels.indices) {
            val oc = origPixels[i]
            val sc = styledPixels[i]
            val r = (Color.red(oc) * inv + Color.red(sc) * intensity).toInt().coerceIn(0, 255)
            val g = (Color.green(oc) * inv + Color.green(sc) * intensity).toInt().coerceIn(0, 255)
            val b = (Color.blue(oc) * inv + Color.blue(sc) * intensity).toInt().coerceIn(0, 255)
            resultPixels[i] = Color.argb(Color.alpha(oc), r, g, b)
        }
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        if (scaledStyled !== styled) scaledStyled.recycle()
        return result
    }

    /**
     * 霓虹色彩矩阵（将灰度边缘映射为青/紫色霓虹）
     */
    private fun applyNeonColorMatrix(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix(floatArrayOf(
            0.2f, 0f, 0f, 0f, 25f,
            0f, 0.8f, 0f, 0f, 0f,
            0f, 0f, 1.0f, 0f, 76f,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrix(bitmap, matrix)
    }

    /**
     * Unsharp Mask 锐化
     */
    private fun unsharpMask(image: Bitmap, amount: Float, radius: Int): Bitmap {
        if (amount <= 0f) return image
        val width = image.width
        val height = image.height
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
     * 应用 ColorMatrix
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
