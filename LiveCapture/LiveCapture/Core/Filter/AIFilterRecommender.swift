//
//  AIFilterRecommender.swift
//  LiveCapture
//
//  AI 滤镜推荐器
//
//  ## 文件作用
//  基于场景类型和光线分析，智能推荐最适合的滤镜预设
//  支持基于图像直方图分析的自动推荐
//  使用规则引擎匹配场景-滤镜最佳组合
//
//  ## 主要类
//  - AIFilterRecommender: AI 滤镜推荐器
//
//  ## 场景-滤镜映射规则
//  - 人像 (portrait) → Doka 人像, 柯达 Portra 160, 哈苏自然
//  - 美食 (food) → 理光正片, 日系透明感, Agfa Vista 400
//  - 风景 (landscape) → 哈苏自然, 徕卡经典, Fuji Pro 400H
//  - 夜景 (nightScene) → Cinestill 800T, 褪色记忆, Ilford HP5
//  - 建筑 (architecture) → 徕卡经典, Ilford HP5, 哈苏自然
//  - 街拍 (street) → 徕卡经典, 柯达 Portra 160, Ilford HP5
//  - 微距 (macro) → 哈苏自然, 理光正片, 日系透明感
//  - 室内 (indoor) → Doka 人像, 柯达 Portra 160, 日系透明感
//  - 日落 (sunset) → 柯达 Portra 160, 褪色记忆, Agfa Vista 400
//  - 海滩 (beach) → 日系透明感, 哈苏自然, Fuji Pro 400H
//  - 雪景 (snow) → 日系透明感, 哈苏自然, Fuji Pro 400H
//  - 未知 (unknown) → 哈苏自然, 柯达 Portra 160, 徕卡经典
//
//  ## 光线调整逻辑
//  - 暖光环境 → 降低暖色调滤镜推荐权重
//  - 冷光环境 → 降低冷色调滤镜推荐权重
//  - 低光环境 → 提高 Cinestill 800T, 褪色记忆 权重
//  - 高光环境 → 提高 日系透明感, 宝丽来 权重
//
//  ## 图像分析
//  - 使用 CIImage 的 CIAreaHistogram 分析颜色分布
//  - 通过 CIAreaAverage 计算平均颜色
//  - 根据亮度分布和颜色特征推荐滤镜
//

import Foundation
import CoreImage
import Accelerate

#if os(iOS)

/// AI 滤镜推荐器
final class AIFilterRecommender {

    // MARK: - 场景-滤镜映射表

    /// 场景类型到推荐滤镜的名称映射
    private let sceneFilterMap: [SceneType: [String]] = [
        .portrait: ["doka_portrait", "kodak_portra_160", "hasselblad_natural"],
        .food: ["ricoh_positive", "japanese_airy", "agfa_vista_400"],
        .landscape: ["hasselblad_natural", "leica_classic", "fuji_pro_400h"],
        .nightScene: ["cinestill_800t", "faded_memory", "ilford_hp5"],
        .architecture: ["leica_classic", "ilford_hp5", "hasselblad_natural"],
        .street: ["leica_classic", "kodak_portra_160", "ilford_hp5"],
        .macro: ["hasselblad_natural", "ricoh_positive", "japanese_airy"],
        .indoor: ["doka_portrait", "kodak_portra_160", "japanese_airy"],
        .sunset: ["kodak_portra_160", "faded_memory", "agfa_vista_400"],
        .beach: ["japanese_airy", "hasselblad_natural", "fuji_pro_400h"],
        .snow: ["japanese_airy", "hasselblad_natural", "fuji_pro_400h"],
        .unknown: ["hasselblad_natural", "kodak_portra_160", "leica_classic"]
    ]

