//
//  FilterPresetManager.swift
//  LiveCapture
//
//  滤镜预设管理器 - 全面管理滤镜预设的加载、收藏、选择和排序
//
//  ## 文件作用
//  管理所有滤镜预设的生命周期，包括内置 30+ 款预设和自定义预设
//  提供按分类筛选、搜索、收藏、最近使用、导入导出等功能
//  使用 ObservableObject 发布状态变化供 SwiftUI 视图绑定
//
//  ## 主要类
//  - FilterPresetManager: 滤镜预设管理器
//
//  ## Published 属性
//  - presets: 所有可用预设列表
//  - favoritePresets: 收藏的预设列表
//  - recentPresets: 最近使用的预设（最多 10 个）
//  - selectedPreset: 当前选中的预设
//  - filterIntensity: 当前滤镜强度（0-1）
//  - activeCategory: 当前激活的分类筛选
//  - searchQuery: 当前搜索关键词
//  - customPresets: 用户自定义/导入的预设
//
//  ## 主要方法
//  - loadBuiltInPresets(): 加载 30+ 款内置预设
//  - toggleFavorite(_:): 切换收藏状态
//  - selectPreset(_:): 选择预设
//  - getPresetsForCategory(_:): 按分类获取预设
//  - searchPresets(query:): 搜索预设
//  - importPreset(_:): 导入自定义预设
//  - exportPreset(_:): 导出预设为可分享格式
//  - addCustomPreset(_:): 添加自定义预设
//
//  ## 持久化
//  使用 UserDefaults 存储收藏列表、最近使用列表和自定义预设
//  启动时自动恢复所有状态
//

import Foundation
import Combine
import CoreImage
import UIKit

#if os(iOS)

/// 滤镜预设管理器
final class FilterPresetManager: ObservableObject {

    // MARK: - Published 属性

    /// 所有可用预设（内置 + 自定义）
    @Published var presets: [LutFilterPreset] = []
    /// 收藏的预设
    @Published var favoritePresets: [LutFilterPreset] = []
    /// 最近使用的预设（最多 10 个）
    @Published var recentPresets: [LutFilterPreset] = []
    /// 当前选中的预设
    @Published var selectedPreset: LutFilterPreset?
    /// 当前滤镜强度（0-1），默认 1.0
    @Published var filterIntensity: Float = 1.0
    /// 当前激活的分类筛选（nil 表示显示全部）
    @Published var activeCategory: FilterCategory? = nil
    /// 搜索关键词
    @Published var searchQuery: String = ""
    /// 用户自定义/导入的预设
    @Published var customPresets: [LutFilterPreset] = []

    // MARK: - 计算属性

    /// 当前显示的预设列表（应用分类筛选和搜索后）
    var displayedPresets: [LutFilterPreset] {
        var result = presets

        // 分类筛选
        if let category = activeCategory {
            result = result.filter { $0.category == category }
        }

        // 搜索筛选
        if !searchQuery.isEmpty {
            let lowercased = searchQuery.lowercased()
            result = result.filter {
                $0.name.lowercased().contains(lowercased) ||
                $0.displayName.lowercased().contains(lowercased)
            }
        }

        return result
    }

    /// 所有分类
    var allCategories: [FilterCategory] {
        FilterCategory.allCases
    }

    /// 分类及其预设数量统计
    var categoryStats: [(FilterCategory, Int)] {
        let map = Dictionary(grouping: presets, by: { $0.category })
        return FilterCategory.allCases.map { cat in
            (cat, map[cat]?.count ?? 0)
        }
    }

    // MARK: - 私有属性

    /// UserDefaults key
    private let favoritesKey = "livecapture.filter.v2.favorites"
    private let recentsKey = "livecapture.filter.v2.recents"
    private let customsKey = "livecapture.filter.v2.customs"
    private let orderKey = "livecapture.filter.v2.order"
    private let selectedKey = "livecapture.filter.v2.selected"
    private let intensityKey = "livecapture.filter.v2.intensity"

    /// 最近使用最大数量
    private let maxRecentCount = 10

    // MARK: - 初始化

    init() {
        loadBuiltInPresets()
        loadCustomPresets()
        loadFavorites()
        loadRecents()
        loadSelectedPreset()
        loadFilterIntensity()

        // 默认选中第一个预设
        if selectedPreset == nil, let first = presets.first {
            selectedPreset = first
        }
    }

