//
//  PoseRecommendationEngine.swift
//  LiveCapture
//
//  AI 姿态推荐引擎 - 基于场景分析提供智能姿态建议
//
//  ## 文件作用
//  基于场景类型、环境光分析和主体检测结果，从内置姿态数据库中
//  智能推荐最适合当前拍摄场景的姿态模板和调整建议
//
//  ## 主要类型
//
//  ### PoseDifficulty 枚举
//  姿态难度等级：beginner（初学者）、intermediate（中级）、advanced（高级）
//
//  ### PoseCategory 枚举
//  姿态分类：portraitStanding、portraitSitting、couple、children、
//  product、food、landscape、wedding
//
//  ### PoseTemplate 结构体
//  姿态模板，包含完整的关键点定义、拍摄技巧和变体
//
//  ### PoseSuggestion 结构体
//  动态调整建议，基于光环境、构图和主体分析生成
//
//  ### PoseRecommendationResult 结构体
//  推荐结果，包含场景类型、置信度、建议列表和推荐姿态
//
//  ### PoseRecommendationEngine 类
//  姿态推荐引擎主类，维护姿态数据库并提供推荐接口
//
//  ## 主要方法
//
//  ### 姿态推荐
//  - generateRecommendations(scene:confidence:subjectDetection:): 生成姿态推荐
//    策略:
//      - 根据场景类型匹配合适的姿态分类
//      - 根据主体检测结果筛选姿态（单人/双人/多人）
//      - 生成动态调整建议（逆光、构图、面部位置等）
//      - 根据置信度调整建议数量和优先级
//      - 低置信度时提供通用兜底建议
//
//  ### 动态建议
//  - generateDynamicSuggestions(scene:light:subject:): 基于环境分析生成动态调整建议
//  - generateBacklitSuggestions(): 逆光场景优化建议
//  - generateCompositionSuggestions(): 构图优化建议
//  - generateFacePositionSuggestions(subject:): 面部位置调整建议
//  - generateGroupSuggestions(humanCount:): 多人场景拍摄建议
//
//  ## 姿态数据库
//  包含 35 个完整姿态模板，覆盖 8 个分类：
//  - 人像站立：6 个姿态
//  - 人像坐姿：4 个姿态
//  - 情侣/双人：5 个姿态
//  - 儿童：4 个姿态
//  - 美食：4 个姿态
//  - 风景：4 个姿态
//  - 婚礼：4 个姿态
//  - 产品：4 个姿态
//

import Foundation
import CoreGraphics

#if os(iOS)

// MARK: - 姿态难度等级

/// 姿态难度等级
enum PoseDifficulty: String, CaseIterable, Codable {
	/// 初学者 - 简单自然的姿态，任何人都能轻松完成
	case beginner
	/// 中级 - 需要一定的身体控制力和表现力
	case intermediate
	/// 高级 - 需要较高的身体协调性和表现力，适合专业拍摄
	case advanced

	/// 难度中文显示名称
	var displayName: String {
		switch self {
		case .beginner:     return "初学者"
		case .intermediate: return "中级"
		case .advanced:     return "高级"
		}
	}

	/// SF Symbol 图标名称
	var iconName: String {
		switch self {
		case .beginner:     return "1.circle"
		case .intermediate: return "2.circle"
		case .advanced:     return "3.circle"
		}
	}
}

// MARK: - 姿态分类

/// 姿态分类
enum PoseCategory: String, CaseIterable, Codable {
	/// 人像站立姿态
	case portraitStanding
	/// 人像坐姿
	case portraitSitting
	/// 情侣/双人姿态
	case couple
	/// 儿童姿态
	case children
	/// 产品拍摄姿态
	case product
	/// 美食拍摄姿态
	case food
	/// 风景人像姿态
	case landscape
	/// 婚礼姿态
	case wedding

	/// 分类中文显示名称
	var displayName: String {
		switch self {
		case .portraitStanding: return "站立人像"
		case .portraitSitting:  return "坐姿人像"
		case .couple:           return "情侣/双人"
		case .children:         return "儿童"
		case .product:          return "产品"
		case .food:             return "美食"
		case .landscape:        return "风景人像"
		case .wedding:          return "婚礼"
		}
	}

	/// SF Symbol 图标名称
	var iconName: String {
		switch self {
		case .portraitStanding: return "person.fill"
		case .portraitSitting:  return "person.crop.rectangle"
		case .couple:           return "person.2.fill"
		case .children:         return "figure.child"
		case .product:          return "cube.box"
		case .food:             return "fork.knife"
		case .landscape:        return "mountain.2"
		case .wedding:          return "heart.fill"
		}
	}
}

// MARK: - 姿态模板

/// 姿态模板 - 定义完整的拍摄姿态方案
struct PoseTemplate: Codable {
	/// 姿态唯一标识
	let id: String
	/// 姿态名称
	let name: String
	/// 姿态详细描述
	let description: String
	/// 所属分类
	let category: PoseCategory
	/// 难度等级
	let difficulty: PoseDifficulty
	/// 关键点映射（身体部位 → 位置描述）
	let keypoints: [String: String]
	/// 姿态变体列表
	let variations: [String]
	/// 拍摄技巧列表
	let tips: [String]
	/// 引导图片名称（可选）
	let guideImageName: String?
}

// MARK: - 姿态建议

/// 动态调整建议
struct PoseSuggestion: Codable {
	/// 建议类型标识
	let type: String
	/// 建议标题
	let title: String
	/// 建议详细描述
	let description: String
	/// 引导图片名称（可选）
	let guideImageName: String?
	/// 优先级
	let priority: Priority
	/// 操作步骤说明
	let instructions: [String]
	/// 预计调整时间
	let estimatedTime: String

	/// 建议优先级
	enum Priority: String, CaseIterable, Codable {
		/// 高优先级 - 建议立即调整
		case high
		/// 中优先级 - 建议在拍摄前调整
		case medium
		/// 低优先级 - 可选调整
		case low

		/// 优先级中文显示名称
		var displayName: String {
			switch self {
			case .high:   return "高"
			case .medium: return "中"
			case .low:    return "低"
			}
		}
	}
}

// MARK: - 推荐结果

/// 姿态推荐结果
struct PoseRecommendationResult: Codable {
	/// 识别到的场景类型
	let sceneType: SceneType
	/// 场景识别置信度 (0.0 - 1.0)
	let confidence: Float
	/// 动态调整建议列表
	let suggestions: [PoseSuggestion]
	/// 推荐的最佳姿态
	let recommendedPose: PoseTemplate
}

// MARK: - 姿态推荐引擎

/// 姿态推荐引擎 - 基于场景分析提供智能姿态建议
final class PoseRecommendationEngine {

	// MARK: - 场景到姿态分类的映射规则

	/// 场景类型到姿态分类的映射表
	private let sceneToCategoryMapping: [SceneType: [PoseCategory]] = [
		.portrait:           [.portraitStanding, .portraitSitting, .couple],
		.portraitStanding:   [.portraitStanding],
		.portraitSitting:    [.portraitSitting],
		.food:               [.food],
		.foodStyling:        [.food],
		.landscape:          [.landscape],
		.landscapeSunset:    [.landscape],
		.landscapeNature:    [.landscape],
		.pet:                [.portraitStanding, .portraitSitting],
		.architecture:       [.landscape],
		.nightScene:         [.portraitStanding, .landscape],
		.nightPortrait:      [.portraitStanding],
		.document:           [.product],
		.sunrise:            [.landscape],
		.snow:               [.landscape, .portraitStanding],
		.beach:              [.landscape, .portraitStanding],
		.flower:             [.portraitStanding, .product],
		.stage:              [.portraitStanding],
		.street:             [.portraitStanding, .landscape],
		.indoor:             [.portraitStanding, .portraitSitting, .product],
		.weddingOutdoor:     [.wedding],
		.weddingIndoor:      [.wedding],
		.childrenOutdoor:    [.children],
		.childrenIndoor:     [.children],
		.productWhite:       [.product],
		.groupPhoto:         [.couple],
		.waterScene:         [.landscape],
		.silhouette:         [.landscape],
		.macroDetail:        [.product],
		.texture:            [.product],
		.unknown:            [.portraitStanding, .portraitSitting]
	]

	// MARK: - 姿态数据库

	/// 完整姿态数据库
	private lazy var poseDatabase: [PoseTemplate] = {
		return buildPoseDatabase()
	}()

	// MARK: - 公开方法

