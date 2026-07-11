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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
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
        // Student (Fast 轻量) 与 Teacher (Pro 完整) 两个 TFLite 模型文件
        private const val STUDENT_MODEL_FILE = "adacrop_student.tflite"
        private const val TEACHER_MODEL_FILE = "adacrop_teacher.tflite"
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
        private val DEFAULT_RESULT = CompositionResult(
            bbox = floatArrayOf(0.5f, 0.5f, 0.8f, 0.8f),
            action = ActionType.STOP,
            actionProbabilities = FloatArray(NUM_ACTIONS) { 1f / NUM_ACTIONS }
        )
    }

    /**
     * 模型变体: 与 SettingsRepository 的 detectionMode 对应
     * - STUDENT: Fast 模式, MobileNetV3-Small, 轻量 ~5fps
     * - TEACHER: Pro 模式, MobileNetV3-Large, 全帧率最高精度
     */
    enum class ModelVariant(val assetFile: String) {
        STUDENT(STUDENT_MODEL_FILE),
        TEACHER(TEACHER_MODEL_FILE)
    }

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null
    @Volatile
    private var isDualOutput = false
    @Volatile
    private var inputDataType: Int = 0 // 0 = float32, 1 = uint8

    // 当前已加载的变体, 用于判断是否需要切换重载
    @Volatile
    private var loadedVariant: ModelVariant? = null

    // 模型加载状态跟踪，防止重复加载
    private val isLoadStarted = AtomicBoolean(false)

    private val inferenceLock = ReentrantLock()

    private val inputBuffer: ByteBuffer
    // 复用 IntArray，避免每帧分配
    private val pixelBuffer: IntArray = IntArray(INPUT_SIZE * INPUT_SIZE)
    // 复用输出数组，避免每帧分配
    private val bboxOutput: Array<FloatArray> = Array(1) { FloatArray(NUM_BBOX_PARAMS) }
    private val actionOutput: Array<FloatArray> = Array(1) { FloatArray(NUM_ACTIONS) }
    private val outputMap: Map<Int, Any> = mapOf(0 to bboxOutput, 1 to actionOutput)
    private val dualInputs: Array<Any> = arrayOf(inputBuffer)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    // 模型加载中状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 模型加载失败状态（供 UI 提示降级模式）
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime

    // 当前活跃变体 (供 UI 显示当前模式)
    private val _activeVariant = MutableStateFlow<ModelVariant?>(null)
    val activeVariant: StateFlow<ModelVariant?> = _activeVariant

    init {
        // init 仅分配 ByteBuffer，不在主线程加载模型
        val numPixels = INPUT_SIZE * INPUT_SIZE * 3
        inputBuffer = ByteBuffer.allocateDirect(numPixels * BYTES_PER_CHANNEL)
        inputBuffer.order(ByteOrder.nativeOrder())
    }

    /**
     * 异步加载指定变体模型，必须在后台线程调用。
     * 线程安全：使用 AtomicBoolean 防止重复加载。
     * 失败时重置 isLoadStarted 允许重试。
     *
     * @param variant 模型变体, 默认 STUDENT (Fast 模式)
     */
    suspend fun loadModelAsync(variant: ModelVariant = ModelVariant.STUDENT) {
        // 若已加载相同变体则跳过
        if (loadedVariant == variant && _isReady.value) {
            Log.d(TAG, "$variant already loaded, skipping")
            return
        }
        if (!isLoadStarted.compareAndSet(false, true)) {
            Log.d(TAG, "Model load already started, skipping")
            return
        }
        _isLoading.value = true
        try {
            withContext(Dispatchers.IO) {
                // 切换变体时先释放旧 interpreter
                if (interpreter != null) {
                    releaseInterpreter()
                }
                loadModel(variant)
            }
        } finally {
            // try-finally 确保取消/异常时 isLoading 不卡死
            _isLoading.value = false
        }
    }

    /**
     * 切换模型变体 (Fast <-> Pro)。
     * 若目标变体与当前一致则不操作; 否则释放旧模型并加载新变体。
     */
    suspend fun switchVariant(variant: ModelVariant) {
        if (loadedVariant == variant && _isReady.value) {
            Log.d(TAG, "Already on $variant, no switch needed")
            return
        }
        Log.d(TAG, "Switching from $loadedVariant to $variant")
        // 重置加载锁以允许重新加载
        isLoadStarted.set(false)
        _loadFailed.value = false
        _isReady.value = false
        loadModelAsync(variant)
    }

    private fun loadModel(variant: ModelVariant) {
        try {
            val modelBuffer = loadModelFile(variant.assetFile)
            val options = Interpreter.Options()
            // 跟踪 NnApiDelegate 以便后续 close
            try {
                val delegate = NnApiDelegate()
                nnApiDelegate = delegate
                options.addDelegate(delegate)
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI not available, falling back to CPU")
            }
            // 根据设备 CPU 核数动态设置线程数，保留一个核心给 UI
            val numThreads = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(1, 4)
            options.setNumThreads(numThreads)

            interpreter = Interpreter(modelBuffer, options)

            val inputDetails = interpreter?.getInputDetails()
            val outputDetails = interpreter?.getOutputDetails()

            if (outputDetails != null && outputDetails.size >= 2) {
                isDualOutput = true
                Log.d(TAG, "${variant}: single-input dual-output (bbox + action_probs)")
            } else {
                isDualOutput = false
                Log.d(TAG, "${variant}: requires two-stage inference")
            }

            // 检查输入数据类型，用于预处理分支
            if (inputDetails != null && inputDetails.isNotEmpty()) {
                inputDataType = inputDetails[0].dataType()
                Log.d(TAG, "${variant}: input data type ${if (inputDataType == 0) "float32" else "uint8/other"}")
            }

            loadedVariant = variant
            _activeVariant.value = variant
            _isReady.value = true
            _loadFailed.value = false
            Log.d(TAG, "$variant model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $variant model: ${e.message}. Running in fallback mode.", e)
            _isReady.value = false
            _loadFailed.value = true
            // 加载失败时重置 isLoadStarted，允许后续重试
            isLoadStarted.set(false)
            // 清理可能已创建的 delegate
            nnApiDelegate?.close()
            nnApiDelegate = null
        }
    }

    /**
     * 加载指定模型文件并正确关闭 FD/Stream/Channel，避免 FD 泄漏
     * MappedByteBuffer 创建后不再依赖 FD，可安全关闭
     */
    private fun loadModelFile(assetFile: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(assetFile)
        assetFileDescriptor.use { afd ->
            FileInputStream(afd.fileDescriptor).use { fis ->
                val fileChannel = fis.channel
                return fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength
                )
            }
        }
    }

    /**
     * 释放当前 interpreter 与 delegate, 不重置加载状态 (供 switchVariant 复用)
     */
    private fun releaseInterpreter() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing interpreter", e)
        }
        interpreter = null
        nnApiDelegate?.close()
        nnApiDelegate = null
        _isReady.value = false
    }

    suspend fun analyze(bitmap: Bitmap): CompositionResult = withContext(Dispatchers.Default) {
        inferenceLock.withLock {
            val startTime = System.currentTimeMillis()

            val interp = interpreter ?: return@withContext defaultResult()

            var resizedBitmap: Bitmap? = null
            var croppedBitmap: Bitmap? = null
            try {
                resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
                preprocess(resizedBitmap)

                val bbox: FloatArray
                val actionProbs: FloatArray

                if (isDualOutput) {
                    // 复用输出数组，避免每帧分配
                    interp.runForMultipleInputsOutputs(dualInputs, outputMap)
                    bbox = bboxOutput[0]
                    actionProbs = actionOutput[0]
                } else {
                    interp.run(inputBuffer, bboxOutput)
                    bbox = bboxOutput[0]

                    croppedBitmap = cropAndResize(bitmap, bbox)
                    preprocess(croppedBitmap)

                    interp.run(inputBuffer, actionOutput)
                    actionProbs = actionOutput[0]
                }

                _inferenceTime.value = System.currentTimeMillis() - startTime

                val action = argMax(actionProbs)
                val confidence = actionProbs.maxOrNull() ?: 0.5f

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
            } finally {
                // 确保异常路径 Bitmap 也被回收，避免 native 内存泄漏
                resizedBitmap?.recycle()
                croppedBitmap?.recycle()
            }
        }
    }

    private fun calculateRuleOfThirdsScore(bbox: FloatArray): Float {
        val cx = bbox[0]
        val cy = bbox[1]
        var minDistance = Float.MAX_VALUE
        for ((px, py) in THIRD_LINE_POINTS) {
            val dist = hypot(cx - px, cy - py)
            minDistance = min(minDistance, dist)
        }
        return (1f - (minDistance / 0.5f)).coerceIn(0f, 1f)
    }

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
        return (minMargin / SAFETY_MARGIN).coerceIn(0f, 1f)
    }

    private fun estimateFaceCoverage(bbox: FloatArray, confidence: Float): Float {
        val bboxArea = bbox[2] * bbox[3]
        val idealCoverage = 0.15f
        val deviation = abs(bboxArea - idealCoverage) / idealCoverage
        val areaScore = max(0f, 1f - deviation)
        return (areaScore * confidence).coerceIn(0f, 1f)
    }

    /**
     * NHWC 格式预处理: [1, 224, 224, 3]
     * 复用 pixelBuffer 避免每帧分配 IntArray
     */
    private fun preprocess(bitmap: Bitmap) {
        inputBuffer.rewind()
        bitmap.getPixels(pixelBuffer, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        if (inputDataType == 0) {
            // float32: 归一化到 [0, 1]
            for (pixel in pixelBuffer) {
                inputBuffer.putFloat((pixel shr 16 and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixel shr 8 and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
        } else {
            // uint8: 原始像素值 0-255
            for (pixel in pixelBuffer) {
                inputBuffer.put((pixel shr 16 and 0xFF).toByte())
                inputBuffer.put((pixel shr 8 and 0xFF).toByte())
                inputBuffer.put((pixel and 0xFF).toByte())
            }
        }
    }

    /**
     * 裁切并缩放，增加零宽/零高与退化 bitmap 安全检查
     */
    private fun cropAndResize(bitmap: Bitmap, bbox: FloatArray): Bitmap {
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            Log.w(TAG, "cropAndResize: degenerate bitmap ${bitmap.width}x${bitmap.height}")
            return Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        }

        val cx = bbox[0]
        val cy = bbox[1]
        val w = bbox[2]
        val h = bbox[3]

        val left = ((cx - w / 2) * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = ((cy - h / 2) * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        // coerceIn 要求 min <= max，确保 left+1 <= bitmap.width
        val rightMax = (left + 1).coerceAtMost(bitmap.width)
        val right = ((cx + w / 2) * bitmap.width).toInt().coerceIn(left + 1, rightMax)
        val bottomMax = (top + 1).coerceAtMost(bitmap.height)
        val bottom = ((cy + h / 2) * bitmap.height).toInt().coerceIn(top + 1, bottomMax)

        val cropWidth = right - left
        val cropHeight = bottom - top

        // 防止零宽/零高导致 createBitmap 崩溃
        if (cropWidth <= 0 || cropHeight <= 0) {
            Log.w(TAG, "cropAndResize: invalid crop size ${cropWidth}x${cropHeight}, using full bitmap")
            return Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        }

        val cropped = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
        val scaled = Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)
        if (cropped !== scaled) {
            cropped.recycle()
        }
        return scaled
    }

    private fun argMax(probabilities: FloatArray): ActionType {
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 6
        return ActionType.entries.getOrElse(maxIndex) { ActionType.STOP }
    }

    private fun defaultResult(): CompositionResult = DEFAULT_RESULT

    fun close() {
        releaseInterpreter()
        loadedVariant = null
        _activeVariant.value = null
        isLoadStarted.set(false)
    }
}
