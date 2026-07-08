//
//  CameraManagerTests.swift
//  LiveCaptureTests
//
//  CameraManager 单元测试：闪光灯、变焦状态转换、摄像头位置切换
//

import XCTest
import AVFoundation
import Combine
@testable import LiveCapture

final class CameraManagerTests: XCTestCase {

	var cameraManager: CameraManager!
	var cancellables: Set<AnyCancellable>!

	override func setUp() {
		super.setUp()
		cameraManager = CameraManager()
		cancellables = []
	}

	override func tearDown() {
		cameraManager = nil
		cancellables = nil
		super.tearDown()
	}

	// MARK: - Session 测试

	func test_initialization_createsSession() {
		XCTAssertNotNil(cameraManager.session)
		XCTAssertTrue(cameraManager.session is AVCaptureSession)
	}

	func test_initialization_sessionPresetIsPhoto() {
		XCTAssertEqual(cameraManager.session.sessionPreset, .photo)
	}

	func test_initialization_sessionIsNotRunning() {
		XCTAssertFalse(cameraManager.isSessionRunning)
	}

	// MARK: - 摄像头位置测试

	func test_initialization_defaultPositionIsBack() {
		XCTAssertEqual(cameraManager.currentPosition, .back)
	}

	func test_currentPosition_isBack() {
		XCTAssertEqual(cameraManager.currentPosition, AVCaptureDevice.Position.back)
	}

	// MARK: - 闪光灯模式测试

	func test_initialization_flashModeIsAuto() {
		XCTAssertEqual(cameraManager.currentFlashMode, .auto)
	}

	func test_flashMode_setToOn() {
		cameraManager.currentFlashMode = .on
		XCTAssertEqual(cameraManager.currentFlashMode, .on)
	}

	func test_flashMode_setToOff() {
		cameraManager.currentFlashMode = .off
		XCTAssertEqual(cameraManager.currentFlashMode, .off)
	}

	func test_flashMode_setToAuto() {
		cameraManager.currentFlashMode = .on
		cameraManager.currentFlashMode = .auto
		XCTAssertEqual(cameraManager.currentFlashMode, .auto)
	}

	func test_flashMode_allModesCanBeSet() {
		let modes: [AVCaptureDevice.FlashMode] = [.auto, .on, .off]
		for mode in modes {
			cameraManager.currentFlashMode = mode
			XCTAssertEqual(cameraManager.currentFlashMode, mode)
		}
	}

	// MARK: - ZoomState 测试

	func test_initialization_zoomStateHasDefaultValues() {
		let zoomState = cameraManager.zoomState
		XCTAssertEqual(zoomState.currentFactor, 1.0)
		XCTAssertEqual(zoomState.displayedFactor, 1.0)
		XCTAssertFalse(zoomState.isContinuous)
	}

	func test_initialization_zoomStateLensIsWide() {
		XCTAssertEqual(cameraManager.zoomState.activeLens, .wide)
	}

	func test_initialization_zoomRangeIsDefault() {
		XCTAssertEqual(cameraManager.zoomRange.lowerBound, 1.0)
		XCTAssertEqual(cameraManager.zoomRange.upperBound, 1.0)
	}

	func test_initialization_zoomPresetsIsEmpty() {
		XCTAssertTrue(cameraManager.zoomPresets.isEmpty)
	}

	// MARK: - 变焦状态转换测试

	func test_updateZoomState_updatesCorrectly() {
		let newState = CameraManager.ZoomState(
			currentFactor: 2.0,
			displayedFactor: 2.0,
			focalLength: 48,
			activeLens: .telephoto,
			isContinuous: true
		)
		cameraManager.updateZoomState(newState)
		XCTAssertEqual(cameraManager.zoomState.currentFactor, 2.0)
		XCTAssertEqual(cameraManager.zoomState.focalLength, 48)
		XCTAssertEqual(cameraManager.zoomState.activeLens, .telephoto)
		XCTAssertTrue(cameraManager.zoomState.isContinuous)
	}

	func test_zoomState_transitionToUltraWide() {
		var state = cameraManager.zoomState
		state = CameraManager.ZoomState(
			currentFactor: 0.5,
			displayedFactor: 0.5,
			focalLength: 13,
			activeLens: .ultraWide,
			isContinuous: false
		)
		cameraManager.updateZoomState(state)

		XCTAssertEqual(cameraManager.zoomState.currentFactor, 0.5)
		XCTAssertEqual(cameraManager.zoomState.activeLens, .ultraWide)
		XCTAssertEqual(cameraManager.zoomState.focalLength, 13)
	}

