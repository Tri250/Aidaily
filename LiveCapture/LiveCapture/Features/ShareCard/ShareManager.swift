import UIKit
import Photos
import SwiftUI

// MARK: - WeChat Scene

enum WeChatScene {
    case session   // 微信好友
    case timeline  // 朋友圈
}

// MARK: - Share Platform

enum SharePlatform {
    case wechatSession
    case wechatTimeline
    case weibo
    case xiaohongshu
    case system
    case saveToPhotos
}

// MARK: - Share Result

enum ShareResult {
    case success
    case cancelled
    case failure(Error)
}

// MARK: - Share Manager

final class ShareManager {

    // MARK: - Singleton

    static let shared = ShareManager()

    private init() {}

    // MARK: - System Share

    func sharePhoto(
        image: UIImage,
        from viewController: UIViewController,
        completion: ((ShareResult) -> Void)? = nil
    ) {
        let activityVC = UIActivityViewController(
            activityItems: [image],
            applicationActivities: nil
        )

        activityVC.completionWithItemsHandler = { _, completed, _, error in
            if let error {
                completion?(.failure(error))
            } else if completed {
                completion?(.success)
            } else {
                completion?(.cancelled)
            }
        }

        if let popover = activityVC.popoverPresentationController {
            popover.sourceView = viewController.view
            popover.sourceRect = CGRect(
                x: viewController.view.bounds.midX,
                y: viewController.view.bounds.midY,
                width: 0,
                height: 0
            )
            popover.permittedArrowDirections = []
        }

        viewController.present(activityVC, animated: true)
    }

    // MARK: - WeChat

    func shareToWeChat(image: UIImage, scene: WeChatScene, completion: ((ShareResult) -> Void)? = nil) {
        guard let imageData = image.pngData() else {
            completion?(.failure(ShareError.imageConversionFailed))
            return
        }

        let urlScheme: String = {
            switch scene {
            case .session: return "weixin://"
            case .timeline: return "weixin://dl/moments"
            }
        }()

        // Check if WeChat is installed
        guard let url = URL(string: urlScheme),
              UIApplication.shared.canOpenURL(url) else {
            // Fallback: show system share sheet
            shareViaSystemFallback(image: image, completion: completion)
            return
        }

        // WeChat doesn't support direct image sharing via URL scheme without SDK.
        // Use UIPasteboard + URL scheme as a workaround, then fallback to system share.
        shareViaSystemFallback(image: image, completion: completion)
    }

    // MARK: - Weibo

    func shareToWeibo(image: UIImage, completion: ((ShareResult) -> Void)? = nil) {
        guard let imageData = image.pngData() else {
            completion?(.failure(ShareError.imageConversionFailed))
            return
        }

        guard let url = URL(string: "weibo://"),
              UIApplication.shared.canOpenURL(url) else {
            shareViaSystemFallback(image: image, completion: completion)
            return
        }

        // Weibo URL scheme does not support direct image transfer.
        // Open Weibo and use pasteboard as fallback.
        UIPasteboard.general.image = image
        UIApplication.shared.open(url) { success in
            if success {
                completion?(.success)
            } else {
                completion?(.failure(ShareError.appNotInstalled("微博")))
            }
        }
    }

    // MARK: - Xiaohongshu

    func shareToXiaohongshu(image: UIImage, completion: ((ShareResult) -> Void)? = nil) {
        guard let url = URL(string: "xhsdiscover://"),
              UIApplication.shared.canOpenURL(url) else {
            completion?(.failure(ShareError.appNotInstalled("小红书")))
            return
        }

        UIPasteboard.general.image = image
        UIApplication.shared.open(url) { success in
            if success {
                completion?(.success)
            } else {
                completion?(.failure(ShareError.appNotInstalled("小红书")))
            }
        }
    }

    // MARK: - Save to Photos

    func saveToPhotos(image: UIImage, completion: ((ShareResult) -> Void)? = nil) {
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else {
                DispatchQueue.main.async {
                    completion?(.failure(ShareError.photoLibraryAccessDenied))
                }
                return
            }

            guard let pngData = image.pngData() else { return }
            PHPhotoLibrary.shared().performChanges {
                PHAssetCreationRequest.forAsset().addResource(
                    with: .photo,
                    data: pngData,
                    options: nil
                )
            } completionHandler: { success, error in
                DispatchQueue.main.async {
                    if success {
                        completion?(.success)
                    } else if let error {
                        completion?(.failure(error))
                    } else {
                        completion?(.failure(ShareError.unknown))
                    }
                }
            }
        }
    }

    // MARK: - Check App Installation

    func isAppInstalled(_ platform: SharePlatform) -> Bool {
        guard let url = URL(string: scheme(for: platform)) else { return false }
        return UIApplication.shared.canOpenURL(url)
    }

    private func scheme(for platform: SharePlatform) -> String {
        switch platform {
        case .wechatSession, .wechatTimeline: return "weixin://"
        case .weibo: return "weibo://"
        case .xiaohongshu: return "xhsdiscover://"
        case .system, .saveToPhotos: return ""
        }
    }

    // MARK: - Private Helpers

    private func shareViaSystemFallback(
        image: UIImage,
        completion: ((ShareResult) -> Void)? = nil
    ) {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else {
            completion?(.failure(ShareError.noViewController))
            return
        }

        var topVC = rootVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        sharePhoto(image: image, from: topVC, completion: completion)
    }
}

// MARK: - Share Error

enum ShareError: LocalizedError {
    case imageConversionFailed
    case appNotInstalled(String)
    case photoLibraryAccessDenied
    case noViewController
    case unknown

    var errorDescription: String? {
        switch self {
        case .imageConversionFailed:
            return "图片转换失败"
        case .appNotInstalled(let name):
            return "未安装\(name)"
        case .photoLibraryAccessDenied:
            return "相册访问权限被拒绝，请在设置中开启"
        case .noViewController:
            return "无法获取当前界面"
        case .unknown:
            return "未知错误"
        }
    }
}