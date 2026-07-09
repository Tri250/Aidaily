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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.features.capture.components.*
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 主拍摄界面 - 2026 国内旗舰手机摄影体验
 * 集成: 点按对焦/测光、双指变焦、AE/AF锁定、闪光灯、拍摄比例、直方图、斑马纹、水平仪、手动控制面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onNavigateToGallery: () -> Unit = {},
    viewModel: CaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val camera = viewModel.camera

    // 控件可见性
    var controlsVisible by remember { mutableStateOf(true) }
    var showManualPanel by remember { mutableStateOf(false) }
    var showPhotoReview by remember { mutableStateOf(false) }
    var reviewData by remember { mutableStateOf<ByteArray?>(null) }

    // 动画状态
    var captureAnimationScale by remember { mutableFloatStateOf(1f) }
    var captureFlashOpacity by remember { mutableFloatStateOf(0f) }
    var cameraFlipRotation by remember { mutableFloatStateOf(0f) }

    // 对焦点
    var focusPoint by remember { mutableStateOf<PointF?>(null) }
    var focusAnimation by remember { mutableFloatStateOf(0f) }

    // 手动变焦
    var pinchZoom by remember { mutableFloatStateOf(1f) }

    // CameraManager 状态
    val zoomState by camera.zoomState.collectAsState()
    val zoomPresets by camera.zoomPresets.collectAsState()
    val zoomRange by camera.zoomRange.collectAsState()
    val flashMode by camera.flashMode.collectAsState()
    val aeLocked by camera.aeLocked.collectAsState()
    val afLocked by camera.afLocked.collectAsState()
    val focusState by camera.focusState.collectAsState()
    val aspectRatio by camera.aspectRatio.collectAsState()
    val cameraErrorState by camera.cameraError.collectAsState()

    // ViewModel 状态
    val cropRect by viewModel.cropRectInView.collectAsState()
    val boxCenter by viewModel.boxCenterManager.currentCenterInView.collectAsState()
    val isAligned by viewModel.isAligned.collectAsState()
    val userGuidanceText by viewModel.userGuidanceText.collectAsState()
    val isPipelineEnabled by viewModel.isCompositionPipelineEnabled.collectAsState()
    val isAutoCapture by viewModel.isAutoCaptureEnabled.collectAsState()
    val motionStable by viewModel.motionIsStable.collectAsState()
    val isFrontCamera = camera.isFrontCamera

    var cameraError by remember { mutableStateOf<CameraErrorType?>(null) }

    // 模式切换
    var captureMode by remember { mutableIntStateOf(0) }

    // 网格模式
    var gridMode by remember { mutableIntStateOf(0) }

    // 显示直方图/斑马纹/水平仪
    var showHistogram by remember { mutableStateOf(false) }
    var showZebra by remember { mutableStateOf(false) }
    var showLevel by remember { mutableStateOf(false) }

    LaunchedEffect(cameraErrorState) { cameraError = cameraErrorState }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) { cameraError = null; camera.openCamera() }
        else { cameraError = CameraErrorType.PERMISSION_DENIED }
    }

    // 控件自动隐藏
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && cameraError == null && !showManualPanel) {
            delay(4000)
            controlsVisible = false
        }
    }

    // 对焦动画
    LaunchedEffect(focusAnimation) {
        if (focusAnimation > 0f) {
            delay(1000)
            focusAnimation = 0f
            focusPoint = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onAppear()
        viewModel.onCaptureTriggered = {
            captureFlashOpacity = 0.8f
            captureAnimationScale = 0.92f
        }
        if (!camera.hasCameraPermission()) {
            cameraError = CameraErrorType.PERMISSION_DENIED
        }
    }

    DisposableEffect(Unit) { onDispose { viewModel.onDisappear() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
            // 点击切换控件显示，点按对焦
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        controlsVisible = !controlsVisible
                        // 点按对焦
                        val sensorRect = camera.getSensorRect()
                        if (sensorRect != null) {
                            camera.tapToFocus(offset.x / size.width, offset.y / size.height, sensorRect)
                            focusPoint = PointF(offset.x, offset.y)
                            focusAnimation = 1f
                        }
                    },
                    onLongPress = { offset ->
                        // 长按 AE/AF 锁定
                        camera.toggleAELock()
                        camera.toggleAFLock()
                        focusPoint = PointF(offset.x, offset.y)
                        focusAnimation = 1f
                    }
                )
            }
            // 双指缩放变焦
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    pinchZoom = (pinchZoom * zoom).coerceIn(zoomRange.start, zoomRange.endInclusive)
                    camera.updateInteractiveZoom(pinchZoom)
                }
            }
    ) {
        // 相机预览
        if (cameraError == null) {
            val animatedScale by animateFloatAsState(
                captureAnimationScale,
                DesignSystem.Animation.shutterPress,
                "captureScale"
            )
            CameraPreview(
                cameraManager = camera,
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
                isAligned = isAligned
            )
        }

        // 网格线
        if (gridMode > 0 && cameraError == null) {
            GridOverlayView(mode = gridMode)
        }

        // 水平仪
        if (showLevel && cameraError == null) {
            LevelIndicatorOverlayView()
        }

        // 点按对焦指示器
        focusPoint?.let { point ->
            val scale by animateFloatAsState(
                if (focusAnimation > 0f) 1f else 0f,
                DesignSystem.Animation.quick
            )
            FocusIndicatorView(point.x, point.y, scale, focusState)
        }

        // 拍照闪光
        AnimatedVisibility(
            visible = captureFlashOpacity > 0f,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(300))
        ) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = captureFlashOpacity)))
        }

        // 相机错误覆盖层
        cameraError?.let { error ->
            CameraErrorOverlay(error, {
                cameraError = null
                if (camera.hasCameraPermission()) camera.openCamera()
                else permissionLauncher.launch(android.Manifest.permission.CAMERA)
            }, {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                })
            })
        }

        // 拍照后即时预览
        if (showPhotoReview) {
            PhotoReviewOverlay(
                data = reviewData,
                onAccept = { showPhotoReview = false },
                onDelete = { showPhotoReview = false } // 实际删除逻辑
            )
        }

        // 手动控制面板
        if (showManualPanel && cameraError == null) {
            ManualControlPanelOverlay(
                onDismiss = { showManualPanel = false }
            )
        }

        // 顶部控制栏
        AnimatedVisibility(
            visible = controlsVisible && cameraError == null && !showManualPanel,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CaptureTopBar(
                userGuidanceText = userGuidanceText,
                isAutoCaptureEnabled = isAutoCapture,
                onToggleCamera = { cameraFlipRotation += 180f; viewModel.toggleCameraPosition() },
                onToggleAutoCapture = { viewModel.toggleAutoCapture() },
                flashMode = flashMode,
                onToggleFlash = { camera.toggleFlashMode() },
                gridMode = gridMode,
                onToggleGrid = { gridMode = (gridMode + 1) % 4 },
                showHistogram = showHistogram,
                onToggleHistogram = { showHistogram = !showHistogram },
                showZebra = showZebra,
                onToggleZebra = { showZebra = !showZebra },
                showLevel = showLevel,
                onToggleLevel = { showLevel = !showLevel }
            )
        }

        // 底部控制栏
        AnimatedVisibility(
            visible = controlsVisible && cameraError == null && !showManualPanel,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CaptureBottomBar(
                zoomPresets = zoomPresets,
                zoomRange = zoomRange,
                zoomState = zoomState,
                isPipelineEnabled = isPipelineEnabled,
                aeLocked = aeLocked,
                afLocked = afLocked,
                aspectRatio = aspectRatio,
                captureMode = captureMode,
                onSelectPreset = { viewModel.selectZoomPreset(it) },
                onZoomDrag = { viewModel.updateZoomInteractively(it) },
                onZoomDragEnd = { viewModel.finalizeZoomInteractively(it) },
                onTogglePipeline = { viewModel.toggleCompositionPipeline() },
                onCapture = { viewModel.capturePhoto() },
                onToggleCamera = { cameraFlipRotation += 180f; viewModel.toggleCameraPosition() },
                onNavigateToGallery = onNavigateToGallery,
                onToggleAELock = { camera.toggleAELock() },
                onToggleAFLock = { camera.toggleAFLock() },
                onToggleAspectRatio = {
                    val ratios = AspectRatio.entries
                    val nextIndex = (ratios.indexOf(aspectRatio) + 1) % ratios.size
                    camera.setAspectRatio(ratios[nextIndex])
                },
                onToggleManualPanel = { showManualPanel = true },
                onCaptureModeChange = { captureMode = it }
            )
        }

        // 直方图浮层
        if (showHistogram && cameraError == null) {
            HistogramOverlayView(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 12.dp)
            )
        }

        // 斑马纹浮层
        if (showZebra && cameraError == null) {
            ZebraOverlayView()
        }
    }
}

