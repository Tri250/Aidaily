//
//  AIFilterRecommender.swift
//  LiveCapture
//
//  AI 滤镜推荐器 - 基于场景类型和光线分析智能推荐滤镜
//
//  ## 场景-滤镜映射规则（更新为 30+ 预设）
//  - 人像 (portrait) → 奶油肌, 冷白皮, 暖阳人像
//  - 美食 (food) → 美食增色, 暖光美食, 清新甜品
//  - 风景 (landscape) → 鲜艳风光, 电影风光, 哈苏自然
//  - 夜景 (nightScene) → 赛博朋克, 霓虹夜色, 九龙城寨
//  - 建筑 (architecture) → 高对比黑白, 冷调黑白, 青橙色调
//  - 街拍 (street) → 港风复古, 王家卫, 柯达金
//  - 日落 (sunset) → 柯达金, 胶片褪色, 暖阳人像
//  - 海滩 (beach) → 冲绳蓝, 小清新, 日系暖阳
//  - 雪景 (snow) → 冷白皮, 和风淡彩, 小清新
//

import Foundation
import CoreImage
import Accelerate

#if os(iOS)

/// AI 滤镜推荐器
final class AIFilterRecommender {

    // MARK: - 场景-滤镜映射表（更新为 30+ 预设名称）

    private let sceneFilterMap: [SceneType: [String]] = [
        .portrait: ["portrait_creamy", "portrait_cool_white", "portrait_warm_sun"],
        .food: ["food_enhancer", "food_warm_light", "food_fresh_dessert"],
        .landscape: ["landscape_vivid", "landscape_cinema", "hasselblad_natural"],
        .nightScene: ["landscape_cyberpunk", "hk_neon", "hk_kowloon"],
        .architecture: ["bw_high_contrast", "bw_cool", "landscape_teal_orange"],
        .street: ["hk_retro", "hk_wong_kar_wai", "kodak_gold"],
        .macro: ["hasselblad_natural", "food_fresh_dessert", "japanese_fresh"],
        .indoor: ["portrait_creamy", "food_warm_light", "japanese_warm_sun"],
        .sunset: ["kodak_gold", "film_fade", "portrait_warm_sun"],
        .beach: ["japanese_okinawa", "japanese_fresh", "japanese_warm_sun"],
        .snow: ["portrait_cool_white", "japanese_pastel", "japanese_fresh"],
        .unknown: ["hasselblad_natural", "fuji_classic", "leica_classic"]
    ]

    /// 场景-滤镜推荐理由
    private let sceneReasonMap: [SceneType: [String: String]] = [
        .portrait: [
            "portrait_creamy": "专为人像优化，柔化肤色同时保持自然",
            "portrait_cool_white": "冷色调让肤色更显白皙通透",
            "portrait_warm_sun": "金色暖调为人像增添温暖感"
        ],
        .food: [
            "food_enhancer": "增强食物色彩饱和度，让每一道菜都诱人",
            "food_warm_light": "暖黄灯光营造温馨用餐氛围",
            "food_fresh_dessert": "明亮通透让甜品更显精致清新"
        ],
        .landscape: [
            "landscape_vivid": "高饱和高对比，展现风光壮丽色彩",
            "landscape_cinema": "电影感色调，大气磅礴",
            "hasselblad_natural": "自然色彩还原风景本色"
        ],
        .nightScene: [
            "landscape_cyberpunk": "赛博朋克风格完美适配夜景",
            "hk_neon": "霓虹色调营造夜间都市氛围",
            "hk_kowloon": "暗沉冷色调增强夜景神秘感"
        ],
        .architecture: [
            "bw_high_contrast": "高对比黑白凸显建筑线条",
            "bw_cool": "冷调黑白展现建筑几何美感",
            "landscape_teal_orange": "青橙色调增强建筑视觉冲击"
        ],
        .street: [
            "hk_retro": "港风复古色调，街拍首选",
            "hk_wong_kar_wai": "王家卫风格，街头故事感",
            "kodak_gold": "柯达金色调增添街头温暖感"
        ],
        .macro: [
            "hasselblad_natural": "自然色彩准确还原微距细节",
            "food_fresh_dessert": "明亮通透展现微距世界的细腻",
            "japanese_fresh": "日系清新风格增强微距表现"
        ],
        .indoor: [
            "portrait_creamy": "室内人像优化，柔化效果好",
            "food_warm_light": "暖光色调提升室内氛围感",
            "japanese_warm_sun": "日系暖阳改善室内光线"
        ],
        .sunset: [
            "kodak_gold": "柯达金色增强日落的金色光辉",
            "film_fade": "胶片褪色渲染日落怀旧情绪",
            "portrait_warm_sun": "暖阳色调让日落更加温馨"
        ],
        .beach: [
            "japanese_okinawa": "冲绳蓝展现碧海蓝天的通透",
            "japanese_fresh": "日系清新展现海滩的纯净",
            "japanese_warm_sun": "日系暖阳营造海滩度假氛围"
        ],
        .snow: [
            "portrait_cool_white": "冷白皮展现雪景的纯净白皙",
            "japanese_pastel": "和风淡彩渲染冬日淡雅氛围",
            "japanese_fresh": "日系清新展现雪景的通透感"
        ],
        .unknown: [
            "hasselblad_natural": "自然色彩，适用于大多数场景",
            "fuji_classic": "富士经典，通用性强",
            "leica_classic": "徕卡风格，提升画面质感"
        ]
    ]

