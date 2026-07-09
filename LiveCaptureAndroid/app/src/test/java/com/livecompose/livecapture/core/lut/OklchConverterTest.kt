package com.livecompose.livecapture.core.lut

import org.junit.Assert.*
import org.junit.Test

/**
 * OKLCH 色彩空间转换器单元测试
 *
 * 测试 sRGB ↔ Linear ↔ Oklab ↔ OKLCH 双向转换精度、
 * 数值范围、边界条件和数学恒等式。
 */
class OklchConverterTest {

    // ====== sRGB ↔ Linear sRGB 转换 ======

    @Test
    fun `srgbToLinear identity maps 0 and 1`() {
        assertEquals(0f, OklchConverter.srgbToLinear(0f), 0.001f)
        assertEquals(1f, OklchConverter.srgbToLinear(1f), 0.001f)
    }

    @Test
    fun `srgbToLinear midpoint is less than input`() {
        // 伽马解码中，0.5 sRGB → < 0.5 linear
        val linear = OklchConverter.srgbToLinear(0.5f)
        assertTrue(linear < 0.5f)
        assertTrue(linear > 0.2f)
    }

    @Test
    fun `linearToSrgb inverse of srgbToLinear`() {
        for (v in listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)) {
            assertEquals(v, OklchConverter.linearToSrgb(OklchConverter.srgbToLinear(v)), 0.001f)
        }
    }

    @Test
    fun `srgbToLinear inverse of linearToSrgb`() {
        for (v in listOf(0f, 0.05f, 0.1f, 0.3f, 0.6f, 0.8f, 1f)) {
            assertEquals(v, OklchConverter.srgbToLinear(OklchConverter.linearToSrgb(v)), 0.001f)
        }
    }

    // ====== Oklab ↔ OKLCH 极坐标转换 ======

    @Test
    fun `oklabToOklch pure white`() {
        // 白色在 Oklab 中为 (1, 0, 0)
        val oklch = OklchConverter.oklabToOklch(1f, 0f, 0f)
        assertEquals(1f, oklch[0], 0.01f) // L = 1
        assertEquals(0f, oklch[1], 0.01f) // C = 0 (无色彩)
        // H 可以是任意值，C=0 时无意义
    }

    @Test
    fun `oklabToOklch pure black`() {
        val oklch = OklchConverter.oklabToOklch(0f, 0f, 0f)
        assertEquals(0f, oklch[0], 0.01f)
        assertEquals(0f, oklch[1], 0.01f)
    }

    @Test
    fun `oklch roundtrip preserves values`() {
        // 测试 L=0.7, C=0.15, H=240 (蓝色区域)
        val oklch = floatArrayOf(0.7f, 0.15f, 240f)
        val oklab = OklchConverter.oklchToOklab(oklch[0], oklch[1], oklch[2])
        val back = OklchConverter.oklabToOklch(oklab[0], oklab[1], oklab[2])
        assertEquals(oklch[0], back[0], 0.01f)
        assertEquals(oklch[1], back[1], 0.01f)
        assertEquals(oklch[2], back[2], 0.5f) // 色相允许微小误差
    }

    @Test
    fun `oklch hue wraps around 360`() {
        // H = 350 应该归一化到 0-360
        val lab = OklchConverter.oklchToOklab(0.5f, 0.2f, 350f)
        val oklch = OklchConverter.oklabToOklch(lab[0], lab[1], lab[2])
        assertTrue(oklch[2] in 0f..360f)
    }

    // ====== 完整 sRGB → OKLCH → sRGB 往返 ======

    @Test
    fun `srgb to oklch and back preserves pure red`() {
        val oklch = OklchConverter.srgbToOklch(1f, 0f, 0f)
        val rgb = OklchConverter.oklchToSrgb(oklch[0], oklch[1], oklch[2])
        assertEquals(1f, rgb[0], 0.01f)
        assertTrue(rgb[1] < 0.1f)
        assertTrue(rgb[2] < 0.1f)
    }

    @Test
    fun `srgb to oklch and back preserves pure green`() {
        val oklch = OklchConverter.srgbToOklch(0f, 1f, 0f)
        val rgb = OklchConverter.oklchToSrgb(oklch[0], oklch[1], oklch[2])
        assertTrue(rgb[0] < 0.1f)
        assertEquals(1f, rgb[1], 0.01f)
        assertTrue(rgb[2] < 0.1f)
    }

    @Test
    fun `srgb to oklch and back preserves pure blue`() {
        val oklch = OklchConverter.srgbToOklch(0f, 0f, 1f)
        val rgb = OklchConverter.oklchToSrgb(oklch[0], oklch[1], oklch[2])
        assertTrue(rgb[0] < 0.1f)
        assertTrue(rgb[1] < 0.1f)
        assertEquals(1f, rgb[2], 0.01f)
    }

    @Test
    fun `srgb to oklch and back preserves gray`() {
        for (gray in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val oklch = OklchConverter.srgbToOklch(gray, gray, gray)
            val rgb = OklchConverter.oklchToSrgb(oklch[0], oklch[1], oklch[2])
            assertEquals(gray, rgb[0], 0.02f)
            assertEquals(gray, rgb[1], 0.02f)
            assertEquals(gray, rgb[2], 0.02f)
        }
    }

    @Test
    fun `oklch hue progression is monotonic`() {
        // 红→黄→绿→青→蓝→紫→红，色相应单调递增
        val colors = listOf(
            floatArrayOf(1f, 0f, 0f),   // 红
            floatArrayOf(1f, 1f, 0f),   // 黄
            floatArrayOf(0f, 1f, 0f),   // 绿
            floatArrayOf(0f, 1f, 1f),   // 青
            floatArrayOf(0f, 0f, 1f),   // 蓝
            floatArrayOf(1f, 0f, 1f),   // 紫
        )
        val hues = colors.map { OklchConverter.srgbToOklch(it[0], it[1], it[2])[2] }
        for (i in 1 until hues.size) {
            assertTrue("Hue should increase: ${hues[i-1]} -> ${hues[i]}", hues[i] > hues[i-1])
        }
    }

    // ====== 边界条件 ======

    @Test
    fun `srgbToOklch handles negative inputs`() {
        // 负值在 sRGB 中不应该出现，但转换不应崩溃
        val result = OklchConverter.srgbToOklch(-0.1f, 0.5f, 0.5f)
        assertNotNull(result)
        assertEquals(3, result.size)
    }

    @Test
    fun `srgbToOklch handles values above 1`() {
        val result = OklchConverter.srgbToOklch(1.5f, 0.5f, 0.5f)
        assertNotNull(result)
        assertEquals(3, result.size)
    }

    @Test
    fun `oklchToSrgb clamps output to 0-1`() {
        // 极端 L 值应被钳制
        val rgb = OklchConverter.oklchToSrgb(2f, 0.5f, 180f)
        assertTrue(rgb[0] in 0f..1f)
        assertTrue(rgb[1] in 0f..1f)
        assertTrue(rgb[2] in 0f..1f)
    }

    @Test
    fun `linearRgbToOklab preserves black`() {
        val lab = OklchConverter.linearRgbToOklab(0f, 0f, 0f)
        assertEquals(0f, lab[0], 0.001f)
    }

    @Test
    fun `linearRgbToOklab preserves white`() {
        val lab = OklchConverter.linearRgbToOklab(1f, 1f, 1f)
        assertTrue(lab[0] > 0.9f) // L 接近 1
        assertTrue(kotlin.math.abs(lab[1]) < 0.01f) // a 接近 0
        assertTrue(kotlin.math.abs(lab[2]) < 0.01f) // b 接近 0
    }
}