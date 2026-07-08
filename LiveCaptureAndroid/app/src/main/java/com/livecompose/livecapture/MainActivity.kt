package com.livecompose.livecapture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.livecompose.livecapture.features.privacy.PrivacyAgreementDialog
import com.livecompose.livecapture.features.privacy.isPrivacyAgreed
import com.livecompose.livecapture.navigation.AppNavigation
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }

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

/**
 * 主屏幕：隐私协议 → 引导 → 主界面
 */
@Composable
private fun MainScreen() {
    val scope = rememberCoroutineScope()
    var privacyAgreed by remember { mutableStateOf(false) }
    var checkingPrivacy by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val agreed = isPrivacyAgreed(context)
        privacyAgreed = agreed
        checkingPrivacy = false
    }

    when {
        checkingPrivacy -> {
            // 加载中（短暂）
        }
        !privacyAgreed -> {
            PrivacyAgreementDialog(
                onAgree = { privacyAgreed = true },
                onDisagree = {
                    val activity = androidx.compose.ui.platform.LocalContext.current as? ComponentActivity
                    activity?.finishAffinity()
                }
            )
        }
        else -> {
            AppNavigation()
        }
    }
}
