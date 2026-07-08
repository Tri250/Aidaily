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
	let onBeautyTap: (() -> Void)?
	let onPortraitModeTap: (() -> Void)?
	let onSettingsTap: (() -> Void)?

	var flashMode: FlashMode = .auto
	var showControls: Bool = true
	var isBeautyEnabled: Bool = false
	var isPortraitModeEnabled: Bool = false

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
				accessibilityLabel: "闪光灯",
				accessibilityHint: "当前模式：\(flashModeDisplayName)，双击切换",
				action: onFlashToggle
			)

			// 美颜
			controlButton(
				icon: "face.smiling",
				isActive: isBeautyEnabled,
				accessibilityLabel: "美颜",
				accessibilityHint: isBeautyEnabled ? "美颜已开启，双击关闭" : "美颜已关闭，双击开启",
				action: onBeautyTap
			)

			Spacer()

			// 人像模式
			controlButton(
				icon: "person.crop.rectangle.portrait",
				isActive: isPortraitModeEnabled,
				accessibilityLabel: "人像模式",
				accessibilityHint: isPortraitModeEnabled ? "人像模式已开启，双击关闭" : "人像模式已关闭，双击开启",
				action: onPortraitModeTap
			)

			// 计时器
			controlButton(
				icon: "timer",
				accessibilityLabel: "计时拍摄",
				accessibilityHint: "双击设置计时拍摄",
				action: onTimerTap
			)

			// 画幅比例
			controlButton(
				icon: "aspectratio",
				accessibilityLabel: "画幅比例",
				accessibilityHint: "双击切换画幅比例",
				action: onAspectRatioTap
			)

			// 设置
			controlButton(
				icon: "gearshape",
				accessibilityLabel: "设置",
				accessibilityHint: "双击打开设置",
				action: onSettingsTap
			)
		}
		.padding(.horizontal, 20)
		.padding(.top, 12)
		.opacity(showControls ? 1 : 0)
		.animation(DesignSystem.Animation.overlayFade, value: showControls)
	}

	private var flashModeDisplayName: String {
		switch flashMode {
		case .auto: return "自动"
		case .on: return "开启"
		case .off: return "关闭"
		}
	}

	@ViewBuilder
	private func controlButton(icon: String, isActive: Bool = false, accessibilityLabel: String, accessibilityHint: String, action: (() -> Void)?) -> some View {
		if let action = action {
			Button {
				HapticManager.shared.light()
				action()
			} label: {
				Image(systemName: icon)
					.font(.system(size: 16, weight: .medium))
					.foregroundColor(isActive ? DesignSystem.Colors.primary : DesignSystem.Colors.minimalLabel)
					.frame(width: 36, height: 36)
					.background(
						Circle()
							.fill(isActive ? DesignSystem.Colors.primary.opacity(0.2) : DesignSystem.Colors.minimalDarkOverlay)
					)
					.overlay(
						Circle()
							.strokeBorder(
								isActive ? DesignSystem.Colors.primary.opacity(0.5) : Color.clear,
								lineWidth: 1.5
							)
					)
			}
			.accessibilityLabel(accessibilityLabel)
			.accessibilityHint(accessibilityHint)
			.accessibilityAddTraits(isActive ? .isSelected : [])
		}
	}
}

#endif