package com.livecompose.livecapture.presentation.capture

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.livecompose.livecapture.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.livecompose.livecapture.core.design.*
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine
import com.livecompose.livecapture.core.detection.CompositionResult
import com.livecompose.livecapture.core.detection.SceneAnalysisResult
import com.livecompose.livecapture.presentation.Screen
import com.livecompose.livecapture.presentation.components.CountdownNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@Composable
fun CaptureView(
    viewModel: CaptureViewModel = hiltViewModel(),
    navController: NavController? = null,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val stage by viewModel.pipelineStage.collectAsStateWithLifecycle()
    val guidanceText by viewModel.guidanceText.collectAsStateWithLifecycle()
    val trackPoint by viewModel.trackPoint.collectAsStateWithLifecycle()
    val isAligned by viewModel.isAligned.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val isTorchEnabled by viewModel.isTorchEnabled.collectAsStateWithLifecycle()
    val inferenceTime by viewModel.inferenceTime.collectAsStateWithLifecycle()
    val isModelReady by viewModel.isModelReady.collectAsStateWithLifecycle()
    val currentScore by viewModel.currentScore.collectAsStateWithLifecycle()
    val exposureCompensation by viewModel.exposureCompensation.collectAsStateWithLifecycle()
    val exposureRange by viewModel.exposureRange.collectAsStateWithLifecycle()
    val zoomRange by viewModel.zoomRange.collectAsStateWithLifecycle()
    val hasTorchUnit by viewModel.hasTorchUnit.collectAsStateWithLifecycle()
    val lastSavedThumbPath by viewModel.lastSavedThumbPath.collectAsStateWithLifecycle()
    val modelLoadFailed by viewModel.modelLoadFailed.collectAsStateWithLifecycle()
    val activeModelVariant by viewModel.activeModelVariant.collectAsStateWithLifecycle()
    val isCameraStarting by viewModel.isCameraStarting.collectAsStateWithLifecycle()
    val cameraError by viewModel.cameraError.collectAsStateWithLifecycle()
    val alignmentProgress by viewModel.alignmentProgress.collectAsStateWithLifecycle()
    val isBurstCapturing by viewModel.isBurstCapturing.collectAsStateWithLifecycle()
    // 拍摄成功反馈
    val captureSuccess by viewModel.captureSuccess.collectAsStateWithLifecycle()
    // 智能场景识别
    val sceneAnalysis by viewModel.sceneAnalysis.collectAsStateWithLifecycle()
    // 倒计时状态
    val countdown by viewModel.countdown.collectAsStateWithLifecycle()
    // 庆祝动画状态
    val showCelebration by viewModel.showCelebration.collectAsStateWithLifecycle()
    // 声控拍照状态
    val voiceCaptureTriggered by viewModel.voiceCaptureTriggered.collectAsStateWithLifecycle()
    val voiceCaptureReady by viewModel.voiceCaptureReady.collectAsStateWithLifecycle()
    val rollAngle by viewModel.rollAngle.collectAsStateWithLifecycle()

    // 设置项
    val gridEnabledSetting by viewModel.gridEnabledFlow.collectAsStateWithLifecycle()
    val voiceCaptureDefaultSetting by viewModel.voiceCaptureDefaultFlow.collectAsStateWithLifecycle()
    val hapticEnabledSetting by viewModel.hapticEnabledFlow.collectAsStateWithLifecycle()
    val sceneRecognitionEnabledSetting by viewModel.sceneRecognitionEnabledFlow.collectAsStateWithLifecycle()

    var isVoiceCaptureEnabled by remember { mutableStateOf(voiceCaptureDefaultSetting) }

    // 权限状态管理
    var hasCameraPermission by remember {
        mutableStateOf(viewModel.hasCameraPermission())
    }
    var hasBeenDenied by remember { mutableStateOf(false) }

    // 麦克风权限（声控拍照）
    var hasMicrophonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicrophonePermission = granted
        if (granted && isVoiceCaptureEnabled) {
            viewModel.startVoiceCapture()
        } else if (!granted) {
            isVoiceCaptureEnabled = false
        }
    }

    var showExposureSlider by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(gridEnabledSetting) }

    // 触觉反馈工具
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // 对齐成功时触觉反馈
    LaunchedEffect(isAligned, hapticEnabledSetting) {
        if (isAligned && hapticEnabledSetting) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    LaunchedEffect(captureSuccess, hapticEnabledSetting) {
        if (captureSuccess && hapticEnabledSetting) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            hasBeenDenied = true
        }
    }

    // 判断是否需要显示 Rationale — 权限状态变化时重新计算
    val activity = context as? android.app.Activity
    val shouldShowRationale = remember(hasBeenDenied, hasCameraPermission) {
        activity?.let { viewModel.shouldShowCameraRationale(it) } ?: false
    }

    // #32: 生命周期绑定 — onPause 停止相机，onResume 重启相机
    var isResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isResumed = false
                    viewModel.stopCamera()
                }
                Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                    // 从系统设置返回后重新检查权限状态，避免权限已授予但 UI 仍显示拒绝
                    hasCameraPermission = viewModel.hasCameraPermission()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopCamera()
        }
    }

    // 权限已授予且页面可见时启动相机
    LaunchedEffect(hasCameraPermission, isResumed) {
        if (hasCameraPermission && isResumed) {
            viewModel.startCamera(lifecycleOwner, previewView)
        }
    }

    val displayMetrics = context.resources.displayMetrics
    LaunchedEffect(Unit) {
        viewModel.setScreenSize(
            displayMetrics.widthPixels.toFloat(),
            displayMetrics.heightPixels.toFloat()
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // #7: 权限被拒绝 — 显示权限请求 UI
            !hasCameraPermission -> {
                PermissionDeniedContent(
                    hasBeenDenied = hasBeenDenied,
                    shouldShowRationale = shouldShowRationale,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onOpenSettings = {
                        viewModel.openAppSettings()
                    }
                )
            }

            // #56/#17: ERROR 状态 — 显示错误重试 UI
            stage == CaptureViewModel.PipelineStage.ERROR || cameraError != null -> {
                CameraErrorContent(
                    errorMessage = cameraError ?: stringResource(R.string.capture_error_message),
                    onRetry = {
                        viewModel.retry()
                        // 重新启动相机
                        if (hasCameraPermission && isResumed) {
                            viewModel.startCamera(lifecycleOwner, previewView)
                        }
                    }
                )
            }

            else -> {
                // Camera Preview — 点击对焦 + 捏合缩放
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(zoomRange) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom != 1f) {
                                    val newRatio = (zoomRatio * zoom).coerceIn(zoomRange.start, zoomRange.endInclusive)
                                    viewModel.setZoom(newRatio)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    viewModel.focusAndMeter(
                                        x = offset.x,
                                        y = offset.y,
                                        previewWidth = size.width.toFloat(),
                                        previewHeight = size.height.toFloat()
                                    )
                                }
                            )
                        }
                )

                // Composition Grid Overlay — 可开关
                if (showGrid) {
                    CompositionGridOverlay()
                }

                // 智能场景识别信息栏（根据设置决定是否显示）
                if (sceneRecognitionEnabledSetting) {
                    sceneAnalysis?.let { SceneInfoOverlay(it) }
                }

                // Tracking Dot
                trackPoint?.let { point ->
                    TrackingDot(
                        position = point,
                        isAligned = isAligned,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // #54: 刘海屏适配 — 使用 WindowInsets 替代硬编码 padding
                TopControlBar(
                    guidanceText = guidanceText,
                    isAligned = isAligned,
                    isModelReady = isModelReady,
                    modelLoadFailed = modelLoadFailed,
                    activeVariant = activeModelVariant,
                    inferenceTime = inferenceTime,
                    currentScore = currentScore,
                    showGrid = showGrid,
                    onGridToggle = { showGrid = !showGrid },
                    isVoiceCaptureEnabled = isVoiceCaptureEnabled,
                    voiceCaptureReady = voiceCaptureReady,
                    rollAngle = rollAngle,
                    onVoiceCaptureToggle = {
                        if (!isVoiceCaptureEnabled) {
                            // 开启前检查麦克风权限
                            if (hasMicrophonePermission) {
                                isVoiceCaptureEnabled = true
                                viewModel.startVoiceCapture()
                            } else {
                                isVoiceCaptureEnabled = true // 预设为开启，权限回调后启动
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            isVoiceCaptureEnabled = false
                            viewModel.stopVoiceCapture()
                        }
                    },
                    onSettingsClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                )

                // Exposure Slider Panel — 动态绑定设备真实曝光范围
                AnimatedVisibility(
                    visible = showExposureSlider,
                    enter = fadeIn() + slideInHorizontally { it / 2 },
                    exit = fadeOut() + slideOutHorizontally { it / 2 },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    ExposureSliderPanel(
                        value = exposureCompensation,
                        valueRange = exposureRange,
                        onValueChange = { viewModel.setExposureCompensation(it) },
                        onDismiss = { showExposureSlider = false }
                    )
                }

                // #55: 加载状态指示 — 淡入淡出 (模型失败提示已在 TopControlBar 显示)
                AnimatedVisibility(
                    visible = isCameraStarting || (!isModelReady && !modelLoadFailed),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    LoadingOverlay(
                        text = if (isCameraStarting) stringResource(R.string.capture_camera_loading) else stringResource(R.string.capture_model_loading)
                    )
                }

                // #69: 拍摄成功闪白反馈动画
                AnimatedVisibility(
                    visible = captureSuccess,
                    enter = fadeIn(animationSpec = tween(AnimationDuration.DURATION_VERY_FAST)),
                    exit = fadeOut(animationSpec = tween(AnimationDuration.DURATION_SLOW)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }

                // 倒计时大数字覆盖层
                if (countdown > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CountdownNumber(currentCount = countdown)
                    }
                }

                // 庆祝动画覆盖层
                if (showCelebration) {
                    com.livecompose.livecapture.presentation.components.CombinedCelebration(
                        modifier = Modifier.fillMaxSize(),
                        trigger = true,
                        showConfetti = true,
                        showRipple = true
                    )
                }

                // Bottom Controls — #54: 刘海屏适配
                BottomControls(
                    zoomRatio = zoomRatio,
                    zoomRange = zoomRange,
                    isTorchEnabled = isTorchEnabled,
                    hasTorchUnit = hasTorchUnit,
                    stage = stage,
                    lastSavedThumbPath = lastSavedThumbPath,
                    alignmentProgress = alignmentProgress,
                    isBurstCapturing = isBurstCapturing,
                    onZoomChange = { viewModel.setZoom(it) },
                    onSwitchCamera = { viewModel.switchCamera(lifecycleOwner, previewView) },
                    onCapture = { viewModel.manualCapture() },
                    onBurstCapture = { viewModel.burstCapture() },
                    onBurstStop = { viewModel.stopBurstCapture() },
                    onTorchToggle = { viewModel.toggleTorch() },
                    onGalleryClick = { navController?.navigate(Screen.Home.route) },
                    onExposureClick = { showExposureSlider = !showExposureSlider },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                )
            }
        }
    }
}

