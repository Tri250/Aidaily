//
//  CompositionModels.swift
//  LiveCapture
//
//  构图引导系统数据模型
//
//  ## 文件作用
//  定义 AR 构图引导系统的所有数据模型
//  包括引导线类型、构图评分和姿势模板
//
//  ## 主要类型
//
//  ### CompositionGuideType 枚举
//  构图引导线类型，支持 5 种经典构图规则
//  - ruleOfThirds: 三分线
//  - goldenRatio: 黄金分割
//  - symmetry: 对称线
//  - centerFocus: 中心聚焦
//  - leadingLines: 引导线（对角线）
//
//  ### CompositionScore 结构体
//  多维度构图评分，0-100 分制
//  - overall: 综合评分
//  - ruleOfThirds: 三分法得分
//  - balance: 平衡性得分
//  - centering: 居中得分
//  - horizonLevel: 水平线得分
//  - feedback: 中文改进建议
//
//  ### PoseTemplate 结构体
//  姿势模板，支持多分类
//  - id: 唯一标识
//  - name: 模板名称
//  - category: 姿势分类（独照/情侣/朋友/家庭/宠物）
//  - overlayImageName: 叠加图片名称
//  - description: 模板描述
//  - tips: 拍摄技巧列表
//

import Foundation
import CoreGraphics

/// 构图引导线类型
enum CompositionGuideType: String, CaseIterable, Codable {
	case ruleOfThirds
	case goldenRatio
	case symmetry
	case centerFocus
	case leadingLines

	var displayName: String {
		switch self {
		case .ruleOfThirds: return "三分线"
		case .goldenRatio: return "黄金分割"
		case .symmetry: return "对称线"
		case .centerFocus: return "中心聚焦"
		case .leadingLines: return "引导线"
		}
	}

	var iconName: String {
		switch self {
		case .ruleOfThirds: return "grid.3x3"
		case .goldenRatio: return "spiral"
		case .symmetry: return "rectangle.split.2x1"
		case .centerFocus: return "scope"
		case .leadingLines: return "line.diagonal"
		}
	}
}

/// 构图评分
struct CompositionScore {
	/// 综合评分 0-100
	let overall: Float
	/// 三分法得分 0-100
	let ruleOfThirds: Float
	/// 平衡性得分 0-100
	let balance: Float
	/// 居中得分 0-100
	let centering: Float
	/// 水平线得分 0-100
	let horizonLevel: Float
	/// 改进建议（中文）
	let feedback: String

	/// 评分等级
	var grade: ScoreGrade {
		switch overall {
		case 90...100: return .excellent
		case 75..<90: return .good
		case 60..<75: return .fair
		case 0..<60: return .poor
		default: return .poor
		}
	}

	/// 评分等级
	enum ScoreGrade: String {
		case excellent = "优秀"
		case good = "良好"
		case fair = "一般"
		case poor = "需改进"

		var colorName: String {
			switch self {
			case .excellent: return "success"
			case .good: return "primary"
			case .fair: return "warning"
			case .poor: return "error"
			}
		}
	}
}

/// 姿势模板
struct PoseTemplate: Identifiable {
	let id: String
	let name: String
	let category: PoseCategory
	let overlayImageName: String
	let description: String
	let tips: [String]

	/// 姿势分类
	enum PoseCategory: String, CaseIterable, Codable {
		case solo
		case couple
		case friends
		case family
		case pet

		var displayName: String {
			switch self {
			case .solo: return "独照"
			case .couple: return "情侣"
			case .friends: return "朋友"
			case .family: return "家庭"
			case .pet: return "宠物"
			}
		}

		var iconName: String {
			switch self {
			case .solo: return "person.fill"
			case .couple: return "heart.fill"
			case .friends: return "person.3.fill"
			case .family: return "house.fill"
			case .pet: return "pawprint.fill"
			}
		}
	}
}