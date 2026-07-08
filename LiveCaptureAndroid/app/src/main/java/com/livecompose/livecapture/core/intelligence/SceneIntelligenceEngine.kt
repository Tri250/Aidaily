package com.livecompose.livecapture.core.intelligence

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 场景智能引擎
 *
 * 对应 iOS 端 SceneIntelligenceEngine.swift，协调 [SceneClassifier] 的各类分析，
 * 为相机实时提供场景识别、光环境分析和自适应拍摄参数。
 *
 * ## 发布属性
 * - [currentScene] 当前识别的场景类型
 * - [sceneConfidence] 场景识别置信度
 * - [lightAnalysis] 光环境分析结果
 * - [subjectDetection] 主体检测结果
 * - [adaptiveParams] 自适应拍摄参数
 * - [isReady] 引擎是否就绪
 *
 * ## 使用方式
 * ```
 * val engine = SceneIntelligenceEngine(context)
 * engine.analyzeFrame(bitmap) // 每 500ms 节流一次
 * engine.getSuggestedLens()   // 获取建议镜头
 * engine.getSuggestedZoomFactor() // 获取建议变焦倍数
 * ```
 *
 * @param context 上下文
 */
class SceneIntelligenceEngine(context: Context) : ViewModel() {

    companion object {
        private const val TAG = "SceneIntelligenceEngine"
        private const val ANALYSIS_THROTTLE_MS = 500L
    }

    private val appContext = context.applicationContext
    private val classifier = SceneClassifier()

    // MARK: - 发布属性

    private val _currentScene = MutableStateFlow(SceneType.UNKNOWN)
    /** 当前识别的场景类型 */
    val currentScene: StateFlow<SceneType> = _currentScene.asStateFlow()

    private val _sceneConfidence = MutableStateFlow(0f)
    /** 场景识别置信度（0.0 - 1.0） */
    val sceneConfidence: StateFlow<Float> = _sceneConfidence.asStateFlow()

    private val _lightAnalysis = MutableStateFlow<LightAnalysis?>(null)
    /** 光环境分析结果 */
    val lightAnalysis: StateFlow<LightAnalysis?> = _lightAnalysis.asStateFlow()

    private val _subjectDetection = MutableStateFlow<SubjectDetection?>(null)
    /** 主体检测结果 */
    val subjectDetection: StateFlow<SubjectDetection?> = _subjectDetection.asStateFlow()

    private val _adaptiveParams = MutableStateFlow<AdaptiveCaptureParams?>(null)
    /** 自适应拍摄参数 */
    val adaptiveParams: StateFlow<AdaptiveCaptureParams?> = _adaptiveParams.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    /** 引擎是否就绪 */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /** 上次分析时间戳（用于节流） */
    private var lastAnalysisTime = 0L

    // MARK: - 帧分析

    /**
     * 分析一帧图像
     *
     * 内部 500ms 节流，并行执行场景分类、光环境分析和主体检测，
     * 完成后计算自适应拍摄参数。
     *
     * @param bitmap 输入图像
     * @param orientation 图像方向（0, 90, 180, 270）
     */
    suspend fun analyzeFrame(bitmap: Bitmap, orientation: Int = 0) = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        if (now - lastAnalysisTime < ANALYSIS_THROTTLE_MS) {
            return@withContext
        }
        lastAnalysisTime = now

