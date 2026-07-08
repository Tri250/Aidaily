package com.livecompose.livecapture.core.filter

import android.graphics.Bitmap
import android.graphics.Color
import com.livecompose.livecapture.core.intelligence.LightAnalysis
import com.livecompose.livecapture.core.intelligence.LightType
import com.livecompose.livecapture.core.intelligence.SceneType
import com.livecompose.livecapture.core.lut.BuiltInPresets
import com.livecompose.livecapture.core.lut.LutPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI 滤镜推荐结果
 *
 * @param preset 推荐的滤镜预设
 * @param confidence 置信度（0-1）
 * @param reason 推荐理由
 */
data class FilterRecommendation(
    val preset: LutPreset,
    val confidence: Float,
    val reason: String
)

/**
 * 简化光线分析结果（用于滤镜推荐）
 *
 * 对应 iOS 端 AIFilterRecommender 内部的 LightAnalysis 结构。
 */
data class FilterLightAnalysis(
    val averageR: Float,
    val averageG: Float,
    val averageB: Float,
    val estimatedBrightness: Float,
    val estimatedTemperature: Float,
    val isWarmLight: Boolean,
    val isCoolLight: Boolean,
    val isLowLight: Boolean,
    val isHighLight: Boolean
)

/**
 * AI 滤镜推荐器
 *
 * 对应 iOS 端 AIFilterRecommender.swift，基于场景类型和光线分析智能推荐滤镜。
 *
 * ## 场景-滤镜映射规则
 * - 人像 (PORTRAIT/SELFIE) → 暖调人像、清新人像、Kodak Gold
 * - 美食 (FOOD) → 鲜艳风景、Kodak Gold、清新人像
 * - 风景 (LANDSCAPE/NATURE) → 鲜艳风景、电影质感、Portra 400
 * - 夜景 (NIGHT) → 电影质感、Ricoh GR、青橙色调
 * - 建筑 (ARCHITECTURE/URBAN) → Ilford HP5、青橙色调、电影质感
 * - 街拍 (STREET) → Kodak Gold、Ricoh GR、电影质感
 * - 日落 (SUNSET) → Kodak Gold、复古褐色、暖调人像
 * - 室内 (INDOOR) → 暖调人像、Kodak Gold、Portra 400
 * - 未知 (UNKNOWN) → Portra 400、Fuji C200、电影质感
 *
 * @param sceneType 目标场景
 * @param lightAnalysis 光线分析
 * @param topK 返回的推荐数量
 * @return 滤镜推荐列表（按置信度降序）
 */
class AiFilterRecommender {

    /**
     * 场景-滤镜预设 ID 映射表
     *
     * 映射到 Android [BuiltInPresets] 中的预设 ID。
     */
    private val sceneFilterMap: Map<SceneType, List<String>> = mapOf(
        SceneType.PORTRAIT to listOf("portrait_warm", "portrait_fresh", "kodak_gold"),
        SceneType.SELFIE to listOf("portrait_warm", "portrait_fresh", "kodak_gold"),
        SceneType.FOOD to listOf("landscape_vivid", "kodak_gold", "portrait_fresh"),
        SceneType.LANDSCAPE to listOf("landscape_vivid", "cinematic", "portra400"),
        SceneType.NATURE to listOf("landscape_vivid", "portra400", "velvia50"),
        SceneType.NIGHT to listOf("cinematic", "ricoh_gr", "teal_orange"),
        SceneType.ARCHITECTURE to listOf("ilford_hp5", "teal_orange", "cinematic"),
        SceneType.URBAN to listOf("ilford_hp5", "teal_orange", "cinematic"),
        SceneType.STREET to listOf("kodak_gold", "ricoh_gr", "cinematic"),
        SceneType.MACRO to listOf("portra400", "portrait_fresh", "fuji_c200"),
        SceneType.INDOOR to listOf("portrait_warm", "kodak_gold", "portra400"),
        SceneType.SUNSET to listOf("kodak_gold", "vintage_sepia", "portrait_warm"),
        SceneType.WEDDING to listOf("portrait_warm", "portra400", "portrait_fresh"),
        SceneType.PRODUCT to listOf("landscape_vivid", "portra400", "cinematic"),
        SceneType.PET to listOf("fuji_c200", "portrait_fresh", "kodak_gold"),
        SceneType.UNKNOWN to listOf("portra400", "fuji_c200", "cinematic")
    )

