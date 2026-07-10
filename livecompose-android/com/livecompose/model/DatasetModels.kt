package com.livecompose.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 数据集样本 - 对应 HuggingFace LiveCompose-outpainted 数据集格式
 *
 * 数据集 Viewer 显示的字段:
 * - image: outpainted 图像
 * - orig_bbox: List[Int] [x1, y1, x2, y2] 原始裁剪框
 * - composition_tags: List[String] 构图标签
 *
 * 数据集规模: ~17,700 行
 * 许可: research-only (LiveCompose-outpainted) / MIT (LiveCompose-outpainted-17k)
 */
@Serializable
data class OutpaintedSample(
    /** outpainted 图像文件路径 */
    val file: String,

    /** 原始裁剪框 [x1, y1, x2, y2]（像素坐标） */
    @SerialName("orig_bbox")
    val origBbox: List<Int>,

    /** 构图标签列表 */
    @SerialName("composition_tags")
    val compositionTags: List<String>
) {
    /** 转换为 BBoxPixel */
    fun toBBoxPixel(): BBoxPixel = BBoxPixel.fromList(origBbox)

    /** 转换为 CompositionTag 列表 */
    fun toCompositionTags(): List<CompositionTag> =
        compositionTags.map { CompositionTag.fromString(it) }
}

/**
 * 数据集样本 - 对应 HuggingFace LiveCompose-outpainted-17k 数据集格式
 *
 * 该数据集为 tar-shards 格式，字段:
 * - file: 文件路径 (如 ./outpainted/HORI2_000001_v01.png)
 * - orig_bbox: List[Int] [x1, y1, x2, y2]
 *
 * 与 OutpaintedSample 的区别: 无 composition_tags 字段
 * 但标签信息编码在文件名前缀中 (如 HORI2_, DIFFUSE_, PATTERN_ 等)
 */
@Serializable
data class Outpainted17kSample(
    /** 文件路径 (含构图标签前缀) */
    val file: String,

    /** 原始裁剪框 [x1, y1, x2, y2] */
    @SerialName("orig_bbox")
    val origBbox: List<Int>
) {
    /** 从文件名解析构图标签 */
    fun extractCompositionTag(): CompositionTag? {
        val filename = file.substringAfterLast("/").substringBefore("_")
        return try {
            CompositionTag.fromString(filename)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** 转换为 BBoxPixel */
    fun toBBoxPixel(): BBoxPixel = BBoxPixel.fromList(origBbox)
}

/**
 * 训练数据对 - 对应 training_pairs.jsonl 格式
 *
 * 用于知识蒸馏的 Stage 1 (BBox Head) 和 Stage 2 (Actor Policy) 训练
 */
@Serializable
data class TrainingPair(
    /** 图像路径 */
    val image: String,

    /** 教师模型预测的 BBox (归一化 cx, cy, w, h) */
    val bbox: List<Float>,

    /** 教师模型的动作概率分布 (长度 7) */
    @SerialName("action_probs")
    val actionProbs: List<Float>? = null,

    /** 美学评分 */
    @SerialName("aesthetic_score")
    val aestheticScore: Float? = null
) {
    /** 转换为 BBoxNormalized */
    fun toBBoxNormalized(): BBoxNormalized = BBoxNormalized(
        cx = bbox[0], cy = bbox[1], w = bbox[2], h = bbox[3]
    )

    /** 转换为 ActionDistribution */
    fun toActionDistribution(): ActionDistribution? {
        val probs = actionProbs ?: return null
        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
        return ActionDistribution(
            probabilities = probs.toFloatArray(),
            selectedIndex = maxIdx,
            selectedAction = CropAction.fromIndex(maxIdx)
        )
    }
}

/**
 * 数据集分割索引 - 对应 splits/ 目录下的 JSON 文件
 *
 * 格式: train_mixed2.json / val_mixed.json
 */
@Serializable
data class DatasetSplit(
    /** 样本列表 */
    val samples: List<DatasetEntry>
)

/**
 * 数据集条目
 */
@Serializable
data class DatasetEntry(
    /** 图像路径 */
    @SerialName("image_path")
    val imagePath: String,

    /** 归一化 BBox [cx, cy, w, h] */
    val bbox: List<Float>,

    /** 数据来源 */
    val source: String = "",

    /** 构图标签 */
    val tag: String = ""
)

/**
 * 照片实体 - 用于 Room 数据库持久化
 *
 * 对应 iOS 端 Gallery 中存储的照片信息
 * 展示的元数据: 检测引擎名称, ISO, 快门速度, 光圈值, 像素尺寸
 */
@Parcelize
data class PhotoEntity(
    val id: Long = 0,
    val imagePath: String,
    val thumbnailPath: String? = null,
    val bboxCx: Float,
    val bboxCy: Float,
    val bboxW: Float,
    val bboxH: Float,
    val compositionTag: String?,
    val aestheticScore: Float,
    val detectionEngine: String,
    val iso: Int? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val pixelWidth: Int? = null,
    val pixelHeight: Int? = null,
    val captureTimestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    /** 转换为 BBoxNormalized */
    fun toBBoxNormalized(): BBoxNormalized = BBoxNormalized(bboxCx, bboxCy, bboxW, bboxH)

    companion object {
        /** 从 CapturedPhoto 创建 */
        fun fromCapturedPhoto(photo: CapturedPhoto, engine: String): PhotoEntity = PhotoEntity(
            imagePath = photo.imagePath,
            bboxCx = photo.bbox.cx,
            bboxCy = photo.bbox.cy,
            bboxW = photo.bbox.w,
            bboxH = photo.bbox.h,
            compositionTag = photo.compositionTag?.name,
            aestheticScore = photo.aestheticScore,
            detectionEngine = engine,
            captureTimestamp = photo.timestamp
        )
    }
}

/**
 * 构图引擎模式
 *
 * 对应 iOS App 的三阶段引擎设置:
 * - None: 使用原生 Apple Vision 框架（Android 对应 ML Kit）
 * - Fast: 使用轻量级 Student 模型 (MobileNetV3)
 * - Pro: 使用完整 Teacher 模型 (ResNet50)
 */
enum class CompositionEngine(val displayName: String) {
    /** 无 AI 模型，使用原生检测框架 */
    NONE("无"),

    /** 快速模式 - Student 蒸馏模型 */
    FAST("快速"),

    /** 专业模式 - Teacher 完整模型 */
    PRO("专业")
}

/**
 * App 设置
 *
 * 对应 iOS App 的设置页面
 */
@Parcelize
data class AppSettings(
    /** 构图引擎模式 */
    val engine: CompositionEngine = CompositionEngine.FAST,

    /** 是否开启自动拍摄 */
    val autoCapture: Boolean = true,

    /** 自动拍摄对齐阈值 (0-1) */
    val autoCaptureThreshold: Float = 0.85f,

    /** 是否开启触觉反馈 */
    val hapticFeedback: Boolean = true,

    /** 主题模式: "system", "light", "dark" */
    val themeMode: String = "system",

    /** 最大推理帧率 */
    val maxInferenceFps: Int = 30
) : Parcelable
