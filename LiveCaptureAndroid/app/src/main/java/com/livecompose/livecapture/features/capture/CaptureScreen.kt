package com.livecompose.livecapture.features.capture

import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.features.capture.components.*
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.launch

/**
 * 主拍摄界面 - 单屏全功能 2026 国内旗舰手机摄影体验
 * 集成: 图库缩略条、设置浮层、点按对焦/测光、双指变焦、AE/AF锁定、闪光灯、网格、直方图、斑马纹、水平仪
 * 适配: 华为/小米/OPPO/vivo/荣耀等国内品牌手机
 * 设计语言: 国潮质感 - 温润光影 + 微拟物 + 自信动效
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

    // 控件可见性
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsAlpha by remember { mutableFloatStateOf(1f) }
    var showManualPanel by remember { mutableStateOf(false) }
    var showPhotoReview by remember { mutableStateOf(false) }
    var reviewData by remember { mutableStateOf<ByteArray?>(null) }

    // 图库 / 设置浮层
    var showGallerySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // 动画状态
    var captureAnimationScale by remember { mutableFloatStateOf(1f) }
    var captureFlashOpacity by remember { mutableFloatStateOf(0f) }
    var cameraFlipScaleX by remember { mutableFloatStateOf(1f) }
    var cameraFlipRotation by remember { mutableFloatStateOf(0f) }
    var isFlipping by remember { mutableStateOf(false) }

    // 对焦点
    var focusPoint by remember { mutableStateOf<PointF?>(null) }
    var focusAnimation by remember { mutableFloatStateOf(0f) }
    var focusAppearTime by remember { mutableLongStateOf(0L) }

    // 手动变焦
    var pinchZoom by remember { mutableFloatStateOf(1f) }

    // 新状态变量
    var selectedMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var isBeautyPanelVisible by remember { mutableStateOf(false) }
    var beautyParams by remember { mutableStateOf(BeautyQuickParams()) }
    var beautyPreset by remember { mutableStateOf(BeautyPreset.NATURAL) }
    var isBeautyEnabled by remember { mutableStateOf(true) }
    var currentFilterIndex by remember { mutableIntStateOf(0) }
    var vignetteIntensity by remember { mutableFloatStateOf(0f) }
    var isEntryAnimationComplete by remember { mutableStateOf(false) }

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

    // 图库数据
    val galleryRecords by homeViewModel.records.collectAsState()

    var cameraError by remember { mutableStateOf<CameraErrorType?>(null) }

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

    // 入场动画
    LaunchedEffect(Unit) {
        delay(50)
        isEntryAnimationComplete = true
    }

    // 控件自动隐藏 - 渐进式淡出
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && cameraError == null && !showManualPanel && !showGallerySheet && !showSettingsSheet) {
            controlsAlpha = 1f
            delay(3000)
            controlsAlpha = 0.5f
            delay(1000)
            controlsVisible = false
            controlsAlpha = 1f
        }
    }

    // 对焦动画 - 弹性出现 + 3秒自动隐藏
    LaunchedEffect(focusAnimation) {
        if (focusAnimation > 0f) {
            focusAppearTime = System.currentTimeMillis()
            delay(3000)
            focusAnimation = 0f
            focusPoint = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onAppear()
        viewModel.onCaptureTriggered = {
            // 多阶段拍照反馈
            scope.launch {
                // 第一阶段：暗角
                vignetteIntensity = 0.8f
                delay(100)
                // 第二阶段：白色闪光
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

    DisposableEffect(Unit) { onDispose { viewModel.onDisappear() } }

    // 相机翻转动画 - 3D缩放效果
    fun triggerCameraFlip() {
        if (isFlipping) return
        isFlipping = true
        scope.launch {
            // 第一半：缩放X从1.0到0.0
            val scaleDown = Animatable(1f)
            scaleDown.animateTo(
                targetValue = 0f,
                animationSpec = DesignSystem.Animation.transitionMorph
            ) {
                cameraFlipScaleX = value
            }
            // 切换相机
            viewModel.toggleCameraPosition()
            cameraFlipRotation += 180f
            // 第二半：缩放X从0.0到1.0
            val scaleUp = Animatable(0f)
            scaleUp.animateTo(
                targetValue = 1f,
                animationSpec = DesignSystem.Animation.transitionMorph
            ) {
                cameraFlipScaleX = value
            }
            isFlipping = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
            // 点击切换控件显示 / 点按对焦
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        controlsVisible = true
                        controlsAlpha = 1f
                        val sensorRect = camera.getSensorRect()
                        if (sensorRect != null) {
                            camera.tapToFocus(offset.x / size.width, offset.y / size.height, sensorRect)
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
            // 双指缩放变焦
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    pinchZoom = (pinchZoom * zoom).coerceIn(zoomRange.start, zoomRange.endInclusive)
                    camera.updateInteractiveZoom(pinchZoom)
                }
            }
            // 上下滑动切换模式
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {},
                    onDragCancel = {},
                    onVerticalDrag = { _, dragAmount ->
                        // 仅在拖拽幅度足够大时切换（由 onDragEnd 处理）
                    }
                )
            }
            // 左右滑动功能切换
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {},
                    onDragCancel = {},
                    onHorizontalDrag = { _, dragAmount ->
                        // 由 onDragEnd 处理
                    }
                )
            }
    ) {
        // 相机预览 - 全屏 + 入场动画
        if (cameraError == null) {
            val animatedScale by animateFloatAsState(
                captureAnimationScale,
                DesignSystem.Animation.shutterPress,
                "captureScale"
            )
            // 入场动画：从黑色渐显
            val previewAlpha by animateFloatAsState(
                targetValue = if (isEntryAnimationComplete) 1f else 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "previewEntryAlpha"
            )
            CameraPreview(
                cameraManager = camera,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = previewAlpha
                        scaleX = cameraFlipScaleX * (animatedScale)
                        scaleY = animatedScale
                        rotationY = cameraFlipRotation
                        cameraDistance = 12f * density.density
                    },
                isFrontCamera = isFrontCamera
            )

            // 暗角效果
            if (vignetteIntensity > 0.01f) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawRect(color = Color.Black.copy(alpha = vignetteIntensity * 0.6f))
                    drawCircle(
                        color = Color.Transparent,
                        radius = size.minDimension * 0.3f,
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstOut
                    )
                }
            }
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
            RedesignedLevelIndicator(tiltX = 0f, tiltY = 0f)
        }

        // 点按对焦指示器 - 重设计版
        focusPoint?.let { point ->
            val scale by animateFloatAsState(
                if (focusAnimation > 0f) 1f else 0f,
                DesignSystem.Animation.quick
            )
            RedesignedFocusIndicator(
                x = point.x,
                y = point.y,
                isFocused = focusState == FocusState.FOCUSED,
                isLocked = aeLocked || afLocked
            )
        }

        // 拍照闪光 - 多阶段径向扩散
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
                onDelete = { showPhotoReview = false },
                onEdit = {
                    showPhotoReview = false
                    // Navigate to edit - photo is already saved by now
                },
                onShare = {
                    showPhotoReview = false
                    // Navigate to share
                }
            )
        }

        // 手动控制面板
        if (showManualPanel && cameraError == null) {
            ManualControlPanelOverlay(
                onDismiss = { showManualPanel = false }
            )
        }

        // 顶部控制栏 - 毛玻璃药丸式设计 + 入场动画
        val topBarOffset by animateFloatAsState(
            targetValue = if (isEntryAnimationComplete) 0f else -100f,
            animationSpec = DesignSystem.Animation.entrySlideUp,
            label = "topBarEntry"
        )
        AnimatedVisibility(
            visible = controlsVisible && cameraError == null && !showManualPanel,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut(DesignSystem.Animation.overlayFade) + slideOutVertically(
                animationSpec = DesignSystem.Animation.overlayFade
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = topBarOffset }
        ) {
            CaptureTopBarRedesigned(
                userGuidanceText = userGuidanceText,
                isAutoCaptureEnabled = isAutoCapture,
                onToggleCamera = { triggerCameraFlip() },
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
                onToggleLevel = { showLevel = !showLevel },
                onOpenSettings = {
                    showSettingsSheet = true
                    controlsVisible = false
                },
                isBeautyEnabled = isBeautyEnabled,
                onToggleBeauty = { isBeautyEnabled = !isBeautyEnabled },
                controlsAlpha = controlsAlpha
            )
        }

        // AI 智能信息条 — 场景识别 + 滤镜推荐 + 姿势提示
        val aiSceneName by viewModel.aiSceneName.collectAsState()
        val aiFilterRecs by viewModel.aiFilterRecommendations.collectAsState()
        val aiPoseSuggestion by viewModel.aiPoseSuggestion.collectAsState()
        if (aiSceneName.isNotEmpty() && controlsVisible && cameraError == null && !showManualPanel) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 100.dp, start = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.45f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    // 场景识别
                    Text(
                        "AI ${aiSceneName}",
                        color = DesignSystem.Colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 姿势提示
                    if (aiPoseSuggestion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            aiPoseSuggestion,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                    // 滤镜推荐（最多显示 2 个）
                    if (aiFilterRecs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            aiFilterRecs.take(2).forEach { rec ->
                                Text(
                                    rec.preset.name,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(
                                            DesignSystem.Colors.primary.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 底部控制栏 - 三段式布局 + 入场动画
        val bottomBarOffset by animateFloatAsState(
            targetValue = if (isEntryAnimationComplete) 0f else 200f,
            animationSpec = DesignSystem.Animation.entrySlideUp,
            label = "bottomBarEntry"
        )
        AnimatedVisibility(
            visible = controlsVisible && cameraError == null && !showManualPanel,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(DesignSystem.Animation.overlayFade) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = DesignSystem.Animation.overlayFade
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = bottomBarOffset
                    alpha = controlsAlpha
                }
        ) {
            CaptureBottomBarRedesigned(
                zoomPresets = zoomPresets,
                zoomRange = zoomRange,
                zoomState = zoomState,
                isPipelineEnabled = isPipelineEnabled,
                aeLocked = aeLocked,
                afLocked = afLocked,
                aspectRatio = aspectRatio,
                selectedMode = selectedMode,
                galleryRecords = galleryRecords,
                isAligned = isAligned,
                isBeautyPanelVisible = isBeautyPanelVisible,
                isBeautyEnabled = isBeautyEnabled,
                beautyParams = beautyParams,
                beautyPreset = beautyPreset,
                onSelectPreset = { viewModel.selectZoomPreset(it) },
                onZoomDrag = { viewModel.updateZoomInteractively(it) },
                onZoomDragEnd = { viewModel.finalizeZoomInteractively(it) },
                onTogglePipeline = { viewModel.toggleCompositionPipeline() },
                onCapture = { viewModel.capturePhoto() },
                onToggleCamera = { triggerCameraFlip() },
                onOpenGallery = {
                    showGallerySheet = true
                    controlsVisible = false
                },
                onToggleAELock = { camera.toggleAELock() },
                onToggleAFLock = { camera.toggleAFLock() },
                onToggleAspectRatio = {
                    val ratios = AspectRatio.entries
                    val nextIndex = (ratios.indexOf(aspectRatio) + 1) % ratios.size
                    camera.setAspectRatio(ratios[nextIndex])
                },
                onToggleManualPanel = { showManualPanel = true },
                onModeSelected = { selectedMode = it },
                onPhotoClick = { photoId ->
                    onNavigateToPhotoDetail?.invoke(photoId)
                },
                onToggleBeautyPanel = { isBeautyPanelVisible = !isBeautyPanelVisible },
                onBeautyParamsChange = { beautyParams = it },
                onBeautyPresetChange = {
                    beautyPreset = it
                    beautyParams = presetParamsFor(it)
                },
                onToggleBeauty = { isBeautyEnabled = !isBeautyEnabled },
                onExpandFullBeauty = { /* 完整美颜面板通过 BeautyPanelScreen 打开 */ }
            )
        }

        // 直方图浮层
        if (showHistogram && cameraError == null) {
            RedesignedHistogramOverlay(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 12.dp)
            )
        }

        // 斑马纹浮层
        if (showZebra && cameraError == null) {
            RedesignedZebraOverlay()
        }
    }

    // ====== 图库全屏浮层 ======
    if (showGallerySheet) {
        GalleryFullSheet(
            records = galleryRecords,
            viewModel = homeViewModel,
            onDismiss = { showGallerySheet = false },
            onPhotoClick = { photoId ->
                showGallerySheet = false
                onNavigateToPhotoDetail?.invoke(photoId)
            }
        )
    }

    // ====== 设置底部浮层 ======
    if (showSettingsSheet) {
        SettingsBottomSheet(
            onDismiss = { showSettingsSheet = false }
        )
    }
}

