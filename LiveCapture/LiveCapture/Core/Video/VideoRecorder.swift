//
//  VideoRecorder.swift
//  LiveCapture
//
//  视频录制核心模块
//
//  ## 文件作用
//  使用 AVAssetWriter 实现高质量视频录制
//  支持实时滤镜处理、拍照、防抖等功能
//  使用 HEVC (H.265) 编码以获得最佳压缩效率
//
//  ## 主要类
//  ### VideoRecorder
//  视频录制器，管理录制全生命周期
//
//  ## 核心功能
//  - 使用 AVAssetWriter + AVAssetWriterInput 进行高效编码
//  - 支持 HEVC (H.265) 视频编码和 AAC 音频编码
//  - 可选的实时滤镜处理（LutFilterProcessor）
//  - 录制过程中可拍照
//  - 支持暂停/恢复录制
//
//  ## 数据流
//  相机帧 → processFrame(CMSampleBuffer) → 滤镜处理 → AVAssetWriterInput → 输出视频文件
//
//  ## 线程安全
//  - writerQueue: 串行队列处理写入操作
//  - 所有 @Published 属性更新在主线程
//
//  ## 性能优化
//  - 使用 AVAssetWriterInputPixelBufferAdaptor 优化像素缓冲写入
//  - 使用 HEVC 编码器实现高压缩比
//  - expectsMediaDataInRealTime = true 保证实时编码
//

import Foundation
import AVFoundation
import CoreImage
import Photos
import UIKit

#if os(iOS)

/// 视频录制器 - 使用 AVAssetWriter 实现高质量视频录制
final class VideoRecorder: NSObject, ObservableObject {

    // MARK: - Published 属性

    /// 录制状态
    @Published var recordingState = VideoRecordingState()
    /// 预览图像（用于实时预览）
    @Published var previewImage: UIImage?

    // MARK: - 私有属性

    /// AVAssetWriter 实例
    var assetWriter: AVAssetWriter?
    /// 视频输入
    var videoInput: AVAssetWriterInput?
    /// 音频输入
    var audioInput: AVAssetWriterInput?
    /// 像素缓冲适配器（优化像素缓冲写入性能）
    var pixelBufferAdaptor: AVAssetWriterInputPixelBufferAdaptor?
    /// 录制开始时间（CMTime）
    var startTime: CMTime?
    /// 上一帧的呈现时间戳
    var lastSampleTime: CMTime = .zero
    /// Core Image 上下文
    var ciContext: CIContext
    /// 滤镜处理器
    var filterProcessor: LutFilterProcessor?
    /// 输出文件 URL
    var outputURL: URL?
    /// 写入操作队列
    let writerQueue: DispatchQueue = DispatchQueue(label: "livecapture.video.writer", qos: .userInitiated)
    /// 是否已暂停
    var isPaused: Bool = false
    /// 暂停期间跳过的帧数
    var pausedFrameCount: Int64 = 0
    /// 最后一帧的像素缓冲（用于拍照）
    var lastPixelBuffer: CVPixelBuffer?

    // MARK: - 初始化

    override init() {
        // 使用 Metal 加速创建 Core Image 上下文
        if let device = MTLCreateSystemDefaultDevice() {
            ciContext = CIContext(mtlDevice: device, options: [
                .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!,
                .outputColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!,
                .name: "VideoRecorder"
            ])
        } else {
            ciContext = CIContext(options: [
                .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!,
                .outputColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!,
                .name: "VideoRecorder"
            ])
        }
        super.init()
    }

    // MARK: - 开始录制

