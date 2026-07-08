package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * 构图网格叠加层
 * 支持多种网格模式切换：关闭 / 九宫格 / 十字 / 黄金螺旋
 *
 * @param gridMode 当前网格模式
 * @param aspectRatio 画幅比例（如 3/4, 1/1, 16/9 等）
 * @param modifier 修饰符
 */
enum class GridMode(val displayName: String) {
    NONE("关闭"),
    RULE_OF_THIRDS("九宫格"),
    CROSS("十字线"),
    GOLDEN_SPIRAL("黄金螺旋"),
    DIAGONAL("对角线")
}

private data class DrawArea(val w: Float, val h: Float, val ox: Float, val oy: Float)

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun GridOverlay(
    gridMode: GridMode,
    aspectRatio: Float = 3f / 4f,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableFloatStateOf(if (gridMode == GridMode.NONE) 0f else 1f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 300),
        label = "gridAlpha"
    )

    if (gridMode == GridMode.NONE && animationProgress <= 0.01f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        if (animatedAlpha < 0.01f) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        // 根据画面比例计算有效绘制区域
        val containerRatio = 3f / 4f
        val (drawW, drawH, offX, offY) = if (aspectRatio > containerRatio) {
            val h = canvasWidth / aspectRatio
            DrawArea(canvasWidth, h, 0f, (canvasHeight - h) / 2f)
        } else {
            val w = canvasHeight * aspectRatio
            DrawArea(w, canvasHeight, (canvasWidth - w) / 2f, 0f)
        }

        val gridColor = Color.White.copy(alpha = 0.4f * animatedAlpha)
        val goldenColor = Color(0xFFFFAB00).copy(alpha = 0.5f * animatedAlpha)
        val strokeWidth = 1.2f

        when (gridMode) {
            GridMode.RULE_OF_THIRDS -> {
                // 九宫格：两条垂直线 + 两条水平线
                for (i in 1..2) {
                    val vx = offX + drawW * i / 3f
                    drawLine(gridColor, Offset(vx, offY), Offset(vx, offY + drawH), strokeWidth)
                    val hy = offY + drawH * i / 3f
                    drawLine(gridColor, Offset(offX, hy), Offset(offX + drawW, hy), strokeWidth)
                }
                // 四个交叉点圆圈
                for (i in listOf(1, 2)) {
                    for (j in listOf(1, 2)) {
                        val cx = offX + drawW * i / 3f
                        val cy = offY + drawH * j / 3f
                        drawCircle(goldenColor, radius = 6f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
                    }
                }
            }

            GridMode.CROSS -> {
                // 十字线：垂直中线 + 水平中线
                val cx = offX + drawW / 2
                val cy = offY + drawH / 2
                drawLine(gridColor.copy(alpha = 0.25f * animatedAlpha), Offset(cx, offY), Offset(cx, offY + drawH), strokeWidth)
                drawLine(gridColor.copy(alpha = 0.25f * animatedAlpha), Offset(offX, cy), Offset(offX + drawW, cy), strokeWidth)
            }

            GridMode.GOLDEN_SPIRAL -> {
                // 黄金比例分割线（简化版：黄金分割点）
                val phi = 1.618033988749895f
                val gx = offX + drawW / phi
                val gy = offY + drawH / phi
                drawLine(goldenColor, Offset(gx, offY), Offset(gx, offY + drawH), strokeWidth)
                drawLine(goldenColor, Offset(offX, gy), Offset(offX + drawW, gy), strokeWidth)
                drawCircle(goldenColor, radius = 10f, center = Offset(gx, gy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
            }

            GridMode.DIAGONAL -> {
                // 对角线
                drawLine(gridColor, Offset(offX, offY), Offset(offX + drawW, offY + drawH), strokeWidth)
                drawLine(gridColor, Offset(offX + drawW, offY), Offset(offX, offY + drawH), strokeWidth)
            }

            GridMode.NONE -> {}
        }
    }
}
