package com.livecompose.livecapture.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    var logoBitmapPath by remember(watermark.logoBitmapPath) { mutableStateOf(watermark.logoBitmapPath) }
    var logoScale by remember(watermark.logoScale) { mutableFloatStateOf(watermark.logoScale) }
    var logoAlpha by remember(watermark.logoAlpha) { mutableFloatStateOf(watermark.logoAlpha) }
    val context = LocalContext.current

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // 将 content URI 转换为实际文件路径
            val path = try {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            // 复制到应用缓存目录
                            val inputStream = context.contentResolver.openInputStream(it)
                            val cacheFile = java.io.File(context.cacheDir, "watermark_logo_${System.currentTimeMillis()}.png")
                            inputStream?.use { input ->
                                java.io.FileOutputStream(cacheFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            cacheFile.absolutePath
                        } else null
                    } else null
                }
            } catch (e: Exception) {
                null
            }
            if (path != null) {
                logoBitmapPath = path
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(DesignSystem.Colors.minimalBackground)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("水印编辑", color = DesignSystem.Colors.minimalLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("取消", color = DesignSystem.Colors.minimalSecondaryLabel) }
            TextButton(onClick = {
                onWatermarkChanged(
                    watermark.copy(
                        text = text,
                        positionX = position,
                        alpha = alpha,
                        logoBitmapPath = logoBitmapPath,
                        logoScale = logoScale,
                        logoAlpha = logoAlpha
                    )
                )
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
                focusedTextColor = DesignSystem.Colors.minimalLabel,
                unfocusedTextColor = DesignSystem.Colors.minimalLabel,
                focusedBorderColor = DesignSystem.Colors.primary,
                unfocusedBorderColor = DesignSystem.Colors.minimalBorder,
                focusedLabelColor = DesignSystem.Colors.primary,
                unfocusedLabelColor = DesignSystem.Colors.minimalSecondaryLabel
            )
        )

        Spacer(Modifier.height(12.dp))

        // 图片水印选择
        Text("图片水印", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DesignSystem.Colors.primary
                )
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("选择图片", fontSize = 13.sp)
            }

            if (logoBitmapPath != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "已选择图片",
                    color = DesignSystem.Colors.success,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { logoBitmapPath = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "清除",
                        tint = DesignSystem.Colors.minimalSecondaryLabel,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 图片水印缩放
        if (logoBitmapPath != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("图片缩放", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 13.sp, modifier = Modifier.width(52.dp))
                Slider(
                    value = logoScale,
                    onValueChange = { logoScale = it },
                    valueRange = 0.05f..0.5f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = DesignSystem.Colors.minimalLabel,
                        activeTrackColor = DesignSystem.Colors.primary,
                        inactiveTrackColor = DesignSystem.Colors.minimalBorder
                    )
                )
                Text("%.0f%%".format(logoScale * 100), color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("图片透明度", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 13.sp, modifier = Modifier.width(52.dp))
                Slider(
                    value = logoAlpha,
                    onValueChange = { logoAlpha = it },
                    valueRange = 0.1f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = DesignSystem.Colors.minimalLabel,
                        activeTrackColor = DesignSystem.Colors.primary,
                        inactiveTrackColor = DesignSystem.Colors.minimalBorder
                    )
                )
                Text("%.0f%%".format(logoAlpha * 100), color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // 位置选择
        Text("位置", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 14.sp)
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
                        selectedLabelColor = DesignSystem.Colors.minimalLabel
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
                        selectedLabelColor = DesignSystem.Colors.minimalLabel
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 透明度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("透明度", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 13.sp, modifier = Modifier.width(52.dp))
            Slider(
                value = alpha,
                onValueChange = { alpha = it },
                valueRange = 0.1f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = DesignSystem.Colors.minimalLabel,
                    activeTrackColor = DesignSystem.Colors.primary,
                    inactiveTrackColor = DesignSystem.Colors.minimalBorder
                )
            )
            Text("%.0f%%".format(alpha * 100), color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 11.sp, modifier = Modifier.width(40.dp))
        }

        Spacer(Modifier.height(12.dp))

        // 启用开关
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("启用水印", color = DesignSystem.Colors.minimalLabel, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = watermark.isEnabled,
                onCheckedChange = {
                    onWatermarkChanged(watermark.copy(
                        isEnabled = it,
                        text = text,
                        positionX = position,
                        alpha = alpha,
                        logoBitmapPath = logoBitmapPath,
                        logoScale = logoScale,
                        logoAlpha = logoAlpha
                    ))
                },
                colors = SwitchDefaults.colors(checkedTrackColor = DesignSystem.Colors.primary)
            )
        }
    }
}
