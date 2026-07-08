//
//  WatermarkService.swift
//  LiveCapture
//
//  水印渲染服务 - 使用 CoreImage 将水印渲染到图像上
//

import Foundation
import CoreImage
import ImageIO
import UIKit

#if os(iOS)

/// 水印处理服务 - 使用 CoreImage 实现真实像素级水印渲染
final class WatermarkService {

    static let shared = WatermarkService()

    private let ciContext = CIContext()
    private let renderQueue = DispatchQueue(label: "livecapture.watermark", qos: .userInitiated)

    private init() {}

    // MARK: - Public API

    /// 对 JPEG 数据应用水印，返回新的 JPEG 数据
    func applyWatermark(to imageData: Data, config: WatermarkConfig) -> Data? {
        guard config.isEnabled else { return imageData }
        guard let source = CGImageSourceCreateWithData(imageData as CFData, nil),
              let cgImage = CGImageSourceCreateImageAtIndex(source, 0, nil) else {
            return imageData
        }

        let inputImage = CIImage(cgImage: cgImage)
        let exifData = WatermarkEXIFData.extract(from: imageData)

        guard let watermarked = renderWatermark(on: inputImage, config: config, exif: exifData),
              let outputData = encodeToJPEG(watermarked) else {
            return imageData
        }

        return outputData
    }

    /// 对 CIImage 应用水印
    func applyWatermark(to ciImage: CIImage, config: WatermarkConfig, exif: WatermarkEXIFData = WatermarkEXIFData(iso: nil, shutterSpeed: nil, aperture: nil, focalLength: nil, lensModel: nil)) -> CIImage? {
        guard config.isEnabled else { return ciImage }
        return renderWatermark(on: ciImage, config: config, exif: exif)
    }

    /// 对 UIImage 应用水印
    func applyWatermark(to image: UIImage, config: WatermarkConfig, exif: WatermarkEXIFData = WatermarkEXIFData(iso: nil, shutterSpeed: nil, aperture: nil, focalLength: nil, lensModel: nil)) -> UIImage? {
        guard config.isEnabled else { return image }
        guard let ciImage = CIImage(image: image) else { return image }
        guard let result = renderWatermark(on: ciImage, config: config, exif: exif),
              let cgImage = ciContext.createCGImage(result, from: result.extent) else {
            return image
        }
        return UIImage(cgImage: cgImage, scale: image.scale, orientation: image.imageOrientation)
    }

    /// 生成预览用的小尺寸水印图片
    func generatePreview(config: WatermarkConfig, size: CGSize = CGSize(width: 300, height: 400)) -> UIImage? {
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { ctx in
            // 背景
            UIColor.darkGray.setFill()
            ctx.fill(CGRect(origin: .zero, size: size))

            // 模拟照片内容 - 渐变
            let colors = [UIColor.systemBlue.cgColor, UIColor.systemTeal.cgColor]
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(),
                                          colors: colors as CFArray,
                                          locations: [0, 1]) {
                ctx.cgContext.drawLinearGradient(gradient,
                                                  start: .zero,
                                                  end: CGPoint(x: size.width, y: size.height),
                                                  options: [])
            }
        }

