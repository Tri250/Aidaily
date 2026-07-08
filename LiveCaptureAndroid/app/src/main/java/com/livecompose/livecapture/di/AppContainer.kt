package com.livecompose.livecapture.di

import android.content.Context
import android.util.Log
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.storage.PhotoStorageService

/**
 * 轻量级 DI 容器
 * 管理应用级单例对象，替代 Hilt 依赖注入
 */
class AppContainer(context: Context) {

    companion object {
        private const val TAG = "AppContainer"
    }

    private val applicationContext = context.applicationContext ?: throw IllegalArgumentException("Application context is required")

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
        try {
            if (::photoStorageService.isInitialized) {
                // PhotoStorageService 的 CoroutineScope 会随 GC 回收，无需显式停止
            }
            if (::motionMonitor.isInitialized) {
                motionMonitor.stop()
            }
            if (::cameraManager.isInitialized) {
                cameraManager.destroy()
            }
        } catch (e: Exception) {
            Log.w(TAG, "清理资源时发生异常", e)
        }
    }
}