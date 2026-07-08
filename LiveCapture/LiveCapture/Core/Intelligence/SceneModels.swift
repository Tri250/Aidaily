//
//  SceneModels.swift
//  LiveCapture
//
//  AI 场景智能引擎 - 数据模型定义
//
//  ## 文件作用
//  定义场景识别、环境光分析、主体检测和自适应拍摄参数的数据模型
//  所有模型支持 Codable，便于持久化和调试
//
//  ## 主要类型
//
//  ### SceneType 枚举
//  支持 14+ 场景类型识别，涵盖日常拍摄常见场景
//  - portrait: 人像
//  - food: 美食
//  - landscape: 风景
//  - pet: 宠物
//  - architecture: 建筑
//  - nightScene: 夜景
//  - document: 文档
//  - sunrise: 日出日落
//  - snow: 雪景
//  - beach: 海滩
//  - flower: 花卉
//  - stage: 舞台
//  - street: 街拍
//  - indoor: 室内
//  - unknown: 未知
//
//  ### LightAnalysis 结构体
//  环境光分析结果，包含色温、亮度、对比度、逆光标和光源类型
//
//  ### SubjectDetection 结构体
//  主体检测结果，包含人物、动物、食物检测信息
//
//  ### AdaptiveCaptureParams 结构体
//  场景自适应拍摄参数，包含 ISO、快门、曝光补偿、白平衡、变焦建议等
//

import Foundation
import CoreGraphics

#if os(iOS)

/// 场景类型枚举 - 支持 14+ 场景识别
enum SceneType: String, CaseIterable, Codable {
	case portrait      // 人像
	case food          // 美食
	case landscape     // 风景
	case pet           // 宠物
	case architecture  // 建筑
	case nightScene    // 夜景
	case document      // 文档
	case sunrise       // 日出日落
	case snow          // 雪景
	case beach         // 海滩
	case flower        // 花卉
	case stage         // 舞台
	case street        // 街拍
	case indoor        // 室内
	case unknown            // 未知
	case portraitStanding   // 人像·站姿
	case portraitSitting    // 人像·坐姿
	case weddingOutdoor     // 婚礼·户外
	case weddingIndoor      // 婚礼·室内
	case childrenOutdoor    // 儿童·户外
	case childrenIndoor     // 儿童·室内
	case productWhite       // 产品·白底
	case landscapeSunset    // 风景·日落
	case landscapeNature    // 风景·自然
	case foodStyling        // 美食·摆盘
	case groupPhoto         // 合影
	case waterScene         // 水景
	case nightPortrait      // 夜景人像
	case silhouette         // 剪影
	case macroDetail        // 微距
	case texture            // 纹理

	/// 场景中文显示名称
	var displayName: String {
		switch self {
		case .portrait:      return "人像"
		case .food:          return "美食"
		case .landscape:     return "风景"
		case .pet:           return "宠物"
		case .architecture:  return "建筑"
		case .nightScene:    return "夜景"
		case .document:      return "文档"
		case .sunrise:       return "日出日落"
		case .snow:          return "雪景"
		case .beach:         return "海滩"
		case .flower:        return "花卉"
		case .stage:         return "舞台"
		case .street:        return "街拍"
		case .indoor:        return "室内"
		case .unknown:            return "自动"
		case .portraitStanding:   return "人像·站姿"
		case .portraitSitting:    return "人像·坐姿"
		case .weddingOutdoor:     return "婚礼·户外"
		case .weddingIndoor:      return "婚礼·室内"
		case .childrenOutdoor:    return "儿童·户外"
		case .childrenIndoor:     return "儿童·室内"
		case .productWhite:       return "产品·白底"
		case .landscapeSunset:    return "风景·日落"
		case .landscapeNature:    return "风景·自然"
		case .foodStyling:        return "美食·摆盘"
		case .groupPhoto:         return "合影"
		case .waterScene:         return "水景"
		case .nightPortrait:      return "夜景人像"
		case .silhouette:         return "剪影"
		case .macroDetail:        return "微距"
		case .texture:            return "纹理"
		}
	}

