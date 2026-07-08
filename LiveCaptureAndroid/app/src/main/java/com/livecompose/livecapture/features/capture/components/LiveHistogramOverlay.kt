package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 实时取景直方图覆盖层
 * 从预览帧实时计算亮度直方图（256 级）
 * 同时计算 RGB 三通道直方图
 * 以半透明叠加方式绘制在取景器角落
 */
@Composable
fun LiveHistogramOverlay(
    previewBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    showRGB: Boolean = true,
    backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
    cornerRadius: Int = 12
) {
    var histogramData by remember { mutableStateOf<HistogramData?>(null) }

    LaunchedEffect(previewBitmap) {
        if (previewBitmap == null) {
            histogramData = null
        } else {
            histogramData = withContext(Dispatchers.Default) {
                computeHistogram(previewBitmap)
            }
        }
    }

    Box(
        modifier = modifier
            .width(120.dp)
            .height(90.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val data = histogramData
        if (data != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawHistogram(data, showRGB)
            }
        }
    }
}

data class HistogramData(
    val luminance: List<Int>,      // 256 级亮度直方图
    val red: List<Int>,            // 256 级红色通道
    val green: List<Int>,          // 256 级绿色通道
    val blue: List<Int>,           // 256 级蓝色通道
    val maxLuminance: Int,
    val maxRgb: Int
)

/**
 * 计算亮度直方图和 RGB 三通道直方图
 */
private fun computeHistogram(bitmap: Bitmap): HistogramData {
    val luminance = IntArray(256)
    val red = IntArray(256)
    val green = IntArray(256)
    val blue = IntArray(256)

    // 缩小以提高性能
    val scaledWidth = bitmap.width / 4
    val scaledHeight = bitmap.height / 4
    val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth.coerceAtLeast(1), scaledHeight.coerceAtLeast(1), true)

    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)

    for (pixel in pixels) {
        val r = AndroidColor.red(pixel)
        val g = AndroidColor.green(pixel)
        val b = AndroidColor.blue(pixel)

        // 感知亮度
        val lum = (0.299f * r + 0.587f * g + 0.114f * b).roundToInt().coerceIn(0, 255)

        luminance[lum]++
        red[r]++
        green[g]++
        blue[b]++
    }

    scaled.recycle()

    return HistogramData(
        luminance = luminance.toList(),
        red = red.toList(),
        green = green.toList(),
        blue = blue.toList(),
        maxLuminance = luminance.maxOrNull() ?: 1,
        maxRgb = maxOf(
            red.maxOrNull() ?: 1,
            green.maxOrNull() ?: 1,
            blue.maxOrNull() ?: 1
        )
    )
}

/**
 * 绘制直方图
 */
private fun DrawScope.drawHistogram(data: HistogramData, showRGB: Boolean) {
    val width = size.width
    val height = size.height
    val barWidth = width / 256f

    // 绘制背景网格
    drawGrid(width, height)

    // 绘制 RGB 通道直方图（先绘制，在亮度图下方）
    if (showRGB) {
        drawChannelHistogram(
            data.red, data.maxRgb, Color.Red.copy(alpha = 0.5f),
            width, height, barWidth
        )
        drawChannelHistogram(
            data.green, data.maxRgb, Color.Green.copy(alpha = 0.5f),
            width, height, barWidth
        )
        drawChannelHistogram(
            data.blue, data.maxRgb, Color.Blue.copy(alpha = 0.5f),
            width, height, barWidth
        )
    }

    // 绘制亮度直方图（最上层）
    drawChannelHistogram(
        data.luminance, data.maxLuminance, Color.White.copy(alpha = 0.85f),
        width, height, barWidth
    )

    // 绘制底部基线
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = Offset(0f, height),
        end = Offset(width, height),
        strokeWidth = 1f
    )
}

/**
 * 绘制单个通道直方图
 */
private fun DrawScope.drawChannelHistogram(
    channelData: List<Int>,
    maxValue: Int,
    color: Color,
    width: Float,
    height: Float,
    barWidth: Float
) {
    if (maxValue <= 0) return

    val path = Path()
    var firstPoint = true

    for (i in 0 until 256) {
        val x = i * barWidth
        val normalizedHeight = (channelData[i].toFloat() / maxValue) * (height - 4f)
        val y = height - normalizedHeight

        if (firstPoint) {
            path.moveTo(x, y)
            firstPoint = false
        } else {
            path.lineTo(x, y)
        }
    }

    // 闭合路径以便填充
    path.lineTo(255 * barWidth, height)
    path.lineTo(0f, height)
    path.close()

    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Fill
    )
}

/**
 * 绘制网格线
 */
private fun DrawScope.drawGrid(width: Float, height: Float) {
    val gridColor = Color.White.copy(alpha = 0.1f)

    // 水平线
    for (i in 1..3) {
        val y = height * i / 4f
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.5f
        )
    }

    // 垂直线（四分位）
    for (i in 1..3) {
        val x = width * i / 4f
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 0.5f
        )
    }
}