package com.livecompose.livecapture.core.editing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.livecompose.livecapture.core.portrait.PortraitImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 天空替换器
 *
 * 基于颜色分析的天空区域检测与替换，对应 iOS 端 CoreImage + Vision 的天空替换能力。
 *
 * ## 主要功能
 * - [replaceSky]：检测天空区域并替换为指定类型天空，同时调整前景光照
 * - [detectSkyRegion]：使用颜色分析 + 形态学操作检测天空区域，生成掩码
 * - [generateSkyGradient]：生成多种类型天空渐变（晴天/日落/夜晚/星空/极光/戏剧）
 * - [adjustForegroundLighting]：根据新天空调整前景光照，使合成更自然
 *
 * ## 技术栈
 * - 像素颜色分析 + 可分离形态学（膨胀/腐蚀）实现天空检测
 * - LinearGradient / RadialGradient + Canvas 生成天空渐变
 * - PorterDuff + ColorMatrix 实现合成与前景光照调整
 * - RenderScript 高斯模糊（复用 [PortraitImageUtils]）用于边缘羽化
 *
 * ## 使用说明
 * 处理前应先调用 [PortraitImageUtils.initRenderScript] 初始化 RenderScript，
 * 否则将退化为纯 Java 高斯模糊实现，性能较低。
 */
class SkyReplacer {

    /**
     * 天空类型
     *
     * @property displayName 中文展示名称
     */
    enum class SkyType {
        SUNNY,
        SUNSET,
        NIGHT,
        STARRY,
        AURORA,
        DRAMATIC;

        val displayName: String
            get() = when (this) {
                SUNNY -> "晴天"
                SUNSET -> "日落"
                NIGHT -> "夜晚"
                STARRY -> "星空"
                AURORA -> "极光"
                DRAMATIC -> "戏剧"
            }
    }

    /** 极光带配置 */
    private class AuroraBand(
        val centerYFraction: Float,
        val amplitudeFraction: Float,
        val frequency: Float,
        val thicknessFraction: Float,
        val colors: IntArray
    )

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /**
     * 替换图像中的天空
     *
     * 处理流程：
     * 1. 检测天空区域并创建掩码（白色=天空，黑色=非天空）
     * 2. 生成新天空渐变 Bitmap
     * 3. 合成：新天空 * 掩码 + 原图 * (1 - 掩码)
     * 4. 调整前景光照以匹配新天空
     *
     * @param image 输入 Bitmap
     * @param skyType 目标天空类型
     * @return 替换天空后的 Bitmap，失败返回原图
     */
    suspend fun replaceSky(image: Bitmap, skyType: SkyType): Bitmap = withContext(Dispatchers.Default) {
        if (image.width <= 0 || image.height <= 0) return@withContext image
        _isProcessing.value = true
        try {
            // 1. 检测天空区域
            val skyMask = detectSkyRegion(image)
            // 若未检测到天空区域则直接返回原图
            if (isMaskEmpty(skyMask)) return@withContext image

            // 2. 生成新天空
            val newSky = generateSkyGradient(skyType, image.width, image.height)

            // 3. 合成：newSky * mask + original * (1 - mask)
            val composited = PortraitImageUtils.blendWithMask(newSky, image, skyMask)

            // 4. 调整前景光照以匹配新天空
            adjustForegroundLighting(composited, skyType, skyMask)
        } catch (e: Exception) {
            // 出错时返回原图
            image
        } finally {
            _isProcessing.value = false
        }
    }

    /**
     * 检测图像中的天空区域，返回掩码（白色=天空，黑色=非天空）
     *
     * 处理流程：
     * 1. 基于颜色分析生成初始二值掩码
     * 2. 形态学闭运算（先膨胀再腐蚀）填充小孔洞
     * 3. 高斯模糊羽化边缘
     * 4. 按垂直位置加权（顶部 60% 偏向天空）
     *
     * @param image 输入图像
     * @return 天空掩码 Bitmap（alpha 通道存储掩码强度）
     */
    private fun detectSkyRegion(image: Bitmap): Bitmap {
        val width = image.width
        val height = image.height

        // 1. 基于颜色分析生成初始天空掩码
        val rawMask = createSkyColorMask(image)

        // 2. 形态学闭运算（膨胀再腐蚀）填充孔洞
        val closedMask = applyMorphologicalClose(rawMask)

        // 3. 高斯模糊羽化边缘（半径近似对应 sigma ≈ 8）
        val feathered = PortraitImageUtils.gaussianBlur(closedMask, GAUSSIAN_RADIUS)

        // 4. 按垂直位置加权（顶部 60% 偏向天空）
        return applyVerticalWeight(feathered)
    }

