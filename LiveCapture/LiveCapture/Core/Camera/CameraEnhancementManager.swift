//
//  CameraEnhancementManager.swift
//  LiveCapture
//
//  拍摄增强管理器：定时拍摄、连拍、HDR 控制、Live Photo、闪光灯模式、音量键快门
//

import Foundation
import AVFoundation
import Combine
import UIKit
import MediaPlayer

#if os(iOS)

/// 拍摄增强管理器
final class CameraEnhancementManager: ObservableObject {
    static let shared = CameraEnhancementManager()

    // MARK: - 定时拍摄

    @Published var timerEnabled = false
    @Published var timerDuration: TimerDuration = .threeSeconds
    @Published var timerCountdown: Int = 0
    @Published var isTimerRunning = false

    enum TimerDuration: Int, CaseIterable {
        case threeSeconds = 3
        case fiveSeconds = 5
        case tenSeconds = 10

        var label: String { "\(rawValue)秒" }
        var icon: String {
            switch self {
            case .threeSeconds: return "3.circle"
            case .fiveSeconds: return "5.circle"
            case .tenSeconds: return "10.circle"
            }
        }
    }

    private var timerWorkItem: DispatchWorkItem?

    func startTimer(completion: @escaping () -> Void) {
        guard timerEnabled else {
            completion()
            return
        }

        isTimerRunning = true
        timerCountdown = timerDuration.rawValue

        // 倒计时
        func tick() {
            guard timerCountdown > 0 else {
                isTimerRunning = false
                HapticManager.shared.success()
                completion()
                return
            }
            if timerCountdown <= 3 {
                HapticManager.shared.light()
            }
            timerCountdown -= 1
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                tick()
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            tick()
        }
    }

    func cancelTimer() {
        timerWorkItem?.cancel()
        isTimerRunning = false
        timerCountdown = 0
    }

    // MARK: - 连拍模式

    @Published var burstModeEnabled = false
    @Published var burstCount = 0
    @Published var isBursting = false

    private let maxBurstCount = 10
    private var burstTimer: Timer?

    func startBurst(captureAction: @escaping () -> Void) {
        guard burstModeEnabled else {
            captureAction()
            return
        }

        isBursting = true
        burstCount = 0

        burstTimer = Timer.scheduledTimer(withTimeInterval: 0.15, repeats: true) { [weak self] timer in
            guard let self = self, self.burstCount < self.maxBurstCount else {
                timer.invalidate()
                self?.isBursting = false
                return
            }
            self.burstCount += 1
            HapticManager.shared.light()
            captureAction()
        }
    }

    func stopBurst() {
        burstTimer?.invalidate()
        burstTimer = nil
        isBursting = false
    }

    // MARK: - HDR 控制

    @Published var hdrMode: HDRMode = .auto

    enum HDRMode: String, CaseIterable {
        case auto
        case on
        case off

        var displayName: String {
            switch self {
            case .auto: return "自动"
            case .on: return "开启"
            case .off: return "关闭"
            }
        }

        var icon: String {
            switch self {
            case .auto: return "hdr"
            case .on: return "hdr.badge.checkmark"
            case .off: return "hdr.badge.slash"
            }
        }
    }

