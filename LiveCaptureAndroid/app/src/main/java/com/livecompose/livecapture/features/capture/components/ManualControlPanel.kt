package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 手动模式控制参数
 */
data class ManualControlParams(
    val iso: Int = 100,
    val isoAuto: Boolean = true,
    val shutterSpeed: Float = 1f / 60f, // 秒
    val shutterSpeedAuto: Boolean = true,
    val whiteBalance: WhiteBalanceMode = WhiteBalanceMode.AUTO,
    val customColorTemp: Int = 5500, // 2000K-8000K
    val focusMode: FocusMode = FocusMode.AF_C,
    val manualFocusDistance: Float = 0.5f, // 0.0 ~ 1.0 (近到远)
    val exposureCompensation: Float = 0f, // -3.0 ~ +3.0, 1/3 步长
)

enum class WhiteBalanceMode(val displayName: String, val colorTemp: Int? = null) {
    AUTO("自动", null),
    DAYLIGHT("日光", 5500),
    CLOUDY("阴天", 6500),
    INCANDESCENT("白炽灯", 3000),
    FLUORESCENT("荧光灯", 4200),
    CUSTOM("自定义", null)
}

enum class FocusMode(val displayName: String) {
    AF_S("AF-S"),
    AF_C("AF-C"),
    MF("MF")
}

/**
 * ISO 预设值
 */
private val isoPresets = intArrayOf(
    50, 64, 80, 100, 125, 160, 200, 250, 320, 400,
    500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200
)

/**
 * 快门速度预设值（秒）
 */
private val shutterSpeedPresets = floatArrayOf(
    1f / 8000f, 1f / 6400f, 1f / 5000f, 1f / 4000f, 1f / 3200f,
    1f / 2500f, 1f / 2000f, 1f / 1600f, 1f / 1250f, 1f / 1000f,
    1f / 800f, 1f / 640f, 1f / 500f, 1f / 400f, 1f / 320f,
    1f / 250f, 1f / 200f, 1f / 160f, 1f / 125f, 1f / 100f,
    1f / 80f, 1f / 60f, 1f / 50f, 1f / 40f, 1f / 30f,
    1f / 25f, 1f / 20f, 1f / 15f, 1f / 13f, 1f / 10f,
    1f / 8f, 1f / 6f, 1f / 5f, 1f / 4f, 1f / 3f,
    1f / 2.5f, 1f / 2f, 1f / 1.6f, 1f / 1.3f, 1f,
    1.3f, 1.6f, 2f, 2.5f, 3.2f, 4f, 5f, 6f, 8f,
    10f, 13f, 15f, 20f, 25f, 30f
)

/**
 * 专业手动模式控制面板
 * 包含 ISO、快门速度、白平衡、对焦模式、曝光补偿
 */
