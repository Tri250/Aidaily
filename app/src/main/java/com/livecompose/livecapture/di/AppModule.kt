package com.livecompose.livecapture.di

import android.content.Context
import androidx.room.Room
import com.livecompose.livecapture.core.storage.AppDatabase
import com.livecompose.livecapture.core.storage.PhotoRecordDao
import com.livecompose.livecapture.core.update.UpdateChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI 模块
 * 各 Manager (CameraManager, AdacropInferenceEngine, MotionStabilityMonitor,
 * BoxCenterManager, PhotoStorageService, CrashHandler, PerformanceMonitor,
 * ShareService, SettingsRepository) 均使用 @Inject constructor + @Singleton 注解，
 * 由 Hilt 自动绑定，无需重复 @Provides。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "livecapture.db"
    ).build()

    @Provides
    fun providePhotoRecordDao(database: AppDatabase): PhotoRecordDao =
        database.photoRecordDao()

    @Provides
    @Singleton
    fun provideUpdateChecker(): UpdateChecker = UpdateChecker()
}
