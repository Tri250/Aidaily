package com.livecompose.livecapture.core.bugly

import android.app.Application
import android.util.Log
import com.tencent.bugly.crashreport.CrashReport

/**
 * Bugly 崩溃上报管理器
 * 国内生产环境必备，替代 Firebase Crashlytics
 */
object BuglyManager {

    private const val TAG = "BuglyManager"

    // Bugly AppID（需在 bugly.qq.com 注册获取）
    // 正式发布前替换为真实 AppID
    private const val BUGLY_APP_ID = "YOUR_BUGLY_APP_ID"

    private var isInitialized = false

    /**
     * 初始化 Bugly
     * 应在 Application.onCreate() 中调用
     */
    fun init(application: Application) {
        if (isInitialized) return
        try {
            val strategy = CrashReport.UserStrategy(application).apply {
                // 设置渠道（配合多渠道包）
                appChannel = "official"
                // 延迟初始化，避免启动卡顿
                appReportDelay = 3000
                // 开发阶段设置为测试设备，不上报
                isBuglyDev = BuildConfig.DEBUG
            }

            CrashReport.initCrashReport(application, BUGLY_APP_ID, BuildConfig.DEBUG, strategy)
            isInitialized = true
            Log.i(TAG, "Bugly 崩溃上报已初始化")
        } catch (e: Exception) {
            Log.e(TAG, "Bugly 初始化失败", e)
        }
    }

    /**
     * 设置用户标识
     */
    fun setUserId(userId: String) {
        try {
            CrashReport.setUserId(userId)
        } catch (e: Exception) {
            Log.w(TAG, "设置用户ID失败", e)
        }
    }

    /**
     * 设置用户标签
     */
    fun setUserTag(tag: Int) {
        try {
            CrashReport.setUserTag(tag)
        } catch (e: Exception) {
            Log.w(TAG, "设置用户标签失败", e)
        }
    }

    /**
     * 添加自定义数据
     */
    fun putCustomData(key: String, value: String) {
        try {
            CrashReport.putCustomData(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "添加自定义数据失败", e)
        }
    }

    /**
     * 手动上报异常
     */
    fun postException(throwable: Throwable) {
        try {
            CrashReport.postException(throwable)
        } catch (e: Exception) {
            Log.w(TAG, "上报异常失败", e)
        }
    }

    /**
     * 设置渠道信息（配合多渠道包使用）
     */
    fun setChannel(channel: String) {
        try {
            CrashReport.putCustomData("channel", channel)
        } catch (e: Exception) {
            Log.w(TAG, "设置渠道信息失败", e)
        }
    }
}