        let sampleExif = WatermarkEXIFData(iso: 400, shutterSpeed: 1/125, aperture: 2.8, focalLength: 26, lensModel: "iPhone 15 Pro back camera")
        return applyWatermark(to: image, config: config, exif: sampleExif)
    }

    // MARK: - Core Rendering

    private func renderWatermark(on inputImage: CIImage, config: WatermarkConfig, exif: WatermarkEXIFData) -> CIImage? {
        let imageRect = inputImage.extent
        let imageWidth = imageRect.width
        let imageHeight = imageRect.height

        // 收集需要渲染的文本行
        var lines: [String] = []
        if !config.text.isEmpty {
            lines.append(config.text)
        }
        if config.showDate {
            let formatter = DateFormatter()
            formatter.dateFormat = config.dateFormat
            lines.append(formatter.string(from: Date()))
        }
        if config.showEXIF {
            let exifText = exif.exifSummary
            if !exifText.isEmpty {
                lines.append(exifText)
            }
        }

        // 如果有 logo 需要渲染，先渲染 logo
        var logoCGImage: CGImage?
        if config.showLogo {
            if let logoData = config.logoImage, let logo = UIImage(data: logoData)?.cgImage {
                logoCGImage = logo
            }
        }

        guard !lines.isEmpty || logoCGImage != nil else { return inputImage }

        // 计算水印整体尺寸
        let font = UIFont.monospacedDigitSystemFont(ofSize: config.fontSize, weight: .medium)
        let lineHeight = font.lineHeight
        let lineSpacing: CGFloat = 4

        let logoSize: CGFloat = config.showLogo ? config.fontSize * 2.5 : 0

        var textWidth: CGFloat = 0
        for line in lines {
            let size = (line as NSString).size(withAttributes: [.font: font])
            textWidth = max(textWidth, size.width)
        }

        let totalTextHeight = CGFloat(lines.count) * lineHeight + CGFloat(max(0, lines.count - 1)) * lineSpacing

        let watermarkWidth: CGFloat
        let watermarkHeight: CGFloat

        if config.showLogo && logoCGImage != nil {
            watermarkWidth = max(textWidth, logoSize) + config.horizontalPadding * 2
            watermarkHeight = logoSize + 8 + totalTextHeight + config.verticalPadding * 2
        } else {
            watermarkWidth = textWidth + config.horizontalPadding * 2
            watermarkHeight = totalTextHeight + config.verticalPadding * 2
        }

        // 计算水印位置
        let origin = calculateOrigin(
            watermarkSize: CGSize(width: watermarkWidth, height: watermarkHeight),
            imageSize: CGSize(width: imageWidth, height: imageHeight),
            position: config.position,
            horizontalPadding: config.horizontalPadding,
            verticalPadding: config.verticalPadding
        )

        // 使用 UIGraphicsImageRenderer 渲染水印文本
        let watermarkScale: CGFloat = 2.0 // 高分辨率渲染
        let renderSize = CGSize(width: watermarkWidth * watermarkScale, height: watermarkHeight * watermarkScale)

        guard let watermarkCGImage = renderWatermarkContent(
            size: renderSize,
            scale: watermarkScale,
            lines: lines,
            font: font,
            lineSpacing: lineSpacing,
            config: config,
            logoCGImage: logoCGImage,
            logoSize: logoSize
        ) else {
            return inputImage
        }

        let watermarkCIImage = CIImage(cgImage: watermarkCGImage)

        // 缩放到实际尺寸
        let scaleFactor = watermarkWidth / watermarkCIImage.extent.width
        let scaledWatermark = watermarkCIImage.transformed(by: CGAffineTransform(scaleX: scaleFactor, y: scaleFactor))

        // 调整透明度
        let alphaFilter = CIFilter(name: "CIColorMatrix")!
        alphaFilter.setValue(scaledWatermark, forKey: kCIInputImageKey)
        alphaFilter.setValue(CIVector(x: 0, y: 0, z: 0, w: CGFloat(config.opacity)), forKey: "inputAVector")

        guard let alphaAdjusted = alphaFilter.outputImage else { return inputImage }

        // 使用 CISourceOverCompositing 合成
        let translated = alphaAdjusted.transformed(by: CGAffineTransform(translationX: origin.x, y: origin.y))

        let composite = CIFilter(name: "CISourceOverCompositing")!
        composite.setValue(translated, forKey: kCIInputImageKey)
        composite.setValue(inputImage, forKey: kCIInputBackgroundImageKey)

        return composite.outputImage
    }

    // MARK: - Private Helpers

    private func renderWatermarkContent(
        size: CGSize,
        scale: CGFloat,
        lines: [String],
        font: UIFont,
        lineSpacing: CGFloat,
        config: WatermarkConfig,
        logoCGImage: CGImage?,
        logoSize: CGFloat
    ) -> CGImage? {
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { ctx in
            let context = ctx.cgContext
            context.setShouldAntialias(true)
            context.setAllowsAntialiasing(true)

            let scaledFontSize = config.fontSize * scale
            let scaledFont = UIFont.monospacedDigitSystemFont(ofSize: scaledFontSize, weight: .medium)
            let scaledLineHeight = scaledFont.lineHeight
            let scaledLineSpacing = lineSpacing * scale
            let scaledHPadding = config.horizontalPadding * scale
            let scaledVPadding = config.verticalPadding * scale
            let scaledLogoSize = logoSize * scale

            let textColor = config.textColor.uiColor.withAlphaComponent(1.0)

            let attributes: [NSAttributedString.Key: Any] = [
                .font: scaledFont,
                .foregroundColor: textColor
            ]

            var yOffset: CGFloat = scaledVPadding

            // 渲染 logo
            if let logo = logoCGImage, config.showLogo {
                let logoRect = CGRect(
                    x: (size.width - scaledLogoSize) / 2,
                    y: yOffset,
                    width: scaledLogoSize,
                    height: scaledLogoSize
                )
                context.draw(logo, in: logoRect)
                yOffset += scaledLogoSize + 8 * scale
            }

            // 渲染文本行
            for line in lines {
                let lineSize = (line as NSString).size(withAttributes: attributes)
                let x = (size.width - lineSize.width) / 2

                (line as NSString).draw(
                    at: CGPoint(x: x, y: yOffset),
                    withAttributes: attributes
                )
                yOffset += scaledLineHeight + scaledLineSpacing
            }
        }

        return image.cgImage
    }

    private func calculateOrigin(
        watermarkSize: CGSize,
        imageSize: CGSize,
        position: WatermarkPosition,
        horizontalPadding: CGFloat,
        verticalPadding: CGFloat
    ) -> CGPoint {
        switch position {
        case .topLeft:
            return CGPoint(x: horizontalPadding, y: verticalPadding)
        case .topRight:
            return CGPoint(x: imageSize.width - watermarkSize.width - horizontalPadding, y: verticalPadding)
        case .bottomLeft:
            return CGPoint(x: horizontalPadding, y: imageSize.height - watermarkSize.height - verticalPadding)
        case .bottomRight:
            return CGPoint(x: imageSize.width - watermarkSize.width - horizontalPadding,
                           y: imageSize.height - watermarkSize.height - verticalPadding)
        case .center:
            return CGPoint(x: (imageSize.width - watermarkSize.width) / 2,
                           y: (imageSize.height - watermarkSize.height) / 2)
        }
    }

    private func encodeToJPEG(_ ciImage: CIImage) -> Data? {
        guard let cgImage = ciContext.createCGImage(ciImage, from: ciImage.extent) else { return nil }
        let uiImage = UIImage(cgImage: cgImage)
        return uiImage.jpegData(compressionQuality: 0.95)
    }
}

#endif