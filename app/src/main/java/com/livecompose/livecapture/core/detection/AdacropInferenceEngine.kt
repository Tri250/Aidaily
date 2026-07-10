package com.livecompose.livecapture.core.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
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
    }

    private var interpreter: Interpreter? = null
    private val inputBuffer: ByteBuffer
    private val bboxOutputBuffer: ByteBuffer
    private val actionOutputBuffer: ByteBuffer

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime

    init {
        val numPixels = INPUT_SIZE * INPUT_SIZE * 3
        inputBuffer = ByteBuffer.allocateDirect(numPixels * BYTES_PER_CHANNEL)
        inputBuffer.order(ByteOrder.nativeOrder())

        bboxOutputBuffer = ByteBuffer.allocateDirect(NUM_BBOX_PARAMS * BYTES_PER_CHANNEL)
        bboxOutputBuffer.order(ByteOrder.nativeOrder())

        actionOutputBuffer = ByteBuffer.allocateDirect(NUM_ACTIONS * BYTES_PER_CHANNEL)
        actionOutputBuffer.order(ByteOrder.nativeOrder())

        loadModel()
    }

    private fun loadModel() {
        try {
            val options = Interpreter.Options().apply {
                // 优先使用 NNAPI 加速
                addDelegate(NnApiDelegate())
                setNumThreads(4)
            }
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer, options)
            _isReady.value = true
            Log.d(TAG, "Model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
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

    suspend fun analyze(imageProxy: ImageProxy): CompositionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. 将 ImageProxy 转换为 Bitmap 并 resize 到 224x224
        val bitmap = imageProxyToBitmap(imageProxy)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        // 2. 预处理: RGB 归一化到 [0, 1]
        preprocess(resizedBitmap)

        // 3. Stage 1: BBox Head 推理
        val interpreter = this@AdacropInferenceEngine.interpreter
            ?: return@withContext defaultResult()

        val bboxOutput = Array(1) { FloatArray(NUM_BBOX_PARAMS) }
        interpreter.run(inputBuffer, bboxOutput)
        val bbox = bboxOutput[0]

        // 4. Stage 2: Actor Policy 推理
        // 构建 state: 裁切图像 + bbox 状态
        val croppedBitmap = cropAndResize(bitmap, bbox)
        preprocess(croppedBitmap)

        val actionOutput = Array(1) { FloatArray(NUM_ACTIONS) }
        interpreter.run(inputBuffer, actionOutput)
        val actionProbs = actionOutput[0]
        val action = argMax(actionProbs)

        _inferenceTime.value = System.currentTimeMillis() - startTime

        resizedBitmap.recycle()
        croppedBitmap.recycle()

        CompositionResult(
            bbox = bbox,
            action = action,
            actionProbabilities = actionProbs,
            confidence = actionProbs.maxOrNull() ?: 0.5f
        )
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * imageProxy.width

        val bitmap = Bitmap.createBitmap(
            imageProxy.width + rowPadding / pixelStride,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)

        // 裁切掉 padding 部分
        return if (rowPadding > 0) {
            Bitmap.createBitmap(bitmap, 0, 0, imageProxy.width, imageProxy.height)
        } else {
            bitmap
        }
    }

    private fun preprocess(bitmap: Bitmap) {
        inputBuffer.rewind()
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
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
