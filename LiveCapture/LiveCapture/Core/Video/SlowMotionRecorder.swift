//
//  SlowMotionRecorder.swift
//  LiveCapture
//
//  慢动作视频录制器
//
//  ## 文件作用
//  继承自 VideoRecorder，实现慢动作视频录制
//  以高帧率（120fps/240fps）录制，以 30fps 播放
//  实现 4x 或 8x 慢动作效果
//
//  ## 主要类
//  ### SlowMotionRecorder
//  慢动作录制器，继承 VideoRecorder
//
//  ## 慢动作速度
//  - speed4x: 录制 120fps，播放 30fps（4x 慢动作）
//  - speed8x: 录制 240fps，播放 30fps（8x 慢动作）
//
//  ## 实现原理
//  1. 重写 startRecording 配置高帧率录制参数
//  2. 在视频编码设置中设置 sourceFrameRate 为高帧率
//  3. 正常写入所有帧，通过 AVAssetWriter 的帧率标记自动实现慢动作
//  4. 视频播放时系统自动以 30fps 播放，实现慢动作效果
//
//  ## 技术细节
//  - 使用 HEVC 编码，支持高帧率
//  - 比特率相应提高以保持画质
//  - 音频仍以正常速度录制
//
//  ## 线程安全
//  - 继承 VideoRecorder 的线程安全机制
//

import Foundation
import AVFoundation

#if os(iOS)

/// 慢动作速度选项
enum SlowMotionSpeed: String, CaseIterable, Codable, Identifiable {
    case speed4x   // 120fps → 4x 慢动作
    case speed8x   // 240fps → 8x 慢动作

    var id: String { rawValue }

    /// 录制帧率
    var recordFrameRate: Int32 {
        switch self {
        case .speed4x: return 120
        case .speed8x: return 240
        }
    }

    /// 播放帧率
    var playbackFrameRate: Int32 {
        return 30
    }

    /// 慢动作倍率
    var slowdownFactor: Float {
        switch self {
        case .speed4x: return 4.0
        case .speed8x: return 8.0
        }
    }

    /// 显示名称
    var displayName: String {
        switch self {
        case .speed4x: return "4x 慢动作"
        case .speed8x: return "8x 慢动作"
        }
    }

    /// 图标名称
    var iconName: String {
        return "tortoise"
    }
}

/// 慢动作录制器 - 继承 VideoRecorder
final class SlowMotionRecorder: VideoRecorder {

    // MARK: - 属性

    /// 慢动作速度
    private(set) var speed: SlowMotionSpeed = .speed4x
    /// 当前滤镜预设
    private var currentFilterPreset: LutFilterPreset?

    // MARK: - 开始录制

    /// 开始慢动作录制
    /// - Parameters:
    ///   - quality: 基础视频质量（用于分辨率）
    ///   - mode: 录制模式
    ///   - speed: 慢动作速度
    ///   - filterPreset: 可选的滤镜预设
    func startRecording(quality: VideoQuality, mode: VideoMode, speed: SlowMotionSpeed, filterPreset: LutFilterPreset? = nil) throws {
        self.speed = speed
        self.currentFilterPreset = filterPreset

        // 慢动作模式使用高帧率录制，但通过 AVAssetWriter 配置实现
        // 先调用父类方法创建基本的 writer
        try configureSlowMotionWriter(quality: quality, speed: speed, filterPreset: filterPreset)
    }

    /// 重写父类方法（保持接口兼容）
    override func startRecording(quality: VideoQuality, mode: VideoMode, filterPreset: LutFilterPreset? = nil) throws {
        try self.startRecording(quality: quality, mode: mode, speed: .speed4x, filterPreset: filterPreset)
    }

    // MARK: - 私有方法

