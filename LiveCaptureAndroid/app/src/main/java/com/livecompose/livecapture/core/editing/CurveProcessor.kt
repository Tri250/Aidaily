package com.livecompose.livecapture.core.editing

import android.graphics.Bitmap
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 曲线通道
 *
 * 对应 iOS 端 CurveChannel，支持 RGB 主曲线与红/绿/蓝三通道独立曲线。
 */
enum class CurveChannel(val displayName: String) {
    RGB("RGB"),
    RED("R"),
    GREEN("G"),
    BLUE("B")
}

/**
 * 曲线预设
 *
 * 对应 iOS 端 CurvePreset，提供线性、柔和对比、强对比、提亮阴影、压暗高光五种预设。
 * 每个预设返回 5 个归一化控制点（x、y 均为 0~1）。
 */
enum class CurvePreset(val displayName: String) {
    LINEAR("线性"),
    SOFT_CONTRAST("柔和对比"),
    STRONG_CONTRAST("强对比"),
    LIFT_SHADOWS("提亮阴影"),
    CRUSH_HIGHLIGHTS("压暗高光");

    /** 预设对应的 5 个控制点（x, y 均为 0~1） */
    fun controlPoints(): List<CurveControlPoint> = when (this) {
        LINEAR -> listOf(
            CurveControlPoint(0.00f, 0.00f),
            CurveControlPoint(0.25f, 0.25f),
            CurveControlPoint(0.50f, 0.50f),
            CurveControlPoint(0.75f, 0.75f),
            CurveControlPoint(1.00f, 1.00f)
        )
        SOFT_CONTRAST -> listOf(
            CurveControlPoint(0.00f, 0.00f),
            CurveControlPoint(0.25f, 0.20f),
            CurveControlPoint(0.50f, 0.50f),
            CurveControlPoint(0.75f, 0.80f),
            CurveControlPoint(1.00f, 1.00f)
        )
        STRONG_CONTRAST -> listOf(
            CurveControlPoint(0.00f, 0.00f),
            CurveControlPoint(0.25f, 0.13f),
            CurveControlPoint(0.50f, 0.50f),
            CurveControlPoint(0.75f, 0.87f),
            CurveControlPoint(1.00f, 1.00f)
        )
        LIFT_SHADOWS -> listOf(
            CurveControlPoint(0.00f, 0.10f),
            CurveControlPoint(0.25f, 0.35f),
            CurveControlPoint(0.50f, 0.50f),
            CurveControlPoint(0.75f, 0.75f),
            CurveControlPoint(1.00f, 1.00f)
        )
        CRUSH_HIGHLIGHTS -> listOf(
            CurveControlPoint(0.00f, 0.00f),
            CurveControlPoint(0.25f, 0.25f),
            CurveControlPoint(0.50f, 0.50f),
            CurveControlPoint(0.75f, 0.65f),
            CurveControlPoint(1.00f, 0.90f)
        )
    }
}

/**
 * 曲线控制点（归一化坐标 0~1）
 */
data class CurveControlPoint(val x: Float, val y: Float)

/**
 * 曲线调整参数
 *
 * @property master RGB 主曲线控制点（作用于全部三通道）
 * @property red 红通道控制点
 * @property green 绿通道控制点
 * @property blue 蓝通道控制点
 */
data class CurveParams(
    val master: List<CurveControlPoint> = CurvePreset.LINEAR.controlPoints(),
    val red: List<CurveControlPoint> = CurvePreset.LINEAR.controlPoints(),
    val green: List<CurveControlPoint> = CurvePreset.LINEAR.controlPoints(),
    val blue: List<CurveControlPoint> = CurvePreset.LINEAR.controlPoints()
) {
    /** 所有通道均为线性时视为默认（无调整） */
    val isDefault: Boolean
        get() = isLinear(master) && isLinear(red) && isLinear(green) && isLinear(blue)

    private fun isLinear(points: List<CurveControlPoint>): Boolean {
        if (points.isEmpty()) return true
        return points.all { kotlin.math.abs(it.x - it.y) < 0.001f }
    }
}

