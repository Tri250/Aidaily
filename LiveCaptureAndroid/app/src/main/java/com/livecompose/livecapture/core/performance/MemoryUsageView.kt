package com.livecompose.livecapture.core.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 内存使用监控视图
 *
 * 对应 iOS 端 MemoryUsageView，显示当前内存使用量、告警等级和峰值。
 *
 * ## 显示内容
 * - 顶部行：标题"内存" + 告警等级标签（颜色随等级变化）
 * - 中间进度条：当前内存占 criticalThreshold 的比例，颜色随等级变化
 * - 底部行：当前内存（等宽字体） + 峰值内存
 *
 * @param monitor 内存监控器
 * @param modifier 修饰符
 */
@Composable
fun MemoryUsageView(
    monitor: MemoryMonitor,
    modifier: Modifier = Modifier
) {
    val currentMemory by monitor.currentMemoryMB.collectAsState()
    val peakMemory by monitor.peakMemoryMB.collectAsState()
    val warningLevel by monitor.warningLevel.collectAsState()

    val warningColor = warningLevelColor(warningLevel)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .background(DesignSystem.Colors.backgroundSecondary())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 顶部行：标题 + 告警等级
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "内存",
                style = DesignSystem.Typography.caption1,
                color = DesignSystem.Colors.textSecondary()
            )
            Text(
                text = warningLevel.displayName,
                style = DesignSystem.Typography.caption1,
                color = warningColor
            )
        }

        // 进度条
        ProgressBar(
            progress = (currentMemory / monitor.criticalThresholdMB).coerceIn(0.0, 1.0).toFloat(),
            color = warningColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )

        // 底部行：当前内存 + 峰值
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%.0f MB", currentMemory),
                style = DesignSystem.Typography.monoCaption,
                color = DesignSystem.Colors.textPrimary()
            )
            Text(
                text = "峰值: ${String.format("%.0f", peakMemory)} MB",
                style = DesignSystem.Typography.caption2,
                color = DesignSystem.Colors.textTertiary()
            )
        }
    }
}

/**
 * 简化进度条
 *
 * @param progress 进度（0-1）
 * @param color 进度条颜色
 * @param modifier 修饰符
 */
@Composable
private fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(DesignSystem.Colors.gray3())
    ) {
        Spacer(
            modifier = Modifier
                .weight(progress.coerceAtLeast(0.001f))
                .background(color)
        )
        Spacer(
            modifier = Modifier
                .weight((1f - progress).coerceAtLeast(0.001f))
        )
    }
}

/**
 * 根据告警等级返回对应颜色
 */
private fun warningLevelColor(level: MemoryWarningLevel): Color = when (level) {
    MemoryWarningLevel.NORMAL -> DesignSystem.Colors.success
    MemoryWarningLevel.WARNING -> DesignSystem.Colors.warning
    MemoryWarningLevel.CRITICAL -> DesignSystem.Colors.error
}
