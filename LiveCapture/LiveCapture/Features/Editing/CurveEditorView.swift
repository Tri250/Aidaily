//
//  CurveEditorView.swift
//  LiveCapture
//
//  色调曲线编辑器 - 交互式 RGB 曲线
//

import SwiftUI

#if os(iOS)

/// 曲线通道
enum CurveChannel: String, CaseIterable, Identifiable {
    case rgb = "RGB"
    case red = "R"
    case green = "G"
    case blue = "B"

    var id: String { rawValue }

    var color: Color {
        switch self {
        case .rgb: return .white
        case .red: return .red
        case .green: return .green
        case .blue: return .blue
        }
    }
}

/// 曲线预设
enum CurvePreset: String, CaseIterable, Identifiable {
    case linear = "线性"
    case softContrast = "柔和对比"
    case strongContrast = "强对比"
    case liftShadows = "提亮阴影"
    case crushHighlights = "压暗高光"

    var id: String { rawValue }

    func curvePoints() -> [CGPoint] {
        switch self {
        case .linear:
            return [CGPoint(x: 0, y: 0), CGPoint(x: 0.25, y: 0.25), CGPoint(x: 0.5, y: 0.5), CGPoint(x: 0.75, y: 0.75), CGPoint(x: 1, y: 1)]
        case .softContrast:
            return [CGPoint(x: 0, y: 0), CGPoint(x: 0.25, y: 0.2), CGPoint(x: 0.5, y: 0.5), CGPoint(x: 0.75, y: 0.8), CGPoint(x: 1, y: 1)]
        case .strongContrast:
            return [CGPoint(x: 0, y: 0), CGPoint(x: 0.25, y: 0.13), CGPoint(x: 0.5, y: 0.5), CGPoint(x: 0.75, y: 0.87), CGPoint(x: 1, y: 1)]
        case .liftShadows:
            return [CGPoint(x: 0, y: 0.1), CGPoint(x: 0.25, y: 0.35), CGPoint(x: 0.5, y: 0.5), CGPoint(x: 0.75, y: 0.75), CGPoint(x: 1, y: 1)]
        case .crushHighlights:
            return [CGPoint(x: 0, y: 0), CGPoint(x: 0.25, y: 0.25), CGPoint(x: 0.5, y: 0.5), CGPoint(x: 0.75, y: 0.65), CGPoint(x: 1, y: 0.9)]
        }
    }
}

struct CurveEditorView: View {
    @ObservedObject var viewModel: PhotoEditorViewModel

    @State private var selectedChannel: CurveChannel = .rgb
    @State private var controlPoints: [CGPoint] = [
        CGPoint(x: 0, y: 0),
        CGPoint(x: 0.25, y: 0.25),
        CGPoint(x: 0.5, y: 0.5),
        CGPoint(x: 0.75, y: 0.75),
        CGPoint(x: 1, y: 1)
    ]
    @State private var rPoints: [CGPoint] = CurvePreset.linear.curvePoints()
    @State private var gPoints: [CGPoint] = CurvePreset.linear.curvePoints()
    @State private var bPoints: [CGPoint] = CurvePreset.linear.curvePoints()
    @State private var draggingIndex: Int? = nil

    private let gridSize: CGFloat = 6