    func configureHDR(for photoOutput: AVCapturePhotoOutput) {
        #if os(iOS) && !targetEnvironment(macCatalyst)
        switch hdrMode {
        case .auto:
            photoOutput.isHighResolutionCaptureEnabled = true
        case .on:
            if #available(iOS 14.0, *) {
                photoOutput.maxPhotoQualityPrioritization = .quality
            }
            photoOutput.isHighResolutionCaptureEnabled = true
        case .off:
            photoOutput.isHighResolutionCaptureEnabled = false
        }
        #endif
    }

    // MARK: - Live Photo

    @Published var livePhotoEnabled = false

    func configureLivePhoto(for photoOutput: AVCapturePhotoOutput) {
        #if os(iOS) && !targetEnvironment(macCatalyst)
        if livePhotoEnabled && photoOutput.isLivePhotoCaptureSupported {
            photoOutput.isLivePhotoCaptureEnabled = true
        } else {
            photoOutput.isLivePhotoCaptureEnabled = false
        }
        #endif
    }

    // MARK: - 闪光灯

    @Published var flashMode: FlashMode = .auto
    @Published var torchEnabled = false

    enum FlashMode: String, CaseIterable {
        case auto = "自动"
        case on = "开启"
        case off = "关闭"
        case screenLight = "屏幕补光"

        var icon: String {
            switch self {
            case .auto: return "bolt.badge.automatic"
            case .on: return "bolt.fill"
            case .off: return "bolt.slash"
            case .screenLight: return "sun.max"
            }
        }
    }

    func configureFlash(for photoSettings: AVCapturePhotoSettings) {
        switch flashMode {
        case .auto:
            photoSettings.flashMode = .auto
        case .on:
            photoSettings.flashMode = .on
        case .off:
            photoSettings.flashMode = .off
        case .screenLight:
            // 屏幕补光：前置摄像头时使用
            photoSettings.flashMode = .off
            torchEnabled = true
        }
    }

    // MARK: - 音量键快门

    private var volumeObserver: Any?
    private var onVolumePress: (() -> Void)?

    func registerVolumeButtonShutter(onPress: @escaping () -> Void) {
        onVolumePress = onPress

        // 监听音量变化
        let audioSession = AVAudioSession.sharedInstance()
        try? audioSession.setActive(true)

        volumeObserver = NotificationCenter.default.addObserver(
            forName: NSNotification.Name(rawValue: "AVSystemController_SystemVolumeDidChangeNotification"),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.onVolumePress?()
            HapticManager.shared.light()
        }
    }

    func unregisterVolumeButtonShutter() {
        if let observer = volumeObserver {
            NotificationCenter.default.removeObserver(observer)
            volumeObserver = nil
        }
    }

    // MARK: - 单手操作

    @Published var oneHandMode: OneHandMode = .center
    @Published var isLeftHanded = false

    enum OneHandMode: String, CaseIterable {
        case center
        case left
        case right

        var displayName: String {
            switch self {
            case .center: return "居中"
            case .left: return "靠左"
            case .right: return "靠右"
            }
        }

        var alignment: HorizontalAlignment {
            switch self {
            case .center: return .center
            case .left: return .leading
            case .right: return .trailing
            }
        }
    }

    // MARK: - 性能优化

    @Published var performanceMode: PerformanceMode = .balanced

    enum PerformanceMode: String, CaseIterable {
        case quality
        case balanced
        case speed
        case battery

        var displayName: String {
            switch self {
            case .quality: return "画质优先"
            case .balanced: return "均衡"
            case .speed: return "速度优先"
            case .battery: return "省电"
            }
        }

        var reduceAIInference: Bool {
            switch self {
            case .quality: return false
            case .balanced: return false
            case .speed: return true
            case .battery: return true
            }
        }
    }

    /// 电池状态监听
    @Published var batteryLevel: Float = 1.0
    @Published var isLowPowerMode = false

    func startBatteryMonitoring() {
        UIDevice.current.isBatteryMonitoringEnabled = true
        batteryLevel = UIDevice.current.batteryLevel
        isLowPowerMode = ProcessInfo.processInfo.isLowPowerModeEnabled

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(batteryStateChanged),
            name: UIDevice.batteryStateDidChangeNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(powerModeChanged),
            name: NSNotification.Name.NSProcessInfoPowerStateDidChange,
            object: nil
        )
    }

    @objc private func batteryStateChanged() {
        batteryLevel = UIDevice.current.batteryLevel
        if batteryLevel < 0.2 {
            performanceMode = .battery
        }
    }

    @objc private func powerModeChanged() {
        isLowPowerMode = ProcessInfo.processInfo.isLowPowerModeEnabled
        if isLowPowerMode {
            performanceMode = .battery
        }
    }

    // MARK: - 存储管理

    @Published var availableStorage: Int64 = 0
    @Published var estimatedRemainingPhotos: Int = 0

    func checkStorage() {
        if let attrs = try? FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory()) {
            availableStorage = (attrs[.systemFreeSize] as? Int64) ?? 0
            let avgPhotoSize: Int64 = 5 * 1024 * 1024  // 5MB per photo
            estimatedRemainingPhotos = Int(availableStorage / avgPhotoSize)
        }
    }

    var isStorageLow: Bool {
        availableStorage < 100 * 1024 * 1024  // < 100MB
    }
}

#endif