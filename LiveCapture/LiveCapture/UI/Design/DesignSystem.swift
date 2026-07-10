//
//  DesignSystem.swift
//  LiveCapture
//
//  统一设计系统 - 魅族极简风格
//  严格遵循魅族 Flyme 设计语言：低饱和、大留白、纤细字体、克制动效
//

#if os(iOS)
import SwiftUI
import UIKit

// MARK: - 设计系统

enum DesignSystem {

    // MARK: - Colors（魅族极简色板）

    enum Colors {
        // 品牌色 - 低饱和清新蓝（魅族 Flyme 风格）
        static let primary = Color(red: 0.23, green: 0.51, blue: 0.96)      // #3B82F6
        static let primaryLight = Color(red: 0.58, green: 0.76, blue: 0.99)  // 亮变体
        static let secondary = Color(red: 0.39, green: 0.40, blue: 0.93)     // #6366F1 Indigo
        static let accent = Color(red: 0.96, green: 0.58, blue: 0.30)       // 柔和橙

        // 语义色 - 低饱和版
        static let success = Color(red: 0.28, green: 0.72, blue: 0.44)       // 柔和绿
        static let successBg = Color(red: 0.28, green: 0.72, blue: 0.44).opacity(0.12)
        static let warning = Color(red: 0.96, green: 0.70, blue: 0.24)       // 柔和琥珀
        static let warningBg = Color(red: 0.96, green: 0.70, blue: 0.24).opacity(0.12)
        static let error = Color(red: 0.94, green: 0.35, blue: 0.35)         // 柔和红
        static let errorBg = Color(red: 0.94, green: 0.35, blue: 0.35).opacity(0.12)
        static let info = Color(red: 0.35, green: 0.70, blue: 0.94)          // 柔和天蓝

        // 中性色阶 - 7 级灰度（魅族极简核心）
        static let gray0 = Color(uiColor: UIColor { trait in   // 最浅 - 背景
            trait.userInterfaceStyle == .dark ? UIColor(white: 0.08, alpha: 1) : UIColor(white: 0.98, alpha: 1)
        })
        static let gray1 = Color(uiColor: UIColor { trait in   // 次级背景
            trait.userInterfaceStyle == .dark ? UIColor(white: 0.12, alpha: 1) : UIColor(white: 0.94, alpha: 1)
        })
        static let gray2 = Color(uiColor: UIColor { trait in   // 卡片/Surface
            trait.userInterfaceStyle == .dark ? UIColor(white: 0.16, alpha: 1) : UIColor(white: 0.90, alpha: 1)
        })
        static let gray3 = Color(uiColor: UIColor { trait in   // 分割线
            trait.userInterfaceStyle == .dark ? UIColor(white: 0.22, alpha: 1) : UIColor(white: 0.82, alpha: 1)
        })
        static let gray4 = Color(uiColor: UIColor { trait in   // 禁用/占位
            trait.userInterfaceStyle == .dark ? UIColor(white: 0.35, alpha: 1) : UIColor(white: 0.55, alpha: 1)
        })
        static let gray5 = Color(uiColor: UIColor { trait in   // 次要文字
            trait.userInterfaceStyle == .dark ? UIColor(white: 0.55, alpha: 1) : UIColor(white: 0.35, alpha: 1)
        })
        static let gray6 = Color(uiColor: UIColor { trait in   // 主要文字
            trait.userInterfaceStyle == .dark ? UIColor(white: 0.92, alpha: 1) : UIColor(white: 0.08, alpha: 1)
        })

        // 语义化文字颜色（兼容旧代码）
        static let textPrimary = gray6
        static let textSecondary = gray5
        static let textTertiary = gray4

        // 语义化背景颜色（兼容旧代码）
        static let backgroundPrimary = gray0
        static let backgroundSecondary = gray1
        static let backgroundTertiary = gray2

        // 品牌渐变
        static let primaryGradient = LinearGradient(
            colors: [primary, secondary],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )

