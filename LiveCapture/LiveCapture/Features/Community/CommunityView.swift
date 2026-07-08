//
//  CommunityView.swift
//  LiveCapture
//
//  社区主视图 - 挑战、滤镜、拍摄地点三大板块
//

import SwiftUI
import CoreLocation

#if os(iOS)

// MARK: - 社区主视图

struct CommunityView: View {
    @StateObject private var challengeManager = PhotoChallengeManager()
    @StateObject private var filterCommunity = FilterCommunityManager()
    @StateObject private var locationRecommender = LocationRecommender()

    @State private var selectedTab: CommunityTab = .challenges

    enum CommunityTab: String, CaseIterable {
        case challenges
        case filters
        case locations

        var displayName: String {
            switch self {
            case .challenges: return "挑战"
            case .filters: return "滤镜"
            case .locations: return "地点"
            }
        }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                Picker("", selection: $selectedTab) {
                    ForEach(CommunityTab.allCases, id: \.self) { tab in
                        Text(tab.displayName).tag(tab)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, DesignSystem.Spacing.medium)
                .padding(.vertical, DesignSystem.Spacing.small)

                switch selectedTab {
                case .challenges:
                    ChallengeListView(manager: challengeManager)
                        .transition(.asymmetric(
                            insertion: .move(edge: shouldSlideRight(old: .challenges, new: .challenges) ? .trailing : .leading).combined(with: .opacity),
                            removal: .move(edge: shouldSlideRight(old: .challenges, new: selectedTab) ? .leading : .trailing).combined(with: .opacity)
                        ))
                case .filters:
                    FilterCommunityListView(manager: filterCommunity)
                        .transition(.asymmetric(
                            insertion: .move(edge: shouldSlideRight(old: .challenges, new: .filters) ? .trailing : .leading).combined(with: .opacity),
                            removal: .move(edge: shouldSlideRight(old: .filters, new: selectedTab) ? .leading : .trailing).combined(with: .opacity)
                        ))
                case .locations:
                    LocationListView(manager: locationRecommender)
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing).combined(with: .opacity),
                            removal: .move(edge: .leading).combined(with: .opacity)
                        ))
                }
            }
            .navigationTitle("社区")
            .navigationBarTitleDisplayMode(.inline)
            .animation(DesignSystem.Animation.modeSlide, value: selectedTab)
        }
    }

    private func shouldSlideRight(old: CommunityTab, new: CommunityTab) -> Bool {
        let allTabs = CommunityTab.allCases
        guard let oldIdx = allTabs.firstIndex(of: old),
              let newIdx = allTabs.firstIndex(of: new) else { return false }
        return newIdx > oldIdx
    }
}

// MARK: - 挑战列表视图

struct ChallengeListView: View {
    @ObservedObject var manager: PhotoChallengeManager

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
                // 当前挑战
                if let current = manager.currentChallenge {
                    currentChallengeSection(current)
                }

                // 即将到来
                if !manager.upcomingChallenges.isEmpty {
                    sectionHeader("即将到来", systemImage: "calendar.badge.clock")
                    ForEach(manager.upcomingChallenges.prefix(3)) { challenge in
                        NavigationLink(destination: ChallengeDetailView(challenge: challenge, manager: manager)) {
                            ChallengeRowView(challenge: challenge)
                        }
                        .buttonStyle(.plain)
                    }
                }

                // 往期挑战
                if !manager.pastChallenges.isEmpty {
                    sectionHeader("往期挑战", systemImage: "clock.arrow.circlepath")
                    ForEach(manager.pastChallenges.prefix(5)) { challenge in
                        NavigationLink(destination: ChallengeDetailView(challenge: challenge, manager: manager)) {
                            ChallengeRowView(challenge: challenge)
                        }
                        .buttonStyle(.plain)
                    }
                }

