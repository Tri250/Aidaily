package com.livecompose.livecapture.core.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * 设备运动稳定性监控器
 */
class MotionStabilityMonitor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _isStable = MutableStateFlow(false)
    val isStable: StateFlow<Boolean> = _isStable.asStateFlow()

    private val _debugInfo = MutableStateFlow("初始化中...")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    private val _deviceMotion = MutableStateFlow<FloatArray?>(null)
    val deviceMotion: StateFlow<FloatArray?> = _deviceMotion.asStateFlow()

    private val _largeMotionDetected = MutableStateFlow(false)
    val largeMotionDetected: StateFlow<Boolean> = _largeMotionDetected.asStateFlow()

    // 可配置参数
    var windowSeconds: Double = 0.8
    var accelerationStdThreshold: Double = 0.12
    var gyroStdThreshold: Double = 0.08
    var largeMotionAccThreshold: Double = 1.5
    var largeMotionGyroThreshold: Double = 2.0

    private val accSamples = mutableListOf<Pair<Long, FloatArray>>()
    private val gyroSamples = mutableListOf<Pair<Long, FloatArray>>()
    private var consecutiveStableFrames = 0
    private var consecutiveUnstableFrames = 0
    private val requiredStableFrames = 10
    private val maxUnstableFrames = 5
    private var lastUpdateTime = 0L
    private val updateInterval = 50L // 50ms

    private var lastPitch = 0.0
    private var lastRoll = 0.0
    private var referencePitch: Double? = null
    private var referenceRoll: Double? = null
    private val maxAngle = Math.PI / 6 // 30 degrees

    private var offsetSmoother = UniformPointSmoother(response = 0.25)

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        accSamples.clear()
        gyroSamples.clear()
        consecutiveStableFrames = 0
        consecutiveUnstableFrames = 0
        referencePitch = null
        referenceRoll = null
        offsetSmoother.reset()
        _isStable.value = false
        _debugInfo.value = "已停止"
        _deviceMotion.value = null
    }

    fun lockReferenceAttitude() {
        referencePitch = lastPitch
        referenceRoll = lastRoll
        offsetSmoother.reset()
        _deviceMotion.value = null
    }

    fun resetReferenceAttitude() {
        referencePitch = null
        referenceRoll = null
        offsetSmoother.reset()
        _deviceMotion.value = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accSamples.add(Pair(System.currentTimeMillis(), event.values.clone()))
                trimSamples(accSamples)
                updateStabilityIfNeeded()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroSamples.add(Pair(System.currentTimeMillis(), event.values.clone()))
                trimSamples(gyroSamples)
                updateStabilityIfNeeded()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                lastPitch = orientation[1].toDouble() // pitch
                lastRoll = orientation[2].toDouble()   // roll
                _deviceMotion.value = orientation.clone()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun trimSamples(samples: MutableList<Pair<Long, FloatArray>>) {
        val cutoff = System.currentTimeMillis() - (windowSeconds * 1000).toLong()
        while (samples.isNotEmpty() && samples.first().first < cutoff) {
            samples.removeFirst()
        }
    }

    private fun updateStabilityIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < updateInterval) return
        lastUpdateTime = now
        updateStability()
    }

    private fun updateStability() {
        if (accSamples.isEmpty() && gyroSamples.isEmpty()) return

        val accMagnitudes = accSamples.map { (_, v) ->
            sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()
        }
        val gyroMagnitudes = gyroSamples.map { (_, v) ->
            sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()
        }

        val accStd = stdDev(accMagnitudes)
        val gyroStd = stdDev(gyroMagnitudes)
        val accMax = accMagnitudes.maxOrNull() ?: 0.0
        val gyroMax = gyroMagnitudes.maxOrNull() ?: 0.0

        val hasLargeMotion = accMax > largeMotionAccThreshold || gyroMax > largeMotionGyroThreshold
        if (hasLargeMotion) {
            _largeMotionDetected.value = true
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                _largeMotionDetected.value = false
            }, 500)
        }

        val currentFrameStable = accStd < accelerationStdThreshold && gyroStd < gyroStdThreshold
        if (currentFrameStable) {
            consecutiveUnstableFrames = 0
            consecutiveStableFrames++
        } else {
            consecutiveStableFrames = 0
            consecutiveUnstableFrames++
        }

        val overallStable = if (_isStable.value) {
            consecutiveUnstableFrames < maxUnstableFrames
        } else {
            consecutiveStableFrames >= requiredStableFrames
        }

        _isStable.value = overallStable
        _debugInfo.value = "加速度: %.3f/%.2f, 陀螺仪: %.3f/%.2f, 连续稳定: $consecutiveStableFrames".format(
            accStd, accelerationStdThreshold, gyroStd, gyroStdThreshold
        )
    }

    private fun stdDev(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.sum() / values.size
        val variance = values.sumOf { (it - mean).pow(2) } / values.size
        return sqrt(variance)
    }
}

/**
 * 统一点平滑器
 */
class UniformPointSmoother(private val response: Double) {
    private var previousX: Double? = null
    private var previousY: Double? = null

    fun filter(x: Double, y: Double): Pair<Double, Double> {
        if (previousX == null || previousY == null) {
            previousX = x
            previousY = y
            return x to y
        }
        val filteredX = previousX!! + response * (x - previousX!!)
        val filteredY = previousY!! + response * (y - previousY!!)
        previousX = filteredX
        previousY = filteredY
        return filteredX to filteredY
    }

    fun reset() {
        previousX = null
        previousY = null
    }
}