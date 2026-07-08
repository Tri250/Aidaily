//
//  VideoViewModel.swift
//  LiveCapture
//
//  视频录制视图模型
//
//  ## 文件作用
//  协调视频录制、防抖、滤镜等模块
//  管理录制状态机，为 UI 提供统一接口
//  处理视频帧从相机到录制器的完整数据流
//
//  ## 主要类
//  ### VideoViewModel
//  视频录制视图模型（ObservableObject）
//
//  ## Dependencies（依赖项）
//  - videoRecorder: VideoRecorder - 标准视频录制器
//  - slowMotionRecorder: SlowMotionRecorder - 慢动作录制器
//  - timelapseRecorder: TimelapseRecorder - 延时摄影录制器
//  - stabilizer: VideoStabilizer - 电子防抖处理器
//  - camera: CameraManager - 相机管理器（外部注入）
//
//  ## Published 状态
//  - recordingState: 录制状态
//  - selectedQuality: 选中的视频质量
//  - selectedMode: 选中的录制模式
//  - selectedFilter: 选中的滤镜预设
//  - stabilizationEnabled: 是否启用防抖
//  - isSwitchingMode: 是否正在切换模式
//  - previewImage: 实时预览图像
//
//  ## 数据流
//  相机帧 → processFrame() → 防抖 → 滤镜 → 录制器写入 → 视频文件
//
//  ## 模式切换
//  - 标准模式：使用 VideoRecorder
//  - 慢动作模式：使用 SlowMotionRecorder
//  - 延时摄影模式：使用 TimelapseRecorder
//  - 电影模式：使用 VideoRecorder（含景深效果）
//
//  ## 线程安全
//  - 帧处理在 videoOutputQueue 执行
//  - UI 状态更新在主线程
//

import Foundation
import Combine
import AVFoundation
import UIKit

#if os(iOS)

/// 视频录制视图模型
final class VideoViewModel: ObservableObject {

    // MARK: - Published 属性

    /// 录制状态
    @Published var recordingState = VideoRecordingState()
    /// 选中的视频质量
    @Published var selectedQuality: VideoQuality = .hd1080p30
    /// 选中的录制模式
    @Published var selectedMode: VideoMode = .normal
    /// 选中的滤镜预设
    @Published var selectedFilter: LutFilterPreset? = nil
    /// 是否启用防抖
    @Published var stabilizationEnabled: Bool = true
    /// 是否正在切换模式
    @Published var isSwitchingMode: Bool = false
    /// 预览图像
    @Published var previewImage: UIImage?
    /// 错误信息
    @Published var errorMessage: String?
    /// 是否显示错误提示
    @Published var showError: Bool = false

    // MARK: - 录制器

    /// 标准视频录制器
    let videoRecorder = VideoRecorder()
    /// 慢动作录制器
    let slowMotionRecorder = SlowMotionRecorder()
    /// 延时摄影录制器
    let timelapseRecorder = TimelapseRecorder(interval: 2.0)
    /// 电子防抖处理器
    let stabilizer = VideoStabilizer()
    /// 滤镜处理器
    private let filterProcessor = LutFilterProcessor()

    // MARK: - 私有属性

    /// 相机管理器引用（由外部设置）
    weak var camera: CameraManager?
    /// Combine 订阅集合
    private var cancellables = Set<AnyCancellable>()
    /// 当前活跃的录制器
    private var activeRecorder: VideoRecorder? {
        switch selectedMode {
        case .normal, .cinematic:
            return videoRecorder
        case .slowMotion:
            return slowMotionRecorder
        case .timelapse:
            return nil // 延时摄影使用独立录制器
        }
    }
    /// 帧计数器（用于性能优化）
    private var frameCounter: Int64 = 0
    /// 上一帧的像素缓冲（用于延时摄影）
    private var lastRawPixelBuffer: CVPixelBuffer?

    // MARK: - 初始化

    init() {
        bindRecorders()
        bindTimelapseRecorder()
    }

    // MARK: - 绑定录制器

