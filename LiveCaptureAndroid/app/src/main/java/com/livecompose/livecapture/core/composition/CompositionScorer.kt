package com.livecompose.livecapture.core.composition

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 构图评分引擎
 *
 * 对应 iOS 端 CompositionScorer.swift，基于多维度构图规则对裁切区域进行综合评分，
 * 结合人脸检测、裁剪框位置和设备倾斜数据，生成 0-100 分制的评分和中文改进建议。
 *
 * ## 评分维度
 * - 三分法 (ruleOfThirds): 主体中心是否接近三分点
 * - 平衡性 (balance): 人脸在裁切区域内的分布均匀度
 * - 居中 (centering): 主体是否在画面中心区域
 * - 水平线 (horizonLevel): 设备是否水平
 *
 * ## 综合评分权重
 * - 有人脸时: 三分法 35% + 平衡性 35% + 居中 20% + 水平线 10%
 * - 无人脸时: 三分法 50% + 水平线 30% + 居中 20%
 */
class CompositionScorer {

    // MARK: - 评分权重

    /** 有人脸时的权重 */
    private object FaceWeights {
        const val RULE_OF_THIRDS = 0.35f
        const val BALANCE = 0.35f
        const val CENTERING = 0.20f
        const val HORIZON = 0.10f
    }

    /** 无人脸时的权重 */
    private object NoFaceWeights {
        const val RULE_OF_THIRDS = 0.50f
        const val HORIZON = 0.30f
        const val CENTERING = 0.20f
    }

    // MARK: - Public API

    /**
     * 对裁切区域进行综合构图评分
     *
     * @param cropRect 裁切矩形（图像像素坐标）
     * @param imageWidth 原始图像宽度（像素）
     * @param imageHeight 原始图像高度（像素）
     * @param faces 人脸边界框列表（归一化坐标 0..1，左上为原点，ML Kit 约定）
     * @param horizonAngle 设备倾斜角度（度数）
     * @return 综合构图评分
     */
    fun scoreComposition(
        cropRect: RectF,
        imageWidth: Int,
        imageHeight: Int,
        faces: List<RectF>,
        horizonAngle: Float
    ): CompositionScore {
        // 将裁切中心转换为归一化坐标
        val cropCenterX = if (imageWidth > 0) cropRect.centerX() / imageWidth else 0.5f
        val cropCenterY = if (imageHeight > 0) cropRect.centerY() / imageHeight else 0.5f

        val ruleOfThirdsScore = scoreRuleOfThirds(
            faces = faces,
            cropCenterX = cropCenterX,
            cropCenterY = cropCenterY
        )
        val balanceScore = scoreBalance(
            faces = faces,
            cropCenterX = cropCenterX,
            cropCenterY = cropCenterY
        )
        val centeringScore = scoreCentering(
            faces = faces,
            cropCenterX = cropCenterX,
            cropCenterY = cropCenterY
        )
        val horizonScore = scoreHorizonLevel(horizonAngle)

        val hasFaces = faces.isNotEmpty()
        val overall: Float = if (hasFaces) {
            ruleOfThirdsScore * FaceWeights.RULE_OF_THIRDS +
                balanceScore * FaceWeights.BALANCE +
                centeringScore * FaceWeights.CENTERING +
                horizonScore * FaceWeights.HORIZON
        } else {
            ruleOfThirdsScore * NoFaceWeights.RULE_OF_THIRDS +
                horizonScore * NoFaceWeights.HORIZON +
                centeringScore * NoFaceWeights.CENTERING
        }

        val feedback = generateFeedback(
            ruleOfThirds = ruleOfThirdsScore,
            balance = balanceScore,
            centering = centeringScore,
            horizon = horizonScore,
            hasFaces = hasFaces
        )

        return CompositionScore(
            overall = overall.coerceIn(0f, 100f).roundToInt(),
            ruleOfThirds = ruleOfThirdsScore.coerceIn(0f, 100f).roundToInt(),
            balance = balanceScore.coerceIn(0f, 100f).roundToInt(),
            centering = centeringScore.coerceIn(0f, 100f).roundToInt(),
            horizonLevel = horizonScore.coerceIn(0f, 100f).roundToInt(),
            feedback = feedback
        )
    }

