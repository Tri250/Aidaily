package com.livecompose.livecapture.core.intelligence

import android.graphics.RectF

// =============================================================================
// 1. SceneType — 场景类型枚举
// =============================================================================

enum class SceneType {
    PORTRAIT,
    PORTRAIT_STANDING,
    PORTRAIT_SITTING,
    COUPLE,
    CHILDREN,
    FOOD,
    LANDSCAPE,
    WEDDING,
    PRODUCT,
    BACKLIT,
    UNKNOWN,
    GROUP,
    SELFIE,
    NIGHT,
    SUNSET,
    URBAN,
    NATURE,
    INDOOR,
    OUTDOOR,
    SILHOUETTE,
    ACTION,
    STILL_LIFE,
    MACRO,
    TRAVEL,
    STREET,
    ARCHITECTURE,
    PET,
    SPORTS,
    EVENT,
    FASHION,
    BEAUTY,
    DOCUMENTARY,
    MINIMAL,
    VINTAGE,
    CINEMATIC
}

// =============================================================================
// 2. PoseCategory — 姿势分类
// =============================================================================

enum class PoseCategory {
    PORTRAIT_STANDING,
    PORTRAIT_SITTING,
    COUPLE,
    CHILDREN,
    FOOD,
    LANDSCAPE,
    WEDDING,
    PRODUCT
}

// =============================================================================
// 3. PoseDifficulty — 姿势难度
// =============================================================================

enum class PoseDifficulty {
    BEGINNER, INTERMEDIATE, ADVANCED
}

// =============================================================================
// 4. PoseGender — 姿势性别适配
// =============================================================================

enum class PoseGender {
    MALE, FEMALE, ANY
}

// =============================================================================
// 5. PoseKeypoints — 姿势关键点
// =============================================================================

data class PoseKeypoints(
    val shoulders: String,
    val head: String,
    val arms: String,
    val legs: String,
    val hands: String,
    val back: String,
    val eyeContact: String,
    val bodyAngle: String,
    val hips: String,
    val feet: String,
    val chin: String,
    val gaze: String
)

// =============================================================================
// 6. PoseTemplate — 姿势模板
// =============================================================================

data class PoseTemplate(
    val id: String,
    val name: String,
    val category: PoseCategory,
    val keypoints: PoseKeypoints,
    val tips: List<String>,
    val variations: List<String>,
    val difficulty: PoseDifficulty = PoseDifficulty.INTERMEDIATE,
    val gender: PoseGender = PoseGender.ANY
)

// =============================================================================
// 7. SubjectDetection — 被摄主体检测结果
// =============================================================================

data class SubjectDetection(
    val faceCount: Int = 0,
    val faceRect: FaceRect? = null,
    val bodyRect: BodyRect? = null,
    val faceConfidence: Float = 0.0f,
    val hasMultiplePeople: Boolean = false,
    val dominantSubjectPosition: SubjectPosition = SubjectPosition.CENTER,
    val subjectSizeRatio: Float = 0.0f,
    val detectedGenders: List<PoseGender> = emptyList(),
    val detectedAges: List<AgeGroup> = emptyList()
)

data class FaceRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float = 1.0f
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = width * height
}

data class BodyRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

enum class SubjectPosition {
    CENTER, TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MIDDLE_LEFT, MIDDLE_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

enum class AgeGroup {
    CHILD, TEEN, YOUNG_ADULT, ADULT, SENIOR
}

// =============================================================================
// 8. PoseSuggestion — 姿势建议
// =============================================================================

data class PoseSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val instructions: String,
    val tips: List<String>,
    val assetPath: String,
    val priority: Float,
    val category: PoseCategory,
    val difficulty: PoseDifficulty = PoseDifficulty.INTERMEDIATE,
    val isAdjustment: Boolean = false,
    val isComposition: Boolean = false,
    val isDynamic: Boolean = false
)

// =============================================================================
// 9. PoseRecommendationResult — 姿势推荐结果
// =============================================================================

data class PoseRecommendationResult(
    val suggestions: List<PoseSuggestion>,
    val adjustments: List<PoseSuggestion>,
    val compositions: List<PoseSuggestion>,
    val primaryRecommendation: PoseSuggestion?,
    val sceneType: SceneType,
    val confidenceScore: Float,
    val generatedAt: Long = System.currentTimeMillis()
) {
    val allSuggestions: List<PoseSuggestion>
        get() = suggestions + adjustments + compositions

    val hasRecommendations: Boolean
        get() = primaryRecommendation != null || suggestions.isNotEmpty()

    companion object {
        val EMPTY = PoseRecommendationResult(
            suggestions = emptyList(),
            adjustments = emptyList(),
            compositions = emptyList(),
            primaryRecommendation = null,
            sceneType = SceneType.UNKNOWN,
            confidenceScore = 0.0f
        )
    }
}

