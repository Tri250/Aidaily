package com.livecompose.livecapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 圆形按钮组件 - 液态玻璃风格
 */
@Composable
fun SecondaryCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 56,
    iconSize: Int = 22
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .liquidGlass(cornerRadius = 0.dp, intensity = 0.15f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DesignSystem.Colors.minimalLabel,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

@Composable
fun TopCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 38,
    iconSize: Int = 18
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .liquidGlass(cornerRadius = 0.dp, intensity = 0.12f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DesignSystem.Colors.minimalLabel,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

private fun Modifier.liquidGlass(cornerRadius: androidx.compose.ui.unit.Dp, intensity: Float): Modifier {
    return this.then(
        com.livecompose.livecapture.ui.design.liquidGlass(cornerRadius, intensity)
    )
}