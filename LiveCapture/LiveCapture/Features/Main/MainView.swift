//
//  MainView.swift
//  LiveCapture
//
//  极简主视图 - 手势导航替代 TabBar
//

import SwiftUI

#if os(iOS)

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

	private var resolvedScheme: ColorScheme? {
		switch colorScheme {
		case "light": return .light
		case "dark": return .dark
		default: return nil
		}
	}

	var body: some View {
		ZStack {
			// 相机始终作为背景主视图
			CaptureView(
				detectionMode: detectionMode,
				isAutoCaptureEnabled: autoCaptureEnabled,
				captureDelay: captureDelay
			)
			.preferredColorScheme(.dark)
			.zIndex(0)

			// 相册面板 - 从左滑入
			if showAlbum {
				albumPanelView
					.zIndex(5)
					.transition(.move(edge: .leading))
			}

			// 设置面板 - 从右滑入
			if showSettings {
				settingsPanelView
					.zIndex(5)
					.transition(.move(edge: .trailing))
			}

			// 社区面板 - 从底部滑入
			if showCommunity {
				communityPanelView
					.zIndex(6)
					.transition(.move(edge: .bottom))
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
	}

	// MARK: - Album Panel

	private var albumPanelView: some View {
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
					.frame(width: UIScreen.main.bounds.width * 0.85)
					.background(Color(uiColor: .systemBackground))

				Spacer()
			}
			.ignoresSafeArea()
		}
	}

	// MARK: - Settings Panel

	private var settingsPanelView: some View {
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
					.frame(width: UIScreen.main.bounds.width * 0.85)
					.background(Color(uiColor: .systemBackground))
			}
			.ignoresSafeArea()
		}
	}

	// MARK: - Community Panel

	private var communityPanelView: some View {
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
					.frame(height: UIScreen.main.bounds.height * 0.85)
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