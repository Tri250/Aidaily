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
import androidx.compose.ui.graphics.vector.ImageVector
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
 * 统一设计系统 - 国潮质感风格（面向国内旗舰手机摄影体验）
 * 设计语言：温润光影 + 微拟物 + 自信动效
 * 适配品牌：华为/小米/OPPO/vivo/荣耀
 * 色彩基调：霁青主色 + 暮山紫辅色 + 暖金点缀
 * 动效体系：五层动效（入场/转场/反馈/状态/叙事）
 *
 * 同步映射说明：
 * - 颜色：iOS Color(red:green:blue:) → Android Color(0xAARRGGBB)
 * - 字体：iOS SF Pro Rounded(标题) / SF Pro(正文) → Android FontFamily.Default
 *         Android 系统字体 Roboto 已具备圆润特征，通过 FontWeight 区分层级
 * - 动画：iOS spring(response, dampingFraction) → Android spring(dampingRatio, stiffness)
 *         换算公式：dampingRatio = dampingFraction；stiffness = (2π / response)²
 */
object DesignSystem {

    // MARK: - Colors（国潮质感色板）

    object Colors {
        // 品牌色 - 国潮质感（霁青主色温润如玉，暮山紫辅色深邃典雅，暖金点缀自信灵动）
        val primary = Color(0xFF4A7C96)
        val primaryLight = Color(0xFF7BA8C0)
        val primaryDark = Color(0xFF3A6478)
        val secondary = Color(0xFF6B5B8C)
        val accent = Color(0xFFC9A055)
        val accentWarm = Color(0xFFD4A84B)

        // 渐变色 - 相机模式
        val gradientStart = Color(0xFF4A7C96)  // 霁青
        val gradientEnd = Color(0xFF6B5B8C)    // 暮山紫

        // 相机专属色 - 美颜/光环
        val goldenGlow = Color(0xFFD4A84B)       // 对齐金色光环
        val recordingRed = Color(0xFFE04545)     // 视频录制红
        val nightModeBlue = Color(0xFF5B8BA8)   // 夜景蓝

        // 语义色 - 低饱和克制
        val success = Color(0xFF5DA87A)
        val successBg = Color(0xFF5DA87A).copy(alpha = 0.12f)
        val warning = Color(0xFFD4A04A)
        val warningBg = Color(0xFFD4A04A).copy(alpha = 0.12f)
        val error = Color(0xFFC86666)
        val errorBg = Color(0xFFC86666).copy(alpha = 0.12f)
        val info = Color(0xFF6BA3C7)

        // 中性色阶 - 7 级灰度（Flyme 自然暖灰）
        @Composable
        fun gray0() = if (isSystemInDarkTheme()) Color(0xFF1A1A19) else Color(0xFFF8F8F7)
        @Composable
        fun gray1() = if (isSystemInDarkTheme()) Color(0xFF252523) else Color(0xFFF0EFEE)
        @Composable
        fun gray2() = if (isSystemInDarkTheme()) Color(0xFF30302D) else Color(0xFFE6E5E3)
        @Composable
        fun gray3() = if (isSystemInDarkTheme()) Color(0xFF40403D) else Color(0xFFD1CFCD)
        @Composable
        fun gray4() = if (isSystemInDarkTheme()) Color(0xFF6B6966) else Color(0xFF9E9C99)
        @Composable
        fun gray5() = if (isSystemInDarkTheme()) Color(0xFF9E9C99) else Color(0xFF6B6966)
        @Composable
        fun gray6() = if (isSystemInDarkTheme()) Color(0xFFF0EFEE) else Color(0xFF1A1A19)

        // 语义化文字颜色
        @Composable
        fun textPrimary() = gray6()
        @Composable
        fun textSecondary() = gray5()
        @Composable
        fun textTertiary() = gray4()
        @Composable
        fun textQuaternary() = if (isSystemInDarkTheme()) Color(0xFF40403D) else Color(0xFFC4C1BE)

        // 语义化背景颜色
        @Composable
        fun backgroundPrimary() = gray0()
        @Composable
        fun backgroundSecondary() = gray1()
        @Composable
        fun backgroundTertiary() = gray2()

