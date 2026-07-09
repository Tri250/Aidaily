package com.livecompose.livecapture

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.livecompose.livecapture.core.phantom.PhantomController
import com.livecompose.livecapture.features.privacy.PrivacyAgreementDialog
import com.livecompose.livecapture.features.privacy.isPrivacyAgreed
import com.livecompose.livecapture.navigation.AppNavigation
import com.livecompose.livecapture.ui.design.LiveCaptureTheme

class MainActivity : ComponentActivity() {

    /** 是否需要弹出幻影模式权限请求（来自磁贴点击的 Intent extra） */
    private var showGhostPermissions by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showGhostPermissions = intent?.getBooleanExtra("show_ghost_permissions", false) ?: false
        setContent {
            MainScreen(showGhostPermissions = showGhostPermissions)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("show_ghost_permissions", false)) {
            showGhostPermissions = true
        }
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
private fun MainScreen(showGhostPermissions: Boolean = false) {
    var privacyAgreed by remember { mutableStateOf(false) }
    var checkingPrivacy by remember { mutableStateOf(true) }
    var pendingGhostPermissions by remember { mutableStateOf(showGhostPermissions) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // 幻影模式权限请求 Launcher
    val ghostPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // 所有权限均已授予时启动幻影模式
        if (results.values.all { it }) {
            PhantomController.start(context)
        }
        pendingGhostPermissions = false
    }

    LaunchedEffect(Unit) {
        val agreed = isPrivacyAgreed(context)
        privacyAgreed = agreed
        checkingPrivacy = false
    }

    // 隐私协议同意后，如有待处理的权限请求则触发
    LaunchedEffect(privacyAgreed, pendingGhostPermissions) {
        if (privacyAgreed && pendingGhostPermissions) {
            ghostPermissionLauncher.launch(PhantomController.getRequiredPermissions())
        }
    }

    when {
        checkingPrivacy -> {
            // 加载中（短暂）
        }
        !privacyAgreed -> {
            LiveCaptureTheme {
                PrivacyAgreementDialog(
                    onAgree = { privacyAgreed = true },
                    onDisagree = {
                        val activity = context as? ComponentActivity
                        activity?.finishAffinity()
                    }
                )
            }
        }
        else -> {
            LiveCaptureTheme {
                AppNavigation()
            }
        }
    }
}
