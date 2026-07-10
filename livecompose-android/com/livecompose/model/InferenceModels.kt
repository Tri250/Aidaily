package com.livecompose.model

import android.graphics.Bitmap
import java.io.Closeable

/**
 * 推理模型类型
 */
enum class ModelType {
    /** Teacher 模型 - ResNet50 backbone (服务器端/高配设备) */
    TEACHER_RESNET50,

    /** Student 模型 - MobileNetV3-Small backbone (端侧部署) */
    STUDENT_MOBILENETV3,

    /** BBox 回归头模型 (第一阶段) */
    BBOX_HEAD_ONLY,

    /** Actor 策略模型 (第二阶段) */
    ACTOR_ONLY
}

/**
 * 推理后端
 */
enum class InferenceBackend {
    /** TensorFlow Lite */
    TFLITE,

    /** ONNX Runtime */
    ONNX,

    /** NNAPI 加速 */
    NNAPI,

    /** GPU Delegate */
    GPU_DELEGATE
}

/**
 * 模型输入规格
 *
 * 描述模型期望的输入格式
 *
 * @param imageWidth 输入图像宽度
 * @param imageHeight 输入图像高度
 * @param imageChannels 输入图像通道数
 * @param stateDim 状态向量维度 (BBox: 4)
 * @param actionDim 动作空间维度 (7)
 */
data class ModelInputSpec(
    val imageWidth: Int = 224,
    val imageHeight: Int = 224,
    val imageChannels: Int = 3,
    val stateDim: Int = 4,
    val actionDim: Int = 7
)

/**
 * 模型性能指标
 *
 * @param inferenceTimeMs 推理耗时（毫秒）
 * @param peakMemoryBytes 峰值内存（字节）
 * @param modelFileSizeBytes 模型文件大小（字节）
 * @param fps 可达帧率
 */
data class ModelPerfMetrics(
    val inferenceTimeMs: Long = 0,
    val peakMemoryBytes: Long = 0,
    val modelFileSizeBytes: Long = 0,
    val fps: Float = 0f
)

/**
 * AdaCrop 推理接口
 *
 * 对应 PyTorch ActorCritic / MobileNetPolicy 的推理功能
 * Android 端通过 TFLite 或 ONNX Runtime 实现
 *
 * 使用示例:
 * ```kotlin
 * val model = AdacropModel.Factory.create(context, ModelType.STUDENT_MOBILENETV3)
 * val bbox = model.predictBBox(bitmap)
 * val (probs, value) = model.predictActor(bitmap, currentState)
 * model.close()
 * ```
 */
interface AdacropModel : Closeable {

    /** 模型类型 */
    val modelType: ModelType

    /** 推理后端 */
    val backend: InferenceBackend

    /** 输入规格 */
    val inputSpec: ModelInputSpec

    /** 性能指标 */
    val perfMetrics: ModelPerfMetrics

    /** 模型是否已加载就绪 */
    val isReady: Boolean

    /**
     * BBox 回归预测
     *
     * 对应 PyTorch model.backbone_forward(img) -> bbox
     * 第一阶段: 从整图预测初始裁剪框
     *
     * @param bitmap 输入图像
     * @return 预测的归一化 BBox
     */
    fun predictBBox(bitmap: Bitmap): BBoxPrediction

    /**
     * Actor-Critic 推理
     *
     * 对应 PyTorch model.forward(img, state) -> (action_probs, value)
     * 输入图像 + 当前 BBox 状态，输出动作概率和价值估计
     *
     * @param bitmap 输入图像
     * @param state 当前归一化 BBox 状态
     * @return 推理结果（动作概率 + 状态价值）
     */
    fun predictActorCritic(bitmap: Bitmap, state: BBoxNormalized): InferenceResult

    /**
     * 仅 Actor 推理（端侧常用）
     *
     * 对应 CoreML 的 StudentActorOnly 模型
     * 不计算 Critic 价值，节省计算量
     *
     * @param bitmap 输入图像
     * @param state 当前归一化 BBox 状态
     * @return 动作概率分布
     */
    fun predictActor(bitmap: Bitmap, state: BBoxNormalized): ActionDistribution

