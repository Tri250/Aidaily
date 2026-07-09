//
//  MainView.swift
//  LiveCapture
//
//  极简主视图 - 手势导航替代 TabBar
//

import SwiftUI

#if os(iOS)

/// 拍摄模式
enum CaptureMode: String, CaseIterable, Identifiable {
    case photo = "照片"
    case video = "视频"
    case slowMotion = "慢动作"
    case timelapse = "延时摄影"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .photo: return "camera.fill"
        case .video: return "video.fill"
        case .slowMotion: return "slowmo"
        case .timelapse: return "timelapse"
        }
    }
}

/// 极简主视图 - 相机始终为主界面，手势导航到其他页面
struct MinimalMainView: View {
	@AppStorage("detectionMode") private var detectionMode: DetectionMode = .fast
	@AppStorage("autoCaptureEnabled") private var autoCaptureEnabled = true
	@AppStorage("captureDelay") private var captureDelay: Double = 1.0
	@AppStorage("colorScheme") private var colorScheme: String = "system"

	@State private var showAlbum = false
	@State private var showSettings = false
	@State private var showCommunity = false
	@State private var albumOffset: CGFloat = 0
	@State private var settingsOffset: CGFloat = 0

    /// 当前拍摄模式
    @State private var captureMode: CaptureMode = .photo

	private var resolvedScheme: ColorScheme? {
		switch colorScheme {
		case "light": return .light
		case "dark": return .dark
		default: return nil
		}
	}

	var body: some View {
		GeometryReader { geo in
			ZStack {
				// 相机始终作为背景主视图
				CaptureView(
					detectionMode: detectionMode,
					isAutoCaptureEnabled: autoCaptureEnabled,
					captureDelay: captureDelay
				)
				.preferredColorScheme(.dark)
				.zIndex(0)

				// 拍摄模式选择器
				VStack {
					Spacer()
					captureModeSelector
						.padding(.bottom, 40)
				}
				.zIndex(1)

				// 相册面板 - 从左滑入
				if showAlbum {
					albumPanelView(size: geo.size)
						.zIndex(5)
						.transition(.move(edge: .leading))
				}

				// 设置面板 - 从右滑入
				if showSettings {
					settingsPanelView(size: geo.size)
						.zIndex(5)
						.transition(.move(edge: .trailing))
				}

				// 社区面板 - 从底部滑入
				if showCommunity {
					communityPanelView(size: geo.size)
						.zIndex(6)
						.transition(.move(edge: .bottom))
				}
			}
		}
		.gesture(
			DragGesture(minimumDistance: 40)
				.onEnded { value in
					let horizontal = value.translation.width
					let vertical = value.translation.height

					// 垂直滑动优先（上下滑动范围更大时）
					if abs(vertical) > abs(horizontal) {
						if vertical < 0 {
							// 上滑 → 打开社区
							HapticManager.shared.light()
							withAnimation(DesignSystem.Animation.modeSlide) {
								showCommunity = true
								showAlbum = false
								showSettings = false
							}
						} else {
							// 下滑 → 关闭社区
							if showCommunity {
								HapticManager.shared.light()
								withAnimation(DesignSystem.Animation.modeSlide) {
									showCommunity = false
								}
							}
						}
						return
					}

					if horizontal > 0 {
						// 右滑 → 打开相册
						HapticManager.shared.light()
						withAnimation(DesignSystem.Animation.modeSlide) {
							showAlbum = true
							showSettings = false
							showCommunity = false
						}
					} else {
						// 左滑 → 打开设置
						HapticManager.shared.light()
						withAnimation(DesignSystem.Animation.modeSlide) {
							showSettings = true
							showAlbum = false
							showCommunity = false
						}
					}
				}
		)
		.onAppear {
			_ = PhotoStorageService.shared.loadRecords()
		}
		.onReceive(NotificationCenter.default.publisher(for: .navigateToCamera)) { _ in
			withAnimation(DesignSystem.Animation.modeSlide) {
				showAlbum = false
				showSettings = false
				showCommunity = false
			}
		}
		.onReceive(NotificationCenter.default.publisher(for: .navigateToSettings)) { _ in
			withAnimation(DesignSystem.Animation.modeSlide) {
				showSettings = true
				showAlbum = false
				showCommunity = false
			}
		}
	}

