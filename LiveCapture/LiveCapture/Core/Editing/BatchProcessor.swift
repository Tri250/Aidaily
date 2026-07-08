//
//  BatchProcessor.swift
//  LiveCapture
//
//  批量处理器 - 并行处理多张照片的滤镜应用、自动增强和批量删除
//
//  ## 主要功能
//  - applyFilter: 批量应用滤镜预设
//  - applyAutoEnhance: 批量自动增强
//  - deleteImages: 批量删除照片
//  - 实时进度跟踪
//

import Foundation
import Combine
import UIKit
import CoreImage

#if os(iOS)

/// 批量处理器
final class BatchProcessor: ObservableObject {

    // MARK: - 发布属性

    @Published var isProcessing = false
    @Published var progress: Float = 0
    @Published var completedCount: Int = 0
    @Published var totalCount: Int = 0

    // MARK: - 私有属性

    private let filterProcessor = LutFilterProcessor()
    private let autoEnhancer = AutoEnhancer()
    private let processingQueue = DispatchQueue(label: "livecapture.batch.processor", qos: .userInitiated, attributes: .concurrent)
    private let context = CIContext(options: [.workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB) ?? CGColorSpaceCreateDeviceRGB()])

    private let lock = NSLock()

    // MARK: - 批量应用滤镜

    /// 批量应用滤镜预设
    /// - Parameters:
    ///   - filter: 滤镜预设
    ///   - images: 照片记录列表
    ///   - intensity: 滤镜强度，默认 1.0
    /// - Returns: 处理后的照片记录列表（由于 PhotoRecord 是值类型，返回新列表）
    func applyFilter(_ filter: LutFilterPreset, to images: [PhotoRecord], intensity: Float = 1.0) async -> [PhotoRecord] {
        await resetProgress(total: images.count)
        let storage = PhotoStorageService.shared

        let processed = await withTaskGroup(of: (Int, Bool).self) { group -> [PhotoRecord] in
            for (index, record) in images.enumerated() {
                group.addTask {
                    let success = await self.processFilter(record: record, filter: filter, intensity: intensity, storage: storage)
                    return (index, success)
                }
            }

            var results: [(Int, Bool)] = []
            for await result in group {
                results.append(result)
            }
            results.sort { $0.0 < $1.0 }

            // 返回所有记录（无论是否处理成功）
            return images
        }

        await finishProcessing()
        return processed
    }

    /// 处理单张照片的滤镜应用
    private func processFilter(record: PhotoRecord, filter: LutFilterPreset, intensity: Float, storage: PhotoStorageService) async -> Bool {
        guard let url = storage.photoURL(for: record.id),
              let data = try? Data(contentsOf: url),
              let uiImage = UIImage(data: data) else {
            await incrementProgress()
            return false
        }

        // 加载 CIImage
        let ciImage: CIImage
        if let ci = CIImage(data: data) {
            ciImage = ci
        } else if let cgImage = uiImage.cgImage {
            ciImage = CIImage(cgImage: cgImage)
        } else {
            await incrementProgress()
            return false
        }

        // 应用滤镜
        let filtered = filterProcessor.applyFilter(to: ciImage, preset: filter, intensity: intensity)

        // 渲染为 JPEG 数据
        guard let cgImage = context.createCGImage(filtered, from: filtered.extent),
              let filteredData = UIImage(cgImage: cgImage).jpegData(compressionQuality: 0.92) else {
            await incrementProgress()
            return false
        }

        // 保存处理后的照片
        let photoURL = storage.photoURL(for: record.id) ?? URL(fileURLWithPath: "")
        try? filteredData.write(to: photoURL, options: .atomic)

        // 更新缩略图
        if let thumbData = ThumbnailGenerator.generate(from: filteredData) {
            let thumbURL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
                .appendingPathComponent("LiveCapture", isDirectory: true)
                .appendingPathComponent("thumbnails", isDirectory: true)
                .appendingPathComponent(PhotoRecord.thumbnailFilename(for: record.id))
            try? thumbData.write(to: thumbURL, options: .atomic)
        }

        await incrementProgress()
        return true
    }

    // MARK: - 批量自动增强

    /// 批量自动增强
    /// - Parameter images: 照片记录列表
    /// - Returns: 处理后的照片记录列表
    func applyAutoEnhance(to images: [PhotoRecord]) async -> [PhotoRecord] {
        await resetProgress(total: images.count)
        let storage = PhotoStorageService.shared

        let processed = await withTaskGroup(of: (Int, Bool).self) { group -> [PhotoRecord] in
            for (index, record) in images.enumerated() {
                group.addTask {
                    let success = await self.processAutoEnhance(record: record, storage: storage)
                    return (index, success)
                }
            }

            var results: [(Int, Bool)] = []
            for await result in group {
                results.append(result)
            }
            results.sort { $0.0 < $1.0 }

            return images
        }

        await finishProcessing()
        return processed
    }

    /// 处理单张照片的自动增强
    private func processAutoEnhance(record: PhotoRecord, storage: PhotoStorageService) async -> Bool {
        guard let url = storage.photoURL(for: record.id),
              let data = try? Data(contentsOf: url),
              let uiImage = UIImage(data: data) else {
            await incrementProgress()
            return false
        }

        let ciImage: CIImage
        if let ci = CIImage(data: data) {
            ciImage = ci
        } else if let cgImage = uiImage.cgImage {
            ciImage = CIImage(cgImage: cgImage)
        } else {
            await incrementProgress()
            return false
        }

        let enhanced = autoEnhancer.autoEnhance(ciImage)

        guard let cgImage = context.createCGImage(enhanced, from: enhanced.extent),
              let enhancedData = UIImage(cgImage: cgImage).jpegData(compressionQuality: 0.92) else {
            await incrementProgress()
            return false
        }

        let photoURL = storage.photoURL(for: record.id) ?? URL(fileURLWithPath: "")
        try? enhancedData.write(to: photoURL, options: .atomic)

        if let thumbData = ThumbnailGenerator.generate(from: enhancedData) {
            let thumbURL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
                .appendingPathComponent("LiveCapture", isDirectory: true)
                .appendingPathComponent("thumbnails", isDirectory: true)
                .appendingPathComponent(PhotoRecord.thumbnailFilename(for: record.id))
            try? thumbData.write(to: thumbURL, options: .atomic)
        }

        await incrementProgress()
        return true
    }

    // MARK: - 批量删除

    /// 批量删除照片
    /// - Parameter records: 要删除的照片记录列表
    func deleteImages(_ records: [PhotoRecord]) async {
        await resetProgress(total: records.count)
        let storage = PhotoStorageService.shared

        await withTaskGroup(of: Void.self) { group in
            for record in records {
                group.addTask {
                    storage.deleteRecord(record.id)
                    await self.incrementProgress()
                }
            }

            for await _ in group {}
        }

        await finishProcessing()
    }

    // MARK: - 进度管理

    @MainActor
    private func resetProgress(total: Int) {
        isProcessing = true
        progress = 0
        completedCount = 0
        totalCount = total
    }

    @MainActor
    private func incrementProgress() {
        completedCount += 1
        if totalCount > 0 {
            progress = Float(completedCount) / Float(totalCount)
        }
    }

    @MainActor
    private func finishProcessing() {
        progress = 1.0
        completedCount = totalCount
        isProcessing = false
    }
}

#endif