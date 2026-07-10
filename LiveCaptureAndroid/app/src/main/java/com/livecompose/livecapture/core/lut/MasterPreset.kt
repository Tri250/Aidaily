package com.livecompose.livecapture.core.lut

import kotlinx.serialization.Serializable

/**
 * OMaster 大师预设数据模型
 *
 * 与 OMaster Community JSON 格式完全兼容：
 * - https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json
 * - https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json
 */
@Serializable
data class MasterPresetCollection(
    val version: Int,
    val name: String,
    val author: String = "@OMaster",
    val build: Int = 1,
    val presets: List<MasterPreset>
)

@Serializable
data class MasterPreset(
    val name: String,
    val author: String = "",
    val coverPath: String = "",
    val galleryImages: List<String> = emptyList(),
    val isNew: Boolean = false,
    val sections: List<PresetSection> = emptyList(),
    val tags: List<String> = emptyList(),
    val description: PresetDescription? = null
) {
    /** 用户是否收藏此预设 */
    var isFavorited: Boolean = false

    /** 本地使用次数 */
    var useCount: Int = 0
}

@Serializable
data class PresetSection(
    val title: String,
    val items: List<PresetParam>
)

@Serializable
data class PresetParam(
    val label: String,
    val value: String,
    val span: Int = 1
)

@Serializable
data class PresetDescription(
    val title: String = "",
    val content: String = ""
)

/**
 * 解析后的预设参数（用于图像处理管线）
 */
data class ParsedPresetParams(
    val filter: String = "",
    val filterIntensity: Float = 1.0f,
    val softLight: String = "无",
    val toneCurve: Float = 0f,
    val saturation: Float = 0f,
    val warmCool: Float = 0f,
    val cyanMagenta: Float = 0f,
    val sharpness: Float = 0f,
    val vignette: Boolean = false,
    val vignetteIntensity: Float = 0f,
    val hue: Float = 0f,
    val contrast: Float = 0f,
    val contrastHighlight: Float = 0f,
    val contrastShadow: Float = 0f,
    val brightness: Float = 0f,
    val clarity: Float = 0f,
    val grain: Float = 0f,
    val grainSize: Float = 0f,
    val dehaze: Float = 0f,
    // 专业模式参数
    val iso: String = "",
    val shutter: String = "",
    val exposure: Float = 0f,
    val colorTemp: Int = 0,
    val tone: Float = 0f
)