    /** 场景-滤镜推荐理由 */
    private val sceneReasonMap: Map<SceneType, Map<String, String>> = mapOf(
        SceneType.PORTRAIT to mapOf(
            "portrait_warm" to "温暖肤色，柔和高光，专为人像优化",
            "portrait_fresh" to "明亮通透，冷色调让肤色更显白皙",
            "kodak_gold" to "金色暖调为人像增添温暖感"
        ),
        SceneType.SELFIE to mapOf(
            "portrait_warm" to "自拍人像优化，柔和肤色表现",
            "portrait_fresh" to "清新自然，适合自拍场景",
            "kodak_gold" to "怀旧暖调，自拍更有氛围"
        ),
        SceneType.FOOD to mapOf(
            "landscape_vivid" to "增强食物色彩饱和度，让每一道菜都诱人",
            "kodak_gold" to "暖黄灯光营造温馨用餐氛围",
            "portrait_fresh" to "明亮通透让甜品更显精致清新"
        ),
        SceneType.LANDSCAPE to mapOf(
            "landscape_vivid" to "高饱和高对比，展现风光壮丽色彩",
            "cinematic" to "电影感色调，大气磅礴",
            "portra400" to "自然色彩还原风景本色"
        ),
        SceneType.NATURE to mapOf(
            "landscape_vivid" to "鲜艳色彩展现自然之美",
            "portra400" to "柔和色调还原自然本色",
            "velvia50" to "超高饱和度，风景神片"
        ),
        SceneType.NIGHT to mapOf(
            "cinematic" to "电影感色调完美适配夜景",
            "ricoh_gr" to "高对比都市感营造夜间氛围",
            "teal_orange" to "青橙色调增强夜景视觉冲击"
        ),
        SceneType.ARCHITECTURE to mapOf(
            "ilford_hp5" to "高对比黑白凸显建筑线条",
            "teal_orange" to "青橙色调增强建筑视觉冲击",
            "cinematic" to "电影感展现建筑几何美感"
        ),
        SceneType.URBAN to mapOf(
            "ilford_hp5" to "黑白影调展现都市质感",
            "teal_orange" to "青橙色调增强城市氛围",
            "cinematic" to "电影感都市色调"
        ),
        SceneType.STREET to mapOf(
            "kodak_gold" to "柯达金色调，街拍首选",
            "ricoh_gr" to "高对比都市感，街头故事感",
            "cinematic" to "电影感增添街头氛围"
        ),
        SceneType.MACRO to mapOf(
            "portra400" to "自然色彩准确还原微距细节",
            "portrait_fresh" to "明亮通透展现微距世界的细腻",
            "fuji_c200" to "清新自然增强微距表现"
        ),
        SceneType.INDOOR to mapOf(
            "portrait_warm" to "室内人像优化，柔化效果好",
            "kodak_gold" to "暖光色调提升室内氛围感",
            "portra400" to "柔和色调改善室内光线"
        ),
        SceneType.SUNSET to mapOf(
            "kodak_gold" to "柯达金色增强日落的金色光辉",
            "vintage_sepia" to "复古褐色渲染日落怀旧情绪",
            "portrait_warm" to "暖阳色调让日落更加温馨"
        ),
        SceneType.WEDDING to mapOf(
            "portrait_warm" to "温暖肤色，婚礼人像首选",
            "portra400" to "经典人像胶片，柔和肤色",
            "portrait_fresh" to "明亮通透，清新婚礼风格"
        ),
        SceneType.PRODUCT to mapOf(
            "landscape_vivid" to "高饱和展现产品色彩",
            "portra400" to "自然色彩还原产品本色",
            "cinematic" to "电影感提升产品质感"
        ),
        SceneType.PET to mapOf(
            "fuji_c200" to "清新自然，适合宠物拍摄",
            "portrait_fresh" to "明亮通透展现宠物可爱",
            "kodak_gold" to "暖调增添宠物温馨感"
        ),
        SceneType.UNKNOWN to mapOf(
            "portra400" to "自然色彩，适用于大多数场景",
            "fuji_c200" to "富士经典，通用性强",
            "cinematic" to "电影质感，提升画面表现力"
        )
    )

    /**
     * 基于场景类型和光线分析推荐滤镜
     *
     * @param sceneType 场景类型
     * @param lightAnalysis 光线分析
     * @param topK 返回的推荐数量，默认 3
     * @return 滤镜推荐列表（按置信度降序）
     */
    fun recommend(
        sceneType: SceneType,
        lightAnalysis: LightAnalysis,
        topK: Int = 3
    ): List<FilterRecommendation> {
        // 预设 ID → LutPreset 映射
        val presetMap = BuiltInPresets.presets.associateBy { it.id }
        val reasonMap = sceneReasonMap[sceneType] ?: emptyMap()

        // 获取该场景的滤镜列表，回退到 UNKNOWN
        var recommendedIds = sceneFilterMap[sceneType] ?: sceneFilterMap[SceneType.UNKNOWN]!!

        // 将 LightAnalysis 转换为简化分析，用于光线调整
        val filterLight = toFilterLightAnalysis(lightAnalysis)

        // 根据光线调整排序
        recommendedIds = adjustForLight(recommendedIds, filterLight)

        val recommendations = mutableListOf<FilterRecommendation>()
        val total = recommendedIds.size

        for ((index, id) in recommendedIds.withIndex()) {
            val preset = presetMap[id] ?: continue
            val baseConfidence = 1.0f - (index.toFloat() / (total + 1).toFloat())
            val adjustedConfidence = adjustConfidence(baseConfidence, preset, filterLight)
            val reason = reasonMap[id] ?: generateReason(preset, sceneType)

            recommendations.add(
                FilterRecommendation(
                    preset = preset,
                    confidence = adjustedConfidence.coerceIn(0f, 1f),
                    reason = reason
                )
            )
        }

        return recommendations.sortedByDescending { it.confidence }.take(topK)
    }