    /**
     * 基于颜色分析创建天空掩码
     *
     * @param image 输入图像
     * @return 二值掩码 Bitmap（天空=白色，非天空=黑色）
     */
    private fun createSkyColorMask(image: Bitmap): Bitmap {
        val width = image.width
        val height = image.height
        val imgPixels = IntArray(width * height)
        image.getPixels(imgPixels, 0, width, 0, 0, width, height)

        val maskPixels = IntArray(width * height)
        for (i in imgPixels.indices) {
            val c = imgPixels[i]
            val r = Color.red(c) / 255f
            val g = Color.green(c) / 255f
            val b = Color.blue(c) / 255f
            val v = if (isSkyColor(r, g, b)) 255 else 0
            // RGB 与 alpha 同时存储掩码值，便于后续模糊保持一致
            maskPixels[i] = (v shl 24) or (v shl 16) or (v shl 8) or v
        }

        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mask.setPixels(maskPixels, 0, width, 0, 0, width, height)
        return mask
    }

    /**
     * 判断颜色是否为天空颜色
     *
     * - 蓝色天空：蓝色通道明显高于红色
     * - 白色/浅灰天空（多云）：所有通道接近且亮度较高
     * - 青蓝色天空
     *
     * @param r 红色分量 [0,1]
     * @param g 绿色分量 [0,1]
     * @param b 蓝色分量 [0,1]
     * @return 是否为天空颜色
     */
    private fun isSkyColor(r: Float, g: Float, b: Float): Boolean {
        // 蓝色天空：蓝色通道明显高于红色
        if (b > r + 0.1f && b > g * 0.9f && b > 0.4f) return true

        // 白色/浅灰天空（多云）：所有通道接近且亮度较高
        val brightness = (r + g + b) / 3f
        if (brightness > 0.6f && abs(r - g) < 0.15f && abs(g - b) < 0.15f) return true

        // 青蓝色天空
        if (b > 0.5f && g > 0.5f && b > r * 1.3f && g > r * 1.2f) return true

        return false
    }

    /**
     * 形态学闭运算（先膨胀再腐蚀）填充孔洞
     *
     * 使用可分离的盒式最大/最小滤波近似形态学操作，为提升性能在降采样图上执行。
     *
     * @param mask 输入二值掩码
     * @return 闭运算后的掩码 Bitmap
     */
    private fun applyMorphologicalClose(mask: Bitmap): Bitmap {
        val width = mask.width
        val height = mask.height

        // 降采样以提升形态学操作性能
        val maxDim = MORPH_MAX_DIM
        val scale = if (maxOf(width, height) > maxDim) {
            maxDim.toFloat() / maxOf(width, height)
        } else {
            1f
        }
        val mw = (width * scale).toInt().coerceAtLeast(1)
        val mh = (height * scale).toInt().coerceAtLeast(1)
        val working = if (scale < 1f) {
            Bitmap.createScaledBitmap(mask, mw, mh, true)
        } else {
            mask
        }

        val pixels = IntArray(mw * mh)
        working.getPixels(pixels, 0, mw, 0, 0, mw, mh)
        val alpha = IntArray(mw * mh) { Color.alpha(pixels[it]) }

        // 先膨胀（最大值滤波）再腐蚀（最小值滤波）
        val dilated = morphApply(alpha, mw, mh, DILATE_RADIUS) { a, b -> maxOf(a, b) }
        val closed = morphApply(dilated, mw, mh, ERODE_RADIUS) { a, b -> minOf(a, b) }

        val outPixels = IntArray(mw * mh)
        for (i in closed.indices) {
            val v = closed[i]
            outPixels[i] = (v shl 24) or (v shl 16) or (v shl 8) or v
        }
        val smallOut = Bitmap.createBitmap(mw, mh, Bitmap.Config.ARGB_8888)
        smallOut.setPixels(outPixels, 0, mw, 0, 0, mw, mh)

        return if (scale < 1f) {
            Bitmap.createScaledBitmap(smallOut, width, height, true)
        } else {
            smallOut
        }
    }

