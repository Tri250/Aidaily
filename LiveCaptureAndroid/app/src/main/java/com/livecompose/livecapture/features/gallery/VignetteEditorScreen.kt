package com.livecompose.livecapture.features.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.editing.VignetteParams
import com.livecompose.livecapture.core.editing.VignetteProcessor
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
 * 暗角编辑状态
 *
 * 使用 MutableStateFlow 持有强度、范围（中点）、羽化、圆度（形状）。
 * 对应 iOS 端 VignetteEditorView 的参数。
 */
class VignetteEditorState {
    private val _intensity = MutableStateFlow(0f)
    private val _midpoint = MutableStateFlow(0.5f)
    private val _feather = MutableStateFlow(0.5f)
    private val _roundness = MutableStateFlow(true) // true=圆形，false=椭圆

    val intensity: StateFlow<Float> = _intensity.asStateFlow()
    val midpoint: StateFlow<Float> = _midpoint.asStateFlow()
    val feather: StateFlow<Float> = _feather.asStateFlow()
    val roundness: StateFlow<Boolean> = _roundness.asStateFlow()

    fun setIntensity(value: Float) { _intensity.value = value }
    fun setMidpoint(value: Float) { _midpoint.value = value }
    fun setFeather(value: Float) { _feather.value = value }
    fun setRoundness(value: Boolean) { _roundness.value = value }

    fun resetAll() {
        _intensity.value = 0f
        _midpoint.value = 0.5f
        _feather.value = 0.5f
        _roundness.value = true
    }

    fun toParams(): VignetteParams = VignetteParams(
        intensity = _intensity.value,
        midpoint = _midpoint.value,
        feather = _feather.value,
        roundness = _roundness.value
    )
}

/**
 * 暗角编辑页面
 *
 * 对应 iOS 端 VignetteEditorView，提供强度、范围、羽化三个滑块，
 * 支持圆形/椭圆形状切换与实时预览。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VignetteEditorScreen(
    photoId: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { VignetteProcessor() }
    val state = remember { VignetteEditorState() }

    // 加载原图
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoId) {
        val full = withContext(Dispatchers.IO) { viewModel.getFullPhoto(photoId) }
        fullBitmap = full
        originalBitmap = full?.let { downscaleForPreview(it, 1080) }
    }

    // 订阅状态
    val intensity by state.intensity.collectAsState()
    val midpoint by state.midpoint.collectAsState()
    val feather by state.feather.collectAsState()
    val roundness by state.roundness.collectAsState()

    val params = remember(intensity, midpoint, feather, roundness) { state.toParams() }
    val hasChanges = remember(params) { !params.isDefault }

    // 实时预览
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
            // 切换到 Default 调度器执行全像素 RadialGradient 叠加，避免阻塞主线程
            adjustedBitmap = withContext(Dispatchers.Default) {
                processor.process(src, params)
            }
            processing = false
        }
    }

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
                    .savePhoto(data, detectionMethod = "vignette")
                stream.close()
            }
            withContext(Dispatchers.Main) { onBack() }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text("暗角编辑", color = Color.White, style = DesignSystem.Typography.headline)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { state.resetAll() }) {
                    Icon(Icons.Default.Refresh, "重置", tint = Color.White)
                }
                IconButton(onClick = { save() }, enabled = originalBitmap != null) {
                    Icon(Icons.Default.Check, "应用", tint = Color.White)
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 预览图（叠加暗角遮罩预览）
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

            Spacer(Modifier.height(12.dp))

            // 暗角遮罩示意预览
            VignettePreviewCanvas(
                intensity = intensity,
                midpoint = midpoint,
                feather = feather,
                roundness = roundness,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.height(16.dp))

            // 滑块区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VignetteSlider(
                    label = "强度",
                    value = intensity,
                    valueRange = 0f..1f,
                    onValueChange = { state.setIntensity(it) }
                )
                VignetteSlider(
                    label = "范围",
                    value = midpoint,
                    valueRange = 0f..1f,
                    onValueChange = { state.setMidpoint(it) }
                )
                VignetteSlider(
                    label = "羽化",
                    value = feather,
                    valueRange = 0f..1f,
                    onValueChange = { state.setFeather(it) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 形状切换
            ShapeToggle(
                roundness = roundness,
                onRound = { state.setRoundness(true) },
                onEllipse = { state.setRoundness(false) }
            )

            Spacer(Modifier.height(20.dp))

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
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("重置暗角", style = DesignSystem.Typography.subheadline)
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
 * 暗角遮罩示意画布
 *
 * 以灰度渐变背景叠加径向暗角遮罩，直观反映当前参数下的暗角形状。
 */
