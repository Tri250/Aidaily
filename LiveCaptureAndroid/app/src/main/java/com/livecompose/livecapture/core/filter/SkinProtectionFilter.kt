package com.livecompose.livecapture.core.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.lut.LutPreset
import com.livecompose.livecapture.core.lut.LutProcessor
import com.livecompose.livecapture.core.portrait.PortraitImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 皮肤保护滤镜
 *
 * 对应 iOS 端 SkinProtectionFilter.swift，在应用滤镜时保护人像皮肤区域，避免肤色被过度染色。
 *
 * ## 工作原理
 * 1. 使用 ML Kit Face Detection 检测人脸区域（替代 iOS VNDetectFaceRectanglesRequest）
 * 2. 扩展人脸矩形以覆盖颈部/肩膀（[faceRectExpansionRatio]）
 * 3. 构建皮肤遮罩 Bitmap（白色 = 皮肤区域，黑色 = 非皮肤区域），
 *    使用 [PortraitImageUtils.createOvalMask] 绘制椭圆并高斯模糊边缘
 * 4. 应用完整滤镜效果（非皮肤区域）和弱滤镜效果（皮肤区域）
 * 5. 使用 [PortraitImageUtils.blendWithMask] 混合两层结果
 *
 * ## 性能优化
 * - 人脸检测结果缓存（同一帧内复用）
 * - 仅在检测到人脸时启用皮肤保护
 *
 * @param lutProcessor LUT 处理器，用于应用滤镜
 */