/**
 * 玻璃拟态 (Glassmorphism) 背景修饰符
 * API 31+ 使用模糊效果，低版本回退到半透明纯色
 */
fun Modifier.glassmorphism(
    blurRadius: Float = BlurRadius.BLUR_RADIUS_MEDIUM,
    alpha: Float = Opacity.GlassMedium
): Modifier = then(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(blurRadius.dp).background(Color.Black.copy(alpha = alpha))
    } else {
        Modifier.background(Color.Black.copy(alpha = (alpha + 0.1f).coerceAtMost(1f)))
    }
)

/**
 * #7/#8: 权限被拒绝时的 UI
 */
@Composable
private fun PermissionDeniedContent(
    hasBeenDenied: Boolean,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Massive),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_camera_title),
            color = Color.White,
            fontSize = FontSize.HeadlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
        Text(
            text = if (shouldShowRationale || !hasBeenDenied) {
                stringResource(R.string.permission_camera_rationale)
            } else {
                stringResource(R.string.permission_camera_denied)
            },
            color = Color.White.copy(alpha = 0.7f),
            fontSize = FontSize.BodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.Huge))
        if (shouldShowRationale || !hasBeenDenied) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(stringResource(R.string.permission_grant), color = Color.White)
            }
        } else {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(stringResource(R.string.permission_open_settings), color = Color.White)
            }
        }
    }
}