                if manager.currentChallenge == nil && manager.upcomingChallenges.isEmpty {
                    emptyStateView
                }
            }
            .padding(.horizontal, DesignSystem.Spacing.medium)
        }
    }

    private func currentChallengeSection(_ challenge: PhotoChallenge) -> some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
            sectionHeader("本周挑战", systemImage: "trophy.fill")

            NavigationLink(destination: ChallengeDetailView(challenge: challenge, manager: manager)) {
                VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
                    HStack {
                        Image(systemName: challenge.theme.iconName)
                            .font(.title2)
                            .foregroundColor(DesignSystem.Colors.accent)
                        Text(challenge.title)
                            .font(DesignSystem.Typography.title3)
                            .foregroundColor(DesignSystem.Colors.textPrimary)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }

                    Text(challenge.description)
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.textSecondary)
                        .lineLimit(2)

                    HStack {
                        Label("\(challenge.startDate, style: .date) - \(challenge.endDate, style: .date)", systemImage: "calendar")
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                        Spacer()
                        Text("\(challenge.userEntries.count) 作品")
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(DesignSystem.Colors.accent)
                    }
                }
                .padding(DesignSystem.Spacing.medium)
                .background(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                        .fill(DesignSystem.Colors.backgroundSecondary)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                        .stroke(DesignSystem.Colors.accent.opacity(0.5), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
    }

    private func sectionHeader(_ title: String, systemImage: String) -> some View {
        HStack {
            Image(systemName: systemImage)
                .foregroundColor(DesignSystem.Colors.primary)
            Text(title)
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textPrimary)
        }
        .padding(.top, DesignSystem.Spacing.small)
    }

    private var emptyStateView: some View {
        VStack(spacing: DesignSystem.Spacing.large) {
            Spacer().frame(height: 60)
            Image(systemName: "camera.macro")
                .font(.system(size: 48))
                .foregroundColor(DesignSystem.Colors.textTertiary)
            Text("暂无挑战")
                .font(DesignSystem.Typography.title3)
                .foregroundColor(DesignSystem.Colors.textSecondary)
            Text("新挑战即将到来，敬请期待")
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textTertiary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 挑战行视图

struct ChallengeRowView: View {
    let challenge: PhotoChallenge

    var body: some View {
        HStack(spacing: DesignSystem.Spacing.medium) {
            Image(systemName: challenge.theme.iconName)
                .font(.title3)
                .foregroundColor(DesignSystem.Colors.primary)
                .frame(width: 36)

            VStack(alignment: .leading, spacing: 4) {
                Text(challenge.title)
                    .font(DesignSystem.Typography.callout)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                Text(challenge.description)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
                    .lineLimit(1)
            }

            Spacer()

            Text("\(challenge.userEntries.count) 作品")
                .font(DesignSystem.Typography.caption2)
                .foregroundColor(DesignSystem.Colors.textTertiary)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(
                    Capsule()
                        .fill(DesignSystem.Colors.backgroundSecondary)
                )
        }
        .padding(DesignSystem.Spacing.small)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
    }
}

// MARK: - 挑战详情视图

struct ChallengeDetailView: View {
    let challenge: PhotoChallenge
    @ObservedObject var manager: PhotoChallengeManager
    @State private var showSubmitSheet = false
    @State private var entryTitle = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
                // 头部信息
                VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
                    HStack {
                        Image(systemName: challenge.theme.iconName)
                            .font(.largeTitle)
                            .foregroundColor(DesignSystem.Colors.accent)
                        VStack(alignment: .leading) {
                            Text(challenge.title)
                                .font(DesignSystem.Typography.title2)
                            Text(challenge.theme.displayName)
                                .font(DesignSystem.Typography.subheadline)
                                .foregroundColor(DesignSystem.Colors.textSecondary)
                        }
                    }

                    Text(challenge.description)
                        .font(DesignSystem.Typography.body)
                        .foregroundColor(DesignSystem.Colors.textSecondary)

                    HStack {
                        Image(systemName: "calendar")
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                        Text("\(challenge.startDate, style: .date) - \(challenge.endDate, style: .date)")
                            .font(DesignSystem.Typography.footnote)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                }
                .padding(DesignSystem.Spacing.medium)
                .background(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                        .fill(DesignSystem.Colors.backgroundSecondary)
                )

                // 提交按钮
                if challenge.isActive {
                    Button(action: { showSubmitSheet = true }) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text("提交作品")
                        }
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, DesignSystem.Spacing.medium)
                        .background(
                            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                                .fill(DesignSystem.Colors.primaryGradient)
                        )
                    }
                }

                // 排行榜
                if !challenge.userEntries.isEmpty {
                    sectionHeader("排行榜", systemImage: "chart.bar.fill")
                    let leaderboard = manager.getLeaderboard(for: challenge.id)
                    ForEach(Array(leaderboard.enumerated()), id: \.element.id) { index, entry in
                        leaderboardRow(index: index, entry: entry)
                    }
                } else {
                    emptyEntriesView
                }
            }
            .padding(.horizontal, DesignSystem.Spacing.medium)
        }
        .navigationTitle("挑战详情")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showSubmitSheet) {
            submitEntrySheet
        }
    }

    private func leaderboardRow(index: Int, entry: ChallengeEntry) -> some View {
        HStack(spacing: DesignSystem.Spacing.small) {
            // 排名
            Text("#\(index + 1)")
                .font(DesignSystem.Typography.headline)
                .foregroundColor(index < 3 ? DesignSystem.Colors.accent : DesignSystem.Colors.textSecondary)
                .frame(width: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(entry.title)
                    .font(DesignSystem.Typography.callout)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                Text(entry.submittedDate, style: .date)
                    .font(DesignSystem.Typography.caption2)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            }

            Spacer()

            HStack(spacing: 4) {
                Image(systemName: "heart.fill")
                    .font(.caption)
                    .foregroundColor(.red)
                Text("\(entry.votes)")
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textSecondary)
            }

            if !entry.hasVoted {
                Button(action: {
                    manager.voteForEntry(entry.id, in: challenge.id)
                }) {
                    Image(systemName: "hand.thumbsup")
                        .font(.caption)
                        .foregroundColor(DesignSystem.Colors.primary)
                }
            }
        }
        .padding(DesignSystem.Spacing.small)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
    }

    private var emptyEntriesView: some View {
        VStack(spacing: DesignSystem.Spacing.medium) {
            Spacer().frame(height: 40)
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 40))
                .foregroundColor(DesignSystem.Colors.textTertiary)
            Text("暂无作品")
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textSecondary)
            Text("成为第一个提交作品的人吧！")
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .frame(maxWidth: .infinity)
    }

    private var submitEntrySheet: some View {
        NavigationView {
            VStack(spacing: DesignSystem.Spacing.large) {
                VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
                    Text("作品标题")
                        .font(DesignSystem.Typography.headline)
                    TextField("给你的作品起个名字...", text: $entryTitle)
                        .textFieldStyle(.roundedBorder)
                }
                .padding(.top, DesignSystem.Spacing.large)

                Button(action: {
                    guard !entryTitle.isEmpty else { return }
                    manager.submitEntry(to: challenge.id, photoFileName: "photo_\(UUID().uuidString).jpg", title: entryTitle)
                    showSubmitSheet = false
                    entryTitle = ""
                }) {
                    Text("提交")
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, DesignSystem.Spacing.medium)
                        .background(
                            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                                .fill(entryTitle.isEmpty ? Color.gray : DesignSystem.Colors.primaryGradient)
                        )
                }
                .disabled(entryTitle.isEmpty)

                Spacer()
            }
            .padding(.horizontal, DesignSystem.Spacing.medium)
            .navigationTitle("提交作品")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { showSubmitSheet = false }
                }
            }
        }
    }

    private func sectionHeader(_ title: String, systemImage: String) -> some View {
        HStack {
            Image(systemName: systemImage)
                .foregroundColor(DesignSystem.Colors.primary)
            Text(title)
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textPrimary)
        }
        .padding(.top, DesignSystem.Spacing.small)
    }
}

