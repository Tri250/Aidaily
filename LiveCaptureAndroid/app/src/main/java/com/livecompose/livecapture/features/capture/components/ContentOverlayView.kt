package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 内容叠加层视图
 */
@Composable
fun ContentOverlayView(
    compositionRect: RectF,
    cropRect: RectF?,
    boxCenter: PointF?,
    isAligned: Boolean,
    distanceToCenter: Float?,
    modifier: Modifier = Modifier
) {
    val animatedIsAligned by animateFloatAsState(
        targetValue = if (isAligned) 1f else 0f,
        animationSpec = tween(300)
    )

    val animatedDistance by animateFloatAsState(
        targetValue = distanceToCenter ?: 0f,
        animationSpec = tween(200)
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 半透明遮罩
        drawRect(
            color = Color.Black.copy(alpha = 0.3f),
            size = Size(canvasWidth, canvasHeight)
        )

        // 裁剪区域（挖空）
        if (compositionRect.width() > 0 && compositionRect.height() > 0) {
            val left = compositionRect.left * canvasWidth
            val top = compositionRect.top * canvasHeight
            val right = compositionRect.right * canvasWidth
            val bottom = compositionRect.bottom * canvasHeight

            drawRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                blendMode = BlendMode.Clear
            )

            // 构图边框
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 1.5f)
            )

            // 九宫格线
            val thirdW = (right - left) / 3
            val thirdH = (bottom - top) / 3
            val gridColor = Color.White.copy(alpha = 0.15f)
            for (i in 1..2) {
                drawLine(gridColor, Offset(left + thirdW * i, top), Offset(left + thirdW * i, bottom), 0.5f)
                drawLine(gridColor, Offset(left, top + thirdH * i), Offset(right, top + thirdH * i), 0.5f)
            }
        }

        // 检测框
        cropRect?.let { rect ->
            if (rect.width() > 0 && rect.height() > 0) {
                val left = rect.left * canvasWidth
                val top = rect.top * canvasHeight
                val right = rect.right * canvasWidth
                val bottom = rect.bottom * canvasHeight

                val boxColor = Color(
                    red = 1.0f,
                    green = (1f - animatedIsAligned) * 0.67f,
                    blue = 0f,
                    alpha = 0.8f
                )

                drawRect(
                    color = boxColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 3f)
                )

                // 圆角边框
                drawRoundRect(
                    color = boxColor.copy(alpha = 0.3f),
                    topLeft = Offset(left - 2, top - 2),
                    size = Size(right - left + 4, bottom - top + 4),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1.5f)
                )
            }
        }

        // 追踪点
        boxCenter?.let { center ->
            val cx = center.x * canvasWidth
            val cy = center.y * canvasHeight

            // 外圈光晕
            val glowRadius = 20f + animatedIsAligned * 8f
            drawCircle(
                color = Color.White.copy(alpha = 0.15f + animatedIsAligned * 0.15f),
                radius = glowRadius,
                center = Offset(cx, cy)
            )

            // 追踪圆
            val dotColor = Color(
                red = 1f,
                green = (1f - animatedIsAligned) * 0.67f,
                blue = 0f,
                alpha = 0.9f
            )
            drawCircle(color = dotColor, radius = 8f, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = 4f, center = Offset(cx, cy))

            // 对准指示环
            if (animatedIsAligned > 0.5f) {
                drawCircle(
                    color = Color(0xFF00C853).copy(alpha = animatedIsAligned),
                    radius = 14f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
            }

            // 距离指示器
            if (distanceToCenter != null && distanceToCenter > 0) {
                val compCenterX = (compositionRect.left + compositionRect.width() / 2) * canvasWidth
                val compCenterY = (compositionRect.top + compositionRect.height() / 2) * canvasHeight
                val lineColor = Color.White.copy(alpha = 0.25f * (1f - animatedIsAligned))
                drawLine(
                    lineColor,
                    Offset(cx, cy),
                    Offset(compCenterX, compCenterY),
                    1f
                )
            }
        }
    }
}