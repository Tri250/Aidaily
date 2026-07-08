//
//  PhotoEditorViewModel.swift
//  LiveCapture
//
//  照片编辑 ViewModel - 协调所有编辑操作
//

import Foundation
import CoreImage
import UIKit
import SwiftUI
import Combine
import Photos

#if os(iOS)

/// 编辑工具类型
enum EditTool: String, CaseIterable, Identifiable {
    case crop = "裁剪"
    case adjustment = "调节"
    case filter = "滤镜"
    case grain = "颗粒"
    case vignette = "暗角"

    var id: String { rawValue }

    var iconName: String {
        switch self {
        case .crop: return "crop"
        case .adjustment: return "slider.horizontal.3"
        case .filter: return "camera.filters"
        case .grain: return "circle.dotted"
        case .vignette: return "circle.lefthalf.filled"
        }
    }
}

/// 照片编辑 ViewModel
final class PhotoEditorViewModel: ObservableObject {

    // MARK: - 发布属性

    @Published var originalImage: UIImage?
    @Published var previewImage: UIImage?
    @Published var selectedTool: EditTool = .adjustment
    @Published var isProcessing: Bool = false
    @Published var showSaveSuccess: Bool = false
    @Published var selectedFilterPreset: LutFilterPreset?

    let editor: PhotoEditor
    let historyManager: EditHistoryManager
    let autoEnhancer: AutoEnhancer

    private var cancellables = Set<AnyCancellable>()
    private let renderQueue = DispatchQueue(label: "com.livecapture.editor.render", qos: .userInteractive)

    // MARK: - 初始化

    init() {
        self.editor = PhotoEditor()
        self.historyManager = EditHistoryManager()
        self.autoEnhancer = AutoEnhancer()

        setupBindings()
    }

    private func setupBindings() {
        editor.$brightness
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$contrast
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$saturation
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$exposure
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$temperature
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$tint
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$sharpness
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$vignetteIntensity
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$vignetteRadius
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$vignetteFeather
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$fadeAmount
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$grainAmount
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$curveR
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$curveG
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$curveB
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$hslHue
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$hslSaturation
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)

        editor.$hslLightness
            .dropFirst()
            .debounce(for: .milliseconds(50), scheduler: renderQueue)
            .sink { [weak self] _ in self?.updatePreview() }
            .store(in: &cancellables)
    }

    // MARK: - 加载图像

    func loadImage(_ uiImage: UIImage) {
        originalImage = uiImage
        previewImage = uiImage
        guard let ciImage = uiImage.ciImage ?? CIImage(image: uiImage) else { return }
        editor.loadImage(ciImage)
        historyManager.reset()
        historyManager.recordStep(filterName: "原始图像", thumbnail: uiImage)
    }

    // MARK: - 预览更新

    func updatePreview() {
        guard let previewSize = previewImage?.size else { return }
        renderQueue.async { [weak self] in
            guard let self = self else { return }
            if let rendered = self.editor.renderPreview(size: CGSize(width: previewSize.width, height: previewSize.height)) {
                DispatchQueue.main.async {
                    self.previewImage = rendered
                }
            }
        }
    }

    func updatePreviewImmediate() {
        guard let previewSize = previewImage?.size else { return }
        if let rendered = editor.renderPreview(size: CGSize(width: previewSize.width, height: previewSize.height)) {
            previewImage = rendered
        }
    }

    // MARK: - 编辑操作

    func applyAdjustment(parameter: String, value: Float) {
        switch parameter {
        case "brightness": editor.brightness = value
        case "contrast": editor.contrast = value
        case "saturation": editor.saturation = value
        case "exposure": editor.exposure = value
        case "temperature": editor.temperature = value
        case "tint": editor.tint = value
        case "sharpness": editor.sharpness = value
        case "fade": editor.fadeAmount = value
        default: break
        }
    }

    func applyCrop(rect: CGRect?, rotation: Double, flip: Bool) {
        editor.cropRect = rect
        editor.rotation = rotation
        editor.flipHorizontal = flip
        editor.markEdited()
        historyManager.recordStep(filterName: "裁剪", parameters: [
            "rect": rect as Any,
            "rotation": rotation,
            "flip": flip
        ])
        updatePreview()
    }

    func applyFilter(_ preset: LutFilterPreset) {
        selectedFilterPreset = preset
        let params = preset.parameters
        editor.brightness = params.brightness
        editor.contrast = params.contrast - 1.0
        editor.saturation = params.saturation - 1.0
        editor.exposure = params.exposure
        editor.temperature = params.temperature
        editor.tint = params.tint
        editor.markEdited()
        historyManager.recordStep(filterName: "滤镜: \(preset.displayName)")
        updatePreview()
    }

    func applyVignette(intensity: Float, radius: Float, feather: Float) {
        editor.vignetteIntensity = intensity
        editor.vignetteRadius = radius
        editor.vignetteFeather = feather
        editor.markEdited()
        historyManager.recordStep(filterName: "暗角", parameters: [
            "intensity": intensity,
            "radius": radius,
            "feather": feather
        ])
        updatePreview()
    }

    func applyGrain(amount: Float) {
        editor.grainAmount = amount
        editor.markEdited()
        updatePreview()
    }

    func applyCurve(r: [Float], g: [Float], b: [Float]) {
        editor.curveR = r
        editor.curveG = g
        editor.curveB = b
        editor.markEdited()
        updatePreview()
    }

    func applyHSL(hue: [Float], saturation: [Float], lightness: [Float]) {
        editor.hslHue = hue
        editor.hslSaturation = saturation
        editor.hslLightness = lightness
        editor.markEdited()
        updatePreview()
    }

    // MARK: - 撤销/重做

    func undo() {
        editor.undo()
        updatePreview()
    }

    func redo() {
        editor.redo()
        updatePreview()
    }

    // MARK: - 自动增强

    func autoEnhanceAction() {
        guard let ciImage = editor.croppedImage() else { return }
        let enhanced = autoEnhancer.autoEnhance(ciImage)
        editor.loadImage(enhanced)
        historyManager.recordStep(filterName: "自动增强")
        updatePreview()
    }

    // MARK: - 保存

    func save() {
        isProcessing = true
        guard let image = editor.renderToUIImage() else {
            isProcessing = false
            return
        }

        PHPhotoLibrary.requestAuthorization(for: .addOnly) { [weak self] status in
            guard let self = self, status == .authorized || status == .limited else {
                DispatchQueue.main.async { self?.isProcessing = false }
                return
            }

            PHPhotoLibrary.shared().performChanges {
                PHAssetCreationRequest.forAsset().addResource(with: .photo, data: image.jpegData(compressionQuality: 0.95)!, options: nil)
            } completionHandler: { [weak self] success, _ in
                DispatchQueue.main.async {
                    self?.isProcessing = false
                    if success {
                        self?.showSaveSuccess = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            self?.showSaveSuccess = false
                        }
                    }
                }
            }
        }
    }

    func export() -> UIImage? {
        return editor.renderToUIImage()
    }

    func exportFullResolution() -> UIImage? {
        return editor.renderToUIImage()
    }

    // MARK: - 重置

    func resetAll() {
        editor.resetAll()
        historyManager.reset()
        previewImage = originalImage
    }
}

#endif