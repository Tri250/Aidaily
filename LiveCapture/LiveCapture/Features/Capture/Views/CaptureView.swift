//
//  CaptureView.swift
//  LiveCapture
//
//  极简拍摄界面 - DOKA 风格
//

import SwiftUI
import AVFoundation

#if os(iOS)

struct CaptureView: View {
	@StateObject private var viewModel: CaptureViewModel
	@StateObject private var filterManager = FilterPresetManager()

	// UI 状态
	@State private var showControls = true
	@State private var showFilterStrip = false
	@State private var showModeSelector = false
	@State private var showZoomIndicator = false
	@State private var flashMode: TopControlBar.FlashMode = .auto
	@State private var selectedCaptureMode: ModeSelectorView.CaptureMode = .photo
	@State private var zoomLevelText: String = "1×"

	// 动画状态
	@State private var captureAnimationScale: CGFloat = 1.0
	@State private var captureFlashOpacity: Double = 0.0
	@State private var cameraFlipRotation: Double = 0.0
	@State private var pinchInitialFactor: CGFloat = 1.0
	@State private var pinchActive = false

	// 自动隐藏计时器
	@State private var autoHideWorkItem: DispatchWorkItem?

	@Environment(\.dismiss) private var dismiss

	init(detectionMode: DetectionMode = .fast, isAutoCaptureEnabled: Bool = true, captureDelay: Double = 1.0) {
		let vm = CaptureViewModel(detectionMode: detectionMode)
		vm.isAutoCaptureEnabled = isAutoCaptureEnabled
		vm.captureDelay = captureDelay
		_viewModel = StateObject(wrappedValue: vm)
	}

	var body: some View {
		GeometryReader { geo in
			let safeInsets = geo.safeAreaInsets

			ZStack {
				// 黑色背景
				Color.black
					.ignoresSafeArea()
					.zIndex(0)

				// 相机预览层
				cameraPreviewLayer(size: geo.size)
					.zIndex(0)

				// 拍照闪光效果
				if captureFlashOpacity > 0 {
					Color.white
						.opacity(captureFlashOpacity)
						.ignoresSafeArea()
						.zIndex(0.5)
						.allowsHitTesting(false)
				}

				// 手势层 - 覆盖全屏
				gestureLayer
					.zIndex(1)

				// UI 覆盖层
				VStack(spacing: 0) {
					// 顶部控制栏
					TopControlBar(
						onFlashToggle: { cycleFlashMode() },
						onTimerTap: { toggleTimer() },
						onAspectRatioTap: { toggleAspectRatio() },
						onSettingsTap: { presentSettings() },
						flashMode: flashMode,
						showControls: showControls
					)
					.padding(.top, safeInsets.top > 0 ? 0 : 8)

					Spacer()

					// 变焦指示器
					if showZoomIndicator {
						zoomIndicator
							.transition(.scale.combined(with: .opacity))
					}

					// 模式选择器
					if showModeSelector {
						ModeSelectorView(selectedMode: $selectedCaptureMode)
							.transition(.move(edge: .bottom).combined(with: .opacity))
					}

					// 滤镜条
					if showFilterStrip {
						FilterStripView(filterManager: filterManager) { preset in
							applyFilter(preset)
						}
						.transition(.move(edge: .bottom).combined(with: .opacity))
					}

					// 底部控制区
					bottomControlBar(bottomInset: max(safeInsets.bottom, 16))
						.padding(.bottom, safeInsets.bottom > 0 ? 0 : 16)
				}
				.zIndex(2)
				.opacity(showControls ? 1 : 0)
				.animation(DesignSystem.Animation.overlayFade, value: showControls)
			}
		}
		.ignoresSafeArea()
		.navigationBarBackButtonHidden(true)
		.preferredColorScheme(.dark)
		.onAppear {
			viewModel.onAppear()
			viewModel.onCaptureTriggered = {
				triggerCaptureAnimation()
			}
			resetAutoHideTimer()
		}
		.onDisappear {
			viewModel.onDisappear()
			autoHideWorkItem?.cancel()
		}
		.onChange(of: viewModel.zoomState.displayedFactor) { _, newFactor in
			updateZoomDisplay(newFactor)
		}
	}