class SkinProtectionFilter(
    private val lutProcessor: LutProcessor = LutProcessor()
) {

    companion object {
        private const val TAG = "SkinProtectionFilter"
    }

    /** 皮肤区域的滤镜强度（0-1），默认 0.3（即 30% 强度） */
    var skinFilterIntensity: Float = 0.3f

    /** 非皮肤区域的滤镜强度（0-1），默认 1.0（即 100% 强度） */
    var nonSkinFilterIntensity: Float = 1.0f

    /** 遮罩边缘模糊半径（像素） */
    var maskBlurRadius: Float = 30f

    /** 人脸检测区域扩展比例（用于覆盖颈部/肩膀） */
    var faceRectExpansionRatio: Float = 1.6f

    /** ML Kit 人脸检测器（替代 iOS VNDetectFaceRectanglesRequest） */
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()
    )

    /** 缓存的最近人脸检测结果 */
    private var cachedFaces: List<Face> = emptyList()
    private var cachedImageWidth: Int = 0
    private var cachedImageHeight: Int = 0

    // MARK: - 皮肤遮罩创建

    /**
     * 从图像创建皮肤区域遮罩
     *
     * @param bitmap 输入位图
     * @return 皮肤遮罩 Bitmap（白色 = 皮肤区域，黑色 = 非皮肤区域），无脸时返回全黑遮罩
     */
    suspend fun createSkinMask(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        // 检测人脸
        val faces = detectFaces(bitmap)
        cachedFaces = faces
        cachedImageWidth = width
        cachedImageHeight = height

        // 未检测到人脸，返回全黑遮罩
        if (faces.isEmpty()) {
            return@withContext createEmptyMask(width, height)
        }

        // 构建皮肤遮罩（扩展人脸矩形 + 椭圆 + 高斯模糊）
        val expandedRects = faces.map { expandFaceRect(it.boundingBox, width, height) }
        PortraitImageUtils.createOvalMask(width, height, expandedRects, maskBlurRadius)
    }

    /**
     * 创建全黑遮罩（无皮肤区域）
     */
    private fun createEmptyMask(width: Int, height: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        canvas.drawColor(Color.BLACK)
        return mask
    }

    /**
     * 扩展人脸矩形以覆盖更多皮肤区域（颈部/肩膀）
     *
     * ML Kit 的 boundingBox 是图像坐标系（左上角原点），与 iOS Vision 的归一化坐标不同。
     *
     * @param faceBounds 人脸边界框
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 扩展后的矩形（已裁剪到图像范围内）
     */
    private fun expandFaceRect(faceBounds: android.graphics.Rect, imageWidth: Int, imageHeight: Int): RectF {
        val centerX = faceBounds.exactCenterX()
        val centerY = faceBounds.exactCenterY()

        // 宽度扩展
        val expandedWidth = faceBounds.width() * faceRectExpansionRatio
        // 高度扩展（向下扩展更多以覆盖颈部）
        val expandedHeight = faceBounds.height() * faceRectExpansionRatio * 1.3f

        var left = centerX - expandedWidth / 2f
        var top = centerY - expandedHeight / 2f
        // 向下偏移以覆盖更多颈部区域
        top -= expandedHeight * 0.1f

        // 裁剪到图像范围内
        left = left.coerceIn(0f, imageWidth.toFloat())
        top = top.coerceIn(0f, imageHeight.toFloat())
        val right = (left + expandedWidth).coerceIn(0f, imageWidth.toFloat())
        val bottom = (top + expandedHeight).coerceIn(0f, imageHeight.toFloat())

        return RectF(left, top, right, bottom)
    }

    // MARK: - 皮肤保护滤镜应用

    /**
     * 应用带皮肤保护的滤镜
     *
     * 1. 创建皮肤遮罩
     * 2. 应用完整滤镜效果（非皮肤区域）
     * 3. 应用弱滤镜效果（皮肤区域）
     * 4. 使用遮罩混合：皮肤区域用弱滤镜，非皮肤区域用完整滤镜
     *
     * @param image 输入图像
     * @param preset 滤镜预设
     * @param intensity 基础滤镜强度
     * @return 处理后的图像（皮肤区域保护 + 非皮肤区域完整滤镜）
     */
    suspend fun applyFilterWithSkinProtection(
        image: Bitmap,
        preset: LutPreset,
        intensity: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. 创建皮肤遮罩
        val skinMask = createSkinMask(image)

        // 如果遮罩全黑（无脸），直接应用完整滤镜
        if (cachedFaces.isEmpty()) {
            return@withContext lutProcessor.applyPreset(image, preset) { /* 忽略进度 */ }
        }

        // 2. 应用完整滤镜效果（非皮肤区域）
        val fullFiltered = lutProcessor.applyPreset(image, preset) { /* 忽略进度 */ }

        // 3. 应用弱滤镜效果（皮肤区域）
        // 通过降低 intensity 实现：使用 skinFilterIntensity 比例
        val skinFiltered = if (skinFilterIntensity <= 0f) {
            // 皮肤区域不应用滤镜，直接用原图
            image.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            // 用更弱的强度应用滤镜（通过混合原图与滤镜结果实现）
            val filtered = lutProcessor.applyPreset(image, preset) { /* 忽略进度 */ }
            blendByIntensity(image, filtered, skinFilterIntensity * intensity)
        }

        // 4. 使用遮罩混合：皮肤区域用 skinFiltered，非皮肤区域用 fullFiltered
        // blendWithMask(foreground, background, mask)：掩码白色区域显示前景
        PortraitImageUtils.blendWithMask(skinFiltered, fullFiltered, skinMask)
    }

    /**
     * 按强度混合原图与滤镜图
     *
     * result = original * (1 - intensity) + filtered * intensity
     *
     * @param original 原图
     * @param filtered 滤镜后的图
     * @param intensity 混合强度（0-1）
     * @return 混合后的 Bitmap
     */
    private fun blendByIntensity(original: Bitmap, filtered: Bitmap, intensity: Float): Bitmap {
        val width = original.width
        val height = original.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 先画原图
        val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(original, 0f, 0f, bgPaint)

        // 再用 SRC_OVER + alpha 叠加滤镜图
        val fgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (intensity.coerceIn(0f, 1f) * 255).toInt()
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawBitmap(filtered, 0f, 0f, fgPaint)

        return result
    }

    // MARK: - 人脸检测

    /**
     * 检测图像中的人脸
     *
     * @param bitmap 输入位图
     * @return 人脸列表，失败返回空列表
     */
    private suspend fun detectFaces(bitmap: Bitmap): List<Face> =
        withContext(Dispatchers.Default) {
            return@withContext try {
                val image = InputImage.fromBitmap(bitmap, 0)
                Tasks.await(faceDetector.process(image))
            } catch (e: Exception) {
                AppLogger.w(TAG, "人脸检测失败", e)
                emptyList()
            }
        }

    /**
     * 检查图像中是否包含人脸
     *
     * @param bitmap 输入位图
     * @return 是否检测到人脸
     */
    suspend fun hasFace(bitmap: Bitmap): Boolean = withContext(Dispatchers.Default) {
        detectFaces(bitmap).isNotEmpty()
    }

    /**
     * 清除缓存的人脸检测结果
     */
    fun clearCache() {
        cachedFaces = emptyList()
        cachedImageWidth = 0
        cachedImageHeight = 0
    }
}
