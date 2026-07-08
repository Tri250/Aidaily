//
//  AccessibilityManager.swift
//  LiveCapture
//
//  集中式无障碍配置管理器
//  动态字体缩放、减少动态效果、高对比度模式、VoiceOver 播报
//

import SwiftUI
import UIKit

#if os(iOS)

/// 集中式无障碍管理器
/// 统一管理所有无障碍相关的配置和状态
final class AccessibilityManager: ObservableObject {
    static let shared = AccessibilityManager()

    // MARK: - Published Properties

    /// 是否启用高对比度模式
    @Published var isHighContrastEnabled: Bool = false
    /// 是否启用减少动态效果
    @Published var isReduceMotionEnabled: Bool = false
    /// 是否启用大字体模式
    @Published var isLargeTextEnabled: Bool = false
    /// 当前字体缩放比例（1.0 为默认）
    @Published var fontScale: CGFloat = 1.0
    /// 是否启用粗体文本
    @Published var isBoldTextEnabled: Bool = false

    // MARK: - Computed Properties

    /// VoiceOver 是否运行中
    var isVoiceOverRunning: Bool {
        UIAccessibility.isVoiceOverRunning
    }

    /// 是否启用了任意辅助功能
    var isAnyAccessibilityEnabled: Bool {
        UIAccessibility.isVoiceOverRunning
            || UIAccessibility.isSwitchControlRunning
            || UIAccessibility.isSpeakScreenEnabled
            || UIAccessibility.isSpeakSelectionEnabled
            || UIAccessibility.isGuidedAccessEnabled
    }

    /// 是否应该减少动态效果
    var shouldReduceMotion: Bool {
        isReduceMotionEnabled || UIAccessibility.isReduceMotionEnabled
    }

    /// 是否应该使用高对比度
    var shouldUseHighContrast: Bool {
        isHighContrastEnabled || UIAccessibility.isDarkerSystemColorsEnabled
    }

    // MARK: - Animation Durations

    /// 根据无障碍设置调整动画时长
    var animationDuration: Double {
        shouldReduceMotion ? 0.0 : 0.3
    }

    /// 弹簧动画响应时间
    var springResponse: Double {
        shouldReduceMotion ? 0.0 : 0.35
    }

    // MARK: - Initialization

    private init() {
        syncWithSystem()
        setupObservers()
    }

    // MARK: - System Sync

    /// 同步系统无障碍设置
    func syncWithSystem() {
        isReduceMotionEnabled = UIAccessibility.isReduceMotionEnabled
        isHighContrastEnabled = UIAccessibility.isDarkerSystemColorsEnabled
        isBoldTextEnabled = UIAccessibility.isBoldTextEnabled

        // 根据系统字体大小类别计算缩放比例
        let contentSizeCategory = UIApplication.shared.preferredContentSizeCategory
        fontScale = fontScaleForCategory(contentSizeCategory)
        isLargeTextEnabled = contentSizeCategory >= .accessibilityMedium
    }

    /// 根据内容大小类别返回字体缩放比例
    func fontScaleForCategory(_ category: UIContentSizeCategory) -> CGFloat {
        switch category {
        case .extraSmall: return 0.8
        case .small: return 0.85
        case .medium: return 0.9
        case .large: return 1.0        // 默认
        case .extraLarge: return 1.1
        case .extraExtraLarge: return 1.2
        case .extraExtraExtraLarge: return 1.3
        case .accessibilityMedium: return 1.4
        case .accessibilityLarge: return 1.55
        case .accessibilityExtraLarge: return 1.7
        case .accessibilityExtraExtraLarge: return 1.85
        case .accessibilityExtraExtraExtraLarge: return 2.0
        default: return 1.0
        }
    }

    /// 缩放字体大小
    func scaledFontSize(_ baseSize: CGFloat) -> CGFloat {
        baseSize * fontScale
    }

    /// 根据无障碍设置返回字体
    func scaledFont(size: CGFloat, weight: Font.Weight = .regular, design: Font.Design = .default) -> Font {
        let scaledSize = scaledFontSize(size)
        return Font.system(size: scaledSize, weight: isBoldTextEnabled ? .bold : weight, design: design)
    }