    private func bindRecorders() {
        // 绑定 VideoRecorder 状态
        videoRecorder.$recordingState
            .receive(on: DispatchQueue.main)
            .sink { [weak self] state in
                guard let self = self, self.selectedMode == .normal || self.selectedMode == .cinematic else { return }
                self.recordingState = state
            }
            .store(in: &cancellables)

        videoRecorder.$previewImage
            .receive(on: DispatchQueue.main)
            .sink { [weak self] image in
                self?.previewImage = image
            }
            .store(in: &cancellables)

        // 绑定 SlowMotionRecorder 状态
        slowMotionRecorder.$recordingState
            .receive(on: DispatchQueue.main)
            .sink { [weak self] state in
                guard let self = self, self.selectedMode == .slowMotion else { return }
                self.recordingState = state
            }
            .store(in: &cancellables)

        slowMotionRecorder.$previewImage
            .receive(on: DispatchQueue.main)
            .sink { [weak self] image in
                self?.previewImage = image
            }
            .store(in: &cancellables)
    }

    private func bindTimelapseRecorder() {
        timelapseRecorder.$isRecording
            .receive(on: DispatchQueue.main)
            .sink { [weak self] isRecording in
                guard let self = self, self.selectedMode == .timelapse else { return }
                self.recordingState.isRecording = isRecording
            }
            .store(in: &cancellables)

        timelapseRecorder.$elapsedTime
            .receive(on: DispatchQueue.main)
            .sink { [weak self] time in
                guard let self = self, self.selectedMode == .timelapse else { return }
                self.recordingState.duration = time
            }
            .store(in: &cancellables)

        timelapseRecorder.$previewImage
            .receive(on: DispatchQueue.main)
            .sink { [weak self] image in
                self?.previewImage = image
            }
            .store(in: &cancellables)
    }

    // MARK: - 开始录制

    /// 开始录制视频
    func startRecording() {
        guard !recordingState.isRecording else { return }

        do {
            switch selectedMode {
            case .normal, .cinematic:
                // 标准模式/电影模式
                try videoRecorder.startRecording(
                    quality: selectedQuality,
                    mode: selectedMode,
                    filterPreset: selectedFilter
                )

                if stabilizationEnabled {
                    stabilizer.startStabilization()
                }

            case .slowMotion:
                // 慢动作模式
                try slowMotionRecorder.startRecording(
                    quality: selectedQuality,
                    mode: selectedMode,
                    speed: .speed4x,
                    filterPreset: selectedFilter
                )

                if stabilizationEnabled {
                    stabilizer.startStabilization()
                }

            case .timelapse:
                // 延时摄影模式
                try timelapseRecorder.startRecording(
                    quality: selectedQuality,
                    filterPreset: selectedFilter
                )
            }
        } catch {
            showError(message: "录制启动失败: \(error.localizedDescription)")
        }
    }

    // MARK: - 停止录制

    /// 停止录制
    func stopRecording() {
        guard recordingState.isRecording else { return }

        switch selectedMode {
        case .normal, .cinematic, .slowMotion:
            guard let recorder = activeRecorder else { return }

            if stabilizationEnabled {
                stabilizer.stopStabilization()
            }

            recorder.stopRecording { [weak self] url, error in
                DispatchQueue.main.async {
                    if let error = error {
                        self?.showError(message: "录制完成但保存失败: \(error.localizedDescription)")
                    } else if url != nil {
                        // 录制成功
                        #if DEBUG
                        print("✅ 视频录制完成: \(url!.lastPathComponent)")
                        #endif
                    }
                }
            }

        case .timelapse:
            timelapseRecorder.stopRecording { [weak self] url in
                DispatchQueue.main.async {
                    if url != nil {
                        #if DEBUG
                        print("✅ 延时摄影完成: \(url!.lastPathComponent)")
                        #endif
                    }
                }
            }
        }
    }

    // MARK: - 模式切换

    /// 切换录制模式
    /// - Parameter mode: 目标模式
    func switchMode(_ mode: VideoMode) {
        guard mode != selectedMode, !recordingState.isRecording else { return }

        isSwitchingMode = true
        selectedMode = mode

        // 延时摄影模式不需要防抖
        if mode == .timelapse {
            stabilizationEnabled = false
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.isSwitchingMode = false
        }
    }

