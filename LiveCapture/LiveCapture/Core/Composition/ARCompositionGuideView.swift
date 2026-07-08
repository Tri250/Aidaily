//
//  ARCompositionGuideView.swift
//  LiveCapture
//
//  AR 构图引导叠加视图
//
//  ## 文件作用
//  在相机预览上渲染 AR 构图引导线和信息
//  使用 CoreGraphics 绘制五种构图引导线
//  显示实时评分、姿势模板叠加和水平指示器
//
//  ## 主要类型
//
//  ### ARCompositionGuideView (UIView)
//  使用 CoreGraphics 绘制的构图引导视图
//  - 绘制五分线、三分线、黄金螺旋、对称线、中心十字
//  - 绘制水平仪指示器
//  - 绘制评分徽章
//  - 绘制姿势模板文字叠加
//
//  ### ARCompositionGuideOverlay (UIViewRepresentable)
//  SwiftUI 桥接包装器
//  - 通过 Binding 与 SwiftUI 状态同步
//  - 支持所有引导类型和显示选项
//
//  ## 绘制细节
//  - 引导线使用半透明白色，带发光效果
//  - 三分点使用小圆点标记
//  - 水平仪使用圆弧和指针
//  - 评分徽章使用圆角矩形带渐变
//

import UIKit
import SwiftUI
import CoreGraphics

#if os(iOS)

// MARK: - ARCompositionGuideView

/// AR 构图引导叠加视图
final class ARCompositionGuideView: UIView {

	// MARK: - Properties

	/// 当前引导线类型
	var guideType: CompositionGuideType = .ruleOfThirds {
		didSet { setNeedsDisplay() }
	}

	/// 当前构图评分
	var score: CompositionScore? {
		didSet { setNeedsDisplay() }
	}

	/// 当前姿势模板
	var currentPoseTemplate: PoseTemplate? {
		didSet { setNeedsDisplay() }
	}

	/// 是否显示评分
	var showScore: Bool = true {
		didSet { setNeedsDisplay() }
	}

	/// 是否显示姿势引导
	var showPoseGuide: Bool = false {
		didSet { setNeedsDisplay() }
	}

	/// 是否显示水平仪
	var showLevel: Bool = true {
		didSet { setNeedsDisplay() }
	}

	/// 当前水平角度（弧度）
	var horizonAngle: CGFloat = 0 {
		didSet { setNeedsDisplay() }
	}

	// MARK: - Drawing Constants

	private enum GuideColors {
		static let lineColor = UIColor.white.withAlphaComponent(0.3)
		static let glowColor = UIColor.white.withAlphaComponent(0.15)
		static let pointColor = UIColor.white.withAlphaComponent(0.6)
		static let centerColor = UIColor.white.withAlphaComponent(0.5)
		static let scoreBackground = UIColor.black.withAlphaComponent(0.6)
		static let scoreText = UIColor.white
		static let goldenColor = UIColor(red: 0.98, green: 0.72, blue: 0.28, alpha: 0.5)
		static let levelColor = UIColor(red: 0.2, green: 0.78, blue: 0.35, alpha: 0.8)
		static let levelOffColor = UIColor(red: 1.0, green: 0.23, blue: 0.19, alpha: 0.8)
	}

	private enum GuideMetrics {
		static let lineWidth: CGFloat = 1.0
		static let glowLineWidth: CGFloat = 3.0
		static let dotRadius: CGFloat = 4.0
		static let crosshairSize: CGFloat = 30.0
		static let crosshairGap: CGFloat = 8.0
		static let scoreBadgeWidth: CGFloat = 80.0
		static let scoreBadgeHeight: CGFloat = 36.0
		static let levelIndicatorRadius: CGFloat = 40.0
		static let levelIndicatorLineWidth: CGFloat = 3.0
		static let levelIndicatorArcWidth: CGFloat = 4.0
	}

	// MARK: - Drawing

