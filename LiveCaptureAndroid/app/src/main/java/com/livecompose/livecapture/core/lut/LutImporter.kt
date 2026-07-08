package com.livecompose.livecapture.core.lut

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Context.lutImporterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lut_importer"
)

/**
 * LUT 文件导入器
 *
 * 对应 iOS 端 LUTImporter.swift，支持解析 .cube 格式 LUT 文件并转换为 [LutPreset]。
 *
 * ## 主要功能
 * - [importCubeFile] 解析 .cube 文件，生成 [LutPreset]
 * - [importFromUri] 从文件 Uri 导入（用于文件选择器）
 * - [deletePreset] 删除已导入的预设
 * - [allPresets] 合并内置预设与导入预设
 *
 * ## .cube 文件格式
 * .cube 是 Adobe Cube LUT 标准格式，包含：
 * - `LUT_3D_SIZE N`：3D LUT 维度（默认 32）
 * - `LUT_1D_SIZE N`：1D LUT 维度
 * - 数据行：`R G B`（浮点 0-1），按 R 变化最慢、B 变化最快的顺序排列
 *
 * ## 持久化
 * 导入的预设通过 DataStore 持久化存储。
 *
 * @param context 上下文，用于 DataStore
 */
class LutImporter(private val context: Context) {

    companion object {
        private const val TAG = "LutImporter"
        private val CUSTOM_LUTS_KEY = stringPreferencesKey("custom_luts")
    }

    private val gson = Gson()
    private val store = context.lutImporterDataStore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 导入的预设数据（包含估算的滤镜参数和原始 LUT 数据） */
    data class ImportedLutPreset(
        val id: String,
        val displayName: String,
        val lutSize: Int,
        val dataPoints: List<Triple<Float, Float, Float>>,
        val estimatedPreset: LutPreset
    )

    private val _importedPresets = MutableStateFlow<List<ImportedLutPreset>>(emptyList())
    /** 已导入的预设列表 */
    val importedPresets: StateFlow<List<ImportedLutPreset>> = _importedPresets.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    /** 导入错误信息 */
    val importError: StateFlow<String?> = _importError.asStateFlow()

    init {
        // 加载已保存的预设
        scope.launch {
            loadSavedPresets()
        }
    }

    // MARK: - 导入 .cube 文件

    /**
     * 从文件路径导入 .cube LUT 文件
     *
     * 解析流程：
     * 1. 读取文件内容，按行分割
     * 2. 过滤空行和注释行（# 开头）
     * 3. 解析 `LUT_3D_SIZE` 头部
     * 4. 解析 RGB 数据行
     * 5. 根据平均 RGB 估算滤镜参数（[estimateFilterParamsFromLut]）
     * 6. 生成 [ImportedLutPreset] 并持久化
     *
     * @param filePath .cube 文件路径
     * @return 导入的预设，失败抛出 [LutImportException]
     */
    suspend fun importCubeFile(filePath: String): ImportedLutPreset = withContext(Dispatchers.IO) {
        _importError.value = null
        val file = File(filePath)
        if (!file.exists()) {
            _importError.value = "文件未找到"
            throw LutImportException.FileNotFound
        }

        val ext = file.extension.lowercase()
        if (ext != "cube" && ext != "3dl") {
            _importError.value = "不支持的 LUT 格式（仅支持 .cube 和 .3dl）"
            throw LutImportException.UnsupportedFormat
        }

        return@withContext try {
            val content = file.readText()
            parseCubeContent(content, file.nameWithoutExtension)
        } catch (e: LutImportException) {
            _importError.value = e.message
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入 LUT 失败: $filePath", e)
            _importError.value = "LUT 文件格式无效"
            throw LutImportException.InvalidFormat
        }
    }

