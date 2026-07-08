package com.livecompose.livecapture.features.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.livecompose.livecapture.core.detection.DetectionMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

/**
 * 应用设置持久化管理
 * 使用 DataStore 存储所有设置项，替代 SettingsScreen 中的局部状态
 */
object SettingsDataStore {

    // Keys
    private val AUTO_CAPTURE_ENABLED = booleanPreferencesKey("auto_capture_enabled")
    private val CAPTURE_DELAY = floatPreferencesKey("capture_delay")
    private val DETECTION_MODE = stringPreferencesKey("detection_mode")
    private val GRID_MODE = intPreferencesKey("grid_mode")
    private val COLOR_SCHEME = stringPreferencesKey("color_scheme")
    private val NATURAL_LIGHT_ENABLED = booleanPreferencesKey("natural_light_enabled")
    private val BLOOM_ENABLED = booleanPreferencesKey("bloom_enabled")
    private val SOFT_GLOW_ENABLED = booleanPreferencesKey("soft_glow_enabled")
    private val QUICK_SHOT_ENABLED = booleanPreferencesKey("quick_shot_enabled")
    private val MULTI_FRAME_DENOISE = booleanPreferencesKey("multi_frame_denoise")
    private val HDR_FUSION_ENABLED = booleanPreferencesKey("hdr_fusion_enabled")
    private val MULTIPLE_EXPOSURE_COUNT = intPreferencesKey("multiple_exposure_count")
    private val HYPERFOCAL_DISPLAY = booleanPreferencesKey("hyperfocal_display")
    private val HEIC_EXPORT = booleanPreferencesKey("heic_export")
    private val RAW_CAPTURE_ENABLED = booleanPreferencesKey("raw_capture_enabled")
    private val AI_COLOR_MATCH = booleanPreferencesKey("ai_color_match")
    private val PHANTOM_LUT_ID = stringPreferencesKey("phantom_lut_id")
    private val PHANTOM_INTENSITY = floatPreferencesKey("phantom_intensity")
    private val PHANTOM_SAVE_AS_NEW = booleanPreferencesKey("phantom_save_as_new")

    // ---- Read ----

    suspend fun isAutoCaptureEnabled(context: Context): Boolean =
        context.settingsDataStore.data.map { it[AUTO_CAPTURE_ENABLED] ?: true }.first()

    suspend fun getCaptureDelay(context: Context): Float =
        context.settingsDataStore.data.map { it[CAPTURE_DELAY] ?: 1.0f }.first()

    suspend fun getDetectionMode(context: Context): DetectionMode =
        context.settingsDataStore.data.map {
            val name = it[DETECTION_MODE] ?: DetectionMode.FAST.name
            try { DetectionMode.valueOf(name) } catch (_: Exception) { DetectionMode.FAST }
        }.first()

    suspend fun getGridMode(context: Context): Int =
        context.settingsDataStore.data.map { it[GRID_MODE] ?: 0 }.first()

    suspend fun isBloomEnabled(context: Context): Boolean =
        context.settingsDataStore.data.map { it[BLOOM_ENABLED] ?: false }.first()

    suspend fun isSoftGlowEnabled(context: Context): Boolean =
        context.settingsDataStore.data.map { it[SOFT_GLOW_ENABLED] ?: false }.first()

    suspend fun isQuickShotEnabled(context: Context): Boolean =
        context.settingsDataStore.data.map { it[QUICK_SHOT_ENABLED] ?: false }.first()

    suspend fun isHdrFusionEnabled(context: Context): Boolean =
        context.settingsDataStore.data.map { it[HDR_FUSION_ENABLED] ?: false }.first()

    suspend fun getMultipleExposureCount(context: Context): Int =
        context.settingsDataStore.data.map { it[MULTIPLE_EXPOSURE_COUNT] ?: 1 }.first()

    suspend fun isRawCaptureEnabled(context: Context): Boolean =
        context.settingsDataStore.data.map { it[RAW_CAPTURE_ENABLED] ?: false }.first()

    suspend fun getPhantomLutId(context: Context): String =
        context.settingsDataStore.data.map { it[PHANTOM_LUT_ID] ?: "" }.first()

    suspend fun getPhantomIntensity(context: Context): Float =
        context.settingsDataStore.data.map { it[PHANTOM_INTENSITY] ?: 0.8f }.first()

    // ---- Write ----

    suspend fun setAutoCaptureEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_CAPTURE_ENABLED] = enabled }
    }

    suspend fun setCaptureDelay(context: Context, delay: Float) {
        context.settingsDataStore.edit { it[CAPTURE_DELAY] = delay }
    }

    suspend fun setDetectionMode(context: Context, mode: DetectionMode) {
        context.settingsDataStore.edit { it[DETECTION_MODE] = mode.name }
    }

    suspend fun setGridMode(context: Context, mode: Int) {
        context.settingsDataStore.edit { it[GRID_MODE] = mode }
    }

    suspend fun setBloomEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[BLOOM_ENABLED] = enabled }
    }

    suspend fun setSoftGlowEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[SOFT_GLOW_ENABLED] = enabled }
    }

    suspend fun setQuickShotEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[QUICK_SHOT_ENABLED] = enabled }
    }

    suspend fun setHdrFusionEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[HDR_FUSION_ENABLED] = enabled }
    }

    suspend fun setMultipleExposureCount(context: Context, count: Int) {
        context.settingsDataStore.edit { it[MULTIPLE_EXPOSURE_COUNT] = count }
    }

    suspend fun setRawCaptureEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[RAW_CAPTURE_ENABLED] = enabled }
    }

    suspend fun setPhantomLutId(context: Context, lutId: String) {
        context.settingsDataStore.edit { it[PHANTOM_LUT_ID] = lutId }
    }

    suspend fun setPhantomIntensity(context: Context, intensity: Float) {
        context.settingsDataStore.edit { it[PHANTOM_INTENSITY] = intensity }
    }
}
