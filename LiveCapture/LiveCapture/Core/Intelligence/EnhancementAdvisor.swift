//
//  EnhancementAdvisor.swift
//  LiveCapture
//
//  AI 增强建议系统 - 基于质量评估和场景分析生成智能增强建议
//
//  ## 文件作用
//  根据图像质量评估、场景类型和环境光分析结果，生成智能增强建议
//  提供场景预设参数和最优拍摄设置，帮助用户自动优化拍摄效果
//
//  ## 主要类型
//  ### EnhancementAdvisor
//  AI 增强顾问主类
//
//  ## 主要方法
//
//  ### 增强建议生成
//  - generateSuggestions(from:scene:light:): 基于质量、场景和光环境生成增强建议
//    策略:
//      - 质量维度分析：清晰度、噪点、曝光、色彩和谐度
//      - 场景特定建议：人像、风景、夜景、美食、婚礼等
//      - 光环境适配：逆光、低光、暖光、冷光
//      - 优先级排序和去重
//
//  - generateSuggestions(from:): 简化版，仅基于质量评估生成建议
//
//  ### 场景预设
//  - getPresetParams(for:): 获取场景特定预设参数
//  - getOptimalSettings(for:light:): 获取考虑光环境的最优设置
//
//  ### 报告生成
//  - generateSummaryReport(quality:suggestions:): 生成中文质量评估摘要报告
//
//  ## 线程安全
//  - 所有方法均为纯计算，无副作用，可在任意线程调用
//  - 返回不可变值类型，线程安全
//

import Foundation

#if os(iOS)

/// AI 增强顾问 - 基于质量评估和场景分析生成智能增强建议
final class EnhancementAdvisor {

	// MARK: - 增强建议生成

	/// 基于质量评估、场景类型和光环境生成增强建议
	/// - Parameters:
	///   - quality: 图像质量评估结果
	///   - scene: 识别到的场景类型
	///   - light: 环境光分析结果
	/// - Returns: 增强建议数组，按优先级排序
	func generateSuggestions(from quality: QualityAssessment, scene: SceneType, light: LightAnalysis) -> [EnhancementSuggestion] {
		var suggestions: [EnhancementSuggestion] = []

		// 1. 质量维度建议
		suggestions.append(contentsOf: generateQualityBasedSuggestions(quality: quality))

		// 2. 场景特定建议
		suggestions.append(contentsOf: generateSceneBasedSuggestions(scene: scene))

		// 3. 光环境适配建议
		suggestions.append(contentsOf: generateLightBasedSuggestions(light: light))

		// 4. 去重并排序
		return deduplicateAndSort(suggestions: suggestions)
	}

	/// 仅基于质量评估生成增强建议（无场景和光环境信息）
	/// - Parameter quality: 图像质量评估结果
	/// - Returns: 增强建议数组
	func generateSuggestions(from quality: QualityAssessment) -> [EnhancementSuggestion] {
		var suggestions: [EnhancementSuggestion] = []

		// 质量维度建议
		suggestions.append(contentsOf: generateQualityBasedSuggestions(quality: quality))

		// 去重并排序
		return deduplicateAndSort(suggestions: suggestions)
	}

	// MARK: - 质量维度建议

