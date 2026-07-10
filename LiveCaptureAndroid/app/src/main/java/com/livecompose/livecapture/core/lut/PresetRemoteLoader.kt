package com.livecompose.livecapture.core.lut

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 大师预设远程加载器
 *
 * 从 OMaster Community CDN 加载预设数据：
 * - OPPO/OnePlus: https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json
 * - Realme GR: https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json
 *
 * 支持本地缓存 + 版本检测增量更新。
 */
class PresetRemoteLoader(private val cacheDir: File) {

    companion object {
        const val OPPO_PRESET_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json"
        const val REALME_PRESET_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json"
        private const val CACHE_FILE_OPPO = "master_presets_oppo.json"
        private const val CACHE_FILE_REALME = "master_presets_realme.json"
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 加载所有大师预设
     */
    suspend fun loadAllPresets(): List<MasterPresetCollection> = withContext(Dispatchers.IO) {
        val collections = mutableListOf<MasterPresetCollection>()

        // 加载 OPPO/OnePlus 预设
        try {
            val oppoJson = loadJson(OPPO_PRESET_URL, CACHE_FILE_OPPO)
            collections.add(parsePresetCollection(oppoJson))
        } catch (e: Exception) {
            // 远程加载失败，尝试本地缓存
            try {
                val cached = loadFromCache(CACHE_FILE_OPPO)
                collections.add(parsePresetCollection(cached))
            } catch (_: Exception) { /* 无缓存，跳过 */ }
        }

        // 加载 Realme 预设
        try {
            val realmeJson = loadJson(REALME_PRESET_URL, CACHE_FILE_REALME)
            collections.add(parsePresetCollection(realmeJson))
        } catch (e: Exception) {
            try {
                val cached = loadFromCache(CACHE_FILE_REALME)
                collections.add(parsePresetCollection(cached))
            } catch (_: Exception) { /* 无缓存，跳过 */ }
        }

        collections
    }

    /**
     * 加载 JSON（远程优先，缓存兜底）
     */
    private suspend fun loadJson(url: String, cacheFileName: String): String = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "LiveCapture-Android")

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val content = reader.readText()
            reader.close()
            connection.disconnect()

            // 缓存到本地
            if (content.isNotEmpty()) {
                saveToCache(cacheFileName, content)
            }

