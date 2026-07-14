package com.livecompose.livecapture.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.design.FontSize
import kotlinx.coroutines.delay

/**
 * 全屏倒计时覆盖层
 * 显示大数字倒计时动画，用于自动拍摄延迟
 *
 * @param countdownSeconds 倒计时秒数
 * @param isRunning 是否正在倒计时
 * @param onCountdownEnd 倒计时结束回调
 */
@Composable
fun CountdownOverlay(
    countdownSeconds: Int = 3,
    isRunning: Boolean = false,
    onCountdownEnd: () -> Unit = {}
) {
    var currentCount by remember { mutableIntStateOf(countdownSeconds) }
    var isCountingDown by remember { mutableStateOf(false) }

    // 倒计时动画
    LaunchedEffect(isRunning) {
        if (isRunning && !isCountingDown) {
            isCountingDown = true
            currentCount = countdownSeconds

            while (currentCount > 0) {
                delay(1000)
                currentCount--
            }

            isCountingDown = false
            onCountdownEnd()
        }
    }

    // 数字动画状态
    var previousCount by remember { mutableIntStateOf(countdownSeconds) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isCountingDown && currentCount > 0) 1f else 0f,
        animationSpec = tween(200),
        label = "countdown_alpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isCountingDown && currentCount > 0 && previousCount != currentCount) {
            previousCount = currentCount
            1.2f
        } else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "countdown_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(animatedAlpha),
        contentAlignment = Alignment.Center
    ) {
        if (isCountingDown && currentCount > 0) {
            Text(
                text = currentCount.toString(),
                fontSize = FontSize.Hero,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .scale(animatedScale)
            )
        }
    }
}

/**
 * 简化的倒计时组件
 * 仅显示数字，不包含完整覆盖层
 *
 * @param currentCount 当前倒计时数字
 * @param modifier 修饰符
 */
@Composable
fun CountdownNumber(
    currentCount: Int,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = 1.15f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "countdown_number_scale"
    )

    Text(
        text = currentCount.toString(),
        fontSize = FontSize.Hero,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier.scale(animatedScale)
    )
}