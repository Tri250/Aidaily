package com.livecompose.livecapture.core.lut

import org.junit.Assert.*
import org.junit.Test

/**
 * LUT 预设单元测试
 *
 * 测试内置预设完整性、分类查询、预设参数有效性。
 */
class LutPresetTest {

    // ====== 内置预设数量 ======

    @Test
    fun `builtInPresets has at least 10 presets`() {
        assertTrue(BuiltInPresets.presets.size >= 10)
    }

    @Test
    fun `builtInPresets findById returns correct preset`() {
        val preset = BuiltInPresets.findById("portra400")
        assertNotNull(preset)
        assertEquals("Kodak Portra 400", preset!!.name)
        assertEquals(LutCategory.FILM, preset.category)
    }

    @Test
    fun `builtInPresets findById returns null for unknown id`() {
        assertNull(BuiltInPresets.findById("nonexistent"))
    }

    // ====== 分类查询 ======

    @Test
    fun `getByCategory film returns film presets`() {
        val film = BuiltInPresets.getByCategory(LutCategory.FILM)
        assertTrue(film.isNotEmpty())
        assertTrue(film.all { it.category == LutCategory.FILM })
    }

    @Test
    fun `getByCategory portrait returns portrait presets`() {
        val portrait = BuiltInPresets.getByCategory(LutCategory.PORTRAIT)
        assertTrue(portrait.isNotEmpty())
        assertTrue(portrait.all { it.category == LutCategory.PORTRAIT })
    }

    @Test
    fun `getByCategory landscape returns landscape presets`() {
        val landscape = BuiltInPresets.getByCategory(LutCategory.LANDSCAPE)
        assertTrue(landscape.isNotEmpty())
        assertTrue(landscape.all { it.category == LutCategory.LANDSCAPE })
    }

    @Test
    fun `getByCategory monochrome returns monochrome presets`() {
        val mono = BuiltInPresets.getByCategory(LutCategory.MONOCHROME)
        assertTrue(mono.isNotEmpty())
        assertTrue(mono.all { it.category == LutCategory.MONOCHROME })
    }

    @Test
    fun `getByCategory vintage returns vintage presets`() {
        val vintage = BuiltInPresets.getByCategory(LutCategory.VINTAGE)
        assertTrue(vintage.isNotEmpty())
        assertTrue(vintage.all { it.category == LutCategory.VINTAGE })
    }

    @Test
    fun `getByCategory standard returns standard presets`() {
        val standard = BuiltInPresets.getByCategory(LutCategory.STANDARD)
        assertTrue(standard.isNotEmpty())
        assertTrue(standard.all { it.category == LutCategory.STANDARD })
    }

    // ====== 预设参数有效性 ======

    @Test
    fun `all presets have saturation in 0-2 range`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue("${preset.name} saturation ${preset.saturation} out of range",
                preset.saturation in 0f..2f)
        }
    }

    @Test
    fun `all presets have contrast in 0-2 range`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue("${preset.name} contrast ${preset.contrast} out of range",
                preset.contrast in 0f..2f)
        }
    }

    @Test
    fun `all presets have warmth in -100 to 100 range`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue("${preset.name} warmth ${preset.warmth} out of range",
                preset.warmth in -100f..100f)
        }
    }

    @Test
    fun `all presets have fade in 0-1 range`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue("${preset.name} fade ${preset.fade} out of range",
                preset.fade in 0f..1f)
        }
    }

    @Test
    fun `all presets have grain in 0-1 range`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue("${preset.name} grain ${preset.grain} out of range",
                preset.grain in 0f..1f)
        }
    }

    @Test
    fun `all presets have vignette in 0-1 range`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue("${preset.name} vignette ${preset.vignette} out of range",
                preset.vignette in 0f..1f)
        }
    }

    @Test
    fun `all presets have sharpening in 0-1 range`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue("${preset.name} sharpening ${preset.sharpening} out of range",
                preset.sharpening in 0f..1f)
        }
    }

    @Test
    fun `all presets have exposure in -2 to 2 range`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue("${preset.name} exposure ${preset.exposure} out of range",
                preset.exposure in -2f..2f)
        }
    }

    // ====== 原始预设 ======

    @Test
    fun `original preset has all default values`() {
        val original = BuiltInPresets.findById("original")
        assertNotNull(original)
        assertEquals(1f, original!!.saturation)
        assertEquals(1f, original.contrast)
        assertEquals(0f, original.warmth)
        assertEquals(0f, original.tint)
        assertEquals(1f, original.highlights)
        assertEquals(1f, original.shadows)
        assertEquals(0f, original.fade)
        assertEquals(0f, original.grain)
        assertEquals(0f, original.vignette)
        assertEquals(0f, original.sharpening)
        assertEquals(0f, original.exposure)
    }

    // ====== 黑白预设 ======

    @Test
    fun `monochrome preset has zero saturation`() {
        val mono = BuiltInPresets.getByCategory(LutCategory.MONOCHROME)
        for (preset in mono) {
            assertEquals(0f, preset.saturation)
        }
    }

    // ====== LutCategory 测试 ======

    @Test
    fun `lutCategory has 6 categories`() {
        assertEquals(6, LutCategory.entries.size)
    }

    @Test
    fun `lutCategory has correct display names`() {
        assertEquals("标准", LutCategory.STANDARD.displayName)
        assertEquals("胶片", LutCategory.FILM.displayName)
        assertEquals("人像", LutCategory.PORTRAIT.displayName)
        assertEquals("风景", LutCategory.LANDSCAPE.displayName)
        assertEquals("黑白", LutCategory.MONOCHROME.displayName)
        assertEquals("复古", LutCategory.VINTAGE.displayName)
    }

    // ====== 预设 ID 唯一性 ======

    @Test
    fun `all preset ids are unique`() {
        val ids = BuiltInPresets.presets.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `all preset names are non-empty`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue(preset.name.isNotEmpty())
        }
    }

    @Test
    fun `all preset descriptions are non-empty`() {
        for (preset in BuiltInPresets.presets) {
            assertTrue(preset.description.isNotEmpty())
        }
    }
}