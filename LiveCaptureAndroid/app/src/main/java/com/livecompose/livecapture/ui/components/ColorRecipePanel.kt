package com.livecompose.livecapture.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.livecompose.livecapture.core.lut.BuiltInPresets
import com.livecompose.livecapture.core.lut.ColorRecipeParams
import com.livecompose.livecapture.core.lut.LutCategory
import com.livecompose.livecapture.core.lut.LutPreset
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 色彩配方面板
 * 支持预设选择 + 手动参数调整
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ColorRecipePanel(
    currentPreset: LutPreset,
    currentParams: ColorRecipeParams,
    onPresetSelected: (LutPreset) -> Unit,
    onParamsChanged: (ColorRecipeParams) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(LutCategory.STANDARD) }
    var showAdvanced by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(DesignSystem.Colors.minimalBackground)
            .padding(16.dp)
    ) {
        // 标题行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("色彩配方", color = DesignSystem.Colors.minimalLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) { Text("重置", color = DesignSystem.Colors.minimalSecondaryLabel) }
            TextButton(onClick = onApply) { Text("应用", color = DesignSystem.Colors.primary) }
        }

        Spacer(Modifier.height(8.dp))

        // 分类标签
        ScrollableTabRow(
            selectedTabIndex = LutCategory.entries.indexOf(selectedCategory),
            containerColor = Color.Transparent,
            contentColor = DesignSystem.Colors.minimalLabel,
            edgePadding = 0.dp,
            divider = {}
        ) {
            LutCategory.entries.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = { Text(category.displayName, fontSize = 13.sp) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 预设列表（水平滚动）
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BuiltInPresets.getByCategory(selectedCategory).forEach { preset ->
                PresetChip(
                    preset = preset,
                    isSelected = currentPreset.id == preset.id,
                    onClick = { onPresetSelected(preset) }
                )
            }
            // 也显示其他分类的预设
            if (selectedCategory == LutCategory.STANDARD) {
                BuiltInPresets.presets.filter { it.category != LutCategory.STANDARD }.forEach { preset ->
                    PresetChip(
                        preset = preset,
                        isSelected = currentPreset.id == preset.id,
                        onClick = { onPresetSelected(preset) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 手动调整滑块
        Text("手动调整", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))

        AdjustableSlider("曝光", currentParams.exposure, -2f..2f) { onParamsChanged(currentParams.copy(exposure = it)) }
        AdjustableSlider("对比度", currentParams.contrast, -100f..100f) { onParamsChanged(currentParams.copy(contrast = it)) }
        AdjustableSlider("高光", currentParams.highlights, -100f..100f) { onParamsChanged(currentParams.copy(highlights = it)) }
        AdjustableSlider("阴影", currentParams.shadows, -100f..100f) { onParamsChanged(currentParams.copy(shadows = it)) }
        AdjustableSlider("饱和度", currentParams.saturation, -100f..100f) { onParamsChanged(currentParams.copy(saturation = it)) }
        AdjustableSlider("色温", currentParams.temperature, -100f..100f) { onParamsChanged(currentParams.copy(temperature = it)) }
        AdjustableSlider("色调", currentParams.tint, -100f..100f) { onParamsChanged(currentParams.copy(tint = it)) }

        // 高级选项
        TextButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "收起高级选项" else "展开高级选项", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 12.sp)
        }

        if (showAdvanced) {
            AdjustableSlider("自然饱和度", currentParams.vibrance, -100f..100f) { onParamsChanged(currentParams.copy(vibrance = it)) }
            AdjustableSlider("白色", currentParams.whites, -100f..100f) { onParamsChanged(currentParams.copy(whites = it)) }
            AdjustableSlider("黑色", currentParams.blacks, -100f..100f) { onParamsChanged(currentParams.copy(blacks = it)) }
            AdjustableSlider("褪色", currentParams.fade, 0f..100f) { onParamsChanged(currentParams.copy(fade = it)) }
            AdjustableSlider("颗粒", currentParams.grain, 0f..100f) { onParamsChanged(currentParams.copy(grain = it)) }
            AdjustableSlider("晕影", currentParams.vignette, 0f..100f) { onParamsChanged(currentParams.copy(vignette = it)) }
            AdjustableSlider("锐化", currentParams.sharpening, 0f..100f) { onParamsChanged(currentParams.copy(sharpening = it)) }
            AdjustableSlider("Bloom", currentParams.bloom, 0f..100f) { onParamsChanged(currentParams.copy(bloom = it)) }
            AdjustableSlider("留银冲洗", currentParams.bleach, 0f..100f) { onParamsChanged(currentParams.copy(bleach = it)) }
        }
    }
}

@Composable
private fun PresetChip(preset: LutPreset, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) DesignSystem.Colors.primary else DesignSystem.Colors.minimalOverlay)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(preset.name, color = if (isSelected) DesignSystem.Colors.minimalLabel else DesignSystem.Colors.minimalSecondaryLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(preset.category.displayName, color = if (isSelected) DesignSystem.Colors.minimalSecondaryLabel else DesignSystem.Colors.minimalSecondaryLabel.copy(alpha = 0.5f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun AdjustableSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 12.sp, modifier = Modifier.width(52.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = DesignSystem.Colors.minimalLabel,
                activeTrackColor = DesignSystem.Colors.primary,
                inactiveTrackColor = DesignSystem.Colors.minimalBorder
            )
        )
        Text(
            "%.1f".format(value),
            color = DesignSystem.Colors.minimalSecondaryLabel,
            fontSize = 11.sp,
            modifier = Modifier.width(40.dp)
        )
    }
}
