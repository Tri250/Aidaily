package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.camera.ZoomPreset
import com.livecompose.livecapture.core.camera.ZoomState
import com.livecompose.livecapture.ui.components.SecondaryCircleButton
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 底部控制区
 */
@Composable
fun BottomSection(
    zoomPresets: List<ZoomPreset>,
    zoomRange: ClosedFloatingPointRange<Float>,
    zoomState: ZoomState,
    isPipelineEnabled: Boolean,
    onSelectPreset: (ZoomPreset) -> Unit,
    onZoomDrag: (Float) -> Unit,
    onZoomDragEnd: (Float) -> Unit,
    onTogglePipeline: () -> Unit,
    onCapture: () -> Unit,
    onReset: () -> Unit,
    onToggleCamera: () -> Unit,
    onNavigateToGallery: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.9f)),
                    startY = -100f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        // 变焦条
        if (zoomPresets.isNotEmpty()) {
            ZoomControlBar(
                zoomPresets = zoomPresets,
                zoomRange = zoomRange,
                zoomState = zoomState,
                onSelectPreset = onSelectPreset,
                onZoomDrag = onZoomDrag,
                onZoomDragEnd = onZoomDragEnd
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 主控制行
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 图库
            SecondaryCircleButton(icon = Icons.Default.PhotoLibrary, onClick = onNavigateToGallery)

            // 构图按钮
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                var scale by remember { mutableFloatStateOf(1f) }
                val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPipelineEnabled) DesignSystem.Colors.primary else Color.White.copy(alpha = 0.15f)
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
                        contentDescription = "构图",
                        tint = if (isPipelineEnabled) Color.White else DesignSystem.Colors.textTertiary(),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isPipelineEnabled) "已开启" else "智能构图",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // 拍摄按钮
            CaptureButton(onCapture = onCapture)

            // 重置按钮
            SecondaryCircleButton(icon = Icons.Default.Refresh, onClick = onReset)

            // 切换摄像头
            SecondaryCircleButton(icon = Icons.Default.FlipCameraAndroid, onClick = onToggleCamera)
        }
    }
}

@Composable
private fun ZoomControlBar(
    zoomPresets: List<ZoomPreset>,
    zoomRange: ClosedFloatingPointRange<Float>,
    zoomState: ZoomState,
    onSelectPreset: (ZoomPreset) -> Unit,
    onZoomDrag: (Float) -> Unit,
    onZoomDragEnd: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 变焦预设按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            zoomPresets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (preset.zoomFactor == zoomState.currentFactor) Color.White.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            preset.label,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = if (preset.zoomFactor == zoomState.currentFactor) FontWeight(600) else FontWeight(400)
                        )
                        Text(
                            preset.focalLengthLabel,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // 变焦滑块
        if (zoomRange.endInclusive > zoomRange.start) {
            Spacer(modifier = Modifier.height(8.dp))
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
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun CaptureButton(onCapture: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.3f))
            .clickable {
                scale = 0.9f
                onCapture()
                scale = 1f
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun ZoomRingView(
    zoomState: ZoomState,
    zoomRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(56.dp)) {
        val strokeWidth = 4f
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // 背景环
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // 进度环
        val range = zoomRange.endInclusive - zoomRange.start
        val progress = if (range > 0f) (zoomState.currentFactor - zoomRange.start) / range else 0f
        drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )

        // 镜头标记
        val markAngle = -90f + 360f * progress
        val markRadius = radius - strokeWidth / 2
        val markX = center.x + markRadius * kotlin.math.cos(Math.toRadians(markAngle.toDouble())).toFloat()
        val markY = center.y + markRadius * kotlin.math.sin(Math.toRadians(markAngle.toDouble())).toFloat()
        drawCircle(
            color = Color.White,
            radius = 5f,
            center = Offset(markX, markY)
        )
    }
}