    var body: some View {
        VStack(spacing: 0) {
            // 通道选择器
            HStack(spacing: DesignSystem.Spacing.xxSmall) {
                ForEach(CurveChannel.allCases) { channel in
                    Button {
                        withAnimation(DesignSystem.Animation.quick) {
                            selectedChannel = channel
                        }
                    } label: {
                        Text(channel.rawValue)
                            .font(DesignSystem.Typography.caption1)
                            .fontWeight(.medium)
                            .foregroundColor(selectedChannel == channel ? channel.color : DesignSystem.Colors.minimalSecondaryLabel)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(
                                Capsule()
                                    .fill(selectedChannel == channel ? channel.color.opacity(0.2) : Color.clear)
                            )
                    }
                }
            }
            .padding(.vertical, DesignSystem.Spacing.small)

            // 曲线编辑区域
            GeometryReader { geo in
                ZStack {
                    // 背景网格
                    curveGrid(in: geo.size)

                    // 直方图背景（简化）
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color.black.opacity(0.3),
                            Color.black.opacity(0.1),
                            Color.black.opacity(0.3)
                        ]),
                        startPoint: .bottom,
                        endPoint: .top
                    )

                    // 曲线路径
                    Path { path in
                        let points = currentPoints()
                        path.move(to: toViewPoint(points[0], size: geo.size))
                        for i in 1..<points.count {
                            path.addLine(to: toViewPoint(points[i], size: geo.size))
                        }
                    }
                    .stroke(selectedChannel.color, style: SwiftUI.StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))

                    // 填充区域
                    Path { path in
                        let points = currentPoints()
                        path.move(to: CGPoint(x: 0, y: geo.size.height))
                        path.addLine(to: toViewPoint(points[0], size: geo.size))
                        for i in 1..<points.count {
                            path.addLine(to: toViewPoint(points[i], size: geo.size))
                        }
                        path.addLine(to: CGPoint(x: geo.size.width, y: geo.size.height))
                        path.closeSubpath()
                    }
                    .fill(selectedChannel.color.opacity(0.08))

