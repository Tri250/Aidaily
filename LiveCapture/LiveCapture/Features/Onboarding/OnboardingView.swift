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
            icon: "viewfinder.circle.fill",
            title: "欢迎使用 构妙",
            description: "AI 智能摄影助手，让每张照片都是艺术品",
            color: .blue
        ),
        OnboardingPage(
            icon: "grid.circle.fill",
            title: "AI 智能构图",
            description: "基于 CoreML 的实时构图分析，引导你找到最佳拍摄角度",
            color: .purple
        ),
        OnboardingPage(
            icon: "camera.filters",
            title: "专业滤镜",
            description: "12 款经典胶片滤镜 + AI 场景推荐，一键出片",
            color: .orange
        ),
        OnboardingPage(
            icon: "video.badge.waveform",
            title: "视频录制",
            description: "支持普通视频、慢动作和延时摄影，记录每一个精彩瞬间",
            color: .green
        ),
        OnboardingPage(
            icon: "paintpalette.fill",
            title: "AI 编辑工具",
            description: "物体移除、天空替换、风格迁移，创意无限",
            color: .pink
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
                            .font(.subheadline)
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
                            .animation(.spring(response: 0.3), value: currentPage)
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
                .animation(.easeInOut, value: currentPage)

                // 底部按钮
                VStack(spacing: 12) {
                    Button {
                        if currentPage < pages.count - 1 {
                            withAnimation {
                                currentPage += 1
                            }
                        } else {
                            showPrivacyConsent = true
                        }
                    } label: {
                        HStack {
                            Text(currentPage < pages.count - 1 ? "继续" : "开始使用")
                                .fontWeight(.semibold)
                            Image(systemName: "arrow.right")
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(pages[currentPage].color)
                        .foregroundColor(.white)
                        .cornerRadius(14)
                    }
                    .padding(.horizontal, 32)

                    // 隐私政策链接
                    HStack(spacing: 4) {
                        Text("继续即表示同意")
                            .font(.caption2)
                            .foregroundColor(.white.opacity(0.4))
                        Button {
                            showPrivacyConsent = true
                        } label: {
                            Text("隐私政策")
                                .font(.caption2)
                                .underline()
                                .foregroundColor(.white.opacity(0.6))
                        }
                        Text("和")
                            .font(.caption2)
                            .foregroundColor(.white.opacity(0.4))
                        Button {
                            showPrivacyConsent = true
                        } label: {
                            Text("用户协议")
                                .font(.caption2)
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
                    .frame(width: 140, height: 140)

                Image(systemName: page.icon)
                    .font(.system(size: 60, weight: .light))
                    .foregroundColor(page.color)
            }

            // 文字
            VStack(spacing: 12) {
                Text(page.title)
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(.white)

                Text(page.description)
                    .font(.body)
                    .foregroundColor(.white.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.horizontal, 40)
            }

            Spacer()
            Spacer()
        }
    }

    private func completeOnboarding() {
        hasCompletedOnboarding = true
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
                .foregroundColor(.blue)
                .padding(.top, 32)

            Text("隐私与协议")
                .font(.title2)
                .fontWeight(.bold)

            Text("""
                请阅读并同意以下协议，我们将严格保护您的隐私和数据安全。

                您的照片和视频仅在设备本地处理，不会上传到云端。
                """)
                .font(.subheadline)
                .foregroundColor(.secondary)
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
                    .background(Color(.systemGray6))
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
                    .background(Color(.systemGray6))
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
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(14)
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