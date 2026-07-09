package com.livecompose.livecapture.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.livecompose.livecapture.core.logger.AppLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livecompose.livecapture.core.security.CryptoHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * 照片存储服务
 */
class PhotoStorageService(context: Context) {

    private val baseDir = File(context.filesDir, "LiveCapture").also { it.mkdirs() }
    private val photosDir = File(baseDir, "photos").also { it.mkdirs() }
    private val thumbnailsDir = File(baseDir, "thumbnails").also { it.mkdirs() }
    private val recordsFile = File(baseDir, "records.json")
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val saveMutex = Mutex()

    private val _records = MutableStateFlow<List<PhotoRecord>>(emptyList())
    val records: StateFlow<List<PhotoRecord>> = _records.asStateFlow()

    private var isLoaded = false

    init {
        loadRecords()
    }

    fun loadRecords() {
        if (isLoaded) return
        try {
            if (recordsFile.exists()) {
                val rawText = recordsFile.readText()
                // 尝试解密（如果数据已加密）
                val json = CryptoHelper.decrypt(rawText) ?: rawText
                val type = object : TypeToken<List<PhotoRecord>>() {}.type
                val loaded: List<PhotoRecord> = gson.fromJson(json, type)
                _records.value = loaded
            }
        } catch (e: Exception) {
            AppLogger.e("PhotoStorage", "加载记录失败", e)
            _records.value = emptyList()
        }
        isLoaded = true
    }

    fun savePhoto(data: ByteArray, detectionMethod: String? = null) {
        val id = java.util.UUID.randomUUID().toString()
        val photoFile = File(photosDir, PhotoRecord.photoFilename(id))
        val thumbFile = File(thumbnailsDir, PhotoRecord.thumbnailFilename(id))

        scope.launch {
            try {
                photoFile.writeBytes(data)

                // 生成缩略图并获取尺寸
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                var imageWidth: Int? = null
                var imageHeight: Int? = null

                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                if (bitmap != null) {
                    imageWidth = bitmap.width * options.inSampleSize
                    imageHeight = bitmap.height * options.inSampleSize
                    try {
                        val thumb = ThumbnailGenerator.generate(bitmap)
                        thumbFile.outputStream().use { out ->
                            thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        }
                        thumb.recycle()
                    } finally {
                        bitmap.recycle()
                    }
                }

                val record = PhotoRecord(
                    id = id,
                    creationDate = System.currentTimeMillis(),
                    detectionMethod = detectionMethod,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight
                )
                val updated = listOf(record) + _records.value
                _records.value = updated
                persist(updated)
            } catch (e: Exception) {
                AppLogger.e("PhotoStorage", "保存照片失败", e)
            }
        }
    }

    /**
     * 更新已有照片的数据（覆盖原文件、重新生成缩略图、更新记录元数据）
     *
     * @param photoId 现有照片的 ID
     * @param newData 新的 JPEG 字节数据
     * @param detectionMethod 编辑方法标签（用于记录更新）
     * @return 是否成功更新
     */
    suspend fun updatePhoto(photoId: String, newData: ByteArray, detectionMethod: String? = null): Boolean {
        return try {
            val photoFile = File(photosDir, PhotoRecord.photoFilename(photoId))
            val thumbFile = File(thumbnailsDir, PhotoRecord.thumbnailFilename(photoId))

            // 覆盖原照片文件
            photoFile.writeBytes(newData)

            // 重新生成缩略图并获取尺寸
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            var imageWidth: Int? = null
            var imageHeight: Int? = null

            val bitmap = BitmapFactory.decodeByteArray(newData, 0, newData.size, options)
            if (bitmap != null) {
                imageWidth = bitmap.width * options.inSampleSize
                imageHeight = bitmap.height * options.inSampleSize
                try {
                    val thumb = ThumbnailGenerator.generate(bitmap)
                    thumbFile.outputStream().use { out ->
                        thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    thumb.recycle()
                } finally {
                    bitmap.recycle()
                }
            }

            // 更新记录元数据
            updateRecord(photoId) { record ->
                record.copy(
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    detectionMethod = detectionMethod ?: record.detectionMethod
                )
            }
            true
        } catch (e: Exception) {
            AppLogger.e("PhotoStorage", "更新照片失败: $photoId", e)
            false
        }
    }

    fun deleteRecord(id: String) {
        scope.launch {
            val photoFile = File(photosDir, PhotoRecord.photoFilename(id))
            val thumbFile = File(thumbnailsDir, PhotoRecord.thumbnailFilename(id))
            photoFile.delete()
            thumbFile.delete()
            val updated = _records.value.filter { it.id != id }
            _records.value = updated
            persist(updated)
        }
    }

    fun getThumbnail(id: String): Bitmap? {
        val thumbFile = File(thumbnailsDir, PhotoRecord.thumbnailFilename(id))
        return if (thumbFile.exists()) BitmapFactory.decodeFile(thumbFile.absolutePath) else null
    }

    fun getPhotoFile(id: String): File {
        return File(photosDir, PhotoRecord.photoFilename(id))
    }

    fun updateRecord(id: String, transform: (PhotoRecord) -> PhotoRecord) {
        val current = _records.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = transform(current[index])
            _records.value = current
            scope.launch { persist(current) }
        }
    }

    private suspend fun persist(records: List<PhotoRecord>) {
        saveMutex.withLock {
            try {
                val json = gson.toJson(records)
                // 加密后存储，保护用户照片元数据隐私
                val encrypted = CryptoHelper.encrypt(json) ?: json
                recordsFile.writeText(encrypted)
            } catch (e: Exception) {
                AppLogger.e("PhotoStorage", "持久化记录失败", e)
            }
        }
    }
}

/**
 * 缩略图生成器
 */
object ThumbnailGenerator {
    fun generate(bitmap: Bitmap, maxDimension: Int = 300): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}