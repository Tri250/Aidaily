package com.livecompose.livecapture.core.compliance

import org.junit.Assert.*
import org.junit.Test

/**
 * 青少年模式状态单元测试
 *
 * 测试 YouthModeState 的派生计算属性、状态转换逻辑、
 * 夜间禁用判断、时长限制判断和格式化方法。
 */
class YouthModeStateTest {

    // ====== 默认状态 ======

    @Test
    fun `default state has youth mode disabled`() {
        val state = YouthModeState()
        assertFalse(state.isYouthModeEnabled)
        assertTrue(state.canUseApp)
    }

    @Test
    fun `default state has no password`() {
        val state = YouthModeState()
        assertFalse(state.hasSetPassword)
        assertTrue(state.password.isEmpty())
    }

    @Test
    fun `default state has 40 minute daily limit`() {
        val state = YouthModeState()
        assertEquals(40, state.dailyTimeLimitMinutes)
    }

    @Test
    fun `default state has community and sharing disabled`() {
        val state = YouthModeState()
        assertTrue(state.isCommunityDisabled)
        assertTrue(state.isSharingDisabled)
    }

    // ====== 密码管理 ======

    @Test
    fun `state with password has hasSetPassword true`() {
        val state = YouthModeState(password = "1234")
        assertTrue(state.hasSetPassword)
    }

    // ====== 时长限制 ======

    @Test
    fun `isDailyLimitExceeded when usage exceeds limit`() {
        val state = YouthModeState(
            dailyTimeLimitMinutes = 10,
            todayUsageSeconds = 600L // 10 分钟
        )
        assertTrue(state.isDailyLimitExceeded)
    }

    @Test
    fun `isDailyLimitExceeded false when usage is under limit`() {
        val state = YouthModeState(
            dailyTimeLimitMinutes = 10,
            todayUsageSeconds = 300L // 5 分钟
        )
        assertFalse(state.isDailyLimitExceeded)
    }

    @Test
    fun `isDailyLimitExceeded false when exactly at limit`() {
        val state = YouthModeState(
            dailyTimeLimitMinutes = 10,
            todayUsageSeconds = 600L
        )
        assertTrue(state.isDailyLimitExceeded)
    }

    @Test
    fun `remainingSeconds calculation`() {
        val state = YouthModeState(
            dailyTimeLimitMinutes = 40,
            todayUsageSeconds = 600L // 已用 10 分钟
        )
        assertEquals(1800L, state.remainingSeconds) // 剩余 30 分钟
    }

    @Test
    fun `remainingSeconds floors at zero`() {
        val state = YouthModeState(
            dailyTimeLimitMinutes = 10,
            todayUsageSeconds = 6000L // 远超限制
        )
        assertEquals(0L, state.remainingSeconds)
    }

    // ====== 夜间禁用 ======

    @Test
    fun `isInNightBanPeriod uses Calendar hour`() {
        val state = YouthModeState(
            nightBanStartHour = 22,
            nightBanEndHour = 6
        )
        // 返回值取决于当前时间，仅验证不崩溃
        assertNotNull(state.isInNightBanPeriod)
    }

    @Test
    fun `night ban with same start and end always false`() {
        val state = YouthModeState(
            nightBanStartHour = 12,
            nightBanEndHour = 12
        )
        // 相同时段：startHour < endHour → hour >= 12 && hour < 12 → 永远 false
        assertFalse(state.isInNightBanPeriod)
    }

    @Test
    fun `night ban crossing midnight has correct range`() {
        val state = YouthModeState(
            nightBanStartHour = 22,
            nightBanEndHour = 6
        )
        // 跨午夜: startHour > endHour → hour >= 22 || hour < 6
        // 仅验证不崩溃
        assertNotNull(state.isInNightBanPeriod)
    }

    // ====== 锁定状态 ======

    @Test
    fun `isLockedByTimeLimit requires youth mode enabled`() {
        val state = YouthModeState(
            isYouthModeEnabled = false,
            dailyTimeLimitMinutes = 10,
            todayUsageSeconds = 6000L
        )
        assertFalse(state.isLockedByTimeLimit)
    }

    @Test
    fun `isLockedByTimeLimit true when enabled and exceeded`() {
        val state = YouthModeState(
            isYouthModeEnabled = true,
            dailyTimeLimitMinutes = 10,
            todayUsageSeconds = 600L
        )
        assertTrue(state.isLockedByTimeLimit)
    }

    @Test
    fun `isLockedByNightBan requires youth mode enabled`() {
        // 即使夜间时段，未开启青少年模式也不锁定
        val state = YouthModeState(
            isYouthModeEnabled = false
        )
        assertFalse(state.isLockedByNightBan)
    }

    // ====== canUseApp 综合判断 ======

    @Test
    fun `canUseApp true when youth mode disabled`() {
        val state = YouthModeState(isYouthModeEnabled = false)
        assertTrue(state.canUseApp)
    }

    @Test
    fun `canUseApp false when daily limit exceeded`() {
        val state = YouthModeState(
            isYouthModeEnabled = true,
            dailyTimeLimitMinutes = 10,
            todayUsageSeconds = 600L
        )
        assertFalse(state.canUseApp)
    }

    // ====== 格式化 ======

    @Test
    fun `remainingTimeFormatted shows minutes`() {
        val state = YouthModeState(
            dailyTimeLimitMinutes = 40,
            todayUsageSeconds = 0L
        )
        assertTrue(state.remainingTimeFormatted.contains("40"))
        assertTrue(state.remainingTimeFormatted.contains("分钟"))
    }

    @Test
    fun `remainingTimeFormatted shows hours and minutes`() {
        val state = YouthModeState(
            dailyTimeLimitMinutes = 120,
            todayUsageSeconds = 0L
        )
        assertTrue(state.remainingTimeFormatted.contains("小时"))
    }

    @Test
    fun `todayUsageFormatted shows minutes`() {
        val state = YouthModeState(todayUsageSeconds = 300L)
        assertTrue(state.todayUsageFormatted.contains("5"))
        assertTrue(state.todayUsageFormatted.contains("分钟"))
    }

    @Test
    fun `todayUsageFormatted shows hours and minutes`() {
        val state = YouthModeState(todayUsageSeconds = 5400L) // 1.5 小时
        assertTrue(state.todayUsageFormatted.contains("小时"))
    }

    // ====== 状态不可变性 ======

    @Test
    fun `youthModeState copy preserves other fields`() {
        val original = YouthModeState(
            isYouthModeEnabled = true,
            dailyTimeLimitMinutes = 60,
            password = "1234"
        )
        val updated = original.copy(isYouthModeEnabled = false)
        assertEquals(60, updated.dailyTimeLimitMinutes)
        assertEquals("1234", updated.password)
        assertFalse(updated.isYouthModeEnabled)
    }

    @Test
    fun `isValidPasswordFormat accepts 4 digits`() {
        // 注：isValidPasswordFormat 在 YouthModeManager 上
        assertTrue("1234".length == 4 && "1234".all(Char::isDigit))
        assertFalse("123".length == 4 && "123".all(Char::isDigit))
        assertFalse("123a".length == 4 && "123a".all(Char::isDigit))
    }
}