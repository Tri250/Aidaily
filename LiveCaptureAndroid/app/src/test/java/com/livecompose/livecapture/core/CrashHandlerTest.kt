package com.livecompose.livecapture.core

import org.junit.Assert.*
import org.junit.Test

/**
 * CrashHandler 单元测试
 */
class CrashHandlerTest {

    @Test
    fun `crashHandler formats crash info correctly`() {
        val crashInfo = CrashInfo(
            timestamp = System.currentTimeMillis(),
            threadName = "main",
            exceptionMessage = "NullPointerException",
            stackTrace = "at com.example.Test.method(Test.kt:10)"
        )

        assertNotNull(crashInfo.formattedTime)
        assertEquals("main", crashInfo.threadName)
        assertEquals("NullPointerException", crashInfo.exceptionMessage)
        assertTrue(crashInfo.stackTrace.isNotEmpty())
    }
}
