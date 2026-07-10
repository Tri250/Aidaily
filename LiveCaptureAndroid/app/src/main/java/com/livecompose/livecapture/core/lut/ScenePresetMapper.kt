package com.livecompose.livecapture.core.lut

/**
 * 场景→预设智能映射表
 *
 * 根据场景识别结果推荐最佳大师预设。
 * 基于 OMaster 22+ OPPO 预设 + 2 Realme 预设的用途分析。
 *
 * 映射逻辑：
 * 1. 场景识别 → 场景关键词
 * 2. 关键词匹配 → 预设推荐列表
 * 3. 排序：匹配度 × 使用频率 × 评分
 */
object ScenePresetMapper {

    /**
     * 场景→预设映射表
     * key: 场景关键词
     * value: 推荐预设名称列表（按优先级排序）
     */
    private val scenePresetMap: Map<String, List<String>> = mapOf(
        // 户外/风光
        "outdoor" to listOf("理光蓝", "蓝调通透", "理光绿", "童话", "晴天复古"),
        "sky" to listOf("蓝调通透", "理光蓝", "晴天复古"),
        "landscape" to listOf("理光蓝", "理光绿", "哈苏浓郁", "德味预设"),
        "mountain" to listOf("哈苏浓郁", "德味预设", "人文"),
        "water" to listOf("蓝调时刻", "蓝调通透", "理光蓝"),
        "beach" to listOf("蓝调通透", "晴天复古", "童话"),
        "snow" to listOf("氛围雪夜", "高对比黑白", "梦幻黑白"),
        "sunset" to listOf("蓝调时刻", "富士胶片", "哈苏浓郁"),
        "sunrise" to listOf("假日清新", "童话", "晴天复古"),

        // 城市/建筑
        "city" to listOf("手机徕卡", "德味预设", "高对比黑白"),
        "architecture" to listOf("德味预设", "手机徕卡", "高对比黑白"),
        "street" to listOf("人文", "手机徕卡", "德味预设"),
        "night" to listOf("蓝调时刻", "氛围雪夜", "梦幻黑柔"),

        // 人像
        "portrait" to listOf("梦幻黑柔", "梦幻富士", "富士NC", "胶片感"),
        "face" to listOf("梦幻黑柔", "梦幻富士", "清新人文"),
        "selfie" to listOf("梦幻黑柔", "梦幻富士", "胶片感"),
        "group" to listOf("清新人文", "假日清新", "童话"),

        // 美食
        "food" to listOf("美味流芳", "美味梦境", "胶片感"),
        "restaurant" to listOf("美味流芳", "美味梦境"),
        "coffee" to listOf("人文", "胶片感", "手机徕卡"),

        // 植物/自然
        "plant" to listOf("理光绿", "童话", "哈苏浓郁"),
        "flower" to listOf("童话", "梦幻富士", "理光绿"),
        "forest" to listOf("理光绿", "哈苏浓郁", "德味预设"),
        "park" to listOf("童话", "理光绿", "假日清新"),

        // 室内/静物
        "indoor" to listOf("胶片感", "人文", "德味预设"),
        "still_life" to listOf("德味预设", "手机徕卡", "胶片感"),

        // 宠物/动物
        "animal" to listOf("童话", "梦幻富士", "胶片感"),
        "pet" to listOf("梦幻富士", "童话", "梦幻黑柔"),

        // 运动/动态
        "sport" to listOf("高对比黑白", "手机徕卡", "德味预设"),
        "action" to listOf("高对比黑白", "手机徕卡"),

        // 文档/文字
        "document" to listOf("高对比黑白", "人文"),
        "text" to listOf("高对比黑白"),

        // 默认
        "default" to listOf("德味预设", "哈苏浓郁", "手机徕卡", "富士胶片", "胶片感")
    )

    /**
     * 根据场景类型推荐预设
     *
     * @param sceneType 场景类型字符串
     * @param topN 返回前 N 个推荐
     * @return 推荐预设名称列表
     */
    fun recommend(sceneType: String, topN: Int = 3): List<String> {
        val normalized = sceneType.lowercase().trim()

        // 精确匹配
        val exact = scenePresetMap[normalized]
        if (exact != null) return exact.take(topN)

        // 模糊匹配
        for ((key, presets) in scenePresetMap) {
            if (normalized.contains(key) || key.contains(normalized)) {
                return presets.take(topN)
            }
        }

        // 默认推荐
        return scenePresetMap["default"]?.take(topN) ?: listOf("德味预设")
    }

    /**
     * 根据场景类型获取推荐预设的完整对象
     *
     * @param sceneType 场景类型
     * @param allPresets 所有可用预设
     * @param topN 返回前 N 个
     * @return 推荐预设列表
     */
    fun recommendPresets(
        sceneType: String,
        allPresets: List<MasterPreset>,
        topN: Int = 3
    ): List<MasterPreset> {
        val recommendedNames = recommend(sceneType, topN)
        return recommendedNames.mapNotNull { name ->
            allPresets.find { it.name == name }
        }.ifEmpty {
            allPresets.take(topN)
        }
    }

    /**
     * 获取所有场景类型
     */
    fun getAllSceneTypes(): List<String> = scenePresetMap.keys.toList().sorted()
}