/**
 * #56/#17: 错误状态 UI
 */
@Composable
private fun CameraErrorContent(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Massive),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.capture_error_title),
            color = ErrorColor,
            fontSize = FontSize.HeadlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.Large))
        Text(
            text = errorMessage,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = FontSize.BodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.Huge))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.Medium))
            Text(stringResource(R.string.capture_error_retry), color = Color.White)
        }
    }
}

/**
 * #55: 加载状态指示
 */
@Composable
private fun LoadingOverlay(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(CornerRadius.Large),
        modifier = modifier.glassmorphism(
            blurRadius = BlurRadius.BLUR_RADIUS_MEDIUM,
            alpha = Opacity.GlassHeavy
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.Huge, vertical = Spacing.ExtraLarge),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(Spacing.Large))
            Text(
                text = text,
                color = Color.White,
                style = GuidanceTextStyle.copy(fontSize = FontSize.BodyMedium)
            )
        }
    }
}

@Composable
private fun CompositionGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.dp.toPx()
        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        // 三分法网格线
        drawLine(
            color = GridLine,
            start = Offset(width / 3, 0f),
            end = Offset(width / 3, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = GridLine,
            start = Offset(width * 2 / 3, 0f),
            end = Offset(width * 2 / 3, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = GridLine,
            start = Offset(0f, height / 3),
            end = Offset(width, height / 3),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = GridLine,
            start = Offset(0f, height * 2 / 3),
            end = Offset(width, height * 2 / 3),
            strokeWidth = strokeWidth
        )

        // 对角线构图线（虚线）
        drawLine(
            color = GridLine,
            start = Offset(0f, 0f),
            end = Offset(width, height),
            strokeWidth = strokeWidth,
            pathEffect = dashPathEffect
        )
        drawLine(
            color = GridLine,
            start = Offset(width, 0f),
            end = Offset(0f, height),
            strokeWidth = strokeWidth,
            pathEffect = dashPathEffect
        )
    }
}

