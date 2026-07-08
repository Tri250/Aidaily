//
//  PortraitModels.swift
//  LiveCapture
//
//  人像模式数据模型
//
//  ## 文件作用
//  定义人像模式所需的所有数据模型，包括光效类型、美颜参数、
//  虚化参数和人像检测结果的结构化定义。
//
//  ## 主要类型
//  - PortraitLightingType: 人像光效类型枚举
//  - BeautyParams: 美颜参数结构体
//  - BokehParams: 虚化（背景模糊）参数结构体
//  - PortraitResult: 人像检测结果结构体
//

import Foundation
import CoreGraphics
import CoreImage

#if os(iOS)

// MARK: - PortraitLightingType

/// 人像光效类型
enum PortraitLightingType: String, CaseIterable, Codable {
    case natural         // 自然光
    case studioLight     // 摄影室灯光
    case contourLight    // 轮廓光
    case stageLight      // 舞台光
    case stageLightMono  // 舞台光黑白

    var displayName: String {
        switch self {
        case .natural:        return "自然光"
        case .studioLight:    return "摄影室灯光"
        case .contourLight:   return "轮廓光"
        case .stageLight:     return "舞台光"
        case .stageLightMono: return "舞台光黑白"
        }
    }

    var iconName: String {
        switch self {
        case .natural:        return "sun.max"
        case .studioLight:    return "studio.light"
        case .contourLight:   return "circle.lefthalf.filled"
        case .stageLight:     return "spotlight.right"
        case .stageLightMono: return "circle.dotted"
        }
    }
}

// MARK: - BeautyParams

/// 美颜参数
struct BeautyParams: Codable {
    /// 磨皮强度 0-1
    var skinSmoothing: Float = 0.3
    /// 肤色调整 -1（冷白）到 1（暖黄）
    var skinTone: Float = 0.0
    /// 亮眼强度 0-1
    var eyeBrightening: Float = 0.2
    /// 牙齿美白强度 0-1
    var teethWhitening: Float = 0.0
    /// 瘦脸强度 0-1
    var faceSlimming: Float = 0.0
    /// 祛痘强度 0-1
    var blemishRemoval: Float = 0.3

    /// 默认美颜参数
    static let `default` = BeautyParams()
    /// 关闭所有美颜
    static let off = BeautyParams(
        skinSmoothing: 0,
        skinTone: 0,
        eyeBrightening: 0,
        teethWhitening: 0,
        faceSlimming: 0,
        blemishRemoval: 0
    )

    /// 是否所有美颜效果都已关闭
    var isOff: Bool {
        skinSmoothing == 0 && skinTone == 0 && eyeBrightening == 0
            && teethWhitening == 0 && faceSlimming == 0 && blemishRemoval == 0
    }
}

// MARK: - BokehParams

/// 虚化参数
struct BokehParams: Codable {
    /// 模拟光圈值 f/1.4 - f/16
    var aperture: Float = 2.8
    /// 虚化光斑形状
    var bokehShape: BokehShape = .circle
    /// 虚化强度 0-1
    var intensity: Float = 1.0

    /// 虚化光斑形状
    enum BokehShape: String, CaseIterable, Codable {
        case circle
        case hexagon
        case heart
        case star

        var displayName: String {
            switch self {
            case .circle:  return "圆形"
            case .hexagon: return "六边形"
            case .heart:   return "心形"
            case .star:    return "星形"
            }
        }

        var iconName: String {
            switch self {
            case .circle:  return "circle.fill"
            case .hexagon: return "hexagon.fill"
            case .heart:   return "heart.fill"
            case .star:    return "star.fill"
            }
        }
    }

    /// 根据光圈值计算模糊半径（映射 f/1.4 → 半径 30, f/16 → 半径 3）
    var blurRadius: Float {
        let clampedAperture = max(1.4, min(16.0, aperture))
        let normalized = (clampedAperture - 1.4) / (16.0 - 1.4)
        let radius = 30.0 - normalized * 27.0
        return Float(radius) * intensity
    }
}

// MARK: - PortraitResult

/// 人像模式检测结果
struct PortraitResult {
    /// 原始图像（CIImage 格式）
    let originalImage: CIImage
    /// 深度数据掩码（可选，双摄设备可用）
    let depthData: CIImage?
    /// 皮肤区域掩码
    let skinMask: CIImage?
    /// 面部关键点坐标（相对于图像坐标）
    let faceLandmarks: [CGPoint]
    /// 是否检测到人像
    let hasPortrait: Bool

    init(
        originalImage: CIImage,
        depthData: CIImage? = nil,
        skinMask: CIImage? = nil,
        faceLandmarks: [CGPoint] = [],
        hasPortrait: Bool = false
    ) {
        self.originalImage = originalImage
        self.depthData = depthData
        self.skinMask = skinMask
        self.faceLandmarks = faceLandmarks
        self.hasPortrait = hasPortrait
    }
}

#endif