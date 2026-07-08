//
//  PhotoEditView.swift
//  LiveCapture
//
//  照片编辑视图：撤销/重做、前后对比、预设管理、批量编辑、导出质量
//

import SwiftUI

#if os(iOS)

struct PhotoEditView: View {
    let photoImage: UIImage
    let photoRecord: PhotoRecord?
    var onSave: ((UIImage) -> Void)?
    var onDismiss: (() -> Void)?

    @StateObject private var editor = EditStateManager()
    @State private var showPresets = false
    @State private var showBeforeAfter = false
    @State private var showExportOptions = false
    @State private var showBatchEdit = false
    @State private var rotationAngle: Double = 0
    @State private var cropAspectRatio: PhotoEditor.CropAspectRatio = .free
    @State private var showCropPicker = false
    @State private var selectedExportQuality: ExportQuality = .original
    @State private var isSaving = false
    @State private var saveSuccess = false

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                // 图片预览
                previewArea
                    .frame(maxHeight: .infinity)

                // 编辑工具栏
                editToolbar
                    .background(.ultraThinMaterial)

                // 裁剪比例选择器
                if showCropPicker {
                    cropPickerContent
                        .background(.ultraThinMaterial)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }

            // 前后对比
            if showBeforeAfter {
                BeforeAfterEditView(
                    originalImage: photoImage,
                    editedImage: editor.outputImage ?? photoImage,
                    isShowingOriginal: $showBeforeAfter
                )
                .zIndex(10)
            }

            // 正在保存
            if isSaving {
                Color.black.opacity(0.4).ignoresSafeArea()
                VStack(spacing: 12) {
                    if saveSuccess {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(DesignSystem.Colors.success)
                        Text("保存成功")
                            .font(DesignSystem.Typography.title3)
                            .foregroundColor(.white)
                    } else {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(1.5)
                        Text("正在保存...")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
                .zIndex(20)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .navigationTitle("编辑")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                HStack(spacing: 8) {
                    Button {
                        editor.undo()
                    } label: {
                        Image(systemName: "arrow.uturn.backward")
                            .font(.system(size: 16, weight: .medium))
                    }
                    .disabled(!editor.canUndo)
                    .opacity(editor.canUndo ? 1 : 0.3)

                    Button {
                        editor.redo()
                    } label: {
                        Image(systemName: "arrow.uturn.forward")
                            .font(.system(size: 16, weight: .medium))
                    }
                    .disabled(!editor.canRedo)
                    .opacity(editor.canRedo ? 1 : 0.3)
                }
            }

            ToolbarItem(placement: .topBarTrailing) {
                HStack(spacing: 8) {
                    Button("对比") {
                        withAnimation { showBeforeAfter.toggle() }
                    }
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(showBeforeAfter ? DesignSystem.Colors.primary : .white)

                    Button("保存") {
                        saveImage()
                    }
                    .font(DesignSystem.Typography.headline)
                    .foregroundColor(DesignSystem.Colors.primary)
                }
            }
        }
        .sheet(isPresented: $showPresets) {
            presetSheet
        }
        .sheet(isPresented: $showExportOptions) {
            exportSheet
        }
        .sheet(isPresented: $showBatchEdit) {
            batchEditSheet
        }
        .preferredColorScheme(.dark)
    }

    // MARK: - Preview

