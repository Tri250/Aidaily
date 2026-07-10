package com.livecompose.livecapture.core.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionResultTest {

    @Test
    fun `bbox center coordinates calculated correctly`() {
        val result = CompositionResult(
            bbox = floatArrayOf(0.5f, 0.3f, 0.8f, 0.6f),
            action = CompositionResult.ActionType.STOP,
            actionProbabilities = FloatArray(7) { 1f / 7f }
        )

        assertEquals(0.5f, result.bboxCenterX, 0.001f)
        assertEquals(0.3f, result.bboxCenterY, 0.001f)
        assertEquals(0.8f, result.bboxWidth, 0.001f)
        assertEquals(0.6f, result.bboxHeight, 0.001f)
    }

    @Test
    fun `overall score uses weighted formula`() {
        val result = CompositionResult(
            bbox = floatArrayOf(0.5f, 0.5f, 0.8f, 0.8f),
            action = CompositionResult.ActionType.STOP,
            actionProbabilities = FloatArray(7) { 1f / 7f },
            confidence = 0.9f,
            faceCoverage = 0.5f,
            ruleOfThirdsScore = 0.8f,
            safetyMarginScore = 1.0f
        )

        val expected = 0.9f * 0.4f + 0.5f * 0.3f + 0.8f * 0.2f + 1.0f * 0.1f
        assertEquals(expected, result.overallScore, 0.001f)
    }

    @Test
    fun `action type has 7 discrete actions`() {
        assertEquals(7, CompositionResult.ActionType.values().size)
    }

    @Test
    fun `action type contains all expected actions`() {
        val actions = CompositionResult.ActionType.values().toList()
        assertTrue(CompositionResult.ActionType.LEFT in actions)
        assertTrue(CompositionResult.ActionType.RIGHT in actions)
        assertTrue(CompositionResult.ActionType.UP in actions)
        assertTrue(CompositionResult.ActionType.DOWN in actions)
        assertTrue(CompositionResult.ActionType.ZOOM_IN in actions)
        assertTrue(CompositionResult.ActionType.ZOOM_OUT in actions)
        assertTrue(CompositionResult.ActionType.STOP in actions)
    }
}
