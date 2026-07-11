package com.livecompose.livecapture.core.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * 统一权限管理器 — 2026 正式版
 * 集中管理所有运行时权限请求、状态检查、Rationale 判断和设置跳转。
 */
object PermissionManager {

    /**
     * 检查相机权限
     */
    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * 检查媒体读取权限 (Android 13+ 使用 READ_MEDIA_IMAGES, 旧版使用 READ_EXTERNAL_STORAGE)
     */
    fun hasMediaPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 判断是否应该显示权限解释 UI (Rationale)
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean =
        activity.shouldShowRequestPermissionRationale(permission)

    /**
     * 请求相机权限
     */
    fun requestCameraPermission(launcher: ActivityResultLauncher<String>) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    /**
     * 请求媒体读取权限
     */
    fun requestMediaPermission(launcher: ActivityResultLauncher<String>) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        launcher.launch(permission)
    }

    /**
     * 跳转到应用设置页面
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    /**
     * 自检所有必需权限
     * @return 缺失的权限列表
     */
    fun checkRequiredPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        if (!hasCameraPermission(context)) {
            missing.add(Manifest.permission.CAMERA)
        }
        if (!hasMediaPermission(context)) {
            missing.add(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            )
        }
        return missing
    }
}