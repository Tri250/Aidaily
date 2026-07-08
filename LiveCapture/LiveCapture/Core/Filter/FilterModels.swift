//
//  FilterModels.swift
//  LiveCapture
//
//  滤镜数据模型与预设定义
//
//  ## 文件作用
//  定义滤镜系统的所有数据模型，包括滤镜预设、分类枚举、AI 推荐结果
//  包含 12 款经典滤镜预设的完整 CIFilter 参数配置
//
//  ## 主要类型
//  - FilterCategory: 滤镜分类枚举（人像、胶片、复古、自然、美食、黑白、创意）
//  - FilterParameters: 滤镜参数结构体，定义 CIFilter 链的所有参数
//  - LutFilterPreset: 滤镜预设模型，包含名称、分类、参数配置
//  - FilterRecommendation: AI 推荐结果，包含预设、置信度、推荐理由
//  - SceneType: 场景类型枚举，用于 AI 推荐
//  - LightAnalysis: 光线分析结果，包含色温、亮度等信息
//
//  ## 12 款预设滤镜
//  1.  Doka 人像 - 暖色人像，轻微肤色柔化
//  2.  柯达 Portra 160 - 暖色调胶片，轻微褪色感
//  3.  Agfa Vista 400 - 高对比度，高饱和度
//  4.  Fuji Pro 400H - 冷调粉彩，低对比度
//  5.  Ilford HP5 - 黑白高反差，颗粒感
//  6.  Cinestill 800T - 电影感，青橙调
//  7.  徕卡经典 - 徕卡风格，浓郁暗部
//  8.  哈苏自然 - 自然色彩，准确还原
//  9.  理光正片 - 正片风格，微洋红
//  10. 宝丽来 - 拍立得，柔和褪色
//  11. 褪色记忆 - 复古褪色，暖调
//  12. 日系透明感 - 明亮通透，低对比
//
//  ## CIFilter 链
//  每个预设使用以下 CIFilter 链：
//  CITemperatureAndTint → CIExposureAdjust → CIColorControls → CIVibrance → CIHighlightShadowAdjust
//
//  ## 参数说明
//  - temperature: 色温偏移（正数=暖色，负数=冷色），单位开尔文
//  - tint: 色调偏移（正数=绿色，负数=洋红）
//  - exposure: 曝光补偿（EV）
//  - brightness: 亮度（-1 到 1）
//  - contrast: 对比度（0.25 到 4.0）
//  - saturation: 饱和度（0 到 2.0）
//  - vibrance: 自然饱和度（-1 到 1）
//  - highlightAmount: 高光调整（0 到 1.0），默认 1.0
//  - shadowAmount: 阴影调整（-1 到 1.0），默认 0.0
//  - colorMonochromeIntensity: 黑白转换强度（0 到 1.0）
//  - colorMonochromeColor: 黑白色调颜色
//

import Foundation
import CoreImage
import UIKit

#if os(iOS)

// MARK: - 滤镜分类

/// 滤镜分类枚举
enum FilterCategory: String, CaseIterable, Identifiable, Codable {
    case portrait = "人像"
    case film = "胶片"
    case vintage = "复古"
    case nature = "自然"
    case food = "美食"
    case bw = "黑白"
    case creative = "创意"

    var id: String { rawValue }

    /// 分类对应的 SF Symbol 图标
    var symbolName: String {
        switch self {
        case .portrait: return "person.crop.square"
        case .film: return "film"
        case .vintage: return "clock.arrow.circlepath"
        case .nature: return "leaf"
        case .food: return "fork.knife"
        case .bw: return "circle.lefthalf.filled"
        case .creative: return "paintpalette"
        }
    }
}

// MARK: - 滤镜参数