    // MARK: - 内置预设加载

    /// 加载 30+ 款内置滤镜预设
    func loadBuiltInPresets() {
        let builtIn = LutFilterPreset.allBuiltInPresets

        // 尝试恢复上次的排序
        if let savedOrder = UserDefaults.standard.array(forKey: orderKey) as? [String] {
            var ordered: [LutFilterPreset] = []
            let presetMap = Dictionary(uniqueKeysWithValues: builtIn.map { ($0.name, $0) })

            for name in savedOrder {
                if let preset = presetMap[name] {
                    ordered.append(preset)
                }
            }

            for preset in builtIn {
                if !ordered.contains(where: { $0.name == preset.name }) {
                    ordered.append(preset)
                }
            }

            presets = ordered
        } else {
            presets = builtIn
        }
    }

    // MARK: - 收藏管理

    /// 切换预设的收藏状态
    func toggleFavorite(_ preset: LutFilterPreset) {
        if isFavorite(preset) {
            favoritePresets.removeAll { $0.id == preset.id }
        } else {
            favoritePresets.append(preset)
        }
        saveFavorites()
    }

    /// 检查预设是否已收藏
    func isFavorite(_ preset: LutFilterPreset) -> Bool {
        favoritePresets.contains(where: { $0.id == preset.id })
    }

    /// 持久化收藏列表
    private func saveFavorites() {
        let ids = favoritePresets.map { $0.id.uuidString }
        UserDefaults.standard.set(ids, forKey: favoritesKey)
    }

    /// 恢复收藏列表
    private func loadFavorites() {
        guard let ids = UserDefaults.standard.array(forKey: favoritesKey) as? [String] else {
            favoritePresets = []
            return
        }
        let presetMap = Dictionary(uniqueKeysWithValues: presets.map { ($0.id.uuidString, $0) })
        favoritePresets = ids.compactMap { presetMap[$0] }
    }

    // MARK: - 最近使用

    /// 添加到最近使用列表
    func addToRecent(_ preset: LutFilterPreset) {
        // 移除已存在的相同预设
        recentPresets.removeAll { $0.id == preset.id }
        // 插入到最前面
        recentPresets.insert(preset, at: 0)
        // 限制最大数量
        if recentPresets.count > maxRecentCount {
            recentPresets = Array(recentPresets.prefix(maxRecentCount))
        }
        saveRecents()
    }

    /// 持久化最近使用列表
    private func saveRecents() {
        let ids = recentPresets.map { $0.id.uuidString }
        UserDefaults.standard.set(ids, forKey: recentsKey)
    }

    /// 恢复最近使用列表
    private func loadRecents() {
        guard let ids = UserDefaults.standard.array(forKey: recentsKey) as? [String] else {
            recentPresets = []
            return
        }
        let presetMap = Dictionary(uniqueKeysWithValues: presets.map { ($0.id.uuidString, $0) })
        recentPresets = ids.compactMap { presetMap[$0] }
    }

    // MARK: - 排序

    /// 重新排序预设列表
    func reorderPresets(_ presets: [LutFilterPreset]) {
        self.presets = presets
        let names = presets.map { $0.name }
        UserDefaults.standard.set(names, forKey: orderKey)
    }

    /// 将预设移到最前面（最近使用）
    func bringToFront(_ preset: LutFilterPreset) {
        guard let index = presets.firstIndex(where: { $0.id == preset.id }) else { return }
        presets.remove(at: index)
        presets.insert(preset, at: 0)
        let names = presets.map { $0.name }
        UserDefaults.standard.set(names, forKey: orderKey)
    }

    // MARK: - 分类筛选

    /// 按分类获取预设列表
    func getPresetsForCategory(_ category: FilterCategory) -> [LutFilterPreset] {
        presets.filter { $0.category == category }
    }

    /// 设置分类筛选
    func setCategory(_ category: FilterCategory?) {
        activeCategory = category
    }

    /// 清除分类筛选
    func clearCategory() {
        activeCategory = nil
    }

    // MARK: - 搜索

    /// 搜索预设（按名称或显示名称）
    func searchPresets(query: String) -> [LutFilterPreset] {
        LutFilterPreset.search(query)
    }

    // MARK: - 预设选择

