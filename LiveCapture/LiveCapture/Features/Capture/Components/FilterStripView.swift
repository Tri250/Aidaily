//
//  FilterStripView.swift
//  LiveCapture
//
//  水平滚动滤镜条 - 分类切换 + 滤镜预览 + 强度滑块 + 快速对比
//
//  ## 功能
//  - 分类标签横向滚动切换
//  - 滤镜缩略图实时预览
//  - 选中滤镜高亮显示
//  - 滤镜强度滑块（选中滤镜时显示）
//  - 滤镜名称和描述
//  - 长按快速对比（显示原图）
//  - 收藏/取消收藏
//

import SwiftUI

#if os(iOS)

/// 滤镜条主视图
struct FilterStripView: View {
    @ObservedObject var filterManager: FilterPresetManager
    var onFilterSelected: (LutFilterPreset) -> Void
    /// 可选的实时预览图像（用于生成缩略图）
    var previewImage: UIImage? = nil

    @State private var isLongPressing = false
    @State private var showIntensity = false

    private let thumbnailSize: CGFloat = 56
    private let categoryHeight: CGFloat = 32

    var body: some View {
        VStack(spacing: 0) {
            // 强度滑块（选中滤镜时显示）
            if let selected = filterManager.selectedPreset, showIntensity {
                IntensitySliderView(
                    intensity: $filterManager.filterIntensity,
                    presetName: selected.displayName
                )
                .accessibilityElement(children: .contain)
                .accessibilityLabel("滤镜强度调节")
                .accessibilityHint("调节 \(selected.displayName) 滤镜强度")
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            // 分类标签行
            CategoryTabRow(
                filterManager: filterManager,
                categoryHeight: categoryHeight
            )

            // 滤镜缩略图行
            FilterThumbnailRow(
                filterManager: filterManager,
                previewImage: previewImage,
                thumbnailSize: thumbnailSize,
                isLongPressing: $isLongPressing,
                onFilterSelected: onFilterSelected
            )
        }
        .padding(.vertical, 8)
        .background(
            Rectangle()
                .fill(DesignSystem.Colors.minimalDarkOverlay)
        )
        .onChange(of: filterManager.selectedPreset?.id) { _, _ in
            if filterManager.selectedPreset != nil {
                withAnimation(DesignSystem.Animation.filterReveal) {
                    showIntensity = true
                }
            } else {
                withAnimation(DesignSystem.Animation.filterReveal) {
                    showIntensity = false
                }
            }
        }
    }
}

// MARK: - 强度滑块视图

private struct IntensitySliderView: View {
    @Binding var intensity: Float
    let presetName: String

    var body: some View {
        HStack(spacing: 10) {
            Text(presetName)
                .font(DesignSystem.Typography.minimalFilterName)
                .foregroundColor(DesignSystem.Colors.minimalLabel)
                .lineLimit(1)
                .frame(width: 60, alignment: .leading)

            Image(systemName: "circle.lefthalf.filled")
                .font(.system(size: 10))
                .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)

            Slider(value: $intensity, in: 0...1, step: 0.01)
                .tint(DesignSystem.Colors.primary)
                .frame(height: 24)

            Image(systemName: "circle.fill")
                .font(.system(size: 10))
                .foregroundColor(DesignSystem.Colors.minimalLabel)

            Text("\(Int(intensity * 100))%")
                .font(.system(size: 10, weight: .medium, design: .monospaced))
                .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                .frame(width: 32, alignment: .trailing)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .background(Color.black.opacity(0.3))
    }
}

// MARK: - 分类标签行

private struct CategoryTabRow: View {
    @ObservedObject var filterManager: FilterPresetManager

    let categoryHeight: CGFloat

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                // "全部" 标签
                CategoryTab(
                    title: "全部",
                    icon: "square.grid.2x2",
                    isSelected: filterManager.activeCategory == nil,
                    height: categoryHeight
                ) {
                    HapticManager.shared.light()
                    withAnimation(DesignSystem.Animation.snappy) {
                        filterManager.clearCategory()
                    }
                }

