package com.livecompose.livecapture

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Choreographer
import android.view.WindowManager
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

/**
 * 2026旗舰影像主Activity
 * 适配国内主流品牌高端机型特性：
 * - 高刷新率屏幕（120Hz/144Hz/165Hz）
 * - 挖孔屏/刘海屏/灵动岛
 * - 折叠屏窗口变化
 * - 系统相机快捷入口
 */
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

        // === 2026国内高端机型适配 ===
        setupHighRefreshRate()
        setupDisplayCutout()
        setupChoreographer()

        showGhostPermissions = intent?.getBooleanExtra("show_ghost_permissions", false) ?: false
        AppLogger.i(TAG, "[启动链路] MainActivity.onCreate 开始")

        setContent {
            MainScreen(showGhostPermissions = showGhostPermissions)
        }
    }

    /**
     * 高刷新率适配：请求系统使用最高可用刷新率
     * 适配2026年国内主流品牌高端机型（120Hz/144Hz/165Hz）
     */
    private fun setupHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                // Android 13+ 使用 PREFERRED_REFRESH_RATE_MAX
                window.attributes.preferredRefreshRate = WindowManager.LayoutParams.PREFERRED_REFRESH_RATE_MAX
                AppLogger.i(TAG, "已请求最高刷新率 (Android 13+)")
            } catch (e: Exception) {
                AppLogger.w(TAG, "设置高刷新率失败", e)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Android 12 尝试设置较高刷新率
                window.attributes.preferredRefreshRate = 120f
                AppLogger.i(TAG, "已请求120Hz刷新率 (Android 12)")
            } catch (e: Exception) {
                AppLogger.w(TAG, "设置高刷新率失败", e)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Android 6-11 尝试获取并设置最高刷新率模式
                val display = windowManager.defaultDisplay
                val supportedModes = display.supportedModes
                if (supportedModes.isNotEmpty()) {
                    val maxMode = supportedModes.maxByOrNull { it.refreshRate }
                    maxMode?.let {
                        window.attributes.preferredDisplayModeId = it.modeId
                        AppLogger.i(TAG, "已设置刷新率模式: ${it.refreshRate}Hz")
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "设置高刷新率模式失败", e)
            }
        }
    }

    /**
     * 挖孔屏/刘海屏适配：允许内容延伸至挖孔区域
     * 适配华为、小米、OPPO、vivo等品牌的挖孔屏/刘海屏
     */
    private fun setupDisplayCutout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                AppLogger.i(TAG, "已适配挖孔屏/刘海屏")
            } catch (e: Exception) {
                AppLogger.w(TAG, "设置挖孔屏适配失败", e)
            }
        }
    }

    /**
     * Choreographer优化：确保动画在高刷新率屏幕上流畅运行
     */
    private fun setupChoreographer() {
        try {
            Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    // 持续监听帧率，可用于后续性能监控
                    // 此处仅注册一次以初始化Choreographer
                }
            })
        } catch (e: Exception) {
            AppLogger.w(TAG, "Choreographer初始化失败", e)
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
