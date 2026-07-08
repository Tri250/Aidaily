//
//  BeautyPanelView.swift
//  LiveCapture
//
//  美颜调节面板 - 底部滑出
//

import SwiftUI

#if os(iOS)

/// 美颜调节面板
struct BeautyPanelView: View {
    @ObservedObject var viewModel: PortraitViewModel
    let onClose: () -> Void

    @State private var dragOffset: CGFloat = 0

    var body: some View {
        VStack(spacing: 0) {
            // 拖拽指示器
            Capsule()
                .fill(DesignSystem.Colors.minimalSecondaryLabel)
                .frame(width: 36, height: 5)
                .padding(.top, 8)
                .padding(.bottom, 4)

            // 标题栏
            HStack {
                Text("美颜")
                    .font(DesignSystem.Typography.title3)
                    .foregroundColor(DesignSystem.Colors.minimalLabel)

                Spacer()

                // 重置按钮
                Button {
                    HapticManager.shared.light()
                    viewModel.reset()
                } label: {
                    Text("重置")
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                }
                .accessibilityLabel("重置美颜")
                .accessibilityHint("双击将所有美颜参数恢复默认值")

                // 关闭按钮
                Button {
                    HapticManager.shared.light()
                    onClose()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                }
                .accessibilityLabel("关闭美颜面板")
                .accessibilityHint("双击关闭美颜设置")
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)

            // 预设选择器
            presetSelector
                .padding(.bottom, 16)

            // 美颜参数滑块列表
            ScrollView {
                VStack(spacing: 0) {
                    beautySlider(
                        icon: "sparkles",
                        label: "磨皮",
                        value: $viewModel.skinSmoothing,
                        range: 0...1,
                        onChange: { viewModel.currentPreset = .custom }
                    )

                    Divider()
                        .background(DesignSystem.Colors.minimalBorder)
                        .padding(.leading, 56)

                    beautySlider(
                        icon: "sun.max",
                        label: "美白",
                        value: Binding(
                            get: { viewModel.skinTone },
                            set: { viewModel.skinTone = $0; viewModel.currentPreset = .custom }
                        ),
                        range: -1...1,
                        onChange: {}
                    )

                    Divider()
                        .background(DesignSystem.Colors.minimalBorder)
                        .padding(.leading, 56)

                    beautySlider(
                        icon: "bandage",
                        label: "祛痘",
                        value: Binding(
                            get: { viewModel.blemishRemoval },
                            set: { viewModel.blemishRemoval = $0; viewModel.currentPreset = .custom }
                        ),
                        range: 0...1,
                        onChange: {}
                    )

                    Divider()
                        .background(DesignSystem.Colors.minimalBorder)
                        .padding(.leading, 56)

                    beautySlider(
                        icon: "eye",
                        label: "亮眼",
                        value: Binding(
                            get: { viewModel.eyeBrightening },
                            set: { viewModel.eyeBrightening = $0; viewModel.currentPreset = .custom }
                        ),
                        range: 0...1,
                        onChange: {}
                    )

                    Divider()
                        .background(DesignSystem.Colors.minimalBorder)
                        .padding(.leading, 56)

                    beautySlider(
                        icon: "mouth",
                        label: "牙齿美白",
                        value: Binding(
                            get: { viewModel.teethWhitening },
                            set: { viewModel.teethWhitening = $0; viewModel.currentPreset = .custom }
                        ),
                        range: 0...1,
                        onChange: {}
                    )

                    Divider()
                        .background(DesignSystem.Colors.minimalBorder)
                        .padding(.leading, 56)

                    beautySlider(
                        icon: "face.smiling",
                        label: "瘦脸",
                        value: Binding(
                            get: { viewModel.faceSlimming },
                            set: { viewModel.faceSlimming = $0; viewModel.currentPreset = .custom }
                        ),
                        range: 0...1,
                        onChange: {}
                    )
                }
            }
        }
        .padding(.bottom, 20)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(Color.black.opacity(0.92))
        )
        .offset(y: dragOffset)
        .gesture(
            DragGesture()
                .onChanged { value in
                    if value.translation.height > 0 {
                        dragOffset = value.translation.height
                    }
                }
                .onEnded { value in
                    if value.translation.height > 80 {
                        onClose()
                    }
                    withAnimation(DesignSystem.Animation.overlayFade) {
                        dragOffset = 0
                    }
                }
        )
    }

    // MARK: - Preset Selector

    private var presetSelector: some View {
        HStack(spacing: 12) {
            ForEach(BeautyPreset.allCases, id: \.self) { preset in
                Button {
                    HapticManager.shared.selection()
                    viewModel.applyPreset(preset)
                } label: {
                    Text(preset.displayName)
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(
                            viewModel.currentPreset == preset
                                ? DesignSystem.Colors.minimalLabel
                                : DesignSystem.Colors.minimalSecondaryLabel
                        )
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(
                            RoundedRectangle(cornerRadius: 20)
                                .fill(
                                    viewModel.currentPreset == preset
                                        ? DesignSystem.Colors.primary.opacity(0.3)
                                        : Color.white.opacity(0.08)
                                )
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .strokeBorder(
                                    viewModel.currentPreset == preset
                                        ? DesignSystem.Colors.primary.opacity(0.5)
                                        : Color.white.opacity(0.12),
                                    lineWidth: 1
                                )
                        )
                }
                .accessibilityLabel("\(preset.displayName)预设")
                .accessibilityHint(viewModel.currentPreset == preset ? "已选中" : "双击应用 \(preset.displayName) 预设")
                .accessibilityAddTraits(viewModel.currentPreset == preset ? .isSelected : [])
            }
        }
        .padding(.horizontal, 20)
    }

    // MARK: - Beauty Slider

    private func beautySlider(
        icon: String,
        label: String,
        value: Binding<Float>,
        range: ClosedRange<Float>,
        onChange: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 12) {
            // 图标
            Image(systemName: icon)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                .frame(width: 24)

            // 标签
            Text(label)
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.minimalLabel)
                .frame(width: 56, alignment: .leading)

            // 滑块
            Slider(value: value, in: range) { editing in
                if !editing {
                    onChange()
                }
            }
            .tint(DesignSystem.Colors.primary)
            .accessibilityLabel("\(label)调节")
            .accessibilityValue(percentageText(for: value.wrappedValue, range: range))
            .accessibilityHint("左右滑动调节 \(label) 强度")

            // 百分比显示
            Text(percentageText(for: value.wrappedValue, range: range))
                .font(DesignSystem.Typography.monoDigit)
                .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                .frame(width: 36, alignment: .trailing)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(label): \(percentageText(for: value.wrappedValue, range: range))")
    }

    private func percentageText(for value: Float, range: ClosedRange<Float>) -> String {
        if range.lowerBound < 0 {
            let normalized = (value - range.lowerBound) / (range.upperBound - range.lowerBound)
            return "\(Int(normalized * 100))%"
        }
        return "\(Int(value * 100))%"
    }
}

#endif