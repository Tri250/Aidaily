//
//  CoreServicesTests.swift
//  LiveCaptureTests
//
//  核心服务单元测试：存储、滤镜、搜索、场景引擎
//

import XCTest
import CoreImage
import AVFoundation
@testable import LiveCapture

// MARK: - PhotoStorageService Tests

final class PhotoStorageServiceTests: XCTestCase {
    var storage: PhotoStorageService!

    override func setUp() {
        super.setUp()
        storage = PhotoStorageService.shared
        _ = storage.loadRecords()
    }

    func test_loadRecords_returnsArray() {
        let records = storage.loadRecords()
        XCTAssertNotNil(records)
        XCTAssertTrue(type(of: records) == [PhotoRecord].self)
    }

    func test_photoURL_forValidId_returnsURL() {
        let record = PhotoRecord(id: UUID(), creationDate: Date())
        let url = storage.photoURL(for: record.id)
        XCTAssertNotNil(UUID(uuidString: url?.lastPathComponent.replacingOccurrences(of: ".jpg", with: "") ?? ""))
    }

    func test_thumbnail_forId_returnsNilWhenNoImage() {
        let record = PhotoRecord(id: UUID(), creationDate: Date())
        let thumbnail = storage.thumbnail(for: record.id)
        // 照片不存在时应返回 nil
        XCTAssertNil(thumbnail)
    }

    func test_recordsPublisher_emitsOnLoad() {
        let expectation = XCTestExpectation(description: "recordsPublisher emits")
        let cancellable = storage.recordsPublisher
            .sink { records in
                XCTAssertNotNil(records)
                expectation.fulfill()
            }
        _ = storage.loadRecords()
        wait(for: [expectation], timeout: 2.0)
        cancellable.cancel()
    }
}

// MARK: - FilterPresetManager Tests

final class FilterPresetManagerTests: XCTestCase {
    var manager: FilterPresetManager!

    override func setUp() {
        super.setUp()
        manager = FilterPresetManager()
    }

    func test_builtInPresets_has12Presets() {
        let presets = manager.presets
        XCTAssertEqual(presets.count, 12)
    }

    func test_presets_allHaveUniqueNames() {
        let names = manager.presets.map { $0.name }
        XCTAssertEqual(names.count, Set(names).count)
    }

    func test_presets_allHaveValidCategories() {
        for preset in manager.presets {
            XCTAssertNotNil(FilterCategory.allCases.first { $0 == preset.category })
        }
    }

    func test_selectPreset_updatesSelectedPreset() {
        let preset = manager.presets[0]
        manager.selectPreset(preset)
        XCTAssertEqual(manager.selectedPreset?.id, preset.id)
    }

    func test_deselectAll_clearsSelection() {
        manager.selectPreset(manager.presets[0])
        manager.deselectAll()
        XCTAssertNil(manager.selectedPreset)
    }

    func test_filterByCategory_returnsCorrectCategory() {
        let portraitPresets = manager.filterByCategory(.portrait)
        for preset in portraitPresets {
            XCTAssertEqual(preset.category, .portrait)
        }
    }

    func test_defaultIntensity_returns1() {
        XCTAssertEqual(manager.currentIntensity, 1.0)
    }
}

// MARK: - PhotoSearchEngine Tests

final class PhotoSearchEngineTests: XCTestCase {
    var engine: PhotoSearchEngine!
    var records: [PhotoRecord]!

    override func setUp() {
        super.setUp()
        engine = PhotoSearchEngine()

        // 创建测试数据
        let today = Date()
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: today)!
        let lastWeek = Calendar.current.date(byAdding: .day, value: -7, to: today)!

