//
//  PortraitEffectEngine.swift
//  LiveCapture
//
//  人像效果引擎
//
//  ## 文件作用
//  实现人像模式的核心效果处理：人像检测、背景虚化（Bokeh）、
//  人像光效（Portrait Lighting）。使用 Vision 框架进行人脸检测，
//  Core Image 进行图像处理。
//
//  ## 主要类
//  - PortraitEffectEngine: 人像效果引擎
//
//  ## 核心功能
//  - detectPortrait(in:): 使用 Vision 检测人脸区域和关键点
//  - applyBokeh(to:params:depthData:faceObservations:): 背景虚化
//  - applyLighting(to:type:faceObservations:): 人像光效
//
//  ## 技术栈
//  - Vision: VNDetectFaceRectanglesRequest, VNDetectFaceLandmarksRequest
//  - CoreImage: CIFilter 链式处理
//

import Foundation
import CoreImage
import Vision
import AVFoundation

#if os(iOS)

/// 人像效果引擎
final class PortraitEffectEngine {
    private let context = CIContext(
        options: [
            .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB(),
            .highQualityDownsample: true
        ]
    )
    private let queue = DispatchQueue(label: "livecapture.portrait.engine")
    private let visionQueue = DispatchQueue(label: "livecapture.portrait.vision")

    // MARK: - Portrait Detection

    /// 检测图像中的人像——返回人脸区域、关键点、深度数据（如可用）
    /// - Parameter image: 输入 CIImage
    /// - Returns: PortraitResult 包含检测结果
    func detectPortrait(in image: CIImage) -> PortraitResult {
        let extent = image.extent
        guard extent.width > 0, extent.height > 0 else {
            return PortraitResult(originalImage: image, hasPortrait: false)
        }

        let request = VNDetectFaceRectanglesRequest()
        let landmarkRequest = VNDetectFaceLandmarksRequest()

        let handler = VNImageRequestHandler(ciImage: image, options: [:])
        do {
            try handler.perform([request, landmarkRequest])
        } catch {
            LiveCaptureLogger.shared.error("PortraitEffectEngine error: \(error)")
            return PortraitResult(originalImage: image, hasPortrait: false)
        }

        guard let faceObservations = request.results, !faceObservations.isEmpty else {
            return PortraitResult(originalImage: image, hasPortrait: false)
        }

        // 提取面部关键点
        var landmarks: [CGPoint] = []
        if let landmarkResults = landmarkRequest.results {
            for face in landmarkResults {
                if let faceLandmarks = face.landmarks {
                    landmarks.append(contentsOf: extractLandmarkPoints(
                        from: faceLandmarks,
                        faceRect: face.boundingBox,
                        imageExtent: extent
                    ))
                }
            }
        }

        // 创建皮肤掩码
        let skinMask = createSkinMask(from: image, faces: faceObservations, imageExtent: extent)

        return PortraitResult(
            originalImage: image,
            depthData: nil,
            skinMask: skinMask,
            faceLandmarks: landmarks,
            hasPortrait: true
        )
    }

    // MARK: - Bokeh (Background Blur)

    /// 应用背景虚化效果
    /// - Parameters:
    ///   - image: 输入图像
    ///   - params: 虚化参数
    ///   - depthData: 深度数据（可选）
    ///   - faceObservations: 人脸检测结果
    /// - Returns: 虚化后的图像
    func applyBokeh(
        to image: CIImage,
        params: BokehParams,
        depthData: CIImage?,
        faceObservations: [VNFaceObservation]
    ) -> CIImage {
        let extent = image.extent

        // 1. 创建主体掩码（人脸区域 = 前景）
        let foregroundMask = createForegroundMask(
            from: faceObservations,
            imageExtent: extent
        )

        // 2. 创建模糊背景
        let blurRadius = params.blurRadius
        let blurredImage = image
            .clampedToExtent()
            .applyingGaussianBlur(sigma: Double(blurRadius))
            .cropped(to: extent)

        // 3. 使用掩码合成：前景清晰 + 背景模糊
        let compositeFilter = CIFilter.blendWithMask()
        compositeFilter.inputImage = blurredImage
        compositeFilter.backgroundImage = image
        compositeFilter.maskImage = foregroundMask

        guard var result = compositeFilter.outputImage else { return image }

        // 4. 应用光斑形状效果（通过调整高光部分的模糊特征）
        if params.bokehShape != .circle {
            result = applyBokehShape(to: result, shape: params.bokehShape, extent: extent)
        }

        return result
    }

