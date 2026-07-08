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
	case unknown       // 未知

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
		case .unknown:       return "自动"
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
		case .unknown:       return "camera"
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

#endif