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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.core.detection.DetectionMode
import com.livecompose.livecapture.core.frame.WatermarkInfo
import com.livecompose.livecapture.core.lut.LchColorAdjustment
import com.livecompose.livecapture.core.phantom.PhantomController
import com.livecompose.livecapture.core.phantom.PhantomService
import com.livecompose.livecapture.ui.components.LchMixerPanel
import com.livecompose.livecapture.ui.components.WatermarkEditSheet
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 设置界面
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
    var showWatermarkSheet by remember { mutableStateOf(false) }
    var currentWatermark = remember { WatermarkInfo() }

    // 低优先级功能状态
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phantomModeEnabled by remember { mutableStateOf(PhantomService.isEnabled(context)) }
    var rawCaptureEnabled by remember { mutableStateOf(false) }
    var aiColorMatchEnabled by remember { mutableStateOf(false) }
    var lchMixerEnabled by remember { mutableStateOf(false) }
    var lchAdjustment by remember { mutableStateOf(LchColorAdjustment()) }
    var showLchMixerSheet by remember { mutableStateOf(false) }

    // 从 DataStore 加载持久化设置
    LaunchedEffect(Unit) {
        autoCaptureEnabled = SettingsDataStore.isAutoCaptureEnabled(context)
        captureDelay = SettingsDataStore.getCaptureDelay(context).toDouble()
        detectionMode = SettingsDataStore.getDetectionMode(context)
        gridMode = SettingsDataStore.getGridMode(context)
        bloomEnabled = SettingsDataStore.isBloomEnabled(context)
        softGlowEnabled = SettingsDataStore.isSoftGlowEnabled(context)
        quickShotEnabled = SettingsDataStore.isQuickShotEnabled(context)
        hdrFusionEnabled = SettingsDataStore.isHdrFusionEnabled(context)
        multipleExposureCount = SettingsDataStore.getMultipleExposureCount(context)
        rawCaptureEnabled = SettingsDataStore.isRawCaptureEnabled(context)
    }

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
                Icon(Icons.Default.Palette, contentDescription = "主题模式", tint = DesignSystem.Colors.primary)
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
                    Icon(Icons.Default.Bolt, contentDescription = "自动拍照开关", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动拍照", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("对准构图框后自动触发拍摄", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = autoCaptureEnabled, onCheckedChange = {
                        autoCaptureEnabled = it
                        scope.launch { SettingsDataStore.setAutoCaptureEnabled(context, it) }
                    })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = "拍照延迟", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("拍照延迟", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${"%.1f".format(captureDelay)}秒", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { delay ->
                        TextButton(onClick = {
                            captureDelay = delay
                            scope.launch { SettingsDataStore.setCaptureDelay(context, delay.toFloat()) }
                        }) {
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
                            scope.launch { SettingsDataStore.setDetectionMode(context, mode) }
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
                Icon(Icons.Default.GridOn, contentDescription = "网格模式", tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("网格线", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("辅助构图参考线", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("关闭", "三分法", "黄金分割", "九宫格").forEachIndexed { index, label ->
                    TextButton(onClick = {
                        gridMode = index
                        scope.launch { SettingsDataStore.setGridMode(context, index) }
                    }, modifier = Modifier.weight(1f)) {
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
                Icon(Icons.Default.Radar, contentDescription = "检测模式选择", tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("检测算法", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text(detectionMode.description, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetectionMode.entries.forEachIndexed { index, mode ->
                    TextButton(onClick = {
                        detectionMode = mode
                        scope.launch { SettingsDataStore.setDetectionMode(context, mode) }
                    }, modifier = Modifier.weight(1f)) {
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
                Icon(Icons.Default.PhotoAlbum, contentDescription = "相框样式", tint = DesignSystem.Colors.primary)
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
                    Icon(Icons.Default.LightMode, contentDescription = "自然光模拟", tint = DesignSystem.Colors.primary)
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
                    Icon(Icons.Default.FlashOn, contentDescription = "Bloom效果", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bloom 效果", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("添加柔和光晕效果", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = bloomEnabled, onCheckedChange = {
                        bloomEnabled = it
                        scope.launch { SettingsDataStore.setBloomEnabled(context, it) }
                    })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = "柔光滤镜", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("柔光滤镜", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("应用柔美光线效果", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = softGlowEnabled, onCheckedChange = {
                        softGlowEnabled = it
                        scope.launch { SettingsDataStore.setSoftGlowEnabled(context, it) }
                    })
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
                Icon(Icons.Default.CameraEnhance, contentDescription = "快速拍摄", tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quick Shot", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("快速抓拍模式，无需对焦即可拍摄", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
                Switch(checked = quickShotEnabled, onCheckedChange = {
                    quickShotEnabled = it
                    scope.launch { SettingsDataStore.setQuickShotEnabled(context, it) }
                })
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
                    Icon(Icons.Default.AutoFixHigh, contentDescription = "多帧降噪", tint = DesignSystem.Colors.primary)
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
                    Icon(Icons.Default.HdrStrong, contentDescription = "HDR融合", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HDR 融合", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("合成高动态范围图像", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = hdrFusionEnabled, onCheckedChange = {
                        hdrFusionEnabled = it
                        scope.launch { SettingsDataStore.setHdrFusionEnabled(context, it) }
                    })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                // 多重曝光选项
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = "多重曝光", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("多重曝光", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("叠加多张照片的艺术效果", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("关闭", "2次", "3次", "5次").forEachIndexed { index, label ->
                        TextButton(onClick = {
                            multipleExposureCount = index
                            scope.launch { SettingsDataStore.setMultipleExposureCount(context, index) }
                        }, modifier = Modifier.weight(1f)) {
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
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "超焦距显示", tint = DesignSystem.Colors.primary)
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
                    Icon(Icons.Default.Folder, contentDescription = "保存位置", tint = DesignSystem.Colors.primary)
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
                    Icon(Icons.Default.Image, contentDescription = "HEIC格式", tint = DesignSystem.Colors.primary)
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
            onClick = { showWatermarkSheet = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Water, contentDescription = "水印设置", tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("水印设置", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("自定义文字、日期、Logo 水印", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "展开", tint = DesignSystem.Colors.textTertiary())
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ====== 低优先级功能：AI 仿色 ======
        Text("AI 仿色", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI色彩匹配", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI 色彩匹配", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("分析参考照片色彩风格，自动生成 LUT", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = aiColorMatchEnabled, onCheckedChange = { aiColorMatchEnabled = it })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Colorize, contentDescription = "OKLCH混色器", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OKLCH 混色器", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("感知均匀色彩空间，9 通道精准调色", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = lchMixerEnabled, onCheckedChange = {
                        lchMixerEnabled = it
                        if (it) showLchMixerSheet = true
                    })
                }
                if (lchMixerEnabled) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showLchMixerSheet = true }) {
                        Text("打开混色器面板", color = DesignSystem.Colors.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ====== 低优先级功能：RAW 管线 ======
        Text("RAW 处理", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Camera, contentDescription = "RAW拍摄", tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("RAW 拍摄", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("全链路 RAW 处理：去马赛克→色彩校正→色调映射", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
                Switch(checked = rawCaptureEnabled, onCheckedChange = {
                    rawCaptureEnabled = it
                    scope.launch { SettingsDataStore.setRawCaptureEnabled(context, it) }
                })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ====== 低优先级功能：幻影模式 ======
        Text("幻影模式", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = "幻影模式", tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("幻影模式", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("监听系统相机输出，自动应用 LUT 色彩处理", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = phantomModeEnabled, onCheckedChange = { enabled ->
                        if (enabled) {
                            if (PhantomController.hasAllPermissions(context)) {
                                PhantomController.start(context)
                                phantomModeEnabled = true
                            }
                            // 无权限时不切换，用户需先授权
                        } else {
                            PhantomController.stop(context)
                            phantomModeEnabled = false
                        }
                    })
                }
                if (!PhantomController.hasAllPermissions(context)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "需要媒体读取权限",
                        style = DesignSystem.Typography.caption1,
                        color = DesignSystem.Colors.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Row {
                        TextButton(onClick = {
                            // 请求权限需由Activity处理，此处提示用户到系统设置
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = android.net.Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        }) {
                            Text("前往设置授权")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 水印编辑底部弹窗
    if (showWatermarkSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showWatermarkSheet = false },
            containerColor = Color(0xFF1A1A1A)
        ) {
            WatermarkEditSheet(
                watermark = currentWatermark,
                onWatermarkChanged = { currentWatermark = it },
                onApply = { showWatermarkSheet = false },
                onDismiss = { showWatermarkSheet = false }
            )
        }
    }

    // OKLCH 混色器底部弹窗
    if (showLchMixerSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showLchMixerSheet = false },
            containerColor = Color(0xFF1A1A1A)
        ) {
            LchMixerPanel(
                adjustment = lchAdjustment,
                onAdjustmentChanged = { lchAdjustment = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
