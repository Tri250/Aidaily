package com.livecompose.livecapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.lut.LchColorAdjustment
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * OKLCH 混色器面板
 * 9 通道色彩调整（皮肤/红/橙/黄/绿/青/蓝/紫/品红）
 */
@Composable
fun LchMixerPanel(
    adjustment: LchColorAdjustment,
    onAdjustmentChanged: (LchColorAdjustment) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChannel by remember { mutableIntStateOf(1) } // 默认选中红色

    Column(modifier = modifier.fillMaxWidth()) {
        // 标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "OKLCH 混色器",
                color = DesignSystem.Colors.minimalLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            // 重置按钮
            TextButton(onClick = { onAdjustmentChanged(LchColorAdjustment()) }) {
                Text("重置", color = DesignSystem.Colors.primary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 9 色环选择器
        ColorRingTabs(
            selectedIndex = selectedChannel,
            onChannelSelected = { selectedChannel = it }
        )

        Spacer(Modifier.height(16.dp))

        // 选中通道的 H/C/L 滑块
        val channel = adjustment.getChannel(selectedChannel)

        LchSlider(
            label = "色相 (Hue)",
            value = channel[0],
            onValueChange = { hue ->
                onAdjustmentChanged(adjustment.setChannel(selectedChannel, hue, channel[1], channel[2]))
            },
            valueRange = -1f..1f
        )

        Spacer(Modifier.height(8.dp))

        LchSlider(
            label = "彩度 (Chroma)",
            value = channel[1],
            onValueChange = { chroma ->
                onAdjustmentChanged(adjustment.setChannel(selectedChannel, channel[0], chroma, channel[2]))
            },
            valueRange = -1f..1f
        )

        Spacer(Modifier.height(8.dp))

        LchSlider(
            label = "明度 (Lightness)",
            value = channel[2],
            onValueChange = { lightness ->
                onAdjustmentChanged(adjustment.setChannel(selectedChannel, channel[0], channel[1], lightness))
            },
            valueRange = -1f..1f
        )
    }
}

@Composable
private fun ColorRingTabs(
    selectedIndex: Int,
    onChannelSelected: (Int) -> Unit
) {
    // 9 个通道的颜色
    val channelColors = listOf(
        Color(0xFFE8B88A), // 皮肤
        Color(0xFFE53935), // 红
        Color(0xFFFF9800), // 橙
        Color(0xFFFFEB3B), // 黄
        Color(0xFF4CAF50), // 绿
        Color(0xFF00BCD4), // 青
        Color(0xFF1E88E5), // 蓝
        Color(0xFF9C27B0), // 紫
        Color(0xFFE91E63)  // 品红
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        channelColors.forEachIndexed { index, color ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(2.dp, DesignSystem.Colors.minimalLabel, CircleShape)
                        else Modifier
                    )
                    .clickable { onChannelSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DesignSystem.Colors.minimalLabel)
                    )
                }
            }
        }
    }
}

@Composable
private fun LchSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -1f..1f
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 13.sp)
            Text(
                "%.2f".format(value),
                color = DesignSystem.Colors.minimalSecondaryLabel,
                fontSize = 12.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = DesignSystem.Colors.minimalLabel,
                activeTrackColor = DesignSystem.Colors.primary,
                inactiveTrackColor = DesignSystem.Colors.minimalBorder
            )
        )
    }
}
