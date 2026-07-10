//
//  iPadAdaptation.swift
//  LiveCapture
//
//  iPad 适配：分屏支持、横竖屏、多窗口
//

import SwiftUI

#if os(iOS)

// MARK: - iPad 适配配置

/// iPad 适配管理器
final class iPadAdaptation: ObservableObject {
    static let shared = iPadAdaptation()

    @Published var isPad: Bool
    @Published var isSplitView: Bool
    @Published var isSidebarVisible: Bool = true

    /// 当前水平尺寸类别
    @Published var horizontalSizeClass: UserInterfaceSizeClass?

    private init() {
        self.isPad = UIDevice.current.userInterfaceIdiom == .pad
        self.isSplitView = false
    }

    /// 更新尺寸类别
    func updateSizeClass(_ sizeClass: UserInterfaceSizeClass?) {
        horizontalSizeClass = sizeClass
        isSplitView = sizeClass == .compact && isPad
    }

    /// 侧边栏宽度
    var sidebarWidth: CGFloat {
        if isPad {
            return isSplitView ? 280 : 320
        }
        return 0
    }

    /// 内容区域最大宽度
    var contentMaxWidth: CGFloat {
        if isPad {
            return 640
        }
        return .infinity
    }

    /// 网格列数
    var gridColumns: Int {
        if isPad {
            return isSplitView ? 2 : 3
        }
        return 1
    }

    /// 底部栏高度
    var bottomBarHeight: CGFloat {
        isPad ? 72 : 56
    }
}

// MARK: - iPad 适配 ViewModifier

/// iPad 居中内容限制
struct iPaddedContent: ViewModifier {
    @ObservedObject private var adapter = iPadAdaptation.shared

    func body(content: Content) -> some View {
        if adapter.isPad {
            content
                .frame(maxWidth: adapter.contentMaxWidth)
                .frame(maxWidth: .infinity)
        } else {
            content
        }
    }
}

/// iPad 侧边栏添加
struct iPadSidebarAdaptable: ViewModifier {
    @ObservedObject private var adapter = iPadAdaptation.shared
    @Binding var isSidebarVisible: Bool

    func body(content: Content) -> some View {
        if adapter.isPad && !adapter.isSplitView {
            HStack(spacing: 0) {
                if isSidebarVisible {
                    // 侧边栏
                    sidebarView
                        .frame(width: adapter.sidebarWidth)
                        .transition(.move(edge: .leading))
                }

                // 主内容
                content
                    .frame(maxWidth: .infinity)
            }
            .animation(.easeInOut(duration: 0.25), value: isSidebarVisible)
        } else {
            content
        }
    }

    private var sidebarView: some View {
        VStack(alignment: .leading, spacing: 0) {
            // 侧边栏内容
            List {
                NavigationLink {
                    // 相机
                } label: {
                    Label("相机", systemImage: "camera.fill")
                }
                NavigationLink {
                    // 相册
                } label: {
                    Label("相册", systemImage: "photo.on.rectangle")
                }
                NavigationLink {
                    // 视频
                } label: {
                    Label("视频", systemImage: "video.fill")
                }
                NavigationLink {
                    // 编辑
                } label: {
                    Label("编辑", systemImage: "paintpalette")
                }
                NavigationLink {
                    // 设置
                } label: {
                    Label("设置", systemImage: "gearshape")
                }
            }
            .listStyle(.sidebar)
        }
        .background(DesignSystem.Colors.backgroundPrimary)
    }
}

// MARK: - View 扩展

extension View {
    /// 应用 iPad 居中内容限制
    func iPadContentAdaptive() -> some View {
        modifier(iPaddedContent())
    }

    /// 添加 iPad 侧边栏
    func iPadSidebarAdaptable(isVisible: Binding<Bool>) -> some View {
        modifier(iPadSidebarAdaptable(isSidebarVisible: isVisible))
    }

    /// 自适应列数
    func adaptiveGridColumns(minWidth: CGFloat = 160) -> some View {
        let columns = iPadAdaptation.shared.gridColumns
        let gridItems = Array(repeating: GridItem(.flexible(minimum: minWidth)), count: columns)
        return self
    }
}

// MARK: - 横竖屏适配

/// 监听设备方向变化
final class OrientationManager: ObservableObject {
    static let shared = OrientationManager()

    @Published var currentOrientation: UIDeviceOrientation = .portrait
    @Published var isLandscape: Bool = false

    private init() {
        currentOrientation = UIDevice.current.orientation
        isLandscape = currentOrientation.isLandscape

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(orientationChanged),
            name: UIDevice.orientationDidChangeNotification,
            object: nil
        )
    }

    @objc private func orientationChanged() {
        currentOrientation = UIDevice.current.orientation
        isLandscape = currentOrientation.isLandscape
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

// MARK: - 横竖屏 ViewModifier

struct OrientationAdaptive: ViewModifier {
    @ObservedObject private var orientation = OrientationManager.shared

    func body(content: Content) -> some View {
        if orientation.isLandscape && iPadAdaptation.shared.isPad {
            content
                .environment(\.horizontalSizeClass, .regular)
        } else {
            content
        }
    }
}

extension View {
    func orientationAdaptive() -> some View {
        modifier(OrientationAdaptive())
    }
}

#endif