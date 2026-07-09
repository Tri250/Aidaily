package com.livecompose.livecapture.core.portrait

import org.junit.Assert.*
import org.junit.Test

/**
 * 美颜设置单元测试
 *
 * 测试 BeautySettings 数据类、预设映射、滑块描述符、
 * BeautyParams 转换以及边界条件。
 */
class BeautySettingsTest {

    // ====== BeautySettings 默认值 ======

    @Test
    fun `default beautySettings has all zero values`() {
        val settings = BeautySettings()
        assertEquals(0f, settings.skinSmoothing)
        assertEquals(0f, settings.skinTone)
        assertEquals(0f, settings.blemishRemoval)
        assertEquals(0f, settings.eyeBrightening)
        assertEquals(0f, settings.teethWhitening)
        assertEquals(0f, settings.faceSlimming)
        assertEquals(0f, settings.ruddy)
    }

    @Test
    fun `default beautySettings is off`() {
        assertTrue(BeautySettings().isOff)
        assertFalse(BeautySettings().isBeautyEnabled)
    }

    @Test
    fun `beautySettings with any nonzero value is not off`() {
        assertFalse(BeautySettings(skinSmoothing = 0.1f).isOff)
        assertFalse(BeautySettings(skinTone = 0.1f).isOff)
        assertFalse(BeautySettings(blemishRemoval = 0.1f).isOff)
        assertFalse(BeautySettings(eyeBrightening = 0.1f).isOff)
        assertFalse(BeautySettings(teethWhitening = 0.1f).isOff)
        assertFalse(BeautySettings(faceSlimming = 0.1f).isOff)
        assertFalse(BeautySettings(ruddy = 0.1f).isOff)
    }

    // ====== BeautyParams 转换 ======

    @Test
    fun `toBeautyParams preserves all values`() {
        val settings = BeautySettings(
            skinSmoothing = 0.5f,
            skinTone = 0.3f,
            blemishRemoval = 0.4f,
            eyeBrightening = 0.6f,
            teethWhitening = 0.2f,
            faceSlimming = 0.3f,
            ruddy = 0.1f
        )
        val params = settings.toBeautyParams()
        assertEquals(0.5f, params.skinSmoothing)
        assertEquals(0.3f, params.skinTone)
        assertEquals(0.4f, params.blemishRemoval)
        assertEquals(0.6f, params.eyeBrightening)
        assertEquals(0.2f, params.teethWhitening)
        assertEquals(0.3f, params.faceSlimming)
        assertEquals(0.1f, params.ruddy)
    }

    @Test
    fun `fromParams roundtrip`() {
        val params = BeautyParams(
            skinSmoothing = 0.5f,
            skinTone = 0.3f,
            eyeBrightening = 0.6f,
            teethWhitening = 0.4f,
            faceSlimming = 0.2f,
            blemishRemoval = 0.3f,
            ruddy = 0.1f
        )
        val settings = BeautySettings.fromParams(params)
        val back = settings.toBeautyParams()
        assertEquals(params.skinSmoothing, back.skinSmoothing)
        assertEquals(params.skinTone, back.skinTone)
        assertEquals(params.eyeBrightening, back.eyeBrightening)
        assertEquals(params.teethWhitening, back.teethWhitening)
        assertEquals(params.faceSlimming, back.faceSlimming)
        assertEquals(params.blemishRemoval, back.blemishRemoval)
        assertEquals(params.ruddy, back.ruddy)
    }

    // ====== 预设映射 ======

    @Test
    fun `natural preset is beauty disabled`() {
        val settings = BeautyPreset.NATURAL.toBeautySettings()
        assertFalse(settings.isBeautyEnabled)
        assertTrue(settings.isOff)
        assertEquals(BeautyPreset.NATURAL, settings.currentPreset)
    }

    @Test
    fun `delicate preset has beauty enabled`() {
        val settings = BeautyPreset.DELICATE.toBeautySettings()
        assertTrue(settings.isBeautyEnabled)
        assertFalse(settings.isOff)
        assertEquals(BeautyPreset.DELICATE, settings.currentPreset)
        assertEquals(0.4f, settings.skinSmoothing)
        assertEquals(0.2f, settings.skinTone)
    }

    @Test
    fun `goddess preset has higher values than delicate`() {
        val delicate = BeautyPreset.DELICATE.toBeautySettings()
        val goddess = BeautyPreset.GODDESS.toBeautySettings()
        assertTrue(goddess.skinSmoothing > delicate.skinSmoothing)
        assertTrue(goddess.skinTone > delicate.skinTone)
        assertTrue(goddess.eyeBrightening > delicate.eyeBrightening)
        assertTrue(goddess.faceSlimming > delicate.faceSlimming)
    }

    @Test
    fun `all beauty preset values are in valid range`() {
        for (preset in listOf(BeautyPreset.NATURAL, BeautyPreset.DELICATE, BeautyPreset.GODDESS, BeautyPreset.CUSTOM)) {
            val settings = preset.toBeautySettings()
            assertTrue(settings.skinSmoothing in 0f..1f)
            assertTrue(settings.skinTone in -1f..1f)
            assertTrue(settings.blemishRemoval in 0f..1f)
            assertTrue(settings.eyeBrightening in 0f..1f)
            assertTrue(settings.teethWhitening in 0f..1f)
            assertTrue(settings.faceSlimming in 0f..1f)
            assertTrue(settings.ruddy in 0f..1f)
        }
    }

    // ====== BeautySliders 测试 ======

    @Test
    fun `beautySliders has 7 sliders`() {
        assertEquals(7, BeautySliders.all.size)
    }

    @Test
    fun `beautySliders all keys are unique`() {
        val keys = BeautySliders.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `beautySliders getter and setter roundtrip`() {
        val settings = BeautySettings(
            skinSmoothing = 0.5f,
            skinTone = 0.3f
        )
        for (slider in BeautySliders.all) {
            val value = slider.getter(settings)
            val updated = slider.setter(settings, value)
            assertEquals(value, slider.getter(updated))
        }
    }

    @Test
    fun `beautySliders setter preserves other fields`() {
        val original = BeautySettings(
            skinSmoothing = 0.5f,
            skinTone = 0.3f
        )
        val smoothingSlider = BeautySliders.all.first { it.key == "smoothing" }
        val updated = smoothingSlider.setter(original, 0.8f)
        assertEquals(0.8f, updated.skinSmoothing)
        assertEquals(0.3f, updated.skinTone) // 其他字段不变
    }

    @Test
    fun `beautySliders labels are in Chinese`() {
        val labels = BeautySliders.all.map { it.label }
        assertEquals(listOf("磨皮", "美白", "祛痘", "亮眼", "牙齿美白", "瘦脸", "红润"), labels)
    }

    @Test
    fun `beautySliders rangeMin and rangeMax are correct`() {
        for (slider in BeautySliders.all) {
            if (slider.key == "tone") {
                assertEquals(-1f, slider.rangeMin)
                assertEquals(1f, slider.rangeMax)
            } else {
                assertEquals(0f, slider.rangeMin)
                assertEquals(1f, slider.rangeMax)
            }
        }
    }

    // ====== BeautySettings copy 操作 ======

    @Test
    fun `copy preserves unchanged fields`() {
        val original = BeautySettings(
            skinSmoothing = 0.5f,
            skinTone = 0.3f,
            blemishRemoval = 0.2f
        )
        val updated = original.copy(skinSmoothing = 0.8f)
        assertEquals(0.8f, updated.skinSmoothing)
        assertEquals(0.3f, updated.skinTone)
        assertEquals(0.2f, updated.blemishRemoval)
    }
}