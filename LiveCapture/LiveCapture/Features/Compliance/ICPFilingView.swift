//
//  ICPFilingView.swift
//  LiveCapture
//
//  ICP 备案展示页面 - 工信部备案信息
//

import SwiftUI

#if os(iOS)

/// ICP 备案展示视图
struct ICPFilingView: View {
    let info = ICPFilingInfo.fromBundle()

    private var beianURL: URL {
        URL(string: "https://beian.miit.gov.cn") ?? URL(fileURLWithPath: "")
    }

    var body: some View {
        VStack(spacing: 12) {
            // 公司名称
            if !info.companyName.isEmpty {
                Text(info.companyName)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            }

            // ICP 备案号
            if !info.icpNumber.isEmpty {
                Link(destination: URL(string: info.icpLink) ?? beianURL) {
                    HStack(spacing: 4) {
                        Image(systemName: "shield.checkered")
                            .font(.system(size: 10))
                        Text(info.icpNumber)
                            .font(DesignSystem.Typography.caption2)
                    }
                    .foregroundColor(DesignSystem.Colors.textTertiary)
                }
                .accessibilityLabel("ICP备案号 \(info.icpNumber)")
                .accessibilityHint("点击前往工信部备案系统查询")
            } else {
                // 占位备案号
                Link(destination: beianURL) {
                    HStack(spacing: 4) {
                        Image(systemName: "shield.checkered")
                            .font(.system(size: 10))
                        Text("ICP备案号：待备案")
                            .font(DesignSystem.Typography.caption2)
                    }
                    .foregroundColor(DesignSystem.Colors.textTertiary)
                }
                .accessibilityLabel("ICP备案号待备案")
                .accessibilityHint("点击前往工信部备案系统")
            }

            // 网安备案号
            if let nsNumber = info.networkSecurityNumber, !nsNumber.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "lock.shield")
                        .font(.system(size: 10))
                    Text(nsNumber)
                        .font(DesignSystem.Typography.caption2)
                }
                .foregroundColor(DesignSystem.Colors.textTertiary)
                .accessibilityLabel("网安备案号 \(nsNumber)")
            }
        }
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 完整 ICP 备案详情页

/// ICP 备案详情页面（用于独立展示）
struct ICPFilingDetailView: View {
    @Environment(\.dismiss) private var dismiss
    let info = ICPFilingInfo.fromBundle()

    private var beianURL: URL {
        URL(string: "https://beian.miit.gov.cn") ?? URL(fileURLWithPath: "")
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // 头部信息
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Image(systemName: "shield.checkered")
                                .font(.system(size: 28))
                                .foregroundColor(DesignSystem.Colors.primary)
                            Text("ICP 备案信息")
                                .font(DesignSystem.Typography.title2)
                                .foregroundColor(DesignSystem.Colors.textPrimary)
                        }

                        Text("根据《中华人民共和国电信条例》和《互联网信息服务管理办法》规定，本应用已完成ICP备案。")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                            .lineSpacing(4)
                    }
                    .padding(.bottom, 8)

                    // 备案信息卡片
                    VStack(spacing: 0) {
                        infoRow(icon: "building.2", title: "主办单位", value: info.companyName.isEmpty ? "待备案" : info.companyName)
                        Divider().padding(.leading, 44)
                        infoRow(icon: "number", title: "ICP 备案号", value: info.icpNumber.isEmpty ? "待备案" : info.icpNumber)
                        if let nsNumber = info.networkSecurityNumber, !nsNumber.isEmpty {
                            Divider().padding(.leading, 44)
                            infoRow(icon: "lock.shield", title: "网安备案号", value: nsNumber)
                        }
                    }
                    .background(
                        RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                            .fill(DesignSystem.Colors.backgroundSecondary)
                    )

                    // 链接到工信部
                    Link(destination: URL(string: info.icpLink) ?? beianURL) {
                        HStack {
                            Image(systemName: "safari")
                                .font(.system(size: 15))
                            Text("前往工信部备案系统查询")
                                .font(DesignSystem.Typography.headline)
                            Spacer()
                            Image(systemName: "arrow.up.forward")
                                .font(.system(size: 13))
                        }
                        .foregroundColor(DesignSystem.Colors.primary)
                        .padding(.vertical, 14)
                        .padding(.horizontal, DesignSystem.Spacing.medium)
                        .background(
                            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                                .fill(DesignSystem.Colors.backgroundSecondary)
                        )
                    }
                    .accessibilityLabel("前往工信部备案系统查询")
                    .accessibilityHint("点击打开工信部备案系统网页")

                    // 说明
                    VStack(alignment: .leading, spacing: 8) {
                        Text("什么是 ICP 备案？")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textPrimary)

                        Text("ICP 备案（Internet Content Provider 备案）是中国大陆境内提供互联网信息服务的网站和应用的法定要求。所有在中国大陆运营的网站和应用都必须完成 ICP 备案。")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                            .lineSpacing(4)
                    }
                    .padding(.top, 8)
                }
                .padding(20)
            }
            .background(DesignSystem.Colors.backgroundPrimary)
            .navigationTitle("ICP 备案")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }

    private func infoRow(icon: String, title: String, value: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 15))
                .foregroundColor(DesignSystem.Colors.primary)
                .frame(width: 24)
            Text(title)
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textPrimary)
            Spacer()
            Text(value)
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .padding(.vertical, 14)
        .padding(.horizontal, DesignSystem.Spacing.medium)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title): \(value)")
    }
}

#endif