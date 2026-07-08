package com.livecompose.livecapture.features.home

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
            storage.getThumbnail(id)
        } catch (e: Exception) {
            Log.e(TAG, "获取缩略图失败: $id", e)
            null
        }
    }

    suspend fun getFullPhoto(id: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = storage.getPhotoFile(id)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (e: Exception) {
            Log.e(TAG, "获取原图失败: $id", e)
            null
        }
    }

    fun updateRating(id: String, rating: Int) {
        storage.updateRecord(id) { it.copy(rating = rating.coerceIn(0, 5)) }
    }

    fun toggleFlag(id: String) {
        storage.updateRecord(id) { it.copy(flag = !it.flag) }
    }
}