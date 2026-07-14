package com.livecompose.livecapture.core.detection

import android.graphics.Bitmap
import android.util.Log
import com.livecompose.livecapture.core.detection.CompositionResult.LightingQuality
import com.livecompose.livecapture.core.detection.CompositionResult.SceneType
import com.livecompose.livecapture.core.detection.CompositionResult.ShootingParams
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 智能场景识别与拍摄指导引擎
 * 参考: ai-photography-assistant (gitee.com/zheng-bojie)
 *
 * 基于 RGB 像素分析实现端侧零依赖场景识别:
 * - 人像检测 (肤色比例分析)
 * - 风景检测 (颜色丰富度 + 三分法)
 * - 夜景检测 (整体亮度分析)
 * - 美食检测 (暖色调 + 高饱和度)
 * - 产品检测 (白色背景 + 低复杂度)
 * - 城市场景 (边缘密度分析)
 *
 * 同时提供:
 * - 光照质量评估 (逆光/过暗/过曝/低对比度)
 * - 智能拍摄指导 (场景匹配的参数建议+技巧)
 */
@Singleton
class SceneAnalyzer @Inject constructor() {

    companion object {
        private const val TAG = "SceneAnalyzer"
        private const val ANALYSIS_SAMPLE_SIZE = 56 // 降采样到 56x56 做分析

        // 肤肤色 HSV 范围 (近似)
        private const val SKIN_H_MIN = 0f
        private const val SKIN_H_MAX = 50f
        private const val SKIN_S_MIN = 0.15f
        private const val SKIN_S_MAX = 0.75f
        private const val SKIN_V_MIN = 0.3f

        // 场景判定阈值
        private const val DARK_THRESHOLD = 0.2f       // 亮度低于 20% 判定为夜景
        private const val BRIGHT_THRESHOLD = 0.85f     // 亮度高于 85% 判定为过曝
        private const val BACKLIGHT_RATIO = 1.4f       // 边缘/中心亮度比 > 1.4 判定逆光
        private const val SKIN_RATIO_PORTRAIT = 0.08f  // 肤肤色占比 > 8% 判定人像
        private const val SATURATION_FOOD = 0.45f      // 高饱和度判定美食
        private const val WARM_RATIO_FOOD = 0.6f       // 暖色占比
        private const val WHITE_BG_PRODUCT = 0.7f      // 白色背景亮度
        private const val EDGE_DENSITY_CITY = 0.15f    // 边缘密度判定城市
    }

    /**
     * 分析 Bitmap 并返回场景识别结果
     * 纯 CPU 计算，无需额外模型
     */
    fun analyzeScene(bitmap: Bitmap): SceneAnalysisResult {
        val startTime = System.currentTimeMillis()

        // 降采样以提升性能
        val sampled = if (bitmap.width > ANALYSIS_SAMPLE_SIZE || bitmap.height > ANALYSIS_SAMPLE_SIZE) {
            Bitmap.createScaledBitmap(bitmap, ANALYSIS_SAMPLE_SIZE, ANALYSIS_SAMPLE_SIZE, true)
        } else {
            bitmap
        }

        try {
            val pixels = IntArray(sampled.width * sampled.height)
            sampled.getPixels(pixels, 0, sampled.width, 0, 0, sampled.width, sampled.height)

            // RGB 直方图统计
            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var skinPixelCount = 0
            var warmPixelCount = 0
            var highSatCount = 0
            var brightPixelCount = 0
            var darkPixelCount = 0

            val histogramR = IntArray(256)
            val histogramG = IntArray(256)
            val histogramB = IntArray(256)

            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                totalR += r
                totalG += g
                totalB += b
                histogramR[r]++
                histogramG[g]++
                histogramB[b]++

                // 亮度
                val brightness = (r * 0.299f + g * 0.587f + b * 0.114f) / 255f

                // 肤肤色检测 (简化 HSV)
                val maxC = max(max(r, g), b)
                val minC = min(min(r, g), b)
                val saturation = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f

                if (isSkinColor(r, g, b, saturation, brightness)) {
                    skinPixelCount++
                }

                // 暖色检测 (R > B)
                if (r > b * 1.2f && saturation > 0.2f) {
                    warmPixelCount++
                }

                // 高饱和度
                if (saturation > 0.5f) {
                    highSatCount++
                }

                // 亮度统计
                if (brightness > WHITE_BG_PRODUCT) brightPixelCount++
                if (brightness < DARK_THRESHOLD) darkPixelCount++
            }

            val pixelCount = pixels.size.toFloat()
            val avgBrightness = (totalR * 0.299f + totalG * 0.587f + totalB * 0.114f) / pixelCount / 255f
            val avgSaturation = highSatCount / pixelCount
            val skinRatio = skinPixelCount / pixelCount
            val warmRatio = warmPixelCount / pixelCount
            val brightRatio = brightPixelCount / pixelCount

            // 对比度 (直方图标准差)
            val contrast = calculateContrast(histogramR, histogramG, histogramB, pixelCount)

            // 逆光检测: 边缘亮度 vs 中心亮度
            val backlightScore = detectBacklight(pixels, sampled.width, sampled.height)

            // 边缘密度 (简化 Sobel)
            val edgeDensity = calculateEdgeDensity(pixels, sampled.width, sampled.height)

            // 场景分类
            val sceneType = classifyScene(
                avgBrightness, avgSaturation, skinRatio, warmRatio,
                brightRatio, contrast, edgeDensity, backlightScore
            )

            // 光照质量
            val lightingQuality = assessLightingQuality(
                avgBrightness, contrast, backlightScore
            )

            // 拍摄指导
            val shootingTip = generateShootingTip(sceneType, lightingQuality)

            val processingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Scene: ${sceneType.label}, Lighting: $lightingQuality, " +
                    "Brightness: ${"%.2f".format(avgBrightness)}, " +
                    "Skin: ${"%.2f".format(skinRatio)}, " +
                    "Time: ${processingTime}ms")

