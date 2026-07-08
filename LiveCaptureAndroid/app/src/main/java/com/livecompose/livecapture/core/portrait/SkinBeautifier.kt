package com.livecompose.livecapture.core.portrait

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.livecompose.livecapture.core.logger.AppLogger
import kotlin.math.abs

/**
 * 皮肤美颜处理器
 *
 * 对应 iOS 端 SkinBeautifier.swift，使用 ML Kit Face Detection 替代 Vision，
 * 使用 Bitmap + Canvas + ColorMatrix 替代 CoreImage 滤镜链。
 *
 * ## 核心功能
 * - [applyBeauty] 应用所有美颜效果（磨皮/肤色/祛痘/亮眼/牙齿美白/瘦脸）
 * - [createSkinMask] 创建皮肤区域掩码
 * - [createEyeRegionMask] 创建眼部区域掩码
 *
 * ## 技术栈
 * - ML Kit Face Detection：人脸检测与关键点
 * - RenderScript / ColorMatrix：图像处理
 */
class SkinBeautifier {

    companion object {
        private const val TAG = "SkinBeautifier"
    }

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )

    /**
     * 应用所有美颜效果
     *
     * @param image 输入图像
     * @param params 美颜参数
     * @param faces 人脸检测结果
     * @return 美颜后的图像
     */
    fun applyBeauty(
        image: Bitmap,
        params: BeautyParams,
        faces: List<Face>
    ): Bitmap {
        if (params.isOff || faces.isEmpty()) return image

        var result = image
        val width = image.width
        val height = image.height

        // 1. 创建皮肤掩码
        val skinMask = createSkinMask(width, height, faces)

        // 2. 磨皮（高斯模糊 + 掩码混合）
        if (params.skinSmoothing > 0f) {
            result = applySkinSmoothing(result, params.skinSmoothing, skinMask, width, height)
        }

        // 3. 肤色调整
        if (abs(params.skinTone) > 0.01f) {
            result = applySkinToneAdjustment(result, params.skinTone, skinMask, width, height)
        }

        // 4. 祛痘（中值滤波模拟）
        if (params.blemishRemoval > 0f) {
            result = applyBlemishRemoval(result, params.blemishRemoval, skinMask, width, height)
        }

        // 5. 亮眼（基于面部关键点）
        if (params.eyeBrightening > 0f) {
            result = applyEyeBrightening(result, params.eyeBrightening, faces, width, height)
        }

        // 6. 牙齿美白（基于面部关键点）
        if (params.teethWhitening > 0f) {
            result = applyTeethWhitening(result, params.teethWhitening, faces, width, height)
        }

        // 7. 瘦脸（基于面部区域）
        if (params.faceSlimming > 0f) {
            result = applyFaceSlimming(result, params.faceSlimming, faces, width, height)
        }

        // 8. 红润（基于皮肤掩码提升 R 通道）
        if (params.ruddy > 0f) {
            result = applyRuddy(result, params.ruddy, skinMask, width, height)
        }

        return result
    }

    // MARK: - 红润

    /**
     * 红润效果：在皮肤区域提升红色通道，使肤色更显红润健康
     * @param amount 红润强度 0-1
     */
    private fun applyRuddy(
        image: Bitmap,
        amount: Float,
        skinMask: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)
        val maskPixels = IntArray(width * height)
        skinMask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        // R 通道提升量：0-40
        val rBoost = (amount * 40f).toInt()

        for (i in pixels.indices) {
            val maskAlpha = (maskPixels[i] ushr 24) and 0xFF
            if (maskAlpha == 0) continue

            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)

            // 按掩码强度混合
            val blend = maskAlpha / 255f
            val newR = (r + rBoost * blend).toInt().coerceIn(0, 255)
            // 轻微降低绿色和蓝色以增强红润感
            val reduce = (rBoost * 0.15f * blend)
            val newG = (g - reduce).toInt().coerceIn(0, 255)
            val newB = (b - reduce).toInt().coerceIn(0, 255)

            pixels[i] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    // MARK: - 皮肤掩码生成

    /**
     * 创建皮肤区域掩码（基于人脸检测框）
     */
    private fun createSkinMask(width: Int, height: Int, faces: List<Face>): Bitmap {
        val rects = faces.map { face ->
            val bounds = face.boundingBox
            // 扩展到覆盖颈部/肩部
            RectF(
                bounds.left - bounds.width() * 0.2f,
                bounds.top - bounds.height() * 0.15f,
                bounds.right + bounds.width() * 0.2f,
                bounds.bottom + bounds.height() * 0.8f
            )
        }
        return PortraitImageUtils.createOvalMask(width, height, rects, blurSigma = 20f)
    }

    // MARK: - 磨皮

    /**
     * 磨皮：使用高斯模糊 + 掩码混合实现双边滤波效果
     */
    private fun applySkinSmoothing(
        image: Bitmap,
        amount: Float,
        skinMask: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        // 模糊半径随磨皮强度线性变化（范围 3-15）
        val blurRadius = 3f + amount * 12f
        val blurred = PortraitImageUtils.gaussianBlur(image, blurRadius)
        return PortraitImageUtils.blendWithMask(blurred, image, skinMask)
    }

    // MARK: - 肤色调整

    /**
     * 肤色调整：使用色温调整
     * amount < 0: 冷白皮（高色温），amount > 0: 暖黄皮（低色温）
     */
    private fun applySkinToneAdjustment(
        image: Bitmap,
        amount: Float,
        skinMask: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        val adjusted = PortraitImageUtils.adjustTemperature(image, amount)
        return PortraitImageUtils.blendWithMask(adjusted, image, skinMask)
    }

    // MARK: - 祛痘

    /**
     * 祛痘：使用较强模糊去除高频细节 + 锐化保持纹理
     */
    private fun applyBlemishRemoval(
        image: Bitmap,
        amount: Float,
        skinMask: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        val blurRadius = 1f + amount * 2f
        val cleaned = PortraitImageUtils.gaussianBlur(image, blurRadius)
        // 增强对比度模拟锐化
        val sharpened = PortraitImageUtils.adjustColorControls(
            cleaned,
            contrast = 1f + amount * 0.1f,
            saturation = 1f
        )
        return PortraitImageUtils.blendWithMask(sharpened, image, skinMask)
    }

    // MARK: - 亮眼

    /**
     * 亮眼：基于眼部关键点提亮眼睛区域
     */
    private fun applyEyeBrightening(
        image: Bitmap,
        amount: Float,
        faces: List<Face>,
        width: Int,
        height: Int
    ): Bitmap {
        val eyeRects = mutableListOf<RectF>()
        for (face in faces) {
            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
            val faceWidth = face.boundingBox.width()
            val eyeSize = faceWidth * 0.12f

            leftEye?.let {
                eyeRects.add(RectF(
                    it.position.x - eyeSize,
                    it.position.y - eyeSize * 0.7f,
                    it.position.x + eyeSize,
                    it.position.y + eyeSize * 0.7f
                ))
            }
            rightEye?.let {
                eyeRects.add(RectF(
                    it.position.x - eyeSize,
                    it.position.y - eyeSize * 0.7f,
                    it.position.x + eyeSize,
                    it.position.y + eyeSize * 0.7f
                ))
            }
        }
        if (eyeRects.isEmpty()) return image

        val eyeMask = PortraitImageUtils.createOvalMask(width, height, eyeRects, blurSigma = 5f)
        // 提亮眼部区域
        val brightened = PortraitImageUtils.adjustExposure(image, amount * 0.4f)
        return PortraitImageUtils.blendWithMask(brightened, image, eyeMask)
    }

    // MARK: - 牙齿美白

    /**
     * 牙齿美白：基于嘴部关键点美白牙齿区域
     */
    private fun applyTeethWhitening(
        image: Bitmap,
        amount: Float,
        faces: List<Face>,
        width: Int,
        height: Int
    ): Bitmap {
        val mouthRects = mutableListOf<RectF>()
        for (face in faces) {
            val mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
            val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
            val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

            if (mouth != null && mouthLeft != null && mouthRight != null) {
                val faceWidth = face.boundingBox.width()
                val mouthWidth = mouthRight.position.x - mouthLeft.position.x
                val mouthCenterX = (mouthLeft.position.x + mouthRight.position.x) / 2f
                val mouthCenterY = (mouthLeft.position.y + mouthRight.position.y + mouth.position.y) / 3f
                val mouthHeight = faceWidth * 0.04f

                mouthRects.add(RectF(
                    mouthCenterX - mouthWidth * 0.4f,
                    mouthCenterY - mouthHeight * 0.5f,
                    mouthCenterX + mouthWidth * 0.4f,
                    mouthCenterY + mouthHeight * 0.5f
                ))
            }
        }
        if (mouthRects.isEmpty()) return image

        val teethMask = PortraitImageUtils.createOvalMask(width, height, mouthRects, blurSigma = 3f)
        // 美白：降低饱和度 + 提亮
        val whitened = PortraitImageUtils.adjustColorControls(
            image,
            saturation = 1f - amount * 0.3f,
            brightness = amount * 0.15f
        )
        return PortraitImageUtils.blendWithMask(whitened, image, teethMask)
    }

    // MARK: - 瘦脸

    /**
     * 瘦脸：对面部下半部分进行轻微的水平压缩
     */
    private fun applyFaceSlimming(
        image: Bitmap,
        amount: Float,
        faces: List<Face>,
        width: Int,
        height: Int
    ): Bitmap {
        val firstFace = faces.firstOrNull() ?: return image
        val faceBounds = firstFace.boundingBox

        // 瘦脸区域：面部下半部分（下巴区域）
        val lowerFaceRect = RectF(
            faceBounds.left.toFloat(),
            (faceBounds.top + faceBounds.height() * 0.45f),
            faceBounds.right.toFloat(),
            faceBounds.bottom.toFloat()
        )

        val slimMask = PortraitImageUtils.createOvalMask(width, height, listOf(lowerFaceRect), blurSigma = 15f)

        // 水平缩放（瘦脸效果）
        val scaleX = 1f - amount * 0.08f
        val scaledBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        val matrix = android.graphics.Matrix().apply {
            postScale(scaleX, 1f, lowerFaceRect.centerX(), lowerFaceRect.centerY())
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(image, matrix, paint)

        return PortraitImageUtils.blendWithMask(scaledBitmap, image, slimMask)
    }

    // MARK: - 同步人脸检测

    /**
     * 同步检测人脸（阻塞调用）
     * 注意：ML Kit 默认异步，这里通过 await 实现。
     */
    fun detectFacesSync(bitmap: Bitmap): List<Face> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val task = faceDetector.process(image)
            // 阻塞等待结果
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
