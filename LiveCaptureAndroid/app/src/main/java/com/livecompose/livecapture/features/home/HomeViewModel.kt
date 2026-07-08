package com.livecompose.livecapture.features.home

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.storage.PhotoStorageService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 图库 ViewModel
 * 对应 iOS 的 HomeViewModel
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

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

    fun getThumbnail(id: String): Bitmap? {
        return storage.getThumbnail(id)
    }

    fun getFullPhoto(id: String): Bitmap? {
        val file = storage.getPhotoFile(id)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun updateRating(id: String, rating: Int) {
        storage.updateRecord(id) { it.copy(rating = rating.coerceIn(0, 5)) }
    }

    fun toggleFlag(id: String) {
        storage.updateRecord(id) { it.copy(flag = !it.flag) }
    }
}