	/// SF Symbol 图标名称
	var iconName: String {
		switch self {
		case .portrait:      return "person.crop.rectangle"
		case .food:          return "fork.knife"
		case .landscape:     return "mountain.2"
		case .pet:           return "pawprint"
		case .architecture:  return "building.2"
		case .nightScene:    return "moon.stars"
		case .document:      return "doc.text"
		case .sunrise:       return "sunrise"
		case .snow:          return "snowflake"
		case .beach:         return "beach.umbrella"
		case .flower:        return "flower"
		case .stage:         return "theatermasks"
		case .street:        return "road.lanes"
		case .indoor:        return "house"
		case .unknown:             return "camera"
		case .portraitStanding:   return "figure.stand"
		case .portraitSitting:    return "figure.seated.side"
		case .weddingOutdoor:     return "heart.circle"
		case .weddingIndoor:      return "heart.rectangle"
		case .childrenOutdoor:    return "figure.and.child.holdinghands"
		case .childrenIndoor:     return "figure.child"
		case .productWhite:       return "cube.box"
		case .landscapeSunset:    return "sunset"
		case .landscapeNature:    return "leaf"
		case .foodStyling:        return "fork.knife.circle"
		case .groupPhoto:         return "person.3.sequence"
		case .waterScene:         return "water.waves"
		case .nightPortrait:      return "person.crop.rectangle.stack"
		case .silhouette:         return "figure"
		case .macroDetail:        return "magnifyingglass"
		case .texture:            return "circle.grid.3x3"
		}
	}
}

/// 环境光分析结果
struct LightAnalysis {
	/// 色温 (K)，范围约 2000-10000
	let colorTemperature: Float
	/// 亮度 (0-1)，0 为全黑，1 为过曝
	let brightness: Float
	/// 对比度 (0-1)，0 为完全平坦，1 为极高对比
	let contrast: Float
	/// 是否逆光场景
	let isBacklit: Bool
	/// 光源类型
	let lightType: LightType

	/// 光源类型枚举
	enum LightType: String, Codable {
		case natural      // 自然光
		case warm        // 暖光
		case cool        // 冷光
		case fluorescent // 荧光灯
		case mixed       // 混合光源
	}

	/// 默认环境光分析结果（中性值）
	static let `default` = LightAnalysis(
		colorTemperature: 5500,
		brightness: 0.5,
		contrast: 0.3,
		isBacklit: false,
		lightType: .natural
	)
}

/// 主体检测结果
struct SubjectDetection {
	/// 是否检测到人物
	let hasHuman: Bool
	/// 是否检测到动物
	let hasAnimal: Bool
	/// 是否检测到食物
	let hasFood: Bool
	/// 检测到的人物数量
	let humanCount: Int
	/// 主要主体在画面中的归一化矩形 [0,1]
	let mainSubjectRect: CGRect
	/// 主体类型描述
	let subjectType: String

	/// 默认主体检测结果
	static let `default` = SubjectDetection(
		hasHuman: false,
		hasAnimal: false,
		hasFood: false,
		humanCount: 0,
		mainSubjectRect: CGRect(x: 0.25, y: 0.25, width: 0.5, height: 0.5),
		subjectType: "未知"
	)
}

/// 场景自适应拍摄参数
struct AdaptiveCaptureParams: Codable {
	/// 目标 ISO 值
	let targetISO: Float
	/// 目标快门速度（秒）
	let targetShutterSpeed: Float
	/// 曝光补偿 (EV)
	let exposureBias: Float
	/// 白平衡色调偏移 (-1 到 1)
	let whiteBalanceTint: Float
	/// 白平衡色温 (K)
	let whiteBalanceTemperature: Float
	/// 建议变焦倍数
	let suggestedZoomFactor: CGFloat
	/// 建议镜头类型
	let suggestedLensType: String
	/// 闪光灯建议
	let flashMode: FlashRecommendation

	/// 闪光灯推荐模式
	enum FlashRecommendation: String, Codable {
		case auto  // 自动
		case on    // 开启
		case off   // 关闭
	}

	/// 默认拍摄参数
	static let defaultParams = AdaptiveCaptureParams(
		targetISO: 200,
		targetShutterSpeed: 1.0 / 120.0,
		exposureBias: 0.0,
		whiteBalanceTint: 0.0,
		whiteBalanceTemperature: 5500,
		suggestedZoomFactor: 1.0,
		suggestedLensType: "wide",
		flashMode: .auto
	)
}

