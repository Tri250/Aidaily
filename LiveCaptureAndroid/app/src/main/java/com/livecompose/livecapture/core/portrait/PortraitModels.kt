package com.livecompose.livecapture.core.portrait

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * 人像模式数据模型
 *
 * 对应 iOS 端 PortraitModels.swift，定义人像模式所需的所有数据模型，
 * 包括光效类型、美颜参数、虚化参数和人像检测结果的结构化定义。
 */

// MARK: - 人像光效类型

/**
 * 人像光效类型
 */
enum class PortraitLightingType {
    NATURAL,        // 自然光
    STUDIO_LIGHT,   // 摄影室灯光
    CONTOUR_LIGHT,  // 轮廓光
    STAGE_LIGHT,    // 舞台光
    STAGE_LIGHT_MONO; // 舞台光黑白

    val displayName: String
        get() = when (this) {
            NATURAL -> "自然光"
            STUDIO_LIGHT -> "摄影室灯光"
            CONTOUR_LIGHT -> "轮廓光"
            STAGE_LIGHT -> "舞台光"
            STAGE_LIGHT_MONO -> "舞台光黑白"
        }

    /** Material 图标键（用于 Compose 图标选择） */
    val iconKey: String
        get() = when (this) {
            NATURAL -> "sun"
            STUDIO_LIGHT -> "studio"
            CONTOUR_LIGHT -> "contour"
            STAGE_LIGHT -> "spotlight"
            STAGE_LIGHT_MONO -> "mono"
        }
}

// MARK: - 美颜参数

/**
 * 美颜参数
 */
data class BeautyParams(
    /** 磨皮强度 0-1 */
    val skinSmoothing: Float = 0.3f,
    /** 肤色调整 -1（冷白）到 1（暖黄） */
    val skinTone: Float = 0.0f,
    /** 亮眼强度 0-1 */
    val eyeBrightening: Float = 0.2f,
    /** 牙齿美白强度 0-1 */
    val teethWhitening: Float = 0.0f,
    /** 瘦脸强度 0-1 */
    val faceSlimming: Float = 0.0f,
    /** 祛痘强度 0-1 */
    val blemishRemoval: Float = 0.3f
) {
    companion object {
        /** 默认美颜参数 */
        val DEFAULT = BeautyParams()

        /** 关闭所有美颜 */
        val OFF = BeautyParams(
            skinSmoothing = 0f,
            skinTone = 0f,
            eyeBrightening = 0f,
            teethWhitening = 0f,
            faceSlimming = 0f,
            blemishRemoval = 0f
        )
    }

    /** 是否所有美颜效果都已关闭 */
    val isOff: Boolean
        get() = skinSmoothing == 0f && skinTone == 0f && eyeBrightening == 0f &&
            teethWhitening == 0f && faceSlimming == 0f && blemishRemoval == 0f
}

// MARK: - 虚化参数

/**
 * 虚化参数
 */
data class BokehParams(
    /** 模拟光圈值 f/1.4 - f/16 */
    val aperture: Float = 2.8f,
    /** 虚化光斑形状 */
    val bokehShape: BokehShape = BokehShape.CIRCLE,
    /** 虚化强度 0-1 */
    val intensity: Float = 1.0f
) {
    /**
     * 虚化光斑形状
     */
    enum class BokehShape {
        CIRCLE,
        HEXAGON,
        HEART,
        STAR;

        val displayName: String
            get() = when (this) {
                CIRCLE -> "圆形"
                HEXAGON -> "六边形"
                HEART -> "心形"
                STAR -> "星形"
            }

        val iconKey: String
            get() = when (this) {
                CIRCLE -> "circle"
                HEXAGON -> "hexagon"
                HEART -> "heart"
                STAR -> "star"
            }
    }

    /**
     * 根据光圈值计算模糊半径（映射 f/1.4 → 半径 30, f/16 → 半径 3）
     */
    val blurRadius: Float
        get() {
            val clampedAperture = aperture.coerceIn(1.4f, 16.0f)
            val normalized = (clampedAperture - 1.4f) / (16.0f - 1.4f)
            val radius = 30.0f - normalized * 27.0f
            return radius * intensity
        }
}

// MARK: - 人像检测结果

/**
 * 人像模式检测结果
 *
 * @param originalBitmap 原始图像
 * @param skinMask 皮肤区域掩码（白色=皮肤，黑色=非皮肤）
 * @param faceRects 检测到的人脸矩形（图像坐标系，左上为原点）
 * @param faceLandmarks 面部关键点坐标（相对于图像坐标）
 * @param hasPortrait 是否检测到人像
 */
data class PortraitResult(
    val originalBitmap: Bitmap,
    val skinMask: Bitmap? = null,
    val faceRects: List<RectF> = emptyList(),
    val faceLandmarks: List<android.graphics.PointF> = emptyList(),
    val hasPortrait: Boolean = false
)

// MARK: - 美颜预设

/**
 * 美颜预设
 */
enum class BeautyPreset {
    NATURAL,   // 自然（关闭）
    DELICATE,  // 精致
    GODDESS,   // 女神
    CUSTOM;    // 自定义

    val displayName: String
        get() = when (this) {
            NATURAL -> "自然"
            DELICATE -> "精致"
            GODDESS -> "女神"
            CUSTOM -> "自定义"
        }

    /** 获取预设对应的美颜参数 */
    fun params(): BeautyParams {
        return when (this) {
            NATURAL -> BeautyParams(
                skinSmoothing = 0f,
                skinTone = 0f,
                eyeBrightening = 0f,
                teethWhitening = 0f,
                faceSlimming = 0f,
                blemishRemoval = 0f
            )
            DELICATE -> BeautyParams(
                skinSmoothing = 0.4f,
                skinTone = 0.2f,
                eyeBrightening = 0.3f,
                teethWhitening = 0.2f,
                faceSlimming = 0.15f,
                blemishRemoval = 0.4f
            )
            GODDESS -> BeautyParams(
                skinSmoothing = 0.7f,
                skinTone = 0.5f,
                eyeBrightening = 0.6f,
                teethWhitening = 0.5f,
                faceSlimming = 0.4f,
                blemishRemoval = 0.7f
            )
            CUSTOM -> BeautyParams.DEFAULT
        }
    }
}
