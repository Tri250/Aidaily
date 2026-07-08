package com.livecompose.livecapture.features.share

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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

    // 微信 AppID（需在 open.weixin.qq.com 注册获取）
    // 正式发布前替换为真实 AppID
    private const val WECHAT_APP_ID = "YOUR_WECHAT_APP_ID"

    private var wxApi: IWXAPI? = null

    /**
     * 初始化微信 SDK
     * 应在 Application.onCreate() 中调用
     */
    fun init(context: Context) {
        try {
            wxApi = WXAPIFactory.createWXAPI(context, WECHAT_APP_ID, true)
            wxApi?.registerApp(WECHAT_APP_ID)
            Log.i(TAG, "微信 SDK 已初始化")
        } catch (e: Exception) {
            Log.e(TAG, "微信 SDK 初始化失败", e)
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
            Log.w(TAG, "检查微信安装状态失败", e)
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
                Log.w(TAG, "微信 API 未初始化")
                return
            }

            // 压缩图片
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val thumbData = outputStream.toByteArray()
            outputStream.close()

            // 创建图片对象
            val imageObject = WXImageObject(bitmap)

            // 创建媒体消息
            val message = WXMediaMessage().apply {
                mediaObject = imageObject
                // 缩略图（微信要求不超过 32KB）
                val thumbBmp = compressThumb(bitmap)
                thumbData = thumbBmp
            }

            // 创建请求
            val request = SendMessageToWX.Req().apply {
                transaction = "img_${System.currentTimeMillis()}"
                message = message
                scene = this@shareImage.scene
            }

            api.sendReq(request)
            Log.i(TAG, "微信分享请求已发送")
        } catch (e: Exception) {
            Log.e(TAG, "微信分享失败", e)
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

            val webObject = WXWebpageObject().apply {
                webUrl = url
            }

            val message = WXMediaMessage().apply {
                mediaObject = webObject
                this.title = title
                this.description = description
                thumbBitmap?.let { thumbData = compressThumb(it) }
            }

            val request = SendMessageToWX.Req().apply {
                transaction = "web_${System.currentTimeMillis()}"
                this.message = message
                this.scene = scene
            }

            api.sendReq(request)
        } catch (e: Exception) {
            Log.e(TAG, "微信网页分享失败", e)
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
     */
    fun handleIntent(context: Context): Boolean {
        return wxApi?.handleIntent(null, null) ?: false
    }
}
