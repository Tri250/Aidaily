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
internal fun PortraitModeOverlay(
    viewModel: com.livecompose.livecapture.core.portrait.PortraitViewModel,
    onDismiss: () -> Unit,
    onProcessImage: (android.graphics.Bitmap) -> Unit,
    skinProtectionFilter: com.livecompose.livecapture.core.filter.SkinProtectionFilter
) {
    val skinSmoothing by viewModel.skinSmoothing.collectAsState()
    val skinTone by viewModel.skinTone.collectAsState()
    val faceSlimming by viewModel.faceSlimming.collectAsState()
    val eyeBrightening by viewModel.eyeBrightening.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val processedPreview by viewModel.processedPreview.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val hasPortrait by viewModel.hasPortrait.collectAsState()
    val faceCount by viewModel.faceCount.collectAsState()

    // 进出场动画
    var isVisible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "portrait_overlay_alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 120f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "portrait_overlay_offset"
    )

    LaunchedEffect(Unit) { isVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffset
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small, vertical = DesignSystem.Spacing.xxSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    isVisible = false
                    // 延迟关闭以播放动画
                    kotlinx.coroutines.MainScope().launch {
                        delay(400)
                        onDismiss()
                    }
                }) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "人像模式",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {
                    isVisible = false
                    kotlinx.coroutines.MainScope().launch {
                        delay(400)
                        onDismiss()
                    }
                }) {
                    Text("完成", color = DesignSystem.Colors.primary)
                }
            }

            // 人像预览区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
                    .height(280.dp)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.08f),
                contentAlignment = Alignment.Center
            ) {
                if (processedPreview != null) {
                    Image(
                        bitmap = processedPreview!!.asImageBitmap(),
                        contentDescription = "人像预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = DesignSystem.Colors.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                        Text(
                            "处理中…",
                            style = DesignSystem.Typography.subheadline,
                            color = DesignSystem.Colors.minimalLabelSecondary
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = null,
                            tint = DesignSystem.Colors.minimalLabelTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                        Text(
                            "人像预览",
                            style = DesignSystem.Typography.subheadline,
                            color = DesignSystem.Colors.minimalLabelTertiary
                        )
                        if (faceCount > 0) {
                            Text(
                                "检测到 ${faceCount} 张人脸",
                                style = DesignSystem.Typography.caption1,
                                color = DesignSystem.Colors.minimalLabelQuaternary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 预设选择
            Text(
                "预设",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxxSmall)
            ) {
                PortraitBeautyPreset.entries.forEach { preset ->
                    val isSelected = preset == currentPreset
                    val bgColor = if (isSelected) DesignSystem.Colors.primary
                    else DesignSystem.Colors.minimalOverlayMedium
                    val textColor = if (isSelected) Color.White
                    else DesignSystem.Colors.minimalLabelSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
                            .background(bgColor)
                            .clickable { viewModel.applyPreset(preset) }
                            .padding(vertical = DesignSystem.Spacing.xxSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            preset.displayName,
                            style = DesignSystem.Typography.caption1,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 美颜参数调节
            Text(
                "美颜参数",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

            // 参数调节卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.06f)
                    .padding(DesignSystem.Spacing.small)
            ) {
                // 磨皮
                BeautySliderRow(
                    label = "磨皮",
                    icon = Icons.Default.Face,
                    value = skinSmoothing,
                    onValueChange = { viewModel.setSkinSmoothing(it) },
                    valueRange = 0f..1f
                )

                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

                // 美白
                BeautySliderRow(
                    label = "美白",
                    icon = Icons.Default.BrightnessHigh,
                    value = skinTone,
                    onValueChange = { viewModel.setSkinTone(it) },
                    valueRange = -1f..1f,
                    displayTransform = { value ->
                        when {
                            value < -0.3f -> "冷白"
                            value < 0.3f -> "自然"
                            else -> "暖黄"
                        }
                    }
                )

                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

                // 瘦脸
                BeautySliderRow(
                    label = "瘦脸",
                    icon = Icons.Default.Face3,
                    value = faceSlimming,
                    onValueChange = { viewModel.setFaceSlimming(it) },
                    valueRange = 0f..1f
                )

                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

                // 大眼
                BeautySliderRow(
                    label = "大眼",
                    icon = Icons.Default.RemoveRedEye,
                    value = eyeBrightening,
                    onValueChange = { viewModel.setEyeBrightening(it) },
                    valueRange = 0f..1f
                )
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 人像虚化
            val portraitBlur by viewModel.portraitBlur.collectAsState()
            Text(
                "人像虚化",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.06f)
                    .padding(DesignSystem.Spacing.small)
            ) {
                BeautySliderRow(
                    label = "虚化",
                    icon = Icons.Default.BlurOn,
                    value = portraitBlur,
                    onValueChange = { viewModel.setPortraitBlur(it) },
                    valueRange = 0f..1f
                )
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 皮肤保护开关
            var skinProtectionEnabled by remember { mutableStateOf(true) }
            Text(
                "AI皮肤保护",
                style = DesignSystem.Typography.headline,
                color = DesignSystem.Colors.minimalLabel,
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .liquidGlass(cornerRadius = DesignSystem.CornerRadius.large, intensity = 0.06f)
                    .padding(horizontal = DesignSystem.Spacing.small, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Face,
                    null,
                    tint = if (skinProtectionEnabled) DesignSystem.Colors.primary
                    else DesignSystem.Colors.minimalLabelTertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "皮肤保护",
                        style = DesignSystem.Typography.headline,
                        color = DesignSystem.Colors.textPrimary()
                    )
                    Text(
                        "应用滤镜时保护皮肤区域不被过度染色",
                        style = DesignSystem.Typography.caption2,
                        color = DesignSystem.Colors.minimalLabelSecondary
                    )
                }
                Switch(
                    checked = skinProtectionEnabled,
                    onCheckedChange = {
                        skinProtectionEnabled = it
                        skinProtectionFilter.skinFilterIntensity = if (it) 0.3f else 1.0f
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DesignSystem.Colors.primary,
                        uncheckedThumbColor = DesignSystem.Colors.gray4(),
                        uncheckedTrackColor = DesignSystem.Colors.gray3()
                    )
                )
            }

            Spacer(Modifier.height(DesignSystem.Spacing.medium))

            // 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xSmall)
            ) {
                OutlinedButton(
                    onClick = {
                        isVisible = false
                        kotlinx.coroutines.MainScope().launch {
                            delay(400)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                    border = BorderStroke(DesignSystem.Stroke.widthStandard, DesignSystem.Colors.minimalBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignSystem.Colors.minimalLabel)
                ) {
                    Text("取消", style = DesignSystem.Typography.headline)
                }
                Button(
                    onClick = {
                        isVisible = false
                        kotlinx.coroutines.MainScope().launch {
                            delay(400)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignSystem.Colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("应用", style = DesignSystem.Typography.headline)
                }
            }

            Spacer(Modifier.height(DesignSystem.Spacing.large))
        }
    }
}

@Composable
private fun BeautySliderRow(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayTransform: ((Float) -> String)? = null
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = DesignSystem.Colors.minimalLabelSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(DesignSystem.Spacing.xxxSmall))
            Text(
                label,
                style = DesignSystem.Typography.subheadline,
                color = DesignSystem.Colors.minimalLabel
            )
            Spacer(Modifier.weight(1f))
            Text(
                displayTransform?.invoke(value) ?: "${(value * 100).toInt()}%",
                style = DesignSystem.Typography.caption1,
                color = DesignSystem.Colors.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = DesignSystem.Colors.primary,
                activeTrackColor = DesignSystem.Colors.primary,
                inactiveTrackColor = DesignSystem.Colors.minimalOverlayMedium,
                activeTickColor = DesignSystem.Colors.primary,
                inactiveTickColor = DesignSystem.Colors.minimalOverlay
            )
        )
    }
}
