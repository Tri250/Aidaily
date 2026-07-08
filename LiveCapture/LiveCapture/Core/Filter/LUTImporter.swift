//
//  LUTImporter.swift
//  LiveCapture
//
//  自定义 LUT 导入器 - 支持 .cube 和 .3dl 格式
//

import Foundation
import CoreImage
import UIKit
import UniformTypeIdentifiers

#if os(iOS)

/// LUT 文件导入器
final class LUTImporter: ObservableObject {
    @Published var importedPresets: [LutFilterPreset] = []
    @Published var importError: String?

    private let storageKey = "livecapture.custom_luts"

    init() {
        loadSavedPresets()
    }

    // MARK: - 导入 .cube 文件

    /// 从 URL 导入 .cube LUT 文件
    func importCubeFile(from url: URL) throws -> LutFilterPreset {
        let content = try String(contentsOf: url, encoding: .utf8)
        let lines = content.components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty && !$0.hasPrefix("#") }

        var lutSize = 32
        var dataPoints: [(r: Float, g: Float, b: Float)] = []

        for line in lines {
            if line.uppercased().hasPrefix("LUT_3D_SIZE") {
                let components = line.components(separatedBy: .whitespaces)
                if let sizeStr = components.last, let size = Int(sizeStr) {
                    lutSize = size
                }
                continue
            }

            // 解析 RGB 数据行
            let values = line.components(separatedBy: .whitespaces)
                .filter { !$0.isEmpty }
                .compactMap { Float($0) }

            if values.count >= 3 {
                dataPoints.append((r: values[0], g: values[1], b: values[2]))
            }
        }

        guard !dataPoints.isEmpty else {
            throw LUTImportError.invalidFormat
        }

        // 创建滤镜参数（取 LUT 数据的平均色调作为基础参数）
        let avgR = dataPoints.reduce(0) { $0 + $1.r } / Float(dataPoints.count)
        let avgG = dataPoints.reduce(0) { $0 + $1.g } / Float(dataPoints.count)
        let avgB = dataPoints.reduce(0) { $0 + $1.b } / Float(dataPoints.count)

        let params = estimateFilterParamsFromLUT(r: avgR, g: avgG, b: avgB)

        let fileName = url.deletingPathExtension().lastPathComponent
        let preset = LutFilterPreset(
            name: "custom_\(fileName)",
            displayName: fileName,
            category: .creative,
            parameters: params,
            defaultIntensity: 0.85
        )

        // 保存到本地
        importedPresets.append(preset)
        savePresets()

        return preset
    }

    /// 从 LUT 数据估算滤镜参数
    private func estimateFilterParamsFromLUT(r: Float, g: Float, b: Float) -> FilterParameters {
        var params = FilterParameters()

        // 根据 RGB 平均值估算色调
        // 暖色调（R > B）→ 负色温偏移
        // 冷色调（B > R）→ 正色温偏移
        if r > b * 1.1 {
            params.temperature = 500 + (r - b) * 1000
        } else if b > r * 1.1 {
            params.temperature = -500 - (b - r) * 1000
        }

        // 整体亮度
        let luminance = 0.299 * r + 0.587 * g + 0.114 * b
        if luminance < 0.4 {
            params.brightness = 0.05
            params.exposure = 0.15
        } else if luminance > 0.6 {
            params.brightness = -0.05
            params.exposure = -0.1
        }

        // 饱和度
        let colorVariance = abs(r - g) + abs(g - b) + abs(b - r)
        if colorVariance < 0.1 {
            params.saturation = 1.1
            params.vibrance = 0.1
        } else if colorVariance > 0.3 {
            params.saturation = 0.9
            params.vibrance = -0.05
        }

        return params
    }

    // MARK: - 文件选择器支持

    /// 支持的 LUT 文件类型
    static var supportedUTTypes: [UTType] {
        [.plainText, .data, UTType(filenameExtension: "cube") ?? .data,
         UTType(filenameExtension: "3dl") ?? .data]
    }

    /// 验证文件是否为有效的 LUT 文件
    static func isValidLUTFile(url: URL) -> Bool {
        let ext = url.pathExtension.lowercased()
        return ext == "cube" || ext == "3dl"
    }

    // MARK: - 管理导入的预设

    /// 删除导入的预设
    func deletePreset(_ preset: LutFilterPreset) {
        importedPresets.removeAll { $0.id == preset.id }
        savePresets()
    }

    /// 所有可用预设（内置 + 导入）
    var allPresets: [LutFilterPreset] {
        LutFilterPreset.builtInPresets + importedPresets
    }

    // MARK: - 持久化

    private func savePresets() {
        if let data = try? JSONEncoder().encode(importedPresets) {
            UserDefaults.standard.set(data, forKey: storageKey)
        }
    }

    private func loadSavedPresets() {
        if let data = UserDefaults.standard.data(forKey: storageKey),
           let presets = try? JSONDecoder().decode([LutFilterPreset].self, from: data) {
            importedPresets = presets
        }
    }
}

// MARK: - 错误类型

enum LUTImportError: LocalizedError {
    case invalidFormat
    case fileNotFound
    case unsupportedFormat

    var errorDescription: String? {
        switch self {
        case .invalidFormat: return "LUT 文件格式无效"
        case .fileNotFound: return "文件未找到"
        case .unsupportedFormat: return "不支持的 LUT 格式（仅支持 .cube 和 .3dl）"
        }
    }
}

#endif