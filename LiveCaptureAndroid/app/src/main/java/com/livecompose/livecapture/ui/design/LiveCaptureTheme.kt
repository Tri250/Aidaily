package com.livecompose.livecapture.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * LiveCapture 统一主题入口
 * 将 DesignSystem 的颜色/字体映射到 Material3，使全应用通过 MaterialTheme 访问一致 token
 * 对标 iOS 在 SwiftUI 中通过 DesignSystem 全局使用的设计语言
 *
 * 使用方式：
 *   LiveCaptureTheme { /* AppNavigation() */ }
 */

/**
 * 浅色 ColorScheme - 对标 iOS 浅色 gray 色阶
 * gray*() 为 @Composable（依赖 isSystemInDarkTheme），需在 Composable 上下文构建
 */
@Composable
private fun buildLightColorScheme() = lightColorScheme(
    primary = DesignSystem.Colors.primary,
    onPrimary = Color.White,
    primaryContainer = DesignSystem.Colors.primaryLight,
    onPrimaryContainer = DesignSystem.Colors.gray6(),
    secondary = DesignSystem.Colors.secondary,
    onSecondary = Color.White,
    tertiary = DesignSystem.Colors.accent,
    onTertiary = Color.White,
    background = DesignSystem.Colors.backgroundPrimary(),
    onBackground = DesignSystem.Colors.textPrimary(),
    surface = DesignSystem.Colors.backgroundSecondary(),
    onSurface = DesignSystem.Colors.textPrimary(),
    surfaceVariant = DesignSystem.Colors.backgroundTertiary(),
    onSurfaceVariant = DesignSystem.Colors.textSecondary(),
    surfaceTint = DesignSystem.Colors.primary,
    outline = DesignSystem.Colors.gray3(),
    outlineVariant = DesignSystem.Colors.gray2(),
    error = DesignSystem.Colors.error,
    onError = Color.White,
    errorContainer = DesignSystem.Colors.errorBg,
    onErrorContainer = DesignSystem.Colors.error
)

/**
 * 深色 ColorScheme - 对标 iOS 深色 gray 色阶
 */
@Composable
private fun buildDarkColorScheme() = darkColorScheme(
    primary = DesignSystem.Colors.primary,
    onPrimary = Color.White,
    primaryContainer = DesignSystem.Colors.primaryLight,
    onPrimaryContainer = DesignSystem.Colors.gray6(),
    secondary = DesignSystem.Colors.secondary,
    onSecondary = Color.White,
    tertiary = DesignSystem.Colors.accent,
    onTertiary = Color.White,
    background = DesignSystem.Colors.backgroundPrimary(),
    onBackground = DesignSystem.Colors.textPrimary(),
    surface = DesignSystem.Colors.backgroundSecondary(),
    onSurface = DesignSystem.Colors.textPrimary(),
    surfaceVariant = DesignSystem.Colors.backgroundTertiary(),
    onSurfaceVariant = DesignSystem.Colors.textSecondary(),
    surfaceTint = DesignSystem.Colors.primary,
    outline = DesignSystem.Colors.gray3(),
    outlineVariant = DesignSystem.Colors.gray2(),
    error = DesignSystem.Colors.error,
    onError = Color.White,
    errorContainer = DesignSystem.Colors.errorBg,
    onErrorContainer = DesignSystem.Colors.error
)

/**
 * Material3 Typography - 映射 DesignSystem.Typography
 * 对标 iOS Typography 全量字体层级
 */
private val LiveCaptureTypography = Typography(
    displayLarge = DesignSystem.Typography.largeTitle,
    displayMedium = DesignSystem.Typography.title1,
    displaySmall = DesignSystem.Typography.title2,
    headlineLarge = DesignSystem.Typography.title1,
    headlineMedium = DesignSystem.Typography.title2,
    headlineSmall = DesignSystem.Typography.title3,
    titleLarge = DesignSystem.Typography.title2,
    titleMedium = DesignSystem.Typography.headline,
    titleSmall = DesignSystem.Typography.subheadline,
    bodyLarge = DesignSystem.Typography.body,
    bodyMedium = DesignSystem.Typography.subheadline,
    bodySmall = DesignSystem.Typography.footnote,
    labelLarge = DesignSystem.Typography.headline,
    labelMedium = DesignSystem.Typography.footnote,
    labelSmall = DesignSystem.Typography.caption1
)

/**
 * 应用统一主题
 * @param darkTheme 是否深色模式，默认跟随系统
 * @param content 子内容
 */
@Composable
fun LiveCaptureTheme(
    // 相机应用默认强制深色模式，确保取景器与控件在 OLED 上呈现纯净黑色
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) buildDarkColorScheme() else buildLightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LiveCaptureTypography,
        content = content
    )
}
