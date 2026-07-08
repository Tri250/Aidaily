package com.livecompose.livecapture.core.camera

import org.junit.Assert.*
import org.junit.Test

/**
 * CameraManager 单元测试
 */
class CameraManagerTest {

    @Test
    fun `cameraModels default values`() {
        val models = CameraModels()
        assertNotNull(models)
    }

    @Test
    fun `hyperfocalCalculator computes correctly`() {
        val calculator = HyperfocalCalculator()
        // 测试50mm镜头，f/2.8，对焦5米
        val result = calculator.calculate(
            focalLength = 50f,
            aperture = 2.8f,
            focusDistance = 5000f,
            sensorSize = 36f
        )
        assertNotNull(result)
        assertTrue(result.hyperfocalDistance > 0)
        assertTrue(result.nearLimit > 0)
        assertTrue(result.farLimit > result.nearLimit)
        assertTrue(result.depthOfField > 0)
    }

    @Test
    fun `hyperfocalCalculator edge case - very small aperture`() {
        val calculator = HyperfocalCalculator()
        val result = calculator.calculate(
            focalLength = 24f,
            aperture = 22f,
            focusDistance = 1000f,
            sensorSize = 36f
        )
        assertNotNull(result)
        // 小光圈（大f值）应产生大景深
        assertTrue(result.depthOfField > 100f)
    }

    @Test
    fun `hyperfocalCalculator edge case - infinity focus`() {
        val calculator = HyperfocalCalculator()
        val result = calculator.calculate(
            focalLength = 35f,
            aperture = 8f,
            focusDistance = Float.MAX_VALUE,
            sensorSize = 36f
        )
        assertNotNull(result)
    }
}
