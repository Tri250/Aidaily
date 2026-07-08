package com.livecompose.livecapture.core.portrait

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.face.Face
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 人像模式视图模型
 *
 * 对应 iOS 端 PortraitViewModel.swift，管理人像模式的状态和图像处理管线，
 * 连接 [PortraitEffectEngine] 和 [SkinBeautifier]，为 UI 层提供响应式数据绑定。
 *
 * ## 处理管线
 * 1. 检测人像（人脸检测）
 * 2. 应用美颜（Beauty）
 * 3. 应用虚化（Bokeh）
 * 4. 应用光效（Lighting）
 * 5. 生成预览
 *
 * ## 预设
 * - 自然: 全部关闭
 * - 精致: 中等美颜
 * - 女神: 高强度美颜
 * - 自定义: 手动调整
 */
class PortraitViewModel(context: Context) : ViewModel() {

    companion object {
        private const val TAG = "PortraitViewModel"
        private const val PREVIEW_MAX_DIMENSION = 800
    }

    private val appContext = context.applicationContext

    private val engine = PortraitEffectEngine()
    private val beautifier = SkinBeautifier()

    // MARK: - 美颜参数状态

    private val _skinSmoothing = MutableStateFlow(0f)
    /** 磨皮 0-1 */
    val skinSmoothing: StateFlow<Float> = _skinSmoothing.asStateFlow()

    private val _skinTone = MutableStateFlow(0f)
    /** 美白 -1（冷白）到 1（暖黄） */
    val skinTone: StateFlow<Float> = _skinTone.asStateFlow()

    private val _blemishRemoval = MutableStateFlow(0f)
    /** 祛痘 0-1 */
    val blemishRemoval: StateFlow<Float> = _blemishRemoval.asStateFlow()

    private val _eyeBrightening = MutableStateFlow(0f)
    /** 亮眼 0-1 */
    val eyeBrightening: StateFlow<Float> = _eyeBrightening.asStateFlow()

    private val _teethWhitening = MutableStateFlow(0f)
    /** 牙齿美白 0-1 */
    val teethWhitening: StateFlow<Float> = _teethWhitening.asStateFlow()

    private val _faceSlimming = MutableStateFlow(0f)
    /** 瘦脸 0-1 */
    val faceSlimming: StateFlow<Float> = _faceSlimming.asStateFlow()

    private val _portraitBlur = MutableStateFlow(0f)
    /** 人像虚化 0-1 */
    val portraitBlur: StateFlow<Float> = _portraitBlur.asStateFlow()

    // MARK: - 模式与预设状态

    private val _currentPreset = MutableStateFlow(BeautyPreset.NATURAL)
    /** 当前预设 */
    val currentPreset: StateFlow<BeautyPreset> = _currentPreset.asStateFlow()

    private val _isBeautyEnabled = MutableStateFlow(false)
    /** 美颜开关 */
    val isBeautyEnabled: StateFlow<Boolean> = _isBeautyEnabled.asStateFlow()

    private val _isPortraitModeEnabled = MutableStateFlow(false)
    /** 人像模式开关 */
    val isPortraitModeEnabled: StateFlow<Boolean> = _isPortraitModeEnabled.asStateFlow()

    private val _lightingType = MutableStateFlow(PortraitLightingType.NATURAL)
    /** 当前光效类型 */
    val lightingType: StateFlow<PortraitLightingType> = _lightingType.asStateFlow()

    private val _bokehParams = MutableStateFlow(BokehParams())
    /** 虚化参数 */
    val bokehParams: StateFlow<BokehParams> = _bokehParams.asStateFlow()

    // MARK: - 处理结果状态

    private val _processedPreview = MutableStateFlow<Bitmap?>(null)
    /** 处理后的预览图像 */
    val processedPreview: StateFlow<Bitmap?> = _processedPreview.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _faceCount = MutableStateFlow(0)
    /** 检测到的人脸数量 */
    val faceCount: StateFlow<Int> = _faceCount.asStateFlow()

