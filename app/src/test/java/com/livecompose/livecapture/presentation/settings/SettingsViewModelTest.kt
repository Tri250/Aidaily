package com.livecompose.livecapture.presentation.settings

import com.livecompose.livecapture.core.settings.DetectionMode
import com.livecompose.livecapture.core.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    private lateinit var repository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel

    private val detectionModeFlow = MutableStateFlow(DetectionMode.FAST)
    private val autoCaptureFlow = MutableStateFlow(true)
    private val captureDelayFlow = MutableStateFlow(0)
    private val darkThemeFlow = MutableStateFlow(true)
    private val torchEnabledFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        repository = mock()

        whenever(repository.detectionMode).thenReturn(detectionModeFlow)
        whenever(repository.autoCapture).thenReturn(autoCaptureFlow)
        whenever(repository.captureDelay).thenReturn(captureDelayFlow)
        whenever(repository.darkTheme).thenReturn(darkThemeFlow)
        whenever(repository.torchEnabled).thenReturn(torchEnabledFlow)

        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `detectionMode 初始值为 FAST`() = runTest {
        val result = viewModel.detectionMode.first()
        assertEquals(DetectionMode.FAST, result)
    }

    @Test
    fun `autoCapture 初始值为 true`() = runTest {
        val result = viewModel.autoCapture.first()
        assertEquals(true, result)
    }

    @Test
    fun `captureDelay 初始值为 0`() = runTest {
        val result = viewModel.captureDelay.first()
        assertEquals(0, result)
    }

    @Test
    fun `darkTheme 初始值为 true`() = runTest {
        val result = viewModel.darkTheme.first()
        assertEquals(true, result)
    }

    @Test
    fun `torchEnabled 初始值为 false`() = runTest {
        val result = viewModel.torchEnabled.first()
        assertEquals(false, result)
    }

    @Test
    fun `detectionMode 反映仓库数据变化`() = runTest {
        detectionModeFlow.value = DetectionMode.PRO
        val result = viewModel.detectionMode.first()
        assertEquals(DetectionMode.PRO, result)
    }

    @Test
    fun `autoCapture 反映仓库数据变化`() = runTest {
        autoCaptureFlow.value = false
        val result = viewModel.autoCapture.first()
        assertEquals(false, result)
    }

    @Test
    fun `captureDelay 反映仓库数据变化`() = runTest {
        captureDelayFlow.value = 500
        val result = viewModel.captureDelay.first()
        assertEquals(500, result)
    }

    @Test
    fun `darkTheme 反映仓库数据变化`() = runTest {
        darkThemeFlow.value = false
        val result = viewModel.darkTheme.first()
        assertEquals(false, result)
    }

    @Test
    fun `torchEnabled 反映仓库数据变化`() = runTest {
        torchEnabledFlow.value = true
        val result = viewModel.torchEnabled.first()
        assertEquals(true, result)
    }

    @Test
    fun `setDetectionMode 委托给仓库`() = runTest {
        viewModel.setDetectionMode(DetectionMode.PRO)
        verify(repository).setDetectionMode(DetectionMode.PRO)
    }

    @Test
    fun `setAutoCapture 委托给仓库`() = runTest {
        viewModel.setAutoCapture(false)
        verify(repository).setAutoCapture(false)
    }

    @Test
    fun `setCaptureDelay 委托给仓库`() = runTest {
        viewModel.setCaptureDelay(300)
        verify(repository).setCaptureDelay(300)
    }

    @Test
    fun `setDarkTheme 委托给仓库`() = runTest {
        viewModel.setDarkTheme(false)
        verify(repository).setDarkTheme(false)
    }

    @Test
    fun `setTorchEnabled 委托给仓库`() = runTest {
        viewModel.setTorchEnabled(true)
        verify(repository).setTorchEnabled(true)
    }

    @Test
    fun `detectionMode 多次更新后反映最终值`() = runTest {
        detectionModeFlow.value = DetectionMode.PRO
        detectionModeFlow.value = DetectionMode.FAST
        val result = viewModel.detectionMode.first()
        assertEquals(DetectionMode.FAST, result)
    }

    @Test
    fun `autoCapture 多次更新后反映最终值`() = runTest {
        autoCaptureFlow.value = false
        autoCaptureFlow.value = true
        autoCaptureFlow.value = false
        val result = viewModel.autoCapture.first()
        assertEquals(false, result)
    }

    @Test
    fun `captureDelay 多次更新后反映最终值`() = runTest {
        captureDelayFlow.value = 100
        captureDelayFlow.value = 200
        captureDelayFlow.value = 300
        val result = viewModel.captureDelay.first()
        assertEquals(300, result)
    }

    @Test
    fun `darkTheme 多次更新后反映最终值`() = runTest {
        darkThemeFlow.value = false
        darkThemeFlow.value = true
        darkThemeFlow.value = false
        val result = viewModel.darkTheme.first()
        assertEquals(false, result)
    }

    @Test
    fun `torchEnabled 多次更新后反映最终值`() = runTest {
        torchEnabledFlow.value = true
        torchEnabledFlow.value = false
        torchEnabledFlow.value = true
        val result = viewModel.torchEnabled.first()
        assertEquals(true, result)
    }

    @Test
    fun `detectionMode 与仓库数据保持一致`() = runTest {
        detectionModeFlow.value = DetectionMode.PRO
        assertEquals(DetectionMode.PRO, viewModel.detectionMode.first())

        detectionModeFlow.value = DetectionMode.FAST
        assertEquals(DetectionMode.FAST, viewModel.detectionMode.first())
    }

    @Test
    fun `autoCapture 与仓库数据保持一致`() = runTest {
        autoCaptureFlow.value = false
        assertEquals(false, viewModel.autoCapture.first())

        autoCaptureFlow.value = true
        assertEquals(true, viewModel.autoCapture.first())
    }

    @Test
    fun `captureDelay 与仓库数据保持一致`() = runTest {
        captureDelayFlow.value = 1000
        assertEquals(1000, viewModel.captureDelay.first())

        captureDelayFlow.value = 0
        assertEquals(0, viewModel.captureDelay.first())
    }

    @Test
    fun `darkTheme 与仓库数据保持一致`() = runTest {
        darkThemeFlow.value = false
        assertEquals(false, viewModel.darkTheme.first())

        darkThemeFlow.value = true
        assertEquals(true, viewModel.darkTheme.first())
    }

    @Test
    fun `torchEnabled 与仓库数据保持一致`() = runTest {
        torchEnabledFlow.value = true
        assertEquals(true, viewModel.torchEnabled.first())

        torchEnabledFlow.value = false
        assertEquals(false, viewModel.torchEnabled.first())
    }
}
