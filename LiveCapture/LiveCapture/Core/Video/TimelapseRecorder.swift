//
//  TimelapseRecorder.swift
//  LiveCapture
//
//  延时摄影录制器
//
//  ## 文件作用
//  实现延时摄影视频录制功能
//  以固定时间间隔捕获帧，合成标准帧率视频
//  实现时间加速效果
//
//  ## 主要类
//  ### TimelapseRecorder
//  延时摄影录制器
//
//  ## 工作原理
//  1. 创建 AVAssetWriter 准备输出视频
//  2. 启动定时器，按固定间隔触发帧捕获
//  3. 每次定时器触发时，从相机获取当前帧
//  4. 写入到 AVAssetWriter
//  5. 停止时完成写入，生成最终视频
//
//  ## 参数配置
//  - interval: 帧捕获间隔（默认 2.0 秒）
//  - outputFrameRate: 输出帧率（默认 30fps）
//  - 默认加速比：30fps × 2s = 60x 速度提升
//
//  ## 使用示例
//  - 2s 间隔，30fps 输出 → 60x 加速
//  - 1s 间隔，30fps 输出 → 30x 加速
//  - 5s 间隔，30fps 输出 → 150x 加速
//
//  ## 线程安全
//  - 定时器在主线程运行
//  - 帧捕获在调用线程执行
//  - 写完操作异步执行
//

import Foundation
import AVFoundation
import CoreImage
import Photos
import UIKit

#if os(iOS)

/// 延时摄影录制器
final class TimelapseRecorder: ObservableObject {

    // MARK: - Published 属性

    /// 是否正在录制
    @Published var isRecording = false
    /// 已录制时间（现实时间）
    @Published var elapsedTime: TimeInterval = 0
    /// 预估输出视频时长（秒）
    @Published var estimatedDuration: TimeInterval = 0
    /// 已捕获帧数
    @Published var frameCount: Int = 0
    /// 预览图像
    @Published var previewImage: UIImage?

    // MARK: - 私有属性

    /// 帧捕获定时器
    private var captureTimer: Timer?
    /// AVAssetWriter 实例
    private var assetWriter: AVAssetWriter?
    /// 视频输入
    private var videoInput: AVAssetWriterInput?
    /// 像素缓冲适配器
    private var pixelBufferAdaptor: AVAssetWriterInputPixelBufferAdaptor?
    /// 帧捕获间隔（秒）
    private let interval: TimeInterval
    /// 输出帧率
    private let outputFrameRate: Int32 = 30
    /// 输出视频尺寸
    private var outputSize: CGSize = CGSize(width: 1920, height: 1080)
    /// 开始时间
    private var startDate: Date?
    /// Core Image 上下文
    private let ciContext: CIContext
    /// 输出文件 URL
    private var outputURL: URL?
    /// 最新像素缓冲（用于预览）
    private var latestPixelBuffer: CVPixelBuffer?
    /// 滤镜处理器
    private var filterProcessor: LutFilterProcessor?
    private var currentFilterPreset: LutFilterPreset?

    // MARK: - 初始化

    /// 创建延时摄影录制器
    /// - Parameter interval: 帧捕获间隔（秒），默认 2.0
    init(interval: TimeInterval = 2.0) {
        self.interval = max(0.1, interval) // 最小间隔 0.1 秒

        if let device = MTLCreateSystemDefaultDevice() {
            ciContext = CIContext(mtlDevice: device, options: [
                .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!,
                .name: "TimelapseRecorder"
            ])
        } else {
            ciContext = CIContext(options: [
                .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!,
                .name: "TimelapseRecorder"
            ])
        }
    }

    // MARK: - 开始录制

