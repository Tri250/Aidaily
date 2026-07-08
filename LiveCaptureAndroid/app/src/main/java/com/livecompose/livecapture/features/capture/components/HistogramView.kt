package com.livecompose.livecapture.features.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

/**
 * 直方图组件
 * 实时显示图像亮度分布 (256 级灰度直方图)
 *
 * @param histogramData 256 个整数的亮度分布数组，可为 null 表示无数据
 * @param modifier 修饰符
 */
@Composable
fun HistogramView(
    histogramData: IntArray?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(120.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = androidx.compose.ui.Modifier.matchParentSize()) {
            if (histogramData == null || histogramData.isEmpty() || histogramData.size != 256) return@Canvas

            val maxCount = histogramData.maxOrNull()?.coerceAtLeast(1) ?: return@Canvas
            val width = size.width
            val height = size.height

            // 绘制填充路径
            val path = Path().apply {
                moveTo(0f, height)
                for (i in histogramData.indices) {
                    val barHeight = (histogramData[i].toFloat() / maxCount * height).coerceIn(0f, height)
                    lineTo(width * i / 256f, height - barHeight)
                }
                lineTo(width, height)
                close()
            }

            drawPath(path = path, color = Color.White.copy(alpha = 0.35f))

            // 绘制顶部高亮边线
            val outlinePath = Path().apply {
                for (i in histogramData.indices) {
                    val barHeight = (histogramData[i].toFloat() / maxCount * height).coerceIn(0f, height)
                    if (i == 0) moveTo(width * i / 256f, height - barHeight)
                    else lineTo(width * i / 256f, height - barHeight)
                }
            }
            drawPath(
                path = outlinePath,
                color = Color.White.copy(alpha = 0.8f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
        }
    }
}

/**
 * 从 Bitmap 计算亮度直方图数据
 * 将图像转换为灰度后统计 256 个亮度等级的像素数量
 */
fun computeHistogramFromBitmap(bitmap: android.graphics.Bitmap): IntArray {
    val histogram = IntArray(256)
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    for (pixel in pixels) {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b + 0.5).toInt().coerceIn(0, 255)
        histogram[luminance]++
    }

    return histogram
}
