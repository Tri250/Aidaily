//
//  CaptureButton.swift
//  LiveCapture
//
//  极简快门按钮 - 圆形设计，支持拍照/录像双模式
//

import SwiftUI

#if os(iOS)

/// 极简快门按钮
struct CaptureButton: View {
	let action: () -> Void
	let onLongPressStart: (() -> Void)?
	let onLongPressEnd: (() -> Void)?

	@State private var isPressed = false
	@State private var isLongPressing = false
	@State private var recordingProgress: CGFloat = 0
	@State private var recordingTimer: Timer?
	@State private var longPressTriggered = false

	// 内部圆直径
	private let innerDiameter: CGFloat = 62
	// 外圈直径
	private let outerDiameter: CGFloat = 78
	// 描边宽度
	private let strokeWidth: CGFloat = 5

	init(
		action: @escaping () -> Void,
		onLongPressStart: (() -> Void)? = nil,
		onLongPressEnd: (() -> Void)? = nil
	) {
		self.action = action
		self.onLongPressStart = onLongPressStart
		self.onLongPressEnd = onLongPressEnd
	}

	var body: some View {
		ZStack {
			// 外圈描边
			Circle()
				.strokeBorder(
					DesignSystem.Colors.shutterStroke,
					lineWidth: strokeWidth
				)
				.frame(width: outerDiameter, height: outerDiameter)

			// 录像进度环
			if isLongPressing {
				Circle()
					.trim(from: 0, to: recordingProgress)
					.stroke(
						Color.red,
						style: StrokeStyle(lineWidth: 3, lineCap: .round)
					)
					.frame(width: outerDiameter + 6, height: outerDiameter + 6)
					.rotationEffect(.degrees(-90))
					.animation(.linear(duration: 0.1), value: recordingProgress)
			}

			// 内圈实心圆
			Circle()
				.fill(DesignSystem.Colors.shutterInner)
				.frame(
					width: isLongPressing ? innerDiameter * 0.7 : innerDiameter,
					height: isLongPressing ? innerDiameter * 0.7 : innerDiameter
				)
				.animation(DesignSystem.Animation.shutterPress, value: isLongPressing)
		}
		.scaleEffect(isPressed ? 0.92 : 1.0)
		.animation(DesignSystem.Animation.shutterPress, value: isPressed)
		.contentShape(Circle())
		.onLongPressGesture(
			minimumDuration: 0.4,
			maximumDistance: 20
		) { pressing in
			// pressing 状态变化
			isPressed = pressing
			if pressing {
				longPressTriggered = false
				HapticManager.shared.light()
			} else {
				// 松手
				if !longPressTriggered {
					// 短按 = 拍照
					HapticManager.shared.capture()
					action()
				} else {
					// 长按结束 = 停止录像
					isLongPressing = false
					stopRecordingAnimation()
					onLongPressEnd?()
				}
				isPressed = false
			}
		} perform: {
			// 长按触发
			longPressTriggered = true
			let generator = UIImpactFeedbackGenerator(style: .heavy)
			generator.impactOccurred()
			isLongPressing = true
			onLongPressStart?()
			startRecordingAnimation()
		}
	}

	private func startRecordingAnimation() {
		recordingProgress = 0
		recordingTimer?.invalidate()
		recordingTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { timer in
			recordingProgress += 0.05 / 60.0
			if recordingProgress >= 1.0 {
				timer.invalidate()
				recordingTimer = nil
			}
		}
	}

	private func stopRecordingAnimation() {
		recordingTimer?.invalidate()
		recordingTimer = nil
		withAnimation(DesignSystem.Animation.shutterRelease) {
			recordingProgress = 0
		}
	}
}

#endif