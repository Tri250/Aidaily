package com.livecompose.livecapture.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI

/**
 * 统一设计系统 - 魅族极简风格（与 iOS DesignSystem.swift 完全对齐）
 * 严格遵循魅族 Flyme 设计语言：低饱和、大留白、纤细字体、克制动效
 *
 * 同步映射说明：
 * - 颜色：iOS Color(red:green:blue:) → Android Color(0xAARRGGBB)
 * - 字体：iOS SF Pro Rounded(标题) / SF Pro(正文) → Android FontFamily.Default
 *         Android 系统字体 Roboto 已具备圆润特征，通过 FontWeight 区分层级
 * - 动画：iOS spring(response, dampingFraction) → Android spring(dampingRatio, stiffness)
 *         换算公式：dampingRatio = dampingFraction；stiffness = (2π / response)²
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
    // 对标 iOS Typography：
    //   标题使用 .rounded 设计风格，正文统一 .default
    //   Android 系统字体 Roboto 自身具备圆润特征，通过 FontWeight + lineHeight + letterSpacing 还原视觉层级

    object Typography {
        // 字体族 - 对标 iOS SF Pro
        val defaultFamily = FontFamily.Default
        val roundedFamily = FontFamily.Default   // 对标 iOS .rounded（Android 无 SF Pro Rounded，使用默认字体 + 字重区分）
        val monoFamily = FontFamily.Monospace    // 对标 iOS .monospaced

        // 标题 - Rounded 设计风格
        val largeTitle = TextStyle(
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = roundedFamily,
            letterSpacing = titleKerning.sp,
            lineHeight = lineHeightFor(34f).sp
        )
        val title1 = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = roundedFamily,
            letterSpacing = titleKerning.sp,
            lineHeight = lineHeightFor(28f).sp
        )
        val title2 = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = roundedFamily,
            letterSpacing = titleKerning.sp,
            lineHeight = lineHeightFor(22f).sp
        )
        val title3 = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = roundedFamily,
            letterSpacing = titleKerning.sp,
            lineHeight = lineHeightFor(20f).sp
        )

        // 正文 - 统一 Default 设计风格
        val headline = TextStyle(
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = defaultFamily,
            letterSpacing = bodyKerning.sp,
            lineHeight = lineHeightFor(17f).sp
        )
        val body = TextStyle(
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = defaultFamily,
            letterSpacing = bodyKerning.sp,
            lineHeight = lineHeightFor(17f).sp
        )
        val callout = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = defaultFamily,
            letterSpacing = bodyKerning.sp,
            lineHeight = lineHeightFor(16f).sp
        )
        val subheadline = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = defaultFamily,
            letterSpacing = bodyKerning.sp,
            lineHeight = lineHeightFor(15f).sp
        )
        val footnote = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = defaultFamily,
            letterSpacing = captionKerning.sp,
            lineHeight = lineHeightFor(13f).sp
        )
        val caption1 = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = defaultFamily,
            letterSpacing = captionKerning.sp,
            lineHeight = lineHeightFor(12f).sp
        )
        val caption2 = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = defaultFamily,
            letterSpacing = captionKerning.sp,
            lineHeight = lineHeightFor(11f).sp
        )

        // 等宽数字（用于 EXIF 数据、计时器）
        val monoBody = TextStyle(
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = monoFamily,
            lineHeight = lineHeightFor(17f).sp
        )
        val monoCaption = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = monoFamily,
            lineHeight = lineHeightFor(13f).sp
        )
        val monoDigit = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = monoFamily,
            lineHeight = lineHeightFor(12f).sp
        )

        // 极简模式专用字体
        val minimalModeLabel = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = defaultFamily,
            lineHeight = lineHeightFor(11f).sp
        )
        val minimalFilterName = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = defaultFamily,
            lineHeight = lineHeightFor(10f).sp
        )
        val minimalControlLabel = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = defaultFamily,
            lineHeight = lineHeightFor(13f).sp
        )
        val minimalTimer = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = monoFamily,
            lineHeight = lineHeightFor(14f).sp
        )
        val minimalZoomIndicator = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = monoFamily,
            lineHeight = lineHeightFor(12f).sp
        )

        // 行高（Line Height）- 基于字体大小的 1.4 倍（对标 iOS lineHeight(for:)）
        fun lineHeightFor(size: Float): Float = size * 1.4f

        // 字间距（Kerning）- 对标 iOS Kerning 常量
        const val titleKerning: Float = -0.3f   // 标题：轻微负字间距提升紧凑感
        const val bodyKerning: Float = 0f       // 正文：保持默认
        const val captionKerning: Float = 0.2f  // 说明文字：轻微正字间距提升可读性
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
    // 对标 iOS Animation：spring(response, dampingFraction) 精确换算为 spring(dampingRatio, stiffness)
    //   dampingRatio = dampingFraction（两者均为 0~1，1 = 无振荡）
    //   stiffness = (2π / response)² ，单位 SpringStiffness（N/m，质量取 1）

    object Animation {

        /** iOS spring(response, dampingFraction) → Android spring(dampingRatio, stiffness) */
        private fun iosSpring(response: Double, dampingFraction: Float): SpringSpec<Float> {
            val stiffness = ((2.0 * PI) / response).let { (it * it).toFloat() }
            return spring(dampingRatio = dampingFraction, stiffness = stiffness)
        }

        // 基础缓动 - 对标 iOS easeIn/easeOut/easeInOut(duration: 0.2 / 0.2 / 0.25)
        val easeIn = tween<Float>(200, easing = EaseIn)
        val easeOut = tween<Float>(200, easing = EaseOut)
        val easeInOut = tween<Float>(250, easing = FastOutSlowInEasing)

        // 弹簧动画 - 魅族偏好柔软弹性（精确对标 iOS 参数）
        val quick = iosSpring(response = 0.25, dampingFraction = 0.75f)     // response 0.25, damping 0.75
        val smooth = iosSpring(response = 0.35, dampingFraction = 0.72f)    // response 0.35, damping 0.72
        val bouncy = iosSpring(response = 0.40, dampingFraction = 0.65f)    // response 0.40, damping 0.65
        val gentle = iosSpring(response = 0.50, dampingFraction = 0.85f)    // response 0.50, damping 0.85

        // 相机专用弹簧动画
        val shutterPress = iosSpring(response = 0.18, dampingFraction = 0.65f)     // response 0.18, damping 0.65
        val shutterRelease = iosSpring(response = 0.28, dampingFraction = 0.70f)   // response 0.28, damping 0.70
        val overlayFade = tween<Float>(250, easing = FastOutSlowInEasing)          // easeInOut 0.25
        val modeSlide = iosSpring(response = 0.35, dampingFraction = 0.78f)        // response 0.35, damping 0.78
        val filterReveal = iosSpring(response = 0.32, dampingFraction = 0.72f)     // response 0.32, damping 0.72
        val zoomPop = iosSpring(response = 0.22, dampingFraction = 0.65f)          // response 0.22, damping 0.65
        val snappy = iosSpring(response = 0.22, dampingFraction = 0.72f)           // response 0.22, damping 0.72

        // 自动隐藏延迟 - 对标 iOS autoHideDelay (3.0s)
        const val autoHideDelay: Long = 3000L

        // 通用的 IntOffset / IntSize 版本（用于 layout 动画，如模式切换滑动）
        fun iosSpringIntOffset(response: Double, dampingFraction: Float): SpringSpec<androidx.compose.ui.unit.IntOffset> {
            val stiffness = ((2.0 * PI) / response).let { (it * it).toFloat() }
            return spring(dampingRatio = dampingFraction, stiffness = stiffness)
        }

        fun iosSpringDp(response: Double, dampingFraction: Float): SpringSpec<Dp> {
            val stiffness = ((2.0 * PI) / response).let { (it * it).toFloat() }
            return spring(dampingRatio = dampingFraction, stiffness = stiffness)
        }
    }
}

