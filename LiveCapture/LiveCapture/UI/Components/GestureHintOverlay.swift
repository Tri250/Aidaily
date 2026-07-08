//
//  GestureHintOverlay.swift
//  LiveCapture
//
//  手势提示叠加层：首次使用时显示手势引导
//  Tab 指示器：显示面板滑动方向
//

import SwiftUI

#if os(iOS)

// MARK: - 手势提示叠加层

struct GestureHintOverlay: View {
    @AppStorage("hasSeenGestureHints") private var hasSeenHints = false
    @State private var currentHint: GestureHint = .swipeUp
    @State private var showHints = false

    enum GestureHint: CaseIterable {
        case swipeUp
        case swipeDown
        case immersiveMode
        case doubleTap
        case longPress

        var icon: String {
            switch self {
            case .swipeUp: return "chevron.up"
            case .swipeDown: return "chevron.down"
            case .immersiveMode: return "arrow.down.forward.and.arrow.up.backward"
            case .doubleTap: return "hand.tap"
            case .longPress: return "hand.point.up.left"
            }
        }

        var title: String {
            switch self {
            case .swipeUp: return "上滑打开滤镜"
            case .swipeDown: return "下滑切换模式"
            case .immersiveMode: return "双指下滑沉浸模式"
            case .doubleTap: return "双击切换镜头"
            case .longPress: return "长按锁定对焦"
            }
        }

        var animation: (offset: CGSize, opacity: Double) {
            switch self {
            case .swipeUp: return (CGSize(width: 0, height: -30), 1.0)
            case .swipeDown: return (CGSize(width: 0, height: 30), 1.0)
            case .immersiveMode: return (CGSize(width: 0, height: 40), 1.0)
            case .doubleTap: return (.zero, 1.0)
            case .longPress: return (.zero, 1.0)
            }
        }
    }

    var body: some View {
        Group {
            if showHints {
                ZStack {
                    Color.black.opacity(0.6).ignoresSafeArea()

                    VStack(spacing: 24) {
                        Spacer()

                        // 手势动画
                        Image(systemName: currentHint.icon)
                            .font(.system(size: 48, weight: .light))
                            .foregroundColor(.white)
                            .offset(currentHint.animation.offset)
                            .animation(
                                .easeInOut(duration: 0.8).repeatForever(autoreverses: true),
                                value: currentHint
                            )

                        Text(currentHint.title)
                            .font(DesignSystem.Typography.title2)
                            .foregroundColor(.white)

                        Text("\(GestureHint.allCases.firstIndex(of: currentHint)! + 1) / \(GestureHint.allCases.count)")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(.white.opacity(0.5))

                        Spacer()

                        HStack(spacing: 40) {
                            Button("跳过") {
                                withAnimation(DesignSystem.Animation.easeOut) {
                                    showHints = false
                                    hasSeenHints = true
                                }
                            }
                            .foregroundColor(.white.opacity(0.6))

                            Button {
                                HapticManager.shared.light()
                                let allHints = GestureHint.allCases
                                if let currentIndex = allHints.firstIndex(of: currentHint),
                                   currentIndex < allHints.count - 1 {
                                    withAnimation(DesignSystem.Animation.smooth) {
                                        currentHint = allHints[currentIndex + 1]
                                    }
                                } else {
                                    withAnimation(DesignSystem.Animation.easeOut) {
                                        showHints = false
                                        hasSeenHints = true
                                    }
                                }
                            } label: {
                                Text("继续")
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 32)
                                    .padding(.vertical, 12)
                                    .background(
                                        Capsule()
                                            .fill(DesignSystem.Colors.primary)
                                    )
                            }
                        }
                        .padding(.bottom, 60)
                    }
                }
                .transition(.opacity)
                .zIndex(50)
            }
        }
        .onAppear {
            if !hasSeenHints {
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    withAnimation(DesignSystem.Animation.easeInOut) {
                        showHints = true
                    }
                }
            }
        }
    }
}

