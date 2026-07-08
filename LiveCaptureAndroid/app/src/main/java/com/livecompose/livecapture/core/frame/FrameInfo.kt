package com.livecompose.livecapture.core.frame

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 相框信息数据类
 */
@Parcelize
data class FrameInfo(
    val id: String,
    val name: String,
    val description: String,
    val borderWidthPercent: Float = 0.04f,      // 边框宽度占图片宽度的百分比
    val borderColor: Long = 0xFFFFFFFF,           // 边框颜色 ARGB
    val backgroundColor: Long = 0xFF000000,       // 边框背景色 ARGB
    val cornerRadiusPercent: Float = 0f,         // 圆角百分比
    val innerPaddingPercent: Float = 0f          // 内边距百分比
) : Parcelable {

    companion object {
        /** 无边框 */
        val NONE: FrameInfo? = null

        /** 经典白边 */
        val CLASSIC_WHITE = FrameInfo(
            id = "classic_white",
            name = "经典白边",
            description = "传统摄影白边相框",
            borderColor = 0xFFF5F5F5,
            backgroundColor = 0xFFEEEEEE,
            borderWidthPercent = 0.06f
        )

        /** 徕卡风格 */
        val LEICA = FrameInfo(
            id = "leica",
            name = "徕卡",
            description = "经典徕卡红标风格",
            borderColor = 0xFFDD0000,
            backgroundColor = 0xFF1A1A1A,
            borderWidthPercent = 0.05f
        )

        /** 哈苏布 */
        val HASSELBLAD = FrameInfo(
            id = "hasselblad",
            name = "哈苏布",
            description = "哈苏方形中画幅风格",
            borderColor = 0xFF888888,
            backgroundColor = 0xFF222222,
            borderWidthPercent = 0.08f
        )

        /** XPAN 宽幅 */
        val XPAN = FrameInfo(
            id = "xpan",
            name = "XPAN",
            description = "哈苏 XPAN 宽幅胶片相机",
            borderColor = 0xFFCCBB99,
            backgroundColor = 0xFF1A1814,
            borderWidthPercent = 0.06f
        )

        /** 宝丽来 */
        val POLAROID = FrameInfo(
            id = "polaroid",
            name = "宝丽来",
            description = "即时成像相框",
            borderColor = 0xFFF0F0F0,
            backgroundColor = 0xFFFAF8F5,
            borderWidthPercent = 0.08f,
            cornerRadiusPercent = 0.02f,
            innerPaddingPercent = 0.02f
        )

        /** 时间戳 */
        val TIMESTAMP = FrameInfo(
            id = "timestamp",
            name = "时间戳",
            description = "底部橙色时间戳相框",
            borderColor = 0xFF333333,
            backgroundColor = 0xFF111111,
            borderWidthPercent = 0.04f
        )

        /** 黑边 */
        val BLACK_BORDER = FrameInfo(
            id = "black_border",
            name = "黑边",
            description = "简约黑色边框",
            borderColor = 0xFF000000,
            backgroundColor = 0xFF000000,
            borderWidthPercent = 0.05f
        )

        /** 无边框 */
        val NO_FRAME = FrameInfo(
            id = "no_frame",
            name = "无边框",
            description = "不添加任何边框",
            borderColor = 0x00000000,
            backgroundColor = 0x00000000,
            borderWidthPercent = 0f
        )

        /** 所有内置相框 */
        val ALL_BUILT_IN: List<FrameInfo> = listOf(
            NO_FRAME, CLASSIC_WHITE, LEICA, HASSELBLAD, XPAN,
            POLAROID, TIMESTAMP, BLACK_BORDER
        )
    }
}
