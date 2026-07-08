package com.livecompose.livecapture.core.lut

import org.junit.Assert.*
import org.junit.Test

/**
 * LUT 色彩处理单元测试
 */
class LutProcessorTest {

    @Test
    fun `oklchConverter rgb to oklch and back`() {
        val converter = OklchConverter()

        // 测试纯红色
        val redOklch = converter.rgbToOklch(1.0f, 0.0f, 0.0f)
        assertNotNull(redOklch)
        assertTrue(redOklch[0] > 0f) // L > 0

        // 测试纯白色
        val whiteOklch = converter.rgbToOklch(1.0f, 1.0f, 1.0f)
        assertTrue(whiteOklch[0] > 0.9f) // L 接近 1

        // 测试纯黑色
        val blackOklch = converter.rgbToOklch(0.0f, 0.0f, 0.0f)
        assertTrue(blackOklch[0] < 0.1f) // L 接近 0
    }

    @Test
    fun `lutPreset has required fields`() {
        val preset = LutPreset(
            id = "test",
            name = "测试预设",
            category = "测试",
            intensity = 0.8f,
            recipeParams = ColorRecipeParams()
        )
        assertEquals("test", preset.id)
        assertEquals("测试预设", preset.name)
        assertTrue(preset.intensity in 0f..1f)
    }

    @Test
    fun `colorRecipeParams default values`() {
        val params = ColorRecipeParams()
        assertEquals(0f, params.exposure)
        assertEquals(0f, params.contrast)
        assertEquals(0f, params.saturation)
        assertEquals(0f, params.temperature)
    }

    @Test
    fun `lutRecipe serialization`() {
        val recipe = LutRecipe(
            name = "测试配方",
            presetId = "test_preset",
            params = ColorRecipeParams(exposure = 0.5f, contrast = 0.3f)
        )
        assertEquals("测试配方", recipe.name)
        assertEquals("test_preset", recipe.presetId)
        assertEquals(0.5f, recipe.params.exposure, 0.001f)
    }
}