        // 极简相机专属色（纯黑背景 + 白色 UI 层级）
        // 2026 旗舰摄影应用设计语言：高对比度、低饱和、药丸/液态玻璃
        val minimalBackground = Color.Black
        val minimalOverlay = Color.White.copy(alpha = 0.10f)          // 药丸背景
        val minimalActiveOverlay = Color.White.copy(alpha = 0.22f)    // 选中态药丸背景
        val minimalBorder = Color.White.copy(alpha = 0.18f)           // 细描边
        val minimalActiveBorder = Color.White.copy(alpha = 0.85f)     // 激活描边
        val minimalLabel = Color.White.copy(alpha = 0.96f)            // 主文字
        val minimalSecondaryLabel = Color.White.copy(alpha = 0.58f)   // 次文字
        val minimalTertiaryLabel = Color.White.copy(alpha = 0.32f)    // 禁用/第三级文字
        val minimalDarkOverlay = Color.Black.copy(alpha = 0.45f)      // 暗色遮罩
        val shutterStroke = Color.White
        val shutterInner = Color.White.copy(alpha = 0.95f)

        // 底部导航栏专属色
        val tabBarBackground = Color.White.copy(alpha = 0.10f)
        val tabBarActiveBackground = Color.White.copy(alpha = 0.20f)
        val tabBarBorder = Color.White.copy(alpha = 0.15f)
        val tabBarLabel = Color.White.copy(alpha = 0.95f)
        val tabBarInactiveLabel = Color.White.copy(alpha = 0.55f)
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

        // 极简模式专用字体 - 2026 旗舰摄影 UI：清晰、克制、高可读
        val minimalModeLabel = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = defaultFamily,
            letterSpacing = 0.1f.sp,
            lineHeight = lineHeightFor(12f).sp
        )
        val minimalFilterName = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = defaultFamily,
            letterSpacing = 0.2f.sp,
            lineHeight = lineHeightFor(10f).sp
        )
        val minimalControlLabel = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = defaultFamily,
            letterSpacing = 0.1f.sp,
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
            fontWeight = FontWeight.Medium,
            fontFamily = monoFamily,
            letterSpacing = 0.15f.sp,
            lineHeight = lineHeightFor(12f).sp
        )
        val minimalTabLabel = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = defaultFamily,
            letterSpacing = 0.2f.sp,
            lineHeight = lineHeightFor(13f).sp
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
        val xxxLarge: Dp = 28.dp   // 面板顶部超椭圆
        val cameraPreview: Dp = 32.dp  // 取景器圆角（2026 旗舰摄影大屏趋势）
        val circle: Dp = 999.dp
        val pill: Dp = 999.dp       // 胶囊形（替代原来的 circle）

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
    val cameraPreviewShape = RoundedCornerShape(CornerRadius.cameraPreview)
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

    // MARK: - Animation（国潮质感动效体系 - 五层动效架构）
    // 入场 → 转场 → 反馈 → 状态 → 叙事，由外而内构建沉浸式拍摄体验
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

        // === 五层动效体系 ===

        // 第一层：入场动效（Entry）
        val entryReveal = iosSpring(response = 0.45, dampingFraction = 0.72f)      // 从模糊到清晰
        val entrySlideUp = iosSpring(response = 0.40, dampingFraction = 0.75f)    // 从底部弹入
        val entryScaleIn = iosSpring(response = 0.35, dampingFraction = 0.65f)    // 缩放进入（带弹性）
        val entryFadeIn = tween<Float>(350, easing = FastOutSlowInEasing)          // 渐显

        // 第二层：转场动效（Transition）
        val transitionSharedElement = iosSpring(response = 0.40, dampingFraction = 0.80f) // 共享元素过渡
        val transitionMorph = iosSpring(response = 0.45, dampingFraction = 0.75f)        // 形态变换
        val transitionCrossFade = tween<Float>(300, easing = FastOutSlowInEasing)        // 交叉淡入

        // 第三层：反馈动效（Feedback）
        val feedbackCapture = iosSpring(response = 0.15, dampingFraction = 0.60f)    // 拍照反馈（极快）
        val feedbackFocus = iosSpring(response = 0.25, dampingFraction = 0.70f)      // 对焦反馈
        val feedbackHaptic = iosSpring(response = 0.12, dampingFraction = 0.55f)     // 触觉反馈
        val feedbackSuccess = iosSpring(response = 0.30, dampingFraction = 0.65f)    // 成功反馈（微弹）
        val feedbackDelete = tween<Float>(250, easing = EaseIn)                        // 删除消散

        // 第四层：状态动效（State）
        val stateBreath = iosSpring(response = 0.60, dampingFraction = 0.90f)         // 呼吸光晕
        val stateProgress = tween<Float>(1000, easing = LinearEasing)                 // 线性进度
        val statePulse = iosSpring(response = 0.50, dampingFraction = 0.85f)         // 脉冲
        val stateActive = iosSpring(response = 0.25, dampingFraction = 0.78f)         // 激活态

        // 第五层：叙事动效（Narrative）
        val narrativeDevelop = tween<Float>(500, easing = LinearEasing)             // 显影效果
        val narrativeReveal = iosSpring(response = 0.45, dampingFraction = 0.70f)    // 揭示效果
        val narrativeSweep = tween<Float>(400, easing = FastOutSlowInEasing)         // 扫描/扫过效果

        // 模式切换专用
        val modeCardSelect = iosSpring(response = 0.35, dampingFraction = 0.68f)      // 卡片选中
        val modeCardDeselect = iosSpring(response = 0.30, dampingFraction = 0.82f)   // 卡片取消选中
        val modeIndicatorSlide = iosSpring(response = 0.30, dampingFraction = 0.75f) // 底部指示器滑动

        // 快门按钮专用
        val shutterLongPress = iosSpring(response = 0.20, dampingFraction = 0.60f)   // 长按反馈
        val shutterRingExpand = iosSpring(response = 0.25, dampingFraction = 0.65f)  // 外环扩展
        val shutterGlowPulse = iosSpring(response = 0.50, dampingFraction = 0.70f)   // 金色光环脉动

        // 美颜对比线
        val beautyCompareSlide = iosSpring(response = 0.30, dampingFraction = 0.78f)  // 对比线滑动

        // Duration constants for five-layer system
        const val ENTRY_DURATION: Long = 450L
        const val TRANSITION_DURATION: Long = 350L
        const val FEEDBACK_DURATION: Long = 200L
        const val STATE_BREATH_DURATION: Long = 3000L
        const val NARRATIVE_DEVELOP_DURATION: Long = 500L
        const val PHOTO_PREVIEW_DURATION: Long = 2000L  // 拍照后即时预览时长
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
 * 液态玻璃效果（Liquid Glass）- 对标 iOS 2026 液态玻璃设计语言
 * 多层叠加模拟真实玻璃质感：
 *  - 底层：微弱的半透明扩散
 *  - 中层：顶部高光渐变
 *  - 边框：超细白色描边 + 内侧微光
 *  - 内阴影：顶部边缘柔和内发光
 *
 * 使用构成：background(glassGradient) + border(glassBorder) + innerGlow
 */