// MARK: - Compose Modifier Extensions（核心视觉修饰器，对标 iOS ViewModifier）

/**
 * 毛玻璃效果（Glassmorphism）- 对标 iOS GlassmorphismModifier
 * iOS 使用 .ultraThinMaterial + 白色叠加 + 细描边
 * Android 12+ 可用 RenderEffect 实现真实模糊，这里采用半透明白色叠加 + 描边还原视觉
 */
fun Modifier.glassmorphism(
    cornerRadius: Dp = DesignSystem.CornerRadius.medium,
    opacity: Float = 0.08f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color.White.copy(alpha = opacity), RoundedCornerShape(cornerRadius))
    .border(
        width = DesignSystem.Stroke.widthThin,
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * 涟漪效果 - 对标 iOS RippleModifier
 * 点击时从触点位置扩散圆形涟漪并淡出（0.6s easeOut）
 */
fun Modifier.rippleEffect(
    color: Color = Color.White.copy(alpha = 0.2f)
): Modifier = composed {
    val ripples = remember { mutableStateListOf<RippleAnim>() }
    val scope = rememberCoroutineScope()

    this
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    val anim = RippleAnim(offset = offset, progress = Animatable(0f))
                    ripples.add(anim)
                    scope.launch {
                        anim.progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 600, easing = EaseOut)
                        )
                        ripples.remove(anim)
                    }
                }
            )
        }
        .drawWithContent {
            drawContent()
            ripples.forEach { r ->
                val p = r.progress.value
                drawCircle(
                    color = color.copy(alpha = (1f - p) * 0.4f),
                    radius = p * 80f,
                    center = r.offset
                )
            }
        }
}

