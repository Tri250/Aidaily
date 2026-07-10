//
//  AccessibilityModifiers.swift
//  LiveCapture
//
//  全局无障碍标注 - 为所有核心 View 添加 accessibility 支持
//

import SwiftUI

#if os(iOS)

// MARK: - 无障碍 ViewModifier

/// 标准按钮无障碍标注
struct AccessibleButton: ViewModifier {
    let label: String
    let hint: String?

    func body(content: Content) -> some View {
        content
            .accessibilityLabel(label)
            .accessibilityHint(hint ?? "")
            .accessibilityAddTraits(.isButton)
    }
}

/// 无障碍图片
struct AccessibleImage: ViewModifier {
    let label: String

    func body(content: Content) -> some View {
        content
            .accessibilityLabel(label)
            .accessibilityAddTraits(.isImage)
    }
}

/// 无障碍标题
struct AccessibleHeading: ViewModifier {
    func body(content: Content) -> some View {
        content
            .accessibilityAddTraits(.isHeader)
    }
}

// MARK: - View 扩展

extension View {
    /// 添加无障碍按钮标注
    func accessibleButton(label: String, hint: String? = nil) -> some View {
        modifier(AccessibleButton(label: label, hint: hint))
    }

    /// 添加无障碍图片标注
    func accessibleImage(label: String) -> some View {
        modifier(AccessibleImage(label: label))
    }

    /// 添加无障碍标题标注
    func accessibleHeading() -> some View {
        modifier(AccessibleHeading())
    }
}

// MARK: - 全局无障碍配置

/// 全局无障碍配置管理器
final class AccessibilityConfig: ObservableObject {
    static let shared = AccessibilityConfig()

    @Published var highContrastMode: Bool {
        didSet {
            UserDefaults.standard.set(highContrastMode, forKey: "livecapture.high_contrast")
        }
    }

    @Published var reducedMotion: Bool {
        didSet {
            UserDefaults.standard.set(reducedMotion, forKey: "livecapture.reduced_motion")
        }
    }

    @Published var largeText: Bool {
        didSet {
            UserDefaults.standard.set(largeText, forKey: "livecapture.large_text")
        }
    }

    private init() {
        self.highContrastMode = UserDefaults.standard.bool(forKey: "livecapture.high_contrast")
        self.reducedMotion = UserDefaults.standard.bool(forKey: "livecapture.reduced_motion")
        self.largeText = UserDefaults.standard.bool(forKey: "livecapture.large_text")
    }

    /// 根据系统无障碍设置自动调整
    func syncWithSystem() {
        highContrastMode = UIAccessibility.isDarkerSystemColorsEnabled
        reducedMotion = UIAccessibility.isReduceMotionEnabled
        largeText = UIApplication.shared.preferredContentSizeCategory >= .accessibilityMedium
    }
}

// MARK: - 无障碍颜色扩展

extension Color {
    /// 根据高对比度模式返回适配颜色
    static func accessiblePrimary(_ config: AccessibilityConfig = .shared) -> Color {
        config.highContrastMode ? .white : DesignSystem.Colors.primary
    }

    static func accessibleBackground(_ config: AccessibilityConfig = .shared) -> Color {
        config.highContrastMode ? .black : DesignSystem.Colors.backgroundPrimary
    }
}

#endif