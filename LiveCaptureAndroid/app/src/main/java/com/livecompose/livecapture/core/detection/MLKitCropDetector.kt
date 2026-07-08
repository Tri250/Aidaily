package com.livecompose.livecapture.core.detection

import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.*

/**
 * 基于 ML Kit 的美学裁切检测器
 * 对应 iOS 的 AestheticCropDetector
 */
class MLKitCropDetector : CropDetectionStrategy {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }

    override fun detectBestCrop(
        pixelBuffer: ByteArray,
        width: Int,
        height: Int,
        rotation: Int,
        targetAspectRatio: Float,
        onResult: (AestheticCrop?) -> Unit
    ) {
        scope.launch {
            try {
                val yuvBytes = pixelBuffer
                val image = InputImage.fromByteArray(yuvBytes, width, height, rotation, InputImage.IMAGE_FORMAT_NV21)
                val faces = withContext(Dispatchers.IO) {
                    Tasks.await(faceDetector.process(image))
                }

                val candidates = mutableListOf<AestheticCrop>()

                faces.forEachIndexed { index, face ->
                    val boundingBox = face.boundingBox
                    val rx = boundingBox.left.toFloat() / width
                    val ry = boundingBox.top.toFloat() / height
                    val rw = boundingBox.width().toFloat() / width
                    val rh = boundingBox.height().toFloat() / height
                    val expandedRect = expandRect(rx, ry, rw, rh, 0.3f)
                    val cropRect = fitToAspectRatio(expandedRect, targetAspectRatio)
                    candidates.add(
                        AestheticCrop(
                            rect = cropRect,
                            confidence = 0.9f,
                            detectionType = "人脸#${index + 1}"
                        )
                    )
                }

                if (candidates.isEmpty()) {
                    withContext(Dispatchers.Main) { onResult(createCenterCrop(targetAspectRatio)) }
                    return@launch
                }

                val best = selectBestCandidate(candidates)
                withContext(Dispatchers.Main) { onResult(best) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(createCenterCrop(targetAspectRatio)) }
            }
        }
    }

    private fun expandRect(rx: Float, ry: Float, rw: Float, rh: Float, factor: Float): RectF {
        val dx = rw * factor / 2
        val dy = rh * factor / 2
        return RectF(
            (rx - dx).coerceIn(0f, 1f),
            (ry - dy).coerceIn(0f, 1f),
            (rx + rw + dx).coerceIn(0f, 1f),
            (ry + rh + dy).coerceIn(0f, 1f)
        )
    }

    private fun fitToAspectRatio(rect: RectF, target: Float): RectF {
        val cx = rect.centerX()
        val cy = rect.centerY()
        var w = rect.width()
        var h = rect.height()
        val currentRatio = if (h > 0f) w / h else 1f
        if (currentRatio > target) {
            h = w / target
        } else {
            w = h * target
        }
        var result = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        result.left = result.left.coerceIn(0f, 1f - result.width())
        result.top = result.top.coerceIn(0f, 1f - result.height())
        if (result.width() > 1f || result.height() > 1f) {
            val scale = 1f / maxOf(result.width(), result.height())
            result = RectF(cx - w * scale / 2, cy - h * scale / 2, cx + w * scale / 2, cy + h * scale / 2)
        }
        return result
    }

    private fun selectBestCandidate(candidates: List<AestheticCrop>): AestheticCrop? {
        if (candidates.isEmpty()) return null
        return candidates.maxByOrNull { candidate ->
            var score = 0f
            score += candidate.confidence * 0.4f
            score += calculateCompositionScore(candidate.rect) * 0.2f
            score += calculateMarginScore(candidate.rect) * 0.1f
            score += 0.3f // default face coverage
            score
        }
    }

    private fun calculateCompositionScore(rect: RectF): Float {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val thirdPoints = listOf(1f/3f to 1f/3f, 2f/3f to 1f/3f, 1f/3f to 2f/3f, 2f/3f to 2f/3f)
        val minDist = thirdPoints.minOf { (x, y) ->
            kotlin.math.sqrt((cx - x) * (cx - x) + (cy - y) * (cy - y))
        }
        return (1f - minDist / 0.5f).coerceAtLeast(0f)
    }

    private fun calculateMarginScore(rect: RectF): Float {
        val margins = listOf(rect.left, rect.top, 1f - rect.right, 1f - rect.bottom)
        val minMargin = margins.minOrNull() ?: 0f
        return (minMargin / 0.05f).coerceAtMost(1f)
    }

    private fun createCenterCrop(aspectRatio: Float): AestheticCrop {
        val maxSize = 0.75f
        val w: Float
        val h: Float
        if (aspectRatio >= 1f) {
            w = (maxSize * aspectRatio).coerceAtMost(maxSize)
            h = w / aspectRatio
        } else {
            h = (maxSize / aspectRatio).coerceAtMost(maxSize)
            w = h * aspectRatio
        }
        return AestheticCrop(
            rect = RectF(0.5f - w / 2, 0.5f - h / 2, 0.5f + w / 2, 0.5f + h / 2),
            confidence = 0.5f,
            detectionType = "默认中心"
        )
    }
}