package com.livecompose.livecapture.core.storage

import androidx.exifinterface.media.ExifInterface
import com.livecompose.livecapture.core.intelligence.SceneType
import com.livecompose.livecapture.core.logger.AppLogger
import java.util.Calendar
import java.util.Locale

/**
 * 日期范围数据类
 *
 * 用于表示搜索查询中解析出的日期范围。[contains] 判断给定时间戳是否落在范围内。
 *
 * @param start 起始时间戳（毫秒），null 表示无下界
 * @param end 结束时间戳（毫秒），null 表示无上界
 */
data class DateRange(
    val start: Long?,
    val end: Long?
) {
    /**
     * 判断给定时间戳是否在范围内
     *
     * @param timestamp 时间戳（毫秒）
     * @return true 表示在范围内
     */
    fun contains(timestamp: Long): Boolean {
        if (start != null && timestamp < start) return false
        if (end != null && timestamp > end) return false
        return true
    }
}

/**
 * 自然语言照片搜索引擎
 *
 * 对应 iOS 端 PhotoSearchEngine.swift，使用正则表达式（替代 iOS NaturalLanguage 框架）
 * 进行中文日期解析、场景关键词提取和地点关键词匹配，对照片记录进行加权评分搜索。
 *
 * ## 主要功能
 * - [search] 自然语言搜索照片（支持中文日期、场景、地点、EXIF 参数等）
 * - [extractDate] 从自然语言中提取日期范围
 * - [extractSceneKeywords] 从查询中提取场景关键词
 * - [extractLocationKeywords] 从查询中提取地点关键词
 *
 * @param photoStorage 照片存储服务，用于读取照片文件以匹配 GPS 数据
 */