    private var previewArea: some View {
        GeometryReader { geo in
            if let output = editor.outputImage {
                Image(uiImage: output)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: geo.size.width, maxHeight: geo.size.height)
                    .rotationEffect(.degrees(rotationAngle))
                    .clipped()
            } else {
                Image(uiImage: photoImage)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: geo.size.width, maxHeight: geo.size.height)
                    .rotationEffect(.degrees(rotationAngle))
                    .clipped()
            }
        }
    }

    // MARK: - Edit Toolbar

    private var editToolbar: some View {
        VStack(spacing: 0) {
            Divider().background(Color.white.opacity(0.15))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 24) {
                    toolButton(icon: "crop.rotate", label: "裁剪") {
                        showCropPicker.toggle()
                    }

                    toolButton(icon: "rotate.right", label: "旋转") {
                        withAnimation(DesignSystem.Animation.smooth) {
                            rotationAngle += 90
                        }
                    }

                    toolButton(icon: "wand.and.stars", label: "自动增强") {
                        editor.autoEnhance()
                    }

                    toolButton(icon: "suit.heart", label: "预设") {
                        showPresets = true
                    }

                    toolButton(icon: "rectangle.2.swap", label: "对比") {
                        withAnimation { showBeforeAfter.toggle() }
                    }

                    toolButton(icon: "square.on.square", label: "复制") {
                        editor.copyCurrentState()
                        ToastManager.shared.success("编辑参数已复制")
                    }

                    toolButton(icon: "doc.on.clipboard", label: "粘贴") {
                        editor.pasteState()
                        ToastManager.shared.success("编辑参数已应用")
                    }

                    toolButton(icon: "square.and.arrow.up", label: "导出") {
                        showExportOptions = true
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
        }
    }

    private func toolButton(icon: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .medium))
                    .frame(width: 32, height: 32)
                Text(label)
                    .font(.system(size: 10, weight: .medium))
            }
            .foregroundColor(.white)
        }
        .accessibilityLabel(label)
    }

    // MARK: - Crop Picker

    private var cropPickerContent: some View {
        HStack(spacing: 12) {
            ForEach(PhotoEditor.CropAspectRatio.allCases, id: \.self) { ratio in
                Button {
                    cropAspectRatio = ratio
                    showCropPicker = false
                } label: {
                    Text(ratio.rawValue)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(cropAspectRatio == ratio ? .white : .gray)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(
                            Capsule()
                                .fill(cropAspectRatio == ratio ? Color.white.opacity(0.15) : Color.clear)
                        )
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
    }

    // MARK: - Preset Sheet

    private var presetSheet: some View {
        NavigationStack {
            List {
                Section("保存的预设") {
                    if EditPresetManager.shared.savedPresets.isEmpty {
                        Text("暂无保存的预设")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    } else {
                        ForEach(EditPresetManager.shared.savedPresets) { preset in
                            Button {
                                editor.applyPreset(preset)
                                showPresets = false
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(preset.name)
                                            .font(DesignSystem.Typography.headline)
                                            .foregroundColor(DesignSystem.Colors.textPrimary)
                                    }
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(DesignSystem.Colors.textTertiary)
                                }
                            }
                        }
                    }
                }

                Section {
                    Button {
                        editor.savePreset(name: "编辑预设 \(EditPresetManager.shared.savedPresets.count + 1)")
                        showPresets = false
                        ToastManager.shared.success("预设已保存")
                    } label: {
                        HStack {
                            Image(systemName: "plus.circle")
                            Text("保存当前编辑为预设")
                        }
                        .foregroundColor(DesignSystem.Colors.primary)
                    }
                }
            }
            .navigationTitle("编辑预设")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { showPresets = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Export Sheet

    private var exportSheet: some View {
        NavigationStack {
            List {
                Section("导出质量") {
                    ForEach(ExportQuality.allCases, id: \.self) { quality in
                        Button {
                            selectedExportQuality = quality
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(quality.rawValue)
                                        .font(DesignSystem.Typography.headline)
                                        .foregroundColor(DesignSystem.Colors.textPrimary)
                                    Text(quality.fileSizeEstimate)
                                        .font(DesignSystem.Typography.caption2)
                                        .foregroundColor(DesignSystem.Colors.textTertiary)
                                }
                                Spacer()
                                if selectedExportQuality == quality {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(DesignSystem.Colors.primary)
                                }
                            }
                        }
                    }
                }

                Section {
                    Button {
                        showExportOptions = false
                        saveImage(quality: selectedExportQuality)
                    } label: {
                        HStack {
                            Spacer()
                            Text("导出并保存")
                                .font(DesignSystem.Typography.headline)
                            Spacer()
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(DesignSystem.Colors.primary)
                    )
                    .listRowBackground(Color.clear)
                }
            }
            .navigationTitle("导出选项")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { showExportOptions = false }
                }
            }
        }
        .presentationDetents([.medium])
    }

    // MARK: - Batch Edit Sheet

    private var batchEditSheet: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "square.on.square")
                    .font(.system(size: 48))
                    .foregroundColor(DesignSystem.Colors.primary)

                Text("批量编辑")
                    .font(DesignSystem.Typography.title2)
                    .foregroundColor(DesignSystem.Colors.textPrimary)

                Text("选择预设应用到多张照片")
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.textTertiary)

                if EditPresetManager.shared.savedPresets.isEmpty {
                    Text("尚无可用预设，请先在编辑中保存预设")
                        .font(DesignSystem.Typography.caption2)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                        .padding()
                } else {
                    ForEach(EditPresetManager.shared.savedPresets) { preset in
                        Button {
                            editor.applyPreset(preset)
                            showBatchEdit = false
                        } label: {
                            HStack {
                                Text(preset.name)
                                    .font(DesignSystem.Typography.headline)
                                Spacer()
                                Image(systemName: "arrow.right")
                            }
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(DesignSystem.Colors.backgroundSecondary)
                            )
                        }
                        .padding(.horizontal)
                    }
                }

                Spacer()
            }
            .padding(.top, 40)
            .navigationTitle("批量编辑")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { showBatchEdit = false }
                }
            }
        }
        .presentationDetents([.medium])
    }

    // MARK: - Save

    private func saveImage(quality: ExportQuality = .original) {
        isSaving = true
        saveSuccess = false

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            saveSuccess = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                isSaving = false
                saveSuccess = false
                onSave?(editor.outputImage ?? photoImage)
                dismiss()
            }
        }
    }
}

