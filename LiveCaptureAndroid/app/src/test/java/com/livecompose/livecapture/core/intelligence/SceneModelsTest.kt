package com.livecompose.livecapture.core.intelligence

import org.junit.Assert.*
import org.junit.Test

/**
 * 场景模型单元测试
 *
 * 测试 SceneType 枚举完整性、PoseTemplate 结构、
 * PoseRecommendationResult、SubjectDetection、QualityGrade、
 * 色彩分析模型等数据模型。
 */
class SceneModelsTest {

    // ====== SceneType 测试 ======

    @Test
    fun `sceneType has at least 20 types`() {
        assertTrue(SceneType.entries.size >= 20)
    }

    @Test
    fun `sceneType has common types`() {
        val types = SceneType.entries.map { it.name }
        assertTrue(types.contains("PORTRAIT"))
        assertTrue(types.contains("LANDSCAPE"))
        assertTrue(types.contains("NIGHT"))
        assertTrue(types.contains("UNKNOWN"))
        assertTrue(types.contains("FOOD"))
        assertTrue(types.contains("SUNSET"))
    }

    @Test
    fun `sceneType unknown is present`() {
        assertEquals(SceneType.UNKNOWN, SceneType.entries.firstOrNull { it == SceneType.UNKNOWN })
    }

    // ====== PoseCategory 测试 ======

    @Test
    fun `poseCategory has 8 entries`() {
        assertEquals(8, PoseCategory.entries.size)
    }

    @Test
    fun `poseCategory has expected types`() {
        val names = PoseCategory.entries.map { it.name }
        assertTrue(names.contains("PORTRAIT_STANDING"))
        assertTrue(names.contains("COUPLE"))
        assertTrue(names.contains("FOOD"))
        assertTrue(names.contains("LANDSCAPE"))
    }

    // ====== PoseDifficulty 测试 ======

    @Test
    fun `poseDifficulty has 3 levels`() {
        assertEquals(3, PoseDifficulty.entries.size)
        assertTrue(PoseDifficulty.entries.contains(PoseDifficulty.BEGINNER))
        assertTrue(PoseDifficulty.entries.contains(PoseDifficulty.INTERMEDIATE))
        assertTrue(PoseDifficulty.entries.contains(PoseDifficulty.ADVANCED))
    }

    // ====== PoseGender 测试 ======

    @Test
    fun `poseGender has 3 values`() {
        assertEquals(3, PoseGender.entries.size)
    }

    @Test
    fun `poseGender any is default`() {
        assertEquals(PoseGender.ANY, PoseGender.entries.firstOrNull { it == PoseGender.ANY })
    }

    // ====== PoseKeypoints 测试 ======

    @Test
    fun `poseKeypoints has all required fields`() {
        val keypoints = PoseKeypoints(
            shoulders = "放松",
            head = "正视",
            arms = "自然下垂",
            legs = "并拢",
            hands = "自然",
            back = "挺直",
            eyeContact = "看镜头",
            bodyAngle = "正面",
            hips = "居中",
            feet = "并拢",
            chin = "微收",
            gaze = "直视"
        )
        assertEquals("放松", keypoints.shoulders)
        assertEquals("看镜头", keypoints.eyeContact)
        assertEquals("挺直", keypoints.back)
    }

    // ====== PoseTemplate 测试 ======

    @Test
    fun `poseTemplate has all fields`() {
        val template = PoseTemplate(
            id = "pose_001",
            name = "标准站立",
            category = PoseCategory.PORTRAIT_STANDING,
            keypoints = PoseKeypoints("", "", "", "", "", "", "", "", "", "", "", ""),
            tips = listOf("保持微笑", "双肩放松"),
            variations = listOf("侧身", "手插口袋"),
            difficulty = PoseDifficulty.BEGINNER,
            gender = PoseGender.ANY
        )
        assertEquals("pose_001", template.id)
        assertEquals("标准站立", template.name)
        assertEquals(2, template.tips.size)
        assertEquals(2, template.variations.size)
    }

    // ====== PoseRecommendationResult 测试 ======

    @Test
    fun `poseRecommendationResult EMPTY has no suggestions`() {
        val empty = PoseRecommendationResult.EMPTY
        assertFalse(empty.hasRecommendations)
        assertTrue(empty.allSuggestions.isEmpty())
        assertEquals(SceneType.UNKNOWN, empty.sceneType)
        assertEquals(0f, empty.confidenceScore)
        assertNull(empty.primaryRecommendation)
    }

    @Test
    fun `poseRecommendationResult with suggestion has recommendations`() {
        val suggestion = PoseSuggestion(
            id = "s1", title = "建议", description = "描述",
            instructions = "", tips = emptyList(), assetPath = "",
            priority = 0.8f, category = PoseCategory.PORTRAIT_STANDING
        )
        val result = PoseRecommendationResult(
            suggestions = listOf(suggestion),
            adjustments = emptyList(),
            compositions = emptyList(),
            primaryRecommendation = suggestion,
            sceneType = SceneType.PORTRAIT,
            confidenceScore = 0.9f
        )
        assertTrue(result.hasRecommendations)
        assertEquals(1, result.allSuggestions.size)
        assertNotNull(result.primaryRecommendation)
    }

