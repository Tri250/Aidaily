package com.livecompose.livecapture.features.capture

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import java.io.ByteArrayOutputStream
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.camera.ZoomPreset
import com.livecompose.livecapture.core.camera.ZoomState
import com.livecompose.livecapture.core.detection.*
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.intelligence.*
import com.livecompose.livecapture.core.intelligence.PoseGender
import com.livecompose.livecapture.core.intelligence.AgeGroup
import com.livecompose.livecapture.core.intelligence.PoseCategory
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.core.lut.PresetManager
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.shutter.HasselbladShutterSound
import com.livecompose.livecapture.core.storage.PhotoStorageService
import com.livecompose.livecapture.core.watermark.HasselbladWatermarkService
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.utilities.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 拍照结果封装
 */
sealed class PhotoCaptureResult {
    object Success : PhotoCaptureResult()
    data class Error(val message: String) : PhotoCaptureResult()
}

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

    // AI 智能引擎
    private val sceneEngine = appContainer.sceneIntelligenceEngine
    private val poseEngine = appContainer.poseRecommendationEngine
    private val filterRecommender = appContainer.aiFilterRecommender

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

    // AI 智能状态
    private val _aiSceneType = MutableStateFlow(SceneType.UNKNOWN)
    val aiSceneType: StateFlow<SceneType> = _aiSceneType.asStateFlow()

    private val _aiSceneName = MutableStateFlow("")
    val aiSceneName: StateFlow<String> = _aiSceneName.asStateFlow()

    private val _aiFilterRecommendations = MutableStateFlow<List<FilterRecommendation>>(emptyList())
    val aiFilterRecommendations: StateFlow<List<FilterRecommendation>> = _aiFilterRecommendations.asStateFlow()

    private val _aiPoseSuggestion = MutableStateFlow("")
    val aiPoseSuggestion: StateFlow<String> = _aiPoseSuggestion.asStateFlow()

    private val _aiZoomSuggestion = MutableStateFlow(1.0f)
    val aiZoomSuggestion: StateFlow<Float> = _aiZoomSuggestion.asStateFlow()

    val isAutoCaptureEnabled = MutableStateFlow(true)
    val captureDelay = MutableStateFlow(1.0)
    val isSwitchingCamera = MutableStateFlow(false)
    val isCompositionPipelineEnabled = MutableStateFlow(false)

    // Live Photo 状态
    private val _livePhotoEnabled = MutableStateFlow(false)
    val livePhotoEnabled: StateFlow<Boolean> = _livePhotoEnabled.asStateFlow()

    fun toggleLivePhoto() {
        _livePhotoEnabled.value = !_livePhotoEnabled.value
        if (_livePhotoEnabled.value) {
            HapticManager.success()
        } else {
            HapticManager.light()
        }
    }

    // 大师预设管理
    private val presetManager: PresetManager = appContainer.presetManager
    private val _allPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    val allPresets: StateFlow<List<MasterPreset>> = _allPresets.asStateFlow()

    private val _recommendedPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    val recommendedPresets: StateFlow<List<MasterPreset>> = _recommendedPresets.asStateFlow()

    private val _selectedPreset = MutableStateFlow<MasterPreset?>(null)
    val selectedPreset: StateFlow<MasterPreset?> = _selectedPreset.asStateFlow()

    private val _presetIntensity = MutableStateFlow(1.0f)
    val presetIntensity: StateFlow<Float> = _presetIntensity.asStateFlow()

    private val _presetAppliedBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val presetAppliedBitmap: StateFlow<android.graphics.Bitmap?> = _presetAppliedBitmap.asStateFlow()

    // 哈苏水印状态
    private val _hasselbladWatermarkEnabled = MutableStateFlow(false)
    val hasselbladWatermarkEnabled: StateFlow<Boolean> = _hasselbladWatermarkEnabled.asStateFlow()

    private val watermarkService: HasselbladWatermarkService = appContainer.hasselbladWatermarkService

    // 哈苏快门音状态
    private val _hasselbladShutterEnabled = MutableStateFlow(true)
    val hasselbladShutterEnabled: StateFlow<Boolean> = _hasselbladShutterEnabled.asStateFlow()

    private val shutterSound: HasselbladShutterSound = appContainer.hasselbladShutterSound

    // XPAN 65:24 宽幅模式
    private val _xpanModeEnabled = MutableStateFlow(false)
    val xpanModeEnabled: StateFlow<Boolean> = _xpanModeEnabled.asStateFlow()

    fun selectPreset(preset: MasterPreset) {
        _selectedPreset.value = preset
        presetManager.currentPreset = preset
        presetManager.presetIntensity = _presetIntensity.value
        HapticManager.light()
    }

    fun clearPreset() {
        _selectedPreset.value = null
        presetManager.clearCurrentPreset()
        _presetAppliedBitmap.value = null
        HapticManager.light()
    }

    fun setPresetIntensity(intensity: Float) {
        _presetIntensity.value = intensity
        presetManager.presetIntensity = intensity
    }

    fun toggleHasselbladWatermark() {
        _hasselbladWatermarkEnabled.value = !_hasselbladWatermarkEnabled.value
        HapticManager.light()
    }

    fun toggleHasselbladShutter() {
        _hasselbladShutterEnabled.value = !_hasselbladShutterEnabled.value
        HapticManager.light()
    }

    fun toggleXpanMode() {
        _xpanModeEnabled.value = !_xpanModeEnabled.value
        if (_xpanModeEnabled.value) {
            camera.setAspectRatio(com.livecompose.livecapture.core.camera.AspectRatio.RATIO_XPAN)
            HapticManager.success()
        } else {
            camera.setAspectRatio(com.livecompose.livecapture.core.camera.AspectRatio.RATIO_3_4)
            HapticManager.light()
        }
    }

    fun playShutterSound() {
        if (_hasselbladShutterEnabled.value) {
            shutterSound.play()
        }
    }

    suspend fun applyPresetToBitmap(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        val preset = _selectedPreset.value ?: return bitmap
        return try {
            val result = presetManager.applyPreset(preset, bitmap, _presetIntensity.value)
            _presetAppliedBitmap.value = result
            result
        } catch (e: Exception) {
            AppLogger.w("CaptureViewModel", "预设应用失败: ${e.message}")
            bitmap
        }
    }

    suspend fun applyWatermarkToBitmap(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        if (!_hasselbladWatermarkEnabled.value) return bitmap
        return try {
            val config = HasselbladWatermarkService.WatermarkConfig(
                dateTime = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date()),
                focalLength = camera.zoomState.value.focalLength.toString() + "mm"
            )
            watermarkService.applyWatermark(bitmap, config)
        } catch (e: Exception) {
            AppLogger.w("CaptureViewModel", "水印应用失败: ${e.message}")
            bitmap
        }
    }

    fun updatePresetRecommendations() {
        viewModelScope.launch {
            try {
                val sceneName = _aiSceneName.value
                if (sceneName.isNotEmpty()) {
                    val recommended = presetManager.recommendForScene(sceneName)
                    _recommendedPresets.value = recommended
                }
            } catch (e: Exception) {
                AppLogger.w("CaptureViewModel", "预设推荐更新失败: ${e.message}")
            }
        }
    }

    private val _photoCaptureResult = MutableStateFlow<PhotoCaptureResult?>(null)
    val photoCaptureResult: StateFlow<PhotoCaptureResult?> = _photoCaptureResult.asStateFlow()

    // [v1.1.7] 拍照后即时预览数据
    private val _reviewPhotoData = MutableStateFlow<ByteArray?>(null)
    val reviewPhotoData: StateFlow<ByteArray?> = _reviewPhotoData.asStateFlow()

    // [v1.1.7] 最近保存的照片ID，供删除/编辑/分享使用
    val lastSavedPhotoId: StateFlow<String?> = storage.lastSavedPhotoId

    // [v1.1.7] 重置拍照结果状态，防止 Toast 重复弹出
    fun resetPhotoCaptureResult() {
        _photoCaptureResult.value = null
    }

    // [v1.1.7] 关闭拍照预览，清除预览数据
    fun dismissPhotoReview() {
        _reviewPhotoData.value = null
    }

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
        bindAISceneEngine()
        refreshUserGuidance()
        initializePresetManager()
    }

    private fun initializePresetManager() {
        viewModelScope.launch {
            try {
                presetManager.initialize()
                _allPresets.value = presetManager.getAllMasterPresets()
                updatePresetRecommendations()
                AppLogger.i("CaptureViewModel", "大师预设加载完成: ${_allPresets.value.size} 个预设")
            } catch (e: Exception) {
                AppLogger.w("CaptureViewModel", "大师预设加载失败: ${e.message}")
            }
        }
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
        viewModelScope.launch {
            try {
                playShutterSound()
                camera.capturePhoto()
                // 成功结果通过 setupCallbacks 中的 onPhotoDataReady 回调发出
            } catch (e: Exception) {
                _photoCaptureResult.value = PhotoCaptureResult.Error(e.message ?: "拍照失败")
            }
        }
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

    // MARK: - AI 智能引擎绑定

    /**
     * 绑定场景智能引擎，持续收集 AI 分析结果
     */
    private fun bindAISceneEngine() {
        // 监听场景识别结果
        viewModelScope.launch {
            sceneEngine.currentScene.collect { scene ->
                _aiSceneType.value = scene
                _aiSceneName.value = scene.name
                updateAIInsights(scene)
            }
        }
        // 监听变焦建议
        viewModelScope.launch {
            sceneEngine.isReady.collect { ready ->
                if (ready) {
                    _aiZoomSuggestion.value = sceneEngine.getSuggestedZoomFactor()
                }
            }
        }
    }

    /**
     * 根据当前场景更新 AI 智能建议
     */
    private fun updateAIInsights(scene: SceneType) {
        // 更新滤镜推荐
        viewModelScope.launch {
            try {
                val lightAnalysis = sceneEngine.lightAnalysis.value
                val recommendations = if (lightAnalysis != null) {
                    filterRecommender.recommend(scene, lightAnalysis)
                } else {
                    filterRecommender.recommend(scene, LightAnalysis.DEFAULT)
                }
                _aiFilterRecommendations.value = recommendations
            } catch (e: Exception) {
                AppLogger.w("CaptureViewModel", "滤镜推荐失败: ${e.message}")
            }
        }
        // 更新姿势推荐（含性别/年龄适配）
        viewModelScope.launch {
            try {
                val subject = sceneEngine.subjectDetection.value ?: SubjectDetection()
                val confidence = sceneEngine.sceneConfidence.value
                // 基于检测到的性别和年龄组适配姿势推荐
                val genderHint = subject.detectedGenders.firstOrNull()
                val ageHint = subject.detectedAges.firstOrNull()
                val result = poseEngine.generateRecommendations(scene, confidence, subject)
                // 根据性别和年龄过滤建议优先级
                val adaptedSuggestions = result.suggestions.map { suggestion ->
                    if (genderHint == PoseGender.FEMALE && suggestion.category == PoseCategory.WEDDING) {
                        suggestion.copy(priority = suggestion.priority + 0.05f)
                    } else if (ageHint == AgeGroup.CHILD && suggestion.category == PoseCategory.CHILDREN) {
                        suggestion.copy(priority = suggestion.priority + 0.1f)
                    } else {
                        suggestion
                    }
                }
                val adaptedResult = result.copy(suggestions = adaptedSuggestions)
                _aiPoseSuggestion.value = adaptedResult.primaryRecommendation?.title ?: ""
            } catch (e: Exception) {
                AppLogger.w("CaptureViewModel", "姿势推荐失败: ${e.message}")
            }
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
            camera.cameraError.collect { error ->
                if (error != null) {
                    AppLogger.w("CaptureViewModel", "相机错误: $error")
                    setStage(PipelineStage.ERROR, "相机错误: $error")
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
            // AI 引擎始终分析场景（独立于构图管线）
            analyzeSceneWithAI(image)
            if (isCompositionPipelineEnabled.value) {
                handleSampleBuffer(image)
            }
        }
        camera.onPhotoDataReady = { data ->
            viewModelScope.launch {
                try {
                    // 1. 解码照片
                    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    val photoBitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

                    if (photoBitmap != null) {
                        // 2. 应用大师预设
                        var processedBitmap = applyPresetToBitmap(photoBitmap)

                        // 3. 应用哈苏水印
                        processedBitmap = applyWatermarkToBitmap(processedBitmap)

                        // 4. 编码为 JPEG
                        if (processedBitmap !== photoBitmap) {
                            val outputStream = ByteArrayOutputStream()
                            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                            val processedData = outputStream.toByteArray()
                            storage.savePhoto(processedData, detectionMode.displayName)
                            _reviewPhotoData.value = processedData
                            processedBitmap.recycle()
                        } else {
                            storage.savePhoto(data, detectionMode.displayName)
                            _reviewPhotoData.value = data
                        }
                        photoBitmap.recycle()
                    } else {
                        storage.savePhoto(data, detectionMode.displayName)
                        _reviewPhotoData.value = data
                    }
                    _photoCaptureResult.value = PhotoCaptureResult.Success
                } catch (e: Exception) {
                    AppLogger.w("CaptureViewModel", "照片后处理失败: ${e.message}")
                    storage.savePhoto(data, detectionMode.displayName)
                    _reviewPhotoData.value = data
                    _photoCaptureResult.value = PhotoCaptureResult.Success
                }
            }
        }
    }

    /**
     * AI 场景智能分析
     * 将相机帧转换为 Bitmap 并送入 SceneIntelligenceEngine 进行实时分析
     */
    private fun analyzeSceneWithAI(image: android.media.Image) {
        viewModelScope.launch {
            try {
                val bitmap = imageToBitmap(image)
                if (bitmap != null) {
                    sceneEngine.analyzeFrame(bitmap, image.format)
                }
            } catch (e: Exception) {
                // AI 分析失败不应影响拍照流程
            }
        }
    }

    /**
     * 将 android.media.Image (YUV_420_888) 转换为 Bitmap
     */
    private fun imageToBitmap(image: android.media.Image): Bitmap? {
        return try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvImage = android.graphics.YuvImage(
                nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null
            )
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 80, out)
            val jpegBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            null
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