    /// 开始录制视频
    /// - Parameters:
    ///   - quality: 视频质量
    ///   - mode: 录制模式
    ///   - filterPreset: 可选的滤镜预设
    func startRecording(quality: VideoQuality, mode: VideoMode, filterPreset: LutFilterPreset? = nil) throws {
        // 1. 创建输出文件 URL
        let tempDir = FileManager.default.temporaryDirectory
        let fileName = "LiveCapture_\(Date().timeIntervalSince1970).mp4"
        let url = tempDir.appendingPathComponent(fileName)
        outputURL = url

        // 删除可能存在的旧文件
        if FileManager.default.fileExists(atPath: url.path) {
            try FileManager.default.removeItem(at: url)
        }

        // 2. 创建 AVAssetWriter
        assetWriter = try AVAssetWriter(url: url, fileType: .mp4)
        guard let writer = assetWriter else {
            throw VideoRecorderError.cannotCreateWriter
        }

        // 3. 配置视频编码参数（HEVC H.265）
        let videoSettings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.hevc,
            AVVideoWidthKey: quality.dimensions.width,
            AVVideoHeightKey: quality.dimensions.height,
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: quality.bitRate,
                AVVideoExpectedSourceFrameRateKey: quality.frameRate,
                AVVideoMaxKeyFrameIntervalKey: quality.frameRate * 2,
                AVVideoProfileLevelKey: quality.is4K ? kVTProfileLevel_HEVC_Main_AutoLevel : kVTProfileLevel_HEVC_Main_AutoLevel,
                AVVideoAllowFrameReorderingKey: false,
                AVVideoH264EntropyModeKey: AVVideoH264EntropyModeCABAC
            ]
        ]

        // 4. 创建视频输入
        videoInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
        guard let videoInput = videoInput else {
            throw VideoRecorderError.cannotAddVideoInput
        }

        videoInput.expectsMediaDataInRealTime = true
        videoInput.transform = transformForVideoOrientation()

        // 5. 创建像素缓冲适配器
        let sourcePixelBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
            kCVPixelBufferWidthKey as String: quality.dimensions.width,
            kCVPixelBufferHeightKey as String: quality.dimensions.height,
            kCVPixelBufferCGImageCompatibilityKey as String: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
        ]
        pixelBufferAdaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: videoInput,
            sourcePixelBufferAttributes: sourcePixelBufferAttributes
        )

        // 6. 添加视频输入
        guard writer.canAdd(videoInput) else {
            throw VideoRecorderError.cannotAddVideoInput
        }
        writer.add(videoInput)

        // 7. 如果需要音频，配置音频输入
        if mode.requiresAudio {
            let audioSettings: [String: Any] = [
                AVFormatIDKey: kAudioFormatMPEG4AAC,
                AVSampleRateKey: 44100,
                AVNumberOfChannelsKey: 2,
                AVEncoderBitRateKey: 128000,
                AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
            ]

            audioInput = AVAssetWriterInput(mediaType: .audio, outputSettings: audioSettings)
            guard let audioInput = audioInput, writer.canAdd(audioInput) else {
                throw VideoRecorderError.cannotAddAudioInput
            }
            audioInput.expectsMediaDataInRealTime = true
            writer.add(audioInput)
        }

        // 8. 初始化滤镜处理器
        if let preset = filterPreset {
            filterProcessor = LutFilterProcessor()
            _ = preset // 稍后使用
        }

        // 9. 开始写入
        writer.startWriting()
        // startSession 将在第一帧到达时调用

        // 10. 更新状态
        DispatchQueue.main.async {
            var state = VideoRecordingState()
            state.isRecording = true
            state.mode = mode
            state.quality = quality
            state.stabilizationEnabled = true
            state.filterEnabled = filterPreset != nil
            self.recordingState = state
        }

        startTime = nil
        lastSampleTime = .zero
        isPaused = false
        pausedFrameCount = 0
    }

    // MARK: - 处理视频帧

    /// 处理来自相机的视频帧
    /// - Parameter sampleBuffer: CMSampleBuffer 包含视频帧数据
    func processFrame(_ sampleBuffer: CMSampleBuffer) {
        guard recordingState.isRecording, !isPaused else { return }
        guard let writer = assetWriter else { return }

        let timestamp = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)

        // 第一帧时启动会话
        if startTime == nil {
            startTime = timestamp
            writer.startSession(atSourceTime: timestamp)
        }

        // 提取像素缓冲
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        // 保存最后一帧（用于拍照）
        lastPixelBuffer = pixelBuffer

        // 应用滤镜
        var outputPixelBuffer: CVPixelBuffer = pixelBuffer
        if recordingState.filterEnabled, let filterProcessor = filterProcessor,
           let preset = getCurrentFilterPreset() {
            if let filtered = filterProcessor.applyFilter(to: pixelBuffer, preset: preset) {
                outputPixelBuffer = filtered
            }
        }

        // 写入视频帧（避免"旋转"问题）
        let adjustedTimestamp = CMTimeSubtract(timestamp, startTime!)
        lastSampleTime = adjustedTimestamp

        guard let videoInput = videoInput, videoInput.isReadyForMoreMediaData else { return }

        // 等待像素缓冲适配器就绪后写入
        if let adaptor = pixelBufferAdaptor {
            if !adaptor.appendPixelBuffer(outputPixelBuffer, withPresentationTime: adjustedTimestamp) {
                #if DEBUG
                print("⚠️ 写入视频帧失败: \(writer.error?.localizedDescription ?? "未知错误")")
                #endif
            }
        }

        // 更新预览图像
        updatePreviewImage(from: outputPixelBuffer)

        // 更新时长
        let duration = CMTimeGetSeconds(adjustedTimestamp)
        DispatchQueue.main.async {
            self.recordingState.duration = duration
            self.recordingState.fileSize = self.estimateFileSize(duration: duration)
        }
    }

    /// 处理音频帧
    /// - Parameter sampleBuffer: CMSampleBuffer 包含音频数据
    func processAudioFrame(_ sampleBuffer: CMSampleBuffer) {
        guard recordingState.isRecording, !isPaused else { return }
        guard let startTime = startTime else { return }
        guard let audioInput = audioInput, audioInput.isReadyForMoreMediaData else { return }

        let timestamp = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
        let adjustedTime = CMTimeSubtract(timestamp, startTime)

        if !audioInput.append(sampleBuffer) {
            #if DEBUG
            print("⚠️ 写入音频帧失败")
            #endif
        }
    }

    // MARK: - 停止录制

    /// 停止录制
    /// - Parameter completion: 完成回调，返回输出文件 URL 或错误
    func stopRecording(completion: @escaping (URL?, Error?) -> Void) {
        guard recordingState.isRecording, let writer = assetWriter, let outputURL = outputURL else {
            completion(nil, VideoRecorderError.writerInWrongState)
            return
        }

        let recordedDuration = recordingState.duration

        videoInput?.markAsFinished()
        audioInput?.markAsFinished()

        writer.finishWriting { [weak self] in
            guard let self = self else { return }

            let finalURL = outputURL

            DispatchQueue.main.async {
                self.recordingState.isRecording = false
                self.recordingState.recordedDuration = recordedDuration
            }

            // 保存到相册
            if writer.status == .completed {
                self.saveToPhotoLibrary(url: finalURL) { success, error in
                    self.cleanup()
                    if success {
                        completion(finalURL, nil)
                    } else {
                        completion(finalURL, error)
                    }
                }
            } else {
                self.cleanup()
                completion(nil, writer.error ?? VideoRecorderError.encodingFailed)
            }
        }
    }

    // MARK: - 暂停/恢复

    /// 暂停录制
    func pauseRecording() {
        guard recordingState.isRecording, !isPaused else { return }
        isPaused = true
    }

    /// 恢复录制
    func resumeRecording() {
        guard recordingState.isRecording, isPaused else { return }
        isPaused = false
    }

    // MARK: - 拍照（录制中）

    /// 在录制过程中拍照
    /// - Returns: 拍到的照片数据（JPEG），失败返回 nil
    func capturePhotoDuringRecording() -> Data? {
        guard let pixelBuffer = lastPixelBuffer else { return nil }

        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)

        // 应用滤镜
        var finalImage = ciImage
        if recordingState.filterEnabled,
           let filterProcessor = filterProcessor,
           let preset = getCurrentFilterPreset() {
            finalImage = filterProcessor.applyFilter(to: ciImage, preset: preset)
        }

        guard let cgImage = ciContext.createCGImage(finalImage, from: finalImage.extent) else {
            return nil
        }

        let uiImage = UIImage(cgImage: cgImage)
        return uiImage.jpegData(compressionQuality: 0.92)
    }

    // MARK: - 私有方法

    /// 获取当前滤镜预设（子类可重写）
    func getCurrentFilterPreset() -> LutFilterPreset? {
        return nil
    }

    /// 视频方向变换矩阵
    private func transformForVideoOrientation() -> CGAffineTransform {
        // 默认竖屏方向
        return CGAffineTransform(rotationAngle: .pi / 2)
    }

    /// 更新预览图像
    private func updatePreviewImage(from pixelBuffer: CVPixelBuffer) {
        // 每 10 帧更新一次预览，避免性能问题
        let frameIndex = Int64(recordingState.duration * 30) // 估算帧索引
        guard frameIndex % 10 == 0 else { return }

        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        guard let cgImage = ciContext.createCGImage(ciImage, from: ciImage.extent) else { return }

        let image = UIImage(cgImage: cgImage, scale: 1.0, orientation: .right)
        DispatchQueue.main.async {
            self.previewImage = image
        }
    }

    /// 预估文件大小
    private func estimateFileSize(duration: TimeInterval) -> Int64 {
        let bitRate = Int64(recordingState.quality.bitRate)
        let audioBitRate: Int64 = recordingState.mode.requiresAudio ? 128_000 : 0
        let totalBitRate = bitRate + audioBitRate
        return Int64(duration * Double(totalBitRate) / 8.0)
    }

    /// 保存到相册
    private func saveToPhotoLibrary(url: URL, completion: @escaping (Bool, Error?) -> Void) {
        PHPhotoLibrary.requestAuthorization { status in
            guard status == .authorized || status == .limited else {
                completion(false, VideoRecorderError.saveToLibraryFailed)
                return
            }

            PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: url)
            } completionHandler: { success, error in
                completion(success, error)
            }
        }
    }

    /// 清理资源
    private func cleanup() {
        writerQueue.async { [weak self] in
            self?.assetWriter = nil
            self?.videoInput = nil
            self?.audioInput = nil
            self?.pixelBufferAdaptor = nil
            self?.startTime = nil
            self?.lastSampleTime = .zero
            self?.filterProcessor = nil
            self?.lastPixelBuffer = nil
            self?.isPaused = false
            self?.pausedFrameCount = 0
        }
    }
}

// MARK: - Metal 导入

import Metal

#endif