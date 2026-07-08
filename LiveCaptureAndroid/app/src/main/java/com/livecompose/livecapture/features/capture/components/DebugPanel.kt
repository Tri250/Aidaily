package com.livecompose.livecapture.features.capture.components

import android.graphics.PointF
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 调试面板
 */
@Composable
fun DebugPanel(
    debugMessage: String,
    motionIsStable: Boolean,
    boxCenterInView: PointF?,
    distanceToCenter: Float?,
    detectionReady: Boolean,
    zoomDisplayText: String,
    focalLengthText: String,
    isAligned: Boolean,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("调试面板", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            DebugRow("状态", debugMessage)
            DebugRow("稳定性", if (motionIsStable) "稳定" else "移动中")
            DebugRow("检测就绪", detectionReady.toString())
            DebugRow("对齐状态", if (isAligned) "已对齐" else "未对齐")
            boxCenterInView?.let {
                DebugRow("坐标", "(${"%.2f".format(it.x)}, ${"%.2f".format(it.y)})")
            }
            distanceToCenter?.let {
                DebugRow("距离中心", "${"%.2f".format(it)} px")
            }
            DebugRow("变焦", "$zoomDisplayText ($focalLengthText)")
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row {
        Text("$label: ", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}