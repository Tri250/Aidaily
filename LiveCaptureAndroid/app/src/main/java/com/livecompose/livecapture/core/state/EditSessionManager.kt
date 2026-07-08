package com.livecompose.livecapture.core.state

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

private val Context.editSessionDataStore by preferencesDataStore(name = "edit_session")

/**
 * 编辑会话数据
 */
data class EditSession(
    val photoId: String,
    val rotation: Int = 0,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val cropRatio: String = "original",
    val lastSavedTimestamp: Long = System.currentTimeMillis()
)

/**
 * 编辑会话管理器
 * 自动保存编辑状态到临时文件，支持草稿恢复
 */
class EditSessionManager(private val context: Context) {

    private val dataStore = context.editSessionDataStore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSaveJob: Job? = null

    private val tempDir = File(context.cacheDir, "edit_sessions").also { it.mkdirs() }

    companion object {
        private const val AUTO_SAVE_INTERVAL_MS = 30_000L

        // DataStore keys
        private val KEY_HAS_SESSION = booleanPreferencesKey("has_session")
        private val KEY_PHOTO_ID = stringPreferencesKey("photo_id")
        private val KEY_ROTATION = intPreferencesKey("rotation")
        private val KEY_SCALE = doublePreferencesKey("scale")
        private val KEY_OFFSET_X = doublePreferencesKey("offset_x")
        private val KEY_OFFSET_Y = doublePreferencesKey("offset_y")
        private val KEY_CROP_RATIO = stringPreferencesKey("crop_ratio")
        private val KEY_LAST_SAVED = longPreferencesKey("last_saved_timestamp")
    }

    /**
     * 保存当前编辑会话
     */
    suspend fun saveSession(session: EditSession) {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_SESSION] = true
            preferences[KEY_PHOTO_ID] = session.photoId
            preferences[KEY_ROTATION] = session.rotation
            preferences[KEY_SCALE] = session.scale.toDouble()
            preferences[KEY_OFFSET_X] = session.offsetX.toDouble()
            preferences[KEY_OFFSET_Y] = session.offsetY.toDouble()
            preferences[KEY_CROP_RATIO] = session.cropRatio
            preferences[KEY_LAST_SAVED] = session.lastSavedTimestamp
        }
    }

    /**
     * 保存编辑后的 Bitmap 到临时文件
     */
    suspend fun saveTempBitmap(photoId: String, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                val tempFile = getTempFile(photoId)
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
            } catch (e: Exception) {
                // 静默处理保存失败
            }
        }
    }

    /**
     * 加载已保存的编辑会话
     */
    suspend fun loadSession(): EditSession? {
        val preferences = dataStore.data.first()
        val hasSession = preferences[KEY_HAS_SESSION] ?: false
        if (!hasSession) return null

        return EditSession(
            photoId = preferences[KEY_PHOTO_ID] ?: return null,
            rotation = preferences[KEY_ROTATION] ?: 0,
            scale = (preferences[KEY_SCALE] ?: 1.0).toFloat(),
            offsetX = (preferences[KEY_OFFSET_X] ?: 0.0).toFloat(),
            offsetY = (preferences[KEY_OFFSET_Y] ?: 0.0).toFloat(),
            cropRatio = preferences[KEY_CROP_RATIO] ?: "original",
            lastSavedTimestamp = preferences[KEY_LAST_SAVED] ?: 0L
        )
    }

    /**
     * 加载临时保存的 Bitmap
     */
    suspend fun loadTempBitmap(photoId: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val tempFile = getTempFile(photoId)
                if (tempFile.exists()) {
                    BitmapFactory.decodeFile(tempFile.absolutePath)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 清除当前编辑会话
     */
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        // 清理临时文件
        withContext(Dispatchers.IO) {
            try {
                tempDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                // 静默处理
            }
        }
        stopAutoSave()
    }

    /**
     * 检查是否有未完成的编辑会话
     */
    suspend fun hasSession(): Boolean {
        val preferences = dataStore.data.first()
        return preferences[KEY_HAS_SESSION] ?: false
    }

    /**
     * 获取会话状态流
     */
    fun hasSessionFlow(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_HAS_SESSION] ?: false
        }
    }

    /**
     * 开始定时自动保存
     */
    fun startAutoSave(
        photoId: String,
        getCurrentState: () -> EditSession
    ) {
        stopAutoSave()
        autoSaveJob = scope.launch {
            while (isActive) {
                delay(AUTO_SAVE_INTERVAL_MS)
                try {
                    val session = getCurrentState()
                    saveSession(session)
                } catch (e: Exception) {
                    // 静默处理自动保存失败
                }
            }
        }
    }

    /**
     * 停止定时自动保存
     */
    fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    /**
     * 应用退出时保存
     */
    suspend fun saveOnExit(photoId: String, getCurrentState: () -> EditSession) {
        try {
            val session = getCurrentState()
            saveSession(session)
        } catch (e: Exception) {
            // 静默处理
        }
        stopAutoSave()
    }

    /**
     * 获取临时文件路径
     */
    private fun getTempFile(photoId: String): File {
        return File(tempDir, "${photoId}_edit_temp.jpg")
    }

    /**
     * 销毁管理器，清理资源
     */
    fun destroy() {
        stopAutoSave()
        scope.cancel()
    }
}