// ====== 顶部控制栏 ======
@Composable
private fun CaptureTopBar(
    userGuidanceText: String,
    isAutoCaptureEnabled: Boolean,
    onToggleCamera: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    flashMode: FlashMode,
    onToggleFlash: () -> Unit,
    gridMode: Int,
    onToggleGrid: () -> Unit,
    showHistogram: Boolean,
    onToggleHistogram: () -> Unit,
    showZebra: Boolean,
    onToggleZebra: () -> Unit,
    showLevel: Boolean,
    onToggleLevel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 引导文字
        if (userGuidanceText.isNotEmpty()) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(DesignSystem.Colors.minimalDarkOverlay)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(userGuidanceText, color = DesignSystem.Colors.minimalLabel,
                    style = DesignSystem.Typography.caption1, maxLines = 1)
            }
        }

        Spacer(Modifier.weight(1f))

        // 功能按钮组
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // 水平仪
            TopIconButton(Icons.Default.AlignHorizontalLeft, showLevel, onToggleLevel)
            // 斑马纹
            TopIconButton(Icons.Default.GridOn, showZebra, onToggleZebra)
            // 直方图
            TopIconButton(Icons.Default.BarChart, showHistogram, onToggleHistogram)
            // 网格
            TopIconButton(Icons.Default.Grid4x4, gridMode > 0, onToggleGrid)
            // 闪光灯
            TopIconButton(flashModeIcon(flashMode), flashMode != FlashMode.OFF, onToggleFlash)
            // 翻转
            TopIconButton(Icons.Default.FlipCameraAndroid, false, onToggleCamera)
        }
    }
}

