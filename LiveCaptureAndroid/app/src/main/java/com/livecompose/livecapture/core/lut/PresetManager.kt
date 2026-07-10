package com.livecompose.livecapture.core.lut

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 预设管理器
 *
 * 管理两类预设：
 * 1. 本地 LUT 预设（用户自定义或导入的 .cube/.png LUT 文件）
 * 2. 大师预设（从 OMaster Community 远程加载的 OPPO/Realme 预设）
 */
class PresetManager(private val cacheDir: File) {

    private val presetEngine = MasterPresetEngine()
    private val remoteLoader = PresetRemoteLoader(cacheDir)

    /** 所有大师预设集合 */
    private var masterPresetCollections: List<MasterPresetCollection> = emptyList()

    /** 所有预设（扁平化） */
    private var allPresets: List<MasterPreset> = emptyList()

    /** 当前选中的预设 */
    var currentPreset: MasterPreset? = null
        private set

    /** 设置当前预设 */
    fun setCurrentPreset(preset: MasterPreset) {
        currentPreset = preset
    }

    /** 预设整体强度 0.0~1.0 */
    var presetIntensity: Float = 1.0f

    /** 是否正在加载 */
    var isLoading: Boolean = false
        private set

    /** 加载状态回调 */
    var onLoadStateChanged: ((Boolean) -> Unit)? = null

    /**
     * 初始化：加载大师预设
     */
    suspend fun initialize() {
        setLoading(true)
        try {
            masterPresetCollections = remoteLoader.loadAllPresets()
            allPresets = masterPresetCollections.flatMap { it.presets }
        } catch (e: Exception) {
            masterPresetCollections = emptyList()
            allPresets = emptyList()
        }
        setLoading(false)
    }

    fun getAllMasterPresets(): List<MasterPreset> = allPresets

    fun getMasterPresetCollections(): List<MasterPresetCollection> = masterPresetCollections

    fun findPresetByName(name: String): MasterPreset? = allPresets.find { it.name == name }

    suspend fun selectAndApplyPreset(preset: MasterPreset, bitmap: Bitmap): Bitmap {
        currentPreset = preset
        preset.useCount++
        return applyPreset(preset, bitmap, presetIntensity)
    }

    suspend fun applyPreset(preset: MasterPreset, bitmap: Bitmap, intensity: Float = 1.0f): Bitmap {
        val params = remoteLoader.parseParams(preset)
        return presetEngine.applyPreset(bitmap, params, intensity)
    }

    suspend fun applyCurrentPreset(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val preset = currentPreset ?: return@withContext bitmap
        applyPreset(preset, bitmap, presetIntensity)
    }

    fun toggleFavorite(preset: MasterPreset) {
        preset.isFavorited = !preset.isFavorited
    }

    fun getFavoritedPresets(): List<MasterPreset> = allPresets.filter { it.isFavorited }

    fun recommendForScene(sceneType: String, topN: Int = 3): List<MasterPreset> {
        return ScenePresetMapper.recommendPresets(sceneType, allPresets, topN)
    }

    fun getPopularPresets(topN: Int = 10): List<MasterPreset> {
        return allPresets.filter { it.useCount > 0 }
            .sortedByDescending { it.useCount }
            .take(topN)
    }

    fun getNewPresets(): List<MasterPreset> = allPresets.filter { it.isNew }

    fun clearCurrentPreset() {
        currentPreset = null
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        onLoadStateChanged?.invoke(loading)
    }
}