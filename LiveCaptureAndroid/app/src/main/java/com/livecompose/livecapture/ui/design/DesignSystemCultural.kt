package com.livecompose.livecapture.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 国潮质感设计系统扩展
 *
 * 在 [DesignSystem] 基础上补充面向国内高端摄影用户的文化感知 token：
 * - 传统色彩：霁青、暮山紫、暖金、朱砂、黛青
 * - 影像情绪色：胶片暖、暗房青、银盐灰
 * - 拍摄上下文面板、胶片模拟标签等新增组件专用样式
 */
object DesignSystemCultural {

    // MARK: - 国潮色彩系统
    object Colors {
        /** 霁青 - 主品牌色，对应雨后初晴天空的青蓝色 */
        val jiQing = Color(0xFF3A7CA5)
        val jiQingLight = Color(0xFF6BA3C7)
        val jiQingDark = Color(0xFF265A7A)

        /** 暮山紫 - 辅色，黄昏山峦的朦胧紫色 */
        val muShanZi = Color(0xFF8B7AA8)
        val muShanZiLight = Color(0xFFB0A3C8)

        /** 暖金 - 点缀色，用于成就态、胶片感高光 */
        val warmGold = Color(0xFFD4A84B)
        val warmGoldLight = Color(0xFFE8C87A)

        /** 朱砂 - 强调色，用于录制、重要提示 */
        val zhuSha = Color(0xFFC84A43)

        /** 黛青 - 深色背景上的沉稳点缀 */
        val daiQing = Color(0xFF3D5A64)

        /** 银盐灰 - 胶片颗粒、复古质感 */
        val silverGrain = Color(0xFFB8B2A7)

        /** 暗房青 - 专业暗房氛围 */
        val darkroomCyan = Color(0xFF4A8B9A)

        /** 宣纸白 - 用于胶片标签底色 */
        val ricePaper = Color(0xFFF5F0E8)

        // 语义化映射（与国潮视觉语言对齐）
        val filmAccent = warmGold
        val culturalHighlight = jiQing
        val contextPanelBackground = Color.Black.copy(alpha = 0.45f)
        val contextPanelBorder = Color.White.copy(alpha = 0.12f)
    }

    // MARK: - 国潮字体层级扩展
    object Typography {
        /** 胶片标签主文字 */
        val filmLabelPrimary = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4f.sp,
            lineHeight = 16.sp
        )

        /** 胶片标签副文字（ISO/颗粒等参数） */
        val filmLabelSecondary = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2f.sp,
            lineHeight = 12.sp
        )

        /** 上下文面板主文字 */
        val contextPrimary = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2f.sp,
            lineHeight = 18.sp
        )

        /** 上下文面板副文字 */
        val contextSecondary = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.1f.sp,
            lineHeight = 14.sp
        )

        /** 农历/宜忌等特殊信息 */
        val contextCultural = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3f.sp,
            lineHeight = 12.sp
        )
    }

    // MARK: - 国潮专属尺寸
    object Dimensions {
        val contextPanelWidth = 140.dp
        val contextPanelItemHeight = 28.dp
        val filmBadgeHeight = 32.dp
        val filmBadgeMinWidth = 96.dp
    }
}
