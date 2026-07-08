package com.livecompose.livecapture.core.composition

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * AR 构图引导叠加视图
 *
 * 对应 iOS 端 ARCompositionGuideView.swift，在相机预览上以 Jetpack Compose Canvas
 * 渲染构图引导线、水平指示器和评分徽章。
 *
 * ## 绘制内容
 * 1. 构图引导线（三分法/黄金比例/黄金螺旋/对称/中心聚焦/引导线/方形/无）
 * 2. 水平指示器（按横滚角度旋转的水平线，水平时绿色，否则黄色）
 * 3. 评分徽章（右上角圆角徽章，显示综合评分与等级颜色）
 */

// MARK: - 构图引导叠加层

/**
 * 构图引导叠加层
 *
 * @param guideType 构图引导线类型
 * @param score 当前构图评分（可为空）
 * @param showScore 是否显示评分徽章
 * @param showLevel 是否显示水平指示器
 * @param rollAngle 当前横滚角度（度数）
 * @param modifier 修饰符
 */
@Composable
fun ARCompositionGuideOverlay(
    guideType: CompositionGuideType,
    score: CompositionScore?,
    showScore: Boolean,
    showLevel: Boolean,
    rollAngle: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // 1. 绘制构图引导线
        drawGuideLines(guideType)

        // 2. 绘制水平指示器
        if (showLevel) {
            drawLevelIndicator(rollAngle)
        }

        // 3. 绘制评分徽章
        if (showScore && score != null) {
            val badgeWidth = 80.dp.toPx()
            val badgeHeight = 44.dp.toPx()
            val padding = 16.dp.toPx()
            val x = size.width - badgeWidth - padding
            val y = 60.dp.toPx()
            drawBadge(score, x, y, badgeWidth, badgeHeight)
        }
    }
}

// MARK: - 评分徽章（独立组件）

/**
 * 评分徽章组件
 *
 * 在自身画布范围内绘制一个带等级颜色的评分徽章。
 * 调用方应通过 [modifier] 指定尺寸（例如 `Modifier.size(80.dp, 44.dp)`）。
 *
 * @param score 构图评分
 * @param modifier 修饰符
 */
@Composable
fun ScoreBadge(score: CompositionScore, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawBadge(score, 0f, 0f, size.width, size.height)
    }
}

// MARK: - 构图引导线绘制

/**
 * 根据引导线类型绘制对应的构图引导线
 */
