package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.utilities.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 2026旗舰影像快门按钮 - 大号双层圆环设计
 *
 * 视觉层级：
 * - 外层：3dp 白色半透明描边圆环（35% alpha）
 * - 内层：实心白色圆（95% alpha）
 * - 按压时：整体缩放至 92%，内圈缩至 85%
 * - 对齐成功时：外层出现金色光环脉动
 * - 视频录制：内层变形为圆角方块 + 变红
 */
@Composable
fun RedesignedShutterButton(
    modifier: Modifier = Modifier,
    isAligned: Boolean = false,
    isRecording: Boolean = false,
    onCapture: () -> Unit = {},
    onLongPressStart: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    onBurstCapture: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var isLongPressing by remember { mutableStateOf(false) }
    var isBurstMode by remember { mutableStateOf(false) }
    val ripples = remember { mutableStateListOf<RippleAnim>() }

    // 连拍旋转
    val burstTransition = rememberInfiniteTransition(label = "burstRotation")
    val burstRotation by burstTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "burstRotation"
    )

    // 缩放动画
    val containerScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = DesignSystem.Animation.shutterPress,
        label = "containerScale"
    )
    val innerScale by animateFloatAsState(
        targetValue = when {
            isLongPressing && isRecording -> 0.7f
            isPressed -> 0.85f
            else -> 1f
        },
        animationSpec = DesignSystem.Animation.shutterPress,
        label = "innerScale"
    )

    // 形状变形（圆 → 圆角方块）
    val cornerMorphProgress by animateFloatAsState(
        targetValue = if (isLongPressing && isRecording) 1f else 0f,
        animationSpec = DesignSystem.Animation.shutterLongPress,
        label = "cornerMorph"
    )

    // 颜色动画
    val innerColor by animateColorAsState(
        targetValue = if (isLongPressing && isRecording) DesignSystem.Colors.recordingRed
        else DesignSystem.Colors.shutterInner,
        animationSpec = tween(durationMillis = 300),
        label = "innerColor"
    )

    // 金色光环脉动
    val glowScaleAnimatable = remember { Animatable(1.0f) }
    val glowAlphaAnimatable = remember { Animatable(0.8f) }

    LaunchedEffect(isAligned) {
        if (isAligned) {
            coroutineScope.launch {
                while (true) {
                    glowScaleAnimatable.animateTo(1.15f, DesignSystem.Animation.shutterGlowPulse)
                    glowScaleAnimatable.animateTo(1.0f, DesignSystem.Animation.shutterGlowPulse)
                }
            }
            coroutineScope.launch {
                while (true) {
                    glowAlphaAnimatable.animateTo(0.3f, DesignSystem.Animation.shutterGlowPulse)
                    glowAlphaAnimatable.animateTo(0.8f, DesignSystem.Animation.shutterGlowPulse)
                }
            }
        } else {
            glowScaleAnimatable.snapTo(1.0f)
            glowAlphaAnimatable.snapTo(0.8f)
        }
    }
    val glowScale by glowScaleAnimatable
    val glowAlpha by glowAlphaAnimatable

    // 连拍定时器
    LaunchedEffect(isBurstMode) {
        if (isBurstMode) {
            while (true) {
                onBurstCapture()
                delay(150L)
            }
        }
    }

    Box(
        modifier = modifier
            .size(DesignSystem.Dimensions.shutterButtonOuter)
            .scale(containerScale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val center = Offset(canvasWidth / 2f, canvasWidth / 2f)
            val outerRadius = (canvasWidth / 2f) - 2.dp.toPx()
            val innerBaseRadius = (DesignSystem.Dimensions.shutterButtonInner.toPx() / 2f)
            val innerRadius = innerBaseRadius * innerScale

            // 1. 金色光环（对齐成功）
            if (isAligned) {
                val glowOuterRadius = outerRadius * glowScale
                drawCircle(
                    color = DesignSystem.Colors.goldenGlow.copy(alpha = glowAlpha),
                    radius = glowOuterRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 2. 连拍旋转弧
            if (isBurstMode && isPressed) {
                val burstRadius = outerRadius + 1.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.6f),
                    startAngle = burstRotation,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - burstRadius, center.y - burstRadius),
                    size = Size(burstRadius * 2, burstRadius * 2),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 3. 外圈描边（35%透明度白色）
            drawCircle(
                color = DesignSystem.Colors.shutterOuterRing,
                radius = outerRadius,
                center = center,
                style = Stroke(width = DesignSystem.Dimensions.shutterButtonRingWidth.toPx())
            )

            // 4. 涟漪
            ripples.forEach { ripple ->
                val progress = ripple.progress.value
                val rippleAlpha = (1f - progress) * 0.4f
                val rippleRadius = outerRadius * progress
                drawCircle(
                    color = Color.White.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 5. 内层形状
            if (cornerMorphProgress > 0.01f) {
                val morphT = cornerMorphProgress
                val rectFraction = 0.7f * morphT + 1.0f * (1f - morphT)
                val actualSize = innerRadius * 2f * rectFraction
                val cornerRadius = morphT * (innerRadius * 0.25f)
                drawRoundRect(
                    color = innerColor,
                    topLeft = Offset(center.x - actualSize / 2f, center.y - actualSize / 2f),
                    size = Size(actualSize, actualSize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            } else {
                drawCircle(
                    color = innerColor,
                    radius = innerRadius,
                    center = center
                )
            }
        }

        // 手势检测
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        HapticManager.light()

                        val longPressJob = coroutineScope.launch {
                            delay(500L)
                            if (!isBurstMode) {
                                isLongPressing = true
                                onLongPressStart()
                            }
                        }
                        val burstJob = coroutineScope.launch {
                            delay(500L)
                            if (!isLongPressing) {
                                isBurstMode = true
                            }
                        }

                        waitForUpOrCancellation()
                        longPressJob.cancel()
                        burstJob.cancel()

                        val wasLongPressing = isLongPressing
                        val wasBurstMode = isBurstMode

                        if (wasLongPressing) {
                            isLongPressing = false
                            HapticManager.success()
                            onLongPressEnd()
                        } else if (wasBurstMode) {
                            isBurstMode = false
                        } else {
                            onCapture()
                            HapticManager.success()
                            coroutineScope.launch {
                                val ripple = RippleAnim(Animatable(0f))
                                ripples.add(ripple)
                                ripple.progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 600, easing = EaseOut)
                                )
                                ripples.remove(ripple)
                            }
                        }
                    }
                }
        )
    }
}

private class RippleAnim(
    val progress: Animatable<Float, AnimationVector1D>
)
