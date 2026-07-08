package com.livecompose.livecapture

import android.app.Application
import android.util.Log
import com.livecompose.livecapture.core.CrashHandler
import com.livecompose.livecapture.utilities.HapticManager

/**
 * Application 类
 * 对应 iOS 的 LiveCaptureApp
 */
class LiveCaptureApp : Application() {

    companion object {
        private const val TAG = "LiveCaptureApp"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            CrashHandler.initialize(this)
        } catch (e: Exception) {
            Log.e(TAG, "崩溃处理器初始化失败", e)
        }
        try {
            HapticManager.init(this)
        } catch (e: Exception) {
            Log.e(TAG, "触觉管理器初始化失败", e)
        }

        // 检查上次是否崩溃
        val crashHandler = CrashHandler.getInstance()
        if (crashHandler != null && crashHandler.wasLastSessionCrashed()) {
            val crashInfo = crashHandler.getLastCrashInfo()
            Log.w(TAG, "上次会话发生崩溃: ${crashInfo?.formattedTime} - ${crashInfo?.exceptionMessage}")
            crashHandler.clearCrashState()
            crashHandler.cleanOldCrashLogs()
        }
    }
}