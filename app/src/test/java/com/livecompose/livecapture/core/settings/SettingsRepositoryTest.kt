package com.livecompose.livecapture.core.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = SettingsRepository(context)
    }

    @Test
    fun `detectionMode 默认值为 FAST`() = runTest {
        val result = repository.detectionMode.first()
        assertEquals(DetectionMode.FAST, result)
    }

    @Test
    fun `autoCapture 默认值为 true`() = runTest {
        val result = repository.autoCapture.first()
        assertEquals(true, result)
    }

    @Test
    fun `captureDelay 默认值为 0`() = runTest {
        val result = repository.captureDelay.first()
        assertEquals(0, result)
    }

    @Test
    fun `darkTheme 默认值为 true`() = runTest {
        val result = repository.darkTheme.first()
        assertEquals(true, result)
    }

    @Test
    fun `torchEnabled 默认值为 false`() = runTest {
        val result = repository.torchEnabled.first()
        assertEquals(false, result)
    }

    @Test
    fun `设置 detectionMode 后可以正确读取`() = runTest {
        repository.setDetectionMode(DetectionMode.PRO)
        val result = repository.detectionMode.first()
        assertEquals(DetectionMode.PRO, result)
    }

    @Test
    fun `设置 autoCapture 为 false 后可以正确读取`() = runTest {
        repository.setAutoCapture(false)
        val result = repository.autoCapture.first()
        assertEquals(false, result)
    }

    @Test
    fun `设置 captureDelay 后可以正确读取`() = runTest {
        repository.setCaptureDelay(500)
        val result = repository.captureDelay.first()
        assertEquals(500, result)
    }

    @Test
    fun `设置 darkTheme 为 false 后可以正确读取`() = runTest {
        repository.setDarkTheme(false)
        val result = repository.darkTheme.first()
        assertEquals(false, result)
    }

    @Test
    fun `设置 torchEnabled 为 true 后可以正确读取`() = runTest {
        repository.setTorchEnabled(true)
        val result = repository.torchEnabled.first()
        assertEquals(true, result)
    }

    @Test
    fun `detectionMode 多次顺序写入后读取最终值`() = runTest {
        repository.setDetectionMode(DetectionMode.PRO)
        repository.setDetectionMode(DetectionMode.FAST)
        val result = repository.detectionMode.first()
        assertEquals(DetectionMode.FAST, result)
    }

    @Test
    fun `autoCapture 多次顺序写入后读取最终值`() = runTest {
        repository.setAutoCapture(false)
        repository.setAutoCapture(true)
        repository.setAutoCapture(false)
        val result = repository.autoCapture.first()
        assertEquals(false, result)
    }

    @Test
    fun `captureDelay 多次顺序写入后读取最终值`() = runTest {
        repository.setCaptureDelay(100)
        repository.setCaptureDelay(200)
        repository.setCaptureDelay(300)
        val result = repository.captureDelay.first()
        assertEquals(300, result)
    }

    @Test
    fun `darkTheme 多次顺序写入后读取最终值`() = runTest {
        repository.setDarkTheme(false)
        repository.setDarkTheme(true)
        repository.setDarkTheme(false)
        val result = repository.darkTheme.first()
        assertEquals(false, result)
    }

    @Test
    fun `torchEnabled 多次顺序写入后读取最终值`() = runTest {
        repository.setTorchEnabled(true)
        repository.setTorchEnabled(false)
        repository.setTorchEnabled(true)
        val result = repository.torchEnabled.first()
        assertEquals(true, result)
    }

    @Test
    fun `detectionMode 设置后持久化并可重新读取`() = runTest {
        repository.setDetectionMode(DetectionMode.PRO)
        val result = repository.detectionMode.first()
        assertEquals(DetectionMode.PRO, result)
    }

    @Test
    fun `autoCapture 从默认值变更后持久化并可重新读取`() = runTest {
        repository.setAutoCapture(false)
        val result = repository.autoCapture.first()
        assertEquals(false, result)
    }

    @Test
    fun `captureDelay 从默认值变更后持久化并可重新读取`() = runTest {
        repository.setCaptureDelay(1000)
        val result = repository.captureDelay.first()
        assertEquals(1000, result)
    }

    @Test
    fun `darkTheme 从默认值变更后持久化并可重新读取`() = runTest {
        repository.setDarkTheme(false)
        val result = repository.darkTheme.first()
        assertEquals(false, result)
    }

    @Test
    fun `torchEnabled 从默认值变更后持久化并可重新读取`() = runTest {
        repository.setTorchEnabled(true)
        val result = repository.torchEnabled.first()
        assertEquals(true, result)
    }

    @Test
    fun `captureDelay 设置为负数后可以正确读取`() = runTest {
        repository.setCaptureDelay(-1)
        val result = repository.captureDelay.first()
        assertEquals(-1, result)
    }

    @Test
    fun `captureDelay 设置为大数值后可以正确读取`() = runTest {
        repository.setCaptureDelay(Int.MAX_VALUE)
        val result = repository.captureDelay.first()
        assertEquals(Int.MAX_VALUE, result)
    }
}
