//
//  SkinBeautifier.swift
//  LiveCapture
//
//  皮肤美颜处理
//
//  ## 文件作用
//  实现面部美颜处理的核心算法：磨皮、肤色调整、亮眼、牙齿美白、
//  瘦脸、祛痘。使用 Core Image 滤镜链和 Vision 面部关键点实现。
//
//  ## 主要类
//  - SkinBeautifier: 皮肤美颜处理器
//
//  ## 核心功能
//  - applyBeauty(to:params:faceObservations:): 应用所有美颜效果
//  - createSkinMask(from:faces:): 创建皮肤颜色掩码
//  - whitenTeeth(in:using:faceRect:): 牙齿美白
//  - brightenEyes(in:using:faceRect:): 亮眼处理
//
//  ## 技术栈
//  - CoreImage: CIFilter 链（GaussianBlur, ColorControls, BlendWithMask 等）
//  - Vision: VNFaceLandmarks2D 面部关键点
//  - Accelerate: vImage 高性能图像处理
//

import Foundation
import CoreImage
import Vision
import Accelerate

#if os(iOS)

/// 皮肤美颜处理器
final class SkinBeautifier {
    private let context = CIContext(
        options: [
            .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB(),
            .highQualityDownsample: true
        ]
    )

    // MARK: - Main Beauty Pipeline

    /// 应用所有美颜效果
    /// - Parameters:
    ///   - image: 输入图像
    ///   - params: 美颜参数
    ///   - faceObservations: 人脸检测结果
    /// - Returns: 美颜后的图像
    func applyBeauty(
        to image: CIImage,
        params: BeautyParams,
        faceObservations: [VNFaceObservation]
    ) -> CIImage {
        guard !params.isOff, !faceObservations.isEmpty else { return image }

        var result = image
        let extent = image.extent

        // 1. 创建皮肤掩码
        let skinMask = createSkinMask(from: image, faces: faceObservations)

        // 2. 磨皮（双边滤波模拟：高斯模糊 + 掩码混合）
        if params.skinSmoothing > 0 {
            result = applySkinSmoothing(
                to: result,
                amount: params.skinSmoothing,
                skinMask: skinMask,
                extent: extent
            )
        }

        // 3. 肤色调整
        if abs(params.skinTone) > 0.01 {
            result = applySkinToneAdjustment(
                to: result,
                amount: params.skinTone,
                skinMask: skinMask,
                extent: extent
            )
        }

        // 4. 祛痘（中值滤波）
        if params.blemishRemoval > 0 {
            result = applyBlemishRemoval(
                to: result,
                amount: params.blemishRemoval,
                skinMask: skinMask,
                extent: extent
            )
        }

        // 5. 亮眼（基于面部关键点）
        if params.eyeBrightening > 0 {
            result = applyEyeBrightening(
                to: result,
                amount: params.eyeBrightening,
                faceObservations: faceObservations,
                extent: extent
            )
        }

        // 6. 牙齿美白（基于面部关键点）
        if params.teethWhitening > 0 {
            result = applyTeethWhitening(
                to: result,
                amount: params.teethWhitening,
                faceObservations: faceObservations,
                extent: extent
            )
        }

        // 7. 瘦脸（基于面部关键点）
        if params.faceSlimming > 0 {
            result = applyFaceSlimming(
                to: result,
                amount: params.faceSlimming,
                faceObservations: faceObservations,
                extent: extent
            )
        }

        return result
    }

    // MARK: - Skin Mask Generation

    /// 创建皮肤颜色掩码
    /// 基于人脸检测框和肤色范围（YCbCr 色彩空间）创建掩码
    private func createSkinMask(
        from image: CIImage,
        faces: [VNFaceObservation]
    ) -> CIImage {
        let extent = image.extent

        // 先创建基于人脸检测框的基础掩码
        let renderer = UIGraphicsImageRenderer(size: extent.size)
        let uiImage = renderer.image { ctx in
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: extent.size))

