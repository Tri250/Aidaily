package com.livecompose.livecapture.core.community

import android.content.Context
import androidx.datastore.core.DataStore
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
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

private val Context.communityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "community_manager"
)

/**
 * 社区管理器（合并 iOS 端 PhotoChallengeManager + FilterCommunityManager 功能）
 *
 * 职责：
 * 1. 照片挑战：12 个内置挑战 + 每周轮换 + 提交作品 + 投票 + 排行榜
 * 2. 滤镜社区：25 个内置社区滤镜 + 创建/下载/移除 + 热门排序 + 分类筛选
 *
 * 持久化：androidx.datastore.preferences + Gson（参考 AlbumManager 模式）
 * 状态管理：MutableStateFlow + StateFlow，UI 通过 collectAsState 订阅
 * 异步：所有持久化与初始化在 Dispatchers.IO 上执行
 */
class CommunityManager(private val context: Context) {

    companion object {
        private const val TAG = "CommunityManager"

        /** 一周的毫秒数（与 iOS weekInSeconds 对齐） */
        private const val WEEK_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

        /** 热门滤镜默认返回数量 */
        private const val POPULAR_FILTER_LIMIT = 10

        // DataStore Keys
        private val CHALLENGES_KEY = stringPreferencesKey("challenges_data")
        private val COMMUNITY_FILTERS_KEY = stringPreferencesKey("community_filters_data")
        private val MY_FILTERS_KEY = stringPreferencesKey("my_filters_data")
        private val DOWNLOADED_FILTERS_KEY = stringPreferencesKey("downloaded_filters_data")
    }

    private val gson = Gson()
    private val store = context.communityDataStore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // MARK: - 挑战相关 StateFlow

    /** 当前进行中的挑战 */
    private val _currentChallenge = MutableStateFlow<PhotoChallenge?>(null)
    val currentChallenge: StateFlow<PhotoChallenge?> = _currentChallenge.asStateFlow()

    /** 即将到来的挑战（按开始时间升序） */
    private val _upcomingChallenges = MutableStateFlow<List<PhotoChallenge>>(emptyList())
    val upcomingChallenges: StateFlow<List<PhotoChallenge>> = _upcomingChallenges.asStateFlow()

    /** 往期挑战（按结束时间降序） */
    private val _pastChallenges = MutableStateFlow<List<PhotoChallenge>>(emptyList())
    val pastChallenges: StateFlow<List<PhotoChallenge>> = _pastChallenges.asStateFlow()

    // MARK: - 滤镜相关 StateFlow

    /** 社区滤镜库 */
    private val _communityFilters = MutableStateFlow<List<UserFilter>>(emptyList())
    val communityFilters: StateFlow<List<UserFilter>> = _communityFilters.asStateFlow()

    /** 我创建的滤镜 */
    private val _myCreatedFilters = MutableStateFlow<List<UserFilter>>(emptyList())
    val myCreatedFilters: StateFlow<List<UserFilter>> = _myCreatedFilters.asStateFlow()

    /** 热门滤镜（按下载量排序） */
    private val _popularFilters = MutableStateFlow<List<UserFilter>>(emptyList())
    val popularFilters: StateFlow<List<UserFilter>> = _popularFilters.asStateFlow()

    /** 已下载滤镜 */
    private val _downloadedFilters = MutableStateFlow<List<UserFilter>>(emptyList())
    val downloadedFilters: StateFlow<List<UserFilter>> = _downloadedFilters.asStateFlow()

    // MARK: - 初始化

    init {
        scope.launch {
            loadAll()
        }
    }

    /**
     * 加载所有社区数据（挑战 + 滤镜）
     */
    private suspend fun loadAll() {
        try {
            loadChallengesInternal()
            loadCommunityFiltersInternal()
            loadMyFiltersInternal()
            loadDownloadedFiltersInternal()
            refreshPopularFilters()
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载社区数据失败", e)
        }
    }

