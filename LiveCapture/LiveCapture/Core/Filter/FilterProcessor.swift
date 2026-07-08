//
//  FilterProcessor.swift
//  LiveCapture
//
//  实时滤镜处理器 - 使用 CIFilter 链对 CIImage 应用滤镜效果
//
//  ## 文件作用
//  提供高性能的实时滤镜处理能力，支持：
//  - CIFilter 链处理（色温/曝光/颜色控制/饱和度/高光阴影/黑白）
//  - CIToneCurve 色调曲线调整
//  - CIColorMatrix 颜色矩阵变换
//  - 强度混合（intensity blending）在原始图像和滤镜效果之间平滑过渡
//  - 多滤镜链式组合
//  - 前后对比模式（before/after split view）
//  - CVPixelBuffer 实时预览和 UIImage 输出
//
//  ## 主要类
//  - FilterProcessor: 实时滤镜处理器（单例模式，共享 CIContext）
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
//  intensity = 1.0 时完全应用滤镜，intensity = 0.0 时返回原图
//
//  ## 性能优化
//  - 使用单个 CIContext 实例复用 GPU 资源（单例模式）
//  - colorSpace 使用 sRGB 确保颜色一致性
//  - 支持 Metal 加速（默认）
//  - 缩略图生成时先缩放再处理
//
//  ## 线程安全
//  - CIContext 是线程安全的
//  - 每次调用创建新的 CIImage 链，无共享状态
//

import Foundation
import CoreImage
import UIKit
import CoreVideo
import Metal

#if os(iOS)

/// 实时滤镜处理器
final class FilterProcessor {

    // MARK: - 单例

    /// 共享实例（复用 CIContext 和 GPU 资源）
    static let shared = FilterProcessor()

    // MARK: - 属性

    /// Core Image 上下文，复用 GPU 资源
    private let context: CIContext
    /// 颜色空间
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()

    /// 前后对比模式
    var comparisonMode: ComparisonMode = .disabled

