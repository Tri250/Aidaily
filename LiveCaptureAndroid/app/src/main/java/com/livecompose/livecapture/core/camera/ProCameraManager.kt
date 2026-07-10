package com.livecompose.livecapture.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.RggbChannelVector
import android.util.Range
import android.util.Rational
import androidx.lifecycle.ViewModel
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 专业相机控制管理器 - 基于 Camera2 API
 * 手动对焦、ISO、快门速度、白平衡、曝光补偿、AE/AF 锁、RAW 捕获
 * 从 iOS ProCameraManager.swift 移植（AVFoundation -> Camera2）
 */
class ProCameraManager(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "ProCameraManager"

        /** 默认色温（K） */
        private const val DEFAULT_COLOR_TEMPERATURE = 5500

        /** 默认快门速度（1/60s，单位：纳秒） */
        private const val DEFAULT_SHUTTER_SPEED_NS = 16_666_667L

        /** 默认 ISO */
        private const val DEFAULT_ISO = 100

        /** 默认斑马纹阈值 */
        private const val DEFAULT_ZEBRA_THRESHOLD = 230

        /** 直方图采样下采样最大边长 */
        private const val HISTOGRAM_SAMPLE_SIZE = 256

        /** RggbChannelVector 增益最小值 */
        private const val MIN_GAIN = 0.5f

        /** RggbChannelVector 增益最大值 */
        private const val MAX_GAIN = 4.0f

        /** ISO 预设列表 */
        val isoPresets: List<Int> = listOf(50, 100, 200, 400, 800, 1600, 3200)

        /** 快门速度预设列表（标签, 纳秒值） */
        val shutterSpeedPresets: List<Pair<String, Long>> = listOf(
            "1/8000" to 125_000L,
            "1/4000" to 250_000L,
            "1/2000" to 500_000L,
            "1/1000" to 1_000_000L,
            "1/500" to 2_000_000L,
            "1/250" to 4_000_000L,
            "1/125" to 8_000_000L,
            "1/60" to 16_666_667L,
            "1/30" to 33_333_333L,
            "1/15" to 66_666_667L,
            "1/8" to 125_000_000L,
            "1/4" to 250_000_000L,
            "1/2" to 500_000_000L,
            "1\"" to 1_000_000_000L
        )

        /** 白平衡色温预设列表（单位：开尔文） */
        val whiteBalancePresets: List<Int> = listOf(2500, 3200, 4000, 5000, 5500, 6500, 7500, 8000)
    }

    // MARK: - StateFlow 状态

    private val _isProModeEnabled = MutableStateFlow(false)
    val isProModeEnabled: StateFlow<Boolean> = _isProModeEnabled.asStateFlow()

    // 手动对焦
    private val _manualFocusEnabled = MutableStateFlow(false)
    val manualFocusEnabled: StateFlow<Boolean> = _manualFocusEnabled.asStateFlow()

    private val _focusLensPosition = MutableStateFlow(0f)
    val focusLensPosition: StateFlow<Float> = _focusLensPosition.asStateFlow()

    private val _focusPeakingEnabled = MutableStateFlow(false)
    val focusPeakingEnabled: StateFlow<Boolean> = _focusPeakingEnabled.asStateFlow()

    // 手动曝光
    private val _manualExposureEnabled = MutableStateFlow(false)
    val manualExposureEnabled: StateFlow<Boolean> = _manualExposureEnabled.asStateFlow()

    private val _iso = MutableStateFlow(DEFAULT_ISO)
    val iso: StateFlow<Int> = _iso.asStateFlow()

    private val _shutterSpeed = MutableStateFlow(DEFAULT_SHUTTER_SPEED_NS)
    val shutterSpeed: StateFlow<Long> = _shutterSpeed.asStateFlow()

    private val _exposureBias = MutableStateFlow(0)
    val exposureBias: StateFlow<Int> = _exposureBias.asStateFlow()

    // 手动白平衡
    private val _manualWhiteBalanceEnabled = MutableStateFlow(false)
    val manualWhiteBalanceEnabled: StateFlow<Boolean> = _manualWhiteBalanceEnabled.asStateFlow()

    private val _colorTemperature = MutableStateFlow(DEFAULT_COLOR_TEMPERATURE)
    val colorTemperature: StateFlow<Int> = _colorTemperature.asStateFlow()

    // RAW 捕获
    private val _rawCaptureEnabled = MutableStateFlow(false)
    val rawCaptureEnabled: StateFlow<Boolean> = _rawCaptureEnabled.asStateFlow()

    // AE/AF 锁定
    private val _aeLocked = MutableStateFlow(false)
    val aeLocked: StateFlow<Boolean> = _aeLocked.asStateFlow()

    private val _afLocked = MutableStateFlow(false)
    val afLocked: StateFlow<Boolean> = _afLocked.asStateFlow()

    // 直方图数据
    private val _histogramData = MutableStateFlow(IntArray(256))
    val histogramData: StateFlow<IntArray> = _histogramData.asStateFlow()

    // 斑马纹
    private val _zebraEnabled = MutableStateFlow(false)
    val zebraEnabled: StateFlow<Boolean> = _zebraEnabled.asStateFlow()

    private val _zebraThreshold = MutableStateFlow(DEFAULT_ZEBRA_THRESHOLD)
    val zebraThreshold: StateFlow<Int> = _zebraThreshold.asStateFlow()

    // 当前对焦点（归一化 0..1）
    private var focusPointX: Float = 0.5f
    private var focusPointY: Float = 0.5f

    // 相机能力范围（由 applyToCaptureRequest 调用时通过 characteristics 设置）
    var isoRange: Range<Int>? = null
    var shutterSpeedRange: Range<Long>? = null

    // MARK: - 专业模式开关

    /**
     * 切换专业模式开关
     * 关闭时重置所有手动设置
     */
    fun setProModeEnabled(enabled: Boolean) {
        _isProModeEnabled.value = enabled
        if (!enabled) {
            resetAllManualSettings()
        }
    }

    // MARK: - 对焦控制

    /**
     * 设置手动对焦距离
     * @param lensPosition 0=无穷远, 1=微距
     * 实际应用到 Camera2 通过 [applyToCaptureRequest] 完成
     */
    fun setManualFocus(lensPosition: Float) {
        val clamped = lensPosition.coerceIn(0f, 1f)
        _focusLensPosition.value = clamped
        _manualFocusEnabled.value = true
        _afLocked.value = true
    }

    /**
     * 设置对焦点（归一化坐标 0..1）
     */
    fun setFocusPoint(x: Float, y: Float) {
        focusPointX = x.coerceIn(0f, 1f)
        focusPointY = y.coerceIn(0f, 1f)
    }

    // MARK: - 曝光控制

    /**
     * 设置手动曝光参数
     * @param iso ISO 值
     * @param shutterSpeedNs 快门速度（纳秒）
     */
    fun setManualExposure(iso: Int, shutterSpeedNs: Long) {
        _iso.value = iso.coerceAtLeast(1)
        _shutterSpeed.value = shutterSpeedNs.coerceAtLeast(1L)
        _manualExposureEnabled.value = true
        _aeLocked.value = true
    }

    /**
     * 设置 ISO 值（范围校验）
     * @param iso ISO 值
     */
    fun setISO(iso: Int) {
        val range = isoRange ?: return
        val clampedIso = iso.coerceIn(range.lower, range.upper)
        _iso.value = clampedIso
        _manualExposureEnabled.value = true
    }

    /**
     * 设置快门速度（范围校验）
     * @param shutterSpeedNs 快门速度（纳秒）
     */
    fun setShutterSpeed(shutterSpeedNs: Long) {
        val range = shutterSpeedRange ?: return
        val clampedShutter = shutterSpeedNs.coerceIn(range.lower, range.upper)
        _shutterSpeed.value = clampedShutter
        _manualExposureEnabled.value = true
    }

    /**
     * 设置曝光补偿（EV 单位）
     */
    fun setExposureBias(bias: Int) {
        _exposureBias.value = bias
    }

    // [v1.1.7] 自动模式方法 - 供 ManualControlPanelOverlay 调用

    /**
     * 恢复自动 ISO - 关闭手动曝光模式
     */
    fun setAutoISO() {
        _manualExposureEnabled.value = false
        _aeLocked.value = false
    }

    /**
     * 恢复自动快门速度 - 关闭手动曝光模式
     */
    fun setAutoShutterSpeed() {
        _manualExposureEnabled.value = false
        _aeLocked.value = false
    }

    /**
     * 设置曝光补偿（别名，兼容 CaptureScreen 调用）
     */
    fun setExposureCompensation(bias: Int) {
        setExposureBias(bias)
    }

    // MARK: - 白平衡控制

    /**
     * 设置手动白平衡
     * @param colorTemperature 色温（开尔文）
     */
    fun setManualWhiteBalance(colorTemperature: Int) {
        val clamped = colorTemperature.coerceIn(1500, 12000)
        _colorTemperature.value = clamped
        _manualWhiteBalanceEnabled.value = true
    }

    // [v1.1.7] 便捷方法

    /**
     * 恢复自动白平衡 - 关闭手动白平衡
     */
    fun setAutoWhiteBalance() {
        _manualWhiteBalanceEnabled.value = false
    }

    /**
     * 设置白平衡色温（别名，兼容 CaptureScreen 调用）
     * @param temperature 色温（开尔文）
     */
    fun setWhiteBalance(temperature: Int) {
        setManualWhiteBalance(temperature)
    }

    // MARK: - AE/AF 锁定

    /**
     * 切换 AE 锁
     */
    fun toggleAELock() {
        _aeLocked.value = !_aeLocked.value
    }

    /**
     * 切换 AF 锁
     */
    fun toggleAFLock() {
        _afLocked.value = !_afLocked.value
    }

    // [v1.1.7] 自动对焦方法

    /**
     * 恢复自动对焦（单次 AF-S）
     * 关闭手动对焦和 AF 锁定
     */
    fun setAutoFocus() {
        _manualFocusEnabled.value = false
        _afLocked.value = false
    }

    /**
     * 恢复连续自动对焦（AF-C）
     * 关闭手动对焦和 AF 锁定
     */
    fun setContinuousAutoFocus() {
        _manualFocusEnabled.value = false
        _afLocked.value = false
    }

    // MARK: - 斑马纹

    /**
     * 设置斑马纹开关和阈值
     * @param enabled 是否启用
     * @param threshold 亮度阈值（0-255）
     */
    fun setZebraEnabled(enabled: Boolean, threshold: Int = DEFAULT_ZEBRA_THRESHOLD) {
        _zebraEnabled.value = enabled
        _zebraThreshold.value = threshold.coerceIn(0, 255)
    }

    // MARK: - RAW 捕获

    /**
     * 设置 RAW 捕获开关
     */
    fun setRawCaptureEnabled(enabled: Boolean) {
        _rawCaptureEnabled.value = enabled
    }

    // MARK: - 重置

    /**
     * 重置所有手动设置为默认值
     */
    fun resetAllManualSettings() {
        _manualFocusEnabled.value = false
        _manualExposureEnabled.value = false
        _manualWhiteBalanceEnabled.value = false
        _aeLocked.value = false
        _afLocked.value = false
        _exposureBias.value = 0
        _focusLensPosition.value = 0f
        _iso.value = DEFAULT_ISO
        _shutterSpeed.value = DEFAULT_SHUTTER_SPEED_NS
        _colorTemperature.value = DEFAULT_COLOR_TEMPERATURE
        focusPointX = 0.5f
        focusPointY = 0.5f
    }

    // MARK: - 直方图

    /**
     * 计算 256 桶亮度直方图，更新 histogramData
     * @param bitmap 输入位图
     */
    fun computeHistogram(bitmap: Bitmap) {
        try {
            val sampleBitmap = downscaleForHistogram(bitmap)
            val width = sampleBitmap.width
            val height = sampleBitmap.height
            val pixels = IntArray(width * height)
            sampleBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val histogram = IntArray(256)
            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // 亮度公式: Y = 0.299*R + 0.587*G + 0.114*B
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt().coerceIn(0, 255)
                histogram[luminance]++
            }

            if (sampleBitmap !== bitmap) {
                sampleBitmap.recycle()
            }

            _histogramData.value = histogram
        } catch (e: Exception) {
            AppLogger.e(TAG, "计算直方图失败", e)
        }
    }

    /**
     * 下采样位图用于直方图计算，提升性能
     */
    private fun downscaleForHistogram(bitmap: Bitmap): Bitmap {
        val maxDim = max(bitmap.width, bitmap.height)
        if (maxDim <= HISTOGRAM_SAMPLE_SIZE) return bitmap
        val scale = HISTOGRAM_SAMPLE_SIZE.toFloat() / maxDim
        val newWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
    }

    // MARK: - 应用到 CaptureRequest

    /**
     * 将所有手动设置应用到 Camera2 CaptureRequest.Builder
     */
    fun applyToCaptureRequest(
        builder: CaptureRequest.Builder,
        characteristics: CameraCharacteristics
    ) {
        try {
            // 手动对焦: focusLensPosition 0=无穷远, 1=微距
            if (_manualFocusEnabled.value) {
                val minFocusDistance = characteristics.get(
                    CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
                ) ?: 0f
                if (minFocusDistance > 0f && !minFocusDistance.isNaN()) {
                    // 屈光度映射: diopter = lensPosition * minFocusDistance
                    // 0 -> 0 (无穷远), 1 -> minFocusDistance (最近对焦)
                    val diopter = _focusLensPosition.value * minFocusDistance
                    builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, diopter)
                }
            }

            // 手动曝光: 关闭 AE, 直接设置 ISO 和快门速度
            if (_manualExposureEnabled.value && isManualExposureSupported(characteristics)) {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
                val isoRange = characteristics.get(
                    CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
                )
                val exposureRange = characteristics.get(
                    CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
                )
                // 更新相机能力范围以供 setISO/setShutterSpeed 使用
                this@ProCameraManager.isoRange = isoRange
                this@ProCameraManager.shutterSpeedRange = exposureRange
                val clampedIso = isoRange?.let { _iso.value.coerceIn(it.lower, it.upper) }
                    ?: _iso.value
                val clampedShutter = exposureRange?.let {
                    _shutterSpeed.value.coerceIn(it.lower, it.upper)
                } ?: _shutterSpeed.value
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, clampedShutter)
            }

            // 曝光补偿: 将 EV 单位转换为设备步长单位
            if (_exposureBias.value != 0) {
                val step = getExposureCompensationStep(characteristics)
                val range = getExposureCompensationRange(characteristics)
                val stepDouble = step.toDouble()
                if (stepDouble > 0.0) {
                    val compensation = (_exposureBias.value.toDouble() / stepDouble).roundToInt()
                    val clampedCompensation = range?.let {
                        compensation.coerceIn(it.lower, it.upper)
                    } ?: compensation
                    builder.set(
                        CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                        clampedCompensation
                    )
                }
            }

            // 手动白平衡: 关闭 AWB, 设置通道增益
            if (_manualWhiteBalanceEnabled.value) {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
                val gains = colorTemperatureToRgbGains(_colorTemperature.value)
                builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
            }

            // AE 锁
            if (_aeLocked.value) {
                builder.set(CaptureRequest.CONTROL_AE_LOCK, true)
            }

            // AF 锁（Camera2 API 无 CONTROL_AF_LOCK，通过 AF_TRIGGER 锁定）
            if (_afLocked.value) {
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "应用 CaptureRequest 失败", e)
        }
    }

    // MARK: - 色温到 RGB 增益转换

    /**
     * 将色温（开尔文）转换为 RggbChannelVector
     * 低色温（暖光）→ R 增益增加, B 增益减少
     * 高色温（冷光）→ B 增益增加, R 增益减少
     * @param temperature 色温（K）
     * @return RggbChannelVector RGB 通道增益
     */
    fun colorTemperatureToRgbGains(temperature: Int): RggbChannelVector {
        val clamped = temperature.coerceIn(1500, 12000).toFloat()
        // 以 5500K（日光）为基准, ratio = T / 5500
        val ratio = clamped / DEFAULT_COLOR_TEMPERATURE.toFloat()
        val redGain = (1f / ratio).coerceIn(MIN_GAIN, MAX_GAIN)
        val blueGain = ratio.coerceIn(MIN_GAIN, MAX_GAIN)
        val greenGain = 1f
        return RggbChannelVector(redGain, greenGain, greenGain, blueGain)
    }

    // MARK: - 能力查询

    /**
     * 检查设备是否支持手动对焦（LENS_INFO_MINIMUM_FOCUS_DISTANCE > 0）
     */
    fun isManualFocusSupported(characteristics: CameraCharacteristics): Boolean {
        val minFocusDistance = characteristics.get(
            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
        ) ?: 0f
        return minFocusDistance > 0f && !minFocusDistance.isNaN()
    }

    /**
     * 检查设备是否支持手动曝光
     * （SENSOR_INFO_SENSITIVITY_RANGE 和 SENSOR_INFO_EXPOSURE_TIME_RANGE 均可用）
     */
    fun isManualExposureSupported(characteristics: CameraCharacteristics): Boolean {
        val isoRange = characteristics.get(
            CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
        ) ?: return false
        val exposureRange = characteristics.get(
            CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
        ) ?: return false
        return isoRange.upper > 0 && exposureRange.upper > 0L
    }

    /**
     * 获取曝光补偿范围（CONTROL_AE_COMPENSATION_RANGE）
     */
    fun getExposureCompensationRange(characteristics: CameraCharacteristics): Range<Int>? {
        return characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
    }

    /**
     * 获取曝光补偿步长（CONTROL_AE_COMPENSATION_STEP）
     */
    fun getExposureCompensationStep(characteristics: CameraCharacteristics): Rational {
        return characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            ?: Rational(1, 1)
    }

    // MARK: - 生命周期

    override fun onCleared() {
        super.onCleared()
        AppLogger.d(TAG, "ProCameraManager 已清除")
    }
}