    // MARK: - 三分法评分

    /**
     * 计算三分法构图得分
     *
     * 主体中心（人脸群重心或裁切中心）越接近四个三分点，得分越高。
     * 距离 0 = 100 分，距离 0.3 及以上 = 0 分。
     */
    private fun scoreRuleOfThirds(
        faces: List<RectF>,
        cropCenterX: Float,
        cropCenterY: Float
    ): Float {
        // 确定主体中心点（归一化坐标）
        val subjectX: Float
        val subjectY: Float
        if (faces.isNotEmpty()) {
            var sumX = 0f
            var sumY = 0f
            for (face in faces) {
                sumX += face.centerX()
                sumY += face.centerY()
            }
            subjectX = sumX / faces.size
            subjectY = sumY / faces.size
        } else {
            subjectX = cropCenterX
            subjectY = cropCenterY
        }

        // 四个三分点
        val thirdPoints = arrayOf(
            floatArrayOf(1f / 3f, 1f / 3f),
            floatArrayOf(2f / 3f, 1f / 3f),
            floatArrayOf(1f / 3f, 2f / 3f),
            floatArrayOf(2f / 3f, 2f / 3f)
        )

        var minDistance = Float.MAX_VALUE
        for (point in thirdPoints) {
            val dx = subjectX - point[0]
            val dy = subjectY - point[1]
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < minDistance) minDistance = distance
        }
        if (minDistance == Float.MAX_VALUE) minDistance = 0.5f