    // MARK: - Portrait Lighting

    /// 应用人像光效
    /// - Parameters:
    ///   - image: 输入图像
    ///   - type: 光效类型
    ///   - faceObservations: 人脸检测结果
    /// - Returns: 应用光效后的图像
    func applyLighting(
        to image: CIImage,
        type: PortraitLightingType,
        faceObservations: [VNFaceObservation]
    ) -> CIImage {
        let extent = image.extent
        guard let firstFace = faceObservations.first else { return image }

        // 将归一化人脸框转换到图像坐标
        let faceRect = VNImageRectForNormalizedRect(
            firstFace.boundingBox,
            Int(extent.width),
            Int(extent.height)
        )

        switch type {
        case .natural:
            return image

        case .studioLight:
            return applyStudioLight(to: image, faceRect: faceRect, extent: extent)

        case .contourLight:
            return applyContourLight(to: image, faceRect: faceRect, extent: extent)

        case .stageLight:
            return applyStageLight(to: image, faceRect: faceRect, extent: extent, mono: false)

        case .stageLightMono:
            return applyStageLight(to: image, faceRect: faceRect, extent: extent, mono: true)
        }
    }

    // MARK: - Private: Lighting Implementations

    /// 摄影室灯光：提亮面部中心，轻微压暗边缘
    private func applyStudioLight(
        to image: CIImage,
        faceRect: CGRect,
        extent: CGRect
    ) -> CIImage {
        // 创建从面部中心向外渐变的光照掩码
        let faceCenter = CIVector(x: faceRect.midX, y: faceRect.midY)
        let faceRadius = Float(max(faceRect.width, faceRect.height)) * 0.8

        let radialGradient = CIFilter.radialGradient()
        radialGradient.center = faceCenter
        radialGradient.radius0 = faceRadius * 0.3
        radialGradient.radius1 = faceRadius
        radialGradient.color0 = CIColor(red: 1, green: 1, blue: 1, alpha: 1)
        radialGradient.color1 = CIColor(red: 0.65, green: 0.65, blue: 0.65, alpha: 1)

        guard let gradientMask = radialGradient.outputImage?.cropped(to: extent) else {
            return image
        }

        // 提亮版本
        let brightened = image
            .applyingFilter("CIExposureAdjust", parameters: [kCIInputEVKey: 0.3])
            .applyingFilter("CIHighlightShadowAdjust", parameters: [
                "inputHighlightAmount": 1.1,
                "inputShadowAmount": 0.9
            ])

        // 用渐变掩码混合
        let blend = CIFilter.blendWithMask()
        blend.inputImage = brightened
        blend.backgroundImage = image
        blend.maskImage = gradientMask

        return blend.outputImage ?? image
    }

    /// 轮廓光：在面部一侧添加戏剧性阴影
    private func applyContourLight(
        to image: CIImage,
        faceRect: CGRect,
        extent: CGRect
    ) -> CIImage {
        // 创建从面部左侧到右侧的线性渐变
        let faceCenter = CIVector(x: faceRect.midX, y: faceRect.midY)
        let leftOfFace = CIVector(x: faceRect.minX - faceRect.width * 0.5, y: faceRect.midY)
        let rightOfFace = CIVector(x: faceRect.maxX + faceRect.width * 0.5, y: faceRect.midY)

        let linearGradient = CIFilter.linearGradient()
        linearGradient.point0 = leftOfFace
        linearGradient.point1 = rightOfFace
        linearGradient.color0 = CIColor(red: 0.3, green: 0.3, blue: 0.3, alpha: 1)
        linearGradient.color1 = CIColor(red: 1, green: 1, blue: 1, alpha: 1)

        guard let gradientMask = linearGradient.outputImage?.cropped(to: extent) else {
            return image
        }

        // 阴影侧
        let shadowed = image
            .applyingFilter("CIExposureAdjust", parameters: [kCIInputEVKey: -0.4])
            .applyingFilter("CIHighlightShadowAdjust", parameters: [
                "inputHighlightAmount": 0.7,
                "inputShadowAmount": 1.3
            ])

        let blend = CIFilter.blendWithMask()
        blend.inputImage = shadowed
        blend.backgroundImage = image
        blend.maskImage = gradientMask

        // 增加整体对比度
        let result = blend.outputImage ?? image
        return result.applyingFilter("CIColorControls", parameters: [
            kCIInputContrastKey: 1.15,
            kCIInputSaturationKey: 0.95
        ])
    }