	func test_zoomState_transitionToWide() {
		var state = cameraManager.zoomState
		state = CameraManager.ZoomState(
			currentFactor: 1.0,
			displayedFactor: 1.0,
			focalLength: 24,
			activeLens: .wide,
			isContinuous: false
		)
		cameraManager.updateZoomState(state)

		XCTAssertEqual(cameraManager.zoomState.currentFactor, 1.0)
		XCTAssertEqual(cameraManager.zoomState.activeLens, .wide)
		XCTAssertEqual(cameraManager.zoomState.focalLength, 24)
	}

	func test_zoomState_transitionToTelephoto() {
		var state = cameraManager.zoomState
		state = CameraManager.ZoomState(
			currentFactor: 3.0,
			displayedFactor: 3.0,
			focalLength: 77,
			activeLens: .telephoto,
			isContinuous: false
		)
		cameraManager.updateZoomState(state)

		XCTAssertEqual(cameraManager.zoomState.currentFactor, 3.0)
		XCTAssertEqual(cameraManager.zoomState.activeLens, .telephoto)
		XCTAssertEqual(cameraManager.zoomState.focalLength, 77)
	}

	func test_zoomState_transitionToContinuous() {
		var state = cameraManager.zoomState
		state = CameraManager.ZoomState(
			currentFactor: 1.5,
			displayedFactor: 1.5,
			focalLength: 36,
			activeLens: .wide,
			isContinuous: true
		)
		cameraManager.updateZoomState(state)

		XCTAssertTrue(cameraManager.zoomState.isContinuous)
		XCTAssertEqual(cameraManager.zoomState.currentFactor, 1.5)
	}

	func test_zoomState_transitionToPreset() {
		var state = cameraManager.zoomState
		state = CameraManager.ZoomState(
			currentFactor: 2.0,
			displayedFactor: 2.0,
			focalLength: 48,
			activeLens: .wide,
			isContinuous: false
		)
		cameraManager.updateZoomState(state)

		XCTAssertFalse(cameraManager.zoomState.isContinuous)
		XCTAssertEqual(cameraManager.zoomState.displayedFactor, 2.0)
	}

	func test_zoomState_equatable() {
		let state1 = CameraManager.ZoomState(
			currentFactor: 1.0, displayedFactor: 1.0, focalLength: 24, activeLens: .wide, isContinuous: false
		)
		let state2 = CameraManager.ZoomState(
			currentFactor: 1.0, displayedFactor: 1.0, focalLength: 24, activeLens: .wide, isContinuous: false
		)
		let state3 = CameraManager.ZoomState(
			currentFactor: 2.0, displayedFactor: 2.0, focalLength: 48, activeLens: .wide, isContinuous: false
		)

		XCTAssertEqual(state1, state2)
		XCTAssertNotEqual(state1, state3)
	}

	// MARK: - applyZoomConfiguration 测试

	func test_applyZoomConfiguration_updatesZoomProperties() {
		let range: ClosedRange<CGFloat> = 0.5...5.0
		let lenses: [CameraManager.LensKind] = [.ultraWide, .wide, .telephoto]
		let presets: [CameraManager.ZoomPreset] = [
			CameraManager.ZoomPreset(
				lens: .wide,
				zoomFactor: 1.0,
				focalLength: 24,
				style: .primary
			)
		]

		cameraManager.applyZoomConfiguration(
			range: range,
			lenses: lenses,
			presets: presets,
			targetFactor: 1.0,
			isContinuous: false
		)

		XCTAssertEqual(cameraManager.zoomRange.lowerBound, 0.5)
		XCTAssertEqual(cameraManager.zoomRange.upperBound, 5.0)
		XCTAssertEqual(cameraManager.availableLenses.count, 3)
		XCTAssertTrue(cameraManager.availableLenses.contains(.wide))
		XCTAssertEqual(cameraManager.zoomPresets.count, 1)
	}