    /**
     * 批量推理（训练时使用）
     *
     * @param bitmaps 批量图像
     * @param states 批量 BBox 状态
     * @return 批量推理结果
     */
    fun predictBatch(
        bitmaps: List<Bitmap>,
        states: List<BBoxNormalized>
    ): List<InferenceResult>

    /**
     * 释放模型资源
     */
    override fun close()

    /**
     * 模型工厂
     */
    companion object Factory {
        /**
         * 创建 AdaCrop 推理模型实例
         *
         * @param modelType 模型类型
         * @param backend 推理后端
         * @param modelPath 模型文件路径
         * @param config 环境配置
         * @return AdacropModel 实例
         */
        fun create(
            modelType: ModelType,
            backend: InferenceBackend,
            modelPath: String,
            config: EnvConfig = EnvConfig()
        ): AdacropModel {
            // 实际实现由具体的 TFLite/ONNX 子类完成
            throw NotImplementedError(
                "Use TFLiteAdacropModel or ONNXAdacropModel implementation"
            )
        }
    }
}

/**
 * 美学评分模型接口
 *
 * 对应 NIMA / GAIC 评分器的推理功能
 */
interface AestheticScorer : Closeable {

    /** 评分器类型 */
    val scorerType: String

    /** 评分范围 */
    val scoreRange: ClosedFloatingPointRange<Float>

    /** 是否已就绪 */
    val isReady: Boolean

    /**
     * 对整图评分
     *
     * @param bitmap 输入图像
     * @return 美学评分
     */
    fun score(bitmap: Bitmap): AestheticScore

    /**
     * 对裁剪区域评分
     *
     * @param bitmap 原始图像
     * @param bbox 裁剪框（像素坐标）
     * @return 美学评分
     */
    fun scoreCrop(bitmap: Bitmap, bbox: BBoxPixel): AestheticScore

    /**
     * 批量评分
     *
     * @param bitmaps 批量图像
     * @return 批量美学评分
     */
    fun scoreBatch(bitmaps: List<Bitmap>): List<AestheticScore>

    override fun close()
}

/**
 * 知识蒸馏配置
 *
 * 对应 distillation/train_mobilenet_distill.py 的参数
 *
 * @param teacherCkptPath Teacher 模型 checkpoint 路径
 * @param trainJsonlPath 训练数据 JSONL 路径
 * @param valJsonPath 验证数据 JSON 路径
 * @param architecture Student 架构: mobilenet_v3_small 或 mobilenet_v3_large
 * @param skipBboxStage 是否跳过 Stage 1 (BBox 蒸馏)
 * @param bboxEpochs Stage 1 训练轮数
 * @param epochs Stage 2 训练轮数
 * @param batchSize 批量大小
 * @param lr 学习率
 * @param bboxLr Stage 1 学习率
 * @param weightDecay 权重衰减
 */
data class DistillationConfig(
    val teacherCkptPath: String = "",
    val trainJsonlPath: String = "",
    val valJsonPath: String = "",
    val architecture: String = "mobilenet_v3_small",
    val skipBboxStage: Boolean = false,
    val bboxEpochs: Int = 5,
    val epochs: Int = 10,
    val batchSize: Int = 64,
    val lr: Float = 1e-4f,
    val bboxLr: Float = 1e-4f,
    val weightDecay: Float = 1e-4f
)

/**
 * CoreML/模型导出配置
 *
 * 对应 coreml_export/export_student_coreml.py 的参数
 * Android 端对应的 TFLite 导出参数
 *
 * @param studentCkptPath Student checkpoint 路径
 * @param outputDir 输出目录
 * @param imgSize 输入图像尺寸
 * @param precision 精度: "float16" 或 "float32"
 * @param enableQuantization 是否启用 INT8 量化
 * @param enableXnnpack 是否启用 XNNPACK 委托
 */
data class ModelExportConfig(
    val studentCkptPath: String = "",
    val outputDir: String = "./export",
    val imgSize: Int = 224,
    val precision: String = "float16",
    val enableQuantization: Boolean = false,
    val enableXnnpack: Boolean = true
)
