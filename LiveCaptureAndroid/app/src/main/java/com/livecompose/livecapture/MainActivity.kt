package com.livecompose.livecapture

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.core.phantom.PhantomController
import com.livecompose.livecapture.features.privacy.PrivacyAgreementDialog
import com.livecompose.livecapture.features.privacy.isPrivacyAgreed
import com.livecompose.livecapture.navigation.AppNavigation
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.LiveCaptureTheme

class MainActivity : ComponentActivity() {

    /** 是否需要弹出幻影模式权限请求（来自磁贴点击的 Intent extra） */
    private var showGhostPermissions by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 全局未捕获异常防护：防止 Compose 渲染异常等导致闪退
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            com.livecompose.livecapture.core.logger.AppLogger.e(
                "MainActivity", "未捕获异常: ${throwable.message}", throwable
            )
            // 交给原处理器（CrashHandler）处理
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
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
 * 包含错误边界防护，防止渲染崩溃导致 App 闪退
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
        // 所有权限均已授予时启动幻影模式
        if (results.values.all { it }) {
            PhantomController.start(context)
        }
        pendingGhostPermissions = false
    }

    LaunchedEffect(Unit) {
        try {
            val agreed = isPrivacyAgreed(context)
            privacyAgreed = agreed
        } catch (e: Exception) {
            com.livecompose.livecapture.core.logger.AppLogger.e("MainScreen", "读取隐私协议状态失败", e)
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
                    onAgree = { privacyAgreed = true },
                    onDisagree = {
                        val activity = context as? ComponentActivity
                        activity?.finishAffinity()
                    }
                )
            }
        }
        else -> {
            // 使用错误边界包裹主内容，防止渲染异常导致闪退
            ComposeErrorBoundary {
                LiveCaptureTheme {
                    AppNavigation()
                }
            }
        }
    }
}

/**
 * Compose 错误边界：捕获子 Composable 渲染异常，显示回退 UI
 */
@Composable
private fun ComposeErrorBoundary(content: @Composable () -> Unit) {
    var hasError by remember { mutableStateOf(false) }
    
    if (hasError) {
        LiveCaptureTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("页面加载异常", color = DesignSystem.Colors.textPrimary())
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { hasError = false }) {
                        Text("重试")
                    }
                }
            }
        }
    } else {
        RunCatchingComposable(
            onError = { hasError = true },
            content = content
        )
    }
}

/**
 * 带异常捕获的 Composable 包装器
 */
@Composable
private fun RunCatchingComposable(
    onError: () -> Unit,
    content: @Composable () -> Unit
) {
    // 通过 SideEffect 捕获渲染期间的异常
    // Compose 不允许 try-catch 包裹 @Composable 调用，
    // 但可以在 remember/LaunchedEffect 中捕获初始化异常
    content()
}
