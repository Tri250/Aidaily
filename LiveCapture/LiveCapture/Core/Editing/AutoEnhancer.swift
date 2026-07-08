//
//  AutoEnhancer.swift
//  LiveCapture
//
//  自动增强器 - 基于图像分析的自适应增强，自动白平衡、曝光、锐化
//
//  ## 主要功能
//  - autoEnhance: 一键自动增强图像
//  - analyzeHistogram: 使用 CIAreaHistogram 分析像素分布
//  - autoWhiteBalance: 灰度世界假设法自动白平衡
//  - autoLevels: 直方图拉伸实现自动色阶
//  - smartExposure: 智能曝光补偿
//  - adaptiveSharpening: 自适应锐化
//

import Foundation
import CoreImage
import Accelerate
import UIKit

#if os(iOS)

/// 直方图分析结果
struct HistogramData {
    /// 最小像素值（0-255）
    let min: Float
    /// 最大像素值（0-255）
    let max: Float
    /// 平均像素值（0-255）
    let mean: Float
    /// 中位数像素值（0-255）
    let median: Float
    /// 是否曝光不足
    let isUnderexposed: Bool
    /// 是否过曝
    let isOverexposed: Bool
    /// 各通道平均值
    let avgR: Float
    let avgG: Float
    let avgB: Float
}

/// 自动增强器
final class AutoEnhancer {

    // MARK: - 私有属性

