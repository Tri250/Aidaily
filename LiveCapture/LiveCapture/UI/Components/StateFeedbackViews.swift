//
//  StateFeedbackViews.swift
//  LiveCapture
//
//  状态反馈组件：空状态、错误状态、加载骨架屏、Toast
//  魅族极简风格 - 精致插画 + 引导操作
//

import SwiftUI

#if os(iOS)

// MARK: - 空状态视图

struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    let actionTitle: String?
    let action: (() -> Void)?

    init(
        icon: String,
        title: String,
        message: String,
        actionTitle: String? = nil,
        action: (() -> Void)? = nil
    ) {
        self.icon = icon
        self.title = title
        self.message = message
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        VStack(spacing: DesignSystem.Spacing.large) {
            Spacer()

            // 图标 - 精致设计
            ZStack {
                Circle()
                    .fill(DesignSystem.Colors.primary.opacity(0.08))
                    .frame(width: 88, height: 88)

                Image(systemName: icon)
                    .font(.system(size: 36, weight: .light))
                    .foregroundColor(DesignSystem.Colors.primary.opacity(0.6))
            }

            // 文字
            VStack(spacing: DesignSystem.Spacing.xxSmall) {
                Text(title)
                    .font(DesignSystem.Typography.title3)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                    .multilineTextAlignment(.center)

                Text(message)
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.horizontal, 32)
            }

            // 操作按钮
            if let actionTitle = actionTitle, let action = action {
                Button(action: action) {
                    Text(actionTitle)
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                                .fill(DesignSystem.Colors.primary)
                        )
                }
                .padding(.top, DesignSystem.Spacing.xxSmall)
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignSystem.Colors.backgroundPrimary)
    }
}

// MARK: - 错误状态视图

struct ErrorStateView: View {
    let error: Error
    let retryAction: (() -> Void)?

    init(error: Error, retryAction: (() -> Void)? = nil) {
        self.error = error
        self.retryAction = retryAction
    }

    var body: some View {
        VStack(spacing: DesignSystem.Spacing.large) {
            Spacer()

            // 错误图标
            ZStack {
                Circle()
                    .fill(DesignSystem.Colors.errorBg)
                    .frame(width: 88, height: 88)

                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 36, weight: .light))
                    .foregroundColor(DesignSystem.Colors.error.opacity(0.7))
            }

            VStack(spacing: DesignSystem.Spacing.xxSmall) {
                Text("出错了")
                    .font(DesignSystem.Typography.title3)
                    .foregroundColor(DesignSystem.Colors.textPrimary)

                Text(error.localizedDescription)
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.horizontal, 32)
            }

            if let retry = retryAction {
                Button(action: retry) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 15))
                        Text("重试")
                            .font(DesignSystem.Typography.headline)
                    }
                    .foregroundColor(DesignSystem.Colors.primary)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                            .fill(DesignSystem.Colors.primary.opacity(0.08))
                    )
                }
                .padding(.top, DesignSystem.Spacing.xxSmall)
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignSystem.Colors.backgroundPrimary)
    }
}

// MARK: - 加载骨架屏

struct SkeletonView: View {
    var body: some View {
        VStack(spacing: DesignSystem.Spacing.small) {
            ForEach(0..<6, id: \.self) { _ in
                HStack(spacing: DesignSystem.Spacing.xxSmall) {
                    // 图片占位
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                        .fill(DesignSystem.Colors.gray2)
                        .frame(width: 80, height: 80)
                        .shimmer()

                    // 文字占位
                    VStack(alignment: .leading, spacing: 6) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(DesignSystem.Colors.gray2)
                            .frame(height: 14)
                            .shimmer()
                        RoundedRectangle(cornerRadius: 4)
                            .fill(DesignSystem.Colors.gray2)
                            .frame(width: 120, height: 12)
                            .shimmer()
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    Spacer()
                }
                .padding(.horizontal, DesignSystem.Spacing.Padding.container)
            }
        }
        .padding(.top, DesignSystem.Spacing.medium)
    }
}

// MARK: - 加载进度视图

struct LoadingProgressView: View {
    let message: String
    var progress: Float?

    init(message: String, progress: Float? = nil) {
        self.message = message
        self.progress = progress
    }

    var body: some View {
        VStack(spacing: DesignSystem.Spacing.small) {
            if let progress = progress {
                // 进度条模式
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(DesignSystem.Colors.gray2)
                            .frame(height: 4)

                        RoundedRectangle(cornerRadius: 2)
                            .fill(DesignSystem.Colors.primary)
                            .frame(width: geo.size.width * CGFloat(progress), height: 4)
                            .animation(DesignSystem.Animation.smooth, value: progress)
                    }
                }
                .frame(height: 4)
                .padding(.horizontal, 40)

                Text("\(Int(progress * 100))%")
                    .font(DesignSystem.Typography.monoCaption)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            } else {
                // 无限旋转模式
                ProgressView()
                    .scaleEffect(1.2)
                    .tint(DesignSystem.Colors.primary)
            }