	/// 根据质量评估各维度分数生成建议
	private func generateQualityBasedSuggestions(quality: QualityAssessment) -> [EnhancementSuggestion] {
		var suggestions: [EnhancementSuggestion] = []

		// 清晰度不足
		if quality.sharpnessScore < 50 {
			let sharpenAmount: Float = {
				if quality.sharpnessScore < 20 {
					return 30.0
				} else if quality.sharpnessScore < 35 {
					return 25.0
				} else {
					return 20.0
				}
			}()
			suggestions.append(EnhancementSuggestion(
				type: .sharpness,
				title: "锐度增强",
				description: "当前清晰度评分 \(Int(quality.sharpnessScore))，建议增强锐度以提升画面细节表现力。",
				parameters: ["sharpen": sharpenAmount],
				priority: quality.sharpnessScore < 30 ? "高" : "中"
			))
		}

		// 噪点过多
		if quality.noiseLevel > 30 {
			let noiseReductionAmount: Float = {
				if quality.noiseLevel > 60 {
					return 25.0
				} else if quality.noiseLevel > 45 {
					return 20.0
				} else {
					return 15.0
				}
			}()
			suggestions.append(EnhancementSuggestion(
				type: .noiseReduction,
				title: "降噪处理",
				description: "当前噪点水平 \(Int(quality.noiseLevel))，建议进行降噪处理以获得更纯净的画面。",
				parameters: ["noise_reduction": noiseReductionAmount],
				priority: quality.noiseLevel > 50 ? "高" : "中"
			))
		}

		// 曝光不足或过度
		if quality.exposureScore < 60 {
			if quality.exposureScore < 30 {
				// 严重曝光不足
				suggestions.append(EnhancementSuggestion(
					type: .exposure,
					title: "曝光补偿",
					description: "当前曝光评分 \(Int(quality.exposureScore))，画面严重偏暗，建议大幅提升亮度和对比度。",
					parameters: ["brightness": 25.0, "contrast": 15.0, "shadows": 20.0],
					priority: "高"
				))
			} else if quality.exposureScore < 45 {
				// 曝光不足
				suggestions.append(EnhancementSuggestion(
					type: .exposure,
					title: "亮度调整",
					description: "当前曝光评分 \(Int(quality.exposureScore))，画面偏暗，建议适当提升亮度。",
					parameters: ["brightness": 15.0, "contrast": 10.0],
					priority: "中"
				))
			} else {
				// 轻微曝光问题
				suggestions.append(EnhancementSuggestion(
					type: .exposure,
					title: "曝光微调",
					description: "当前曝光评分 \(Int(quality.exposureScore))，建议微调曝光以获得最佳效果。",
					parameters: ["brightness": 8.0],
					priority: "低"
				))
			}
		}

		// 色彩和谐度不足
		if quality.colorHarmonyScore < 40 {
			let saturationAmount: Float = {
				if quality.colorHarmonyScore < 20 {
					return 15.0
				} else if quality.colorHarmonyScore < 30 {
					return 12.0
				} else {
					return 10.0
				}
			}()
			suggestions.append(EnhancementSuggestion(
				type: .colorHarmony,
				title: "色彩优化",
				description: "当前色彩和谐度评分 \(Int(quality.colorHarmonyScore))，建议调整饱和度以增强色彩表现力。",
				parameters: ["saturation": saturationAmount],
				priority: quality.colorHarmonyScore < 25 ? "高" : "中"
			))
		}

		return suggestions
	}

	// MARK: - 场景特定建议