private fun DrawScope.drawGuideLines(guideType: CompositionGuideType) {
    val lineColor = Color.White.copy(alpha = 0.6f)
    val pointColor = Color.White.copy(alpha = 0.8f)
    val strokeWidthPx = 1.dp.toPx()
    val dotRadius = 4.dp.toPx()

    when (guideType) {
        CompositionGuideType.RULE_OF_THIRDS -> {
            // 2 条垂直线 + 2 条水平线（1/3、2/3 处）
            val x1 = size.width / 3f
            val x2 = size.width * 2f / 3f
            val y1 = size.height / 3f
            val y2 = size.height * 2f / 3f
            drawLine(lineColor, Offset(x1, 0f), Offset(x1, size.height), strokeWidthPx)
            drawLine(lineColor, Offset(x2, 0f), Offset(x2, size.height), strokeWidthPx)
            drawLine(lineColor, Offset(0f, y1), Offset(size.width, y1), strokeWidthPx)
            drawLine(lineColor, Offset(0f, y2), Offset(size.width, y2), strokeWidthPx)
            // 四个三分点
            listOf(
                Offset(x1, y1), Offset(x2, y1),
                Offset(x1, y2), Offset(x2, y2)
            ).forEach { point ->
                drawCircle(pointColor, dotRadius, point)
            }
        }

        CompositionGuideType.GOLDEN_RATIO -> {
            // 黄金比例线（0.382、0.618）
            val gx1 = size.width * 0.382f
            val gx2 = size.width * 0.618f
            val gy1 = size.height * 0.382f
            val gy2 = size.height * 0.618f
            val goldenColor = Color(red = 0.98f, green = 0.72f, blue = 0.28f, alpha = 0.6f)
            drawLine(goldenColor, Offset(gx1, 0f), Offset(gx1, size.height), strokeWidthPx)
            drawLine(goldenColor, Offset(gx2, 0f), Offset(gx2, size.height), strokeWidthPx)
            drawLine(goldenColor, Offset(0f, gy1), Offset(size.width, gy1), strokeWidthPx)
            drawLine(goldenColor, Offset(0f, gy2), Offset(size.width, gy2), strokeWidthPx)
            listOf(
                Offset(gx1, gy1), Offset(gx2, gy1),
                Offset(gx1, gy2), Offset(gx2, gy2)
            ).forEach { point ->
                drawCircle(goldenColor, dotRadius, point)
            }
        }

        CompositionGuideType.GOLDEN_SPIRAL -> {
            drawGoldenSpiral()
        }

        CompositionGuideType.SYMMETRY -> {
            // 1 条垂直中线 + 1 条水平中线
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(lineColor, Offset(cx, 0f), Offset(cx, size.height), strokeWidthPx)
            drawLine(lineColor, Offset(0f, cy), Offset(size.width, cy), strokeWidthPx)
            drawCircle(pointColor, dotRadius * 1.5f, Offset(cx, cy))
        }

        CompositionGuideType.CENTER_FOCUS -> {
            drawCenterCrosshair(lineColor, pointColor, strokeWidthPx)
        }

        CompositionGuideType.LEADING_LINES -> {
            // 四角到中心的引导线
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(lineColor, Offset(0f, 0f), Offset(cx, cy), strokeWidthPx)
            drawLine(lineColor, Offset(size.width, 0f), Offset(cx, cy), strokeWidthPx)
            drawLine(lineColor, Offset(0f, size.height), Offset(cx, cy), strokeWidthPx)
            drawLine(lineColor, Offset(size.width, size.height), Offset(cx, cy), strokeWidthPx)
            drawCircle(pointColor, dotRadius, Offset(cx, cy))
        }

        CompositionGuideType.SQUARE -> {
            // 中心方形框
            val side = min(size.width, size.height) * 0.7f
            val left = (size.width - side) / 2f
            val top = (size.height - side) / 2f
            drawRect(
                color = lineColor,
                topLeft = Offset(left, top),
                size = Size(side, side),
                style = Stroke(strokeWidthPx)
            )
        }

        CompositionGuideType.NONE -> {
            // 不绘制任何引导线
        }
    }
}

// MARK: - 黄金螺旋（用弧近似）

/**
 * 用递减的 1/4 圆弧近似黄金螺旋，并叠加黄金分割线
 */
private fun DrawScope.drawGoldenSpiral() {
    val phi = 1.618034f
    val spiralColor = Color(red = 0.98f, green = 0.72f, blue = 0.28f, alpha = 0.6f)
    val strokeWidthPx = 1.dp.toPx()
    val arcStrokeWidthPx = 1.5.dp.toPx()

    // 黄金分割线
    val gx = size.width / phi
    val gy = size.height / phi
    drawLine(spiralColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidthPx)
    drawLine(spiralColor, Offset(0f, gy), Offset(size.width, gy), strokeWidthPx)

    // 递减 1/4 圆弧近似螺旋
    var currentSize = size.width.coerceAtMost(size.height)
    var x = 0f
    var y = size.height - currentSize / phi
    for (i in 0 until 6) {
        val arcSize = currentSize / Math.pow(phi.toDouble(), i.toDouble()).toFloat()
        if (arcSize <= 0f) break
        val centerX = x + arcSize
        val centerY = y + arcSize
        drawArc(
            color = spiralColor,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(centerX - arcSize, centerY - arcSize),
            size = Size(arcSize * 2f, arcSize * 2f),
            style = Stroke(arcStrokeWidthPx)
        )
        x += arcSize
        y += arcSize * (1f - 1f / phi)
    }
}

