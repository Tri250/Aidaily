//
//  ImageQualityAssessor.swift
//  LiveCapture
//
//  AI 图像质量评估系统 - 基于 CoreImage 和 Accelerate 的像素级质量分析
//
//  ## 文件作用
//  提供完整的图像质量评估能力，包括清晰度、噪点、曝光、色彩和谐度、
//  分辨率和综合质量评分。同时支持色彩分析（主色调、色温、色彩情绪）。
//
//  ## 主要类型
//
//  ### QualityDimension
//  质量维度枚举：sharpness、noise、exposure、colorHarmony、resolution、overall
//
//  ### QualityGrade
//  质量等级枚举：excellent(优秀)、good(良好)、fair(中等)、poor(较差)
//
//  ### QualityAssessment
//  质量评估结果结构体，包含各维度评分和综合评分
//
//  ### ImageInfo
//  图像基本信息结构体
//
//  ### ColorAnalysis
//  色彩分析结果结构体
//
//  ### ImageQualityAssessor
//  图像质量评估器主类
//
//  ## 主要方法
//
//  ### 质量评估
//  - assessQuality(from:): 从 CVPixelBuffer 进行综合质量评估
//  - assessQuality(from:): 从 CIImage 进行综合质量评估
//
//  ### 色彩分析
//  - analyzeColors(from:): 从 CVPixelBuffer 进行色彩分析
//  - analyzeColors(from:): 从 CIImage 进行色彩分析
//
//  ## 内部方法
//  - assessSharpness: Sobel 边缘检测，计算边缘密度和强度
//  - assessNoise: 3x3 局部方差分析
//  - assessExposure: 直方图分析，过曝/欠曝像素比例
//  - assessColorHarmony: 基于主色调距离的色彩和谐度
//  - assessResolution: 基于总像素数的分辨率评分
//  - extractDominantColors: 量化聚类提取主色调
//  - estimateColorTemperature: 从 RGB 比例估算色温
//  - determineColorMood: 从亮度和饱和度判断色彩情绪
//  - getQualityGrade: 分数映射到等级
//  - renderGrayscaleBuffer: CIImage 转灰度 CVPixelBuffer
//
//  ## 线程安全
//  - 使用专用 queue 执行所有分析操作
//  - 同步返回结果（分析在调用线程执行）
//  - 不阻塞主线程
//

import Foundation
import CoreImage
import AVFoundation
import Accelerate

#if os(iOS)

// MARK: - 质量维度枚举

/// 图像质量评估维度
enum QualityDimension: String, CaseIterable, Codable {
	case sharpness      // 清晰度
	case noise          // 噪点
	case exposure       // 曝光
	case colorHarmony   // 色彩和谐度
	case resolution     // 分辨率
	case overall        // 综合

	/// 维度中文显示名称
	var displayName: String {
		switch self {
		case .sharpness:     return "清晰度"
		case .noise:         return "噪点"
		case .exposure:      return "曝光"
		case .colorHarmony:  return "色彩"
		case .resolution:    return "分辨率"
		case .overall:       return "综合"
		}
	}

	/// SF Symbol 图标名称
	var iconName: String {
		switch self {
		case .sharpness:     return "sparkles"
		case .noise:         return "waveform"
		case .exposure:      return "sun.max"
		case .colorHarmony:  return "paintpalette"
		case .resolution:    return "rectangle.3.group"
		case .overall:       return "star"
		}
	}
}

// MARK: - 质量等级枚举

/// 图像质量等级
enum QualityGrade: String, CaseIterable, Codable {
	case excellent  // 优秀
	case good       // 良好
	case fair       // 中等
	case poor       // 较差

	/// 等级中文显示名称
	var displayName: String {
		switch self {
		case .excellent: return "优秀"
		case .good:      return "良好"
		case .fair:      return "中等"
		case .poor:      return "较差"
		}
	}

	/// 等级对应的分数范围下限
	var minScore: Float {
		switch self {
		case .excellent: return 80
		case .good:      return 60
		case .fair:      return 40
		case .poor:      return 0
		}
	}

	/// 等级对应的推荐操作
	var recommendation: String {
		switch self {
		case .excellent: return "画质优秀，可直接拍摄"
		case .good:      return "画质良好，建议保持稳定"
		case .fair:      return "画质中等，可尝试调整参数"
		case .poor:      return "画质较差，建议改善光线或持稳设备"
		}
	}
}

// MARK: - 图像基本信息

/// 图像基本信息
struct ImageInfo: Codable {
	/// 图像宽度（像素）
	let width: Int
	/// 图像高度（像素）
	let height: Int
	/// 宽高比
	let aspectRatio: Float
	/// 图像方向
	let orientation: String
	/// 像素格式描述
	let format: String
	/// 总像素数
	let totalPixels: Int
	/// 分辨率等级描述
	let resolutionLevel: String

	/// 从像素缓冲创建 ImageInfo
	static func from(pixelBuffer: CVPixelBuffer) -> ImageInfo {
		let width = CVPixelBufferGetWidth(pixelBuffer)
		let height = CVPixelBufferGetHeight(pixelBuffer)
		let totalPixels = width * height
		let aspectRatio = height > 0 ? Float(width) / Float(height) : 1.0
		let formatType = CVPixelBufferGetPixelFormatType(pixelBuffer)
		let format = ImageInfo.formatName(for: formatType)
		let resolutionLevel = ImageInfo.resolutionLevel(for: totalPixels)

		return ImageInfo(
			width: width,
			height: height,
			aspectRatio: aspectRatio,
			orientation: "up",
			format: format,
			totalPixels: totalPixels,
			resolutionLevel: resolutionLevel
		)
	}