	/// 根据场景类型生成特定增强建议
	private func generateSceneBasedSuggestions(scene: SceneType) -> [EnhancementSuggestion] {
		var suggestions: [EnhancementSuggestion] = []

		switch scene {
		case .portrait, .portraitStanding, .portraitSitting:
			suggestions.append(EnhancementSuggestion(
				type: .portraitEnhancement,
				title: "人像增强",
				description: "检测到人像场景，建议优化肤色、柔化背景并增强立体感。",
				parameters: ["brightness": 5.0, "contrast": 10.0, "saturation": 5.0],
				priority: "中"
			))

		case .landscape, .landscapeNature, .landscapeSunset:
			suggestions.append(EnhancementSuggestion(
				type: .landscapeEnhancement,
				title: "风景增强",
				description: "检测到风景场景，建议增强色彩饱和度和对比度以展现自然之美。",
				parameters: ["saturation": 15.0, "contrast": 10.0],
				priority: "中"
			))

		case .nightScene, .nightPortrait:
			suggestions.append(EnhancementSuggestion(
				type: .nightOptimization,
				title: "夜景优化",
				description: "检测到夜景场景，建议提亮画面并增强锐度以还原暗部细节。",
				parameters: ["brightness": 20.0, "sharpen": 15.0],
				priority: "高"
			))

		case .food, .foodStyling:
			suggestions.append(EnhancementSuggestion(
				type: .foodEnhancement,
				title: "美食增强",
				description: "检测到美食场景，建议增强饱和度、对比度和暖色调以增加食欲感。",
				parameters: ["saturation": 15.0, "contrast": 10.0, "warmth": 5.0],
				priority: "中"
			))

		case .weddingOutdoor, .weddingIndoor:
			suggestions.append(EnhancementSuggestion(
				type: .portraitEnhancement,
				title: "婚礼优化",
				description: "检测到婚礼场景，建议柔化色调、提升亮度和增强暖色氛围。",
				parameters: ["brightness": 10.0, "contrast": 8.0, "warmth": 10.0],
				priority: "高"
			))

		case .childrenOutdoor, .childrenIndoor:
			suggestions.append(EnhancementSuggestion(
				type: .portraitEnhancement,
				title: "儿童摄影增强",
				description: "检测到儿童场景，建议提升亮度、增强饱和度以呈现活泼生动的画面。",
				parameters: ["brightness": 15.0, "saturation": 10.0],
				priority: "中"
			))

		case .productWhite:
			suggestions.append(EnhancementSuggestion(
				type: .sharpness,
				title: "产品展示增强",
				description: "检测到产品拍摄场景，建议大幅增强对比度和清晰度以突出产品细节。",
				parameters: ["contrast": 25.0, "clarity": 30.0],
				priority: "高"
			))

		case .snow, .beach:
			suggestions.append(EnhancementSuggestion(
				type: .exposure,
				title: "亮度优化",
				description: "检测到高亮场景，建议适当降低曝光以避免过曝，保留高光细节。",
				parameters: ["exposure": -8.0],
				priority: "高"
			))

		case .pet:
			suggestions.append(EnhancementSuggestion(
				type: .sharpness,
				title: "宠物摄影增强",
				description: "检测到宠物场景，建议增强锐度以清晰呈现毛发细节。",
				parameters: ["sharpen": 15.0, "clarity": 10.0],
				priority: "中"
			))

		case .architecture:
			suggestions.append(EnhancementSuggestion(
				type: .sharpness,
				title: "建筑摄影增强",
				description: "检测到建筑场景，建议增强锐度和清晰度以突出线条和结构。",
				parameters: ["sharpen": 20.0, "clarity": 25.0, "contrast": 12.0],
				priority: "中"
			))

		case .document:
			suggestions.append(EnhancementSuggestion(
				type: .sharpness,
				title: "文档增强",
				description: "检测到文档场景，建议增强对比度和锐度以提高文字可读性。",
				parameters: ["contrast": 30.0, "sharpen": 25.0, "clarity": 20.0],
				priority: "高"
			))

		case .sunrise:
			suggestions.append(EnhancementSuggestion(
				type: .landscapeEnhancement,
				title: "日出日落增强",
				description: "检测到日出日落场景，建议增强暖色调和饱和度以展现金色时刻。",
				parameters: ["warmth": 15.0, "saturation": 12.0, "contrast": 8.0],
				priority: "中"
			))

		case .flower, .macroDetail:
			suggestions.append(EnhancementSuggestion(
				type: .sharpness,
				title: "微距增强",
				description: "检测到微距/花卉场景，建议增强锐度和饱和度以展现精致细节。",
				parameters: ["sharpen": 20.0, "saturation": 10.0, "clarity": 15.0],
				priority: "中"
			))

		case .stage:
			suggestions.append(EnhancementSuggestion(
				type: .exposure,
				title: "舞台光线优化",
				description: "检测到舞台场景，建议降低高光、提升阴影以平衡舞台光比。",
				parameters: ["highlights": -15.0, "shadows": 15.0, "contrast": 10.0],
				priority: "中"
			))

		case .street:
			suggestions.append(EnhancementSuggestion(
				type: .sharpness,
				title: "街拍增强",
				description: "检测到街拍场景，建议增强对比度和锐度以突出街头氛围。",
				parameters: ["contrast": 15.0, "sharpen": 12.0, "clarity": 10.0],
				priority: "中"
			))

		case .indoor:
			suggestions.append(EnhancementSuggestion(
				type: .exposure,
				title: "室内光线补偿",
				description: "检测到室内场景，建议提升亮度和阴影以补偿室内光线不足。",
				parameters: ["brightness": 10.0, "shadows": 12.0],
				priority: "中"
			))

		case .groupPhoto:
			suggestions.append(EnhancementSuggestion(
				type: .portraitEnhancement,
				title: "合影优化",
				description: "检测到合影场景，建议均衡亮度、增强锐度以确保每个人都清晰可见。",
				parameters: ["brightness": 8.0, "sharpen": 15.0, "contrast": 5.0],
				priority: "中"
			))

		case .waterScene:
			suggestions.append(EnhancementSuggestion(
				type: .landscapeEnhancement,
				title: "水景增强",
				description: "检测到水景场景，建议增强饱和度并降低高光以展现水面质感。",
				parameters: ["saturation": 12.0, "highlights": -10.0, "clarity": 10.0],
				priority: "中"
			))

		case .silhouette:
			suggestions.append(EnhancementSuggestion(
				type: .exposure,
				title: "剪影优化",
				description: "检测到剪影场景，建议增强对比度并降低阴影以强化剪影效果。",
				parameters: ["contrast": 20.0, "shadows": -10.0, "highlights": -15.0],
				priority: "中"
			))

		case .texture:
			suggestions.append(EnhancementSuggestion(
				type: .sharpness,
				title: "纹理增强",
				description: "检测到纹理场景，建议大幅增强锐度和清晰度以突出纹理细节。",
				parameters: ["sharpen": 30.0, "clarity": 35.0, "contrast": 10.0],
				priority: "中"
			))

		case .unknown:
			break
		}

		return suggestions
	}