	override func draw(_ rect: CGRect) {
		guard let ctx = UIGraphicsGetCurrentContext() else { return }

		ctx.setShouldAntialias(true)
		ctx.setAllowsAntialiasing(true)

		// 绘制引导线
		switch guideType {
		case .ruleOfThirds:
			drawRuleOfThirdsGrid(in: rect, context: ctx)
		case .goldenRatio:
			drawGoldenRatioSpiral(in: rect, context: ctx)
		case .symmetry:
			drawSymmetryLines(in: rect, context: ctx)
		case .centerFocus:
			drawCenterFocus(in: rect, context: ctx)
		case .leadingLines:
			drawLeadingLines(in: rect, context: ctx)
		}

		// 绘制水平仪
		if showLevel {
			drawLevelIndicator(in: rect, context: ctx)
		}

		// 绘制评分徽章
		if showScore, let score = score {
			drawScoreBadge(in: rect, score: score, context: ctx)
		}

		// 绘制姿势模板提示
		if showPoseGuide, let template = currentPoseTemplate {
			drawPoseTemplateOverlay(in: rect, template: template, context: ctx)
		}
	}

	// MARK: - 三分线网格

	private func drawRuleOfThirdsGrid(in rect: CGRect, context: CGContext) {
		let thirdX1 = rect.width / 3.0
		let thirdX2 = rect.width * 2.0 / 3.0
		let thirdY1 = rect.height / 3.0
		let thirdY2 = rect.height * 2.0 / 3.0

		// 发光效果
		context.setStrokeColor(GuideColors.glowColor.cgColor)
		context.setLineWidth(GuideMetrics.glowLineWidth)

		context.move(to: CGPoint(x: thirdX1, y: 0))
		context.addLine(to: CGPoint(x: thirdX1, y: rect.height))
		context.move(to: CGPoint(x: thirdX2, y: 0))
		context.addLine(to: CGPoint(x: thirdX2, y: rect.height))
		context.move(to: CGPoint(x: 0, y: thirdY1))
		context.addLine(to: CGPoint(x: rect.width, y: thirdY1))
		context.move(to: CGPoint(x: 0, y: thirdY2))
		context.addLine(to: CGPoint(x: rect.width, y: thirdY2))
		context.strokePath()

		// 实线
		context.setStrokeColor(GuideColors.lineColor.cgColor)
		context.setLineWidth(GuideMetrics.lineWidth)

		context.move(to: CGPoint(x: thirdX1, y: 0))
		context.addLine(to: CGPoint(x: thirdX1, y: rect.height))
		context.move(to: CGPoint(x: thirdX2, y: 0))
		context.addLine(to: CGPoint(x: thirdX2, y: rect.height))
		context.move(to: CGPoint(x: 0, y: thirdY1))
		context.addLine(to: CGPoint(x: rect.width, y: thirdY1))
		context.move(to: CGPoint(x: 0, y: thirdY2))
		context.addLine(to: CGPoint(x: rect.width, y: thirdY2))
		context.strokePath()

		// 三分点圆点
		let dotRadius = GuideMetrics.dotRadius
		let thirdPoints = [
			CGPoint(x: thirdX1, y: thirdY1),
			CGPoint(x: thirdX2, y: thirdY1),
			CGPoint(x: thirdX1, y: thirdY2),
			CGPoint(x: thirdX2, y: thirdY2),
		]

		context.setFillColor(GuideColors.pointColor.cgColor)
		for point in thirdPoints {
			let dotRect = CGRect(
				x: point.x - dotRadius,
				y: point.y - dotRadius,
				width: dotRadius * 2,
				height: dotRadius * 2
			)
			context.fillEllipse(in: dotRect)
		}
	}

	// MARK: - 黄金分割螺旋