	/// 生成姿态推荐结果
	/// - Parameters:
	///   - scene: 识别到的场景类型
	///   - confidence: 场景识别置信度
	///   - subjectDetection: 主体检测结果
	/// - Returns: 姿态推荐结果
	func generateRecommendations(
		scene: SceneType,
		confidence: Float,
		subjectDetection: SubjectDetection
	) -> PoseRecommendationResult {
		// 根据场景类型获取候选姿态分类
		let categories = sceneToCategoryMapping[scene] ?? [.portraitStanding]

		// 根据主体检测结果筛选姿态
		let filteredPoses = filterPosesBySubject(
			categories: categories,
			subjectDetection: subjectDetection,
			confidence: confidence
		)

		// 选择最佳推荐姿态
		let recommendedPose: PoseTemplate
		if let bestPose = filteredPoses.first {
			recommendedPose = bestPose
		} else {
			// 低置信度兜底：使用自然站立姿态
			recommendedPose = fallbackPose()
		}

		// 生成动态调整建议
		let suggestions = generateDynamicSuggestions(
			scene: scene,
			confidence: confidence,
			subjectDetection: subjectDetection
		)

		return PoseRecommendationResult(
			sceneType: scene,
			confidence: confidence,
			suggestions: suggestions,
			recommendedPose: recommendedPose
		)
	}

	/// 获取所有可用姿态
	func allPoses() -> [PoseTemplate] {
		return poseDatabase
	}

	/// 根据分类获取姿态列表
	/// - Parameter category: 姿态分类
	/// - Returns: 该分类下的所有姿态
	func poses(for category: PoseCategory) -> [PoseTemplate] {
		return poseDatabase.filter { $0.category == category }
	}

	/// 根据难度获取姿态列表
	/// - Parameter difficulty: 难度等级
	/// - Returns: 该难度下的所有姿态
	func poses(for difficulty: PoseDifficulty) -> [PoseTemplate] {
		return poseDatabase.filter { $0.difficulty == difficulty }
	}

	// MARK: - 姿态筛选

	/// 根据主体检测结果筛选姿态
	private func filterPosesBySubject(
		categories: [PoseCategory],
		subjectDetection: SubjectDetection,
		confidence: Float
	) -> [PoseTemplate] {
		var filtered = poseDatabase.filter { categories.contains($0.category) }

		// 根据人物数量过滤
		if subjectDetection.hasHuman {
			if subjectDetection.humanCount == 1 {
				// 单人：排除情侣/双人姿态
				filtered = filtered.filter { $0.category != .couple }
			} else if subjectDetection.humanCount >= 2 {
				// 多人：优先推荐情侣/双人姿态，保留其他人像姿态
				let couplePoses = filtered.filter { $0.category == .couple }
				if !couplePoses.isEmpty {
					// 将情侣姿态排在前面
					let otherPoses = filtered.filter { $0.category != .couple }
					filtered = couplePoses + otherPoses
				}
			}
		}

		// 根据置信度调整候选数量
		if confidence < 0.5 {
			// 低置信度：只保留低难度姿态
			filtered = filtered.filter { $0.difficulty == .beginner }
		} else if confidence < 0.7 {
			// 中等置信度：保留初级和中级
			filtered = filtered.filter { $0.difficulty != .advanced }
		}

		return filtered
	}

	/// 低置信度兜底姿态
	private func fallbackPose() -> PoseTemplate {
		return poseDatabase.first { $0.id == "naturalStanding" } ?? poseDatabase[0]
	}

	// MARK: - 动态建议生成

	/// 生成动态调整建议
	/// - Parameters:
	///   - scene: 场景类型
	///   - confidence: 置信度
	///   - subjectDetection: 主体检测结果
	/// - Returns: 动态建议列表
	private func generateDynamicSuggestions(
		scene: SceneType,
		confidence: Float,
		subjectDetection: SubjectDetection
	) -> [PoseSuggestion] {
		var suggestions: [PoseSuggestion] = []

		// 根据主体检测生成建议
		if subjectDetection.hasHuman {
			// 面部位置分析建议
			suggestions.append(contentsOf: generateFacePositionSuggestions(subject: subjectDetection))

			// 多人场景建议
			if subjectDetection.humanCount >= 2 {
				suggestions.append(contentsOf: generateGroupSuggestions(humanCount: subjectDetection.humanCount))
			}
		}

		// 构图优化建议
		suggestions.append(contentsOf: generateCompositionSuggestions(subject: subjectDetection, scene: scene))

		// 根据置信度限制建议数量
		let maxSuggestions: Int
		if confidence > 0.8 {
			maxSuggestions = 5
		} else if confidence > 0.5 {
			maxSuggestions = 3
		} else {
			maxSuggestions = 2
		}

		// 按优先级排序并限制数量
		let sorted = suggestions.sorted { a, b in
			let priorityOrder: [PoseSuggestion.Priority: Int] = [.high: 0, .medium: 1, .low: 2]
			return (priorityOrder[a.priority] ?? 99) < (priorityOrder[b.priority] ?? 99)
		}

		return Array(sorted.prefix(maxSuggestions))
	}

	/// 生成逆光场景优化建议
	/// - Returns: 逆光优化建议列表
	func generateBacklitSuggestions() -> [PoseSuggestion] {
		return [
			PoseSuggestion(
				type: "backlightOptimization",
				title: "逆光优化",
				description: "检测到逆光场景，建议调整拍摄角度或使用补光设备，以获得更好的主体曝光效果",
				guideImageName: "backlight_guide",
				priority: .high,
				instructions: [
					"调整拍摄角度，使主体背对光源，形成轮廓光效果",
					"使用反光板或闪光灯对主体面部进行补光",
					"适当增加曝光补偿 (+0.7 至 +1.3 EV)",
					"尝试使用 HDR 模式平衡明暗区域"
				],
				estimatedTime: "30秒"
			),
			PoseSuggestion(
				type: "silhouettePose",
				title: "剪影姿态建议",
				description: "逆光场景适合拍摄剪影效果，建议采用侧身或伸展姿态以突出轮廓线条",
				guideImageName: "silhouette_pose_guide",
				priority: .medium,
				instructions: [
					"采用侧身站立姿态，突出身体轮廓",
					"伸展手臂或腿部，增加剪影的动感线条",
					"保持身体与背景的清晰分离",
					"避免穿着过于宽松的衣物"
				],
				estimatedTime: "20秒"
			)
		]
	}

	/// 生成构图优化建议
	/// - Parameters:
	///   - subject: 主体检测结果
	///   - scene: 场景类型
	/// - Returns: 构图优化建议列表
	private func generateCompositionSuggestions(
		subject: SubjectDetection,
		scene: SceneType
	) -> [PoseSuggestion] {
		var suggestions: [PoseSuggestion] = []

		// 主体位置分析
		let centerX = subject.mainSubjectRect.midX
		let centerY = subject.mainSubjectRect.midY
		let subjectWidth = subject.mainSubjectRect.width
		let subjectHeight = subject.mainSubjectRect.height

		// 主体偏左或偏右
		if centerX < 0.35 || centerX > 0.65 {
			suggestions.append(PoseSuggestion(
				type: "compositionCentering",
				title: "构图优化",
				description: "主体偏离画面中心，建议使用三分法构图重新调整主体位置，使画面更加平衡",
				guideImageName: "composition_rule_of_thirds",
				priority: .medium,
				instructions: [
					"将主体放置在画面三分线的交叉点附近",
					"为主体视线方向留出更多空间",
					"调整拍摄角度，使主体处于黄金分割位置",
					"使用网格线辅助构图"
				],
				estimatedTime: "15秒"
			))
		}

		// 主体占比过大
		if subjectWidth > 0.7 || subjectHeight > 0.8 {
			suggestions.append(PoseSuggestion(
				type: "compositionTooClose",
				title: "构图优化",
				description: "主体在画面中占比过大，建议适当后退或调整焦距，为主体周围留出呼吸空间",
				guideImageName: "composition_breathing_room",
				priority: .medium,
				instructions: [
					"后退一步或使用更广角的镜头",
					"确保主体周围有适当的留白空间",
					"在主体上方保留约 10-15% 的头顶空间"
				],
				estimatedTime: "10秒"
			))
		}

		// 主体占比过小
		if subjectWidth < 0.2 && subjectHeight < 0.3 {
			suggestions.append(PoseSuggestion(
				type: "compositionTooFar",
				title: "构图优化",
				description: "主体在画面中占比过小，建议靠近拍摄或使用长焦镜头，突出主体",
				guideImageName: "composition_fill_frame",
				priority: .medium,
				instructions: [
					"靠近主体或使用更长的焦距",
					"确保主体占据画面的 30-50%",
					"去除画面中分散注意力的元素"
				],
				estimatedTime: "10秒"
			))
		}

		return suggestions
	}

