package com.livecompose.livecapture.core.community

/**
 * 社区与社交生态系统数据模型
 *
 * 与 iOS 端 CommunityModels.swift + FilterModels.swift 功能对齐：
 * - 地理坐标 / 拍照地点
 * - 照片挑战 / 参赛作品
 * - 用户滤镜 / 滤镜参数 / 滤镜分类
 * - 社区帖子
 *
 * 所有时间戳统一使用 epoch 毫秒（Long），便于 DataStore + Gson 持久化。
 */

// MARK: - 地理坐标

/**
 * 地理坐标（与 iOS CodableCoordinate 对齐）
 */
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
)

// MARK: - 拍照地点难度

/**
 * 拍照地点难度（与 iOS PhotoLocation.Difficulty 对齐）
 */
enum class PhotoDifficulty(val displayName: String) {
    EASY("轻松"),
    MEDIUM("中等"),
    HARD("挑战");

    companion object {
        fun fromName(name: String): PhotoDifficulty =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: EASY
    }
}

// MARK: - 拍照地点

/**
 * 拍照地点推荐数据模型（与 iOS PhotoLocation 对齐）
 *
 * @param id 唯一标识
 * @param name 地点名称
 * @param description 地点描述
 * @param coordinate 经纬度
 * @param bestTime 最佳拍摄时间
 * @param tags 标签列表（用于分类筛选）
 * @param samplePhotoName 示例图片名（可选）
 * @param difficulty 拍摄难度
 */
data class PhotoLocation(
    val id: String,
    val name: String,
    val description: String,
    val coordinate: GeoCoordinate,
    val bestTime: String,
    val tags: List<String>,
    val samplePhotoName: String? = null,
    val difficulty: PhotoDifficulty
)

// MARK: - 挑战主题

/**
 * 照片挑战主题（与 iOS PhotoChallenge.ChallengeTheme 对齐）
 *
 * @param displayName 中文展示名
 * @param iconName Material 图标键（由 UI 层映射为 ImageVector）
 */
enum class ChallengeTheme(val displayName: String, val iconName: String) {
    PORTRAIT("人像", "person"),
    LANDSCAPE("风光", "landscape"),
    FOOD("美食", "restaurant"),
    STREET("街拍", "location_city"),
    NIGHT("夜景", "nightlight"),
    NATURE("自然", "eco"),
    ARCHITECTURE("建筑", "account_balance"),
    MACRO("微距", "bug_report"),
    BLACK_AND_WHITE("黑白", "contrast"),
    MINIMALISM("极简", "radio_button_unchecked"),
    REFLECTION("倒影", "waves"),
    SILHOUETTE("剪影", "center_focus_strong"),
    PET("宠物", "pets"),
    COLOR("色彩", "palette"),
    LIGHT_SHADOW("光影", "light_mode"),
    URBAN("城市", "apartment"),
    VINTAGE("复古", "history"),
    WATER("水景", "water_drop"),
    SKY("天空", "cloud")
}

// MARK: - 照片挑战

/**
 * 照片挑战（与 iOS PhotoChallenge 对齐）
 *
 * @param id 唯一标识
 * @param title 挑战标题
 * @param description 挑战描述
 * @param theme 挑战主题
 * @param startDate 开始时间（epoch 毫秒）
 * @param endDate 结束时间（epoch 毫秒）
 * @param userEntries 用户参赛作品列表
 * @param isActive 是否当前进行中
 */
data class PhotoChallenge(
    val id: String,
    val title: String,
    val description: String,
    val theme: ChallengeTheme,
    val startDate: Long,
    val endDate: Long,
    val userEntries: List<ChallengeEntry>,
    val isActive: Boolean
)

// MARK: - 参赛作品

/**
 * 挑战参赛作品（与 iOS ChallengeEntry 对齐）
 *
 * @param id 唯一标识
 * @param photoFileName 照片文件名
 * @param submittedDate 提交时间（epoch 毫秒）
 * @param title 作品标题
 * @param votes 投票数
 * @param hasVoted 当前用户是否已投票
 */