        // 极简相机专属色
        static let minimalBackground = Color.black
        static let minimalOverlay = Color.white.opacity(0.08)
        static let minimalBorder = Color.white.opacity(0.20)
        static let minimalActiveBorder = Color.white.opacity(0.85)
        static let minimalLabel = Color.white.opacity(0.92)
        static let minimalSecondaryLabel = Color.white.opacity(0.50)
        static let minimalDarkOverlay = Color.black.opacity(0.40)
        static let shutterStroke = Color.white
        static let shutterInner = Color.white.opacity(0.95)
    }

    // MARK: - Typography（魅族级字体系统）

    enum Typography {
        // 字体族
        // 统一使用系统字体 SF Pro（苹方），不再混用 .rounded 和 .default
        // 标题保留 .rounded 设计风格，正文统一 .default

        // 标题 - Rounded 设计风格
        static let largeTitle  = Font.system(size: 34, weight: .bold, design: .rounded)
        static let title1      = Font.system(size: 28, weight: .bold, design: .rounded)
        static let title2      = Font.system(size: 22, weight: .bold, design: .rounded)
        static let title3      = Font.system(size: 20, weight: .semibold, design: .rounded)

        // 正文 - 统一 Default 设计风格
        static let headline    = Font.system(size: 17, weight: .semibold, design: .default)
        static let body        = Font.system(size: 17, weight: .regular, design: .default)
        static let callout     = Font.system(size: 16, weight: .regular, design: .default)
        static let subheadline = Font.system(size: 15, weight: .regular, design: .default)
        static let footnote    = Font.system(size: 13, weight: .regular, design: .default)
        static let caption1    = Font.system(size: 12, weight: .regular, design: .default)
        static let caption2    = Font.system(size: 11, weight: .regular, design: .default)

        // 等宽数字（用于 EXIF 数据、计时器）
        static let monoBody    = Font.system(size: 17, weight: .regular, design: .monospaced)
        static let monoCaption = Font.system(size: 13, weight: .medium, design: .monospaced)
        static let monoDigit   = Font.system(size: 12, weight: .regular, design: .monospaced)

        // 行高（Line Height）- 基于字体大小的 1.4 倍
        static func lineHeight(for size: CGFloat) -> CGFloat { size * 1.4 }

        // 字间距（Kerning）
        // 标题：轻微负字间距提升紧凑感
        static let titleKerning: CGFloat = -0.3
        // 正文：保持默认
        static let bodyKerning: CGFloat = 0
        // 说明文字：轻微正字间距提升可读性
        static let captionKerning: CGFloat = 0.2

        // 极简模式专用字体
        static let minimalModeLabel     = Font.system(size: 11, weight: .medium, design: .default)
        static let minimalFilterName    = Font.system(size: 10, weight: .medium, design: .default)
        static let minimalControlLabel  = Font.system(size: 13, weight: .medium, design: .default)
        static let minimalTimer         = Font.system(size: 14, weight: .medium, design: .monospaced)
        static let minimalZoomIndicator = Font.system(size: 12, weight: .regular, design: .monospaced)
    }

    // MARK: - Spacing（严格 4pt 基准网格）

    enum Spacing {
        // 所有间距值为 4 的倍数
        static let xxxSmall: CGFloat = 4
        static let xxSmall: CGFloat  = 8
        static let xSmall: CGFloat   = 12
        static let small: CGFloat    = 16
        static let medium: CGFloat   = 20
        static let large: CGFloat    = 24
        static let xLarge: CGFloat   = 32
        static let xxLarge: CGFloat  = 48
        static let xxxLarge: CGFloat = 64

        // 水平间距规范
        enum Horizontal {
            static let tight: CGFloat   = 4
            static let compact: CGFloat = 8
            static let standard: CGFloat = 12
            static let relaxed: CGFloat = 16
            static let loose: CGFloat   = 24
        }

        // 垂直间距规范
        enum Vertical {
            static let tight: CGFloat   = 4
            static let compact: CGFloat = 8
            static let standard: CGFloat = 12
            static let relaxed: CGFloat = 16
            static let loose: CGFloat   = 24
        }

