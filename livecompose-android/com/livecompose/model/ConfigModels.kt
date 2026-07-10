package com.livecompose.model

/**
 * 环境配置
 *
 * 对应 config.yaml 中的 env 部分
 */
data class EnvConfig(
    /** 输入图像尺寸 (正方形) */
    val imgSize: Int = 224,

    /** 最大交互步数 */
    val maxSteps: Int = 200,

    /** 每步动作的位移增量（归一化） */
    val actionDelta: Float = 0.05f,

    /** 使用预测框初始化的概率 */
    val initWithPredProb: Float = 0.4f,

    /** 初始框的随机抖动量 */
    val initBoxJitter: Float = 0.05f,

    /** 调试日志开关 */
    val debugLog: Boolean = false,

    /** 调试日志间隔 */
    val debugLogInterval: Int = 50,

    /** 观测缓存大小 */
    val obsCacheSize: Int = 1024,

    /** 微调模式（false=正常训练模式） */
    val observationMode: Boolean = false
)

/**
 * 训练配置
 *
 * 对应 config.yaml 中的 train 部分
 */
data class TrainConfig(
    /** 训练 GPU 数量 */
    val trainingGpus: Int = 1,

    /** 算法名称 */
    val algorithm: String = "PPO",

    /** 工作线程数 */
    val numWorkers: Int = 16,

    /** 是否 pin rollout 数据到 GPU */
    val pinRollout: Boolean = true,

    /** 是否使用快速 reset */
    val fastReset: Boolean = true,

    /** 是否进行监督预训练 */
    val supervisedPretrain: Boolean = true,

    /** 预训练轮数 */
    val pretrainEpochs: Int = 20,

    /** 预训练学习率 */
    val pretrainLr: Float = 5e-5f,

    /** 预训练批量大小 */
    val pretrainBatchSize: Int = 512,

    /** PPO 初始 checkpoint 路径 */
    val initCkpt: String = "",

    /** 是否应用初始动作偏置 */
    val applyInitActionBias: Boolean = false,

    /** 折扣因子 */
    val gamma: Float = 0.99f,

    /** GAE lambda */
    val lam: Float = 0.95f,

    /** PPO clip 参数 */
    val clipParam: Float = 0.2f,

    /** 学习率 */
    val lr: Float = 3e-4f,

    /** 每次采样的步数 */
    val nSteps: Int = 256,

    /** 批量大小 */
    val batchSize: Int = 2048,

    /** 小批量大小 */
    val minibatchSize: Int = 2048,

    /** PPO 更新轮数 */
    val ppoEpochs: Int = 4,

    /** 价值损失系数 */
    val valueLossCoef: Float = 0.5f,

    /** 熵系数 */
    val entropyCoef: Float = 0.07f,

    /** 最终熵系数 */
    val entropyCoefFinal: Float = 0.01f,

    /** stop 动作掩码步数 */
    val stopMaskSteps: Int = 8,

    /** 最大梯度范数 */
    val maxGradNorm: Float = 0.8f,

    /** 最大训练步数 */
    val maxSteps: Int = 1_000_000,

    /** 日志间隔 */
    val logInterval: Int = 10,

    /** 保存间隔 */
    val saveInterval: Int = 20,

    /** 验证间隔 */
    val valInterval: Int = 20,

    /** 验证评估轮数 */
    val valEvalEpisodes: Int = 64,

    /** 保存目录 */
    val saveDir: String = "./logs"
)

/**
 * NIMA/GAIC 评分器配置
 *
 * 对应 config.yaml 中的 nima 部分
 */
