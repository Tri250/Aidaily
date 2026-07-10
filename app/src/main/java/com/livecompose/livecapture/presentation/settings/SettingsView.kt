package com.livecompose.livecapture.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.core.design.TitleTextStyle

@Composable
fun SettingsView() {
    var detectionMode by remember { mutableStateOf(DetectionMode.FAST) }
    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var captureDelay by remember { mutableStateOf(0) }
    var isDarkTheme by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = TitleTextStyle,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Detection Mode
        SettingsSection(title = "检测模式") {
            DetectionMode.values().forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = detectionMode == mode,
                            onClick = { detectionMode = mode },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = detectionMode == mode,
                        onClick = null
                    )
                    Text(
                        text = mode.label,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Auto Capture
        SettingsSection(title = "自动拍摄") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("对齐后自动拍摄")
                Switch(
                    checked = autoCaptureEnabled,
                    onCheckedChange = { autoCaptureEnabled = it }
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Capture Delay
        SettingsSection(title = "拍摄延迟") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${captureDelay} 秒")
                Slider(
                    value = captureDelay.toFloat(),
                    onValueChange = { captureDelay = it.toInt() },
                    valueRange = 0f..5f,
                    steps = 4,
                    modifier = Modifier.width(200.dp)
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Theme
        SettingsSection(title = "主题") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("深色模式")
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { isDarkTheme = it }
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // About
        SettingsSection(title = "关于") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("构妙 LiveCapture v1.0.0")
                Text(
                    "基于强化学习的 AI 端侧智能构图辅助",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "模型: MobileNetV3-Small (蒸馏)",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "框架: TensorFlow Lite",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

enum class DetectionMode(val label: String) {
    FAST("快速 (MobileNetV3)"),
    PRO("专业 (ResNet50)")
}