	func test_applyZoomConfiguration_withFrontCamera() {
		let range: ClosedRange<CGFloat> = 1.0...1.0
		let lenses: [CameraManager.LensKind] = [.front]
		let presets: [CameraManager.ZoomPreset] = [
			CameraManager.ZoomPreset(
				lens: .front,
				zoomFactor: 1.0,
				focalLength: 24,
				style: .primary
			)
		]

		cameraManager.applyZoomConfiguration(
			range: range,
			lenses: lenses,
			presets: presets,
			targetFactor: 1.0,
			isContinuous: false
		)

		XCTAssertEqual(cameraManager.availableLenses.count, 1)
		XCTAssertTrue(cameraManager.availableLenses.contains(.front))
		XCTAssertEqual(cameraManager.zoomRange.upperBound, 1.0)
	}

	func test_applyZoomConfiguration_withMultiplePresets() {
		let range: ClosedRange<CGFloat> = 0.5...5.0
		let lenses: [CameraManager.LensKind] = [.ultraWide, .wide, .telephoto]
		let presets: [CameraManager.ZoomPreset] = [
			CameraManager.ZoomPreset(lens: .ultraWide, zoomFactor: 0.5, focalLength: 13, style: .secondary),
			CameraManager.ZoomPreset(lens: .wide, zoomFactor: 1.0, focalLength: 24, style: .primary),
			CameraManager.ZoomPreset(lens: .telephoto, zoomFactor: 3.0, focalLength: 77, style: .secondary)
		]

		cameraManager.applyZoomConfiguration(
			range: range,
			lenses: lenses,
			presets: presets,
			targetFactor: 1.0,
			isContinuous: false
		)

		XCTAssertEqual(cameraManager.zoomPresets.count, 3)
	}

	// MARK: - 变焦预设构建测试

	func test_buildZoomPresets_withWideOnly() {
		let range: ClosedRange<CGFloat> = 1.0...3.0
		let lenses: [CameraManager.LensKind] = [.wide]

		let presets = cameraManager.buildZoomPresets(range: range, lenses: lenses)

		XCTAssertFalse(presets.isEmpty)
		XCTAssertTrue(presets.contains(where: { $0.lens == .wide }))
	}

	func test_buildZoomPresets_withAllLenses() {
		let range: ClosedRange<CGFloat> = 0.5...5.0
		let lenses: [CameraManager.LensKind] = [.ultraWide, .wide, .telephoto]

		let presets = cameraManager.buildZoomPresets(range: range, lenses: lenses)

		XCTAssertTrue(presets.contains(where: { $0.lens == .ultraWide }))
		XCTAssertTrue(presets.contains(where: { $0.lens == .wide }))
	}

	func test_buildZoomPresets_withFrontCamera() {
		let range: ClosedRange<CGFloat> = 1.0...1.0
		let lenses: [CameraManager.LensKind] = [.front]

		let presets = cameraManager.buildZoomPresets(range: range, lenses: lenses)

		XCTAssertEqual(presets.count, 1)
		XCTAssertEqual(presets.first?.lens, .front)
		XCTAssertEqual(presets.first?.zoomFactor, 1.0)
	}

	func test_buildZoomPresets_emptyLensesReturnsEmpty() {
		let range: ClosedRange<CGFloat> = 1.0...3.0
		let lenses: [CameraManager.LensKind] = []

		let presets = cameraManager.buildZoomPresets(range: range, lenses: lenses)

		XCTAssertTrue(presets.isEmpty)
	}

	func test_buildZoomPresets_sortedByZoomFactor() {
		let range: ClosedRange<CGFloat> = 0.5...5.0
		let lenses: [CameraManager.LensKind] = [.ultraWide, .wide, .telephoto]

		let presets = cameraManager.buildZoomPresets(range: range, lenses: lenses)

		// 验证按倍率排序
		for i in 1..<presets.count {
			XCTAssertLessThanOrEqual(presets[i-1].zoomFactor, presets[i].zoomFactor)
		}
	}

	// MARK: - 焦距估算测试

	func test_estimateFocalLength_wideLens() {
		let focal = cameraManager.estimateFocalLength(for: 1.0, lens: .wide)
		XCTAssertEqual(focal, 24)
	}

	func test_estimateFocalLength_ultraWideLens() {
		let focal = cameraManager.estimateFocalLength(for: 0.5, lens: .ultraWide)
		XCTAssertEqual(focal, 12)
	}

	func test_estimateFocalLength_telephotoLens() {
		let focal = cameraManager.estimateFocalLength(for: 3.0, lens: .telephoto)
		// 3.0 * 24 = 72, 与光学 77 差距 < 8, 应返回 77
		XCTAssertEqual(focal, 77)
	}