    private val _hasPortrait = MutableStateFlow(false)
    /** 是否检测到人像 */
    val hasPortrait: StateFlow<Boolean> = _hasPortrait.asStateFlow()

    // MARK: - 私有状态

    private var lastProcessedImage: Bitmap? = null
    private var lastFaceObservations: List<Face> = emptyList()

    init {
        // 初始化 RenderScript（用于高斯模糊加速）
        PortraitImageUtils.initRenderScript(appContext)
    }

    // MARK: - 图像处理

    /**
     * 处理输入图像，执行完整的人像效果管线
     *
     * @param image 输入 Bitmap
     */
    fun processImage(image: Bitmap) {
        if (!_isPortraitModeEnabled.value && !_isBeautyEnabled.value) return
        if (_isProcessing.value) return

        _isProcessing.value = true
        val currentBeautyParams = buildBeautyParams()
        val currentBlur = _portraitBlur.value
        val currentLighting = _lightingType.value

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                // 1. 检测人脸
                val faces = detectFaces(image)
                val hasFace = faces.isNotEmpty()

                _faceCount.value = faces.size
                _hasPortrait.value = hasFace

                // 2. 应用效果管线
                var processed = image

                // 2a. 美颜
                if (!currentBeautyParams.isOff && hasFace) {
                    processed = beautifier.applyBeauty(processed, currentBeautyParams, faces)
                }

                // 2b. 背景虚化
                if (currentBlur > 0.01f && hasFace) {
                    val blurRadius = currentBlur * 20f
                    processed = applyDepthBlur(processed, blurRadius, faces)
                }

                // 2c. 光效
                if (currentLighting != PortraitLightingType.NATURAL && hasFace) {
                    processed = engine.applyLighting(processed, currentLighting, faces)
                }

                // 3. 生成预览（缩放到合理尺寸）
                scaleForPreview(processed)
            }

