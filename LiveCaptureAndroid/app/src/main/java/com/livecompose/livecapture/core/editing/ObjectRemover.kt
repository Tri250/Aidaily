package com.livecompose.livecapture.core.editing

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 物体移除器
 *
 * 对应 iOS 端 ObjectRemover.swift，基于多尺度金字塔内容感知填充算法，
 * 将指定掩码区域用周围像素纹理填充，实现物体消除。
 *
 * ## 算法原理
 * 1. 扩展并羽化掩码边界
 * 2. 构建 4 级图像金字塔
 * 3. 在每层金字塔上，用不断扩大的窗口从有效邻居像素迭代填充掩码区域
 * 4. 将结果上采样到下一层并重复
 * 5. 在顶层对接缝进行边缘平滑
 *
 * ## 使用方式
 * ```
 * val remover = ObjectRemover()
 * val result = remover.removeObject(bitmap, RectF(0.3f, 0.3f, 0.6f, 0.6f))
 * ```
 */
class ObjectRemover {

    companion object {
        private const val TAG = "ObjectRemover"
        private const val PYRAMID_LEVELS = 4
        private const val MAX_FILL_WINDOW = 24
    }

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** 处理进度（0.0 - 1.0） */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    /**
     * 移除图像中指定区域的物体
     *
     * @param image 输入 Bitmap
     * @param maskRect 掩码矩形（归一化坐标 0..1）
     * @return 填充后的 Bitmap，失败返回原图
     */
    suspend fun removeObject(image: Bitmap, maskRect: RectF): Bitmap = withContext(Dispatchers.Default) {
        if (image.width <= 0 || image.height <= 0) {
            return@withContext image
        }
        // 归一化坐标转像素坐标
        val maskLeft = (maskRect.left.coerceIn(0f, 1f) * image.width).toInt()
        val maskTop = (maskRect.top.coerceIn(0f, 1f) * image.height).toInt()
        val maskRight = (maskRect.right.coerceIn(0f, 1f) * image.width).toInt()
        val maskBottom = (maskRect.bottom.coerceIn(0f, 1f) * image.height).toInt()

        if (maskRight <= maskLeft || maskBottom <= maskTop) {
            AppLogger.w(TAG, "掩码区域无效，返回原图")
            return@withContext image
        }

        _isProcessing.value = true
        _progress.value = 0f

        try {
            val result = processPyramidFill(image, maskLeft, maskTop, maskRight, maskBottom)
            _progress.value = 1.0f
            result
        } catch (e: Exception) {
            AppLogger.e(TAG, "物体移除失败", e)
            image
        } finally {
            _isProcessing.value = false
        }
    }

    /**
     * 多尺度金字塔内容感知填充
     */
    private suspend fun processPyramidFill(
        image: Bitmap,
        maskLeft: Int,
        maskTop: Int,
        maskRight: Int,
        maskBottom: Int
    ): Bitmap {
        // 1. 准备工作副本和掩码
        var currentBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
        val width = currentBitmap.width
        val height = currentBitmap.height

        // 扩展掩码（向外膨胀几个像素以覆盖边缘）
        val expandPx = (Math.min(width, height) * 0.02f).toInt().coerceAtLeast(2)
        val expandedLeft = (maskLeft - expandPx).coerceAtLeast(0)
        val expandedTop = (maskTop - expandPx).coerceAtLeast(0)
        val expandedRight = (maskRight + expandPx).coerceAtLeast(0).coerceAtMost(width)
        val expandedBottom = (maskBottom + expandPx).coerceAtLeast(0).coerceAtMost(height)

        // 2. 构建金字塔：从最底层（最小）开始填充
        val pyramidBitmaps = ArrayList<Bitmap>(PYRAMID_LEVELS)
        val pyramidMasks = ArrayList<BooleanArray>(PYRAMID_LEVELS)
        var tempBitmap = currentBitmap
        var tempMask = createMaskArray(width, height, expandedLeft, expandedTop, expandedRight, expandedBottom)

        pyramidBitmaps.add(tempBitmap)
        pyramidMasks.add(tempMask)

        for (level in 1 until PYRAMID_LEVELS) {
            val downsampled = downsample(tempBitmap)
            val downsampledMask = downsampleMask(tempMask, tempBitmap.width, tempBitmap.height, downsampled.width, downsampled.height)
            if (downsampled.width < 8 || downsampled.height < 8) break
            pyramidBitmaps.add(downsampled)
            pyramidMasks.add(downsampledMask)
            tempBitmap = downsampled
            tempMask = downsampledMask
        }

        val levels = pyramidBitmaps.size

        // 3. 从最小层开始迭代填充
        for (level in levels - 1 downTo 0) {
            val levelBitmap = pyramidBitmaps[level]
            val levelMask = pyramidMasks[level]
            val levelWidth = levelBitmap.width
            val levelHeight = levelBitmap.height

            iterativeFill(levelBitmap, levelMask, levelWidth, levelHeight)

            _progress.value = 0.2f + 0.6f * (levels - level).toFloat() / levels

            // 上采样到上一层并合并
            if (level > 0) {
                val upsampled = upsample(levelBitmap, pyramidBitmaps[level - 1].width, pyramidBitmaps[level - 1].height)
                mergeFillResult(pyramidBitmaps[level - 1], upsampled, pyramidMasks[level - 1])
            } else {
                // 最顶层即原始分辨率
                currentBitmap = levelBitmap
            }
        }

        // 4. 接缝边缘平滑
        val smoothed = smoothSeams(currentBitmap, expandedLeft, expandedTop, expandedRight, expandedBottom)

        // 清理金字塔中间位图
        for (i in 1 until levels) {
            pyramidBitmaps[i].recycle()
        }

        return smoothed
    }