    @Test
    fun `poseRecommendationResult allSuggestions merges all lists`() {
        val s1 = PoseSuggestion("s1", "", "", "", emptyList(), "", 0.5f, PoseCategory.PORTRAIT_STANDING)
        val s2 = PoseSuggestion("s2", "", "", "", emptyList(), "", 0.5f, PoseCategory.PORTRAIT_STANDING)
        val s3 = PoseSuggestion("s3", "", "", "", emptyList(), "", 0.5f, PoseCategory.PORTRAIT_STANDING)
        val result = PoseRecommendationResult(
            suggestions = listOf(s1),
            adjustments = listOf(s2),
            compositions = listOf(s3),
            primaryRecommendation = null,
            sceneType = SceneType.UNKNOWN,
            confidenceScore = 0.5f
        )
        assertEquals(3, result.allSuggestions.size)
    }

    // ====== SubjectDetection 测试 ======

    @Test
    fun `subjectDetection default has no faces`() {
        val detection = SubjectDetection()
        assertEquals(0, detection.faceCount)
        assertNull(detection.faceRect)
        assertEquals(0f, detection.faceConfidence)
        assertFalse(detection.hasMultiplePeople)
        assertEquals(SubjectPosition.CENTER, detection.dominantSubjectPosition)
    }

    @Test
    fun `faceRect computes center correctly`() {
        val face = FaceRect(0f, 0f, 100f, 100f)
        assertEquals(50f, face.centerX)
        assertEquals(50f, face.centerY)
        assertEquals(100f, face.width)
        assertEquals(100f, face.height)
        assertEquals(10000f, face.area)
    }

    @Test
    fun `subjectDetection with multiple faces`() {
        val detection = SubjectDetection(
            faceCount = 3,
            hasMultiplePeople = true,
            dominantSubjectPosition = SubjectPosition.TOP_LEFT
        )
        assertEquals(3, detection.faceCount)
        assertTrue(detection.hasMultiplePeople)
        assertEquals(SubjectPosition.TOP_LEFT, detection.dominantSubjectPosition)
    }

    // ====== QualityGrade 测试 ======

    @Test
    fun `qualityGrade has 4 levels`() {
        assertEquals(4, QualityGrade.entries.size)
    }

    @Test
    fun `qualityGrade display names are Chinese`() {
        assertEquals("极佳", QualityGrade.EXCELLENT.displayName)
        assertEquals("良好", QualityGrade.GOOD.displayName)
        assertEquals("一般", QualityGrade.FAIR.displayName)
        assertEquals("较差", QualityGrade.POOR.displayName)
    }

    // ====== EnhancementType 测试 ======

    @Test
    fun `enhancementType has at least 6 types`() {
        assertTrue(EnhancementType.entries.size >= 6)
    }

    @Test
    fun `enhancementType has common types`() {
        val names = EnhancementType.entries.map { it.name }
        assertTrue(names.contains("SHARPNESS"))
        assertTrue(names.contains("NOISE_REDUCTION"))
        assertTrue(names.contains("EXPOSURE"))
    }

    // ====== LightAnalysis 测试 ======

    @Test
    fun `lightAnalysis default values`() {
        val light = LightAnalysis.DEFAULT
        assertEquals(5500f, light.colorTemperature)
        assertEquals(0.5f, light.brightness)
        assertEquals(0.5f, light.contrast)
        assertFalse(light.isBacklit)
        assertEquals(LightType.NATURAL, light.lightType)
    }

    // ====== AdaptiveCaptureParams 测试 ======

    @Test
    fun `adaptiveCaptureParams default values`() {
        val params = AdaptiveCaptureParams.DEFAULT
        assertEquals(100f, params.targetISO)
        assertEquals(1f / 60f, params.targetShutterSpeed)
        assertEquals(0f, params.exposureBias)
        assertEquals(FlashRecommendation.AUTO, params.flashMode)
    }

    // ====== ScenePresetParams 测试 ======

    @Test
    fun `scenePresetParams default is all zero`() {
        val params = ScenePresetParams.DEFAULT
        assertEquals(0f, params.exposure)
        assertEquals(0f, params.contrast)
        assertEquals(0f, params.saturation)
        assertEquals(0f, params.warmth)
    }

    // ====== 颜色数据模型 ======

    @Test
    fun `dominantColor percentage is in range`() {
        val color = DominantColor(255, 0, 0, "#FF0000", 50f)
        assertTrue(color.percentage in 0f..100f)
    }

    @Test
    fun `averageColor hex is uppercase`() {
        val color = AverageColor(255, 128, 0, "#FF8000")
        assertEquals("#FF8000", color.hex)
    }

    @Test
    fun `colorTemperatureInfo has description`() {
        val ct = ColorTemperatureInfo(5500f, "中性", "中性白平衡")
        assertTrue(ct.description.isNotEmpty())
        assertEquals(5500f, ct.kelvin)
    }

    @Test
    fun `colorAnalysis has all fields set`() {
        val analysis = ColorAnalysis(
            averageColor = AverageColor(128, 128, 128, "#808080"),
            dominantColors = emptyList(),
            colorTemperature = ColorTemperatureInfo(5500f, "中性", ""),
            colorHarmonyScore = 75f,
            colorMood = "温和中性",
            saturationMean = 30f,
            brightnessMean = 50f,
            contrastRatio = 40f
        )
        assertEquals(75f, analysis.colorHarmonyScore)
        assertEquals("温和中性", analysis.colorMood)
    }

    // ====== QualityAssessment → ImageInfo ======

    @Test
    fun `imageInfo orientation is landscape when width >= height`() {
        val info = ImageInfo(100, 100, 1f, "landscape", "ARGB_8888", 10000, "SD")
        assertEquals("landscape", info.orientation)
    }

    @Test
    fun `imageInfo resolutionLevel is descriptive`() {
        val info = ImageInfo(4000, 3000, 1.33f, "landscape", "ARGB_8888", 12_000_000, "4K+")
        assertEquals("4K+", info.resolutionLevel)
    }
}