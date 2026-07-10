package com.livecompose.livecapture.core.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.livecompose.livecapture.core.detection.CompositionResult.ActionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Singleton
class AdacropInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AdacropInferenceEngine"
        private const val MODEL_FILE = "adacrop_student.tflite"
        private const val INPUT_SIZE = 224
        private const val NUM_ACTIONS = 7
        private const val NUM_BBOX_PARAMS = 4
        private const val BYTES_PER_CHANNEL = 4 // float32
        // 三分线交点 (归一化坐标)
        private val THIRD_LINE_POINTS = listOf(
            1f / 3f to 1f / 3f,
            1f / 3f to 2f / 3f,
            2f / 3f to 1f / 3f,
            2f / 3f to 2f / 3f
        )
        private const val SAFETY_MARGIN = 0.1f // 边缘安全区 10%
    }

    private var interpreter: Interpreter? = null
    private var isDualOutput = false
    private var inputDataType: Int = 0 // 0 = float32, 1 = uint8

    private val inputBuffer: ByteBuffer

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime

    init {
        val numPixels = INPUT_SIZE * INPUT_SIZE * 3
        inputBuffer = ByteBuffer.allocateDirect(numPixels * BYTES_PER_CHANNEL)
        inputBuffer.order(ByteOrder.nativeOrder())
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                try {
                    addDelegate(NnApiDelegate())
                } catch (e: Exception) {
                    Log.w(TAG, "NNAPI not available, falling back to CPU")
                }
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)

            val inputDetails = interpreter?.getInputDetails()
            val outputDetails = interpreter?.getOutputDetails()

            if (outputDetails != null && outputDetails.size >= 2) {
                isDualOutput = true
                Log.d(TAG, "Model is single-input dual-output (bbox + action_probs)")
            } else {
                isDualOutput = false
                Log.d(TAG, "Model requires two-stage inference")
            }

            // 检查输入数据类型，用于预处理分支
            if (inputDetails != null && inputDetails.isNotEmpty()) {
                inputDataType = inputDetails[0].dataType()
                Log.d(TAG, "Input data type: ${if (inputDataType == 0) "float32" else "uint8/other"}")
            }

            _isReady.value = true
            Log.d(TAG, "Model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}. Running in fallback mode.")
            _isReady.value = false
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    suspend fun analyze(bitmap: Bitmap): CompositionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        val interp = interpreter ?: return@withContext defaultResult()

        try {
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            preprocess(resizedBitmap)

            val bbox: FloatArray
            val actionProbs: FloatArray

            if (isDualOutput) {
                val bboxOutput = Array(1) { FloatArray(NUM_BBOX_PARAMS) }
                val actionOutput = Array(1) { FloatArray(NUM_ACTIONS) }
                val outputMap = mapOf(0 to bboxOutput, 1 to actionOutput)
                interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)
                bbox = bboxOutput[0]
                actionProbs = actionOutput[0]
                resizedBitmap.recycle()
            } else {
                val bboxOutput = Array(1) { FloatArray(NUM_BBOX_PARAMS) }
                interp.run(inputBuffer, bboxOutput)
                bbox = bboxOutput[0]

                val croppedBitmap = cropAndResize(bitmap, bbox)
                preprocess(croppedBitmap)

                val actionOutput = Array(1) { FloatArray(NUM_ACTIONS) }
                interp.run(inputBuffer, actionOutput)
                actionProbs = actionOutput[0]
                resizedBitmap.recycle()
                croppedBitmap.recycle()
            }

            _inferenceTime.value = System.currentTimeMillis() - startTime

            val action = argMax(actionProbs)
            val confidence = actionProbs.maxOrNull() ?: 0.5f

            // 激活美学评分计算
            val ruleOfThirdsScore = calculateRuleOfThirdsScore(bbox)
            val safetyMarginScore = calculateSafetyMarginScore(bbox)
            val faceCoverage = estimateFaceCoverage(bbox, confidence)

            CompositionResult(
                bbox = bbox,
                action = action,
                actionProbabilities = actionProbs,
                confidence = confidence,
                faceCoverage = faceCoverage,
                ruleOfThirdsScore = ruleOfThirdsScore,
                safetyMarginScore = safetyMarginScore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
            defaultResult()
        }
    }

    /**
     * 计算三分法构图得分: bbox 中心与最近三分线交点的距离
     * 距离越近得分越高
     */
    private fun calculateRuleOfThirdsScore(bbox: FloatArray): Float {
        val cx = bbox[0]
        val cy = bbox[1]
        var minDistance = Float.MAX_VALUE
        for ((px, py) in THIRD_LINE_POINTS) {
            val dist = hypot(cx - px, cy - py)
            minDistance = min(minDistance, dist)
        }
        // 距离 0 → 得分 1.0, 距离 >= 0.5 → 得分 0
        return (1f - (minDistance / 0.5f)).coerceIn(0f, 1f)
    }

    /**
     * 计算边缘安全区得分: bbox 是否触及画面边缘
     * 离边缘越远得分越高
     */
    private fun calculateSafetyMarginScore(bbox: FloatArray): Float {
        val cx = bbox[0]
        val cy = bbox[1]
        val w = bbox[2]
        val h = bbox[3]
        val left = cx - w / 2f
        val top = cy - h / 2f
        val right = cx + w / 2f
        val bottom = cy + h / 2f

        val leftMargin = left
        val topMargin = top
        val rightMargin = 1f - right
        val bottomMargin = 1f - bottom

        val minMargin = min(min(leftMargin, rightMargin), min(topMargin, bottomMargin))
        // minMargin >= SAFETY_MARGIN → 得分 1.0, minMargin <= 0 → 得分 0
        return (minMargin / SAFETY_MARGIN).coerceIn(0f, 1f)
    }

    /**
     * 估算人脸覆盖率: 基于 bbox 面积与置信度
     * 无独立人脸检测模型时，用 bbox 面积和置信度估算
     */
    private fun estimateFaceCoverage(bbox: FloatArray, confidence: Float): Float {
        val bboxArea = bbox[2] * bbox[3]
        // 理想人脸覆盖范围 0.05~0.25，过大或过小都降低得分
        val idealCoverage = 0.15f
        val deviation = abs(bboxArea - idealCoverage) / idealCoverage
        val areaScore = max(0f, 1f - deviation)
        return (areaScore * confidence).coerceIn(0f, 1f)
    }

    /**
     * NHWC 格式预处理: [1, 224, 224, 3]
     * 根据 inputDataType 选择 float32 或 uint8 写入
     */
    private fun preprocess(bitmap: Bitmap) {
        inputBuffer.rewind()
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        if (inputDataType == 0) {
            // float32: 归一化到 [0, 1]
            for (pixel in pixels) {
                inputBuffer.putFloat((pixel shr 16 and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixel shr 8 and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
        } else {
            // uint8: 原始像素值 0-255
            for (pixel in pixels) {
                inputBuffer.put((pixel shr 16 and 0xFF).toByte())
                inputBuffer.put((pixel shr 8 and 0xFF).toByte())
                inputBuffer.put((pixel and 0xFF).toByte())
            }
        }
    }

    private fun cropAndResize(bitmap: Bitmap, bbox: FloatArray): Bitmap {
        val cx = bbox[0]
        val cy = bbox[1]
        val w = bbox[2]
        val h = bbox[3]

        val left = ((cx - w / 2) * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = ((cy - h / 2) * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = ((cx + w / 2) * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = ((cy + h / 2) * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)

        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        return Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)
    }

    private fun argMax(probabilities: FloatArray): ActionType {
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 6
        return ActionType.entries.getOrElse(maxIndex) { ActionType.STOP }
    }

    private fun defaultResult(): CompositionResult {
        return CompositionResult(
            bbox = floatArrayOf(0.5f, 0.5f, 0.8f, 0.8f),
            action = ActionType.STOP,
            actionProbabilities = FloatArray(NUM_ACTIONS) { 1f / NUM_ACTIONS }
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        _isReady.value = false
    }
}