	// MARK: - Camera Preview Layer

	private func cameraPreviewLayer(size: CGSize) -> some View {
		CameraPreviewSection(
			session: viewModel.session,
			compositionRect: viewModel.compositionRectInView,
			canvasSize: size,
			cropRectInView: viewModel.cropRectInView,
			boxCenterInView: viewModel.boxCenterInView,
			isAligned: viewModel.isAligned,
			distanceToCenter: viewModel.distanceToCenter,
			isFrontCamera: viewModel.isFrontCamera,
			onCompositionRectUpdate: { rect in
				viewModel.registerCompositionRect(rect)
			}
		)
		.frame(width: size.width, height: size.height)
		.scaleEffect(captureAnimationScale)
		.rotation3DEffect(
			.degrees(cameraFlipRotation),
			axis: (x: 0, y: 1, z: 0),
			perspective: 0.5
		)
		.animation(.spring(response: 0.3, dampingFraction: 0.6), value: captureAnimationScale)
		.animation(.spring(response: 0.5, dampingFraction: 0.75), value: cameraFlipRotation)
		.ignoresSafeArea()
	}

	// MARK: - Gesture Layer

	private var gestureLayer: some View {
		Color.clear
			.contentShape(Rectangle())
			.captureGestures(
				GestureCallbacks(
					onSwipeUp: {
						withAnimation(DesignSystem.Animation.filterStripReveal) {
							showFilterStrip = true
							showModeSelector = false
						}
						resetAutoHideTimer()
					},
					onSwipeDown: {
						withAnimation(DesignSystem.Animation.filterStripReveal) {
							showModeSelector = true
							showFilterStrip = false
						}
						resetAutoHideTimer()
					},
					onLongPress: { _ in
						// 锁定对焦/曝光
						HapticManager.shared.focusLock()
						// 显示对焦锁定指示
						showControls = true
						resetAutoHideTimer()
					},
					onDoubleTap: {
						triggerCameraFlipAnimation()
						viewModel.toggleCameraPosition()
						resetAutoHideTimer()
					},
					onPinch: { scale in
						if !pinchActive {
							pinchInitialFactor = viewModel.zoomState.currentFactor
							pinchActive = true
						}
						let target = clampedZoomFactor(for: pinchInitialFactor * scale)
						viewModel.updateZoomInteractively(to: target)
						showZoomIndicator = true
					},
					onPinchEnd: { scale in
						let target = clampedZoomFactor(for: pinchInitialFactor * scale)
						viewModel.finalizeZoomInteractively(at: target, smooth: true)
						pinchActive = false
						DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
							withAnimation(DesignSystem.Animation.overlayFade) {
								showZoomIndicator = false
							}
						}
					},
					onTap: { _ in
						// 单击显示/隐藏控件
						withAnimation(DesignSystem.Animation.overlayFade) {
							showControls.toggle()
						}
						if showControls {
							resetAutoHideTimer()
						}
					}
				)
			)
			.onTapGesture { location in
				// 单击时切换控件显示
				withAnimation(DesignSystem.Animation.overlayFade) {
					showControls.toggle()
					showFilterStrip = false
					showModeSelector = false
				}
				if showControls {
					resetAutoHideTimer()
				}
			}
	}

	// MARK: - Bottom Control Bar

	private func bottomControlBar(bottomInset: CGFloat) -> some View {
		HStack(alignment: .bottom) {
			// 相册缩略图
			albumThumbnailButton

			Spacer()

			// 快门按钮
			CaptureButton(
				action: {
					HapticManager.shared.capture()
					viewModel.capturePhoto()
				},
				onLongPressStart: {
					// 开始录像
				},
				onLongPressEnd: {
					// 停止录像
				}
			)

			Spacer()

			// 翻转摄像头按钮
			flipCameraButton
		}
		.padding(.horizontal, 24)
	}

	// MARK: - Album Thumbnail

	private var albumThumbnailButton: some View {
		Button {
			HapticManager.shared.light()
			viewModel.openSystemPhotoLibrary()
		} label: {
			RoundedRectangle(cornerRadius: 6)
				.fill(Color.white.opacity(0.15))
				.frame(width: 42, height: 42)
				.overlay(
					RoundedRectangle(cornerRadius: 6)
						.strokeBorder(Color.white.opacity(0.3), lineWidth: 1)
				)
				.overlay(
					Image(systemName: "photo.on.rectangle")
						.font(.system(size: 16, weight: .medium))
						.foregroundColor(.white.opacity(0.7))
				)
		}
	}

	// MARK: - Flip Camera Button

	private var flipCameraButton: some View {
		Button {
			HapticManager.shared.light()
			triggerCameraFlipAnimation()
			viewModel.toggleCameraPosition()
		} label: {
			Image(systemName: "arrow.triangle.2.circlepath.camera")
				.font(.system(size: 18, weight: .medium))
				.foregroundColor(DesignSystem.Colors.minimalLabel)
				.frame(width: 42, height: 42)
				.background(
					Circle()
						.fill(DesignSystem.Colors.minimalDarkOverlay)
				)
		}
	}

	// MARK: - Zoom Indicator

	private var zoomIndicator: some View {
		Text(zoomLevelText)
			.font(DesignSystem.Typography.minimalZoomIndicator)
			.foregroundColor(DesignSystem.Colors.minimalLabel)
			.padding(.horizontal, 12)
			.padding(.vertical, 6)
			.background(
				Capsule()
					.fill(DesignSystem.Colors.minimalDarkOverlay)
			)
			.padding(.bottom, 8)
	}

	// MARK: - Actions

	private func cycleFlashMode() {
		flashMode = flashMode.next
	}

	private func toggleTimer() {
		// 切换计时器
		HapticManager.shared.light()
		resetAutoHideTimer()
	}

	private func toggleAspectRatio() {
		// 切换画幅
		HapticManager.shared.light()
		resetAutoHideTimer()
	}

	private func presentSettings() {
		HapticManager.shared.light()
		dismiss()
		resetAutoHideTimer()
	}

	private func applyFilter(_ preset: LutFilterPreset) {
		// 应用滤镜预设
		resetAutoHideTimer()
	}

	// MARK: - Auto-Hide Timer

	private func resetAutoHideTimer() {
		autoHideWorkItem?.cancel()
		showControls = true

		let work = DispatchWorkItem {
			withAnimation(DesignSystem.Animation.overlayFade) {
				showControls = false
				showFilterStrip = false
				showModeSelector = false
			}
		}
		autoHideWorkItem = work
		DispatchQueue.main.asyncAfter(deadline: .now() + DesignSystem.Animation.autoHideDelay, execute: work)
	}

	// MARK: - Zoom Helpers

	private func updateZoomDisplay(_ factor: CGFloat) {
		let rounded = Int(round(factor * 10))
		if rounded % 10 == 0 {
			zoomLevelText = "\(Int(factor))×"
		} else {
			zoomLevelText = String(format: "%.1f×", factor)
		}
	}

	private func clampedZoomFactor(for factor: CGFloat) -> CGFloat {
		min(max(factor, viewModel.zoomRange.lowerBound), viewModel.zoomRange.upperBound)
	}

	// MARK: - Animations

	private func triggerCaptureAnimation() {
		withAnimation(.easeOut(duration: 0.1)) {
			captureFlashOpacity = 0.8
		}
		withAnimation(.easeIn(duration: 0.2).delay(0.1)) {
			captureFlashOpacity = 0.0
		}

		withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
			captureAnimationScale = 2.0
		}

		DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
			withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
				captureAnimationScale = 1.0
			}
		}
	}

	private func triggerCameraFlipAnimation() {
		withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
			cameraFlipRotation += 180
		}
	}
}

#endif