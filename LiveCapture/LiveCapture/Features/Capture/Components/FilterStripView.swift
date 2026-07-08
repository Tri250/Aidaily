//
//  FilterStripView.swift
//  LiveCapture
//
//  水平滚动滤镜条 - 底部实时预览
//

import SwiftUI

#if os(iOS)

/// 水平滚动滤镜选择条
struct FilterStripView: View {
	@ObservedObject var filterManager: FilterPresetManager
	var onFilterSelected: (LutFilterPreset) -> Void

	var body: some View {
		VStack(spacing: 6) {
			// 当前滤镜名称
			if let selected = filterManager.selectedPreset {
				Text(selected.displayName)
					.font(DesignSystem.Typography.minimalFilterName)
					.foregroundColor(DesignSystem.Colors.minimalLabel)
					.transition(.opacity.combined(with: .scale))
			}

			ScrollView(.horizontal, showsIndicators: false) {
				HStack(spacing: 12) {
					// 无滤镜选项
					FilterPreviewCell(
						preset: nil,
						isSelected: filterManager.selectedPreset == nil,
						onTap: {
							HapticManager.shared.light()
							filterManager.clearSelection()
						}
					)

					// 所有滤镜预设
					ForEach(filterManager.presets) { preset in
						FilterPreviewCell(
							preset: preset,
							isSelected: filterManager.selectedPreset?.id == preset.id,
							onTap: {
								HapticManager.shared.selection()
								filterManager.selectPreset(preset)
								onFilterSelected(preset)
							}
						)
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
}

/// 单个滤镜预览圆
private struct FilterPreviewCell: View {
	let preset: LutFilterPreset?
	let isSelected: Bool
	let onTap: () -> Void

	private let circleSize: CGFloat = 52

	var body: some View {
		Button(action: onTap) {
			VStack(spacing: 4) {
				ZStack {
					Circle()
						.fill(Color.white.opacity(0.08))
						.frame(width: circleSize, height: circleSize)

					if let preset = preset {
						// 滤镜名称首字母
						Text(String(preset.displayName.prefix(1)))
							.font(.system(size: 18, weight: .medium))
							.foregroundColor(.white.opacity(0.8))
					} else {
						// 原始 - 无滤镜
						Image(systemName: "camera.fill")
							.font(.system(size: 16, weight: .medium))
							.foregroundColor(.white.opacity(0.6))
					}
				}
				.overlay(
					Circle()
						.strokeBorder(
							isSelected
								? DesignSystem.Colors.minimalActiveBorder
								: DesignSystem.Colors.minimalBorder,
							lineWidth: isSelected ? 2.0 : 1.0
						)
				)
				.animation(DesignSystem.Animation.snappy, value: isSelected)

				Text(preset?.displayName ?? "原始")
					.font(DesignSystem.Typography.minimalFilterName)
					.foregroundColor(
						isSelected
							? DesignSystem.Colors.minimalLabel
							: DesignSystem.Colors.minimalSecondaryLabel
					)
					.lineLimit(1)
			}
		}
		.frame(width: circleSize + 8)
	}
}

#endif