package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 调试信息面板 - 对标 iOS DebugPanel
 *
 * 在拍摄界面顶部显示详细的调试信息
 * 方便开发和测试时查看系统状态
 * 提供分组化的信息展示
 *
 * UI 结构（与 iOS 完全对齐）：
 * 1. 标题栏：图标徽章 + "调试信息" + 关闭按钮
 * 2. 状态信息：运动稳定性 / 检测就绪 / 对齐状态
 * 3. 追踪信息：追踪点位置 / 到中心距离
 * 4. 相机信息：变焦倍率 / 对准状态
 * 5. 调试消息：系统调试文本
 */
@Composable
fun DebugPanel(
    debugMessage: String,
    motionIsStable: Boolean,
    boxCenterInView: android.graphics.PointF?,
    distanceToCenter: Float?,
    detectionReady: Boolean,
    zoomDisplayText: String,
    focalLengthText: String,
    isAligned: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.75f),
                        Color.Black.copy(alpha = 0.65f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DesignSystem.Colors.accent.copy(alpha = 0.5f),
                        DesignSystem.Colors.accent.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        // 标题栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 图标徽章
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DesignSystem.Colors.accent, DesignSystem.Colors.accent.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "调试信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            // 关闭按钮
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null,
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        GradientDivider()

        // 状态信息
        Spacer(Modifier.height(12.dp))
        DebugInfoRow(
            icon = Icons.Default.Settings,
            title = "状态",
            value = debugMessage,
            iconColor = DesignSystem.Colors.info
        )
        DebugInfoRow(
            icon = if (motionIsStable) Icons.Default.CheckCircle else Icons.Default.Warning,
            title = "稳定性",
            value = if (motionIsStable) "稳定" else "不稳定",
            valueColor = if (motionIsStable) DesignSystem.Colors.success else DesignSystem.Colors.warning,
            iconColor = if (motionIsStable) DesignSystem.Colors.success else DesignSystem.Colors.warning
        )

        Spacer(Modifier.height(8.dp))
        GradientDivider(alpha = 0.1f)

        // 追踪和检测信息
        Spacer(Modifier.height(8.dp))
        if (boxCenterInView != null) {
            DebugInfoRow(
                icon = Icons.Default.GpsFixed,
                title = "跟踪位置",
                value = "(${boxCenterInView.x.toInt()}, ${boxCenterInView.y.toInt()})",
                iconColor = DesignSystem.Colors.primary
            )
        } else {
            DebugInfoRow(
                icon = Icons.Default.GpsFixed,
                title = "跟踪位置",
                value = "无",
                valueColor = Color.Gray,
                iconColor = Color.Gray
            )
        }

        if (distanceToCenter != null) {
            DebugInfoRow(
                icon = Icons.Default.Info,
                title = "距离中心",
                value = "${"%.1f".format(distanceToCenter)} pts",
                valueColor = if (distanceToCenter < 15f) DesignSystem.Colors.success else Color.White,
                iconColor = if (distanceToCenter < 15f) DesignSystem.Colors.success else DesignSystem.Colors.primary
            )
        } else {
            DebugInfoRow(
                icon = Icons.Default.Info,
                title = "距离中心",
                value = "--",
                valueColor = Color.Gray,
                iconColor = Color.Gray
            )
        }

        DebugInfoRow(
            icon = if (detectionReady) Icons.Default.CheckCircle else Icons.Default.Info,
            title = "检测状态",
            value = if (detectionReady) "已就绪" else "未就绪",
            valueColor = if (detectionReady) DesignSystem.Colors.success else Color.Gray,
            iconColor = if (detectionReady) DesignSystem.Colors.success else Color.Gray
        )

        Spacer(Modifier.height(8.dp))
        GradientDivider(alpha = 0.1f)

        // 相机参数
        Spacer(Modifier.height(8.dp))
        DebugInfoRow(
            icon = Icons.Default.CameraAlt,
            title = "变焦",
            value = "$zoomDisplayText / $focalLengthText",
            iconColor = DesignSystem.Colors.secondary
        )
        DebugInfoRow(
            icon = if (isAligned) Icons.Default.GpsFixed else Icons.Default.Info,
            title = "对准状态",
            value = if (isAligned) "已对准" else "未对准",
            valueColor = if (isAligned) DesignSystem.Colors.success else Color.White,
            iconColor = if (isAligned) DesignSystem.Colors.success else Color.Gray
        )
    }
}

@Composable
private fun DebugInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: Color = Color.White,
    iconColor: Color = DesignSystem.Colors.accent
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.weight(1f))
        // 值标签（胶囊）
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(valueColor.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GradientDivider(alpha: Float = 0.3f) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        Color.White.copy(alpha = alpha * 0.3f)
                    )
                )
            )
    )
}
