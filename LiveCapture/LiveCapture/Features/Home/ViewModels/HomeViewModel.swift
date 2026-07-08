import Foundation
import Combine
import UIKit

/// 分组模式枚举
enum GroupingMode: String, CaseIterable, Identifiable {
    case none = "全部"
    case date = "日期"
    case scene = "场景"
    case location = "位置"
    case faces = "人脸"

    var id: String { rawValue }

    var iconName: String {
        switch self {
        case .none: return "rectangle.grid.2x2"
        case .date: return "calendar"
        case .scene: return "tag"
        case .location: return "location"
        case .faces: return "face.smiling"
        }
    }
}

final class HomeViewModel: ObservableObject {
    @Published private(set) var records: [PhotoRecord] = []
    @Published var searchQuery: String = ""
    @Published var groupingMode: GroupingMode = .none
    @Published var isSearching: Bool = false
    @Published var isBatchEditing: Bool = false
    @Published var selectedIDs: Set<UUID> = []
    @Published var batchProcessor = BatchProcessor()

    // 分组后的数据
    @Published private(set) var groupedRecords: [(title: String, records: [PhotoRecord])] = []

    private let classifier = SmartAlbumClassifier()
    private let searchEngine = PhotoSearchEngine()
    private var cancellables: Set<AnyCancellable> = []

    init() {
        PhotoStorageService.shared.recordsPublisher
            .receive(on: DispatchQueue.main)
            .map { $0.sorted { $0.creationDate > $1.creationDate } }
            .sink { [weak self] records in
                self?.records = records
                self?.updateGroupedRecords()
            }
            .store(in: &cancellables)

        // 监听搜索和分组变化
        $searchQuery
            .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
            .removeDuplicates()
            .sink { [weak self] _ in
                self?.updateGroupedRecords()
            }
            .store(in: &cancellables)

        $groupingMode
            .sink { [weak self] _ in
                self?.updateGroupedRecords()
            }
            .store(in: &cancellables)
    }

    // MARK: - 数据获取

    /// 获取当前显示的记录（搜索过滤后）
    var filteredRecords: [PhotoRecord] {
        if searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return records
        }
        return searchEngine.search(searchQuery, in: records)
    }

    /// 更新分组数据
    private func updateGroupedRecords() {
        let source = filteredRecords

        switch groupingMode {
        case .none:
            groupedRecords = [("全部", source)]
        case .date:
            let groups = classifier.groupByDate(source)
            groupedRecords = sortDateGroups(groups)
        case .scene:
            let groups = classifier.groupByScene(source)
            groupedRecords = groups.map { (title: $0.key.displayName, records: $0.value) }
                .filter { !$0.records.isEmpty }
                .sorted { $0.records.count > $1.records.count }
        case .location:
            let groups = classifier.groupByLocation(source)
            groupedRecords = groups.map { (title: $0.key, records: $0.value) }
                .filter { !$0.records.isEmpty }
                .sorted { $0.records.count > $1.records.count }
        case .faces:
            let faceGroups = classifier.groupByFaces(source)
            groupedRecords = faceGroups.enumerated().map { index, records in
                (title: "人物 \(index + 1)", records: records)
            }
        }
    }

    /// 日期分组排序
    private func sortDateGroups(_ groups: [String: [PhotoRecord]]) -> [(title: String, records: [PhotoRecord])] {
        let today = "今天"
        let yesterday = "昨天"
        let weekPrefix = "本周"

        var result: [(title: String, records: [PhotoRecord])] = []

        // 今天
        if let todays = groups[today] {
            result.append((title: today, records: todays))
        }
        // 昨天
        if let yesterdays = groups[yesterday] {
            result.append((title: yesterday, records: yesterdays))
        }
        // 本周
        for (key, records) in groups {
            if key.hasPrefix(weekPrefix) {
                result.append((title: key, records: records))
            }
        }
        // 本月
        for (key, records) in groups {
            if !key.hasPrefix(weekPrefix) && key != today && key != yesterday && !key.contains("年") {
                result.append((title: key, records: records))
            }
        }
        // 更早
        for (key, records) in groups {
            if key.contains("年") {
                result.append((title: key, records: records))
            }
        }

        return result
    }

    // MARK: - 删除

    func deleteRecord(_ id: UUID) {
        PhotoStorageService.shared.deleteRecord(id)
    }

    func deleteRecords(_ ids: [UUID]) {
        for id in ids {
            PhotoStorageService.shared.deleteRecord(id)
        }
    }

    // MARK: - 缩略图和原图

    func thumbnail(for id: UUID) -> UIImage? {
        PhotoStorageService.shared.thumbnail(for: id)
    }

    func fullPhoto(for id: UUID) -> UIImage? {
        guard let url = PhotoStorageService.shared.photoURL(for: id),
              let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
    }

    /// 更新照片（编辑后保存）
    func updatePhoto(_ id: UUID, with image: UIImage) {
        guard let url = PhotoStorageService.shared.photoURL(for: id),
              let data = image.jpegData(compressionQuality: 0.95) else { return }
        try? data.write(to: url)
        // 刷新列表
        loadRecords()
    }

    // MARK: - 批量选择

    func toggleSelection(_ id: UUID) {
        if selectedIDs.contains(id) {
            selectedIDs.remove(id)
        } else {
            selectedIDs.insert(id)
        }
    }

    func selectAll() {
        selectedIDs = Set(filteredRecords.map { $0.id })
    }

    func deselectAll() {
        selectedIDs.removeAll()
    }

    // MARK: - 批量编辑

    /// 批量删除选中的照片
    func batchDeleteSelected() {
        let ids = Array(selectedIDs)
        deleteRecords(ids)
        selectedIDs.removeAll()
        isBatchEditing = false
    }

    /// 批量应用滤镜
    func batchApplyFilter(_ filter: LutFilterPreset) {
        let selected = records.filter { selectedIDs.contains($0.id) }
        Task {
            _ = await batchProcessor.applyFilter(filter, to: selected)
            await MainActor.run {
                selectedIDs.removeAll()
                isBatchEditing = false
            }
        }
    }

    /// 批量自动增强
    func batchAutoEnhance() {
        let selected = records.filter { selectedIDs.contains($0.id) }
        Task {
            _ = await batchProcessor.applyAutoEnhance(to: selected)
            await MainActor.run {
                selectedIDs.removeAll()
                isBatchEditing = false
            }
        }
    }
}