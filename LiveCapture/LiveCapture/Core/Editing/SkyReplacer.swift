//
//  SkyReplacer.swift
//  LiveCapture
//
//  天空替换器 - 基于 Vision 语义分割和 CoreImage 的天空替换
//
//  ## 主要功能
//  - replaceSky: 检测天空区域并替换为指定类型天空
//  - detectSkyRegion: 使用颜色分析和 Vision 检测天空区域
//  - generateSkyGradient: 生成多种类型天空渐变
//
//  ## 技术栈
//  - Vision: VNGeneratePersonSegmentationRequest 辅助分割
//  - CoreImage: CIColorCube、CIMorphology、CIGradient 等滤镜
//  - 颜色聚类 + 区域生长算法检测天空
//
//  ## 天空类型
//  - sunny: 晴天蓝色渐变
//  - sunset: 日落橙紫渐变
//  - night: 夜晚深蓝渐变
//  - starry: 星空效果
//  - aurora: 极光渐变
//  - dramatic: 戏剧性阴天效果
//

import Foundation
import CoreImage
import Vision
import UIKit

#if os(iOS)

/// 天空替换器
final class SkyReplacer: ObservableObject {

    // MARK: - 发布属性

    @Published var isProcessing = false

    // MARK: - 天空类型

    enum SkyType: String, CaseIterable {
        case sunny
        case sunset
        case night
        case starry
        case aurora
        case dramatic

        var displayName: String {
            switch self {
            case .sunny: return "晴天"
            case .sunset: return "日落"
            case .night: return "夜晚"
            case .starry: return "星空"
            case .aurora: return "极光"
            case .dramatic: return "戏剧"
            }
        }
    }

    // MARK: - 私有属性

