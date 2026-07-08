//
//  LutFilterProcessor.swift
//  LiveCapture
//
//  LUT 滤镜处理器 - 兼容层，委托给 FilterProcessor 实现
//
//  ## 文件作用
//  保持与旧代码的兼容性，内部委托给 FilterProcessor 处理
//  支持 CIFilter 链 + 色调曲线 + 颜色矩阵的完整滤镜处理
//
//  ## 主要类
//  - LutFilterProcessor: 滤镜处理器（兼容层）
//
//  ## CIFilter 链顺序
//  1. CITemperatureAndTint - 色温色调调整
//  2. CIExposureAdjust - 曝光调整
//  3. CIColorControls - 亮度、对比度、饱和度
//  4. CIVibrance - 自然饱和度
//  5. CIHighlightShadowAdjust - 高光阴影
//  6. CIColorMonochrome（可选）- 黑白转换
//  7. CIToneCurve（可选）- RGB 色调曲线
//  8. CIColorMatrix（可选）- 颜色矩阵变换
//
//  ## 强度混合
//  使用 CIBlendWithAlphaMask 实现强度控制
//
//  ## 性能优化
//  - 委托给 FilterProcessor.shared 复用 GPU 资源
//  - 支持 Metal 加速
//

import Foundation
import CoreImage
import UIKit
import CoreVideo
import Metal

#if os(iOS)

/// LUT 滤镜处理器 - 兼容层，委托给 FilterProcessor
final class LutFilterProcessor {

    // MARK: - 属性

    /// 委托的 FilterProcessor 实例
    private let processor: FilterProcessor

    /// Core Image 上下文（兼容旧代码）
    private let context: CIContext
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()

    // MARK: - 初始化