@Composable
private fun TopIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (active) DesignSystem.Colors.primary.copy(alpha = 0.25f) else DesignSystem.Colors.minimalDarkOverlay)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) DesignSystem.Colors.primary
        else DesignSystem.Colors.minimalSecondaryLabel, modifier = Modifier.size(16.dp))
    }
}

private fun flashModeIcon(mode: FlashMode) = when (mode) {
    FlashMode.OFF -> Icons.Default.FlashOff
    FlashMode.AUTO -> Icons.Default.FlashAuto
    FlashMode.ON -> Icons.Default.FlashOn
    FlashMode.TORCH -> Icons.Default.Highlight
}

// ====== 底部控制栏 ======
@Composable
private fun CaptureBottomBar(
    zoomPresets: List<ZoomPreset>,
    zoomRange: ClosedFloatingPointRange<Float>,
    zoomState: ZoomState,
    isPipelineEnabled: Boolean,
    aeLocked: Boolean,
    afLocked: Boolean,
    aspectRatio: AspectRatio,
    captureMode: Int,
    onSelectPreset: (ZoomPreset) -> Unit,
    onZoomDrag: (Float) -> Unit,
    onZoomDragEnd: (Float) -> Unit,
    onTogglePipeline: () -> Unit,
    onCapture: () -> Unit,
    onToggleCamera: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onToggleAELock: () -> Unit,
    onToggleAFLock: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onToggleManualPanel: () -> Unit,
    onCaptureModeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        // 拍摄模式切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("拍照", "视频", "专业", "人像").forEachIndexed { index, label ->
                val isActive = captureMode == index
                Text(
                    label,
                    style = DesignSystem.Typography.subheadline,
                    color = if (isActive) DesignSystem.Colors.primary
                    else DesignSystem.Colors.minimalSecondaryLabel,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCaptureModeChange(index) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 变焦预设
        if (zoomPresets.isNotEmpty()) {
            ZoomPresetBar(zoomPresets, zoomState, onSelectPreset)
            Spacer(Modifier.height(8.dp))
        }

        // 主控制行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧: 图库
            GalleryThumbBtn(onClick = onNavigateToGallery)

            // AE/AF 锁
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(36.dp).clip(CircleShape)
                    .background(if (aeLocked) DesignSystem.Colors.accent.copy(alpha = 0.3f)
                    else DesignSystem.Colors.minimalOverlay)
                    .clickable { onToggleAELock() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, tint = if (aeLocked) DesignSystem.Colors.accent
                    else DesignSystem.Colors.minimalSecondaryLabel, modifier = Modifier.size(16.dp))
                }
                Text("AE", style = DesignSystem.Typography.caption2,
                    color = if (aeLocked) DesignSystem.Colors.accent else DesignSystem.Colors.minimalSecondaryLabel)
            }

            // 左侧: AI构图
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(36.dp).clip(CircleShape)
                    .background(if (isPipelineEnabled) DesignSystem.Colors.primary.copy(alpha = 0.25f)
                    else DesignSystem.Colors.minimalOverlay)
                    .clickable { onTogglePipeline() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = if (isPipelineEnabled) DesignSystem.Colors.primary
                    else DesignSystem.Colors.minimalSecondaryLabel, modifier = Modifier.size(18.dp))
                }
                Text("构图", style = DesignSystem.Typography.caption2,
                    color = if (isPipelineEnabled) DesignSystem.Colors.primary else DesignSystem.Colors.minimalSecondaryLabel)
            }

            // 快门
            ShutterButton(onCapture = onCapture)

            // 比例切换
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(DesignSystem.Colors.minimalOverlay)
                    .clickable { onToggleAspectRatio() }, contentAlignment = Alignment.Center) {
                    Text(aspectRatio.displayName, color = DesignSystem.Colors.minimalLabel, fontSize = 10.sp)
                }
                Text("比例", style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.minimalSecondaryLabel)
            }

            // 手动模式
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(36.dp).clip(CircleShape)
                    .background(DesignSystem.Colors.minimalOverlay)
                    .clickable { onToggleManualPanel() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tune, null, tint = DesignSystem.Colors.minimalSecondaryLabel, modifier = Modifier.size(18.dp))
                }
                Text("手动", style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.minimalSecondaryLabel)
            }

            // 翻转
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(36.dp).clip(CircleShape)
                    .background(DesignSystem.Colors.minimalOverlay)
                    .clickable { onToggleCamera() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FlipCameraAndroid, null, tint = DesignSystem.Colors.minimalSecondaryLabel, modifier = Modifier.size(18.dp))
                }
                Text("翻转", style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.minimalSecondaryLabel)
            }
        }
    }
}

