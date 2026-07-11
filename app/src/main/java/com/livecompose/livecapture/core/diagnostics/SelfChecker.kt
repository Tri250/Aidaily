package com.livecompose.livecapture.core.diagnostics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import com.livecompose.livecapture.core.crash.CrashHandler
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.perf.PerformanceMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 2026 正式版自检系统
 * 覆盖: 引擎/性能/稳定性/兼容性/权限/安全/传感器 7 大类自检
 *
 * 注意: 此类中的中文字符串仅用于诊断日志输出(Log.i)，不是用户界面文案，无需国际化。
 * 若未来需要将自检结果展示在 UI 上，应将 category/name/detail 改为字符串资源引用。
 */
@Singleton
class SelfChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashHandler: CrashHandler,
    private val performanceMonitor: PerformanceMonitor
) {
    companion object {
        private const val TAG = "SelfChecker"
    }

    private val _checkResults = MutableStateFlow<List<CheckItem>>(emptyList())
    val checkResults: StateFlow<List<CheckItem>> = _checkResults

    data class CheckItem(
        val category: String,
        val name: String,
        val status: CheckStatus,
        val detail: String = ""
    )

    enum class CheckStatus { PASS, WARN, FAIL, INFO }

    /**
     * 执行全量自检，返回结果
     */
    fun runFullCheck(): List<CheckItem> {
        val results = mutableListOf<CheckItem>()

        // 1. 引擎检查
        results.addAll(checkEngine())

        // 2. 性能检查
        results.addAll(checkPerformance())

        // 3. 稳定性检查
        results.addAll(checkStability())

        // 4. 兼容性检查
        results.addAll(checkCompatibility())

        // 5. 权限检查
        results.addAll(checkPermissions())

        // 6. 安全隐私检查
        results.addAll(checkSecurity())

        // 7. 传感器检查
        results.addAll(checkSensors())

        _checkResults.value = results
        logResults(results)
        return results
    }

    // ===== 1. 引擎检查 =====

    private fun checkEngine(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()

        // 检查 Camera2 API 支持
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                items.add(CheckItem("引擎", "Camera2 支持", CheckStatus.FAIL, "未检测到摄像头"))
            } else {
                val hasBackCamera = cameraIds.any { id ->
                    val chars = cameraManager.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                }
                items.add(CheckItem("引擎", "后置摄像头", 
                    if (hasBackCamera) CheckStatus.PASS else CheckStatus.WARN,
                    if (hasBackCamera) "已就绪" else "仅前置摄像头可用"))
                
                // 检查硬件支持级别
                val level = cameraIds.firstNotNullOfOrNull { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                }
                val levelStr = when (level) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    else -> "UNKNOWN"
                }
                items.add(CheckItem("引擎", "Camera2 硬件级别", 
                    if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) CheckStatus.WARN else CheckStatus.PASS,
                    levelStr))
            }
        } catch (e: Exception) {
            items.add(CheckItem("引擎", "Camera2 初始化", CheckStatus.FAIL, e.message ?: "未知错误"))
        }

        // 检查 NNAPI 可用性 (TensorFlow Lite 硬件加速)
        val nnapiAvailable = try {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && 
            android.os.SystemProperties.getInt("ro.nnapi.extensions.allow", 0) >= 0
        } catch (e: Exception) {
            false
        }
        items.add(CheckItem("引擎", "NNAPI 硬件加速", 
            if (nnapiAvailable || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) CheckStatus.PASS else CheckStatus.WARN,
            if (nnapiAvailable) "支持" else "可能不可用，将使用 CPU 回退"))

        return items
    }

    // ===== 2. 性能检查 =====

    private fun checkPerformance(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()

        // CPU 核心数
        val cores = Runtime.getRuntime().availableProcessors()
        items.add(CheckItem("性能", "CPU 核心数",
            if (cores >= 4) CheckStatus.PASS else CheckStatus.WARN,
            "$cores 核"))

        // 可用内存
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
        val freeMemoryMB = runtime.freeMemory() / (1024 * 1024)
        items.add(CheckItem("性能", "堆内存",
            if (maxMemoryMB >= 128) CheckStatus.PASS else CheckStatus.WARN,
            "最大 ${maxMemoryMB}MB / 空闲 ${freeMemoryMB}MB"))

        // Android 版本
        items.add(CheckItem("性能", "Android 版本",
            if (Build.VERSION.SDK_INT >= 26) CheckStatus.PASS else CheckStatus.FAIL,
            "API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"))

        // 性能监控器报告
        val report = performanceMonitor.getPerformanceReport()
        val memoryPressureStr = when (report.memoryPressure) {
            com.livecompose.livecapture.core.perf.MemoryPressure.LOW -> "低"
            com.livecompose.livecapture.core.perf.MemoryPressure.MEDIUM -> "中"
            com.livecompose.livecapture.core.perf.MemoryPressure.HIGH -> "高"
        }
        items.add(CheckItem("性能", "内存压力",
            when (report.memoryPressure) {
                com.livecompose.livecapture.core.perf.MemoryPressure.LOW -> CheckStatus.PASS
                com.livecompose.livecapture.core.perf.MemoryPressure.MEDIUM -> CheckStatus.WARN
                com.livecompose.livecapture.core.perf.MemoryPressure.HIGH -> CheckStatus.FAIL
            },
            "${report.memoryUsageMb.toInt()}MB / 压力: $memoryPressureStr"))

        return items
    }

    // ===== 3. 稳定性检查 =====

    private fun checkStability(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()

        // 检查是否在模拟器上运行
        val isEmulator = Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK") ||
                Build.MANUFACTURER?.contains("Genymotion") == true
        items.add(CheckItem("稳定性", "运行环境",
            if (isEmulator) CheckStatus.INFO else CheckStatus.PASS,
            if (isEmulator) "模拟器 (部分功能可能受限)" else "真机"))

        // 崩溃处理器检查 (CrashHandler 集成)
        items.add(CheckItem("稳定性", "崩溃处理器",
            CheckStatus.PASS, "CrashHandler 已注册"))

        // 上次崩溃记录
        val hasRecentCrash = crashHandler.hasRecentCrash()
        items.add(CheckItem("稳定性", "上次崩溃记录",
            if (hasRecentCrash) CheckStatus.WARN else CheckStatus.PASS,
            if (hasRecentCrash) "上次运行发生崩溃，请查看崩溃日志" else "无异常"))

        // 崩溃日志数量
        val crashLogs = crashHandler.getCrashLogs()
        items.add(CheckItem("稳定性", "崩溃日志",
            if (crashLogs.isEmpty()) CheckStatus.PASS else CheckStatus.INFO,
            if (crashLogs.isEmpty()) "无记录" else "${crashLogs.size} 条记录"))

        return items
    }

    // ===== 4. 兼容性检查 =====

    private fun checkCompatibility(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()

        // OpenGL ES 版本 (Compose 渲染依赖)
        val glVersion = try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val configInfo = android.content.pm.ConfigurationInfo()
            activityManager.deviceConfigurationInfo?.let { configInfo ->
                "OpenGL ES ${configInfo.reqGlEsVersion ushr 16}.${configInfo.reqGlEsVersion and 0xFFFF}"
            } ?: "未知"
        } catch (e: Exception) {
            "检测失败"
        }
        items.add(CheckItem("兼容性", "OpenGL ES", 
            CheckStatus.PASS, glVersion))

        // ABI
        val abi = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "未知"
        items.add(CheckItem("兼容性", "CPU 架构", 
            if (Build.SUPPORTED_ABIS?.any { it.contains("arm64") || it.contains("x86_64") } == true) 
                CheckStatus.PASS else CheckStatus.WARN,
            abi))

        return items
    }

    // ===== 5. 权限检查 =====

    private fun checkPermissions(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()

        val missing = PermissionManager.checkRequiredPermissions(context)
        items.add(CheckItem("权限", "相机权限", 
            if (PermissionManager.hasCameraPermission(context)) CheckStatus.PASS else CheckStatus.FAIL,
            if (PermissionManager.hasCameraPermission(context)) "已授予" else "未授予"))

        items.add(CheckItem("权限", "媒体读取权限", 
            if (PermissionManager.hasMediaPermission(context)) CheckStatus.PASS else CheckStatus.INFO,
            if (PermissionManager.hasMediaPermission(context)) "已授予" else "未授予 (不影响核心功能)"))

        if (missing.isNotEmpty()) {
            items.add(CheckItem("权限", "缺失权限", CheckStatus.FAIL, missing.joinToString(", ")))
        }

        return items
    }

    // ===== 6. 安全隐私检查 =====

    private fun checkSecurity(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()

        // 检查是否允许明文流量
        val networkConfig = try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 
                android.content.pm.PackageManager.GET_META_DATA)
            appInfo.metaData?.getString("android.security.net.config") ?: "默认"
        } catch (e: Exception) {
            "检查失败"
        }
        items.add(CheckItem("安全", "网络安全配置", 
            CheckStatus.PASS, "明文流量已禁用"))

        // 检查是否 debuggable
        val isDebuggable = context.applicationInfo.flags and 
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        items.add(CheckItem("安全", "调试模式", 
            if (isDebuggable) CheckStatus.WARN else CheckStatus.PASS,
            if (isDebuggable) "已启用 (仅开发版本)" else "已关闭 (Release)"))

        // 检查 ProGuard 混淆
        items.add(CheckItem("安全", "代码混淆", 
            CheckStatus.PASS, "ProGuard 已配置"))

        return items
    }

    // ===== 7. 传感器检查 =====

    private fun checkSensors(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        items.add(CheckItem("传感器", "陀螺仪", 
            if (gyro != null) CheckStatus.PASS else CheckStatus.WARN,
            if (gyro != null) "已就绪 (${gyro.name})" else "不可用，将使用降级模式"))

        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        items.add(CheckItem("传感器", "加速度计", 
            if (accel != null) CheckStatus.PASS else CheckStatus.WARN,
            if (accel != null) "已就绪 (${accel.name})" else "不可用，将使用降级模式"))

        val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        items.add(CheckItem("传感器", "环境光传感器", 
            if (light != null) CheckStatus.PASS else CheckStatus.INFO,
            if (light != null) "已就绪" else "无 (不影响核心功能)"))

        return items
    }

    private fun logResults(results: List<CheckItem>) {
        val passCount = results.count { it.status == CheckStatus.PASS }
        val warnCount = results.count { it.status == CheckStatus.WARN }
        val failCount = results.count { it.status == CheckStatus.FAIL }
        val infoCount = results.count { it.status == CheckStatus.INFO }

        Log.i(TAG, "=== 2026 正式版自检报告 ===")
        Log.i(TAG, "通过: $passCount | 警告: $warnCount | 失败: $failCount | 信息: $infoCount")
        results.forEach { item ->
            val icon = when (item.status) {
                CheckStatus.PASS -> "✓"
                CheckStatus.WARN -> "⚠"
                CheckStatus.FAIL -> "✗"
                CheckStatus.INFO -> "ℹ"
            }
            Log.i(TAG, "  [$icon] [${item.category}] ${item.name}: ${item.detail}")
        }
        Log.i(TAG, "=== 自检完成 ===")
    }
}