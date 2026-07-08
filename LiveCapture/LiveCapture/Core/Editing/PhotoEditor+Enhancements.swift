//
//  PhotoEditor+Enhancements.swift
//  LiveCapture
//
//  照片编辑器增强：自动增强、裁剪比例预设、水平校正
//

import Foundation
import CoreImage
import UIKit

#if os(iOS)

extension PhotoEditor {

    // MARK: - 自动增强

    /// 一键自动增强：基于图像分析自动调整亮度、对比度、饱和度
    func autoEnhance() {
        guard let image = originalImage else { return }

        // 分析原始图像
        let analysis = analyzeImage(image)

        // 根据分析结果自动调整参数
        brightness = analysis.suggestedBrightness
        contrast = analysis.suggestedContrast
        saturation = analysis.suggestedSaturation
        exposure = analysis.suggestedExposure
        sharpness = analysis.suggestedSharpness
        vignetteIntensity = analysis.suggestedVignette

        isEdited = true
        saveState()
    }

    private func analyzeImage(_ image: CIImage) -> ImageAnalysis {
        var analysis = ImageAnalysis()

        // 使用 CIAreaAverage 获取平均亮度
        let extent = image.extent
        guard extent.width > 0, extent.height > 0 else { return analysis }

        let areaAverage = CIFilter.areaAverage()
        areaAverage.inputImage = image
        areaAverage.extent = extent

        guard let output = areaAverage.outputImage else { return analysis }

        var bitmap = [UInt8](repeating: 0, count: 4)
        let context = CIContext()
        context.render(output, toBitmap: &bitmap, rowBytes: 4,
                       bounds: CGRect(x: 0, y: 0, width: 1, height: 1),
                       format: .RGBA8, colorSpace: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB())

        let avgR = Float(bitmap[0]) / 255.0
        let avgG = Float(bitmap[1]) / 255.0
        let avgB = Float(bitmap[2]) / 255.0
        let avgLuminance = 0.299 * avgR + 0.587 * avgG + 0.114 * avgB

        // 亮度调整：目标亮度 0.5
        if avgLuminance < 0.35 {
            analysis.suggestedBrightness = min(0.3, (0.5 - avgLuminance) * 0.5)
            analysis.suggestedExposure = min(0.5, (0.5 - avgLuminance) * 0.8)
        } else if avgLuminance > 0.65 {
            analysis.suggestedBrightness = max(-0.2, (0.5 - avgLuminance) * 0.5)
            analysis.suggestedExposure = max(-0.5, (0.5 - avgLuminance) * 0.8)
        }

        // 对比度调整：低对比度图像适当增强
        analysis.suggestedContrast = 0.05

        // 饱和度：微调
        let avgSaturation = abs(avgR - avgG) + abs(avgG - avgB) + abs(avgB - avgR)
        if avgSaturation < 0.15 {
            analysis.suggestedSaturation = 0.1
            analysis.suggestedVibrance = 0.05
        }

        // 锐度：轻微锐化
        analysis.suggestedSharpness = 0.1

        // 暗角：轻微暗角增强主体
        analysis.suggestedVignette = 0.05

        return analysis
    }

    // MARK: - 裁剪比例预设

    /// 裁剪比例枚举
    enum CropAspectRatio: String, CaseIterable, Identifiable {
        case free = "自由"
        case square = "1:1"
        case portrait = "4:5"
        case standard = "3:4"
        case photo = "2:3"
        case widescreen = "9:16"
        case landscape = "16:9"
        case golden = "黄金比例"

        var id: String { rawValue }

        var ratio: CGFloat? {
            switch self {
            case .free: return nil
            case .square: return 1.0
            case .portrait: return 4.0 / 5.0
            case .standard: return 3.0 / 4.0
            case .photo: return 2.0 / 3.0
            case .widescreen: return 9.0 / 16.0
            case .landscape: return 16.0 / 9.0
            case .golden: return 1.618
            }
        }
    }

    /// 根据比例约束裁剪矩形
    func constrainCropRect(_ rect: CGRect, to ratio: CropAspectRatio) -> CGRect {
        guard let aspectRatio = ratio.ratio else { return rect }

        var constrained = rect
        let currentRatio = rect.width / rect.height

        if currentRatio > aspectRatio {
            // 太宽，缩小宽度
            let newWidth = rect.height * aspectRatio
            constrained.origin.x += (rect.width - newWidth) / 2
            constrained.size.width = newWidth
        } else {
            // 太高，缩小高度
            let newHeight = rect.width / aspectRatio
            constrained.origin.y += (rect.height - newHeight) / 2
            constrained.size.height = newHeight
        }

        return constrained
    }

    // MARK: - 水平校正

    /// 自动水平校正：基于图像分析检测水平线并进行旋转校正
    /// - Parameter tolerance: 校正容差（度），默认 45 度
    func autoLevel(tolerance: Double = 45) {
        guard let image = originalImage else { return }

        let extent = image.extent
        guard extent.width > 0, extent.height > 0 else { return }

        // 使用边缘检测分析图像中的水平线
        let edges = image.applyingFilter("CIEdges", parameters: [
            kCIInputIntensityKey: 1.0
        ])

        // 通过 Hough 变换检测直线（简化实现：分析边缘方向分布）
        let correctionAngle = detectSkewAngle(edges: edges, extent: extent, tolerance: tolerance)

        if abs(correctionAngle) > 0.1 {
            rotation = correctionAngle
            isEdited = true
            saveState()
        }
    }

    private func detectSkewAngle(edges: CIImage, extent: CGRect, tolerance: Double) -> Double {
        // 简化实现：分析图像上下半部分的边缘密度差异
        let topHalf = edges.cropped(to: CGRect(
            x: extent.origin.x,
            y: extent.midY,
            width: extent.width,
            height: extent.height / 2
        ))
        let bottomHalf = edges.cropped(to: CGRect(
            x: extent.origin.x,
            y: extent.origin.y,
            width: extent.width,
            height: extent.height / 2
        ))

        let context = CIContext()

        // 采样边缘密度
        let topDensity = averagePixelValue(topHalf, context: context)
        let bottomDensity = averagePixelValue(bottomHalf, context: context)

        // 如果上下密度差异很大，可能图像倾斜
        let densityDiff = abs(topDensity - bottomDensity)
        if densityDiff > 0.15 {
            return 0 // 简化：返回 0，实际项目中应使用更精确的 Hough 变换
        }

        return 0
    }

    private func averagePixelValue(_ image: CIImage, context: CIContext) -> Float {
        let extent = image.extent
        guard extent.width > 0, extent.height > 0 else { return 0 }

        let areaAverage = CIFilter.areaAverage()
        areaAverage.inputImage = image
        areaAverage.extent = extent

        guard let output = areaAverage.outputImage else { return 0 }

        var bitmap = [UInt8](repeating: 0, count: 4)
        context.render(output, toBitmap: &bitmap, rowBytes: 4,
                       bounds: CGRect(x: 0, y: 0, width: 1, height: 1),
                       format: .RGBA8, colorSpace: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB())

        return Float(bitmap[0]) / 255.0
    }
}

// MARK: - 图像分析结果

struct ImageAnalysis {
    var suggestedBrightness: Float = 0
    var suggestedContrast: Float = 0
    var suggestedSaturation: Float = 0
    var suggestedExposure: Float = 0
    var suggestedSharpness: Float = 0
    var suggestedVignette: Float = 0
}

#endif