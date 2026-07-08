package com.livecompose.livecapture.features.capture

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.CameraPreview
import com.livecompose.livecapture.core.camera.ZoomPreset
import com.livecompose.livecapture.features.capture.components.*
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 主拍摄界面
 * 对应 iOS 的 CaptureView
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
    val isFrontCamera by remember { mutableStateOf(false) }

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

        // 构图叠加层
        CompositionOverlay(
            compositionRect = viewModel.compositionRectInView.collectAsState().value,
            cropRect = cropRect,
            boxCenter = boxCenter,
            isAligned = isAligned,
            distanceToCenter = distanceToCenter
        )

        // 拍照闪光
        if (captureFlashOpacity > 0f) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = captureFlashOpacity))
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

            // 调试面板
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