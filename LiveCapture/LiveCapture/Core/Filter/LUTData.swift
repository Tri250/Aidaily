//
//  LUTData.swift
//  LiveCapture
//
//  滤镜 LUT 数据 - 30+ 款内置滤镜预设的完整参数定义
//
//  ## 文件作用
//  包含所有内置滤镜预设的真实 CIFilter 参数配置
//  每个预设使用 CIFilter 链 + 色调曲线 + 颜色矩阵产生可见的差异化效果
//
//  ## 滤镜分类
//  - 胶片系列 (Film): 6 款
//  - 日系系列 (Japanese): 5 款
//  - 港风系列 (HK Style): 4 款
//  - 黑白系列 (B&W): 4 款
//  - 人像系列 (Portrait): 4 款
//  - 风光系列 (Landscape): 4 款
//  - 美食系列 (Food): 3 款
//
//  ## 技术实现
//  - CITemperatureAndTint: 色温色调
//  - CIExposureAdjust: 曝光
//  - CIColorControls: 亮度/对比度/饱和度
//  - CIVibrance: 自然饱和度
//  - CIHighlightShadowAdjust: 高光/阴影
//  - CIColorMonochrome: 黑白转换
//  - CIToneCurve: RGB 色调曲线
//  - CIColorMatrix: 颜色矩阵变换
//

import Foundation
import CoreImage

#if os(iOS)

// MARK: - 30+ 款内置滤镜预设

extension LutFilterPreset {

    /// 所有内置预设（新 30+ 款 + 经典 12 款）
    static let allBuiltInPresets: [LutFilterPreset] = {
        FilmPresets.all + JapanesePresets.all + HKStylePresets.all +
        BWPresets.all + PortraitPresets.all + LandscapePresets.all + FoodPresets.all +
        ClassicPresets.all
    }()

    /// 兼容旧 API：返回所有内置预设
    static let builtInPresets: [LutFilterPreset] = allBuiltInPresets

    // MARK: - 分类获取方法

    /// 按分类获取预设
    static func presetsForCategory(_ category: FilterCategory) -> [LutFilterPreset] {
        allBuiltInPresets.filter { $0.category == category }
    }

    /// 所有分类及其预设数量
    static var categoryStats: [(FilterCategory, Int)] {
        let map = Dictionary(grouping: allBuiltInPresets, by: { $0.category })
        return FilterCategory.allCases.map { cat in
            (cat, map[cat]?.count ?? 0)
        }
    }
}

// MARK: - 胶片系列 (Film)

private enum FilmPresets {
    static let all: [LutFilterPreset] = [
        fujiClassic, kodakGold, agfaVista, cinemaFilm, filmFade, filmGrain
    ]

    /// 富士经典 - 经典富士胶片风格：冷调粉彩，柔和对比，淡雅色调
    static let fujiClassic = LutFilterPreset(
        name: "fuji_classic",
        displayName: "富士经典",
        category: .film,
        parameters: FilterParameters(
            temperature: -400,
            tint: -6,
            exposure: 0.18,
            brightness: 0.06,
            contrast: 0.84,
            saturation: 0.90,
            vibrance: -0.06,
            highlightAmount: 0.80,
            shadowAmount: 0.18,
            useToneCurve: true,
            toneCurveR: [0.0, 0.22, 0.48, 0.74, 1.0],
            toneCurveG: [0.0, 0.26, 0.52, 0.78, 1.0],
            toneCurveB: [0.0, 0.20, 0.44, 0.72, 0.98]
        ),
        defaultIntensity: 0.85,
        description: "经典富士胶片风格，冷调粉彩，柔和对比，淡雅色调"
    )

