package com.livecompose.livecapture.core.lut

import android.graphics.Bitmap
import com.livecompose.livecapture.core.editing.VignetteProcessor
import com.livecompose.livecapture.core.processing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

/**
 * 大师预设引擎
 *
 * 将 OMaster 预设参数完整应用到图像的管道处理器。
 * 按照预设参数的标准顺序依次执行：
 *
 * 1. 基础调整: brightness → contrast(global/highlight/shadow) → clarity
 * 2. 色彩调整: warm_cool → cyan_magenta → saturation → hue
 * 3. 影调: tone_curve
 * 4. 风格: filter(LUT) → soft_light → dehaze
 * 5. 细节: sharpness → grain → vignette
 *
 * 每个步骤都是可选的（参数为 0/默认值时跳过）。
 */
class MasterPresetEngine {

    private val sharpnessProcessor = SharpnessProcessor()
    private val clarityProcessor = ClarityProcessor()
    private val contrastProcessor = ContrastProcessor()
    private val colorBalanceProcessor = ColorBalanceProcessor()
    private val grainProcessor = GrainProcessor()
    private val softLightProcessor = SoftLightProcessor()
    private val dehazeProcessor = DehazeProcessor()
    private val vignetteProcessor = VignetteProcessor()
    private val splitToneProcessor = SplitToneProcessor()

    /**
     * 应用完整的预设管道
     *
     * @param bitmap 原始图像
     * @param params 解析后的预设参数
     * @param intensity 预设整体强度 0.0~1.0
     * @return 应用预设后的图像
     */
    suspend fun applyPreset(
        bitmap: Bitmap,
        params: ParsedPresetParams,
        intensity: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        var result = bitmap

        // 阶段 1: 基础调整
        result = applyBasicAdjustments(result, params, intensity)

        // 阶段 2: 色彩调整
        result = applyColorAdjustments(result, params, intensity)

        // 阶段 3: 影调
        result = applyToneCurve(result, params, intensity)

        // 阶段 4: 风格
        result = applyStyleEffects(result, params, intensity)

        // 阶段 5: 细节
        result = applyDetailEffects(result, params, intensity)

        result
    }

    private suspend fun applyBasicAdjustments(
        bitmap: Bitmap, params: ParsedPresetParams, intensity: Float
    ): Bitmap {
        var result = bitmap

        // 亮度
        if (params.brightness != 0f) {
            result = applyBrightness(result, params.brightness * intensity)
        }

        // 对比度
        if (params.contrast != 0f || params.contrastHighlight != 0f || params.contrastShadow != 0f) {
            result = contrastProcessor.apply(
                result,
                global = params.contrast * intensity,
                highlight = params.contrastHighlight * intensity,
                shadow = params.contrastShadow * intensity
            )
        }

        // 清晰度
        if (params.clarity != 0f) {
            result = clarityProcessor.apply(result, params.clarity * intensity)
        }

        return result
    }

    private suspend fun applyColorAdjustments(
        bitmap: Bitmap, params: ParsedPresetParams, intensity: Float
    ): Bitmap {
        var result = bitmap

        // 色温 + 色调
        if (params.warmCool != 0f || params.cyanMagenta != 0f) {
            result = colorBalanceProcessor.apply(
                result,
                warmCool = params.warmCool * intensity,
                cyanMagenta = params.cyanMagenta * intensity
            )
        }

        // 饱和度
        if (params.saturation != 0f) {
            result = applySaturation(result, params.saturation * intensity)
        }

        // 色相
        if (params.hue != 0f) {
            result = applyHue(result, params.hue * intensity)
        }

        return result
    }

