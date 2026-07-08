//
//  MemoryMonitor.swift
//  LiveCapture
//
//  内存监控系统 - 追踪内存使用，阈值告警，自动释放 CIContext 缓存
//

import Foundation
import UIKit
import CoreImage

#if os(iOS)

/// 内存监控器
final class MemoryMonitor: ObservableObject {
    static let shared = MemoryMonitor()

    // MARK: - Published 属性

    /// 当前内存使用量（MB）
    @Published private(set) var currentMemoryMB: Double = 0
    /// 内存使用峰值（MB）
    @Published private(set) var peakMemoryMB: Double = 0
    /// 是否处于内存压力状态
    @Published private(set) var isUnderMemoryPressure: Bool = false
    /// 内存告警等级
    @Published private(set) var warningLevel: MemoryWarningLevel = .normal
    /// 内存使用历史（最近 60 个采样点）
    @Published private(set) var memoryHistory: [Double] = []

    // MARK: - 配置

    /// 内存警告阈值（MB），默认 200MB
    var warningThresholdMB: Double = 200
    /// 高内存压力阈值（MB），默认 300MB
    var criticalThresholdMB: Double = 300
    /// 采样间隔（秒），默认 2.0
    var samplingInterval: TimeInterval = 2.0
    /// 是否启用自动监控
    var isMonitoringEnabled: Bool = false {
        didSet {
            if isMonitoringEnabled {
                startMonitoring()
            } else {
                stopMonitoring()
            }
        }
    }

    // MARK: - 私有属性

    private var timer: Timer?
    private var maxHistoryCount = 60
    private var registeredCIContexts: [CIContext] = []

    private init() {
        registerMemoryWarningObserver()
    }

    deinit {
        stopMonitoring()
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: - 内存警告等级

    enum MemoryWarningLevel: String {
        case normal = "正常"
        case warning = "警告"
        case critical = "严重"

        var color: String {
            switch self {
            case .normal: return "green"
            case .warning: return "yellow"
            case .critical: return "red"
            }
        }
    }

    // MARK: - 监控控制

    /// 开始内存监控
    func startMonitoring() {
        guard timer == nil else { return }

        // 立即采样一次
        sampleMemory()

        timer = Timer.scheduledTimer(withTimeInterval: samplingInterval, repeats: true) { [weak self] _ in
            self?.sampleMemory()
        }

        LiveCaptureLogger.shared.info("内存监控已启动（阈值: \(Int(warningThresholdMB))MB / \(Int(criticalThresholdMB))MB）")
    }

    /// 停止内存监控
    func stopMonitoring() {
        timer?.invalidate()
        timer = nil
        LiveCaptureLogger.shared.info("内存监控已停止")
    }

    // MARK: - 内存采样

    /// 执行一次内存采样
    @discardableResult
    func sampleMemory() -> Double {
        let memoryMB = getCurrentMemoryUsage()
        let timestamp = Date()

        currentMemoryMB = memoryMB

        // 更新峰值
        if memoryMB > peakMemoryMB {
            peakMemoryMB = memoryMB
        }

        // 更新历史
        memoryHistory.append(memoryMB)
        if memoryHistory.count > maxHistoryCount {
            memoryHistory.removeFirst()
        }

        // 检查告警等级
        updateWarningLevel(memoryMB: memoryMB)

        // 如果达到严重级别，自动释放 CIContext 缓存
        if warningLevel == .critical {
            handleMemoryPressure()
            LiveCaptureLogger.shared.warning(
                "内存压力严重: \(String(format: "%.1f", memoryMB))MB (峰值: \(String(format: "%.1f", peakMemoryMB))MB)"
            )
        } else if warningLevel == .warning {
            LiveCaptureLogger.shared.debug(
                "内存使用较高: \(String(format: "%.1f", memoryMB))MB"
            )
        }

        return memoryMB
    }

    // MARK: - 内存使用获取

    /// 获取当前应用内存使用量（MB）
    func getCurrentMemoryUsage() -> Double {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4

        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(
                    mach_task_self_,
                    task_flavor_t(MACH_TASK_BASIC_INFO),
                    $0,
                    &count
                )
            }
        }

        if result == KERN_SUCCESS {
            let bytes = Double(info.resident_size)
            return bytes / (1024.0 * 1024.0)
        }

