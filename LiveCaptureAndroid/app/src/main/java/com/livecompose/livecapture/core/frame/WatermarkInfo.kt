package com.livecompose.livecapture.core.frame

import android.graphics.Paint
import android.graphics.Typeface

/**
 * 水印配置信息
 */
data class WatermarkInfo(
    val text: String = "",
    val isEnabled: Boolean = false,

    // 文字样式
    val textSize: Float = 14f,
    val textColor: Int = 0x80FFFFFF.toInt(),     // 半透明白色
    val fontName: String = "default",     // default / ds-digital (数码字体)

    // 位置
    val positionX: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val positionY: Float = 0.95f,        // Y 方向位置 0~1
    val marginDp: Float = 12f,            // 边距 dp

    // 角度
    val rotationDegrees: Float = 0f,      // 旋转角度

    // 透明度
    val alpha: Float = 0.5f,              // 0~1

    // 图片水印
    val logoBitmapPath: String? = null,    // 图片水印文件路径
    val logoScale: Float = 0.15f,          // 图片水印缩放比例 (相对于图片宽度)
    val logoAlpha: Float = 0.8f            // 图片水印透明度 0~1
) {
    companion object {
        val EMPTY = WatermarkInfo()
    }
}

/** 水印位置 */
enum class WatermarkPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER_CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

    fun toPaintAlign(): Paint.Align {
        return when (this) {
            TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> Paint.Align.LEFT
            TOP_CENTER, CENTER_CENTER, BOTTOM_CENTER -> Paint.Align.CENTER
            TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> Paint.Align.RIGHT
        }
    }
}
