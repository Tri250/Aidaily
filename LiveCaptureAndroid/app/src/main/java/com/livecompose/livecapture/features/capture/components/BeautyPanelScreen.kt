package com.livecompose.livecapture.features.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.portrait.BeautySettings
import com.livecompose.livecapture.core.portrait.BeautySliderDescriptor
import com.livecompose.livecapture.core.portrait.BeautySliders
import com.livecompose.livecapture.core.portrait.toBeautySettings

// 美颜面板深色风格（对应 iOS BeautyPanelView 的黑色半透明背景）
private val PanelBackground = Color.Black.copy(alpha = 0.92f)
private val PanelLabel = Color.White.copy(alpha = 0.92f)
private val PanelSecondaryLabel = Color.White.copy(alpha = 0.50f)
private val PanelBorder = Color.White.copy(alpha = 0.20f)
private val PanelAccent = Color(0xFF3B82F6)

/**
 * 美颜调节面板
 *
 * 对应 iOS 端 BeautyPanelView，以底部面板形式呈现：包含预设选择器、
 * 7 项美颜参数滑块（磨皮/美白/祛痘/亮眼/牙齿美白/瘦脸/红润）、重置与关闭按钮，
 * 支持下拉拖拽关闭。
 *
 * @param settings 当前美颜设置
 * @param onSettingsChange 设置变更回调
 * @param onClose 关闭面板回调
 */
@Composable
fun BeautyPanelScreen(
    settings: BeautySettings,
    onSettingsChange: (BeautySettings) -> Unit,
    onClose: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(PanelBackground)
            .padding(bottom = 20.dp)
    ) {
        // 顶部拖拽区（指示器 + 标题栏）：仅此处响应下拉关闭手势，
        // 避免与下方滑块列表的滚动冲突。
        Column(
            modifier = Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffset > 240f) {
                            AppLogger.d("BeautyPanel", "下拉关闭美颜面板")
                            onClose()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f }
                ) { _, dragAmount ->
                    if (dragAmount.y > 0) dragOffset += dragAmount.y
                }
            }
        ) {
            // 拖拽指示器
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(PanelSecondaryLabel)
            )

            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "美颜",
                    color = PanelLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // 重置按钮
                TextButton(onClick = {
                    AppLogger.d("BeautyPanel", "重置美颜参数")
                    onSettingsChange(BeautySettings.DEFAULT.copy(isBeautyEnabled = settings.isBeautyEnabled))
                }) {
                    Text("重置", color = PanelSecondaryLabel, fontSize = 15.sp)
                }
                // 关闭按钮
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭美颜面板",
                        tint = PanelSecondaryLabel,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 预设选择器
        PresetSelector(
            current = settings.currentPreset,
            onSelect = { preset ->
                AppLogger.d("BeautyPanel", "应用美颜预设: ${preset.displayName}")
                onSettingsChange(preset.toBeautySettings())
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 美颜参数滑块列表
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            BeautySliders.all.forEachIndexed { index, descriptor ->
                BeautySliderRow(
                    descriptor = descriptor,
                    value = descriptor.getter(settings),
                    onValueChange = { newValue ->
                        // 调节任一参数即切换为自定义预设并启用美颜
                        onSettingsChange(
                            descriptor.setter(settings, newValue).copy(
                                currentPreset = com.livecompose.livecapture.core.portrait.BeautyPreset.CUSTOM,
                                isBeautyEnabled = true
                            )
                        )
                    }
                )
                if (index != BeautySliders.all.lastIndex) {
                    HorizontalDivider(
                        color = PanelBorder,
                        modifier = Modifier.padding(start = 56.dp)
                    )
                }
            }
        }
    }
}

// MARK: - 预设选择器

@Composable
private fun PresetSelector(
    current: com.livecompose.livecapture.core.portrait.BeautyPreset,
    onSelect: (com.livecompose.livecapture.core.portrait.BeautyPreset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        com.livecompose.livecapture.core.portrait.BeautyPreset.entries.forEach { preset ->
            val selected = current == preset
            Text(
                text = preset.displayName,
                color = if (selected) PanelLabel else PanelSecondaryLabel,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) PanelAccent.copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.08f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) PanelAccent.copy(alpha = 0.5f) else PanelBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(preset) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// MARK: - 单个美颜滑块行

@Composable
private fun BeautySliderRow(
    descriptor: BeautySliderDescriptor,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 图标
        Icon(
            imageVector = descriptor.icon,
            contentDescription = null,
            tint = PanelSecondaryLabel,
            modifier = Modifier.size(20.dp)
        )
        // 标签
        Text(
            text = descriptor.label,
            color = PanelLabel,
            fontSize = 15.sp,
            modifier = Modifier.width(56.dp)
        )
        // 滑块
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = descriptor.rangeMin..descriptor.rangeMax,
            colors = SliderDefaults.colors(
                thumbColor = PanelAccent,
                activeTrackColor = PanelAccent,
                inactiveTrackColor = PanelBorder
            ),
            modifier = Modifier.weight(1f)
        )
        // 百分比显示
        Text(
            text = percentageText(value, descriptor.rangeMin, descriptor.rangeMax),
            color = PanelSecondaryLabel,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/**
 * 计算滑块百分比文本。
 * 范围下界为负时（如美白 -1..1）先归一化到 0..1 再显示百分比。
 */
private fun percentageText(value: Float, rangeMin: Float, rangeMax: Float): String {
    return if (rangeMin < 0) {
        val normalized = (value - rangeMin) / (rangeMax - rangeMin)
        "${(normalized * 100).toInt()}%"
    } else {
        "${(value * 100).toInt()}%"
    }
}
