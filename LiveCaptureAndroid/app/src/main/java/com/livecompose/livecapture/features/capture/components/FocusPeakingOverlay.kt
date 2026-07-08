package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

/**
 * 峰值对焦覆盖层
 * 从预览帧 Bitmap 计算边缘强度（Sobel 算子）
 * 将高对比度边缘以配置颜色高亮显示
 */
@Composable
fun FocusPeakingOverlay(
    previewBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    peakColor: PeakColor = PeakColor.RED,
    sensitivity: Float = 0.5f,
    transparency: Float = 0.6f,
    downscaleFactor: Int = 4
) {
    val peakPoints = remember(previewBitmap, sensitivity, downscaleFactor) {
        if (previewBitmap == null) return@remember emptyList<PeakPoint>()
        computeEdgePoints(previewBitmap, sensitivity, downscaleFactor)
    }

    val paintColor = when (peakColor) {
        PeakColor.RED -> Color(1f, 0f, 0f, transparency)
        PeakColor.YELLOW -> Color(1f, 1f, 0f, transparency)
        PeakColor.BLUE -> Color(0f, 0.5f, 1f, transparency)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / (previewBitmap?.width?.toFloat() ?: 1f)
        val scaleY = size.height / (previewBitmap?.height?.toFloat() ?: 1f)

        for (point in peakPoints) {
            drawCircle(
                color = paintColor,
                radius = 2.5f,
                center = Offset(
                    point.x * scaleX * downscaleFactor,
                    point.y * scaleY * downscaleFactor
                )
            )
        }
    }
}

enum class PeakColor(val displayName: String) {
    RED("红色"),
    YELLOW("黄色"),
    BLUE("蓝色")
}

data class PeakPoint(val x: Float, val y: Float, val intensity: Float)

/**
 * 使用 Sobel 算子计算边缘强度
 */
private fun computeEdgePoints(
    bitmap: Bitmap,
    sensitivity: Float,
    downscale: Int
): List<PeakPoint> {
    val points = mutableListOf<PeakPoint>()

    val scaledWidth = bitmap.width / downscale
    val scaledHeight = bitmap.height / downscale

    if (scaledWidth < 2 || scaledHeight < 2) return points

    // 缩小图像提高性能
    val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

    val pixels = IntArray(scaledWidth * scaledHeight)
    scaled.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)

    val gray = FloatArray(scaledWidth * scaledHeight)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = AndroidColor.red(pixel)
        val g = AndroidColor.green(pixel)
        val b = AndroidColor.blue(pixel)
        // 加权灰度转换 (感知亮度)
        gray[i] = 0.299f * r + 0.587f * g + 0.114f * b
    }

    // Sobel 算子
    // Gx: [-1 0 1; -2 0 2; -1 0 1]
    // Gy: [-1 -2 -1; 0 0 0; 1 2 1]

    for (y in 1 until scaledHeight - 1) {
        for (x in 1 until scaledWidth - 1) {
            val idx = y * scaledWidth + x

            val tl = gray[idx - scaledWidth - 1]
            val t = gray[idx - scaledWidth]
            val tr = gray[idx - scaledWidth + 1]
            val l = gray[idx - 1]
            val r = gray[idx + 1]
            val bl = gray[idx + scaledWidth - 1]
            val b = gray[idx + scaledWidth]
            val br = gray[idx + scaledWidth + 1]

            val gx = -tl + tr - 2f * l + 2f * r - bl + br
            val gy = -tl - 2f * t - tr + bl + 2f * b + br

            val magnitude = sqrt(gx * gx + gy * gy)

            // 根据灵敏度过滤
            val threshold = 50f + (1f - sensitivity) * 150f
            if (magnitude > threshold) {
                points.add(
                    PeakPoint(
                        x = x.toFloat(),
                        y = y.toFloat(),
                        intensity = (magnitude / 255f).coerceAtMost(1f)
                    )
                )
            }
        }
    }

    scaled.recycle()
    return points
}