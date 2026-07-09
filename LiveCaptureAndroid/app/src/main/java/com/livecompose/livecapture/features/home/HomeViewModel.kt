package com.livecompose.livecapture.features.home

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.livecompose.livecapture.core.logger.AppLogger
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.storage.PhotoStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 图库 ViewModel
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val storage = PhotoStorageService(application.applicationContext)

    // 缩略图 LRU 缓存，避免重复解码
    private val thumbnailCache = LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt())

    // 原图 LRU 缓存
    private val fullPhotoCache = LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 4).toInt())

    private val _records = MutableStateFlow<List<PhotoRecord>>(emptyList())
    val records: StateFlow<List<PhotoRecord>> = _records.asStateFlow()

    init {
        viewModelScope.launch {
            storage.records.collect { list ->
                _records.value = list.sortedByDescending { it.creationDate }
            }
        }
    }

    fun deleteRecord(id: String) {
        storage.deleteRecord(id)
    }

    fun deleteRecords(ids: List<String>) {
        ids.forEach { storage.deleteRecord(it) }
    }

    suspend fun getThumbnail(id: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 先从缓存读取
            thumbnailCache.get(id)?.let { return@withContext it }
            val file = storage.getPhotoFile(id)
            if (!file.exists()) return@withContext null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 256)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            bitmap?.let { thumbnailCache.put(id, it) }
            bitmap
        } catch (e: Exception) { AppLogger.e(TAG, "获取缩略图失败: $id", e); null }
    }

    suspend fun getFullPhoto(id: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            fullPhotoCache.get(id)?.let { return@withContext it }
            val file = storage.getPhotoFile(id)
            if (!file.exists()) return@withContext null
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, 2048)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            bitmap?.let { fullPhotoCache.put(id, it) }
            bitmap
        } catch (e: Exception) { AppLogger.e(TAG, "获取原图失败: $id", e); null }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqSize: Int): Int {
        var inSampleSize = 1
        if (height > reqSize || width > reqSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqSize && halfWidth / inSampleSize >= reqSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun updateRating(id: String, rating: Int) {
        storage.updateRecord(id) { it.copy(rating = rating.coerceIn(0, 5)) }
    }

    fun toggleFlag(id: String) {
        storage.updateRecord(id) { it.copy(flag = !it.flag) }
    }

    suspend fun saveProcessedBitmap(photoId: String, bitmap: Bitmap, method: String = "编辑") {
        withContext(Dispatchers.IO) {
            val bytes = java.io.ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.toByteArray()
            }
            storage.updatePhoto(photoId, bytes, method)
            fullPhotoCache.remove(photoId)
            thumbnailCache.remove(photoId)
        }
    }
}