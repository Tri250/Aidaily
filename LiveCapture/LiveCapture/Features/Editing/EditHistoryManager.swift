//
//  EditHistoryManager.swift
//  LiveCapture
//
//  编辑历史管理器 - 撤销/重做栈
//

import Foundation
import SwiftUI
import Combine

#if os(iOS)

/// 编辑历史步骤
struct EditHistoryStep: Identifiable, Equatable {
    let id = UUID()
    let filterName: String
    let timestamp: Date
    let thumbnail: UIImage?
    let parameters: [String: Any]

    static func == (lhs: EditHistoryStep, rhs: EditHistoryStep) -> Bool {
        lhs.id == rhs.id
    }

    var displayName: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "HH:mm:ss"
        return "\(filterName) \(formatter.string(from: timestamp))"
    }
}

/// 编辑历史管理器
final class EditHistoryManager: ObservableObject {

    // MARK: - 发布属性

    @Published var historySteps: [EditHistoryStep] = []
    @Published var currentIndex: Int = -1
    @Published var canUndo: Bool = false
    @Published var canRedo: Bool = false

    /// 最大历史步骤数
    private let maxSteps: Int = 50

    // MARK: - 初始化

    init() {}

    // MARK: - 记录历史

    /// 记录编辑步骤
    func recordStep(filterName: String, parameters: [String: Any] = [:], thumbnail: UIImage? = nil) {
        let step = EditHistoryStep(
            filterName: filterName,
            timestamp: Date(),
            thumbnail: thumbnail,
            parameters: parameters
        )

        // 移除当前位置之后的历史
        if currentIndex < historySteps.count - 1 {
            historySteps = Array(historySteps[...currentIndex])
        }

        historySteps.append(step)

        // 限制最大数量
        if historySteps.count > maxSteps {
            historySteps.removeFirst()
        } else {
            currentIndex = historySteps.count - 1
        }

        updateCapabilities()
    }

    // MARK: - 撤销/重做

    /// 撤销一步
    func undo() -> EditHistoryStep? {
        guard canUndo else { return nil }
        currentIndex = max(0, currentIndex - 1)
        updateCapabilities()
        return historySteps[safe: currentIndex]
    }

    /// 重做一步
    func redo() -> EditHistoryStep? {
        guard canRedo else { return nil }
        currentIndex = min(historySteps.count - 1, currentIndex + 1)
        updateCapabilities()
        return historySteps[safe: currentIndex]
    }

    // MARK: - 重置

    func reset() {
        historySteps.removeAll()
        currentIndex = -1
        updateCapabilities()
    }

    // MARK: - 私有方法

    private func updateCapabilities() {
        canUndo = currentIndex > 0
        canRedo = currentIndex < historySteps.count - 1
    }

    /// 当前步骤
    var currentStep: EditHistoryStep? {
        historySteps[safe: currentIndex]
    }
}

// MARK: - Array 安全扩展

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

#endif