    // MARK: - 推荐方法

    /// 基于场景类型和光线分析推荐滤镜
    func recommend(for scene: SceneType, lightAnalysis: LightAnalysis, topK: Int = 3) -> [FilterRecommendation] {
        let presetMap = Dictionary(uniqueKeysWithValues: LutFilterPreset.builtInPresets.map { ($0.name, $0) })
        let reasonMap = sceneReasonMap[scene] ?? [:]

        var recommendedNames = sceneFilterMap[scene] ?? sceneFilterMap[.unknown]!
        recommendedNames = adjustForLight(recommendedNames, lightAnalysis: lightAnalysis)

        var recommendations: [FilterRecommendation] = []
        let total = recommendedNames.count

        for (index, name) in recommendedNames.enumerated() {
            guard let preset = presetMap[name] else { continue }
            let baseConfidence: Float = 1.0 - (Float(index) / Float(total + 1))
            let adjustedConfidence = adjustConfidence(baseConfidence, preset: preset, lightAnalysis: lightAnalysis)
            let reason = reasonMap[name] ?? generateReason(for: preset, scene: scene)

            recommendations.append(FilterRecommendation(
                preset: preset,
                confidence: min(1.0, max(0.0, adjustedConfidence)),
                reason: reason
            ))
        }

        return Array(recommendations.sorted { $0.confidence > $1.confidence }.prefix(topK))
    }

    /// 基于图像分析推荐滤镜
    func recommendForImage(_ image: CIImage) -> [FilterRecommendation] {
        let lightAnalysis = analyzeImage(image)
        let scene = inferScene(from: lightAnalysis)
        return recommend(for: scene, lightAnalysis: lightAnalysis, topK: 3)
    }

    // MARK: - 光线调整

    private func adjustForLight(_ names: [String], lightAnalysis: LightAnalysis) -> [String] {
        var adjusted = names

        if lightAnalysis.isWarmLight {
            let warmFilters = ["kodak_gold", "film_fade", "portrait_warm_sun"]
            adjusted = deprioritize(adjusted, filters: warmFilters)
        }

        if lightAnalysis.isCoolLight {
            let coolFilters = ["portrait_cool_white", "landscape_cyberpunk", "japanese_fresh"]
            adjusted = deprioritize(adjusted, filters: coolFilters)
        }

        if lightAnalysis.isLowLight {
            let nightFilters = ["landscape_cyberpunk", "hk_neon", "hk_kowloon"]
            adjusted = prioritize(adjusted, filters: nightFilters)
        }

        if lightAnalysis.isHighLight {
            let brightFilters = ["japanese_fresh", "japanese_warm_sun", "hasselblad_natural"]
            adjusted = prioritize(adjusted, filters: brightFilters)
        }

        return adjusted
    }