// MARK: - 中心聚焦十字

/**
 * 绘制中心聚焦十字准星（四角 L 形 + 中心点）
 */
private fun DrawScope.drawCenterCrosshair(
    lineColor: Color,
    pointColor: Color,
    strokeWidthPx: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val crossSize = 30.dp.toPx()
    val gap = 8.dp.toPx()
    val dotRadius = 4.dp.toPx()

    // 左上 L 形
    drawLine(lineColor, Offset(cx - crossSize, cy - gap), Offset(cx - gap, cy - gap), strokeWidthPx)
    drawLine(lineColor, Offset(cx - gap, cy - crossSize), Offset(cx - gap, cy - gap), strokeWidthPx)
    // 右上 L 形
    drawLine(lineColor, Offset(cx + gap, cy - crossSize), Offset(cx + gap, cy - gap), strokeWidthPx)
    drawLine(lineColor, Offset(cx + gap, cy - gap), Offset(cx + crossSize, cy - gap), strokeWidthPx)
    // 左下 L 形
    drawLine(lineColor, Offset(cx - crossSize, cy + gap), Offset(cx - gap, cy + gap), strokeWidthPx)
    drawLine(lineColor, Offset(cx - gap, cy + gap), Offset(cx - gap, cy + crossSize), strokeWidthPx)
    // 右下 L 形
    drawLine(lineColor, Offset(cx + gap, cy + gap), Offset(cx + crossSize, cy + gap), strokeWidthPx)
    drawLine(lineColor, Offset(cx + gap, cy + crossSize), Offset(cx + gap, cy + gap), strokeWidthPx)

    drawCircle(pointColor, dotRadius, Offset(cx, cy))
}

// MARK: - 水平指示器

/**
 * 绘制水平指示器：按横滚角度旋转的水平线
 *
 * |rollAngle| < 1° 时为绿色，否则为黄色。
 */
private fun DrawScope.drawLevelIndicator(rollAngle: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val lineLength = min(size.width, size.height) * 0.3f
    val isLevel = abs(rollAngle) < 1f
    val color = if (isLevel) {
        Color(red = 0.2f, green = 0.78f, blue = 0.35f, alpha = 0.9f)
    } else {
        Color(red = 1.0f, green = 0.85f, blue = 0.0f, alpha = 0.9f)
    }
    val strokeWidthPx = 3.dp.toPx()

    val radians = Math.toRadians(rollAngle.toDouble()).toFloat()
    val dx = cos(radians) * lineLength
    val dy = sin(radians) * lineLength
    drawLine(color, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), strokeWidthPx)
}

// MARK: - 评分徽章绘制

/**
 * 在指定位置绘制评分徽章
 *
 * @param score 构图评分
 * @param x 徽章左上角 x
 * @param y 徽章左上角 y
 * @param width 徽章宽度
 * @param height 徽章高度
 */
private fun DrawScope.drawBadge(
    score: CompositionScore,
    x: Float,
    y: Float,
    width: Float,
    height: Float
) {
    if (width <= 0f || height <= 0f) return
    val cornerRadius = min(10.dp.toPx(), height / 4f)
    val gradeColor = Color(score.grade.color)

    // 半透明黑色背景
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )

    // 等级颜色边框
    drawRoundRect(
        color = gradeColor,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(2.dp.toPx())
    )

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas

        // 综合评分文字
        val scorePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = height * 0.45f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val scoreCenterY = y + height * 0.42f
        val scoreBaseline = scoreCenterY - (scorePaint.descent() + scorePaint.ascent()) / 2f
        nativeCanvas.drawText(score.overall.toString(), x + width / 2f, scoreBaseline, scorePaint)

        // 等级文字
        val gradePaint = android.graphics.Paint().apply {
            color = gradeColor.toArgb()
            textSize = height * 0.22f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val gradeBaseline = y + height * 0.85f
        nativeCanvas.drawText(score.grade.displayName, x + width / 2f, gradeBaseline, gradePaint)
    }
}