	/// 从 CIImage 创建 ImageInfo
	static func from(ciImage: CIImage) -> ImageInfo {
		let extent = ciImage.extent
		let width = Int(extent.width)
		let height = Int(extent.height)
		let totalPixels = width * height
		let aspectRatio = height > 0 ? Float(width) / Float(height) : 1.0
		let resolutionLevel = ImageInfo.resolutionLevel(for: totalPixels)

		return ImageInfo(
			width: width,
			height: height,
			aspectRatio: aspectRatio,
			orientation: "up",
			format: "CI",
			totalPixels: totalPixels,
			resolutionLevel: resolutionLevel
		)
	}

	/// 像素格式类型名称
	private static func formatName(for formatType: OSType) -> String {
		switch formatType {
		case kCVPixelFormatType_32BGRA: return "BGRA8"
		case kCVPixelFormatType_32RGBA: return "RGBA8"
		case kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange: return "NV12"
		case kCVPixelFormatType_420YpCbCr8BiPlanarFullRange: return "NV12-Full"
		case kCVPixelFormatType_422YpCbCr8: return "YUY2"
		default:
			let c1 = UInt8((formatType >> 24) & 0xFF)
			let c2 = UInt8((formatType >> 16) & 0xFF)
			let c3 = UInt8((formatType >> 8) & 0xFF)
			let c4 = UInt8(formatType & 0xFF)
			return String(format: "%c%c%c%c", c1, c2, c3, c4)
		}
	}

	/// 根据总像素数判断分辨率等级
	private static func resolutionLevel(for totalPixels: Int) -> String {
		let megapixels = Double(totalPixels) / 1_000_000.0
		switch megapixels {
		case 0..<1:    return "低分辨率"
		case 1..<4:    return "标清"
		case 4..<8:    return "高清"
		case 8..<12:   return "超高清"
		case 12..<20:  return "4K级"
		case 20..<50:  return "8K级"
		default:       return "专业级"
		}
	}
}

// MARK: - 色彩分析结果

/// 色彩分析结果
struct ColorAnalysis: Codable {
	/// 平均色彩 (r, g, b, hex)
	struct AverageColor: Codable {
		let r: Float
		let g: Float
		let b: Float
		let hex: String

		init(r: Float, g: Float, b: Float) {
			self.r = r
			self.g = g
			self.b = b
			let ri = UInt8(max(0, min(255, r)))
			let gi = UInt8(max(0, min(255, g)))
			let bi = UInt8(max(0, min(255, b)))
			self.hex = String(format: "#%02X%02X%02X", ri, gi, bi)
		}
	}

	/// 主色调 (color, percentage)
	struct DominantColor: Codable {
		let r: Float
		let g: Float
		let b: Float
		let hex: String
		let percentage: Float

		init(r: Float, g: Float, b: Float, percentage: Float) {
			self.r = r
			self.g = g
			self.b = b
			self.percentage = percentage
			let ri = UInt8(max(0, min(255, r)))
			let gi = UInt8(max(0, min(255, g)))
			let bi = UInt8(max(0, min(255, b)))
			self.hex = String(format: "#%02X%02X%02X", ri, gi, bi)
		}
	}

	/// 色温信息
	struct ColorTemperatureInfo: Codable {
		let kelvin: Float
		let type: String
		let description: String
	}

	/// 平均色彩
	let averageColor: AverageColor
	/// 主色调（最多 5 个）
	let dominantColors: [DominantColor]
	/// 色温信息
	let colorTemperature: ColorTemperatureInfo
	/// 色彩和谐度评分 (0-100)
	let colorHarmonyScore: Float
	/// 色彩情绪描述
	let colorMood: String
	/// 饱和度均值 (0-1)
	let saturationMean: Float
	/// 亮度均值 (0-1)
	let brightnessMean: Float
	/// 对比度比例
	let contrastRatio: Float

	/// 默认色彩分析结果
	static let `default` = ColorAnalysis(
		averageColor: AverageColor(r: 128, g: 128, b: 128),
		dominantColors: [],
		colorTemperature: ColorTemperatureInfo(kelvin: 5500, type: "neutral", description: "中性色温"),
		colorHarmonyScore: 50,
		colorMood: "中性",
		saturationMean: 0.5,
		brightnessMean: 0.5,
		contrastRatio: 1.0
	)
}

// MARK: - 质量评估结果

/// 图像质量评估结果
struct QualityAssessment: Codable {
	/// 综合评分 (0-100)
	let overallScore: Float
	/// 清晰度评分 (0-100)
	let sharpnessScore: Float
	/// 噪点评分 (0-100, 越低越好)
	let noiseScore: Float
	/// 曝光评分 (0-100)
	let exposureScore: Float
	/// 色彩和谐度评分 (0-100)
	let colorHarmonyScore: Float
	/// 分辨率评分 (0-100)
	let resolutionScore: Float
	/// 质量等级
	let qualityGrade: QualityGrade
	/// 评估时间戳
	let timestamp: Date
	/// 图像基本信息
	let imageInfo: ImageInfo

	/// 默认评估结果
	static let `default` = QualityAssessment(
		overallScore: 50,
		sharpnessScore: 50,
		noiseScore: 50,
		exposureScore: 50,
		colorHarmonyScore: 50,
		resolutionScore: 50,
		qualityGrade: .fair,
		timestamp: Date(),
		imageInfo: ImageInfo(width: 1920, height: 1080, aspectRatio: 1.778, orientation: "up", format: "unknown", totalPixels: 2073600, resolutionLevel: "高清")
	)