	private func drawGoldenRatioSpiral(in rect: CGRect, context: CGContext) {
		let phi: CGFloat = 1.618034

		// 绘制黄金分割线（水平和垂直各一条）
		let goldenX = rect.width / phi
		let goldenY = rect.height / phi

		context.setStrokeColor(GuideColors.goldenColor.cgColor)
		context.setLineWidth(GuideMetrics.lineWidth)

		// 垂直黄金分割线
		context.move(to: CGPoint(x: goldenX, y: 0))
		context.addLine(to: CGPoint(x: goldenX, y: rect.height))
		context.strokePath()

		// 水平黄金分割线
		context.move(to: CGPoint(x: 0, y: goldenY))
		context.addLine(to: CGPoint(x: rect.width, y: goldenY))
		context.strokePath()

		// 黄金分割点
		let goldenPoints = [
			CGPoint(x: goldenX, y: goldenY),
			CGPoint(x: rect.width - goldenX, y: goldenY),
			CGPoint(x: goldenX, y: rect.height - goldenY),
			CGPoint(x: rect.width - goldenX, y: rect.height - goldenY),
		]

		context.setFillColor(GuideColors.goldenColor.cgColor)
		let dotRadius = GuideMetrics.dotRadius
		for point in goldenPoints {
			let dotRect = CGRect(
				x: point.x - dotRadius,
				y: point.y - dotRadius,
				width: dotRadius * 2,
				height: dotRadius * 2
			)
			context.fillEllipse(in: dotRect)
		}

		// 绘制黄金螺旋（简化版：用一系列 1/4 圆弧近似）
		drawGoldenSpiral(in: rect, context: context)
	}

	private func drawGoldenSpiral(in rect: CGRect, context: CGContext) {
		let phi: CGFloat = 1.618034
		context.setStrokeColor(GuideColors.goldenColor.cgColor)
		context.setLineWidth(1.5)

		// 从右下角开始螺旋
		var currentSize = rect.width
		var x: CGFloat = 0
		var y: CGFloat = rect.height - currentSize / phi

		// 简化螺旋：绘制递减的弧线
		for i in 0..<6 {
			let size = currentSize / pow(phi, CGFloat(i))
			let centerX = x + size
			let centerY = y + size

			let arcRect = CGRect(x: centerX - size, y: centerY - size, width: size * 2, height: size * 2)

			context.addArc(
				center: CGPoint(x: centerX, y: centerY),
				radius: size,
				startAngle: 0,
				endAngle: .pi / 2,
				clockwise: false
			)
			context.strokePath()

			x += size
			y += size * (1.0 - 1.0 / phi)
		}
	}

	// MARK: - 对称线

	private func drawSymmetryLines(in rect: CGRect, context: CGContext) {
		let centerX = rect.width / 2.0
		let centerY = rect.height / 2.0

		context.setStrokeColor(GuideColors.lineColor.cgColor)
		context.setLineWidth(GuideMetrics.lineWidth)

		// 垂直中线
		context.move(to: CGPoint(x: centerX, y: 0))
		context.addLine(to: CGPoint(x: centerX, y: rect.height))
		context.strokePath()

		// 水平中线
		context.move(to: CGPoint(x: 0, y: centerY))
		context.addLine(to: CGPoint(x: rect.width, y: centerY))
		context.strokePath()

		// 中心点
		context.setFillColor(GuideColors.pointColor.cgColor)
		let dotRadius = GuideMetrics.dotRadius * 1.5
		let centerDot = CGRect(
			x: centerX - dotRadius,
			y: centerY - dotRadius,
			width: dotRadius * 2,
			height: dotRadius * 2
		)
		context.fillEllipse(in: centerDot)
	}

	// MARK: - 中心聚焦