        // 距离映射: 0 距离 = 100 分, 0.3 距离 = 0 分
        val normalizedDistance = (minDistance / 0.3f).coerceIn(0f, 1f)
        val score = (1f - normalizedDistance) * 100f
        return score.coerceIn(0f, 100f)
    }

    // MARK: - 平衡性评分

    /**
     * 计算裁切区域内的视觉平衡性得分
     *
     * 左右平衡和上下平衡各占 40%，多人脸分散度占 20%。
     * 无人脸时返回 50 分（中性）。
     */
    private fun scoreBalance(
        faces: List<RectF>,
        cropCenterX: Float,
        cropCenterY: Float
    ): Float {
        if (faces.isEmpty()) return 50f

        val faceCenters = faces.map { face ->
            floatArrayOf(face.centerX(), face.centerY())
        }

        // 人脸群重心
        var sumX = 0f
        var sumY = 0f
        for (center in faceCenters) {
            sumX += center[0]
            sumY += center[1]
        }
        val overallCenterX = sumX / faceCenters.size
        val overallCenterY = sumY / faceCenters.size

        // 左右平衡: 人脸群重心相对于裁切中心的水平偏移
        val horizontalOffset = abs(overallCenterX - cropCenterX)
        val horizontalScore = (100f - (horizontalOffset / 0.3f) * 100f).coerceIn(0f, 100f)

        // 上下平衡: 人脸群重心相对于裁切中心的垂直偏移
        val verticalOffset = abs(overallCenterY - cropCenterY)
        val verticalScore = (100f - (verticalOffset / 0.3f) * 100f).coerceIn(0f, 100f)

        // 多人脸时额外检查分散度
        var dispersionScore = 100f
        if (faceCenters.size > 1) {
            var distanceSum = 0f
            for (center in faceCenters) {
                val dx = center[0] - overallCenterX
                val dy = center[1] - overallCenterY
                distanceSum += sqrt(dx * dx + dy * dy)
            }
            val avgDistance = distanceSum / faceCenters.size
            // 理想分散度约为 0.2（归一化坐标）
            val idealDispersion = 0.2f
            val dispersionDeviation = abs(avgDistance - idealDispersion)
            dispersionScore = (100f - dispersionDeviation * 200f).coerceIn(0f, 100f)
        }

        val balanceScore = (horizontalScore + verticalScore) * 0.4f + dispersionScore * 0.2f
        return balanceScore.coerceIn(0f, 100f)
    }

    // MARK: - 居中评分

    /**
     * 计算主体居中度得分
     *
     * - 无人脸时: 裁切中心越接近画面中心越好
     * - 有人脸时: 人脸群中心应靠近裁切中心，但避免完全正中央（鼓励微偏移以配合三分法）
     */
    private fun scoreCentering(
        faces: List<RectF>,
        cropCenterX: Float,
        cropCenterY: Float
    ): Float {
        if (faces.isEmpty()) {
            // 无人脸时，裁切中心越接近画面中心越好
            val dx = cropCenterX - 0.5f
            val dy = cropCenterY - 0.5f
            val distance = sqrt(dx * dx + dy * dy)
            // 距离 0 = 100 分, 距离 0.25 = 0 分
            val score = 100f - (distance / 0.25f) * 100f
            return score.coerceIn(0f, 100f)
        }

        // 人脸群重心
        var sumX = 0f
        var sumY = 0f
        for (face in faces) {
            sumX += face.centerX()
            sumY += face.centerY()
        }
        val facesCenterX = sumX / faces.size
        val facesCenterY = sumY / faces.size

        val dx = facesCenterX - cropCenterX
        val dy = facesCenterY - cropCenterY
        val distance = sqrt(dx * dx + dy * dy)

        // 主体应靠近裁切中心但避免正中央（理想偏移 0.1）
        // 偏移 0.1 = 100 分, 偏移 0.1±0.2 = 0 分
        val idealOffset = 0.1f
        val tolerance = 0.2f
        val deviation = abs(distance - idealOffset)
        val score = 100f - (deviation / tolerance) * 100f
        return score.coerceIn(0f, 100f)
    }

    // MARK: - 水平线评分

    /**
     * 基于设备倾斜角度计算水平线得分
     *
     * score = 100 - |angle| * 5，结果限制在 0-100。
     * 0° = 100 分，20° = 0 分。
     */
    private fun scoreHorizonLevel(angle: Float): Float {
        val score = 100f - abs(angle) * 5f
        return score.coerceIn(0f, 100f)
    }

    // MARK: - 反馈生成

    /**
     * 根据各维度得分生成中文改进建议（基于最弱维度）
     */
    private fun generateFeedback(
        ruleOfThirds: Float,
        balance: Float,
        centering: Float,
        horizon: Float,
        hasFaces: Boolean
    ): String {
        val suggestions = mutableListOf<String>()

        // 各维度得分，用于查找最弱维度
        val scores = listOf(
            "三分法" to ruleOfThirds,
            "平衡性" to balance,
            "居中" to centering,
            "水平" to horizon
        )
        val sorted = scores.sortedBy { it.second }

        // 水平线最关键，优先提示
        if (horizon < 60f) {
            suggestions.add("请将设备放平，保持水平")
        }
        if (ruleOfThirds < 50f) {
            suggestions.add("尝试将主体放在画面三分线交叉点")
        }
        if (hasFaces && balance < 50f) {
            suggestions.add("调整构图使人物分布更均匀")
        }
        if (centering < 50f) {
            if (hasFaces) {
                suggestions.add("将人物置于画面中心区域")
            } else {
                suggestions.add("将主体置于画面中心")
            }
        }

        // 所有维度都良好时，依据最弱维度给出鼓励性提示
        if (suggestions.isEmpty()) {
            val minScore = sorted.first().second
            val minName = sorted.first().first
            return when {
                minScore >= 85f -> "构图优秀，继续保持！"
                minScore >= 70f -> "构图良好，可微调「$minName」"
                else -> "构图尚可，注意「$minName」"
            }
        }

        return suggestions.joinToString("；")
    }
}
