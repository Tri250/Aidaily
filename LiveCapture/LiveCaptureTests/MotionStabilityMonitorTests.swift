//
//  MotionStabilityMonitorTests.swift
//  LiveCaptureTests
//
//  MotionStabilityMonitor 单元测试
//

import XCTest
import Combine
import CoreMotion
@testable import LiveCapture

final class MotionStabilityMonitorTests: XCTestCase {

	var monitor: MotionStabilityMonitor!
	var cancellables: Set<AnyCancellable>!

	override func setUp() {
		super.setUp()
		monitor = MotionStabilityMonitor()
		cancellables = []
	}

	override func tearDown() {
		monitor = nil
		cancellables = nil
		super.tearDown()
	}

	// MARK: - 初始化测试

	func test_initialization_isStableIsFalse() {
		// 测试初始化后 isStable 为 false
		XCTAssertFalse(monitor.isStable)
	}

	func test_initialization_debugInfoIsInitializing() {
		// 测试初始化后 debugInfo 为"初始化中..."
		XCTAssertEqual(monitor.debugInfo, "初始化中...")
	}

	func test_initialization_deviceMotionIsNil() {
		// 测试初始化后 deviceMotion 为 nil
		XCTAssertNil(monitor.deviceMotion)
	}

	func test_initialization_largeMotionDetectedIsFalse() {
		// 测试初始化后 largeMotionDetected 为 false
		XCTAssertFalse(monitor.largeMotionDetected)
	}

	// MARK: - 可配置参数测试

	func test_windowSeconds_defaultValue() {
		// 测试默认时间窗口为 0.8 秒
		XCTAssertEqual(monitor.windowSeconds, 0.8)
	}

	func test_accelerationStdThreshold_defaultValue() {
		// 测试默认加速度标准差阈值
		XCTAssertEqual(monitor.accelerationStdThreshold, 0.12)
	}

	func test_gyroStdThreshold_defaultValue() {
		// 测试默认陀螺仪标准差阈值
		XCTAssertEqual(monitor.gyroStdThreshold, 0.08)
	}

	func test_largeMotionAccThreshold_defaultValue() {
		// 测试默认大幅度运动加速度阈值
		XCTAssertEqual(monitor.largeMotionAccThreshold, 1.5)
	}

	func test_largeMotionGyroThreshold_defaultValue() {
		// 测试默认大幅度运动陀螺仪阈值
		XCTAssertEqual(monitor.largeMotionGyroThreshold, 2.0)
	}

	// MARK: - start/stop 测试

	func test_start_doesNotCrash() {
		// 测试 start 方法可以被调用且不崩溃
		monitor.start()
		// 在 test 环境中，传感器可能不可用，start 会安全返回
		// 不抛异常即为通过
		XCTAssertTrue(true)
	}

	func test_stop_doesNotCrash() {
		// 测试 stop 方法可以被调用且不崩溃
		monitor.start()
		monitor.stop()
		XCTAssertTrue(true)
	}

	func test_stop_whenNotStarted_doesNotCrash() {
		// 测试在未启动时调用 stop 不崩溃
		monitor.stop()
		XCTAssertTrue(true)
	}

	func test_stop_resetsIsStableToFalse() {
		// 测试 stop 后 isStable 重置为 false
		monitor.start()
		monitor.stop()
		XCTAssertFalse(monitor.isStable)
	}

	func test_stop_resetsDebugInfo() {
		// 测试 stop 后 debugInfo 更新为"已停止"
		monitor.start()
		monitor.stop()
		XCTAssertEqual(monitor.debugInfo, "已停止")
	}

	func test_stop_resetsDeviceMotion() {
		// 测试 stop 后 deviceMotion 重置为 nil
		monitor.start()
		monitor.stop()
		XCTAssertNil(monitor.deviceMotion)
	}

	// MARK: - lockReferenceAttitude / resetReferenceAttitude 测试

	func test_lockReferenceAttitude_doesNotCrash() {
		// 测试 lockReferenceAttitude 不崩溃
		monitor.start()
		monitor.lockReferenceAttitude()
		monitor.stop()
		XCTAssertTrue(true)
	}

	func test_resetReferenceAttitude_doesNotCrash() {
		// 测试 resetReferenceAttitude 不崩溃
		monitor.start()
		monitor.resetReferenceAttitude()
		monitor.stop()
		XCTAssertTrue(true)
	}

	func test_lockAndResetReferenceAttitude_doesNotCrash() {
		// 测试连续锁定和重置参考姿态不崩溃
		monitor.start()
		monitor.lockReferenceAttitude()
		monitor.resetReferenceAttitude()
		monitor.lockReferenceAttitude()
		monitor.stop()
		XCTAssertTrue(true)
	}

	// MARK: - 参数可配置测试

	func test_windowSeconds_canBeChanged() {
		// 测试时间窗口可以被修改
		monitor.windowSeconds = 1.0
		XCTAssertEqual(monitor.windowSeconds, 1.0)
	}

	func test_accelerationThreshold_canBeChanged() {
		// 测试加速度阈值可以被修改
		monitor.accelerationStdThreshold = 0.2
		XCTAssertEqual(monitor.accelerationStdThreshold, 0.2)
	}

	func test_gyroThreshold_canBeChanged() {
		// 测试陀螺仪阈值可以被修改
		monitor.gyroStdThreshold = 0.15
		XCTAssertEqual(monitor.gyroStdThreshold, 0.15)
	}

	func test_largeMotionAccThreshold_canBeChanged() {
		// 测试大幅度运动加速度阈值可以被修改
		monitor.largeMotionAccThreshold = 2.0
		XCTAssertEqual(monitor.largeMotionAccThreshold, 2.0)
	}

	func test_largeMotionGyroThreshold_canBeChanged() {
		// 测试大幅度运动陀螺仪阈值可以被修改
		monitor.largeMotionGyroThreshold = 3.0
		XCTAssertEqual(monitor.largeMotionGyroThreshold, 3.0)
	}
}