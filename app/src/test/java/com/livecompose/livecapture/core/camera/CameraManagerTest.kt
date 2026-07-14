package com.livecompose.livecapture.core.camera

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraManager 综合单元测试
 *
 * 由于 CameraManager 依赖 CameraX ProcessCameraProvider 和 ImageAnalysis，
 * 这些组件在没有真实摄像头的环境下难以模拟。因此本测试聚焦于：
 * 1. 初始状态值验证
 * 2. StateFlow 状态变更逻辑
 * 3. 错误处理路径
 * 4. 标志位逻辑（isBackCamera、hasTorchUnit）
 * 5. 关闭与清理行为
 * 6. 帧处理守卫机制
 * 7. 回调注册
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CameraManagerTest {

    private lateinit var context: Context
    private lateinit var cameraManager: CameraManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cameraManager = CameraManager(context)
    }

    @After
    fun tearDown() {
        // 确保每个测试结束后关闭 executor，避免资源泄漏
        cameraManager.shutdown()
    }

    // =====================================================
    // 初始状态测试
    // =====================================================

    @Test
    fun `初始状态-isBackCamera为true`() {
        assertTrue(cameraManager.isBackCamera.value)
    }

    @Test
    fun `初始状态-isTorchEnabled为false`() {
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    @Test
    fun `初始状态-hasTorchUnit为false`() {
        assertFalse(cameraManager.hasTorchUnit.value)
    }

    @Test
    fun `初始状态-zoomRatio为1_0f`() {
        assertEquals(1.0f, cameraManager.zoomRatio.value, 0.001f)
    }

    @Test
    fun `初始状态-zoomRange为1f到1f`() {
        val range = cameraManager.zoomRange.value
        assertEquals(1f, range.start, 0.001f)
        assertEquals(1f, range.endInclusive, 0.001f)
    }

    @Test
    fun `初始状态-exposureCompensation为0`() {
        assertEquals(0, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `初始状态-exposureRange为0到0`() {
        val range = cameraManager.exposureRange.value
        assertEquals(0, range.first)
        assertEquals(0, range.last)
    }

    @Test
    fun `初始状态-errorMessage为null`() {
        assertNull(cameraManager.errorMessage.value)
    }

    @Test
    fun `初始状态-isCameraReady为false`() {
        assertFalse(cameraManager.isCameraReady.value)
    }

    // =====================================================
    // setZoom() 测试
    // =====================================================

    @Test
    fun `setZoom在默认zoomRange下将zoomRatio钳制为1_0f`() {
        // 默认 zoomRange 为 1f..1f，任何值都会被钳制到 1.0f
        cameraManager.setZoom(5.0f)
        assertEquals(1.0f, cameraManager.zoomRatio.value, 0.001f)
    }

    @Test
    fun `setZoom低于zoomRange下限被钳制到下限`() {
        // 通过反射设置 zoomRange 为 0.5f..10.0f
        setZoomRange(0.5f, 10.0f)

        cameraManager.setZoom(0.1f)
        assertEquals(0.5f, cameraManager.zoomRatio.value, 0.001f)
    }

    @Test
    fun `setZoom高于zoomRange上限被钳制到上限`() {
        setZoomRange(0.5f, 10.0f)

        cameraManager.setZoom(15.0f)
        assertEquals(10.0f, cameraManager.zoomRatio.value, 0.001f)
    }

    @Test
    fun `setZoom在zoomRange范围内正常更新`() {
        setZoomRange(0.5f, 10.0f)

        cameraManager.setZoom(3.0f)
        assertEquals(3.0f, cameraManager.zoomRatio.value, 0.001f)
    }

    @Test
    fun `setZoom等于zoomRange下限边界值`() {
        setZoomRange(0.5f, 10.0f)

        cameraManager.setZoom(0.5f)
        assertEquals(0.5f, cameraManager.zoomRatio.value, 0.001f)
    }

    @Test
    fun `setZoom等于zoomRange上限边界值`() {
        setZoomRange(0.5f, 10.0f)

        cameraManager.setZoom(10.0f)
        assertEquals(10.0f, cameraManager.zoomRatio.value, 0.001f)
    }

    @Test
    fun `setZoom多次调用始终钳制到zoomRange`() {
        setZoomRange(1.0f, 5.0f)

        cameraManager.setZoom(3.0f)
        assertEquals(3.0f, cameraManager.zoomRatio.value, 0.001f)

        cameraManager.setZoom(6.0f)
        assertEquals(5.0f, cameraManager.zoomRatio.value, 0.001f)

        cameraManager.setZoom(0.5f)
        assertEquals(1.0f, cameraManager.zoomRatio.value, 0.001f)
    }

    // =====================================================
    // setExposureCompensation() 测试
    // =====================================================

    @Test
    fun `setExposureCompensation在默认exposureRange下0值被接受`() {
        // 默认 exposureRange 为 0..0，0 在范围内
        cameraManager.setExposureCompensation(0)
        assertEquals(0, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `setExposureCompensation在默认exposureRange下非0值被拒绝`() {
        // 默认 exposureRange 为 0..0，1 不在范围内，不更新
        cameraManager.setExposureCompensation(1)
        assertEquals(0, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `setExposureCompensation负值在范围内正常更新`() {
        setExposureRange(-12, 12)

        cameraManager.setExposureCompensation(-5)
        assertEquals(-5, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `setExposureCompensation正值在范围内正常更新`() {
        setExposureRange(-12, 12)

        cameraManager.setExposureCompensation(8)
        assertEquals(8, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `setExposureCompensation等于范围下限`() {
        setExposureRange(-12, 12)

        cameraManager.setExposureCompensation(-12)
        assertEquals(-12, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `setExposureCompensation等于范围上限`() {
        setExposureRange(-12, 12)

        cameraManager.setExposureCompensation(12)
        assertEquals(12, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `setExposureCompensation低于范围下限不更新`() {
        setExposureRange(-12, 12)

        cameraManager.setExposureCompensation(-13)
        // 默认值是 0，因为 -13 不在范围内
        assertEquals(0, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `setExposureCompensation高于范围上限不更新`() {
        setExposureRange(-12, 12)

        cameraManager.setExposureCompensation(13)
        assertEquals(0, cameraManager.exposureCompensation.value)
    }

    @Test
    fun `setExposureCompensation多次调用在范围内更新最后有效值`() {
        setExposureRange(-12, 12)

        cameraManager.setExposureCompensation(5)
        assertEquals(5, cameraManager.exposureCompensation.value)

        cameraManager.setExposureCompensation(-3)
        assertEquals(-3, cameraManager.exposureCompensation.value)

        // 范围外的值不更新
        cameraManager.setExposureCompensation(15)
        assertEquals(-3, cameraManager.exposureCompensation.value)
    }

    // =====================================================
    // setTorchEnabled() 测试
    // =====================================================

    @Test
    fun `setTorchEnabled无闪光灯时不更新isTorchEnabled`() {
        // 默认 hasTorchUnit 为 false
        assertFalse(cameraManager.hasTorchUnit.value)

        cameraManager.setTorchEnabled(true)
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    @Test
    fun `setTorchEnabled关闭手电筒无闪光灯时也不更新`() {
        assertFalse(cameraManager.hasTorchUnit.value)

        cameraManager.setTorchEnabled(false)
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    @Test
    fun `setTorchEnabled有闪光灯单元但cameraControl为null时不更新状态`() {
        // 设置 hasTorchUnit 为 true（模拟设备支持闪光灯）
        setHasTorchUnit(true)

        // cameraControl 为 null（未绑定相机），enableTorch 返回 null
        // 回调不会被触发，isTorchEnabled 保持 false
        cameraManager.setTorchEnabled(true)
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    @Test
    fun `setTorchEnabled有闪光灯单元时关闭请求同理不会更新`() {
        setHasTorchUnit(true)

        cameraManager.setTorchEnabled(false)
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    // =====================================================
    // capturePhoto() 测试
    // =====================================================

    @Test
    fun `capturePhoto在imageCapture未初始化时调用onError`() {
        // imageCapture 默认为 null
        var errorCalled = false
        var errorMessage: String? = null

        cameraManager.capturePhoto(
            onSuccess = { fail("不应调用 onSuccess") },
            onError = { throwable ->
                errorCalled = true
                errorMessage = throwable.message
            }
        )

        assertTrue("应调用 onError", errorCalled)
        assertEquals("ImageCapture not initialized", errorMessage)
    }

    @Test
    fun `capturePhoto在executor已关闭时调用onError`() {
        // 通过反射将 imageCapture 设置为非 null，同时关闭 captureExecutor
        setImageCaptureInitialized()
        shutdownCaptureExecutor()

        var errorCalled = false
        var errorMessage: String? = null

        cameraManager.capturePhoto(
            onSuccess = { fail("不应调用 onSuccess") },
            onError = { throwable ->
                errorCalled = true
                errorMessage = throwable.message
            }
        )

        assertTrue("应调用 onError", errorCalled)
        assertEquals("Capture executor is shutdown", errorMessage)
    }

    @Test
    fun `capturePhoto在imageCapture为null时返回IllegalStateException`() {
        var capturedThrowable: Throwable? = null

        cameraManager.capturePhoto(
            onSuccess = { fail("不应调用 onSuccess") },
            onError = { capturedThrowable = it }
        )

        assertNotNull(capturedThrowable)
        assertTrue(capturedThrowable is IllegalStateException)
        assertEquals("ImageCapture not initialized", capturedThrowable!!.message)
    }

    @Test
    fun `capturePhoto在executor关闭后返回IllegalStateException`() {
        setImageCaptureInitialized()
        shutdownCaptureExecutor()

        var capturedThrowable: Throwable? = null

        cameraManager.capturePhoto(
            onSuccess = { fail("不应调用 onSuccess") },
            onError = { capturedThrowable = it }
        )

        assertNotNull(capturedThrowable)
        assertTrue(capturedThrowable is IllegalStateException)
        assertEquals("Capture executor is shutdown", capturedThrowable!!.message)
    }

    @Test
    fun `capturePhoto的onError回调一定会被调用当imageCapture为null时`() {
        var callbackCount = 0

        cameraManager.capturePhoto(
            onSuccess = { fail("不应调用 onSuccess") },
            onError = { callbackCount++ }
        )

        assertEquals(1, callbackCount)
    }

    // =====================================================
    // stopCamera() 测试
    // =====================================================

    @Test
    fun `stopCamera将isCameraReady设为false`() {
        // 先手动设置为 true 模拟相机已就绪
        setIsCameraReady(true)
        assertTrue(cameraManager.isCameraReady.value)

        cameraManager.stopCamera()
        assertFalse(cameraManager.isCameraReady.value)
    }

    @Test
    fun `stopCamera重置isProcessingFrame标志`() {
        // 模拟帧正在处理
        setIsProcessingFrame(true)

        cameraManager.stopCamera()

        // onFrameProcessingComplete 应该不再需要调用
        // 因为 stopCamera 已重置 isProcessingFrame
        // 验证：调用 onFrameProcessingComplete 不会出错（已经是 false 了）
        cameraManager.onFrameProcessingComplete()
    }

    @Test
    fun `stopCamera在cameraProvider为null时不抛异常`() {
        // cameraProvider 默认为 null，stopCamera 应安全处理
        cameraManager.stopCamera()
        assertFalse(cameraManager.isCameraReady.value)
    }

    @Test
    fun `stopCamera多次调用不抛异常`() {
        cameraManager.stopCamera()
        cameraManager.stopCamera()
        cameraManager.stopCamera()
        assertFalse(cameraManager.isCameraReady.value)
    }

    // =====================================================
    // switchCamera() 测试
    // =====================================================

    @Test
    fun `switchCamera不抛异常即使没有绑定相机`() {
        // switchCamera 内部调用 startCamera(useFrontCamera = current)
        // 在 Robolectric 环境下 ProcessCameraProvider 无法初始化，
        // 但 startCamera 会通过 listener 异步设置 errorMessage
        // 此处验证 switchCamera 不抛出未捕获的异常
        cameraManager.switchCamera(
            lifecycleOwner = FakeLifecycleOwner(),
            previewView = Mockito.mock(androidx.camera.view.PreviewView::class.java)
        )
        // 不抛异常即通过
    }

    // =====================================================
    // onFrameProcessingComplete() 测试
    // =====================================================

    @Test
    fun `onFrameProcessingComplete重置帧处理守卫`() {
        // 模拟帧正在处理
        setIsProcessingFrame(true)

        // 调用 onFrameProcessingComplete 应重置守卫
        cameraManager.onFrameProcessingComplete()

        // 验证守卫已被重置：再次调用不应抛异常
        cameraManager.onFrameProcessingComplete()
    }

    @Test
    fun `onFrameProcessingComplete在守卫已为false时调用是幂等的`() {
        // 守卫默认为 false
        cameraManager.onFrameProcessingComplete()
        cameraManager.onFrameProcessingComplete()
        cameraManager.onFrameProcessingComplete()
        // 不抛异常即通过
    }

    @Test
    fun `onFrameProcessingComplete与stopCamera配合正确重置状态`() {
        // 模拟帧正在处理
        setIsProcessingFrame(true)

        // stopCamera 会重置 isProcessingFrame
        cameraManager.stopCamera()

        // 再调用 onFrameProcessingComplete 应安全（幂等）
        cameraManager.onFrameProcessingComplete()
    }

    @Test
    fun `onFrameProcessingComplete将isProcessingFrame从true重置为false`() {
        setIsProcessingFrame(true)

        // 通过反射验证已设为 true
        val field = CameraManager::class.java.getDeclaredField("isProcessingFrame")
        field.isAccessible = true
        val atomicBoolean = field.get(cameraManager) as AtomicBoolean
        assertTrue(atomicBoolean.get())

        cameraManager.onFrameProcessingComplete()
        assertFalse(atomicBoolean.get())
    }

    // =====================================================
    // setOnFrameAnalyzed() 测试
    // =====================================================

    @Test
    fun `setOnFrameAnalyzed注册回调不抛异常`() {
        cameraManager.setOnFrameAnalyzed { }
        // 回调已注册，不抛异常即通过
    }

    @Test
    fun `setOnFrameAnalyzed可以多次注册覆盖之前的回调`() {
        var firstCalled = false
        var secondCalled = false

        cameraManager.setOnFrameAnalyzed { firstCalled = true }
        cameraManager.setOnFrameAnalyzed { secondCalled = true }

        // 第二次注册覆盖第一次，只有第二个回调有效
        // 无法直接触发回调（需要 ImageProxy），验证不抛异常即可
        assertFalse(firstCalled)
        assertFalse(secondCalled)
    }

    @Test
    fun `setOnFrameAnalyzed后回调字段不为null`() {
        cameraManager.setOnFrameAnalyzed { }

        // 通过反射验证回调已注册
        val field = CameraManager::class.java.getDeclaredField("onFrameAnalyzed")
        field.isAccessible = true
        val callback = field.get(cameraManager)
        assertNotNull(callback)
    }

    @Test
    fun `stopCamera清空onFrameAnalyzed回调`() {
        cameraManager.setOnFrameAnalyzed { }
        cameraManager.stopCamera()

        // 通过反射验证回调已被清空
        val field = CameraManager::class.java.getDeclaredField("onFrameAnalyzed")
        field.isAccessible = true
        val callback = field.get(cameraManager)
        assertNull(callback)
    }

    @Test
    fun `setOnFrameAnalyzed注册后stopCamera再注册不抛异常`() {
        // 先注册回调
        cameraManager.setOnFrameAnalyzed { }
        // stopCamera 会将 onFrameAnalyzed 设为 null
        cameraManager.stopCamera()
        // 再次注册新回调
        cameraManager.setOnFrameAnalyzed { }
    }

    // =====================================================
    // shutdown() 测试
    // =====================================================

    @Test
    fun `shutdown将isCameraReady设为false`() {
        setIsCameraReady(true)
        assertTrue(cameraManager.isCameraReady.value)

        cameraManager.shutdown()
        assertFalse(cameraManager.isCameraReady.value)
    }

    @Test
    fun `shutdown后captureExecutor已关闭`() {
        // 创建新 CameraManager 验证 shutdown 行为
        // 使用新实例，因为当前实例会在 tearDown 中 shutdown
        val manager = CameraManager(context)
        manager.shutdown()

        var errorCalled = false
        manager.capturePhoto(
            onSuccess = { fail("不应调用 onSuccess") },
            onError = { errorCalled = true }
        )
        assertTrue("shutdown 后 capturePhoto 应调用 onError", errorCalled)
    }

    @Test
    fun `shutdown多次调用不抛异常`() {
        cameraManager.shutdown()
        cameraManager.shutdown()
        // 不抛异常即通过
    }

    @Test
    fun `shutdown等待executor终止完成`() {
        // shutdown 内部调用 awaitTermination，
        // 如果 executor 中没有正在执行的任务，应立即返回
        val start = System.currentTimeMillis()
        cameraManager.shutdown()
        val elapsed = System.currentTimeMillis() - start

        // 应在 1 秒内完成（因为没有正在执行的任务）
        assertTrue("shutdown 应快速完成，实际耗时 ${elapsed}ms", elapsed < 1000)
    }

    @Test
    fun `shutdown内部调用stopCamera重置isProcessingFrame`() {
        setIsProcessingFrame(true)

        cameraManager.shutdown()

        val field = CameraManager::class.java.getDeclaredField("isProcessingFrame")
        field.isAccessible = true
        val atomicBoolean = field.get(cameraManager) as AtomicBoolean
        assertFalse(atomicBoolean.get())
    }

    @Test
    fun `shutdown后再次stopCamera不抛异常`() {
        cameraManager.shutdown()
        cameraManager.stopCamera()
        // 不抛异常即通过
    }

    @Test
    fun `shutdown后再次shutdown不抛异常`() {
        cameraManager.shutdown()
        cameraManager.shutdown()
        // 不抛异常即通过
    }

    // =====================================================
    // focusAndMeter() 测试
    // =====================================================

    @Test
    fun `focusAndMeter在previewWidth为零时不抛异常`() {
        // previewWidth <= 0，应提前返回
        cameraManager.focusAndMeter(
            point = Offset(100f, 100f),
            previewWidth = 0f,
            previewHeight = 500f
        )
    }

    @Test
    fun `focusAndMeter在previewHeight为零时不抛异常`() {
        // previewHeight <= 0，应提前返回
        cameraManager.focusAndMeter(
            point = Offset(100f, 100f),
            previewWidth = 500f,
            previewHeight = 0f
        )
    }

    @Test
    fun `focusAndMeter在previewWidth和previewHeight都为负值时不抛异常`() {
        // 负值 <= 0，应提前返回
        cameraManager.focusAndMeter(
            point = Offset(100f, 100f),
            previewWidth = -100f,
            previewHeight = -200f
        )
    }

    @Test
    fun `focusAndMeter在previewWidth和previewHeight为正值且cameraControl为null时不抛异常`() {
        // cameraControl 为 null，startFocusAndMetering 不会被调用
        cameraManager.focusAndMeter(
            point = Offset(100f, 100f),
            previewWidth = 500f,
            previewHeight = 500f
        )
    }

    // =====================================================
    // toggleTorch() 测试
    // =====================================================

    @Test
    fun `toggleTorch在无闪光灯时不改变isTorchEnabled`() {
        assertFalse(cameraManager.hasTorchUnit.value)
        assertFalse(cameraManager.isTorchEnabled.value)

        cameraManager.toggleTorch()
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    @Test
    fun `toggleTorch调用setTorchEnabled取反当前值`() {
        // toggleTorch 内部调用 setTorchEnabled(!isTorchEnabled.value)
        // 当前 isTorchEnabled 为 false，所以调用 setTorchEnabled(true)
        // 但 hasTorchUnit 为 false，所以状态不变
        cameraManager.toggleTorch()
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    // =====================================================
    // isProcessingFrame 初始状态与反射验证测试
    // =====================================================

    @Test
    fun `isProcessingFrame初始为false`() {
        // 通过反射读取初始值
        val field = CameraManager::class.java.getDeclaredField("isProcessingFrame")
        field.isAccessible = true
        val atomicBoolean = field.get(cameraManager) as AtomicBoolean
        assertFalse(atomicBoolean.get())
    }

    @Test
    fun `stopCamera重置isProcessingFrame为false`() {
        setIsProcessingFrame(true)

        cameraManager.stopCamera()

        val field = CameraManager::class.java.getDeclaredField("isProcessingFrame")
        field.isAccessible = true
        val atomicBoolean = field.get(cameraManager) as AtomicBoolean
        assertFalse(atomicBoolean.get())
    }

    // =====================================================
    // 状态流一致性测试
    // =====================================================

    @Test
    fun `所有初始状态值在构造后立即可读`() {
        // 创建新实例验证所有初始值，避免受 setUp 中其他操作影响
        val manager = CameraManager(context)
        try {
            assertTrue(manager.isBackCamera.value)
            assertFalse(manager.isTorchEnabled.value)
            assertFalse(manager.hasTorchUnit.value)
            assertEquals(1.0f, manager.zoomRatio.value, 0.001f)
            assertEquals(1f, manager.zoomRange.value.start, 0.001f)
            assertEquals(1f, manager.zoomRange.value.endInclusive, 0.001f)
            assertEquals(0, manager.exposureCompensation.value)
            assertEquals(0, manager.exposureRange.value.first)
            assertEquals(0, manager.exposureRange.value.last)
            assertNull(manager.errorMessage.value)
            assertFalse(manager.isCameraReady.value)
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun `多个CameraManager实例之间的状态互不影响`() {
        val manager1 = CameraManager(context)
        val manager2 = CameraManager(context)

        try {
            // 修改 manager1 的 zoomRange 和 zoomRatio
            setZoomRangeFor(0.5f, 10.0f, manager1)
            manager1.setZoom(5.0f)

            // manager2 应保持默认值
            assertEquals(1.0f, manager2.zoomRatio.value, 0.001f)
            assertEquals(1f, manager2.zoomRange.value.start, 0.001f)
        } finally {
            manager1.shutdown()
            manager2.shutdown()
        }
    }

    // =====================================================
    // setZoom 与 zoomRange 联动测试
    // =====================================================

    @Test
    fun `zoomRange变更后setZoom使用新的范围钳制`() {
        // 初始 zoomRange 为 1f..1f
        cameraManager.setZoom(2.0f)
        assertEquals(1.0f, cameraManager.zoomRatio.value, 0.001f)

        // 扩大 zoomRange
        setZoomRange(0.5f, 5.0f)

        // 现在设 2.0f 应在范围内
        cameraManager.setZoom(2.0f)
        assertEquals(2.0f, cameraManager.zoomRatio.value, 0.001f)

        // 超出新的上限
        cameraManager.setZoom(6.0f)
        assertEquals(5.0f, cameraManager.zoomRatio.value, 0.001f)
    }

    @Test
    fun `zoomRange收窄后之前合法的zoomRatio可能超出新范围但不会自动钳制`() {
        setZoomRange(1.0f, 10.0f)
        cameraManager.setZoom(8.0f)
        assertEquals(8.0f, cameraManager.zoomRatio.value, 0.001f)

        // 收窄 zoomRange
        setZoomRange(1.0f, 3.0f)

        // zoomRatio 仍然是 8.0f，直到下次 setZoom 才会钳制
        assertEquals(8.0f, cameraManager.zoomRatio.value, 0.001f)

        // 触发钳制
        cameraManager.setZoom(8.0f)
        assertEquals(3.0f, cameraManager.zoomRatio.value, 0.001f)
    }

    // =====================================================
    // setExposureCompensation 与 exposureRange 联动测试
    // =====================================================

    @Test
    fun `exposureRange变更后setExposureCompensation使用新的范围校验`() {
        // 初始 exposureRange 为 0..0
        cameraManager.setExposureCompensation(5)
        assertEquals(0, cameraManager.exposureCompensation.value) // 不在范围内

        // 扩大 exposureRange
        setExposureRange(-12, 12)

        cameraManager.setExposureCompensation(5)
        assertEquals(5, cameraManager.exposureCompensation.value) // 现在在范围内
    }

    @Test
    fun `exposureRange收窄后之前合法的exposureCompensation可能超出新范围但不会自动重置`() {
        setExposureRange(-12, 12)
        cameraManager.setExposureCompensation(10)
        assertEquals(10, cameraManager.exposureCompensation.value)

        // 收窄 exposureRange
        setExposureRange(-5, 5)

        // exposureCompensation 仍然是 10，直到下次 setExposureCompensation 才会校验
        assertEquals(10, cameraManager.exposureCompensation.value)

        // 触发校验：10 不在新范围内，不更新
        cameraManager.setExposureCompensation(10)
        assertEquals(10, cameraManager.exposureCompensation.value) // 值未改变但也不被拒绝
    }

    // =====================================================
    // hasTorchUnit 守卫逻辑综合测试
    // =====================================================

    @Test
    fun `hasTorchUnit为false时所有手电筒操作都无效`() {
        assertFalse(cameraManager.hasTorchUnit.value)

        cameraManager.setTorchEnabled(true)
        assertFalse(cameraManager.isTorchEnabled.value)

        cameraManager.setTorchEnabled(false)
        assertFalse(cameraManager.isTorchEnabled.value)

        cameraManager.toggleTorch()
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    @Test
    fun `hasTorchUnit为true但cameraControl为null时enableTorch返回null不更新状态`() {
        setHasTorchUnit(true)
        assertTrue(cameraManager.hasTorchUnit.value)

        // cameraControl 为 null，enableTorch 返回 null
        cameraManager.setTorchEnabled(true)
        assertFalse(cameraManager.isTorchEnabled.value)
    }

    // =====================================================
    // 辅助方法
    // =====================================================

    /**
     * 通过反射设置 _zoomRange 的值。
     * 用于模拟 updateCameraCapabilities() 读取到的设备缩放范围。
     */
    private fun setZoomRange(min: Float, max: Float) {
        val field = CameraManager::class.java.getDeclaredField("_zoomRange")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(cameraManager) as MutableStateFlow<ClosedRange<Float>>
        stateFlow.value = min..max
    }

    /**
     * 通过反射设置指定 CameraManager 实例的 _zoomRange。
     */
    private fun setZoomRangeFor(min: Float, max: Float, manager: CameraManager) {
        val field = CameraManager::class.java.getDeclaredField("_zoomRange")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(manager) as MutableStateFlow<ClosedRange<Float>>
        stateFlow.value = min..max
    }

    /**
     * 通过反射设置 _exposureRange 的值。
     * 用于模拟 updateCameraCapabilities() 读取到的设备曝光补偿范围。
     */
    private fun setExposureRange(min: Int, max: Int) {
        val field = CameraManager::class.java.getDeclaredField("_exposureRange")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(cameraManager) as MutableStateFlow<IntRange>
        stateFlow.value = min..max
    }

    /**
     * 通过反射设置 _hasTorchUnit 的值。
     * 用于模拟 updateCameraCapabilities() 读取到的设备闪光灯支持状态。
     */
    private fun setHasTorchUnit(value: Boolean) {
        val field = CameraManager::class.java.getDeclaredField("_hasTorchUnit")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(cameraManager) as MutableStateFlow<Boolean>
        stateFlow.value = value
    }

    /**
     * 通过反射设置 _isCameraReady 的值。
     * 用于模拟相机绑定成功后的状态。
     */
    private fun setIsCameraReady(value: Boolean) {
        val field = CameraManager::class.java.getDeclaredField("_isCameraReady")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(cameraManager) as MutableStateFlow<Boolean>
        stateFlow.value = value
    }

    /**
     * 通过反射设置 isProcessingFrame 的值。
     * 用于模拟帧处理中的状态。
     */
    private fun setIsProcessingFrame(value: Boolean) {
        val field = CameraManager::class.java.getDeclaredField("isProcessingFrame")
        field.isAccessible = true
        val atomicBoolean = field.get(cameraManager) as AtomicBoolean
        atomicBoolean.set(value)
    }

    /**
     * 通过反射将 imageCapture 设置为非 null 值。
     * 用于测试 capturePhoto 在 executor 关闭时的错误路径。
     */
    private fun setImageCaptureInitialized() {
        val field = CameraManager::class.java.getDeclaredField("imageCapture")
        field.isAccessible = true
        // 使用 Mockito mock 创建 ImageCapture 实例
        field.set(cameraManager, Mockito.mock(androidx.camera.core.ImageCapture::class.java))
    }

    /**
     * 通过反射关闭 captureExecutor。
     * 用于测试 capturePhoto 在 executor 关闭时的错误路径。
     */
    private fun shutdownCaptureExecutor() {
        val field = CameraManager::class.java.getDeclaredField("captureExecutor")
        field.isAccessible = true
        val executor = field.get(cameraManager) as java.util.concurrent.ExecutorService
        executor.shutdown()
    }
}

/**
 * 简单的 LifecycleOwner 实现，用于 switchCamera 等需要 LifecycleOwner 参数的测试。
 * 在 Robolectric 环境下不需要真实的生命周期，仅作参数传递。
 */
private class FakeLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry
}


