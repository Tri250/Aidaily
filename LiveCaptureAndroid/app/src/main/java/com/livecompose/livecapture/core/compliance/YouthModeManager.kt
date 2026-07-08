package com.livecompose.livecapture.core.compliance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Context.youthModeDataStore: DataStore<Preferences> by preferencesDataStore(name = "youth_mode_prefs")

private val ENABLED_KEY = booleanPreferencesKey("youth_mode_enabled")
private val TIME_LIMIT_KEY = intPreferencesKey("daily_time_limit")
private val NIGHT_START_KEY = intPreferencesKey("night_ban_start")
private val NIGHT_END_KEY = intPreferencesKey("night_ban_end")
private val COMMUNITY_DISABLED_KEY = booleanPreferencesKey("community_disabled")
private val SHARING_DISABLED_KEY = booleanPreferencesKey("sharing_disabled")
private val PASSWORD_KEY = stringPreferencesKey("youth_mode_password")
private val USAGE_MAP_KEY = stringPreferencesKey("today_usage_map")

/**
 * 青少年模式状态
 *
 * 不可变状态快照，包含所有可观察字段及派生计算属性。
 */
data class YouthModeState(
    val isYouthModeEnabled: Boolean = false,
    val dailyTimeLimitMinutes: Int = YouthModeManager.DEFAULT_DAILY_LIMIT_MINUTES,
    val todayUsageSeconds: Long = 0L,
    val nightBanStartHour: Int = YouthModeManager.DEFAULT_NIGHT_BAN_START,
    val nightBanEndHour: Int = YouthModeManager.DEFAULT_NIGHT_BAN_END,
    val isCommunityDisabled: Boolean = true,
    val isSharingDisabled: Boolean = true,
    val password: String = ""
) {
    /** 是否已设置密码 */
    val hasSetPassword: Boolean get() = password.isNotEmpty()

    /** 是否超过每日时长限制 */
    val isDailyLimitExceeded: Boolean
        get() = todayUsageSeconds >= dailyTimeLimitMinutes * 60L

    /** 今日剩余可用时长（秒） */
    val remainingSeconds: Long
        get() = (dailyTimeLimitMinutes * 60L - todayUsageSeconds).coerceAtLeast(0L)

    /** 是否处于夜间禁用时段 */
    val isInNightBanPeriod: Boolean
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return if (nightBanStartHour < nightBanEndHour) {
                hour >= nightBanStartHour && hour < nightBanEndHour
            } else {
                hour >= nightBanStartHour || hour < nightBanEndHour
            }
        }

    /** 是否因时长限制被锁定 */
    val isLockedByTimeLimit: Boolean get() = isYouthModeEnabled && isDailyLimitExceeded

    /** 是否因夜间禁用被锁定 */
    val isLockedByNightBan: Boolean get() = isYouthModeEnabled && isInNightBanPeriod

    /** 是否允许使用应用（综合判断） */
    val canUseApp: Boolean
        get() {
            if (!isYouthModeEnabled) return true
            if (isInNightBanPeriod) return false
            if (isDailyLimitExceeded) return false
            return true
        }

    /** 今日剩余可用时长（格式化字符串） */
    val remainingTimeFormatted: String
        get() {
            val minutes = (remainingSeconds / 60L).toInt()
            return if (minutes < 60) "$minutes 分钟"
            else "${minutes / 60} 小时 ${minutes % 60} 分钟"
        }

    /** 今日已使用时长（格式化字符串） */
    val todayUsageFormatted: String
        get() {
            val minutes = (todayUsageSeconds / 60L).toInt()
            return if (minutes < 60) "$minutes 分钟"
            else "${minutes / 60} 小时 ${minutes % 60} 分钟"
        }
}

/**
 * 单日使用记录
 */
data class UsageRecord(val date: String, val seconds: Long)

/**
 * 青少年模式管理器
 *
 * 负责管理青少年模式的所有状态与限制：使用时长限制、夜间禁用、内容过滤（社区/分享）、
 * 4 位数字密码保护、每日使用追踪与历史记录。使用 DataStore 持久化。
 *
 * 对应 iOS 端 YouthModeManager。
 *
 * @param context 应用上下文
 */