        // 内边距规范
        enum Padding {
            static let inline: CGFloat  = 16
            static let block: CGFloat   = 20
            static let container: CGFloat = 24
        }

        // 元素间距规范
        enum Gap {
            static let minimal: CGFloat = 4
            static let tight: CGFloat   = 8
            static let standard: CGFloat = 12
            static let relaxed: CGFloat = 16
        }
    }

    // MARK: - Corner Radius（统一圆角系统）

    enum CornerRadius {
        // 从小到大的圆角层级
        static let micro: CGFloat  = 4
        static let small: CGFloat  = 8
        static let medium: CGFloat = 12
        static let large: CGFloat  = 16
        static let xLarge: CGFloat = 20
        static let xxLarge: CGFloat = 24
        static let circle: CGFloat = 999

        // 嵌套圆角规则：内层 = 外层 - 差值
        // 差值默认 4pt
        static func nested(outer: CGFloat) -> CGFloat {
            max(outer - 4, 4)
        }
    }

    // MARK: - Shadows（柔和阴影系统）

    enum Shadows {
        // 轻微阴影 - 卡片
        static func subtle() -> (color: Color, radius: CGFloat, x: CGFloat, y: CGFloat) {
            (Color.black.opacity(0.06), 8, 0, 2)
        }
        // 中等阴影 - 浮动元素
        static func elevated() -> (color: Color, radius: CGFloat, x: CGFloat, y: CGFloat) {
            (Color.black.opacity(0.10), 16, 0, 4)
        }
        // 深度阴影 - 模态
        static func modal() -> (color: Color, radius: CGFloat, x: CGFloat, y: CGFloat) {
            (Color.black.opacity(0.15), 24, 0, 8)
        }
        // 发光效果
        static func glow(color: Color) -> (color: Color, radius: CGFloat, x: CGFloat, y: CGFloat) {
            (color.opacity(0.4), 12, 0, 0)
        }
    }

    // MARK: - Stroke（描边系统）

    enum Stroke {
        static let subtle = Color.white.opacity(0.10)
        static let standard = Color.white.opacity(0.20)
        static let prominent = Color.white.opacity(0.35)
        static let active = Color.white.opacity(0.85)

        static let widthThin: CGFloat = 0.5
        static let widthStandard: CGFloat = 1.0
        static let widthThick: CGFloat = 1.5
        static let widthHeavy: CGFloat = 2.0
    }

    // MARK: - Animation（魅族级别克制动效）

    enum Animation {
        // 基础缓动
        static let easeIn    = SwiftUI.Animation.easeIn(duration: 0.2)
        static let easeOut   = SwiftUI.Animation.easeOut(duration: 0.2)
        static let easeInOut = SwiftUI.Animation.easeInOut(duration: 0.25)

        // 弹簧动画 - 魅族偏好柔软弹性
        static let quick     = SwiftUI.Animation.spring(response: 0.25, dampingFraction: 0.75)
        static let smooth    = SwiftUI.Animation.spring(response: 0.35, dampingFraction: 0.72)
        static let bouncy    = SwiftUI.Animation.spring(response: 0.40, dampingFraction: 0.65)
        static let gentle    = SwiftUI.Animation.spring(response: 0.50, dampingFraction: 0.85)

        // 相机专用弹簧动画
        static let shutterPress   = SwiftUI.Animation.spring(response: 0.18, dampingFraction: 0.65)
        static let shutterRelease = SwiftUI.Animation.spring(response: 0.28, dampingFraction: 0.70)
        static let overlayFade    = SwiftUI.Animation.easeInOut(duration: 0.25)
        static let modeSlide      = SwiftUI.Animation.spring(response: 0.35, dampingFraction: 0.78)
        static let filterReveal   = SwiftUI.Animation.spring(response: 0.32, dampingFraction: 0.72)
        static let zoomPop        = SwiftUI.Animation.spring(response: 0.22, dampingFraction: 0.65)
        static let snappy         = SwiftUI.Animation.spring(response: 0.22, dampingFraction: 0.72)

        // 自动隐藏延迟
        static let autoHideDelay: Double = 3.0
    }
}

