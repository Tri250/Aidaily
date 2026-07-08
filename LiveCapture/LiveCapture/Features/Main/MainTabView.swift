//
//  MainTabView.swift
//  LiveCapture
//
//  已弃用 TabBar - 由 MinimalMainView 手势导航替代
//  保留此文件以兼容现有引用
//

import SwiftUI

struct MainTabView: View {
	@AppStorage("detectionMode") private var detectionMode: DetectionMode = .fast
	@AppStorage("autoCaptureEnabled") private var autoCaptureEnabled = true
	@AppStorage("captureDelay") private var captureDelay: Double = 1.0
	@AppStorage("colorScheme") private var colorScheme: String = "system"

	private var resolvedScheme: ColorScheme? {
		switch colorScheme {
		case "light": return .light
		case "dark": return .dark
		default: return nil
		}
	}

	var body: some View {
		ZStack {
			CaptureView(
				detectionMode: detectionMode,
				isAutoCaptureEnabled: autoCaptureEnabled,
				captureDelay: captureDelay
			)
			.preferredColorScheme(.dark)
		}
		.preferredColorScheme(resolvedScheme)
		.onAppear {
			_ = PhotoStorageService.shared.loadRecords()
		}
	}
}