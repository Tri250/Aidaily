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
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(photoId, retryTrigger) {
        isLoading = true
        loadError = null
        try {
            withContext(Dispatchers.IO) {
                photoBitmap = viewModel.getFullPhoto(photoId)
            }
        } catch (e: Exception) {
            loadError = "照片加载失败: ${e.message}"
        } finally {
            isLoading = false
        }
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
                        tint = DesignSystem.Colors.minimalLabel
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // 编辑按钮
                IconButton(onClick = { onEdit(photoId) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = DesignSystem.Colors.minimalLabel)
                }
                // AI编辑按钮
                IconButton(onClick = { showAIEdit = true }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI编辑", tint = DesignSystem.Colors.primary)
                }
                // 调整按钮
                IconButton(onClick = { onAdjust(photoId) }) {
                    Icon(Icons.Default.Tune, contentDescription = "调整", tint = DesignSystem.Colors.minimalLabel)
                }
                // 分享按钮
                IconButton(onClick = {
                    sharePhoto(context, photoFile)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "分享", tint = DesignSystem.Colors.minimalLabel)
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
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = DesignSystem.Colors.primary)
                    }
                    loadError != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = DesignSystem.Colors.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                loadError!!,
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                style = DesignSystem.Typography.footnote
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { retryTrigger++ }) {
                                Text("重试", color = DesignSystem.Colors.primary)
                            }
                        }
                    }
                    photoBitmap != null -> {
                        Image(
                            bitmap = photoBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignSystem.Spacing.small))

            // 拍摄参数区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
            ) {
                // 拍摄日期
                record?.let {
                    val dateFormat = SimpleDateFormat("yyyy年M月d日 HH:mm:ss", Locale.CHINA)
                    Text(
                        text = dateFormat.format(Date(it.creationDate)),
                        style = DesignSystem.Typography.callout,
                        color = DesignSystem.Colors.minimalLabel
                    )
                }

                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))

                // 拍摄参数卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DesignSystem.Colors.gray1()
                    ),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
                ) {
                    Column(modifier = Modifier.padding(DesignSystem.Spacing.small)) {
                        Text(
                            "拍摄参数",
                            style = DesignSystem.Typography.headline,
                            color = DesignSystem.Colors.minimalLabel
                        )
                        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxSmall))

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

                        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxSmall))

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

                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))

                // RGB 直方图卡片
                if (photoBitmap != null) {
                    val bmp = photoBitmap
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DesignSystem.Colors.gray1()
                        ),
                        shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
                    ) {
                        Column(modifier = Modifier.padding(DesignSystem.Spacing.small)) {
                            Text(
                                "RGB 直方图",
                                style = DesignSystem.Typography.headline,
                                color = DesignSystem.Colors.minimalLabel
                            )
                            Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxSmall))
                            if (bmp != null) RgbHistogram(bmp)
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))
                }

                // GPS 位置卡片
                if (exifData?.hasGps == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DesignSystem.Colors.gray1()
                        ),
                        shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
                    ) {
                        Column(modifier = Modifier.padding(DesignSystem.Spacing.small)) {
                            Text(
                                "GPS 位置",
                                style = DesignSystem.Typography.headline,
                                color = DesignSystem.Colors.minimalLabel
                            )
                            Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxSmall))
                            Text(
                                "纬度: ${exifData.gpsLatitude ?: "-"}",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                style = DesignSystem.Typography.footnote
                            )
                            Text(
                                "经度: ${exifData.gpsLongitude ?: "-"}",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                style = DesignSystem.Typography.footnote
                            )
                            if (exifData.gpsAltitude != null) {
                                Text(
                                    "海拔: ${exifData.gpsAltitude}",
                                    color = DesignSystem.Colors.minimalSecondaryLabel,
                                    style = DesignSystem.Typography.footnote
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))
                }

                // 文件信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DesignSystem.Colors.gray1()
                    ),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
                ) {
                    Column(modifier = Modifier.padding(DesignSystem.Spacing.small)) {
                        Text(
                            "文件信息",
                            style = DesignSystem.Typography.headline,
                            color = DesignSystem.Colors.minimalLabel
                        )
                        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxSmall))
                        Text(
                            "格式: ${photoFile.extension.uppercase()}",
                            color = DesignSystem.Colors.minimalSecondaryLabel,
                            style = DesignSystem.Typography.footnote
                        )
                        Text(
                            "大小: ${formatFileSize(photoFile.length())}",
                            color = DesignSystem.Colors.minimalSecondaryLabel,
                            style = DesignSystem.Typography.footnote
                        )
                        exifData?.cameraModel?.let {
                            Text(
                                "相机型号: $it",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                style = DesignSystem.Typography.footnote
                            )
                        }
                        exifData?.dateTime?.let {
                            Text(
                                "拍摄时间: $it",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                style = DesignSystem.Typography.footnote
                            )
                        }
                        exifData?.flash?.let {
                            Text(
                                "闪光灯: $it",
                                color = DesignSystem.Colors.minimalSecondaryLabel,
                                style = DesignSystem.Typography.footnote
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))

                // AI 质量评估卡片
                if (photoBitmap != null) {
                    AIQualityAssessmentCard(photoBitmap!!, context)
                    Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))
                }

                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xLarge))
            }
        }
    }

    // AI编辑界面
    if (showAIEdit) {
        AIEditScreen(
            photoId = photoId,
            sourceBitmap = photoBitmap,
            onBack = { showAIEdit = false }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除照片", color = DesignSystem.Colors.minimalLabel) },
            text = { Text("确定要删除这张照片吗？此操作不可恢复。", color = DesignSystem.Colors.minimalSecondaryLabel) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(photoId)
                    if (photoFile.exists()) photoFile.delete()
                    Toast.makeText(context, "照片已删除", Toast.LENGTH_SHORT).show()
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text("删除", color = DesignSystem.Colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = DesignSystem.Colors.minimalSecondaryLabel)
                }
            },
            containerColor = DesignSystem.Colors.gray2(),
            titleContentColor = DesignSystem.Colors.minimalLabel
        )
    }
}

