//
//  CaptureViewModelTests.swift
//  LiveCaptureTests
//
//  CaptureViewModel 单元测试
//

import XCTest
import Combine
import AVFoundation
import CoreMotion
@testable import LiveCapture

final class CaptureViewModelTests: XCTestCase {

	var viewModel: CaptureViewModel!
	var cancellables: Set<AnyCancellable>!

	override func setUp() {
		super.setUp()
		viewModel = CaptureViewModel(detectionMode: .fast)
		cancellables = []
	}

	override func tearDown() {
		viewModel = nil
		cancellables = nil
		super.tearDown()
	}

	// MARK: - 初始化测试

	func test_initialization_setsCorrectDefaultState() {
		// 测试初始化后所有 Published 状态为预期默认值
		XCTAssertEqual(viewModel.pipelineStage, .idle)
		XCTAssertFalse(viewModel.isAligned)
		XCTAssertNil(viewModel.cropRectInView)
		XCTAssertNil(viewModel.initialCropRectInView)
		XCTAssertEqual(viewModel.compositionRectInView, .zero)
		XCTAssertFalse(viewModel.detectionReady)
		XCTAssertFalse(viewModel.motionIsStable)
		XCTAssertNil(viewModel.distanceToCenter)
		XCTAssertTrue(viewModel.isAutoCaptureEnabled)
		XCTAssertEqual(viewModel.captureDelay, 1.0)
		XCTAssertFalse(viewModel.isSwitchingCamera)
		XCTAssertFalse(viewModel.isCompositionPipelineEnabled)
	}

	func test_initialization_setsCameraDependency() {
		// 测试 camera 依赖不为空
		XCTAssertNotNil(viewModel.camera)
	}

	func test_initialization_setsSessionCorrectly() {
		// 测试 AVCaptureSession 存在
		XCTAssertNotNil(viewModel.session)
		XCTAssertTrue(viewModel.session is AVCaptureSession)
	}

	func test_initialization_defaultCameraPositionIsBack() {
		// 测试默认摄像头位置为后置
		XCTAssertFalse(viewModel.isFrontCamera)
	}

	// MARK: - toggleCameraPosition 测试

	func test_toggleCameraPosition_setsSwitchingFlag() {
		// 测试切换摄像头时设置切换标志
		viewModel.isSwitchingCamera = false
		viewModel.toggleCameraPosition()
		XCTAssertTrue(viewModel.isSwitchingCamera)
	}

	func test_toggleCameraPosition_resetsDetectionState() {
		// 测试切换摄像头时重置检测状态
		viewModel.toggleCameraPosition()
		XCTAssertFalse(viewModel.detectionReady)
		XCTAssertFalse(viewModel.isAligned)
		XCTAssertNil(viewModel.cropRectInView)
		XCTAssertNil(viewModel.initialCropRectInView)
	}

	// MARK: - toggleAutoCapture 测试

	func test_toggleAutoCapture_togglesState() {
		// 测试切换自动拍照开关
		let initialValue = viewModel.isAutoCaptureEnabled
		viewModel.toggleAutoCapture()
		XCTAssertNotEqual(viewModel.isAutoCaptureEnabled, initialValue)
	}

	func test_toggleAutoCapture_twiceRestoresOriginalState() {
		// 测试连按两次恢复原值
		let initialValue = viewModel.isAutoCaptureEnabled
		viewModel.toggleAutoCapture()
		viewModel.toggleAutoCapture()
		XCTAssertEqual(viewModel.isAutoCaptureEnabled, initialValue)
	}

	// MARK: - toggleCompositionPipeline 测试

	func test_toggleCompositionPipeline_togglesState() {
		// 测试切换构图流水线
		let initialValue = viewModel.isCompositionPipelineEnabled
		viewModel.toggleCompositionPipeline()
		XCTAssertNotEqual(viewModel.isCompositionPipelineEnabled, initialValue)
	}

	func test_toggleCompositionPipeline_enableUpdatesGuidanceText() {
		// 开启流水线后，引导文字应更新
		viewModel.isCompositionPipelineEnabled = false
		viewModel.toggleCompositionPipeline()
		XCTAssertTrue(viewModel.isCompositionPipelineEnabled)
		XCTAssertEqual(viewModel.userGuidanceText, "构图流水线已开启")
	}

	func test_toggleCompositionPipeline_disableResetsDetectionState() {
		// 先开启再关闭流水线，应重置检测状态
		viewModel.toggleCompositionPipeline() // 开启
		viewModel.toggleCompositionPipeline() // 关闭
		XCTAssertFalse(viewModel.isCompositionPipelineEnabled)
		XCTAssertFalse(viewModel.detectionReady)
		XCTAssertFalse(viewModel.isAligned)
	}