// MARK: - 姿势建议系统

/// 姿势建议（用于姿势推荐系统）
struct PoseSuggestion: Codable {
	/// 建议类型
	let type: String
	/// 建议标题
	let title: String
	/// 建议描述
	let description: String
	/// 引导图片名称
	let guideImageName: String?
	/// 优先级
	let priority: String
	/// 操作指令列表
	let instructions: [String]
	/// 预计耗时
	let estimatedTime: String
}

// MARK: - 质量评估系统

/// 质量等级
enum QualityGrade: String, Codable {
	case excellent  // 极佳
	case good       // 良好
	case fair       // 一般
	case poor       // 较差

	/// 中文显示名称
	var displayName: String {
		switch self {
		case .excellent: return "极佳"
		case .good:      return "良好"
		case .fair:      return "一般"
		case .poor:      return "较差"
		}
	}
}

/// 质量评估（用于质量评估器）
struct QualityAssessment: Codable {
	/// 综合评分 (0-100)
	let overallScore: Float
	/// 清晰度评分
	let sharpnessScore: Float
	/// 噪声水平
	let noiseLevel: Float
	/// 曝光评分
	let exposureScore: Float
	/// 色彩和谐度评分
	let colorHarmonyScore: Float
	/// 分辨率评分
	let resolutionScore: Float
	/// 质量等级
	let qualityGrade: QualityGrade
	/// 评估时间戳
	let timestamp: Date
}

// MARK: - AI 增强建议系统

/// 增强类型
enum EnhancementType: String, Codable {
	case sharpness             // 锐度增强
	case noiseReduction        // 降噪
	case exposure              // 曝光调整
	case colorHarmony          // 色彩和谐
	case portraitEnhancement   // 人像增强
	case landscapeEnhancement  // 风景增强
	case nightOptimization     // 夜景优化
	case foodEnhancement       // 美食增强
}

/// 增强建议（用于 AI 增强顾问）
struct EnhancementSuggestion: Codable {
	/// 增强类型
	let type: EnhancementType
	/// 建议标题
	let title: String
	/// 建议描述
	let description: String
	/// 可调参数 (如 sharpen=20, noise_reduction=15, brightness=10 等)
	let parameters: [String: Float]
	/// 优先级
	let priority: String
}

// MARK: - 场景预设参数

/// 场景特定后期处理参数
struct ScenePresetParams: Codable {
	/// 曝光
	let exposure: Float
	/// 对比度
	let contrast: Float
	/// 饱和度
	let saturation: Float
	/// 高光
	let highlights: Float
	/// 阴影
	let shadows: Float
	/// 清晰度
	let clarity: Float
	/// 色温
	let warmth: Float
	/// 锐度
	let sharpness: Float
	/// 降噪
	let noiseReduction: Float
	/// 暗角
	let vignette: Float

	/// 默认预设参数
	static let `default` = ScenePresetParams(
		exposure: 0.0,
		contrast: 0.0,
		saturation: 0.0,
		highlights: 0.0,
		shadows: 0.0,
		clarity: 0.0,
		warmth: 0.0,
		sharpness: 0.0,
		noiseReduction: 0.0,
		vignette: 0.0
	)