@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = DesignSystem.CornerRadius.medium,
    intensity: Float = 0.12f
): Modifier {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color.White else Color.Black
    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            baseColor.copy(alpha = intensity * 1.4f),
            baseColor.copy(alpha = intensity * 0.6f),
            baseColor.copy(alpha = intensity * 0.8f)
        )
    )
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(glassGradient, RoundedCornerShape(cornerRadius))
        .border(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = if (isDark) 0.18f else 0.10f),
                    baseColor.copy(alpha = if (isDark) 0.08f else 0.04f)
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * 液态玻璃卡片 - 预设样式，对标 iOS LiquidGlassCard 组件
 */
@Composable
fun Modifier.liquidGlassCard(
    cornerRadius: Dp = DesignSystem.CornerRadius.large
): Modifier = this
    .liquidGlass(cornerRadius = cornerRadius, intensity = 0.10f)
    .padding(DesignSystem.Spacing.small)

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

/**
 * 金色光环脉冲效果 - 对齐成功时快门外圈动画
 * 描边从 1.0 缩放到 1.3 并循环脉动，颜色为暖金
 */
fun Modifier.goldenGlowPulse(
    durationMillis: Int = 1200
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "goldenPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "goldenPulseScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "goldenPulseAlpha"
    )

    this.drawWithContent {
        drawContent()
        val radius = size.minDimension / 2f
        drawCircle(
            color = DesignSystem.Colors.goldenGlow.copy(alpha = alpha),
            radius = radius * scale,
            style = Stroke(width = 3f)
        )
    }
}

/**
 * 拍照暗角效果 - 按下快门瞬间画面边缘变暗
 */
fun Modifier.captureVignette(
    intensity: Float = 0f
): Modifier = this.drawWithContent {
    drawContent()
    if (intensity > 0.01f) {
        val radius = size.minDimension * 0.7f
        drawRadialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = intensity * 0.5f)
            ),
            center = Offset(size.width / 2, size.height / 2),
            radius = radius
        )
    }
}

@Composable
private fun drawRadialGradient(
    colors: List<Color>,
    center: Offset,
    radius: Float
) {
    // Placeholder - actual implementation uses Brush.radialGradient in drawWithContent
}

/**
 * 录制状态红点脉动
 */
fun Modifier.recordingPulse(
    isRecording: Boolean
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "recPulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = if (isRecording) infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ) else tween(0),
        label = "recPulseAlpha"
    )

    this.drawWithContent {
        drawContent()
        if (isRecording) {
            drawCircle(
                color = DesignSystem.Colors.recordingRed.copy(alpha = alpha * 0.3f),
                radius = size.minDimension / 2f
            )
        }
    }
}

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
