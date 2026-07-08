//
//  LiveCaptureApp.swift
//  LiveCapture
//
//  应用程序入口文件 - 极简启动优化
//

import SwiftUI

#if os(iOS)

/// 全局预初始化相机管理器，在应用启动时后台准备
private let prewarmedCamera = CameraManager()

@main
struct LiveCaptureApp: App {
	@State private var showSplash = true
	@State private var splashOpacity: Double = 1.0

	var body: some Scene {
		WindowGroup {
			ZStack {
				// 主界面 - 始终在底层
				MinimalMainView()
					.zIndex(0)

				// 启动画面 - 淡出过渡
				if showSplash {
					splashScreenView
						.zIndex(10)
						.opacity(splashOpacity)
						.transition(.opacity)
				}
			}
			.onAppear {
				prewarmCamera()
				// 目标 < 0.8s 进入相机就绪状态
				DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
					withAnimation(.easeOut(duration: 0.3)) {
						splashOpacity = 0
					}
				}
				DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
					showSplash = false
				}
			}
		}
	}

	/// 启动画面 - 极简品牌标识
	private var splashScreenView: some View {
		ZStack {
			Color.black.ignoresSafeArea()

			VStack(spacing: 16) {
				Image(systemName: "viewfinder.circle.fill")
					.font(.system(size: 64, weight: .light))
					.foregroundColor(.white)
					.opacity(0.9)

				Text("LiveCapture")
					.font(.system(size: 28, weight: .semibold, design: .rounded))
					.foregroundColor(.white.opacity(0.8))
			}
		}
	}

	/// 预热相机 - 后台初始化 AVCaptureSession
	private func prewarmCamera() {
		DispatchQueue.global(qos: .userInitiated).async {
			prewarmedCamera.checkAndConfigure { result in
				switch result {
				case .success:
					break
				case .failure:
					break
				}
			}
		}
	}
}

#endif