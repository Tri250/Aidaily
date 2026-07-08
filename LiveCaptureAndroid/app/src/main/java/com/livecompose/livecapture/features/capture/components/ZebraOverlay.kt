package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 斑马纹过曝警告覆盖层
 * 检测预览帧中亮度超过阈值的像素，以斜条纹图案叠加显示
 */
@Composable
fun ZebraOverlay(
    previewBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    threshold: Float = 0.95f, // 亮度阈值 0.85 ~ 1.0
    stripeColor: Color = Color.Black.copy(alpha = 0.6f),
    backgroundColor: Color = Color.White.copy(alpha = 0.3f),
    stripeAngle: Float = 45f, // 条纹角度
    stripeWidth: Float = 8f, // 条纹宽度
    downscaleFactor: Int = 4
) {
    val zebraRegions = remember(previewBitmap, threshold, downscaleFactor) {
        if (previewBitmap == null) return@remember emptyList<ZebraRegion>()
        detectOverexposedRegions(previewBitmap, threshold, downscaleFactor)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (previewBitmap == null) return@Canvas

        val scaleX = size.width / previewBitmap.width.toFloat()
        val scaleY = size.height / previewBitmap.height.toFloat()

        // 绘制底色区域
        for (region in zebraRegions) {
            val left = region.x * scaleX * downscaleFactor
            val top = region.y * scaleY * downscaleFactor
            val cellWidth = scaleX * downscaleFactor * region.blockSize
            val cellHeight = scaleY * downscaleFactor * region.blockSize

            // 底色
            drawRect(
                color = backgroundColor,
                topLeft = Offset(left, top),
                size = Size(cellWidth, cellHeight)
            )

            // 斑马纹线条
            drawZebraStripes(
                topLeft = Offset(left, top),
                cellSize = Size(cellWidth, cellHeight),
                stripeColor = stripeColor,
                stripeAngle = stripeAngle,
                stripeWidth = stripeWidth
            )
        }
    }
}

data class ZebraRegion(
    val x: Float,
    val y: Float,
    val blockSize: Int = 4,
    val avgBrightness: Float = 0f
)

/**
 * 检测过曝区域
 * 使用分块检测提高性能
 */
private fun detectOverexposedRegions(
    bitmap: Bitmap,
    threshold: Float,
    downscale: Int
): List<ZebraRegion> {
    val regions = mutableListOf<ZebraRegion>()

    val scaledWidth = bitmap.width / downscale
    val scaledHeight = bitmap.height / downscale

    if (scaledWidth < 2 || scaledHeight < 2) return regions

    val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

    val pixels = IntArray(scaledWidth * scaledHeight)
    scaled.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)

    val blockSize = 4
    val thresholdValue = (threshold * 255f).toInt()

    for (by in 0 until scaledHeight - blockSize step blockSize) {
        for (bx in 0 until scaledWidth - blockSize step blockSize) {
            var totalBrightness = 0f
            var count = 0

            for (dy in 0 until blockSize) {
                for (dx in 0 until blockSize) {
                    val x = bx + dx
                    val y = by + dy
                    if (x < scaledWidth && y < scaledHeight) {
                        val pixel = pixels[y * scaledWidth + x]
                        val r = AndroidColor.red(pixel)
                        val g = AndroidColor.green(pixel)
                        val b = AndroidColor.blue(pixel)
                        // 感知亮度
                        val luminance = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                        totalBrightness += luminance
                        count++
                    }
                }
            }

            if (count > 0) {
                val avgBrightness = totalBrightness / count
                if (avgBrightness > thresholdValue) {
                    regions.add(
                        ZebraRegion(
                            x = bx.toFloat(),
                            y = by.toFloat(),
                            blockSize = blockSize,
                            avgBrightness = avgBrightness / 255f
                        )
                    )
                }
            }
        }
    }

    scaled.recycle()
    return regions
}

/**
 * 绘制斑马条纹
 */
private fun DrawScope.drawZebraStripes(
    topLeft: Offset,
    cellSize: Size,
    stripeColor: Color,
    stripeAngle: Float,
    stripeWidth: Float
) {
    val angleRad = stripeAngle * PI.toFloat() / 180f
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)

    // 对角线长度
    val diagonal = kotlin.math.sqrt(cellSize.width * cellSize.width + cellSize.height * cellSize.height)

    // 条纹间距
    val spacing = stripeWidth * 2f

    // 计算在旋转方向上的步进
    val stepX = spacing * cosA
    val stepY = spacing * sinA

    // 绘制多条平行线
    var currentOffset = -diagonal
    while (currentOffset < diagonal * 2) {
        val startX = topLeft.x + currentOffset * cosA
        val startY = topLeft.y + currentOffset * sinA
        val endX = startX + diagonal * sinA * 2
        val endY = startY - diagonal * cosA * 2

        drawLine(
            color = stripeColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = stripeWidth,
            pathEffect = null
        )

        currentOffset += spacing
    }
}