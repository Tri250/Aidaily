package com.livecompose.livecapture.core.composition

import android.graphics.RectF
import org.junit.Assert.*
import org.junit.Test

/**
 * 构图评分引擎单元测试
 *
 * 测试三分法、平衡性、居中、水平线四个维度的评分算法，
 * 以及综合评分、反馈生成和边界条件。
 */
class CompositionScorerTest {

    private val scorer = CompositionScorer()

    // ====== 综合评分参数测试 ======

    @Test
    fun `scoreComposition returns valid range`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100,
            imageHeight = 100,
            faces = emptyList(),
            horizonAngle = 0f
        )
        assertTrue("Overall score should be 0-100, got ${result.overall}", result.overall in 0..100)
        assertTrue(result.ruleOfThirds in 0..100)
        assertTrue(result.balance in 0..100)
        assertTrue(result.centering in 0..100)
        assertTrue(result.horizonLevel in 0..100)
    }

    @Test
    fun `scoreComposition with perfect conditions gives high score with no faces`() {
        // 裁切中心在画面中心，无倾斜
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100,
            imageHeight = 100,
            faces = emptyList(),
            horizonAngle = 0f
        )
        assertTrue("Overall score should be relatively high, got ${result.overall}",
            result.overall >= 50)
    }

    @Test
    fun `scoreComposition with tilted horizon gives lower score`() {
        val level = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100,
            imageHeight = 100,
            faces = emptyList(),
            horizonAngle = 0f
        )
        val tilted = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100,
            imageHeight = 100,
            faces = emptyList(),
            horizonAngle = 15f
        )
        assertTrue("Tilted should score lower than level", tilted.overall < level.overall)
    }

    // ====== 水平线评分 ======

    @Test
    fun `horizon level 0 degrees gives 100`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 0f
        )
        assertEquals(100, result.horizonLevel)
    }

    @Test
    fun `horizon level 10 degrees gives 50`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 10f
        )
        assertEquals(50, result.horizonLevel)
    }

    @Test
    fun `horizon level 20 degrees gives 0`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 20f
        )
        assertEquals(0, result.horizonLevel)
    }

    @Test
    fun `horizon level negative angle handled symmetrically`() {
        val pos = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 5f
        )
        val neg = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = -5f
        )
        assertEquals(pos.horizonLevel, neg.horizonLevel)
    }

    // ====== 三分法评分 ======

    @Test
    fun `ruleOfThirds with face at third point gives high score`() {
        // 人脸中心在 (1/3, 1/3) 即左上三分点
        val face = RectF(0.28f, 0.28f, 0.38f, 0.38f) // center ≈ (0.33, 0.33)
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = listOf(face), horizonAngle = 0f
        )
        assertTrue("Rule of thirds with face at third point should be high, got ${result.ruleOfThirds}",
            result.ruleOfThirds >= 80)
    }

    @Test
    fun `ruleOfThirds with face at center gives lower score`() {
        // 人脸中心在 (0.5, 0.5) 即画面中心
        val face = RectF(0.45f, 0.45f, 0.55f, 0.55f)
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = listOf(face), horizonAngle = 0f
        )
        // 中心不在三分点上，分数应较低
        assertTrue("Rule of thirds with face at center should be lower",
            result.ruleOfThirds < 85)
    }

    @Test
    fun `ruleOfThirds with no faces uses crop center`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(25f, 25f, 75f, 75f), // center = (0.5, 0.5)
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 0f
        )
        assertTrue(result.ruleOfThirds in 0..100)
        assertNotNull(result.feedback)
    }

    // ====== 平衡性评分 ======

    @Test
    fun `balance with no faces returns 50`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 0f
        )
        assertEquals(50, result.balance)
    }

    @Test
    fun `balance with single face centered is high`() {
        // 单张人脸在裁切区域中心
        val face = RectF(0.45f, 0.45f, 0.55f, 0.55f)
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = listOf(face), horizonAngle = 0f
        )
        assertTrue("Balance with centered face should be high, got ${result.balance}",
            result.balance >= 70)
    }

    @Test
    fun `balance with multiple faces is calculated`() {
        val faces = listOf(
            RectF(0.2f, 0.2f, 0.3f, 0.3f),
            RectF(0.6f, 0.6f, 0.7f, 0.7f)
        )
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = faces, horizonAngle = 0f
        )
        assertTrue(result.balance in 0..100)
    }

    // ====== 居中评分 ======

    @Test
    fun `centering with no faces and crop at center gives high score`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 0f
        )
        assertTrue("Centering should be high, got ${result.centering}",
            result.centering >= 80)
    }

    @Test
    fun `centering with offset crop gives lower score`() {
        val centered = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 0f
        )
        val offset = scorer.scoreComposition(
            cropRect = RectF(20f, 20f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 0f
        )
        assertTrue("Offset should score lower than centered",
            offset.centering < centered.centering)
    }

    // ====== 反馈生成 ======

    @Test
    fun `feedback is never empty`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 0f
        )
        assertNotNull(result.feedback)
        assertTrue(result.feedback.isNotEmpty())
    }

    @Test
    fun `feedback is in Chinese`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 10f
        )
        // 倾斜较大时应有水平提示
        assertTrue(result.feedback.contains("水平"))
    }

    @Test
    fun `feedback with perfect conditions gives encouraging message`() {
        // 完美条件：人脸在三分点，水平仪完美
        val face = RectF(0.28f, 0.28f, 0.38f, 0.38f)
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = listOf(face), horizonAngle = 0f
        )
        // 至少不应是错误提示
        assertTrue(result.feedback.isNotEmpty())
    }

    // ====== 边界条件 ======

    @Test
    fun `scoreComposition handles zero width gracefully`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 0f, 0f),
            imageWidth = 0, imageHeight = 0,
            faces = emptyList(), horizonAngle = 0f
        )
        assertTrue(result.overall in 0..100)
        assertNotNull(result.feedback)
    }

    @Test
    fun `scoreComposition handles extreme horizon angle`() {
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = emptyList(), horizonAngle = 90f
        )
        assertEquals(0, result.horizonLevel)
    }

    @Test
    fun `scoreComposition with dozens of faces`() {
        val faces = (0 until 50).map { RectF(0.1f, 0.1f, 0.2f, 0.2f) }
        val result = scorer.scoreComposition(
            cropRect = RectF(0f, 0f, 100f, 100f),
            imageWidth = 100, imageHeight = 100,
            faces = faces, horizonAngle = 0f
        )
        assertTrue(result.overall in 0..100)
        assertTrue(result.balance in 0..100)
    }
}