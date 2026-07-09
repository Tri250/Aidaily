package com.livecompose.livecapture.features.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.editing.CurveChannel
import com.livecompose.livecapture.core.editing.CurveControlPoint
import com.livecompose.livecapture.core.editing.CurveParams
import com.livecompose.livecapture.core.editing.CurvePreset
import com.livecompose.livecapture.core.editing.CurveProcessor
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 通道对应的高亮颜色（与 iOS 端 channel.color 对齐）
 * 顺序与 [CurveChannel] 枚举一致：RGB / R / G / B
 */
private val channelColors = listOf(
    Color.White,
    Color.Red,
    Color.Green,
    Color.Blue
)

/**
 * 曲线编辑器状态
 *
 * 使用 MutableStateFlow 持有四个通道的控制点与当前选中通道，
 * 对外暴露 StateFlow 供 Compose 订阅。
 */
class CurveEditorState(initial: CurveParams = CurveParams()) {
    private val _master = MutableStateFlow(initial.master)
    private val _red = MutableStateFlow(initial.red)
    private val _green = MutableStateFlow(initial.green)
    private val _blue = MutableStateFlow(initial.blue)
    private val _selectedChannel = MutableStateFlow(CurveChannel.RGB)

    val master: StateFlow<List<CurveControlPoint>> = _master.asStateFlow()
    val red: StateFlow<List<CurveControlPoint>> = _red.asStateFlow()
    val green: StateFlow<List<CurveControlPoint>> = _green.asStateFlow()
    val blue: StateFlow<List<CurveControlPoint>> = _blue.asStateFlow()
    val selectedChannel: StateFlow<CurveChannel> = _selectedChannel.asStateFlow()

    /** 获取指定通道的控制点 */
    fun pointsOf(channel: CurveChannel): List<CurveControlPoint> = when (channel) {
        CurveChannel.RGB -> _master.value
        CurveChannel.RED -> _red.value
        CurveChannel.GREEN -> _green.value
        CurveChannel.BLUE -> _blue.value
    }

    /** 更新指定通道的控制点（用于拖拽） */
    fun updatePoints(channel: CurveChannel, points: List<CurveControlPoint>) {
        val sorted = points.sortedBy { it.x }
        if (sorted.isNotEmpty()) {
            val first = sorted.first().copy(x = 0f)
            val last = sorted.last().copy(x = 1f)
            val result = if (sorted.size > 1) {
                listOf(first) + sorted.subList(1, sorted.size - 1) + listOf(last)
            } else {
                listOf(first)
            }
            when (channel) {
                CurveChannel.RGB -> _master.value = result
                CurveChannel.RED -> _red.value = result
                CurveChannel.GREEN -> _green.value = result
                CurveChannel.BLUE -> _blue.value = result
            }
        }
    }

    fun selectChannel(channel: CurveChannel) {
        _selectedChannel.value = channel
    }

    /** 应用预设到指定通道 */
    fun applyPreset(channel: CurveChannel, preset: CurvePreset) {
        when (channel) {
            CurveChannel.RGB -> _master.value = preset.controlPoints()
            CurveChannel.RED -> _red.value = preset.controlPoints()
            CurveChannel.GREEN -> _green.value = preset.controlPoints()
            CurveChannel.BLUE -> _blue.value = preset.controlPoints()
        }
    }

    /** 重置所有通道为线性 */
    fun resetAll() {
        val linear = CurvePreset.LINEAR.controlPoints()
        _master.value = linear
        _red.value = linear
        _green.value = linear
        _blue.value = linear
    }

    /** 构造当前曲线参数 */
    fun toParams(): CurveParams = CurveParams(
        master = _master.value,
        red = _red.value,
        green = _green.value,
        blue = _blue.value
    )
}

