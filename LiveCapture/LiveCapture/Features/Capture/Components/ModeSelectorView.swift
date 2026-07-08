//
//  ModeSelectorView.swift
//  LiveCapture
//
//  模式选择器 - 水平滚动，触觉反馈
//

import SwiftUI

#if os(iOS)

/// 拍摄模式选择器
struct ModeSelectorView: View {
	@Binding var selectedMode: CaptureMode

	enum CaptureMode: String, CaseIterable {
		case photo
		case portrait
		case video
		case slowMotion
		case timelapse

		var displayName: String {
			switch self {
			case .photo: return "照片"
			case .portrait: return "人像"
			case .video: return "视频"
			case .slowMotion: return "慢动作"
			case .timelapse: return "延时"
			}
		}

		var iconName: String {
			switch self {
			case .photo: return "camera.fill"
			case .portrait: return "person.crop.square.fill"
			case .video: return "video.fill"
			case .slowMotion: return "slowmo"
			case .timelapse: return "timelapse"
			}
		}

		var color: Color {
			switch self {
			case .photo: return .white
			case .portrait: return .yellow
			case .video: return .red
			case .slowMotion: return .orange
			case .timelapse: return .mint
			}
		}
	}

	var body: some View {
		ScrollViewReader { proxy in
			ScrollView(.horizontal, showsIndicators: false) {
				HStack(spacing: 8) {
					ForEach(CaptureMode.allCases, id: \.self) { mode in
						modeButton(for: mode, proxy: proxy)
					}
				}
				.padding(.horizontal, 16)
			}
		}
		.padding(.vertical, 10)
		.background(
			Rectangle()
				.fill(DesignSystem.Colors.minimalDarkOverlay)
		)
	}

	@ViewBuilder
	private func modeButton(for mode: CaptureMode, proxy: ScrollViewProxy) -> some View {
		let isActive = selectedMode == mode

		Button {
			HapticManager.shared.selection()
			withAnimation(DesignSystem.Animation.modeSlide) {
				selectedMode = mode
			}
			withAnimation(DesignSystem.Animation.smooth) {
				proxy.scrollTo(mode, anchor: .center)
			}
		} label: {
			VStack(spacing: 4) {
				Image(systemName: mode.iconName)
					.font(.system(size: 18, weight: .medium))
					.foregroundColor(isActive ? mode.color : DesignSystem.Colors.minimalSecondaryLabel)
					.frame(width: 40, height: 28)

				Text(mode.displayName)
					.font(DesignSystem.Typography.minimalModeLabel)
					.foregroundColor(isActive ? DesignSystem.Colors.minimalLabel : DesignSystem.Colors.minimalSecondaryLabel)
			}
			.padding(.horizontal, 12)
			.padding(.vertical, 6)
			.background(
				RoundedRectangle(cornerRadius: 8)
					.fill(isActive ? Color.white.opacity(0.15) : Color.clear)
			)
			.overlay(
				RoundedRectangle(cornerRadius: 8)
					.strokeBorder(
						isActive ? Color.white.opacity(0.3) : Color.clear,
						lineWidth: 1
					)
			)
		}
		.id(mode)
	}
}

#endif