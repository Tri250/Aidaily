package com.livecompose.livecapture.presentation.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import com.livecompose.livecapture.R
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine
import com.livecompose.livecapture.core.detection.AdacropInferenceEngine.ModelVariant
import com.livecompose.livecapture.core.detection.CompositionResult
import com.livecompose.livecapture.core.detection.SceneAnalyzer
import com.livecompose.livecapture.core.detection.SceneAnalysisResult
import com.livecompose.livecapture.core.motion.BoxCenterManager
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.settings.DetectionMode
import com.livecompose.livecapture.core.settings.SettingsRepository
import com.livecompose.livecapture.core.storage.CropRegion
import com.livecompose.livecapture.core.storage.ExifData
import com.livecompose.livecapture.core.storage.PhotoStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraManager: CameraManager,
    private val detectionEngine: AdacropInferenceEngine,
    private val sceneAnalyzer: SceneAnalyzer,
    private val motionMonitor: MotionStabilityMonitor,
    private val boxCenterManager: BoxCenterManager,
    private val storageService: PhotoStorageService,
    private val settingsRepository: SettingsRepository,
    private val permissionManager: PermissionManager,
    private val voiceCaptureService: com.livecompose.livecapture.core.voice.VoiceCaptureService
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
        COUNTDOWN,          // 新增：倒计时阶段
        CAPTURING_PHOTO,
        SAVING_PHOTO,
        ERROR
    }

    private val _pipelineStage = MutableStateFlow(PipelineStage.IDLE)
    val pipelineStage: StateFlow<PipelineStage> = _pipelineStage

    // 倒计时状态
    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown

    // 庆祝动画触发状态
    private val _showCelebration = MutableStateFlow(false)
    val showCelebration: StateFlow<Boolean> = _showCelebration

    // 声控拍照状态
    val voiceCaptureTriggered: StateFlow<Boolean> = voiceCaptureService.captureTriggered
    val voiceCaptureReady: StateFlow<Boolean> = voiceCaptureService.isReady
    val voiceCaptureHeardText: StateFlow<String> = voiceCaptureService.lastHeardText

    private val _guidanceText = MutableStateFlow(context.getString(R.string.guidance_idle))
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

    // 拍摄成功反馈 (用于 UI 闪白动画)
    private val _captureSuccess = MutableStateFlow(false)
    val captureSuccess: StateFlow<Boolean> = _captureSuccess

    val isModelReady: StateFlow<Boolean> = detectionEngine.isReady
    val isModelLoading: StateFlow<Boolean> = detectionEngine.isLoading
    val activeModelVariant: StateFlow<AdacropInferenceEngine.ModelVariant?> = detectionEngine.activeVariant

    private val _currentScore = MutableStateFlow(0f)
    val currentScore: StateFlow<Float> = _currentScore

    // 智能场景识别
    private val _sceneAnalysis = MutableStateFlow<SceneAnalysisResult?>(null)
    val sceneAnalysis: StateFlow<SceneAnalysisResult?> = _sceneAnalysis

    // 加载状态指示
    private val _isCameraStarting = MutableStateFlow(false)
    val isCameraStarting: StateFlow<Boolean> = _isCameraStarting

    // 模型加载失败状态 — 用于 pipeline 降级推进
    private val _modelLoadFailed = MutableStateFlow(false)
    val modelLoadFailed: StateFlow<Boolean> = _modelLoadFailed

    // 连拍状态
    private val _isBurstCapturing = MutableStateFlow(false)
    val isBurstCapturing: StateFlow<Boolean> = _isBurstCapturing

    // 相机错误状态
    val cameraError: StateFlow<String?> = cameraManager.errorMessage

    val gridEnabledFlow: StateFlow<Boolean> = settingsRepository.gridEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val voiceCaptureDefaultFlow: StateFlow<Boolean> = settingsRepository.voiceCaptureDefault
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hapticEnabledFlow: StateFlow<Boolean> = settingsRepository.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val sceneRecognitionEnabledFlow: StateFlow<Boolean> = settingsRepository.sceneRecognitionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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

    // 水平仪：从加速度计计算绕 X 轴的 roll 角度（平放时约为 0°）
    val rollAngle: StateFlow<Float> = motionMonitor.motionData.map { data ->
        kotlin.math.atan2(data.accelY, data.accelZ) * (180f / kotlin.math.PI.toFloat())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // @Volatile 保证跨线程可见性（processFrame 在 analysisExecutor 线程读写）
    @Volatile
    private var isPipelineActive = false
    @Volatile
    private var isCapturing = false
    @Volatile
    private var detectionMode = DetectionMode.FAST
    private val lastInferenceTimeMs = AtomicLong(0L)
    @Volatile
    private var autoCaptureEnabled = true
    @Volatile
    private var currentCaptureDelay = 0
    @Volatile
    private var watermarkEnabled = true
    @Volatile
    private var aspectRatio = "3:4"
    @Volatile
    private var gridEnabled = true
    @Volatile
    private var voiceCaptureDefault = false
    @Volatile
    private var hapticEnabled = true
    @Volatile
    private var sceneRecognitionEnabled = true

    private val frameCount = AtomicLong(0L)

    private var lastDetectionResult: CompositionResult? = null

    // 协程引用，用于取消
    private var stateTransitionJob: Job? = null
    private var torchSettingsJob: Job? = null
    private var modeSettingsJob: Job? = null
    private var autoCaptureJob: Job? = null
    private var burstJob: Job? = null

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (isPipelineActive) return
        isPipelineActive = true
        _captureSuccess.value = false
        _isCameraStarting.value = true
        _pipelineStage.value = PipelineStage.STARTING_CAMERA
        updateGuidanceText(PipelineStage.STARTING_CAMERA)

        cameraManager.setOnFrameAnalyzed { imageProxy ->
            if (!isPipelineActive) {
                imageProxy.close()
                cameraManager.onFrameProcessingComplete()
                return@setOnFrameAnalyzed
            }
            processFrame(imageProxy)
        }

        cameraManager.startCamera(lifecycleOwner, previewView)
        motionMonitor.startMonitoring()

        viewModelScope.launch {
            detectionEngine.loadModelAsync(ModelVariant.STUDENT)
        }

        // 监听模型加载失败状态，同步到 pipeline 降级分支
        viewModelScope.launch {
            detectionEngine.loadFailed.collect { failed ->
                _modelLoadFailed.value = failed
                if (failed) {
                    Log.w(TAG, "TFLite model load failed, pipeline will use fallback mode")
                    _guidanceText.value = context.getString(R.string.guidance_model_unavailable)
                }
            }
        }

        torchSettingsJob?.cancel()
        torchSettingsJob = viewModelScope.launch {
            settingsRepository.torchEnabled.collect { enabled ->
                cameraManager.setTorchEnabled(enabled)
            }
        }

        modeSettingsJob?.cancel()
        modeSettingsJob = viewModelScope.launch {
            settingsRepository.detectionMode.collect { mode ->
                detectionMode = mode
                val targetVariant = when (mode) {
                    DetectionMode.PRO -> ModelVariant.TEACHER
                    DetectionMode.FAST -> ModelVariant.STUDENT
                }
                detectionEngine.switchVariant(targetVariant)
            }
        }

        viewModelScope.launch {
            settingsRepository.autoCapture.collect { enabled ->
                autoCaptureEnabled = enabled
            }
        }

        viewModelScope.launch {
            settingsRepository.captureDelay.collect { delay ->
                currentCaptureDelay = delay
            }
        }

        viewModelScope.launch {
            settingsRepository.watermarkEnabled.collect { enabled ->
                watermarkEnabled = enabled
            }
        }

        viewModelScope.launch {
            settingsRepository.aspectRatio.collect { ratio ->
                aspectRatio = ratio
            }
        }

        viewModelScope.launch {
            settingsRepository.gridEnabled.collect { enabled ->
                gridEnabled = enabled
            }
        }

        viewModelScope.launch {
            settingsRepository.voiceCaptureDefault.collect { enabled ->
                voiceCaptureDefault = enabled
            }
        }

        viewModelScope.launch {
            settingsRepository.hapticEnabled.collect { enabled ->
                hapticEnabled = enabled
            }
        }

        viewModelScope.launch {
            settingsRepository.sceneRecognitionEnabled.collect { enabled ->
                sceneRecognitionEnabled = enabled
            }
        }

        viewModelScope.launch {
            cameraManager.isCameraReady.first { it }
            _isCameraStarting.value = false
        }

        // 声控拍照触发监听
        viewModelScope.launch {
            voiceCaptureService.captureTriggered.collect { triggered ->
                if (triggered && _pipelineStage.value == PipelineStage.READY_TO_CAPTURE) {
                    Log.i(TAG, "Voice trigger capture")
                    autoCapture(0) // 立即拍摄，无延迟
                    voiceCaptureService.resetTrigger()
                }
            }
        }

        observeStateTransitions()
    }

    private fun observeStateTransitions() {
        stateTransitionJob?.cancel()
        stateTransitionJob = viewModelScope.launch {
            combine(
                motionMonitor.isStable,
                isDetectionReady,
                isAligned,
                _modelLoadFailed
            ) { stable, detectionReady, aligned, modelFailed ->
                Quadruple(stable, detectionReady, aligned, modelFailed)
            }.collect { (stable, detectionReady, aligned, modelFailed) ->
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
                        // 正常路径：对齐成功 → 准备拍摄
                        // 降级路径：模型不可用 → 跳过对齐，直接进入手动拍摄
                        if (aligned || modelFailed) PipelineStage.READY_TO_CAPTURE
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

                // 仅模型正常时触发自动拍摄；模型失败时由用户手动拍摄
                if (newStage == PipelineStage.READY_TO_CAPTURE && !isCapturing && autoCaptureEnabled && !modelFailed) {
                    autoCapture(currentCaptureDelay)
                }
            }
        }
    }

    // 4-tuple helper (Kotlin stdlib 仅提供 Pair/Triple)
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun processFrame(imageProxy: ImageProxy) {
        // 模型未就绪时：若加载失败则用默认结果推进状态机，否则跳过等待加载
        if (!detectionEngine.isReady.value) {
            if (detectionEngine.loadFailed.value) {
                // 模型加载失败，仅首次设置检测就绪标志，避免每帧重复操作
                if (!_isDetectionReady.value) {
                    _isDetectionReady.value = true
                    _currentScore.value = 0.5f
                    Log.i(TAG, "Model load failed, fallback to manual capture mode")
                }
            }
            imageProxy.close()
            cameraManager.onFrameProcessingComplete()
            return
        }

        // 推理节流 — FAST 模式限速 ~5fps，PRO 模式每帧处理
        val now = System.currentTimeMillis()
        val throttleMs = when (detectionMode) {
            DetectionMode.PRO -> PRO_MODE_THROTTLE_MS
            DetectionMode.FAST -> FAST_MODE_THROTTLE_MS
        }
        if (throttleMs > 0) {
            val lastTime = lastInferenceTimeMs.get()
            if (now - lastTime < throttleMs) {
                imageProxy.close()
                cameraManager.onFrameProcessingComplete()
                return
            }
            // CAS 确保只有一个线程能更新时间，避免并发帧同时通过
            if (!lastInferenceTimeMs.compareAndSet(lastTime, now)) {
                imageProxy.close()
                cameraManager.onFrameProcessingComplete()
                return
            }
        } else {
            lastInferenceTimeMs.set(now)
        }

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
                frameCount.incrementAndGet()
                val result = detectionEngine.analyze(bitmap)
                lastDetectionResult = result
                _isDetectionReady.value = true
                _inferenceTime.value = detectionEngine.inferenceTime.value
                _currentScore.value = result.overallScore

                // 智能场景识别 (每3帧执行一次，降低 CPU 开销)
                if (frameCount.get() % 3 == 0L) {
                    val sceneResult = sceneAnalyzer.analyzeScene(bitmap)
                    _sceneAnalysis.value = sceneResult
                }

                val motionData = motionMonitor.motionData.value
                boxCenterManager.updateFromDetection(
                    bboxCenterX = result.bboxCenterX * width,
                    bboxCenterY = result.bboxCenterY * height,
                    motionData = motionData
                )

                // PRO 模式持续显示动作指引；FAST 模式仅在 TEMPLATE_READY 显示
                if (detectionMode == DetectionMode.PRO) {
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
            PipelineStage.IDLE -> context.getString(R.string.guidance_idle)
            PipelineStage.STARTING_CAMERA -> context.getString(R.string.guidance_starting)
            PipelineStage.WAITING_FOR_STABILITY -> context.getString(R.string.guidance_wait_stability)
            PipelineStage.DETECTING_REGION -> context.getString(R.string.guidance_detecting)
            PipelineStage.TEMPLATE_READY -> {
                if (_modelLoadFailed.value) context.getString(R.string.guidance_template_ready_fallback)
                else context.getString(R.string.guidance_template_ready)
            }
            PipelineStage.READY_TO_CAPTURE -> {
                if (_modelLoadFailed.value) context.getString(R.string.guidance_ready_capture_fallback)
                else if (autoCaptureEnabled) context.getString(R.string.guidance_ready_capture_auto)
                else context.getString(R.string.guidance_ready_capture_manual)
            }
            PipelineStage.COUNTDOWN -> context.getString(R.string.guidance_countdown)
            PipelineStage.CAPTURING_PHOTO -> context.getString(R.string.guidance_capturing)
            PipelineStage.SAVING_PHOTO -> context.getString(R.string.guidance_saving)
            PipelineStage.ERROR -> context.getString(R.string.guidance_error)
        }
    }

    private fun updateGuidanceByAction(action: CompositionResult.ActionType) {
        _guidanceText.value = when (action) {
            CompositionResult.ActionType.LEFT -> context.getString(R.string.guidance_move_left)
            CompositionResult.ActionType.RIGHT -> context.getString(R.string.guidance_move_right)
            CompositionResult.ActionType.UP -> context.getString(R.string.guidance_move_up)
            CompositionResult.ActionType.DOWN -> context.getString(R.string.guidance_move_down)
            CompositionResult.ActionType.ZOOM_IN -> context.getString(R.string.guidance_zoom_in)
            CompositionResult.ActionType.ZOOM_OUT -> context.getString(R.string.guidance_zoom_out)
            CompositionResult.ActionType.STOP -> context.getString(R.string.guidance_hold)
        }
    }

    private fun autoCapture(delaySeconds: Int) {
        if (isCapturing) return
        isCapturing = true

        autoCaptureJob?.cancel()
        autoCaptureJob = viewModelScope.launch {
            try {
                if (delaySeconds > 0) {
                    // 倒计时阶段
                    _pipelineStage.value = PipelineStage.COUNTDOWN
                    updateGuidanceText(PipelineStage.COUNTDOWN)

                    for (i in delaySeconds downTo 1) {
                        _countdown.value = i
                        _guidanceText.value = context.getString(R.string.guidance_countdown_format, i)
                        delay(1000)
                    }

                    _countdown.value = 0
                }

                // delay 后重新校验状态，避免用户移开后仍拍摄
                // 模型失败降级时，同时接受 TEMPLATE_READY 和 READY_TO_CAPTURE
                val validStage = _pipelineStage.value == PipelineStage.READY_TO_CAPTURE ||
                        (_modelLoadFailed.value && _pipelineStage.value == PipelineStage.TEMPLATE_READY)
                if (!isPipelineActive || !validStage) {
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
                            return@capturePhoto
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
                                    aestheticScore = aestheticScore,
                                    watermarkEnabled = watermarkEnabled,
                                    aspectRatio = aspectRatio
                                )

                                _lastSavedPhotoPath.value = record.filePath
                                _lastSavedThumbPath.value = record.thumbPath
                                
                                // 触发拍摄成功反馈
                                _captureSuccess.value = true
                                
                                // 触发庆祝动画
                                _showCelebration.value = true
                                launch {
                                    delay(2000) // 2秒后关闭庆祝动画
                                    _showCelebration.value = false
                                }
                                
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

    fun burstCapture() {
        if (burstJob?.isActive == true) return
        _isBurstCapturing.value = true
        burstJob = viewModelScope.launch {
            try {
                var count = 0
                while (count < 10 && isActive && isPipelineActive) {
                    if (!isCapturing) {
                        manualCapture()
                        count++
                    }
                    delay(500)
                }
            } finally {
                _isBurstCapturing.value = false
            }
        }
    }

    fun stopBurstCapture() {
        burstJob?.cancel()
        burstJob = null
        _isBurstCapturing.value = false
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

    // 声控拍照控制
    fun startVoiceCapture() {
        voiceCaptureService.startListening()
    }

    fun stopVoiceCapture() {
        voiceCaptureService.stopListening()
    }

    // ERROR 状态重试 — 完整重置所有状态
    fun retry() {
        autoCaptureJob?.cancel()
        burstJob?.cancel()
        burstJob = null
        _isBurstCapturing.value = false
        isCapturing = false
        isPipelineActive = false
        _isDetectionReady.value = false
        lastDetectionResult = null
        _currentScore.value = 0f
        _captureSuccess.value = false
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
        _captureSuccess.value = false
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
        burstJob?.cancel()
        burstJob = null
        _isBurstCapturing.value = false
        cameraManager.stopCamera()
        motionMonitor.stopMonitoring()
        _pipelineStage.value = PipelineStage.IDLE
        _isCameraStarting.value = false
    }

    fun setScreenSize(width: Float, height: Float) {
        boxCenterManager.setScreenSize(width, height)
    }

    fun hasCameraPermission(): Boolean = permissionManager.hasCameraPermission()

    fun shouldShowCameraRationale(activity: android.app.Activity): Boolean =
        permissionManager.shouldShowRationale(activity, android.Manifest.permission.CAMERA)

    fun openAppSettings() = permissionManager.openAppSettings()

    // Singleton 资源生命周期与 App 进程一致，不在 onCleared 中 shutdown/close
    // 仅停止相机预览和传感器监听
    override fun onCleared() {
        super.onCleared()
        isPipelineActive = false
        stateTransitionJob?.cancel()
        torchSettingsJob?.cancel()
        modeSettingsJob?.cancel()
        autoCaptureJob?.cancel()
        burstJob?.cancel()
        cameraManager.stopCamera()
        motionMonitor.stopMonitoring()
    }
}
