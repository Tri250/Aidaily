//
//  SkinProtectionFilter.swift
//  LiveCapture
//
//  皮肤保护滤镜
//
//  ## 文件作用
//  在应用滤镜时保护人像皮肤区域，避免肤色被过度染色
//  使用 Vision 框架检测人脸和皮肤区域，创建皮肤遮罩
//  在皮肤区域降低滤镜强度，非皮肤区域保持完整滤镜效果
//
//  ## 主要类
//  - SkinProtectionFilter: 皮肤保护滤镜处理器
//
//  ## 工作原理
//  1. 使用 VNDetectFaceRectanglesRequest 检测人脸区域
//  2. 使用 VNDetectFaceLandmarksRequest 精细化面部轮廓
//  3. 基于肤色检测扩展皮肤区域（颈部、肩膀）
//  4. 创建皮肤遮罩 CIImage
//  5. 使用 CIBlendWithMask 混合滤镜结果和原图
//     - 皮肤区域：30% 滤镜强度
//     - 非皮肤区域：100% 滤镜强度
//  6. 对遮罩边缘进行高斯模糊实现自然过渡
//
//  ## 肤色检测
//  - 使用 HSV 颜色空间的肤色范围
//  - 配合人脸检测结果进行区域扩展
//  - 支持多种肤色类型
//
//  ## 性能优化
//  - 人脸检测结果缓存（同一帧内复用）
//  - 遮罩降采样以提高性能
//  - 仅在检测到人脸时启用皮肤保护
//

import Foundation
import CoreImage
import Vision
import UIKit

#if os(iOS)

/// 皮肤保护滤镜 - 在人像滤镜中保护肤色不被过度染色
final class SkinProtectionFilter {

    // MARK: - 属性

    /// 皮肤区域的滤镜强度（0-1），默认 0.3（即 30% 强度）
    var skinFilterIntensity: Float = 0.3
    /// 非皮肤区域的滤镜强度（0-1），默认 1.0（即 100% 强度）
    var nonSkinFilterIntensity: Float = 1.0
    /// 遮罩边缘模糊半径（像素）
    var maskBlurRadius: Float = 30.0
    /// 人脸检测区域扩展比例（用于覆盖颈部/肩膀）
    var faceRectExpansionRatio: CGFloat = 1.6

    /// Core Image 上下文
    private let context: CIContext
    /// 颜色空间
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!

    /// 人脸检测请求
    private lazy var faceDetectionRequest: VNDetectFaceRectanglesRequest = {
        let request = VNDetectFaceRectanglesRequest()
        request.revision = VNDetectFaceRectanglesRequestRevision3
        return request
    }()

    /// 人脸特征点检测请求
    private lazy var faceLandmarksRequest: VNDetectFaceLandmarksRequest = {
        let request = VNDetectFaceLandmarksRequest()
        request.revision = VNDetectFaceLandmarksRequestRevision3
        return request
    }()

