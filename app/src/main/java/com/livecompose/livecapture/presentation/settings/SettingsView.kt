package com.livecompose.livecapture.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livecompose.livecapture.core.design.TitleTextStyle
import com.livecompose.livecapture.core.diagnostics.SelfChecker
import com.livecompose.livecapture.core.settings.DetectionMode

private val DetectionMode.label: String
    get() = when (this) {
        DetectionMode.FAST -> "快速 (Student 模型, 节电 ~5fps)"
        DetectionMode.PRO -> "专业 (Teacher 模型, 全帧率高精度)"
    }

@Composable
fun SettingsView(
    viewModel: SettingsViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val detectionMode by viewModel.detectionMode.collectAsStateWithLifecycle()
    val autoCaptureEnabled by viewModel.autoCapture.collectAsStateWithLifecycle()
    val captureDelay by viewModel.captureDelay.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()
    val selfCheckResults by viewModel.selfCheckResults.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "设置",
                style = TitleTextStyle
            )
            androidx.compose.material3.IconButton(onClick = onDismiss) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "关闭"
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Detection Mode
        SettingsSection(title = "检测模式") {
            DetectionMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = detectionMode == mode,
                            onClick = { viewModel.setDetectionMode(mode) },
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                    onCheckedChange = { viewModel.setAutoCapture(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Capture Delay
        SettingsSection(title = "拍摄延迟") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$captureDelay 秒")
                Slider(
                    value = captureDelay.toFloat(),
                    onValueChange = { viewModel.setCaptureDelay(it.toInt()) },
                    valueRange = 0f..5f,
                    steps = 4,
                    modifier = Modifier.width(200.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                    onCheckedChange = { viewModel.setDarkTheme(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Torch
        SettingsSection(title = "闪光灯") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("默认开启闪光灯")
                Switch(
                    checked = torchEnabled,
                    onCheckedChange = { viewModel.setTorchEnabled(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // About
        SettingsSection(title = "关于") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("构妙 LiveCapture v1.5.9")
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
                    "框架: TensorFlow Lite (端侧推理)",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Self Check
        SettingsSection(title = "设备自检") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = { viewModel.runSelfCheck() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("运行自检")
                }
                if (selfCheckResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val passCount = selfCheckResults.count { it.status == SelfChecker.CheckStatus.PASS }
                    val warnCount = selfCheckResults.count { it.status == SelfChecker.CheckStatus.WARN }
                    val failCount = selfCheckResults.count { it.status == SelfChecker.CheckStatus.FAIL }
                    Text(
                        "通过: $passCount | 警告: $warnCount | 失败: $failCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (failCount > 0) MaterialTheme.colorScheme.error
                        else if (warnCount > 0) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    selfCheckResults.forEach { item ->
                        val icon = when (item.status) {
                            SelfChecker.CheckStatus.PASS -> "✓"
                            SelfChecker.CheckStatus.WARN -> "⚠"
                            SelfChecker.CheckStatus.FAIL -> "✗"
                            SelfChecker.CheckStatus.INFO -> "ℹ"
                        }
                        val color = when (item.status) {
                            SelfChecker.CheckStatus.PASS -> MaterialTheme.colorScheme.primary
                            SelfChecker.CheckStatus.WARN -> MaterialTheme.colorScheme.tertiary
                            SelfChecker.CheckStatus.FAIL -> MaterialTheme.colorScheme.error
                            SelfChecker.CheckStatus.INFO -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                        Text(
                            text = "$icon [${item.category}] ${item.name}: ${item.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = color,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
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