            _processedPreview.value = result
            lastProcessedImage = image
            lastFaceObservations = emptyList() // 简化：每次重新检测
            _isProcessing.value = false
        }
    }

    /**
     * 对图像应用美颜管线（供外部调用，如拍照时）
     *
     * @param image 输入图像
     * @return 美颜后的图像
     */
    suspend fun applyBeautyPipeline(image: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val faces = detectFaces(image)
        val params = buildBeautyParams()

        if (params.isOff || faces.isEmpty()) return@withContext image

        var result = beautifier.applyBeauty(image, params, faces)

        // 人像虚化
        if (_portraitBlur.value > 0.01f) {
            val blurRadius = _portraitBlur.value * 20f
            result = applyDepthBlur(result, blurRadius, faces)
        }

        result
    }

    // MARK: - 人脸检测

    /**
     * 检测人脸
     */
    private suspend fun detectFaces(image: Bitmap): List<Face> = withContext(Dispatchers.Default) {
        beautifier.detectFacesSync(image)
    }

    // MARK: - 深度虚化（模拟）

    /**
     * 使用高斯模糊模拟人像虚化效果
     */
    private fun applyDepthBlur(image: Bitmap, blurRadius: Float, faces: List<Face>): Bitmap {
        val width = image.width
        val height = image.height

        // 创建人脸区域掩码（前景清晰）
        val faceRects = faces.map { face: Face ->
            val bounds = face.boundingBox
            RectF(
                bounds.left - bounds.width() * 0.15f,
                bounds.top - bounds.height() * 0.1f,
                bounds.right + bounds.width() * 0.15f,
                bounds.bottom + bounds.height() * 0.15f
            )
        }
        val faceMask = PortraitImageUtils.createOvalMask(width, height, faceRects, blurSigma = 10f)

        // 模糊全图
        val blurred = PortraitImageUtils.gaussianBlur(image, blurRadius.coerceIn(0f, 25f))

        // 人脸区域保持清晰（背景图），其余区域模糊（前景图）
        return PortraitImageUtils.blendWithMask(blurred, image, faceMask)
    }

    // MARK: - 预设

    /**
     * 应用预设
     */
    fun applyPreset(preset: BeautyPreset) {
        _currentPreset.value = preset
        val params = preset.params()
        _skinSmoothing.value = params.skinSmoothing
        _skinTone.value = params.skinTone
        _blemishRemoval.value = params.blemishRemoval
        _eyeBrightening.value = params.eyeBrightening
        _teethWhitening.value = params.teethWhitening
        _faceSlimming.value = params.faceSlimming
        reprocessIfNeeded()
    }

    // MARK: - 参数设置方法

    fun setSkinSmoothing(value: Float) {
        _skinSmoothing.value = value
        reprocessIfNeeded()
    }

    fun setSkinTone(value: Float) {
        _skinTone.value = value
        reprocessIfNeeded()
    }

    fun setBlemishRemoval(value: Float) {
        _blemishRemoval.value = value
        reprocessIfNeeded()
    }

    fun setEyeBrightening(value: Float) {
        _eyeBrightening.value = value
        reprocessIfNeeded()
    }

    fun setTeethWhitening(value: Float) {
        _teethWhitening.value = value
        reprocessIfNeeded()
    }

    fun setFaceSlimming(value: Float) {
        _faceSlimming.value = value
        reprocessIfNeeded()
    }

    fun setPortraitBlur(value: Float) {
        _portraitBlur.value = value
        reprocessIfNeeded()
    }

    fun togglePortraitMode() {
        _isPortraitModeEnabled.value = !_isPortraitModeEnabled.value
        if (!_isPortraitModeEnabled.value) {
            _processedPreview.value = null
            lastProcessedImage = null
            _faceCount.value = 0
            _hasPortrait.value = false
        } else {
            reprocessIfNeeded()
        }
    }

    fun selectLighting(type: PortraitLightingType) {
        _lightingType.value = type
        reprocessIfNeeded()
    }

    fun updateBokehParams(params: BokehParams) {
        _bokehParams.value = params
        reprocessIfNeeded()
    }

    fun setBeautyEnabled(enabled: Boolean) {
        _isBeautyEnabled.value = enabled
        reprocessIfNeeded()
    }

    /**
     * 重置所有参数到默认值
     */
    fun reset() {
        applyPreset(BeautyPreset.NATURAL)
        _portraitBlur.value = 0f
        _bokehParams.value = BokehParams()
        _lightingType.value = PortraitLightingType.NATURAL
        _isPortraitModeEnabled.value = false
        _isBeautyEnabled.value = false
        _processedPreview.value = null
        lastProcessedImage = null
        _faceCount.value = 0
        _hasPortrait.value = false
        _isProcessing.value = false
    }

    // MARK: - 私有辅助

    /**
     * 使用上次的图像重新处理
     */
    private fun reprocessIfNeeded() {
        lastProcessedImage?.let { processImage(it) }
    }

    /**
     * 从当前属性构建 BeautyParams
     */
    private fun buildBeautyParams(): BeautyParams {
        return BeautyParams(
            skinSmoothing = _skinSmoothing.value,
            skinTone = _skinTone.value,
            eyeBrightening = _eyeBrightening.value,
            teethWhitening = _teethWhitening.value,
            faceSlimming = _faceSlimming.value,
            blemishRemoval = _blemishRemoval.value
        )
    }

    /**
     * 缩放 Bitmap 用于预览（最大边 [PREVIEW_MAX_DIMENSION]）
     */
    private fun scaleForPreview(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= PREVIEW_MAX_DIMENSION && height <= PREVIEW_MAX_DIMENSION) {
            return bitmap
        }
        val scale = if (width > height) {
            PREVIEW_MAX_DIMENSION.toFloat() / width
        } else {
            PREVIEW_MAX_DIMENSION.toFloat() / height
        }
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // MARK: - 资源清理

    override fun onCleared() {
        super.onCleared()
        try {
            beautifier.close()
            engine.close()
            PortraitImageUtils.release()
        } catch (e: Exception) {
            AppLogger.w(TAG, "PortraitViewModel 资源清理异常", e)
        }
    }
}
