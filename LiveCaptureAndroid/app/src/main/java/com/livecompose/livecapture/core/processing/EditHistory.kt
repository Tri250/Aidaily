package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import java.lang.ref.WeakReference
import java.util.LinkedList

/**
 * 编辑历史管理器
 * 维护编辑操作栈，支持撤销/重做
 * 使用弱引用管理 Bitmap，自动清理旧快照
 */
class EditHistory(private val maxHistorySize: Int = 50) {

    /**
     * 编辑操作记录
     */
    data class EditOperation(
        val actionName: String,
        val snapshot: WeakReference<Bitmap>,
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
        // 保存当前状态到撤销栈
        val snapshot = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        val operation = EditOperation(actionName, WeakReference(snapshot))

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
        val bitmap = operation.snapshot.get()

        if (bitmap == null) {
            // 快照已被回收，尝试下一个
            return undo()
        }

        // 将当前状态保存到重做栈（调用者需要先 push 当前状态）
        return bitmap
    }

    /**
     * 记录重做操作
     * @param bitmap 重做前的 Bitmap（即当前状态）
     * @param actionName 操作名称
     */
    fun pushRedoOperation(bitmap: Bitmap, actionName: String) {
        val snapshot = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        val operation = EditOperation(actionName, WeakReference(snapshot))
        redoStack.addLast(operation)
    }

    /**
     * 重做操作
     * @return 重做前的 Bitmap，如果无法重做则返回 null
     */
    fun redo(): Bitmap? {
        if (redoStack.isEmpty()) return null

        val operation = redoStack.removeLast()
        val bitmap = operation.snapshot.get()

        if (bitmap == null) {
            return redo()
        }

        return bitmap
    }

    /**
     * 清除撤销栈
     */
    fun clearUndoStack() {
        undoStack.clear()
    }

    /**
     * 清除重做栈
     */
    private fun clearRedoStack() {
        redoStack.clear()
    }

    /**
     * 清除所有历史
     */
    fun clearAll() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * 裁剪撤销栈，移除超出限制的旧操作
     */
    private fun trimUndoStack() {
        while (undoStack.size > maxHistorySize) {
            val removed = undoStack.removeFirst()
            // 显式清除引用以帮助 GC
            removed.snapshot.clear()
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
     * 强制清理所有已回收的弱引用
     */
    fun cleanExpiredReferences() {
        undoStack.removeAll { it.snapshot.get() == null }
        redoStack.removeAll { it.snapshot.get() == null }
    }

    /**
     * 获取当前内存使用情况（估算）
     * @return Pair(撤销栈占用 MB, 重做栈占用 MB)
     */
    fun getMemoryUsage(): Pair<Float, Float> {
        var undoMemory = 0L
        var redoMemory = 0L

        for (op in undoStack) {
            op.snapshot.get()?.let {
                undoMemory += it.allocationByteCount.toLong()
            }
        }

        for (op in redoStack) {
            op.snapshot.get()?.let {
                redoMemory += it.allocationByteCount.toLong()
            }
        }

        return Pair(
            undoMemory / (1024f * 1024f),
            redoMemory / (1024f * 1024f)
        )
    }
}