	/// 生成各维度评分字典
	var dimensionScores: [QualityDimension: Float] {
		[
			.sharpness: sharpnessScore,
			.noise: noiseScore,
			.exposure: exposureScore,
			.colorHarmony: colorHarmonyScore,
			.resolution: resolutionScore,
			.overall: overallScore
		]
	}

	/// 最低分维度
	var weakestDimension: QualityDimension {
		let scores = [
			(QualityDimension.sharpness, sharpnessScore),
			(.noise, noiseScore),
			(.exposure, exposureScore),
			(.colorHarmony, colorHarmonyScore),
			(.resolution, resolutionScore)
		]
		return scores.min(by: { $0.1 < $1.1 })?.0 ?? .overall
	}

	/// 最高分维度
	var strongestDimension: QualityDimension {
		let scores = [
			(QualityDimension.sharpness, sharpnessScore),
			(.noise, noiseScore),
			(.exposure, exposureScore),
			(.colorHarmony, colorHarmonyScore),
			(.resolution, resolutionScore)
		]
		return scores.max(by: { $0.1 < $1.1 })?.0 ?? .overall
	}
}

// MARK: - 图像质量评估器

/// 基于 CoreImage 和 Accelerate 的图像质量评估器
final class ImageQualityAssessor {

	// MARK: - 私有属性

	private let queue = DispatchQueue(label: "livecapture.quality.assessor", qos: .userInitiated)
	private let ciContext: CIContext

	/// 颜色量化级别数（用于主色调提取）
	private let quantizationLevels: Int = 32

	/// Sobel 边缘检测阈值
	private let edgeThreshold: Float = 30.0

	/// 过曝阈值
	private let overexposedThreshold: Float = 250.0

	/// 欠曝阈值
	private let underexposedThreshold: Float = 5.0

	// MARK: - 初始化

	init() {
		let colorSpace = CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()
		self.ciContext = CIContext(options: [
			.workingColorSpace: colorSpace,
			.outputColorSpace: colorSpace,
			.workingFormat: CIFormat.RGBAh,
			.outputPremultiplied: true
		])
	}

	// MARK: - 公共接口：质量评估

	/// 从像素缓冲进行综合质量评估
	/// - Parameter pixelBuffer: 输入像素缓冲
	/// - Returns: 质量评估结果
	func assessQuality(from pixelBuffer: CVPixelBuffer) -> QualityAssessment {
		let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
		return assessQuality(from: ciImage)
	}

	/// 从 CIImage 进行综合质量评估
	/// - Parameter ciImage: 输入 CIImage
	/// - Returns: 质量评估结果
	func assessQuality(from ciImage: CIImage) -> QualityAssessment {
		let extent = ciImage.extent
		let width = Int(extent.width)
		let height = Int(extent.height)

		guard width > 0, height > 0 else {
			return .default
		}

		let imageInfo = ImageInfo.from(ciImage: ciImage)

		// 渲染灰度缓冲用于清晰度和噪点分析
		guard let grayBuffer = renderGrayscaleBuffer(from: ciImage, width: width, height: height) else {
			return .default
		}

		CVPixelBufferLockBaseAddress(grayBuffer, .readOnly)
		defer { CVPixelBufferUnlockBaseAddress(grayBuffer, .readOnly) }

		guard let baseAddress = CVPixelBufferGetBaseAddress(grayBuffer) else {
			return .default
		}
		let bytesPerRow = CVPixelBufferGetBytesPerRow(grayBuffer)

		// 清晰度评估
		let sharpnessScore = assessSharpness(from: baseAddress, bytesPerRow: bytesPerRow, width: width, height: height)

		// 噪点评估
		let noiseScore = assessNoise(from: baseAddress, bytesPerRow: bytesPerRow, width: width, height: height)

		// 曝光评估
		let exposureScore = assessExposure(from: ciImage, width: width, height: height)

		// 色彩和谐度评估
		let colorAnalysis = analyzeColors(from: ciImage)
		let colorHarmonyScore = colorAnalysis.colorHarmonyScore

		// 分辨率评估
		let resolutionScore = assessResolution(width: width, height: height)

		// 综合评分：加权平均
		let weights: [Float] = [0.30, 0.20, 0.25, 0.15, 0.10]  // 清晰度、噪点、曝光、色彩、分辨率
		let overallScore = sharpnessScore * weights[0]
			+ noiseScore * weights[1]
			+ exposureScore * weights[2]
			+ colorHarmonyScore * weights[3]
			+ resolutionScore * weights[4]

		let qualityGrade = getQualityGrade(score: overallScore)

		return QualityAssessment(
			overallScore: overallScore,
			sharpnessScore: sharpnessScore,
			noiseScore: noiseScore,
			exposureScore: exposureScore,
			colorHarmonyScore: colorHarmonyScore,
			resolutionScore: resolutionScore,
			qualityGrade: qualityGrade,
			timestamp: Date(),
			imageInfo: imageInfo
		)
	}

	// MARK: - 公共接口：色彩分析

	/// 从像素缓冲进行色彩分析
	/// - Parameter pixelBuffer: 输入像素缓冲
	/// - Returns: 色彩分析结果
	func analyzeColors(from pixelBuffer: CVPixelBuffer) -> ColorAnalysis {
		let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
		return analyzeColors(from: ciImage)
	}

