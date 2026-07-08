package com.livecompose.livecapture.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * 曲线编辑器 - 专业曲线编辑面板
 * 支持亮度曲线和 RGB 通道曲线
 * 贝塞尔曲线插值，可添加/删除/拖拽控制点
 * 支持预设曲线
 */
@Composable
fun CurveEditor(
    initialCurve: CurveData = CurveData.defaultLuma(),
    onCurveChanged: (CurveData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentChannel by remember { mutableStateOf(CurveChannel.LUMA) }
    var curveData by remember { mutableStateOf(initialCurve) }
    var selectedPointIndex by remember { mutableStateOf(-1) }
    var showHistogram by remember { mutableStateOf(false) }

    val points = when (currentChannel) {
        CurveChannel.LUMA -> curveData.lumaPoints
        CurveChannel.RED -> curveData.redPoints
        CurveChannel.GREEN -> curveData.greenPoints
        CurveChannel.BLUE -> curveData.bluePoints
    }

    val onPointsChanged: (List<CurvePoint>) -> Unit = { newPoints ->
        curveData = when (currentChannel) {
            CurveChannel.LUMA -> curveData.copy(lumaPoints = newPoints)
            CurveChannel.RED -> curveData.copy(redPoints = newPoints)
            CurveChannel.GREEN -> curveData.copy(greenPoints = newPoints)
            CurveChannel.BLUE -> curveData.copy(bluePoints = newPoints)
        }
        onCurveChanged(curveData)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // 通道选择器
        ChannelSelector(
            currentChannel = currentChannel,
            onChannelSelected = { channel ->
                currentChannel = channel
                selectedPointIndex = -1
            }
        )

        Spacer(Modifier.height(12.dp))

        // 曲线编辑区域
        CurveCanvas(
            points = points,
            channel = currentChannel,
            selectedPointIndex = selectedPointIndex,
            showHistogram = showHistogram,
            onPointsChanged = onPointsChanged,
            onPointSelected = { selectedPointIndex = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(Modifier.height(12.dp))

        // 预设曲线按钮
        PresetCurves(
            currentChannel = currentChannel,
            onPresetSelected = { preset ->
                curveData = preset.applyTo(curveData, currentChannel)
                onCurveChanged(curveData)
                selectedPointIndex = -1
            }
        )

        Spacer(Modifier.height(8.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 重置
            TextButton(
                onClick = {
                    curveData = curveData.resetChannel(currentChannel)
                    onCurveChanged(curveData)
                    selectedPointIndex = -1
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Text("重置", fontSize = 12.sp)
            }

            // 删除选中点
            TextButton(
                onClick = {
                    if (selectedPointIndex >= 0 && selectedPointIndex < points.size) {
                        val mutable = points.toMutableList()
                        mutable.removeAt(selectedPointIndex)
                        onPointsChanged(mutable)
                        selectedPointIndex = -1
                    }
                },
                enabled = selectedPointIndex >= 0 && selectedPointIndex < points.size,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9500))
            ) {
                Text("删除点", fontSize = 12.sp)
            }

            Spacer(Modifier.weight(1f))

            // 显示直方图
            TextButton(
                onClick = { showHistogram = !showHistogram },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (showHistogram) Color(0xFFFF9500) else Color.White.copy(alpha = 0.5f)
                )
            ) {
                Text("直方图", fontSize = 12.sp)
            }
        }
    }
}

/**
 * 曲线通道
 */
enum class CurveChannel(val displayName: String, val lineColor: Color) {
    LUMA("亮度", Color.White),
    RED("红", Color(0xFFFF4444)),
    GREEN("绿", Color(0xFF44FF44)),
    BLUE("蓝", Color(0xFF4488FF))
}

/**
 * 曲线控制点
 */
data class CurvePoint(
    val x: Float, // 0.0 ~ 1.0
    val y: Float  // 0.0 ~ 1.0
)

/**
 * 曲线数据
 */
data class CurveData(
    val lumaPoints: List<CurvePoint>,
    val redPoints: List<CurvePoint>,
    val greenPoints: List<CurvePoint>,
    val bluePoints: List<CurvePoint>
) {
    companion object {
        fun defaultLuma() = CurveData(
            lumaPoints = listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.5f), CurvePoint(1f, 1f)),
            redPoints = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            greenPoints = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            bluePoints = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        )
    }

    fun resetChannel(channel: CurveChannel): CurveData = when (channel) {
        CurveChannel.LUMA -> copy(lumaPoints = listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.5f), CurvePoint(1f, 1f)))
        CurveChannel.RED -> copy(redPoints = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)))
        CurveChannel.GREEN -> copy(greenPoints = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)))
        CurveChannel.BLUE -> copy(bluePoints = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)))
    }
}

