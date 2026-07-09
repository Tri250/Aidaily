package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlin.math.abs
import kotlin.math.sin

/**
 * 重设计水平仪 - 实时倾斜角度指示器
 *
 * 特性：
 * - 中心水平线随设备倾斜实时偏移
 * - 水平时变为绿色 + 弹性归位
 * - 不水平时显示黄色
 * - 两侧刻度标记
 */
@Composable
fun RedesignedLevelIndicator(
    tiltX: Float = 0f,  // -1 to 1, left to right tilt
    tiltY: Float = 0f,  // -1 to 1, forward to backward tilt
    modifier: Modifier = Modifier
) {
    val isLevel = abs(tiltX) < 0.05f

    val lineColor by animateColorAsState(
        targetValue = if (isLevel) DesignSystem.Colors.success else DesignSystem.Colors.warning,
        animationSpec = DesignSystem.Animation.stateActive,
        label = "levelLineColor"
    )

    Box(
        modifier = modifier
            .size(width = 120.dp, height = 32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DesignSystem.Colors.minimalDarkOverlay),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Horizontal line that shifts based on tiltX
            val displacement = tiltX.coerceIn(-1f, 1f) * 40.dp.toPx()
            val lineStartX = centerX + displacement - 30.dp.toPx()
            val lineEndX = centerX + displacement + 30.dp.toPx()

            drawLine(
                color = lineColor,
                start = Offset(lineStartX, centerY),
                end = Offset(lineEndX, centerY),
                strokeWidth = 2.dp.toPx()
            )

            // Left tick mark at -30dp from center
            val leftTickX = centerX - 30.dp.toPx()
            drawLine(
                color = DesignSystem.Colors.minimalSecondaryLabel,
                start = Offset(leftTickX, centerY - 4.dp.toPx()),
                end = Offset(leftTickX, centerY + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Right tick mark at +30dp from center
            val rightTickX = centerX + 30.dp.toPx()
            drawLine(
                color = DesignSystem.Colors.minimalSecondaryLabel,
                start = Offset(rightTickX, centerY - 4.dp.toPx()),
                end = Offset(rightTickX, centerY + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * 重设计直方图浮层
 *
 * 特性：
 * - 毛玻璃背景 + 圆角
 * - 三通道RGB波形（红/绿/蓝）
 * - 实时更新动画
 */
@Composable
fun RedesignedHistogramOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "histogram")

    val amplitude by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "histogramAmplitude"
    )

    Box(
        modifier = modifier
            .size(width = 100.dp, height = 60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DesignSystem.Colors.minimalDarkOverlay),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val padding = 4.dp.toPx()

            // Red channel
            val redPath = Path().apply {
                moveTo(padding, height * 0.7f)
                for (x in 0..(width - 2 * padding).toInt()) {
                    val fraction = x / (width - 2 * padding)
                    val y = height * 0.7f - (sin(fraction * Math.PI * 2.5) * height * 0.25f * amplitude).toFloat() *
                            (1f - fraction * 0.3f)
                    lineTo(padding + x, y)
                }
            }
            drawPath(
                path = redPath,
                color = Color.Red.copy(alpha = 0.6f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Green channel
            val greenPath = Path().apply {
                moveTo(padding, height * 0.65f)
                for (x in 0..(width - 2 * padding).toInt()) {
                    val fraction = x / (width - 2 * padding)
                    val y = height * 0.65f - (sin(fraction * Math.PI * 3.0 + 0.5) * height * 0.22f * amplitude).toFloat() *
                            (1f - fraction * 0.2f)
                    lineTo(padding + x, y)
                }
            }
            drawPath(
                path = greenPath,
                color = Color.Green.copy(alpha = 0.5f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Blue channel
            val bluePath = Path().apply {
                moveTo(padding, height * 0.6f)
                for (x in 0..(width - 2 * padding).toInt()) {
                    val fraction = x / (width - 2 * padding)
                    val y = height * 0.6f - (sin(fraction * Math.PI * 2.0 + 1.0) * height * 0.2f * amplitude).toFloat() *
                            (1f - fraction * 0.4f)
                    lineTo(padding + x, y)
                }
            }
            drawPath(
                path = bluePath,
                color = Color.Blue.copy(alpha = 0.5f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Text(
            text = "直方图",
            fontSize = 9.sp,
            color = DesignSystem.Colors.minimalSecondaryLabel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        )
    }
}

/**
 * 重设计斑马纹浮层
 *
 * 特性：
 * - 对角线条纹动画（持续移动）
 * - 半透明覆盖
 */
@Composable
fun RedesignedZebraOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "zebra")

    val stripeOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 24.dp.toPx(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "zebraOffset"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val spacing = 12.dp.toPx()
            val diagonal = kotlin.math.sqrt(
                canvasWidth * canvasWidth + canvasHeight * canvasHeight
            )

            // Draw diagonal stripes at 45 degrees
            var currentOffset = -diagonal + stripeOffset
            while (currentOffset < diagonal * 2) {
                val startX = currentOffset
                val startY = 0f
                val endX = currentOffset - canvasHeight
                val endY = canvasHeight

                drawLine(
                    color = DesignSystem.Colors.warning.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx()
                )

                currentOffset += spacing
            }
        }

        Text(
            text = "过曝",
            fontSize = 11.sp,
            color = DesignSystem.Colors.warning,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(
                    DesignSystem.Colors.minimalDarkOverlay,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * 夜景模式指示器
 *
 * 特性：
 * - 月亮图标 + "夜景"文字
 * - 呼吸脉动效果
 * - 冷蓝色调
 */
@Composable
fun NightModeIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nightMode")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nightPulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nightPulseScale"
    )

    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 800f
            ),
            initialScale = 0.8f
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    DesignSystem.Colors.minimalDarkOverlay,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "\uD83C\uDF19",
                fontSize = 14.sp,
                color = DesignSystem.Colors.nightModeBlue.copy(alpha = pulseAlpha),
                modifier = Modifier
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "夜景",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DesignSystem.Colors.nightModeBlue
            )
        }
    }
}
