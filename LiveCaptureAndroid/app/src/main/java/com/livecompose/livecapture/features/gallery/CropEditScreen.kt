package com.livecompose.livecapture.features.gallery

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.storage.PhotoStorageService
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlin.math.max
import kotlin.math.min

/**
 * 裁剪比例预设
 */
enum class CropAspectRatio(val displayName: String, val ratio: Float) {
    ORIGINAL("原始", Float.NaN),
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_1_1("1:1", 1f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f),
    RATIO_4_3("4:3", 4f / 3f),
    XPAN("XPAN", 65f / 24f),
    RATIO_2_3("2:3", 2f / 3f)
}

/**
 * 裁剪编辑界面
 * 支持自由裁剪和多种比例预设
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropEditScreen(
    photoId: String,
    onBack: () -> Unit,
    onSave: ((Bitmap) -> Unit)? = null,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoId) {
        bitmap = viewModel.getFullPhoto(photoId)
    }
    var selectedRatio by remember { mutableStateOf(CropAspectRatio.ORIGINAL) }
    var rotation by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 裁剪区域 (归一化 0~1)
    var cropRect by remember {
        mutableStateOf(Rect(0.1f, 0.1f, 0.9f, 0.9f))
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text("裁剪", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    bitmap?.let { src ->
                        val rotated = rotateBitmap(src, rotation)
                        val cropped = cropBitmap(rotated, cropRect, selectedRatio)
                        if (onSave != null) {
                            onSave(cropped)
                        } else {
                            // 默认保存逻辑：保存到图库并返回
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val stream = java.io.ByteArrayOutputStream()
                                cropped.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                                val data = stream.toByteArray()
                                val storage = PhotoStorageService(context)
                                storage.savePhoto(data, detectionMethod = "crop")
                                stream.close()
                            }
                            onBack()
                        }
                    }
                }) {
                    Icon(Icons.Default.Check, null, tint = DesignSystem.Colors.success)
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(vertical = 12.dp)
            ) {
                // 比例选择
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CropAspectRatio.entries.forEach { ratio ->
                        FilterChip(
                            selected = selectedRatio == ratio,
                            onClick = {
                                selectedRatio = ratio
                                cropRect = calculateInitialCropRect(ratio)
                            },
                            label = { Text(ratio.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesignSystem.Colors.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 工具按钮
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { rotation = (rotation + 90) % 360 }) {
                        Icon(Icons.Default.RotateRight, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    TextButton(onClick = {
                        scale = 1f; offsetX = 0f; offsetY = 0f
                        cropRect = calculateInitialCropRect(selectedRatio)
                        rotation = 0
                    }) { Text("重置", color = Color.White.copy(alpha = 0.7f)) }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) {
                var rotatedBitmap by remember(rotation) { mutableStateOf(rotateBitmap(bmp, rotation)) }

                // 可缩放拖动的图片
                Image(
                    bitmap = rotatedBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = offsetX; translationY = offsetY
                        }
                )

                // 裁剪框叠加
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height
                    val left = cropRect.left * canvasW
                    val top = cropRect.top * canvasH
                    val right = cropRect.right * canvasW
                    val bottom = cropRect.bottom * canvasH

                    // 半透明遮罩
                    drawRect(Color.Black.copy(alpha = 0.55f))
                    // 挖空裁剪区域
                    drawRect(Color.Transparent, topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(right - left, bottom - top), blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
                    // 裁剪框
                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(right - left, bottom - top), style = Stroke(2f))
                    // 三分线
                    val thirdW = (right - left) / 3
                    val thirdH = (bottom - top) / 3
                    for (i in 1..2) {
                        drawLine(Color.White.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(left + thirdW * i, top),
                            androidx.compose.ui.geometry.Offset(left + thirdW * i, bottom), 0.5f)
                        drawLine(Color.White.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(left, top + thirdH * i),
                            androidx.compose.ui.geometry.Offset(right, top + thirdH * i), 0.5f)
                    }
                }
            } else {
                Text("无法加载照片", color = Color.White)
            }
        }
    }
}

private fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return source
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun cropBitmap(source: Bitmap, cropRect: Rect, ratio: CropAspectRatio): Bitmap {
    val effectiveRatio = if (ratio.ratio.isNaN()) source.width.toFloat() / source.height else ratio.ratio
    var cropW = source.width
    var cropH = source.height
    val currentRatio = cropW.toFloat() / cropH

    if (currentRatio > effectiveRatio) {
        cropW = (cropH * effectiveRatio).toInt()
    } else {
        cropH = (cropW / effectiveRatio).toInt()
    }

    val startX = ((source.width - cropW) / 2f * (cropRect.left + (1f - cropRect.right)) / (1f - (cropRect.right - cropRect.left))).toInt().coerceIn(0, source.width - cropW)
    val startY = ((source.height - cropH) / 2f * (cropRect.top + (1f - cropRect.bottom)) / (1f - (cropRect.bottom - cropRect.top))).toInt().coerceIn(0, source.height - cropH)

    return Bitmap.createBitmap(source, startX, startY, cropW.coerceAtMost(source.width - startX), cropH.coerceAtMost(source.height - startY))
}

private fun calculateInitialCropRect(ratio: CropAspectRatio): Rect {
    if (ratio.ratio.isNaN()) return Rect(0.05f, 0.05f, 0.95f, 0.95f)
    val r = ratio.ratio
    return if (r >= 1f) {
        val h = 0.9f / r
        Rect(0.05f, 0.5f - h / 2, 0.95f, 0.5f + h / 2)
    } else {
        val w = 0.9f * r
        Rect(0.5f - w / 2, 0.05f, 0.5f + w / 2, 0.95f)
    }
}