                    // 控制点
                    ForEach(Array(currentPoints().enumerated()), id: \.offset) { index, point in
                        let viewPoint = toViewPoint(point, size: geo.size)
                        Circle()
                            .fill(selectedChannel.color)
                            .frame(width: draggingIndex == index ? 18 : 14, height: draggingIndex == index ? 18 : 14)
                            .overlay(
                                Circle()
                                    .stroke(Color.white, lineWidth: 2)
                            )
                            .position(viewPoint)
                            .gesture(
                                DragGesture()
                                    .onChanged { value in
                                        draggingIndex = index
                                        let newPoint = fromViewPoint(value.location, size: geo.size)
                                        updateControlPoint(at: index, point: newPoint)
                                    }
                                    .onEnded { _ in
                                        draggingIndex = nil
                                        applyToEditor()
                                    }
                            )
                    }
                }
            }
            .padding(.horizontal, DesignSystem.Spacing.small)
            .frame(height: 240)

            // 预设曲线
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DesignSystem.Spacing.xxSmall) {
                    ForEach(CurvePreset.allCases) { preset in
                        Button {
                            withAnimation(DesignSystem.Animation.quick) {
                                applyPreset(preset)
                            }
                        } label: {
                            Text(preset.rawValue)
                                .font(DesignSystem.Typography.caption2)
                                .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(
                                    Capsule()
                                        .strokeBorder(DesignSystem.Colors.minimalBorder, lineWidth: 1)
                                )
                        }
                    }
                }
                .padding(.horizontal, DesignSystem.Spacing.small)
            }
            .padding(.vertical, DesignSystem.Spacing.small)

            // 重置
            Button {
                withAnimation(DesignSystem.Animation.quick) {
                    resetCurves()
                }
            } label: {
                HStack {
                    Image(systemName: "arrow.counterclockwise")
                    Text("重置曲线")
                }
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                .padding(.vertical, 10)
                .padding(.horizontal, 20)
                .background(
                    Capsule()
                        .strokeBorder(DesignSystem.Colors.minimalBorder, lineWidth: 1)
                )
            }
            .padding(.bottom, DesignSystem.Spacing.small)
        }
        .background(Color.black)
        .onAppear {
            syncFromEditor()
        }
    }

    // MARK: - 网格

    private func curveGrid(in size: CGSize) -> some View {
        Canvas { context, _ in
            let gridColor = Color.white.opacity(0.08)
            let w = size.width / 4
            let h = size.height / 4

            for i in 1..<4 {
                let x = w * CGFloat(i)
                var linePath = Path()
                linePath.move(to: CGPoint(x: x, y: 0))
                linePath.addLine(to: CGPoint(x: x, y: size.height))
                context.stroke(linePath, with: .color(gridColor), lineWidth: 0.5)

                let y = h * CGFloat(i)
                var hPath = Path()
                hPath.move(to: CGPoint(x: 0, y: y))
                hPath.addLine(to: CGPoint(x: size.width, y: y))
                context.stroke(hPath, with: .color(gridColor), lineWidth: 0.5)
            }
        }
    }

    // MARK: - 坐标转换

    private func toViewPoint(_ point: CGPoint, size: CGSize) -> CGPoint {
        CGPoint(
            x: point.x * size.width,
            y: (1 - point.y) * size.height
        )
    }

    private func fromViewPoint(_ viewPoint: CGPoint, size: CGSize) -> CGPoint {
        let x = max(0, min(1, viewPoint.x / size.width))
        let y = max(0, min(1, 1 - viewPoint.y / size.height))
        return CGPoint(x: x, y: y)
    }

    // MARK: - 控制点管理

    private func currentPoints() -> [CGPoint] {
        switch selectedChannel {
        case .rgb: return controlPoints
        case .red: return rPoints
        case .green: return gPoints
        case .blue: return bPoints
        }
    }

    private func updateControlPoint(at index: Int, point: CGPoint) {
        switch selectedChannel {
        case .rgb:
            guard index >= 0 && index < controlPoints.count else { return }
            var newPoints = controlPoints
            newPoints[index] = point
            // 保持 x 排序
            newPoints.sort { $0.x < $1.x }
            newPoints[0].x = 0
            newPoints[newPoints.count - 1].x = 1
            controlPoints = newPoints
        case .red:
            guard index >= 0 && index < rPoints.count else { return }
            rPoints[index] = point
            rPoints.sort { $0.x < $1.x }
            rPoints[0].x = 0
            rPoints[rPoints.count - 1].x = 1
        case .green:
            guard index >= 0 && index < gPoints.count else { return }
            gPoints[index] = point
            gPoints.sort { $0.x < $1.x }
            gPoints[0].x = 0
            gPoints[gPoints.count - 1].x = 1
        case .blue:
            guard index >= 0 && index < bPoints.count else { return }
            bPoints[index] = point
            bPoints.sort { $0.x < $1.x }
            bPoints[0].x = 0
            bPoints[bPoints.count - 1].x = 1
        }
    }

    private func applyPreset(_ preset: CurvePreset) {
        let points = preset.curvePoints()
        switch selectedChannel {
        case .rgb: controlPoints = points
        case .red: rPoints = points
        case .green: gPoints = points
        case .blue: bPoints = points
        }
        applyToEditor()
    }

    private func resetCurves() {
        let linear = CurvePreset.linear.curvePoints()
        controlPoints = linear
        rPoints = linear
        gPoints = linear
        bPoints = linear
        applyToEditor()
    }

    // MARK: - 应用到编辑器

    private func applyToEditor() {
        let r = rPoints.map { Float($0.y) }
        let g = gPoints.map { Float($0.y) }
        let b = bPoints.map { Float($0.y) }
        viewModel.applyCurve(r: r, g: g, b: b)
    }

    private func syncFromEditor() {
        let editor = viewModel.editor
        if editor.curveR.count == 5 {
            rPoints = zip([0.0, 0.25, 0.5, 0.75, 1.0], editor.curveR).map { CGPoint(x: $0, y: CGFloat($1)) }
        }
        if editor.curveG.count == 5 {
            gPoints = zip([0.0, 0.25, 0.5, 0.75, 1.0], editor.curveG).map { CGPoint(x: $0, y: CGFloat($1)) }
        }
        if editor.curveB.count == 5 {
            bPoints = zip([0.0, 0.25, 0.5, 0.75, 1.0], editor.curveB).map { CGPoint(x: $0, y: CGFloat($1)) }
        }
    }
}

#endif