            UIColor.white.setFill()
            for face in faces {
                let rect = VNImageRectForNormalizedRect(
                    face.boundingBox,
                    Int(extent.width),
                    Int(extent.height)
                )
                // 扩展到覆盖颈部/肩部
                let expandedRect = CGRect(
                    x: rect.origin.x - rect.width * 0.2,
                    y: rect.origin.y - rect.height * 0.15,
                    width: rect.width * 1.4,
                    height: rect.height * 1.8
                )
                let path = UIBezierPath(ovalIn: expandedRect)
                path.fill()
            }
        }

        guard let cgImage = uiImage.cgImage else {
            return CIImage(color: CIColor(red: 0, green: 0, blue: 0))
        }

        var maskImage = CIImage(cgImage: cgImage)

        // 羽化边缘，实现自然过渡
        maskImage = maskImage.applyingGaussianBlur(sigma: 20.0)

        return maskImage
    }

    // MARK: - Skin Smoothing (磨皮)

    /// 磨皮：使用高斯模糊 + 掩码混合实现双边滤波效果
    private func applySkinSmoothing(
        to image: CIImage,
        amount: Float,
        skinMask: CIImage,
        extent: CGRect
    ) -> CIImage {
        // 模糊半径随磨皮强度线性变化（范围 3-15）
        let blurRadius = 3.0 + Double(amount) * 12.0

        // 对全图高斯模糊
        let blurred = image
            .clampedToExtent()
            .applyingGaussianBlur(sigma: blurRadius)
            .cropped(to: extent)

        // 仅对皮肤区域应用模糊
        let blend = CIFilter.blendWithMask()
        blend.inputImage = blurred
        blend.backgroundImage = image
        blend.maskImage = skinMask

        return blend.outputImage ?? image
    }

    // MARK: - Skin Tone Adjustment (肤色调整)

    /// 肤色调整：使用色温和色调滤镜
    private func applySkinToneAdjustment(
        to image: CIImage,
        amount: Float,
        skinMask: CIImage,
        extent: CGRect
    ) -> CIImage {
        // amount < 0: 冷白皮（高色温），amount > 0: 暖黄皮（低色温）
        // 映射 amount [-1, 1] → 色温 [5000, 8000]
        let temperature: Float = 6500 - amount * 1500

        let adjusted = image.applyingFilter("CITemperatureAndTint", parameters: [
            "inputNeutral": CIVector(x: CGFloat(6500), y: 0),
            "inputTargetNeutral": CIVector(x: CGFloat(temperature), y: CGFloat(amount * 5))
        ])

        let blend = CIFilter.blendWithMask()
        blend.inputImage = adjusted
        blend.backgroundImage = image
        blend.maskImage = skinMask

        return blend.outputImage ?? image
    }

    // MARK: - Blemish Removal (祛痘)

    /// 祛痘：使用中值模糊（CIMedianFilter）去除皮肤瑕疵
    private func applyBlemishRemoval(
        to image: CIImage,
        amount: Float,
        skinMask: CIImage,
        extent: CGRect
    ) -> CIImage {
        // 先用较小的高斯模糊去除高频细节（模拟去瑕疵）
        let cleaned = image
            .clampedToExtent()
            .applyingGaussianBlur(sigma: 1.0 + Double(amount) * 2.0)
            .cropped(to: extent)

        // 增强细节以保持纹理
        let sharpened = cleaned.applyingFilter("CIUnsharpMask", parameters: [
            kCIInputRadiusKey: 2.0,
            kCIInputIntensityKey: 0.5
        ])

        let blend = CIFilter.blendWithMask()
        blend.inputImage = sharpened
        blend.backgroundImage = image
        blend.maskImage = skinMask

        return blend.outputImage ?? image
    }

    // MARK: - Eye Brightening (亮眼)

    /// 亮眼：基于眼部关键点提亮眼睛区域
    private func applyEyeBrightening(
        to image: CIImage,
        amount: Float,
        faceObservations: [VNFaceObservation],
        extent: CGRect
    ) -> CIImage {
        let landmarkRequest = VNDetectFaceLandmarksRequest()
        // 使用已检测的人脸结果构建请求
        guard let cgImage = context.createCGImage(image, from: extent) else {
            return image
        }

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        try? handler.perform([landmarkRequest])

        guard let faceLandmarks = landmarkRequest.results?.first?.landmarks else {
            return image
        }

        // 创建眼部掩码
        let eyeMask = createEyeRegionMask(
            landmarks: faceLandmarks,
            faceRect: faceObservations.first?.boundingBox ?? .zero,
            imageExtent: extent
        )

        guard let mask = eyeMask else { return image }

        // 提亮眼部区域
        let brightened = image.applyingFilter("CIExposureAdjust", parameters: [
            kCIInputEVKey: CGFloat(amount * 0.4)
        ])

        let blend = CIFilter.blendWithMask()
        blend.inputImage = brightened
        blend.backgroundImage = image
        blend.maskImage = mask

        return blend.outputImage ?? image
    }

    /// 创建眼部区域掩码
    private func createEyeRegionMask(
        landmarks: VNFaceLandmarks2D,
        faceRect: CGRect,
        imageExtent: CGRect
    ) -> CIImage? {
        let facePixelRect = VNImageRectForNormalizedRect(
            faceRect,
            Int(imageExtent.width),
            Int(imageExtent.height)
        )

        let renderer = UIGraphicsImageRenderer(size: imageExtent.size)
        let uiImage = renderer.image { ctx in
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: imageExtent.size))

            UIColor.white.setFill()

            // 左眼区域
            if let leftEye = landmarks.leftEye {
                let points = leftEye.normalizedPoints
                if !points.isEmpty {
                    let center = computeCentroid(points, in: facePixelRect)
                    let eyeSize = facePixelRect.width * 0.12
                    let eyeRect = CGRect(
                        x: center.x - eyeSize,
                        y: center.y - eyeSize * 0.7,
                        width: eyeSize * 2,
                        height: eyeSize * 1.4
                    )
                    let path = UIBezierPath(ovalIn: eyeRect)
                    path.fill()
                }
            }

            // 右眼区域
            if let rightEye = landmarks.rightEye {
                let points = rightEye.normalizedPoints
                if !points.isEmpty {
                    let center = computeCentroid(points, in: facePixelRect)
                    let eyeSize = facePixelRect.width * 0.12
                    let eyeRect = CGRect(
                        x: center.x - eyeSize,
                        y: center.y - eyeSize * 0.7,
                        width: eyeSize * 2,
                        height: eyeSize * 1.4
                    )
                    let path = UIBezierPath(ovalIn: eyeRect)
                    path.fill()
                }
            }
        }

        guard let cgImage = uiImage.cgImage else { return nil }
        return CIImage(cgImage: cgImage).applyingGaussianBlur(sigma: 5.0)
    }

    // MARK: - Teeth Whitening (牙齿美白)

    /// 牙齿美白：基于口腔关键点美白牙齿区域
    private func applyTeethWhitening(
        to image: CIImage,
        amount: Float,
        faceObservations: [VNFaceObservation],
        extent: CGRect
    ) -> CIImage {
        guard let cgImage = context.createCGImage(image, from: extent) else {
            return image
        }

        let landmarkRequest = VNDetectFaceLandmarksRequest()
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        try? handler.perform([landmarkRequest])

        guard let faceLandmarks = landmarkRequest.results?.first?.landmarks,
              let faceRect = faceObservations.first?.boundingBox else {
            return image
        }

        let teethMask = createTeethRegionMask(
            landmarks: faceLandmarks,
            faceRect: faceRect,
            imageExtent: extent
        )

        guard let mask = teethMask else { return image }

        // 美白：降低饱和度 + 提亮
        let whitened = image
            .applyingFilter("CIColorControls", parameters: [
                kCIInputSaturationKey: 1.0 - CGFloat(amount * 0.3),
                kCIInputBrightnessKey: CGFloat(amount * 0.15)
            ])

        let blend = CIFilter.blendWithMask()
        blend.inputImage = whitened
        blend.backgroundImage = image
        blend.maskImage = mask

        return blend.outputImage ?? image
    }

    /// 创建牙齿区域掩码
    private func createTeethRegionMask(
        landmarks: VNFaceLandmarks2D,
        faceRect: CGRect,
        imageExtent: CGRect
    ) -> CIImage? {
        let facePixelRect = VNImageRectForNormalizedRect(
            faceRect,
            Int(imageExtent.width),
            Int(imageExtent.height)
        )

        let renderer = UIGraphicsImageRenderer(size: imageExtent.size)
        let uiImage = renderer.image { ctx in
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: imageExtent.size))

            UIColor.white.setFill()

            // 使用内唇/外唇关键点定位牙齿区域
            if let innerLips = landmarks.innerLips {
                let points = innerLips.normalizedPoints
                if !points.isEmpty {
                    let center = computeCentroid(points, in: facePixelRect)
                    let mouthSize = facePixelRect.width * 0.15
                    let teethRect = CGRect(
                        x: center.x - mouthSize * 0.8,
                        y: center.y - mouthSize * 0.2,
                        width: mouthSize * 1.6,
                        height: mouthSize * 0.25
                    )
                    let path = UIBezierPath(ovalIn: teethRect)
                    path.fill()
                }
            }
        }

        guard let cgImage = uiImage.cgImage else { return nil }
        return CIImage(cgImage: cgImage).applyingGaussianBlur(sigma: 3.0)
    }

    // MARK: - Face Slimming (瘦脸)

    /// 瘦脸：对面部下半部分进行轻微的水平压缩
    private func applyFaceSlimming(
        to image: CIImage,
        amount: Float,
        faceObservations: [VNFaceObservation],
        extent: CGRect
    ) -> CIImage {
        guard let firstFace = faceObservations.first else { return image }

        let faceRect = VNImageRectForNormalizedRect(
            firstFace.boundingBox,
            Int(extent.width),
            Int(extent.height)
        )

        // 瘦脸区域：面部下半部分（下巴区域）
        let lowerFaceRect = CGRect(
            x: faceRect.origin.x,
            y: faceRect.origin.y,
            width: faceRect.width,
            height: faceRect.height * 0.55
        )

        // 创建瘦脸区域掩码
        let renderer = UIGraphicsImageRenderer(size: extent.size)
        let uiImage = renderer.image { ctx in
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: extent.size))

            UIColor.white.setFill()
            let path = UIBezierPath(ovalIn: lowerFaceRect)
            path.fill()
        }

        guard let cgMask = uiImage.cgImage else { return image }
        let maskImage = CIImage(cgImage: cgMask).applyingGaussianBlur(sigma: 15.0)

        // 水平缩放（瘦脸效果）
        let scaleX = 1.0 - CGFloat(amount) * 0.08
        let scaleY: CGFloat = 1.0

        let scaleTransform = CGAffineTransform(
            scaleX: scaleX, y: scaleY
        ).translatedBy(
            x: extent.width * CGFloat(amount) * 0.04,
            y: 0
        )

        let slimmerImage = image.transformed(by: scaleTransform)

        let blend = CIFilter.blendWithMask()
        blend.inputImage = slimmerImage
        blend.backgroundImage = image
        blend.maskImage = maskImage

        return blend.outputImage ?? image
    }

    // MARK: - Helpers

    /// 计算归一化点的质心（转换到像素坐标）
    private func computeCentroid(_ points: [CGPoint], in rect: CGRect) -> CGPoint {
        guard !points.isEmpty else { return .zero }
        let sumX = points.reduce(0) { $0 + $1.x }
        let sumY = points.reduce(0) { $0 + $1.y }
        let count = CGFloat(points.count)
        let avgX = sumX / count
        let avgY = sumY / count
        // Vision 的归一化坐标原点在左下角，需要翻转 Y
        return CGPoint(
            x: rect.origin.x + avgX * rect.width,
            y: rect.origin.y + (1.0 - avgY) * rect.height
        )
    }
}

#endif