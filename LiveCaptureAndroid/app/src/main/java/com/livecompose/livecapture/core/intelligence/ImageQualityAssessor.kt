package com.livecompose.livecapture.core.intelligence

import android.graphics.Bitmap
import kotlin.math.sqrt

/**
 * 图像质量评估系统
 *
 * 基于像素级分析对图像进行全面的质量评估，包括：
 * - 锐度评估（Sobel 3x3 边缘检测）
 * - 噪声评估（局部方差分析）
 * - 曝光评估（直方图分析）
 * - 色彩和谐度评估
 * - 分辨率评估
 * - 主导色彩提取
 * - 色温估计
 */
class ImageQualityAssessor {

    companion object {
        private const val QUANTIZATION_LEVELS = 32
        private const val MAX_DOMINANT_COLORS = 5
        private const val COLOR_MERGE_THRESHOLD = 60f
        private const val OVEREXPOSED_THRESHOLD = 250
        private const val UNDEREXPOSED_THRESHOLD = 5
        private const val HISTOGRAM_BINS = 256
    }

    // ─── 公共 API ───────────────────────────────────────────────

    /**
     * 对 Bitmap 进行综合质量评估
     */
    fun assessQuality(bitmap: Bitmap): QualityAssessment {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return assessQuality(pixels, width, height)
    }

    /**
     * 对原始像素数组进行综合质量评估
     */
    fun assessQuality(pixels: IntArray, width: Int, height: Int): QualityAssessment {
        val sharpness = assessSharpness(pixels, width, height)
        val noise = assessNoise(pixels, width, height)
        val exposure = assessExposure(pixels, width, height)
        val resolution = assessResolution(width, height)
        val colorAnalysis = analyzeColors(pixels, width, height)
        val colorHarmony = assessColorHarmony(colorAnalysis.dominantColors)
        val imageInfo = ImageInfo(
            width = width,
            height = height,
            aspectRatio = width.toFloat() / height,
            orientation = if (width >= height) "landscape" else "portrait",
            format = "ARGB_8888",
            totalPixels = width.toLong() * height,
            resolutionLevel = getResolutionLevel(width, height)
        )

        // 加权综合评分：锐度 30%，噪声 20%，曝光 25%，色彩和谐 15%，分辨率 10%
        val score = (sharpness * 0.30f +
                noise * 0.20f +
                exposure * 0.25f +
                colorHarmony * 0.15f +
                resolution * 0.10f)
            .coerceIn(0f, 100f)

        val grade = getQualityGrade(score)

        return QualityAssessment(
            overallScore = score,
            sharpnessScore = sharpness,
            noiseLevel = noise,
            exposureScore = exposure,
            colorHarmonyScore = colorHarmony,
            resolutionScore = resolution,
            qualityGrade = grade,
            timestamp = System.currentTimeMillis(),
            imageInfo = imageInfo
        )
    }

    /**
     * 对 Bitmap 进行完整色彩分析
     */
    fun analyzeColors(bitmap: Bitmap): ColorAnalysis {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return analyzeColors(pixels, width, height)
    }

    /**
     * 对原始像素数组进行完整色彩分析
     */
    fun analyzeColors(pixels: IntArray, width: Int, height: Int): ColorAnalysis {
        val pixelCount = pixels.size.toFloat()

        // 计算 RGB 平均值
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var sumBrightness = 0f
        var sumBrightnessSq = 0f

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            sumR += r
            sumG += g
            sumB += b
            val brightness = (r + g + b) / 3f
            sumBrightness += brightness
            sumBrightnessSq += brightness * brightness
        }

        val avgR = sumR / pixelCount
        val avgG = sumG / pixelCount
        val avgB = sumB / pixelCount
        val avgRInt = avgR.toInt().coerceIn(0, 255)
        val avgGInt = avgG.toInt().coerceIn(0, 255)
        val avgBInt = avgB.toInt().coerceIn(0, 255)
        val avgHex = String.format("#%02X%02X%02X", avgRInt, avgGInt, avgBInt)
        val averageColor = AverageColor(r = avgRInt, g = avgGInt, b = avgBInt, hex = avgHex)

