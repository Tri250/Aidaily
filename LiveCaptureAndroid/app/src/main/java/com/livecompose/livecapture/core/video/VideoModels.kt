package com.livecompose.livecapture.core.video

import android.util.Size

/**
 * 视频录制数据模型定义
 *
 * 对应 iOS 端 VideoModels.swift，定义视频录制系统的所有数据模型和枚举类型。
 * 包括视频质量、录制模式、录制状态等。
 */

// MARK: - 视频质量

/**
 * 视频录制质量选项
 */
enum class VideoQuality {
    HD_1080P_30,
    HD_1080P_60,
    UHD_4K_30,
    UHD_4K_60;

    /** 视频尺寸 */
    val dimensions: Size
        get() = when (this) {
            HD_1080P_30, HD_1080P_60 -> Size(1920, 1080)
            UHD_4K_30, UHD_4K_60 -> Size(3840, 2160)
        }

    /** 帧率 */
    val frameRate: Int
        get() = when (this) {
            HD_1080P_30, UHD_4K_30 -> 30
            HD_1080P_60, UHD_4K_60 -> 60
        }

    /** 目标比特率（bps） */
    val bitRate: Int
        get() = when (this) {
            HD_1080P_30 -> 8_000_000   // 8 Mbps
            HD_1080P_60 -> 12_000_000  // 12 Mbps
            UHD_4K_30 -> 25_000_000    // 25 Mbps
            UHD_4K_60 -> 40_000_000    // 40 Mbps
        }

    /** 显示名称 */
    val displayName: String
        get() = when (this) {
            HD_1080P_30 -> "1080p HD·30"
            HD_1080P_60 -> "1080p HD·60"
            UHD_4K_30 -> "4K·30"
            UHD_4K_60 -> "4K·60"
        }

    /** 是否为 4K 画质 */
    val is4K: Boolean
        get() = this == UHD_4K_30 || this == UHD_4K_60
}

// MARK: - 视频录制模式

/**
 * 视频录制模式
 */
enum class VideoMode {
    NORMAL,       // 标准录制
    SLOW_MOTION,  // 慢动作录制（120/240fps 录制，30fps 播放）
    TIMELAPSE,    // 延时摄影（间隔帧录制）
    CINEMATIC;    // 电影模式（带景深效果）

    /** 显示名称 */
    val displayName: String
        get() = when (this) {
            NORMAL -> "标准"
            SLOW_MOTION -> "慢动作"
            TIMELAPSE -> "延时摄影"
            CINEMATIC -> "电影"
        }

    /** Material 图标名称（用于 Compose 图标选择） */
    val iconKey: String
        get() = when (this) {
            NORMAL -> "video"
            SLOW_MOTION -> "tortoise"
            TIMELAPSE -> "timelapse"
            CINEMATIC -> "film"
        }

    /** 是否需要音频录制 */
    val requiresAudio: Boolean
        get() = when (this) {
            TIMELAPSE -> false
            else -> true
        }
}

// MARK: - 录制状态

/**
 * 视频录制状态的实时快照，供 UI 绑定
 */
data class VideoRecordingState(
    /** 是否正在录制 */
    val isRecording: Boolean = false,
    /** 当前已录制时长（秒） */
    val duration: Double = 0.0,
    /** 录制完成后的最终时长（秒） */
    val recordedDuration: Double = 0.0,
    /** 当前录制模式 */
    val mode: VideoMode = VideoMode.NORMAL,
    /** 当前录制质量 */
    val quality: VideoQuality = VideoQuality.HD_1080P_30,
    /** 是否启用电子防抖 */
    val stabilizationEnabled: Boolean = true,
    /** 是否启用滤镜 */
    val filterEnabled: Boolean = false,
    /** 预估文件大小（字节） */
    val fileSize: Long = 0L
) {
    /** 格式化时长显示（MM:SS） */
    val formattedDuration: String
        get() {
            val totalSeconds = duration.toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    /** 格式化文件大小显示 */
    val formattedFileSize: String
        get() {
            val bytes = fileSize
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
}

// MARK: - 慢动作速度

/**
 * 慢动作速度选项
 */
enum class SlowMotionSpeed {
    SPEED_4X,   // 120fps → 4x 慢动作
    SPEED_8X;   // 240fps → 8x 慢动作

    /** 录制帧率 */
    val recordFrameRate: Int
        get() = when (this) {
            SPEED_4X -> 120
            SPEED_8X -> 240
        }

    /** 播放帧率 */
    val playbackFrameRate: Int
        get() = 30

    /** 慢动作倍率 */
    val slowdownFactor: Float
        get() = when (this) {
            SPEED_4X -> 4.0f
            SPEED_8X -> 8.0f
        }

    /** 显示名称 */
    val displayName: String
        get() = when (this) {
            SPEED_4X -> "4x 慢动作"
            SPEED_8X -> "8x 慢动作"
        }
}

// MARK: - 视频录制错误

/**
 * 视频录制可能的错误类型
 */
sealed class VideoRecorderError(message: String) : Exception(message) {
    object CannotCreateWriter : VideoRecorderError("无法创建视频写入器")
    object CannotAddVideoInput : VideoRecorderError("无法添加视频输入")
    object CannotAddAudioInput : VideoRecorderError("无法添加音频输入")
    object NoCameraPermission : VideoRecorderError("未授权相机访问")
    object NoMicrophonePermission : VideoRecorderError("未授权麦克风访问")
    object WriterInWrongState : VideoRecorderError("写入器状态异常")
    object NoSampleBuffer : VideoRecorderError("缺少样本缓冲")
    object EncodingFailed : VideoRecorderError("视频编码失败")
    object SaveToLibraryFailed : VideoRecorderError("保存到相册失败")
    object FileCreationFailed : VideoRecorderError("创建输出文件失败")
}
