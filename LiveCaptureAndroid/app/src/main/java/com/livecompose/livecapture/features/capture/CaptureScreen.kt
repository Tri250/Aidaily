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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

    // 控件可见性
    var controlsVisible by remember { mutableStateOf(true) }
    var showManualPanel by remember { mutableStateOf(false) }
    var showPhotoReview by remember { mutableStateOf(false) }
    var reviewData by remember { mutableStateOf<ByteArray?>(null) }

    // 图库 / 设置浮层
    var showGallerySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

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

    // 图库数据
    val galleryRecords by homeViewModel.records.collectAsState()

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
        if (controlsVisible && cameraError == null && !showManualPanel && !showGallerySheet && !showSettingsSheet) {
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
    ) {
        // 相机预览 - 全屏
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
                onDelete = { showPhotoReview = false }
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
                onToggleLevel = { showLevel = !showLevel },
                onOpenSettings = {
                    showSettingsSheet = true
                    controlsVisible = false
                }
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
                galleryRecords = galleryRecords,
                onSelectPreset = { viewModel.selectZoomPreset(it) },
                onZoomDrag = { viewModel.updateZoomInteractively(it) },
                onZoomDragEnd = { viewModel.finalizeZoomInteractively(it) },
                onTogglePipeline = { viewModel.toggleCompositionPipeline() },
                onCapture = { viewModel.capturePhoto() },
                onToggleCamera = { cameraFlipRotation += 180f; viewModel.toggleCameraPosition() },
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
                onCaptureModeChange = { captureMode = it },
                onPhotoClick = { photoId ->
                    onNavigateToPhotoDetail?.invoke(photoId)
                }
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
    onToggleLevel: () -> Unit,
    onOpenSettings: () -> Unit
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
            // 设置（新增）
            TopIconButton(Icons.Default.Settings, false, onOpenSettings)
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
private fun TopIconButton(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
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
    galleryRecords: List<PhotoRecord>,
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
    onCaptureModeChange: (Int) -> Unit,
    onPhotoClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        // 图库缩略条（最近照片）
        if (galleryRecords.isNotEmpty()) {
            GalleryThumbnailStrip(
                records = galleryRecords.take(15),
                onOpenGallery = onOpenGallery,
                onPhotoClick = onPhotoClick
            )
            Spacer(Modifier.height(8.dp))
        }

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
            GalleryThumbBtn(onClick = onOpenGallery)

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
                    // 跳转 ICP 备案页面
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
            1 -> {
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width/3, 0f), end = androidx.compose.ui.geometry.Offset(size.width/3, size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width*2/3, 0f), end = androidx.compose.ui.geometry.Offset(size.width*2/3, size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height/3), end = androidx.compose.ui.geometry.Offset(size.width, size.height/3), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height*2/3), end = androidx.compose.ui.geometry.Offset(size.width, size.height*2/3), strokeWidth = 1f)
            }
            2 -> {
                val phi = 0.618f
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width*phi, 0f), end = androidx.compose.ui.geometry.Offset(size.width*phi, size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width*(1-phi), 0f), end = androidx.compose.ui.geometry.Offset(size.width*(1-phi), size.height), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height*phi), end = androidx.compose.ui.geometry.Offset(size.width, size.height*phi), strokeWidth = 1f)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height*(1-phi)), end = androidx.compose.ui.geometry.Offset(size.width, size.height*(1-phi)), strokeWidth = 1f)
            }
            3 -> {
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