                // 各分类标签
                ForEach(filterManager.allCategories, id: \.self) { category in
                    if filterManager.categoryStats.first(where: { $0.0 == category })?.1 ?? 0 > 0 {
                        CategoryTab(
                            title: category.rawValue,
                            icon: category.symbolName,
                            isSelected: filterManager.activeCategory == category,
                            height: categoryHeight
                        ) {
                            HapticManager.shared.light()
                            withAnimation(DesignSystem.Animation.snappy) {
                                filterManager.setCategory(category)
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 12)
        }
        .padding(.bottom, 6)
    }
}

/// 单个分类标签
private struct CategoryTab: View {
    let title: String
    let icon: String
    let isSelected: Bool
    let height: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 10, weight: .medium))
                Text(title)
                    .font(.system(size: 11, weight: .medium))
            }
            .foregroundColor(isSelected ? .white : DesignSystem.Colors.minimalSecondaryLabel)
            .padding(.horizontal, 10)
            .frame(height: height)
            .background(
                Capsule()
                    .fill(isSelected
                        ? DesignSystem.Colors.primary.opacity(0.6)
                        : Color.white.opacity(0.08)
                    )
            )
            .overlay(
                Capsule()
                    .strokeBorder(
                        isSelected
                            ? DesignSystem.Colors.primary
                            : Color.white.opacity(0.12),
                        lineWidth: isSelected ? 1.0 : 0.5
                    )
            )
        }
        .accessibilityLabel("\(title)滤镜分类")
        .accessibilityHint(isSelected ? "已选中" : "双击选择 \(title) 分类")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .pressScale(0.96)
    }
}

// MARK: - 滤镜缩略图行

private struct FilterThumbnailRow: View {
    @ObservedObject var filterManager: FilterPresetManager
    var previewImage: UIImage?
    let thumbnailSize: CGFloat
    @Binding var isLongPressing: Bool
    var onFilterSelected: (LutFilterPreset) -> Void

    var body: some View {
        ScrollViewReader { scrollProxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    // 无滤镜选项
                    NoFilterCell(
                        isSelected: filterManager.selectedPreset == nil,
                        size: thumbnailSize,
                        isLongPressing: $isLongPressing
                    ) {
                        HapticManager.shared.light()
                        filterManager.clearSelection()
                    }

                    // 当前显示的预设列表
                    ForEach(filterManager.displayedPresets) { preset in
                        FilterThumbnailCell(
                            preset: preset,
                            isSelected: filterManager.selectedPreset?.id == preset.id,
                            isFavorite: filterManager.isFavorite(preset),
                            previewImage: previewImage,
                            size: thumbnailSize,
                            isLongPressing: $isLongPressing,
                            onTap: {
                                HapticManager.shared.selection()
                                filterManager.selectPreset(preset)
                                onFilterSelected(preset)
                            },
                            onDoubleTap: {
                                filterManager.toggleFavorite(preset)
                            }
                        )
                        .id(preset.id)
                    }
                }
                .padding(.horizontal, 12)
            }
            .onChange(of: filterManager.selectedPreset?.id) { _, newID in
                if let id = newID {
                    withAnimation(DesignSystem.Animation.smooth) {
                        scrollProxy.scrollTo(id, anchor: .center)
                    }
                }
            }
        }
    }
}

// MARK: - 无滤镜单元格

