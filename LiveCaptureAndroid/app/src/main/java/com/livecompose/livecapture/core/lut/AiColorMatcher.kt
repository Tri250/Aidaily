package com.livecompose.livecapture.core.lut

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI 仿色编排器
 * 协调色彩分析、LUT 生成和应用的完整流程
 * 支持三种模式：单图仿色 / 图像对仿色 / 自定义控制点
 */
object AiColorMatcher {

    /**
     * 从单张风格照片生成 LUT（基于灰世界假设+对比度归一化）
     */
    suspend fun matchFromStyleImage(
        styleImage: Bitmap,
        recipeName: String = "StyleMatch"
    ): LutRecipe = withContext(Dispatchers.Default) {
        LocalImageAnalyzer.analyzeSingleStyleImage(styleImage, recipeName)
    }

    /**
     * 从源图+目标图图像对生成 LUT（精确像素级匹配）
     */
    suspend fun matchFromImagePair(
        source: Bitmap,
        target: Bitmap,
        recipeName: String = "PairMatch"
    ): LutRecipe = withContext(Dispatchers.Default) {
        LocalImageAnalyzer.analyzeSourceTargetImages(source, target, recipeName)
    }

    /**
     * 从自定义控制点生成 LUT
     */
    suspend fun matchFromControlPoints(
        controlPoints: List<ControlPoint>,
        recipeName: String = "Custom"
    ): LutRecipe = withContext(Dispatchers.Default) {
        LutRecipe(controlPoints, recipeName)
    }

    /**
     * 从 LutRecipe 生成 3D LUT 并应用到 Bitmap
     * @return 处理后的 Bitmap
     */
    suspend fun applyColorMatch(
        bitmap: Bitmap,
        recipe: LutRecipe,
        intensity: Float = 1f
    ): Bitmap = withContext(Dispatchers.Default) {
        val lut = LutGenerator.generateLut(recipe)
        applyLutToBitmap(bitmap, lut, intensity)
    }

    /**
     * 将 3D LUT 应用到 Bitmap
     */
    fun applyLutToBitmap(
        bitmap: Bitmap,
        lut: FloatArray,
        intensity: Float = 1f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val resultPixels = LutGenerator.applyLutToPixels(pixels, lut, intensity)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 导出 LUT 为 .cube 格式字符串
     */
    fun exportLutAsCube(recipe: LutRecipe): String {
        val lut = LutGenerator.generateLut(recipe)
        return LutGenerator.exportToCubeString(lut, recipe.name)
    }

    /**
     * 预处理图像：裁剪为正方形并缩放
     */
    fun preprocessImage(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val startX = (bitmap.width - size) / 2
        val startY = (bitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, startX, startY, size, size)

        return if (size > maxSize) {
            Bitmap.createScaledBitmap(cropped, maxSize, maxSize, true)
        } else {
            cropped
        }
    }
}
