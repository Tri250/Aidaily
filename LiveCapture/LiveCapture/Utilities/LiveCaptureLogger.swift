//
//  LiveCaptureLogger.swift
//  LiveCapture
//
//  统一日志系统 - 生产环境自动禁用调试日志
//

import Foundation
import os.log

/// 统一日志管理器
/// 使用 os_log 作为底层，生产环境自动过滤调试级别日志
final class LiveCaptureLogger {
    static let shared = LiveCaptureLogger()

    private let subsystem = "com.livecapture.app"
    private let isDebugMode: Bool

    private init() {
        #if DEBUG
        isDebugMode = true
        #else
        isDebugMode = false
        #endif
    }

    func debug(_ message: String, file: String = #file, function: String = #function) {
        guard isDebugMode else { return }
        let log = OSLog(subsystem: subsystem, category: "debug")
        os_log(.debug, log: log, "%{public}@", message)
    }

    func info(_ message: String) {
        let log = OSLog(subsystem: subsystem, category: "info")
        os_log(.info, log: log, "%{public}@", message)
    }

    func warning(_ message: String) {
        let log = OSLog(subsystem: subsystem, category: "warning")
        os_log(.default, log: log, "⚠️ %{public}@", message)
    }

    func error(_ message: String, error: Error? = nil) {
        let log = OSLog(subsystem: subsystem, category: "error")
        if let error = error {
            os_log(.error, log: log, "❌ %{public}@ | %{public}@", message, error.localizedDescription)
        } else {
            os_log(.error, log: log, "❌ %{public}@", message)
        }
    }

    func critical(_ message: String) {
        let log = OSLog(subsystem: subsystem, category: "critical")
        os_log(.fault, log: log, "🔥 %{public}@", message)
    }
}