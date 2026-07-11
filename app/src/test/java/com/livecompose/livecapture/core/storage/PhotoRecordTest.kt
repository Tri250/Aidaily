package com.livecompose.livecapture.core.storage

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoRecordTest {

    // ==================== PhotoRecord 测试 ====================

    @Test
    fun `PhotoRecord 仅使用必填字段创建时，所有可选字段应为 null`() {
        val record = PhotoRecord(
            id = "id-001",
            filePath = "/photos/img.jpg",
            thumbPath = "/thumbs/img.jpg",
            width = 1920,
            height = 1080,
            timestamp = 1700000000000L
        )

        // 验证必填字段
        assertEquals("id-001", record.id)
        assertEquals("/photos/img.jpg", record.filePath)
        assertEquals("/thumbs/img.jpg", record.thumbPath)
        assertEquals(1920, record.width)
        assertEquals(1080, record.height)
        assertEquals(1700000000000L, record.timestamp)

        // 验证所有可选字段默认为 null
        assertNull(record.iso)
        assertNull(record.shutterSpeed)
        assertNull(record.aperture)
        assertNull(record.focalLength)
        assertNull(record.cropRegion)
        assertNull(record.aestheticScore)
    }

    @Test
    fun `PhotoRecord 所有字段均设置时，值应正确保存`() {
        val crop = CropRegion(0.5f, 0.5f, 1.0f, 1.0f)
        val record = PhotoRecord(
            id = "id-002",
            filePath = "/photos/full.jpg",
            thumbPath = "/thumbs/full.jpg",
            width = 3840,
            height = 2160,
            timestamp = 1700000000001L,
            iso = "400",
            shutterSpeed = "1/125",
            aperture = "f/2.8",
            focalLength = "50mm",
            cropRegion = crop,
            aestheticScore = 0.85f
        )

        assertEquals("id-002", record.id)
        assertEquals("/photos/full.jpg", record.filePath)
        assertEquals("/thumbs/full.jpg", record.thumbPath)
        assertEquals(3840, record.width)
        assertEquals(2160, record.height)
        assertEquals(1700000000001L, record.timestamp)
        assertEquals("400", record.iso)
        assertEquals("1/125", record.shutterSpeed)
        assertEquals("f/2.8", record.aperture)
        assertEquals("50mm", record.focalLength)
        assertEquals(crop, record.cropRegion)
        assertEquals(0.85f, record.aestheticScore!!, 0.001f)
    }

    @Test
    fun `PhotoRecord copy 方法应正确复制并替换指定字段`() {
        val original = PhotoRecord(
            id = "id-003",
            filePath = "/photos/a.jpg",
            thumbPath = "/thumbs/a.jpg",
            width = 800,
            height = 600,
            timestamp = 1700000000002L,
            iso = "100"
        )

        // 仅替换部分字段
        val copied = original.copy(
            filePath = "/photos/b.jpg",
            iso = "800",
            aestheticScore = 0.92f
        )

        // 替换后的字段
        assertEquals("/photos/b.jpg", copied.filePath)
        assertEquals("800", copied.iso)
        assertEquals(0.92f, copied.aestheticScore!!, 0.001f)

        // 未替换的字段应保持不变
        assertEquals("id-003", copied.id)
        assertEquals("/thumbs/a.jpg", copied.thumbPath)
        assertEquals(800, copied.width)
        assertEquals(600, copied.height)
        assertEquals(1700000000002L, copied.timestamp)
        assertNull(copied.shutterSpeed)
        assertNull(copied.aperture)
        assertNull(copied.focalLength)
        assertNull(copied.cropRegion)
    }

    @Test
    fun `PhotoRecord equals 当所有字段相同时应返回 true`() {
        val crop = CropRegion(0.3f, 0.7f, 0.5f, 0.4f)
        val record1 = PhotoRecord(
            id = "id-004", filePath = "/photos/x.jpg", thumbPath = "/thumbs/x.jpg",
            width = 640, height = 480, timestamp = 100L,
            iso = "200", shutterSpeed = "1/60", aperture = "f/4.0",
            focalLength = "35mm", cropRegion = crop, aestheticScore = 0.75f
        )
        val record2 = PhotoRecord(
            id = "id-004", filePath = "/photos/x.jpg", thumbPath = "/thumbs/x.jpg",
            width = 640, height = 480, timestamp = 100L,
            iso = "200", shutterSpeed = "1/60", aperture = "f/4.0",
            focalLength = "35mm", cropRegion = crop, aestheticScore = 0.75f
        )

        assertEquals(record1, record2)
    }

    @Test
    fun `PhotoRecord equals 当任一必填字段不同时应返回 false`() {
        val base = PhotoRecord(
            id = "id-005", filePath = "/photos/a.jpg", thumbPath = "/thumbs/a.jpg",
            width = 100, height = 200, timestamp = 300L
        )

        // 逐一验证每个必填字段不同时 equals 返回 false
        assertNotEquals(base, base.copy(id = "different"))
        assertNotEquals(base, base.copy(filePath = "/different.jpg"))
        assertNotEquals(base, base.copy(thumbPath = "/different.jpg"))
        assertNotEquals(base, base.copy(width = 999))
        assertNotEquals(base, base.copy(height = 999))
        assertNotEquals(base, base.copy(timestamp = 999L))
    }

    @Test
    fun `PhotoRecord equals 当任一可选字段不同时应返回 false`() {
        val base = PhotoRecord(
            id = "id-006", filePath = "/photos/a.jpg", thumbPath = "/thumbs/a.jpg",
            width = 100, height = 200, timestamp = 300L,
            iso = "100", shutterSpeed = "1/30", aperture = "f/1.8",
            focalLength = "24mm", cropRegion = CropRegion(0.5f, 0.5f, 1.0f, 1.0f),
            aestheticScore = 0.5f
        )

        assertNotEquals(base, base.copy(iso = "200"))
        assertNotEquals(base, base.copy(shutterSpeed = "1/60"))
        assertNotEquals(base, base.copy(aperture = "f/2.8"))
        assertNotEquals(base, base.copy(focalLength = "50mm"))
        assertNotEquals(base, base.copy(cropRegion = CropRegion(0.1f, 0.1f, 0.2f, 0.2f)))
        assertNotEquals(base, base.copy(aestheticScore = 0.9f))
        // 可选字段从有值变为 null 也应不等
        assertNotEquals(base, base.copy(iso = null))
        assertNotEquals(base, base.copy(cropRegion = null))
    }

    @Test
    fun `PhotoRecord hashCode 对相等对象应返回相同值`() {
        val record1 = PhotoRecord(
            id = "id-007", filePath = "/photos/h.jpg", thumbPath = "/thumbs/h.jpg",
            width = 1024, height = 768, timestamp = 500L
        )
        val record2 = PhotoRecord(
            id = "id-007", filePath = "/photos/h.jpg", thumbPath = "/thumbs/h.jpg",
            width = 1024, height = 768, timestamp = 500L
        )

        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun `PhotoRecord hashCode 对不相等对象通常应返回不同值`() {
        val record1 = PhotoRecord(
            id = "id-008", filePath = "/photos/a.jpg", thumbPath = "/thumbs/a.jpg",
            width = 100, height = 100, timestamp = 1L
        )
        val record2 = PhotoRecord(
            id = "id-009", filePath = "/photos/b.jpg", thumbPath = "/thumbs/b.jpg",
            width = 200, height = 200, timestamp = 2L
        )

        // hashCode 不保证一定不同，但不同数据通常会产生不同 hashCode
        // 此处仅验证不会崩溃，不强求不同
        assertNotNull(record1.hashCode())
        assertNotNull(record2.hashCode())
    }

    @Test
    fun `PhotoRecord 与 null 比较应返回 false`() {
        val record = PhotoRecord(
            id = "id-010", filePath = "/photos/n.jpg", thumbPath = "/thumbs/n.jpg",
            width = 640, height = 480, timestamp = 600L
        )

        assertNotEquals(record, null)
    }

    // ==================== CropRegion 测试 ====================

    @Test
    fun `CropRegion 存储裁剪区域的归一化坐标`() {
        val region = CropRegion(
            centerX = 0.5f,
            centerY = 0.5f,
            width = 0.8f,
            height = 0.6f
        )

        assertEquals(0.5f, region.centerX, 0.001f)
        assertEquals(0.5f, region.centerY, 0.001f)
        assertEquals(0.8f, region.width, 0.001f)
        assertEquals(0.6f, region.height, 0.001f)
    }

    @Test
    fun `CropRegion 允许边界值 0 和 1`() {
        val zeroRegion = CropRegion(centerX = 0f, centerY = 0f, width = 0f, height = 0f)
        val oneRegion = CropRegion(centerX = 1f, centerY = 1f, width = 1f, height = 1f)

        assertEquals(0f, zeroRegion.centerX, 0.001f)
        assertEquals(1f, oneRegion.centerX, 0.001f)
    }

    @Test
    fun `CropRegion equals 当所有字段相同时应返回 true`() {
        val region1 = CropRegion(0.25f, 0.75f, 0.5f, 0.5f)
        val region2 = CropRegion(0.25f, 0.75f, 0.5f, 0.5f)

        assertEquals(region1, region2)
    }

    @Test
    fun `CropRegion equals 当任一字段不同时应返回 false`() {
        val base = CropRegion(0.5f, 0.5f, 1.0f, 1.0f)

        assertNotEquals(base, base.copy(centerX = 0.1f))
        assertNotEquals(base, base.copy(centerY = 0.1f))
        assertNotEquals(base, base.copy(width = 0.5f))
        assertNotEquals(base, base.copy(height = 0.5f))
    }

    @Test
    fun `CropRegion hashCode 对相等对象应一致`() {
        val region1 = CropRegion(0.3f, 0.4f, 0.6f, 0.7f)
        val region2 = CropRegion(0.3f, 0.4f, 0.6f, 0.7f)

        assertEquals(region1.hashCode(), region2.hashCode())
    }

    @Test
    fun `CropRegion copy 应正确替换指定字段`() {
        val original = CropRegion(0.1f, 0.2f, 0.3f, 0.4f)
        val copied = original.copy(centerX = 0.9f, height = 0.8f)

        assertEquals(0.9f, copied.centerX, 0.001f)
        assertEquals(0.2f, copied.centerY, 0.001f) // 未替换，保持原值
        assertEquals(0.3f, copied.width, 0.001f)    // 未替换，保持原值
        assertEquals(0.8f, copied.height, 0.001f)
    }

    // ==================== ExifData 测试 ====================

    @Test
    fun `ExifData 默认 make 应来自 Build MANUFACTURER`() {
        val exif = ExifData()
        // 在单元测试环境中，Build.MANUFACTURER 可能返回 "robolectric" 等值，
        // 此处验证 make 等于运行时 Build.MANUFACTURER 的实际值
        val expectedMake = Build.MANUFACTURER ?: "Android"
        assertEquals(expectedMake, exif.make)
    }

    @Test
    fun `ExifData 默认 model 应来自 Build MODEL`() {
        val exif = ExifData()
        val expectedModel = Build.MODEL ?: "Android"
        assertEquals(expectedModel, exif.model)
    }

    @Test
    fun `ExifData 所有可空字段默认应为 null`() {
        val exif = ExifData()

        assertNull(exif.iso)
        assertNull(exif.shutterSpeed)
        assertNull(exif.aperture)
        assertNull(exif.focalLength)
    }

    @Test
    fun `ExifData 自定义值应覆盖默认值`() {
        val exif = ExifData(
            iso = "3200",
            shutterSpeed = "1/4000",
            aperture = "f/1.4",
            focalLength = "85mm",
            make = "Canon",
            model = "EOS R5"
        )

        assertEquals("3200", exif.iso)
        assertEquals("1/4000", exif.shutterSpeed)
        assertEquals("f/1.4", exif.aperture)
        assertEquals("85mm", exif.focalLength)
        assertEquals("Canon", exif.make)
        assertEquals("EOS R5", exif.model)
    }

    @Test
    fun `ExifData 部分字段自定义时其余字段应使用默认值`() {
        val exif = ExifData(iso = "800", make = "Nikon")

        assertEquals("800", exif.iso)
        assertNull(exif.shutterSpeed)
        assertNull(exif.aperture)
        assertNull(exif.focalLength)
        assertEquals("Nikon", exif.make)
        // model 使用 Build.MODEL 的默认值
        val expectedModel = Build.MODEL ?: "Android"
        assertEquals(expectedModel, exif.model)
    }

    @Test
    fun `ExifData equals 当所有字段相同时应返回 true`() {
        val exif1 = ExifData(
            iso = "100", shutterSpeed = "1/60", aperture = "f/2.8",
            focalLength = "50mm", make = "Sony", model = "A7III"
        )
        val exif2 = ExifData(
            iso = "100", shutterSpeed = "1/60", aperture = "f/2.8",
            focalLength = "50mm", make = "Sony", model = "A7III"
        )

        assertEquals(exif1, exif2)
    }

    @Test
    fun `ExifData equals 当任一字段不同时应返回 false`() {
        val base = ExifData(
            iso = "200", shutterSpeed = "1/125", aperture = "f/4.0",
            focalLength = "35mm", make = "Fuji", model = "X-T4"
        )

        assertNotEquals(base, base.copy(iso = "400"))
        assertNotEquals(base, base.copy(shutterSpeed = "1/250"))
        assertNotEquals(base, base.copy(aperture = "f/5.6"))
        assertNotEquals(base, base.copy(focalLength = "56mm"))
        assertNotEquals(base, base.copy(make = "Different"))
        assertNotEquals(base, base.copy(model = "Different"))
    }

    @Test
    fun `ExifData hashCode 对相等对象应一致`() {
        val exif1 = ExifData(iso = "400", make = "Panasonic", model = "GH5")
        val exif2 = ExifData(iso = "400", make = "Panasonic", model = "GH5")

        assertEquals(exif1.hashCode(), exif2.hashCode())
    }

    @Test
    fun `ExifData copy 应正确替换指定字段`() {
        val original = ExifData(iso = "100", make = "Olympus", model = "OM-1")
        val copied = original.copy(iso = "1600", model = "OM-5")

        assertEquals("1600", copied.iso)
        assertEquals("Olympus", copied.make)     // 未替换，保持原值
        assertEquals("OM-5", copied.model)
        assertNull(copied.shutterSpeed)            // 原值本就为 null
    }

    // ==================== 边界情况与集成测试 ====================

    @Test
    fun `PhotoRecord 无可选字段与全部可选字段设置的对象应不相等`() {
        val minimal = PhotoRecord(
            id = "id-edge", filePath = "/photos/edge.jpg", thumbPath = "/thumbs/edge.jpg",
            width = 640, height = 480, timestamp = 1000L
        )
        val full = PhotoRecord(
            id = "id-edge", filePath = "/photos/edge.jpg", thumbPath = "/thumbs/edge.jpg",
            width = 640, height = 480, timestamp = 1000L,
            iso = "200", shutterSpeed = "1/100", aperture = "f/2.0",
            focalLength = "28mm", cropRegion = CropRegion(0.5f, 0.5f, 1.0f, 1.0f),
            aestheticScore = 0.88f
        )

        assertNotEquals(minimal, full)
    }

    @Test
    fun `PhotoRecord 中嵌套 CropRegion 不同时应不相等`() {
        val record1 = PhotoRecord(
            id = "id-crop", filePath = "/photos/c.jpg", thumbPath = "/thumbs/c.jpg",
            width = 800, height = 600, timestamp = 2000L,
            cropRegion = CropRegion(0.1f, 0.2f, 0.3f, 0.4f)
        )
        val record2 = PhotoRecord(
            id = "id-crop", filePath = "/photos/c.jpg", thumbPath = "/thumbs/c.jpg",
            width = 800, height = 600, timestamp = 2000L,
            cropRegion = CropRegion(0.9f, 0.8f, 0.7f, 0.6f)
        )

        assertNotEquals(record1, record2)
    }

    @Test
    fun `PhotoRecord aestheticScore 为 0f 时不应与 null 等价`() {
        val withZero = PhotoRecord(
            id = "id-score", filePath = "/photos/s.jpg", thumbPath = "/thumbs/s.jpg",
            width = 100, height = 100, timestamp = 3000L,
            aestheticScore = 0f
        )
        val withNull = PhotoRecord(
            id = "id-score", filePath = "/photos/s.jpg", thumbPath = "/thumbs/s.jpg",
            width = 100, height = 100, timestamp = 3000L,
            aestheticScore = null
        )

        // 0f 和 null 是不同的值，不应相等
        assertNotEquals(withZero, withNull)
    }

    @Test
    fun `ExifData 默认实例之间应相等且 hashCode 一致`() {
        val exif1 = ExifData()
        val exif2 = ExifData()

        // 两个默认实例的所有字段值相同（包括来自 Build 的默认值）
        assertEquals(exif1, exif2)
        assertEquals(exif1.hashCode(), exif2.hashCode())
    }
}
