package com.livecompose.livecapture.features.capture

import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.features.capture.components.*
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager

/**
 * 2026旗舰影像主拍摄界面 - 纯黑沉浸式设计
 * 参考高端原生相机应用交互范式
 * 结构：大圆角预览区 + 底部功能行 + 快门区 + 胶囊导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onNavigateToPhotoDetail: ((String) -> Unit)? = null,
    viewModel: CaptureViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val camera = viewModel.camera
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // === 控件可见性 ===
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsAlpha by remember { mutableFloatStateOf(1f) }
    var showManualPanel by remember { mutableStateOf(false) }
    var showPhotoReview by remember { mutableStateOf(false) }
    var reviewData by remember { mutableStateOf<ByteArray?>(null) }
    var showGallerySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // === 动画状态 ===
    var captureAnimationScale by remember { mutableFloatStateOf(1f) }
    var captureFlashOpacity by remember { mutableFloatStateOf(0f) }
    var cameraFlipScaleX by remember { mutableFloatStateOf(1f) }
    var cameraFlipRotation by remember { mutableFloatStateOf(0f) }
    var isFlipping by remember { mutableStateOf(false) }
    var isEntryAnimationComplete by remember { mutableStateOf(false) }
    var vignetteIntensity by remember { mutableFloatStateOf(0f) }

    // === 对焦点 ===
    var focusPoint by remember { mutableStateOf<PointF?>(null) }
    var focusAnimation by remember { mutableFloatStateOf(0f) }

    // === 手动变焦 ===
    var pinchZoom by remember { mutableFloatStateOf(1f) }

    // === 模式与美颜 ===
    var selectedMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var isBeautyPanelVisible by remember { mutableStateOf(false) }
    var beautyParams by remember { mutableStateOf(BeautyQuickParams()) }
    var beautyPreset by remember { mutableStateOf(BeautyPreset.NATURAL) }
    var isBeautyEnabled by remember { mutableStateOf(true) }
    var showPortraitMode by remember { mutableStateOf(false) }
    val portraitViewModel = remember { PortraitViewModel(context) }

    // === 底部导航选中 ===
    var bottomNavSelected by remember { mutableIntStateOf(0) }

    // === CameraManager 状态 ===
    val zoomState by camera.zoomState.collectAsState()
    val zoomPresets by camera.zoomPresets.collectAsState()
    val zoomRange by camera.zoomRange.collectAsState()
    val flashMode by camera.flashMode.collectAsState()
    val aeLocked by camera.aeLocked.collectAsState()
    val afLocked by camera.afLocked.collectAsState()
    val focusState by camera.focusState.collectAsState()
    val aspectRatio by camera.aspectRatio.collectAsState()
    val cameraErrorState by camera.cameraError.collectAsState()

    // === ViewModel 状态 ===
    val cropRect by viewModel.cropRectInView.collectAsState()
    val boxCenter by viewModel.boxCenterManager.currentCenterInView.collectAsState()
    val isAligned by viewModel.isAligned.collectAsState()
    val userGuidanceText by viewModel.userGuidanceText.collectAsState()
    val isPipelineEnabled by viewModel.isCompositionPipelineEnabled.collectAsState()
    val isAutoCapture by viewModel.isAutoCaptureEnabled.collectAsState()
    val motionStable by viewModel.motionIsStable.collectAsState()
    val isFrontCamera = camera.isFrontCamera
    val galleryRecords by homeViewModel.records.collectAsState()
    var cameraError by remember { mutableStateOf<CameraErrorType?>(null) }
    var gridMode by remember { mutableIntStateOf(0) }
    var showHistogram by remember { mutableStateOf(false) }
    var showZebra by remember { mutableStateOf(false) }
    var showLevel by remember { mutableStateOf(false) }

    // === 工具状态 ===
    var isGridEnabled by remember { mutableStateOf(false) }
    var currentRatio by remember { mutableStateOf("3:4") }

    // 比例切换联动相机API
    LaunchedEffect(currentRatio) {
        val ratio = when (currentRatio) {
            "3:4" -> AspectRatio.RATIO_3_4
            "9:16" -> AspectRatio.RATIO_9_16
            "1:1" -> AspectRatio.RATIO_1_1
            else -> AspectRatio.RATIO_3_4
        }
        camera.setAspectRatio(ratio)
    }

    // 权限
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) { cameraError = null; camera.openCamera() }
        else { cameraError = CameraErrorType.PERMISSION_DENIED }
    }

    // 入场动画
    LaunchedEffect(Unit) {
        delay(100)
        isEntryAnimationComplete = true
    }

    // 控件自动隐藏 - 用户与底部控件交互时不隐藏
    var interactionCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(controlsVisible, interactionCounter) {
        if (controlsVisible && cameraError == null && !showManualPanel && !showGallerySheet && !showSettingsSheet) {
            controlsAlpha = 1f
            delay(4000)
            // 再次检查是否在隐藏前用户有新的交互
            controlsAlpha = 0.4f
            delay(800)
            controlsVisible = false
            controlsAlpha = 1f
        }
    }

    // 对焦动画自动隐藏
    LaunchedEffect(focusAnimation) {
        if (focusAnimation > 0f) {
            delay(3000)
            focusAnimation = 0f
            focusPoint = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onAppear()
        viewModel.onCaptureTriggered = {
            scope.launch {
                vignetteIntensity = 0.8f
                delay(100)
                captureFlashOpacity = 0.8f
                captureAnimationScale = 0.92f
                vignetteIntensity = 0f
                delay(150)
                captureFlashOpacity = 0f
                delay(200)
                captureAnimationScale = 1f
            }
        }
        if (!camera.hasCameraPermission()) {
            cameraError = CameraErrorType.PERMISSION_DENIED
        }
    }

    LaunchedEffect(cameraErrorState) { cameraError = cameraErrorState }

    DisposableEffect(Unit) { onDispose { viewModel.onDisappear() } }

    // 相机翻转动画
    fun triggerCameraFlip() {
        if (isFlipping) return
        isFlipping = true
        scope.launch {
            val scaleDown = Animatable(1f)
            scaleDown.animateTo(targetValue = 0f, animationSpec = DesignSystem.Animation.transitionMorph) {
                cameraFlipScaleX = value
            }
            viewModel.toggleCameraPosition()
            cameraFlipRotation += 180f
            val scaleUp = Animatable(0f)
            scaleUp.animateTo(targetValue = 1f, animationSpec = DesignSystem.Animation.transitionMorph) {
                cameraFlipScaleX = value
            }
            isFlipping = false
        }
    }

    // === 主布局 ===
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        // === 预览区域（大圆角） ===
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    horizontal = DesignSystem.Dimensions.previewMarginHorizontal,
                    vertical = DesignSystem.Dimensions.previewMarginTop
                )
        ) {
            // 预览容器 - 手势检测仅在此区域内生效，避免与底部按钮冲突
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.preview))
                    .background(DesignSystem.Colors.minimalSurface)
                    // 手势检测移至预览区内部，解决与底部控制按钮的手势冲突
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                controlsVisible = true
                                controlsAlpha = 1f
                                val sensorRect = camera.getSensorRect()
                                if (sensorRect != null) {
                                    // 坐标已相对于预览区，直接使用
                                    val focusX = offset.x / size.width
                                    val focusY = offset.y / size.height
                                    camera.tapToFocus(focusX, focusY, sensorRect)
                                    focusPoint = PointF(offset.x, offset.y)
                                    focusAnimation = 1f
                                }
                            },
                            onLongPress = { offset ->
                                camera.toggleAELock()
                                camera.toggleAFLock()
                                focusPoint = PointF(offset.x, offset.y)
                                focusAnimation = 1f
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            pinchZoom = (pinchZoom * zoom).coerceIn(zoomRange.start, zoomRange.endInclusive)
                            camera.updateInteractiveZoom(pinchZoom)
                        }
                    }
            ) {
                // 相机预览
                if (cameraError == null) {
                    val animatedScale by animateFloatAsState(
                        captureAnimationScale,
                        DesignSystem.Animation.shutterPress
                    )
                    val previewAlpha by animateFloatAsState(
                        targetValue = if (isEntryAnimationComplete) 1f else 0f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        label = "previewEntryAlpha"
                    )
                    CameraPreview(
                        cameraManager = camera,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = previewAlpha
                                scaleX = cameraFlipScaleX * animatedScale
                                scaleY = animatedScale
                                rotationY = cameraFlipRotation
                                cameraDistance = 12f * density.density
                            },
                        isFrontCamera = isFrontCamera
                    )
                }

                // 暗角效果
                if (vignetteIntensity > 0.01f) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color.Black.copy(alpha = vignetteIntensity * 0.6f))
                        drawCircle(
                            color = Color.Transparent,
                            radius = size.minDimension * 0.3f,
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstOut
                        )
                    }
                }

                // 网格线
                if (isGridEnabled && cameraError == null) {
                    GridOverlayView(mode = gridMode)
                }

                // 水平仪
                if (showLevel && cameraError == null) {
                    RedesignedLevelIndicator(tiltX = 0f, tiltY = 0f)
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

                // 右上角设置按钮
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    SettingsButton(onClick = {
                        showSettingsSheet = true
                        controlsVisible = false
                    })
                }

                // 右下角魔法棒
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 64.dp)
                ) {
                    MagicWandButton(onClick = { /* 滤镜/魔法效果 */ })
                }

                // 预览区底部变焦条 - 增加底部padding避免手指遮挡
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    ZoomPresetBar2026(
                        presets = zoomPresets,
                        zoomState = zoomState,
                        onSelectPreset = { viewModel.selectZoomPreset(it) }
                    )
                }

                // 点按对焦指示器 - 坐标直接相对于预览区Box
                focusPoint?.let { point ->
                    RedesignedFocusIndicator(
                        x = point.x,
                        y = point.y,
                        isFocused = focusState == FocusState.FOCUSED,
                        isLocked = aeLocked || afLocked
                    )
                }
            }

            // === 底部控制区 ===
            Spacer(modifier = Modifier.height(8.dp))

            // 功能图标行（Beta / 构图框 / 魔法棒 / 比例 / 翻转）
            ToolIconRow(
                isGridEnabled = isGridEnabled,
                onToggleGrid = {
                    isGridEnabled = !isGridEnabled
                    gridMode = if (isGridEnabled) 1 else 0
                    interactionCounter++
                },
                currentRatio = currentRatio,
                onToggleRatio = {
                    currentRatio = when (currentRatio) {
                        "3:4" -> "9:16"
                        "9:16" -> "1:1"
                        else -> "3:4"
                    }
                    interactionCounter++
                },
                onToggleCamera = { triggerCameraFlip(); interactionCounter++ }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 快门控制行（缩略图 | 快门 | AI构图）
            ShutterControlRow(
                galleryRecords = galleryRecords,
                isAligned = isAligned,
                isPipelineEnabled = isPipelineEnabled,
                onCapture = {
                    viewModel.capturePhoto()
                    interactionCounter++
                },
                onOpenGallery = {
                    showGallerySheet = true
                    controlsVisible = false
                    interactionCounter++
                },
                onTogglePipeline = {
                    viewModel.toggleCompositionPipeline()
                    interactionCounter++
                },
                onPhotoClick = { photoId ->
                    onNavigateToPhotoDetail?.invoke(photoId)
                    interactionCounter++
                },
                onLongPressStart = { /* TODO: 开始视频录制 */ },
                onLongPressEnd = { /* TODO: 结束视频录制 */ }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 底部胶囊导航
            BottomPillNav(
                selectedIndex = bottomNavSelected,
                onSelect = { index ->
                    bottomNavSelected = index
                    interactionCounter++
                    when (index) {
                        1 -> { showGallerySheet = true; controlsVisible = false }
                        2 -> { showSettingsSheet = true; controlsVisible = false }
                    }
                }
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        // === 全屏覆盖层 ===

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
            CameraErrorOverlay2026(
                errorType = error,
                onRetry = {
                    cameraError = null
                    if (camera.hasCameraPermission()) camera.openCamera()
                    else permissionLauncher.launch(android.Manifest.permission.CAMERA)
                },
                onGoToSettings = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                }
            )
        }

        // 拍照后即时预览
        if (showPhotoReview) {
            PhotoReviewOverlay2026(
                data = reviewData,
                onAccept = { showPhotoReview = false },
                onDelete = { showPhotoReview = false },
                onEdit = { showPhotoReview = false },
                onShare = { showPhotoReview = false }
            )
        }

        // 手动控制面板
        if (showManualPanel && cameraError == null) {
            ManualControlPanelOverlay(
                onDismiss = { showManualPanel = false }
            )
        }

        // 图库全屏浮层
        if (showGallerySheet) {
            GalleryFullSheet2026(
                records = galleryRecords,
                viewModel = homeViewModel,
                onDismiss = { showGallerySheet = false },
                onPhotoClick = { photoId ->
                    showGallerySheet = false
                    onNavigateToPhotoDetail?.invoke(photoId)
                }
            )
        }

        // 设置底部浮层
        if (showSettingsSheet) {
            SettingsBottomSheet2026(
                onDismiss = { showSettingsSheet = false }
            )
        }

        // 人像模式浮层
        if (showPortraitMode && cameraError == null) {
            PortraitModeOverlay(
                viewModel = portraitViewModel,
                onDismiss = { showPortraitMode = false },
                onProcessImage = { bitmap -> portraitViewModel.processImage(bitmap) }
            )
        }
    }
}

