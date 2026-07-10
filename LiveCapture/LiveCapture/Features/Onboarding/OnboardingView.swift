//
//  OnboardingView.swift
//  LiveCapture
//
//  首次启动引导页 - 功能介绍与权限引导
//

import SwiftUI

#if os(iOS)

struct OnboardingView: View {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var currentPage = 0
    @State private var showPrivacyConsent = false

    private let pages: [OnboardingPage] = [
        OnboardingPage(
            icon: "camera.viewfinder",
            title: "AI 智能构图",
            description: "基于 CoreML 的实时构图分析，AI 帮你找到最佳拍摄角度与画面布局",
            color: DesignSystem.Colors.primary
        ),
        OnboardingPage(
            icon: "camera.filters",
            title: "专业级滤镜",
            description: "42+ 款经典胶片模拟滤镜，AI 场景识别自动推荐，一键出片",
            color: DesignSystem.Colors.secondary
        ),
        OnboardingPage(
            icon: "face.smiling",
            title: "智能美颜",
            description: "自然美颜算法，磨皮、美白、瘦脸，保留肌肤质感不失真",
            color: DesignSystem.Colors.accent
        ),
        OnboardingPage(
            icon: "square.and.arrow.up",
            title: "一键分享",
            description: "精美分享卡片生成，一键分享到社交平台，展示你的摄影作品",
            color: DesignSystem.Colors.success
        ),
    ]

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                // 跳过按钮
                HStack {
                    Spacer()
                    Button {
                        completeOnboarding()
                    } label: {
                        Text("跳过")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(.white.opacity(0.6))
                            .padding(.horizontal, 20)
                            .padding(.vertical, 8)
                    }
                }
                .padding(.top, 16)

                // 页面指示器
                HStack(spacing: 8) {
                    ForEach(0..<pages.count, id: \.self) { index in
                        Capsule()
                            .fill(index == currentPage ? pages[index].color : Color.white.opacity(0.3))
                            .frame(width: index == currentPage ? 20 : 8, height: 8)
                            .animation(DesignSystem.Animation.smooth, value: currentPage)
                    }
                }
                .padding(.top, 20)

                // 内容区
                TabView(selection: $currentPage) {
                    ForEach(0..<pages.count, id: \.self) { index in
                        pageView(pages[index])
                            .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(DesignSystem.Animation.easeInOut, value: currentPage)

                // 底部按钮
                VStack(spacing: 12) {
                    if currentPage < pages.count - 1 {
                        Button {
                            withAnimation(DesignSystem.Animation.smooth) {
                                currentPage += 1
                            }
                        } label: {
                            HStack {
                                Text("继续")
                                    .fontWeight(.semibold)
                                Image(systemName: "arrow.right")
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(pages[currentPage].color)
                            .foregroundColor(.white)
                            .cornerRadius(DesignSystem.CornerRadius.large)
                        }
                        .padding(.horizontal, 32)
                    } else {
                        Button {
                            showPrivacyConsent = true
                        } label: {
                            HStack {
                                Text("开始使用")
                                    .fontWeight(.semibold)
                                Image(systemName: "arrow.right")
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(pages[currentPage].color)
                            .foregroundColor(.white)
                            .cornerRadius(DesignSystem.CornerRadius.large)
                        }
                        .padding(.horizontal, 32)
                    }

                    // 隐私政策链接
                    HStack(spacing: 4) {
                        Text("继续即表示同意")
                            .font(DesignSystem.Typography.caption2)
                            .foregroundColor(.white.opacity(0.4))
                        Button {
                            showPrivacyConsent = true
                        } label: {
                            Text("隐私政策")
                                .font(DesignSystem.Typography.caption2)
                                .underline()
                                .foregroundColor(.white.opacity(0.6))
                        }
                        Text("和")
                            .font(DesignSystem.Typography.caption2)
                            .foregroundColor(.white.opacity(0.4))
                        Button {
                            showPrivacyConsent = true
                        } label: {
                            Text("用户协议")
                                .font(DesignSystem.Typography.caption2)
                                .underline()
                                .foregroundColor(.white.opacity(0.6))
                        }
                    }
                    .padding(.bottom, 8)
                }
                .padding(.bottom, 40)
            }
        }
        .sheet(isPresented: $showPrivacyConsent) {
            PrivacyConsentSheet {
                completeOnboarding()
            }
        }
    }

    private func pageView(_ page: OnboardingPage) -> some View {
        VStack(spacing: 32) {
            Spacer()

            // 图标
            ZStack {
                Circle()
                    .fill(page.color.opacity(0.15))
                    .frame(width: 160, height: 160)

                Circle()
                    .stroke(page.color.opacity(0.3), lineWidth: 1)
                    .frame(width: 160, height: 160)

                Image(systemName: page.icon)
                    .font(.system(size: 64, weight: .light))
                    .foregroundColor(page.color)
            }
            .shadow(color: page.color.opacity(0.2), radius: 20, x: 0, y: 0)

            // 文字
            VStack(spacing: 16) {
                Text(page.title)
                    .font(DesignSystem.Typography.title1)
                    .foregroundColor(.white)

                Text(page.description)
                    .font(DesignSystem.Typography.body)
                    .foregroundColor(.white.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .lineSpacing(6)
                    .padding(.horizontal, 40)
            }

            Spacer()
            Spacer()
        }
    }

    private func completeOnboarding() {
        withAnimation(DesignSystem.Animation.easeOut) {
            hasCompletedOnboarding = true
        }
    }
}

// MARK: - 隐私同意弹窗

struct PrivacyConsentSheet: View {
    let onAgree: () -> Void
    @State private var showPrivacy = false
    @State private var showAgreement = false

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "hand.raised.fill")
                .font(.system(size: 40))
                .foregroundColor(DesignSystem.Colors.primary)
                .padding(.top, 32)

            Text("隐私与协议")
                .font(DesignSystem.Typography.title2)
                .fontWeight(.bold)

            Text("""
                请阅读并同意以下协议，我们将严格保护您的隐私和数据安全。

                您的照片和视频仅在设备本地处理，不会上传到云端。
                """)
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textSecondary)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .padding(.horizontal, 24)

            VStack(spacing: 12) {
                Button {
                    showPrivacy = true
                } label: {
                    HStack {
                        Image(systemName: "doc.text")
                        Text("查看隐私政策")
                        Spacer()
                        Image(systemName: "chevron.right")
                    }
                    .padding()
                    .background(DesignSystem.Colors.backgroundSecondary)
                    .cornerRadius(10)
                }

                Button {
                    showAgreement = true
                } label: {
                    HStack {
                        Image(systemName: "doc.text.fill")
                        Text("查看用户协议")
                        Spacer()
                        Image(systemName: "chevron.right")
                    }
                    .padding()
                    .background(DesignSystem.Colors.backgroundSecondary)
                    .cornerRadius(10)
                }
            }
            .padding(.horizontal, 24)

            Button {
                onAgree()
            } label: {
                Text("同意并继续")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(DesignSystem.Colors.primary)
                    .foregroundColor(.white)
                    .cornerRadius(DesignSystem.CornerRadius.large)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
        .sheet(isPresented: $showPrivacy) {
            PrivacyPolicyView()
        }
        .sheet(isPresented: $showAgreement) {
            UserAgreementView()
        }
    }
}

// MARK: - 数据模型

struct OnboardingPage {
    let icon: String
    let title: String
    let description: String
    let color: Color
}

#endif