// MARK: - 滤镜社区列表视图

struct FilterCommunityListView: View {
    @ObservedObject var manager: FilterCommunityManager
    @State private var selectedCategory: FilterCategory?
    @State private var showMyFilters = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
                // 分类切换
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DesignSystem.Spacing.xSmall) {
                        FilterCategoryChip(
                            title: "全部",
                            isSelected: selectedCategory == nil,
                            action: { selectedCategory = nil }
                        )
                        ForEach(FilterCategory.allCases) { category in
                            FilterCategoryChip(
                                title: category.rawValue,
                                isSelected: selectedCategory == category,
                                action: { selectedCategory = category }
                            )
                        }
                    }
                    .padding(.horizontal, DesignSystem.Spacing.medium)
                }

                // 热门滤镜
                if selectedCategory == nil {
                    sectionHeader("热门滤镜", systemImage: "flame.fill")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: DesignSystem.Spacing.medium) {
                            ForEach(manager.popularFilters) { filter in
                                NavigationLink(destination: FilterDetailView(filter: filter, manager: manager)) {
                                    PopularFilterCard(filter: filter)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, DesignSystem.Spacing.medium)
                    }
                }

                // 滤镜列表
                let filters = selectedCategory.map { manager.getFiltersForCategory($0) } ?? manager.communityFilters
                sectionHeader(selectedCategory?.rawValue ?? "全部滤镜", systemImage: "camera.filters")

                ForEach(filters) { filter in
                    NavigationLink(destination: FilterDetailView(filter: filter, manager: manager)) {
                        FilterRowView(filter: filter, isDownloaded: manager.isDownloaded(filter))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.bottom, DesignSystem.Spacing.large)
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showMyFilters.toggle() }) {
                    Image(systemName: "person.crop.circle")
                }
            }
        }
        .sheet(isPresented: $showMyFilters) {
            MyFiltersView(manager: manager)
        }
    }

    private func sectionHeader(_ title: String, systemImage: String) -> some View {
        HStack {
            Image(systemName: systemImage)
                .foregroundColor(DesignSystem.Colors.primary)
            Text(title)
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textPrimary)
        }
        .padding(.horizontal, DesignSystem.Spacing.medium)
        .padding(.top, DesignSystem.Spacing.small)
    }
}

