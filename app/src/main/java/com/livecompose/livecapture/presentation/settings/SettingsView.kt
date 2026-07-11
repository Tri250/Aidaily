package com.livecompose.livecapture.presentation.settings

import androidx.compose.foundation.clickable
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.livecompose.livecapture.core.crash.CrashHandler.CrashLogEntry
import com.livecompose.livecapture.R
import com.livecompose.livecapture.core.design.TitleTextStyle
import com.livecompose.livecapture.core.perf.MemoryPressure
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DetectionMode(@StringRes val labelResId: Int, val key: String) {
    FAST(R.string.settings_detection_fast, "FAST"),
    PRO(R.string.settings_detection_pro, "PRO")
}

@Composable
fun SettingsView(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val detectionMode by viewModel.detectionMode.collectAsStateWithLifecycle()
    val autoCaptureEnabled by viewModel.autoCapture.collectAsStateWithLifecycle()
    val captureDelay by viewModel.captureDelay.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()

    // Developer options state
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()
    val fps by viewModel.fps.collectAsStateWithLifecycle()
    val memoryUsageMb by viewModel.memoryUsageMb.collectAsStateWithLifecycle()
    val memoryPressure by viewModel.memoryPressure.collectAsStateWithLifecycle()
    val jankCount by viewModel.jankCount.collectAsStateWithLifecycle()
    val crashLogs by viewModel.crashLogs.collectAsStateWithLifecycle()

    var showCrashLogDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = TitleTextStyle,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .semantics { heading() }
        )

        // Detection Mode
        SettingsSection(title = stringResource(R.string.setting_detection_mode)) {
            DetectionMode.entries.forEach { mode ->
                val modeLabel = stringResource(mode.labelResId)
                val isSelected = detectionMode == mode.key
                val radioDesc = if (isSelected) {
                    stringResource(R.string.a11y_radio_selected, modeLabel)
                } else {
                    stringResource(R.string.a11y_radio_not_selected, modeLabel)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { viewModel.setDetectionMode(mode.key) },
                            role = Role.RadioButton
                        )
                        .semantics {
                            contentDescription = radioDesc
                            stateDescription = if (isSelected) "已选中" else "未选中"
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                    Text(
                        text = modeLabel,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Auto Capture
        SettingsSection(title = stringResource(R.string.setting_auto_capture)) {
            val autoCaptureLabel = stringResource(R.string.settings_auto_capture_desc)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(autoCaptureLabel)
                Switch(
                    checked = autoCaptureEnabled,
                    onCheckedChange = { viewModel.setAutoCapture(it) },
                    modifier = Modifier.semantics {
                        contentDescription = if (autoCaptureEnabled) {
                            stringResource(R.string.a11y_switch_on, autoCaptureLabel)
                        } else {
                            stringResource(R.string.a11y_switch_off, autoCaptureLabel)
                        }
                        stateDescription = if (autoCaptureEnabled) "已开启" else "已关闭"
                    }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Capture Delay
        SettingsSection(title = stringResource(R.string.setting_capture_delay)) {
            val delayLabel = stringResource(R.string.setting_capture_delay)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_delay_format, captureDelay))
                Slider(
                    value = captureDelay.toFloat(),
                    onValueChange = { viewModel.setCaptureDelay(it.toInt()) },
                    valueRange = 0f..5f,
                    steps = 4,
                    modifier = Modifier
                        .width(200.dp)
                        .semantics {
                            contentDescription = stringResource(
                                R.string.a11y_slider_label,
                                delayLabel,
                                captureDelay.toString()
                            )
                        }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Theme
        SettingsSection(title = stringResource(R.string.setting_theme)) {
            val darkModeLabel = stringResource(R.string.settings_dark_mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(darkModeLabel)
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) },
                    modifier = Modifier.semantics {
                        contentDescription = if (isDarkTheme) {
                            stringResource(R.string.a11y_switch_on, darkModeLabel)
                        } else {
                            stringResource(R.string.a11y_switch_off, darkModeLabel)
                        }
                        stateDescription = if (isDarkTheme) "已开启" else "已关闭"
                    }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Torch
        SettingsSection(title = stringResource(R.string.settings_flash)) {
            val flashLabel = stringResource(R.string.settings_flash_default_on)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(flashLabel)
                Switch(
                    checked = torchEnabled,
                    onCheckedChange = { viewModel.setTorchEnabled(it) },
                    modifier = Modifier.semantics {
                        contentDescription = if (torchEnabled) {
                            stringResource(R.string.a11y_switch_on, flashLabel)
                        } else {
                            stringResource(R.string.a11y_switch_off, flashLabel)
                        }
                        stateDescription = if (torchEnabled) "已开启" else "已关闭"
                    }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Developer Options
        SettingsSection(title = stringResource(R.string.dev_options)) {
            // Performance monitoring toggle
            val monitorLabel = stringResource(R.string.dev_perf_monitor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(monitorLabel)
                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { viewModel.togglePerformanceMonitoring(it) },
                    modifier = Modifier.semantics {
                        contentDescription = if (isMonitoring) {
                            stringResource(R.string.a11y_switch_on, monitorLabel)
                        } else {
                            stringResource(R.string.a11y_switch_off, monitorLabel)
                        }
                        stateDescription = if (isMonitoring) "已开启" else "已关闭"
                    }
                )
            }

            // Performance info (if monitoring is active)
            if (isMonitoring) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    val fpsColor = when {
                        fps >= 55f -> MaterialTheme.colorScheme.primary
                        fps >= 30f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = stringResource(R.string.settings_fps_format, fps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = fpsColor
                    )
                    val memPressureStr = when (memoryPressure) {
                        MemoryPressure.LOW -> stringResource(R.string.memory_pressure_low)
                        MemoryPressure.MEDIUM -> stringResource(R.string.memory_pressure_medium)
                        MemoryPressure.HIGH -> stringResource(R.string.memory_pressure_high)
                    }
                    Text(
                        text = stringResource(R.string.settings_memory_format, memoryUsageMb, memPressureStr),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.settings_jank_frames, jankCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Crash logs clickable item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.loadCrashLogs()
                        showCrashLogDialog = true
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.dev_crash_logs))
                Text(
                    text = if (crashLogs.isNotEmpty()) stringResource(R.string.settings_crash_log_count_format, crashLogs.size) else stringResource(R.string.settings_none),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Feedback
        SettingsSection(title = stringResource(R.string.settings_feedback)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("feedback") }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_submit_feedback))
                Text(
                    text = "→",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Legal Info
        SettingsSection(title = stringResource(R.string.settings_legal_info)) {
            Column {
                LegalInfoItem(
                    title = stringResource(R.string.settings_privacy_policy),
                    onClick = { navController.navigate("privacy_policy") }
                )
                LegalInfoItem(
                    title = stringResource(R.string.settings_user_agreement),
                    onClick = { navController.navigate("user_agreement") }
                )
                LegalInfoItem(
                    title = stringResource(R.string.settings_personal_info_declaration),
                    onClick = { navController.navigate("personal_info_declaration") }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // About
        SettingsSection(title = stringResource(R.string.settings_about)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_about_version))
                Text(
                    stringResource(R.string.settings_about_description),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_about_model),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    stringResource(R.string.settings_about_framework),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }

    // Crash log dialog
    if (showCrashLogDialog) {
        CrashLogDialog(
            crashLogs = crashLogs,
            onDismiss = { showCrashLogDialog = false },
            onClear = {
                viewModel.clearCrashLogs()
                showCrashLogDialog = false
            }
        )
    }
}

@Composable
private fun CrashLogDialog(
    crashLogs: List<CrashLogEntry>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dev_crash_logs)) },
        text = {
            if (crashLogs.isEmpty()) {
                Text(stringResource(R.string.dev_no_crash_logs))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    crashLogs.forEach { log ->
                        val dateStr = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date(log.timestamp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.stackTrace.take(200) + if (log.stackTrace.length > 200) "…" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (crashLogs.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.dev_clear_crash_logs), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dev_close))
            }
        }
    )
}

@Composable
private fun LegalInfoItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
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
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() }
        )
        content()
    }
}