@Composable
fun ManualControlPanel(
    params: ManualControlParams,
    onParamsChanged: (ManualControlParams) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .verticalScroll(scrollState)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 面板标题
        PanelHeader("手动控制")

        // ISO 控制
        IsoControl(
            iso = params.iso,
            isAuto = params.isoAuto,
            onIsoChanged = { iso -> onParamsChanged(params.copy(iso = iso)) },
            onAutoToggle = { onParamsChanged(params.copy(isoAuto = !params.isoAuto)) }
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

        // 快门速度控制
        ShutterSpeedControl(
            shutterSpeed = params.shutterSpeed,
            isAuto = params.shutterSpeedAuto,
            onShutterChanged = { sp -> onParamsChanged(params.copy(shutterSpeed = sp)) },
            onAutoToggle = { onParamsChanged(params.copy(shutterSpeedAuto = !params.shutterSpeedAuto)) }
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

        // 白平衡选择器
        WhiteBalanceControl(
            currentMode = params.whiteBalance,
            customTemp = params.customColorTemp,
            onModeChanged = { mode -> onParamsChanged(params.copy(whiteBalance = mode)) },
            onTempChanged = { temp -> onParamsChanged(params.copy(customColorTemp = temp)) }
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

        // 对焦模式
        FocusModeControl(
            currentMode = params.focusMode,
            focusDistance = params.manualFocusDistance,
            onModeChanged = { mode -> onParamsChanged(params.copy(focusMode = mode)) },
            onDistanceChanged = { dist -> onParamsChanged(params.copy(manualFocusDistance = dist)) }
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

        // 曝光补偿
        ExposureCompensationControl(
            currentEV = params.exposureCompensation,
            onEVChanged = { ev -> onParamsChanged(params.copy(exposureCompensation = ev)) }
        )
    }
}

@Composable
private fun PanelHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun IsoControl(
    iso: Int,
    isAuto: Boolean,
    onIsoChanged: (Int) -> Unit,
    onAutoToggle: () -> Unit
) {
    ControlRow(
        label = "ISO",
        value = if (isAuto) "自动" else iso.toString(),
        isAuto = isAuto,
        onAutoToggle = onAutoToggle,
        icon = Icons.Default.Iso
    ) {
        if (!isAuto) {
            IsoSlider(
                value = iso,
                onValueChange = onIsoChanged
            )
        }
    }
}

@Composable
private fun IsoSlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val index = isoPresets.indexOf(value).coerceIn(0, isoPresets.lastIndex)

    Column {
        Text(
            text = "ISO ${isoPresets[index]}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Slider(
            value = index.toFloat(),
            onValueChange = { idx -> onValueChange(isoPresets[idx.roundToInt().coerceIn(0, isoPresets.lastIndex)]) },
            valueRange = 0f..(isoPresets.lastIndex.toFloat()),
            steps = 0,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF9500),
                activeTrackColor = Color(0xFFFF9500),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("50", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Text("3200", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun ShutterSpeedControl(
    shutterSpeed: Float,
    isAuto: Boolean,
    onShutterChanged: (Float) -> Unit,
    onAutoToggle: () -> Unit
) {
    ControlRow(
        label = "快门",
        value = if (isAuto) "自动" else formatShutterSpeed(shutterSpeed),
        isAuto = isAuto,
        onAutoToggle = onAutoToggle,
        icon = Icons.Default.Speed
    ) {
        if (!isAuto) {
            ShutterSpeedSlider(
                value = shutterSpeed,
                onValueChange = onShutterChanged
            )
        }
    }
}

@Composable
private fun ShutterSpeedSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val closestIndex = shutterSpeedPresets.indexOf(
        shutterSpeedPresets.minByOrNull { kotlin.math.abs(it - value) } ?: 1f / 60f
    ).coerceIn(0, shutterSpeedPresets.lastIndex)

    Column {
        Text(
            text = formatShutterSpeed(shutterSpeedPresets[closestIndex]),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Slider(
            value = closestIndex.toFloat(),
            onValueChange = { idx ->
                onValueChange(shutterSpeedPresets[idx.roundToInt().coerceIn(0, shutterSpeedPresets.lastIndex)])
            },
            valueRange = 0f..(shutterSpeedPresets.lastIndex.toFloat()),
            steps = 0,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF9500),
                activeTrackColor = Color(0xFFFF9500),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1/8000s", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Text("30s", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun WhiteBalanceControl(
    currentMode: WhiteBalanceMode,
    customTemp: Int,
    onModeChanged: (WhiteBalanceMode) -> Unit,
    onTempChanged: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "白平衡",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (currentMode == WhiteBalanceMode.CUSTOM) "${customTemp}K" else currentMode.displayName,
                color = Color(0xFFFF9500),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(WhiteBalanceMode.entries.size) { index ->
                val mode = WhiteBalanceMode.entries[index]
                val isSelected = mode == currentMode
                WbChip(
                    label = mode.displayName,
                    isSelected = isSelected,
                    onClick = { onModeChanged(mode) }
                )
            }
        }

        if (currentMode == WhiteBalanceMode.CUSTOM) {
            Spacer(Modifier.height(8.dp))
            ColorTempSlider(
                value = customTemp,
                onValueChange = onTempChanged
            )
        }
    }
}

@Composable
private fun WbChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFFF9500).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9500)) else null
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFFFF9500) else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ColorTempSlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "${value}K",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(2000, 8000)) },
            valueRange = 2000f..8000f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF9500),
                activeTrackColor = Color(0xFFFF9500),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("2000K", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Text("8000K", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun FocusModeControl(
    currentMode: FocusMode,
    focusDistance: Float,
    onModeChanged: (FocusMode) -> Unit,
    onDistanceChanged: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "对焦模式",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = currentMode.displayName,
                color = Color(0xFFFF9500),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocusMode.entries.forEach { mode ->
                val isSelected = mode == currentMode
                FocusChip(
                    label = mode.displayName,
                    isSelected = isSelected,
                    onClick = { onModeChanged(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (currentMode == FocusMode.MF) {
            Spacer(Modifier.height(8.dp))
            ManualFocusSlider(
                distance = focusDistance,
                onDistanceChanged = onDistanceChanged
            )
        }
    }
}

@Composable
private fun FocusChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFFFF9500) else Color.White.copy(alpha = 0.1f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9500)) else null
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
        )
    }
}

