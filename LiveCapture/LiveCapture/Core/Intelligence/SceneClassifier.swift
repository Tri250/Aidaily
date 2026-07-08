//
//  SceneClassifier.swift
//  LiveCapture
//
//  AI 场景分类器 - 基于 Vision 框架的场景识别、光环境分析和主体检测
//
//  ## 文件作用
//  使用 Apple Vision 框架实现场景分类、环境光分析和主体检测
//  VNClassifyImageRequest 提供内置场景分类（需 iOS 17+）
//  同时实现基于像素缓冲的规则化兜底分析
//
//  ## 主要类型
//  ### SceneClassifier
//  场景分类器主类
//
//  ## 主要方法
//
//  ### 场景分类
//  - classifyScene(from:completion:): 使用 Vision 分类场景并返回 SceneType 和置信度
//    策略:
//      - 优先使用 VNClassifyImageRequest（iOS 17+）
//      - 映射 Vision 标签到我们的 SceneType 枚举
//      - 结合主体检测结果进行二次判断
//      - 使用规则化兜底作为后备方案
//
//  ### 光环境分析
//  - analyzeLight(from:): 分析像素缓冲的环境光属性
//    计算:
//      - 色温：从 RGB 通道比例估算
//      - 亮度：从亮度直方图均值
//      - 对比度：从亮度直方图标准差
//      - 逆光：从亮度空间分布判断
//      - 光源类型：从色温和亮度分布判断
//
//  ### 主体检测
//  - detectSubjects(from:completion:): 检测画面中的主体
//    使用:
//      - VNDetectHumanRectanglesRequest: 人体检测
//      - VNRecognizeAnimalsRequest: 动物检测（猫/狗）
//      - VNDetectFaceRectanglesRequest: 人脸检测
//      - 组合结果生成 SubjectDetection
//
//  ## 线程安全
//  - 使用专用 queue 执行所有检测操作
//  - 通过 completion 异步返回结果
//  - 不阻塞主线程
//

import Foundation
import Vision
import CoreImage
import AVFoundation
import Accelerate

#if os(iOS)

/// 基于 Vision 框架的场景分类器
final class SceneClassifier {

	// MARK: - 私有属性

	private let queue = DispatchQueue(label: "livecapture.scene.classifier", qos: .userInitiated)
	private let ciContext = CIContext(options: [.workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!])

	/// 场景分类所需的 Vision 标签到 SceneType 的映射表
	private let sceneLabelMapping: [String: SceneType] = [
		"portrait": .portrait,
		"human": .portrait,
		"people": .portrait,
		"person": .portrait,
		"food": .food,
		"cuisine": .food,
		"meal": .food,
		"dish": .food,
		"landscape": .landscape,
		"nature": .landscape,
		"mountain": .landscape,
		"forest": .landscape,
		"sky": .landscape,
		"animal": .pet,
		"dog": .pet,
		"cat": .pet,
		"pet": .pet,
		"building": .architecture,
		"architecture": .architecture,
		"city": .architecture,
		"night": .nightScene,
		"nightlife": .nightScene,
		"document": .document,
		"text": .document,
		"paper": .document,
		"sunrise": .sunrise,
		"sunset": .sunrise,
		"dusk": .sunrise,
		"dawn": .sunrise,
		"snow": .snow,
		"snowscape": .snow,
		"beach": .beach,
		"ocean": .beach,
		"seaside": .beach,
		"flower": .flower,
		"blossom": .flower,
		"stage": .stage,
		"performance": .stage,
		"concert": .stage,
		"street": .street,
		"road": .street,
		"indoor": .indoor,
		"interior": .indoor,
		"room": .indoor
	]

	// MARK: - 场景分类