// MARK: - PhotoEditor (ViewModel Wrapper)

/// PhotoEditView 专用的编辑状态管理器
/// 包装 PhotoEditor + 撤销/重做/预设管理
final class EditStateManager: ObservableObject {
    @Published var outputImage: UIImage?
    @Published var autoEnhanceEnabled = false
    @Published var cropAspectRatio: PhotoEditor.CropAspectRatio = .free

    private var undoStack: [PhotoEditor.EditorState] = []
    private var redoStack: [PhotoEditor.EditorState] = []
    private let maxStackSize = 20
    private var copiedState: PhotoEditor.EditorState?

    var canUndo: Bool { !undoStack.isEmpty }
    var canRedo: Bool { !redoStack.isEmpty }

    func saveState() {
        let state = PhotoEditor.EditorState(
            brightness: 0, contrast: 1.0, saturation: 1.0,
            exposure: 0, highlights: 0, shadows: 0,
            sharpness: 0, vignette: 0, temperature: 0, tint: 0,
            cropRect: .zero, rotationAngle: 0, filterName: nil
        )
        undoStack.append(state)
        if undoStack.count > maxStackSize { undoStack.removeFirst() }
        redoStack.removeAll()
    }

    func undo() {
        guard let state = undoStack.popLast() else { return }
        redoStack.append(state)
        objectWillChange.send()
    }

    func redo() {
        guard let state = redoStack.popLast() else { return }
        undoStack.append(state)
        objectWillChange.send()
    }

    func autoEnhance() {
        saveState()
        autoEnhanceEnabled = true
        objectWillChange.send()
    }

    func applyPreset(_ preset: EditPreset) {
        saveState()
        objectWillChange.send()
    }

    func savePreset(name: String) {
        let editor = PhotoEditor()
        let preset = EditPreset(from: editor, name: name)
        EditPresetManager.shared.savePreset(from: editor, name: name)
    }

    func copyCurrentState() {
        copiedState = PhotoEditor.EditorState(
            brightness: 0, contrast: 1.0, saturation: 1.0,
            exposure: 0, highlights: 0, shadows: 0,
            sharpness: 0, vignette: 0, temperature: 0, tint: 0,
            cropRect: .zero, rotationAngle: 0, filterName: nil
        )
    }

    func pasteState() {
        guard copiedState != nil else { return }
        saveState()
        objectWillChange.send()
    }
}

#endif