/**
 * 曲线预设
 */
enum class CurvePreset(
    val displayName: String,
    val description: String
) {
    S_CURVE("S 曲线", "增强对比度"),
    FILM("胶片曲线", "提亮阴影，压暗高光"),
    LIFT_SHADOWS("提亮阴影", "提升暗部细节"),
    CRUSH_BLACKS("压暗阴影", "深邃暗部"),
    FLAT("平坦", "降低对比度"),
    INVERT("反转", "负片效果")
}

fun CurvePreset.applyTo(data: CurveData, channel: CurveChannel): CurveData {
    val points = when (this) {
        CurvePreset.S_CURVE -> listOf(
            CurvePoint(0f, 0f), CurvePoint(0.25f, 0.15f),
            CurvePoint(0.5f, 0.5f), CurvePoint(0.75f, 0.85f), CurvePoint(1f, 1f)
        )
        CurvePreset.FILM -> listOf(
            CurvePoint(0f, 0.05f), CurvePoint(0.3f, 0.4f),
            CurvePoint(0.7f, 0.65f), CurvePoint(1f, 0.95f)
        )
        CurvePreset.LIFT_SHADOWS -> listOf(
            CurvePoint(0f, 0.1f), CurvePoint(0.3f, 0.4f),
            CurvePoint(0.5f, 0.5f), CurvePoint(1f, 1f)
        )
        CurvePreset.CRUSH_BLACKS -> listOf(
            CurvePoint(0f, 0f), CurvePoint(0.3f, 0.2f),
            CurvePoint(0.5f, 0.5f), CurvePoint(1f, 1f)
        )
        CurvePreset.FLAT -> listOf(
            CurvePoint(0f, 0.0f), CurvePoint(0.2f, 0.3f),
            CurvePoint(0.5f, 0.5f), CurvePoint(0.8f, 0.7f), CurvePoint(1f, 1f)
        )
        CurvePreset.INVERT -> listOf(
            CurvePoint(0f, 1f), CurvePoint(1f, 0f)
        )
    }
    return when (channel) {
        CurveChannel.LUMA -> data.copy(lumaPoints = points)
        CurveChannel.RED -> data.copy(redPoints = points)
        CurveChannel.GREEN -> data.copy(greenPoints = points)
        CurveChannel.BLUE -> data.copy(bluePoints = points)
    }
}

