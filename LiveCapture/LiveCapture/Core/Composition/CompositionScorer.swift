//
//  CompositionScorer.swift
//  LiveCapture
//
//  构图评分引擎
//
//  ## 文件作用
//  基于多维度构图规则对裁切区域进行综合评分
//  结合 Vision 人脸检测、裁剪框位置和设备陀螺仪数据
//  生成 0-100 分制的评分和中文改进建议
//
//  ## 主要类
//
//  ### CompositionScorer
//  构图评分计算器
//
//  ## 评分维度
//  - 三分法 (ruleOfThirds): 裁切中心是否接近三分点
//  - 平衡性 (balance): 人脸在裁切区域内的分布均匀度
//  - 居中 (centering): 主体是否在画面中心
//  - 水平线 (horizonLevel): 设备是否水平
//
//  ## 评分算法
//  - 三分法: 计算裁切中心到最近三分点的归一化距离，映射到 0-100
//  - 平衡性: 计算所有人脸重心到裁切中心的距离，检查左右/上下分布
//  - 居中: 计算主体群中心到裁切中心的距离
//  - 水平线: 基于陀螺仪 roll 角度，1° 以内满分，线性递减
//
//  ## 综合评分
//  - 人脸存在时: 三分法 35% + 平衡性 35% + 居中 20% + 水平线 10%
//  - 无人脸时: 三分法 50% + 水平线 30% + 居中 20%
//

import Foundation
import CoreGraphics
import Vision
import AVFoundation

#if os(iOS)

/// 构图评分引擎
final class CompositionScorer {

	// MARK: - 评分权重

	/// 有人脸时的权重
	private struct FaceWeights {
		static let ruleOfThirds: Float = 0.35
		static let balance: Float = 0.35
		static let centering: Float = 0.20
		static let horizon: Float = 0.10
	}

	/// 无人脸时的权重
	private struct NoFaceWeights {
		static let ruleOfThirds: Float = 0.50
		static let horizon: Float = 0.30
		static let centering: Float = 0.20
	}

	// MARK: - Public API

	/// 对裁切区域进行综合构图评分
	/// - Parameters:
	///   - cropRect: 归一化裁切矩形 [0,1]
	///   - imageSize: 原始图像尺寸
	///   - faceObservations: Vision 人脸检测结果
	///   - horizonAngle: 设备陀螺仪 roll 角度（弧度）
	/// - Returns: 综合构图评分
	func scoreComposition(
		cropRect: CGRect,
		imageSize: CGSize,
		faceObservations: [VNFaceObservation],
		horizonAngle: CGFloat
	) -> CompositionScore {
		let ruleOfThirdsScore = scoreRuleOfThirds(cropRect: cropRect, imageSize: imageSize)
		let balanceScore = scoreBalance(cropRect: cropRect, faces: faceObservations)
		let centeringScore = scoreCentering(cropRect: cropRect, faces: faceObservations)
		let horizonScore = scoreHorizonLevel(angle: horizonAngle)

		let hasFaces = !faceObservations.isEmpty

		let overall: Float
		if hasFaces {
			overall = ruleOfThirdsScore * FaceWeights.ruleOfThirds
				+ balanceScore * FaceWeights.balance
				+ centeringScore * FaceWeights.centering
				+ horizonScore * FaceWeights.horizon
		} else {
			overall = ruleOfThirdsScore * NoFaceWeights.ruleOfThirds
				+ horizonScore * NoFaceWeights.horizon
				+ centeringScore * NoFaceWeights.centering
		}

		let feedback = generateFeedback(
			ruleOfThirds: ruleOfThirdsScore,
			balance: balanceScore,
			centering: centeringScore,
			horizon: horizonScore,
			hasFaces: hasFaces
		)

		return CompositionScore(
			overall: min(100, max(0, overall)),
			ruleOfThirds: ruleOfThirdsScore,
			balance: balanceScore,
			centering: centeringScore,
			horizonLevel: horizonScore,
			feedback: feedback
		)
	}

