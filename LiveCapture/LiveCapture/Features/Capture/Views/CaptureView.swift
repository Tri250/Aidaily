//
//  CaptureView.swift
//  LiveCapture
//
//  极简拍摄界面 - 集成专业相机控制、构图引导、定时/连拍、手势提示、快速预览
//

import SwiftUI
import AVFoundation

#if os(iOS)

struct CaptureView: View {
	@StateObject private var viewModel: CaptureViewModel
	@StateObject private var filterManager = FilterPresetManager()
	@StateObject private var proManager = ProCameraManager.shared
	@StateObject private var enhancementManager = CameraEnhancementManager.shared
	@StateObject private var levelMonitor = LevelMonitor()

	// UI 状态
	@State private var showControls = true
	@State private var showFilterStrip = false
	@State private var showModeSelector = false
	@State private var showZoomIndicator = false
	@State private var isImmersiveMode = false
	@State private var flashMode: TopControlBar.FlashMode = .auto
	@State private var selectedCaptureMode: ModeSelectorView.CaptureMode = .photo
	@State private var zoomLevelText: String = "1×"

	// 构图引导
	@State private var compositionGuideType: CompositionGuideType = .grid
	@State private var showLevelIndicator = false
	@State private var showHistogram = false
	@State private var currentRoll: Double = 0

	// Pro 控制面板
	@State private var showProControls = false

	// 渐进式取景框：0=全部隐藏, 1=仅中心准星, 2=准星+追踪, 3=全部显示
	@State private var overlayProgression: Int = 3

	// 动画状态
	@State private var captureAnimationScale: CGFloat = 1.0
	@State private var captureFlashOpacity: Double = 0.0
	@State private var cameraFlipRotation: Double = 0.0
	@State private var pinchInitialFactor: CGFloat = 1.0
	@State private var pinchActive = false

	// 手势优先级标识
	@State private var isGestureActive = false

	// 自动隐藏计时器
	@State private var autoHideWorkItem: DispatchWorkItem?

	// 拍照后预览
	@State private var lastCapturedImage: UIImage?
	@State private var lastCapturedRecord: PhotoRecord?

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

				// 构图引导叠加层（九宫格/黄金螺线/水平仪等）
				CompositionGuideOverlay(
					guideType: compositionGuideType,
					compositionRect: geo.size.width > 0 ? CGRect(origin: .zero, size: geo.size) : .zero,
					isActive: compositionGuideType != .none && !isImmersiveMode
				)
				.zIndex(1.5)

				// 水平仪
				if showLevelIndicator && !isImmersiveMode {
					VStack {
						Spacer()
						LevelIndicator(roll: currentRoll, isActive: abs(currentRoll) < 0.05)
							.padding(.bottom, 16)
					}
					.zIndex(3)
				}

				// 直方图
				if showHistogram && !isImmersiveMode {
					VStack {
						HStack {
							HistogramView(data: proManager.histogramData, isActive: showHistogram)
								.frame(width: 160, height: 40)
								.padding(.leading, 12)
								.padding(.top, 12)
							Spacer()
						}
						Spacer()
					}
					.zIndex(3)
				}

				// 手势提示叠加层
				GestureHintOverlay()
					.zIndex(40)

				// 拍照后快速预览
				if let image = lastCapturedImage, let record = lastCapturedRecord {
					PostCaptureFlow(
						photoImage: image,
						record: record,
						onShare: { img in
							let activityVC = UIActivityViewController(activityItems: [img], applicationActivities: nil)
							if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
							   let rootVC = windowScene.windows.first?.rootViewController {
								rootVC.present(activityVC, animated: true)
							}
						},
						onEdit: { _ in },
						onDelete: {
							lastCapturedImage = nil
							lastCapturedRecord = nil
							ToastManager.shared.info("照片已移至最近删除")
						},
						onDismiss: {
							lastCapturedImage = nil
							lastCapturedRecord = nil
						}
					)
					.zIndex(50)
				}

				// 定时拍摄倒计时
				if enhancementManager.isTimerRunning {
					timerCountdownOverlay
						.zIndex(30)
				}

