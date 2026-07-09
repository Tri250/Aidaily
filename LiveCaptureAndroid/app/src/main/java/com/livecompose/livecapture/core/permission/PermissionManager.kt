package com.livecompose.livecapture.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.livecompose.livecapture.core.logger.AppLogger

/**
 * 统一权限管理中心
 *
 * 负责所有 Android 运行时权限的检查、请求和引导。
 * 适配国内品牌手机权限策略差异：
 *   - 华为 EMUI/HarmonyOS: 严格的权限分组，需注意权限依赖
 *   - 小米 MIUI/HyperOS: 定制权限管理界面，部分权限需额外引导
 *   - OPPO ColorOS: 智能权限管理，可能自动回收后台权限
 *   - vivo OriginOS: 权限弹窗有额外确认步骤
 *   - 荣耀 MagicOS: 与华为类似，权限分组严格
 */
class PermissionManager(private val context: Context) {

    companion object {
        private const val TAG = "PermissionManager"

        @Volatile
        private var instance: PermissionManager? = null

        fun getInstance(context: Context): PermissionManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // MARK: - 相机权限组

    /** 检查相机权限 */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    /** 相机权限所需的权限列表 */
    val cameraPermissions: List<String>
        get() = listOf(Manifest.permission.CAMERA)

    // MARK: - 存储权限组（按 SDK 版本）

    /** 检查存储/媒体权限 */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: 细粒度媒体权限
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 及以下
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /** 存储权限列表 */
    val storagePermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    // MARK: - 麦克风权限

    /** 检查麦克风权限 */
    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    val microphonePermissions: List<String>
        get() = listOf(Manifest.permission.RECORD_AUDIO)

    // MARK: - 位置权限组

    /** 检查粗略位置权限 */
    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    /** 检查精确定位权限 */
    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    /** 检查是否有任何位置权限 */
    fun hasAnyLocationPermission(): Boolean {
        return hasCoarseLocationPermission() || hasFineLocationPermission()
    }

    val locationPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    // MARK: - 通知权限（Android 13+）

    /** 检查通知权限 */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 及以下不需要运行时权限
        }
    }

    // MARK: - 批量权限检查（相机启动时）

    /** 检查相机启动所需的最小权限 */
    fun hasCameraStartupPermissions(): Boolean {
        return hasCameraPermission()
    }

    /** 检查相机启动所需的所有权限 */
    val cameraStartupPermissions: List<String>
        get() = cameraPermissions

    /** 检查完整功能所需的所有权限 */
    fun hasAllCorePermissions(): Boolean {
        return hasCameraPermission() && hasStoragePermission() && hasMicrophonePermission()
    }

    val allCorePermissions: List<String>
        get() {
            val permissions = mutableListOf<String>()
            permissions.addAll(cameraPermissions)
            permissions.addAll(storagePermissions)
            permissions.addAll(microphonePermissions)
            return permissions
        }

    // MARK: - 权限状态总结

    data class PermissionSummary(
        val camera: Boolean,
        val storage: Boolean,
        val microphone: Boolean,
        val location: Boolean,
        val notification: Boolean
    ) {
        val allGranted: Boolean
            get() = camera && storage && microphone && location && notification

        val coreGranted: Boolean
            get() = camera && storage && microphone
    }

    fun getPermissionSummary(): PermissionSummary {
        return PermissionSummary(
            camera = hasCameraPermission(),
            storage = hasStoragePermission(),
            microphone = hasMicrophonePermission(),
            location = hasAnyLocationPermission(),
            notification = hasNotificationPermission()
        )
    }

    // MARK: - 日志

    fun logPermissionStatus() {
        val summary = getPermissionSummary()
        AppLogger.i(TAG, "=== 权限状态 ===")
        AppLogger.i(TAG, "相机: ${if (summary.camera) "已授权" else "未授权"}")
        AppLogger.i(TAG, "存储: ${if (summary.storage) "已授权" else "未授权"}")
        AppLogger.i(TAG, "麦克风: ${if (summary.microphone) "已授权" else "未授权"}")
        AppLogger.i(TAG, "位置: ${if (summary.location) "已授权" else "未授权"}")
        AppLogger.i(TAG, "通知: ${if (summary.notification) "已授权" else "未授权"}")
        AppLogger.i(TAG, "核心权限完整: ${if (summary.coreGranted) "是" else "否"}")
    }
}