//
//  PhotoEditView.swift
//  LiveCapture
//
//  全屏照片编辑视图
//

import SwiftUI
import Photos

#if os(iOS)

struct PhotoEditView: View {
    @StateObject private var viewModel = PhotoEditorViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showToolPanel: Bool = true
    @State private var selectedTool: EditTool = .adjustment
    @State private var isProcessing: Bool = false
    @State private var showSaveToast: Bool = false
    @State private var showHistory: Bool = false

    let image: UIImage
    let onSave: ((UIImage) -> Void)?

    init(image: UIImage, onSave: ((UIImage) -> Void)? = nil) {
        self.image = image
        self.onSave = onSave
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                // 导航栏
                navigationBar

                // 图像预览
                imagePreview

                // 工具面板
                if showToolPanel {
                    toolPanel
                }
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            viewModel.loadImage(image)
        }
        .overlay {
            if showSaveToast {
                saveToast
            }
        }
        .overlay {
            if showHistory {
                historyOverlay
            }
        }
        .sheet(isPresented: $showHistory) {
            historySheet
        }
    }

    // MARK: - 导航栏

    private var navigationBar: some View {
        HStack {
            // 取消
            Button {
                dismiss()
            } label: {
                Text("取消")
                    .font(DesignSystem.Typography.body)
                    .foregroundColor(DesignSystem.Colors.minimalLabel)
            }

            Spacer()

            // 撤销/重做
            HStack(spacing: DesignSystem.Spacing.medium) {
                Button {
                    viewModel.undo()
                } label: {
                    Image(systemName: "arrow.uturn.backward")
                        .font(.system(size: 18))
                        .foregroundColor(viewModel.editor.canUndo ? DesignSystem.Colors.minimalLabel : DesignSystem.Colors.gray4)
                }
                .disabled(!viewModel.editor.canUndo)

                Button {
                    viewModel.redo()
                } label: {
                    Image(systemName: "arrow.uturn.forward")
                        .font(.system(size: 18))
                        .foregroundColor(viewModel.editor.canRedo ? DesignSystem.Colors.minimalLabel : DesignSystem.Colors.gray4)
                }
                .disabled(!viewModel.editor.canRedo)

                Button {
                    showHistory = true
                } label: {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 18))
                        .foregroundColor(DesignSystem.Colors.minimalLabel)
                }
            }

            Spacer()

            // 完成
            Button {
                saveEditedImage()
            } label: {
                Text("完成")
                    .font(DesignSystem.Typography.headline)
                    .foregroundColor(DesignSystem.Colors.primary)
            }
            .disabled(isProcessing)
        }
        .padding(.horizontal, DesignSystem.Spacing.small)
        .padding(.vertical, DesignSystem.Spacing.xSmall)
        .background(Color.black)
    }

    // MARK: - 图像预览

    private var imagePreview: some View {
        GeometryReader { geo in
            ZStack {
                Color.black

                if let preview = viewModel.previewImage {
                    Image(uiImage: preview)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(maxWidth: geo.size.width, maxHeight: geo.size.height)
                } else {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(maxWidth: geo.size.width, maxHeight: geo.size.height)
                }

                // 加载指示器
                if isProcessing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.5)
                }
            }
        }
        .frame(maxHeight: UIScreen.main.bounds.height * 0.55)
    }

    // MARK: - 工具面板

    private var toolPanel: some View {
        VStack(spacing: 0) {
            // 工具内容
            VStack {
                switch selectedTool {
                case .crop:
                    CropEditView(viewModel: viewModel)
                case .adjustment:
                    AdjustmentPanelView(viewModel: viewModel)
                case .filter:
                    filterPanel
                case .grain:
                    grainPanel
                case .vignette:
                    VignetteEditorView(viewModel: viewModel)
                }
            }
            .frame(height: 280)

            // 底部工具栏
            bottomToolbar
        }
        .background(Color.black)
        .transition(.move(edge: .bottom))
    }

    // MARK: - 底部工具栏

    private var bottomToolbar: some View {
        HStack(spacing: 0) {
            ForEach(EditTool.allCases) { tool in
                Button {
                    withAnimation(DesignSystem.Animation.quick) {
                        selectedTool = tool
                    }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tool.iconName)
                            .font(.system(size: 20))
                        Text(tool.rawValue)
                            .font(DesignSystem.Typography.caption2)
                    }
                    .foregroundColor(selectedTool == tool ? DesignSystem.Colors.primary : DesignSystem.Colors.minimalSecondaryLabel)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, DesignSystem.Spacing.xxSmall)
                }
            }
        }
        .padding(.horizontal, DesignSystem.Spacing.small)
        .padding(.bottom, 4)
        .background(
            Rectangle()
                .fill(Color.black)
                .overlay(
                    Rectangle()
                        .fill(DesignSystem.Colors.minimalBorder)
                        .frame(height: 0.5),
                    alignment: .top
                )
        )
    }

    // MARK: - 滤镜面板

    private var filterPanel: some View {
        VStack(spacing: DesignSystem.Spacing.small) {
            Text("选择滤镜")
                .font(DesignSystem.Typography.callout)
                .foregroundColor(DesignSystem.Colors.minimalLabel)
                .padding(.top, DesignSystem.Spacing.small)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DesignSystem.Spacing.small) {
                    // 无滤镜
                    filterChip(name: "原图", preset: nil)

                    // 内置滤镜
                    let presets: [LutFilterPreset] = [
                        .dokaPortrait,
                        .kodakPortra160,
                        .agfaVista400,
                        .fujiPro400H,
                        .ilfordHP5,
                        .cinestill800T,
                        .leicaClassic,
                        .hasselbladNatural,
                        .ricohPositive,
                        .polaroid,
                        .fadedMemory,
                        .japaneseAiry
                    ]

                    ForEach(presets.indices, id: \.self) { index in
                        filterChip(name: presets[index].displayName, preset: presets[index])
                    }
                }
                .padding(.horizontal, DesignSystem.Spacing.small)
            }

            Spacer()
        }
        .background(Color.black)
    }

    private func filterChip(name: String, preset: LutFilterPreset?) -> some View {
        Button {
            if let preset = preset {
                viewModel.applyFilter(preset)
            } else {
                viewModel.resetAll()
            }
        } label: {
            VStack(spacing: 8) {
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [Color.gray.opacity(0.5), Color.gray.opacity(0.3)]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 64, height: 64)
                    .overlay(
                        Text(name.prefix(2))
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(.white.opacity(0.8))
                    )

                Text(name)
                    .font(DesignSystem.Typography.caption2)
                    .foregroundColor(
                        (preset == nil && viewModel.selectedFilterPreset == nil) ||
                        (preset != nil && viewModel.selectedFilterPreset?.id == preset?.id)
                        ? DesignSystem.Colors.primary
                        : DesignSystem.Colors.minimalSecondaryLabel
                    )
                    .lineLimit(1)
            }
            .frame(width: 64)
        }
    }

    // MARK: - 颗粒面板

    private var grainPanel: some View {
        VStack(spacing: 0) {
            Text("颗粒效果")
                .font(DesignSystem.Typography.callout)
                .foregroundColor(DesignSystem.Colors.minimalLabel)
                .padding(.top, DesignSystem.Spacing.small)

            VStack(spacing: DesignSystem.Spacing.medium) {
                HStack {
                    Text("强度")
                        .font(DesignSystem.Typography.callout)
                        .foregroundColor(DesignSystem.Colors.minimalLabel)

                    Spacer()

                    Text(String(format: "%.2f", viewModel.editor.grainAmount))
                        .font(DesignSystem.Typography.monoDigit)
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                }

                Slider(value: Binding(
                    get: { viewModel.editor.grainAmount },
                    set: { viewModel.applyGrain(amount: $0) }
                ), in: 0...1, step: 0.01)
                .tint(DesignSystem.Colors.primary)
            }
            .padding(.horizontal, DesignSystem.Spacing.small)
            .padding(.vertical, DesignSystem.Spacing.medium)

            Spacer()

            Button {
                viewModel.applyGrain(amount: 0)
            } label: {
                HStack {
                    Image(systemName: "arrow.counterclockwise")
                    Text("重置颗粒")
                }
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                .padding(.vertical, 12)
                .padding(.horizontal, 24)
                .background(
                    Capsule()
                        .strokeBorder(DesignSystem.Colors.minimalBorder, lineWidth: 1)
                )
            }
            .padding(.bottom, DesignSystem.Spacing.small)
        }
        .background(Color.black)
    }

    // MARK: - 保存成功提示

    private var saveToast: some View {
        VStack {
            Spacer()
            HStack {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(DesignSystem.Colors.success)
                Text("已保存到相册")
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(
                Capsule()
                    .fill(Color.black.opacity(0.8))
            )
            .padding(.bottom, 100)
        }
        .transition(.opacity.combined(with: .scale))
        .animation(DesignSystem.Animation.smooth, value: showSaveToast)
    }

    // MARK: - 历史记录

    private var historyOverlay: some View {
        Color.black.opacity(0.01)
            .ignoresSafeArea()
            .onTapGesture {
                showHistory = false
            }
    }

    private var historySheet: some View {
        NavigationStack {
            List {
                if viewModel.historyManager.historySteps.isEmpty {
                    Text("暂无编辑历史")
                        .font(DesignSystem.Typography.body)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .listRowBackground(Color.clear)
                } else {
                    ForEach(Array(viewModel.historyManager.historySteps.enumerated()), id: \.element.id) { index, step in
                        HStack {
                            if let thumbnail = step.thumbnail {
                                Image(uiImage: thumbnail)
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 44, height: 44)
                                    .clipShape(RoundedRectangle(cornerRadius: 6))
                            } else {
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(Color.gray.opacity(0.3))
                                    .frame(width: 44, height: 44)
                            }

                            VStack(alignment: .leading, spacing: 2) {
                                Text(step.filterName)
                                    .font(DesignSystem.Typography.subheadline)
                                    .foregroundColor(DesignSystem.Colors.textPrimary)

                                Text(step.displayName)
                                    .font(DesignSystem.Typography.caption2)
                                    .foregroundColor(DesignSystem.Colors.textTertiary)
                            }

                            Spacer()

                            if index == viewModel.historyManager.currentIndex {
                                Text("当前")
                                    .font(DesignSystem.Typography.caption2)
                                    .foregroundColor(DesignSystem.Colors.primary)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(
                                        Capsule()
                                            .fill(DesignSystem.Colors.primary.opacity(0.15))
                                    )
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
            .navigationTitle("编辑历史")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") {
                        showHistory = false
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - 保存

    private func saveEditedImage() {
        isProcessing = true
        if let edited = viewModel.export() {
            onSave?(edited)
            viewModel.save()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                isProcessing = false
                showSaveToast = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                    showSaveToast = false
                    dismiss()
                }
            }
        } else {
            isProcessing = false
        }
    }
}

#endif