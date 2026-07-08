package com.livecompose.livecapture.wxapi

import android.app.Activity
import android.os.Bundle
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.livecompose.livecapture.features.share.WeChatShareHelper
import android.util.Log

/**
 * 微信分享回调 Activity
 * 包名必须为 wxapi.WXEntryActivity，微信 SDK 强制要求
 */
class WXEntryActivity : Activity(), IWXAPIEventHandler {

    companion object {
        private const val TAG = "WXEntryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            WeChatShareHelper.handleIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "微信回调处理失败", e)
        } finally {
            finish()
        }
    }

    override fun onReq(req: BaseReq?) {
        finish()
    }

    override fun onResp(resp: BaseResp?) {
        when (resp?.type) {
            ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> {
                when (resp.errCode) {
                    BaseResp.ErrCode.ERR_OK -> {
                        Log.i(TAG, "微信分享成功")
                    }
                    BaseResp.ErrCode.ERR_USER_CANCEL -> {
                        Log.i(TAG, "用户取消微信分享")
                    }
                    else -> {
                        Log.w(TAG, "微信分享失败: ${resp.errCode} - ${resp.errStr}")
                    }
                }
            }
        }
        finish()
    }
}
