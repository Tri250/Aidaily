package com.livecompose.livecapture.core.composition

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * 实时水平仪/地平线指示器（运动数据监控）
 *
 * 对应 iOS 端 LevelIndicator.swift（CoreMotion），使用 Android 旋转矢量传感器
 * 实时监测设备倾斜角度，并在设备达到水平时触发触觉反馈。
 *
 * ## 发布状态
 * - rollAngle: 横滚角度（度数，-180° 到 180°）
 * - pitchAngle: 俯仰角度（度数，-90° 到 90°）
 * - isLevel: 是否水平（roll 与 pitch 均在 ±1° 以内）
 * - levelDeviation: 偏离水平的度数（roll、pitch 绝对值的最大值）
 *
 * ## 工作原理
 * 1. 注册 TYPE_ROTATION_VECTOR（优先）或 TYPE_GAME_ROTATION_VECTOR
 * 2. 由旋转矢量推导旋转矩阵，再通过 getOrientation 获取 [方位角, 俯仰角, 横滚角]（弧度）
 * 3. 转为度数后对 roll、pitch 应用低通滤波（alpha=0.3）平滑数据
 * 4. 当 roll 与 pitch 均在 ±1° 以内判定为水平
 * 5. 连续 5 帧水平后才更新 isLevel 为 true（防抖）
 * 6. 首次进入水平状态时触发触觉反馈
 */
class LevelMonitor(context: Context) : SensorEventListener {

    // MARK: - 发布状态

    private val _rollAngle = MutableStateFlow(0f)
    /** 横滚角度（度数，-180° 到 180°） */
    val rollAngle: StateFlow<Float> = _rollAngle.asStateFlow()

    private val _pitchAngle = MutableStateFlow(0f)
    /** 俯仰角度（度数，-90° 到 90°） */
    val pitchAngle: StateFlow<Float> = _pitchAngle.asStateFlow()

    private val _isLevel = MutableStateFlow(false)
    /** 是否处于水平状态 */
    val isLevel: StateFlow<Boolean> = _isLevel.asStateFlow()

    private val _levelDeviation = MutableStateFlow(0f)
    /** 偏离水平的度数（roll、pitch 绝对值的最大值） */
    val levelDeviation: StateFlow<Float> = _levelDeviation.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(false)
    /** 旋转矢量传感器是否可用（不可用时 UI 应提示用户） */
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    // MARK: - 私有状态

    private val sensorManager: SensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val appContext = context.applicationContext

    /** 旋转矢量传感器（优先 TYPE_ROTATION_VECTOR，回退 TYPE_GAME_ROTATION_VECTOR） */
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    /** 水平阈值（度数） */
    private val levelThreshold = 1.0f

    /** 低通滤波系数 [0,1]，值越小越平滑但响应越慢 */
    private val smoothingFactor = 0.3f

    /** 平滑后的 roll 角度（度数） */
    private var smoothedRoll = 0f

    /** 平滑后的 pitch 角度（度数） */
    private var smoothedPitch = 0f

    /** 连续水平帧计数（用于防抖） */
    private var consecutiveLevelCount = 0

    /** 进入水平状态所需连续帧数 */
    private val requiredLevelFrames = 5

    /** 是否已触发水平触觉反馈（避免重复触发） */
    private var didTriggerLevelHaptic = false

    /** 当前已发布的水平状态（用于检测状态转换） */
    private var publishedLevel = false

    // MARK: - Public API

    /**
     * 启动水平仪监控
     */
    fun startMonitoring() {
        val sensor = rotationSensor
        _isSensorAvailable.value = sensor != null
        if (sensor == null) return
        // 重置状态
        smoothedRoll = 0f
        smoothedPitch = 0f
        consecutiveLevelCount = 0
        didTriggerLevelHaptic = false
        publishedLevel = false
        _isLevel.value = false
        _levelDeviation.value = 0f
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /**
     * 停止水平仪监控
     */
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        smoothedRoll = 0f
        smoothedPitch = 0f
        consecutiveLevelCount = 0
        didTriggerLevelHaptic = false
        publishedLevel = false
        _rollAngle.value = 0f
        _pitchAngle.value = 0f
        _isLevel.value = false
        _levelDeviation.value = 0f
    }

    // MARK: - SensorEventListener

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val sensorType = event.sensor.type
        if (sensorType != Sensor.TYPE_ROTATION_VECTOR &&
            sensorType != Sensor.TYPE_GAME_ROTATION_VECTOR
        ) {
            return
        }

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        // orientation[0] = azimuth, [1] = pitch, [2] = roll（弧度）

        // 转为度数
        val rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()
        val rawPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()

        // 低通滤波: filteredValue = alpha * newValue + (1 - alpha) * oldValue
        smoothedRoll = smoothingFactor * rawRoll + (1f - smoothingFactor) * smoothedRoll
        smoothedPitch = smoothingFactor * rawPitch + (1f - smoothingFactor) * smoothedPitch

        val absRoll = abs(smoothedRoll)
        val absPitch = abs(smoothedPitch)
        val deviation = maxOf(absRoll, absPitch)
        val currentlyLevel = absRoll < levelThreshold && absPitch < levelThreshold

        // 防抖: 连续 5 帧水平后才判定为水平
        if (currentlyLevel) {
            consecutiveLevelCount++
        } else {
            consecutiveLevelCount = 0
        }
        val shouldBeLevel = currentlyLevel && consecutiveLevelCount >= requiredLevelFrames

        // 触觉反馈: 首次进入水平状态时触发
        if (shouldBeLevel && !publishedLevel && !didTriggerLevelHaptic) {
            didTriggerLevelHaptic = true
            triggerLevelHaptic()
        }
        // 离开水平状态时重置触觉标志，允许下次再次触发
        if (!shouldBeLevel) {
            didTriggerLevelHaptic = false
        }

        // 发布状态
        publishedLevel = shouldBeLevel
        _rollAngle.value = smoothedRoll
        _pitchAngle.value = smoothedPitch
        _isLevel.value = shouldBeLevel
        _levelDeviation.value = deviation
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不处理精度变化
    }

    // MARK: - 触觉反馈

    /**
     * 触发水平触觉反馈
     */
    private fun triggerLevelHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return
            vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // 忽略振动异常，避免影响主流程
        }
    }
}
