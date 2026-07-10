//
//  PortraitViewModel.swift
//  LiveCapture
//
//  人像模式视图模型
//
//  ## 文件作用
//  管理人像模式的状态和图像处理管线，连接 PortraitEffectEngine 和
//  SkinBeautifier，为 UI 层提供响应式数据绑定。
//
//  ## 主要类
//  - PortraitViewModel: 人像模式视图模型（ObservableObject）
//
//  ## Published 状态
//  - 各美颜参数：skinSmoothing, skinTone, blemishRemoval 等
//  - portraitBlur: 人像虚化强度
//  - bokehParams: 虚化参数
//  - lightingType: 当前光效类型
//  - isPortraitModeEnabled: 人像模式开关
//  - isBeautyEnabled: 美颜开关
//  - processedPreview: 处理后的预览图像
//  - isProcessing: 是否正在处理
//  - faceCount: 检测到的人脸数量
//  - hasPortrait: 是否检测到人像
//  - currentPreset: 当前预设
//
//  ## 预设
//  - 自然: 全部关闭
//  - 精致: 中等美颜
//  - 女神: 高强度美颜
//  - 自定义: 手动调整
//
//  ## 核心方法
//  - processImage(_:): 执行完整的人像效果处理管线
//  - applyBeautyPipeline(to:faceObservations:): 仅美颜管线
//  - detectFaces(in:): 使用 Vision 检测人脸
//  - applyPreset(_:): 应用预设
//  - reset(): 重置所有参数到默认值
//
//  ## 处理管线
//  1. 检测人像（人脸检测）
//  2. 应用美颜（Beauty）
//  3. 应用虚化（Bokeh）
//  4. 应用光效（Lighting）
//  5. 生成预览
//

import Foundation
import Combine
import CoreImage
import Vision

#if os(iOS)
import UIKit

/// 美颜预设
enum BeautyPreset: String, CaseIterable {
    case natural      // 自然（关闭）
    case delicate     // 精致
    case goddess      // 女神
    case custom       // 自定义

    var displayName: String {
        switch self {
        case .natural:  return "自然"
        case .delicate: return "精致"
        case .goddess:  return "女神"
        case .custom:   return "自定义"
        }
    }

    /// 获取预设对应的美颜参数
    func params() -> BeautyParams {
        switch self {
        case .natural:
            return BeautyParams(
                skinSmoothing: 0,
                skinTone: 0,
                eyeBrightening: 0,
                teethWhitening: 0,
                faceSlimming: 0,
                blemishRemoval: 0
            )
        case .delicate:
            return BeautyParams(
                skinSmoothing: 0.4,
                skinTone: 0.2,
                eyeBrightening: 0.3,
                teethWhitening: 0.2,
                faceSlimming: 0.15,
                blemishRemoval: 0.4
            )
        case .goddess:
            return BeautyParams(
                skinSmoothing: 0.7,
                skinTone: 0.5,
                eyeBrightening: 0.6,
                teethWhitening: 0.5,
                faceSlimming: 0.4,
                blemishRemoval: 0.7
            )
        case .custom:
            return BeautyParams.default
        }
    }
}

/// 人像模式视图模型
final class PortraitViewModel: ObservableObject {
    // MARK: - Published State: 美颜参数

    /// 磨皮 0-1
    @Published var skinSmoothing: Float = 0.0
    /// 美白 -1（冷白）到 1（暖黄）
    @Published var skinTone: Float = 0.0
    /// 祛痘 0-1
    @Published var blemishRemoval: Float = 0.0
    /// 亮眼 0-1
    @Published var eyeBrightening: Float = 0.0
    /// 牙齿美白 0-1
    @Published var teethWhitening: Float = 0.0
    /// 瘦脸 0-1
    @Published var faceSlimming: Float = 0.0
    /// 人像虚化 0-1
    @Published var portraitBlur: Float = 0.0

    // MARK: - Published State: 模式与预设

    /// 当前预设
    @Published var currentPreset: BeautyPreset = .natural
    /// 美颜开关
    @Published var isBeautyEnabled: Bool = false
    /// 人像模式开关
    @Published var isPortraitModeEnabled: Bool = false
    /// 当前光效类型
    @Published var lightingType: PortraitLightingType = .natural
    /// 虚化参数
    @Published var bokehParams = BokehParams()

    // MARK: - Published State: 处理结果

    /// 处理后的预览图像
    @Published var processedPreview: UIImage?
    /// 是否正在处理
    @Published private(set) var isProcessing: Bool = false
    /// 检测到的人脸数量
    @Published private(set) var faceCount: Int = 0
    /// 是否检测到人像
    @Published private(set) var hasPortrait: Bool = false

