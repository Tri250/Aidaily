package com.livecompose.livecapture.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.storage.PhotoStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storageService: PhotoStorageService
) : ViewModel() {

    private val _records = MutableStateFlow<List<PhotoRecord>>(emptyList())
    val records: StateFlow<List<PhotoRecord>> = _records

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            _records.value = storageService.getAllRecords()
        }
    }

    fun deleteRecord(record: PhotoRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            storageService.deleteRecordAsync(record)
            _records.value = storageService.getAllRecords()
        }
    }
}
