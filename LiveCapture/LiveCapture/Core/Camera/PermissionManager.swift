//
//  PermissionManager.swift
//  LiveCapture
//
//  统一权限管理中心 - 负责所有系统权限的检查、请求和引导
//

import Foundation
import AVFoundation
import Photos
import CoreLocation
import UIKit

#if os(iOS)

/// 统一权限管理中心
final class PermissionManager: ObservableObject {
    static let shared = PermissionManager()

    // MARK: - 权限状态

    @Published var cameraStatus: PermissionStatus = .notDetermined
    @Published var photoLibraryStatus: PermissionStatus = .notDetermined
    @Published var microphoneStatus: PermissionStatus = .notDetermined
    @Published var locationStatus: PermissionStatus = .notDetermined

    enum PermissionStatus: String {
        case authorized
        case notDetermined
        case denied
        case restricted

        var isGranted: Bool { self == .authorized }
        var needsGuide: Bool { self == .denied || self == .restricted }
    }

    private init() {
        refreshAllStatus()
    }

    // MARK: - 刷新所有权限状态

    func refreshAllStatus() {
        refreshCameraStatus()
        refreshPhotoLibraryStatus()
        refreshMicrophoneStatus()
        refreshLocationStatus()
    }

    // MARK: - 相机权限

    func refreshCameraStatus() {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        cameraStatus = mapStatus(status)
    }

    func requestCamera(completion: @escaping (Bool) -> Void) {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            cameraStatus = .authorized
            completion(true)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    self?.cameraStatus = granted ? .authorized : .denied
                    completion(granted)
                }
            }
        case .denied, .restricted:
            cameraStatus = mapStatus(AVCaptureDevice.authorizationStatus(for: .video))
            completion(false)
        @unknown default:
            completion(false)
        }
    }

    // MARK: - 相册权限

    func refreshPhotoLibraryStatus() {
        let status: PHAuthorizationStatus
        if #available(iOS 14, *) {
            status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        } else {
            status = PHPhotoLibrary.authorizationStatus()
        }
        photoLibraryStatus = mapPhotoStatus(status)
    }

    func requestPhotoLibrary(completion: @escaping (Bool) -> Void) {
        let handler: (PHAuthorizationStatus) -> Void = { [weak self] status in
            DispatchQueue.main.async {
                self?.photoLibraryStatus = self?.mapPhotoStatus(status) ?? .denied
                completion(status == .authorized || status == .limited)
            }
        }

        if #available(iOS 14, *) {
            PHPhotoLibrary.requestAuthorization(for: .readWrite, handler: handler)
        } else {
            PHPhotoLibrary.requestAuthorization(handler)
        }
    }

    // MARK: - 麦克风权限

    func refreshMicrophoneStatus() {
        let status = AVAudioSession.sharedInstance().recordPermission
        microphoneStatus = mapRecordPermission(status)
    }

    func requestMicrophone(completion: @escaping (Bool) -> Void) {
        switch AVAudioSession.sharedInstance().recordPermission {
        case .granted:
            microphoneStatus = .authorized
            completion(true)
        case .undetermined:
            AVAudioSession.sharedInstance().requestRecordPermission { [weak self] granted in
                DispatchQueue.main.async {
                    self?.microphoneStatus = granted ? .authorized : .denied
                    completion(granted)
                }
            }
        case .denied:
            microphoneStatus = .denied
            completion(false)
        @unknown default:
            completion(false)
        }
    }

    // MARK: - 位置权限

    func refreshLocationStatus() {
        let status = CLLocationManager().authorizationStatus
        locationStatus = mapLocationStatus(status)
    }

    // MARK: - 引导到系统设置

    func openSystemSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
    }

    // MARK: - 批量权限请求（用于相机启动）

    func requestCameraPermissions(completion: @escaping (Bool) -> Void) {
        requestCamera { [weak self] cameraGranted in
            guard cameraGranted else {
                completion(false)
                return
            }
            self?.requestPhotoLibrary { _ in
                // 相册权限不影响相机启动
                completion(true)
            }
        }
    }

    // MARK: - 映射工具

    private func mapStatus(_ status: AVAuthorizationStatus) -> PermissionStatus {
        switch status {
        case .authorized: return .authorized
        case .notDetermined: return .notDetermined
        case .denied: return .denied
        case .restricted: return .restricted
        @unknown default: return .denied
        }
    }

    private func mapPhotoStatus(_ status: PHAuthorizationStatus) -> PermissionStatus {
        switch status {
        case .authorized, .limited: return .authorized
        case .notDetermined: return .notDetermined
        case .denied: return .denied
        case .restricted: return .restricted
        @unknown default: return .denied
        }
    }

    private func mapRecordPermission(_ permission: AVAudioSession.RecordPermission) -> PermissionStatus {
        switch permission {
        case .granted: return .authorized
        case .denied: return .denied
        case .undetermined: return .notDetermined
        @unknown default: return .denied
        }
    }

    private func mapLocationStatus(_ status: CLAuthorizationStatus) -> PermissionStatus {
        switch status {
        case .authorizedAlways, .authorizedWhenInUse: return .authorized
        case .notDetermined: return .notDetermined
        case .denied: return .denied
        case .restricted: return .restricted
        @unknown default: return .denied
        }
    }
}

#endif