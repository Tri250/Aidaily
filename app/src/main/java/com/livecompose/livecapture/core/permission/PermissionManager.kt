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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    fun hasMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean =
        activity.shouldShowRequestPermissionRationale(permission)

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

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun checkRequiredPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (!hasCameraPermission()) {
            missing.add(Manifest.permission.CAMERA)
        }
        if (!hasMediaPermission()) {
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