// ==========================================
// 2026 UI 组件
// ==========================================

/**
 * 设置按钮 - 六边形风格
 */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = DesignSystem.Animation.quick
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(DesignSystem.Colors.minimalDarkOverlayLight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 六边形设置图标（用已有图标近似）
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "设置",
            tint = DesignSystem.Colors.minimalLabel,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 魔法棒按钮
 */
@Composable
private fun MagicWandButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = DesignSystem.Animation.quick
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(DesignSystem.Colors.minimalOverlayMedium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoFixHigh,
            contentDescription = "魔法效果",
            tint = DesignSystem.Colors.minimalLabel,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 2026变焦条 - 胶囊半透明背景
 */
@Composable
private fun ZoomPresetBar2026(
    presets: List<ZoomPreset>,
    zoomState: ZoomState,
    onSelectPreset: (ZoomPreset) -> Unit
) {
    if (presets.isEmpty()) return

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.pill))
            .background(DesignSystem.Colors.minimalDarkOverlayLight)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEachIndexed { index, preset ->
            val isActive = kotlin.math.abs(preset.zoomFactor - zoomState.currentFactor) < 0.05f
            val bgColor by animateColorAsState(
                targetValue = if (isActive) DesignSystem.Colors.minimalOverlayStrong else Color.Transparent,
                animationSpec = tween(200)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.pill))
                    .background(bgColor)
                    .clickable { onSelectPreset(preset) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.label,
                    style = if (isActive) DesignSystem.Typography.zoomIndicatorActive
                    else DesignSystem.Typography.zoomIndicator,
                    color = if (isActive) DesignSystem.Colors.minimalLabel
                    else DesignSystem.Colors.minimalLabelSecondary
                )
            }

            if (index < presets.size - 1) {
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}

