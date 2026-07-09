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

        // 1. 崩溃处理（本地日志）— 最先初始化
        try {
            CrashHandler.initialize(this)
            AppLogger.i(TAG, "[启动链路] 1/7 CrashHandler 初始化完成")
        } catch (e: Exception) {
            AppLogger.e(TAG, "崩溃处理器初始化失败", e)
        }

        // 2. ML Kit 手动初始化（禁用了自动 ContentProvider 以避免 Android 15 QPR2 崩溃）
        try {
            com.google.mlkit.common.MlKit.initialize(this)
            AppLogger.i(TAG, "[启动链路] 2/7 ML Kit 手动初始化完成")
        } catch (e: Throwable) {
            AppLogger.e(TAG, "ML Kit 初始化失败（不影响核心功能）", e)
        }

        // 3. Bugly 远程崩溃上报
        try {
            BuglyManager.init(this)
            // 设置渠道信息（配合多渠道包）
            try {
                val channel = try {
                    BuildConfig.CHANNEL
                } catch (_: Exception) {
                    "official"
                }
                BuglyManager.setChannel(channel ?: "official")
                BuglyManager.putCustomData("versionName", BuildConfig.VERSION_NAME)
                BuglyManager.putCustomData("versionCode", BuildConfig.VERSION_CODE.toString())
            } catch (_: Exception) {}
            AppLogger.i(TAG, "[启动链路] 3/7 Bugly 初始化完成")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Bugly 初始化失败", e)
        }

        // 4. 微信分享 SDK
        try {
            WeChatShareHelper.init(this)
            AppLogger.i(TAG, "[启动链路] 4/7 微信 SDK 初始化完成")
        } catch (e: Exception) {
            AppLogger.e(TAG, "微信 SDK 初始化失败", e)
        }

        // 5. 触觉反馈管理器
        try {
            HapticManager.init(this)
            AppLogger.i(TAG, "[启动链路] 5/7 触觉管理器初始化完成")
        } catch (e: Exception) {
            AppLogger.e(TAG, "触觉管理器初始化失败", e)
        }

        // 6. 推送服务（根据渠道自动选择厂商推送）
        try {
            PushManager.init(this)
            PushManager.registerPush()
            AppLogger.i(TAG, "[启动链路] 6/7 推送服务初始化完成")
        } catch (e: Exception) {
            AppLogger.e(TAG, "推送服务初始化失败", e)
        }

        // 7. 检查上次是否崩溃
        try {
            val crashHandler = CrashHandler.getInstance()
            if (crashHandler != null && crashHandler.wasLastSessionCrashed()) {
                val crashInfo = crashHandler.getLastCrashInfo()
                AppLogger.w(TAG, "上次会话发生崩溃: ${crashInfo?.formattedTime} - ${crashInfo?.exceptionMessage}")
                crashHandler.clearCrashState()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "崩溃状态检查失败", e)
        }

        AppLogger.i(TAG, "[启动链路] 7/7 LiveCapture v${BuildConfig.VERSION_NAME} (${BuildConfig.CHANNEL}) 启动完成")
    }
}
