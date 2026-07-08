//
//  CaptureView.swift
//  LiveCapture
//
//  专业拍摄界面 - DOKA 级别全功能
//

import SwiftUI
import AVFoundation
import CoreImage
import CoreMotion

#if os(iOS)

// MARK: - CaptureView

struct CaptureView: View {
	@StateObject private var viewModel: CaptureViewModel
	@StateObject private var filterManager = FilterPresetManager()
	@StateObject private var portraitViewModel = PortraitViewModel()

	// UI 状态
	@State private var showControls = true
	@State private var showFilterStrip = false
	@State private var showModeSelector = false
	@State private var showZoomIndicator = false
	@State private var isImmersiveMode = false
	@State private var selectedCaptureMode: ModeSelectorView.CaptureMode = .photo
	@State private var zoomLevelText: String = "1×"

	// 美颜与人物模式面板
	@State private var showBeautyPanel = false
	@State private var showPortraitModePanel = false

	// 渐进式取景框
	@State private var overlayProgression: Int = 3

	// 动画状态
	@State private var captureAnimationScale: CGFloat = 1.0
	@State private var captureFlashOpacity: Double = 0.0
	@State private var cameraFlipRotation: Double = 0.0
	@State private var pinchInitialFactor: CGFloat = 1.0
	@State private var pinchActive = false
	@State private var isGestureActive = false
	@State private var autoHideWorkItem: DispatchWorkItem?

	// MARK: - 新功能状态

	// 闪光灯（通过 CameraManager 控制）
	@State private var flashMode: AVCaptureDevice.FlashMode = .auto

	// 计时器
	enum TimerMode: String, CaseIterable { case off, three, five, ten }
	@State private var timerMode: TimerMode = .off
	@State private var timerCountdown: Int = 0
	@State private var showTimerSelection = false
	@State private var timerWorkItem: DispatchWorkItem?

	// 画幅比例
	enum AspectRatio: CaseIterable {
		case full, oneOne, threeFour, nineSixteen
		var ratio: CGFloat? {
			switch self {
			case .full: return nil
			case .oneOne: return 1.0
			case .threeFour: return 3.0 / 4.0
			case .nineSixteen: return 9.0 / 16.0
			}
		}
		var displayName: String {
			switch self {
			case .full: return "全屏"
			case .oneOne: return "1:1"
			case .threeFour: return "3:4"
			case .nineSixteen: return "9:16"
			}
		}
	}
	@State private var aspectRatio: AspectRatio = .full

	// 网格
	enum GridType: String, CaseIterable { case off, thirds, golden, nineGrid }
	@State private var gridType: GridType = .off
	@State private var showGrid = false

	// 水平仪
	@State private var showLevel = false
	@State private var levelAngle: CGFloat = 0
	private let motionManager = CMMotionManager()

	// 曝光补偿
	@State private var exposureBias: Float = 0
	@State private var showExposureSlider = false
	@State private var focusPoint: CGPoint = .zero
	@State private var showFocusIndicator = false
	@State private var focusIndicatorScale: CGFloat = 1.0

	// 对焦锁定
	@State private var isFocusLocked = false
	@State private var focusLockPoint: CGPoint = .zero
	@State private var showFocusLock = false

	// 滤镜实时预览
	@State private var selectedFilter: LutFilterPreset? = nil
	@State private var filterIntensity: Float = 1.0
	@State private var filteredPreviewImage: UIImage? = nil
	private let filterPreviewContext = CIContext(options: [.workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!])

	// 视频录制
	@State private var isRecording = false
	@State private var recordingStartTime: Date?
	@State private var recordingDuration: String = "00:00"
	@State private var recordingTimer: Timer?
	private var videoRecorder: VideoRecorder?

	// 照片回看
	@State private var showPhotoReview = false
	@State private var lastCapturedImage: UIImage? = nil
	@State private var lastCapturedData: Data? = nil

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
			let size = geo.size