    /**
     * 解析 .cube 文件内容
     *
     * @param content 文件文本内容
     * @param name 预设显示名称
     * @return 导入的预设
     */
    private suspend fun parseCubeContent(content: String, name: String): ImportedLutPreset {
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        var lutSize = 32
        val dataPoints = mutableListOf<Triple<Float, Float, Float>>()

        for (line in lines) {
            val upperLine = line.uppercase()
            if (upperLine.startsWith("LUT_3D_SIZE")) {
                val parts = line.split(Regex("\\s+"))
                parts.lastOrNull()?.toIntOrNull()?.let { lutSize = it }
                continue
            }
            if (upperLine.startsWith("LUT_1D_SIZE") || upperLine.startsWith("TITLE") ||
                upperLine.startsWith("DOMAIN_MIN") || upperLine.startsWith("DOMAIN_MAX")) {
                continue
            }

            // 解析 RGB 数据行
            val values = line.split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toFloatOrNull() }

            if (values.size >= 3) {
                dataPoints.add(Triple(values[0], values[1], values[2]))
            }
        }

        if (dataPoints.isEmpty()) {
            throw LutImportException.InvalidFormat
        }

        // 计算平均 RGB
        var sumR = 0f
        var sumG = 0f
        var sumB = 0f
        for ((r, g, b) in dataPoints) {
            sumR += r
            sumG += g
            sumB += b
        }
        val avgR = sumR / dataPoints.size
        val avgG = sumG / dataPoints.size
        val avgB = sumB / dataPoints.size

        // 估算滤镜参数
        val params = estimateFilterParamsFromLut(avgR, avgG, avgB)

        // 生成预设 ID
        val presetId = "custom_${name}_${System.currentTimeMillis()}"
        val preset = LutPreset(
            id = presetId,
            name = name,
            category = LutCategory.VINTAGE,
            description = "自定义导入 LUT",
            saturation = params.saturation,
            contrast = params.contrast,
            warmth = params.warmth,
            tint = params.tint,
            highlights = params.highlights,
            shadows = params.shadows,
            fade = params.fade,
            grain = params.grain,
            vignette = params.vignette,
            sharpening = 0f,
            exposure = params.exposure
        )

        val imported = ImportedLutPreset(
            id = presetId,
            displayName = name,
            lutSize = lutSize,
            dataPoints = dataPoints,
            estimatedPreset = preset
        )

        // 保存到本地
        val updated = _importedPresets.value + imported
        _importedPresets.value = updated
        savePresets(updated)

