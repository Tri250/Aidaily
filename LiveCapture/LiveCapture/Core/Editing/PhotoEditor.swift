//
//  PhotoEditor.swift
//  LiveCapture
//
//  照片编辑器 - 基于 CIFilter 链的实时图像编辑，支持撤销/重做
//
//  ## 主要功能
//  - 亮度、对比度、饱和度、曝光、色温、色调、锐度、暗角调整
//  - 旋转和裁剪
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
    let vignette: Float
    let rotation: Double
    let cropRect: CGRect?
}

/// 照片编辑器
final class PhotoEditor: ObservableObject {

    // MARK: - 发布属性

    @Published var brightness: Float = 0       // -1 到 1
    @Published var contrast: Float = 0         // -1 到 1
    @Published var saturation: Float = 0       // -1 到 1
    @Published var exposure: Float = 0         // -2 到 2
    @Published var temperature: Float = 0      // -1 到 1 (warm/cool)
    @Published var tint: Float = 0             // -1 到 1 (green/magenta)
    @Published var sharpness: Float = 0        // 0 到 1
    @Published var vignette: Float = 0         // 0 到 1
    @Published var rotation: Double = 0        // degrees
    @Published var cropRect: CGRect?
    @Published var isEdited: Bool = false

    // MARK: - 私有属性

    private let context: CIContext
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!
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

    /// 加载原始图像
    /// - Parameter image: 输入 CIImage
    func loadImage(_ image: CIImage) {
        originalImage = image
        resetAll()
        saveState()
    }

    /// 从 UIImage 加载
    func loadUIImage(_ uiImage: UIImage) {
        if let ciImage = uiImage.ciImage ?? CIImage(image: uiImage) {
            loadImage(ciImage)
        }
    }

    // MARK: - 应用编辑

    /// 应用所有编辑效果，返回处理后的 CIImage
    /// - Returns: 编辑后的 CIImage，如果未加载原始图像返回 nil
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
            // 将 contrast/saturation 从 -1...1 映射到 CIColorControls 的有效范围
            let mappedContrast = 1.0 + contrast       // 0...2
            let mappedSaturation = 1.0 + saturation   // 0...2
            output = applyFilter(name: "CIColorControls", to: output, params: [
                kCIInputBrightnessKey: brightness,
                kCIInputContrastKey: max(0.25, mappedContrast),
                kCIInputSaturationKey: max(0, mappedSaturation)
            ]) ?? output
        }

        // 3. 色温色调 (CITemperatureAndTint)
        if abs(temperature) > 0.001 || abs(tint) > 0.001 {
            // 将 -1...1 映射到色温范围
            let tempOffset = temperature * 3000  // -3000K 到 3000K
            let tintOffset = tint * 100           // -100 到 100
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

        // 5. 暗角 (CIVignette)
        if vignette > 0.001 {
            let radius: Float = 1.0 - vignette * 0.5  // 1.0 到 0.5
            output = applyFilter(name: "CIVignette", to: output, params: [
                kCIInputIntensityKey: vignette * 2,  // 0 到 2
                kCIInputRadiusKey: radius
            ]) ?? output
        }

        // 6. 裁剪
        if let cropRect = cropRect {
            output = output.cropped(to: cropRect)
        }

        // 7. 旋转
        if abs(rotation) > 0.001 {
            let radians = rotation * .pi / 180.0
            output = output.transformed(by: CGAffineTransform(rotationAngle: radians))
        }

        return output
    }

    /// 通用 CIFilter 应用方法
    private func applyFilter(name: String, to image: CIImage, params: [String: Any]) -> CIImage? {
        guard let filter = CIFilter(name: name) else { return nil }
        filter.setValue(image, forKey: kCIInputImageKey)
        for (key, value) in params {
            filter.setValue(value, forKey: key)
        }
        return filter.outputImage
    }

    /// 获取裁剪后的图像
    func croppedImage() -> CIImage? {
        guard let edited = applyEdits() else { return nil }
        return edited
    }

    /// 渲染为 UIImage
    func renderToUIImage() -> UIImage? {
        guard let ciImage = applyEdits() else { return nil }
        let extent = ciImage.extent
        // 防止无限大范围
        guard extent.width > 0 && extent.height > 0,
              extent.width < 50000 && extent.height < 50000 else { return nil }

        guard let cgImage = context.createCGImage(ciImage, from: extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }

    // MARK: - 撤销/重做

    /// 撤销到上一个编辑状态
    func undo() {
        guard historyIndex > 0 else { return }
        historyIndex -= 1
        restoreState(editHistory[historyIndex])
    }

    /// 重做到下一个编辑状态
    func redo() {
        guard historyIndex < editHistory.count - 1 else { return }
        historyIndex += 1
        restoreState(editHistory[historyIndex])
    }

    /// 是否可以撤销
    var canUndo: Bool { historyIndex > 0 }

    /// 是否可以重做
    var canRedo: Bool { historyIndex < editHistory.count - 1 }

    // MARK: - 重置

    /// 重置所有编辑参数
    func resetAll() {
        brightness = 0
        contrast = 0
        saturation = 0
        exposure = 0
        temperature = 0
        tint = 0
        sharpness = 0
        vignette = 0
        rotation = 0
        cropRect = nil
        isEdited = false
    }

    // MARK: - 状态管理

    /// 保存当前编辑状态到历史记录
    private func saveState() {
        let state = EditState(
            brightness: brightness,
            contrast: contrast,
            saturation: saturation,
            exposure: exposure,
            temperature: temperature,
            tint: tint,
            sharpness: sharpness,
            vignette: vignette,
            rotation: rotation,
            cropRect: cropRect
        )

        // 移除当前位置之后的历史（因为新操作会覆盖旧的重做历史）
        if historyIndex < editHistory.count - 1 {
            editHistory = Array(editHistory[...historyIndex])
        }

        editHistory.append(state)
        historyIndex = editHistory.count - 1

        // 限制历史记录数量
        if editHistory.count > 50 {
            editHistory.removeFirst()
            historyIndex -= 1
        }

        updateEditedState()
    }

    /// 从历史记录恢复状态
    private func restoreState(_ state: EditState) {
        brightness = state.brightness
        contrast = state.contrast
        saturation = state.saturation
        exposure = state.exposure
        temperature = state.temperature
        tint = state.tint
        sharpness = state.sharpness
        vignette = state.vignette
        rotation = state.rotation
        cropRect = state.cropRect
        updateEditedState()
    }

    /// 更新编辑状态标识
    private func updateEditedState() {
        let hasEdits = abs(brightness) > 0.001 ||
            abs(contrast) > 0.001 ||
            abs(saturation) > 0.001 ||
            abs(exposure) > 0.001 ||
            abs(temperature) > 0.001 ||
            abs(tint) > 0.001 ||
            abs(sharpness) > 0.001 ||
            abs(vignette) > 0.001 ||
            abs(rotation) > 0.001 ||
            cropRect != nil
        isEdited = hasEdits
    }

    /// 标记当前状态已编辑（在参数变化时调用）
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