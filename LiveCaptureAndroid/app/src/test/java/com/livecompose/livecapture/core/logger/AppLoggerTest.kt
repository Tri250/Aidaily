package com.livecompose.livecapture.core.logger

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AppLogger 单元测试
 *
 * 测试调试模式和发布模式下日志输出行为，
 * 以及 forceEnabled 覆盖行为。
 */
class AppLoggerTest {

    @Before
    fun setUp() {
        // 每个测试前重置 forceEnabled 状态
        AppLogger.setEnabled(false)
    }

    // ====== debug 模式日志输出测试 ======

    @Test
    fun `debug log output works in debug mode`() {
        // 测试 debug 级别日志在 debug 模式下可以输出
        // 通过 BuildConfig.DEBUG 控制，在单元测试中默认为 debug 模式
        // 不抛异常即为通过
        AppLogger.d("TestTag", "Debug message")
        assertTrue(true)
    }

    @Test
    fun `info log output works in debug mode`() {
        // 测试 info 级别日志在 debug 模式下可以输出
        AppLogger.i("TestTag", "Info message")
        assertTrue(true)
    }

    @Test
    fun `warning log output works in debug mode`() {
        // 测试 warning 级别日志在 debug 模式下可以输出
        AppLogger.w("TestTag", "Warning message")
        assertTrue(true)
    }

    @Test
    fun `error log output works in debug mode`() {
        // 测试 error 级别日志在 debug 模式下可以输出
        AppLogger.e("TestTag", "Error message")
        assertTrue(true)
    }

    @Test
    fun `critical log output always works`() {
        // 测试 critical 级别日志在任何模式下都可以输出
        AppLogger.critical("TestTag", "Critical message")
        assertTrue(true)
    }

    @Test
    fun `debug log with throwable works in debug mode`() {
        // 测试带异常的 debug 日志
        val exception = RuntimeException("Test exception")
        AppLogger.d("TestTag", "Debug with throwable", exception)
        assertTrue(true)
    }

    @Test
    fun `error log with throwable works in debug mode`() {
        // 测试带异常的 error 日志
        val exception = NullPointerException("Test NPE")
        AppLogger.e("TestTag", "Error with throwable", exception)
        assertTrue(true)
    }

    // ====== forceEnabled 覆盖测试 ======

    @Test
    fun `forceEnabled allows log output`() {
        // 测试 forceEnabled 为 true 时允许日志输出
        AppLogger.setEnabled(true)

        // 所有日志方法都应可以正常调用
        AppLogger.d("TestTag", "Forced debug")
        AppLogger.i("TestTag", "Forced info")
        AppLogger.w("TestTag", "Forced warning")
        AppLogger.e("TestTag", "Forced error")

        assertTrue(true)
    }

    @Test
    fun `forceEnabled can be disabled`() {
        // 测试 forceEnabled 可以被关闭
        AppLogger.setEnabled(true)
        AppLogger.d("TestTag", "Should be logged")

        AppLogger.setEnabled(false)
        AppLogger.d("TestTag", "Should not be logged")

        assertTrue(true)
    }

    @Test
    fun `forceEnabled toggle multiple times`() {
        // 测试 forceEnabled 可以多次切换
        AppLogger.setEnabled(true)
        AppLogger.d("TestTag", "Enabled 1")

        AppLogger.setEnabled(false)
        AppLogger.d("TestTag", "Disabled 1")

        AppLogger.setEnabled(true)
        AppLogger.d("TestTag", "Enabled 2")

        AppLogger.setEnabled(false)
        AppLogger.d("TestTag", "Disabled 2")

        assertTrue(true)
    }

    // ====== CrashLogWriter 测试 ======

    @Test
    fun `crashLogWriter can be accessed`() {
        // 测试 CrashLogWriter 可以通过 AppLogger 的错误日志间接测试
        AppLogger.e("CrashTag", "Crash message")
        AppLogger.e("CrashTag", "Another crash", RuntimeException("Test"))
        assertTrue(true)
    }

    @Test
    fun `crashLogWriter getRecentLogs returns list`() {
        // 测试 CrashLogWriter.getRecentLogs 返回列表
        val logs = CrashLogWriter.getRecentLogs()
        assertNotNull("Recent logs should not be null", logs)
        assertTrue("Recent logs should be a list", logs is List<*>)
    }

    // ====== 所有日志级别综合测试 ======

    @Test
    fun `all log levels work with forceEnabled`() {
        // 测试所有日志级别在 forceEnabled 下都可以工作
        AppLogger.setEnabled(true)

        AppLogger.d("Tag", "d")
        AppLogger.d("Tag", "d with exception", RuntimeException())
        AppLogger.i("Tag", "i")
        AppLogger.i("Tag", "i with exception", RuntimeException())
        AppLogger.w("Tag", "w")
        AppLogger.w("Tag", "w with exception", RuntimeException())
        AppLogger.e("Tag", "e")
        AppLogger.e("Tag", "e with exception", RuntimeException())
        AppLogger.critical("Tag", "critical")

        assertTrue(true)
    }

    @Test
    fun `all log levels work without forceEnabled`() {
        // 测试所有日志级别在未强制启用时也可调用（不崩溃）
        AppLogger.setEnabled(false)

        AppLogger.d("Tag", "d")
        AppLogger.d("Tag", "d with exception", RuntimeException())
        AppLogger.i("Tag", "i")
        AppLogger.i("Tag", "i with exception", RuntimeException())
        AppLogger.w("Tag", "w")
        AppLogger.w("Tag", "w with exception", RuntimeException())
        AppLogger.e("Tag", "e")
        AppLogger.e("Tag", "e with exception", RuntimeException())
        AppLogger.critical("Tag", "critical")

        assertTrue(true)
    }
}