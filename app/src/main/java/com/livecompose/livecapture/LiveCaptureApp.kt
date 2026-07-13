package com.livecompose.livecapture

import android.app.Application
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine
import com.livecompose.livecapture.core.diagnostics.CrashHandler
import com.livecompose.livecapture.core.diagnostics.SelfChecker
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class LiveCaptureApp : Application() {

    @Inject
    lateinit var cameraManager: CameraManager

    @Inject
    lateinit var detectionEngine: AdacropInferenceEngine

    @Inject
    lateinit var selfChecker: SelfChecker

    companion object {
        @Volatile
        private var resourcesReleased = false
    }

    override fun onCreate() {
        super.onCreate()

        // 注册全局崩溃处理器，确保所有未捕获异常都被记录
        try {
            val crashLogDir = File(filesDir, "crash_logs")
            CrashHandler.register(crashLogDir)
        } catch (e: Exception) {
            // CrashHandler 注册失败不应阻止 App 启动
            android.util.Log.e("LiveCaptureApp", "Failed to register CrashHandler", e)
        }

        // 启动时执行全量自检，输出诊断日志
        // try-catch 保护：自检过程涉及 CameraManager/SensorManager 等系统服务，
        // 在部分设备上可能抛异常，绝不应阻塞 App 正常启动
        try {
            selfChecker.runFullCheck()
        } catch (e: Exception) {
            android.util.Log.e("LiveCaptureApp", "SelfChecker failed, app continues", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // NOTE: onTerminate() is NOT guaranteed to be called on real Android devices.
        // It is only invoked in emulated environments. Do NOT rely on this method
        // for releasing resources in production. Use releaseResources() instead.
        releaseResources()
    }

    /**
     * Releases native resources (TFLite Interpreter / NNAPI Delegate / Executor thread pools)
     * held by singletons. Safe to call from any thread and multiple times — subsequent
     * calls after the first are no-ops.
     */
    fun releaseResources() {
        if (resourcesReleased) return
        resourcesReleased = true

        try {
            cameraManager.shutdown()
        } catch (e: Exception) {
            // Swallow so detectionEngine.close() below still gets a chance to run
        }

        try {
            detectionEngine.close()
        } catch (e: Exception) {
            // Swallow; resources are best-effort released
        }
    }
}