// MARK: - View Modifiers（核心视觉修饰器）

// MARK: 毛玻璃效果
struct GlassmorphismModifier: ViewModifier {
    var cornerRadius: CGFloat = DesignSystem.CornerRadius.medium
    var opacity: Double = 0.08

    func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(.ultraThinMaterial)
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .fill(Color.white.opacity(opacity))
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .strokeBorder(
                        DesignSystem.Stroke.subtle,
                        lineWidth: DesignSystem.Stroke.widthThin
                    )
            )
    }
}

// MARK: 涟漪效果
struct RippleModifier: ViewModifier {
    @State private var ripples: [Ripple] = []
    let color: Color

    struct Ripple: Identifiable {
        let id = UUID()
        var location: CGPoint
        var scale: CGFloat = 0
        var opacity: Double = 0.4
    }

    func body(content: Content) -> some View {
        content
            .overlay(
                GeometryReader { geo in
                    ZStack {
                        ForEach(ripples) { ripple in
                            Circle()
                                .fill(color)
                                .frame(width: 20, height: 20)
                                .position(ripple.location)
                                .scaleEffect(ripple.scale)
                                .opacity(ripple.opacity)
                        }
                    }
                    .allowsHitTesting(false)
                }
            )
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .onEnded { value in
                        let location = value.location
                        let ripple = Ripple(location: location)
                        ripples.append(ripple)
                        withAnimation(.easeOut(duration: 0.6)) {
                            if let idx = ripples.firstIndex(where: { $0.id == ripple.id }) {
                                ripples[idx].scale = 4
                                ripples[idx].opacity = 0
                            }
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                            ripples.removeAll { $0.id == ripple.id }
                        }
                    }
            )
    }
}

// MARK: 骨架屏 Shimmer
struct ShimmerModifier: ViewModifier {
    @State private var phase: CGFloat = -1

    func body(content: Content) -> some View {
        content
            .overlay(
                GeometryReader { geo in
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0),
                            Color.white.opacity(0.15),
                            Color.white.opacity(0)
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: geo.size.width * 3)
                    .offset(x: phase * geo.size.width * 3)
                }
            )
            .mask(content)
            .onAppear {
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                    phase = 1
                }
            }
    }
}

// MARK: 按钮按压缩放
struct PressScaleModifier: ViewModifier {
    let scale: CGFloat

    @State private var isPressed = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(isPressed ? scale : 1.0)
            .animation(DesignSystem.Animation.quick, value: isPressed)
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        if !isPressed { isPressed = true }
                    }
                    .onEnded { _ in
                        isPressed = false
                    }
            )
    }
}

// MARK: 发光效果
struct GlowModifier: ViewModifier {
    var color: Color
    var radius: CGFloat = 12

    func body(content: Content) -> some View {
        content
            .shadow(color: color.opacity(0.35), radius: radius, x: 0, y: 0)
            .shadow(color: color.opacity(0.15), radius: radius * 2, x: 0, y: 0)
    }
}

// MARK: 脉动动画
struct PulseModifier: ViewModifier {
    @State private var isPulsing = false
    var color: Color
    var duration: Double = 1.5

    func body(content: Content) -> some View {
        content
            .overlay(
                Circle()
                    .stroke(color, lineWidth: 2)
                    .scaleEffect(isPulsing ? 1.5 : 1.0)
                    .opacity(isPulsing ? 0.0 : 1.0)
            )
            .onAppear {
                withAnimation(.easeOut(duration: duration).repeatForever(autoreverses: false)) {
                    isPulsing = true
                }
            }
    }
}

// MARK: - View Extensions

extension View {
    func glassmorphism(cornerRadius: CGFloat = DesignSystem.CornerRadius.medium, opacity: Double = 0.08) -> some View {
        modifier(GlassmorphismModifier(cornerRadius: cornerRadius, opacity: opacity))
    }

    func rippleEffect(color: Color = Color.white.opacity(0.2)) -> some View {
        modifier(RippleModifier(color: color))
    }

    func shimmer() -> some View {
        modifier(ShimmerModifier())
    }

