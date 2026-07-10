package com.livecompose.livecapture.presentation.capture

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.livecompose.livecapture.core.design.*
import kotlinx.coroutines.launch

@Composable
fun CaptureView(
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val stage by viewModel.pipelineStage.collectAsState()
    val guidanceText by viewModel.guidanceText.collectAsState()
    val trackPoint by viewModel.trackPoint.collectAsState()
    val isAligned by viewModel.isAligned.collectAsState()
    val alignmentProgress by viewModel.alignmentProgress.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val isBackCamera by viewModel.isBackCamera.collectAsState()

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            viewModel.startCamera(lifecycleOwner, previewView)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopCamera()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Composition Grid Overlay
        CompositionGridOverlay()

        // Tracking Dot
        trackPoint?.let { point ->
            TrackingDot(
                position = point,
                isAligned = isAligned,
                progress = alignmentProgress,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Control Bar
        TopControlBar(
            guidanceText = guidanceText,
            isAligned = isAligned,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom Controls
        BottomControls(
            zoomRatio = zoomRatio,
            isBackCamera = isBackCamera,
            onZoomChange = { viewModel.setZoom(it) },
            onSwitchCamera = { viewModel.switchCamera(lifecycleOwner, previewView) },
            onCapture = { viewModel.manualCapture() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CompositionGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.dp.toPx()

        // Vertical lines (1/3, 2/3)
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

        // Horizontal lines (1/3, 2/3)
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
    progress: Float,
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

            // Outer ring
            drawCircle(
                color = dotColor.copy(alpha = 0.3f),
                radius = radius * pulseScale,
                center = Offset(x, y)
            )

            // Inner dot
            drawCircle(
                color = dotColor,
                radius = radius * 0.6f,
                center = Offset(x, y)
            )

            // Crosshair when aligned
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (guidanceText.isNotEmpty()) {
            Surface(
                color = GuidanceBg,
                shape = RoundedCornerShape(CornerMedium)
            ) {
                Text(
                    text = guidanceText,
                    style = GuidanceTextStyle,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
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
private fun BottomControls(
    zoomRatio: Float,
    isBackCamera: Boolean,
    onZoomChange: (Float) -> Unit,
    onSwitchCamera: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZoomButton("0.5x", zoomRatio == 0.5f) { onZoomChange(0.5f) }
            ZoomButton("1x", zoomRatio == 1.0f) { onZoomChange(1.0f) }
            ZoomButton("2x", zoomRatio == 2.0f) { onZoomChange(2.0f) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Capture Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery preview button
            IconButton(onClick = { /* TODO: open gallery */ }) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Capture button
            CaptureButton(onClick = onCapture)

            // Switch camera button
            IconButton(onClick = onSwitchCamera) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
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
private fun CaptureButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.White, CircleShape)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                )
            }
        }
    }
}