@Composable
private fun TrackingDot(
    position: PointF,
    isAligned: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAligned) 1.3f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AnimationDuration.DURATION_VERY_SLOW, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val dotColor = if (isAligned) TrackingDotAligned else TrackingDotColor
    val dotSize = if (isAligned) 24.dp else 20.dp

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val x = position.x
            val y = position.y
            val radius = dotSize.toPx() / 2

            drawCircle(
                color = dotColor.copy(alpha = 0.3f),
                radius = radius * pulseScale,
                center = Offset(x, y)
            )

            drawCircle(
                color = dotColor,
                radius = radius * 0.6f,
                center = Offset(x, y)
            )

            if (isAligned) {
                val crossSize = 30.dp.toPx()
                drawLine(
                    color = dotColor,
                    start = Offset(x - crossSize, y),
                    end = Offset(x + crossSize, y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = dotColor,
                    start = Offset(x, y - crossSize),
                    end = Offset(x, y + crossSize),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

/**
 * 水平仪指示器
 */
@Composable
private fun LevelIndicator(rollAngle: Float) {
    val absRoll = kotlin.math.abs(rollAngle)
    val indicatorColor = when {
        absRoll < 1f -> TrackingDotAligned
        absRoll > 3f -> Color(0xFFFF7043)
        else -> Color(0xFFFFD54F)
    }

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f

            // 背景条
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = height
            )

            // 中心标记线
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = 1.dp.toPx()
            )

            // 指示点：最大 ±15° 映射到条宽
            val maxAngle = 15f
            val offsetX = (rollAngle / maxAngle * (width / 2f)).coerceIn(-width / 2f, width / 2f)

            drawCircle(
                color = indicatorColor,
                radius = height * 1.2f,
                center = Offset(centerX + offsetX, height / 2)
            )
        }
    }
}