    /// 场景-滤镜推荐理由
    private let sceneReasonMap: [SceneType: [String: String]] = [
        .portrait: [
            "doka_portrait": "专为人像优化，柔化肤色同时保持自然",
            "kodak_portra_160": "经典胶片色调，给人像增添温暖感",
            "hasselblad_natural": "自然色彩还原，展现真实肤色"
        ],
        .food: [
            "ricoh_positive": "正片风格增强食物色彩层次",
            "japanese_airy": "明亮通透让食物更显新鲜",
            "agfa_vista_400": "高饱和度突显食材的丰富色彩"
        ],
        .landscape: [
            "hasselblad_natural": "自然色彩准确还原风景本色",
            "leica_classic": "浓郁暗部增强风景层次感",
            "fuji_pro_400h": "冷调粉彩营造清新自然氛围"
        ],
        .nightScene: [
            "cinestill_800t": "电影感色调完美适配夜景氛围",
            "faded_memory": "复古褪色增添夜景神秘感",
            "ilford_hp5": "黑白高反差突出夜景光影对比"
        ],
        .architecture: [
            "leica_classic": "徕卡风格强化建筑线条与质感",
            "ilford_hp5": "黑白影调凸显建筑几何美感",
            "hasselblad_natural": "自然色彩还原建筑真实面貌"
        ],
        .street: [
            "leica_classic": "徕卡经典色调，街拍首选",
            "kodak_portra_160": "胶片感增添街头故事性",
            "ilford_hp5": "黑白街拍，永恒经典"
        ],
        .macro: [
            "hasselblad_natural": "自然色彩准确还原微距细节",
            "ricoh_positive": "正片色彩增强微距主体表现",
            "japanese_airy": "明亮通透展现微距世界的细腻"
        ],
        .indoor: [
            "doka_portrait": "室内人像优化，暖色补光",
            "kodak_portra_160": "胶片色调提升室内氛围感",
            "japanese_airy": "明亮通透感改善室内光线"
        ],
        .sunset: [
            "kodak_portra_160": "暖色胶片增强日落的金色光辉",
            "faded_memory": "复古褪色渲染日落怀旧情绪",
            "agfa_vista_400": "高饱和度让日落色彩更加绚烂"
        ],
        .beach: [
            "japanese_airy": "日系透明感展现海滩清新",
            "hasselblad_natural": "自然色彩还原碧海蓝天",
            "fuji_pro_400h": "冷调粉彩营造海滩度假氛围"
        ],
        .snow: [
            "japanese_airy": "明亮通透展现雪景的纯净",
            "hasselblad_natural": "自然色彩还原皑皑白雪",
            "fuji_pro_400h": "冷调粉彩增强冬日氛围"
        ],
        .unknown: [
            "hasselblad_natural": "自然色彩，适用于大多数场景",
            "kodak_portra_160": "经典胶片，通用性强",
            "leica_classic": "徕卡风格，提升画面质感"
        ]
    ]

    // MARK: - 推荐方法

    /// 基于场景类型和光线分析推荐滤镜
    /// - Parameters:
    ///   - scene: 场景类型
    ///   - lightAnalysis: 光线分析结果
    ///   - topK: 返回前 K 个推荐，默认 3
    /// - Returns: 推荐结果数组，按置信度降序排列
    func recommend(for scene: SceneType, lightAnalysis: LightAnalysis, topK: Int = 3) -> [FilterRecommendation] {
        let presetMap = Dictionary(uniqueKeysWithValues: LutFilterPreset.builtInPresets.map { ($0.name, $0) })
        let reasonMap = sceneReasonMap[scene] ?? [:]

        // 获取该场景的基础推荐滤镜名
        var recommendedNames = sceneFilterMap[scene] ?? sceneFilterMap[.unknown]!

        // 根据光线调整推荐顺序
        recommendedNames = adjustForLight(recommendedNames, lightAnalysis: lightAnalysis)

        // 构建推荐结果
        var recommendations: [FilterRecommendation] = []
        let total = recommendedNames.count

        for (index, name) in recommendedNames.enumerated() {
            guard let preset = presetMap[name] else { continue }

            // 基础置信度：排名越靠前越高
            let baseConfidence: Float = 1.0 - (Float(index) / Float(total + 1))

            // 根据光线微调置信度
            let adjustedConfidence = adjustConfidence(baseConfidence, preset: preset, lightAnalysis: lightAnalysis)

            let reason = reasonMap[name] ?? generateReason(for: preset, scene: scene)

            recommendations.append(FilterRecommendation(
                preset: preset,
                confidence: min(1.0, max(0.0, adjustedConfidence)),
                reason: reason
            ))
        }

        // 按置信度排序并取前 K 个
        return Array(recommendations.sorted { $0.confidence > $1.confidence }.prefix(topK))
    }

    /// 基于图像分析推荐滤镜
    /// - Parameter image: 输入图像
    /// - Returns: 推荐结果数组
    func recommendForImage(_ image: CIImage) -> [FilterRecommendation] {
        // 分析图像颜色分布
        let lightAnalysis = analyzeImage(image)

        // 推断场景类型
        let scene = inferScene(from: lightAnalysis)

        return recommend(for: scene, lightAnalysis: lightAnalysis, topK: 3)
    }

    // MARK: - 光线调整

    /// 根据光线条件调整推荐滤镜顺序
    private func adjustForLight(_ names: [String], lightAnalysis: LightAnalysis) -> [String] {
        var adjusted = names

        // 暖光环境：降低暖色调滤镜优先级
        if lightAnalysis.isWarmLight {
            // 将过于暖色的滤镜移后
            let warmFilters = ["kodak_portra_160", "faded_memory"]
            adjusted = deprioritize(adjusted, filters: warmFilters)
        }

        // 冷光环境：降低冷色调滤镜优先级
        if lightAnalysis.isCoolLight {
            let coolFilters = ["fuji_pro_400h", "cinestill_800t", "japanese_airy"]
            adjusted = deprioritize(adjusted, filters: coolFilters)
        }

        // 低光环境：提升夜景滤镜优先级
        if lightAnalysis.isLowLight {
            let nightFilters = ["cinestill_800t", "faded_memory", "ilford_hp5"]
            adjusted = prioritize(adjusted, filters: nightFilters)
        }

        // 高光环境：提升明亮滤镜优先级
        if lightAnalysis.isHighLight {
            let brightFilters = ["japanese_airy", "polaroid", "hasselblad_natural"]
            adjusted = prioritize(adjusted, filters: brightFilters)
        }

        return adjusted
    }

