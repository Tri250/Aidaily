//
//  PortraitModeView.swift
//  LiveCapture
//
//  人像模式覆盖层 - 虚化与光效控制
//

import SwiftUI

#if os(iOS)

/// 人像模式覆盖层
struct PortraitModeView: View {
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
                Text("人像模式")
                    .font(DesignSystem.Typography.title3)
                    .foregroundColor(DesignSystem.Colors.minimalLabel)

                Spacer()

                // 人像模式开关
                Toggle("", isOn: $viewModel.isPortraitModeEnabled)
                    .labelsHidden()
                    .toggleStyle(SwitchToggleStyle(tint: DesignSystem.Colors.primary))
                    .accessibilityLabel("人像模式开关")
                    .accessibilityHint("双击开启或关闭人像模式")

                // 关闭按钮
                Button {
                    HapticManager.shared.light()
                    onClose()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)

            ScrollView {
                VStack(spacing: 20) {
                    // 虚化强度滑块
                    blurSection

                    Divider()
                        .background(DesignSystem.Colors.minimalBorder)
                        .padding(.horizontal, 20)

                    // 光效选择器
                    lightingSection
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

    // MARK: - Blur Section

    private var blurSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "camera.aperture")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)

                Text("背景虚化")
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.minimalLabel)

                Spacer()

                Text("\(Int(viewModel.portraitBlur * 100))%")
                    .font(DesignSystem.Typography.monoDigit)
                    .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
            }

            // 虚化预览
            blurPreview

            Slider(value: $viewModel.portraitBlur, in: 0...1)
                .tint(DesignSystem.Colors.primary)
                .padding(.horizontal, 4)
        }
        .padding(.horizontal, 20)
    }

    /// 虚化效果预览
    private var blurPreview: some View {
        GeometryReader { geo in
            ZStack {
                // 背景模糊区域
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.white.opacity(0.05))

                // 模拟人像区域
                VStack(spacing: 4) {
                    Circle()
                        .fill(Color.white.opacity(0.2))
                        .frame(width: 32, height: 32)

                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color.white.opacity(0.15))
                        .frame(width: 24, height: 12)
                }
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.white.opacity(0.08))
                        .frame(
                            width: geo.size.width * 0.35,
                            height: geo.size.height * 0.7
                        )
                        .blur(radius: 0)
                )

                // 模糊叠加层
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.black.opacity(0))
                    .overlay(
                        Rectangle()
                            .fill(Color.black.opacity(0))
                            .mask(
                                // 中心区域保持清晰，其余模糊
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.white)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .frame(
                                                width: geo.size.width * 0.35,
                                                height: geo.size.height * 0.7
                                            )
                                            .blendMode(.destinationOut)
                                    )
                            )
                    )
                    .blur(radius: CGFloat(viewModel.portraitBlur * 15))
            }
            .frame(height: 60)
        }
        .frame(height: 60)
    }

    // MARK: - Lighting Section

    private var lightingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "lightbulb")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)

                Text("光效")
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.minimalLabel)
            }

            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 10),
                GridItem(.flexible(), spacing: 10),
                GridItem(.flexible(), spacing: 10)
            ], spacing: 10) {
                ForEach(PortraitLightingType.allCases, id: \.self) { lighting in
                    lightingButton(lighting)
                }
            }
        }
        .padding(.horizontal, 20)
    }

    private func lightingButton(_ lighting: PortraitLightingType) -> some View {
        Button {
            HapticManager.shared.selection()
            viewModel.selectLighting(lighting)
        } label: {
            VStack(spacing: 6) {
                ZStack {
                    Circle()
                        .fill(
                            viewModel.lightingType == lighting
                                ? DesignSystem.Colors.primary.opacity(0.25)
                                : Color.white.opacity(0.08)
                        )
                        .frame(width: 48, height: 48)

                    Image(systemName: lighting.iconName)
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(
                            viewModel.lightingType == lighting
                                ? DesignSystem.Colors.primary
                                : DesignSystem.Colors.minimalSecondaryLabel
                        )
                }

                Text(lighting.displayName)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(
                        viewModel.lightingType == lighting
                            ? DesignSystem.Colors.minimalLabel
                            : DesignSystem.Colors.minimalSecondaryLabel
                    )
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            .frame(maxWidth: .infinity)
        }
    }
}

#endif