	// MARK: - 三分法评分

	/// 计算裁切区域的三分法构图得分
	/// - 计算裁切中心到最近三分点的距离
	/// - 归一化后映射到 0-100 分
	private func scoreRuleOfThirds(cropRect: CGRect, imageSize: CGSize) -> Float {
		let center = CGPoint(x: cropRect.midX, y: cropRect.midY)

		// 四个三分点（归一化坐标）
		let thirdPoints: [CGPoint] = [
			CGPoint(x: 1.0 / 3.0, y: 1.0 / 3.0),
			CGPoint(x: 2.0 / 3.0, y: 1.0 / 3.0),
			CGPoint(x: 1.0 / 3.0, y: 2.0 / 3.0),
			CGPoint(x: 2.0 / 3.0, y: 2.0 / 3.0),
		]

		// 计算到每个三分点的距离
		let distances = thirdPoints.map { point -> CGFloat in
			let dx = center.x - point.x
			let dy = center.y - point.y
			return sqrt(dx * dx + dy * dy)
		}

		let minDistance = distances.min() ?? 0.5

		// 距离映射: 0 距离 = 100 分, 0.3 距离以上 = 0 分
		// 使用指数衰减使近距离得分更高
		let normalizedDistance = min(minDistance / 0.3, 1.0)
		let score = (1.0 - normalizedDistance) * 100.0

		return Float(max(0, min(100, score)))
	}

	// MARK: - 平衡性评分

	/// 计算裁切区域内的视觉平衡性得分
	/// - 检查人脸在裁切区域内的分布是否均匀
	/// - 左右平衡和上下平衡各占 50%
	private func scoreBalance(cropRect: CGRect, faces: [VNFaceObservation]) -> Float {
		guard !faces.isEmpty else { return 50.0 }

		// 计算所有人脸的重心
		let faceCenters = faces.map { face -> CGPoint in
			let box = face.boundingBox
			return CGPoint(x: box.midX, y: box.midY)
		}

		let overallCenter = CGPoint(
			x: faceCenters.reduce(0) { $0 + $1.x } / CGFloat(faceCenters.count),
			y: faceCenters.reduce(0) { $0 + $1.y } / CGFloat(faceCenters.count)
		)

		// 左右平衡: 检查人脸中心相对于裁切中心的水平偏移
		let cropCenterX = cropRect.midX
		let horizontalOffset = abs(overallCenter.x - cropCenterX)
		// 归一化: 偏移 0 = 完美, 偏移 0.3 = 0 分
		let horizontalScore = max(0, 100.0 - (horizontalOffset / 0.3) * 100.0)

		// 上下平衡: 检查人脸中心相对于裁切中心的垂直偏移
		let cropCenterY = cropRect.midY
		let verticalOffset = abs(overallCenter.y - cropCenterY)
		let verticalScore = max(0, 100.0 - (verticalOffset / 0.3) * 100.0)

		// 多人脸时额外检查分散度
		var dispersionScore: Double = 100.0
		if faceCenters.count > 1 {
			// 计算所有人脸到整体中心的平均距离
			let avgDistance = faceCenters.reduce(0.0) { sum, pt in
				let dx = pt.x - overallCenter.x
				let dy = pt.y - overallCenter.y
				return sum + sqrt(Double(dx * dx + dy * dy))
			} / Double(faceCenters.count)

			// 适度的分散度得分高，太集中或太分散得分低
			// 理想分散度约为 0.15-0.25（归一化坐标）
			let idealDispersion: Double = 0.2
			let dispersionDeviation = abs(avgDistance - idealDispersion)
			dispersionScore = max(0, 100.0 - dispersionDeviation * 200.0)
		}

		let balanceScore = (horizontalScore + verticalScore) * 0.4 + dispersionScore * 0.2
		return Float(max(0, min(100, balanceScore)))
	}

	// MARK: - 居中评分

