//
//  VideoStabilizer.swift
//  LiveCapture
//
//  电子防抖模块
//
//  ## 文件作用
//  使用陀螺仪数据对视频帧进行实时电子防抖处理
//  通过检测设备姿态变化，对每帧应用反向仿射变换补偿抖动
//  实现类似运动相机的防抖效果
//
//  ## 主要类
//  ### VideoStabilizer
//  电子防抖处理器，使用 Core Motion 传感器数据
//
//  ## 工作原理
//  1. 启动时记录参考姿态（referenceAttitude）
//  2. 每帧获取当前设备姿态
//  3. 计算当前姿态与参考姿态的差异
//  4. 应用平滑处理减少高频抖动
//  5. 生成反向仿射变换补偿抖动
//  6. 对像素缓冲应用变换和裁剪
//
//  ## 姿态映射
//  - roll（翻滚）→ 旋转补偿
//  - pitch（俯仰）→ Y 轴平移
//  - yaw（偏航）→ X 轴平移
//
//  ## 平滑参数
//  - smoothingFactor: 0.8（低通滤波系数）
//  - maxRotation: ±5°（最大旋转补偿）
//  - maxTranslation: 图像尺寸的 5%（最大平移补偿）
//  - cropMargin: 10%（边缘裁剪比例，消除黑边）
//
//  ## 线程安全
//  - motionQueue: 串行队列处理传感器数据
//  - 帧处理在调用线程执行
//

import Foundation
import CoreMotion
import AVFoundation
import CoreImage
import simd

#if os(iOS)

/// 电子防抖处理器 - 使用陀螺仪数据补偿设备抖动
final class VideoStabilizer {

    // MARK: - 属性

