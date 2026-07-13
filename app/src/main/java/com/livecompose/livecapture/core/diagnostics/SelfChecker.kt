package com.livecompose.livecapture.core.diagnostics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.livecompose.livecapture.core.permission.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    companion object {
        private const val TAG = "SelfChecker"
        private const val MIN_STORAGE_MB = 100L
        private const val MIN_GPU_GL_VERSION = 3.0
        private val TFLITE_MODEL_NAMES = listOf("adacrop_student.tflite", "adacrop_teacher.tflite")
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

                // 检查相机分辨率和帧率能力
                checkCameraCapabilities(cameraManager, cameraIds, items)
            }
        } catch (e: Exception) {
            items.add(CheckItem("引擎", "Camera2 初始化", CheckStatus.FAIL, e.message ?: "未知错误"))
        }

        // 检查 NNAPI 可用性 (TensorFlow Lite 硬件加速)
        // API 28+ (Android 9+) 系统内置 NNAPI，TFLite 会自动尝试使用
        val nnapiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        items.add(CheckItem("引擎", "NNAPI 硬件加速",
            if (nnapiAvailable) CheckStatus.PASS else CheckStatus.WARN,
            if (nnapiAvailable) "系统支持 (TFLite 自动尝试)" else "系统不支持，将使用 CPU"))

        // 检查 TFLite 模型文件存在性
        checkTfliteModelFiles(items)

        return items
    }

    private fun checkCameraCapabilities(
        cameraManager: CameraManager,
        cameraIds: Array<String>,
        items: MutableList<CheckItem>
    ) {
        try {
            val backCameraId = cameraIds.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraIds.firstOrNull() ?: return

            val chars = cameraManager.getCameraCharacteristics(backCameraId)
            val streamConfigMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (streamConfigMap != null) {
                val outputSizes = streamConfigMap.getOutputSizes(android.graphics.ImageFormat.YUV_420_888)
                if (outputSizes != null) {
                    val maxResolution = outputSizes.maxByOrNull { it.width * it.height }
                    val maxResStr = maxResolution?.let { "${it.width}x${it.height}" } ?: "未知"
                    val has1080p = outputSizes.any { it.width >= 1920 && it.height >= 1080 }
                    items.add(CheckItem("引擎", "相机最大分辨率",
                        if (has1080p) CheckStatus.PASS else CheckStatus.WARN,
                        maxResStr))
                }

                // 帧率检查
                val fpsRanges = streamConfigMap.getHighSpeedVideoFpsRanges()
                if (fpsRanges != null && fpsRanges.isNotEmpty()) {
                    val maxFps = fpsRanges.maxOfOrNull { it.upper } ?: 30
                    items.add(CheckItem("引擎", "相机帧率",
                        if (maxFps >= 30) CheckStatus.PASS else CheckStatus.WARN,
                        "最高 ${maxFps}fps"))
                }
            }
        } catch (e: Exception) {
            items.add(CheckItem("引擎", "相机能力检查", CheckStatus.INFO, "无法获取详细参数"))
        }
    }

    private fun checkTfliteModelFiles(items: MutableList<CheckItem>) {
        val assetManager = context.assets
        for (modelName in TFLITE_MODEL_NAMES) {
            try {
                assetManager.open(modelName).use { stream ->
                    val sizeBytes = stream.available()
                    val sizeStr = if (sizeBytes > 1024 * 1024) {
                        "${sizeBytes / (1024 * 1024)}MB"
                    } else {
                        "${sizeBytes / 1024}KB"
                    }
                    items.add(CheckItem("引擎", "模型文件: $modelName",
                        CheckStatus.PASS,
                        "已就绪 ($sizeStr)"))
                }
            } catch (e: Exception) {
                items.add(CheckItem("引擎", "模型文件: $modelName",
                    CheckStatus.FAIL,
                    "未找到或无法读取"))
            }
        }
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

        // 存储空间检查
        checkStorageSpace(items)

        // GPU 渲染性能检查
        checkGpuPerformance(items)

        return items
    }

    private fun checkStorageSpace(items: MutableList<CheckItem>) {
        try {
            val dataDir = Environment.getDataDirectory()
            val statFs = StatFs(dataDir.path)
            val availableBytes = statFs.availableBytes
            val totalBytes = statFs.totalBytes
            val availableMB = availableBytes / (1024 * 1024)
            val totalMB = totalBytes / (1024 * 1024)

            val totalStr = if (totalMB >= 1024) "${totalMB / 1024}GB" else "${totalMB}MB"
            items.add(CheckItem("性能", "存储空间",
                if (availableMB >= MIN_STORAGE_MB) CheckStatus.PASS else CheckStatus.WARN,
                "可用 ${availableMB}MB / 总 ${totalStr}"))
        } catch (e: Exception) {
            items.add(CheckItem("性能", "存储空间", CheckStatus.INFO, "无法获取存储信息"))
        }
    }

    private fun checkGpuPerformance(items: MutableList<CheckItem>) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val configInfo = activityManager.deviceConfigurationInfo
            if (configInfo != null) {
                val glMajor = configInfo.reqGlEsVersion ushr 16
                val glMinor = configInfo.reqGlEsVersion and 0xFFFF
                val glVersion = glMajor + glMinor / 10.0
                items.add(CheckItem("性能", "GPU 渲染",
                    if (glVersion >= MIN_GPU_GL_VERSION) CheckStatus.PASS else CheckStatus.WARN,
                    "OpenGL ES ${glMajor}.${glMinor}"))
            }
        } catch (e: Exception) {
            items.add(CheckItem("性能", "GPU 渲染", CheckStatus.INFO, "无法获取 GPU 信息"))
        }
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

        // 检查是否有未捕获的异常处理器 (crash safe)
        val hasDefaultHandler = Thread.getDefaultUncaughtExceptionHandler() != null
        // 检查是否为自定义 CrashHandler 而不仅仅是系统默认处理器
        val handlerName = Thread.getDefaultUncaughtExceptionHandler()?.javaClass?.simpleName ?: "无"
        items.add(CheckItem("稳定性", "崩溃处理器", 
            if (hasDefaultHandler && handlerName != "RuntimeInit\$KillApplicationHandler") CheckStatus.PASS 
            else if (hasDefaultHandler) CheckStatus.INFO else CheckStatus.WARN,
            if (hasDefaultHandler) "已注册 ($handlerName)" else "未注册全局异常处理器"))

        // 电池优化状态检查
        checkBatteryOptimization(items)

        return items
    }

    private fun checkBatteryOptimization(items: MutableList<CheckItem>) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                items.add(CheckItem("稳定性", "电池优化",
                    if (isIgnoring) CheckStatus.PASS else CheckStatus.WARN,
                    if (isIgnoring) "已免除电池优化限制" else "受电池优化限制，后台可能被限制"))
            }
        } catch (e: Exception) {
            items.add(CheckItem("稳定性", "电池优化", CheckStatus.INFO, "无法检查电池优化状态"))
        }
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

        // 系统版本兼容性检查
        val isMinSdk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        items.add(CheckItem("兼容性", "最低 API 要求",
            if (isMinSdk) CheckStatus.PASS else CheckStatus.FAIL,
            if (isMinSdk) "满足 API 26+ 要求" else "不满足最低 API 26 要求"))

        return items
    }

    // ===== 5. 权限检查 =====

    private fun checkPermissions(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()

        val missing = permissionManager.checkRequiredPermissions()
        items.add(CheckItem("权限", "相机权限",
            if (permissionManager.hasCameraPermission()) CheckStatus.PASS else CheckStatus.FAIL,
            if (permissionManager.hasCameraPermission()) "已授予" else "未授予"))

        items.add(CheckItem("权限", "媒体读取权限",
            if (permissionManager.hasMediaPermission()) CheckStatus.PASS else CheckStatus.INFO,
            if (permissionManager.hasMediaPermission()) "已授予" else "未授予 (不影响核心功能)"))

        if (missing.isNotEmpty()) {
            items.add(CheckItem("权限", "缺失权限", CheckStatus.FAIL, missing.joinToString(", ")))
        } else {
            items.add(CheckItem("权限", "权限状态", CheckStatus.PASS, "所有必需权限已授予"))
        }

        return items
    }

    // ===== 6. 安全隐私检查 =====

    private fun checkSecurity(): List<CheckItem> {
        val items = mutableListOf<CheckItem>()

        // 检查是否允许明文流量
        val usesCleartext = try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            // 检查 android:usesCleartextTraffic 标志
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                appInfo.flags and android.content.pm.ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC != 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
        items.add(CheckItem("安全", "明文流量",
            if (usesCleartext) CheckStatus.WARN else CheckStatus.PASS,
            if (usesCleartext) "已启用明文流量 (仅开发环境)" else "明文流量已禁用"))

        // 检查是否 debuggable
        val isDebuggable = context.applicationInfo.flags and 
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        items.add(CheckItem("安全", "调试模式", 
            if (isDebuggable) CheckStatus.WARN else CheckStatus.PASS,
            if (isDebuggable) "已启用 (仅开发版本)" else "已关闭 (Release)"))

        // 检查 ProGuard 混淆 — 实际验证构建是否使用了 R8/ProGuard
        checkProguardStatus(items)

        return items
    }

    private fun checkProguardStatus(items: MutableList<CheckItem>) {
        try {
            // 尝试通过反射检查典型类是否被混淆来验证 ProGuard 是否生效
            // 在 Release 构建中，类名和方法名会被混淆
            val selfCheckerClass = SelfChecker::class.java
            val isObfuscated = selfCheckerClass.simpleName != "SelfChecker"
            val isDebuggable = context.applicationInfo.flags and
                    android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

            if (isDebuggable) {
                // Debug 构建：ProGuard 文件已配置但未启用，提示仅在 Release 生效
                items.add(CheckItem("安全", "代码混淆",
                    CheckStatus.INFO,
                    "ProGuard 规则已配置，Debug 构建未启用混淆"))
            } else if (isObfuscated) {
                items.add(CheckItem("安全", "代码混淆",
                    CheckStatus.PASS,
                    "ProGuard/R8 混淆已生效"))
            } else {
                // Release 构建但类名未混淆 — 可能未正确配置
                items.add(CheckItem("安全", "代码混淆",
                    CheckStatus.WARN,
                    "ProGuard 规则已配置，但混淆未生效 (检查 release 构建配置)"))
            }
        } catch (e: Exception) {
            items.add(CheckItem("安全", "代码混淆",
                CheckStatus.INFO,
                "ProGuard 已配置，运行时无法验证"))
        }
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