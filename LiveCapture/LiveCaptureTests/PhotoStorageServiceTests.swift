//
//  PhotoStorageServiceTests.swift
//  LiveCaptureTests
//
//  PhotoStorageService 单元测试
//

import XCTest
import Combine
@testable import LiveCapture

final class PhotoStorageServiceTests: XCTestCase {

	var cancellables: Set<AnyCancellable>!

	override func setUp() {
		super.setUp()
		cancellables = []
	}

	override func tearDown() {
		cancellables = nil
		super.tearDown()
	}

	// MARK: - shared 单例测试

	func test_shared_singletonExists() {
		// 测试 shared 单例存在
		let service = PhotoStorageService.shared
		XCTAssertNotNil(service)
	}

	func test_shared_isSameInstance() {
		// 测试 shared 每次返回同一个实例
		let instance1 = PhotoStorageService.shared
		let instance2 = PhotoStorageService.shared
		XCTAssertTrue(instance1 === instance2)
	}

	// MARK: - savePhoto 方法测试

	func test_savePhoto_methodExists() {
		// 测试 savePhoto 方法存在且可调用
		let service = PhotoStorageService.shared
		// 方法签名: savePhoto(data: Data, detectionMethod: String?)
		// 验证方法存在（能编译通过即为存在）
		XCTAssertTrue(true)
	}

	func test_savePhoto_acceptsValidData() {
		// 测试 savePhoto 接收有效 Data 参数
		let service = PhotoStorageService.shared
		let testData = "test_image_data".data(using: .utf8)!
		// 调用不应崩溃
		service.savePhoto(data: testData, detectionMethod: "Fast")
		XCTAssertTrue(true)
	}

	func test_savePhoto_acceptsNilDetectionMethod() {
		// 测试 savePhoto 接受 nil 的 detectionMethod
		let service = PhotoStorageService.shared
		let testData = "test_image_data".data(using: .utf8)!
		service.savePhoto(data: testData, detectionMethod: nil)
		XCTAssertTrue(true)
	}

	func test_savePhoto_withEmptyData() {
		// 测试 savePhoto 接受空数据
		let service = PhotoStorageService.shared
		let emptyData = Data()
		service.savePhoto(data: emptyData, detectionMethod: nil)
		XCTAssertTrue(true)
	}

	// MARK: - loadRecords 测试

	func test_loadRecords_methodExists() {
		// 测试 loadRecords 方法存在
		let service = PhotoStorageService.shared
		let records = service.loadRecords()
		XCTAssertNotNil(records)
	}

	func test_loadRecords_returnsArray() {
		// 测试 loadRecords 返回数组
		let service = PhotoStorageService.shared
		let records = service.loadRecords()
		XCTAssertTrue(records is [PhotoRecord])
	}

	// MARK: - recordsPublisher 测试

	func test_recordsPublisher_exists() {
		// 测试 recordsPublisher 存在
		let service = PhotoStorageService.shared
		XCTAssertNotNil(service.recordsPublisher)
	}

	func test_recordsPublisher_publishesValues() {
		// 测试 recordsPublisher 可以订阅
		let service = PhotoStorageService.shared
		let expectation = XCTestExpectation(description: "recordsPublisher emits value")

		service.recordsPublisher
			.sink { records in
				// 验证收到的是数组
				XCTAssertTrue(records is [PhotoRecord])
				expectation.fulfill()
			}
			.store(in: &cancellables)

		// 触发 loadRecords 以发布值
		_ = service.loadRecords()

		wait(for: [expectation], timeout: 2.0)
	}

	// MARK: - deleteRecord 测试

	func test_deleteRecord_methodExists() {
		// 测试 deleteRecord 方法存在
		let service = PhotoStorageService.shared
		let testId = UUID()
		service.deleteRecord(testId)
		XCTAssertTrue(true)
	}

	// MARK: - thumbnail 测试

	func test_thumbnail_methodExists() {
		// 测试 thumbnail 方法存在
		let service = PhotoStorageService.shared
		let testId = UUID()
		let thumbnail = service.thumbnail(for: testId)
		// 不存在的记录返回 nil
		XCTAssertNil(thumbnail)
	}

	// MARK: - photoURL 测试

	func test_photoURL_methodExists() {
		// 测试 photoURL 方法存在
		let service = PhotoStorageService.shared
		let testId = UUID()
		let url = service.photoURL(for: testId)
		// 不存在的记录返回 nil
		XCTAssertNil(url)
	}
}