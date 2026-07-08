package com.livecompose.livecapture

import android.app.Application
import com.livecompose.livecapture.utilities.HapticManager

/**
 * Application 类
 * 对应 iOS 的 LiveCaptureApp
 */
class LiveCaptureApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HapticManager.init(this)
    }
}