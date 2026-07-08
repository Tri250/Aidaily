//
//  ImageExpander.swift
//  LiveCapture
//
//  图像扩展器 - 基于边缘镜像和内容感知的图像外扩填充
//
//  ## 主要功能
//  - expandImage: 向外扩展图像，自动填充新增区域
//  - mirrorEdge: 镜像边缘像素
//  - blendSeams: 在接缝处混合使过渡自然
//
//  ## 技术栈
//  - CoreImage: CILanczosScaleTransform、CIAffineTransform、CIGaussianBlur
//  - 边缘镜像 + 渐变淡出 + 噪点纹理
//
//  ## 算法原理
//  - 1. 创建更大的画布，将原图放在中心
//  - 2. 对每个扩展方向，截取边缘条带并镜像翻转
//  - 3. 对镜像条带应用渐变淡出遮罩
//  - 4. 在接缝处混合模糊
//  - 5. 添加噪点使扩展区域看起来自然
//

import Foundation
import CoreImage
import Accelerate
import UIKit

#if os(iOS)

/// 图像扩展器
final class ImageExpander: ObservableObject {

    // MARK: - 发布属性

    @Published var isProcessing = false
    @Published var progress: Float = 0

    // MARK: - 扩展方向

    enum ExpansionDirection: String, CaseIterable {
        case all
        case horizontal
        case vertical
        case up
        case down
        case left
        case right

