//
//  PoseGuideManager.swift
//  LiveCapture
//
//  姿势引导管理器
//
//  ## 文件作用
//  管理内置姿势模板库
//  提供按分类浏览和选择姿势模板的能力
//  支持 20+ 种预定义姿势模板覆盖 5 个分类
//
//  ## 主要类
//
//  ### PoseGuideManager
//  姿势模板管理器（ObservableObject）
//
//  ## Published 属性
//  - templates: [PoseTemplate] - 所有模板
//  - selectedTemplate: PoseTemplate? - 当前选中模板
//  - currentCategory: PoseCategory - 当前分类
//
//  ## 模板分类
//  - solo (独照): 6 个模板
//  - couple (情侣): 5 个模板
//  - friends (朋友): 4 个模板
//  - family (家庭): 3 个模板
//  - pet (宠物): 3 个模板
//
//  ## 主要方法
//  - selectTemplate(_:): 选择模板
//  - templatesForCategory(_:): 按分类筛选模板
//  - selectRandomTemplate(): 随机选择模板
//

import Foundation
import Combine

#if os(iOS)

/// 姿势引导管理器
final class PoseGuideManager: ObservableObject {

	// MARK: - Published State

	/// 所有内置模板
	@Published var templates: [PoseTemplate] = []

	/// 当前选中的模板
	@Published var selectedTemplate: PoseTemplate?

	/// 当前分类
	@Published var currentCategory: PoseTemplate.PoseCategory = .solo

	// MARK: - Lifecycle

	init() {
		loadBuiltInTemplates()
	}

	// MARK: - Public API

	/// 选择指定模板
	func selectTemplate(_ template: PoseTemplate) {
		selectedTemplate = template
		currentCategory = template.category
	}

	/// 获取指定分类的模板列表
	func templatesForCategory(_ category: PoseTemplate.PoseCategory) -> [PoseTemplate] {
		templates.filter { $0.category == category }
	}

	/// 随机选择模板
	func selectRandomTemplate() {
		let categoryTemplates = templatesForCategory(currentCategory)
		guard !categoryTemplates.isEmpty else { return }
		selectedTemplate = categoryTemplates.randomElement()
	}

	// MARK: - 模板加载

