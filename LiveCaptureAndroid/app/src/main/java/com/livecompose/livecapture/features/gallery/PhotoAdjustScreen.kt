package com.livecompose.livecapture.features.gallery

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 照片调整页面
 * 支持亮度、对比度、饱和度实时调整
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAdjustScreen(
    photoId: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 加载原图
    val originalBitmap = remember(photoId) { viewModel.getFullPhoto(photoId) }

    // 调整参数
    var brightness by remember { mutableFloatStateOf(0f) }     // -100 ~ 100
    var contrast by remember { mutableFloatStateOf(1f) }       // 0.5 ~ 2.0
    var saturation by remember { mutableFloatStateOf(1f) }     // 0 ~ 2

    // 实时计算调整后的 Bitmap
    val adjustedBitmap by remember {
        derivedStateOf {
            val src = originalBitmap ?: return@derivedStateOf null
            if (brightness == 0f && contrast == 1f && saturation == 1f) {
                src
            } else {
                applyAdjustments(src, brightness, contrast, saturation)
            }
        }
    }

    val hasChanges = brightness != 0f || contrast != 1f || saturation != 1f

    // 保存调整后的照片
    fun saveAdjustment() {
        val bitmap = adjustedBitmap ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                val data = stream.toByteArray()
                val storage = com.livecompose.livecapture.core.storage.PhotoStorageService(context)
                storage.savePhoto(data, detectionMethod = "adjust")
                stream.close()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "调整已保存", Toast.LENGTH_SHORT).show()
                onBack()
            }
        }
    }

    // 重置调整参数
    fun resetAdjustment() {
        brightness = 0f
        contrast = 1f
        saturation = 1f
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
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "照片调整",
                    color = Color.White,
                    style = DesignSystem.Typography.headline
                )
                Spacer(modifier = Modifier.weight(1f))
                // 重置按钮
                IconButton(onClick = { resetAdjustment() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "重置", tint = Color.White)
                }
                // 应用按钮
                IconButton(
                    onClick = { saveAdjustment() },
                    enabled = adjustedBitmap != null && hasChanges
                ) {
                    Icon(Icons.Default.Check, contentDescription = "应用", tint = Color.White)
                }
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 照片展示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = adjustedBitmap
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DesignSystem.Colors.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 调整参数面板
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "参数调整",
                        color = Color.White,
                        style = DesignSystem.Typography.title3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 亮度滑块
                    AdjustSlider(
                        label = "亮度",
                        value = brightness,
                        valueRange = -100f..100f,
                        onValueChange = { brightness = it },
                        valueLabel = brightness.toInt().toString()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 对比度滑块
                    AdjustSlider(
                        label = "对比度",
                        value = contrast,
                        valueRange = 0.5f..2.0f,
                        onValueChange = { contrast = it },
                        valueLabel = String.format("%.2f", contrast)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 饱和度滑块
                    AdjustSlider(
                        label = "饱和度",
                        value = saturation,
                        valueRange = 0f..2f,
                        onValueChange = { saturation = it },
                        valueLabel = String.format("%.2f", saturation)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 重置按钮
                        OutlinedButton(
                            onClick = { resetAdjustment() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = DesignSystem.mediumRoundedShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("重置", style = DesignSystem.Typography.subheadline)
                        }

                        // 应用保存按钮
                        Button(
                            onClick = { saveAdjustment() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = DesignSystem.mediumRoundedShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DesignSystem.Colors.primary
                            ),
                            enabled = adjustedBitmap != null && hasChanges
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("应用", style = DesignSystem.Typography.subheadline)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 调整滑块组件
 */
@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Color.White,
                style = DesignSystem.Typography.subheadline
            )
            Text(
                valueLabel,
                color = DesignSystem.Colors.primary,
                style = DesignSystem.Typography.monoCaption
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
 * 对 Bitmap 应用亮度、对比度、饱和度调整
 * 通过像素级操作实现
 */
private fun applyAdjustments(
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float
): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val outputPixels = IntArray(width * height)

    // 亮度偏移量（-100~100 映射到 -128~128）
    val brightnessOffset = brightness * 1.28f

    for (i in pixels.indices) {
        val pixel = pixels[i]
        var r = ((pixel shr 16) and 0xFF) / 255f
        var g = ((pixel shr 8) and 0xFF) / 255f
        var b = (pixel and 0xFF) / 255f

        // 亮度调整
        if (brightnessOffset != 0f) {
            val offset = brightnessOffset / 255f
            r = (r + offset).coerceIn(0f, 1f)
            g = (g + offset).coerceIn(0f, 1f)
            b = (b + offset).coerceIn(0f, 1f)
        }

        // 对比度调整
        if (contrast != 1f) {
            r = ((r - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
            g = ((g - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
            b = ((b - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
        }

        // 饱和度调整
        if (saturation != 1f) {
            val gray = 0.299f * r + 0.587f * g + 0.114f * b
            r = (gray + (r - gray) * saturation).coerceIn(0f, 1f)
            g = (gray + (g - gray) * saturation).coerceIn(0f, 1f)
            b = (gray + (b - gray) * saturation).coerceIn(0f, 1f)
        }

        val outR = (r * 255f).toInt().coerceIn(0, 255)
        val outG = (g * 255f).toInt().coerceIn(0, 255)
        val outB = (b * 255f).toInt().coerceIn(0, 255)
        outputPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
    }

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    result.setPixels(outputPixels, 0, width, 0, 0, width, height)
    return result
}
