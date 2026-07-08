package com.livecompose.livecapture.core.camera

import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Jetpack Compose 相机预览组件
 * 对应 iOS 的 CameraPreviewView
 */
@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        try {
                            val surface = Surface(surface)
                            cameraManager.setPreviewSurface(surface)
                        } catch (e: Exception) {
                            Log.e("CameraPreview", "设置预览 Surface 失败", e)
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        update = { view ->
            if (isFrontCamera) {
                view.scaleX = -1f
            } else {
                view.scaleX = 1f
            }
        }
    )
}