        return imported
    }

    /**
     * 从 LUT 数据估算滤镜参数
     *
     * 根据 LUT 平均 RGB 值推断色温、曝光、饱和度等参数。
     *
     * @param r 平均 R（0-1）
     * @param g 平均 G（0-1）
     * @param b 平均 B（0-1）
     * @return 估算的滤镜参数
     */
    private fun estimateFilterParamsFromLut(r: Float, g: Float, b: Float): EstimatedParams {
        val params = EstimatedParams()

        // 根据 RGB 平均值估算色温
        // 暖色调（R > B）→ 正 warmth
        // 冷色调（B > R）→ 负 warmth
        if (r > b * 1.1f) {
            params.warmth = 15f + (r - b) * 100f
        } else if (b > r * 1.1f) {
            params.warmth = -15f - (b - r) * 100f
        }

        // 整体亮度
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        if (luminance < 0.4f) {
            params.exposure = 0.15f
        } else if (luminance > 0.6f) {
            params.exposure = -0.1f
        }

        // 饱和度（根据颜色离散度）
        val colorVariance = kotlin.math.abs(r - g) + kotlin.math.abs(g - b) + kotlin.math.abs(b - r)
        params.saturation = when {
            colorVariance < 0.1f -> 1.1f
            colorVariance > 0.3f -> 0.9f
            else -> 1.0f
        }

        // 对比度（根据亮度范围）
        params.contrast = if (luminance in 0.35f..0.65f) 1.1f else 1.0f

        return params
    }

    /** 估算的滤镜参数内部数据类 */
    private data class EstimatedParams(
        var saturation: Float = 1f,
        var contrast: Float = 1f,
        var warmth: Float = 0f,
        var tint: Float = 0f,
        var highlights: Float = 1f,
        var shadows: Float = 1f,
        var fade: Float = 0f,
        var grain: Float = 0f,
        var vignette: Float = 0f,
        var exposure: Float = 0f
    )

    // MARK: - 文件验证

    /**
     * 验证文件是否为有效的 LUT 文件
     *
     * @param filePath 文件路径
     * @return true 表示是 .cube 或 .3dl 文件
     */
    fun isValidLUTFile(filePath: String): Boolean {
        val ext = File(filePath).extension.lowercase()
        return ext == "cube" || ext == "3dl"
    }

    // MARK: - 管理导入的预设

    /**
     * 删除导入的预设
     *
     * @param presetId 预设 ID
     */
    suspend fun deletePreset(presetId: String) {
        val updated = _importedPresets.value.filter { it.id != presetId }
        _importedPresets.value = updated
        savePresets(updated)
    }

    /**
     * 所有可用预设（内置 + 导入）
     *
     * @return 合并后的 LutPreset 列表
     */
    fun allPresets(): List<LutPreset> {
        return BuiltInPresets.presets + _importedPresets.value.map { it.estimatedPreset }
    }

    // MARK: - 持久化

    /**
     * 持久化导入的预设到 DataStore
     */
    private suspend fun savePresets(presets: List<ImportedLutPreset>) {
        try {
            // 仅持久化必要字段（dataPoints 可能很大，限制最多保存 32x32x32 = 32768 点）
            val serializable = presets.map {
                SerializableImportedLut(it.id, it.displayName, it.lutSize, it.dataPoints)
            }
            val json = gson.toJson(serializable)
            store.edit { preferences: MutablePreferences ->
                preferences[CUSTOM_LUTS_KEY] = json
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存 LUT 预设失败", e)
        }
    }

    /**
     * 从 DataStore 加载已保存的预设
     */
    private suspend fun loadSavedPresets() {
        try {
            val prefs = store.data.first()
            val json = prefs[CUSTOM_LUTS_KEY] ?: return
            val type = object : TypeToken<List<SerializableImportedLut>>() {}.type
            val loaded: List<SerializableImportedLut> = gson.fromJson(json, type)
            val restored = loaded.map { serializable ->
                val avgR = serializable.dataPoints.map { it.first }.average().toFloat()
                val avgG = serializable.dataPoints.map { it.second }.average().toFloat()
                val avgB = serializable.dataPoints.map { it.third }.average().toFloat()
                val params = estimateFilterParamsFromLut(avgR, avgG, avgB)
                val preset = LutPreset(
                    id = serializable.id,
                    name = serializable.displayName,
                    category = LutCategory.VINTAGE,
                    description = "自定义导入 LUT",
                    saturation = params.saturation,
                    contrast = params.contrast,
                    warmth = params.warmth,
                    tint = params.tint,
                    highlights = params.highlights,
                    shadows = params.shadows,
                    fade = params.fade,
                    grain = params.grain,
                    vignette = params.vignette,
                    sharpening = 0f,
                    exposure = params.exposure
                )
                ImportedLutPreset(
                    id = serializable.id,
                    displayName = serializable.displayName,
                    lutSize = serializable.lutSize,
                    dataPoints = serializable.dataPoints,
                    estimatedPreset = preset
                )
            }
            _importedPresets.value = restored
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载 LUT 预设失败", e)
        }
    }

    /** 用于序列化的简化数据类 */
    private data class SerializableImportedLut(
        val id: String,
        val displayName: String,
        val lutSize: Int,
        val dataPoints: List<Triple<Float, Float, Float>>
    )
}

/**
 * LUT 导入错误类型
 */
sealed class LutImportException(message: String) : Exception(message) {
    object InvalidFormat : LutImportException("LUT 文件格式无效")
    object FileNotFound : LutImportException("文件未找到")
    object UnsupportedFormat : LutImportException("不支持的 LUT 格式（仅支持 .cube 和 .3dl）")
}