// ====== 预设参数映射 ======
private fun presetParamsFor(preset: BeautyPreset): BeautyQuickParams = when (preset) {
    BeautyPreset.NATURAL -> BeautyQuickParams(
        smoothing = 0.2f, whitening = 0.1f, slimFace = 0.05f, enlargeEye = 0.05f
    )
    BeautyPreset.FAIR -> BeautyQuickParams(
        smoothing = 0.5f, whitening = 0.6f, slimFace = 0.2f, enlargeEye = 0.15f
    )
    BeautyPreset.VIBRANT -> BeautyQuickParams(
        smoothing = 0.4f, whitening = 0.3f, slimFace = 0.25f, enlargeEye = 0.3f
    )
    BeautyPreset.PREMIUM -> BeautyQuickParams(
        smoothing = 0.3f, whitening = 0.15f, slimFace = 0.15f, enlargeEye = 0.1f
    )
    BeautyPreset.CUSTOM -> BeautyQuickParams()
}

// ====== 重设计顶部控制栏 - 毛玻璃药丸式 ======
@Composable
private fun CaptureTopBarRedesigned(
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
    onToggleLevel: () -> Unit,
    onOpenSettings: () -> Unit,
    isBeautyEnabled: Boolean,
    onToggleBeauty: () -> Unit,
    controlsAlpha: Float
) {
    // 引导文字动画交叉淡入
    var currentGuidance by remember { mutableStateOf("") }
    var guidanceTransition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(userGuidanceText) {
        if (userGuidanceText != currentGuidance) {
            guidanceTransition = 0f
            currentGuidance = userGuidanceText
            guidanceTransition = 1f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 主控制行 - 毛玻璃药丸
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    cornerRadius = DesignSystem.CornerRadius.xLarge,
                    intensity = 0.12f
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 引导文字
            if (currentGuidance.isNotEmpty()) {
                AnimatedContent(
                    targetState = currentGuidance,
                    transitionSpec = {
                        fadeIn(tween(300)) + scaleIn(initialScale = 0.9f) togetherWith
                        fadeOut(tween(200)) + scaleOut(targetScale = 0.9f)
                    }
                ) { text ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(DesignSystem.Colors.minimalDarkOverlay)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text, color = DesignSystem.Colors.minimalLabel,
                            style = DesignSystem.Typography.caption1, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 功能按钮组 - 更大触摸目标 40dp
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // 美颜开关
                TopIconButtonRedesigned(
                    icon = Icons.Default.AutoFixHigh,
                    active = isBeautyEnabled,
                    onClick = onToggleBeauty
                )
                // 设置
                TopIconButtonRedesigned(
                    icon = Icons.Default.Settings,
                    active = false,
                    onClick = onOpenSettings
                )
                // 水平仪
                TopIconButtonRedesigned(
                    icon = Icons.Default.AlignHorizontalLeft,
                    active = showLevel,
                    onClick = onToggleLevel
                )
                // 斑马纹
                TopIconButtonRedesigned(
                    icon = Icons.Default.GridOn,
                    active = showZebra,
                    onClick = onToggleZebra
                )
                // 直方图
                TopIconButtonRedesigned(
                    icon = Icons.Default.BarChart,
                    active = showHistogram,
                    onClick = onToggleHistogram
                )
                // 网格
                TopIconButtonRedesigned(
                    icon = Icons.Default.Grid4x4,
                    active = gridMode > 0,
                    onClick = onToggleGrid
                )
                // 闪光灯
                TopIconButtonRedesigned(
                    icon = flashModeIcon(flashMode),
                    active = flashMode != FlashMode.OFF,
                    onClick = onToggleFlash
                )
                // 翻转
                TopIconButtonRedesigned(
                    icon = Icons.Default.FlipCameraAndroid,
                    active = false,
                    onClick = onToggleCamera
                )
            }
        }
    }
}

@Composable
private fun TopIconButtonRedesigned(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) DesignSystem.Colors.primary.copy(alpha = 0.25f) else DesignSystem.Colors.minimalDarkOverlay)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) DesignSystem.Colors.primary
        else DesignSystem.Colors.minimalSecondaryLabel, modifier = Modifier.size(18.dp))
    }
}

