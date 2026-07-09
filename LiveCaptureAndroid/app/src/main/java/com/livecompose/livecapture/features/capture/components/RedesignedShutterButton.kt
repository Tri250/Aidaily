package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.utilities.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 重设计快门按钮 - 国潮质感风格
 *
 * 视觉层级：
 * - 外层：2dp 白色描边圆环（按压时变粗 + 向外扩散涟漪）
 * - 内层：实心白色圆（按压时缩小至 85%）
 * - 长按视频：内层变为红色圆角方块
 * - 对齐成功时：外层出现金色光环脉动
 * - 连拍模式：外环进度条旋转
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

    // 按压状态
    var isPressed by remember { mutableStateOf(false) }

    // 长按视频录制状态
    var isLongPressing by remember { mutableStateOf(false) }

    // 连拍模式状态
    var isBurstMode by remember { mutableStateOf(false) }

    // 涟漪动画列表
    val ripples = remember { mutableStateListOf<RippleAnim>() }

    // 连拍旋转进度角度
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

    // 内圈缩放动画（按压时缩小至 85%，录制时更小）
    val innerScale by animateFloatAsState(
        targetValue = when {
            isLongPressing && isRecording -> 0.7f
            isPressed -> 0.85f
            else -> 1f
        },
        animationSpec = DesignSystem.Animation.shutterPress,
        label = "innerScale"
    )

    // 外圈描边宽度动画（2dp → 4dp）
    val outerStrokeWidth by animateFloatAsState(
        targetValue = if (isPressed) 4f else 2f,
        animationSpec = DesignSystem.Animation.shutterPress,
        label = "outerStrokeWidth"
    )

    // 内圈形状变形动画（0f = 圆形, 1f = 圆角方块）
    val cornerMorphProgress by animateFloatAsState(
        targetValue = if (isLongPressing && isRecording) 1f else 0f,
        animationSpec = DesignSystem.Animation.shutterLongPress,
        label = "cornerMorph"
    )

    // 内圈颜色动画（白色 → 录制红）
    val innerColor by animateColorAsState(
        targetValue = if (isLongPressing && isRecording) DesignSystem.Colors.recordingRed
        else DesignSystem.Colors.shutterInner,
        animationSpec = tween(durationMillis = 300),
        label = "innerColor"
    )

    // 外圈颜色动画（白色 → 录制红 0.6 alpha）
    val outerColor by animateColorAsState(
        targetValue = if (isLongPressing && isRecording) DesignSystem.Colors.recordingRed.copy(alpha = 0.6f)
        else Color.White,
        animationSpec = tween(durationMillis = 300),
        label = "outerColor"
    )

    // 金色光环脉动（仅 isAligned = true 时可见）
    // 使用 Animatable + LaunchedEffect 实现基于 spring 的无限脉动
    val glowScaleAnimatable = remember { Animatable(1.0f) }
    val glowAlphaAnimatable = remember { Animatable(0.8f) }

    LaunchedEffect(isAligned) {
        if (isAligned) {
            coroutineScope.launch {
                while (true) {
                    glowScaleAnimatable.animateTo(
                        1.15f,
                        animationSpec = DesignSystem.Animation.shutterGlowPulse
                    )
                    glowScaleAnimatable.animateTo(
                        1.0f,
                        animationSpec = DesignSystem.Animation.shutterGlowPulse
                    )
                }
            }
            coroutineScope.launch {
                while (true) {
                    glowAlphaAnimatable.animateTo(
                        0.3f,
                        animationSpec = DesignSystem.Animation.shutterGlowPulse
                    )
                    glowAlphaAnimatable.animateTo(
                        0.8f,
                        animationSpec = DesignSystem.Animation.shutterGlowPulse
                    )
                }
            }
        } else {
            glowScaleAnimatable.snapTo(1.0f)
            glowAlphaAnimatable.snapTo(0.8f)
        }
    }
    val glowScale by glowScaleAnimatable
    val glowAlpha by glowAlphaAnimatable

    // 连拍定时器：每 150ms 调用 onBurstCapture
    LaunchedEffect(isBurstMode) {
        if (isBurstMode) {
            while (true) {
                onBurstCapture()
                delay(150L)
            }
        }
    }

    Box(
        modifier = modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val center = Offset(canvasWidth / 2f, canvasWidth / 2f)

            val outerRadius = (canvasWidth / 2f) - (outerStrokeWidth / 2f)
            val innerBaseRadius = (62f / 76f) * (canvasWidth / 2f)
            val innerRadius = innerBaseRadius * innerScale

            // 1. 金色光环脉动（仅对齐成功时）
            if (isAligned) {
                val glowOuterRadius = outerRadius * glowScale
                drawCircle(
                    color = DesignSystem.Colors.goldenGlow.copy(alpha = glowAlpha),
                    radius = glowOuterRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 2. 连拍模式旋转进度弧
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

            // 3. 外圈描边圆环
            drawCircle(
                color = outerColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = outerStrokeWidth)
            )

            // 4. 涟漪效果
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
                // 圆角方块（视频录制模式变形）
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
                // 纯圆形（普通拍照模式）
                drawCircle(
                    color = innerColor,
                    radius = innerRadius,
                    center = center
                )
            }
        }

        // 手势检测层 - 统一使用 onPress 处理所有手势
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        HapticManager.light()

                        // 启动长按检测（500ms 阈值）
                        val longPressJob = coroutineScope.launch {
                            delay(500L)
                            // 500ms 后仍按着 → 开始录制视频
                            if (isPressed && !isBurstMode) {
                                isLongPressing = true
                                onLongPressStart()
                            }
                        }

                        // 同时启动连拍延迟检测（仅当未进入录制模式时）
                        val burstJob = coroutineScope.launch {
                            delay(500L)
                            // 500ms 后仍按着且未进入录制模式 → 连拍
                            if (isPressed && !isLongPressing) {
                                isBurstMode = true
                            }
                        }

                        // 等待抬起或取消
                        waitForUpOrCancellation()

                        // 取消长按/连拍检测任务
                        longPressJob.cancel()
                        burstJob.cancel()

                        val wasLongPressing = isLongPressing
                        val wasBurstMode = isBurstMode

                        // 重置按压状态
                        isPressed = false

                        if (wasLongPressing) {
                            // 结束录制
                            isLongPressing = false
                            HapticManager.success()
                            onLongPressEnd()
                        } else if (wasBurstMode) {
                            // 结束连拍
                            isBurstMode = false
                        } else {
                            // 短按 → 单次拍照
                            onCapture()
                            HapticManager.success()

                            // 触发涟漪动画
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

/**
 * 涟漪动画状态
 */
private class RippleAnim(
    val progress: Animatable<Float, AnimationVector1D>
)
