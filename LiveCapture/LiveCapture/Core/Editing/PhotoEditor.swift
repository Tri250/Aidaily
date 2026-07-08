//
//  PhotoEditor.swift
//  LiveCapture
//
//  照片编辑器 - 基于 CIFilter 链的实时图像编辑，支持撤销/重做
//
//  ## 主要功能
//  - 亮度、对比度、饱和度、曝光、色温、色调、锐度、暗角调整
//  - 旋转和裁剪
//  - 色调曲线编辑
//  - HSL 颜色调整
//  - 褪色效果
//  - 颗粒效果
//  - 水平翻转
//  - 完整的撤销/重做历史
//  - CIFilter 链顺序执行
//

import Foundation
import CoreImage
import UIKit
import Combine

#if os(iOS)

/// 编辑状态快照
struct EditState {
    let brightness: Float
    let contrast: Float
    let saturation: Float
    let exposure: Float
    let temperature: Float
    let tint: Float
    let sharpness: Float
    let vignetteIntensity: Float
    let vignetteRadius: Float
    let vignetteFeather: Float
    let highlightAmount: Float
    let shadowAmount: Float
    let fadeAmount: Float
    let grainAmount: Float
    let rotation: Double
    let flipHorizontal: Bool
    let cropRect: CGRect?
    let curveR: [Float]
    let curveG: [Float]
    let curveB: [Float]
    let hslHue: [Float]
    let hslSaturation: [Float]
    let hslLightness: [Float]
}

/// 照片编辑器
final class PhotoEditor: ObservableObject {

    // MARK: - 发布属性

    @Published var brightness: Float = 0
    @Published var contrast: Float = 0
    @Published var saturation: Float = 0
    @Published var exposure: Float = 0
    @Published var temperature: Float = 0
    @Published var tint: Float = 0
    @Published var sharpness: Float = 0
    @Published var highlightAmount: Float = 1.0
    @Published var shadowAmount: Float = 0.0
    @Published var vignetteIntensity: Float = 0
    @Published var vignetteRadius: Float = 0.5
    @Published var vignetteFeather: Float = 0.5
    @Published var fadeAmount: Float = 0
    @Published var grainAmount: Float = 0
    @Published var rotation: Double = 0
    @Published var flipHorizontal: Bool = false
    @Published var cropRect: CGRect?
    @Published var isEdited: Bool = false

    @Published var curveR: [Float] = [0.0, 0.25, 0.5, 0.75, 1.0]
    @Published var curveG: [Float] = [0.0, 0.25, 0.5, 0.75, 1.0]
    @Published var curveB: [Float] = [0.0, 0.25, 0.5, 0.75, 1.0]

    @Published var hslHue: [Float] = [0, 0, 0, 0, 0, 0, 0, 0]
    @Published var hslSaturation: [Float] = [0, 0, 0, 0, 0, 0, 0, 0]
    @Published var hslLightness: [Float] = [0, 0, 0, 0, 0, 0, 0, 0]

    // MARK: - 私有属性

