package com.livecompose.livecapture.core.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CompositionResult 数据类的综合单元测试
 * 覆盖所有公共方法、属性访问器、equals/hashCode、默认值以及边界情况
 */
class CompositionResultTest {

    // region 辅助方法

    /** 创建一个默认的 CompositionResult 实例，方便各测试复用 */
    private fun createDefaultResult(
        bbox: FloatArray = floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f),
        action: CompositionResult.ActionType = CompositionResult.ActionType.STOP,
        actionProbabilities: FloatArray = FloatArray(7) { 1f / 7f },
        confidence: Float = 0.5f,
        faceCoverage: Float = 0f,
        ruleOfThirdsScore: Float = 0f,
        safetyMarginScore: Float = 1f
    ) = CompositionResult(
        bbox = bbox,
        action = action,
        actionProbabilities = actionProbabilities,
        confidence = confidence,
        faceCoverage = faceCoverage,
        ruleOfThirdsScore = ruleOfThirdsScore,
        safetyMarginScore = safetyMarginScore
    )

    // endregion

    // region bbox 属性访问器测试

    @Test
    fun `bboxCenterX 返回 bbox 数组的第一个元素`() {
        val result = createDefaultResult(bbox = floatArrayOf(0.25f, 0.5f, 0.4f, 0.6f))
        assertEquals(0.25f, result.bboxCenterX, 0.001f)
    }

    @Test
    fun `bboxCenterY 返回 bbox 数组的第二个元素`() {
        val result = createDefaultResult(bbox = floatArrayOf(0.5f, 0.75f, 0.4f, 0.6f))
        assertEquals(0.75f, result.bboxCenterY, 0.001f)
    }

    @Test
    fun `bboxWidth 返回 bbox 数组的第三个元素`() {
        val result = createDefaultResult(bbox = floatArrayOf(0.5f, 0.5f, 0.9f, 0.6f))
        assertEquals(0.9f, result.bboxWidth, 0.001f)
    }

    @Test
    fun `bboxHeight 返回 bbox 数组的第四个元素`() {
        val result = createDefaultResult(bbox = floatArrayOf(0.5f, 0.5f, 0.4f, 1.0f))
        assertEquals(1.0f, result.bboxHeight, 0.001f)
    }

    @Test
    fun `bbox 属性在边界值零时正确返回`() {
        val result = createDefaultResult(bbox = floatArrayOf(0f, 0f, 0f, 0f))
        assertEquals(0f, result.bboxCenterX, 0.001f)
        assertEquals(0f, result.bboxCenterY, 0.001f)
        assertEquals(0f, result.bboxWidth, 0.001f)
        assertEquals(0f, result.bboxHeight, 0.001f)
    }

    @Test
    fun `bbox 属性在最大值一时正确返回`() {
        val result = createDefaultResult(bbox = floatArrayOf(1f, 1f, 1f, 1f))
        assertEquals(1f, result.bboxCenterX, 0.001f)
        assertEquals(1f, result.bboxCenterY, 0.001f)
        assertEquals(1f, result.bboxWidth, 0.001f)
        assertEquals(1f, result.bboxHeight, 0.001f)
    }

    // endregion

    // region overallScore 加权公式测试

    @Test
    fun `overallScore 按加权公式正确计算 - 典型值`() {
        val result = createDefaultResult(
            confidence = 0.9f,
            faceCoverage = 0.5f,
            ruleOfThirdsScore = 0.8f,
            safetyMarginScore = 1.0f
        )
        // 公式: confidence*0.4 + faceCoverage*0.3 + ruleOfThirdsScore*0.2 + safetyMarginScore*0.1
        val expected = 0.9f * 0.4f + 0.5f * 0.3f + 0.8f * 0.2f + 1.0f * 0.1f
        assertEquals(expected, result.overallScore, 0.001f)
    }

    @Test
    fun `overallScore 所有权重之和为一`() {
        // 验证权重 0.4 + 0.3 + 0.2 + 0.1 = 1.0
        val result = createDefaultResult(
            confidence = 1f,
            faceCoverage = 1f,
            ruleOfThirdsScore = 1f,
            safetyMarginScore = 1f
        )
        assertEquals(1.0f, result.overallScore, 0.001f)
    }

    @Test
    fun `overallScore 所有参数为零时结果为零`() {
        val result = createDefaultResult(
            confidence = 0f,
            faceCoverage = 0f,
            ruleOfThirdsScore = 0f,
            safetyMarginScore = 0f
        )
        assertEquals(0f, result.overallScore, 0.001f)
    }

    @Test
    fun `overallScore 仅 confidence 为一时结果为 0_4`() {
        val result = createDefaultResult(
            confidence = 1f,
            faceCoverage = 0f,
            ruleOfThirdsScore = 0f,
            safetyMarginScore = 0f
        )
        assertEquals(0.4f, result.overallScore, 0.001f)
    }

    @Test
    fun `overallScore 仅 faceCoverage 为一时结果为 0_3`() {
        val result = createDefaultResult(
            confidence = 0f,
            faceCoverage = 1f,
            ruleOfThirdsScore = 0f,
            safetyMarginScore = 0f
        )
        assertEquals(0.3f, result.overallScore, 0.001f)
    }

    @Test
    fun `overallScore 仅 ruleOfThirdsScore 为一时结果为 0_2`() {
        val result = createDefaultResult(
            confidence = 0f,
            faceCoverage = 0f,
            ruleOfThirdsScore = 1f,
            safetyMarginScore = 0f
        )
        assertEquals(0.2f, result.overallScore, 0.001f)
    }

    @Test
    fun `overallScore 仅 safetyMarginScore 为一时结果为 0_1`() {
        val result = createDefaultResult(
            confidence = 0f,
            faceCoverage = 0f,
            ruleOfThirdsScore = 0f,
            safetyMarginScore = 1f
        )
        assertEquals(0.1f, result.overallScore, 0.001f)
    }

    @Test
    fun `overallScore 使用默认参数值时正确计算`() {
        // 默认值: confidence=0.5, faceCoverage=0, ruleOfThirdsScore=0, safetyMarginScore=1
        val result = createDefaultResult()
        val expected = 0.5f * 0.4f + 0f * 0.3f + 0f * 0.2f + 1f * 0.1f
        assertEquals(expected, result.overallScore, 0.001f)
    }

    @Test
    fun `overallScore 混合小数值时精确计算`() {
        val result = createDefaultResult(
            confidence = 0.33f,
            faceCoverage = 0.67f,
            ruleOfThirdsScore = 0.42f,
            safetyMarginScore = 0.55f
        )
        val expected = 0.33f * 0.4f + 0.67f * 0.3f + 0.42f * 0.2f + 0.55f * 0.1f
        assertEquals(expected, result.overallScore, 0.001f)
    }

    // endregion

    // region ActionType 枚举完整性测试

    @Test
    fun `ActionType 包含7种枚举值`() {
        assertEquals(7, CompositionResult.ActionType.values().size)
    }

    @Test
    fun `ActionType 包含所有预期的动作类型`() {
        val actions = CompositionResult.ActionType.values().toSet()
        assertTrue(CompositionResult.ActionType.LEFT in actions)
        assertTrue(CompositionResult.ActionType.RIGHT in actions)
        assertTrue(CompositionResult.ActionType.UP in actions)
        assertTrue(CompositionResult.ActionType.DOWN in actions)
        assertTrue(CompositionResult.ActionType.ZOOM_IN in actions)
        assertTrue(CompositionResult.ActionType.ZOOM_OUT in actions)
        assertTrue(CompositionResult.ActionType.STOP in actions)
    }

    @Test
    fun `ActionType 枚举值按声明顺序排列`() {
        val values = CompositionResult.ActionType.values()
        assertEquals(CompositionResult.ActionType.LEFT, values[0])
        assertEquals(CompositionResult.ActionType.RIGHT, values[1])
        assertEquals(CompositionResult.ActionType.UP, values[2])
        assertEquals(CompositionResult.ActionType.DOWN, values[3])
        assertEquals(CompositionResult.ActionType.ZOOM_IN, values[4])
        assertEquals(CompositionResult.ActionType.ZOOM_OUT, values[5])
        assertEquals(CompositionResult.ActionType.STOP, values[6])
    }

    @Test
    fun `ActionType 可通过 valueOf 按名称获取`() {
        assertEquals(
            CompositionResult.ActionType.LEFT,
            CompositionResult.ActionType.valueOf("LEFT")
        )
        assertEquals(
            CompositionResult.ActionType.ZOOM_IN,
            CompositionResult.ActionType.valueOf("ZOOM_IN")
        )
        assertEquals(
            CompositionResult.ActionType.STOP,
            CompositionResult.ActionType.valueOf("STOP")
        )
    }

    // endregion

    // region equals() 方法测试

    @Test
    fun `equals 同一对象引用返回 true`() {
        val result = createDefaultResult()
        // 同一引用比较
        assertTrue(result.equals(result))
    }

    @Test
    fun `equals 相同内容的不同对象返回 true`() {
        val result1 = createDefaultResult()
        val result2 = createDefaultResult()
        assertTrue(result1.equals(result2))
    }

    @Test
    fun `equals 不同 bbox 返回 false`() {
        val result1 = createDefaultResult(bbox = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f))
        val result2 = createDefaultResult(bbox = floatArrayOf(0.9f, 0.8f, 0.7f, 0.6f))
        assertFalse(result1.equals(result2))
    }

    @Test
    fun `equals bbox 仅一个元素不同返回 false`() {
        val result1 = createDefaultResult(bbox = floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f))
        val result2 = createDefaultResult(bbox = floatArrayOf(0.5f, 0.5f, 0.4f, 0.7f))
        assertFalse(result1.equals(result2))
    }

    @Test
    fun `equals 不同 action 返回 false`() {
        val result1 = createDefaultResult(action = CompositionResult.ActionType.LEFT)
        val result2 = createDefaultResult(action = CompositionResult.ActionType.RIGHT)
        assertFalse(result1.equals(result2))
    }

    @Test
    fun `equals 不同 actionProbabilities 返回 false`() {
        val probs1 = floatArrayOf(0.7f, 0.1f, 0.05f, 0.05f, 0.05f, 0.025f, 0.025f)
        val probs2 = floatArrayOf(0.1f, 0.7f, 0.05f, 0.05f, 0.05f, 0.025f, 0.025f)
        val result1 = createDefaultResult(actionProbabilities = probs1)
        val result2 = createDefaultResult(actionProbabilities = probs2)
        assertFalse(result1.equals(result2))
    }

    @Test
    fun `equals actionProbabilities 仅一个元素不同返回 false`() {
        val probs1 = floatArrayOf(0.7f, 0.1f, 0.05f, 0.05f, 0.05f, 0.025f, 0.025f)
        val probs2 = floatArrayOf(0.7f, 0.1f, 0.05f, 0.05f, 0.05f, 0.025f, 0.026f)
        val result1 = createDefaultResult(actionProbabilities = probs1)
        val result2 = createDefaultResult(actionProbabilities = probs2)
        assertFalse(result1.equals(result2))
    }

    @Test
    fun `equals 不同 confidence 仍返回 true - confidence 不参与 equals 判断`() {
        // equals 方法只比较 bbox、action、actionProbabilities
        // confidence 等字段有默认值但不参与 equals
        val result1 = createDefaultResult(confidence = 0.1f)
        val result2 = createDefaultResult(confidence = 0.9f)
        assertTrue(result1.equals(result2))
    }

    @Test
    fun `equals 不同 faceCoverage 仍返回 true - faceCoverage 不参与 equals 判断`() {
        val result1 = createDefaultResult(faceCoverage = 0f)
        val result2 = createDefaultResult(faceCoverage = 1f)
        assertTrue(result1.equals(result2))
    }

    @Test
    fun `equals 不同 ruleOfThirdsScore 仍返回 true - 不参与 equals 判断`() {
        val result1 = createDefaultResult(ruleOfThirdsScore = 0f)
        val result2 = createDefaultResult(ruleOfThirdsScore = 1f)
        assertTrue(result1.equals(result2))
    }

    @Test
    fun `equals 不同 safetyMarginScore 仍返回 true - 不参与 equals 判断`() {
        val result1 = createDefaultResult(safetyMarginScore = 0f)
        val result2 = createDefaultResult(safetyMarginScore = 1f)
        assertTrue(result1.equals(result2))
    }

    @Test
    fun `equals 传入 null 返回 false`() {
        val result = createDefaultResult()
        assertFalse(result.equals(null))
    }

    @Test
    fun `equals 传入不同类型的对象返回 false`() {
        val result = createDefaultResult()
        assertFalse(result.equals("一个字符串"))
        assertFalse(result.equals(42))
        assertFalse(result.equals<Any>(object {}))
    }

    @Test
    fun `equals 使用 == 操作符语义一致`() {
        val result1 = createDefaultResult()
        val result2 = createDefaultResult()
        // Kotlin 的 == 会调用 equals()
        assertTrue(result1 == result2)
    }

    // endregion

    // region hashCode() 一致性测试

    @Test
    fun `hashCode 相同内容的不同对象返回相同值`() {
        val result1 = createDefaultResult()
        val result2 = createDefaultResult()
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `hashCode 同一对象多次调用返回一致值`() {
        val result = createDefaultResult()
        val hash1 = result.hashCode()
        val hash2 = result.hashCode()
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hashCode 不同 bbox 返回不同值`() {
        val result1 = createDefaultResult(bbox = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f))
        val result2 = createDefaultResult(bbox = floatArrayOf(0.9f, 0.8f, 0.7f, 0.6f))
        // 虽然不强制不同，但通常应不同
        assertNotEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `hashCode 不同 action 返回不同值`() {
        val result1 = createDefaultResult(action = CompositionResult.ActionType.LEFT)
        val result2 = createDefaultResult(action = CompositionResult.ActionType.RIGHT)
        assertNotEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `hashCode 不同 actionProbabilities 返回不同值`() {
        val probs1 = floatArrayOf(0.7f, 0.1f, 0.05f, 0.05f, 0.05f, 0.025f, 0.025f)
        val probs2 = floatArrayOf(0.1f, 0.7f, 0.05f, 0.05f, 0.05f, 0.025f, 0.025f)
        val result1 = createDefaultResult(actionProbabilities = probs1)
        val result2 = createDefaultResult(actionProbabilities = probs2)
        assertNotEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `hashCode 不受 confidence 等非 equals 字段影响`() {
        // confidence 不参与 equals，也不参与 hashCode
        val result1 = createDefaultResult(confidence = 0.1f)
        val result2 = createDefaultResult(confidence = 0.9f)
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `equals 相等的对象 hashCode 必须相同 - hashCode 契约`() {
        val result1 = createDefaultResult()
        val result2 = createDefaultResult()
        // Java 规范：equals 为 true 则 hashCode 必须相同
        assertTrue(result1.equals(result2))
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    // endregion

    // region 默认值测试

    @Test
    fun `confidence 默认值为 0_5`() {
        val result = CompositionResult(
            bbox = floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f),
            action = CompositionResult.ActionType.STOP,
            actionProbabilities = FloatArray(7) { 1f / 7f }
        )
        assertEquals(0.5f, result.confidence, 0.001f)
    }

    @Test
    fun `faceCoverage 默认值为 0`() {
        val result = CompositionResult(
            bbox = floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f),
            action = CompositionResult.ActionType.STOP,
            actionProbabilities = FloatArray(7) { 1f / 7f }
        )
        assertEquals(0f, result.faceCoverage, 0.001f)
    }

    @Test
    fun `ruleOfThirdsScore 默认值为 0`() {
        val result = CompositionResult(
            bbox = floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f),
            action = CompositionResult.ActionType.STOP,
            actionProbabilities = FloatArray(7) { 1f / 7f }
        )
        assertEquals(0f, result.ruleOfThirdsScore, 0.001f)
    }

    @Test
    fun `safetyMarginScore 默认值为 1`() {
        val result = CompositionResult(
            bbox = floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f),
            action = CompositionResult.ActionType.STOP,
            actionProbabilities = FloatArray(7) { 1f / 7f }
        )
        assertEquals(1f, result.safetyMarginScore, 0.001f)
    }

    // endregion

    // region copy 和 data class 行为测试

    @Test
    fun `copy 方法可修改单个字段`() {
        val original = createDefaultResult()
        val copied = original.copy(action = CompositionResult.ActionType.ZOOM_IN)
        assertEquals(CompositionResult.ActionType.ZOOM_IN, copied.action)
        // 其他字段保持不变
        assertTrue(original.bbox.contentEquals(copied.bbox))
        assertTrue(original.actionProbabilities.contentEquals(copied.actionProbabilities))
        assertEquals(original.confidence, copied.confidence, 0.001f)
    }

    @Test
    fun `copy 方法修改 bbox 后新旧对象互不影响`() {
        val original = createDefaultResult()
        val newBbox = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val copied = original.copy(bbox = newBbox)
        // 验证新对象的 bbox 是新值
        assertTrue(newBbox.contentEquals(copied.bbox))
        // 验证原对象未受影响
        assertTrue(floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f).contentEquals(original.bbox))
    }

    @Test
    fun `copy 方法修改 confidence 后 equals 仍为 true`() {
        // confidence 不参与 equals
        val original = createDefaultResult()
        val copied = original.copy(confidence = 0.99f)
        assertTrue(original.equals(copied))
    }

    @Test
    fun `copy 不传参数时产生相等对象`() {
        val original = createDefaultResult()
        val copied = original.copy()
        assertTrue(original.equals(copied))
        assertEquals(original.hashCode(), copied.hashCode())
    }

    @Test
    fun `copy 修改 action 后 equals 返回 false`() {
        val original = createDefaultResult()
        val copied = original.copy(action = CompositionResult.ActionType.DOWN)
        assertFalse(original.equals(copied))
    }

    @Test
    fun `data class component1 到 component4 提取 bbox 元素`() {
        val result = createDefaultResult(bbox = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f))
        // data class 自动生成 componentN 方法，但 bbox 是 FloatArray 类型
        // component1 是 bbox 本身
        val bboxValue = result.component1()
        assertTrue(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f).contentEquals(bboxValue))
    }

    @Test
    fun `data class toString 包含类名`() {
        val result = createDefaultResult()
        val str = result.toString()
        assertTrue(str.contains("CompositionResult"))
    }

    @Test
    fun `data class component 方法按声明顺序访问属性`() {
        val result = createDefaultResult(
            action = CompositionResult.ActionType.LEFT,
            confidence = 0.8f
        )
        // component1 = bbox, component2 = action, component3 = actionProbabilities
        // component4 = confidence, component5 = faceCoverage, component6 = ruleOfThirdsScore
        // component7 = safetyMarginScore
        assertTrue(result.component1().contentEquals(result.bbox))
        assertEquals(result.component2(), result.action)
        assertTrue(result.component3().contentEquals(result.actionProbabilities))
        assertEquals(result.component4(), result.confidence, 0.001f)
        assertEquals(result.component5(), result.faceCoverage, 0.001f)
        assertEquals(result.component6(), result.ruleOfThirdsScore, 0.001f)
        assertEquals(result.component7(), result.safetyMarginScore, 0.001f)
    }

    // endregion

    // region 整合与边界场景测试

    @Test
    fun `bbox 属性与 overallScore 联合使用`() {
        val result = CompositionResult(
            bbox = floatArrayOf(0.33f, 0.66f, 0.2f, 0.5f),
            action = CompositionResult.ActionType.UP,
            actionProbabilities = FloatArray(7) { 0f }.also { it[2] = 1f },
            confidence = 0.7f,
            faceCoverage = 0.4f,
            ruleOfThirdsScore = 0.6f,
            safetyMarginScore = 0.9f
        )
        // bbox 属性正确
        assertEquals(0.33f, result.bboxCenterX, 0.001f)
        assertEquals(0.66f, result.bboxCenterY, 0.001f)
        assertEquals(0.2f, result.bboxWidth, 0.001f)
        assertEquals(0.5f, result.bboxHeight, 0.001f)
        // overallScore 正确
        val expected = 0.7f * 0.4f + 0.4f * 0.3f + 0.6f * 0.2f + 0.9f * 0.1f
        assertEquals(expected, result.overallScore, 0.001f)
    }

    @Test
    fun `不同 action 类型均可创建有效结果`() {
        for (action in CompositionResult.ActionType.values()) {
            val result = createDefaultResult(action = action)
            assertEquals(action, result.action)
        }
    }

    @Test
    fun `FloatArray 内容相同但引用不同的 bbox 仍 equals 为 true`() {
        val bbox1 = floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f)
        val bbox2 = floatArrayOf(0.5f, 0.5f, 0.4f, 0.6f)
        // 不是同一个数组对象
        assertFalse(bbox1 === bbox2)
        val result1 = createDefaultResult(bbox = bbox1)
        val result2 = createDefaultResult(bbox = bbox2)
        assertTrue(result1.equals(result2))
    }

    @Test
    fun `FloatArray 内容相同但引用不同的 actionProbabilities 仍 equals 为 true`() {
        val probs1 = floatArrayOf(0.7f, 0.1f, 0.05f, 0.05f, 0.05f, 0.025f, 0.025f)
        val probs2 = floatArrayOf(0.7f, 0.1f, 0.05f, 0.05f, 0.05f, 0.025f, 0.025f)
        assertFalse(probs1 === probs2)
        val result1 = createDefaultResult(actionProbabilities = probs1)
        val result2 = createDefaultResult(actionProbabilities = probs2)
        assertTrue(result1.equals(result2))
    }

    // endregion
}
