//
//  FilterPresetManager.swift
//  LiveCapture
//
//  滤镜预设管理器
//
//  ## 文件作用
//  管理所有滤镜预设的加载、收藏、选择和排序
//  提供按分类筛选和搜索功能
//  使用 ObservableObject 发布状态变化供 SwiftUI 视图绑定
//
//  ## 主要类
//  - FilterPresetManager: 滤镜预设管理器，管理预设生命周期
//
//  ## Published 属性
//  - presets: 所有可用预设列表
//  - favoritePresets: 收藏的预设列表
//  - selectedPreset: 当前选中的预设
//  - filterIntensity: 当前滤镜强度（0-1）
//
//  ## 主要方法
//  - loadBuiltInPresets(): 加载内置 12 款预设
//  - toggleFavorite(_:): 切换收藏状态
//  - reorderPresets(_:): 重新排序预设
//  - getPresetsForCategory(_:): 按分类获取预设
//  - saveFavorites() / loadFavorites(): 持久化收藏列表
//
//  ## 持久化
//  使用 UserDefaults 存储收藏的预设名称列表
//  启动时自动恢复收藏状态
//

import Foundation
import Combine

#if os(iOS)

/// 滤镜预设管理器
final class FilterPresetManager: ObservableObject {

    // MARK: - Published 属性

    /// 所有可用预设
    @Published var presets: [LutFilterPreset] = []
    /// 收藏的预设
    @Published var favoritePresets: [LutFilterPreset] = []
    /// 当前选中的预设
    @Published var selectedPreset: LutFilterPreset?
    /// 当前滤镜强度（0-1），默认 1.0
    @Published var filterIntensity: Float = 1.0

    // MARK: - 私有属性

    /// UserDefaults 中存储收藏预设名称的 key
    private let favoritesKey = "livecapture.filter.favorites"
    /// UserDefaults 中存储预设排序的 key
    private let orderKey = "livecapture.filter.order"

    // MARK: - 初始化

    init() {
        loadBuiltInPresets()
        loadFavorites()
        // 默认选中第一个预设
        if selectedPreset == nil, let first = presets.first {
            selectedPreset = first
        }
    }

    // MARK: - 内置预设加载

    /// 加载内置 12 款经典滤镜预设
    func loadBuiltInPresets() {
        let builtIn = LutFilterPreset.builtInPresets

        // 尝试恢复上次的排序
        if let savedOrder = UserDefaults.standard.array(forKey: orderKey) as? [String] {
            var ordered: [LutFilterPreset] = []
            let presetMap = Dictionary(uniqueKeysWithValues: builtIn.map { ($0.name, $0) })

            // 按保存的顺序排列
            for name in savedOrder {
                if let preset = presetMap[name] {
                    ordered.append(preset)
                }
            }

            // 添加新预设（不在保存顺序中的）
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
    /// - Parameter preset: 要切换收藏状态的预设
    func toggleFavorite(_ preset: LutFilterPreset) {
        if favoritePresets.contains(where: { $0.id == preset.id }) {
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

    /// 持久化收藏列表到 UserDefaults
    private func saveFavorites() {
        let names = favoritePresets.map { $0.name }
        UserDefaults.standard.set(names, forKey: favoritesKey)
    }

    /// 从 UserDefaults 恢复收藏列表
    private func loadFavorites() {
        guard let names = UserDefaults.standard.array(forKey: favoritesKey) as? [String] else {
            favoritePresets = []
            return
        }

        let presetMap = Dictionary(uniqueKeysWithValues: presets.map { ($0.name, $0) })
        favoritePresets = names.compactMap { presetMap[$0] }
    }

    // MARK: - 排序

    /// 重新排序预设列表
    /// - Parameter presets: 新顺序的预设数组
    func reorderPresets(_ presets: [LutFilterPreset]) {
        self.presets = presets
        // 持久化排序
        let names = presets.map { $0.name }
        UserDefaults.standard.set(names, forKey: orderKey)
    }

    /// 将预设移到最前面（最近使用）
    func bringToFront(_ preset: LutFilterPreset) {
        guard let index = presets.firstIndex(where: { $0.id == preset.id }) else { return }
        presets.remove(at: index)
        presets.insert(preset, at: 0)
        // 持久化排序
        let names = presets.map { $0.name }
        UserDefaults.standard.set(names, forKey: orderKey)
    }

    // MARK: - 分类筛选

    /// 按分类获取预设列表
    /// - Parameter category: 滤镜分类
    /// - Returns: 该分类下的所有预设
    func getPresetsForCategory(_ category: FilterCategory) -> [LutFilterPreset] {
        presets.filter { $0.category == category }
    }

    /// 获取所有分类及其预设数量
    func getCategoryStats() -> [(FilterCategory, Int)] {
        FilterCategory.allCases.map { category in
            let count = presets.filter { $0.category == category }.count
            return (category, count)
        }
    }

    // MARK: - 搜索

    /// 搜索预设（按名称或显示名称）
    /// - Parameter query: 搜索关键词
    /// - Returns: 匹配的预设列表
    func searchPresets(query: String) -> [LutFilterPreset] {
        guard !query.isEmpty else { return presets }
        let lowercased = query.lowercased()
        return presets.filter {
            $0.name.lowercased().contains(lowercased) ||
            $0.displayName.lowercased().contains(lowercased)
        }
    }

    // MARK: - 预设选择

    /// 选择预设并设置默认强度
    func selectPreset(_ preset: LutFilterPreset) {
        selectedPreset = preset
        filterIntensity = preset.defaultIntensity
        // 最近使用的移到最前面
        bringToFront(preset)
    }

    /// 清除滤镜选择（返回无滤镜状态）
    func clearSelection() {
        selectedPreset = nil
        filterIntensity = 1.0
    }
}

#endif