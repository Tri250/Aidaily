//
//  VideoEditor.swift
//  LiveCapture
//
//  视频编辑器 - 裁剪、变速、导出
//

import Foundation
import AVFoundation
import UIKit

#if os(iOS)

/// 视频编辑器
final class VideoEditor: ObservableObject {
    @Published var isProcessing = false
    @Published var progress: Float = 0
    @Published var errorMessage: String?

    // MARK: - 视频裁剪

    /// 裁剪视频时间范围
    func trimVideo(sourceURL: URL, startTime: CMTime, endTime: CMTime, completion: @escaping (URL?) -> Void) {
        isProcessing = true
        progress = 0

        let asset = AVAsset(url: sourceURL)
        let exportSession = AVAssetExportSession(asset: asset, presetName: AVAssetExportPresetHighestQuality)

        let outputURL = tempOutputURL(suffix: "_trimmed")
        exportSession?.outputURL = outputURL
        exportSession?.outputFileType = .mp4
        exportSession?.timeRange = CMTimeRange(start: startTime, end: endTime)

        exportSession?.exportAsynchronously { [weak self] in
            DispatchQueue.main.async {
                self?.isProcessing = false
                self?.progress = 1.0
                if exportSession?.status == .completed {
                    completion(outputURL)
                } else {
                    self?.errorMessage = exportSession?.error?.localizedDescription ?? "裁剪失败"
                    completion(nil)
                }
            }
        }

        // 进度追踪
        Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] timer in
            DispatchQueue.main.async {
                self?.progress = exportSession?.progress ?? 0
                if exportSession?.status != .exporting {
                    timer.invalidate()
                }
            }
        }
    }

    // MARK: - 视频变速

    /// 调整视频速度
    func adjustSpeed(sourceURL: URL, speed: Float, completion: @escaping (URL?) -> Void) {
        isProcessing = true
        progress = 0

        let asset = AVAsset(url: sourceURL)
        let composition = AVMutableComposition()

        guard let videoTrack = asset.tracks(withMediaType: .video).first,
              let audioTrack = asset.tracks(withMediaType: .audio).first,
              let compositionVideoTrack = composition.addMutableTrack(
                withMediaType: .video,
                preferredTrackID: kCMPersistentTrackID_Invalid
              ),
              let compositionAudioTrack = composition.addMutableTrack(
                withMediaType: .audio,
                preferredTrackID: kCMPersistentTrackID_Invalid
              ) else {
            isProcessing = false
            errorMessage = "无法读取视频轨道"
            completion(nil)
            return
        }

        let duration = asset.duration
        let scaledDuration = CMTimeMultiplyByFloat64(duration, multiplier: Float64(1.0 / speed))

        try? compositionVideoTrack.insertTimeRange(
            CMTimeRange(start: .zero, duration: duration),
            of: videoTrack,
            at: .zero
        )
        try? compositionAudioTrack.insertTimeRange(
            CMTimeRange(start: .zero, duration: duration),
            of: audioTrack,
            at: .zero
        )

        compositionVideoTrack.scaleTimeRange(
            CMTimeRange(start: .zero, duration: duration),
            toDuration: scaledDuration
        )
        compositionAudioTrack.scaleTimeRange(
            CMTimeRange(start: .zero, duration: duration),
            toDuration: scaledDuration
        )

        let exportSession = AVAssetExportSession(asset: composition, presetName: AVAssetExportPresetHighestQuality)
        let outputURL = tempOutputURL(suffix: "_speed\(speed)")
        exportSession?.outputURL = outputURL
        exportSession?.outputFileType = .mp4

        exportSession?.exportAsynchronously { [weak self] in
            DispatchQueue.main.async {
                self?.isProcessing = false
                self?.progress = 1.0
                if exportSession?.status == .completed {
                    completion(outputURL)
                } else {
                    self?.errorMessage = exportSession?.error?.localizedDescription ?? "变速处理失败"
                    completion(nil)
                }
            }
        }
    }

    // MARK: - 视频信息

    /// 获取视频时长
    func videoDuration(url: URL) -> CMTime {
        let asset = AVAsset(url: url)
        return asset.duration
    }

    /// 生成视频缩略图
    func generateThumbnail(url: URL, at time: CMTime = .zero) -> UIImage? {
        let asset = AVAsset(url: url)
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.maximumSize = CGSize(width: 480, height: 360)

        do {
            let cgImage = try generator.copyCGImage(at: time, actualTime: nil)
            return UIImage(cgImage: cgImage)
        } catch {
            LiveCaptureLogger.shared.error("VideoEditor error: \(error)")
            return nil
        }
    }

    // MARK: - 保存到相册

    func saveToPhotoLibrary(url: URL, completion: @escaping (Bool) -> Void) {
        PHPhotoLibrary.requestAuthorization { status in
            guard status == .authorized || status == .limited else {
                completion(false)
                return
            }
            PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: url)
            } completionHandler: { success, _ in
                completion(success)
            }
        }
    }

    // MARK: - Helper

    private func tempOutputURL(suffix: String) -> URL {
        let tempDir = FileManager.default.temporaryDirectory
        let fileName = "LiveCapture_\(suffix)_\(Date().timeIntervalSince1970).mp4"
        return tempDir.appendingPathComponent(fileName)
    }
}

#endif