@Composable
private fun TopControlBar(
    guidanceText: String,
    isAligned: Boolean,
    isModelReady: Boolean,
    modelLoadFailed: Boolean,
    activeVariant: AdacropInferenceEngine.ModelVariant?,
    inferenceTime: Long,
    currentScore: Float,
    showGrid: Boolean,
    onGridToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    isVoiceCaptureEnabled: Boolean = false,
    voiceCaptureReady: Boolean = false,
    rollAngle: Float = 0f,
    onVoiceCaptureToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.Medium, start = Spacing.ExtraLarge, end = Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            // 声控拍照开关
            IconButton(onClick = onVoiceCaptureToggle) {
                Icon(
                    imageVector = if (isVoiceCaptureEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isVoiceCaptureEnabled) stringResource(R.string.capture_voice_on) else stringResource(R.string.capture_voice_off),
                    tint = when {
                        isVoiceCaptureEnabled && voiceCaptureReady -> Color(0xFF4CAF50) // 绿色：正在监听
                        isVoiceCaptureEnabled -> Color(0xFFFFD700) // 金色：已开启但未就绪
                        else -> Color.White.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
            // 网格线开关
            IconButton(onClick = onGridToggle) {
                Icon(
                    imageVector = if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                    contentDescription = if (showGrid) stringResource(R.string.capture_grid_on) else stringResource(R.string.capture_grid_off),
                    tint = if (showGrid) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.capture_settings),
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Medium))

        // 模型状态指示: 加载中 / 加载失败降级 / 正常显示当前变体
        if (modelLoadFailed) {
            Surface(
                color = Color(0xFFFF7043).copy(alpha = 0.85f),
                shape = RoundedCornerShape(CornerRadius.Large)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Small + Spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small + Spacing.ExtraSmall)
                ) {
                    Text(
                        text = "⚠",
                        color = Color.White,
                        fontSize = FontSize.BodyMedium
                    )
                    Text(
                        text = stringResource(R.string.capture_model_failed),
                        style = GuidanceTextStyle.copy(fontSize = FontSize.BodySmall, color = Color.White)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.Small))
        } else if (!isModelReady) {
            Surface(
                color = GuidanceBg,
                shape = RoundedCornerShape(CornerRadius.Large)
            ) {
                Text(
                    text = stringResource(R.string.capture_model_loading),
                    style = GuidanceTextStyle.copy(fontSize = FontSize.BodySmall),
                    modifier = Modifier.padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Small + Spacing.ExtraSmall)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.Small))
        }

        if (guidanceText.isNotEmpty()) {
            Surface(
                color = if (modelLoadFailed) Color(0xFFFF7043).copy(alpha = 0.25f) else Color.Transparent,
                shape = RoundedCornerShape(CornerRadius.Large),
                modifier = if (modelLoadFailed) Modifier else Modifier.glassmorphism()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.ExtraLarge + Spacing.Medium, vertical = Spacing.Medium + Spacing.Small),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 当前模型变体标识 (Student=Fast / Teacher=Pro)
                    if (activeVariant != null) {
                        Text(
                            text = if (activeVariant == AdacropInferenceEngine.ModelVariant.TEACHER) stringResource(R.string.capture_model_variant_pro) else stringResource(R.string.capture_model_variant_fast),
                            color = if (activeVariant == AdacropInferenceEngine.ModelVariant.TEACHER) TrackingDotAligned else Color.White.copy(alpha = 0.8f),
                            style = GuidanceTextStyle.copy(fontSize = FontSize.Label, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .clip(RoundedCornerShape(CornerRadius.Small))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = Spacing.Small + Spacing.ExtraSmall, vertical = Spacing.ExtraSmall)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Medium))
                    }
                    // 指引文案切换淡入淡出动画
                    AnimatedContent(
                        targetState = guidanceText,
                        transitionSpec = {
                            fadeIn(tween(AnimationDuration.DURATION_MEDIUM)) togetherWith fadeOut(tween(AnimationDuration.DURATION_FAST))
                        },
                        label = "guidanceText"
                    ) { text ->
                        Text(
                            text = text,
                            style = GuidanceTextStyle
                        )
                    }
                    if (inferenceTime > 0) {
                        Spacer(modifier = Modifier.width(Spacing.Large))
                        Text(
                            text = "${inferenceTime}ms",
                            color = Color.White.copy(alpha = 0.6f),
                            style = GuidanceTextStyle.copy(fontSize = FontSize.BodySmall)
                        )
                    }
                    if (currentScore > 0f) {
                        Spacer(modifier = Modifier.width(Spacing.Large))
                        Text(
                            text = stringResource(R.string.capture_score_label, currentScore),
                            color = if (currentScore > 0.6f) TrackingDotAligned else Color.White.copy(alpha = 0.6f),
                            style = GuidanceTextStyle.copy(fontSize = FontSize.BodySmall)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.Small + Spacing.ExtraSmall))
            LevelIndicator(rollAngle = rollAngle)
        }

        if (isAligned) {
            Text(
                text = stringResource(R.string.guidance_aligned),
                color = TrackingDotAligned,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = Spacing.Medium)
            )
        }
    }
}

