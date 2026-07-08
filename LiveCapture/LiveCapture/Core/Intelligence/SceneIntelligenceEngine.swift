//
//  SceneIntelligenceEngine.swift
//  LiveCapture
//
//  AI 场景智能引擎 - 主引擎
//
//  ## 文件作用
//  编排场景分类、环境光分析和主体检测
//  生成自适应拍摄参数，为相机提供智能参数建议
//  作为 ObservableObject 驱动 UI 状态更新
//
//  ## 主要类型
//  ### SceneIntelligenceEngine
//  场景智能引擎主类（ObservableObject）
//
//  ## Published 属性
//  - currentScene: 当前识别场景
//  - sceneConfidence: 场景识别置信度
//  - lightAnalysis: 环境光分析结果
//  - subjectDetection: 主体检测结果
//  - adaptiveParams: 自适应拍摄参数
//  - isReady: 引擎是否已就绪
//
//  ## 主要方法
//
//  ### 帧分析
//  - analyzeFrame(_:orientation:): 分析视频帧
//    策略:
//      - 500ms 节流控制
//      - 并行执行场景分类、光环境分析和主体检测
//      - 组合结果生成自适应参数
//      - 更新 @Published 属性到主线程
//
//  ### 参数建议
//  - getSuggestedLens(): 基于场景类型推荐镜头
//  - getSuggestedZoomFactor(): 基于场景类型推荐变焦倍数
//  - computeAdaptiveParams(scene:light:subject:): 计算自适应参数
//
//  ## 线程安全
//  - 使用专用 queue 执行分析操作
//  - @Published 属性更新确保在主线程
//  - 节流控制避免过载
//

import Foundation
import AVFoundation
import Combine

#if os(iOS)

/// 场景智能引擎 - 编排场景分析并生成自适应参数
final class SceneIntelligenceEngine: ObservableObject {

	// MARK: - Published 属性

	/// 当前识别场景
	@Published var currentScene: SceneType = .unknown
	/// 场景识别置信度
	@Published var sceneConfidence: Float = 0.0
	/// 环境光分析结果
	@Published var lightAnalysis: LightAnalysis = .default
	/// 主体检测结果
	@Published var subjectDetection: SubjectDetection = .default
	/// 自适应拍摄参数
	@Published var adaptiveParams: AdaptiveCaptureParams = .defaultParams
	/// 引擎是否已就绪（至少完成一次分析）
	@Published var isReady: Bool = false

	// MARK: - 私有属性

	private let classifier = SceneClassifier()
	private let queue = DispatchQueue(label: "livecapture.intelligence.engine", qos: .userInitiated)
	private var lastAnalysisTime: Date = Date()
	private let analysisInterval: TimeInterval = 0.5  // 每 500ms 分析一次
	private var analysisInProgress: Bool = false

	// MARK: - 帧分析

	/// 分析视频帧，节流控制防止过载
	/// - Parameters:
	///   - pixelBuffer: 输入像素缓冲
	///   - orientation: 图像方向
	func analyzeFrame(_ pixelBuffer: CVPixelBuffer, orientation: CGImagePropertyOrientation) {
		// 节流控制：距离上次分析不足 500ms 则跳过
		let now = Date()
		guard now.timeIntervalSince(lastAnalysisTime) >= analysisInterval else { return }
		guard !analysisInProgress else { return }

		lastAnalysisTime = now
		analysisInProgress = true

		queue.async { [weak self] in
			guard let self else { return }

			// 同步执行光环境分析（无需 Vision 检测，可在当前队列完成）
			let light = self.classifier.analyzeLight(from: pixelBuffer)

			// 异步执行场景分类
			self.classifier.classifyScene(from: pixelBuffer) { [weak self] sceneType, confidence in
				guard let self else { return }

				// 异步执行主体检测
				self.classifier.detectSubjects(from: pixelBuffer) { [weak self] subjects in
					guard let self else { return }

					// 组合所有结果，生成自适应参数
					let params = self.computeAdaptiveParams(
						scene: sceneType,
						confidence: confidence,
						light: light,
						subject: subjects
					)

					// 更新 @Published 属性到主线程
					DispatchQueue.main.async {
						self.currentScene = sceneType
						self.sceneConfidence = confidence
						self.lightAnalysis = light
						self.subjectDetection = subjects
						self.adaptiveParams = params
						self.isReady = true
					}

					self.analysisInProgress = false
				}
			}
		}
	}

	// MARK: - 镜头建议