	private func drawCenterFocus(in rect: CGRect, context: CGContext) {
		let centerX = rect.width / 2.0
		let centerY = rect.height / 2.0
		let crossSize = GuideMetrics.crosshairSize
		let gap = GuideMetrics.crosshairGap

		context.setStrokeColor(GuideColors.centerColor.cgColor)
		context.setLineWidth(2.0)

		// 左上角
		context.move(to: CGPoint(x: centerX - crossSize, y: centerY - gap))
		context.addLine(to: CGPoint(x: centerX - gap, y: centerY - gap))
		context.move(to: CGPoint(x: centerX - gap, y: centerY - crossSize))
		context.addLine(to: CGPoint(x: centerX - gap, y: centerY - gap))

		// 右上角
		context.move(to: CGPoint(x: centerX + gap, y: centerY - crossSize))
		context.addLine(to: CGPoint(x: centerX + gap, y: centerY - gap))
		context.move(to: CGPoint(x: centerX + gap, y: centerY - gap))
		context.addLine(to: CGPoint(x: centerX + crossSize, y: centerY - gap))

		// 左下角
		context.move(to: CGPoint(x: centerX - crossSize, y: centerY + gap))
		context.addLine(to: CGPoint(x: centerX - gap, y: centerY + gap))
		context.move(to: CGPoint(x: centerX - gap, y: centerY + gap))
		context.addLine(to: CGPoint(x: centerX - gap, y: centerY + crossSize))

		// 右下角
		context.move(to: CGPoint(x: centerX + gap, y: centerY + gap))
		context.addLine(to: CGPoint(x: centerX + crossSize, y: centerY + gap))
		context.move(to: CGPoint(x: centerX + gap, y: centerY + crossSize))
		context.addLine(to: CGPoint(x: centerX + gap, y: centerY + gap))

		context.strokePath()

		// 中心小圆
		context.setFillColor(GuideColors.pointColor.cgColor)
		let dotRadius = GuideMetrics.dotRadius
		let dotRect = CGRect(
			x: centerX - dotRadius,
			y: centerY - dotRadius,
			width: dotRadius * 2,
			height: dotRadius * 2
		)
		context.fillEllipse(in: dotRect)
	}

	// MARK: - 引导线（对角线）

	private func drawLeadingLines(in rect: CGRect, context: CGContext) {
		context.setStrokeColor(GuideColors.lineColor.cgColor)
		context.setLineWidth(GuideMetrics.lineWidth)

		// 两条对角线
		context.move(to: CGPoint(x: 0, y: 0))
		context.addLine(to: CGPoint(x: rect.width, y: rect.height))
		context.move(to: CGPoint(x: rect.width, y: 0))
		context.addLine(to: CGPoint(x: 0, y: rect.height))
		context.strokePath()

		// 中心点
		let centerX = rect.width / 2.0
		let centerY = rect.height / 2.0
		context.setFillColor(GuideColors.pointColor.cgColor)
		let dotRadius = GuideMetrics.dotRadius
		let dotRect = CGRect(
			x: centerX - dotRadius,
			y: centerY - dotRadius,
			width: dotRadius * 2,
			height: dotRadius * 2
		)
		context.fillEllipse(in: dotRect)
	}

	// MARK: - 水平仪