	func test_estimateFocalLength_frontLens() {
		let focal = cameraManager.estimateFocalLength(for: 1.0, lens: .front)
		XCTAssertEqual(focal, 24)
	}

	// MARK: - 镜头排序测试

	func test_lensOrder_correctRanking() {
		let result = cameraManager.lensOrder(lhs: .ultraWide, rhs: .wide)
		XCTAssertTrue(result)

		let result2 = cameraManager.lensOrder(lhs: .wide, rhs: .telephoto)
		XCTAssertTrue(result2)

		let result3 = cameraManager.lensOrder(lhs: .telephoto, rhs: .ultraWide)
		XCTAssertFalse(result3)
	}

	// MARK: - 变焦 clamp 测试

	func test_clampZoom_withinRange() {
		// 默认 zoomRange 是 1.0...1.0，但 clampZoom 上限是 10.0
		let clamped = cameraManager.clampZoom(1.5)
		XCTAssertEqual(clamped, 1.5)
	}

	func test_clampZoom_belowLowerBound() {
		let clamped = cameraManager.clampZoom(0.1)
		// 默认 lowerBound 是 1.0
		XCTAssertEqual(clamped, 1.0)
	}

	func test_clampZoom_aboveUpperBound() {
		let clamped = cameraManager.clampZoom(15.0)
		// 上限是 10.0
		XCTAssertEqual(clamped, 10.0)
	}

	// MARK: - 镜头类型推断测试

	func test_currentLensKind_backWide() {
		cameraManager.currentPosition = .back
		cameraManager.availableLenses = [.wide]
		let lens = cameraManager.currentLensKind(for: 1.0)
		XCTAssertEqual(lens, .wide)
	}

	func test_currentLensKind_frontCamera() {
		cameraManager.currentPosition = .front
		let lens = cameraManager.currentLensKind(for: 1.0)
		XCTAssertEqual(lens, .front)
	}

	func test_currentLensKind_ultraWideFactor() {
		cameraManager.currentPosition = .back
		cameraManager.availableLenses = [.ultraWide, .wide]
		let lens = cameraManager.currentLensKind(for: 0.5)
		XCTAssertEqual(lens, .ultraWide)
	}

	func test_currentLensKind_telephotoFactor() {
		cameraManager.currentPosition = .back
		cameraManager.availableLenses = [.wide, .telephoto]
		let lens = cameraManager.currentLensKind(for: 3.0)
		XCTAssertEqual(lens, .telephoto)
	}

	// MARK: - 变焦速率测试

	func test_optimalRampRate_lowZoom() {
		let rate = cameraManager.optimalRampRate(for: 0.5)
		XCTAssertEqual(rate, 5.0)
	}

	func test_optimalRampRate_mediumZoom() {
		let rate = cameraManager.optimalRampRate(for: 2.0)
		XCTAssertEqual(rate, 8.0)
	}

	func test_optimalRampRate_highZoom() {
		let rate = cameraManager.optimalRampRate(for: 5.0)
		XCTAssertEqual(rate, 10.0)
	}

	// MARK: - Photo Output 测试

	func test_initialization_photoOutputExists() {
		XCTAssertNotNil(cameraManager.photoOutput)
	}

	func test_initialization_videoOutputExists() {
		XCTAssertNotNil(cameraManager.videoOutput)
	}

	// MARK: - 队列测试

	func test_initialization_sessionQueueExists() {
		XCTAssertNotNil(cameraManager.sessionQueue)
	}

	func test_initialization_videoOutputQueueExists() {
		XCTAssertNotNil(cameraManager.videoOutputQueue)
	}

	// MARK: - lastPhotoSaved 测试

	func test_initialization_lastPhotoSavedIsFalse() {
		XCTAssertFalse(cameraManager.lastPhotoSaved)
	}

	// MARK: - shouldBeRunning 测试

	func test_initialization_shouldBeRunningIsFalse() {
		XCTAssertFalse(cameraManager.shouldBeRunning)
	}

	// MARK: - availableLenses 测试

	func test_initialization_availableLensesIsEmpty() {
		XCTAssertTrue(cameraManager.availableLenses.isEmpty)
	}

	// MARK: - backCameraCatalog 测试

	func test_initialization_backCameraCatalogIsEmpty() {
		XCTAssertTrue(cameraManager.backCameraCatalog.isEmpty)
	}