// MARK: - 滤镜分类标签

struct FilterCategoryChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(DesignSystem.Typography.footnote)
                .fontWeight(isSelected ? .semibold : .regular)
                .foregroundColor(isSelected ? .white : DesignSystem.Colors.textSecondary)
                .padding(.horizontal, DesignSystem.Spacing.small)
                .padding(.vertical, DesignSystem.Spacing.xSmall)
                .background(
                    Capsule()
                        .fill(isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.backgroundSecondary)
                )
        }
    }
}

// MARK: - 热门滤镜卡片

struct PopularFilterCard: View {
    let filter: UserFilter

    var body: some View {
        VStack(alignment: .leading, spacing: DesignSystem.Spacing.xSmall) {
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                .fill(
                    LinearGradient(
                        colors: [DesignSystem.Colors.primary.opacity(0.3), DesignSystem.Colors.secondary.opacity(0.3)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 120, height: 80)
                .overlay(
                    Image(systemName: filter.category.symbolName)
                        .font(.title2)
                        .foregroundColor(.white.opacity(0.8))
                )

            Text(filter.name)
                .font(DesignSystem.Typography.caption1)
                .foregroundColor(DesignSystem.Colors.textPrimary)
                .lineLimit(1)

            Text("\(filter.downloads) 下载")
                .font(DesignSystem.Typography.caption2)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .frame(width: 120)
    }
}

// MARK: - 滤镜行视图

struct FilterRowView: View {
    let filter: UserFilter
    let isDownloaded: Bool

    var body: some View {
        HStack(spacing: DesignSystem.Spacing.medium) {
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                .fill(
                    LinearGradient(
                        colors: [DesignSystem.Colors.primary.opacity(0.2), DesignSystem.Colors.secondary.opacity(0.2)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 52, height: 52)
                .overlay(
                    Image(systemName: filter.category.symbolName)
                        .foregroundColor(DesignSystem.Colors.primary.opacity(0.7))
                )

            VStack(alignment: .leading, spacing: 4) {
                Text(filter.name)
                    .font(DesignSystem.Typography.callout)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                Text(filter.creatorName)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
            }

            Spacer()

            if isDownloaded {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(DesignSystem.Colors.success)
            }

            HStack(spacing: 2) {
                Image(systemName: "arrow.down.circle")
                    .font(.caption)
                Text("\(filter.downloads)")
                    .font(DesignSystem.Typography.caption2)
            }
            .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .padding(DesignSystem.Spacing.small)
        .padding(.horizontal, DesignSystem.Spacing.small)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
        .padding(.horizontal, DesignSystem.Spacing.medium)
    }
}

// MARK: - 滤镜详情视图

struct FilterDetailView: View {
    let filter: UserFilter
    @ObservedObject var manager: FilterCommunityManager

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignSystem.Spacing.large) {
                // 预览区域
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                    .fill(
                        LinearGradient(
                            colors: [DesignSystem.Colors.primary.opacity(0.4), DesignSystem.Colors.secondary.opacity(0.4)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(height: 200)
                    .overlay(
                        VStack(spacing: DesignSystem.Spacing.small) {
                            Image(systemName: filter.category.symbolName)
                                .font(.system(size: 48))
                                .foregroundColor(.white.opacity(0.8))
                            Text(filter.name)
                                .font(DesignSystem.Typography.title2)
                                .foregroundColor(.white)
                        }
                    )
                    .padding(.horizontal, DesignSystem.Spacing.medium)

                // 滤镜信息
                VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(filter.name)
                                .font(DesignSystem.Typography.title3)
                            Text("by \(filter.creatorName)")
                                .font(DesignSystem.Typography.subheadline)
                                .foregroundColor(DesignSystem.Colors.textSecondary)
                        }
                        Spacer()
                        Text(filter.category.rawValue)
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(DesignSystem.Colors.primary)
                            .padding(.horizontal, DesignSystem.Spacing.small)
                            .padding(.vertical, 4)
                            .background(
                                Capsule()
                                    .fill(DesignSystem.Colors.primary.opacity(0.15))
                            )
                    }

                    Text(filter.filterDescription)
                        .font(DesignSystem.Typography.body)
                        .foregroundColor(DesignSystem.Colors.textSecondary)

                    // 参数详情
                    VStack(alignment: .leading, spacing: 8) {
                        parameterRow("色温", value: String(format: "%.0fK", filter.parameters.temperature + 6500))
                        parameterRow("色调", value: String(format: "%.0f", filter.parameters.tint))
                        parameterRow("曝光", value: String(format: "%.2f EV", filter.parameters.exposure))
                        parameterRow("对比度", value: String(format: "%.2f", filter.parameters.contrast))
                        parameterRow("饱和度", value: String(format: "%.2f", filter.parameters.saturation))
                        if filter.parameters.isMonochrome {
                            parameterRow("黑白", value: "开启")
                        }
                    }
                    .padding(DesignSystem.Spacing.medium)
                    .background(
                        RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                            .fill(DesignSystem.Colors.backgroundSecondary)
                    )
                }
                .padding(.horizontal, DesignSystem.Spacing.medium)

                // 下载按钮
                if manager.isDownloaded(filter) {
                    Button(action: { manager.removeFilter(filter) }) {
                        HStack {
                            Image(systemName: "trash")
                            Text("移除下载")
                        }
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(DesignSystem.Colors.error)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, DesignSystem.Spacing.medium)
                        .background(
                            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                                .strokeBorder(DesignSystem.Colors.error, lineWidth: 1)
                        )
                    }
                    .padding(.horizontal, DesignSystem.Spacing.medium)
                } else {
                    Button(action: { manager.downloadFilter(filter) }) {
                        HStack {
                            Image(systemName: "arrow.down.circle.fill")
                            Text("下载滤镜 (\(filter.downloads))")
                        }
                        .font(DesignSystem.Typography.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, DesignSystem.Spacing.medium)
                        .background(
                            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.medium)
                                .fill(DesignSystem.Colors.primaryGradient)
                        )
                    }
                    .padding(.horizontal, DesignSystem.Spacing.medium)
                }
            }
            .padding(.bottom, DesignSystem.Spacing.large)
        }
        .navigationTitle("滤镜详情")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func parameterRow(_ label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(DesignSystem.Typography.caption1)
                .foregroundColor(DesignSystem.Colors.textTertiary)
            Spacer()
            Text(value)
                .font(DesignSystem.Typography.caption1)
                .foregroundColor(DesignSystem.Colors.textPrimary)
        }
    }
}

// MARK: - 我的滤镜视图

struct MyFiltersView: View {
    @ObservedObject var manager: FilterCommunityManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            List {
                if !manager.downloadedFilters.isEmpty {
                    Section("已下载") {
                        ForEach(manager.downloadedFilters) { filter in
                            HStack {
                                Image(systemName: filter.category.symbolName)
                                    .foregroundColor(DesignSystem.Colors.primary)
                                VStack(alignment: .leading) {
                                    Text(filter.name)
                                        .font(DesignSystem.Typography.callout)
                                    Text(filter.creatorName)
                                        .font(DesignSystem.Typography.caption2)
                                        .foregroundColor(DesignSystem.Colors.textTertiary)
                                }
                            }
                        }
                        .onDelete { indexSet in
                            for index in indexSet {
                                manager.removeFilter(manager.downloadedFilters[index])
                            }
                        }
                    }
                }

                if !manager.myCreatedFilters.isEmpty {
                    Section("我创建的") {
                        ForEach(manager.myCreatedFilters) { filter in
                            HStack {
                                Image(systemName: filter.category.symbolName)
                                    .foregroundColor(DesignSystem.Colors.accent)
                                VStack(alignment: .leading) {
                                    Text(filter.name)
                                        .font(DesignSystem.Typography.callout)
                                    Text(filter.filterDescription)
                                        .font(DesignSystem.Typography.caption2)
                                        .foregroundColor(DesignSystem.Colors.textTertiary)
                                }
                            }
                        }
                    }
                }

                if manager.downloadedFilters.isEmpty && manager.myCreatedFilters.isEmpty {
                    VStack(spacing: DesignSystem.Spacing.medium) {
                        Spacer().frame(height: 40)
                        Image(systemName: "camera.filters")
                            .font(.system(size: 40))
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                        Text("还没有滤镜")
                            .font(DesignSystem.Typography.headline)
                            .foregroundColor(DesignSystem.Colors.textSecondary)
                        Text("浏览社区滤镜并下载你喜欢的")
                            .font(DesignSystem.Typography.subheadline)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("我的滤镜")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }
}

// MARK: - 拍摄地点列表视图

struct LocationListView: View {
    @ObservedObject var manager: LocationRecommender
    @State private var selectedTag: String?
    @State private var searchRadius: Double = 5000

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignSystem.Spacing.medium) {
                // 定位状态
                locationStatusBar

                // 标签筛选
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DesignSystem.Spacing.xSmall) {
                        LocationTagChip(
                            title: "全部",
                            isSelected: selectedTag == nil,
                            action: { selectedTag = nil }
                        )
                        ForEach(manager.getAllTags(), id: \.self) { tag in
                            LocationTagChip(
                                title: tag,
                                isSelected: selectedTag == tag,
                                action: { selectedTag = tag }
                            )
                        }
                    }
                    .padding(.horizontal, DesignSystem.Spacing.medium)
                }