	/// 生成面部位置调整建议
	/// - Parameter subject: 主体检测结果
	/// - Returns: 面部位置调整建议列表
	private func generateFacePositionSuggestions(subject: SubjectDetection) -> [PoseSuggestion] {
		var suggestions: [PoseSuggestion] = []

		let centerY = subject.mainSubjectRect.midY

		// 面部位置偏高
		if centerY < 0.35 {
			suggestions.append(PoseSuggestion(
				type: "facePositionHigh",
				title: "头部位置调整",
				description: "主体面部位置偏上，建议适当调整构图使面部位于画面中上部三分之一处",
				guideImageName: "face_position_guide",
				priority: .medium,
				instructions: [
					"将面部放置在画面上方三分之一处",
					"确保下巴以下有足够的身体空间",
					"略微低头或调整拍摄角度"
				],
				estimatedTime: "10秒"
			))
		}

		// 面部位置偏低
		if centerY > 0.65 {
			suggestions.append(PoseSuggestion(
				type: "facePositionLow",
				title: "头部位置调整",
				description: "主体面部位置偏下，建议抬高拍摄角度或引导主体略微抬头，获得更好的面部表现",
				guideImageName: "face_position_guide",
				priority: .medium,
				instructions: [
					"降低拍摄角度或引导主体抬头",
					"确保面部位于画面上部三分之一处",
					"避免双下巴效果，保持颈部伸展"
				],
				estimatedTime: "10秒"
			))
		}

		return suggestions
	}

	/// 生成多人场景拍摄建议
	/// - Parameter humanCount: 检测到的人物数量
	/// - Returns: 多人场景建议列表
	private func generateGroupSuggestions(humanCount: Int) -> [PoseSuggestion] {
		var suggestions: [PoseSuggestion] = []

		if humanCount == 2 {
			suggestions.append(PoseSuggestion(
				type: "couplePoseSuggestion",
				title: "双人姿态建议",
				description: "检测到双人场景，推荐使用情侣/双人姿态模板，营造温馨自然的互动氛围",
				guideImageName: "couple_pose_guide",
				priority: .high,
				instructions: [
					"引导两人保持自然互动，避免僵硬站立",
					"注意两人之间的身高差和距离",
					"利用眼神交流和肢体接触增加亲密感",
					"尝试不同角度捕捉自然瞬间"
				],
				estimatedTime: "1分钟"
			))
		} else if humanCount >= 3 {
			suggestions.append(PoseSuggestion(
				type: "groupPoseSuggestion",
				title: "多人合影建议",
				description: "检测到多人场景，建议采用错落有致的排列方式，避免单调的直线排列",
				guideImageName: "group_pose_guide",
				priority: .high,
				instructions: [
					"采用三角构图排列人物，避免直线排列",
					"高个子站在后面，矮个子或儿童站在前面",
					"确保每个人的面部都清晰可见",
					"引导大家保持自然放松的表情"
				],
				estimatedTime: "2分钟"
			))
		}

		return suggestions
	}

	// MARK: - 姿态数据库构建

	/// 构建完整姿态数据库
	/// - Returns: 所有姿态模板的数组
	private func buildPoseDatabase() -> [PoseTemplate] {
		var poses: [PoseTemplate] = []

		// MARK: 人像站立姿态

		// 1. 自然站立
		poses.append(PoseTemplate(
			id: "naturalStanding",
			name: "自然站立",
			description: "最基础的人像站立姿态，适合日常拍摄。主体自然站立，双手自然垂放，面对镜头微笑，适合所有人快速上手。",
			category: .portraitStanding,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松，双肩齐平",
				"head": "正面朝向相机，略微上扬 5 度",
				"arms": "自然垂放于身体两侧",
				"legs": "与肩同宽，自然站立",
				"hands": "手指自然放松，微微弯曲",
				"back": "挺直但不僵硬，保持自然弧度",
				"eyeContact": "直视镜头",
				"bodyAngle": "正对相机",
				"hips": "自然居中",
				"feet": "与肩同宽，脚尖略微外八",
				"chin": "水平，略微前伸避免双下巴",
				"gaze": "看向镜头中心"
			],
			variations: [
				"单手插口袋：将一只手自然放入口袋，增加休闲感",
				"微侧身：身体略微旋转 15 度，增加立体感",
				"双手背后：双手自然交叠于背后，显得自信大方"
			],
			tips: [
				"保持肩膀放松下沉，避免耸肩造成的紧张感",
				"重心放在后脚，前脚略微前伸，营造轻松感",
				"下巴微收并前伸，可以有效避免双下巴",
				"自然呼吸，在呼气时按下快门，获得最放松的表情",
				"想象头顶有一根线轻轻向上拉，保持优雅姿态"
			],
			guideImageName: "pose_natural_standing"
		))