// ====== 辅助组件 ======

@Composable
private fun ZoomPresetBar(presets: List<ZoomPreset>, state: ZoomState, onSelect: (ZoomPreset) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        presets.forEach { preset ->
            val isActive = kotlin.math.abs(preset.zoomFactor - state.currentFactor) < 0.05f
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) DesignSystem.Colors.minimalBorder else Color.Transparent)
                    .clickable { onSelect(preset) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(preset.label, color = if (isActive) DesignSystem.Colors.minimalLabel
                else DesignSystem.Colors.minimalSecondaryLabel, style = DesignSystem.Typography.caption1)
            }
        }
    }
}

@Composable
private fun ShutterButton(onCapture: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, DesignSystem.Animation.shutterPress)
    Box(Modifier.size(72.dp).scale(scale).clip(CircleShape)
        .background(DesignSystem.Colors.minimalBorder)
        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
            pressed = true; onCapture(); pressed = false
        }, contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(DesignSystem.Colors.shutterInner))
    }
}

@Composable
private fun GalleryThumbBtn(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(DesignSystem.Colors.minimalOverlay).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhotoLibrary, null, tint = DesignSystem.Colors.minimalLabel, modifier = Modifier.size(18.dp))
        }
        Text("图库", style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.minimalSecondaryLabel)
    }
}

// ====== 叠加层组件 ======

@Composable
private fun FocusIndicatorView(x: Float, y: Float, scale: Float, state: FocusState) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = scale; scaleY = scale; translationX = x - size.width / 2; translationY = y - size.height / 2 }
    ) {
        val color = when (state) {
            FocusState.FOCUSED -> DesignSystem.Colors.success
            FocusState.FAILED -> DesignSystem.Colors.error
            else -> DesignSystem.Colors.primary
        }
        drawCircle(color = color.copy(alpha = 0.3f), radius = 30f)
        drawCircle(color = color, radius = 30f, style = Stroke(2f))
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(-40f, 0f), end = androidx.compose.ui.geometry.Offset(-15f, 0f), strokeWidth = 2f)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(15f, 0f), end = androidx.compose.ui.geometry.Offset(40f, 0f), strokeWidth = 2f)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, -40f), end = androidx.compose.ui.geometry.Offset(0f, -15f), strokeWidth = 2f)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, 15f), end = androidx.compose.ui.geometry.Offset(0f, 40f), strokeWidth = 2f)
    }
}

@Composable
private fun GridOverlayView(mode: Int) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val color = DesignSystem.Colors.minimalLabel.copy(alpha = 0.15f)
        when (mode) {
            1 -> { // 三分法
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width/3, 0f), end = androidx.compose.ui.geometry.Offset(size.width/3, size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width*2/3, 0f), end = androidx.compose.ui.geometry.Offset(size.width*2/3, size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height/3), end = androidx.compose.ui.geometry.Offset(size.width, size.height/3), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height*2/3), end = androidx.compose.ui.geometry.Offset(size.width, size.height*2/3), strokeWidth = 1f)
            }
            2 -> { // 黄金分割
                val phi = 0.618f
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width*phi, 0f), end = androidx.compose.ui.geometry.Offset(size.width*phi, size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width*(1-phi), 0f), end = androidx.compose.ui.geometry.Offset(size.width*(1-phi), size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height*phi), end = androidx.compose.ui.geometry.Offset(size.width, size.height*phi), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height*(1-phi)), end = androidx.compose.ui.geometry.Offset(size.width, size.height*(1-phi)), strokeWidth = 1f)
            }
            3 -> { // 中心十字
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width/2, 0f), end = androidx.compose.ui.geometry.Offset(size.width/2, size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height/2), end = androidx.compose.ui.geometry.Offset(size.width, size.height/2), strokeWidth = 1f)
            }
        }
    }
}

