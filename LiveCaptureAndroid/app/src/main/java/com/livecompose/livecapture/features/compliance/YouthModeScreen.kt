package com.livecompose.livecapture.features.compliance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.compliance.UsageRecord
import com.livecompose.livecapture.core.compliance.YouthModeManager
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.launch

/**
 * 密码弹窗状态
 */
private sealed class PasswordDialogState {
    object None : PasswordDialogState()
    data class Verify(val purpose: String, val onSuccess: (String) -> Unit) : PasswordDialogState()
    data class Setup(val onSuccess: (String) -> Unit) : PasswordDialogState()
}

/**
 * 青少年模式设置界面
 *
 * 对应 iOS 端青少年模式设置入口，包含模式开关、使用统计、时长限制、
 * 夜间禁用、内容过滤、密码保护、使用历史等模块。
 *
 * @param manager 青少年模式管理器
 * @param onBack 返回回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouthModeScreen(manager: YouthModeManager, onBack: () -> Unit) {
    val state by manager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var passwordDialog by remember { mutableStateOf<PasswordDialogState>(PasswordDialogState.None) }

    // 轻提示
    fun showToast(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // 切换青少年模式的统一入口：根据是否已设置密码选择验证或设置流程
    fun requestToggle() {
        if (state.isYouthModeEnabled) {
            // 关闭：需要验证密码
            passwordDialog = PasswordDialogState.Verify(
                purpose = "关闭青少年模式",
                onSuccess = { pwd ->
                    manager.disableWithPassword(pwd)
                    showToast("青少年模式已关闭")
                }
            )
        } else if (manager.hasSetPassword) {
            // 已设置过密码：验证后开启
            passwordDialog = PasswordDialogState.Verify(
                purpose = "开启青少年模式",
                onSuccess = { manager.setYouthModeEnabled(true) }
            )
        } else {
            // 首次开启：设置密码
            passwordDialog = PasswordDialogState.Setup(
                onSuccess = {
                    manager.setPassword(it)
                    manager.setYouthModeEnabled(true)
                    showToast("青少年模式已开启")
                }
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = DesignSystem.Colors.gray2(),
                    contentColor = DesignSystem.Colors.minimalLabel
                ) {
                    Text(data.visuals.message)
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("青少年模式", color = DesignSystem.Colors.minimalLabel, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = DesignSystem.Colors.minimalLabel)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DesignSystem.Colors.gray1())
            )
        },
        containerColor = DesignSystem.Colors.gray1()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(isEnabled = state.isYouthModeEnabled)
            ToggleCard(isEnabled = state.isYouthModeEnabled, onToggle = { requestToggle() })

            if (state.isYouthModeEnabled) {
                UsageStatsCard(state = state)
                DailyLimitCard(
                    currentMinutes = state.dailyTimeLimitMinutes,
                    onSelect = { manager.setDailyTimeLimit(it) }
                )
                NightBanCard(
                    startHour = state.nightBanStartHour,
                    endHour = state.nightBanEndHour,
                    inBan = state.isInNightBanPeriod,
                    onSet = { s, e -> manager.setNightBanHours(s, e) }
                )
                ContentFilterCard(
                    communityDisabled = state.isCommunityDisabled,
                    sharingDisabled = state.isSharingDisabled,
                    onCommunityChange = { manager.setCommunityDisabled(it) },
                    onSharingChange = { manager.setSharingDisabled(it) }
                )
                PasswordCard(hasPassword = state.hasSetPassword, onReset = {
                    passwordDialog = PasswordDialogState.Setup(
                        onSuccess = {
                            manager.setPassword(it)
                            showToast("密码已更新")
                        }
                    )
                })
                HistoryCard(
                    manager = manager,
                    onClear = { showToast("使用记录已清除") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // 密码弹窗
    when (val dialog = passwordDialog) {
        is PasswordDialogState.None -> Unit
        is PasswordDialogState.Verify -> PasswordVerifyDialog(
            purpose = dialog.purpose,
            manager = manager,
            onVerify = { pwd ->
                val success = manager.verifyPassword(pwd)
                if (success) {
                    dialog.onSuccess(pwd)
                    passwordDialog = PasswordDialogState.None
                } else {
                    showToast("密码错误")
                }
            },
            onDismiss = { passwordDialog = PasswordDialogState.None }
        )
        is PasswordDialogState.Setup -> PasswordSetupDialog(
            manager = manager,
            onConfirm = { pwd ->
                if (manager.isValidPasswordFormat(pwd)) {
                    dialog.onSuccess(pwd)
                    passwordDialog = PasswordDialogState.None
                } else {
                    showToast("密码需为 4 位数字")
                }
            },
            onDismiss = { passwordDialog = PasswordDialogState.None }
        )
    }
}

// MARK: - 头部卡片

@Composable
private fun HeaderCard(isEnabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystem.Colors.gray2())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                contentDescription = null,
                tint = if (isEnabled) DesignSystem.Colors.primary else DesignSystem.Colors.minimalSecondaryLabel,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "青少年模式",
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    if (isEnabled) "已开启，正在保护使用时长" else "未开启",
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "开启后将限制每日使用时长、夜间禁用，并过滤社区与分享等不适合青少年的内容。",
            color = DesignSystem.Colors.minimalTertiaryLabel,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

// MARK: - 开关卡片

@Composable
private fun ToggleCard(isEnabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystem.Colors.gray2())
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "青少年模式",
            color = DesignSystem.Colors.minimalLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DesignSystem.Colors.primary
            )
        )
    }
}

// MARK: - 使用统计卡片

@Composable
private fun UsageStatsCard(state: com.livecompose.livecapture.core.compliance.YouthModeState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystem.Colors.gray2())
            .padding(20.dp)
    ) {
        Text("今日使用情况", color = DesignSystem.Colors.minimalLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text("已使用", color = DesignSystem.Colors.minimalTertiaryLabel, fontSize = 12.sp)
                Text(
                    state.todayUsageFormatted,
                    color = DesignSystem.Colors.minimalLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("剩余可用", color = DesignSystem.Colors.minimalTertiaryLabel, fontSize = 12.sp)
                Text(
                    state.remainingTimeFormatted,
                    color = if (state.isDailyLimitExceeded) DesignSystem.Colors.error else DesignSystem.Colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when {
            state.isLockedByTimeLimit -> StatusPill(text = "已达时长上限", color = DesignSystem.Colors.error)
            state.isLockedByNightBan -> StatusPill(text = "夜间禁用时段", color = DesignSystem.Colors.warning)
            else -> StatusPill(text = "可正常使用", color = DesignSystem.Colors.primary)
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

// MARK: - 每日时长限制卡片

@Composable
private fun DailyLimitCard(currentMinutes: Int, onSelect: (Int) -> Unit) {
    val options = listOf(40, 60, 90, 120)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystem.Colors.gray2())
            .padding(20.dp)
    ) {
        Text("每日使用时长限制", color = DesignSystem.Colors.minimalLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("达到上限后将锁定应用", color = DesignSystem.Colors.minimalTertiaryLabel, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { minutes ->
                val selected = currentMinutes == minutes
                Text(
                    text = "$minutes 分钟",
                    color = if (selected) Color.White else DesignSystem.Colors.minimalSecondaryLabel,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) DesignSystem.Colors.primary.copy(alpha = 0.25f) else DesignSystem.Colors.minimalOverlay)
                        .border(
                            width = 1.dp,
                            color = if (selected) DesignSystem.Colors.primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(minutes) }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

// MARK: - 夜间禁用卡片

@Composable
private fun NightBanCard(
    startHour: Int,
    endHour: Int,
    inBan: Boolean,
    onSet: (Int, Int) -> Unit
) {
    val startOptions = listOf(20, 21, 22, 23)
    val endOptions = listOf(5, 6, 7, 8)
    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystem.Colors.gray2())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("夜间禁用时段", color = DesignSystem.Colors.minimalLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (inBan) "当前处于禁用时段" else "${formatHour(startHour)} - ${formatHour(endHour)} 禁用",
                    color = if (inBan) DesignSystem.Colors.warning else DesignSystem.Colors.minimalTertiaryLabel,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 起始时间
            Text(
                text = "开始 ${formatHour(startHour)}",
                color = DesignSystem.Colors.minimalLabel,
                fontSize = 13.sp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DesignSystem.Colors.minimalOverlay)
                    .clickable { editingStart = true }
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            )
            // 结束时间
            Text(
                text = "结束 ${formatHour(endHour)}",
                color = DesignSystem.Colors.minimalLabel,
                fontSize = 13.sp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DesignSystem.Colors.minimalOverlay)
                    .clickable { editingEnd = true }
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            )
        }
    }

    if (editingStart) {
        HourPickerDialog(
            title = "选择禁用开始时间",
            options = startOptions,
            current = startHour,
            onPick = {
                onSet(it, endHour)
                editingStart = false
            },
            onDismiss = { editingStart = false }
        )
    }
    if (editingEnd) {
        HourPickerDialog(
            title = "选择禁用结束时间",
            options = endOptions,
            current = endHour,
            onPick = {
                onSet(startHour, it)
                editingEnd = false
            },
            onDismiss = { editingEnd = false }
        )
    }
}

private fun formatHour(hour: Int): String = "${hour.toString().padStart(2, '0')}:00"

@Composable
private fun HourPickerDialog(
    title: String,
    options: List<Int>,
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = DesignSystem.Colors.minimalLabel) },
        text = {
            Column {
                options.forEach { h ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(h) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatHour(h),
                            color = if (h == current) DesignSystem.Colors.primary else DesignSystem.Colors.minimalLabel,
                            fontWeight = if (h == current) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (h == current) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = DesignSystem.Colors.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = DesignSystem.Colors.minimalSecondaryLabel) } },
        containerColor = DesignSystem.Colors.gray2(),
        titleContentColor = DesignSystem.Colors.minimalLabel
    )
}

// MARK: - 内容过滤卡片

@Composable
private fun ContentFilterCard(
    communityDisabled: Boolean,
    sharingDisabled: Boolean,
    onCommunityChange: (Boolean) -> Unit,
    onSharingChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystem.Colors.gray2())
            .padding(20.dp)
    ) {
        Text("内容过滤", color = DesignSystem.Colors.minimalLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("限制不适合青少年的功能", color = DesignSystem.Colors.minimalTertiaryLabel, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        FilterRow(
            icon = Icons.Default.Group,
            title = "禁用社区功能",
            subtitle = "隐藏社区动态与评论",
            checked = communityDisabled,
            onChange = onCommunityChange
        )
        HorizontalDivider(color = DesignSystem.Colors.minimalOverlay, modifier = Modifier.padding(vertical = 4.dp))
        FilterRow(
            icon = Icons.Default.Share,
            title = "禁用分享功能",
            subtitle = "禁止将内容分享至外部",
            checked = sharingDisabled,
            onChange = onSharingChange
        )
    }
}

@Composable
private fun FilterRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = DesignSystem.Colors.minimalSecondaryLabel, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DesignSystem.Colors.minimalLabel, fontSize = 15.sp)
            Text(subtitle, color = DesignSystem.Colors.minimalTertiaryLabel, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DesignSystem.Colors.primary
            )
        )
    }
}

// MARK: - 密码卡片

@Composable
private fun PasswordCard(hasPassword: Boolean, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystem.Colors.gray2())
            .clickable { onReset() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("密码保护", color = DesignSystem.Colors.minimalLabel, fontSize = 15.sp)
            Text(
                if (hasPassword) "已设置 4 位数字密码" else "未设置密码",
                color = DesignSystem.Colors.minimalTertiaryLabel,
                fontSize = 12.sp
            )
        }
        Text(
            if (hasPassword) "修改" else "设置",
            color = DesignSystem.Colors.primary,
            fontSize = 14.sp
        )
    }
}

// MARK: - 使用历史卡片

@Composable
private fun HistoryCard(manager: YouthModeManager, onClear: () -> Unit) {
    var history by remember { mutableStateOf<List<UsageRecord>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) {
        history = manager.recentUsageHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystem.Colors.gray2())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "最近 7 天使用记录",
                color = DesignSystem.Colors.minimalLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "清除",
                color = DesignSystem.Colors.error,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    manager.clearUsageHistory()
                    refreshKey++
                    onClear()
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (history.isEmpty()) {
            Text("暂无记录", color = DesignSystem.Colors.minimalTertiaryLabel, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            history.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(record.date, color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        formatUsage(record.seconds),
                        color = DesignSystem.Colors.minimalLabel,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(color = DesignSystem.Colors.minimalOverlay)
            }
        }
    }
}

private fun formatUsage(seconds: Long): String {
    val minutes = seconds / 60L
    return if (minutes < 60) "${minutes} 分钟" else "${minutes / 60} 小时 ${minutes % 60} 分钟"
}

// MARK: - 密码弹窗

@Composable
private fun PasswordVerifyDialog(
    purpose: String,
    manager: YouthModeManager,
    onVerify: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(purpose, color = DesignSystem.Colors.minimalLabel) },
        text = {
            Column {
                Text("请输入 4 位数字密码", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) input = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DesignSystem.Colors.minimalLabel,
                        unfocusedTextColor = DesignSystem.Colors.minimalLabel,
                        cursorColor = DesignSystem.Colors.primary,
                        focusedBorderColor = DesignSystem.Colors.primary,
                        unfocusedBorderColor = DesignSystem.Colors.minimalOverlay
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (manager.isValidPasswordFormat(input)) onVerify(input) else onVerify("")
            }) { Text("确定", color = DesignSystem.Colors.primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = DesignSystem.Colors.minimalSecondaryLabel) } },
        containerColor = DesignSystem.Colors.gray2(),
        titleContentColor = DesignSystem.Colors.minimalLabel
    )
}

@Composable
private fun PasswordSetupDialog(
    manager: YouthModeManager,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置青少年模式密码", color = DesignSystem.Colors.minimalLabel) },
        text = {
            Column {
                Text("请设置 4 位数字密码，用于关闭模式和修改设置", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) input = it },
                    label = { Text("密码", color = DesignSystem.Colors.minimalSecondaryLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirm = it },
                    label = { Text("确认密码", color = DesignSystem.Colors.minimalSecondaryLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = DesignSystem.Colors.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    !manager.isValidPasswordFormat(input) -> error = "密码需为 4 位数字"
                    input != confirm -> error = "两次输入不一致"
                    else -> {
                        error = null
                        onConfirm(input)
                    }
                }
            }) { Text("确定", color = DesignSystem.Colors.primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = DesignSystem.Colors.minimalSecondaryLabel) } },
        containerColor = DesignSystem.Colors.gray2(),
        titleContentColor = DesignSystem.Colors.minimalLabel
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DesignSystem.Colors.minimalLabel,
    unfocusedTextColor = DesignSystem.Colors.minimalLabel,
    cursorColor = DesignSystem.Colors.primary,
    focusedBorderColor = DesignSystem.Colors.primary,
    unfocusedBorderColor = DesignSystem.Colors.minimalOverlay,
    focusedLabelColor = DesignSystem.Colors.primary,
    unfocusedLabelColor = DesignSystem.Colors.minimalSecondaryLabel
)
