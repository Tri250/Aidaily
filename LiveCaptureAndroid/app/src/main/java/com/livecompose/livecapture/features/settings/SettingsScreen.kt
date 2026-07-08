package com.livecompose.livecapture.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.core.detection.DetectionMode
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 设置界面
 * 对应 iOS 的 SettingsView
 */
@Composable
fun SettingsScreen() {
    var detectionMode by remember { mutableStateOf(DetectionMode.FAST) }
    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var captureDelay by remember { mutableStateOf(1.0) }
    var colorScheme by remember { mutableStateOf("system") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("设置", style = DesignSystem.Typography.largeTitle, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(24.dp))

        // 外观
        Text("外观", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = DesignSystem.Colors.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("主题模式", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Text("切换深色 / 浅色外观", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                }
                Spacer(modifier = Modifier.weight(1f))
                var selectedIndex by remember { mutableIntStateOf(0) }
                val options = listOf("系统", "浅色", "深色")
                options.forEachIndexed { index, text ->
                    TextButton(onClick = {
                        selectedIndex = index
                        colorScheme = when (index) { 1 -> "light"; 2 -> "dark"; else -> "system" }
                    }) {
                        Text(text, color = if (selectedIndex == index) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 拍摄设置
        Text("拍摄设置", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动拍照", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                        Text("对准构图框后自动触发拍摄", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
                    }
                    Switch(checked = autoCaptureEnabled, onCheckedChange = { autoCaptureEnabled = it })
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = DesignSystem.Colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("拍照延迟", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${"%.1f".format(captureDelay)}秒", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { delay ->
                        TextButton(onClick = { captureDelay = delay }) {
                            Text(
                                "${"%.1f".format(delay)}秒",
                                color = if (captureDelay == delay) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 构图引擎
        Text("构图引擎", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = DesignSystem.mediumRoundedShape,
            colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                var selectedMode by remember { mutableIntStateOf(1) }
                Row {
                    DetectionMode.entries.forEachIndexed { index, mode ->
                        TextButton(onClick = {
                            selectedMode = index
                            detectionMode = mode
                        }) {
                            Text(mode.displayName, color = if (selectedMode == index) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(detectionMode.description, style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
            }
        }
    }
}