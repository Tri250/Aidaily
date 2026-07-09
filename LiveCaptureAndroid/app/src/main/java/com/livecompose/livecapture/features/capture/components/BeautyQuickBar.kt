package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 美颜预设
 */
enum class BeautyPreset(val displayName: String) {
    NATURAL("自然"),
    FAIR("白皙"),
    VIBRANT("元气"),
    PREMIUM("高级感"),
    CUSTOM("自定义")
}

/**
 * 快调美颜参数
 */
data class BeautyQuickParams(
    val smoothing: Float = 0.5f,    // 磨皮 0-1
    val whitening: Float = 0.3f,    // 美白 0-1
    val slimFace: Float = 0.3f,     // 瘦脸 0-1
    val enlargeEye: Float = 0.2f    // 大眼 0-1
)

/** 暖金色 - 美颜开关激活状态 */
private val AccentWarm = Color(0xFFD4A76A)

/**
 * 预设 → 快调参数映射
 */
private fun presetParams(preset: BeautyPreset): BeautyQuickParams = when (preset) {
    BeautyPreset.NATURAL -> BeautyQuickParams(
        smoothing = 0.2f, whitening = 0.1f, slimFace = 0.05f, enlargeEye = 0.05f
    )
    BeautyPreset.FAIR -> BeautyQuickParams(
        smoothing = 0.5f, whitening = 0.6f, slimFace = 0.2f, enlargeEye = 0.15f
    )
    BeautyPreset.VIBRANT -> BeautyQuickParams(
        smoothing = 0.4f, whitening = 0.3f, slimFace = 0.25f, enlargeEye = 0.3f
    )
    BeautyPreset.PREMIUM -> BeautyQuickParams(
        smoothing = 0.3f, whitening = 0.15f, slimFace = 0.15f, enlargeEye = 0.1f
    )
    BeautyPreset.CUSTOM -> BeautyQuickParams()
}

/**
 * 重设计美颜快调条 - 国潮质感实时预览风格
 *
 * 特性：
 * - AI预设横向滑动（自然/白皙/元气/高级感/自定义）
 * - 4个快调滑块（磨皮/美白/瘦脸/大眼），每行两个
 * - 点击"展开全部"进入完整美颜面板
 * - 美颜开/关一键切换
 * - 实时预览（调节即见效果）
 */
@Composable
fun BeautyQuickBar(
    params: BeautyQuickParams,
    currentPreset: BeautyPreset,
    isBeautyEnabled: Boolean,
    onParamsChange: (BeautyQuickParams) -> Unit,
    onPresetChange: (BeautyPreset) -> Unit,
    onToggleBeauty: () -> Unit,
    onExpandFull: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Top row: title + beauty toggle ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "美颜",
                color = DesignSystem.Colors.minimalLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            BeautyToggle(
                isBeautyEnabled = isBeautyEnabled,
                onToggle = onToggleBeauty,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // ── Preset chips ──
        PresetChipRow(
            currentPreset = currentPreset,
            onPresetChange = onPresetChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ── Quick adjust sliders (2×2 grid) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickSliderColumn(
                label = "磨皮",
                value = params.smoothing,
                onValueChange = { new ->
                    onParamsChange(params.copy(smoothing = new))
                },
                modifier = Modifier.weight(1f)
            )
            QuickSliderColumn(
                label = "美白",
                value = params.whitening,
                onValueChange = { new ->
                    onParamsChange(params.copy(whitening = new))
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickSliderColumn(
                label = "瘦脸",
                value = params.slimFace,
                onValueChange = { new ->
                    onParamsChange(params.copy(slimFace = new))
                },
                modifier = Modifier.weight(1f)
            )
            QuickSliderColumn(
                label = "大眼",
                value = params.enlargeEye,
                onValueChange = { new ->
                    onParamsChange(params.copy(enlargeEye = new))
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Expand button ──
        Text(
            text = "展开全部美颜参数 ▸",
            color = DesignSystem.Colors.minimalSecondaryLabel,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpandFull() }
                .padding(vertical = 4.dp)
        )
    }
}

// MARK: - Beauty Toggle ✨

@Composable
private fun BeautyToggle(
    isBeautyEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = DesignSystem.Animation.quick,
        label = "beautyToggleScale"
    )
    val iconColor = if (isBeautyEnabled) AccentWarm else DesignSystem.Colors.minimalSecondaryLabel

    Box(
        modifier = modifier
            .size(36.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isBeautyEnabled) AccentWarm.copy(alpha = 0.15f) else Color.Transparent
            )
            .then(
                if (isBeautyEnabled) Modifier.border(
                    width = 1.dp,
                    color = AccentWarm.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(18.dp)
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✨",
            fontSize = 16.sp,
            color = iconColor
        )
    }
}

// MARK: - Preset Chip Row

@Composable
private fun PresetChipRow(
    currentPreset: BeautyPreset,
    onPresetChange: (BeautyPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BeautyPreset.entries.forEach { preset ->
            val selected = currentPreset == preset
            val bgColor = if (selected) {
                DesignSystem.Colors.primary.copy(alpha = 0.25f)
            } else {
                DesignSystem.Colors.minimalOverlay
            }
            val textColor = if (selected) {
                DesignSystem.Colors.minimalLabel
            } else {
                DesignSystem.Colors.minimalSecondaryLabel
            }
            val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

            Text(
                text = preset.displayName,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = fontWeight,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .then(
                        if (selected) Modifier.border(
                            width = 1.dp,
                            color = DesignSystem.Colors.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        ) else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onPresetChange(preset)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

// MARK: - Quick Slider Column (label + slider + value)

@Composable
private fun QuickSliderColumn(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = DesignSystem.Colors.minimalLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(40.dp)
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = DesignSystem.Colors.primary,
                    activeTrackColor = DesignSystem.Colors.primary,
                    inactiveTrackColor = DesignSystem.Colors.minimalOverlay
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(value * 100).toInt()}",
                color = DesignSystem.Colors.minimalSecondaryLabel,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}
