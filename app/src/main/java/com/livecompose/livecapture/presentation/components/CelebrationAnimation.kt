package com.livecompose.livecapture.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.core.design.*
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * 彩纸粒子动画
 * 显示从顶部飘落的彩色粒子效果
 */
@Composable
fun ConfettiAnimation(
    modifier: Modifier = Modifier,
    trigger: Boolean = false,
    onAnimationEnd: () -> Unit = {}
) {
    // 粒子数量
    val particleCount = 50

    // 粒子颜色
    val colors = listOf(
        Color(0xFFFF6B6B),  // 红色
        Color(0xFFFFD93D),  // 金色
        Color(0xFF6BCB77),  // 绿色
        Color(0xFF4D96FF),  // 蓝色
        Color(0xFF9B59B6)   // 紫色
    )

    // 粒子状态
    data class Particle(
        val x: Float,
        val y: Float,
        val color: Color,
        val speed: Float,
        val drift: Float,
        val size: Float
    )

    var isPlaying by remember { mutableStateOf(false) }
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var animationProgress by remember { mutableFloatStateOf(0f) }

    // 触发动画
    LaunchedEffect(trigger) {
        if (trigger && !isPlaying) {
            isPlaying = true
            animationProgress = 0f

            // 生成随机粒子
            particles = List(particleCount) {
                Particle(
                    x = Random.nextFloat(),
                    y = -Random.nextFloat() * 0.5f,
                    color = colors.random(),
                    speed = 0.5f + Random.nextFloat() * 1.5f,
                    drift = Random.nextFloat() * 2f - 1f,
                    size = 4f + Random.nextFloat() * 6f
                )
            }

            // 动画循环
            val startTime = System.currentTimeMillis()
            val duration = 2000L

            while (System.currentTimeMillis() - startTime < duration) {
                animationProgress = (System.currentTimeMillis() - startTime).toFloat() / duration
                delay(16) // ~60fps
            }

            isPlaying = false
            onAnimationEnd()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (isPlaying) {
            particles.forEach { particle ->
                val x = particle.x * size.width + sin(animationProgress * 10 + particle.drift) * 50f
                val y = (particle.y + animationProgress * particle.speed) * size.height

                if (y >= 0 && y <= size.height) {
                    // 计算淡出
                    val alpha = if (animationProgress > 0.8f) {
                        1f - (animationProgress - 0.8f) / 0.2f
                    } else 1f

                    drawCircle(
                        color = particle.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                        radius = particle.size,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

/**
 * 涟漪效果动画
 * 从中心向外扩散的圆形涟漪
 */
@Composable
fun RippleEffect(
    modifier: Modifier = Modifier,
    trigger: Boolean = false,
    onAnimationEnd: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(false) }
    var animationProgress by remember { mutableFloatStateOf(0f) }
    val rippleColor = Color(0xFFFFD700) // 金色

    LaunchedEffect(trigger) {
        if (trigger && !isPlaying) {
            isPlaying = true
            animationProgress = 0f

            val startTime = System.currentTimeMillis()
            val duration = 1500L

            while (System.currentTimeMillis() - startTime < duration) {
                animationProgress = (System.currentTimeMillis() - startTime).toFloat() / duration
                delay(16)
            }

            isPlaying = false
            onAnimationEnd()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (isPlaying) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = size.minDimension

            // 绘制3个涟漪
            repeat(3) { index ->
                val delay = index * 0.15f
                val progress = ((animationProgress - delay) / (1f - delay)).coerceIn(0f, 1f)

                if (progress > 0 && progress < 1f) {
                    val radius = progress * maxRadius * 0.5f
                    val alpha = (1f - progress) * 0.6f

                    drawCircle(
                        color = rippleColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}

/**
 * 组合庆祝动画
 * 同时显示彩纸粒子效果和涟漪效果
 */
@Composable
fun CombinedCelebration(
    modifier: Modifier = Modifier,
    trigger: Boolean = false,
    showConfetti: Boolean = true,
    showRipple: Boolean = true,
    onAnimationEnd: () -> Unit = {}
) {
    var confettiEnded by remember { mutableStateOf(false) }
    var rippleEnded by remember { mutableStateOf(false) }

    LaunchedEffect(confettiEnded, rippleEnded) {
        if ((confettiEnded || !showConfetti) && (rippleEnded || !showRipple)) {
            onAnimationEnd()
        }
    }

    Box(modifier = modifier) {
        if (showConfetti) {
            ConfettiAnimation(
                modifier = Modifier.matchParentSize(),
                trigger = trigger,
                onAnimationEnd = { confettiEnded = true }
            )
        }

        if (showRipple) {
            RippleEffect(
                modifier = Modifier.matchParentSize(),
                trigger = trigger,
                onAnimationEnd = { rippleEnded = true }
            )
        }
    }
}