/**
 * 曲线编辑器页面
 *
 * 对应 iOS 端 CurveEditorView，提供 RGB/红/绿/蓝四通道交互式曲线编辑，
 * 支持拖拽控制点、预设切换、实时预览。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurveEditorScreen(
    photoId: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { CurveProcessor() }
    val state = remember { CurveEditorState() }

    // 加载原图（预览用降采样以保持流畅）
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoId) {
        val full = withContext(Dispatchers.IO) { viewModel.getFullPhoto(photoId) }
        fullBitmap = full
        originalBitmap = full?.let { downscaleForPreview(it, 1080) }
    }

    // 订阅状态
    val master by state.master.collectAsState()
    val red by state.red.collectAsState()
    val green by state.green.collectAsState()
    val blue by state.blue.collectAsState()
    val selectedChannel by state.selectedChannel.collectAsState()

    val params = remember(master, red, green, blue) { state.toParams() }
    val hasChanges = remember(params) { !params.isDefault }

    // 实时预览：参数变化时重新处理（LaunchedEffect 自动取消旧任务，实现节流）
    var adjustedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processing by remember { mutableStateOf(false) }
    LaunchedEffect(originalBitmap, params) {
        val src = originalBitmap
        if (src == null) {
            adjustedBitmap = null
        } else if (!hasChanges) {
            adjustedBitmap = src
        } else {
            processing = true
            // 切换到 Default 调度器执行全像素遍历，避免阻塞主线程
            adjustedBitmap = withContext(Dispatchers.Default) {
                processor.process(src, params)
            }
            processing = false
        }
    }

    // 保存
    fun save() {
        val src = fullBitmap ?: return
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                if (hasChanges) processor.process(src, params) else src
            }
            withContext(Dispatchers.IO) {
                val stream = ByteArrayOutputStream()
                result.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                val data = stream.toByteArray()
                com.livecompose.livecapture.core.storage.PhotoStorageService(context)
                    .updatePhoto(photoId, data, detectionMethod = "curve")
                stream.close()
            }
            withContext(Dispatchers.Main) {
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DesignSystem.Colors.minimalBackground)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text("曲线编辑", color = Color.White, style = DesignSystem.Typography.headline)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { state.resetAll() }) {
                    Icon(Icons.Default.Refresh, "重置", tint = Color.White)
                }
                IconButton(onClick = { save() }, enabled = originalBitmap != null) {
                    Icon(Icons.Default.Check, "应用", tint = Color.White)
                }
            }
        },
        containerColor = DesignSystem.Colors.minimalBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 预览图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
                contentAlignment = Alignment.Center
            ) {
                adjustedBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: CircularProgressIndicator(color = DesignSystem.Colors.primary)
                if (processing) {
                    CircularProgressIndicator(
                        color = DesignSystem.Colors.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 通道选择器
            ChannelSelector(
                selected = selectedChannel,
                onSelect = { state.selectChannel(it) }
            )

            Spacer(Modifier.height(8.dp))

            // 曲线编辑区
            CurveEditorCanvas(
                channel = selectedChannel,
                points = state.pointsOf(selectedChannel),
                processor = processor,
                onPointsChange = { state.updatePoints(selectedChannel, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(240.dp)
            )

            Spacer(Modifier.height(12.dp))

            // 预设
            PresetRow(
                selectedChannel = selectedChannel,
                onApplyPreset = { state.applyPreset(selectedChannel, it) }
            )

            Spacer(Modifier.height(16.dp))

            // 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { state.resetAll() },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = DesignSystem.mediumRoundedShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignSystem.Colors.minimalBorder)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("重置曲线", style = DesignSystem.Typography.subheadline)
                }
                Button(
                    onClick = { save() },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = DesignSystem.mediumRoundedShape,
                    colors = ButtonDefaults.buttonColors(containerColor = DesignSystem.Colors.primary),
                    enabled = originalBitmap != null
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("应用", style = DesignSystem.Typography.subheadline)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * 通道选择器
 */
