package com.livecompose.livecapture.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设计系统
 */
object DesignSystem {

    // Colors
    object Colors {
        val primary = Color(0xFF007AFF)
        val secondary = Color(0xFF5956D6)
        val accent = Color(0xFFFF9500)
        val success = Color(0xFF34C759)
        val warning = Color(0xFFFFCC00)
        val error = Color(0xFFFF3B30)
        val info = Color(0xFF5AC8FA)

        @Composable
        fun textPrimary() = if (isSystemInDarkTheme()) Color.White else Color(0xFF1C1C1E)
        @Composable
        fun textSecondary() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color(0xFF3C3C43).copy(alpha = 0.65f)
        @Composable
        fun textTertiary() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.45f)
        @Composable
        fun backgroundPrimary() = if (isSystemInDarkTheme()) Color(0xFF000000) else Color(0xFFF2F2F7)
        @Composable
        fun backgroundSecondary() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
        @Composable
        fun backgroundTertiary() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
    }

    // Typography
    object Typography {
        val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold)
        val title1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
        val title2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
        val title3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        val body = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal)
        val callout = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
        val subheadline = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
        val footnote = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
        val caption1 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
        val caption2 = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal)
        val monoBody = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Monospace)
        val monoCaption = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }

    // Spacing
    object Spacing {
        val xxxSmall = 2.dp
        val xxSmall = 4.dp
        val xSmall = 8.dp
        val small = 12.dp
        val medium = 16.dp
        val large = 24.dp
        val xLarge = 32.dp
        val xxLarge = 48.dp
        val xxxLarge = 64.dp
    }

    // Corner Radius
    object CornerRadius {
        val small = 8.dp
        val medium = 12.dp
        val large = 16.dp
        val xLarge = 24.dp
        val xxLarge = 32.dp
    }

    // Shapes
    val smallRoundedShape = RoundedCornerShape(CornerRadius.small)
    val mediumRoundedShape = RoundedCornerShape(CornerRadius.medium)
    val largeRoundedShape = RoundedCornerShape(CornerRadius.large)

    // Animation
    object Animation {
        val quick = tween<Float>(250, easing = FastOutSlowInEasing)
        val smooth = tween<Float>(350, easing = FastOutSlowInEasing)
        val bouncy = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        val gentle = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)
        val easeIn = tween<Float>(200, easing = EaseIn)
        val easeOut = tween<Float>(200, easing = EaseOut)
        val easeInOut = tween<Float>(300, easing = FastOutSlowInEasing)
    }
}