    private suspend fun applyToneCurve(
        bitmap: Bitmap, params: ParsedPresetParams, intensity: Float
    ): Bitmap {
        if (params.toneCurve == 0f) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val amount = (params.toneCurve / 100f * intensity).coerceIn(-1f, 1f)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            // S 曲线影调
            val adjustedR = applyScurve(r, amount)
            val adjustedG = applyScurve(g, amount)
            val adjustedB = applyScurve(b, amount)

            outPixels[i] = (0xFF shl 24) or
                    ((adjustedR * 255f).toInt() shl 16) or
                    ((adjustedG * 255f).toInt() shl 8) or
                    (adjustedB * 255f).toInt()
        }
        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    private suspend fun applyStyleEffects(
        bitmap: Bitmap, params: ParsedPresetParams, intensity: Float
    ): Bitmap {
        var result = bitmap

        // 滤镜 (LUT)
        if (params.filter.isNotEmpty() && params.filter != "无") {
            // OMaster filter 字段为元数据标识，实际滤镜效果已通过
            // 上述亮度/对比度/饱和度/色温/影调等参数完整应用到图像
            // 如需使用 LUT cube 文件，可在此处调用 LutProcessor.applyPreset()
        }

        // 柔光
        if (params.softLight != "无") {
            result = softLightProcessor.apply(
                result,
                mode = when (params.softLight) {
                    "柔美" -> SoftLightProcessor.SoftLightMode.SOFT
                    "梦幻" -> SoftLightProcessor.SoftLightMode.DREAMY
                    else -> SoftLightProcessor.SoftLightMode.NONE
                },
                intensity = intensity
            )
        }

        // 去雾
        if (params.dehaze != 0f) {
            result = dehazeProcessor.apply(result, params.dehaze * intensity)
        }

        return result
    }

    private suspend fun applyDetailEffects(
        bitmap: Bitmap, params: ParsedPresetParams, intensity: Float
    ): Bitmap {
        var result = bitmap

        // 锐度
        if (params.sharpness != 0f) {
            result = sharpnessProcessor.apply(result, params.sharpness * intensity)
        }

        // 颗粒
        if (params.grain != 0f) {
            result = grainProcessor.apply(
                result,
                intensity = params.grain * intensity,
                size = params.grainSize * intensity
            )
        }

        // 暗角
        if (params.vignette) {
            result = vignetteProcessor.apply(
                result,
                intensity = params.vignetteIntensity * intensity
            )
        }

        return result
    }

    // 辅助方法
    private fun applyScurve(value: Float, amount: Float): Float {
        if (amount == 0f) return value
        return if (amount > 0f) {
            value.pow(1f / (1f + amount * 0.5f))
        } else {
            value * (1f - kotlin.math.abs(amount) * 0.5f) + 0.5f * kotlin.math.abs(amount) * 0.5f
        }
    }

    private fun applyBrightness(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount == 0f) return bitmap
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF).toFloat() + amount * 2.55f
            val g = ((p shr 8) and 0xFF).toFloat() + amount * 2.55f
            val b = (p and 0xFF).toFloat() + amount * 2.55f
            outPixels[i] = (0xFF shl 24) or
                    (r.toInt().coerceIn(0, 255) shl 16) or
                    (g.toInt().coerceIn(0, 255) shl 8) or
                    b.toInt().coerceIn(0, 255)
        }
        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applySaturation(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount == 0f) return bitmap
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)
        val factor = 1f + amount / 100f
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val gray = (r * 0.299f + g * 0.587f + b * 0.114f).toInt()
            val outR = (gray + (r - gray) * factor).toInt().coerceIn(0, 255)
            val outG = (gray + (g - gray) * factor).toInt().coerceIn(0, 255)
            val outB = (gray + (b - gray) * factor).toInt().coerceIn(0, 255)
            outPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }
        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyHue(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount == 0f) return bitmap
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)
        val hueShift = amount / 100f * 360f
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            hsv[0] = (hsv[0] * 360f + hueShift + 360f) % 360f
            val adjusted = android.graphics.Color.HSVToColor(hsv)
            outPixels[i] = (0xFF shl 24) or (adjusted and 0x00FFFFFF)
        }
        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun Float.pow(exp: Float): Float = this.toDouble().pow(exp.toDouble()).toFloat()
}