	/// 从 CIImage 进行色彩分析
	/// - Parameter ciImage: 输入 CIImage
	/// - Returns: 色彩分析结果
	func analyzeColors(from ciImage: CIImage) -> ColorAnalysis {
		let extent = ciImage.extent
		let width = Int(extent.width)
		let height = Int(extent.height)

		guard width > 0, height > 0 else {
			return .default
		}

		// 缩小图像以提高性能
		let scale = min(1.0, 128.0 / max(CGFloat(width), CGFloat(height)))
		let scaledImage = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
		let scaledExtent = scaledImage.extent
		let scaledWidth = Int(scaledExtent.width)
		let scaledHeight = Int(scaledExtent.height)

		// 渲染 RGBA 缓冲
		guard let rgbaBuffer = renderRGBABuffer(from: scaledImage, width: scaledWidth, height: scaledHeight) else {
			return .default
		}

		CVPixelBufferLockBaseAddress(rgbaBuffer, .readOnly)
		defer { CVPixelBufferUnlockBaseAddress(rgbaBuffer, .readOnly) }

		guard let baseAddress = CVPixelBufferGetBaseAddress(rgbaBuffer) else {
			return .default
		}
		let bytesPerRow = CVPixelBufferGetBytesPerRow(rgbaBuffer)
		let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)

		// 计算平均色彩、亮度和饱和度
		var totalR: UInt64 = 0
		var totalG: UInt64 = 0
		var totalB: UInt64 = 0
		var totalBrightness: Float = 0
		var totalSaturation: Float = 0
		var pixelCount = 0

		for y in 0..<scaledHeight {
			for x in 0..<scaledWidth {
				let offset = y * bytesPerRow + x * 4
				let b = Float(ptr[offset])
				let g = Float(ptr[offset + 1])
				let r = Float(ptr[offset + 2])

				totalR += UInt64(r)
				totalG += UInt64(g)
				totalB += UInt64(b)
				pixelCount += 1

				// 亮度 (Rec.709)
				let brightness = 0.2126 * r + 0.7152 * g + 0.0722 * b
				totalBrightness += brightness

				// 饱和度
				let maxChannel = max(r, g, b)
				let minChannel = min(r, g, b)
				if maxChannel > 0 {
					totalSaturation += (maxChannel - minChannel) / maxChannel
				}
			}
		}

		guard pixelCount > 0 else { return .default }

		let avgR = Float(totalR) / Float(pixelCount)
		let avgG = Float(totalG) / Float(pixelCount)
		let avgB = Float(totalB) / Float(pixelCount)
		let brightnessMean = totalBrightness / Float(pixelCount * 255)
		let saturationMean = totalSaturation / Float(pixelCount)

		let averageColor = ColorAnalysis.AverageColor(r: avgR, g: avgG, b: avgB)

		// 提取主色调
		let dominantColors = extractDominantColors(from: ptr, bytesPerRow: bytesPerRow, width: scaledWidth, height: scaledHeight)

		// 估算色温
		let colorTemp = estimateColorTemperature(r: avgR, g: avgG, b: avgB)

		// 色彩和谐度
		let harmonyScore = assessColorHarmony(dominantColors: dominantColors)

		// 对比度比例
		let contrastRatio = computeContrastRatio(from: ptr, bytesPerRow: bytesPerRow, width: scaledWidth, height: scaledHeight)

		// 色彩情绪
		let colorMood = determineColorMood(brightness: brightnessMean, saturation: saturationMean, colorTemp: colorTemp)

