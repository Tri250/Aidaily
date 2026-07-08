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

// MARK: - 照片网格骨架屏

struct PhotoGridSkeletonView: View {
    let columns: Int
    let rows: Int
    let spacing: CGFloat

    init(columns: Int = 3, rows: Int = 4, spacing: CGFloat = 2) {
        self.columns = columns
        self.rows = rows
        self.spacing = spacing
    }

    var body: some View {
        VStack(spacing: spacing) {
            ForEach(0..<rows, id: \.self) { _ in
                HStack(spacing: spacing) {
                    ForEach(0..<columns, id: \.self) { _ in
                        RoundedRectangle(cornerRadius: 0)
                            .fill(DesignSystem.Colors.gray2)
                            .aspectRatio(1, contentMode: .fit)
                            .shimmer()
                    }
                }
            }
        }
    }
}

// MARK: - 列表项骨架屏

struct ListItemSkeletonView: View {
    var body: some View {
        HStack(spacing: DesignSystem.Spacing.small) {
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                .fill(DesignSystem.Colors.gray2)
                .frame(width: 56, height: 56)
                .shimmer()

            VStack(alignment: .leading, spacing: 6) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(DesignSystem.Colors.gray2)
                    .frame(width: 120, height: 14)
                    .shimmer()
                RoundedRectangle(cornerRadius: 4)
                    .fill(DesignSystem.Colors.gray2)
                    .frame(width: 80, height: 12)
                    .shimmer()
            }
        }
        .padding(.horizontal, DesignSystem.Spacing.Padding.container)
        .padding(.vertical, DesignSystem.Spacing.xxSmall)
    }
}

// MARK: - 叠加加载指示器

struct OverlayLoadingView: View {
    let message: String

    init(_ message: String = "处理中...") {
        self.message = message
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()

            VStack(spacing: DesignSystem.Spacing.small) {
                ProgressView()
                    .scaleEffect(1.5)
                    .tint(.white)

                Text(message)
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(.white)
            }
            .padding(DesignSystem.Spacing.large)
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                    .fill(Color.black.opacity(0.75))
            )
        }
    }
}

// MARK: - 图片处理进度指示器

struct ImageProcessingProgressView: View {
    let progress: Float
    let stage: ImageProcessingStage

    enum ImageProcessingStage: String {
        case loading = "加载图片"
        case analyzing = "分析图像"
        case applyingFilter = "应用滤镜"
        case enhancing = "增强处理"
        case exporting = "导出中"

        var icon: String {
            switch self {
            case .loading: return "photo"
            case .analyzing: return "camera.metering.center.weighted"
            case .applyingFilter: return "camera.filters"
            case .enhancing: return "wand.and.stars"
            case .exporting: return "square.and.arrow.up"
            }
        }
    }

    var body: some View {
        VStack(spacing: DesignSystem.Spacing.medium) {
            // 图标
            ZStack {
                Circle()
                    .fill(DesignSystem.Colors.primary.opacity(0.1))
                    .frame(width: 72, height: 72)

                Image(systemName: stage.icon)
                    .font(.system(size: 28, weight: .medium))
                    .foregroundColor(DesignSystem.Colors.primary)
            }

            // 阶段文字
            Text(stage.rawValue)
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            // 进度条
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(DesignSystem.Colors.gray2)
                        .frame(height: 6)

                    RoundedRectangle(cornerRadius: 4)
                        .fill(DesignSystem.Colors.primary)
                        .frame(width: max(geo.size.width * CGFloat(progress), 6), height: 6)
                        .animation(DesignSystem.Animation.smooth, value: progress)
                }
            }
            .frame(height: 6)
            .padding(.horizontal, 40)

            // 百分比
            Text("\(Int(progress * 100))%")
                .font(DesignSystem.Typography.monoCaption)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .padding(DesignSystem.Spacing.large)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
        .subtleShadow()
        .padding(.horizontal, 40)
    }
}

// MARK: - 操作反馈 Toast 叠加层

struct ToastOverlay: View {
    @ObservedObject var toastManager = ToastManager.shared

    var body: some View {
        Group {
            if toastManager.showToast {
                VStack {
                    Spacer()
                    ToastView(
                        message: toastManager.toastMessage,
                        icon: toastManager.toastIcon,
                        style: toastManager.toastStyle
                    )
                    .padding(.bottom, 40)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .animation(DesignSystem.Animation.bouncy, value: toastManager.showToast)
            }
        }
    }
}

// MARK: - View 扩展：加载状态

extension View {

    /// 骨架屏加载修饰器
    func skeletonLoading(_ isLoading: Bool) -> some View {
        self.overlay(
            Group {
                if isLoading {
                    SkeletonView()
                }
            }
        )
    }

    /// 照片网格骨架屏加载修饰器
    func photoGridSkeleton(_ isLoading: Bool, columns: Int = 3, rows: Int = 4) -> some View {
        self.overlay(
            Group {
                if isLoading {
                    PhotoGridSkeletonView(columns: columns, rows: rows)
                }
            }
        )
    }

    /// 叠加加载指示器
    func overlayLoading(_ isLoading: Bool, message: String = "处理中...") -> some View {
        self.overlay(
            Group {
                if isLoading {
                    OverlayLoadingView(message)
                }
            }
            .animation(DesignSystem.Animation.overlayFade, value: isLoading)
        )
    }

    /// Toast 消息叠加层
    func toastOverlay() -> some View {
        self.overlay(ToastOverlay())
    }
}

// MARK: - 便捷 Toast 调用

func showSuccessToast(_ message: String) {
    ToastManager.shared.success(message)
}

func showErrorToast(_ message: String) {
    ToastManager.shared.error(message)
}

func showWarningToast(_ message: String) {
    ToastManager.shared.warning(message)
}

func showInfoToast(_ message: String) {
    ToastManager.shared.info(message)
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