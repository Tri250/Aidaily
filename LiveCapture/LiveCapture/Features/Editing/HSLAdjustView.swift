//
//  HSLAdjustView.swift
//  LiveCapture
//
//  HSL 颜色调整 - 8 个颜色通道的色相/饱和度/明度
//

import SwiftUI

#if os(iOS)

/// HSL 颜色通道
struct HSLChannel: Identifiable {
    let id: Int
    let name: String
    let color: Color
    let index: Int // 对应 hslHue/Saturation/Lightness 数组索引

    static let channels: [HSLChannel] = [
        HSLChannel(id: 0, name: "红", color: Color(red: 1, green: 0.1, blue: 0.1), index: 0),
        HSLChannel(id: 1, name: "橙", color: Color(red: 1, green: 0.5, blue: 0), index: 1),
        HSLChannel(id: 2, name: "黄", color: Color(red: 1, green: 1, blue: 0), index: 2),
        HSLChannel(id: 3, name: "绿", color: Color(red: 0, green: 0.8, blue: 0.2), index: 3),
        HSLChannel(id: 4, name: "青", color: Color(red: 0, green: 0.8, blue: 0.8), index: 4),
        HSLChannel(id: 5, name: "蓝", color: Color(red: 0.1, green: 0.3, blue: 1), index: 5),
        HSLChannel(id: 6, name: "紫", color: Color(red: 0.6, green: 0.1, blue: 1), index: 6),
        HSLChannel(id: 7, name: "洋红", color: Color(red: 1, green: 0.1, blue: 0.6), index: 7)
    ]
}

struct HSLAdjustView: View {
    @ObservedObject var viewModel: PhotoEditorViewModel

    @State private var selectedChannel: HSLChannel = HSLChannel.channels[0]
    @State private var hue: Float = 0
    @State private var saturation: Float = 0
    @State private var lightness: Float = 0

    @State private var allHue: [Float] = [0, 0, 0, 0, 0, 0, 0, 0]
    @State private var allSaturation: [Float] = [0, 0, 0, 0, 0, 0, 0, 0]
    @State private var allLightness: [Float] = [0, 0, 0, 0, 0, 0, 0, 0]

    var body: some View {
        VStack(spacing: 0) {
            // 颜色通道选择
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DesignSystem.Spacing.xSmall) {
                    ForEach(HSLChannel.channels) { channel in
                        Button {
                            selectChannel(channel)
                        } label: {
                            VStack(spacing: 6) {
                                Circle()
                                    .fill(channel.color)
                                    .frame(width: 32, height: 32)
                                    .overlay(
                                        Circle()
                                            .stroke(
                                                selectedChannel.id == channel.id ? Color.white : Color.clear,
                                                lineWidth: 2
                                            )
                                    )
                                    .shadow(color: channel.color.opacity(0.3), radius: 4)

                                Text(channel.name)
                                    .font(DesignSystem.Typography.caption2)
                                    .foregroundColor(selectedChannel.id == channel.id ? .white : DesignSystem.Colors.minimalSecondaryLabel)
                            }
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
                hslSlider(
                    label: "色相",
                    value: $hue,
                    range: -0.5...0.5,
                    tintColor: selectedChannel.color,
                    format: "%.2f",
                    onChanged: { v in
                        updateChannelValue(hue: v)
                        applyToEditor()
                    }
                )

                hslSlider(
                    label: "饱和度",
                    value: $saturation,
                    range: -1...1,
                    tintColor: selectedChannel.color,
                    format: "%.2f",
                    onChanged: { v in
                        updateChannelValue(saturation: v)
                        applyToEditor()
                    }
                )

                hslSlider(
                    label: "明度",
                    value: $lightness,
                    range: -1...1,
                    tintColor: selectedChannel.color,
                    format: "%.2f",
                    onChanged: { v in
                        updateChannelValue(lightness: v)
                        applyToEditor()
                    }
                )
            }
            .padding(.horizontal, DesignSystem.Spacing.small)
            .padding(.vertical, DesignSystem.Spacing.medium)

            Spacer()

            // 重置按钮
            Button {
                withAnimation(DesignSystem.Animation.quick) {
                    resetAll()
                }
            } label: {
                HStack {
                    Image(systemName: "arrow.counterclockwise")
                    Text("重置所有 HSL")
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

    // MARK: - 滑块

    private func hslSlider(
        label: String,
        value: Binding<Float>,
        range: ClosedRange<Float>,
        tintColor: Color,
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
                .tint(tintColor)
                .onChange(of: value.wrappedValue) { newValue in
                    onChanged(newValue)
                }
        }
    }

    // MARK: - 数据管理

    private func selectChannel(_ channel: HSLChannel) {
        selectedChannel = channel
        hue = allHue[channel.index]
        saturation = allSaturation[channel.index]
        lightness = allLightness[channel.index]
    }

    private func updateChannelValue(hue: Float? = nil, saturation: Float? = nil, lightness: Float? = nil) {
        let idx = selectedChannel.index
        if let h = hue { allHue[idx] = h }
        if let s = saturation { allSaturation[idx] = s }
        if let l = lightness { allLightness[idx] = l }
    }

    private func applyToEditor() {
        viewModel.applyHSL(hue: allHue, saturation: allSaturation, lightness: allLightness)
    }

    private func syncFromEditor() {
        allHue = viewModel.editor.hslHue
        allSaturation = viewModel.editor.hslSaturation
        allLightness = viewModel.editor.hslLightness
        // 确保数组长度正确
        if allHue.count < 8 { allHue = [0, 0, 0, 0, 0, 0, 0, 0] }
        if allSaturation.count < 8 { allSaturation = [0, 0, 0, 0, 0, 0, 0, 0] }
        if allLightness.count < 8 { allLightness = [0, 0, 0, 0, 0, 0, 0, 0] }
        selectChannel(selectedChannel)
    }

    private func resetAll() {
        allHue = [0, 0, 0, 0, 0, 0, 0, 0]
        allSaturation = [0, 0, 0, 0, 0, 0, 0, 0]
        allLightness = [0, 0, 0, 0, 0, 0, 0, 0]
        hue = 0
        saturation = 0
        lightness = 0
        applyToEditor()
    }
}

#endif