                // 地点列表
                let locations = filteredLocations
                if locations.isEmpty {
                    emptyLocationsView
                } else {
                    ForEach(locations) { location in
                        NavigationLink(destination: LocationDetailView(location: location, userLocation: manager.currentLocation)) {
                            LocationRowView(location: location, userLocation: manager.currentLocation)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.bottom, DesignSystem.Spacing.large)
        }
        .onAppear {
            manager.requestLocation()
        }
    }

    private var filteredLocations: [PhotoLocation] {
        if let tag = selectedTag {
            return manager.getLocationsByTag(tag)
        }
        return manager.nearbyLocations.isEmpty ? manager.allLocations : manager.nearbyLocations
    }

    private var locationStatusBar: some View {
        HStack {
            Image(systemName: locationStatusIcon)
                .foregroundColor(locationStatusColor)
            Text(locationStatusText)
                .font(DesignSystem.Typography.footnote)
                .foregroundColor(DesignSystem.Colors.textSecondary)
            Spacer()
            if manager.currentLocation != nil {
                Button(action: { manager.requestLocation() }) {
                    Image(systemName: "location.circle")
                        .font(.caption)
                }
            }
        }
        .padding(.horizontal, DesignSystem.Spacing.medium)
        .padding(.vertical, DesignSystem.Spacing.xSmall)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
        .padding(.horizontal, DesignSystem.Spacing.medium)
    }

