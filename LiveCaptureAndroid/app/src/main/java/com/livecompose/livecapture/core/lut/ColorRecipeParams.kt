package com.livecompose.livecapture.core.lut

/**
 * 色彩配方参数
 * 完整的色彩调整系统参数集
 */
data class ColorRecipeParams(
    // === 基础调整 ===
    val exposure: Float = 0f,        // 曝光 -2.0 ~ +2.0
    val contrast: Float = 0f,         // 对比度 -100 ~ +100
    val highlights: Float = 0f,       // 高光 -100 ~ +100
    val shadows: Float = 0f,          // 阴影 -100 ~ +100
    val whites: Float = 0f,           // 白色 -100 ~ +100
    val blacks: Float = 0f,           // 黑色 -100 ~ +100
    val saturation: Float = 0f,       // 饱和度 -100 ~ +100
    val vibrance: Float = 0f,         // 自然饱和度 -100 ~ +100
    val temperature: Float = 0f,      // 色温 -100 ~ +100
    val tint: Float = 0f,             // 色调（绿/品） -100 ~ +100

    // === 效果调整 ===
    val fade: Float = 0f,             // 褪色 0 ~ 100
    val grain: Float = 0f,            // 颗粒 0 ~ 100
    val vignette: Float = 0f,         // 晕影 0 ~ 100
    val sharpening: Float = 0f,       // 锐化 0 ~ 100
    val blur: Float = 0f,             // 模糊 0 ~ 100

    // === 特殊效果 ===
    val bloom: Float = 0f,            // 高光扩散 (HDF) 0 ~ 100
    val dispersion: Float = 0f,       // 色散 0 ~ 100
    val bleach: Float = 0f,           // 留银冲洗 0 ~ 100
    val splitToneHighlight: Int = 0,  // 分离色调高光色值 (ARGB int)
    val splitToneShadow: Int = 0,     // 分离色调阴影色值 (ARGB int)

    // === 曲线控制点 (归一化 0~1) ===
    val curveRGB: List<Pair<Float, Float>> = emptyList(),

    // === 元信息 ===
    val presetName: String = "自定义"
) {
    /** 默认参数 */
    companion object {
        val DEFAULT = ColorRecipeParams()

        /** 重置为默认值 */
        fun reset(): ColorRecipeParams = DEFAULT

        /**
         * 从 Map 反序列化
         */
        fun fromMap(map: Map<String, Any>): ColorRecipeParams = ColorRecipeParams(
            exposure = (map["exposure"] as? Number)?.toFloat() ?: 0f,
            contrast = (map["contrast"] as? Number)?.toFloat() ?: 0f,
            highlights = (map["highlights"] as? Number)?.toFloat() ?: 0f,
            shadows = (map["shadows"] as? Number)?.toFloat() ?: 0f,
            saturation = (map["saturation"] as? Number)?.toFloat() ?: 0f,
            temperature = (map["temperature"] as? Number)?.toFloat() ?: 0f,
            tint = (map["tint"] as? Number)?.toFloat() ?: 0f,
            fade = (map["fade"] as? Number)?.toFloat() ?: 0f,
            grain = (map["grain"] as? Number)?.toFloat() ?: 0f,
            vignette = (map["vignette"] as? Number)?.toFloat() ?: 0f,
            sharpening = (map["sharpening"] as? Number)?.toFloat() ?: 0f,
            bloom = (map["bloom"] as? Number)?.toFloat() ?: 0f,
            bleach = (map["bleach"] as? Number)?.toFloat() ?: 0f
        )
    }

    /**
     * 检查是否为默认值（所有参数均为默认）
     */
    val isDefault: Boolean get() = this == DEFAULT

    /**
     * 序列化为 Map 用于持久化
     */
    fun toMap(): Map<String, Any> = mapOf(
        "exposure" to exposure, "contrast" to contrast,
        "highlights" to highlights, "shadows" to shadows,
        "saturation" to saturation, "temperature" to temperature,
        "tint" to tint, "fade" to fade, "grain" to grain,
        "vignette" to vignette, "sharpening" to sharpening,
        "bloom" to bloom, "bleach" to bleach
    )
}
