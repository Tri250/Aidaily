//
//  AIEditViewModel.swift
//  LiveCapture
//
//  AI 编辑视图模型 - 管理生成式 AI 编辑工具的状态和交互
//
//  ## 主要功能
//  - 管理四种 AI 编辑工具的选择和切换
//  - 协调 ObjectRemover、SkyReplacer、ImageExpander、StyleTransfer 的执行
//  - 提供编辑前后的图像对比和进度跟踪
//
//  ## 编辑工具
//  - remove: 物体移除 - 涂抹区域后自动填充
//  - skyReplace: 天空替换 - 选择天空类型替换天空
//  - expand: 图像扩展 - 内容感知扩展画布
//  - styleTransfer: 风格迁移 - 应用艺术风格滤镜
//
//  ## 使用方式
//  1. 设置 sourceImage
//  2. 选择 selectedTool
//  3. 配置工具参数
//  4. 调用 applyEdit() 执行编辑
//  5. 观察 editedImage 获取结果
//

import Foundation
import Combine
import CoreImage
import UIKit

#if os(iOS)

/// AI 编辑视图模型
final class AIEditViewModel: ObservableObject {

    // MARK: - 发布属性

    /// 源图像（编辑前）
    @Published var sourceImage: UIImage?

    /// 编辑后图像
    @Published var editedImage: UIImage?

    /// 是否正在处理
    @Published var isProcessing = false

    /// 当前选中的编辑工具
    @Published var selectedTool: AIEditTool = .remove

    // MARK: - 物体移除参数

    /// 移除区域掩码矩形（归一化坐标 0...1）
    @Published var maskRect: CGRect?

    // MARK: - 天空替换参数

    /// 选择的天空类型
    @Published var selectedSkyType: SkyReplacer.SkyType = .sunset

    // MARK: - 风格迁移参数

    /// 选择的艺术风格
    @Published var selectedStyle: StyleTransfer.ArtStyle = .watercolor

    /// 风格强度（0.0 = 原图，1.0 = 完全风格化）
    @Published var styleIntensity: Float = 0.7

    // MARK: - 图像扩展参数

    /// 每边扩展像素数
    @Published var expandAmount: CGFloat = 100

    /// 扩展方向
    @Published var expandDirection: ImageExpander.ExpansionDirection = .all

    // MARK: - 进度

    /// 当前进度（0.0 - 1.0）
    @Published var progress: Float = 0

    // MARK: - 错误信息

    @Published var errorMessage: String?

    // MARK: - 编辑工具枚举

    enum AIEditTool: String, CaseIterable {
        case remove
        case skyReplace
        case expand
        case styleTransfer

        var displayName: String {
            switch self {
            case .remove: return "物体移除"
            case .skyReplace: return "天空替换"
            case .expand: return "图像扩展"
            case .styleTransfer: return "风格迁移"
            }
        }

        var iconName: String {
            switch self {
            case .remove: return "wand.and.stars"
            case .skyReplace: return "cloud.sun.fill"
            case .expand: return "arrow.up.left.and.arrow.down.right"
            case .styleTransfer: return "paintpalette.fill"
            }
        }
    }

    // MARK: - 私有属性

    private let objectRemover = ObjectRemover()
    private let skyReplacer = SkyReplacer()
    private let imageExpander = ImageExpander()
    private let styleTransfer = StyleTransfer()

    private let context: CIContext
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!

    private var cancellables = Set<AnyCancellable>()

    // MARK: - 初始化

    init() {
        if let device = MTLCreateSystemDefaultDevice() {
            context = CIContext(mtlDevice: device, options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "AIEditViewModel"
            ])
        } else {
            context = CIContext(options: [
                .workingColorSpace: colorSpace,
                .outputColorSpace: colorSpace,
                .name: "AIEditViewModel"
            ])
        }