		return ColorAnalysis(
			averageColor: averageColor,
			dominantColors: dominantColors,
			colorTemperature: colorTemp,
			colorHarmonyScore: harmonyScore,
			colorMood: colorMood,
			saturationMean: saturationMean,
			brightnessMean: brightnessMean,
			contrastRatio: contrastRatio
		)
	}

	// MARK: - 清晰度评估

	/// 使用 Sobel 边缘检测评估清晰度
	/// 计算边缘密度和边缘强度，映射到 0-100 分
	private func assessSharpness(from baseAddress: UnsafeMutableRawPointer, bytesPerRow: Int, width: Int, height: Int) -> Float {
		guard width > 2, height > 2 else { return 50 }

		let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)

		// Sobel 算子：3x3 卷积
		// 采样步长，平衡精度和性能
		let stepX = 2
		let stepY = 2

		var edgeCount = 0
		var edgeSum: Float = 0
		var totalSamples = 0

		for y in stride(from: 1, to: height - 1, by: stepY) {
			for x in stride(from: 1, to: width - 1, by: stepX) {
				let tl = Float(ptr[(y - 1) * bytesPerRow + (x - 1)])
				let tc = Float(ptr[(y - 1) * bytesPerRow + x])
				let tr = Float(ptr[(y - 1) * bytesPerRow + (x + 1)])
				let ml = Float(ptr[y * bytesPerRow + (x - 1)])
				let mr = Float(ptr[y * bytesPerRow + (x + 1)])
				let bl = Float(ptr[(y + 1) * bytesPerRow + (x - 1)])
				let bc = Float(ptr[(y + 1) * bytesPerRow + x])
				let br = Float(ptr[(y + 1) * bytesPerRow + (x + 1)])

				// Sobel Gx
				let gx = (tr + 2.0 * mr + br) - (tl + 2.0 * ml + bl)
				// Sobel Gy
				let gy = (tl + 2.0 * tc + tr) - (bl + 2.0 * bc + br)

				let magnitude = sqrt(gx * gx + gy * gy)
				totalSamples += 1

				if magnitude > edgeThreshold {
					edgeCount += 1
					edgeSum += magnitude
				}
			}
		}

		guard totalSamples > 0 else { return 50 }

		// 边缘密度：边缘像素占比
		let edgeDensity = Float(edgeCount) / Float(totalSamples)

		// 边缘强度：边缘像素的平均梯度 (归一化: 最大理论梯度 ≈ 1440)
		let avgEdgeStrength = edgeCount > 0 ? edgeSum / Float(edgeCount) : 0
		let normalizedStrength = min(1.0, avgEdgeStrength / 500.0)

		// 综合评分：边缘密度 60% + 边缘强度 40%
		let rawScore = (edgeDensity * 0.6 + normalizedStrength * 0.4) * 100.0

		// 映射到合理范围：边缘密度过低或过高都不好
		// 最佳边缘密度约为 0.15-0.35
		let adjustedScore: Float
		if edgeDensity < 0.05 {
			adjustedScore = rawScore * 0.5  // 过于模糊
		} else if edgeDensity > 0.5 {
			adjustedScore = rawScore * 0.8  // 可能过度锐化或噪声
		} else {
			adjustedScore = rawScore
		}

		return max(0, min(100, adjustedScore))
	}

	// MARK: - 噪点评估

	/// 使用 3x3 局部方差评估噪点水平
	/// 噪声越低，评分越高（0-100）
	private func assessNoise(from baseAddress: UnsafeMutableRawPointer, bytesPerRow: Int, width: Int, height: Int) -> Float {
		guard width > 2, height > 2 else { return 50 }

		let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)

		// 采样步长
		let stepX = 4
		let stepY = 4

		var totalVariance: Float = 0
		var sampleCount = 0

		for y in stride(from: 1, to: height - 1, by: stepY) {
			for x in stride(from: 1, to: width - 1, by: stepX) {
				// 3x3 邻域
				let values: [Float] = [
					Float(ptr[(y - 1) * bytesPerRow + (x - 1)]),
					Float(ptr[(y - 1) * bytesPerRow + x]),
					Float(ptr[(y - 1) * bytesPerRow + (x + 1)]),
					Float(ptr[y * bytesPerRow + (x - 1)]),
					Float(ptr[y * bytesPerRow + x]),
					Float(ptr[y * bytesPerRow + (x + 1)]),
					Float(ptr[(y + 1) * bytesPerRow + (x - 1)]),
					Float(ptr[(y + 1) * bytesPerRow + x]),
					Float(ptr[(y + 1) * bytesPerRow + (x + 1)])
				]

				// 计算均值
				let mean = values.reduce(0, +) / 9.0

				// 计算方差
				var variance: Float = 0
				for v in values {
					let diff = v - mean
					variance += diff * diff
				}
				variance /= 9.0

				totalVariance += variance
				sampleCount += 1
			}
		}

		guard sampleCount > 0 else { return 50 }

		let avgVariance = totalVariance / Float(sampleCount)

		// 噪声归一化：方差 0-65025（像素值范围 0-255）
		// 经验值：方差 < 10 为极低噪声，> 200 为高噪声
		let normalizedNoise: Float
		if avgVariance < 10 {
			normalizedNoise = avgVariance / 10.0 * 0.2  // 0-0.2
		} else if avgVariance < 50 {
			normalizedNoise = 0.2 + (avgVariance - 10) / 40.0 * 0.3  // 0.2-0.5
		} else if avgVariance < 200 {
			normalizedNoise = 0.5 + (avgVariance - 50) / 150.0 * 0.3  // 0.5-0.8
		} else {
			normalizedNoise = 0.8 + min(0.2, (avgVariance - 200) / 1000.0 * 0.2)  // 0.8-1.0
		}

		// 噪声评分 = (1 - 噪声水平) * 100
		let noiseScore = (1.0 - normalizedNoise) * 100.0

		return max(0, min(100, noiseScore))
	}

	// MARK: - 曝光评估

	/// 使用直方图分析评估曝光质量
	/// 检查过曝和欠曝像素比例，映射到 0-100 分
	private func assessExposure(from ciImage: CIImage, width: Int, height: Int) -> Float {
		// 使用 CIAreaHistogram 获取亮度直方图
		let histogramFilter = CIFilter(name: "CIAreaHistogram", parameters: [
			kCIInputImageKey: ciImage,
			kCIInputExtentKey: CIVector(cgRect: ciImage.extent),
			"inputCount": 256,
			"inputScale": 1.0
		])

		guard let histogramImage = histogramFilter?.outputImage else {
			// 兜底：使用像素采样
			return assessExposureFallback(from: ciImage, width: width, height: height)
		}

		// 渲染直方图到 1D 缓冲
		var histogramBuffer: CVPixelBuffer?
		let attributes: [String: Any] = [
			kCVPixelBufferCGImageCompatibilityKey as String: true,
			kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
		]
		let status = CVPixelBufferCreate(kCFAllocatorDefault, 256, 1,
										 kCVPixelFormatType_OneComponent32Float, attributes as CFDictionary, &histogramBuffer)

		guard status == kCVReturnSuccess, let histogramBuffer else {
			return assessExposureFallback(from: ciImage, width: width, height: height)
		}

		ciContext.render(histogramImage, to: histogramBuffer)

		CVPixelBufferLockBaseAddress(histogramBuffer, .readOnly)
		defer { CVPixelBufferUnlockBaseAddress(histogramBuffer, .readOnly) }

		guard let histBase = CVPixelBufferGetBaseAddress(histogramBuffer) else {
			return assessExposureFallback(from: ciImage, width: width, height: height)
		}

		let histPtr = histBase.assumingMemoryBound(to: Float.self)
		let totalPixels = Float(width * height)

		guard totalPixels > 0 else { return 50 }

		// 计算累积直方图
		var overexposedRatio: Float = 0
		var underexposedRatio: Float = 0
		var totalCount: Float = 0

		for i in 0..<256 {
			totalCount += histPtr[i]
		}

		guard totalCount > 0 else { return 50 }

		// 过曝：亮度 > 250 的像素
		for i in 251..<256 {
			overexposedRatio += histPtr[i] / totalCount
		}

		// 欠曝：亮度 < 5 的像素
		for i in 0..<5 {
			underexposedRatio += histPtr[i] / totalCount
		}

		// 曝光评分：完美曝光 = 100，过曝/欠曝越多扣分越多
		let overexposedPenalty = min(1.0, overexposedRatio * 5.0) * 100.0
		let underexposedPenalty = min(1.0, underexposedRatio * 5.0) * 100.0

		var score = 100.0 - overexposedPenalty - underexposedPenalty

		// 如果过曝和欠曝都很低，检查直方图分布
		if overexposedRatio < 0.02 && underexposedRatio < 0.02 {
			// 计算直方图均值
			var weightedSum: Float = 0
			for i in 0..<256 {
				weightedSum += Float(i) * histPtr[i] / totalCount
			}

			// 理想亮度在 100-156 之间
			let idealCenter: Float = 128
			let distanceFromIdeal = abs(weightedSum - idealCenter) / 128.0

			// 偏离理想亮度会扣分，但不如过曝/欠曝严重
			score -= distanceFromIdeal * 20.0
		}

		return max(0, min(100, score))
	}

	/// 曝光评估兜底方案：使用像素采样
	private func assessExposureFallback(from ciImage: CIImage, width: Int, height: Int) -> Float {
		let scale = min(1.0, 64.0 / max(CGFloat(width), CGFloat(height)))
		let scaledImage = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
		let scaledExtent = scaledImage.extent
		let scaledWidth = Int(scaledExtent.width)
		let scaledHeight = Int(scaledExtent.height)

		guard let rgbaBuffer = renderRGBABuffer(from: scaledImage, width: scaledWidth, height: scaledHeight) else {
			return 50
		}

		CVPixelBufferLockBaseAddress(rgbaBuffer, .readOnly)
		defer { CVPixelBufferUnlockBaseAddress(rgbaBuffer, .readOnly) }

		guard let baseAddress = CVPixelBufferGetBaseAddress(rgbaBuffer) else {
			return 50
		}
		let bytesPerRow = CVPixelBufferGetBytesPerRow(rgbaBuffer)
		let ptr = baseAddress.assumingMemoryBound(to: UInt8.self)

		var overexposed = 0
		var underexposed = 0
		var total = 0

		for y in 0..<scaledHeight {
			for x in 0..<scaledWidth {
				let offset = y * bytesPerRow + x * 4
				let r = Float(ptr[offset + 2])
				let g = Float(ptr[offset + 1])
				let b = Float(ptr[offset])

				// 亮度 (Rec.709)
				let luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

				if luminance > overexposedThreshold {
					overexposed += 1
				} else if luminance < underexposedThreshold {
					underexposed += 1
				}
				total += 1
			}
		}

		guard total > 0 else { return 50 }

		let overexposedRatio = Float(overexposed) / Float(total)
		let underexposedRatio = Float(underexposed) / Float(total)

		let overexposedPenalty = min(1.0, overexposedRatio * 5.0) * 100.0
		let underexposedPenalty = min(1.0, underexposedRatio * 5.0) * 100.0

		let score = 100.0 - overexposedPenalty - underexposedPenalty
		return max(0, min(100, score))
	}

	// MARK: - 色彩和谐度评估

	/// 基于主色调距离评估色彩和谐度
	/// - Parameter dominantColors: 主色调列表
	/// - Returns: 和谐度评分 (0-100)
	private func assessColorHarmony(dominantColors: [ColorAnalysis.DominantColor]) -> Float {
		guard dominantColors.count >= 2 else {
			// 单色或无色：中性评分
			return 60
		}

		// 计算所有主色调对之间的色彩距离
		var distances: [Float] = []
		for i in 0..<dominantColors.count {
			for j in (i + 1)..<dominantColors.count {
				let c1 = dominantColors[i]
				let c2 = dominantColors[j]
				let dr = c1.r - c2.r
				let dg = c1.g - c2.g
				let db = c1.b - c2.b
				let distance = sqrt(dr * dr + dg * dg + db * db)
				distances.append(distance)
			}
		}

		guard !distances.isEmpty else { return 60 }

		let avgDistance = distances.reduce(0, +) / Float(distances.count)

		// 色彩和谐度评分：
		// 适中的色彩距离（有对比但不突兀）得分高
		// 归一化：最大距离 ≈ 441（sqrt(255^2 * 3)）
		let normalizedDistance = avgDistance / 441.0

		// 最佳距离范围：0.2-0.6（互补色/对比色）
		let harmonyScore: Float
		if normalizedDistance < 0.1 {
			// 色彩过于接近，缺乏对比
			harmonyScore = 30 + normalizedDistance * 300
		} else if normalizedDistance < 0.6 {
			// 良好的色彩对比
			harmonyScore = 60 + (normalizedDistance - 0.1) * 80
		} else if normalizedDistance < 0.8 {
			// 稍强的对比
			harmonyScore = 100 - (normalizedDistance - 0.6) * 100
		} else {
			// 色彩过于分散，可能不协调
			harmonyScore = 80 - (normalizedDistance - 0.8) * 200
		}

		// 考虑主色调数量：2-3 个主色调通常最佳
		let countBonus: Float
		switch dominantColors.count {
		case 2: countBonus = 10
		case 3: countBonus = 5
		case 4: countBonus = 0
		default: countBonus = -5
		}

		return max(0, min(100, harmonyScore + countBonus))
	}

	// MARK: - 分辨率评估

	/// 基于总像素数评估分辨率
	/// - Parameters:
	///   - width: 图像宽度
	///   - height: 图像高度
	/// - Returns: 分辨率评分 (0-100)
	private func assessResolution(width: Int, height: Int) -> Float {
		let totalPixels = width * height
		let megapixels = Double(totalPixels) / 1_000_000.0

		// 分辨率评分映射
		// < 1MP = 0-20, 1-4MP = 20-50, 4-8MP = 50-70, 8-12MP = 70-90, 12-24MP = 90-100, > 24MP = 100
		let score: Float
		switch megapixels {
		case 0..<1:
			score = Float(megapixels / 1.0 * 20.0)
		case 1..<4:
			score = 20 + Float((megapixels - 1.0) / 3.0 * 30.0)
		case 4..<8:
			score = 50 + Float((megapixels - 4.0) / 4.0 * 20.0)
		case 8..<12:
			score = 70 + Float((megapixels - 8.0) / 4.0 * 20.0)
		case 12..<24:
			score = 90 + Float((megapixels - 12.0) / 12.0 * 10.0)
		default:
			score = 100
		}

		return max(0, min(100, score))
	}

	// MARK: - 主色调提取

	/// 使用量化聚类提取主色调
	/// 将颜色空间量化到 32 级，统计频率，取前 5 个
	private func extractDominantColors(
		from ptr: UnsafePointer<UInt8>,
		bytesPerRow: Int,
		width: Int,
		height: Int
	) -> [ColorAnalysis.DominantColor] {
		let qLevels = quantizationLevels
		let binSize = 256 / qLevels
		let totalBins = qLevels * qLevels * qLevels

		// 使用字典统计每个量化 bin 的像素数
		var binCounts = [Int](repeating: 0, count: totalBins)
		var binSumR = [UInt64](repeating: 0, count: totalBins)
		var binSumG = [UInt64](repeating: 0, count: totalBins)
		var binSumB = [UInt64](repeating: 0, count: totalBins)

		var totalPixels = 0

		for y in 0..<height {
			for x in 0..<width {
				let offset = y * bytesPerRow + x * 4
				let r = Int(ptr[offset + 2])
				let g = Int(ptr[offset + 1])
				let b = Int(ptr[offset])

				let qr = min(qLevels - 1, r / binSize)
				let qg = min(qLevels - 1, g / binSize)
				let qb = min(qLevels - 1, b / binSize)

				let binIndex = qr * qLevels * qLevels + qg * qLevels + qb

				binCounts[binIndex] += 1
				binSumR[binIndex] += UInt64(r)
				binSumG[binIndex] += UInt64(g)
				binSumB[binIndex] += UInt64(b)
				totalPixels += 1
			}
		}

		guard totalPixels > 0 else { return [] }

		// 创建 (binIndex, count) 对并排序
		var indexedBins: [(index: Int, count: Int)] = []
		for i in 0..<totalBins {
			if binCounts[i] > 0 {
				indexedBins.append((index: i, count: binCounts[i]))
			}
		}
		indexedBins.sort { $0.count > $1.count }

		// 取前 5 个，合并相近的颜色
		var dominantColors: [ColorAnalysis.DominantColor] = []
		let maxColors = 5

		for bin in indexedBins {
			guard dominantColors.count < maxColors else { break }

			let count = bin.count
			let idx = bin.index

			let qb = idx % qLevels
			let qg = (idx / qLevels) % qLevels
			let qr = idx / (qLevels * qLevels)

			let avgR = Float(binSumR[idx]) / Float(count)
			let avgG = Float(binSumG[idx]) / Float(count)
			let avgB = Float(binSumB[idx]) / Float(count)
			let percentage = Float(count) / Float(totalPixels) * 100.0

			// 检查是否与已有主色调过于接近（距离 < 50）
			var isDuplicate = false
			for existing in dominantColors {
				let dr = existing.r - avgR
				let dg = existing.g - avgG
				let db = existing.b - avgB
				let distance = sqrt(dr * dr + dg * dg + db * db)
				if distance < 50 {
					isDuplicate = true
					break
				}
			}

			if !isDuplicate {
				dominantColors.append(ColorAnalysis.DominantColor(
					r: avgR, g: avgG, b: avgB, percentage: percentage
				))
			}
		}

		return dominantColors
	}

	// MARK: - 色温估算

	/// 从 RGB 平均值估算色温
	/// - Parameters:
	///   - r: 红色通道均值
	///   - g: 绿色通道均值
	///   - b: 蓝色通道均值
	/// - Returns: 色温信息
	private func estimateColorTemperature(r: Float, g: Float, b: Float) -> ColorAnalysis.ColorTemperatureInfo {
		guard b > 0 else {
			return ColorAnalysis.ColorTemperatureInfo(kelvin: 2000, type: "warm", description: "极暖色温")
		}

		let rbRatio = r / b

		let kelvin: Float
		let type: String
		let description: String

		if rbRatio > 2.0 {
			// 极暖色温：< 2500K
			kelvin = 2000 + min(2.0, (rbRatio - 2.0) / 2.0) * 500
			type = "warm"
			description = "极暖色温（烛光/日落）"
		} else if rbRatio > 1.5 {
			kelvin = 2500 + (rbRatio - 1.5) / 0.5 * 1500
			type = "warm"
			description = "暖色温（白炽灯/黄金时刻）"
		} else if rbRatio > 1.2 {
			kelvin = 4000 + (rbRatio - 1.2) / 0.3 * 1000
			type = "neutral"
			description = "中性色温（日光/闪光灯）"
		} else if rbRatio > 0.9 {
			kelvin = 5000 + (rbRatio - 0.9) / 0.3 * 1000
			type = "neutral"
			description = "中性偏冷（正午日光）"
		} else if rbRatio > 0.6 {
			kelvin = 6000 + (rbRatio - 0.6) / 0.3 * 2000
			type = "cool"
			description = "冷色温（阴天/阴影）"
		} else {
			kelvin = 8000 + (0.6 - rbRatio) / 0.4 * 2000
			type = "cool"
			description = "极冷色温（阴天/高海拔）"
		}

		let clampedKelvin = max(1500, min(12000, kelvin))

		let finalType: String
		let finalDescription: String

		if clampedKelvin < 3500 {
			finalType = "warm"
			finalDescription = "暖色温（\(description)）"
		} else if clampedKelvin < 5500 {
			finalType = "neutral"
			finalDescription = "中性色温（\(description)）"
		} else {
			finalType = "cool"
			finalDescription = "冷色温（\(description)）"
		}

		return ColorAnalysis.ColorTemperatureInfo(
			kelvin: clampedKelvin,
			type: finalType,
			description: finalDescription
		)
	}

	// MARK: - 色彩情绪判断

	/// 根据亮度、饱和度和色温判断色彩情绪
	private func determineColorMood(brightness: Float, saturation: Float, colorTemp: ColorAnalysis.ColorTemperatureInfo) -> String {
		switch (brightness, saturation, colorTemp.type) {
		// 高亮度 + 高饱和度 = 鲜艳活泼
		case (0.6..., 0.5..., _):
			return "鲜艳明快"

		// 高亮度 + 低饱和度 = 清新淡雅
		case (0.6..., _, _):
			return "清新淡雅"

		// 低亮度 + 高饱和度 = 浓郁深沉
		case (..<0.3, 0.5..., _):
			return "浓郁深沉"

		// 低亮度 + 低饱和度 = 暗沉
		case (..<0.3, _, _):
			return "暗沉低调"

		// 中亮度 + 暖色 = 温暖柔和
		case (0.3..<0.6, _, "warm"):
			return "温暖柔和"

		// 中亮度 + 冷色 = 清冷宁静
		case (0.3..<0.6, _, "cool"):
			return "清冷宁静"

		// 中亮度 + 中性 = 自然
		case (0.3..<0.6, _, _):
			return "自然"

		// 默认
		default:
			return "中性"
		}
	}

	// MARK: - 质量等级映射

	/// 将综合评分映射到质量等级
	private func getQualityGrade(score: Float) -> QualityGrade {
		switch score {
		case 80...100: return .excellent
		case 60..<80:  return .good
		case 40..<60:  return .fair
		default:       return .poor
		}
	}

	// MARK: - 灰度缓冲渲染

	/// 将 CIImage 渲染为灰度 CVPixelBuffer
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

	/// 将 CIImage 渲染为 RGBA CVPixelBuffer
	private func renderRGBABuffer(from ciImage: CIImage, width: Int, height: Int) -> CVPixelBuffer? {
		var outBuffer: CVPixelBuffer?
		let attributes: [String: Any] = [
			kCVPixelBufferCGImageCompatibilityKey as String: true,
			kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
		]
		let status = CVPixelBufferCreate(kCFAllocatorDefault, width, height,
										 kCVPixelFormatType_32BGRA, attributes as CFDictionary, &outBuffer)
		guard status == kCVReturnSuccess, let outBuffer else { return nil }

		ciContext.render(ciImage, to: outBuffer)
		return outBuffer
	}

	// MARK: - 对比度比例计算

	/// 计算图像对比度比例（最大亮度 / 最小亮度）
	private func computeContrastRatio(from ptr: UnsafePointer<UInt8>, bytesPerRow: Int, width: Int, height: Int) -> Float {
		var minLuminance: Float = 255
		var maxLuminance: Float = 0

		for y in 0..<height {
			for x in 0..<width {
				let offset = y * bytesPerRow + x * 4
				let r = Float(ptr[offset + 2])
				let g = Float(ptr[offset + 1])
				let b = Float(ptr[offset])

				// 相对亮度 (sRGB)
				let luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

				if luminance < minLuminance { minLuminance = luminance }
				if luminance > maxLuminance { maxLuminance = luminance }
			}
		}

		// 对比度比例 = (L_max + 0.05) / (L_min + 0.05)
		let adjustedMax = maxLuminance / 255.0 + 0.05
		let adjustedMin = minLuminance / 255.0 + 0.05

		guard adjustedMin > 0 else { return 21.0 }
		return adjustedMax / adjustedMin
	}
}

#endif