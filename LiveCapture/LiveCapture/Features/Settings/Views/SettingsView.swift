//
//  SettingsView.swift
//  LiveCapture
//
//  设置页面 - 含合规入口
//

import SwiftUI

struct SettingsView: View {
    @AppStorage("detectionMode") private var detectionMode: DetectionMode = .fast
    @AppStorage("autoCaptureEnabled") private var autoCaptureEnabled = true
    @AppStorage("captureDelay") private var captureDelay: Double = 1.0
    @AppStorage("colorScheme") private var colorScheme: String = "system"

    @State private var showPrivacy = false
    @State private var showAgreement = false
    @State private var showAccountDeletion = false
    @State private var showYouthMode = false
    @State private var showPersonalInfo = false
    @State private var showWatermarkSettings = false
    @State private var showShootingGuide = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: DesignSystem.Spacing.large) {
                    headerSection

                    themeSection

                    watermarkSection

                    captureSection

                    modelSection

                    complianceSection

                    tutorialSection

                    aboutSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
            .background(Color(uiColor: .systemBackground))
            .navigationBarHidden(true)
        }
        .sheet(isPresented: $showPrivacy) {
            PrivacyPolicyView()
        }
        .sheet(isPresented: $showAgreement) {
            UserAgreementView()
        }
        .sheet(isPresented: $showAccountDeletion) {
            AccountDeletionView()
        }
        .sheet(isPresented: $showYouthMode) {
            YouthModeView()
        }
        .sheet(isPresented: $showPersonalInfo) {
            PersonalInfoCollectionView()
        }
        .sheet(isPresented: $showWatermarkSettings) {
            WatermarkEditView()
        }
        .sheet(isPresented: $showShootingGuide) {
            ShootingGuideView()
        }
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("设置")
                .font(DesignSystem.Typography.title)
                .foregroundColor(DesignSystem.Colors.textPrimary)
            Text("定制你的拍摄体验")
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .padding(.top, 20)
        .accessibilityAddTraits(.isHeader)
    }

    // MARK: - Theme

    private var themeSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("外观")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            Picker("外观", selection: $colorScheme) {
                Text("跟随系统").tag("system")
                Text("浅色").tag("light")
                Text("深色").tag("dark")
            }
            .pickerStyle(.segmented)
            .accessibilityLabel("主题模式选择")
        }
    }

    // MARK: - Watermark

    private var watermarkSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("水印")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                Button {
                    showWatermarkSettings = true
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "text.below.photo")
                            .font(.system(size: 15))
                            .foregroundColor(DesignSystem.Colors.primary)
                            .frame(width: 24)
                        Text("水印设置")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    .padding(.vertical, 14)
                    .padding(.horizontal, DesignSystem.Spacing.medium)
                }
                .accessibilityLabel("水印设置")
                .accessibilityHint("点击配置照片水印")
            }
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    // MARK: - Capture

    private var captureSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("自动拍摄")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                ToggleRow(
                    icon: "viewfinder",
                    title: "AI 自动拍摄",
                    description: "当构图对齐中心时自动拍摄",
                    isOn: $autoCaptureEnabled
                )

                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 10) {
                        Image(systemName: "timer")
                            .font(.system(size: 15))
                            .foregroundColor(DesignSystem.Colors.primary)
                            .frame(width: 24)
                        Text("拍照延迟")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                        Spacer()
                        Text("\(String(format: "%.1f", captureDelay))秒")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("拍照延迟 \(String(format: "%.1f", captureDelay)) 秒")

                    Picker("延迟", selection: $captureDelay) {
                        Text("0.5秒").tag(0.5)
                        Text("1.0秒").tag(1.0)
                        Text("1.5秒").tag(1.5)
                        Text("2.0秒").tag(2.0)
                    }
                    .pickerStyle(.segmented)

                    Text("对齐中心后等待此时间再自动拍摄")
                        .font(DesignSystem.Typography.caption1)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, 14)
                .padding(.horizontal, DesignSystem.Spacing.medium)
            }
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    // MARK: - Model

    private var modelSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("构图引擎")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            Picker("构图引擎", selection: $detectionMode) {
                ForEach(DetectionMode.allCases) { mode in
                    Text(mode.displayName).tag(mode)
                }
            }
            .pickerStyle(.segmented)
            .accessibilityLabel("构图引擎模式选择")

            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Image(systemName: modelIcon)
                        .font(.system(size: 13))
                        .foregroundColor(DesignSystem.Colors.primary)
                    Text(detectionMode.displayName)
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                }
                Text(detectionMode.description)
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
                    .lineSpacing(3)
            }
            .padding(DesignSystem.Spacing.medium)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    // MARK: - Compliance

    private var complianceSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("隐私与合规")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                complianceRow(icon: "doc.text", title: "隐私政策") {
                    showPrivacy = true
                }

                Divider().padding(.leading, 44)

                complianceRow(icon: "doc.text.fill", title: "用户协议") {
                    showAgreement = true
                }

                Divider().padding(.leading, 44)

                complianceRow(icon: "list.clipboard", title: "个人信息收集清单") {
                    showPersonalInfo = true
                }

                Divider().padding(.leading, 44)

                complianceRow(icon: "person.crop.circle.badge.xmark", title: "账号管理") {
                    showAccountDeletion = true
                }

                Divider().padding(.leading, 44)

                complianceRow(icon: "lock.shield", title: "青少年模式") {
                    showYouthMode = true
                }
            }
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    // MARK: - Tutorial

    private var tutorialSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("学习")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                Button {
                    showShootingGuide = true
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "book")
                            .font(.system(size: 15))
                            .foregroundColor(DesignSystem.Colors.primary)
                            .frame(width: 24)
                        Text("拍摄教程")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    .padding(.vertical, 14)
                    .padding(.horizontal, DesignSystem.Spacing.medium)
                }
                .accessibilityLabel("拍摄教程")
                .accessibilityHint("点击查看拍摄技巧指南")
            }
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    // MARK: - About

    private var aboutSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("关于")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                HStack {
                    Text("版本")
                        .font(DesignSystem.Typography.headline)
                    Spacer()
                    Text(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0.0")
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, 14)
                .padding(.horizontal, DesignSystem.Spacing.medium)
                .accessibilityElement(children: .combine)
                .accessibilityLabel("版本 \(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0.0")")
            }
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )

            // ICP 备案
            ICPFilingView()
        }
    }

    private func complianceRow(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 15))
                    .foregroundColor(DesignSystem.Colors.primary)
                    .frame(width: 24)
                Text(title)
                    .font(DesignSystem.Typography.headline)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            }
            .padding(.vertical, 14)
            .padding(.horizontal, DesignSystem.Spacing.medium)
        }
        .accessibilityLabel(title)
        .accessibilityHint("点击查看 \(title)")
    }

    private var modelIcon: String {
        switch detectionMode {
        case .vision: return "eye"
        case .fast:   return "bolt"
        case .pro:    return "sparkles"
        }
    }
}

// MARK: - Subviews

private struct ToggleRow: View {
    let icon: String
    let title: String
    let description: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 15))
                .foregroundColor(DesignSystem.Colors.primary)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(DesignSystem.Typography.headline)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                Text(description)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            }
            Spacer()
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(DesignSystem.Colors.primary)
        }
        .padding(.vertical, 14)
        .padding(.horizontal, DesignSystem.Spacing.medium)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title), \(isOn ? "已开启" : "已关闭")")
        .accessibilityHint(description)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}