        records = [
            PhotoRecord(id: UUID(), creationDate: today, detectionMethod: "portrait", iso: 100, shutterSpeed: 1/120, aperture: 2.8),
            PhotoRecord(id: UUID(), creationDate: yesterday, detectionMethod: "landscape", iso: 200, shutterSpeed: 1/500, aperture: 8.0),
            PhotoRecord(id: UUID(), creationDate: lastWeek, detectionMethod: "night", iso: 1600, shutterSpeed: 0.5, aperture: 1.8),
        ]
    }

    func test_search_emptyQuery_returnsAll() {
        let results = engine.search("", in: records)
        XCTAssertEqual(results.count, 3)
    }

    func test_search_byDate_today() {
        let results = engine.search("今天", in: records)
        XCTAssertEqual(results.count, 1)
    }

    func test_search_byDate_yesterday() {
        let results = engine.search("昨天", in: records)
        XCTAssertEqual(results.count, 1)
    }

    func test_search_byScene_portrait() {
        let results = engine.search("人像", in: records)
        XCTAssertGreaterThanOrEqual(results.count, 1)
    }

    func test_search_byScene_landscape() {
        let results = engine.search("风景", in: records)
        XCTAssertGreaterThanOrEqual(results.count, 1)
    }

    func test_search_byISO_highISO() {
        let results = engine.search("夜市", in: records)
        XCTAssertGreaterThanOrEqual(results.count, 0) // 至少不应崩溃
    }

    func test_search_noMatch_returnsEmpty() {
        let results = engine.search("不存在的关键词xyz", in: records)
        XCTAssertEqual(results.count, 0)
    }
}

// MARK: - PhotoRecord Tests

final class PhotoRecordTests: XCTestCase {
    func test_init_setsDefaultValues() {
        let record = PhotoRecord()
        XCTAssertNotNil(record.id)
        XCTAssertNotNil(record.creationDate)
        XCTAssertNil(record.localIdentifier)
        XCTAssertNil(record.detectionMethod)
    }

    func test_init_withCustomValues() {
        let id = UUID()
        let date = Date(timeIntervalSince1970: 0)
        let record = PhotoRecord(
            id: id,
            creationDate: date,
            localIdentifier: "test_id",
            detectionMethod: "vision",
            iso: 400,
            shutterSpeed: 1/60,
            aperture: 4.0,
            imageWidth: 4032,
            imageHeight: 3024
        )
        XCTAssertEqual(record.id, id)
        XCTAssertEqual(record.creationDate, date)
        XCTAssertEqual(record.localIdentifier, "test_id")
        XCTAssertEqual(record.iso, 400)
        XCTAssertEqual(record.aperture, 4.0)
        XCTAssertEqual(record.imageWidth, 4032)
    }

    func test_photoFilename_generatesCorrectName() {
        let id = UUID()
        let filename = PhotoRecord.photoFilename(for: id)
        XCTAssertTrue(filename.hasSuffix(".jpg"))
        XCTAssertTrue(filename.contains(id.uuidString))
    }

    func test_thumbnailFilename_generatesCorrectName() {
        let id = UUID()
        let filename = PhotoRecord.thumbnailFilename(for: id)
        XCTAssertTrue(filename.hasSuffix("_thumb.jpg"))
        XCTAssertTrue(filename.contains(id.uuidString))
    }
}

// MARK: - FilterParameters Tests

final class FilterParametersTests: XCTestCase {
    func test_neutral_hasDefaultValues() {
        let neutral = FilterParameters.neutral
        XCTAssertEqual(neutral.temperature, 0)
        XCTAssertEqual(neutral.exposure, 0)
        XCTAssertEqual(neutral.brightness, 0)
        XCTAssertEqual(neutral.contrast, 1.0)
        XCTAssertEqual(neutral.saturation, 1.0)
        XCTAssertFalse(neutral.isMonochrome)
    }

    func test_customFilter_encodesCorrectly() {
        var params = FilterParameters()
        params.temperature = 800
        params.exposure = 0.15
        params.contrast = 0.92
        params.saturation = 1.05

        let encoder = JSONEncoder()
        let data = try? encoder.encode(params)
        XCTAssertNotNil(data)

        let decoder = JSONDecoder()
        let decoded = try? decoder.decode(FilterParameters.self, from: data!)
        XCTAssertEqual(decoded?.temperature, 800)
        XCTAssertEqual(decoded?.contrast, 0.92)
    }
}

// MARK: - DetectionMode Tests

final class DetectionModeTests: XCTestCase {
    func test_allCases_areThree() {
        XCTAssertEqual(DetectionMode.allCases.count, 3)
    }

    func test_displayName_isNotEmpty() {
        for mode in DetectionMode.allCases {
            XCTAssertFalse(mode.displayName.isEmpty)
        }
    }

    func test_description_isNotEmpty() {
        for mode in DetectionMode.allCases {
            XCTAssertFalse(mode.description.isEmpty)
        }
    }
}

