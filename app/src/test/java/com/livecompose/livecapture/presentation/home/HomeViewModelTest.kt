package com.livecompose.livecapture.presentation.home

import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.storage.PhotoStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class HomeViewModelTest {

    // 模拟的存储服务
    private lateinit var storageService: PhotoStorageService

    // 测试作用域与调度器，用于控制协程执行
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // 被测试的 ViewModel
    private lateinit var viewModel: HomeViewModel

    // 测试用的示例数据
    private val sampleRecord = PhotoRecord(
        id = "record-1",
        filePath = "/storage/emulated/0/LiveCapture/photos/test1.jpg",
        thumbPath = "/storage/emulated/0/LiveCapture/photos/thumbs/thumb_test1.jpg",
        width = 1080,
        height = 1440,
        timestamp = 1700000000000L
    )

    private val sampleRecord2 = PhotoRecord(
        id = "record-2",
        filePath = "/storage/emulated/0/LiveCapture/photos/test2.jpg",
        thumbPath = "/storage/emulated/0/LiveCapture/photos/thumbs/thumb_test2.jpg",
        width = 720,
        height = 960,
        timestamp = 1700000001000L
    )

    @Before
    fun setUp() {
        // 设置主调度器为测试调度器，以便控制协程执行时机
        Dispatchers.setMain(testDispatcher)
        // 创建模拟的存储服务
        storageService = mock()
    }

    @After
    fun tearDown() {
        // 恢复主调度器
        Dispatchers.resetMain()
    }

    // ========== init 初始化测试 ==========

    @Test
    fun `init 时自动调用 loadRecords 加载记录`() = testScope.runTest {
        // 预设存储服务返回空列表
        whenever(storageService.getAllRecords()).thenReturn(emptyList())

        // 创建 ViewModel，init 块会触发 loadRecords
        viewModel = HomeViewModel(storageService)

        // 推进协程执行
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证 getAllRecords 被调用，说明 loadRecords 已执行
        verify(storageService).getAllRecords()
    }

    @Test
    fun `init 时 loadRecords 加载到的记录会更新到 records StateFlow`() = testScope.runTest {
        // 预设存储服务返回两条记录
        val records = listOf(sampleRecord, sampleRecord2)
        whenever(storageService.getAllRecords()).thenReturn(records)

        // 创建 ViewModel
        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证 records StateFlow 包含正确的数据
        val result = viewModel.records.first()
        assertEquals(records, result)
    }

    // ========== loadRecords 测试 ==========

    @Test
    fun `loadRecords 从存储服务获取记录并更新 StateFlow`() = testScope.runTest {
        // 初始化时返回空列表
        whenever(storageService.getAllRecords()).thenReturn(emptyList())
        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 设置后续调用返回两条记录
        whenever(storageService.getAllRecords()).thenReturn(listOf(sampleRecord, sampleRecord2))

        // 手动调用 loadRecords
        viewModel.loadRecords()
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证 StateFlow 已更新
        val result = viewModel.records.first()
        assertEquals(listOf(sampleRecord, sampleRecord2), result)
    }

    @Test
    fun `loadRecords 当没有记录时返回空列表`() = testScope.runTest {
        // 预设存储服务返回空列表
        whenever(storageService.getAllRecords()).thenReturn(emptyList())

        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证 records StateFlow 为空列表
        val result = viewModel.records.first()
        assertEquals(emptyList<PhotoRecord>(), result)
    }

    @Test
    fun `loadRecords 存储服务抛出异常时 StateFlow 保持为空列表`() = testScope.runTest {
        // 预设存储服务抛出异常
        whenever(storageService.getAllRecords()).thenThrow(RuntimeException("存储读取失败"))

        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 由于异常发生在协程中，StateFlow 仍为初始空列表
        val result = viewModel.records.first()
        assertEquals(emptyList<PhotoRecord>(), result)
    }

    // ========== deleteRecord 测试 ==========

    @Test
    fun `deleteRecord 调用存储服务的 deleteRecordAsync`() = testScope.runTest {
        // 初始化时存储服务有两条记录
        whenever(storageService.getAllRecords())
            .thenReturn(listOf(sampleRecord, sampleRecord2))  // 第一次调用（init）
            .thenReturn(listOf(sampleRecord2))                 // 第二次调用（删除后刷新）

        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 执行删除操作
        viewModel.deleteRecord(sampleRecord)
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证存储服务的 deleteRecordAsync 被正确调用
        verify(storageService).deleteRecordAsync(sampleRecord)
    }

    @Test
    fun `deleteRecord 删除后刷新记录列表`() = testScope.runTest {
        // 设置多次调用的返回值
        whenever(storageService.getAllRecords())
            .thenReturn(listOf(sampleRecord, sampleRecord2))  // init 加载
            .thenReturn(listOf(sampleRecord2))                 // 删除后刷新

        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 确认初始状态
        assertEquals(listOf(sampleRecord, sampleRecord2), viewModel.records.first())

        // 执行删除
        viewModel.deleteRecord(sampleRecord)
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证删除后记录已更新，只剩 record2
        val result = viewModel.records.first()
        assertEquals(listOf(sampleRecord2), result)
    }

    @Test
    fun `deleteRecord 删除最后一条记录后列表为空`() = testScope.runTest {
        // 只有一条记录
        whenever(storageService.getAllRecords())
            .thenReturn(listOf(sampleRecord))  // init 加载
            .thenReturn(emptyList())            // 删除后刷新

        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 确认初始状态
        assertEquals(listOf(sampleRecord), viewModel.records.first())

        // 执行删除
        viewModel.deleteRecord(sampleRecord)
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证删除后列表为空
        val result = viewModel.records.first()
        assertEquals(emptyList<PhotoRecord>(), result)
    }

    @Test
    fun `deleteRecord 删除不存在的记录后列表不变`() = testScope.runTest {
        // 不存在的记录
        val nonExistentRecord = sampleRecord.copy(id = "non-existent")

        whenever(storageService.getAllRecords())
            .thenReturn(listOf(sampleRecord, sampleRecord2))  // init 加载
            .thenReturn(listOf(sampleRecord, sampleRecord2))  // 删除后刷新（存储服务未真正删除，返回相同列表）

        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 执行删除不存在的记录
        viewModel.deleteRecord(nonExistentRecord)
        testDispatcher.scheduler.advanceUntilIdle()

        // 仍然调用 deleteRecordAsync
        verify(storageService).deleteRecordAsync(nonExistentRecord)

        // 列表不变
        val result = viewModel.records.first()
        assertEquals(listOf(sampleRecord, sampleRecord2), result)
    }

    // ========== records StateFlow 发射测试 ==========

    @Test
    fun `records StateFlow 初始值为空列表`() = testScope.runTest {
        // 不设置 getAllRecords 的返回值，让 init 中的协程尚未执行
        whenever(storageService.getAllRecords()).thenReturn(emptyList())

        viewModel = HomeViewModel(storageService)

        // 在协程执行前，StateFlow 应为初始空列表
        assertEquals(emptyList<PhotoRecord>(), viewModel.records.value)
    }

    @Test
    fun `records StateFlow 在 loadRecords 后发射正确的值`() = testScope.runTest {
        whenever(storageService.getAllRecords())
            .thenReturn(listOf(sampleRecord))               // init 加载
            .thenReturn(listOf(sampleRecord, sampleRecord2)) // 再次加载

        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()

        // 第一次加载后
        assertEquals(listOf(sampleRecord), viewModel.records.first())

        // 再次调用 loadRecords
        viewModel.loadRecords()
        testDispatcher.scheduler.advanceUntilIdle()

        // 第二次加载后
        assertEquals(listOf(sampleRecord, sampleRecord2), viewModel.records.first())
    }

    @Test
    fun `records StateFlow 多次更新后发射最新值`() = testScope.runTest {
        whenever(storageService.getAllRecords())
            .thenReturn(listOf(sampleRecord))                            // init 加载
            .thenReturn(listOf(sampleRecord, sampleRecord2))             // 第二次加载
            .thenReturn(listOf(sampleRecord2))                           // 第三次加载

        viewModel = HomeViewModel(storageService)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(sampleRecord), viewModel.records.value)

        viewModel.loadRecords()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(sampleRecord, sampleRecord2), viewModel.records.value)

        viewModel.loadRecords()
        testDispatcher.scheduler.advanceUntilIdle()

        // 最终值为最近一次更新的结果
        assertEquals(listOf(sampleRecord2), viewModel.records.value)
    }
}
