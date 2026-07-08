import SwiftUI

struct GalleryView: View {
    @StateObject private var viewModel = HomeViewModel()
    @State private var selectedPhotoIndex: Int?
    @State private var isSelectionMode = false
    @State private var selectedIDs: Set<UUID> = []
    @State private var showFilterSheet = false
    @State private var showBatchActions = false
    @State private var isLoading = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // 搜索栏
                searchBar
                    .padding(.horizontal, DesignSystem.Spacing.Padding.container)
                    .padding(.top, DesignSystem.Spacing.xxSmall)

                // 分组模式选择器
                groupingModeSelector
                    .padding(.vertical, DesignSystem.Spacing.xxSmall)

                // 批量编辑工具栏
                if isSelectionMode {
                    batchEditToolbar
                        .padding(.horizontal, DesignSystem.Spacing.Padding.container)
                        .padding(.vertical, DesignSystem.Spacing.xxSmall)
                }

                // 主内容区
                if isLoading {
                    SkeletonView()
                } else {
                    ScrollView {
                        VStack(spacing: 0) {
                            // 顶部标题栏 - 增加呼吸感留白
                            headerBar
                                .padding(.horizontal, DesignSystem.Spacing.Padding.container)
                                .padding(.bottom, DesignSystem.Spacing.xxSmall)
                                .padding(.top, DesignSystem.Spacing.xxSmall)

                            if !isSelectionMode && !viewModel.filteredRecords.isEmpty {
                                guidanceBanner
                                    .padding(.horizontal, DesignSystem.Spacing.Padding.container)
                                    .padding(.bottom, DesignSystem.Spacing.xxSmall)
                            }

                            if viewModel.filteredRecords.isEmpty {
                                emptyStateView
                                    .padding(.top, 80)
                            } else {
                                groupedPhotoGrid
                                    .padding(.horizontal, DesignSystem.Spacing.Gap.minimal)
                            }
                        }
                    }
                    .refreshable {
                        // 下拉刷新
                        await refreshData()
                    }
                }
            }
            .background(DesignSystem.Colors.backgroundPrimary)
            .navigationBarHidden(true)
            .navigationDestination(item: $selectedPhotoIndex) { index in
                PhotoBrowserView(
                    records: viewModel.filteredRecords,
                    initialIndex: index,
                    photoProvider: { [weak viewModel] id in
                        viewModel?.fullPhoto(for: id)
                    }
                )
            }
            .sheet(isPresented: $showFilterSheet) {
                filterSheetView
            }
            .confirmationDialog("批量操作", isPresented: $showBatchActions, titleVisibility: .visible) {
                Button("批量删除", role: .destructive) {
                    viewModel.batchDeleteSelected()
                }
                Button("自动增强") {
                    viewModel.batchAutoEnhance()
                }
                Button("应用滤镜") {
                    showFilterSheet = true
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("已选择 \(selectedIDs.count) 张照片")
            }
        }
        .onAppear {
            isLoading = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                withAnimation(DesignSystem.Animation.easeOut) {
                    isLoading = false
                }
            }
        }
    }

    private func refreshData() async {
        isLoading = true
        try? await Task.sleep(nanoseconds: 500_000_000)
        withAnimation(DesignSystem.Animation.easeOut) {
            isLoading = false
        }
    }

    // MARK: - Search Bar

    private var searchBar: some View {
        HStack(spacing: 8) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 14))
                    .foregroundColor(DesignSystem.Colors.textTertiary)

                TextField("搜索照片...", text: $viewModel.searchQuery)
                    .font(DesignSystem.Typography.subheadline)
                    .foregroundColor(DesignSystem.Colors.textPrimary)
                    .textFieldStyle(.plain)

                if !viewModel.searchQuery.isEmpty {
                    Button {
                        viewModel.searchQuery = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 14))
                            .foregroundColor(DesignSystem.Colors.textTertiary)
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(DesignSystem.Colors.backgroundSecondary)
            )
        }
    }

    // MARK: - Grouping Mode Selector

    private var groupingModeSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(GroupingMode.allCases) { mode in
                    Button {
                        withAnimation(DesignSystem.Animation.quick) {
                            viewModel.groupingMode = mode
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: mode.iconName)
                                .font(.system(size: 11))
                            Text(mode.rawValue)
                                .font(DesignSystem.Typography.caption1)
                        }
                        .foregroundColor(viewModel.groupingMode == mode ? .white : DesignSystem.Colors.textSecondary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(
                            Capsule()
                                .fill(viewModel.groupingMode == mode
                                      ? DesignSystem.Colors.primary
                                      : DesignSystem.Colors.backgroundSecondary)
                        )
                    }
                }
            }
            .padding(.horizontal, 20)
        }
    }

    // MARK: - Header Bar

    private var headerBar: some View {
        HStack {
            Text("图库")
                .font(DesignSystem.Typography.largeTitle)
                .foregroundColor(DesignSystem.Colors.textPrimary)

            if isSelectionMode {
                Spacer()

                Button {
                    viewModel.selectAll()
                } label: {
                    Text("全选")
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.primary)
                }

                Button {
                    if !selectedIDs.isEmpty {
                        showBatchActions = true
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(selectedIDs.isEmpty ? DesignSystem.Colors.textTertiary : DesignSystem.Colors.primary)
                        .padding(8)
                        .background(Circle().fill(.ultraThinMaterial))
                }
                .disabled(selectedIDs.isEmpty)

                Button {
                    isSelectionMode = false
                    selectedIDs.removeAll()
                    viewModel.isBatchEditing = false
                } label: {
                    Text("取消")
                        .font(DesignSystem.Typography.subheadline)
                        .foregroundColor(DesignSystem.Colors.textPrimary)
                        .padding(.leading, 8)
                }
            } else {
                Spacer()

                if !viewModel.records.isEmpty {
                    Text("\(viewModel.records.count) 张照片")
                        .font(DesignSystem.Typography.caption1)
                        .foregroundColor(DesignSystem.Colors.textTertiary)
                }

                // 批量编辑按钮
                if !viewModel.filteredRecords.isEmpty {
                    Button {
                        isSelectionMode = true
                        viewModel.isBatchEditing = true
                    } label: {
                        Image(systemName: "checkmark.circle")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(DesignSystem.Colors.textSecondary)
                            .padding(8)
                            .background(Circle().fill(.ultraThinMaterial))
                    }
                    .padding(.leading, 8)
                }
            }
        }
        .padding(.top, 8)
    }

    // MARK: - Batch Edit Toolbar

    private var batchEditToolbar: some View {
        HStack(spacing: 12) {
            Button {
                showFilterSheet = true
            } label: {
                Label("滤镜", systemImage: "camera.filters")
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.primary)
            }

            Divider().frame(height: 20)

            Button {
                viewModel.batchAutoEnhance()
            } label: {
                Label("增强", systemImage: "wand.and.stars")
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(DesignSystem.Colors.primary)
            }

            Divider().frame(height: 20)

            Button(role: .destructive) {
                viewModel.batchDeleteSelected()
            } label: {
                Label("删除", systemImage: "trash")
                    .font(DesignSystem.Typography.caption1)
                    .foregroundColor(.red)
            }
            .disabled(selectedIDs.isEmpty)

            Spacer()

            Text("已选 \(selectedIDs.count) 张")
                .font(DesignSystem.Typography.caption1)
                .foregroundColor(DesignSystem.Colors.textTertiary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
    }

    // MARK: - Guidance

    private var guidanceBanner: some View {
        HStack(spacing: 8) {
            Image(systemName: "info.circle.fill")
                .font(.system(size: 12))
                .foregroundColor(DesignSystem.Colors.primary)
            Text("点击照片浏览 · 长按多选删除 · 进入照片可导出精美卡片")
                .font(DesignSystem.Typography.caption1)
                .foregroundColor(DesignSystem.Colors.textTertiary)
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(DesignSystem.Colors.backgroundSecondary)
        )
    }

    // MARK: - Grouped Photo Grid

    private var groupedPhotoGrid: some View {
        LazyVStack(spacing: 0, pinnedViews: [.sectionHeaders]) {
            ForEach(viewModel.groupedRecords, id: \.title) { group in
                Section {
                    LazyVGrid(
                        columns: Array(repeating: .init(.flexible(), spacing: DesignSystem.Spacing.Gap.minimal), count: 3),
                        spacing: DesignSystem.Spacing.Gap.minimal
                    ) {
                        ForEach(group.records) { record in
                            photoGridItem(record)
                        }
                    }
                } header: {
                    if viewModel.groupingMode != .none {
                        groupHeader(group.title, count: group.records.count)
                    }
                }
            }
        }
    }

    private func groupHeader(_ title: String, count: Int) -> some View {
        HStack {
            Text(title)
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textPrimary)
            Text("\(count)")
                .font(DesignSystem.Typography.caption1)
                .foregroundColor(DesignSystem.Colors.textTertiary)
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
        .background(
            Rectangle()
                .fill(.ultraThinMaterial)
                .background(Color(uiColor: .systemBackground))
        )
    }

    @ViewBuilder
    private func photoGridItem(_ record: PhotoRecord) -> some View {
        Button {
            if isSelectionMode {
                toggleSelection(record.id)
            } else {
                if let index = viewModel.filteredRecords.firstIndex(where: { $0.id == record.id }) {
                    selectedPhotoIndex = index
                }
            }
        } label: {
            ZStack(alignment: .topTrailing) {
                PhotoCard(
                    record: record,
                    thumbnailProvider: { [weak viewModel] id in
                        viewModel?.thumbnail(for: id)
                    }
                )

                if isSelectionMode {
                    RoundedRectangle(cornerRadius: 0)
                        .fill(Color.black.opacity(0.4))

                    Image(systemName: selectedIDs.contains(record.id) ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 22))
                        .foregroundColor(selectedIDs.contains(record.id) ? DesignSystem.Colors.primary : .white.opacity(0.7))
                        .padding(6)
                }
            }
        }
        .contextMenu { contextMenu(for: record) }
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.5).onEnded { _ in
                if !isSelectionMode {
                    isSelectionMode = true
                    selectedIDs = [record.id]
                }
            }
        )
    }

    private func toggleSelection(_ id: UUID) {
        if selectedIDs.contains(id) {
            selectedIDs.remove(id)
            if selectedIDs.isEmpty {
                isSelectionMode = false
            }
        } else {
            selectedIDs.insert(id)
        }
    }

    // MARK: - Context Menu

    @ViewBuilder
    private func contextMenu(for record: PhotoRecord) -> some View {
        Button(role: .destructive) {
            viewModel.deleteRecord(record.id)
        } label: {
            Label("删除", systemImage: "trash")
        }
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        if viewModel.searchQuery.isEmpty {
            EmptyStateView(
                icon: "photo.on.rectangle",
                title: "暂无照片",
                message: "使用下方拍摄按钮开始创作",
                actionTitle: "开始拍摄",
                action: {
                    NotificationCenter.default.post(name: .navigateToCamera, object: nil)
                }
            )
        } else {
            EmptyStateView(
                icon: "magnifyingglass",
                title: "未找到匹配的照片",
                message: "尝试其他关键词"
            )
        }
    }

    // MARK: - Filter Sheet

    private var filterSheetView: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: Array(repeating: .init(.flexible(), spacing: 12), count: 3), spacing: 12) {
                    ForEach(LutFilterPreset.builtInPresets) { preset in
                        Button {
                            if isSelectionMode {
                                viewModel.batchApplyFilter(preset)
                                showFilterSheet = false
                                isSelectionMode = false
                            }
                        } label: {
                            VStack(spacing: 8) {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(DesignSystem.Colors.backgroundSecondary)
                                    .aspectRatio(1, contentMode: .fit)
                                    .overlay {
                                        Image(systemName: "camera.filters")
                                            .font(.system(size: 24))
                                            .foregroundColor(DesignSystem.Colors.textTertiary)
                                    }

                                Text(preset.displayName)
                                    .font(DesignSystem.Typography.caption1)
                                    .foregroundColor(DesignSystem.Colors.textPrimary)
                                    .lineLimit(1)

                                Text(preset.category.rawValue)
                                    .font(DesignSystem.Typography.caption2)
                                    .foregroundColor(DesignSystem.Colors.textTertiary)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .navigationTitle("选择滤镜")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") {
                        showFilterSheet = false
                    }
                }
            }
        }
    }
}

extension Int: @retroactive Identifiable {
    public var id: Int { self }
}