    // MARK: - Dependencies

    private let engine = PortraitEffectEngine()
    private let beautifier = SkinBeautifier()
    private let processingQueue = DispatchQueue(label: "livecapture.portrait.viewmodel")
    private let faceDetectionQueue = DispatchQueue(label: "livecapture.portrait.facedetect")

    // MARK: - Private State

    private var lastProcessedImage: CIImage?
    private var lastFaceObservations: [VNFaceObservation] = []
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Initialization

    init() {
        setupBindings()
    }

    // MARK: - Bindings

    private func setupBindings() {
        // 监听美颜参数变化，自动重新处理
        Publishers.CombineLatest7(
            $skinSmoothing,
            $skinTone,
            $blemishRemoval,
            $eyeBrightening,
            $teethWhitening,
            $faceSlimming,
            $portraitBlur
        )
        .dropFirst()
        .debounce(for: .milliseconds(80), scheduler: DispatchQueue.main)
        .sink { [weak self] _ in
            self?.reprocessIfNeeded()
        }
        .store(in: &cancellables)

        $bokehParams
            .dropFirst()
            .debounce(for: .milliseconds(100), scheduler: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.reprocessIfNeeded()
            }
            .store(in: &cancellables)

        $lightingType
            .dropFirst()
            .sink { [weak self] _ in
                self?.reprocessIfNeeded()
            }
            .store(in: &cancellables)

        $isPortraitModeEnabled
            .dropFirst()
            .sink { [weak self] enabled in
                if enabled {
                    self?.reprocessIfNeeded()
                } else {
                    self?.processedPreview = nil
                    self?.lastProcessedImage = nil
                    self?.lastFaceObservations = []
                    self?.faceCount = 0
                    self?.hasPortrait = false
                }
            }
            .store(in: &cancellables)
    }

    // MARK: - Image Processing

    /// 处理输入图像，执行完整的人像效果管线
    /// - Parameter image: 输入 CIImage
    func processImage(_ image: CIImage) {
        guard isPortraitModeEnabled || isBeautyEnabled else { return }
        guard !isProcessing else { return }

        isProcessing = true
        let currentBeautyParams = buildBeautyParams()
        let currentBokehParams = bokehParams
        let currentLighting = lightingType
        let currentBlur = portraitBlur

        processingQueue.async { [weak self] in
            guard let self else { return }

            // 1. 检测人脸
            let faceObservations = self.detectFaceObservations(in: image)

            let hasFace = !faceObservations.isEmpty

            DispatchQueue.main.async {
                self.faceCount = faceObservations.count
                self.hasPortrait = hasFace
            }

            // 2. 应用效果管线
            var result = image

            // 2a. 美颜
            if !currentBeautyParams.isOff && hasFace {
                result = self.beautifier.applyBeauty(
                    to: result,
                    params: currentBeautyParams,
                    faceObservations: faceObservations
                )
            }

            // 2b. 背景虚化
            if currentBlur > 0.01 && hasFace {
                let blurRadius = CGFloat(currentBlur) * 20.0
                result = self.applyDepthBlur(to: result, blurRadius: blurRadius, faceObservations: faceObservations)
            }

            // 2c. 光效
            if currentLighting != .natural && hasFace {
                result = self.engine.applyLighting(
                    to: result,
                    type: currentLighting,
                    faceObservations: faceObservations
                )
            }

            // 3. 生成预览
            let preview = self.renderPreview(from: result)

            DispatchQueue.main.async {
                self.processedPreview = preview
                self.lastProcessedImage = result
                self.lastFaceObservations = faceObservations
                self.isProcessing = false
            }
        }
    }

    /// 使用上次的参数重新处理
    private func reprocessIfNeeded() {
        guard let lastImage = lastProcessedImage else { return }
        processImage(lastImage)
    }

    // MARK: - Public API: Photo Pipeline

    /// 异步处理照片：检测人脸并应用完整的人像效果管线（美颜 + 虚化 + 光效）。
    /// - Parameters:
    ///   - image: 输入图像
    ///   - completion: 处理完成回调（主线程）
    func processPhoto(_ image: CIImage, completion: @escaping (CIImage) -> Void) {
        processingQueue.async { [weak self] in
            guard let self else { return }
            let faces = self.detectFaceObservations(in: image)
            let processed = self.applyFullPipeline(to: image, faceObservations: faces)
            DispatchQueue.main.async {
                completion(processed)
            }
        }
    }

