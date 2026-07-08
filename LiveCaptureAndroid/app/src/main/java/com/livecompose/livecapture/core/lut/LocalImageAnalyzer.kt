package com.livecompose.livecapture.core.lut

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 本地色彩分析器
 * 从源图像和目标图像提取 Source→Target 色彩映射控制点
 * 算法：Oklab K-Means 聚类 + 归一化块匹配
 */
object LocalImageAnalyzer {

    private const val K_MEANS_K = 16
    private const val K_MEANS_ITERATIONS = 15
    private const val MAX_SAMPLE_PIXELS = 96
    private const val PATCH_RADIUS = 6
    private const val PATCH_MATCH_THRESHOLD = 0.28f
    private const val DISTANCE_PENALTY = 0.015f
    private const val OKLAB_ANOMALY_THRESHOLD = 0.45f

    /**
     * 分析源图像和目标图像，提取 LutRecipe 控制点
     * 适用于"有原图+风格图"的场景
     */
    fun analyzeSourceTargetImages(
        source: Bitmap,
        target: Bitmap,
        recipeName: String = "AutoMatch"
    ): LutRecipe {
        val controlPoints = extractSourceTargetControlPoints(source, target)
        val filtered = filterAndValidate(controlPoints)
        return LutRecipe(filtered, recipeName)
    }

