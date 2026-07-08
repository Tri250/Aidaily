//
//  WatermarkModels.swift
//  LiveCapture
//
//  水印数据模型 - 配置、模板、位置
//

import Foundation
import SwiftUI

#if os(iOS)
import UIKit

// MARK: - 水印位置

enum WatermarkPosition: String, CaseIterable, Codable {
    case topLeft
    case topRight
    case bottomLeft
    case bottomRight
    case center

    var displayName: String {
        switch self {
        case .topLeft: return "左上"
        case .topRight: return "右上"
        case .bottomLeft: return "左下"
        case .bottomRight: return "右下"
        case .center: return "居中"
        }
    }

    var systemImageName: String {
        switch self {
        case .topLeft: return "rectangle.inset.topleft.filled"
        case .topRight: return "rectangle.inset.topright.filled"
        case .bottomLeft: return "rectangle.inset.bottomleft.filled"
        case .bottomRight: return "rectangle.inset.bottomright.filled"
        case .center: return "rectangle.center.inset.filled"
        }
    }
}

// MARK: - 水印样式

enum WatermarkStyle: String, CaseIterable, Codable {
    case minimal
    case classic
    case modern
    case film

    var displayName: String {
        switch self {
        case .minimal: return "极简"
        case .classic: return "经典"
        case .modern: return "现代"
        case .film: return "胶片"
        }
    }
}

// MARK: - 水印颜色（Codable 通过 hex 字符串）

struct WatermarkColor: Codable, Equatable {
    var red: Double
    var green: Double
    var blue: Double
    var alpha: Double

    init(red: Double, green: Double, blue: Double, alpha: Double = 1.0) {
        self.red = red
        self.green = green
        self.blue = blue
        self.alpha = alpha
    }

    init(hex: String, alpha: Double = 1.0) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        self.red = Double((int >> 16) & 0xFF) / 255.0
        self.green = Double((int >> 8) & 0xFF) / 255.0
        self.blue = Double(int & 0xFF) / 255.0
        self.alpha = alpha
    }

    var swiftUIColor: Color {
        Color(red: red, green: green, blue: blue, opacity: alpha)
    }

    var uiColor: UIColor {
        UIColor(red: CGFloat(red), green: CGFloat(green), blue: CGFloat(blue), alpha: CGFloat(alpha))
    }

    // 预设颜色
    static let white = WatermarkColor(hex: "FFFFFF")
    static let black = WatermarkColor(hex: "000000")
    static let gray = WatermarkColor(hex: "888888")
    static let primaryBlue = WatermarkColor(hex: "3B82F6")

    static let presetPalette: [WatermarkColor] = [.white, .black, .gray, .primaryBlue]
}

// MARK: - 水印配置

struct WatermarkConfig: Codable, Equatable {
    var isEnabled: Bool = false
    var text: String = ""
    var showDate: Bool = true
    var dateFormat: String = "yyyy-MM-dd HH:mm"
    var showEXIF: Bool = false
    var showLogo: Bool = false
    var logoImage: Data? = nil
    var position: WatermarkPosition = .bottomRight
    var fontSize: CGFloat = 14
    var textColor: WatermarkColor = .white
    var opacity: Double = 0.85
    var horizontalPadding: CGFloat = 16
    var verticalPadding: CGFloat = 16
    var style: WatermarkStyle = .minimal

    // 从 UserDefaults 加载
    static func load() -> WatermarkConfig {
        guard let data = UserDefaults.standard.data(forKey: "watermark_config"),
              let config = try? JSONDecoder().decode(WatermarkConfig.self, from: data) else {
            return WatermarkConfig()
        }
        return config
    }

    // 保存到 UserDefaults
    func save() {
        if let data = try? JSONEncoder().encode(self) {
            UserDefaults.standard.set(data, forKey: "watermark_config")
        }
    }
}

// MARK: - 水印模板

struct WatermarkTemplate: Identifiable {
    let id: String
    let name: String
    let description: String
    let systemImageName: String
    let config: WatermarkConfig

    static let allTemplates: [WatermarkTemplate] = [
        .minimalWhite,
        .filmBorder,
        .cameraParams,
        .dateStamp,
        .brandLogo
    ]

    /// 极简白
    static let minimalWhite = WatermarkTemplate(
        id: "minimal_white",
        name: "极简白",
        description: "白色半透明文字，右下角",
        systemImageName: "text.word.spacing",
        config: WatermarkConfig(
            isEnabled: true,
            text: "",
            showDate: true,
            dateFormat: "yyyy-MM-dd",
            showEXIF: false,
            showLogo: false,
            position: .bottomRight,
            fontSize: 13,
            textColor: .white,
            opacity: 0.7,
            horizontalPadding: 16,
            verticalPadding: 16,
            style: .minimal
        )
    )