/**
 * 色调曲线处理器
 *
 * 对应 iOS 端 CurveEditorView 的曲线应用逻辑，使用控制点生成 256 项查找表（LUT），
 * 通过像素级查找表应用到 Bitmap。
 *
 * ## 处理流程
 * 1. [buildLut] 使用 Catmull-Rom 样条对控制点插值，生成 256 项 LUT
 * 2. 先应用主曲线（master）到 R/G/B 三通道，再分别应用各通道曲线
 * 3. 使用 [Bitmap.getPixels] / [Bitmap.setPixels] 进行像素级处理
 *
 * ## 与 UI 的一致性
 * 曲线编辑器 UI（CurveEditorScreen）使用相同的 Catmull-Rom 样条绘制贝塞尔曲线，
 * 保证预览与处理结果一致。
 */
class CurveProcessor {

    companion object {
        private const val TAG = "CurveProcessor"
        private const val LUT_SIZE = 256
    }

    /**
     * 应用曲线调整到 Bitmap
     *
     * @param bitmap 输入 Bitmap
     * @param params 曲线参数
     * @return 调整后的 Bitmap；参数为默认时返回新拷贝
     */
    suspend fun process(bitmap: Bitmap, params: CurveParams): Bitmap = withContext(Dispatchers.Default) {
        if (params.isDefault) {
            return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // 生成各通道 LUT
            val masterLut = buildLut(params.master)
            val rLut = buildLut(params.red)
            val gLut = buildLut(params.green)
            val bLut = buildLut(params.blue)

            val needMaster = !params.master.all { kotlin.math.abs(it.x - it.y) < 0.001f }
            val needR = !params.red.all { kotlin.math.abs(it.x - it.y) < 0.001f }
            val needG = !params.green.all { kotlin.math.abs(it.x - it.y) < 0.001f }
            val needB = !params.blue.all { kotlin.math.abs(it.x - it.y) < 0.001f }

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // 先应用主曲线，再应用各通道曲线
                val outR = (if (needR) rLut[if (needMaster) masterLut[r] else r] else if (needMaster) masterLut[r] else r)
                val outG = (if (needG) gLut[if (needMaster) masterLut[g] else g] else if (needMaster) masterLut[g] else g)
                val outB = (if (needB) bLut[if (needMaster) masterLut[b] else b] else if (needMaster) masterLut[b] else b)

                pixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
        } catch (e: OutOfMemoryError) {
            AppLogger.e(TAG, "曲线处理内存不足", e)
            throw RuntimeException("曲线处理内存不足，请尝试降低图像分辨率", e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "曲线处理失败", e)
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * 采样曲线用于 UI 绘制
     *
     * 返回 [sampleCount] 个均匀分布的 y 值（0~1），与 [buildLut] 使用相同的
     * Catmull-Rom 插值，保证 UI 绘制的曲线与实际应用的 LUT 视觉完全一致。
     *
     * @param points 控制点列表
     * @param sampleCount 采样数量，默认 256
     * @return 长度为 sampleCount 的 FloatArray，每个元素为 0~1
     */
    fun sampleCurve(points: List<CurveControlPoint>, sampleCount: Int = LUT_SIZE): FloatArray {
        val samples = FloatArray(sampleCount)
        val pts = normalizePoints(points)
        val xs = FloatArray(pts.size) { pts[it].x.coerceIn(0f, 1f) }
        val ys = FloatArray(pts.size) { pts[it].y.coerceIn(0f, 1f) }
        for (i in 0 until sampleCount) {
            val t = if (sampleCount > 1) i / (sampleCount - 1).toFloat() else 0f
            samples[i] = catmullRomSample(t, xs, ys).coerceIn(0f, 1f)
        }
        return samples
    }

    /**
     * 归一化控制点：保证至少 2 个点且端点 x 固定在 0 和 1
     */
    private fun normalizePoints(points: List<CurveControlPoint>): List<CurveControlPoint> {
        return if (points.size < 2) {
            listOf(CurveControlPoint(0f, 0f), CurveControlPoint(1f, 1f))
        } else {
            val sorted = points.sortedBy { it.x }
            sorted.toMutableList().also {
                it[0] = CurveControlPoint(0f, it[0].y.coerceIn(0f, 1f))
                val last = it.size - 1
                it[last] = CurveControlPoint(1f, it[last].y.coerceIn(0f, 1f))
            }
        }
    }

    /**
     * 由控制点构建 256 项查找表
     *
     * 使用 Catmull-Rom 样条对控制点插值（端点处使用钳位切线），
     * 保证曲线穿过所有控制点且过渡平滑，与 UI 绘制的贝塞尔曲线一致。
     *
     * @param points 控制点列表（需按 x 排序，x、y 为 0~1）
     * @return 长度为 256 的 IntArray，每个元素为 0~255
     */
    fun buildLut(points: List<CurveControlPoint>): IntArray {
        val lut = IntArray(LUT_SIZE)
        val pts = normalizePoints(points)
        val xs = FloatArray(pts.size) { pts[it].x.coerceIn(0f, 1f) }
        val ys = FloatArray(pts.size) { pts[it].y.coerceIn(0f, 1f) }

        for (i in 0 until LUT_SIZE) {
            val t = i / (LUT_SIZE - 1).toFloat()
            val v = catmullRomSample(t, xs, ys).coerceIn(0f, 1f)
            lut[i] = (v * (LUT_SIZE - 1)).roundToInt().coerceIn(0, LUT_SIZE - 1)
        }
        return lut
    }

    /**
     * Catmull-Rom 样条采样
     *
     * 在控制点之间使用分段三次 Catmull-Rom 插值（端点处切线钳位）。
     *
     * @param t 采样位置（0~1）
     * @param xs 控制点 x 数组（递增，首尾为 0/1）
     * @param ys 控制点 y 数组
     * @return 采样值（0~1）
     */
    private fun catmullRomSample(t: Float, xs: FloatArray, ys: FloatArray): Float {
        val n = xs.size
        if (t <= xs[0]) return ys[0]
        if (t >= xs[n - 1]) return ys[n - 1]

        // 找到 t 所在区间 [i, i+1]
        var i = 0
        while (i < n - 1 && xs[i + 1] < t) i++

        val p0 = ys[if (i - 1 >= 0) i - 1 else i]
        val p1 = ys[i]
        val p2 = ys[i + 1]
        val p3 = ys[if (i + 2 < n) i + 2 else i + 1]

        val x0 = xs[if (i - 1 >= 0) i - 1 else i]
        val x1 = xs[i]
        val x2 = xs[i + 1]
        val x3 = xs[if (i + 2 < n) i + 2 else i + 1]

        // 区间内局部参数
        val span = (x2 - x1)
        val localT = if (span > 0f) (t - x1) / span else 0f

        // 非均匀 Catmull-Rom（使用 x 间隔加权切线，避免控制点 x 不均匀时过冲）
        val t1x = if (x2 - x0 > 0f) (p2 - p0) / (x2 - x0) * span else 0f
        val t2x = if (x3 - x1 > 0f) (p3 - p1) / (x3 - x1) * span else 0f

        val t2 = localT * localT
        val t3 = t2 * localT

        // Hermite 基函数
        val h00 = 2f * t3 - 3f * t2 + 1f
        val h10 = t3 - 2f * t2 + localT
        val h01 = -2f * t3 + 3f * t2
        val h11 = t3 - t2

        return h00 * p1 + h10 * t1x + h01 * p2 + h11 * t2x
    }
}
