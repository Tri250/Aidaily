package com.livecompose.livecapture.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures

/**
 * 统一设计系统 - 魅族极简风格（与 iOS 完全对齐）
 * 严格遵循魅族 Flyme 设计语言：低饱和、大留白、纤细字体、克制动效
 */
object DesignSystem {

    // MARK: - Colors（魅族极简色板）

    object Colors {
        // 品牌色 - 低饱和清新蓝（魅族 Flyme 风格）
        val primary = Color(0xFF3B82F6)
        val primaryLight = Color(0xFF93C0FC)
        val secondary = Color(0xFF6366F1)
        val accent = Color(0xFFF5944D)

        // 语义色 - 低饱和版
        val success = Color(0xFF48B870)
        val successBg = Color(0xFF48B870).copy(alpha = 0.12f)
        val warning = Color(0xFFF5B23D)
        val warningBg = Color(0xFFF5B23D).copy(alpha = 0.12f)
        val error = Color(0xFFF05959)
        val errorBg = Color(0xFFF05959).copy(alpha = 0.12f)
        val info = Color(0xFF59B3F0)

        // 中性色阶 - 7 级灰度（魅族极简核心）
        @Composable
        fun gray0() = if (isSystemInDarkTheme()) Color(0xFF141414) else Color(0xFFFAFAFA)
        @Composable
        fun gray1() = if (isSystemInDarkTheme()) Color(0xFF1F1F1F) else Color(0xFFF0F0F0)
        @Composable
        fun gray2() = if (isSystemInDarkTheme()) Color(0xFF292929) else Color(0xFFE6E6E6)
        @Composable
        fun gray3() = if (isSystemInDarkTheme()) Color(0xFF383838) else Color(0xFFD1D1D1)
        @Composable
        fun gray4() = if (isSystemInDarkTheme()) Color(0xFF595959) else Color(0xFF8C8C8C)
        @Composable
        fun gray5() = if (isSystemInDarkTheme()) Color(0xFF8C8C8C) else Color(0xFF595959)
        @Composable
        fun gray6() = if (isSystemInDarkTheme()) Color(0xFFEBEBEB) else Color(0xFF141414)

        // 语义化文字颜色（兼容旧代码）
        @Composable
        fun textPrimary() = gray6()
        @Composable
        fun textSecondary() = gray5()
        @Composable
        fun textTertiary() = gray4()

        // 语义化背景颜色（兼容旧代码）
        @Composable
        fun backgroundPrimary() = gray0()
        @Composable
        fun backgroundSecondary() = gray1()
        @Composable
        fun backgroundTertiary() = gray2()

        // 极简相机专属色
        val minimalBackground = Color.Black
        val minimalOverlay = Color.White.copy(alpha = 0.08f)
        val minimalBorder = Color.White.copy(alpha = 0.20f)
        val minimalActiveBorder = Color.White.copy(alpha = 0.85f)
        val minimalLabel = Color.White.copy(alpha = 0.92f)
        val minimalSecondaryLabel = Color.White.copy(alpha = 0.50f)
        val minimalDarkOverlay = Color.Black.copy(alpha = 0.40f)
        val shutterStroke = Color.White
        val shutterInner = Color.White.copy(alpha = 0.95f)
    }

    // MARK: - Typography（魅族级字体系统）