    /// 舞台光：聚光灯打在面部，背景变暗+模糊
    private func applyStageLight(
        to image: CIImage,
        faceRect: CGRect,
        extent: CGRect,
        mono: Bool
    ) -> CIImage {
        // 创建聚光灯掩码（从面部中心放射）
        let faceCenter = CIVector(x: faceRect.midX, y: faceRect.midY)
        let spotlightRadius = Float(max(faceRect.width, faceRect.height)) * 1.2

        let radialGradient = CIFilter.radialGradient()
        radialGradient.center = faceCenter
        radialGradient.radius0 = spotlightRadius * 0.15
        radialGradient.radius1 = spotlightRadius
        radialGradient.color0 = CIColor(red: 1, green: 1, blue: 1, alpha: 1)
        radialGradient.color1 = CIColor(red: 0, green: 0, blue: 0, alpha: 1)

        guard let spotlightMask = radialGradient.outputImage?.cropped(to: extent) else {
            return image
        }

        // 亮点版本（面部区域）
        let brightened = image
            .applyingFilter("CIExposureAdjust", parameters: [kCIInputEVKey: 0.5])
            .applyingFilter("CIHighlightShadowAdjust", parameters: [
                "inputHighlightAmount": 1.2,
                "inputShadowAmount": 0.8
            ])

        // 暗化背景版本
        let darkened = image
            .applyingGaussianBlur(sigma: 8.0)
            .applyingFilter("CIExposureAdjust", parameters: [kCIInputEVKey: -1.5])
            .applyingFilter("CIColorControls", parameters: [
                kCIInputContrastKey: 0.8,
                kCIInputSaturationKey: 0.3
            ])

        let blend = CIFilter.blendWithMask()
        blend.inputImage = brightened
        blend.backgroundImage = darkened
        blend.maskImage = spotlightMask

        var result = blend.outputImage ?? image

        // 黑白模式
        if mono {
            let monoFilter = CIFilter.colorControls()
            monoFilter.inputImage = result
            monoFilter.saturation = 0.0
            monoFilter.contrast = 1.1
            result = monoFilter.outputImage ?? result
        }

        return result
    }

    // MARK: - Private: Mask Generation

    /// 创建前景掩码（人脸区域为白色=前景，其余为黑色=背景）
    private func createForegroundMask(
        from faces: [VNFaceObservation],
        imageExtent: CGRect
    ) -> CIImage {
        let renderer = UIGraphicsImageRenderer(size: imageExtent.size)
        let uiImage = renderer.image { ctx in
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: imageExtent.size))

