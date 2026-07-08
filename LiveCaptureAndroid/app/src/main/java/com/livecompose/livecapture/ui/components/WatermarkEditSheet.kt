package com.livecompose.livecapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.frame.WatermarkInfo
import com.livecompose.livecapture.core.frame.WatermarkPosition
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 水印编辑面板
 */
@Composable
fun WatermarkEditSheet(
    watermark: WatermarkInfo,
    onWatermarkChanged: (WatermarkInfo) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(watermark.text) { mutableStateOf(watermark.text) }
    var position by remember(watermark.positionX) { mutableStateOf(watermark.positionX) }
    var alpha by remember(watermark.alpha) { mutableFloatStateOf(watermark.alpha) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("水印编辑", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("取消", color = Color.White.copy(alpha = 0.7f)) }
            TextButton(onClick = {
                onWatermarkChanged(watermark.copy(text = text, positionX = position, alpha = alpha))
                onApply()
            }) { Text("确定", color = DesignSystem.Colors.primary) }
        }

        Spacer(Modifier.height(16.dp))

        // 文字输入
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("水印文字") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = DesignSystem.Colors.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = DesignSystem.Colors.primary,
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
            )
        )

        Spacer(Modifier.height(12.dp))

        // 位置选择
        Text("位置", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WatermarkPosition.entries.take(3).forEach { pos ->
                FilterChip(
                    selected = position == pos,
                    onClick = { position = pos },
                    label = { Text(pos.name.replace("_", " "), fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DesignSystem.Colors.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WatermarkPosition.entries.drop(3).take(3).forEach { pos ->
                FilterChip(
                    selected = position == pos,
                    onClick = { position = pos },
                    label = { Text(pos.name.replace("_", " "), fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DesignSystem.Colors.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 透明度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("透明度", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.width(52.dp))
            Slider(
                value = alpha,
                onValueChange = { alpha = it },
                valueRange = 0.1f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = DesignSystem.Colors.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Text("%.0f%%".format(alpha * 100), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.width(40.dp))
        }

        Spacer(Modifier.height(12.dp))

        // 启用开关
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("启用水印", color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = watermark.isEnabled,
                onCheckedChange = {
                    onWatermarkChanged(watermark.copy(
                        isEnabled = it,
                        text = text,
                        positionX = position,
                        alpha = alpha
                    ))
                },
                colors = SwitchDefaults.colors(checkedTrackColor = DesignSystem.Colors.primary)
            )
        }
    }
}
