//
//  AppErrorHandler.swift
//  LiveCapture
//
//  集中错误处理系统 - 统一的错误类型、恢复建议、错误告警视图
//  集成 BuglyCrashReporter 进行崩溃上报
//

import Foundation
import SwiftUI

#if os(iOS)

// MARK: - AppError 枚举

/// 应用程序统一错误类型
enum AppError: Error, Identifiable, Equatable {
    // 相机错误
    case cameraNotAuthorized
    case cameraUnavailable
    case cameraSetupFailed(String)
    case cameraCaptureFailed
    case cameraSwitchFailed

    // 存储错误
    case storageSaveFailed(String)
    case storageLoadFailed(String)
    case storageDeleteFailed(String)
    case storageDiskFull

    // 网络错误
    case networkUnavailable
    case networkTimeout
    case networkServerError(Int)

    // 处理错误
    case processingFilterFailed
    case processingEnhanceFailed
    case processingExportFailed
    case processingTimeout

    // 权限错误
    case permissionCameraDenied
    case permissionPhotoLibraryDenied
    case permissionMicrophoneDenied

    // 通用错误
    case unknown(String)

    var id: String {
        switch self {
        case .cameraNotAuthorized: return "camera_not_authorized"
        case .cameraUnavailable: return "camera_unavailable"
        case .cameraSetupFailed(let msg): return "camera_setup_\(msg)"
        case .cameraCaptureFailed: return "camera_capture_failed"
        case .cameraSwitchFailed: return "camera_switch_failed"
        case .storageSaveFailed(let msg): return "storage_save_\(msg)"
        case .storageLoadFailed(let msg): return "storage_load_\(msg)"
        case .storageDeleteFailed(let msg): return "storage_delete_\(msg)"
        case .storageDiskFull: return "storage_disk_full"
        case .networkUnavailable: return "network_unavailable"
        case .networkTimeout: return "network_timeout"
        case .networkServerError(let code): return "network_server_\(code)"
        case .processingFilterFailed: return "processing_filter"
        case .processingEnhanceFailed: return "processing_enhance"
        case .processingExportFailed: return "processing_export"
        case .processingTimeout: return "processing_timeout"
        case .permissionCameraDenied: return "permission_camera"
        case .permissionPhotoLibraryDenied: return "permission_photo_library"
        case .permissionMicrophoneDenied: return "permission_microphone"
        case .unknown(let msg): return "unknown_\(msg)"
        }
    }

    /// 中文错误描述
    var localizedDescription: String {
        switch self {
        case .cameraNotAuthorized:
            return "相机未授权"
        case .cameraUnavailable:
            return "相机不可用"
        case .cameraSetupFailed(let detail):
            return "相机初始化失败：\(detail)"
        case .cameraCaptureFailed:
            return "拍照失败"
        case .cameraSwitchFailed:
            return "摄像头切换失败"
        case .storageSaveFailed(let detail):
            return "保存失败：\(detail)"
        case .storageLoadFailed(let detail):
            return "加载失败：\(detail)"
        case .storageDeleteFailed(let detail):
            return "删除失败：\(detail)"
        case .storageDiskFull:
            return "存储空间不足"
        case .networkUnavailable:
            return "网络不可用"
        case .networkTimeout:
            return "网络请求超时"
        case .networkServerError(let code):
            return "服务器错误（\(code)）"
        case .processingFilterFailed:
            return "滤镜处理失败"
        case .processingEnhanceFailed:
            return "图像增强失败"
        case .processingExportFailed:
            return "导出失败"
        case .processingTimeout:
            return "处理超时"
        case .permissionCameraDenied:
            return "相机权限被拒绝"
        case .permissionPhotoLibraryDenied:
            return "相册权限被拒绝"
        case .permissionMicrophoneDenied:
            return "麦克风权限被拒绝"
        case .unknown(let detail):
            return "未知错误：\(detail)"
        }
    }

