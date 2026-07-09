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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.foundation.Image
import com.livecompose.livecapture.core.metadata.ExifData
import com.livecompose.livecapture.core.metadata.ExifReader
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.core.intelligence.ImageQualityAssessor
import com.livecompose.livecapture.core.intelligence.EnhancementAdvisor
import com.livecompose.livecapture.core.intelligence.QualityGrade
import com.livecompose.livecapture.core.intelligence.LightAnalysis
import com.livecompose.livecapture.core.intelligence.SceneType
import com.livecompose.livecapture.core.intelligence.CompositionAnalysis
import com.livecompose.livecapture.features.gallery.AIEditScreen
import com.livecompose.livecapture.ui.design.DesignSystem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
    onAdjust: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoId) {
        bitmap = viewModel.getFullPhoto(photoId)
    }
    val records by viewModel.records.collectAsState()
    val record = remember(photoId, records) {
        records.find { it.id == photoId }
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
    var showAIEdit by remember { mutableStateOf(false) }

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
                // AI编辑按钮
                IconButton(onClick = { showAIEdit = true }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI编辑", tint = DesignSystem.Colors.primary)
                }
                // 调整按钮
                IconButton(onClick = { onAdjust(photoId) }) {
                    Icon(Icons.Default.Tune, contentDescription = "调整", tint = Color.White)
                }
                // 分享按钮
                IconButton(onClick = {
                    sharePhoto(context, photoFile)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White)
                }
                // 删除按钮
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = DesignSystem.Colors.error)
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
            // 照片展示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (record?.imageWidth != null && record?.imageHeight != null)
                        record.imageWidth.toFloat() / record.imageHeight else 3f / 4f),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    val bmp = bitmap!!
                    Image(
                        bitmap = bmp.asImageBitmap(),
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
                        containerColor = DesignSystem.Colors.gray1()
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
                    val bmp = bitmap
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DesignSystem.Colors.gray1()
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
                            if (bmp != null) RgbHistogram(bmp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // GPS 位置卡片
                if (exifData?.hasGps == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DesignSystem.Colors.gray1()
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
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                fontSize = 13.sp
                            )
                            Text(
                                "经度: ${exifData.gpsLongitude ?: "-"}",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                fontSize = 13.sp
                            )
                            if (exifData.gpsAltitude != null) {
                                Text(
                                    "海拔: ${exifData.gpsAltitude}",
                                    color = DesignSystem.Colors.minimalSecondaryLabel,
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
                        containerColor = DesignSystem.Colors.gray1()
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
                            color = DesignSystem.Colors.minimalSecondaryLabel,
                            fontSize = 13.sp
                        )
                        Text(
                            "大小: ${formatFileSize(photoFile.length())}",
                            color = DesignSystem.Colors.minimalSecondaryLabel,
                            fontSize = 13.sp
                        )
                        exifData?.cameraModel?.let {
                            Text(
                                "相机型号: $it",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                fontSize = 13.sp
                            )
                        }
                        exifData?.dateTime?.let {
                            Text(
                                "拍摄时间: $it",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                fontSize = 13.sp
                            )
                        }
                        exifData?.flash?.let {
                            Text(
                                "闪光灯: $it",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AI 质量评估卡片
                if (bitmap != null) {
                    AIQualityAssessmentCard(bitmap!!, context)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // AI编辑界面
    if (showAIEdit) {
        AIEditScreen(
            photoId = photoId,
            sourceBitmap = bitmap,
            onBack = { showAIEdit = false }
        )
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
                    Text("删除", color = DesignSystem.Colors.error)
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
            color = DesignSystem.Colors.minimalSecondaryLabel,
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
 * AI 质量评估卡片
 */
@Composable
private fun AIQualityAssessmentCard(bitmap: Bitmap, context: android.content.Context) {
    val appContainer = remember { AppContainer.getInstance(context) }
    val assessor = remember { appContainer.imageQualityAssessor }
    val advisor = remember { EnhancementAdvisor() }

    val pixels = remember(bitmap) {
        val scaled = if (bitmap.width > 256 || bitmap.height > 256) {
            Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        } else bitmap
        val arr = IntArray(scaled.width * scaled.height)
        scaled.getPixels(arr, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        if (scaled !== bitmap) scaled.recycle()
        arr
    }

    val quality = remember(bitmap) {
        assessor.assessQuality(pixels, bitmap.width, bitmap.height)
    }

    val composition = remember(bitmap) {
        try {
            assessor.analyzeComposition(bitmap)
        } catch (e: Exception) {
            null
        }
    }

    val enhancements = remember(bitmap) {
        try {
            val light = LightAnalysis.DEFAULT
            val scene = SceneType.UNKNOWN
            advisor.generateSuggestions(quality, scene, light)
        } catch (e: Exception) {
            emptyList()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.gray1()),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = DesignSystem.Colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI 画质评估",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 质量等级
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("综合评分: ", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 13.sp)
                Text(
                    "${quality.overallScore.roundToInt()}分",
                    color = when (quality.qualityGrade) {
                        QualityGrade.EXCELLENT -> DesignSystem.Colors.success
                        QualityGrade.GOOD -> DesignSystem.Colors.success
                        QualityGrade.FAIR -> DesignSystem.Colors.warning
                        QualityGrade.POOR -> DesignSystem.Colors.error
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    quality.qualityGrade.displayName,
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 各维度进度条
            QualityBar("锐度", quality.sharpnessScore)
            QualityBar("噪声", quality.noiseLevel)
            QualityBar("曝光", quality.exposureScore)
            QualityBar("色彩", quality.colorHarmonyScore)

            // 构图分析
            if (composition != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "AI 构图分析",
                    color = DesignSystem.Colors.minimalLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "类型: ${composition.compositionType}",
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    fontSize = 12.sp
                )
                QualityBar("三分法", composition.ruleOfThirdsScore)
                QualityBar("对称性", composition.symmetryScore)
                QualityBar("视觉平衡", composition.visualBalanceScore)
                Text(
                    composition.feedback,
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 增强建议
            if (enhancements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "AI 增强建议",
                    color = DesignSystem.Colors.minimalLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                enhancements.take(3).forEach { suggestion ->
                    Text(
                        "• ${suggestion.title}",
                        color = DesignSystem.Colors.minimalSecondaryLabel,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityBar(label: String, score: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            label,
            color = DesignSystem.Colors.minimalSecondaryLabel,
            fontSize = 11.sp,
            modifier = Modifier.width(32.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DesignSystem.Colors.minimalOverlay)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(score / 100f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when {
                            score >= 80 -> DesignSystem.Colors.success
                            score >= 60 -> DesignSystem.Colors.warning
                            else -> DesignSystem.Colors.error
                        }
                    )
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "${score.toInt()}",
            color = DesignSystem.Colors.minimalSecondaryLabel,
            fontSize = 10.sp
        )
    }
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