    private let context: CIContext
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()
    private var originalImage: CIImage?
    private var editHistory: [EditState] = []
    private var historyIndex: Int = -1

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "PhotoEditor"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "PhotoEditor"
            ])
        }
    }

    // MARK: - 加载图像

    func loadImage(_ image: CIImage) {
        originalImage = image
        resetAll()
        saveState()
    }

    func loadUIImage(_ uiImage: UIImage) {
        if let ciImage = uiImage.ciImage ?? CIImage(image: uiImage) {
            loadImage(ciImage)
        }
    }

    // MARK: - 应用编辑

    func applyEdits() -> CIImage? {
        guard let image = originalImage else { return nil }
        var output = image

        // 1. 曝光调整 (CIExposureAdjust)
        if abs(exposure) > 0.001 {
            output = applyFilter(name: "CIExposureAdjust", to: output, params: [
                kCIInputEVKey: exposure
            ]) ?? output
        }

        // 2. 颜色控制 (CIColorControls: 亮度、对比度、饱和度)
        if abs(brightness) > 0.001 || abs(contrast) > 0.001 || abs(saturation) > 0.001 {
            let mappedContrast = 1.0 + contrast
            let mappedSaturation = 1.0 + saturation
            output = applyFilter(name: "CIColorControls", to: output, params: [
                kCIInputBrightnessKey: brightness,
                kCIInputContrastKey: max(0.25, mappedContrast),
                kCIInputSaturationKey: max(0, mappedSaturation)
            ]) ?? output
        }

        // 3. 色温色调 (CITemperatureAndTint)
        if abs(temperature) > 0.001 || abs(tint) > 0.001 {
            let tempOffset = temperature * 3000
            let tintOffset = tint * 100
            let neutral = CIVector(x: 6500, y: 0)
            let target = CIVector(x: CGFloat(6500 + tempOffset), y: CGFloat(tintOffset))
            output = applyFilter(name: "CITemperatureAndTint", to: output, params: [
                "inputNeutral": neutral,
                "inputTargetNeutral": target
            ]) ?? output
        }

        // 4. 锐度 (CISharpenLuminance)
        if sharpness > 0.001 {
            output = applyFilter(name: "CISharpenLuminance", to: output, params: [
                kCIInputSharpnessKey: sharpness
            ]) ?? output
        }

        // 5. 高光阴影调整
        output = applyHighlightShadow(to: output)

        // 6. 色调曲线
        if hasCurveEdits() {
            output = applyToneCurve(to: output)
        }

        // 7. HSL 颜色调整
        if hasHSLEdits() {
            output = applyHSLColorMatrix(to: output)
        }

        // 8. 褪色效果
        if fadeAmount > 0.001 {
            output = applyFadeEffect(to: output, amount: fadeAmount)
        }

        // 9. 颗粒效果
        if grainAmount > 0.001 {
            output = applyGrainEffect(to: output, amount: grainAmount)
        }

        // 10. 暗角 (CIVignette)
        if vignetteIntensity > 0.001 {
            output = applyVignetteFull(to: output)
        }

        // 11. 水平翻转
        if flipHorizontal {
            output = applyFlip(to: output)
        }

        // 12. 裁剪
        if let cropRect = cropRect {
            output = output.cropped(to: cropRect)
        }

        // 13. 旋转
        if abs(rotation) > 0.001 {
            let radians = rotation * .pi / 180.0
            output = output.transformed(by: CGAffineTransform(rotationAngle: radians))
        }

        return output
    }

    // MARK: - 高光阴影

    private func applyHighlightShadow(to image: CIImage) -> CIImage {
        if abs(highlightAmount - 1.0) < 0.001 && abs(shadowAmount) < 0.001 { return image }
        guard let filter = CIFilter(name: "CIHighlightShadowAdjust") else { return image }
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(CGFloat(highlightAmount), forKey: "inputHighlightAmount")
        filter.setValue(CGFloat(shadowAmount), forKey: "inputShadowAmount")
        return filter.outputImage ?? image
    }

    // MARK: - 色调曲线

    private func hasCurveEdits() -> Bool {
        let linear: [Float] = [0.0, 0.25, 0.5, 0.75, 1.0]
        return curveR != linear || curveG != linear || curveB != linear
    }

    private func applyToneCurve(to image: CIImage) -> CIImage {
        guard let filter = CIFilter(name: "CIToneCurve") else { return image }
        let xPoints: [CGFloat] = [0.0, 0.25, 0.5, 0.75, 1.0]
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(CIVector(values: xPoints, count: 5), forKey: "inputPoint0")
        filter.setValue(CIVector(values: curveR.map { CGFloat($0) }, count: 5), forKey: "inputPoint1")
        filter.setValue(CIVector(values: curveG.map { CGFloat($0) }, count: 5), forKey: "inputPoint2")
        filter.setValue(CIVector(values: curveB.map { CGFloat($0) }, count: 5), forKey: "inputPoint3")
        filter.setValue(CIVector(values: xPoints, count: 5), forKey: "inputPoint4")
        return filter.outputImage ?? image
    }

    // MARK: - HSL 颜色矩阵

    private func hasHSLEdits() -> Bool {
        for i in 0..<8 {
            if abs(hslHue[i]) > 0.001 || abs(hslSaturation[i]) > 0.001 || abs(hslLightness[i]) > 0.001 {
                return true
            }
        }
        return false
    }

    private func applyHSLColorMatrix(to image: CIImage) -> CIImage {
        var output = image
        let channelColors: [CIColor] = [
            CIColor(red: 1, green: 0, blue: 0),       // 红
            CIColor(red: 1, green: 0.5, blue: 0),      // 橙
            CIColor(red: 1, green: 1, blue: 0),         // 黄
            CIColor(red: 0, green: 1, blue: 0),         // 绿
            CIColor(red: 0, green: 1, blue: 1),         // 青
            CIColor(red: 0, green: 0, blue: 1),         // 蓝
            CIColor(red: 0.5, green: 0, blue: 1),       // 紫
            CIColor(red: 1, green: 0, blue: 0.5)        // 洋红
        ]

        for i in 0..<8 {
            let h = hslHue[i]
            let s = hslSaturation[i]
            let l = hslLightness[i]

            if abs(h) < 0.001 && abs(s) < 0.001 && abs(l) < 0.001 { continue }

            let hueShift = CGFloat(h) * CGFloat.pi * 2
            let cosH = cos(hueShift)
            let sinH = sin(hueShift)

            let satScale = 1.0 + CGFloat(s)
            let lightScale = 1.0 + CGFloat(l)

            let targetColor = channelColors[i]
            let r = targetColor.red
            let g = targetColor.green
            let b = targetColor.blue

            let lumR: CGFloat = 0.299
            let lumG: CGFloat = 0.587
            let lumB: CGFloat = 0.114

            let rr = (r * cosH + (r * lumR - r) * (1 - cosH) + ((g * lumG + b * lumB)) * sinH) * satScale * lightScale
            let rg = (g * cosH + (g * lumG - g) * (1 - cosH) + ((b * lumB + r * lumR)) * sinH) * satScale * lightScale
            let rb = (b * cosH + (b * lumB - b) * (1 - cosH) + ((r * lumR + g * lumG)) * sinH) * satScale * lightScale

            let gr = (r * cosH + (r * lumR - r) * (1 - cosH) + ((g * lumG + b * lumB)) * sinH) * satScale * lightScale
            let gg = (g * cosH + (g * lumG - g) * (1 - cosH) + ((b * lumB + r * lumR)) * sinH) * satScale * lightScale
            let gb = (b * cosH + (b * lumB - b) * (1 - cosH) + ((r * lumR + g * lumG)) * sinH) * satScale * lightScale

            let br = (r * cosH + (r * lumR - r) * (1 - cosH) + ((g * lumG + b * lumB)) * sinH) * satScale * lightScale
            let bg = (g * cosH + (g * lumG - g) * (1 - cosH) + ((b * lumB + r * lumR)) * sinH) * satScale * lightScale
            let bb = (b * cosH + (b * lumB - b) * (1 - cosH) + ((r * lumR + g * lumG)) * sinH) * satScale * lightScale

            output = applyHueMatrix(to: output, targetColor: targetColor,
                                     rv: CIVector(x: rr, y: rg, z: rb, w: 0),
                                     gv: CIVector(x: gr, y: gg, z: gb, w: 0),
                                     bv: CIVector(x: br, y: bg, z: bb, w: 0))
        }

        return output
    }

    private func applyHueMatrix(to image: CIImage, targetColor: CIColor,
                                 rv: CIVector, gv: CIVector, bv: CIVector) -> CIImage {
        let mask = image
            .applyingFilter("CIColorMatrix", parameters: [
                "inputRVector": CIVector(x: 0, y: 0, z: 0, w: CGFloat(targetColor.red)),
                "inputGVector": CIVector(x: 0, y: 0, z: 0, w: CGFloat(targetColor.green)),
                "inputBVector": CIVector(x: 0, y: 0, z: 0, w: CGFloat(targetColor.blue)),
                "inputBiasVector": CIVector(x: 0, y: 0, z: 0, w: 0)
            ])

        let masked = image.applyingFilter("CIBlendWithMask", parameters: [
            kCIInputBackgroundImageKey: image,
            kCIInputMaskImageKey: mask
        ])

        let hueShifted = masked.applyingFilter("CIColorMatrix", parameters: [
            "inputRVector": rv,
            "inputGVector": gv,
            "inputBVector": bv,
            "inputBiasVector": CIVector(x: 0, y: 0, z: 0, w: 0)
        ])

        let blended = hueShifted.applyingFilter("CIBlendWithMask", parameters: [
            kCIInputBackgroundImageKey: image,
            kCIInputMaskImageKey: mask
        ])

        return blended ?? image
    }

    // MARK: - 褪色效果

    private func applyFadeEffect(to image: CIImage, amount: Float) -> CIImage {
        let fade = 1.0 - amount * 0.4
        guard let controls = CIFilter(name: "CIColorControls") else { return image }
        controls.setValue(image, forKey: kCIInputImageKey)
        controls.setValue(CGFloat(fade), forKey: kCIInputContrastKey)
        let contrastAdjusted = controls.outputImage ?? image

        let lift = amount * 0.15
        guard let exposureFilter = CIFilter(name: "CIExposureAdjust") else { return contrastAdjusted }
        exposureFilter.setValue(contrastAdjusted, forKey: kCIInputImageKey)
        exposureFilter.setValue(CGFloat(lift), forKey: kCIInputEVKey)
        let lifted = exposureFilter.outputImage ?? contrastAdjusted

        let desat = 1.0 - amount * 0.3
        guard let satFilter = CIFilter(name: "CIColorControls") else { return lifted }
        satFilter.setValue(lifted, forKey: kCIInputImageKey)
        satFilter.setValue(CGFloat(desat), forKey: kCIInputSaturationKey)
        return satFilter.outputImage ?? lifted
    }

    // MARK: - 颗粒效果

    private func applyGrainEffect(to image: CIImage, amount: Float) -> CIImage {
        guard let noise = CIFilter(name: "CIRandomGenerator")?.outputImage else { return image }
        let extent = image.extent

        let noiseScaled = noise.transformed(by: CGAffineTransform(scaleX: extent.width / 100, y: extent.height / 100))
        let noiseCropped = noiseScaled.cropped(to: extent)

        guard let monoNoise = CIFilter(name: "CIColorMonochrome") else { return image }
        monoNoise.setValue(noiseCropped, forKey: kCIInputImageKey)
        monoNoise.setValue(CIColor(red: 0.5, green: 0.5, blue: 0.5), forKey: kCIInputColorKey)
        monoNoise.setValue(1.0, forKey: kCIInputIntensityKey)
        let grayNoise = monoNoise.outputImage ?? noiseCropped

        let alpha = CGFloat(amount * 0.15)
        guard let blend = CIFilter(name: "CIBlendWithAlphaMask") else { return image }
        blend.setValue(image, forKey: kCIInputImageKey)
        blend.setValue(grayNoise, forKey: kCIInputBackgroundImageKey)

        guard let constantColor = CIFilter(name: "CIConstantColorGenerator") else { return image }
        constantColor.setValue(CIColor(red: 1, green: 1, blue: 1, alpha: alpha), forKey: kCIInputColorKey)
        let maskImg = constantColor.outputImage?.cropped(to: extent)
        blend.setValue(maskImg, forKey: kCIInputMaskImageKey)

        return blend.outputImage ?? image
    }

    // MARK: - 暗角（完整）

    private func applyVignetteFull(to image: CIImage) -> CIImage {
        let radius = 1.0 - vignetteRadius * 0.7
        let intensity = vignetteIntensity * 2.0
        let softness = vignetteFeather

        guard let vignette = CIFilter(name: "CIVignette") else { return image }
        vignette.setValue(image, forKey: kCIInputImageKey)
        vignette.setValue(CGFloat(intensity), forKey: kCIInputIntensityKey)
        vignette.setValue(CGFloat(radius), forKey: kCIInputRadiusKey)

        return vignette.outputImage ?? image
    }

    // MARK: - 水平翻转

    private func applyFlip(to image: CIImage) -> CIImage {
        let extent = image.extent
        let transform = CGAffineTransform(scaleX: -1, y: 1)
            .translatedBy(x: -extent.width, y: 0)
        return image.transformed(by: transform)
    }

    // MARK: - 通用 CIFilter 应用

    private func applyFilter(name: String, to image: CIImage, params: [String: Any]) -> CIImage? {
        guard let filter = CIFilter(name: name) else { return nil }
        filter.setValue(image, forKey: kCIInputImageKey)
        for (key, value) in params {
            filter.setValue(value, forKey: key)
        }
        return filter.outputImage
    }

    // MARK: - 渲染

    func croppedImage() -> CIImage? {
        guard let edited = applyEdits() else { return nil }
        return edited
    }

    func renderToUIImage() -> UIImage? {
        guard let ciImage = applyEdits() else { return nil }
        let extent = ciImage.extent
        guard extent.width > 0 && extent.height > 0,
              extent.width < 50000 && extent.height < 50000 else { return nil }

        guard let cgImage = context.createCGImage(ciImage, from: extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }

    func renderPreview(size: CGSize) -> UIImage? {
        guard let ciImage = applyEdits() else { return nil }
        let extent = ciImage.extent
        let scaleX = size.width / extent.width
        let scaleY = size.height / extent.height
        let scale = min(scaleX, scaleY)
        let scaled = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let scaledExtent = scaled.extent
        guard let cgImage = context.createCGImage(scaled, from: scaledExtent) else { return nil }
        return UIImage(cgImage: cgImage)
    }

    // MARK: - 撤销/重做

    func undo() {
        guard historyIndex > 0 else { return }
        historyIndex -= 1
        restoreState(editHistory[historyIndex])
    }

    func redo() {
        guard historyIndex < editHistory.count - 1 else { return }
        historyIndex += 1
        restoreState(editHistory[historyIndex])
    }

    var canUndo: Bool { historyIndex > 0 }
    var canRedo: Bool { historyIndex < editHistory.count - 1 }

    // MARK: - 重置

    func resetAll() {
        brightness = 0
        contrast = 0
        saturation = 0
        exposure = 0
        temperature = 0
        tint = 0
        sharpness = 0
        vignetteIntensity = 0
        vignetteRadius = 0.5
        vignetteFeather = 0.5
        highlightAmount = 1.0
        shadowAmount = 0.0
        fadeAmount = 0
        grainAmount = 0
        rotation = 0
        flipHorizontal = false
        cropRect = nil
        curveR = [0.0, 0.25, 0.5, 0.75, 1.0]
        curveG = [0.0, 0.25, 0.5, 0.75, 1.0]
        curveB = [0.0, 0.25, 0.5, 0.75, 1.0]
        hslHue = [0, 0, 0, 0, 0, 0, 0, 0]
        hslSaturation = [0, 0, 0, 0, 0, 0, 0, 0]
        hslLightness = [0, 0, 0, 0, 0, 0, 0, 0]
        isEdited = false
    }

    // MARK: - 状态管理

    func saveState() {
        let state = EditState(
            brightness: brightness,
            contrast: contrast,
            saturation: saturation,
            exposure: exposure,
            temperature: temperature,
            tint: tint,
            sharpness: sharpness,
            vignetteIntensity: vignetteIntensity,
            vignetteRadius: vignetteRadius,
            vignetteFeather: vignetteFeather,
            highlightAmount: highlightAmount,
            shadowAmount: shadowAmount,
            fadeAmount: fadeAmount,
            grainAmount: grainAmount,
            rotation: rotation,
            flipHorizontal: flipHorizontal,
            cropRect: cropRect,
            curveR: curveR,
            curveG: curveG,
            curveB: curveB,
            hslHue: hslHue,
            hslSaturation: hslSaturation,
            hslLightness: hslLightness
        )

        if historyIndex < editHistory.count - 1 {
            editHistory = Array(editHistory[...historyIndex])
        }

        editHistory.append(state)
        historyIndex = editHistory.count - 1

        if editHistory.count > 50 {
            editHistory.removeFirst()
            historyIndex -= 1
        }

        updateEditedState()
    }

    private func restoreState(_ state: EditState) {
        brightness = state.brightness
        contrast = state.contrast
        saturation = state.saturation
        exposure = state.exposure
        temperature = state.temperature
        tint = state.tint
        sharpness = state.sharpness
        vignetteIntensity = state.vignetteIntensity
        vignetteRadius = state.vignetteRadius
        vignetteFeather = state.vignetteFeather
        highlightAmount = state.highlightAmount
        shadowAmount = state.shadowAmount
        fadeAmount = state.fadeAmount
        grainAmount = state.grainAmount
        rotation = state.rotation
        flipHorizontal = state.flipHorizontal
        cropRect = state.cropRect
        curveR = state.curveR
        curveG = state.curveG
        curveB = state.curveB
        hslHue = state.hslHue
        hslSaturation = state.hslSaturation
        hslLightness = state.hslLightness
        updateEditedState()
    }

    private func updateEditedState() {
        let hasEdits = abs(brightness) > 0.001 ||
            abs(contrast) > 0.001 ||
            abs(saturation) > 0.001 ||
            abs(exposure) > 0.001 ||
            abs(temperature) > 0.001 ||
            abs(tint) > 0.001 ||
            abs(sharpness) > 0.001 ||
            abs(vignetteIntensity) > 0.001 ||
            abs(fadeAmount) > 0.001 ||
            abs(grainAmount) > 0.001 ||
            abs(rotation) > 0.001 ||
            flipHorizontal ||
            cropRect != nil ||
            hasCurveEdits() ||
            hasHSLEdits()
        isEdited = hasEdits
    }

    func markEdited() {
        updateEditedState()
        if isEdited {
            saveState()
        }
    }
}

// MARK: - Metal 导入

import Metal

#endif