            UIColor.white.setFill()
            for face in faces {
                let rect = VNImageRectForNormalizedRect(
                    face.boundingBox,
                    Int(imageExtent.width),
                    Int(imageExtent.height)
                )
                // 使用椭圆拟合面部区域
                let expandedRect = rect.insetBy(
                    dx: -rect.width * 0.25,
                    dy: -rect.height * 0.35
                )
                let path = UIBezierPath(ovalIn: expandedRect)
                path.fill()
            }
        }

        guard let cgImage = uiImage.cgImage else {
            return CIImage(color: CIColor(red: 0, green: 0, blue: 0))
        }

        var maskImage = CIImage(cgImage: cgImage)

        // 羽化边缘，使过渡自然
        maskImage = maskImage
            .applyingGaussianBlur(sigma: 15.0)

        return maskImage
    }

    /// 创建皮肤区域掩码
    private func createSkinMask(
        from image: CIImage,
        faces: [VNFaceObservation],
        imageExtent: CGRect
    ) -> CIImage {
        // 使用与前景掩码相同的逻辑，但更精细
        // 这里使用基于人脸检测框的初始掩码
        let renderer = UIGraphicsImageRenderer(size: imageExtent.size)
        let uiImage = renderer.image { ctx in
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: imageExtent.size))

            UIColor.white.setFill()
            for face in faces {
                let rect = VNImageRectForNormalizedRect(
                    face.boundingBox,
                    Int(imageExtent.width),
                    Int(imageExtent.height)
                )
                // 扩展到颈部/肩部
                let expandedRect = CGRect(
                    x: rect.origin.x - rect.width * 0.15,
                    y: rect.origin.y - rect.height * 0.1,
                    width: rect.width * 1.3,
                    height: rect.height * 1.6
                )
                let path = UIBezierPath(ovalIn: expandedRect)
                path.fill()
            }
        }

        guard let cgImage = uiImage.cgImage else {
            return CIImage(color: CIColor(red: 0, green: 0, blue: 0))
        }

        return CIImage(cgImage: cgImage)
            .applyingGaussianBlur(sigma: 10.0)
    }

    /// 提取面部关键点坐标
    private func extractLandmarkPoints(
        from landmarks: VNFaceLandmarks2D,
        faceRect: CGRect,
        imageExtent: CGRect
    ) -> [CGPoint] {
        var points: [CGPoint] = []

        let facePixelRect = VNImageRectForNormalizedRect(
            faceRect,
            Int(imageExtent.width),
            Int(imageExtent.height)
        )

        let allLandmarks: [(VNFaceLandmarkRegion2D?, String)] = [
            (landmarks.leftEye, "leftEye"),
            (landmarks.rightEye, "rightEye"),
            (landmarks.outerLips, "outerLips"),
            (landmarks.innerLips, "innerLips"),
            (landmarks.nose, "nose"),
            (landmarks.faceContour, "faceContour"),
            (landmarks.noseCrest, "noseCrest"),
            (landmarks.medianLine, "medianLine")
        ]

        for (region, _) in allLandmarks {
            guard let region = region else { continue }
            let normalizedPoints = region.normalizedPoints
            for point in normalizedPoints {
                let pixelX = facePixelRect.origin.x + point.x * facePixelRect.width
                let pixelY = facePixelRect.origin.y + (1.0 - point.y) * facePixelRect.height
                points.append(CGPoint(x: pixelX, y: pixelY))
            }
        }

        return points
    }

    /// 应用光斑形状效果
    private func applyBokehShape(
        to image: CIImage,
        shape: BokehParams.BokehShape,
        extent: CGRect
    ) -> CIImage {
        // 通过调整高光阈值和锐化来模拟不同光斑形状
        // 实际应用中可以通过自定义 CIKernel 实现精确形状
        var result = image

        switch shape {
        case .hexagon:
            // 六边形光斑：增加高光 + 特定方向锐化
            result = result.applyingFilter("CIHighlightShadowAdjust", parameters: [
                "inputHighlightAmount": 1.3,
                "inputShadowAmount": 0.7
            ])
        case .heart:
            // 心形光斑：暖色调高光
            result = result.applyingFilter("CIHighlightShadowAdjust", parameters: [
                "inputHighlightAmount": 1.25,
                "inputShadowAmount": 0.75
            ])
            result = result.applyingFilter("CITemperatureAndTint", parameters: [
                "inputNeutral": CIVector(x: 7000, y: 0),
                "inputTargetNeutral": CIVector(x: 5500, y: 10)
            ])
        case .star:
            // 星形光斑：高对比度 + 冷色调
            result = result.applyingFilter("CIHighlightShadowAdjust", parameters: [
                "inputHighlightAmount": 1.4,
                "inputShadowAmount": 0.6
            ])
            result = result.applyingFilter("CITemperatureAndTint", parameters: [
                "inputNeutral": CIVector(x: 6500, y: 0),
                "inputTargetNeutral": CIVector(x: 7500, y: -5)
            ])
        case .circle:
            break // 默认圆形
        }

        return result
    }
}

#endif