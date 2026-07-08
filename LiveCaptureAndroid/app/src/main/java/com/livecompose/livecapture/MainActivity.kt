package com.livecompose.livecapture

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.livecompose.livecapture.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }

    // 快捷拍摄：支持从音量键或其他快捷方式启动
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.action?.let { action ->
            when {
                action == "android.media.action.STILL_IMAGE_CAMERA" -> {
                    // 系统相机快捷入口
                }
                action == "android.media.action.VIDEO_CAMERA" -> {
                    // 视频快捷入口
                }
            }
        }
    }

    companion object {
        const val ACTION_QUICK_CAPTURE = "com.livecompose.livecapture.QUICK_CAPTURE"
        const val ACTION_CAMERA_SHORTCUT = "android.media.action.STILL_IMAGE_CAMERA"
    }
}
