package com.livecompose.livecapture.core.community

import org.junit.Assert.*
import org.junit.Test

/**
 * 社区模型单元测试
 *
 * 测试 GeoCoordinate、PhotoLocation、ChallengeTheme、PhotoChallenge、
 * 社区滤镜、社区帖子等数据模型。
 */
class CommunityModelsTest {

    // ====== GeoCoordinate 测试 ======

    @Test
    fun `geoCoordinate stores lat and lng`() {
        val coord = GeoCoordinate(39.9042, 116.4074)
        assertEquals(39.9042, coord.latitude, 0.0001)
        assertEquals(116.4074, coord.longitude, 0.0001)
    }

    @Test
    fun `geoCoordinate equals and hashCode`() {
        val a = GeoCoordinate(39.9, 116.4)
        val b = GeoCoordinate(39.9, 116.4)
        val c = GeoCoordinate(40.0, 116.5)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }

    // ====== PhotoDifficulty ======

    @Test
    fun `photoDifficulty has 3 levels`() {
        assertEquals(3, PhotoDifficulty.entries.size)
    }

    @Test
    fun `photoDifficulty display names are Chinese`() {
        assertEquals("轻松", PhotoDifficulty.EASY.displayName)
        assertEquals("中等", PhotoDifficulty.MEDIUM.displayName)
        assertEquals("挑战", PhotoDifficulty.HARD.displayName)
    }

    @Test
    fun `photoDifficulty fromName is case insensitive`() {
        assertEquals(PhotoDifficulty.EASY, PhotoDifficulty.fromName("easy"))
        assertEquals(PhotoDifficulty.EASY, PhotoDifficulty.fromName("EASY"))
        assertEquals(PhotoDifficulty.HARD, PhotoDifficulty.fromName("hard"))
    }

    @Test
    fun `photoDifficulty fromName unknown returns EASY`() {
        assertEquals(PhotoDifficulty.EASY, PhotoDifficulty.fromName("unknown"))
        assertEquals(PhotoDifficulty.EASY, PhotoDifficulty.fromName(""))
    }

    // ====== PhotoLocation ======

    @Test
    fun `photoLocation has all required fields`() {
        val location = PhotoLocation(
            id = "loc_001",
            name = "故宫角楼",
            description = "北京故宫角楼拍摄点",
            coordinate = GeoCoordinate(39.9108, 116.3928),
            bestTime = "傍晚日落时分",
            tags = listOf("古建筑", "长城", "日落"),
            difficulty = PhotoDifficulty.EASY
        )
        assertEquals("loc_001", location.id)
        assertEquals("故宫角楼", location.name)
        assertEquals(3, location.tags.size)
        assertEquals(PhotoDifficulty.EASY, location.difficulty)
        assertNull(location.samplePhotoName)
    }

    // ====== ChallengeTheme ======

    @Test
    fun `challengeTheme has at least 10 themes`() {
        assertTrue(ChallengeTheme.entries.size >= 10)
    }

    @Test
    fun `challengeTheme has common themes`() {
        val names = ChallengeTheme.entries.map { it.displayName }
        assertTrue(names.contains("人像"))
        assertTrue(names.contains("风光"))
        assertTrue(names.contains("美食"))
        assertTrue(names.contains("街拍"))
        assertTrue(names.contains("夜景"))
    }

    @Test
    fun `challengeTheme has icon names`() {
        for (theme in ChallengeTheme.entries) {
            assertTrue("${theme.displayName} should have icon name",
                theme.iconName.isNotEmpty())
        }
    }
}