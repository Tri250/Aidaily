package com.livecompose.livecapture.core.lut

import android.graphics.Bitmap
import com.livecompose.livecapture.core.filter.SkinProtectionFilter
import com.livecompose.livecapture.core.processing.BloomProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * LUT 色彩处理器
 * 将 LutPreset 参数实时应用到图像
 */
class LutProcessor {

    private val bloomProcessor = BloomProcessor()

    /** 皮肤保护滤镜（延迟初始化，避免循环依赖） */
    val skinProtectionFilter by lazy { SkinProtectionFilter(this) }

    /**
     * 应用 LUT 预设到 Bitmap
     */
    suspend fun applyPreset(
        source: Bitmap,
        preset: LutPreset,
        onProgress: (Float) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        var result: Bitmap? = null
        try {
            val width = source.width
            val height = source.height
            val pixels = IntArray(width * height)
            source.getPixels(pixels, 0, width, 0, 0, width, height)

            val outputPixels = IntArray(width * height)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                var r = ((pixel shr 16) and 0xFF) / 255f
                var g = ((pixel shr 8) and 0xFF) / 255f
                var b = (pixel and 0xFF) / 255f

                // 曝光
                if (preset.exposure != 0f) {
                    val factor = 2f.pow(preset.exposure)
                    r = (r * factor).coerceIn(0f, 1f)
                    g = (g * factor).coerceIn(0f, 1f)
                    b = (b * factor).coerceIn(0f, 1f)
                }

                // 对比度
                if (preset.contrast != 1f) {
                    r = ((r - 0.5f) * preset.contrast + 0.5f).coerceIn(0f, 1f)
                    g = ((g - 0.5f) * preset.contrast + 0.5f).coerceIn(0f, 1f)
                    b = ((b - 0.5f) * preset.contrast + 0.5f).coerceIn(0f, 1f)
                }

                // 高光与阴影
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (preset.highlights != 1f) {
                    val highlightMask = (lum - 0.5f).coerceIn(0f, 0.5f) / 0.5f
                    val adjust = (preset.highlights - 1f) * highlightMask
                    r = (r + adjust).coerceIn(0f, 1f)
                    g = (g + adjust).coerceIn(0f, 1f)
                    b = (b + adjust).coerceIn(0f, 1f)
                }
                if (preset.shadows != 1f) {
                    val shadowMask = (0.5f - lum).coerceIn(0f, 0.5f) / 0.5f
                    val adjust = (preset.shadows - 1f) * shadowMask
                    r = (r + adjust).coerceIn(0f, 1f)
                    g = (g + adjust).coerceIn(0f, 1f)
                    b = (b + adjust).coerceIn(0f, 1f)
                }

                // 色温
                if (preset.warmth != 0f) {
                    val shift = preset.warmth / 100f
                    r = (r + shift * 0.1f).coerceIn(0f, 1f)
                    b = (b - shift * 0.1f).coerceIn(0f, 1f)
                }

                // 色调（绿/品）
                if (preset.tint != 0f) {
                    val shift = preset.tint / 100f
                    g = (g + shift * 0.05f).coerceIn(0f, 1f)
                }

                // 饱和度
                if (preset.saturation != 1f) {
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    r = (gray + (r - gray) * preset.saturation).coerceIn(0f, 1f)
                    g = (gray + (g - gray) * preset.saturation).coerceIn(0f, 1f)
                    b = (gray + (b - gray) * preset.saturation).coerceIn(0f, 1f)
                }

                // 褪色
                if (preset.fade > 0f) {
                    val blackLift = preset.fade * 0.15f
                    r = (r * (1f - blackLift) + blackLift).coerceIn(0f, 1f)
                    g = (g * (1f - blackLift) + blackLift).coerceIn(0f, 1f)
                    b = (b * (1f - blackLift) + blackLift).coerceIn(0f, 1f)
                }

                // 晕影
                // (applied in post-process below)

                val outR = (r * 255f).roundToInt().coerceIn(0, 255)
                val outG = (g * 255f).roundToInt().coerceIn(0, 255)
                val outB = (b * 255f).roundToInt().coerceIn(0, 255)
                outputPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB

                if (i % (width * 20) == 0) onProgress(i.toFloat() / pixels.size * 0.7f)
            }

            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            result.setPixels(outputPixels, 0, width, 0, 0, width, height)

            // 晕影效果
            if (preset.vignette > 0f) {
                applyVignette(result, preset.vignette)
            }

            // 颗粒
            if (preset.grain > 0f) {
                applyGrain(result, preset.grain)
            }

            // 锐化
            if (preset.sharpening > 0f) {
                applySharpening(result, preset.sharpening)
            }

            // Bloom
            if (preset.grain > 0f && preset.id != "original") {
                // 轻微 bloom 效果增强胶片感
                bloomProcessor.applyBloom(result, intensity = preset.grain * 0.1f, threshold = 0.85f, radius = 4)
            }

            onProgress(1f)
            result
        } catch (e: OutOfMemoryError) {
            result?.recycle()
            throw RuntimeException("LUT 预设处理内存不足，请尝试降低图像分辨率", e)
        }
    }

    /**
     * 应用带皮肤保护的 LUT 预设
     * 自动检测人脸区域，在皮肤区域使用弱滤镜以保护肤色
     */
    suspend fun applyPresetWithSkinProtection(
        source: Bitmap,
        preset: LutPreset,
        intensity: Float = 1f
    ): Bitmap {
        return skinProtectionFilter.applyFilterWithSkinProtection(source, preset, intensity)
    }

    /**
     * 应用色彩配方参数
     */
    suspend fun applyColorRecipe(
        source: Bitmap,
        params: ColorRecipeParams,
        onProgress: (Float) -> Unit = {}
    ): Bitmap = withContext(Dispatchers.Default) {
        if (params.isDefault) return@withContext source

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val outputPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = ((pixel shr 16) and 0xFF) / 255f
            var g = ((pixel shr 8) and 0xFF) / 255f
            var b = (pixel and 0xFF) / 255f

            // 曝光
            if (params.exposure != 0f) {
                val factor = 2f.pow(params.exposure)
                r = (r * factor).coerceIn(0f, 1f)
                g = (g * factor).coerceIn(0f, 1f)
                b = (b * factor).coerceIn(0f, 1f)
            }

            // 对比度
            if (params.contrast != 0f) {
                val c = 1f + params.contrast / 100f
                r = ((r - 0.5f) * c + 0.5f).coerceIn(0f, 1f)
                g = ((g - 0.5f) * c + 0.5f).coerceIn(0f, 1f)
                b = ((b - 0.5f) * c + 0.5f).coerceIn(0f, 1f)
            }

            // 高光
            if (params.highlights != 0f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val mask = (lum - 0.5f).coerceIn(0f, 0.5f) / 0.5f
                val adj = params.highlights / 100f * mask
                r = (r + adj).coerceIn(0f, 1f); g = (g + adj).coerceIn(0f, 1f); b = (b + adj).coerceIn(0f, 1f)
            }

            // 阴影
            if (params.shadows != 0f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val mask = (0.5f - lum).coerceIn(0f, 0.5f) / 0.5f
                val adj = params.shadows / 100f * mask
                r = (r + adj).coerceIn(0f, 1f); g = (g + adj).coerceIn(0f, 1f); b = (b + adj).coerceIn(0f, 1f)
            }

            // 色温
            if (params.temperature != 0f) {
                val shift = params.temperature / 100f
                r = (r + shift * 0.1f).coerceIn(0f, 1f)
                b = (b - shift * 0.1f).coerceIn(0f, 1f)
            }

            // 色调
            if (params.tint != 0f) {
                g = (g + params.tint / 100f * 0.05f).coerceIn(0f, 1f)
            }

            // 饱和度
            if (params.saturation != 0f) {
                val sat = 1f + params.saturation / 100f
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                r = (gray + (r - gray) * sat).coerceIn(0f, 1f)
                g = (gray + (g - gray) * sat).coerceIn(0f, 1f)
                b = (gray + (b - gray) * sat).coerceIn(0f, 1f)
            }

            // 自然饱和度
            if (params.vibrance != 0f) {
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                val maxChannel = maxOf(r, g, b)
                val satAmount = (maxChannel - gray).coerceIn(0f, 1f)
                val vibranceAmount = 1f + params.vibrance / 100f * (1f - satAmount)
                r = (gray + (r - gray) * vibranceAmount).coerceIn(0f, 1f)
                g = (gray + (g - gray) * vibranceAmount).coerceIn(0f, 1f)
                b = (gray + (b - gray) * vibranceAmount).coerceIn(0f, 1f)
            }

            // 褪色
            if (params.fade > 0f) {
                val lift = params.fade / 100f * 0.15f
                r = (r * (1f - lift) + lift).coerceIn(0f, 1f)
                g = (g * (1f - lift) + lift).coerceIn(0f, 1f)
                b = (b * (1f - lift) + lift).coerceIn(0f, 1f)
            }

            // 留银冲洗
            if (params.bleach > 0f) {
                val strength = params.bleach / 100f
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                r = (r * (1f - strength) + gray * strength * 1.2f).coerceIn(0f, 1f)
                g = (g * (1f - strength) + gray * strength * 0.9f).coerceIn(0f, 1f)
                b = (b * (1f - strength) + gray * strength * 0.8f).coerceIn(0f, 1f)
            }

            val outR = (r * 255f).roundToInt().coerceIn(0, 255)
            val outG = (g * 255f).roundToInt().coerceIn(0, 255)
            val outB = (b * 255f).roundToInt().coerceIn(0, 255)
            outputPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outputPixels, 0, width, 0, 0, width, height)

        if (params.vignette > 0f) applyVignette(result, params.vignette / 100f)
        if (params.grain > 0f) applyGrain(result, params.grain / 100f)
        if (params.sharpening > 0f) applySharpening(result, params.sharpening / 100f)
        if (params.bloom > 0f) bloomProcessor.applyBloom(result, params.bloom / 100f, 0.75f, 6)

        onProgress(1f)
        result
    }

    private fun applyVignette(bitmap: Bitmap, strength: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val cx = width / 2f
        val cy = height / 2f
        val maxDist = kotlin.math.sqrt(cx * cx + cy * cy)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - cx
                val dy = y - cy
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                val vignette = 1f - (dist / maxDist).pow(2f) * strength * 1.5f

                if (vignette < 0.99f) {
                    val idx = y * width + x
                    val pixel = pixels[idx]
                    val r = (((pixel shr 16) and 0xFF) * vignette).toInt().coerceIn(0, 255)
                    val g = (((pixel shr 8) and 0xFF) * vignette).toInt().coerceIn(0, 255)
                    val b = ((pixel and 0xFF) * vignette).toInt().coerceIn(0, 255)
                    pixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun applyGrain(bitmap: Bitmap, strength: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val random = java.util.Random()

        for (i in pixels.indices) {
            val noise = (random.nextGaussian() * strength * 50f).toFloat()
            val pixel = pixels[i]
            val r = (((pixel shr 16) and 0xFF) + noise).toInt().coerceIn(0, 255)
            val g = (((pixel shr 8) and 0xFF) + noise).toInt().coerceIn(0, 255)
            val b = ((pixel and 0xFF) + noise).toInt().coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun applySharpening(bitmap: Bitmap, strength: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        val amount = strength * 2f

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val center = pixels[idx]
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[y * width + (x - 1)]
                val right = pixels[y * width + (x + 1)]

                // Unsharp Mask：中心像素与四邻均值差，按 amount 增强
                val cr = ((center shr 16) and 0xFF).toFloat()
                val cg = ((center shr 8) and 0xFF).toFloat()
                val cb = (center and 0xFF).toFloat()

                val avgR = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) +
                        ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) / 4f
                val avgG = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) +
                        ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) / 4f
                val avgB = ((top and 0xFF) + (bottom and 0xFF) +
                        (left and 0xFF) + (right and 0xFF)) / 4f

                val outR = (cr + (cr - avgR) * amount).roundToInt().coerceIn(0, 255)
                val outG = (cg + (cg - avgG) * amount).roundToInt().coerceIn(0, 255)
                val outB = (cb + (cb - avgB) * amount).roundToInt().coerceIn(0, 255)

                output[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        // 复制边缘
        for (x in 0 until width) {
            output[x] = pixels[x]
            output[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            output[y * width] = pixels[y * width]
            output[y * width + width - 1] = pixels[y * width + width - 1]
        }

        bitmap.setPixels(output, 0, width, 0, 0, width, height)
    }
}
