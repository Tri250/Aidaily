//
//  FeatureTipView.swift
//  LiveCapture
//
//  功能提示气泡系统 - 首次使用时弹出引导
//

import SwiftUI

#if os(iOS)

// MARK: - 功能提示数据模型

struct FeatureTip: Identifiable, Equatable {
    let id: String
    let title: String
    let description: String
    let icon: String
    let arrowDirection: ArrowDirection

    enum ArrowDirection {
        case top, bottom, left, right
    }

    static func == (lhs: FeatureTip, rhs: FeatureTip) -> Bool {
        lhs.id == rhs.id
    }

    // MARK: - 预设提示

    static let flashTip = FeatureTip(
        id: "flash",
        title: "闪光灯",
        description: "点击切换自动/开启/关闭闪光灯模式",
        icon: "bolt.fill",
        arrowDirection: .top
    )

    static let timerTip = FeatureTip(
        id: "timer",
        title: "定时拍摄",
        description: "设置 3/5/10 秒倒计时，轻松自拍",
        icon: "timer",
        arrowDirection: .top
    )

    static let filterStripTip = FeatureTip(
        id: "filter_strip",
        title: "滤镜选择",
        description: "左右滑动浏览 42+ 款滤镜，点击应用",
        icon: "camera.filters",
        arrowDirection: .bottom
    )

    static let beautyPanelTip = FeatureTip(
        id: "beauty_panel",
        title: "美颜调节",
        description: "点击打开美颜面板，调节磨皮、美白等参数",
        icon: "face.smiling",
        arrowDirection: .top
    )

    static let gridToggleTip = FeatureTip(
        id: "grid_toggle",
        title: "构图网格",
        description: "开启九宫格辅助线，帮助构图",
        icon: "grid",
        arrowDirection: .top
    )

    static let aspectRatioTip = FeatureTip(
        id: "aspect_ratio",
        title: "画幅比例",
        description: "切换全屏/1:1/3:4/9:16 多种画幅",
        icon: "aspectratio",
        arrowDirection: .top
    )

    static let gestureNavTip = FeatureTip(
        id: "gesture_nav",
        title: "手势导航",
        description: "右滑打开相册，左滑打开设置，上滑打开社区",
        icon: "hand.draw",
        arrowDirection: .bottom
    )

    static let allTips: [FeatureTip] = [
        .flashTip, .timerTip, .filterStripTip, .beautyPanelTip,
        .gridToggleTip, .aspectRatioTip, .gestureNavTip
    ]
}

// MARK: - 功能提示视图

struct FeatureTipView: View {
    let tip: FeatureTip
    let onDismiss: () -> Void

    @State private var isVisible = false
    @State private var isPulsing = false

    var body: some View {
        VStack(spacing: 0) {
            // 上箭头
            if tip.arrowDirection == .bottom {
                arrowUp
            }

            // 气泡内容
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: tip.icon)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 28, height: 28)
                        .background(
                            Circle()
                                .fill(DesignSystem.Colors.primary.opacity(0.15))
                        )

                    Text(tip.title)
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)

                    Spacer()

                    Button {
                        withAnimation(DesignSystem.Animation.easeOut) {
                            isVisible = false
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                            onDismiss()
                        }
                    } label: {
                        Text("知道了")
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(DesignSystem.Colors.primary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                Capsule()
                                    .fill(DesignSystem.Colors.primary.opacity(0.12))
                            )
                    }
                }

                Text(tip.description)
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.textSecondary)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                    .fill(DesignSystem.Colors.backgroundSecondary)
                    .overlay(
                        RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                            .strokeBorder(DesignSystem.Colors.primary.opacity(0.2), lineWidth: 1)
                    )
            )
            .shadow(color: Color.black.opacity(0.15), radius: 16, x: 0, y: 4)

            // 下箭头
            if tip.arrowDirection == .top {
                arrowDown
            }

            if tip.arrowDirection == .left {
                // 左箭头通过 overlay 实现
                EmptyView()
            }
            if tip.arrowDirection == .right {
                EmptyView()
            }
        }
        .frame(maxWidth: 280)
        .opacity(isVisible ? 1 : 0)
        .scaleEffect(isVisible ? 1 : 0.8)
        .offset(y: isVisible ? 0 : 10)
        .onAppear {
            withAnimation(DesignSystem.Animation.bouncy) {
                isVisible = true
            }
        }
    }

    private var arrowUp: some View {
        Image(systemName: "arrowtriangle.up.fill")
            .font(.system(size: 12))
            .foregroundColor(DesignSystem.Colors.backgroundSecondary)
            .offset(y: 1)
    }

    private var arrowDown: some View {
        Image(systemName: "arrowtriangle.down.fill")
            .font(.system(size: 12))
            .foregroundColor(DesignSystem.Colors.backgroundSecondary)
            .offset(y: -1)
    }
}

// MARK: - 功能提示管理器

final class FeatureTipManager: ObservableObject {
    static let shared = FeatureTipManager()

    @Published var currentTip: FeatureTip?
    @Published var showTip = false

    private let defaults = UserDefaults.standard
    private let shownTipsKey = "com.livecapture.shownFeatureTips"

    private var shownTips: Set<String> {
        get {
            let array = defaults.stringArray(forKey: shownTipsKey) ?? []
            return Set(array)
        }
        set {
            defaults.set(Array(newValue), forKey: shownTipsKey)
        }
    }

    /// 按顺序显示下一个未展示的提示
    func showNextTip() -> FeatureTip? {
        for tip in FeatureTip.allTips {
            if !shownTips.contains(tip.id) {
                return tip
            }
        }
        return nil
    }

    /// 显示指定提示（如果未展示过）
    func showTipIfNeeded(_ tip: FeatureTip) {
        guard !shownTips.contains(tip.id) else { return }
        currentTip = tip
        showTip = true
    }

    /// 标记提示已展示
    func markTipAsShown(_ tip: FeatureTip) {
        var shown = shownTips
        shown.insert(tip.id)
        shownTips = shown
        showTip = false
    }

    /// 检查提示是否已展示
    func isTipShown(_ tip: FeatureTip) -> Bool {
        shownTips.contains(tip.id)
    }

    /// 重置所有提示
    func resetAllTips() {
        defaults.removeObject(forKey: shownTipsKey)
    }
}

// MARK: - 功能提示覆盖层

struct FeatureTipOverlay: View {
    @ObservedObject var tipManager = FeatureTipManager.shared
    let alignment: Alignment

    init(alignment: Alignment = .center) {
        self.alignment = alignment
    }

    var body: some View {
        Group {
            if let tip = tipManager.currentTip, tipManager.showTip {
                ZStack {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                        .onTapGesture {
                            tipManager.markTipAsShown(tip)
                        }

                    FeatureTipView(tip: tip) {
                        tipManager.markTipAsShown(tip)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: alignment)
                    .padding(.horizontal, 20)
                }
                .transition(.opacity)
                .zIndex(100)
            }
        }
        .animation(DesignSystem.Animation.easeInOut, value: tipManager.showTip)
    }
}

#endif