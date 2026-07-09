package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 拍摄模式定义
 */
enum class CaptureMode(
    val label: String,
    val subtitle: String,
    val icon: ImageVector
) {
    PHOTO("拍照", "单张", Icons.Default.CameraAlt),
    VIDEO("视频", "15s", Icons.Default.Videocam),
    PORTRAIT("人像", "美颜", Icons.Default.Person),
    PRO("专业", "手动", Icons.Default.Tune),
    NIGHT("夜景", "暗光", Icons.Default.Nightlight),
    MORE("更多", "全部", Icons.Default.MoreHoriz)
}

/**
 * 模式对应的强调色
 */
private val CaptureMode.accentColor: Color
    get() = when (this) {
        CaptureMode.PHOTO -> DesignSystem.Colors.primary
        CaptureMode.VIDEO -> DesignSystem.Colors.error
        CaptureMode.PORTRAIT -> DesignSystem.Colors.accent
        CaptureMode.PRO -> DesignSystem.Colors.info
        CaptureMode.NIGHT -> Color(0xFF6B5BA5)
        CaptureMode.MORE -> DesignSystem.Colors.secondary
    }

/**
 * 重设计模式选择器 - 国潮质感卡片式
 *
 * 特性：
 * - 水平滑动卡片选择（非简单文字切换）
 * - 选中卡片放大 1.1x + 发光边框
 * - 底部滑动指示器
 * - 切换时取景框缩放呼吸过渡
 */
@Composable
fun ModeSelector(
    modes: List<CaptureMode> = listOf(
        CaptureMode.PHOTO,
        CaptureMode.VIDEO,
        CaptureMode.PORTRAIT,
        CaptureMode.PRO
    ),
    selectedMode: CaptureMode = CaptureMode.PHOTO,
    onModeSelected: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)
    val cardWidth = 72.dp
    val cardSpacing = 8.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEachIndexed { index, mode ->
                val isSelected = mode == selectedMode

                ModeCard(
                    mode = mode,
                    isSelected = isSelected,
                    onClick = { onModeSelected(mode) }
                )
            }
        }

        // Bottom sliding indicator
        val indicatorOffset by animateDpAsState(
            targetValue = selectedIndex * (cardWidth + cardSpacing),
            animationSpec = DesignSystem.Animation.modeSlide,
            label = "modeIndicatorOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(16.dp)
                .height(2.dp)
                .background(
                    color = selectedMode.accentColor,
                    shape = RoundedCornerShape(1.dp)
                )
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun ModeCard(
    mode: CaptureMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scaleValue by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = DesignSystem.Animation.smooth,
        label = "modeCardScale"
    )

    val cardShape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(56.dp)
            .scale(scaleValue)
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = cardShape,
                        ambientColor = mode.accentColor.copy(alpha = 0.3f),
                        spotColor = mode.accentColor.copy(alpha = 0.3f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(cardShape)
            .background(DesignSystem.Colors.minimalDarkOverlay)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = mode.accentColor,
                        shape = cardShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = mode.icon,
                contentDescription = mode.label,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) mode.accentColor else DesignSystem.Colors.minimalLabel
            )

            Text(
                text = mode.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) mode.accentColor else DesignSystem.Colors.minimalLabel
            )

            Text(
                text = mode.subtitle,
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = DesignSystem.Colors.minimalSecondaryLabel
            )
        }
    }
}
