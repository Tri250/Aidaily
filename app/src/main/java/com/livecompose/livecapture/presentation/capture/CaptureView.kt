package com.livecompose.livecapture.presentation.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.livecompose.livecapture.core.design.*
import com.livecompose.livecapture.presentation.Screen
import java.io.File
import kotlin.math.abs

@Composable
fun CaptureView(
    viewModel: CaptureViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val stage by viewModel.pipelineStage.collectAsState()
    val guidanceText by viewModel.guidanceText.collectAsState()
    val trackPoint by viewModel.trackPoint.collectAsState()
    val isAligned by viewModel.isAligned.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val isTorchEnabled by viewModel.isTorchEnabled.collectAsState()
    val inferenceTime by viewModel.inferenceTime.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()
    val currentScore by viewModel.currentScore.collectAsState()
    val lastSavedPhotoPath by viewModel.lastSavedPhotoPath.collectAsState()
    val exposureCompensation by viewModel.exposureCompensation.collectAsState()
    val isCameraStarting by viewModel.isCameraStarting.collectAsState()
    val cameraError by viewModel.cameraError.collectAsState()

    // #7: 权限状态管理
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasBeenDenied by remember { mutableStateOf(false) }

    var showExposureSlider by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            hasBeenDenied = true
        }
    }

    // #8: 判断是否需要显示 Rationale
    val activity = context as? android.app.Activity
    val shouldShowRationale = remember(hasBeenDenied) {
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ?: false
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
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // #56/#17: ERROR 状态 — 显示错误重试 UI
            stage == CaptureViewModel.PipelineStage.ERROR || cameraError != null -> {
                CameraErrorContent(
                    errorMessage = cameraError ?: "拍摄流程发生错误",
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
                // Camera Preview — 点击对焦
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            androidx.compose.foundation.gestures.detectTapGestures(
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

                // Composition Grid Overlay
                CompositionGridOverlay()

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
                    inferenceTime = inferenceTime,
                    currentScore = currentScore,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                )

                // Exposure Slider Panel
                if (showExposureSlider) {
                    ExposureSliderPanel(
                        value = exposureCompensation,
                        onValueChange = { viewModel.setExposureCompensation(it) },
                        onDismiss = { showExposureSlider = false },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }

                // #55: 加载状态指示
                if (isCameraStarting || !isModelReady) {
                    LoadingOverlay(
                        text = if (isCameraStarting) "启动相机中..." else "加载 AI 模型中...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Bottom Controls — #54: 刘海屏适配
                BottomControls(
                    zoomRatio = zoomRatio,
                    isTorchEnabled = isTorchEnabled,
                    stage = stage,
                    lastSavedPhotoPath = lastSavedPhotoPath,
                    onZoomChange = { viewModel.setZoom(it) },
                    onSwitchCamera = { viewModel.switchCamera(lifecycleOwner, previewView) },
                    onCapture = { viewModel.manualCapture() },
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要相机权限",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (shouldShowRationale || !hasBeenDenied) {
                "构妙 LiveCapture 需要相机权限来实现实时构图辅助拍摄，请授权使用相机。"
            } else {
                "相机权限已被永久拒绝，请在设置中手动开启权限后返回。"
            },
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (shouldShowRationale || !hasBeenDenied) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("授予权限", color = Color.White)
            }
        } else {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("打开设置", color = Color.White)
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "发生错误",
            color = ErrorColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = errorMessage,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("重试", color = Color.White)
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
        color = GuidanceBg.copy(alpha = 0.9f),
        shape = RoundedCornerShape(CornerMedium),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                style = GuidanceTextStyle.copy(fontSize = 14.sp)
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
            animation = tween(800, easing = FastOutSlowInEasing),
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

@Composable
private fun TopControlBar(
    guidanceText: String,
    isAligned: Boolean,
    isModelReady: Boolean,
    inferenceTime: Long,
    currentScore: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isModelReady) {
            Surface(
                color = ErrorColor.copy(alpha = 0.8f),
                shape = RoundedCornerShape(CornerMedium)
            ) {
                Text(
                    text = "AI 模型加载中，使用默认构图",
                    style = GuidanceTextStyle.copy(fontSize = 12.sp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (guidanceText.isNotEmpty()) {
            Surface(
                color = GuidanceBg,
                shape = RoundedCornerShape(CornerMedium)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = guidanceText,
                        style = GuidanceTextStyle
                    )
                    if (inferenceTime > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${inferenceTime}ms",
                            color = Color.White.copy(alpha = 0.6f),
                            style = GuidanceTextStyle.copy(fontSize = 12.sp)
                        )
                    }
                    if (currentScore > 0f) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "评分 ${String.format("%.2f", currentScore)}",
                            color = if (currentScore > 0.6f) TrackingDotAligned else Color.White.copy(alpha = 0.6f),
                            style = GuidanceTextStyle.copy(fontSize = 12.sp)
                        )
                    }
                }
            }
        }

        if (isAligned) {
            Text(
                text = "对齐完美！",
                color = TrackingDotAligned,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ExposureSliderPanel(
    value: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = GuidanceBg,
        shape = RoundedCornerShape(CornerMedium),
        modifier = modifier.padding(end = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EV $value",
                color = Color.White,
                style = GuidanceTextStyle.copy(fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = -4f..4f,
                steps = 7,
                modifier = Modifier.height(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BottomControls(
    zoomRatio: Float,
    isTorchEnabled: Boolean,
    stage: CaptureViewModel.PipelineStage,
    lastSavedPhotoPath: String?,
    onZoomChange: (Float) -> Unit,
    onSwitchCamera: () -> Unit,
    onCapture: () -> Unit,
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
            .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZoomButton("0.5x", abs(zoomRatio - 0.5f) < 0.01f) { onZoomChange(0.5f) }
            ZoomButton("1x", abs(zoomRatio - 1.0f) < 0.01f) { onZoomChange(1.0f) }
            ZoomButton("2x", abs(zoomRatio - 2.0f) < 0.01f) { onZoomChange(2.0f) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Capture Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lastSavedPhotoPath != null) {
                LastPhotoThumbnail(
                    photoPath = lastSavedPhotoPath,
                    onClick = onGalleryClick
                )
            } else {
                IconButton(onClick = onGalleryClick) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            CaptureButton(
                enabled = isCaptureEnabled,
                stage = stage,
                onClick = onCapture
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onSwitchCamera) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Row {
                    IconButton(onClick = onTorchToggle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isTorchEnabled) Color.Yellow else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onExposureClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Exposure,
                            contentDescription = "Exposure",
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
            .clip(RoundedCornerShape(8.dp))
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
    onClick: () -> Unit
) {
    val ringColor = when (stage) {
        CaptureViewModel.PipelineStage.READY_TO_CAPTURE -> TrackingDotAligned
        CaptureViewModel.PipelineStage.CAPTURING_PHOTO -> Color.Yellow
        CaptureViewModel.PipelineStage.SAVING_PHOTO -> Color.Cyan
        else -> Color.White
    }

    val alpha = if (enabled || stage == CaptureViewModel.PipelineStage.IDLE) 1f else 0.4f

    Box(
        modifier = Modifier
            .size(80.dp)
            .background(ringColor, CircleShape)
            .padding(4.dp)
            .alpha(alpha)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .background(if (enabled) Color.Red else Color.Gray, CircleShape)
        )
    }
}