	// MARK: - 滤镜预览处理器测试

	func test_filterPreviewProcessorExists() {
		XCTAssertNotNil(cameraManager.filterPreviewProcessor)
	}

	func test_initialization_activeFilterPresetIsNil() {
		XCTAssertNil(cameraManager.activeFilterPreset)
	}

	func test_initialization_activeFilterIntensityIsDefault() {
		XCTAssertEqual(cameraManager.activeFilterIntensity, 1.0)
	}

	// MARK: - LensKind 测试

	func test_lensKind_allCases() {
		let all = CameraManager.LensKind.allCases
		XCTAssertEqual(all.count, 4)
		XCTAssertTrue(all.contains(.ultraWide))
		XCTAssertTrue(all.contains(.wide))
		XCTAssertTrue(all.contains(.telephoto))
		XCTAssertTrue(all.contains(.front))
	}

	func test_lensKind_approximateFocalLength() {
		XCTAssertEqual(CameraManager.LensKind.ultraWide.approximateFocalLength, 13)
		XCTAssertEqual(CameraManager.LensKind.wide.approximateFocalLength, 24)
		XCTAssertEqual(CameraManager.LensKind.telephoto.approximateFocalLength, 77)
		XCTAssertEqual(CameraManager.LensKind.front.approximateFocalLength, 24)
	}

	func test_lensKind_opticalZoomFactor() {
		XCTAssertEqual(CameraManager.LensKind.ultraWide.opticalZoomFactor, 0.5)
		XCTAssertEqual(CameraManager.LensKind.wide.opticalZoomFactor, 1.0)
		XCTAssertEqual(CameraManager.LensKind.telephoto.opticalZoomFactor, 3.0)
		XCTAssertEqual(CameraManager.LensKind.front.opticalZoomFactor, 1.0)
	}

	func test_lensKind_displayName() {
		XCTAssertEqual(CameraManager.LensKind.ultraWide.displayName, "0.5×")
		XCTAssertEqual(CameraManager.LensKind.wide.displayName, "1×")
		XCTAssertEqual(CameraManager.LensKind.telephoto.displayName, "3×")
		XCTAssertEqual(CameraManager.LensKind.front.displayName, "1×")
	}

	// MARK: - ZoomPreset 测试

	func test_zoomPreset_label() {
		let preset = CameraManager.ZoomPreset(
			lens: .wide, zoomFactor: 1.0, focalLength: 24, style: .primary
		)
		XCTAssertEqual(preset.label, "1×")
	}

	func test_zoomPreset_labelDecimal() {
		let preset = CameraManager.ZoomPreset(
			lens: .wide, zoomFactor: 2.5, focalLength: 60, style: .secondary
		)
		XCTAssertEqual(preset.label, "2.5×")
	}

	func test_zoomPreset_focalLengthLabel() {
		let preset = CameraManager.ZoomPreset(
			lens: .wide, zoomFactor: 1.0, focalLength: 24, style: .primary
		)
		XCTAssertEqual(preset.focalLengthLabel, "24mm")
	}

	func test_zoomPreset_hashable() {
		let preset1 = CameraManager.ZoomPreset(
			lens: .wide, zoomFactor: 1.0, focalLength: 24, style: .primary
		)
		let preset2 = CameraManager.ZoomPreset(
			lens: .wide, zoomFactor: 1.0, focalLength: 24, style: .primary
		)

		// 不同 UUID，所以 hash 不同
		XCTAssertNotEqual(preset1.id, preset2.id)
	}

	// MARK: - CameraError 测试

	func test_cameraError_cases() {
		let errors: [CameraManager.CameraError] = [
			.cameraUnavailable, .cannotAddInput, .cannotAddOutput,
			.photoDataMissing, .saveFailed, .notAuthorized
		]
		XCTAssertEqual(errors.count, 6)
	}

	// MARK: - objectWillChange 测试

	func test_objectWillChangeExists() {
		XCTAssertNotNil(cameraManager.objectWillChange)
	}

	func test_objectWillChange_emitsOnStateChange() {
		let expectation = XCTestExpectation(description: "objectWillChange emits")

		cameraManager.objectWillChange
			.sink {
				expectation.fulfill()
			}
			.store(in: &cancellables)

		cameraManager.objectWillChange.send()

		wait(for: [expectation], timeout: 2.0)
	}
}