				// 连拍计数
				if enhancementManager.isBursting {
					burstCountOverlay
						.zIndex(30)
				}

				// 拍摄提示
				if !isImmersiveMode && showControls {
					VStack {
						Spacer()
						ShootingTipBanner(
							tip: "点击屏幕切换控件，双指下滑沉浸模式",
							icon: "hand.point.up.left"
						)
						.padding(.bottom, 60)
					}
					.zIndex(3)
				}

				// UI 覆盖层
				VStack(spacing: 0) {
					// 顶部控制栏 - 渐进式显示
					if !isImmersiveMode {
						topControlBar
							.padding(.top, safeInsets.top > 0 ? 0 : 8)
							.transition(.move(edge: .top).combined(with: .opacity))
					}

					Spacer()

					// 变焦指示器
					if showZoomIndicator && !isImmersiveMode {
						zoomIndicator
							.transition(.scale.combined(with: .opacity))
					}

					// 模式选择器
					if showModeSelector && !isImmersiveMode {
						ModeSelectorView(selectedMode: $selectedCaptureMode)
							.transition(.move(edge: .bottom).combined(with: .opacity))
					}

					// 滤镜条
					if showFilterStrip && !isImmersiveMode {
						FilterStripView(filterManager: filterManager) { preset in
							applyFilter(preset)
						}
						.transition(.move(edge: .bottom).combined(with: .opacity))
					}

					// Pro 专业控制面板
					if showProControls && !isImmersiveMode {
						ProCameraControlView(cameraManager: viewModel.camera)
							.transition(.move(edge: .bottom).combined(with: .opacity))
					}

					// 底部控制区
					if !isImmersiveMode {
						bottomControlBar(bottomInset: max(safeInsets.bottom, 16))
							.padding(.bottom, safeInsets.bottom > 0 ? 0 : 16)
					}
				}
				.zIndex(2)
				.opacity(showControls && !isImmersiveMode ? 1 : 0)
				.animation(DesignSystem.Animation.overlayFade, value: showControls)
				.animation(DesignSystem.Animation.smooth, value: isImmersiveMode)