    /**
     * 基于图像分析推荐滤镜
     *
     * 采样图像像素计算平均 RGB 和亮度，推断场景后调用 [recommend]。
     *
     * @param bitmap 输入图像
     * @return 滤镜推荐列表
     */
    suspend fun recommendForImage(bitmap: Bitmap): List<FilterRecommendation> =
        withContext(Dispatchers.Default) {
            val lightAnalysis = analyzeImage(bitmap)
            val scene = inferScene(lightAnalysis)
            // 构造 LightAnalysis 对象（用于 recommend）
            val fullLight = LightAnalysis(
                colorTemperature = lightAnalysis.estimatedTemperature,
                brightness = lightAnalysis.estimatedBrightness,
                contrast = 0.5f,
                isBacklit = false,
                lightType = if (lightAnalysis.isWarmLight) LightType.WARM
                    else if (lightAnalysis.isCoolLight) LightType.COOL
                    else LightType.NATURAL
            )
            recommend(scene, fullLight, topK = 3)
        }

    // MARK: - 光线调整

    /**
     * 根据光线调整滤镜排序
     *
     * - 暖光：降低暖色滤镜优先级
     * - 冷光：降低冷色滤镜优先级
     * - 低光：提升夜景滤镜优先级
     * - 高光：提升明亮滤镜优先级
     */
    private fun adjustForLight(names: List<String>, light: FilterLightAnalysis): List<String> {
        val result = names.toMutableList()

        if (light.isWarmLight) {
            deprioritize(result, listOf("kodak_gold", "vintage_sepia", "portrait_warm"))
        }
        if (light.isCoolLight) {
            deprioritize(result, listOf("portrait_fresh", "cinematic", "fuji_c200"))
        }
        if (light.isLowLight) {
            prioritize(result, listOf("cinematic", "ricoh_gr", "teal_orange"))
        }
        if (light.isHighLight) {
            prioritize(result, listOf("fuji_c200", "kodak_gold", "portra400"))
        }

        return result
    }

    /**
     * 降低指定滤镜的优先级（移到列表末尾）
     */
    private fun deprioritize(list: MutableList<String>, filters: List<String>) {
        for (filterName in filters) {
            val index = list.indexOf(filterName)
            if (index >= 0) {
                list.removeAt(index)
                list.add(filterName)
            }
        }
    }

    /**
     * 提升指定滤镜的优先级（移到列表开头）
     */
    private fun prioritize(list: MutableList<String>, filters: List<String>) {
        for (filterName in filters.reversed()) {
            val index = list.indexOf(filterName)
            if (index >= 0) {
                list.removeAt(index)
                list.add(0, filterName)
            }
        }
    }

    // MARK: - 置信度调整

    /**
     * 根据光线和预设参数调整置信度
     */
    private fun adjustConfidence(
        base: Float,
        preset: LutPreset,
        light: FilterLightAnalysis
    ): Float {
        var confidence = base

        // 暖光 + 暖色滤镜 → 降低
        if (light.isWarmLight && preset.warmth > 10f) {
            confidence -= 0.1f
        }
        // 冷光 + 冷色滤镜 → 降低
        if (light.isCoolLight && preset.warmth < -10f) {
            confidence -= 0.1f
        }
        // 低光 + 高对比 → 略提升
        if (light.isLowLight && preset.contrast > 1.15f) {
            confidence += 0.05f
        }
        // 低光 + 负曝光 → 降低
        if (light.isLowLight && preset.exposure < -0.1f) {
            confidence -= 0.08f
        }
        // 高光 + 低对比 → 略提升
        if (light.isHighLight && preset.contrast < 0.95f) {
            confidence += 0.05f
        }

        return confidence.coerceIn(0f, 1f)
    }

    // MARK: - 图像分析

    /**
     * 分析图像光线（替代 iOS CIAreaAverage）
     *
     * 降采样到 64x64 后采样像素计算平均 RGB、亮度和色温。
     *
     * @param bitmap 输入位图
     * @return 简化光线分析结果
     */
    private fun analyzeImage(bitmap: Bitmap): FilterLightAnalysis {
        val targetSize = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)

        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0

