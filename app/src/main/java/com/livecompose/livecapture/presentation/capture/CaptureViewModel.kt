package com.livecompose.livecapture.presentation.capture

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
import com.livecompose.livecapture.core.storage.CropRegion
import com.livecompose.livecapture.core.storage.ExifData
import com.livecompose.livecapture.core.storage.PhotoStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val detectionEngine: AdacropInferenceEngine,
    private val motionMonitor: MotionStabilityMonitor,
    private val boxCenterManager: BoxCenterManager,
    private val storageService: PhotoStorageService
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

    private val _guidanceText = MutableStateFlow("")
    val guidanceText: StateFlow<String> = _guidanceText

    private val _isDetectionReady = MutableStateFlow(false)
    val isDetectionReady: StateFlow<Boolean> = _isDetectionReady

    val trackPoint: StateFlow<PointF?> = boxCenterManager.trackPoint
    val isAligned: StateFlow<Boolean> = boxCenterManager.isAligned
    val alignmentProgress: StateFlow<Float> = boxCenterManager.alignmentProgress
    val zoomRatio: StateFlow<Float> = cameraManager.zoomRatio
    val isBackCamera: StateFlow<Boolean> = cameraManager.isBackCamera
    val motionStable: StateFlow<Boolean> = motionMonitor.isStable

    private var lastDetectionResult: CompositionResult? = null
    private var isPipelineActive = false

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        isPipelineActive = true
        _pipelineStage.value = PipelineStage.STARTING_CAMERA

        cameraManager.setOnFrameAnalyzed { imageProxy ->
            if (!isPipelineActive) return@setOnFrameAnalyzed
            processFrame(imageProxy)
        }

        cameraManager.startCamera(lifecycleOwner, previewView)
        motionMonitor.startMonitoring()

        viewModelScope.launch {
            observeStateTransitions()
        }
    }

    private fun observeStateTransitions() {
        viewModelScope.launch {
            combine(
                motionMonitor.isStable,
                isDetectionReady,
                isAligned
            ) { stable, detectionReady, aligned ->
                when (_pipelineStage.value) {
                    PipelineStage.STARTING_CAMERA -> {
                        _pipelineStage.value = PipelineStage.WAITING_FOR_STABILITY
                    }
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
                        autoCapture()
                        PipelineStage.IDLE
                    }
                    else -> _pipelineStage.value
                }
            }.collect { newStage ->
                if (newStage != _pipelineStage.value) {
                    _pipelineStage.value = newStage
                    updateGuidanceText(newStage)
                }
            }
        }
    }

    private fun processFrame(imageProxy: ImageProxy) {
        viewModelScope.launch {
            try {
                val result = detectionEngine.analyze(imageProxy)
                lastDetectionResult = result
                _isDetectionReady.value = true

                // 更新 BoxCenterManager
                val motionData = motionMonitor.motionData.value
                boxCenterManager.updateFromDetection(
                    bboxCenterX = result.bboxCenterX * imageProxy.width,
                    bboxCenterY = result.bboxCenterY * imageProxy.height,
                    motionData = motionData
                )

                // 更新引导文字（根据动作）
                if (_pipelineStage.value == PipelineStage.TEMPLATE_READY) {
                    updateGuidanceByAction(result.action)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Frame processing error", e)
            }
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

    private fun autoCapture() {
        viewModelScope.launch {
            _pipelineStage.value = PipelineStage.CAPTURING_PHOTO
            _guidanceText.value = "拍摄中..."

            cameraManager.capturePhoto(
                onSuccess = { imageProxy ->
                    viewModelScope.launch {
                        _pipelineStage.value = PipelineStage.SAVING_PHOTO
                        _guidanceText.value = "保存中..."

                        val cropRegion = lastDetectionResult?.let {
                            CropRegion(it.bboxCenterX, it.bboxCenterY, it.bboxWidth, it.bboxHeight)
                        }

                        storageService.savePhoto(
                            imageProxy = imageProxy,
                            cropRegion = cropRegion,
                            exifData = ExifData()
                        )

                        // 重置流水线
                        resetPipeline()
                    }
                },
                onError = { error ->
                    Log.e(TAG, "Capture failed", error)
                    _pipelineStage.value = PipelineStage.ERROR
                    _guidanceText.value = "拍摄失败，请重试"
                }
            )
        }
    }

    fun manualCapture() {
        if (_pipelineStage.value == PipelineStage.TEMPLATE_READY ||
            _pipelineStage.value == PipelineStage.READY_TO_CAPTURE
        ) {
            autoCapture()
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraManager.switchCamera(lifecycleOwner, previewView)
        resetPipeline()
    }

    fun setZoom(zoomRatio: Float) {
        cameraManager.setZoom(zoomRatio)
    }

    fun resetPipeline() {
        _pipelineStage.value = PipelineStage.WAITING_FOR_STABILITY
        _isDetectionReady.value = false
        boxCenterManager.reset()
        updateGuidanceText(PipelineStage.WAITING_FOR_STABILITY)
    }

    fun stopCamera() {
        isPipelineActive = false
        cameraManager.stopCamera()
        motionMonitor.stopMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.shutdown()
        detectionEngine.close()
    }
}