	func test_toggleCompositionPipeline_disableUpdatesGuidanceText() {
		// 关闭流水线后，引导文字应更新
		viewModel.isCompositionPipelineEnabled = true
		viewModel.toggleCompositionPipeline()
		XCTAssertFalse(viewModel.isCompositionPipelineEnabled)
		XCTAssertEqual(viewModel.userGuidanceText, "点击魔术棒开启智能构图")
	}

	// MARK: - resetDetectionState 测试

	func test_resetDetectionState_clearsAllState() {
		// 测试重置检测状态后所有检测相关状态被清除
		viewModel.resetDetectionState()
		XCTAssertFalse(viewModel.detectionReady)
		XCTAssertFalse(viewModel.isAligned)
		XCTAssertNil(viewModel.cropRectInView)
		XCTAssertNil(viewModel.initialCropRectInView)
	}

	func test_resetDetectionState_whenPipelineEnabled_updatesGuidanceText() {
		// 测试流水线开启时重置，引导文字应更新
		viewModel.isCompositionPipelineEnabled = true
		viewModel.resetDetectionState()
		XCTAssertEqual(viewModel.userGuidanceText, "构图流水线已开启")
	}

	func test_resetDetectionState_whenPipelineDisabled_updatesGuidanceText() {
		// 测试流水线关闭时重置，引导文字应更新
		viewModel.isCompositionPipelineEnabled = false
		viewModel.resetDetectionState()
		XCTAssertEqual(viewModel.userGuidanceText, "点击魔术棒开启智能构图")
	}

	// MARK: - setCaptureDelay 测试

	func test_setCaptureDelay_updatesValue() {
		// 测试设置拍照延迟
		viewModel.setCaptureDelay(2.5)
		XCTAssertEqual(viewModel.captureDelay, 2.5)
	}

	func test_setCaptureDelay_zeroValue() {
		// 测试设置零延迟
		viewModel.setCaptureDelay(0.0)
		XCTAssertEqual(viewModel.captureDelay, 0.0)
	}

	// MARK: - PipelineStage 测试

	func test_pipelineStage_idle_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.idle.progress, 0.05)
	}

	func test_pipelineStage_startingCamera_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.startingCamera.progress, 0.15)
	}

	func test_pipelineStage_waitingForStability_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.waitingForStability.progress, 0.3)
	}

	func test_pipelineStage_detectingRegion_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.detectingRegion.progress, 0.55)
	}

	func test_pipelineStage_templateReady_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.templateReady.progress, 0.7)
	}

	func test_pipelineStage_readyToCapture_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.readyToCapture.progress, 0.92)
	}

	func test_pipelineStage_capturingPhoto_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.capturingPhoto.progress, 0.95)
	}

	func test_pipelineStage_savingPhoto_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.savingPhoto.progress, 1.0)
	}

	func test_pipelineStage_error_hasCorrectProgress() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.error.progress, 0.2)
	}

	func test_pipelineStage_idle_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.idle.guidanceText, "")
	}

	func test_pipelineStage_startingCamera_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.startingCamera.guidanceText, "正在启动相机")
	}

	func test_pipelineStage_waitingForStability_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.waitingForStability.guidanceText, "请保持稳定")
	}

	func test_pipelineStage_detectingRegion_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.detectingRegion.guidanceText, "正在识别最佳构图...")
	}

	func test_pipelineStage_templateReady_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.templateReady.guidanceText, "请将圆点移动到画面中心")
	}

	func test_pipelineStage_readyToCapture_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.readyToCapture.guidanceText, "即将拍照，请保持稳定")
	}

	func test_pipelineStage_capturingPhoto_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.capturingPhoto.guidanceText, "正在拍照...")
	}

	func test_pipelineStage_savingPhoto_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.savingPhoto.guidanceText, "照片已保存")
	}

	func test_pipelineStage_error_hasCorrectGuidanceText() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.error.guidanceText, "发生错误，请重试")
	}

	// MARK: - PipelineStage Equatable 测试

	func test_pipelineStage_equatable() {
		XCTAssertEqual(CaptureViewModel.PipelineStage.idle, .idle)
		XCTAssertNotEqual(CaptureViewModel.PipelineStage.idle, .error)
		XCTAssertEqual(CaptureViewModel.PipelineStage.detectingRegion, .detectingRegion)
	}

	// MARK: - 初始 detectionMode 测试

	func test_initialization_withFastMode() {
		let vm = CaptureViewModel(detectionMode: .fast)
		XCTAssertNotNil(vm)
		XCTAssertEqual(vm.pipelineStage, .idle)
	}

	func test_initialization_withProMode() {
		let vm = CaptureViewModel(detectionMode: .pro)
		XCTAssertNotNil(vm)
		XCTAssertEqual(vm.pipelineStage, .idle)
	}

	func test_initialization_withVisionMode() {
		let vm = CaptureViewModel(detectionMode: .vision)
		XCTAssertNotNil(vm)
		XCTAssertEqual(vm.pipelineStage, .idle)
	}
}