    /**
     * 从单张风格照片生成 LutRecipe
     * 基于灰世界假设 + 对比度归一化推算"原图"
     */
    fun analyzeSingleStyleImage(
        styleImage: Bitmap,
        recipeName: String = "StyleMatch"
    ): LutRecipe {
        // 缩放以减少计算量
        val maxDim = 256
        val scale = min(maxDim.toFloat() / styleImage.width, maxDim.toFloat() / styleImage.height)
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(styleImage,
            (styleImage.width * scale).toInt(), (styleImage.height * scale).toInt(), true)
        else styleImage

        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        // 在 Oklab 空间做 K-Means 聚类
        val clusters = kMeansInOklab(pixels, width, height)

        // 对每个聚类，基于灰世界假设生成控制点
        val controlPoints = mutableListOf<ControlPoint>()

        for (cluster in clusters) {
            if (cluster.pixelCount < 10) continue

            val avgR = cluster.sumR / cluster.pixelCount
            val avgG = cluster.sumG / cluster.pixelCount
            val avgB = cluster.sumB / cluster.pixelCount

            // 目标色（风格照实际颜色）
            val targetR = avgR / 255f
            val targetG = avgG / 255f
            val targetB = avgB / 255f

            // 推算源色：灰世界假设 + 对比度归一化
            val grayR = sqrt(avgR / 255f)
            val grayG = sqrt(avgG / 255f)
            val grayB = sqrt(avgB / 255f)
            val grayAvg = (grayR + grayG + grayB) / 3f

            val sourceR = (grayAvg * 0.3f + targetR * 0.7f).coerceIn(0f, 1f)
            val sourceG = (grayAvg * 0.3f + targetG * 0.7f).coerceIn(0f, 1f)
            val sourceB = (grayAvg * 0.3f + targetB * 0.7f).coerceIn(0f, 1f)

            controlPoints.add(ControlPoint(
                sourceR, sourceG, sourceB,
                targetR, targetG, targetB,
                confidence = 0.6f
            ))
        }

        // 添加灰阶锚点（7 级）
        for (i in 0..6) {
            val v = i / 6f
            controlPoints.add(ControlPoint(v, v, v, v, v, v, confidence = 0.3f))
        }

        // 添加 RGB 角落锚点
        val corners = listOf(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 1f, 1f),
            floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f), floatArrayOf(1f, 1f, 0f),
            floatArrayOf(1f, 0f, 1f), floatArrayOf(0f, 1f, 1f)
        )
        for (c in corners) {
            controlPoints.add(ControlPoint(c[0], c[1], c[2], c[0], c[1], c[2], confidence = 0.2f))
        }

        val filtered = filterAndValidate(controlPoints)
        return LutRecipe(filtered, recipeName)
    }

    /**
     * 从源图像和目标图像提取控制点（图像对分析）
     */
    private fun extractSourceTargetControlPoints(
        source: Bitmap,
        target: Bitmap
    ): List<ControlPoint> {
        val maxDim = 256
        val srcScaled = scaleBitmap(source, maxDim)
        val tgtScaled = scaleBitmap(target, maxDim)

        val srcW = srcScaled.width; val srcH = srcScaled.height
        val srcPixels = IntArray(srcW * srcH)
        srcScaled.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH)

        val tgtW = tgtScaled.width; val tgtH = tgtScaled.height
        val tgtPixels = IntArray(tgtW * tgtH)
        tgtScaled.getPixels(tgtPixels, 0, tgtW, 0, 0, tgtW, tgtH)

        // K-Means 聚类源图像
        val clusters = kMeansInOklab(srcPixels, srcW, srcH)
        val controlPoints = mutableListOf<ControlPoint>()

        // 对每个聚类，在目标图中块匹配寻找目标颜色
        for (cluster in clusters) {
            if (cluster.pixelCount < 5) continue

            val samplePixels = cluster.pixelIndices.shuffled().take(MAX_SAMPLE_PIXELS)
            val matchColors = mutableListOf<FloatArray>()
            var totalMatchQuality = 0f
            var matchCount = 0

            for (srcIdx in samplePixels) {
                val sx = srcIdx % srcW
                val sy = srcIdx / srcW

                // 映射到目标图坐标
                val tx = (sx.toFloat() / srcW * tgtW).toInt()
                val ty = (sy.toFloat() / srcH * tgtH).toInt()

                val bestMatch = findBestPatchMatch(
                    srcPixels, srcW, srcH, sx, sy,
                    tgtPixels, tgtW, tgtH, tx, ty
                )

                if (bestMatch != null) {
                    matchColors.add(bestMatch)
                    totalMatchQuality += bestMatch[3]
                    matchCount++
                }
            }

            if (matchCount < 3) continue

            // 取匹配目标像素的中值 RGB
            val avgSourceR = cluster.sumR / cluster.pixelCount / 255f
            val avgSourceG = cluster.sumG / cluster.pixelCount / 255f
            val avgSourceB = cluster.sumB / cluster.pixelCount / 255f

            val sortedR = matchColors.map { it[0] }.sorted()
            val sortedG = matchColors.map { it[1] }.sorted()
            val sortedB = matchColors.map { it[2] }.sorted()
            val mid = sortedR.size / 2

            val confidence = (totalMatchQuality / matchCount) * (matchCount.toFloat() / samplePixels.size)

            controlPoints.add(ControlPoint(
                avgSourceR, avgSourceG, avgSourceB,
                sortedR[mid], sortedG[mid], sortedB[mid],
                confidence = confidence.coerceIn(0f, 1f)
            ))
        }

        // 灰阶采样
        addGrayscaleAnchors(srcPixels, srcW, srcH, tgtPixels, tgtW, tgtH, controlPoints)

        return controlPoints
    }

    /**
     * 归一化块匹配：在目标图中搜索最佳匹配位置
     * 返回 [R, G, B, matchQuality] 或 null
     */
    private fun findBestPatchMatch(
        srcPixels: IntArray, srcW: Int, srcH: Int, srcX: Int, srcY: Int,
        tgtPixels: IntArray, tgtW: Int, tgtH: Int, centerTx: Int, centerTy: Int
    ): FloatArray? {
        val radius = PATCH_RADIUS
        var bestScore = Float.MAX_VALUE
        var bestTx = centerTx
        var bestTy = centerTy

        // 源 patch 的亮度统计
        val srcPatchLuma = extractPatchLuma(srcPixels, srcW, srcH, srcX, srcY, 1)
        val srcMean = srcPatchLuma.first
        val srcStd = srcPatchLuma.second

        // 在目标图中搜索
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val tx = (centerTx + dx).coerceIn(0, tgtW - 1)
                val ty = (centerTy + dy).coerceIn(0, tgtH - 1)

                val tgtPatchLuma = extractPatchLuma(tgtPixels, tgtW, tgtH, tx, ty, 1)
                val tgtMean = tgtPatchLuma.first
                val tgtStd = tgtPatchLuma.second

                // 归一化互相关 (NCC)
                val ncc = if (srcStd > 0.01f && tgtStd > 0.01f) {
                    abs(srcMean - tgtMean) / (srcStd + tgtStd + 1e-6f)
                } else {
                    abs(srcMean - tgtMean)
                }

                // 距离惩罚
                val manhattanDist = abs(dx) + abs(dy)
                val score = ncc + manhattanDist * DISTANCE_PENALTY

                if (score < bestScore) {
                    bestScore = score
                    bestTx = tx
                    bestTy = ty
                }
            }
        }

        if (bestScore > PATCH_MATCH_THRESHOLD) return null

        val tgtPixel = tgtPixels[bestTy * tgtW + bestTx]
        val r = ((tgtPixel shr 16) and 0xFF) / 255f
        val g = ((tgtPixel shr 8) and 0xFF) / 255f
        val b = (tgtPixel and 0xFF) / 255f

        return floatArrayOf(r, g, b, 1f - bestScore / PATCH_MATCH_THRESHOLD)
    }

    private fun extractPatchLuma(
        pixels: IntArray, w: Int, h: Int, cx: Int, cy: Int, radius: Int
    ): Pair<Float, Float> {
        var sum = 0f
        var sumSq = 0f
        var count = 0

        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = (cx + dx).coerceIn(0, w - 1)
                val y = (cy + dy).coerceIn(0, h - 1)
                val pixel = pixels[y * w + x]
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                sum += luma
                sumSq += luma * luma
                count++
            }
        }

        val mean = sum / count
        val variance = sumSq / count - mean * mean
        return Pair(mean, sqrt(variance.coerceAtLeast(0f)))
    }

    /**
     * 在 Oklab 空间进行 K-Means 聚类
     */
    private fun kMeansInOklab(pixels: IntArray, width: Int, height: Int): List<PixelCluster> {
        val n = pixels.size
        val oklabData = Array(n) { i ->
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            val lab = OklchConverter.linearRgbToOklab(
                OklchConverter.srgbToLinear(r),
                OklchConverter.srgbToLinear(g),
                OklchConverter.srgbToLinear(b)
            )
            PixelData(i, lab[0], lab[1], lab[2], r, g, b)
        }

        // 均匀初始化质心
        val centroids = Array(K_MEANS_K) { k ->
            val idx = (k * n / K_MEANS_K).coerceIn(0, n - 1)
            floatArrayOf(oklabData[idx].L, oklabData[idx].a, oklabData[idx].b)
        }

        val assignments = IntArray(n)

        // 迭代
        for (iter in 0 until K_MEANS_ITERATIONS) {
            // 分配
            for (i in 0 until n) {
                var bestCluster = 0
                var bestDist = Float.MAX_VALUE
                for (k in 0 until K_MEANS_K) {
                    val dL = oklabData[i].L - centroids[k][0]
                    val da = oklabData[i].a - centroids[k][1]
                    val db = oklabData[i].b - centroids[k][2]
                    val dist = dL * dL + da * da + db * db
                    if (dist < bestDist) { bestDist = dist; bestCluster = k }
                }
                assignments[i] = bestCluster
            }

            // 更新质心
            val sums = Array(K_MEANS_K) { FloatArray(3) }
            val counts = IntArray(K_MEANS_K)
            for (i in 0 until n) {
                val k = assignments[i]
                sums[k][0] += oklabData[i].L
                sums[k][1] += oklabData[i].a
                sums[k][2] += oklabData[i].b
                counts[k]++
            }
            for (k in 0 until K_MEANS_K) {
                if (counts[k] > 0) {
                    centroids[k][0] = sums[k][0] / counts[k]
                    centroids[k][1] = sums[k][1] / counts[k]
                    centroids[k][2] = sums[k][2] / counts[k]
                }
            }
        }

        // 构建聚类结果
        return (0 until K_MEANS_K).map { k ->
            val members = oklabData.filterIndexed { i, _ -> assignments[i] == k }
            PixelCluster(
                pixelIndices = members.map { it.index },
                pixelCount = members.size,
                sumR = members.sumOf { (it.r * 255f).toDouble() }.toFloat(),
                sumG = members.sumOf { (it.g * 255f).toDouble() }.toFloat(),
                sumB = members.sumOf { (it.b * 255f).toDouble() }.toFloat(),
                centroidL = centroids[k][0],
                centroidA = centroids[k][1],
                centroidB = centroids[k][2]
            )
        }
    }

    /**
     * 添加灰阶锚点控制点
     */
    private fun addGrayscaleAnchors(
        srcPixels: IntArray, srcW: Int, srcH: Int,
        tgtPixels: IntArray, tgtW: Int, tgtH: Int,
        controlPoints: MutableList<ControlPoint>
    ) {
        for (i in 0..6) {
            val targetLuma = i / 6f
            val nearNeutralPixels = mutableListOf<Int>()

            for (idx in srcPixels.indices) {
                val p = srcPixels[idx]
                val r = ((p shr 16) and 0xFF) / 255f
                val g = ((p shr 8) and 0xFF) / 255f
                val b = (p and 0xFF) / 255f

                val linR = OklchConverter.srgbToLinear(r)
                val linG = OklchConverter.srgbToLinear(g)
                val linB = OklchConverter.srgbToLinear(b)
                val lab = OklchConverter.linearRgbToOklab(linR, linG, linB)

                // 检查是否为近中性像素
                val chromaSq = lab[1] * lab[1] + lab[2] * lab[2]
                if (chromaSq < 0.001f && abs(lab[0] - targetLuma) < 0.1f) {
                    nearNeutralPixels.add(idx)
                }
            }

            if (nearNeutralPixels.size >= 10) {
                // 在目标图中匹配
                val srcIdx = nearNeutralPixels[nearNeutralPixels.size / 2]
                val sx = srcIdx % srcW
                val sy = srcIdx / srcW
                val tx = (sx.toFloat() / srcW * tgtW).toInt()
                val ty = (sy.toFloat() / srcH * tgtH).toInt()

                if (tx < tgtW && ty < tgtH) {
                    val tgtPixel = tgtPixels[ty * tgtW + tx]
                    val tr = ((tgtPixel shr 16) and 0xFF) / 255f
                    val tg = ((tgtPixel shr 8) and 0xFF) / 255f
                    val tb = (tgtPixel and 0xFF) / 255f

                    controlPoints.add(ControlPoint(
                        targetLuma, targetLuma, targetLuma,
                        tr, tg, tb,
                        confidence = 0.5f
                    ))
                }
            } else {
                // 恒等映射锚点
                controlPoints.add(ControlPoint(
                    targetLuma, targetLuma, targetLuma,
                    targetLuma, targetLuma, targetLuma,
                    confidence = 0.2f
                ))
            }
        }
    }

    /**
     * 过滤和验证控制点
     */
    private fun filterAndValidate(controlPoints: List<ControlPoint>): List<ControlPoint> {
        var points = controlPoints.toMutableList()

        // 1. Oklab 异常过滤
        points = points.filter { cp ->
            val srcLab = OklchConverter.linearRgbToOklab(
                OklchConverter.srgbToLinear(cp.sourceR),
                OklchConverter.srgbToLinear(cp.sourceG),
                OklchConverter.srgbToLinear(cp.sourceB)
            )
            val tgtLab = OklchConverter.linearRgbToOklab(
                OklchConverter.srgbToLinear(cp.targetR),
                OklchConverter.srgbToLinear(cp.targetG),
                OklchConverter.srgbToLinear(cp.targetB)
            )
            val dL = srcLab[0] - tgtLab[0]
            val da = srcLab[1] - tgtLab[1]
            val db = srcLab[2] - tgtLab[2]
            sqrt(dL * dL + da * da + db * db) <= OKLAB_ANOMALY_THRESHOLD
        }.toMutableList()

        // 2. 合并近距离控制点
        val merged = mutableListOf<ControlPoint>()
        val used = BooleanArray(points.size)
        for (i in points.indices) {
            if (used[i]) continue
            var group = listOf(points[i])
            for (j in i + 1 until points.size) {
                if (used[j]) continue
                val dR = points[i].sourceR - points[j].sourceR
                val dG = points[i].sourceG - points[j].sourceG
                val dB = points[i].sourceB - points[j].sourceB
                if (sqrt(dR * dR + dG * dG + dB * dB) < 0.05f) {
                    group = group + points[j]
                    used[j] = true
                }
            }
            used[i] = true
            // 加权平均
            val totalConf = group.sumOf { it.confidence.toDouble() }.toFloat()
            merged.add(ControlPoint(
                sourceR = group.sumOf { (it.sourceR * it.confidence).toDouble() }.toFloat() / totalConf,
                sourceG = group.sumOf { (it.sourceG * it.confidence).toDouble() }.toFloat() / totalConf,
                sourceB = group.sumOf { (it.sourceB * it.confidence).toDouble() }.toFloat() / totalConf,
                targetR = group.sumOf { (it.targetR * it.confidence).toDouble() }.toFloat() / totalConf,
                targetG = group.sumOf { (it.targetG * it.confidence).toDouble() }.toFloat() / totalConf,
                targetB = group.sumOf { (it.targetB * it.confidence).toDouble() }.toFloat() / totalConf,
                confidence = totalConf / group.size
            ))
        }

        return merged
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val scale = min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap,
                (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
    }

    private data class PixelData(
        val index: Int,
        val L: Float, val a: Float, val b: Float,
        val r: Float, val g: Float, val bVal: Float
    )

    private data class PixelCluster(
        val pixelIndices: List<Int>,
        val pixelCount: Int,
        val sumR: Float, val sumG: Float, val sumB: Float,
        val centroidL: Float, val centroidA: Float, val centroidB: Float
    )
}