    /// 开始延时摄影录制
    /// - Parameters:
    ///   - quality: 视频质量（决定输出分辨率）
    ///   - filterPreset: 可选的滤镜预设
    func startRecording(quality: VideoQuality = .hd1080p30, filterPreset: LutFilterPreset? = nil) throws {
        guard !isRecording else { return }

        outputSize = quality.dimensions
        currentFilterPreset = filterPreset

        if let preset = filterPreset {
            filterProcessor = LutFilterProcessor()
            _ = preset
        }

        // 1. 创建输出文件
        let tempDir = FileManager.default.temporaryDirectory
        let fileName = "LiveCapture_Timelapse_\(Date().timeIntervalSince1970).mp4"
        let url = tempDir.appendingPathComponent(fileName)

        if FileManager.default.fileExists(atPath: url.path) {
            try FileManager.default.removeItem(at: url)
        }
        outputURL = url

        // 2. 创建 AVAssetWriter
        let writer = try AVAssetWriter(url: url, fileType: .mp4)

        // 3. 配置视频编码参数
        let videoSettings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.hevc,
            AVVideoWidthKey: outputSize.width,
            AVVideoHeightKey: outputSize.height,
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: quality.bitRate,
                AVVideoExpectedSourceFrameRateKey: outputFrameRate,
                AVVideoMaxKeyFrameIntervalKey: outputFrameRate * 2,
                AVVideoProfileLevelKey: kVTProfileLevel_HEVC_Main_AutoLevel,
                AVVideoAllowFrameReorderingKey: false
            ]
        ]

        // 4. 创建视频输入
        videoInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
        guard let videoInput = videoInput, writer.canAdd(videoInput) else {
            throw VideoRecorderError.cannotAddVideoInput
        }
        videoInput.expectsMediaDataInRealTime = true
        videoInput.transform = CGAffineTransform(rotationAngle: .pi / 2)
        writer.add(videoInput)

        // 5. 创建像素缓冲适配器
        let sourcePixelBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
            kCVPixelBufferWidthKey as String: outputSize.width,
            kCVPixelBufferHeightKey as String: outputSize.height,
            kCVPixelBufferCGImageCompatibilityKey as String: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
        ]
        pixelBufferAdaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: videoInput,
            sourcePixelBufferAttributes: sourcePixelBufferAttributes
        )

        // 6. 开始写入
        writer.startWriting()
        writer.startSession(atSourceTime: .zero)

        assetWriter = writer
        startDate = Date()
        frameCount = 0
        estimatedDuration = 0

        // 7. 启动定时器
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.isRecording = true
            self.elapsedTime = 0

            // 立即捕获第一帧
            self.captureTimer?.invalidate()
            self.captureTimer = Timer.scheduledTimer(
                withTimeInterval: self.interval,
                repeats: true
            ) { [weak self] _ in
                self?.onTimerTick()
            }
        }
    }

    // MARK: - 帧捕获

    /// 向录制器提供当前帧（由外部调用方传递）
    /// - Parameter pixelBuffer: 当前帧的像素缓冲
    func captureFrame(from pixelBuffer: CVPixelBuffer) {
        guard isRecording else { return }

        // 保存最新帧
        latestPixelBuffer = pixelBuffer

        // 更新预览
        updatePreview(from: pixelBuffer)
    }

    /// 定时器触发时的处理
    private func onTimerTick() {
        guard isRecording, let startDate = startDate else { return }

        // 更新已录制时间
        let elapsed = Date().timeIntervalSince(startDate)
        DispatchQueue.main.async {
            self.elapsedTime = elapsed
        }

        // 将最新帧写入视频
        guard let writer = assetWriter,
              let videoInput = videoInput,
              let pixelBuffer = latestPixelBuffer,
              videoInput.isReadyForMoreMediaData else {
            return
        }

        // 应用滤镜
        var outputPixelBuffer = pixelBuffer
        if let filterProcessor = filterProcessor,
           let preset = currentFilterPreset {
            if let filtered = filterProcessor.applyFilter(to: pixelBuffer, preset: preset) {
                outputPixelBuffer = filtered
            }
        }

        // 写入帧
        let presentationTime = CMTime(value: Int64(frameCount), timescale: outputFrameRate)

        if let adaptor = pixelBufferAdaptor {
            if adaptor.appendPixelBuffer(outputPixelBuffer, withPresentationTime: presentationTime) {
                frameCount += 1

                // 更新预估时长
                let estimated = Double(frameCount) / Double(outputFrameRate)
                DispatchQueue.main.async {
                    self.estimatedDuration = estimated
                }
            }
        }
    }

    /// 更新预览图像
    private func updatePreview(from pixelBuffer: CVPixelBuffer) {
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        guard let cgImage = ciContext.createCGImage(ciImage, from: ciImage.extent) else { return }

        let image = UIImage(cgImage: cgImage, scale: 1.0, orientation: .right)
        DispatchQueue.main.async {
            self.previewImage = image
        }
    }

    // MARK: - 停止录制

    /// 停止录制
    /// - Parameter completion: 完成回调，返回输出文件 URL
    func stopRecording(completion: @escaping (URL?) -> Void) {
        guard isRecording else {
            completion(nil)
            return
        }

        // 停止定时器
        captureTimer?.invalidate()
        captureTimer = nil

        isRecording = false

        guard let writer = assetWriter, let outputURL = outputURL else {
            completion(nil)
            return
        }

        videoInput?.markAsFinished()

        writer.finishWriting { [weak self] in
            guard let self = self else { return }

            let finalURL = outputURL

            // 保存到相册
            if writer.status == .completed {
                self.saveToPhotoLibrary(url: finalURL) { _ in
                    self.cleanup()
                    completion(finalURL)
                }
            } else {
                self.cleanup()
                completion(finalURL)
            }
        }
    }

    // MARK: - 私有方法

    /// 保存到相册
    private func saveToPhotoLibrary(url: URL, completion: @escaping (Bool) -> Void) {
        PHPhotoLibrary.requestAuthorization { status in
            guard status == .authorized || status == .limited else {
                completion(false)
                return
            }

            PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: url)
            } completionHandler: { success, _ in
                completion(success)
            }
        }
    }

    /// 清理资源
    private func cleanup() {
        assetWriter = nil
        videoInput = nil
        pixelBufferAdaptor = nil
        outputURL = nil
        latestPixelBuffer = nil
        filterProcessor = nil
        currentFilterPreset = nil
        startDate = nil
        frameCount = 0
        estimatedDuration = 0
        elapsedTime = 0
    }

    // MARK: - 计算属性

    /// 加速比
    var speedupRatio: Double {
        guard frameCount > 0 else { return 0 }
        let realTime = elapsedTime
        let playbackTime = Double(frameCount) / Double(outputFrameRate)
        guard playbackTime > 0 else { return 0 }
        return realTime / playbackTime
    }

    /// 格式化时长显示
    var formattedElapsedTime: String {
        let totalSeconds = Int(elapsedTime)
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%02d:%02d", minutes, seconds)
    }

    /// 格式化预估时长
    var formattedEstimatedDuration: String {
        let totalSeconds = Int(estimatedDuration)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}

// MARK: - Metal 导入

import Metal

#endif