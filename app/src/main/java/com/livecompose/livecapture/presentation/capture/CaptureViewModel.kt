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
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine.ModelVariant
import com.livecompose.livecapture.core.detection.CompositionResult
import com.livecompose.livecapture.core.motion.BoxCenterManager
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.settings.SettingsRepository
import com.livecompose.livecapture.core.storage.CropRegion
import com.livecompose.livecapture.core.storage.ExifData
import com.livecompose.livecapture.core.storage.PhotoStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
        private const val FAST_MODE_THROTTLE_MS = 200L  // ~5fps
        private const val PRO_MODE_THROTTLE_MS = 0L     // 每帧处理
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

    // 缩略图路径（供 UI 加载小图，避免解码全分辨率主图）
    private val _lastSavedThumbPath = MutableStateFlow<String?>(null)
    val lastSavedThumbPath: StateFlow<String?> = _lastSavedThumbPath

    val isModelReady: StateFlow<Boolean> = detectionEngine.isReady
    val isModelLoading: StateFlow<Boolean> = detectionEngine.isLoading
    val modelLoadFailed: StateFlow<Boolean> = detectionEngine.loadFailed
    val activeModelVariant: StateFlow<AdacropInferenceEngine.ModelVariant?> = detectionEngine.activeVariant

    private val _currentScore = MutableStateFlow(0f)
    val currentScore: StateFlow<Float> = _currentScore

    // 加载状态指示
    private val _isCameraStarting = MutableStateFlow(false)
    val isCameraStarting: StateFlow<Boolean> = _isCameraStarting

    // 相机错误状态
    val cameraError: StateFlow<String?> = cameraManager.errorMessage

    val trackPoint: StateFlow<PointF?> = boxCenterManager.trackPoint
    val isAligned: StateFlow<Boolean> = boxCenterManager.isAligned
    val alignmentProgress: StateFlow<Float> = boxCenterManager.alignmentProgress
    val zoomRatio: StateFlow<Float> = cameraManager.zoomRatio
    val zoomRange: StateFlow<ClosedRange<Float>> = cameraManager.zoomRange
    val isBackCamera: StateFlow<Boolean> = cameraManager.isBackCamera
    val isTorchEnabled: StateFlow<Boolean> = cameraManager.isTorchEnabled
    val hasTorchUnit: StateFlow<Boolean> = cameraManager.hasTorchUnit
    val exposureCompensation: StateFlow<Int> = cameraManager.exposureCompensation
    val exposureRange: StateFlow<IntRange> = cameraManager.exposureRange

    // @Volatile 保证跨线程可见性（processFrame 在 analysisExecutor 线程读写）
    @Volatile
    private var isPipelineActive = false
    @Volatile
    private var isCapturing = false
    @Volatile
    private var detectionMode = "FAST"
    @Volatile
    private var lastInferenceTimeMs = 0L
    @Volatile
    private var autoCaptureEnabled = true

    private var lastDetectionResult: CompositionResult? = null

    // 协程引用，用于取消
    private var stateTransitionJob: Job? = null
    private var torchSettingsJob: Job? = null
    private var modeSettingsJob: Job? = null
    private var autoCaptureJob: Job? = null

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (isPipelineActive) return
        isPipelineActive = true
        _isCameraStarting.value = true
        _pipelineStage.value = PipelineStage.STARTING_CAMERA
        updateGuidanceText(PipelineStage.STARTING_CAMERA)

        cameraManager.setOnFrameAnalyzed { imageProxy ->
            if (!isPipelineActive) return@setOnFrameAnalyzed
            processFrame(imageProxy)
        }

        cameraManager.startCamera(lifecycleOwner, previewView)
        motionMonitor.startMonitoring()

        // 异步加载 Student 模型 (Fast 模式默认), 避免主线程 ANR
        viewModelScope.launch {
            detectionEngine.loadModelAsync(ModelVariant.STUDENT)
        }

        // 实时监听闪光灯设置变更并应用
        torchSettingsJob?.cancel()
        torchSettingsJob = viewModelScope.launch {
            settingsRepository.torchEnabled.collect { enabled ->
                cameraManager.setTorchEnabled(enabled)
            }
        }

        // 实时监听检测模式变更: PRO 切换到 Teacher 模型, FAST 切换回 Student
        modeSettingsJob?.cancel()
        modeSettingsJob = viewModelScope.launch {
            settingsRepository.detectionMode.collect { mode ->
                detectionMode = mode
                val targetVariant = if (mode == "PRO") ModelVariant.TEACHER else ModelVariant.STUDENT
                // 切换模型变体 (若与当前一致则内部跳过)
                detectionEngine.switchVariant(targetVariant)
            }
        }

        // 缓存 autoCapture 设置，避免状态机每次 collect 都 first()
        viewModelScope.launch {
            settingsRepository.autoCapture.collect { enabled ->
                autoCaptureEnabled = enabled
            }
        }

        // 相机就绪后清除加载状态（等待 isCameraReady 变为 true）
        viewModelScope.launch {
            cameraManager.isCameraReady.first { it }
            _isCameraStarting.value = false
        }

        observeStateTransitions()
    }

    private fun observeStateTransitions() {
        stateTransitionJob?.cancel()
        stateTransitionJob = viewModelScope.launch {
            combine(
                motionMonitor.isStable,
                isDetectionReady,
                isAligned
            ) { stable, detectionReady, aligned ->
                Triple(stable, detectionReady, aligned)
            }.collect { (stable, detectionReady, aligned) ->
                if (!isPipelineActive) return@collect

                val current = _pipelineStage.value
                val newStage = when (current) {
                    PipelineStage.STARTING_CAMERA -> PipelineStage.WAITING_FOR_STABILITY
                    PipelineStage.WAITING_FOR_STABILITY -> {
                        if (stable) PipelineStage.DETECTING_REGION
                        else PipelineStage.WAITING_FOR_STABILITY
                    }
                    PipelineStage.DETECTING_REGION -> {
                        // 模型就绪且检测就绪时推进；模型加载失败时降级推进（使用默认构图）
                        if (detectionReady) PipelineStage.TEMPLATE_READY
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

                if (newStage == PipelineStage.READY_TO_CAPTURE && !isCapturing && autoCaptureEnabled) {
                    val delaySec = settingsRepository.captureDelay.first()
                    autoCapture(delaySec)
                }
            }
        }
    }

    private fun processFrame(imageProxy: ImageProxy) {
        // 模型未就绪时：若加载失败则用默认结果推进状态机，否则跳过等待加载
        if (!detectionEngine.isReady.value) {
            if (detectionEngine.loadFailed.value) {
                // 模型加载失败，用默认结果推进检测就绪状态
                _isDetectionReady.value = true
                _currentScore.value = 0.5f
            }
            imageProxy.close()
            cameraManager.onFrameProcessingComplete()
            return
        }

        // 推理节流 — FAST 模式限速 ~5fps，PRO 模式每帧处理
        val now = System.currentTimeMillis()
        val throttleMs = if (detectionMode == "PRO") PRO_MODE_THROTTLE_MS else FAST_MODE_THROTTLE_MS
        if (throttleMs > 0 && now - lastInferenceTimeMs < throttleMs) {
            imageProxy.close()
            cameraManager.onFrameProcessingComplete()
            return
        }
        lastInferenceTimeMs = now

        val bitmap = try {
            imageProxyToBitmap(imageProxy)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert frame", e)
            imageProxy.close()
            cameraManager.onFrameProcessingComplete()
            return
        }

        // Bitmap has been copied from the ImageProxy buffer — close it now to free the camera buffer immediately
        imageProxy.close()

        val width = bitmap.width
        val height = bitmap.height

        viewModelScope.launch {
            try {
                val result = detectionEngine.analyze(bitmap)
                lastDetectionResult = result
                _isDetectionReady.value = true
                _inferenceTime.value = detectionEngine.inferenceTime.value
                _currentScore.value = result.overallScore

                val motionData = motionMonitor.motionData.value
                boxCenterManager.updateFromDetection(
                    bboxCenterX = result.bboxCenterX * width,
                    bboxCenterY = result.bboxCenterY * height,
                    motionData = motionData
                )

                // PRO 模式持续显示动作指引；FAST 模式仅在 TEMPLATE_READY 显示
                if (detectionMode == "PRO") {
                    updateGuidanceByAction(result.action)
                } else {
                    if (_pipelineStage.value == PipelineStage.TEMPLATE_READY) {
                        updateGuidanceByAction(result.action)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Frame processing error", e)
            } finally {
                cameraManager.onFrameProcessingComplete()
                bitmap.recycle()
            }
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val plane = imageProxy.planes.firstOrNull()
            ?: throw IllegalStateException("ImageProxy has no planes")
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride.let { if (it <= 0) 4 else it }
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
            // 裁切掉 padding 区域，回收原始带 padding 的 Bitmap 避免 60fps 下内存泄漏
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, imageProxy.width, imageProxy.height)
            bitmap.recycle()
            cropped
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
            PipelineStage.READY_TO_CAPTURE -> {
                // 根据 autoCapture 设置显示不同文案
                if (autoCaptureEnabled) "即将自动拍摄" else "对齐完美，点击拍摄"
            }
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

        autoCaptureJob?.cancel()
        autoCaptureJob = viewModelScope.launch {
            try {
                if (delaySeconds > 0) {
                    _guidanceText.value = "${delaySeconds} 秒后拍摄..."
                    delay(delaySeconds * 1000L)
                }

                // delay 后重新校验状态，避免用户移开后仍拍摄
                if (!isPipelineActive || _pipelineStage.value != PipelineStage.READY_TO_CAPTURE) {
                    isCapturing = false
                    return@launch
                }

                _pipelineStage.value = PipelineStage.CAPTURING_PHOTO
                updateGuidanceText(PipelineStage.CAPTURING_PHOTO)

                cameraManager.capturePhoto(
                    onSuccess = { imageProxy ->
                        if (!isPipelineActive) {
                            // ViewModel 已 cleared，直接关闭 imageProxy
                            imageProxy.close()
                            return@onSuccess
                        }
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
                                _lastSavedThumbPath.value = record.thumbPath
                                resetPipeline()
                            } catch (e: CancellationException) {
                                throw e
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
            } catch (e: CancellationException) {
                throw e
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
            // 取消进行中的自动拍摄延迟，立即拍摄
            autoCaptureJob?.cancel()
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

    // 闪光灯切换同步到设置，保证 Settings 和 Capture 一致
    fun toggleTorch() {
        viewModelScope.launch {
            val current = settingsRepository.torchEnabled.first()
            settingsRepository.setTorchEnabled(!current)
        }
    }

    fun setExposureCompensation(value: Int) {
        cameraManager.setExposureCompensation(value)
    }

    fun focusAndMeter(x: Float, y: Float, previewWidth: Float, previewHeight: Float) {
        cameraManager.focusAndMeter(
            androidx.compose.ui.geometry.Offset(x, y),
            previewWidth,
            previewHeight
        )
    }

    // ERROR 状态重试 — 完整重置所有状态
    fun retry() {
        autoCaptureJob?.cancel()
        isCapturing = false
        isPipelineActive = false
        _isDetectionReady.value = false
        lastDetectionResult = null
        _currentScore.value = 0f
        boxCenterManager.reset()
        cameraManager.stopCamera()
        motionMonitor.stopMonitoring()
        _pipelineStage.value = PipelineStage.IDLE
        _isCameraStarting.value = false
        // 重新启动将由 UI 触发 startCamera
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
        stateTransitionJob?.cancel()
        stateTransitionJob = null
        torchSettingsJob?.cancel()
        torchSettingsJob = null
        modeSettingsJob?.cancel()
        modeSettingsJob = null
        autoCaptureJob?.cancel()
        autoCaptureJob = null
        cameraManager.stopCamera()
        motionMonitor.stopMonitoring()
        _pipelineStage.value = PipelineStage.IDLE
        _isCameraStarting.value = false
    }

    fun setScreenSize(width: Float, height: Float) {
        boxCenterManager.setScreenSize(width, height)
    }

    // Singleton 资源生命周期与 App 进程一致，不在 onCleared 中 shutdown/close
    // 仅停止相机预览和传感器监听
    override fun onCleared() {
        super.onCleared()
        isPipelineActive = false
        stateTransitionJob?.cancel()
        torchSettingsJob?.cancel()
        modeSettingsJob?.cancel()
        autoCaptureJob?.cancel()
        cameraManager.stopCamera()
        motionMonitor.stopMonitoring()
    }
}