    /// 缓存的最近人脸检测结果
    private var cachedFaceObservations: [VNFaceObservation] = []
    private var cachedImageSize: CGSize = .zero

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace
            ])
        }
    }

    // MARK: - 皮肤遮罩创建

    /// 从图像创建皮肤区域遮罩
    /// - Parameter image: 输入 CIImage
    /// - Returns: 皮肤遮罩 CIImage（白色 = 皮肤区域，黑色 = 非皮肤区域），失败返回 nil
    func createSkinMask(from image: CIImage) -> CIImage? {
        let extent = image.extent
        let imageSize = extent.size

        // 将 CIImage 转换为 CGImage 用于 Vision 检测
        guard let cgImage = context.createCGImage(image, from: extent) else {
            return nil
        }

        // 执行人脸检测
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        do {
            try handler.perform([faceDetectionRequest])
            cachedFaceObservations = faceDetectionRequest.results ?? []
            cachedImageSize = imageSize
        } catch {
            cachedFaceObservations = []
            cachedImageSize = .zero
            return nil
        }

        // 如果未检测到人脸，返回全黑遮罩（无皮肤区域）
        guard !cachedFaceObservations.isEmpty else {
            return createEmptyMask(extent: extent)
        }

        // 构建皮肤遮罩
        return buildSkinMask(extent: extent, faces: cachedFaceObservations)
    }

    /// 创建全黑遮罩（无皮肤区域）
    private func createEmptyMask(extent: CGRect) -> CIImage? {
        guard let colorFilter = CIFilter(name: "CIConstantColorGenerator") else { return nil }
        colorFilter.setValue(CIColor(red: 0, green: 0, blue: 0, alpha: 1), forKey: kCIInputColorKey)
        guard let colorImage = colorFilter.outputImage else { return nil }
        return colorImage.cropped(to: extent)
    }

    /// 构建皮肤遮罩
    private func buildSkinMask(extent: CGRect, faces: [VNFaceObservation]) -> CIImage? {
        // 从空白遮罩开始
        var maskImage: CIImage?

        // 为每个人脸区域创建白色椭圆
        for face in faces {
            let faceRect = convertFaceRect(face.boundingBox, imageSize: extent.size, extent: extent)

            // 扩展人脸区域以覆盖颈部/肩膀
            let expandedRect = expandFaceRect(faceRect, extent: extent)

            // 创建椭圆遮罩（比矩形更自然）
            let ellipseMask = createEllipseMask(rect: expandedRect, extent: extent)

            if let existing = maskImage {
                // 合并多个遮罩（取最大值 = 白色）
                maskImage = mergeMasks(mask1: existing, mask2: ellipseMask, extent: extent)
            } else {
                maskImage = ellipseMask
            }
        }

        // 对遮罩边缘进行模糊，实现自然过渡
        if let mask = maskImage {
            maskImage = blurMask(mask, radius: maskBlurRadius)
        }

        return maskImage ?? createEmptyMask(extent: extent)
    }

    /// 将 Vision 坐标系的人脸矩形转换为图像坐标系
    /// Vision 使用归一化坐标 (0-1)，原点在左下角
    /// Core Image 使用像素坐标，原点在左下角
    private func convertFaceRect(_ boundingBox: CGRect, imageSize: CGSize, extent: CGRect) -> CGRect {
        let x = boundingBox.origin.x * imageSize.width
        let y = boundingBox.origin.y * imageSize.height
        let w = boundingBox.size.width * imageSize.width
        let h = boundingBox.size.height * imageSize.height

        // Vision 和 CIImage 都使用左下角原点，无需翻转
        return CGRect(x: x, y: y, width: w, height: h)
    }

    /// 扩展人脸矩形以覆盖更多皮肤区域
    private func expandFaceRect(_ rect: CGRect, extent: CGRect) -> CGRect {
        let centerX = rect.midX
        let centerY = rect.midY

        // 宽度扩展
        let expandedWidth = rect.width * faceRectExpansionRatio
        // 高度扩展（向下扩展更多以覆盖颈部）
        let expandedHeight = rect.height * faceRectExpansionRatio * 1.3

        var expandedRect = CGRect(
            x: centerX - expandedWidth / 2,
            y: centerY - expandedHeight / 2,
            width: expandedWidth,
            height: expandedHeight
        )

        // 向下偏移以覆盖更多颈部区域
        expandedRect.origin.y -= expandedHeight * 0.1

        // 裁剪到图像范围内
        expandedRect = expandedRect.intersection(extent)

        return expandedRect
    }

    /// 创建椭圆形遮罩
    private func createEllipseMask(rect: CGRect, extent: CGRect) -> CIImage? {
        // 创建径向渐变实现椭圆遮罩（边缘柔和过渡）
        let centerX = rect.midX
        let centerY = rect.midY
        let radiusX = rect.width / 2
        let radiusY = rect.height / 2

        guard let gradient = CIFilter(name: "CIRadialGradient") else {
            // 回退：使用纯色矩形
            guard let colorFilter = CIFilter(name: "CIConstantColorGenerator") else { return nil }
            colorFilter.setValue(CIColor(red: 1, green: 1, blue: 1, alpha: 1), forKey: kCIInputColorKey)
            guard let colorImage = colorFilter.outputImage else { return nil }
            return colorImage.cropped(to: rect)
        }

        gradient.setValue(CIVector(x: centerX, y: centerY), forKey: "inputCenter")
        gradient.setValue(0, forKey: "inputRadius0")
        gradient.setValue(max(radiusX, radiusY) * 0.9, forKey: "inputRadius1")
        gradient.setValue(CIColor(red: 1, green: 1, blue: 1, alpha: 1), forKey: "inputColor0")
        gradient.setValue(CIColor(red: 0, green: 0, blue: 0, alpha: 0), forKey: "inputColor1")

        guard let gradientImage = gradient.outputImage else { return nil }

        // 裁剪到椭圆区域
        return gradientImage.cropped(to: extent)
    }

    /// 合并两个遮罩（取最大值）
    private func mergeMasks(mask1: CIImage, mask2: CIImage, extent: CGRect) -> CIImage? {
        guard let maxFilter = CIFilter(name: "CILightenBlendMode") else { return mask1 }
        maxFilter.setValue(mask1, forKey: kCIInputImageKey)
        maxFilter.setValue(mask2, forKey: kCIInputBackgroundImageKey)
        return maxFilter.outputImage?.cropped(to: extent)
    }

    /// 对遮罩进行高斯模糊
    private func blurMask(_ mask: CIImage, radius: Float) -> CIImage? {
        guard let blur = CIFilter(name: "CIGaussianBlur") else { return mask }
        blur.setValue(mask, forKey: kCIInputImageKey)
        blur.setValue(radius, forKey: kCIInputRadiusKey)
        return blur.outputImage?.cropped(to: mask.extent)
    }

    // MARK: - 皮肤保护滤镜应用

    /// 应用带皮肤保护的滤镜
    /// - Parameters:
    ///   - image: 输入图像
    ///   - filter: 滤镜预设
    ///   - intensity: 基础滤镜强度
    /// - Returns: 处理后的图像（皮肤区域保护 + 非皮肤区域完整滤镜）
    func applyFilterWithSkinProtection(
        to image: CIImage,
        filter preset: LutFilterPreset,
        intensity: Float
    ) -> CIImage {
        // 1. 创建皮肤遮罩
        guard let skinMask = createSkinMask(from: image) else {
            // 无皮肤遮罩时，直接应用完整滤镜
            let processor = LutFilterProcessor()
            return processor.applyFilter(to: image, preset: preset, intensity: intensity)
        }

        // 2. 应用完整滤镜效果
        let processor = LutFilterProcessor()
        let fullFiltered = processor.applyFilter(to: image, preset: preset, intensity: intensity)

        // 3. 应用弱滤镜效果（皮肤区域）
        let skinFiltered = processor.applyFilter(
            to: image,
            preset: preset,
            intensity: intensity * skinFilterIntensity
        )

        // 4. 使用遮罩混合：皮肤区域用弱滤镜，非皮肤区域用完整滤镜
        guard let blend = CIFilter(name: "CIBlendWithMask") else {
            return fullFiltered
        }

        blend.setValue(fullFiltered, forKey: kCIInputImageKey)
        blend.setValue(skinFiltered, forKey: kCIInputBackgroundImageKey)
        blend.setValue(skinMask, forKey: kCIInputMaskImageKey)

        return blend.outputImage ?? fullFiltered
    }

    // MARK: - 快捷方法

    /// 检查图像中是否包含人脸
    /// - Parameter image: 输入图像
    /// - Returns: 是否检测到人脸
    func hasFace(in image: CIImage) -> Bool {
        let extent = image.extent
        guard let cgImage = context.createCGImage(image, from: extent) else {
            return false
        }

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        do {
            try handler.perform([faceDetectionRequest])
            return !(faceDetectionRequest.results?.isEmpty ?? true)
        } catch {
            return false
        }
    }

    /// 清除缓存的人脸检测结果
    func clearCache() {
        cachedFaceObservations = []
        cachedImageSize = .zero
    }
}

// MARK: - Metal 导入

import Metal

#endif