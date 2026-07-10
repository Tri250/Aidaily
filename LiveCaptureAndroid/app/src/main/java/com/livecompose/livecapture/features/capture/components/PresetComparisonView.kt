package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 预设对比视图
 *
 * 对标 OPPO Find X9 哈苏大师的对比功能：
 * 1. 左右分屏对比（原图 vs 预设效果）
 * 2. 拖动分割线调节对比区域
 * 3. 显示预设名称
 * 4. 支持确认/取消操作
 */
@Composable
fun PresetComparisonView(
    originalBitmap: Bitmap?,
    presetBitmap: Bitmap?,
    presetName: String,
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (originalBitmap != null && presetBitmap != null) {
                ComparisonSplitView(
                    originalBitmap = originalBitmap,
                    presetBitmap = presetBitmap,
                    presetName = presetName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 底部操作栏
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 取消
                FilledIconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "取消", tint = Color.White)
                }

                // 预设名称
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        presetName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "左右滑动分割线对比效果",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                // 确认
                FilledIconButton(
                    onClick = onConfirm,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = DesignSystem.Colors.primary
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "确认", tint = Color.White)
                }
            }

            // 左侧标签
            Text(
                "原图",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // 右侧标签
            Text(
                presetName,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ComparisonSplitView(
    originalBitmap: Bitmap,
    presetBitmap: Bitmap,
    presetName: String,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var viewWidth by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.onSizeChanged { viewWidth = it.width }) {
        // 预设效果图（底层）
        androidx.compose.foundation.Image(
            bitmap = presetBitmap.asImageBitmap(),
            contentDescription = "预设效果",
            modifier = Modifier.fillMaxSize()
        )

        // 原图（上层，裁剪到分割线左侧）
        if (viewWidth > 0) {
            val clipWidth = (viewWidth * sliderPosition).toInt()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                androidx.compose.foundation.Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = "原图",
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = (0).dp)
                )
                // 裁剪遮罩
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = (clipWidth / LocalDensity.current.density).dp)
                ) {
                    // 右侧用黑色遮罩覆盖
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset.Zero,
                        size = size
                    )
                }
            }
        }

        // 分割线
        if (viewWidth > 0) {
            val sliderX = (viewWidth * sliderPosition).toFloat()
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 分割线
                drawLine(
                    color = Color.White,
                    start = Offset(sliderX, 0f),
                    end = Offset(sliderX, size.height),
                    strokeWidth = 2f
                )
                // 手柄圆
                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = Offset(sliderX, size.height / 2f)
                )
                drawCircle(
                    color = DesignSystem.Colors.primary,
                    radius = 10f,
                    center = Offset(sliderX, size.height / 2f)
                )
                // 手柄箭头
                drawLine(
                    color = Color.White,
                    start = Offset(sliderX - 6f, size.height / 2f),
                    end = Offset(sliderX - 2f, size.height / 2f - 4f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(sliderX - 6f, size.height / 2f),
                    end = Offset(sliderX - 2f, size.height / 2f + 4f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(sliderX + 6f, size.height / 2f),
                    end = Offset(sliderX + 2f, size.height / 2f - 4f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(sliderX + 6f, size.height / 2f),
                    end = Offset(sliderX + 2f, size.height / 2f + 4f),
                    strokeWidth = 2f
                )
            }
        }

        // 拖动控制
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {},
                        onDragEnd = {},
                        onDragCancel = {},
                        onHorizontalDrag = { _, dragAmount ->
                            if (viewWidth > 0) {
                                val delta = dragAmount / viewWidth.toFloat()
                                sliderPosition = (sliderPosition + delta).coerceIn(0.05f, 0.95f)
                            }
                        }
                    )
                }
        )
    }
}