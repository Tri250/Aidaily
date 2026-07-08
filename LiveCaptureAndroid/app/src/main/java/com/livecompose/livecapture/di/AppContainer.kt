package com.livecompose.livecapture.di

import android.content.Context
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.storage.PhotoStorageService

/**
 * 轻量级 DI 容器
 * 管理应用级单例对象，替代 Hilt 依赖注入
 */
class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    /** 相机管理器 */
    val cameraManager by lazy {
        CameraManager(applicationContext)
    }

    /** 运动稳定性监控器 */
    val motionMonitor by lazy {
        MotionStabilityMonitor(applicationContext)
    }

    /** 照片存储服务 */
    val photoStorageService by lazy {
        PhotoStorageService(applicationContext)
    }

    /**
     * 清理所有资源
     */
    fun destroy() {
        if (::cameraManager.isInitialized) {
            cameraManager.destroy()
        }
        if (::motionMonitor.isInitialized) {
            motionMonitor.stop()
        }
    }
}