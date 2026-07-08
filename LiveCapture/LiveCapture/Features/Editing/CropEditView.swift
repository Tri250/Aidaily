//
//  CropEditView.swift
//  LiveCapture
//
//  裁剪编辑视图 - 手势裁剪、旋转、翻转
//

import SwiftUI
import CoreImage

#if os(iOS)

/// 裁剪比例预设
enum CropAspectPreset: String, CaseIterable, Identifiable {
    case original = "原始"
    case square = "1:1"
    case ratio3_4 = "3:4"
    case ratio4_3 = "4:3"
    case ratio9_16 = "9:16"
    case ratio16_9 = "16:9"
    case ratio3_2 = "3:2"
    case ratio2_3 = "2:3"

    var id: String { rawValue }

    var ratio: CGFloat? {
        switch self {
        case .original: return nil
        case .square: return 1.0
        case .ratio3_4: return 3.0 / 4.0
        case .ratio4_3: return 4.0 / 3.0
        case .ratio9_16: return 9.0 / 16.0
        case .ratio16_9: return 16.0 / 9.0
        case .ratio3_2: return 3.0 / 2.0
        case .ratio2_3: return 2.0 / 3.0
        }
    }
}

struct CropEditView: View {
    @ObservedObject var viewModel: PhotoEditorViewModel
    @State private var cropRect: CGRect = .zero
    @State private var rotation: Double = 0
    @State private var flipHorizontal: Bool = false
    @State private var selectedRatio: CropAspectPreset = .original
    @State private var isDragging = false
    @State private var dragStartPoint: CGPoint = .zero
    @State private var initialCropRect: CGRect = .zero
    @State private var cornerDrag: CropCornerDrag?
    @State private var showGrid: Bool = true

    private let minCropSize: CGFloat = 50

    enum CropCornerDrag {
        case topLeft, topRight, bottomLeft, bottomRight
    }

    var body: some View {
        VStack(spacing: 0) {
            // 裁剪预览区域
            GeometryReader { geo in
                ZStack {
                    Color.black
                        .ignoresSafeArea()

                    // 裁剪后的图像预览
                    if let image = viewModel.originalImage {
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(maxWidth: geo.size.width, maxHeight: geo.size.height)
                            .rotationEffect(.degrees(rotation))
                            .scaleEffect(x: flipHorizontal ? -1 : 1, y: 1)
                            .overlay(
                                // 暗色遮罩
                                cropOverlay(size: geo.size, imageSize: imageSize(in: geo.size))
                            )
                            .clipped()
                    }

                    // 裁剪框
                    cropRectView(size: geo.size, imageSize: imageSize(in: geo.size))
                        .allowsHitTesting(true)

                    // 网格覆盖
                    if showGrid {
                        gridOverlay(size: geo.size)
                    }
                }
                .onAppear {
                    initializeCropRect(in: geo.size)
                }
            }
            .frame(maxHeight: UIScreen.main.bounds.height * 0.55)

            // 底部控制面板
            VStack(spacing: DesignSystem.Spacing.small) {
                // 比例选择
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DesignSystem.Spacing.xxSmall) {
                        ForEach(CropAspectPreset.allCases) { preset in
                            Button {
                                withAnimation(DesignSystem.Animation.quick) {
                                    selectedRatio = preset
                                    // 需要重新计算裁剪框
                                }
                            } label: {
                                Text(preset.rawValue)
                                    .font(DesignSystem.Typography.caption1)
                                    .foregroundColor(selectedRatio == preset ? .white : DesignSystem.Colors.minimalSecondaryLabel)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(
                                        Capsule()
                                            .fill(selectedRatio == preset ? DesignSystem.Colors.primary : Color.white.opacity(0.1))
                                    )
                            }
                        }
                    }
                    .padding(.horizontal, DesignSystem.Spacing.small)
                }

                // 旋转滑块
                HStack(spacing: DesignSystem.Spacing.small) {
                    Image(systemName: "rotate.left")
                        .font(.system(size: 14))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)

                    Slider(value: $rotation, in: -45...45, step: 0.5)
                        .tint(DesignSystem.Colors.primary)
                        .onChange(of: rotation) { _ in
                            viewModel.applyCrop(rect: cropRect, rotation: rotation, flip: flipHorizontal)
                        }

                    Image(systemName: "rotate.right")
                        .font(.system(size: 14))
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)

