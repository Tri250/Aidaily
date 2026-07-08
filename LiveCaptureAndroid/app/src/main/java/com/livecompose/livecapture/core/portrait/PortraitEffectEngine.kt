package com.livecompose.livecapture.core.portrait

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.livecompose.livecapture.core.logger.AppLogger

/**
 * 人像效果引擎
 *
 * 对应 iOS 端 PortraitEffectEngine.swift，使用 ML Kit Face Detection 替代 Vision，
 * 使用 Bitmap + Canvas + RenderScript 替代 CoreImage 滤镜链。
 *
 * ## 核心功能
 * - [detectPortrait] 使用 ML Kit 检测人脸区域和关键点
 * - [applyBokeh] 背景虚化
 * - [applyLighting] 人像光效（自然/摄影室/轮廓/舞台/舞台黑白）
 */
class PortraitEffectEngine {

    companion object {
        private const val TAG = "PortraitEffectEngine"
    }

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
    )

    // MARK: - 人像检测

    /**
     * 检测图像中的人像——返回人脸区域、关键点
     *
     * @param bitmap 输入图像
     * @return [PortraitResult] 包含检测结果
     */
    fun detectPortrait(bitmap: Bitmap): PortraitResult {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return PortraitResult(originalBitmap = bitmap, hasPortrait = false)
        }

        val faces = detectFacesSync(bitmap)
        if (faces.isEmpty()) {
            return PortraitResult(originalBitmap = bitmap, hasPortrait = false)
        }

        val faceRects = faces.map { face ->
            RectF(face.boundingBox)
        }

        // 创建皮肤掩码
        val skinMask = createSkinMask(bitmap, faces)

        return PortraitResult(
            originalBitmap = bitmap,
            skinMask = skinMask,
            faceRects = faceRects,
            hasPortrait = true
        )
    }

    // MARK: - 背景虚化

    /**
     * 应用背景虚化效果
     *
     * @param image 输入图像
     * @param params 虚化参数
     * @param faces 人脸检测结果
     * @return 虚化后的图像
     */
    fun applyBokeh(
        image: Bitmap,
        params: BokehParams,
        faces: List<Face>
    ): Bitmap {
        val width = image.width
        val height = image.height

        // 1. 创建主体掩码（人脸区域 = 前景，保持清晰）
        val foregroundMask = createForegroundMask(width, height, faces)

        // 2. 创建模糊背景
        val blurRadius = params.blurRadius.coerceIn(0f, 25f)
        val blurred = PortraitImageUtils.gaussianBlur(image, blurRadius)

        // 3. 使用掩码合成：前景清晰（背景图）+ 背景模糊（前景图）
        // mask 白色区域显示 backgroundImage（原图），黑色区域显示 inputImage（模糊图）
        var result = PortraitImageUtils.blendWithMask(blurred, image, foregroundMask)

        // 4. 应用光斑形状效果
        if (params.bokehShape != BokehParams.BokehShape.CIRCLE) {
            result = applyBokehShape(result, params.bokehShape)
        }

        return result
    }

    // MARK: - 人像光效

    /**
     * 应用人像光效
     *
     * @param image 输入图像
     * @param type 光效类型
     * @param faces 人脸检测结果
     * @return 应用光效后的图像
     */
    fun applyLighting(
        image: Bitmap,
        type: PortraitLightingType,
        faces: List<Face>
    ): Bitmap {
        val firstFace = faces.firstOrNull() ?: return image
        val faceRect = RectF(firstFace.boundingBox)

        return when (type) {
            PortraitLightingType.NATURAL -> image
            PortraitLightingType.STUDIO_LIGHT -> applyStudioLight(image, faceRect)
            PortraitLightingType.CONTOUR_LIGHT -> applyContourLight(image, faceRect)
            PortraitLightingType.STAGE_LIGHT -> applyStageLight(image, faceRect, mono = false)
            PortraitLightingType.STAGE_LIGHT_MONO -> applyStageLight(image, faceRect, mono = true)
        }
    }

    // MARK: - 光效实现

    /**
     * 摄影室灯光：提亮面部中心，轻微压暗边缘
     */
    private fun applyStudioLight(image: Bitmap, faceRect: RectF): Bitmap {
        val width = image.width
        val height = image.height

        // 创建径向渐变光照掩码（面部中心亮，边缘暗）
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        canvas.drawColor(Color.BLACK)
        val faceCenterX = faceRect.centerX()
        val faceCenterY = faceRect.centerY()
        val faceRadius = maxOf(faceRect.width(), faceRect.height()) * 0.8f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                faceCenterX, faceCenterY, faceRadius,
                intArrayOf(Color.WHITE, Color.argb(165, 165, 165, 165)),
                floatArrayOf(0.3f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 提亮版本
        val brightened = PortraitImageUtils.adjustExposure(image, 0.3f)
        val contrastAdjusted = PortraitImageUtils.adjustColorControls(
            brightened, contrast = 1.1f
        )
        return PortraitImageUtils.blendWithMask(contrastAdjusted, image, mask)
    }

    /**
     * 轮廓光：在面部一侧添加戏剧性阴影
     */
    private fun applyContourLight(image: Bitmap, faceRect: RectF): Bitmap {
        val width = image.width
        val height = image.height

        // 创建线性渐变（左暗右亮）
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val leftX = faceRect.left - faceRect.width() * 0.5f
        val rightX = faceRect.right + faceRect.width() * 0.5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                leftX, faceRect.centerY(),
                rightX, faceRect.centerY(),
                intArrayOf(Color.argb(76, 76, 76, 76), Color.WHITE),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 阴影侧（压暗）
        val shadowed = PortraitImageUtils.adjustExposure(image, -0.4f)
        val result = PortraitImageUtils.blendWithMask(shadowed, image, mask)

        // 增加整体对比度
        return PortraitImageUtils.adjustColorControls(
            result, contrast = 1.15f, saturation = 0.95f
        )
    }

    /**
     * 舞台光：聚光灯打在面部，背景变暗+模糊
     */
    private fun applyStageLight(image: Bitmap, faceRect: RectF, mono: Boolean): Bitmap {
        val width = image.width
        val height = image.height

        // 创建聚光灯掩码（从面部中心放射）
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        canvas.drawColor(Color.BLACK)
        val faceCenterX = faceRect.centerX()
        val faceCenterY = faceRect.centerY()
        val spotlightRadius = maxOf(faceRect.width(), faceRect.height()) * 1.2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                faceCenterX, faceCenterY, spotlightRadius,
                intArrayOf(Color.WHITE, Color.BLACK),
                floatArrayOf(0.15f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 亮点版本（面部区域）
        val brightened = PortraitImageUtils.adjustExposure(image, 0.5f)
        val contrastBrightened = PortraitImageUtils.adjustColorControls(
            brightened, contrast = 1.2f
        )

        // 暗化背景版本（模糊 + 压暗 + 降饱和度）
        val blurred = PortraitImageUtils.gaussianBlur(image, 8f)
        val darkened = PortraitImageUtils.adjustExposure(blurred, -1.5f)
        val desaturated = PortraitImageUtils.adjustColorControls(
            darkened, contrast = 0.8f, saturation = 0.3f
        )

        var result = PortraitImageUtils.blendWithMask(contrastBrightened, desaturated, mask)

        // 黑白模式
        if (mono) {
            result = PortraitImageUtils.adjustColorControls(
                result, saturation = 0f, contrast = 1.1f
            )
        }

        return result
    }

    // MARK: - 掩码生成

    /**
     * 创建前景掩码（人脸区域为白色=前景，其余为黑色=背景）
     */
    private fun createForegroundMask(width: Int, height: Int, faces: List<Face>): Bitmap {
        val rects = faces.map { face ->
            val bounds = face.boundingBox
            // 扩展并使用椭圆拟合
            RectF(
                bounds.left - bounds.width() * 0.25f,
                bounds.top - bounds.height() * 0.35f,
                bounds.right + bounds.width() * 0.25f,
                bounds.bottom + bounds.height() * 0.35f
            )
        }
        return PortraitImageUtils.createOvalMask(width, height, rects, blurSigma = 15f)
    }

    /**
     * 创建皮肤区域掩码
     */
    private fun createSkinMask(image: Bitmap, faces: List<Face>): Bitmap {
        val width = image.width
        val height = image.height
        val rects = faces.map { face ->
            val bounds = face.boundingBox
            // 扩展到颈部/肩部
            RectF(
                bounds.left - bounds.width() * 0.15f,
                bounds.top - bounds.height() * 0.1f,
                bounds.right + bounds.width() * 0.15f,
                bounds.bottom + bounds.height() * 0.6f
            )
        }
        return PortraitImageUtils.createOvalMask(width, height, rects, blurSigma = 10f)
    }

    /**
     * 应用光斑形状效果
     */
    private fun applyBokehShape(image: Bitmap, shape: BokehParams.BokehShape): Bitmap {
        return when (shape) {
            BokehParams.BokehShape.HEXAGON -> {
                // 六边形光斑：增加高光对比
                PortraitImageUtils.adjustColorControls(
                    image, contrast = 1.15f
                )
            }
            BokehParams.BokehShape.HEART -> {
                // 心形光斑：暖色调
                PortraitImageUtils.adjustTemperature(image, 0.3f)
            }
            BokehParams.BokehShape.STAR -> {
                // 星形光斑：高对比度 + 冷色调
                val contrasted = PortraitImageUtils.adjustColorControls(
                    image, contrast = 1.2f
                )
                PortraitImageUtils.adjustTemperature(contrasted, -0.2f)
            }
            BokehParams.BokehShape.CIRCLE -> image
        }
    }

    // MARK: - 同步人脸检测

    /**
     * 同步检测人脸（阻塞调用）
     */
    private fun detectFacesSync(bitmap: Bitmap): List<Face> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val task = faceDetector.process(image)
            Tasks.await(task)
            task.result ?: emptyList()
        } catch (e: Exception) {
            AppLogger.w(TAG, "人脸检测失败", e)
            emptyList()
        }
    }

    /**
     * 关闭检测器，释放资源
     */
    fun close() {
        try {
            faceDetector.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭人脸检测器失败", e)
        }
    }
}