    private func deprioritize(_ names: [String], filters: [String]) -> [String] {
        var result = names
        for filterName in filters {
            if let index = result.firstIndex(of: filterName) {
                result.remove(at: index)
                result.append(filterName)
            }
        }
        return result
    }

    private func prioritize(_ names: [String], filters: [String]) -> [String] {
        var result = names
        for filterName in filters.reversed() {
            if let index = result.firstIndex(of: filterName) {
                result.remove(at: index)
                result.insert(filterName, at: 0)
            }
        }
        return result
    }

    // MARK: - 置信度调整

    private func adjustConfidence(_ base: Float, preset: LutFilterPreset, lightAnalysis: LightAnalysis) -> Float {
        var confidence = base
        let params = preset.parameters

        if lightAnalysis.isWarmLight && params.temperature > 500 {
            confidence -= 0.1
        }
        if lightAnalysis.isCoolLight && params.temperature < -300 {
            confidence -= 0.1
        }
        if lightAnalysis.isLowLight && params.contrast > 1.15 {
            confidence += 0.05
        }
        if lightAnalysis.isLowLight && params.brightness < -0.03 {
            confidence -= 0.08
        }
        if lightAnalysis.isHighLight && params.contrast < 0.9 {
            confidence += 0.05
        }

        return min(1.0, max(0.0, confidence))
    }

    // MARK: - 图像分析

    private func analyzeImage(_ image: CIImage) -> LightAnalysis {
        var analysis = LightAnalysis()
        let extent = image.extent
        guard let averageFilter = CIFilter(name: "CIAreaAverage") else { return analysis }
        averageFilter.setValue(image, forKey: kCIInputImageKey)
        averageFilter.setValue(CIVector(cgRect: extent), forKey: kCIInputExtentKey)
        guard let outputImage = averageFilter.outputImage else { return analysis }

        var bitmap = [UInt8](repeating: 0, count: 4)
        let context = CIContext()
        context.render(
            outputImage,
            toBitmap: &bitmap,
            rowBytes: 4,
            bounds: CGRect(x: 0, y: 0, width: 1, height: 1),
            format: .RGBA8,
            colorSpace: nil
        )

        let r = Float(bitmap[0]) / 255.0
        let g = Float(bitmap[1]) / 255.0
        let b = Float(bitmap[2]) / 255.0

        analysis.averageR = r
        analysis.averageG = g
        analysis.averageB = b
        analysis.estimatedBrightness = 0.299 * r + 0.587 * g + 0.114 * b

        let rbRatio = r / max(b, 0.001)
        if rbRatio > 1.2 {
            analysis.estimatedTemperature = 6500 - (rbRatio - 1.0) * 3000
        } else if rbRatio < 0.8 {
            analysis.estimatedTemperature = 6500 + (1.0 - rbRatio) * 3000
        } else {
            analysis.estimatedTemperature = 6500
        }
        analysis.estimatedTemperature = max(2000, min(10000, analysis.estimatedTemperature))

        analysis.isWarmLight = analysis.estimatedTemperature < 5000
        analysis.isCoolLight = analysis.estimatedTemperature > 7500
        analysis.isLowLight = analysis.estimatedBrightness < 0.3
        analysis.isHighLight = analysis.estimatedBrightness > 0.8

        return analysis
    }

    private func inferScene(from light: LightAnalysis) -> SceneType {
        if light.isLowLight { return .nightScene }
        if light.isWarmLight && light.averageR > light.averageB * 1.3 { return .sunset }
        if light.isCoolLight && light.isHighLight { return .beach }
        return .unknown
    }

    private func generateReason(for preset: LutFilterPreset, scene: SceneType) -> String {
        let params = preset.parameters
        if params.isMonochrome { return "黑白影调增强画面表现力" }
        if params.contrast > 1.2 { return "高对比度增强画面层次感" }
        if params.contrast < 0.85 { return "柔和对比营造舒适观感" }
        if params.temperature > 500 { return "暖色调增添画面温馨感" }
        if params.temperature < -500 { return "冷色调营造清新氛围" }
        return "适用于\(scene.displayName)场景"
    }
}

#endif