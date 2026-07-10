package com.livecompose.livecapture.core.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class MotionStabilityMonitorTest {

    @Test
    fun `MotionData default values are zero`() {
        val data = MotionStabilityMonitor.MotionData()
        assertEquals(0f, data.gyroX, 0.001f)
        assertEquals(0f, data.gyroY, 0.001f)
        assertEquals(0f, data.gyroZ, 0.001f)
        assertEquals(0f, data.accelX, 0.001f)
        assertEquals(0f, data.accelY, 0.001f)
        assertEquals(0f, data.accelZ, 0.001f)
    }

    @Test
    fun `MotionData stores sensor readings`() {
        val data = MotionStabilityMonitor.MotionData(
            gyroX = 0.1f, gyroY = 0.2f, gyroZ = 0.3f,
            accelX = 1.0f, accelY = 9.8f, accelZ = 0.5f
        )
        assertEquals(0.1f, data.gyroX, 0.001f)
        assertEquals(9.8f, data.accelY, 0.001f)
    }
}
