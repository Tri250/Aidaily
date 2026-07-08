package com.livecompose.livecapture.core.performance

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Debug
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 内存告警等级
 *
 * 对应 iOS MemoryWarningLevel。
 */
enum class MemoryWarningLevel(val displayName: String) {
    NORMAL("正常"),
    WARNING("警告"),
    CRITICAL("严重");

    /** 用于 UI 显示的颜色角色名（映射到 DesignSystem.Colors） */
    val colorRole: String
        get() = when (this) {
            NORMAL -> "success"
            WARNING -> "warning"
            CRITICAL -> "error"
        }
}

/**
 * 内存使用统计摘要
 */
data class MemoryStats(
    val currentMB: Double,
    val averageMB: Double,
    val minMB: Double,
    val maxMB: Double,
    val peakMB: Double,
    val totalDeviceMB: Double,
    val availableMB: Double,
    val warningLevel: MemoryWarningLevel
)

/**
 * 内存监控器
 *
 * 对应 iOS 端 MemoryMonitor.swift，追踪应用内存使用、阈值告警，并在内存压力时触发清理回调。
 *
 * ## 技术映射
 * - iOS mach_task_basic_info（resident_size）→ Android [Debug.MemoryInfo.getTotalPrivateDirty]
 * - iOS ProcessInfo.physicalMemory → Android [ActivityManager.MemoryInfo]
 * - iOS UIApplication.didReceiveMemoryWarningNotification → Android [ComponentCallbacks2.onTrimMemory]
 * - iOS Timer → Android Kotlin 协程 [delay]
 *
 * ## 主要功能
 * - [startMonitoring] / [stopMonitoring] 启停周期性采样
 * - [sampleMemory] 执行一次内存采样
 * - [registerCleanupCallback] 注册内存压力时的清理回调（替代 iOS CIContext.clearCaches）
 * - [memoryStats] 获取统计摘要
 *
 * @param context 应用上下文
 */
class MemoryMonitor(private val context: Context) {

    companion object {
        private const val TAG = "MemoryMonitor"
        private const val MAX_HISTORY_COUNT = 60
    }

    // MARK: - 配置

    /** 内存警告阈值（MB），默认 200MB */
    var warningThresholdMB: Double = 200.0

    /** 高内存压力阈值（MB），默认 300MB */
    var criticalThresholdMB: Double = 300.0

    /** 采样间隔（毫秒），默认 2000ms */
    var samplingIntervalMs: Long = 2000L

    // MARK: - 发布属性

    private val _currentMemoryMB = MutableStateFlow(0.0)
    /** 当前内存使用量（MB） */
    val currentMemoryMB: StateFlow<Double> = _currentMemoryMB.asStateFlow()

    private val _peakMemoryMB = MutableStateFlow(0.0)
    /** 内存使用峰值（MB） */
    val peakMemoryMB: StateFlow<Double> = _peakMemoryMB.asStateFlow()

    private val _isUnderMemoryPressure = MutableStateFlow(false)
    /** 是否处于内存压力状态 */
    val isUnderMemoryPressure: StateFlow<Boolean> = _isUnderMemoryPressure.asStateFlow()

    private val _warningLevel = MutableStateFlow(MemoryWarningLevel.NORMAL)
    /** 内存告警等级 */
    val warningLevel: StateFlow<MemoryWarningLevel> = _warningLevel.asStateFlow()

    private val _memoryHistory = MutableStateFlow<List<Double>>(emptyList())
    /** 内存使用历史（最近 60 个采样点） */
    val memoryHistory: StateFlow<List<Double>> = _memoryHistory.asStateFlow()

    // MARK: - 私有属性

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var samplingJob: Job? = null
    private val cleanupCallbacks = mutableListOf<() -> Unit>()
    private var memoryCallbackRegistered = false

