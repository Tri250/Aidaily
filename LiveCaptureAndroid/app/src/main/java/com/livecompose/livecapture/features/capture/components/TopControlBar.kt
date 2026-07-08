package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.components.TopCircleButton
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 顶部控制栏
 */
@Composable
fun TopControlBar(
    userGuidanceText: String,
    showDebugInfo: Boolean,
    isAutoCaptureEnabled: Boolean,
    captureDelay: Double,
    onBack: () -> Unit,
    onToggleDebug: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    onSetCaptureDelay: (Double) -> Unit
) {
    var showCaptureMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧按钮组
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopCircleButton(icon = Icons.Default.Close, onClick = onBack)
            TopCircleButton(icon = Icons.Default.BugReport, onClick = onToggleDebug)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 中间引导文字
        if (userGuidanceText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    userGuidanceText,
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 右侧按钮组
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopCircleButton(icon = Icons.Default.FlipCameraAndroid, onClick = onToggleCamera)
            TopCircleButton(icon = Icons.Default.Timer, onClick = { showCaptureMenu = true })
        }
    }

    // 自动拍照菜单
    DropdownMenu(expanded = showCaptureMenu, onDismissRequest = { showCaptureMenu = false }) {
        DropdownMenuItem(
            text = { Text("自动拍照: ${if (isAutoCaptureEnabled) "开启" else "关闭"}") },
            onClick = { onToggleAutoCapture(); showCaptureMenu = false },
            leadingIcon = { Icon(if (isAutoCaptureEnabled) Icons.Default.CheckCircle else Icons.Default.Circle, null) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("延迟: ${"%.1f".format(captureDelay)}秒") },
            onClick = { },
            leadingIcon = { Icon(Icons.Default.Timer, null) },
            enabled = false
        )
        listOf(0.25, 0.5, 1.0, 1.5, 2.0).forEach { delay ->
            DropdownMenuItem(
                text = {
                    Text("${"%.1f".format(delay)}秒", modifier = Modifier.fillMaxWidth())
                },
                onClick = { onSetCaptureDelay(delay); showCaptureMenu = false },
                leadingIcon = {
                    if (delay == captureDelay) Icon(Icons.Default.Check, null, tint = DesignSystem.Colors.primary)
                    else Spacer(modifier = Modifier.size(24.dp))
                }
            )
        }
    }
}