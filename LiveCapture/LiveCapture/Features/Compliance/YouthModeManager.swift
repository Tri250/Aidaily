//
//  YouthModeManager.swift
//  LiveCapture
//
//  青少年模式管理器 - 独立管理
//  功能：时长限制、社区/分享禁用、密码保护、每日追踪
//

import Foundation
import SwiftUI
import Combine
import Security

#if os(iOS)

/// 青少年模式管理器
/// 负责管理青少年模式的所有状态和限制
final class YouthModeManager: ObservableObject {
    static let shared = YouthModeManager()

    // MARK: - Published Properties

    /// 青少年模式是否启用
    @Published var isYouthModeEnabled: Bool {
        didSet {
            UserDefaults.standard.set(isYouthModeEnabled, forKey: youthModeKey)
            LiveCaptureLogger.shared.info("青少年模式: \(isYouthModeEnabled ? "已开启" : "已关闭")")
            if isYouthModeEnabled {
                startUsageTracking()
            } else {
                stopUsageTracking()
            }
        }
    }

    /// 每日使用时长限制（分钟），默认 40 分钟
    @Published var dailyTimeLimit: Int {
        didSet {
            UserDefaults.standard.set(dailyTimeLimit, forKey: timeLimitKey)
        }
    }

    /// 今日已使用时长（秒）
    @Published var todayUsageSeconds: TimeInterval = 0

    /// 夜间禁用开始时间（小时，22 表示晚上 10 点）
    @Published var nightBanStartHour: Int {
        didSet {
            UserDefaults.standard.set(nightBanStartHour, forKey: nightBanStartKey)
        }
    }

    /// 夜间禁用结束时间（小时，6 表示早上 6 点）
    @Published var nightBanEndHour: Int {
        didSet {
            UserDefaults.standard.set(nightBanEndHour, forKey: nightBanEndKey)
        }
    }

    /// 是否禁用社区功能
    @Published var isCommunityDisabled: Bool = true

    /// 是否禁用分享功能
    @Published var isSharingDisabled: Bool = true

    // MARK: - Computed Properties

    /// 是否在夜间禁用时段
    var isInNightBanPeriod: Bool {
        let hour = Calendar.current.component(.hour, from: Date())
        if nightBanStartHour < nightBanEndHour {
            return hour >= nightBanStartHour && hour < nightBanEndHour
        } else {
            return hour >= nightBanStartHour || hour < nightBanEndHour
        }
    }

    /// 是否超过每日时长限制
    var isDailyLimitExceeded: Bool {
        todayUsageSeconds >= TimeInterval(dailyTimeLimit * 60)
    }

    /// 今日剩余可用时长（秒）
    var remainingSeconds: TimeInterval {
        let limit = TimeInterval(dailyTimeLimit * 60)
        let remaining = limit - todayUsageSeconds
        return max(0, remaining)
    }

    /// 今日剩余可用时长（格式化字符串）
    var remainingTimeFormatted: String {
        let minutes = Int(remainingSeconds / 60)
        if minutes < 60 {
            return "\(minutes) 分钟"
        } else {
            let hours = minutes / 60
            let mins = minutes % 60
            return "\(hours) 小时 \(mins) 分钟"
        }
    }

    /// 今日使用时长（格式化字符串）
    var todayUsageFormatted: String {
        let minutes = Int(todayUsageSeconds / 60)
        if minutes < 60 {
            return "\(minutes) 分钟"
        } else {
            let hours = minutes / 60
            let mins = minutes % 60
            return "\(hours) 小时 \(mins) 分钟"
        }
    }

    /// 是否因时长限制被锁定
    var isLockedByTimeLimit: Bool {
        isYouthModeEnabled && isDailyLimitExceeded
    }

    /// 是否因夜间禁用被锁定
    var isLockedByNightBan: Bool {
        isYouthModeEnabled && isInNightBanPeriod
    }

    /// 是否允许使用应用（综合判断）
    var canUseApp: Bool {
        if !isYouthModeEnabled { return true }
        if isInNightBanPeriod { return false }
        if isDailyLimitExceeded { return false }
        return true
    }

    // MARK: - Private Properties

    private let youthModeKey = "livecapture.youth_mode"
    private let timeLimitKey = "livecapture.daily_time_limit"
    private let nightBanStartKey = "livecapture.night_ban_start"
    private let nightBanEndKey = "livecapture.night_ban_end"
    private let usageKey = "livecapture.today_usage"
    private let communityDisabledKey = "livecapture.community_disabled"
    private let sharingDisabledKey = "livecapture.sharing_disabled"

    private var usageTimer: Timer?
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Initialization

    private init() {
        let defaults = UserDefaults.standard

        self.isYouthModeEnabled = defaults.bool(forKey: youthModeKey)
        self.dailyTimeLimit = defaults.integer(forKey: timeLimitKey)
        self.nightBanStartHour = defaults.integer(forKey: nightBanStartKey)
        self.nightBanEndHour = defaults.integer(forKey: nightBanEndKey)

        // 默认值设定
        if dailyTimeLimit == 0 { dailyTimeLimit = 40 }  // 默认 40 分钟/日
        if nightBanStartHour == 0 { nightBanStartHour = 22 }
        if nightBanEndHour == 0 { nightBanEndHour = 6 }

        // 社区和分享限制默认值
        if defaults.object(forKey: communityDisabledKey) == nil {
            self.isCommunityDisabled = true
        } else {
            self.isCommunityDisabled = defaults.bool(forKey: communityDisabledKey)
        }
        if defaults.object(forKey: sharingDisabledKey) == nil {
            self.isSharingDisabled = true
        } else {
            self.isSharingDisabled = defaults.bool(forKey: sharingDisabledKey)
        }

        loadTodayUsage()
        if isYouthModeEnabled {
            startUsageTracking()
        }
    }