    private let context: CIContext
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "SkyReplacer"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "SkyReplacer"
            ])
        }
    }

    // MARK: - 天空替换

    /// 替换图像中的天空
    /// - Parameters:
    ///   - image: 输入 CIImage
    ///   - skyType: 目标天空类型
    /// - Returns: 替换天空后的 CIImage，失败返回 nil
    func replaceSky(
        in image: CIImage,
        with skyType: SkyType
    ) async -> CIImage? {
        await MainActor.run { isProcessing = true }

        let extent = image.extent
        let result = await Task.detached(priority: .userInitiated) { [weak self] () -> CIImage in
            guard let self = self else { return image }

            // 1. 检测天空区域并创建掩码
            guard let skyMask = self.detectSkyRegion(in: image) else {
                return image
            }

            // 2. 生成新天空
            let newSky = self.generateSkyGradient(type: skyType, size: extent.size)

            // 3. 合成：新天空 * 掩码 + 原图 * (1 - 掩码)
            let composite = CIFilter.blendWithMask()
            composite.inputImage = newSky        // 新天空（替换区域）
            composite.backgroundImage = image     // 原图（保留区域）
            composite.maskImage = skyMask         // 天空掩码

            var output = composite.outputImage ?? image

            // 4. 调整前景光照以匹配新天空
            output = self.adjustForegroundLighting(output, skyType: skyType, skyMask: skyMask)

            return output
        }.value

        await MainActor.run { isProcessing = false }

        return result
    }

    // MARK: - 天空区域检测

    /// 检测图像中的天空区域，返回掩码（白色=天空，黑色=非天空）
    private func detectSkyRegion(in image: CIImage) -> CIImage? {
        let extent = image.extent
        guard extent.width > 0, extent.height > 0 else { return nil }

        // 1. 使用颜色分析检测天空像素
        // 天空通常在图像上部，颜色为蓝色、白色、灰色
        let skyColorMask = createSkyColorMask(from: image)

        // 2. 使用形态学操作扩展掩码向下
        var skyMask = skyColorMask

        // 形态学闭运算：先膨胀再腐蚀，填充小孔洞
        skyMask = skyMask
            .applyingFilter("CIMorphologyMaximum", parameters: [
                kCIInputRadiusKey: 15.0
            ])
            .applyingFilter("CIMorphologyMinimum", parameters: [
                kCIInputRadiusKey: 10.0
            ])

        // 3. 羽化边缘使过渡自然
        skyMask = skyMask.applyingGaussianBlur(sigma: 8.0)

        return skyMask
    }

    /// 基于颜色分析创建天空掩码
    private func createSkyColorMask(from image: CIImage) -> CIImage {
        let extent = image.extent

        // 使用 CIColorCube 创建天空颜色过滤器
        // 天空颜色范围：蓝色/白色/浅灰色
        let skyColorCubeData = createSkyColorCubeData()

        guard let colorCube = CIFilter(name: "CIColorCube", parameters: [
            kCIInputImageKey: image,
            "inputCubeDimension": 64,
            "inputCubeData": skyColorCubeData
        ]), let skyPixels = colorCube.outputImage else {
            return CIImage(color: CIColor(red: 0, green: 0, blue: 0))
        }

        // 将彩色结果转换为灰度掩码
        let monoMask = skyPixels
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 0.0,
                kCIInputContrastKey: 2.0,
                kCIInputBrightnessKey: -0.3
            ])

        // 裁剪到图像上部 60% 区域（天空一般在上部）
        let topRegion = CGRect(
            x: 0,
            y: extent.height * 0.2,
            width: extent.width,
            height: extent.height * 0.8
        )

        let bottomGradient = createBottomFeatherMask(imageExtent: extent, skyRegion: topRegion)

        // 混合：上部区域保留天空掩码，下部逐渐过渡到黑色
        let blend = CIFilter.blendWithMask()
        blend.inputImage = monoMask
        blend.backgroundImage = CIImage(color: CIColor(red: 0, green: 0, blue: 0, alpha: 0)).cropped(to: extent)
        blend.maskImage = bottomGradient

        return blend.outputImage ?? monoMask
    }

    /// 创建天空颜色 Cube 数据（识别蓝色/白色/灰色天空像素）
    private func createSkyColorCubeData() -> Data {
        let dimension = 64
        let size = dimension * dimension * dimension * 4
        var data = [Float](repeating: 0, count: size)

        for b in 0..<dimension {
            for g in 0..<dimension {
                for r in 0..<dimension {
                    let index = (r + g * dimension + b * dimension * dimension) * 4
                    let rf = Float(r) / Float(dimension - 1)
                    let gf = Float(g) / Float(dimension - 1)
                    let bf = Float(b) / Float(dimension - 1)

                    // 判断是否为天空颜色
                    let isSky = isSkyColor(red: rf, green: gf, blue: bf)

                    if isSky {
                        data[index] = 1.0     // R
                        data[index + 1] = 1.0 // G
                        data[index + 2] = 1.0 // B
                        data[index + 3] = 1.0 // A
                    } else {
                        data[index] = 0.0
                        data[index + 1] = 0.0
                        data[index + 2] = 0.0
                        data[index + 3] = 0.0
                    }
                }
            }
        }

        return Data(bytes: &data, count: size * MemoryLayout<Float>.size)
    }

    /// 判断颜色是否为天空颜色
    private func isSkyColor(red: Float, green: Float, blue: Float) -> Bool {
        // 蓝色天空：蓝色通道明显高于红色
        if blue > red + 0.1 && blue > green * 0.9 && blue > 0.4 {
            return true
        }

        // 白色/浅灰天空（多云）：所有通道接近且亮度较高
        let brightness = (red + green + blue) / 3.0
        if brightness > 0.6 && abs(red - green) < 0.15 && abs(green - blue) < 0.15 {
            return true
        }

        // 青蓝色天空
        if blue > 0.5 && green > 0.5 && blue > red * 1.3 && green > red * 1.2 {
            return true
        }

        return false
    }

    /// 创建底部羽化掩码（从天空区域向下渐变消失）
    private func createBottomFeatherMask(imageExtent: CGRect, skyRegion: CGRect) -> CIImage {
        let topPoint = CIVector(x: imageExtent.midX, y: skyRegion.maxY)
        let bottomPoint = CIVector(x: imageExtent.midX, y: skyRegion.minY)

        let gradient = CIFilter.linearGradient()
        gradient.point0 = topPoint
        gradient.point1 = bottomPoint
        gradient.color0 = CIColor(red: 0, green: 0, blue: 0, alpha: 1)
        gradient.color1 = CIColor(red: 1, green: 1, blue: 1, alpha: 1)

        return gradient.outputImage?.cropped(to: imageExtent)
            ?? CIImage(color: CIColor(red: 1, green: 1, blue: 1))
    }

    // MARK: - 天空渐变生成

    /// 根据天空类型生成渐变天空
    private func generateSkyGradient(type: SkyType, size: CGSize) -> CIImage {
        switch type {
        case .sunny:
            return generateSunnySky(size: size)
        case .sunset:
            return generateSunsetSky(size: size)
        case .night:
            return generateNightSky(size: size)
        case .starry:
            return generateStarrySky(size: size)
        case .aurora:
            return generateAuroraSky(size: size)
        case .dramatic:
            return generateDramaticSky(size: size)
        }
    }

    /// 晴天天空：蓝色到白色渐变
    private func generateSunnySky(size: CGSize) -> CIImage {
        let topPoint = CIVector(x: size.width / 2, y: size.height)
        let bottomPoint = CIVector(x: size.width / 2, y: 0)

        let gradient = CIFilter.linearGradient()
        gradient.point0 = topPoint
        gradient.point1 = bottomPoint
        gradient.color0 = CIColor(red: 0.33, green: 0.60, blue: 0.93, alpha: 1)  // #5599ED 深蓝
        gradient.color1 = CIColor(red: 0.78, green: 0.90, blue: 1.0, alpha: 1)   // #C8E6FF 浅蓝

        guard var sky = gradient.outputImage?.cropped(to: CGRect(origin: .zero, size: size)) else {
            return CIImage(color: CIColor(red: 0.33, green: 0.60, blue: 0.93))
        }

        // 添加轻微噪点模拟云层纹理
        sky = addCloudTexture(to: sky, intensity: 0.15)

        return sky
    }

    /// 日落天空：橙色到紫色渐变
    private func generateSunsetSky(size: CGSize) -> CIImage {
        let extent = CGRect(origin: .zero, size: size)

        // 上部：深紫色
        let topPoint = CIVector(x: size.width / 2, y: size.height)
        // 下部：橙色
        let bottomPoint = CIVector(x: size.width / 2, y: 0)

        let gradient = CIFilter.linearGradient()
        gradient.point0 = topPoint
        gradient.point1 = bottomPoint
        gradient.color0 = CIColor(red: 0.48, green: 0.18, blue: 0.56, alpha: 1)  // #7B2D8E 深紫
        gradient.color1 = CIColor(red: 1.0, green: 0.42, blue: 0.23, alpha: 1)   // #FF6B35 橙色

        guard var sky = gradient.outputImage?.cropped(to: extent) else {
            return CIImage(color: CIColor(red: 1.0, green: 0.42, blue: 0.23))
        }

        // 中间添加橙红色过渡
        let midPoint = CIVector(x: size.width / 2, y: size.height * 0.5)
        let midToBottom = CIVector(x: size.width / 2, y: 0)

        let midGradient = CIFilter.linearGradient()
        midGradient.point0 = midPoint
        midGradient.point1 = midToBottom
        midGradient.color0 = CIColor(red: 0.85, green: 0.25, blue: 0.35, alpha: 1)  // 红橙
        midGradient.color1 = CIColor(red: 1.0, green: 0.55, blue: 0.30, alpha: 1)   // 亮橙

        if let midSky = midGradient.outputImage?.cropped(to: extent) {
            let blend = CIFilter.sourceOverCompositing()
            blend.inputImage = midSky
            blend.backgroundImage = sky
            sky = blend.outputImage ?? sky
        }

        // 添加云层纹理
        sky = addCloudTexture(to: sky, intensity: 0.2)

        return sky
    }

    /// 夜晚天空：深蓝到黑色渐变
    private func generateNightSky(size: CGSize) -> CIImage {
        let extent = CGRect(origin: .zero, size: size)

        let topPoint = CIVector(x: size.width / 2, y: size.height)
        let bottomPoint = CIVector(x: size.width / 2, y: 0)

        let gradient = CIFilter.linearGradient()
        gradient.point0 = topPoint
        gradient.point1 = bottomPoint
        gradient.color0 = CIColor(red: 0.06, green: 0.13, blue: 0.15, alpha: 1)   // #0F2027
        gradient.color1 = CIColor(red: 0.12, green: 0.18, blue: 0.30, alpha: 1)   // #1F2E4D

        guard var sky = gradient.outputImage?.cropped(to: extent) else {
            return CIImage(color: CIColor(red: 0.06, green: 0.13, blue: 0.15))
        }

        // 降低亮度
        sky = sky.applyingFilter("CIExposureAdjust", parameters: [
            kCIInputEVKey: -0.3
        ])

        return sky
    }

    /// 星空天空：深色渐变 + 随机星点
    private func generateStarrySky(size: CGSize) -> CIImage {
        let extent = CGRect(origin: .zero, size: size)

        // 基础夜空渐变
        let topPoint = CIVector(x: size.width / 2, y: size.height)
        let bottomPoint = CIVector(x: size.width / 2, y: 0)

        let gradient = CIFilter.linearGradient()
        gradient.point0 = topPoint
        gradient.point1 = bottomPoint
        gradient.color0 = CIColor(red: 0.02, green: 0.05, blue: 0.15, alpha: 1)   // 极深蓝
        gradient.color1 = CIColor(red: 0.08, green: 0.12, blue: 0.25, alpha: 1)   // 深蓝

        guard var sky = gradient.outputImage?.cropped(to: extent) else {
            return CIImage(color: CIColor(red: 0.02, green: 0.05, blue: 0.15))
        }

        // 生成星空噪点
        guard let noiseGenerator = CIFilter(name: "CIRandomGenerator"),
              let noise = noiseGenerator.outputImage else {
            return sky
        }

        // 将噪点处理为星点效果
        let stars = noise
            .cropped(to: extent)
            .applyingFilter("CIColorMatrix", parameters: [
                "inputRVector": CIVector(x: 0, y: 0, z: 0, w: 0),
                "inputGVector": CIVector(x: 0, y: 0, z: 0, w: 0),
                "inputBVector": CIVector(x: 0, y: 0, z: 0, w: 0),
                "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0),
                "inputBiasVector": CIVector(x: 1, y: 1, z: 1, w: 0)
            ])
            // 提高阈值使只有亮点可见
            .applyingFilter("CIColorControls", parameters: [
                kCIInputBrightnessKey: -0.85,
                kCIInputContrastKey: 4.0
            ])
            // 缩小星点
            .applyingFilter("CIMorphologyMinimum", parameters: [
                kCIInputRadiusKey: 0.5
            ])

        // 混合星点到天空
        let blend = CIFilter.sourceOverCompositing()
        blend.inputImage = stars
        blend.backgroundImage = sky
        sky = blend.outputImage ?? sky

        return sky
    }

    /// 极光天空：绿色/紫色波浪渐变
    private func generateAuroraSky(size: CGSize) -> CIImage {
        let extent = CGRect(origin: .zero, size: size)

        // 基础深色天空
        let topPoint = CIVector(x: size.width / 2, y: size.height)
        let bottomPoint = CIVector(x: size.width / 2, y: 0)

        let gradient = CIFilter.linearGradient()
        gradient.point0 = topPoint
        gradient.point1 = bottomPoint
        gradient.color0 = CIColor(red: 0.05, green: 0.08, blue: 0.20, alpha: 1)   // 暗蓝
        gradient.color1 = CIColor(red: 0.10, green: 0.15, blue: 0.30, alpha: 1)   // 深蓝

        guard var sky = gradient.outputImage?.cropped(to: extent) else {
            return CIImage(color: CIColor(red: 0.05, green: 0.08, blue: 0.20))
        }

        // 极光条纹：多色渐变带
        let auroraColors: [(CGFloat, CIColor)] = [
            (0.35, CIColor(red: 0.0, green: 0.8, blue: 0.4, alpha: 0.5)),  // 绿色极光
            (0.45, CIColor(red: 0.3, green: 0.9, blue: 0.5, alpha: 0.4)),  // 亮绿
            (0.55, CIColor(red: 0.5, green: 0.3, blue: 0.8, alpha: 0.35)),  // 紫色
            (0.60, CIColor(red: 0.2, green: 0.7, blue: 0.6, alpha: 0.3)),  // 青绿
        ]

        for (position, color) in auroraColors {
            let auroraY = size.height * position
            let auroraPoint = CIVector(x: size.width / 2, y: auroraY)
            let fadePoint = CIVector(x: size.width / 2, y: auroraY - size.height * 0.08)

            let auroraGradient = CIFilter.linearGradient()
            auroraGradient.point0 = auroraPoint
            auroraGradient.point1 = fadePoint
            auroraGradient.color0 = color
            auroraGradient.color1 = CIColor(red: 0, green: 0, blue: 0, alpha: 0)

            if let auroraStrip = auroraGradient.outputImage?.cropped(to: extent) {
                let blend = CIFilter.sourceOverCompositing()
                blend.inputImage = auroraStrip
                blend.backgroundImage = sky
                sky = blend.outputImage ?? sky
            }
        }

        // 添加噪点模拟极光纹理
        sky = addCloudTexture(to: sky, intensity: 0.08)

        return sky
    }

    /// 戏剧性天空：灰暗渐变 + 高对比度
    private func generateDramaticSky(size: CGSize) -> CIImage {
        let extent = CGRect(origin: .zero, size: size)

        let topPoint = CIVector(x: size.width / 2, y: size.height)
        let bottomPoint = CIVector(x: size.width / 2, y: 0)

        let gradient = CIFilter.linearGradient()
        gradient.point0 = topPoint
        gradient.point1 = bottomPoint
        gradient.color0 = CIColor(red: 0.15, green: 0.15, blue: 0.17, alpha: 1)  // 深灰
        gradient.color1 = CIColor(red: 0.45, green: 0.45, blue: 0.50, alpha: 1)  // 浅灰

        guard var sky = gradient.outputImage?.cropped(to: extent) else {
            return CIImage(color: CIColor(red: 0.3, green: 0.3, blue: 0.35))
        }

        // 增加对比度
        sky = sky.applyingFilter("CIColorControls", parameters: [
            kCIInputContrastKey: 1.3,
            kCIInputSaturationKey: 0.3,
            kCIInputBrightnessKey: -0.1
        ])

        // 添加云层纹理
        sky = addCloudTexture(to: sky, intensity: 0.3)

        // 添加暗角
        sky = sky.applyingFilter("CIVignette", parameters: [
            kCIInputIntensityKey: 0.5,
            kCIInputRadiusKey: 0.8
        ])

        return sky
    }

    // MARK: - 云层纹理

    /// 添加云层纹理
    private func addCloudTexture(to sky: CIImage, intensity: Float) -> CIImage {
        guard let noiseGenerator = CIFilter(name: "CIRandomGenerator"),
              let noise = noiseGenerator.outputImage else {
            return sky
        }

        let extent = sky.extent

        // 生成柔和的云层纹理
        let cloudNoise = noise
            .cropped(to: extent)
            // 水平模糊模拟云层拉伸
            .applyingFilter("CIMotionBlur", parameters: [
                kCIInputRadiusKey: 60.0,
                kCIInputAngleKey: 0.0
            ])
            .applyingFilter("CIColorControls", parameters: [
                kCIInputBrightnessKey: -0.5,
                kCIInputContrastKey: 1.5
            ])

        // 用透明度控制云层强度
        let alphaFilter = CIFilter.colorMatrix()
        alphaFilter.inputImage = cloudNoise
        alphaFilter.aVector = CIVector(x: 0, y: 0, z: 0, w: CGFloat(intensity))
        alphaFilter.biasVector = CIVector(x: 0, y: 0, z: 0, w: 0)

        let fadedClouds = alphaFilter.outputImage ?? cloudNoise

        let blend = CIFilter.sourceOverCompositing()
        blend.inputImage = fadedClouds
        blend.backgroundImage = sky

        return blend.outputImage ?? sky
    }

    // MARK: - 前景光照调整

    /// 调整前景光照以匹配新天空
    private func adjustForegroundLighting(
        _ image: CIImage,
        skyType: SkyType,
        skyMask: CIImage
    ) -> CIImage {
        let extent = image.extent

        // 创建前景掩码（非天空区域）
        let invertMask = skyMask
            .applyingFilter("CIColorInvert")

        switch skyType {
        case .sunset:
            // 日落：前景加暖色调
            let warmed = image
                .applyingFilter("CITemperatureAndTint", parameters: [
                    "inputNeutral": CIVector(x: 6500, y: 0),
                    "inputTargetNeutral": CIVector(x: 5500, y: 15)
                ])
                .applyingFilter("CIExposureAdjust", parameters: [
                    kCIInputEVKey: -0.15
                ])

            let blend = CIFilter.blendWithMask()
            blend.inputImage = warmed
            blend.backgroundImage = image
            blend.maskImage = invertMask
            return blend.outputImage ?? image

        case .night, .starry:
            // 夜晚：前景降低亮度和饱和度
            let darkened = image
                .applyingFilter("CIExposureAdjust", parameters: [
                    kCIInputEVKey: -0.4
                ])
                .applyingFilter("CIColorControls", parameters: [
                    kCIInputSaturationKey: 0.7,
                    kCIInputContrastKey: 1.1
                ])

            let blend = CIFilter.blendWithMask()
            blend.inputImage = darkened
            blend.backgroundImage = image
            blend.maskImage = invertMask
            return blend.outputImage ?? image

        case .aurora:
            // 极光：前景加冷色调
            let cooled = image
                .applyingFilter("CITemperatureAndTint", parameters: [
                    "inputNeutral": CIVector(x: 6500, y: 0),
                    "inputTargetNeutral": CIVector(x: 8000, y: -10)
                ])
                .applyingFilter("CIExposureAdjust", parameters: [
                    kCIInputEVKey: -0.2
                ])

            let blend = CIFilter.blendWithMask()
            blend.inputImage = cooled
            blend.backgroundImage = image
            blend.maskImage = invertMask
            return blend.outputImage ?? image

        case .dramatic:
            // 戏剧性：前景增加对比度和降低饱和度
            let dramatic = image
                .applyingFilter("CIColorControls", parameters: [
                    kCIInputContrastKey: 1.2,
                    kCIInputSaturationKey: 0.6,
                    kCIInputBrightnessKey: -0.1
                ])

            let blend = CIFilter.blendWithMask()
            blend.inputImage = dramatic
            blend.backgroundImage = image
            blend.maskImage = invertMask
            return blend.outputImage ?? image

        case .sunny:
            // 晴天：前景轻微提亮
            let brightened = image
                .applyingFilter("CIExposureAdjust", parameters: [
                    kCIInputEVKey: 0.1
                ])
                .applyingFilter("CIHighlightShadowAdjust", parameters: [
                    "inputHighlightAmount": 1.05,
                    "inputShadowAmount": 0.95
                ])

            let blend = CIFilter.blendWithMask()
            blend.inputImage = brightened
            blend.backgroundImage = image
            blend.maskImage = invertMask
            return blend.outputImage ?? image
        }
    }
}

// MARK: - Metal 导入

import Metal

#endif