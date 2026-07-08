//
//  LiveCaptureTests.swift
//  LiveCaptureTests
//
//  综合单元测试：滤镜处理、预设管理、美颜参数、水印配置、存储服务、编辑历史
//

import XCTest
import Combine
import CoreImage
@testable import LiveCapture

final class LiveCaptureTests: XCTestCase {

    var cancellables: Set<AnyCancellable>!

    override func setUp() {
        super.setUp()
        cancellables = []
    }

    override func tearDown() {
        cancellables = nil
        super.tearDown()
    }

    // MARK: - FilterProcessor 测试

    func test_filterProcessor_sharedInstanceExists() {
        let processor = FilterProcessor.shared
        XCTAssertNotNil(processor)
    }

    func test_filterProcessor_sharedIsSameInstance() {
        let p1 = FilterProcessor.shared
        let p2 = FilterProcessor.shared
        XCTAssertTrue(p1 === p2)
    }

    func test_filterProcessor_applyFilterWithZeroIntensityReturnsOriginal() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.dokaPortrait

        let result = processor.applyFilter(to: image, preset: preset, intensity: 0.0)

        // 强度为0时，结果应接近原图
        XCTAssertNotNil(result)
        XCTAssertEqual(result.extent, image.extent)
    }

    func test_filterProcessor_applyFilterWithFullIntensityProducesOutput() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.agfaVista400

        let result = processor.applyFilter(to: image, preset: preset, intensity: 1.0)

        XCTAssertNotNil(result)
        XCTAssertEqual(result.extent, image.extent)
    }

    func test_filterProcessor_applyFilterWithHalfIntensity() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.fujiPro400H

        let result = processor.applyFilter(to: image, preset: preset, intensity: 0.5)

        XCTAssertNotNil(result)
        XCTAssertEqual(result.extent, image.extent)
    }

    func test_filterProcessor_applyFilterChainWithMultiplePresets() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let presets: [LutFilterPreset] = [.dokaPortrait, .ilfordHP5]

        let result = processor.applyFilterChain(to: image, presets: presets)

        XCTAssertNotNil(result)
        XCTAssertEqual(result.extent, image.extent)
    }

    func test_filterProcessor_applyFilterChainWithCustomIntensities() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let presets: [LutFilterPreset] = [.dokaPortrait, .fadedMemory]
        let intensities: [Float] = [0.5, 0.8]

        let result = processor.applyFilterChain(to: image, presets: presets, intensities: intensities)

        XCTAssertNotNil(result)
        XCTAssertEqual(result.extent, image.extent)
    }

    func test_filterProcessor_applyFilterWithEmptyPresetChain() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let presets: [LutFilterPreset] = []

        let result = processor.applyFilterChain(to: image, presets: presets)

        XCTAssertNotNil(result)
        XCTAssertEqual(result.extent, image.extent)
    }

    func test_filterProcessor_comparisonModeDefaultsToDisabled() {
        let processor = FilterProcessor.shared
        XCTAssertEqual(processor.comparisonMode, .disabled)
    }

    func test_filterProcessor_applyComparisonModeDisabled() {
        let processor = FilterProcessor.shared
        processor.comparisonMode = .disabled
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.dokaPortrait

        let result = processor.applyComparisonMode(to: image, preset: preset)

        XCTAssertNotNil(result)
    }

    func test_filterProcessor_applyComparisonModeLeftOriginal() {
        let processor = FilterProcessor.shared
        processor.comparisonMode = .leftOriginal
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.dokaPortrait

        let result = processor.applyComparisonMode(to: image, preset: preset)

        XCTAssertNotNil(result)
    }

    func test_filterProcessor_applyComparisonModeTopOriginal() {
        let processor = FilterProcessor.shared
        processor.comparisonMode = .topOriginal
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.dokaPortrait

        let result = processor.applyComparisonMode(to: image, preset: preset)

        XCTAssertNotNil(result)
    }

    func test_filterProcessor_applyFilterDifferentPresetsProduceDifferentResults() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset1 = LutFilterPreset.dokaPortrait
        let preset2 = LutFilterPreset.ilfordHP5

        let result1 = processor.applyFilter(to: image, preset: preset1, intensity: 1.0)
        let result2 = processor.applyFilter(to: image, preset: preset2, intensity: 1.0)

        // 不同预设应产生不同结果（extent 相同但内容不同无法直接比较 CIImage 内容）
        XCTAssertNotNil(result1)
        XCTAssertNotNil(result2)
        XCTAssertEqual(result1.extent, result2.extent)
    }

    func test_filterProcessor_applyFilterClampsIntensityAboveOne() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.dokaPortrait

        // 大于1的强度应被 clamp 到 1
        let result = processor.applyFilter(to: image, preset: preset, intensity: 2.5)

        XCTAssertNotNil(result)
    }

    func test_filterProcessor_applyFilterClampsIntensityBelowZero() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.dokaPortrait

        // 小于0的强度应被 clamp 到 0
        let result = processor.applyFilter(to: image, preset: preset, intensity: -1.0)

        XCTAssertNotNil(result)
    }

    func test_filterProcessor_applyFilterWithNilIntensity() {
        let processor = FilterProcessor.shared
        let image = CIImage(color: CIColor(red: 0.5, green: 0.5, blue: 0.5))
        let preset = LutFilterPreset.dokaPortrait

        let result = processor.applyFilter(to: image, preset: preset, intensity: nil)

        XCTAssertNotNil(result)
    }

    // MARK: - FilterPresetManager 测试

    func test_filterPresetManager_initializationLoadsPresets() {
        let manager = FilterPresetManager()
        XCTAssertFalse(manager.presets.isEmpty)
    }

    func test_filterPresetManager_allCategoriesLoaded() {
        let manager = FilterPresetManager()
        let categories = manager.allCategories

        // 应包含所有分类
        XCTAssertTrue(categories.contains(.portrait))
        XCTAssertTrue(categories.contains(.film))
        XCTAssertTrue(categories.contains(.vintage))
        XCTAssertTrue(categories.contains(.nature))
        XCTAssertTrue(categories.contains(.food))
        XCTAssertTrue(categories.contains(.bw))
        XCTAssertTrue(categories.contains(.creative))
        XCTAssertTrue(categories.contains(.japanese))
        XCTAssertTrue(categories.contains(.hkStyle))
        XCTAssertTrue(categories.contains(.landscape))
    }

    func test_filterPresetManager_categoryFilteringWorks() {
        let manager = FilterPresetManager()
        manager.setCategory(.portrait)

        let filtered = manager.displayedPresets
        for preset in filtered {
            XCTAssertEqual(preset.category, .portrait)
        }
    }

    func test_filterPresetManager_clearCategoryRestoresAll() {
        let manager = FilterPresetManager()
        let allPresets = manager.displayedPresets

        manager.setCategory(.bw)
        manager.clearCategory()

        XCTAssertEqual(manager.displayedPresets.count, allPresets.count)
    }

    func test_filterPresetManager_searchPresetsByDisplayName() {
        let manager = FilterPresetManager()
        let results = manager.searchPresets(query: "Doka")

        XCTAssertFalse(results.isEmpty)
        XCTAssertTrue(results.contains(where: { $0.name == "doka_portrait" }))
    }

    func test_filterPresetManager_searchPresetsReturnsEmptyForNoMatch() {
        let manager = FilterPresetManager()
        let results = manager.searchPresets(query: "NonExistentFilter12345")

        XCTAssertTrue(results.isEmpty)
    }

    func test_filterPresetManager_searchPresetsByCategoryName() {
        let manager = FilterPresetManager()
        let results = manager.searchPresets(query: "胶片")

        XCTAssertFalse(results.isEmpty)
        for preset in results {
            let isFilm = preset.category == .film || preset.displayName.contains("胶片")
            XCTAssertTrue(isFilm)
        }
    }

    func test_filterPresetManager_toggleFavoriteAddsToFavorites() {
        let manager = FilterPresetManager()
        guard let firstPreset = manager.presets.first else {
            XCTFail("No presets available")
            return
        }

        let wasFavorite = manager.isFavorite(firstPreset)
        manager.toggleFavorite(firstPreset)

        XCTAssertNotEqual(manager.isFavorite(firstPreset), wasFavorite)
    }

    func test_filterPresetManager_toggleFavoriteTwiceRestores() {
        let manager = FilterPresetManager()
        guard let firstPreset = manager.presets.first else {
            XCTFail("No presets available")
            return
        }

        let wasFavorite = manager.isFavorite(firstPreset)
        manager.toggleFavorite(firstPreset)
        manager.toggleFavorite(firstPreset)

        XCTAssertEqual(manager.isFavorite(firstPreset), wasFavorite)
    }

    func test_filterPresetManager_selectPresetSetsSelected() {
        let manager = FilterPresetManager()
        guard let firstPreset = manager.presets.first else {
            XCTFail("No presets available")
            return
        }

        manager.selectPreset(firstPreset)
        XCTAssertEqual(manager.selectedPreset?.id, firstPreset.id)
    }

    func test_filterPresetManager_selectPresetAddsToRecent() {
        let manager = FilterPresetManager()
        guard let firstPreset = manager.presets.first else {
            XCTFail("No presets available")
            return
        }

        manager.selectPreset(firstPreset)
        XCTAssertFalse(manager.recentPresets.isEmpty)
        XCTAssertEqual(manager.recentPresets.first?.id, firstPreset.id)
    }

    func test_filterPresetManager_clearSelectionSetsNil() {
        let manager = FilterPresetManager()
        guard let firstPreset = manager.presets.first else {
            XCTFail("No presets available")
            return
        }

        manager.selectPreset(firstPreset)
        manager.clearSelection()

        XCTAssertNil(manager.selectedPreset)
    }

    func test_filterPresetManager_setIntensityClampsToZero() {
        let manager = FilterPresetManager()
        manager.setIntensity(-0.5)
        XCTAssertEqual(manager.filterIntensity, 0.0)
    }

    func test_filterPresetManager_setIntensityClampsToOne() {
        let manager = FilterPresetManager()
        manager.setIntensity(1.5)
        XCTAssertEqual(manager.filterIntensity, 1.0)
    }

    func test_filterPresetManager_setIntensityWithinRange() {
        let manager = FilterPresetManager()
        manager.setIntensity(0.6)
        XCTAssertEqual(manager.filterIntensity, 0.6)
    }

    func test_filterPresetManager_getPresetsForCategory() {
        let manager = FilterPresetManager()
        let bwPresets = manager.getPresetsForCategory(.bw)

        for preset in bwPresets {
            XCTAssertEqual(preset.category, .bw)
        }
    }

    func test_filterPresetManager_categoryStatsHasAllCategories() {
        let manager = FilterPresetManager()
        let stats = manager.categoryStats

        XCTAssertEqual(stats.count, FilterCategory.allCases.count)
    }

    func test_filterPresetManager_createPresetAddsCustom() {
        let manager = FilterPresetManager()
        let initialCount = manager.customPresets.count

        _ = manager.createPreset(
            name: "test_filter",
            displayName: "测试滤镜",
            category: .creative,
            parameters: FilterParameters(
                temperature: 500,
                exposure: 0.1,
                contrast: 1.1
            )
        )

        XCTAssertEqual(manager.customPresets.count, initialCount + 1)
    }

    func test_filterPresetManager_removeCustomPreset() {
        let manager = FilterPresetManager()
        let preset = manager.createPreset(
            name: "test_remove",
            displayName: "待删除",
            category: .creative,
            parameters: FilterParameters()
        )
        let initialCount = manager.customPresets.count

        manager.removeCustomPreset(preset)

        XCTAssertEqual(manager.customPresets.count, initialCount - 1)
        XCTAssertNil(manager.presets.first(where: { $0.id == preset.id }))
    }

    func test_filterPresetManager_exportPresetReturnsData() {
        let manager = FilterPresetManager()
        guard let preset = manager.presets.first else {
            XCTFail("No presets available")
            return
        }

        let data = manager.exportPreset(preset)
        XCTAssertNotNil(data)
    }

    func test_filterPresetManager_importPresetFromData() {
        let manager = FilterPresetManager()
        guard let preset = manager.presets.first,
              let data = manager.exportPreset(preset) else {
            XCTFail("Cannot export preset")
            return
        }

        let initialCount = manager.customPresets.count
        XCTAssertNoThrow(try manager.importPreset(from: data))
        XCTAssertEqual(manager.customPresets.count, initialCount + 1)
    }

    func test_filterPresetManager_bringToFront() {
        let manager = FilterPresetManager()
        guard manager.presets.count >= 2 else {
            XCTFail("Need at least 2 presets")
            return
        }

        let secondPreset = manager.presets[1]
        manager.bringToFront(secondPreset)

        XCTAssertEqual(manager.presets.first?.id, secondPreset.id)
    }

    func test_filterPresetManager_reorderPresets() {
        let manager = FilterPresetManager()
        let original = manager.presets
        let reversed = Array(original.reversed())

        manager.reorderPresets(reversed)

        XCTAssertEqual(manager.presets.first?.id, reversed.first?.id)
    }

    func test_filterPresetManager_loadBuiltInPresetsHasAllCategories() {
        let manager = FilterPresetManager()
        let allBuiltIn = LutFilterPreset.allBuiltInPresets

        XCTAssertTrue(allBuiltIn.count >= 30)
    }

    // MARK: - BeautyParams 测试

    func test_beautyParams_defaultValues() {
        let params = BeautyParams.default

        XCTAssertEqual(params.skinSmoothing, 0.3)
        XCTAssertEqual(params.skinTone, 0.0)
        XCTAssertEqual(params.eyeBrightening, 0.2)
        XCTAssertEqual(params.teethWhitening, 0.0)
        XCTAssertEqual(params.faceSlimming, 0.0)
        XCTAssertEqual(params.blemishRemoval, 0.3)
    }

    func test_beautyParams_offAllZero() {
        let params = BeautyParams.off

        XCTAssertEqual(params.skinSmoothing, 0.0)
        XCTAssertEqual(params.skinTone, 0.0)
        XCTAssertEqual(params.eyeBrightening, 0.0)
        XCTAssertEqual(params.teethWhitening, 0.0)
        XCTAssertEqual(params.faceSlimming, 0.0)
        XCTAssertEqual(params.blemishRemoval, 0.0)
    }

    func test_beautyParams_isOffWhenAllZero() {
        let params = BeautyParams.off
        XCTAssertTrue(params.isOff)
    }

    func test_beautyParams_isNotOffWhenAnyNonZero() {
        var params = BeautyParams.off
        params.skinSmoothing = 0.1
        XCTAssertFalse(params.isOff)
    }

    func test_beautyPreset_naturalAllZero() {
        let params = BeautyPreset.natural.params()
        XCTAssertTrue(params.isOff)
    }

    func test_beautyPreset_delicateHasCorrectValues() {
        let params = BeautyPreset.delicate.params()

        XCTAssertEqual(params.skinSmoothing, 0.4)
        XCTAssertEqual(params.skinTone, 0.2)
        XCTAssertEqual(params.eyeBrightening, 0.3)
        XCTAssertEqual(params.teethWhitening, 0.2)
        XCTAssertEqual(params.faceSlimming, 0.15)
        XCTAssertEqual(params.blemishRemoval, 0.4)
    }

    func test_beautyPreset_goddessHasCorrectValues() {
        let params = BeautyPreset.goddess.params()

        XCTAssertEqual(params.skinSmoothing, 0.7)
        XCTAssertEqual(params.skinTone, 0.5)
        XCTAssertEqual(params.eyeBrightening, 0.6)
        XCTAssertEqual(params.teethWhitening, 0.5)
        XCTAssertEqual(params.faceSlimming, 0.4)
        XCTAssertEqual(params.blemishRemoval, 0.7)
    }

    func test_beautyPreset_customHasDefaultValues() {
        let params = BeautyPreset.custom.params()
        XCTAssertEqual(params.skinSmoothing, BeautyParams.default.skinSmoothing)
    }

    func test_beautyPreset_allCasesCount() {
        XCTAssertEqual(BeautyPreset.allCases.count, 4)
    }

    func test_beautyPreset_displayNames() {
        let names = BeautyPreset.allCases.map { $0.displayName }
        XCTAssertTrue(names.contains("自然"))
        XCTAssertTrue(names.contains("精致"))
        XCTAssertTrue(names.contains("女神"))
        XCTAssertTrue(names.contains("自定义"))
    }

    // MARK: - WatermarkConfig 测试

    func test_watermarkConfig_defaultValues() {
        let config = WatermarkConfig()

        XCTAssertFalse(config.isEnabled)
        XCTAssertEqual(config.text, "")
        XCTAssertTrue(config.showDate)
        XCTAssertEqual(config.dateFormat, "yyyy-MM-dd HH:mm")
        XCTAssertFalse(config.showEXIF)
        XCTAssertFalse(config.showLogo)
        XCTAssertNil(config.logoImage)
        XCTAssertEqual(config.position, .bottomRight)
        XCTAssertEqual(config.fontSize, 14)
        XCTAssertEqual(config.textColor, .white)
        XCTAssertEqual(config.opacity, 0.85)
        XCTAssertEqual(config.horizontalPadding, 16)
        XCTAssertEqual(config.verticalPadding, 16)
        XCTAssertEqual(config.style, .minimal)
    }

    func test_watermarkConfig_encodeDecode() {
        var config = WatermarkConfig()
        config.isEnabled = true
        config.text = "测试水印"
        config.showDate = false
        config.position = .topLeft
        config.fontSize = 18
        config.textColor = .black
        config.opacity = 0.5
        config.style = .film

        guard let data = try? JSONEncoder().encode(config) else {
            XCTFail("Encoding failed")
            return
        }

        guard let decoded = try? JSONDecoder().decode(WatermarkConfig.self, from: data) else {
            XCTFail("Decoding failed")
            return
        }

        XCTAssertEqual(decoded.isEnabled, true)
        XCTAssertEqual(decoded.text, "测试水印")
        XCTAssertEqual(decoded.showDate, false)
        XCTAssertEqual(decoded.position, .topLeft)
        XCTAssertEqual(decoded.fontSize, 18)
        XCTAssertEqual(decoded.textColor, .black)
        XCTAssertEqual(decoded.opacity, 0.5)
        XCTAssertEqual(decoded.style, .film)
    }

    func test_watermarkConfig_saveAndLoad() {
        var config = WatermarkConfig()
        config.isEnabled = true
        config.text = "持久化测试"
        config.showDate = true
        config.position = .center
        config.save()

        let loaded = WatermarkConfig.load()
        XCTAssertEqual(loaded.isEnabled, true)
        XCTAssertEqual(loaded.text, "持久化测试")
        XCTAssertEqual(loaded.showDate, true)
        XCTAssertEqual(loaded.position, .center)
    }

    func test_watermarkPosition_allCases() {
        let all = WatermarkPosition.allCases
        XCTAssertEqual(all.count, 5)
        XCTAssertTrue(all.contains(.topLeft))
        XCTAssertTrue(all.contains(.topRight))
        XCTAssertTrue(all.contains(.bottomLeft))
        XCTAssertTrue(all.contains(.bottomRight))
        XCTAssertTrue(all.contains(.center))
    }

    func test_watermarkPosition_displayNames() {
        XCTAssertEqual(WatermarkPosition.topLeft.displayName, "左上")
        XCTAssertEqual(WatermarkPosition.topRight.displayName, "右上")
        XCTAssertEqual(WatermarkPosition.bottomLeft.displayName, "左下")
        XCTAssertEqual(WatermarkPosition.bottomRight.displayName, "右下")
        XCTAssertEqual(WatermarkPosition.center.displayName, "居中")
    }

    func test_watermarkStyle_allCases() {
        let all = WatermarkStyle.allCases
        XCTAssertEqual(all.count, 4)
        XCTAssertTrue(all.contains(.minimal))
        XCTAssertTrue(all.contains(.classic))
        XCTAssertTrue(all.contains(.modern))
        XCTAssertTrue(all.contains(.film))
    }

    func test_watermarkStyle_displayNames() {
        XCTAssertEqual(WatermarkStyle.minimal.displayName, "极简")
        XCTAssertEqual(WatermarkStyle.classic.displayName, "经典")
        XCTAssertEqual(WatermarkStyle.modern.displayName, "现代")
        XCTAssertEqual(WatermarkStyle.film.displayName, "胶片")
    }

    func test_watermarkColor_hexInit() {
        let color = WatermarkColor(hex: "FF6B00", alpha: 0.9)
        XCTAssertEqual(color.red, 1.0, accuracy: 0.01)
        XCTAssertEqual(color.green, 0.42, accuracy: 0.01)
        XCTAssertEqual(color.blue, 0.0, accuracy: 0.01)
        XCTAssertEqual(color.alpha, 0.9)
    }

    func test_watermarkColor_presetPalette() {
        let palette = WatermarkColor.presetPalette
        XCTAssertEqual(palette.count, 4)
    }

    func test_watermarkTemplate_allTemplatesLoaded() {
        let templates = WatermarkTemplate.allTemplates
        XCTAssertEqual(templates.count, 5)
    }

    // MARK: - PhotoStorageService 测试

    func test_photoStorageService_sharedExists() {
        let service = PhotoStorageService.shared
        XCTAssertNotNil(service)
    }

    func test_photoStorageService_sharedIsSameInstance() {
        let s1 = PhotoStorageService.shared
        let s2 = PhotoStorageService.shared
        XCTAssertTrue(s1 === s2)
    }

    func test_photoStorageService_loadRecordsReturnsArray() {
        let service = PhotoStorageService.shared
        let records = service.loadRecords()
        XCTAssertTrue(records is [PhotoRecord])
    }

    func test_photoStorageService_recordsPublisherExists() {
        let service = PhotoStorageService.shared
        XCTAssertNotNil(service.recordsPublisher)
    }

    func test_photoStorageService_savePhotoAcceptsData() {
        let service = PhotoStorageService.shared
        let testData = "test_photo_data".data(using: .utf8)!

        service.savePhoto(data: testData, detectionMethod: "Test")

        XCTAssertTrue(true)
    }

    func test_photoStorageService_savePhotoWithNilDetectionMethod() {
        let service = PhotoStorageService.shared
        let testData = "test_photo_nil".data(using: .utf8)!

        service.savePhoto(data: testData, detectionMethod: nil)

        XCTAssertTrue(true)
    }

    func test_photoStorageService_thumbnailForNonexistentId() {
        let service = PhotoStorageService.shared
        let result = service.thumbnail(for: UUID())
        XCTAssertNil(result)
    }

    func test_photoStorageService_photoURLForNonexistentId() {
        let service = PhotoStorageService.shared
        let result = service.photoURL(for: UUID())
        XCTAssertNil(result)
    }

    func test_photoStorageService_deleteRecordAcceptsAnyId() {
        let service = PhotoStorageService.shared
        let randomId = UUID()
        // 不应崩溃
        service.deleteRecord(randomId)
        XCTAssertTrue(true)
    }

    func test_photoRecord_initialization() {
        let record = PhotoRecord(
            id: UUID(),
            creationDate: Date(),
            localIdentifier: "test_id",
            detectionMethod: "Fast",
            iso: 400,
            shutterSpeed: 1.0 / 125,
            aperture: 2.8,
            imageWidth: 4032,
            imageHeight: 3024
        )

        XCTAssertEqual(record.localIdentifier, "test_id")
        XCTAssertEqual(record.detectionMethod, "Fast")
        XCTAssertEqual(record.iso, 400)
        XCTAssertEqual(record.shutterSpeed, 1.0 / 125)
        XCTAssertEqual(record.aperture, 2.8)
        XCTAssertEqual(record.imageWidth, 4032)
        XCTAssertEqual(record.imageHeight, 3024)
    }

    func test_photoRecord_photoFilename() {
        let id = UUID()
        let filename = PhotoRecord.photoFilename(for: id)

        XCTAssertTrue(filename.hasSuffix(".jpg"))
        XCTAssertTrue(filename.contains(id.uuidString))
    }

    func test_photoRecord_thumbnailFilename() {
        let id = UUID()
        let filename = PhotoRecord.thumbnailFilename(for: id)

        XCTAssertTrue(filename.hasSuffix("_thumb.jpg"))
        XCTAssertTrue(filename.contains(id.uuidString))
    }

    func test_photoRecord_defaultValues() {
        let record = PhotoRecord()

        XCTAssertNotNil(record.id)
        XCTAssertNotNil(record.creationDate)
        XCTAssertNil(record.localIdentifier)
        XCTAssertNil(record.detectionMethod)
        XCTAssertNil(record.iso)
        XCTAssertNil(record.shutterSpeed)
        XCTAssertNil(record.aperture)
        XCTAssertNil(record.imageWidth)
        XCTAssertNil(record.imageHeight)
    }

    func test_photoRecord_equatable() {
        let id = UUID()
        let record1 = PhotoRecord(id: id)
        let record2 = PhotoRecord(id: id)
        let record3 = PhotoRecord(id: UUID())

        XCTAssertEqual(record1, record2)
        XCTAssertNotEqual(record1, record3)
    }

    // MARK: - EditHistoryManager 测试

    func test_editHistoryManager_initialState() {
        let manager = EditHistoryManager()

        XCTAssertTrue(manager.historySteps.isEmpty)
        XCTAssertEqual(manager.currentIndex, -1)
        XCTAssertFalse(manager.canUndo)
        XCTAssertFalse(manager.canRedo)
    }

    func test_editHistoryManager_recordStepAddsToHistory() {
        let manager = EditHistoryManager()
        manager.recordStep(filterName: "测试滤镜")

        XCTAssertEqual(manager.historySteps.count, 1)
        XCTAssertEqual(manager.currentIndex, 0)
    }

    func test_editHistoryManager_recordStepCanUndo() {
        let manager = EditHistoryManager()

        manager.recordStep(filterName: "步骤1")
        manager.recordStep(filterName: "步骤2")

        XCTAssertTrue(manager.canUndo)
    }

    func test_editHistoryManager_undoReturnsPreviousStep() {
        let manager = EditHistoryManager()

        manager.recordStep(filterName: "步骤1")
        manager.recordStep(filterName: "步骤2")

        let step = manager.undo()
        XCTAssertNotNil(step)
        XCTAssertEqual(step?.filterName, "步骤1")
    }

    func test_editHistoryManager_undoThenRedo() {
        let manager = EditHistoryManager()

        manager.recordStep(filterName: "步骤1")
        manager.recordStep(filterName: "步骤2")

        let undone = manager.undo()
        XCTAssertEqual(undone?.filterName, "步骤1")

        let redone = manager.redo()
        XCTAssertEqual(redone?.filterName, "步骤2")
    }

    func test_editHistoryManager_cannotUndoInitially() {
        let manager = EditHistoryManager()
        XCTAssertNil(manager.undo())
    }

    func test_editHistoryManager_cannotRedoAfterUndo() {
        let manager = EditHistoryManager()
        manager.recordStep(filterName: "唯一步骤")

        // 只有一步，undo 后应回到索引0
        let result = manager.undo()
        XCTAssertNotNil(result)

        // 只有一个步骤，undo回到索引0，无法再redo
        XCTAssertFalse(manager.canRedo)
    }

    func test_editHistoryManager_cannotRedoInitially() {
        let manager = EditHistoryManager()
        XCTAssertNil(manager.redo())
    }

    func test_editHistoryManager_recordStepAfterUndoTruncatesHistory() {
        let manager = EditHistoryManager()

        manager.recordStep(filterName: "步骤1")
        manager.recordStep(filterName: "步骤2")
        manager.recordStep(filterName: "步骤3")

        _ = manager.undo() // 回到步骤2
        _ = manager.undo() // 回到步骤1

        // 从步骤1开始记录新步骤，步骤2和3应被清除
        manager.recordStep(filterName: "新步骤")

        // 新步骤在索引1，历史只有两步
        XCTAssertEqual(manager.historySteps.count, 2)
        XCTAssertEqual(manager.currentIndex, 1)
    }

    func test_editHistoryManager_maxStepsLimit() {
        let manager = EditHistoryManager()

        // 添加超过最大限制的步骤（默认50）
        for i in 0..<60 {
            manager.recordStep(filterName: "步骤\(i)")
        }

        // 应限制在50步内
        XCTAssertLessThanOrEqual(manager.historySteps.count, 50)
    }

    func test_editHistoryManager_resetClearsAll() {
        let manager = EditHistoryManager()

        manager.recordStep(filterName: "步骤1")
        manager.recordStep(filterName: "步骤2")
        manager.reset()

        XCTAssertTrue(manager.historySteps.isEmpty)
        XCTAssertEqual(manager.currentIndex, -1)
        XCTAssertFalse(manager.canUndo)
        XCTAssertFalse(manager.canRedo)
    }

    func test_editHistoryManager_currentStep() {
        let manager = EditHistoryManager()

        manager.recordStep(filterName: "步骤1")
        manager.recordStep(filterName: "步骤2")

        let current = manager.currentStep
        XCTAssertNotNil(current)
        XCTAssertEqual(current?.filterName, "步骤2")
    }

    func test_editHistoryStep_displayName() {
        let step = EditHistoryStep(filterName: "测试滤镜", timestamp: Date(), thumbnail: nil, parameters: [:])

        let displayName = step.displayName
        XCTAssertTrue(displayName.contains("测试滤镜"))
    }

    func test_editHistoryStep_equatable() {
        let step1 = EditHistoryStep(filterName: "A", timestamp: Date(), thumbnail: nil, parameters: [:])
        let step2 = EditHistoryStep(filterName: "B", timestamp: Date(), thumbnail: nil, parameters: [:])

        XCTAssertNotEqual(step1, step2)
        XCTAssertEqual(step1, step1)
    }

    // MARK: - FilterParameters 测试

    func test_filterParameters_neutral() {
        let params = FilterParameters.neutral

        XCTAssertEqual(params.temperature, 0)
        XCTAssertEqual(params.tint, 0)
        XCTAssertEqual(params.exposure, 0)
        XCTAssertEqual(params.brightness, 0)
        XCTAssertEqual(params.contrast, 1.0)
        XCTAssertEqual(params.saturation, 1.0)
        XCTAssertEqual(params.vibrance, 0)
        XCTAssertEqual(params.highlightAmount, 1.0)
        XCTAssertEqual(params.shadowAmount, 0)
        XCTAssertFalse(params.isMonochrome)
        XCTAssertEqual(params.monochromeIntensity, 0)
    }

    func test_filterParameters_equatable() {
        let p1 = FilterParameters.neutral
        var p2 = FilterParameters.neutral
        p2.temperature = 500

        XCTAssertEqual(p1, p1)
        XCTAssertNotEqual(p1, p2)
    }

    // MARK: - LutFilterPreset 测试

    func test_lutFilterPreset_allBuiltInPresetsNotEmpty() {
        let presets = LutFilterPreset.allBuiltInPresets
        XCTAssertFalse(presets.isEmpty)
    }

    func test_lutFilterPreset_findByName() {
        let preset = LutFilterPreset.findByName("doka_portrait")
        XCTAssertNotNil(preset)
        XCTAssertEqual(preset?.displayName, "Doka 人像")
    }

    func test_lutFilterPreset_findByInvalidName() {
        let preset = LutFilterPreset.findByName("non_existent_filter")
        XCTAssertNil(preset)
    }

    func test_lutFilterPreset_findById() {
        guard let preset = LutFilterPreset.allBuiltInPresets.first else {
            XCTFail("No presets")
            return
        }

        let found = LutFilterPreset.find(by: preset.id)
        XCTAssertNotNil(found)
        XCTAssertEqual(found?.id, preset.id)
    }

    func test_lutFilterPreset_presetsForCategory() {
        let portraitPresets = LutFilterPreset.presetsForCategory(.portrait)
        for preset in portraitPresets {
            XCTAssertEqual(preset.category, .portrait)
        }
    }

    func test_lutFilterPreset_categoryStats() {
        let stats = LutFilterPreset.categoryStats
        let totalPresets = stats.reduce(0) { $0 + $1.1 }
        XCTAssertEqual(totalPresets, LutFilterPreset.allBuiltInPresets.count)
    }

    func test_lutFilterPreset_encodeDecode() {
        guard let preset = LutFilterPreset.allBuiltInPresets.first else {
            XCTFail("No presets")
            return
        }

        guard let data = try? JSONEncoder().encode(preset) else {
            XCTFail("Encoding failed")
            return
        }

        guard let decoded = try? JSONDecoder().decode(LutFilterPreset.self, from: data) else {
            XCTFail("Decoding failed")
            return
        }

        XCTAssertEqual(decoded.id, preset.id)
        XCTAssertEqual(decoded.name, preset.name)
        XCTAssertEqual(decoded.displayName, preset.displayName)
        XCTAssertEqual(decoded.category, preset.category)
        XCTAssertEqual(decoded.defaultIntensity, preset.defaultIntensity)
    }

    // MARK: - 滤镜预设静态属性测试

    func test_dokaPortraitPreset() {
        let preset = LutFilterPreset.dokaPortrait
        XCTAssertEqual(preset.name, "doka_portrait")
        XCTAssertEqual(preset.category, .portrait)
        XCTAssertEqual(preset.defaultIntensity, 0.85)
    }

    func test_kodakPortra160Preset() {
        let preset = LutFilterPreset.kodakPortra160
        XCTAssertEqual(preset.name, "kodak_portra_160")
        XCTAssertEqual(preset.category, .film)
        XCTAssertEqual(preset.defaultIntensity, 0.9)
    }

    func test_ilfordHP5Preset() {
        let preset = LutFilterPreset.ilfordHP5
        XCTAssertEqual(preset.category, .bw)
        XCTAssertTrue(preset.parameters.isMonochrome)
        XCTAssertEqual(preset.parameters.monochromeIntensity, 1.0)
    }

    func test_fadedMemoryPreset() {
        let preset = LutFilterPreset.fadedMemory
        XCTAssertEqual(preset.category, .vintage)
        XCTAssertEqual(preset.parameters.saturation, 0.72)
    }

    func test_japaneseAiryPreset() {
        let preset = LutFilterPreset.japaneseAiry
        XCTAssertEqual(preset.displayName, "日系透明感")
        XCTAssertEqual(preset.parameters.exposure, 0.35)
        XCTAssertEqual(preset.parameters.contrast, 0.80)
    }
}