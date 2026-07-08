//
//  TopControlBar.swift
//  LiveCapture
//
//  极简顶部控制栏 - 自动隐藏
//

import SwiftUI

#if os(iOS)

/// 极简顶部控制栏
struct TopControlBar: View {
	let onFlashToggle: (() -> Void)?
	let onTimerTap: (() -> Void)?
	let onAspectRatioTap: (() -> Void)?
	let onSettingsTap: (() -> Void)?

	var flashMode: FlashMode = .auto
	var showControls: Bool = true

	enum FlashMode: String, CaseIterable {
		case auto, on, off

		var iconName: String {
			switch self {
			case .auto: return "bolt.badge.automatic"
			case .on: return "bolt.fill"
			case .off: return "bolt.slash.fill"
			}
		}

		var next: FlashMode {
			switch self {
			case .auto: return .on
			case .on: return .off
			case .off: return .auto
			}
		}
	}

	var body: some View {
		HStack(spacing: 20) {
			// 闪光灯
			controlButton(
				icon: flashMode.iconName,
				action: onFlashToggle
			)

			Spacer()

			// 计时器
			controlButton(
				icon: "timer",
				action: onTimerTap
			)

			// 画幅比例
			controlButton(
				icon: "aspectratio",
				action: onAspectRatioTap
			)

			// 设置
			controlButton(
				icon: "gearshape",
				action: onSettingsTap
			)
		}
		.padding(.horizontal, 20)
		.padding(.top, 12)
		.opacity(showControls ? 1 : 0)
		.animation(DesignSystem.Animation.overlayFade, value: showControls)
	}

	@ViewBuilder
	private func controlButton(icon: String, action: (() -> Void)?) -> some View {
		if let action = action {
			Button {
				HapticManager.shared.light()
				action()
			} label: {
				Image(systemName: icon)
					.font(.system(size: 16, weight: .medium))
					.foregroundColor(DesignSystem.Colors.minimalLabel)
					.frame(width: 36, height: 36)
					.background(
						Circle()
							.fill(DesignSystem.Colors.minimalDarkOverlay)
					)
			}
		}
	}
}

#endif