        setupBindings()
    }

    /// 设置数据绑定
    private func setupBindings() {
        // 监听物体移除器进度
        objectRemover.$progress
            .receive(on: DispatchQueue.main)
            .sink { [weak self] progress in
                self?.progress = progress
            }
            .store(in: &cancellables)

        // 监听图像扩展器进度
        imageExpander.$progress
            .receive(on: DispatchQueue.main)
            .sink { [weak self] progress in
                self?.progress = progress
            }
            .store(in: &cancellables)
    }

    // MARK: - 编辑执行

    /// 应用当前选中的编辑
    func applyEdit() async {
        guard let image = sourceImage else {
            await MainActor.run {
                errorMessage = "请先选择一张图片"
            }
            return
        }

        await MainActor.run {
            isProcessing = true
            progress = 0
            errorMessage = nil
        }

        let ciImage: CIImage
        if let ci = image.ciImage {
            ciImage = ci
        } else if let cgImage = image.cgImage {
            ciImage = CIImage(cgImage: cgImage)
        } else {
            await MainActor.run {
                isProcessing = false
                errorMessage = "无法加载图片数据"
            }
            return
        }

        let result: CIImage?

        switch selectedTool {
        case .remove:
            result = await applyRemoveTool(ciImage: ciImage, imageSize: image.size)

        case .skyReplace:
            result = await applySkyReplaceTool(ciImage: ciImage)

        case .expand:
            result = await applyExpandTool(ciImage: ciImage)

        case .styleTransfer:
            result = await applyStyleTransferTool(ciImage: ciImage)
        }

        if let result = result {
            let rendered = renderToUIImage(result)
            await MainActor.run {
                editedImage = rendered
                isProcessing = false
                progress = 1.0
            }
        } else {
            await MainActor.run {
                isProcessing = false
                progress = 0
                errorMessage = "编辑处理失败，请重试"
            }
        }
    }

    // MARK: - 物体移除

    private func applyRemoveTool(ciImage: CIImage, imageSize: CGSize) async -> CIImage? {
        guard let rect = maskRect, rect.width > 0, rect.height > 0 else {
            await MainActor.run {
                errorMessage = "请先在图片上涂抹要移除的物体区域"
            }
            return nil
        }

        return await objectRemover.removeObject(
            from: ciImage,
            maskRect: rect,
            imageSize: imageSize
        )
    }

    // MARK: - 天空替换

    private func applySkyReplaceTool(ciImage: CIImage) async -> CIImage? {
        return await skyReplacer.replaceSky(
            in: ciImage,
            with: selectedSkyType
        )
    }

    // MARK: - 图像扩展

    private func applyExpandTool(ciImage: CIImage) async -> CIImage? {
        guard expandAmount > 0 else {
            await MainActor.run {
                errorMessage = "扩展像素数必须大于 0"
            }
            return nil
        }

        return await imageExpander.expandImage(
            ciImage,
            expandBy: expandAmount,
            direction: expandDirection
        )
    }

    // MARK: - 风格迁移

    private func applyStyleTransferTool(ciImage: CIImage) async -> CIImage? {
        let styled = styleTransfer.applyStyle(
            to: ciImage,
            style: selectedStyle,
            intensity: styleIntensity
        )
        return styled
    }

    // MARK: - 图像渲染

    /// 将 CIImage 渲染为 UIImage
    private func renderToUIImage(_ ciImage: CIImage) -> UIImage? {
        let extent = ciImage.extent
        guard extent.width > 0 && extent.height > 0,
              extent.width < 50000 && extent.height < 50000,
              let cgImage = context.createCGImage(ciImage, from: extent) else {
            return nil
        }
        return UIImage(cgImage: cgImage)
    }

    // MARK: - 快捷操作方法

    /// 快速应用物体移除
    func quickRemove(rect: CGRect) async {
        maskRect = rect
        selectedTool = .remove
        await applyEdit()
    }

    /// 快速应用天空替换
    func quickSkyReplace(type: SkyReplacer.SkyType) async {
        selectedSkyType = type
        selectedTool = .skyReplace
        await applyEdit()
    }

    /// 快速应用图像扩展
    func quickExpand(by pixels: CGFloat, direction: ImageExpander.ExpansionDirection = .all) async {
        expandAmount = pixels
        expandDirection = direction
        selectedTool = .expand
        await applyEdit()
    }

    /// 快速应用风格迁移
    func quickStyleTransfer(style: StyleTransfer.ArtStyle, intensity: Float = 0.7) async {
        selectedStyle = style
        styleIntensity = intensity
        selectedTool = .styleTransfer
        await applyEdit()
    }

    // MARK: - 重置

    /// 重置所有编辑状态
    func reset() {
        editedImage = nil
        maskRect = nil
        errorMessage = nil
        isProcessing = false
        progress = 0
    }

    /// 将编辑结果设为源图像（继续编辑）
    func applyAndContinue() {
        if let edited = editedImage {
            sourceImage = edited
            editedImage = nil
            maskRect = nil
        }
    }

    /// 撤销编辑（恢复源图像）
    func undo() {
        editedImage = nil
        maskRect = nil
        errorMessage = nil
    }

    // MARK: - 状态查询

    /// 是否有编辑结果
    var hasEditResult: Bool {
        editedImage != nil
    }

    /// 是否可以进行物体移除（已设置移除区域）
    var canRemove: Bool {
        guard let rect = maskRect else { return false }
        return rect.width > 0 && rect.height > 0
    }

    /// 当前工具的描述
    var currentToolDescription: String {
        switch selectedTool {
        case .remove:
            return "在图片上涂抹想要移除的物体区域"
        case .skyReplace:
            return "选择一种天空类型替换当前天空"
        case .expand:
            return "设置扩展像素数，向外扩展画布"
        case .styleTransfer:
            return "选择一种艺术风格应用到图片"
        }
    }
}

// MARK: - Metal 导入

import Metal

#endif