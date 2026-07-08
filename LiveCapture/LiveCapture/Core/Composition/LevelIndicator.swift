//
//  LevelIndicator.swift
//  LiveCapture
//
//  实时水平仪/地平线指示器
//
//  ## 文件作用
//  使用 CMMotionManager 的陀螺仪和加速度计数据
//  实时监测设备倾斜角度并提供水平状态反馈
//  在设备达到水平时触发触觉反馈
//
//  ## 主要类
//
//  ### LevelIndicator
//  水平仪监控器（ObservableObject）
//
//  ## Published 属性
//  - rollAngle: CGFloat - 横滚角度 (-180° 到 180°)
//  - pitchAngle: CGFloat - 俯仰角度 (-90° 到 90°)
//  - isLevel: Bool - 是否水平 (roll ±1° 以内)
//  - levelDeviation: CGFloat - 偏离水平的度数
//
//  ## 工作原理
//  1. 启动 CMMotionManager 的 deviceMotion 更新（60Hz）
//  2. 从 attitude.roll 获取横滚角度
//  3. 从 attitude.pitch 获取俯仰角度
//  4. 当 roll 角度在 ±1° 以内时标记为水平
//  5. 首次达到水平时触发触觉反馈
//  6. 使用低通滤波器平滑角度数据
//
//  ## 性能优化
//  - 使用专用串行队列处理传感器数据
//  - 60Hz 更新频率平衡精度和功耗
//  - 平滑滤波减少抖动
//

import Foundation
import Combine
import CoreMotion
import CoreGraphics

#if os(iOS)
import UIKit

/// 实时水平仪/地平线指示器
final class LevelIndicator: ObservableObject {

	// MARK: - Published State

	/// 横滚角度（-180° 到 180°），反映设备绕前后轴的旋转
	@Published var rollAngle: CGFloat = 0

	/// 俯仰角度（-90° 到 90°），反映设备绕左右轴的旋转
	@Published var pitchAngle: CGFloat = 0

	/// 是否处于水平状态（roll 在 ±1° 以内）
	@Published var isLevel: Bool = false

	/// 偏离水平的度数（绝对值）
	@Published var levelDeviation: CGFloat = 0

	// MARK: - Private State

	private let motionManager = CMMotionManager()
	private let dataQueue = DispatchQueue(label: "livecapture.level.data", qos: .userInitiated)

	/// 水平阈值（度数）
	private let levelThreshold: CGFloat = 1.0

	/// 低通滤波系数 [0,1]，值越小越平滑但响应越慢
	private let smoothingFactor: CGFloat = 0.3

	/// 平滑后的 roll 角度
	private var smoothedRoll: CGFloat = 0

	/// 平滑后的 pitch 角度
	private var smoothedPitch: CGFloat = 0

	/// 是否曾经触发过水平触觉反馈（避免重复触发）
	private var didTriggerLevelHaptic: Bool = false

	/// 退出水平状态的计数器（避免频繁切换）
	private var offLevelCount: Int = 0
	private let offLevelThreshold: Int = 5

	// MARK: - Public API

	/// 启动水平仪监控
	func startMonitoring() {
		guard motionManager.isDeviceMotionAvailable else { return }

		motionManager.deviceMotionUpdateInterval = 1.0 / 60.0

		// 重置状态
		smoothedRoll = 0
		smoothedPitch = 0
		didTriggerLevelHaptic = false
		offLevelCount = 0

		motionManager.startDeviceMotionUpdates(
			using: .xArbitraryZVertical,
			to: OperationQueue()
		) { [weak self] motion, error in
			guard let self, let motion = motion, error == nil else { return }
			self.dataQueue.async {
				self.processMotionData(motion)
			}
		}
	}

	/// 停止水平仪监控
	func stopMonitoring() {
		motionManager.stopDeviceMotionUpdates()
		dataQueue.async {
			self.smoothedRoll = 0
			self.smoothedPitch = 0
			self.didTriggerLevelHaptic = false
			self.offLevelCount = 0
		}
		DispatchQueue.main.async {
			self.rollAngle = 0
			self.pitchAngle = 0
			self.isLevel = false
			self.levelDeviation = 0
		}
	}

	// MARK: - Motion Processing

	/// 处理原始设备运动数据
	private func processMotionData(_ motion: CMDeviceMotion) {
		// 获取原始角度（弧度转为度数）
		let rawRoll = CGFloat(motion.attitude.roll) * 180.0 / .pi
		let rawPitch = CGFloat(motion.attitude.pitch) * 180.0 / .pi

		// 低通滤波
		smoothedRoll = smoothedRoll + smoothingFactor * (rawRoll - smoothedRoll)
		smoothedPitch = smoothedPitch + smoothingFactor * (rawPitch - smoothedPitch)

		// 计算偏离水平度数
		let deviation = abs(smoothedRoll)
		let currentlyLevel = deviation <= levelThreshold

		// 防抖：需要连续多帧不在水平状态才切换
		if currentlyLevel {
			offLevelCount = 0
		} else {
			offLevelCount += 1
		}

		let shouldBeLevel: Bool
		if currentlyLevel {
			shouldBeLevel = true
		} else {
			shouldBeLevel = offLevelCount < offLevelThreshold
		}

		// 触觉反馈：首次进入水平状态
		if shouldBeLevel && !didTriggerLevelHaptic {
			didTriggerLevelHaptic = true
			triggerLevelHaptic()
		}

		// 离开水平状态时重置触觉标志
		if !shouldBeLevel {
			didTriggerLevelHaptic = false
		}

		// 发布到主线程
		DispatchQueue.main.async {
			self.rollAngle = self.smoothedRoll
			self.pitchAngle = self.smoothedPitch
			self.isLevel = shouldBeLevel
			self.levelDeviation = deviation
		}
	}

	// MARK: - Haptic Feedback

	/// 触发水平触觉反馈
	private func triggerLevelHaptic() {
		DispatchQueue.main.async {
			HapticManager.shared.soft()
		}
	}
}

#endif