    /// 前后对比模式枚举
    enum ComparisonMode {
        case disabled       // 正常模式
        case leftOriginal   // 左半原图，右半滤镜
        case topOriginal    // 上半原图，下半滤镜
    }

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "FilterProcessor"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "FilterProcessor"
            ])
        }
    }

    // MARK: - CIImage 处理

    /// 对 CIImage 应用单个滤镜
    /// - Parameters:
    ///   - image: 输入图像
    ///   - preset: 滤镜预设
    ///   - intensity: 滤镜强度（0-1），默认使用预设默认强度
    /// - Returns: 处理后的 CIImage
    func applyFilter(
        to image: CIImage,
        preset: LutFilterPreset,
        intensity: Float? = nil
    ) -> CIImage {
        let clampedIntensity = max(0, min(1, intensity ?? preset.defaultIntensity))

        // 如果强度为 0，直接返回原图
        guard clampedIntensity > 0.001 else { return image }

        // 构建滤镜链
        let filtered = applyFilterChain(to: image, parameters: preset.parameters)

        // 如果完全强度，直接返回滤镜结果
        guard clampedIntensity < 0.999 else { return filtered }

        // 强度混合：在原始图像和滤镜结果之间插值
        return blendImages(original: image, filtered: filtered, intensity: clampedIntensity)
    }

    /// 对 CIImage 应用多个滤镜链式组合
    /// - Parameters:
    ///   - image: 输入图像
    ///   - presets: 滤镜预设数组，按顺序应用
    ///   - intensities: 对应每个预设的强度，默认 nil 使用预设默认值
    /// - Returns: 处理后的 CIImage
    func applyFilterChain(
        to image: CIImage,
        presets: [LutFilterPreset],
        intensities: [Float]? = nil
    ) -> CIImage {
        var output = image
        for (index, preset) in presets.enumerated() {
            let intensity = intensities?[safe: index] ?? preset.defaultIntensity
            output = applyFilter(to: output, preset: preset, intensity: intensity)
        }
        return output
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

    /// 色温色调调整
    private func applyTemperatureAndTint(to image: CIImage, temperature: Float, tint: Float) -> CIImage {
        guard let filter = CIFilter(name: "CITemperatureAndTint") else { return image }
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

    /// 在原始图像和滤镜结果之间按强度混合
    private func blendImages(original: CIImage, filtered: CIImage, intensity: Float) -> CIImage {
        let extent = filtered.extent
        guard let constantColor = CIFilter(name: "CIConstantColorGenerator") else {
            return filtered
        }
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

    // MARK: - 前后对比模式

    /// 应用前后对比效果
    /// - Parameters:
    ///   - image: 原始输入图像
    ///   - preset: 滤镜预设
    ///   - intensity: 滤镜强度
    /// - Returns: 带对比效果的 CIImage
    func applyComparisonMode(
        to image: CIImage,
        preset: LutFilterPreset,
        intensity: Float? = nil
    ) -> CIImage {
        let filtered = applyFilter(to: image, preset: preset, intensity: intensity)

        switch comparisonMode {
        case .disabled:
            return filtered

        case .leftOriginal:
            return splitLeftRight(original: image, filtered: filtered)

        case .topOriginal:
            return splitTopBottom(original: image, filtered: filtered)
        }
    }

    /// 左右分割：左半原图，右半滤镜
    private func splitLeftRight(original: CIImage, filtered: CIImage) -> CIImage {
        let extent = filtered.extent
        let midX = extent.midX

        // 左半部分遮罩（白色=显示原始图）
        guard let leftMask = createGradientMask(
            extent: extent,
            startPoint: CGPoint(x: midX - 5, y: 0),
            endPoint: CGPoint(x: midX + 5, y: 0),
            startAlpha: 0,
            endAlpha: 1
        ) else { return filtered }

        guard let blend = CIFilter(name: "CIBlendWithMask") else { return filtered }
        blend.setValue(filtered, forKey: kCIInputImageKey)
        blend.setValue(original, forKey: kCIInputBackgroundImageKey)
        blend.setValue(leftMask, forKey: kCIInputMaskImageKey)

        return blend.outputImage ?? filtered
    }

    /// 上下分割：上半原图，下半滤镜
    private func splitTopBottom(original: CIImage, filtered: CIImage) -> CIImage {
        let extent = filtered.extent
        let midY = extent.midY

        guard let topMask = createGradientMask(
            extent: extent,
            startPoint: CGPoint(x: 0, y: midY - 5),
            endPoint: CGPoint(x: 0, y: midY + 5),
            startAlpha: 0,
            endAlpha: 1
        ) else { return filtered }

        guard let blend = CIFilter(name: "CIBlendWithMask") else { return filtered }
        blend.setValue(filtered, forKey: kCIInputImageKey)
        blend.setValue(original, forKey: kCIInputBackgroundImageKey)
        blend.setValue(topMask, forKey: kCIInputMaskImageKey)

        return blend.outputImage ?? filtered
    }

    /// 创建渐变遮罩
    private func createGradientMask(
        extent: CGRect,
        startPoint: CGPoint,
        endPoint: CGPoint,
        startAlpha: CGFloat,
        endAlpha: CGFloat
    ) -> CIImage? {
        guard let gradient = CIFilter(name: "CILinearGradient") else { return nil }
        gradient.setValue(CIVector(cgPoint: startPoint), forKey: "inputPoint0")
        gradient.setValue(CIVector(cgPoint: endPoint), forKey: "inputPoint1")
        gradient.setValue(CIColor(red: 1, green: 1, blue: 1, alpha: startAlpha), forKey: "inputColor0")
        gradient.setValue(CIColor(red: 1, green: 1, blue: 1, alpha: endAlpha), forKey: "inputColor1")
        return gradient.outputImage?.cropped(to: extent)
    }

    // MARK: - CVPixelBuffer 处理

    /// 对 CVPixelBuffer 应用滤镜
    /// - Parameters:
    ///   - pixelBuffer: 输入像素缓冲
    ///   - preset: 滤镜预设
    ///   - intensity: 滤镜强度（0-1），默认使用预设默认强度
    /// - Returns: 处理后的 CVPixelBuffer，失败返回 nil
    func applyFilter(
        to pixelBuffer: CVPixelBuffer,
        preset: LutFilterPreset,
        intensity: Float? = nil
    ) -> CVPixelBuffer? {
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        let filtered = applyFilter(to: ciImage, preset: preset, intensity: intensity)

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

        context.render(filtered, to: output)
        return output
    }

    // MARK: - 渲染输出

    /// 将 CIImage 渲染为 UIImage
    /// - Parameters:
    ///   - image: 输入 CIImage
    ///   - targetSize: 目标尺寸（可选，nil 表示使用原尺寸）
    /// - Returns: 渲染后的 UIImage
    func renderToUIImage(_ image: CIImage, targetSize: CGSize? = nil) -> UIImage? {
        let outputImage: CIImage
        if let size = targetSize {
            let scaleX = size.width / image.extent.width
            let scaleY = size.height / image.extent.height
            let scale = min(scaleX, scaleY)
            outputImage = image.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        } else {
            outputImage = image
        }

        guard let cgImage = context.createCGImage(outputImage, from: outputImage.extent) else {
            return nil
        }
        return UIImage(cgImage: cgImage)
    }

    /// 快速预览：从 CIImage 生成 UIImage
    func quickPreview(ciImage: CIImage, preset: LutFilterPreset) -> UIImage? {
        let filtered = applyFilter(to: ciImage, preset: preset)
        guard let cgImage = context.createCGImage(filtered, from: filtered.extent) else {
            return nil
        }
        return UIImage(cgImage: cgImage)
    }

    /// 快速预览：从 CVPixelBuffer 生成 UIImage
    func quickPreview(pixelBuffer: CVPixelBuffer, preset: LutFilterPreset) -> UIImage? {
        guard let filtered = applyFilter(to: pixelBuffer, preset: preset) else {
            return nil
        }
        let ciImage = CIImage(cvPixelBuffer: filtered)
        guard let cgImage = context.createCGImage(ciImage, from: ciImage.extent) else {
            return nil
        }
        return UIImage(cgImage: cgImage)
    }

    // MARK: - 缩略图预览

    /// 生成缩略图预览（用于滤镜选择器中的小图）
    /// - Parameters:
    ///   - image: 输入 UIImage
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

        let filtered = applyFilter(to: scaled, preset: preset)
        guard let resultCG = context.createCGImage(filtered, from: filtered.extent) else {
            return nil
        }
        return UIImage(cgImage: resultCG)
    }

    /// 批量生成缩略图预览（用于滤镜条）
    func thumbnailPreviews(
        image: UIImage,
        presets: [LutFilterPreset],
        targetSize: CGSize
    ) -> [UUID: UIImage] {
        guard let cgImage = image.cgImage else { return [:] }
        let ciImage = CIImage(cgImage: cgImage)
        let scaleX = targetSize.width / ciImage.extent.width
        let scaleY = targetSize.height / ciImage.extent.height
        let scale = min(scaleX, scaleY)
        let scaled = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))

        var results: [UUID: UIImage] = [:]
        for preset in presets {
            let filtered = applyFilter(to: scaled, preset: preset)
            if let cg = context.createCGImage(filtered, from: filtered.extent) {
                results[preset.id] = UIImage(cgImage: cg)
            }
        }
        return results
    }
}

// MARK: - Array 安全访问扩展

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

#endif