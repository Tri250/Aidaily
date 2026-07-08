//
//  VignetteEditorView.swift
//  LiveCapture
//
//  暗角编辑器 - 强度、范围、羽化、中心位置
//

import SwiftUI

#if os(iOS)

struct VignetteEditorView: View {
    @ObservedObject var viewModel: PhotoEditorViewModel

    @State private var intensity: Float = 0
    @State private var radius: Float = 0.5
    @State private var feather: Float = 0.5
    @State private var isElliptical: Bool = false
    @State private var centerX: Float = 0.5
    @State private var centerY: Float = 0.5

    var body: some View {
        VStack(spacing: 0) {
            // 预览区域
            vignettePreview
                .frame(height: 180)
                .padding(DesignSystem.Spacing.small)

            Divider()
                .background(DesignSystem.Colors.minimalBorder)

            // 滑块
            VStack(spacing: DesignSystem.Spacing.medium) {
                vignetteSlider(
                    label: "强度",
                    value: $intensity,
                    range: 0...1,
                    onChanged: { update() }
                )

                vignetteSlider(
                    label: "范围",
                    value: $radius,
                    range: 0...1,
                    onChanged: { update() }
                )

                vignetteSlider(
                    label: "羽化",
                    value: $feather,
                    range: 0...1,
                    onChanged: { update() }
                )
            }
            .padding(.horizontal, DesignSystem.Spacing.small)
            .padding(.vertical, DesignSystem.Spacing.medium)

            // 形状切换
            HStack(spacing: DesignSystem.Spacing.medium) {
                Button {
                    withAnimation(DesignSystem.Animation.quick) {
                        isElliptical = false
                        update()
                    }
                } label: {
                    VStack(spacing: 4) {
                        Circle()
                            .strokeBorder(isElliptical ? DesignSystem.Colors.minimalBorder : DesignSystem.Colors.primary, lineWidth: 2)
                            .frame(width: 32, height: 32)
                        Text("圆形")
                            .font(DesignSystem.Typography.caption2)
                    }
                    .foregroundColor(isElliptical ? DesignSystem.Colors.minimalSecondaryLabel : .white)
                }

                Button {
                    withAnimation(DesignSystem.Animation.quick) {
                        isElliptical = true
                        update()
                    }
                } label: {
                    VStack(spacing: 4) {
                        Ellipse()
                            .strokeBorder(isElliptical ? DesignSystem.Colors.primary : DesignSystem.Colors.minimalBorder, lineWidth: 2)
                            .frame(width: 32, height: 22)
                        Text("椭圆")
                            .font(DesignSystem.Typography.caption2)
                    }
                    .foregroundColor(isElliptical ? .white : DesignSystem.Colors.minimalSecondaryLabel)
                }
            }
            .padding(.bottom, DesignSystem.Spacing.small)

            Spacer()

            // 重置
            Button {
                withAnimation(DesignSystem.Animation.quick) {
                    intensity = 0
                    radius = 0.5
                    feather = 0.5
                    isElliptical = false
                    centerX = 0.5
                    centerY = 0.5
                    update()
                }
            } label: {
                HStack {
                    Image(systemName: "arrow.counterclockwise")
                    Text("重置暗角")
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

    // MARK: - 暗角预览

    private var vignettePreview: some View {
        GeometryReader { geo in
            ZStack {
                // 背景渐变模拟暗角
                LinearGradient(
                    gradient: Gradient(colors: [Color.gray.opacity(0.8), Color.gray.opacity(0.3)]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )

                // 暗角覆盖
                vignetteOverlay(size: geo.size)
            }
            .clipShape(RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium))
        }
    }

    private func vignetteOverlay(size: CGSize) -> some View {
        Canvas { context, _ in
            let cx = CGFloat(centerX) * size.width
            let cy = CGFloat(centerY) * size.height
            let maxDim = max(size.width, size.height)
            let outerRadius = maxDim * 0.8
            let innerRadius = outerRadius * CGFloat(radius)

            let featherWidth = CGFloat(feather) * outerRadius * 0.5

            // 绘制径向渐变暗角
            for i in 0..<100 {
                let t = CGFloat(i) / 100.0
                let r = innerRadius + featherWidth * t
                let opacity = CGFloat(intensity) * t * 0.7

                if isElliptical {
                    let ellipse = Path(ellipseIn: CGRect(
                        x: cx - r,
                        y: cy - r * 0.7,
                        width: r * 2,
                        height: r * 1.4
                    ))
                    context.stroke(ellipse, with: .color(.black.opacity(opacity)), lineWidth: 1)
                } else {
                    let circle = Path(ellipseIn: CGRect(
                        x: cx - r,
                        y: cy - r,
                        width: r * 2,
                        height: r * 2
                    ))
                    context.stroke(circle, with: .color(.black.opacity(opacity)), lineWidth: 1)
                }
            }
        }
    }

    // MARK: - 滑块

    private func vignetteSlider(
        label: String,
        value: Binding<Float>,
        range: ClosedRange<Float>,
        onChanged: @escaping () -> Void
    ) -> some View {
        VStack(spacing: DesignSystem.Spacing.xxSmall) {
            HStack {
                Text(label)
                    .font(DesignSystem.Typography.callout)
                    .foregroundColor(DesignSystem.Colors.minimalLabel)

                Spacer()

                Text(String(format: "%.2f", value.wrappedValue))
                    .font(DesignSystem.Typography.monoDigit)
                    .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                    .frame(width: 40, alignment: .trailing)
            }

            Slider(value: value, in: range, step: 0.01)
                .tint(DesignSystem.Colors.primary)
                .onChange(of: value.wrappedValue) { _ in
                    onChanged()
                }
        }
    }

    // MARK: - 数据同步

    private func update() {
        viewModel.applyVignette(intensity: intensity, radius: radius, feather: feather)
    }

    private func syncFromEditor() {
        intensity = viewModel.editor.vignetteIntensity
        radius = viewModel.editor.vignetteRadius
        feather = viewModel.editor.vignetteFeather
    }
}

#endif