// MARK: - FilterCategory Tests

final class FilterCategoryTests: XCTestCase {
    func test_allCases_sevenCategories() {
        XCTAssertEqual(FilterCategory.allCases.count, 7)
    }

    func test_eachCategory_hasSymbolName() {
        for category in FilterCategory.allCases {
            XCTAssertFalse(category.symbolName.isEmpty)
        }
    }
}

// MARK: - LightAnalysis Tests

final class LightAnalysisTests: XCTestCase {
    func test_neutral_hasDefaultValues() {
        let neutral = LightAnalysis.neutral
        XCTAssertEqual(neutral.estimatedTemperature, 6500)
        XCTAssertEqual(neutral.estimatedBrightness, 0.5)
        XCTAssertFalse(neutral.isWarmLight)
        XCTAssertFalse(neutral.isCoolLight)
    }

    func test_from_warmLight() {
        let analysis = LightAnalysis.from(estimatedTemperature: 4000, estimatedBrightness: 0.5)
        XCTAssertTrue(analysis.isWarmLight)
        XCTAssertFalse(analysis.isCoolLight)
    }

    func test_from_coolLight() {
        let analysis = LightAnalysis.from(estimatedTemperature: 8000, estimatedBrightness: 0.5)
        XCTAssertFalse(analysis.isWarmLight)
        XCTAssertTrue(analysis.isCoolLight)
    }

    func test_from_lowLight() {
        let analysis = LightAnalysis.from(estimatedTemperature: 6500, estimatedBrightness: 0.1)
        XCTAssertTrue(analysis.isLowLight)
    }

    func test_from_highLight() {
        let analysis = LightAnalysis.from(estimatedTemperature: 6500, estimatedBrightness: 0.9)
        XCTAssertTrue(analysis.isHighLight)
    }
}

// MARK: - SceneType Tests

final class SceneTypeTests: XCTestCase {
    func test_allCases_count() {
        XCTAssertEqual(SceneType.allCases.count, 12)
    }

    func test_displayName_matchesRawValue() {
        for scene in SceneType.allCases {
            XCTAssertEqual(scene.displayName, scene.rawValue)
        }
    }
}

// MARK: - SlowMotionSpeed Tests

final class SlowMotionSpeedTests: XCTestCase {
    func test_speed4x_recordFrameRate() {
        XCTAssertEqual(SlowMotionSpeed.speed4x.recordFrameRate, 120)
    }

    func test_speed8x_recordFrameRate() {
        XCTAssertEqual(SlowMotionSpeed.speed8x.recordFrameRate, 240)
    }

    func test_playbackFrameRate() {
        XCTAssertEqual(SlowMotionSpeed.speed4x.playbackFrameRate, 30)
        XCTAssertEqual(SlowMotionSpeed.speed8x.playbackFrameRate, 30)
    }

    func test_slowdownFactor() {
        XCTAssertEqual(SlowMotionSpeed.speed4x.slowdownFactor, 4.0)
        XCTAssertEqual(SlowMotionSpeed.speed8x.slowdownFactor, 8.0)
    }
}

// MARK: - VideoQuality Tests

final class VideoQualityTests: XCTestCase {
    func test_allCases_notEmpty() {
        XCTAssertFalse(VideoQuality.allCases.isEmpty)
    }

    func test_eachQuality_hasDimensions() {
        for quality in VideoQuality.allCases {
            XCTAssertGreaterThan(quality.dimensions.width, 0)
            XCTAssertGreaterThan(quality.dimensions.height, 0)
        }
    }

    func test_eachQuality_hasBitRate() {
        for quality in VideoQuality.allCases {
            XCTAssertGreaterThan(quality.bitRate, 0)
        }
    }
}

// MARK: - BeautyParams Tests

final class BeautyParamsTests: XCTestCase {
    func test_isOff_whenAllZero() {
        let params = BeautyParams()
        XCTAssertTrue(params.isOff)
    }

    func test_isOff_whenSkinSmoothingActive() {
        var params = BeautyParams()
        params.skinSmoothing = 0.5
        XCTAssertFalse(params.isOff)
    }

    func test_isOff_whenEyeBrighteningActive() {
        var params = BeautyParams()
        params.eyeBrightening = 0.5
        XCTAssertFalse(params.isOff)
    }
}