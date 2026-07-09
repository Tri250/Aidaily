package com.livecompose.livecapture.core.editing

import org.junit.Assert.*
import org.junit.Test

/**
 * HSL 处理器单元测试
 *
 * 测试 HslChannel 色相中心、HslParams 数据类、色相环距离计算、
 * RGB↔HSV 转换以及参数边界条件。
 */
class HslProcessorTest {

    private val processor = HslProcessor()

    // ====== HslChannel 测试 ======

    @Test
    fun `hslChannel has 8 entries`() {
        assertEquals(8, HslChannel.entries.size)
    }

    @Test
    fun `hslChannel hueCenters has 8 elements`() {
        assertEquals(8, HslChannel.hueCenters.size)
    }

    @Test
    fun `hslChannel hueCenters match enum values`() {
        for (channel in HslChannel.entries) {
            assertEquals(channel.hueCenter, HslChannel.hueCenters[channel.index])
        }
    }

    @Test
    fun `hslChannel indices are sequential 0 to 7`() {
        val indices = HslChannel.entries.map { it.index }.sorted()
        assertEquals((0..7).toList(), indices)
    }

    @Test
    fun `hslChannel hueCenters are evenly spaced by 30 degrees`() {
        for (i in 1 until HslChannel.hueCenters.size) {
            val diff = HslChannel.hueCenters[i] - HslChannel.hueCenters[i - 1]
            assertEquals(30f, diff, 0.01f)
        }
    }

    @Test
    fun `hslChannel red hueCenter is 0`() {
        assertEquals(0f, HslChannel.RED.hueCenter)
    }

    @Test
    fun `hslChannel blue hueCenter is 240`() {
        assertEquals(240f, HslChannel.BLUE.hueCenter)
    }

    @Test
    fun `hslChannel cyan hueCenter is 180`() {
        assertEquals(180f, HslChannel.CYAN.hueCenter)
    }

    // ====== HslParams 测试 ======

    @Test
    fun `hslParams default isDefault`() {
        val params = HslParams()
        assertTrue(params.isDefault)
    }

    @Test
    fun `hslParams with modified hue is not default`() {
        val hue = FloatArray(8) { 0f }
        hue[0] = 0.1f
        val params = HslParams(hue = hue)
        assertFalse(params.isDefault)
    }

    @Test
    fun `hslParams with modified saturation is not default`() {
        val sat = FloatArray(8) { 0f }
        sat[3] = -0.5f
        val params = HslParams(saturation = sat)
        assertFalse(params.isDefault)
    }

    @Test
    fun `hslParams with modified lightness is not default`() {
        val light = FloatArray(8) { 0f }
        light[7] = 0.3f
        val params = HslParams(lightness = light)
        assertFalse(params.isDefault)
    }

    @Test
    fun `hslParams equals when same values`() {
        val a = HslParams(
            hue = FloatArray(8) { 0.1f },
            saturation = FloatArray(8) { 0.2f },
            lightness = FloatArray(8) { 0.3f }
        )
        val b = HslParams(
            hue = FloatArray(8) { 0.1f },
            saturation = FloatArray(8) { 0.2f },
            lightness = FloatArray(8) { 0.3f }
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `hslParams not equals when different values`() {
        val a = HslParams(hue = FloatArray(8) { 0.1f })
        val b = HslParams(hue = FloatArray(8) { 0.2f })
        assertNotEquals(a, b)
    }

    // ====== 色相环距离测试 ======

    @Test
    fun `circularDistance same hue is 0`() {
        val dist = processor.circularDistance(180f, 180f)
        assertEquals(0f, dist, 0.001f)
    }

    @Test
    fun `circularDistance opposite hue is 180`() {
        val dist = processor.circularDistance(0f, 180f)
        assertEquals(180f, dist, 0.001f)
    }

    @Test
    fun `circularDistance wraps around 360`() {
        val dist = processor.circularDistance(350f, 10f)
        assertEquals(20f, dist, 0.001f)
    }

    @Test
    fun `circularDistance symmetric`() {
        for (a in listOf(0f, 90f, 180f, 270f, 359f)) {
            for (b in listOf(30f, 120f, 210f, 300f)) {
                assertEquals(
                    processor.circularDistance(a, b),
                    processor.circularDistance(b, a),
                    0.001f
                )
            }
        }
    }

    @Test
    fun `circularDistance max value is 180`() {
        for (a in listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)) {
            for (b in listOf(0f, 60f, 120f, 180f, 240f, 300f)) {
                val dist = processor.circularDistance(a, b)
                assertTrue("Distance $dist should be <= 180", dist <= 180f)
            }
        }
    }

    @Test
    fun `circularDistance handles negative values`() {
        val dist = processor.circularDistance(-10f, 10f)
        assertEquals(20f, dist, 0.001f)
    }
}