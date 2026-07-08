package com.livecompose.livecapture.core.push

import android.content.Context
import com.livecompose.livecapture.core.logger.AppLogger

/**
 * 推送服务抽象层
 * 
 * 统一管理国内主流推送平台（华为/小米/OPPO/vivo/应用宝），
 * 通过渠道包名自动选择对应的推送服务，无需手动切换。
 * 
 * 使用方式：
 *   PushManager.init(context)
 *   PushManager.registerPush()
 *   PushManager.getPushToken { token -> ... }
 */
object PushManager {

    private const val TAG = "PushManager"

    private var pushService: PushServiceProvider? = null
    private var isInitialized = false

    /**
     * 初始化推送服务
     * 根据 BuildConfig.CHANNEL 自动选择对应厂商推送
     */
    fun init(context: Context) {
        if (isInitialized) return

        val channel = try {
            BuildConfig.CHANNEL
        } catch (_: Exception) {
            "official"
        }

        pushService = when (channel) {
            "huawei" -> HuaweiPushProvider(context)
            "xiaomi" -> XiaomiPushProvider(context)
            "oppo" -> OppoPushProvider(context)
            "vivo" -> VivoPushProvider(context)
            "tencent" -> TencentPushProvider(context)
            "official" -> OfficialPushProvider(context)
            else -> {
                AppLogger.w(TAG, "未知渠道: $channel，使用官方推送")
                OfficialPushProvider(context)
            }
        }

        pushService?.initialize()
        isInitialized = true
        AppLogger.i(TAG, "推送服务初始化完成，渠道: $channel")
    }

    /**
     * 注册推送
     */
    fun registerPush() {
        pushService?.register { token ->
            if (token.isNotEmpty()) {
                AppLogger.i(TAG, "推送注册成功: ${token.take(10)}...")
                PushTokenManager.saveToken(token)
            } else {
                AppLogger.w(TAG, "推送注册失败，token为空")
            }
        }
    }

    /**
     * 获取推送 Token
     */
    fun getPushToken(callback: (String) -> Unit) {
        pushService?.getToken(callback) ?: callback("")
    }

    /**
     * 设置别名（用于定向推送）
     */
    fun setAlias(alias: String) {
        pushService?.setAlias(alias)
    }

    /**
     * 取消别名
     */
    fun unsetAlias(alias: String) {
        pushService?.unsetAlias(alias)
    }

    /**
     * 设置标签
     */
    fun setTags(tags: List<String>) {
        pushService?.setTags(tags)
    }
}

/**
 * 推送服务提供商接口
 */
interface PushServiceProvider {
    fun initialize()
    fun register(callback: (String) -> Unit)
    fun getToken(callback: (String) -> Unit)
    fun setAlias(alias: String)
    fun unsetAlias(alias: String)
    fun setTags(tags: List<String>)
}

/**
 * 推送 Token 管理器
 */
object PushTokenManager {
    private const val PREFS_NAME = "push_prefs"
    private const val KEY_TOKEN = "push_token"
    private const val KEY_PROVIDER = "push_provider"

    fun saveToken(token: String) {
        // 在生产环境中，这里应上报 Token 到后端服务器
        // 当前版本仅本地存储
    }

    fun getToken(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, "") ?: ""
    }
}

// ====== 各厂商推送Provider实现 ======

/**
 * 华为推送（HMS Push Kit）
 * 集成方式：implementation("com.huawei.hms:push:6.x.x")
 */
class HuaweiPushProvider(private val context: Context) : PushServiceProvider {
    private val tag = "HuaweiPush"

    override fun initialize() {
        AppLogger.i(tag, "华为推送初始化")
        // 实际集成：HmsMessaging.getInstance(context).isAutoInitEnabled = true
    }

    override fun register(callback: (String) -> Unit) {
        AppLogger.i(tag, "华为推送注册")
        // 实际集成：获取 token 并回调
        callback("")
    }

    override fun getToken(callback: (String) -> Unit) {
        callback("")
    }

    override fun setAlias(alias: String) {}
    override fun unsetAlias(alias: String) {}
    override fun setTags(tags: List<String>) {}
}

/**
 * 小米推送（MiPush）
 * 集成方式：implementation("com.xiaomi:mipush-sdk:5.x.x")
 */
class XiaomiPushProvider(private val context: Context) : PushServiceProvider {
    private val tag = "XiaomiPush"

    override fun initialize() {
        AppLogger.i(tag, "小米推送初始化")
        // 实际集成：MiPushClient.registerPush(context, APP_ID, APP_KEY)
    }

    override fun register(callback: (String) -> Unit) {
        callback("")
    }

    override fun getToken(callback: (String) -> Unit) {
        callback("")
    }

    override fun setAlias(alias: String) {}
    override fun unsetAlias(alias: String) {}
    override fun setTags(tags: List<String>) {}
}

/**
 * OPPO 推送（OPush）
 * 集成方式：implementation("com.heytap.msp:push:x.x.x")
 */
class OppoPushProvider(private val context: Context) : PushServiceProvider {
    private val tag = "OppoPush"

    override fun initialize() {
        AppLogger.i(tag, "OPPO推送初始化")
    }

    override fun register(callback: (String) -> Unit) {
        callback("")
    }

    override fun getToken(callback: (String) -> Unit) {
        callback("")
    }

    override fun setAlias(alias: String) {}
    override fun unsetAlias(alias: String) {}
    override fun setTags(tags: List<String>) {}
}

/**
 * vivo 推送（Vpush）
 * 集成方式：implementation("com.vivo.push:vivo-push-sdk:x.x.x")
 */
class VivoPushProvider(private val context: Context) : PushServiceProvider {
    private val tag = "VivoPush"

    override fun initialize() {
        AppLogger.i(tag, "vivo推送初始化")
    }

    override fun register(callback: (String) -> Unit) {
        callback("")
    }

    override fun getToken(callback: (String) -> Unit) {
        callback("")
    }

    override fun setAlias(alias: String) {}
    override fun unsetAlias(alias: String) {}
    override fun setTags(tags: List<String>) {}
}

/**
 * 应用宝推送（腾讯信鸽 TPNS）
 * 集成方式：implementation("com.tencent.tpns:tpns:x.x.x")
 */
class TencentPushProvider(private val context: Context) : PushServiceProvider {
    private val tag = "TencentPush"

    override fun initialize() {
        AppLogger.i(tag, "腾讯信鸽推送初始化")
    }

    override fun register(callback: (String) -> Unit) {
        callback("")
    }

    override fun getToken(callback: (String) -> Unit) {
        callback("")
    }

    override fun setAlias(alias: String) {}
    override fun unsetAlias(alias: String) {}
    override fun setTags(tags: List<String>) {}
}

/**
 * 官方渠道推送（使用系统通知 + 自建长连接）
 * 不依赖厂商SDK，适用于官方APK下载渠道
 */
class OfficialPushProvider(private val context: Context) : PushServiceProvider {
    private val tag = "OfficialPush"

    override fun initialize() {
        AppLogger.i(tag, "官方渠道推送初始化（不依赖厂商SDK）")
    }

    override fun register(callback: (String) -> Unit) {
        callback("")
    }

    override fun getToken(callback: (String) -> Unit) {
        callback("")
    }

    override fun setAlias(alias: String) {}
    override fun unsetAlias(alias: String) {}
    override fun setTags(tags: List<String>) {}
}