/**
 * 底部功能图标行
 * Beta | 构图框 | 魔法棒 | 3:4 | 翻转
 */
@Composable
private fun ToolIconRow(
    isGridEnabled: Boolean,
    onToggleGrid: () -> Unit,
    currentRatio: String,
    onToggleRatio: () -> Unit,
    onToggleCamera: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Beta 靶心图标
        ToolIconItem(
            icon = Icons.Default.Adjust,
            contentDescription = "Beta功能",
            label = "Beta",
            isBeta = true,
            onClick = { /* Beta功能 */ }
        )

        // 构图框
        ToolIconItem(
            icon = Icons.Default.CropFree,
            contentDescription = if (isGridEnabled) "关闭构图网格" else "开启构图网格",
            isActive = isGridEnabled,
            onClick = onToggleGrid
        )

        // 魔法棒编辑
        ToolIconItem(
            icon = Icons.Default.AutoFixHigh,
            contentDescription = "魔法效果",
            onClick = { /* 编辑功能 */ }
        )

        // 比例切换
        RatioToolItem(
            ratio = currentRatio,
            contentDescription = "切换画面比例，当前$currentRatio",
            onClick = onToggleRatio
        )

        // 翻转摄像头
        ToolIconItem(
            icon = Icons.Default.FlipCameraAndroid,
            contentDescription = "切换前后摄像头",
            onClick = onToggleCamera
        )
    }
}