			ZStack {
				Color.black
					.ignoresSafeArea()
					.zIndex(0)

				// 相机预览层
				cameraPreviewLayer(size: size)
					.zIndex(0)

				// 滤镜实时预览覆盖层
				if selectedFilter != nil, let filtered = filteredPreviewImage {
					Image(uiImage: filtered)
						.resizable()
						.aspectRatio(contentMode: .fill)
						.frame(width: size.width, height: size.height)
						.clipped()
						.allowsHitTesting(false)
						.zIndex(0.1)
				}

				// 画幅比例遮罩
				if aspectRatio != .full {
					aspectRatioMask(size: size)
						.zIndex(0.2)
						.allowsHitTesting(false)
				}

				// 网格覆盖层
				if showGrid && gridType != .off {
					gridOverlay(size: size)
						.zIndex(0.3)
						.allowsHitTesting(false)
				}

				// 拍照闪光效果
				if captureFlashOpacity > 0 {
					Color.white
						.opacity(captureFlashOpacity)
						.ignoresSafeArea()
						.zIndex(0.5)
						.allowsHitTesting(false)
				}

				// 手势层
				gestureLayer(size: size)
					.zIndex(1)

				// 对焦指示器
				if showFocusIndicator {
					focusIndicatorView
						.position(focusPoint)
						.zIndex(1.5)
						.allowsHitTesting(false)
				}

				// 对焦锁定指示器
				if showFocusLock {
					focusLockIndicator
						.position(focusLockPoint)
						.zIndex(1.5)
						.allowsHitTesting(false)
				}

				// 曝光补偿滑块
				if showExposureSlider {
					exposureSliderView
						.zIndex(1.5)
				}

				// 水平仪
				if showLevel {
					levelIndicatorView(size: size)
						.zIndex(1.5)
						.allowsHitTesting(false)
				}

				// UI 覆盖层
				VStack(spacing: 0) {
					if !isImmersiveMode {
						TopControlBar(
							onFlashToggle: { cycleFlashMode() },
							onTimerTap: { toggleTimerSelection() },
							onAspectRatioTap: { cycleAspectRatio() },
							onBeautyTap: { toggleBeautyPanel() },
							onPortraitModeTap: { togglePortraitModePanel() },
							onSettingsTap: { presentSettings() },
							flashMode: flashModeBarIcon,
							showControls: showControls,
							isBeautyEnabled: portraitViewModel.isBeautyEnabled,
							isPortraitModeEnabled: portraitViewModel.isPortraitModeEnabled
						)
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
								.background(Capsule().fill(DesignSystem.Colors.minimalDarkOverlay))
								.padding(.trailing, 16)
								.padding(.top, 12)
						}
						Spacer()
					}
					.zIndex(2)
					.transition(.opacity)
				}

				// 计时器选择浮层
				if showTimerSelection {
					timerSelectionOverlay
						.zIndex(3)
						.transition(.opacity)
				}

				// 计时器倒计时覆盖层
				if timerCountdown > 0 {
					countdownOverlay
						.zIndex(4)
				}

				// 录像状态指示器
				if isRecording {
					recordingIndicator(safeInsets: safeInsets)
						.zIndex(3)
				}

				// 照片回看界面
				if showPhotoReview, let image = lastCapturedImage {
					photoReviewView(image: image)
						.zIndex(5)
						.transition(.opacity)
				}

				// 美颜面板（底部滑出）
				if showBeautyPanel {
					beautyPanelOverlay(safeInsets: safeInsets)
						.zIndex(6)
						.transition(.move(edge: .bottom).combined(with: .opacity))
				}

