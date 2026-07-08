package com.livecompose.livecapture.core.phantom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 幻影模式控制器
 * 管理幻影模式的启动/停止/权限检查
 */
object PhantomController {

    /**
     * 检查是否有读取媒体图片权限（幻影模式必需）
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
     * 获取需要请求的权限列表
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * 检查所有必要权限
     */
    fun hasAllPermissions(context: Context): Boolean = hasMediaPermission(context)

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