	// MARK: - 光环境适配建议

	/// 根据光环境分析结果生成适配建议
	private func generateLightBasedSuggestions(light: LightAnalysis) -> [EnhancementSuggestion] {
		var suggestions: [EnhancementSuggestion] = []

		// 逆光场景：曝光补偿
		if light.isBacklit {
			suggestions.append(EnhancementSuggestion(
				type: .exposure,
				title: "逆光补偿",
				description: "检测到逆光场景，建议提升阴影并增加曝光补偿以还原主体细节。",
				parameters: ["shadows": 20.0, "exposure": 10.0, "brightness": 12.0],
				priority: "高"
			))
		}

		// 低光环境：降噪 + 提亮
		if light.brightness < 0.25 {
			suggestions.append(EnhancementSuggestion(
				type: .noiseReduction,
				title: "低光降噪",
				description: "当前环境亮度较低，建议进行降噪处理并提升画面亮度。",
				parameters: ["noise_reduction": 20.0, "brightness": 18.0, "shadows": 15.0],
				priority: "高"
			))
		} else if light.brightness < 0.4 {
			suggestions.append(EnhancementSuggestion(
				type: .exposure,
				title: "亮度提升",
				description: "当前环境偏暗，建议适当提升亮度以获得更好的画面效果。",
				parameters: ["brightness": 10.0, "shadows": 8.0],
				priority: "低"
			))
		}

		// 暖光环境：色温调整
		if light.lightType == .warm {
			suggestions.append(EnhancementSuggestion(
				type: .colorHarmony,
				title: "色温平衡",
				description: "检测到暖色光源，建议适当降低色温以获得更自然的白平衡。",
				parameters: ["warmth": -10.0],
				priority: "低"
			))
		}

		// 冷光环境：暖色调整
		if light.lightType == .cool {
			suggestions.append(EnhancementSuggestion(
				type: .colorHarmony,
				title: "暖色补偿",
				description: "检测到冷色光源，建议增加暖色调以平衡画面色彩。",
				parameters: ["warmth": 10.0],
				priority: "低"
			))
		}

		// 荧光灯环境
		if light.lightType == .fluorescent {
			suggestions.append(EnhancementSuggestion(
				type: .colorHarmony,
				title: "荧光灯色彩校正",
				description: "检测到荧光灯光源，建议进行色彩校正以消除偏色。",
				parameters: ["warmth": 5.0, "saturation": -5.0],
				priority: "低"
			))
		}

		// 混合光源
		if light.lightType == .mixed {
			suggestions.append(EnhancementSuggestion(
				type: .colorHarmony,
				title: "混合光源平衡",
				description: "检测到混合光源，建议进行白平衡微调以获得统一色调。",
				parameters: ["warmth": 3.0, "saturation": 5.0],
				priority: "低"
			))
		}

		return suggestions
	}

	// MARK: - 建议去重与排序

