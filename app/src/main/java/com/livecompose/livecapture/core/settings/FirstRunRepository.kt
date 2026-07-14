package com.livecompose.livecapture.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "first_run_prefs")

/**
 * 首次启动状态管理
 * 用于判断是否需要显示引导页
 */
@Singleton
class FirstRunRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    /**
     * 是否已完成引导
     */
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    /**
     * 标记引导已完成
     */
    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
            }
        }
    }
}