//
//  ProCameraManager.swift
//  LiveCapture
//
//  专业相机控制管理器
//  手动对焦、ISO、快门速度、白平衡、曝光补偿、AE/AF 锁、RAW 捕获
//

import Foundation
import AVFoundation
import CoreImage
import Combine

#if os(iOS)

/// 专业相机控制管理器
final class ProCameraManager: ObservableObject {
    static let shared = ProCameraManager()

    // MARK: - Published State

    @Published var isProModeEnabled = false

    // 手动对焦
    @Published var manualFocusEnabled = false
    @Published var focusLensPosition: Float = 0.5  // 0.0(近) ~ 1.0(远)
    @Published var focusPeakingEnabled = false

    // 手动曝光
    @Published var manualExposureEnabled = false
    @Published var iso: Float = 100                // 50 ~ 3200
    @Published var shutterSpeed: CMTime = CMTime(value: 1, timescale: 120)  // 1/120s
    @Published var exposureBias: Float = 0.0       // -3.0 ~ +3.0 EV

    // 手动白平衡
    @Published var manualWhiteBalanceEnabled = false
    @Published var colorTemperature: Float = 5500  // 2500K ~ 8000K

    // RAW 捕获
    @Published var rawCaptureEnabled = false

    // AE/AF 锁定
    @Published var aeLocked = false
    @Published var afLocked = false

    // 直方图数据
    @Published var histogramData: [Float] = Array(repeating: 0, count: 256)

    // 斑马纹
    @Published var zebraEnabled = false
    @Published var zebraThreshold: Float = 0.95    // 过曝阈值 95%

    // MARK: - ISO 预设

    var isoPresets: [(value: Float, label: String)] {
        [
            (50, "50"), (64, "64"), (80, "80"), (100, "100"),
            (125, "125"), (160, "160"), (200, "200"), (250, "250"),
            (320, "320"), (400, "400"), (500, "500"), (640, "640"),
            (800, "800"), (1000, "1000"), (1250, "1250"), (1600, "1600"),
            (2000, "2000"), (2500, "2500"), (3200, "3200")
        ]
    }

    // MARK: - 快门速度预设

    var shutterSpeedPresets: [(value: CMTime, label: String)] {
        [
            (CMTime(value: 1, timescale: 8000), "1/8000"),
            (CMTime(value: 1, timescale: 4000), "1/4000"),
            (CMTime(value: 1, timescale: 2000), "1/2000"),
            (CMTime(value: 1, timescale: 1000), "1/1000"),
            (CMTime(value: 1, timescale: 500), "1/500"),
            (CMTime(value: 1, timescale: 250), "1/250"),
            (CMTime(value: 1, timescale: 125), "1/125"),
            (CMTime(value: 1, timescale: 60), "1/60"),
            (CMTime(value: 1, timescale: 30), "1/30"),
            (CMTime(value: 1, timescale: 15), "1/15"),
            (CMTime(value: 1, timescale: 8), "1/8"),
            (CMTime(value: 1, timescale: 4), "1/4"),
            (CMTime(value: 1, timescale: 2), "1/2"),
            (CMTime(value: 1, timescale: 1), "1\""),
        ]
    }

    // MARK: - 白平衡预设

    var whiteBalancePresets: [(value: Float, label: String)] {
        [
            (2500, "白炽灯"), (3200, "暖光"), (4000, "荧光灯"),
            (5000, "日光"), (5500, "晴天"), (6500, "阴天"),
            (7500, "阴影"), (8000, "冷色")
        ]
    }

    private init() {}

    // MARK: - 对焦控制

    func setFocusPoint(_ point: CGPoint, in bounds: CGRect, device: AVCaptureDevice) throws {
        try device.lockForConfiguration()
        defer { device.unlockForConfiguration() }

        if device.isFocusPointOfInterestSupported {
            device.focusPointOfInterest = point
            device.focusMode = .autoFocus
        }
        if device.isExposurePointOfInterestSupported {
            device.exposurePointOfInterest = point
            device.exposureMode = .autoExpose
        }
    }

    func setManualFocus(lensPosition: Float, device: AVCaptureDevice) throws {
        guard device.isLockingFocusWithCustomLensPositionSupported else { return }
        try device.lockForConfiguration()
        defer { device.unlockForConfiguration() }

        device.setFocusModeLocked(lensPosition: lensPosition)
        focusLensPosition = lensPosition
        manualFocusEnabled = true
        afLocked = true
    }

