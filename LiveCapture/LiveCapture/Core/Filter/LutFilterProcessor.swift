//
//  LutFilterProcessor.swift
//  LiveCapture
//
//  LUT 滤镜处理器
//
//  ## 文件作用
//  使用 Core Image 的 CIFilter 链对图像和像素缓冲应用滤镜效果
//  支持强度混合（intensity blending），在原始图像和滤镜效果之间平滑过渡
//  支持 CVPixelBuffer 实时预览和 UIImage 输出
//
//  ## 主要类
//  - LutFilterProcessor: 滤镜处理器，使用 CIFilter 链实现预设滤镜
//
//  ## CIFilter 链顺序
//  1. CITemperatureAndTint - 色温色调调整
//  2. CIExposureAdjust - 曝光调整
//  3. CIColorControls - 亮度、对比度、饱和度
//  4. CIVibrance - 自然饱和度
//  5. CIHighlightShadowAdjust - 高光阴影
//  6. CIColorMonochrome（可选）- 黑白转换
//
//  ## 强度混合
//  使用 CIBlendWithAlphaMask 或手动混合实现强度控制
//  intensity = 1.0 时完全应用滤镜，intensity = 0.0 时返回原图
//
//  ## 性能优化
//  - 使用单个 CIContext 实例复用 GPU 资源
//  - colorSpace 使用 sRGB 确保颜色一致性
//  - 支持 Metal 加速（默认）
//
//  ## 线程安全
//  - CIContext 是线程安全的
//  - 每次调用创建新的 CIImage 链，无共享状态
//

import Foundation
import CoreImage
import UIKit
import CoreVideo

#if os(iOS)

/// LUT 滤镜处理器 - 使用 CIFilter 链实现滤镜效果
final class LutFilterProcessor {

    // MARK: - 属性

    /// Core Image 上下文，复用 GPU 资源
    private let context: CIContext
    /// 颜色空间
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!

    // MARK: - 初始化

    init() {
        // 使用 Metal 加速，回退到 CPU
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
    /// - Parameters:
    ///   - image: 输入图像
    ///   - preset: 滤镜预设
    ///   - intensity: 滤镜强度（0-1），默认 1.0
    /// - Returns: 处理后的 CIImage
    func applyFilter(to image: CIImage, preset: LutFilterPreset, intensity: Float = 1.0) -> CIImage {
        let clampedIntensity = max(0, min(1, intensity))

        // 如果强度为 0，直接返回原图
        guard clampedIntensity > 0.001 else { return image }

        // 构建滤镜链
        let filtered = applyFilterChain(to: image, parameters: preset.parameters)

        // 如果完全强度，直接返回滤镜结果
        guard clampedIntensity < 0.999 else { return filtered }

        // 强度混合：在原始图像和滤镜结果之间插值
        return blendImages(original: image, filtered: filtered, intensity: clampedIntensity)
    }

    /// 对 CIImage 应用滤镜链
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

        // 3. 颜色控制（亮度、对比度、饱和度）
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

        return output
    }

    // MARK: - 单个 CIFilter 应用

    /// 色温色调调整
    private func applyTemperatureAndTint(to image: CIImage, temperature: Float, tint: Float) -> CIImage {
        guard let filter = CIFilter(name: "CITemperatureAndTint") else { return image }
        // 中性色温 6500K，色调 0
        let neutralVector = CIVector(x: 6500, y: 0)
        let targetVector = CIVector(x: CGFloat(6500 + temperature), y: CGFloat(tint))

        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(neutralVector, forKey: "inputNeutral")
        filter.setValue(targetVector, forKey: "inputTargetNeutral")

        return filter.outputImage ?? image
    }

    /// 曝光调整
    private func applyExposureAdjust(to image: CIImage, ev: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIExposureAdjust") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(ev, forKey: kCIInputEVKey)
        return filter.outputImage ?? image
    }

    /// 颜色控制（亮度、对比度、饱和度）
    private func applyColorControls(to image: CIImage, brightness: Float, contrast: Float, saturation: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIColorControls") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(brightness, forKey: kCIInputBrightnessKey)
        filter.setValue(contrast, forKey: kCIInputContrastKey)
        filter.setValue(saturation, forKey: kCIInputSaturationKey)
        return filter.outputImage ?? image
    }

    /// 自然饱和度
    private func applyVibrance(to image: CIImage, amount: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIVibrance") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(amount, forKey: "inputAmount")
        return filter.outputImage ?? image
    }

    /// 高光阴影调整
    private func applyHighlightShadow(to image: CIImage, highlightAmount: Float, shadowAmount: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIHighlightShadowAdjust") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(highlightAmount, forKey: "inputHighlightAmount")
        filter.setValue(shadowAmount, forKey: "inputShadowAmount")
        return filter.outputImage ?? image
    }

    /// 黑白转换
    private func applyMonochrome(to image: CIImage, color: CIColor, intensity: Float) -> CIImage {
        guard let filter = CIFilter(name: "CIColorMonochrome") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(color, forKey: kCIInputColorKey)
        filter.setValue(intensity, forKey: kCIInputIntensityKey)
        return filter.outputImage ?? image
    }