        for (y in 0 until targetSize) {
            for (x in 0 until targetSize) {
                val pixel = scaled.getPixel(x, y)
                sumR += Color.red(pixel)
                sumG += Color.green(pixel)
                sumB += Color.blue(pixel)
                count++
            }
        }
        if (scaled !== bitmap) scaled.recycle()

        val avgR = (sumR / count / 255.0).toFloat()
        val avgG = (sumG / count / 255.0).toFloat()
        val avgB = (sumB / count / 255.0).toFloat()
        val brightness = 0.299f * avgR + 0.587f * avgG + 0.114f * avgB

        // 估算色温（基于 R/B 比值）
        val rbRatio = avgR / maxOf(avgB, 0.001f)
        val temperature = when {
            rbRatio > 1.2f -> 6500f - (rbRatio - 1.0f) * 3000f
            rbRatio < 0.8f -> 6500f + (1.0f - rbRatio) * 3000f
            else -> 6500f
        }.coerceIn(2000f, 10000f)

        return FilterLightAnalysis(
            averageR = avgR,
            averageG = avgG,
            averageB = avgB,
            estimatedBrightness = brightness,
            estimatedTemperature = temperature,
            isWarmLight = temperature < 5000f,
            isCoolLight = temperature > 7500f,
            isLowLight = brightness < 0.3f,
            isHighLight = brightness > 0.8f
        )
    }

    /**
     * 从光线推断场景
     */
    private fun inferScene(light: FilterLightAnalysis): SceneType {
        if (light.isLowLight) return SceneType.NIGHT
        if (light.isWarmLight && light.averageR > light.averageB * 1.3f) return SceneType.SUNSET
        return SceneType.UNKNOWN
    }

    /**
     * 将 LightAnalysis 转换为简化分析
     */
    private fun toFilterLightAnalysis(light: LightAnalysis): FilterLightAnalysis {
        val temp = light.colorTemperature
        val brightness = light.brightness
        return FilterLightAnalysis(
            averageR = 0.5f,
            averageG = 0.5f,
            averageB = 0.5f,
            estimatedBrightness = brightness,
            estimatedTemperature = temp,
            isWarmLight = temp < 5000f,
            isCoolLight = temp > 7500f,
            isLowLight = brightness < 0.3f,
            isHighLight = brightness > 0.8f
        )
    }

    /**
     * 生成默认推荐理由
     */
    private fun generateReason(preset: LutPreset, scene: SceneType): String {
        if (preset.saturation == 0f) return "黑白影调增强画面表现力"
        if (preset.contrast > 1.15f) return "高对比度增强画面层次感"
        if (preset.contrast < 0.95f) return "柔和对比营造舒适观感"
        if (preset.warmth > 10f) return "暖色调增添画面温馨感"
        if (preset.warmth < -10f) return "冷色调营造清新氛围"
        return "适用于${scene.displayName()}场景"
    }
}

/**
 * SceneType 显示名称扩展
 */
private fun SceneType.displayName(): String = when (this) {
    SceneType.PORTRAIT -> "人像"
    SceneType.PORTRAIT_STANDING -> "站姿人像"
    SceneType.PORTRAIT_SITTING -> "坐姿人像"
    SceneType.COUPLE -> "双人"
    SceneType.CHILDREN -> "儿童"
    SceneType.FOOD -> "美食"
    SceneType.LANDSCAPE -> "风景"
    SceneType.WEDDING -> "婚礼"
    SceneType.PRODUCT -> "产品"
    SceneType.BACKLIT -> "逆光"
    SceneType.UNKNOWN -> "通用"
    SceneType.GROUP -> "合照"
    SceneType.SELFIE -> "自拍"
    SceneType.NIGHT -> "夜景"
    SceneType.SUNSET -> "日落"
    SceneType.URBAN -> "城市"
    SceneType.NATURE -> "自然"
    SceneType.INDOOR -> "室内"
    SceneType.OUTDOOR -> "户外"
    SceneType.SILHOUETTE -> "剪影"
    SceneType.ACTION -> "运动"
    SceneType.STILL_LIFE -> "静物"
    SceneType.MACRO -> "微距"
    SceneType.TRAVEL -> "旅行"
    SceneType.STREET -> "街拍"
    SceneType.ARCHITECTURE -> "建筑"
    SceneType.PET -> "宠物"
    SceneType.SPORTS -> "体育"
    SceneType.EVENT -> "活动"
    SceneType.FASHION -> "时尚"
    SceneType.BEAUTY -> "美妆"
    SceneType.DOCUMENTARY -> "纪实"
    SceneType.MINIMAL -> "极简"
    SceneType.VINTAGE -> "复古"
    SceneType.CINEMATIC -> "电影"
}
