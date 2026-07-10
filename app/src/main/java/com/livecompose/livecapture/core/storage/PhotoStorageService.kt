package com.livecompose.livecapture.core.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
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
        exifData: ExifData = ExifData()
    ): PhotoRecord = withContext(Dispatchers.IO) {
        val bitmap = imageProxyToBitmap(imageProxy)

        // 3:4 裁切
        val targetAspect = 3f / 4f
        val croppedBitmap = cropToAspectRatio(bitmap, targetAspect)

        // 旋转校正
        val rotatedBitmap = rotateBitmap(croppedBitmap, imageProxy.imageInfo.rotationDegrees.toFloat())

        val fileName = "${UUID.randomUUID()}.jpg"

        // 保存主图
        val uri = saveImageToStorage(rotatedBitmap, fileName)
        val filePath = if (uri != null) uri.toString() else File(storageDir, fileName).absolutePath

        // 生成缩略图
        val thumbBitmap = ThumbnailUtils.extractThumbnail(rotatedBitmap, MAX_THUMB_SIZE, MAX_THUMB_SIZE)
        val thumbFile = File(thumbsDir, "thumb_$fileName")
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        // 写入 EXIF
        if (uri == null) {
            writeExif(File(storageDir, fileName), exifData)
        }

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
            cropRegion = cropRegion
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

    private fun saveImageToStorage(bitmap: Bitmap, fileName: String): android.net.Uri? {
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

    private fun writeExif(file: File, exifData: ExifData) {
        try {
            val exif = ExifInterface(file.absolutePath)
            exifData.iso?.let { exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, it) }
            exifData.aperture?.let { exif.setAttribute(ExifInterface.TAG_F_NUMBER, it) }
            exifData.focalLength?.let { exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it) }
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
                })
            }
            recordsFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save records", e)
        }
    }

    private fun parseRecord(json: JSONObject): PhotoRecord {
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
            focalLength = json.optString("focalLength", null)
        )
    }

    fun deleteRecord(record: PhotoRecord) {
        try {
            File(record.filePath).delete()
            File(record.thumbPath).delete()
            val records = getAllRecords().filter { it.id != record.id }
            saveRecords(records)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete record", e)
        }
    }
}
