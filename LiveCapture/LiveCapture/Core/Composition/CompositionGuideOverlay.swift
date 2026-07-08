//
//  CompositionGuideOverlay.swift
//  LiveCapture
//
//  辅助构图叠加层：九宫格、黄金螺线、水平仪、方形模式、直方图、斑马纹
//

import SwiftUI

#if os(iOS)

// MARK: - 构图引导类型

enum CompositionGuideType: String, CaseIterable {
    case grid = "九宫格"
    case goldenRatio = "黄金分割"
    case goldenSpiral = "黄金螺线"
    case diagonal = "对角线"
    case crosshair = "十字准星"
    case square = "方形构图"
    case none = "关闭"

    var icon: String {
        switch self {
        case .grid: return "grid"
        case .goldenRatio: return "rectangle.split.3x3"
        case .goldenSpiral: return "spiral"
        case .diagonal: return "line.diagonal"
        case .crosshair: return "plus"
        case .square: return "square"
        case .none: return "xmark"
        }
    }
}

// MARK: - 构图引导叠加层

struct CompositionGuideOverlay: View {
    let guideType: CompositionGuideType
    let compositionRect: CGRect
    let isActive: Bool

    var body: some View {
        GeometryReader { geo in
            let rect = compositionRect != .zero ? compositionRect : geo.frame(in: .local)

            ZStack {
                switch guideType {
                case .grid:
                    ruleOfThirdsGrid(rect: rect)
                case .goldenRatio:
                    goldenRatioGrid(rect: rect)
                case .goldenSpiral:
                    goldenSpiralPath(rect: rect)
                case .diagonal:
                    diagonalLines(rect: rect)
                case .crosshair:
                    crosshair(rect: rect)
                case .square:
                    squareOverlay(rect: rect)
                case .none:
                    EmptyView()
                }
            }
            .opacity(isActive ? 0.60 : 0)
            .animation(DesignSystem.Animation.overlayFade, value: isActive)
        }
        .allowsHitTesting(false)
    }

    // MARK: - 九宫格

    private func ruleOfThirdsGrid(rect: CGRect) -> some View {
        Path { path in
            let w = rect.width
            let h = rect.height
            let x = rect.minX
            let y = rect.minY

            // 垂直线
            path.move(to: CGPoint(x: x + w / 3, y: y))
            path.addLine(to: CGPoint(x: x + w / 3, y: y + h))
            path.move(to: CGPoint(x: x + w * 2 / 3, y: y))
            path.addLine(to: CGPoint(x: x + w * 2 / 3, y: y + h))

            // 水平线
            path.move(to: CGPoint(x: x, y: y + h / 3))
            path.addLine(to: CGPoint(x: x + w, y: y + h / 3))
            path.move(to: CGPoint(x: x, y: y + h * 2 / 3))
            path.addLine(to: CGPoint(x: x + w, y: y + h * 2 / 3))
        }
        .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
    }

    // MARK: - 黄金分割

    private func goldenRatioGrid(rect: CGRect) -> some View {
        let phi: CGFloat = 1.618
        Path { path in
            let w = rect.width
            let h = rect.height
            let x = rect.minX
            let y = rect.minY

            let gx = w / phi
            let gy = h / phi

            path.move(to: CGPoint(x: x + gx, y: y))
            path.addLine(to: CGPoint(x: x + gx, y: y + h))
            path.move(to: CGPoint(x: x + w - gx, y: y))
            path.addLine(to: CGPoint(x: x + w - gx, y: y + h))
            path.move(to: CGPoint(x: x, y: y + gy))
            path.addLine(to: CGPoint(x: x + w, y: y + gy))
            path.move(to: CGPoint(x: x, y: y + h - gy))
            path.addLine(to: CGPoint(x: x + w, y: y + h - gy))
        }
        .stroke(Color.white.opacity(0.4), lineWidth: 0.5)
    }

    // MARK: - 黄金螺线