data class ScorerConfig(
    /** GPU 设备 ID */
    val deviceId: Int = 0,

    /** 是否使用双 GPU */
    val useDualGpu: Boolean = false,

    /** 批量大小 */
    val batchSize: Int = 128,

    /** 最大等待时间 */
    val maxWaitTime: Float = 0.01f,

    /** 缓存大小 */
    val cacheSize: Int = 10000,

    /** 队列大小 */
    val queueSize: Int = 2048,

    /** NIMA 权重路径 */
    val weightsPath: String = "",

    /** 实际评分间隔 */
    val realScoreInterval: Int = 1,

    /** 评分器类型: "gaic" 或 "nima" */
    val scorerType: String = "gaic",

    /** GAIC 仓库目录 */
    val gaicRepoDir: String = "",

    /** GAIC checkpoint 路径 */
    val gaicCkpt: String = "",

    /** GAIC backbone */
    val gaicBackbone: String = "mobilenetv2",

    /** 是否归一化到 NIMA 评分尺度 */
    val normalizeToNimaScale: Boolean = true
)

/**
 * 奖励配置
 *
 * 对应 config.yaml 中的 reward 部分
 */
data class RewardConfig(
    // 基础惩罚
    val noOpPenalty: Float = 0.02f,
    val boundaryPenalty: Float = 0.05f,

    // 重复惩罚
    val visitPenalty: Float = 0.02f,
    val repeatPenalty: Float = 0.08f,
    val maxRepeat: Int = 12,
    val backtrackPenalty: Float = 0.20f,

    // 停止动作塑形
    val stopEarlyStep: Int = 5,
    val stopEarlyPenalty: Float = 0.2f,
    val stopCloseBestEps1: Float = 0.01f,
    val stopCloseBestEps2: Float = 0.03f,
    val stopCloseBestEps3: Float = 0.08f,
    val stopRewardEps1: Float = 0.35f,
    val stopRewardEps2: Float = 0.15f,
    val stopPenaltyEps3: Float = 0.10f,
    val stopPenaltyFar: Float = 0.30f,
    val stopPrevClip: Float = 0.2f,

    // 同动作连续惩罚
    val sameActionT1: Int = 4,
    val sameActionT2: Int = 6,
    val sameActionP1: Float = 0.03f,
    val sameActionP2: Float = 0.08f,

    // 振荡惩罚
    val oscWindow: Int = 12,
    val oscT1: Int = 2,
    val oscT2: Int = 4,
    val oscT3: Int = 6,
    val oscP1: Float = 0.06f,
    val oscP2: Float = 0.18f,
    val oscP3: Float = 0.35f,

    // 移动/缩放奖励
    val moveBase: Float = 0.05f,
    val moveDistScale: Float = 0.25f,
    val moveNewRegionBonus: Float = 0.05f,
    val zoomBonus: Float = 0.06f,
    val zoomAllowDrop: Float = 0.01f,

    // 尺寸控制
    val areaTarget: Float = 0.35f,
    val areaWeight: Float = 0.15f,
    val areaTolerance: Float = 0.10f,

    // 缩小限制
    val zoomOutNoopPenalty: Float = 0.05f,
    val zoomOutAreaMax: Float = 0.55f,
    val zoomOutAreaPenalty: Float = 0.8f
)

/**
 * 数据配置
 *
 * 对应 config.yaml 中的 data 部分
 */
data class DataConfig(
    /** 训练集 JSON 路径 */
    val trainJson: String = "",

    /** 验证集 JSON 路径 */
    val valJson: String = "",

    /** 环境数量 */
    val numEnvs: Int = 128
)

/**
 * 导出配置
 *
 * 对应 config.yaml 中的 export 部分
 */
data class ExportConfig(
    /** ONNX opset 版本 */
    val onnxOpset: Int = 13,

    /** CoreML 精度 */
    val coremlPrecision: String = "float16"
)

/**
 * 完整的 LiveCompose 配置
 *
 * 对应完整的 config.yaml
 */
data class LiveComposeConfig(
    val env: EnvConfig = EnvConfig(),
    val train: TrainConfig = TrainConfig(),
    val nima: ScorerConfig = ScorerConfig(),
    val reward: RewardConfig = RewardConfig(),
    val data: DataConfig = DataConfig(),
    val export: ExportConfig = ExportConfig()
)
