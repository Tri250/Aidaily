package com.livecompose.livecapture.core.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class MotionStabilityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    companion object {
        private const val TAG = "MotionStabilityMonitor"
        private const val SAMPLING_PERIOD_US = 16_667 // 60Hz
        private const val STABILITY_WINDOW = 10
        private const val INSTABILITY_THRESHOLD = 5
        private const val GYROSCOPE_THRESHOLD = 0.15f
        private const val ACCELEROMETER_THRESHOLD = 0.3f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // #16: 传感器可能为 null（低端设备无陀螺仪/加速度计）
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // #16: 传感器可用性状态
    private val _isAvailable = MutableStateFlow(gyroscope != null && accelerometer != null)
    val isAvailable: StateFlow<Boolean> = _isAvailable

    private val _isStable = MutableStateFlow(false)
    val isStable: StateFlow<Boolean> = _isStable

    private val _motionData = MutableStateFlow(MotionData())
    val motionData: StateFlow<MotionData> = _motionData

    private var gyroReadings = FloatArray(3) { 0f }
    private var accelReadings = FloatArray(3) { 0f }

    private var stableFrameCount = 0
    private var unstableFrameCount = 0
    private var isMonitoring = false

    data class MotionData(
        val gyroX: Float = 0f,
        val gyroY: Float = 0f,
        val gyroZ: Float = 0f,
        val accelX: Float = 0f,
        val accelY: Float = 0f,
        val accelZ: Float = 0f
    )

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        // #16: 传感器不可用时降级为始终稳定，不阻塞拍摄流程
        if (!_isAvailable.value) {
            Log.w(TAG, "Sensors not available, assuming stable for degraded mode")
            _isStable.value = true
            return
        }

        gyroscope?.let { sensorManager.registerListener(this, it, SAMPLING_PERIOD_US) }
        accelerometer?.let { sensorManager.registerListener(this, it, SAMPLING_PERIOD_US) }
        Log.d(TAG, "Motion monitoring started")
    }

    fun stopMonitoring() {
        isMonitoring = false
        sensorManager.unregisterListener(this)
        stableFrameCount = 0
        unstableFrameCount = 0
        _isStable.value = false
        Log.d(TAG, "Motion monitoring stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                gyroReadings[0] = event.values[0]
                gyroReadings[1] = event.values[1]
                gyroReadings[2] = event.values[2]
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelReadings[0] = event.values[0]
                accelReadings[1] = event.values[1]
                accelReadings[2] = event.values[2]
            }
        }

        _motionData.value = MotionData(
            gyroX = gyroReadings[0],
            gyroY = gyroReadings[1],
            gyroZ = gyroReadings[2],
            accelX = accelReadings[0],
            accelY = accelReadings[1],
            accelZ = accelReadings[2]
        )

        evaluateStability()
    }

    private fun evaluateStability() {
        val gyroMagnitude = sqrt(
            gyroReadings[0] * gyroReadings[0] +
            gyroReadings[1] * gyroReadings[1] +
            gyroReadings[2] * gyroReadings[2]
        )

        val accelMagnitude = sqrt(
            accelReadings[0] * accelReadings[0] +
            accelReadings[1] * accelReadings[1] +
            accelReadings[2] * accelReadings[2]
        )
        val accelDeviation = abs(accelMagnitude - 9.8f)

        val isCurrentlyStable = gyroMagnitude < GYROSCOPE_THRESHOLD &&
                accelDeviation < ACCELEROMETER_THRESHOLD

        if (isCurrentlyStable) {
            stableFrameCount++
            unstableFrameCount = 0
            if (stableFrameCount >= STABILITY_WINDOW) {
                _isStable.value = true
            }
        } else {
            unstableFrameCount++
            stableFrameCount = 0
            if (unstableFrameCount >= INSTABILITY_THRESHOLD) {
                _isStable.value = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