	/// 去重并按照优先级排序建议
	private func deduplicateAndSort(suggestions: [EnhancementSuggestion]) -> [EnhancementSuggestion] {
		// 按类型去重：同一类型保留优先级最高的
		var seenTypes: [EnhancementType: EnhancementSuggestion] = [:]
		let priorityOrder: [String: Int] = ["高": 0, "中": 1, "低": 2]

		for suggestion in suggestions {
			if let existing = seenTypes[suggestion.type] {
				let currentPriority = priorityOrder[suggestion.priority] ?? 3
				let existingPriority = priorityOrder[existing.priority] ?? 3
				if currentPriority < existingPriority {
					seenTypes[suggestion.type] = suggestion
				}
			} else {
				seenTypes[suggestion.type] = suggestion
			}
		}

		// 按优先级排序
		return Array(seenTypes.values).sorted { a, b in
			let pa = priorityOrder[a.priority] ?? 3
			let pb = priorityOrder[b.priority] ?? 3
			return pa < pb
		}
	}

	// MARK: - 场景预设参数

	/// 获取场景特定的预设参数
	/// - Parameter scene: 场景类型
	/// - Returns: 针对该场景优化的预设参数
	func getPresetParams(for scene: SceneType) -> ScenePresetParams {
		switch scene {
		case .portrait:
			return ScenePresetParams(
				exposure: 0.12, contrast: -0.08, saturation: 0.08,
				highlights: -0.12, shadows: 0.12, clarity: 0.12,
				warmth: 0.12, sharpness: 0.18, noiseReduction: 0.18, vignette: 0.12
			)
		case .portraitStanding:
			return ScenePresetParams(
				exposure: 0.10, contrast: -0.03, saturation: 0.06,
				highlights: -0.08, shadows: 0.10, clarity: 0.14,
				warmth: 0.10, sharpness: 0.20, noiseReduction: 0.15, vignette: 0.10
			)
		case .portraitSitting:
			return ScenePresetParams(
				exposure: 0.10, contrast: -0.06, saturation: 0.05,
				highlights: -0.10, shadows: 0.14, clarity: 0.10,
				warmth: 0.14, sharpness: 0.16, noiseReduction: 0.20, vignette: 0.14
			)
		case .food:
			return ScenePresetParams(
				exposure: 0.18, contrast: 0.12, saturation: 0.22,
				highlights: -0.05, shadows: 0.08, clarity: 0.22,
				warmth: 0.18, sharpness: 0.22, noiseReduction: 0.08, vignette: 0.18
			)
		case .foodStyling:
			return ScenePresetParams(
				exposure: 0.20, contrast: 0.10, saturation: 0.25,
				highlights: -0.08, shadows: 0.06, clarity: 0.28,
				warmth: 0.20, sharpness: 0.28, noiseReduction: 0.08, vignette: 0.20
			)
		case .landscape:
			return ScenePresetParams(
				exposure: 0.02, contrast: 0.18, saturation: 0.18,
				highlights: -0.18, shadows: 0.18, clarity: 0.22,
				warmth: 0.02, sharpness: 0.22, noiseReduction: 0.08, vignette: 0.06
			)
		case .landscapeNature:
			return ScenePresetParams(
				exposure: 0.00, contrast: 0.16, saturation: 0.20,
				highlights: -0.16, shadows: 0.16, clarity: 0.20,
				warmth: 0.05, sharpness: 0.22, noiseReduction: 0.08, vignette: 0.05
			)
		case .landscapeSunset:
			return ScenePresetParams(
				exposure: -0.08, contrast: 0.22, saturation: 0.28,
				highlights: -0.22, shadows: 0.22, clarity: 0.16,
				warmth: 0.32, sharpness: 0.16, noiseReduction: 0.10, vignette: 0.22
			)
		case .pet:
			return ScenePresetParams(
				exposure: 0.12, contrast: 0.06, saturation: 0.12,
				highlights: -0.06, shadows: 0.12, clarity: 0.16,
				warmth: 0.06, sharpness: 0.22, noiseReduction: 0.14, vignette: 0.12
			)
		case .architecture:
			return ScenePresetParams(
				exposure: 0.02, contrast: 0.18, saturation: 0.06,
				highlights: -0.12, shadows: 0.12, clarity: 0.28,
				warmth: 0.00, sharpness: 0.32, noiseReduction: 0.06, vignette: 0.02
			)
		case .nightScene:
			return ScenePresetParams(
				exposure: 0.28, contrast: 0.12, saturation: -0.06,
				highlights: -0.22, shadows: 0.32, clarity: 0.12,
				warmth: -0.06, sharpness: 0.12, noiseReduction: 0.42, vignette: 0.28
			)
		case .nightPortrait:
			return ScenePresetParams(
				exposure: 0.32, contrast: 0.08, saturation: -0.04,
				highlights: -0.20, shadows: 0.28, clarity: 0.14,
				warmth: -0.04, sharpness: 0.14, noiseReduction: 0.38, vignette: 0.24
			)
		case .document:
			return ScenePresetParams(
				exposure: 0.28, contrast: 0.32, saturation: 0.00,
				highlights: -0.12, shadows: 0.12, clarity: 0.32,
				warmth: 0.00, sharpness: 0.38, noiseReduction: 0.08, vignette: 0.00
			)
		case .sunrise:
			return ScenePresetParams(
				exposure: -0.06, contrast: 0.16, saturation: 0.22,
				highlights: -0.22, shadows: 0.16, clarity: 0.16,
				warmth: 0.28, sharpness: 0.16, noiseReduction: 0.10, vignette: 0.16
			)
		case .snow:
			return ScenePresetParams(
				exposure: 0.32, contrast: 0.06, saturation: -0.12,
				highlights: -0.12, shadows: 0.12, clarity: 0.16,
				warmth: -0.12, sharpness: 0.22, noiseReduction: 0.14, vignette: 0.06
			)
		case .beach:
			return ScenePresetParams(
				exposure: 0.12, contrast: 0.12, saturation: 0.22,
				highlights: -0.16, shadows: 0.12, clarity: 0.22,
				warmth: 0.12, sharpness: 0.22, noiseReduction: 0.06, vignette: 0.02
			)
		case .flower:
			return ScenePresetParams(
				exposure: 0.12, contrast: 0.06, saturation: 0.22,
				highlights: -0.12, shadows: 0.06, clarity: 0.26,
				warmth: 0.06, sharpness: 0.32, noiseReduction: 0.10, vignette: 0.22
			)
		case .macroDetail:
			return ScenePresetParams(
				exposure: 0.08, contrast: 0.08, saturation: 0.18,
				highlights: -0.08, shadows: 0.08, clarity: 0.30,
				warmth: 0.04, sharpness: 0.34, noiseReduction: 0.08, vignette: 0.18
			)
		case .stage:
			return ScenePresetParams(
				exposure: 0.16, contrast: 0.16, saturation: 0.12,
				highlights: -0.16, shadows: 0.22, clarity: 0.16,
				warmth: 0.06, sharpness: 0.16, noiseReduction: 0.22, vignette: 0.16
			)
		case .street:
			return ScenePresetParams(
				exposure: 0.02, contrast: 0.22, saturation: 0.12,
				highlights: -0.12, shadows: 0.16, clarity: 0.22,
				warmth: 0.02, sharpness: 0.22, noiseReduction: 0.10, vignette: 0.16
			)
		case .indoor:
			return ScenePresetParams(
				exposure: 0.16, contrast: 0.02, saturation: 0.06,
				highlights: -0.12, shadows: 0.16, clarity: 0.12,
				warmth: 0.12, sharpness: 0.16, noiseReduction: 0.22, vignette: 0.12
			)
		case .weddingOutdoor:
			return ScenePresetParams(
				exposure: 0.16, contrast: 0.02, saturation: 0.12,
				highlights: -0.12, shadows: 0.12, clarity: 0.12,
				warmth: 0.16, sharpness: 0.16, noiseReduction: 0.10, vignette: 0.16
			)
		case .weddingIndoor:
			return ScenePresetParams(
				exposure: 0.22, contrast: -0.04, saturation: 0.06,
				highlights: -0.16, shadows: 0.22, clarity: 0.12,
				warmth: 0.22, sharpness: 0.16, noiseReduction: 0.22, vignette: 0.22
			)
		case .childrenOutdoor:
			return ScenePresetParams(
				exposure: 0.16, contrast: 0.02, saturation: 0.16,
				highlights: -0.06, shadows: 0.12, clarity: 0.12,
				warmth: 0.12, sharpness: 0.16, noiseReduction: 0.10, vignette: 0.12
			)
		case .childrenIndoor:
			return ScenePresetParams(
				exposure: 0.22, contrast: -0.04, saturation: 0.12,
				highlights: -0.12, shadows: 0.16, clarity: 0.12,
				warmth: 0.16, sharpness: 0.16, noiseReduction: 0.16, vignette: 0.16
			)
		case .productWhite:
			return ScenePresetParams(
				exposure: 0.32, contrast: 0.12, saturation: 0.02,
				highlights: -0.06, shadows: 0.06, clarity: 0.28,
				warmth: 0.02, sharpness: 0.38, noiseReduction: 0.06, vignette: 0.02
			)
		case .groupPhoto:
			return ScenePresetParams(
				exposure: 0.16, contrast: 0.06, saturation: 0.12,
				highlights: -0.12, shadows: 0.12, clarity: 0.16,
				warmth: 0.12, sharpness: 0.22, noiseReduction: 0.16, vignette: 0.12
			)
		case .waterScene:
			return ScenePresetParams(
				exposure: 0.02, contrast: 0.12, saturation: 0.16,
				highlights: -0.16, shadows: 0.12, clarity: 0.22,
				warmth: -0.06, sharpness: 0.22, noiseReduction: 0.10, vignette: 0.06
			)
		case .silhouette:
			return ScenePresetParams(
				exposure: -0.28, contrast: 0.32, saturation: -0.12,
				highlights: -0.32, shadows: 0.32, clarity: 0.12,
				warmth: 0.06, sharpness: 0.12, noiseReduction: 0.16, vignette: 0.32
			)
		case .texture:
			return ScenePresetParams(
				exposure: 0.02, contrast: 0.22, saturation: 0.02,
				highlights: -0.12, shadows: 0.12, clarity: 0.38,
				warmth: 0.02, sharpness: 0.38, noiseReduction: 0.06, vignette: 0.06
			)
		case .unknown:
			return ScenePresetParams.default
		}
	}

