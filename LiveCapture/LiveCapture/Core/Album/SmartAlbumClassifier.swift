//
//  SmartAlbumClassifier.swift
//  LiveCapture
//
//  智能相册分类器 - 基于 Vision 框架的场景识别、日期分组、位置分组和人脸聚类
//
//  ## 主要功能
//  - classifyPhoto: 使用 VNClassifyImageRequest 对单张照片进行场景分类
//  - groupByScene: 按场景类型分组照片
//  - groupByDate: 按日期分组照片（今天、昨天、本周、本月、更早）
//  - groupByLocation: 按位置分组照片
//  - groupByFaces: 按人脸聚类分组照片
//

import Foundation
import Vision
import Photos
import UIKit
import CoreImage

#if os(iOS)

/// 智能相册分类器
final class SmartAlbumClassifier {

    // MARK: - 私有属性

    private let queue = DispatchQueue(label: "livecapture.album.classifier", qos: .userInitiated)
    private let ciContext = CIContext(options: [.workingColorSpace: CGColorSpace(name: CGColorSpace.sRGB)!])

    /// Vision 场景标签到 SceneType 的映射表
    private let sceneLabelMapping: [String: SceneType] = [
        "portrait": .portrait,
        "human": .portrait,
        "people": .portrait,
        "person": .portrait,
        "food": .food,
        "cuisine": .food,
        "meal": .food,
        "dish": .food,
        "landscape": .landscape,
        "nature": .landscape,
        "mountain": .landscape,
        "forest": .landscape,
        "sky": .landscape,
        "animal": .pet,
        "dog": .pet,
        "cat": .pet,
        "pet": .pet,
        "building": .architecture,
        "architecture": .architecture,
        "city": .architecture,
        "night": .nightScene,
        "nightlife": .nightScene,
        "document": .document,
        "text": .document,
        "paper": .document,
        "sunrise": .sunrise,
        "sunset": .sunrise,
        "dusk": .sunrise,
        "dawn": .sunrise,
        "snow": .snow,
        "snowscape": .snow,
        "beach": .beach,
        "ocean": .beach,
        "seaside": .beach,
        "flower": .flower,
        "blossom": .flower,
        "stage": .stage,
        "performance": .stage,
        "concert": .stage,
        "street": .street,
        "road": .street,
        "indoor": .indoor,
        "interior": .indoor,
        "room": .indoor
    ]

    // MARK: - 场景分类