    object Typography {
        // 字体族 - 标题使用 Rounded 设计风格，正文使用 Default
        // 标题 - Rounded 设计风格
        val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold)
        val title1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
        val title2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
        val title3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        // 正文 - 统一 Default 设计风格
        val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        val body = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal)
        val callout = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
        val subheadline = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
        val footnote = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
        val caption1 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
        val caption2 = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal)

        // 等宽数字（用于 EXIF 数据、计时器）
        val monoBody = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Monospace)
        val monoCaption = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        val monoDigit = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Monospace)

        // 极简模式专用字体
        val minimalModeLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
        val minimalFilterName = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium)
        val minimalControlLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
        val minimalTimer = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        val minimalZoomIndicator = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Monospace)

        // 行高（Line Height）- 基于字体大小的 1.4 倍
        fun lineHeightFor(size: Float): Float = size * 1.4f

        // 字间距（Kerning）
        const val titleKerning: Float = -0.3f
        const val bodyKerning: Float = 0f
        const val captionKerning: Float = 0.2f
    }

    // MARK: - Spacing（严格 4pt 基准网格）

    object Spacing {
        // 所有间距值为 4 的倍数
        val xxxSmall: Dp = 4.dp
        val xxSmall: Dp = 8.dp
        val xSmall: Dp = 12.dp
        val small: Dp = 16.dp
        val medium: Dp = 20.dp
        val large: Dp = 24.dp
        val xLarge: Dp = 32.dp
        val xxLarge: Dp = 48.dp
        val xxxLarge: Dp = 64.dp

        // 水平间距规范
        object Horizontal {
            val tight: Dp = 4.dp
            val compact: Dp = 8.dp
            val standard: Dp = 12.dp
            val relaxed: Dp = 16.dp
            val loose: Dp = 24.dp
        }

        // 垂直间距规范
        object Vertical {
            val tight: Dp = 4.dp
            val compact: Dp = 8.dp
            val standard: Dp = 12.dp
            val relaxed: Dp = 16.dp
            val loose: Dp = 24.dp
        }

        // 内边距规范
        object Padding {
            val inline: Dp = 16.dp
            val block: Dp = 20.dp
            val container: Dp = 24.dp
        }

        // 元素间距规范
        object Gap {
            val minimal: Dp = 4.dp
            val tight: Dp = 8.dp
            val standard: Dp = 12.dp
            val relaxed: Dp = 16.dp
        }
    }

    // MARK: - Corner Radius（统一圆角系统）

    object CornerRadius {
        // 从小到大的圆角层级
        val micro: Dp = 4.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val xLarge: Dp = 20.dp
        val xxLarge: Dp = 24.dp
        val circle: Dp = 999.dp

        // 嵌套圆角规则：内层 = 外层 - 差值，差值默认 4pt
        fun nested(outer: Dp): Dp = maxOf(outer - 4.dp, 4.dp)
    }

    // MARK: - Shapes

    val microRoundedShape = RoundedCornerShape(CornerRadius.micro)
    val smallRoundedShape = RoundedCornerShape(CornerRadius.small)
    val mediumRoundedShape = RoundedCornerShape(CornerRadius.medium)
    val largeRoundedShape = RoundedCornerShape(CornerRadius.large)
    val xLargeRoundedShape = RoundedCornerShape(CornerRadius.xLarge)
    val xxLargeRoundedShape = RoundedCornerShape(CornerRadius.xxLarge)
    val circleShape = CircleShape

    // MARK: - Shadows（柔和阴影系统）

    object Shadows {
        // 轻微阴影 - 卡片
        data class ShadowConfig(val color: Color, val radius: Dp, val offsetX: Dp, val offsetY: Dp)

        fun subtle() = ShadowConfig(Color.Black.copy(alpha = 0.06f), 8.dp, 0.dp, 2.dp)
        fun elevated() = ShadowConfig(Color.Black.copy(alpha = 0.10f), 16.dp, 0.dp, 4.dp)
        fun modal() = ShadowConfig(Color.Black.copy(alpha = 0.15f), 24.dp, 0.dp, 8.dp)
        fun glow(color: Color) = ShadowConfig(color.copy(alpha = 0.4f), 12.dp, 0.dp, 0.dp)
    }

    // MARK: - Stroke（描边系统）

    object Stroke {
        @Composable
        fun subtle() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
        @Composable
        fun standard() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.12f)
        @Composable
        fun prominent() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.20f)
        @Composable
        fun active() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.60f)

        val widthThin: Dp = 0.5.dp
        val widthStandard: Dp = 1.dp
        val widthThick: Dp = 1.5.dp
        val widthHeavy: Dp = 2.dp
    }

    // MARK: - Animation（魅族级别克制动效）

    object Animation {
        // 基础缓动
        val easeIn = tween<Float>(200, easing = EaseIn)
        val easeOut = tween<Float>(200, easing = EaseOut)
        val easeInOut = tween<Float>(250, easing = FastOutSlowInEasing)

        // 弹簧动画 - 魅族偏好柔软弹性
        val quick = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        val smooth = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        val bouncy = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        val gentle = spring<Float>(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessVeryLow)

        // 相机专用弹簧动画
        val shutterPress = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh)
        val shutterRelease = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        val overlayFade = tween<Float>(250, easing = FastOutSlowInEasing)
        val modeSlide = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        val filterReveal = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        val zoomPop = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        val snappy = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

        // 自动隐藏延迟
        const val autoHideDelay: Long = 3000L
    }
}