private class RippleAnim(
    val offset: Offset,
    val progress: Animatable<Float, AnimationVector1D>
)

/**
 * 骨架屏 Shimmer 效果 - 对标 iOS ShimmerModifier
 * 线性渐变循环扫过，模拟加载占位
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0f)
    )

    this.drawWithContent {
        drawContent()
        val width = size.width
        val offset = translateAnim * width * 2 - width
        drawRect(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(offset, 0f),
                end = Offset(offset + width, size.height)
            )
        )
    }
}

/**
 * 按钮按压缩放 - 对标 iOS PressScaleModifier
 * 按下时缩放（默认 0.97），使用 quick 弹簧动画
 */
fun Modifier.pressScale(
    scale: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scaleValue by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = DesignSystem.Animation.quick,
        label = "pressScale"
    )

    this
        .scale(scaleValue)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
        .let { mod ->
            if (onClick != null) {
                mod.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
            } else {
                mod
            }
        }
}

/**
 * 发光效果 - 对标 iOS GlowModifier
 * 双层阴影还原 iOS shadow + shadow 组合
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
    .shadow(
        elevation = radius * 2,
        shape = CircleShape,
        ambientColor = color.copy(alpha = 0.15f),
        spotColor = color.copy(alpha = 0.06f)
    )

/**
 * 脉动动画 - 对标 iOS PulseModifier
 * 圆形描边从 1.0 缩放到 1.5 并淡出，循环播放
 */
fun Modifier.pulse(
    color: Color,
    durationMillis: Int = 1500
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    this.drawWithContent {
        drawContent()
        val radius = size.minDimension / 2f
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius * scale,
            style = Stroke(width = 2f)
        )
    }
}

/**
 * 标准阴影 - 对标 iOS subtleShadow()
 */
fun Modifier.subtleShadow(
    shape: Shape = DesignSystem.mediumRoundedShape
): Modifier {
    val shadow = DesignSystem.Shadows.subtle()
    return this.shadow(
        elevation = shadow.radius,
        shape = shape,
        ambientColor = shadow.color,
        spotColor = shadow.color
    )
}

