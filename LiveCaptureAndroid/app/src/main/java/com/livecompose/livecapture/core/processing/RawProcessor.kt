package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * RAW 处理管线
 * 基于 Android Camera2 RAW sensor 数据的处理管线
 * 简化版实现（无需 NDK/LibRaw），使用纯 Kotlin 算法
 * 流程：去马赛克 → 色彩校正 → 色调映射 → 输出
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object RawProcessor {

    // 常见传感器的色彩校正矩阵 (sRGB D65)
    private val SRGB_D65_CCM = floatArrayOf(
         3.2406f, -1.5372f, -0.4986f,
        -0.9689f,  1.8758f,  0.0415f,
         0.0557f, -0.2040f,  1.0570f
    )

    // D65 白点
    private val D65 = floatArrayOf(0.95047f, 1.0f, 1.08883f)

    /**
     * 处理 RAW Image 为 Bitmap
     * 完整管线：去马赛克 → 黑电平减除 → 色彩校正 → 色调映射 → 输出
     */
    suspend fun processRawImage(
        image: Image,
        blackLevel: Int = 0,
        whiteLevel: Int = 1023,
        exposureCompensation: Float = 1.0f,
        toneMapping: ToneMappingMode = ToneMappingMode.FILMIC
    ): Bitmap = withContext(Dispatchers.Default) {
        require(image.format == ImageFormat.RAW_SENSOR) { "Expected RAW_SENSOR format" }

        val plane = image.planes[0]
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val buffer = plane.buffer

        // Step 1: 提取 RAW 像素数据
        val rawData = ShortArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = y * rowStride + x * pixelStride
                rawData[y * width + x] = buffer.getShort(offset)
            }
        }

        // Step 2: 黑电平减除 + 归一化
        val normalizedData = normalizeRawData(rawData, blackLevel, whiteLevel, width, height)

        // Step 3: 去马赛克（双线性插值）
        val demosaicedData = demosaicBilinear(normalizedData, width, height)

        // Step 4: 色彩校正矩阵
        val colorCorrectedData = applyColorCorrectionMatrix(demosaicedData, width, height)

        // Step 5: 色调映射
        val toneMappedData = applyToneMapping(colorCorrectedData, width, height, exposureCompensation, toneMapping)

        // Step 6: 伽马编码 + 生成 Bitmap
        val bitmap = rawToBitmap(toneMappedData, width, height)

        // 旋转（根据 EXIF 方向）
        val rotation = image.rotationInfo
        if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    /**
     * 黑电平减除 + 归一化到 [0, 1]
     */
    private fun normalizeRawData(
        data: ShortArray, blackLevel: Int, whiteLevel: Int,
        width: Int, height: Int
    ): FloatArray {
        val result = FloatArray(data.size)
        val range = (whiteLevel - blackLevel).toFloat()
        for (i in data.indices) {
            val value = (data[i].toInt() and 0xFFFF) - blackLevel
            result[i] = (value / range).coerceIn(0f, 1f)
        }
        return result
    }

    /**
     * 双线性插值去马赛克
     * 假设 RGGB Bayer 排列:
     * R  G
     * G  B
     */
    private fun demosaicBilinear(raw: FloatArray, width: Int, height: Int): FloatArray {
        val rgb = FloatArray(width * height * 3)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val isGreenRow = y % 2 == 0
                val isGreenCol = x % 2 == 0

                val r: Float
                val g: Float
                val b: Float

                when {
                    // R 像素 (偶数行, 奇数列 in RGGB)
                    !isGreenRow && !isGreenCol -> {
                        r = raw[idx]
                        g = averageNeighbors(raw, x, y, width, height, greenOffsets())
                        b = averageDiagonal(raw, x, y, width, height)
                    }
                    // B 像素 (奇数行, 偶数列 in RGGB)
                    isGreenRow && isGreenCol && y > 0 -> {
                        r = averageDiagonal(raw, x, y, width, height)
                        g = averageNeighbors(raw, x, y, width, height, greenOffsets())
                        b = raw[idx]
                    }
                    // 绿像素 on R 行 (偶数行, 偶数列)
                    !isGreenRow && isGreenCol -> {
                        r = averageHorizontal(raw, x, y, width, height)
                        g = raw[idx]
                        b = averageVertical(raw, x, y, width, height)
                    }
                    // 绿像素 on B 行 (奇数行, 奇数列)
                    else -> {
                        r = averageVertical(raw, x, y, width, height)
                        g = raw[idx]
                        b = averageHorizontal(raw, x, y, width, height)
                    }
                }

                val outIdx = idx * 3
                rgb[outIdx] = r.coerceIn(0f, 1f)
                rgb[outIdx + 1] = g.coerceIn(0f, 1f)
                rgb[outIdx + 2] = b.coerceIn(0f, 1f)
            }
        }

        return rgb
    }

    private fun greenOffsets(): List<Pair<Int, Int>> =
        listOf((-1 to 0), (1 to 0), (0 to -1), (0 to 1))

    private fun averageNeighbors(
        raw: FloatArray, x: Int, y: Int, w: Int, h: Int,
        offsets: List<Pair<Int, Int>>
    ): Float {
        var sum = 0f
        var count = 0
        for ((dx, dy) in offsets) {
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until w && ny in 0 until h) {
                sum += raw[ny * w + nx]; count++
            }
        }
        return if (count > 0) sum / count else 0f
    }

    private fun averageDiagonal(raw: FloatArray, x: Int, y: Int, w: Int, h: Int): Float {
        var sum = 0f; var count = 0
        for (dx in intArrayOf(-1, 1)) {
            for (dy in intArrayOf(-1, 1)) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until w && ny in 0 until h) {
                    sum += raw[ny * w + nx]; count++
                }
            }
        }
        return if (count > 0) sum / count else 0f
    }

    private fun averageHorizontal(raw: FloatArray, x: Int, y: Int, w: Int, h: Int): Float {
        var sum = 0f; var count = 0
        if (x > 0) { sum += raw[y * w + x - 1]; count++ }
        if (x < w - 1) { sum += raw[y * w + x + 1]; count++ }
        return if (count > 0) sum / count else 0f
    }

    private fun averageVertical(raw: FloatArray, x: Int, y: Int, w: Int, h: Int): Float {
        var sum = 0f; var count = 0
        if (y > 0) { sum += raw[(y - 1) * w + x]; count++ }
        if (y < h - 1) { sum += raw[(y + 1) * w + x]; count++ }
        return if (count > 0) sum / count else 0f
    }

    /**
     * 色彩校正矩阵应用
     * Camera RGB → sRGB (D65)
     */
    private fun applyColorCorrectionMatrix(rgb: FloatArray, width: Int, height: Int): FloatArray {
        val result = FloatArray(rgb.size)
        for (i in 0 until width * height) {
            val r = rgb[i * 3]
            val g = rgb[i * 3 + 1]
            val b = rgb[i * 3 + 2]

            // 应用 3x3 CCM
            result[i * 3] = (SRGB_D65_CCM[0] * r + SRGB_D65_CCM[1] * g + SRGB_D65_CCM[2] * b).coerceIn(0f, 1f)
            result[i * 3 + 1] = (SRGB_D65_CCM[3] * r + SRGB_D65_CCM[4] * g + SRGB_D65_CCM[5] * b).coerceIn(0f, 1f)
            result[i * 3 + 2] = (SRGB_D65_CCM[6] * r + SRGB_D65_CCM[7] * g + SRGB_D65_CCM[8] * b).coerceIn(0f, 1f)
        }
        return result
    }

    /**
     * 色调映射
     */
    private fun applyToneMapping(
        rgb: FloatArray, width: Int, height: Int,
        exposure: Float, mode: ToneMappingMode
    ): FloatArray {
        val result = FloatArray(rgb.size)
        for (i in 0 until width * height) {
            var r = rgb[i * 3] * exposure
            var g = rgb[i * 3 + 1] * exposure
            var b = rgb[i * 3 + 2] * exposure

            when (mode) {
                ToneMappingMode.CLAMP -> {
                    r = r.coerceIn(0f, 1f)
                    g = g.coerceIn(0f, 1f)
                    b = b.coerceIn(0f, 1f)
                }
                ToneMappingMode.REINHARD -> {
                    r = reinhard(r); g = reinhard(g); b = reinhard(b)
                }
                ToneMappingMode.FILMIC -> {
                    r = filmicToneMap(r); g = filmicToneMap(g); b = filmicToneMap(b)
                }
                ToneMappingMode.AGX -> {
                    r = agxToneMap(r); g = agxToneMap(g); b = agxToneMap(b)
                }
            }

            result[i * 3] = r
            result[i * 3 + 1] = g
            result[i * 3 + 2] = b
        }
        return result
    }

    // Reinhard 色调映射
    private fun reinhard(x: Float): Float = x / (1f + x)

    // Filmic 色调映射 (ACES 近似)
    private fun filmicToneMap(x: Float): Float {
        val a = 2.51f; val b = 0.03f
        val c = 2.43f; val d = 0.59f; val e = 0.14f
        return ((x * (a * x + b)) / (x * (c * x + d) + e)).coerceIn(0f, 1f)
    }

    // AgX 色调映射 (Blender AgX 近似)
    private fun agxToneMap(x: Float): Float {
        val agxInset = 0.38319f
        val agxOutset = 2.0f
        val agxMinEv = -12.0f
        val agxMaxEv = 4.0f

        // Log2 空间映射
        val ev = if (x > 0f) (max(x, 1e-10f).toDouble().pow(1.0/2.2) * 2.0 - 0.5).toFloat()
                 else 0f
        val evClamped = ev.coerceIn(agxMinEv, agxMaxEv)
        val t = (evClamped - agxMinEv) / (agxMaxEv - agxMinEv)
        return (t * agxOutset - agxInset).coerceIn(0f, 1f)
    }

    /**
     * RAW 数据 → Bitmap（伽马编码）
     */
    private fun rawToBitmap(rgb: FloatArray, width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height)
        for (i in 0 until width * height) {
            val r = (gammaEncode(rgb[i * 3]) * 255f).toInt().coerceIn(0, 255)
            val g = (gammaEncode(rgb[i * 3 + 1]) * 255f).toInt().coerceIn(0, 255)
            val b = (gammaEncode(rgb[i * 3 + 2]) * 255f).toInt().coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    // sRGB 伽马编码
    private fun gammaEncode(linear: Float): Float =
        if (linear <= 0.0031308f) 12.92f * linear
        else 1.055f * linear.pow(1f / 2.4f) - 0.055f

    /**
     * 色调映射模式
     */
    enum class ToneMappingMode {
        CLAMP,       // 硬裁剪
        REINHARD,    // Reinhard
        FILMIC,      // ACES Filmic
        AGX          // Blender AgX
    }
}
