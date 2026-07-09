package com.livecompose.livecapture.core.editing

import org.junit.Assert.*
import org.junit.Test

/**
 * 曲线处理器单元测试
 *
 * 测试曲线预设、LUT 构建、Catmull-Rom 插值和曲线参数的完整性。
 */
class CurveProcessorTest {

    private val processor = CurveProcessor()

    // ====== 曲线预设测试 ======

    @Test
    fun `linear preset has 5 control points`() {
        val points = CurvePreset.LINEAR.controlPoints()
        assertEquals(5, points.size)
    }

    @Test
    fun `linear preset x equals y`() {
        for (point in CurvePreset.LINEAR.controlPoints()) {
            assertEquals(point.x, point.y, 0.001f)
        }
    }

    @Test
    fun `linear preset endpoints at 0,0 and 1,1`() {
        val points = CurvePreset.LINEAR.controlPoints()
        assertEquals(0f, points.first().x)
        assertEquals(0f, points.first().y)
        assertEquals(1f, points.last().x)
        assertEquals(1f, points.last().y)
    }

    @Test
    fun `soft contrast darkens shadows and brightens highlights`() {
        val points = CurvePreset.SOFT_CONTRAST.controlPoints()
        // 阴影区域 (x=0.25) 的 y 应低于 x
        assertTrue(points[1].y < points[1].x)
        // 高光区域 (x=0.75) 的 y 应高于 x
        assertTrue(points[3].y > points[3].x)
    }

    @Test
    fun `strong contrast has more extreme curve than soft contrast`() {
        val soft = CurvePreset.SOFT_CONTRAST.controlPoints()
        val strong = CurvePreset.STRONG_CONTRAST.controlPoints()
        // 强对比的阴影偏离更大
        val softShadowDeviation = soft[1].x - soft[1].y
        val strongShadowDeviation = strong[1].x - strong[1].y
        assertTrue(strongShadowDeviation > softShadowDeviation)
    }

    @Test
    fun `lift shadows raises black point`() {
        val points = CurvePreset.LIFT_SHADOWS.controlPoints()
        // 黑点 (x=0) 的 y 应 > 0
        assertTrue(points[0].y > 0f)
    }

    @Test
    fun `crush highlights lowers white point`() {
        val points = CurvePreset.CRUSH_HIGHLIGHTS.controlPoints()
        // 白点 (x=1) 的 y 应 < 1
        assertTrue(points[4].y < 1f)
    }

    @Test
    fun `all presets have 5 control points`() {
        for (preset in CurvePreset.entries) {
            assertEquals("${preset.displayName} should have 5 control points",
                5, preset.controlPoints().size)
        }
    }

    @Test
    fun `all presets start at 0,0 and end at 1,1 or near`() {
        for (preset in CurvePreset.entries) {
            val points = preset.controlPoints()
            assertEquals("${preset.displayName} should start at x=0",
                0f, points.first().x, 0.001f)
            assertEquals("${preset.displayName} should end at x=1",
                1f, points.last().x, 0.001f)
        }
    }

    // ====== LUT 构建测试 ======

    @Test
    fun `buildLut from linear control points returns identity`() {
        val lut = processor.buildLut(CurvePreset.LINEAR.controlPoints())
        assertEquals(256, lut.size)
        for (i in 0 until 256) {
            assertEquals(i, lut[i])
        }
    }

    @Test
    fun `buildLut returns 256 elements`() {
        for (preset in CurvePreset.entries) {
            val lut = processor.buildLut(preset.controlPoints())
            assertEquals("${preset.displayName} LUT should have 256 elements",
                256, lut.size)
        }
    }

    @Test
    fun `buildLut values are clamped to 0-255`() {
        for (preset in CurvePreset.entries) {
            val lut = processor.buildLut(preset.controlPoints())
            for (value in lut) {
                assertTrue("Value $value out of range for ${preset.displayName}",
                    value in 0..255)
            }
        }
    }

    @Test
    fun `buildLut is monotonic for linear preset`() {
        val lut = processor.buildLut(CurvePreset.LINEAR.controlPoints())
        for (i in 1 until lut.size) {
            assertTrue("LUT should be monotonic at index $i", lut[i] >= lut[i - 1])
        }
    }

    @Test
    fun `buildLut from empty points returns identity`() {
        val lut = processor.buildLut(emptyList())
        assertEquals(256, lut.size)
        for (i in 0 until 256) {
            assertEquals(i, lut[i])
        }
    }

    @Test
    fun `buildLut from single point returns identity`() {
        val lut = processor.buildLut(listOf(CurveControlPoint(0.5f, 0.5f)))
        assertEquals(256, lut.size)
        for (i in 0 until 256) {
            assertEquals(i, lut[i])
        }
    }

    // ====== 曲线采样测试 ======

    @Test
    fun `sampleCurve returns correct count`() {
        val samples = processor.sampleCurve(CurvePreset.LINEAR.controlPoints(), 128)
        assertEquals(128, samples.size)
    }

    @Test
    fun `sampleCurve values are in 0-1 range`() {
        for (preset in CurvePreset.entries) {
            val samples = processor.sampleCurve(preset.controlPoints())
            for (value in samples) {
                assertTrue("${preset.displayName} sample $value out of range",
                    value in 0f..1f)
            }
        }
    }

    @Test
    fun `sampleCurve linear returns increasing values`() {
        val samples = processor.sampleCurve(CurvePreset.LINEAR.controlPoints(), 64)
        for (i in 1 until samples.size) {
            assertTrue("Linear samples should increase: ${samples[i-1]} -> ${samples[i]}",
                samples[i] >= samples[i - 1] - 0.001f)
        }
    }

    // ====== CurveParams 测试 ======

    @Test
    fun `curveParams default is considered default`() {
        val params = CurveParams()
        assertTrue(params.isDefault)
    }

    @Test
    fun `curveParams with modified master is not default`() {
        val params = CurveParams(
            master = CurvePreset.SOFT_CONTRAST.controlPoints()
        )
        assertFalse(params.isDefault)
    }

    @Test
    fun `curveParams with modified red channel is not default`() {
        val params = CurveParams(
            red = CurvePreset.STRONG_CONTRAST.controlPoints()
        )
        assertFalse(params.isDefault)
    }

    // ====== CurveChannel 测试 ======

    @Test
    fun `curveChannel has correct display names`() {
        assertEquals("RGB", CurveChannel.RGB.displayName)
        assertEquals("R", CurveChannel.RED.displayName)
        assertEquals("G", CurveChannel.GREEN.displayName)
        assertEquals("B", CurveChannel.BLUE.displayName)
    }

    @Test
    fun `curveChannel has 4 entries`() {
        assertEquals(4, CurveChannel.entries.size)
    }

    // ====== Catmull-Rom 端点钳位测试 ======

    @Test
    fun `catmullRom endpoint at t=0 returns first point`() {
        val lut = processor.buildLut(CurvePreset.SOFT_CONTRAST.controlPoints())
        assertEquals(0, lut[0])
    }

    @Test
    fun `catmullRom endpoint at t=1 returns last point`() {
        val lut = processor.buildLut(CurvePreset.SOFT_CONTRAST.controlPoints())
        assertEquals(255, lut[255])
    }

    @Test
    fun `soft contrast LUT midpoint is approximately 128`() {
        val lut = processor.buildLut(CurvePreset.SOFT_CONTRAST.controlPoints())
        // 中点 (128) 应该接近 128，因为 S 曲线在中心交叉
        val midValue = lut[128]
        assertTrue("Midpoint should be near 128, got $midValue",
            kotlin.math.abs(midValue - 128) <= 5)
    }
}