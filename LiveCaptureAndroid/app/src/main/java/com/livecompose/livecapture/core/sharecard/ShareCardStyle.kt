package com.livecompose.livecapture.core.sharecard

import android.graphics.Color

/**
 * 分享卡片样式数据模型
 *
 * 对标 iOS ShareCardStyle，定义每种卡片风格的视觉参数：背景色、文字颜色、
 * 内边距、圆角、水印（品牌区）位置等。生成器根据 [cardTheme] 应用对应的
 * 布局与装饰（胶片齿孔、杂志页眉横条、拍立得边框等），与 iOS 视觉效果保持一致。
 *
 * @property id 唯一标识
 * @property displayName 显示名称（中文）
 * @property cardTheme 主题枚举，决定装饰风格与布局策略
 * @property backgroundColor 卡片背景色
 * @property titleColor 主标题文字颜色
 * @property subtitleColor 副标题/日期文字颜色
 * @property paramColor 拍摄参数文字颜色
 * @property accentColor 强调色（杂志页眉横条、装饰线等）
 * @property horizontalPadding 水平内边距
 * @property topPadding 顶部留白
 * @property bottomReserved 底部预留高度（用于品牌区）
 * @property cardCornerRadius 卡片外圆角
 * @property photoCornerRadius 图片圆角
 * @property watermarkPosition 水印（品牌区）位置
 */
data class ShareCardStyle(
    val id: String,
    val displayName: String,
    val cardTheme: CardTheme,
    val backgroundColor: Int,
    val titleColor: Int,
    val subtitleColor: Int,
    val paramColor: Int,
    val accentColor: Int,
    val horizontalPadding: Float,
    val topPadding: Float,
    val bottomReserved: Float,
    val cardCornerRadius: Float,
    val photoCornerRadius: Float,
    val watermarkPosition: WatermarkPosition,
) {
    /** 卡片主题，决定装饰风格与布局策略 */
    enum class CardTheme { MINIMAL, FILM, MAGAZINE, POLAROID }

    /** 水印（品牌区）位置 */
    enum class WatermarkPosition { TOP, BOTTOM, BOTTOM_CENTER }

    companion object {
        /** 极简：白底，居中照片，品牌区位于底部 */
        val Minimal = ShareCardStyle(
            id = "minimal",
            displayName = "极简",
            cardTheme = CardTheme.MINIMAL,
            backgroundColor = Color.WHITE,
            titleColor = Color.BLACK,
            subtitleColor = gray(0.4f),
            paramColor = gray(0.5f),
            accentColor = gray(0.8f),
            horizontalPadding = 80f,
            topPadding = 120f,
            bottomReserved = 300f,
            cardCornerRadius = 24f,
            photoCornerRadius = 8f,
            watermarkPosition = WatermarkPosition.BOTTOM_CENTER,
        )

        /** 胶片：深色底，两侧齿孔装饰，品牌区位于底部 */
        val Film = ShareCardStyle(
            id = "film",
            displayName = "胶片",
            cardTheme = CardTheme.FILM,
            backgroundColor = gray(0.15f),
            titleColor = Color.WHITE,
            subtitleColor = gray(0.7f),
            paramColor = gray(0.6f),
            accentColor = gray(0.25f),
            horizontalPadding = 40f,
            topPadding = 40f,
            bottomReserved = 200f,
            cardCornerRadius = 24f,
            photoCornerRadius = 2f,
            watermarkPosition = WatermarkPosition.BOTTOM_CENTER,
        )

        /** 杂志：暖米色底，红色页眉横条，品牌区位于顶部与底部 */
        val Magazine = ShareCardStyle(
            id = "magazine",
            displayName = "杂志",
            cardTheme = CardTheme.MAGAZINE,
            backgroundColor = 0xFFFAF5EB.toInt(),
            titleColor = gray(0.2f),
            subtitleColor = gray(0.4f),
            paramColor = gray(0.5f),
            accentColor = 0xFFD94D40.toInt(),
            horizontalPadding = 30f,
            topPadding = 200f,
            bottomReserved = 200f,
            cardCornerRadius = 24f,
            photoCornerRadius = 4f,
            watermarkPosition = WatermarkPosition.BOTTOM_CENTER,
        )

        /** 拍立得：白色相纸边框，底部留白较大 */
        val Polaroid = ShareCardStyle(
            id = "polaroid",
            displayName = "拍立得",
            cardTheme = CardTheme.POLAROID,
            backgroundColor = 0xFFF7F7F2.toInt(),
            titleColor = gray(0.25f),
            subtitleColor = gray(0.45f),
            paramColor = gray(0.5f),
            accentColor = gray(0.85f),
            horizontalPadding = 50f,
            topPadding = 60f,
            bottomReserved = 100f,
            cardCornerRadius = 8f,
            photoCornerRadius = 2f,
            watermarkPosition = WatermarkPosition.BOTTOM_CENTER,
        )

        /** 全部预置样式，用于样式选择器展示 */
        val all: List<ShareCardStyle> = listOf(Minimal, Film, Magazine, Polaroid)
    }
}

/**
 * 生成指定灰度级别（0..1）的不透明灰色 ARGB 值。
 * R = G = B = level * 255。
 */
private fun gray(level: Float): Int {
    val v = (level.coerceIn(0f, 1f) * 255f).toInt()
    return (0xFF shl 24) or (v shl 16) or (v shl 8) or v
}
