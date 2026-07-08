package com.livecompose.livecapture.features.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.editing.HslChannel
import com.livecompose.livecapture.core.editing.HslParams
import com.livecompose.livecapture.core.editing.HslProcessor
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
 * HSL 调整状态
 *
 * 使用 MutableStateFlow 持有 8 通道的色相/饱和度/明度数组及当前选中通道。
 * 数组每次更新都创建新引用以保证 StateFlow 能感知变化。
 */
class HslAdjustState {
    private val _hue = MutableStateFlow(FloatArray(8))
    private val _saturation = MutableStateFlow(FloatArray(8))
    private val _lightness = MutableStateFlow(FloatArray(8))
    private val _selected = MutableStateFlow(HslChannel.RED)

    val hue: StateFlow<FloatArray> = _hue.asStateFlow()
    val saturation: StateFlow<FloatArray> = _saturation.asStateFlow()
    val lightness: StateFlow<FloatArray> = _lightness.asStateFlow()
    val selected: StateFlow<HslChannel> = _selected.asStateFlow()

    fun select(channel: HslChannel) {
        _selected.value = channel
    }

    /** 更新当前选中通道的色相 */
    fun setHue(value: Float) {
        val idx = _selected.value.index
        _hue.value = _hue.value.copyOf().also { it[idx] = value }
    }

    /** 更新当前选中通道的饱和度 */
    fun setSaturation(value: Float) {
        val idx = _selected.value.index
        _saturation.value = _saturation.value.copyOf().also { it[idx] = value }
    }

    /** 更新当前选中通道的明度 */
    fun setLightness(value: Float) {
        val idx = _selected.value.index
        _lightness.value = _lightness.value.copyOf().also { it[idx] = value }
    }

    fun resetAll() {
        _hue.value = FloatArray(8)
        _saturation.value = FloatArray(8)
        _lightness.value = FloatArray(8)
    }

    fun toParams(): HslParams = HslParams(
        hue = _hue.value,
        saturation = _saturation.value,
        lightness = _lightness.value
    )
}

/**
 * HSL 调整页面
 *
 * 对应 iOS 端 HSLAdjustView，提供 8 种颜色独立调整色相/饱和度/明度，
 * 支持颜色通道选择、滑块调整与实时预览。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HslAdjustScreen(
    photoId: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { HslProcessor() }
    val state = remember { HslAdjustState() }

    // 加载原图
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoId) {
        val full = withContext(Dispatchers.IO) { viewModel.getFullPhoto(photoId) }
        fullBitmap = full
        originalBitmap = full?.let { downscaleForPreview(it, 1080) }
    }

    // 订阅状态
    val hueArr by state.hue.collectAsState()
    val satArr by state.saturation.collectAsState()
    val lightArr by state.lightness.collectAsState()
    val selected by state.selected.collectAsState()

    // 当前通道的滑块值
    val currentHue = hueArr[selected.index]
    val currentSat = satArr[selected.index]
    val currentLight = lightArr[selected.index]

    val params = remember(hueArr, satArr, lightArr) { state.toParams() }
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
            // 切换到 Default 调度器执行全像素 HSV 转换，避免阻塞主线程
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
                    .savePhoto(data, detectionMethod = "hsl")
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
                Text("HSL 调整", color = Color.White, style = DesignSystem.Typography.headline)
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

            Spacer(Modifier.height(12.dp))

            // 颜色通道选择
            ColorChannelSelector(
                selected = selected,
                onSelect = { state.select(it) }
            )

            Spacer(Modifier.height(16.dp))

            // 滑块区域
            val tintColor = Color(selected.argb)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HslSlider(
                    label = "色相",
                    value = currentHue,
                    valueRange = -0.5f..0.5f,
                    tintColor = tintColor,
                    onValueChange = { state.setHue(it) }
                )
                HslSlider(
                    label = "饱和度",
                    value = currentSat,
                    valueRange = -1f..1f,
                    tintColor = tintColor,
                    onValueChange = { state.setSaturation(it) }
                )
                HslSlider(
                    label = "明度",
                    value = currentLight,
                    valueRange = -1f..1f,
                    tintColor = tintColor,
                    onValueChange = { state.setLightness(it) }
                )
            }

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
                    Text("重置所有 HSL", style = DesignSystem.Typography.subheadline)
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
 * 颜色通道选择器（8 个色环）
 */
@Composable
private fun ColorChannelSelector(
    selected: HslChannel,
    onSelect: (HslChannel) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(HslChannel.entries) { channel ->
            val isSelected = selected == channel
            val color = Color(channel.argb)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = color.copy(alpha = 0.3f),
                            spotColor = color.copy(alpha = 0.3f)
                        )
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickableNoRipple { onSelect(channel) }
                )
                Text(
                    channel.name,
                    color = if (isSelected) Color.White else DesignSystem.Colors.minimalSecondaryLabel,
                    style = DesignSystem.Typography.caption2
                )
            }
        }
    }
}

/**
 * HSL 滑块组件
 */
@Composable
private fun HslSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    tintColor: Color,
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
                modifier = Modifier.width(48.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = tintColor,
                activeTrackColor = tintColor,
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
        detectTapGestures(onTap = { onClick() })
    }
)