	/// 基于当前场景类型推荐镜头
	/// - Returns: 镜头类型字符串 ("wide", "telephoto", "ultraWide")
	func getSuggestedLens() -> String {
		switch currentScene {
		case .portrait:
			// 人像：优先使用长焦以获得自然透视
			return "telephoto"
		case .food:
			// 美食：使用长焦获得浅景深效果
			return "telephoto"
		case .landscape:
			// 风景：使用超广角获得广阔视野
			return "ultraWide"
		case .pet:
			// 宠物：使用广角以便近距离拍摄
			return "wide"
		case .architecture:
			// 建筑：使用超广角捕捉全貌
			return "ultraWide"
		case .nightScene:
			// 夜景：使用广角（通常光圈更大）
			return "wide"
		case .document:
			// 文档：使用广角，避免畸变
			return "wide"
		case .sunrise:
			// 日出日落：使用超广角捕捉天空
			return "ultraWide"
		case .snow:
			// 雪景：使用超广角
			return "ultraWide"
		case .beach:
			// 海滩：使用超广角
			return "ultraWide"
		case .flower:
			// 花卉：使用长焦获得特写
			return "telephoto"
		case .stage:
			// 舞台：使用长焦从远处拍摄
			return "telephoto"
		case .street:
			// 街拍：使用广角，灵活构图
			return "wide"
		case .indoor:
			// 室内：使用广角（通常光圈更大，空间限制）
			return "wide"
		case .unknown:
			// 未知：保持当前镜头
			return "wide"
		}
	}

	// MARK: - 变焦建议

	/// 基于当前场景类型推荐变焦倍数
	/// - Returns: 建议的变焦倍率
	func getSuggestedZoomFactor() -> CGFloat {
		switch currentScene {
		case .portrait:
			// 人像：使用 2x-3x 变焦获得自然透视
			return 2.0
		case .food:
			// 美食：使用 1.5x-2x 变焦
			return 1.5
		case .landscape:
			// 风景：使用 0.5x 超广角
			return 0.5
		case .pet:
			// 宠物：使用 1x 广角
			return 1.0
		case .architecture:
			// 建筑：使用 0.5x 超广角
			return 0.5
		case .nightScene:
			// 夜景：使用 1x 广角（光圈更大）
			return 1.0
		case .document:
			// 文档：使用 1x
			return 1.0
		case .sunrise:
			// 日出日落：使用 0.5x 超广角
			return 0.5
		case .snow:
			// 雪景：使用 0.5x 超广角
			return 0.5
		case .beach:
			// 海滩：使用 0.5x 超广角
			return 0.5
		case .flower:
			// 花卉：使用 2x-3x 变焦获得特写
			return 2.5
		case .stage:
			// 舞台：使用 3x 长焦
			return 3.0
		case .street:
			// 街拍：使用 1x
			return 1.0
		case .indoor:
			// 室内：使用 1x
			return 1.0
		case .unknown:
			return 1.0
		}
	}

	// MARK: - 自适应参数计算

	/// 基于场景、光环境和主体检测结果计算自适应拍摄参数
	/// - Parameters:
	///   - scene: 识别场景类型
	///   - confidence: 场景识别置信度
	///   - light: 环境光分析结果
	///   - subject: 主体检测结果
	/// - Returns: 自适应拍摄参数
	private func computeAdaptiveParams(
		scene: SceneType,
		confidence: Float,
		light: LightAnalysis,
		subject: SubjectDetection
	) -> AdaptiveCaptureParams {
		// 基础参数（从环境光分析推导）
		let baseISO = computeBaseISO(brightness: light.brightness)
		let baseShutterSpeed = computeBaseShutterSpeed(brightness: light.brightness)
		let exposureBias = computeExposureBias(scene: scene, light: light, confidence: confidence)
		let whiteBalanceTint = computeWhiteBalanceTint(light: light)
		let whiteBalanceTemperature = light.colorTemperature
		let zoomFactor = getSuggestedZoomFactor()
		let lensType = getSuggestedLens()
		let flash = computeFlashRecommendation(scene: scene, light: light) 

		// 根据场景微调 ISO
		let adjustedISO = adjustISOForScene(baseISO: baseISO, scene: scene, subject: subject)

		// 根据场景微调快门速度
		let adjustedShutterSpeed = adjustShutterSpeedForScene(
			baseShutterSpeed: baseShutterSpeed,
			scene: scene,
			subject: subject
		)

		return AdaptiveCaptureParams(
			targetISO: adjustedISO,
			targetShutterSpeed: adjustedShutterSpeed,
			exposureBias: exposureBias,
			whiteBalanceTint: whiteBalanceTint,
			whiteBalanceTemperature: whiteBalanceTemperature,
			suggestedZoomFactor: zoomFactor,
			suggestedLensType: lensType,
			flashMode: flash
		)
	}

	/// 根据亮度计算基础 ISO
	private func computeBaseISO(brightness: Float) -> Float {
		// 亮度 0.5 对应 ISO 200
		// 亮度越低，ISO 越高
		if brightness < 0.1 {
			return 800
		} else if brightness < 0.2 {
			return 400
		} else if brightness < 0.35 {
			return 250
		} else if brightness < 0.55 {
			return 200
		} else if brightness < 0.7 {
			return 125
		} else {
			return 100
		}
	}

