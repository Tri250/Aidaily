package com.livecompose.livecapture.core.lut

/**
 * LUT 配方 — 描述从参考照片提取的色彩映射
 * 包含稀疏控制点列表 (Source RGB → Target RGB)
 */
data class ControlPoint(
    val sourceR: Float,
    val sourceG: Float,
    val sourceB: Float,
    val targetR: Float,
    val targetG: Float,
    val targetB: Float,
    val confidence: Float = 1.0f
) {
    fun sourceArray() = floatArrayOf(sourceR, sourceG, sourceB)
    fun targetArray() = floatArrayOf(targetR, targetG, targetB)
}

data class LutRecipe(
    val controlPoints: List<ControlPoint>,
    val name: String = "Custom",
    val lutSize: Int = 33
) {
    fun isValid(): Boolean = controlPoints.size >= 6
}