@Composable
private fun ChannelSelector(
    currentChannel: CurveChannel,
    onChannelSelected: (CurveChannel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CurveChannel.entries.forEach { channel ->
            val isSelected = channel == currentChannel
            Surface(
                modifier = Modifier.clickable { onChannelSelected(channel) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) channel.lineColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, channel.lineColor) else null
            ) {
                Text(
                    text = channel.displayName,
                    color = if (isSelected) channel.lineColor else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CurveCanvas(
    points: List<CurvePoint>,
    channel: CurveChannel,
    selectedPointIndex: Int,
    showHistogram: Boolean,
    onPointsChanged: (List<CurvePoint>) -> Unit,
    onPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A2E))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        handleCanvasTap(
                            tapOffset, canvasSize, points,
                            onPointsChanged, onPointSelected
                        )
                    }
                }
                .pointerInput(points, selectedPointIndex) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val idx = findNearestPoint(offset, canvasSize, points, 40f)
                            onPointSelected(idx)
                        },
                        onDrag = { change, _ ->
                            if (selectedPointIndex >= 0 && selectedPointIndex < points.size) {
                                val newX = (change.position.x / canvasSize.width).coerceIn(0f, 1f)
                                val newY = (1f - change.position.y / canvasSize.height).coerceIn(0f, 1f)
                                val mutable = points.toMutableList()
                                mutable[selectedPointIndex] = CurvePoint(newX, newY)
                                // 排序
                                mutable.sortBy { it.x }
                                onPointsChanged(mutable)
                            }
                        }
                    )
                }
        ) {
            canvasSize = size

            val padding = 20f
            val drawRect = Rect(
                padding, padding,
                size.width - padding, size.height - padding
            )

            // 背景网格
            drawGrid(drawRect)

            // 底部直方图参考
            if (showHistogram) {
                drawHistogramBackground(drawRect)
            }

            // 贝塞尔曲线
            drawBezierCurve(points, channel.lineColor, drawRect)

            // 控制点
            drawControlPoints(points, selectedPointIndex, channel.lineColor, drawRect)
        }
    }
}

private fun handleCanvasTap(
    tapOffset: Offset,
    canvasSize: Size,
    points: List<CurvePoint>,
    onPointsChanged: (List<CurvePoint>) -> Unit,
    onPointSelected: (Int) -> Unit
) {
    if (canvasSize == Size.Zero) return

    val padding = 20f
    val drawWidth = canvasSize.width - padding * 2
    val drawHeight = canvasSize.height - padding * 2

    val x = ((tapOffset.x - padding) / drawWidth).coerceIn(0f, 1f)
    val y = (1f - (tapOffset.y - padding) / drawHeight).coerceIn(0f, 1f)

    // 检查是否双击（删除） - 检测靠近已有控制点
    val nearIndex = findNearestPoint(tapOffset, canvasSize, points, 30f)
    if (nearIndex >= 0) {
        // 选中已有控制点
        onPointSelected(nearIndex)
        return
    }

    // 添加新控制点
    val newPoint = CurvePoint(x, y)
    val mutable = points.toMutableList()
    mutable.add(newPoint)
    mutable.sortBy { it.x }
    onPointsChanged(mutable)
    onPointSelected(mutable.indexOf(newPoint))
}

private fun findNearestPoint(
    tapOffset: Offset,
    canvasSize: Size,
    points: List<CurvePoint>,
    maxDistance: Float
): Int {
    if (canvasSize == Size.Zero) return -1

    val padding = 20f
    val drawWidth = canvasSize.width - padding * 2
    val drawHeight = canvasSize.height - padding * 2

    var nearest = -1
    var minDist = Float.MAX_VALUE

    for (i in points.indices) {
        val px = padding + points[i].x * drawWidth
        val py = padding + (1f - points[i].y) * drawHeight
        val dist = sqrt((tapOffset.x - px).pow(2) + (tapOffset.y - py).pow(2))
        if (dist < minDist && dist < maxDistance) {
            minDist = dist
            nearest = i
        }
    }

    return nearest
}

private fun DrawScope.drawGrid(rect: Rect) {
    val gridColor = Color.White.copy(alpha = 0.08f)
    val axisColor = Color.White.copy(alpha = 0.25f)

    // 垂直网格线
    for (i in 0..4) {
        val x = rect.left + (rect.width * i / 4f)
        val color = if (i == 2) axisColor else gridColor
        drawLine(color, Offset(x, rect.top), Offset(x, rect.bottom), 1f)
    }

    // 水平网格线
    for (i in 0..4) {
        val y = rect.top + (rect.height * i / 4f)
        val color = if (i == 2) axisColor else gridColor
        drawLine(color, Offset(rect.left, y), Offset(rect.right, y), 1f)
    }

    // 对角线
    drawLine(
        Color.White.copy(alpha = 0.06f),
        Offset(rect.left, rect.bottom),
        Offset(rect.right, rect.top),
        1f
    )
}

