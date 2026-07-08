//
//  ComplianceService.swift
//  LiveCapture
//
//  国内合规统一服务：账号注销、数据删除、青少年模式、ICP备案
//

import Foundation
import SwiftUI
import Combine

#if os(iOS)

// MARK: - 账号注销与数据删除服务

/// 账号注销与数据删除服务
final class AccountDeletionService: ObservableObject {
    static let shared = AccountDeletionService()

    @Published var isDeletionInProgress = false
    @Published var deletionCompleted = false
    @Published var deletionError: String?

    /// 注销冷静期（天），默认 15 天，国内法规建议 7-30 天
    private let coolingOffDays = 15

    private let defaults = UserDefaults.standard
    private let deletionRequestKey = "livecapture.account_deletion_requested"
    private let deletionDateKey = "livecapture.account_deletion_date"

    var deletionRequested: Bool {
        defaults.bool(forKey: deletionRequestKey)
    }

    var deletionRequestDate: Date? {
        guard let timestamp = defaults.object(forKey: deletionDateKey) as? TimeInterval else { return nil }
        return Date(timeIntervalSince1970: timestamp)
    }

    var canCancelDeletion: Bool {
        deletionRequested && deletionRequestDate != nil
    }

    var remainingDays: Int {
        guard let date = deletionRequestDate else { return 0 }
        let elapsed = Date().timeIntervalSince(date)
        let remaining = Double(coolingOffDays * 24 * 60 * 60) - elapsed
        return max(0, Int(ceil(remaining / (24 * 60 * 60))))
    }

    private init() {}

    /// 请求注销账号
    func requestDeletion() {
        guard !deletionRequested else {
            deletionError = "已存在注销请求，请等待处理"
            return
        }
        defaults.set(true, forKey: deletionRequestKey)
        defaults.set(Date().timeIntervalSince1970, forKey: deletionDateKey)
        LiveCaptureLogger.shared.info("账号注销已请求，冷静期 \(coolingOffDays) 天")
    }

    /// 取消注销
    func cancelDeletion() {
        defaults.removeObject(forKey: deletionRequestKey)
        defaults.removeObject(forKey: deletionDateKey)
        LiveCaptureLogger.shared.info("账号注销已取消")
    }

    /// 立即执行数据删除（在冷静期后调用）
    func executeDataDeletion() {
        guard deletionRequested else { return }
        isDeletionInProgress = true

        // 1. 删除所有本地照片数据
        let storage = PhotoStorageService.shared
        let records = storage.loadRecords()
        for record in records {
            storage.deleteRecord(record.id)
        }

        // 2. 清除 UserDefaults
        if let bundleId = Bundle.main.bundleIdentifier {
            defaults.removePersistentDomain(forName: bundleId)
        }

        // 3. 清除缓存
        let fileManager = FileManager.default
        if let cachesURL = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first {
            try? fileManager.removeItem(at: cachesURL)
        }
        if let tmpURL = fileManager.temporaryDirectory as URL? {
            try? fileManager.contentsOfDirectory(at: tmpURL, includingPropertiesForKeys: nil).forEach {
                try? fileManager.removeItem(at: $0)
            }
        }

        // 4. 清除应用数据目录
        if let appSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first {
            let liveCaptureDir = appSupport.appendingPathComponent("LiveCapture")
            try? fileManager.removeItem(at: liveCaptureDir)
        }

        isDeletionInProgress = false
        deletionCompleted = true
        LiveCaptureLogger.shared.info("所有用户数据已删除")
    }

    /// 导出个人数据（个人信息保护法要求提供数据导出）
    func exportPersonalData() -> URL? {
        let records = PhotoStorageService.shared.loadRecords()
        var exportData: [[String: Any]] = []

        for record in records {
            exportData.append([
                "id": record.id.uuidString,
                "creationDate": ISO8601DateFormatter().string(from: record.creationDate),
                "detectionMethod": record.detectionMethod ?? "",
                "iso": record.iso ?? 0,
                "shutterSpeed": record.shutterSpeed ?? 0,
                "aperture": record.aperture ?? 0,
                "imageWidth": record.imageWidth ?? 0,
                "imageHeight": record.imageHeight ?? 0
            ])
        }

        do {
            let jsonData = try JSONSerialization.data(withJSONObject: exportData, options: .prettyPrinted)
            let tempURL = FileManager.default.temporaryDirectory
                .appendingPathComponent("LiveCapture_个人数据_\(Date().timeIntervalSince1970).json")
            try jsonData.write(to: tempURL)
            return tempURL
        } catch {
            LiveCaptureLogger.shared.error("数据导出失败: \(error)")
            return nil
        }
    }
}

// MARK: - 青少年模式管理器

/// 青少年模式管理器
final class YouthModeManager: ObservableObject {
    static let shared = YouthModeManager()