        var displayName: String {
            switch self {
            case .all: return "全部"
            case .horizontal: return "水平"
            case .vertical: return "垂直"
            case .up: return "向上"
            case .down: return "向下"
            case .left: return "向左"
            case .right: return "向右"
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
                .name: "ImageExpander"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "ImageExpander"
            ])
        }
    }

    // MARK: - 图像扩展

    /// 向外扩展图像，自动填充新增区域
    /// - Parameters:
    ///   - image: 输入 CIImage
    ///   - expandBy: 每边扩展像素数
    ///   - direction: 扩展方向
    /// - Returns: 扩展后的 CIImage，失败返回 nil
    func expandImage(
        _ image: CIImage,
        expandBy: CGFloat,
        direction: ExpansionDirection = .all
    ) async -> CIImage? {
        await MainActor.run {
            isProcessing = true
            progress = 0
        }

        let result = await Task.detached(priority: .userInitiated) { [weak self] () -> CIImage in
            guard let self = self else { return image }

            let originalExtent = image.extent
            let expandPixels = max(expandBy, 0)

            guard expandPixels > 0 else { return image }

            // 计算各方向扩展量
            let topExpand: CGFloat
            let bottomExpand: CGFloat
            let leftExpand: CGFloat
            let rightExpand: CGFloat

            switch direction {
            case .all:
                topExpand = expandPixels
                bottomExpand = expandPixels
                leftExpand = expandPixels
                rightExpand = expandPixels
            case .horizontal:
                topExpand = 0
                bottomExpand = 0
                leftExpand = expandPixels
                rightExpand = expandPixels
            case .vertical:
                topExpand = expandPixels
                bottomExpand = expandPixels
                leftExpand = 0
                rightExpand = 0
            case .up:
                topExpand = expandPixels
                bottomExpand = 0
                leftExpand = 0
                rightExpand = 0
            case .down:
                topExpand = 0
                bottomExpand = expandPixels
                leftExpand = 0
                rightExpand = 0
            case .left:
                topExpand = 0
                bottomExpand = 0
                leftExpand = expandPixels
                rightExpand = 0
            case .right:
                topExpand = 0
                bottomExpand = 0
                leftExpand = 0
                rightExpand = expandPixels
            }

            // 新画布尺寸
            let newWidth = originalExtent.width + leftExpand + rightExpand
            let newHeight = originalExtent.height + topExpand + bottomExpand
            let newExtent = CGRect(x: 0, y: 0, width: newWidth, height: newHeight)

            // 原图在新画布中的位置
            let imagePlacement = CGRect(
                x: leftExpand,
                y: bottomExpand,
                width: originalExtent.width,
                height: originalExtent.height
            )

            // 1. 创建黑色背景画布
            var canvas = CIImage(color: CIColor(red: 0, green: 0, blue: 0, alpha: 1))
                .cropped(to: newExtent)

            // 2. 将原图放在中心
            let translatedImage = image.transformed(by: CGAffineTransform(
                translationX: leftExpand - originalExtent.origin.x,
                y: bottomExpand - originalExtent.origin.y
            ))

            let imageOver = CIFilter.sourceOverCompositing()
            imageOver.inputImage = translatedImage
            imageOver.backgroundImage = canvas
            canvas = imageOver.outputImage ?? canvas

            await MainActor.run { self.progress = 0.2 }

            // 3. 填充各方向的扩展区域
            var stepCount: Float = 0
            let totalSteps: Float = Float(
                (topExpand > 0 ? 1 : 0) +
                (bottomExpand > 0 ? 1 : 0) +
                (leftExpand > 0 ? 1 : 0) +
                (rightExpand > 0 ? 1 : 0)
            )

            // 顶部扩展
            if topExpand > 0 {
                let topEdge = self.mirrorEdge(
                    image,
                    edge: .up,
                    width: topExpand,
                    originalExtent: originalExtent,
                    newExtent: newExtent,
                    imagePlacement: imagePlacement
                )
                let topOver = CIFilter.sourceOverCompositing()
                topOver.inputImage = topEdge
                topOver.backgroundImage = canvas
                canvas = topOver.outputImage ?? canvas
                stepCount += 1
                await MainActor.run { self.progress = 0.2 + 0.6 * (stepCount / totalSteps) }
            }

            // 底部扩展
            if bottomExpand > 0 {
                let bottomEdge = self.mirrorEdge(
                    image,
                    edge: .down,
                    width: bottomExpand,
                    originalExtent: originalExtent,
                    newExtent: newExtent,
                    imagePlacement: imagePlacement
                )
                let bottomOver = CIFilter.sourceOverCompositing()
                bottomOver.inputImage = bottomEdge
                bottomOver.backgroundImage = canvas
                canvas = bottomOver.outputImage ?? canvas
                stepCount += 1
                await MainActor.run { self.progress = 0.2 + 0.6 * (stepCount / totalSteps) }
            }

            // 左侧扩展
            if leftExpand > 0 {
                let leftEdge = self.mirrorEdge(
                    image,
                    edge: .left,
                    width: leftExpand,
                    originalExtent: originalExtent,
                    newExtent: newExtent,
                    imagePlacement: imagePlacement
                )
                let leftOver = CIFilter.sourceOverCompositing()
                leftOver.inputImage = leftEdge
                leftOver.backgroundImage = canvas
                canvas = leftOver.outputImage ?? canvas
                stepCount += 1
                await MainActor.run { self.progress = 0.2 + 0.6 * (stepCount / totalSteps) }
            }

            // 右侧扩展
            if rightExpand > 0 {
                let rightEdge = self.mirrorEdge(
                    image,
                    edge: .right,
                    width: rightExpand,
                    originalExtent: originalExtent,
                    newExtent: newExtent,
                    imagePlacement: imagePlacement
                )
                let rightOver = CIFilter.sourceOverCompositing()
                rightOver.inputImage = rightEdge
                rightOver.backgroundImage = canvas
                canvas = rightOver.outputImage ?? canvas
                stepCount += 1
                await MainActor.run { self.progress = 0.2 + 0.6 * (stepCount / totalSteps) }
            }

            // 4. 填充角落区域（如果同时在两个方向扩展）
            if topExpand > 0 && leftExpand > 0 {
                canvas = self.fillCorner(
                    canvas,
                    corner: .topLeft,
                    size: CGSize(width: leftExpand, height: topExpand),
                    newExtent: newExtent,
                    imagePlacement: imagePlacement
                )
            }
            if topExpand > 0 && rightExpand > 0 {
                canvas = self.fillCorner(
                    canvas,
                    corner: .topRight,
                    size: CGSize(width: rightExpand, height: topExpand),
                    newExtent: newExtent,
                    imagePlacement: imagePlacement
                )
            }
            if bottomExpand > 0 && leftExpand > 0 {
                canvas = self.fillCorner(
                    canvas,
                    corner: .bottomLeft,
                    size: CGSize(width: leftExpand, height: bottomExpand),
                    newExtent: newExtent,
                    imagePlacement: imagePlacement
                )
            }
            if bottomExpand > 0 && rightExpand > 0 {
                canvas = self.fillCorner(
                    canvas,
                    corner: .bottomRight,
                    size: CGSize(width: rightExpand, height: bottomExpand),
                    newExtent: newExtent,
                    imagePlacement: imagePlacement
                )
            }

            await MainActor.run { self.progress = 0.85 }

            // 5. 在接缝处模糊混合
            canvas = self.blendSeams(canvas, imagePlacement: imagePlacement)

            await MainActor.run { self.progress = 0.95 }

            // 6. 添加噪点使扩展区域自然
            canvas = self.addExpansionNoise(canvas, imagePlacement: imagePlacement)

            await MainActor.run { self.progress = 1.0 }

            return canvas
        }.value

        await MainActor.run {
            isProcessing = false
            progress = 1.0
        }

        return result
    }

    // MARK: - 边缘镜像

    /// 镜像图像边缘生成扩展条带
    private func mirrorEdge(
        _ image: CIImage,
        edge: ExpansionDirection,
        width: CGFloat,
        originalExtent: CGRect,
        newExtent: CGRect,
        imagePlacement: CGRect
    ) -> CIImage {
        var edgeStrip: CIImage
        var transform: CGAffineTransform

        switch edge {
        case .up:
            // 截取原图顶部条带
            edgeStrip = image.cropped(to: CGRect(
                x: originalExtent.origin.x,
                y: originalExtent.maxY - width,
                width: originalExtent.width,
                height: width
            ))

            // 垂直翻转
            transform = CGAffineTransform(scaleX: 1, y: -1)
                .translatedBy(x: 0, y: -(originalExtent.maxY - width + originalExtent.maxY))

            edgeStrip = edgeStrip.transformed(by: transform)

            // 移动到目标位置
            let finalTransform = CGAffineTransform(
                translationX: imagePlacement.origin.x - edgeStrip.extent.origin.x,
                y: imagePlacement.maxY - edgeStrip.extent.origin.y
            )
            edgeStrip = edgeStrip.transformed(by: finalTransform)

        case .down:
            // 截取原图底部条带
            edgeStrip = image.cropped(to: CGRect(
                x: originalExtent.origin.x,
                y: originalExtent.origin.y,
                width: originalExtent.width,
                height: width
            ))

            // 垂直翻转
            transform = CGAffineTransform(scaleX: 1, y: -1)
                .translatedBy(x: 0, y: -(originalExtent.origin.y + originalExtent.origin.y))

            edgeStrip = edgeStrip.transformed(by: transform)

            // 移动到目标位置
            let finalTransform = CGAffineTransform(
                translationX: imagePlacement.origin.x - edgeStrip.extent.origin.x,
                y: (imagePlacement.origin.y - width) - edgeStrip.extent.origin.y
            )
            edgeStrip = edgeStrip.transformed(by: finalTransform)

        case .left:
            // 截取原图左侧条带
            edgeStrip = image.cropped(to: CGRect(
                x: originalExtent.origin.x,
                y: originalExtent.origin.y,
                width: width,
                height: originalExtent.height
            ))

            // 水平翻转
            transform = CGAffineTransform(scaleX: -1, y: 1)
                .translatedBy(x: -(originalExtent.origin.x + originalExtent.origin.x), y: 0)

            edgeStrip = edgeStrip.transformed(by: transform)

            // 移动到目标位置
            let finalTransform = CGAffineTransform(
                translationX: (imagePlacement.origin.x - width) - edgeStrip.extent.origin.x,
                y: imagePlacement.origin.y - edgeStrip.extent.origin.y
            )
            edgeStrip = edgeStrip.transformed(by: finalTransform)

        case .right:
            // 截取原图右侧条带
            edgeStrip = image.cropped(to: CGRect(
                x: originalExtent.maxX - width,
                y: originalExtent.origin.y,
                width: width,
                height: originalExtent.height
            ))

            // 水平翻转
            transform = CGAffineTransform(scaleX: -1, y: 1)
                .translatedBy(x: -(originalExtent.maxX - width + originalExtent.maxX), y: 0)

            edgeStrip = edgeStrip.transformed(by: transform)

            // 移动到目标位置
            let finalTransform = CGAffineTransform(
                translationX: imagePlacement.maxX - edgeStrip.extent.origin.x,
                y: imagePlacement.origin.y - edgeStrip.extent.origin.y
            )
            edgeStrip = edgeStrip.transformed(by: finalTransform)

        default:
            return CIImage(color: CIColor(red: 0, green: 0, blue: 0))
        }

        // 应用渐变淡出（从接缝处向外渐变）
        edgeStrip = applyFadeMask(to: edgeStrip, edge: edge, width: width)

        return edgeStrip
    }

    /// 应用渐变淡出掩码（从接缝处向外渐变透明）
    private func applyFadeMask(
        to strip: CIImage,
        edge: ExpansionDirection,
        width: CGFloat
    ) -> CIImage {
        let stripExtent = strip.extent
        guard stripExtent.width > 0 && stripExtent.height > 0 else { return strip }

        let gradient: CIFilter
        let point0: CIVector
        let point1: CIVector

        switch edge {
        case .up:
            gradient = CIFilter.linearGradient()
            point0 = CIVector(x: stripExtent.midX, y: stripExtent.minY)  // 接缝处
            point1 = CIVector(x: stripExtent.midX, y: stripExtent.maxY)  // 远处
            gradient.point0 = point0
            gradient.point1 = point1
            gradient.color0 = CIColor(red: 1, green: 1, blue: 1, alpha: 1)
            gradient.color1 = CIColor(red: 1, green: 1, blue: 1, alpha: 0)

        case .down:
            gradient = CIFilter.linearGradient()
            point0 = CIVector(x: stripExtent.midX, y: stripExtent.maxY)  // 接缝处
            point1 = CIVector(x: stripExtent.midX, y: stripExtent.minY)  // 远处
            gradient.point0 = point0
            gradient.point1 = point1
            gradient.color0 = CIColor(red: 1, green: 1, blue: 1, alpha: 1)
            gradient.color1 = CIColor(red: 1, green: 1, blue: 1, alpha: 0)

        case .left:
            gradient = CIFilter.linearGradient()
            point0 = CIVector(x: stripExtent.maxX, y: stripExtent.midY)  // 接缝处
            point1 = CIVector(x: stripExtent.minX, y: stripExtent.midY)  // 远处
            gradient.point0 = point0
            gradient.point1 = point1
            gradient.color0 = CIColor(red: 1, green: 1, blue: 1, alpha: 1)
            gradient.color1 = CIColor(red: 1, green: 1, blue: 1, alpha: 0)

        case .right:
            gradient = CIFilter.linearGradient()
            point0 = CIVector(x: stripExtent.minX, y: stripExtent.midY)  // 接缝处
            point1 = CIVector(x: stripExtent.maxX, y: stripExtent.midY)  // 远处
            gradient.point0 = point0
            gradient.point1 = point1
            gradient.color0 = CIColor(red: 1, green: 1, blue: 1, alpha: 1)
            gradient.color1 = CIColor(red: 1, green: 1, blue: 1, alpha: 0)

        default:
            return strip
        }

        guard let maskImage = gradient.outputImage?.cropped(to: stripExtent) else {
            return strip
        }

        // 使用掩码混合
        let blend = CIFilter.blendWithMask()
        blend.inputImage = strip
        blend.backgroundImage = CIImage(color: CIColor(red: 0, green: 0, blue: 0, alpha: 0))
            .cropped(to: stripExtent)
        blend.maskImage = maskImage

        return blend.outputImage ?? strip
    }

    // MARK: - 角落填充

    /// 填充角落区域（双向扩展的交汇处）
    private func fillCorner(
        _ canvas: CIImage,
        corner: Corner,
        size: CGSize,
        newExtent: CGRect,
        imagePlacement: CGRect
    ) -> CIImage {
        // 角落区域矩形
        let cornerRect: CGRect
        switch corner {
        case .topLeft:
            cornerRect = CGRect(
                x: 0,
                y: imagePlacement.maxY,
                width: size.width,
                height: size.height
            )
        case .topRight:
            cornerRect = CGRect(
                x: imagePlacement.maxX,
                y: imagePlacement.maxY,
                width: size.width,
                height: size.height
            )
        case .bottomLeft:
            cornerRect = CGRect(
                x: 0,
                y: 0,
                width: size.width,
                height: size.height
            )
        case .bottomRight:
            cornerRect = CGRect(
                x: imagePlacement.maxX,
                y: 0,
                width: size.width,
                height: size.height
            )
        }

        // 采样角落附近颜色
        let sampleColor = sampleCornerColor(
            from: canvas,
            corner: corner,
            imagePlacement: imagePlacement,
            cornerRect: cornerRect
        )

        // 创建径向渐变填充角落
        let center: CIVector
        switch corner {
        case .topLeft:
            center = CIVector(x: imagePlacement.minX, y: imagePlacement.maxY)
        case .topRight:
            center = CIVector(x: imagePlacement.maxX, y: imagePlacement.maxY)
        case .bottomLeft:
            center = CIVector(x: imagePlacement.minX, y: imagePlacement.minY)
        case .bottomRight:
            center = CIVector(x: imagePlacement.maxX, y: imagePlacement.minY)
        }

        let radialGradient = CIFilter.radialGradient()
        radialGradient.center = center
        radialGradient.radius0 = 0
        radialGradient.radius1 = Float(max(size.width, size.height) * 1.5)
        radialGradient.color0 = sampleColor
        radialGradient.color1 = CIColor(red: 0, green: 0, blue: 0, alpha: 0)

        guard let gradientImage = radialGradient.outputImage?.cropped(to: cornerRect) else {
            return canvas
        }

        let blend = CIFilter.sourceOverCompositing()
        blend.inputImage = gradientImage
        blend.backgroundImage = canvas

        return blend.outputImage ?? canvas
    }

    /// 采样角落附近的颜色
    private func sampleCornerColor(
        from canvas: CIImage,
        corner: Corner,
        imagePlacement: CGRect,
        cornerRect: CGRect
    ) -> CIColor {
        let sampleRect: CGRect
        let sampleSize: CGFloat = 10

        switch corner {
        case .topLeft:
            sampleRect = CGRect(
                x: imagePlacement.minX,
                y: imagePlacement.maxY - sampleSize,
                width: sampleSize,
                height: sampleSize
            )
        case .topRight:
            sampleRect = CGRect(
                x: imagePlacement.maxX - sampleSize,
                y: imagePlacement.maxY - sampleSize,
                width: sampleSize,
                height: sampleSize
            )
        case .bottomLeft:
            sampleRect = CGRect(
                x: imagePlacement.minX,
                y: imagePlacement.minY,
                width: sampleSize,
                height: sampleSize
            )
        case .bottomRight:
            sampleRect = CGRect(
                x: imagePlacement.maxX - sampleSize,
                y: imagePlacement.minY,
                width: sampleSize,
                height: sampleSize
            )
        }

        let areaAverage = CIFilter.areaAverage()
        areaAverage.inputImage = canvas.cropped(to: sampleRect)
        areaAverage.extent = sampleRect

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
            alpha: 1.0
        )
    }

    // MARK: - 接缝混合

    /// 在接缝处进行模糊混合
    private func blendSeams(_ canvas: CIImage, imagePlacement: CGRect) -> CIImage {
        let extent = canvas.extent

        // 创建接缝掩码（在接缝处为白色）
        let renderer = UIGraphicsImageRenderer(size: extent.size)
        let maskImage = renderer.image { ctx in
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: extent.size))

            // 原图区域为黑色（不模糊）
            UIColor.black.setFill()
            ctx.fill(imagePlacement)

            // 接缝附近为白色（需要模糊）
            UIColor.white.setFill()

            let seamWidth: CGFloat = 15

            // 顶部接缝
            if imagePlacement.maxY < extent.height {
                ctx.fill(CGRect(
                    x: imagePlacement.minX,
                    y: imagePlacement.maxY - seamWidth / 2,
                    width: imagePlacement.width,
                    height: seamWidth
                ))
            }

            // 底部接缝
            if imagePlacement.minY > 0 {
                ctx.fill(CGRect(
                    x: imagePlacement.minX,
                    y: imagePlacement.minY - seamWidth / 2,
                    width: imagePlacement.width,
                    height: seamWidth
                ))
            }

            // 左侧接缝
            if imagePlacement.minX > 0 {
                ctx.fill(CGRect(
                    x: imagePlacement.minX - seamWidth / 2,
                    y: imagePlacement.minY,
                    width: seamWidth,
                    height: imagePlacement.height
                ))
            }

            // 右侧接缝
            if imagePlacement.maxX < extent.width {
                ctx.fill(CGRect(
                    x: imagePlacement.maxX - seamWidth / 2,
                    y: imagePlacement.minY,
                    width: seamWidth,
                    height: imagePlacement.height
                ))
            }
        }

        guard let cgMask = maskImage.cgImage else { return canvas }

        var seamMask = CIImage(cgImage: cgMask)
        seamMask = seamMask.applyingGaussianBlur(sigma: 3.0)

        // 在接缝区域应用模糊
        let blurred = canvas
            .clampedToExtent()
            .applyingGaussianBlur(sigma: 4.0)
            .cropped(to: extent)

        let blend = CIFilter.blendWithMask()
        blend.inputImage = blurred
        blend.backgroundImage = canvas
        blend.maskImage = seamMask

        return blend.outputImage ?? canvas
    }

    // MARK: - 噪点添加

    /// 在扩展区域添加噪点
    private func addExpansionNoise(_ canvas: CIImage, imagePlacement: CGRect) -> CIImage {
        let extent = canvas.extent

        guard let noiseGenerator = CIFilter(name: "CIRandomGenerator"),
              let noise = noiseGenerator.outputImage else {
            return canvas
        }

        // 处理噪点
        let processedNoise = noise
            .cropped(to: extent)
            .applyingFilter("CIColorMatrix", parameters: [
                "inputRVector": CIVector(x: 0.3, y: 0, z: 0, w: 0),
                "inputGVector": CIVector(x: 0.3, y: 0, z: 0, w: 0),
                "inputBVector": CIVector(x: 0.3, y: 0, z: 0, w: 0),
                "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0),
                "inputBiasVector": CIVector(x: 0.5, y: 0.5, z: 0.5, w: 0)
            ])

        // 创建扩展区域掩码（扩展区域=白色，原图区域=黑色）
        let renderer = UIGraphicsImageRenderer(size: extent.size)
        let expansionMaskImage = renderer.image { ctx in
            UIColor.white.setFill()
            ctx.fill(CGRect(origin: .zero, size: extent.size))

            UIColor.black.setFill()
            ctx.fill(imagePlacement)
        }

        guard let cgMask = expansionMaskImage.cgImage else { return canvas }

        var expansionMask = CIImage(cgImage: cgMask)
        expansionMask = expansionMask.applyingGaussianBlur(sigma: 5.0)

        // 只在扩展区域混合噪点
        let blend = CIFilter.blendWithMask()
        blend.inputImage = processedNoise
        blend.backgroundImage = canvas
        blend.maskImage = expansionMask

        return blend.outputImage ?? canvas
    }

    // MARK: - 角落枚举

    private enum Corner {
        case topLeft, topRight, bottomLeft, bottomRight
    }
}

// MARK: - Metal 导入

import Metal

#endif