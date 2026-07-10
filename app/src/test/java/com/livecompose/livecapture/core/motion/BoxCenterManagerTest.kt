package com.livecompose.livecapture.core.motion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoxCenterManagerTest {

    private lateinit var manager: BoxCenterManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager = BoxCenterManager(context)
        manager.setScreenSize(720f, 1280f)
    }

    @Test
    fun `initial state has no track point`() {
        assertNull(manager.trackPoint.value)
        assertFalse(manager.isAligned.value)
        assertEquals(0f, manager.alignmentProgress.value, 0.001f)
    }

    @Test
    fun `update from detection sets track point`() {
        val motionData = MotionStabilityMonitor.MotionData(
            gyroX = 0f, gyroY = 0f, gyroZ = 0f,
            accelX = 0f, accelY = 9.8f, accelZ = 0f
        )

        manager.updateFromDetection(360f, 640f, motionData)

        assertNotNull(manager.trackPoint.value)
    }

    @Test
    fun `reset clears all state`() {
        val motionData = MotionStabilityMonitor.MotionData()
        manager.updateFromDetection(360f, 640f, motionData)

        manager.reset()

        assertNull(manager.trackPoint.value)
        assertFalse(manager.isAligned.value)
        assertEquals(0f, manager.alignmentProgress.value, 0.001f)
    }

    @Test
    fun `center detection achieves high alignment progress`() {
        val motionData = MotionStabilityMonitor.MotionData(
            gyroX = 0f, gyroY = 0f, gyroZ = 0f
        )

        // 检测中心 = 屏幕中心 (360, 640)
        manager.updateFromDetection(360f, 640f, motionData)

        // 进度应接近 1.0
        assertTrue(manager.alignmentProgress.value > 0.5f)
    }
}
