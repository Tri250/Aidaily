package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

/**
 * 编辑历史单元测试
 *
 * 测试撤销/重做堆栈、容量限制、Bitmap 内存管理。
 */
class EditHistoryTest {

    private fun createBitmap(w: Int = 10, h: Int = 10): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

    // ====== 基本状态 ======

    @Test
    fun `empty history can't undo`() {
        val history = EditHistory(maxHistorySize = 10)
        assertFalse(history.canUndo)
        assertNull(history.undo())
    }

    @Test
    fun `empty history can't redo`() {
        val history = EditHistory(maxHistorySize = 10)
        assertFalse(history.canRedo)
        assertNull(history.redo())
    }

    @Test
    fun `empty history undoCount is 0`() {
        val history = EditHistory(maxHistorySize = 10)
        assertEquals(0, history.undoCount)
        assertEquals(0, history.redoCount)
    }

    // ====== 单步操作 ======

    @Test
    fun `push one state can undo`() {
        val history = EditHistory(maxHistorySize = 10)
        val bitmap = createBitmap()
        history.pushOperation(bitmap, "测试操作")
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(1, history.undoCount)
        assertEquals("测试操作", history.lastUndoActionName)

        val undone = history.undo()
        assertNotNull(undone)
        assertEquals(0, history.undoCount)
        assertFalse(history.canUndo)
    }

    @Test
    fun `undo then pushRedoOperation then redo works`() {
        val history = EditHistory(maxHistorySize = 10)
        val bmp1 = createBitmap()
        val bmp2 = createBitmap()
        history.pushOperation(bmp1, "操作1")
        history.pushOperation(bmp2, "操作2")

        val undone = history.undo()
        history.pushRedoOperation(undone!!, "操作2")
        assertTrue(history.canRedo)

        val redone = history.redo()
        assertNotNull(redone)
        assertFalse(history.canRedo)
    }

    // ====== 操作名称 ======

    @Test
    fun `lastUndoActionName returns latest action`() {
        val history = EditHistory(maxHistorySize = 10)
        history.pushOperation(createBitmap(), "第一步")
        history.pushOperation(createBitmap(), "第二步")
        assertEquals("第二步", history.lastUndoActionName)
    }

    @Test
    fun `getUndoActionNames returns reversed list`() {
        val history = EditHistory(maxHistorySize = 10)
        history.pushOperation(createBitmap(), "操作A")
        history.pushOperation(createBitmap(), "操作B")
        history.pushOperation(createBitmap(), "操作C")
        val names = history.getUndoActionNames()
        assertEquals(listOf("操作C", "操作B", "操作A"), names)
    }

    @Test
    fun `getRedoActionNames returns reversed list`() {
        val history = EditHistory(maxHistorySize = 10)
        history.pushOperation(createBitmap(), "操作A")
        history.pushOperation(createBitmap(), "操作B")

        val undone = history.undo()!!
        history.pushRedoOperation(undone, "操作B")

        val undone2 = history.undo()!!
        history.pushRedoOperation(undone2, "操作A")

        val names = history.getRedoActionNames()
        assertEquals(listOf("操作A", "操作B"), names)
    }

    // ====== 容量限制 ======

    @Test
    fun `exceeding maxHistorySize drops oldest states`() {
        val history = EditHistory(maxHistorySize = 3)
        history.pushOperation(createBitmap(), "s1")
        history.pushOperation(createBitmap(), "s2")
        history.pushOperation(createBitmap(), "s3")
        history.pushOperation(createBitmap(), "s4")
        // 容量限制 3，s1 被丢弃，保留 s2, s3, s4
        assertEquals(3, history.undoCount)
        val names = history.getUndoActionNames()
        assertEquals(listOf("s4", "s3", "s2"), names)
    }

    @Test
    fun `maxHistorySize 1 only keeps latest`() {
        val history = EditHistory(maxHistorySize = 1)
        history.pushOperation(createBitmap(), "s1")
        history.pushOperation(createBitmap(), "s2")
        assertEquals(1, history.undoCount)
        assertEquals("s2", history.lastUndoActionName)
    }

    // ====== 分支操作 ======

    @Test
    fun `new push clears redo stack`() {
        val history = EditHistory(maxHistorySize = 10)
        history.pushOperation(createBitmap(), "s1")
        history.pushOperation(createBitmap(), "s2")

        val undone = history.undo()
        history.pushRedoOperation(undone!!, "s2")
        assertTrue(history.canRedo)

        // 新操作应清除 redo 栈
        history.pushOperation(createBitmap(), "s2-new")
        assertFalse(history.canRedo)
        assertEquals(0, history.redoCount)
    }

    // ====== clearAll ======

    @Test
    fun `clearAll empties all stacks`() {
        val history = EditHistory(maxHistorySize = 10)
        history.pushOperation(createBitmap(), "s1")
        history.pushOperation(createBitmap(), "s2")

        history.clearAll()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(0, history.undoCount)
        assertEquals(0, history.redoCount)
        assertNull(history.lastUndoActionName)
        assertNull(history.lastRedoActionName)
    }

    // ====== 边界 ======

    @Test
    fun `undo from empty returns null`() {
        val history = EditHistory(maxHistorySize = 10)
        assertNull(history.undo())
    }

    @Test
    fun `redo from empty returns null`() {
        val history = EditHistory(maxHistorySize = 10)
        assertNull(history.redo())
    }

    @Test
    fun `maxHistorySize 0 works`() {
        val history = EditHistory(maxHistorySize = 0)
        history.pushOperation(createBitmap(), "test")
        assertEquals(0, history.undoCount)
    }

    @Test
    fun `getUndoActionNames empty returns empty list`() {
        val history = EditHistory(maxHistorySize = 10)
        assertEquals(emptyList<String>(), history.getUndoActionNames())
    }

    @Test
    fun `getRedoActionNames empty returns empty list`() {
        val history = EditHistory(maxHistorySize = 10)
        assertEquals(emptyList<String>(), history.getRedoActionNames())
    }

    // ====== 内存使用 ======

    @Test
    fun `getMemoryUsage returns valid pair`() {
        val history = EditHistory(maxHistorySize = 10)
        val (undoMb, redoMb) = history.getMemoryUsage()
        assertTrue(undoMb >= 0f)
        assertTrue(redoMb >= 0f)
    }

    // ====== EditOperation 数据类 ======

    @Test
    fun `editOperation data class equality`() {
        val bmp = createBitmap()
        val a = EditHistory.EditOperation("crop", bmp, 100L)
        val b = EditHistory.EditOperation("crop", bmp, 100L)
        assertEquals(a.actionName, b.actionName)
        assertEquals(a.timestamp, b.timestamp)
    }
}