            Text(message)
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .padding(DesignSystem.Spacing.xLarge)
    }
}

// MARK: - 现代 Toast 组件

struct ToastView: View {
    let message: String
    let icon: String
    let style: ToastStyle
    let duration: TimeInterval

    enum ToastStyle {
        case success, error, warning, info

        var color: Color {
            switch self {
            case .success: return DesignSystem.Colors.success
            case .error: return DesignSystem.Colors.error
            case .warning: return DesignSystem.Colors.warning
            case .info: return DesignSystem.Colors.info
            }
        }

        var bgColor: Color {
            switch self {
            case .success: return DesignSystem.Colors.successBg
            case .error: return DesignSystem.Colors.errorBg
            case .warning: return DesignSystem.Colors.warningBg
            case .info: return DesignSystem.Colors.primary.opacity(0.08)
            }
        }
    }

    @State private var isVisible = false

    init(
        message: String,
        icon: String,
        style: ToastStyle = .info,
        duration: TimeInterval = 2.0
    ) {
        self.message = message
        self.icon = icon
        self.style = style
        self.duration = duration
    }

    var body: some View {
        HStack(spacing: DesignSystem.Spacing.xxSmall) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(style.color)

            Text(message)
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textPrimary)
                .lineLimit(2)
        }
        .padding(.horizontal, DesignSystem.Spacing.small)
        .padding(.vertical, DesignSystem.Spacing.xxSmall)
        .background(
            Capsule()
                .fill(style.bgColor)
                .overlay(
                    Capsule()
                        .strokeBorder(style.color.opacity(0.2), lineWidth: 0.5)
                )
        )
        .opacity(isVisible ? 1 : 0)
        .offset(y: isVisible ? 0 : 10)
        .onAppear {
            withAnimation(DesignSystem.Animation.bouncy) {
                isVisible = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
                withAnimation(DesignSystem.Animation.easeOut) {
                    isVisible = false
                }
            }
        }
    }
}

// MARK: - Toast 管理器

final class ToastManager: ObservableObject {
    static let shared = ToastManager()

    @Published var showToast = false
    @Published var toastMessage = ""
    @Published var toastIcon = ""
    @Published var toastStyle: ToastView.ToastStyle = .info

    private var toastWorkItem: DispatchWorkItem?

    func show(_ message: String, icon: String, style: ToastView.ToastStyle = .info, duration: TimeInterval = 2.0) {
        toastWorkItem?.cancel()

        toastMessage = message
        toastIcon = icon
        toastStyle = style
        showToast = true

        HapticManager.shared.light()

        let workItem = DispatchWorkItem { [weak self] in
            withAnimation(DesignSystem.Animation.easeOut) {
                self?.showToast = false
            }
        }
        toastWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + duration, execute: workItem)
    }

    func success(_ message: String) {
        show(message, icon: "checkmark.circle.fill", style: .success)
    }

    func error(_ message: String) {
        show(message, icon: "xmark.circle.fill", style: .error)
    }

    func warning(_ message: String) {
        show(message, icon: "exclamationmark.triangle.fill", style: .warning)
    }

    func info(_ message: String) {
        show(message, icon: "info.circle.fill", style: .info)
    }
}

// MARK: - 操作确认弹窗

struct ConfirmationAlert: View {
    let title: String
    let message: String
    let confirmTitle: String
    let isDestructive: Bool
    let onConfirm: () -> Void
    let onCancel: () -> Void

    @State private var scale: CGFloat = 0.95
    @State private var opacity: Double = 0

    var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture { onCancel() }

            VStack(spacing: 0) {
                // 内容
                VStack(spacing: DesignSystem.Spacing.xxSmall) {
                    Text(title)
                        .font(DesignSystem.Typography.title3)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Text(message)
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                }
                .padding(DesignSystem.Spacing.large)

                Divider()

                // 按钮
                HStack(spacing: 0) {
                    Button(action: {
                        HapticManager.shared.light()
                        onCancel()
                    }) {
                        Text("取消")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textSecondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }

                    Divider()

                    Button(action: {
                        HapticManager.shared.medium()
                        onConfirm()
                    }) {
                        Text(confirmTitle)
                            .font(DesignSystem.Typography.headline)
                            .fontWeight(.semibold)
                            .foregroundColor(isDestructive ? DesignSystem.Colors.error : DesignSystem.Colors.primary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                }
            }
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.xxLarge)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
            .padding(.horizontal, 40)
            .scaleEffect(scale)
            .opacity(opacity)
        }
        .onAppear {
            withAnimation(DesignSystem.Animation.bouncy) {
                scale = 1.0
                opacity = 1.0
            }
        }
    }
}

#endif