        try {
            // 并行执行三类分析
            val (sceneResult, light, subject) = coroutineScope {
                val sceneDeferred = async { classifier.classifyScene(bitmap) }
                val lightDeferred = async { classifier.analyzeLight(bitmap) }
                val subjectDeferred = async { classifier.detectSubjects(bitmap) }

                Triple(sceneDeferred.await(), lightDeferred.await(), subjectDeferred.await())
            }

            val (scene, confidence) = sceneResult

            // 更新状态
            _currentScene.value = scene
            _sceneConfidence.value = confidence
            _lightAnalysis.value = light
            _subjectDetection.value = subject

            // 计算自适应参数
            val params = computeAdaptiveParams(scene, light, subject)
            _adaptiveParams.value = params
            _isReady.value = true
        } catch (e: Exception) {
            AppLogger.e(TAG, "帧分析失败", e)
        }
    }

    // MARK: - 镜头/变焦建议

    /**
     * 获取建议镜头类型
     *
     * 根据当前场景推荐镜头焦距。
     *
     * @return 镜头类型字符串（"wide"/"standard"/"telephoto"）
     */
    fun getSuggestedLens(): String {
        return when (_currentScene.value) {
            SceneType.PORTRAIT, SceneType.PORTRAIT_STANDING, SceneType.PORTRAIT_SITTING,
            SceneType.FASHION, SceneType.BEAUTY, SceneType.WEDDING -> "telephoto"
            SceneType.LANDSCAPE, SceneType.NATURE, SceneType.ARCHITECTURE,
            SceneType.URBAN, SceneType.STREET -> "wide"
            SceneType.MACRO, SceneType.FOOD, SceneType.PRODUCT -> "standard"
            SceneType.SPORTS, SceneType.ACTION, SceneType.EVENT -> "telephoto"
            SceneType.SELFIE, SceneType.CHILDREN, SceneType.GROUP -> "wide"
            SceneType.NIGHT, SceneType.SUNSET, SceneType.TRAVEL -> "wide"
            else -> "standard"
        }
    }

    /**
     * 获取建议变焦倍数
     *
     * @return 变焦倍数（1.0 = 默认）
     */
    fun getSuggestedZoomFactor(): Float {
        return when (_currentScene.value) {
            SceneType.PORTRAIT, SceneType.PORTRAIT_STANDING, SceneType.PORTRAIT_SITTING,
            SceneType.FASHION, SceneType.BEAUTY -> 2.0f
            SceneType.FOOD, SceneType.PRODUCT, SceneType.MACRO -> 1.5f
            SceneType.SPORTS, SceneType.ACTION, SceneType.EVENT -> 2.5f
            SceneType.WEDDING -> 1.5f
            SceneType.SELFIE -> 0.5f
            else -> 1.0f
        }
    }

    // MARK: - 自适应参数计算

    /**
     * 计算自适应拍摄参数
     *
     * @param scene 场景类型
     * @param light 光环境分析
     * @param subject 主体检测
     * @return 自适应拍摄参数
     */
    fun computeAdaptiveParams(
        scene: SceneType,
        light: LightAnalysis,
        subject: SubjectDetection
    ): AdaptiveCaptureParams {
        val baseISO = computeBaseISO(light)
        val baseShutter = computeBaseShutterSpeed(light, scene)

        val adjustedISO = adjustISOForScene(baseISO, scene)
        val adjustedShutter = adjustShutterSpeedForScene(baseShutter, scene)

        val exposureBias = computeExposureBias(light)
        val wbTint = computeWhiteBalanceTint(light)
        val wbTemperature = light.colorTemperature
        val zoomFactor = getSuggestedZoomFactor()
        val lensType = getSuggestedLens()
        val flashMode = computeFlashRecommendation(light, scene)

        return AdaptiveCaptureParams(
            targetISO = adjustedISO,
            targetShutterSpeed = adjustedShutter,
            exposureBias = exposureBias,
            whiteBalanceTint = wbTint,
            whiteBalanceTemperature = wbTemperature,
            suggestedZoomFactor = zoomFactor,
            suggestedLensType = lensType,
            flashMode = flashMode
        )
    }

    /**
     * 计算基础 ISO（基于亮度）
     *
     * 暗光 → 高 ISO，亮光 → 低 ISO
     */
    private fun computeBaseISO(light: LightAnalysis): Float {
        // 亮度 0..1 映射到 ISO 50..3200
        // 亮度高 → 低 ISO；亮度低 → 高 ISO
        val brightness = light.brightness.coerceIn(0f, 1f)
        return when {
            brightness < 0.1f -> 3200f
            brightness < 0.2f -> 1600f
            brightness < 0.3f -> 800f
            brightness < 0.5f -> 400f
            brightness < 0.7f -> 200f
            else -> 100f
        }
    }

    /**
     * 计算基础快门速度（秒）
     *
     * 暗光 → 慢快门，亮光 → 快快门
     */
    private fun computeBaseShutterSpeed(light: LightAnalysis, scene: SceneType): Float {
        val brightness = light.brightness.coerceIn(0f, 1f)
        return when {
            brightness < 0.1f -> 1f / 15f   // 低光，慢快门
            brightness < 0.2f -> 1f / 30f
            brightness < 0.3f -> 1f / 60f
            brightness < 0.5f -> 1f / 125f
            brightness < 0.7f -> 1f / 250f
            brightness < 0.85f -> 1f / 500f
            else -> 1f / 1000f              // 强光，快快门
        }
    }

    /**
     * 场景特定的 ISO 调整
     */
    private fun adjustISOForScene(iso: Float, scene: SceneType): Float {
        return when (scene) {
            // 运动/动作场景需要更高快门速度，提高 ISO
            SceneType.SPORTS, SceneType.ACTION -> (iso * 2f).coerceAtMost(6400f)
            // 夜景允许更高 ISO
            SceneType.NIGHT -> (iso * 1.5f).coerceAtMost(6400f)
            // 静态场景降低 ISO 以提升画质
            SceneType.PRODUCT, SceneType.STILL_LIFE, SceneType.FOOD,
            SceneType.MACRO, SceneType.ARCHITECTURE -> (iso * 0.5f).coerceAtLeast(50f)
            else -> iso
        }
    }

    /**
     * 场景特定的快门速度调整
     */
    private fun adjustShutterSpeedForScene(shutter: Float, scene: SceneType): Float {
        return when (scene) {
            // 运动/动作场景需要更快快门
            SceneType.SPORTS, SceneType.ACTION -> (shutter / 2f).coerceAtLeast(1f / 2000f)
            // 夜景允许更慢快门
            SceneType.NIGHT -> (shutter * 2f).coerceAtMost(1f / 8f)
            // 静态场景允许更慢快门以降低 ISO
            SceneType.PRODUCT, SceneType.STILL_LIFE, SceneType.FOOD,
            SceneType.MACRO, SceneType.ARCHITECTURE -> (shutter * 2f).coerceAtMost(1f / 8f)
            else -> shutter
        }
    }

    /**
     * 计算曝光补偿
     *
     * 曝光不足 → 正向补偿，过曝 → 负向补偿
     */
    private fun computeExposureBias(light: LightAnalysis): Float {
        val brightness = light.brightness
        return when {
            brightness < 0.2f -> 1.0f    // 严重曝光不足，+1 EV
            brightness < 0.3f -> 0.5f    // 曝光不足，+0.5 EV
            brightness > 0.9f -> -1.0f   // 严重过曝，-1 EV
            brightness > 0.8f -> -0.5f   // 过曝，-0.5 EV
            else -> 0f
        }
    }

    /**
     * 计算白平衡色调偏移
     *
     * 暖光 → 偏蓝补偿（正值），冷光 → 偏暖补偿（负值）
     */
    private fun computeWhiteBalanceTint(light: LightAnalysis): Float {
        val colorTemp = light.colorTemperature
        return when {
            colorTemp < 3500f -> 15f   // 暖光，加蓝
            colorTemp < 4500f -> 5f    // 轻微暖
            colorTemp > 7000f -> -15f  // 冷光，加暖
            colorTemp > 6000f -> -5f   // 轻微冷
            else -> 0f
        }
    }

    /**
     * 计算闪光灯推荐
     */
    private fun computeFlashRecommendation(light: LightAnalysis, scene: SceneType): FlashRecommendation {
        // 逆光人像 → 补光
        if (light.isBacklit && (scene == SceneType.PORTRAIT || scene == SceneType.PORTRAIT_STANDING ||
                scene == SceneType.PORTRAIT_SITTING || scene == SceneType.SELFIE)) {
            return FlashRecommendation.ON
        }

        // 严重低光 → 开启
        if (light.brightness < 0.1f) {
            return FlashRecommendation.ON
        }

        // 静态近距离场景 → 自动
        if (scene == SceneType.FOOD || scene == SceneType.PRODUCT || scene == SceneType.MACRO) {
            return FlashRecommendation.AUTO
        }

        // 远景/风景/夜景 → 关闭
        if (scene == SceneType.LANDSCAPE || scene == SceneType.NATURE ||
            scene == SceneType.ARCHITECTURE || scene == SceneType.URBAN ||
            scene == SceneType.STREET || scene == SceneType.NIGHT ||
            scene == SceneType.SPORTS || scene == SceneType.ACTION ||
            scene == SceneType.EVENT) {
            return FlashRecommendation.OFF
        }

        // 其他场景 → 自动
        return FlashRecommendation.AUTO
    }

    // MARK: - 重置

    /**
     * 重置所有状态到默认值
     */
    fun reset() {
        _currentScene.value = SceneType.UNKNOWN
        _sceneConfidence.value = 0f
        _lightAnalysis.value = null
        _subjectDetection.value = null
        _adaptiveParams.value = null
        _isReady.value = false
        lastAnalysisTime = 0L
    }

    // MARK: - 生命周期

    override fun onCleared() {
        super.onCleared()
        try {
            classifier.close()
        } catch (e: Exception) {
            AppLogger.e(TAG, "释放分类器资源失败", e)
        }
    }
}
