//
//  ShootingGuideView.swift
//  LiveCapture
//
//  拍摄教程视图 - 分类拍摄技巧指南
//

import SwiftUI

#if os(iOS)

// MARK: - 拍摄技巧数据模型

struct ShootingTip: Identifiable {
    let id: String
    let icon: String
    let title: String
    let description: String
}

struct ShootingCategory: Identifiable {
    let id: String
    let icon: String
    let title: String
    let tips: [ShootingTip]
}

// MARK: - 预设教程数据

enum ShootingGuideData {
    static let categories: [ShootingCategory] = [
        ShootingCategory(
            id: "composition",
            icon: "rectangle.split.3x3",
            title: "构图技巧",
            tips: [
                ShootingTip(
                    id: "rule_of_thirds",
                    icon: "grid",
                    title: "三分法",
                    description: "将画面横竖各分三等份，形成九宫格。把主体放在四条线的交叉点上，能让画面更加平衡和自然。这是最基础也最实用的构图法则，适用于几乎所有拍摄场景。"
                ),
                ShootingTip(
                    id: "leading_lines",
                    icon: "point.topleft.down.to.point.bottomright.curvepath",
                    title: "引导线",
                    description: "利用道路、栏杆、河流等自然线条，引导观众的视线进入画面，增强纵深感和层次感。引导线的终点最好是画面的主体，形成视觉焦点。"
                ),
                ShootingTip(
                    id: "symmetry",
                    icon: "circle.lefthalf.filled",
                    title: "对称构图",
                    description: "利用建筑、水面倒影等对称元素，营造平衡、稳定、庄重的视觉效果。对称构图适合表现建筑的宏伟、水面的宁静等主题。"
                ),
                ShootingTip(
                    id: "foreground_frame",
                    icon: "rectangle.dashed",
                    title: "前景框架",
                    description: "利用门窗、树枝、拱门等作为前景框架，将主体框在其中，增加画面的层次感和深度，引导观众注意力聚焦到主体上。"
                ),
            ]
        ),
        ShootingCategory(
            id: "lighting",
            icon: "sun.max",
            title: "光线运用",
            tips: [
                ShootingTip(
                    id: "golden_hour",
                    icon: "sunrise",
                    title: "黄金时刻",
                    description: "日出后和日落前的一小时是摄影的黄金时刻。此时光线柔和温暖，色温偏暖，能拍出梦幻般的效果。尽量在这段时间拍摄人像和风光。"
                ),
                ShootingTip(
                    id: "backlight",
                    icon: "sun.max.trianglebadge.exclamationmark",
                    title: "逆光拍摄",
                    description: "让光源位于主体后方，可以拍出剪影效果或营造梦幻的光晕。逆光拍摄时注意使用点测光对准主体，适当增加曝光补偿。"
                ),
                ShootingTip(
                    id: "side_light_portrait",
                    icon: "person.fill.viewfinder",
                    title: "侧光人像",
                    description: "侧光能在人物面部产生明暗对比，增强立体感和轮廓感。45度侧光是最经典的人像光线角度，可以很好地塑造面部特征。"
                ),
                ShootingTip(
                    id: "soft_light",
                    icon: "cloud.sun",
                    title: "柔光技巧",
                    description: "阴天、树荫下或使用柔光设备，可以获得柔和的散射光。柔光适合拍摄人像，减少面部阴影，让皮肤看起来更加细腻光滑。"
                ),
            ]
        ),
        ShootingCategory(
            id: "portrait",
            icon: "person.crop.rectangle.portrait",
            title: "人像摄影",
            tips: [
                ShootingTip(
                    id: "angle_selection",
                    icon: "camera.viewfinder",
                    title: "角度选择",
                    description: "俯拍显脸小，仰拍显腿长，平拍最自然。尝试不同角度找到最适合被摄者的角度。一般建议镜头略高于眼睛水平线，避免双下巴。"
                ),
                ShootingTip(
                    id: "expression_guide",
                    icon: "face.smiling",
                    title: "表情引导",
                    description: "与模特保持沟通，用轻松的话题引导自然表情。抓拍比摆拍往往更自然。可以让模特看向别处、走动或做一些小动作，捕捉真实瞬间。"
                ),
                ShootingTip(
                    id: "environmental_portrait",
                    icon: "building.2",
                    title: "环境人像",
                    description: "将人物融入环境中，讲述一个故事。环境人像强调人物与场景的关系，背景元素应与人物形成呼应，而非单纯的背景虚化。"
                ),
                ShootingTip(
                    id: "close_up",
                    icon: "eye",
                    title: "特写技巧",
                    description: "聚焦人物的眼睛，使用大光圈虚化背景。特写能够传达强烈的情感，注意眼部对焦要精准，稍微的失焦都会影响整体效果。"
                ),
            ]
        ),
        ShootingCategory(
            id: "landscape",
            icon: "mountain.2",
            title: "风光摄影",
            tips: [
                ShootingTip(
                    id: "wide_angle",
                    icon: "camera.aperture",
                    title: "广角运用",
                    description: "使用广角镜头可以拍出宏大的场景，增强透视感。广角适合拍摄壮丽的山川、建筑等，前景物体会显得更大，背景更远，营造空间感。"
                ),
                ShootingTip(
                    id: "long_exposure",
                    icon: "water.waves",
                    title: "长曝光",
                    description: "使用慢速快门（需三脚架），可以拍出丝绸般的流水、车轨等效果。拍摄水流时通常使用 1/4 秒到 2 秒的快门速度，配合 ND 滤镜使用。"
                ),
                ShootingTip(
                    id: "hdr_scene",
                    icon: "square.3.layers.3d.down.right",
                    title: "HDR 场景",
                    description: "在明暗对比强烈的场景使用 HDR 模式，保留亮部和暗部细节。适合拍摄日出日落、逆光建筑等，避免高光过曝或暗部死黑。"
                ),
                ShootingTip(
                    id: "panorama",
                    icon: "rectangle.portrait.arrowtriangle.2.outward",
                    title: "全景拍摄",
                    description: "拍摄宽广场景时使用全景模式，保持手机平稳移动。全景拍摄时注意保持水平，避免画面中出现移动物体导致拼接错位。"
                ),
            ]
        ),
        ShootingCategory(
            id: "mobile_photo",
            icon: "iphone",
            title: "手机摄影",
            tips: [
                ShootingTip(
                    id: "stability",
                    icon: "hand.raised",
                    title: "稳定性",
                    description: "保持手机稳定是拍出清晰照片的关键。双手握持、肘部贴紧身体，或使用三脚架。拍摄时轻按快门，避免晃动。"
                ),
                ShootingTip(
                    id: "clean_lens",
                    icon: "sparkles",
                    title: "清洁镜头",
                    description: "手机镜头容易被指纹和灰尘污染，拍照前用软布擦拭镜头。一个干净的镜头能让照片清晰度大幅提升，避免朦胧和光晕。"
                ),
                ShootingTip(
                    id: "use_grid",
                    icon: "rectangle.split.3x3",
                    title: "使用网格",
                    description: "开启相机网格线，帮助构图和保持水平。网格线是三分法构图的视觉辅助，也能帮助你在拍摄建筑时保持垂直线条的平直。"
                ),
                ShootingTip(
                    id: "avoid_digital_zoom",
                    icon: "minus.magnifyingglass",
                    title: "避免数码变焦",
                    description: "数码变焦会损失画质，尽量靠近拍摄对象或后期裁剪。如果需要放大，使用光学变焦或拍摄后裁剪，裁剪比数码变焦保留更多细节。"
                ),
            ]
        ),
    ]
}