        val brightness = sumBrightness / pixelCount
        val brightnessVariance = (sumBrightnessSq / pixelCount) - (brightness * brightness)
        val contrast = sqrt(brightnessVariance.coerceAtLeast(0f)) / 128f * 100f

        // 饱和度计算
        val maxRGB = maxOf(avgR, avgG, avgB)
        val minRGB = minOf(avgR, avgG, avgB)
        val saturation = if (maxRGB > 0f) {
            (maxRGB - minRGB) / maxRGB * 100f
        } else 0f

        // 提取主导色彩
        val dominantColors = extractDominantColors(pixels, width, height)

        // 色温估计
        val colorTemperature = estimateColorTemperature(avgR, avgG, avgB)

        // 色彩和谐度
        val colorHarmony = assessColorHarmony(dominantColors)

        // 色彩情绪
        val colorMood = determineColorMood(avgR, avgG, avgB, saturation)

        return ColorAnalysis(
            averageColor = averageColor,
            dominantColors = dominantColors,
            colorTemperature = colorTemperature,
            colorHarmonyScore = colorHarmony,
            colorMood = colorMood,
            saturationMean = saturation,
            brightnessMean = brightness / 255f * 100f,
            contrastRatio = contrast.coerceIn(0f, 100f)
        )
    }

    // ─── 锐度评估 ───────────────────────────────────────────────

    /**
     * 使用 Sobel 3x3 边缘检测评估锐度
     *
     * 1. 将像素转换为灰度图
     * 2. 应用 Sobel Gx 和 Gy 3x3 卷积核
     * 3. 计算梯度幅值: sqrt(Gx² + Gy²)
     * 4. 计算边缘密度（平均梯度幅值）
     * 5. 映射到 0-100 分
     */
    fun assessSharpness(pixels: IntArray, width: Int, height: Int): Float {
        if (width < 3 || height < 3) return 0f

        // 转换为灰度图
        val gray = IntArray(pixels.size) { i ->
            val p = pixels[i]
            rgbToGrayscale(
                (p shr 16) and 0xFF,
                (p shr 8) and 0xFF,
                p and 0xFF
            )
        }

        var totalGradient = 0f
        var edgeCount = 0

        // Sobel 3x3 卷积核
        // Gx = [[-1, 0, 1], [-2, 0, 2], [-1, 0, 1]]
        // Gy = [[-1, -2, -1], [0, 0, 0], [1, 2, 1]]

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x

                // 读取 3x3 邻域
                val tl = gray[idx - width - 1]
                val tc = gray[idx - width]
                val tr = gray[idx - width + 1]
                val ml = gray[idx - 1]
                val mr = gray[idx + 1]
                val bl = gray[idx + width - 1]
                val bc = gray[idx + width]
                val br = gray[idx + width + 1]

                // Sobel Gx
                val gx = (-1 * tl + 0 * tc + 1 * tr +
                        -2 * ml + 0 * 0 + 2 * mr +
                        -1 * bl + 0 * bc + 1 * br).toFloat()

                // Sobel Gy
                val gy = (-1 * tl + -2 * tc + -1 * tr +
                        0 * ml + 0 * 0 + 0 * mr +
                        1 * bl + 2 * bc + 1 * br).toFloat()

                val gradient = sqrt(gx * gx + gy * gy)
                totalGradient += gradient
                edgeCount++
            }
        }

        if (edgeCount == 0) return 0f

        // 边缘密度（平均梯度幅值，最大理论值为 4*255 ≈ 1020）
        val avgGradient = totalGradient / edgeCount

        // 映射到 0-100：经验值，优秀锐度约在 30-80 梯度范围
        val sharpnessScore = (avgGradient / 4f / 255f * 100f).coerceIn(0f, 100f)

        // 对低梯度区域进行非线性拉伸，使分数更合理
        return (sharpnessScore * 2.5f).coerceIn(0f, 100f)
    }

    // ─── 噪声评估 ───────────────────────────────────────────────

    /**
     * 使用局部方差分析评估噪声水平
     *
     * 1. 在 3x3 块中计算局部方差
     * 2. 对所有块求平均方差
     * 3. 归一化到 0-100（方差越大 = 噪声越多 = 分数越低）
     */
    fun assessNoise(pixels: IntArray, width: Int, height: Int): Float {
        if (width < 3 || height < 3) return 100f

        val gray = IntArray(pixels.size) { i ->
            val p = pixels[i]
            rgbToGrayscale(
                (p shr 16) and 0xFF,
                (p shr 8) and 0xFF,
                p and 0xFF
            )
        }

        var totalVariance = 0f
        var blockCount = 0

        // 使用 step=3 的滑动窗口分析 3x3 块
        for (y in 0 until height - 2 step 3) {
            for (x in 0 until width - 2 step 3) {
                val idx = y * width + x

                val v0 = gray[idx]
                val v1 = gray[idx + 1]
                val v2 = gray[idx + 2]
                val v3 = gray[idx + width]
                val v4 = gray[idx + width + 1]
                val v5 = gray[idx + width + 2]
                val v6 = gray[idx + width * 2]
                val v7 = gray[idx + width * 2 + 1]
                val v8 = gray[idx + width * 2 + 2]

                val mean = (v0 + v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8) / 9f

                val variance = (
                        (v0 - mean) * (v0 - mean) +
                                (v1 - mean) * (v1 - mean) +
                                (v2 - mean) * (v2 - mean) +
                                (v3 - mean) * (v3 - mean) +
                                (v4 - mean) * (v4 - mean) +
                                (v5 - mean) * (v5 - mean) +
                                (v6 - mean) * (v6 - mean) +
                                (v7 - mean) * (v7 - mean) +
                                (v8 - mean) * (v8 - mean)
                        ) / 9f

                totalVariance += variance
                blockCount++
            }
        }

        if (blockCount == 0) return 100f

        val avgVariance = totalVariance / blockCount

        // 归一化：方差 0 → 100 分，方差 5000 → 0 分
        // 噪声越小分数越高
        val noiseScore = (100f - (avgVariance / 50f).coerceIn(0f, 100f))
        return noiseScore
    }

    // ─── 曝光评估 ───────────────────────────────────────────────

    /**
     * 使用直方图分析评估曝光质量
     *
     * 1. 构建灰度直方图（256 bins）
     * 2. 计算过曝（>250）和欠曝（<5）像素比例
     * 3. 基于直方图分布评分
     */
    fun assessExposure(pixels: IntArray, width: Int, height: Int): Float {
        val histogram = IntArray(HISTOGRAM_BINS)
        val totalPixels = pixels.size.toFloat()

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val gray = rgbToGrayscale(r, g, b)
            histogram[gray.coerceIn(0, 255)]++
        }

        // 过曝像素比例
        var overexposedCount = 0
        for (i in OVEREXPOSED_THRESHOLD until HISTOGRAM_BINS) {
            overexposedCount += histogram[i]
        }
        val overexposedRatio = overexposedCount / totalPixels

        // 欠曝像素比例
        var underexposedCount = 0
        for (i in 0..UNDEREXPOSED_THRESHOLD) {
            underexposedCount += histogram[i]
        }
        val underexposedRatio = underexposedCount / totalPixels

        // 直方图熵：衡量分布均匀程度
        var entropy = 0f
        for (count in histogram) {
            if (count > 0) {
                val prob = count / totalPixels
                entropy -= prob * kotlin.math.ln(prob + 1e-10f).toFloat()
            }
        }
        // 归一化熵（最大熵为 ln(256) ≈ 5.545）
        val normalizedEntropy = entropy / 5.545f

        // 综合评分
        // 过曝/欠曝惩罚
        val overexposedPenalty = overexposedRatio * 100f
        val underexposedPenalty = underexposedRatio * 100f

        // 基础分由熵决定
        var exposureScore = normalizedEntropy * 100f

        // 惩罚过曝和欠曝
        exposureScore -= overexposedPenalty * 1.5f
        exposureScore -= underexposedPenalty * 1.5f

        return exposureScore.coerceIn(0f, 100f)
    }

    // ─── 色彩和谐度评估 ─────────────────────────────────────────

    /**
     * 基于主导色彩之间的距离评估色彩和谐度
     *
     * 和谐的色彩往往具有适中的距离：
     * - 距离过小 → 色彩过于单一
     * - 距离过大 → 色彩冲突
     * - 适中距离 → 和谐
     */
    fun assessColorHarmony(dominantColors: List<DominantColor>): Float {
        if (dominantColors.isEmpty()) return 0f
        if (dominantColors.size == 1) return 50f

        // 计算所有主导色对之间的平均欧几里得距离
        var totalDistance = 0f
        var pairCount = 0

        for (i in dominantColors.indices) {
            for (j in i + 1 until dominantColors.size) {
                val a = dominantColors[i]
                val b = dominantColors[j]
                val dR = (a.r - b.r).toFloat()
                val dG = (a.g - b.g).toFloat()
                val dB = (a.b - b.b).toFloat()
                val distance = sqrt(dR * dR + dG * dG + dB * dB)
                totalDistance += distance
                pairCount++
            }
        }

        if (pairCount == 0) return 50f

        val avgDistance = totalDistance / pairCount

        // 色彩距离的理想范围在 50-200 之间
        // 过小（<30）→ 单调，过大（>300）→ 冲突
        val harmonyScore = when {
            avgDistance < 30f -> avgDistance / 30f * 50f
            avgDistance <= 200f -> 50f + (avgDistance - 30f) / 170f * 50f
            avgDistance <= 350f -> 100f - (avgDistance - 200f) / 150f * 50f
            else -> 50f
        }

        return harmonyScore.coerceIn(0f, 100f)
    }

    // ─── 分辨率评估 ─────────────────────────────────────────────

    /**
     * 基于总像素数评估分辨率
     *
     * 参考标准：
     * - 12MP (4000×3000) → 100 分
     * - 8MP (3264×2448) → 85 分
     * - 4MP (2000×2000) → 60 分
     * - 2MP (1920×1080) → 40 分
     * - 1MP (1280×720) → 20 分
     */
    fun assessResolution(width: Int, height: Int): Float {
        val totalPixels = width.toLong() * height

        val score = when {
            totalPixels >= 12_000_000L -> 100f
            totalPixels >= 8_000_000L -> 85f + (totalPixels - 8_000_000L).toFloat() / 4_000_000f * 15f
            totalPixels >= 4_000_000L -> 60f + (totalPixels - 4_000_000L).toFloat() / 4_000_000f * 25f
            totalPixels >= 2_000_000L -> 40f + (totalPixels - 2_000_000L).toFloat() / 2_000_000f * 20f
            totalPixels >= 1_000_000L -> 20f + (totalPixels - 1_000_000L).toFloat() / 1_000_000f * 20f
            else -> (totalPixels.toFloat() / 1_000_000f * 20f).coerceIn(0f, 20f)
        }

        return score.coerceIn(0f, 100f)
    }

    // ─── 主导色彩提取 ───────────────────────────────────────────

    /**
     * 使用色彩量化提取主导色彩
     *
     * 1. 将 RGB 量化到 32 级/通道
     * 2. 统计每种量化颜色的频率
     * 3. 取频率最高的前 N 个颜色
     * 4. 合并相似颜色
     */
    fun extractDominantColors(
        pixels: IntArray,
        width: Int,
        height: Int
    ): List<DominantColor> {
        val totalPixels = pixels.size.toFloat()
        val quantStep = 256 / QUANTIZATION_LEVELS // 8

        // 量化并统计频率
        val colorFrequency = mutableMapOf<Int, Int>()

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // 量化到 32 级
            val qr = r / quantStep
            val qg = g / quantStep
            val qb = b / quantStep

            // 编码为单个 Int
            val quantizedKey = (qr shl 10) or (qg shl 5) or qb
            colorFrequency[quantizedKey] = colorFrequency.getOrDefault(quantizedKey, 0) + 1
        }

        // 按频率降序排序
        val sortedColors = colorFrequency.entries
            .sortedByDescending { it.value }
            .take(MAX_DOMINANT_COLORS * 3) // 取多一些用于合并

        // 反量化为实际 RGB 值
        val rawColors = sortedColors.map { entry ->
            val key = entry.key
            val qr = (key shr 10) and 0x1F
            val qg = (key shr 5) and 0x1F
            val qb = key and 0x1F

            // 取量化区间的中间值
            val r = (qr * quantStep + quantStep / 2).coerceIn(0, 255)
            val g = (qg * quantStep + quantStep / 2).coerceIn(0, 255)
            val b = (qb * quantStep + quantStep / 2).coerceIn(0, 255)

            Triple(r, g, b) to entry.value
        }

        // 合并相似颜色（欧几里得距离 < 阈值）
        val merged = mutableListOf<Pair<Triple<Int, Int, Int>, Int>>()
        val used = BooleanArray(rawColors.size)

        for (i in rawColors.indices) {
            if (used[i]) continue
            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var totalCount = 0

            for (j in i until rawColors.size) {
                if (used[j]) continue
                val (color1, _) = rawColors[i]
                val (color2, _) = rawColors[j]
                val dR = (color1.first - color2.first).toFloat()
                val dG = (color1.second - color2.second).toFloat()
                val dB = (color1.third - color2.third).toFloat()
                val distance = sqrt(dR * dR + dG * dG + dB * dB)

                if (distance <= COLOR_MERGE_THRESHOLD) {
                    used[j] = true
                    totalR += color2.first.toLong() * rawColors[j].second
                    totalG += color2.second.toLong() * rawColors[j].second
                    totalB += color2.third.toLong() * rawColors[j].second
                    totalCount += rawColors[j].second
                }
            }

            if (totalCount > 0) {
                val avgR = (totalR / totalCount).toInt().coerceIn(0, 255)
                val avgG = (totalG / totalCount).toInt().coerceIn(0, 255)
                val avgB = (totalB / totalCount).toInt().coerceIn(0, 255)
                merged.add(Triple(avgR, avgG, avgB) to totalCount)
            }
        }

        // 按频率排序并取前 N 个
        return merged
            .sortedByDescending { it.second }
            .take(MAX_DOMINANT_COLORS)
            .map { (color, count) ->
                val (r, g, b) = color
                val hex = String.format("#%02X%02X%02X", r, g, b)
                DominantColor(
                    r = r,
                    g = g,
                    b = b,
                    hex = hex,
                    percentage = (count / totalPixels * 100f)
                )
            }
    }

    // ─── 色温估计 ───────────────────────────────────────────────

    /**
     * 从 RGB 平均值估计色温
     *
     * 基于红/蓝比例判断冷暖色调，近似估计开尔文色温值
     */
    fun estimateColorTemperature(
        avgR: Float,
        avgG: Float,
        avgB: Float
    ): ColorTemperatureInfo {
        if (avgR == 0f && avgG == 0f && avgB == 0f) {
            return ColorTemperatureInfo(
                kelvin = 5500f,
                type = "中性",
                description = "纯黑图像"
            )
        }

        // 红/蓝比例用于判断冷暖
        val rbRatio = if (avgB > 0f) avgR / avgB else 1f

        // 估计开尔文色温（经验公式）
        val kelvin = when {
            rbRatio > 1.5f -> {
                // 暖色调：红偏多
                2000f + (rbRatio - 1.5f) / 2f * 2000f
            }
            rbRatio > 1.2f -> {
                3500f + (rbRatio - 1.2f) / 0.3f * 1500f
            }
            rbRatio > 0.9f -> {
                // 中性
                5000f + (rbRatio - 0.9f) / 0.3f * 1000f
            }
            rbRatio > 0.7f -> {
                // 冷色调
                6500f + (0.9f - rbRatio) / 0.2f * 2500f
            }
            else -> {
                // 非常冷
                9000f + (0.7f - rbRatio) / 0.3f * 3000f
            }
        }.coerceIn(1000f, 12000f)

        val (type, description) = when {
            kelvin < 3000f -> "暖色" to "暖色调，类似烛光/白炽灯"
            kelvin < 4000f -> "偏暖" to "略偏暖，类似清晨/黄昏光线"
            kelvin < 5500f -> "中性偏暖" to "中性偏暖，类似正午日光"
            kelvin < 6500f -> "中性" to "中性白平衡"
            kelvin < 8000f -> "偏冷" to "略偏冷，类似阴天"
            else -> "冷色" to "冷色调，类似阴影/蓝天光"
        }

        return ColorTemperatureInfo(
            kelvin = kelvin,
            type = type,
            description = description
        )
    }

    // ─── 色彩情绪判断 ───────────────────────────────────────────

    /**
     * 基于平均亮度和饱和度判断色彩情绪
     */
    fun determineColorMood(
        avgR: Float,
        avgG: Float,
        avgB: Float,
        saturation: Float
    ): String {
        // 亮度 (0-255)
        val brightness = (avgR + avgG + avgB) / 3f

        // 判断主色调倾向
        val isWarm = avgR > avgB * 1.1f
        val isCool = avgB > avgR * 1.1f
        val isGreen = avgG > avgR * 1.05f && avgG > avgB * 1.05f

        return when {
            brightness < 40f -> when {
                saturation < 20f -> "暗沉压抑"
                isWarm -> "温暖暗调"
                isCool -> "冷峻暗调"
                else -> "深沉暗调"
            }
            brightness < 100f -> when {
                saturation < 20f -> "柔和中性"
                isWarm -> "温暖柔和"
                isCool -> "冷静柔和"
                isGreen -> "自然清新"
                else -> "温和中性"
            }
            brightness < 180f -> when {
                saturation < 20f -> "明亮淡雅"
                isWarm -> "温暖明亮"
                isCool -> "清新明亮"
                isGreen -> "生机盎然"
                else -> "明亮中性"
            }
            else -> when {
                saturation < 20f -> "高调淡雅"
                isWarm -> "热情洋溢"
                isCool -> "清透高亮"
                else -> "明亮鲜艳"
            }
        }
    }

    // ─── 质量等级判定 ───────────────────────────────────────────

    /**
     * 根据分数获取质量等级
     */
    fun getQualityGrade(score: Float): QualityGrade {
        return when {
            score >= 85f -> QualityGrade.EXCELLENT
            score >= 70f -> QualityGrade.GOOD
            score >= 50f -> QualityGrade.FAIR
            else -> QualityGrade.POOR
        }
    }

    // ─── 图像信息 ────────────────────────────────────────────────

    /**
     * 获取图像基本信息
     */
    fun getImageInfo(bitmap: Bitmap): ImageInfo {
        return ImageInfo(
            width = bitmap.width,
            height = bitmap.height,
            aspectRatio = bitmap.width.toFloat() / bitmap.height,
            orientation = if (bitmap.width >= bitmap.height) "landscape" else "portrait",
            format = bitmap.config?.name ?: "UNKNOWN",
            totalPixels = bitmap.width.toLong() * bitmap.height,
            resolutionLevel = getResolutionLevel(bitmap.width, bitmap.height)
        )
    }

    // ─── 工具方法 ────────────────────────────────────────────────

    /**
     * 使用感知亮度公式将 RGB 转为灰度值
     * 公式: Y = 0.299R + 0.587G + 0.114B
     */
    fun rgbToGrayscale(r: Int, g: Int, b: Int): Int {
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
    }

    /**
     * 根据分辨率获取分辨率等级描述
     */
    private fun getResolutionLevel(width: Int, height: Int): String {
        val mp = (width.toLong() * height) / 1_000_000f
        return when {
            mp >= 12f -> "4K+"
            mp >= 8f -> "4K"
            mp >= 4f -> "2K"
            mp >= 2f -> "1080p"
            mp >= 1f -> "720p"
            else -> "SD"
        }
    }
}