    /// 柯达金 - 柯达金胶卷风格：温暖金色，适中对饱和，暖调高光
    static let kodakGold = LutFilterPreset(
        name: "kodak_gold",
        displayName: "柯达金",
        category: .film,
        parameters: FilterParameters(
            temperature: 1500,
            tint: 10,
            exposure: 0.08,
            brightness: 0.04,
            contrast: 1.10,
            saturation: 1.15,
            vibrance: 0.10,
            highlightAmount: 0.85,
            shadowAmount: 0.08,
            useToneCurve: true,
            toneCurveR: [0.0, 0.28, 0.54, 0.80, 1.0],
            toneCurveG: [0.0, 0.24, 0.50, 0.76, 1.0],
            toneCurveB: [0.0, 0.18, 0.42, 0.68, 0.96]
        ),
        defaultIntensity: 0.88,
        description: "柯达金胶卷风格，温暖金色调，适中对饱和，复古暖调高光"
    )

    /// 爱克发 - Agfa 胶片风格：高对比度，高饱和度，色彩浓郁
    static let agfaVista = LutFilterPreset(
        name: "agfa_film",
        displayName: "爱克发",
        category: .film,
        parameters: FilterParameters(
            temperature: 400,
            tint: 3,
            exposure: 0.03,
            brightness: 0.01,
            contrast: 1.28,
            saturation: 1.35,
            vibrance: 0.18,
            highlightAmount: 0.92,
            shadowAmount: -0.06,
            useToneCurve: true,
            toneCurveR: [0.0, 0.20, 0.46, 0.72, 1.0],
            toneCurveG: [0.0, 0.22, 0.48, 0.74, 1.0],
            toneCurveB: [0.0, 0.24, 0.50, 0.76, 1.0]
        ),
        defaultIntensity: 0.90,
        description: "Agfa 胶片风格，高对比度，高饱和度，色彩浓郁鲜明"
    )

    /// 电影卷 - 电影胶片风格：青橙色调，适中对对比，电影感
    static let cinemaFilm = LutFilterPreset(
        name: "cinema_film",
        displayName: "电影卷",
        category: .film,
        parameters: FilterParameters(
            temperature: -600,
            tint: -12,
            exposure: 0.0,
            brightness: -0.02,
            contrast: 1.18,
            saturation: 1.22,
            vibrance: 0.12,
            highlightAmount: 0.88,
            shadowAmount: 0.04,
            useColorMatrix: true,
            colorMatrixRR: 1.15, colorMatrixRG: 0.0, colorMatrixRB: 0.0, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 1.10, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.0, colorMatrixBG: 0.0, colorMatrixBB: 0.95, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.88,
        description: "电影胶片风格，青橙色调，适中对比度，电影感十足"
    )

    /// 胶片褪色 - 复古褪色胶片：暖色，低饱和，低对比，褪色感
    static let filmFade = LutFilterPreset(
        name: "film_fade",
        displayName: "胶片褪色",
        category: .film,
        parameters: FilterParameters(
            temperature: 1200,
            tint: 8,
            exposure: 0.22,
            brightness: 0.10,
            contrast: 0.72,
            saturation: 0.68,
            vibrance: -0.12,
            highlightAmount: 0.68,
            shadowAmount: 0.28,
            useToneCurve: true,
            toneCurveR: [0.0, 0.30, 0.55, 0.80, 1.0],
            toneCurveG: [0.0, 0.28, 0.52, 0.78, 1.0],
            toneCurveB: [0.0, 0.22, 0.45, 0.72, 0.98]
        ),
        defaultIntensity: 0.85,
        description: "复古褪色胶片风格，暖色调，低饱和，低对比，褪色质感"
    )

    /// 胶片颗粒 - 胶片颗粒感风格：暖色，适中对饱和，颗粒氛围
    static let filmGrain = LutFilterPreset(
        name: "film_grain",
        displayName: "胶片颗粒",
        category: .film,
        parameters: FilterParameters(
            temperature: 800,
            tint: 5,
            exposure: -0.05,
            brightness: -0.03,
            contrast: 1.15,
            saturation: 1.05,
            vibrance: 0.05,
            highlightAmount: 0.90,
            shadowAmount: -0.04,
            useToneCurve: true,
            toneCurveR: [0.0, 0.18, 0.44, 0.70, 1.0],
            toneCurveG: [0.0, 0.20, 0.46, 0.72, 1.0],
            toneCurveB: [0.0, 0.22, 0.48, 0.74, 1.0]
        ),
        defaultIntensity: 0.90,
        description: "胶片颗粒感风格，暖色调，颗粒质感，经典胶片氛围"
    )
}

// MARK: - 日系系列 (Japanese)

private enum JapanesePresets {
    static let all: [LutFilterPreset] = [
        japaneseFresh, japaneseWarmSun, japanesePastel, japaneseForest, japaneseOkinawa
    ]

