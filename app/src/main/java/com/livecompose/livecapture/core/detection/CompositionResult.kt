package com.livecompose.livecapture.core.detection

data class CompositionResult(
    val bbox: FloatArray,           // [cx, cy, w, h] 相对坐标 (0~1)
    val action: ActionType,         // 最佳动作
    val actionProbabilities: FloatArray, // 7维动作概率分布
    val confidence: Float = 0.5f,   // 置信度
    val faceCoverage: Float = 0f,   // 人脸覆盖比例
    val ruleOfThirdsScore: Float = 0f, // 三分法构图得分
    val safetyMarginScore: Float = 1f  // 边缘安全区得分
) {
    enum class ActionType {
        LEFT, RIGHT, UP, DOWN, ZOOM_IN, ZOOM_OUT, STOP
    }

    val overallScore: Float
        get() = confidence * 0.4f +
                faceCoverage * 0.3f +
                ruleOfThirdsScore * 0.2f +
                safetyMarginScore * 0.1f

    val bboxCenterX: Float get() = bbox[0]
    val bboxCenterY: Float get() = bbox[1]
    val bboxWidth: Float get() = bbox[2]
    val bboxHeight: Float get() = bbox[3]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CompositionResult
        return bbox.contentEquals(other.bbox) &&
                action == other.action &&
                actionProbabilities.contentEquals(other.actionProbabilities)
    }

    override fun hashCode(): Int {
        var result = bbox.contentHashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + actionProbabilities.contentHashCode()
        return result
    }
}