    private let context: CIContext
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "AutoEnhancer"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "AutoEnhancer"
            ])
        }
    }

    // MARK: - 自动增强

    /// 一键自动增强图像
    /// - Parameter image: 输入 CIImage
    /// - Returns: 增强后的 CIImage
    func autoEnhance(_ image: CIImage) -> CIImage {
        var output = image

        // 1. 分析直方图
        let histogram = analyzeHistogram(image)

        // 2. 自动白平衡
        output = autoWhiteBalance(output)

        // 3. 自动色阶（直方图拉伸）
        output = autoLevels(output, histogram: histogram)

        // 4. 智能曝光补偿
        output = smartExposure(output, histogram: histogram)

        // 5. 自适应锐化
        output = adaptiveSharpening(output, histogram: histogram)

        return output
    }

    /// 从 UIImage 自动增强
    func autoEnhanceUIImage(_ uiImage: UIImage) -> UIImage? {
        guard let ciImage = uiImage.ciImage ?? CIImage(image: uiImage) else { return nil }
        let enhanced = autoEnhance(ciImage)

        guard let cgImage = context.createCGImage(enhanced, from: enhanced.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }

    // MARK: - 直方图分析

    /// 使用 CIAreaHistogram 分析图像直方图
    /// - Parameter image: 输入 CIImage
    /// - Returns: 直方图分析结果
    private func analyzeHistogram(_ image: CIImage) -> HistogramData {
        let extent = image.extent

        // 使用 CIAreaHistogram 获取直方图
        guard let histogramFilter = CIFilter(name: "CIAreaHistogram", parameters: [
            kCIInputImageKey: image,
            kCIInputExtentKey: CIVector(cgRect: extent),
            "inputCount": 256,
            "inputScale": 1.0
        ]), let histogramImage = histogramFilter.outputImage else {
            return HistogramData(min: 0, max: 255, mean: 128, median: 128,
                                 isUnderexposed: false, isOverexposed: false,
                                 avgR: 128, avgG: 128, avgB: 128)
        }

        // 渲染直方图数据
        var histogramData = [UInt8](repeating: 0, count: 256 * 4)
        let bitmapFormat = CGBitmapInfo(rawValue: CGImageAlphaInfo.noneSkipFirst.rawValue)
        context.render(histogramImage, toBitmap: &histogramData, rowBytes: 256 * 4,
                       bounds: CGRect(x: 0, y: 0, width: 256, height: 1),
                       format: .ARGB8, colorSpace: colorSpace)

        // 从直方图数据计算统计量
        var rCounts = [UInt32](repeating: 0, count: 256)
        var gCounts = [UInt32](repeating: 0, count: 256)
        var bCounts = [UInt32](repeating: 0, count: 256)

        for i in 0..<256 {
            let offset = i * 4
            bCounts[i] = UInt32(histogramData[offset + 1])  // B
            gCounts[i] = UInt32(histogramData[offset + 2])  // G
            rCounts[i] = UInt32(histogramData[offset + 3])  // R
        }

        // 计算各通道平均值
        let avgR = computeAverageChannel(counts: rCounts)
        let avgG = computeAverageChannel(counts: gCounts)
        let avgB = computeAverageChannel(counts: bCounts)

        // 计算总亮度的最小/最大/平均/中位数
        var totalCounts = [UInt32](repeating: 0, count: 256)
        for i in 0..<256 {
            totalCounts[i] = rCounts[i] + gCounts[i] + bCounts[i]
        }

        let totalPixels = totalCounts.reduce(0, +)
        guard totalPixels > 0 else {
            return HistogramData(min: 0, max: 255, mean: 128, median: 128,
                                 isUnderexposed: false, isOverexposed: false,
                                 avgR: 128, avgG: 128, avgB: 128)
        }

        // 最小非零值
        var minVal: Int = 0
        for i in 0..<256 {
            if totalCounts[i] > 0 { minVal = i; break }
        }

        // 最大非零值
        var maxVal: Int = 255
        for i in (0..<256).reversed() {
            if totalCounts[i] > 0 { maxVal = i; break }
        }

        // 平均值
        var sum: UInt64 = 0
        for i in 0..<256 {
            sum += UInt64(i) * UInt64(totalCounts[i])
        }
        let mean = Float(sum) / Float(totalPixels)

        // 中位数
        let halfCount = totalPixels / 2
        var cumulative: UInt32 = 0
        var medianVal: Int = 128
        for i in 0..<256 {
            cumulative += totalCounts[i]
            if cumulative >= halfCount { medianVal = i; break }
        }

        let isUnderexposed = mean < 85
        let isOverexposed = mean > 170

        return HistogramData(
            min: Float(minVal),
            max: Float(maxVal),
            mean: mean,
            median: Float(medianVal),
            isUnderexposed: isUnderexposed,
            isOverexposed: isOverexposed,
            avgR: Float(avgR),
            avgG: Float(avgG),
            avgB: Float(avgB)
        )
    }

    /// 计算单通道平均值
    private func computeAverageChannel(counts: [UInt32]) -> Float {
        var totalPixels: UInt64 = 0
        var sum: UInt64 = 0
        for i in 0..<counts.count {
            let count = UInt64(counts[i])
            totalPixels += count
            sum += UInt64(i) * count
        }
        guard totalPixels > 0 else { return 128 }
        return Float(sum) / Float(totalPixels)
    }

    // MARK: - 自动白平衡

    /// 灰度世界假设法自动白平衡
    /// - Parameter image: 输入 CIImage
    /// - Returns: 白平衡后的 CIImage
    private func autoWhiteBalance(_ image: CIImage) -> CIImage {
        let histogram = analyzeHistogram(image)

        // 灰度世界假设：所有颜色的平均值应该为灰色
        let avgGray = (histogram.avgR + histogram.avgG + histogram.avgB) / 3.0

        guard avgGray > 0 else { return image }

        let rGain = avgGray / max(histogram.avgR, 1)
        let gGain = avgGray / max(histogram.avgG, 1)
        let bGain = avgGray / max(histogram.avgB, 1)

        // 限制增益范围，避免过度修正
        let clampedRGain = max(0.5, min(2.0, rGain))
        let clampedGGain = max(0.5, min(2.0, gGain))
        let clampedBGain = max(0.5, min(2.0, bGain))

        // 使用 CIColorMatrix 进行白平衡调整
        guard let colorMatrixFilter = CIFilter(name: "CIColorMatrix") else { return image }
        colorMatrixFilter.setValue(image, forKey: kCIInputImageKey)

        // R 向量
        colorMatrixFilter.setValue(CIVector(x: CGFloat(clampedRGain), y: 0, z: 0, w: 0), forKey: "inputRVector")
        // G 向量
        colorMatrixFilter.setValue(CIVector(x: 0, y: CGFloat(clampedGGain), z: 0, w: 0), forKey: "inputGVector")
        // B 向量
        colorMatrixFilter.setValue(CIVector(x: 0, y: 0, z: CGFloat(clampedBGain), w: 0), forKey: "inputBVector")
        // Bias 向量
        let rBias = (1.0 - clampedRGain) * 0.5
        let gBias = (1.0 - clampedGGain) * 0.5
        let bBias = (1.0 - clampedBGain) * 0.5
        colorMatrixFilter.setValue(CIVector(x: CGFloat(rBias), y: CGFloat(gBias), z: CGFloat(bBias), w: 0), forKey: "inputBiasVector")

        return colorMatrixFilter.outputImage ?? image
    }

    // MARK: - 自动色阶

    /// 直方图拉伸实现自动色阶
    private func autoLevels(_ image: CIImage, histogram: HistogramData) -> CIImage {
        let minVal = histogram.min
        let maxVal = histogram.max

        // 如果直方图范围已经足够宽，不需要拉伸
        guard maxVal > minVal && (maxVal - minVal) < 200 else { return image }

        // 计算映射：将 [minVal, maxVal] 映射到 [0, 255]
        let scale = 255.0 / (maxVal - minVal)
        let offset = -minVal * scale / 255.0

        // 使用 CIColorControls 进行亮度和对比度调整模拟色阶
        let contrastAdjust = (scale / 255.0 - 1.0) * 0.5  // 缩放到合理范围
        let brightnessAdjust = offset * 0.5

        guard let colorControls = CIFilter(name: "CIColorControls") else { return image }
        colorControls.setValue(image, forKey: kCIInputImageKey)
        colorControls.setValue(brightnessAdjust, forKey: kCIInputBrightnessKey)
        colorControls.setValue(1.0 + contrastAdjust, forKey: kCIInputContrastKey)
        colorControls.setValue(1.0, forKey: kCIInputSaturationKey)

        return colorControls.outputImage ?? image
    }

    // MARK: - 智能曝光

    /// 智能曝光补偿
    private func smartExposure(_ image: CIImage, histogram: HistogramData) -> CIImage {
        var ev: Float = 0

        if histogram.isUnderexposed {
            // 曝光不足：提升曝光
            ev = (85.0 - histogram.mean) / 85.0 * 1.5  // 最多 +1.5 EV
            ev = min(ev, 1.5)
        } else if histogram.isOverexposed {
            // 过曝：降低曝光
            ev = (170.0 - histogram.mean) / 85.0 * 1.0  // 最多 -1.0 EV
            ev = max(ev, -1.0)
        }

        guard abs(ev) > 0.05 else { return image }

        guard let exposureFilter = CIFilter(name: "CIExposureAdjust") else { return image }
        exposureFilter.setValue(image, forKey: kCIInputImageKey)
        exposureFilter.setValue(ev, forKey: kCIInputEVKey)

        return exposureFilter.outputImage ?? image
    }

    // MARK: - 自适应锐化

    /// 自适应锐化（根据图像内容调整锐化强度）
    private func adaptiveSharpening(_ image: CIImage, histogram: HistogramData) -> CIImage {
        // 根据直方图对比度决定锐化强度
        let contrast = (histogram.max - histogram.min) / 255.0
        let sharpnessAmount: Float

        if contrast < 0.3 {
            // 低对比度 → 强锐化
            sharpnessAmount = 0.6
        } else if contrast < 0.6 {
            // 中等对比度 → 中等锐化
            sharpnessAmount = 0.4
        } else {
            // 高对比度 → 弱锐化
            sharpnessAmount = 0.2
        }

        guard let sharpenFilter = CIFilter(name: "CISharpenLuminance") else { return image }
        sharpenFilter.setValue(image, forKey: kCIInputImageKey)
        sharpenFilter.setValue(sharpnessAmount, forKey: kCIInputSharpnessKey)

        return sharpenFilter.outputImage ?? image
    }
}

// MARK: - Metal 导入

import Metal

#endif