	/// 计算主体居中度得分
	/// - 有脸时: 检查人脸群中心到裁切中心的距离
	/// - 无脸时: 检查裁切中心到画面中心的距离
	private func scoreCentering(cropRect: CGRect, faces: [VNFaceObservation]) -> Float {
		if faces.isEmpty {
			// 无人脸时，裁切中心越接近画面中心越好
			let center = CGPoint(x: cropRect.midX, y: cropRect.midY)
			let imageCenter = CGPoint(x: 0.5, y: 0.5)
			let dx = center.x - imageCenter.x
			let dy = center.y - imageCenter.y
			let distance = sqrt(dx * dx + dy * dy)
			// 距离 0 = 100 分, 距离 0.25 = 0 分
			let score = max(0, 100.0 - (distance / 0.25) * 100.0)
			return Float(score)
		}

		// 有脸时，检查人脸中心到裁切中心的距离
		let faceCenters = faces.map { face -> CGPoint in
			let box = face.boundingBox
			return CGPoint(x: box.midX, y: box.midY)
		}

		let facesCenter = CGPoint(
			x: faceCenters.reduce(0) { $0 + $1.x } / CGFloat(faceCenters.count),
			y: faceCenters.reduce(0) { $0 + $1.y } / CGFloat(faceCenters.count)
		)

		let cropCenter = CGPoint(x: cropRect.midX, y: cropRect.midY)
		let dx = facesCenter.x - cropCenter.x
		let dy = facesCenter.y - cropCenter.y
		let distance = sqrt(dx * dx + dy * dy)

		// 距离 0 = 100 分, 距离 0.2 = 0 分
		let score = max(0, 100.0 - (distance / 0.2) * 100.0)
		return Float(score)
	}

	// MARK: - 水平线评分

	/// 基于陀螺仪角度计算水平线得分
	/// - 角度在 ±1° 以内满分
	/// - 角度在 ±1° 到 ±10° 线性递减
	/// - 超过 ±10° 为 0 分
	private func scoreHorizonLevel(angle: CGFloat) -> Float {
		// 将弧度转为度数
		let degrees = abs(angle * 180.0 / .pi)

		if degrees <= 1.0 {
			return 100.0
		} else if degrees >= 10.0 {
			return 0.0
		} else {
			// 线性映射: 1° = 100, 10° = 0
			let score = 100.0 - (degrees - 1.0) / 9.0 * 100.0
			return Float(max(0, min(100, score)))
		}
	}

	// MARK: - 反馈生成

	/// 根据各维度得分生成中文改进建议
	private func generateFeedback(
		ruleOfThirds: Float,
		balance: Float,
		centering: Float,
		horizon: Float,
		hasFaces: Bool
	) -> String {
		var suggestions: [String] = []

		// 找出最低分维度给出建议
		let scores: [(String, Float)] = [
			("三分法", ruleOfThirds),
			("平衡性", balance),
			("居中", centering),
			("水平", horizon),
		]

		let sortedScores = scores.sorted { $0.1 < $1.1 }

		// 水平线是最重要的，优先提示
		if horizon < 60 {
			suggestions.append("请将设备放平，保持水平")
		}

		if ruleOfThirds < 50 {
			suggestions.append("尝试将主体放在画面三分线交叉点")
		}

		if hasFaces && balance < 50 {
			suggestions.append("调整构图使人物分布更均匀")
		}

		if centering < 50 {
			if hasFaces {
				suggestions.append("将人物置于画面中心区域")
			} else {
				suggestions.append("将主体置于画面中心")
			}
		}

		// 如果所有维度都很好
		if suggestions.isEmpty {
			let minScore = sortedScores.first?.1 ?? 100
			if minScore >= 85 {
				return "构图优秀，继续保持！"
			} else if minScore >= 70 {
				return "构图良好，可微调「\(sortedScores.first?.0 ?? "")」"
			} else {
				return "构图尚可，注意「\(sortedScores.first?.0 ?? "")」"
			}
		}

		return suggestions.joined(separator: "；")
	}
}

#endif