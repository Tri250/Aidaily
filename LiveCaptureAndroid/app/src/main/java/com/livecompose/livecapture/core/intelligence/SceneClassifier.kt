package com.livecompose.livecapture.core.intelligence

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.livecompose.livecapture.core.logger.AppLogger

/**
 * 场景分类器
 *
 * 对应 iOS 端 SceneClassifier.swift，使用 ML Kit 进行场景分类、
 * 光环境分析和主体检测，替代 iOS Vision 框架。
 *
 * ## 主要功能
 * - [classifyScene] 场景分类，返回场景类型与置信度
 * - [analyzeLight] 基于像素的光环境分析（亮度/对比度/色温/逆光）
 * - [detectSubjects] 主体检测（人脸 + 通用目标）
 * - [refineClassification] 根据光环境微调场景分类
 */
class SceneClassifier {

    companion object {
        private const val TAG = "SceneClassifier"
        private const val MIN_CONFIDENCE = 0.6f
        private const val SAMPLE_STEP_TARGET = 16384 // 直方图采样目标像素数
    }

    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(MIN_CONFIDENCE)
            .build()
    )

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()
    )

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .setMultipleObjects(true)
            .build()
    )

    /**
     * 场景分类
     *
     * @param bitmap 输入图像
     * @return 场景类型与置信度的 Pair，失败返回 (UNKNOWN, 0)
     */
    suspend fun classifyScene(bitmap: Bitmap): Pair<SceneType, Float> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val labels = Tasks.await(imageLabeler.process(image))

            if (labels.isEmpty()) {
                val light = analyzeLight(bitmap)
                return ruleBasedClassification(light)
            }

            // 遍历所有标签，按置信度匹配场景
            var bestScene = SceneType.UNKNOWN
            var bestConfidence = 0f

            for (label in labels) {
                val scene = mapLabelToScene(label.text)
                if (scene != SceneType.UNKNOWN && label.confidence > bestConfidence) {
                    bestScene = scene
                    bestConfidence = label.confidence
                }
            }

            if (bestScene == SceneType.UNKNOWN) {
                val light = analyzeLight(bitmap)
                return ruleBasedClassification(light)
            }

            // 用光环境微调
            val (refinedScene, refinedConfidence) = refineClassification(bestScene, bestConfidence, analyzeLight(bitmap))
            refinedScene to refinedConfidence
        } catch (e: Exception) {
            AppLogger.e(TAG, "场景分类失败", e)
            val light = analyzeLight(bitmap)
            ruleBasedClassification(light)
        }
    }

    /**
     * 光环境分析
     *
     * 基于像素统计计算亮度、对比度、色温和逆光判断。
     *
     * @param bitmap 输入图像
     * @return 光环境分析结果
     */
    fun analyzeLight(bitmap: Bitmap): LightAnalysis {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return LightAnalysis.DEFAULT

        // 降采样以加速
        val sampleStep = Math.max(1, Math.sqrt((width * height / SAMPLE_STEP_TARGET).toDouble()).toInt())
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var rSum = 0L; var gSum = 0L; var bSum = 0L
        var lumSum = 0L
        var lumSqSum = 0L
        var sampleCount = 0

        // 中心区域与边缘区域分别统计（用于逆光检测）
        val centerX = width / 2
        val centerY = height / 2
        val centerRadius = Math.min(width, height) / 4
        var centerLumSum = 0L; var centerCount = 0
        var edgeLumSum = 0L; var edgeCount = 0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val c = pixels[y * width + x]
                val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
                val lum = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

                rSum += r; gSum += g; bSum += b
                lumSum += lum
                lumSqSum += lum.toLong() * lum
                sampleCount++

                val dx = x - centerX
                val dy = y - centerY
                if (dx * dx + dy * dy < centerRadius * centerRadius) {
                    centerLumSum += lum; centerCount++
                } else {
                    edgeLumSum += lum; edgeCount++
                }
                x += sampleStep
            }
            y += sampleStep
        }

        if (sampleCount == 0) return LightAnalysis.DEFAULT

        val avgR = rSum.toFloat() / sampleCount
        val avgG = gSum.toFloat() / sampleCount
        val avgB = bSum.toFloat() / sampleCount
        val avgLum = lumSum.toFloat() / sampleCount
        val variance = (lumSqSum.toFloat() / sampleCount) - avgLum * avgLum
        val contrast = Math.sqrt(Math.max(0.0, variance.toDouble())).toFloat() / 128f

        val brightness = avgLum / 255f
        val colorTemperature = estimateColorTemperature(avgR, avgG, avgB)
        val isBacklit = detectBacklit(
            if (centerCount > 0) centerLumSum.toFloat() / centerCount else avgLum,
            if (edgeCount > 0) edgeLumSum.toFloat() / edgeCount else avgLum
        )
        val lightType = determineLightType(brightness, colorTemperature, isBacklit, avgR, avgG, avgB)

        return LightAnalysis(
            colorTemperature = colorTemperature,
            brightness = brightness,
            contrast = contrast.coerceIn(0f, 1f),
            isBacklit = isBacklit,
            lightType = lightType
        )
    }

    /**
     * 主体检测
     *
     * @param bitmap 输入图像
     * @return 主体检测结果
     */
    suspend fun detectSubjects(bitmap: Bitmap): SubjectDetection {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = Tasks.await(faceDetector.process(image))
            val objects = Tasks.await(objectDetector.process(image))

            val faceCount = faces.size
            val hasMultiplePeople = faceCount > 1

            val faceRect = faces.firstOrNull()?.let { face ->
                val bounds = face.boundingBox
                FaceRect(
                    left = bounds.left.toFloat() / bitmap.width,
                    top = bounds.top.toFloat() / bitmap.height,
                    right = bounds.right.toFloat() / bitmap.width,
                    bottom = bounds.bottom.toFloat() / bitmap.height,
                    confidence = 1.0f
                )
            }

            val bodyRect = objects.firstOrNull()?.let { obj ->
                val bounds = obj.boundingBox
                BodyRect(
                    left = bounds.left.toFloat() / bitmap.width,
                    top = bounds.top.toFloat() / bitmap.height,
                    right = bounds.right.toFloat() / bitmap.width,
                    bottom = bounds.bottom.toFloat() / bitmap.height
                )
            }

            val subjectSizeRatio = (faceRect?.area ?: 0f).let { faceArea ->
                val bodyArea = bodyRect?.let { (it.right - it.left) * (it.bottom - it.top) } ?: 0f
                Math.max(faceArea, bodyArea)
            }

            val dominantPosition = faceRect?.let { computeSubjectPosition(it) }
                ?: bodyRect?.let { computeSubjectPosition(BodyRect(it.left, it.top, it.right, it.bottom)) }
                ?: SubjectPosition.CENTER

            SubjectDetection(
                faceCount = faceCount,
                faceRect = faceRect,
                bodyRect = bodyRect,
                faceConfidence = faces.firstOrNull()?.let { 1.0f } ?: 0f,
                hasMultiplePeople = hasMultiplePeople,
                dominantSubjectPosition = dominantPosition,
                subjectSizeRatio = subjectSizeRatio
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "主体检测失败", e)
            SubjectDetection()
        }
    }

    /**
     * 根据光环境微调场景分类
     *
     * @param scene 原始场景类型
     * @param confidence 原始置信度
     * @param light 光环境分析
     * @return 微调后的场景类型与置信度
     */
    fun refineClassification(
        scene: SceneType,
        confidence: Float,
        light: LightAnalysis
    ): Pair<SceneType, Float> {
        var refinedScene = scene
        var refinedConfidence = confidence

        // 低光环境 + 户外场景 → 夜景
        if (light.brightness < 0.2f && (scene == SceneType.URBAN || scene == SceneType.STREET ||
                scene == SceneType.LANDSCAPE || scene == SceneType.OUTDOOR)) {
            refinedScene = SceneType.NIGHT
            refinedConfidence = (confidence + 0.1f).coerceAtMost(1f)
        }

        // 日落色温 + 户外 → 日落场景
        else if (light.colorTemperature in 2500f..4000f && light.lightType == LightType.WARM &&
            (scene == SceneType.LANDSCAPE || scene == SceneType.OUTDOOR || scene == SceneType.NATURE)) {
            refinedScene = SceneType.SUNSET
            refinedConfidence = (confidence + 0.05f).coerceAtMost(1f)
        }

        // 逆光 → 逆光/剪影场景
        else if (light.isBacklit && (scene == SceneType.PORTRAIT || scene == SceneType.PORTRAIT_STANDING)) {
            refinedScene = SceneType.BACKLIT
            refinedConfidence = (confidence + 0.05f).coerceAtMost(1f)
        }

        return refinedScene to refinedConfidence
    }

    // MARK: - 规则化分类（兜底）

    /**
     * 基于光环境的规则化分类（ML Kit 无匹配标签时使用）
     */
    private fun ruleBasedClassification(light: LightAnalysis): Pair<SceneType, Float> {
        val scene = when {
            light.brightness < 0.2f -> SceneType.NIGHT
            light.brightness > 0.8f && light.isBacklit -> SceneType.BACKLIT
            light.colorTemperature in 2500f..4000f -> SceneType.SUNSET
            light.lightType == LightType.WARM -> SceneType.INDOOR
            else -> SceneType.OUTDOOR
        }
        return scene to 0.4f
    }

    // MARK: - 标签映射

    /**
     * 将 ML Kit 标签映射到 SceneType
     */
    private fun mapLabelToScene(label: String): SceneType {
        val lower = label.lowercase()
        return when {
            // 人物
            lower.contains("person") || lower.contains("man") || lower.contains("woman") ||
                lower.contains("people") || lower.contains("face") -> SceneType.PORTRAIT
            lower.contains("selfie") -> SceneType.SELFIE
            lower.contains("child") || lower.contains("kid") || lower.contains("baby") -> SceneType.CHILDREN
            lower.contains("wedding") || lower.contains("bride") || lower.contains("groom") -> SceneType.WEDDING
            lower.contains("fashion") || lower.contains("model") -> SceneType.FASHION
            lower.contains("beauty") || lower.contains("cosmetic") -> SceneType.BEAUTY

            // 食物
            lower.contains("food") || lower.contains("dish") || lower.contains("meal") ||
                lower.contains("fruit") || lower.contains("dessert") || lower.contains("drink") -> SceneType.FOOD

            // 风景
            lower.contains("landscape") || lower.contains("mountain") || lower.contains("valley") ||
                lower.contains("forest") || lower.contains("field") || lower.contains("countryside") -> SceneType.LANDSCAPE
            lower.contains("nature") -> SceneType.NATURE
            lower.contains("sky") || lower.contains("sunset") || lower.contains("sunrise") -> SceneType.SUNSET
            lower.contains("beach") || lower.contains("sea") || lower.contains("ocean") ||
                lower.contains("lake") || lower.contains("river") -> SceneType.TRAVEL

            // 城市
            lower.contains("building") || lower.contains("architecture") || lower.contains("skyscraper") -> SceneType.ARCHITECTURE
            lower.contains("street") || lower.contains("road") -> SceneType.STREET
            lower.contains("city") || lower.contains("urban") -> SceneType.URBAN

            // 动物
            lower.contains("cat") || lower.contains("dog") || lower.contains("pet") ||
                lower.contains("animal") || lower.contains("bird") || lower.contains("horse") -> SceneType.PET

            // 物品
            lower.contains("product") || lower.contains("merchandise") -> SceneType.PRODUCT
            lower.contains("macro") || lower.contains("flower") || lower.contains("insect") -> SceneType.MACRO
            lower.contains("document") || lower.contains("text") || lower.contains("paper") -> SceneType.PRODUCT

            // 活动
            lower.contains("sport") || lower.contains("running") || lower.contains("ball") -> SceneType.SPORTS
            lower.contains("action") -> SceneType.ACTION
            lower.contains("event") || lower.contains("party") || lower.contains("concert") -> SceneType.EVENT

            // 室内
            lower.contains("indoor") || lower.contains("room") || lower.contains("interior") -> SceneType.INDOOR
            lower.contains("outdoor") -> SceneType.OUTDOOR

            else -> SceneType.UNKNOWN
        }
    }

    // MARK: - 光环境计算

    /**
     * 估算色温（基于 RGB 平均值）
     */
    private fun estimateColorTemperature(avgR: Float, avgG: Float, avgB: Float): Float {
        // 暖色（R > B）→ 低色温；冷色（B > R）→ 高色温
        val ratio = if (avgB > 0) avgR / avgB else 1f
        // ratio=1 → 5500K（日光）
        // ratio>1（暖）→ <5500K
        // ratio<1（冷）→ >5500K
        return (5500f / ratio.coerceIn(0.3f, 3.0f)).coerceIn(2000f, 12000f)
    }

    /**
     * 逆光检测：边缘亮度 > 中心亮度
     */
    private fun detectBacklit(centerLum: Float, edgeLum: Float): Boolean {
        return edgeLum > centerLum * 1.3f && edgeLum > 100f
    }

    /**
     * 确定光源类型
     */
    private fun determineLightType(
        brightness: Float,
        colorTemperature: Float,
        isBacklit: Boolean,
        avgR: Float,
        avgG: Float,
        avgB: Float
    ): LightType {
        if (isBacklit) return LightType.MIXED
        if (brightness < 0.15f) return LightType.MIXED // 低光难以判断

        return when {
            colorTemperature < 3500f -> LightType.WARM // 钨丝灯/日落
            colorTemperature > 7000f -> LightType.COOL // 阴影/阴天
            Math.abs(avgG - avgR) > 30 && Math.abs(avgG - avgB) > 30 -> LightType.FLUORESCENT // 荧光灯偏绿
            else -> LightType.NATURAL
        }
    }

    /**
     * 计算主体位置（9 宫格）
     */
    private fun computeSubjectPosition(face: FaceRect): SubjectPosition {
        val centerX = face.centerX
        val centerY = face.centerY

        val horizontal = when {
            centerX < 0.33f -> "LEFT"
            centerX > 0.66f -> "RIGHT"
            else -> "CENTER"
        }
        val vertical = when {
            centerY < 0.33f -> "TOP"
            centerY > 0.66f -> "BOTTOM"
            else -> "MIDDLE"
        }

        return when {
            vertical == "TOP" && horizontal == "LEFT" -> SubjectPosition.TOP_LEFT
            vertical == "TOP" && horizontal == "CENTER" -> SubjectPosition.TOP_CENTER
            vertical == "TOP" && horizontal == "RIGHT" -> SubjectPosition.TOP_RIGHT
            vertical == "MIDDLE" && horizontal == "LEFT" -> SubjectPosition.MIDDLE_LEFT
            vertical == "MIDDLE" && horizontal == "RIGHT" -> SubjectPosition.MIDDLE_RIGHT
            vertical == "BOTTOM" && horizontal == "LEFT" -> SubjectPosition.BOTTOM_LEFT
            vertical == "BOTTOM" && horizontal == "CENTER" -> SubjectPosition.BOTTOM_CENTER
            vertical == "BOTTOM" && horizontal == "RIGHT" -> SubjectPosition.BOTTOM_RIGHT
            else -> SubjectPosition.CENTER
        }
    }

    /**
     * 释放资源
     */
    fun close() {
        try { imageLabeler.close() } catch (_: Exception) {}
        try { faceDetector.close() } catch (_: Exception) {}
        try { objectDetector.close() } catch (_: Exception) {}
    }
}
