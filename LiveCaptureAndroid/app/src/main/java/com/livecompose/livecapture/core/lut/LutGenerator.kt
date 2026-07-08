package com.livecompose.livecapture.core.lut

import kotlin.math.max
import kotlin.math.min

/**
 * RBF 插值 LUT 生成器
 * 使用带仿射多项式的 Cubic RBF 核函数，从稀疏控制点平滑扩展到完整 33³ 色彩查找表
 * 参考 PhotonCamera LutGenerator 实现
 */
object LutGenerator {

    private const val LUT_SIZE = 33
    private const val REGULARIZATION = 1e-4f

    /**
     * 从 LutRecipe 生成 33³ 的 3D LUT 浮点数组
     * 返回 FloatArray[LUT_SIZE³ * 3]，布局为 [R0,G0,B0, R1,G1,B1, ...]
     */
    fun generateLut(recipe: LutRecipe): FloatArray {
        val points = recipe.controlPoints
        require(points.size >= 4) { "至少需要4个控制点" }

        val n = points.size
        val size = LUT_SIZE * LUT_SIZE * LUT_SIZE * 3
        val lut = FloatArray(size)

        // 构建 RBF 系统: 对 R/G/B 各通道分别求解
        val channels = Array(3) { ch ->
            // 构建矩阵系统 (N+4) × (N+4)
            val dim = n + 4
            val matrix = Array(dim) { FloatArray(dim) }
            val rhs = FloatArray(dim)

            // RBF 核矩阵 M
            for (i in 0 until n) {
                for (j in 0 until n) {
                    val dist = cubicRbf(points[i].sourceArray(), points[j].sourceArray())
                    matrix[i][j] = dist
                }
                matrix[i][i] += REGULARIZATION
            }

            // 多项式块 P
            for (i in 0 until n) {
                matrix[i][n] = 1f
                matrix[i][n + 1] = points[i].sourceR
                matrix[i][n + 2] = points[i].sourceG
                matrix[i][n + 3] = points[i].sourceB
                matrix[n][i] = 1f
                matrix[n + 1][i] = points[i].sourceR
                matrix[n + 2][i] = points[i].sourceG
                matrix[n + 3][i] = points[i].sourceB
            }

            // 右端项: 残差 = Target - Source
            val targetCh = when (ch) {
                0 -> points.map { it.targetR - it.sourceR }
                1 -> points.map { it.targetG - it.sourceG }
                else -> points.map { it.targetB - it.sourceB }
            }
            for (i in 0 until n) {
                rhs[i] = targetCh[i]
            }

            // 高斯消元求解
            solveLinearSystem(matrix, rhs, dim)
        }

        // 生成 LUT
        var idx = 0
        for (b in 0 until LUT_SIZE) {
            for (g in 0 until LUT_SIZE) {
                for (r in 0 until LUT_SIZE) {
                    val rf = r.toFloat() / (LUT_SIZE - 1)
                    val gf = g.toFloat() / (LUT_SIZE - 1)
                    val bf = b.toFloat() / (LUT_SIZE - 1)

                    val input = floatArrayOf(rf, gf, bf)
                    for (ch in 0..2) {
                        var value = input[ch] // 仿射基底从 identity 开始

                        // 多项式部分
                        val n = points.size
                        value += channels[ch][n] // c0
                        value += channels[ch][n + 1] * rf // c1*R
                        value += channels[ch][n + 2] * gf // c2*G
                        value += channels[ch][n + 3] * bf // c3*B

                        // RBF 贡献
                        for (i in 0 until n) {
                            val dist = cubicRbf(input, points[i].sourceArray())
                            value += channels[ch][i] * dist
                        }

                        lut[idx + ch] = value.coerceIn(0f, 1f)
                    }
                    idx += 3
                }
            }
        }

        // 强制单调性
        enforceMonotonicity(lut)

        return lut
    }

    /**
     * 将 LUT 导出为 .cube 格式文本
     */
    fun exportToCubeString(lut: FloatArray, name: String = "Custom"): String {
        val sb = StringBuilder()
        sb.appendLine("TITLE \"$name\"")
        sb.appendLine("DOMAIN_MIN 0.0 0.0 0.0")
        sb.appendLine("DOMAIN_MAX 1.0 1.0 1.0")
        sb.appendLine("LUT_3D_SIZE $LUT_SIZE")
        var idx = 0
        for (b in 0 until LUT_SIZE) {
            for (g in 0 until LUT_SIZE) {
                for (r in 0 until LUT_SIZE) {
                    sb.appendLine("%.6f %.6f %.6f".format(lut[idx], lut[idx + 1], lut[idx + 2]))
                    idx += 3
                }
            }
        }
        return sb.toString()
    }

