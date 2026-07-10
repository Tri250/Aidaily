package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 用户引导视图 - 对标 iOS UserGuidanceView
 *
 * 在顶部中央显示动态的用户操作引导
 * 根据不同的流程阶段提供文字和图标提示
 * 使用不同颜色表示不同状态
 *
 * 状态映射（与 iOS 完全对齐）：
 * - "魔术棒"/"构图流水线" → wand.and.stars → 紫色
 * - "启动" → power → 主色
 * - "保持"/"稳定" → hand.raised.fill → 警告黄
 * - "识别"/"检测" → viewfinder → 信息蓝
 * - "移动"/"对准" → arrows → 主色
 * - "即将"/"拍照" → camera.fill → 成功绿
 * - "保存"/"完成" → checkmark.circle.fill → 成功绿
 * - "错误" → exclamationmark.triangle.fill → 错误红
 * - 默认 → info.circle.fill → 主色
 */
@Composable
fun UserGuidanceView(
    guidanceText: String,
    modifier: Modifier = Modifier
) {
    if (guidanceText.isEmpty()) return

    val statusIcon = statusIconFor(guidanceText)
    val statusColor = statusColorFor(guidanceText)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.5.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.height(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            guidanceText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1
        )
    }
}

private fun statusIconFor(guidance: String): ImageVector {
    return when {
        guidance.contains("魔术棒") || guidance.contains("构图流水线") -> Icons.Default.AutoFixHigh
        guidance.contains("启动") -> Icons.Default.PowerSettingsNew
        guidance.contains("保持") || guidance.contains("稳定") -> Icons.Default.PanTool
        guidance.contains("识别") || guidance.contains("检测") -> Icons.Default.GpsFixed
        guidance.contains("移动") || guidance.contains("对准") -> Icons.Default.OpenInFull
        guidance.contains("即将") || guidance.contains("拍照") -> Icons.Default.CameraAlt
        guidance.contains("保存") || guidance.contains("完成") -> Icons.Default.CheckCircle
        guidance.contains("错误") -> Icons.Default.Warning
        else -> Icons.Default.Info
    }
}

private fun statusColorFor(guidance: String): Color {
    return when {
        guidance.contains("错误") -> DesignSystem.Colors.error
        guidance.contains("构图流水线已开启") -> DesignSystem.Colors.success
        guidance.contains("魔术棒") -> DesignSystem.Colors.secondary
        guidance.contains("保存") || guidance.contains("完成") || guidance.contains("即将") -> DesignSystem.Colors.success
        guidance.contains("保持") || guidance.contains("稳定") -> DesignSystem.Colors.warning
        guidance.contains("识别") || guidance.contains("检测") -> DesignSystem.Colors.info
        else -> DesignSystem.Colors.primary
    }
}
