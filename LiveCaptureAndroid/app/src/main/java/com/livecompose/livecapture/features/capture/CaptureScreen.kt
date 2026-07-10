package com.livecompose.livecapture.features.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.components.*
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    onNavigateToShootingGuide: (() -> Unit)? = null,
    onNavigateToPrivacy: (() -> Unit)? = null,
    onNavigateToAgreement: (() -> Unit)? = null,
    onNavigateToCommunity: (() -> Unit)? = null,
    onNavigateToIcp: (() -> Unit)? = null,
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
    var showProfileSheet by remember { mutableStateOf(false) }

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

    // === AI 滤镜推荐状态 ===
    var showFilterRecommendationSheet by remember { mutableStateOf(false) }
    val filterRecommendations by viewModel.aiFilterRecommendations.collectAsState()
    val appContainer = remember { AppContainer.getInstance(context) }

    // === 底部导航选中 ===
    var bottomNavSelected by remember { mutableIntStateOf(0) }

    // === CameraManager 状态 ===
    val zoomState by camera.zoomState.collectAsState()
    val zoomPresets by camera.zoomPresets.collectAsState()
    val zoomRange by camera.zoomRange.collectAsState()
    val flashMode by camera.flashMode.collectAsState()

    // === AI 智能引擎状态 ===
    val aiSceneName by viewModel.aiSceneName.collectAsState()
    val aiSceneType by viewModel.aiSceneType.collectAsState()
    val aiPoseSuggestion by viewModel.aiPoseSuggestion.collectAsState()
    val aiZoomSuggestion by viewModel.aiZoomSuggestion.collectAsState()
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

    // === 专业功能状态 ===
    var rawCaptureEnabled by remember { mutableStateOf(false) }
    var dngCaptureEnabled by remember { mutableStateOf(false) }
    var hyperfocalEnabled by remember { mutableStateOf(false) }
    var hyperfocalDistanceText by remember { mutableStateOf("--") }
    var burstModeEnabled by remember { mutableStateOf(false) }
    var burstCount by remember { mutableIntStateOf(0) }
    var isBursting by remember { mutableStateOf(false) }
    var multiExposureEnabled by remember { mutableStateOf(false) }
    var multiExposureFrameCount by remember { mutableIntStateOf(0) }
    var multiExposureBitmaps by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    val quickShotManager = remember { QuickShotManager() }
    val multipleExposure = remember { MultipleExposure() }
    val dngCaptureManager = remember { DngCaptureManager(context) }

    // === 视频录制状态 ===
    val videoViewModel = remember { VideoViewModel(context) }
    val videoRecordingState by videoViewModel.recordingState.collectAsState()
    val videoStabilizationEnabled by videoViewModel.stabilizationEnabled.collectAsState()
    var slowMotionEnabled by remember { mutableStateOf(false) }
    var showVideoEditor by remember { mutableStateOf(false) }
    var lastRecordedVideoPath by remember { mutableStateOf<String?>(null) }
    var videoEditorStartTime by remember { mutableLongStateOf(0L) }
    var videoEditorEndTime by remember { mutableLongStateOf(0L) }

    // 视频模式切换时同步 VideoViewModel
    LaunchedEffect(selectedMode) {
        if (selectedMode == CaptureMode.VIDEO) {
            if (slowMotionEnabled) {
                videoViewModel.switchMode(VideoMode.SLOW_MOTION)
            } else {
                videoViewModel.switchMode(VideoMode.NORMAL)
            }
        }
    }

    // 慢动作切换时同步模式
    LaunchedEffect(slowMotionEnabled) {
        if (selectedMode == CaptureMode.VIDEO) {
            if (slowMotionEnabled) {
                videoViewModel.switchMode(VideoMode.SLOW_MOTION)
            } else {
                videoViewModel.switchMode(VideoMode.NORMAL)
            }
        }
    }

    // 防抖开关同步
    LaunchedEffect(videoStabilizationEnabled) {
        // VideoViewModel manages its own stabilization state
    }

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

    // 超焦距计算
    LaunchedEffect(hyperfocalEnabled, zoomState.currentFactor) {
        if (hyperfocalEnabled) {
            try {
                val focalLength = HyperfocalCalculator.estimateFocalLength(zoomState.currentFactor)
                val aperture = 1.8f // 默认光圈值
                val result = HyperfocalCalculator.calculate(
                    focalLengthMm = focalLength,
                    aperture = aperture,
                    sensorKey = "phone_main"
                )
                hyperfocalDistanceText = result.displayText
            } catch (e: Exception) {
                hyperfocalDistanceText = "--"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDisappear()
            try {
                videoViewModel.stabilizer.stopStabilization()
            } catch (_: Exception) {}
        }
    }

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
                    MagicWandButton(onClick = {
                        showFilterRecommendationSheet = true
                        controlsVisible = false
                    })
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

                // 超焦距距离指示器
                if (hyperfocalEnabled && cameraError == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignSystem.Colors.minimalDarkOverlayLight)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CenterFocusStrong,
                                null,
                                tint = DesignSystem.Colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "超焦距 $hyperfocalDistanceText",
                                style = DesignSystem.Typography.caption2,
                                color = DesignSystem.Colors.minimalLabel
                            )
                        }
                    }
                }

                // 连拍计数指示器
                if (isBursting) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignSystem.Colors.recordingRed.copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BurstMode,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "连拍 $burstCount",
                                style = DesignSystem.Typography.caption2,
                                color = Color.White
                            )
                        }
                    }
                }

                // 多重曝光帧计数指示器
                if (multiExposureEnabled && multiExposureFrameCount > 0 && cameraError == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (isBursting) 52.dp else 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignSystem.Colors.minimalDarkOverlayLight)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Layers,
                                null,
                                tint = DesignSystem.Colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "多重曝光 ${multiExposureFrameCount}帧",
                                style = DesignSystem.Typography.caption2,
                                color = DesignSystem.Colors.minimalLabel
                            )
                        }
                    }
                }

                // 视频录制时长指示器
                if (selectedMode == CaptureMode.VIDEO && videoRecordingState.isRecording && cameraError == null) {
                    val recordingPulseAlpha by rememberInfiniteTransition(label = "recPulse").animateFloat(
                        initialValue = 1f,
                        targetValue = 0.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "recPulseAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 16.dp, start = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignSystem.Colors.recordingRed.copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = recordingPulseAlpha))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = videoRecordingState.formattedDuration,
                                style = DesignSystem.Typography.caption1,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 慢动作指示器
                if (selectedMode == CaptureMode.VIDEO && slowMotionEnabled && cameraError == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                top = if (videoRecordingState.isRecording) 56.dp else 16.dp,
                                start = 16.dp
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignSystem.Colors.accentWarm.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SlowMotionVideo,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "慢动作",
                                style = DesignSystem.Typography.caption2,
                                color = Color.White
                            )
                        }
                    }
                }

                // 防抖指示器
                if (selectedMode == CaptureMode.VIDEO && videoStabilizationEnabled && cameraError == null) {
                    val stabTopPadding = if (videoRecordingState.isRecording) {
                        if (slowMotionEnabled) 96.dp else 56.dp
                    } else {
                        if (slowMotionEnabled) 56.dp else 16.dp
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = stabTopPadding, start = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignSystem.Colors.info.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Vibration,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "防抖",
                                style = DesignSystem.Typography.caption2,
                                color = Color.White
                            )
                        }
                    }
                }

                // AI 场景识别指示器
                if (aiSceneName.isNotEmpty() && aiSceneType != com.livecompose.livecapture.core.intelligence.SceneType.UNKNOWN && cameraError == null) {
                    val aiTopPadding = if (selectedMode == CaptureMode.VIDEO) {
                        val base = if (videoRecordingState.isRecording) 56.dp else 16.dp
                        if (slowMotionEnabled) base + 40.dp else base
                    } else {
                        if (hyperfocalEnabled) 56.dp else 16.dp
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = aiTopPadding, start = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignSystem.Colors.primary.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "AI $aiSceneName",
                                style = DesignSystem.Typography.caption2,
                                color = Color.White
                            )
                        }
                    }
                }

                // AI 姿势建议指示器
                if (aiPoseSuggestion.isNotEmpty() && cameraError == null) {
                    val poseTopPadding = if (aiSceneName.isNotEmpty() && aiSceneType != com.livecompose.livecapture.core.intelligence.SceneType.UNKNOWN) {
                        val base = if (hyperfocalEnabled) 56.dp else 16.dp
                        base + 40.dp
                    } else {
                        if (hyperfocalEnabled) 56.dp else 16.dp
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = poseTopPadding, start = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DesignSystem.Colors.accentWarm.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = aiPoseSuggestion,
                            style = DesignSystem.Typography.caption2,
                            color = Color.White
                        )
                    }
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
                onToggleCamera = { triggerCameraFlip(); interactionCounter++ },
                onMagicWandClick = {
                    showFilterRecommendationSheet = true
                    controlsVisible = false
                },
                onBetaClick = {
                    scope.launch {
                        try {
                            // 捕获当前帧并应用自动增强
                            val originalCallback = camera.onPhotoDataReady
                            val photoData = kotlinx.coroutines.CompletableDeferred<ByteArray?>()
                            camera.onPhotoDataReady = { data ->
                                photoData.complete(data)
                            }
                            camera.capturePhoto()
                            val data = photoData.await()
                            camera.onPhotoDataReady = originalCallback
                            if (data != null) {
                                val bitmap = withContext(Dispatchers.Default) {
                                    BitmapFactory.decodeByteArray(data, 0, data.size)
                                }
                                if (bitmap != null) {
                                    val enhanced = withContext(Dispatchers.Default) {
                                        appContainer.autoEnhancer.autoEnhance(bitmap)
                                    }
                                    val stream = java.io.ByteArrayOutputStream()
                                    withContext(Dispatchers.Default) {
                                        enhanced.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                                    }
                                    val enhancedData = stream.toByteArray()
                                    stream.close()
                                    appContainer.photoStorageService.savePhoto(enhancedData, "auto_enhance")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "AI增强完成", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "AI增强失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    interactionCounter++
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 快门控制行（缩略图 | 快门 | AI构图）
            ShutterControlRow(
                galleryRecords = galleryRecords,
                isAligned = isAligned,
                isPipelineEnabled = isPipelineEnabled,
                onCapture = {
                    scope.launch {
                        try {
                            if (selectedMode == CaptureMode.VIDEO) {
                                // 视频模式：切换录制状态
                                if (videoRecordingState.isRecording) {
                                    videoViewModel.stopRecording()
                                    val duration = videoViewModel.recordingState.value.duration
                                    Toast.makeText(context, "视频录制完成 (${"%.1f".format(duration)}秒)", Toast.LENGTH_SHORT).show()
                                    // 显示视频编辑选项
                                    lastRecordedVideoPath = videoViewModel.videoRecorder.lastRecordedPath
                                    if (lastRecordedVideoPath != null) {
                                        try {
                                            val dur = videoViewModel.editor.getVideoDuration(lastRecordedVideoPath!!)
                                            videoEditorStartTime = 0L
                                            videoEditorEndTime = dur
                                        } catch (e: Exception) {
                                            videoEditorEndTime = 0L
                                        }
                                        showVideoEditor = true
                                    }
                                } else {
                                    videoViewModel.startRecording()
                                    Toast.makeText(context, "开始录制视频", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // 拍照模式
                                // RAW 格式处理
                                if (rawCaptureEnabled) {
                                    val rawSupported = RawCaptureManager.isRawSupported(context)
                                    if (rawSupported) {
                                        Toast.makeText(context, "RAW 拍摄已启用", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "设备不支持 RAW 拍摄", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                // DNG 格式处理
                                if (dngCaptureEnabled) {
                                    val dngSupported = dngCaptureManager.isRawSupported()
                                    if (dngSupported) {
                                        dngCaptureManager.onDngSaved = { path ->
                                            Toast.makeText(context, "DNG 已保存", Toast.LENGTH_SHORT).show()
                                        }
                                        dngCaptureManager.onError = { msg ->
                                            Toast.makeText(context, "DNG 错误: $msg", Toast.LENGTH_SHORT).show()
                                        }
                                        dngCaptureManager.captureRaw()
                                    } else {
                                        Toast.makeText(context, "设备不支持 DNG 拍摄", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                // 超焦距对焦
                                if (hyperfocalEnabled) {
                                    val sensorRect = camera.getSensorRect()
                                    if (sensorRect != null) {
                                        camera.tapToFocus(0.5f, 0.5f, sensorRect)
                                    }
                                }
                                // 多重曝光
                                if (multiExposureEnabled) {
                                    multiExposureFrameCount++
                                    Toast.makeText(context, "多重曝光 ${multiExposureFrameCount}帧", Toast.LENGTH_SHORT).show()
                                }
                                // 基础拍照
                                viewModel.capturePhoto()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "拍摄失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                onLongPressStart = {
                    if (burstModeEnabled) {
                        isBursting = true
                        burstCount = 0
                        quickShotManager.startBurst()
                        Toast.makeText(context, "连拍开始", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongPressEnd = {
                    if (burstModeEnabled && isBursting) {
                        isBursting = false
                        val frames = quickShotManager.stopBurst()
                        burstCount = frames.size
                        Toast.makeText(context, "连拍完成，共 ${frames.size} 张", Toast.LENGTH_SHORT).show()
                    }
                },
                isRecording = videoRecordingState.isRecording,
                isVideoMode = selectedMode == CaptureMode.VIDEO
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
                        2 -> { showProfileSheet = true; controlsVisible = false }
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
                onDismiss = { showSettingsSheet = false },
                rawCaptureEnabled = rawCaptureEnabled,
                onRawCaptureChange = { rawCaptureEnabled = it },
                dngCaptureEnabled = dngCaptureEnabled,
                onDngCaptureChange = { dngCaptureEnabled = it },
                hyperfocalEnabled = hyperfocalEnabled,
                onHyperfocalChange = { hyperfocalEnabled = it },
                burstModeEnabled = burstModeEnabled,
                onBurstModeChange = { burstModeEnabled = it },
                multiExposureEnabled = multiExposureEnabled,
                onMultiExposureChange = { multiExposureEnabled = it },
                multiExposureFrameCount = multiExposureFrameCount,
                onMultiExposureReset = {
                    multiExposureFrameCount = 0
                    multiExposureBitmaps = emptyList()
                },
                slowMotionEnabled = slowMotionEnabled,
                onSlowMotionChange = { slowMotionEnabled = it },
                videoStabilizationEnabled = videoStabilizationEnabled,
                onVideoStabilizationChange = { videoViewModel.toggleStabilization() }
            )
        }

        // 我的页面
        if (showProfileSheet) {
            ProfileSheet(
                onDismiss = { showProfileSheet = false },
                onNavigateToSettings = {
                    showProfileSheet = false
                    showSettingsSheet = true
                },
                onNavigateToShootingGuide = {
                    showProfileSheet = false
                    onNavigateToShootingGuide?.invoke()
                },
                onNavigateToPrivacy = {
                    showProfileSheet = false
                    onNavigateToPrivacy?.invoke()
                },
                onNavigateToAgreement = {
                    showProfileSheet = false
                    onNavigateToAgreement?.invoke()
                },
                onNavigateToCommunity = {
                    showProfileSheet = false
                    onNavigateToCommunity?.invoke()
                },
                onNavigateToIcp = {
                    showProfileSheet = false
                    onNavigateToIcp?.invoke()
                }
            )
        }

        // 人像模式浮层
        if (showPortraitMode && cameraError == null) {
            PortraitModeOverlay(
                viewModel = portraitViewModel,
                onDismiss = { showPortraitMode = false },
                onProcessImage = { bitmap -> portraitViewModel.processImage(bitmap) },
                skinProtectionFilter = appContainer.skinProtectionFilter
            )
        }

        // AI滤镜推荐底部弹窗
        if (showFilterRecommendationSheet) {
            FilterRecommendationSheet(
                recommendations = filterRecommendations,
                onDismiss = { showFilterRecommendationSheet = false }
            )
        }

        // 视频编辑浮层
        if (showVideoEditor && lastRecordedVideoPath != null) {
            VideoEditorOverlay(
                videoPath = lastRecordedVideoPath!!,
                videoEditor = videoViewModel.editor,
                startTimeUs = videoEditorStartTime,
                endTimeUs = videoEditorEndTime,
                onDismiss = { showVideoEditor = false },
                onTrimComplete = { outputPath ->
                    showVideoEditor = false
                    if (outputPath != null) {
                        Toast.makeText(context, "视频已保存", Toast.LENGTH_SHORT).show()
                    }
                }
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
    onToggleCamera: () -> Unit,
    onMagicWandClick: () -> Unit = {},
    onBetaClick: () -> Unit = {}
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
            contentDescription = "AI一键增强",
            label = "Beta",
            isBeta = true,
            onClick = onBetaClick
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
            contentDescription = "AI滤镜推荐",
            onClick = onMagicWandClick
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
    onLongPressEnd: () -> Unit = {},
    isRecording: Boolean = false,
    isVideoMode: Boolean = false
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
            onLongPressEnd = onLongPressEnd,
            isRecording = isRecording,
            isVideoMode = isVideoMode
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
    onLongPressEnd: () -> Unit = {},
    isRecording: Boolean = false,
    isVideoMode: Boolean = false
) {
    var isPressedState by remember { mutableStateOf(false) }
    var isLongPressing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = when {
            isRecording || isLongPressing -> 0.88f
            isPressedState -> 0.92f
            else -> 1f
        },
        animationSpec = DesignSystem.Animation.shutterPress,
        label = "shutterScale"
    )

    // 内圈颜色：录制时变红，否则纯白
    val innerColor by animateColorAsState(
        targetValue = if (isRecording || isLongPressing) DesignSystem.Colors.recordingRed else DesignSystem.Colors.shutterInner,
        animationSpec = tween(300),
        label = "shutterColor"
    )

    // 内圈缩放：录制时缩小至65%（变形为方块效果）
    val innerScale by animateFloatAsState(
        targetValue = if (isRecording || isLongPressing) 0.65f else 1f,
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
            .pointerInput(isVideoMode) {
                detectTapGestures(
                    onPress = {
                        isPressedState = true
                        if (isVideoMode) {
                            // 视频模式：轻触即开始/停止录制
                            val released = tryAwaitRelease()
                            isPressedState = false
                            if (released) {
                                HapticManager.light()
                                onCapture()
                            }
                        } else {
                            // 拍照模式：长按触发连拍/录制
                            val longPressJob = coroutineScope.launch {
                                delay(500L)
                                if (!isLongPressing) {
                                    isLongPressing = true
                                    HapticManager.success()
                                    onLongPressStart()
                                }
                            }
                            val released = tryAwaitRelease()
                            longPressJob.cancel()
                            isPressedState = false
                            if (released && !isLongPressing) {
                                HapticManager.light()
                                onCapture()
                            } else if (isLongPressing) {
                                isLongPressing = false
                                onLongPressEnd()
                            }
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
                    color = if (isRecording || isLongPressing) DesignSystem.Colors.recordingRed.copy(alpha = 0.6f)
                        else DesignSystem.Colors.shutterOuterRing,
                    shape = CircleShape
                )
        )

        // 内圈：录制时变形为圆角方块
        if (isRecording || isLongPressing) {
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
private fun SettingsBottomSheet2026(
    onDismiss: () -> Unit,
    rawCaptureEnabled: Boolean,
    onRawCaptureChange: (Boolean) -> Unit,
    dngCaptureEnabled: Boolean,
    onDngCaptureChange: (Boolean) -> Unit,
    hyperfocalEnabled: Boolean,
    onHyperfocalChange: (Boolean) -> Unit,
    burstModeEnabled: Boolean,
    onBurstModeChange: (Boolean) -> Unit,
    multiExposureEnabled: Boolean,
    onMultiExposureChange: (Boolean) -> Unit,
    multiExposureFrameCount: Int,
    onMultiExposureReset: () -> Unit,
    slowMotionEnabled: Boolean = false,
    onSlowMotionChange: (Boolean) -> Unit = {},
    videoStabilizationEnabled: Boolean = true,
    onVideoStabilizationChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var captureDelay by remember { mutableStateOf(1.0) }
    var gridMode by remember { mutableIntStateOf(0) }
    var phantomModeEnabled by remember { mutableStateOf(false) }
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
                SettingsSwitchRow("RAW 格式", "全链路 RAW 传感器数据保存", Icons.Default.RawOn, rawCaptureEnabled) { onRawCaptureChange(it) }
                SettingsDivider()
                SettingsSwitchRow("DNG 格式", "标准 DNG 数字负片格式保存", Icons.Default.Image, dngCaptureEnabled) { onDngCaptureChange(it) }
            }

            SettingsSectionHeader("专业功能", Icons.Default.Tune)
            SettingsCard {
                SettingsSwitchRow("超焦距对焦", "自动对焦至超焦距距离，前后景均清晰", Icons.Default.CenterFocusStrong, hyperfocalEnabled) { onHyperfocalChange(it) }
                SettingsDivider()
                SettingsSwitchRow("连拍模式", "长按快门触发高速连拍", Icons.Default.BurstMode, burstModeEnabled) { onBurstModeChange(it) }
                SettingsDivider()
                SettingsSwitchRow("多重曝光", "每次拍摄叠加到前一张画面", Icons.Default.Layers, multiExposureEnabled) { onMultiExposureChange(it) }
                if (multiExposureEnabled) {
                    SettingsDivider()
                    SettingsRow("曝光层数", "当前已叠加 ${multiExposureFrameCount} 帧", Icons.Default.Layers) {
                        TextButton(onClick = {
                            onMultiExposureReset()
                            Toast.makeText(context, "多重曝光已重置", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("重置", color = DesignSystem.Colors.error, style = DesignSystem.Typography.caption1)
                        }
                    }
                }
            }

            SettingsSectionHeader("幻影模式", Icons.Default.Visibility)
            SettingsCard {
                SettingsSwitchRow("幻影模式", "监听系统相机输出，自动应用 LUT 色彩处理", Icons.Default.VisibilityOff, phantomModeEnabled) { phantomModeEnabled = it }
            }

            SettingsSectionHeader("视频录制", Icons.Default.Videocam)
            SettingsCard {
                SettingsSwitchRow("慢动作", "拍摄高帧率视频，以慢动作播放", Icons.Default.SlowMotionVideo, slowMotionEnabled) { onSlowMotionChange(it) }
                SettingsDivider()
                SettingsSwitchRow("视频防抖", "录制时启用电子防抖，减少画面抖动", Icons.Default.Vibration, videoStabilizationEnabled) { onVideoStabilizationChange(it) }
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
                SettingsRow("版本信息", "构妙 LiveCapture v1.1.7", Icons.Default.Info)
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
    onProcessImage: (android.graphics.Bitmap) -> Unit,
    skinProtectionFilter: com.livecompose.livecapture.core.filter.SkinProtectionFilter
) {
    val skinSmoothing by viewModel.skinSmoothing.collectAsState()
    val skinTone by viewModel.skinTone.collectAsState()
    val faceSlimming by viewModel.faceSlimming.collectAsState()
    val eyeBrightening by viewModel.eyeBrightening.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val processedPreview by viewModel.processedPreview.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val hasPortrait by viewModel.hasPortrait.collectAsState()
    val faceCount by viewModel.faceCount.collectAsState()

    // 进出场动画
    var isVisible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "portrait_overlay_alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 120f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "portrait_overlay_offset"
    )

    LaunchedEffect(Unit) { isVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffset
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small, vertical = DesignSystem.Spacing.xxSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    isVisible = false
                    // 延迟关闭以播放动画
                    kotlinx.coroutines.MainScope().launch {
                        delay(400)
                        onDismiss()
                    }
                }) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "人像模式",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {
                    isVisible = false
                    kotlinx.coroutines.MainScope().launch {
                        delay(400)
                        onDismiss()
                    }
                }) {
                    Text("完成", color = DesignSystem.Colors.primary)
                }
            }

            // 人像预览区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
                    .height(280.dp)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.08f),
                contentAlignment = Alignment.Center
            ) {
                if (processedPreview != null) {
                    Image(
                        bitmap = processedPreview!!.asImageBitmap(),
                        contentDescription = "人像预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = DesignSystem.Colors.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                        Text(
                            "处理中…",
                            style = DesignSystem.Typography.subheadline,
                            color = DesignSystem.Colors.minimalLabelSecondary
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = null,
                            tint = DesignSystem.Colors.minimalLabelTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                        Text(
                            "人像预览",
                            style = DesignSystem.Typography.subheadline,
                            color = DesignSystem.Colors.minimalLabelTertiary
                        )
                        if (faceCount > 0) {
                            Text(
                                "检测到 ${faceCount} 张人脸",
                                style = DesignSystem.Typography.caption1,
                                color = DesignSystem.Colors.minimalLabelQuaternary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 预设选择
            Text(
                "预设",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxxSmall)
            ) {
                PortraitBeautyPreset.entries.forEach { preset ->
                    val isSelected = preset == currentPreset
                    val bgColor = if (isSelected) DesignSystem.Colors.primary
                    else DesignSystem.Colors.minimalOverlayMedium
                    val textColor = if (isSelected) Color.White
                    else DesignSystem.Colors.minimalLabelSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
                            .background(bgColor)
                            .clickable { viewModel.applyPreset(preset) }
                            .padding(vertical = DesignSystem.Spacing.xxSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            preset.displayName,
                            style = DesignSystem.Typography.caption1,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 美颜参数调节
            Text(
                "美颜参数",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

            // 参数调节卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.06f)
                    .padding(DesignSystem.Spacing.small)
            ) {
                // 磨皮
                BeautySliderRow(
                    label = "磨皮",
                    icon = Icons.Default.Face,
                    value = skinSmoothing,
                    onValueChange = { viewModel.setSkinSmoothing(it) },
                    valueRange = 0f..1f
                )

                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

                // 美白
                BeautySliderRow(
                    label = "美白",
                    icon = Icons.Default.BrightnessHigh,
                    value = skinTone,
                    onValueChange = { viewModel.setSkinTone(it) },
                    valueRange = -1f..1f,
                    displayTransform = { value ->
                        when {
                            value < -0.3f -> "冷白"
                            value < 0.3f -> "自然"
                            else -> "暖黄"
                        }
                    }
                )

                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

                // 瘦脸
                BeautySliderRow(
                    label = "瘦脸",
                    icon = Icons.Default.Face3,
                    value = faceSlimming,
                    onValueChange = { viewModel.setFaceSlimming(it) },
                    valueRange = 0f..1f
                )

                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

                // 大眼
                BeautySliderRow(
                    label = "大眼",
                    icon = Icons.Default.RemoveRedEye,
                    value = eyeBrightening,
                    onValueChange = { viewModel.setEyeBrightening(it) },
                    valueRange = 0f..1f
                )
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 人像虚化
            val portraitBlur by viewModel.portraitBlur.collectAsState()
            Text(
                "人像虚化",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.06f)
                    .padding(DesignSystem.Spacing.small)
            ) {
                BeautySliderRow(
                    label = "虚化",
                    icon = Icons.Default.BlurOn,
                    value = portraitBlur,
                    onValueChange = { viewModel.setPortraitBlur(it) },
                    valueRange = 0f..1f
                )
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 皮肤保护开关
            var skinProtectionEnabled by remember { mutableStateOf(true) }
            Text(
                "AI皮肤保护",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.06f)
                    .padding(horizontal = DesignSystem.Spacing.small, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Face,
                    null,
                    tint = if (skinProtectionEnabled) DesignSystem.Colors.primary
                    else DesignSystem.Colors.minimalLabelTertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "皮肤保护",
                        style = DesignSystem.Typography.headline,
                        color = DesignSystem.Colors.textPrimary()
                    )
                    Text(
                        "应用滤镜时保护皮肤区域不被过度染色",
                        style = DesignSystem.Typography.caption2,
                        color = DesignSystem.Colors.minimalLabelSecondary
                    )
                }
                Switch(
                    checked = skinProtectionEnabled,
                    onCheckedChange = {
                        skinProtectionEnabled = it
                        skinProtectionFilter.skinFilterIntensity = if (it) 0.3f else 1.0f
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DesignSystem.Colors.primary,
                        uncheckedThumbColor = DesignSystem.Colors.gray4(),
                        uncheckedTrackColor = DesignSystem.Colors.gray3()
                    )
                )
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xSmall)
            ) {
                OutlinedButton(
                    onClick = {
                        isVisible = false
                        kotlinx.coroutines.MainScope().launch {
                            delay(400)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                    border = BorderStroke(DesignSystem.Stroke.widthStandard, DesignSystem.Colors.minimalBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignSystem.Colors.minimalLabel)
                ) {
                    Text("取消", style = DesignSystem.Typography.headline)
                }
                Button(
                    onClick = {
                        isVisible = false
                        kotlinx.coroutines.MainScope().launch {
                            delay(400)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignSystem.Colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("应用", style = DesignSystem.Typography.headline)
                }
            }

            Spacer(Modifier.height(DesignSystem.Spacing.large))
        }
    }
}

@Composable
private fun BeautySliderRow(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayTransform: ((Float) -> String)? = null
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = DesignSystem.Colors.minimalLabelSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(DesignSystem.Spacing.xxxSmall))
            Text(
                label,
                style = DesignSystem.Typography.subheadline,
                color = DesignSystem.Colors.minimalLabel
            )
            Spacer(Modifier.weight(1f))
            Text(
                displayTransform?.invoke(value) ?: "${(value * 100).toInt()}%",
                style = DesignSystem.Typography.caption1,
                color = DesignSystem.Colors.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = DesignSystem.Colors.primary,
                activeTrackColor = DesignSystem.Colors.primary,
                inactiveTrackColor = DesignSystem.Colors.minimalOverlayMedium,
                activeTickColor = DesignSystem.Colors.primary,
                inactiveTickColor = DesignSystem.Colors.minimalOverlay
            )
        )
    }
}

// ====== 视频编辑浮层 ======

@Composable
private fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(DesignSystem.Colors.minimalSurface),
                contentAlignment = Alignment.Center
            ) {
                // 尝试加载缩略图
                var thumbnail by remember(videoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(videoPath) {
                    thumbnail = try {
                        videoEditor.generateThumbnail(videoPath)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "视频预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Videocam,
                            null,
                            tint = DesignSystem.Colors.minimalLabelTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "视频预览",
                            style = DesignSystem.Typography.caption1,
                            color = DesignSystem.Colors.minimalLabelTertiary
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 裁剪控制区
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "裁剪范围",
                    style = DesignSystem.Typography.headline,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))

                // 开始时间滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "起始",
                        style = DesignSystem.Typography.caption1,
                        color = DesignSystem.Colors.minimalLabelSecondary,
                        modifier = Modifier.width(40.dp)
                    )
                    Slider(
                        value = trimStart,
                        onValueChange = { newVal ->
                            if (newVal < trimEnd) trimStart = newVal
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = DesignSystem.Colors.primary,
                            activeTrackColor = DesignSystem.Colors.primary,
                            inactiveTrackColor = DesignSystem.Colors.minimalOverlayStrong
                        )
                    )
                    Text(
                        text = formatVideoTime((totalDuration * trimStart / 1_000_000).toLong()),
                        style = DesignSystem.Typography.caption2,
                        color = DesignSystem.Colors.minimalLabelSecondary,
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 结束时间滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "结束",
                        style = DesignSystem.Typography.caption1,
                        color = DesignSystem.Colors.minimalLabelSecondary,
                        modifier = Modifier.width(40.dp)
                    )
                    Slider(
                        value = trimEnd,
                        onValueChange = { newVal ->
                            if (newVal > trimStart) trimEnd = newVal
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = DesignSystem.Colors.primary,
                            activeTrackColor = DesignSystem.Colors.primary,
                            inactiveTrackColor = DesignSystem.Colors.minimalOverlayStrong
                        )
                    )
                    Text(
                        text = formatVideoTime((totalDuration * trimEnd / 1_000_000).toLong()),
                        style = DesignSystem.Typography.caption2,
                        color = DesignSystem.Colors.minimalLabelSecondary,
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 时长信息
                Text(
                    text = "裁剪后时长: ${formatVideoTime((totalDuration * (trimEnd - trimStart) / 1_000_000).toLong())}",
                    style = DesignSystem.Typography.caption1,
                    color = DesignSystem.Colors.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 处理中遮罩
        if (isTrimming) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = DesignSystem.Colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "正在处理视频...",
                        style = DesignSystem.Typography.headline,
                        color = DesignSystem.Colors.minimalLabel
                    )
                }
            }
        }
    }
}

/**
 * 格式化视频时长
 */
private fun formatVideoTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

// ====== AI滤镜推荐底部弹窗 ======

@Composable
private fun FilterRecommendationSheet(
    recommendations: List<FilterRecommendation>,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DesignSystem.Colors.backgroundPrimary())
                .clickable(enabled = false, onClick = {})
        ) {
            // 拖动指示器
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
                Icon(
                    Icons.Default.AutoAwesome,
                    null,
                    tint = DesignSystem.Colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI 滤镜推荐",
                    style = DesignSystem.Typography.title2,
                    color = DesignSystem.Colors.textPrimary(),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = DesignSystem.Colors.primary)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (recommendations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            null,
                            tint = DesignSystem.Colors.minimalLabelTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "AI正在分析场景...",
                            style = DesignSystem.Typography.subheadline,
                            color = DesignSystem.Colors.minimalLabelTertiary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "请确保相机对着拍摄场景",
                            style = DesignSystem.Typography.caption2,
                            color = DesignSystem.Colors.minimalLabelQuaternary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recommendations) { recommendation ->
                        FilterRecommendationCard(recommendation = recommendation)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterRecommendationCard(
    recommendation: FilterRecommendation
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .liquidGlass(cornerRadius = DesignSystem.CornerRadius.medium, intensity = 0.06f)
            .clickable {
                // 点击应用滤镜
                scope.launch {
                    try {
                        val appContainer = AppContainer.getInstance(context)
                        // 这里可以触发拍照并应用推荐的滤镜
                        Toast.makeText(context, "已选择: ${recommendation.preset.name}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "应用滤镜失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 预设图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DesignSystem.Colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FilterVintage,
                    null,
                    tint = DesignSystem.Colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recommendation.preset.name,
                        style = DesignSystem.Typography.headline,
                        color = DesignSystem.Colors.textPrimary(),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(6.dp))
                    // 置信度标签
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    recommendation.confidence >= 0.8f -> DesignSystem.Colors.success.copy(alpha = 0.15f)
                                    recommendation.confidence >= 0.6f -> DesignSystem.Colors.warning.copy(alpha = 0.15f)
                                    else -> DesignSystem.Colors.minimalOverlayMedium
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${(recommendation.confidence * 100).toInt()}%",
                            style = DesignSystem.Typography.caption2,
                            color = when {
                                recommendation.confidence >= 0.8f -> DesignSystem.Colors.success
                                recommendation.confidence >= 0.6f -> DesignSystem.Colors.warning
                                else -> DesignSystem.Colors.minimalLabelSecondary
                            }
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    recommendation.reason,
                    style = DesignSystem.Typography.caption2,
                    color = DesignSystem.Colors.minimalLabelSecondary
                )
            }
        }
    }
}

// ====== 预设参数映射 ======
private fun presetParamsFor(preset: BeautyPreset): BeautyQuickParams = when (preset) {
    BeautyPreset.NATURAL -> BeautyQuickParams(smoothing = 0.2f, whitening = 0.1f, slimFace = 0.05f, enlargeEye = 0.05f)
    BeautyPreset.FAIR -> BeautyQuickParams(smoothing = 0.5f, whitening = 0.6f, slimFace = 0.2f, enlargeEye = 0.15f)
    BeautyPreset.VIBRANT -> BeautyQuickParams(smoothing = 0.4f, whitening = 0.3f, slimFace = 0.25f, enlargeEye = 0.3f)
    BeautyPreset.PREMIUM -> BeautyQuickParams(smoothing = 0.3f, whitening = 0.15f, slimFace = 0.15f, enlargeEye = 0.1f)
    BeautyPreset.CUSTOM -> BeautyQuickParams()
}

// ====== ProfileSheet 我的页面浮层 ======

@Composable
private fun ProfileSheet(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToShootingGuide: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAgreement: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToIcp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.backgroundPrimary())
            .systemBarsPadding()
    ) {
        // 关闭按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(DesignSystem.Spacing.small)
                .statusBarsPadding()
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "关闭",
                tint = DesignSystem.Colors.textPrimary()
            )
        }

        ProfileScreen(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToShootingGuide = onNavigateToShootingGuide,
            onNavigateToPrivacy = onNavigateToPrivacy,
            onNavigateToAgreement = onNavigateToAgreement,
            onNavigateToCommunity = onNavigateToCommunity,
            onNavigateToIcp = onNavigateToIcp,
            modifier = Modifier.fillMaxSize()
        )
    }
}