    /**
     * 应用 3D LUT 到像素数组
     * @param pixels ARGB 像素数组
     * @param lut 3D LUT 浮点数组
     * @param intensity 应用强度 [0,1]
     */
    fun applyLutToPixels(pixels: IntArray, lut: FloatArray, intensity: Float = 1f): IntArray {
        val result = IntArray(pixels.size)
        val sizeMinus1 = LUT_SIZE - 1

        for (i in pixels.indices) {
            val argb = pixels[i]
            val r0 = ((argb shr 16) and 0xFF) / 255f
            val g0 = ((argb shr 8) and 0xFF) / 255f
            val b0 = (argb and 0xFF) / 255f

            // 三线性插值
            val rIdx = r0 * sizeMinus1
            val gIdx = g0 * sizeMinus1
            val bIdx = b0 * sizeMinus1

            val r0i = rIdx.toInt().coerceIn(0, sizeMinus1 - 1)
            val g0i = gIdx.toInt().coerceIn(0, sizeMinus1 - 1)
            val b0i = bIdx.toInt().coerceIn(0, sizeMinus1 - 1)
            val r1i = r0i + 1
            val g1i = g0i + 1
            val b1i = b0i + 1

            val rFrac = rIdx - r0i
            val gFrac = gIdx - g0i
            val bFrac = bIdx - b0i

            // 8 个角的 LUT 查找
            fun lutAt(ri: Int, gi: Int, bi: Int): FloatArray {
                val idx = (bi * LUT_SIZE * LUT_SIZE + gi * LUT_SIZE + ri) * 3
                return floatArrayOf(lut[idx], lut[idx + 1], lut[idx + 2])
            }

            val c000 = lutAt(r0i, g0i, b0i)
            val c100 = lutAt(r1i, g0i, b0i)
            val c010 = lutAt(r0i, g1i, b0i)
            val c110 = lutAt(r1i, g1i, b0i)
            val c001 = lutAt(r0i, g0i, b1i)
            val c101 = lutAt(r1i, g0i, b1i)
            val c011 = lutAt(r0i, g1i, b1i)
            val c111 = lutAt(r1i, g1i, b1i)

            // 三线性插值
            val rr = lerp(
                lerp(lerp(c000[0], c100[0], rFrac), lerp(c010[0], c110[0], rFrac), gFrac),
                lerp(lerp(c001[0], c101[0], rFrac), lerp(c011[0], c111[0], rFrac), gFrac),
                bFrac
            )
            val gg = lerp(
                lerp(lerp(c000[1], c100[1], rFrac), lerp(c010[1], c110[1], rFrac), gFrac),
                lerp(lerp(c001[1], c101[1], rFrac), lerp(c011[1], c111[1], rFrac), gFrac),
                bFrac
            )
            val bb = lerp(
                lerp(lerp(c000[2], c100[2], rFrac), lerp(c010[2], c110[2], rFrac), gFrac),
                lerp(lerp(c001[2], c101[2], rFrac), lerp(c011[2], c111[2], rFrac), gFrac),
                bFrac
            )

            // 混合原始值和 LUT 值
            val finalR = (lerp(r0, rr, intensity) * 255f).toInt().coerceIn(0, 255)
            val finalG = (lerp(g0, gg, intensity) * 255f).toInt().coerceIn(0, 255)
            val finalB = (lerp(b0, bb, intensity) * 255f).toInt().coerceIn(0, 255)

            result[i] = (argb and 0xFF000000.toInt()) or (finalR shl 16) or (finalG shl 8) or finalB
        }
        return result
    }

    // Cubic RBF 核函数
    private fun cubicRbf(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        val r = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        return r * r * r
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    // 高斯消元法 + 部分主元选取
    private fun solveLinearSystem(A: Array<FloatArray>, b: FloatArray, n: Int): FloatArray {
        // 增广矩阵
        val aug = Array(n) { FloatArray(n + 1) }
        for (i in 0 until n) {
            for (j in 0 until n) aug[i][j] = A[i][j]
            aug[i][n] = b[i]
        }

        // 前向消元
        for (col in 0 until n) {
            // 部分主元选取
            var maxRow = col
            var maxVal = kotlin.math.abs(aug[col][col])
            for (row in col + 1 until n) {
                val v = kotlin.math.abs(aug[row][col])
                if (v > maxVal) { maxVal = v; maxRow = row }
            }
            if (maxRow != col) {
                val tmp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = tmp
            }

            val pivot = aug[col][col]
            if (kotlin.math.abs(pivot) < 1e-10f) continue

            for (row in col + 1 until n) {
                val factor = aug[row][col] / pivot
                for (j in col..n) aug[row][j] -= factor * aug[col][j]
            }
        }

        // 回代
        val x = FloatArray(n)
        for (i in n - 1 downTo 0) {
            var sum = aug[i][n]
            for (j in i + 1 until n) sum -= aug[i][j] * x[j]
            x[i] = if (kotlin.math.abs(aug[i][i]) < 1e-10f) 0f else sum / aug[i][i]
        }
        return x
    }

    // 沿 R/G/B 轴强制输出单调递增
    private fun enforceMonotonicity(lut: FloatArray) {
        for (ch in 0..2) {
            // 沿 R 轴
            for (b in 0 until LUT_SIZE) {
                for (g in 0 until LUT_SIZE) {
                    var prev = 0f
                    for (r in 0 until LUT_SIZE) {
                        val idx = (b * LUT_SIZE * LUT_SIZE + g * LUT_SIZE + r) * 3 + ch
                        lut[idx] = max(lut[idx], prev)
                        prev = lut[idx]
                    }
                }
            }
            // 沿 G 轴
            for (b in 0 until LUT_SIZE) {
                for (r in 0 until LUT_SIZE) {
                    var prev = 0f
                    for (g in 0 until LUT_SIZE) {
                        val idx = (b * LUT_SIZE * LUT_SIZE + g * LUT_SIZE + r) * 3 + ch
                        lut[idx] = max(lut[idx], prev)
                        prev = lut[idx]
                    }
                }
            }
            // 沿 B 轴
            for (g in 0 until LUT_SIZE) {
                for (r in 0 until LUT_SIZE) {
                    var prev = 0f
                    for (b in 0 until LUT_SIZE) {
                        val idx = (b * LUT_SIZE * LUT_SIZE + g * LUT_SIZE + r) * 3 + ch
                        lut[idx] = max(lut[idx], prev)
                        prev = lut[idx]
                    }
                }
            }
        }
    }
}