    /** 系统内存压力回调（替代 iOS UIApplication.didReceiveMemoryWarningNotification） */
    private val componentCallbacks = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) {}

        override fun onLowMemory() {
            handleSystemMemoryWarning()
        }

        override fun onTrimMemory(level: Int) {
            // TRIM_MEMORY_RUNNING_CRITICAL = 15, TRIM_MEMORY_COMPLETE = 80
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                handleSystemMemoryWarning()
            }
        }
    }

    // MARK: - 监控控制

    /**
     * 开始内存监控
     *
     * 立即采样一次，然后按 [samplingIntervalMs] 周期性采样。
     * 同时注册系统内存压力回调。
     */
    fun startMonitoring() {
        if (samplingJob != null) return

        // 注册系统内存压力回调
        if (!memoryCallbackRegistered) {
            context.registerComponentCallbacks(componentCallbacks)
            memoryCallbackRegistered = true
        }

        // 立即采样一次
        sampleMemory()

        // 启动周期性采样
        samplingJob = scope.launch {
            while (isActive) {
                delay(samplingIntervalMs)
                sampleMemory()
            }
        }

        AppLogger.d(TAG, "内存监控已启动（阈值: ${warningThresholdMB.toInt()}MB / ${criticalThresholdMB.toInt()}MB）")
    }

    /**
     * 停止内存监控
     */
    fun stopMonitoring() {
        samplingJob?.cancel()
        samplingJob = null
        if (memoryCallbackRegistered) {
            try {
                context.unregisterComponentCallbacks(componentCallbacks)
            } catch (e: Exception) {
                AppLogger.w(TAG, "取消注册内存回调失败", e)
            }
            memoryCallbackRegistered = false
        }
        AppLogger.d(TAG, "内存监控已停止")
    }

    /**
     * 销毁监控器，释放所有资源
     *
     * 取消所有协程（包括采样任务和压力状态恢复延迟协程），反注册系统回调。
     * 应在 Application.onTerminate 或 AppContainer.destroy 中调用。
     */
    fun dispose() {
        stopMonitoring()
        cleanupCallbacks.clear()
        scope.cancel()
    }

    // MARK: - 内存采样

    /**
     * 执行一次内存采样
     *
     * 获取当前应用内存使用量，更新峰值、历史、告警等级，
     * 达到严重级别时触发 [handleMemoryPressure]。
     *
     * @return 当前内存使用量（MB）
     */
    fun sampleMemory(): Double {
        val memoryMB = getCurrentMemoryUsage()

        _currentMemoryMB.value = memoryMB

        // 更新峰值
        if (memoryMB > _peakMemoryMB.value) {
            _peakMemoryMB.value = memoryMB
        }

        // 更新历史
        val history = _memoryHistory.value.toMutableList()
        history.add(memoryMB)
        while (history.size > MAX_HISTORY_COUNT) {
            history.removeAt(0)
        }
        _memoryHistory.value = history

        // 检查告警等级
        updateWarningLevel(memoryMB)

        // 严重级别触发内存清理
        if (_warningLevel.value == MemoryWarningLevel.CRITICAL) {
            handleMemoryPressure()
            AppLogger.w(
                TAG,
                "内存压力严重: ${String.format("%.1f", memoryMB)}MB (峰值: ${String.format("%.1f", _peakMemoryMB.value)}MB)"
            )
        } else if (_warningLevel.value == MemoryWarningLevel.WARNING) {
            AppLogger.d(TAG, "内存使用较高: ${String.format("%.1f", memoryMB)}MB")
        }

        return memoryMB
    }

    // MARK: - 内存使用获取

    /**
     * 获取当前应用内存使用量（MB）
     *
     * 使用 [Debug.MemoryInfo] 获取应用进程的私有内存（替代 iOS mach_task_basic_info 的 resident_size）。
     *
     * @return 内存使用量（MB）
     */
    fun getCurrentMemoryUsage(): Double {
        return try {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            // getTotalPrivateDirty 返回 KB
            memoryInfo.totalPrivateDirty / 1024.0
        } catch (e: Exception) {
            AppLogger.w(TAG, "获取内存使用失败", e)
            0.0
        }
    }

    /**
     * 获取设备总内存（MB）
     *
     * 使用 [ActivityManager.MemoryInfo]（替代 iOS ProcessInfo.physicalMemory）。
     *
     * @return 设备总内存（MB）
     */
    fun getTotalDeviceMemoryMB(): Double {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            info.totalMem / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * 获取可用内存（MB）
     *
     * @return 可用内存（MB）
     */
    fun getAvailableMemoryMB(): Double {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            info.availMem / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }

    // MARK: - 内存压力处理

    /**
     * 处理内存压力（达到 CRITICAL 级别时调用）
     *
     * 触发所有注册的清理回调，并执行通用清理。
     */
    private fun handleMemoryPressure() {
        performCleanupCallbacks()
        performMemoryCleanup()
    }

    /**
     * 处理系统内存警告（onTrimMemory / onLowMemory 回调）
     */
    private fun handleSystemMemoryWarning() {
        _isUnderMemoryPressure.value = true
        AppLogger.w(
            TAG,
            "收到系统内存警告！当前内存: ${String.format("%.1f", _currentMemoryMB.value)}MB"
        )

        // 立即触发清理
        performCleanupCallbacks()
        performMemoryCleanup()

        // 采样一次
        sampleMemory()

        // 延迟恢复压力状态（3 秒后）
        scope.launch {
            delay(3000L)
            _isUnderMemoryPressure.value = false
        }
    }

    /**
     * 执行所有注册的清理回调
     */
    private fun performCleanupCallbacks() {
        synchronized(cleanupCallbacks) {
            for (callback in cleanupCallbacks) {
                try {
                    callback()
                } catch (e: Exception) {
                    AppLogger.w(TAG, "内存清理回调执行失败", e)
                }
            }
        }
        AppLogger.d(TAG, "已执行 ${cleanupCallbacks.size} 个内存清理回调")
    }

    /**
     * 注册内存清理回调
     *
     * 当达到内存压力阈值时，回调会被调用以释放缓存。
     * 替代 iOS 端的 CIContext.clearCaches 注册机制。
     *
     * @param callback 清理回调
     */
    fun registerCleanupCallback(callback: () -> Unit) {
        synchronized(cleanupCallbacks) {
            cleanupCallbacks.add(callback)
        }
    }

    /**
     * 取消注册内存清理回调
     */
    fun unregisterCleanupCallback(callback: () -> Unit) {
        synchronized(cleanupCallbacks) {
            cleanupCallbacks.remove(callback)
        }
    }

    /**
     * 执行通用内存清理
     *
     * 清理应用级缓存。Bitmap 缓存等由各自模块通过 [registerCleanupCallback] 自行清理。
     */
    private fun performMemoryCleanup() {
        // 提示 GC 回收不可达对象（非强制，仅作为内存压力时的辅助）
        try {
            System.gc()
        } catch (e: Exception) {
            // 忽略
        }
        // 各模块的缓存（Bitmap LRU、Coil 图片缓存等）由 registerCleanupCallback 注册的回调清理
    }

    // MARK: - 告警等级更新

    /**
     * 根据当前内存使用量更新告警等级
     */
    private fun updateWarningLevel(memoryMB: Double) {
        val newLevel = when {
            memoryMB >= criticalThresholdMB -> MemoryWarningLevel.CRITICAL
            memoryMB >= warningThresholdMB -> MemoryWarningLevel.WARNING
            else -> MemoryWarningLevel.NORMAL
        }
        _warningLevel.value = newLevel
    }

    // MARK: - 统计信息

    /**
     * 获取内存使用统计摘要
     */
    val memoryStats: MemoryStats
        get() {
            val history = _memoryHistory.value
            val avg = if (history.isEmpty()) 0.0 else history.average()
            val min = history.minOrNull() ?: 0.0
            val max = history.maxOrNull() ?: 0.0

            return MemoryStats(
                currentMB = _currentMemoryMB.value,
                averageMB = avg,
                minMB = min,
                maxMB = max,
                peakMB = _peakMemoryMB.value,
                totalDeviceMB = getTotalDeviceMemoryMB(),
                availableMB = getAvailableMemoryMB(),
                warningLevel = _warningLevel.value
            )
        }

    // MARK: - 重置

    /**
     * 重置内存统计
     */
    fun reset() {
        _peakMemoryMB.value = 0.0
        _memoryHistory.value = emptyList()
        _warningLevel.value = MemoryWarningLevel.NORMAL
        _isUnderMemoryPressure.value = false
    }
}