class PhotoSearchEngine(
    private val photoStorage: PhotoStorageService
) {

    companion object {
        private const val TAG = "PhotoSearchEngine"
    }

    /** 场景关键词 → SceneType 映射 */
    private val sceneKeywords: Map<String, SceneType> = mapOf(
        // 美食
        "美食" to SceneType.FOOD, "食物" to SceneType.FOOD, "吃饭" to SceneType.FOOD,
        "餐厅" to SceneType.FOOD, "料理" to SceneType.FOOD, "火锅" to SceneType.FOOD,
        "烧烤" to SceneType.FOOD, "甜品" to SceneType.FOOD, "咖啡" to SceneType.FOOD,
        "奶茶" to SceneType.FOOD,
        // 风景
        "风景" to SceneType.LANDSCAPE, "山水" to SceneType.LANDSCAPE, "自然" to SceneType.NATURE,
        "森林" to SceneType.NATURE, "草原" to SceneType.LANDSCAPE, "湖泊" to SceneType.LANDSCAPE,
        "河流" to SceneType.LANDSCAPE, "山" to SceneType.LANDSCAPE, "天空" to SceneType.LANDSCAPE,
        "云" to SceneType.LANDSCAPE,
        // 人像
        "人像" to SceneType.PORTRAIT, "人物" to SceneType.PORTRAIT, "自拍" to SceneType.SELFIE,
        "合照" to SceneType.GROUP, "合影" to SceneType.GROUP, "朋友" to SceneType.PORTRAIT,
        "家人" to SceneType.PORTRAIT,
        // 夜景
        "夜景" to SceneType.NIGHT, "晚上" to SceneType.NIGHT, "夜晚" to SceneType.NIGHT,
        "灯光" to SceneType.NIGHT, "霓虹" to SceneType.NIGHT, "星空" to SceneType.NIGHT,
        // 建筑
        "建筑" to SceneType.ARCHITECTURE, "城市" to SceneType.URBAN, "高楼" to SceneType.ARCHITECTURE,
        "大厦" to SceneType.ARCHITECTURE, "街道" to SceneType.STREET,
        // 街拍
        "街拍" to SceneType.STREET, "街头" to SceneType.STREET, "马路" to SceneType.STREET,
        // 宠物
        "宠物" to SceneType.PET, "猫" to SceneType.PET, "狗" to SceneType.PET,
        "动物" to SceneType.PET, "猫咪" to SceneType.PET, "狗狗" to SceneType.PET,
        // 自然（花卉/海滩/雪景/日出日落合并到 NATURE/SUNSET）
        "花" to SceneType.NATURE, "花卉" to SceneType.NATURE, "花朵" to SceneType.NATURE,
        "樱花" to SceneType.NATURE, "梅花" to SceneType.NATURE, "荷花" to SceneType.NATURE,
        "玫瑰" to SceneType.NATURE,
        "海滩" to SceneType.NATURE, "海边" to SceneType.NATURE, "大海" to SceneType.NATURE,
        "沙滩" to SceneType.NATURE, "海" to SceneType.NATURE, "湖" to SceneType.NATURE,
        "雪景" to SceneType.NATURE, "雪" to SceneType.NATURE, "下雪" to SceneType.NATURE,
        "冬天" to SceneType.NATURE,
        "日出" to SceneType.SUNSET, "日落" to SceneType.SUNSET, "夕阳" to SceneType.SUNSET,
        "黄昏" to SceneType.SUNSET, "晚霞" to SceneType.SUNSET, "朝霞" to SceneType.SUNSET,
        "黎明" to SceneType.SUNSET,
        // 室内
        "室内" to SceneType.INDOOR, "家里" to SceneType.INDOOR, "房间" to SceneType.INDOOR,
        // 文档（映射到 DOCUMENTARY）
        "文档" to SceneType.DOCUMENTARY, "文字" to SceneType.DOCUMENTARY,
        "书本" to SceneType.DOCUMENTARY, "书籍" to SceneType.DOCUMENTARY,
        // 舞台（映射到 EVENT）
        "舞台" to SceneType.EVENT, "演出" to SceneType.EVENT, "演唱会" to SceneType.EVENT,
        "表演" to SceneType.EVENT,
        // 婚礼
        "婚礼" to SceneType.WEDDING, "婚纱" to SceneType.WEDDING,
        // 产品
        "产品" to SceneType.PRODUCT, "商品" to SceneType.PRODUCT,
        // 微距
        "微距" to SceneType.MACRO, "特写" to SceneType.MACRO
    )

    /** 地点关键词集合 */
    private val locationKeywords: Set<String> = setOf(
        "北京", "上海", "广州", "深圳", "杭州", "成都", "南京", "武汉", "西安", "重庆",
        "苏州", "厦门", "青岛", "大连", "三亚", "香港", "澳门", "台北", "哈尔滨", "昆明",
        "拉萨", "乌鲁木齐", "海口", "桂林", "丽江", "张家界", "长沙", "郑州", "合肥",
        "济南", "天津", "福州", "南宁", "贵阳", "兰州", "银川", "西宁", "呼和浩特",
        "西湖", "故宫", "长城", "外滩", "东方明珠", "迪士尼", "环球影城", "故宫博物院",
        "颐和园", "天坛", "鸟巢", "水立方", "深圳湾", "广州塔", "中山陵", "夫子庙",
        "武大", "武大樱花", "大雁塔", "兵马俑", "解放碑", "洪崖洞", "鼓浪屿", "栈桥",
        "星海", "亚龙湾", "太平山", "大三巴", "日月潭", "阿里山", "冰雪大世界",
        "滇池", "石林", "布达拉宫", "大昭寺", "天山", "吐鲁番", "漓江", "阳朔",
        "黄山", "泰山", "华山", "峨眉山", "庐山", "武夷山", "九寨沟"
    )

    // MARK: - 搜索

    /**
     * 自然语言搜索照片
     *
     * 解析查询字符串中的日期、场景、地点和通用关键词，对每张照片进行加权评分：
     * - 日期匹配：+3.0（权重最高）
     * - 地点匹配：+2.0
     * - 场景匹配：+1.5
     * - EXIF 参数匹配（高 ISO/长曝光/高速/大光圈）：+1.0
     * - detectionMethod 关键词匹配：+0.5
     *
     * 仅返回评分 > 0 的照片，按评分降序排列。
     *
     * @param query 自然语言查询字符串
     * @param records 照片记录列表
     * @return 匹配的照片记录列表（按相关性排序）
     */
    fun search(query: String, records: List<PhotoRecord>): List<PhotoRecord> {
        if (query.trim().isEmpty()) return records

        // 1. 提取日期范围
        val dateRange = extractDate(query)

        // 2. 提取场景关键词
        val scenes = extractSceneKeywords(query)

        // 3. 提取地点关键词
        val locations = extractLocationKeywords(query)

        // 4. 提取通用关键词（去除已识别的场景和地点关键词）
        val generalKeywords = tokenize(query).filter { token ->
            scenes.none { it == token } && locations.none { it == token }
        }

        // 评分
        val scoredRecords = mutableListOf<Pair<PhotoRecord, Float>>()

        for (record in records) {
            var score = 0f

            // 日期匹配
            if (dateRange != null && dateRange.contains(record.creationDate)) {
                score += 3.0f
            }

            // 场景关键词匹配
            for (keyword in scenes) {
                val sceneType = sceneKeywords[keyword]
                if (sceneType != null && matchSceneType(record, sceneType)) {
                    score += 1.5f
                }
            }

            // 地点关键词匹配
            for (location in locations) {
                if (matchLocation(record, location)) {
                    score += 2.0f
                }
            }

            // detectionMethod 通用关键词匹配
            val method = record.detectionMethod
            if (method != null) {
                for (keyword in generalKeywords) {
                    if (method.lowercase(Locale.US).contains(keyword.lowercase(Locale.US))) {
                        score += 0.5f
                    }
                }
            }

            // EXIF 参数匹配
            record.iso?.let { iso ->
                if ((generalKeywords.contains("高iso") || generalKeywords.contains("暗光")) && iso >= 1600) {
                    score += 1.0f
                }
            }
            record.shutterSpeed?.let { shutter ->
                if ((generalKeywords.contains("长曝光") || generalKeywords.contains("慢门")) && shutter > 0.5) {
                    score += 1.0f
                }
                if ((generalKeywords.contains("高速") || generalKeywords.contains("抓拍")) && shutter < 1.0 / 1000.0) {
                    score += 1.0f
                }
            }
            record.aperture?.let { aperture ->
                if ((generalKeywords.contains("大光圈") || generalKeywords.contains("虚化")) && aperture < 2.8) {
                    score += 1.0f
                }
            }

            if (score > 0) {
                scoredRecords.add(record to score)
            }
        }

        // 按分数降序排列
        scoredRecords.sortByDescending { it.second }

        return scoredRecords.map { it.first }
    }

    // MARK: - 日期提取

    /**
     * 从自然语言中提取日期范围
     *
     * 支持的关键词：今天、昨天、前天、本周/这周、上周、本月/这个月、上个月/上月、
     * 去年、今年，以及正则匹配 "yyyy年"、"yyyy年M月" 格式。
     *
     * @param query 查询字符串
     * @return 日期范围，无法解析返回 null
     */
    private fun extractDate(query: String): DateRange? {
        val calendar = Calendar.getInstance(Locale.CHINA)
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now
        // 归零到当天 00:00:00
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 今天
        if (query.contains("今天")) {
            val start = calendar.timeInMillis
            val end = start + MILLIS_PER_DAY - 1
            return DateRange(start, end)
        }

        // 昨天
        if (query.contains("昨天")) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val start = calendar.timeInMillis
            val end = start + MILLIS_PER_DAY - 1
            return DateRange(start, end)
        }

        // 前天
        if (query.contains("前天")) {
            calendar.add(Calendar.DAY_OF_YEAR, -2)
            val start = calendar.timeInMillis
            val end = start + MILLIS_PER_DAY - 1
            return DateRange(start, end)
        }

        // 本周/这周（按周一为起点）
        if (query.contains("本周") || query.contains("这周")) {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            val end = start + MILLIS_PER_DAY * 7 - 1
            return DateRange(start, end)
        }

        // 上周
        if (query.contains("上周")) {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val start = calendar.timeInMillis
            val end = start + MILLIS_PER_DAY * 7 - 1
            return DateRange(start, end)
        }

        // 本月/这个月
        if (query.contains("本月") || query.contains("这个月")) {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val end = calendar.timeInMillis - 1
            return DateRange(start, end)
        }

        // 上个月/上月
        if (query.contains("上个月") || query.contains("上月")) {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.MONTH, -1)
            val start = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val end = calendar.timeInMillis - 1
            return DateRange(start, end)
        }

        // 去年
        if (query.contains("去年")) {
            val thisYear = Calendar.getInstance(Locale.CHINA).apply { timeInMillis = now }
                .get(Calendar.YEAR)
            calendar.clear()
            calendar.set(Calendar.YEAR, thisYear - 1)
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val start = calendar.timeInMillis
            calendar.add(Calendar.YEAR, 1)
            val end = calendar.timeInMillis - 1
            return DateRange(start, end)
        }

        // 今年
        if (query.contains("今年")) {
            val thisYear = Calendar.getInstance(Locale.CHINA).apply { timeInMillis = now }
                .get(Calendar.YEAR)
            calendar.clear()
            calendar.set(Calendar.YEAR, thisYear)
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val start = calendar.timeInMillis
            calendar.add(Calendar.YEAR, 1)
            val end = calendar.timeInMillis - 1
            return DateRange(start, end)
        }

        // 匹配 "yyyy年M月" 格式（先匹配更具体的形式）
        val yearMonthRegex = Regex("(\\d{4})年(\\d{1,2})月")
        yearMonthRegex.find(query)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return@let
            val month = match.groupValues[2].toIntOrNull() ?: return@let
            if (year in 2000..2100 && month in 1..12) {
                calendar.clear()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month - 1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis - 1
                return DateRange(start, end)
            }
        }

        // 匹配 "yyyy年" 格式
        val yearRegex = Regex("(\\d{4})年")
        yearRegex.find(query)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return@let
            if (year in 2000..2100) {
                calendar.clear()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                val end = calendar.timeInMillis - 1
                return DateRange(start, end)
            }
        }

        return null
    }

    // MARK: - 场景关键词提取

    /**
     * 从查询中提取场景关键词
     *
     * @param query 查询字符串
     * @return 匹配到的场景关键词数组
     */
    private fun extractSceneKeywords(query: String): List<String> {
        val found = mutableListOf<String>()
        for (keyword in sceneKeywords.keys) {
            if (query.contains(keyword)) {
                found.add(keyword)
            }
        }
        return found
    }

    // MARK: - 地点关键词提取

    /**
     * 从查询中提取地点关键词
     *
     * @param query 查询字符串
     * @return 匹配到的地点关键词数组
     */
    private fun extractLocationKeywords(query: String): List<String> {
        val found = mutableListOf<String>()
        for (location in locationKeywords) {
            if (query.contains(location)) {
                found.add(location)
            }
        }
        return found
    }

    // MARK: - 分词

    /**
     * 简化分词（替代 iOS NLTokenizer）
     *
     * 由于 Android 无内置中文分词，采用基于关键词库的反向最大匹配：
     * 优先匹配场景/地点关键词库中的词，剩余部分按非字母数字分割为单字 token。
     *
     * @param text 输入文本
     * @return token 列表
     */
    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        // 提取所有关键词命中
        val allKeywords = sceneKeywords.keys + locationKeywords
        for (keyword in allKeywords) {
            if (text.contains(keyword)) {
                tokens.add(keyword)
            }
        }
        // 提取英文/数字词组（用于 EXIF 参数匹配，如 "高iso"、"f2.8" 等）
        val englishRegex = Regex("[a-zA-Z0-9.]+")
        englishRegex.findAll(text).forEach { match ->
            tokens.add(match.value.lowercase(Locale.US))
        }
        return tokens
    }

    // MARK: - 匹配逻辑

    /**
     * 匹配场景类型（基于 EXIF 数据）
     *
     * @param record 照片记录
     * @param sceneType 目标场景类型
     * @return true 表示匹配
     */
    private fun matchSceneType(record: PhotoRecord, sceneType: SceneType): Boolean {
        return when (sceneType) {
            SceneType.NIGHT -> {
                val iso = record.iso
                val shutter = record.shutterSpeed
                (iso != null && iso >= 1600) || (shutter != null && shutter > 0.5)
            }
            SceneType.LANDSCAPE, SceneType.NATURE -> {
                val iso = record.iso
                val shutter = record.shutterSpeed
                (iso != null && iso < 400) || (shutter != null && shutter < 1.0 / 2000.0)
            }
            SceneType.PORTRAIT, SceneType.SELFIE -> {
                val aperture = record.aperture
                aperture != null && aperture < 3.0
            }
            else -> true // 对于无法精确匹配的场景，默认返回匹配
        }
    }

    /**
     * 地点关键词 → 经纬度范围（用于基于 EXIF GPS 的地点匹配，替代 iOS Geocoder）
     *
     * 每个条目为 Triple(名称, 纬度范围, 经度范围)，与 [SmartAlbumClassifier.coordinateToRegionLabel] 对齐。
     */
    private val locationCoordinates: List<Triple<String, ClosedFloatingPointRange<Double>, ClosedFloatingPointRange<Double>>> = listOf(
        Triple("北京", 39.4..41.0, 115.4..117.5),
        Triple("上海", 30.7..31.5, 120.8..122.0),
        Triple("广州", 22.5..23.5, 112.9..114.0),
        Triple("深圳", 22.4..22.8, 113.7..114.6),
        Triple("杭州", 29.8..30.5, 119.7..120.8),
        Triple("成都", 30.0..31.0, 103.5..104.5),
        Triple("南京", 31.5..32.5, 118.3..119.2),
        Triple("武汉", 30.0..31.0, 113.7..115.0),
        Triple("西安", 33.8..34.6, 108.5..109.5),
        Triple("重庆", 29.0..30.0, 106.0..107.0),
        Triple("苏州", 30.8..31.5, 120.3..121.0),
        Triple("厦门", 24.2..24.6, 117.8..118.3),
        Triple("青岛", 35.8..36.5, 120.0..120.8),
        Triple("大连", 38.5..39.2, 121.0..122.0),
        Triple("三亚", 18.0..18.5, 109.0..109.8),
        Triple("香港", 22.1..22.5, 114.0..114.4),
        Triple("澳门", 22.0..22.2, 113.4..113.6),
        Triple("台北", 24.9..25.2, 121.4..121.6),
        Triple("哈尔滨", 45.3..46.0, 126.0..127.0),
        Triple("昆明", 24.5..25.5, 102.0..103.0),
        Triple("拉萨", 29.3..30.0, 90.5..91.5),
        Triple("乌鲁木齐", 43.3..44.2, 87.0..88.0),
        Triple("海口", 19.5..20.2, 110.0..110.5),
        Triple("桂林", 24.5..25.5, 110.0..110.8),
        Triple("丽江", 26.5..27.5, 100.0..100.5),
        Triple("张家界", 28.5..29.5, 110.0..111.0)
    )

    /**
     * 匹配地点（基于照片 EXIF GPS 数据）
     *
     * 将 [location] 关键词映射到城市经纬度范围，判断照片 GPS 是否落在该城市范围内。
     * 若关键词不匹配任何已知城市，回退到"中国范围"粗略匹配（避免漏召回）。
     *
     * @param record 照片记录
     * @param location 地点关键词
     * @return true 表示匹配
     */
    private fun matchLocation(record: PhotoRecord, location: String): Boolean {
        val file = photoStorage.getPhotoFile(record.id)
        if (!file.exists()) return false
        return try {
            val exif = ExifInterface(file.absolutePath)
            val latLong = exif.latLong ?: return false
            val lat = latLong[0].toDouble()
            val lon = latLong[1].toDouble()

            // 查找地点关键词对应的城市坐标范围
            val cityRange = locationCoordinates.firstOrNull { (name, _, _) ->
                location.contains(name) || name.contains(location)
            }

            if (cityRange != null) {
                val (_, latRange, lonRange) = cityRange
                lat in latRange && lon in lonRange
            } else {
                // 未知城市关键词，回退到中国范围粗略匹配
                lat in 18.0..54.0 && lon in 73.0..135.0
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "读取 EXIF GPS 失败: ${record.id}", e)
            false
        }
    }

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
