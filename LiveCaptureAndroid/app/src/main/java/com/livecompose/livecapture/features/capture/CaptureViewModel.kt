package com.livecompose.livecapture.features.capture

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.camera.ZoomPreset
import com.livecompose.livecapture.core.camera.ZoomState
import com.livecompose.livecapture.core.detection.*
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.storage.PhotoStorageService
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.utilities.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 拍摄功能 ViewModel
 */
class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val appContainer = AppContainer.getInstance(application.applicationContext)

    val camera = appContainer.cameraManager
    private val motion = appContainer.motionMonitor
    private val detector: CropDetectionStrategy
    val boxCenterManager = BoxCenterManager()
    private val storage = appContainer.photoStorageService

    // Published State
    private val _cropRectInView = MutableStateFlow<RectF?>(null)
    val cropRectInView: StateFlow<RectF?> = _cropRectInView.asStateFlow()

    private val _initialCropRectInView = MutableStateFlow<RectF?>(null)
    val initialCropRectInView: StateFlow<RectF?> = _initialCropRectInView.asStateFlow()

    private val _compositionRectInView = MutableStateFlow(RectF())
    val compositionRectInView: StateFlow<RectF> = _compositionRectInView.asStateFlow()

    private val _isAligned = MutableStateFlow(false)
    val isAligned: StateFlow<Boolean> = _isAligned.asStateFlow()

    private val _debugMessage = MutableStateFlow("等待相机启动...")
    val debugMessage: StateFlow<String> = _debugMessage.asStateFlow()

    private val _pipelineStage = MutableStateFlow(PipelineStage.IDLE)
    val pipelineStage: StateFlow<PipelineStage> = _pipelineStage.asStateFlow()

    private val _distanceToCenter = MutableStateFlow<Float?>(null)
    val distanceToCenter: StateFlow<Float?> = _distanceToCenter.asStateFlow()

    private val _detectionReady = MutableStateFlow(false)
    val detectionReady: StateFlow<Boolean> = _detectionReady.asStateFlow()

    private val _motionIsStable = MutableStateFlow(false)
    val motionIsStable: StateFlow<Boolean> = _motionIsStable.asStateFlow()

    private val _zoomState = MutableStateFlow(ZoomState())
    val zoomState: StateFlow<ZoomState> = _zoomState.asStateFlow()

    private val _zoomPresets = MutableStateFlow<List<ZoomPreset>>(emptyList())
    val zoomPresets: StateFlow<List<ZoomPreset>> = _zoomPresets.asStateFlow()

    private val _zoomRange = MutableStateFlow(1.0f..1.0f)
    val zoomRange: StateFlow<ClosedFloatingPointRange<Float>> = _zoomRange.asStateFlow()

    private val _userGuidanceText = MutableStateFlow("")
    val userGuidanceText: StateFlow<String> = _userGuidanceText.asStateFlow()

    val isAutoCaptureEnabled = MutableStateFlow(true)
    val captureDelay = MutableStateFlow(1.0)
    val isSwitchingCamera = MutableStateFlow(false)
    val isCompositionPipelineEnabled = MutableStateFlow(false)

    var onCaptureTriggered: (() -> Unit)? = null

    // Computed
    val baseBoxCenterInView: PointF? get() = boxCenterManager.baseCenterInView.value
    val boxCenterInView: PointF? get() = boxCenterManager.currentCenterInView.value
    val isFrontCamera: Boolean get() = camera.isFrontCamera

    val adjustedCropRectInView: RectF?
        get() {
            val initial = _initialCropRectInView.value ?: return null
            val baseCenter = baseBoxCenterInView ?: return null
            val currentCenter = boxCenterInView ?: return null
            val dx = currentCenter.x - baseCenter.x
            val dy = currentCenter.y - baseCenter.y
            return RectF(initial.left + dx, initial.top + dy, initial.right + dx, initial.bottom + dy)
        }

    val zoomDisplayText: String
        get() {
            val factor = _zoomState.value.displayedFactor
            return if (factor == factor.toInt().toFloat()) "${factor.toInt()}×" else "%.2f×".format(factor)
        }

    val focalLengthText: String get() = "${_zoomState.value.focalLength}mm"

    private val alignmentTolerance = 15.0f
    @Volatile private var detectionInProgress = false
    private var autoCaptureJob: kotlinx.coroutines.Job? = null
    private val detectionMode: DetectionMode

    init {
        detectionMode = DetectionMode.FAST
        detector = when (detectionMode) {
            DetectionMode.VISION -> MLKitCropDetector()
            DetectionMode.FAST, DetectionMode.PRO -> MLKitCropDetector() // TFLite fallback
        }
        boxCenterManager.setFrontCamera(false)
        bindMotion()
        bindCamera()
        refreshUserGuidance()
    }

    fun onAppear() {
        camera.shouldBeRunning = true
        if (camera.hasCameraPermission()) {
            camera.openCamera("0")
        }
        motion.start()
        setupCallbacks()
    }

    fun onDisappear() {
        autoCaptureJob?.cancel()
        motion.stop()
        camera.closeCamera()
    }

    fun registerCompositionRect(rect: RectF) {
        if (_compositionRectInView.value == rect) return
        _compositionRectInView.value = rect
        boxCenterManager.updateCompositionRect(rect)
    }

    fun capturePhoto() {
        camera.capturePhoto()
    }

    fun selectZoomPreset(preset: ZoomPreset) {
        camera.selectZoomPreset(preset)
    }

    fun updateZoomInteractively(factor: Float) {
        camera.updateInteractiveZoom(factor)
    }

    fun finalizeZoomInteractively(factor: Float) {
        camera.finalizeInteractiveZoom(factor)
    }

    fun toggleCameraPosition() {
        isSwitchingCamera.value = true
        resetDetectionState()
        camera.toggleCameraPosition()
        boxCenterManager.setFrontCamera(camera.isFrontCamera)
        if (isCompositionPipelineEnabled.value) {
            setStage(PipelineStage.WAITING_FOR_STABILITY, "切换镜头，等待稳定")
        } else {
            refreshUserGuidance()
        }
        viewModelScope.launch {
            delay(500)
            isSwitchingCamera.value = false
        }
    }

    fun resetDetectionState() {
        _detectionReady.value = false
        _isAligned.value = false
        _cropRectInView.value = null
        _initialCropRectInView.value = null
        boxCenterManager.reset()
        autoCaptureJob?.cancel()
        motion.resetReferenceAttitude()
        detectionInProgress = false
        refreshUserGuidance()
    }

    fun toggleAutoCapture() {
        isAutoCaptureEnabled.value = !isAutoCaptureEnabled.value
    }

    fun setCaptureDelay(delay: Double) {
        captureDelay.value = delay
    }

    fun toggleCompositionPipeline() {
        isCompositionPipelineEnabled.value = !isCompositionPipelineEnabled.value
        if (isCompositionPipelineEnabled.value) {
            HapticManager.success()
        } else {
            HapticManager.light()
            resetDetectionState()
        }
        refreshUserGuidance()
    }

    private fun refreshUserGuidance() {
        _userGuidanceText.value = if (isCompositionPipelineEnabled.value) {
            if (_detectionReady.value) _pipelineStage.value.guidanceText else "构图流水线已开启"
        } else {
            "点击魔术棒开启智能构图"
        }
    }

    private fun bindMotion() {
        viewModelScope.launch {
            motion.deviceMotion.collect { motionData ->
                motionData?.let { boxCenterManager.updateCenter(it) }
                _distanceToCenter.value = boxCenterManager.distanceToCenter()
                adjustedCropRectInView?.let { _cropRectInView.value = it }
                if (_detectionReady.value) checkAlignmentByDistance()
            }
        }
        viewModelScope.launch {
            motion.isStable.collect { _motionIsStable.value = it }
        }
        viewModelScope.launch {
            motion.largeMotionDetected.collect { detected ->
                if (detected && _detectionReady.value) {
                    HapticManager.warning()
                    resetDetectionState()
                }
            }
        }
    }

    private fun bindCamera() {
        viewModelScope.launch {
            camera.lastPhotoSaved.collect { saved ->
                if (saved) {
                    HapticManager.success()
                    setStage(PipelineStage.SAVING_PHOTO, "照片已保存")
                    delay(1000)
                    if (_pipelineStage.value == PipelineStage.SAVING_PHOTO) {
                        isCompositionPipelineEnabled.value = false
                        resetDetectionState()
                        refreshUserGuidance()
                    }
                }
            }
        }
        viewModelScope.launch {
            camera.zoomState.collect { state ->
                _zoomState.value = state
                boxCenterManager.updateZoomFactor(state.currentFactor)
            }
        }
        viewModelScope.launch {
            camera.zoomPresets.collect { _zoomPresets.value = it }
        }
        viewModelScope.launch {
            camera.zoomRange.collect { _zoomRange.value = it }
        }
    }

    private fun setupCallbacks() {
        camera.onSampleBuffer = { image ->
            if (!isCompositionPipelineEnabled.value) return@onSampleBuffer
            handleSampleBuffer(image)
        }
        camera.onPhotoDataReady = { data ->
            storage.savePhoto(data, detectionMode.displayName)
        }
    }

    private fun handleSampleBuffer(image: android.media.Image) {
        if (!motion.isStable.value) {
            if (!_detectionReady.value) {
                setStage(PipelineStage.WAITING_FOR_STABILITY, "等待设备稳定...")
            }
            return
        }

        if (!_detectionReady.value && !detectionInProgress) {
            setStage(PipelineStage.DETECTING_REGION, "设备已稳定，开始识别目标区域...")
            detectionInProgress = true
            detectCropRegion(image)
        }
    }

    private fun detectCropRegion(image: android.media.Image) {
        val aspectRatio = with(_compositionRectInView.value) {
            if (!isEmpty) width() / height() else 3.0f / 4.0f
        }

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        detector.detectBestCrop(bytes, image.width, image.height, 0, aspectRatio) { crop ->
            if (crop == null) {
                setStage(PipelineStage.WAITING_FOR_STABILITY, "目标识别失败，等待重试...")
                resetDetectionState()
                return@detectBestCrop
            }

            val rectInView = rectInCompositionSpace(crop.rect)
            if (rectInView != null) {
                _initialCropRectInView.value = rectInView
                _cropRectInView.value = rectInView
                val center = PointF(rectInView.centerX(), rectInView.centerY())
                boxCenterManager.setBaseCenter(center, motion.deviceMotion.value)
                motion.lockReferenceAttitude()
                _detectionReady.value = true
                HapticManager.success()
                setStage(PipelineStage.TEMPLATE_READY, "目标已锁定: ${crop.detectionType}，移动设备对齐中心圆")
                _isAligned.value = false
            } else {
                _initialCropRectInView.value = null
                _cropRectInView.value = null
                boxCenterManager.reset()
            }
            detectionInProgress = false
        }
    }

    private fun rectInCompositionSpace(normalizedRect: RectF): RectF? {
        val composition = _compositionRectInView.value
        if (composition.isEmpty) return null
        val x = composition.left + normalizedRect.left * composition.width()
        val y = composition.top + (1.0f - normalizedRect.bottom) * composition.height()
        val w = normalizedRect.width() * composition.width()
        val h = normalizedRect.height() * composition.height()
        return RectF(x, y, x + w, y + h)
    }

    private fun scheduleAutoCapture() {
        if (!isAutoCaptureEnabled.value) return
        autoCaptureJob?.cancel()
        setStage(PipelineStage.READY_TO_CAPTURE, "对准成功，准备拍照...")

        autoCaptureJob = viewModelScope.launch {
            delay((captureDelay.value * 1000).toLong())
            if (!_isAligned.value) return@launch
            setStage(PipelineStage.CAPTURING_PHOTO, "正在拍照")
            onCaptureTriggered?.invoke()
            delay(200)
            capturePhoto()
        }
    }

    private fun cancelAutoCapture() {
        autoCaptureJob?.cancel()
        autoCaptureJob = null
    }

    private fun checkAlignmentByDistance() {
        val alignedNow = boxCenterManager.isAlignedWithCenter(alignmentTolerance)
        if (alignedNow && !_isAligned.value) {
            HapticManager.focusLock()
            scheduleAutoCapture()
        } else if (!alignedNow && _isAligned.value) {
            HapticManager.warning()
            cancelAutoCapture()
            setStage(PipelineStage.TEMPLATE_READY, "请重新对准中心点")
        }
        _isAligned.value = alignedNow
    }

    private fun setStage(stage: PipelineStage, message: String? = null) {
        _pipelineStage.value = stage
        message?.let { _debugMessage.value = it }
        refreshUserGuidance()
    }

    override fun onCleared() {
        super.onCleared()
        appContainer.destroy()
    }
}

/**
 * 流水线阶段
 */
enum class PipelineStage(val progress: Float, val guidanceText: String) {
    IDLE(0.05f, ""),
    STARTING_CAMERA(0.15f, "正在启动相机"),
    WAITING_FOR_STABILITY(0.3f, "请保持稳定"),
    DETECTING_REGION(0.55f, "正在识别最佳构图..."),
    TEMPLATE_READY(0.7f, "请将圆点移动到画面中心"),
    READY_TO_CAPTURE(0.92f, "即将拍照，请保持稳定"),
    CAPTURING_PHOTO(0.95f, "正在拍照..."),
    SAVING_PHOTO(1.0f, "照片已保存"),
    ERROR(0.2f, "发生错误，请重试")
}