	// MARK: - 最优设置

	/// 获取考虑场景和光环境的最优拍摄设置
	/// - Parameters:
	///   - scene: 场景类型
	///   - light: 环境光分析结果
	/// - Returns: 调整后的最优预设参数
	func getOptimalSettings(for scene: SceneType, light: LightAnalysis) -> ScenePresetParams {
		let basePreset = getPresetParams(for: scene)

		var exposure = basePreset.exposure
		var contrast = basePreset.contrast
		var saturation = basePreset.saturation
		var highlights = basePreset.highlights
		var shadows = basePreset.shadows
		var clarity = basePreset.clarity
		var warmth = basePreset.warmth
		var sharpness = basePreset.sharpness
		var noiseReduction = basePreset.noiseReduction
		var vignette = basePreset.vignette

		// 低亮度：增加曝光、增加阴影、降低降噪强度
		if light.brightness < 0.2 {
			exposure += 0.20
			shadows += 0.12
			noiseReduction = max(0.0, noiseReduction - 0.05)
		} else if light.brightness < 0.35 {
			exposure += 0.10
			shadows += 0.06
			noiseReduction = max(0.0, noiseReduction - 0.03)
		}

		// 高亮度：降低曝光、增加高光
		if light.brightness > 0.8 {
			exposure -= 0.15
			highlights += 0.10
		} else if light.brightness > 0.65 {
			exposure -= 0.08
			highlights += 0.05
		}

		// 逆光：大幅增加阴影、微增曝光
		if light.isBacklit {
			shadows += 0.20
			exposure += 0.08
		}

		// 暖光：降低色温
		if light.lightType == .warm {
			warmth -= 0.12
		}

		// 冷光：增加色温
		if light.lightType == .cool {
			warmth += 0.12
		}

		// 荧光灯：微调色温和饱和度
		if light.lightType == .fluorescent {
			warmth += 0.06
			saturation -= 0.04
		}

		// 混合光源：微调色温
		if light.lightType == .mixed {
			warmth += 0.04
		}

		// 高对比度：降低对比度、增加阴影
		if light.contrast > 0.7 {
			contrast -= 0.12
			shadows += 0.08
		} else if light.contrast > 0.55 {
			contrast -= 0.06
			shadows += 0.04
		}

		// 低对比度：增加对比度、降低高光
		if light.contrast < 0.15 {
			contrast += 0.12
			highlights -= 0.08
		} else if light.contrast < 0.25 {
			contrast += 0.06
			highlights -= 0.04
		}

		// 限制各参数在合理范围内
		exposure = max(-0.5, min(0.5, exposure))
		contrast = max(-0.3, min(0.3, contrast))
		saturation = max(-0.3, min(0.3, saturation))
		highlights = max(-0.5, min(0.5, highlights))
		shadows = max(-0.3, min(0.5, shadows))
		clarity = max(0.0, min(0.5, clarity))
		warmth = max(-0.3, min(0.5, warmth))
		sharpness = max(0.0, min(0.5, sharpness))
		noiseReduction = max(0.0, min(0.5, noiseReduction))
		vignette = max(0.0, min(0.4, vignette))

		return ScenePresetParams(
			exposure: exposure,
			contrast: contrast,
			saturation: saturation,
			highlights: highlights,
			shadows: shadows,
			clarity: clarity,
			warmth: warmth,
			sharpness: sharpness,
			noiseReduction: noiseReduction,
			vignette: vignette
		)
	}