	private func drawLevelIndicator(in rect: CGRect, context: CGContext) {
		let centerX = rect.width / 2.0
		let indicatorY = rect.height - 80.0
		let radius = GuideMetrics.levelIndicatorRadius

		let degrees = abs(horizonAngle * 180.0 / .pi)
		let isLevel = degrees <= 1.0

		let arcColor = isLevel ? GuideColors.levelColor : GuideColors.levelOffColor

		// 绘制背景圆弧
		context.setStrokeColor(UIColor.white.withAlphaComponent(0.15).cgColor)
		context.setLineWidth(GuideMetrics.levelIndicatorArcWidth)
		context.addArc(
			center: CGPoint(x: centerX, y: indicatorY),
			radius: radius,
			startAngle: .pi * 0.25,
			endAngle: .pi * 0.75,
			clockwise: true
		)
		context.strokePath()

		// 绘制当前角度指示弧
		context.setStrokeColor(arcColor.cgColor)
		context.setLineWidth(GuideMetrics.levelIndicatorArcWidth)

		let angleRange: CGFloat = .pi * 0.5 // 90度范围
		let normalizedAngle = min(max(horizonAngle / CGFloat(10.0 * .pi / 180.0), -1.0), 1.0)
		let indicatorAngle: CGFloat = .pi / 2.0 // 默认指向正上方

		// 绘制指针
		let pointerStart = CGPoint(
			x: centerX,
			y: indicatorY - radius + 8
		)
		let pointerEnd = CGPoint(
			x: centerX + sin(normalizedAngle * angleRange / 2) * (radius - 4),
			y: indicatorY - cos(normalizedAngle * angleRange / 2) * (radius - 4)
		)

		context.setStrokeColor(arcColor.cgColor)
		context.setLineWidth(GuideMetrics.levelIndicatorLineWidth)
		context.move(to: pointerStart)
		context.addLine(to: pointerEnd)
		context.strokePath()

		// 中心点
		context.setFillColor(arcColor.cgColor)
		let dotRadius: CGFloat = 3.0
		let dotRect = CGRect(
			x: centerX - dotRadius,
			y: indicatorY - dotRadius,
			width: dotRadius * 2,
			height: dotRadius * 2
		)
		context.fillEllipse(in: dotRect)

		// 角度文字
		let angleText = String(format: "%.1f°", degrees)
		let textAttributes: [NSAttributedString.Key: Any] = [
			.font: UIFont.monospacedDigitSystemFont(ofSize: 11, weight: .medium),
			.foregroundColor: arcColor,
		]
		let textSize = angleText.size(withAttributes: textAttributes)
		let textRect = CGRect(
			x: centerX - textSize.width / 2,
			y: indicatorY + radius + 4,
			width: textSize.width,
			height: textSize.height
		)
		angleText.draw(in: textRect, withAttributes: textAttributes)
	}

	// MARK: - 评分徽章

	private func drawScoreBadge(in rect: CGRect, score: CompositionScore, context: CGContext) {
		let badgeWidth = GuideMetrics.scoreBadgeWidth
		let badgeHeight = GuideMetrics.scoreBadgeHeight
		let badgeX = rect.width - badgeWidth - 16
		let badgeY: CGFloat = 60

		let badgeRect = CGRect(x: badgeX, y: badgeY, width: badgeWidth, height: badgeHeight)
		let badgePath = UIBezierPath(roundedRect: badgeRect, cornerRadius: 10)

		context.saveGState()
		context.addPath(badgePath.cgPath)
		context.setFillColor(GuideColors.scoreBackground.cgColor)
		context.fillPath()

		// 边框
		context.addPath(badgePath.cgPath)
		context.setStrokeColor(UIColor.white.withAlphaComponent(0.3).cgColor)
		context.setLineWidth(1.0)
		context.strokePath()
		context.restoreGState()

		// 分数文字
		let scoreText = String(format: "%.0f", score.overall)
		let scoreAttributes: [NSAttributedString.Key: Any] = [
			.font: UIFont.systemFont(ofSize: 18, weight: .bold),
			.foregroundColor: GuideColors.scoreText,
		]
		let scoreSize = scoreText.size(withAttributes: scoreAttributes)
		let scoreRect = CGRect(
			x: badgeX + (badgeWidth - scoreSize.width) / 2,
			y: badgeY + 2,
			width: scoreSize.width,
			height: scoreSize.height
		)
		scoreText.draw(in: scoreRect, withAttributes: scoreAttributes)

		// 等级文字
		let gradeText = score.grade.rawValue
		let gradeAttributes: [NSAttributedString.Key: Any] = [
			.font: UIFont.systemFont(ofSize: 10, weight: .medium),
			.foregroundColor: UIColor.white.withAlphaComponent(0.7),
		]
		let gradeSize = gradeText.size(withAttributes: gradeAttributes)
		let gradeRect = CGRect(
			x: badgeX + (badgeWidth - gradeSize.width) / 2,
			y: badgeY + 22,
			width: gradeSize.width,
			height: gradeSize.height
		)
		gradeText.draw(in: gradeRect, withAttributes: gradeAttributes)
	}

