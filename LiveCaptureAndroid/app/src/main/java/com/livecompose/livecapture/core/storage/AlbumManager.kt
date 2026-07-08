package com.livecompose.livecapture.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

private val Context.albumDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "album_manager"
)

/**
 * 相册数据模型
 */
data class Album(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val photoIds: MutableList<String> = mutableListOf(),
    val coverPhotoId: String? = null
) {
    val photoCount: Int get() = photoIds.size
}

/**
 * 相册管理器
 * 创建/删除/重命名相册，移动照片到相册
 * 使用 DataStore 持久化相册信息
 */
class AlbumManager(private val context: Context) {

    private val gson = Gson()
    private val store = context.albumDataStore

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // 从 DataStore 加载数据
        scope.launch {
            loadAlbums()
        }
    }

    /**
     * 从 DataStore 加载相册列表
     */
    private suspend fun loadAlbums() {
        val albumsMap = store.data.first()
        val albumsJson = albumsMap[ALBUMS_KEY] ?: return
        try {
            val type = object : TypeToken<List<Album>>() {}.type
            val loaded: List<Album> = gson.fromJson(albumsJson, type)
            _albums.value = loaded
        } catch (e: Exception) {
            _albums.value = emptyList()
        }
    }

    /**
     * 持久化相册列表到 DataStore
     */
    private suspend fun persistAlbums(albums: List<Album>) {
        val json = gson.toJson(albums)
        store.edit { preferences ->
            preferences[ALBUMS_KEY] = json
        }
    }

    /**
     * 创建新相册
     */
    suspend fun createAlbum(name: String, description: String = ""): Album {
        val album = Album(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        val updated = _albums.value + album
        _albums.value = updated
        persistAlbums(updated)
        return album
    }

    /**
     * 删除相册
     */
    suspend fun deleteAlbum(albumId: String): Boolean {
        val album = _albums.value.find { it.id == albumId } ?: return false
        val updated = _albums.value.filter { it.id != albumId }
        _albums.value = updated
        persistAlbums(updated)
        return true
    }

    /**
     * 重命名相册
     */
    suspend fun renameAlbum(albumId: String, newName: String): Boolean {
        val updated = _albums.value.map { album ->
            if (album.id == albumId) album.copy(name = newName) else album
        }
        _albums.value = updated
        persistAlbums(updated)
        return true
    }

    /**
     * 添加照片到相册
     */
    suspend fun addPhotoToAlbum(albumId: String, photoId: String): Boolean {
        val updated = _albums.value.map { album ->
            if (album.id == albumId) {
                val photoIds = album.photoIds.toMutableList()
                if (!photoIds.contains(photoId)) {
                    photoIds.add(photoId)
                    album.copy(
                        photoIds = photoIds,
                        coverPhotoId = album.coverPhotoId ?: photoId
                    )
                } else {
                    album
                }
            } else {
                album
            }
        }
        _albums.value = updated
        persistAlbums(updated)
        return true
    }

    /**
     * 从相册中移除照片
     */
    suspend fun removePhotoFromAlbum(albumId: String, photoId: String): Boolean {
        val updated = _albums.value.map { album ->
            if (album.id == albumId) {
                val photoIds = album.photoIds.toMutableList()
                photoIds.remove(photoId)
                album.copy(
                    photoIds = photoIds,
                    coverPhotoId = if (album.coverPhotoId == photoId) photoIds.firstOrNull() else album.coverPhotoId
                )
            } else {
                album
            }
        }
        _albums.value = updated
        persistAlbums(updated)
        return true
    }

    /**
     * 移动照片到另一个相册
     */
    suspend fun movePhotoToAlbum(
        photoId: String,
        fromAlbumId: String,
        toAlbumId: String
    ): Boolean {
        // 从源相册移除
        removePhotoFromAlbum(fromAlbumId, photoId)
        // 添加到目标相册
        addPhotoToAlbum(toAlbumId, photoId)
        return true
    }

    /**
     * 获取相册列表
     */
    fun getAlbums(): List<Album> = _albums.value

    /**
     * 获取指定相册
     */
    fun getAlbum(albumId: String): Album? {
        return _albums.value.find { it.id == albumId }
    }

    /**
     * 获取相册中的照片 ID 列表
     */
    fun getPhotosInAlbum(albumId: String): List<String> {
        return _albums.value.find { it.id == albumId }?.photoIds ?: emptyList()
    }

    /**
     * 获取包含指定照片的相册列表
     */
    fun getAlbumsContainingPhoto(photoId: String): List<Album> {
        return _albums.value.filter { it.photoIds.contains(photoId) }
    }

    /**
     * 获取相册照片数量
     */
    fun getAlbumPhotoCount(albumId: String): Int {
        return _albums.value.find { it.id == albumId }?.photoCount ?: 0
    }

    /**
     * 设置相册封面
     */
    suspend fun setCoverPhoto(albumId: String, photoId: String): Boolean {
        val updated = _albums.value.map { album ->
            if (album.id == albumId) album.copy(coverPhotoId = photoId) else album
        }
        _albums.value = updated
        persistAlbums(updated)
        return true
    }

    /**
     * 更新相册描述
     */
    suspend fun updateDescription(albumId: String, description: String): Boolean {
        val updated = _albums.value.map { album ->
            if (album.id == albumId) album.copy(description = description) else album
        }
        _albums.value = updated
        persistAlbums(updated)
        return true
    }

    companion object {
        private val ALBUMS_KEY = stringPreferencesKey("albums_data")
    }
}