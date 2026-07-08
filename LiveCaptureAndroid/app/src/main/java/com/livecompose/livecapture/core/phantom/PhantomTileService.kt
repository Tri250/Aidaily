package com.livecompose.livecapture.core.phantom

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * 幻影模式快速设置磁贴
 * 在下拉通知栏中快速切换幻影模式
 */
class PhantomTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (!PhantomController.hasAllPermissions(this)) {
            // 无权限，跳转到 MainActivity 引导授权
            val intent = Intent(this, com.livecompose.livecapture.MainActivity::class.java)
            intent.putExtra("show_ghost_permissions", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(intent)
            return
        }

        val isEnabled = PhantomService.isEnabled(this)
        if (isEnabled) {
            PhantomController.stop(this)
        } else {
            PhantomController.start(this)
        }
        updateTile()
    }

    private fun updateTile() {
        val isEnabled = PhantomService.isEnabled(this)
        qsTile?.apply {
            state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (isEnabled) "幻影模式开" else "幻影模式"
            updateTile()
        }
    }
}
