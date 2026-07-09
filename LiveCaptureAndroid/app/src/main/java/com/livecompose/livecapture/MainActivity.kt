package com.livecompose.livecapture

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.phantom.PhantomController
import com.livecompose.livecapture.features.privacy.PrivacyAgreementDialog
import com.livecompose.livecapture.features.privacy.isPrivacyAgreed
import com.livecompose.livecapture.navigation.AppNavigation
import com.livecompose.livecapture.ui.design.LiveCaptureTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_QUICK_CAPTURE = "com.livecompose.livecapture.QUICK_CAPTURE"
        const val ACTION_CAMERA_SHORTCUT = "android.media.action.STILL_IMAGE_CAMERA"
    }

    /** 是否需要弹出幻影模式权限请求（来自磁贴点击的 Intent extra） */
    private var showGhostPermissions by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全局未捕获异常防护：记录日志后延迟重启 Activity 而非闪退
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.e(TAG, "未捕获异常 (thread=$thread): ${throwable.message}", throwable)
            // 交给原处理器（CrashHandler）保存崩溃信息后终止进程
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            AppLogger.e(TAG, "enableEdgeToEdge 失败", e)
        }

        showGhostPermissions = intent?.getBooleanExtra("show_ghost_permissions", false) ?: false
        AppLogger.i(TAG, "[启动链路] MainActivity.onCreate 开始")

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
                action == "android.media.action.STILL_IMAGE_CAMERA" -> {}
                action == "android.media.action.VIDEO_CAMERA" -> {}
            }
        }
    }
}

/**
 * 主屏幕：隐私协议 → 主界面
 */
@Composable
private fun MainScreen(showGhostPermissions: Boolean = false) {
    var privacyAgreed by remember { mutableStateOf(false) }
    var checkingPrivacy by remember { mutableStateOf(true) }
    var pendingGhostPermissions by remember { mutableStateOf(showGhostPermissions) }

    val context = LocalContext.current

    // 幻影模式权限请求 Launcher
    val ghostPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            PhantomController.start(context)
        }
        pendingGhostPermissions = false
    }

    LaunchedEffect(Unit) {
        try {
            val agreed = isPrivacyAgreed(context)
            privacyAgreed = agreed
            AppLogger.i("MainScreen", "[启动链路] 隐私协议状态: agreed=$agreed")
        } catch (e: Exception) {
            AppLogger.e("MainScreen", "读取隐私协议状态失败", e)
            privacyAgreed = false
        }
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
                    onAgree = {
                        AppLogger.i("MainScreen", "[启动链路] 用户同意隐私协议，进入主界面")
                        privacyAgreed = true
                    },
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