@Composable
private fun ExposureSliderPanel(
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用设备真实曝光范围，避免硬编码 -4..4 与设备能力脱节
    val minVal = valueRange.first.toFloat()
    val maxVal = valueRange.last.toFloat()
    // steps = 区间内整数点数 - 1（Slider steps 不含两端）
    val stepCount = (valueRange.last - valueRange.first - 1).coerceAtLeast(0)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(CornerRadius.Large),
        modifier = Modifier
            .padding(end = Spacing.ExtraLarge)
            .glassmorphism()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.capture_exposure) + " $value",
                color = Color.White,
                style = GuidanceTextStyle.copy(fontSize = FontSize.BodySmall)
            )
            Spacer(modifier = Modifier.height(Spacing.Medium))
            Slider(
                value = value.toFloat().coerceIn(minVal, maxVal),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = minVal..maxVal,
                steps = stepCount,
                modifier = Modifier.height(120.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.Medium))
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.gallery_close), color = Color.White, fontSize = FontSize.BodySmall)
            }
        }
    }
}

@Composable
private fun BottomControls(
    zoomRatio: Float,
    zoomRange: ClosedRange<Float>,
    isTorchEnabled: Boolean,
    hasTorchUnit: Boolean,
    stage: CaptureViewModel.PipelineStage,
    lastSavedThumbPath: String?,
    alignmentProgress: Float,
    isBurstCapturing: Boolean,
    onZoomChange: (Float) -> Unit,
    onSwitchCamera: () -> Unit,
    onCapture: () -> Unit,
    onBurstCapture: () -> Unit,
    onBurstStop: () -> Unit,
    onTorchToggle: () -> Unit,
    onGalleryClick: () -> Unit,
    onExposureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCaptureEnabled = stage == CaptureViewModel.PipelineStage.TEMPLATE_READY ||
            stage == CaptureViewModel.PipelineStage.READY_TO_CAPTURE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.ExtraLarge, start = Spacing.Huge, end = Spacing.Huge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom Buttons — 根据设备真实缩放范围动态显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 0.5x 仅在设备支持超广角时显示
            if (zoomRange.start <= 0.5f) {
                ZoomButton("0.5x", abs(zoomRatio - 0.5f) < 0.01f) { onZoomChange(0.5f) }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            ZoomButton("1x", abs(zoomRatio - 1.0f) < 0.01f) { onZoomChange(1.0f) }
            // 2x 仅在设备支持时显示
            if (zoomRange.endInclusive >= 2.0f) {
                ZoomButton("2x", abs(zoomRatio - 2.0f) < 0.01f) { onZoomChange(2.0f) }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Huge))

        // Capture Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图使用 thumbPath（小图），避免解码全分辨率主图造成卡顿
            if (lastSavedThumbPath != null) {
                LastPhotoThumbnail(
                    photoPath = lastSavedThumbPath,
                    onClick = onGalleryClick
                )
            } else {
                IconButton(onClick = onGalleryClick) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = stringResource(R.string.capture_gallery),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            CaptureButton(
                enabled = isCaptureEnabled,
                stage = stage,
                alignmentProgress = alignmentProgress,
                isBurstCapturing = isBurstCapturing,
                onClick = onCapture,
                onBurstCapture = onBurstCapture,
                onBurstStop = onBurstStop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onSwitchCamera) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = stringResource(R.string.capture_switch_camera),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Row {
                    // 无闪光灯单元时禁用按钮，避免假显示
                    IconButton(
                        onClick = onTorchToggle,
                        enabled = hasTorchUnit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (isTorchEnabled) stringResource(R.string.capture_torch_on) else stringResource(R.string.capture_torch_off),
                            tint = when {
                                !hasTorchUnit -> Color.White.copy(alpha = 0.3f)
                                isTorchEnabled -> Color.Yellow
                                else -> Color.White
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onExposureClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Exposure,
                            contentDescription = stringResource(R.string.capture_exposure),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LastPhotoThumbnail(
    photoPath: String,
    onClick: () -> Unit
) {
    val imageModel = if (photoPath.startsWith("content://")) {
        Uri.parse(photoPath)
    } else {
        File(photoPath)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(CornerRadius.Medium))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = "Last photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 智能场景识别信息覆盖层
 * 显示: 场景类型标签 + 光照质量指示 + 拍摄指导提示
 */
@Composable
private fun SceneInfoOverlay(
    result: SceneAnalysisResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.ExtraLarge, end = Spacing.ExtraLarge, top = Spacing.Medium)
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.Start
    ) {
        // 场景类型标签 + 光照质量
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            // 场景类型标签
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(CornerRadius.ExtraLarge),
                modifier = Modifier.glassmorphism(alpha = Opacity.GlassMedium)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small + Spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    // 场景图标点 — 颜色跟随场景变化动画
                    val dotColor by animateColorAsState(
                        targetValue = sceneTypeColor(result.sceneType),
                        animationSpec = tween(AnimationDuration.DURATION_SLOW),
                        label = "sceneDotColor"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(dotColor, CircleShape)
                    )
                    // 场景标签切换动画
                    AnimatedContent(
                        targetState = result.sceneType.label,
                        transitionSpec = {
                            (fadeIn(tween(AnimationDuration.DURATION_MEDIUM)) + slideInVertically { -it }) togetherWith
                                    (fadeOut(tween(AnimationDuration.DURATION_FAST)) + slideOutVertically { it })
                        },
                        label = "sceneLabel"
                    ) { label ->
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = FontSize.BodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 光照质量指示
            if (result.lightingQuality != CompositionResult.LightingQuality.GOOD) {
                Surface(
                    color = lightingQualityColor(result.lightingQuality).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(CornerRadius.ExtraLarge)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.Medium + Spacing.Small, vertical = Spacing.Small + Spacing.ExtraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(lightingQualityColor(result.lightingQuality), CircleShape)
                        )
                        Text(
                            text = lightingQualityLabel(result.lightingQuality),
                            color = lightingQualityColor(result.lightingQuality),
                            fontSize = FontSize.BodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 拍摄指导提示条 (仅在有提示时显示)
        if (result.shootingTip.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.Small + Spacing.ExtraSmall))
            AnimatedVisibility(
                visible = result.shootingTip.isNotEmpty(),
                enter = fadeIn(tween(AnimationDuration.DURATION_MEDIUM)) + expandVertically(tween(AnimationDuration.DURATION_FAST)),
                exit = fadeOut(tween(AnimationDuration.DURATION_FAST)) + shrinkVertically(tween(AnimationDuration.DURATION_FAST))
            ) {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(CornerRadius.Medium),
                    modifier = Modifier.glassmorphism(alpha = Opacity.GlassMedium)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small + Spacing.ExtraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.shootingTip,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = FontSize.BodySmall,
                            lineHeight = FontSize.TitleMedium
                        )
                    }
                }
            }
        }

        // 拍摄参数建议 (场景识别推荐参数)
        val params = result.sceneType.shootingParams
        Row(
            modifier = Modifier.padding(top = Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small + Spacing.ExtraSmall)
        ) {
            ParamChip("f/${params.aperture.removePrefix("F")}")
            ParamChip(params.shutter)
            ParamChip("ISO ${params.iso}")
        }
    }
}

@Composable
private fun ParamChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(CornerRadius.Small)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = FontSize.Label,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = Spacing.Small + Spacing.ExtraSmall, vertical = Spacing.ExtraSmall)
        )
    }
}

