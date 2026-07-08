package com.livecompose.livecapture.core.portrait

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 美颜参数 UI 数据模型
 *
 * 面向美颜面板的响应式数据模型，复用 [BeautyParams] / [BeautyPreset]（定义于 PortraitModels.kt），
 * 在其基础上补充"红润"参数与面板所需的预设、开关状态。
 *
 * 各参数取值范围：
 * - skinSmoothing  磨皮      0..1
 * - skinTone       美白     -1（冷白）..1（暖黄）
 * - blemishRemoval 祛痘      0..1
 * - eyeBrightening 亮眼/大眼  0..1
 * - teethWhitening 牙齿美白    0..1
 * - faceSlimming   瘦脸      0..1
 * - ruddy          红润      0..1
 */
data class BeautySettings(
    val skinSmoothing: Float = 0f,
    val skinTone: Float = 0f,
    val blemishRemoval: Float = 0f,
    val eyeBrightening: Float = 0f,
    val teethWhitening: Float = 0f,
    val faceSlimming: Float = 0f,
    val ruddy: Float = 0f,
    val currentPreset: BeautyPreset = BeautyPreset.NATURAL,
    val isBeautyEnabled: Boolean = false
) {

    /** 转换为引擎使用的 [BeautyParams] */
    fun toBeautyParams(): BeautyParams = BeautyParams(
        skinSmoothing = skinSmoothing,
        skinTone = skinTone,
        eyeBrightening = eyeBrightening,
        teethWhitening = teethWhitening,
        faceSlimming = faceSlimming,
        blemishRemoval = blemishRemoval,
        ruddy = ruddy
    )

    /** 是否所有美颜效果均已关闭 */
    val isOff: Boolean
        get() = skinSmoothing == 0f && skinTone == 0f && blemishRemoval == 0f &&
            eyeBrightening == 0f && teethWhitening == 0f && faceSlimming == 0f && ruddy == 0f

    companion object {
        /** 默认（全部关闭） */
        val DEFAULT = BeautySettings()

        /**
         * 由 [BeautyParams] 构建 [BeautySettings]（红润默认为 0）。
         */
        fun fromParams(
            params: BeautyParams,
            preset: BeautyPreset = BeautyPreset.CUSTOM,
            enabled: Boolean = true
        ): BeautySettings = BeautySettings(
            skinSmoothing = params.skinSmoothing,
            skinTone = params.skinTone,
            blemishRemoval = params.blemishRemoval,
            eyeBrightening = params.eyeBrightening,
            teethWhitening = params.teethWhitening,
            faceSlimming = params.faceSlimming,
            ruddy = params.ruddy,
            currentPreset = preset,
            isBeautyEnabled = enabled
        )
    }
}

/**
 * 美颜预设 → [BeautySettings] 映射
 *
 * 在 [BeautyPreset.params] 基础上补充红润参数与面板开关状态。
 */
fun BeautyPreset.toBeautySettings(): BeautySettings = when (this) {
    BeautyPreset.NATURAL -> BeautySettings(
        currentPreset = BeautyPreset.NATURAL,
        isBeautyEnabled = false
    )
    BeautyPreset.DELICATE -> BeautySettings(
        skinSmoothing = 0.4f,
        skinTone = 0.2f,
        eyeBrightening = 0.3f,
        teethWhitening = 0.2f,
        faceSlimming = 0.15f,
        blemishRemoval = 0.4f,
        ruddy = 0.15f,
        currentPreset = BeautyPreset.DELICATE,
        isBeautyEnabled = true
    )
    BeautyPreset.GODDESS -> BeautySettings(
        skinSmoothing = 0.7f,
        skinTone = 0.5f,
        eyeBrightening = 0.6f,
        teethWhitening = 0.5f,
        faceSlimming = 0.4f,
        blemishRemoval = 0.7f,
        ruddy = 0.3f,
        currentPreset = BeautyPreset.GODDESS,
        isBeautyEnabled = true
    )
    BeautyPreset.CUSTOM -> BeautySettings(
        currentPreset = BeautyPreset.CUSTOM,
        isBeautyEnabled = true
    )
}

/**
 * 美颜滑块描述符
 *
 * 描述面板中单个滑块的图标、标签、取值范围及其对 [BeautySettings] 的读写方式。
 *
 * @param key 唯一标识
 * @param label 显示标签
 * @param icon 图标
 * @param rangeMin 最小值
 * @param rangeMax 最大值
 * @param getter 从 [BeautySettings] 读取当前值
 * @param setter 返回更新后的 [BeautySettings]
 */
data class BeautySliderDescriptor(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val rangeMin: Float,
    val rangeMax: Float,
    val getter: (BeautySettings) -> Float,
    val setter: (BeautySettings, Float) -> BeautySettings
)

/**
 * 美颜面板滑块集合（顺序即面板展示顺序，与 iOS BeautyPanelView 对齐）
 */
object BeautySliders {
    val all: List<BeautySliderDescriptor> = listOf(
        BeautySliderDescriptor(
            key = "smoothing",
            label = "磨皮",
            icon = Icons.Filled.AutoFixHigh,
            rangeMin = 0f,
            rangeMax = 1f,
            getter = { it.skinSmoothing },
            setter = { s, v -> s.copy(skinSmoothing = v) }
        ),
        BeautySliderDescriptor(
            key = "tone",
            label = "美白",
            icon = Icons.Filled.WbSunny,
            rangeMin = -1f,
            rangeMax = 1f,
            getter = { it.skinTone },
            setter = { s, v -> s.copy(skinTone = v) }
        ),
        BeautySliderDescriptor(
            key = "blemish",
            label = "祛痘",
            icon = Icons.Filled.Healing,
            rangeMin = 0f,
            rangeMax = 1f,
            getter = { it.blemishRemoval },
            setter = { s, v -> s.copy(blemishRemoval = v) }
        ),
        BeautySliderDescriptor(
            key = "eye",
            label = "亮眼",
            icon = Icons.Filled.RemoveRedEye,
            rangeMin = 0f,
            rangeMax = 1f,
            getter = { it.eyeBrightening },
            setter = { s, v -> s.copy(eyeBrightening = v) }
        ),
        BeautySliderDescriptor(
            key = "teeth",
            label = "牙齿美白",
            icon = Icons.Filled.SentimentSatisfied,
            rangeMin = 0f,
            rangeMax = 1f,
            getter = { it.teethWhitening },
            setter = { s, v -> s.copy(teethWhitening = v) }
        ),
        BeautySliderDescriptor(
            key = "slimming",
            label = "瘦脸",
            icon = Icons.Filled.Face,
            rangeMin = 0f,
            rangeMax = 1f,
            getter = { it.faceSlimming },
            setter = { s, v -> s.copy(faceSlimming = v) }
        ),
        BeautySliderDescriptor(
            key = "ruddy",
            label = "红润",
            icon = Icons.Filled.Favorite,
            rangeMin = 0f,
            rangeMax = 1f,
            getter = { it.ruddy },
            setter = { s, v -> s.copy(ruddy = v) }
        )
    )
}
