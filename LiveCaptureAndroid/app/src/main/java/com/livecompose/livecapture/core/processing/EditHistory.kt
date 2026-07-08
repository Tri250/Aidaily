package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import java.util.LinkedList

/**
 * 编辑历史管理器
 * 维护编辑操作栈，支持撤销/重做
 * 使用 LRU 策略管理 Bitmap，防止内存溢出
 */
class EditHistory(private val maxHistorySize: Int = 50) {

    /**
     * 编辑操作记录
     */
    data class EditOperation(
        val actionName: String,
        val snapshot: Bitmap,
        val timestamp: Long = System.currentTimeMillis()
    )

    // 撤销栈
    private val undoStack = LinkedList<EditOperation>()

    // 重做栈
    private val redoStack = LinkedList<EditOperation>()

    /**
     * 是否可以撤销
     */
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    /**
     * 是否可以重做
     */
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * 撤销栈大小
     */
    val undoCount: Int get() = undoStack.size

    /**
     * 重做栈大小
     */
    val redoCount: Int get() = redoStack.size

    /**
     * 获取最近的撤销操作名称
     */
    val lastUndoActionName: String?
        get() = undoStack.lastOrNull()?.actionName

    /**
     * 获取最近的重做操作名称
     */
    val lastRedoActionName: String?
        get() = redoStack.lastOrNull()?.actionName

    /**
     * 记录一个新的编辑操作
     * @param bitmap 操作前的 Bitmap
     * @param actionName 操作名称（如 "裁剪", "滤镜", "调整曝光"）
     */
    fun pushOperation(bitmap: Bitmap, actionName: String) {
        // 保存当前状态到撤销栈（强引用持有快照）
        val snapshot = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        val operation = EditOperation(actionName, snapshot)

        undoStack.addLast(operation)

        // 清除重做栈（新操作后不能重做）
        clearRedoStack()

        // 限制大小
        trimUndoStack()
    }

    /**
     * 撤销操作
     * @return 撤销前的 Bitmap，如果无法撤销则返回 null
     */
    fun undo(): Bitmap? {
        if (undoStack.isEmpty()) return null

        val operation = undoStack.removeLast()
        return operation.snapshot
    }

    /**
     * 记录重做操作
     * @param bitmap 重做前的 Bitmap（即当前状态）
     * @param actionName 操作名称
     */
    fun pushRedoOperation(bitmap: Bitmap, actionName: String) {
        val snapshot = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        val operation = EditOperation(actionName, snapshot)
        redoStack.addLast(operation)
    }

    /**
     * 重做操作
     * @return 重做前的 Bitmap，如果无法重做则返回 null
     */
    fun redo(): Bitmap? {
        if (redoStack.isEmpty()) return null

        val operation = redoStack.removeLast()
        return operation.snapshot
    }

    /**
     * 清除撤销栈
     */
    fun clearUndoStack() {
        undoStack.forEach { it.snapshot.recycle() }
        undoStack.clear()
    }

    /**
     * 清除重做栈
     */
    private fun clearRedoStack() {
        redoStack.forEach { it.snapshot.recycle() }
        redoStack.clear()
    }

    /**
     * 清除所有历史
     */
    fun clearAll() {
        clearUndoStack()
        clearRedoStack()
    }

    /**
     * 裁剪撤销栈，移除超出限制的旧操作
     */
    private fun trimUndoStack() {
        while (undoStack.size > maxHistorySize) {
            val removed = undoStack.removeFirst()
            removed.snapshot.recycle()
        }
    }

    /**
     * 获取撤销栈中所有操作名称列表
     */
    fun getUndoActionNames(): List<String> {
        return undoStack.map { it.actionName }.reversed()
    }

    /**
     * 获取重做栈中所有操作名称列表
     */
    fun getRedoActionNames(): List<String> {
        return redoStack.map { it.actionName }.reversed()
    }

    /**
     * 获取当前内存使用情况（估算）
     * @return Pair(撤销栈占用 MB, 重做栈占用 MB)
     */
    fun getMemoryUsage(): Pair<Float, Float> {
        var undoMemory = 0L
        var redoMemory = 0L

        for (op in undoStack) {
            if (!op.snapshot.isRecycled) {
                undoMemory += op.snapshot.allocationByteCount.toLong()
            }
        }

        for (op in redoStack) {
            if (!op.snapshot.isRecycled) {
                redoMemory += op.snapshot.allocationByteCount.toLong()
            }
        }

        return Pair(
            undoMemory / (1024f * 1024f),
            redoMemory / (1024f * 1024f)
        )
    }
}