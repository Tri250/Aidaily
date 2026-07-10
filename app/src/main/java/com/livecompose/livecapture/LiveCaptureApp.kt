package com.livecompose.livecapture

import android.app.Application
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LiveCaptureApp : Application() {

    @Inject
    lateinit var cameraManager: CameraManager

    @Inject
    lateinit var detectionEngine: AdacropInferenceEngine

    override fun onTerminate() {
        super.onTerminate()
        // 释放 Singleton 持有的 native 资源（TFLite Interpreter / NNAPI Delegate / Executor 线程池）
        cameraManager.shutdown()
        detectionEngine.close()
    }
}
