package com.livecompose.livecapture.di

import android.content.Context
import com.livecompose.livecapture.core.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI 模块
 * 各 Manager (CameraManager, AdacropInferenceEngine, MotionStabilityMonitor,
 * BoxCenterManager, PhotoStorageService) 均使用 @Inject constructor + @Singleton 注解，
 * 由 Hilt 自动绑定，无需重复 @Provides。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(context)
}
