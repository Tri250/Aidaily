package com.livecompose.livecapture.core.lut

/**
 * 9 通道 LCH 颜色调整参数
 * 基于 OKLCH 感知均匀色彩空间的混色器
 * 参考 PhotonCamera ColorRecipeParams LCH 部分
 */
data class LchColorAdjustment(
    // 皮肤通道（特殊：CIELAB 皮肤分类器）
    val skinHue: Float = 0f,
    val skinChroma: Float = 0f,
    val skinLightness: Float = 0f,
    // 8 个标准色相通道
    val redHue: Float = 0f,
    val redChroma: Float = 0f,
    val redLightness: Float = 0f,
    val orangeHue: Float = 0f,
    val orangeChroma: Float = 0f,
    val orangeLightness: Float = 0f,
    val yellowHue: Float = 0f,
    val yellowChroma: Float = 0f,
    val yellowLightness: Float = 0f,
    val greenHue: Float = 0f,
    val greenChroma: Float = 0f,
    val greenLightness: Float = 0f,
    val cyanHue: Float = 0f,
    val cyanChroma: Float = 0f,
    val cyanLightness: Float = 0f,
    val blueHue: Float = 0f,
    val blueChroma: Float = 0f,
    val blueLightness: Float = 0f,
    val purpleHue: Float = 0f,
    val purpleChroma: Float = 0f,
    val purpleLightness: Float = 0f,
    val magentaHue: Float = 0f,
    val magentaChroma: Float = 0f,
    val magentaLightness: Float = 0f
) {
    companion object {
        // 8 个色相频带中心角（度）
        val HUE_CENTERS = floatArrayOf(29f, 52f, 86f, 144f, 196f, 263f, 304f, 341f)
        val CHANNEL_NAMES = listOf(
            "Skin", "Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Purple", "Magenta"
        )
    }

    /** 获取指定通道的 (Hue, Chroma, Lightness) 调整值 */
    fun getChannel(index: Int): FloatArray = when (index) {
        0 -> floatArrayOf(skinHue, skinChroma, skinLightness)
        1 -> floatArrayOf(redHue, redChroma, redLightness)
        2 -> floatArrayOf(orangeHue, orangeChroma, orangeLightness)
        3 -> floatArrayOf(yellowHue, yellowChroma, yellowLightness)
        4 -> floatArrayOf(greenHue, greenChroma, greenLightness)
        5 -> floatArrayOf(cyanHue, cyanChroma, cyanLightness)
        6 -> floatArrayOf(blueHue, blueChroma, blueLightness)
        7 -> floatArrayOf(purpleHue, purpleChroma, purpleLightness)
        8 -> floatArrayOf(magentaHue, magentaChroma, magentaLightness)
        else -> floatArrayOf(0f, 0f, 0f)
    }

    /** 设置指定通道的调整值 */
    fun setChannel(index: Int, hue: Float, chroma: Float, lightness: Float): LchColorAdjustment {
        return when (index) {
            0 -> copy(skinHue = hue, skinChroma = chroma, skinLightness = lightness)
            1 -> copy(redHue = hue, redChroma = chroma, redLightness = lightness)
            2 -> copy(orangeHue = hue, orangeChroma = chroma, orangeLightness = lightness)
            3 -> copy(yellowHue = hue, yellowChroma = chroma, yellowLightness = lightness)
            4 -> copy(greenHue = hue, greenChroma = chroma, greenLightness = lightness)
            5 -> copy(cyanHue = hue, cyanChroma = chroma, cyanLightness = lightness)
            6 -> copy(blueHue = hue, blueChroma = chroma, blueLightness = lightness)
            7 -> copy(purpleHue = hue, purpleChroma = chroma, purpleLightness = lightness)
            8 -> copy(magentaHue = hue, magentaChroma = chroma, magentaLightness = lightness)
            else -> this
        }
    }

    /** 色彩密度效果（Vibrance 处理） */
    val hasAnyAdjustment: Boolean
        get() = skinHue != 0f || skinChroma != 0f || skinLightness != 0f ||
            redHue != 0f || redChroma != 0f || redLightness != 0f ||
            orangeHue != 0f || orangeChroma != 0f || orangeLightness != 0f ||
            yellowHue != 0f || yellowChroma != 0f || yellowLightness != 0f ||
            greenHue != 0f || greenChroma != 0f || greenLightness != 0f ||
            cyanHue != 0f || cyanChroma != 0f || cyanLightness != 0f ||
            blueHue != 0f || blueChroma != 0f || blueLightness != 0f ||
            purpleHue != 0f || purpleChroma != 0f || purpleLightness != 0f ||
            magentaHue != 0f || magentaChroma != 0f || magentaLightness != 0f
}