    // MARK: - 12 个内置挑战定义（与 iOS builtInChallenges 对齐）

    /**
     * 生成 12 个内置挑战（按周轮换，基准周从当前周一零点开始）
     */
    private fun builtInChallenges(): List<PhotoChallenge> {
        val now = System.currentTimeMillis()
        val baseMonday = currentWeekMondayStart(now)

        // 与 iOS 完全一致的 12 个挑战定义
        val definitions = listOf(
            Triple("光影猎人", "捕捉城市中令人惊叹的光影对比，让明暗交错的画面讲述故事", ChallengeTheme.LIGHT_SHADOW),
            Triple("极简之美", "少即是多，用最简洁的构图展现最纯粹的美感", ChallengeTheme.MINIMALISM),
            Triple("色彩狂欢", "寻找城市中最鲜艳、最富冲击力的色彩组合", ChallengeTheme.COLOR),
            Triple("老街故事", "漫步城市老街，用镜头记录市井生活的温度", ChallengeTheme.STREET),
            Triple("自然之美", "走进大自然，发现山川、湖泊、森林的壮丽景色", ChallengeTheme.NATURE),
            Triple("美食日记", "记录令人垂涎的美食瞬间，让味蕾通过视觉绽放", ChallengeTheme.FOOD),
            Triple("城市天际线", "仰望城市建筑与天空的交汇，捕捉天际线的韵律", ChallengeTheme.ARCHITECTURE),
            Triple("微距世界", "放大微观世界，发现不易察觉的细节之美", ChallengeTheme.MACRO),
            Triple("黑白影像", "舍弃色彩，用纯粹的明暗讲述故事", ChallengeTheme.BLACK_AND_WHITE),
            Triple("倒影之美", "寻找水面、镜面中的对称世界，捕捉倒影的梦幻", ChallengeTheme.REFLECTION),
            Triple("剪影艺术", "利用逆光创造剪影，用轮廓诉说故事", ChallengeTheme.SILHOUETTE),
            Triple("宠物时光", "记录萌宠的可爱瞬间，分享生命中的温暖陪伴", ChallengeTheme.PET)
        )

        return definitions.mapIndexed { index, (title, description, theme) ->
            val weekOffset = index.toLong() * WEEK_MILLIS
            val startDate = baseMonday + weekOffset
            val endDate = startDate + WEEK_MILLIS - 1
            // 仅第一周（当前周）的挑战默认激活
            val isActive = index == 0 && now in startDate..endDate

            PhotoChallenge(
                id = "challenge_$index",
                title = title,
                description = description,
                theme = theme,
                startDate = startDate,
                endDate = endDate,
                userEntries = emptyList(),
                isActive = isActive
            )
        }
    }