/// 滤镜参数结构体，定义 CIFilter 链的所有可调参数
struct FilterParameters: Codable, Equatable {
    /// 色温偏移（开尔文），正数 = 暖色，负数 = 冷色
    var temperature: Float = 0
    /// 色调偏移，正数 = 偏绿，负数 = 偏洋红
    var tint: Float = 0
    /// 曝光补偿（EV），-2 到 2
    var exposure: Float = 0
    /// 亮度，-1 到 1，默认 0
    var brightness: Float = 0
    /// 对比度，0.25 到 4.0，默认 1.0
    var contrast: Float = 1.0
    /// 饱和度，0 到 2.0，默认 1.0
    var saturation: Float = 1.0
    /// 自然饱和度，-1 到 1，默认 0
    var vibrance: Float = 0
    /// 高光调整，0.3 到 1.0，默认 1.0（值越小高光越暗）
    var highlightAmount: Float = 1.0
    /// 阴影调整，-1 到 1.0，默认 0（正数提亮阴影）
    var shadowAmount: Float = 0
    /// 是否启用黑白模式
    var isMonochrome: Bool = false
    /// 黑白模式强度，0 到 1.0
    var monochromeIntensity: Float = 0
    /// 黑白色调颜色（RGB 分量，0-1）
    var monochromeColorR: Float = 1.0
    var monochromeColorG: Float = 1.0
    var monochromeColorB: Float = 1.0

    /// 默认参数（无滤镜效果）
    static let neutral = FilterParameters()
}

// MARK: - 滤镜预设

/// 滤镜预设模型
struct LutFilterPreset: Identifiable, Codable, Equatable {
    let id: UUID
    /// 预设唯一标识名称
    let name: String
    /// UI 显示名称
    let displayName: String
    /// 滤镜分类
    let category: FilterCategory
    /// 滤镜参数配置
    let parameters: FilterParameters
    /// 预览图名称（Assets 中的图片名）
    let previewImageName: String?
    /// 默认强度
    let defaultIntensity: Float

    init(
        id: UUID = UUID(),
        name: String,
        displayName: String,
        category: FilterCategory,
        parameters: FilterParameters = .neutral,
        previewImageName: String? = nil,
        defaultIntensity: Float = 1.0
    ) {
        self.id = id
        self.name = name
        self.displayName = displayName
        self.category = category
        self.parameters = parameters
        self.previewImageName = previewImageName
        self.defaultIntensity = defaultIntensity
    }
}

// MARK: - 12 款经典滤镜预设定义

extension LutFilterPreset {

    /// 所有内置预设
    static let builtInPresets: [LutFilterPreset] = [
        .dokaPortrait,
        .kodakPortra160,
        .agfaVista400,
        .fujiPro400H,
        .ilfordHP5,
        .cinestill800T,
        .leicaClassic,
        .hasselbladNatural,
        .ricohPositive,
        .polaroid,
        .fadedMemory,
        .japaneseAiry
    ]

    // MARK: 1. Doka 人像 - 暖色人像，轻微肤色柔化

    static let dokaPortrait = LutFilterPreset(
        name: "doka_portrait",
        displayName: "Doka 人像",
        category: .portrait,
        parameters: FilterParameters(
            temperature: 800,
            tint: 5,
            exposure: 0.15,
            brightness: 0.05,
            contrast: 0.92,
            saturation: 1.05,
            vibrance: 0.08,
            highlightAmount: 0.90,
            shadowAmount: 0.12
        ),
        previewImageName: nil,
        defaultIntensity: 0.85
    )

    // MARK: 2. 柯达 Portra 160 - 暖色调胶片，轻微褪色

    static let kodakPortra160 = LutFilterPreset(
        name: "kodak_portra_160",
        displayName: "柯达 Portra 160",
        category: .film,
        parameters: FilterParameters(
            temperature: 1200,
            tint: 8,
            exposure: 0.10,
            brightness: 0.03,
            contrast: 0.88,
            saturation: 1.08,
            vibrance: 0.05,
            highlightAmount: 0.85,
            shadowAmount: 0.15
        ),
        previewImageName: nil,
        defaultIntensity: 0.9
    )

    // MARK: 3. Agfa Vista 400 - 高对比度，高饱和度

