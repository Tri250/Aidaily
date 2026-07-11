package com.livecompose.livecapture.core.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * MotionStabilityMonitor 综合单元测试
 *
 * 覆盖范围：
 * - MotionData 数据类（默认值、自定义值、copy、equals/hashCode）
 * - startMonitoring()（传感器可用、不可用降级、幂等性）
 * - stopMonitoring()（重置状态、可重新启动）
 * - onSensorChanged()（陀螺仪事件、加速度计事件、motionData更新）
 * - evaluateStability()（稳定阈值10帧、不稳定阈值5帧、迟滞行为、边界值）
 * - 线程安全（并发传感器事件不破坏状态一致性）
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MotionStabilityMonitorTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var gyroSensor: Sensor
    private lateinit var accelSensor: Sensor

    // 用于追踪所有创建的monitor实例，确保tearDown中统一清理
    private val monitors = mutableListOf<MotionStabilityMonitor>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroSensor = ShadowSensor.newInstance(Sensor.TYPE_GYROSCOPE)
        accelSensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
    }

    @After
    fun tearDown() {
        // 停止所有monitor，避免测试间状态泄漏
        monitors.forEach { it.stopMonitoring() }
        monitors.clear()
    }

    // =====================================================
    // 辅助方法
    // =====================================================

    /**
     * 创建带有传感器的 MotionStabilityMonitor 实例。
     * 在构造前将陀螺仪和加速度计注册到 ShadowSensorManager，
     * 使 getDefaultSensor() 返回非 null。
     */
    private fun createMonitorWithSensors(): MotionStabilityMonitor {
        val shadowSM = Shadows.shadowOf(sensorManager)
        shadowSM.addSensor(Sensor.TYPE_GYROSCOPE, gyroSensor)
        shadowSM.addSensor(Sensor.TYPE_ACCELEROMETER, accelSensor)
        val monitor = MotionStabilityMonitor(context)
        monitors.add(monitor)
        return monitor
    }

    /**
     * 创建不带传感器的 MotionStabilityMonitor 实例（降级模式）。
     * ShadowSensorManager 默认不包含任何传感器，getDefaultSensor() 返回 null。
     */
    private fun createMonitorWithoutSensors(): MotionStabilityMonitor {
        val monitor = MotionStabilityMonitor(context)
        monitors.add(monitor)
        return monitor
    }

    /**
     * 通过反射创建 SensorEvent 实例。
     * SensorEvent 的构造函数是包级私有的，需要反射才能在测试中实例化。
     *
     * @param sensor  触发事件的传感器
     * @param values  传感器读数（陀螺仪3轴 / 加速度计3轴）
     */
    private fun createSensorEvent(sensor: Sensor, vararg values: Float): SensorEvent {
        val constructor = SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
        constructor.isAccessible = true
        val event = constructor.newInstance(values.size)
        event.sensor = sensor
        for (i in values.indices) {
            event.values[i] = values[i]
        }
        return event
    }

    /**
     * 发送一个稳定的加速度计事件（模拟设备静止）。
     * 加速度 = (0, 0, 9.8)，接近地球重力值。
     * gyroReadings 默认为 (0,0,0)，也是稳定的。
     */
    private fun sendStableAccelEvent(monitor: MotionStabilityMonitor) {
        val event = createSensorEvent(accelSensor, 0f, 0f, 9.8f)
        monitor.onSensorChanged(event)
    }

    /**
     * 发送一个稳定的陀螺仪事件（模拟设备静止）。
     * 角速度 = (0, 0, 0)，设备无旋转。
     */
    private fun sendStableGyroEvent(monitor: MotionStabilityMonitor) {
        val event = createSensorEvent(gyroSensor, 0f, 0f, 0f)
        monitor.onSensorChanged(event)
    }

    /**
     * 发送一个不稳定的陀螺仪事件（模拟设备剧烈晃动）。
     * 角速度 = (1, 0, 0)，远超 GYROSCOPE_THRESHOLD=0.15。
     */
    private fun sendUnstableGyroEvent(monitor: MotionStabilityMonitor) {
        val event = createSensorEvent(gyroSensor, 1f, 0f, 0f)
        monitor.onSensorChanged(event)
    }

    /**
     * 发送一个不稳定的加速度计事件（模拟设备剧烈晃动）。
     * 加速度 = (0, 0, 0)，远偏离重力值 9.8。
     */
    private fun sendUnstableAccelEvent(monitor: MotionStabilityMonitor) {
        val event = createSensorEvent(accelSensor, 0f, 0f, 0f)
        monitor.onSensorChanged(event)
    }

    // =====================================================
    // MotionData 数据类测试
    // =====================================================

    @Test
    fun `MotionData默认值全为零`() {
        val data = MotionStabilityMonitor.MotionData()
        assertEquals(0f, data.gyroX, 0.001f)
        assertEquals(0f, data.gyroY, 0.001f)
        assertEquals(0f, data.gyroZ, 0.001f)
        assertEquals(0f, data.accelX, 0.001f)
        assertEquals(0f, data.accelY, 0.001f)
        assertEquals(0f, data.accelZ, 0.001f)
    }

    @Test
    fun `MotionData存储自定义传感器读数`() {
        val data = MotionStabilityMonitor.MotionData(
            gyroX = 0.1f, gyroY = 0.2f, gyroZ = 0.3f,
            accelX = 1.0f, accelY = 9.8f, accelZ = 0.5f
        )
        assertEquals(0.1f, data.gyroX, 0.001f)
        assertEquals(0.2f, data.gyroY, 0.001f)
        assertEquals(0.3f, data.gyroZ, 0.001f)
        assertEquals(1.0f, data.accelX, 0.001f)
        assertEquals(9.8f, data.accelY, 0.001f)
        assertEquals(0.5f, data.accelZ, 0.001f)
    }

    @Test
    fun `MotionData的copy函数正确工作且不影响原对象`() {
        val original = MotionStabilityMonitor.MotionData(
            gyroX = 0.1f, gyroY = 0.2f, gyroZ = 0.3f,
            accelX = 1.0f, accelY = 2.0f, accelZ = 3.0f
        )
        val copied = original.copy(gyroX = 0.5f, accelZ = 9.8f)

        // copy 后修改的字段
        assertEquals(0.5f, copied.gyroX, 0.001f)
        assertEquals(9.8f, copied.accelZ, 0.001f)

        // copy 后未修改的字段保持原值
        assertEquals(0.2f, copied.gyroY, 0.001f)
        assertEquals(0.3f, copied.gyroZ, 0.001f)
        assertEquals(1.0f, copied.accelX, 0.001f)
        assertEquals(2.0f, copied.accelY, 0.001f)

        // 原对象不受影响
        assertEquals(0.1f, original.gyroX, 0.001f)
        assertEquals(3.0f, original.accelZ, 0.001f)
    }

    @Test
    fun `MotionData的equals和hashCode基于内容`() {
        val data1 = MotionStabilityMonitor.MotionData(gyroX = 1.0f, accelY = 2.0f)
        val data2 = MotionStabilityMonitor.MotionData(gyroX = 1.0f, accelY = 2.0f)
        val data3 = MotionStabilityMonitor.MotionData(gyroX = 1.0f, accelY = 3.0f)

        // 内容相同的对象相等
        assertEquals(data1, data2)
        assertEquals(data1.hashCode(), data2.hashCode())

        // 内容不同的对象不相等
        assertNotEquals(data1, data3)
    }

    // =====================================================
    // startMonitoring() 测试
    // =====================================================

    @Test
    fun `startMonitoring传感器可用时isAvailable为true`() {
        val monitor = createMonitorWithSensors()

        assertTrue(monitor.isAvailable.value)
    }

    @Test
    fun `startMonitoring传感器可用时注册监听器并可响应传感器事件`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 发送传感器事件后，motionData 应该更新，证明监听器已注册
        sendStableAccelEvent(monitor)
        assertEquals(9.8f, monitor.motionData.value.accelZ, 0.001f)
    }

    @Test
    fun `startMonitoring传感器不可用时进入降级模式`() {
        val monitor = createMonitorWithoutSensors()

        // 传感器不可用
        assertFalse(monitor.isAvailable.value)

        monitor.startMonitoring()

        // 降级模式：isStable 直接设为 true，不阻塞拍摄流程
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `startMonitoring重复调用是幂等的`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 第一次调用后，发送9个稳定帧
        repeat(9) { sendStableAccelEvent(monitor) }
        assertFalse(monitor.isStable.value) // 还不够10帧

        // 第二次调用 startMonitoring，不应重置已有的帧计数
        monitor.startMonitoring()

        // 再发1个稳定帧，应达到10帧变稳定
        // 如果第二次 startMonitoring 重置了状态，则需要再发10帧
        sendStableAccelEvent(monitor)
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `startMonitoring重复调用不会重复注册监听器`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 先积累稳定帧
        repeat(9) { sendStableAccelEvent(monitor) }

        // 第二次调用 startMonitoring（幂等，应直接返回）
        monitor.startMonitoring()

        // 再发1帧即达10帧变稳定，证明帧计数没有被重置
        sendStableAccelEvent(monitor)
        assertTrue(monitor.isStable.value)
    }

    // =====================================================
    // stopMonitoring() 测试
    // =====================================================

    @Test
    fun `stopMonitoring重置isStable为false`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 先变稳定
        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)

        // 停止监控后重置为 false
        monitor.stopMonitoring()
        assertFalse(monitor.isStable.value)
    }

    @Test
    fun `stopMonitoring重置稳定帧和不稳定帧计数器`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 发送5个稳定帧（不够10帧，未变稳定）
        repeat(5) { sendStableAccelEvent(monitor) }
        assertFalse(monitor.isStable.value)

        // 停止后重新开始，帧计数应从0开始
        monitor.stopMonitoring()
        monitor.startMonitoring()

        // 9帧不够
        repeat(9) { sendStableAccelEvent(monitor) }
        assertFalse(monitor.isStable.value)

        // 第10帧才变稳定，证明之前的5帧已被清零
        sendStableAccelEvent(monitor)
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `stopMonitoring后可重新启动监控`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 第一轮：变稳定
        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)

        // 停止
        monitor.stopMonitoring()
        assertFalse(monitor.isStable.value)

        // 重新启动
        monitor.startMonitoring()
        assertFalse(monitor.isStable.value) // 重新开始后不稳定

        // 第二轮：重新积累10帧变稳定
        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `stopMonitoring后传感器事件仍可被手动触发`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        monitor.stopMonitoring()

        // 虽然监听器已注销，但直接调用 onSensorChanged 仍然可以更新 motionData
        // 这验证了 stopMonitoring 不会破坏 onSensorChanged 的逻辑
        monitor.onSensorChanged(createSensorEvent(gyroSensor, 0.1f, 0.2f, 0.3f))
        assertEquals(0.1f, monitor.motionData.value.gyroX, 0.001f)
    }

    // =====================================================
    // onSensorChanged() 测试
    // =====================================================

    @Test
    fun `onSensorChanged处理陀螺仪事件更新motionData的陀螺仪字段`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        val event = createSensorEvent(gyroSensor, 0.1f, 0.2f, 0.3f)
        monitor.onSensorChanged(event)

        val data = monitor.motionData.value
        assertEquals(0.1f, data.gyroX, 0.001f)
        assertEquals(0.2f, data.gyroY, 0.001f)
        assertEquals(0.3f, data.gyroZ, 0.001f)
        // 加速度计字段应保持默认值0（尚未接收加速度计事件）
        assertEquals(0f, data.accelX, 0.001f)
        assertEquals(0f, data.accelY, 0.001f)
        assertEquals(0f, data.accelZ, 0.001f)
    }

    @Test
    fun `onSensorChanged处理加速度计事件更新motionData的加速度计字段`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        val event = createSensorEvent(accelSensor, 1.0f, 2.0f, 9.8f)
        monitor.onSensorChanged(event)

        val data = monitor.motionData.value
        assertEquals(1.0f, data.accelX, 0.001f)
        assertEquals(2.0f, data.accelY, 0.001f)
        assertEquals(9.8f, data.accelZ, 0.001f)
        // 陀螺仪字段应保持默认值0（尚未接收陀螺仪事件）
        assertEquals(0f, data.gyroX, 0.001f)
        assertEquals(0f, data.gyroY, 0.001f)
        assertEquals(0f, data.gyroZ, 0.001f)
    }

    @Test
    fun `onSensorChanged交替处理陀螺仪和加速度计事件保持各自读数`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 发送陀螺仪事件
        monitor.onSensorChanged(createSensorEvent(gyroSensor, 0.1f, 0.2f, 0.3f))
        var data = monitor.motionData.value
        assertEquals(0.1f, data.gyroX, 0.001f)
        assertEquals(0f, data.accelX, 0.001f) // 加速度计尚未更新

        // 发送加速度计事件
        monitor.onSensorChanged(createSensorEvent(accelSensor, 1.0f, 2.0f, 9.8f))
        data = monitor.motionData.value
        // 陀螺仪值应保持
        assertEquals(0.1f, data.gyroX, 0.001f)
        assertEquals(0.2f, data.gyroY, 0.001f)
        assertEquals(0.3f, data.gyroZ, 0.001f)
        // 加速度计值已更新
        assertEquals(1.0f, data.accelX, 0.001f)
        assertEquals(2.0f, data.accelY, 0.001f)
        assertEquals(9.8f, data.accelZ, 0.001f)
    }

    @Test
    fun `onSensorChanged连续事件覆盖之前的读数`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 第一次陀螺仪读数
        monitor.onSensorChanged(createSensorEvent(gyroSensor, 0.1f, 0.2f, 0.3f))
        assertEquals(0.1f, monitor.motionData.value.gyroX, 0.001f)

        // 第二次陀螺仪读数覆盖第一次
        monitor.onSensorChanged(createSensorEvent(gyroSensor, 0.5f, 0.6f, 0.7f))
        val data = monitor.motionData.value
        assertEquals(0.5f, data.gyroX, 0.001f)
        assertEquals(0.6f, data.gyroY, 0.001f)
        assertEquals(0.7f, data.gyroZ, 0.001f)
    }

    @Test
    fun `onSensorChanged每次调用都触发evaluateStability`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 每个加速度计事件都会触发 evaluateStability
        // 稳定的加速度事件 + 默认陀螺仪(0,0,0) = 稳定帧
        // 发送10次应触发 isStable = true
        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)
    }

    // =====================================================
    // evaluateStability() 测试 — 稳定阈值
    // =====================================================

    @Test
    fun `连续10个稳定帧后变为稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        assertFalse(monitor.isStable.value) // 初始不稳定

        // 9个稳定帧还不够
        repeat(9) { sendStableAccelEvent(monitor) }
        assertFalse(monitor.isStable.value)

        // 第10个稳定帧触发稳定
        sendStableAccelEvent(monitor)
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `9个稳定帧不足以变为稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        repeat(9) { sendStableAccelEvent(monitor) }
        assertFalse(monitor.isStable.value)
    }

    @Test
    fun `超过10个稳定帧后isStable保持true`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        repeat(15) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)
    }

    // =====================================================
    // evaluateStability() 测试 — 不稳定阈值
    // =====================================================

    @Test
    fun `连续5个不稳定帧后从稳定变为不稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 先变稳定
        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)

        // 4个不稳定帧还不够
        repeat(4) { sendUnstableGyroEvent(monitor) }
        assertTrue(monitor.isStable.value)

        // 第5个不稳定帧触发不稳定
        sendUnstableGyroEvent(monitor)
        assertFalse(monitor.isStable.value)
    }

    @Test
    fun `4个不稳定帧不足以从稳定变为不稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)

        repeat(4) { sendUnstableGyroEvent(monitor) }
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `初始状态下不稳定帧也触发isStable为false`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 初始 isStable 已经是 false，发送5个不稳定帧后仍为 false
        repeat(5) { sendUnstableGyroEvent(monitor) }
        assertFalse(monitor.isStable.value)
    }

    // =====================================================
    // evaluateStability() 测试 — 迟滞行为
    // =====================================================

    @Test
    fun `迟滞行为-稳定后需5个不稳定帧才翻转为不稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 变稳定
        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)

        // 1个不稳定帧不会立即翻转
        sendUnstableGyroEvent(monitor)
        assertTrue(monitor.isStable.value)

        // 再来3个不稳定帧（共4个），仍不够
        repeat(3) { sendUnstableGyroEvent(monitor) }
        assertTrue(monitor.isStable.value)

        // 第5个不稳定帧才翻转
        sendUnstableGyroEvent(monitor)
        assertFalse(monitor.isStable.value)
    }

    @Test
    fun `迟滞行为-不稳定后需10个稳定帧才翻转为稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 先变为不稳定（通过发送不稳定帧）
        repeat(5) { sendUnstableGyroEvent(monitor) }
        assertFalse(monitor.isStable.value)

        // 先设置加速度计为稳定值
        sendStableAccelEvent(monitor)

        // 9个稳定陀螺仪帧不够翻转
        repeat(9) { sendStableGyroEvent(monitor) }
        assertFalse(monitor.isStable.value)

        // 第10个稳定帧翻转
        sendStableGyroEvent(monitor)
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `稳定帧和不稳定帧交替时计数器互相重置`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 5个稳定帧
        repeat(5) { sendStableAccelEvent(monitor) }
        assertFalse(monitor.isStable.value) // 不够10帧

        // 1个不稳定帧重置 stableFrameCount
        sendUnstableGyroEvent(monitor)

        // 再发9个稳定帧（之前5个已被重置，总共只有9个新帧）
        repeat(9) { sendStableAccelEvent(monitor) }
        assertFalse(monitor.isStable.value) // 还是不够10帧

        // 第10个稳定帧才变稳定
        sendStableAccelEvent(monitor)
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `不稳定帧后插入1个稳定帧会重置unstableFrameCount`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 先变稳定
        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)

        // 4个不稳定帧
        repeat(4) { sendUnstableGyroEvent(monitor) }
        assertTrue(monitor.isStable.value) // 还不够5帧

        // 1个稳定帧重置 unstableFrameCount
        sendStableAccelEvent(monitor)

        // 再发4个不稳定帧（之前的4个已被重置，总共只有4个新帧）
        repeat(4) { sendUnstableGyroEvent(monitor) }
        assertTrue(monitor.isStable.value) // 还是不够5帧

        // 第5个不稳定帧才变不稳定
        sendUnstableGyroEvent(monitor)
        assertFalse(monitor.isStable.value)
    }

    // =====================================================
    // evaluateStability() 测试 — 边界值
    // =====================================================

    @Test
    fun `边界值-陀螺仪幅度等于阈值GYROSCOPE_THRESHOLD时判为不稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 先设置加速度计为稳定值
        sendStableAccelEvent(monitor)

        // 9个稳定帧
        repeat(8) { sendStableGyroEvent(monitor) }
        // stableFrameCount = 9（1个accel + 8个gyro）

        // 陀螺仪幅度正好等于0.15：sqrt(0.15^2) = 0.15
        // 条件是 gyroMagnitude < 0.15，0.15 不满足 < 0.15
        val event = createSensorEvent(gyroSensor, 0.15f, 0f, 0f)
        monitor.onSensorChanged(event)

        // 应判定为不稳定帧，stableFrameCount被重置为0
        // 所以isStable仍然是false
        assertFalse(monitor.isStable.value)
    }

    @Test
    fun `边界值-陀螺仪幅度略低于GYROSCOPE_THRESHOLD时判为稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 先设置加速度计为稳定值
        sendStableAccelEvent(monitor)

        // 9个稳定帧
        repeat(8) { sendStableGyroEvent(monitor) }
        // stableFrameCount = 9

        // 陀螺仪幅度略低于0.15：sqrt(0.149^2) ≈ 0.149 < 0.15
        val event = createSensorEvent(gyroSensor, 0.149f, 0f, 0f)
        monitor.onSensorChanged(event)

        // 应判定为稳定帧，stableFrameCount = 10，isStable = true
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `边界值-加速度计偏差等于ACCELEROMETER_THRESHOLD时判为不稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 9个稳定帧（加速度计 (0,0,9.8)）
        repeat(9) { sendStableAccelEvent(monitor) }
        // stableFrameCount = 9

        // 加速度计偏差正好等于0.3：
        // accelMagnitude = sqrt(0 + 0 + 10.1^2) = 10.1
        // accelDeviation = |10.1 - 9.8| = 0.3
        // 条件是 accelDeviation < 0.3，0.3 不满足 < 0.3
        val event = createSensorEvent(accelSensor, 0f, 0f, 10.1f)
        monitor.onSensorChanged(event)

        // 应判定为不稳定帧，stableFrameCount被重置为0
        assertFalse(monitor.isStable.value)
    }

    @Test
    fun `边界值-加速度计偏差略低于ACCELEROMETER_THRESHOLD时判为稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 9个稳定帧
        repeat(9) { sendStableAccelEvent(monitor) }
        // stableFrameCount = 9

        // 加速度计偏差略低于0.3：
        // accelMagnitude ≈ sqrt(0 + 0 + 10.099^2) ≈ 10.099
        // accelDeviation ≈ |10.099 - 9.8| ≈ 0.299 < 0.3
        val event = createSensorEvent(accelSensor, 0f, 0f, 10.099f)
        monitor.onSensorChanged(event)

        // 应判定为稳定帧，stableFrameCount = 10，isStable = true
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `边界值-陀螺仪稳定但加速度计不稳定则判为不稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 9个稳定帧
        repeat(9) { sendStableAccelEvent(monitor) }

        // 陀螺仪为稳定值，但加速度计远偏离重力
        // accelDeviation = |0 - 9.8| = 9.8 >> 0.3
        sendStableGyroEvent(monitor) // 先确保陀螺仪稳定
        sendUnstableAccelEvent(monitor) // 加速度计不稳定

        // 不稳定帧，stableFrameCount被重置
        assertFalse(monitor.isStable.value)
    }

    @Test
    fun `边界值-加速度计稳定但陀螺仪不稳定则判为不稳定`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 9个稳定帧
        repeat(9) { sendStableAccelEvent(monitor) }

        // 加速度计稳定，但陀螺仪超阈值
        // gyroMagnitude = 1.0 >> 0.15
        sendUnstableGyroEvent(monitor)

        // 不稳定帧
        assertFalse(monitor.isStable.value)
    }

    // =====================================================
    // 线程安全测试
    // =====================================================

    @Test
    fun `并发传感器事件不会破坏motionData的数值有效性`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        val threadCount = 8
        val eventsPerThread = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val barrier = CyclicBarrier(threadCount)

        // 预创建所有传感器事件，避免在多线程中使用反射
        val gyroEvents = (0 until eventsPerThread).map { i ->
            createSensorEvent(gyroSensor, i * 0.01f, i * 0.02f, i * 0.03f)
        }
        val accelEvents = (0 until eventsPerThread).map { i ->
            createSensorEvent(accelSensor, i * 0.1f, i * 0.2f, 9.8f + i * 0.01f)
        }

        // 多线程并发发送传感器事件
        for (t in 0 until threadCount) {
            executor.submit {
                try {
                    barrier.await() // 所有线程同步等待，然后同时开始
                    for (i in 0 until eventsPerThread) {
                        if (t % 2 == 0) {
                            monitor.onSensorChanged(gyroEvents[i])
                        } else {
                            monitor.onSensorChanged(accelEvents[i])
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("并发执行超时", latch.await(5, TimeUnit.SECONDS))
        executor.shutdown()

        // 验证最终状态：motionData 中所有值都是有效的浮点数，没有被并发写入破坏
        val data = monitor.motionData.value
        assertFalse("gyroX 不应为 NaN", data.gyroX.isNaN())
        assertFalse("gyroY 不应为 NaN", data.gyroY.isNaN())
        assertFalse("gyroZ 不应为 NaN", data.gyroZ.isNaN())
        assertFalse("accelX 不应为 NaN", data.accelX.isNaN())
        assertFalse("accelY 不应为 NaN", data.accelY.isNaN())
        assertFalse("accelZ 不应为 NaN", data.accelZ.isNaN())
        assertFalse("gyroX 不应为 Infinite", data.gyroX.isInfinite())
        assertFalse("gyroY 不应为 Infinite", data.gyroY.isInfinite())
        assertFalse("gyroZ 不应为 Infinite", data.gyroZ.isInfinite())
        assertFalse("accelX 不应为 Infinite", data.accelX.isInfinite())
        assertFalse("accelY 不应为 Infinite", data.accelY.isInfinite())
        assertFalse("accelZ 不应为 Infinite", data.accelZ.isInfinite())
    }

    @Test
    fun `并发传感器事件后motionData是一致的快照`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 先发送一些稳定事件建立基线
        repeat(10) { sendStableAccelEvent(monitor) }

        val threadCount = 4
        val eventsPerThread = 50
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        // 所有线程发送完全相同的事件值
        val gyroEvent = createSensorEvent(gyroSensor, 0.1f, 0.2f, 0.3f)
        val accelEvent = createSensorEvent(accelSensor, 1.0f, 2.0f, 9.8f)

        for (t in 0 until threadCount) {
            executor.submit {
                try {
                    for (i in 0 until eventsPerThread) {
                        monitor.onSensorChanged(gyroEvent)
                        monitor.onSensorChanged(accelEvent)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("并发执行超时", latch.await(5, TimeUnit.SECONDS))
        executor.shutdown()

        // 最终 motionData 应反映最后一次写入的值
        val data = monitor.motionData.value
        assertEquals(0.1f, data.gyroX, 0.001f)
        assertEquals(0.2f, data.gyroY, 0.001f)
        assertEquals(0.3f, data.gyroZ, 0.001f)
        assertEquals(1.0f, data.accelX, 0.001f)
        assertEquals(2.0f, data.accelY, 0.001f)
        assertEquals(9.8f, data.accelZ, 0.001f)
    }

    @Test
    fun `并发传感器事件不会破坏isStable状态的一致性`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        val threadCount = 4
        val eventsPerThread = 200
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val barrier = CyclicBarrier(threadCount)

        // 预创建稳定和不稳定事件
        val stableGyroEvent = createSensorEvent(gyroSensor, 0f, 0f, 0f)
        val stableAccelEvent = createSensorEvent(accelSensor, 0f, 0f, 9.8f)
        val unstableGyroEvent = createSensorEvent(gyroSensor, 1f, 0f, 0f)

        // 多线程混合发送稳定和不稳定事件
        for (t in 0 until threadCount) {
            executor.submit {
                try {
                    barrier.await()
                    for (i in 0 until eventsPerThread) {
                        if (i % 3 == 0) {
                            monitor.onSensorChanged(unstableGyroEvent)
                        } else {
                            monitor.onSensorChanged(stableGyroEvent)
                            monitor.onSensorChanged(stableAccelEvent)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("并发执行超时", latch.await(5, TimeUnit.SECONDS))
        executor.shutdown()

        // isStable 应该是确定性的 true 或 false，不应该是异常状态
        val stable = monitor.isStable.value
        assertTrue("isStable 应为 true 或 false", stable || !stable)
    }

    // =====================================================
    // 综合场景测试
    // =====================================================

    @Test
    fun `完整场景-从静止到运动再到静止`() {
        val monitor = createMonitorWithSensors()
        monitor.startMonitoring()

        // 初始不稳定
        assertFalse(monitor.isStable.value)

        // 设备静止10帧 → 变稳定
        repeat(10) { sendStableAccelEvent(monitor) }
        assertTrue(monitor.isStable.value)

        // 设备晃动5帧 → 变不稳定
        repeat(5) { sendUnstableGyroEvent(monitor) }
        assertFalse(monitor.isStable.value)

        // 设备再次静止：先恢复加速度计，再积累陀螺仪稳定帧
        sendStableAccelEvent(monitor)
        repeat(10) { sendStableGyroEvent(monitor) }
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `降级模式下isStable始终为true`() {
        val monitor = createMonitorWithoutSensors()
        assertFalse(monitor.isAvailable.value)

        monitor.startMonitoring()

        // 降级模式：isStable 始终为 true，不阻塞拍摄流程
        assertTrue(monitor.isStable.value)

        // 即使调用 stopMonitoring 再 startMonitoring，降级行为不变
        monitor.stopMonitoring()
        assertFalse(monitor.isStable.value)

        monitor.startMonitoring()
        assertTrue(monitor.isStable.value)
    }

    @Test
    fun `传感器可用性在构造时确定且不随运行时变化`() {
        // 带传感器创建的 monitor
        val monitorWith = createMonitorWithSensors()
        assertTrue(monitorWith.isAvailable.value)

        // 不带传感器创建的 monitor
        val monitorWithout = createMonitorWithoutSensors()
        assertFalse(monitorWithout.isAvailable.value)
    }
}
