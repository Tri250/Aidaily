package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/**
 * 对焦指示器
 * 手动点击对焦 + AE/AF 锁定指示
 *
 * @param onFocus 触摸对焦回调 (x: 0~1, y: 0~1)
 * @param modifier 修饰符
 */
@Composable
fun FocusIndicator(
    onFocus: (normalizedX: Float, normalizedY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // 对焦状态
    data class FocusState(
        val position: Offset = Offset.Zero,
        val isActive: Boolean = false,
        val isLocked: Boolean = false
    )

    var focusState by remember { mutableStateOf(FocusState()) }
    val coroutineScope = rememberCoroutineScope()
    val animatedScale by animateFloatAsState(
        targetValue = if (focusState.isActive) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "focusScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (focusState.isActive) 1f else 0f,
        animationSpec = tween(300),
        label = "focusAlpha"
    )

    // 自动隐藏
    LaunchedEffect(focusState.isActive) {
        if (focusState.isActive) {
            delay(2500)
            focusState = focusState.copy(isActive = false)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normalizedX = offset.x / size.width
                    val normalizedY = offset.y / size.height
                    focusState = FocusState(
                        position = Offset(offset.x, offset.y),
                        isActive = true,
                        isLocked = false
                    )
                    // 延迟后变"锁定"
                    coroutineScope.launch {
                        delay(800)
                        focusState = focusState.copy(isLocked = true)
                    }
                    onFocus(normalizedX, normalizedY)
                }
            }
    ) {
        if (!focusState.isActive || animatedAlpha < 0.01f) return@Canvas

        val pos = focusState.position
        val indicatorSize = 60f

        // 外圈
        val color = if (focusState.isLocked) Color(0xFF00C853) else Color(0xFFFFAB00)
        drawRoundRect(
            color = color,
            topLeft = Offset(pos.x - indicatorSize / 2, pos.y - indicatorSize / 2),
            size = Size(indicatorSize, indicatorSize),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f * animatedScale),
            alpha = animatedAlpha
        )

        // 中心十字线
        val crossLen = 8f
        drawLine(
            color.copy(alpha = 0.6f * animatedAlpha),
            Offset(pos.x - crossLen, pos.y),
            Offset(pos.x + crossLen, pos.y),
            strokeWidth = 1f
        )
        drawLine(
            color.copy(alpha = 0.6f * animatedAlpha),
            Offset(pos.x, pos.y - crossLen),
            Offset(pos.x, pos.y + crossLen),
            strokeWidth = 1f
        )

        // 锁定标识（绿色小锁图标简化版）
        if (focusState.isLocked) {
            drawCircle(
                color = Color(0xFF00C853).copy(alpha = 0.4f * animatedAlpha),
                radius = indicatorSize / 2 + 4f,
                center = pos,
                style = Stroke(width = 1.5f)
            )
        }
    }
}
