package com.livecompose.livecapture.core.sharecard

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

/**
 * 分享卡片样式单元测试
 *
 * 测试 ShareCardStyle 四种预设样式、CardTheme、WatermarkPosition 枚举。
 */
class ShareCardStyleTest {

    // ====== 预设样式数量 ======

    @Test
    fun `all presets has 4 styles`() {
        assertEquals(4, ShareCardStyle.all.size)
    }

    @Test
    fun `all preset ids are unique`() {
        val ids = ShareCardStyle.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    // ====== 极简风格 ======

    @Test
    fun `minimal style has white background`() {
        assertEquals(Color.WHITE, ShareCardStyle.Minimal.backgroundColor)
    }

    @Test
    fun `minimal style has black title`() {
        assertEquals(Color.BLACK, ShareCardStyle.Minimal.titleColor)
    }

    @Test
    fun `minimal style has minimal theme`() {
        assertEquals(ShareCardStyle.CardTheme.MINIMAL, ShareCardStyle.Minimal.cardTheme)
    }

    @Test
    fun `minimal style watermark at bottom center`() {
        assertEquals(ShareCardStyle.WatermarkPosition.BOTTOM_CENTER,
            ShareCardStyle.Minimal.watermarkPosition)
    }

    // ====== 胶片风格 ======

    @Test
    fun `film style has dark background`() {
        val bg = ShareCardStyle.Film.backgroundColor
        // 深色背景：RGB 值应较小
        val r = (bg shr 16) and 0xFF
        val g = (bg shr 8) and 0xFF
        val b = bg and 0xFF
        assertTrue("Film background should be dark, r=$r", r < 100)
    }

    @Test
    fun `film style has white title`() {
        assertEquals(Color.WHITE, ShareCardStyle.Film.titleColor)
    }

    @Test
    fun `film style has film theme`() {
        assertEquals(ShareCardStyle.CardTheme.FILM, ShareCardStyle.Film.cardTheme)
    }

    // ====== 杂志风格 ======

    @Test
    fun `magazine style has warm background`() {
        assertEquals(0xFFFAF5EB.toInt(), ShareCardStyle.Magazine.backgroundColor)
    }

    @Test
    fun `magazine style has red accent`() {
        val accent = ShareCardStyle.Magazine.accentColor
        // 红色强调色：RGB 中 R 应较大
        val r = (accent shr 16) and 0xFF
        assertTrue("Magazine accent should be reddish, r=$r", r > 100)
    }

    @Test
    fun `magazine style has larger top padding`() {
        assertTrue(ShareCardStyle.Magazine.topPadding > 100f)
    }

    // ====== 拍立得风格 ======

    @Test
    fun `polaroid style has small corner radius`() {
        assertEquals(8f, ShareCardStyle.Polaroid.cardCornerRadius)
    }

    @Test
    fun `polaroid style has polaroid theme`() {
        assertEquals(ShareCardStyle.CardTheme.POLAROID, ShareCardStyle.Polaroid.cardTheme)
    }

    @Test
    fun `polaroid style has small bottom reserved`() {
        assertEquals(100f, ShareCardStyle.Polaroid.bottomReserved)
    }

    // ====== CardTheme 枚举 ======

    @Test
    fun `cardTheme has 4 themes`() {
        assertEquals(4, ShareCardStyle.CardTheme.entries.size)
    }

    @Test
    fun `cardTheme has expected values`() {
        val themes = ShareCardStyle.CardTheme.entries
        assertTrue(themes.contains(ShareCardStyle.CardTheme.MINIMAL))
        assertTrue(themes.contains(ShareCardStyle.CardTheme.FILM))
        assertTrue(themes.contains(ShareCardStyle.CardTheme.MAGAZINE))
        assertTrue(themes.contains(ShareCardStyle.CardTheme.POLAROID))
    }

    // ====== WatermarkPosition 枚举 ======

    @Test
    fun `watermarkPosition has 3 positions`() {
        assertEquals(3, ShareCardStyle.WatermarkPosition.entries.size)
    }

    @Test
    fun `watermarkPosition has expected values`() {
        val positions = ShareCardStyle.WatermarkPosition.entries
        assertTrue(positions.contains(ShareCardStyle.WatermarkPosition.TOP))
        assertTrue(positions.contains(ShareCardStyle.WatermarkPosition.BOTTOM))
        assertTrue(positions.contains(ShareCardStyle.WatermarkPosition.BOTTOM_CENTER))
    }

    // ====== 样式一致性 ======

    @Test
    fun `all styles have non-empty display name`() {
        for (style in ShareCardStyle.all) {
            assertTrue(style.displayName.isNotEmpty())
        }
    }

    @Test
    fun `all styles have valid corner radius`() {
        for (style in ShareCardStyle.all) {
            assertTrue(style.cardCornerRadius > 0f)
            assertTrue(style.photoCornerRadius >= 0f)
        }
    }

    @Test
    fun `all styles have valid padding`() {
        for (style in ShareCardStyle.all) {
            assertTrue(style.horizontalPadding >= 0f)
            assertTrue(style.topPadding >= 0f)
            assertTrue(style.bottomReserved >= 0f)
        }
    }

    @Test
    fun `display names are Chinese`() {
        val names = ShareCardStyle.all.map { it.displayName }
        assertEquals(listOf("极简", "胶片", "杂志", "拍立得"), names)
    }
}