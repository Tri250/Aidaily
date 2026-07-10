package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 预设快捷栏
 *
 * 对标 OPPO Find X9 哈苏大师预设栏，提供：
 * 1. 横向滑动预设列表
 * 2. 当前选中预设高亮
 * 3. AI 推荐预设标记
 * 4. 预设强度快捷调节
 * 5. 对比视图入口
 */
@Composable
fun PresetQuickBar(
    presets: List<MasterPreset>,
    recommendedPresets: List<MasterPreset>,
    selectedPreset: MasterPreset?,
    intensity: Float,
    onPresetSelected: (MasterPreset) -> Unit,
    onClearPreset: () -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onCompareRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 预设列表
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 无预设选项
            item {
                PresetChip(
                    name = "原图",
                    isSelected = selectedPreset == null,
                    isRecommended = false,
                    icon = Icons.Filled.CameraAlt,
                    onClick = onClearPreset
                )
            }

            // 推荐预设
            items(recommendedPresets) { preset ->
                PresetChip(
                    name = preset.name,
                    isSelected = selectedPreset?.name == preset.name,
                    isRecommended = true,
                    icon = presetIcon(preset),
                    onClick = { onPresetSelected(preset) }
                )
            }

            // 分隔线
            if (presets.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(DesignSystem.Colors.textTertiary().copy(alpha = 0.3f))
                    )
                }
            }

            // 全部预设
            items(presets.filter { it.name !in recommendedPresets.map { r -> r.name } }.take(12)) { preset ->
                PresetChip(
                    name = preset.name,
                    isSelected = selectedPreset?.name == preset.name,
                    isRecommended = false,
                    icon = presetIcon(preset),
                    onClick = { onPresetSelected(preset) }
                )
            }
        }

        // 强度调节条（仅当有预设选中时显示）
        if (selectedPreset != null) {
            Spacer(Modifier.height(4.dp))
            PresetIntensityBar(
                intensity = intensity,
                onIntensityChanged = onIntensityChanged,
                onCompareRequested = onCompareRequested
            )
        }
    }
}

@Composable
private fun PresetChip(
    name: String,
    isSelected: Boolean,
    isRecommended: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) DesignSystem.Colors.primary
        else DesignSystem.Colors.backgroundSecondary(),
        animationSpec = tween(200),
        label = "presetBg"
    )
    val textColor by animateColorAsState(
        if (isSelected) Color.White
        else DesignSystem.Colors.textPrimary(),
        animationSpec = tween(200),
        label = "presetText"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, DesignSystem.Colors.primary, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // AI 推荐标记
        if (isRecommended && !isSelected) {
            Text(
                "AI",
                fontSize = 8.sp,
                color = DesignSystem.Colors.accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
        Icon(
            icon,
            contentDescription = name,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            name,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PresetIntensityBar(
    intensity: Float,
    onIntensityChanged: (Float) -> Unit,
    onCompareRequested: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Tune,
            contentDescription = "强度",
            tint = DesignSystem.Colors.textSecondary(),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))

        Slider(
            value = intensity,
            onValueChange = onIntensityChanged,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = DesignSystem.Colors.primary,
                activeTrackColor = DesignSystem.Colors.primary
            )
        )

        Text(
            "${(intensity * 100).toInt()}%",
            fontSize = 11.sp,
            color = DesignSystem.Colors.textSecondary(),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(4.dp))

        // 对比按钮
        IconButton(
            onClick = onCompareRequested,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Filled.Compare,
                contentDescription = "对比",
                tint = DesignSystem.Colors.textSecondary(),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 根据预设名称推断图标
 */
private fun presetIcon(preset: MasterPreset): ImageVector {
    val name = preset.name
    val tags = preset.tags.map { it.lowercase() }

    return when {
        name.contains("蓝调") || name.contains("蓝") -> Icons.Filled.WaterDrop
        name.contains("黑白") -> Icons.Filled.Contrast
        name.contains("胶片") || name.contains("富士") -> Icons.Filled.Movie
        name.contains("童话") || name.contains("梦幻") -> Icons.Filled.AutoAwesome
        name.contains("美食") || name.contains("美味") -> Icons.Filled.Restaurant
        name.contains("哈苏") -> Icons.Filled.Camera
        name.contains("徕卡") -> Icons.Filled.Lens
        name.contains("德味") -> Icons.Filled.TempleBuddhist
        name.contains("人文") || name.contains("清新") -> Icons.Filled.Person
        name.contains("夜景") || name.contains("雪夜") -> Icons.Filled.Nightlight
        name.contains("复古") || name.contains("晴天") -> Icons.Filled.WbSunny
        name.contains("假日") -> Icons.Filled.BeachAccess
        name.contains("理光") -> Icons.Filled.Palette
        tags.contains("portrait") || tags.contains("人像") -> Icons.Filled.Face
        tags.contains("landscape") || tags.contains("风光") -> Icons.Filled.Landscape
        tags.contains("food") || tags.contains("美食") -> Icons.Filled.Restaurant
        tags.contains("night") || tags.contains("夜景") -> Icons.Filled.Nightlight
        tags.contains("city") || tags.contains("城市") -> Icons.Filled.LocationCity
        else -> Icons.Filled.PhotoFilter
    }
}