private fun flashModeIcon(mode: FlashMode) = when (mode) {
    FlashMode.OFF -> Icons.Default.FlashOff
    FlashMode.AUTO -> Icons.Default.FlashAuto
    FlashMode.ON -> Icons.Default.FlashOn
    FlashMode.TORCH -> Icons.Default.Highlight
}

// ====== 重设计底部控制栏 - 三段式布局 ======
@Composable
private fun CaptureBottomBarRedesigned(
    zoomPresets: List<ZoomPreset>,
    zoomRange: ClosedFloatingPointRange<Float>,
    zoomState: ZoomState,
    isPipelineEnabled: Boolean,
    aeLocked: Boolean,
    afLocked: Boolean,
    aspectRatio: AspectRatio,
    selectedMode: CaptureMode,
    galleryRecords: List<PhotoRecord>,
    isAligned: Boolean,
    isBeautyPanelVisible: Boolean,
    isBeautyEnabled: Boolean,
    beautyParams: BeautyQuickParams,
    beautyPreset: BeautyPreset,
    onSelectPreset: (ZoomPreset) -> Unit,
    onZoomDrag: (Float) -> Unit,
    onZoomDragEnd: (Float) -> Unit,
    onTogglePipeline: () -> Unit,
    onCapture: () -> Unit,
    onToggleCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onToggleAELock: () -> Unit,
    onToggleAFLock: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onToggleManualPanel: () -> Unit,
    onModeSelected: (CaptureMode) -> Unit,
    onPhotoClick: (String) -> Unit,
    onToggleBeautyPanel: () -> Unit,
    onBeautyParamsChange: (BeautyQuickParams) -> Unit,
    onBeautyPresetChange: (BeautyPreset) -> Unit,
    onToggleBeauty: () -> Unit,
    onExpandFullBeauty: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        // 美颜快调条 - 仅当美颜面板可见时
        AnimatedVisibility(
            visible = isBeautyPanelVisible && isBeautyEnabled,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            BeautyQuickBar(
                params = beautyParams,
                currentPreset = beautyPreset,
                isBeautyEnabled = isBeautyEnabled,
                onParamsChange = onBeautyParamsChange,
                onPresetChange = onBeautyPresetChange,
                onToggleBeauty = onToggleBeauty,
                onExpandFull = onExpandFullBeauty
            )
            Spacer(Modifier.height(8.dp))
        }

        // 图库缩略条（最近照片）
        if (galleryRecords.isNotEmpty()) {
            GalleryThumbnailStrip(
                records = galleryRecords.take(15),
                onOpenGallery = onOpenGallery,
                onPhotoClick = onPhotoClick
            )
            Spacer(Modifier.height(8.dp))
        }

        // 模式选择器 - 卡片式
        ModeSelector(
            modes = listOf(
                CaptureMode.PHOTO,
                CaptureMode.VIDEO,
                CaptureMode.PORTRAIT,
                CaptureMode.PRO
            ),
            selectedMode = selectedMode,
            onModeSelected = onModeSelected
        )

        Spacer(Modifier.height(8.dp))

        // 变焦预设
        if (zoomPresets.isNotEmpty()) {
            ZoomPresetBar(zoomPresets, zoomState, onSelectPreset)
            Spacer(Modifier.height(8.dp))
        }

        // 主控制行 - 简化为3项：图库 / 快门 / 翻转
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 左侧: 图库缩略图按钮
            GalleryThumbBtn(onClick = onOpenGallery)

            // 中间: 重设计快门按钮
            RedesignedShutterButton(
                isAligned = isAligned,
                isRecording = selectedMode == CaptureMode.VIDEO,
                onCapture = onCapture,
                onLongPressStart = { /* 视频录制开始 */ },
                onLongPressEnd = { /* 视频录制结束 */ }
            )

            // 右侧: 相机翻转按钮
            CameraFlipBtn(onClick = onToggleCamera)
        }
    }
}

