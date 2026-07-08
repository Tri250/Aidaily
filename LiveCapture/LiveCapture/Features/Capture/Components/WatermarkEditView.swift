//
//  WatermarkEditView.swift
//  LiveCapture
//
//  水印编辑界面 - 底部弹出式编辑面板
//

import SwiftUI

#if os(iOS)

// MARK: - WatermarkEditView

struct WatermarkEditView: View {
    @State private var config: WatermarkConfig
    @State private var previewImage: UIImage?
    @State private var selectedTemplateID: String?
    @State private var showDateFormatPicker = false

    private let dateFormatOptions: [(String, String)] = [
        ("yyyy-MM-dd", "2026-07-08"),
        ("yyyy.MM.dd", "2026.07.08"),
        ("yyyy/MM/dd", "2026/07/08"),
        ("yyyy-MM-dd HH:mm", "2026-07-08 14:30"),
        ("MM.dd.yyyy", "07.08.2026"),
        ("dd/MM/yyyy", "08/07/2026"),
        ("yyyy年MM月dd日", "2026年07月08日"),
        ("MMM dd, yyyy", "Jul 08, 2026")
    ]

    init(config: WatermarkConfig = WatermarkConfig.load()) {
        _config = State(initialValue: config)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DesignSystem.Spacing.large) {
                    enableToggleSection
                    templateSection
                    previewSection
                    contentSection
                    positionSection
                    styleSection
                    saveButton
                }
                .padding(.horizontal, DesignSystem.Spacing.Padding.container)
                .padding(.vertical, DesignSystem.Spacing.medium)
            }
            .background(DesignSystem.Colors.backgroundPrimary)
            .navigationTitle("水印设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                    .foregroundColor(DesignSystem.Colors.textSecondary)
                }
            }
            .onAppear {
                updatePreview()
            }
            .onChange(of: config.isEnabled) { _, _ in updatePreview() }
            .onChange(of: config.text) { _, _ in updatePreview() }
            .onChange(of: config.showDate) { _, _ in updatePreview() }
            .onChange(of: config.dateFormat) { _, _ in updatePreview() }
            .onChange(of: config.showEXIF) { _, _ in updatePreview() }
            .onChange(of: config.showLogo) { _, _ in updatePreview() }
            .onChange(of: config.position) { _, _ in updatePreview() }
            .onChange(of: config.fontSize) { _, _ in updatePreview() }
            .onChange(of: config.textColor) { _, _ in updatePreview() }
            .onChange(of: config.opacity) { _, _ in updatePreview() }
            .onChange(of: config.horizontalPadding) { _, _ in updatePreview() }
            .onChange(of: config.verticalPadding) { _, _ in updatePreview() }
        }
    }

    @Environment(\.dismiss) private var dismissAction
    private func dismiss() { dismissAction() }

    // MARK: - Enable Toggle

    private var enableToggleSection: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("启用水印")
                    .font(DesignSystem.Typography.headline)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                Text("拍照时自动添加水印到照片")
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            }
            Spacer()
            Toggle("", isOn: $config.isEnabled)
                .labelsHidden()
                .tint(DesignSystem.Colors.primary)
        }
        .padding(DesignSystem.Spacing.medium)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
    }

    // MARK: - Template Selection

    private var templateSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
            Text("模板")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DesignSystem.Spacing.xSmall) {
                    ForEach(WatermarkTemplate.allTemplates) { template in
                        templateCard(template)
                    }
                }
            }
        }
    }

    private func templateCard(_ template: WatermarkTemplate) -> some View {
        let isSelected = selectedTemplateID == template.id
        return Button {
            selectTemplate(template)
        } label: {
            VStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                        .fill(DesignSystem.Colors.backgroundTertiary)
                        .frame(width: 72, height: 72)

                    Image(systemName: template.systemImageName)
                        .font(.system(size: 24))
                        .foregroundColor(isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.textSecondary)
                }
                .overlay(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                        .strokeBorder(
                            isSelected ? DesignSystem.Colors.primary : Color.clear,
                            lineWidth: 2
                        )
                )

                Text(template.name)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.textSecondary)
                    .lineLimit(1)
            }
            .frame(width: 72)
        }
    }

    private func selectTemplate(_ template: WatermarkTemplate) {
        selectedTemplateID = template.id
        config = template.config
        config.isEnabled = true
        HapticManager.shared.selection()
    }

    // MARK: - Preview

    private var previewSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
            Text("预览")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            ZStack {
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundTertiary)
                    .aspectRatio(3.0 / 4.0, contentMode: .fit)

                if let preview = previewImage {
                    Image(uiImage: preview)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .clipShape(RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium))
                } else {
                    VStack(spacing: 8) {
                        Image(systemName: "photo")
                            .font(.system(size: 32))
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                        Text("预览加载中...")
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                }
            }
            .overlay(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .strokeBorder(DesignSystem.Colors.gray3, lineWidth: 0.5)
            )
        }
    }

    // MARK: - Content Section

    private var contentSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("内容")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                // 自定义文字
                VStack(alignment: .leading, spacing: 6) {
                    Text("自定义文字")
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.textSecondary)
                    TextField("输入水印文字（可选）", text: $config.text)
                        .font(DesignSystem.Typography.body)
                        .padding(DesignSystem.Spacing.xSmall)
                        .background(
                            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                                .fill(DesignSystem.Colors.backgroundTertiary)
                        )
                }
                .padding(.vertical, 12)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, DesignSystem.Spacing.medium)

                // 显示日期
                ToggleRow(
                    icon: "calendar",
                    title: "显示日期",
                    description: "在照片上显示拍摄日期",
                    isOn: $config.showDate
                )

                if config.showDate {
                    Divider().padding(.leading, 44)

                    Button {
                        withAnimation(DesignSystem.Animation.quick) {
                            showDateFormatPicker.toggle()
                        }
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "textformat")
                                .font(.system(size: 15))
                                .foregroundColor(DesignSystem.Colors.primary)
                                .frame(width: 24)
                            Text("日期格式")
                                .font(DesignSystem.Typography.headline)
                                .foregroundColor(DesignSystem.Colors.textPrimary)
                            Spacer()
                            Text(config.dateFormat)
                                .font(DesignSystem.Typography.subheadline)
                                .foregroundColor(DesignSystem.Colors.textTertiary)
                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(DesignSystem.Colors.textTertiary)
                        }
                        .padding(.vertical, 14)
                        .padding(.horizontal, DesignSystem.Spacing.medium)
                    }

                    if showDateFormatPicker {
                        Divider().padding(.leading, 44)

                        VStack(spacing: 0) {
                            ForEach(dateFormatOptions, id: \.0) { option in
                                Button {
                                    config.dateFormat = option.0
                                    withAnimation(DesignSystem.Animation.quick) {
                                        showDateFormatPicker = false
                                    }
                                } label: {
                                    HStack {
                                        Text(option.0)
                                            .font(DesignSystem.Typography.callout)
                                            .foregroundColor(DesignSystem.Colors.textPrimary)
                                        Spacer()
                                        Text("例: \(option.1)")
                                            .font(DesignSystem.Typography.caption1)
                                            .foregroundColor(DesignSystem.Colors.textTertiary)
                                        if config.dateFormat == option.0 {
                                            Image(systemName: "checkmark")
                                                .font(.system(size: 14, weight: .semibold))
                                                .foregroundColor(DesignSystem.Colors.primary)
                                        }
                                    }
                                    .padding(.vertical, 10)
                                    .padding(.horizontal, DesignSystem.Spacing.medium)
                                }
                            }
                        }
                    }
                }

                Divider().padding(.leading, DesignSystem.Spacing.medium)

                // 显示 EXIF
                ToggleRow(
                    icon: "camera.aperture",
                    title: "显示摄影参数",
                    description: "ISO / 光圈 / 快门 / 焦距",
                    isOn: $config.showEXIF
                )

                if config.showEXIF {
                    Divider().padding(.leading, 44)

                    HStack(spacing: 10) {
                        Image(systemName: "info.circle")
                            .font(.system(size: 15))
                            .foregroundColor(DesignSystem.Colors.primary)
                            .frame(width: 24)
                        Text("示例: ISO 400  f/2.8  1/125s  26mm")
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    .padding(.vertical, 10)
                    .padding(.horizontal, DesignSystem.Spacing.medium)
                }

                Divider().padding(.leading, DesignSystem.Spacing.medium)

                // 显示 Logo
                ToggleRow(
                    icon: "signature",
                    title: "显示品牌标识",
                    description: "LiveCapture 品牌水印",
                    isOn: $config.showLogo
                )
            }
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    // MARK: - Position Selection

    private var positionSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
            Text("位置")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: DesignSystem.Spacing.xxSmall) {
                HStack(spacing: DesignSystem.Spacing.xxSmall) {
                    positionButton(.topLeft)
                    positionButton(.topRight)
                }
                HStack(spacing: DesignSystem.Spacing.xxSmall) {
                    positionButton(.center)
                }
                HStack(spacing: DesignSystem.Spacing.xxSmall) {
                    positionButton(.bottomLeft)
                    positionButton(.bottomRight)
                }
            }
            .padding(DesignSystem.Spacing.medium)
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    private func positionButton(_ position: WatermarkPosition) -> some View {
        let isSelected = config.position == position
        return Button {
            config.position = position
            HapticManager.shared.light()
        } label: {
            HStack(spacing: 6) {
                Image(systemName: position.systemImageName)
                    .font(.system(size: 14))
                Text(position.displayName)
                    .font(DesignSystem.Typography.subheadline)
            }
            .foregroundColor(isSelected ? .white : DesignSystem.Colors.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                    .fill(isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.backgroundTertiary)
            )
        }
    }

    // MARK: - Style Section

    private var styleSection: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
            Text("样式")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            VStack(spacing: 0) {
                // 字号
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("字号")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                        Spacer()
                        Text("\(Int(config.fontSize))pt")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    Slider(value: $config.fontSize, in: 8...36, step: 1)
                        .tint(DesignSystem.Colors.primary)
                }
                .padding(.vertical, 12)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, DesignSystem.Spacing.medium)

                // 颜色
                VStack(alignment: .leading, spacing: 6) {
                    Text("颜色")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    HStack(spacing: DesignSystem.Spacing.xSmall) {
                        ForEach(WatermarkColor.presetPalette.indices, id: \.self) { index in
                            let color = WatermarkColor.presetPalette[index]
                            colorButton(color)
                        }
                    }
                }
                .padding(.vertical, 12)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, DesignSystem.Spacing.medium)

                // 透明度
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("透明度")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                        Spacer()
                        Text("\(Int(config.opacity * 100))%")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    Slider(value: $config.opacity, in: 0.1...1.0, step: 0.05)
                        .tint(DesignSystem.Colors.primary)
                }
                .padding(.vertical, 12)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, DesignSystem.Spacing.medium)

                // 水平间距
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("水平间距")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                        Spacer()
                        Text("\(Int(config.horizontalPadding))pt")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    Slider(value: $config.horizontalPadding, in: 0...60, step: 2)
                        .tint(DesignSystem.Colors.primary)
                }
                .padding(.vertical, 12)
                .padding(.horizontal, DesignSystem.Spacing.medium)

                Divider().padding(.leading, DesignSystem.Spacing.medium)

                // 垂直间距
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("垂直间距")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                        Spacer()
                        Text("\(Int(config.verticalPadding))pt")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    Slider(value: $config.verticalPadding, in: 0...60, step: 2)
                        .tint(DesignSystem.Colors.primary)
                }
                .padding(.vertical, 12)
                .padding(.horizontal, DesignSystem.Spacing.medium)
            }
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    private func colorButton(_ color: WatermarkColor) -> some View {
        let isSelected = config.textColor.red == color.red
            && config.textColor.green == color.green
            && config.textColor.blue == color.blue

        return Button {
            config.textColor = color
            HapticManager.shared.light()
        } label: {
            Circle()
                .fill(color.swiftUIColor)
                .frame(width: 32, height: 32)
                .overlay(
                    Circle()
                        .strokeBorder(
                            isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.gray3,
                            lineWidth: isSelected ? 3 : 1
                        )
                )
                .overlay(
                    isSelected
                        ? Image(systemName: "checkmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(color.red + color.green + color.blue > 1.5 ? .black : .white)
                        : nil
                )
        }
    }

    // MARK: - Save Button

    private var saveButton: some View {
        Button {
            config.save()
            HapticManager.shared.success()
            dismiss()
        } label: {
            Text("保存设置")
                .font(DesignSystem.Typography.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                        .fill(DesignSystem.Colors.primary)
                )
        }
        .padding(.top, DesignSystem.Spacing.xxSmall)
    }

    // MARK: - Preview Generation

    private func updatePreview() {
        DispatchQueue.global(qos: .userInitiated).async {
            let preview = WatermarkService.shared.generatePreview(config: config)
            DispatchQueue.main.async {
                self.previewImage = preview
            }
        }
    }
}

// MARK: - Reusable ToggleRow (Local)

private struct ToggleRow: View {
    let icon: String
    let title: String
    let description: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 15))
                .foregroundColor(DesignSystem.Colors.primary)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(DesignSystem.Typography.headline)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                Text(description)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            }
            Spacer()
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(DesignSystem.Colors.primary)
        }
        .padding(.vertical, 14)
        .padding(.horizontal, DesignSystem.Spacing.medium)
    }
}

#endif