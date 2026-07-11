package com.livecompose.livecapture.presentation.settings

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

// 使用 Robolectric 运行，因为 ViewModel 需要 Android 环境
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    // 被模拟的仓库
    private lateinit var repository: SettingsRepository

    // 被测试的 ViewModel
    private lateinit var viewModel: SettingsViewModel

    // 为仓库的每个属性创建 MutableStateFlow，模拟上游数据流
    private val detectionModeFlow = MutableStateFlow("FAST")
    private val autoCaptureFlow = MutableStateFlow(true)
    private val captureDelayFlow = MutableStateFlow(0)
    private val darkThemeFlow = MutableStateFlow(true)
    private val torchEnabledFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        // 设置主调度器为 UnconfinedTestDispatcher，使 viewModelScope 中的协程立即执行
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // 创建仓库的 Mockito 模拟对象
        repository = mock()

        // 配置仓库属性的模拟返回值
        whenever(repository.detectionMode).thenReturn(detectionModeFlow)
        whenever(repository.autoCapture).thenReturn(autoCaptureFlow)
        whenever(repository.captureDelay).thenReturn(captureDelayFlow)
        whenever(repository.darkTheme).thenReturn(darkThemeFlow)
        whenever(repository.torchEnabled).thenReturn(torchEnabledFlow)

        // 使用模拟仓库直接构造 ViewModel
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        // 重置主调度器，避免影响其他测试
        Dispatchers.resetMain()
    }

    // ========== StateFlow 初始值测试 ==========

    @Test
    fun `detectionMode 初始值为 FAST`() = runTest {
        // stateIn 的默认值为 "FAST"
        val result = viewModel.detectionMode.first()
        assertEquals("FAST", result)
    }

    @Test
    fun `autoCapture 初始值为 true`() = runTest {
        // stateIn 的默认值为 true
        val result = viewModel.autoCapture.first()
        assertEquals(true, result)
    }

    @Test
    fun `captureDelay 初始值为 0`() = runTest {
        // stateIn 的默认值为 0
        val result = viewModel.captureDelay.first()
        assertEquals(0, result)
    }

    @Test
    fun `darkTheme 初始值为 true`() = runTest {
        // stateIn 的默认值为 true
        val result = viewModel.darkTheme.first()
        assertEquals(true, result)
    }

    @Test
    fun `torchEnabled 初始值为 false`() = runTest {
        // stateIn 的默认值为 false
        val result = viewModel.torchEnabled.first()
        assertEquals(false, result)
    }

    // ========== StateFlow 反映仓库数据变化测试 ==========

    @Test
    fun `detectionMode 反映仓库数据变化`() = runTest {
        // 更新仓库的 MutableStateFlow
        detectionModeFlow.value = "ACCURATE"
        // ViewModel 的 StateFlow 应该反映最新值
        val result = viewModel.detectionMode.first()
        assertEquals("ACCURATE", result)
    }

    @Test
    fun `autoCapture 反映仓库数据变化`() = runTest {
        // 更新仓库的 MutableStateFlow
        autoCaptureFlow.value = false
        // ViewModel 的 StateFlow 应该反映最新值
        val result = viewModel.autoCapture.first()
        assertEquals(false, result)
    }

    @Test
    fun `captureDelay 反映仓库数据变化`() = runTest {
        // 更新仓库的 MutableStateFlow
        captureDelayFlow.value = 500
        // ViewModel 的 StateFlow 应该反映最新值
        val result = viewModel.captureDelay.first()
        assertEquals(500, result)
    }

    @Test
    fun `darkTheme 反映仓库数据变化`() = runTest {
        // 更新仓库的 MutableStateFlow
        darkThemeFlow.value = false
        // ViewModel 的 StateFlow 应该反映最新值
        val result = viewModel.darkTheme.first()
        assertEquals(false, result)
    }

    @Test
    fun `torchEnabled 反映仓库数据变化`() = runTest {
        // 更新仓库的 MutableStateFlow
        torchEnabledFlow.value = true
        // ViewModel 的 StateFlow 应该反映最新值
        val result = viewModel.torchEnabled.first()
        assertEquals(true, result)
    }

    // ========== setter 方法委托仓库测试 ==========

    @Test
    fun `setDetectionMode 委托给仓库`() = runTest {
        // 调用 ViewModel 的 setter 方法
        viewModel.setDetectionMode("ACCURATE")
        // 验证仓库的对应方法被调用，且参数正确
        verify(repository).setDetectionMode("ACCURATE")
    }

    @Test
    fun `setAutoCapture 委托给仓库`() = runTest {
        // 调用 ViewModel 的 setter 方法
        viewModel.setAutoCapture(false)
        // 验证仓库的对应方法被调用，且参数正确
        verify(repository).setAutoCapture(false)
    }

    @Test
    fun `setCaptureDelay 委托给仓库`() = runTest {
        // 调用 ViewModel 的 setter 方法
        viewModel.setCaptureDelay(300)
        // 验证仓库的对应方法被调用，且参数正确
        verify(repository).setCaptureDelay(300)
    }

    @Test
    fun `setDarkTheme 委托给仓库`() = runTest {
        // 调用 ViewModel 的 setter 方法
        viewModel.setDarkTheme(false)
        // 验证仓库的对应方法被调用，且参数正确
        verify(repository).setDarkTheme(false)
    }

    @Test
    fun `setTorchEnabled 委托给仓库`() = runTest {
        // 调用 ViewModel 的 setter 方法
        viewModel.setTorchEnabled(true)
        // 验证仓库的对应方法被调用，且参数正确
        verify(repository).setTorchEnabled(true)
    }

    // ========== StateFlow 多次更新测试 ==========

    @Test
    fun `detectionMode 多次更新后反映最终值`() = runTest {
        // 模拟多次更新仓库数据
        detectionModeFlow.value = "ACCURATE"
        detectionModeFlow.value = "BALANCED"
        detectionModeFlow.value = "FAST"
        // ViewModel 的 StateFlow 应反映最终值
        val result = viewModel.detectionMode.first()
        assertEquals("FAST", result)
    }

    @Test
    fun `autoCapture 多次更新后反映最终值`() = runTest {
        // 模拟多次更新仓库数据
        autoCaptureFlow.value = false
        autoCaptureFlow.value = true
        autoCaptureFlow.value = false
        // ViewModel 的 StateFlow 应反映最终值
        val result = viewModel.autoCapture.first()
        assertEquals(false, result)
    }

    @Test
    fun `captureDelay 多次更新后反映最终值`() = runTest {
        // 模拟多次更新仓库数据
        captureDelayFlow.value = 100
        captureDelayFlow.value = 200
        captureDelayFlow.value = 300
        // ViewModel 的 StateFlow 应反映最终值
        val result = viewModel.captureDelay.first()
        assertEquals(300, result)
    }

    @Test
    fun `darkTheme 多次更新后反映最终值`() = runTest {
        // 模拟多次更新仓库数据
        darkThemeFlow.value = false
        darkThemeFlow.value = true
        darkThemeFlow.value = false
        // ViewModel 的 StateFlow 应反映最终值
        val result = viewModel.darkTheme.first()
        assertEquals(false, result)
    }

    @Test
    fun `torchEnabled 多次更新后反映最终值`() = runTest {
        // 模拟多次更新仓库数据
        torchEnabledFlow.value = true
        torchEnabledFlow.value = false
        torchEnabledFlow.value = true
        // ViewModel 的 StateFlow 应反映最终值
        val result = viewModel.torchEnabled.first()
        assertEquals(true, result)
    }

    // ========== setter 方法参数传递验证 ==========

    @Test
    fun `setDetectionMode 传递不同模式值`() = runTest {
        // 验证传递 "BALANCED" 模式
        viewModel.setDetectionMode("BALANCED")
        verify(repository).setDetectionMode("BALANCED")
    }

    @Test
    fun `setAutoCapture 传递 true 值`() = runTest {
        // autoCapture 默认为 true，验证显式传递 true
        viewModel.setAutoCapture(true)
        verify(repository).setAutoCapture(true)
    }

    @Test
    fun `setCaptureDelay 传递零值`() = runTest {
        // 验证传递延迟为 0
        viewModel.setCaptureDelay(0)
        verify(repository).setCaptureDelay(0)
    }

    @Test
    fun `setDarkTheme 传递 true 值`() = runTest {
        // darkTheme 默认为 true，验证显式传递 true
        viewModel.setDarkTheme(true)
        verify(repository).setDarkTheme(true)
    }

    @Test
    fun `setTorchEnabled 传递 false 值`() = runTest {
        // torchEnabled 默认为 false，验证显式传递 false
        viewModel.setTorchEnabled(false)
        verify(repository).setTorchEnabled(false)
    }

    // ========== StateFlow 与仓库数据一致性测试 ==========

    @Test
    fun `detectionMode 与仓库数据保持一致`() = runTest {
        // 仓库发出非默认值
        detectionModeFlow.value = "ACCURATE"
        assertEquals("ACCURATE", viewModel.detectionMode.first())

        // 仓库恢复默认值
        detectionModeFlow.value = "FAST"
        assertEquals("FAST", viewModel.detectionMode.first())
    }

    @Test
    fun `autoCapture 与仓库数据保持一致`() = runTest {
        // 仓库发出非默认值
        autoCaptureFlow.value = false
        assertEquals(false, viewModel.autoCapture.first())

        // 仓库恢复默认值
        autoCaptureFlow.value = true
        assertEquals(true, viewModel.autoCapture.first())
    }

    @Test
    fun `captureDelay 与仓库数据保持一致`() = runTest {
        // 仓库发出非默认值
        captureDelayFlow.value = 1000
        assertEquals(1000, viewModel.captureDelay.first())

        // 仓库恢复默认值
        captureDelayFlow.value = 0
        assertEquals(0, viewModel.captureDelay.first())
    }

    @Test
    fun `darkTheme 与仓库数据保持一致`() = runTest {
        // 仓库发出非默认值
        darkThemeFlow.value = false
        assertEquals(false, viewModel.darkTheme.first())

        // 仓库恢复默认值
        darkThemeFlow.value = true
        assertEquals(true, viewModel.darkTheme.first())
    }

    @Test
    fun `torchEnabled 与仓库数据保持一致`() = runTest {
        // 仓库发出非默认值
        torchEnabledFlow.value = true
        assertEquals(true, viewModel.torchEnabled.first())

        // 仓库恢复默认值
        torchEnabledFlow.value = false
        assertEquals(false, viewModel.torchEnabled.first())
    }
}