    private var locationStatusIcon: String {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            return manager.currentLocation != nil ? "location.fill" : "location"
        case .denied, .restricted:
            return "location.slash"
        default:
            return "location"
        }
    }

    private var locationStatusColor: Color {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            return DesignSystem.Colors.success
        case .denied, .restricted:
            return DesignSystem.Colors.error
        default:
            return DesignSystem.Colors.textTertiary
        }
    }

    private var locationStatusText: String {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            return manager.currentLocation != nil ? "已定位，显示附近拍摄点" : "正在获取位置..."
        case .denied, .restricted:
            return "定位权限未开启，显示全部拍摄点"
        default:
            return "点击获取位置推荐"
        }
    }

    private var emptyLocationsView: some View {
        VStack(spacing: DesignSystem.Spacing.medium) {
            Spacer().frame(height: 40)
            Image(systemName: "mappin.slash")
                .font(.system(size: 40))
                .foregroundColor(DesignSystem.Colors.textTertiary)
            Text("暂无拍摄点")
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textSecondary)
            Text("该标签下暂无拍摄点推荐")
                .font(DesignSystem.Typography.subheadline)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 地点标签

struct LocationTagChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(DesignSystem.Typography.footnote)
                .fontWeight(isSelected ? .semibold : .regular)
                .foregroundColor(isSelected ? .white : DesignSystem.Colors.textSecondary)
                .padding(.horizontal, DesignSystem.Spacing.small)
                .padding(.vertical, DesignSystem.Spacing.xSmall)
                .background(
                    Capsule()
                        .fill(isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.backgroundSecondary)
                )
        }
    }
}