@Composable
private fun LevelIndicatorOverlayView() {
    Box(Modifier.fillMaxSize().padding(top = 48.dp), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(DesignSystem.Colors.minimalDarkOverlay).padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("水平仪", color = DesignSystem.Colors.minimalLabel, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HistogramOverlayView(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(DesignSystem.Colors.minimalDarkOverlay).size(80.dp, 50.dp).padding(4.dp)) {
        Text("直方图", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 9.sp, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ZebraOverlayView() {
    Box(Modifier.fillMaxSize().padding(top = 48.dp), contentAlignment = Alignment.TopEnd) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(DesignSystem.Colors.minimalDarkOverlay).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("斑马纹", color = DesignSystem.Colors.warning, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PhotoReviewOverlay(data: ByteArray?, onAccept: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DesignSystem.Colors.minimalBackground)) {
        if (data != null) {
            val bitmap = remember(data) {
                android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
            }
            bitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = DesignSystem.Colors.error)
                Spacer(Modifier.width(4.dp))
                Text("删除", color = DesignSystem.Colors.error)
            }
            TextButton(onClick = onAccept) {
                Icon(Icons.Default.Check, null, tint = DesignSystem.Colors.success)
                Spacer(Modifier.width(4.dp))
                Text("保存", color = DesignSystem.Colors.success)
            }
        }
    }
}

@Composable
private fun ManualControlPanelOverlay(onDismiss: () -> Unit) {
    var params by remember { mutableStateOf(ManualControlParams()) }
    Box(Modifier.fillMaxSize().background(DesignSystem.Colors.minimalBackground.copy(alpha = 0.95f))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // 顶部关闭按钮
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("手动控制", color = DesignSystem.Colors.minimalLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onDismiss) { Text("完成", color = DesignSystem.Colors.primary) }
            }
            ManualControlPanel(params = params, onParamsChanged = { params = it })
        }
    }
}

@Composable
private fun CompositionOverlay(
    compositionRect: RectF,
    cropRect: RectF?,
    boxCenter: PointF?,
    isAligned: Boolean,
) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        cropRect?.let { rect ->
            drawRect(color = if (isAligned) DesignSystem.Colors.success else DesignSystem.Colors.warning,
                topLeft = androidx.compose.ui.geometry.Offset(rect.left * size.width, rect.top * size.height),
                size = androidx.compose.ui.geometry.Size(rect.width() * size.width, rect.height() * size.height),
                style = Stroke(2.5f))
        }
        boxCenter?.let { center ->
            drawCircle(color = if (isAligned) DesignSystem.Colors.success else DesignSystem.Colors.minimalLabel,
                radius = 12f, center = androidx.compose.ui.geometry.Offset(center.x * size.width, center.y * size.height))
            drawCircle(color = DesignSystem.Colors.primary.copy(alpha = 0.25f), radius = 20f,
                center = androidx.compose.ui.geometry.Offset(center.x * size.width, center.y * size.height), style = Stroke(2f))
        }
    }
}

@Composable
private fun CameraErrorOverlay(errorType: CameraErrorType, onRetry: () -> Unit, onGoToSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DesignSystem.Colors.minimalBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(when (errorType) {
                CameraErrorType.PERMISSION_DENIED -> Icons.Default.Lock
                CameraErrorType.CAMERA_IN_USE -> Icons.Default.Sync
                else -> Icons.Default.ErrorOutline
            }, null, modifier = Modifier.size(64.dp), tint = DesignSystem.Colors.error)
            Spacer(Modifier.height(24.dp))
            Text(when (errorType) {
                CameraErrorType.PERMISSION_DENIED -> "相机权限被拒绝"
                CameraErrorType.CAMERA_IN_USE -> "相机被占用"
                else -> "相机打开失败"
            }, style = DesignSystem.Typography.title2, fontWeight = FontWeight.Bold, color = DesignSystem.Colors.minimalLabel, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onRetry, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DesignSystem.Colors.primary)) {
                Text("重试", style = DesignSystem.Typography.headline)
            }
        }
    }
}