package com.livecompose.livecapture.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.share.ShareService
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.storage.PhotoStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storageService: PhotoStorageService,
    private val shareService: ShareService
) : ViewModel() {

    val records: StateFlow<List<PhotoRecord>> = storageService.getAllRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteRecord(record: PhotoRecord) {
        viewModelScope.launch {
            storageService.deleteRecordAsync(record)
        }
    }

    fun sharePhoto(record: PhotoRecord, context: Context) {
        shareService.sharePhoto(record.filePath, context)
    }

    fun shareToWechat(record: PhotoRecord, context: Context) {
        shareService.sharePhotoToWechat(record.filePath, context)
    }

    fun isWechatInstalled(context: Context): Boolean {
        return shareService.isWechatInstalled(context)
    }

    fun getPhotoForEditing(record: PhotoRecord): String {
        return record.filePath
    }

    fun loadRecords() {
        // Room Flow 自动更新，此方法为空操作
        // 保留以兼容 HomeView 的 ON_RESUME 刷新逻辑
    }
}
