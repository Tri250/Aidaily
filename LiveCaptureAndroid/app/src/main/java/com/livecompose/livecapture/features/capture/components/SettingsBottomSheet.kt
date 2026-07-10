package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsBottomSheet2026(
    onDismiss: () -> Unit,
    // 拍摄格式
    rawCaptureEnabled: Boolean,
    onRawCaptureChange: (Boolean) -> Unit,
    dngCaptureEnabled: Boolean,
    onDngCaptureChange: (Boolean) -> Unit,
    // 构图辅助
    gridMode: Int = 0,
    onGridModeChange: (Int) -> Unit = {},
    showLevel: Boolean = false,
    onShowLevelChange: (Boolean) -> Unit = {},
    showHistogram: Boolean = false,
    onShowHistogramChange: (Boolean) -> Unit = {},
    showZebra: Boolean = false,
    onShowZebraChange: (Boolean) -> Unit = {},
    smartCompositionEnabled: Boolean = false,
    onSmartCompositionChange: (Boolean) -> Unit = {},
    // 专业功能
    hyperfocalEnabled: Boolean,
    onHyperfocalChange: (Boolean) -> Unit,
    burstModeEnabled: Boolean,
    onBurstModeChange: (Boolean) -> Unit,
    multiExposureEnabled: Boolean,
    onMultiExposureChange: (Boolean) -> Unit,
    multiExposureFrameCount: Int,
    onMultiExposureReset: () -> Unit,
    slowMotionEnabled: Boolean = false,
    onSlowMotionChange: (Boolean) -> Unit = {},
    videoStabilizationEnabled: Boolean = true,
    onVideoStabilizationChange: (Boolean) -> Unit = {},
    showMemoryMonitor: Boolean,
    onShowMemoryMonitorChange: (Boolean) -> Unit,
    showFocusPeaking: Boolean = false,
    onFocusPeakingChange: (Boolean) -> Unit = {},
    hdrEnabled: Boolean = false,
    onHdrChange: (Boolean) -> Unit = {},
    // 美颜
    isBeautyEnabled: Boolean = true,
    onBeautyEnabledChange: (Boolean) -> Unit = {},
    showPortraitMode: Boolean = false,
    onPortraitModeChange: (Boolean) -> Unit = {},
    // 通用
    autoCaptureEnabled: Boolean = true,
    onAutoCaptureChange: (Boolean) -> Unit = {},
    captureDelay: Double = 1.0,
    onCaptureDelayChange: (Double) -> Unit = {},
    flashMode: FlashMode = FlashMode.OFF,
    onFlashModeChange: (FlashMode) -> Unit = {},
    // [v1.1.7] 定时拍摄 - 从主作用域提升
    timerEnabled: Boolean = false,
    onTimerEnabledChange: (Boolean) -> Unit = {},
    timerDuration: Int = 3,
    onTimerDurationChange: (Int) -> Unit = {},
    // [v1.1.7] 哈苏水印与快门音
    hasselbladWatermarkEnabled: Boolean = false,
    onHasselbladWatermarkChange: (Boolean) -> Unit = {},
    hasselbladShutterEnabled: Boolean = true,
    onHasselbladShutterChange: (Boolean) -> Unit = {},
    // [v1.1.7] Live Photo / 单手模式 / 左手模式
    livePhotoEnabled: Boolean = false,
    onLivePhotoChange: (Boolean) -> Unit = {},
    oneHandMode: Boolean = false,
    onOneHandModeChange: (Boolean) -> Unit = {},
    leftHandMode: Boolean = false,
    onLeftHandModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    var phantomModeEnabled by remember { mutableStateOf(false) }
    var selectedThemeIndex by remember { mutableIntStateOf(0) }

    // 拍摄格式（本地状态）
    var jpegHeifIndex by remember { mutableIntStateOf(0) }
    var qualityIndex by remember { mutableIntStateOf(0) }

    // 专业功能（本地状态）
    var mirrorFrontEnabled by remember { mutableStateOf(false) }
    var voiceCaptureEnabled by remember { mutableStateOf(false) }

    // 通用（本地状态）
    // [v1.1.7] timerEnabled/timerDuration 已提升到主作用域
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }
    var smartTrackingEnabled by remember { mutableStateOf(false) }
    var locationTagEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DesignSystem.Colors.backgroundPrimary())
                .clickable(enabled = false, onClick = {})
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DesignSystem.Colors.gray4())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("设置", style = DesignSystem.Typography.largeTitle, color = DesignSystem.Colors.textPrimary())
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("完成", color = DesignSystem.Colors.primary) }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSectionHeader("外观", Icons.Default.Palette)
            SettingsCard {
                SettingsRow("主题模式", "切换深色 / 浅色外观", Icons.Default.Brightness6) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("系统", "浅色", "深色").forEachIndexed { index, text ->
                            FilterChip(
                                selected = selectedThemeIndex == index,
                                onClick = { selectedThemeIndex = index },
                                label = { Text(text, style = DesignSystem.Typography.caption1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
            }

            SettingsSectionHeader("拍摄格式", Icons.Default.PhotoCamera)
            SettingsCard {
                SettingsRow("图片格式", "JPEG 通用兼容 / HEIF 高效压缩", Icons.Default.Image) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("JPEG", "HEIF").forEachIndexed { index, label ->
                            FilterChip(
                                selected = jpegHeifIndex == index,
                                onClick = { jpegHeifIndex = index },
                                label = { Text(label, style = DesignSystem.Typography.caption1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
                SettingsDivider()
                SettingsRow("画质设置", "影响照片文件大小与细节保留", Icons.Default.HighQuality) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("标准", "高质量", "节省空间").forEachIndexed { index, label ->
                            FilterChip(
                                selected = qualityIndex == index,
                                onClick = { qualityIndex = index },
                                label = { Text(label, style = DesignSystem.Typography.caption1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
            }

            SettingsSectionHeader("拍摄设置", Icons.Default.CameraAlt)
            SettingsCard {
                SettingsSwitchRow("自动拍照", "对准构图框后自动触发拍摄", Icons.Default.Bolt, autoCaptureEnabled) {
                    onAutoCaptureChange(it)
                }
                SettingsDivider()
                SettingsRow("拍照延迟", "${"%.1f".format(captureDelay)}秒后触发", Icons.Default.Timer) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.5, 1.0, 1.5, 2.0).forEach { delay ->
                            FilterChip(
                                selected = captureDelay == delay,
                                onClick = { onCaptureDelayChange(delay) },
                                label = { Text("${"%.1f".format(delay)}秒", style = DesignSystem.Typography.caption1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
            }

            SettingsSectionHeader("拍摄辅助", Icons.Default.AutoAwesome)
            SettingsCard {
                SettingsRow("网格线", "辅助构图参考线", Icons.Default.GridOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("关闭", "三分法", "黄金分割", "九宫格").forEachIndexed { index, label ->
                            FilterChip(
                                selected = gridMode == index,
                                onClick = { onGridModeChange(index) },
                                label = { Text(label, style = DesignSystem.Typography.caption2) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
                SettingsDivider()
                SettingsSwitchRow("水平仪", "实时显示设备倾斜角度，辅助水平构图", Icons.Default.AlignHorizontalLeft, showLevel) { onShowLevelChange(it) }
                SettingsDivider()
                SettingsSwitchRow("直方图", "实时亮度分布直方图，辅助曝光判断", Icons.Default.BarChart, showHistogram) { onShowHistogramChange(it) }
                SettingsDivider()
                SettingsSwitchRow("斑马纹", "过曝区域高亮提示，防止高光溢出", Icons.Default.Texture, showZebra) { onShowZebraChange(it) }
                SettingsDivider()
                SettingsSwitchRow("智能构图", "AI 实时分析场景，推荐最佳构图方案", Icons.Default.AutoAwesome, smartCompositionEnabled) { onSmartCompositionChange(it) }
            }

            SettingsSectionHeader("RAW 处理", Icons.Default.Camera)
            SettingsCard {
                SettingsSwitchRow("RAW 格式", "全链路 RAW 传感器数据保存", Icons.Default.RawOn, rawCaptureEnabled) { onRawCaptureChange(it) }
                SettingsDivider()
                SettingsSwitchRow("DNG 格式", "标准 DNG 数字负片格式保存", Icons.Default.Image, dngCaptureEnabled) { onDngCaptureChange(it) }
            }

            SettingsSectionHeader("专业功能", Icons.Default.Tune)
            SettingsCard {
                SettingsSwitchRow("峰值对焦", "Sobel 边缘检测高亮合焦区域", Icons.Default.CenterFocusStrong, showFocusPeaking) { onFocusPeakingChange(it) }
                SettingsDivider()
                SettingsSwitchRow("超焦距对焦", "自动对焦至超焦距距离，前后景均清晰", Icons.Default.CenterFocusStrong, hyperfocalEnabled) { onHyperfocalChange(it) }
                SettingsDivider()
                SettingsSwitchRow("连拍模式", "长按快门触发高速连拍", Icons.Default.BurstMode, burstModeEnabled) { onBurstModeChange(it) }
                SettingsDivider()
                SettingsSwitchRow("多重曝光", "每次拍摄叠加到前一张画面", Icons.Default.Layers, multiExposureEnabled) { onMultiExposureChange(it) }
                if (multiExposureEnabled) {
                    SettingsDivider()
                    SettingsRow("曝光层数", "当前已叠加 ${multiExposureFrameCount} 帧", Icons.Default.Layers) {
                        TextButton(onClick = {
                            onMultiExposureReset()
                            Toast.makeText(context, "多重曝光已重置", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("重置", color = DesignSystem.Colors.error, style = DesignSystem.Typography.caption1)
                        }
                    }
                }
                SettingsDivider()
                SettingsSwitchRow("HDR 模式", "多曝光融合保留高光细节", Icons.Default.HdrOn, hdrEnabled) { onHdrChange(it) }
                SettingsDivider()
                SettingsSwitchRow("Live Photo", "拍摄时同时记录1.5秒动态短片，让照片活起来", Icons.Default.MotionPhotosOn, livePhotoEnabled) { onLivePhotoChange(it) }
                SettingsDivider()
                SettingsSwitchRow("单手模式", "将核心控件下移，适配大屏单手操作，方便拇指触达", Icons.Default.PanTool, oneHandMode) { onOneHandModeChange(it) }
                SettingsDivider()
                SettingsSwitchRow("左手模式", "镜像翻转控制栏布局，适配左手持机习惯", Icons.Default.SwipeLeft, leftHandMode) { onLeftHandModeChange(it) }
                SettingsDivider()
                SettingsSwitchRow("镜像前置", "前置摄像头拍摄时水平翻转预览画面", Icons.Default.FlipCameraAndroid, mirrorFrontEnabled) { mirrorFrontEnabled = it }
                SettingsDivider()
                SettingsSwitchRow("声控拍照", "通过语音指令触发快门拍照", Icons.Default.Mic, voiceCaptureEnabled) { voiceCaptureEnabled = it }
                SettingsDivider()
                SettingsSwitchRow("内存监控", "显示当前内存使用量、告警等级和峰值", Icons.Default.Memory, showMemoryMonitor) { onShowMemoryMonitorChange(it) }
            }

            SettingsSectionHeader("美颜", Icons.Default.Face)
            SettingsCard {
                SettingsSwitchRow("美颜开关", "启用 AI 智能美颜，自动优化肤色与肤质", Icons.Default.FaceRetouchingNatural, isBeautyEnabled) { onBeautyEnabledChange(it) }
                SettingsDivider()
                SettingsSwitchRow("虚化/人像模式", "背景虚化，突出人像主体", Icons.Default.Portrait, showPortraitMode) { onPortraitModeChange(it) }
            }

            SettingsSectionHeader("幻影模式", Icons.Default.Visibility)
            SettingsCard {
                SettingsSwitchRow("幻影模式", "监听系统相机输出，自动应用 LUT 色彩处理", Icons.Default.VisibilityOff, phantomModeEnabled) { phantomModeEnabled = it }
            }

            SettingsSectionHeader("视频录制", Icons.Default.Videocam)
            SettingsCard {
                SettingsSwitchRow("慢动作", "拍摄高帧率视频，以慢动作播放", Icons.Default.SlowMotionVideo, slowMotionEnabled) { onSlowMotionChange(it) }
                SettingsDivider()
                SettingsSwitchRow("视频防抖", "录制时启用电子防抖，减少画面抖动", Icons.Default.Vibration, videoStabilizationEnabled) { onVideoStabilizationChange(it) }
            }

            SettingsSectionHeader("通用设置", Icons.Default.Settings)
            SettingsCard {
                SettingsSwitchRow("定时拍摄", "延迟触发快门，方便自拍与合影", Icons.Default.Timer, timerEnabled) { onTimerEnabledChange(it) }
                if (timerEnabled) {
                    SettingsDivider()
                    SettingsRow("定时时长", "${timerDuration}秒后自动拍摄", Icons.Default.Timer) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(3, 5, 10).forEach { sec ->
                                FilterChip(
                                    selected = timerDuration == sec,
                                    onClick = { onTimerDurationChange(sec) },
                                    label = { Text("${sec}秒", style = DesignSystem.Typography.caption1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                        selectedLabelColor = DesignSystem.Colors.primary
                                    )
                                )
                            }
                        }
                    }
                }
                SettingsDivider()
                SettingsSwitchRow("触觉反馈", "操作时触发振动反馈，提升交互确认感", Icons.Default.Vibration, hapticFeedbackEnabled) { hapticFeedbackEnabled = it }
                SettingsDivider()
                SettingsSwitchRow("智能追焦", "AI 自动识别并追踪移动主体，保持对焦", Icons.Default.GpsFixed, smartTrackingEnabled) { smartTrackingEnabled = it }
                SettingsDivider()
                SettingsSwitchRow("位置标记", "在照片 EXIF 中嵌入 GPS 地理位置信息", Icons.Default.LocationOn, locationTagEnabled) { locationTagEnabled = it }
                SettingsDivider()
                SettingsSwitchRow("哈苏水印", "添加哈苏标志性白框水印，参数条与型号标识", Icons.Default.BrandingWatermark, hasselbladWatermarkEnabled) { onHasselbladWatermarkChange(it) }
                SettingsDivider()
                SettingsSwitchRow("哈苏快门音", "哈苏经典机械快门音效，低沉金属质感", Icons.Default.MusicNote, hasselbladShutterEnabled) { onHasselbladShutterChange(it) }
                SettingsDivider()
                SettingsSwitchRow("Live Photo", "捕捉拍照前后1.5秒的动态画面", Icons.Default.MotionPhotosOn, livePhotoEnabled) { onLivePhotoChange(it) }
                SettingsDivider()
                SettingsSwitchRow("单手模式", "缩小操作区域，方便单手操作", Icons.Default.PanTool, oneHandMode) { onOneHandModeChange(it) }
                SettingsDivider()
                SettingsSwitchRow("左手模式", "水平翻转控制区，适配左手持机", Icons.Default.SwapHoriz, leftHandMode) { onLeftHandModeChange(it) }
                SettingsDivider()
                SettingsRow("闪光灯", "控制拍摄时闪光灯工作模式", Icons.Default.FlashlightOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(FlashMode.OFF, FlashMode.AUTO, FlashMode.ON, FlashMode.TORCH).forEach { mode ->
                            FilterChip(
                                selected = flashMode == mode,
                                onClick = { onFlashModeChange(mode) },
                                label = { Text(mode.displayName, style = DesignSystem.Typography.caption1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignSystem.Colors.primary
                                )
                            )
                        }
                    }
                }
            }

            SettingsSectionHeader("隐私与合规", Icons.Default.Security)
            SettingsCard {
                SettingsClickRow("权限管理", "查看和管理应用已授权的各项权限", Icons.Default.AdminPanelSettings) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
                SettingsDivider()
                ComplianceItem("隐私政策", Icons.Default.PrivacyTip, "privacy")
                ComplianceItem("用户服务协议", Icons.Default.Description, "agreement")
                ComplianceItem("个人信息收集清单", Icons.Default.ListAlt, "personal_info")
                ComplianceItem("青少年模式", Icons.Default.PersonRemove, "youth_mode")
                ComplianceItem("第三方SDK清单", Icons.Default.Code, "sdk_list")
            }

            SettingsSectionHeader("关于", Icons.Default.Info)
            SettingsCard {
                SettingsRow("版本信息", "秒简相机 v1.2.1", Icons.Default.Info)
                SettingsDivider()
                SettingsClickRow("ICP备案号", "待备案", Icons.Default.VerifiedUser) {
                    val intent = android.content.Intent(context, com.livecompose.livecapture.features.compliance.ComplianceHostActivity::class.java).apply {
                        putExtra("compliance_page", "icp_filing")
                    }
                    context.startActivity(intent)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 10.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary(), fontWeight = FontWeight.SemiBold)
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
private fun SettingsRow(title: String, subtitle: String, icon: ImageVector, trailing: @Composable () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        trailing()
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
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
private fun SettingsClickRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        Icon(Icons.Default.ChevronRight, null, tint = DesignSystem.Colors.textTertiary())
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = DesignSystem.Colors.gray3(), thickness = 0.5.dp)
}

@Composable
private fun ComplianceItem(title: String, icon: ImageVector, page: String) {
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
        Spacer(Modifier.width(12.dp))
        Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary(), modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = DesignSystem.Colors.textTertiary())
    }
}