				// 人像模式面板（底部滑出）
				if showPortraitModePanel {
					portraitModeOverlay(safeInsets: safeInsets)
						.zIndex(6)
						.transition(.move(edge: .bottom).combined(with: .opacity))
				}
			}
		}
		.ignoresSafeArea()
		.navigationBarBackButtonHidden(true)
		.preferredColorScheme(.dark)
		.onAppear {
			viewModel.onAppear()
			viewModel.onCaptureTriggered = { triggerCaptureAnimation() }
			resetAutoHideTimer()
			startMotionUpdates()
			setupFilterPreview()
			setupPhotoCallback()
			// 同步初始闪光灯模式
			flashMode = viewModel.camera.currentFlashMode
		}
		.onDisappear {
			viewModel.onDisappear()
			autoHideWorkItem?.cancel()
			stopMotionUpdates()
			stopRecording()
			timerWorkItem?.cancel()
			viewModel.camera.onFilteredFrame = nil
		}
		.onChange(of: viewModel.zoomState.displayedFactor) { _, newFactor in
			updateZoomDisplay(newFactor)
		}
		.onChange(of: viewModel.camera.currentFlashMode) { _, newMode in
			flashMode = newMode
		}
	}

	// MARK: - Camera Preview

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

	// MARK: - Flash Mode

	private var flashModeBarIcon: TopControlBar.FlashMode {
		switch flashMode {
		case .auto: return .auto
		case .on: return .on
		case .off: return .off
		@unknown default: return .auto
		}
	}

	private func cycleFlashMode() {
		switch flashMode {
		case .auto: flashMode = .on
		case .on: flashMode = .off
		case .off: flashMode = .auto
		@unknown default: flashMode = .auto
		}
		viewModel.camera.setFlashMode(flashMode)
		HapticManager.shared.light()
		resetAutoHideTimer()
	}

	// MARK: - Timer

	private func toggleTimerSelection() {
		HapticManager.shared.light()
		withAnimation(DesignSystem.Animation.overlayFade) {
			showTimerSelection.toggle()
		}
		resetAutoHideTimer()
	}

	private func selectTimerMode(_ mode: TimerMode) {
		HapticManager.shared.selection()
		timerMode = mode
		withAnimation(DesignSystem.Animation.overlayFade) {
			showTimerSelection = false
		}
		resetAutoHideTimer()
	}

	private func startTimerCapture() {
		let seconds: Int
		switch timerMode {
		case .off: return
		case .three: seconds = 3
		case .five: seconds = 5
		case .ten: seconds = 10
		}
		timerCountdown = seconds
		runCountdown(seconds)
	}

	private func runCountdown(_ remaining: Int) {
		guard remaining > 0 else {
			timerCountdown = 0
			HapticManager.shared.heavy()
			performCapture()
			return
		}
		timerCountdown = remaining
		HapticManager.shared.countdown(step: remaining, total: timerMode == .three ? 3 : timerMode == .five ? 5 : 10)
		let work = DispatchWorkItem { [self] in
			runCountdown(remaining - 1)
		}
		timerWorkItem = work
		DispatchQueue.main.asyncAfter(deadline: .now() + 1.0, execute: work)
	}

	private var timerSelectionOverlay: some View {
		ZStack {
			Color.black.opacity(0.5)
				.ignoresSafeArea()
				.onTapGesture {
					withAnimation(DesignSystem.Animation.overlayFade) { showTimerSelection = false }
				}

			VStack(spacing: 0) {
				Text("计时拍摄")
					.font(DesignSystem.Typography.headline)
					.foregroundColor(.white)
					.padding(.bottom, 20)

				ForEach(TimerMode.allCases, id: \.self) { mode in
					Button {
						selectTimerMode(mode)
					} label: {
						HStack {
							Text(mode == .off ? "关闭" : "\(mode == .three ? 3 : mode == .five ? 5 : 10)秒")
								.font(DesignSystem.Typography.body)
								.foregroundColor(.white)
							Spacer()
							if timerMode == mode {
								Image(systemName: "checkmark")
									.font(.system(size: 16, weight: .semibold))
									.foregroundColor(DesignSystem.Colors.primary)
							}
						}
						.padding(.horizontal, 24)
						.padding(.vertical, 14)
					}
				}
			}
			.padding(.vertical, 20)
			.background(
				RoundedRectangle(cornerRadius: 16)
					.fill(Color.black.opacity(0.85))
			)
			.padding(.horizontal, 60)
		}
	}

	private var countdownOverlay: some View {
		ZStack {
			Color.black.opacity(0.4)
				.ignoresSafeArea()

			Text("\(timerCountdown)")
				.font(.system(size: 120, weight: .thin, design: .rounded))
				.foregroundColor(.white)
				.transition(.scale.combined(with: .opacity))
				.animation(.easeInOut(duration: 0.15), value: timerCountdown)
		}
	}

	// MARK: - Aspect Ratio

	private func cycleAspectRatio() {
		HapticManager.shared.light()
		let all: [AspectRatio] = [.full, .oneOne, .threeFour, .nineSixteen]
		if let idx = all.firstIndex(of: aspectRatio) {
			aspectRatio = all[(idx + 1) % all.count]
		}
		resetAutoHideTimer()
	}

	private func aspectRatioMask(size: CGSize) -> some View {
		let ratio = aspectRatio.ratio ?? 1.0
		let previewHeight: CGFloat
		let previewWidth: CGFloat
		if size.width / size.height > ratio {
			previewHeight = size.height
			previewWidth = size.height * ratio
		} else {
			previewWidth = size.width
			previewHeight = size.width / ratio
		}

		return ZStack {
			Rectangle()
				.fill(Color.black.opacity(0.6))
				.mask(
					Rectangle()
						.overlay(
							RoundedRectangle(cornerRadius: 2)
								.frame(width: previewWidth, height: previewHeight)
								.blendMode(.destinationOut)
						)
				)
		}
		.ignoresSafeArea()
	}

	// MARK: - Grid Overlay

	private func gridOverlay(size: CGSize) -> some View {
		Canvas { context, canvasSize in
			let w = canvasSize.width
			let h = canvasSize.height
			let lineColor = Color.white.opacity(0.35)

			context.stroke(
				Path(CGRect(x: 0, y: 0, width: w, height: h)),
				with: .color(lineColor),
				lineWidth: 0.5
			)

			switch gridType {
			case .thirds:
				let v1 = w / 3
				let v2 = w * 2 / 3
				let h1 = h / 3
				let h2 = h * 2 / 3
				context.stroke(Path { p in p.move(to: CGPoint(x: v1, y: 0)); p.addLine(to: CGPoint(x: v1, y: h)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: v2, y: 0)); p.addLine(to: CGPoint(x: v2, y: h)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: 0, y: h1)); p.addLine(to: CGPoint(x: w, y: h1)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: 0, y: h2)); p.addLine(to: CGPoint(x: w, y: h2)) }, with: .color(lineColor), lineWidth: 0.5)

			case .golden:
				let phi: CGFloat = 1.618
				let v1 = w / phi
				let v2 = w - w / phi
				let h1 = h / phi
				let h2 = h - h / phi
				context.stroke(Path { p in p.move(to: CGPoint(x: v1, y: 0)); p.addLine(to: CGPoint(x: v1, y: h)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: v2, y: 0)); p.addLine(to: CGPoint(x: v2, y: h)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: 0, y: h1)); p.addLine(to: CGPoint(x: w, y: h1)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: 0, y: h2)); p.addLine(to: CGPoint(x: w, y: h2)) }, with: .color(lineColor), lineWidth: 0.5)

			case .nineGrid:
				let v1 = w / 3
				let v2 = w * 2 / 3
				let h1 = h / 3
				let h2 = h * 2 / 3
				context.stroke(Path { p in p.move(to: CGPoint(x: v1, y: 0)); p.addLine(to: CGPoint(x: v1, y: h)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: v2, y: 0)); p.addLine(to: CGPoint(x: v2, y: h)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: 0, y: h1)); p.addLine(to: CGPoint(x: w, y: h1)) }, with: .color(lineColor), lineWidth: 0.5)
				context.stroke(Path { p in p.move(to: CGPoint(x: 0, y: h2)); p.addLine(to: CGPoint(x: w, y: h2)) }, with: .color(lineColor), lineWidth: 0.5)
				for i in 0..<3 {
					for j in 0..<3 {
						let cx = v1 * CGFloat(i) + v1 / 2
						let cy = h1 * CGFloat(j) + h1 / 2
						context.fill(Path(ellipseIn: CGRect(x: cx - 2, y: cy - 2, width: 4, height: 4)), with: .color(lineColor))
					}
				}

			case .off: break
			}
		}
		.ignoresSafeArea()
	}

	// MARK: - Level Indicator

	private func startMotionUpdates() {
		guard motionManager.isDeviceMotionAvailable else { return }
		motionManager.deviceMotionUpdateInterval = 0.05
		motionManager.startDeviceMotionUpdates(to: .main) { motion, _ in
			guard let motion = motion else { return }
			let gravity = motion.gravity
			self.levelAngle = atan2(gravity.x, gravity.y) * 180 / .pi
		}
	}

	private func stopMotionUpdates() {
		motionManager.stopDeviceMotionUpdates()
	}

	private func levelIndicatorView(size: CGSize) -> some View {
		VStack {
			Spacer()
			ZStack {
				RoundedRectangle(cornerRadius: 2)
					.fill(Color.white.opacity(0.2))
					.frame(width: min(size.width * 0.6, 200), height: 2)

				RoundedRectangle(cornerRadius: 2)
					.fill(abs(levelAngle) < 0.5 ? Color.green : abs(levelAngle) < 2.0 ? Color.yellow : Color.white)
					.frame(width: min(size.width * 0.6, 200), height: 2)

				Circle()
					.fill(abs(levelAngle) < 0.5 ? Color.green : Color.white)
					.frame(width: 6, height: 6)
					.offset(x: min(max(levelAngle * 3, -min(size.width * 0.3, 100)), min(size.width * 0.3, 100)))
			}
			.padding(.bottom, 120)
		}
	}

	// MARK: - Focus & Exposure

	private func handleTapFocus(at location: CGPoint, in size: CGSize) {
		guard !isFocusLocked else { return }
		HapticManager.shared.medium()
		focusPoint = location
		showFocusIndicator = true
		showExposureSlider = true
		focusIndicatorScale = 1.5
		withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
			focusIndicatorScale = 1.0
		}

		// 设置设备对焦点
		let devicePoint = CGPoint(x: location.x / size.width, y: location.y / size.height)
		viewModel.camera.sessionQueue.async { [weak viewModel] in
			guard let device = viewModel?.camera.activeVideoDevice else { return }
			do {
				try device.lockForConfiguration()
				if device.isFocusPointOfInterestSupported {
					device.focusPointOfInterest = devicePoint
					device.focusMode = .autoFocus
				}
				if device.isExposurePointOfInterestSupported {
					device.exposurePointOfInterest = devicePoint
					device.exposureMode = .autoExpose
				}
				device.unlockForConfiguration()
			} catch {}
		}

		DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
			withAnimation(DesignSystem.Animation.overlayFade) {
				showFocusIndicator = false
			}
		}
	}

	private var focusIndicatorView: some View {
		Image(systemName: "squareshape.split.2x2")
			.font(.system(size: 24, weight: .light))
			.foregroundColor(.yellow)
			.frame(width: 60, height: 60)
			.scaleEffect(focusIndicatorScale)
			.animation(.spring(response: 0.3, dampingFraction: 0.6), value: focusIndicatorScale)
	}

	private func handleLongPressFocus(at location: CGPoint, in size: CGSize) {
		if isFocusLocked {
			// 解锁
			isFocusLocked = false
			showFocusLock = false
			viewModel.camera.sessionQueue.async { [weak viewModel] in
				guard let device = viewModel?.camera.activeVideoDevice else { return }
				do {
					try device.lockForConfiguration()
					if device.isFocusModeSupported(.continuousAutoFocus) {
						device.focusMode = .continuousAutoFocus
					}
					if device.isExposureModeSupported(.continuousAutoExposure) {
						device.exposureMode = .continuousAutoExposure
					}
					device.unlockForConfiguration()
				} catch {}
			}
			HapticManager.shared.light()
		} else {
			// 锁定
			isFocusLocked = true
			focusLockPoint = location
			showFocusLock = true
			let devicePoint = CGPoint(x: location.x / size.width, y: location.y / size.height)
			viewModel.camera.sessionQueue.async { [weak viewModel] in
				guard let device = viewModel?.camera.activeVideoDevice else { return }
				do {
					try device.lockForConfiguration()
					if device.isFocusPointOfInterestSupported {
						device.focusPointOfInterest = devicePoint
						device.focusMode = .autoFocus
					}
					if device.isExposurePointOfInterestSupported {
						device.exposurePointOfInterest = devicePoint
						device.exposureMode = .autoExpose
					}
					device.unlockForConfiguration()
				} catch {}
			}
			HapticManager.shared.focusLock()
		}
		resetAutoHideTimer()
	}

	private var focusLockIndicator: some View {
		ZStack {
			Circle()
				.stroke(Color.yellow, lineWidth: 2)
				.frame(width: 70, height: 70)

			Image(systemName: "lock.fill")
				.font(.system(size: 14, weight: .bold))
				.foregroundColor(.yellow)
		}
	}

	private var exposureSliderView: some View {
		VStack {
			Spacer()
			HStack {
				Spacer()
				VStack(spacing: 4) {
					Text("EV \(exposureBias >= 0 ? "+" : "")\(String(format: "%.1f", exposureBias))")
						.font(DesignSystem.Typography.caption1)
						.foregroundColor(.white)

					VStack {
						Slider(value: $exposureBias, in: -2...2, step: 0.1) { editing in
							if !editing {
								applyExposureBias()
							}
						}
						.frame(width: 30, height: 150)
						.rotationEffect(.degrees(-90))
						.tint(.yellow)
					}
					.frame(width: 30, height: 150)
				}
				.padding(.trailing, 20)
				.padding(.bottom, 200)
			}
		}
	}

	private func applyExposureBias() {
		viewModel.camera.sessionQueue.async { [weak viewModel] in
			guard let device = viewModel?.camera.activeVideoDevice else { return }
			do {
				try device.lockForConfiguration()
				let clamped = max(min(exposureBias, device.maxExposureTargetBias), device.minExposureTargetBias)
				device.setExposureTargetBias(clamped, completionHandler: nil)
				device.unlockForConfiguration()
			} catch {}
		}
	}

	// MARK: - Filter

	private func setupFilterPreview() {
		let ctx = self.filterPreviewContext
		viewModel.camera.onFilteredFrame = { ciImage in
			let extent = ciImage.extent
			guard let cgImage = ctx.createCGImage(ciImage, from: extent) else {
				return
			}
			let uiImage = UIImage(cgImage: cgImage)
			DispatchQueue.main.async {
				self.filteredPreviewImage = uiImage
			}
		}
	}

	private func setupPhotoCallback() {
		let ctx = self.filterPreviewContext
		let beautyCtx = CIContext(options: [.workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!])
		viewModel.camera.onPhotoDataReady = { [weak self] data in
			guard let self = self else { return }
			let methodName = self.viewModel.detectionMode.displayName
			let watermarkConfig = WatermarkConfig.load()

			// 先处理滤镜，再处理水印
			var processedData: Data = data

			// 美颜处理（在滤镜之前）
			if self.portraitViewModel.isBeautyEnabled || self.portraitViewModel.isPortraitModeEnabled {
				if let image = UIImage(data: data),
				   let ciImage = CIImage(image: image) {
					let beautyResult = self.portraitViewModel.applyBeautyPipeline(to: ciImage)
					if let cgImage = beautyCtx.createCGImage(beautyResult, from: beautyResult.extent) {
						let beautyImage = UIImage(cgImage: cgImage)
						if let jpegData = beautyImage.jpegData(compressionQuality: 0.95) {
							processedData = jpegData
						}
					}
				}
			}

			// 滤镜处理
			if let filter = self.selectedFilter {
				if let image = UIImage(data: processedData),
				   let ciImage = CIImage(image: image) {
					let processor = LutFilterProcessor()
					let filtered = processor.applyFilter(to: ciImage, preset: filter, intensity: self.filterIntensity)
					if let cgImage = ctx.createCGImage(filtered, from: filtered.extent) {
						let filteredImage = UIImage(cgImage: cgImage)
						if let filteredData = filteredImage.jpegData(compressionQuality: 0.95) {
							processedData = filteredData
						}
					}
				}
			}

			// 应用水印
			if watermarkConfig.isEnabled {
				if let watermarkedData = WatermarkService.shared.applyWatermark(to: processedData, config: watermarkConfig) {
					processedData = watermarkedData
				}
			}

			PhotoStorageService.shared.savePhoto(data: processedData, detectionMethod: methodName)
		}
	}

	private func applyFilter(_ preset: LutFilterPreset) {
		HapticManager.shared.selection()
		selectedFilter = preset
		filterIntensity = preset.defaultIntensity
		viewModel.camera.activeFilterPreset = preset
		viewModel.camera.activeFilterIntensity = preset.defaultIntensity
		resetAutoHideTimer()
	}

	// MARK: - Gesture Layer

	private func gestureLayer(size: CGSize) -> some View {
		Color.clear
			.contentShape(Rectangle())
			// 单击：切换控件 / 退出沉浸模式 / 对焦
			.onTapGesture(count: 1) { location in
				if isImmersiveMode {
					withAnimation(DesignSystem.Animation.smooth) {
						isImmersiveMode = false
						showControls = true
					}
					resetAutoHideTimer()
				} else if showExposureSlider || showTimerSelection {
					withAnimation(DesignSystem.Animation.overlayFade) {
						showExposureSlider = false
						showTimerSelection = false
					}
				} else {
					handleTapFocus(at: location, in: size)
					withAnimation(DesignSystem.Animation.overlayFade) {
						showControls.toggle()
						showFilterStrip = false
						showModeSelector = false
					}
					if showControls { resetAutoHideTimer() }
				}
			}
			// 双击切换摄像头
			.onTapGesture(count: 2) { _ in
				triggerCameraFlipAnimation()
				viewModel.toggleCameraPosition()
				resetAutoHideTimer()
			}
			// 长按：对焦锁定
			.simultaneousGesture(
				LongPressGesture(minimumDuration: 0.4)
					.onEnded { _ in
						handleLongPressFocus(at: focusLockPoint == .zero ? CGPoint(x: size.width / 2, y: size.height / 2) : focusLockPoint, in: size)
					}
			)
			// 双指缩放
			.simultaneousGesture(
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
							withAnimation(DesignSystem.Animation.overlayFade) { showZoomIndicator = false }
						}
					}
			)
			// 双指下滑 → 沉浸模式
			.simultaneousGesture(
				DragGesture(minimumDistance: 40)
					.onEnded { value in
						guard !isGestureActive else { return }
						if value.translation.height > 60 && abs(value.translation.width) < 60 {
							withAnimation(DesignSystem.Animation.smooth) {
								isImmersiveMode = true
								showControls = false
								showFilterStrip = false
								showModeSelector = false
							}
							HapticManager.shared.medium()
						}
					}
			)
	}

	// MARK: - Bottom Control Bar

	private func bottomControlBar(bottomInset: CGFloat) -> some View {
		HStack(alignment: .bottom) {
			albumThumbnailButton
			Spacer()
			CaptureButton(
				action: {
					if timerMode != .off {
						startTimerCapture()
					} else {
						HapticManager.shared.capture()
						performCapture()
					}
				},
				onLongPressStart: { startRecording() },
				onLongPressEnd: { stopRecording() }
			)
			Spacer()
			flipCameraButton
		}
		.padding(.horizontal, 24)
	}

	private func performCapture() {
		viewModel.capturePhoto()
		triggerCaptureAnimation()
	}

	// MARK: - Video Recording

	private func startRecording() {
		guard !isRecording else { return }
		isRecording = true
		recordingStartTime = Date()
		recordingDuration = "00:00"
		recordingTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
			guard let start = recordingStartTime else { return }
			let elapsed = Date().timeIntervalSince(start)
			let minutes = Int(elapsed) / 60
			let seconds = Int(elapsed) % 60
			recordingDuration = String(format: "%02d:%02d", minutes, seconds)
		}
		HapticManager.shared.heavy()
	}

	private func stopRecording() {
		guard isRecording else { return }
		isRecording = false
		recordingTimer?.invalidate()
		recordingTimer = nil
		recordingDuration = "00:00"
		HapticManager.shared.heavy()
	}

	private func recordingIndicator(safeInsets: EdgeInsets) -> some View {
		VStack {
			HStack {
				HStack(spacing: 6) {
					Circle()
						.fill(Color.red)
						.frame(width: 8, height: 8)
						.opacity(isRecording && (Int(Date().timeIntervalSince1970 * 2) % 2 == 0) ? 1 : 0.3)
					Text(recordingDuration)
						.font(DesignSystem.Typography.monoCaption)
						.foregroundColor(.white)
				}
				.padding(.horizontal, 12)
				.padding(.vertical, 6)
				.background(Capsule().fill(Color.black.opacity(0.6)))
				.padding(.top, safeInsets.top > 0 ? safeInsets.top + 8 : 20)
				Spacer()
			}
			.padding(.horizontal, 16)
			Spacer()
		}
	}

	// MARK: - Photo Review

	private func photoReviewView(image: UIImage) -> some View {
		ZStack {
			Color.black
				.ignoresSafeArea()

			VStack(spacing: 0) {
				// 顶部操作栏
				HStack {
					Button {
						withAnimation(DesignSystem.Animation.overlayFade) {
							showPhotoReview = false
							lastCapturedImage = nil
						}
					} label: {
						Image(systemName: "xmark")
							.font(.system(size: 18, weight: .medium))
							.foregroundColor(.white)
							.frame(width: 40, height: 40)
					}
					.accessibilityLabel("关闭预览")
					.accessibilityHint("双击关闭照片预览")

					Spacer()

					Text("预览")
						.font(DesignSystem.Typography.headline)
						.foregroundColor(.white)

					Spacer()

					Button {
						// 分享
						sharePhoto(image)
					} label: {
						Image(systemName: "square.and.arrow.up")
							.font(.system(size: 18, weight: .medium))
							.foregroundColor(.white)
							.frame(width: 40, height: 40)
					}
					.accessibilityLabel("分享照片")
					.accessibilityHint("双击分享照片")
				}
				.padding(.horizontal, 16)
				.padding(.top, 8)

				Spacer()

				// 照片预览
				Image(uiImage: image)
					.resizable()
					.aspectRatio(contentMode: .fit)
					.frame(maxWidth: .infinity, maxHeight: .infinity)

				Spacer()

				// 底部操作栏
				HStack(spacing: 40) {
					Button {
						// 删除
						withAnimation(DesignSystem.Animation.overlayFade) {
							showPhotoReview = false
							lastCapturedImage = nil
							lastCapturedData = nil
						}
					} label: {
						VStack(spacing: 4) {
							Image(systemName: "trash")
								.font(.system(size: 22))
							Text("删除")
								.font(DesignSystem.Typography.caption1)
						}
						.foregroundColor(.white.opacity(0.8))
					}
					.accessibilityLabel("删除照片")
					.accessibilityHint("双击删除当前照片")

					Button {
						// 编辑
						editPhoto(image)
					} label: {
						VStack(spacing: 4) {
							Image(systemName: "slider.horizontal.3")
								.font(.system(size: 22))
							Text("编辑")
								.font(DesignSystem.Typography.caption1)
						}
						.foregroundColor(.white.opacity(0.8))
					}
					.accessibilityLabel("编辑照片")
					.accessibilityHint("双击编辑照片")

					Button {
						// 保存
						savePhoto()
					} label: {
						VStack(spacing: 4) {
							Image(systemName: "square.and.arrow.down")
								.font(.system(size: 22))
							Text("保存")
								.font(DesignSystem.Typography.caption1)
						}
						.foregroundColor(DesignSystem.Colors.primary)
					}
					.accessibilityLabel("保存照片")
					.accessibilityHint("双击保存照片到相册")
				}
				.padding(.bottom, 40)
			}
		}
	}

	private func sharePhoto(_ image: UIImage) {
		guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
			  let root = windowScene.windows.first?.rootViewController else { return }
		ShareManager.shared.sharePhoto(image: image, from: root)
	}

	private func editPhoto(_ image: UIImage) {
		// 打开系统照片编辑
		if let data = lastCapturedData ?? image.jpegData(compressionQuality: 0.95) {
			lastCapturedData = data
		}
		// 保存到临时目录并打开
		let tmpPath = NSTemporaryDirectory() + "livecapture_edit.jpg"
		if let data = lastCapturedData {
			try? data.write(to: URL(fileURLWithPath: tmpPath))
			let url = URL(fileURLWithPath: tmpPath)
			let av = UIActivityViewController(activityItems: [url], applicationActivities: nil)
			guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
				  let root = windowScene.windows.first?.rootViewController else { return }
			root.present(av, animated: true)
		}
	}

	private func savePhoto() {
		if let data = lastCapturedData {
			PhotoStorageService.shared.savePhoto(data: data, detectionMethod: viewModel.detectionMode.displayName)
		}
		withAnimation(DesignSystem.Animation.overlayFade) {
			showPhotoReview = false
			lastCapturedImage = nil
			lastCapturedData = nil
		}
		HapticManager.shared.success()
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
		.accessibilityLabel("相册")
		.accessibilityHint("双击打开系统相册")
		.accessibilityAddTraits(.isButton)
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
				.background(Circle().fill(DesignSystem.Colors.minimalDarkOverlay))
		}
		.accessibilityLabel("切换摄像头")
		.accessibilityHint("双击切换前后摄像头")
		.accessibilityAddTraits(.isButton)
	}

	// MARK: - Zoom Indicator

	private var zoomIndicator: some View {
		Text(zoomLevelText)
			.font(DesignSystem.Typography.minimalZoomIndicator)
			.foregroundColor(DesignSystem.Colors.minimalLabel)
			.padding(.horizontal, 12)
			.padding(.vertical, 6)
			.background(Capsule().fill(DesignSystem.Colors.minimalDarkOverlay))
			.padding(.bottom, 8)
	}

	// MARK: - Actions

	private func presentSettings() {
		HapticManager.shared.light()
		dismiss()
	}

	// MARK: - Beauty & Portrait Mode

	private func toggleBeautyPanel() {
		HapticManager.shared.light()
		withAnimation(DesignSystem.Animation.modeSlide) {
			if showPortraitModePanel {
				showPortraitModePanel = false
			}
			showBeautyPanel.toggle()
			if showBeautyPanel {
				portraitViewModel.isBeautyEnabled = true
			}
		}
		resetAutoHideTimer()
	}

	private func togglePortraitModePanel() {
		HapticManager.shared.light()
		withAnimation(DesignSystem.Animation.modeSlide) {
			if showBeautyPanel {
				showBeautyPanel = false
			}
			showPortraitModePanel.toggle()
			if showPortraitModePanel {
				portraitViewModel.isPortraitModeEnabled = true
			}
		}
		resetAutoHideTimer()
	}

	private func beautyPanelOverlay(safeInsets: EdgeInsets) -> some View {
		VStack {
			Spacer()
			BeautyPanelView(viewModel: portraitViewModel) {
				withAnimation(DesignSystem.Animation.modeSlide) {
					showBeautyPanel = false
				}
			}
			.padding(.bottom, safeInsets.bottom)
		}
		.background(
			Color.black.opacity(0.3)
				.ignoresSafeArea()
				.onTapGesture {
					withAnimation(DesignSystem.Animation.modeSlide) {
						showBeautyPanel = false
					}
				}
		)
	}

	private func portraitModeOverlay(safeInsets: EdgeInsets) -> some View {
		VStack {
			Spacer()
			PortraitModeView(viewModel: portraitViewModel) {
				withAnimation(DesignSystem.Animation.modeSlide) {
					showPortraitModePanel = false
				}
			}
			.padding(.bottom, safeInsets.bottom)
		}
		.background(
			Color.black.opacity(0.3)
				.ignoresSafeArea()
				.onTapGesture {
					withAnimation(DesignSystem.Animation.modeSlide) {
						showPortraitModePanel = false
					}
				}
		)
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
		withAnimation(.easeOut(duration: 0.1)) { captureFlashOpacity = 0.8 }
		withAnimation(.easeIn(duration: 0.2).delay(0.1)) { captureFlashOpacity = 0.0 }
		withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) { captureAnimationScale = 2.0 }
		DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
			withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) { captureAnimationScale = 1.0 }
		}
	}

	private func triggerCameraFlipAnimation() {
		withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
			cameraFlipRotation += 180
		}
	}
}

// MARK: - CaptureViewModel Extension for CaptureView

extension CaptureViewModel {
	var detectionMode: DetectionMode {
		// 通过 reflection 访问私有属性（仅用于 CaptureView 内部）
		Mirror(reflecting: self).children.first(where: { $0.label == "detectionMode" })?.value as? DetectionMode ?? .fast
	}
}

#endif