@Composable
private fun ChannelSelector(
    selected: CurveChannel,
    onSelect: (CurveChannel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CurveChannel.entries.forEachIndexed { idx, channel ->
            val color = channelColors[idx]
            val isSelected = selected == channel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) color.copy(alpha = 0.2f) else DesignSystem.Colors.minimalOverlay)
                    .border(
                        1.dp,
                        if (isSelected) color else Color.Transparent,
                        RoundedCornerShape(50)
                    )
                    .clickableNoRipple { onSelect(channel) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    channel.displayName,
                    color = if (isSelected) color else DesignSystem.Colors.minimalSecondaryLabel,
                    style = DesignSystem.Typography.caption1,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 曲线编辑画布
 *
 * 绘制 4x4 网格、对角参考线、填充区域、平滑曲线（采样自 [CurveProcessor.sampleCurve]，
 * 保证与 LUT 一致）及可拖拽控制点。拖拽通过 [detectDragGestures] 实现。
 */
@Composable
private fun CurveEditorCanvas(
    channel: CurveChannel,
    points: List<CurveControlPoint>,
    processor: CurveProcessor,
    onPointsChange: (List<CurveControlPoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    val channelColor = channelColors[channel.ordinal]
    val dragIndex = remember { mutableStateOf<Int?>(null) }

    // 使用 rememberUpdatedState 保证拖拽过程中始终读取最新的 points / 回调，
    // 同时 pointerInput 使用稳定 key，避免 points 变化重启手势检测导致拖拽中断。
    val currentPoints by rememberUpdatedState(points)
    val currentOnPointsChange by rememberUpdatedState(onPointsChange)

    // 采样曲线点（与 LUT 一致）
    val samples = remember(points) { processor.sampleCurve(points, 128) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DesignSystem.Colors.gray1())
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            // 找到最近的控制点
                            var nearest = -1
                            var minDist = Float.MAX_VALUE
                            currentPoints.forEachIndexed { i, p ->
                                val px = p.x * w
                                val py = (1f - p.y) * h
                                val d = (px - offset.x) * (px - offset.x) + (py - offset.y) * (py - offset.y)
                                if (d < minDist) {
                                    minDist = d
                                    nearest = i
                                }
                            }
                            // 命中阈值（48px 半径）
                            if (minDist < 48f * 48f) dragIndex.value = nearest
                        },
                        onDrag = { change, _ ->
                            val idx = dragIndex.value
                            val pts = currentPoints
                            if (idx != null && idx in pts.indices) {
                                change.consume()
                                val nx = (change.position.x / w).coerceIn(0f, 1f)
                                val ny = (1f - change.position.y / h).coerceIn(0f, 1f)
                                // 首尾点不允许移动 x（保持 0/1）
                                val fixedX = when (idx) {
                                    0 -> 0f
                                    pts.lastIndex -> 1f
                                    else -> nx
                                }
                                val newPoints = pts.toMutableList()
                                newPoints[idx] = CurveControlPoint(fixedX, ny)
                                currentOnPointsChange(newPoints)
                            }
                        },
                        onDragEnd = { dragIndex.value = null },
                        onDragCancel = { dragIndex.value = null }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val gridColor = DesignSystem.Colors.minimalOverlay
            val refColor = DesignSystem.Colors.minimalOverlay

            // 网格
            for (i in 1 until 4) {
                val x = w * i / 4f
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), 0.5f)
                val y = h * i / 4f
                drawLine(gridColor, Offset(0f, y), Offset(w, y), 0.5f)
            }
            // 对角参考线
            drawLine(refColor, Offset(0f, h), Offset(w, 0f), 0.5f)

            // 填充区域
            val fillPath = Path().apply {
                moveTo(0f, h)
                moveTo(0f, (1f - samples[0]) * h)
                for (i in 1 until samples.size) {
                    lineTo((i / (samples.size - 1).toFloat()) * w, (1f - samples[i]) * h)
                }
                lineTo(w, h)
                close()
            }
            drawPath(fillPath, channelColor.copy(alpha = 0.08f))

            // 曲线
            val curvePath = Path().apply {
                moveTo(0f, (1f - samples[0]) * h)
                for (i in 1 until samples.size) {
                    lineTo((i / (samples.size - 1).toFloat()) * w, (1f - samples[i]) * h)
                }
            }
            drawPath(
                curvePath,
                color = channelColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 控制点
            points.forEachIndexed { i, p ->
                val px = p.x * w
                val py = (1f - p.y) * h
                val isDragging = dragIndex.value == i
                val radius = if (isDragging) 9f else 7f
                drawCircle(Color.White, radius = radius + 2f, center = Offset(px, py))
                drawCircle(channelColor, radius = radius, center = Offset(px, py))
            }
        }
    }
}

/**
 * 预设行
 */
@Composable
private fun PresetRow(
    selectedChannel: CurveChannel,
    onApplyPreset: (CurvePreset) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CurvePreset.entries) { preset ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, DesignSystem.Colors.minimalBorder, RoundedCornerShape(50))
                    .clickableNoRipple { onApplyPreset(preset) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    preset.displayName,
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    style = DesignSystem.Typography.caption2
                )
            }
        }
    }
}

/**
 * 预览降采样：限制最大边长，保持比例
 */
private fun downscaleForPreview(src: Bitmap, maxDim: Int): Bitmap {
    val w = src.width
    val h = src.height
    val longest = maxOf(w, h)
    if (longest <= maxDim) return src
    val scale = maxDim.toFloat() / longest
    val nw = (w * scale).toInt().coerceAtLeast(1)
    val nh = (h * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(src, nw, nh, true)
}

/**
 * 无涟漪点击修饰
 */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = { onClick() })
    }
)