	/// 根据亮度计算基础快门速度
	private func computeBaseShutterSpeed(brightness: Float) -> Float {
		// 亮度 0.5 对应 1/120s
		if brightness < 0.1 {
			return 1.0 / 30.0
		} else if brightness < 0.2 {
			return 1.0 / 60.0
		} else if brightness < 0.35 {
			return 1.0 / 100.0
		} else if brightness < 0.55 {
			return 1.0 / 120.0
		} else if brightness < 0.7 {
			return 1.0 / 250.0
		} else {
			return 1.0 / 500.0
		}
	}

	/// 计算曝光补偿值
	private func computeExposureBias(scene: SceneType, light: LightAnalysis, confidence: Float) -> Float {
		var bias: Float = 0.0

		// 逆光场景：增加曝光补偿
		if light.isBacklit {
			bias += 0.7
		}

		// 雪景/海滩：增加曝光补偿防止欠曝
		if scene == .snow || scene == .beach {
			bias += 0.5
		}

		// 夜景：降低曝光补偿，保留暗部细节
		if scene == .nightScene {
			bias -= 0.5
		}

		// 舞台：降低曝光补偿，避免过曝
		if scene == .stage {
			bias -= 0.3
		}

		// 根据置信度调整补偿强度
		let adjustedBias = bias * confidence

		// 限制在 -2 到 +2 EV 范围内
		return max(-2.0, min(2.0, adjustedBias))
	}

	/// 计算白平衡色调偏移
	private func computeWhiteBalanceTint(light: LightAnalysis) -> Float {
		switch light.lightType {
		case .fluorescent:
			return 0.1  // 荧光灯偏绿，增加品红偏移
		case .warm:
			return -0.05  // 暖光略微减少暖色
		case .cool:
			return 0.05  // 冷光略微增加暖色
		case .mixed, .natural:
			return 0.0
		}
	}

	/// 根据场景微调 ISO
	private func adjustISOForScene(baseISO: Float, scene: SceneType, subject: SubjectDetection) -> Float {
		var iso = baseISO

		switch scene {
		case .portrait:
			// 人像：适当降低 ISO 保证画质
			iso *= 0.8
		case .nightScene:
			// 夜景：可能需要更高 ISO
			iso *= 1.3
		case .stage:
			// 舞台：保持较低 ISO，使用较慢快门
			iso *= 0.7
		case .pet:
			// 宠物：提高 ISO 保证快门速度
			iso *= 1.2
		case .street:
			// 街拍：提高 ISO 保证快门速度
			iso *= 1.15
		default:
			break
		}

		// 如果有人物，优先保证画质
		if subject.hasHuman {
			iso *= 0.9
		}

		// 限制 ISO 范围
		return max(50, min(1600, iso))
	}

	/// 根据场景微调快门速度
	private func adjustShutterSpeedForScene(
		baseShutterSpeed: Float,
		scene: SceneType,
		subject: SubjectDetection
	) -> Float {
		var speed = baseShutterSpeed

		switch scene {
		case .portrait:
			// 人像：适当快门速度即可
			speed = max(speed, 1.0 / 100.0)
		case .pet:
			// 宠物：需要更快的快门冻结动作
			speed = max(speed, 1.0 / 250.0)
		case .street:
			// 街拍：需要较快快门
			speed = max(speed, 1.0 / 200.0)
		case .stage:
			// 舞台：可以使用较慢快门
			speed = min(speed, 1.0 / 60.0)
		case .nightScene:
			// 夜景：使用较慢快门
			speed = min(speed, 1.0 / 30.0)
		case .landscape, .architecture:
			// 风景/建筑：可以使用较慢快门（假设使用三脚架）
			speed = min(speed, 1.0 / 60.0)
		default:
			break
		}

		// 如果有人物，快门不能太慢
		if subject.hasHuman && speed < 1.0 / 60.0 {
			speed = 1.0 / 60.0
		}

		// 限制快门速度范围
		return max(1.0 / 8000.0, min(1.0, speed))
	}

	/// 计算闪光灯推荐
	private func computeFlashRecommendation(scene: SceneType, light: LightAnalysis) -> AdaptiveCaptureParams.FlashRecommendation {
		// 极暗环境：开启闪光灯
		if light.brightness < 0.1 {
			return .on
		}

		// 逆光人像：开启闪光灯补光
		if light.isBacklit && scene == .portrait {
			return .on
		}

		// 以下场景禁用闪光灯
		switch scene {
		case .landscape, .nightScene, .sunrise, .stage, .architecture:
			return .off
		case .document:
			// 文档拍摄：可能开启闪光灯消除阴影
			return .auto
		default:
			return .auto
		}
	}

	// MARK: - 重置

	/// 重置引擎到初始状态
	func reset() {
		DispatchQueue.main.async { [weak self] in
			guard let self else { return }
			self.currentScene = .unknown
			self.sceneConfidence = 0.0
			self.lightAnalysis = .default
			self.subjectDetection = .default
			self.adaptiveParams = .defaultParams
			self.isReady = false
		}
		analysisInProgress = false
		lastAnalysisTime = Date()
	}
}

#endif