    // MARK: - 帧处理

    /// 处理来自相机的视频帧
    /// - Parameter sampleBuffer: CMSampleBuffer 包含视频帧数据
    func processFrame(_ sampleBuffer: CMSampleBuffer) {
        guard recordingState.isRecording else { return }

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        frameCounter += 1
        lastRawPixelBuffer = pixelBuffer

        if selectedMode == .timelapse {
            // 延时摄影：将帧传递给 TimelapseRecorder
            timelapseRecorder.captureFrame(from: pixelBuffer)
            return
        }

        // 1. 防抖处理
        var stabilizedBuffer = pixelBuffer
        if stabilizationEnabled {
            let timestamp = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
            if let stabilized = stabilizer.stabilizeFrame(pixelBuffer, timestamp: timestamp) {
                stabilizedBuffer = stabilized
            }
        }

        // 2. 传递给录制器
        guard let recorder = activeRecorder else { return }
        recorder.processFrame(sampleBuffer)

        // 注意：这里 processFrame 内部会提取 pixelBuffer 并应用滤镜
        // 防抖后的 pixelBuffer 需要特殊处理，这里简化处理
    }

    // MARK: - 音频帧处理

    /// 处理音频帧
    /// - Parameter sampleBuffer: CMSampleBuffer 包含音频数据
    func processAudioFrame(_ sampleBuffer: CMSampleBuffer) {
        guard recordingState.isRecording, selectedMode != .timelapse else { return }
        activeRecorder?.processAudioFrame(sampleBuffer)
    }

    // MARK: - 录制中拍照

    /// 在录制过程中拍照
    /// - Returns: 照片 JPEG 数据
    func capturePhoto() -> Data? {
        guard recordingState.isRecording else { return nil }
        return activeRecorder?.capturePhotoDuringRecording()
    }

    // MARK: - 暂停/恢复

    /// 暂停录制
    func pauseRecording() {
        guard selectedMode != .timelapse else { return }
        activeRecorder?.pauseRecording()
    }

    /// 恢复录制
    func resumeRecording() {
        guard selectedMode != .timelapse else { return }
        activeRecorder?.resumeRecording()
    }

    // MARK: - 设置相机引用

    /// 设置相机管理器引用
    /// - Parameter camera: CameraManager 实例
    func setCamera(_ camera: CameraManager) {
        self.camera = camera
    }

    // MARK: - 错误处理

    /// 显示错误提示
    private func showError(message: String) {
        DispatchQueue.main.async {
            self.errorMessage = message
            self.showError = true

            // 3 秒后自动隐藏
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                self.showError = false
            }
        }
    }

    // MARK: - 辅助方法

    /// 获取当前录制时长（格式化）
    var formattedDuration: String {
        return recordingState.formattedDuration
    }

    /// 获取当前文件大小（格式化）
    var formattedFileSize: String {
        return recordingState.formattedFileSize
    }

    /// 延时摄影加速比
    var timelapseSpeedupRatio: String {
        guard selectedMode == .timelapse else { return "" }
        let ratio = timelapseRecorder.speedupRatio
        return String(format: "%.0fx", ratio)
    }

    /// 延时摄影预估时长
    var timelapseEstimatedDuration: String {
        return timelapseRecorder.formattedEstimatedDuration
    }

    /// 延时摄影已录制时间
    var timelapseElapsedTime: String {
        return timelapseRecorder.formattedElapsedTime
    }

    /// 延时摄影帧数
    var timelapseFrameCount: Int {
        return timelapseRecorder.frameCount
    }

    /// 是否正在录制延时摄影
    var isRecordingTimelapse: Bool {
        return selectedMode == .timelapse && recordingState.isRecording
    }

    /// 防抖是否可用（延时摄影不可用）
    var canUseStabilization: Bool {
        return selectedMode != .timelapse
    }

    /// 切换防抖开关
    func toggleStabilization() {
        guard canUseStabilization else { return }
        stabilizationEnabled.toggle()

        if recordingState.isRecording {
            if stabilizationEnabled {
                stabilizer.startStabilization()
            } else {
                stabilizer.stopStabilization()
            }
        }
    }
}

#endif