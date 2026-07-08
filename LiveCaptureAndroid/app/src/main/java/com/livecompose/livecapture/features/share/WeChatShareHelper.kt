package com.livecompose.livecapture.features.share

import android.content.Context
import android.graphics.Bitmap
import com.livecompose.livecapture.core.logger.AppLogger
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXImageObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import java.io.ByteArrayOutputStream

/**
 * 微信分享助手
 * 集成微信 OpenSDK 实现照片分享至微信好友/朋友圈
 */
object WeChatShareHelper {

    private const val TAG = "WeChatShare"

    // 微信 AppID（从 BuildConfig 读取，在 build.gradle.kts 中配置）
    // 需在 open.weixin.qq.com 注册获取
    private val WECHAT_APP_ID: String by lazy {
        try {
            com.livecompose.livecapture.BuildConfig.WECHAT_APP_ID
        } catch (_: Exception) {
            AppLogger.w(TAG, "WECHAT_APP_ID 未在 BuildConfig 中配置，微信分享不可用")
            ""
        }
    }

    private var wxApi: IWXAPI? = null

    /**
     * 初始化微信 SDK
     * 应在 Application.onCreate() 中调用
     */
    fun init(context: Context) {
        if (WECHAT_APP_ID.isBlank()) {
            AppLogger.w(TAG, "微信 AppID 未配置，跳过初始化")
            return
        }
        try {
            wxApi = WXAPIFactory.createWXAPI(context, WECHAT_APP_ID, true)
            wxApi?.registerApp(WECHAT_APP_ID)
            AppLogger.i(TAG, "微信 SDK 已初始化")
        } catch (e: Exception) {
            AppLogger.e(TAG, "微信 SDK 初始化失败", e)
        }
    }

    /**
     * 检查微信是否已安装
     */
    fun isWeChatInstalled(context: Context): Boolean {
        return try {
            val api = wxApi ?: WXAPIFactory.createWXAPI(context, WECHAT_APP_ID, false)
            api.isWXAppInstalled
        } catch (e: Exception) {
            AppLogger.w(TAG, "检查微信安装状态失败", e)
            false
        }
    }

    /**
     * 分享图片到微信
     * @param bitmap 要分享的图片
     * @param scene 分享场景：好友(SESSION) 或 朋友圈(TIMELINE)
     */
    fun shareImage(
        bitmap: Bitmap,
        scene: Int = SendMessageToWX.Req.WXSceneSession
    ) {
        try {
            val api = wxApi ?: run {
                AppLogger.w(TAG, "微信 API 未初始化")
                return
            }

            // 压缩图片
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()

            // 创建图片对象
            val imageObject = WXImageObject(bitmap)

            // 创建媒体消息
            val message = WXMediaMessage()
            message.mediaObject = imageObject
            message.thumbData = compressThumb(bitmap)

            // 创建请求
            val request = SendMessageToWX.Req()
            request.transaction = "img_${System.currentTimeMillis()}"
            request.message = message
            request.scene = scene

            api.sendReq(request)
            AppLogger.i(TAG, "微信分享请求已发送")
        } catch (e: Exception) {
            AppLogger.e(TAG, "微信分享失败", e)
        }
    }

    /**
     * 分享网页链接到微信
     */
    fun shareWebPage(
        url: String,
        title: String,
        description: String,
        thumbBitmap: Bitmap? = null,
        scene: Int = SendMessageToWX.Req.WXSceneSession
    ) {
        try {
            val api = wxApi ?: return

            val webObject = WXWebpageObject()
            webObject.webpageUrl = url

            val message = WXMediaMessage()
            message.mediaObject = webObject
            message.title = title
            message.description = description
            if (thumbBitmap != null) {
                message.thumbData = compressThumb(thumbBitmap)
            }

            val request = SendMessageToWX.Req()
            request.transaction = "web_${System.currentTimeMillis()}"
            request.message = message
            request.scene = scene

            api.sendReq(request)
        } catch (e: Exception) {
            AppLogger.e(TAG, "微信网页分享失败", e)
        }
    }

    /**
     * 压缩缩略图至微信要求的 32KB 以内
     */
    private fun compressThumb(bitmap: Bitmap): ByteArray {
        var quality = 85
        var width = bitmap.width
        var height = bitmap.height

        // 先缩小尺寸
        if (width > 150 || height > 150) {
            val scale = 150f / maxOf(width, height)
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val outputStream = ByteArrayOutputStream()

        do {
            outputStream.reset()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() > 32768 && quality > 10)

        scaledBitmap.recycle()
        return outputStream.toByteArray()
    }

    /**
     * 处理微信回调
     * 需要在对应的 WXEntryActivity 中调用
     * @param intent 从 WXEntryActivity 传入的 Intent
     */
    fun handleIntent(intent: android.content.Intent): Boolean {
        return wxApi?.handleIntent(intent, null) ?: false
    }
}
