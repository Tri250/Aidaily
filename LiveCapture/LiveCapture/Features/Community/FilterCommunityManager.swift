//
//  FilterCommunityManager.swift
//  LiveCapture
//
//  滤镜社区管理器 - 本地滤镜分享与发现
//

import Foundation
import Combine

#if os(iOS)

final class FilterCommunityManager: ObservableObject {
    @Published var communityFilters: [UserFilter] = []
    @Published var myCreatedFilters: [UserFilter] = []
    @Published var popularFilters: [UserFilter] = []
    @Published var downloadedFilters: [UserFilter] = []

    private let storageKey = "livecapture.community_filters"
    private let myFiltersKey = "livecapture.my_filters"
    private let downloadsKey = "livecapture.downloaded_filters"

    init() {
        loadCommunityFilters()
        loadMyFilters()
        loadDownloadedFilters()
        refreshPopularFilters()
    }

    // MARK: - 内置社区滤镜（20+ 款）

    func loadCommunityFilters() {
        if let saved = loadSavedCommunityFilters(), !saved.isEmpty {
            communityFilters = saved
            return
        }

        communityFilters = builtInCommunityFilters()
        saveCommunityFilters()
    }

    private func builtInCommunityFilters() -> [UserFilter] {
        let creators = ["JettyCoffee", "ZyanNo1", "zzsyppt", "光影猎人", "极简大师", "街头摄影师", "美食博主", "旅行达人"]
        let now = Date()

        return [
            // 1-5: 城市夜景系列
            UserFilter(
                id: "cf_1", name: "东京午夜", creatorName: creators[0],
                parameters: FilterParameters(temperature: -1200, tint: -20, exposure: -0.15, brightness: -0.08, contrast: 1.30, saturation: 1.15, vibrance: 0.12, highlightAmount: 0.85, shadowAmount: -0.10),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 30), downloads: 1520, category: .creative, filterDescription: "霓虹灯下的东京街头，青橙色调营造赛博朋克氛围"
            ),
            UserFilter(
                id: "cf_2", name: "巴黎清晨", creatorName: creators[1],
                parameters: FilterParameters(temperature: 600, tint: 5, exposure: 0.20, brightness: 0.08, contrast: 0.85, saturation: 0.92, vibrance: 0.05, highlightAmount: 0.78, shadowAmount: 0.20),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 28), downloads: 1340, category: .nature, filterDescription: "巴黎清晨的柔和光线，温暖的氛围感"
            ),
            UserFilter(
                id: "cf_3", name: "冰岛蓝调", creatorName: creators[2],
                parameters: FilterParameters(temperature: -2000, tint: -30, exposure: 0.10, brightness: 0.03, contrast: 1.10, saturation: 0.85, vibrance: -0.05, highlightAmount: 0.90, shadowAmount: 0.05),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 25), downloads: 2180, category: .nature, filterDescription: "冰岛冰川与蓝湖的冷色调，清冷而神秘"
            ),
            UserFilter(
                id: "cf_4", name: "摩洛哥暖阳", creatorName: creators[0],
                parameters: FilterParameters(temperature: 2500, tint: 15, exposure: 0.15, brightness: 0.10, contrast: 1.05, saturation: 1.35, vibrance: 0.20, highlightAmount: 0.92, shadowAmount: 0.15),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 22), downloads: 980, category: .vintage, filterDescription: "摩洛哥市场暖阳色调，浓郁的地中海色彩"
            ),
            UserFilter(
                id: "cf_5", name: "北欧极简", creatorName: creators[3],
                parameters: FilterParameters(temperature: -300, tint: -5, exposure: 0.25, brightness: 0.12, contrast: 0.78, saturation: 0.70, vibrance: -0.10, highlightAmount: 0.75, shadowAmount: 0.25),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 20), downloads: 1650, category: .creative, filterDescription: "北欧极简风格，低饱和度、高亮度的通透感"
            ),
            // 6-10: 人像系列
            UserFilter(
                id: "cf_6", name: "奶油肌肤", creatorName: creators[1],
                parameters: FilterParameters(temperature: 500, tint: 8, exposure: 0.20, brightness: 0.10, contrast: 0.80, saturation: 0.95, vibrance: 0.05, highlightAmount: 0.82, shadowAmount: 0.18),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 18), downloads: 3200, category: .portrait, filterDescription: "柔和肤色处理，打造奶油般细腻的肌肤质感"
            ),
            UserFilter(
                id: "cf_7", name: "复古胶片人像", creatorName: creators[4],
                parameters: FilterParameters(temperature: 1500, tint: 10, exposure: -0.05, brightness: -0.03, contrast: 1.15, saturation: 1.10, vibrance: 0.08, highlightAmount: 0.88, shadowAmount: 0.05),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 15), downloads: 890, category: .portrait, filterDescription: "复古胶片质感，温暖色调让人像更有故事感"
            ),
            UserFilter(
                id: "cf_8", name: "清新日系", creatorName: creators[5],
                parameters: FilterParameters(temperature: -200, tint: -3, exposure: 0.35, brightness: 0.15, contrast: 0.75, saturation: 0.88, vibrance: -0.03, highlightAmount: 0.78, shadowAmount: 0.30),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 12), downloads: 2800, category: .portrait, filterDescription: "日系清新风格，明亮通透，适合日常人像"
            ),
            UserFilter(
                id: "cf_9", name: "暗调情绪", creatorName: creators[2],
                parameters: FilterParameters(temperature: -500, tint: -10, exposure: -0.30, brightness: -0.10, contrast: 1.40, saturation: 0.75, vibrance: -0.08, highlightAmount: 0.95, shadowAmount: -0.15),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 10), downloads: 1450, category: .portrait, filterDescription: "暗调情绪人像，深沉而富有表现力"
            ),
            UserFilter(
                id: "cf_10", name: "金色时刻", creatorName: creators[0],
                parameters: FilterParameters(temperature: 2000, tint: 12, exposure: 0.10, brightness: 0.05, contrast: 0.95, saturation: 1.20, vibrance: 0.15, highlightAmount: 0.85, shadowAmount: 0.10),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 8), downloads: 1950, category: .portrait, filterDescription: "黄金时刻色调，温暖的金色光线洒满画面"
            ),
            // 11-15: 美食/生活系列
            UserFilter(
                id: "cf_11", name: "食欲大增", creatorName: creators[6],
                parameters: FilterParameters(temperature: 1500, tint: 5, exposure: 0.15, brightness: 0.08, contrast: 1.15, saturation: 1.40, vibrance: 0.25, highlightAmount: 0.90, shadowAmount: 0.10),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 7), downloads: 2100, category: .food, filterDescription: "增加食物饱和度与暖色调，让每一道菜都诱人无比"
            ),
            UserFilter(
                id: "cf_12", name: "咖啡时光", creatorName: creators[7],
                parameters: FilterParameters(temperature: 1800, tint: 8, exposure: 0.05, brightness: 0.03, contrast: 1.05, saturation: 1.05, vibrance: 0.08, highlightAmount: 0.88, shadowAmount: 0.12),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 5), downloads: 1250, category: .food, filterDescription: "咖啡馆暖调氛围，适合记录惬意时光"
            ),
            UserFilter(
                id: "cf_13", name: "甜品诱惑", creatorName: creators[6],
                parameters: FilterParameters(temperature: 1000, tint: 3, exposure: 0.25, brightness: 0.12, contrast: 0.90, saturation: 1.25, vibrance: 0.18, highlightAmount: 0.85, shadowAmount: 0.20),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 4), downloads: 1800, category: .food, filterDescription: "甜品专属滤镜，柔和明亮，突出甜品的精致感"
            ),
            UserFilter(
                id: "cf_14", name: "深夜食堂", creatorName: creators[5],
                parameters: FilterParameters(temperature: 2000, tint: 10, exposure: -0.10, brightness: -0.05, contrast: 1.20, saturation: 1.15, vibrance: 0.10, highlightAmount: 0.92, shadowAmount: -0.05),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 3), downloads: 780, category: .food, filterDescription: "深夜食堂的暖黄灯光，营造温馨的用餐氛围"
            ),
            UserFilter(
                id: "cf_15", name: "生活碎片", creatorName: creators[4],
                parameters: FilterParameters(temperature: 300, tint: 0, exposure: 0.10, brightness: 0.05, contrast: 0.95, saturation: 1.05, vibrance: 0.05, highlightAmount: 0.90, shadowAmount: 0.08),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 2), downloads: 1600, category: .vintage, filterDescription: "记录日常生活的温暖滤镜，真实而自然"
            ),
            // 16-20: 创意/黑白系列
            UserFilter(
                id: "cf_16", name: "赛博朋克", creatorName: creators[0],
                parameters: FilterParameters(temperature: -1500, tint: -25, exposure: -0.10, brightness: -0.05, contrast: 1.50, saturation: 1.30, vibrance: 0.20, highlightAmount: 0.80, shadowAmount: -0.15),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 35), downloads: 2300, category: .creative, filterDescription: "赛博朋克风格，高对比度青橙色调，未来感十足"
            ),
            UserFilter(
                id: "cf_17", name: "经典黑白", creatorName: creators[3],
                parameters: FilterParameters(temperature: 0, tint: 0, exposure: 0, brightness: -0.05, contrast: 1.35, saturation: 0, vibrance: 0, highlightAmount: 1.0, shadowAmount: -0.08, isMonochrome: true, monochromeIntensity: 1.0, monochromeColorR: 0.95, monochromeColorG: 0.94, monochromeColorB: 0.92),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 32), downloads: 1100, category: .bw, filterDescription: "经典黑白影调，纯粹的光影表达"
            ),
            UserFilter(
                id: "cf_18", name: "暖调黑白", creatorName: creators[1],
                parameters: FilterParameters(temperature: 0, tint: 0, exposure: 0.05, brightness: 0.02, contrast: 1.20, saturation: 0, vibrance: 0, highlightAmount: 0.95, shadowAmount: 0.05, isMonochrome: true, monochromeIntensity: 1.0, monochromeColorR: 0.98, monochromeColorG: 0.92, monochromeColorB: 0.85),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 30), downloads: 850, category: .bw, filterDescription: "温暖色调的黑白风格，带一丝怀旧感"
            ),
            UserFilter(
                id: "cf_19", name: "梦幻柔焦", creatorName: creators[2],
                parameters: FilterParameters(temperature: 500, tint: 5, exposure: 0.30, brightness: 0.15, contrast: 0.65, saturation: 0.85, vibrance: -0.05, highlightAmount: 0.70, shadowAmount: 0.30),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 27), downloads: 1900, category: .creative, filterDescription: "梦幻柔焦效果，高光溢出营造浪漫氛围"
            ),
            UserFilter(
                id: "cf_20", name: "老照片", creatorName: creators[7],
                parameters: FilterParameters(temperature: 2000, tint: 15, exposure: 0.10, brightness: 0.05, contrast: 0.70, saturation: 0.60, vibrance: -0.15, highlightAmount: 0.65, shadowAmount: 0.30),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 24), downloads: 1350, category: .vintage, filterDescription: "老照片风格，褪色、泛黄、低对比度，营造年代感"
            ),
            // 附加5款
            UserFilter(
                id: "cf_21", name: "西山晚霞", creatorName: creators[4],
                parameters: FilterParameters(temperature: 3000, tint: 20, exposure: 0.05, brightness: 0.03, contrast: 1.10, saturation: 1.30, vibrance: 0.22, highlightAmount: 0.88, shadowAmount: 0.12),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 19), downloads: 750, category: .nature, filterDescription: "日落晚霞色调，绚丽的天空色彩"
            ),
            UserFilter(
                id: "cf_22", name: "雨巷", creatorName: creators[5],
                parameters: FilterParameters(temperature: -800, tint: -15, exposure: -0.20, brightness: -0.08, contrast: 0.90, saturation: 0.70, vibrance: -0.10, highlightAmount: 0.92, shadowAmount: 0.05),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 16), downloads: 620, category: .creative, filterDescription: "雨天氛围，冷色调、低饱和度，营造忧郁浪漫"
            ),
            UserFilter(
                id: "cf_23", name: "胶片褪色", creatorName: creators[0],
                parameters: FilterParameters(temperature: 1200, tint: 8, exposure: 0.15, brightness: 0.06, contrast: 0.75, saturation: 0.72, vibrance: -0.08, highlightAmount: 0.72, shadowAmount: 0.25),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 14), downloads: 1580, category: .film, filterDescription: "胶片褪色效果，温暖的复古质感"
            ),
            UserFilter(
                id: "cf_24", name: "青橙调", creatorName: creators[2],
                parameters: FilterParameters(temperature: -1000, tint: -20, exposure: 0, brightness: -0.02, contrast: 1.25, saturation: 1.20, vibrance: 0.15, highlightAmount: 0.88, shadowAmount: -0.05),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 11), downloads: 2800, category: .creative, filterDescription: "经典青橙对比色调，电影感十足"
            ),
            UserFilter(
                id: "cf_25", name: "春日花语", creatorName: creators[3],
                parameters: FilterParameters(temperature: 400, tint: -5, exposure: 0.25, brightness: 0.10, contrast: 0.85, saturation: 1.15, vibrance: 0.10, highlightAmount: 0.82, shadowAmount: 0.22),
                previewImageName: nil, createdDate: now.addingTimeInterval(-86400 * 9), downloads: 1050, category: .nature, filterDescription: "春日花卉滤镜，明亮柔和的色调"
            ),
        ]
    }

    // MARK: - 创建滤镜

    func createFilter(name: String, parameters: FilterParameters, category: FilterCategory) {
        let newFilter = UserFilter(
            id: UUID().uuidString,
            name: name,
            creatorName: "我",
            parameters: parameters,
            previewImageName: nil,
            createdDate: Date(),
            downloads: 0,
            category: category,
            filterDescription: "我的自定义滤镜"
        )
        myCreatedFilters.append(newFilter)
        saveMyFilters()
    }

    // MARK: - 下载/移除滤镜

    func downloadFilter(_ filter: UserFilter) {
        guard !downloadedFilters.contains(where: { $0.id == filter.id }) else { return }
        var updated = filter
        // 递增下载量
        if let communityIndex = communityFilters.firstIndex(where: { $0.id == filter.id }) {
            communityFilters[communityIndex].downloads += 1
            saveCommunityFilters()
        }
        downloadedFilters.append(filter)
        saveDownloadedFilters()
        refreshPopularFilters()
    }

    func removeFilter(_ filter: UserFilter) {
        downloadedFilters.removeAll { $0.id == filter.id }
        saveDownloadedFilters()
    }

    func isDownloaded(_ filter: UserFilter) -> Bool {
        downloadedFilters.contains(where: { $0.id == filter.id })
    }

    // MARK: - 热门排序

    func getPopularFilters(limit: Int = 10) -> [UserFilter] {
        Array(communityFilters.sorted { $0.downloads > $1.downloads }.prefix(limit))
    }

    func refreshPopularFilters() {
        popularFilters = getPopularFilters(limit: 10)
    }

    // MARK: - 分类筛选

    func getFiltersForCategory(_ category: FilterCategory) -> [UserFilter] {
        communityFilters.filter { $0.category == category }
    }

    // MARK: - 持久化

    private func saveCommunityFilters() {
        guard let data = try? JSONEncoder().encode(communityFilters) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }

    private func loadSavedCommunityFilters() -> [UserFilter]? {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let filters = try? JSONDecoder().decode([UserFilter].self, from: data)
        else { return nil }
        return filters
    }

    private func saveMyFilters() {
        guard let data = try? JSONEncoder().encode(myCreatedFilters) else { return }
        UserDefaults.standard.set(data, forKey: myFiltersKey)
    }

    private func loadMyFilters() {
        guard let data = UserDefaults.standard.data(forKey: myFiltersKey),
              let filters = try? JSONDecoder().decode([UserFilter].self, from: data)
        else { myCreatedFilters = []; return }
        myCreatedFilters = filters
    }

    private func saveDownloadedFilters() {
        guard let data = try? JSONEncoder().encode(downloadedFilters) else { return }
        UserDefaults.standard.set(data, forKey: downloadsKey)
    }

    private func loadDownloadedFilters() {
        guard let data = UserDefaults.standard.data(forKey: downloadsKey),
              let filters = try? JSONDecoder().decode([UserFilter].self, from: data)
        else { downloadedFilters = []; return }
        downloadedFilters = filters
    }
}

#endif