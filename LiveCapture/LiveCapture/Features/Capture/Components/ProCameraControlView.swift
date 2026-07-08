//
//  ProCameraControlView.swift
//  LiveCapture
//
//  专业相机控制面板 UI - 手动对焦/ISO/快门/白平衡/EV/AEAF锁
//  魅族极简风格，滑动条 + 数值显示
//

import SwiftUI

#if os(iOS)

struct ProCameraControlView: View {
    @ObservedObject var proManager = ProCameraManager.shared
    let cameraManager: CameraManager

    @State private var selectedTab: ProTab = .focus
    @State private var showProPanel = false

    enum ProTab: String, CaseIterable {
        case focus = "对焦"
        case exposure = "曝光"
        case whiteBalance = "白平衡"
        case tools = "工具"

        var icon: String {
            switch self {
            case .focus: return "circle.dotted.circle"
            case .exposure: return "camera.aperture"
            case .whiteBalance: return "thermometer.sun"
            case .tools: return "wrench.and.screwdriver"
            }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            if showProPanel {
                // 标签切换
                HStack(spacing: 0) {
                    ForEach(ProTab.allCases, id: \.self) { tab in
                        Button {
                            HapticManager.shared.light()
                            withAnimation(DesignSystem.Animation.smooth) {
                                selectedTab = tab
                            }
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: tab.icon)
                                    .font(.system(size: 14, weight: .medium))
                                Text(tab.rawValue)
                                    .font(.system(size: 10, weight: .medium))
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .foregroundColor(selectedTab == tab ? .white : DesignSystem.Colors.minimalSecondaryLabel)
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(selectedTab == tab ? Color.white.opacity(0.15) : Color.clear)
                            )
                        }
                        .accessibilityLabel("\(tab.rawValue)控制")
                    }
                }
                .padding(.horizontal, 12)
                .padding(.top, 8)

