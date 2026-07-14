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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.livecompose.livecapture.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livecompose.livecapture.core.design.TitleTextStyle
import com.livecompose.livecapture.core.design.*
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
    val watermarkEnabled by viewModel.watermarkEnabled.collectAsStateWithLifecycle()
    val gridEnabled by viewModel.gridEnabled.collectAsStateWithLifecycle()
    val voiceCaptureDefault by viewModel.voiceCaptureDefault.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
    val sceneRecognitionEnabled by viewModel.sceneRecognitionEnabled.collectAsStateWithLifecycle()
    val aspectRatio by viewModel.aspectRatio.collectAsStateWithLifecycle()
    val selfCheckResults by viewModel.selfCheckResults.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.ExtraLarge)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = TitleTextStyle
            )
            androidx.compose.material3.IconButton(onClick = onDismiss) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = stringResource(R.string.settings_close)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        // Detection Mode
        SettingsSection(title = stringResource(R.string.settings_section_detection)) {
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
                        .padding(horizontal = Spacing.ExtraLarge),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = detectionMode == mode,
                        onClick = null
                    )
                    Text(
                        text = mode.label,
                        modifier = Modifier.padding(start = Spacing.ExtraLarge)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Auto Capture
        SettingsSection(title = stringResource(R.string.settings_section_auto_capture)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_auto_capture_label))
                Switch(
                    checked = autoCaptureEnabled,
                    onCheckedChange = { viewModel.setAutoCapture(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Capture Delay
        SettingsSection(title = stringResource(R.string.settings_section_delay)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
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

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Aspect Ratio
        val aspectRatioOptions = listOf("3:4", "1:1", "16:9", "Full")
        SettingsSection(title = "照片比例") {
            Column(modifier = Modifier.selectableGroup()) {
                aspectRatioOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = aspectRatio == option,
                                onClick = { viewModel.setAspectRatio(option) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = Spacing.ExtraLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = aspectRatio == option,
                            onClick = null
                        )
                        Text(
                            text = option,
                            modifier = Modifier.padding(start = Spacing.ExtraLarge)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Theme
        SettingsSection(title = stringResource(R.string.settings_section_theme)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_dark_theme))
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Torch
        SettingsSection(title = stringResource(R.string.settings_section_torch)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_torch_default))
                Switch(
                    checked = torchEnabled,
                    onCheckedChange = { viewModel.setTorchEnabled(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Grid
        SettingsSection(title = stringResource(R.string.settings_section_grid)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_grid_enable))
                Switch(
                    checked = gridEnabled,
                    onCheckedChange = { viewModel.setGridEnabled(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Watermark
        SettingsSection(title = stringResource(R.string.settings_section_watermark)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_watermark_enable))
                Switch(
                    checked = watermarkEnabled,
                    onCheckedChange = { viewModel.setWatermarkEnabled(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Voice Capture
        SettingsSection(title = stringResource(R.string.settings_section_voice)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_voice_default))
                Switch(
                    checked = voiceCaptureDefault,
                    onCheckedChange = { viewModel.setVoiceCaptureDefault(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Haptic
        SettingsSection(title = stringResource(R.string.settings_section_haptic)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_haptic_enable))
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = { viewModel.setHapticEnabled(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Scene Recognition
        SettingsSection(title = stringResource(R.string.settings_section_scene)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_scene_enable))
                Switch(
                    checked = sceneRecognitionEnabled,
                    onCheckedChange = { viewModel.setSceneRecognitionEnabled(it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // About
        SettingsSection(title = stringResource(R.string.settings_section_about)) {
            Column(modifier = Modifier.padding(Spacing.ExtraLarge)) {
                Text(stringResource(R.string.settings_app_name_version))
                Text(
                    stringResource(R.string.settings_about_desc),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(Spacing.Medium))
                Text(
                    stringResource(R.string.settings_model_info),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    stringResource(R.string.settings_framework_info),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Medium))

        // Self Check
        SettingsSection(title = stringResource(R.string.settings_section_self_check)) {
            Column(modifier = Modifier.padding(horizontal = Spacing.ExtraLarge)) {
                Button(
                    onClick = { viewModel.runSelfCheck() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_self_check_run))
                }
                if (selfCheckResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.Large))
                    val passCount = selfCheckResults.count { it.status == SelfChecker.CheckStatus.PASS }
                    val warnCount = selfCheckResults.count { it.status == SelfChecker.CheckStatus.WARN }
                    val failCount = selfCheckResults.count { it.status == SelfChecker.CheckStatus.FAIL }
                    Text(
                        stringResource(R.string.settings_self_check_result, passCount, warnCount, failCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (failCount > 0) MaterialTheme.colorScheme.error
                        else if (warnCount > 0) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.Medium))
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
                            modifier = Modifier.padding(vertical = Spacing.ExtraSmall)
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
            modifier = Modifier.padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Medium)
        )
        content()
    }
}