// =============================================================================
// 10. LightAnalysis — 光线分析
// =============================================================================

enum class LightType {
    NATURAL, WARM, COOL, FLUORESCENT, MIXED
}

data class LightAnalysis(
    val colorTemperature: Float,
    val brightness: Float,
    val contrast: Float,
    val isBacklit: Boolean,
    val lightType: LightType
) {
    companion object {
        val DEFAULT = LightAnalysis(
            colorTemperature = 5500f,
            brightness = 0.5f,
            contrast = 0.5f,
            isBacklit = false,
            lightType = LightType.NATURAL
        )
    }
}

// =============================================================================
// 11. AdaptiveCaptureParams — 自适应拍摄参数
// =============================================================================

enum class FlashRecommendation {
    AUTO, ON, OFF
}

data class AdaptiveCaptureParams(
    val targetISO: Float,
    val targetShutterSpeed: Float,
    val exposureBias: Float,
    val whiteBalanceTint: Float,
    val whiteBalanceTemperature: Float,
    val suggestedZoomFactor: Float,
    val suggestedLensType: String,
    val flashMode: FlashRecommendation
) {
    companion object {
        val DEFAULT = AdaptiveCaptureParams(
            targetISO = 100f,
            targetShutterSpeed = 1f / 60f,
            exposureBias = 0f,
            whiteBalanceTint = 0f,
            whiteBalanceTemperature = 5500f,
            suggestedZoomFactor = 1.0f,
            suggestedLensType = "wide",
            flashMode = FlashRecommendation.AUTO
        )
    }
}

// =============================================================================
// 12. QualityGrade / QualityAssessment — 画质评估
// =============================================================================

enum class QualityGrade(val displayName: String) {
    EXCELLENT("极佳"),
    GOOD("良好"),
    FAIR("一般"),
    POOR("较差")
}

data class QualityAssessment(
    val overallScore: Float,
    val sharpnessScore: Float,
    val noiseLevel: Float,
    val exposureScore: Float,
    val colorHarmonyScore: Float,
    val resolutionScore: Float,
    val qualityGrade: QualityGrade,
    val timestamp: Long,
    val imageInfo: ImageInfo?
)

data class ImageInfo(
    val width: Int,
    val height: Int,
    val aspectRatio: Float,
    val orientation: String,
    val format: String,
    val totalPixels: Long,
    val resolutionLevel: String
)

// =============================================================================
// 13. ColorAnalysis — 色彩分析
// =============================================================================

data class AverageColor(
    val r: Int, val g: Int, val b: Int, val hex: String
)

data class DominantColor(
    val r: Int, val g: Int, val b: Int, val hex: String, val percentage: Float
)

data class ColorTemperatureInfo(
    val kelvin: Float, val type: String, val description: String
)

data class ColorAnalysis(
    val averageColor: AverageColor,
    val dominantColors: List<DominantColor>,
    val colorTemperature: ColorTemperatureInfo,
    val colorHarmonyScore: Float,
    val colorMood: String,
    val saturationMean: Float,
    val brightnessMean: Float,
    val contrastRatio: Float
)

// =============================================================================
// 14. EnhancementType / EnhancementSuggestion — 增强建议
// =============================================================================

enum class EnhancementType {
    SHARPNESS, NOISE_REDUCTION, EXPOSURE, COLOR_HARMONY,
    PORTRAIT_ENHANCEMENT, LANDSCAPE_ENHANCEMENT,
    NIGHT_OPTIMIZATION, FOOD_ENHANCEMENT
}

enum class Priority {
    HIGH, MEDIUM, LOW
}

data class EnhancementSuggestion(
    val type: EnhancementType,
    val title: String,
    val description: String,
    val parameters: Map<String, Float>,
    val priority: Priority
)

// =============================================================================
// 15. ScenePresetParams — 场景预设参数
// =============================================================================

data class ScenePresetParams(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val clarity: Float = 0f,
    val warmth: Float = 0f,
    val sharpness: Float = 0f,
    val noiseReduction: Float = 0f,
    val vignette: Float = 0f
) {
    companion object {
        val DEFAULT = ScenePresetParams()
    }
}

// =============================================================================
// 16. InspirationEntry — 灵感条目
// =============================================================================

data class InspirationEntry(
    val id: String,
    val scene: SceneType,
    val style: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val photographerNote: String
)

// =============================================================================
// 17. CompositionAnalysis — 构图分析
// =============================================================================

data class CompositionAnalysis(
    val ruleOfThirdsScore: Float,
    val symmetryScore: Float,
    val visualBalanceScore: Float,
    val leadingLinesCount: Int,
    val focalPointsCount: Int,
    val compositionType: String,
    val feedback: String
)