    @Published var isYouthModeEnabled: Bool {
        didSet {
            UserDefaults.standard.set(isYouthModeEnabled, forKey: youthModeKey)
            LiveCaptureLogger.shared.info("青少年模式: \(isYouthModeEnabled ? "已开启" : "已关闭")")
        }
    }

    /// 每日使用时长限制（分钟）
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

    private let youthModeKey = "livecapture.youth_mode"
    private let timeLimitKey = "livecapture.daily_time_limit"
    private let nightBanStartKey = "livecapture.night_ban_start"
    private let nightBanEndKey = "livecapture.night_ban_end"
    private let usageKey = "livecapture.today_usage"
    private var usageTimer: Timer?

    private init() {
        self.isYouthModeEnabled = UserDefaults.standard.bool(forKey: youthModeKey)
        self.dailyTimeLimit = UserDefaults.standard.integer(forKey: timeLimitKey)
        self.nightBanStartHour = UserDefaults.standard.integer(forKey: nightBanStartKey)
        self.nightBanEndHour = UserDefaults.standard.integer(forKey: nightBanEndKey)

        if dailyTimeLimit == 0 { dailyTimeLimit = 60 }
        if nightBanStartHour == 0 { nightBanStartHour = 22 }
        if nightBanEndHour == 0 { nightBanEndHour = 6 }

        loadTodayUsage()
        startUsageTracking()
    }

    /// 设置青少年模式密码（需要家长/监护人设置）
    private let passwordKey = "livecapture.youth_mode_password"
    private var hasSetPassword: Bool {
        UserDefaults.standard.string(forKey: passwordKey) != nil
    }

    func setPassword(_ password: String) {
        UserDefaults.standard.set(password, forKey: passwordKey)
    }

    func verifyPassword(_ password: String) -> Bool {
        UserDefaults.standard.string(forKey: passwordKey) == password
    }

    func toggleYouthMode(password: String) -> Bool {
        guard verifyPassword(password) else { return false }
        isYouthModeEnabled.toggle()
        return true
    }

    // MARK: - Usage Tracking

    private func startUsageTracking() {
        usageTimer?.invalidate()
        usageTimer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in
            guard let self = self, self.isYouthModeEnabled else { return }
            self.todayUsageSeconds += 60
            self.saveTodayUsage()
        }
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
}

// MARK: - ICP 备案信息

/// ICP 备案信息展示模型
struct ICPFilingInfo {
    /// ICP 备案号
    let icpNumber: String
    /// 网安备案号
    let networkSecurityNumber: String?
    /// 公司名称
    let companyName: String
    /// 备案链接
    let icpLink: String

    /// 从 Info.plist 读取备案信息
    static func fromBundle() -> ICPFilingInfo {
        let icpNumber = Bundle.main.object(forInfoDictionaryKey: "ICP_FILING_NUMBER") as? String ?? ""
        let networkSecurityNumber = Bundle.main.object(forInfoDictionaryKey: "NETWORK_SECURITY_NUMBER") as? String
        let companyName = Bundle.main.object(forInfoDictionaryKey: "COMPANY_NAME") as? String ?? ""
        let icpLink = Bundle.main.object(forInfoDictionaryKey: "ICP_LINK") as? String ?? "https://beian.miit.gov.cn"

        return ICPFilingInfo(
            icpNumber: icpNumber,
            networkSecurityNumber: networkSecurityNumber,
            companyName: companyName,
            icpLink: icpLink
        )
    }
}

// MARK: - 个人信息收集清单

/// 个人信息收集清单
struct PersonalInfoCollection {
    /// 数据类别
    enum DataCategory: String, CaseIterable {
        case camera = "相机权限"
        case photoLibrary = "相册权限"
        case microphone = "麦克风权限"
        case location = "位置信息"
        case deviceInfo = "设备信息"
        case crashLog = "崩溃日志"
        case usageData = "使用数据"

        var purpose: String {
            switch self {
            case .camera: return "用于拍摄照片和视频"
            case .photoLibrary: return "用于保存和读取照片"
            case .microphone: return "用于录制视频时采集音频"
            case .location: return "用于在照片中添加位置信息"
            case .deviceInfo: return "用于优化应用性能和兼容性"
            case .crashLog: return "用于分析和修复应用崩溃问题"
            case .usageData: return "用于改进产品功能和用户体验"
            }
        }

        var isRequired: Bool {
            switch self {
            case .camera, .photoLibrary: return true
            case .microphone, .location: return false
            case .deviceInfo, .crashLog, .usageData: return false
            }
        }
    }

    /// 所有数据类别及其用途
    static let allCategories: [(category: DataCategory, isEnabled: Bool)] = [
        (.camera, true),
        (.photoLibrary, true),
        (.microphone, false),
        (.location, false),
        (.deviceInfo, true),
        (.crashLog, true),
        (.usageData, false),
    ]
}

#endif