private struct NoFilterCell: View {
    let isSelected: Bool
    let size: CGFloat
    @Binding var isLongPressing: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.white.opacity(0.06))
                        .frame(width: size, height: size)

                    Image(systemName: "camera.fill")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(isSelected ? .white : Color.white.opacity(0.5))
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .strokeBorder(
                            isSelected
                                ? DesignSystem.Colors.primary
                                : Color.white.opacity(0.15),
                            lineWidth: isSelected ? 2.0 : 1.0
                        )
                )

                Text("原始")
                    .font(DesignSystem.Typography.minimalFilterName)
                    .foregroundColor(isSelected
                        ? DesignSystem.Colors.minimalLabel
                        : DesignSystem.Colors.minimalSecondaryLabel
                    )
                    .lineLimit(1)
            }
        }
        .accessibilityLabel("原始滤镜")
        .accessibilityHint(isSelected ? "已选中，无滤镜效果" : "双击选择原始无滤镜效果")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .frame(width: size + 8)
        .pressScale(0.95)
    }
}

// MARK: - 滤镜缩略图单元格

private struct FilterThumbnailCell: View {
    let preset: LutFilterPreset
    let isSelected: Bool
    let isFavorite: Bool
    var previewImage: UIImage?
    let size: CGFloat
    @Binding var isLongPressing: Bool
    let onTap: () -> Void
    let onDoubleTap: () -> Void

    @State private var thumbnail: UIImage? = nil
    @State private var isGeneratingThumbnail = false

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                ZStack {
                    // 缩略图或占位
                    thumbnailView

                    // 选中边框
                    RoundedRectangle(cornerRadius: 12)
                        .strokeBorder(
                            isSelected
                                ? DesignSystem.Colors.primary
                                : Color.white.opacity(0.15),
                            lineWidth: isSelected ? 2.0 : 1.0
                        )

                    // 收藏标记
                    if isFavorite {
                        Image(systemName: "heart.fill")
                            .font(.system(size: 8))
                            .foregroundColor(DesignSystem.Colors.error)
                            .position(x: 4, y: 4)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                            .padding(4)
                    }
                }
                .frame(width: size, height: size)

                Text(preset.displayName)
                    .font(DesignSystem.Typography.minimalFilterName)
                    .foregroundColor(isSelected
                        ? DesignSystem.Colors.minimalLabel
                        : DesignSystem.Colors.minimalSecondaryLabel
                    )
                    .lineLimit(1)
            }
        }
        .accessibilityLabel("\(preset.displayName)滤镜")
        .accessibilityHint(isSelected ? "已选中，双击可取消" : "双击选择 \(preset.displayName) 滤镜")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .frame(width: size + 8)
        .pressScale(0.95)
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.3)
                .onChanged { _ in
                    isLongPressing = true
                }
                .onEnded { _ in
                    isLongPressing = false
                }
        )
        .onAppear {
            generateThumbnailIfNeeded()
        }
        .onChange(of: previewImage?.cgImage) { _, _ in
            thumbnail = nil
            generateThumbnailIfNeeded()
        }
    }

    @ViewBuilder
    private var thumbnailView: some View {
        if let thumb = thumbnail {
            Image(uiImage: thumb)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: size, height: size)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.white.opacity(0.06))

                if isGeneratingThumbnail {
                    ProgressView()
                        .scaleEffect(0.6)
                        .tint(Color.white.opacity(0.4))
                } else {
                    Text(String(preset.displayName.prefix(1)))
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(Color.white.opacity(0.35))
                }
            }
            .frame(width: size, height: size)
        }
    }

    private func generateThumbnailIfNeeded() {
        guard thumbnail == nil, !isGeneratingThumbnail, let image = previewImage else { return }

        isGeneratingThumbnail = true
        let targetSize = CGSize(width: size * 2, height: size * 2)

        DispatchQueue.global(qos: .userInitiated).async {
            let processor = FilterProcessor.shared
            let result = processor.thumbnailPreview(
                image: image,
                preset: preset,
                targetSize: targetSize
            )
            DispatchQueue.main.async {
                self.thumbnail = result
                self.isGeneratingThumbnail = false
            }
        }
    }
}

// MARK: - 预览

struct FilterStripView_Previews: PreviewProvider {
    static var previews: some View {
        ZStack {
            Color.black
            FilterStripView(
                filterManager: FilterPresetManager(),
                onFilterSelected: { _ in }
            )
        }
    }
}

#endif