data class ChallengeEntry(
    val id: String,
    val photoFileName: String,
    val submittedDate: Long,
    val title: String,
    val votes: Int,
    val hasVoted: Boolean
)

// MARK: - 滤镜分类

/**
 * 滤镜分类（与 iOS FilterCategory 对齐）
 *
 * @param displayName 中文展示名
 * @param symbolName Material 图标键（由 UI 层映射为 ImageVector）
 */
enum class FilterCategory(val displayName: String, val symbolName: String) {
    PORTRAIT("人像", "person"),
    FILM("胶片", "movie"),
    VINTAGE("复古", "history"),
    NATURE("自然", "eco"),
    FOOD("美食", "restaurant"),
    BW("黑白", "contrast"),
    CREATIVE("创意", "palette"),
    JAPANESE("日系", "temple_buddhist"),
    HK_STYLE("港风", "location_city"),
    LANDSCAPE("风光", "landscape");

    companion object {
        fun fromName(name: String): FilterCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CREATIVE
    }
}

// MARK: - 滤镜参数

/**
 * 滤镜参数（与 iOS FilterParameters 对齐，保留社区滤镜用到的核心字段）
 *
 * 参数说明：
 * - temperature: 色温偏移（开尔文），正数=暖色，负数=冷色
 * - tint: 色调偏移，正数=偏绿，负数=偏洋红
 * - exposure: 曝光补偿（EV）
 * - brightness: 亮度（-1 到 1）
 * - contrast: 对比度（默认 1.0）
 * - saturation: 饱和度（默认 1.0）
 * - vibrance: 自然饱和度（-1 到 1）
 * - highlightAmount: 高光调整（0.3 到 1.0，默认 1.0）
 * - shadowAmount: 阴影调整（-1 到 1，默认 0）
 * - isMonochrome: 是否黑白
 * - monochromeIntensity: 黑白强度
 * - monochromeColorR/G/B: 黑白色调 RGB（0-1）
 */
data class FilterParameters(
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val vibrance: Float = 0f,
    val highlightAmount: Float = 1.0f,
    val shadowAmount: Float = 0f,
    val isMonochrome: Boolean = false,
    val monochromeIntensity: Float = 0f,
    val monochromeColorR: Float = 1.0f,
    val monochromeColorG: Float = 1.0f,
    val monochromeColorB: Float = 1.0f
) {
    companion object {
        /** 默认参数（无滤镜效果） */
        val neutral = FilterParameters()
    }
}

// MARK: - 用户滤镜

/**
 * 用户滤镜（与 iOS UserFilter 对齐）
 *
 * @param id 唯一标识
 * @param name 滤镜名称
 * @param creatorName 创作者名
 * @param parameters 滤镜参数
 * @param previewImageName 预览图名（可选）
 * @param createdDate 创建时间（epoch 毫秒）
 * @param downloads 下载量
 * @param category 滤镜分类
 * @param filterDescription 滤镜描述
 */
data class UserFilter(
    val id: String,
    val name: String,
    val creatorName: String,
    val parameters: FilterParameters,
    val previewImageName: String? = null,
    val createdDate: Long,
    val downloads: Int,
    val category: FilterCategory,
    val filterDescription: String
)

// MARK: - 社区帖子

/**
 * 社区帖子（任务要求新增的 data class，用于社区交流互动）
 *
 * @param id 唯一标识
 * @param title 帖子标题
 * @param authorName 作者名
 * @param content 帖子正文
 * @param createdDate 创建时间（epoch 毫秒）
 * @param likes 点赞数
 * @param hasLiked 当前用户是否已点赞
 * @param challengeId 关联的挑战 ID（可选）
 */
data class CommunityPost(
    val id: String,
    val title: String,
    val authorName: String,
    val content: String,
    val createdDate: Long,
    val likes: Int,
    val hasLiked: Boolean,
    val challengeId: String? = null
)