    /**
     * 创建掩码数组（true = 需要填充）
     */
    private fun createMaskArray(
        width: Int, height: Int,
        maskLeft: Int, maskTop: Int, maskRight: Int, maskBottom: Int
    ): BooleanArray {
        val mask = BooleanArray(width * height)
        for (y in maskTop until maskBottom) {
            for (x in maskLeft until maskRight) {
                mask[y * width + x] = true
            }
        }
        return mask
    }

    /**
     * 下采样 Bitmap（2x 缩小，取平均）
     */
    private fun downsample(bitmap: Bitmap): Bitmap {
        val newWidth = bitmap.width / 2
        val newHeight = bitmap.height / 2
        if (newWidth < 1 || newHeight < 1) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val result = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        return result
    }

    /**
     * 下采样掩码
     */
    private fun downsampleMask(
        mask: BooleanArray, srcWidth: Int, srcHeight: Int,
        dstWidth: Int, dstHeight: Int
    ): BooleanArray {
        val result = BooleanArray(dstWidth * dstHeight)
        for (y in 0 until dstHeight) {
            for (x in 0 until dstWidth) {
                val sx = x * 2
                val sy = y * 2
                // 只要 2x2 块中有任意一个为 true，则下采样后为 true
                var anyTrue = false
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        val px = (sx + dx).coerceAtMost(srcWidth - 1)
                        val py = (sy + dy).coerceAtMost(srcHeight - 1)
                        if (mask[py * srcWidth + px]) {
                            anyTrue = true
                            break
                        }
                    }
                    if (anyTrue) break
                }
                result[y * dstWidth + x] = anyTrue
            }
        }
        return result
    }

    /**
     * 上采样 Bitmap 到指定尺寸
     */
    private fun upsample(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 迭代填充：用不断扩大的窗口从有效邻居像素填充掩码区域
     */
    private fun iterativeFill(bitmap: Bitmap, mask: BooleanArray, width: Int, height: Int) {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var hasMasked = true
        var windowRadius = 1

        while (hasMasked && windowRadius <= MAX_FILL_WINDOW) {
            hasMasked = false
            val newPixels = pixels.copyOf()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (!mask[y * width + x]) continue

                    // 收集窗口内有效邻居
                    var rSum = 0L
                    var gSum = 0L
                    var bSum = 0L
                    var count = 0

                    for (dy in -windowRadius..windowRadius) {
                        val ny = y + dy
                        if (ny < 0 || ny >= height) continue
                        for (dx in -windowRadius..windowRadius) {
                            val nx = x + dx
                            if (nx < 0 || nx >= width) continue
                            if (mask[ny * width + nx]) continue // 跳过仍需填充的像素
                            val c = pixels[ny * width + nx]
                            rSum += Color.red(c)
                            gSum += Color.green(c)
                            bSum += Color.blue(c)
                            count++
                        }
                    }

                    if (count > 0) {
                        // 加权平均：距离越近权重越大
                        newPixels[y * width + x] = Color.rgb(
                            (rSum / count).toInt().coerceIn(0, 255),
                            (gSum / count).toInt().coerceIn(0, 255),
                            (bSum / count).toInt().coerceIn(0, 255)
                        )
                        // 标记为已填充（本回合不再需要处理）
                        mask[y * width + x] = false
                    } else {
                        hasMasked = true
                    }
                }
            }

            // 复制回 pixels
            newPixels.copyInto(pixels)
            windowRadius *= 2
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * 将上采样结果合并到上一层（仅填充掩码区域）
     */
    private fun mergeFillResult(target: Bitmap, upsampled: Bitmap, mask: BooleanArray) {
        val width = target.width
        val height = target.height
        val targetPixels = IntArray(width * height)
        target.getPixels(targetPixels, 0, width, 0, 0, width, height)
        val upsampledPixels = IntArray(width * height)
        upsampled.getPixels(upsampledPixels, 0, width, 0, 0, width, height)

        for (i in mask.indices) {
            if (mask[i]) {
                targetPixels[i] = upsampledPixels[i]
            }
        }
        target.setPixels(targetPixels, 0, width, 0, 0, width, height)
    }

    /**
     * 接缝边缘平滑：在掩码边界周围应用盒式模糊以消除填充痕迹
     */
    private fun smoothSeams(
        bitmap: Bitmap,
        maskLeft: Int, maskTop: Int, maskRight: Int, maskBottom: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val seamWidth = (Math.min(width, height) * 0.03f).toInt().coerceAtLeast(4).coerceAtMost(20)

        // 创建接缝掩码（边界附近为 true）
        val seamMask = BooleanArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val distLeft = Math.abs(x - maskLeft)
                val distRight = Math.abs(x - maskRight)
                val distTop = Math.abs(y - maskTop)
                val distBottom = Math.abs(y - maskBottom)

                // 仅在掩码边界附近的环形区域进行平滑
                val inHorizontalBand = (y in maskTop..maskBottom) &&
                    (distLeft < seamWidth || distRight < seamWidth)
                val inVerticalBand = (x in maskLeft..maskRight) &&
                    (distTop < seamWidth || distBottom < seamWidth)
                // 掩码内部也平滑
                val insideMask = x in maskLeft..maskRight && y in maskTop..maskBottom

                seamMask[y * width + x] = inHorizontalBand || inVerticalBand || insideMask
            }
        }

        // 对整图做盒式模糊
        val blurred = boxBlur(bitmap, seamWidth / 2)

        // 混合：接缝区域用模糊结果，其他区域保留原图
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val origPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)
        blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(width * height)
        for (i in seamMask.indices) {
            resultPixels[i] = if (seamMask[i]) blurPixels[i] else origPixels[i]
        }
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)

        blurred.recycle()
        return result
    }

    /**
     * 盒式模糊（近似高斯）
     */
    private fun boxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        val temp = pixels.copyOf()
        val r = radius.coerceAtLeast(1)

        // 水平方向
        for (y in 0 until height) {
            for (x in 0 until width) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (kx in -r..r) {
                    val px = (x + kx).coerceIn(0, width - 1)
                    val c = temp[y * width + px]
                    rSum += Color.red(c); gSum += Color.green(c); bSum += Color.blue(c)
                    count++
                }
                pixels[y * width + x] = Color.rgb(rSum / count, gSum / count, bSum / count)
            }
        }
        // 垂直方向
        temp.copyInto(pixels)
        for (x in 0 until width) {
            for (y in 0 until height) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (ky in -r..r) {
                    val py = (y + ky).coerceIn(0, height - 1)
                    val c = pixels[py * width + x]
                    rSum += Color.red(c); gSum += Color.green(c); bSum += Color.blue(c)
                    count++
                }
                temp[y * width + x] = Color.rgb(rSum / count, gSum / count, bSum / count)
            }
        }
        result.setPixels(temp, 0, width, 0, 0, width, height)
        return result
    }
}
