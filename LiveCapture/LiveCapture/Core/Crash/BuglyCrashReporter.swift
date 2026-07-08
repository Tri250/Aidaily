//
//  BuglyCrashReporter.swift
//  LiveCapture
//
//  Bugly iOS 崩溃上报集成（腾讯 Bugly 国内生产环境必备）
//  替代 Firebase Crashlytics，适配国内网络环境
//

import Foundation
import UIKit

#if os(iOS)

/// Bugly 崩溃上报管理器
/// 在 App 启动时调用 `BuglyCrashReporter.start()` 初始化
final class BuglyCrashReporter {
    static let shared = BuglyCrashReporter()

    private let appId: String
    private let appKey: String
    private var isInitialized = false

    private init() {
        self.appId = Bundle.main.object(forInfoDictionaryKey: "BUGLY_APP_ID") as? String ?? ""
        self.appKey = Bundle.main.object(forInfoDictionaryKey: "BUGLY_APP_KEY") as? String ?? ""
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
        LiveCaptureLogger.shared.info("Bugly 崩溃上报已初始化 (AppID: \(appId.prefix(8))...)")

        // 注册未捕获异常处理器
        registerUncaughtExceptionHandler()

        // 注册信号处理器
        registerSignalHandlers()
    }

    private func configureBugly() {
        // ===== Bugly SDK 初始化（添加 Bugly iOS SDK 依赖后取消注释） =====
        //
        // 集成步骤：
        // 1. 通过 CocoaPods/SPM 添加 Bugly SDK
        // 2. 在 Info.plist 中添加 BUGLY_APP_ID 和 BUGLY_APP_KEY
        // 3. 取消下面代码的注释
        //
        // let config = BuglyConfig()
        // config.channel = channelName()
        // config.debugMode = false
        // config.reportLogLevel = .warn
        // config.blockMonitorTimeout = 3.0  // 卡顿监控阈值 3 秒
        // config.consoleLogEnabled = false
        // Bugly.start(withAppId: appId, config: config)

        // ===== 当前降级方案：使用系统崩溃日志 + 自定义异常上报 =====
        LiveCaptureLogger.shared.info("Bugly SDK 未集成，使用系统级崩溃捕获降级方案")
    }

    // MARK: - 渠道名称

    private func channelName() -> String {
        #if DEBUG
        return "Debug"
        #else
        // 检测是否从 App Store 下载
        if let receiptURL = Bundle.main.appStoreReceiptURL {
            if receiptURL.lastPathComponent == "sandboxReceipt" {
                return "TestFlight"
            }
            return "AppStore"
        }
        return "Enterprise"
        #endif
    }

    // MARK: - 未捕获异常处理器

    private func registerUncaughtExceptionHandler() {
        NSSetUncaughtExceptionHandler { exception in
            let reason = exception.reason ?? "Unknown"
            let name = exception.name.rawValue
            let callStack = exception.callStackSymbols.joined(separator: "\n")
            LiveCaptureLogger.shared.error("未捕获异常: \(name) - \(reason)\n调用栈:\n\(callStack)")

            // 保存崩溃信息到本地，下次启动时上报
            UserDefaults.standard.set([
                "name": name,
                "reason": reason,
                "callStack": callStack,
                "timestamp": Date().timeIntervalSince1970
            ], forKey: "livecapture.last_crash")
            UserDefaults.standard.synchronize()
        }
    }

    // MARK: - 信号处理器

    private func registerSignalHandlers() {
        // 捕获 SIGABRT, SIGSEGV, SIGBUS 等致命信号
        let signals: [Int32] = [SIGABRT, SIGSEGV, SIGBUS, SIGFPE, SIGILL, SIGTRAP]
        for sig in signals {
            signal(sig) { signal in
                let signalName: String
                switch signal {
                case SIGABRT: signalName = "SIGABRT"
                case SIGSEGV: signalName = "SIGSEGV"
                case SIGBUS: signalName = "SIGBUS"
                case SIGFPE: signalName = "SIGFPE"
                case SIGILL: signalName = "SIGILL"
                case SIGTRAP: signalName = "SIGTRAP"
                default: signalName = "Unknown(\(signal))"
                }
                LiveCaptureLogger.shared.error("收到致命信号: \(signalName)")
                UserDefaults.standard.set([
                    "signal": signalName,
                    "timestamp": Date().timeIntervalSince1970
                ], forKey: "livecapture.last_signal_crash")
                UserDefaults.standard.synchronize()
                exit(signal)
            }
        }
    }

    // MARK: - 上次崩溃检测

    /// 检测上次启动是否发生崩溃
    /// - Returns: 崩溃信息字典，如果没有崩溃返回 nil
    func checkLastCrash() -> [String: Any]? {
        if let crashInfo = UserDefaults.standard.dictionary(forKey: "livecapture.last_crash") {
            UserDefaults.standard.removeObject(forKey: "livecapture.last_crash")
            LiveCaptureLogger.shared.info("检测到上次启动发生崩溃: \(crashInfo)")
            return crashInfo
        }
        if let signalInfo = UserDefaults.standard.dictionary(forKey: "livecapture.last_signal_crash") {
            UserDefaults.standard.removeObject(forKey: "livecapture.last_signal_crash")
            LiveCaptureLogger.shared.info("检测到上次启动收到致命信号: \(signalInfo)")
            return signalInfo
        }
        return nil
    }

    // MARK: - 用户标识

    /// 设置用户标识（用于关联崩溃和用户）
    func setUserId(_ userId: String) {
        // Bugly.setUserIdentifier(userId)
        LiveCaptureLogger.shared.debug("Bugly 用户标识: \(userId)")
    }

    /// 设置自定义数据（崩溃时附带上报）
    func setCustomData(_ value: String, forKey key: String) {
        // Bugly.setUserValue(value, forKey: key)
        LiveCaptureLogger.shared.debug("Bugly 自定义数据: \(key)=\(value)")
    }

    /// 设置场景标签（帮助定位崩溃发生位置）
    func setScene(_ scene: String) {
        setCustomData(scene, forKey: "current_scene")
    }

    // MARK: - 手动上报

    /// 手动上报异常
    func reportException(_ exception: NSException) {
        // Bugly.report(exception)
        LiveCaptureLogger.shared.error("手动上报异常: \(exception.name.rawValue) - \(exception.reason ?? "Unknown")")
    }

    /// 手动上报 NSError
    func reportError(_ error: Error, file: String = #file, line: Int = #line) {
        // Bugly.reportError(error)
        let fileName = (file as NSString).lastPathComponent
        LiveCaptureLogger.shared.error("手动上报错误 [\(fileName):\(line)]: \(error.localizedDescription)")
    }

    /// 手动上报自定义错误（带分类标签）
    func reportCustomError(domain: String, code: Int, message: String) {
        let error = NSError(
            domain: "com.livecapture.\(domain)",
            code: code,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
        reportError(error)
    }
}

#endif