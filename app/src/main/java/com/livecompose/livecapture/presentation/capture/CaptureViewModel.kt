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

    // 激活: 暴露模型就绪状态给 UI
    val isModelReady: StateFlow<Boolean> = detectionEngine.isReady

    // 激活: 暴露美学评分给 UI
    private val _currentScore = MutableStateFlow(0f)
    val currentScore: StateFlow<Float> = _currentScore

    val trackPoint: StateFlow<PointF?> = boxCenterManager.trackPoint
    val isAligned: StateFlow<Boolean> = boxCenterManager.isAligned
    val alignmentProgress: StateFlow<Float> = boxCenterManager.alignmentProgress
    val zoomRatio: StateFlow<Float> = cameraManager.zoomRatio
    val isBackCamera: StateFlow<Boolean> = cameraManager.isBackCamera
    val isTorchEnabled: StateFlow<Boolean> = cameraManager.isTorchEnabled
    // 激活: 暴露曝光补偿状态给 UI
    val exposureCompensation: StateFlow<Int> = cameraManager.exposureCompensation

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

        // 激活: 读取闪光灯设置并应用默认值
        viewModelScope.launch {
            val defaultTorch = settingsRepository.torchEnabled.first()
            if (defaultTorch) {
                cameraManager.setTorchEnabled(true)
            }
        }

        // 激活: 读取检测模式设置并记录
        viewModelScope.launch {
            val mode = settingsRepository.detectionMode.first()
            Log.d(TAG, "Detection mode: $mode")
        }

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
                        // 激活: 仅当模型就绪且检测就绪时才推进
                        if (detectionReady && detectionEngine.isReady.value) PipelineStage.TEMPLATE_READY
                        else PipelineStage.DETECTING_REGION
                    }
                    PipelineStage.TEMPLATE_READY -> {
                        if (aligned) PipelineStage.READY_TO_CAPTURE
                        else PipelineStage.TEMPLATE_READY
                    }
                    PipelineStage.READY_TO_CAPTURE -> {
                        PipelineStage.READY_TO_CAPTURE
                    }
                    else -> current
                }

                if (newStage != current) {
                    _pipelineStage.value = newStage
                    updateGuidanceText(newStage)
                }

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
        // 激活: 模型未就绪时不处理帧，避免 fallback 结果错误推进状态机
        if (!detectionEngine.isReady.value) {
            cameraManager.onFrameProcessingComplete()
            return
        }

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
                val result = detectionEngine.analyze(bitmap)
                lastDetectionResult = result
                _isDetectionReady.value = true
                _inferenceTime.value = detectionEngine.inferenceTime.value
                // 激活: 更新美学评分
                _currentScore.value = result.overallScore

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
                cameraManager.onFrameProcessingComplete()
                bitmap.recycle()
            }
        }
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

    // 激活: 曝光补偿
    fun setExposureCompensation(value: Int) {
        cameraManager.setExposureCompensation(value)
    }

    // 激活: 点击对焦/测光
    fun focusAndMeter(x: Float, y: Float, previewWidth: Float, previewHeight: Float) {
        cameraManager.focusAndMeter(
            androidx.compose.ui.geometry.Offset(x, y),
            previewWidth,
            previewHeight
        )
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