                    Text("\(Int(rotation))°")
                        .font(DesignSystem.Typography.caption1)
                        .foregroundColor(DesignSystem.Colors.minimalLabel)
                        .frame(width: 36)
                }
                .padding(.horizontal, DesignSystem.Spacing.small)

                // 底部按钮行
                HStack(spacing: DesignSystem.Spacing.xLarge) {
                    // 翻转
                    Button {
                        withAnimation(DesignSystem.Animation.quick) {
                            flipHorizontal.toggle()
                            viewModel.applyCrop(rect: cropRect, rotation: rotation, flip: flipHorizontal)
                        }
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: "arrow.left.and.right.righttriangle.left.righttriangle.right")
                                .font(.system(size: 20))
                            Text("翻转")
                                .font(DesignSystem.Typography.caption2)
                        }
                        .foregroundColor(flipHorizontal ? DesignSystem.Colors.primary : DesignSystem.Colors.minimalSecondaryLabel)
                    }

                    Spacer()

                    // 网格
                    Button {
                        withAnimation(DesignSystem.Animation.quick) {
                            showGrid.toggle()
                        }
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: "grid")
                                .font(.system(size: 20))
                            Text("网格")
                                .font(DesignSystem.Typography.caption2)
                        }
                        .foregroundColor(showGrid ? DesignSystem.Colors.primary : DesignSystem.Colors.minimalSecondaryLabel)
                    }

                    Spacer()

                    // 重置
                    Button {
                        withAnimation(DesignSystem.Animation.quick) {
                            rotation = 0
                            flipHorizontal = false
                            selectedRatio = .original
                            viewModel.applyCrop(rect: nil, rotation: 0, flip: false)
                        }
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: "arrow.counterclockwise")
                                .font(.system(size: 20))
                            Text("重置")
                                .font(DesignSystem.Typography.caption2)
                        }
                        .foregroundColor(DesignSystem.Colors.minimalSecondaryLabel)
                    }
                }
                .padding(.horizontal, DesignSystem.Spacing.xLarge)
                .padding(.bottom, DesignSystem.Spacing.xxSmall)
            }
            .padding(.top, DesignSystem.Spacing.small)
            .background(Color.black)
        }
        .background(Color.black)
    }

    // MARK: - 裁剪覆盖层

    private func cropOverlay(size: CGSize, imageSize: CGSize) -> some View {
        Rectangle()
            .fill(Color.black.opacity(0.6))
            .mask(
                ZStack {
                    Rectangle()
                        .fill(Color.white)
                    // 裁剪区域透明
                    Rectangle()
                        .fill(Color.black)
                        .frame(width: cropRect.width, height: cropRect.height)
                        .position(x: cropRect.midX, y: cropRect.midY)
                }
            )
            .allowsHitTesting(false)
    }

    // MARK: - 裁剪框

    private func cropRectView(size: CGSize, imageSize: CGSize) -> some View {
        ZStack {
            // 裁剪边框
            RoundedRectangle(cornerRadius: 2)
                .strokeBorder(Color.white, lineWidth: 1.5)
                .frame(width: cropRect.width, height: cropRect.height)
                .position(x: cropRect.midX, y: cropRect.midY)

            // 四角拖拽手柄
            ForEach(cornerPositions(), id: \.0) { corner, position in
                Circle()
                    .fill(.white)
                    .frame(width: 24, height: 24)
                    .position(position)
                    .gesture(
                        DragGesture()
                            .onChanged { value in
                                handleCornerDrag(corner: corner, translation: value.translation, size: size)
                            }
                            .onEnded { _ in
                                applyCropToEditor()
                            }
                    )
            }
        }
        .gesture(
            DragGesture()
                .onChanged { value in
                    if !isDragging {
                        isDragging = true
                        initialCropRect = cropRect
                    }
                    let tx = value.translation.width
                    let ty = value.translation.height
                    var newRect = initialCropRect.offsetBy(dx: tx, dy: ty)

                    // 限制在图片范围内
                    let imgRect = CGRect(origin: .zero, size: size)
                    if newRect.minX < 0 { newRect.origin.x = 0 }
                    if newRect.minY < 0 { newRect.origin.y = 0 }
                    if newRect.maxX > imgRect.maxX { newRect.origin.x = imgRect.maxX - newRect.width }
                    if newRect.maxY > imgRect.maxY { newRect.origin.y = imgRect.maxY - newRect.height }

                    cropRect = newRect
                }
                .onEnded { _ in
                    isDragging = false
                    applyCropToEditor()
                }
        )
    }

    private func cornerPositions() -> [(CropCornerDrag, CGPoint)] {
        [
            (.topLeft, CGPoint(x: cropRect.minX, y: cropRect.minY)),
            (.topRight, CGPoint(x: cropRect.maxX, y: cropRect.minY)),
            (.bottomLeft, CGPoint(x: cropRect.minX, y: cropRect.maxY)),
            (.bottomRight, CGPoint(x: cropRect.maxX, y: cropRect.maxY))
        ]
    }

    private func handleCornerDrag(corner: CropCornerDrag, translation: CGSize, size: CGSize) {
        var newRect = cropRect

        switch corner {
        case .topLeft:
            newRect.origin.x = max(0, cropRect.origin.x + translation.width)
            newRect.origin.y = max(0, cropRect.origin.y + translation.height)
            newRect.size.width = max(minCropSize, cropRect.maxX - newRect.origin.x)
            newRect.size.height = max(minCropSize, cropRect.maxY - newRect.origin.y)
        case .topRight:
            newRect.size.width = max(minCropSize, cropRect.width + translation.width)
            newRect.origin.y = max(0, cropRect.origin.y + translation.height)
            newRect.size.height = max(minCropSize, cropRect.maxY - newRect.origin.y)
            if newRect.maxX > size.width { newRect.size.width = size.width - newRect.origin.x }
        case .bottomLeft:
            newRect.origin.x = max(0, cropRect.origin.x + translation.width)
            newRect.size.width = max(minCropSize, cropRect.maxX - newRect.origin.x)
            newRect.size.height = max(minCropSize, cropRect.height + translation.height)
            if newRect.maxY > size.height { newRect.size.height = size.height - newRect.origin.y }
        case .bottomRight:
            newRect.size.width = max(minCropSize, cropRect.width + translation.width)
            newRect.size.height = max(minCropSize, cropRect.height + translation.height)
            if newRect.maxX > size.width { newRect.size.width = size.width - newRect.origin.x }
            if newRect.maxY > size.height { newRect.size.height = size.height - newRect.origin.y }
        }

        cropRect = newRect
    }

    // MARK: - 网格覆盖

    private func gridOverlay(size: CGSize) -> some View {
        ZStack {
            // 三分线
            Path { path in
                let w = cropRect.width / 3
                let h = cropRect.height / 3
                for i in 1..<3 {
                    let x = cropRect.minX + w * CGFloat(i)
                    path.move(to: CGPoint(x: x, y: cropRect.minY))
                    path.addLine(to: CGPoint(x: x, y: cropRect.maxY))
                }
                for i in 1..<3 {
                    let y = cropRect.minY + h * CGFloat(i)
                    path.move(to: CGPoint(x: cropRect.minX, y: y))
                    path.addLine(to: CGPoint(x: cropRect.maxX, y: y))
                }
            }
            .stroke(Color.white.opacity(0.3), lineWidth: 0.5)
            .allowsHitTesting(false)
        }
    }

    // MARK: - 辅助方法

    private func imageSize(in containerSize: CGSize) -> CGSize {
        guard let image = viewModel.originalImage else { return containerSize }
        let imageRatio = image.size.width / image.size.height
        let containerRatio = containerSize.width / containerSize.height

        if imageRatio > containerRatio {
            let width = containerSize.width
            let height = width / imageRatio
            return CGSize(width: width, height: height)
        } else {
            let height = containerSize.height
            let width = height * imageRatio
            return CGSize(width: width, height: height)
        }
    }

    private func initializeCropRect(in containerSize: CGSize) {
        let imgSize = imageSize(in: containerSize)
        let originX = (containerSize.width - imgSize.width) / 2
        let originY = (containerSize.height - imgSize.height) / 2
        cropRect = CGRect(origin: CGPoint(x: originX, y: originY), size: imgSize)
    }

    private func applyCropToEditor() {
        viewModel.applyCrop(rect: cropRect, rotation: rotation, flip: flipHorizontal)
    }
}

#endif