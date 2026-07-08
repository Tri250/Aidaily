package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 拍照动画快照数据
 */
data class CaptureAnimationSnapshot(
    val bitmap: ImageBitmap,
    val sourceBounds: Rect,
    val targetBounds: Rect,
    val id: Long = System.nanoTime()
)

/** 动画阶段 */
private enum class CaptureAnimationPhase {
    EXPAND,   // 展开：从拍摄位置放大到全屏
    DEVELOP,  // 显影：模拟胶片显影效果（从暗到亮）
    COLLAPSE  // 收缩：缩小回图库位置
}

/** 动画总时长配置 */
private object CaptureAnimationConfig {
    const val EXPAND_DURATION_MS = 350L
    const val DEVELOP_DURATION_MS = 500L
    const val COLLAPSE_DURATION_MS = 400L
    const val FLASH_DURATION_MS = 100L
}

/**
 * 三阶段拍照动画覆盖层
 * 模拟胶片相机体验：展开 → 显影 → 收缩
 *
 * @param snapshot 拍照快照数据
 * @param onAnimationComplete 动画完成回调
 * @param modifier 修饰符
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun CaptureAnimationOverlay(
    snapshot: CaptureAnimationSnapshot?,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (snapshot == null) {
        onAnimationComplete()
        return
    }

    val density = LocalDensity.current

    var currentPhase by remember { mutableStateOf<CaptureAnimationPhase?>(null) }
    val scaleAnim = remember { Animatable(1f) }
    val alphaAnim = remember { Animatable(1f) }
    val brightnessAnim = remember { Animatable(-1f) } // -1 = 全黑, 0 = 正常, > 0 = 过曝

    // 白色闪光层
    var flashOpacity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(snapshot.id) {
        coroutineScope {
            try {
                // === 阶段 1: EXPAND ===
                currentPhase = CaptureAnimationPhase.EXPAND
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1.15f,
                        animationSpec = tween(
                            durationMillis = CaptureAnimationConfig.EXPAND_DURATION_MS.toInt(),
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                // 同步执行白色闪光
                async {
                    flashOpacity = 1f
                    kotlinx.coroutines.delay(CaptureAnimationConfig.FLASH_DURATION_MS)
                    flashOpacity = 0f
                }.await()

                kotlinx.coroutines.delay(CaptureAnimationConfig.EXPAND_DURATION_MS)

                // === 阶段 2: DEVELOP（显影效果）===
                currentPhase = CaptureAnimationPhase.DEVELOP
                brightnessAnim.snapTo(-1f)

                launch {
                    brightnessAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = CaptureAnimationConfig.DEVELOP_DURATION_MS.toInt(),
                            easing = LinearEasing
                        )
                    )
                }

                // 轻微的 alpha 闪烁模拟显影过程
                launch {
                    alphaAnim.snapTo(0.85f)
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = CaptureAnimationConfig.DEVELOP_DURATION_MS.toInt(),
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                kotlinx.coroutines.delay(CaptureAnimationConfig.DEVELOP_DURATION_MS)

                // === 阶段 3: COLLAPSE ===
                currentPhase = CaptureAnimationPhase.COLLAPSE
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = CaptureAnimationConfig.COLLAPSE_DURATION_MS.toInt(),
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                launch {
                    alphaAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = CaptureAnimationConfig.COLLAPSE_DURATION_MS.toInt(),
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                kotlinx.coroutines.delay(CaptureAnimationConfig.COLLAPSE_DURATION_MS)

                // 重置状态
                currentPhase = null
                scaleAnim.snapTo(1f)
                alphaAnim.snapTo(1f)
                brightnessAnim.snapTo(0f)
                flashOpacity = 0f

                onAnimationComplete()

            } catch (_: CancellationException) {
                // 取消时重置
                currentPhase = null
                scaleAnim.snapTo(1f)
                alphaAnim.snapTo(1f)
                brightnessAnim.snapTo(0f)
                flashOpacity = 0f
                onAnimationComplete()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { /* 点击穿透 */ } },
        contentAlignment = Alignment.Center
    ) {
        // 闪光层
        if (flashOpacity > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(flashOpacity)
                    .background(Color.White)
            )
        }

        // 照片动画层
        Image(
            bitmap = snapshot.bitmap,
            contentDescription = "捕获照片",
            contentScale = ContentScale.Fit,
            colorFilter = if (brightnessAnim.value != 0f) {
                val brightness = brightnessAnim.value
                ColorFilter.colorMatrix(ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, brightness,
                        0f, 1f, 0f, 0f, brightness,
                        0f, 0f, 1f, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )
                ))
            } else null,
            modifier = Modifier
                .fillMaxSize(0.85f)
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    this.alpha = alphaAnim.value
                }
                .clip(RoundedCornerShape(8.dp))
        )
    }
}