    // MARK: - Announcements

    /// 通过 VoiceOver 播报消息
    func announce(_ message: String, delay: TimeInterval = 0.5) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            UIAccessibility.post(notification: .announcement, argument: message)
        }
    }

    /// 播报屏幕变化
    func announceScreenChange(_ message: String) {
        DispatchQueue.main.async {
            UIAccessibility.post(notification: .screenChanged, argument: message)
        }
    }

    /// 播报布局变化
    func announceLayoutChange(_ message: String) {
        DispatchQueue.main.async {
            UIAccessibility.post(notification: .layoutChanged, argument: message)
        }
    }

    /// 播报按钮状态变化
    func announceStateChange(_ state: String, for element: String) {
        announce("\(element)：\(state)")
    }

    // MARK: - Private

    private func setupObservers() {
        NotificationCenter.default.addObserver(
            forName: UIContentSizeCategory.didChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.syncWithSystem()
        }

        NotificationCenter.default.addObserver(
            forName: UIAccessibility.voiceOverStatusDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.objectWillChange.send()
        }
    }
}

// MARK: - View Extensions for Accessibility

extension View {

    /// 应用动态字体缩放
    func accessibilityScaledFont(size: CGFloat, weight: Font.Weight = .regular, design: Font.Design = .default) -> some View {
        self.font(AccessibilityManager.shared.scaledFont(size: size, weight: weight, design: design))
    }

    /// 条件性禁用动画（根据无障碍设置）
    func accessibilityReduceMotion() -> some View {
        self.animation(AccessibilityManager.shared.shouldReduceMotion ? .none : .default, value: UUID())
    }

    /// 应用高对比度适配
    func accessibilityHighContrast() -> some View {
        self.environment(\.colorScheme, AccessibilityManager.shared.shouldUseHighContrast ? .dark : .light)
    }

    /// 标准无障碍按钮配置
    func accessibilityButton(label: String, hint: String = "") -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityHint(hint)
            .accessibilityAddTraits(.isButton)
    }

    /// 标准无障碍图像配置
    func accessibilityImage(label: String) -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityAddTraits(.isImage)
    }

    /// 标准无障碍标题配置
    func accessibilityHeading() -> some View {
        self.accessibilityAddTraits(.isHeader)
    }

    /// 标准无障碍滑块配置
    func accessibilitySlider(label: String, value: String, hint: String = "") -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityValue(value)
            .accessibilityHint(hint)
            .accessibilityAddTraits(.isAdjustable)
    }

    /// 标准无障碍开关配置
    func accessibilityToggle(label: String, isOn: Bool, hint: String = "") -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityValue(isOn ? "已开启" : "已关闭")
            .accessibilityHint(hint)
            .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}

// MARK: - Accessibility Preview Helper

/// 无障碍预览辅助：在 Xcode 预览中模拟无障碍模式
struct AccessibilityPreviewModifier: ViewModifier {
    let isVoiceOver: Bool
    let isHighContrast: Bool
    let isReduceMotion: Bool
    let scaleFactor: CGFloat

    func body(content: Content) -> some View {
        content
            .environment(\.dynamicTypeSize, DynamicTypeSize(scaleFactor))
            .environment(\.colorScheme, isHighContrast ? .dark : .light)
    }
}

extension DynamicTypeSize {
    init(_ scale: CGFloat) {
        switch scale {
        case ..<0.85: self = .xSmall
        case 0.85..<0.95: self = .small
        case 0.95..<1.05: self = .large
        case 1.05..<1.2: self = .xLarge
        case 1.2..<1.4: self = .xxLarge
        case 1.4..<1.55: self = .xxxLarge
        case 1.55..<1.7: self = .accessibility1
        case 1.7..<1.85: self = .accessibility2
        case 1.85..<2.0: self = .accessibility3
        case 2.0...: self = .accessibility4
        default: self = .large
        }
    }
}

#endif