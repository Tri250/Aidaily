package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.utilities.HapticManager
import kotlinx.coroutines.delay

/**
 * 重设计对焦指示器 - 国潮质感风格
 *
 * 动画序列：
 * 1. 出现时从 1.5x 弹性收缩到 1.0x（弹性弹簧，模拟对焦"锁定"的物理感）
 * 2. 对焦中：黄色方框 + 中心十字
 * 3. 对焦成功：方框变绿色 + 呼吸光晕
 * 4. AE/AF 锁定：黄色锁定框 + 持续呼吸光晕
 * 5. 3秒后自动淡出
 */
@Composable
fun RedesignedFocusIndicator(
    x: Float,
    y: Float,
    isFocused: Boolean = false,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    // --- Appear scale animation: 1.5f → 1.0f with elastic spring ---
    val appearScale = remember { Animatable(1.5f) }
    LaunchedEffect(x, y) {
        appearScale.snapTo(1.5f)
        appearScale.animateTo(
            targetValue = 1.0f,
            animationSpec = DesignSystem.Animation.feedbackFocus
        )
    }

    // --- Fade-out alpha (1.0 → 0.0 over 300ms after 3s) ---
    val fadeAlpha = remember { Animatable(1f) }

    LaunchedEffect(x, y) {
        fadeAlpha.snapTo(1f)
        delay(3000L)
        fadeAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }

    // --- Breathing glow pulse (for focused or locked state) ---
    val showBreathingGlow = isFocused || isLocked
    val infiniteTransition = rememberInfiniteTransition(label = "focusBreath")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )

    // --- Haptic feedback on state changes ---
    var prevFocused by remember { mutableStateOf(false) }
    var prevLocked by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused && !prevFocused) {
            HapticManager.success()
        }
        prevFocused = isFocused
    }

    LaunchedEffect(isLocked) {
        if (isLocked && !prevLocked) {
            HapticManager.focusLock()
        }
        prevLocked = isLocked
    }

    // --- Derive colors from state ---
    val focusColor: Color = when {
        isLocked -> DesignSystem.Colors.warning
        isFocused -> DesignSystem.Colors.success
        else -> DesignSystem.Colors.warning
    }

    val focusStrokeWidth: Float = when {
        isLocked -> 2.5f
        else -> 2f
    }

    // --- Pixel values (density-independent via dp → px) ---
    val density = LocalDensity.current
    val boxSizePx = with(density) { 64.dp.toPx() }
    val cornerRadiusPx = with(density) { 4.dp.toPx() }
    val crossLengthPx = with(density) { 8.dp.toPx() }
    val glowPaddingPx = with(density) { 4.dp.toPx() }
    val glowStrokeWidthPx = with(density) { 1.5f.dp.toPx() }
    val crossStrokeWidthPx = with(density) { 1.dp.toPx() }
    val lockCircleRadiusPx = with(density) { 3.dp.toPx() }

    val currentAlpha = fadeAlpha.value
    val currentScale = appearScale.value

    Canvas(modifier = modifier.fillMaxSize()) {
        if (currentAlpha < 0.01f) return@Canvas

        val center = Offset(x, y)
        val scaledBoxSize = boxSizePx * currentScale
        val halfBox = scaledBoxSize / 2f

        // --- Breathing glow ring (behind the focus box) ---
        if (showBreathingGlow) {
            val glowSize = scaledBoxSize + glowPaddingPx * 2f
            val glowHalf = glowSize / 2f
            drawRoundRect(
                color = focusColor,
                topLeft = Offset(center.x - glowHalf, center.y - glowHalf),
                size = Size(glowSize, glowSize),
                cornerRadius = CornerRadius(cornerRadiusPx + glowPaddingPx, cornerRadiusPx + glowPaddingPx),
                style = Stroke(width = glowStrokeWidthPx),
                alpha = breathingAlpha * currentAlpha
            )
        }

        // --- Focus box ---
        drawRoundRect(
            color = focusColor,
            topLeft = Offset(center.x - halfBox, center.y - halfBox),
            size = Size(scaledBoxSize, scaledBoxSize),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = focusStrokeWidth),
            alpha = currentAlpha
        )

        // --- Crosshair lines (4 segments: top, bottom, left, right of center) ---
        val crossAlpha = 0.6f * currentAlpha
        val crossColor = focusColor.copy(alpha = crossAlpha)

        // Top
        drawLine(
            color = crossColor,
            start = Offset(center.x, center.y - crossLengthPx),
            end = Offset(center.x, center.y - 1f),
            strokeWidth = crossStrokeWidthPx
        )
        // Bottom
        drawLine(
            color = crossColor,
            start = Offset(center.x, center.y + 1f),
            end = Offset(center.x, center.y + crossLengthPx),
            strokeWidth = crossStrokeWidthPx
        )
        // Left
        drawLine(
            color = crossColor,
            start = Offset(center.x - crossLengthPx, center.y),
            end = Offset(center.x - 1f, center.y),
            strokeWidth = crossStrokeWidthPx
        )
        // Right
        drawLine(
            color = crossColor,
            start = Offset(center.x + 1f, center.y),
            end = Offset(center.x + crossLengthPx, center.y),
            strokeWidth = crossStrokeWidthPx
        )

        // --- Lock indicator (small filled circle at top-right corner of focus box) ---
        if (isLocked) {
            val lockCenter = Offset(
                center.x + halfBox,
                center.y - halfBox
            )
            drawCircle(
                color = DesignSystem.Colors.warning,
                radius = lockCircleRadiusPx,
                center = lockCenter,
                alpha = currentAlpha
            )
        }
    }
}
