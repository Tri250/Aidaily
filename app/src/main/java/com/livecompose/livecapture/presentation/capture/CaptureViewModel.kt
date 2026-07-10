package com.livecompose.livecapture.presentation.capture

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine
import com.livecompose.livecapture.core.detection.CompositionResult
import com.livecompose.livecapture.core.motion.BoxCenterManager
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.settings.SettingsRepository
import com.livecompose.livecapture.core.storage.CropRegion
import com.livecompose.livecapture.core.storage.ExifData
import com.livecompose.livecapture.core.storage.PhotoStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val detectionEngine: AdacropInferenceEngine,
    private val motionMonitor: MotionStabilityMonitor,
    private val boxCenterManager: BoxCenterManager,
    private val storageService: PhotoStorageService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CaptureViewModel"
    }

    enum class PipelineStage {
        IDLE,
        STARTING_CAMERA,
        WAITING_FOR_STABILITY,
        DETECTING_REGION,
        TEMPLATE_READY,
        READY_TO_CAPTURE,
        CAPTURING_PHOTO,
        SAVING_PHOTO,
        ERROR
    }

    private val _pipelineStage = MutableStateFlow(PipelineStage.IDLE)
    val pipelineStage: StateFlow<PipelineStage> = _pipelineStage

    private val _guidanceText = MutableStateFlow("准备拍摄")
    val guidanceText: StateFlow<String> = _guidanceText

    private val _isDetectionReady = MutableStateFlow(false)
    val isDetectionReady: StateFlow<Boolean> = _isDetectionReady

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime

    private val _lastSavedPhotoPath = MutableStateFlow<String?>(null)
    val lastSavedPhotoPath: StateFlow<String?> = _lastSavedPhotoPath

    val trackPoint: StateFlow<PointF?> = boxCenterManager.trackPoint
    val isAligned: StateFlow<Boolean> = boxCenterManager.isAligned
    val alignmentProgress: StateFlow<Float> = boxCenterManager.alignmentProgress
    val zoomRatio: StateFlow<Float> = cameraManager.zoomRatio
    val isBackCamera: StateFlow<Boolean> = cameraManager.isBackCamera
    val motionStable: StateFlow<Boolean> = motionMonitor.isStable
    val isTorchEnabled: StateFlow<Boolean> = cameraManager.isTorchEnabled

    private var lastDetectionResult: CompositionResult? = null
    private var isPipelineActive = false
    private var isCapturing = false

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        isPipelineActive = true
        _pipelineStage.value = PipelineStage.STARTING_CAMERA
        updateGuidanceText(PipelineStage.STARTING_CAMERA)

        cameraManager.setOnFrameAnalyzed { imageProxy ->
            if (!isPipelineActive) return@setOnFrameAnalyzed
            processFrame(imageProxy)
        }

        cameraManager.startCamera(lifecycleOwner, previewView)
        motionMonitor.startMonitoring()

        // 启动状态流转监听
        observeStateTransitions()
    }

    private fun observeStateTransitions() {
        viewModelScope.launch {
            combine(
                motionMonitor.isStable,
                isDetectionReady,
                isAligned
            ) { stable, detectionReady, aligned ->
                Triple(stable, detectionReady, aligned)
            }.collect { (stable, detectionReady, aligned) ->
                val current = _pipelineStage.value
                val newStage = when (current) {
                    PipelineStage.STARTING_CAMERA -> PipelineStage.WAITING_FOR_STABILITY
                    PipelineStage.WAITING_FOR_STABILITY -> {
                        if (stable) PipelineStage.DETECTING_REGION
                        else PipelineStage.WAITING_FOR_STABILITY
                    }
                    PipelineStage.DETECTING_REGION -> {
                        if (detectionReady) PipelineStage.TEMPLATE_READY
                        else PipelineStage.DETECTING_REGION
                    }
                    PipelineStage.TEMPLATE_READY -> {
                        if (aligned) PipelineStage.READY_TO_CAPTURE
                        else PipelineStage.TEMPLATE_READY
                    }
                    PipelineStage.READY_TO_CAPTURE -> {
                        // 不在这里自动跳转，由 autoCapture 方法处理
                        PipelineStage.READY_TO_CAPTURE
                    }
                    else -> current
                }

                if (newStage != current) {
                    _pipelineStage.value = newStage
                    updateGuidanceText(newStage)
                }

                // 当进入 READY_TO_CAPTURE 时自动触发拍摄
                if (newStage == PipelineStage.READY_TO_CAPTURE && !isCapturing) {
                    val autoCaptureEnabled = settingsRepository.autoCapture.first()
                    if (autoCaptureEnabled) {
                        val delaySec = settingsRepository.captureDelay.first()
                        autoCapture(delaySec)
                    }
                }
            }
        }
    }

    private fun processFrame(imageProxy: ImageProxy) {
        // 同步拷贝帧数据到 Bitmap，避免 imageProxy.close() 后 buffer 不可访问
        val bitmap = try {
            imageProxyToBitmap(imageProxy)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert frame", e)
            cameraManager.onFrameProcessingComplete()
            return
        }

        val width = imageProxy.width
        val height = imageProxy.height

        viewModelScope.launch {
            try {
                // 传入已拷贝的 bitmap，imageProxy 已被 close 不影响
                val result = detectionEngine.analyze(bitmap)
                lastDetectionResult = result
                _isDetectionReady.value = true
                _inferenceTime.value = detectionEngine.inferenceTime.value

                val motionData = motionMonitor.motionData.value
                boxCenterManager.updateFromDetection(
                    bboxCenterX = result.bboxCenterX * width,
                    bboxCenterY = result.bboxCenterY * height,
                    motionData = motionData
                )

                if (_pipelineStage.value == PipelineStage.TEMPLATE_READY) {
                    updateGuidanceByAction(result.action)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Frame processing error", e)
            } finally {
                // 标记帧处理完成，允许下一帧进入
                cameraManager.onFrameProcessingComplete()
                bitmap.recycle()
            }
        }
    }

    /**
     * 将 ImageProxy 转换为 Bitmap，在 close 之前拷贝数据
     */
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

        return if (rowPadding > 0) {
            Bitmap.createBitmap(bitmap, 0, 0, imageProxy.width, imageProxy.height)
        } else {
            bitmap
        }
    }

    private fun updateGuidanceText(stage: PipelineStage) {
        _guidanceText.value = when (stage) {
            PipelineStage.IDLE -> "准备拍摄"
            PipelineStage.STARTING_CAMERA -> "启动相机中..."
            PipelineStage.WAITING_FOR_STABILITY -> "请保持手机稳定"
            PipelineStage.DETECTING_REGION -> "AI 分析画面中..."
            PipelineStage.TEMPLATE_READY -> "跟随指引移动手机"
            PipelineStage.READY_TO_CAPTURE -> "即将自动拍摄"
            PipelineStage.CAPTURING_PHOTO -> "拍摄中..."
            PipelineStage.SAVING_PHOTO -> "保存中..."
            PipelineStage.ERROR -> "发生错误，请重试"
        }
    }

    private fun updateGuidanceByAction(action: CompositionResult.ActionType) {
        _guidanceText.value = when (action) {
            CompositionResult.ActionType.LEFT -> "向左移动"
            CompositionResult.ActionType.RIGHT -> "向右移动"
            CompositionResult.ActionType.UP -> "向上移动"
            CompositionResult.ActionType.DOWN -> "向下移动"
            CompositionResult.ActionType.ZOOM_IN -> "靠近一些"
            CompositionResult.ActionType.ZOOM_OUT -> "远离一些"
            CompositionResult.ActionType.STOP -> "保持不动"
        }
    }

    private fun autoCapture(delaySeconds: Int) {
        if (isCapturing) return
        isCapturing = true

        viewModelScope.launch {
            try {
                // 延迟拍摄
                if (delaySeconds > 0) {
                    _guidanceText.value = "${delaySeconds} 秒后拍摄..."
                    delay(delaySeconds * 1000L)
                }

                _pipelineStage.value = PipelineStage.CAPTURING_PHOTO
                updateGuidanceText(PipelineStage.CAPTURING_PHOTO)

                cameraManager.capturePhoto(
                    onSuccess = { imageProxy ->
                        viewModelScope.launch {
                            try {
                                _pipelineStage.value = PipelineStage.SAVING_PHOTO
                                updateGuidanceText(PipelineStage.SAVING_PHOTO)

                                val cropRegion = lastDetectionResult?.let {
                                    CropRegion(it.bboxCenterX, it.bboxCenterY, it.bboxWidth, it.bboxHeight)
                                }

                                val aestheticScore = lastDetectionResult?.overallScore
                                val record = storageService.savePhoto(
                                    imageProxy = imageProxy,
                                    cropRegion = cropRegion,
                                    exifData = ExifData(),
                                    aestheticScore = aestheticScore
                                )

                                _lastSavedPhotoPath.value = record.filePath
                                resetPipeline()
                            } catch (e: Exception) {
                                Log.e(TAG, "Save failed", e)
                                _pipelineStage.value = PipelineStage.ERROR
                                updateGuidanceText(PipelineStage.ERROR)
                            } finally {
                                isCapturing = false
                            }
                        }
                    },
                    onError = { error ->
                        Log.e(TAG, "Capture failed", error)
                        _pipelineStage.value = PipelineStage.ERROR
                        updateGuidanceText(PipelineStage.ERROR)
                        isCapturing = false
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Auto capture failed", e)
                _pipelineStage.value = PipelineStage.ERROR
                updateGuidanceText(PipelineStage.ERROR)
                isCapturing = false
            }
        }
    }

    fun manualCapture() {
        if (isCapturing) return
        if (_pipelineStage.value == PipelineStage.TEMPLATE_READY ||
            _pipelineStage.value == PipelineStage.READY_TO_CAPTURE
        ) {
            viewModelScope.launch {
                autoCapture(0)
            }
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraManager.switchCamera(lifecycleOwner, previewView)
        resetPipeline()
    }

    fun setZoom(zoomRatio: Float) {
        cameraManager.setZoom(zoomRatio)
    }

    fun toggleTorch() {
        cameraManager.toggleTorch()
    }

    fun resetPipeline() {
        _pipelineStage.value = PipelineStage.WAITING_FOR_STABILITY
        _isDetectionReady.value = false
        boxCenterManager.reset()
        isCapturing = false
        updateGuidanceText(PipelineStage.WAITING_FOR_STABILITY)
    }

    fun stopCamera() {
        isPipelineActive = false
        cameraManager.stopCamera()
        motionMonitor.stopMonitoring()
        _pipelineStage.value = PipelineStage.IDLE
    }

    fun setScreenSize(width: Float, height: Float) {
        boxCenterManager.setScreenSize(width, height)
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.shutdown()
        detectionEngine.close()
    }
}
