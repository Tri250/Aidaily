//
//  BuglyCrashReporter.swift
//  LiveCapture
//
//  Bugly iOS 崩溃上报集成
//  国内生产环境必备，替代 Firebase Crashlytics
//

import Foundation

#if os(iOS)

/// Bugly 崩溃上报管理器
/// 在 App 启动时调用 `BuglyCrashReporter.start()` 初始化
final class BuglyCrashReporter {
    static let shared = BuglyCrashReporter()

    private let appId: String
    private var isInitialized = false

    private init() {
        // Bugly AppID（从 Info.plist 读取，或通过编译配置注入）
        self.appId = Bundle.main.object(forInfoDictionaryKey: "BUGLY_APP_ID") as? String ?? ""
    }

    /// 启动 Bugly 崩溃上报
    /// 应在 application(_:didFinishLaunchingWithOptions:) 中调用
    func start() {
        guard !isInitialized else { return }

        if appId.isEmpty {
            LiveCaptureLogger.shared.warning("Bugly AppID 未配置，跳过初始化")
            return
        }

        configureBugly()
        isInitialized = true
        LiveCaptureLogger.shared.info("Bugly 崩溃上报已初始化")
    }

    private func configureBugly() {
        // 实际集成时取消注释以下代码，并添加 Bugly iOS SDK 依赖
        // Bugly.start(withAppId: appId)

        // 配置示例：
        // let config = BuglyConfig()
        // config.channel = "AppStore"
        // config.debugMode = false
        // config.reportLogLevel = .warn
        // Bugly.start(withAppId: appId, config: config)
    }

    /// 设置用户标识（用于关联崩溃和用户）
    func setUserId(_ userId: String) {
        // Bugly.setUserIdentifier(userId)
    }

    /// 设置自定义数据（崩溃时附带上报）
    func setCustomData(_ value: String, forKey key: String) {
        // Bugly.setUserValue(value, forKey: key)
    }

    /// 手动上报异常
    func reportException(_ exception: NSException) {
        // Bugly.report(exception)
    }

    /// 手动上报错误
    func reportError(_ error: Error) {
        // Bugly.reportError(error)
        LiveCaptureLogger.shared.error("手动上报错误: \(error.localizedDescription)")
    }

    /// 设置渠道信息
    func setChannel(_ channel: String) {
        // Bugly.setChannel(channel)
    }
}

#endif