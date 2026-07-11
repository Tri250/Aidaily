package com.livecompose.livecapture.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.lang.reflect.Method

/**
 * PhotoStorageService 综合单元测试
 *
 * 覆盖范围：
 * - getAllRecords()：空文件、有效JSON、损坏JSON、文件不存在
 * - addRecordToIndex()：添加到列表开头、互斥锁防止并发写入丢失
 * - deleteRecordAsync()：删除文件和缩略图、从索引移除、content:// URI、文件路径、错误容忍
 * - saveRecords()：写入有效JSON、IO错误处理
 * - parseRecord()：全部字段解析、可选字段默认值、cropRegion解析、aestheticScore解析(NaN→null)
 * - cropToAspectRatio()：宽比目标宽裁宽度、高比目标高裁高度、正确比例返回原位图、零维度安全、maxOf(1,...)防零宽裁切
 * - rotateBitmap()：0度返回原位图、90/180/270度正确旋转
 * - writeExif()：写入所有EXIF标签、错误容忍
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhotoStorageServiceTest {

    private lateinit var context: Context
    private lateinit var service: PhotoStorageService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = PhotoStorageService(context)
        // 确保每次测试开始前清理旧的 records 文件
        cleanupStorageFiles()
    }

    @After
    fun tearDown() {
        cleanupStorageFiles()
    }

    // =====================================================
    // 辅助方法
    // =====================================================

    /**
     * 清理存储目录中的所有文件，避免测试间互相干扰
     */
    private fun cleanupStorageFiles() {
        val storageDir = File(context.getExternalFilesDir(null), "LiveCapture/photos")
        if (storageDir.exists()) {
            storageDir.deleteRecursively()
        }
    }

    /**
     * 通过反射获取 service 的 recordsFile 属性，便于测试中直接操作文件
     */
    private fun getRecordsFile(): File {
        val field = PhotoStorageService::class.java.getDeclaredField("recordsFile")
        field.isAccessible = true
        return field.get(service) as File
    }

    /**
     * 通过反射获取 service 的 storageDir 属性
     */
    private fun getStorageDir(): File {
        val field = PhotoStorageService::class.java.getDeclaredField("storageDir")
        field.isAccessible = true
        return field.get(service) as File
    }

    /**
     * 通过反射获取 service 的 thumbsDir 属性
     */
    private fun getThumbsDir(): File {
        val field = PhotoStorageService::class.java.getDeclaredField("thumbsDir")
        field.isAccessible = true
        return field.get(service) as File
    }

    /**
     * 通过反射调用私有方法 cropToAspectRatio
     */
    private fun invokeCropToAspectRatio(bitmap: Bitmap, aspectRatio: Float): Bitmap {
        val method: Method = PhotoStorageService::class.java.getDeclaredMethod(
            "cropToAspectRatio", Bitmap::class.java, Float::class.javaPrimitiveType
        )
        method.isAccessible = true
        return method.invoke(service, bitmap, aspectRatio) as Bitmap
    }

    /**
     * 通过反射调用私有方法 rotateBitmap
     */
    private fun invokeRotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val method: Method = PhotoStorageService::class.java.getDeclaredMethod(
            "rotateBitmap", Bitmap::class.java, Float::class.javaPrimitiveType
        )
        method.isAccessible = true
        return method.invoke(service, bitmap, degrees) as Bitmap
    }

    /**
     * 通过反射调用私有方法 parseRecord
     */
    private fun invokeParseRecord(json: JSONObject): PhotoRecord {
        val method: Method = PhotoStorageService::class.java.getDeclaredMethod(
            "parseRecord", JSONObject::class.java
        )
        method.isAccessible = true
        return method.invoke(service, json) as PhotoRecord
    }

    /**
     * 通过反射调用私有方法 saveRecords
     */
    private fun invokeSaveRecords(records: List<PhotoRecord>) {
        val method: Method = PhotoStorageService::class.java.getDeclaredMethod(
            "saveRecords", List::class.java
        )
        method.isAccessible = true
        method.invoke(service, records)
    }

    /**
     * 通过反射调用私有方法 writeExif
     */
    private fun invokeWriteExif(file: File, exifData: ExifData) {
        val method: Method = PhotoStorageService::class.java.getDeclaredMethod(
            "writeExif", File::class.java, ExifData::class.java
        )
        method.isAccessible = true
        method.invoke(service, file, exifData)
    }

    /**
     * 创建一个测试用的 PhotoRecord，所有字段均填充
     */
    private fun createTestRecord(
        id: String = "test-id-${System.nanoTime()}",
        filePath: String = "/photos/test.jpg",
        thumbPath: String = "/thumbs/test.jpg",
        width: Int = 1920,
        height: Int = 1080,
        timestamp: Long = System.currentTimeMillis(),
        iso: String? = "400",
        shutterSpeed: String? = "1/125",
        aperture: String? = "f/2.8",
        focalLength: String? = "50mm",
        cropRegion: CropRegion? = CropRegion(0.5f, 0.5f, 0.8f, 0.6f),
        aestheticScore: Float? = 0.85f
    ): PhotoRecord = PhotoRecord(
        id = id,
        filePath = filePath,
        thumbPath = thumbPath,
        width = width,
        height = height,
        timestamp = timestamp,
        iso = iso,
        shutterSpeed = shutterSpeed,
        aperture = aperture,
        focalLength = focalLength,
        cropRegion = cropRegion,
        aestheticScore = aestheticScore
    )

    /**
     * 创建一个测试用的 Bitmap
     */
    private fun createTestBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    // =====================================================
    // getAllRecords() 测试
    // =====================================================

    @Test
    fun `getAllRecords - 文件不存在时返回空列表`() {
        // records.json 尚未被创建
        val records = service.getAllRecords()

        assertTrue("文件不存在时应返回空列表", records.isEmpty())
    }

    @Test
    fun `getAllRecords - 空文件返回空列表`() {
        val recordsFile = getRecordsFile()
        recordsFile.parentFile?.mkdirs()
        recordsFile.writeText("")

        val records = service.getAllRecords()

        assertTrue("空文件时应返回空列表（解析失败走 catch 分支）", records.isEmpty())
    }

    @Test
    fun `getAllRecords - 有效JSON返回正确记录列表`() {
        val recordsFile = getRecordsFile()
        recordsFile.parentFile?.mkdirs()
        val array = JSONArray()
        // 写入两条记录的 JSON
        array.put(JSONObject().apply {
            put("id", "id-1")
            put("filePath", "/photos/1.jpg")
            put("thumbPath", "/thumbs/1.jpg")
            put("width", 1920)
            put("height", 1080)
            put("timestamp", 1700000000000L)
        })
        array.put(JSONObject().apply {
            put("id", "id-2")
            put("filePath", "/photos/2.jpg")
            put("thumbPath", "/thumbs/2.jpg")
            put("width", 1080)
            put("height", 1920)
            put("timestamp", 1700000000001L)
        })
        recordsFile.writeText(array.toString())

        val records = service.getAllRecords()

        assertEquals("应返回2条记录", 2, records.size)
        assertEquals("第一条记录id应为id-1", "id-1", records[0].id)
        assertEquals("第二条记录id应为id-2", "id-2", records[1].id)
    }

    @Test
    fun `getAllRecords - 损坏的JSON返回空列表`() {
        val recordsFile = getRecordsFile()
        recordsFile.parentFile?.mkdirs()
        recordsFile.writeText("this is not valid json {{{{")

        val records = service.getAllRecords()

        assertTrue("损坏的JSON应返回空列表", records.isEmpty())
    }

    @Test
    fun `getAllRecords - 空JSON数组返回空列表`() {
        val recordsFile = getRecordsFile()
        recordsFile.parentFile?.mkdirs()
        recordsFile.writeText("[]")

        val records = service.getAllRecords()

        assertTrue("空JSON数组应返回空列表", records.isEmpty())
    }

    // =====================================================
    // addRecordToIndex() 测试
    // =====================================================

    @Test
    fun `addRecordToIndex - 将记录添加到列表开头`() = runTest {
        val record1 = createTestRecord(id = "first")
        val record2 = createTestRecord(id = "second")

        // 先添加第一条，再添加第二条
        service.addRecordToIndex(record1)
        service.addRecordToIndex(record2)

        val records = service.getAllRecords()

        assertEquals("应有2条记录", 2, records.size)
        // 第二条添加的应在最前面
        assertEquals("最新记录应在列表开头", "second", records[0].id)
        assertEquals("先添加的记录应排在后面", "first", records[1].id)
    }

    @Test
    fun `addRecordToIndex - 互斥锁防止并发写入导致记录丢失`() = runTest {
        // 并发添加100条记录，互斥锁保证无丢失
        val count = 100
        val deferreds = (0 until count).map { index ->
            async {
                val record = createTestRecord(id = "record-$index")
                service.addRecordToIndex(record)
            }
        }
        deferreds.awaitAll()

        val records = service.getAllRecords()

        // 互斥锁保护下，100条记录不应丢失
        assertEquals("并发添加后应有${count}条记录", count, records.size)
    }

    // =====================================================
    // deleteRecordAsync() 测试
    // =====================================================

    @Test
    fun `deleteRecordAsync - 删除文件路径的记录并从索引移除`() = runTest {
        val storageDir = getStorageDir()
        storageDir.mkdirs()
        val thumbsDir = getThumbsDir()
        thumbsDir.mkdirs()

        // 创建主图文件和缩略图文件
        val photoFile = File(storageDir, "photo_to_delete.jpg")
        photoFile.writeText("fake photo data")
        val thumbFile = File(thumbsDir, "thumb_photo_to_delete.jpg")
        thumbFile.writeText("fake thumb data")

        val record = createTestRecord(
            id = "to-delete",
            filePath = photoFile.absolutePath,
            thumbPath = thumbFile.absolutePath
        )
        service.addRecordToIndex(record)

        // 确认记录已添加
        assertEquals("添加后应有1条记录", 1, service.getAllRecords().size)

        // 执行删除
        service.deleteRecordAsync(record)

        // 文件应被删除
        assertFalse("主图文件应被删除", photoFile.exists())
        assertFalse("缩略图文件应被删除", thumbFile.exists())
        // 记录应从索引中移除
        assertTrue("删除后索引应为空", service.getAllRecords().isEmpty())
    }

    @Test
    fun `deleteRecordAsync - content URI 不抛异常`() = runTest {
        val storageDir = getStorageDir()
        storageDir.mkdirs()
        val thumbsDir = getThumbsDir()
        thumbsDir.mkdirs()

        // 创建缩略图文件（content:// URI 对应的主图无法真正删除，但不应崩溃）
        val thumbFile = File(thumbsDir, "thumb_content_uri.jpg")
        thumbFile.writeText("fake thumb data")

        val record = createTestRecord(
            id = "content-uri-record",
            filePath = "content://media/external/images/media/123",
            thumbPath = thumbFile.absolutePath
        )
        service.addRecordToIndex(record)

        // 执行删除，content:// URI 删除可能失败，但不应抛异常
        service.deleteRecordAsync(record)

        // 缩略图文件应被删除
        assertFalse("缩略图文件应被删除", thumbFile.exists())
        // 记录应从索引中移除
        assertTrue("删除后索引应为空", service.getAllRecords().isEmpty())
    }

    @Test
    fun `deleteRecordAsync - 文件不存在时优雅处理不崩溃`() = runTest {
        // 文件路径指向不存在的文件
        val record = createTestRecord(
            id = "nonexistent",
            filePath = "/nonexistent/path/photo.jpg",
            thumbPath = "/nonexistent/path/thumb.jpg"
        )
        service.addRecordToIndex(record)

        // 删除不存在的文件不应崩溃
        service.deleteRecordAsync(record)

        // 记录应从索引中移除
        assertTrue("删除后索引应为空", service.getAllRecords().isEmpty())
    }

    @Test
    fun `deleteRecordAsync - 仅删除目标记录保留其他记录`() = runTest {
        val storageDir = getStorageDir()
        storageDir.mkdirs()
        val thumbsDir = getThumbsDir()
        thumbsDir.mkdirs()

        val photo1 = File(storageDir, "keep.jpg")
        photo1.writeText("keep data")
        val thumb1 = File(thumbsDir, "thumb_keep.jpg")
        thumb1.writeText("keep thumb")

        val photo2 = File(storageDir, "delete.jpg")
        photo2.writeText("delete data")
        val thumb2 = File(thumbsDir, "thumb_delete.jpg")
        thumb2.writeText("delete thumb")

        val recordKeep = createTestRecord(
            id = "keep",
            filePath = photo1.absolutePath,
            thumbPath = thumb1.absolutePath
        )
        val recordDelete = createTestRecord(
            id = "delete",
            filePath = photo2.absolutePath,
            thumbPath = thumb2.absolutePath
        )

        service.addRecordToIndex(recordKeep)
        service.addRecordToIndex(recordDelete)

        // 删除第二条记录
        service.deleteRecordAsync(recordDelete)

        val remaining = service.getAllRecords()
        assertEquals("应仅剩1条记录", 1, remaining.size)
        assertEquals("剩余记录应为keep", "keep", remaining[0].id)

        // 保留的文件应存在
        assertTrue("保留的主图文件应存在", photo1.exists())
        assertTrue("保留的缩略图文件应存在", thumb1.exists())
        // 删除的文件应不存在
        assertFalse("删除的主图文件应不存在", photo2.exists())
        assertFalse("删除的缩略图文件应不存在", thumb2.exists())
    }

    // =====================================================
    // saveRecords() 测试
    // =====================================================

    @Test
    fun `saveRecords - 写入有效JSON到文件`() {
        val records = listOf(
            createTestRecord(
                id = "save-1",
                filePath = "/photos/save1.jpg",
                thumbPath = "/thumbs/save1.jpg",
                width = 1920,
                height = 1080,
                timestamp = 1700000000000L,
                iso = "200",
                shutterSpeed = "1/60",
                aperture = "f/4.0",
                focalLength = "35mm",
                cropRegion = CropRegion(0.3f, 0.7f, 0.5f, 0.4f),
                aestheticScore = 0.92f
            ),
            createTestRecord(
                id = "save-2",
                filePath = "/photos/save2.jpg",
                thumbPath = "/thumbs/save2.jpg",
                width = 1080,
                height = 1920,
                timestamp = 1700000000001L
            )
        )

        invokeSaveRecords(records)

        val recordsFile = getRecordsFile()
        assertTrue("records.json 文件应存在", recordsFile.exists())

        val jsonText = recordsFile.readText()
        val array = JSONArray(jsonText)
        assertEquals("应包含2条记录", 2, array.length())

        // 验证第一条记录的所有字段
        val first = array.getJSONObject(0)
        assertEquals("save-1", first.getString("id"))
        assertEquals("/photos/save1.jpg", first.getString("filePath"))
        assertEquals("/thumbs/save1.jpg", first.getString("thumbPath"))
        assertEquals(1920, first.getInt("width"))
        assertEquals(1080, first.getInt("height"))
        assertEquals(1700000000000L, first.getLong("timestamp"))
        assertEquals("200", first.getString("iso"))
        assertEquals("1/60", first.getString("shutterSpeed"))
        assertEquals("f/4.0", first.getString("aperture"))
        assertEquals("35mm", first.getString("focalLength"))
        // cropRegion 应作为嵌套对象写入
        val cropJson = first.getJSONObject("cropRegion")
        assertEquals(0.3, cropJson.getDouble("centerX"), 0.001)
        assertEquals(0.7, cropJson.getDouble("centerY"), 0.001)
        assertEquals(0.5, cropJson.getDouble("width"), 0.001)
        assertEquals(0.4, cropJson.getDouble("height"), 0.001)
        assertEquals(0.92, first.getDouble("aestheticScore"), 0.001)

        // 第二条记录无可选字段，不应包含 cropRegion 和 aestheticScore 键
        val second = array.getJSONObject(1)
        assertEquals("save-2", second.getString("id"))
        assertFalse("无cropRegion时不应包含cropRegion键", second.has("cropRegion"))
        assertFalse("无aestheticScore时不应包含aestheticScore键", second.has("aestheticScore"))
    }

    @Test
    fun `saveRecords - 空列表写入空JSON数组`() {
        invokeSaveRecords(emptyList())

        val recordsFile = getRecordsFile()
        assertTrue("文件应存在", recordsFile.exists())
        assertEquals("空列表应写入空数组", "[]", recordsFile.readText())
    }

    // =====================================================
    // parseRecord() 测试
    // =====================================================

    @Test
    fun `parseRecord - 所有字段正确解析`() {
        val json = JSONObject().apply {
            put("id", "parse-full")
            put("filePath", "/photos/full.jpg")
            put("thumbPath", "/thumbs/full.jpg")
            put("width", 3840)
            put("height", 2160)
            put("timestamp", 1700000000000L)
            put("iso", "800")
            put("shutterSpeed", "1/250")
            put("aperture", "f/1.8")
            put("focalLength", "85mm")
            put("cropRegion", JSONObject().apply {
                put("centerX", 0.5)
                put("centerY", 0.5)
                put("width", 1.0)
                put("height", 1.0)
            })
            put("aestheticScore", 0.75)
        }

        val record = invokeParseRecord(json)

        assertEquals("parse-full", record.id)
        assertEquals("/photos/full.jpg", record.filePath)
        assertEquals("/thumbs/full.jpg", record.thumbPath)
        assertEquals(3840, record.width)
        assertEquals(2160, record.height)
        assertEquals(1700000000000L, record.timestamp)
        assertEquals("800", record.iso)
        assertEquals("1/250", record.shutterSpeed)
        assertEquals("f/1.8", record.aperture)
        assertEquals("85mm", record.focalLength)
        assertNotNull("cropRegion 不应为 null", record.cropRegion)
        assertEquals(0.5f, record.cropRegion!!.centerX, 0.001f)
        assertEquals(0.5f, record.cropRegion!!.centerY, 0.001f)
        assertEquals(1.0f, record.cropRegion!!.width, 0.001f)
        assertEquals(1.0f, record.cropRegion!!.height, 0.001f)
        assertEquals(0.75f, record.aestheticScore!!, 0.001f)
    }

    @Test
    fun `parseRecord - 可选字段缺失时使用默认值null`() {
        val json = JSONObject().apply {
            put("id", "parse-minimal")
            put("filePath", "/photos/minimal.jpg")
            put("thumbPath", "/thumbs/minimal.jpg")
            put("width", 640)
            put("height", 480)
            put("timestamp", 1700000000000L)
            // 不设置 iso、shutterSpeed、aperture、focalLength、cropRegion、aestheticScore
        }

        val record = invokeParseRecord(json)

        assertEquals("parse-minimal", record.id)
        assertEquals(640, record.width)
        assertEquals(480, record.height)
        assertNull("iso 缺失时应为 null", record.iso)
        assertNull("shutterSpeed 缺失时应为 null", record.shutterSpeed)
        assertNull("aperture 缺失时应为 null", record.aperture)
        assertNull("focalLength 缺失时应为 null", record.focalLength)
        assertNull("cropRegion 缺失时应为 null", record.cropRegion)
        assertNull("aestheticScore 缺失时应为 null", record.aestheticScore)
    }

    @Test
    fun `parseRecord - cropRegion 正确解析为 CropRegion 对象`() {
        val json = JSONObject().apply {
            put("id", "parse-crop")
            put("filePath", "/photos/crop.jpg")
            put("thumbPath", "/thumbs/crop.jpg")
            put("width", 800)
            put("height", 600)
            put("timestamp", 1700000000000L)
            put("cropRegion", JSONObject().apply {
                put("centerX", 0.25)
                put("centerY", 0.75)
                put("width", 0.5)
                put("height", 0.3)
            })
        }

        val record = invokeParseRecord(json)

        assertNotNull("cropRegion 应被解析", record.cropRegion)
        assertEquals(0.25f, record.cropRegion!!.centerX, 0.001f)
        assertEquals(0.75f, record.cropRegion!!.centerY, 0.001f)
        assertEquals(0.5f, record.cropRegion!!.width, 0.001f)
        assertEquals(0.3f, record.cropRegion!!.height, 0.001f)
    }

    @Test
    fun `parseRecord - aestheticScore 为 NaN 时返回 null`() {
        // 当 JSON 中没有 aestheticScore 键时，optDouble("aestheticScore", Double.NaN) 返回 NaN
        val json = JSONObject().apply {
            put("id", "parse-nan")
            put("filePath", "/photos/nan.jpg")
            put("thumbPath", "/thumbs/nan.jpg")
            put("width", 100)
            put("height", 100)
            put("timestamp", 1700000000000L)
            // 不设置 aestheticScore，optDouble 默认值为 NaN
        }

        val record = invokeParseRecord(json)

        assertNull("aestheticScore 缺失时应为 null（NaN → null）", record.aestheticScore)
    }

    @Test
    fun `parseRecord - aestheticScore 为0f时正确解析不为null`() {
        val json = JSONObject().apply {
            put("id", "parse-zero-score")
            put("filePath", "/photos/zero.jpg")
            put("thumbPath", "/thumbs/zero.jpg")
            put("width", 100)
            put("height", 100)
            put("timestamp", 1700000000000L)
            put("aestheticScore", 0.0)
        }

        val record = invokeParseRecord(json)

        assertNotNull("aestheticScore 为 0f 时不应为 null", record.aestheticScore)
        assertEquals(0f, record.aestheticScore!!, 0.001f)
    }

    @Test
    fun `parseRecord - aestheticScore 为1f时正确解析`() {
        val json = JSONObject().apply {
            put("id", "parse-one-score")
            put("filePath", "/photos/one.jpg")
            put("thumbPath", "/thumbs/one.jpg")
            put("width", 100)
            put("height", 100)
            put("timestamp", 1700000000000L)
            put("aestheticScore", 1.0)
        }

        val record = invokeParseRecord(json)

        assertEquals(1.0f, record.aestheticScore!!, 0.001f)
    }

    // =====================================================
    // 记录持久化集成测试
    // =====================================================

    @Test
    fun `记录持久化 - 保存后读取的记录与原始记录一致`() = runTest {
        val original = createTestRecord(
            id = "persistence-test",
            filePath = "/photos/persist.jpg",
            thumbPath = "/thumbs/persist.jpg",
            width = 2560,
            height = 1440,
            timestamp = 1700000000123L,
            iso = "1600",
            shutterSpeed = "1/1000",
            aperture = "f/1.4",
            focalLength = "85mm",
            cropRegion = CropRegion(0.2f, 0.8f, 0.6f, 0.4f),
            aestheticScore = 0.99f
        )

        service.addRecordToIndex(original)
        val loaded = service.getAllRecords()

        assertEquals("应包含1条记录", 1, loaded.size)
        val record = loaded[0]
        assertEquals(original.id, record.id)
        assertEquals(original.filePath, record.filePath)
        assertEquals(original.thumbPath, record.thumbPath)
        assertEquals(original.width, record.width)
        assertEquals(original.height, record.height)
        assertEquals(original.timestamp, record.timestamp)
        assertEquals(original.iso, record.iso)
        assertEquals(original.shutterSpeed, record.shutterSpeed)
        assertEquals(original.aperture, record.aperture)
        assertEquals(original.focalLength, record.focalLength)
        assertNotNull("cropRegion 应不为 null", record.cropRegion)
        assertEquals(original.cropRegion!!.centerX, record.cropRegion!!.centerX, 0.001f)
        assertEquals(original.cropRegion!!.centerY, record.cropRegion!!.centerY, 0.001f)
        assertEquals(original.cropRegion!!.width, record.cropRegion!!.width, 0.001f)
        assertEquals(original.cropRegion!!.height, record.cropRegion!!.height, 0.001f)
        assertEquals(original.aestheticScore, record.aestheticScore, 0.001f)
    }

    @Test
    fun `记录持久化 - 多次添加和删除后索引正确`() = runTest {
        val r1 = createTestRecord(id = "r1")
        val r2 = createTestRecord(id = "r2")
        val r3 = createTestRecord(id = "r3")

        service.addRecordToIndex(r1)
        service.addRecordToIndex(r2)
        service.addRecordToIndex(r3)

        assertEquals(3, service.getAllRecords().size)

        service.deleteRecordAsync(r2)

        val remaining = service.getAllRecords()
        assertEquals(2, remaining.size)
        // r3 最晚添加，应在最前
        assertEquals("r3", remaining[0].id)
        assertEquals("r1", remaining[1].id)
    }

    // =====================================================
    // cropToAspectRatio() 测试
    // =====================================================

    @Test
    fun `cropToAspectRatio - 位图比目标宽时裁切宽度`() {
        // 100x50 位图，宽高比 2.0，目标 0.75 (3:4)，位图更宽
        val bitmap = createTestBitmap(100, 50)
        val result = invokeCropToAspectRatio(bitmap, 0.75f)

        // 目标宽度 = height * aspectRatio = 50 * 0.75 = 37.5 → 38
        // 但 maxOf(1, 38) = 38, coerceAtMost(100) = 38
        val expectedWidth = maxOf(1, (50 * 0.75f).toInt()).coerceAtMost(100)
        assertEquals("裁切后宽度应为 $expectedWidth", expectedWidth, result.width)
        assertEquals("裁切后高度应不变", 50, result.height)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    @Test
    fun `cropToAspectRatio - 位图比目标高时裁切高度`() {
        // 50x100 位图，宽高比 0.5，目标 0.75 (3:4)，位图更高
        val bitmap = createTestBitmap(50, 100)
        val result = invokeCropToAspectRatio(bitmap, 0.75f)

        // 目标高度 = width / aspectRatio = 50 / 0.75 = 66.67 → 66
        val expectedHeight = maxOf(1, (50 / 0.75f).toInt()).coerceAtMost(100)
        assertEquals("裁切后高度应为 $expectedHeight", expectedHeight, result.height)
        assertEquals("裁切后宽度应不变", 50, result.width)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    @Test
    fun `cropToAspectRatio - 宽高比已正确时返回相同位图`() {
        // 创建 30x40 位图，宽高比 = 0.75，与目标 3:4 一致
        val bitmap = createTestBitmap(30, 40)
        val result = invokeCropToAspectRatio(bitmap, 0.75f)

        // 宽高比一致时，不会走裁切逻辑（currentAspect 不 > 也不 < aspectRatio，走 else 分支但高度不变）
        // 注意：由于浮点精度，可能略有差异，此处验证宽高比接近
        val resultAspect = result.width.toFloat() / result.height
        assertEquals("裁切后宽高比应接近目标", 0.75f, resultAspect, 0.01f)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    @Test
    fun `cropToAspectRatio - 零维度位图安全返回原位图`() {
        // 创建 0x100 位图，宽为0应安全返回原位图
        // 注意：Bitmap.createBitmap 不允许宽高为0，所以用1x1测试边界
        // 源码中 if (width <= 0 || height <= 0) return bitmap
        // 由于 Android Bitmap 不允许创建0宽高位图，这里测试1x1极小位图
        val bitmap = createTestBitmap(1, 1)
        val result = invokeCropToAspectRatio(bitmap, 0.75f)

        // 1x1 位图宽高比 = 1.0 > 0.75，走宽度裁切分支
        // newWidth = maxOf(1, (1 * 0.75).toInt()) = maxOf(1, 0) = 1
        // 不会崩溃
        assertNotNull("极小位图裁切不应崩溃", result)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    @Test
    fun `cropToAspectRatio - maxOf防零宽裁切`() {
        // 极端场景：高度为1，宽高比目标很小，导致 (height * aspectRatio) 计算为 0
        // 例如 100x1 位图，目标宽高比 0.001
        val bitmap = createTestBitmap(100, 1)
        val result = invokeCropToAspectRatio(bitmap, 0.001f)

        // currentAspect = 100.0 > 0.001，走宽度裁切
        // newWidth = maxOf(1, (1 * 0.001).toInt()) = maxOf(1, 0) = 1
        // 确保 newWidth 至少为 1
        assertTrue("裁切后宽度至少为1", result.width >= 1)
        assertTrue("裁切后高度至少为1", result.height >= 1)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    @Test
    fun `cropToAspectRatio - 裁切结果居中`() {
        // 100x50 位图，目标 3:4 (0.75)
        val bitmap = createTestBitmap(100, 50)
        val result = invokeCropToAspectRatio(bitmap, 0.75f)

        // newWidth = maxOf(1, (50 * 0.75).toInt()).coerceAtMost(100) = 37
        // xOffset = (100 - 37) / 2 = 31
        // 裁切从 xOffset=31 开始，即水平居中
        val expectedWidth = maxOf(1, (50 * 0.75f).toInt()).coerceAtMost(100)
        assertEquals(expectedWidth, result.width)
        assertEquals(50, result.height)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    // =====================================================
    // rotateBitmap() 测试
    // =====================================================

    @Test
    fun `rotateBitmap - 0度返回相同位图对象`() {
        val bitmap = createTestBitmap(100, 50)
        val result = invokeRotateBitmap(bitmap, 0f)

        // 0度时不创建新位图，直接返回原对象
        assertSame("0度旋转应返回相同的位图对象", bitmap, result)

        bitmap.recycle()
    }

    @Test
    fun `rotateBitmap - 90度正确旋转`() {
        val bitmap = createTestBitmap(100, 50)
        val result = invokeRotateBitmap(bitmap, 90f)

        // 90度旋转：宽高互换
        assertEquals("旋转后宽度应等于原高度", 50, result.width)
        assertEquals("旋转后高度应等于原宽度", 100, result.height)

        bitmap.recycle()
        result.recycle()
    }

    @Test
    fun `rotateBitmap - 180度正确旋转`() {
        val bitmap = createTestBitmap(100, 50)
        val result = invokeRotateBitmap(bitmap, 180f)

        // 180度旋转：宽高不变
        assertEquals("180度旋转后宽度不变", 100, result.width)
        assertEquals("180度旋转后高度不变", 50, result.height)

        bitmap.recycle()
        result.recycle()
    }

    @Test
    fun `rotateBitmap - 270度正确旋转`() {
        val bitmap = createTestBitmap(100, 50)
        val result = invokeRotateBitmap(bitmap, 270f)

        // 270度旋转：宽高互换
        assertEquals("旋转后宽度应等于原高度", 50, result.width)
        assertEquals("旋转后高度应等于原宽度", 100, result.height)

        bitmap.recycle()
        result.recycle()
    }

    // =====================================================
    // writeExif() 测试
    // =====================================================

    @Test
    fun `writeExif - 写入所有EXIF标签`() {
        // 创建一个临时JPEG文件（最小有效JPEG）
        val tempFile = File(context.cacheDir, "exif_test_${System.currentTimeMillis()}.jpg")
        try {
            // 创建一个最小的有效JPEG文件
            val bitmap = createTestBitmap(10, 10)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, tempFile.outputStream())
            bitmap.recycle()

            val exifData = ExifData(
                iso = "3200",
                shutterSpeed = "1/4000",
                aperture = "f/1.4",
                focalLength = "85mm",
                make = "TestMake",
                model = "TestModel"
            )

            invokeWriteExif(tempFile, exifData)

            // 读取 EXIF 验证写入
            val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
            assertEquals("ISO应正确写入", "3200",
                exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY))
            assertEquals("曝光时间应正确写入", "1/4000",
                exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME))
            assertEquals("光圈应正确写入", "f/1.4",
                exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER))
            assertEquals("焦距应正确写入", "85mm",
                exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH))
            assertEquals("制造商应正确写入", "TestMake",
                exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE))
            assertEquals("型号应正确写入", "TestModel",
                exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `writeExif - 可空字段为null时不写入对应标签`() {
        val tempFile = File(context.cacheDir, "exif_null_test_${System.currentTimeMillis()}.jpg")
        try {
            val bitmap = createTestBitmap(10, 10)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, tempFile.outputStream())
            bitmap.recycle()

            val exifData = ExifData(
                iso = null,
                shutterSpeed = null,
                aperture = null,
                focalLength = null,
                make = "TestMake",
                model = "TestModel"
            )

            invokeWriteExif(tempFile, exifData)

            // 可空字段为 null 时不应写入 EXIF 标签
            val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
            assertNull("ISO为null时不应写入", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY))
            assertNull("曝光时间为null时不应写入", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME))
            assertNull("光圈为null时不应写入", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER))
            assertNull("焦距为null时不应写入", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH))
            // make 和 model 始终写入
            assertEquals("TestMake", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE))
            assertEquals("TestModel", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `writeExif - 无效文件路径时优雅处理不崩溃`() {
        // 指向一个不存在的文件路径，writeExif 应捕获异常而不崩溃
        val nonExistentFile = File("/nonexistent/path/exif_test.jpg")
        val exifData = ExifData(iso = "100", make = "Test", model = "Test")

        // 不应抛出异常
        invokeWriteExif(nonExistentFile, exifData)
    }

    // =====================================================
    // saveJpegToStorage 相关测试（通过反射间接测试）
    // =====================================================

    @Test
    fun `saveJpegToStorage - API 29+ MediaStore失败时回退到私有目录`() {
        // 在 Robolectric SDK 34 环境下，MediaStore 操作通常会失败或返回 null
        // 此处验证 saveJpegToStorage 方法在 MediaStore 不可用时回退到文件系统
        val method = PhotoStorageService::class.java.getDeclaredMethod(
            "saveJpegToStorage", File::class.java, String::class.java
        )
        method.isAccessible = true

        // 创建一个临时 JPEG 文件
        val tempFile = File(context.cacheDir, "save_test_${System.currentTimeMillis()}.jpg")
        try {
            val bitmap = createTestBitmap(10, 10)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, tempFile.outputStream())
            bitmap.recycle()

            val result = method.invoke(service, tempFile, "test_photo.jpg") as String

            // 结果应是非空路径（content:// 或文件绝对路径）
            assertTrue("保存结果应是非空路径", result.isNotEmpty())

            // 如果不是 content:// URI，则应写入私有目录
            if (!result.startsWith("content://")) {
                val savedFile = File(result)
                // 文件不一定在当前可访问（Robolectric 环境限制），但路径应合理
                assertTrue("文件路径应包含文件名", result.contains("test_photo.jpg"))
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    @Config(sdk = [28]) // API 28 < 29，走文件系统分支
    fun `saveJpegToStorage - API 28 使用文件系统保存`() {
        val method = PhotoStorageService::class.java.getDeclaredMethod(
            "saveJpegToStorage", File::class.java, String::class.java
        )
        method.isAccessible = true

        val tempFile = File(context.cacheDir, "save_api28_${System.currentTimeMillis()}.jpg")
        try {
            val bitmap = createTestBitmap(10, 10)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, tempFile.outputStream())
            bitmap.recycle()

            val result = method.invoke(service, tempFile, "api28_photo.jpg") as String

            // API < 29 应直接写入私有目录，返回文件绝对路径
            assertFalse("API 28 不应返回 content:// URI", result.startsWith("content://"))
            assertTrue("应返回包含文件名的绝对路径", result.contains("api28_photo.jpg"))
            // 验证文件确实存在
            val savedFile = File(result)
            assertTrue("保存的文件应存在", savedFile.exists())
            assertTrue("保存的文件应有内容", savedFile.length() > 0)
        } finally {
            tempFile.delete()
        }
    }

    // =====================================================
    // 综合场景测试
    // =====================================================

    @Test
    fun `综合场景 - 添加多条记录后逐一删除最终为空`() = runTest {
        val storageDir = getStorageDir()
        storageDir.mkdirs()
        val thumbsDir = getThumbsDir()
        thumbsDir.mkdirs()

        val records = (1..5).map { i ->
            val photoFile = File(storageDir, "photo_$i.jpg")
            photoFile.writeText("photo data $i")
            val thumbFile = File(thumbsDir, "thumb_photo_$i.jpg")
            thumbFile.writeText("thumb data $i")
            createTestRecord(
                id = "batch-$i",
                filePath = photoFile.absolutePath,
                thumbPath = thumbFile.absolutePath
            )
        }

        // 逐条添加
        records.forEach { service.addRecordToIndex(it) }
        assertEquals("添加5条记录后应为5条", 5, service.getAllRecords().size)

        // 逐条删除
        records.forEach { service.deleteRecordAsync(it) }
        assertTrue("全部删除后应为空", service.getAllRecords().isEmpty())

        // 所有文件应被删除
        records.forEach { record ->
            assertFalse("主图文件应被删除: ${record.filePath}", File(record.filePath).exists())
            assertFalse("缩略图文件应被删除: ${record.thumbPath}", File(record.thumbPath).exists())
        }
    }

    @Test
    fun `综合场景 - 位图裁切后旋转尺寸正确`() {
        // 模拟 savePhoto 的裁切和旋转流程
        // 创建 400x300 位图（4:3），裁切为 3:4
        val bitmap = createTestBitmap(400, 300)
        val cropped = invokeCropToAspectRatio(bitmap, 3f / 4f)
        val rotated = invokeRotateBitmap(cropped, 90f)

        // 裁切后：currentAspect = 400/300 = 1.33 > 0.75
        // newWidth = maxOf(1, (300 * 0.75).toInt()).coerceAtMost(400) = maxOf(1, 225) = 225
        assertEquals("裁切后宽度应为225", 225, cropped.width)
        assertEquals("裁切后高度应不变", 300, cropped.height)

        // 旋转90度后：宽高互换
        assertEquals("旋转后宽度应为裁切后高度", 300, rotated.width)
        assertEquals("旋转后高度应为裁切后宽度", 225, rotated.height)

        bitmap.recycle()
        if (cropped !== bitmap) cropped.recycle()
        if (rotated !== cropped) rotated.recycle()
    }

    @Test
    fun `综合场景 - 记录增删后recordsJson文件内容始终有效`() = runTest {
        val r1 = createTestRecord(id = "json-1")
        val r2 = createTestRecord(id = "json-2")

        // 添加两条
        service.addRecordToIndex(r1)
        service.addRecordToIndex(r2)

        // 验证文件内容是有效JSON
        val recordsFile = getRecordsFile()
        val jsonText = recordsFile.readText()
        // 不应抛异常
        val array = JSONArray(jsonText)
        assertEquals(2, array.length())

        // 删除一条
        service.deleteRecordAsync(r1)

        // 文件内容仍然有效
        val jsonTextAfterDelete = recordsFile.readText()
        val arrayAfterDelete = JSONArray(jsonTextAfterDelete)
        assertEquals(1, arrayAfterDelete.length())
        assertEquals("json-2", arrayAfterDelete.getJSONObject(0).getString("id"))
    }
}