    func pressScale(_ scale: CGFloat = 0.97) -> some View {
        modifier(PressScaleModifier(scale: scale))
    }

    func glow(color: Color, radius: CGFloat = 12) -> some View {
        modifier(GlowModifier(color: color, radius: radius))
    }

    func pulse(color: Color, duration: Double = 1.5) -> some View {
        modifier(PulseModifier(color: color, duration: duration))
    }

    // 标准阴影
    func subtleShadow() -> some View {
        let s = DesignSystem.Shadows.subtle()
        return shadow(color: s.color, radius: s.radius, x: s.x, y: s.y)
    }

    func elevatedShadow() -> some View {
        let s = DesignSystem.Shadows.elevated()
        return shadow(color: s.color, radius: s.radius, x: s.x, y: s.y)
    }

    func modalShadow() -> some View {
        let s = DesignSystem.Shadows.modal()
        return shadow(color: s.color, radius: s.radius, x: s.x, y: s.y)
    }

    // 标题字间距
    func titleKerning() -> some View {
        kerning(DesignSystem.Typography.titleKerning)
    }

    // 说明文字字间距
    func captionKerning() -> some View {
        kerning(DesignSystem.Typography.captionKerning)
    }

    // 标准行高
    func standardLineHeight(size: CGFloat) -> some View {
        lineSpacing(DesignSystem.Typography.lineHeight(for: size) - size)
    }

    // 页面统一内边距
    func pagePadding() -> some View {
        padding(.horizontal, DesignSystem.Spacing.Padding.container)
    }
}

// MARK: - Button Styles（完整按钮体系）

// 主按钮
struct PrimaryButtonStyle: ButtonStyle {
    var isEnabled: Bool = true
    var size: ButtonSize = .medium

    enum ButtonSize {
        case small, medium, large
        var height: CGFloat {
            switch self {
            case .small: return 36
            case .medium: return 44
            case .large: return 52
            }
        }
        var fontSize: Font {
            switch self {
            case .small: return DesignSystem.Typography.footnote
            case .medium: return DesignSystem.Typography.headline
            case .large: return DesignSystem.Typography.title3
            }
        }
        var horizontalPadding: CGFloat {
            switch self {
            case .small: return 16
            case .medium: return 24
            case .large: return 32
            }
        }
    }

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(size.fontSize)
            .foregroundColor(.white)
            .padding(.horizontal, size.horizontalPadding)
            .frame(height: size.height)
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                    .fill(isEnabled ? DesignSystem.Colors.primary : DesignSystem.Colors.gray3)
            )
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .opacity(isEnabled ? 1.0 : 0.5)
            .animation(DesignSystem.Animation.quick, value: configuration.isPressed)
    }
}

// 次要按钮
struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(DesignSystem.Typography.headline)
            .foregroundColor(DesignSystem.Colors.textPrimary)
            .padding(.horizontal, 24)
            .frame(height: 44)
            .glassmorphism()
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(DesignSystem.Animation.quick, value: configuration.isPressed)
    }
}

// 幽灵按钮
struct GhostButtonStyle: ButtonStyle {
    var isDestructive: Bool = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(DesignSystem.Typography.headline)
            .foregroundColor(isDestructive ? DesignSystem.Colors.error : DesignSystem.Colors.primary)
            .padding(.horizontal, 16)
            .frame(height: 44)
            .background(
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                    .fill(isDestructive ? DesignSystem.Colors.errorBg : DesignSystem.Colors.primary.opacity(0.08))
            )
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(DesignSystem.Animation.quick, value: configuration.isPressed)
    }
}

// 图标按钮
struct IconButtonStyle: ButtonStyle {
    var size: CGFloat = 44
    var isActive: Bool = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .frame(width: size, height: size)
            .background(
                Circle()
                    .fill(isActive ? DesignSystem.Colors.primary.opacity(0.12) : Color.clear)
            )
            .scaleEffect(configuration.isPressed ? 0.92 : 1.0)
            .animation(DesignSystem.Animation.quick, value: configuration.isPressed)
    }
}

#endif