    /// 恢复建议
    var recoverySuggestion: String {
        switch self {
        case .cameraNotAuthorized:
            return "请在「设置」>「隐私」>「相机」中允许 LiveCapture 访问相机"
        case .cameraUnavailable:
            return "请检查是否有其他应用正在使用相机，或尝试重启设备"
        case .cameraSetupFailed:
            return "请尝试重启应用，如果问题持续请检查设备摄像头是否正常"
        case .cameraCaptureFailed:
            return "请检查相机是否正常运行，尝试切换镜头或重启应用"
        case .cameraSwitchFailed:
            return "切换镜头时出现问题，请尝试返回主界面重新进入"
        case .storageSaveFailed:
            return "请检查设备存储空间是否充足，尝试清理一些不需要的照片"
        case .storageLoadFailed:
            return "数据可能已损坏，请尝试重新打开应用"
        case .storageDeleteFailed:
            return "文件可能已被删除，请尝试刷新列表"
        case .storageDiskFull:
            return "设备存储空间不足，请清理一些不必要的文件后重试"
        case .networkUnavailable:
            return "请检查网络连接，确保 Wi-Fi 或蜂窝数据已开启"
        case .networkTimeout:
            return "网络响应较慢，请检查网络状态后重试"
        case .networkServerError:
            return "服务器暂时不可用，请稍后重试"
        case .processingFilterFailed:
            return "滤镜处理失败，请尝试选择其他滤镜"
        case .processingEnhanceFailed:
            return "图像增强失败，请尝试使用其他编辑工具"
        case .processingExportFailed:
            return "导出失败，请检查存储空间是否充足"
        case .processingTimeout:
            return "处理时间过长，请尝试缩小图片尺寸后重试"
        case .permissionCameraDenied:
            return "请在「设置」>「隐私」>「相机」中允许 LiveCapture 访问相机"
        case .permissionPhotoLibraryDenied:
            return "请在「设置」>「隐私」>「照片」中允许 LiveCapture 访问相册"
        case .permissionMicrophoneDenied:
            return "请在「设置」>「隐私」>「麦克风」中允许 LiveCapture 访问麦克风"
        case .unknown:
            return "请尝试重启应用，如果问题持续请联系技术支持"
        }
    }

    /// 错误类型分类
    var category: String {
        switch self {
        case .cameraNotAuthorized, .cameraUnavailable, .cameraSetupFailed, .cameraCaptureFailed, .cameraSwitchFailed:
            return "相机"
        case .storageSaveFailed, .storageLoadFailed, .storageDeleteFailed, .storageDiskFull:
            return "存储"
        case .networkUnavailable, .networkTimeout, .networkServerError:
            return "网络"
        case .processingFilterFailed, .processingEnhanceFailed, .processingExportFailed, .processingTimeout:
            return "处理"
        case .permissionCameraDenied, .permissionPhotoLibraryDenied, .permissionMicrophoneDenied:
            return "权限"
        case .unknown:
            return "未知"
        }
    }
}

// MARK: - 全局错误处理器

/// 全局错误处理器，可从任何地方调用
final class AppErrorHandler: ObservableObject {
    static let shared = AppErrorHandler()

    /// 当前需要显示的错误
    @Published var currentError: AppError?
    /// 是否显示错误弹窗
    @Published var showErrorAlert: Bool = false
    /// 错误历史记录
    @Published private(set) var errorHistory: [LoggedError] = []

    /// 最大错误历史记录数
    private let maxHistoryCount = 100

    private init() {
        checkLastCrash()
    }

    // MARK: - 启动时检查上次崩溃

