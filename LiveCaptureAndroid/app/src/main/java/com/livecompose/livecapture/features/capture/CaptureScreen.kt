package com.livecompose.livecapture.features.capture

import android.content.Intent
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.CameraPreview
import com.livecompose.livecapture.features.capture.components.*
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 相机错误类型
 */
enum class CameraErrorType {
    PERMISSION_DENIED,
    CAMERA_IN_USE,
    NO_CAMERA_HARDWARE,
    UNKNOWN
}

/**
 * 主拍摄界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    viewModel: CaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDebugInfo by remember { mutableStateOf(false) }
    var captureAnimationScale by remember { mutableStateOf(1f) }
    var captureFlashOpacity by remember { mutableStateOf(0f) }
    var cameraFlipRotation by remember { mutableStateOf(0f) }

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

    // 相机错误状态
    var cameraError by remember { mutableStateOf<CameraErrorType?>(null) }
    var cameraErrorRetryCounter by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(Unit) {
        viewModel.onAppear()
        viewModel.onCaptureTriggered = {
            captureFlashOpacity = 0.8f
            captureAnimationScale = 2.0f
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onDisappear() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 黑色背景
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))

        // 相机预览
        if (cameraError == null) {
            CameraPreview(
                cameraManager = viewModel.camera,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(captureAnimationScale)
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
        if (captureFlashOpacity > 0f) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = captureFlashOpacity))
            )
        }

        // 相机错误覆盖层
        cameraError?.let { error ->
            CameraErrorOverlay(
                errorType = error,
                onRetry = {
                    cameraErrorRetryCounter++
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

        // UI 层
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部控制栏
            TopControlBar(
                userGuidanceText = userGuidanceText,
                showDebugInfo = showDebugInfo,
                isAutoCaptureEnabled = isAutoCapture,
                captureDelay = captureDelay,
                onBack = onBack,
                onToggleDebug = { showDebugInfo = !showDebugInfo },
                onToggleCamera = {
                    cameraFlipRotation += 180f
                    viewModel.toggleCameraPosition()
                },
                onToggleAutoCapture = { viewModel.toggleAutoCapture() },
                onSetCaptureDelay = { viewModel.setCaptureDelay(it) }
            )

            // 调试面板（仅在 DEBUG 构建中可用）
            if (com.livecompose.livecapture.BuildConfig.DEBUG) {
                AnimatedVisibility(visible = showDebugInfo) {
                    DebugPanel(
                        debugMessage = debugMessage,
                        motionIsStable = motionStable,
                        boxCenterInView = boxCenter,
                        distanceToCenter = distanceToCenter,
                        detectionReady = detectionReady,
                        zoomDisplayText = viewModel.zoomDisplayText,
                        focalLengthText = viewModel.focalLengthText,
                        isAligned = isAligned,
                        onClose = { showDebugInfo = false }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部控制区
            BottomSection(
                zoomPresets = zoomPresets,
                zoomRange = zoomRange,
                zoomState = zoomState,
                isPipelineEnabled = isPipelineEnabled,
                onSelectPreset = { viewModel.selectZoomPreset(it) },
                onZoomDrag = { viewModel.updateZoomInteractively(it) },
                onZoomDragEnd = { viewModel.finalizeZoomInteractively(it) },
                onTogglePipeline = { viewModel.toggleCompositionPipeline() },
                onCapture = { viewModel.capturePhoto() },
                onReset = { viewModel.resetDetectionState() },
                onToggleCamera = {
                    cameraFlipRotation += 180f
                    viewModel.toggleCameraPosition()
                }
            )
        }
    }
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
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // 错误图标
            Icon(
                imageVector = when (errorType) {
                    CameraErrorType.PERMISSION_DENIED -> Icons.Default.NoPhotography
                    CameraErrorType.CAMERA_IN_USE -> Icons.Default.Cameraswitch
                    CameraErrorType.NO_CAMERA_HARDWARE -> Icons.Default.CameraAlt
                    CameraErrorType.UNKNOWN -> Icons.Default.ErrorOutline
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFF3B30)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 错误标题
            Text(
                text = when (errorType) {
                    CameraErrorType.PERMISSION_DENIED -> "相机权限被拒绝"
                    CameraErrorType.CAMERA_IN_USE -> "相机被占用"
                    CameraErrorType.NO_CAMERA_HARDWARE -> "无可用相机"
                    CameraErrorType.UNKNOWN -> "相机打开失败"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 错误原因说明
            Text(
                text = when (errorType) {
                    CameraErrorType.PERMISSION_DENIED -> "需要相机权限才能拍摄照片，请在系统设置中授权相机权限"
                    CameraErrorType.CAMERA_IN_USE -> "相机正在被其他应用使用，请关闭其他应用后重试"
                    CameraErrorType.NO_CAMERA_HARDWARE -> "设备未检测到相机硬件，无法使用拍摄功能"
                    CameraErrorType.UNKNOWN -> "相机无法正常启动，请尝试重启应用或检查设备状态"
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 重试按钮
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
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("重试", fontSize = 16.sp)
            }

            // 如果是权限问题，显示"去设置"按钮
            if (errorType == CameraErrorType.PERMISSION_DENIED) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onGoToSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f))
                    )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("去设置", fontSize = 16.sp)
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

        // 绘制检测框
        cropRect?.let { rect ->
            val left = rect.left * canvasWidth
            val top = rect.top * canvasHeight
            val right = rect.right * canvasWidth
            val bottom = rect.bottom * canvasHeight
            drawRect(
                color = if (isAligned) Color(0xFF00C853) else Color(0xFFFFAB00),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 3f)
            )
        }

        // 绘制追踪点
        boxCenter?.let { center ->
            val cx = center.x * canvasWidth
            val cy = center.y * canvasHeight
            val radius = 15f
            drawCircle(
                color = if (isAligned) Color(0xFF00C853) else Color.White,
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF007AFF).copy(alpha = 0.3f),
                radius = radius + 8f,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = Stroke(width = 2f)
            )
        }
    }
}