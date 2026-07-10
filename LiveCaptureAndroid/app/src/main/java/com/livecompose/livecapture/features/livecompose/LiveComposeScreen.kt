package com.livecompose.livecapture.features.livecompose

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.livecompose.livecapture.core.lut.BuiltInPresets
import com.livecompose.livecapture.core.lut.LutImporter
import com.livecompose.livecapture.core.lut.LutPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import java.io.File

/**
 * 秒简相机品牌页 + 实时滤镜预览
 */
@Composable
fun LiveComposeScreen(
    onNavigateToGallery: () -> Unit = {},
    viewModel: LiveComposeViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val intensity by viewModel.intensity.collectAsState()
    val processedBitmap by viewModel.processedBitmap.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.generateDemoBitmap(context)
    }

    // LUT 导入器
    val lutImporter = remember { LutImporter(context) }
    val importedPresets by lutImporter.importedPresets.collectAsState()
    val allPresets = remember(importedPresets) { lutImporter.allPresets() }

    // 文件选择器
    val lutFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val tempFile = File(context.cacheDir, "lut_import_${System.currentTimeMillis()}.cube")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val result = lutImporter.importCubeFile(tempFile.absolutePath)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "LUT 导入成功: ${result.displayName}", Toast.LENGTH_SHORT).show()
                    }
                    // 导入成功后自动选中新预设
                    viewModel.selectPreset(result.estimatedPreset)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "LUT 导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 预览区域标题
        Text(
            "滤镜预览",
            style = DesignSystem.Typography.title2,
            color = DesignSystem.Colors.textPrimary(),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            "选择预设，实时预览效果",
            style = DesignSystem.Typography.subheadline,
            color = DesignSystem.Colors.textSecondary(),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 示例图片预览
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .aspectRatio(4f / 3f)
                .clip(DesignSystem.mediumRoundedShape)
                .background(DesignSystem.Colors.backgroundSecondary()),
            contentAlignment = Alignment.Center
        ) {
            val displayBitmap = processedBitmap
            if (displayBitmap != null) {
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = "滤镜预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = DesignSystem.Colors.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "处理中...",
                            style = DesignSystem.Typography.caption1,
                            color = DesignSystem.Colors.textTertiary()
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = DesignSystem.Colors.textTertiary(),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "加载示例图片...",
                            style = DesignSystem.Typography.caption1,
                            color = DesignSystem.Colors.textTertiary()
                        )
                    }
                }
            }

            // 当前预设名称浮层
            if (selectedPreset != null && displayBitmap != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = DesignSystem.Colors.minimalDarkOverlay
                ) {
                    Text(
                        selectedPreset!!.name,
                        style = DesignSystem.Typography.caption1,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 强度滑块
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "强度",
                    style = DesignSystem.Typography.subheadline,
                    color = DesignSystem.Colors.textPrimary()
                )
                Text(
                    "${(intensity * 100).toInt()}%",
                    style = DesignSystem.Typography.monoCaption,
                    color = DesignSystem.Colors.primary
                )
            }
            Slider(
                value = intensity,
                onValueChange = { viewModel.updateIntensity(it) },
                colors = SliderDefaults.colors(
                    thumbColor = DesignSystem.Colors.primary,
                    activeTrackColor = DesignSystem.Colors.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // LUT 预设横向列表
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "色彩预设",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.textPrimary()
            )
            TextButton(
                onClick = { lutFilePickerLauncher.launch("*/*") },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = DesignSystem.Colors.primary
                )
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "导入 LUT",
                    style = DesignSystem.Typography.subheadline
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allPresets.size) { index ->
                val preset = allPresets[index]
                val isSelected = selectedPreset?.id == preset.id

                PresetCard(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = { viewModel.selectPreset(preset) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 保存按钮
        Button(
            onClick = {
                scope.launch {
                    viewModel.saveProcessedPhoto(context)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp),
            shape = DesignSystem.mediumRoundedShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = DesignSystem.Colors.primary
            ),
            enabled = processedBitmap != null && !isProcessing
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "保存照片",
                style = DesignSystem.Typography.headline,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== 原有品牌区域 =====

        // Logo 区域
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DesignSystem.Colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "秒简相机",
            style = DesignSystem.Typography.largeTitle,
            color = DesignSystem.Colors.textPrimary(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            "让每一次快门，都定格最美的瞬间",
            style = DesignSystem.Typography.body,
            color = DesignSystem.Colors.textSecondary(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 关于我们
        SectionCard(title = "关于我们", icon = Icons.Default.Star) {
            Text(
                "秒简相机 致力于让每一位普通用户都能轻松拍出专业级构图照片。不同于传统相机的静态九宫格辅助线，我们通过 AI 实时分析取景画面，结合设备陀螺仪实现物理级追踪引导，主动「告诉」用户如何移动手机以获得最佳构图，并在对齐完美构图时自动拍摄。",
                style = DesignSystem.Typography.body,
                color = DesignSystem.Colors.textTertiary()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 项目仓库
        SectionCard(title = "项目仓库", icon = Icons.Default.Folder) {
            ProjectRow(
                icon = Icons.Default.Apps,
                name = "秒简相机",
                desc = "Android 客户端 App — 基于 Jetpack Compose 构建，集成 Adacrop 美学裁切模型、陀螺仪运动追踪与实时构图引导。",
                url = "https://github.com/LiveCompose/LiveCapture"
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProjectRow(
                icon = Icons.Default.Memory,
                name = "LiveCompose",
                desc = "核心模型仓库 — 包含 Adacrop 强化学习训练框架、模型定义与实验配置。",
                url = "https://github.com/LiveCompose/LiveCompose"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 资源链接
        SectionCard(title = "资源链接", icon = Icons.Default.Link) {
            LinkRow("GitHub 组织", "github.com/LiveCompose", "https://github.com/LiveCompose")
            LinkRow("HuggingFace 模型库", "huggingface.co/LiveCompose", "https://huggingface.co/LiveCompose")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 核心技术
        SectionCard(title = "核心技术", icon = Icons.Default.Settings) {
            TechRow(Icons.Default.Memory, "Adacrop 强化学习模型", "基于 Actor-Critic 架构的自适应美学裁切。")
            HorizontalDivider()
            TechRow(Icons.Default.Sensors, "陀螺仪运动追踪", "实时采集设备角速度与加速度。")
            HorizontalDivider()
            TechRow(Icons.Default.Visibility, "Vision 原生检测", "集成 ML Kit 人脸/人体检测。")
            HorizontalDivider()
            TechRow(Icons.Default.Camera, "多镜头智能变焦", "支持超广角、广角、长焦等多种镜头。")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * 预设卡片
 */
@Composable
private fun PresetCard(
    preset: LutPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        // 色彩圆圈（根据预设参数生成代表色）
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) DesignSystem.Colors.primary
                    else DesignSystem.Colors.backgroundSecondary()
                ),
            contentAlignment = Alignment.Center
        ) {
            // 内圈展示预设色调
            val tint = remember(preset.id) { generatePresetTint(preset) }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(tint)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            preset.name,
            style = if (isSelected) DesignSystem.Typography.caption1.copy(
                fontWeight = FontWeight.SemiBold
            ) else DesignSystem.Typography.caption1,
            color = if (isSelected) DesignSystem.Colors.primary else DesignSystem.Colors.textSecondary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 根据预设参数生成代表色
 */
private fun generatePresetTint(preset: LutPreset): androidx.compose.ui.graphics.Color {
    if (preset.id == "original") {
        return DesignSystem.Colors.minimalSecondaryLabel
    }
    // 根据色温、饱和度、对比度推算代表色
    val warmthShift = preset.warmth.coerceIn(-100f, 100f) / 100f
    val r = (0.5f + warmthShift * 0.3f).coerceIn(0f, 1f)
    val g = 0.4f
    val b = (0.5f - warmthShift * 0.3f).coerceIn(0f, 1f)
    val sat = preset.saturation.coerceIn(0f, 2f)
    val gray = 0.299f * r + 0.587f * g + 0.114f * b
    val sr = (gray + (r - gray) * sat).coerceIn(0f, 1f)
    val sg = (gray + (g - gray) * sat).coerceIn(0f, 1f)
    val sb = (gray + (b - gray) * sat).coerceIn(0f, 1f)

    return androidx.compose.ui.graphics.Color(sr, sg, sb, 1f)
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        shape = DesignSystem.mediumRoundedShape,
        colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ProjectRow(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, desc: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(DesignSystem.Colors.backgroundTertiary())
        .padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary(), modifier = Modifier.weight(1f))
            IconButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DesignSystem.Colors.backgroundTertiary())
            .clickable { uriHandler.openUri(url) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.textTertiary())
        }
        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TechRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(desc, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
    }
}