    /// 检查上次启动是否发生崩溃，并上报
    private func checkLastCrash() {
        if let crashInfo = BuglyCrashReporter.shared.checkLastCrash() {
            let reason = crashInfo["reason"] as? String ?? crashInfo["signal"] as? String ?? "Unknown"
            LiveCaptureLogger.shared.error("检测到上次启动发生崩溃: \(crashInfo)")
            // 记录为静默错误
            logSilently(.unknown("上次崩溃: \(reason)"), file: "AppErrorHandler", line: #line, function: "checkLastCrash()")
        }
    }

    // MARK: - 错误处理

    /// 处理并显示错误
    /// - Parameters:
    ///   - error: AppError 实例
    ///   - file: 调用文件
    ///   - line: 调用行号
    ///   - function: 调用函数
    func handle(_ error: AppError, file: String = #file, line: Int = #line, function: String = #function) {
        // 记录到日志
        logError(error, file: file, line: line, function: function)

        // 上报到崩溃系统
        reportToBugly(error, file: file, line: line)

        // UI 显示
        DispatchQueue.main.async { [weak self] in
            self?.currentError = error
            self?.showErrorAlert = true
        }
    }

    /// 处理系统 Error（转换为 AppError）
    func handle(_ error: Error, file: String = #file, line: Int = #line, function: String = #function) {
        let appError = mapToAppError(error)
        handle(appError, file: file, line: line, function: function)
    }

    /// 静默记录错误（不显示 UI）
    func logSilently(_ error: AppError, file: String = #file, line: Int = #line, function: String = #function) {
        logError(error, file: file, line: line, function: function)
        reportToBugly(error, file: file, line: line)
    }

    /// 关闭错误弹窗
    func dismissError() {
        showErrorAlert = false
        currentError = nil
    }

    // MARK: - 私有方法

    /// 记录错误日志
    private func logError(_ error: AppError, file: String, line: Int, function: String) {
        let fileName = (file as NSString).lastPathComponent
        let timestamp = Date()
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        formatter.locale = Locale(identifier: "zh_CN")
        let timestampString = formatter.string(from: timestamp)

        let logEntry = LoggedError(
            error: error,
            timestamp: timestamp,
            file: fileName,
            line: line,
            function: function
        )

        // 添加到历史
        if errorHistory.count >= maxHistoryCount {
            errorHistory.removeFirst()
        }
        errorHistory.append(logEntry)

        // 输出到控制台
        LiveCaptureLogger.shared.error(
            "[\(timestampString)] [\(error.category)] \(error.localizedDescription) | \(fileName):\(line) \(function)",
            error: error
        )
    }

    /// 上报到 Bugly
    private func reportToBugly(_ error: AppError, file: String, line: Int) {
        let fileName = (file as NSString).lastPathComponent
        BuglyCrashReporter.shared.reportCustomError(
            domain: "error",
            code: error.id.hashValue,
            message: "[\(error.category)] \(error.localizedDescription) - \(fileName):\(line)"
        )
    }

    /// 将系统 Error 映射为 AppError
    private func mapToAppError(_ error: Error) -> AppError {
        if let appError = error as? AppError {
            return appError
        }

        let nsError = error as NSError
        switch nsError.domain {
        case NSCocoaErrorDomain:
            if nsError.code == NSFileWriteOutOfSpaceError {
                return .storageDiskFull
            }
            return .storageSaveFailed(nsError.localizedDescription)
        case NSURLErrorDomain:
            switch nsError.code {
            case NSURLErrorNotConnectedToInternet, NSURLErrorDataNotAllowed:
                return .networkUnavailable
            case NSURLErrorTimedOut:
                return .networkTimeout
            default:
                return .networkServerError(nsError.code)
            }
        case AVFoundationErrorDomain:
            return .cameraSetupFailed(nsError.localizedDescription)
        default:
            return .unknown(nsError.localizedDescription)
        }
    }
}

// MARK: - 已记录的错误

struct LoggedError: Identifiable {
    let id = UUID()
    let error: AppError
    let timestamp: Date
    let file: String
    let line: Int
    let function: String

    var formattedTimestamp: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter.string(from: timestamp)
    }
}

// MARK: - ErrorAlertView

/// 可复用的错误弹窗视图
struct ErrorAlertView: View {
    let error: AppError
    let onDismiss: () -> Void
    let onRetry: (() -> Void)?

    init(error: AppError, onDismiss: @escaping () -> Void, onRetry: (() -> Void)? = nil) {
        self.error = error
        self.onDismiss = onDismiss
        self.onRetry = onRetry
    }