    deinit {
        stopUsageTracking()
    }

    // MARK: - Password Management (Keychain)

    /// Keychain 服务标识
    private let keychainService = "com.livecapture.youth_mode"
    private let keychainAccount = "youth_mode_password"

    /// 是否已设置密码
    var hasSetPassword: Bool {
        loadPasswordFromKeychain() != nil
    }

    /// 设置密码（使用 Keychain 安全存储）
    func setPassword(_ password: String) {
        savePasswordToKeychain(password)
        LiveCaptureLogger.shared.info("青少年模式密码已设置")
    }

    /// 验证密码
    func verifyPassword(_ password: String) -> Bool {
        loadPasswordFromKeychain() == password
    }

    // MARK: - Keychain Helpers

    private func savePasswordToKeychain(_ password: String) {
        let data = password.data(using: .utf8) ?? Data()
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount
        ]
        SecItemDelete(query as CFDictionary)
        var attributes = query
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        SecItemAdd(attributes as CFDictionary, nil)
    }

    private func loadPasswordFromKeychain() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    /// 切换青少年模式（需要密码验证）
    func toggleYouthMode(password: String) -> Bool {
        guard verifyPassword(password) else { return false }
        isYouthModeEnabled.toggle()
        return true
    }

    /// 通过密码关闭青少年模式
    func disableWithPassword(_ password: String) -> Bool {
        guard verifyPassword(password) else { return false }
        isYouthModeEnabled = false
        return true
    }

    // MARK: - Community & Sharing Controls

    /// 更新社区功能禁用状态
    func setCommunityDisabled(_ disabled: Bool) {
        isCommunityDisabled = disabled
        UserDefaults.standard.set(disabled, forKey: communityDisabledKey)
    }

    /// 更新分享功能禁用状态
    func setSharingDisabled(_ disabled: Bool) {
        isSharingDisabled = disabled
        UserDefaults.standard.set(disabled, forKey: sharingDisabledKey)
    }

    // MARK: - Usage Tracking

    private func startUsageTracking() {
        usageTimer?.invalidate()
        usageTimer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in
            guard let self = self, self.isYouthModeEnabled else { return }
            self.todayUsageSeconds += 60
            self.saveTodayUsage()

            // 检查是否超过每日限制
            if self.isDailyLimitExceeded {
                LiveCaptureLogger.shared.info("青少年模式：今日使用时长已达上限")
                NotificationCenter.default.post(name: .youthModeTimeLimitReached, object: nil)
            }
        }
        // 确保定时器在 RunLoop 中运行
        if let timer = usageTimer {
            RunLoop.main.add(timer, forMode: .common)
        }
    }

    private func stopUsageTracking() {
        usageTimer?.invalidate()
        usageTimer = nil
    }

    private func loadTodayUsage() {
        let today = Calendar.current.startOfDay(for: Date())
        let saved = UserDefaults.standard.dictionary(forKey: usageKey) as? [String: TimeInterval] ?? [:]
        let key = dateKey(today)
        todayUsageSeconds = saved[key] ?? 0
    }

    private func saveTodayUsage() {
        let today = Calendar.current.startOfDay(for: Date())
        var saved = UserDefaults.standard.dictionary(forKey: usageKey) as? [String: TimeInterval] ?? [:]
        saved[dateKey(today)] = todayUsageSeconds
        UserDefaults.standard.set(saved, forKey: usageKey)
    }

    private func dateKey(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    // MARK: - History

    /// 获取最近 7 天的使用记录
    func recentUsageHistory() -> [(date: String, seconds: TimeInterval)] {
        let saved = UserDefaults.standard.dictionary(forKey: usageKey) as? [String: TimeInterval] ?? [:]
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())

        return (0..<7).compactMap { daysAgo in
            guard let date = calendar.date(byAdding: .day, value: -daysAgo, to: today) else { return nil }
            let key = dateKey(date)
            let seconds = saved[key] ?? 0
            return (key, seconds)
        }.sorted { $0.date < $1.date }
    }

    /// 清除所有使用记录
    func clearUsageHistory() {
        UserDefaults.standard.removeObject(forKey: usageKey)
        todayUsageSeconds = 0
    }

    /// 重置当日使用时长（跨天时自动调用）
    func resetIfNewDay() {
        let today = Calendar.current.startOfDay(for: Date())
        let saved = UserDefaults.standard.dictionary(forKey: usageKey) as? [String: TimeInterval] ?? [:]
        let key = dateKey(today)
        if saved[key] == nil {
            todayUsageSeconds = 0
        }
    }
}

// MARK: - Notification Names

extension Notification.Name {
    /// 青少年模式每日时长达到上限
    static let youthModeTimeLimitReached = Notification.Name("livecapture.youth_mode_time_limit_reached")
    /// 青少年模式夜间禁用时段开始
    static let youthModeNightBanStarted = Notification.Name("livecapture.youth_mode_night_ban_started")
    /// 青少年模式状态变更
    static let youthModeStateChanged = Notification.Name("livecapture.youth_mode_state_changed")
}

#endif