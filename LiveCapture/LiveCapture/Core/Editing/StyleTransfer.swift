//
//  StyleTransfer.swift
//  LiveCapture
//
//  风格迁移器 - 基于 CoreImage 滤镜链的艺术风格转换
//
//  ## 主要功能
//  - applyStyle: 应用指定艺术风格
//  - 每种风格包含具体的 CIFilter 链和参数
//  - intensity 控制风格强度（原始图像与风格化图像的混合比例）
//
//  ## 技术栈
//  - CoreImage: CIFilter 链式组合
//  - 每种风格是多个 CIFilter 的特定组合
//
//  ## 支持的风格
//  - watercolor: 水彩画效果
//  - oilPainting: 油画效果
//  - sketch: 素描效果
//  - comicBook: 漫画书效果
//  - pixelArt: 像素艺术效果
//  - vintage: 复古胶片效果
//  - neon: 霓虹灯效果
//  - pencil: 铅笔素描效果
//

import Foundation
import CoreImage
import Accelerate

#if os(iOS)

/// 风格迁移器
final class StyleTransfer: ObservableObject {

    // MARK: - 发布属性

    @Published var isProcessing = false

    // MARK: - 艺术风格

    enum ArtStyle: String, CaseIterable {
        case watercolor
        case oilPainting
        case sketch
        case comicBook
        case pixelArt
        case vintage
        case neon
        case pencil

        var displayName: String {
            switch self {
            case .watercolor: return "水彩画"
            case .oilPainting: return "油画"
            case .sketch: return "素描"
            case .comicBook: return "漫画"
            case .pixelArt: return "像素艺术"
            case .vintage: return "复古"
            case .neon: return "霓虹灯"
            case .pencil: return "铅笔"
            }
        }
    }

    // MARK: - 私有属性