	/// 加载内置姿势模板（20+ 个模板覆盖 5 个分类）
	func loadBuiltInTemplates() {
		var allTemplates: [PoseTemplate] = []

		// MARK: 独照 (Solo) - 6 个模板
		allTemplates.append(PoseTemplate(
			id: "solo_standing",
			name: "自然站立",
			category: .solo,
			overlayImageName: "pose_solo_standing",
			description: "面向镜头自然站立，双手自然下垂或轻放身侧",
			tips: [
				"身体微微侧转 15° 更显瘦",
				"一只脚稍向前伸，拉长腿部线条",
				"肩膀放松，避免耸肩",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "solo_sitting",
			name: "优雅坐姿",
			category: .solo,
			overlayImageName: "pose_solo_sitting",
			description: "坐姿拍摄，身体微微前倾，手自然放在膝盖上",
			tips: [
				"坐在椅子前 1/3 处，背部挺直",
				"双腿并拢斜放更显优雅",
				"手轻搭膝盖，避免握拳",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "solo_lookback",
			name: "回眸一笑",
			category: .solo,
			overlayImageName: "pose_solo_lookback",
			description: "背对镜头行走，回头看向镜头微笑",
			tips: [
				"先走几步再回头，动作更自然",
				"回眸时下巴微收更上镜",
				"利用头发飘动增加动感",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "solo_walking",
			name: "街拍行走",
			category: .solo,
			overlayImageName: "pose_solo_walking",
			description: "自然向前行走，抓拍动态瞬间",
			tips: [
				"步伐适中，不要太大步",
				"视线看向远方而非地面",
				"手臂自然摆动增加生活感",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "solo_jumping",
			name: "跳跃抓拍",
			category: .solo,
			overlayImageName: "pose_solo_jumping",
			description: "原地跳跃，抓拍空中姿态",
			tips: [
				"使用连拍模式捕捉最佳瞬间",
				"跳跃时双腿微曲更显动感",
				"表情放松，享受跳跃的快乐",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "solo_sideprofile",
			name: "侧身剪影",
			category: .solo,
			overlayImageName: "pose_solo_sideprofile",
			description: "侧身站立，利用逆光拍摄剪影效果",
			tips: [
				"选择黄昏或日出时分拍摄",
				"侧脸轮廓清晰时效果最佳",
				"身体与镜头呈 90°",
			]
		))

		// MARK: 情侣 (Couple) - 5 个模板
		allTemplates.append(PoseTemplate(
			id: "couple_holdinghands",
			name: "牵手漫步",
			category: .couple,
			overlayImageName: "pose_couple_holdinghands",
			description: "两人牵手并排行走，自然温馨",
			tips: [
				"保持步伐一致更协调",
				"偶尔相视而笑更自然",
				"选择林荫道或海边作为背景",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "couple_hug",
			name: "温暖拥抱",
			category: .couple,
			overlayImageName: "pose_couple_hug",
			description: "面对面拥抱，展现亲密感",
			tips: [
				"一人轻轻环抱另一人腰部",
				"另一人将手搭在对方肩上",
				"额头相贴增加温馨感",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "couple_backtoback",
			name: "背靠背",
			category: .couple,
			overlayImageName: "pose_couple_backtoback",
			description: "两人背靠背站立，时尚感十足",
			tips: [
				"身体微微后仰，靠在对方身上",
				"双手插兜或交叉抱臂增加酷感",
				"适合城市街拍风格",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "couple_princesscarry",
			name: "公主抱",
			category: .couple,
			overlayImageName: "pose_couple_princesscarry",
			description: "一方将另一方公主抱起，浪漫甜蜜",
			tips: [
				"被抱者一手勾住对方脖子",
				"抱人者注意膝盖微曲保持稳定",
				"两人同时看向镜头或彼此对视",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "couple_eyegaze",
			name: "深情对视",
			category: .couple,
			overlayImageName: "pose_couple_eyegaze",
			description: "两人面对面，深情注视对方",
			tips: [
				"距离适中，约 30-50cm",
				"一人可轻抚对方脸颊",
				"使用大光圈虚化背景突出主体",
			]
		))

		// MARK: 朋友 (Friends) - 4 个模板
		allTemplates.append(PoseTemplate(
			id: "friends_shoulder",
			name: "并肩而立",
			category: .friends,
			overlayImageName: "pose_friends_shoulder",
			description: "多人并排站立，展现团结和友谊",
			tips: [
				"身高错落有致，避免一条直线",
				"可以互相搭肩增加亲密感",
				"统一穿搭风格更出片",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "friends_jumping",
			name: "集体跳跃",
			category: .friends,
			overlayImageName: "pose_friends_jumping",
			description: "多人同时跳跃，充满活力和欢乐",
			tips: [
				"喊口号 '三二一跳' 确保同步",
				"使用连拍模式捕捉最佳瞬间",
				"手臂上扬增加画面张力",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "friends_funny",
			name: "搞怪合影",
			category: .friends,
			overlayImageName: "pose_friends_funny",
			description: "做出夸张搞怪表情和动作，展现个性",
			tips: [
				"每个人做不同的搞怪动作",
				"利用道具增加趣味性",
				"不要害羞，越夸张效果越好",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "friends_circle",
			name: "围坐一圈",
			category: .friends,
			overlayImageName: "pose_friends_circle",
			description: "大家围坐在一起，俯拍视角",
			tips: [
				"使用俯拍角度，全员入镜",
				"大家可以举起饮料碰杯",
				"地面铺上野餐垫更温馨",
			]
		))

		// MARK: 家庭 (Family) - 3 个模板
		allTemplates.append(PoseTemplate(
			id: "family_portrait",
			name: "全家福",
			category: .family,
			overlayImageName: "pose_family_portrait",
			description: "全家成员站在一起，温馨合影",
			tips: [
				"长辈居中，晚辈两侧",
				"小朋友可以站在前面或抱起",
				"使用三脚架和定时拍摄",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "family_parentchild",
			name: "亲子时光",
			category: .family,
			overlayImageName: "pose_family_parentchild",
			description: "父母与孩子的温馨互动",
			tips: [
				"蹲下与孩子保持同一高度",
				"捕捉自然互动而非摆拍",
				"选择户外自然光环境",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "family_grandparent",
			name: "祖孙情深",
			category: .family,
			overlayImageName: "pose_family_grandparent",
			description: "祖父母与孙辈的温馨合影",
			tips: [
				"让老人坐着，孩子站在一旁",
				"捕捉老人看孩子的温柔眼神",
				"使用柔和光线减少皱纹",
			]
		))

		// MARK: 宠物 (Pet) - 3 个模板
		allTemplates.append(PoseTemplate(
			id: "pet_holding",
			name: "怀抱宠物",
			category: .pet,
			overlayImageName: "pose_pet_holding",
			description: "将宠物抱在怀中，展现亲密关系",
			tips: [
				"用零食或玩具吸引宠物看镜头",
				"确保宠物舒适安全",
				"使用连拍捕捉宠物自然表情",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "pet_interaction",
			name: "互动玩耍",
			category: .pet,
			overlayImageName: "pose_pet_interaction",
			description: "与宠物一起玩耍时的自然抓拍",
			tips: [
				"蹲下与宠物保持同一高度",
				"使用宠物喜欢的玩具引导",
				"抓拍宠物最自然可爱的瞬间",
			]
		))

		allTemplates.append(PoseTemplate(
			id: "pet_overhead",
			name: "俯拍合影",
			category: .pet,
			overlayImageName: "pose_pet_overhead",
			description: "从上方俯拍你与宠物的合影",
			tips: [
				"宠物躺在地上，你从上往下拍",
				"可以伸出手让宠物互动",
				"使用广角镜头纳入更多环境",
			]
		))

		templates = allTemplates
	}
}

#endif