    /// 选择预设并设置默认强度
    func selectPreset(_ preset: LutFilterPreset) {
        selectedPreset = preset
        filterIntensity = preset.defaultIntensity
        addToRecent(preset)
        saveSelectedPreset()
        saveFilterIntensity()
    }

    /// 清除滤镜选择（返回无滤镜状态）
    func clearSelection() {
        selectedPreset = nil
        filterIntensity = 1.0
        saveSelectedPreset()
        saveFilterIntensity()
    }

    /// 设置滤镜强度
    func setIntensity(_ intensity: Float) {
        filterIntensity = max(0.0, min(1.0, intensity))
        saveFilterIntensity()
    }

    // MARK: - 自定义预设管理

    /// 添加自定义预设
    func addCustomPreset(_ preset: LutFilterPreset) {
        customPresets.append(preset)
        presets.append(preset)
        saveCustomPresets()
    }

    /// 删除自定义预设
    func removeCustomPreset(_ preset: LutFilterPreset) {
        customPresets.removeAll { $0.id == preset.id }
        presets.removeAll { $0.id == preset.id }
        if selectedPreset?.id == preset.id {
            clearSelection()
        }
        favoritePresets.removeAll { $0.id == preset.id }
        recentPresets.removeAll { $0.id == preset.id }
        saveCustomPresets()
        saveFavorites()
        saveRecents()
    }

    /// 从 CIFilter 参数创建自定义预设
    func createPreset(
        name: String,
        displayName: String,
        category: FilterCategory,
        parameters: FilterParameters,
        description: String = ""
    ) -> LutFilterPreset {
        let preset = LutFilterPreset(
            name: "custom_\(name)",
            displayName: displayName,
            category: category,
            parameters: parameters,
            defaultIntensity: 0.85,
            description: description
        )
        addCustomPreset(preset)
        return preset
    }

    /// 导出预设为可分享的 JSON 数据
    func exportPreset(_ preset: LutFilterPreset) -> Data? {
        try? JSONEncoder().encode(preset)
    }

    /// 从 JSON 数据导入预设
    func importPreset(from data: Data) throws -> LutFilterPreset {
        let preset = try JSONDecoder().decode(LutFilterPreset.self, from: data)
        addCustomPreset(preset)
        return preset
    }

    // MARK: - 持久化

    private func saveCustomPresets() {
        guard let data = try? JSONEncoder().encode(customPresets) else { return }
        UserDefaults.standard.set(data, forKey: customsKey)
    }

    private func loadCustomPresets() {
        guard let data = UserDefaults.standard.data(forKey: customsKey),
              let presets = try? JSONDecoder().decode([LutFilterPreset].self, from: data)
        else {
            customPresets = []
            return
        }
        customPresets = presets
        // 合并到总预设列表
        for preset in customPresets {
            if !self.presets.contains(where: { $0.id == preset.id }) {
                self.presets.append(preset)
            }
        }
    }

    private func saveSelectedPreset() {
        if let selected = selectedPreset {
            UserDefaults.standard.set(selected.id.uuidString, forKey: selectedKey)
        } else {
            UserDefaults.standard.removeObject(forKey: selectedKey)
        }
    }

    private func loadSelectedPreset() {
        guard let idString = UserDefaults.standard.string(forKey: selectedKey),
              let id = UUID(uuidString: idString)
        else { return }
        selectedPreset = presets.first(where: { $0.id == id })
    }

    private func saveFilterIntensity() {
        UserDefaults.standard.set(filterIntensity, forKey: intensityKey)
    }

    private func loadFilterIntensity() {
        if UserDefaults.standard.object(forKey: intensityKey) != nil {
            filterIntensity = UserDefaults.standard.float(forKey: intensityKey)
        }
    }

    // MARK: - 滤镜预览缩略图生成

    /// 从 CIImage 生成滤镜预览缩略图
    /// 使用共享的 FilterProcessor 进行高效处理
    func generateThumbnail(
        from image: CIImage,
        preset: LutFilterPreset,
        targetSize: CGSize,
        intensity: Float? = nil
    ) -> UIImage? {
        let processor = FilterProcessor.shared
        let filtered = processor.applyFilter(
            to: image,
            preset: preset,
            intensity: intensity ?? preset.defaultIntensity
        )
        return processor.renderToUIImage(filtered, targetSize: targetSize)
    }
}

#endif