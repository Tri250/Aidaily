package com.livecompose.livecapture.core.design

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设计令牌 - 统一的设计系统常量
 * Design Tokens - Unified design system constants
 *
 * 包含排版、动画、模糊效果和缓动曲线等设计基础元素
 */

// ============================================================
// Typography System / 排版系统
// ============================================================

/**
 * 字体大小令牌 - 适合中文显示的友好字体大小
 * Font size tokens - Chinese-friendly font sizes
 */
object FontSize {
    val Hero = 48.sp             // 倒计时数字 / Countdown numbers
    val DisplayLarge = 36.sp     // 大型展示标题 / Large display titles
    val DisplayMedium = 28.sp    // 中型展示标题 / Medium display titles
    val HeadlineLarge = 24.sp    // 大型标题 / Large headlines
    val HeadlineMedium = 20.sp   // 中型标题 / Medium headlines
    val TitleLarge = 18.sp       // 大型标题文本 / Large title text
    val TitleMedium = 16.sp      // 中型标题文本 / Medium title text
    val BodyLarge = 16.sp        // 大型正文 / Large body text
    val BodyMedium = 14.sp       // 中型正文 / Medium body text
    val BodySmall = 12.sp        // 小型正文 / Small body text
    val Label = 10.sp            // 标签文本 / Label text
}

/**
 * 字体粗细令牌
 * Font weight tokens
 */
object FontWeightTokens {
    val Light = androidx.compose.ui.text.font.FontWeight.Light
    val Regular = androidx.compose.ui.text.font.FontWeight.Normal
    val Medium = androidx.compose.ui.text.font.FontWeight.Medium
    val SemiBold = androidx.compose.ui.text.font.FontWeight.SemiBold
    val Bold = androidx.compose.ui.text.font.FontWeight.Bold
}

/**
 * 行高令牌 - 相对于字体大小的倍数
 * Line height tokens - Multipliers relative to font size
 */
object LineHeight {
    val Tight = 1.2f      // 紧凑行高 / Tight line height
    val Normal = 1.5f     // 正常行高 / Normal line height
    val Relaxed = 1.75f   // 宽松行高 / Relaxed line height
}

// ============================================================
// Chinese Font Family / 中文字体家族
// ============================================================

/**
 * 中文字体家族推荐 - 使用系统字体
 * Chinese font family recommendations using system fonts
 *
 * Android 系统会自动选择最适合的字体:
 * - 中文: Noto Sans SC (思源黑体)
 * - 英文: Roboto
 * - 数字: Roboto
 */
object ChineseFontFamily {
    /**
     * 默认系统字体 - 适合大多数场景
     * Default system font - Suitable for most scenarios
     */
    val Default: FontFamily = FontFamily.Default

    /**
     * 无衬线字体 - 现代简洁风格
     * Sans-serif font - Modern clean style
     *
     * Android 5.0+ 使用 Roboto + Noto Sans SC
     */
    val SansSerif: FontFamily = FontFamily.SansSerif

    /**
     * 等宽字体 - 适合数字显示和代码
     * Monospace font - Ideal for numbers and code
     *
     * 建议用于: 倒计时数字、计时器、代码片段
     */
    val Monospace: FontFamily = FontFamily.Monospace

    /**
     * 倒计时数字专用字体配置
     * Countdown number font configuration
     *
     * 使用等宽字体确保数字切换时宽度一致
     */
    val Countdown: FontFamily = FontFamily.Monospace
}

// ============================================================
// Animation System / 动画系统
// ============================================================

/**
 * 动画持续时间令牌 (毫秒)
 * Animation duration tokens (milliseconds)
 */
object AnimationDuration {
    const val DURATION_VERY_FAST = 100    // 极快 - 微交互、状态变化
    const val DURATION_FAST = 200         // 快速 - 小型元素动画
    const val DURATION_MEDIUM = 300       // 中等 - 标准过渡动画
    const val DURATION_SLOW = 500         // 慢速 - 重要状态转换
    const val DURATION_VERY_SLOW = 800    // 极慢 - 大型元素、复杂动画

    // 常用动画时长预设
    const val ENTER_ANIMATION = DURATION_MEDIUM
    const val EXIT_ANIMATION = DURATION_FAST
    const val SCALE_ANIMATION = DURATION_FAST
    const val FADE_ANIMATION = DURATION_MEDIUM
    const val SLIDE_ANIMATION = DURATION_MEDIUM
}

/**
 * 动画延迟令牌 (毫秒)
 * Animation delay tokens (milliseconds)
 */
object AnimationDelay {
    const val DELAY_SHORT = 50
    const val DELAY_MEDIUM = 100
    const val DELAY_LONG = 200
}

/**
 * 生成标准动画规范的工具函数
 * Utility function to create standard animation specs
 */
