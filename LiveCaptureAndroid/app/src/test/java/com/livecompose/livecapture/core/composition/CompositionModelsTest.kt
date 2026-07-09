package com.livecompose.livecapture.core.composition

import org.junit.Assert.*
import org.junit.Test

/**
 * 构图模型单元测试
 *
 * 测试 CompositionGuideType、ScoreGrade、CompositionScore、
 * 姿势分类和模板等数据模型。
 */
class CompositionModelsTest {

    // ====== CompositionGuideType 测试 ======

    @Test
    fun `compositionGuideType has 8 types`() {
        assertEquals(8, CompositionGuideType.entries.size)
    }

    @Test
    fun `compositionGuideType display names are Chinese`() {
        assertEquals("三分法", CompositionGuideType.RULE_OF_THIRDS.displayName)
        assertEquals("黄金比例", CompositionGuideType.GOLDEN_RATIO.displayName)
        assertEquals("黄金螺旋", CompositionGuideType.GOLDEN_SPIRAL.displayName)
        assertEquals("对称构图", CompositionGuideType.SYMMETRY.displayName)
        assertEquals("中心聚焦", CompositionGuideType.CENTER_FOCUS.displayName)
        assertEquals("引导线", CompositionGuideType.LEADING_LINES.displayName)
        assertEquals("方形", CompositionGuideType.SQUARE.displayName)
        assertEquals("无", CompositionGuideType.NONE.displayName)
    }

    @Test
    fun `compositionGuideType icon names are non-empty`() {
        for (type in CompositionGuideType.entries) {
            assertTrue("${type.name} should have icon name", type.iconName.isNotEmpty())
        }
    }

    @Test
    fun `compositionGuideType icon names are distinct`() {
        val icons = CompositionGuideType.entries.map { it.iconName }
        assertEquals(icons.size, icons.toSet().size)
    }

    // ====== ScoreGrade 测试 ======

    @Test
    fun `scoreGrade has 4 levels`() {
        assertEquals(4, ScoreGrade.entries.size)
    }

    @Test
    fun `scoreGrade display names are Chinese`() {
        assertEquals("优秀", ScoreGrade.EXCELLENT.displayName)
        assertEquals("良好", ScoreGrade.GOOD.displayName)
        assertEquals("一般", ScoreGrade.AVERAGE.displayName)
        assertEquals("需改进", ScoreGrade.NEEDS_IMPROVEMENT.displayName)
    }

    @Test
    fun `scoreGrade colors are distinct`() {
        val colors = ScoreGrade.entries.map { it.color }
        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun `scoreGrade EXCELLENT is green`() {
        assertEquals(0xFF4CAF50, ScoreGrade.EXCELLENT.color)
    }

    @Test
    fun `scoreGrade NEEDS_IMPROVEMENT is red`() {
        assertEquals(0xFFF44336, ScoreGrade.NEEDS_IMPROVEMENT.color)
    }

    // ====== CompositionScore 测试 ======

    @Test
    fun `compositionScore 95 is EXCELLENT`() {
        val score = CompositionScore(95, 90, 95, 100, 100, "")
        assertEquals(ScoreGrade.EXCELLENT, score.grade)
    }

    @Test
    fun `compositionScore 80 is GOOD`() {
        val score = CompositionScore(80, 75, 80, 85, 80, "")
        assertEquals(ScoreGrade.GOOD, score.grade)
    }

    @Test
    fun `compositionScore 65 is AVERAGE`() {
        val score = CompositionScore(65, 60, 70, 60, 70, "")
        assertEquals(ScoreGrade.AVERAGE, score.grade)
    }

    @Test
    fun `compositionScore 50 is NEEDS_IMPROVEMENT`() {
        val score = CompositionScore(50, 50, 50, 50, 50, "")
        assertEquals(ScoreGrade.NEEDS_IMPROVEMENT, score.grade)
    }

    @Test
    fun `compositionScore all scores in 0-100 range`() {
        val score = CompositionScore(75, 80, 70, 85, 90, "建议")
        assertTrue(score.overall in 0..100)
        assertTrue(score.ruleOfThirds in 0..100)
        assertTrue(score.balance in 0..100)
        assertTrue(score.centering in 0..100)
        assertTrue(score.horizonLevel in 0..100)
    }

    @Test
    fun `compositionScore feedback is preserved`() {
        val feedback = "建议将主体放在三分线交点处"
        val score = CompositionScore(60, 55, 60, 65, 70, feedback)
        assertEquals(feedback, score.feedback)
    }

    @Test
    fun `compositionScore grade boundary 90`() {
        // 90 → EXCELLENT
        assertEquals(ScoreGrade.EXCELLENT, CompositionScore(90, 90, 90, 90, 90, "").grade)
        // 89 → GOOD
        assertEquals(ScoreGrade.GOOD, CompositionScore(89, 89, 89, 89, 89, "").grade)
    }

    @Test
    fun `compositionScore grade boundary 75`() {
        assertEquals(ScoreGrade.GOOD, CompositionScore(75, 75, 75, 75, 75, "").grade)
        assertEquals(ScoreGrade.AVERAGE, CompositionScore(74, 74, 74, 74, 74, "").grade)
    }

    @Test
    fun `compositionScore grade boundary 60`() {
        assertEquals(ScoreGrade.AVERAGE, CompositionScore(60, 60, 60, 60, 60, "").grade)
        assertEquals(ScoreGrade.NEEDS_IMPROVEMENT, CompositionScore(59, 59, 59, 59, 59, "").grade)
    }

    // ====== PoseCategory (composition) 测试 ======

    @Test
    fun `poseCategory has 5 categories`() {
        assertEquals(5, PoseCategory.entries.size)
    }

    @Test
    fun `poseCategory display names are Chinese`() {
        assertEquals("单人", PoseCategory.SOLO.displayName)
        assertEquals("双人", PoseCategory.COUPLE.displayName)
        assertEquals("朋友", PoseCategory.FRIENDS.displayName)
        assertEquals("家庭", PoseCategory.FAMILY.displayName)
        assertEquals("宠物", PoseCategory.PET.displayName)
    }

    // ====== PoseTemplate (composition) 测试 ======

    @Test
    fun `poseTemplate has all fields`() {
        val template = PoseTemplate(
            id = "pose_001",
            name = "经典站立",
            category = PoseCategory.SOLO,
            description = "经典单人站立姿势",
            tips = listOf("保持微笑", "双肩放松", "微收下巴")
        )
        assertEquals("pose_001", template.id)
        assertEquals("经典站立", template.name)
        assertEquals(PoseCategory.SOLO, template.category)
        assertEquals(3, template.tips.size)
    }
}