	// MARK: - 姿势模板叠加

	private func drawPoseTemplateOverlay(in rect: CGRect, template: PoseTemplate, context: CGContext) {
		// 底部半透明条
		let overlayHeight: CGFloat = 60
		let overlayY = rect.height - overlayHeight - 120
		let overlayRect = CGRect(x: 0, y: overlayY, width: rect.width, height: overlayHeight)

		context.saveGState()
		context.setFillColor(UIColor.black.withAlphaComponent(0.5).cgColor)
		context.fill(overlayRect)
		context.restoreGState()

		// 模板名称
		let nameAttributes: [NSAttributedString.Key: Any] = [
			.font: UIFont.systemFont(ofSize: 16, weight: .semibold),
			.foregroundColor: UIColor.white,
		]
		let nameSize = template.name.size(withAttributes: nameAttributes)
		let nameRect = CGRect(
			x: 16,
			y: overlayY + 8,
			width: nameSize.width,
			height: nameSize.height
		)
		template.name.draw(in: nameRect, withAttributes: nameAttributes)

		// 模板描述
		let descAttributes: [NSAttributedString.Key: Any] = [
			.font: UIFont.systemFont(ofSize: 12),
			.foregroundColor: UIColor.white.withAlphaComponent(0.7),
		]
		let descSize = template.description.size(withAttributes: descAttributes)
		let descRect = CGRect(
			x: 16,
			y: overlayY + 28,
			width: descSize.width,
			height: descSize.height
		)
		template.description.draw(in: descRect, withAttributes: descAttributes)

		// 技巧提示数量
		if let firstTip = template.tips.first {
			let tipAttributes: [NSAttributedString.Key: Any] = [
				.font: UIFont.systemFont(ofSize: 11),
				.foregroundColor: UIColor(red: 0.98, green: 0.72, blue: 0.28, alpha: 1.0),
			]
			let tipText = "💡 \(firstTip)"
			let tipSize = tipText.size(withAttributes: tipAttributes)
			let tipRect = CGRect(
				x: 16,
				y: overlayY + 44,
				width: min(tipSize.width, rect.width - 32),
				height: tipSize.height
			)
			tipText.draw(in: tipRect, withAttributes: tipAttributes)
		}
	}

	// MARK: - 动画更新

	/// 带动画更新评分显示
	func updateScore(_ newScore: CompositionScore, animated: Bool) {
		if animated {
			UIView.transition(
				with: self,
				duration: 0.3,
				options: [.transitionCrossDissolve, .allowUserInteraction],
				animations: {
					self.score = newScore
				}
			)
		} else {
			self.score = newScore
		}
	}
}

// MARK: - SwiftUI Wrapper

/// AR 构图引导叠加层的 SwiftUI 包装器
struct ARCompositionGuideOverlay: UIViewRepresentable {
	@Binding var guideType: CompositionGuideType
	@Binding var score: CompositionScore?
	@Binding var poseTemplate: PoseTemplate?
	@Binding var showScore: Bool
	@Binding var showPoseGuide: Bool
	@Binding var showLevel: Bool
	@Binding var horizonAngle: CGFloat

	func makeUIView(context: Context) -> ARCompositionGuideView {
		let view = ARCompositionGuideView()
		view.backgroundColor = .clear
		view.isUserInteractionEnabled = false
		return view
	}

	func updateUIView(_ uiView: ARCompositionGuideView, context: Context) {
		uiView.guideType = guideType
		uiView.score = score
		uiView.currentPoseTemplate = poseTemplate
		uiView.showScore = showScore
		uiView.showPoseGuide = showPoseGuide
		uiView.showLevel = showLevel
		uiView.horizonAngle = horizonAngle
	}
}

#endif