				// 沉浸模式指示器
				if isImmersiveMode {
					VStack {
						HStack {
							Spacer()
							Text("沉浸模式")
								.font(DesignSystem.Typography.minimalModeLabel)
								.foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
								.padding(.horizontal, 12)
								.padding(.vertical, 6)
								.background(
									Capsule()
										.fill(DesignSystem.Colors.minimalDarkOverlay)
								)
								.padding(.trailing, 16)
								.padding(.top, 12)
						}
						Spacer()
					}
					.zIndex(2)
					.transition(.opacity)
				}
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
			levelMonitor.startMonitoring()
			enhancementManager.startBatteryMonitoring()
			enhancementManager.checkStorage()
			registerVolumeButtonShutter()
		}
		.onDisappear {
			viewModel.onDisappear()
			autoHideWorkItem?.cancel()
			levelMonitor.stopMonitoring()
			enhancementManager.unregisterVolumeButtonShutter()
		}
		.onChange(of: viewModel.zoomState.displayedFactor) { _, newFactor in
			updateZoomDisplay(newFactor)
		}
		.onChange(of: levelMonitor.rollAngle) { _, roll in
			currentRoll = Double(roll) * .pi / 180.0
		}
		.onChange(of: proManager.histogramData) { _, _ in
			// 直方图数据自动更新
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
			// 手势优先级：Pinch > LongPress > Swipe > DoubleTap > SingleTap
			.gesture(
				MagnificationGesture()
					.onChanged { scale in
						isGestureActive = true
						if !pinchActive {
							pinchInitialFactor = viewModel.zoomState.currentFactor
							pinchActive = true
						}
						let target = clampedZoomFactor(for: pinchInitialFactor * scale)
						viewModel.updateZoomInteractively(to: target)
						showZoomIndicator = true
					}
					.onEnded { scale in
						let target = clampedZoomFactor(for: pinchInitialFactor * scale)
						viewModel.finalizeZoomInteractively(at: target, smooth: true)
						pinchActive = false
						isGestureActive = false
						DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
							withAnimation(DesignSystem.Animation.overlayFade) {
								showZoomIndicator = false
							}
						}
					}
			)
			.simultaneousGesture(
				LongPressGesture(minimumDuration: 0.5)
					.sequenced(before: DragGesture(minimumDistance: 0))
					.onEnded { _ in
						HapticManager.shared.focusLock()
						showControls = true
						resetAutoHideTimer()
					}
			)
			.simultaneousGesture(
				// 双指下滑 → 沉浸模式
				DragGesture(minimumDistance: 40)
					.onEnded { value in
						guard !isGestureActive else { return }
						// 向下滑动进入沉浸模式
						if value.translation.height > 60 && abs(value.translation.width) < 60 {
							withAnimation(DesignSystem.Animation.smooth) {
								isImmersiveMode = true
								showControls = false
								showFilterStrip = false
								showModeSelector = false
								showProControls = false
							}
							HapticManager.shared.medium()
						}
					}
			)
			.onTapGesture(count: 2) { location in
				// 双击切换摄像头
				triggerCameraFlipAnimation()
				viewModel.toggleCameraPosition()
				resetAutoHideTimer()
			}
			.onTapGesture(count: 1) { location in
				// 单击：沉浸模式下退出，否则切换控件
				if isImmersiveMode {
					withAnimation(DesignSystem.Animation.smooth) {
						isImmersiveMode = false
						showControls = true
					}
					resetAutoHideTimer()
				} else {
					withAnimation(DesignSystem.Animation.overlayFade) {
						showControls.toggle()
						showFilterStrip = false
						showModeSelector = false
						showProControls = false
					}
					if showControls {
						resetAutoHideTimer()
					}
				}
			}
	}

	// MARK: - Top Control Bar

	private var topControlBar: some View {
		HStack(spacing: 20) {
			// 闪光灯
			controlButton(
				icon: flashMode.iconName,
				action: { cycleFlashMode() }
			)

			// 构图引导切换
			controlButton(
				icon: compositionGuideType.icon,
				action: { cycleCompositionGuide() }
			)

			Spacer()

			// 水平仪
			controlButton(
				icon: showLevelIndicator ? "level.fill" : "level",
				action: { toggleLevelIndicator() }
			)

			// 直方图
			controlButton(
				icon: showHistogram ? "waveform.path.ecg" : "waveform.path",
				action: { toggleHistogram() }
			)

			// 计时器
			controlButton(
				icon: enhancementManager.timerEnabled ? "timer.circle.fill" : "timer",
				action: { toggleTimer() }
			)

			// 设置
			controlButton(
				icon: "gearshape",
				action: { presentSettings() }
			)
		}
		.padding(.horizontal, 20)
		.padding(.top, 12)
	}

	@ViewBuilder
	private func controlButton(icon: String, action: @escaping () -> Void) -> some View {
		Button {
			HapticManager.shared.light()
			action()
		} label: {
			Image(systemName: icon)
				.font(.system(size: 16, weight: .medium))
				.foregroundColor(DesignSystem.Colors.minimalLabel)
				.frame(width: 36, height: 36)
				.background(
					Circle()
						.fill(DesignSystem.Colors.minimalDarkOverlay)
				)
		}
	}

	// MARK: - Bottom Control Bar

	private func bottomControlBar(bottomInset: CGFloat) -> some View {
		VStack(spacing: 8) {
			HStack(alignment: .bottom) {
				// 相册缩略图
				albumThumbnailButton

				Spacer()

				// 快门按钮
				CaptureButton(
					action: {
						HapticManager.shared.capture()
						performCapture()
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

			// Pro 模式切换按钮
			Button {
				HapticManager.shared.light()
				withAnimation(DesignSystem.Animation.bouncy) {
					showProControls.toggle()
					showFilterStrip = false
					showModeSelector = false
				}
				resetAutoHideTimer()
			} label: {
				HStack(spacing: 4) {
					Image(systemName: "slider.horizontal.3")
						.font(.system(size: 10, weight: .medium))
					Text(showProControls ? "收起专业模式" : "专业模式")
						.font(.system(size: 10, weight: .medium))
				}
				.foregroundColor(showProControls ? .white : DesignSystem.Colors.minimalSecondaryLabel)
				.padding(.horizontal, 10)
				.padding(.vertical, 4)
				.background(
					Capsule()
						.fill(showProControls ? Color.white.opacity(0.15) : Color.white.opacity(0.08))
				)
			}
		}
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

	// MARK: - Timer Countdown Overlay

	private var timerCountdownOverlay: some View {
		ZStack {
			Color.black.opacity(0.3).ignoresSafeArea()

			VStack(spacing: 16) {
				Text("\(enhancementManager.timerCountdown)")
					.font(.system(size: 96, weight: .thin, design: .rounded))
					.foregroundColor(.white)
					.scaleEffect(enhancementManager.timerCountdown <= 3 ? 1.2 : 1.0)
					.animation(.easeInOut(duration: 0.3), value: enhancementManager.timerCountdown)

				if enhancementManager.timerCountdown <= 3 {
					Text("保持稳定...")
						.font(DesignSystem.Typography.title3)
						.foregroundColor(.white.opacity(0.7))
				}
			}
		}
		.allowsHitTesting(false)
	}

	// MARK: - Burst Count Overlay

	private var burstCountOverlay: some View {
		VStack {
			Spacer()
			HStack {
				Spacer()
				VStack(spacing: 4) {
					Text("\(enhancementManager.burstCount)")
						.font(.system(size: 36, weight: .bold, design: .monospaced))
						.foregroundColor(.white)
					Text("连拍")
						.font(.system(size: 12, weight: .medium))
						.foregroundColor(.white.opacity(0.7))
				}
				.padding(12)
				.background(
					RoundedRectangle(cornerRadius: 12)
						.fill(Color.black.opacity(0.5))
				)
				.padding(.trailing, 20)
				.padding(.bottom, 120)
			}
		}
		.allowsHitTesting(false)
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

	private func cycleCompositionGuide() {
		let allTypes = CompositionGuideType.allCases
		if let currentIndex = allTypes.firstIndex(of: compositionGuideType) {
			let nextIndex = (currentIndex + 1) % allTypes.count
			compositionGuideType = allTypes[nextIndex]
		}
		resetAutoHideTimer()
	}

	private func toggleLevelIndicator() {
		withAnimation(DesignSystem.Animation.overlayFade) {
			showLevelIndicator.toggle()
		}
		resetAutoHideTimer()
	}

	private func toggleHistogram() {
		withAnimation(DesignSystem.Animation.overlayFade) {
			showHistogram.toggle()
		}
		resetAutoHideTimer()
	}

	private func toggleTimer() {
		HapticManager.shared.light()
		enhancementManager.timerEnabled.toggle()
		if enhancementManager.timerEnabled {
			ToastManager.shared.info("定时拍摄: \(enhancementManager.timerDuration.label)")
		} else {
			ToastManager.shared.info("定时拍摄已关闭")
		}
		resetAutoHideTimer()
	}

	private func toggleAspectRatio() {
		HapticManager.shared.light()
		resetAutoHideTimer()
	}

	private func presentSettings() {
		HapticManager.shared.light()
		dismiss()
		resetAutoHideTimer()
	}

	private func applyFilter(_ preset: LutFilterPreset) {
		resetAutoHideTimer()
	}

	private func performCapture() {
		// 定时拍摄
		enhancementManager.startTimer {
			// 连拍
			enhancementManager.startBurst {
				viewModel.capturePhoto()
			}
		}
	}

	// MARK: - Volume Button Shutter

	private func registerVolumeButtonShutter() {
		enhancementManager.registerVolumeButtonShutter { [weak enhancementManager] in
			guard let manager = enhancementManager, !manager.isTimerRunning else { return }
			manager.startTimer {
				manager.startBurst {
					DispatchQueue.main.async {
						self.viewModel.capturePhoto()
					}
				}
			}
		}
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
				showProControls = false
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