package com.livecompose.livecapture.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val DETECTION_MODE = stringPreferencesKey("detection_mode")
        val AUTO_CAPTURE = booleanPreferencesKey("auto_capture")
        val CAPTURE_DELAY = intPreferencesKey("capture_delay")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val TORCH_ENABLED = booleanPreferencesKey("torch_enabled")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val GRID_ENABLED = booleanPreferencesKey("grid_enabled")
        val VOICE_CAPTURE_DEFAULT = booleanPreferencesKey("voice_capture_default")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SCENE_RECOGNITION_ENABLED = booleanPreferencesKey("scene_recognition_enabled")
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
    }

    val detectionMode: Flow<DetectionMode> = context.dataStore.data.map {
        DetectionMode.fromValue(it[DETECTION_MODE])
    }
    val autoCapture: Flow<Boolean> = context.dataStore.data.map { it[AUTO_CAPTURE] ?: true }
    val captureDelay: Flow<Int> = context.dataStore.data.map { it[CAPTURE_DELAY] ?: 0 }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: true }
    val torchEnabled: Flow<Boolean> = context.dataStore.data.map { it[TORCH_ENABLED] ?: false }
    val watermarkEnabled: Flow<Boolean> = context.dataStore.data.map { it[WATERMARK_ENABLED] ?: true }
    val gridEnabled: Flow<Boolean> = context.dataStore.data.map { it[GRID_ENABLED] ?: true }
    val voiceCaptureDefault: Flow<Boolean> = context.dataStore.data.map { it[VOICE_CAPTURE_DEFAULT] ?: false }
    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_ENABLED] ?: true }
    val sceneRecognitionEnabled: Flow<Boolean> = context.dataStore.data.map { it[SCENE_RECOGNITION_ENABLED] ?: true }
    val aspectRatio: Flow<String> = context.dataStore.data.map { it[ASPECT_RATIO] ?: "3:4" }


    suspend fun setDetectionMode(mode: DetectionMode) {
        context.dataStore.edit { it[DETECTION_MODE] = mode.value }
    }

    suspend fun setAutoCapture(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_CAPTURE] = enabled }
    }

    suspend fun setCaptureDelay(delay: Int) {
        context.dataStore.edit { it[CAPTURE_DELAY] = delay }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = enabled }
    }

    suspend fun setTorchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TORCH_ENABLED] = enabled }
    }

    suspend fun setWatermarkEnabled(enabled: Boolean) {
        context.dataStore.edit { it[WATERMARK_ENABLED] = enabled }
    }

    suspend fun setGridEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GRID_ENABLED] = enabled }
    }

    suspend fun setVoiceCaptureDefault(enabled: Boolean) {
        context.dataStore.edit { it[VOICE_CAPTURE_DEFAULT] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[HAPTIC_ENABLED] = enabled }
    }

    suspend fun setSceneRecognitionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SCENE_RECOGNITION_ENABLED] = enabled }
    }

    suspend fun setAspectRatio(ratio: String) {
        context.dataStore.edit { it[ASPECT_RATIO] = ratio }
    }
}