// MARK: - 地点行视图

struct LocationRowView: View {
    let location: PhotoLocation
    let userLocation: CLLocation?

    var body: some View {
        HStack(spacing: DesignSystem.Spacing.medium) {
            // 左侧图标
            ZStack {
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                    .fill(DesignSystem.Colors.backgroundSecondary)
                    .frame(width: 56, height: 56)

                Image(systemName: "mappin.and.ellipse")
                    .font(.title3)
                    .foregroundColor(difficultyColor)
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(location.name)
                        .font(DesignSystem.Typography.callout)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                    Spacer()
                    Text(location.difficulty.displayName)
                        .font(DesignSystem.Typography.caption2)
                        .foregroundColor(difficultyColor)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            Capsule()
                                .fill(difficultyColor.opacity(0.15))
                        )
                }

                Text(location.description)
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.textTertiary)
                    .lineLimit(2)

                HStack {
                    Image(systemName: "clock")
                        .font(.system(size: 10))
                    Text(location.bestTime)
                        .font(DesignSystem.Typography.caption2)
                        .lineLimit(1)
                    Spacer()
                    if let distance = distanceText {
                        Text(distance)
                            .font(DesignSystem.Typography.caption2)
                            .foregroundColor(DesignSystem.Colors.primary)
                    }
                }
                .foregroundColor(DesignSystem.Colors.textTertiary)
            }
        }
        .padding(DesignSystem.Spacing.small)
        .padding(.horizontal, DesignSystem.Spacing.small)
        .background(
            RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
        .padding(.horizontal, DesignSystem.Spacing.medium)
    }

    private var difficultyColor: Color {
        switch location.difficulty {
        case .easy: return DesignSystem.Colors.success
        case .medium: return DesignSystem.Colors.accent
        case .hard: return DesignSystem.Colors.error
        }
    }

    private var distanceText: String? {
        guard let userLoc = userLocation else { return nil }
        let dist = userLoc.distance(from: location.coordinate.clLocation)
        if dist < 1000 {
            return String(format: "%.0fm", dist)
        } else {
            return String(format: "%.1fkm", dist / 1000)
        }
    }
}

