package com.livecompose.livecapture.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoRecordTest {

    @Test
    fun `PhotoRecord creates with required fields`() {
        val record = PhotoRecord(
            id = "test-id",
            filePath = "/photos/test.jpg",
            thumbPath = "/thumbs/test.jpg",
            width = 1080,
            height = 1440,
            timestamp = 1700000000000L
        )

        assertEquals("test-id", record.id)
        assertEquals("/photos/test.jpg", record.filePath)
        assertEquals(1080, record.width)
        assertEquals(1440, record.height)
        assertNull(record.iso)
        assertNull(record.cropRegion)
    }

    @Test
    fun `CropRegion stores normalized coordinates`() {
        val region = CropRegion(
            centerX = 0.5f,
            centerY = 0.5f,
            width = 0.8f,
            height = 0.6f
        )

        assertEquals(0.5f, region.centerX, 0.001f)
        assertEquals(0.8f, region.width, 0.001f)
    }

    @Test
    fun `ExifData has default values`() {
        val exif = ExifData()
        assertEquals("LiveCapture", exif.make)
        assertEquals("Android", exif.model)
        assertNull(exif.iso)
    }
}
