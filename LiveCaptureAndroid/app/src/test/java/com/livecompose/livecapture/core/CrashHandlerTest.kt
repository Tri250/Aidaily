package com.livecompose.livecapture.core

import org.junit.Assert.*
import org.junit.Test

/**
 * CrashHandler 单元测试
 */
class CrashHandlerTest {

    @Test
    fun `crashHandler formats crash info correctly`() {
        val crashInfo = CrashHandler.CrashInfo(
            timestamp = System.currentTimeMillis(),
            threadName = "main",
            exceptionMessage = "NullPointerException",
            stackTrace = "at com.example.Test.method(Test.kt:10)",
            deviceInfo = "Android 14 (SDK 34), Google Pixel 8"
        )

        assertNotNull(crashInfo.formattedTime)
        assertEquals("main", crashInfo.threadName)
        assertEquals("NullPointerException", crashInfo.exceptionMessage)
        assertTrue(crashInfo.stackTrace.isNotEmpty())
        assertEquals("Android 14 (SDK 34), Google Pixel 8", crashInfo.deviceInfo)
    }
}