    // MARK: - 强度混合

    /// 在原始图像和滤镜结果之间按强度混合
    /// - 使用 CIMix 不够精确，改用逐像素线性插值
    private func blendImages(original: CIImage, filtered: CIImage, intensity: Float) -> CIImage {
        // 方法：使用 CIBlendWithAlphaMask 进行混合
        // 创建强度遮罩（纯色图，alpha = intensity）
        let extent = filtered.extent
        guard let constantColor = CIFilter(name: "CIConstantColorGenerator") else {
            return filtered
        }
        let maskColor = CIColor(red: 1, green: 1, blue: 1, alpha: CGFloat(intensity))
        constantColor.setValue(maskColor, forKey: kCIInputColorKey)
        guard let maskImage = constantColor.outputImage else { return filtered }
        let mask = maskImage.cropped(to: extent)

        // 使用 CIBlendWithMask 混合：原图作为背景，滤镜图作为前景
        // 遮罩为白色(intensity)时显示滤镜图，黑色(1-intensity)时显示原图
        guard let blend = CIFilter(name: "CIBlendWithMask") else { return filtered }
        blend.setValue(filtered, forKey: kCIInputImageKey)
        blend.setValue(original, forKey: kCIInputBackgroundImageKey)
        blend.setValue(mask, forKey: kCIInputMaskImageKey)

        return blend.outputImage ?? filtered
    }

    // MARK: - CVPixelBuffer 处理

    /// 对 CVPixelBuffer 应用滤镜
    /// - Parameters:
    ///   - pixelBuffer: 输入像素缓冲
    ///   - preset: 滤镜预设
    ///   - intensity: 滤镜强度（0-1），默认 1.0
    /// - Returns: 处理后的 CVPixelBuffer，失败返回 nil
    func applyFilter(to pixelBuffer: CVPixelBuffer, preset: LutFilterPreset, intensity: Float = 1.0) -> CVPixelBuffer? {
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)

        let filtered = applyFilter(to: ciImage, preset: preset, intensity: intensity)

        // 创建输出像素缓冲
        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)

        var outputBuffer: CVPixelBuffer?
        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            CVPixelBufferGetPixelFormatType(pixelBuffer),
            nil,
            &outputBuffer
        )

        guard status == kCVReturnSuccess, let output = outputBuffer else {
            return nil
        }

        // 渲染到输出像素缓冲
        context.render(filtered, to: output)

        return output
    }

    // MARK: - 快速预览

    /// 快速预览：从 CVPixelBuffer 生成 UIImage（带滤镜效果）
    /// - Parameters:
    ///   - pixelBuffer: 输入像素缓冲
    ///   - preset: 滤镜预设
    /// - Returns: 应用滤镜后的 UIImage，失败返回 nil
    func quickPreview(pixelBuffer: CVPixelBuffer, preset: LutFilterPreset) -> UIImage? {
        guard let filtered = applyFilter(to: pixelBuffer, preset: preset, intensity: preset.defaultIntensity) else {
            return nil
        }

        let ciImage = CIImage(cvPixelBuffer: filtered)

        // 从 CIImage 创建 CGImage
        guard let cgImage = context.createCGImage(ciImage, from: ciImage.extent) else {
            return nil
        }

        return UIImage(cgImage: cgImage)
    }

    /// 快速预览：从 CIImage 生成 UIImage（带滤镜效果）
    func quickPreview(ciImage: CIImage, preset: LutFilterPreset) -> UIImage? {
        let filtered = applyFilter(to: ciImage, preset: preset, intensity: preset.defaultIntensity)

        guard let cgImage = context.createCGImage(filtered, from: filtered.extent) else {
            return nil
        }

        return UIImage(cgImage: cgImage)
    }

    // MARK: - 缩略图预览

    /// 生成缩略图预览（用于滤镜选择器中的小图）
    /// - Parameters:
    ///   - image: 输入图像
    ///   - preset: 滤镜预设
    ///   - targetSize: 目标缩略图尺寸
    /// - Returns: 缩略图 UIImage
    func thumbnailPreview(image: UIImage, preset: LutFilterPreset, targetSize: CGSize) -> UIImage? {
        guard let cgImage = image.cgImage else { return nil }
        let ciImage = CIImage(cgImage: cgImage)

        // 先缩放到目标尺寸以提高性能
        let scaleX = targetSize.width / ciImage.extent.width
        let scaleY = targetSize.height / ciImage.extent.height
        let scale = min(scaleX, scaleY)

        let scaled = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))

        let filtered = applyFilter(to: scaled, preset: preset, intensity: preset.defaultIntensity)

        guard let resultCG = context.createCGImage(filtered, from: filtered.extent) else {
            return nil
        }

        return UIImage(cgImage: resultCG)
    }
}

// MARK: - Metal 导入

import Metal

#endif