package com.livecompose.livecapture.core.detection

import android.graphics.RectF

/**
 * 美学裁切结果
 */
data class AestheticCrop(
    val rect: RectF,          // 归一化坐标 [0,1]
    val confidence: Float,    // 置信度
    val detectionType: String // 检测类型描述
)

/**
 * 裁切检测策略协议
 */
interface CropDetectionStrategy {
    fun detectBestCrop(
        pixelBuffer: ByteArray,
        width: Int,
        height: Int,
        rotation: Int,
        targetAspectRatio: Float,
        onResult: (AestheticCrop?) -> Unit
    )
}

/**
 * 检测模式
 */
enum class DetectionMode(val displayName: String, val description: String) {
    VISION("None", "使用 Android 原生框架进行人脸、人体和显著性检测，无额外模型"),
    FAST("Fast", "使用轻量级 Adacrop Student 模型，在速度和精度之间取得平衡，适合日常拍摄"),
    PRO("Pro", "使用全量专业级 Adacrop Teacher 模型，提供最高精度的构图建议，适合专业场景")
}