object AnimationSpecs {
    /**
     * 标准缓动动画
     * Standard easing animation
     */
    fun <T> standard(
        durationMillis: Int = AnimationDuration.DURATION_MEDIUM,
        delayMillis: Int = 0
    ): AnimationSpec<T> = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = FastOutSlowInEasing
    )

    /**
     * 快速进入动画
     * Fast enter animation
     */
    fun <T> fastEnter(): AnimationSpec<T> = tween(
        durationMillis = AnimationDuration.DURATION_FAST,
        easing = LinearOutSlowInEasing
    )

    /**
     * 慢速退出动画
     * Slow exit animation
     */
    fun <T> slowExit(): AnimationSpec<T> = tween(
        durationMillis = AnimationDuration.DURATION_MEDIUM,
        easing = FastOutSlowInEasing
    )

    /**
     * 线性动画
     * Linear animation
     */
    fun <T> linear(
        durationMillis: Int = AnimationDuration.DURATION_MEDIUM
    ): AnimationSpec<T> = tween(
        durationMillis = durationMillis,
        easing = LinearEasing
    )

    /**
     * 弹性动画 - 用于强调
     * Bounce animation - For emphasis
     */
    fun <T> bounce(): AnimationSpec<T> = tween(
        durationMillis = AnimationDuration.DURATION_SLOW,
        easing = FastOutSlowInEasing
    )
}

// ============================================================
// Blur Effects / 模糊效果
// ============================================================

/**
 * 模糊半径令牌 (像素)
 * Blur radius tokens (pixels)
 *
 * 用于 Glassmorphism (玻璃拟态) 效果
 * 建议搭配半透明背景色使用
 */
object BlurRadius {
    const val BLUR_RADIUS_SMALL = 8f    // 小模糊 - 轻微背景模糊
    const val BLUR_RADIUS_MEDIUM = 16f  // 中等模糊 - 标准玻璃效果
    const val BLUR_RADIUS_LARGE = 24f   // 大模糊 - 强调前景内容

    // 特殊用途模糊半径
    const val BLUR_RADIUS_OVERLAY = 32f  // 覆盖层模糊
    const val BLUR_RADIUS_MODAL = 48f    // 模态窗口模糊
}

/**
 * 模糊效果配置
 * Blur effect configuration
 */
object BlurConfig {
    /**
     * 默认模糊采样率
     * Default blur sampling rate
     */
    const val DEFAULT_SAMPLING = 1f

    /**
     * 高质量模糊采样率
     * High quality blur sampling rate
     */
    const val HIGH_QUALITY_SAMPLING = 2f
}

// ============================================================
// Easing Curves / 缓动曲线
// ============================================================

/**
 * 缓动曲线令牌
 * Easing curve tokens
 *
 * 提供自然的动画节奏感
 */
object EasingCurves {
    /**
     * 标准缓动 - 最常用的缓动曲线
     * Standard easing - Most commonly used
     *
     * 开始慢，中间快，结束慢
     * 适合: 大多数UI动画
     */
    val Standard = FastOutSlowInEasing

    /**
     * 进入缓动 - 元素进入视野
     * Enter easing - Elements entering view
     *
     * 开始快，结束慢
     * 适合: 淡入、滑入动画
     */
    val Enter = LinearOutSlowInEasing

    /**
     * 退出缓动 - 元素离开视野
     * Exit easing - Elements leaving view
     *
     * 开始慢，结束快
     * 适合: 淡出、滑出动画
     */
    val Exit = FastOutSlowInEasing

    /**
     * 线性缓动 - 恒定速度
     * Linear easing - Constant speed
     *
     * 适合: 进度条、连续动画
     */
    val Linear = LinearEasing

    /**
     * 强调缓动 - 显著变化
     * Emphasized easing - Significant change
     *
     * 更明显的加速和减速
     * 适合: 重要状态转换
     */
    val Emphasized = FastOutSlowInEasing
}

// ============================================================
// Spacing System / 间距系统
// ============================================================

/**
 * 间距令牌
 * Spacing tokens
 */
object Spacing {
    val None = 0.dp
    val ExtraSmall = 2.dp
    val Small = 4.dp
    val Medium = 8.dp
    val Large = 12.dp
    val ExtraLarge = 16.dp
    val Huge = 24.dp
    val Massive = 32.dp
}

// ============================================================
// Corner Radius System / 圆角系统
// ============================================================

/**
 * 圆角半径令牌
 * Corner radius tokens
 */
object CornerRadius {
    val None = 0.dp
    val Small = 4.dp
    val Medium = 8.dp
    val Large = 12.dp
    val ExtraLarge = 16.dp
    val Huge = 24.dp
    val Full = 9999.dp  // 完全圆形
}

// ============================================================
// Opacity System / 透明度系统
// ============================================================

/**
 * 透明度令牌
 * Opacity tokens
 */
object Opacity {
    const val Disabled = 0.38f
    const val MediumEmphasis = 0.6f
    const val HighEmphasis = 0.87f
    const val Full = 1.0f

    // 玻璃拟态透明度
    const val GlassLight = 0.1f
    const val GlassMedium = 0.2f
    const val GlassHeavy = 0.3f
}

// ============================================================
// Z-Index System / 层级系统
// ============================================================

/**
 * 层级令牌 - 控制元素叠加顺序
 * Z-index tokens - Control element stacking order
 */
object ZIndex {
    const val Background = 0
    const val Content = 1
    const val Overlay = 10
    const val FloatingAction = 20
    const val Modal = 30
    const val Tooltip = 40
    const val SystemUI = 50
}

// ============================================================
// Extensions / 扩展函数
// ============================================================

/**
 * 创建标准动画规范的扩展属性
 * Extension property for creating standard animation specs
 */
val Int.standardAnimation: AnimationSpec<Any>
    get() = AnimationSpecs.standard(this)