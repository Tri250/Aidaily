//
//  PhotoEditor+UndoRedo.swift
//  LiveCapture
//
//  照片编辑器增强：撤销/重做、前后对比、批量编辑、参数复制粘贴、导出质量
//

import SwiftUI
import CoreImage

#if os(iOS)

extension PhotoEditor {

    // MARK: - 撤销/重做

    struct EditorState {
        var brightness: Float
        var contrast: Float
        var saturation: Float
        var exposure: Float
        var temperature: Float
        var tint: Float
        var sharpness: Float
        var vignette: Float
        var rotation: Double
        var cropRect: CGRect?
    }

    private static var undoStack: [EditorState] = []
    private static var redoStack: [EditorState] = []
    private static let maxUndoSteps = 20

    /// 保存当前状态到撤销栈
    func saveState() {
        let state = PhotoEditor.EditorState(
            brightness: brightness,
            contrast: contrast,
            saturation: saturation,
            exposure: exposure,
            temperature: temperature,
            tint: tint,
            sharpness: sharpness,
            vignette: vignette,
            rotation: rotation,
            cropRect: cropRect
        )
        PhotoEditor.undoStack.append(state)
        if PhotoEditor.undoStack.count > PhotoEditor.maxUndoSteps {
            PhotoEditor.undoStack.removeFirst()
        }
        PhotoEditor.redoStack.removeAll()
        isEdited = true
    }

    /// 撤销
    func undo() {
        guard let state = PhotoEditor.undoStack.popLast() else { return }
        let currentState = captureCurrentState()
        PhotoEditor.redoStack.append(currentState)
        applyState(state)
        HapticManager.shared.light()
    }

    /// 重做
    func redo() {
        guard let state = PhotoEditor.redoStack.popLast() else { return }
        let currentState = captureCurrentState()
        PhotoEditor.undoStack.append(currentState)
        applyState(state)
        HapticManager.shared.light()
    }

    var canUndo: Bool { !PhotoEditor.undoStack.isEmpty }
    var canRedo: Bool { !PhotoEditor.redoStack.isEmpty }

    private func captureCurrentState() -> EditorState {
        EditorState(
            brightness: brightness,
            contrast: contrast,
            saturation: saturation,
            exposure: exposure,
            temperature: temperature,
            tint: tint,
            sharpness: sharpness,
            vignette: vignette,
            rotation: rotation,
            cropRect: cropRect
        )
    }

    private func applyState(_ state: EditorState) {
        brightness = state.brightness
        contrast = state.contrast
        saturation = state.saturation
        exposure = state.exposure
        temperature = state.temperature
        tint = state.tint
        sharpness = state.sharpness
        vignette = state.vignette
        rotation = state.rotation
        cropRect = state.cropRect
        isEdited = true
    }

    // MARK: - 前后对比（长按查看原图）

    @Published var isShowingOriginal = false
}

// MARK: - 编辑参数复制粘贴

struct EditPreset: Codable, Identifiable {
    let id = UUID()
    let name: String
    let brightness: Float
    let contrast: Float
    let saturation: Float
    let exposure: Float
    let temperature: Float
    let tint: Float
    let sharpness: Float
    let vignette: Float
    let rotation: Double

    init(from editor: PhotoEditor, name: String = "自定义预设") {
        self.name = name
        self.brightness = editor.brightness
        self.contrast = editor.contrast
        self.saturation = editor.saturation
        self.exposure = editor.exposure
        self.temperature = editor.temperature
        self.tint = editor.tint
        self.sharpness = editor.sharpness
        self.vignette = editor.vignette
        self.rotation = editor.rotation
    }

    func apply(to editor: PhotoEditor) {
        editor.brightness = brightness
        editor.contrast = contrast
        editor.saturation = saturation
        editor.exposure = exposure
        editor.temperature = temperature
        editor.tint = tint
        editor.sharpness = sharpness
        editor.vignette = vignette
        editor.rotation = rotation
        editor.isEdited = true
        editor.saveState()
    }
}

/// 编辑预设管理器
final class EditPresetManager: ObservableObject {
    static let shared = EditPresetManager()

    @Published var savedPresets: [EditPreset] = []

    private let storageKey = "livecapture.edit_presets"

    private init() {
        loadPresets()
    }