    /// 对单张照片进行场景分类
    /// - Parameters:
    ///   - image: 输入 UIImage
    ///   - completion: 完成回调，返回场景类型和置信度
    func classifyPhoto(_ image: UIImage, completion: @escaping (SceneType, Float) -> Void) {
        guard let cgImage = image.cgImage else {
            completion(.unknown, 0)
            return
        }

        queue.async { [weak self] in
            guard let self else {
                completion(.unknown, 0)
                return
            }

            if #available(iOS 17.0, *) {
                self.performVisionClassification(cgImage: cgImage, completion: completion)
            } else {
                // iOS 17 以下使用图像分析兜底
                self.performFallbackClassification(image: image, completion: completion)
            }
        }
    }

    @available(iOS 17.0, *)
    private func performVisionClassification(cgImage: CGImage, completion: @escaping (SceneType, Float) -> Void) {
        let request = VNClassifyImageRequest()
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])

        do {
            try handler.perform([request])
            guard let observations = request.results, !observations.isEmpty else {
                completion(.unknown, 0)
                return
            }

            var bestScene: SceneType = .unknown
            var bestConfidence: Float = 0

            for observation in observations {
                let label = observation.identifier.lowercased()
                let confidence = observation.confidence

                // 直接匹配
                if let mapped = self.sceneLabelMapping[label] {
                    if confidence > bestConfidence {
                        bestScene = mapped
                        bestConfidence = confidence
                    }
                }

                // 子串匹配
                if bestScene == .unknown {
                    for (key, sceneType) in self.sceneLabelMapping {
                        if label.contains(key) || key.contains(label) {
                            bestScene = sceneType
                            bestConfidence = confidence
                            break
                        }
                    }
                }
            }

            completion(bestScene, bestConfidence)
        } catch {
            completion(.unknown, 0)
        }
    }

    /// iOS 17 以下的兜底分类：基于图像基本属性
    private func performFallbackClassification(image: UIImage, completion: @escaping (SceneType, Float) -> Void) {
        guard let cgImage = image.cgImage else {
            completion(.unknown, 0)
            return
        }

        let ciImage = CIImage(cgImage: cgImage)
        let brightness = computeAverageBrightness(ciImage: ciImage)

        // 简单规则化分类
        let scene: SceneType
        if brightness < 0.15 {
            scene = .nightScene
        } else if brightness > 0.75 {
            scene = .landscape
        } else {
            scene = .unknown
        }

        completion(scene, 0.5)
    }

    /// 计算图像平均亮度
    private func computeAverageBrightness(ciImage: CIImage) -> Float {
        let extent = ciImage.extent
        let scale = min(1.0, 64.0 / max(extent.width, extent.height))
        let scaled = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))

        var outBuffer: CVPixelBuffer?
        let attrs: [String: Any] = [
            kCVPixelBufferCGImageCompatibilityKey as String: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
        ]
        guard CVPixelBufferCreate(kCFAllocatorDefault, Int(scaled.extent.width), Int(scaled.extent.height),
                                  kCVPixelFormatType_OneComponent8, attrs as CFDictionary, &outBuffer) == kCVReturnSuccess,
              let buffer = outBuffer else { return 0.5 }

        let monoFilter = CIFilter(name: "CIPhotoEffectMono", parameters: [kCIInputImageKey: scaled])
        guard let mono = monoFilter?.outputImage else { return 0.5 }
        ciContext.render(mono, to: buffer)

        CVPixelBufferLockBaseAddress(buffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(buffer, .readOnly) }

        guard let base = CVPixelBufferGetBaseAddress(buffer) else { return 0.5 }
        let bytesPerRow = CVPixelBufferGetBytesPerRow(buffer)
        let ptr = base.assumingMemoryBound(to: UInt8.self)
        let w = Int(scaled.extent.width)
        let h = Int(scaled.extent.height)

        var sum: UInt64 = 0
        var count = 0
        for y in stride(from: 0, to: h, by: 4) {
            for x in stride(from: 0, to: w, by: 4) {
                sum += UInt64(ptr[y * bytesPerRow + x])
                count += 1
            }
        }
        return count > 0 ? Float(sum) / Float(count * 255) : 0.5
    }

    // MARK: - 按场景分组

    /// 按场景类型分组照片
    /// - Parameter records: 照片记录列表
    /// - Returns: 场景类型到照片记录列表的映射
    func groupByScene(_ records: [PhotoRecord]) -> [SceneType: [PhotoRecord]] {
        var groups: [SceneType: [PhotoRecord]] = [:]
        let storage = PhotoStorageService.shared

        for record in records {
            // 基于 EXIF 数据进行快速规则化场景分类
            let scene = classifyRecordByMetadata(record)
            groups[scene, default: []].append(record)
        }

        return groups
    }

    /// 基于 EXIF 元数据快速分类
    private func classifyRecordByMetadata(_ record: PhotoRecord) -> SceneType {
        // 根据 ISO 和快门速度推断场景
        if let iso = record.iso {
            if iso >= 1600 {
                return .nightScene  // 高 ISO 通常意味着暗光环境
            }
        }
        if let shutter = record.shutterSpeed {
            if shutter > 0.5 {
                return .nightScene  // 长曝光 → 夜景
            }
            if shutter < 1.0 / 2000.0 {
                return .landscape  // 高速快门 → 户外亮光
            }
        }
        return .unknown
    }

    // MARK: - 按日期分组

    /// 按日期分组照片（中文标签）
    /// - Parameter records: 照片记录列表
    /// - Returns: 日期标签到照片记录列表的映射
    func groupByDate(_ records: [PhotoRecord]) -> [String: [PhotoRecord]] {
        let calendar = Calendar.current
        let now = Date()
        let startOfToday = calendar.startOfDay(for: now)
        guard let startOfYesterday = calendar.date(byAdding: .day, value: -1, to: startOfToday),
              let startOfWeek = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)),
              let startOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)) else {
            return ["全部": records]
        }

        var groups: [String: [PhotoRecord]] = [:]
        let dateFormatter = DateFormatter()
        dateFormatter.locale = Locale(identifier: "zh_CN")

        for record in records {
            let date = record.creationDate
            let label: String

            if date >= startOfToday {
                label = "今天"
            } else if date >= startOfYesterday {
                label = "昨天"
            } else if date >= startOfWeek {
                let weekday = calendar.component(.weekday, from: date)
                let weekdayNames = ["", "周日", "周一", "周二", "周三", "周四", "周五", "周六"]
                label = "本周" + (weekdayNames[safe: weekday] ?? "")
            } else if date >= startOfMonth {
                dateFormatter.dateFormat = "M月d日"
                label = dateFormatter.string(from: date)
            } else {
                dateFormatter.dateFormat = "yyyy年M月"
                label = dateFormatter.string(from: date)
            }

            groups[label, default: []].append(record)
        }

        return groups
    }

    // MARK: - 按位置分组

    /// 按位置分组照片（基于位置元数据）
    /// - Parameter records: 照片记录列表
    /// - Returns: 位置标签到照片记录列表的映射
    func groupByLocation(_ records: [PhotoRecord]) -> [String: [PhotoRecord]] {
        var groups: [String: [PhotoRecord]] = [:]
        let storage = PhotoStorageService.shared

        for record in records {
            guard let url = storage.photoURL(for: record.id),
                  let data = try? Data(contentsOf: url),
                  let source = CGImageSourceCreateWithData(data as CFData, nil),
                  let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [String: Any],
                  let gps = props[kCGImagePropertyGPSDictionary as String] as? [String: Any] else {
                groups["未知位置", default: []].append(record)
                continue
            }

            let label = extractLocationLabel(from: gps)
            groups[label, default: []].append(record)
        }

        return groups
    }

    /// 从 GPS 字典提取位置标签
    private func extractLocationLabel(from gps: [String: Any]) -> String {
        // 尝试从 GPS 坐标反推城市名
        if let lat = gps[kCGImagePropertyGPSLatitude as String] as? Double,
           let lon = gps[kCGImagePropertyGPSLongitude as String] as? Double {
            let latRef = gps[kCGImagePropertyGPSLatitudeRef as String] as? String ?? "N"
            let lonRef = gps[kCGImagePropertyGPSLongitudeRef as String] as? String ?? "E"
            let latitude = latRef == "S" ? -lat : lat
            let longitude = lonRef == "W" ? -lon : lon

            return coordinateToRegionLabel(lat: latitude, lon: longitude)
        }
        return "未知位置"
    }

    /// 将坐标映射到中国城市区域
    private func coordinateToRegionLabel(lat: Double, lon: Double) -> String {
        // 简化的中国城市坐标映射
        let cities: [(name: String, latRange: ClosedRange<Double>, lonRange: ClosedRange<Double>)] = [
            ("北京", 39.4...41.0, 115.4...117.5),
            ("上海", 30.7...31.5, 120.8...122.0),
            ("广州", 22.5...23.5, 112.9...114.0),
            ("深圳", 22.4...22.8, 113.7...114.6),
            ("杭州", 29.8...30.5, 119.7...120.8),
            ("成都", 30.0...31.0, 103.5...104.5),
            ("南京", 31.5...32.5, 118.3...119.2),
            ("武汉", 30.0...31.0, 113.7...115.0),
            ("西安", 33.8...34.6, 108.5...109.5),
            ("重庆", 29.0...30.0, 106.0...107.0),
            ("苏州", 30.8...31.5, 120.3...121.0),
            ("厦门", 24.2...24.6, 117.8...118.3),
            ("青岛", 35.8...36.5, 120.0...120.8),
            ("大连", 38.5...39.2, 121.0...122.0),
            ("三亚", 18.0...18.5, 109.0...109.8),
            ("香港", 22.1...22.5, 114.0...114.4),
            ("澳门", 22.0...22.2, 113.4...113.6),
            ("台北", 24.9...25.2, 121.4...121.6),
            ("哈尔滨", 45.3...46.0, 126.0...127.0),
            ("昆明", 24.5...25.5, 102.0...103.0),
            ("拉萨", 29.3...30.0, 90.5...91.5),
            ("乌鲁木齐", 43.3...44.2, 87.0...88.0),
            ("海口", 19.5...20.2, 110.0...110.5),
            ("桂林", 24.5...25.5, 110.0...110.8),
            ("丽江", 26.5...27.5, 100.0...100.5),
            ("张家界", 28.5...29.5, 110.0...111.0)
        ]

        for city in cities {
            if city.latRange.contains(lat) && city.lonRange.contains(lon) {
                return city.name
            }
        }

        // 粗略的中国区域
        if lat > 35 && lon > 100 { return "中国北方" }
        if lat <= 35 && lon > 100 { return "中国南方" }
        if lon <= 100 { return "中国西部" }

        return String(format: "%.2f°, %.2f°", lat, lon)
    }

    // MARK: - 按人脸分组

    /// 按人脸聚类分组照片
    /// - Parameter records: 照片记录列表
    /// - Returns: 人脸聚类分组（每个内层数组是一组相似人脸的照片）
    func groupByFaces(_ records: [PhotoRecord]) -> [[PhotoRecord]] {
        let storage = PhotoStorageService.shared
        var faceGroups: [[(record: PhotoRecord, faceObservations: [VNFaceObservation])]] = []
        let faceDetectionQueue = DispatchQueue(label: "livecapture.album.faces", qos: .userInitiated)

        let group = DispatchGroup()
        let lock = NSLock()

        for record in records {
            guard let url = storage.photoURL(for: record.id),
                  let data = try? Data(contentsOf: url),
                  let uiImage = UIImage(data: data),
                  let cgImage = uiImage.cgImage else { continue }

            group.enter()
            faceDetectionQueue.async {
                defer { group.leave() }

                let request = VNDetectFaceRectanglesRequest()
                // 同时获取面部特征点用于比对
                let landmarksRequest = VNDetectFaceLandmarksRequest()

                let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
                do {
                    try handler.perform([request, landmarksRequest])
                } catch {
                    return
                }

                let faces = request.results ?? []
                let landmarks = landmarksRequest.results ?? []

                if !faces.isEmpty {
                    lock.lock()
                    faceGroups.append([(record: record, faceObservations: faces)])
                    lock.unlock()
                }
            }
        }

        group.wait()

        // 如果有多个包含人脸的照片，尝试合并相似的人脸组
        // 简化实现：按人脸数量分组
        if faceGroups.count <= 1 {
            return faceGroups.map { $0.map { $0.record } }
        }

        // 按人脸数量聚类
        var byFaceCount: [Int: [PhotoRecord]] = [:]
        for group in faceGroups {
            for (record, faces) in group {
                let count = faces.count
                byFaceCount[count, default: []].append(record)
            }
        }

        return Array(byFaceCount.values)
    }
}

// MARK: - Array 安全下标

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

#endif