            content
        } catch (e: Exception) {
            // 远程失败，使用缓存
            loadFromCache(cacheFileName)
        }
    }

    private fun loadFromCache(fileName: String): String {
        val file = File(cacheDir, fileName)
        if (!file.exists()) throw Exception("No cached preset data")
        return file.readText()
    }

    private fun saveToCache(fileName: String, content: String) {
        val file = File(cacheDir, fileName)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    /**
     * 解析预设集合 JSON
     */
    private fun parsePresetCollection(jsonStr: String): MasterPresetCollection {
        val root = json.parseToJsonElement(jsonStr).jsonObject

        return MasterPresetCollection(
            version = root["version"]?.jsonPrimitive?.int ?: 1,
            name = root["name"]?.jsonPrimitive?.content ?: "",
            author = root["author"]?.jsonPrimitive?.content ?: "@OMaster",
            build = root["build"]?.jsonPrimitive?.int ?: 1,
            presets = root["presets"]?.jsonArray?.map { parsePreset(it.jsonObject) } ?: emptyList()
        )
    }

    private fun parsePreset(obj: JsonObject): MasterPreset {
        return MasterPreset(
            name = obj["name"]?.jsonPrimitive?.content ?: "",
            author = obj["author"]?.jsonPrimitive?.content ?: "",
            coverPath = obj["coverPath"]?.jsonPrimitive?.content ?: "",
            galleryImages = obj["galleryImages"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            isNew = obj["isNew"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            sections = obj["sections"]?.jsonArray?.map { parseSection(it.jsonObject) } ?: emptyList(),
            tags = obj["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            description = obj["description"]?.jsonObject?.let {
                PresetDescription(
                    title = it["title"]?.jsonPrimitive?.content ?: "",
                    content = it["content"]?.jsonPrimitive?.content ?: ""
                )
            }
        )
    }

    private fun parseSection(obj: JsonObject): PresetSection {
        return PresetSection(
            title = obj["title"]?.jsonPrimitive?.content ?: "",
            items = obj["items"]?.jsonArray?.map { parseParam(it.jsonObject) } ?: emptyList()
        )
    }

    private fun parseParam(obj: JsonObject): PresetParam {
        return PresetParam(
            label = obj["label"]?.jsonPrimitive?.content ?: "",
            value = obj["value"]?.jsonPrimitive?.content ?: "",
            span = obj["span"]?.jsonPrimitive?.int ?: 1
        )
    }

    /**
     * 将预设参数解析为 ParsedPresetParams
     */
    fun parseParams(preset: MasterPreset): ParsedPresetParams {
        val params = mutableMapOf<String, String>()

        for (section in preset.sections) {
            for (item in section.items) {
                params[item.label] = item.value
            }
        }

        return ParsedPresetParams(
            filter = params["@string/param_filter"] ?: params["滤镜"] ?: "",
            filterIntensity = parseFilterIntensity(params["@string/param_filter"] ?: params["滤镜"] ?: ""),
            softLight = params["@string/param_soft_light"] ?: params["柔光"] ?: "无",
            toneCurve = parseNumeric(params["@string/param_tone_curve"] ?: params["色调曲线"]),
            saturation = parseNumeric(params["@string/param_saturation"] ?: params["饱和度"]),
            warmCool = parseNumeric(params["@string/param_warm_cool"] ?: params["冷暖色温"]),
            cyanMagenta = parseNumeric(params["@string/param_cyan_magenta"] ?: params["青品色调"]),
            sharpness = parseNumeric(params["@string/param_sharpness"] ?: params["锐度"]),
            vignette = (params["@string/param_vignette"] ?: params["暗角"]) == "开",
            vignetteIntensity = 0.5f,
            hue = parseNumeric(params["@string/param_hue"] ?: params["色相"]),
            contrast = parseNumeric(params["@string/param_contrast"] ?: params["对比度"]),
            contrastHighlight = parseNumeric(params["@string/param_contrast_highlight"] ?: params["高光对比"]),
            contrastShadow = parseNumeric(params["@string/param_contrast_shadow"] ?: params["阴影对比"]),
            brightness = parseNumeric(params["@string/param_brightness"] ?: params["亮度"]),
            clarity = parseNumeric(params["@string/param_clarity"] ?: params["清晰度"]),
            grain = parseNumeric(params["@string/param_grain"] ?: params["颗粒"]),
            grainSize = parseNumeric(params["@string/param_grain_size"] ?: params["颗粒大小"]),
            dehaze = parseNumeric(params["@string/param_dehaze"] ?: params["去雾"]),
            iso = params["@string/param_iso"] ?: params["ISO"] ?: "",
            shutter = params["@string/param_shutter"] ?: params["快门"] ?: "",
            exposure = parseNumeric(params["@string/param_exposure"] ?: params["曝光补偿"]),
            colorTemp = (params["@string/param_color_temp"] ?: params["色温"])?.replace("K", "")?.toIntOrNull() ?: 0,
            tone = parseNumeric(params["@string/param_tone"] ?: params["色调"])
        )
    }

    private fun parseNumeric(value: String?): Float {
        if (value == null || value == "无" || value == "Auto" || value.isEmpty()) return 0f
        return value.replace("+", "").replace("%", "").toFloatOrNull() ?: 0f
    }

    private fun parseFilterIntensity(value: String): Float {
        // 解析 "明艳 100%" 格式
        val parts = value.split(" ")
        return if (parts.size > 1) {
            parts[1].replace("%", "").toFloatOrNull()?.div(100f) ?: 1.0f
        } else 1.0f
    }
}