/**
 * 单个功能图标项
 * 无障碍适配：必须提供 contentDescription
 */
@Composable
private fun ToolIconItem(
    icon: ImageVector,
    contentDescription: String,
    label: String? = null,
    isActive: Boolean = false,
    isBeta: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = DesignSystem.Animation.quick
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(DesignSystem.Dimensions.toolIconContainer)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isActive) DesignSystem.Colors.minimalOverlayStrong
                    else Color.Transparent
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) DesignSystem.Colors.minimalLabel
                else DesignSystem.Colors.minimalLabelSecondary,
                modifier = Modifier.size(DesignSystem.Dimensions.toolIconSize)
            )
        }

        if (isBeta && label != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DesignSystem.Colors.betaBadgeBg)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = label,
                    style = DesignSystem.Typography.betaBadge,
                    color = DesignSystem.Colors.betaBadgeText
                )
            }
        }
    }
}

/**
 * 比例切换工具项
 * 无障碍适配：提供语义描述
 */
@Composable
private fun RatioToolItem(
    ratio: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = DesignSystem.Animation.quick
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(DesignSystem.Dimensions.toolIconContainer)
                .scale(scale)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ratio,
                style = DesignSystem.Typography.caption1,
                color = DesignSystem.Colors.minimalLabelSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 快门控制行
 * 最近照片 | 快门按钮 | AI构图
 */
@Composable
private fun ShutterControlRow(
    galleryRecords: List<PhotoRecord>,
    isAligned: Boolean,
    isPipelineEnabled: Boolean,
    onCapture: () -> Unit,
    onOpenGallery: () -> Unit,
    onTogglePipeline: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onLongPressStart: () -> Unit = {},
    onLongPressEnd: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧：最近照片缩略图
        LatestPhotoThumbnail(
            records = galleryRecords,
            onClick = onOpenGallery,
            onPhotoClick = onPhotoClick
        )

        // 中央：大号快门按钮
        MainShutterButton2026(
            isAligned = isAligned,
            onCapture = onCapture,
            onLongPressStart = onLongPressStart,
            onLongPressEnd = onLongPressEnd
        )

        // 右侧：AI构图按钮
        AIComposeButton(
            isEnabled = isPipelineEnabled,
            onClick = onTogglePipeline
        )
    }
}

/**
 * 最近照片缩略图
 */
