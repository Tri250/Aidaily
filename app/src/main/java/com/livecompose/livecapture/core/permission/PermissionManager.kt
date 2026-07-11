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
 * 权限管理工具类
 * 统一管理相机、媒体等权限的检查与请求
 */
object PermissionManager {

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasMediaPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    fun requestCameraPermission(launcher: ActivityResultLauncher<String>) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    fun requestMediaPermission(launcher: ActivityResultLauncher<String>) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        launcher.launch(permission)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun checkRequiredPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (!hasCameraPermission(context)) {
            missing.add(Manifest.permission.CAMERA)
        }

        if (!hasMediaPermission(context)) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            missing.add(permission)
        }

        return missing
    }
}
