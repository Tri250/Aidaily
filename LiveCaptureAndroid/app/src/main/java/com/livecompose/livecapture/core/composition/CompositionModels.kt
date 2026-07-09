package com.livecompose.livecapture.core.composition

/**
 * 构图引导系统数据模型
 *
 * 对应 iOS 端 CompositionModels.swift，定义 AR 构图引导系统所需的所有数据模型，
 * 包括构图引导线类型、构图评分、评分等级和姿势模板的结构化定义。
 */

// MARK: - 构图引导线类型

/**
 * 构图引导线类型
 *
 * 支持三分法、黄金比例、黄金螺旋、对称构图、中心聚焦、引导线、方形和无。
 */
enum class CompositionGuideType {
    RULE_OF_THIRDS,   // 三分法
    GOLDEN_RATIO,     // 黄金比例
    GOLDEN_SPIRAL,    // 黄金螺旋
    SYMMETRY,         // 对称构图
    CENTER_FOCUS,     // 中心聚焦
    LEADING_LINES,    // 引导线
    SQUARE,           // 方形
    NONE;             // 无

    val displayName: String
        get() = when (this) {
            RULE_OF_THIRDS -> "三分法"
            GOLDEN_RATIO -> "黄金比例"
            GOLDEN_SPIRAL -> "黄金螺旋"
            SYMMETRY -> "对称构图"
            CENTER_FOCUS -> "中心聚焦"
            LEADING_LINES -> "引导线"
            SQUARE -> "方形"
            NONE -> "无"
        }

    /** Material 图标名称（用于 Compose 图标渲染） */
    val iconName: String
        get() = when (this) {
            RULE_OF_THIRDS -> "grid_3x3"
            GOLDEN_RATIO -> "auto_awesome"
            GOLDEN_SPIRAL -> "all_inclusive"
            SYMMETRY -> "flip"
            CENTER_FOCUS -> "center_focus_strong"
            LEADING_LINES -> "linear_scale"
            SQUARE -> "crop_square"
            NONE -> "crop_free"
        }
}

// MARK: - 构图评分

/**
 * 多维度构图评分（0-100 分制）
 *
 * @param overall 综合评分
 * @param ruleOfThirds 三分法得分
 * @param balance 平衡性得分
 * @param centering 居中得分
 * @param horizonLevel 水平线得分
 * @param feedback 中文改进建议
 */
data class CompositionScore(
    val overall: Int,
    val ruleOfThirds: Int,
    val balance: Int,
    val centering: Int,
    val horizonLevel: Int,
    val feedback: String
)
