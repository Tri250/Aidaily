//
//  FirstLaunchManager.swift
//  LiveCapture
//
//  首次启动管理器 - 跟踪版本、引导页、新功能展示
//

import Foundation
import SwiftUI

#if os(iOS)

// MARK: - 首次启动管理器

final class FirstLaunchManager: ObservableObject {
    static let shared = FirstLaunchManager()

    private let defaults = UserDefaults.standard

    // MARK: - Keys

    private enum Keys {
        static let hasCompletedOnboarding = "hasCompletedOnboarding"
        static let lastAppVersion = "com.livecapture.lastAppVersion"
        static let appInstallDate = "com.livecapture.appInstallDate"
        static let launchCount = "com.livecapture.launchCount"
        static let whatsNewSeenForVersion = "com.livecapture.whatsNewSeenForVersion"
    }

    // MARK: - Version Info

    var currentVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0.0"
    }

    var lastAppVersion: String {
        get { defaults.string(forKey: Keys.lastAppVersion) ?? "" }
        set { defaults.set(newValue, forKey: Keys.lastAppVersion) }
    }

    // MARK: - Launch State

    var isFirstLaunch: Bool {
        lastAppVersion.isEmpty
    }

    var isVersionUpdate: Bool {
        let last = lastAppVersion
        return !last.isEmpty && last != currentVersion
    }

    var launchCount: Int {
        get { defaults.integer(forKey: Keys.launchCount) }
        set { defaults.set(newValue, forKey: Keys.launchCount) }
    }

    var installDate: Date? {
        get {
            guard let interval = defaults.object(forKey: Keys.appInstallDate) as? TimeInterval else { return nil }
            return Date(timeIntervalSince1970: interval)
        }
        set {
            if let date = newValue {
                defaults.set(date.timeIntervalSince1970, forKey: Keys.appInstallDate)
            }
        }
    }

    // MARK: - What's New

    var hasSeenWhatsNew: Bool {
        get { defaults.string(forKey: Keys.whatsNewSeenForVersion) == currentVersion }
        set {
            if newValue {
                defaults.set(currentVersion, forKey: Keys.whatsNewSeenForVersion)
            } else {
                defaults.removeObject(forKey: Keys.whatsNewSeenForVersion)
            }
        }
    }

    // MARK: - Onboarding

    var hasCompletedOnboarding: Bool {
        get { defaults.bool(forKey: Keys.hasCompletedOnboarding) }
        set { defaults.set(newValue, forKey: Keys.hasCompletedOnboarding) }
    }

    // MARK: - Lifecycle

    func handleAppLaunch() {
        // 记录安装日期
        if installDate == nil {
            installDate = Date()
        }

        // 增加启动次数
        launchCount += 1

        // 更新版本记录
        lastAppVersion = currentVersion
    }

    // MARK: - What's New Content

    func getWhatsNewItems() -> [WhatsNewItem] {
        // 根据版本返回对应的新功能列表
        // 这里可以根据实际版本号做判断
        return [
            WhatsNewItem(
                icon: "camera.viewfinder",
                title: "AI 智能构图增强",
                description: "全新 CoreML 模型，构图识别更精准"
            ),
            WhatsNewItem(
                icon: "camera.filters",
                title: "新增 12 款滤镜",
                description: "42+ 款经典胶片滤镜，更多风格选择"
            ),
            WhatsNewItem(
                icon: "face.smiling",
                title: "智能美颜升级",
                description: "自然美颜算法优化，保留更多肌肤质感"
            ),
            WhatsNewItem(
                icon: "book",
                title: "拍摄教程",
                description: "新增拍摄技巧指南，助你快速提升摄影水平"
            ),
        ]
    }
}

// MARK: - What's New 数据模型

struct WhatsNewItem: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let description: String
}

// MARK: - What's New 视图

struct WhatsNewView: View {
    @Environment(\.dismiss) private var dismiss
    let items: [WhatsNewItem]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // 头部
                VStack(spacing: 8) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 40, weight: .light))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .padding(.top, 32)

                    Text("新功能")
                        .font(DesignSystem.Typography.title1)
                        .foregroundColor(DesignSystem.Colors.textPrimary)

                    Text("版本 \(FirstLaunchManager.shared.currentVersion)")
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                }
                .padding(.bottom, DesignSystem.Spacing.xLarge)

                // 功能列表
                ScrollView {
                    VStack(spacing: DesignSystem.Spacing.small) {
                        ForEach(items) { item in
                            whatsNewRow(item)
                        }
                    }
                    .padding(.horizontal, DesignSystem.Spacing.Padding.container)
                }

                // 底部按钮
                Button {
                    FirstLaunchManager.shared.hasSeenWhatsNew = true
                    dismiss()
                } label: {
                    Text("开始使用")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(DesignSystem.Colors.primary)
                        .foregroundColor(.white)
                        .cornerRadius(DesignSystem.CornerRadius.large)
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 40)
            }
            .background(DesignSystem.Colors.backgroundPrimary)
        }
    }

    private func whatsNewRow(_ item: WhatsNewItem) -> some View {
        HStack(spacing: DesignSystem.Spacing.medium) {
            ZStack {
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.primary.opacity(0.1))
                    .frame(width: 48, height: 48)

                Image(systemName: item.icon)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(DesignSystem.Colors.primary)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(DesignSystem.Typography.headline)
                    .foregroundColor(DesignSystem.Colors.textPrimary)

                Text(item.description)
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.textSecondary)
                    .lineSpacing(2)
            }

            Spacer()
        }
        .padding(DesignSystem.Spacing.small)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
    }
}

#endif