@Composable
private fun VignettePreviewCanvas(
    intensity: Float,
    midpoint: Float,
    feather: Float,
    roundness: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color(0xFF1C1C1E))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 背景灰度渐变
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Gray.copy(alpha = 0.8f), Color.Gray.copy(alpha = 0.3f)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // 径向暗角遮罩
            val cx = w / 2f
            val cy = h / 2f
            val maxDim = maxOf(w, h)
            val outerRadius = maxDim * 1.5f
            val innerRadius = outerRadius * midpoint.coerceIn(0f, 1f)
            val maxAlpha = intensity.coerceIn(0f, 1f)
            val stopInner = (innerRadius / outerRadius).coerceIn(0f, 0.99f)
            val stopOuter = minOf(1f, stopInner + feather.coerceIn(0.01f, 1f) * (1f - stopInner))

            val colors = listOf(
                Color.Black.copy(alpha = 0f),
                Color.Black.copy(alpha = 0f),
                Color.Black.copy(alpha = maxAlpha)
            )
            val stops = listOf(0f, stopInner, stopOuter)

            val scaleY = if (roundness) 1f else 0.7f
            val brush = Brush.radialGradient(
                colors = colors,
                stops = stops.toFloatArray(),
                center = Offset(cx, cy),
                radius = outerRadius
            )
            if (scaleY < 1f) {
                // 椭圆暗角：以中心为锚点纵向压缩渐变绘制
                scale(
                    scaleX = 1f,
                    scaleY = scaleY,
                    pivot = Offset(cx, cy)
                ) {
                    drawRect(brush)
                }
            } else {
                drawRect(brush)
            }
        }
    }
}

/**
 * 形状切换组件
 */
@Composable
private fun ShapeToggle(
    roundness: Boolean,
    onRound: () -> Unit,
    onEllipse: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        // 圆形
        ShapeButton(
            label = "圆形",
            selected = roundness,
            shape = CircleShape,
            modifier = Modifier.size(32.dp),
            onClick = onRound
        )
        // 椭圆
        ShapeButton(
            label = "椭圆",
            selected = !roundness,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(32.dp, 22.dp),
            onClick = onEllipse
        )
    }
}

@Composable
private fun ShapeButton(
    label: String,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = modifier
                .clip(shape)
                .border(
                    2.dp,
                    if (selected) DesignSystem.Colors.primary else DesignSystem.Colors.minimalBorder,
                    shape
                )
                .clickableNoRipple { onClick() }
        )
        Text(
            label,
            color = if (selected) Color.White else DesignSystem.Colors.minimalSecondaryLabel,
            style = DesignSystem.Typography.caption2
        )
    }
}

/**
 * 暗角滑块组件
 */
@Composable
private fun VignetteSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, style = DesignSystem.Typography.callout)
            Text(
                String.format("%.2f", value),
                color = DesignSystem.Colors.minimalSecondaryLabel,
                style = DesignSystem.Typography.monoDigit,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = DesignSystem.Colors.primary,
                activeTrackColor = DesignSystem.Colors.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
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
        androidx.compose.foundation.gestures.detectTapGestures(onTap = { onClick() })
    }
)