@Composable
private fun LatestPhotoThumbnail(
    records: List<PhotoRecord>,
    onClick: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    Box(
        modifier = Modifier.size(DesignSystem.Dimensions.thumbnailSize)
    ) {
        if (records.isNotEmpty()) {
            val latest = records.first()
            var thumbnail by remember(latest.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(latest.id) {
                thumbnail = try {
                    val file = java.io.File(latest.filePath)
                    if (file.exists()) BitmapFactory.decodeFile(latest.filePath) else null
                } catch (e: Exception) { null }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(DesignSystem.Dimensions.thumbnailRadius))
                    .background(DesignSystem.Colors.minimalOverlay)
                    .clickable { onPhotoClick(latest.id) }
                    .semantics { contentDescription = "查看最近拍摄的照片" },
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        tint = DesignSystem.Colors.minimalLabelTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(DesignSystem.Dimensions.thumbnailRadius))
                    .background(DesignSystem.Colors.minimalOverlay)
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = "打开相册" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    null,
                    tint = DesignSystem.Colors.minimalLabelTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 2026主快门按钮 - 大号双层圆环
 * 支持：轻触拍照 / 长按录制 / 对齐成功金色光环脉动
 * 适配国内用户习惯：右手持机，快门居中偏下，按压反馈明显
 */
@Composable
private fun MainShutterButton2026(
    isAligned: Boolean,
    onCapture: () -> Unit,
    onLongPressStart: () -> Unit = {},
    onLongPressEnd: () -> Unit = {}
) {
    var isRecording by remember { mutableStateOf(false) }
    var isPressedState by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = when {
            isRecording -> 0.88f
            isPressedState -> 0.92f
            else -> 1f
        },
        animationSpec = DesignSystem.Animation.shutterPress,
        label = "shutterScale"
    )

    // 内圈颜色：录制时变红，否则纯白
    val innerColor by animateColorAsState(
        targetValue = if (isRecording) DesignSystem.Colors.recordingRed else DesignSystem.Colors.shutterInner,
        animationSpec = tween(300),
        label = "shutterColor"
    )

    // 内圈缩放：录制时缩小至65%（变形为方块效果）
    val innerScale by animateFloatAsState(
        targetValue = if (isRecording) 0.65f else 1f,
        animationSpec = DesignSystem.Animation.shutterLongPress,
        label = "shutterInnerScale"
    )

    // 对齐成功时的金色脉动
    val glowScaleAnimatable = remember { Animatable(1.0f) }
    val glowAlphaAnimatable = remember { Animatable(0.8f) }

    LaunchedEffect(isAligned) {
        if (isAligned) {
            launch {
                while (isActive) {
                    glowScaleAnimatable.animateTo(1.12f, DesignSystem.Animation.shutterGlowPulse)
                    glowScaleAnimatable.animateTo(1.0f, DesignSystem.Animation.shutterGlowPulse)
                }
            }
            launch {
                while (isActive) {
                    glowAlphaAnimatable.animateTo(0.25f, DesignSystem.Animation.shutterGlowPulse)
                    glowAlphaAnimatable.animateTo(0.8f, DesignSystem.Animation.shutterGlowPulse)
                }
            }
        } else {
            glowScaleAnimatable.snapTo(1.0f)
            glowAlphaAnimatable.snapTo(0.8f)
        }
    }

    Box(
        modifier = Modifier
            .size(DesignSystem.Dimensions.shutterButtonOuter)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressedState = true
                        val longPressJob = coroutineScope.launch {
                            delay(500L)
                            if (!isRecording) {
                                isRecording = true
                                HapticManager.success()
                                onLongPressStart()
                            }
                        }
                        val released = tryAwaitRelease()
                        longPressJob.cancel()
                        isPressedState = false
                        if (released && !isRecording) {
                            // 轻触拍照
                            HapticManager.light()
                            onCapture()
                        } else if (isRecording) {
                            // 长按结束，停止录制
                            isRecording = false
                            onLongPressEnd()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 金色光环（对齐成功）
        if (isAligned) {
            val glowScale = glowScaleAnimatable.value
            val glowAlpha = glowAlphaAnimatable.value
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = DesignSystem.Colors.goldenGlow.copy(alpha = glowAlpha),
                    radius = (size.minDimension / 2f) * glowScale,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }

        // 外圈
        Box(
            modifier = Modifier
                .size(DesignSystem.Dimensions.shutterButtonOuter)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(
                    width = DesignSystem.Dimensions.shutterButtonRingWidth,
                    color = if (isRecording) DesignSystem.Colors.recordingRed.copy(alpha = 0.6f)
                        else DesignSystem.Colors.shutterOuterRing,
                    shape = CircleShape
                )
        )

        // 内圈：录制时变形为圆角方块
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(DesignSystem.Dimensions.shutterButtonInner * innerScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(innerColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(DesignSystem.Dimensions.shutterButtonInner * innerScale)
                    .clip(CircleShape)
                    .background(innerColor)
            )
        }
    }
}

/**
 * AI构图按钮
 */
@Composable
private fun AIComposeButton(isEnabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = DesignSystem.Animation.quick
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(DesignSystem.Dimensions.toolIconContainer)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isEnabled) DesignSystem.Colors.minimalOverlayStrong
                    else Color.Transparent
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI构图",
                tint = if (isEnabled) DesignSystem.Colors.minimalLabel
                else DesignSystem.Colors.minimalLabelTertiary,
                modifier = Modifier.size(DesignSystem.Dimensions.toolIconSize)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "AI构图",
            style = DesignSystem.Typography.modeLabel,
            color = if (isEnabled) DesignSystem.Colors.minimalLabelSecondary
            else DesignSystem.Colors.minimalLabelQuaternary
        )
    }
}