    /// 对图像应用完整的人像效果管线（美颜 + 虚化 + 光效）。
    /// - Parameters:
    ///   - image: 输入图像
    ///   - faceObservations: 人脸检测结果（可选，不传则自动检测）
    /// - Returns: 处理后的图像
    func applyFullPipeline(to image: CIImage, faceObservations: [VNFaceObservation]? = nil) -> CIImage {
        let faces: [VNFaceObservation]
        if let provided = faceObservations, !provided.isEmpty {
            faces = provided
        } else {
            faces = detectFaceObservations(in: image)
        }

        guard !faces.isEmpty else { return image }

        var result = image

        // 1. 美颜
        let params = buildBeautyParams()
        if !params.isOff {
            result = beautifier.applyBeauty(to: result, params: params, faceObservations: faces)
        }

        // 2. 背景虚化
        if portraitBlur > 0.01 {
            let blurRadius = CGFloat(portraitBlur) * 20.0
            result = applyDepthBlur(to: result, blurRadius: blurRadius, faceObservations: faces)
        }

        // 3. 光效
        if lightingType != .natural {
            result = engine.applyLighting(to: result, type: lightingType, faceObservations: faces)
        }

        return result
    }

    /// 对图像应用美颜管线（供外部调用，如拍照时）
    /// - Parameters:
    ///   - image: 输入图像
    ///   - faceObservations: 人脸检测结果（可选，不传则自动检测）
    /// - Returns: 美颜后的图像
    func applyBeautyPipeline(to image: CIImage, faceObservations: [VNFaceObservation]? = nil) -> CIImage {
        let faces: [VNFaceObservation]
        if let provided = faceObservations, !provided.isEmpty {
            faces = provided
        } else {
            faces = detectFaceObservations(in: image)
        }

        let params = buildBeautyParams()

        guard !params.isOff, !faces.isEmpty else { return image }

        var result = image

        // 使用 SkinBeautifier 应用美颜
        result = beautifier.applyBeauty(
            to: result,
            params: params,
            faceObservations: faces
        )

        // 人像虚化
        if portraitBlur > 0.01 {
            let blurRadius = CGFloat(portraitBlur) * 20.0
            result = applyDepthBlur(to: result, blurRadius: blurRadius, faceObservations: faces)
        }

        return result
    }

    // MARK: - Face Detection

    /// 使用 Vision 框架检测人脸
    /// - Parameter image: 输入图像
    /// - Returns: 人脸检测结果数组
    func detectFaces(in image: CIImage) -> [VNFaceObservation] {
        return detectFaceObservations(in: image)
    }

    /// 内部人脸检测实现
    private func detectFaceObservations(in image: CIImage) -> [VNFaceObservation] {
        let extent = image.extent
        guard extent.width > 0, extent.height > 0 else { return [] }

        let request = VNDetectFaceRectanglesRequest()
        let handler = VNImageRequestHandler(ciImage: image, options: [:])

        do {
            try handler.perform([request])
            return request.results ?? []
        } catch {
            print("[PortraitViewModel] Face detection failed: \(error)")
            return []
        }
    }

    // MARK: - Depth Blur (Simulated)

    /// 使用高斯模糊模拟人像虚化效果
    private func applyDepthBlur(to image: CIImage, blurRadius: CGFloat, faceObservations: [VNFaceObservation]) -> CIImage {
        let extent = image.extent

        // 创建人脸区域掩码
        let renderer = UIGraphicsImageRenderer(size: extent.size)
        let uiImage = renderer.image { ctx in
            UIColor.black.setFill()
            ctx.fill(CGRect(origin: .zero, size: extent.size))

            UIColor.white.setFill()
            for face in faceObservations {
                let rect = VNImageRectForNormalizedRect(
                    face.boundingBox,
                    Int(extent.width),
                    Int(extent.height)
                )
                let expandedRect = CGRect(
                    x: rect.origin.x - rect.width * 0.15,
                    y: rect.origin.y - rect.height * 0.1,
                    width: rect.width * 1.3,
                    height: rect.height * 1.15
                )
                let path = UIBezierPath(ovalIn: expandedRect)
                path.fill()
            }
        }

        guard let cgMask = uiImage.cgImage else { return image }
        let faceMask = CIImage(cgImage: cgMask).applyingGaussianBlur(sigma: 10.0)

        // 模糊全图
        let blurred = image
            .clampedToExtent()
            .applyingGaussianBlur(sigma: Double(blurRadius))
            .cropped(to: extent)

        // 使用人脸掩码的反向：人脸区域保持清晰，背景模糊
        // 使用 blendWithMask，其中 backgroundImage = 清晰原图，inputImage = 模糊图
        // mask 为白色区域（人脸）时显示 backgroundImage（原图）
        let blend = CIFilter.blendWithMask()
        blend.inputImage = blurred
        blend.backgroundImage = image
        blend.maskImage = faceMask

        return blend.outputImage ?? image
    }