@Composable
private fun ParamItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = DesignSystem.Typography.monoBody,
            color = DesignSystem.Colors.minimalLabel
        )
        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxxSmall))
        Text(
            text = label,
            color = DesignSystem.Colors.minimalSecondaryLabel,
            style = DesignSystem.Typography.caption2
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
        shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
    ) {
        Column(modifier = Modifier.padding(DesignSystem.Spacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = DesignSystem.Colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxSmall))
                Text(
                    "AI 画质评估",
                    style = DesignSystem.Typography.headline,
                    color = DesignSystem.Colors.minimalLabel
                )
            }
            Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxSmall))

            // 质量等级
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("综合评分: ", color = DesignSystem.Colors.minimalSecondaryLabel, style = DesignSystem.Typography.footnote)
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
                Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxSmall))
                Text(
                    quality.qualityGrade.displayName,
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    style = DesignSystem.Typography.caption1
                )
            }

            Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxxSmall))

            // 各维度进度条
            QualityBar("锐度", quality.sharpnessScore)
            QualityBar("噪声", quality.noiseLevel)
            QualityBar("曝光", quality.exposureScore)
            QualityBar("色彩", quality.colorHarmonyScore)

            // 构图分析
            if (composition != null) {
                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxSmall))
                Text(
                    "AI 构图分析",
                    color = DesignSystem.Colors.minimalLabel,
                    style = DesignSystem.Typography.footnote
                )
                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxxSmall))
                Text(
                    "类型: ${composition.compositionType}",
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    style = DesignSystem.Typography.caption1
                )
                QualityBar("三分法", composition.ruleOfThirdsScore)
                QualityBar("对称性", composition.symmetryScore)
                QualityBar("视觉平衡", composition.visualBalanceScore)
                Text(
                    composition.feedback,
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    style = DesignSystem.Typography.caption2,
                    modifier = Modifier.padding(top = DesignSystem.Spacing.xxxSmall)
                )
            }

            // 增强建议
            if (enhancements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxSmall))
                Text(
                    "AI 增强建议",
                    color = DesignSystem.Colors.minimalLabel,
                    style = DesignSystem.Typography.footnote
                )
                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxxSmall))
                enhancements.take(3).forEach { suggestion ->
                    Text(
                        "• ${suggestion.title}",
                        color = DesignSystem.Colors.minimalSecondaryLabel,
                        style = DesignSystem.Typography.caption1
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
            style = DesignSystem.Typography.caption2,
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
        Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxxSmall))
        Text(
            "${score.toInt()}",
            color = DesignSystem.Colors.minimalSecondaryLabel,
            style = DesignSystem.Typography.toolLabel
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