    /// 胶片边框
    static let filmBorder = WatermarkTemplate(
        id: "film_border",
        name: "胶片边框",
        description: "底部黑边 + 日期和相机参数",
        systemImageName: "film",
        config: WatermarkConfig(
            isEnabled: true,
            text: "",
            showDate: true,
            dateFormat: "yyyy-MM-dd",
            showEXIF: true,
            showLogo: false,
            position: .bottomRight,
            fontSize: 11,
            textColor: .white,
            opacity: 0.9,
            horizontalPadding: 12,
            verticalPadding: 8,
            style: .film
        )
    )

    /// 摄影参数
    static let cameraParams = WatermarkTemplate(
        id: "camera_params",
        name: "摄影参数",
        description: "ISO / 光圈 / 快门 / 焦距叠加",
        systemImageName: "camera.aperture",
        config: WatermarkConfig(
            isEnabled: true,
            text: "",
            showDate: true,
            dateFormat: "yyyy-MM-dd",
            showEXIF: true,
            showLogo: false,
            position: .bottomLeft,
            fontSize: 12,
            textColor: .white,
            opacity: 0.75,
            horizontalPadding: 16,
            verticalPadding: 16,
            style: .modern
        )
    )

    /// 日期戳
    static let dateStamp = WatermarkTemplate(
        id: "date_stamp",
        name: "日期戳",
        description: "胶片风格日期戳，右下角",
        systemImageName: "calendar",
        config: WatermarkConfig(
            isEnabled: true,
            text: "",
            showDate: true,
            dateFormat: "yyyy.MM.dd",
            showEXIF: false,
            showLogo: false,
            position: .bottomRight,
            fontSize: 15,
            textColor: WatermarkColor(hex: "FF6B00"),
            opacity: 0.9,
            horizontalPadding: 20,
            verticalPadding: 20,
            style: .classic
        )
    )

    /// 品牌标识
    static let brandLogo = WatermarkTemplate(
        id: "brand_logo",
        name: "品牌标识",
        description: "品牌 logo + 文字，左下角",
        systemImageName: "signature",
        config: WatermarkConfig(
            isEnabled: true,
            text: "LiveCapture",
            showDate: false,
            dateFormat: "yyyy-MM-dd",
            showEXIF: false,
            showLogo: true,
            position: .bottomLeft,
            fontSize: 12,
            textColor: .white,
            opacity: 0.8,
            horizontalPadding: 16,
            verticalPadding: 16,
            style: .modern
        )
    )
}

// MARK: - EXIF 数据模型

struct WatermarkEXIFData {
    let iso: Float?
    let shutterSpeed: Double?
    let aperture: Double?
    let focalLength: Float?
    let lensModel: String?

    var isoText: String {
        guard let iso = iso else { return "" }
        return "ISO \(Int(iso))"
    }

    var shutterText: String {
        guard let shutter = shutterSpeed else { return "" }
        if shutter >= 1.0 {
            return "\(Int(shutter))s"
        } else {
            return "1/\(Int(1.0 / shutter))s"
        }
    }

    var apertureText: String {
        guard let aperture = aperture else { return "" }
        return "f/\(String(format: "%.1f", aperture))"
    }

    var focalLengthText: String {
        guard let focal = focalLength else { return "" }
        return "\(Int(focal))mm"
    }

    var exifSummary: String {
        var parts: [String] = []
        if !isoText.isEmpty { parts.append(isoText) }
        if !apertureText.isEmpty { parts.append(apertureText) }
        if !shutterText.isEmpty { parts.append(shutterText) }
        if !focalLengthText.isEmpty { parts.append(focalLengthText) }
        return parts.joined(separator: "  ")
    }

    /// 从 CGImageSource 提取 EXIF
    static func extract(from imageData: Data) -> WatermarkEXIFData {
        var iso: Float?
        var shutter: Double?
        var aperture: Double?
        var focalLength: Float?
        var lensModel: String?

        guard let source = CGImageSourceCreateWithData(imageData as CFData, nil),
              let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [String: Any] else {
            return WatermarkEXIFData(iso: nil, shutterSpeed: nil, aperture: nil, focalLength: nil, lensModel: nil)
        }

        // EXIF 字典
        if let exifDict = props[kCGImagePropertyExifDictionary as String] as? [String: Any] {
            if let isoValues = exifDict[kCGImagePropertyExifISOSpeedRatings as String] as? [Float] {
                iso = isoValues.first
            }
            shutter = exifDict[kCGImagePropertyExifExposureTime as String] as? Double
            aperture = exifDict[kCGImagePropertyExifFNumber as String] as? Double
            if let focal = exifDict[kCGImagePropertyExifFocalLength as String] as? Double {
                focalLength = Float(focal)
            }
            lensModel = exifDict[kCGImagePropertyExifLensModel as String] as? String
        }

        // TIFF 字典
        if let tiffDict = props[kCGImagePropertyTIFFDictionary as String] as? [String: Any] {
            if focalLength == nil, let focal = tiffDict[kCGImagePropertyTIFFFocalLength as String] as? Double {
                focalLength = Float(focal)
            }
        }

        return WatermarkEXIFData(iso: iso, shutterSpeed: shutter, aperture: aperture,
                                  focalLength: focalLength, lensModel: lensModel)
    }
}

#endif