package com.livecompose.livecapture.core.lut

/**
 * LUT 预设定义
 * 内置常用胶片色彩配方
 */
data class LutPreset(
    val id: String,
    val name: String,
    val category: LutCategory,
    val description: String,
    // 以下是简化的色彩参数（实际 LUT 使用 .cube 文件或着色器）
    val saturation: Float = 1f,
    val contrast: Float = 1f,
    val warmth: Float = 0f,       // -100 ~ 100
    val tint: Float = 0f,         // 绿-品 -100 ~ 100
    val highlights: Float = 1f,    // 高光 0~2
    val shadows: Float = 1f,       // 阴影 0~2
    val fade: Float = 0f,          // 褪色 0~1
    val grain: Float = 0f,         // 颗粒 0~1
    val vignette: Float = 0f,       // 晕影 0~1
    val sharpening: Float = 0f,     // 锐化 0~1
    val exposure: Float = 0f        // 曝光 -2~+2
)

enum class LutCategory(val displayName: String) {
    STANDARD("标准"),
    FILM("胶片"),
    PORTRAIT("人像"),
    LANDSCAPE("风景"),
    MONOCHROME("黑白"),
    VINTAGE("复古")
}

/** 内置预设列表 */
object BuiltInPresets {
    val presets: List<LutPreset> = listOf(
        LutPreset("original", "原片", LutCategory.STANDARD, "原始色彩", 1f, 1f, 0, 0, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f),

        // --- 胶片系列 ---
        LutPreset("portra400", "Kodak Portra 400", LutCategory.FILM,
            "经典人像胶片，柔和肤色表现",
            saturation = 1.05f, contrast = 1.02f, warmth = 8f, tint = -3f,
            highlights = 1.08f, shadows = 1.12f, fade = 0.08f, grain = 0.15f, vignette = 0.1f),

        LutPreset("velvia50", "Fujifilm Velvia 50", LutCategory.LANDSCAPE,
            "风景神片，超高饱和度和对比度",
            saturation = 1.40f, contrast = 1.18f, warmth = -5f, tint = 2f,
            highlights = 1.20f, shadows = 0.92f, fade = 0f, grain = 0.08f, vignette = 0.12f),

        LutPreset("kodak_gold", "Kodak Gold 200", LutCategory.VINTAGE,
            "日常金色记忆，温暖怀旧感",
            saturation = 1.15f, contrast = 0.98f, warmth = 18f, tint = -5f,
            highlights = 0.95f, shadows = 1.15f, fade = 0.15f, grain = 0.22f, vignette = 0.18f),

        LutPreset("fuji_c200", "Fujifilm C200", LutCategory.FILM,
            "富士入门卷，清新自然",
            saturation = 1.08f, contrast = 1.0f, warmth = 3f, tint = -1f,
            highlights = 1.02f, shadows = 1.05f, fade = 0.05f, grain = 0.1f, vignette = 0.06f),

        LutPreset("ilford_hp5", "Ilford HP5 Plus", LutCategory.MONOCHROME,
            "黑白专业胶片，丰富层次",
            saturation = 0f, contrast = 1.15f, warmth = -8f, tint = 0f,
            highlights = 0.88f, shadows = 1.20f, fade = 0.1f, grain = 0.30f, vignette = 0.15f),

        LutPreset("ricoh_gr", "Ricoh GR Positive", LutCategory.FILM,
            "街拍利器，高对比都市感",
            saturation = 1.12f, contrast = 1.20f, warmth = -2f, tint = 0f,
            highlights = 0.95f, shadows = 1.08f, fade = 0f, grain = 0.18f, vignette = 0.2f),

        // --- 人像系列 ---
        LutPreset("portrait_warm", "暖调人像", LutCategory.PORTRAIT,
            "温暖肤色，柔和高光",
            saturation = 1.0f, contrast = 0.96f, warmth = 12f, tint = -4f,
            highlights = 0.90f, shadows = 1.18f, fade = 0.1f, grain = 0f, vignette = 0.15f),

        LutPreset("portrait_fresh", "清新人像", LutCategory.PORTRAIT,
            "明亮通透，自然肤色",
            saturation = 1.06f, contrast = 0.94f, warmth = 5f, tint = -2f,
            highlights = 1.10f, shadows = 1.08f, fade = 0f, grain = 0f, vignette = 0.05f),

        // --- 风景系列 ---
        LutPreset("landscape_vivid", "鲜艳风景", LutCategory.LANDSCAPE,
            "增强自然饱和度",
            saturation = 1.25f, contrast = 1.08f, warmth = 2f, tint = 0f,
            highlights = 1.05f, shadows = 1.10f, fade = 0f, grain = 0f, vignette = 0.08f),

        LutPreset("teal_orange", "青橙色调", LutCategory.LANDSCAPE,
            "电影级青橙配色",
            saturation = 1.10f, contrast = 1.12f, warmth = -15f, tint = 8f,
            highlights = 1.05f, shadows = 1.15f, fade = 0.05f, grain = 0f, vignette = 0.12f),

        // --- 复古 ---
        LutPreset("vintage_sepia", "复古褐色", LutCategory.VINTAGE,
            "经典棕褐色调",
            saturation = 0.3f, contrast = 1.05f, warmth = 25f, tint = 10f,
            highlights = 0.95f, shadows = 1.10f, fade = 0.20f, grain = 0.25f, vignette = 0.20f),

        LutPreset("cinematic", "电影质感", LutCategory.STANDARD,
            "低饱和电影色调",
            saturation = 0.82f, contrast = 1.10f, warmth = -3f, tint = 2f,
            highlights = 0.92f, shadows = 1.18f, fade = 0.12f, grain = 0.12f, vignette = 0.22f)
    )

    fun findById(id: String): LutPreset? = presets.find { it.id == id }
    fun getByCategory(category: LutCategory): List<LutPreset> =
        presets.filter { it.category == category }
}
