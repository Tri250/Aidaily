package com.livecompose.livecapture.di

import android.content.Context
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine
import com.livecompose.livecapture.core.motion.BoxCenterManager
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.storage.PhotoStorageService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCameraManager(
        @ApplicationContext context: Context
    ): CameraManager = CameraManager(context)

    @Provides
    @Singleton
    fun provideAdacropInferenceEngine(
        @ApplicationContext context: Context
    ): AdacropInferenceEngine = AdacropInferenceEngine(context)

    @Provides
    @Singleton
    fun provideMotionStabilityMonitor(
        @ApplicationContext context: Context
    ): MotionStabilityMonitor = MotionStabilityMonitor(context)

    @Provides
    @Singleton
    fun provideBoxCenterManager(): BoxCenterManager = BoxCenterManager()

    @Provides
    @Singleton
    fun providePhotoStorageService(
        @ApplicationContext context: Context
    ): PhotoStorageService = PhotoStorageService(context)
}