	/// 对像素缓冲进行场景分类
	/// - Parameters:
	///   - pixelBuffer: 输入像素缓冲
	///   - completion: 完成回调，返回场景类型和置信度
	func classifyScene(from pixelBuffer: CVPixelBuffer, completion: @escaping (SceneType, Float) -> Void) {
		queue.async { [weak self] in
			guard let self else {
				completion(.unknown, 0.0)
				return
			}

			// 使用 Vision 内置场景分类
			if #available(iOS 17.0, *) {
				self.performVisionClassification(pixelBuffer: pixelBuffer, completion: completion)
			} else {
				// iOS 17 以下使用规则化兜底
				let light = self.analyzeLight(from: pixelBuffer)
				let sceneType = self.ruleBasedClassification(light: light)
				completion(sceneType, 0.6)
			}
		}
	}

	/// 使用 VNClassifyImageRequest 进行场景分类
	@available(iOS 17.0, *)
	private func performVisionClassification(pixelBuffer: CVPixelBuffer, completion: @escaping (SceneType, Float) -> Void) {
		let request = VNClassifyImageRequest()
		let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])

		do {
			try handler.perform([request])
			guard let observations = request.results else {
				let light = self.analyzeLight(from: pixelBuffer)
				let sceneType = self.ruleBasedClassification(light: light)
				completion(sceneType, 0.5)
				return
			}

			// 收集所有分类结果，按置信度排序
			var classificationResults: [(label: String, confidence: Float)] = []
			for observation in observations {
				let label = observation.identifier.lowercased()
				let confidence = observation.confidence
				classificationResults.append((label: label, confidence: confidence))
			}
			classificationResults.sort { $0.confidence > $1.confidence }

			// 遍历分类结果，尝试映射到 SceneType
			var bestScene: SceneType = .unknown
			var bestConfidence: Float = 0.0

			for result in classificationResults {
				// 直接匹配
				if let mapped = self.sceneLabelMapping[result.label] {
					if bestScene == .unknown || result.confidence > bestConfidence {
						bestScene = mapped
						bestConfidence = result.confidence
					}
				}
				// 子串匹配
				if bestScene == .unknown {
					for (key, sceneType) in self.sceneLabelMapping {
						if result.label.contains(key) || key.contains(result.label) {
							bestScene = sceneType
							bestConfidence = result.confidence
							break
						}
					}
				}
			}

			// 如果 Vision 分类成功，使用规则化分析进行微调
			if bestScene != .unknown {
				let light = self.analyzeLight(from: pixelBuffer)
				let refined = self.refineClassification(scene: bestScene, confidence: bestConfidence, light: light)
				completion(refined.scene, refined.confidence)
			} else {
				// Vision 分类失败，使用规则化兜底
				let light = self.analyzeLight(from: pixelBuffer)
				let sceneType = self.ruleBasedClassification(light: light)
				completion(sceneType, 0.5)
			}
		} catch {
			let light = self.analyzeLight(from: pixelBuffer)
			let sceneType = self.ruleBasedClassification(light: light)
			completion(sceneType, 0.4)
		}
	}

	/// 使用光环境分析结果微调场景分类
	private func refineClassification(scene: SceneType, confidence: Float, light: LightAnalysis) -> (scene: SceneType, confidence: Float) {
		// 夜景微调：低亮度 + 特定场景 → 可能是夜景
		if light.brightness < 0.3 && scene != .nightScene {
			return (.nightScene, confidence * 0.8)
		}
		// 日出日落微调：低色温 + 自然光 → 可能是日出日落
		if light.colorTemperature < 4000 && light.colorTemperature > 2500 && light.lightType == .natural {
			if scene == .landscape || scene == .unknown {
				return (.sunrise, confidence * 0.7)
			}
		}
		// 雪景微调：高亮度 + 低对比度 → 可能是雪景
		if light.brightness > 0.7 && light.contrast < 0.2 {
			if scene == .landscape || scene == .unknown {
				return (.snow, confidence * 0.7)
			}
		}
		// 舞台微调：混合光源 + 高对比度 → 可能是舞台
		if light.lightType == .mixed && light.contrast > 0.6 {
			if scene == .indoor || scene == .unknown {
				return (.stage, confidence * 0.7)
			}
		}
		return (scene, confidence)
	}

	/// 基于光环境分析的规则化场景分类
	private func ruleBasedClassification(light: LightAnalysis) -> SceneType {
		// 极低亮度 → 夜景
		if light.brightness < 0.15 {
			return .nightScene
		}
		// 低亮度 + 逆光 → 可能是夜景或室内
		if light.brightness < 0.3 {
			if light.isBacklit {
				return .nightScene
			} else {
				return .indoor
			}
		}
		// 暖色温 + 自然光 → 日出日落
		if light.colorTemperature < 4000 && light.lightType == .natural {
			return .sunrise
		}
		// 高亮度 + 低对比度 → 雪景/海滩
		if light.brightness > 0.7 && light.contrast < 0.2 {
			return .snow
		}
		// 荧光灯光源 → 室内
		if light.lightType == .fluorescent {
			return .indoor
		}
		// 混合光源 + 高对比度 → 舞台
		if light.lightType == .mixed && light.contrast > 0.5 {
			return .stage
		}
		// 默认返回未知
		return .unknown
	}

	// MARK: - 环境光分析

	/// 分析像素缓冲的环境光属性
	/// - Parameter pixelBuffer: 输入像素缓冲
	/// - Returns: 环境光分析结果
	func analyzeLight(from pixelBuffer: CVPixelBuffer) -> LightAnalysis {
		let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
		let extent = ciImage.extent
		let width = Int(extent.width)
		let height = Int(extent.height)

		// 创建灰度渲染缓冲
		guard let grayBuffer = renderGrayscaleBuffer(from: ciImage, width: width, height: height) else {
			return .default
		}
		defer { CVPixelBufferUnlockBaseAddress(grayBuffer, .readOnly) }

		CVPixelBufferLockBaseAddress(grayBuffer, .readOnly)
		guard let baseAddress = CVPixelBufferGetBaseAddress(grayBuffer) else {
			return .default
		}
		let bytesPerRow = CVPixelBufferGetBytesPerRow(grayBuffer)

		// 计算亮度统计
		let brightness = computeBrightness(from: baseAddress, bytesPerRow: bytesPerRow, width: width, height: height)
		let contrast = computeContrast(from: baseAddress, bytesPerRow: bytesPerRow, width: width, height: height, mean: brightness)
		let isBacklit = detectBacklit(from: baseAddress, bytesPerRow: bytesPerRow, width: width, height: height)

		// 计算色温
		let colorTemperature = estimateColorTemperature(from: ciImage, width: width, height: height)

		// 判断光源类型
		let lightType = determineLightType(colorTemperature: colorTemperature, brightness: brightness, contrast: contrast)

		return LightAnalysis(
			colorTemperature: colorTemperature,
			brightness: brightness,
			contrast: contrast,
			isBacklit: isBacklit,
			lightType: lightType
		)
	}

	/// 渲染灰度缓冲
	private func renderGrayscaleBuffer(from ciImage: CIImage, width: Int, height: Int) -> CVPixelBuffer? {
		var outBuffer: CVPixelBuffer?
		let attributes: [String: Any] = [
			kCVPixelBufferCGImageCompatibilityKey as String: true,
			kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
		]
		let status = CVPixelBufferCreate(kCFAllocatorDefault, width, height,
										 kCVPixelFormatType_OneComponent8, attributes as CFDictionary, &outBuffer)
		guard status == kCVReturnSuccess, let outBuffer else { return nil }

		// 使用灰度色彩空间渲染
		let grayFilter = CIFilter(name: "CIPhotoEffectMono", parameters: [kCIInputImageKey: ciImage])
		guard let grayImage = grayFilter?.outputImage else { return nil }
		ciContext.render(grayImage, to: outBuffer)
		return outBuffer
	}

	/// 计算平均亮度
	private func computeBrightness(from baseAddress: UnsafeMutableRawPointer, bytesPerRow: Int, width: Int, height: Int) -> Float {
		let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)
		var totalSum: UInt64 = 0
		let totalPixels = width * height

		// 采样加速：每 4 个像素采样一次
		let stepX = 4
		let stepY = 4
		var sampleCount = 0

		for y in stride(from: 0, to: height, by: stepY) {
			for x in stride(from: 0, to: width, by: stepX) {
				let offset = y * bytesPerRow + x
				totalSum += UInt64(ptr[offset])
				sampleCount += 1
			}
		}

		guard sampleCount > 0 else { return 0.5 }
		return Float(totalSum) / Float(sampleCount * 255)
	}

	/// 计算对比度（亮度标准差）
	private func computeContrast(from baseAddress: UnsafeMutableRawPointer, bytesPerRow: Int, width: Int, height: Int, mean: Float) -> Float {
		let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)
		let mean256 = mean * 255.0
		var sumSquaredDiff: Float = 0.0

		let stepX = 4
		let stepY = 4
		var sampleCount = 0

		for y in stride(from: 0, to: height, by: stepY) {
			for x in stride(from: 0, to: width, by: stepX) {
				let offset = y * bytesPerRow + x
				let diff = Float(ptr[offset]) - mean256
				sumSquaredDiff += diff * diff
				sampleCount += 1
			}
		}

		guard sampleCount > 0 else { return 0.3 }
		let variance = sumSquaredDiff / Float(sampleCount)
		// 归一化：最大可能标准差为 128（范围 0-255）
		let stdDev = sqrt(variance)
		return min(1.0, stdDev / 128.0)
	}

	/// 检测逆光：边缘亮度显著高于中心亮度
	private func detectBacklit(from baseAddress: UnsafeMutableRawPointer, bytesPerRow: Int, width: Int, height: Int) -> Bool {
		let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)

		let edgeMargin = min(width, height) / 8
		var edgeSum: UInt64 = 0
		var edgeCount = 0
		var centerSum: UInt64 = 0
		var centerCount = 0

		let centerStartX = width / 4
		let centerEndX = width * 3 / 4
		let centerStartY = height / 4
		let centerEndY = height * 3 / 4

		for y in 0..<height {
			for x in 0..<width {
				let offset = y * bytesPerRow + x
				let value = UInt64(ptr[offset])

				// 边缘区域
				if x < edgeMargin || x >= width - edgeMargin || y < edgeMargin || y >= height - edgeMargin {
					edgeSum += value
					edgeCount += 1
				}

				// 中心区域
				if x >= centerStartX && x < centerEndX && y >= centerStartY && y < centerEndY {
					centerSum += value
					centerCount += 1
				}
			}
		}

		guard edgeCount > 0, centerCount > 0 else { return false }
		let edgeAvg = Float(edgeSum) / Float(edgeCount)
		let centerAvg = Float(centerSum) / Float(centerCount)

		// 边缘比中心亮 30% 以上 → 逆光
		return edgeAvg > centerAvg * 1.3
	}

	/// 估算色温：从 RGB 通道比例估算
	private func estimateColorTemperature(from ciImage: CIImage, width: Int, height: Int) -> Float {
		// 缩小图像以提高性能
		let scale = min(1.0, 64.0 / max(CGFloat(width), CGFloat(height)))
		let scaledImage = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
		let scaledExtent = scaledImage.extent
		let scaledWidth = Int(scaledExtent.width)
		let scaledHeight = Int(scaledExtent.height)

		var outBuffer: CVPixelBuffer?
		let attributes: [String: Any] = [
			kCVPixelBufferCGImageCompatibilityKey as String: true,
			kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
		]
		guard CVPixelBufferCreate(kCFAllocatorDefault, scaledWidth, scaledHeight,
								  kCVPixelFormatType_32BGRA, attributes as CFDictionary, &outBuffer) == kCVReturnSuccess,
			  let outBuffer else { return 5500 }

		ciContext.render(scaledImage, to: outBuffer)

		CVPixelBufferLockBaseAddress(outBuffer, .readOnly)
		defer { CVPixelBufferUnlockBaseAddress(outBuffer, .readOnly) }

		guard let baseAddress = CVPixelBufferGetBaseAddress(outBuffer) else { return 5500 }
		let bytesPerRow = CVPixelBufferGetBytesPerRow(outBuffer)
		let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)

		var totalR: UInt64 = 0
		var totalG: UInt64 = 0
		var totalB: UInt64 = 0
		var count = 0

		for y in 0..<scaledHeight {
			for x in 0..<scaledWidth {
				let offset = y * bytesPerRow + x * 4
				let b = UInt64(ptr[offset])
				let g = UInt64(ptr[offset + 1])
				let r = UInt64(ptr[offset + 2])
				totalR += r
				totalG += g
				totalB += b
				count += 1
			}
		}

		guard count > 0 else { return 5500 }

		let avgR = Float(totalR) / Float(count)
		let avgG = Float(totalG) / Float(count)
		let avgB = Float(totalB) / Float(count)

		// 基于 RGB 比例估算色温
		// 简化算法：红/蓝比例越大，色温越低
		guard avgB > 0 else { return 2000 }
		let rbRatio = avgR / avgB

		if rbRatio > 1.5 {
			// 暖色调（低色温）：2000K - 4000K
			let normalizedRatio = min(1.0, (rbRatio - 1.5) / 2.0)
			return 4000 - normalizedRatio * 2000
		} else if rbRatio < 0.8 {
			// 冷色调（高色温）：5500K - 10000K
			let normalizedRatio = min(1.0, (0.8 - rbRatio) / 0.5)
			return 5500 + normalizedRatio * 4500
		} else {
			// 中性色温：4000K - 5500K
			let normalizedRatio = (rbRatio - 0.8) / 0.7
			return 4000 + normalizedRatio * 1500
		}
	}

	/// 判断光源类型
	private func determineLightType(colorTemperature: Float, brightness: Float, contrast: Float) -> LightAnalysis.LightType {
		if brightness < 0.15 {
			return .natural  // 极暗环境，视为自然光（夜间）
		}
		if colorTemperature < 3500 {
			return .warm
		}
		if colorTemperature > 6500 {
			return .cool
		}
		// 荧光灯特征：特定色温范围 + 特殊的亮度特征
		if colorTemperature > 3800 && colorTemperature < 4500 && contrast > 0.3 {
			return .fluorescent
		}
		// 混合光：高对比度 + 中等色温
		if contrast > 0.5 {
			return .mixed
		}
		return .natural
	}

	// MARK: - 主体检测

	/// 检测画面中的主体
	/// - Parameters:
	///   - pixelBuffer: 输入像素缓冲
	///   - completion: 完成回调，返回主体检测结果
	func detectSubjects(from pixelBuffer: CVPixelBuffer, completion: @escaping (SubjectDetection) -> Void) {
		queue.async { [weak self] in
			guard let self else {
				completion(.default)
				return
			}

			let faceRequest = VNDetectFaceRectanglesRequest()
			let humanRequest = VNDetectHumanRectanglesRequest()
			let animalRequest = VNRecognizeAnimalsRequest()

			let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])

			do {
				try handler.perform([faceRequest, humanRequest, animalRequest])
			} catch {
				completion(.default)
				return
			}

			let faces = faceRequest.results ?? []
			let humans = humanRequest.results ?? []
			let animals = animalRequest.results ?? []

			let hasHuman = !faces.isEmpty || !humans.isEmpty
			let hasAnimal = !animals.isEmpty
			let humanCount = faces.count

			// 确定主要主体矩形
			let mainRect = self.computeMainSubjectRect(faces: faces, humans: humans, animals: animals)

			// 食物检测：基于规则判断（结合后续光环境分析）
			let hasFood = false  // 食物检测需要专门的模型，Vision 框架不直接支持

			// 确定主体类型
			let subjectType = self.determineSubjectType(hasHuman: hasHuman, hasAnimal: hasAnimal, hasFood: hasFood, humanCount: humanCount)

			completion(SubjectDetection(
				hasHuman: hasHuman,
				hasAnimal: hasAnimal,
				hasFood: hasFood,
				humanCount: humanCount,
				mainSubjectRect: mainRect,
				subjectType: subjectType
			))
		}
	}

	/// 计算主要主体所在的归一化矩形
	private func computeMainSubjectRect(
		faces: [VNFaceObservation],
		humans: [VNHumanObservation],
		animals: [VNRecognizedObjectObservation]
	) -> CGRect {
		var allRects: [CGRect] = []

		// 人脸检测结果
		allRects.append(contentsOf: faces.map { $0.boundingBox })
		// 人体检测结果
		allRects.append(contentsOf: humans.map { $0.boundingBox })
		// 动物检测结果
		allRects.append(contentsOf: animals.map { $0.boundingBox })

		// 如果没有任何检测结果，返回画面中心区域
		guard !allRects.isEmpty else {
			return CGRect(x: 0.25, y: 0.25, width: 0.5, height: 0.5)
		}

		// 优先使用人脸矩形
		if !faces.isEmpty {
			// 计算所有人脸的包围盒
			var minX: CGFloat = 1.0, minY: CGFloat = 1.0
			var maxX: CGFloat = 0.0, maxY: CGFloat = 0.0
			for face in faces {
				let rect = face.boundingBox
				minX = min(minX, rect.minX)
				minY = min(minY, rect.minY)
				maxX = max(maxX, rect.maxX)
				maxY = max(maxY, rect.maxY)
			}
			// 扩展 20% 以包含身体
			let expandX = (maxX - minX) * 0.2
			let expandY = (maxY - minY) * 0.2
			return CGRect(
				x: max(0, minX - expandX),
				y: max(0, minY - expandY * 2),  // 向下扩展更多，包含身体
				width: min(1, maxX - minX + expandX * 2),
				height: min(1, maxY - minY + expandY * 3)
			)
		}

		// 使用人体检测结果
		if !humans.isEmpty {
			var minX: CGFloat = 1.0, minY: CGFloat = 1.0
			var maxX: CGFloat = 0.0, maxY: CGFloat = 0.0
			for human in humans {
				let rect = human.boundingBox
				minX = min(minX, rect.minX)
				minY = min(minY, rect.minY)
				maxX = max(maxX, rect.maxX)
				maxY = max(maxY, rect.maxY)
			}
			return CGRect(x: minX, y: minY, width: maxX - minX, height: maxY - minY)
		}

		// 使用动物检测结果
		if !animals.isEmpty {
			let rect = animals[0].boundingBox
			return rect
		}

		return CGRect(x: 0.25, y: 0.25, width: 0.5, height: 0.5)
	}

	/// 确定主体类型描述
	private func determineSubjectType(hasHuman: Bool, hasAnimal: Bool, hasFood: Bool, humanCount: Int) -> String {
		if hasHuman {
			switch humanCount {
			case 0: return "人物"
			case 1: return "单人"
			case 2: return "双人"
			default: return "多人(\(humanCount))"
			}
		}
		if hasAnimal {
			return "动物"
		}
		if hasFood {
			return "食物"
		}
		return "未知"
	}
}

#endif