        return 0
    }

    /// 获取设备总内存（MB）
    func getTotalDeviceMemoryMB() -> Double {
        let physicalMemory = ProcessInfo.processInfo.physicalMemory
        return Double(physicalMemory) / (1024.0 * 1024.0)
    }

    /// 获取可用内存（MB）
    func getAvailableMemoryMB() -> Double {
        var pagesize: vm_size_t = 0
        let hostPort = mach_host_self()
        var hostSize = mach_msg_type_number_t(MemoryLayout<vm_statistics_data_t>.size / MemoryLayout<integer_t>.size)
        host_page_size(hostPort, &pagesize)

        var hostInfo = vm_statistics_data_t()
        _ = withUnsafeMutablePointer(to: &hostInfo) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(hostSize)) {
                host_statistics(hostPort, HOST_VM_INFO, $0, &hostSize)
            }
        }

        let freeBytes = Double(hostInfo.free_count + hostInfo.inactive_count) * Double(pagesize)
        return freeBytes / (1024.0 * 1024.0)
    }

    // MARK: - 内存压力处理

    /// 处理内存压力
    private func handleMemoryPressure() {
        // 释放所有注册的 CIContext 缓存
        clearCIContextCaches()

        // 触发内存警告处理
        performMemoryCleanup()
    }

    /// 清除所有 CIContext 缓存
    func clearCIContextCaches() {
        for context in registeredCIContexts {
            context.clearCaches()
        }
        LiveCaptureLogger.shared.info("已清除 \(registeredCIContexts.count) 个 CIContext 缓存")
    }

    /// 注册 CIContext 以便内存压力时自动清理
    func registerCIContext(_ context: CIContext) {
        guard !registeredCIContexts.contains(where: { $0 === context }) else { return }
        registeredCIContexts.append(context)
    }

    /// 取消注册 CIContext
    func unregisterCIContext(_ context: CIContext) {
        registeredCIContexts.removeAll { $0 === context }
    }

    /// 执行内存清理
    private func performMemoryCleanup() {
        // 清除 URLCache
        URLCache.shared.removeAllCachedResponses()

        // 通知系统内存压力
        // 系统会自动处理 UIImage 缓存等
    }

    // MARK: - 内存警告观察

    /// 注册 UIApplication 内存警告通知
    private func registerMemoryWarningObserver() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleMemoryWarningNotification),
            name: UIApplication.didReceiveMemoryWarningNotification,
            object: nil
        )
    }

    @objc private func handleMemoryWarningNotification() {
        isUnderMemoryPressure = true
        LiveCaptureLogger.shared.warning(
            "收到系统内存警告！当前内存: \(String(format: "%.1f", currentMemoryMB))MB"
        )

        // 立即释放 CIContext 缓存
        clearCIContextCaches()
        performMemoryCleanup()

        // 采样一次
        sampleMemory()

        // 延迟恢复压力状态
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { [weak self] in
            self?.isUnderMemoryPressure = false
        }
    }

    // MARK: - 告警等级更新

    private func updateWarningLevel(memoryMB: Double) {
        if memoryMB >= criticalThresholdMB {
            warningLevel = .critical
        } else if memoryMB >= warningThresholdMB {
            warningLevel = .warning
        } else {
            warningLevel = .normal
        }
    }

    // MARK: - 统计信息

    /// 获取内存使用统计摘要
    var memoryStats: MemoryStats {
        let avg = memoryHistory.isEmpty ? 0 : memoryHistory.reduce(0, +) / Double(memoryHistory.count)
        let min = memoryHistory.min() ?? 0
        let max = memoryHistory.max() ?? 0

        return MemoryStats(
            currentMB: currentMemoryMB,
            averageMB: avg,
            minMB: min,
            maxMB: max,
            peakMB: peakMemoryMB,
            totalDeviceMB: getTotalDeviceMemoryMB(),
            availableMB: getAvailableMemoryMB(),
            warningLevel: warningLevel
        )
    }

    struct MemoryStats {
        let currentMB: Double
        let averageMB: Double
        let minMB: Double
        let maxMB: Double
        let peakMB: Double
        let totalDeviceMB: Double
        let availableMB: Double
        let warningLevel: MemoryWarningLevel
    }

    // MARK: - 重置

    /// 重置内存统计
    func reset() {
        peakMemoryMB = 0
        memoryHistory.removeAll()
        warningLevel = .normal
        isUnderMemoryPressure = false
    }
}

// MARK: - MemoryUsageView（内存监控 UI 视图）

/// 内存监控视图（调试用）
struct MemoryUsageView: View {
    @ObservedObject var monitor = MemoryMonitor.shared

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("内存")
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textSecondary)

                Spacer()

                Text(warningLevelText)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(warningLevelColor)
            }

            // 进度条
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(DesignSystem.Colors.gray3)
                        .frame(height: 4)

                    RoundedRectangle(cornerRadius: 2)
                        .fill(warningLevelColor)
                        .frame(width: min(geo.size.width * CGFloat(monitor.currentMemoryMB / monitor.criticalThresholdMB), geo.size.width), height: 4)
                }
            }
            .frame(height: 4)

            HStack {
                Text(String(format: "%.0f MB", monitor.currentMemoryMB))
                    .font(DesignSystem.Typography.monoCaption)
                    .foregroundColor(DesignSystem.Colors.textPrimary)

                Spacer()

                Text("峰值: \(String(format: "%.0f", monitor.peakMemoryMB)) MB")
                    .font(DesignSystem.Typography.caption2)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
    }

    private var warningLevelText: String {
        monitor.warningLevel.rawValue
    }

    private var warningLevelColor: Color {
        switch monitor.warningLevel {
        case .normal: return DesignSystem.Colors.success
        case .warning: return DesignSystem.Colors.warning
        case .critical: return DesignSystem.Colors.error
        }
    }
}

#endif