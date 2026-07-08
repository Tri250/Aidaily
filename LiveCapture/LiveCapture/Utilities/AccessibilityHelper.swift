//
//  AccessibilityHelper.swift
//  LiveCapture
//
//  无障碍适配工具类
//
//  ## 文件作用
//  提供统一的无障碍适配接口
//  封装 SwiftUI 的 Accessibility API
//  为 VoiceOver 用户提供完整的操作体验
//
//  ## 主要组件
//  ### AccessibilityHelper
//  无障碍工具类，提供 VoiceOver 播报功能
//
//  ### View 扩展
//  - accessibilityLabel(_:): 设置无障碍标签
//  - accessibilityHint(_:): 设置无障碍提示
//  - accessibilityAnnouncement(_:): VoiceOver 语音播报
//
//  ## 使用方式
//  ```swift
//  // 设置标签和提示
//  Button("拍照") { ... }
//      .accessibilityLabel("拍照按钮")
//      .accessibilityHint("双击拍摄照片")
//
//  // 动态播报
//  AccessibilityHelper.announce("照片已保存")
//  ```
//

import SwiftUI

/// 无障碍辅助工具类
enum AccessibilityHelper {

    /// 通过 VoiceOver 播报消息
    /// - Parameters:
    ///   - message: 要播报的消息内容
    ///   - delay: 延迟播报时间（秒），默认 0.5 秒
    static func announce(_ message: String, delay: TimeInterval = 0.5) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            UIAccessibility.post(notification: .announcement, argument: message)
        }
    }

    /// 播报屏幕变化（用于页面切换）
    /// - Parameter message: 屏幕描述
    static func announceScreenChange(_ message: String) {
        DispatchQueue.main.async {
            UIAccessibility.post(notification: .screenChanged, argument: message)
        }
    }

    /// 播报布局变化（用于动态内容更新）
    /// - Parameter message: 布局变化描述
    static func announceLayoutChange(_ message: String) {
        DispatchQueue.main.async {
            UIAccessibility.post(notification: .layoutChanged, argument: message)
        }
    }

    /// 检查 VoiceOver 是否正在运行
    static var isVoiceOverRunning: Bool {
        UIAccessibility.isVoiceOverRunning
    }

    /// 检查是否启用了辅助功能（任意类型）
    static var isAccessibilityEnabled: Bool {
        UIAccessibility.isVoiceOverRunning
            || UIAccessibility.isSwitchControlRunning
            || UIAccessibility.isSpeakScreenEnabled
            || UIAccessibility.isSpeakSelectionEnabled
    }
}

// MARK: - View 无障碍修饰符扩展

extension View {

    /// 设置无障碍标签
    /// - Parameter label: 描述元素的简短文本
    /// - Returns: 设置了 accessibilityLabel 的视图
    func accessibilityLabel(_ label: String) -> some View {
        self.accessibility(label: Text(label))
    }

    /// 设置无障碍提示
    /// - Parameter hint: 描述操作结果的提示文本
    /// - Returns: 设置了 accessibilityHint 的视图
    func accessibilityHint(_ hint: String) -> some View {
        self.accessibility(hint: Text(hint))
    }

    /// 将视图标记为无障碍元素并配置标签和提示
    /// - Parameters:
    ///   - label: 无障碍标签
    ///   - hint: 无障碍提示
    ///   - isButton: 是否作为按钮处理
    /// - Returns: 配置了无障碍属性的视图
    func accessibilityElement(label: String, hint: String = "", isButton: Bool = false) -> some View {
        self
            .accessibility(label: Text(label))
            .accessibility(hint: Text(hint))
            .accessibility(addTraits: isButton ? .isButton : [])
    }

    /// 添加无障碍操作
    /// - Parameters:
    ///   - name: 操作名称
    ///   - action: 操作闭包
    /// - Returns: 添加了无障碍操作的视图
    func accessibilityAction(_ name: String, action: @escaping () -> Void) -> some View {
        self.accessibilityAction(named: Text(name), action)
    }

    /// 为视图设置无障碍值描述
    /// - Parameter value: 值描述文本
    /// - Returns: 设置了 accessibilityValue 的视图
    func accessibilityValue(_ value: String) -> some View {
        self.accessibility(value: Text(value))
    }

    /// 配置完整的无障碍信息
    /// - Parameters:
    ///   - label: 无障碍标签
    ///   - value: 当前值描述
    ///   - hint: 操作提示
    ///   - traits: 无障碍特征
    /// - Returns: 配置完成的视图
    func accessibilityConfigure(
        label: String,
        value: String? = nil,
        hint: String = "",
        traits: AccessibilityTraits = []
    ) -> some View {
        self
            .accessibility(label: Text(label))
            .accessibility(hint: Text(hint))
            .accessibility(value: value.map { Text($0) } ?? Text(""))
            .accessibility(addTraits: traits)
    }
}