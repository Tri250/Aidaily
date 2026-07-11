package com.livecompose.livecapture

import android.app.Application
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.crash.CrashHandler
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine
import com.livecompose.livecapture.core.diagnostics.SelfChecker
import com.livecompose.livecapture.core.perf.PerformanceMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LiveCaptureApp : Application() {

    @Inject
    lateinit var cameraManager: CameraManager

    @Inject
    lateinit var detectionEngine: AdacropInferenceEngine

    @Inject
    lateinit var selfChecker: SelfChecker

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var performanceMonitor: PerformanceMonitor

    companion object {
        @Volatile
        private var resourcesReleased = false
    }

    override fun onCreate() {
        super.onCreate()
        // 初始化崩溃处理器
        crashHandler.init()
        // 启动时执行全量自检，输出诊断日志
        selfChecker.runFullCheck()
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
