//
//  Card.swift
//  LiveCapture
//
//  统一卡片组件 - 魅族极简风格
//

import SwiftUI

#if os(iOS)

// MARK: - 卡片组件

struct Card<Content: View>: View {
    let content: Content
    var cornerRadius: CGFloat = DesignSystem.CornerRadius.large
    var padding: CGFloat = 16
    var hasShadow: Bool = true
    var hasBorder: Bool = true
    var onTap: (() -> Void)?

    init(
        cornerRadius: CGFloat = DesignSystem.CornerRadius.large,
        padding: CGFloat = 16,
        hasShadow: Bool = true,
        hasBorder: Bool = true,
        onTap: (() -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.padding = padding
        self.hasShadow = hasShadow
        self.hasBorder = hasBorder
        self.onTap = onTap
        self.content = content()
    }

    var body: some View {
        Group {
            if let onTap = onTap {
                Button(action: onTap) {
                    cardContent
                }
                .buttonStyle(.plain)
            } else {
                cardContent
            }
        }
    }

    private var cardContent: some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
            .overlay(
                Group {
                    if hasBorder {
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .strokeBorder(DesignSystem.Colors.gray3, lineWidth: 0.5)
                    }
                }
            )
            .if(hasShadow) { view in
                view.subtleShadow()
            }
    }
}

// MARK: - 选择卡片

struct SelectableCard<Content: View>: View {
    let content: Content
    let isSelected: Bool
    let onTap: () -> Void

    init(isSelected: Bool, onTap: @escaping () -> Void, @ViewBuilder content: () -> Content) {
        self.isSelected = isSelected
        self.onTap = onTap
        self.content = content()
    }

    var body: some View {
        Button(action: {
            HapticManager.shared.light()
            onTap()
        }) {
            content
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                        .fill(isSelected ? DesignSystem.Colors.primary.opacity(0.06) : DesignSystem.Colors.backgroundSecondary)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                        .strokeBorder(
                            isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.gray3,
                            lineWidth: isSelected ? 1.5 : 0.5
                        )
                )
                .if(isSelected) { view in
                    view.glow(color: DesignSystem.Colors.primary.opacity(0.15), radius: 8)
                }
        }
        .buttonStyle(.plain)
        .animation(DesignSystem.Animation.smooth, value: isSelected)
    }
}

// MARK: - 图片卡片

struct ImageCard: View {
    let image: Image?
    let title: String
    let subtitle: String?
    let aspectRatio: CGFloat
    let onTap: (() -> Void)?

    init(
        image: Image?,
        title: String,
        subtitle: String? = nil,
        aspectRatio: CGFloat = 1.0,
        onTap: (() -> Void)? = nil
    ) {
        self.image = image
        self.title = title
        self.subtitle = subtitle
        self.aspectRatio = aspectRatio
        self.onTap = onTap
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // 图片区域
            GeometryReader { geo in
                if let image = image {
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: geo.size.width, height: geo.size.width / aspectRatio)
                        .clipped()
                } else {
                    ZStack {
                        Color(uiColor: .systemGray5)
                        Image(systemName: "photo")
                            .font(.title2)
                            .foregroundColor(.secondary)
                    }
                    .frame(width: geo.size.width, height: geo.size.width / aspectRatio)
                }
            }
            .aspectRatio(aspectRatio, contentMode: .fit)

            // 文字区域
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(DesignSystem.Typography.headline)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                    .lineLimit(1)
                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(DesignSystem.Typography.caption1)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                        .lineLimit(1)
                }
            }
            .padding(DesignSystem.Spacing.xxSmall)
        }
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                .strokeBorder(DesignSystem.Colors.gray3, lineWidth: 0.5)
        )
        .subtleShadow()
        .onTapGesture {
            HapticManager.shared.light()
            onTap?()
        }
    }
}

// MARK: - 条件修饰符

extension View {
    @ViewBuilder
    func `if`<Content: View>(_ condition: Bool, transform: (Self) -> Content) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}

#endif