	// MARK: - 摘要报告

	/// 生成中文质量评估摘要报告
	/// - Parameters:
	///   - quality: 图像质量评估结果
	///   - suggestions: 生成的增强建议列表
	/// - Returns: 中文格式的摘要报告字符串
	func generateSummaryReport(quality: QualityAssessment, suggestions: [EnhancementSuggestion]) -> String {
		var report = ""

		// 标题
		report += "📊 图像质量评估报告\n"
		report += "━━━━━━━━━━━━━━━━━━\n\n"

		// 综合评分
		report += "【综合评分】\(String(format: "%.1f", quality.overallScore)) 分 (\(quality.qualityGrade.displayName))\n\n"

		// 各维度评分
		report += "【各维度评分】\n"
		report += "  • 清晰度：\(String(format: "%.1f", quality.sharpnessScore)) 分"
		if quality.sharpnessScore < 50 {
			report += " ⚠️ 需要增强"
		} else if quality.sharpnessScore >= 80 {
			report += " ✅ 优秀"
		}
		report += "\n"

		report += "  • 噪点水平：\(String(format: "%.1f", quality.noiseLevel))"
		if quality.noiseLevel > 30 {
			report += " ⚠️ 需要降噪"
		} else if quality.noiseLevel <= 15 {
			report += " ✅ 非常干净"
		}
		report += "\n"

		report += "  • 曝光：\(String(format: "%.1f", quality.exposureScore)) 分"
		if quality.exposureScore < 60 {
			report += " ⚠️ 需要调整"
		} else if quality.exposureScore >= 80 {
			report += " ✅ 优秀"
		}
		report += "\n"

		report += "  • 色彩和谐度：\(String(format: "%.1f", quality.colorHarmonyScore)) 分"
		if quality.colorHarmonyScore < 40 {
			report += " ⚠️ 需要优化"
		} else if quality.colorHarmonyScore >= 80 {
			report += " ✅ 优秀"
		}
		report += "\n"

		report += "  • 分辨率：\(String(format: "%.1f", quality.resolutionScore)) 分"
		if quality.resolutionScore >= 80 {
			report += " ✅ 优秀"
		}
		report += "\n\n"

		// 增强建议
		if suggestions.isEmpty {
			report += "【增强建议】无需调整，画面质量良好。\n"
		} else {
			report += "【增强建议】共 \(suggestions.count) 条\n"
			report += "━━━━━━━━━━━━━━━━━━\n"
			for (index, suggestion) in suggestions.enumerated() {
				let priorityIcon: String = {
					switch suggestion.priority {
					case "高": return "🔴"
					case "中": return "🟡"
					case "低": return "🟢"
					default: return "⚪"
					}
				}()
				report += "\n\(index + 1). \(priorityIcon) [\(suggestion.priority)优先级] \(suggestion.title)\n"
				report += "   \(suggestion.description)\n"
				if !suggestion.parameters.isEmpty {
					let paramsStr = suggestion.parameters.map { "\($0.key)=\(String(format: "%.0f", $0.value))" }.joined(separator: ", ")
					report += "   参数：\(paramsStr)\n"
				}
			}
		}

		report += "\n━━━━━━━━━━━━━━━━━━\n"
		report += "评估时间：\(formatDate(quality.timestamp))\n"

		return report
	}

	// MARK: - 私有辅助方法

	/// 格式化日期为中文格式
	private func formatDate(_ date: Date) -> String {
		let formatter = DateFormatter()
		formatter.locale = Locale(identifier: "zh_CN")
		formatter.dateFormat = "yyyy年MM月dd日 HH:mm:ss"
		return formatter.string(from: date)
	}
}

#endif