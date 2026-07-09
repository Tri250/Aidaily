package com.livecompose.livecapture.core.composition

import org.junit.Assert.*
import org.junit.Test

/**
 * 构图模型单元测试
 *
 * 测试 CompositionGuideType 和 CompositionScore 等数据模型。
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

    // ====== CompositionScore 测试 ======

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
}