    /// Core Motion 运动管理器
    private let motionManager = CMMotionManager()
    /// 参考姿态（录制开始时的设备姿态）
    private var referenceAttitude: CMAttitude?
    /// 平滑系数（0-1），值越大平滑越强但响应越慢
    private let smoothingFactor: Float = 0.8
    /// 当前平滑后的变换矩阵
    private var smoothedTransform: CGAffineTransform = .identity
    /// 上一个平滑姿态（用于低通滤波）
    private var lastSmoothedRoll: Double = 0
    private var lastSmoothedPitch: Double = 0
    private var lastSmoothedYaw: Double = 0
    /// 运动数据队列
    private let motionQueue = OperationQueue()
    /// 最大旋转角度（弧度，约 ±5°）
    private let maxRotation: Double = 5.0 * .pi / 180.0
    /// 最大平移比例（相对于图像尺寸）
    private let maxTranslationRatio: CGFloat = 0.05
    /// 裁剪边距比例（消除黑边）
    private let cropMargin: CGFloat = 0.10
    /// Core Image 上下文
    private let ciContext: CIContext
    /// 是否正在运行
    private var isRunning: Bool = false

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            ciContext = CIContext(mtlDevice: device, options: [
                .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB(),
                .name: "VideoStabilizer"
            ])
        } else {
            ciContext = CIContext(options: [
                .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB(),
                .name: "VideoStabilizer"
            ])
        }

        motionQueue.maxConcurrentOperationCount = 1
        motionQueue.qualityOfService = .userInitiated
    }

    // MARK: - 生命周期

    /// 启动防抖（开始采集陀螺仪数据）
    func startStabilization() {
        guard !isRunning, motionManager.isDeviceMotionAvailable else { return }

        // 启动设备运动采集（60Hz）
        motionManager.deviceMotionUpdateInterval = 1.0 / 60.0

        motionManager.startDeviceMotionUpdates(
            using: .xArbitraryZVertical, // 使用设备坐标系，Z轴垂直
            to: motionQueue
        ) { [weak self] motion, error in
            guard let self = self, let motion = motion, error == nil else { return }

            // 首次设置参考姿态
            if self.referenceAttitude == nil {
                self.referenceAttitude = motion.attitude
                self.lastSmoothedRoll = 0
                self.lastSmoothedPitch = 0
                self.lastSmoothedYaw = 0
            }
        }

        isRunning = true
    }

    /// 停止防抖
    func stopStabilization() {
        guard isRunning else { return }
        motionManager.stopDeviceMotionUpdates()
        referenceAttitude = nil
        smoothedTransform = .identity
        lastSmoothedRoll = 0
        lastSmoothedPitch = 0
        lastSmoothedYaw = 0
        isRunning = false
    }

    /// 重置参考姿态（用于切换场景）
    func resetReference() {
        referenceAttitude = nil
        smoothedTransform = .identity
        lastSmoothedRoll = 0
        lastSmoothedPitch = 0
        lastSmoothedYaw = 0
    }

    // MARK: - 帧处理

    /// 对像素缓冲应用防抖变换
    /// - Parameters:
    ///   - pixelBuffer: 输入像素缓冲
    ///   - timestamp: 帧时间戳
    /// - Returns: 防抖后的像素缓冲，失败返回 nil
    func stabilizeFrame(_ pixelBuffer: CVPixelBuffer, timestamp: CMTime) -> CVPixelBuffer? {
        guard isRunning, let referenceAttitude = referenceAttitude else {
            return pixelBuffer
        }

        // 1. 获取当前设备姿态
        guard let currentAttitude = motionManager.deviceMotion?.attitude else {
            return pixelBuffer
        }

        // 2. 计算姿态变化
        currentAttitude.multiply(byInverseOf: referenceAttitude)

        let deltaRoll = currentAttitude.roll
        let deltaPitch = currentAttitude.pitch
        let deltaYaw = currentAttitude.yaw

        // 3. 应用低通滤波平滑
        let smoothRoll = lowPassFilter(current: deltaRoll, previous: lastSmoothedRoll)
        let smoothPitch = lowPassFilter(current: deltaPitch, previous: lastSmoothedPitch)
        let smoothYaw = lowPassFilter(current: deltaYaw, previous: lastSmoothedYaw)

        lastSmoothedRoll = smoothRoll
        lastSmoothedPitch = smoothPitch
        lastSmoothedYaw = smoothYaw

        // 4. 限制旋转角度
        let clampedRoll = max(-maxRotation, min(maxRotation, smoothRoll))
        let clampedPitch = max(-maxRotation, min(maxRotation, smoothPitch))
        let clampedYaw = max(-maxRotation, min(maxRotation, smoothYaw))

        // 5. 计算平移补偿
        let width = CGFloat(CVPixelBufferGetWidth(pixelBuffer))
        let height = CGFloat(CVPixelBufferGetHeight(pixelBuffer))
        let maxTranslationX = width * maxTranslationRatio
        let maxTranslationY = height * maxTranslationRatio

        let tx = CGFloat(clampedYaw) * maxTranslationX / CGFloat(maxRotation)
        let ty = CGFloat(clampedPitch) * maxTranslationY / CGFloat(maxRotation)
        let clampedTx = max(-maxTranslationX, min(maxTranslationX, tx))
        let clampedTy = max(-maxTranslationY, min(maxTranslationY, ty))

        // 6. 构建防抖变换（反向补偿）
        // 先平移再旋转，围绕图像中心
        let imageCenter = CGPoint(x: width / 2, y: height / 2)

        var transform = CGAffineTransform.identity
        // 移动到中心
        transform = transform.translatedBy(x: imageCenter.x, y: imageCenter.y)
        // 旋转（roll 映射到 2D 旋转）
        transform = transform.rotated(by: -CGFloat(clampedRoll))
        // 平移补偿（反向）
        transform = transform.translatedBy(x: -clampedTx, y: -clampedTy)
        // 移回原点
        transform = transform.translatedBy(x: -imageCenter.x, y: -imageCenter.y)

        // 平滑变换矩阵
        smoothedTransform = lerpTransform(from: smoothedTransform, to: transform, t: CGFloat(1.0 - smoothingFactor))

        // 7. 应用变换到像素缓冲
        guard let result = applyTransform(smoothedTransform, to: pixelBuffer) else {
            return pixelBuffer
        }

        return result
    }

    // MARK: - 私有方法

    /// 低通滤波器
    private func lowPassFilter(current: Double, previous: Double) -> Double {
        return previous + Double(smoothingFactor) * (current - previous)
    }

    /// 线性插值变换矩阵
    private func lerpTransform(from: CGAffineTransform, to: CGAffineTransform, t: CGFloat) -> CGAffineTransform {
        return CGAffineTransform(
            a: from.a + (to.a - from.a) * t,
            b: from.b + (to.b - from.b) * t,
            c: from.c + (to.c - from.c) * t,
            d: from.d + (to.d - from.d) * t,
            tx: from.tx + (to.tx - from.tx) * t,
            ty: from.ty + (to.ty - from.ty) * t
        )
    }

    /// 对像素缓冲应用仿射变换
    private func applyTransform(_ transform: CGAffineTransform, to pixelBuffer: CVPixelBuffer) -> CVPixelBuffer? {
        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)

        // 如果需要裁剪黑边，先放大再裁剪
        let scale = 1.0 + cropMargin * 2
        let scaledTransform = transform.scaledBy(x: scale, y: scale)

        let ciImage = CIImage(cvPixelBuffer: pixelBuffer).transformed(by: scaledTransform)

        // 裁剪回原始尺寸（去除黑边）
        let cropRect = CGRect(
            x: CGFloat(width) * cropMargin,
            y: CGFloat(height) * cropMargin,
            width: CGFloat(width),
            height: CGFloat(height)
        )

        // 创建输出像素缓冲
        var outputBuffer: CVPixelBuffer?
        let pixelFormat = CVPixelBufferGetPixelFormatType(pixelBuffer)
        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            pixelFormat,
            nil,
            &outputBuffer
        )

        guard status == kCVReturnSuccess, let output = outputBuffer else {
            return nil
        }

        ciContext.render(ciImage.cropped(to: cropRect), to: output)

        return output
    }

    // MARK: - 姿态查询

    /// 当前设备是否稳定（旋转幅度很小）
    var isCurrentlyStable: Bool {
        guard let attitude = motionManager.deviceMotion?.attitude,
              let reference = referenceAttitude else {
            return true
        }

        guard let currentAttitude = attitude.copy() as? CMAttitude else { return true }
        currentAttitude.multiply(byInverseOf: reference)

        let rotationThreshold: Double = 0.5 * .pi / 180.0 // 0.5°
        return abs(currentAttitude.roll) < rotationThreshold &&
               abs(currentAttitude.pitch) < rotationThreshold &&
               abs(currentAttitude.yaw) < rotationThreshold
    }

    /// 当前旋转幅度（弧度）
    var currentRotationMagnitude: Double {
        guard let attitude = motionManager.deviceMotion?.attitude,
              let reference = referenceAttitude else {
            return 0
        }

        guard let currentAttitude = attitude.copy() as? CMAttitude else { return 0 }
        currentAttitude.multiply(byInverseOf: reference)

        return sqrt(
            currentAttitude.roll * currentAttitude.roll +
            currentAttitude.pitch * currentAttitude.pitch +
            currentAttitude.yaw * currentAttitude.yaw
        )
    }
}

// MARK: - Metal 导入

import Metal

#endif