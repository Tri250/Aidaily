package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 倒计时覆盖层
 * 在自动拍照前显示倒计时数字动画
 *
 * @param seconds 倒计时秒数
 * @param isActive 是否激活倒计时
 * @param onComplete 倒计时完成回调
 * @param onCancel 用户取消倒计时回调
 * @param modifier 修饰符
 */
@Composable
fun CountdownOverlay(
    seconds: Double,
    isActive: Boolean,
    onComplete: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember(seconds) { mutableIntStateOf(seconds.toInt()) }
    var currentProgress by remember { mutableFloatStateOf(1f) }
    var isVisible by remember(isActive) { mutableStateOf(isActive) }

    // 数字动画
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.2f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "countScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "countAlpha"
    )

    LaunchedEffect(isActive) {
        if (!isActive) {
            isVisible = false
            return@LaunchedEffect
        }

        isVisible = true
        remainingSeconds = seconds.toInt()
        currentProgress = 1f

        while (remainingSeconds > 0 && isVisible) {
            // 进度条动画
            currentProgress = 1f
            animate(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = tween(1000, easing = LinearEasing)
            ) { value, _ ->
                currentProgress = value
            }

            remainingSeconds--
            delay(50)
        }

        if (isVisible) {
            onComplete()
            isVisible = false
        }
    }

    if (!isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                this.alpha = animatedAlpha
            }
        ) {
            // 圆形进度环
            Canvas(modifier = Modifier.size(140.dp)) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                // 背景环
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = radius - 4.dp.toPx(),
                    center = center,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // 进度弧
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * currentProgress,
                    useCenter = false,
                    topLeft = center - androidx.compose.ui.geometry.Size(radius * 2, radius * 2) / 2f,
                    size = androidx.compose.ui.geometry.Size(radius * 2 - 8.dp.toPx(), radius * 2 - 8.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Spacer(Modifier.height(12.dp))

            // 倒计时数字
            androidx.compose.material3.Text(
                text = if (remainingSeconds > 0) "$remainingSeconds" else "",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-2).sp
            )
        }

        // 取消按钮（如果提供）
        if (onCancel != null) {
            androidx.compose.material3.TextButton(
                onClick = {
                    isVisible = false
                    onCancel.invoke()
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "取消",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }
        }
    }
}