                // 内容区
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 0) {
                        focusPanel
                            .frame(width: UIScreen.main.bounds.width - 24)
                        exposurePanel
                            .frame(width: UIScreen.main.bounds.width - 24)
                        whiteBalancePanel
                            .frame(width: UIScreen.main.bounds.width - 24)
                        toolsPanel
                            .frame(width: UIScreen.main.bounds.width - 24)
                    }
                }
                .content.offset(x: -CGFloat(selectedTab.offset) * (UIScreen.main.bounds.width - 24))
                .animation(DesignSystem.Animation.modeSlide, value: selectedTab)
                .frame(height: 160)
            }

            // 展开/收起按钮
            Button {
                HapticManager.shared.light()
                withAnimation(DesignSystem.Animation.bouncy) {
                    showProPanel.toggle()
                }
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "slider.horizontal.3")
                        .font(.system(size: 12, weight: .medium))
                    Text(showProPanel ? "收起专业模式" : "专业模式")
                        .font(DesignSystem.Typography.minimalControlLabel)
                    Image(systemName: showProPanel ? "chevron.down" : "chevron.up")
                        .font(.system(size: 10, weight: .medium))
                }
                .foregroundColor(showProPanel ? .white : DesignSystem.Colors.minimalSecondaryLabel)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    Capsule()
                        .fill(showProPanel ? Color.white.opacity(0.12) : DesignSystem.Colors.minimalDarkOverlay)
                )
            }
            .padding(.top, 4)
        }
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.black.opacity(0.85))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .strokeBorder(DesignSystem.Stroke.subtle, lineWidth: 0.5)
                )
        )
        .padding(.horizontal, 12)
    }

    // MARK: - 对焦面板

    private var focusPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("手动对焦")
                    .font(DesignSystem.Typography.minimalControlLabel)
                    .foregroundColor(.white)
                Spacer()
                Toggle("", isOn: $proManager.manualFocusEnabled)
                    .labelsHidden()
                    .tint(DesignSystem.Colors.primary)
                    .scaleEffect(0.8)
            }

            VStack(spacing: 4) {
                HStack {
                    Text("近")
                        .font(.system(size: 10))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                    Slider(value: $proManager.focusLensPosition, in: 0...1) { isEditing in
                        if !isEditing, let device = cameraManager.activeVideoDevice {
                            try? proManager.setManualFocus(lensPosition: proManager.focusLensPosition, device: device)
                        }
                    }
                    .tint(DesignSystem.Colors.primary)
                    Text("远")
                        .font(.system(size: 10))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                }
                Text("镜片位置: \(String(format: "%.2f", proManager.focusLensPosition))")
                    .font(DesignSystem.Typography.monoDigit)
                    .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
            }

            HStack {
                Toggle("峰值对焦", isOn: $proManager.focusPeakingEnabled)
                    .font(DesignSystem.Typography.minimalControlLabel)
                    .foregroundColor(.white)
                    .tint(DesignSystem.Colors.primary)
                Spacer()
                Button {
                    HapticManager.shared.light()
                    if let device = cameraManager.activeVideoDevice {
                        try? proManager.toggleAFLock(device: device)
                    }
                } label: {
                    Text(proManager.afLocked ? "AF 已锁定" : "AF 锁定")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(proManager.afLocked ? DesignSystem.Colors.warning : DesignSystem.Colors.minimalSecondaryLabel)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(proManager.afLocked ? DesignSystem.Colors.warningBg : Color.white.opacity(0.1)))
                }
            }
        }
        .padding(12)
    }

    // MARK: - 曝光面板

    private var exposurePanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("手动曝光")
                    .font(DesignSystem.Typography.minimalControlLabel)
                    .foregroundColor(.white)
                Spacer()
                Toggle("", isOn: $proManager.manualExposureEnabled)
                    .labelsHidden()
                    .tint(DesignSystem.Colors.primary)
                    .scaleEffect(0.8)
            }

            // ISO
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text("ISO")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                    Spacer()
                    Text("\(Int(proManager.iso))")
                        .font(DesignSystem.Typography.monoCaption)
                        .foregroundColor(.white)
                }
                Slider(value: $proManager.iso, in: 50...3200, step: 1) { isEditing in
                    if !isEditing, let device = cameraManager.activeVideoDevice {
                        try? proManager.setManualExposure(iso: proManager.iso,
                                                           duration: proManager.shutterSpeed,
                                                           device: device)
                    }
                }
                .tint(DesignSystem.Colors.primary)
            }

            // 快门速度
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text("快门")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                    Spacer()
                    Text(shutterLabel)
                        .font(DesignSystem.Typography.monoCaption)
                        .foregroundColor(.white)
                }
                Picker("快门", selection: $proManager.shutterSpeed) {
                    ForEach(proManager.shutterSpeedPresets, id: \.value) { preset in
                        Text(preset.label).tag(preset.value)
                    }
                }
                .pickerStyle(.wheel)
                .frame(height: 60)
                .onChange(of: proManager.shutterSpeed) { _, newValue in
                    if let device = cameraManager.activeVideoDevice {
                        try? proManager.setManualExposure(iso: proManager.iso,
                                                           duration: newValue,
                                                           device: device)
                    }
                }
            }

            // EV 补偿
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text("EV 补偿")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                    Spacer()
                    Text(String(format: "%+.1f", proManager.exposureBias))
                        .font(DesignSystem.Typography.monoCaption)
                        .foregroundColor(.white)
                }
                Slider(value: $proManager.exposureBias, in: -3...3, step: 0.33) { isEditing in
                    if !isEditing, let device = cameraManager.activeVideoDevice {
                        try? proManager.setExposureBias(proManager.exposureBias, device: device)
                    }
                }
                .tint(DesignSystem.Colors.primary)
            }
        }
        .padding(12)
    }

    // MARK: - 白平衡面板

    private var whiteBalancePanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("白平衡")
                    .font(DesignSystem.Typography.minimalControlLabel)
                    .foregroundColor(.white)
                Spacer()
                Toggle("", isOn: $proManager.manualWhiteBalanceEnabled)
                    .labelsHidden()
                    .tint(DesignSystem.Colors.primary)
                    .scaleEffect(0.8)
            }

            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text("色温")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                    Spacer()
                    Text("\(Int(proManager.colorTemperature))K")
                        .font(DesignSystem.Typography.monoCaption)
                        .foregroundColor(.white)
                }
                Slider(value: $proManager.colorTemperature, in: 2500...8000, step: 100) { isEditing in
                    if !isEditing, let device = cameraManager.activeVideoDevice {
                        try? proManager.setManualWhiteBalance(temperature: proManager.colorTemperature, device: device)
                    }
                }
                .tint(Color.orange)
            }

            // 预设按钮
            LazyVGrid(columns: Array(repeating: .init(.flexible()), count: 4), spacing: 6) {
                ForEach(proManager.whiteBalancePresets, id: \.value) { preset in
                    Button {
                        HapticManager.shared.light()
                        proManager.colorTemperature = preset.value
                        if let device = cameraManager.activeVideoDevice {
                            try? proManager.setManualWhiteBalance(temperature: preset.value, device: device)
                        }
                    } label: {
                        Text(preset.label)
                            .font(.system(size: 9, weight: .medium))
                            .foregroundColor(abs(proManager.colorTemperature - preset.value) < 100 ? .white : DesignSystem.Colors.minimalSecondaryLabel)
                            .padding(.vertical, 4)
                            .frame(maxWidth: .infinity)
                            .background(
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(abs(proManager.colorTemperature - preset.value) < 100 ? DesignSystem.Colors.primary.opacity(0.3) : Color.white.opacity(0.05))
                            )
                    }
                }
            }
        }
        .padding(12)
    }

    // MARK: - 工具面板

    private var toolsPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("辅助工具")
                    .font(DesignSystem.Typography.minimalControlLabel)
                    .foregroundColor(.white)
                Spacer()
            }

            HStack(spacing: 12) {
                toolToggle(icon: "waveform.path.ecg", label: "直方图", isOn: Binding(
                    get: { false },
                    set: { _ in }
                ))
                toolToggle(icon: "rectangle.split.2x2", label: "斑马纹", isOn: $proManager.zebraEnabled)
                toolToggle(icon: "circle.dotted", label: "峰值", isOn: $proManager.focusPeakingEnabled)
                toolToggle(icon: "camera.raw", label: "RAW", isOn: $proManager.rawCaptureEnabled)
            }

            // AE 锁定
            HStack {
                Button {
                    HapticManager.shared.light()
                    if let device = cameraManager.activeVideoDevice {
                        try? proManager.toggleAELock(device: device)
                    }
                } label: {
                    Label(proManager.aeLocked ? "AE 已锁定" : "AE 锁定",
                          systemImage: proManager.aeLocked ? "lock.fill" : "lock.open")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(proManager.aeLocked ? DesignSystem.Colors.warning : .white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(
                            Capsule()
                                .fill(proManager.aeLocked ? DesignSystem.Colors.warningBg : Color.white.opacity(0.1))
                        )
                }

                Button {
                    HapticManager.shared.light()
                    if let device = cameraManager.activeVideoDevice {
                        try? proManager.resetAllManualSettings(device: device)
                    }
                } label: {
                    Label("重置全部", systemImage: "arrow.counterclockwise")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(Color.white.opacity(0.1)))
                }
            }
        }
        .padding(12)
    }

    private func toolToggle(icon: String, label: String, isOn: Binding<Bool>) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(isOn.wrappedValue ? .white : DesignSystem.Colors.minimalSecondaryLabel)
            Text(label)
                .font(.system(size: 9, weight: .medium))
                .foregroundColor(isOn.wrappedValue ? .white : DesignSystem.Colors.minimalSecondaryLabel)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(isOn.wrappedValue ? DesignSystem.Colors.primary.opacity(0.3) : Color.white.opacity(0.05))
        )
        .onTapGesture {
            HapticManager.shared.light()
            isOn.wrappedValue.toggle()
        }
    }

    private var shutterLabel: String {
        let s = proManager.shutterSpeed
        if s.timescale > s.value {
            return "1/\(s.timescale / s.value)"
        } else {
            return "\(Float(s.value) / Float(s.timescale))\""
        }
    }
}

private extension ProCameraControlView.ProTab {
    var offset: Int {
        ProCameraControlView.ProTab.allCases.firstIndex(of: self) ?? 0
    }
}

#endif