/**
 * 底部胶囊导航
 * 相机 | 相册 | 我的
 * 适配国内全面屏手势：增加底部安全区，避免与系统手势冲突
 */
@Composable
private fun BottomPillNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val items = listOf("相机", "相册", "我的")

    // 国内品牌手势缓冲区：在系统导航栏之上额外增加16dp，避免误触
    val gestureSafeZone = 16.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = gestureSafeZone),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.pill))
                .background(DesignSystem.Colors.pillBackground)
                .padding(DesignSystem.Dimensions.navPillPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedIndex == index
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) DesignSystem.Colors.pillBackgroundActive else Color.Transparent,
                    animationSpec = tween(250)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignSystem.CornerRadius.pill))
                        .background(bgColor)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = if (isSelected) DesignSystem.Typography.navLabelActive
                        else DesignSystem.Typography.navLabel,
                        color = if (isSelected) DesignSystem.Colors.pillIndicator
                        else DesignSystem.Colors.minimalLabelTertiary
                    )
                }
            }
        }
    }
}

// ==========================================
// 辅助组件（保留原有逻辑，适配新样式）
// ==========================================

@Composable
private fun CompositionOverlay(
    compositionRect: RectF,
    cropRect: RectF?,
    boxCenter: PointF?,
    isAligned: Boolean,
) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        cropRect?.let { rect ->
            drawRect(
                color = if (isAligned) DesignSystem.Colors.success else DesignSystem.Colors.warning,
                topLeft = Offset(rect.left * size.width, rect.top * size.height),
                size = androidx.compose.ui.geometry.Size(rect.width() * size.width, rect.height() * size.height),
                style = Stroke(2.5f)
            )
        }
        boxCenter?.let { center ->
            drawCircle(
                color = if (isAligned) DesignSystem.Colors.success else DesignSystem.Colors.minimalLabel,
                radius = 12f, center = Offset(center.x * size.width, center.y * size.height)
            )
            drawCircle(
                color = DesignSystem.Colors.primary.copy(alpha = 0.25f), radius = 20f,
                center = Offset(center.x * size.width, center.y * size.height), style = Stroke(2f)
            )
        }
    }
}

@Composable
private fun GridOverlayView(mode: Int) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val color = DesignSystem.Colors.minimalLabel.copy(alpha = 0.15f)
        when (mode) {
            1 -> {
                drawLine(color, start = Offset(size.width/3, 0f), end = Offset(size.width/3, size.height), strokeWidth = 1f)
                drawLine(color, start = Offset(size.width*2/3, 0f), end = Offset(size.width*2/3, size.height), strokeWidth = 1f)
                drawLine(color, start = Offset(0f, size.height/3), end = Offset(size.width, size.height/3), strokeWidth = 1f)
                drawLine(color, start = Offset(0f, size.height*2/3), end = Offset(size.width, size.height*2/3), strokeWidth = 1f)
            }
            2 -> {
                val phi = 0.618f
                drawLine(color, start = Offset(size.width*phi, 0f), end = Offset(size.width*phi, size.height), strokeWidth = 1f)
                drawLine(color, start = Offset(size.width*(1-phi), 0f), end = Offset(size.width*(1-phi), size.height), strokeWidth = 1f)
                drawLine(color, start = Offset(0f, size.height*phi), end = Offset(size.width, size.height*phi), strokeWidth = 1f)
                drawLine(color, start = Offset(0f, size.height*(1-phi)), end = Offset(size.width, size.height*(1-phi)), strokeWidth = 1f)
            }
            3 -> {
                drawLine(color, start = Offset(size.width/2, 0f), end = Offset(size.width/2, size.height), strokeWidth = 1f)
                drawLine(color, start = Offset(0f, size.height/2), end = Offset(size.width, size.height/2), strokeWidth = 1f)
            }
        }
    }
}

@Composable
private fun CameraErrorOverlay2026(
    errorType: CameraErrorType,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                when (errorType) {
                    CameraErrorType.PERMISSION_DENIED -> Icons.Default.Lock
                    CameraErrorType.CAMERA_IN_USE -> Icons.Default.Sync
                    else -> Icons.Default.ErrorOutline
                },
                null,
                modifier = Modifier.size(64.dp),
                tint = DesignSystem.Colors.error
            )
            Spacer(Modifier.height(24.dp))
            Text(
                when (errorType) {
                    CameraErrorType.PERMISSION_DENIED -> "相机权限被拒绝"
                    CameraErrorType.CAMERA_IN_USE -> "相机被占用"
                    else -> "相机打开失败"
                },
                style = DesignSystem.Typography.title2,
                fontWeight = FontWeight.Bold,
                color = DesignSystem.Colors.minimalLabel,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DesignSystem.Colors.primary)
            ) {
                Text("重试", style = DesignSystem.Typography.headline)
            }
            if (errorType == CameraErrorType.PERMISSION_DENIED) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onGoToSettings) {
                    Text("去设置", color = DesignSystem.Colors.primaryLight)
                }
            }
        }
    }
}