private fun sceneTypeColor(sceneType: CompositionResult.SceneType): Color = when (sceneType) {
    CompositionResult.SceneType.PORTRAIT_STANDING,
    CompositionResult.SceneType.PORTRAIT_SITTING -> Color(0xFFFF8A80) // 暖粉红
    CompositionResult.SceneType.LANDSCAPE_SUNSET,
    CompositionResult.SceneType.LANDSCAPE_NATURE -> Color(0xFF69F0AE) // 自然绿
    CompositionResult.SceneType.NIGHT_SCENE -> Color(0xFFB388FF)       // 夜紫
    CompositionResult.SceneType.FOOD_STYLING -> Color(0xFFFFD54F)      // 美食金
    CompositionResult.SceneType.PRODUCT_WHITE -> Color(0xFFE0E0E0)     // 产品灰
    CompositionResult.SceneType.CITY_URBAN -> Color(0xFF40C4FF)        // 城市蓝
    CompositionResult.SceneType.GENERAL -> Color(0xFFB0BEC5)           // 通用灰蓝
}

private fun lightingQualityColor(quality: CompositionResult.LightingQuality): Color = when (quality) {
    CompositionResult.LightingQuality.GOOD -> Color(0xFF69F0AE)
    CompositionResult.LightingQuality.TOO_DARK -> Color(0xFFFF8A65)
    CompositionResult.LightingQuality.TOO_BRIGHT -> Color(0xFFFFD54F)
    CompositionResult.LightingQuality.BACKLIT -> Color(0xFFFFAB40)
    CompositionResult.LightingQuality.LOW_CONTRAST -> Color(0xFFB0BEC5)
}