    /**
     * 可分离盒式形态学滤波（水平 + 垂直两遍）
     *
     * @param values 输入灰度值数组
     * @param width 宽度
     * @param height 高度
     * @param radius 滤波半径
     * @param combine 聚合函数（max 膨胀，min 腐蚀）
     * @return 滤波后的灰度值数组
     */
    private fun morphApply(
        values: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        combine: (Int, Int) -> Int
    ): IntArray {
        // 水平方向
        val temp = IntArray(width * height)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var v = values[row + x]
                val x0 = (x - radius).coerceAtLeast(0)
                val x1 = (x + radius).coerceAtMost(width - 1)
                for (kx in x0..x1) v = combine(v, values[row + kx])
                temp[row + x] = v
            }
        }
        // 垂直方向
        val out = IntArray(width * height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                var v = temp[y * width + x]
                val y0 = (y - radius).coerceAtLeast(0)
                val y1 = (y + radius).coerceAtMost(height - 1)
                for (ky in y0..y1) v = combine(v, temp[ky * width + x])
                out[y * width + x] = v
            }
        }
        return out
    }

    /**
     * 按垂直位置加权掩码（顶部 60% 偏向天空）
     *
     * 使用线性梯度：顶部权重 1.0，在 60% 高度处衰减为 0.0，下部保持 0。
     *
     * @param mask 输入掩码
     * @return 加权后的掩码 Bitmap
     */
    private fun applyVerticalWeight(mask: Bitmap): Bitmap {
        val width = mask.width
        val height = mask.height
        val result = mask.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        val fadeEnd = (height * 0.6f).coerceAtLeast(1f)
        for (y in 0 until height) {
            val weight = (1f - y / fadeEnd).coerceIn(0f, 1f)
            val rowStart = y * width
            for (x in 0 until width) {
                val a = Color.alpha(pixels[rowStart + x])
                val v = (a * weight).toInt()
                pixels[rowStart + x] = (v shl 24) or (v shl 16) or (v shl 8) or v
            }
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 判断掩码是否为空（几乎无天空像素）
     */
    private fun isMaskEmpty(mask: Bitmap): Boolean {
        val width = mask.width
        val height = mask.height
        val pixels = IntArray(width * height)
        mask.getPixels(pixels, 0, width, 0, 0, width, height)
        var sum = 0
        for (p in pixels) sum += Color.alpha(p)
        // 低于 1% 像素为天空则视为无天空
        return sum < (pixels.size * 255 * 0.01f)
    }

    /**
     * 根据天空类型生成渐变天空
     *
     * @param type 天空类型
     * @param width 输出宽度
     * @param height 输出高度
     * @return 天空渐变 Bitmap
     */
    private fun generateSkyGradient(type: SkyType, width: Int, height: Int): Bitmap {
        return when (type) {
            SkyType.SUNNY -> generateSunnySky(width, height)
            SkyType.SUNSET -> generateSunsetSky(width, height)
            SkyType.NIGHT -> generateNightSky(width, height)
            SkyType.STARRY -> generateStarrySky(width, height)
            SkyType.AURORA -> generateAuroraSky(width, height)
            SkyType.DRAMATIC -> generateDramaticSky(width, height)
        }
    }

    /**
     * 晴天天空：蓝色到浅蓝渐变，附加轻微云层噪点
     */
    private fun generateSunnySky(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                0xFF5599ED.toInt(), 0xFFC8E6FF.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        return addCloudTexture(bitmap, 0.15f)
    }

    /**
     * 日落天空：深紫 -> 红橙 -> 亮橙的多停靠点渐变，附加云层噪点
     */
    private fun generateSunsetSky(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val colors = intArrayOf(
            0xFF7B2D8E.toInt(),  // 顶部 深紫
            0xFFD94059.toInt(),  // 中部 红橙
            0xFFFF8C4D.toInt()   // 底部 亮橙
        )
        val positions = floatArrayOf(0f, 0.5f, 1f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                colors, positions, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        return addCloudTexture(bitmap, 0.2f)
    }

    /**
     * 夜晚天空：深黑到深蓝渐变
     */
    private fun generateNightSky(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                0xFF0A0E27.toInt(), 0xFF1A2456.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        return bitmap
    }

    /**
     * 星空天空：深色夜空基础 + 随机星点（大小与亮度变化，亮星带光晕）
     */
    private fun generateStarrySky(width: Int, height: Int): Bitmap {
        // 基础夜空渐变
        val bitmap = generateNightSky(width, height)
        val canvas = Canvas(bitmap)
        val rnd = Random()
        val starCount = (width * height / 2500).coerceIn(60, 600)
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until starCount) {
            val x = rnd.nextFloat() * width
            // 星点偏向上方 85% 区域
            val y = rnd.nextFloat() * height * 0.85f
            val brightness = rnd.nextFloat()
            val isBright = brightness > 0.92f
            val radius = if (isBright) 2.5f else 1.2f
            val alpha = ((0.4f + brightness * 0.6f) * 255f).toInt().coerceIn(0, 255)

            // 亮星附加径向光晕
            if (isBright) {
                val glowRadius = radius * 3f
                starPaint.shader = RadialGradient(
                    x, y, glowRadius,
                    intArrayOf(
                        Color.argb(alpha, 255, 255, 255),
                        Color.argb(0, 255, 255, 255)
                    ),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(x, y, glowRadius, starPaint)
                starPaint.shader = null
            }

            starPaint.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawCircle(x, y, radius, starPaint)
        }
        return bitmap
    }

    /**
     * 极光天空：暗色夜空基础 + 正弦波形绿/青极光带
     */
    private fun generateAuroraSky(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 暗色夜空底
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                0xFF050820.toInt(), 0xFF0A1830.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), basePaint)

        // 多条极光带：正弦波形 + 颜色渐变
        val bands = listOf(
            AuroraBand(0.35f, 0.06f, 0.0025f, 0.18f, intArrayOf(0xFF00CC66.toInt(), 0x0000CC66.toInt())),
            AuroraBand(0.45f, 0.05f, 0.0035f, 0.15f, intArrayOf(0xFF33E6B0.toInt(), 0x0033E6B0.toInt())),
            AuroraBand(0.55f, 0.07f, 0.0020f, 0.12f, intArrayOf(0xFF5566FF.toInt(), 0x005566FF.toInt()))
        )
        val rnd = Random(7L)
        for (band in bands) {
            drawAuroraBand(canvas, width, height, band, rnd)
        }

        // 轻微噪点模拟极光纹理
        return addCloudTexture(bitmap, 0.08f)
    }

    /**
     * 绘制单条极光带（正弦波形带状区域，垂直方向颜色渐变）
     */
    private fun drawAuroraBand(canvas: Canvas, width: Int, height: Int, band: AuroraBand, rnd: Random) {
        val centerY = height * band.centerYFraction
        val amplitude = height * band.amplitudeFraction
        val thickness = (height * band.thicknessFraction).coerceAtLeast(2f)
        val phase = rnd.nextFloat() * (2f * PI).toFloat()
        val step = (width / 60).coerceAtLeast(4)

        val path = Path()
        // 顶边：正弦曲线
        var first = true
        var x = 0
        while (x <= width) {
            val y = centerY + amplitude * sin(x * band.frequency + phase).toFloat() - thickness / 2f
            if (first) {
                path.moveTo(x.toFloat(), y)
                first = false
            } else {
                path.lineTo(x.toFloat(), y)
            }
            x += step
        }
        // 底边：反向正弦曲线
        x = width
        while (x >= 0) {
            val y = centerY + amplitude * sin(x * band.frequency + phase).toFloat() + thickness / 2f
            path.lineTo(x.toFloat(), y)
            x -= step
        }
        path.close()

        // 垂直渐变填充（颜色 -> 透明）
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, centerY - thickness / 2f,
                0f, centerY + thickness / 2f,
                band.colors, null, Shader.TileMode.CLAMP
            )
            alpha = 180
        }
        canvas.drawPath(path, paint)
    }

    /**
     * 戏剧性天空：深灰到浅灰渐变 + 重度云层噪点纹理
     */
    private fun generateDramaticSky(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                0xFF4A4A4A.toInt(), 0xFF8C8C8C.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        // 重度云层噪点
        return addCloudTexture(bitmap, 0.3f)
    }

    /**
     * 添加云层纹理
     *
     * 生成低分辨率随机噪点，放大并模糊后形成柔和云团，叠加到天空上。
     *
     * @param sky 输入天空 Bitmap
     * @param intensity 叠加强度 [0,1]
     * @return 附加云层纹理后的 Bitmap
     */
    private fun addCloudTexture(sky: Bitmap, intensity: Float): Bitmap {
        val width = sky.width
        val height = sky.height

        // 低分辨率随机噪点
        val noiseW = (width / 12).coerceAtLeast(8)
        val noiseH = (height / 12).coerceAtLeast(8)
        val noise = Bitmap.createBitmap(noiseW, noiseH, Bitmap.Config.ARGB_8888)
        val noisePixels = IntArray(noiseW * noiseH)
        val rnd = Random()
        for (i in noisePixels.indices) {
            val v = rnd.nextInt(256)
            noisePixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        noise.setPixels(noisePixels, 0, noiseW, 0, 0, noiseW, noiseH)

        // 放大并模糊形成云团
        val scaled = Bitmap.createScaledBitmap(noise, width, height, true)
        val cloud = PortraitImageUtils.gaussianBlur(scaled, 10f)

        // 以 OVERLAY 模式叠加，亮处提亮、暗处压暗形成云层质感
        val result = sky.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (intensity * 255f).toInt().coerceIn(0, 255)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        }
        canvas.drawBitmap(cloud, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        return result
    }

    /**
     * 调整前景光照以匹配新天空
     *
     * 在非天空区域（反转掩码）应用 ColorMatrix 调整，天空区域保持原样。
     *
     * @param image 已合成新天空的图像
     * @param skyType 天空类型
     * @param skyMask 天空掩码
     * @return 前景光照调整后的 Bitmap
     */
    private fun adjustForegroundLighting(image: Bitmap, skyType: SkyType, skyMask: Bitmap): Bitmap {
        // 创建前景掩码（非天空区域 = 反转天空掩码）
        val fgMask = invertMask(skyMask)

        val adjusted = applyColorMatrix(image, foregroundColorMatrix(skyType))

        // 在前景区域应用调整，天空区域保持原样
        return PortraitImageUtils.blendWithMask(adjusted, image, fgMask)
    }

    /**
     * 反转掩码（前景掩码 = 255 - 天空掩码）
     */
    private fun invertMask(mask: Bitmap): Bitmap {
        val width = mask.width
        val height = mask.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        mask.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val v = 255 - Color.alpha(pixels[i])
            pixels[i] = (v shl 24) or (v shl 16) or (v shl 8) or v
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 应用颜色矩阵到整张图像
     */
    private fun applyColorMatrix(image: Bitmap, matrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(image, 0f, 0f, paint)
        return result
    }

    /**
     * 根据天空类型构建前景光照颜色矩阵
     *
     * - SUNNY：轻微暖色调
     * - SUNSET：强烈暖橙色调
     * - NIGHT / STARRY / AURORA：冷色调 + 压暗
     * - DRAMATIC：降饱和 + 降低对比度
     */
    private fun foregroundColorMatrix(skyType: SkyType): ColorMatrix {
        return when (skyType) {
            SkyType.SUNNY -> {
                // 轻微暖色调：增红减蓝 + 轻微提亮
                ColorMatrix().apply {
                    setScale(1.06f, 1.0f, 0.94f, 1f)
                    postConcat(ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, 8f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, -8f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
            }
            SkyType.SUNSET -> {
                // 强烈暖橙色调
                ColorMatrix().apply {
                    setScale(1.2f, 1.05f, 0.78f, 1f)
                    postConcat(ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, 18f,
                        0f, 1f, 0f, 0f, 4f,
                        0f, 0f, 1f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
            }
            SkyType.NIGHT, SkyType.STARRY, SkyType.AURORA -> {
                // 冷色调 + 压暗
                ColorMatrix().apply {
                    setScale(0.86f, 0.92f, 1.12f, 1f)
                    postConcat(ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, -10f,
                        0f, 1f, 0f, 0f, -6f,
                        0f, 0f, 1f, 0f, 8f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
            }
            SkyType.DRAMATIC -> {
                // 降饱和 + 降低对比度
                val m = ColorMatrix().apply { setSaturation(0.6f) }
                m.postConcat(ColorMatrix(floatArrayOf(
                    0.9f, 0f, 0f, 0f, 12.75f,
                    0f, 0.9f, 0f, 0f, 12.75f,
                    0f, 0f, 0.9f, 0f, 12.75f,
                    0f, 0f, 0f, 1f, 0f
                )))
                m
            }
        }
    }

    companion object {
        /** 形态学操作降采样最大边长 */
        private const val MORPH_MAX_DIM = 1000
        /** 膨胀半径 */
        private const val DILATE_RADIUS = 5
        /** 腐蚀半径 */
        private const val ERODE_RADIUS = 4
        /** 羽化高斯模糊半径（近似 sigma ≈ 8） */
        private const val GAUSSIAN_RADIUS = 16f
    }
}