@Composable
private fun ManualFocusSlider(
    distance: Float,
    onDistanceChanged: (Float) -> Unit
) {
    Column {
        val displayText = when {
            distance < 0.05f -> "微距"
            distance < 0.3f -> "${(distance * 100).roundToInt()}cm"
            distance < 0.9f -> "${String.format("%.1f", distance * 10)}m"
            else -> "∞"
        }

        Text(
            text = displayText,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Slider(
            value = distance,
            onValueChange = onDistanceChanged,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF9500),
                activeTrackColor = Color(0xFFFF9500),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("近", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Text("∞", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun ExposureCompensationControl(
    currentEV: Float,
    onEVChanged: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Exposure,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "曝光补偿",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatEV(currentEV),
                color = Color(0xFFFF9500),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(4.dp))

        // EV 步进按钮 + 滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 减按钮
            EVStepButton(
                icon = Icons.Default.Remove,
                onClick = { onEVChanged((currentEV - 1f / 3f).coerceIn(-3f, 3f)) }
            )

            Slider(
                value = currentEV,
                onValueChange = { ev ->
                    // 对齐到 1/3 步长
                    val stepped = (ev * 3f).roundToInt() / 3f
                    onEVChanged(stepped.coerceIn(-3f, 3f))
                },
                valueRange = -3f..3f,
                steps = 17, // 6 / (1/3) - 1 = 17
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF9500),
                    activeTrackColor = Color(0xFFFF9500),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            // 加按钮
            EVStepButton(
                icon = Icons.Default.Add,
                onClick = { onEVChanged((currentEV + 1f / 3f).coerceIn(-3f, 3f)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-3", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Text("0", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Text("+3", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun EVStepButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ControlRow(
    label: String,
    value: String,
    isAuto: Boolean,
    onAutoToggle: () -> Unit,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                color = Color(0xFFFF9500),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            // Auto 切换按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isAuto) Color(0xFFFF9500).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
                    )
                    .clickable(onClick = onAutoToggle)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AUTO",
                    color = if (isAuto) Color(0xFFFF9500) else Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        content()
    }
}

/**
 * 格式化快门速度显示
 */
private fun formatShutterSpeed(speed: Float): String {
    return if (speed >= 1f) {
        if (speed == speed.toInt().toFloat()) "${speed.toInt()}s" else String.format("%.1fs", speed)
    } else {
        val denominator = (1f / speed).roundToInt()
        "1/${denominator}s"
    }
}

/**
 * 格式化 EV 显示
 */
private fun formatEV(ev: Float): String {
    val rounded = (ev * 3f).roundToInt() / 3f
    return if (rounded >= 0) "+%.1f".format(rounded) else "%.1f".format(rounded)
}