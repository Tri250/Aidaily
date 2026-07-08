package com.livecompose.livecapture

import android.app.Application
import com.livecompose.livecapture.core.CrashHandler
import com.livecompose.livecapture.core.bugly.BuglyManager
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.push.PushManager
import com.livecompose.livecapture.features.share.WeChatShareHelper
import com.livecompose.livecapture.utilities.HapticManager

/**
 * Application 类
 * 初始化全局组件：崩溃处理、Bugly、微信SDK、触觉反馈
 */
class LiveCaptureApp : Application() {

    companion object {
        private const val TAG = "LiveCaptureApp"
        @JvmStatic lateinit var instance: LiveCaptureApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. 崩溃处理（本地日志）
        try {
            CrashHandler.initialize(this)
        } catch (e: Exception) {
            AppLogger.e(TAG, "崩溃处理器初始化失败", e)
        }

        // 2. Bugly 远程崩溃上报
        try {
            BuglyManager.init(this)
            // 设置渠道信息（配合多渠道包）
            try {
                val channel = BuildConfig.CHANNEL ?: "official"
                BuglyManager.setChannel(channel)
                BuglyManager.putCustomData("versionName", BuildConfig.VERSION_NAME)
                BuglyManager.putCustomData("versionCode", BuildConfig.VERSION_CODE.toString())
            } catch (_: Exception) {}
        } catch (e: Exception) {
            AppLogger.e(TAG, "Bugly 初始化失败", e)
        }

        // 3. 微信分享 SDK
        try {
            WeChatShareHelper.init(this)
        } catch (e: Exception) {
            AppLogger.e(TAG, "微信 SDK 初始化失败", e)
        }

        // 4. 触觉反馈管理器
        try {
            HapticManager.init(this)
        } catch (e: Exception) {
            AppLogger.e(TAG, "触觉管理器初始化失败", e)
        }

        // 5. 推送服务（根据渠道自动选择厂商推送）
        try {
            PushManager.init(this)
            PushManager.registerPush()
        } catch (e: Exception) {
            AppLogger.e(TAG, "推送服务初始化失败", e)
        }

        // 检查上次是否崩溃
        val crashHandler = CrashHandler.getInstance()
        if (crashHandler != null && crashHandler.wasLastSessionCrashed()) {
            val crashInfo = crashHandler.getLastCrashInfo()
            AppLogger.w(TAG, "上次会话发生崩溃: ${crashInfo?.formattedTime} - ${crashInfo?.exceptionMessage}")
            crashHandler.clearCrashState()
            crashHandler.cleanOldCrashLogs()
        }

        AppLogger.i(TAG, "LiveCapture v${BuildConfig.VERSION_NAME} (${BuildConfig.CHANNEL}) 启动完成")
    }
}