		// 2. 自信姿态
		poses.append(PoseTemplate(
			id: "confidentPose",
			name: "自信姿态",
			description: "展现自信气场的站立姿态，适合商务人像和个人形象照。双手叉腰或交叉于胸前，挺胸收腹，展现强大气场。",
			category: .portraitStanding,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "微微后展，展现胸廓",
				"head": "略微上扬 10 度，展现自信",
				"arms": "双手叉腰，肘部向外打开",
				"legs": "一脚前一脚后，重心在后脚",
				"hands": "双手叉腰，虎口卡在髋骨上方",
				"back": "挺直，肩胛骨微微收紧",
				"eyeContact": "直视镜头，眼神坚定",
				"bodyAngle": "身体略微侧转 20 度，面部正对镜头",
				"hips": "略微向一侧倾斜",
				"feet": "前脚脚尖指向镜头，后脚横放",
				"chin": "略微抬起，展现下颌线",
				"gaze": "坚定地看向镜头"
			],
			variations: [
				"双臂交叉：双手交叉抱于胸前，展现权威感",
				"单手扶墙：一手自然扶墙，身体略带倾斜",
				"坐姿靠桌：半坐在桌边，一手撑桌面"
			],
			tips: [
				"深呼吸挺胸，让胸腔充分展开",
				"眼神要坚定但不咄咄逼人，保持自然微笑",
				"肘部向外打开时注意角度，不要遮挡面部",
				"前脚可以略微踮起脚尖，增加腿部线条",
				"拍摄角度略微仰拍 5-10 度，增强气场"
			],
			guideImageName: "pose_confident"
		))

		// 3. 休闲倚靠
		poses.append(PoseTemplate(
			id: "casualLean",
			name: "休闲倚靠",
			description: "轻松自然的倚靠姿态，适合街拍和日常写真。身体倚靠墙壁或栏杆，营造慵懒随性的氛围。",
			category: .portraitStanding,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "一侧肩膀倚靠支撑物，自然放松",
				"head": "略微侧向倚靠方向，或转向镜头",
				"arms": "一侧手臂自然垂放，另一侧可扶支撑物",
				"legs": "一条腿承重，另一条腿弯曲交叉",
				"hands": "一手自然下垂，一手可扶支撑物或插口袋",
				"back": "略微弯曲，贴合支撑物弧度",
				"eyeContact": "可以看向镜头或看向远方",
				"bodyAngle": "身体侧向支撑物，约 30-45 度",
				"hips": "向承重腿一侧偏移",
				"feet": "承重脚平放，另一脚脚尖点地",
				"chin": "自然放松",
				"gaze": "看向镜头或远方，营造不经意感"
			],
			variations: [
				"单手撑墙：一手撑墙，身体略微倾斜",
				"背靠栏杆：背部倚靠栏杆，双手自然搭在栏杆上",
				"侧坐台阶：侧坐在台阶上，一条腿弯曲一条腿伸直"
			],
			tips: [
				"确保倚靠自然不僵硬，身体重量真正放在支撑物上",
				"交叉腿时注意不要遮挡另一条腿的线条",
				"利用支撑物创造画面中的引导线",
				"选择有质感的墙壁或栏杆作为背景",
				"拍摄时可以从侧面捕捉，增加故事感"
			],
			guideImageName: "pose_casual_lean"
		))

		// 4. 动态动作
		poses.append(PoseTemplate(
			id: "dynamicMovement",
			name: "动态动作",
			description: "充满活力的动态姿态，适合时尚拍摄和创意人像。通过行走、转身或跳跃等动作，捕捉瞬间的动感之美。",
			category: .portraitStanding,
			difficulty: .advanced,
			keypoints: [
				"shoulders": "自然摆动，跟随身体动势",
				"head": "跟随身体运动方向，或回头看向镜头",
				"arms": "自然摆动或做出舒展动作",
				"legs": "一前一后，做出行走或跳跃动作",
				"hands": "自然张开或握拳，配合动作韵律",
				"back": "保持动态平衡，略微前倾或后仰",
				"eyeContact": "看向运动方向或回头看向镜头",
				"bodyAngle": "身体朝向运动方向",
				"hips": "随步伐自然扭动",
				"feet": "一脚着地一脚抬起，展现动态",
				"chin": "自然抬起，朝向运动方向",
				"gaze": "向前看或回眸"
			],
			variations: [
				"回眸一笑：向前走几步后回头看向镜头",
				"甩发动作：快速转头，让头发飘起",
				"跳跃瞬间：原地轻跳，捕捉悬空瞬间"
			],
			tips: [
				"使用连拍模式捕捉最佳瞬间",
				"快门速度不低于 1/500s 以冻结动作",
				"提前预判动作轨迹，预留构图空间",
				"动作幅度不宜过大，保持面部表情自然",
				"可以配合裙摆、围巾等飘逸元素增加动感"
			],
			guideImageName: "pose_dynamic_movement"
		))

		// 5. 回眸侧身
		poses.append(PoseTemplate(
			id: "overShoulder",
			name: "回眸侧身",
			description: "经典的侧身回眸姿态，背对镜头然后转头回望，营造不经意的抓拍感，适合街拍和旅行写真。",
			category: .portraitStanding,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "一侧肩膀朝向镜头，另一侧在后方",
				"head": "转向镜头方向，角度约 90 度",
				"arms": "自然垂放或一手轻抚头发",
				"legs": "略微前后分开，重心在后脚",
				"hands": "可以轻抚头发或自然垂放",
				"back": "挺直，展现背部线条",
				"eyeContact": "回眸看向镜头",
				"bodyAngle": "身体背对镜头约 135 度，头部转回",
				"hips": "略微向镜头方向扭转",
				"feet": "指向远离镜头的方向",
				"chin": "转向镜头时自然抬起",
				"gaze": "回眸看向镜头，眼神略带惊喜"
			],
			variations: [
				"手扶帽檐：回眸时一手轻扶帽檐，增加时尚感",
				"撩发回眸：回眸时一手撩起头发",
				"半遮面回眸：用围巾或衣领半遮面部"
			],
			tips: [
				"转头时保持颈部线条优美，不要过度扭转",
				"回眸时眼神要自然，仿佛刚刚注意到镜头",
				"身体背对角度越大，回眸效果越有戏剧性",
				"注意头发不要遮挡面部",
				"利用侧光或逆光增加轮廓光效果"
			],
			guideImageName: "pose_over_shoulder"
		))

		// 6. 行走姿态
		poses.append(PoseTemplate(
			id: "walkingPose",
			name: "行走姿态",
			description: "自然行走中的抓拍姿态，适合街拍、旅行和日常记录。在行走过程中捕捉最自然的体态和表情。",
			category: .portraitStanding,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松，随步伐轻微摆动",
				"head": "自然面向前方或略微看向镜头",
				"arms": "自然前后摆动，幅度适中",
				"legs": "一前一后，做出自然行走动作",
				"hands": "自然半握拳或完全放松",
				"back": "保持挺直，不要弯腰驼背",
				"eyeContact": "可以看向前方、地面或镜头",
				"bodyAngle": "身体朝向行走方向",
				"hips": "随步伐自然摆动",
				"feet": "交替前进，步幅自然",
				"chin": "自然抬起，保持水平",
				"gaze": "看向前方或不经意看向镜头"
			],
			variations: [
				"低头行走：低头看路，营造忧郁文艺感",
				"牵手行走：两人牵手并肩行走",
				"奔跑行走：快步行走，增加活力和动感"
			],
			tips: [
				"让模特真正行走而不是摆拍，用连拍捕捉最佳瞬间",
				"步伐要比平时稍慢，方便相机捕捉",
				"手臂摆动幅度适中，不要过于夸张",
				"看向前方时眼神要柔和，不要紧张",
				"选择有趣的背景，让行走路线形成引导线"
			],
			guideImageName: "pose_walking"
		))

		// MARK: 人像坐姿

		// 7. 正式坐姿
		poses.append(PoseTemplate(
			id: "formalSitting",
			name: "正式坐姿",
			description: "端庄正式的坐姿，适合商务人像和正式场合。挺直腰背，双腿并拢或略微交叉，展现专业形象。",
			category: .portraitSitting,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "后展下沉，保持端正",
				"head": "正面朝向相机，略微上扬",
				"arms": "双手自然放在膝盖上或桌面上",
				"legs": "双腿并拢或优雅地交叉",
				"hands": "双手交叠放在膝盖上或自然平放",
				"back": "挺直，不靠椅背",
				"eyeContact": "直视镜头",
				"bodyAngle": "正对镜头或略微侧转 15 度",
				"hips": "坐于椅子前三分之一处",
				"feet": "双腿并拢，脚踝可交叉",
				"chin": "水平，略微前伸",
				"gaze": "自信地看向镜头"
			],
			variations: [
				"侧坐扶手椅：侧坐在扶手椅上，一手搭在扶手上",
				"桌前坐姿：双手交叠放在桌面上，身体略微前倾",
				"交叉腿坐姿：双腿在膝盖处交叉，显得优雅"
			],
			tips: [
				"只坐椅子前三分之一，保持腰背挺直",
				"双腿向一侧倾斜可以拉长腿部线条",
				"手部动作要轻柔优雅，避免紧握拳头",
				"略微前倾可以增加亲和力",
				"选择简洁的背景突出主体"
			],
			guideImageName: "pose_formal_sitting"
		))

		// 8. 放松坐姿
		poses.append(PoseTemplate(
			id: "relaxedSitting",
			name: "放松坐姿",
			description: "轻松惬意的坐姿，适合咖啡厅、居家等休闲场景。身体微微后靠，手臂自然搭放，散发慵懒气息。",
			category: .portraitSitting,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松，略微下沉",
				"head": "略微倾斜，增添亲切感",
				"arms": "一手搭在扶手或椅背上，一手自然垂放",
				"legs": "一条腿弯曲，另一条腿自然伸展",
				"hands": "自然放松，可手持咖啡杯或道具",
				"back": "轻轻靠在椅背上",
				"eyeContact": "看向镜头或看向窗外",
				"bodyAngle": "略微侧坐，30-45 度",
				"hips": "自然坐于椅中",
				"feet": "一腿弯曲踩地，一腿伸展",
				"chin": "自然放松",
				"gaze": "柔和地看向镜头或远方"
			],
			variations: [
				"手持咖啡杯：一手持杯，增添生活气息",
				"托腮沉思：一手托腮，营造文艺氛围",
				"侧坐沙发：整个身体侧坐在沙发上，双腿收拢"
			],
			tips: [
				"保持自然的身体曲线，不要刻意摆姿势",
				"利用道具（抱枕、书籍、咖啡杯）增加生活感",
				"光线从窗户一侧射入，营造柔和氛围",
				"腿部姿态要自然，避免僵硬并拢",
				"表情放松，可以略带微笑或不笑"
			],
			guideImageName: "pose_relaxed_sitting"
		))

		// 9. 创意坐姿
		poses.append(PoseTemplate(
			id: "creativeSitting",
			name: "创意坐姿",
			description: "富有艺术感的创意坐姿，适合时尚拍摄和个性写真。打破常规坐姿，通过独特的肢体造型展现个性。",
			category: .portraitSitting,
			difficulty: .advanced,
			keypoints: [
				"shoulders": "一侧下沉，一侧抬高，创造不对称美感",
				"head": "偏向一侧，或仰头向上",
				"arms": "一手撑地，一手向上伸展或抚面",
				"legs": "一条腿弯曲立起，另一条腿侧放",
				"hands": "做出有表现力的手势",
				"back": "略微弯曲或后仰，创造曲线",
				"eyeContact": "可以不看镜头，增加神秘感",
				"bodyAngle": "身体扭曲成 S 形曲线",
				"hips": "一侧着地，另一侧抬起",
				"feet": "脚尖绷直或自然弯曲",
				"chin": "仰起或偏向一侧",
				"gaze": "看向上方或远方"
			],
			variations: [
				"跪坐造型：双腿折叠跪坐，身体后仰单手撑地",
				"侧卧撑坐：侧身坐地，一手撑地一手抚发",
				"蜷缩造型：双腿收拢环抱，营造私密感"
			],
			tips: [
				"注意身体线条的流畅性，避免关节处出现直角",
				"利用光影突出身体曲线",
				"面部表情要与姿态的戏剧性相匹配",
				"拍摄角度可以多尝试俯拍或仰拍",
				"衣着选择修身款式以展现身体线条"
			],
			guideImageName: "pose_creative_sitting"
		))

		// 10. 地面坐姿
		poses.append(PoseTemplate(
			id: "groundSitting",
			name: "地面坐姿",
			description: "自然随意地坐在地面上的姿态，适合户外草地、沙滩、台阶等场景。亲近自然，展现真实自然的一面。",
			category: .portraitSitting,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松",
				"head": "略微偏向一侧或看向镜头",
				"arms": "双手撑在身后或环抱膝盖",
				"legs": "双腿弯曲或一条腿伸直一条弯曲",
				"hands": "自然放在膝盖上或撑在身后",
				"back": "略微弯曲或挺直",
				"eyeContact": "看向镜头或看向远方",
				"bodyAngle": "正面或侧面对镜头",
				"hips": "坐于地面，臀部着地",
				"feet": "赤脚或穿鞋，自然摆放",
				"chin": "自然放松",
				"gaze": "看向镜头微笑或看向远方"
			],
			variations: [
				"环抱膝盖：双腿弯曲，双手环抱膝盖",
				"侧坐双腿：双腿并拢侧向一边",
				"盘腿坐：双腿交叉盘坐，双手放在膝盖上"
			],
			tips: [
				"选择干净平整的地面或铺上垫子/外套",
				"从低角度拍摄可以获得更自然的视角",
				"注意腿部摆放，避免走光",
				"利用周围环境（落叶、花瓣、草地）增加氛围",
				"自然光下拍摄效果最佳，避免正午强光"
			],
			guideImageName: "pose_ground_sitting"
		))

		// MARK: 情侣/双人姿态

		// 11. 面对面
		poses.append(PoseTemplate(
			id: "facingEachOther",
			name: "面对面",
			description: "经典的面对面情侣姿态，两人四目相对，展现最真挚的情感交流。适合情侣写真和婚纱照。",
			category: .couple,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "两人肩膀相对，距离约 30-50cm",
				"head": "两人头部略微倾向对方",
				"arms": "一人手臂搭在对方肩上或腰间",
				"legs": "两人脚步自然分开，保持稳定",
				"hands": "牵手或轻抚对方脸颊",
				"back": "保持自然弧度",
				"eyeContact": "深情对视，眼神交流",
				"bodyAngle": "两人身体正对彼此",
				"hips": "自然放松",
				"feet": "两人脚步略微交错",
				"chin": "略微抬起，看向对方",
				"gaze": "温柔地注视对方眼睛"
			],
			variations: [
				"额头相贴：两人额头轻触，闭眼微笑",
				"鼻尖相触：鼻子轻轻碰在一起，俏皮温馨",
				"牵手对视：双手牵在一起，深情对视"
			],
			tips: [
				"引导两人自然交谈，捕捉真实的情感瞬间",
				"注意身高差，高的一方可以略微低头",
				"手的姿态要自然，不要用力过猛",
				"利用侧光增强两人之间的轮廓和层次",
				"拍摄时保持一定距离，给两人私密空间"
			],
			guideImageName: "pose_facing_each_other"
		))

		// 12. 并肩而立
		poses.append(PoseTemplate(
			id: "sideBySide",
			name: "并肩而立",
			description: "两人并肩站立的姿态，适合正式合照和日常合影。简洁大方，展现两人之间的默契与陪伴。",
			category: .couple,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "两人肩膀靠近但不紧贴",
				"head": "两人头部略微倾向对方",
				"arms": "内侧手臂可以搂住对方腰部或肩膀",
				"legs": "自然站立，与肩同宽",
				"hands": "外侧手自然垂放，内侧手搂住对方",
				"back": "挺直自然",
				"eyeContact": "都看向镜头",
				"bodyAngle": "两人正面朝向镜头",
				"hips": "自然居中",
				"feet": "与肩同宽",
				"chin": "自然水平",
				"gaze": "一起看向镜头"
			],
			variations: [
				"一前一后：一人站在另一人身后，手臂环抱前方的人",
				"搭肩并立：一人手臂搭在另一人肩上",
				"牵手并立：两人手牵手并肩站立"
			],
			tips: [
				"两人之间保持适当距离，不要太远也不要太挤",
				"内侧手臂搭放要自然，不要用力抓握",
				"注意两人的服装颜色搭配协调",
				"可以让两人同时朝一个方向看，增加故事感",
				"从正面或略微侧面拍摄效果最佳"
			],
			guideImageName: "pose_side_by_side"
		))

		// 13. 背靠背
		poses.append(PoseTemplate(
			id: "backToBack",
			name: "背靠背",
			description: "两人背靠背的姿态，象征相互依靠和支持。适合情侣写真和创意双人照，具有独特的构图美感。",
			category: .couple,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "两人背部轻轻相靠",
				"head": "各自转向一侧或都看向镜头",
				"arms": "双手交叉抱于胸前或自然垂放",
				"legs": "自然站立，略微分开",
				"hands": "自然垂放或叉腰",
				"back": "两人背部贴合，保持挺直",
				"eyeContact": "可以各自看向不同方向，或一人回眸",
				"bodyAngle": "两人背对背，身体形成对称",
				"hips": "自然居中",
				"feet": "略微分开，保持平衡",
				"chin": "各自抬起或转向",
				"gaze": "各自看向不同方向"
			],
			variations: [
				"一人回眸：背靠背时一人回头看向镜头",
				"手臂相扣：两人手臂在背后相扣",
				"坐姿背靠背：两人坐在地上背靠背"
			],
			tips: [
				"确保两人身高匹配，或利用台阶调整高度",
				"背部接触要轻柔自然，不要用力推挤",
				"可以利用对称构图增强视觉效果",
				"两人的着装可以形成对比色",
				"选择简洁的背景突出两人轮廓"
			],
			guideImageName: "pose_back_to_back"
		))

		// 14. 背驮姿态
		poses.append(PoseTemplate(
			id: "piggyback",
			name: "背驮姿态",
			description: "一人背着另一人的亲密姿态，充满趣味和甜蜜感。适合情侣写真和轻松愉快的户外拍摄。",
			category: .couple,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "下方的人肩膀承重，上方的人手臂搭在肩上",
				"head": "上方的人头部靠近下方的人耳侧",
				"arms": "下方的人托住上方的人腿部，上方的人环抱下方的人",
				"legs": "下方的人双腿分开与肩同宽，上方的人双腿弯曲",
				"hands": "下方的人双手托住，上方的人双手环抱",
				"back": "下方的人腰背挺直，上方的人紧贴",
				"eyeContact": "两人都看向镜头，或上方的人看向下方的人",
				"bodyAngle": "正面或侧面朝向镜头",
				"hips": "下方的人略微后坐以保持平衡",
				"feet": "下方的人站稳，双脚分开",
				"chin": "上方的人下巴放在下方的人肩上",
				"gaze": "开心地看向镜头"
			],
			variations: [
				"公主抱：一人横抱另一人，浪漫经典",
				"侧抱：一人从侧面托起另一人",
				"跳跃背驮：下方的人轻轻跳跃，增加动感"
			],
			tips: [
				"确保下方的人有足够的力量，注意安全",
				"上方的人要放松，不要僵硬地抓住",
				"两人的表情要自然开心，不要紧张",
				"从侧面拍摄可以更好地展现两人的轮廓",
				"选择开阔的户外场景，增加画面空间感"
			],
			guideImageName: "pose_piggyback"
		))

		// 15. 额头相贴
		poses.append(PoseTemplate(
			id: "foreheadTouch",
			name: "额头相贴",
			description: "两人额头轻轻相贴的亲密姿态，闭眼感受彼此的呼吸和温度。极简而深情，适合情绪化的人像拍摄。",
			category: .couple,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "两人肩膀靠近，身体略微倾向对方",
				"head": "两人额头轻轻相贴，略微低头",
				"arms": "双手自然放在对方腰间或肩膀上",
				"legs": "自然站立，脚步略微交错",
				"hands": "轻抚对方脸颊、脖子或手臂",
				"back": "略微弯曲，倾向对方",
				"eyeContact": "闭眼，感受彼此",
				"bodyAngle": "两人身体正对彼此",
				"hips": "自然放松",
				"feet": "脚步交错，拉近距离",
				"chin": "略微低垂",
				"gaze": "闭眼"
			],
			variations: [
				"鼻尖相触：额头分开，鼻尖轻轻触碰",
				"侧脸相贴：两人的脸颊贴在一起",
				"低头拥抱：额头相贴后自然过渡到拥抱"
			],
			tips: [
				"引导两人闭上眼睛，专注于感受彼此",
				"额头相贴的力度要轻，不要用力挤压",
				"使用大光圈虚化背景，突出情感氛围",
				"选择柔和的自然光或暖色调灯光",
				"拍摄特写镜头，聚焦在两人的面部表情"
			],
			guideImageName: "pose_forehead_touch"
		))

		// MARK: 儿童姿态

		// 16. 玩耍坐姿
		poses.append(PoseTemplate(
			id: "playfulSitting",
			name: "玩耍坐姿",
			description: "儿童天真烂漫的坐姿，适合捕捉孩子最自然可爱的瞬间。不拘泥于形式，让孩子自由发挥。",
			category: .children,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松，不刻意摆拍",
				"head": "自由转动，充满好奇",
				"arms": "自由活动，可以拿着玩具",
				"legs": "随意摆放，盘腿或伸直",
				"hands": "拿着玩具、零食或自然摆放",
				"back": "自然弯曲，不刻意挺直",
				"eyeContact": "可以看镜头也可以看玩具",
				"bodyAngle": "自由不拘束",
				"hips": "坐于地面",
				"feet": "赤脚或穿可爱袜子",
				"chin": "自然",
				"gaze": "可以看向镜头、玩具或远方"
			],
			variations: [
				"抱着玩具：抱着心爱的毛绒玩具，自然微笑",
				"看书姿态：假装看书，展现专注可爱的一面",
				"趴着玩耍：趴在地上，双手托腮或摆弄玩具"
			],
			tips: [
				"不要强迫孩子摆姿势，让他们自由玩耍",
				"使用连拍模式捕捉自然表情",
				"准备一些孩子喜欢的玩具或零食作为道具",
				"降低拍摄角度，与孩子视线平齐",
				"利用自然光，避免使用闪光灯惊吓孩子"
			],
			guideImageName: "pose_playful_sitting"
		))

		// 17. 好奇站立
		poses.append(PoseTemplate(
			id: "curiousStanding",
			name: "好奇站立",
			description: "孩子充满好奇心站立的姿态，踮起脚尖或探身观察周围的事物。捕捉孩子探索世界的纯真瞬间。",
			category: .children,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然，可能略微前倾",
				"head": "略微仰起或歪头，充满好奇",
				"arms": "一手自然垂放，一手可能指向某物",
				"legs": "自然站立，可能踮起脚尖",
				"hands": "手指张开或握着小物件",
				"back": "自然挺直",
				"eyeContact": "看向感兴趣的事物",
				"bodyAngle": "面向吸引注意力的方向",
				"hips": "自然",
				"feet": "可能踮起脚尖",
				"chin": "略微抬起",
				"gaze": "充满好奇地看向某个方向"
			],
			variations: [
				"踮脚张望：踮起脚尖探头看高处",
				"手指远方：一手指向远方，表情兴奋",
				"歪头观察：歪着头认真观察某个事物"
			],
			tips: [
				"利用孩子对新鲜事物的好奇心引导他们的视线",
				"从孩子的视角高度拍摄，获得更真实的画面",
				"孩子注意力时间短，拍摄要快速果断",
				"使用快门优先模式，保证快门速度",
				"准备一些有趣的物品吸引孩子注意力"
			],
			guideImageName: "pose_curious_standing"
		))

		// 18. 自由奔跑
		poses.append(PoseTemplate(
			id: "runningFree",
			name: "自由奔跑",
			description: "孩子在户外自由奔跑的快乐姿态，充满活力和生命力。最佳捕捉儿童自然状态的方式之一。",
			category: .children,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "随跑步动作自然摆动",
				"head": "面向前方，表情开心",
				"arms": "自然摆动，配合跑步节奏",
				"legs": "大步奔跑，一前一后",
				"hands": "自然半握拳",
				"back": "略微前倾",
				"eyeContact": "看向前方或看向镜头",
				"bodyAngle": "身体朝向奔跑方向",
				"hips": "随步伐扭动",
				"feet": "交替着地，展现奔跑动态",
				"chin": "自然抬起",
				"gaze": "看向前方或回头看向镜头"
			],
			variations: [
				"追逐泡泡：孩子追逐飞舞的泡泡",
				"放风筝：牵着风筝线奔跑",
				"追逐宠物：和小狗一起奔跑玩耍"
			],
			tips: [
				"使用高速连拍和追踪对焦捕捉清晰画面",
				"快门速度不低于 1/1000s",
				"选择开阔安全的场地让孩子自由奔跑",
				"从正面或侧面拍摄，捕捉奔跑的动态",
				"利用逆光拍摄，增加发丝光和梦幻感"
			],
			guideImageName: "pose_running_free"
		))

		// 19. 躲猫猫
		poses.append(PoseTemplate(
			id: "peekaboo",
			name: "躲猫猫",
			description: "孩子玩躲猫猫时的可爱姿态，从遮挡物后面探出头来或用手遮住脸然后打开。充满童趣和惊喜。",
			category: .children,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "部分隐藏在遮挡物后",
				"head": "从遮挡物后探出，表情惊喜",
				"arms": "双手抓住遮挡物或遮住脸部",
				"legs": "站立或蹲在遮挡物后面",
				"hands": "抓住遮挡物边缘或遮住脸",
				"back": "略微弯曲，躲在遮挡物后",
				"eyeContact": "看向镜头，眼睛睁大",
				"bodyAngle": "身体躲在遮挡物后面",
				"hips": "隐藏在遮挡物后",
				"feet": "隐藏在遮挡物后或露出",
				"chin": "自然",
				"gaze": "惊喜地看向镜头"
			],
			variations: [
				"手遮脸：双手遮住脸，然后打开露出笑脸",
				"树后探出：从树干后面探出半个身子",
				"门后探头：从门框后面探出头来"
			],
			tips: [
				"和孩子一起玩躲猫猫游戏，捕捉最自然的笑脸",
				"使用大光圈虚化前景遮挡物",
				"预判孩子探头的时机，提前半按快门",
				"遮挡物不要完全遮住孩子，留出空间",
				"利用自然光线打到孩子脸上"
			],
			guideImageName: "pose_peekaboo"
		))

		// MARK: 美食姿态

		// 20. 俯拍平面
		poses.append(PoseTemplate(
			id: "topDown",
			name: "俯拍平面",
			description: "从正上方垂直俯拍美食的经典构图方式，适合展示餐盘的整体布局和色彩搭配，是社交媒体最流行的美食拍摄角度。",
			category: .food,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "不适用",
				"head": "不适用",
				"arms": "双臂稳定支撑相机于食物正上方",
				"legs": "不适用",
				"hands": "稳定握持设备，确保水平",
				"back": "不适用",
				"eyeContact": "不适用",
				"bodyAngle": "相机与桌面完全平行",
				"hips": "不适用",
				"feet": "不适用",
				"chin": "不适用",
				"gaze": "不适用"
			],
			variations: [
				"手持入镜：一只手入镜手持餐具，增加互动感",
				"人物俯视：拍摄人物从上方俯视美食的视角",
				"动态撒粉：从上方撒糖粉或香料，捕捉动态瞬间"
			],
			tips: [
				"确保相机与桌面完全平行，使用网格线辅助对齐",
				"注意光源位置，避免拍摄者自身阴影落入画面",
				"合理布置餐具、餐巾和配饰，丰富画面层次",
				"使用自然光或柔和的侧光，避免硬光直射",
				"保持背景简洁，使用纯色桌面或桌布"
			],
			guideImageName: "pose_top_down"
		))

		// 21. 45度角
		poses.append(PoseTemplate(
			id: "fortyFiveDegree",
			name: "45度角",
			description: "从45度角拍摄美食的经典角度，能够同时展示食物的顶部和侧面，最接近人眼用餐时的自然视角，适合大多数美食拍摄。",
			category: .food,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "不适用",
				"head": "不适用",
				"arms": "稳定握持设备，保持 45 度俯角",
				"legs": "不适用",
				"hands": "稳定握持",
				"back": "不适用",
				"eyeContact": "不适用",
				"bodyAngle": "相机与桌面约 45 度角",
				"hips": "不适用",
				"feet": "不适用",
				"chin": "不适用",
				"gaze": "不适用"
			],
			variations: [
				"低角度45度：更低的45度角，突出食物的高度和层次",
				"人物入镜45度：有人在背景中模糊入镜",
				"夹取瞬间：用筷子或叉子夹起食物的瞬间"
			],
			tips: [
				"45度角是最接近人眼视角的拍摄角度，最为自然",
				"对焦在食物的最前方，利用景深虚化后部",
				"注意前景和背景的层次关系",
				"利用窗口自然光从侧面或后方打光",
				"适当加入人物手部动作增加生活气息"
			],
			guideImageName: "pose_45_degree"
		))

		// 22. 平面摆放
		poses.append(PoseTemplate(
			id: "flatLay",
			name: "平面摆放",
			description: "精心布置的平面摆放拍摄，适合展示食材、烘焙成品和餐桌布置。通过精心构图和道具搭配，营造高级感。",
			category: .food,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "不适用",
				"head": "不适用",
				"arms": "稳定握持设备于正上方",
				"legs": "不适用",
				"hands": "稳定握持，确保画面水平",
				"back": "不适用",
				"eyeContact": "不适用",
				"bodyAngle": "相机完全平行于桌面",
				"hips": "不适用",
				"feet": "不适用",
				"chin": "不适用",
				"gaze": "不适用"
			],
			variations: [
				"食材散落：食材随意散落在桌面，自然随性",
				"几何排列：按照几何图案整齐排列食材",
				"制作过程：展示从食材到成品的制作过程"
			],
			tips: [
				"使用三角形构图法则，主次分明",
				"合理利用负空间（留白），让画面呼吸",
				"搭配与食物颜色互补的道具和背景",
				"确保光线均匀，使用散射光或柔光箱",
				"注意餐具、餐巾等配饰的质感和摆放角度"
			],
			guideImageName: "pose_flat_lay"
		))

		// 23. 动态瞬间
		poses.append(PoseTemplate(
			id: "actionShot",
			name: "动态瞬间",
			description: "捕捉美食制作或享用过程中的动态瞬间，如倒酱汁、切牛排、撒调料等。动感十足，让美食照片更有生命力。",
			category: .food,
			difficulty: .advanced,
			keypoints: [
				"shoulders": "不适用",
				"head": "不适用",
				"arms": "一手操作（倒酱汁/切食物），一手握持设备",
				"legs": "不适用",
				"hands": "一手稳定设备，一手执行动作",
				"back": "不适用",
				"eyeContact": "不适用",
				"bodyAngle": "相机角度根据动作调整",
				"hips": "不适用",
				"feet": "不适用",
				"chin": "不适用",
				"gaze": "不适用"
			],
			variations: [
				"倒酱汁：捕捉酱汁流淌的瞬间",
				"切牛排：刀切入牛排露出粉嫩切面的瞬间",
				"撒糖粉：糖粉从筛网中飘落的瞬间"
			],
			tips: [
				"使用高速快门（不低于 1/500s）冻结动态",
				"使用连拍模式增加成功率",
				"提前对焦并锁定焦点在动作发生的位置",
				"注意液体飞溅的方向，保护设备",
				"背景要简洁，让动态成为视觉焦点"
			],
			guideImageName: "pose_action_shot"
		))

		// MARK: 风景人像

		// 24. 剪影姿态
		poses.append(PoseTemplate(
			id: "silhouettePose",
			name: "剪影姿态",
			description: "在日落或逆光场景中拍摄人物剪影，利用天空的暖色背景和人物的黑色轮廓形成强烈对比，营造浪漫氛围。",
			category: .landscape,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "保持清晰轮廓，双肩展开",
				"head": "侧身轮廓清晰，或仰头向上",
				"arms": "伸展手臂，增加轮廓线条感",
				"legs": "分开站立，避免并拢成单一色块",
				"hands": "手指张开，形成清晰轮廓",
				"back": "挺直，展现清晰的身体轮廓",
				"eyeContact": "侧脸或仰望天空",
				"bodyAngle": "侧身面对相机，展现身体轮廓",
				"hips": "自然",
				"feet": "略微分开，与肩同宽",
				"chin": "抬起，展现下颌线",
				"gaze": "看向远方或天空"
			],
			variations: [
				"跳跃剪影：在逆光中跳跃，捕捉空中姿态",
				"牵手剪影：两人牵手面对夕阳",
				"舞蹈剪影：做出舞蹈动作，展现优美线条"
			],
			tips: [
				"选择日出或日落黄金时段，天空色彩最丰富",
				"对天空测光，让人物自然变暗形成剪影",
				"确保人物轮廓清晰，避免与背景重叠",
				"肢体之间要有间隙，避免粘在一起形成一团黑",
				"选择开阔场景，确保天空占据画面大部分"
			],
			guideImageName: "pose_silhouette"
		))

		// 25. 指向远方
		poses.append(PoseTemplate(
			id: "pointingAway",
			name: "指向远方",
			description: "人物背对镜头，指向远处的风景或地标。引导观众视线跟随人物手指方向，增加画面的故事性和探索感。",
			category: .landscape,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松",
				"head": "略微转向手指方向",
				"arms": "一臂伸展指向远方，另一臂自然垂放",
				"legs": "自然站立，略微分开",
				"hands": "指向远方的手指自然伸直",
				"back": "挺直，面对风景",
				"eyeContact": "看向手指指向的方向",
				"bodyAngle": "背对镜头，略微侧身",
				"hips": "自然",
				"feet": "与肩同宽",
				"chin": "略微抬起，朝向远方",
				"gaze": "看向远方的风景"
			],
			variations: [
				"坐姿指向：坐在岩石或长椅上指向远方",
				"牵手指向：两人并肩，一人指向远方，另一人看向所指方向",
				"回眸指向：指向远方后回眸看向镜头"
			],
			tips: [
				"确保手指指向的方向有明确的视觉焦点",
				"人物在画面中占据较小比例，突出风景的壮阔",
				"使用三分法构图，将人物放在一侧",
				"选择有层次感的远景，增加画面深度",
				"穿着鲜艳的服装可以在风景中形成视觉焦点"
			],
			guideImageName: "pose_pointing_away"
		))

		// 26. 坐观风景
		poses.append(PoseTemplate(
			id: "sittingScenic",
			name: "坐观风景",
			description: "人物坐在风景前，安静地欣赏远方。传达宁静、沉思的情绪，适合山川、湖泊、海边等大场景风光人像。",
			category: .landscape,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松",
				"head": "看向远方风景",
				"arms": "双手环抱膝盖或自然放在身旁",
				"legs": "双腿弯曲或一条腿伸直一条弯曲",
				"hands": "自然放在膝盖上或身旁",
				"back": "略微弯曲，放松姿态",
				"eyeContact": "不看向镜头，望向远方",
				"bodyAngle": "侧身或背对镜头",
				"hips": "坐于地面",
				"feet": "自然摆放",
				"chin": "自然",
				"gaze": "安静地望向远方"
			],
			variations: [
				"悬崖边坐姿：坐在悬崖边缘，双腿悬空（注意安全）",
				"长椅坐姿：坐在公园长椅上欣赏风景",
				"草地躺姿：躺在草地上，仰望天空"
			],
			tips: [
				"人物不宜占据画面过大比例，约 10-20% 即可",
				"利用广角镜头拍摄，突出风景的壮阔",
				"选择黄金时段拍摄，光线柔和温暖",
				"人物衣着颜色应与风景形成对比",
				"注意安全，远离危险边缘"
			],
			guideImageName: "pose_sitting_scenic"
		))

		// 27. 走入风景
		poses.append(PoseTemplate(
			id: "walkingInto",
			name: "走入风景",
			description: "人物背对镜头走向远方风景的姿态，营造强烈的故事感和探索精神。让观众产生跟随人物一起走入画面的冲动。",
			category: .landscape,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松，随步伐轻微摆动",
				"head": "面向前方风景",
				"arms": "自然摆动或一手插口袋",
				"legs": "迈步走向远方",
				"hands": "自然摆动",
				"back": "挺直，面向远方",
				"eyeContact": "不看向镜头",
				"bodyAngle": "完全背对镜头，走向远方",
				"hips": "随步伐自然摆动",
				"feet": "交替前进",
				"chin": "自然抬起",
				"gaze": "看向前方的风景"
			],
			variations: [
				"牵手走入：两人牵手一起走向远方",
				"奔跑走入：小跑着奔向远方",
				"回眸走入：走向远方时回头看向镜头"
			],
			tips: [
				"使用小光圈（f/8-f/16）确保远近都清晰",
				"人物在画面中占据较小比例，约 5-15%",
				"选择有引导线的场景（道路、小径、海岸线）",
				"等待人物走到画面中最佳位置时按下快门",
				"利用对称构图增强画面的仪式感"
			],
			guideImageName: "pose_walking_into"
		))

		// MARK: 婚礼姿态

		// 28. 手持捧花
		poses.append(PoseTemplate(
			id: "bouquetHold",
			name: "手持捧花",
			description: "新娘优雅地手持捧花的经典婚礼姿态，展现新娘的柔美与幸福。适合婚礼当天的单人写真和细节拍摄。",
			category: .wedding,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "自然放松，双肩齐平",
				"head": "略微倾向捧花一侧或正面看向镜头",
				"arms": "双手捧花置于腰部前方，肘部微弯",
				"legs": "自然站立，略微侧身",
				"hands": "双手优雅地捧住花束",
				"back": "挺直优雅",
				"eyeContact": "可以看向镜头或低头看花",
				"bodyAngle": "正面或略微侧身 15-20 度",
				"hips": "略微侧转",
				"feet": "一脚前一脚后，丁字步",
				"chin": "略微抬起，展现颈部线条",
				"gaze": "幸福地看向镜头或柔美地低头看花"
			],
			variations: [
				"单手捧花：一手捧花，另一手自然垂放",
				"举花遮面：捧花举到面前，从花后露出眼睛",
				"转身捧花：背对镜头然后转身，捧花自然跟随"
			],
			tips: [
				"捧花高度在腰部到胸部之间，不要遮挡婚纱细节",
				"手部姿态要轻柔优雅，不要用力抓握花束",
				"利用捧花的颜色与婚纱形成对比",
				"选择柔和的自然光，突出婚纱的质感",
				"注意婚纱拖尾的摆放，增加画面层次"
			],
			guideImageName: "pose_bouquet_hold"
		))

		// 29. 交换戒指
		poses.append(PoseTemplate(
			id: "ringExchange",
			name: "交换戒指",
			description: "婚礼仪式中交换戒指的经典瞬间，特写两人手部为对方戴上戒指的动作。充满仪式感和情感张力。",
			category: .wedding,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "两人身体靠近",
				"head": "两人低头看向手部",
				"arms": "一人伸出手，另一人手持戒指靠近",
				"legs": "自然站立",
				"hands": "手部特写：一人手指微伸，另一人手持戒指",
				"back": "略微弯曲，倾向对方",
				"eyeContact": "低头看向戒指和对方的手",
				"bodyAngle": "两人面对面或略微侧身",
				"hips": "自然",
				"feet": "脚步交错",
				"chin": "低头",
				"gaze": "专注地看着戒指和对方的手"
			],
			variations: [
				"手部特写：纯手部特写，聚焦戒指和手指",
				"半身交换：半身构图，包含两人的面部表情",
				"花丛中交换：在花束或花环的框架中交换戒指"
			],
			tips: [
				"使用大光圈（f/1.4-f/2.8）虚化背景突出手部",
				"对焦在戒指上，确保戒指清晰锐利",
				"注意手部姿态的优雅，手指自然伸展",
				"可以同时拍摄手部特写和半身画面",
				"利用自然柔光或暖色调灯光营造温馨氛围"
			],
			guideImageName: "pose_ring_exchange"
		))

		// 30. 第一支舞
		poses.append(PoseTemplate(
			id: "firstDance",
			name: "第一支舞",
			description: "新人在婚礼上的第一支舞，浪漫的舞姿展现两人的默契与甜蜜。适合婚礼晚宴和户外婚礼的舞蹈环节。",
			category: .wedding,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "男方肩膀自然，女方手臂搭在男方肩上",
				"head": "两人头部靠近，女方可能靠在男方肩上",
				"arms": "男方一手搂女方腰部，一手握住女方手；女方一手搭肩，一手被握",
				"legs": "随舞步移动，优雅缓慢",
				"hands": "男方握住女方的手，女方手自然放在男方手中",
				"back": "女方略微后仰，男方挺直支撑",
				"eyeContact": "深情对视",
				"bodyAngle": "两人身体贴近，形成舞姿框架",
				"hips": "贴近但不紧贴",
				"feet": "随舞步缓慢移动",
				"chin": "女方略微抬起，男方略微低头",
				"gaze": "深情注视对方"
			],
			variations: [
				"旋转瞬间：女方旋转时裙摆飞扬的瞬间",
				"俯身下腰：男方扶着女方做下腰动作",
				"慢舞拥抱：两人紧紧拥抱慢慢摇摆"
			],
			tips: [
				"使用较慢快门（1/30-1/60s）创造动态模糊效果",
				"配合闪光灯后帘同步，冻结最终姿态",
				"注意新娘婚纱裙摆的动态表现",
				"选择合适的背景音乐让新人放松",
				"从多角度拍摄，包括正面、侧面和背面"
			],
			guideImageName: "pose_first_dance"
		))

		// 31. 头纱飘扬
		poses.append(PoseTemplate(
			id: "veilFlow",
			name: "头纱飘扬",
			description: "新娘头纱在风中飘扬的浪漫姿态，充满梦幻感和仙气。适合户外婚礼和婚纱照，捕捉头纱飘动的唯美瞬间。",
			category: .wedding,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "自然放松，双肩齐平",
				"head": "略微迎风或侧向风的方向",
				"arms": "一手轻抚头纱，另一手自然垂放",
				"legs": "自然站立，略微侧身",
				"hands": "轻抚头纱或自然张开感受风",
				"back": "挺直优雅",
				"eyeContact": "可以闭眼感受风或看向镜头",
				"bodyAngle": "侧身或背对风吹来的方向",
				"hips": "自然",
				"feet": "略微分开保持平衡",
				"chin": "略微抬起，迎向微风",
				"gaze": "闭眼享受或温柔地看向镜头"
			],
			variations: [
				"奔跑头纱：新娘向前奔跑，头纱在身后飘扬",
				"转身头纱：快速转身让头纱旋转飘起",
				"两人头纱：新郎掀起新娘头纱的瞬间"
			],
			tips: [
				"选择有微风的户外场景，风力不宜过大",
				"使用高速连拍捕捉头纱最佳飘动形态",
				"逆光拍摄可以让头纱呈现半透明效果",
				"注意头纱不要遮挡面部",
				"选择开阔的场景，避免头纱被树枝等勾住"
			],
			guideImageName: "pose_veil_flow"
		))

		// MARK: 产品姿态

		// 32. 平面展示
		poses.append(PoseTemplate(
			id: "productFlatLay",
			name: "平面展示",
			description: "从正上方俯拍产品的平面展示方式，适合化妆品、食品、文具等小型产品的展示。通过精心搭配道具营造品牌调性。",
			category: .product,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "不适用",
				"head": "不适用",
				"arms": "稳定握持设备于产品正上方",
				"legs": "不适用",
				"hands": "稳定握持，确保画面水平",
				"back": "不适用",
				"eyeContact": "不适用",
				"bodyAngle": "相机与拍摄平面完全平行",
				"hips": "不适用",
				"feet": "不适用",
				"chin": "不适用",
				"gaze": "不适用"
			],
			variations: [
				"几何排列：产品按照几何图案整齐排列",
				"散落自然：产品随意散落，搭配自然元素",
				"手持入镜：一只手入镜手持产品"
			],
			tips: [
				"使用纯色或纹理背景，避免分散注意力",
				"确保光线均匀，使用柔光箱或自然散射光",
				"产品占据画面主体，道具作为辅助装饰",
				"保持产品清洁，去除指纹和灰尘",
				"使用三角形构图，产品放在视觉重心位置"
			],
			guideImageName: "pose_product_flat_lay"
		))

		// 33. 角度展示
		poses.append(PoseTemplate(
			id: "angledDisplay",
			name: "角度展示",
			description: "从30-45度角展示产品的立体感和设计细节，适合电子产品、包装、鞋履等需要展示多面性的产品。",
			category: .product,
			difficulty: .beginner,
			keypoints: [
				"shoulders": "不适用",
				"head": "不适用",
				"arms": "稳定握持设备，保持 30-45 度角",
				"legs": "不适用",
				"hands": "稳定握持",
				"back": "不适用",
				"eyeContact": "不适用",
				"bodyAngle": "相机与产品保持 30-45 度角",
				"hips": "不适用",
				"feet": "不适用",
				"chin": "不适用",
				"gaze": "不适用"
			],
			variations: [
				"三面展示：同时展示产品的正面、侧面和顶面",
				"悬浮效果：用鱼线或后期让产品悬浮",
				"倒影展示：利用镜面或水面创造倒影"
			],
			tips: [
				"选择能展示产品最佳特征的角度",
				"使用渐变背景纸创造专业影棚效果",
				"注意产品的反光面，使用偏振镜减少反光",
				"对焦在产品的品牌标志或关键细节上",
				"适当使用侧光突出产品的立体感和材质"
			],
			guideImageName: "pose_angled_display"
		))

		// 34. 使用场景
		poses.append(PoseTemplate(
			id: "inUse",
			name: "使用场景",
			description: "展示产品在实际使用场景中的状态，让消费者直观了解产品的使用方式和效果。适合数码产品、厨具、服饰等。",
			category: .product,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "自然，做出使用产品的动作",
				"head": "自然看向产品所在方向",
				"arms": "双手或单手操作产品",
				"legs": "自然站立或坐下",
				"hands": "自然地使用产品",
				"back": "自然",
				"eyeContact": "看向产品或镜头",
				"bodyAngle": "根据使用场景自然调整",
				"hips": "自然",
				"feet": "自然摆放",
				"chin": "自然",
				"gaze": "专注于使用产品"
			],
			variations: [
				"第一人称视角：从使用者视角拍摄产品使用",
				"生活场景：在真实家居或办公环境中使用产品",
				"对比展示：展示使用产品前后的对比效果"
			],
			tips: [
				"选择与产品调性匹配的场景和光线",
				"确保产品在画面中清晰可见，不被手部遮挡",
				"手部姿态要自然优雅，指甲保持整洁",
				"背景环境要干净整洁，避免杂乱元素",
				"可以利用生活化场景增加产品的真实感"
			],
			guideImageName: "pose_in_use"
		))

		// 35. 细节特写
		poses.append(PoseTemplate(
			id: "detailCloseup",
			name: "细节特写",
			description: "近距离特写产品的材质纹理、工艺细节和品牌标识，展现产品的品质感和精致度。适合高端产品和手工艺品的展示。",
			category: .product,
			difficulty: .intermediate,
			keypoints: [
				"shoulders": "不适用",
				"head": "不适用",
				"arms": "稳定握持设备，靠近产品",
				"legs": "不适用",
				"hands": "稳定握持，可能需要三脚架",
				"back": "不适用",
				"eyeContact": "不适用",
				"bodyAngle": "相机贴近产品表面",
				"hips": "不适用",
				"feet": "不适用",
				"chin": "不适用",
				"gaze": "不适用"
			],
			variations: [
				"纹理特写：聚焦产品材质纹理（皮革、金属、织物）",
				"LOGO特写：突出品牌标识的精致工艺",
				"微距细节：使用微距镜头拍摄肉眼难以察觉的细节"
			],
			tips: [
				"使用微距镜头或长焦镜头获得最大放大倍率",
				"使用三脚架确保画面稳定清晰",
				"合理控制景深，确保关键细节在焦内",
				"使用侧光或逆光突出材质纹理",
				"注意灰尘和指纹，拍摄前彻底清洁产品"
			],
			guideImageName: "pose_detail_closeup"
		))

		return poses
	}
}

#endif