    var body: some View {
        VStack(spacing: 0) {
            // 错误图标
            ZStack {
                Circle()
                    .fill(DesignSystem.Colors.errorBg)
                    .frame(width: 64, height: 64)

                Image(systemName: errorIcon)
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundColor(DesignSystem.Colors.error)
            }
            .padding(.top, 28)
            .padding(.bottom, 16)

            // 错误标题
            Text(error.localizedDescription)
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textPrimary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
                .padding(.bottom, 8)

            // 恢复建议
            Text(error.recoverySuggestion)
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
                .padding(.bottom, 24)

            // 按钮
            Divider()
                .background(DesignSystem.Colors.gray3)

            HStack(spacing: 0) {
                // 关闭按钮
                Button {
                    HapticManager.shared.light()
                    onDismiss()
                } label: {
                    Text("关闭")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                }

                if onRetry != nil {
                    Divider()
                        .background(DesignSystem.Colors.gray3)

                    // 重试按钮
                    Button {
                        HapticManager.shared.light()
                        onRetry?()
                    } label: {
                        Text("重试")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.primary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                    }
                }
            }
        }
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.xLarge)
                .fill(DesignSystem.Colors.backgroundPrimary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.xLarge)
                .strokeBorder(DesignSystem.Colors.gray3, lineWidth: 0.5)
        )
        .subtleShadow()
        .padding(.horizontal, 40)
    }

    private var errorIcon: String {
        switch error {
        case .cameraNotAuthorized, .cameraUnavailable, .cameraSetupFailed,
             .cameraCaptureFailed, .cameraSwitchFailed:
            return "camera.fill.badge.ellipsis"
        case .storageSaveFailed, .storageLoadFailed, .storageDeleteFailed, .storageDiskFull:
            return "externaldrive.fill.badge.exclamationmark"
        case .networkUnavailable, .networkTimeout, .networkServerError:
            return "wifi.slash"
        case .processingFilterFailed, .processingEnhanceFailed,
             .processingExportFailed, .processingTimeout:
            return "photo.badge.exclamationmark"
        case .permissionCameraDenied, .permissionPhotoLibraryDenied, .permissionMicrophoneDenied:
            return "lock.shield.fill"
        case .unknown:
            return "exclamationmark.triangle.fill"
        }
    }
}

// MARK: - View 扩展

extension View {
    /// 全局错误弹窗修饰器
    func appErrorAlert(
        error: Binding<AppError?>,
        isPresented: Binding<Bool>,
        onRetry: (() -> Void)? = nil
    ) -> some View {
        self.overlay(
            Group {
                if isPresented.wrappedValue, let appError = error.wrappedValue {
                    ZStack {
                        Color.black.opacity(0.4)
                            .ignoresSafeArea()
                            .onTapGesture {
                                AppErrorHandler.shared.dismissError()
                            }

                        ErrorAlertView(
                            error: appError,
                            onDismiss: {
                                AppErrorHandler.shared.dismissError()
                            },
                            onRetry: onRetry
                        )
                    }
                    .animation(DesignSystem.Animation.overlayFade, value: isPresented.wrappedValue)
                }
            }
        )
    }
}

// MARK: - 便捷错误处理扩展

/// 便捷的错误处理 API
struct ErrorHandler {
    /// 快速处理相机错误
    static func camera(_ error: AppError) {
        AppErrorHandler.shared.handle(error)
    }

    /// 快速处理存储错误
    static func storage(_ error: AppError) {
        AppErrorHandler.shared.handle(error)
    }

    /// 快速处理网络错误
    static func network(_ error: AppError) {
        AppErrorHandler.shared.handle(error)
    }

    /// 快速处理处理错误
    static func processing(_ error: AppError) {
        AppErrorHandler.shared.handle(error)
    }

    /// 快速处理权限错误
    static func permission(_ error: AppError) {
        AppErrorHandler.shared.handle(error)
    }

    /// 静默记录错误
    static func log(_ error: AppError) {
        AppErrorHandler.shared.logSilently(error)
    }
}

#endif