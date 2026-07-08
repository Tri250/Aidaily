//
//  GestureHandler.swift
//  LiveCapture
//
//  统一手势系统 - 单指手势区域
//

import SwiftUI

#if os(iOS)

/// 手势处理回调
struct GestureCallbacks {
	var onSwipeUp: (() -> Void)?
	var onSwipeDown: (() -> Void)?
	var onLongPress: ((CGPoint) -> Void)?
	var onDoubleTap: (() -> Void)?
	var onPinch: ((CGFloat) -> Void)?
	var onPinchEnd: ((CGFloat) -> Void)?
	var onTap: ((CGPoint) -> Void)?
}

/// 统一手势修饰器
struct GestureHandler: ViewModifier {
	let callbacks: GestureCallbacks
	@State private var lastPinchScale: CGFloat = 1.0

	func body(content: Content) -> some View {
		content
			// 双击 - 切换摄像头
			.gesture(
				TapGesture(count: 2)
					.onEnded {
						HapticManager.shared.medium()
						callbacks.onDoubleTap?()
					}
			)
			// 单击 - 对焦
			.simultaneousGesture(
				TapGesture(count: 1)
					.onEnded {
						// 单击不消费，由外层 SpatialTapGesture 处理坐标
					}
			)
			// 长按 - 锁定对焦/曝光
			.gesture(
				LongPressGesture(minimumDuration: 0.5)
					.onEnded { _ in
						HapticManager.shared.focusLock()
						// 长按成功，通知对焦锁定
						callbacks.onLongPress?(.zero)
					}
			)
			// 捏合 - 变焦
			.simultaneousGesture(
				MagnificationGesture()
					.onChanged { scale in
						callbacks.onPinch?(scale)
					}
					.onEnded { scale in
						callbacks.onPinchEnd?(scale)
					}
			)
			// 上下滑动
			.simultaneousGesture(
				DragGesture(minimumDistance: 30)
					.onEnded { value in
						let vertical = value.translation.height
						let horizontal = value.translation.width

						if abs(vertical) > abs(horizontal) {
							if vertical < 0 {
								// 上滑 - 显示滤镜
								HapticManager.shared.light()
								callbacks.onSwipeUp?()
							} else {
								// 下滑 - 显示模式选择
								HapticManager.shared.soft()
								callbacks.onSwipeDown?()
							}
						}
					}
			)
	}
}

extension View {
	/// 应用统一手势系统
	func captureGestures(_ callbacks: GestureCallbacks) -> some View {
		self.modifier(GestureHandler(callbacks: callbacks))
	}
}

#endif