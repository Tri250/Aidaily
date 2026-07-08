//
//  PostCaptureFlow.swift
//  LiveCapture
//
//  拍照后快速预览流程 - 左下角缩略图 + 上滑分享 + 编辑/删除入口
//

import SwiftUI
import Photos

#if os(iOS)

struct PostCaptureFlow: View {
    let photoImage: UIImage?
    let record: PhotoRecord?
    let onShare: (UIImage) -> Void
    let onEdit: (PhotoRecord) -> Void
    let onDelete: () -> Void
    let onDismiss: () -> Void

    @State private var showPreview = false
    @State private var previewScale: CGFloat = 0.3
    @State private var dragOffset: CGSize = .zero
    @State private var showOptions = false

    var body: some View {
        Group {
            if let image = photoImage, let record = record {
                VStack {
                    Spacer()
                    HStack {
                        // 左下角缩略图
                        Button {
                            HapticManager.shared.light()
                            withAnimation(DesignSystem.Animation.bouncy) {
                                showPreview = true
                            }
                        } label: {
                            Image(uiImage: image)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 52, height: 52)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .strokeBorder(DesignSystem.Stroke.standard, lineWidth: 1)
                                )
                                .shadow(color: .black.opacity(0.3), radius: 4, x: 0, y: 2)
                        }
                        .accessibilityLabel("查看刚拍摄的照片")
                        .accessibilityHint("点击查看大图，上滑快速分享")

                        Spacer()
                    }
                    .padding(.leading, 16)
                    .padding(.bottom, 100)
                }
                .transition(.move(edge: .leading).combined(with: .opacity))

                // 全屏预览 + 操作
                if showPreview {
                    postCapturePreview(image: image, record: record)
                }
            }
        }
        .onChange(of: photoImage) { _, _ in
            // 新照片到来时重置状态
            showPreview = false
            showOptions = false
        }
    }

    // MARK: - 拍照后预览

    private func postCapturePreview(image: UIImage, record: PhotoRecord) -> some View {
        ZStack {
            Color.black.ignoresSafeArea()

            // 照片
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .scaleEffect(previewScale)
                .offset(dragOffset)
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            dragOffset = value.translation
                            if abs(value.translation.height) > 50 {
                                previewScale = 0.7
                            }
                        }
                        .onEnded { value in
                            if abs(value.translation.height) > 100 {
                                // 下滑关闭
                                withAnimation(DesignSystem.Animation.easeOut) {
                                    showPreview = false
                                    previewScale = 0.3
                                    dragOffset = .zero
                                }
                            } else if value.translation.height < -80 {
                                // 上滑分享
                                onShare(image)
                                showOptions = true
                            } else {
                                withAnimation(DesignSystem.Animation.bouncy) {
                                    previewScale = 1.0
                                    dragOffset = .zero
                                }
                            }
                        }
                )
                .onTapGesture {
                    HapticManager.shared.light()
                    withAnimation(DesignSystem.Animation.bouncy) {
                        showPreview = false
                        previewScale = 0.3
                        dragOffset = .zero
                    }
                }

            // 底部操作栏
            VStack {
                Spacer()
                HStack(spacing: 40) {
                    // 编辑
                    postCaptureButton(icon: "paintpalette", label: "编辑") {
                        showPreview = false
                        onEdit(record)
                    }

                    // 分享
                    postCaptureButton(icon: "square.and.arrow.up", label: "分享") {
                        onShare(image)
                    }

                    // 删除
                    postCaptureButton(icon: "trash", label: "删除", isDestructive: true) {
                        showPreview = false
                        onDelete()
                    }
                }
                .padding(.bottom, 40)
            }
            .transition(.move(edge: .bottom))

            // 关闭按钮
            VStack {
                HStack {
                    Button {
                        withAnimation(DesignSystem.Animation.easeOut) {
                            showPreview = false
                            previewScale = 0.3
                        }
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title2)
                            .foregroundColor(.white.opacity(0.7))
                    }
                    .padding(.leading, 20)
                    .padding(.top, 16)
                    Spacer()
                }
                Spacer()
            }
        }
        .transition(.opacity)
        .zIndex(100)
    }

    private func postCaptureButton(icon: String, label: String, isDestructive: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: {
            HapticManager.shared.medium()
            action()
        }) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .medium))
                Text(label)
                    .font(.system(size: 11, weight: .medium))
            }
            .foregroundColor(isDestructive ? DesignSystem.Colors.error : .white)
            .frame(width: 56, height: 56)
            .background(
                Circle()
                    .fill(isDestructive ? DesignSystem.Colors.error.opacity(0.15) : Color.white.opacity(0.1))
            )
        }
        .accessibilityLabel(label)
    }
}

// MARK: - View Extension

extension View {
    func postCaptureFlow(
        image: UIImage?,
        record: PhotoRecord?,
        onShare: @escaping (UIImage) -> Void,
        onEdit: @escaping (PhotoRecord) -> Void,
        onDelete: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) -> some View {
        overlay(
            PostCaptureFlow(
                photoImage: image,
                record: record,
                onShare: onShare,
                onEdit: onEdit,
                onDelete: onDelete,
                onDismiss: onDismiss
            )
        )
    }
}

#endif