    init() {
        processor = FilterProcessor.shared
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "LutFilterProcessor"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "LutFilterProcessor"
            ])
        }
    }

    // MARK: - CIImage 处理

    /// 对 CIImage 应用滤镜
    func applyFilter(to image: CIImage, preset: LutFilterPreset, intensity: Float = 1.0) -> CIImage {
        let clampedIntensity = max(0, min(1, intensity))
        guard clampedIntensity > 0.001 else { return image }

        let filtered = applyFilterChain(to: image, parameters: preset.parameters)
        guard clampedIntensity < 0.999 else { return filtered }

        return blendImages(original: image, filtered: filtered, intensity: clampedIntensity)
    }

    /// 对 CIImage 应用滤镜参数链
    private func applyFilterChain(to image: CIImage, parameters: FilterParameters) -> CIImage {
        var output = image

        // 1. 色温色调调整
        if abs(parameters.temperature) > 1 || abs(parameters.tint) > 0.5 {
            output = applyTemperatureAndTint(to: output,
                                             temperature: parameters.temperature,
                                             tint: parameters.tint)
        }

        // 2. 曝光调整
        if abs(parameters.exposure) > 0.01 {
            output = applyExposureAdjust(to: output, ev: parameters.exposure)
        }

        // 3. 颜色控制
        if parameters.brightness != 0 || parameters.contrast != 1.0 || parameters.saturation != 1.0 {
            output = applyColorControls(to: output,
                                        brightness: parameters.brightness,
                                        contrast: parameters.contrast,
                                        saturation: parameters.saturation)
        }

        // 4. 自然饱和度
        if abs(parameters.vibrance) > 0.01 {
            output = applyVibrance(to: output, amount: parameters.vibrance)
        }

        // 5. 高光阴影调整
        if parameters.highlightAmount < 0.99 || abs(parameters.shadowAmount) > 0.01 {
            output = applyHighlightShadow(to: output,
                                          highlightAmount: parameters.highlightAmount,
                                          shadowAmount: parameters.shadowAmount)
        }

        // 6. 黑白模式
        if parameters.isMonochrome && parameters.monochromeIntensity > 0.01 {
            let monoColor = CIColor(
                red: CGFloat(parameters.monochromeColorR),
                green: CGFloat(parameters.monochromeColorG),
                blue: CGFloat(parameters.monochromeColorB)
            )
            output = applyMonochrome(to: output,
                                     color: monoColor,
                                     intensity: parameters.monochromeIntensity)
        }

        // 7. 色调曲线
        if parameters.useToneCurve {
            output = applyToneCurve(to: output,
                                    rCurve: parameters.toneCurveR,
                                    gCurve: parameters.toneCurveG,
                                    bCurve: parameters.toneCurveB)
        }

        // 8. 颜色矩阵
        if parameters.useColorMatrix {
            output = applyColorMatrix(to: output, parameters: parameters)
        }

        return output
    }

    // MARK: - 单个 CIFilter 应用

    private func applyTemperatureAndTint(to image: CIImage, temperature: Float, tint: Float) -> CIImage {
        guard let filter = CIFilter(name: "CITemperatureAndTint") else { return image }
        let neutralVector = CIVector(x: 6500, y: 0)
        let targetVector = CIVector(x: CGFloat(6500 + temperature), y: CGFloat(tint))
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(neutralVector, forKey: "inputNeutral")
        filter.setValue(targetVector, forKey: "inputTargetNeutral")
        return filter.outputImage ?? image
    }

    private func applyExposureAdjust(to image: CIImage, ev: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIExposureAdjust") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(ev, forKey: kCIInputEVKey)
        return filter.outputImage ?? image
    }

    private func applyColorControls(to image: CIImage, brightness: Float, contrast: Float, saturation: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIColorControls") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(brightness, forKey: kCIInputBrightnessKey)
        filter.setValue(contrast, forKey: kCIInputContrastKey)
        filter.setValue(saturation, forKey: kCIInputSaturationKey)
        return filter.outputImage ?? image
    }

    private func applyVibrance(to image: CIImage, amount: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIVibrance") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(amount, forKey: "inputAmount")
        return filter.outputImage ?? image
    }

    private func applyHighlightShadow(to image: CIImage, highlightAmount: Float, shadowAmount: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIHighlightShadowAdjust") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(highlightAmount, forKey: "inputHighlightAmount")
        filter.setValue(shadowAmount, forKey: "inputShadowAmount")
        return filter.outputImage ?? image
    }

    private func applyMonochrome(to image: CIImage, color: CIColor, intensity: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIColorMonochrome") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(color, forKey: kCIInputColorKey)
        filter.setValue(intensity, forKey: kCIInputIntensityKey)
        return filter.outputImage ?? image
    }

    /// 色调曲线（RGB 各通道独立曲线）
    private func applyToneCurve(
        to image: CIImage,
        rCurve: [Float],
        gCurve: [Float],
        bCurve: [Float]
    ) -> CIImage {
        guard let filter = CIFilter(name: "CIToneCurve") else { return image }
        let xPoints: [CGFloat] = [0.0, 0.25, 0.5, 0.75, 1.0]
        let rValues = rCurve.map { CGFloat($0) }
        let gValues = gCurve.map { CGFloat($0) }
        let bValues = bCurve.map { CGFloat($0) }

        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(CIVector(values: xPoints, count: 5), forKey: "inputPoint0")
        filter.setValue(CIVector(values: rValues, count: 5), forKey: "inputPoint1")
        filter.setValue(CIVector(values: gValues, count: 5), forKey: "inputPoint2")
        filter.setValue(CIVector(values: bValues, count: 5), forKey: "inputPoint3")
        filter.setValue(CIVector(values: xPoints, count: 5), forKey: "inputPoint4")

        return filter.outputImage ?? image
    }

    /// 颜色矩阵变换
    private func applyColorMatrix(to image: CIImage, parameters: FilterParameters) -> CIImage {
        guard let filter = CIFilter(name: "CIColorMatrix") else { return image }

        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(CIVector(
            x: CGFloat(parameters.colorMatrixRR),
            y: CGFloat(parameters.colorMatrixRG),
            z: CGFloat(parameters.colorMatrixRB),
            w: CGFloat(parameters.colorMatrixRA)
        ), forKey: "inputRVector")
        filter.setValue(CIVector(
            x: CGFloat(parameters.colorMatrixGR),
            y: CGFloat(parameters.colorMatrixGG),
            z: CGFloat(parameters.colorMatrixGB),
            w: CGFloat(parameters.colorMatrixGA)
        ), forKey: "inputGVector")
        filter.setValue(CIVector(
            x: CGFloat(parameters.colorMatrixBR),
            y: CGFloat(parameters.colorMatrixBG),
            z: CGFloat(parameters.colorMatrixBB),
            w: CGFloat(parameters.colorMatrixBA)
        ), forKey: "inputBVector")
        filter.setValue(CIVector(
            x: CGFloat(parameters.colorMatrixRBias),
            y: CGFloat(parameters.colorMatrixGBias),
            z: CGFloat(parameters.colorMatrixBBias),
            w: 0
        ), forKey: "inputBiasVector")

        return filter.outputImage ?? image
    }

    // MARK: - 强度混合

    private func blendImages(original: CIImage, filtered: CIImage, intensity: Float) -> CIImage {
        let extent = filtered.extent
        guard let constantColor = CIFilter(name: "CIConstantColorGenerator") else { return filtered }
        let maskColor = CIColor(red: 1, green: 1, blue: 1, alpha: CGFloat(intensity))
        constantColor.setValue(maskColor, forKey: kCIInputColorKey)
        guard let maskImage = constantColor.outputImage else { return filtered }
        let mask = maskImage.cropped(to: extent)

        guard let blend = CIFilter(name: "CIBlendWithMask") else { return filtered }
        blend.setValue(filtered, forKey: kCIInputImageKey)
        blend.setValue(original, forKey: kCIInputBackgroundImageKey)
        blend.setValue(mask, forKey: kCIInputMaskImageKey)

        return blend.outputImage ?? filtered
    }

    // MARK: - CVPixelBuffer 处理

    func applyFilter(to pixelBuffer: CVPixelBuffer, preset: LutFilterPreset, intensity: Float = 1.0) -> CVPixelBuffer? {
        return processor.applyFilter(to: pixelBuffer, preset: preset, intensity: intensity)
    }

    // MARK: - 快速预览

    func quickPreview(pixelBuffer: CVPixelBuffer, preset: LutFilterPreset) -> UIImage? {
        return processor.quickPreview(pixelBuffer: pixelBuffer, preset: preset)
    }

    func quickPreview(ciImage: CIImage, preset: LutFilterPreset) -> UIImage? {
        return processor.quickPreview(ciImage: ciImage, preset: preset)
    }

    // MARK: - 缩略图预览

    func thumbnailPreview(image: UIImage, preset: LutFilterPreset, targetSize: CGSize) -> UIImage? {
        return processor.thumbnailPreview(image: image, preset: preset, targetSize: targetSize)
    }
}

#endif