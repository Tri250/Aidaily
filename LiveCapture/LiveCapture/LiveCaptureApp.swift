//
//  LiveCaptureApp.swift
//  LiveCapture
//
//  应用程序入口文件 - 集成 Bugly、引导页、青少年模式
//

import SwiftUI
import AppTrackingTransparency
import AdSupport

#if os(iOS)

/// 全局预初始化相机管理器，在应用启动时后台准备
private let prewarmedCamera = CameraManager()

@main
struct LiveCaptureApp: App {
    @State private var showSplash = true
    @State private var splashOpacity: Double = 1.0
    @State private var privacyConsentGiven = false
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @StateObject private var youthModeManager = YouthModeManager.shared
    @StateObject private var launchManager = FirstLaunchManager.shared
    @State private var showWhatsNew = false

    var body: some Scene {
        WindowGroup {
            ZStack {
                // 主界面 - 始终在底层
                if hasCompletedOnboarding {
                    MinimalMainView()
                        .zIndex(0)
                } else {
                    OnboardingView()
                        .zIndex(0)
                }

                // What's New 弹窗 - 版本更新时显示
                if showWhatsNew {
                    WhatsNewView(items: launchManager.getWhatsNewItems())
                        .zIndex(15)
                        .transition(.opacity)
                }

                // 启动画面 - 淡出过渡
                if showSplash {
                    splashScreenView
                        .zIndex(10)
                        .opacity(splashOpacity)
                        .transition(.opacity)
                }

                // 青少年模式限制覆盖层
                if youthModeManager.isYouthModeEnabled && youthModeManager.isInNightBanPeriod {
                    nightBanOverlay
                        .zIndex(20)
                }

                if youthModeManager.isYouthModeEnabled && youthModeManager.isDailyLimitExceeded {
                    timeLimitOverlay
                        .zIndex(20)
                }
            }
            .onAppear {
                // 初始化崩溃上报
                BuglyCrashReporter.shared.start()

                // 跟踪启动状态
                launchManager.handleAppLaunch()

                // 检查上次崩溃
                if let crashInfo = BuglyCrashReporter.shared.checkLastCrash() {
                    LiveCaptureLogger.shared.warning("上次启动发生崩溃: \(crashInfo)")
                }

                prewarmCamera()
                requestATTAuthorization()

                // 目标 < 0.8s 进入相机就绪状态
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    withAnimation(.easeOut(duration: 0.3)) {
                        splashOpacity = 0
                    }
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                    showSplash = false

                    // 版本更新时显示 What's New
                    if launchManager.isVersionUpdate && !launchManager.hasSeenWhatsNew {
                        withAnimation(DesignSystem.Animation.easeInOut) {
                            showWhatsNew = true
                        }
                    }
                }
            }
        }
    }

    /// 请求 App Tracking Transparency 授权
    private func requestATTAuthorization() {
        if #available(iOS 14, *) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                ATTrackingManager.requestTrackingAuthorization { status in
                    switch status {
                    case .authorized:
                        LiveCaptureLogger.shared.info("ATT 授权: 已授权")
                    case .denied:
                        LiveCaptureLogger.shared.info("ATT 授权: 被拒绝")
                    case .restricted:
                        LiveCaptureLogger.shared.info("ATT 授权: 受限")
                    case .notDetermined:
                        LiveCaptureLogger.shared.info("ATT 授权: 未决定")
                    @unknown default:
                        break
                    }
                }
            }
        }
    }

    /// 启动画面
    private var splashScreenView: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 16) {
                Image(systemName: "viewfinder.circle.fill")
                    .font(.system(size: 64, weight: .light))
                    .foregroundColor(.white)
                    .opacity(0.9)

                Text("LiveCapture")
                    .font(.system(size: 28, weight: .semibold, design: .rounded))
                    .foregroundColor(.white.opacity(0.8))
            }
        }
        .accessibilityLabel("构妙 LiveCapture 启动画面")
    }

    /// 夜间禁用覆盖层
    private var nightBanOverlay: some View {
        ZStack {
            Color.black.opacity(0.95).ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "moon.stars.fill")
                    .font(.system(size: 48, weight: .light))
                    .foregroundColor(.white.opacity(0.7))

                Text("夜间禁用时段")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)

                Text("当前处于青少年模式夜间禁用时段\n请在 \(youthModeManager.nightBanEndHour):00 后再使用")
                    .font(.body)
                    .foregroundColor(.white.opacity(0.6))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
            }
            .padding(40)
        }
        .accessibilityLabel("青少年模式夜间禁用")
    }

    /// 时长限制覆盖层
    private var timeLimitOverlay: some View {
        ZStack {
            Color.black.opacity(0.95).ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "hourglass.bottomhalf.filled")
                    .font(.system(size: 48, weight: .light))
                    .foregroundColor(.white.opacity(0.7))

                Text("使用时长已达上限")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)

                Text("今日使用时长已达 \(youthModeManager.dailyTimeLimit) 分钟\n请明天再来")
                    .font(.body)
                    .foregroundColor(.white.opacity(0.6))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
            }
            .padding(40)
        }
        .accessibilityLabel("青少年模式时长限制已达上限")
    }

    /// 预热相机
    private func prewarmCamera() {
        DispatchQueue.global(qos: .userInitiated).async {
            prewarmedCamera.checkAndConfigure { result in
                switch result {
                case .success:
                    break
                case .failure:
                    break
                }
            }
        }
    }
}

#endif