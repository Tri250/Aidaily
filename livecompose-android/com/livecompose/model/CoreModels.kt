package com.livecompose.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 构图标签枚举 - 对应原始数据集的 composition_tags 字段
 *
 * 对应 HuggingFace 数据集 LiveCompose-outpainted-17k 中的标签：
 * - HORI2: 水平二分法构图
 * - HORI3: 水平三分法构图
 * - DIA: 对角线构图
 * - DENSE: 密集构图
 * - DIFFUSE: 散漫构图
 * - PATTERN: 图案/规律构图
 */
enum class CompositionTag(val displayName: String) {
    HORI2("水平二分法"),
    HORI3("水平三分法"),
    DIA("对角线"),
    DENSE("密集"),
    DIFFUSE("散漫"),
    PATTERN("图案");

    companion object {
        fun fromString(tag: String): CompositionTag =
            entries.find { it.name == tag }
                ?: throw IllegalArgumentException("Unknown composition tag: $tag")
    }
}

/**
 * 归一化边界框 - 以中心点坐标+相对宽高表示
 *
 * 对应 PyTorch 模型中 state 的 (cx, cy, w, h) 格式
 * 所有值归一化到 [0, 1] 范围
 *
 * @param cx 中心点 x 坐标（归一化）
 * @param cy 中心点 y 坐标（归一化）
 * @param w  相对宽度（归一化）
 * @param h  相对高度（归一化）
 */
@Parcelize
data class BBoxNormalized(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float
) : Parcelable {

    /** 转换为模型输入的 FloatArray，长度为 4 */
    fun toStateArray(): FloatArray = floatArrayOf(cx, cy, w, h)

    /** 转换为像素坐标的 BBoxPixel */
    fun toPixelBBox(imageWidth: Int, imageHeight: Int): BBoxPixel {
        val x1 = (cx - w / 2f) * imageWidth
        val y1 = (cy - h / 2f) * imageHeight
        val x2 = (cx + w / 2f) * imageWidth
        val y2 = (cy + h / 2f) * imageHeight
        return BBoxPixel(
            x1 = x1.toInt().coerceIn(0, imageWidth),
            y1 = y1.toInt().coerceIn(0, imageHeight),
            x2 = x2.toInt().coerceIn(0, imageWidth),
            y2 = y2.toInt().coerceIn(0, imageHeight)
        )
    }

    companion object {
        /** 从像素坐标的 BBoxPixel 转换 */
        fun fromPixelBBox(bbox: BBoxPixel, imageWidth: Int, imageHeight: Int): BBoxNormalized {
            val cx = (bbox.x1 + bbox.x2) / 2f / imageWidth
            val cy = (bbox.y1 + bbox.y2) / 2f / imageHeight
            val w = (bbox.x2 - bbox.x1).toFloat() / imageWidth
            val h = (bbox.y2 - bbox.y1).toFloat() / imageHeight
            return BBoxNormalized(cx, cy, w, h)
        }

        /** 初始默认 BBox (居中，覆盖约 60% 面积) */
        val DEFAULT = BBoxNormalized(0.5f, 0.5f, 0.6f, 0.6f)
    }
}

/**
 * 像素坐标边界框 - 以左上角和右下角坐标表示
 *
 * 对应 HuggingFace 数据集中 orig_bbox 字段的 [x1, y1, x2, y2] 格式
 *
 * @param x1 左上角 x 坐标（像素）
 * @param y1 左上角 y 坐标（像素）
 * @param x2 右下角 x 坐标（像素）
 * @param y2 右下角 y 坐标（像素）
 */
@Parcelize
data class BBoxPixel(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int
) : Parcelable {

    /** 像素宽度 */
    val width: Int get() = x2 - x1

    /** 像素高度 */
    val height: Int get() = y2 - y1

    /** 面积（像素） */
    val area: Int get() = width * height

    /** 宽高比 */
    val aspectRatio: Float get() = if (height != 0) width.toFloat() / height else 1f

    /** 转换为 IntArray */
    fun toArray(): IntArray = intArrayOf(x1, y1, x2, y2)

    companion object {
        /** 从 IntArray [x1, y1, x2, y2] 创建 */
        fun fromArray(arr: IntArray): BBoxPixel =
            BBoxPixel(arr[0], arr[1], arr[2], arr[3])

        /** 从 List<Int> 创建（用于 JSON 反序列化） */
        fun fromList(list: List<Int>): BBoxPixel =
            BBoxPixel(list[0], list[1], list[2], list[3])
    }
}

/**
 * 裁剪动作枚举
 *
 * 对应 PyTorch CropEnv 中的 7 个离散动作
 * Action space: left, right, up, down, zoom_in, zoom_out, stop
 */
enum class CropAction(val actionIndex: Int, val displayName: String) {
    LEFT(0, "左移"),
    RIGHT(1, "右移"),
    UP(2, "上移"),
    DOWN(3, "下移"),
    ZOOM_IN(4, "放大"),
    ZOOM_OUT(5, "缩小"),
    STOP(6, "停止");

    companion object {
        val ALL = entries.toList()

        fun fromIndex(index: Int): CropAction =
            entries.find { it.actionIndex == index }
                ?: throw IllegalArgumentException("Invalid action index: $index")
    }
}

/**
 * Actor 网络输出 - 动作概率分布
 *
 * @param probabilities 各动作的概率，长度为 7，顺序对应 CropAction
 * @param selectedIndex 选择的动作索引
 * @param selectedAction 选择的动作
 */
data class ActionDistribution(
    val probabilities: FloatArray,
    val selectedIndex: Int,
    val selectedAction: CropAction
) {
    /** 获取指定动作的概率 */
    fun probabilityOf(action: CropAction): Float = probabilities[action.actionIndex]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionDistribution) return false
        return probabilities.contentEquals(other.probabilities) &&
                selectedIndex == other.selectedIndex
    }

    override fun hashCode(): Int {
        var result = probabilities.contentHashCode()
        result = 31 * result + selectedIndex
        return result
    }
}

/**
 * Critic 网络输出 - 状态价值估计
 *
 * @param value 估计的状态价值（标量）
 */
data class StateValue(
    val value: Float
)

/**
 * 单步推理结果 - Actor + Critic 的完整输出
 *
 * 对应 PyTorch model.forward() 返回的 (action_probs, value)
 *
 * @param bbox 当前裁剪框状态
 * @param actionDistribution 动作概率分布
 * @param stateValue 状态价值
 * @param step 当前步数
 */
data class InferenceResult(
    val bbox: BBoxNormalized,
    val actionDistribution: ActionDistribution,
    val stateValue: StateValue,
    val step: Int
)

/**
 * BBox 回归头输出
 *
 * 对应 PyTorch bbox_head 的输出，预测初始裁剪框
 *
 * @param bbox 预测的归一化边界框
 * @param confidence 置信度（如果可用）
 */
data class BBoxPrediction(
    val bbox: BBoxNormalized,
    val confidence: Float = 1.0f
)