	// MARK: - Capture Mode Selector

    private var captureModeSelector: some View {
        HStack(spacing: 8) {
            ForEach(CaptureMode.allCases) { mode in
                Button {
                    HapticManager.shared.light()
                    withAnimation(.spring(response: 0.3)) {
                        captureMode = mode
                    }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: mode.icon)
                            .font(.system(size: 18, weight: .medium))
                        Text(mode.rawValue)
                            .font(.system(size: 10, weight: .medium))
                    }
                    .frame(width: 56, height: 56)
                    .background(
                        RoundedRectangle(cornerRadius: 14)
                            .fill(captureMode == mode
                                  ? Color.white.opacity(0.2)
                                  : Color.black.opacity(0.4))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(captureMode == mode
                                    ? Color.white.opacity(0.6)
                                    : Color.white.opacity(0.15),
                                    lineWidth: 1)
                    )
                    .foregroundColor(captureMode == mode ? .white : .white.opacity(0.6))
                }
                .accessibilityLabel("\(mode.rawValue)模式")
                .accessibilityHint(captureMode == mode ? "当前已选中" : "点击切换到\(mode.rawValue)")
                .accessibilityAddTraits(captureMode == mode ? .isSelected : [])
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(
            Capsule()
                .fill(Color.black.opacity(0.5))
        )
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.1), lineWidth: 0.5)
        )
    }

	// MARK: - Album Panel

	private func albumPanelView(size: CGSize) -> some View {
		ZStack(alignment: .leading) {
			// 半透明背景，点击关闭
			Color.black.opacity(0.5)
				.ignoresSafeArea()
				.onTapGesture {
					withAnimation(DesignSystem.Animation.modeSlide) {
						showAlbum = false
					}
				}

			// 相册内容
			HStack(spacing: 0) {
				LiveComposeView()
					.frame(width: size.width * 0.85)
					.background(Color(uiColor: .systemBackground))

				Spacer()
			}
			.ignoresSafeArea()
		}
	}

	// MARK: - Settings Panel

	private func settingsPanelView(size: CGSize) -> some View {
		ZStack(alignment: .trailing) {
			// 半透明背景，点击关闭
			Color.black.opacity(0.5)
				.ignoresSafeArea()
				.onTapGesture {
					withAnimation(DesignSystem.Animation.modeSlide) {
						showSettings = false
					}
				}

			// 设置内容
			HStack(spacing: 0) {
				Spacer()

				SettingsView()
					.frame(width: size.width * 0.85)
					.background(Color(uiColor: .systemBackground))
			}
			.ignoresSafeArea()
		}
	}

	// MARK: - Community Panel

	private func communityPanelView(size: CGSize) -> some View {
		ZStack(alignment: .bottom) {
			// 半透明背景，点击关闭
			Color.black.opacity(0.5)
				.ignoresSafeArea()
				.onTapGesture {
					withAnimation(DesignSystem.Animation.modeSlide) {
						showCommunity = false
					}
				}

			// 社区内容
			VStack(spacing: 0) {
				Spacer()

				CommunityView()
					.frame(height: size.height * 0.85)
					.background(Color(uiColor: .systemBackground))
					.clipShape(
						RoundedCorner(radius: DesignSystem.CornerRadius.xLarge, corners: [.topLeft, .topRight])
					)
			}
			.ignoresSafeArea()
		}
	}
}

// MARK: - 自定义圆角形状

struct RoundedCorner: Shape {
	var radius: CGFloat = .infinity
	var corners: UIRectCorner = .allCorners

	func path(in rect: CGRect) -> Path {
		let path = UIBezierPath(
			roundedRect: rect,
			byRoundingCorners: corners,
			cornerRadii: CGSize(width: radius, height: radius)
		)
		return Path(path.cgPath)
	}
}

#endif

// MARK: - 导航通知

extension Notification.Name {
    static let navigateToCamera = Notification.Name("com.livecapture.navigateToCamera")
    static let navigateToSettings = Notification.Name("com.livecapture.navigateToSettings")
}