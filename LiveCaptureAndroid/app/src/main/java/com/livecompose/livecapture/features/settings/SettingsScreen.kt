package com.livecompose.livecapture.features.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.detection.DetectionMode
import com.livecompose.livecapture.core.frame.WatermarkInfo
import com.livecompose.livecapture.core.lut.LchColorAdjustment
import com.livecompose.livecapture.core.phantom.PhantomController
import com.livecompose.livecapture.core.phantom.PhantomService
import com.livecompose.livecapture.ui.components.LchMixerPanel
import com.livecompose.livecapture.ui.components.WatermarkEditSheet
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.glassmorphism
import com.livecompose.livecapture.ui.design.liquidGlass
import com.livecompose.livecapture.BuildConfig
import kotlinx.coroutines.launch

/**
 * 设置界面 - 液态玻璃风格 2026 高端摄影体验
 * 国内一流旗舰品牌手机摄影交互设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var detectionMode by remember { mutableStateOf(DetectionMode.FAST) }
    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var captureDelay by remember { mutableStateOf(1.0) }
    var colorScheme by remember { mutableStateOf("system") }

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phantomModeEnabled by remember { mutableStateOf(PhantomService.isEnabled(context)) }
    var rawCaptureEnabled by remember { mutableStateOf(false) }
    var aiColorMatchEnabled by remember { mutableStateOf(false) }
    var lchMixerEnabled by remember { mutableStateOf(false) }
    var lchAdjustment by remember { mutableStateOf(LchColorAdjustment()) }
    var showLchMixerSheet by remember { mutableStateOf(false) }

    var selectedThemeIndex by remember { mutableIntStateOf(0) }
    var selectedDetectionModeIndex by remember { mutableIntStateOf(1) }

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
            .background(DesignSystem.Colors.backgroundPrimary())
            .verticalScroll(rememberScrollState())
    ) {
        // 头部
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "设置",
            style = DesignSystem.Typography.largeTitle,
            color = DesignSystem.Colors.textPrimary(),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // ====== 外观 ======
        SectionHeader(title = "外观", icon = Icons.Default.Palette)
        SettingsCard {
            SettingsRow(
                icon = Icons.Default.Brightness6,
                title = "主题模式",
                subtitle = "切换深色 / 浅色外观"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("系统", "浅色", "深色").forEachIndexed { index, text ->
                        FilterChip(
                            selected = selectedThemeIndex == index,
                            onClick = {
                                selectedThemeIndex = index
                                colorScheme = when (index) { 1 -> "light"; 2 -> "dark"; else -> "system" }
                            },
                            label = {
                                Text(text, style = DesignSystem.Typography.caption1)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                selectedLabelColor = DesignSystem.Colors.primary
                            )
                        )
                    }
                }
            }
        }

        // ====== 拍摄设置 ======
        SectionHeader(title = "拍摄设置", icon = Icons.Default.CameraAlt)
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.Bolt,
                title = "自动拍照",
                subtitle = "对准构图框后自动触发拍摄",
                checked = autoCaptureEnabled,
                onCheckedChange = {
                    autoCaptureEnabled = it
                    scope.launch { SettingsDataStore.setAutoCaptureEnabled(context, it) }
                }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Timer,
                title = "拍照延迟",
                subtitle = "${"%.1f".format(captureDelay)}秒后触发"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { delay ->
                        FilterChip(
                            selected = captureDelay == delay,
                            onClick = {
                                captureDelay = delay
                                scope.launch { SettingsDataStore.setCaptureDelay(context, delay.toFloat()) }
                            },
                            label = {
                                Text("${"%.1f".format(delay)}秒", style = DesignSystem.Typography.caption1)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                selectedLabelColor = DesignSystem.Colors.primary
                            )
                        )
                    }
                }
            }
        }

        // ====== 构图引擎 ======
        SectionHeader(title = "构图引擎", icon = Icons.Default.AutoAwesome)
        SettingsCard {
            SettingsRow(
                icon = Icons.Default.Radar,
                title = "检测算法",
                subtitle = detectionMode.description
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetectionMode.entries.forEachIndexed { index, mode ->
                        FilterChip(
                            selected = detectionMode == mode,
                            onClick = {
                                detectionMode = mode
                                scope.launch { SettingsDataStore.setDetectionMode(context, mode) }
                            },
                            label = {
                                Text(mode.displayName, style = DesignSystem.Typography.caption1)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                selectedLabelColor = DesignSystem.Colors.primary
                            )
                        )
                    }
                }
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.GridOn,
                title = "网格线",
                subtitle = "辅助构图参考线"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("关闭", "三分法", "黄金分割", "九宫格").forEachIndexed { index, label ->
                        FilterChip(
                            selected = gridMode == index,
                            onClick = {
                                gridMode = index
                                scope.launch { SettingsDataStore.setGridMode(context, index) }
                            },
                            label = {
                                Text(label, style = DesignSystem.Typography.caption2)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                selectedLabelColor = DesignSystem.Colors.primary
                            )
                        )
                    }
                }
            }
        }

        // ====== 光效 ======
        SectionHeader(title = "光效", icon = Icons.Default.LightMode)
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.WbSunny,
                title = "自然光模拟",
                subtitle = "增强自然光照效果",
                checked = naturalLightEnabled,
                onCheckedChange = { naturalLightEnabled = it }
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Default.FlashOn,
                title = "Bloom 效果",
                subtitle = "添加柔和光晕效果",
                checked = bloomEnabled,
                onCheckedChange = {
                    bloomEnabled = it
                    scope.launch { SettingsDataStore.setBloomEnabled(context, it) }
                }
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Default.AutoAwesome,
                title = "柔光滤镜",
                subtitle = "应用柔美光线效果",
                checked = softGlowEnabled,
                onCheckedChange = {
                    softGlowEnabled = it
                    scope.launch { SettingsDataStore.setSoftGlowEnabled(context, it) }
                }
            )
        }

        // ====== 图像处理 ======
        SectionHeader(title = "图像处理", icon = Icons.Default.Tune)
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.AutoFixHigh,
                title = "多帧降噪",
                subtitle = "通过多张合成降低噪点",
                checked = multiFrameDenoiseEnabled,
                onCheckedChange = { multiFrameDenoiseEnabled = it }
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Default.HdrStrong,
                title = "HDR 融合",
                subtitle = "合成高动态范围图像",
                checked = hdrFusionEnabled,
                onCheckedChange = {
                    hdrFusionEnabled = it
                    scope.launch { SettingsDataStore.setHdrFusionEnabled(context, it) }
                }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Layers,
                title = "多重曝光",
                subtitle = "叠加多张照片的艺术效果"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("关闭", "2次", "3次", "5次").forEachIndexed { index, label ->
                        FilterChip(
                            selected = multipleExposureCount == index,
                            onClick = {
                                multipleExposureCount = index
                                scope.launch { SettingsDataStore.setMultipleExposureCount(context, index) }
                            },
                            label = {
                                Text(label, style = DesignSystem.Typography.caption2)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                selectedLabelColor = DesignSystem.Colors.primary
                            )
                        )
                    }
                }
            }
        }

        // ====== 快速拍摄 ======
        SectionHeader(title = "快速拍摄", icon = Icons.Default.Speed)
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.CameraEnhance,
                title = "Quick Shot",
                subtitle = "快速抓拍模式，无需对焦即可拍摄",
                checked = quickShotEnabled,
                onCheckedChange = {
                    quickShotEnabled = it
                    scope.launch { SettingsDataStore.setQuickShotEnabled(context, it) }
                }
            )
        }

        // ====== 显示与导出 ======
        SectionHeader(title = "显示与导出", icon = Icons.Default.DisplaySettings)
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.CenterFocusStrong,
                title = "超焦距显示",
                subtitle = "在取景器显示超焦距离标尺",
                checked = hyperfocalDisplayEnabled,
                onCheckedChange = { hyperfocalDisplayEnabled = it }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Default.Folder,
                title = "保存位置",
                subtitle = "选择照片存储目录"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("DCIM", "秒简", "自定义").forEachIndexed { index, label ->
                        FilterChip(
                            selected = saveLocationIndex == index,
                            onClick = { saveLocationIndex = index },
                            label = {
                                Text(label, style = DesignSystem.Typography.caption2)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                selectedLabelColor = DesignSystem.Colors.primary
                            )
                        )
                    }
                }
            }
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Default.Image,
                title = "HEIC 格式",
                subtitle = "高效图片格式，节省存储空间",
                checked = heicExportEnabled,
                onCheckedChange = { heicExportEnabled = it }
            )
        }

        // ====== 水印 ======
        SectionHeader(title = "水印", icon = Icons.Default.Water)
        SettingsCard {
            SettingsClickRow(
                icon = Icons.Default.Brush,
                title = "水印设置",
                subtitle = "自定义文字、日期、Logo 水印",
                onClick = { showWatermarkSheet = true }
            )
        }

        // ====== AI 仿色 ======
        SectionHeader(title = "AI 仿色", icon = Icons.Default.Colorize)
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.AutoAwesome,
                title = "AI 色彩匹配",
                subtitle = "分析参考照片色彩风格，自动生成 LUT",
                checked = aiColorMatchEnabled,
                onCheckedChange = { aiColorMatchEnabled = it }
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Default.Palette,
                title = "OKLCH 混色器",
                subtitle = "感知均匀色彩空间，9 通道精准调色",
                checked = lchMixerEnabled,
                onCheckedChange = {
                    lchMixerEnabled = it
                    if (it) showLchMixerSheet = true
                }
            )
            if (lchMixerEnabled) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showLchMixerSheet = true }) {
                    Text("打开混色器面板", color = DesignSystem.Colors.primary)
                }
            }
        }

        // ====== RAW 处理 ======
        SectionHeader(title = "RAW 处理", icon = Icons.Default.Camera)
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.RawOn,
                title = "RAW 拍摄",
                subtitle = "全链路 RAW 处理：去马赛克→色彩校正→色调映射",
                checked = rawCaptureEnabled,
                onCheckedChange = {
                    rawCaptureEnabled = it
                    scope.launch { SettingsDataStore.setRawCaptureEnabled(context, it) }
                }
            )
        }

        // ====== 幻影模式 ======
        SectionHeader(title = "幻影模式", icon = Icons.Default.Visibility)
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.VisibilityOff,
                title = "幻影模式",
                subtitle = "监听系统相机输出，自动应用 LUT 色彩处理",
                checked = phantomModeEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (PhantomController.hasAllPermissions(context)) {
                            PhantomController.start(context)
                            phantomModeEnabled = true
                        }
                    } else {
                        PhantomController.stop(context)
                        phantomModeEnabled = false
                    }
                }
            )
            if (!PhantomController.hasAllPermissions(context)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "需要媒体读取权限",
                    style = DesignSystem.Typography.caption1,
                    color = DesignSystem.Colors.primary
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                }) {
                    Text("前往设置授权")
                }
            }
        }

        // ====== 隐私与合规 ======
        SectionHeader(title = "隐私与合规", icon = Icons.Default.Security)
        SettingsCard {
            ComplianceItem(
                icon = Icons.Default.PrivacyTip,
                title = "隐私政策",
                page = "privacy"
            )
            ComplianceItem(
                icon = Icons.Default.Description,
                title = "用户服务协议",
                page = "agreement"
            )
            ComplianceItem(
                icon = Icons.Default.ListAlt,
                title = "个人信息收集清单",
                page = "personal_info"
            )
            ComplianceItem(
                icon = Icons.Default.PersonRemove,
                title = "账号管理",
                page = "account_deletion"
            )
            ComplianceItem(
                icon = Icons.Default.Security,
                title = "青少年模式",
                page = "youth_mode"
            )
            ComplianceItem(
                icon = Icons.Default.Code,
                title = "第三方SDK清单",
                page = "sdk_list"
            )
        }

        // ====== 社区 ======
        SectionHeader(title = "社区", icon = Icons.Default.Groups)
        SettingsCard {
            SettingsClickRow(
                icon = Icons.Default.Explore,
                title = "发现社区",
                subtitle = "挑战 / 滤镜 / 地点探索",
                onClick = {
                    val intent = android.content.Intent(context, com.livecompose.livecapture.features.compliance.ComplianceHostActivity::class.java).apply {
                        putExtra("compliance_page", "community")
                    }
                    context.startActivity(intent)
                }
            )
        }

        // ====== 关于 ======
        SectionHeader(title = "关于", icon = Icons.Default.Info)
        SettingsCard {
            SettingsRow(
                icon = Icons.Default.Info,
                title = "版本信息",
                subtitle = "秒简相机 v${BuildConfig.VERSION_NAME}"
            )
            SettingsDivider()
            SettingsClickRow(
                icon = Icons.Default.VerifiedUser,
                title = "ICP备案号",
                subtitle = "待备案（请前往工信部ICP备案系统完成备案）",
                onClick = {
                    val intent = android.content.Intent(context, com.livecompose.livecapture.features.compliance.ComplianceHostActivity::class.java).apply {
                        putExtra("compliance_page", "icp_filing")
                    }
                    context.startActivity(intent)
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 水印编辑底部弹窗
    if (showWatermarkSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWatermarkSheet = false },
            containerColor = DesignSystem.Colors.gray1()
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
        ModalBottomSheet(
            onDismissRequest = { showLchMixerSheet = false },
            containerColor = DesignSystem.Colors.gray1()
        ) {
            LchMixerPanel(
                adjustment = lchAdjustment,
                onAdjustmentChanged = { lchAdjustment = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ====== 设置页通用组件 ======

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 10.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧强调色块
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(DesignSystem.Colors.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            icon,
            contentDescription = null,
            tint = DesignSystem.Colors.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = DesignSystem.Typography.headline,
            color = DesignSystem.Colors.textPrimary(),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.08f)
            .padding(4.dp),
        content = content
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        trailing()
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DesignSystem.Colors.primary,
                uncheckedThumbColor = DesignSystem.Colors.gray4(),
                uncheckedTrackColor = DesignSystem.Colors.gray3()
            )
        )
    }
}

@Composable
private fun SettingsClickRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        Icon(Icons.Default.ChevronRight, null, tint = DesignSystem.Colors.textTertiary())
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        color = DesignSystem.Colors.gray3(),
        thickness = 0.5.dp
    )
}

@Composable
private fun ComplianceItem(icon: ImageVector, title: String, page: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .clickable {
                val intent = android.content.Intent(context, com.livecompose.livecapture.features.compliance.ComplianceHostActivity::class.java).apply {
                    putExtra("compliance_page", page)
                }
                context.startActivity(intent)
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            style = DesignSystem.Typography.headline,
            color = DesignSystem.Colors.textPrimary(),
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, null, tint = DesignSystem.Colors.textTertiary())
    }
}