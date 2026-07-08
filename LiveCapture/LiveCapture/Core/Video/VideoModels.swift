//
//  VideoModels.swift
//  LiveCapture
//
//  视频录制数据模型定义
//
//  ## 文件作用
//  定义视频录制系统的所有数据模型和枚举类型
//  包括视频质量、录制模式、录制状态等
//
//  ## 主要类型
//
//  ### VideoQuality 枚举
//  定义视频录制质量选项
//  - hd1080p30: 1080p @ 30fps
//  - hd1080p60: 1080p @ 60fps
//  - uhd4k30: 4K @ 30fps
//  - uhd4k60: 4K @ 60fps
//
//  ### VideoMode 枚举
//  定义视频录制模式
//  - normal: 标准录制
//  - slowMotion: 慢动作录制（120/240fps 录制，30fps 播放）
//  - timelapse: 延时摄影（间隔帧录制）
//  - cinematic: 电影模式（带景深效果）
//
//  ### VideoRecordingState 结构体
//  录制状态的实时快照，供 UI 绑定
//  - isRecording: 是否正在录制
//  - duration: 当前已录制时长
//  - recordedDuration: 最终录制时长
//  - mode: 当前录制模式
//  - quality: 当前录制质量
//  - stabilizationEnabled: 是否启用防抖
//  - filterEnabled: 是否启用滤镜
//  - fileSize: 预估文件大小（字节）
//

import Foundation
import AVFoundation

#if os(iOS)

// MARK: - 视频质量

/// 视频录制质量选项
enum VideoQuality: String, CaseIterable, Codable, Identifiable {
    case hd1080p30
    case hd1080p60
    case uhd4k30
    case uhd4k60

    var id: String { rawValue }

    /// 视频尺寸
    var dimensions: CGSize {
        switch self {
        case .hd1080p30, .hd1080p60:
            return CGSize(width: 1920, height: 1080)
        case .uhd4k30, .uhd4k60:
            return CGSize(width: 3840, height: 2160)
        }
    }

    /// 帧率
    var frameRate: Int32 {
        switch self {
        case .hd1080p30, .uhd4k30:
            return 30
        case .hd1080p60, .uhd4k60:
            return 60
        }
    }

    /// 目标比特率（bps）
    var bitRate: Int {
        switch self {
        case .hd1080p30:
            return 8_000_000   // 8 Mbps
        case .hd1080p60:
            return 12_000_000  // 12 Mbps
        case .uhd4k30:
            return 25_000_000  // 25 Mbps
        case .uhd4k60:
            return 40_000_000  // 40 Mbps
        }
    }

    /// 显示名称
    var displayName: String {
        switch self {
        case .hd1080p30: return "1080p HD·30"
        case .hd1080p60: return "1080p HD·60"
        case .uhd4k30:  return "4K·30"
        case .uhd4k60:  return "4K·60"
        }
    }

    /// 是否为 4K 画质
    var is4K: Bool {
        switch self {
        case .uhd4k30, .uhd4k60: return true
        default: return false
        }
    }
}

// MARK: - 视频录制模式

/// 视频录制模式
enum VideoMode: String, CaseIterable, Codable, Identifiable {
    case normal
    case slowMotion
    case timelapse
    case cinematic

    var id: String { rawValue }

    /// 显示名称
    var displayName: String {
        switch self {
        case .normal:      return "标准"
        case .slowMotion:  return "慢动作"
        case .timelapse:   return "延时摄影"
        case .cinematic:   return "电影"
        }
    }

    /// SF Symbol 图标名称
    var iconName: String {
        switch self {
        case .normal:      return "video"
        case .slowMotion:  return "tortoise"
        case .timelapse:   return "timelapse"
        case .cinematic:   return "film"
        }
    }

    /// 是否需要音频录制
    var requiresAudio: Bool {
        switch self {
        case .normal, .cinematic, .slowMotion: return true
        case .timelapse: return false
        }
    }
}

// MARK: - 录制状态

/// 视频录制状态的实时快照
struct VideoRecordingState {
    /// 是否正在录制
    var isRecording: Bool = false
    /// 当前已录制时长（秒）
    var duration: TimeInterval = 0
    /// 录制完成后的最终时长（秒）
    var recordedDuration: TimeInterval = 0
    /// 当前录制模式
    var mode: VideoMode = .normal
    /// 当前录制质量
    var quality: VideoQuality = .hd1080p30
    /// 是否启用电子防抖
    var stabilizationEnabled: Bool = true
    /// 是否启用滤镜
    var filterEnabled: Bool = false
    /// 预估文件大小（字节）
    var fileSize: Int64 = 0

    /// 格式化时长显示（MM:SS）
    var formattedDuration: String {
        let totalSeconds = Int(duration)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }

    /// 格式化文件大小显示
    var formattedFileSize: String {
        let bytes = fileSize
        if bytes < 1024 {
            return "\(bytes) B"
        } else if bytes < 1024 * 1024 {
            return String(format: "%.1f KB", Double(bytes) / 1024.0)
        } else if bytes < 1024 * 1024 * 1024 {
            return String(format: "%.1f MB", Double(bytes) / (1024.0 * 1024.0))
        } else {
            return String(format: "%.2f GB", Double(bytes) / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

// MARK: - 视频录制错误

/// 视频录制可能的错误类型
enum VideoRecorderError: Error, LocalizedError {
    case cannotCreateWriter
    case cannotAddVideoInput
    case cannotAddAudioInput
    case noCameraPermission
    case noMicrophonePermission
    case writerInWrongState
    case noSampleBuffer
    case encodingFailed
    case saveToLibraryFailed
    case fileCreationFailed

    var errorDescription: String? {
        switch self {
        case .cannotCreateWriter:
            return "无法创建视频写入器"
        case .cannotAddVideoInput:
            return "无法添加视频输入"
        case .cannotAddAudioInput:
            return "无法添加音频输入"
        case .noCameraPermission:
            return "未授权相机访问"
        case .noMicrophonePermission:
            return "未授权麦克风访问"
        case .writerInWrongState:
            return "写入器状态异常"
        case .noSampleBuffer:
            return "缺少样本缓冲"
        case .encodingFailed:
            return "视频编码失败"
        case .saveToLibraryFailed:
            return "保存到相册失败"
        case .fileCreationFailed:
            return "创建输出文件失败"
        }
    }
}

#endif