    /// 降低指定滤镜的优先级（移到列表末尾）
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

    /// 提升指定滤镜的优先级（移到列表前面）
    private func prioritize(_ names: [String], filters: [String]) -> [String] {
        var result = names
        // 逆序处理以保持原始相对顺序
        for filterName in filters.reversed() {
            if let index = result.firstIndex(of: filterName) {
                result.remove(at: index)
                result.insert(filterName, at: 0)
            }
        }
        return result
    }

    // MARK: - 置信度调整

    /// 根据光线条件微调置信度
    private func adjustConfidence(_ base: Float, preset: LutFilterPreset, lightAnalysis: LightAnalysis) -> Float {
        var confidence = base

        let params = preset.parameters

        // 暖光 + 暖色滤镜 → 降低置信度（避免过度暖色）
        if lightAnalysis.isWarmLight && params.temperature > 500 {
            confidence -= 0.1
        }

        // 冷光 + 冷色滤镜 → 降低置信度
        if lightAnalysis.isCoolLight && params.temperature < -300 {
            confidence -= 0.1
        }

        // 低光 + 高对比度滤镜 → 提高置信度
        if lightAnalysis.isLowLight && params.contrast > 1.15 {
            confidence += 0.05
        }

        // 低光 + 低亮度滤镜 → 降低置信度
        if lightAnalysis.isLowLight && params.brightness < -0.03 {
            confidence -= 0.08
        }

        // 高光 + 低对比滤镜 → 提高置信度
        if lightAnalysis.isHighLight && params.contrast < 0.9 {
            confidence += 0.05
        }

        return min(1.0, max(0.0, confidence))
    }

    // MARK: - 图像分析

    /// 分析图像的颜色和亮度特征
    /// - Parameter image: 输入 CIImage
    /// - Returns: 光线分析结果
    private func analyzeImage(_ image: CIImage) -> LightAnalysis {
        var analysis = LightAnalysis()

        // 使用 CIAreaAverage 获取平均颜色
        let extent = image.extent
        guard let averageFilter = CIFilter(name: "CIAreaAverage") else {
            return analysis
        }
        averageFilter.setValue(image, forKey: kCIInputImageKey)
        averageFilter.setValue(CIVector(cgRect: extent), forKey: kCIInputExtentKey)

        guard let outputImage = averageFilter.outputImage else {
            return analysis
        }

        // 渲染单个像素获取平均颜色
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

        // 估算亮度（感知亮度公式）
        analysis.estimatedBrightness = 0.299 * r + 0.587 * g + 0.114 * b

        // 估算色温（基于 R/B 比值）
        let rbRatio = r / max(b, 0.001)
        if rbRatio > 1.2 {
            analysis.estimatedTemperature = 6500 - (rbRatio - 1.0) * 3000
        } else if rbRatio < 0.8 {
            analysis.estimatedTemperature = 6500 + (1.0 - rbRatio) * 3000
        } else {
            analysis.estimatedTemperature = 6500
        }
        analysis.estimatedTemperature = max(2000, min(10000, analysis.estimatedTemperature))

        // 光线条件判断
        analysis.isWarmLight = analysis.estimatedTemperature < 5000
        analysis.isCoolLight = analysis.estimatedTemperature > 7500
        analysis.isLowLight = analysis.estimatedBrightness < 0.3
        analysis.isHighLight = analysis.estimatedBrightness > 0.8

        return analysis
    }

    /// 根据光线分析推断场景类型
    private func inferScene(from light: LightAnalysis) -> SceneType {
        // 低光环境 → 可能是夜景
        if light.isLowLight {
            return .nightScene
        }

        // 暖色 + 偏红 → 可能是日落
        if light.isWarmLight && light.averageR > light.averageB * 1.3 {
            return .sunset
        }

        // 偏蓝 + 明亮 → 可能是海滩或雪景
        if light.isCoolLight && light.isHighLight {
            return .beach
        }

        // 默认返回未知
        return .unknown
    }

    // MARK: - 辅助方法

    /// 生成推荐理由
    private func generateReason(for preset: LutFilterPreset, scene: SceneType) -> String {
        let params = preset.parameters

        if params.isMonochrome {
            return "黑白影调增强画面表现力"
        }

        if params.contrast > 1.2 {
            return "高对比度增强画面层次感"
        }

        if params.contrast < 0.85 {
            return "柔和对比营造舒适观感"
        }

        if params.temperature > 500 {
            return "暖色调增添画面温馨感"
        }

        if params.temperature < -500 {
            return "冷色调营造清新氛围"
        }

        return "适用于\(scene.displayName)场景"
    }
}

#endif