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

    // #69: 传感器回调在独立线程执行，读写需同步保护
    private val sensorLock = Any()
    private var gyroReadings = FloatArray(3) { 0f }
    private var accelReadings = FloatArray(3) { 0f }

    private var stableFrameCount = 0
    private var unstableFrameCount = 0
    @Volatile
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
        // #69: 传感器回调在独立线程执行，同步保护读写
        val gx: Float; val gy: Float; val gz: Float
        val ax: Float; val ay: Float; val az: Float
        synchronized(sensorLock) {
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
            gx = gyroReadings[0]; gy = gyroReadings[1]; gz = gyroReadings[2]
            ax = accelReadings[0]; ay = accelReadings[1]; az = accelReadings[2]
        }

        _motionData.value = MotionData(
            gyroX = gx, gyroY = gy, gyroZ = gz,
            accelX = ax, accelY = ay, accelZ = az
        )

        evaluateStability()
    }

    private fun evaluateStability() {
        val values = synchronized(sensorLock) {
            floatArrayOf(
                gyroReadings[0], gyroReadings[1], gyroReadings[2],
                accelReadings[0], accelReadings[1], accelReadings[2]
            )
        }
        val gx = values[0]; val gy = values[1]; val gz = values[2]
        val ax = values[3]; val ay = values[4]; val az = values[5]

        val gyroMagnitude = sqrt(gx * gx + gy * gy + gz * gz)
        val accelMagnitude = sqrt(ax * ax + ay * ay + az * az)
        val accelDeviation = abs(accelMagnitude - 9.8f)

        val isCurrentlyStable = gyroMagnitude < GYROSCOPE_THRESHOLD &&
                accelDeviation < ACCELEROMETER_THRESHOLD

        synchronized(sensorLock) {
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
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
