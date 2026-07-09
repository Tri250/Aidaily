package com.livecompose.livecapture.features.capture

import android.content.Intent
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.CameraPreview
import com.livecompose.livecapture.core.camera.CameraErrorType
import com.livecompose.livecapture.core.camera.ZoomPreset
import com.livecompose.livecapture.core.camera.ZoomState
import com.livecompose.livecapture.features.capture.components.*
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 主拍摄界面 - 魅族 Flyme 极简风格 2026 高端摄影体验
 *
 * 设计原则：
 *  - 全屏取景，最小化 UI 干扰
 *  - 半透明控件层，仅在需要时浮现
 *  - 流畅弹性动画，对标 iOS spring 动效
 *  - 手势优先：点按对焦、滑动变焦、长按连拍
 *  - AI 构图引导，优雅的文字提示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onNavigateToGallery: () -> Unit = {},
    viewModel: CaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    var showTopControls by remember { mutableStateOf(true) }
    var showBottomControls by remember { mutableStateOf(true) }
    var captureAnimationScale by remember { mutableFloatStateOf(1f) }
    var captureFlashOpacity by remember { mutableFloatStateOf(0f) }
    var cameraFlipRotation by remember { mutableFloatStateOf(0f) }

    val zoomState by viewModel.zoomState.collectAsState()
    val zoomPresets by viewModel.zoomPresets.collectAsState()
    val zoomRange by viewModel.zoomRange.collectAsState()
    val cropRect by viewModel.cropRectInView.collectAsState()
    val boxCenter by viewModel.boxCenterManager.currentCenterInView.collectAsState()
    val isAligned by viewModel.isAligned.collectAsState()
    val distanceToCenter by viewModel.distanceToCenter.collectAsState()
    val userGuidanceText by viewModel.userGuidanceText.collectAsState()
    val isPipelineEnabled by viewModel.isCompositionPipelineEnabled.collectAsState()
    val isAutoCapture by viewModel.isAutoCaptureEnabled.collectAsState()
    val captureDelay by viewModel.captureDelay.collectAsState()
    val debugMessage by viewModel.debugMessage.collectAsState()
    val motionStable by viewModel.motionIsStable.collectAsState()
    val detectionReady by viewModel.detectionReady.collectAsState()
    val isFrontCamera = viewModel.camera.isFrontCamera
    val cameraErrorState by viewModel.camera.cameraError.collectAsState()

    var cameraError by remember { mutableStateOf<CameraErrorType?>(null) }

    // 同步 ViewModel 的相机错误
    LaunchedEffect(cameraErrorState) {
        cameraError = cameraErrorState
    }

    // 权限请求 launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraError = null
            viewModel.camera.openCamera()
        } else {
            cameraError = CameraErrorType.PERMISSION_DENIED
        }
    }

    // 控件自动隐藏定时器
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && cameraError == null) {
            delay(5000)
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onAppear()
        viewModel.onCaptureTriggered = {
            captureFlashOpacity = 0.8f
            captureAnimationScale = 0.92f
        }
        if (!viewModel.camera.hasCameraPermission()) {
            cameraError = CameraErrorType.PERMISSION_DENIED
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onDisappear() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible }
                )
            }
    ) {
        // 相机预览
        if (cameraError == null) {
            // 缩放动画 - 拍照时视觉反馈
            val animatedScale by animateFloatAsState(
                targetValue = captureAnimationScale,
                animationSpec = DesignSystem.Animation.shutterPress,
                label = "captureScale"
            )
            CameraPreview(
                cameraManager = viewModel.camera,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(animatedScale)
                    .graphicsLayer {
                        rotationY = cameraFlipRotation
                        cameraDistance = 8f * density
                    },
                isFrontCamera = isFrontCamera
            )
        }

        // 构图叠加层
        if (cameraError == null) {
            CompositionOverlay(
                compositionRect = viewModel.compositionRectInView.collectAsState().value,
                cropRect = cropRect,
                boxCenter = boxCenter,
                isAligned = isAligned,
                distanceToCenter = distanceToCenter
            )
        }

        // 拍照闪光
        AnimatedVisibility(
            visible = captureFlashOpacity > 0f,
            enter = fadeIn(animationSpec = tween(80)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = captureFlashOpacity))
            )
        }

        // 相机错误覆盖层
        cameraError?.let { error ->
            CameraErrorOverlay(
                errorType = error,
                onRetry = {
                    cameraError = null
                    if (viewModel.camera.hasCameraPermission()) {
                        viewModel.camera.openCamera()
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                onGoToSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 顶部控制栏 - 淡入淡出
        AnimatedVisibility(
            visible = controlsVisible && cameraError == null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CaptureTopBar(
                userGuidanceText = userGuidanceText,
                isAutoCaptureEnabled = isAutoCapture,
                captureDelay = captureDelay,
                onToggleCamera = {
                    cameraFlipRotation += 180f
                    viewModel.toggleCameraPosition()
                },
                onToggleAutoCapture = { viewModel.toggleAutoCapture() },
                onSetCaptureDelay = { viewModel.setCaptureDelay(it) }
            )
        }

        // 底部控制区 - 淡入淡出
        AnimatedVisibility(
            visible = controlsVisible && cameraError == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CaptureBottomBar(
                zoomPresets = zoomPresets,
                zoomRange = zoomRange,
                zoomState = zoomState,
                isPipelineEnabled = isPipelineEnabled,
                onSelectPreset = { viewModel.selectZoomPreset(it) },
                onZoomDrag = { viewModel.updateZoomInteractively(it) },
                onZoomDragEnd = { viewModel.finalizeZoomInteractively(it) },
                onTogglePipeline = { viewModel.toggleCompositionPipeline() },
                onCapture = { viewModel.capturePhoto() },
                onToggleCamera = {
                    cameraFlipRotation += 180f
                    viewModel.toggleCameraPosition()
                },
                onNavigateToGallery = onNavigateToGallery
            )
        }
    }
}

/**
 * 顶部控制栏 - 魅族极简风格
 */
@Composable
private fun CaptureTopBar(
    userGuidanceText: String,
    isAutoCaptureEnabled: Boolean,
    captureDelay: Double,
    onToggleCamera: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    onSetCaptureDelay: (Double) -> Unit
) {
    var showCaptureMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧: 引导文字
        if (userGuidanceText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DesignSystem.Colors.minimalDarkOverlay)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    userGuidanceText,
                    color = DesignSystem.Colors.minimalLabel,
                    style = DesignSystem.Typography.minimalControlLabel,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 右侧: 功能按钮组
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // 翻转摄像头
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DesignSystem.Colors.minimalDarkOverlay)
                    .clickable { onToggleCamera() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = "翻转",
                    tint = DesignSystem.Colors.minimalLabel,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 定时器
            Box {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DesignSystem.Colors.minimalDarkOverlay)
                        .clickable { showCaptureMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "定时",
                        tint = if (isAutoCaptureEnabled) DesignSystem.Colors.primary
                        else DesignSystem.Colors.minimalLabel,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showCaptureMenu,
                    onDismissRequest = { showCaptureMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "自动拍照: ${if (isAutoCaptureEnabled) "开" else "关"}",
                                style = DesignSystem.Typography.subheadline
                            )
                        },
                        onClick = { onToggleAutoCapture(); showCaptureMenu = false },
                        leadingIcon = {
                            Icon(
                                if (isAutoCaptureEnabled) Icons.Default.CheckCircle else Icons.Default.Circle,
                                contentDescription = null
                            )
                        }
                    )
                    HorizontalDivider()
                    Text(
                        "拍照延迟",
                        style = DesignSystem.Typography.caption1,
                        color = DesignSystem.Colors.textTertiary(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    listOf(0.25, 0.5, 1.0, 1.5, 2.0).forEach { delay ->
                        DropdownMenuItem(
                            text = { Text("${"%.1f".format(delay)}秒") },
                            onClick = { onSetCaptureDelay(delay); showCaptureMenu = false },
                            leadingIcon = {
                                if (delay == captureDelay)
                                    Icon(Icons.Default.Check, null, tint = DesignSystem.Colors.primary)
                                else Spacer(modifier = Modifier.size(24.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 底部控制栏 - 魅族极简风格重新设计
 * 对标 2026 高端手机摄影交互：大快门 + 简洁功能按钮
 */
@Composable
private fun CaptureBottomBar(
    zoomPresets: List<ZoomPreset>,
    zoomRange: ClosedFloatingPointRange<Float>,
    zoomState: ZoomState,
    isPipelineEnabled: Boolean,
    onSelectPreset: (ZoomPreset) -> Unit,
    onZoomDrag: (Float) -> Unit,
    onZoomDragEnd: (Float) -> Unit,
    onTogglePipeline: () -> Unit,
    onCapture: () -> Unit,
    onToggleCamera: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        // 变焦预设条
        if (zoomPresets.isNotEmpty()) {
            ZoomPresetBar(
                zoomPresets = zoomPresets,
                zoomState = zoomState,
                onSelectPreset = onSelectPreset
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 变焦滑块
        if (zoomRange.endInclusive > zoomRange.start) {
            ZoomSlider(
                zoomRange = zoomRange,
                zoomState = zoomState,
                onZoomDrag = onZoomDrag,
                onZoomDragEnd = onZoomDragEnd
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 主控制行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧: 图库入口
            GalleryThumbnailButton(onClick = onNavigateToGallery)

            // 中间: AI构图按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(0.5f)
            ) {
                var scale by remember { mutableFloatStateOf(1f) }
                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = DesignSystem.Animation.quick
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(
                            if (isPipelineEnabled) DesignSystem.Colors.primary.copy(alpha = 0.25f)
                            else DesignSystem.Colors.minimalOverlay
                        )
                        .clickable {
                            scale = 0.85f
                            onTogglePipeline()
                            scale = 1f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI构图",
                        tint = if (isPipelineEnabled) DesignSystem.Colors.primary
                        else DesignSystem.Colors.minimalSecondaryLabel,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (isPipelineEnabled) "AI构图" else "构图",
                    style = DesignSystem.Typography.minimalModeLabel,
                    color = if (isPipelineEnabled) DesignSystem.Colors.primary
                    else DesignSystem.Colors.minimalSecondaryLabel
                )
            }

            // 中间: 快门按钮
            ShutterButton(onCapture = onCapture)

            // 右侧: 翻转摄像头
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DesignSystem.Colors.minimalOverlay)
                        .clickable { onToggleCamera() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "翻转",
                        tint = DesignSystem.Colors.minimalLabel,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "翻转",
                    style = DesignSystem.Typography.minimalModeLabel,
                    color = DesignSystem.Colors.minimalSecondaryLabel
                )
            }
        }
    }
}

/**
 * 快门按钮 - 大圆形设计，环形+实心内圈
 */
@Composable
private fun ShutterButton(onCapture: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = DesignSystem.Animation.shutterPress,
        label = "shutterScale"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(DesignSystem.Colors.minimalBorder)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                pressed = true
                onCapture()
                pressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        // 内圈 - 纯白
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DesignSystem.Colors.shutterInner)
        )
    }
}

/**
 * 图库缩略图按钮
 */
@Composable
private fun GalleryThumbnailButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(0.5f)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DesignSystem.Colors.minimalOverlay)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = "图库",
                tint = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "图库",
            style = DesignSystem.Typography.minimalModeLabel,
            color = DesignSystem.Colors.minimalSecondaryLabel
        )
    }
}

/**
 * 变焦预设条
 */
@Composable
private fun ZoomPresetBar(
    zoomPresets: List<ZoomPreset>,
    zoomState: ZoomState,
    onSelectPreset: (ZoomPreset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        zoomPresets.forEach { preset ->
            val isActive = kotlin.math.abs(preset.zoomFactor - zoomState.currentFactor) < 0.05f
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isActive) DesignSystem.Colors.minimalBorder
                        else Color.Transparent
                    )
                    .clickable { onSelectPreset(preset) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    preset.label,
                    style = DesignSystem.Typography.minimalControlLabel,
                    color = if (isActive) DesignSystem.Colors.minimalLabel
                    else DesignSystem.Colors.minimalSecondaryLabel,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 变焦滑块
 */
@Composable
private fun ZoomSlider(
    zoomRange: ClosedFloatingPointRange<Float>,
    zoomState: ZoomState,
    onZoomDrag: (Float) -> Unit,
    onZoomDragEnd: (Float) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(zoomState.currentFactor) }
    var isDragging by remember { mutableStateOf(false) }

    Slider(
        value = if (isDragging) sliderPosition else zoomState.currentFactor,
        onValueChange = { value ->
            sliderPosition = value
            isDragging = true
            onZoomDrag(value)
        },
        onValueChangeFinished = {
            isDragging = false
            onZoomDragEnd(sliderPosition)
        },
        valueRange = zoomRange.start..zoomRange.endInclusive,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        colors = SliderDefaults.colors(
            thumbColor = DesignSystem.Colors.minimalLabel,
            activeTrackColor = DesignSystem.Colors.minimalLabel,
            inactiveTrackColor = DesignSystem.Colors.minimalBorder
        )
    )
}

/**
 * 相机错误覆盖层
 */
@Composable
private fun CameraErrorOverlay(
    errorType: CameraErrorType,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = when (errorType) {
                    CameraErrorType.PERMISSION_DENIED -> Icons.Default.Lock
                    CameraErrorType.CAMERA_IN_USE -> Icons.Default.Sync
                    CameraErrorType.NO_CAMERA_HARDWARE -> Icons.Default.CameraAlt
                    CameraErrorType.CAMERA_DISCONNECTED -> Icons.Default.Cable
                    CameraErrorType.SESSION_CONFIG_FAILED -> Icons.Default.ErrorOutline
                    CameraErrorType.UNKNOWN -> Icons.Default.ErrorOutline
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = DesignSystem.Colors.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when (errorType) {
                    CameraErrorType.PERMISSION_DENIED -> "相机权限被拒绝"
                    CameraErrorType.CAMERA_IN_USE -> "相机被占用"
                    CameraErrorType.NO_CAMERA_HARDWARE -> "无可用相机"
                    CameraErrorType.CAMERA_DISCONNECTED -> "相机已断开"
                    CameraErrorType.SESSION_CONFIG_FAILED -> "相机配置失败"
                    CameraErrorType.UNKNOWN -> "相机打开失败"
                },
                style = DesignSystem.Typography.title2,
                fontWeight = FontWeight.Bold,
                color = DesignSystem.Colors.minimalLabel,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (errorType) {
                    CameraErrorType.PERMISSION_DENIED -> "需要相机权限才能拍摄照片，请在系统设置中授权"
                    CameraErrorType.CAMERA_IN_USE -> "相机正在被其他应用使用，请关闭后重试"
                    CameraErrorType.NO_CAMERA_HARDWARE -> "设备未检测到相机硬件"
                    CameraErrorType.CAMERA_DISCONNECTED -> "相机连接已断开，请重试"
                    CameraErrorType.SESSION_CONFIG_FAILED -> "相机预览配置失败，请重试"
                    CameraErrorType.UNKNOWN -> "相机无法正常启动，请尝试重启应用"
                },
                style = DesignSystem.Typography.subheadline,
                color = DesignSystem.Colors.minimalSecondaryLabel,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesignSystem.Colors.primary
                )
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("重试", style = DesignSystem.Typography.headline)
            }

            if (errorType == CameraErrorType.PERMISSION_DENIED) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onGoToSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DesignSystem.Colors.minimalLabel
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DesignSystem.Colors.minimalBorder)
                    )
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("去设置", style = DesignSystem.Typography.headline)
                }
            }
        }
    }
}

@Composable
private fun CompositionOverlay(
    compositionRect: RectF,
    cropRect: RectF?,
    boxCenter: PointF?,
    isAligned: Boolean,
    distanceToCenter: Float?
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        cropRect?.let { rect ->
            val left = rect.left * canvasWidth
            val top = rect.top * canvasHeight
            val right = rect.right * canvasWidth
            val bottom = rect.bottom * canvasHeight
            drawRect(
                color = if (isAligned) DesignSystem.Colors.success else DesignSystem.Colors.warning,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 2.5f)
            )
        }

        boxCenter?.let { center ->
            val cx = center.x * canvasWidth
            val cy = center.y * canvasHeight
            drawCircle(
                color = if (isAligned) DesignSystem.Colors.success else DesignSystem.Colors.minimalLabel,
                radius = 12f,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )
            drawCircle(
                color = DesignSystem.Colors.primary.copy(alpha = 0.25f),
                radius = 20f,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = Stroke(width = 2f)
            )
        }
    }
}