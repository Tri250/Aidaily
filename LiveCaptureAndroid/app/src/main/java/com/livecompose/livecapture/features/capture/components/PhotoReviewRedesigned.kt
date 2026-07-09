package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 重设计照片预览覆盖层 - 国潮质感风格
 *
 * 体验流程：
 * 1. 照片以"显影"动画出现（从暗到亮，模拟胶片显影过程）
 * 2. 底部显示操作按钮（删除/编辑/分享/保存）
 * 3. 上滑保存并关闭
 * 4. 下滑删除并关闭
 * 5. 2秒后自动保存（如果用户无操作）
 */
@Composable
fun PhotoReviewRedesigned(
    data: ByteArray?,
    onAccept: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (data == null) return

    val bitmap = remember(data) {
        android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
    }

    if (bitmap == null) return

    val scope = rememberCoroutineScope()
    var dismissed by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density

    // === Entry animation: overlay fade-in ===
    val entryAlpha = remember { Animatable(0f) }
    // === Entry animation: bottom bar slide-up (100dp in pixels) ===
    val bottomBarOffset = remember { Animatable(100f * density) }
    // === Film develop animation: brightness from -1 (dark) to 0 (normal) ===
    val brightnessAnim = remember { Animatable(-1f) }
    // === Film develop animation: alpha from 0.7 to 1.0 ===
    val developAlpha = remember { Animatable(0.7f) }
    // === Auto-save countdown progress ===
    val countdownProgress = remember { Animatable(0f) }
    // === Swipe gesture offset and alpha ===
    val swipeOffsetY = remember { Animatable(0f) }
    val swipeAlpha = remember { Animatable(1f) }
    // === User interaction tracker ===
    var userInteracted by remember { mutableStateOf(false) }

    // Launch entry + develop animations
    LaunchedEffect(Unit) {
        // Entry: overlay fade-in (250ms)
        launch {
            entryAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            )
        }
        // Entry: bottom bar slide up
        launch {
            bottomBarOffset.animateTo(
                targetValue = 0f,
                animationSpec = DesignSystem.Animation.entrySlideUp
            )
        }
        // Film develop: brightness -1 → 0 over 500ms
        launch {
            brightnessAnim.animateTo(
                targetValue = 0f,
                animationSpec = DesignSystem.Animation.narrativeDevelop
            )
        }
        // Film develop: alpha 0.7 → 1.0 over 500ms
        launch {
            developAlpha.animateTo(
                targetValue = 1f,
                animationSpec = DesignSystem.Animation.narrativeDevelop
            )
        }
    }

    // Auto-save timer
    LaunchedEffect(Unit) {
        // Countdown progress line fills over 2s
        launch {
            countdownProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = DesignSystem.Animation.PHOTO_PREVIEW_DURATION.toInt(),
                    easing = LinearEasing
                )
            )
        }
        // Auto-save after 2s if no interaction
        delay(DesignSystem.Animation.PHOTO_PREVIEW_DURATION)
        if (!userInteracted && !dismissed) {
            dismissed = true
            onAccept()
        }
    }

    if (dismissed) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(entryAlpha.value)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Photo with film-develop + swipe gesture
        Image(
            bitmap = bitmap,
            contentDescription = "拍摄预览",
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
                .fillMaxSize()
                .graphicsLayer {
                    translationY = swipeOffsetY.value
                    alpha = swipeAlpha.value * developAlpha.value
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            userInteracted = true
                        },
                        onVerticalDrag = { _, dragAmount ->
                            scope.launch {
                                swipeOffsetY.snapTo(swipeOffsetY.value + dragAmount)
                                // Alpha decreases proportionally with offset
                                val offsetDp = kotlin.math.abs(swipeOffsetY.value) / density
                                val alphaValue = 1f - (offsetDp / 300f).coerceIn(0f, 0.7f)
                                swipeAlpha.snapTo(alphaValue)
                            }
                        },
                        onDragEnd = {
                            val offsetPx = kotlin.math.abs(swipeOffsetY.value)
                            val thresholdPx = with(LocalDensity.current) { 100.dp.toPx() }
                            if (offsetPx > thresholdPx) {
                                // Threshold exceeded
                                if (swipeOffsetY.value < 0) {
                                    // Swiped up → save
                                    dismissed = true
                                    onAccept()
                                } else {
                                    // Swiped down → delete
                                    dismissed = true
                                    onDelete()
                                }
                            } else {
                                // Snap back
                                scope.launch {
                                    swipeOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = DesignSystem.Animation.smooth
                                    )
                                    swipeAlpha.animateTo(
                                        targetValue = 1f,
                                        animationSpec = DesignSystem.Animation.smooth
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                swipeOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = DesignSystem.Animation.smooth
                                )
                                swipeAlpha.animateTo(
                                    targetValue = 1f,
                                    animationSpec = DesignSystem.Animation.smooth
                                )
                            }
                        }
                    )
                }
        )

        // Auto-save countdown progress line at the top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(2.dp)
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.15f))
            )
            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(countdownProgress.value)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.6f))
            )
        }

        // Bottom action bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .graphicsLayer {
                    translationY = bottomBarOffset.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pill-shaped container
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "删除",
                    tint = DesignSystem.Colors.error,
                    onClick = {
                        userInteracted = true
                        dismissed = true
                        onDelete()
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Edit
                ActionButton(
                    icon = Icons.Default.Edit,
                    label = "编辑",
                    tint = DesignSystem.Colors.minimalLabel,
                    onClick = {
                        userInteracted = true
                        onEdit()
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Share
                ActionButton(
                    icon = Icons.Default.Share,
                    label = "分享",
                    tint = DesignSystem.Colors.primary,
                    onClick = {
                        userInteracted = true
                        onShare()
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Save
                ActionButton(
                    icon = Icons.Default.CheckCircle,
                    label = "保存",
                    tint = DesignSystem.Colors.success,
                    onClick = {
                        userInteracted = true
                        dismissed = true
                        onAccept()
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = DesignSystem.Animation.quick,
        label = "actionButtonScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
