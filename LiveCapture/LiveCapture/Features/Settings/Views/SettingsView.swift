//
//  SettingsView.swift
//  LiveCapture
//
//  设置页面 - 含合规入口、拍摄设置、编辑设置
//

import SwiftUI

struct SettingsView: View {
    @AppStorage("detectionMode") private var detectionMode: DetectionMode = .fast
    @AppStorage("autoCaptureEnabled") private var autoCaptureEnabled = true
    @AppStorage("captureDelay") private var captureDelay: Double = 1.0
    @AppStorage("colorScheme") private var colorScheme: String = "system"

    // 摄影设置
    @AppStorage("defaultCompositionGuide") private var defaultCompositionGuide = "grid"
    @AppStorage("showLevelIndicator") private var showLevelIndicatorDefault = false
    @AppStorage("showHistogram") private var showHistogramDefault = false
    @AppStorage("timerEnabled") private var timerEnabled = false
    @AppStorage("timerDuration") private var timerDuration = 3
    @AppStorage("burstModeEnabled") private var burstModeEnabled = false
    @AppStorage("hdrMode") private var hdrMode = "auto"
    @AppStorage("livePhotoEnabled") private var livePhotoEnabled = false
    @AppStorage("performanceMode") private var performanceMode = "balanced"
    @AppStorage("oneHandMode") private var oneHandMode = "center"
    @AppStorage("isLeftHanded") private var isLeftHanded = false
    @AppStorage("exportQuality") private var exportQuality = "original"

    @State private var showPrivacy = false
    @State private var showAgreement = false
    @State private var showAccountDeletion = false
    @State private var showYouthMode = false
    @State private var showPersonalInfo = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: DesignSystem.Spacing.large) {
                    headerSection

                    themeSection

                    captureSection

                    photographySection

                    modelSection

                    editingSection

                    complianceSection

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
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("设置")
                .font(DesignSystem.Typography.title1)
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

    // MARK: - Photography Settings

    private var photographySection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("拍摄设置")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                // 构图引导默认
                HStack(spacing: 10) {
                    Image(systemName: "rectangle.split.3x3")
                        .font(.system(size: 15))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 24)
                    Text("默认构图引导")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Spacer()
                    Picker("", selection: $defaultCompositionGuide) {
                        Text("九宫格").tag("grid")
                        Text("黄金分割").tag("goldenRatio")
                        Text("黄金螺线").tag("goldenSpiral")
                        Text("对角线").tag("diagonal")
                        Text("十字准星").tag("crosshair")
                        Text("方形").tag("square")
                        Text("关闭").tag("none")
                    }
                    .pickerStyle(.menu)
                    .tint(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, 14)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, 44)

                // 水平仪
                ToggleRow(
                    icon: "level",
                    title: "默认显示水平仪",
                    description: "拍摄时显示水平仪辅助",
                    isOn: $showLevelIndicatorDefault
                )

                Divider().padding(.leading, 44)

                // 直方图
                ToggleRow(
                    icon: "waveform.path.ecg",
                    title: "默认显示直方图",
                    description: "实时亮度分布直方图",
                    isOn: $showHistogramDefault
                )

                Divider().padding(.leading, 44)

                // 定时拍摄
                VStack(alignment: .leading, spacing: 8) {
                    ToggleRow(
                        icon: "timer",
                        title: "定时拍摄",
                        description: "按下快门后延迟拍摄",
                        isOn: $timerEnabled
                    )

                    if timerEnabled {
                        Picker("定时", selection: $timerDuration) {
                            Text("3秒").tag(3)
                            Text("5秒").tag(5)
                            Text("10秒").tag(10)
                        }
                        .pickerStyle(.segmented)
                        .padding(.horizontal, DesignSystem.Spacing.medium)
                        .padding(.bottom, 8)
                    }
                }

                Divider().padding(.leading, 44)

                // 连拍
                ToggleRow(
                    icon: "burst",
                    title: "连拍模式",
                    description: "按住快门连续拍摄10张",
                    isOn: $burstModeEnabled
                )

                Divider().padding(.leading, 44)

                // HDR
                HStack(spacing: 10) {
                    Image(systemName: "hdr")
                        .font(.system(size: 15))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 24)
                    Text("HDR 模式")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Spacer()
                    Picker("", selection: $hdrMode) {
                        Text("自动").tag("auto")
                        Text("开启").tag("on")
                        Text("关闭").tag("off")
                    }
                    .pickerStyle(.menu)
                    .tint(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, 14)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, 44)

                // Live Photo
                ToggleRow(
                    icon: "livephoto",
                    title: "Live Photo",
                    description: "记录拍摄前后的动态瞬间",
                    isOn: $livePhotoEnabled
                )

                Divider().padding(.leading, 44)

                // 性能模式
                HStack(spacing: 10) {
                    Image(systemName: "gauge.with.dots.needle.33percent")
                        .font(.system(size: 15))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 24)
                    Text("性能模式")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Spacer()
                    Picker("", selection: $performanceMode) {
                        Text("画质优先").tag("quality")
                        Text("均衡").tag("balanced")
                        Text("速度优先").tag("speed")
                        Text("省电").tag("battery")
                    }
                    .pickerStyle(.menu)
                    .tint(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, 14)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, 44)

                // 单手操作
                HStack(spacing: 10) {
                    Image(systemName: "hand.raised")
                        .font(.system(size: 15))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 24)
                    Text("单手操作")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Spacer()
                    Picker("", selection: $oneHandMode) {
                        Text("居中").tag("center")
                        Text("靠左").tag("left")
                        Text("靠右").tag("right")
                    }
                    .pickerStyle(.menu)
                    .tint(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, 14)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, 44)

                // 左手模式
                ToggleRow(
                    icon: "hand.point.left",
                    title: "左手模式",
                    description: "优化左手操作布局",
                    isOn: $isLeftHanded
                )
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

    // MARK: - Editing Settings

    private var editingSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("编辑与导出")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                // 导出质量
                HStack(spacing: 10) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 15))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 24)
                    Text("导出质量")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Spacer()
                    Picker("", selection: $exportQuality) {
                        Text("原图 (~5MB)").tag("original")
                        Text("高质量 90%").tag("high")
                        Text("中等 70%").tag("medium")
                        Text("压缩 50%").tag("compressed")
                    }
                    .pickerStyle(.menu)
                    .tint(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, 14)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, 44)

                // 编辑预设
                HStack(spacing: 10) {
                    Image(systemName: "square.on.square")
                        .font(.system(size: 15))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 24)
                    Text("编辑预设")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Spacer()
                    Text("\(EditPresetManager.shared.savedPresets.count) 个")
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, 14)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, 44)

                // 缓存管理
                HStack(spacing: 10) {
                    Image(systemName: "opticaldisc")
                        .font(.system(size: 15))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 24)
                    Text("清除图片缓存")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Spacer()
                    Button {
                        PhotoCacheManager.shared.clearCache()
                        ToastManager.shared.success("缓存已清除")
                    } label: {
                        Text("清除")
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(DesignSystem.Colors.error)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 4)
                            .background(
                                Capsule()
                                    .fill(DesignSystem.Colors.errorBg)
                            )
                    }
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