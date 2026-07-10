package com.livecompose.livecapture.core.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoStorageService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "PhotoStorageService"
        private const val RECORDS_FILE = "records.json"
        private const val THUMBS_DIR = "thumbs"
        private const val MAX_THUMB_SIZE = 300
    }

    private val storageDir: File by lazy {
        File(context.getExternalFilesDir(null), "LiveCapture/photos").apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbsDir: File by lazy {
        File(storageDir, THUMBS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    private val recordsFile: File by lazy {
        File(storageDir, RECORDS_FILE)
    }

    suspend fun savePhoto(
        imageProxy: ImageProxy,
        cropRegion: CropRegion? = null,
        exifData: ExifData = ExifData(),
        aestheticScore: Float? = null
    ): PhotoRecord = withContext(Dispatchers.IO) {
        val bitmap = imageProxyToBitmap(imageProxy)

        // 3:4 裁切
        val targetAspect = 3f / 4f
        val croppedBitmap = cropToAspectRatio(bitmap, targetAspect)

        // 旋转校正
        val rotatedBitmap = rotateBitmap(croppedBitmap, imageProxy.imageInfo.rotationDegrees.toFloat())

        val fileName = "${UUID.randomUUID()}.jpg"

        // 保存主图到 MediaStore (API 29+) 或文件系统
        val uri = saveImageToStorage(rotatedBitmap, fileName)

        // 保存 JPEG 字节到临时文件用于写 EXIF (MediaStore 路径)
        if (uri != null) {
            writeExifToUri(uri, rotatedBitmap, exifData)
        } else {
            val file = File(storageDir, fileName)
            writeExif(file, exifData)
        }

        // 生成缩略图
        val thumbBitmap = ThumbnailUtils.extractThumbnail(rotatedBitmap, MAX_THUMB_SIZE, MAX_THUMB_SIZE)
        val thumbFile = File(thumbsDir, "thumb_$fileName")
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        val filePath = if (uri != null) uri.toString() else File(storageDir, fileName).absolutePath

        val record = PhotoRecord(
            id = UUID.randomUUID().toString(),
            filePath = filePath,
            thumbPath = thumbFile.absolutePath,
            width = rotatedBitmap.width,
            height = rotatedBitmap.height,
            timestamp = System.currentTimeMillis(),
            iso = exifData.iso,
            shutterSpeed = exifData.shutterSpeed,
            aperture = exifData.aperture,
            focalLength = exifData.focalLength,
            cropRegion = cropRegion,
            aestheticScore = aestheticScore
        )

        addRecordToIndex(record)

        // 回收
        bitmap.recycle()
        croppedBitmap.recycle()
        rotatedBitmap.recycle()
        thumbBitmap.recycle()

        record
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // ImageCapture 默认输出 JPEG 格式
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun cropToAspectRatio(bitmap: Bitmap, aspectRatio: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val currentAspect = width.toFloat() / height

        return if (currentAspect > aspectRatio) {
            val newWidth = (height * aspectRatio).toInt()
            val xOffset = (width - newWidth) / 2
            Bitmap.createBitmap(bitmap, xOffset, 0, newWidth, height)
        } else {
            val newHeight = (width / aspectRatio).toInt()
            val yOffset = (height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, yOffset, width, newHeight)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun saveImageToStorage(bitmap: Bitmap, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LiveCapture")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
            }
            uri
        } else {
            val file = File(storageDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            null
        }
    }

    /**
     * 通过 MediaStore Uri 写入 EXIF 数据
     */
    private fun writeExifToUri(uri: Uri, bitmap: Bitmap, exifData: ExifData) {
        try {
            // 先将 bitmap 压缩为 JPEG 字节流
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            val jpegBytes = outputStream.toByteArray()

            // 写入临时文件，用 ExifInterface 修改
            val tempFile = File(context.cacheDir, "temp_exif_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { it.write(jpegBytes) }

            val exif = ExifInterface(tempFile.absolutePath)
            exifData.iso?.let { exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, it) }
            exifData.aperture?.let { exif.setAttribute(ExifInterface.TAG_F_NUMBER, it) }
            exifData.focalLength?.let { exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it) }
            exifData.shutterSpeed?.let {
                exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, it)
            }
            exif.setAttribute(ExifInterface.TAG_MAKE, exifData.make)
            exif.setAttribute(ExifInterface.TAG_MODEL, exifData.model)
            exif.saveAttributes()

            // 将带 EXIF 的文件写回 MediaStore Uri
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(out)
                }
            }

            tempFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write EXIF to Uri", e)
        }
    }

    private fun writeExif(file: File, exifData: ExifData) {
        try {
            val exif = ExifInterface(file.absolutePath)
            exifData.iso?.let { exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, it) }
            exifData.aperture?.let { exif.setAttribute(ExifInterface.TAG_F_NUMBER, it) }
            exifData.focalLength?.let { exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it) }
            exifData.shutterSpeed?.let {
                exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, it)
            }
            exif.setAttribute(ExifInterface.TAG_MAKE, exifData.make)
            exif.setAttribute(ExifInterface.TAG_MODEL, exifData.model)
            exif.saveAttributes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write EXIF", e)
        }
    }

    fun getAllRecords(): List<PhotoRecord> {
        return try {
            if (!recordsFile.exists()) return emptyList()
            val json = recordsFile.readText()
            val array = JSONArray(json)
            List(array.length()) { parseRecord(array.getJSONObject(it)) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load records", e)
            emptyList()
        }
    }

    private fun addRecordToIndex(record: PhotoRecord) {
        val records = getAllRecords().toMutableList()
        records.add(0, record)
        saveRecords(records)
    }

    private fun saveRecords(records: List<PhotoRecord>) {
        try {
            val array = JSONArray()
            records.forEach { record ->
                array.put(JSONObject().apply {
                    put("id", record.id)
                    put("filePath", record.filePath)
                    put("thumbPath", record.thumbPath)
                    put("width", record.width)
                    put("height", record.height)
                    put("timestamp", record.timestamp)
                    put("iso", record.iso)
                    put("shutterSpeed", record.shutterSpeed)
                    put("aperture", record.aperture)
                    put("focalLength", record.focalLength)
                    // 完整持久化 cropRegion
                    record.cropRegion?.let { region ->
                        put("cropRegion", JSONObject().apply {
                            put("centerX", region.centerX)
                            put("centerY", region.centerY)
                            put("width", region.width)
                            put("height", region.height)
                        })
                    }
                    // 持久化 aestheticScore
                    record.aestheticScore?.let { put("aestheticScore", it) }
                })
            }
            recordsFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save records", e)
        }
    }

    private fun parseRecord(json: JSONObject): PhotoRecord {
        val cropRegion = json.optJSONObject("cropRegion")?.let { regionJson ->
            CropRegion(
                centerX = regionJson.getDouble("centerX").toFloat(),
                centerY = regionJson.getDouble("centerY").toFloat(),
                width = regionJson.getDouble("width").toFloat(),
                height = regionJson.getDouble("height").toFloat()
            )
        }

        return PhotoRecord(
            id = json.getString("id"),
            filePath = json.getString("filePath"),
            thumbPath = json.getString("thumbPath"),
            width = json.getInt("width"),
            height = json.getInt("height"),
            timestamp = json.getLong("timestamp"),
            iso = json.optString("iso", null),
            shutterSpeed = json.optString("shutterSpeed", null),
            aperture = json.optString("aperture", null),
            focalLength = json.optString("focalLength", null),
            cropRegion = cropRegion,
            aestheticScore = json.optDouble("aestheticScore", Double.NaN).let {
                if (it.isNaN()) null else it.toFloat()
            }
        )
    }

    fun deleteRecord(record: PhotoRecord) {
        try {
            // 删除 MediaStore Uri 或文件
            if (record.filePath.startsWith("content://")) {
                context.contentResolver.delete(Uri.parse(record.filePath), null, null)
            } else {
                File(record.filePath).delete()
            }
            File(record.thumbPath).delete()
            val records = getAllRecords().filter { it.id != record.id }
            saveRecords(records)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete record", e)
        }
    }
}