    /// 小清新 - 日系小清新：明亮通透，低对比，微冷色调
    static let japaneseFresh = LutFilterPreset(
        name: "japanese_fresh",
        displayName: "小清新",
        category: .japanese,
        parameters: FilterParameters(
            temperature: -250,
            tint: -4,
            exposure: 0.35,
            brightness: 0.14,
            contrast: 0.78,
            saturation: 0.88,
            vibrance: -0.04,
            highlightAmount: 0.76,
            shadowAmount: 0.30,
            useToneCurve: true,
            toneCurveR: [0.0, 0.38, 0.62, 0.84, 1.0],
            toneCurveG: [0.0, 0.36, 0.60, 0.82, 1.0],
            toneCurveB: [0.0, 0.34, 0.58, 0.80, 1.0]
        ),
        defaultIntensity: 0.82,
        description: "日系小清新风格，明亮通透，低对比，微冷色调"
    )

    /// 日系暖阳 - 日系暖阳风格：温暖明亮，柔和高光，空气感
    static let japaneseWarmSun = LutFilterPreset(
        name: "japanese_warm_sun",
        displayName: "日系暖阳",
        category: .japanese,
        parameters: FilterParameters(
            temperature: 600,
            tint: 6,
            exposure: 0.28,
            brightness: 0.10,
            contrast: 0.82,
            saturation: 0.95,
            vibrance: 0.06,
            highlightAmount: 0.78,
            shadowAmount: 0.25,
            useToneCurve: true,
            toneCurveR: [0.0, 0.35, 0.58, 0.82, 1.0],
            toneCurveG: [0.0, 0.32, 0.55, 0.80, 1.0],
            toneCurveB: [0.0, 0.28, 0.52, 0.78, 1.0]
        ),
        defaultIntensity: 0.85,
        description: "日系暖阳风格，温暖明亮，柔和高光，空气感十足"
    )