class YouthModeManager(private val context: Context) {

    companion object {
        private const val TAG = "YouthModeManager"
        /** 默认每日使用时长限制（分钟） */
        const val DEFAULT_DAILY_LIMIT_MINUTES = 40
        /** 默认夜间禁用开始时间（22 点） */
        const val DEFAULT_NIGHT_BAN_START = 22
        /** 默认夜间禁用结束时间（6 点） */
        const val DEFAULT_NIGHT_BAN_END = 6
        /** 密码长度 */
        const val PASSWORD_LENGTH = 4
        /** 使用时长追踪周期（毫秒） */
        private const val TRACKING_INTERVAL_MS = 60_000L
    }

    private val gson = Gson()
    private val store = context.youthModeDataStore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(YouthModeState())
    val state: StateFlow<YouthModeState> = _state.asStateFlow()

    /** 每日时长达到上限事件 */
    private val _timeLimitReached = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timeLimitReached: SharedFlow<Unit> = _timeLimitReached.asSharedFlow()

    private var trackingJob: Job? = null
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        scope.launch { loadAll() }
    }

    // MARK: - 加载与持久化

    private suspend fun loadAll() {
        val prefs = store.data.first()
        val todayUsage = loadTodayUsageSync(prefs)
        _state.value = YouthModeState(
            isYouthModeEnabled = prefs[ENABLED_KEY] ?: false,
            dailyTimeLimitMinutes = prefs[TIME_LIMIT_KEY] ?: DEFAULT_DAILY_LIMIT_MINUTES,
            nightBanStartHour = prefs[NIGHT_START_KEY] ?: DEFAULT_NIGHT_BAN_START,
            nightBanEndHour = prefs[NIGHT_END_KEY] ?: DEFAULT_NIGHT_BAN_END,
            isCommunityDisabled = prefs[COMMUNITY_DISABLED_KEY] ?: true,
            isSharingDisabled = prefs[SHARING_DISABLED_KEY] ?: true,
            password = prefs[PASSWORD_KEY] ?: "",
            todayUsageSeconds = todayUsage
        )
        if (_state.value.isYouthModeEnabled) startUsageTracking()
    }

    private fun loadTodayUsageSync(prefs: Preferences): Long {
        val json = prefs[USAGE_MAP_KEY] ?: return 0L
        return try {
            val type = object : TypeToken<Map<String, Long>>() {}.type
            val map: Map<String, Long> = gson.fromJson(json, type)
            map[今日日期()] ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun saveTodayUsage() {
        val map = readUsageMap()
        map[今日日期()] = _state.value.todayUsageSeconds
        store.edit { it[USAGE_MAP_KEY] = gson.toJson(map) }
    }

    private suspend fun readUsageMap(): MutableMap<String, Long> {
        val prefs = store.data.first()
        val raw = prefs[USAGE_MAP_KEY] ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, Long>>() {}.type
            gson.fromJson<MutableMap<String, Long>>(raw, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun 今日日期(): String = dateFormatter.format(Date())

    // MARK: - 模式开关

    /**
     * 设置青少年模式开关
     */
    fun setYouthModeEnabled(enabled: Boolean) {
        _state.update { it.copy(isYouthModeEnabled = enabled) }
        scope.launch {
            store.edit { it[ENABLED_KEY] = enabled }
            AppLogger.i(TAG, "青少年模式: ${if (enabled) "已开启" else "已关闭"}")
        }
        if (enabled) startUsageTracking() else stopUsageTracking()
    }

    /**
     * 通过密码切换青少年模式
     * @return 密码正确并切换成功返回 true
     */
    fun toggleYouthMode(password: String): Boolean {
        if (!verifyPassword(password)) return false
        setYouthModeEnabled(!_state.value.isYouthModeEnabled)
        return true
    }

    /**
     * 通过密码关闭青少年模式
     * @return 密码正确并关闭成功返回 true
     */
    fun disableWithPassword(password: String): Boolean {
        if (!verifyPassword(password)) return false
        setYouthModeEnabled(false)
        return true
    }

    // MARK: - 密码管理

    /** 是否已设置密码 */
    val hasSetPassword: Boolean get() = _state.value.hasSetPassword

    /**
     * 设置密码（应为 4 位数字）
     */
    fun setPassword(password: String) {
        _state.update { it.copy(password = password) }
        scope.launch {
            store.edit { it[PASSWORD_KEY] = password }
            AppLogger.i(TAG, "青少年模式密码已设置")
        }
    }

    /**
     * 验证密码
     */
    fun verifyPassword(password: String): Boolean = _state.value.password == password

    /** 校验密码格式（4 位数字） */
    fun isValidPasswordFormat(password: String): Boolean =
        password.length == PASSWORD_LENGTH && password.all(Char::isDigit)

    // MARK: - 时长与时段配置

    /** 设置每日使用时长限制（分钟） */
    fun setDailyTimeLimit(minutes: Int) {
        val safe = minutes.coerceAtLeast(1)
        _state.update { it.copy(dailyTimeLimitMinutes = safe) }
        scope.launch { store.edit { it[TIME_LIMIT_KEY] = safe } }
    }

    /** 设置夜间禁用时段 */
    fun setNightBanHours(startHour: Int, endHour: Int) {
        _state.update {
            it.copy(
                nightBanStartHour = startHour.coerceIn(0, 23),
                nightBanEndHour = endHour.coerceIn(0, 23)
            )
        }
        scope.launch {
            store.edit { p ->
                p[NIGHT_START_KEY] = startHour.coerceIn(0, 23)
                p[NIGHT_END_KEY] = endHour.coerceIn(0, 23)
            }
        }
    }

    // MARK: - 内容过滤

    /** 设置是否禁用社区功能 */
    fun setCommunityDisabled(disabled: Boolean) {
        _state.update { it.copy(isCommunityDisabled = disabled) }
        scope.launch { store.edit { it[COMMUNITY_DISABLED_KEY] = disabled } }
    }

    /** 设置是否禁用分享功能 */
    fun setSharingDisabled(disabled: Boolean) {
        _state.update { it.copy(isSharingDisabled = disabled) }
        scope.launch { store.edit { it[SHARING_DISABLED_KEY] = disabled } }
    }

    // MARK: - 使用时长追踪

    private fun startUsageTracking() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            while (isActive) {
                delay(TRACKING_INTERVAL_MS)
                if (!_state.value.isYouthModeEnabled) break
                _state.update { it.copy(todayUsageSeconds = it.todayUsageSeconds + 60L) }
                saveTodayUsage()
                if (_state.value.isDailyLimitExceeded) {
                    AppLogger.i(TAG, "青少年模式：今日使用时长已达上限")
                    _timeLimitReached.tryEmit(Unit)
                }
            }
        }
    }

    private fun stopUsageTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }

    // MARK: - 历史记录

    /**
     * 获取最近 7 天的使用记录（按日期升序）
     */
    suspend fun recentUsageHistory(): List<UsageRecord> {
        val map = readUsageMapAsync()
        val calendar = Calendar.getInstance()
        val today = calendar.time
        val todayKey = 今日日期()
        // 当日实时值优先
        val result = mutableListOf<UsageRecord>()
        repeat(7) { daysAgo ->
            calendar.time = today
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val key = dateFormatter.format(calendar.time)
            val seconds = if (key == todayKey) _state.value.todayUsageSeconds else (map[key] ?: 0L)
            result.add(UsageRecord(date = key, seconds = seconds))
        }
        return result.sortedBy { it.date }
    }

    private suspend fun readUsageMapAsync(): Map<String, Long> {
        val prefs = store.data.first()
        val raw = prefs[USAGE_MAP_KEY] ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, Long>>() {}.type
            gson.fromJson(raw, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 清除所有使用记录
     */
    fun clearUsageHistory() {
        _state.update { it.copy(todayUsageSeconds = 0L) }
        scope.launch { store.edit { it.remove(USAGE_MAP_KEY) } }
    }

    /**
     * 跨天时重置当日使用时长
     */
    suspend fun resetIfNewDay() {
        val map = readUsageMapAsync()
        if (map[今日日期()] == null) {
            _state.update { it.copy(todayUsageSeconds = 0L) }
        }
    }
}