// MARK: - 拍摄教程主视图

struct ShootingGuideView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""
    @State private var expandedCategories: Set<String> = []

    var filteredCategories: [ShootingCategory] {
        if searchText.isEmpty {
            return ShootingGuideData.categories
        }
        return ShootingGuideData.categories.compactMap { category in
            let matchingTips = category.tips.filter { tip in
                tip.title.localizedCaseInsensitiveContains(searchText) ||
                tip.description.localizedCaseInsensitiveContains(searchText)
            }
            if matchingTips.isEmpty {
                return nil
            }
            return ShootingCategory(id: category.id, icon: category.icon, title: category.title, tips: matchingTips)
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DesignSystem.Spacing.medium) {
                    if filteredCategories.isEmpty {
                        emptySearchView
                    } else {
                        ForEach(filteredCategories) { category in
                            categorySection(category)
                        }
                    }
                }
                .padding(.horizontal, DesignSystem.Spacing.Padding.container)
                .padding(.bottom, 40)
            }
            .background(DesignSystem.Colors.backgroundPrimary)
            .navigationTitle("拍摄教程")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .always), prompt: "搜索拍摄技巧")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("完成") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            // 默认展开第一个分类
            if expandedCategories.isEmpty, let first = ShootingGuideData.categories.first {
                expandedCategories.insert(first.id)
            }
        }
    }

    // MARK: - Category Section

    private func categorySection(_ category: ShootingCategory) -> some View {
        VStack(spacing: 0) {
            // 分类标题
            Button {
                withAnimation(DesignSystem.Animation.smooth) {
                    if expandedCategories.contains(category.id) {
                        expandedCategories.remove(category.id)
                    } else {
                        expandedCategories.insert(category.id)
                    }
                }
            } label: {
                HStack(spacing: DesignSystem.Spacing.small) {
                    Image(systemName: category.icon)
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(DesignSystem.Colors.primary)
                        .frame(width: 28)

                    Text(category.title)
                        .font(DesignSystem.Typography.title3)
                        .foregroundColor(DesignSystem.Colors.textPrimary)

                    Text("\(category.tips.count) 条")
                        .font(DesignSystem.Typography.caption1)
                        .foregroundColor(DesignSystem.Colors.textTertiary)

                    Spacer()

                    Image(systemName: expandedCategories.contains(category.id) ? "chevron.up" : "chevron.down")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                }
                .padding(.vertical, DesignSystem.Spacing.small)
                .padding(.horizontal, DesignSystem.Spacing.small)
                .background(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                        .fill(DesignSystem.Colors.backgroundSecondary)
                )
            }
            .buttonStyle(.plain)

            // 展开的提示列表
            if expandedCategories.contains(category.id) {
                VStack(spacing: DesignSystem.Spacing.xSmall) {
                    ForEach(Array(category.tips.enumerated()), id: \.element.id) { index, tip in
                        tipRow(tip, isLast: index == category.tips.count - 1)
                            .transition(.move(edge: .top).combined(with: .opacity))
                    }
                }
                .padding(.top, DesignSystem.Spacing.xxSmall)
            }
        }
    }

    // MARK: - Tip Row

    private func tipRow(_ tip: ShootingTip, isLast: Bool) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: DesignSystem.Spacing.small) {
                Image(systemName: tip.icon)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(DesignSystem.Colors.primary)
                    .frame(width: 32, height: 32)
                    .background(
                        RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                            .fill(DesignSystem.Colors.primary.opacity(0.08))
                    )
                    .padding(.top, 2)

                VStack(alignment: .leading, spacing: 6) {
                    Text(tip.title)
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)

                    Text(tip.description)
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.textSecondary)
                        .lineSpacing(4)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .padding(.vertical, DesignSystem.Spacing.small)
            .padding(.horizontal, DesignSystem.Spacing.small)

            if !isLast {
                Divider()
                    .padding(.leading, 48)
            }
        }
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                .fill(DesignSystem.Colors.backgroundSecondary.opacity(0.5))
        )
    }

    // MARK: - Empty Search

    private var emptySearchView: some View {
        VStack(spacing: DesignSystem.Spacing.large) {
            Spacer().frame(height: 80)
            Image(systemName: "magnifyingglass")
                .font(.system(size: 40, weight: .light))
                .foregroundColor(DesignSystem.Colors.textTertiary)
            Text("未找到相关技巧")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textSecondary)
            Text("尝试其他搜索关键词")
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .frame(maxWidth: .infinity)
    }
}

#endif