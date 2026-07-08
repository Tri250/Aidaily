package com.livecompose.livecapture.core.community

import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 拍照地点推荐器（与 iOS LocationRecommender.swift 功能对齐）
 *
 * 纯算法实现，不依赖网络 API：
 * - 内置 35+ 个中国热门拍照地点数据库（北京/上海/杭州/重庆/成都/西安等城市）
 * - 基于当前 GPS 坐标使用 Haversine 公式计算球面距离
 * - 在指定半径内推荐附近热门拍照点
 * - 支持按标签筛选
 *
 * 与 iOS 差异：iOS 内部管理 CLLocationManager 定位权限；
 * Android 端为保持纯算法与可测试性，由调用方通过 [setCurrentLocation] 注入定位坐标，
 * UI 层（CommunityScreen / ViewModel）负责权限请求与定位获取。
 */
class LocationRecommender {

    companion object {
        private const val TAG = "LocationRecommender"

        /** 地球半径（米），Haversine 公式用 */
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        /** 默认搜索半径（米），与 iOS 端 5000m 一致 */
        const val DEFAULT_RADIUS_METERS: Double = 5000.0
    }

    /** 全部内置拍照地点（只读） */
    val allLocations: List<PhotoLocation> = loadLocationDatabase()

    /** 附近地点（按距离升序），无定位时为全部地点按名称排序 */
    private val _nearbyLocations = MutableStateFlow<List<PhotoLocation>>(emptyList())
    val nearbyLocations: StateFlow<List<PhotoLocation>> = _nearbyLocations.asStateFlow()

    /** 当前定位（由调用方注入） */
    private val _currentLocation = MutableStateFlow<GeoCoordinate?>(null)
    val currentLocation: StateFlow<GeoCoordinate?> = _currentLocation.asStateFlow()