    private func goldenSpiralPath(rect: CGRect) -> some View {
        let phi: CGFloat = 1.618
        Path { path in
            var r = min(rect.width, rect.height) * 0.9
            let cx = rect.midX
            let cy = rect.midY
            var angle: CGFloat = 0
            let steps = 20

            for i in 0..<steps {
                let x = cx + r * cos(angle)
                let y = cy + r * sin(angle)
                if i == 0 {
                    path.move(to: CGPoint(x: x, y: y))
                } else {
                    path.addLine(to: CGPoint(x: x, y: y))
                }
                angle += .pi / 4
                r /= pow(phi, 0.25)
            }
        }
        .stroke(Color.white.opacity(0.45), style: StrokeStyle(lineWidth: 1.0, lineCap: .round))
    }

    // MARK: - 对角线

    private func diagonalLines(rect: CGRect) -> some View {
        Path { path in
            path.move(to: CGPoint(x: rect.minX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
            path.move(to: CGPoint(x: rect.maxX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        }
        .stroke(Color.white.opacity(0.4), lineWidth: 0.5)
    }

    // MARK: - 十字准星

    private func crosshair(rect: CGRect) -> some View {
        Path { path in
            let cx = rect.midX
            let cy = rect.midY
            let len: CGFloat = 24

            path.move(to: CGPoint(x: cx - len, y: cy))
            path.addLine(to: CGPoint(x: cx + len, y: cy))
            path.move(to: CGPoint(x: cx, y: cy - len))
            path.addLine(to: CGPoint(x: cx, y: cy + len))
        }
        .stroke(Color.white.opacity(0.5), style: StrokeStyle(lineWidth: 0.5, dash: [4, 4]))
    }

    // MARK: - 方形构图

    private func squareOverlay(rect: CGRect) -> some View {
        let size = min(rect.width, rect.height)
        let squareRect = CGRect(
            x: rect.midX - size / 2,
            y: rect.midY - size / 2,
            width: size,
            height: size
        )
        Path { path in
            path.addRect(squareRect)
        }
        .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
    }
}

// MARK: - 水平仪

struct LevelIndicator: View {
    let roll: Double  // 弧度
    let isActive: Bool

    var body: some View {
        VStack(spacing: 0) {
            // 水平线
            ZStack {
                // 参考线
                Rectangle()
                    .fill(Color.white.opacity(0.3))
                    .frame(width: 120, height: 1)

                // 活动线
                Rectangle()
                    .fill(isActive ? Color.green : Color.white.opacity(0.5))
                    .frame(width: 120, height: 1.5)
                    .rotationEffect(.radians(roll))
                    .animation(DesignSystem.Animation.quick, value: roll)
            }
            .frame(width: 120, height: 40)

            // 角度指示
            HStack(spacing: 0) {
                Text("\(String(format: "%.1f", abs(roll * 180 / .pi)))°")
                    .font(DesignSystem.Typography.monoCaption)
                    .foregroundColor(isActive ? .green : Color.white.opacity(0.5))
            }
        }
        .padding(6)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.black.opacity(0.5))
        )
        .opacity(isActive ? 1 : 0.5)
    }
}

// MARK: - 直方图

struct HistogramView: View {
    let data: [Float]
    let isActive: Bool

    var body: some View {
        GeometryReader { geo in
            let maxVal = data.max() ?? 1
            let width = geo.size.width / CGFloat(data.count)

            HStack(spacing: 0) {
                ForEach(0..<min(data.count, 128), id: \.self) { i in
                    let value = data.count > 128 ? data[i * 2] : data[i]
                    let height = maxVal > 0 ? CGFloat(value / maxVal) * geo.size.height : 0

                    Rectangle()
                        .fill(Color.white.opacity(0.6))
                        .frame(width: width * (data.count > 128 ? 2 : 1), height: max(height, 1))
                }
            }
        }
        .frame(height: 40)
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 6)
                .fill(Color.black.opacity(0.5))
        )
        .opacity(isActive ? 0.85 : 0)
    }
}

// MARK: - 斑马纹

struct ZebraOverlay: View {
    let pixelBuffer: CVPixelBuffer?
    let threshold: Float
    let isActive: Bool

    var body: some View {
        if isActive, let pb = pixelBuffer {
            // 简化的斑马纹实现：在 Metal/CoreImage 层处理
            // 此处为 SwiftUI 占位，实际渲染使用 Metal Shader
            Color.clear
                .overlay(
                    Text("ZEBRA")
                        .font(.system(size: 8))
                        .foregroundColor(Color.red.opacity(0.3))
                        .rotationEffect(.degrees(45))
                        .opacity(0.15)
                )
        }
    }
}

#endif