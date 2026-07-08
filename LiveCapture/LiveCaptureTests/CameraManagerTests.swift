//
//  CameraManagerTests.swift
//  LiveCaptureTests
//
//  CameraManager 单元测试
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
		// 测试初始化后 session 不为空
		XCTAssertNotNil(cameraManager.session)
		XCTAssertTrue(cameraManager.session is AVCaptureSession)
	}

	func test_initialization_sessionPresetIsPhoto() {
		// 测试 session preset 为 photo
		XCTAssertEqual(cameraManager.session.sessionPreset, .photo)
	}

	func test_initialization_sessionIsNotRunning() {
		// 测试初始化后 session 未运行
		XCTAssertFalse(cameraManager.isSessionRunning)
	}

	// MARK: - 摄像头位置测试

	func test_initialization_defaultPositionIsBack() {
		// 测试默认摄像头位置为后置
		XCTAssertEqual(cameraManager.currentPosition, .back)
	}

	func test_currentPosition_isBack() {
		XCTAssertEqual(cameraManager.currentPosition, AVCaptureDevice.Position.back)
	}

	// MARK: - ZoomState 测试

	func test_initialization_zoomStateHasDefaultValues() {
		// 测试 zoomState 初始值
		let zoomState = cameraManager.zoomState
		XCTAssertEqual(zoomState.currentFactor, 1.0)
		XCTAssertEqual(zoomState.displayedFactor, 1.0)
		XCTAssertFalse(zoomState.isContinuous)
	}

	func test_initialization_zoomStateLensIsWide() {
		// 测试 zoomState 初始镜头为广角
		XCTAssertEqual(cameraManager.zoomState.activeLens, .wide)
	}

	func test_initialization_zoomRangeIsDefault() {
		// 测试 zoomRange 初始值
		XCTAssertEqual(cameraManager.zoomRange.lowerBound, 1.0)
		XCTAssertEqual(cameraManager.zoomRange.upperBound, 1.0)
	}

	func test_initialization_zoomPresetsIsEmpty() {
		// 测试 zoomPresets 初始为空
		XCTAssertTrue(cameraManager.zoomPresets.isEmpty)
	}

	// MARK: - Photo Output 测试

	func test_initialization_photoOutputExists() {
		// 测试 photoOutput 存在
		XCTAssertNotNil(cameraManager.photoOutput)
	}

	func test_initialization_videoOutputExists() {
		// 测试 videoOutput 存在
		XCTAssertNotNil(cameraManager.videoOutput)
	}

	// MARK: - 队列测试

	func test_initialization_sessionQueueExists() {
		// 测试 sessionQueue 存在
		XCTAssertNotNil(cameraManager.sessionQueue)
	}

	func test_initialization_videoOutputQueueExists() {
		// 测试 videoOutputQueue 存在
		XCTAssertNotNil(cameraManager.videoOutputQueue)
	}

	// MARK: - lastPhotoSaved 测试

	func test_initialization_lastPhotoSavedIsFalse() {
		// 测试初始状态 lastPhotoSaved 为 false
		XCTAssertFalse(cameraManager.lastPhotoSaved)
	}

	// MARK: - shouldBeRunning 测试

	func test_initialization_shouldBeRunningIsFalse() {
		// 测试初始 shouldBeRunning 为 false
		XCTAssertFalse(cameraManager.shouldBeRunning)
	}

	// MARK: - availableLenses 测试

	func test_initialization_availableLensesIsEmpty() {
		// 测试初始 availableLenses 为空
		XCTAssertTrue(cameraManager.availableLenses.isEmpty)
	}

	// MARK: - backCameraCatalog 测试

	func test_initialization_backCameraCatalogIsEmpty() {
		// 测试初始 backCameraCatalog 为空
		XCTAssertTrue(cameraManager.backCameraCatalog.isEmpty)
	}

	// MARK: - 变焦配置更新测试

	func test_updateZoomState_updatesCorrectly() {
		// 测试 updateZoomState 正确更新
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

	// MARK: - applyZoomConfiguration 测试

	func test_applyZoomConfiguration_updatesZoomProperties() {
		// 测试 applyZoomConfiguration 更新所有变焦相关属性
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
}