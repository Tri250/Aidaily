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
    }

    val detectionMode: Flow<String> = context.dataStore.data.map { it[DETECTION_MODE] ?: "FAST" }
    val autoCapture: Flow<Boolean> = context.dataStore.data.map { it[AUTO_CAPTURE] ?: true }
    val captureDelay: Flow<Int> = context.dataStore.data.map { it[CAPTURE_DELAY] ?: 0 }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: true }
    val torchEnabled: Flow<Boolean> = context.dataStore.data.map { it[TORCH_ENABLED] ?: false }

    suspend fun setDetectionMode(mode: String) {
        context.dataStore.edit { it[DETECTION_MODE] = mode }
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
}