// MARK: - Tab 导航指示器

struct NavigationIndicator: View {
    let panelCount: Int
    let currentPanel: Int
    let direction: Direction

    enum Direction {
        case horizontal
        case vertical
    }

    var body: some View {
        Group {
            if direction == .vertical {
                VStack(spacing: 6) {
                    ForEach(0..<panelCount, id: \.self) { index in
                        Circle()
                            .fill(index == currentPanel ? Color.white : Color.white.opacity(0.3))
                            .frame(width: index == currentPanel ? 6 : 4, height: index == currentPanel ? 6 : 4)
                            .animation(DesignSystem.Animation.smooth, value: currentPanel)
                    }
                }
                .padding(6)
                .background(
                    Capsule()
                        .fill(Color.white.opacity(0.1))
                )
            } else {
                HStack(spacing: 6) {
                    ForEach(0..<panelCount, id: \.self) { index in
                        Capsule()
                            .fill(index == currentPanel ? Color.white : Color.white.opacity(0.3))
                            .frame(width: index == currentPanel ? 16 : 6, height: 6)
                            .animation(DesignSystem.Animation.smooth, value: currentPanel)
                    }
                }
                .padding(6)
                .background(
                    Capsule()
                        .fill(Color.white.opacity(0.1))
                )
            }
        }
    }
}

// MARK: - 拍摄前提示

struct ShootingTipBanner: View {
    let tip: String
    let icon: String
    @State private var isVisible = true

    var body: some View {
        if isVisible {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(DesignSystem.Colors.warning)

                Text(tip)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(.white)

                Spacer()

                Button {
                    withAnimation(DesignSystem.Animation.easeOut) {
                        isVisible = false
                    }
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white.opacity(0.5))
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.black.opacity(0.6))
            )
            .padding(.horizontal, 16)
            .transition(.move(edge: .top).combined(with: .opacity))
        }
    }
}

// MARK: - 照片缓存管理器

final class PhotoCacheManager {
    static let shared = PhotoCacheManager()

    private let memoryCache = NSCache<NSString, UIImage>()
    private let diskCacheQueue = DispatchQueue(label: "com.livecapture.photocache", qos: .utility)

    private init() {
        memoryCache.countLimit = 50
        memoryCache.totalCostLimit = 50 * 1024 * 1024  // 50MB
    }

    func cachedImage(for id: UUID) -> UIImage? {
        let key = id.uuidString as NSString
        if let cached = memoryCache.object(forKey: key) {
            return cached
        }
        return diskCache(for: key as String)
    }

    func cacheImage(_ image: UIImage, for id: UUID) {
        let key = id.uuidString as NSString
        let cost = Int(image.size.width * image.size.height * 4)
        memoryCache.setObject(image, forKey: key, cost: cost)
        diskCacheQueue.async {
            self.saveDiskCache(image, for: key as String)
        }
    }

    func clearCache() {
        memoryCache.removeAllObjects()
        diskCacheQueue.async {
            let cacheDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
            let photoCacheDir = cacheDir.appendingPathComponent("PhotoCache")
            try? FileManager.default.removeItem(at: photoCacheDir)
        }
    }

    private func diskCachePath(for key: String) -> URL {
        let cacheDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let photoCacheDir = cacheDir.appendingPathComponent("PhotoCache")
        try? FileManager.default.createDirectory(at: photoCacheDir, withIntermediateDirectories: true)
        return photoCacheDir.appendingPathComponent("\(key).jpg")
    }

    private func diskCache(for key: String) -> UIImage? {
        let path = diskCachePath(for: key)
        guard let data = try? Data(contentsOf: path) else { return nil }
        return UIImage(data: data)
    }

    private func saveDiskCache(_ image: UIImage, for key: String) {
        let path = diskCachePath(for: key)
        if let data = image.jpegData(compressionQuality: 0.8) {
            try? data.write(to: path)
        }
    }
}

#endif