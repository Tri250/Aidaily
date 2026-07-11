package com.livecompose.livecapture.core.perf

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class MemoryPressure { LOW, MEDIUM, HIGH }

data class PerformanceReport(
    val fps: Float,
    val jankCount: Long,
    val memoryUsageMb: Float,
    val memoryPressure: MemoryPressure,
    val timestamp: Long
)

@Singleton
class PerformanceMonitor @Inject constructor() {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val JANK_THRESHOLD_NS = 16_666_667L // 16ms in nanoseconds (60fps)
        private const val FPS_SAMPLE_INTERVAL_MS = 1000L
    }

    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _jankCount = MutableStateFlow(0L)
    val jankCount: StateFlow<Long> = _jankCount.asStateFlow()

    private val _isPerformant = MutableStateFlow(true)
    val isPerformant: StateFlow<Boolean> = _isPerformant.asStateFlow()

    private val _memoryUsageMb = MutableStateFlow(0f)
    val memoryUsageMb: StateFlow<Float> = _memoryUsageMb.asStateFlow()

    private val _memoryPressure = MutableStateFlow(MemoryPressure.LOW)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var choreographer: Choreographer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Frame tracking state
    private var frameStartTimeNs = 0L
    private var frameCount = 0
    private var lastFpsCalculationTimeMs = 0L
    private var totalJankCount = 0L

    // Memory tracking
    private val memoryUpdateIntervalMs = 2000L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!_isMonitoring.value) return

            if (frameStartTimeNs > 0) {
                val frameDurationNs = frameTimeNanos - frameStartTimeNs
                frameCount++

                if (frameDurationNs > JANK_THRESHOLD_NS) {
                    totalJankCount++
                    _jankCount.value = totalJankCount
                }
            }

            frameStartTimeNs = frameTimeNanos

            // Calculate FPS every second
            val currentTimeMs = System.currentTimeMillis()
            if (lastFpsCalculationTimeMs > 0L &&
                currentTimeMs - lastFpsCalculationTimeMs >= FPS_SAMPLE_INTERVAL_MS
            ) {
                val elapsedS = (currentTimeMs - lastFpsCalculationTimeMs) / 1000f
                val currentFps = frameCount / elapsedS
                _fps.value = currentFps
                _isPerformant.value = currentFps >= 55f

                frameCount = 0
                lastFpsCalculationTimeMs = currentTimeMs
            } else if (lastFpsCalculationTimeMs == 0L) {
                lastFpsCalculationTimeMs = currentTimeMs
            }

            choreographer?.postFrameCallback(this)
        }
    }

    private val memoryRunnable = object : Runnable {
        override fun run() {
            if (!_isMonitoring.value) return
            updateMemoryStats()
            mainHandler.postDelayed(this, memoryUpdateIntervalMs)
        }
    }

    fun startMonitoring() {
        if (_isMonitoring.value) return
        _isMonitoring.value = true
        frameStartTimeNs = 0L
        frameCount = 0
        lastFpsCalculationTimeMs = 0L
        totalJankCount = 0L
        _jankCount.value = 0L
        _fps.value = 0f

        // Post frame callback on main thread
        mainHandler.post {
            choreographer = Choreographer.getInstance()
            choreographer?.postFrameCallback(frameCallback)
        }

        // Start memory monitoring
        updateMemoryStats()
        mainHandler.postDelayed(memoryRunnable, memoryUpdateIntervalMs)

        Log.i(TAG, "性能监控已启动")
    }

    fun stopMonitoring() {
        if (!_isMonitoring.value) return
        _isMonitoring.value = false

        mainHandler.post {
            choreographer?.removeFrameCallback(frameCallback)
        }
        mainHandler.removeCallbacks(memoryRunnable)

        _fps.value = 0f
        _jankCount.value = 0L
        _isPerformant.value = true

        Log.i(TAG, "性能监控已停止")
    }

    private fun updateMemoryStats() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMb = usedMemory / (1024f * 1024f)

        _memoryUsageMb.value = usedMb

        val usageRatio = if (maxMemory > 0) usedMemory.toFloat() / maxMemory else 0f
        _memoryPressure.value = when {
            usageRatio < 0.6f -> MemoryPressure.LOW
            usageRatio < 0.8f -> MemoryPressure.MEDIUM
            else -> MemoryPressure.HIGH
        }
    }

    fun getPerformanceReport(): PerformanceReport {
        updateMemoryStats()
        return PerformanceReport(
            fps = _fps.value,
            jankCount = _jankCount.value,
            memoryUsageMb = _memoryUsageMb.value,
            memoryPressure = _memoryPressure.value,
            timestamp = System.currentTimeMillis()
        )
    }
}
