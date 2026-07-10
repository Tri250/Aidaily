package com.livecompose.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 捕获状态机状态
 *
 * 对应 iOS CaptureViewModel 的状态机：
 * waiting → detecting → tracking → capturing
 */
enum class CaptureState {
    /** 等待用户启动 */
    WAITING,

    /** AI 正在检测画面中的构图 */
    DETECTING,

    /** 正在追踪构图目标并引导用户移动 */
    TRACKING,

    /** 对齐完成，正在自动拍摄 */
    CAPTURING,

    /** 错误状态 */
    ERROR
}

/**
 * 陀螺仪追踪数据
 *
 * 对应 iOS CoreMotion 的设备运动数据
 *
 * @param attitudeX 设备姿态 Pitch（弧度）
 * @param attitudeY 设备姿态 Yaw（弧度）
 * @param attitudeZ 设备姿态 Roll（弧度）
 * @param rotationRateX 绕 X 轴角速度（弧度/秒）
 * @param rotationRateY 绕 Y 轴角速度（弧度/秒）
 * @param rotationRateZ 绕 Z 轴角速度（弧度/秒）
 * @param gravityX 重力方向 X 分量
 * @param gravityY 重力方向 Y 分量
 * @param gravityZ 重力方向 Z 分量
 * @param timestamp 时间戳（纳秒）
 */
@Parcelize
data class MotionData(
    val attitudeX: Float = 0f,
    val attitudeY: Float = 0f,
    val attitudeZ: Float = 0f,
    val rotationRateX: Float = 0f,
    val rotationRateY: Float = 0f,
    val rotationRateZ: Float = 0f,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = 0f,
    val timestamp: Long = 0L
) : Parcelable

/**
 * 构图引导方向
 *
 * AI 分析后建议用户移动手机的方向
 */
enum class GuidanceDirection {
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_UP,
    MOVE_DOWN,
    ZOOM_IN,
    ZOOM_OUT,
    HOLD_STILL,
    ALIGNED
}

/**
 * 用户引导信息
 *
 * 对应 iOS UserGuidanceView 的数据模型
 * 提供给用户的实时构图引导指示
 *
 * @param direction 引导方向
 * @param targetBBox 目标裁剪框
 * @param currentBBox 当前裁剪框
 * @param alignmentScore 对齐评分 (0-1, 1=完美对齐)
 * @param isAligned 是否已对齐到目标
 * @param hapticFeedback 是否应触发触觉反馈
 * @param distanceToTarget 与目标的距离（归一化）
 */
data class UserGuidance(
    val direction: GuidanceDirection,
    val targetBBox: BBoxNormalized,
    val currentBBox: BBoxNormalized,
    val alignmentScore: Float = 0f,
    val isAligned: Boolean = false,
    val hapticFeedback: Boolean = false,
    val distanceToTarget: Float = 1f
)

/**
 * 磁性吸附点
 *
 * 用于 BoxCenterManager 的磁性吸附逻辑
 * 当 BBox 中心接近目标点时自动吸附
 *
 * @param x 吸附点 x（归一化）
 * @param y 吸附点 y（归一化）
 * @param threshold 吸附阈值（归一化距离）
 * @param strength 吸附强度 (0-1)
 */
@Parcelize
data class SnapPoint(
    val x: Float,
    val y: Float,
    val threshold: Float = 0.03f,
    val strength: Float = 0.5f
) : Parcelable

/**
 * 框中心追踪状态
 *
 * 对应 iOS BoxCenterManager 的内部状态
 * 管理 BBox 中心的物理追踪和磁性吸附
 *
 * @param currentCenter 当前中心坐标 (cx, cy)
 * @param targetCenter 目标中心坐标 (cx, cy)
 * @param velocity 追踪速度 (vx, vy)
 * @param isSnapped 是否已吸附到目标
 * @param snapPoint 当前吸附点（如果吸附）
 * @param motionData 最新的陀螺仪数据
 */
data class BoxTrackingState(
    val currentCenter: Pair<Float, Float>,
    val targetCenter: Pair<Float, Float>,
    val velocity: Pair<Float, Float> = Pair(0f, 0f),
    val isSnapped: Boolean = false,
    val snapPoint: SnapPoint? = null,
    val motionData: MotionData? = null
)

/**
 * 捕获会话状态
 *
 * 整合了捕获过程中的所有状态信息
 * 对应 iOS CaptureViewModel 的完整状态
 *
 * @param captureState 当前捕获状态
 * @param currentBBox 当前 AI 建议的裁剪框
 * @param targetBBox 目标裁剪框
 * @param guidance 用户引导信息
 * @param trackingState 追踪状态
 * @param aestheticScore 当前美学评分
 * @param detectedComposition 检测到的构图类型
 * @param frameCount 已处理帧数
 * @param isInferenceReady 推理模型是否就绪
 * @param errorMessage 错误信息（如果有）
 */
data class CaptureSessionState(
    val captureState: CaptureState = CaptureState.WAITING,
    val currentBBox: BBoxNormalized = BBoxNormalized.DEFAULT,
    val targetBBox: BBoxNormalized? = null,
    val guidance: UserGuidance? = null,
    val trackingState: BoxTrackingState? = null,
    val aestheticScore: AestheticScore? = null,
    val detectedComposition: CompositionTag? = null,
    val frameCount: Int = 0,
    val isInferenceReady: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 拍摄的照片结果
 *
 * @param imagePath 照片存储路径
 * @param bbox 最终裁剪框
 * @param compositionTag 构图标签
 * @param aestheticScore 美学评分
 * @param captureState 拍摄时的状态
 * @param timestamp 拍摄时间戳
 */
@Parcelize
data class CapturedPhoto(
    val imagePath: String,
    val bbox: BBoxNormalized,
    val compositionTag: CompositionTag?,
    val aestheticScore: Float,
    val captureState: CaptureState,
    val timestamp: Long
) : Parcelable
