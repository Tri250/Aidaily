package com.livecompose.livecapture.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.core.detection.DetectionMode
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 设置界面
 * 对应 iOS 的 SettingsView
 */
@Composable
fun SettingsScreen() {
    var detectionMode by remember { mutableStateOf(DetectionMode.FAST) }
    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var captureDelay by remember { mutableStateOf(1.0) }
    var colorScheme by remember { mutableStateOf("system") }

    // 新增设置状态
    var gridMode by remember { mutableIntStateOf(0) }
    var selectedFrame by remember { mutableIntStateOf(0) }
    var naturalLightEnabled by remember { mutableStateOf(false) }
    var quickShotEnabled by remember { mutableStateOf(false) }
    var multiFrameDenoiseEnabled by remember { mutableStateOf(false) }
    var hdrFusionEnabled by remember { mutableStateOf(false) }
    var multipleExposureCount by remember { mutableIntStateOf(1) }
    var bloomEnabled by remember { mutableStateOf(false) }
    var softGlowEnabled by remember { mutableStateOf(false) }
    var hyperfocalDisplayEnabled by remember { mutableStateOf(false) }
    var saveLocationIndex by remember { mutableIntStateOf(0) }
    var heicExportEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("设置", style = DesignSystem.Typography.largeTitle, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(24.dp))

        // 外观
        Text("外观", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("主题模式", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("切换深色 / 浅色外观", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
                Spacer(modifier = Modifier.weight(1f))
                var selectedIndex by remember { mutableIntStateOf(0) }
                val options = listOf("系统", "浅色", "深色")
                options.forEachIndexed { index, text ->
                    TextButton(onClick = {
                        selectedIndex = index
                        colorScheme = when (index) { 1 -> "light"; 2 -> "dark"; else -> "system" }
                    }) {
                        Text(text, color = if (selectedIndex == index) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 拍摄设置
        Text("拍摄设置", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动拍照", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("对准构图框后自动触发拍摄", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = autoCaptureEnabled, onCheckedChange = { autoCaptureEnabled = it })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("拍照延迟", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${"%.1f".format(captureDelay)}秒", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { delay ->
                        TextButton(onClick = { captureDelay = delay }) {
                            Text(
                                "${"%.1f".format(delay)}秒",
                                color = if (captureDelay == delay) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 构图引擎
        Text("构图引擎", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                var selectedMode by remember { mutableIntStateOf(1) }
                Row {
                    DetectionMode.entries.forEachIndexed { index, mode ->
                        TextButton(onClick = {
                            selectedMode = index
                            detectionMode = mode
                        }) {
                            Text(mode.displayName, color = if (selectedMode == index) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(detectionMode.description, style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 网格模式选择
        Text("网格模式", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null, tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("网格线", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("辅助构图参考线", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("关闭", "三分法", "黄金分割", "九宫格").forEachIndexed { index, label ->
                    TextButton(onClick = { gridMode = index }, modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            color = if (gridMode == index) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary(),
                            style = DesignSystem.Typography.caption1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 检测模式选择
        Text("检测模式", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Radar, contentDescription = null, tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("检测算法", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text(detectionMode.description, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetectionMode.entries.forEachIndexed { index, mode ->
                    TextButton(onClick = { detectionMode = mode }, modifier = Modifier.weight(1f)) {
                        Text(
                            mode.displayName,
                            color = if (detectionMode == mode) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary(),
                            style = DesignSystem.Typography.caption1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 相框选择
        Text("相框", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PhotoFrame, contentDescription = null, tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("边框样式", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("为照片添加装饰性边框", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("无", "简约", "复古", "艺术", "胶片").forEachIndexed { index, label ->
                    TextButton(onClick = { selectedFrame = index }, modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            color = if (selectedFrame == index) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary(),
                            style = DesignSystem.Typography.caption1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 光效设置
        Text("光效", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 自然光开关
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LightMode, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自然光模拟", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("增强自然光照效果", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = naturalLightEnabled, onCheckedChange = { naturalLightEnabled = it })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                // Bloom/柔光选项
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bloom 效果", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("添加柔和光晕效果", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = bloomEnabled, onCheckedChange = { bloomEnabled = it })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("柔光滤镜", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("应用柔美光线效果", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = softGlowEnabled, onCheckedChange = { softGlowEnabled = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 快速拍摄
        Text("快速拍摄", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CameraEnhance, contentDescription = null, tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quick Shot", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("快速抓拍模式，无需对焦即可拍摄", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
                Switch(checked = quickShotEnabled, onCheckedChange = { quickShotEnabled = it })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 图像处理
        Text("图像处理", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 多帧降噪开关
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("多帧降噪", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("通过多张合成降低噪点", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = multiFrameDenoiseEnabled, onCheckedChange = { multiFrameDenoiseEnabled = it })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                // HDR 融合开关
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HdrStrong, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HDR 融合", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("合成高动态范围图像", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = hdrFusionEnabled, onCheckedChange = { hdrFusionEnabled = it })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                // 多重曝光选项
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("多重曝光", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("叠加多张照片的艺术效果", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("关闭", "2次", "3次", "5次").forEachIndexed { index, label ->
                        TextButton(onClick = { multipleExposureCount = index }, modifier = Modifier.weight(1f)) {
                            Text(
                                label,
                                color = if (multipleExposureCount == index) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary(),
                                style = DesignSystem.Typography.caption1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 显示与导出
        Text("显示与导出", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 超焦距显示开关
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("超焦距显示", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("在取景器显示超焦距离标尺", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = hyperfocalDisplayEnabled, onCheckedChange = { hyperfocalDisplayEnabled = it })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                // 保存位置选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("保存位置", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("选择照片存储目录", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DCIM/Pictures", "LiveCapture", "自定义").forEachIndexed { index, label ->
                        TextButton(onClick = { saveLocationIndex = index }, modifier = Modifier.weight(1f)) {
                            Text(
                                label,
                                color = if (saveLocationIndex == index) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary(),
                                style = DesignSystem.Typography.caption1
                            )
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                // HEIC 导出选项
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HEIC 格式", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("使用高效图片格式，节省存储空间", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = heicExportEnabled, onCheckedChange = { heicExportEnabled = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 水印设置入口
        Text("水印", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary()),
            onClick = { /* TODO: Navigate to watermark settings */ }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Water, contentDescription = null, tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("水印设置", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("自定义文字、日期、Logo 水印", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DesignSystem.Colors.textTertiary())
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
