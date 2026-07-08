package com.livecompose.livecapture.core.phantom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Environment

/**
 * 幻影模式控制器
 * 管理幻影模式的启动/停止/权限检查
 */
object PhantomController {

    /**
     * 检查是否有悬浮窗权限
     */
    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /**
     * 请求悬浮窗权限
     */
    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 检查是否有所有文件访问权限 (Android 11+)
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Android 10 及以下使用传统存储权限
        }
    }

    /**
     * 请求所有文件访问权限
     */
    fun requestStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * 检查所有必要权限
     */
    fun hasAllPermissions(context: Context): Boolean =
        hasOverlayPermission(context) && hasStoragePermission(context)

    /**
     * 启动幻影模式
     */
    fun start(context: Context) {
        if (!hasAllPermissions(context)) return
        PhantomService.setEnabled(context, true)
        val intent = Intent(context, PhantomService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * 停止幻影模式
     */
    fun stop(context: Context) {
        PhantomService.setEnabled(context, false)
        context.stopService(Intent(context, PhantomService::class.java))
    }

    /**
     * 切换幻影模式
     */
    fun toggle(context: Context): Boolean {
        val isEnabled = PhantomService.isEnabled(context)
        if (isEnabled) {
            stop(context)
            return false
        } else {
            start(context)
            return true
        }
    }
}
