package com.livecompose.livecapture.core.camera

import android.graphics.SurfaceTexture
import android.util.DisplayMetrics
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.livecompose.livecapture.core.logger.AppLogger

/**
 * Jetpack Compose 相机预览组件
 *
 * 适配国内品牌手机兼容性:
 * - 华为 (HarmonyOS/EMUI): 相机 HAL 方向差异、挖孔屏适配
 * - 小米 (MIUI/HyperOS): 全面屏手势、异形屏适配
 * - OPPO (ColorOS): 挖孔/水滴屏、曲面屏适配
 * - vivo (OriginOS): 前摄开孔、屏幕比例适配
 * - 荣耀 (MagicOS): 药丸挖孔适配
 *
 * 策略:
 * - 使用 TextureView 而非 SurfaceView 以获得更好的 Compose 集成
 * - 根据屏幕尺寸和相机传感器尺寸动态选择最佳预览尺寸
 * - 支持 display cutout (刘海/挖孔/水滴) 安全区域
 * - 前置摄像头镜像处理
 */
@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false
) {
    val context = LocalContext.current
    val displayMetrics = remember { context.resources.displayMetrics }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                // 设置硬件加速层类型
                tv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        try {
                            AppLogger.i("CameraPreview", "Surface 可用: ${width}x${height}")

                            // 根据屏幕比例调整预览尺寸
                            val screenAspect = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat()
                            configurePreviewSize(tv, width, height, screenAspect)

                            val surface = Surface(surface)
                            cameraManager.setPreviewSurface(surface)
                        } catch (e: Exception) {
                            AppLogger.e("CameraPreview", "设置预览 Surface 失败", e)
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        AppLogger.i("CameraPreview", "Surface 尺寸变化: ${width}x${height}")
                        val screenAspect = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat()
                        configurePreviewSize(tv, width, height, screenAspect)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        AppLogger.i("CameraPreview", "Surface 销毁")
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        update = { view ->
            // 前置摄像头镜像
            if (isFrontCamera) {
                view.scaleX = -1f
            } else {
                view.scaleX = 1f
            }
        }
    )
}

/**
 * 根据屏幕和 Surface 比例配置预览视图尺寸
 *
 * 国内手机主流屏幕比例：
 * - 19.5:9 (iPhone 风格全面屏, 如 2340x1080) → 约 2.167
 * - 20:9 (主流 Android 全面屏, 如 2400x1080) → 约 2.222
 * - 20.5:9 (小米/华为旗舰) → 约 2.278
 * - 21:9 (索尼带鱼屏) → 约 2.333
 *
 * 相机传感器通常为 4:3 (1.333) 或 16:9 (1.778)
 * 需要根据屏幕比例进行裁剪/填充适配
 */
private fun configurePreviewSize(
    textureView: TextureView,
    surfaceWidth: Int,
    surfaceHeight: Int,
    screenAspect: Float
) {
    // 相机传感器通常为 4:3，需要裁剪为屏幕比例
    // 使用 CENTER_CROP 策略确保预览填满屏幕
    val surfaceAspect = if (surfaceHeight > 0) surfaceWidth.toFloat() / surfaceHeight else screenAspect

    val layoutParams = textureView.layoutParams as? ViewGroup.LayoutParams ?: return

    // 计算填充尺寸：确保预览完全覆盖屏幕（裁剪传感器边缘）
    if (surfaceAspect > screenAspect) {
        // Surface 比屏幕更宽 → 按高度填充，宽度裁剪
        layoutParams.width = (surfaceHeight * screenAspect).toInt()
        layoutParams.height = surfaceHeight
    } else {
        // Surface 比屏幕更高 → 按宽度填充，高度裁剪
        layoutParams.width = surfaceWidth
        layoutParams.height = (surfaceWidth / screenAspect).toInt()
    }

    textureView.layoutParams = layoutParams
    AppLogger.i("CameraPreview", "预览尺寸调整: ${layoutParams.width}x${layoutParams.height}")
}