    static let agfaVista400 = LutFilterPreset(
        name: "agfa_vista_400",
        displayName: "Agfa Vista 400",
        category: .film,
        parameters: FilterParameters(
            temperature: 300,
            tint: 0,
            exposure: 0.05,
            brightness: 0.02,
            contrast: 1.25,
            saturation: 1.30,
            vibrance: 0.15,
            highlightAmount: 0.95,
            shadowAmount: -0.05
        ),
        previewImageName: nil,
        defaultIntensity: 0.9
    )

    // MARK: 4. Fuji Pro 400H - 冷调粉彩，低对比度

    static let fujiPro400H = LutFilterPreset(
        name: "fuji_pro_400h",
        displayName: "Fuji Pro 400H",
        category: .film,
        parameters: FilterParameters(
            temperature: -500,
            tint: -5,
            exposure: 0.25,
            brightness: 0.08,
            contrast: 0.82,
            saturation: 0.88,
            vibrance: -0.08,
            highlightAmount: 0.82,
            shadowAmount: 0.20
        ),
        previewImageName: nil,
        defaultIntensity: 0.85
    )

    // MARK: 5. Ilford HP5 - 黑白高反差，颗粒感

    static let ilfordHP5 = LutFilterPreset(
        name: "ilford_hp5",
        displayName: "Ilford HP5",
        category: .bw,
        parameters: FilterParameters(
            temperature: 0,
            tint: 0,
            exposure: 0,
            brightness: -0.05,
            contrast: 1.35,
            saturation: 0,
            vibrance: 0,
            highlightAmount: 1.0,
            shadowAmount: -0.10,
            isMonochrome: true,
            monochromeIntensity: 1.0,
            monochromeColorR: 0.95,
            monochromeColorG: 0.94,
            monochromeColorB: 0.92
        ),
        previewImageName: nil,
        defaultIntensity: 1.0
    )

    // MARK: 6. Cinestill 800T - 电影感，青橙调

    static let cinestill800T = LutFilterPreset(
        name: "cinestill_800t",
        displayName: "Cinestill 800T",
        category: .creative,
        parameters: FilterParameters(
            temperature: -800,
            tint: -15,
            exposure: 0,
            brightness: -0.03,
            contrast: 1.15,
            saturation: 1.20,
            vibrance: 0.10,
            highlightAmount: 0.90,
            shadowAmount: 0.05
        ),
        previewImageName: nil,
        defaultIntensity: 0.9
    )

    // MARK: 7. 徕卡经典 - 徕卡风格，浓郁暗部

    static let leicaClassic = LutFilterPreset(
        name: "leica_classic",
        displayName: "徕卡经典",
        category: .nature,
        parameters: FilterParameters(
            temperature: 200,
            tint: 3,
            exposure: -0.05,
            brightness: -0.02,
            contrast: 1.20,
            saturation: 1.10,
            vibrance: 0.05,
            highlightAmount: 0.95,
            shadowAmount: -0.08
        ),
        previewImageName: nil,
        defaultIntensity: 0.95
    )

    // MARK: 8. 哈苏自然 - 自然色彩，准确还原

    static let hasselbladNatural = LutFilterPreset(
        name: "hasselblad_natural",
        displayName: "哈苏自然",
        category: .nature,
        parameters: FilterParameters(
            temperature: 100,
            tint: 0,
            exposure: 0,
            brightness: 0,
            contrast: 1.05,
            saturation: 1.02,
            vibrance: 0.02,
            highlightAmount: 0.98,
            shadowAmount: 0.03
        ),
        previewImageName: nil,
        defaultIntensity: 1.0
    )

    // MARK: 9. 理光正片 - 正片风格，微洋红

    static let ricohPositive = LutFilterPreset(
        name: "ricoh_positive",
        displayName: "理光正片",
        category: .food,
        parameters: FilterParameters(
            temperature: 400,
            tint: -8,
            exposure: 0.10,
            brightness: 0.04,
            contrast: 1.18,
            saturation: 1.25,
            vibrance: 0.12,
            highlightAmount: 0.92,
            shadowAmount: -0.03
        ),
        previewImageName: nil,
        defaultIntensity: 0.88
    )