    private let context: CIContext
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "StyleTransfer"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "StyleTransfer"
            ])
        }
    }

    // MARK: - 风格应用

    /// 应用艺术风格转换
    /// - Parameters:
    ///   - image: 输入 CIImage
    ///   - style: 目标艺术风格
    ///   - intensity: 风格强度（0.0 = 原图，1.0 = 完全风格化）
    /// - Returns: 风格化后的 CIImage
    func applyStyle(
        to image: CIImage,
        style: ArtStyle,
        intensity: Float = 0.7
    ) -> CIImage {
        let extent = image.extent

        // 应用对应风格
        let styled: CIImage
        switch style {
        case .watercolor:
            styled = applyWatercolor(image)
        case .oilPainting:
            styled = applyOilPainting(image)
        case .sketch:
            styled = applySketch(image)
        case .comicBook:
            styled = applyComicBook(image)
        case .pixelArt:
            styled = applyPixelArt(image)
        case .vintage:
            styled = applyVintage(image)
        case .neon:
            styled = applyNeon(image)
        case .pencil:
            styled = applyPencil(image)
        }

        // 根据强度混合原图和风格化图像
        let clampedIntensity = max(0.0, min(1.0, intensity))
        if clampedIntensity >= 1.0 {
            return styled.cropped(to: extent)
        }

        return blendImages(original: image, styled: styled, intensity: CGFloat(clampedIntensity))
            .cropped(to: extent)
    }

    // MARK: - 水彩画效果

    /// 水彩画：模糊 + 边缘强化 + 颜色柔化
    private func applyWatercolor(_ image: CIImage) -> CIImage {
        let extent = image.extent

        // 1. 轻微高斯模糊模拟水彩渲染
        let blurred = image
            .clampedToExtent()
            .applyingGaussianBlur(sigma: 3.0)
            .cropped(to: extent)

        // 2. 提取边缘（水彩画边缘特征）
        let edges = image
            .applyingFilter("CIEdgeWork", parameters: [
                kCIInputRadiusKey: 3.0
            ])

        // 3. 将边缘叠加到模糊图像上
        let edgeBlend = CIFilter.multiplyCompositing()
        edgeBlend.inputImage = edges
        edgeBlend.backgroundImage = blurred

        var result = edgeBlend.outputImage ?? blurred

        // 4. 调整颜色使水彩感更强
        result = result
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 1.2,
                kCIInputContrastKey: 0.95,
                kCIInputBrightnessKey: 0.05
            ])

        // 5. 轻微噪点模拟纸纹
        guard let noiseGenerator = CIFilter(name: "CIRandomGenerator"),
              let noise = noiseGenerator.outputImage else {
            return result
        }

        let paperTexture = noise
            .cropped(to: extent)
            .applyingFilter("CIColorMatrix", parameters: [
                "inputRVector": CIVector(x: 0.1, y: 0, z: 0, w: 0),
                "inputGVector": CIVector(x: 0.1, y: 0, z: 0, w: 0),
                "inputBVector": CIVector(x: 0.1, y: 0, z: 0, w: 0),
                "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0.05),
                "inputBiasVector": CIVector(x: 0.95, y: 0.95, z: 0.95, w: 0)
            ])

        let paperBlend = CIFilter.sourceOverCompositing()
        paperBlend.inputImage = paperTexture
        paperBlend.backgroundImage = result
        result = paperBlend.outputImage ?? result

        return result
    }

    // MARK: - 油画效果

    /// 油画：像素化 + 中值滤波 + 发光效果
    private func applyOilPainting(_ image: CIImage) -> CIImage {
        let extent = image.extent

        // 1. 轻微像素化模拟油画笔触
        let pixellated = image
            .applyingFilter("CIPixellate", parameters: [
                kCIInputScaleKey: 4.0
            ])

        // 2. 中值滤波平滑色块（模拟油画颜料混合）
        let smoothed = pixellated
            .applyingFilter("CIMedianFilter")

        // 3. 发光效果增加油画光泽
        var result = smoothed
            .applyingFilter("CIGloom", parameters: [
                kCIInputRadiusKey: 10.0,
                kCIInputIntensityKey: 0.5
            ])

        // 4. 增强对比度和饱和度
        result = result
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 1.3,
                kCIInputContrastKey: 1.15,
                kCIInputBrightnessKey: 0.0
            ])

        // 5. 增加锐度突出笔触
        result = result
            .applyingFilter("CISharpenLuminance", parameters: [
                kCIInputSharpnessKey: 0.3
            ])

        return result
    }

    // MARK: - 素描效果

    /// 素描：单色 + 边缘强化 + 反相
    private func applySketch(_ image: CIImage) -> CIImage {
        let extent = image.extent

        // 1. 转换为灰度
        let grayscale = image
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 0.0,
                kCIInputContrastKey: 1.1
            ])

        // 2. 提取边缘（素描线条）
        let edges = grayscale
            .applyingFilter("CIEdgeWork", parameters: [
                kCIInputRadiusKey: 2.5
            ])

        // 3. 反相颜色（白底黑线）
        let inverted = edges
            .applyingFilter("CIColorInvert")

        // 4. 调整亮度和对比度使线条更清晰
        var result = inverted
            .applyingFilter("CIColorControls", parameters: [
                kCIInputBrightnessKey: 0.1,
                kCIInputContrastKey: 1.3,
                kCIInputSaturationKey: 0.0
            ])

        // 5. 轻微模糊使线条柔和
        result = result
            .applyingGaussianBlur(sigma: 0.5)

        return result
    }

    // MARK: - 漫画书效果

    /// 漫画书：色调分离 + 边缘线条 + 色彩增强
    private func applyComicBook(_ image: CIImage) -> CIImage {
        let extent = image.extent

        // 1. 色调分离减少颜色数量（漫画特征）
        let posterized = image
            .applyingFilter("CIColorPosterize", parameters: [
                "inputLevels": 6.0
            ])

        // 2. 提取边缘作为漫画线条
        let edges = image
            .applyingFilter("CIEdgeWork", parameters: [
                kCIInputRadiusKey: 2.0
            ])

        // 3. 将黑色线条叠加到色调分离图像上
        let edgeBlend = CIFilter.multiplyCompositing()
        edgeBlend.inputImage = edges
        edgeBlend.backgroundImage = posterized

        var result = edgeBlend.outputImage ?? posterized

        // 4. 增强色彩饱和度（漫画鲜艳色彩）
        result = result
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 1.5,
                kCIInputContrastKey: 1.2,
                kCIInputBrightnessKey: 0.05
            ])

        // 5. 半色调网点效果（漫画印刷特征）
        result = result
            .applyingFilter("CIDotScreen", parameters: [
                kCIInputWidthKey: 4.0,
                kCIInputSharpnessKey: 0.7,
                kCIInputAngleKey: 0.0
            ])

        return result
    }

    // MARK: - 像素艺术效果

    /// 像素艺术：像素化 + 色调分离 + 锐化
    private func applyPixelArt(_ image: CIImage) -> CIImage {
        let extent = image.extent

        // 1. 强像素化
        let pixellated = image
            .applyingFilter("CIPixellate", parameters: [
                kCIInputScaleKey: 8.0
            ])

        // 2. 色调分离减少颜色（像素艺术特征）
        var result = pixellated
            .applyingFilter("CIColorPosterize", parameters: [
                "inputLevels": 5.0
            ])

        // 3. 增强对比度使像素块更明显
        result = result
            .applyingFilter("CIColorControls", parameters: [
                kCIInputContrastKey: 1.3,
                kCIInputSaturationKey: 1.2,
                kCIInputBrightnessKey: 0.0
            ])

        // 4. 锐化像素边缘
        result = result
            .applyingFilter("CISharpenLuminance", parameters: [
                kCIInputSharpnessKey: 0.5
            ])

        return result
    }

    // MARK: - 复古胶片效果

    /// 复古胶片：棕褐色 + 暗角 + 噪点 + 褪色
    private func applyVintage(_ image: CIImage) -> CIImage {
        let extent = image.extent

        // 1. 棕褐色调
        var result = image
            .applyingFilter("CISepiaTone", parameters: [
                kCIInputIntensityKey: 0.7
            ])

        // 2. 褪色效果（降低对比度，提升亮度）
        result = result
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 0.6,
                kCIInputContrastKey: 0.85,
                kCIInputBrightnessKey: 0.05
            ])

        // 3. 暖色调偏移
        result = result
            .applyingFilter("CITemperatureAndTint", parameters: [
                "inputNeutral": CIVector(x: 6500, y: 0),
                "inputTargetNeutral": CIVector(x: 5000, y: 20)
            ])

        // 4. 暗角效果
        result = result
            .applyingFilter("CIVignette", parameters: [
                kCIInputIntensityKey: 0.8,
                kCIInputRadiusKey: 0.7
            ])

        // 5. 胶片噪点
        guard let noiseGenerator = CIFilter(name: "CIRandomGenerator"),
              let noise = noiseGenerator.outputImage else {
            return result
        }

        let filmGrain = noise
            .cropped(to: extent)
            .applyingFilter("CIColorMatrix", parameters: [
                "inputRVector": CIVector(x: 0.15, y: 0, z: 0, w: 0),
                "inputGVector": CIVector(x: 0.15, y: 0, z: 0, w: 0),
                "inputBVector": CIVector(x: 0.15, y: 0, z: 0, w: 0),
                "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0.08),
                "inputBiasVector": CIVector(x: 0.92, y: 0.92, z: 0.92, w: 0)
            ])

        let grainBlend = CIFilter.sourceOverCompositing()
        grainBlend.inputImage = filmGrain
        grainBlend.backgroundImage = result
        result = grainBlend.outputImage ?? result

        // 6. 轻微模糊模拟老镜头
        result = result
            .applyingGaussianBlur(sigma: 0.8)

        return result
    }

    // MARK: - 霓虹灯效果

    /// 霓虹灯：边缘提取 + 反相 + 色彩矩阵变换
    private func applyNeon(_ image: CIImage) -> CIImage {
        let extent = image.extent

        // 1. 提取边缘
        let edges = image
            .applyingFilter("CIEdgeWork", parameters: [
                kCIInputRadiusKey: 1.5
            ])

        // 2. 反相（黑底亮线）
        let inverted = edges
            .applyingFilter("CIColorInvert")

        // 3. 色彩矩阵：将线条转换为霓虹色
        // 将白色线条映射到霓虹青/紫色
        let neonized = inverted
            .applyingFilter("CIColorMatrix", parameters: [
                "inputRVector": CIVector(x: 0.2, y: 0, z: 0, w: 0),
                "inputGVector": CIVector(x: 0, y: 0.8, z: 0, w: 0),
                "inputBVector": CIVector(x: 0, y: 0, z: 1.0, w: 0),
                "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 1.0),
                "inputBiasVector": CIVector(x: 0.1, y: 0, z: 0.3, w: 0)
            ])

        // 4. 发光效果（霓虹灯管发光）
        var result = neonized
            .applyingFilter("CIBloom", parameters: [
                kCIInputRadiusKey: 5.0,
                kCIInputIntensityKey: 1.5
            ])

        // 5. 将霓虹效果叠加到原图上（暗化原图作为背景）
        let darkened = image
            .applyingFilter("CIExposureAdjust", parameters: [
                kCIInputEVKey: -1.5
            ])
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 0.3,
                kCIInputContrastKey: 0.8
            ])

        let blend = CIFilter.sourceOverCompositing()
        blend.inputImage = result
        blend.backgroundImage = darkened
        result = blend.outputImage ?? result

        // 6. 增加对比度
        result = result
            .applyingFilter("CIColorControls", parameters: [
                kCIInputContrastKey: 1.2,
                kCIInputSaturationKey: 1.3
            ])

        return result
    }

    // MARK: - 铅笔素描效果

    /// 铅笔素描：单色 + 交叉阴影 + 纹理
    private func applyPencil(_ image: CIImage) -> CIImage {
        let extent = image.extent

        // 1. 转换为灰度
        let grayscale = image
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 0.0,
                kCIInputContrastKey: 1.1
            ])

        // 2. 使用交叉阴影滤镜模拟铅笔线条
        let hatched = grayscale
            .applyingFilter("CIHatchedScreen", parameters: [
                kCIInputWidthKey: 3.0,
                kCIInputSharpnessKey: 0.8,
                kCIInputAngleKey: 0.0
            ])

        // 3. 反相使白底黑线
        let inverted = hatched
            .applyingFilter("CIColorInvert")

        // 4. 调整对比度使铅笔线条更清晰
        var result = inverted
            .applyingFilter("CIColorControls", parameters: [
                kCIInputBrightnessKey: 0.15,
                kCIInputContrastKey: 1.4,
                kCIInputSaturationKey: 0.0
            ])

        // 5. 添加纸张纹理
        guard let noiseGenerator = CIFilter(name: "CIRandomGenerator"),
              let noise = noiseGenerator.outputImage else {
            return result
        }

        let paperTexture = noise
            .cropped(to: extent)
            .applyingFilter("CIColorMatrix", parameters: [
                "inputRVector": CIVector(x: 0.05, y: 0, z: 0, w: 0),
                "inputGVector": CIVector(x: 0.05, y: 0, z: 0, w: 0),
                "inputBVector": CIVector(x: 0.05, y: 0, z: 0, w: 0),
                "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0.03),
                "inputBiasVector": CIVector(x: 0.97, y: 0.97, z: 0.97, w: 0)
            ])

        let paperBlend = CIFilter.sourceOverCompositing()
        paperBlend.inputImage = paperTexture
        paperBlend.backgroundImage = result
        result = paperBlend.outputImage ?? result

        return result
    }

    // MARK: - 图像混合

    /// 根据强度混合原始图像和风格化图像
    private func blendImages(
        original: CIImage,
        styled: CIImage,
        intensity: CGFloat
    ) -> CIImage {
        // 创建均匀灰色掩码（强度控制透明度）
        let maskColor = CIColor(red: intensity, green: intensity, blue: intensity, alpha: 1.0)
        let maskImage = CIImage(color: maskColor)
            .cropped(to: styled.extent)

        let blend = CIFilter.blendWithMask()
        blend.inputImage = styled       // 风格化图像
        blend.backgroundImage = original // 原始图像
        blend.maskImage = maskImage      // 强度掩码

        return blend.outputImage ?? styled
    }

    // MARK: - 渲染方法

    /// 渲染风格化图像为 UIImage
    func renderStyledImage(
        from image: UIImage,
        style: ArtStyle,
        intensity: Float = 0.7
    ) -> UIImage? {
        guard let ciImage = image.ciImage ?? CIImage(image: image) else {
            return nil
        }

        let styled = applyStyle(to: ciImage, style: style, intensity: intensity)
        let extent = styled.extent

        guard extent.width > 0 && extent.height > 0,
              extent.width < 50000 && extent.height < 50000,
              let cgImage = context.createCGImage(styled, from: extent) else {
            return nil
        }

        return UIImage(cgImage: cgImage)
    }
}

// MARK: - Metal 导入

import Metal

#endif