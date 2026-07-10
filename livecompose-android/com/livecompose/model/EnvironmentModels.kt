package com.livecompose.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 强化学习环境观测状态
 *
 * 对应 PyTorch CropEnv 的状态表示
 * state = 图像特征 + 归一化 BBox (cx, cy, w, h)
 *
 * @param bitmap 当前帧的图像 (imgSize x imgSize)
 * @param bbox 当前裁剪框的归一化状态
 * @param step 当前交互步数
 * @param prevAction 上一步采取的动作
 * @param prevReward 上一步获得的奖励
 * @param done 当前 episode 是否结束
 */
data class CropObservation(
    val bitmap: Bitmap,
    val bbox: BBoxNormalized,
    val step: Int = 0,
    val prevAction: CropAction? = null,
    val prevReward: Float = 0f,
    val done: Boolean = false
)

/**
 * 环境交互一步的结果
 *
 * 对应 PyTorch CropEnv.step() 的返回
 *
 * @param observation 新的观测状态
 * @param reward 本步奖励
 * @param done episode 是否结束
 * @param info 附加信息
 */
data class CropStepResult(
    val observation: CropObservation,
    val reward: Float,
    val done: Boolean,
    val info: StepInfo
)

/**
 * 步骤附加信息
 *
 * @param aestheticScore 当前美学评分
 * @param scoreDelta 评分变化量
 * @param actionTaken 采取的动作
 * @param areaRatio 当前 BBox 面积占图像面积的比例
 * @param bounded 是否触碰了边界
 * @param repeatedAction 是否重复了相同动作
 */
data class StepInfo(
    val aestheticScore: Float = 0f,
    val scoreDelta: Float = 0f,
    val actionTaken: CropAction = CropAction.STOP,
    val areaRatio: Float = 0f,
    val bounded: Boolean = false,
    val repeatedAction: Boolean = false
)

/**
 * 完整的 Episode 轨迹
 *
 * 对应 PPO 训练中的 rollout buffer 数据
 *
 * @param observations 观测序列
 * @param actions 动作序列
 * @param rewards 奖励序列
 * @param values 价值估计序列
 * @param logProbs 动作对数概率序列
 * @param dones 终止标志序列
 */
data class EpisodeTrajectory(
    val observations: List<CropObservation>,
    val actions: List<CropAction>,
    val rewards: List<Float>,
    val values: List<Float>,
    val logProbs: List<Float>,
    val dones: List<Boolean>
) {
    /** Episode 总奖励 */
    val totalReward: Float get() = rewards.sum()

    /** Episode 长度 */
    val length: Int get() = actions.size
}

/**
 * 动作执行结果 - 对 BBox 的修改
 *
 * 记录动作对裁剪框的具体影响，用于引导 UI 显示
 *
 * @param action 执行的动作
 * @param bboxBefore 执行前的 BBox
 * @param bboxAfter 执行后的 BBox
 * @param deltaCx cx 的变化量
 * @param deltaCy cy 的变化量
 * @param deltaW w 的变化量
 * @param deltaH h 的变化量
 */
@Parcelize
data class ActionEffect(
    val action: CropAction,
    val bboxBefore: BBoxNormalized,
    val bboxAfter: BBoxNormalized,
    val deltaCx: Float = 0f,
    val deltaCy: Float = 0f,
    val deltaW: Float = 0f,
    val deltaH: Float = 0f
) : Parcelable {

    companion object {
        /**
         * 计算动作对 BBox 的影响
         * 对应 PyTorch CropEnv._apply_action() 逻辑
         */
        fun applyAction(
            action: CropAction,
            bbox: BBoxNormalized,
            delta: Float
        ): ActionEffect {
            var dCx = 0f
            var dCy = 0f
            var dW = 0f
            var dH = 0f

            when (action) {
                CropAction.LEFT -> dCx = -delta
                CropAction.RIGHT -> dCx = delta
                CropAction.UP -> dCy = -delta
                CropAction.DOWN -> dCy = delta
                CropAction.ZOOM_IN -> {
                    dW = -delta
                    dH = -delta
                }
                CropAction.ZOOM_OUT -> {
                    dW = delta
                    dH = delta
                }
                CropAction.STOP -> { /* 无变化 */ }
            }

            val newBBox = BBoxNormalized(
                cx = (bbox.cx + dCx).coerceIn(0f, 1f),
                cy = (bbox.cy + dCy).coerceIn(0f, 1f),
                w = (bbox.w + dW).coerceIn(delta, 1f),
                h = (bbox.h + dH).coerceIn(delta, 1f)
            )

            return ActionEffect(
                action = action,
                bboxBefore = bbox,
                bboxAfter = newBBox,
                deltaCx = dCx,
                deltaCy = dCy,
                deltaW = dW,
                deltaH = dH
            )
        }
    }
}

/**
 * PPO 更新所需的批量数据
 *
 * 对应 PyTorch RolloutBuffer 中的数据，已展平
 */
data class PPOBatch(
    val states: Array<FloatArray>,       // [batch, 4] 归一化 BBox
    val images: Array<FloatArray>,       // [batch, 3, H, W] 图像张量
    val actions: IntArray,               // [batch] 动作索引
    val logProbs: FloatArray,            // [batch] 旧对数概率
    val returns: FloatArray,             // [batch] GAE 回报
    val advantages: FloatArray           // [batch] GAE 优势
) {
    val batchSize: Int get() = actions.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PPOBatch) return false
        return states.contentDeepEquals(other.states) &&
                images.contentDeepEquals(other.images) &&
                actions.contentEquals(other.actions)
    }

    override fun hashCode(): Int {
        var result = states.contentDeepHashCode()
        result = 31 * result + images.contentDeepHashCode()
        result = 31 * result + actions.contentHashCode()
        return result
    }
}

/**
 * 美学评分结果
 *
 * @param score 美学评分（NIMA: 1-10, GAIC: 0-1）
 * @param scorerType 评分器类型
 * @param rawScore 原始评分（未归一化）
 */
data class AestheticScore(
    val score: Float,
    val scorerType: String,
    val rawScore: Float = score
)
