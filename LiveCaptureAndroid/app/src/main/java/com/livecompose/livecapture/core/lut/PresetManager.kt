package com.livecompose.livecapture.core.lut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.UUID

private val Context.presetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "preset_manager"
)

/**
 * 预设数据模型
 */
data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "自定义",
    val description: String = "",
    val params: ColorRecipeParams,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false
)

/**
 * 预设管理器
 * 保存/加载/删除/重命名预设
 * 导出为 .cube 文件
 * 导入 .cube 文件
 */
class PresetManager(private val context: Context) {

    private val gson = Gson()
    private val store = context.presetDataStore

    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    private val thumbnailsDir = File(context.filesDir, "preset_thumbnails").also {
        it.mkdirs()
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            loadPresets()
        }
    }

    /**
     * 从 DataStore 加载预设列表
     */
    private suspend fun loadPresets() {
        val preferences = store.data.first()
        val presetsJson = preferences[PRESETS_KEY] ?: return
        try {
            val type = object : TypeToken<List<Preset>>() {}.type
            val loaded: List<Preset> = gson.fromJson(presetsJson, type)
            _presets.value = loaded
        } catch (e: Exception) {
            _presets.value = emptyList()
        }
    }

    /**
     * 持久化预设列表
     */
    private suspend fun persistPresets(presets: List<Preset>) {
        val json = gson.toJson(presets)
        store.edit { preferences ->
            preferences[PRESETS_KEY] = json
        }
    }

    /**
     * 保存当前参数为自定义预设
     */
    suspend fun savePreset(
        name: String,
        params: ColorRecipeParams,
        category: String = "自定义",
        description: String = "",
        thumbnail: Bitmap? = null
    ): Preset {
        // 保存缩略图
        val thumbnailPath = if (thumbnail != null) {
            saveThumbnail(thumbnail)
        } else {
            null
        }

        val preset = Preset(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            description = description,
            params = params,
            thumbnailPath = thumbnailPath,
            createdAt = System.currentTimeMillis(),
            isBuiltIn = false
        )

        val updated = _presets.value + preset
        _presets.value = updated
        persistPresets(updated)

        return preset
    }

    /**
     * 加载预设（返回预设参数）
     */
    fun loadPreset(presetId: String): ColorRecipeParams? {
        return _presets.value.find { it.id == presetId }?.params
    }

    /**
     * 删除预设
     */
    suspend fun deletePreset(presetId: String): Boolean {
        val preset = _presets.value.find { it.id == presetId } ?: return false
        if (preset.isBuiltIn) return false // 内置预设不可删除

        // 删除缩略图
        preset.thumbnailPath?.let {
            File(it).delete()
        }

        val updated = _presets.value.filter { it.id != presetId }
        _presets.value = updated
        persistPresets(updated)
        return true
    }

    /**
     * 重命名预设
     */
    suspend fun renamePreset(presetId: String, newName: String): Boolean {
        val updated = _presets.value.map { preset ->
            if (preset.id == presetId) preset.copy(name = newName) else preset
        }
        _presets.value = updated
        persistPresets(updated)
        return true
    }

    /**
     * 获取所有预设
     */
    fun getAllPresets(): List<Preset> = _presets.value

    /**
     * 按分类获取预设
     */
    fun getPresetsByCategory(category: String): List<Preset> {
        return _presets.value.filter { it.category == category }
    }

    /**
     * 获取所有分类
     */
    fun getAllCategories(): List<String> {
        return _presets.value.map { it.category }.distinct()
    }

    /**
     * 获取预设缩略图
     */
    fun getPresetThumbnail(presetId: String): Bitmap? {
        val preset = _presets.value.find { it.id == presetId } ?: return null
        val path = preset.thumbnailPath ?: return null
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 导出预设为 .cube 文件
     */
    suspend fun exportToCubeFile(presetId: String, outputFile: File): Boolean {
        val preset = _presets.value.find { it.id == presetId } ?: return false
        return try {
            val cubeContent = generateCubeContent(preset)
            outputFile.writeText(cubeContent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从 .cube 文件导入预设
     */
    suspend fun importFromCubeFile(cubeFile: File, name: String? = null): Preset? {
        return try {
            val content = cubeFile.readText()
            val params = parseCubeContent(content)
            val presetName = name ?: cubeFile.nameWithoutExtension

            val preset = Preset(
                id = UUID.randomUUID().toString(),
                name = presetName,
                category = "导入",
                description = "从 ${cubeFile.name} 导入",
                params = params,
                createdAt = System.currentTimeMillis(),
                isBuiltIn = false
            )

            val updated = _presets.value + preset
            _presets.value = updated
            persistPresets(updated)
            preset
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 导出预设到文件（返回文件路径）
     */
    suspend fun exportPreset(presetId: String): File? {
        val preset = _presets.value.find { it.id == presetId } ?: return null
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val fileName = "${preset.name.replace(" ", "_")}.cube"
        val exportFile = File(exportDir, fileName)
        return if (exportToCubeFile(presetId, exportFile)) exportFile else null
    }

    /**
     * 生成 .cube 文件内容
     */
    private fun generateCubeContent(preset: Preset): String {
        val sb = StringBuilder()
        sb.appendLine("# LUT generated by LiveCapture")
        sb.appendLine("# Preset: ${preset.name}")
        sb.appendLine("# Category: ${preset.category}")
        sb.appendLine("# Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(preset.createdAt))}")
        sb.appendLine()
        sb.appendLine("TITLE \"${preset.name}\"")
        sb.appendLine()

        // 使用 33x33x33 LUT
        sb.appendLine("LUT_3D_SIZE 33")
        sb.appendLine()

        val params = preset.params
        val size = 33

        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    val rNorm = r.toFloat() / (size - 1)
                    val gNorm = g.toFloat() / (size - 1)
                    val bNorm = b.toFloat() / (size - 1)

                    // 应用预设参数
                    val adjustedR = applyPresetAdjustment(rNorm, params)
                    val adjustedG = applyPresetAdjustment(gNorm, params)
                    val adjustedB = applyPresetAdjustment(bNorm, params)

                    sb.appendLine("${formatFloat(adjustedR)} ${formatFloat(adjustedG)} ${formatFloat(adjustedB)}")
                }
            }
        }

        return sb.toString()
    }

    /**
     * 解析 .cube 文件内容
     */
    private fun parseCubeContent(content: String): ColorRecipeParams {
        var size = 33
        val rgbValues = mutableListOf<FloatArray>()

        for (line in content.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#") || trimmed.isEmpty() -> continue
                trimmed.startsWith("TITLE") -> continue
                trimmed.startsWith("LUT_3D_SIZE") -> {
                    size = trimmed.split("\\s+".toRegex()).lastOrNull()?.toIntOrNull() ?: 33
                }
                trimmed.startsWith("DOMAIN_MIN") -> continue
                trimmed.startsWith("DOMAIN_MAX") -> continue
                else -> {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 3) {
                        val r = parts[0].toFloatOrNull() ?: continue
                        val g = parts[1].toFloatOrNull() ?: continue
                        val b = parts[2].toFloatOrNull() ?: continue
                        rgbValues.add(floatArrayOf(r, g, b))
                    }
                }
            }
        }

        // 从 LUT 数据中提取平均调整参数
        return extractAverageParams(rgbValues)
    }

    /**
     * 从 LUT 数据中提取平均参数
     */
    private fun extractAverageParams(rgbValues: List<FloatArray>): ColorRecipeParams {
        if (rgbValues.isEmpty()) return ColorRecipeParams.DEFAULT

        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        var count = 0

        for (rgb in rgbValues) {
            totalR += rgb[0]
            totalG += rgb[1]
            totalB += rgb[2]
            count++
        }

        if (count == 0) return ColorRecipeParams.DEFAULT

        val avgR = totalR / count
        val avgG = totalG / count
        val avgB = totalB / count

        // 根据平均偏移估算参数
        val exposure = (avgR + avgG + avgB) / 3f - 0.5f
        val saturation = (avgR - avgG).coerceIn(-0.5f, 0.5f) + 1f
        val contrast = ((avgR - avgB).coerceIn(-0.3f, 0.3f) + 1f)

        return ColorRecipeParams(
            exposure = exposure.coerceIn(-2f, 2f),
            contrast = (contrast - 1f) * 100f,
            saturation = (saturation - 1f) * 100f
        )
    }

    /**
     * 应用预设参数调整
     */
    private fun applyPresetAdjustment(value: Float, params: ColorRecipeParams): Float {
        var adjusted = value

        // 曝光
        adjusted += params.exposure * 0.5f

        // 对比度
        adjusted = (adjusted - 0.5f) * (1f + params.contrast / 100f) + 0.5f

        // 高光
        if (adjusted > 0.5f) {
            adjusted += params.highlights / 100f * 0.3f
        }

        // 阴影
        if (adjusted < 0.5f) {
            adjusted += params.shadows / 100f * 0.3f
        }

        return adjusted.coerceIn(0f, 1f)
    }

    /**
     * 保存缩略图
     */
    private fun saveThumbnail(bitmap: Bitmap): String {
        val id = UUID.randomUUID().toString()
        val thumbFile = File(thumbnailsDir, "${id}.jpg")
        try {
            thumbFile.outputStream().use { out ->
                val scaled = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                // 仅当 createScaledBitmap 返回新实例时才回收，避免误回收调用者传入的 bitmap
                if (scaled !== bitmap) {
                    scaled.recycle()
                }
            }
        } catch (e: Exception) {
            // 缩略图保存失败不影响主流程
        }
        return thumbFile.absolutePath
    }

    private fun formatFloat(value: Float): String {
        return String.format("%.6f", value)
    }

    companion object {
        private val PRESETS_KEY = stringPreferencesKey("presets_data")
    }
}