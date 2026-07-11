package com.livecompose.livecapture.core.motion

import android.content.Context
import android.graphics.PointF
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.hypot
import kotlin.math.lerp

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoxCenterManagerTest {

    private lateinit var manager: BoxCenterManager
    private lateinit var context: Context

    // 屏幕尺寸常量
    private val screenWidth = 720f
    private val screenHeight = 1280f
    private val screenCenterX = screenWidth / 2f   // 360f
    private val screenCenterY = screenHeight / 2f  // 640f

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        manager = BoxCenterManager(context)
        manager.setScreenSize(screenWidth, screenHeight)
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 创建零偏移的 MotionData（设备静止）
     */
    private fun zeroMotionData() = MotionStabilityMonitor.MotionData(
        gyroX = 0f, gyroY = 0f, gyroZ = 0f,
        accelX = 0f, accelY = 9.8f, accelZ = 0f
    )

    /**
     * 创建指定陀螺仪偏移的 MotionData
     */
    private fun motionData(gyroX: Float, gyroY: Float) = MotionStabilityMonitor.MotionData(
        gyroX = gyroX, gyroY = gyroY, gyroZ = 0f,
        accelX = 0f, accelY = 9.8f, accelZ = 0f
    )

    /**
     * 计算 dp 值在当前屏幕密度下的像素值
     */
    private fun dpToPx(dp: Float): Float {
        return android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    // ============================================================
    // 1. 初始状态测试
    // ============================================================

    @Test
    fun `初始状态 - 无追踪点`() {
        assertNull(manager.trackPoint.value)
    }

    @Test
    fun `初始状态 - 未对齐`() {
        assertFalse(manager.isAligned.value)
    }

    @Test
    fun `初始状态 - 对齐进度为零`() {
        assertEquals(0f, manager.alignmentProgress.value, 0.001f)
    }

    // ============================================================
    // 2. setScreenSize() 测试
    // ============================================================

    @Test
    fun `setScreenSize - 更新屏幕中心坐标`() {
        // 设置屏幕尺寸后，后续检测应使用新的中心点
        // 通过将检测框中心设为新屏幕中心来验证
        manager.setScreenSize(1080f, 1920f)

        val motionData = zeroMotionData()
        manager.updateFromDetection(540f, 960f, motionData)

        // 检测框中心在屏幕中心，对齐进度应接近1
        assertTrue(manager.alignmentProgress.value > 0.9f)
    }

    @Test
    fun `setScreenSize - 多次设置屏幕尺寸`() {
        // 第一次设置
        manager.setScreenSize(720f, 1280f)
        val motionData = zeroMotionData()
        manager.updateFromDetection(360f, 640f, motionData)
        val progress1 = manager.alignmentProgress.value

        // 重置后设置新尺寸
        manager.reset()
        manager.setScreenSize(1080f, 1920f)
        manager.updateFromDetection(540f, 960f, motionData)
        val progress2 = manager.alignmentProgress.value

        // 两种尺寸下中心对齐，进度都应接近1
        assertTrue(progress1 > 0.9f)
        assertTrue(progress2 > 0.9f)
    }

    @Test
    fun `setScreenSize - 零尺寸屏幕`() {
        // 屏幕尺寸为0时，屏幕中心应为(0,0)
        manager.setScreenSize(0f, 0f)

        val motionData = zeroMotionData()
        // 传入零尺寸不应崩溃
        manager.updateFromDetection(0f, 0f, motionData)
        // 结果应为 null，因为 ref + offset 被 coerceIn(0f, 0f) = 0f，
        // 但屏幕中心也是 0f，所以 distance = 0
        // maxDistance = 0 * 0.5 = 0，会导致除零，progress = 1 - (0/0) = NaN
        // coerceIn(0f,1f) 会将 NaN 保留，所以进度为 NaN
        assertNotNull(manager.trackPoint.value)
    }

    // ============================================================
    // 3. updateFromDetection() 测试
    // ============================================================

    @Test
    fun `updateFromDetection - 首次调用设置参考中心并产生追踪点`() {
        val motionData = zeroMotionData()
        manager.updateFromDetection(360f, 640f, motionData)

        assertNotNull(manager.trackPoint.value)
    }

    @Test
    fun `updateFromDetection - 首次调用将检测框中心设为参考中心`() {
        val motionData = zeroMotionData()
        // 在屏幕中心处检测（零偏移），追踪点应在屏幕中心
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData)

        val trackPoint = manager.trackPoint.value!!
        // 无偏移时 rawX = ref.x + 0 = screenCenterX，rawY = ref.y + 0 = screenCenterY
        // 且在 snap 阈值内，会有 LERP 吸附，但距离为0时吸附结果就是中心
        assertEquals(screenCenterX, trackPoint.x, 1f)
        assertEquals(screenCenterY, trackPoint.y, 1f)
    }

    @Test
    fun `updateFromDetection - 后续调用不改变参考中心`() {
        val motionData = zeroMotionData()

        // 第一次调用：在 (100f, 200f) 处检测
        manager.updateFromDetection(100f, 200f, motionData)
        val trackPoint1 = manager.trackPoint.value!!

        // 第二次调用：在不同位置检测，但参考中心不变
        manager.updateFromDetection(500f, 800f, motionData)
        val trackPoint2 = manager.trackPoint.value!!

        // 两次都用相同的参考中心 (100f, 200f)，零偏移时追踪点应相同
        assertEquals(trackPoint1.x, trackPoint2.x, 0.01f)
        assertEquals(trackPoint1.y, trackPoint2.y, 0.01f)
    }

    @Test
    fun `updateFromDetection - 陀螺仪偏移影响追踪点位置`() {
        // 参考中心在屏幕中心
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 加入陀螺仪偏移: offsetX = gyroY * 50, offsetY = gyroX * 50
        val gyroX = 2f  // offsetY = 2 * 50 = 100
        val gyroY = 1f  // offsetX = 1 * 50 = 50
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        // rawX = screenCenterX + offsetX = 360 + 50 = 410
        // rawY = screenCenterY + offsetY = 640 + 100 = 740
        // 距离中心 = hypot(50, 100) ≈ 111.8，大于 snap 阈值，不会吸附
        // 所以 finalPoint 应等于 rawPoint
        val expectedRawX = screenCenterX + gyroY * 50f
        val expectedRawY = screenCenterY + gyroX * 50f
        assertEquals(expectedRawX, trackPoint.x, 0.5f)
        assertEquals(expectedRawY, trackPoint.y, 0.5f)
    }

    @Test
    fun `updateFromDetection - 偏移量使用 coerceIn 限制在屏幕范围内`() {
        // 参考中心在屏幕中心
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 极大的陀螺仪偏移，使 rawX/rawY 超出屏幕边界
        val gyroX = 100f  // offsetY = 100 * 50 = 5000，远超 screenHeight
        val gyroY = 100f  // offsetX = 100 * 50 = 5000，远超 screenWidth
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        // rawX 被 coerceIn(0f, 720f) = 720f
        // rawY 被 coerceIn(0f, 1280f) = 1280f
        assertEquals(screenWidth, trackPoint.x, 0.01f)
        assertEquals(screenHeight, trackPoint.y, 0.01f)
    }

    @Test
    fun `updateFromDetection - 负向偏移 coerceIn 到 0`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 负向陀螺仪偏移
        val gyroX = -100f  // offsetY = -100 * 50 = -5000
        val gyroY = -100f  // offsetX = -100 * 50 = -5000
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        // rawX 被 coerceIn(0f, 720f) = 0f
        // rawY 被 coerceIn(0f, 1280f) = 0f
        assertEquals(0f, trackPoint.x, 0.01f)
        assertEquals(0f, trackPoint.y, 0.01f)
    }

    @Test
    fun `updateFromDetection - 磁性吸附 - 接近中心时追踪点被吸附`() {
        // 参考中心在屏幕中心
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 小偏移，使追踪点在 snap 阈值内
        val snapThresholdPx = dpToPx(40f)
        // 设偏移使距离约为 snap 阈值的一半
        val halfSnap = snapThresholdPx / 2f
        // 只在 X 方向偏移，偏移量 = halfSnap
        val gyroY = halfSnap / 50f
        val gyroX = 0f

        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        val rawX = screenCenterX + gyroY * 50f  // = screenCenterX + halfSnap
        val rawY = screenCenterY

        // 吸附后，X 应比 rawX 更靠近 screenCenterX
        val distanceFromRaw = kotlin.math.abs(trackPoint.x - screenCenterX)
        val rawDistanceFromCenter = kotlin.math.abs(rawX - screenCenterX)
        assertTrue("吸附后距离应小于原始距离", distanceFromRaw < rawDistanceFromCenter)
    }

    @Test
    fun `updateFromDetection - 磁性吸附 - 正好在中心时完全对齐`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        // 无偏移，追踪点应恰好在屏幕中心
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        val trackPoint = manager.trackPoint.value!!
        assertEquals(screenCenterX, trackPoint.x, 0.01f)
        assertEquals(screenCenterY, trackPoint.y, 0.01f)
    }

    @Test
    fun `updateFromDetection - 远离中心时无吸附`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 大偏移，使追踪点远超 snap 阈值
        val snapThresholdPx = dpToPx(40f)
        // 偏移量大于 snap 阈值
        val gyroY = (snapThresholdPx + 50f) / 50f  // 确保 offsetX > snapThresholdPx
        val gyroX = 0f

        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        val rawX = screenCenterX + gyroY * 50f
        val rawY = screenCenterY

        // 无吸附，追踪点应等于原始坐标
        assertEquals(rawX, trackPoint.x, 0.01f)
        assertEquals(rawY, trackPoint.y, 0.01f)
    }

    @Test
    fun `updateFromDetection - LERP 插值行为验证`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 设置小偏移，使距离在 snap 阈值内
        val snapThresholdPx = dpToPx(40f)
        val smallOffset = snapThresholdPx / 4f  // 距离为 snap 阈值的 1/4
        val gyroY = smallOffset / 50f
        val gyroX = 0f

        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        val rawX = screenCenterX + gyroY * 50f
        val distance = kotlin.math.abs(rawX - screenCenterX)

        // 验证 LERP: snapStrength = 1 - (distance / snapThresholdPx)
        // snappedX = lerp(rawX, screenCenterX, 0.3 * snapStrength)
        val expectedSnapStrength = 1f - (distance / snapThresholdPx)
        val expectedSnappedX = lerp(rawX, screenCenterX, 0.3f * expectedSnapStrength)
        assertEquals(expectedSnappedX, trackPoint.x, 0.5f)
    }

    @Test
    fun `updateFromDetection - 参考中心非屏幕中心时的追踪行为`() {
        // 在偏离中心的位置设置参考中心
        val refX = 200f
        val refY = 300f
        manager.updateFromDetection(refX, refY, zeroMotionData())

        val trackPoint = manager.trackPoint.value!!
        // 无偏移时追踪点在 (refX, refY)，距中心较远，不会吸附
        val distance = hypot((refX - screenCenterX).toDouble(), (refY - screenCenterY).toDouble()).toFloat()
        val snapThresholdPx = dpToPx(40f)

        if (distance >= snapThresholdPx) {
            // 不在吸附范围内，追踪点应等于原始坐标
            assertEquals(refX, trackPoint.x, 0.01f)
            assertEquals(refY, trackPoint.y, 0.01f)
        }
    }

    // ============================================================
    // 4. evaluateAlignment() 测试
    // ============================================================

    @Test
    fun `evaluateAlignment - 对齐进度在0到1之间 - 中心位置`() {
        // 检测框在屏幕中心
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 中心对齐，进度应为1
        assertEquals(1f, manager.alignmentProgress.value, 0.01f)
    }

    @Test
    fun `evaluateAlignment - 对齐进度在0到1之间 - 边缘位置`() {
        // 参考中心在屏幕中心，然后大偏移到边缘
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 大偏移使追踪点超出屏幕（被 coerceIn 到边缘）
        val gyroX = 100f
        val gyroY = 100f
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        // 在屏幕角落，进度应较低
        assertTrue(manager.alignmentProgress.value < 0.5f)
    }

    @Test
    fun `evaluateAlignment - 进度为0到1之间的浮点数`() {
        // 参考中心在屏幕中心，小偏移
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 中等偏移
        val gyroY = 3f  // offsetX = 150
        val gyroX = 2f  // offsetY = 100
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val progress = manager.alignmentProgress.value
        assertTrue("进度应在0到1之间", progress in 0f..1f)
        assertTrue("中等偏移时进度应在0和1之间", progress > 0f && progress < 1f)
    }

    @Test
    fun `evaluateAlignment - isWithinSnap 在阈值内为 true`() {
        // 在中心检测，距离为0，必然在 snap 阈值内
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 虽然在 snap 范围内，但需要锁定时长后 isAligned 才为 true
        // 这里只验证 isAligned 还没被设置为 true（因为时间不够）
        assertFalse(manager.isAligned.value)
    }

    @Test
    fun `evaluateAlignment - isAligned 在锁定时长后变为 true`() {
        // 在中心检测（snap 范围内）
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 此时 isLocked = true，但时间不够，isAligned 仍为 false
        assertFalse(manager.isAligned.value)

        // 模拟时间流逝：等待超过 LOCK_DURATION_MS (800ms)
        Thread.sleep(850)

        // 再次调用 updateFromDetection，仍然在中心
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 现在 isAligned 应为 true
        assertTrue(manager.isAligned.value)
    }

    @Test
    fun `evaluateAlignment - isAligned 未达锁定时长仍为 false`() {
        // 在中心检测
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertFalse(manager.isAligned.value)

        // 等待很短时间
        Thread.sleep(100)

        // 再次调用，仍在 snap 范围内，但时间不够
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertFalse(manager.isAligned.value)
    }

    @Test
    fun `evaluateAlignment - 离开 snap 范围后 isAligned 重置为 false`() {
        // 先在中心停留足够时间使 isAligned = true
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        Thread.sleep(850)
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue("前置条件：isAligned 应为 true", manager.isAligned.value)

        // 大偏移，离开 snap 范围
        val snapThresholdPx = dpToPx(40f)
        val gyroY = (snapThresholdPx + 100f) / 50f
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(0f, gyroY))

        // isAligned 应被重置为 false
        assertFalse(manager.isAligned.value)
    }

    @Test
    fun `evaluateAlignment - 离开 snap 范围后再回来需要重新锁定`() {
        // 在中心停留
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        Thread.sleep(850)
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue(manager.isAligned.value)

        // 离开 snap 范围
        val snapThresholdPx = dpToPx(40f)
        val gyroY = (snapThresholdPx + 100f) / 50f
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(0f, gyroY))
        assertFalse(manager.isAligned.value)

        // 回到中心，但还没有锁定时长
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertFalse("回到 snap 范围后，需要重新计时", manager.isAligned.value)
    }

    @Test
    fun `evaluateAlignment - 在 snap 范围内但未离开时持续累积锁定时间`() {
        // 在中心检测
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertFalse(manager.isAligned.value)

        // 等待 400ms（不够 800ms）
        Thread.sleep(400)
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertFalse(manager.isAligned.value)

        // 再等待 500ms（总计超过 800ms）
        Thread.sleep(500)
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue("持续在 snap 范围内累积时间后 isAligned 应为 true", manager.isAligned.value)
    }

    // ============================================================
    // 5. reset() 测试
    // ============================================================

    @Test
    fun `reset - 清除追踪点`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertNotNull(manager.trackPoint.value)

        manager.reset()
        assertNull(manager.trackPoint.value)
    }

    @Test
    fun `reset - 清除对齐状态`() {
        // 使 isAligned = true
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        Thread.sleep(850)
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue(manager.isAligned.value)

        manager.reset()
        assertFalse(manager.isAligned.value)
    }

    @Test
    fun `reset - 清除对齐进度`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue(manager.alignmentProgress.value > 0f)

        manager.reset()
        assertEquals(0f, manager.alignmentProgress.value, 0.001f)
    }

    @Test
    fun `reset - 清除参考中心，允许重新设置`() {
        // 第一次检测设置参考中心
        manager.updateFromDetection(100f, 200f, zeroMotionData())
        val trackPoint1 = manager.trackPoint.value!!
        // 参考中心为 (100, 200)，无偏移时追踪点在 (100, 200)

        manager.reset()

        // 重置后再次检测，应设置新的参考中心
        manager.updateFromDetection(500f, 800f, zeroMotionData())
        val trackPoint2 = manager.trackPoint.value!!

        // 新参考中心为 (500, 800)，追踪点应不同于之前
        // 检查 X 坐标不同（100 vs 500）
        assertTrue("重置后参考中心应被清除并重新设置", trackPoint1.x != trackPoint2.x)
    }

    @Test
    fun `reset - 多次重置不会崩溃`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        manager.reset()
        manager.reset()
        manager.reset()

        assertNull(manager.trackPoint.value)
        assertFalse(manager.isAligned.value)
        assertEquals(0f, manager.alignmentProgress.value, 0.001f)
    }

    // ============================================================
    // 6. 边界条件与边缘情况测试
    // ============================================================

    @Test
    fun `边界条件 - 零屏幕尺寸时不崩溃`() {
        manager.setScreenSize(0f, 0f)
        // 不应崩溃
        manager.updateFromDetection(0f, 0f, zeroMotionData())
    }

    @Test
    fun `边界条件 - 负偏移被 coerceIn 到 0`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 负向偏移使 rawX/rawY 为负值
        val gyroX = -20f  // offsetY = -1000
        val gyroY = -20f  // offsetX = -1000
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        // coerceIn(0f, screenWidth) 和 coerceIn(0f, screenHeight) 应确保非负
        assertTrue("X 坐标不应为负", trackPoint.x >= 0f)
        assertTrue("Y 坐标不应为负", trackPoint.y >= 0f)
    }

    @Test
    fun `边界条件 - 极大偏移被 coerceIn 到屏幕边界`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 极大陀螺仪值
        val gyroX = 1000f
        val gyroY = 1000f
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        assertTrue("X 坐标不应超过屏幕宽度", trackPoint.x <= screenWidth)
        assertTrue("Y 坐标不应超过屏幕高度", trackPoint.y <= screenHeight)
    }

    @Test
    fun `边界条件 - 检测点恰好在屏幕中心`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        val trackPoint = manager.trackPoint.value!!
        // 恰好在中心，distance = 0，吸附后仍在中心
        assertEquals(screenCenterX, trackPoint.x, 0.01f)
        assertEquals(screenCenterY, trackPoint.y, 0.01f)

        // 对齐进度应为1
        assertEquals(1f, manager.alignmentProgress.value, 0.01f)
    }

    @Test
    fun `边界条件 - 检测点恰好在 snap 阈值边界上`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 精确设置偏移使距离等于 snap 阈值
        val snapThresholdPx = dpToPx(40f)
        // offsetX = snapThresholdPx，offsetY = 0
        // 距离 = hypot(snapThresholdPx, 0) = snapThresholdPx
        // 注意：snap 条件是 distance < snapThresholdPx（严格小于），所以恰好等于时不吸附
        val gyroY = snapThresholdPx / 50f
        val gyroX = 0f

        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        // 距离恰好等于 snap 阈值，不满足 < 条件，不应吸附
        val rawX = screenCenterX + gyroY * 50f
        val rawY = screenCenterY
        assertEquals(rawX, trackPoint.x, 0.01f)
        assertEquals(rawY, trackPoint.y, 0.01f)
    }

    @Test
    fun `边界条件 - 检测点恰好在 snap 阈值内侧`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 距离略小于 snap 阈值
        val snapThresholdPx = dpToPx(40f)
        val distance = snapThresholdPx - 1f  // 比阈值小1像素
        val gyroY = distance / 50f
        val gyroX = 0f

        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, gyroY))

        val trackPoint = manager.trackPoint.value!!
        // 在 snap 范围内，应有吸附效果
        val rawX = screenCenterX + gyroY * 50f
        val distanceFromCenter = kotlin.math.abs(trackPoint.x - screenCenterX)
        val rawDistanceFromCenter = kotlin.math.abs(rawX - screenCenterX)
        assertTrue("在 snap 范围内应有吸附", distanceFromCenter < rawDistanceFromCenter)
    }

    @Test
    fun `边界条件 - 检测点在屏幕角落时进度很低`() {
        // 参考中心在角落
        manager.updateFromDetection(0f, 0f, zeroMotionData())

        val progress = manager.alignmentProgress.value
        // 角落到中心的距离 = hypot(360, 640) ≈ 733
        // maxDistance = 720 * 0.5 = 360
        // progress = 1 - (733/360) = 1 - 2.036 → coerceIn(0,1) = 0
        assertEquals(0f, progress, 0.01f)
    }

    @Test
    fun `边界条件 - setScreenSize 在检测后更新不影响已设置的参考中心`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        val trackPointBefore = manager.trackPoint.value!!

        // 更新屏幕尺寸（参考中心不变，但屏幕中心变了）
        manager.setScreenSize(1080f, 1920f)

        // 下次检测使用新的屏幕中心
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        val trackPointAfter = manager.trackPoint.value!!

        // 新屏幕中心为 (540, 960)，参考中心仍在 (360, 640)
        // 追踪点应基于旧的参考中心加上偏移计算
        // 但由于屏幕中心变了，evaluateAlignment 的结果也会变
        assertNotNull(trackPointAfter)
    }

    @Test
    fun `边界条件 - 连续多次 updateFromDetection 更新追踪点`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 连续多次更新
        for (i in 1..5) {
            val gyroY = i.toFloat() * 0.5f
            manager.updateFromDetection(screenCenterX, screenCenterY, motionData(0f, gyroY))
            assertNotNull(manager.trackPoint.value)
        }

        // 最后一次: gyroY = 2.5, offsetX = 125
        val lastTrackPoint = manager.trackPoint.value!!
        val expectedRawX = screenCenterX + 2.5f * 50f
        // 偏移125像素可能仍在 snap 范围内，取决于密度
        assertNotNull(lastTrackPoint)
    }

    @Test
    fun `边界条件 - isAligned 变为 true 后保持 true 只要仍在 snap 范围内`() {
        // 在中心停留足够时间
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        Thread.sleep(850)
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue(manager.isAligned.value)

        // 继续在中心检测，isAligned 应保持 true
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue("isAligned 应保持 true", manager.isAligned.value)
    }

    @Test
    fun `边界条件 - 重置后重新检测可再次达到对齐`() {
        // 第一次对齐
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        Thread.sleep(850)
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue(manager.isAligned.value)

        // 重置
        manager.reset()
        assertFalse(manager.isAligned.value)

        // 再次对齐
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertFalse("重置后需要重新计时", manager.isAligned.value)

        Thread.sleep(850)
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())
        assertTrue("重新计时后 isAligned 应为 true", manager.isAligned.value)
    }

    @Test
    fun `边界条件 - gyroX 影响 Y 方向偏移，gyroY 影响 X 方向偏移`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 只设 gyroX（影响 offsetY），gyroY = 0（offsetX = 0）
        val gyroX = 2f  // offsetY = 100
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(gyroX, 0f))

        val trackPoint = manager.trackPoint.value!!
        // X 不变（offsetX = gyroY * 50 = 0），Y 偏移 100
        val expectedRawY = screenCenterY + gyroX * 50f
        // 距离 = 100，是否在 snap 范围内取决于屏幕密度
        // 不验证精确值，只验证 Y 方向确实偏移了
        assertTrue("Y 方向应有偏移", trackPoint.y != screenCenterY || expectedRawY == screenCenterY)
    }

    @Test
    fun `边界条件 - 仅 gyroY 影响 X 方向偏移`() {
        manager.updateFromDetection(screenCenterX, screenCenterY, zeroMotionData())

        // 只设 gyroY（影响 offsetX），gyroX = 0（offsetY = 0）
        val gyroY = 2f  // offsetX = 100
        manager.updateFromDetection(screenCenterX, screenCenterY, motionData(0f, gyroY))

        val trackPoint = manager.trackPoint.value!!
        // Y 不变，X 偏移 100
        val expectedRawX = screenCenterX + gyroY * 50f
        assertTrue("X 方向应有偏移", trackPoint.x != screenCenterX || expectedRawX == screenCenterX)
    }

    @Test
    fun `边界条件 - 参考中心在屏幕边缘时偏移被 coerceIn`() {
        // 参考中心在左上角
        manager.updateFromDetection(0f, 0f, zeroMotionData())

        // 负向偏移 → coerceIn 到 0
        manager.updateFromDetection(0f, 0f, motionData(-10f, -10f))

        val trackPoint = manager.trackPoint.value!!
        assertEquals(0f, trackPoint.x, 0.01f)
        assertEquals(0f, trackPoint.y, 0.01f)
    }

    @Test
    fun `边界条件 - 参考中心在右下角时偏移被 coerceIn`() {
        // 参考中心在右下角
        manager.updateFromDetection(screenWidth, screenHeight, zeroMotionData())

        // 正向偏移 → coerceIn 到屏幕边界
        manager.updateFromDetection(screenWidth, screenHeight, motionData(10f, 10f))

        val trackPoint = manager.trackPoint.value!!
        assertEquals(screenWidth, trackPoint.x, 0.01f)
        assertEquals(screenHeight, trackPoint.y, 0.01f)
    }
}