// ====== 图库缩略条 ======
@Composable
private fun GalleryThumbnailStrip(
    records: List<PhotoRecord>,
    onOpenGallery: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        records.forEach { record ->
            var thumbnail by remember(record.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(record.id) {
                thumbnail = try {
                    val file = java.io.File(record.filePath)
                    if (file.exists()) BitmapFactory.decodeFile(record.filePath) else null
                } catch (e: Exception) { null }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DesignSystem.Colors.minimalOverlay)
                    .clickable { onPhotoClick(record.id) }
            ) {
                if (thumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        // "查看全部"按钮
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DesignSystem.Colors.minimalDarkOverlay)
                .clickable { onOpenGallery() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = DesignSystem.Colors.minimalSecondaryLabel,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ====== 相机翻转按钮 ======
@Composable
private fun CameraFlipBtn(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DesignSystem.Colors.minimalOverlay)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.FlipCameraAndroid,
                null,
                tint = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            "翻转",
            style = DesignSystem.Typography.caption2,
            color = DesignSystem.Colors.minimalSecondaryLabel
        )
    }
}

// ====== 图库全屏浮层 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryFullSheet(
    records: List<PhotoRecord>,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    var selectedPhotoIndex by remember { mutableIntStateOf(-1) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.backgroundPrimary())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "图库",
                    style = DesignSystem.Typography.largeTitle,
                    color = DesignSystem.Colors.textPrimary()
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isSelectionMode) {
                    TextButton(onClick = {
                        viewModel.deleteRecords(selectedIds.toList())
                        selectedIds = emptySet()
                        isSelectionMode = false
                    }) {
                        Text("删除", color = DesignSystem.Colors.error)
                    }
                    TextButton(onClick = {
                        isSelectionMode = false
                        selectedIds = emptySet()
                    }) {
                        Text("取消", color = DesignSystem.Colors.textPrimary())
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = DesignSystem.Colors.primary)
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoLibrary, null,
                            modifier = Modifier.size(64.dp),
                            tint = DesignSystem.Colors.textTertiary()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("暂无照片", color = DesignSystem.Colors.textSecondary(),
                            style = DesignSystem.Typography.title2)
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
                                Box(Modifier.fillMaxSize().background(DesignSystem.Colors.gray2()),
                                    contentAlignment = Alignment.Center) {
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

// ====== 设置底部浮层 ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var captureDelay by remember { mutableStateOf(1.0) }
    var gridMode by remember { mutableIntStateOf(0) }
    var phantomModeEnabled by remember { mutableStateOf(false) }
    var rawCaptureEnabled by remember { mutableStateOf(false) }
    var selectedThemeIndex by remember { mutableIntStateOf(0) }
    var selectedDetectionModeIndex by remember { mutableIntStateOf(1) }

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
                .verticalScroll(rememberScrollState())
        ) {
            // 拖拽指示器
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

            // 标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "设置",
                    style = DesignSystem.Typography.largeTitle,
                    color = DesignSystem.Colors.textPrimary()
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("完成", color = DesignSystem.Colors.primary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ====== 外观 ======
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

            // ====== 拍摄设置 ======
            SettingsSectionHeader("拍摄设置", Icons.Default.CameraAlt)
            SettingsCard {
                SettingsSwitchRow("自动拍照", "对准构图框后自动触发拍摄", Icons.Default.Bolt,
                    autoCaptureEnabled) {
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

            // ====== 构图引擎 ======
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

            // ====== RAW 处理 ======
            SettingsSectionHeader("RAW 处理", Icons.Default.Camera)
            SettingsCard {
                SettingsSwitchRow("RAW 拍摄", "全链路 RAW 处理", Icons.Default.RawOn,
                    rawCaptureEnabled) { rawCaptureEnabled = it }
            }

            // ====== 幻影模式 ======
            SettingsSectionHeader("幻影模式", Icons.Default.Visibility)
            SettingsCard {
                SettingsSwitchRow("幻影模式", "监听系统相机输出，自动应用 LUT 色彩处理", Icons.Default.VisibilityOff,
                    phantomModeEnabled) { phantomModeEnabled = it }
            }

            // ====== 隐私与合规 ======
            SettingsSectionHeader("隐私与合规", Icons.Default.Security)
            SettingsCard {
                ComplianceItem("隐私政策", Icons.Default.PrivacyTip, "privacy")
                ComplianceItem("用户服务协议", Icons.Default.Description, "agreement")
                ComplianceItem("个人信息收集清单", Icons.Default.ListAlt, "personal_info")
                ComplianceItem("青少年模式", Icons.Default.PersonRemove, "youth_mode")
                ComplianceItem("第三方SDK清单", Icons.Default.Code, "sdk_list")
            }

            // ====== 关于 ======
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

// ====== 设置浮层通用组件 ======

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
        Text(title, style = DesignSystem.Typography.title3,
            color = DesignSystem.Colors.textPrimary(), fontWeight = FontWeight.SemiBold)
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
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
private fun SettingsClickRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
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
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        color = DesignSystem.Colors.gray3(),
        thickness = 0.5.dp
    )
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
        Text(title, style = DesignSystem.Typography.headline,
            color = DesignSystem.Colors.textPrimary(), modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = DesignSystem.Colors.textTertiary())
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

/**
 * 重设计照片预览浮层 - 国潮质感
 * - 显影动画（亮度从-1到0，模拟胶片显影）
 * - 3按钮药丸式容器（删除/编辑/分享/保存）
 * - 上下滑动手势
 */
@Composable
private fun PhotoReviewOverlay(data: ByteArray?, onAccept: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit, onShare: () -> Unit) {
    // 显影动画
    var developProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        developProgress = 0f
        val animatable = Animatable(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = DesignSystem.Animation.narrativeDevelop
        ) {
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
                            // 显影效果：亮度从-1到0
                            colorMatrix = androidx.compose.ui.graphics.ColorMatrix().apply {
                                setToBrightness(-1f + developProgress)
                            }
                        }
                )
            }
        }

        // 底部操作按钮 - 药丸式容器
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
            // 删除
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = DesignSystem.Colors.error)
                Spacer(Modifier.width(4.dp))
                Text("删除", color = DesignSystem.Colors.error)
            }
            // 编辑
            TextButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = DesignSystem.Colors.minimalLabel)
                Spacer(Modifier.width(4.dp))
                Text("编辑", color = DesignSystem.Colors.minimalLabel)
            }
            // 分享
            TextButton(onClick = onShare) {
                Icon(Icons.Default.Share, null, tint = DesignSystem.Colors.minimalLabel)
                Spacer(Modifier.width(4.dp))
                Text("分享", color = DesignSystem.Colors.minimalLabel)
            }
            // 保存
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
                topLeft = Offset(rect.left * size.width, rect.top * size.height),
                size = androidx.compose.ui.geometry.Size(rect.width() * size.width, rect.height() * size.height),
                style = Stroke(2.5f))
        }
        boxCenter?.let { center ->
            drawCircle(color = if (isAligned) DesignSystem.Colors.success else DesignSystem.Colors.minimalLabel,
                radius = 12f, center = Offset(center.x * size.width, center.y * size.height))
            drawCircle(color = DesignSystem.Colors.primary.copy(alpha = 0.25f), radius = 20f,
                center = Offset(center.x * size.width, center.y * size.height), style = Stroke(2f))
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