            return SceneAnalysisResult(
                sceneType = sceneType,
                lightingQuality = lightingQuality,
                brightness = avgBrightness,
                contrast = contrast,
                skinRatio = skinRatio,
                saturation = avgSaturation,
                backlightScore = backlightScore,
                shootingTip = shootingTip,
                processingTimeMs = processingTime
            )
        } finally {
            if (sampled !== bitmap) {
                sampled.recycle()
            }
        }
    }

    private fun isSkinColor(r: Int, g: Int, b: Int, saturation: Float, brightness: Float): Boolean {
        // 简化肤色检测: R > G > B, 饱和度适中, 亮度适中
        return r > 80 && g > 50 && b > 30 &&
                r > g && g > b &&
                saturation in 0.1f..0.65f &&
                brightness in 0.25f..0.85f &&
                (r - g) in 5..80 &&
                (r - b) in 15..130
    }

    private fun classifyScene(
        brightness: Float, saturation: Float, skinRatio: Float,
        warmRatio: Float, brightRatio: Float, contrast: Float,
        edgeDensity: Float, backlightScore: Float
    ): SceneType {
        // 夜景优先判断 (亮度极低)
        if (brightness < DARK_THRESHOLD) {
            return SceneType.NIGHT_SCENE
        }

        // 人像 (肤色占比高)
        if (skinRatio > SKIN_RATIO_PORTRAIT) {
            // 区分站姿/坐姿: 站姿通常 bbox 较窄 (竖长), 坐姿较宽
            // 简化: 如果饱和度较低, 更可能是站姿; 较高更可能是坐姿（室内暖光）
            return if (saturation < 0.35f) SceneType.PORTRAIT_STANDING
            else SceneType.PORTRAIT_SITTING
        }

        // 美食 (高饱和 + 暖色调)
        if (saturation > SATURATION_FOOD && warmRatio > WARM_RATIO_FOOD && skinRatio < 0.05f) {
            return SceneType.FOOD_STYLING
        }

        // 产品白底 (大面积白色 + 低饱和)
        if (brightRatio > 0.5f && saturation < 0.15f && skinRatio < 0.03f) {
            return SceneType.PRODUCT_WHITE
        }

        // 城市街拍 (高边缘密度 + 中等亮度)
        if (edgeDensity > EDGE_DENSITY_CITY && brightness in 0.3f..0.8f) {
            return SceneType.CITY_URBAN
        }

        // 风景 (低肤色 + 低边缘密度 + 日落暖光判断)
        if (skinRatio < 0.05f) {
            return if (warmRatio > 0.4f && brightness > 0.5f) {
                SceneType.LANDSCAPE_SUNSET
            } else {
                SceneType.LANDSCAPE_NATURE
            }
        }

        return SceneType.GENERAL
    }

    private fun assessLightingQuality(brightness: Float, contrast: Float, backlightScore: Float): LightingQuality {
        if (backlightScore > 0.6f) return LightingQuality.BACKLIT
        if (brightness < DARK_THRESHOLD) return LightingQuality.TOO_DARK
        if (brightness > BRIGHT_THRESHOLD) return LightingQuality.TOO_BRIGHT
        if (contrast < 30f) return LightingQuality.LOW_CONTRAST
        return LightingQuality.GOOD
    }

    private fun generateShootingTip(sceneType: SceneType, lightingQuality: LightingQuality): String {
        val baseTip = sceneType.shootingParams.tip
        val lightingTip = when (lightingQuality) {
            LightingQuality.BACKLIT -> "检测到逆光，建议调整角度或补光"
            LightingQuality.TOO_DARK -> "光线不足，建议开启闪光灯或寻找光源"
            LightingQuality.TOO_BRIGHT -> "光线过强，建议避开直射光"
            LightingQuality.LOW_CONTRAST -> "对比度偏低，建议调整构图增强层次"
            LightingQuality.GOOD -> ""
        }
        return listOf(baseTip, lightingTip).filter { it.isNotEmpty() }.joinToString("；")
    }

    private fun calculateContrast(
        histR: IntArray, histG: IntArray, histB: IntArray,
        pixelCount: Float
    ): Float {
        // RGB 各通道标准差的均值
        var sumR = 0f; var sumR2 = 0f
        var sumG = 0f; var sumG2 = 0f
        var sumB = 0f; var sumB2 = 0f

        for (i in 0..255) {
            val freq = histR[i] / pixelCount
            sumR += i * freq; sumR2 += i * i * freq
            val freqG = histG[i] / pixelCount
            sumG += i * freqG; sumG2 += i * i * freqG
            val freqB = histB[i] / pixelCount
            sumB += i * freqB; sumB2 += i * i * freqB
        }

        val stdR = sqrt(max(0f, sumR2 - sumR * sumR))
        val stdG = sqrt(max(0f, sumG2 - sumG * sumG))
        val stdB = sqrt(max(0f, sumB2 - sumB * sumB))
        return (stdR + stdG + stdB) / 3f
    }

    private fun detectBacklight(pixels: IntArray, width: Int, height: Int): Float {
        // 边缘区域 vs 中心区域亮度对比
        val edgeMargin = max(1, min(width, height) / 6)
        var edgeBrightness = 0f
        var edgeCount = 0
        var centerBrightness = 0f
        var centerCount = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val brightness = (r * 0.299f + g * 0.587f + b * 0.114f) / 255f

                if (x < edgeMargin || x >= width - edgeMargin ||
                    y < edgeMargin || y >= height - edgeMargin) {
                    edgeBrightness += brightness
                    edgeCount++
                } else {
                    centerBrightness += brightness
                    centerCount++
                }
            }
        }

        if (edgeCount == 0 || centerCount == 0) return 0f
        val avgEdge = edgeBrightness / edgeCount
        val avgCenter = centerBrightness / centerCount

        return if (avgEdge > avgCenter && avgCenter > 0.01f) {
            (avgEdge / avgCenter - 1f).coerceIn(0f, 1f)
        } else 0f
    }

    private fun calculateEdgeDensity(pixels: IntArray, width: Int, height: Int): Float {
        if (width < 3 || height < 3) return 0f
        var edgeCount = 0
        val totalCells = (width - 2) * (height - 2)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 2) {
                val idx = y * width + x
                val left = pixelBrightness(pixels[idx - 1])
                val right = pixelBrightness(pixels[idx + 1])
                val top = pixelBrightness(pixels[idx - width])
                val bottom = pixelBrightness(pixels[idx + width])

                val gx = abs(right - left)
                val gy = abs(bottom - top)
                if (gx + gy > 40) edgeCount++
            }
        }
        return edgeCount.toFloat() / totalCells
    }

    private fun pixelBrightness(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return r * 0.299f + g * 0.587f + b * 0.114f
    }
}

data class SceneAnalysisResult(
    val sceneType: SceneType,
    val lightingQuality: LightingQuality,
    val brightness: Float,
    val contrast: Float,
    val skinRatio: Float,
    val saturation: Float,
    val backlightScore: Float,
    val shootingTip: String,
    val processingTimeMs: Long
)
