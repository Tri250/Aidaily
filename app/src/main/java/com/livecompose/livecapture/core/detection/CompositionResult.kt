package com.livecompose.livecapture.core.detection

data class CompositionResult(
    val bbox: FloatArray,           // [cx, cy, w, h] 相对坐标 (0~1)
    val action: ActionType,         // 最佳动作
    val actionProbabilities: FloatArray, // 7维动作概率分布
    val confidence: Float = 0.5f,   // 置信度
    val faceCoverage: Float = 0f,   // 人脸覆盖比例
    val ruleOfThirdsScore: Float = 0f, // 三分法构图得分
    val safetyMarginScore: Float = 1f,  // 边缘安全区得分
    val sceneType: SceneType = SceneType.GENERAL, // 智能场景识别
    val lightingQuality: LightingQuality = LightingQuality.GOOD, // 光照质量
    val shootingTip: String = ""    // 智能拍摄指导提示
) {
    enum class ActionType {
        LEFT, RIGHT, UP, DOWN, ZOOM_IN, ZOOM_OUT, STOP
    }

    /**
     * 智能场景识别类型
     * 参考: ai-photography-assistant (gitee.com/zheng-bojie)
     */
    enum class SceneType(val label: String, val shootingParams: ShootingParams) {
        PORTRAIT_STANDING("人像·站姿", ShootingParams(aperture = "F1.8", shutter = "1/200s", iso = "100", wb = "自动", tip = "背景虚化，注意对焦眼睛")),
        PORTRAIT_SITTING("人像·坐姿", ShootingParams(aperture = "F2.8", shutter = "1/160s", iso = "200", wb = "自动", tip = "佳能人像色彩表现佳，建议用大光圈")),
        LANDSCAPE_SUNSET("风景·日落", ShootingParams(aperture = "F8", shutter = "1/125s", iso = "100", wb = "晴天", tip = "使用三脚架，保证画面清晰")),
        LANDSCAPE_NATURE("风景·自然", ShootingParams(aperture = "F8", shutter = "1/200s", iso = "100", wb = "晴天", tip = "利用前景增加层次感")),
        NIGHT_SCENE("夜景", ShootingParams(aperture = "F2.0", shutter = "1/30s", iso = "800", wb = "钨丝灯", tip = "建议开启防抖，使用低速快门")),
        FOOD_STYLING("美食·摆盘", ShootingParams(aperture = "F2.4", shutter = "1/100s", iso = "200", wb = "自动", tip = "45度俯拍，突出摆盘美感")),
        PRODUCT_WHITE("产品·白底", ShootingParams(aperture = "F5.6", shutter = "1/160s", iso = "100", wb = "自定义", tip = "确保光线均匀，消除阴影")),
        CITY_URBAN("城市·街拍", ShootingParams(aperture = "F4.0", shutter = "1/250s", iso = "200", wb = "自动", tip = "抓拍动态瞬间，注意光影变化")),
        GENERAL("通用场景", ShootingParams(aperture = "F4.0", shutter = "1/125s", iso = "100", wb = "自动", tip = ""));
    }

    enum class LightingQuality { GOOD, TOO_DARK, TOO_BRIGHT, BACKLIT, LOW_CONTRAST }

    data class ShootingParams(
        val aperture: String,
        val shutter: String,
        val iso: String,
        val wb: String,
        val tip: String
    )

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