private fun DrawScope.drawHistogramBackground(rect: Rect) {
    // 绘制模拟直方图（山峰形状）
    val histColor = Color.White.copy(alpha = 0.1f)
    val path = Path()
    val segments = 50
    path.moveTo(rect.left, rect.bottom)

    for (i in 0..segments) {
        val x = rect.left + (rect.width * i / segments)
        // 模拟山峰形状
        val t = i.toFloat() / segments
        val peak = sin(t * PI).toFloat() * 0.6f + 0.1f
        // 多个山峰叠加
        val peak2 = sin(t * PI * 3f).toFloat() * 0.2f
        val peak3 = sin(t * PI * 5f).toFloat() * 0.1f
        val h = (peak + peak2 + peak3).coerceIn(0f, 1f)
        val y = rect.bottom - h * rect.height
        path.lineTo(x, y)
    }

    path.lineTo(rect.right, rect.bottom)
    path.close()
    drawPath(path, histColor)
}

/**
 * 使用 Catmull-Rom 样条插值绘制平滑曲线
 */
private fun DrawScope.drawBezierCurve(
    points: List<CurvePoint>,
    color: Color,
    rect: Rect
) {
    if (points.size < 2) return

    val path = Path()
    val drawPoints = points.map { point ->
        Offset(
            rect.left + point.x * rect.width,
            rect.top + (1f - point.y) * rect.height
        )
    }

    path.moveTo(drawPoints[0].x, drawPoints[0].y)

    if (points.size == 2) {
        // 两点之间画直线
        path.lineTo(drawPoints[1].x, drawPoints[1].y)
    } else {
        // 使用 Catmull-Rom 样条插值
        val segments = 100
        for (i in 0 until segments) {
            val t = i.toFloat() / segments
            val pt = catmullRomInterpolate(drawPoints, t)
            path.lineTo(pt.x, pt.y)
        }
        path.lineTo(drawPoints.last().x, drawPoints.last().y)
    }

    drawPath(
        path = path,
        color = color.copy(alpha = 0.8f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Catmull-Rom 样条插值
 */
private fun catmullRomInterpolate(
    points: List<Offset>,
    t: Float
): Offset {
    val n = points.size
    if (n < 2) return points.first()

    val totalSegments = n - 1
    val segment = (t * totalSegments).toInt().coerceIn(0, totalSegments - 1)
    val localT = (t * totalSegments) - segment

    val p0 = if (segment > 0) points[segment - 1] else points[0]
    val p1 = points[segment]
    val p2 = points[min(segment + 1, n - 1)]
    val p3 = if (segment + 2 < n) points[segment + 2] else points[n - 1]

    val t2 = localT * localT
    val t3 = t2 * localT

    val x = 0.5f * (
        (2f * p1.x) +
        (-p0.x + p2.x) * localT +
        (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
        (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3
    )

    val y = 0.5f * (
        (2f * p1.y) +
        (-p0.y + p2.y) * localT +
        (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
        (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
    )

    return Offset(x, y)
}

private fun DrawScope.drawControlPoints(
    points: List<CurvePoint>,
    selectedIndex: Int,
    color: Color,
    rect: Rect
) {
    for (i in points.indices) {
        val point = points[i]
        val center = Offset(
            rect.left + point.x * rect.width,
            rect.top + (1f - point.y) * rect.height
        )

        val isSelected = i == selectedIndex
        val radius = if (isSelected) 8f else 6f

        // 外圈
        drawCircle(
            color = if (isSelected) color else Color.White.copy(alpha = 0.7f),
            radius = radius,
            center = center,
            style = Stroke(width = if (isSelected) 3f else 2f)
        )

        // 填充
        drawCircle(
            color = if (isSelected) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.3f),
            radius = radius - 2f,
            center = center
        )
    }
}

@Composable
private fun PresetCurves(
    currentChannel: CurveChannel,
    onPresetSelected: (CurvePreset) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CurvePreset.entries.size) { index ->
            val preset = CurvePreset.entries[index]
            Surface(
                modifier = Modifier.clickable { onPresetSelected(preset) },
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = preset.displayName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = preset.description,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}