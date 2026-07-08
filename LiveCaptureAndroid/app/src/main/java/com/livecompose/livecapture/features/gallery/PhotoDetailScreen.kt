package com.livecompose.livecapture.features.gallery

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.livecompose.livecapture.core.metadata.ExifData
import com.livecompose.livecapture.core.metadata.ExifReader
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.ui.design.DesignSystem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 照片详情页
 * 全屏显示照片及详细拍摄参数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photoId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val bitmap = remember(photoId) { viewModel.getFullPhoto(photoId) }
    val record = remember(photoId) {
        viewModel.records.collectAsState().value.find { it.id == photoId }
    }
    val photoFile = remember(photoId) {
        viewModel.let { vm ->
            val storage = com.livecompose.livecapture.core.storage.PhotoStorageService(context)
            storage.getPhotoFile(photoId)
        }
    }
    val exifData = remember(photoFile) {
        if (photoFile.exists()) ExifReader.readExif(photoFile.absolutePath) else null
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

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
                // 编辑按钮
                IconButton(onClick = { onEdit(photoId) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White)
                }
                // 分享按钮
                IconButton(onClick = {
                    sharePhoto(context, photoFile)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White)
                }
                // 删除按钮
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFFF3B30))
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
            // 照片展示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (record?.imageWidth != null && record?.imageHeight != null)
                        record.imageWidth.toFloat() / record.imageHeight else 3f / 4f),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    AsyncImage(
                        model = bitmap.asImageBitmap(),
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

            // 拍摄参数区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // 拍摄日期
                record?.let {
                    val dateFormat = SimpleDateFormat("yyyy年M月d日 HH:mm:ss", Locale.CHINA)
                    Text(
                        text = dateFormat.format(Date(it.creationDate)),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 拍摄参数卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1C1C1E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "拍摄参数",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 从 EXIF 优先读取，否则用 record 中的值
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ParamItem("ISO", exifData?.iso?.toString() ?: record?.iso?.toInt()?.toString() ?: "-")
                            ParamItem(
                                "快门",
                                exifData?.shutterSpeed ?: record?.shutterSpeed?.let {
                                    "1/${(1.0 / it).toInt()}s"
                                } ?: "-"
                            )
                            ParamItem(
                                "光圈",
                                exifData?.aperture ?: record?.aperture?.let { "f/%.1f".format(it) } ?: "-"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ParamItem("焦距", exifData?.focalLength ?: "-")
                            ParamItem(
                                "分辨率",
                                "${record?.imageWidth ?: exifData?.imageWidth ?: "-"}×${record?.imageHeight ?: exifData?.imageHeight ?: "-"}"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RGB 直方图卡片
                if (bitmap != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1C1C1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "RGB 直方图",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            RgbHistogram(bitmap)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // GPS 位置卡片
                if (exifData?.hasGps == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1C1C1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "GPS 位置",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "纬度: ${exifData.gpsLatitude ?: "-"}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            Text(
                                "经度: ${exifData.gpsLongitude ?: "-"}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            if (exifData.gpsAltitude != null) {
                                Text(
                                    "海拔: ${exifData.gpsAltitude}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 文件信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1C1C1E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "文件信息",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "格式: ${photoFile.extension.uppercase()}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Text(
                            "大小: ${formatFileSize(photoFile.length())}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        exifData?.cameraModel?.let {
                            Text(
                                "相机型号: $it",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                        exifData?.dateTime?.let {
                            Text(
                                "拍摄时间: $it",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                        exifData?.flash?.let {
                            Text(
                                "闪光灯: $it",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，确定要删除这张照片吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(photoId)
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text("删除", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ParamItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}

/**
 * RGB 直方图组件
 */
@Composable
private fun RgbHistogram(bitmap: Bitmap) {
    val histogram = remember(bitmap) { computeRgbHistogram(bitmap) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val maxCount = maxOf(
            histogram.red.maxOrNull() ?: 1,
            histogram.green.maxOrNull() ?: 1,
            histogram.blue.maxOrNull() ?: 1
        ).toFloat()

        val barWidth = canvasWidth / 256f

        // 绘制 RGB 通道
        for (i in 0 until 256) {
            val barHeightR = (histogram.red.getOrElse(i) { 0 } / maxCount * canvasHeight).coerceAtLeast(0.5f)
            val barHeightG = (histogram.green.getOrElse(i) { 0 } / maxCount * canvasHeight).coerceAtLeast(0.5f)
            val barHeightB = (histogram.blue.getOrElse(i) { 0 } / maxCount * canvasHeight).coerceAtLeast(0.5f)

            val x = i * barWidth

            // 红色通道
            drawRect(
                color = Color.Red.copy(alpha = 0.6f),
                topLeft = Offset(x, canvasHeight - barHeightR),
                size = Size(barWidth, barHeightR)
            )
            // 绿色通道
            drawRect(
                color = Color.Green.copy(alpha = 0.6f),
                topLeft = Offset(x, canvasHeight - barHeightG),
                size = Size(barWidth, barHeightG)
            )
            // 蓝色通道
            drawRect(
                color = Color.Blue.copy(alpha = 0.6f),
                topLeft = Offset(x, canvasHeight - barHeightB),
                size = Size(barWidth, barHeightB)
            )
        }
    }
}

/**
 * 直方图数据结构
 */
private data class HistogramData(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray
)

/**
 * 计算 RGB 直方图
 */
private fun computeRgbHistogram(bitmap: Bitmap): HistogramData {
    val red = IntArray(256)
    val green = IntArray(256)
    val blue = IntArray(256)

    val sampleBitmap = if (bitmap.width > 256 || bitmap.height > 256) {
        Bitmap.createScaledBitmap(bitmap, 256, 256, true)
    } else bitmap

    for (y in 0 until sampleBitmap.height) {
        for (x in 0 until sampleBitmap.width) {
            val pixel = sampleBitmap.getPixel(x, y)
            red[(pixel shr 16) and 0xFF]++
            green[(pixel shr 8) and 0xFF]++
            blue[pixel and 0xFF]++
        }
    }

    if (sampleBitmap !== bitmap) {
        sampleBitmap.recycle()
    }

    return HistogramData(red, green, blue)
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * 分享照片
 */
private fun sharePhoto(context: android.content.Context, photoFile: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享照片"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}