    // MARK: - Public API: 预设

    /// 应用预设
    func applyPreset(_ preset: BeautyPreset) {
        currentPreset = preset
        let params = preset.params()
        skinSmoothing = params.skinSmoothing
        skinTone = params.skinTone
        blemishRemoval = params.blemishRemoval
        eyeBrightening = params.eyeBrightening
        teethWhitening = params.teethWhitening
        faceSlimming = params.faceSlimming
    }

    /// 重置所有参数到默认值
    func reset() {
        applyPreset(.natural)
        portraitBlur = 0
        bokehParams = BokehParams()
        lightingType = .natural
        isPortraitModeEnabled = false
        isBeautyEnabled = false
        processedPreview = nil
        lastProcessedImage = nil
        lastFaceObservations = []
        faceCount = 0
        hasPortrait = false
        isProcessing = false
    }

    /// 切换人像模式
    func togglePortraitMode() {
        isPortraitModeEnabled.toggle()
    }

    /// 选择光效类型
    func selectLighting(_ type: PortraitLightingType) {
        lightingType = type
    }

    /// 更新虚化参数
    func updateBokehParams(_ params: BokehParams) {
        bokehParams = params
    }

    // MARK: - Private Helpers

    /// 从当前属性构建 BeautyParams
    private func buildBeautyParams() -> BeautyParams {
        return BeautyParams(
            skinSmoothing: skinSmoothing,
            skinTone: skinTone,
            eyeBrightening: eyeBrightening,
            teethWhitening: teethWhitening,
            faceSlimming: faceSlimming,
            blemishRemoval: blemishRemoval
        )
    }

    /// 将 CIImage 渲染为 UIImage 预览
    private func renderPreview(from image: CIImage) -> UIImage? {
        let extent = image.extent
        guard extent.width > 0, extent.height > 0 else { return nil }

        let maxDimension: CGFloat = 800
        let scale: CGFloat
        if extent.width > extent.height {
            scale = min(1.0, maxDimension / extent.width)
        } else {
            scale = min(1.0, maxDimension / extent.height)
        }

        let scaledImage: CIImage
        if scale < 1.0 {
            scaledImage = image
                .applyingFilter("CILanczosScaleTransform", parameters: [
                    kCIInputScaleKey: scale,
                    kCIInputAspectRatioKey: 1.0
                ])
        } else {
            scaledImage = image
        }

        let context = CIContext(options: [
            .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB(),
            .highQualityDownsample: true
        ])

        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else {
            return nil
        }

        return UIImage(cgImage: cgImage)
    }
}

// MARK: - Publishers.CombineLatest7

extension Publishers {
    struct CombineLatest7<A, B, C, D, E, F, G>: Publisher
    where A: Publisher, B: Publisher, C: Publisher, D: Publisher,
          E: Publisher, F: Publisher, G: Publisher,
          A.Failure == B.Failure, B.Failure == C.Failure,
          C.Failure == D.Failure, D.Failure == E.Failure,
          E.Failure == F.Failure, F.Failure == G.Failure {
        typealias Output = (A.Output, B.Output, C.Output, D.Output, E.Output, F.Output, G.Output)
        typealias Failure = A.Failure

        let a: A
        let b: B
        let c: C
        let d: D
        let e: E
        let f: F
        let g: G

        init(_ a: A, _ b: B, _ c: C, _ d: D, _ e: E, _ f: F, _ g: G) {
            self.a = a
            self.b = b
            self.c = c
            self.d = d
            self.e = e
            self.f = f
            self.g = g
        }

        func receive<S>(subscriber: S) where S: Subscriber, Failure == S.Failure, Output == S.Input {
            let combined = Publishers.CombineLatest(a, b)
            let combined2 = Publishers.CombineLatest(combined, c)
            let combined3 = Publishers.CombineLatest(combined2, d)
            let combined4 = Publishers.CombineLatest(combined3, e)
            let combined5 = Publishers.CombineLatest(combined4, f)
            let combined6 = Publishers.CombineLatest(combined5, g)

            combined6
                .map { ($0.0.0.0.0.0, $0.0.0.0.0.1, $0.0.0.0.1, $0.0.0.1, $0.0.1, $0.1, $1) }
                .receive(subscriber: subscriber)
        }
    }
}

#endif