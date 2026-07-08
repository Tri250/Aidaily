package com.livecompose.livecapture.core.editing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * 暗角调整参数
 *
 * 对应 iOS 端 VignetteEditorView 的参数：强度、范围（中点）、羽化、圆度（形状）。
 *
 * @property intensity 暗角强度（0~1），0 表示无暗角
 * @property midpoint 中点/范围（0~1），控制暗角开始的位置，值越大暗角区域越靠外
 * @property feather 羽化（0~1），控制暗角边缘的柔和程度
 * @property roundness 圆度（true=圆形，false=椭圆），对应 iOS isElliptical 取反后的形状选项
 * @property centerX 暗角中心 X（0~1，默认 0.5）
 * @property centerY 暗角中心 Y（0~1，默认 0.5）
 */
data class VignetteParams(
    val intensity: Float = 0f,
    val midpoint: Float = 0.5f,
    val feather: Float = 0.5f,
    val roundness: Boolean = true,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f
) {
    /** 强度为 0 时视为默认（无暗角） */
    val isDefault: Boolean
        get() = intensity <= 0.001f
}

/**
 * 暗角处理器
 *
 * 对应 iOS 端 VignetteEditorView 的暗角应用逻辑，使用 [RadialGradient] 绘制径向渐变遮罩，
 * 通过 [PorterDuff.Mode] 混合实现边缘压暗效果。
 *
 * ## 处理流程
 * 1. 拷贝原图作为输出
 * 2. 创建一个与图像等大的暗角遮罩 Bitmap，使用 [RadialGradient] 绘制：
 *    - 中心透明（不压暗），边缘为半透明黑（按 intensity 压暗）
 *    - midpoint 控制透明区域的半径占比
 *    - feather 控制透明到黑色过渡的宽度
 *    - roundness=false（椭圆）时纵向压缩渐变
 * 3. 使用 [PorterDuff.Mode.SRC_OVER] 将遮罩叠加到输出上
 *
 * 叠加原理：黑色 alpha=A 的像素覆盖后，结果 = 原像素 * (1 - A) + 黑 * A = 原像素 * (1 - A)，
 * 即按 alpha 比例压暗，符合暗角视觉预期。
 */
class VignetteProcessor {

    companion object {
        private const val TAG = "VignetteProcessor"
    }

    /**
     * 应用暗角到 Bitmap
     *
     * @param bitmap 输入 Bitmap
     * @param params 暗角参数
     * @return 调整后的 Bitmap；参数为默认时返回新拷贝
     */
    suspend fun process(bitmap: Bitmap, params: VignetteParams): Bitmap = withContext(Dispatchers.Default) {
        if (params.isDefault) {
            return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        try {
            val width = bitmap.width
            val height = bitmap.height
            val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            applyVignette(output, params)
            output
        } catch (e: OutOfMemoryError) {
            AppLogger.e(TAG, "暗角处理内存不足", e)
            throw RuntimeException("暗角处理内存不足，请尝试降低图像分辨率", e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "暗角处理失败", e)
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * 在 Bitmap 上原地绘制暗角
     */
    private fun applyVignette(bitmap: Bitmap, params: VignetteParams) {
        val width = bitmap.width
        val height = bitmap.height
        val cx = params.centerX * width
        val cy = params.centerY * height

        // 以图像最大维度为参考计算半径，保证暗角覆盖到边角
        val maxDim = max(width, height).toFloat()
        // 外半径：始终覆盖到画布对角，确保边角被压暗
        val outerRadius = maxDim * 1.5f
        // 内半径（透明区）：由 midpoint 控制，越大则中心不被压暗的区域越大
        val innerRadius = outerRadius * params.midpoint.coerceIn(0f, 1f)

        // 椭圆时纵向压缩比例
        val ellipseScaleY = if (params.roundness) 1f else 0.7f

        // 最大透明度对应的黑色 alpha（0~255），由 intensity 控制
        val maxAlpha = (params.intensity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)

        // 构建径向渐变：中心透明 -> 内半径透明 -> 外半径全强度黑
        // 通过三色 stop 实现内透明区 + 羽化过渡
        val transparent = Color.argb(0, 0, 0, 0)
        val blackFull = Color.argb(maxAlpha, 0, 0, 0)

        // feather 控制 transparent->black 的过渡占比，过渡起点为 innerRadius/outerRadius
        val stopInner = (innerRadius / outerRadius).coerceIn(0f, 0.99f)
        val stopOuter = min(1f, stopInner + params.feather.coerceIn(0.01f, 1f) * (1f - stopInner))

        val colors = intArrayOf(transparent, transparent, blackFull)
        val stops = floatArrayOf(0f, stopInner, stopOuter)

        val shader = RadialGradient(
            cx, cy, outerRadius,
            colors, stops,
            Shader.TileMode.CLAMP
        )

        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
            this.shader = shader
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

        if (ellipseScaleY < 1f) {
            // 椭圆暗角：以中心为锚点纵向压缩渐变
            canvas.save()
            canvas.scale(1f, ellipseScaleY, cx, cy)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            canvas.restore()
        } else {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
}