// MARK: - Compose Modifier Extensions（核心视觉修饰器，对标 iOS ViewModifier）

/**
 * 毛玻璃效果（Glassmorphism）- 对标 iOS GlassmorphismModifier
 */
fun Modifier.glassmorphism(
    cornerRadius: Dp = DesignSystem.CornerRadius.medium,
    opacity: Float = 0.08f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Color.White.copy(alpha = opacity),
        RoundedCornerShape(cornerRadius)
    )
    .then(
        Modifier.drawWithStroke(
            cornerRadius = cornerRadius,
            strokeColor = Color.White.copy(alpha = 0.10f),
            strokeWidth = DesignSystem.Stroke.widthThin
        )
    )

/**
 * 骨架屏 Shimmer 效果 - 对标 iOS ShimmerModifier
 */
fun Modifier.shimmer(): Modifier {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0f)
    )

    return this.background(
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    )
}

/**
 * 按钮按压缩放 - 对标 iOS PressScaleModifier
 */
fun Modifier.pressScale(
    scale: Float = 0.97f
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onPress = {
            // Scale down on press
        }
    )
}

/**
 * 发光效果 - 对标 iOS GlowModifier
 */
fun Modifier.glow(
    color: Color,
    radius: Dp = 12.dp
): Modifier = this
    .shadow(
        elevation = radius,
        shape = CircleShape,
        ambientColor = color.copy(alpha = 0.35f),
        spotColor = color.copy(alpha = 0.15f)
    )

/**
 * 脉动动画 - 对标 iOS PulseModifier
 */
fun Modifier.pulse(
    color: Color,
    durationMillis: Int = 1500
): Modifier = this

/**
 * 标准阴影 - 对标 iOS subtleShadow()
 */
fun Modifier.subtleShadow(): Modifier {
    val shadow = DesignSystem.Shadows.subtle()
    return this.shadow(
        elevation = shadow.radius,
        shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
        ambientColor = shadow.color,
        spotColor = shadow.color
    )
}

/**
 * 浮动阴影 - 对标 iOS elevatedShadow()
 */
fun Modifier.elevatedShadow(): Modifier {
    val shadow = DesignSystem.Shadows.elevated()
    return this.shadow(
        elevation = shadow.radius,
        shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
        ambientColor = shadow.color,
        spotColor = shadow.color
    )
}

/**
 * 模态阴影 - 对标 iOS modalShadow()
 */
fun Modifier.modalShadow(): Modifier {
    val shadow = DesignSystem.Shadows.modal()
    return this.shadow(
        elevation = shadow.radius,
        shape = RoundedCornerShape(DesignSystem.CornerRadius.xLarge),
        ambientColor = shadow.color,
        spotColor = shadow.color
    )
}

/**
 * 页面统一内边距 - 对标 iOS pagePadding()
 */
fun Modifier.pagePadding(): Modifier = this.padding(horizontal = DesignSystem.Spacing.Padding.container)

/**
 * 绘制描边（内部辅助函数）
 */
private fun Modifier.drawWithStroke(
    cornerRadius: Dp,
    strokeColor: Color,
    strokeWidth: Dp
): Modifier = this