    /** 是否正在搜索（与 iOS isSearching 对齐） */
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        // 初始无定位：展示全部地点按名称排序
        _nearbyLocations.value = allLocations.sortedBy { it.name }
    }

    // MARK: - 定位注入

    /**
     * 设置当前定位坐标（由 UI 层获取 GPS 后注入）
     * 设置后会自动触发 [findNearbySpots] 重新计算附近地点。
     */
    fun setCurrentLocation(coordinate: GeoCoordinate?) {
        _currentLocation.value = coordinate
        if (coordinate != null) {
            findNearbySpots()
        } else {
            _nearbyLocations.value = allLocations.sortedBy { it.name }
        }
        AppLogger.d(TAG, "当前定位已更新: ${coordinate ?: "无"}")
    }

    // MARK: - 查找附近拍摄点

    /**
     * 查找指定半径内的拍摄点（与 iOS findNearbySpots(within:) 对齐）
     *
     * @param radius 搜索半径（米），默认 5000m
     */
    suspend fun findNearbySpotsAsync(radius: Double = DEFAULT_RADIUS_METERS) {
        withContext(Dispatchers.Default) {
            findNearbySpots(radius)
        }
    }

    /**
     * 同步查找附近拍摄点（与 iOS findNearbySpots 行为一致）
     */
    fun findNearbySpots(radius: Double = DEFAULT_RADIUS_METERS) {
        val userLocation = _currentLocation.value
        if (userLocation == null) {
            // 无定位时展示全部地点按名称排序（与 iOS 行为一致）
            _nearbyLocations.value = allLocations.sortedBy { it.name }
            return
        }

        _isSearching.value = true
        try {
            val nearby = allLocations
                .map { location -> location to distanceBetween(userLocation, location.coordinate) }
                .filter { (_, distance) -> distance <= radius }
                .sortedBy { (_, distance) -> distance }
                .map { (location, _) -> location }

            _nearbyLocations.value = nearby
            AppLogger.d(TAG, "找到 ${nearby.size} 个附近拍摄点（半径 ${radius}m）")
        } catch (e: Exception) {
            AppLogger.e(TAG, "查找附近拍摄点失败", e)
            _nearbyLocations.value = allLocations.sortedBy { it.name }
        } finally {
            _isSearching.value = false
        }
    }

    // MARK: - 按标签筛选

    /**
     * 按标签筛选地点（与 iOS getLocationsByTag 对齐）
     */
    fun getLocationsByTag(tag: String): List<PhotoLocation> {
        return allLocations.filter { it.tags.contains(tag) }
    }

    /**
     * 获取全部标签（去重排序，与 iOS getAllTags 对齐）
     */
    fun getAllTags(): List<String> {
        return allLocations
            .flatMap { it.tags }
            .toSet()
            .sorted()
    }

    // MARK: - 距离计算

    /**
     * 计算当前定位到指定地点的距离（米），无定位时返回 null
     */
    fun distanceToCurrent(location: PhotoLocation): Double? {
        val userLocation = _currentLocation.value ?: return null
        return distanceBetween(userLocation, location.coordinate)
    }

    /**
     * Haversine 公式计算两个坐标间的球面距离（米）
     */
    private fun distanceBetween(a: GeoCoordinate, b: GeoCoordinate): Double {
        val lat1Rad = Math.toRadians(a.latitude)
        val lat2Rad = Math.toRadians(b.latitude)
        val dLatRad = Math.toRadians(b.latitude - a.latitude)
        val dLonRad = Math.toRadians(b.longitude - a.longitude)

        val sinDLat = sin(dLatRad / 2)
        val sinDLon = sin(dLonRad / 2)
        val h = sinDLat * sinDLat + cos(lat1Rad) * cos(lat2Rad) * sinDLon * sinDLon
        val c = 2 * atan2(sqrt(h), sqrt(1 - h))
        return EARTH_RADIUS_METERS * c
    }

    // MARK: - 内置拍摄地点数据库（与 iOS loadLocationDatabase 完全对齐，35 个中国摄影地点）

    private fun loadLocationDatabase(): List<PhotoLocation> {
        return listOf(
            // 北京
            PhotoLocation("loc_01", "故宫角楼", "故宫西北角楼是北京最经典的摄影机位之一，护城河倒影与角楼交相辉映，四季皆宜", GeoCoordinate(39.9163, 116.3860), "清晨或黄昏，光线柔和时拍摄最佳", listOf("建筑", "历史", "倒影", "北京"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_02", "颐和园十七孔桥", "金光穿洞奇观闻名遐迩，冬至前后落日余晖穿过桥洞，美不胜收", GeoCoordinate(39.9999, 116.2754), "冬至前后傍晚，金光穿洞奇观", listOf("园林", "桥", "日落", "北京"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_03", "八达岭长城", "万里长城最壮丽的段落之一，蜿蜒于群山之巅，日出日落尤为壮观", GeoCoordinate(40.3597, 116.0185), "秋季清晨日出，游客较少光影好", listOf("长城", "山脉", "历史", "北京"), null, PhotoDifficulty.MEDIUM),
            PhotoLocation("loc_04", "天坛祈年殿", "天坛标志性建筑，三重檐攒尖顶，对称构图极为出片", GeoCoordinate(39.8822, 116.4066), "晴天上午，蓝天下鎏金顶格外耀眼", listOf("建筑", "历史", "对称", "北京"), null, PhotoDifficulty.EASY),

            // 上海
            PhotoLocation("loc_05", "外滩", "黄浦江畔百年建筑群，与陆家嘴摩天大楼隔江相望，新旧对比强烈", GeoCoordinate(31.2400, 121.4905), "傍晚蓝调时刻，灯光初上", listOf("城市", "建筑", "夜景", "上海"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_06", "豫园", "明代江南园林，亭台楼阁错落有致，九曲桥与湖心亭是经典机位", GeoCoordinate(31.2272, 121.4925), "清晨开园时游客较少", listOf("园林", "古建筑", "倒影", "上海"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_07", "陆家嘴环形天桥", "仰望三件套的最佳位置，超广角仰拍极具视觉冲击力", GeoCoordinate(31.2355, 121.5010), "夜晚灯光璀璨时", listOf("城市", "建筑", "夜景", "上海"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_08", "朱家角古镇", "上海近郊水乡古镇，小桥流水，明清建筑保存完好", GeoCoordinate(31.1080, 121.0580), "清晨或傍晚，游客较少的时段", listOf("古镇", "水乡", "人文", "上海"), null, PhotoDifficulty.EASY),

            // 杭州
            PhotoLocation("loc_09", "西湖断桥", "西湖十景之一，白堤起点，雷峰塔与保俶塔遥相呼应", GeoCoordinate(30.2590, 120.1490), "清晨日出或雪后初晴", listOf("湖泊", "桥", "园林", "杭州"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_10", "灵隐寺", "千年古刹，深山藏古寺，黄墙红柱与竹林相映成趣", GeoCoordinate(30.2425, 120.0970), "清晨香火缭绕时分", listOf("寺庙", "古建筑", "山林", "杭州"), null, PhotoDifficulty.EASY),

            // 重庆
            PhotoLocation("loc_11", "洪崖洞", "依山而建的吊脚楼群，夜晚灯火辉煌，宛如千与千寻场景", GeoCoordinate(29.5647, 106.5830), "日落后蓝调时刻，灯光全开", listOf("建筑", "夜景", "山城", "重庆"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_12", "长江索道", "横跨长江的空中索道，拍摄山城立体交通的独特视角", GeoCoordinate(29.5610, 106.5850), "傍晚日落时分", listOf("城市", "交通", "长江", "重庆"), null, PhotoDifficulty.MEDIUM),

            // 成都
            PhotoLocation("loc_13", "宽窄巷子", "成都三大历史文化保护区之一，青砖黛瓦，川西民居风格", GeoCoordinate(30.6680, 104.0560), "清晨游客较少时", listOf("古街", "人文", "建筑", "成都"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_14", "锦里古街", "武侯祠旁的古街，红灯笼挂满街巷，三国文化浓郁", GeoCoordinate(30.6480, 104.0470), "傍晚灯笼亮起时", listOf("古街", "夜景", "人文", "成都"), null, PhotoDifficulty.EASY),

            // 西安
            PhotoLocation("loc_15", "大雁塔", "唐代古塔，大慈恩寺内，广场喷泉与古塔形成现代与古典的碰撞", GeoCoordinate(34.2195, 108.9640), "傍晚喷泉表演时", listOf("古塔", "历史", "喷泉", "西安"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_16", "钟楼", "西安市中心地标，东西南北四条大街交汇处，夜景灯光璀璨", GeoCoordinate(34.2610, 108.9425), "夜晚灯光亮起时", listOf("建筑", "夜景", "历史", "西安"), null, PhotoDifficulty.EASY),

            // 厦门
            PhotoLocation("loc_17", "鼓浪屿", "万国建筑博览，琴声悠扬，海风轻拂，文艺小清新的天堂", GeoCoordinate(24.4479, 118.0680), "清晨或傍晚，避开正午烈日", listOf("海岛", "建筑", "文艺", "厦门"), null, PhotoDifficulty.EASY),

            // 桂林
            PhotoLocation("loc_18", "漓江兴坪", "20元人民币背景取景地，漓江山水甲天下，喀斯特地貌奇观", GeoCoordinate(24.9250, 110.5350), "清晨薄雾或傍晚日落", listOf("山水", "河流", "喀斯特", "桂林"), null, PhotoDifficulty.MEDIUM),
            PhotoLocation("loc_19", "龙脊梯田", "壮族梯田奇观，层层叠叠如登天阶梯，春秋两季尤为壮观", GeoCoordinate(25.7600, 110.1250), "春季灌水或秋季收割时", listOf("梯田", "农耕", "少数民族", "桂林"), null, PhotoDifficulty.HARD),

            // 云南
            PhotoLocation("loc_20", "丽江古城", "世界文化遗产，纳西族建筑风格，小桥流水，四方街热闹非凡", GeoCoordinate(26.8720, 100.2330), "清晨古城苏醒时，游客较少", listOf("古城", "少数民族", "人文", "丽江"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_21", "大理洱海", "苍山洱海，风花雪月，环海公路处处是风景", GeoCoordinate(25.6050, 100.2350), "日出或日落时分，湖面如镜", listOf("湖泊", "山脉", "公路", "大理"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_22", "元阳梯田", "哈尼族梯田，被誉为大地的雕塑，日出时分云雾缭绕如仙境", GeoCoordinate(23.0930, 102.7800), "冬季清晨，梯田灌水反射天光", listOf("梯田", "日出", "云雾", "云南"), null, PhotoDifficulty.HARD),

            // 四川
            PhotoLocation("loc_23", "稻城亚丁", "蓝色星球上最后一片净土，三座神山巍峨矗立，牛奶海五色海如宝石", GeoCoordinate(28.4680, 100.2940), "秋季10月，色彩最丰富", listOf("雪山", "湖泊", "高原", "四川"), null, PhotoDifficulty.HARD),
            PhotoLocation("loc_24", "九寨沟", "人间仙境，五彩池、诺日朗瀑布，水色斑斓令人叹为观止", GeoCoordinate(33.2630, 103.9030), "秋季10月中下旬，彩林与碧水", listOf("湖泊", "瀑布", "彩林", "四川"), null, PhotoDifficulty.MEDIUM),

            // 张家界
            PhotoLocation("loc_25", "张家界天子山", "石英砂岩峰林地貌，阿凡达取景地，云雾缭绕如悬浮山", GeoCoordinate(29.3450, 110.4350), "雨后初晴，云雾缭绕时", listOf("山峰", "云雾", "自然", "张家界"), null, PhotoDifficulty.MEDIUM),

            // 黄山
            PhotoLocation("loc_26", "黄山迎客松", "天下第一奇山，奇松、怪石、云海、温泉四绝，日出壮观", GeoCoordinate(30.1340, 118.1670), "冬季雪后或日出时分", listOf("山峰", "云海", "日出", "黄山"), null, PhotoDifficulty.HARD),

            // 苏州
            PhotoLocation("loc_27", "拙政园", "中国四大名园之一，江南园林代表作，移步换景处处是画", GeoCoordinate(31.3260, 120.6290), "春季花开或秋季红叶时", listOf("园林", "古建筑", "苏州"), null, PhotoDifficulty.EASY),

            // 南京
            PhotoLocation("loc_28", "中山陵", "孙中山先生陵寝，392级台阶气势恢宏，俯瞰整个南京城", GeoCoordinate(32.0645, 118.8480), "秋季梧桐金黄时", listOf("陵墓", "建筑", "历史", "南京"), null, PhotoDifficulty.EASY),

            // 青岛
            PhotoLocation("loc_29", "八大关", "万国建筑博览区，红瓦绿树碧海蓝天，德式建筑与海岸线交织", GeoCoordinate(36.0560, 120.3460), "秋季银杏金黄时", listOf("建筑", "海岸", "秋叶", "青岛"), null, PhotoDifficulty.EASY),

            // 深圳
            PhotoLocation("loc_30", "深圳湾公园", "城市与自然交融，15公里滨海长廊，日出方向正对香港", GeoCoordinate(22.5170, 113.9480), "清晨日出时分", listOf("海岸", "城市", "日出", "深圳"), null, PhotoDifficulty.EASY),

            // 广州
            PhotoLocation("loc_31", "广州塔", "小蛮腰，600米高空俯瞰珠江新城，城市夜景迷人", GeoCoordinate(23.1065, 113.3240), "傍晚蓝调时刻，灯光亮起", listOf("建筑", "夜景", "城市", "广州"), null, PhotoDifficulty.EASY),
            PhotoLocation("loc_32", "沙面", "欧陆风情建筑群，百年榕树遮天蔽日，文艺街拍圣地", GeoCoordinate(23.1100, 113.2440), "下午阳光透过榕树叶洒落", listOf("建筑", "历史", "街拍", "广州"), null, PhotoDifficulty.EASY),

            // 武汉
            PhotoLocation("loc_33", "黄鹤楼", "天下江山第一楼，长江与汉江交汇处，登楼远眺气势磅礴", GeoCoordinate(30.5470, 114.3030), "傍晚日落时分", listOf("古楼", "长江", "历史", "武汉"), null, PhotoDifficulty.EASY),

            // 拉萨
            PhotoLocation("loc_34", "布达拉宫", "世界屋脊上的宫殿，红白相间的宫殿群，信仰的力量", GeoCoordinate(29.6578, 91.1169), "清晨日出或傍晚日落", listOf("宫殿", "高原", "宗教", "拉萨"), null, PhotoDifficulty.HARD),

            // 哈尔滨
            PhotoLocation("loc_35", "圣索菲亚教堂", "远东最大东正教堂，拜占庭风格建筑，雪后更显庄严", GeoCoordinate(45.7680, 126.6240), "冬季雪后或夜晚灯光", listOf("教堂", "建筑", "雪景", "哈尔滨"), null, PhotoDifficulty.EASY)
        )
    }
}