    // MARK: 10. 宝丽来 - 拍立得，柔和褪色

    static let polaroid = LutFilterPreset(
        name: "polaroid",
        displayName: "宝丽来",
        category: .vintage,
        parameters: FilterParameters(
            temperature: 500,
            tint: 5,
            exposure: 0.20,
            brightness: 0.10,
            contrast: 0.78,
            saturation: 0.85,
            vibrance: -0.05,
            highlightAmount: 0.75,
            shadowAmount: 0.22
        ),
        previewImageName: nil,
        defaultIntensity: 0.85
    )

    // MARK: 11. 褪色记忆 - 复古褪色，暖调

    static let fadedMemory = LutFilterPreset(
        name: "faded_memory",
        displayName: "褪色记忆",
        category: .vintage,
        parameters: FilterParameters(
            temperature: 1500,
            tint: 10,
            exposure: 0.15,
            brightness: 0.06,
            contrast: 0.75,
            saturation: 0.72,
            vibrance: -0.10,
            highlightAmount: 0.70,
            shadowAmount: 0.25
        ),
        previewImageName: nil,
        defaultIntensity: 0.9
    )

    // MARK: 12. 日系透明感 - 明亮通透，低对比

    static let japaneseAiry = LutFilterPreset(
        name: "japanese_airy",
        displayName: "日系透明感",
        category: .food,
        parameters: FilterParameters(
            temperature: -200,
            tint: -3,
            exposure: 0.35,
            brightness: 0.12,
            contrast: 0.80,
            saturation: 0.90,
            vibrance: -0.03,
            highlightAmount: 0.80,
            shadowAmount: 0.28
        ),
        previewImageName: nil,
        defaultIntensity: 0.85
    )
}

// MARK: - AI 推荐结果

/// AI 滤镜推荐结果
struct FilterRecommendation: Identifiable, Equatable {
    let id = UUID()
    /// 推荐的滤镜预设
    let preset: LutFilterPreset
    /// 推荐置信度（0-1）
    let confidence: Float
    /// 推荐理由（中文）
    let reason: String

    static func == (lhs: FilterRecommendation, rhs: FilterRecommendation) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - 场景类型

/// 场景类型枚举，用于 AI 滤镜推荐
enum SceneType: String, CaseIterable {
    case portrait = "人像"
    case food = "美食"
    case landscape = "风景"
    case nightScene = "夜景"
    case architecture = "建筑"
    case street = "街拍"
    case macro = "微距"
    case indoor = "室内"
    case sunset = "日落"
    case beach = "海滩"
    case snow = "雪景"
    case unknown = "未知"

    /// 中文描述
    var displayName: String { rawValue }
}

// MARK: - 光线分析

/// 光线分析结果
struct LightAnalysis: Equatable {
    /// 估算色温（开尔文），2000-10000
    var estimatedTemperature: Float = 6500
    /// 估算亮度（0-1）
    var estimatedBrightness: Float = 0.5
    /// 是否为暖光环境
    var isWarmLight: Bool = false
    /// 是否为冷光环境
    var isCoolLight: Bool = false
    /// 是否为低光环境
    var isLowLight: Bool = false
    /// 是否为高光环境
    var isHighLight: Bool = false
    /// 平均像素值（RGB）
    var averageR: Float = 0.5
    var averageG: Float = 0.5
    var averageB: Float = 0.5

    static let neutral = LightAnalysis()

    /// 从图像统计信息创建光线分析
    static func from(estimatedTemperature: Float, estimatedBrightness: Float) -> LightAnalysis {
        var analysis = LightAnalysis()
        analysis.estimatedTemperature = estimatedTemperature
        analysis.estimatedBrightness = estimatedBrightness
        analysis.isWarmLight = estimatedTemperature < 5000
        analysis.isCoolLight = estimatedTemperature > 7500
        analysis.isLowLight = estimatedBrightness < 0.3
        analysis.isHighLight = estimatedBrightness > 0.8
        return analysis
    }
}

#endif