@Composable
private fun PhotoReviewOverlay2026(
    data: ByteArray?,
    onAccept: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    var developProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val animatable = Animatable(0f)
        animatable.animateTo(targetValue = 1f, animationSpec = DesignSystem.Animation.narrativeDevelop) {
            developProgress = value
        }
    }

    Box(Modifier.fillMaxSize().background(DesignSystem.Colors.minimalBackground)) {
        if (data != null) {
            val bitmap = remember(data) {
                android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
            }
            bitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = developProgress
                            }
                    )
                }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DesignSystem.Colors.minimalDarkOverlay)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = DesignSystem.Colors.error)
                Spacer(Modifier.width(4.dp))
                Text("删除", color = DesignSystem.Colors.error)
            }
            TextButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = DesignSystem.Colors.minimalLabel)
                Spacer(Modifier.width(4.dp))
                Text("编辑", color = DesignSystem.Colors.minimalLabel)
            }
            TextButton(onClick = onShare) {
                Icon(Icons.Default.Share, null, tint = DesignSystem.Colors.minimalLabel)
                Spacer(Modifier.width(4.dp))
                Text("分享", color = DesignSystem.Colors.minimalLabel)
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
        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "手动控制",
                    color = DesignSystem.Colors.minimalLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onDismiss) { Text("完成", color = DesignSystem.Colors.primary) }
            }
            ManualControlPanel(params = params, onParamsChanged = { params = it })
        }
    }
}

