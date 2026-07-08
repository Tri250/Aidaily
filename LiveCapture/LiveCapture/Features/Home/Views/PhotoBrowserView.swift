//
//  PhotoBrowserView.swift
//  LiveCapture
//
//  照片浏览器：EXIF 查看、左右滑动切换、双击放大、捏合退出、导出卡片
//

import SwiftUI
import Photos

#if os(iOS)

struct PhotoBrowserView: View {
    let records: [PhotoRecord]
    @State var currentIndex: Int
    let photoProvider: (UUID) -> UIImage?

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var showEXIF = false
    @State private var showDeleteConfirm = false
    @State private var dragOffset: CGSize = .zero
    @State private var isDragging = false

    // 导出
    @State private var showExportSheet = false
    @State private var cardImage: UIImage?
    @State private var isGenerating = false
    @State private var saveSuccess = false
    @State private var loadedPhotos: [UUID: UIImage] = [:]

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            // 照片滑动
            TabView(selection: $currentIndex) {
                ForEach(Array(records.enumerated()), id: \.element.id) { index, record in
                    Group {
                        if let image = loadedPhotos[record.id] ?? photoProvider(record.id) {
                            GeometryReader { geo in
                                Image(uiImage: image)
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .scaleEffect(scale)
                                    .offset(offset)
                                    .gesture(
                                        SimultaneousGesture(
                                            TapGesture(count: 2).onEnded {
                                                withAnimation(DesignSystem.Animation.bouncy) {
                                                    if scale > 1.0 {
                                                        scale = 1.0
                                                        offset = .zero
                                                    } else {
                                                        scale = 3.0
                                                    }
                                                }
                                            },
                                            MagnificationGesture()
                                                .onChanged { value in
                                                    scale = max(1.0, min(5.0, lastScale * value))
                                                }
                                                .onEnded { _ in
                                                    lastScale = scale
                                                    if scale <= 1.0 {
                                                        withAnimation(DesignSystem.Animation.bouncy) {
                                                            scale = 1.0
                                                            offset = .zero
                                                        }
                                                    }
                                                }
                                        )
                                    )
                                    .gesture(
                                        DragGesture()
                                            .onChanged { value in
                                                if scale <= 1.0 {
                                                    dragOffset = value.translation
                                                    isDragging = true
                                                } else {
                                                    offset = CGSize(
                                                        width: offset.width + value.translation.width,
                                                        height: offset.height + value.translation.height
                                                    )
                                                }
                                            }
                                            .onEnded { value in
                                                if scale <= 1.0 {
                                                    if abs(value.translation.height) > 120 {
                                                        dismiss()
                                                    } else {
                                                        withAnimation(DesignSystem.Animation.bouncy) {
                                                            dragOffset = .zero
                                                        }
                                                    }
                                                    isDragging = false
                                                }
                                            }
                                    )
                                    .offset(dragOffset)
                                    .opacity(scale <= 1.0 ? 1.0 - abs(dragOffset.height) / 300 : 1.0)
                            }
                            .tag(index)
                            .onAppear {
                                if loadedPhotos[record.id] == nil {
                                    loadedPhotos[record.id] = image
                                }
                            }
                        } else {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(1.5)
                                .tag(index)
                                .onAppear {
                                    if let img = photoProvider(record.id) {
                                        loadedPhotos[record.id] = img
                                    }
                                }
                        }
                    }
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .ignoresSafeArea()

            // 顶部栏
            VStack {
                HStack {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(10)
                            .background(Circle().fill(Color.black.opacity(0.4)))
                    }
                    .accessibilityLabel("关闭")

                    Spacer()

                    Text("\(currentIndex + 1) / \(records.count)")
                        .font(DesignSystem.Typography.monoCaption)
                        .foregroundColor(.white)

                    Spacer()

                    // 导出按钮
                    Button {
                        generateExportCard()
                    } label: {
                        if isGenerating {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Image(systemName: "square.and.arrow.down")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                                .padding(10)
                                .background(Circle().fill(Color.black.opacity(0.4)))
                        }
                    }
                    .disabled(isGenerating)
                    .accessibilityLabel("导出分享卡片")

                    Button {
                        HapticManager.shared.light()
                        withAnimation(DesignSystem.Animation.smooth) {
                            showEXIF.toggle()
                        }
                    } label: {
                        Image(systemName: "info.circle")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(10)
                            .background(Circle().fill(Color.black.opacity(0.4)))
                    }
                    .accessibilityLabel("查看照片信息")

                    Button {
                        HapticManager.shared.medium()
                        showDeleteConfirm = true
                    } label: {
                        Image(systemName: "trash")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(DesignSystem.Colors.error)
                            .padding(10)
                            .background(Circle().fill(Color.black.opacity(0.4)))
                    }
                    .accessibilityLabel("删除照片")
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .opacity(scale <= 1.0 ? 1 : 0)

                Spacer()

                // EXIF 面板
                if showEXIF, let record = records[safe: currentIndex] {
                    exifPanel(record: record)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }

            // 底部操作栏
            VStack {
                Spacer()
                if scale <= 1.0 {
                    // 元数据标签
                    if let record = records[safe: currentIndex] {
                        metadataSection(record)
                            .padding(.bottom, 8)
                    }

                    HStack(spacing: 40) {
                        Button {
                            if let image = loadedPhotos[records[currentIndex].id] ?? photoProvider(records[currentIndex].id) {
                                let activityVC = UIActivityViewController(activityItems: [image], applicationActivities: nil)
                                if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                                   let rootVC = windowScene.windows.first?.rootViewController {
                                    rootVC.present(activityVC, animated: true)
                                }
                            }
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: "square.and.arrow.up")
                                    .font(.title3)
                                Text("分享")
                                    .font(.system(size: 10))
                            }
                            .foregroundColor(.white)
                        }
                        .accessibilityLabel("分享照片")

                        Button {
                            // 编辑
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: "paintpalette")
                                    .font(.title3)
                                Text("编辑")
                                    .font(.system(size: 10))
                            }
                            .foregroundColor(.white)
                        }
                        .accessibilityLabel("编辑照片")

                        Button {
                            // 收藏
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: "heart")
                                    .font(.title3)
                                Text("收藏")
                                    .font(.system(size: 10))
                            }
                            .foregroundColor(.white)
                        }
                        .accessibilityLabel("收藏照片")
                    }
                    .padding(.bottom, 30)
                }
            }
        }
        .alert("删除照片", isPresented: $showDeleteConfirm) {
            Button("删除", role: .destructive) {
                PhotoStorageService.shared.deleteRecord(records[currentIndex].id)
                dismiss()
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("此照片将被移至最近删除")
        }
        .sheet(isPresented: $showExportSheet) {
            exportPreviewView
        }
        .statusBar(hidden: true)
    }

    // MARK: - EXIF 面板

    private func exifPanel(record: PhotoRecord) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 24) {
                exifItem(label: "ISO", value: record.iso.map { "\(Int($0))" } ?? "--")
                exifItem(label: "快门", value: record.shutterSpeed.map { formatShutterSpeed($0) } ?? "--")
                exifItem(label: "光圈", value: record.aperture.map { "f/\(String(format: "%.1f", $0))" } ?? "--")
                exifItem(label: "尺寸", value: "\(record.imageWidth ?? 0)×\(record.imageHeight ?? 0)")
                exifItem(label: "检测", value: record.detectionMethod ?? "--")
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(.ultraThinMaterial)
            )
            .padding(.horizontal, 12)
            .padding(.bottom, 20)
        }
    }

    private func exifItem(label: String, value: String) -> some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 10, weight: .medium))
                .foregroundColor(.white.opacity(0.5))
            Text(value)
                .font(DesignSystem.Typography.monoCaption)
                .foregroundColor(.white)
        }
    }

    // MARK: - Metadata Section

    private func metadataSection(_ record: PhotoRecord) -> some View {
        VStack(spacing: 6) {
            Text(formattedDate(record.creationDate))
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textTertiary)

            HStack(spacing: 16) {
                MetadataBadge(label: record.detectionMethod ?? "未知引擎")

                if let iso = record.iso {
                    MetadataBadge(label: "ISO \(Int(iso))")
                }
                if let shutter = record.shutterSpeed {
                    MetadataBadge(label: shutterDisplay(shutter))
                }
                if let aperture = record.aperture {
                    MetadataBadge(label: "f/\(String(format: "%.1f", aperture))")
                }
            }
        }
    }

    // MARK: - Export Preview

    private var exportPreviewView: some View {
        NavigationStack {
            VStack {
                if let cardImage {
                    VStack(spacing: 0) {
                        Image(uiImage: cardImage)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .padding(16)

                        VStack(spacing: 12) {
                            Button {
                                saveToPhotos(cardImage)
                            } label: {
                                HStack {
                                    Image(systemName: saveSuccess ? "checkmark.circle.fill" : "square.and.arrow.down")
                                    Text(saveSuccess ? "已保存" : "保存到相册")
                                }
                                .font(DesignSystem.Typography.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(saveSuccess ? DesignSystem.Colors.success : DesignSystem.Colors.primary)
                                )
                            }
                            .disabled(saveSuccess)
                            .padding(.horizontal, 16)

                            Text("图片将保存到系统相册")
                                .font(DesignSystem.Typography.caption2)
                                .foregroundColor(DesignSystem.Colors.textTertiary)
                        }
                        .padding(.bottom, 24)
                    }
                } else {
                    Spacer()
                    ProgressView("正在生成分享卡片...")
                    Spacer()
                }
            }
            .background(Color(uiColor: .systemBackground))
            .navigationTitle("导出预览")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") {
                        showExportSheet = false
                        saveSuccess = false
                    }
                }
            }
        }
    }

    // MARK: - Helpers

    private func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy年M月d日 HH:mm"
        return formatter.string(from: date)
    }

    private func shutterDisplay(_ speed: Double) -> String {
        if speed >= 1 { return "\(Int(speed))s" }
        else { return "1/\(Int(1.0 / speed))s" }
    }

    private func formatShutterSpeed(_ speed: Double) -> String {
        if speed >= 1.0 {
            return "\(Int(speed))\""
        } else {
            return "1/\(Int(1.0 / speed))"
        }
    }

    // MARK: - Export

    private func generateExportCard() {
        guard let record = records[safe: currentIndex] else { return }
        isGenerating = true
        showExportSheet = true

        let image = loadedPhotos[record.id] ?? photoProvider(record.id)
        if let photo = image {
            DispatchQueue.global(qos: .userInitiated).async {
                let card = ShareCardGenerator.generate(
                    photo: photo,
                    date: record.creationDate,
                    detectionMethod: record.detectionMethod,
                    iso: record.iso,
                    shutterSpeed: record.shutterSpeed,
                    aperture: record.aperture,
                    imageWidth: record.imageWidth,
                    imageHeight: record.imageHeight
                )
                DispatchQueue.main.async {
                    self.isGenerating = false
                    self.cardImage = card
                }
            }
        }
    }

    private func saveToPhotos(_ image: UIImage) {
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else { return }
            guard let pngData = image.pngData() else { return }
            PHPhotoLibrary.shared().performChanges({
                PHAssetCreationRequest.forAsset().addResource(with: .photo, data: pngData, options: nil)
            }) { success, _ in
                DispatchQueue.main.async {
                    if success {
                        saveSuccess = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                            showExportSheet = false
                            saveSuccess = false
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Metadata Badge

private struct MetadataBadge: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.system(size: 11, weight: .medium, design: .monospaced))
            .foregroundColor(DesignSystem.Colors.textTertiary)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(
                Capsule()
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

#endif