    /**
     * 计算当前周周一零点的时间戳（与 iOS baseMonday 对齐逻辑）
     */
    private fun currentWeekMondayStart(now: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = now
        // 对齐到本周一零点：周日=1，周一=2 ... 周六=7
        val weekday = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToMonday = (weekday - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -daysToMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // MARK: - 挑战加载与轮换

    /**
     * 加载挑战：优先从 DataStore 读取，无缓存则使用内置挑战并持久化
     */
    private suspend fun loadChallengesInternal() {
        val saved = loadSavedChallenges()
        val challenges = saved ?: builtInChallenges().also { persistChallenges(it) }
        val now = System.currentTimeMillis()
        val rotated = applyRotation(challenges, now)
        applyCategorization(rotated, now)
        persistChallenges(rotated)
    }

    /**
     * 检查并轮换挑战（与 iOS checkAndRotateChallenge 对齐，可在 App 启动或前台时调用）
     */
    suspend fun checkAndRotateChallenge() {
        withContext(Dispatchers.IO) {
            val challenges = loadSavedChallenges() ?: builtInChallenges()
            val now = System.currentTimeMillis()
            val rotated = applyRotation(challenges, now)
            applyCategorization(rotated, now)
            persistChallenges(rotated)
            AppLogger.d(TAG, "挑战轮换检查完成")
        }
    }

    /**
     * 根据当前时间更新各挑战的 isActive 标志（与 iOS checkRotation 对齐）
     */
    private fun applyRotation(challenges: List<PhotoChallenge>, now: Long): List<PhotoChallenge> {
        return challenges.map { challenge ->
            challenge.copy(isActive = now in challenge.startDate..challenge.endDate)
        }
    }

    /**
     * 将挑战分类为当前/即将到来/往期，并更新对应 StateFlow（与 iOS categorizeChallenges 对齐）
     */
    private fun applyCategorization(challenges: List<PhotoChallenge>, now: Long) {
        val current = challenges.firstOrNull { it.isActive }
        val upcoming = challenges
            .filter { it.startDate > now }
            .sortedBy { it.startDate }
        val past = challenges
            .filter { it.endDate < now }
            .sortedByDescending { it.endDate }

        _currentChallenge.value = current
        _upcomingChallenges.value = upcoming
        _pastChallenges.value = past
    }

    // MARK: - 参赛

    /**
     * 提交作品（与 iOS submitEntry(to:photoFileName:title:) 对齐）
     */
    suspend fun submitEntry(challengeId: String, photoFileName: String, title: String) {
        withContext(Dispatchers.IO) {
            val challenges = loadSavedChallenges() ?: builtInChallenges()
            val index = challenges.indexOfFirst { it.id == challengeId }
            if (index < 0) {
                AppLogger.w(TAG, "提交作品失败：未找到挑战 $challengeId")
                return@withContext
            }

            val entry = ChallengeEntry(
                id = UUID.randomUUID().toString(),
                photoFileName = photoFileName,
                submittedDate = System.currentTimeMillis(),
                title = title,
                votes = 0,
                hasVoted = false
            )

            val updated = challenges.toMutableList()
            updated[index] = updated[index].copy(userEntries = updated[index].userEntries + entry)

            val now = System.currentTimeMillis()
            applyCategorization(updated, now)
            persistChallenges(updated)
            AppLogger.d(TAG, "作品已提交至挑战 $challengeId：$title")
        }
    }

    // MARK: - 投票

    /**
     * 为作品投票（与 iOS voteForEntry(_:in:) 对齐，每人每作品仅可投一次）
     */
    suspend fun voteForEntry(challengeId: String, entryId: String) {
        withContext(Dispatchers.IO) {
            val challenges = loadSavedChallenges() ?: builtInChallenges()
            val challengeIndex = challenges.indexOfFirst { it.id == challengeId }
            if (challengeIndex < 0) return@withContext

            val challenge = challenges[challengeIndex]
            val entryIndex = challenge.userEntries.indexOfFirst { it.id == entryId }
            if (entryIndex < 0) return@withContext
            if (challenge.userEntries[entryIndex].hasVoted) {
                AppLogger.w(TAG, "已投过票，无法重复投票：entry=$entryId")
                return@withContext
            }

            val updatedEntries = challenge.userEntries.toMutableList()
            updatedEntries[entryIndex] = updatedEntries[entryIndex].copy(
                votes = updatedEntries[entryIndex].votes + 1,
                hasVoted = true
            )

            val updated = challenges.toMutableList()
            updated[challengeIndex] = challenge.copy(userEntries = updatedEntries)

            val now = System.currentTimeMillis()
            applyCategorization(updated, now)
            persistChallenges(updated)
            AppLogger.d(TAG, "投票成功：challenge=$challengeId entry=$entryId")
        }
    }

    // MARK: - 排行榜

    /**
     * 获取挑战排行榜（与 iOS getLeaderboard 对齐，按票数降序）
     */
    fun getLeaderboard(challengeId: String): List<ChallengeEntry> {
        // 使用内存中 StateFlow 的最新值（同步、反映最近一次投票/提交结果）
        val challenge = sequence {
            yield(_currentChallenge.value)
            yieldAll(_upcomingChallenges.value)
            yieldAll(_pastChallenges.value)
        }.filterNotNull().firstOrNull { it.id == challengeId } ?: return emptyList()
        return challenge.userEntries.sortedByDescending { it.votes }
    }

    // MARK: - 滤镜社区：加载内置

    /**
     * 加载社区滤镜：优先从 DataStore 读取，无缓存则使用内置滤镜并持久化
     */
    private suspend fun loadCommunityFiltersInternal() {
        val saved = loadSavedCommunityFilters()
        _communityFilters.value = if (saved.isNullOrEmpty()) {
            val builtIn = builtInCommunityFilters()
            persistCommunityFilters(builtIn)
            builtIn
        } else {
            saved
        }
    }

    // MARK: - 25 款内置社区滤镜（与 iOS builtInCommunityFilters 完全对齐）

    private fun builtInCommunityFilters(): List<UserFilter> {
        val creators = listOf("JettyCoffee", "ZyanNo1", "zzsyppt", "光影猎人", "极简大师", "街头摄影师", "美食博主", "旅行达人")
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        return listOf(
            // 1-5: 城市夜景系列
            UserFilter("cf_1", "东京午夜", creators[0], FilterParameters(temperature = -1200f, tint = -20f, exposure = -0.15f, brightness = -0.08f, contrast = 1.30f, saturation = 1.15f, vibrance = 0.12f, highlightAmount = 0.85f, shadowAmount = -0.10f), null, now - day * 30, 1520, FilterCategory.CREATIVE, "霓虹灯下的东京街头，青橙色调营造赛博朋克氛围"),
            UserFilter("cf_2", "巴黎清晨", creators[1], FilterParameters(temperature = 600f, tint = 5f, exposure = 0.20f, brightness = 0.08f, contrast = 0.85f, saturation = 0.92f, vibrance = 0.05f, highlightAmount = 0.78f, shadowAmount = 0.20f), null, now - day * 28, 1340, FilterCategory.NATURE, "巴黎清晨的柔和光线，温暖的氛围感"),
            UserFilter("cf_3", "冰岛蓝调", creators[2], FilterParameters(temperature = -2000f, tint = -30f, exposure = 0.10f, brightness = 0.03f, contrast = 1.10f, saturation = 0.85f, vibrance = -0.05f, highlightAmount = 0.90f, shadowAmount = 0.05f), null, now - day * 25, 2180, FilterCategory.NATURE, "冰岛冰川与蓝湖的冷色调，清冷而神秘"),
            UserFilter("cf_4", "摩洛哥暖阳", creators[0], FilterParameters(temperature = 2500f, tint = 15f, exposure = 0.15f, brightness = 0.10f, contrast = 1.05f, saturation = 1.35f, vibrance = 0.20f, highlightAmount = 0.92f, shadowAmount = 0.15f), null, now - day * 22, 980, FilterCategory.VINTAGE, "摩洛哥市场暖阳色调，浓郁的地中海色彩"),
            UserFilter("cf_5", "北欧极简", creators[3], FilterParameters(temperature = -300f, tint = -5f, exposure = 0.25f, brightness = 0.12f, contrast = 0.78f, saturation = 0.70f, vibrance = -0.10f, highlightAmount = 0.75f, shadowAmount = 0.25f), null, now - day * 20, 1650, FilterCategory.CREATIVE, "北欧极简风格，低饱和度、高亮度的通透感"),
            // 6-10: 人像系列
            UserFilter("cf_6", "奶油肌肤", creators[1], FilterParameters(temperature = 500f, tint = 8f, exposure = 0.20f, brightness = 0.10f, contrast = 0.80f, saturation = 0.95f, vibrance = 0.05f, highlightAmount = 0.82f, shadowAmount = 0.18f), null, now - day * 18, 3200, FilterCategory.PORTRAIT, "柔和肤色处理，打造奶油般细腻的肌肤质感"),
            UserFilter("cf_7", "复古胶片人像", creators[4], FilterParameters(temperature = 1500f, tint = 10f, exposure = -0.05f, brightness = -0.03f, contrast = 1.15f, saturation = 1.10f, vibrance = 0.08f, highlightAmount = 0.88f, shadowAmount = 0.05f), null, now - day * 15, 890, FilterCategory.PORTRAIT, "复古胶片质感，温暖色调让人像更有故事感"),
            UserFilter("cf_8", "清新日系", creators[5], FilterParameters(temperature = -200f, tint = -3f, exposure = 0.35f, brightness = 0.15f, contrast = 0.75f, saturation = 0.88f, vibrance = -0.03f, highlightAmount = 0.78f, shadowAmount = 0.30f), null, now - day * 12, 2800, FilterCategory.PORTRAIT, "日系清新风格，明亮通透，适合日常人像"),
            UserFilter("cf_9", "暗调情绪", creators[2], FilterParameters(temperature = -500f, tint = -10f, exposure = -0.30f, brightness = -0.10f, contrast = 1.40f, saturation = 0.75f, vibrance = -0.08f, highlightAmount = 0.95f, shadowAmount = -0.15f), null, now - day * 10, 1450, FilterCategory.PORTRAIT, "暗调情绪人像，深沉而富有表现力"),
            UserFilter("cf_10", "金色时刻", creators[0], FilterParameters(temperature = 2000f, tint = 12f, exposure = 0.10f, brightness = 0.05f, contrast = 0.95f, saturation = 1.20f, vibrance = 0.15f, highlightAmount = 0.85f, shadowAmount = 0.10f), null, now - day * 8, 1950, FilterCategory.PORTRAIT, "黄金时刻色调，温暖的金色光线洒满画面"),
            // 11-15: 美食/生活系列
            UserFilter("cf_11", "食欲大增", creators[6], FilterParameters(temperature = 1500f, tint = 5f, exposure = 0.15f, brightness = 0.08f, contrast = 1.15f, saturation = 1.40f, vibrance = 0.25f, highlightAmount = 0.90f, shadowAmount = 0.10f), null, now - day * 7, 2100, FilterCategory.FOOD, "增加食物饱和度与暖色调，让每一道菜都诱人无比"),
            UserFilter("cf_12", "咖啡时光", creators[7], FilterParameters(temperature = 1800f, tint = 8f, exposure = 0.05f, brightness = 0.03f, contrast = 1.05f, saturation = 1.05f, vibrance = 0.08f, highlightAmount = 0.88f, shadowAmount = 0.12f), null, now - day * 5, 1250, FilterCategory.FOOD, "咖啡馆暖调氛围，适合记录惬意时光"),
            UserFilter("cf_13", "甜品诱惑", creators[6], FilterParameters(temperature = 1000f, tint = 3f, exposure = 0.25f, brightness = 0.12f, contrast = 0.90f, saturation = 1.25f, vibrance = 0.18f, highlightAmount = 0.85f, shadowAmount = 0.20f), null, now - day * 4, 1800, FilterCategory.FOOD, "甜品专属滤镜，柔和明亮，突出甜品的精致感"),
            UserFilter("cf_14", "深夜食堂", creators[5], FilterParameters(temperature = 2000f, tint = 10f, exposure = -0.10f, brightness = -0.05f, contrast = 1.20f, saturation = 1.15f, vibrance = 0.10f, highlightAmount = 0.92f, shadowAmount = -0.05f), null, now - day * 3, 780, FilterCategory.FOOD, "深夜食堂的暖黄灯光，营造温馨的用餐氛围"),
            UserFilter("cf_15", "生活碎片", creators[4], FilterParameters(temperature = 300f, tint = 0f, exposure = 0.10f, brightness = 0.05f, contrast = 0.95f, saturation = 1.05f, vibrance = 0.05f, highlightAmount = 0.90f, shadowAmount = 0.08f), null, now - day * 2, 1600, FilterCategory.VINTAGE, "记录日常生活的温暖滤镜，真实而自然"),
            // 16-20: 创意/黑白系列
            UserFilter("cf_16", "赛博朋克", creators[0], FilterParameters(temperature = -1500f, tint = -25f, exposure = -0.10f, brightness = -0.05f, contrast = 1.50f, saturation = 1.30f, vibrance = 0.20f, highlightAmount = 0.80f, shadowAmount = -0.15f), null, now - day * 35, 2300, FilterCategory.CREATIVE, "赛博朋克风格，高对比度青橙色调，未来感十足"),
            UserFilter("cf_17", "经典黑白", creators[3], FilterParameters(temperature = 0f, tint = 0f, exposure = 0f, brightness = -0.05f, contrast = 1.35f, saturation = 0f, vibrance = 0f, highlightAmount = 1.0f, shadowAmount = -0.08f, isMonochrome = true, monochromeIntensity = 1.0f, monochromeColorR = 0.95f, monochromeColorG = 0.94f, monochromeColorB = 0.92f), null, now - day * 32, 1100, FilterCategory.BW, "经典黑白影调，纯粹的光影表达"),
            UserFilter("cf_18", "暖调黑白", creators[1], FilterParameters(temperature = 0f, tint = 0f, exposure = 0.05f, brightness = 0.02f, contrast = 1.20f, saturation = 0f, vibrance = 0f, highlightAmount = 0.95f, shadowAmount = 0.05f, isMonochrome = true, monochromeIntensity = 1.0f, monochromeColorR = 0.98f, monochromeColorG = 0.92f, monochromeColorB = 0.85f), null, now - day * 30, 850, FilterCategory.BW, "温暖色调的黑白风格，带一丝怀旧感"),
            UserFilter("cf_19", "梦幻柔焦", creators[2], FilterParameters(temperature = 500f, tint = 5f, exposure = 0.30f, brightness = 0.15f, contrast = 0.65f, saturation = 0.85f, vibrance = -0.05f, highlightAmount = 0.70f, shadowAmount = 0.30f), null, now - day * 27, 1900, FilterCategory.CREATIVE, "梦幻柔焦效果，高光溢出营造浪漫氛围"),
            UserFilter("cf_20", "老照片", creators[7], FilterParameters(temperature = 2000f, tint = 15f, exposure = 0.10f, brightness = 0.05f, contrast = 0.70f, saturation = 0.60f, vibrance = -0.15f, highlightAmount = 0.65f, shadowAmount = 0.30f), null, now - day * 24, 1350, FilterCategory.VINTAGE, "老照片风格，褪色、泛黄、低对比度，营造年代感"),
            // 附加 5 款
            UserFilter("cf_21", "西山晚霞", creators[4], FilterParameters(temperature = 3000f, tint = 20f, exposure = 0.05f, brightness = 0.03f, contrast = 1.10f, saturation = 1.30f, vibrance = 0.22f, highlightAmount = 0.88f, shadowAmount = 0.12f), null, now - day * 19, 750, FilterCategory.NATURE, "日落晚霞色调，绚丽的天空色彩"),
            UserFilter("cf_22", "雨巷", creators[5], FilterParameters(temperature = -800f, tint = -15f, exposure = -0.20f, brightness = -0.08f, contrast = 0.90f, saturation = 0.70f, vibrance = -0.10f, highlightAmount = 0.92f, shadowAmount = 0.05f), null, now - day * 16, 620, FilterCategory.CREATIVE, "雨天氛围，冷色调、低饱和度，营造忧郁浪漫"),
            UserFilter("cf_23", "胶片褪色", creators[0], FilterParameters(temperature = 1200f, tint = 8f, exposure = 0.15f, brightness = 0.06f, contrast = 0.75f, saturation = 0.72f, vibrance = -0.08f, highlightAmount = 0.72f, shadowAmount = 0.25f), null, now - day * 14, 1580, FilterCategory.FILM, "胶片褪色效果，温暖的复古质感"),
            UserFilter("cf_24", "青橙调", creators[2], FilterParameters(temperature = -1000f, tint = -20f, exposure = 0f, brightness = -0.02f, contrast = 1.25f, saturation = 1.20f, vibrance = 0.15f, highlightAmount = 0.88f, shadowAmount = -0.05f), null, now - day * 11, 2800, FilterCategory.CREATIVE, "经典青橙对比色调，电影感十足"),
            UserFilter("cf_25", "春日花语", creators[3], FilterParameters(temperature = 400f, tint = -5f, exposure = 0.25f, brightness = 0.10f, contrast = 0.85f, saturation = 1.15f, vibrance = 0.10f, highlightAmount = 0.82f, shadowAmount = 0.22f), null, now - day * 9, 1050, FilterCategory.NATURE, "春日花卉滤镜，明亮柔和的色调")
        )
    }

    // MARK: - 创建滤镜

    /**
     * 创建自定义滤镜（与 iOS createFilter 对齐）
     */
    suspend fun createFilter(name: String, parameters: FilterParameters, category: FilterCategory) {
        withContext(Dispatchers.IO) {
            val newFilter = UserFilter(
                id = UUID.randomUUID().toString(),
                name = name,
                creatorName = "我",
                parameters = parameters,
                previewImageName = null,
                createdDate = System.currentTimeMillis(),
                downloads = 0,
                category = category,
                filterDescription = "我的自定义滤镜"
            )
            _myCreatedFilters.value = _myCreatedFilters.value + newFilter
            persistMyFilters()
            AppLogger.d(TAG, "创建滤镜：$name")
        }
    }

    // MARK: - 下载/移除滤镜

    /**
     * 下载滤镜（与 iOS downloadFilter 对齐，递增社区滤镜下载量并加入已下载列表）
     */
    suspend fun downloadFilter(filter: UserFilter) {
        withContext(Dispatchers.IO) {
            if (_downloadedFilters.value.any { it.id == filter.id }) {
                AppLogger.w(TAG, "滤镜已下载，跳过：${filter.id}")
                return@withContext
            }

            // 递增社区滤镜下载量（若来源社区）
            val communityIndex = _communityFilters.value.indexOfFirst { it.id == filter.id }
            if (communityIndex >= 0) {
                val updatedCommunity = _communityFilters.value.toMutableList()
                updatedCommunity[communityIndex] = updatedCommunity[communityIndex].copy(
                    downloads = updatedCommunity[communityIndex].downloads + 1
                )
                _communityFilters.value = updatedCommunity
                persistCommunityFilters(updatedCommunity)
            }

            _downloadedFilters.value = _downloadedFilters.value + filter
            persistDownloadedFilters()
            refreshPopularFilters()
            AppLogger.d(TAG, "下载滤镜：${filter.name}")
        }
    }

    /**
     * 移除已下载滤镜（与 iOS removeFilter 对齐）
     */
    suspend fun removeFilter(filter: UserFilter) {
        withContext(Dispatchers.IO) {
            _downloadedFilters.value = _downloadedFilters.value.filterNot { it.id == filter.id }
            persistDownloadedFilters()
            AppLogger.d(TAG, "移除滤镜：${filter.name}")
        }
    }

    /**
     * 判断滤镜是否已下载（与 iOS isDownloaded 对齐）
     */
    fun isDownloaded(filter: UserFilter): Boolean =
        _downloadedFilters.value.any { it.id == filter.id }

    /**
     * 判断滤镜是否已下载（同步快照版本，供 Compose 直接调用）
     */
    fun isDownloadedById(filterId: String): Boolean =
        _downloadedFilters.value.any { it.id == filterId }

    // MARK: - 热门排序

    /**
     * 获取热门滤镜（与 iOS getPopularFilters 对齐，按下载量降序）
     */
    fun getPopularFilters(limit: Int = POPULAR_FILTER_LIMIT): List<UserFilter> {
        return _communityFilters.value
            .sortedByDescending { it.downloads }
            .take(limit)
    }

    /**
     * 刷新热门滤镜列表（与 iOS refreshPopularFilters 对齐）
     */
    fun refreshPopularFilters() {
        _popularFilters.value = getPopularFilters(POPULAR_FILTER_LIMIT)
    }

    // MARK: - 分类筛选

    /**
     * 按分类筛选社区滤镜（与 iOS getFiltersForCategory 对齐）
     */
    fun getFiltersForCategory(category: FilterCategory): List<UserFilter> {
        return _communityFilters.value.filter { it.category == category }
    }

    // MARK: - 持久化：挑战

    private suspend fun persistChallenges(challenges: List<PhotoChallenge>) {
        try {
            val json = gson.toJson(challenges)
            store.edit { it[CHALLENGES_KEY] = json }
        } catch (e: Exception) {
            AppLogger.e(TAG, "持久化挑战失败", e)
        }
    }

    private suspend fun loadSavedChallenges(): List<PhotoChallenge>? {
        return try {
            val json = store.data.first()[CHALLENGES_KEY] ?: return null
            val type = challengeListType()
            val list: List<PhotoChallenge> = gson.fromJson(json, type) ?: return null
            list
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载挑战失败", e)
            null
        }
    }

    // MARK: - 持久化：社区滤镜

    private suspend fun persistCommunityFilters(filters: List<UserFilter>) {
        try {
            val json = gson.toJson(filters)
            store.edit { it[COMMUNITY_FILTERS_KEY] = json }
        } catch (e: Exception) {
            AppLogger.e(TAG, "持久化社区滤镜失败", e)
        }
    }

    private suspend fun loadSavedCommunityFilters(): List<UserFilter>? {
        return try {
            val json = store.data.first()[COMMUNITY_FILTERS_KEY] ?: return null
            val type = filterListType()
            gson.fromJson(json, type)
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载社区滤镜失败", e)
            null
        }
    }

    // MARK: - 持久化：我的滤镜

    private suspend fun persistMyFilters() {
        try {
            val json = gson.toJson(_myCreatedFilters.value)
            store.edit { it[MY_FILTERS_KEY] = json }
        } catch (e: Exception) {
            AppLogger.e(TAG, "持久化我的滤镜失败", e)
        }
    }

    private suspend fun loadMyFiltersInternal() {
        try {
            val json = store.data.first()[MY_FILTERS_KEY]
            val type = filterListType()
            _myCreatedFilters.value = json?.let { gson.fromJson(it, type) } ?: emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载我的滤镜失败", e)
            _myCreatedFilters.value = emptyList()
        }
    }

    // MARK: - 持久化：已下载滤镜

    private suspend fun persistDownloadedFilters() {
        try {
            val json = gson.toJson(_downloadedFilters.value)
            store.edit { it[DOWNLOADED_FILTERS_KEY] = json }
        } catch (e: Exception) {
            AppLogger.e(TAG, "持久化已下载滤镜失败", e)
        }
    }

    private suspend fun loadDownloadedFiltersInternal() {
        try {
            val json = store.data.first()[DOWNLOADED_FILTERS_KEY]
            val type = filterListType()
            _downloadedFilters.value = json?.let { gson.fromJson(it, type) } ?: emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载已下载滤镜失败", e)
            _downloadedFilters.value = emptyList()
        }
    }

    // MARK: - Gson TypeToken 工具

    private fun challengeListType() = object : TypeToken<List<PhotoChallenge>>() {}.type
    private fun filterListType() = object : TypeToken<List<UserFilter>>() {}.type
}