	/// 根据场景类型获取预设参数
	static func preset(for scene: SceneType) -> ScenePresetParams {
		switch scene {
		case .portrait:
			return ScenePresetParams(
				exposure: 0.15, contrast: -0.05, saturation: 0.05,
				highlights: -0.1, shadows: 0.1, clarity: 0.1,
				warmth: 0.1, sharpness: 0.15, noiseReduction: 0.2, vignette: 0.1
			)
		case .portraitStanding:
			return ScenePresetParams(
				exposure: 0.1, contrast: 0.0, saturation: 0.05,
				highlights: -0.05, shadows: 0.1, clarity: 0.15,
				warmth: 0.1, sharpness: 0.2, noiseReduction: 0.15, vignette: 0.1
			)
		case .portraitSitting:
			return ScenePresetParams(
				exposure: 0.1, contrast: -0.05, saturation: 0.05,
				highlights: -0.1, shadows: 0.15, clarity: 0.1,
				warmth: 0.15, sharpness: 0.15, noiseReduction: 0.2, vignette: 0.15
			)
		case .food, .foodStyling:
			return ScenePresetParams(
				exposure: 0.2, contrast: 0.1, saturation: 0.2,
				highlights: -0.05, shadows: 0.05, clarity: 0.25,
				warmth: 0.15, sharpness: 0.25, noiseReduction: 0.1, vignette: 0.15
			)
		case .landscape, .landscapeNature:
			return ScenePresetParams(
				exposure: 0.0, contrast: 0.15, saturation: 0.15,
				highlights: -0.15, shadows: 0.15, clarity: 0.2,
				warmth: 0.0, sharpness: 0.2, noiseReduction: 0.1, vignette: 0.05
			)
		case .landscapeSunset:
			return ScenePresetParams(
				exposure: -0.1, contrast: 0.2, saturation: 0.25,
				highlights: -0.2, shadows: 0.2, clarity: 0.15,
				warmth: 0.3, sharpness: 0.15, noiseReduction: 0.1, vignette: 0.2
			)
		case .pet:
			return ScenePresetParams(
				exposure: 0.1, contrast: 0.05, saturation: 0.1,
				highlights: -0.05, shadows: 0.1, clarity: 0.15,
				warmth: 0.05, sharpness: 0.2, noiseReduction: 0.15, vignette: 0.1
			)
		case .architecture:
			return ScenePresetParams(
				exposure: 0.0, contrast: 0.15, saturation: 0.05,
				highlights: -0.1, shadows: 0.1, clarity: 0.25,
				warmth: 0.0, sharpness: 0.3, noiseReduction: 0.05, vignette: 0.0
			)
		case .nightScene, .nightPortrait:
			return ScenePresetParams(
				exposure: 0.3, contrast: 0.1, saturation: -0.05,
				highlights: -0.2, shadows: 0.3, clarity: 0.1,
				warmth: -0.05, sharpness: 0.1, noiseReduction: 0.4, vignette: 0.25
			)
		case .document:
			return ScenePresetParams(
				exposure: 0.25, contrast: 0.3, saturation: 0.0,
				highlights: -0.1, shadows: 0.1, clarity: 0.3,
				warmth: 0.0, sharpness: 0.35, noiseReduction: 0.1, vignette: 0.0
			)
		case .sunrise:
			return ScenePresetParams(
				exposure: -0.05, contrast: 0.15, saturation: 0.2,
				highlights: -0.2, shadows: 0.15, clarity: 0.15,
				warmth: 0.25, sharpness: 0.15, noiseReduction: 0.1, vignette: 0.15
			)
		case .snow:
			return ScenePresetParams(
				exposure: 0.3, contrast: 0.05, saturation: -0.1,
				highlights: -0.1, shadows: 0.1, clarity: 0.15,
				warmth: -0.1, sharpness: 0.2, noiseReduction: 0.15, vignette: 0.05
			)
		case .beach:
			return ScenePresetParams(
				exposure: 0.1, contrast: 0.1, saturation: 0.2,
				highlights: -0.15, shadows: 0.1, clarity: 0.2,
				warmth: 0.1, sharpness: 0.2, noiseReduction: 0.05, vignette: 0.0
			)
		case .flower, .macroDetail:
			return ScenePresetParams(
				exposure: 0.1, contrast: 0.05, saturation: 0.2,
				highlights: -0.1, shadows: 0.05, clarity: 0.25,
				warmth: 0.05, sharpness: 0.3, noiseReduction: 0.1, vignette: 0.2
			)
		case .stage:
			return ScenePresetParams(
				exposure: 0.15, contrast: 0.15, saturation: 0.1,
				highlights: -0.15, shadows: 0.2, clarity: 0.15,
				warmth: 0.05, sharpness: 0.15, noiseReduction: 0.2, vignette: 0.15
			)
		case .street:
			return ScenePresetParams(
				exposure: 0.0, contrast: 0.2, saturation: 0.1,
				highlights: -0.1, shadows: 0.15, clarity: 0.2,
				warmth: 0.0, sharpness: 0.2, noiseReduction: 0.1, vignette: 0.15
			)
		case .indoor:
			return ScenePresetParams(
				exposure: 0.15, contrast: 0.0, saturation: 0.05,
				highlights: -0.1, shadows: 0.15, clarity: 0.1,
				warmth: 0.1, sharpness: 0.15, noiseReduction: 0.2, vignette: 0.1
			)
		case .weddingOutdoor:
			return ScenePresetParams(
				exposure: 0.15, contrast: 0.0, saturation: 0.1,
				highlights: -0.1, shadows: 0.1, clarity: 0.1,
				warmth: 0.15, sharpness: 0.15, noiseReduction: 0.1, vignette: 0.15
			)
		case .weddingIndoor:
			return ScenePresetParams(
				exposure: 0.2, contrast: -0.05, saturation: 0.05,
				highlights: -0.15, shadows: 0.2, clarity: 0.1,
				warmth: 0.2, sharpness: 0.15, noiseReduction: 0.2, vignette: 0.2
			)
		case .childrenOutdoor:
			return ScenePresetParams(
				exposure: 0.15, contrast: 0.0, saturation: 0.15,
				highlights: -0.05, shadows: 0.1, clarity: 0.1,
				warmth: 0.1, sharpness: 0.15, noiseReduction: 0.1, vignette: 0.1
			)
		case .childrenIndoor:
			return ScenePresetParams(
				exposure: 0.2, contrast: -0.05, saturation: 0.1,
				highlights: -0.1, shadows: 0.15, clarity: 0.1,
				warmth: 0.15, sharpness: 0.15, noiseReduction: 0.15, vignette: 0.15
			)
		case .productWhite:
			return ScenePresetParams(
				exposure: 0.3, contrast: 0.1, saturation: 0.0,
				highlights: -0.05, shadows: 0.05, clarity: 0.25,
				warmth: 0.0, sharpness: 0.35, noiseReduction: 0.05, vignette: 0.0
			)
		case .groupPhoto:
			return ScenePresetParams(
				exposure: 0.15, contrast: 0.05, saturation: 0.1,
				highlights: -0.1, shadows: 0.1, clarity: 0.15,
				warmth: 0.1, sharpness: 0.2, noiseReduction: 0.15, vignette: 0.1
			)
		case .waterScene:
			return ScenePresetParams(
				exposure: 0.0, contrast: 0.1, saturation: 0.15,
				highlights: -0.15, shadows: 0.1, clarity: 0.2,
				warmth: -0.05, sharpness: 0.2, noiseReduction: 0.1, vignette: 0.05
			)
		case .silhouette:
			return ScenePresetParams(
				exposure: -0.3, contrast: 0.3, saturation: -0.1,
				highlights: -0.3, shadows: 0.3, clarity: 0.1,
				warmth: 0.05, sharpness: 0.1, noiseReduction: 0.15, vignette: 0.3
			)
		case .texture:
			return ScenePresetParams(
				exposure: 0.0, contrast: 0.2, saturation: 0.0,
				highlights: -0.1, shadows: 0.1, clarity: 0.35,
				warmth: 0.0, sharpness: 0.35, noiseReduction: 0.05, vignette: 0.05
			)
		case .unknown:
			return ScenePresetParams.default
		}
	}
}

// MARK: - 灵感库

/// 灵感库条目
struct InspirationEntry: Codable {
	/// 条目唯一标识
	let id: String
	/// 场景类型
	let scene: SceneType
	/// 风格
	let style: String
	/// 标题
	let title: String
	/// 描述
	let description: String
	/// 标签
	let tags: [String]
	/// 摄影师备注
	let photographerNote: String
}

// MARK: - 构图分析

/// 构图分析（扩展版）
struct CompositionAnalysis: Codable {
	/// 三分法评分 (0-100)
	let ruleOfThirdsScore: Float
	/// 对称性评分 (0-100)
	let symmetryScore: Float
	/// 视觉平衡评分 (0-100)
	let visualBalanceScore: Float
	/// 引导线数量
	let leadingLinesCount: Int
	/// 焦点数量
	let focalPointsCount: Int
	/// 构图类型
	let compositionType: String
	/// 构图反馈
	let feedback: String
}

#endif