@Composable
private fun GalleryFullSheet2026(
    records: List<PhotoRecord>,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.backgroundPrimary())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("图库", style = DesignSystem.Typography.largeTitle, color = DesignSystem.Colors.textPrimary())
                Spacer(modifier = Modifier.weight(1f))
                if (isSelectionMode) {
                    TextButton(onClick = {
                        viewModel.deleteRecords(selectedIds.toList())
                        selectedIds = emptySet()
                        isSelectionMode = false
                    }) { Text("删除", color = DesignSystem.Colors.error) }
                    TextButton(onClick = {
                        isSelectionMode = false
                        selectedIds = emptySet()
                    }) { Text("取消", color = DesignSystem.Colors.textPrimary()) }
                }
                TextButton(onClick = onDismiss) { Text("关闭", color = DesignSystem.Colors.primary) }
            }

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoLibrary, null,
                            modifier = Modifier.size(64.dp),
                            tint = DesignSystem.Colors.textTertiary()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("暂无照片", color = DesignSystem.Colors.textSecondary(), style = DesignSystem.Typography.title2)
                    }
                }
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    items(records.size, key = { records[it].id }) { index ->
                        val record = records[index]
                        var thumbnail by remember(record.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(record.id) {
                            thumbnail = try {
                                val file = java.io.File(record.filePath)
                                if (file.exists()) BitmapFactory.decodeFile(record.filePath) else null
                            } catch (e: Exception) { null }
                        }
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    if (isSelectionMode) {
                                        selectedIds = if (record.id in selectedIds) {
                                            val new = selectedIds - record.id
                                            if (new.isEmpty()) isSelectionMode = false
                                            new
                                        } else selectedIds + record.id
                                    } else {
                                        onPhotoClick(record.id)
                                    }
                                }
                        ) {
                            if (thumbnail != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = thumbnail!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(DesignSystem.Colors.gray2()), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Image, null, tint = DesignSystem.Colors.textTertiary())
                                }
                            }
                            if (isSelectionMode) {
                                Box(Modifier.fillMaxSize().background(DesignSystem.Colors.minimalDarkOverlay))
                                Icon(
                                    if (record.id in selectedIds) Icons.Default.CheckCircle else Icons.Default.Circle,
                                    null,
                                    tint = if (record.id in selectedIds) DesignSystem.Colors.primary
                                    else DesignSystem.Colors.minimalSecondaryLabel,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsBottomSheet2026(onDismiss: () -> Unit) {
    val context = LocalContext.current

    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var captureDelay by remember { mutableStateOf(1.0) }
    var gridMode by remember { mutableIntStateOf(0) }
    var phantomModeEnabled by remember { mutableStateOf(false) }
    var rawCaptureEnabled by remember { mutableStateOf(false) }
    var selectedThemeIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DesignSystem.Colors.backgroundPrimary())
                .clickable(enabled = false, onClick = {})
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DesignSystem.Colors.gray4())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("设置", style = DesignSystem.Typography.largeTitle, color = DesignSystem.Colors.textPrimary())
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("完成", color = DesignSystem.Colors.primary) }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSectionHeader("外观", Icons.Default.Palette)
            SettingsCard {
                SettingsRow("主题模式", "切换深色 / 浅色外观", Icons.Default.Brightness6) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("系统", "浅色", "深色").forEachIndexed { index, text ->
                            FilterChip(
                                selected = selectedThemeIndex == index,
                                onClick = { selectedThemeIndex = index },
                                label = { Text(text, style = DesignSystem.Typography.caption1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
            }

            SettingsSectionHeader("拍摄设置", Icons.Default.CameraAlt)
            SettingsCard {
                SettingsSwitchRow("自动拍照", "对准构图框后自动触发拍摄", Icons.Default.Bolt, autoCaptureEnabled) {
                    autoCaptureEnabled = it
                }
                SettingsDivider()
                SettingsRow("拍照延迟", "${"%.1f".format(captureDelay)}秒后触发", Icons.Default.Timer) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.5, 1.0, 1.5, 2.0).forEach { delay ->
                            FilterChip(
                                selected = captureDelay == delay,
                                onClick = { captureDelay = delay },
                                label = { Text("${"%.1f".format(delay)}秒", style = DesignSystem.Typography.caption1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
            }

            SettingsSectionHeader("构图引擎", Icons.Default.AutoAwesome)
            SettingsCard {
                SettingsRow("网格线", "辅助构图参考线", Icons.Default.GridOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("关闭", "三分法", "黄金分割", "九宫格").forEachIndexed { index, label ->
                            FilterChip(
                                selected = gridMode == index,
                                onClick = { gridMode = index },
                                label = { Text(label, style = DesignSystem.Typography.caption2) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
            }

            SettingsSectionHeader("RAW 处理", Icons.Default.Camera)
            SettingsCard {
                SettingsSwitchRow("RAW 拍摄", "全链路 RAW 处理", Icons.Default.RawOn, rawCaptureEnabled) { rawCaptureEnabled = it }
            }

            SettingsSectionHeader("幻影模式", Icons.Default.Visibility)
            SettingsCard {
                SettingsSwitchRow("幻影模式", "监听系统相机输出，自动应用 LUT 色彩处理", Icons.Default.VisibilityOff, phantomModeEnabled) { phantomModeEnabled = it }
            }

            SettingsSectionHeader("隐私与合规", Icons.Default.Security)
            SettingsCard {
                ComplianceItem("隐私政策", Icons.Default.PrivacyTip, "privacy")
                ComplianceItem("用户服务协议", Icons.Default.Description, "agreement")
                ComplianceItem("个人信息收集清单", Icons.Default.ListAlt, "personal_info")
                ComplianceItem("青少年模式", Icons.Default.PersonRemove, "youth_mode")
                ComplianceItem("第三方SDK清单", Icons.Default.Code, "sdk_list")
            }

            SettingsSectionHeader("关于", Icons.Default.Info)
            SettingsCard {
                SettingsRow("版本信息", "构妙 LiveCapture v1.1.3", Icons.Default.Info)
                SettingsDivider()
                SettingsClickRow("ICP备案号", "待备案", Icons.Default.VerifiedUser) {
                    val intent = android.content.Intent(context, com.livecompose.livecapture.features.compliance.ComplianceHostActivity::class.java).apply {
                        putExtra("compliance_page", "icp_filing")
                    }
                    context.startActivity(intent)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 10.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary(), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.08f)
            .padding(4.dp),
        content = content
    )
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: ImageVector, trailing: @Composable () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        trailing()
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DesignSystem.Colors.primary,
                uncheckedThumbColor = DesignSystem.Colors.gray4(),
                uncheckedTrackColor = DesignSystem.Colors.gray3()
            )
        )
    }
}

@Composable
private fun SettingsClickRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        Icon(Icons.Default.ChevronRight, null, tint = DesignSystem.Colors.textTertiary())
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = DesignSystem.Colors.gray3(), thickness = 0.5.dp)
}

@Composable
private fun ComplianceItem(title: String, icon: ImageVector, page: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .clickable {
                val intent = android.content.Intent(context, com.livecompose.livecapture.features.compliance.ComplianceHostActivity::class.java).apply {
                    putExtra("compliance_page", page)
                }
                context.startActivity(intent)
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary(), modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = DesignSystem.Colors.textTertiary())
    }
}

@Composable
private fun PortraitModeOverlay(
    viewModel: com.livecompose.livecapture.core.portrait.PortraitViewModel,
    onDismiss: () -> Unit,
    onProcessImage: (android.graphics.Bitmap) -> Unit
) {
    // TODO: 实现人像模式浮层
}

// ====== 预设参数映射 ======
private fun presetParamsFor(preset: BeautyPreset): BeautyQuickParams = when (preset) {
    BeautyPreset.NATURAL -> BeautyQuickParams(smoothing = 0.2f, whitening = 0.1f, slimFace = 0.05f, enlargeEye = 0.05f)
    BeautyPreset.FAIR -> BeautyQuickParams(smoothing = 0.5f, whitening = 0.6f, slimFace = 0.2f, enlargeEye = 0.15f)
    BeautyPreset.VIBRANT -> BeautyQuickParams(smoothing = 0.4f, whitening = 0.3f, slimFace = 0.25f, enlargeEye = 0.3f)
    BeautyPreset.PREMIUM -> BeautyQuickParams(smoothing = 0.3f, whitening = 0.15f, slimFace = 0.15f, enlargeEye = 0.1f)
    BeautyPreset.CUSTOM -> BeautyQuickParams()
}
