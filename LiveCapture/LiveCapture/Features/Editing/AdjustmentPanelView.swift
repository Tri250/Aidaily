//
//  AdjustmentPanelView.swift
//  LiveCapture
//
//  参数调节面板 - 亮度、对比度、饱和度、色温、色调、锐度、高光、阴影、褪色
//

import SwiftUI

#if os(iOS)

/// 调节参数定义
struct AdjustmentParameter: Identifiable {
    let id: String
    let name: String
    let icon: String
    let range: ClosedRange<Float>
    let defaultValue: Float
    var value: Float
    let step: Float
}

struct AdjustmentPanelView: View {
    @ObservedObject var viewModel: PhotoEditorViewModel

    @State private var parameters: [AdjustmentParameter] = []
    @State private var selectedParam: String = "brightness"

    // 实时预览用的参数
    @State private var brightness: Float = 0
    @State private var contrast: Float = 0
    @State private var saturation: Float = 0
    @State private var temperature: Float = 0
    @State private var tint: Float = 0
    @State private var sharpness: Float = 0
    @State private var highlights: Float = 0
    @State private var shadows: Float = 0
    @State private var fade: Float = 0

    var body: some View {
        VStack(spacing: 0) {
            // 参数分类选择器
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DesignSystem.Spacing.xxSmall) {
                    let options: [(String, String, String)] = [
                        ("brightness", "亮度", "sun.max"),
                        ("contrast", "对比度", "circle.lefthalf.filled"),
                        ("saturation", "饱和度", "paintpalette"),
                        ("temperature", "色温", "thermometer.medium"),
                        ("tint", "色调", "drop"),
                        ("sharpness", "锐度", "triangle"),
                        ("highlights", "高光", "circle.topthird.flip"),
                        ("shadows", "阴影", "circle.bottomthird.flip"),
                        ("fade", "褪色", "moon")
                    ]

                    ForEach(options, id: \.0) { option in
                        Button {
                            withAnimation(DesignSystem.Animation.quick) {
                                selectedParam = option.0
                            }
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: option.2)
                                    .font(.system(size: 16))
                                Text(option.1)
                                    .font(DesignSystem.Typography.caption2)
                            }
                            .foregroundColor(selectedParam == option.0 ? .white : DesignSystem.Colors.minimalSecondaryLabel)
                            .frame(width: 56)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                                    .fill(selectedParam == option.0 ? DesignSystem.Colors.primary.opacity(0.3) : Color.clear)
                            )
                        }
                    }
                }
                .padding(.horizontal, DesignSystem.Spacing.small)
            }
            .padding(.vertical, DesignSystem.Spacing.small)

            Divider()
                .background(DesignSystem.Colors.minimalBorder)

            // 滑块区域
            VStack(spacing: DesignSystem.Spacing.medium) {
                switch selectedParam {
                case "brightness":
                    adjustmentSlider(
                        label: "亮度",
                        value: $brightness,
                        range: -1...1,
                        format: "%.2f",
                        onChanged: { v in viewModel.applyAdjustment(parameter: "brightness", value: v) }
                    )
                case "contrast":
                    adjustmentSlider(
                        label: "对比度",
                        value: $contrast,
                        range: -0.5...1.0,
                        format: "%.2f",
                        onChanged: { v in viewModel.applyAdjustment(parameter: "contrast", value: v) }
                    )
                case "saturation":
                    adjustmentSlider(
                        label: "饱和度",
                        value: $saturation,
                        range: -1...1,
                        format: "%.2f",
                        onChanged: { v in viewModel.applyAdjustment(parameter: "saturation", value: v) }
                    )
                case "temperature":
                    adjustmentSlider(
                        label: "色温",
                        value: $temperature,
                        range: -1...1,
                        format: "%.2f",
                        onChanged: { v in viewModel.applyAdjustment(parameter: "temperature", value: v) }
                    )
                case "tint":
                    adjustmentSlider(
                        label: "色调",
                        value: $tint,
                        range: -1...1,
                        format: "%.2f",
                        onChanged: { v in viewModel.applyAdjustment(parameter: "tint", value: v) }
                    )
                case "sharpness":
                    adjustmentSlider(
                        label: "锐度",
                        value: $sharpness,
                        range: 0...2,
                        format: "%.2f",
                        onChanged: { v in viewModel.applyAdjustment(parameter: "sharpness", value: v) }
                    )
                case "highlights":
                    adjustmentSlider(
                        label: "高光",
                        value: $highlights,
                        range: -1...1,
                        format: "%.2f",
                        onChanged: { v in
                            viewModel.editor.highlightAmount = 1.0 - v
                            viewModel.editor.markEdited()
                            viewModel.updatePreview()
                        }
                    )
                case "shadows":
                    adjustmentSlider(
                        label: "阴影",
                        value: $shadows,
                        range: -1...1,
                        format: "%.2f",
                        onChanged: { v in
                            viewModel.editor.shadowAmount = v
                            viewModel.editor.markEdited()
                            viewModel.updatePreview()
                        }
                    )
                case "fade":
                    adjustmentSlider(
                        label: "褪色",
                        value: $fade,
                        range: 0...1,
                        format: "%.2f",
                        onChanged: { v in viewModel.applyAdjustment(parameter: "fade", value: v) }
                    )
                default:
                    EmptyView()
                }
            }
            .padding(.horizontal, DesignSystem.Spacing.small)
            .padding(.vertical, DesignSystem.Spacing.medium)

            Spacer()

            // 重置按钮
            Button {
                withAnimation(DesignSystem.Animation.quick) {
                    viewModel.resetAll()
                    brightness = 0
                    contrast = 0
                    saturation = 0
                    temperature = 0
                    tint = 0
                    sharpness = 0
                    highlights = 0
                    shadows = 0
                    fade = 0
                }
            } label: {
                HStack {
                    Image(systemName: "arrow.counterclockwise")
                    Text("重置所有调整")
                }
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                .padding(.vertical, 12)
                .padding(.horizontal, 24)
                .background(
                    Capsule()
                        .strokeBorder(DesignSystem.Colors.minimalBorder, lineWidth: 1)
                )
            }
            .padding(.bottom, DesignSystem.Spacing.small)
        }
        .background(Color.black)
        .onAppear {
            syncFromEditor()
        }
    }

    // MARK: - 滑块组件

    private func adjustmentSlider(
        label: String,
        value: Binding<Float>,
        range: ClosedRange<Float>,
        format: String,
        onChanged: @escaping (Float) -> Void
    ) -> some View {
        VStack(spacing: DesignSystem.Spacing.xxSmall) {
            HStack {
                Text(label)
                    .font(DesignSystem.Typography.callout)
                    .foregroundColor(DesignSystem.Colors.minimalLabel)

                Spacer()

                Text(String(format: format, value.wrappedValue))
                    .font(DesignSystem.Typography.monoDigit)
                    .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                    .frame(width: 48, alignment: .trailing)
            }

            Slider(value: value, in: range, step: 0.01)
                .tint(DesignSystem.Colors.primary)
                .onChange(of: value.wrappedValue) { newValue in
                    onChanged(newValue)
                }
        }
    }

    // MARK: - 同步数据

    private func syncFromEditor() {
        brightness = viewModel.editor.brightness
        contrast = viewModel.editor.contrast
        saturation = viewModel.editor.saturation
        temperature = viewModel.editor.temperature
        tint = viewModel.editor.tint
        sharpness = viewModel.editor.sharpness
        fade = viewModel.editor.fadeAmount
    }
}

#endif