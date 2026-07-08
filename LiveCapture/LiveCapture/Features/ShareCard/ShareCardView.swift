import SwiftUI
import Photos

#if os(iOS)

/// Bottom sheet view for sharing photos with various platforms.
struct ShareCardView: View {
    let photo: UIImage
    let record: PhotoRecord

    @Environment(\.dismiss) private var dismiss

    @State private var selectedStyle: ShareCardStyle = .minimal
    @State private var cardImage: UIImage?
    @State private var isGenerating = false
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var toastStyle: ToastStyle = .success
    @State private var showUnsavedAlert = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Drag indicator
                dragIndicator
                    .padding(.top, 8)

                // Card preview
                cardPreview
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                // Style picker
                ShareCardStylePicker(
                    selectedStyle: $selectedStyle,
                    photo: photo,
                    record: record
                )
                .padding(.top, 16)
                .onChange(of: selectedStyle) { _, _ in
                    regenerateCard()
                }

                // Platform buttons
                platformButtons
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 32)
            }
            .background(DesignSystem.Colors.backgroundPrimary)
            .navigationTitle("分享")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") {
                        dismiss()
                    }
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.textSecondary)
                }
            }
            .toast(isShowing: $showToast, message: toastMessage, style: toastStyle)
            .alert("提示", isPresented: $showUnsavedAlert) {
                Button("好的", role: .cancel) {}
            } message: {
                Text("未安装小红书 App，请先安装后再试。")
            }
            .onAppear {
                generateCard()
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.hidden)
    }

    // MARK: - Drag Indicator

    private var dragIndicator: some View {
        RoundedRectangle(cornerRadius: 2.5)
            .fill(DesignSystem.Colors.gray3)
            .frame(width: 36, height: 5)
    }

    // MARK: - Card Preview

    private var cardPreview: some View {
        VStack(spacing: 0) {
            if let cardImage {
                Image(uiImage: cardImage)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large))
                    .overlay(
                        RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                            .strokeBorder(DesignSystem.Colors.gray3, lineWidth: 1)
                    )
                    .elevatedShadow()
            } else {
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                    .fill(DesignSystem.Colors.backgroundSecondary)
                    .aspectRatio(3.0 / 4.0, contentMode: .fit)
                    .overlay {
                        VStack(spacing: 12) {
                            if isGenerating {
                                ProgressView()
                                    .scaleEffect(1.2)
                                Text("正在生成分享卡片...")
                                    .font(DesignSystem.Typography.caption1)
                                    .foregroundColor(DesignSystem.Colors.textTertiary)
                            } else {
                                Image(systemName: "photo.artframe")
                                    .font(.system(size: 36))
                                    .foregroundColor(DesignSystem.Colors.textTertiary)
                            }
                        }
                    }
            }
        }
        .padding(.horizontal, 16)
    }

    // MARK: - Platform Buttons

    private var platformButtons: some View {
        VStack(spacing: 16) {
            // Row 1: WeChat & Weibo
            HStack(spacing: 12) {
                shareButton(
                    icon: "message.fill",
                    color: Color(red: 0.18, green: 0.74, blue: 0.31),
                    label: "微信好友",
                    action: { shareToWeChatSession() }
                )

                shareButton(
                    icon: "circle.grid.2x2.fill",
                    color: Color(red: 0.18, green: 0.74, blue: 0.31),
                    label: "朋友圈",
                    action: { shareToWeChatTimeline() }
                )

                shareButton(
                    icon: "flame.fill",
                    color: Color(red: 0.90, green: 0.22, blue: 0.22),
                    label: "微博",
                    action: { shareToWeibo() }
                )

                shareButton(
                    icon: "heart.fill",
                    color: Color(red: 0.94, green: 0.22, blue: 0.22),
                    label: "小红书",
                    action: { shareToXiaohongshu() }
                )
            }

            // Row 2: Save & More
            HStack(spacing: 12) {
                shareButton(
                    icon: "square.and.arrow.down",
                    color: DesignSystem.Colors.primary,
                    label: "保存图片",
                    action: { saveToPhotos() }
                )

                shareButton(
                    icon: "ellipsis",
                    color: DesignSystem.Colors.gray4,
                    label: "更多",
                    action: { shareViaSystem() }
                )

                Spacer(minLength: 0)
            }
        }
    }

    private func shareButton(
        icon: String,
        color: Color,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(color.opacity(0.12))
                        .frame(width: 52, height: 52)

                    Image(systemName: icon)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(color)
                }

                Text(label)
                    .font(DesignSystem.Typography.caption2)
                    .foregroundColor(DesignSystem.Colors.textSecondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
        .pressScale()
    }

    // MARK: - Card Generation

    private func generateCard() {
        guard !isGenerating else { return }
        isGenerating = true

        DispatchQueue.global(qos: .userInitiated).async {
            let card = ShareCardGenerator.generate(
                photo: photo,
                style: selectedStyle,
                date: record.creationDate,
                detectionMethod: record.detectionMethod,
                iso: record.iso,
                shutterSpeed: record.shutterSpeed,
                aperture: record.aperture,
                imageWidth: record.imageWidth,
                imageHeight: record.imageHeight
            )

            DispatchQueue.main.async {
                self.cardImage = card
                self.isGenerating = false
            }
        }
    }

    private func regenerateCard() {
        guard !isGenerating else { return }
        cardImage = nil
        generateCard()
    }

    /// Returns the current card image, or the original photo if card is not yet generated.
    private var shareImage: UIImage {
        cardImage ?? photo
    }

    // MARK: - Share Actions

    private func shareToWeChatSession() {
        ShareManager.shared.shareToWeChat(image: shareImage, scene: .session) { result in
            handleResult(result, platform: "微信好友")
        }
    }

    private func shareToWeChatTimeline() {
        ShareManager.shared.shareToWeChat(image: shareImage, scene: .timeline) { result in
            handleResult(result, platform: "朋友圈")
        }
    }

    private func shareToWeibo() {
        ShareManager.shared.shareToWeibo(image: shareImage) { result in
            handleResult(result, platform: "微博")
        }
    }

    private func shareToXiaohongshu() {
        ShareManager.shared.shareToXiaohongshu(image: shareImage) { result in
            switch result {
            case .failure(let error):
                if let shareError = error as? ShareError,
                   case .appNotInstalled = shareError {
                    showUnsavedAlert = true
                } else {
                    handleResult(result, platform: "小红书")
                }
            case .success:
                handleResult(result, platform: "小红书")
            case .cancelled:
                break
            }
        }
    }

    private func shareViaSystem() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else {
            showToastMessage("无法打开分享", style: .error)
            return
        }

        var topVC = rootVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        ShareManager.shared.sharePhoto(image: shareImage, from: topVC) { result in
            handleResult(result, platform: "系统分享")
        }
    }

    private func saveToPhotos() {
        ShareManager.shared.saveToPhotos(image: shareImage) { result in
            switch result {
            case .success:
                showToastMessage("已保存到相册", style: .success)
            case .failure(let error):
                showToastMessage(error.localizedDescription, style: .error)
            case .cancelled:
                break
            }
        }
    }

    // MARK: - Helpers

    private func handleResult(_ result: ShareResult, platform: String) {
        DispatchQueue.main.async {
            switch result {
            case .success:
                showToastMessage("已分享到\(platform)", style: .success)
            case .failure(let error):
                showToastMessage(error.localizedDescription, style: .error)
            case .cancelled:
                break
            }
        }
    }

    private func showToastMessage(_ message: String, style: ToastStyle) {
        toastMessage = message
        toastStyle = style
        withAnimation(DesignSystem.Animation.bouncy) {
            showToast = true
        }
    }
}

#endif