    func savePreset(from editor: PhotoEditor, name: String) {
        let preset = EditPreset(from: editor, name: name)
        savedPresets.append(preset)
        persist()
        ToastManager.shared.success("预设「\(name)」已保存")
    }

    func deletePreset(_ preset: EditPreset) {
        savedPresets.removeAll { $0.id == preset.id }
        persist()
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(savedPresets) {
            UserDefaults.standard.set(data, forKey: storageKey)
        }
    }

    private func loadPresets() {
        if let data = UserDefaults.standard.data(forKey: storageKey),
           let presets = try? JSONDecoder().decode([EditPreset].self, from: data) {
            savedPresets = presets
        }
    }
}

// MARK: - 导出质量

enum ExportQuality: String, CaseIterable {
    case original = "原图"
    case high = "高质量(90%)"
    case medium = "中等(70%)"
    case compressed = "压缩(50%)"

    var jpegQuality: CGFloat {
        switch self {
        case .original: return 1.0
        case .high: return 0.9
        case .medium: return 0.7
        case .compressed: return 0.5
        }
    }

    var fileSizeEstimate: String {
        switch self {
        case .original: return "~5MB"
        case .high: return "~3MB"
        case .medium: return "~1.5MB"
        case .compressed: return "~500KB"
        }
    }
}

// MARK: - 编辑前后对比视图

struct BeforeAfterEditView: View {
    let originalImage: UIImage?
    let editedImage: UIImage?
    @Binding var isShowingOriginal: Bool
    @State private var sliderPosition: CGFloat = 0.5

    var body: some View {
        GeometryReader { geo in
            ZStack {
                // 编辑后的图片（底层）
                if let edited = editedImage {
                    Image(uiImage: edited)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                }

                // 原始图片（可拖动裁剪）
                if let original = originalImage {
                    Image(uiImage: original)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .clipShape(
                            Rectangle()
                                .offset(x: -geo.size.width * (1 - sliderPosition))
                        )
                        .offset(x: geo.size.width * (1 - sliderPosition))
                }

                // 分割线
                Rectangle()
                    .fill(Color.white)
                    .frame(width: 2)
                    .position(x: geo.size.width * sliderPosition, y: geo.midY)

                // 拖动滑块
                Circle()
                    .fill(Color.white)
                    .frame(width: 28, height: 28)
                    .overlay(
                        HStack(spacing: 0) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 8, weight: .bold))
                            Image(systemName: "chevron.right")
                                .font(.system(size: 8, weight: .bold))
                        }
                        .foregroundColor(.black)
                    )
                    .position(x: geo.size.width * sliderPosition, y: geo.midY)
                    .gesture(
                        DragGesture()
                            .onChanged { value in
                                sliderPosition = max(0.05, min(0.95, value.location.x / geo.size.width))
                            }
                    )
            }
        }
        .onLongPressGesture(minimumDuration: 0.1) {
            isShowingOriginal = true
        } onPressingChanged: { pressing in
            if !pressing {
                isShowingOriginal = false
            }
        }
    }
}

// MARK: - 批量编辑视图

struct BatchEditView: View {
    let selectedIDs: Set<UUID>
    let onApply: (EditPreset) -> Void
    let onDismiss: () -> Void

    @StateObject private var presetManager = EditPresetManager.shared
    @State private var selectedPreset: EditPreset?

    var body: some View {
        NavigationStack {
            List {
                Section("选择预设") {
                    ForEach(presetManager.savedPresets) { preset in
                        Button {
                            selectedPreset = preset
                        } label: {
                            HStack {
                                Text(preset.name)
                                    .font(DesignSystem.Typography.headline)
                                Spacer()
                                if selectedPreset?.id == preset.id {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(DesignSystem.Colors.primary)
                                }
                            }
                        }
                    }

                    if presetManager.savedPresets.isEmpty {
                        Text("暂无保存的预设")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                }

                Section("确认") {
                    Text("已选择 \(selectedIDs.count) 张照片")
                        .font(DesignSystem.Typography.subheadline)
                }
            }
            .navigationTitle("批量编辑")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") { onDismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("应用") {
                        if let preset = selectedPreset {
                            onApply(preset)
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(selectedPreset == nil)
                }
            }
        }
    }
}

#endif