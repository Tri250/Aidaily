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
//  - beautyParams: 美颜参数
//  - bokehParams: 虚化参数
//  - lightingType: 当前光效类型
//  - isPortraitModeEnabled: 人像模式开关
//  - processedPreview: 处理后的预览图像
//  - isProcessing: 是否正在处理
//  - faceCount: 检测到的人脸数量
//  - hasPortrait: 是否检测到人像
//
//  ## 核心方法
//  - processImage(_:): 执行完整的人像效果处理管线
//  - reset(): 重置所有参数到默认值
//
//  ## 处理管线
//  1. 检测人像（人脸检测）
//  2. 应用虚化（Bokeh）
//  3. 应用美颜（Beauty）
//  4. 应用光效（Lighting）
//  5. 生成预览
//

import Foundation
import Combine
import CoreImage
import Vision

#if os(iOS)
import UIKit

/// 人像模式视图模型
final class PortraitViewModel: ObservableObject {
    // MARK: - Published State

    /// 美颜参数
    @Published var beautyParams = BeautyParams.default
    /// 虚化参数
    @Published var bokehParams = BokehParams()
    /// 当前光效类型
    @Published var lightingType: PortraitLightingType = .natural
    /// 人像模式开关
    @Published var isPortraitModeEnabled: Bool = false
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
        // 监听参数变化，自动重新处理
        $beautyParams
            .dropFirst()
            .debounce(for: .milliseconds(100), scheduler: DispatchQueue.main)
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
        guard isPortraitModeEnabled else { return }
        guard !isProcessing else { return }

        isProcessing = true
        let processingParams = (beautyParams, bokehParams, lightingType)

        processingQueue.async { [weak self] in
            guard let self else { return }

            // 1. 检测人像
            let portraitResult = self.engine.detectPortrait(in: image)

            let faceObservations = self.extractFaceObservations(from: portraitResult)
            let hasFace = !faceObservations.isEmpty

            DispatchQueue.main.async {
                self.faceCount = faceObservations.count
                self.hasPortrait = hasFace
            }

            // 2. 应用效果管线
            var result = image

            // 2a. 背景虚化
            if processingParams.2.intensity > 0.01 && hasFace {
                result = self.engine.applyBokeh(
                    to: result,
                    params: processingParams.2,
                    depthData: portraitResult.depthData,
                    faceObservations: faceObservations
                )
            }

            // 2b. 美颜
            if !processingParams.0.isOff && hasFace {
                result = self.beautifier.applyBeauty(
                    to: result,
                    params: processingParams.0,
                    faceObservations: faceObservations
                )
            }

            // 2c. 光效
            if processingParams.1 != .natural && hasFace {
                result = self.engine.applyLighting(
                    to: result,
                    type: processingParams.1,
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

    // MARK: - Public API

    /// 重置所有参数到默认值
    func reset() {
        beautyParams = .default
        bokehParams = BokehParams()
        lightingType = .natural
        isPortraitModeEnabled = false
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

    /// 更新美颜参数
    func updateBeautyParams(_ params: BeautyParams) {
        beautyParams = params
    }

    /// 更新虚化参数
    func updateBokehParams(_ params: BokehParams) {
        bokehParams = params
    }

    // MARK: - Private Helpers

    /// 从 PortraitResult 中提取 VNFaceObservation
    private func extractFaceObservations(from result: PortraitResult) -> [VNFaceObservation] {
        guard result.hasPortrait, result.faceLandmarks.count > 3 else {
            return []
        }

        // 基于 landmark 点重建 face observation
        // 从 landmarks 推算边界框
        let points = result.faceLandmarks
        let extent = result.originalImage.extent

        let minX = points.map(\.x).min() ?? 0
        let maxX = points.map(\.x).max() ?? extent.width
        let minY = points.map(\.y).min() ?? 0
        let maxY = points.map(\.y).max() ?? extent.height

        // 归一化到 [0, 1]
        let normalizedRect = CGRect(
            x: (minX - extent.width * 0.05) / extent.width,
            y: (minY - extent.height * 0.05) / extent.height,
            width: (maxX - minX + extent.width * 0.1) / extent.width,
            height: (maxY - minY + extent.height * 0.1) / extent.height
        )

        let observation = VNFaceObservation(boundingBox: normalizedRect)
        return [observation]
    }

    /// 将 CIImage 渲染为 UIImage 预览
    private func renderPreview(from image: CIImage) -> UIImage? {
        let extent = image.extent
        guard extent.width > 0, extent.height > 0 else { return nil }

        // 限制预览尺寸以提升性能
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
            .workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!,
            .highQualityDownsample: true
        ])

        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else {
            return nil
        }

        return UIImage(cgImage: cgImage)
    }
}

#endif