@Composable
private fun lightingQualityLabel(quality: CompositionResult.LightingQuality): String = when (quality) {
    CompositionResult.LightingQuality.GOOD -> stringResource(R.string.lighting_good)
    CompositionResult.LightingQuality.TOO_DARK -> stringResource(R.string.lighting_dark)
    CompositionResult.LightingQuality.TOO_BRIGHT -> stringResource(R.string.lighting_bright)
    CompositionResult.LightingQuality.BACKLIT -> stringResource(R.string.lighting_backlit)
    CompositionResult.LightingQuality.LOW_CONTRAST -> stringResource(R.string.lighting_low_contrast)
}

@Composable
private fun ZoomButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (isSelected) Primary else Color.White.copy(alpha = 0.7f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    stage: CaptureViewModel.PipelineStage,
    alignmentProgress: Float,
    isBurstCapturing: Boolean,
    onClick: () -> Unit,
    onBurstCapture: () -> Unit,
    onBurstStop: () -> Unit
) {
    val ringColor = when {
        isBurstCapturing -> Color.Yellow
        stage == CaptureViewModel.PipelineStage.READY_TO_CAPTURE -> TrackingDotAligned
        stage == CaptureViewModel.PipelineStage.CAPTURING_PHOTO -> Color.Yellow
        stage == CaptureViewModel.PipelineStage.SAVING_PHOTO -> Color.Cyan
        else -> Color.White
    }

    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(AnimationDuration.DURATION_FAST),
        label = "captureAlpha"
    )

    val ringScale by animateFloatAsState(
        targetValue = if (stage == CaptureViewModel.PipelineStage.CAPTURING_PHOTO) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "ringScale"
    )

    val animatedRingColor by animateColorAsState(
        targetValue = ringColor,
        animationSpec = tween(AnimationDuration.DURATION_MEDIUM),
        label = "ringColor"
    )

    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )

    val burstPulseAnim = remember { Animatable(1f) }
    LaunchedEffect(isBurstCapturing) {
        if (isBurstCapturing) {
            while (isActive) {
                burstPulseAnim.animateTo(1.15f, tween(AnimationDuration.DURATION_SLOW, easing = FastOutSlowInEasing))
                burstPulseAnim.animateTo(1f, tween(AnimationDuration.DURATION_SLOW, easing = FastOutSlowInEasing))
            }
        } else {
            burstPulseAnim.snapTo(1f)
        }
    }

    val finalScale = ringScale * pressScale * if (isBurstCapturing) burstPulseAnim.value else 1f

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnBurstCapture by rememberUpdatedState(onBurstCapture)
    val currentOnBurstStop by rememberUpdatedState(onBurstStop)
    val currentIsBurstCapturing by rememberUpdatedState(isBurstCapturing)
    val scope = rememberCoroutineScope()

    Box(contentAlignment = Alignment.Center) {
        if (alignmentProgress > 0f && alignmentProgress < 1f) {
            val progressColor = lerp(Color.White, Color.Green, alignmentProgress)
            Canvas(modifier = Modifier.size(88.dp)) {
                val strokeWidth = 4.dp.toPx()
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = alignmentProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(finalScale)
                .background(animatedRingColor, CircleShape)
                .padding(Spacing.Small)
                .alpha(alpha)
                .pointerInput(enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            val job = scope.launch {
                                delay(500)
                                currentOnBurstCapture()
                            }
                            val released = tryAwaitRelease()
                            isPressed = false
                            job.cancel()
                            if (released) {
                                if (!currentIsBurstCapturing) {
                                    currentOnClick()
                                } else {
                                    currentOnBurstStop()
                                }
                            } else {
                                currentOnBurstStop()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .background(if (enabled) Color.Red else Color.Gray, CircleShape)
            )
        }
    }
}