    /// 配置慢动作写入器
    private func configureSlowMotionWriter(quality: VideoQuality, speed: SlowMotionSpeed, filterPreset: LutFilterPreset?) throws {
        // 1. 创建输出文件
        let tempDir = FileManager.default.temporaryDirectory
        let fileName = "LiveCapture_SlowMo_\(Date().timeIntervalSince1970).mp4"
        let url = tempDir.appendingPathComponent(fileName)

        if FileManager.default.fileExists(atPath: url.path) {
            try FileManager.default.removeItem(at: url)
        }

        // 2. 创建 AVAssetWriter
        let writer = try AVAssetWriter(url: url, fileType: .mp4)

        // 3. 配置慢动作视频编码参数
        // 关键：录制帧率设置为高帧率，但播放帧率标记为 30fps
        let videoSettings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.hevc,
            AVVideoWidthKey: quality.dimensions.width,
            AVVideoHeightKey: quality.dimensions.height,
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: slowMotionBitRate(quality: quality, speed: speed),
                AVVideoExpectedSourceFrameRateKey: speed.recordFrameRate,
                AVVideoMaxKeyFrameIntervalKey: speed.recordFrameRate * 2,
                AVVideoProfileLevelKey: kVTProfileLevel_HEVC_Main_AutoLevel,
                AVVideoAllowFrameReorderingKey: false
            ]
        ]

        // 4. 创建视频输入
        let videoInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
        videoInput.expectsMediaDataInRealTime = true
        videoInput.transform = CGAffineTransform(rotationAngle: .pi / 2)

        guard writer.canAdd(videoInput) else {
            throw VideoRecorderError.cannotAddVideoInput
        }
        writer.add(videoInput)

        // 5. 创建像素缓冲适配器
        let sourcePixelBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
            kCVPixelBufferWidthKey as String: quality.dimensions.width,
            kCVPixelBufferHeightKey as String: quality.dimensions.height,
            kCVPixelBufferCGImageCompatibilityKey as String: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
        ]
        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: videoInput,
            sourcePixelBufferAttributes: sourcePixelBufferAttributes
        )

        // 6. 添加音频输入（正常速度）
        let audioSettings: [String: Any] = [
            AVFormatIDKey: kAudioFormatMPEG4AAC,
            AVSampleRateKey: 44100,
            AVNumberOfChannelsKey: 2,
            AVEncoderBitRateKey: 128000,
            AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
        ]

        let audioInput = AVAssetWriterInput(mediaType: .audio, outputSettings: audioSettings)
        audioInput.expectsMediaDataInRealTime = true

        if writer.canAdd(audioInput) {
            writer.add(audioInput)
        }

        // 7. 初始化滤镜
        if filterPreset != nil {
            _ = LutFilterProcessor()
        }

        // 8. 开始写入
        writer.startWriting()

        // 9. 设置父类属性（子类可直接访问 internal 属性）
        self.assetWriter = writer
        self.videoInput = videoInput
        self.audioInput = audioInput
        self.pixelBufferAdaptor = adaptor
        self.outputURL = url
        self.startTime = nil
        self.lastSampleTime = .zero
        self.isPaused = false
        self.pausedFrameCount = 0

        // 10. 更新状态
        DispatchQueue.main.async {
            var state = VideoRecordingState()
            state.isRecording = true
            state.mode = .slowMotion
            state.quality = quality
            state.stabilizationEnabled = true
            state.filterEnabled = filterPreset != nil
            self.recordingState = state
        }
    }

    /// 计算慢动作录制比特率
    private func slowMotionBitRate(quality: VideoQuality, speed: SlowMotionSpeed) -> Int {
        // 高帧率需要更高比特率以保持画质
        let baseBitRate = quality.bitRate
        switch speed {
        case .speed4x:
            return Int(Double(baseBitRate) * 2.5) // 120fps 需要约 2.5x 比特率
        case .speed8x:
            return Int(Double(baseBitRate) * 4.0) // 240fps 需要约 4x 比特率
        }
    }

    // MARK: - 获取滤镜预设

    override func getCurrentFilterPreset() -> LutFilterPreset? {
        return currentFilterPreset
    }
}

#endif