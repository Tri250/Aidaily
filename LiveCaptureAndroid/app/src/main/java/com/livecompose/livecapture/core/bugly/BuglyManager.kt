package com.livecompose.livecapture.core.bugly

import android.app.Application
import com.livecompose.livecapture.core.logger.AppLogger
import com.tencent.bugly.crashreport.CrashReport

/**
 * Bugly 崩溃上报管理器
 * 国内生产环境必备，替代 Firebase Crashlytics
 */
object BuglyManager {

    private const val TAG = "BuglyManager"

    // Bugly AppID（从 BuildConfig 读取，在 build.gradle.kts 中配置）
    private val BUGLY_APP_ID: String by lazy {
        try {
            BuildConfig.BUGLY_APP_ID
        } catch (_: Exception) {
            AppLogger.w(TAG, "BUGLY_APP_ID 未在 BuildConfig 中配置，崩溃上报不可用")
            ""
        }
    }

    private var isInitialized = false

    /**
     * 初始化 Bugly
     * 应在 Application.onCreate() 中调用
     */
    fun init(application: Application) {
        if (isInitialized) return
        if (BUGLY_APP_ID.isBlank()) {
            AppLogger.w(TAG, "Bugly AppID 未配置，跳过初始化")
            return
        }
        try {
            val strategy = CrashReport.UserStrategy(application).apply {
                // 设置渠道（配合多渠道包）
                appChannel = "official"
                // 延迟初始化，避免启动卡顿
                appReportDelay = 3000
                // 开发阶段设置为测试设备，不上报
                setIsDevelopmentDevice(BuildConfig.DEBUG)
            }

            CrashReport.initCrashReport(application, BUGLY_APP_ID, BuildConfig.DEBUG, strategy)
            isInitialized = true
            AppLogger.i(TAG, "Bugly 崩溃上报已初始化")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Bugly 初始化失败", e)
        }
    }

    /**
     * 设置用户标识
     */
    fun setUserId(userId: String) {
        try {
            CrashReport.setUserId(userId)
        } catch (e: Exception) {
            AppLogger.w(TAG, "设置用户ID失败", e)
        }
    }

    /**
     * 设置用户标签
     */
    fun setUserTag(tag: Int) {
        try {
            CrashReport.setUserTag(tag)
        } catch (e: Exception) {
            AppLogger.w(TAG, "设置用户标签失败", e)
        }
    }

    /**
     * 添加自定义数据
     */
    fun putCustomData(key: String, value: String) {
        try {
            CrashReport.putCustomData(key, value)
        } catch (e: Exception) {
            AppLogger.w(TAG, "添加自定义数据失败", e)
        }
    }

    /**
     * 手动上报异常
     */
    fun postException(throwable: Throwable) {
        try {
            CrashReport.postException(throwable)
        } catch (e: Exception) {
            AppLogger.w(TAG, "上报异常失败", e)
        }
    }

    /**
     * 设置渠道信息（配合多渠道包使用）
     */
    fun setChannel(channel: String) {
        try {
            CrashReport.putCustomData("channel", channel)
        } catch (e: Exception) {
            AppLogger.w(TAG, "设置渠道信息失败", e)
        }
    }
}