// MARK: - 地点详情视图

struct LocationDetailView: View {
    let location: PhotoLocation
    let userLocation: CLLocation?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignSystem.Spacing.large) {
                // 预览图占位
                RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.large)
                    .fill(
                        LinearGradient(
                            colors: [DesignSystem.Colors.primary.opacity(0.3), DesignSystem.Colors.secondary.opacity(0.3)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(height: 200)
                    .overlay(
                        VStack(spacing: DesignSystem.Spacing.small) {
                            Image(systemName: "mappin.and.ellipse")
                                .font(.system(size: 48))
                                .foregroundColor(.white.opacity(0.8))
                            Text(location.name)
                                .font(DesignSystem.Typography.title2)
                                .foregroundColor(.white)
                        }
                    )
                    .padding(.horizontal, DesignSystem.Spacing.medium)

                // 基本信息
                VStack(alignment: .leading, spacing: DesignSystem.Spacing.small) {
                    Text(location.name)
                        .font(DesignSystem.Typography.title2)

                    Text(location.description)
                        .font(DesignSystem.Typography.body)
                        .foregroundColor(DesignSystem.Colors.textSecondary)

                    // 坐标
                    HStack {
                        Image(systemName: "location.fill")
                            .foregroundColor(DesignSystem.Colors.primary)
                        Text(String(format: "%.4f, %.4f", location.coordinate.latitude, location.coordinate.longitude))
                            .font(DesignSystem.Typography.caption1)
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }

                    // 最佳拍摄时间
                    HStack {
                        Image(systemName: "clock.fill")
                            .foregroundColor(DesignSystem.Colors.accent)
                        Text(location.bestTime)
                            .font(DesignSystem.Typography.callout)
                            .foregroundColor(DesignSystem.Colors.textSecondary)
                    }

                    // 难度
                    HStack {
                        Text("拍摄难度:")
                            .font(DesignSystem.Typography.callout)
                            .foregroundColor(DesignSystem.Colors.textSecondary)
                        Text(location.difficulty.displayName)
                            .font(DesignSystem.Typography.callout)
                            .foregroundColor(difficultyColor)
                            .fontWeight(.semibold)
                    }

                    // 距离
                    if let userLoc = userLocation {
                        let dist = userLoc.distance(from: location.coordinate.clLocation)
                        HStack {
                            Image(systemName: "point.topleft.down.to.point.bottomright.curvepath")
                                .foregroundColor(DesignSystem.Colors.primary)
                            Text(distanceFormatted(dist))
                                .font(DesignSystem.Typography.callout)
                                .foregroundColor(DesignSystem.Colors.textSecondary)
                        }
                    }

                    // 标签
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: DesignSystem.Spacing.xSmall) {
                            ForEach(location.tags, id: \.self) { tag in
                                Text(tag)
                                    .font(DesignSystem.Typography.caption2)
                                    .foregroundColor(DesignSystem.Colors.primary)
                                    .padding(.horizontal, DesignSystem.Spacing.xSmall)
                                    .padding(.vertical, 4)
                                    .background(
                                        Capsule()
                                            .fill(DesignSystem.Colors.primary.opacity(0.15))
                                    )
                            }
                        }
                    }
                }
                .padding(.horizontal, DesignSystem.Spacing.medium)
            }
            .padding(.bottom, DesignSystem.Spacing.large)
        }
        .navigationTitle("拍摄点详情")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var difficultyColor: Color {
        switch location.difficulty {
        case .easy: return DesignSystem.Colors.success
        case .medium: return DesignSystem.Colors.accent
        case .hard: return DesignSystem.Colors.error
        }
    }

    private func distanceFormatted(_ distance: CLLocationDistance) -> String {
        if distance < 1000 {
            return String(format: "距离: %.0f 米", distance)
        } else {
            return String(format: "距离: %.1f 公里", distance / 1000)
        }
    }
}

#endif