    /// 和风淡彩 - 和风淡彩风格：低饱和，柔和色调，淡雅
    static let japanesePastel = LutFilterPreset(
        name: "japanese_pastel",
        displayName: "和风淡彩",
        category: .japanese,
        parameters: FilterParameters(
            temperature: 200,
            tint: -2,
            exposure: 0.20,
            brightness: 0.08,
            contrast: 0.85,
            saturation: 0.78,
            vibrance: -0.08,
            highlightAmount: 0.82,
            shadowAmount: 0.20,
            useColorMatrix: true,
            colorMatrixRR: 0.95, colorMatrixRG: 0.05, colorMatrixRB: 0.0, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 0.92, colorMatrixGB: 0.08, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.0, colorMatrixBG: 0.0, colorMatrixBB: 0.90, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.85,
        description: "和风淡彩风格，低饱和，柔和色调，淡雅清新"
    )

    /// 森系 - 森系风格：绿色调，自然感，柔和
    static let japaneseForest = LutFilterPreset(
        name: "japanese_forest",
        displayName: "森系",
        category: .japanese,
        parameters: FilterParameters(
            temperature: -100,
            tint: -8,
            exposure: 0.12,
            brightness: 0.04,
            contrast: 0.90,
            saturation: 0.92,
            vibrance: 0.08,
            highlightAmount: 0.85,
            shadowAmount: 0.15,
            useColorMatrix: true,
            colorMatrixRR: 0.92, colorMatrixRG: 0.0, colorMatrixRB: 0.0, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 1.10, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.0, colorMatrixBG: 0.0, colorMatrixBB: 0.88, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.85,
        description: "森系风格，绿色调，自然感，柔和清新"
    )

    /// 冲绳蓝 - 冲绳蓝风格：明亮蓝色，通透，高对比
    static let japaneseOkinawa = LutFilterPreset(
        name: "japanese_okinawa",
        displayName: "冲绳蓝",
        category: .japanese,
        parameters: FilterParameters(
            temperature: -600,
            tint: -10,
            exposure: 0.15,
            brightness: 0.05,
            contrast: 1.08,
            saturation: 1.15,
            vibrance: 0.10,
            highlightAmount: 0.88,
            shadowAmount: 0.10,
            useColorMatrix: true,
            colorMatrixRR: 0.90, colorMatrixRG: 0.0, colorMatrixRB: 0.0, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 1.05, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.0, colorMatrixBG: 0.0, colorMatrixBB: 1.20, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.88,
        description: "冲绳蓝风格，明亮蓝色，通透感，高对比"
    )
}

// MARK: - 港风系列 (HK Style)

private enum HKStylePresets {
    static let all: [LutFilterPreset] = [
        hkRetro, hkNeon, hkWongKarWai, hkKowloon
    ]

    /// 港风复古 - 港风复古风格：暖色，微褪色，怀旧感
    static let hkRetro = LutFilterPreset(
        name: "hk_retro",
        displayName: "港风复古",
        category: .hkStyle,
        parameters: FilterParameters(
            temperature: 1800,
            tint: 12,
            exposure: 0.05,
            brightness: 0.02,
            contrast: 1.05,
            saturation: 1.08,
            vibrance: 0.08,
            highlightAmount: 0.82,
            shadowAmount: 0.12,
            useToneCurve: true,
            toneCurveR: [0.0, 0.30, 0.56, 0.82, 1.0],
            toneCurveG: [0.0, 0.26, 0.50, 0.76, 1.0],
            toneCurveB: [0.0, 0.20, 0.42, 0.68, 0.96]
        ),
        defaultIntensity: 0.88,
        description: "港风复古风格，暖色微褪色，怀旧感，经典港片色调"
    )

    /// 霓虹夜色 - 霓虹夜色风格：青品色，高对比，赛博朋克
    static let hkNeon = LutFilterPreset(
        name: "hk_neon",
        displayName: "霓虹夜色",
        category: .hkStyle,
        parameters: FilterParameters(
            temperature: -1200,
            tint: -20,
            exposure: -0.10,
            brightness: -0.05,
            contrast: 1.35,
            saturation: 1.25,
            vibrance: 0.15,
            highlightAmount: 0.82,
            shadowAmount: -0.10,
            useColorMatrix: true,
            colorMatrixRR: 1.10, colorMatrixRG: 0.0, colorMatrixRB: 0.05, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 0.95, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.05, colorMatrixBG: 0.0, colorMatrixBB: 1.15, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.90,
        description: "霓虹夜色风格，青品色调，高对比，赛博朋克氛围"
    )

    /// 王家卫 - 王家卫电影风格：绿色调，梦幻，低对比
    static let hkWongKarWai = LutFilterPreset(
        name: "hk_wong_kar_wai",
        displayName: "王家卫",
        category: .hkStyle,
        parameters: FilterParameters(
            temperature: 300,
            tint: -15,
            exposure: 0.10,
            brightness: 0.04,
            contrast: 0.82,
            saturation: 0.90,
            vibrance: -0.05,
            highlightAmount: 0.78,
            shadowAmount: 0.18,
            useColorMatrix: true,
            colorMatrixRR: 0.88, colorMatrixRG: 0.0, colorMatrixRB: 0.0, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 1.12, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.0, colorMatrixBG: 0.0, colorMatrixBB: 0.85, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.88,
        description: "王家卫电影风格，绿色调，梦幻柔焦，低对比"
    )

    /// 九龙城寨 - 九龙城寨风格：暗沉，高对比，冷色调
    static let hkKowloon = LutFilterPreset(
        name: "hk_kowloon",
        displayName: "九龙城寨",
        category: .hkStyle,
        parameters: FilterParameters(
            temperature: -500,
            tint: -8,
            exposure: -0.20,
            brightness: -0.08,
            contrast: 1.40,
            saturation: 0.85,
            vibrance: -0.05,
            highlightAmount: 0.88,
            shadowAmount: -0.12,
            useToneCurve: true,
            toneCurveR: [0.0, 0.15, 0.38, 0.65, 0.95],
            toneCurveG: [0.0, 0.16, 0.40, 0.68, 0.95],
            toneCurveB: [0.0, 0.18, 0.42, 0.70, 0.95]
        ),
        defaultIntensity: 0.90,
        description: "九龙城寨风格，暗沉冷色调，高对比，压抑氛围"
    )
}

// MARK: - 黑白系列 (B&W)

private enum BWPresets {
    static let all: [LutFilterPreset] = [
        bwHighContrast, bwSoft, bwWarm, bwCool
    ]

    /// 高对比黑白 - 高对比度黑白：强对比，中性色调
    static let bwHighContrast = LutFilterPreset(
        name: "bw_high_contrast",
        displayName: "高对比黑白",
        category: .bw,
        parameters: FilterParameters(
            temperature: 0, tint: 0,
            exposure: 0.0,
            brightness: -0.06,
            contrast: 1.45,
            saturation: 0.0,
            vibrance: 0.0,
            highlightAmount: 1.0,
            shadowAmount: -0.12,
            isMonochrome: true,
            monochromeIntensity: 1.0,
            monochromeColorR: 0.95, monochromeColorG: 0.94, monochromeColorB: 0.92
        ),
        defaultIntensity: 1.0,
        description: "高对比度黑白，强对比，中性色调，经典黑白摄影"
    )

    /// 柔和黑白 - 柔和黑白：低对比，平滑渐变
    static let bwSoft = LutFilterPreset(
        name: "bw_soft",
        displayName: "柔和黑白",
        category: .bw,
        parameters: FilterParameters(
            temperature: 0, tint: 0,
            exposure: 0.10,
            brightness: 0.05,
            contrast: 0.88,
            saturation: 0.0,
            vibrance: 0.0,
            highlightAmount: 0.90,
            shadowAmount: 0.15,
            isMonochrome: true,
            monochromeIntensity: 1.0,
            monochromeColorR: 0.95, monochromeColorG: 0.94, monochromeColorB: 0.92
        ),
        defaultIntensity: 1.0,
        description: "柔和黑白，低对比，平滑渐变，细腻柔和"
    )

    /// 暖调黑白 - 暖色调黑白：暖色基底，复古感
    static let bwWarm = LutFilterPreset(
        name: "bw_warm",
        displayName: "暖调黑白",
        category: .bw,
        parameters: FilterParameters(
            temperature: 0, tint: 0,
            exposure: 0.05,
            brightness: 0.02,
            contrast: 1.15,
            saturation: 0.0,
            vibrance: 0.0,
            highlightAmount: 0.95,
            shadowAmount: 0.05,
            isMonochrome: true,
            monochromeIntensity: 1.0,
            monochromeColorR: 0.98, monochromeColorG: 0.90, monochromeColorB: 0.82
        ),
        defaultIntensity: 1.0,
        description: "暖色调黑白，复古暖色基底，怀旧感"
    )

    /// 冷调黑白 - 冷色调黑白：冷色基底，清冷感
    static let bwCool = LutFilterPreset(
        name: "bw_cool",
        displayName: "冷调黑白",
        category: .bw,
        parameters: FilterParameters(
            temperature: 0, tint: 0,
            exposure: -0.05,
            brightness: -0.03,
            contrast: 1.20,
            saturation: 0.0,
            vibrance: 0.0,
            highlightAmount: 0.98,
            shadowAmount: -0.05,
            isMonochrome: true,
            monochromeIntensity: 1.0,
            monochromeColorR: 0.85, monochromeColorG: 0.90, monochromeColorB: 0.98
        ),
        defaultIntensity: 1.0,
        description: "冷色调黑白，冷色基底，清冷感，现代风格"
    )
}

// MARK: - 人像系列 (Portrait)

private enum PortraitPresets {
    static let all: [LutFilterPreset] = [
        portraitCreamy, portraitCoolWhite, portraitWarmSun, portraitAtmospheric
    ]

    /// 奶油肌 - 奶油肌人像：暖色，柔化，明亮
    static let portraitCreamy = LutFilterPreset(
        name: "portrait_creamy",
        displayName: "奶油肌",
        category: .portrait,
        parameters: FilterParameters(
            temperature: 500,
            tint: 8,
            exposure: 0.20,
            brightness: 0.10,
            contrast: 0.80,
            saturation: 0.95,
            vibrance: 0.05,
            highlightAmount: 0.82,
            shadowAmount: 0.18,
            useToneCurve: true,
            toneCurveR: [0.0, 0.35, 0.60, 0.84, 1.0],
            toneCurveG: [0.0, 0.32, 0.58, 0.82, 1.0],
            toneCurveB: [0.0, 0.30, 0.55, 0.80, 0.98]
        ),
        defaultIntensity: 0.82,
        description: "奶油肌人像，暖色柔化，明亮通透，细腻肌肤质感"
    )

    /// 冷白皮 - 冷白皮人像：冷色，明亮，白皙
    static let portraitCoolWhite = LutFilterPreset(
        name: "portrait_cool_white",
        displayName: "冷白皮",
        category: .portrait,
        parameters: FilterParameters(
            temperature: -500,
            tint: -5,
            exposure: 0.25,
            brightness: 0.12,
            contrast: 0.85,
            saturation: 0.82,
            vibrance: -0.05,
            highlightAmount: 0.80,
            shadowAmount: 0.22,
            useColorMatrix: true,
            colorMatrixRR: 0.95, colorMatrixRG: 0.0, colorMatrixRB: 0.0, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 0.98, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.0, colorMatrixBG: 0.0, colorMatrixBB: 1.08, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.85,
        description: "冷白皮人像，冷色调，明亮白皙，通透干净"
    )

    /// 暖阳人像 - 暖阳人像：金色暖调，柔和
    static let portraitWarmSun = LutFilterPreset(
        name: "portrait_warm_sun",
        displayName: "暖阳人像",
        category: .portrait,
        parameters: FilterParameters(
            temperature: 1800,
            tint: 12,
            exposure: 0.15,
            brightness: 0.06,
            contrast: 0.88,
            saturation: 1.05,
            vibrance: 0.10,
            highlightAmount: 0.85,
            shadowAmount: 0.12,
            useToneCurve: true,
            toneCurveR: [0.0, 0.32, 0.56, 0.82, 1.0],
            toneCurveG: [0.0, 0.28, 0.52, 0.78, 1.0],
            toneCurveB: [0.0, 0.22, 0.45, 0.72, 0.98]
        ),
        defaultIntensity: 0.85,
        description: "暖阳人像，金色暖调，柔和光线，温暖氛围"
    )

    /// 氛围感人像 - 氛围感人像：暗调，情绪化，电影感
    static let portraitAtmospheric = LutFilterPreset(
        name: "portrait_atmospheric",
        displayName: "氛围感人像",
        category: .portrait,
        parameters: FilterParameters(
            temperature: -300,
            tint: -8,
            exposure: -0.15,
            brightness: -0.06,
            contrast: 1.22,
            saturation: 0.88,
            vibrance: -0.03,
            highlightAmount: 0.90,
            shadowAmount: -0.05,
            useToneCurve: true,
            toneCurveR: [0.0, 0.18, 0.42, 0.70, 0.98],
            toneCurveG: [0.0, 0.20, 0.44, 0.72, 0.98],
            toneCurveB: [0.0, 0.22, 0.46, 0.74, 0.98]
        ),
        defaultIntensity: 0.88,
        description: "氛围感人像，暗调情绪化，电影感，深沉表现力"
    )
}

// MARK: - 风光系列 (Landscape)

private enum LandscapePresets {
    static let all: [LutFilterPreset] = [
        landscapeVivid, landscapeCinema, landscapeTealOrange, landscapeCyberpunk
    ]

    /// 鲜艳风光 - 鲜艳风光：高饱和，高对比，鲜明
    static let landscapeVivid = LutFilterPreset(
        name: "landscape_vivid",
        displayName: "鲜艳风光",
        category: .landscape,
        parameters: FilterParameters(
            temperature: 200,
            tint: 0,
            exposure: 0.08,
            brightness: 0.03,
            contrast: 1.25,
            saturation: 1.40,
            vibrance: 0.22,
            highlightAmount: 0.92,
            shadowAmount: -0.04,
            useToneCurve: true,
            toneCurveR: [0.0, 0.22, 0.48, 0.74, 1.0],
            toneCurveG: [0.0, 0.24, 0.50, 0.76, 1.0],
            toneCurveB: [0.0, 0.20, 0.46, 0.72, 1.0]
        ),
        defaultIntensity: 0.90,
        description: "鲜艳风光，高饱和高对比，鲜明色彩，风光大片"
    )

    /// 电影风光 - 电影风光：青橙色调，电影感
    static let landscapeCinema = LutFilterPreset(
        name: "landscape_cinema",
        displayName: "电影风光",
        category: .landscape,
        parameters: FilterParameters(
            temperature: -400,
            tint: -10,
            exposure: 0.0,
            brightness: -0.02,
            contrast: 1.15,
            saturation: 1.20,
            vibrance: 0.12,
            highlightAmount: 0.88,
            shadowAmount: 0.04,
            useColorMatrix: true,
            colorMatrixRR: 1.12, colorMatrixRG: 0.0, colorMatrixRB: 0.0, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 1.08, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.0, colorMatrixBG: 0.0, colorMatrixBB: 0.92, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.88,
        description: "电影风光，青橙色调，电影感，大气磅礴"
    )

    /// 青橙色调 - 青橙色调：强烈青橙对比，高饱和
    static let landscapeTealOrange = LutFilterPreset(
        name: "landscape_teal_orange",
        displayName: "青橙色调",
        category: .landscape,
        parameters: FilterParameters(
            temperature: -800,
            tint: -18,
            exposure: 0.0,
            brightness: -0.02,
            contrast: 1.22,
            saturation: 1.25,
            vibrance: 0.18,
            highlightAmount: 0.86,
            shadowAmount: -0.03,
            useColorMatrix: true,
            colorMatrixRR: 1.20, colorMatrixRG: 0.0, colorMatrixRB: 0.0, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 1.05, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.0, colorMatrixBG: 0.0, colorMatrixBB: 0.85, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.90,
        description: "青橙色调，强烈青橙对比，高饱和，Instagram 风格"
    )

    /// 赛博朋克 - 赛博朋克风格：蓝紫调，高对比，霓虹感
    static let landscapeCyberpunk = LutFilterPreset(
        name: "landscape_cyberpunk",
        displayName: "赛博朋克",
        category: .landscape,
        parameters: FilterParameters(
            temperature: -1500,
            tint: -25,
            exposure: -0.10,
            brightness: -0.05,
            contrast: 1.45,
            saturation: 1.30,
            vibrance: 0.20,
            highlightAmount: 0.80,
            shadowAmount: -0.15,
            useColorMatrix: true,
            colorMatrixRR: 1.05, colorMatrixRG: 0.0, colorMatrixRB: 0.08, colorMatrixRA: 0.0, colorMatrixRBias: 0.0,
            colorMatrixGR: 0.0, colorMatrixGG: 0.90, colorMatrixGB: 0.0, colorMatrixGA: 0.0, colorMatrixGBias: 0.0,
            colorMatrixBR: 0.08, colorMatrixBG: 0.0, colorMatrixBB: 1.20, colorMatrixBA: 0.0, colorMatrixBBias: 0.0
        ),
        defaultIntensity: 0.90,
        description: "赛博朋克风格，蓝紫调，高对比，霓虹感，未来主义"
    )
}

// MARK: - 美食系列 (Food)

private enum FoodPresets {
    static let all: [LutFilterPreset] = [
        foodEnhancer, foodWarmLight, foodFreshDessert
    ]

    /// 美食增色 - 美食增色：高饱和，暖色，鲜艳
    static let foodEnhancer = LutFilterPreset(
        name: "food_enhancer",
        displayName: "美食增色",
        category: .food,
        parameters: FilterParameters(
            temperature: 1200,
            tint: 5,
            exposure: 0.15,
            brightness: 0.08,
            contrast: 1.12,
            saturation: 1.38,
            vibrance: 0.25,
            highlightAmount: 0.88,
            shadowAmount: 0.08,
            useToneCurve: true,
            toneCurveR: [0.0, 0.28, 0.54, 0.80, 1.0],
            toneCurveG: [0.0, 0.26, 0.52, 0.78, 1.0],
            toneCurveB: [0.0, 0.22, 0.48, 0.74, 1.0]
        ),
        defaultIntensity: 0.88,
        description: "美食增色，高饱和暖色，鲜艳诱人，食欲大增"
    )

    /// 暖光美食 - 暖光美食：暖黄光，柔和，温馨
    static let foodWarmLight = LutFilterPreset(
        name: "food_warm_light",
        displayName: "暖光美食",
        category: .food,
        parameters: FilterParameters(
            temperature: 2000,
            tint: 8,
            exposure: 0.10,
            brightness: 0.05,
            contrast: 1.02,
            saturation: 1.15,
            vibrance: 0.12,
            highlightAmount: 0.85,
            shadowAmount: 0.12,
            useToneCurve: true,
            toneCurveR: [0.0, 0.32, 0.58, 0.82, 1.0],
            toneCurveG: [0.0, 0.28, 0.54, 0.80, 1.0],
            toneCurveB: [0.0, 0.20, 0.44, 0.70, 0.98]
        ),
        defaultIntensity: 0.85,
        description: "暖光美食，暖黄灯光，柔和温馨，咖啡馆氛围"
    )

    /// 清新甜品 - 清新甜品：明亮，微冷，通透
    static let foodFreshDessert = LutFilterPreset(
        name: "food_fresh_dessert",
        displayName: "清新甜品",
        category: .food,
        parameters: FilterParameters(
            temperature: -200,
            tint: -3,
            exposure: 0.28,
            brightness: 0.12,
            contrast: 0.85,
            saturation: 1.05,
            vibrance: 0.08,
            highlightAmount: 0.80,
            shadowAmount: 0.22,
            useToneCurve: true,
            toneCurveR: [0.0, 0.36, 0.62, 0.84, 1.0],
            toneCurveG: [0.0, 0.34, 0.60, 0.82, 1.0],
            toneCurveB: [0.0, 0.32, 0.58, 0.80, 1.0]
        ),
        defaultIntensity: 0.85,
        description: "清新甜品，明亮微冷，通透感，甜品精致呈现"
    )
}

// MARK: - 经典系列 (Classic) - 兼容旧 API 的 12 款经典预设

private enum ClassicPresets {
    static let all: [LutFilterPreset] = [
        LutFilterPreset.dokaPortrait,
        LutFilterPreset.kodakPortra160,
        LutFilterPreset.agfaVista400,
        LutFilterPreset.fujiPro400H,
        LutFilterPreset.ilfordHP5,
        LutFilterPreset.cinestill800T,
        LutFilterPreset.leicaClassic,
        LutFilterPreset.hasselbladNatural,
        LutFilterPreset.ricohPositive,
        LutFilterPreset.polaroid,
        LutFilterPreset.fadedMemory,
        LutFilterPreset.japaneseAiry
    ]
}

// MARK: - 预设查找辅助

extension LutFilterPreset {

    /// 按名称查找预设
    static func findByName(_ name: String) -> LutFilterPreset? {
        allBuiltInPresets.first { $0.name == name }
    }

    /// 按 ID 查找预设
    static func find(by id: UUID) -> LutFilterPreset? {
        allBuiltInPresets.first { $0.id == id }
    }

    /// 搜索预设（按名称或显示名称）
    static func search(_ query: String) -> [LutFilterPreset] {
        guard !query.isEmpty else { return allBuiltInPresets }
        let lowercased = query.lowercased()
        return allBuiltInPresets.filter {
            $0.name.lowercased().contains(lowercased) ||
            $0.displayName.lowercased().contains(lowercased)
        }
    }
}

#endif