    // MARK: - 曝光控制

    func setManualExposure(iso: Float, duration: CMTime, device: AVCaptureDevice) throws {
        try device.lockForConfiguration()
        defer { device.unlockForConfiguration() }

        let clampedISO = max(device.activeFormat.minISO, min(device.activeFormat.maxISO, iso))
        let clampedDuration = max(device.activeFormat.minExposureDuration,
                                   min(device.activeFormat.maxExposureDuration, duration))

        device.setExposureModeCustom(duration: clampedDuration, iso: clampedISO)
        self.iso = clampedISO
        self.shutterSpeed = clampedDuration
        manualExposureEnabled = true
        aeLocked = true
    }

    func setExposureBias(_ bias: Float, device: AVCaptureDevice) throws {
        try device.lockForConfiguration()
        defer { device.unlockForConfiguration() }

        let clamped = max(device.minExposureTargetBias, min(device.maxExposureTargetBias, bias))
        device.setExposureTargetBias(clamped)
        exposureBias = clamped
    }

    // MARK: - 白平衡控制

    func setManualWhiteBalance(temperature: Float, device: AVCaptureDevice) throws {
        try device.lockForConfiguration()
        defer { device.unlockForConfiguration() }

        let gains = device.deviceWhiteBalanceGains
        let clamped = max(2500, min(8000, temperature))
        // 简化的色温到增益映射
        let ratio = clamped / 5500
        let adjusted = AVCaptureDevice.WhiteBalanceGains(
            redGain: gains.redGain * (1.0 / ratio),
            greenGain: gains.greenGain,
            blueGain: gains.blueGain * ratio
        )
        let clampedGains = AVCaptureDevice.WhiteBalanceGains(
            redGain: max(1.0, min(device.maxWhiteBalanceGain, adjusted.redGain)),
            greenGain: max(1.0, min(device.maxWhiteBalanceGain, adjusted.greenGain)),
            blueGain: max(1.0, min(device.maxWhiteBalanceGain, adjusted.blueGain))
        )

        device.setWhiteBalanceModeLocked(with: clampedGains)
        colorTemperature = clamped
        manualWhiteBalanceEnabled = true
    }

    // MARK: - AE/AF 锁定

    func toggleAELock(device: AVCaptureDevice) throws {
        try device.lockForConfiguration()
        defer { device.unlockForConfiguration() }

        aeLocked.toggle()
        if aeLocked {
            device.exposureMode = .locked
        } else {
            device.exposureMode = .continuousAutoExposure
        }
    }

    func toggleAFLock(device: AVCaptureDevice) throws {
        try device.lockForConfiguration()
        defer { device.unlockForConfiguration() }

        afLocked.toggle()
        if afLocked {
            device.focusMode = .locked
        } else {
            device.focusMode = .continuousAutoFocus
        }
    }

    // MARK: - 直方图

    func computeHistogram(from pixelBuffer: CVPixelBuffer) {
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)

        // 使用 CIAreaHistogram 计算亮度直方图
        let histogram = ciImage.applyingFilter("CIAreaHistogram", parameters: [
            kCIInputExtentKey: CIVector(cgRect: ciImage.extent),
            "inputCount": 256,
            "inputScale": 1.0
        ])

        var data = [Float](repeating: 0, count: 256)
        let context = CIContext()
        context.render(histogram, toBitmap: &data, rowBytes: 256 * MemoryLayout<Float>.size,
                       bounds: CGRect(x: 0, y: 0, width: 256, height: 1),
                       format: .A8, colorSpace: nil)

        DispatchQueue.main.async {
            self.histogramData = data
        }
    }

    // MARK: - 重置

    func resetAllManualSettings(device: AVCaptureDevice) throws {
        try device.lockForConfiguration()
        defer { device.unlockForConfiguration() }

        device.focusMode = .continuousAutoFocus
        device.exposureMode = .continuousAutoExposure
        device.whiteBalanceMode = .continuousAutoWhiteBalance
        device.setExposureTargetBias(0)

        manualFocusEnabled = false
        manualExposureEnabled = false
        manualWhiteBalanceEnabled = false
        aeLocked = false
        afLocked = false
        exposureBias = 0
        focusLensPosition = 0.5
    }
}

#endif