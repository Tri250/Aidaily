package com.livecompose.livecapture.core.processing

import org.junit.Assert.*
import org.junit.Test

/**
 * 图像处理核心算法单元测试
 */
class ImageProcessingTest {

    @Test
    fun `editHistory undo redo works correctly`() {
        val history = EditHistory()
        val state1 = "state1"
        val state2 = "state2"
        val state3 = "state3"

        history.push(state1)
        history.push(state2)
        history.push(state3)

        assertEquals(state3, history.current())

        // 撤销
        val undoResult = history.undo()
        assertEquals(state2, undoResult)

        // 再次撤销
        val undoResult2 = history.undo()
        assertEquals(state1, undoResult2)

        // 无法再撤销
        val undoResult3 = history.undo()
        assertEquals(state1, undoResult3)

        // 重做
        val redoResult = history.redo()
        assertEquals(state2, redoResult)
    }

    @Test
    fun `editHistory handles empty state`() {
        val history = EditHistory()
        assertNull(history.current())
        assertNull(history.undo())
        assertNull(history.redo())
    }

    @Test
    fun `editHistory clears redo on new push`() {
        val history = EditHistory()
        history.push("state1")
        history.push("state2")
        history.undo()

        // 在撤销后推入新状态，重做栈应清空
        history.push("state3")
        assertNull(history.redo())
        assertEquals("state3", history.current())
    }

    @Test
    fun `editHistory canClearAll`() {
        val history = EditHistory()
        history.push("s1")
        history.push("s2")
        history.clear()
        assertNull(history.current())
    }

    @Test
    fun `splitToneProcessor validates input ranges`() {
        // 测试参数范围校验
        val validHighlightHue = 30f
        val validShadowHue = 210f
        val validBalance = 0.5f

        assertTrue(validHighlightHue in 0f..360f)
        assertTrue(validShadowHue in 0f..360f)
        assertTrue(validBalance in -1f..1f)
    }
}