/**
 * 浮动阴影 - 对标 iOS elevatedShadow()
 */
fun Modifier.elevatedShadow(
    shape: Shape = DesignSystem.mediumRoundedShape
): Modifier {
    val shadow = DesignSystem.Shadows.elevated()
    return this.shadow(
        elevation = shadow.radius,
        shape = shape,
        ambientColor = shadow.color,
        spotColor = shadow.color
    )
}

/**
 * 模态阴影 - 对标 iOS modalShadow()
 */
fun Modifier.modalShadow(
    shape: Shape = DesignSystem.xLargeRoundedShape
): Modifier {
    val shadow = DesignSystem.Shadows.modal()
    return this.shadow(
        elevation = shadow.radius,
        shape = shape,
        ambientColor = shadow.color,
        spotColor = shadow.color
    )
}

/**
 * 页面统一内边距 - 对标 iOS pagePadding()
 */
fun Modifier.pagePadding(): Modifier = this.padding(horizontal = DesignSystem.Spacing.Padding.container)

// MARK: - Button Components（完整按钮体系，对标 iOS ButtonStyle）

/**
 * 按钮尺寸 - 对标 iOS PrimaryButtonStyle.ButtonSize
 */
enum class DesignButtonSize(val height: Dp, val horizontalPadding: Dp) {
    SMALL(height = 36.dp, horizontalPadding = 16.dp),
    MEDIUM(height = 44.dp, horizontalPadding = 24.dp),
    LARGE(height = 52.dp, horizontalPadding = 32.dp);

    val typography: TextStyle
        get() = when (this) {
            SMALL -> DesignSystem.Typography.footnote
            MEDIUM -> DesignSystem.Typography.headline
            LARGE -> DesignSystem.Typography.title3
        }
}

/**
 * 主按钮 - 对标 iOS PrimaryButtonStyle
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    size: DesignButtonSize = DesignButtonSize.MEDIUM
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = DesignSystem.Animation.quick,
        label = "primaryButtonScale"
    )

    Text(
        text = text,
        style = size.typography,
        color = Color.White,
        modifier = modifier
            .scale(scale)
            .clip(DesignSystem.largeRoundedShape)
            .background(if (isEnabled) DesignSystem.Colors.primary else DesignSystem.Colors.gray3())
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled
            ) { onClick() }
            .padding(horizontal = size.horizontalPadding)
            .height(size.height)
            .wrapContentSize(Alignment.Center)
    )
}

/**
 * 次要按钮 - 对标 iOS SecondaryButtonStyle
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = DesignSystem.Animation.quick,
        label = "secondaryButtonScale"
    )

    Text(
        text = text,
        style = DesignSystem.Typography.headline,
        color = DesignSystem.Colors.textPrimary(),
        modifier = modifier
            .scale(scale)
            .glassmorphism()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 24.dp)
            .height(44.dp)
            .wrapContentSize(Alignment.Center)
    )
}

/**
 * 幽灵按钮 - 对标 iOS GhostButtonStyle
 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = DesignSystem.Animation.quick,
        label = "ghostButtonScale"
    )

    Text(
        text = text,
        style = DesignSystem.Typography.headline,
        color = if (isDestructive) DesignSystem.Colors.error else DesignSystem.Colors.primary,
        modifier = modifier
            .scale(scale)
            .clip(DesignSystem.largeRoundedShape)
            .background(
                if (isDestructive) DesignSystem.Colors.errorBg
                else DesignSystem.Colors.primary.copy(alpha = 0.08f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp)
            .height(44.dp)
            .wrapContentSize(Alignment.Center)
    )
}

/**
 * 图标按钮 - 对标 iOS IconButtonStyle
 */
@Composable
fun DesignIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = DesignSystem.Animation.quick,
        label = "iconButtonScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isActive) DesignSystem.Colors.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 内容由调用方通过 modifier 包裹 Icon 后传入；此处仅作为容器示例
    }
}
