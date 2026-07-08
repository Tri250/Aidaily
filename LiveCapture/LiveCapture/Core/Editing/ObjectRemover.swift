//
//  ObjectRemover.swift
//  LiveCapture
//
//  物体移除器 - 基于 CoreImage 的内容感知填充，模拟 inpaint 效果
//
//  ## 主要功能
//  - removeObject: 从图像中移除指定区域的物体
//  - fillAtScale: 多尺度金字塔填充
//  - generateFill: 从周围像素生成自然填充纹理
//
//  ## 技术栈
//  - CoreImage: CIFilter 链式处理
//  - 多尺度采样 + 纹理合成 + 边缘混合
//
//  ## 算法原理
//  - 1. 扩展掩码区域并羽化边缘
//  - 2. 从周围区域采样纹理
//  - 3. 多尺度金字塔填充（从粗到细）
//  - 4. 边缘渐变混合 + 噪点模拟自然过渡
//

import Foundation
import CoreImage
import UIKit
import Accelerate

#if os(iOS)

/// 物体移除器
final class ObjectRemover: ObservableObject {

    // MARK: - 发布属性

    @Published var isProcessing = false
    @Published var progress: Float = 0

    // MARK: - 私有属性

    private let context: CIContext
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "ObjectRemover"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "ObjectRemover"
            ])
        }
    }

    // MARK: - 物体移除

    /// 从图像中移除指定区域的物体
    /// - Parameters:
    ///   - image: 输入 CIImage
    ///   - maskRect: 用户绘制的物体区域矩形（归一化坐标 0...1）
    ///   - imageSize: 图像尺寸
    /// - Returns: 移除物体后的 CIImage，失败返回 nil
    func removeObject(
        from image: CIImage,
        maskRect: CGRect,
        imageSize: CGSize
    ) async -> CIImage? {
        await MainActor.run {
            isProcessing = true
            progress = 0
        }

        let extent = image.extent
        let result = await Task.detached(priority: .userInitiated) { [weak self] () -> CIImage in
            guard let self = self else { return image }

            // 1. 将归一化 rect 转换为图像坐标
            let pixelRect = CGRect(
                x: maskRect.origin.x * imageSize.width,
                y: maskRect.origin.y * imageSize.height,
                width: maskRect.size.width * imageSize.width,
                height: maskRect.size.height * imageSize.height
            )

            // 2. 扩展掩码区域（向外扩展 15%）
            let expandedRect = pixelRect.insetBy(
                dx: -pixelRect.width * 0.15,
                dy: -pixelRect.height * 0.15
            )

            // 3. 创建羽化掩码
            let mask = self.createFeatheredMask(rect: expandedRect, imageExtent: extent)

            // 4. 多尺度内容感知填充
            var output = image
            let scales: [CGFloat] = [0.25, 0.5, 0.75, 1.0]

            for (index, scale) in scales.enumerated() {
                let fillSource = self.generateFill(
                    from: output,
                    targetSize: CGSize(width: expandedRect.width * scale, height: expandedRect.height * scale),
                    excludeRect: expandedRect
                )
                output = self.fillAtScale(
                    image: output,
                    mask: mask,
                    fillSource: fillSource,
                    scale: scale
                )

                await MainActor.run {
                    self.progress = Float(index + 1) / Float(scales.count)
                }
            }

            // 5. 最终边缘平滑
            output = self.smoothEdges(output, mask: mask)

            return output
        }.value

        await MainActor.run {
            isProcessing = false
            progress = 1.0
        }

        return result
    }

    // MARK: - 羽化掩码

    /// 创建带羽化边缘的掩码（白色=填充区域，黑色=保留区域）
    private func createFeatheredMask(rect: CGRect, imageExtent: CGRect) -> CIImage {
        let renderer = UIGraphicsImageRenderer(size: imageExtent.size)
        let uiImage = renderer.image { ctx in
            // 黑色背景（保留区域）
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: imageExtent.size))

            // 白色椭圆（填充区域）
            UIColor.white.setFill()
            let path = UIBezierPath(roundedRect: rect, cornerRadius: rect.width * 0.1)
            path.fill()
        }

        guard let cgImage = uiImage.cgImage else {
            return CIImage(color: CIColor(red: 0, green: 0, blue: 0))
        }

        var mask = CIImage(cgImage: cgImage)

        // 羽化边缘，使过渡自然
        let blurRadius = max(rect.width, rect.height) * 0.05
        mask = mask.applyingGaussianBlur(sigma: Double(blurRadius))

        return mask
    }

    // MARK: - 多尺度填充

    /// 在指定尺度进行内容感知填充
    private func fillAtScale(
        image: CIImage,
        mask: CIImage,
        fillSource: CIImage,
        scale: CGFloat
    ) -> CIImage {
        let extent = image.extent

        // 缩放图像和掩码到目标尺度
        let scaledImage: CIImage
        let scaledMask: CIImage
        let scaledFill: CIImage

        if scale < 1.0 {
            let scaleFilter = CIFilter.lanczosScaleTransform()
            scaleFilter.inputImage = image
            scaleFilter.scale = Float(scale)
            scaleFilter.aspectRatio = 1.0
            scaledImage = scaleFilter.outputImage ?? image

            let maskScaleFilter = CIFilter.lanczosScaleTransform()
            maskScaleFilter.inputImage = mask
            maskScaleFilter.scale = Float(scale)
            maskScaleFilter.aspectRatio = 1.0
            scaledMask = maskScaleFilter.outputImage ?? mask

            let fillScaleFilter = CIFilter.lanczosScaleTransform()
            fillScaleFilter.inputImage = fillSource
            fillScaleFilter.scale = Float(scale)
            fillScaleFilter.aspectRatio = 1.0
            scaledFill = fillScaleFilter.outputImage ?? fillSource
        } else {
            scaledImage = image
            scaledMask = mask
            scaledFill = fillSource
        }

        // 使用掩码混合填充源和原始图像
        let blend = CIFilter.blendWithMask()
        blend.inputImage = scaledFill      // 填充源（替代移除区域）
        blend.backgroundImage = scaledImage // 原始图像（保留区域）
        blend.maskImage = scaledMask        // 掩码控制混合区域

        var result = blend.outputImage ?? scaledImage

        // 如果缩放了，需要放大回去
        if scale < 1.0 {
            let upscaleFilter = CIFilter.lanczosScaleTransform()
            upscaleFilter.inputImage = result
            upscaleFilter.scale = Float(1.0 / scale)
            upscaleFilter.aspectRatio = 1.0
            result = upscaleFilter.outputImage?.cropped(to: extent) ?? result
        }

        return result
    }

    // MARK: - 纹理生成

    /// 从周围区域采样生成自然填充纹理
    private func generateFill(
        from source: CIImage,
        targetSize: CGSize,
        excludeRect: CGRect
    ) -> CIImage {
        let extent = source.extent

        // 1. 采样排除区域周围的像素
        // 顶部采样带
        let topBand = source.cropped(to: CGRect(
            x: excludeRect.origin.x,
            y: max(0, excludeRect.origin.y - excludeRect.height * 0.5),
            width: excludeRect.width,
            height: min(excludeRect.height * 0.5, excludeRect.origin.y)
        ))

        // 底部采样带
        let bottomBand = source.cropped(to: CGRect(
            x: excludeRect.origin.x,
            y: excludeRect.maxY,
            width: excludeRect.width,
            height: min(excludeRect.height * 0.5, extent.height - excludeRect.maxY)
        ))

        // 左侧采样带
        let leftBand = source.cropped(to: CGRect(
            x: max(0, excludeRect.origin.x - excludeRect.width * 0.5),
            y: excludeRect.origin.y,
            width: min(excludeRect.width * 0.5, excludeRect.origin.x),
            height: excludeRect.height
        ))

        // 右侧采样带
        let rightBand = source.cropped(to: CGRect(
            x: excludeRect.maxX,
            y: excludeRect.origin.y,
            width: min(excludeRect.width * 0.5, extent.width - excludeRect.maxX),
            height: excludeRect.height
        ))

        // 2. 创建渐变背景（从周围颜色插值）
        let gradient = createGradientFill(
            from: source,
            excludeRect: excludeRect,
            targetSize: targetSize
        )

        // 3. 镜像采样带纹理覆盖目标区域
        var fillImage = gradient

        if topBand.extent.height > 0 {
            let mirroredTop = mirrorBand(topBand, direction: .vertical, targetSize: targetSize)
            fillImage = blendBands(fillImage, mirroredTop, alpha: 0.25)
        }

        if bottomBand.extent.height > 0 {
            let mirroredBottom = mirrorBand(bottomBand, direction: .vertical, targetSize: targetSize)
            fillImage = blendBands(fillImage, mirroredBottom, alpha: 0.25)
        }

        if leftBand.extent.width > 0 {
            let mirroredLeft = mirrorBand(leftBand, direction: .horizontal, targetSize: targetSize)
            fillImage = blendBands(fillImage, mirroredLeft, alpha: 0.25)
        }

        if rightBand.extent.width > 0 {
            let mirroredRight = mirrorBand(rightBand, direction: .horizontal, targetSize: targetSize)
            fillImage = blendBands(fillImage, mirroredRight, alpha: 0.25)
        }

        // 4. 添加噪点使纹理自然
        fillImage = addNoise(to: fillImage, intensity: 0.03)

        // 5. 轻微模糊使过渡平滑
        fillImage = fillImage.applyingGaussianBlur(sigma: 1.0)

        return fillImage
    }

    /// 创建渐变填充（从周围颜色插值）
    private func createGradientFill(
        from source: CIImage,
        excludeRect: CGRect,
        targetSize: CGSize
    ) -> CIImage {
        let extent = source.extent

        // 采样排除区域周围的平均颜色
        let sampleSize: CGFloat = 20

        // 顶部颜色
        let topColor = averageColor(of: source, in: CGRect(
            x: excludeRect.midX - sampleSize / 2,
            y: max(0, excludeRect.origin.y - sampleSize),
            width: sampleSize,
            height: min(sampleSize, excludeRect.origin.y)
        ))

        // 底部颜色
        let bottomColor = averageColor(of: source, in: CGRect(
            x: excludeRect.midX - sampleSize / 2,
            y: excludeRect.maxY,
            width: sampleSize,
            height: min(sampleSize, extent.height - excludeRect.maxY)
        ))

        // 使用两个颜色创建线性渐变
        let topPoint = CIVector(x: targetSize.width / 2, y: 0)
        let bottomPoint = CIVector(x: targetSize.width / 2, y: targetSize.height)

        let gradient = CIFilter.linearGradient()
        gradient.point0 = topPoint
        gradient.point1 = bottomPoint
        gradient.color0 = topColor
        gradient.color1 = bottomColor

        return gradient.outputImage?.cropped(to: CGRect(origin: .zero, size: targetSize))
            ?? CIImage(color: topColor).cropped(to: CGRect(origin: .zero, size: targetSize))
    }

    /// 获取指定区域的平均颜色
    private func averageColor(of image: CIImage, in rect: CGRect) -> CIColor {
        let validRect = rect.intersection(image.extent)
        guard validRect.width > 0 && validRect.height > 0 else {
            return CIColor(red: 0.5, green: 0.5, blue: 0.5)
        }

        let areaAverage = CIFilter.areaAverage()
        areaAverage.inputImage = image.cropped(to: validRect)
        areaAverage.extent = validRect

        guard let output = areaAverage.outputImage else {
            return CIColor(red: 0.5, green: 0.5, blue: 0.5)
        }

        var bitmap = [UInt8](repeating: 0, count: 4)
        context.render(output, toBitmap: &bitmap, rowBytes: 4,
                       bounds: CGRect(x: 0, y: 0, width: 1, height: 1),
                       format: .RGBA8, colorSpace: colorSpace)

        return CIColor(
            red: CGFloat(bitmap[0]) / 255.0,
            green: CGFloat(bitmap[1]) / 255.0,
            blue: CGFloat(bitmap[2]) / 255.0,
            alpha: CGFloat(bitmap[3]) / 255.0
        )
    }

    /// 镜像采样带纹理
    private func mirrorBand(
        _ band: CIImage,
        direction: MirrorDirection,
        targetSize: CGSize
    ) -> CIImage {
        let bandExtent = band.extent
        guard bandExtent.width > 0 && bandExtent.height > 0 else { return band }

        var transform: CGAffineTransform
        var scaledBand: CIImage

        switch direction {
        case .vertical:
            // 垂直翻转并缩放到目标尺寸
            let scaleY = targetSize.height / bandExtent.height
            transform = CGAffineTransform(scaleX: 1, y: -scaleY)
                .translatedBy(x: 0, y: -targetSize.height)

            let scaleFilter = CIFilter.lanczosScaleTransform()
            scaleFilter.inputImage = band
            scaleFilter.scale = Float(targetSize.width / bandExtent.width)
            scaleFilter.aspectRatio = Float(targetSize.height / bandExtent.height / (targetSize.width / bandExtent.width))
            scaledBand = scaleFilter.outputImage?.cropped(to: CGRect(origin: .zero, size: targetSize)) ?? band

        case .horizontal:
            let scaleX = targetSize.width / bandExtent.width
            transform = CGAffineTransform(scaleX: -scaleX, y: 1)
                .translatedBy(x: -targetSize.width, y: 0)

            let scaleFilter = CIFilter.lanczosScaleTransform()
            scaleFilter.inputImage = band
            scaleFilter.scale = Float(targetSize.width / bandExtent.width)
            scaleFilter.aspectRatio = Float(targetSize.height / bandExtent.height / (targetSize.width / bandExtent.width))
            scaledBand = scaleFilter.outputImage?.cropped(to: CGRect(origin: .zero, size: targetSize)) ?? band
        }

        // 应用镜像变换
        let transformed = scaledBand.transformed(by: transform)
        return transformed.cropped(to: CGRect(origin: .zero, size: targetSize))
    }

    /// 混合两个纹理
    private func blendBands(_ base: CIImage, _ overlay: CIImage, alpha: CGFloat) -> CIImage {
        let alphaFilter = CIFilter.colorMatrix()
        alphaFilter.inputImage = overlay
        alphaFilter.aVector = CIVector(x: 0, y: 0, z: 0, w: alpha)
        alphaFilter.biasVector = CIVector(x: 0, y: 0, z: 0, w: 0)

        let fadedOverlay = alphaFilter.outputImage ?? overlay

        let blend = CIFilter.sourceOverCompositing()
        blend.inputImage = fadedOverlay
        blend.backgroundImage = base

        return blend.outputImage ?? base
    }

    /// 添加随机噪点
    private func addNoise(to image: CIImage, intensity: Float) -> CIImage {
        guard let noiseFilter = CIFilter(name: "CIRandomGenerator") else { return image }
        guard let noiseImage = noiseFilter.outputImage else { return image }

        // 将噪点缩放到合适范围
        let monoNoise = noiseImage
            .applyingFilter("CIColorMatrix", parameters: [
                "inputRVector": CIVector(x: 0.5, y: 0, z: 0, w: 0),
                "inputGVector": CIVector(x: 0.5, y: 0, z: 0, w: 0),
                "inputBVector": CIVector(x: 0.5, y: 0, z: 0, w: 0),
                "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0),
                "inputBiasVector": CIVector(x: 0.5, y: 0.5, z: 0.5, w: 0)
            ])
            .cropped(to: image.extent)

        // 混合噪点
        let blend = CIFilter.sourceOverCompositing()
        blend.inputImage = monoNoise
        blend.backgroundImage = image

        // 用透明度控制噪点强度
        let alphaFilter = CIFilter.colorMatrix()
        alphaFilter.inputImage = blend.outputImage ?? image
        let alpha = CGFloat(intensity)
        alphaFilter.aVector = CIVector(x: 0, y: 0, z: 0, w: alpha)
        alphaFilter.biasVector = CIVector(x: 0, y: 0, z: 0, w: 1 - alpha)

        return alphaFilter.outputImage ?? image
    }

    // MARK: - 边缘平滑

    /// 平滑填充区域边缘
    private func smoothEdges(_ image: CIImage, mask: CIImage) -> CIImage {
        let extent = image.extent

        // 使用形态学梯度检测边缘
        let gradientMask = mask
            .applyingFilter("CIMorphologyGradient", parameters: [
                kCIInputRadiusKey: 5.0
            ])

        // 在边缘区域应用模糊
        let blurred = image
            .clampedToExtent()
            .applyingGaussianBlur(sigma: 3.0)
            .cropped(to: extent)

        // 只在边缘区域混合模糊
        let blend = CIFilter.blendWithMask()
        blend.inputImage = blurred
        blend.backgroundImage = image
        blend.maskImage = gradientMask

        return blend.outputImage ?? image
    }

    // MARK: - 镜像方向

    private enum MirrorDirection {
        case vertical
        case horizontal
    }
}

// MARK: - Metal 导入

import Metal

#endif