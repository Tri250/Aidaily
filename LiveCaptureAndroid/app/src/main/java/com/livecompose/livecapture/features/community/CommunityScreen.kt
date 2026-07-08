package com.livecompose.livecapture.features.community

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.livecompose.livecapture.core.community.*
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// MARK: - 导航路由

/**
 * 社区内部导航路由（自管理，避免修改全局 AppNavigation）
 */
sealed class CommunityRoute {
    data object List : CommunityRoute()
    data class ChallengeDetail(val challengeId: String) : CommunityRoute()
    data class FilterDetail(val filterId: String) : CommunityRoute()
    data class LocationDetail(val locationId: String) : CommunityRoute()
}

// MARK: - 社区主界面

/**
 * 社区主界面（与 iOS CommunityView 对齐：挑战 / 滤镜 / 地点 三大板块）
 *
 * @param communityManager 社区管理器（挑战 + 滤镜），由 DI 注入
 * @param locationRecommender 拍照地点推荐器，由 DI 注入
 */
@Composable
fun CommunityScreen(
    communityManager: CommunityManager,
    locationRecommender: LocationRecommender,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CommunityTab.CHALLENGES) }
    val routeStack = remember { mutableStateListOf<CommunityRoute>(CommunityRoute.List) }

    val currentRoute = routeStack.last()

    fun navigateTo(route: CommunityRoute) {
        routeStack.add(route)
    }

    fun navigateBack() {
        if (routeStack.size > 1) {
            routeStack.removeAt(routeStack.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CommunityTopBar(
                title = currentRouteTitle(currentRoute, selectedTab),
                showBack = currentRoute !is CommunityRoute.List,
                onBack = ::navigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 仅在列表路由显示分段选择器
            if (currentRoute is CommunityRoute.List) {
                CommunitySegmentedControl(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180)))
                },
                label = "community_route"
            ) { route ->
                when (route) {
                    CommunityRoute.List -> when (selectedTab) {
                        CommunityTab.CHALLENGES -> ChallengeTab(
                            manager = communityManager,
                            onChallengeClick = { id -> navigateTo(CommunityRoute.ChallengeDetail(id)) },
                            onBrowseFilters = { selectedTab = CommunityTab.FILTERS },
                            onExploreLocations = { selectedTab = CommunityTab.LOCATIONS }
                        )
                        CommunityTab.FILTERS -> FilterCommunityTab(
                            manager = communityManager,
                            onFilterClick = { id -> navigateTo(CommunityRoute.FilterDetail(id)) }
                        )
                        CommunityTab.LOCATIONS -> LocationTab(
                            recommender = locationRecommender,
                            onLocationClick = { id -> navigateTo(CommunityRoute.LocationDetail(id)) }
                        )
                    }
                    is CommunityRoute.ChallengeDetail -> ChallengeDetailScreen(
                        challengeId = route.challengeId,
                        manager = communityManager
                    )
                    is CommunityRoute.FilterDetail -> FilterDetailScreen(
                        filterId = route.filterId,
                        manager = communityManager
                    )
                    is CommunityRoute.LocationDetail -> LocationDetailScreen(
                        locationId = route.locationId,
                        recommender = locationRecommender
                    )
                }
            }
        }
    }
}

private enum class CommunityTab(val displayName: String) {
    CHALLENGES("挑战"),
    FILTERS("滤镜"),
    LOCATIONS("地点")
}

private fun currentRouteTitle(route: CommunityRoute, tab: CommunityTab): String = when (route) {
    CommunityRoute.List -> "社区"
    is CommunityRoute.ChallengeDetail -> "挑战详情"
    is CommunityRoute.FilterDetail -> "滤镜详情"
    is CommunityRoute.LocationDetail -> "拍摄点详情"
}

// MARK: - 顶栏

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityTopBar(title: String, showBack: Boolean, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary()) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = DesignSystem.Colors.textPrimary())
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DesignSystem.Colors.backgroundPrimary()
        )
    )
}

// MARK: - 分段选择器

@Composable
private fun CommunitySegmentedControl(selectedTab: CommunityTab, onTabSelected: (CommunityTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Spacing.medium, vertical = DesignSystem.Spacing.xxSmall),
        horizontalArrangement = Arrangement.Center
    ) {
        CommunityTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Text(
                text = tab.displayName,
                style = DesignSystem.Typography.subheadline.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (isSelected) DesignSystem.Colors.primary else DesignSystem.Colors.textTertiary(),
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.circle))
                    .background(if (isSelected) DesignSystem.Colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = DesignSystem.Spacing.large, vertical = DesignSystem.Spacing.xxSmall)
            )
        }
    }
}

// MARK: - 挑战 Tab

@Composable
private fun ChallengeTab(
    manager: CommunityManager,
    onChallengeClick: (String) -> Unit,
    onBrowseFilters: () -> Unit,
    onExploreLocations: () -> Unit
) {
    val current by manager.currentChallenge.collectAsState()
    val upcoming by manager.upcomingChallenges.collectAsState()
    val past by manager.pastChallenges.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DesignSystem.Spacing.medium)
    ) {
        // 当前挑战
        if (current != null) {
            SectionHeader(title = "本周挑战", icon = Icons.Filled.EmojiEvents)
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            CurrentChallengeCard(challenge = current!!, onClick = { onChallengeClick(current!!.id) })
            Spacer(Modifier.height(DesignSystem.Spacing.medium))
        }

        // 即将到来
        if (upcoming.isNotEmpty()) {
            SectionHeader(title = "即将到来", icon = Icons.Filled.Event)
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            upcoming.take(3).forEach { challenge ->
                ChallengeRow(challenge = challenge, onClick = { onChallengeClick(challenge.id) })
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            }
            Spacer(Modifier.height(DesignSystem.Spacing.medium))
        }

        // 往期挑战
        if (past.isNotEmpty()) {
            SectionHeader(title = "往期挑战", icon = Icons.Filled.History)
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            past.take(5).forEach { challenge ->
                ChallengeRow(challenge = challenge, onClick = { onChallengeClick(challenge.id) })
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            }
        }

        // 空状态
        if (current == null && upcoming.isEmpty()) {
            ChallengeEmptyState(
                onBrowseFilters = onBrowseFilters,
                onExploreLocations = onExploreLocations
            )
        }
        Spacer(Modifier.height(DesignSystem.Spacing.xLarge))
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = DesignSystem.Spacing.xxSmall)) {
        Icon(icon, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
        Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
    }
}

@Composable
private fun CurrentChallengeCard(challenge: PhotoChallenge, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
            .background(DesignSystem.Colors.backgroundSecondary())
            .border(1.dp, DesignSystem.Colors.accent.copy(alpha = 0.5f), RoundedCornerShape(DesignSystem.CornerRadius.large))
            .clickable(onClick = onClick)
            .padding(DesignSystem.Spacing.medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(communityIcon(challenge.theme.iconName), contentDescription = null, tint = DesignSystem.Colors.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
            Text(challenge.title, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary(), modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        Text(challenge.description, style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textSecondary(), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "${formatDate(challenge.startDate)} - ${formatDate(challenge.endDate)}",
                style = DesignSystem.Typography.caption1,
                color = DesignSystem.Colors.textTertiary()
            )
            Spacer(Modifier.weight(1f))
            Text("${challenge.userEntries.size} 作品", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.accent)
        }
    }
}

@Composable
private fun ChallengeRow(challenge: PhotoChallenge, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
            .background(DesignSystem.Colors.backgroundSecondary())
            .clickable(onClick = onClick)
            .padding(DesignSystem.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(communityIcon(challenge.theme.iconName), contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(DesignSystem.Spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(challenge.title, style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(challenge.description, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
        Surface(shape = CircleShape, color = DesignSystem.Colors.backgroundTertiary()) {
            Text(
                "${challenge.userEntries.size} 作品",
                style = DesignSystem.Typography.caption2,
                color = DesignSystem.Colors.textTertiary(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ChallengeEmptyState(onBrowseFilters: () -> Unit, onExploreLocations: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = DesignSystem.Spacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(DesignSystem.Spacing.medium))
        Text("暂无挑战", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textSecondary())
        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        Text("新挑战即将到来，敬请期待", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
        Spacer(Modifier.height(DesignSystem.Spacing.large))
        TextButton(onClick = onBrowseFilters) {
            Icon(Icons.Filled.PhotoFilter, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("浏览滤镜", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.primary)
        }
        TextButton(onClick = onExploreLocations) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = DesignSystem.Colors.accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("探索地点", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.accent)
        }
    }
}

// MARK: - 挑战详情

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeDetailScreen(challengeId: String, manager: CommunityManager) {
    val scope = rememberCoroutineScope()
    // 从 manager 状态中查找挑战（当前/即将/往期）
    val current by manager.currentChallenge.collectAsState()
    val upcoming by manager.upcomingChallenges.collectAsState()
    val past by manager.pastChallenges.collectAsState()
    val challenge = remember(current, upcoming, past, challengeId) {
        sequence { yield(current); yieldAll(upcoming); yieldAll(past) }
            .filterNotNull()
            .firstOrNull { it.id == challengeId }
    }

    var showSubmitSheet by remember { mutableStateOf(false) }
    var entryTitle by remember { mutableStateOf("") }

    if (challenge == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("挑战不存在", style = DesignSystem.Typography.body, color = DesignSystem.Colors.textTertiary())
        }
        return
    }

    val leaderboard = remember(challenge) { manager.getLeaderboard(challenge.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DesignSystem.Spacing.medium)
    ) {
        Spacer(Modifier.height(DesignSystem.Spacing.small))
        // 头部信息卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                .background(DesignSystem.Colors.backgroundSecondary())
                .padding(DesignSystem.Spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(communityIcon(challenge.theme.iconName), contentDescription = null, tint = DesignSystem.Colors.accent, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(DesignSystem.Spacing.small))
                Column {
                    Text(challenge.title, style = DesignSystem.Typography.title2, color = DesignSystem.Colors.textPrimary())
                    Text(challenge.theme.displayName, style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textSecondary())
                }
            }
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            Text(challenge.description, style = DesignSystem.Typography.body, color = DesignSystem.Colors.textSecondary())
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${formatDate(challenge.startDate)} - ${formatDate(challenge.endDate)}",
                    style = DesignSystem.Typography.footnote,
                    color = DesignSystem.Colors.textTertiary()
                )
            }
        }

        Spacer(Modifier.height(DesignSystem.Spacing.medium))

        // 提交按钮（仅当前进行中的挑战）
        if (challenge.isActive) {
            Button(
                onClick = { showSubmitSheet = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                colors = ButtonDefaults.buttonColors(containerColor = DesignSystem.Colors.primary)
            ) {
                Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
                Text("提交作品", style = DesignSystem.Typography.headline, color = Color.White)
            }
            Spacer(Modifier.height(DesignSystem.Spacing.medium))
        }

        // 排行榜
        SectionHeader(title = "排行榜", icon = Icons.Filled.BarChart)
        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        if (leaderboard.isEmpty()) {
            EmptyEntriesView()
        } else {
            leaderboard.forEachIndexed { index, entry ->
                LeaderboardRow(
                    rank = index + 1,
                    entry = entry,
                    onVote = {
                        scope.launch { manager.voteForEntry(challenge.id, entry.id) }
                    }
                )
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            }
        }
        Spacer(Modifier.height(DesignSystem.Spacing.xLarge))
    }

    // 提交作品弹窗
    if (showSubmitSheet) {
        ModalBottomSheet(onDismissRequest = { showSubmitSheet = false }) {
            Column(
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.medium).padding(bottom = DesignSystem.Spacing.xLarge)
            ) {
                Text("作品标题", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                OutlinedTextField(
                    value = entryTitle,
                    onValueChange = { entryTitle = it },
                    placeholder = { Text("给你的作品起个名字...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(DesignSystem.Spacing.large))
                Button(
                    onClick = {
                        if (entryTitle.isNotBlank()) {
                            scope.launch {
                                manager.submitEntry(
                                    challengeId = challenge.id,
                                    photoFileName = "photo_${System.currentTimeMillis()}.jpg",
                                    title = entryTitle.trim()
                                )
                            }
                            entryTitle = ""
                            showSubmitSheet = false
                        }
                    },
                    enabled = entryTitle.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entryTitle.isBlank()) Color.Gray else DesignSystem.Colors.primary
                    )
                ) {
                    Text("提交", style = DesignSystem.Typography.headline, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: ChallengeEntry, onVote: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
            .background(DesignSystem.Colors.backgroundSecondary())
            .padding(DesignSystem.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#$rank",
            style = DesignSystem.Typography.headline,
            color = if (rank <= 3) DesignSystem.Colors.accent else DesignSystem.Colors.textSecondary(),
            modifier = Modifier.width(36.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatDate(entry.submittedDate), style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.textTertiary())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("${entry.votes}", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textSecondary())
        }
        if (!entry.hasVoted) {
            Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
            IconButton(onClick = onVote, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.ThumbUp, contentDescription = "投票", tint = DesignSystem.Colors.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyEntriesView() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = DesignSystem.Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(DesignSystem.Spacing.medium))
        Text("暂无作品", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textSecondary())
        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        Text("成为第一个提交作品的人吧！", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
    }
}

// MARK: - 滤镜社区 Tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterCommunityTab(manager: CommunityManager, onFilterClick: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val communityFilters by manager.communityFilters.collectAsState()
    val popularFilters by manager.popularFilters.collectAsState()
    val downloadedFilters by manager.downloadedFilters.collectAsState()
    val myCreatedFilters by manager.myCreatedFilters.collectAsState()
    var selectedCategory by remember { mutableStateOf<FilterCategory?>(null) }
    var showMyFilters by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部：我的滤镜入口
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesignSystem.Spacing.medium, vertical = DesignSystem.Spacing.xxSmall),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showMyFilters = true }) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("我的滤镜", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.primary)
            }
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = DesignSystem.Spacing.xLarge)) {
            // 分类切换
            LazyRow(
                contentPadding = PaddingValues(horizontal = DesignSystem.Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxSmall)
            ) {
                item {
                    FilterCategoryChip(title = "全部", isSelected = selectedCategory == null) { selectedCategory = null }
                }
                items(FilterCategory.entries) { category ->
                    FilterCategoryChip(title = category.displayName, isSelected = selectedCategory == category) { selectedCategory = category }
                }
            }

            // 热门滤镜横滑
            if (selectedCategory == null && popularFilters.isNotEmpty()) {
                SectionHeader(title = "热门滤镜", icon = Icons.Filled.LocalFireDepartment)
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = DesignSystem.Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.small)
                ) {
                    items(popularFilters) { filter ->
                        PopularFilterCard(filter = filter, onClick = { onFilterClick(filter.id) })
                    }
                }
                Spacer(Modifier.height(DesignSystem.Spacing.medium))
            }

            // 滤镜列表
            val filters = selectedCategory?.let { manager.getFiltersForCategory(it) } ?: communityFilters
            SectionHeader(
                title = selectedCategory?.displayName ?: "全部滤镜",
                icon = Icons.Filled.PhotoFilter
            )
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            Column(modifier = Modifier.padding(horizontal = DesignSystem.Spacing.medium)) {
                filters.forEach { filter ->
                    FilterRow(filter = filter, isDownloaded = manager.isDownloadedById(filter.id), onClick = { onFilterClick(filter.id) })
                    Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                }
            }
        }
    }

    // 我的滤镜弹窗
    if (showMyFilters) {
        ModalBottomSheet(onDismissRequest = { showMyFilters = false }) {
            MyFiltersContent(
                downloadedFilters = downloadedFilters,
                myCreatedFilters = myCreatedFilters,
                onRemoveDownloaded = { filter ->
                    scope.launch { manager.removeFilter(filter) }
                }
            )
        }
    }
}

@Composable
private fun FilterCategoryChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = title,
        style = DesignSystem.Typography.footnote.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
        color = if (isSelected) Color.White else DesignSystem.Colors.textSecondary(),
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) DesignSystem.Colors.primary else DesignSystem.Colors.backgroundSecondary())
            .clickable(onClick = onClick)
            .padding(horizontal = DesignSystem.Spacing.small, vertical = DesignSystem.Spacing.xxSmall)
    )
}

@Composable
private fun PopularFilterCard(filter: UserFilter, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 80.dp)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DesignSystem.Colors.primary.copy(alpha = 0.3f),
                            DesignSystem.Colors.secondary.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(communityIcon(filter.category.symbolName), contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(DesignSystem.Spacing.xxxSmall))
        Text(filter.name, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${filter.downloads} 下载", style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.textTertiary())
    }
}

@Composable
private fun FilterRow(filter: UserFilter, isDownloaded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
            .background(DesignSystem.Colors.backgroundSecondary())
            .clickable(onClick = onClick)
            .padding(DesignSystem.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DesignSystem.Colors.primary.copy(alpha = 0.2f),
                            DesignSystem.Colors.secondary.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(communityIcon(filter.category.symbolName), contentDescription = null, tint = DesignSystem.Colors.primary.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(DesignSystem.Spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(filter.name, style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(filter.creatorName, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
        if (isDownloaded) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DesignSystem.Colors.success, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Download, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text("${filter.downloads}", style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.textTertiary())
        }
    }
}

@Composable
private fun MyFiltersContent(
    downloadedFilters: List<UserFilter>,
    myCreatedFilters: List<UserFilter>,
    onRemoveDownloaded: (UserFilter) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = DesignSystem.Spacing.medium).padding(bottom = DesignSystem.Spacing.xLarge)) {
        Text("我的滤镜", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
        Spacer(Modifier.height(DesignSystem.Spacing.medium))
        if (downloadedFilters.isEmpty() && myCreatedFilters.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignSystem.Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.PhotoFilter, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(DesignSystem.Spacing.medium))
                Text("还没有滤镜", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textSecondary())
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                Text("浏览社区滤镜并下载你喜欢的", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
            }
        } else {
            if (downloadedFilters.isNotEmpty()) {
                Text("已下载", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                downloadedFilters.forEach { filter ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = DesignSystem.Spacing.xxxSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(communityIcon(filter.category.symbolName), contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(DesignSystem.Spacing.small))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(filter.name, style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textPrimary())
                            Text(filter.creatorName, style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.textTertiary())
                        }
                        IconButton(onClick = { onRemoveDownloaded(filter) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "移除", tint = DesignSystem.Colors.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            if (myCreatedFilters.isNotEmpty()) {
                Spacer(Modifier.height(DesignSystem.Spacing.medium))
                Text("我创建的", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                myCreatedFilters.forEach { filter ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = DesignSystem.Spacing.xxxSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(communityIcon(filter.category.symbolName), contentDescription = null, tint = DesignSystem.Colors.accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(DesignSystem.Spacing.small))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(filter.name, style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textPrimary())
                            Text(filter.filterDescription, style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.textTertiary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 滤镜详情

@Composable
private fun FilterDetailScreen(filterId: String, manager: CommunityManager) {
    val scope = rememberCoroutineScope()
    val communityFilters by manager.communityFilters.collectAsState()
    val downloadedFilters by manager.downloadedFilters.collectAsState()
    val allFilters = communityFilters + downloadedFilters
    val filter = remember(allFilters, filterId) { allFilters.firstOrNull { it.id == filterId } }

    if (filter == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("滤镜不存在", style = DesignSystem.Typography.body, color = DesignSystem.Colors.textTertiary())
        }
        return
    }

    val isDownloaded = downloadedFilters.any { it.id == filter.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DesignSystem.Spacing.medium)
    ) {
        Spacer(Modifier.height(DesignSystem.Spacing.small))
        // 预览区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DesignSystem.Colors.primary.copy(alpha = 0.4f),
                            DesignSystem.Colors.secondary.copy(alpha = 0.4f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(communityIcon(filter.category.symbolName), contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                Text(filter.name, style = DesignSystem.Typography.title2, color = Color.White)
            }
        }

        Spacer(Modifier.height(DesignSystem.Spacing.large))

        // 滤镜信息
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(filter.name, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
                Text("by ${filter.creatorName}", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textSecondary())
            }
            Surface(shape = CircleShape, color = DesignSystem.Colors.primary.copy(alpha = 0.15f)) {
                Text(
                    filter.category.displayName,
                    style = DesignSystem.Typography.caption1,
                    color = DesignSystem.Colors.primary,
                    modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        Text(filter.filterDescription, style = DesignSystem.Typography.body, color = DesignSystem.Colors.textSecondary())

        Spacer(Modifier.height(DesignSystem.Spacing.small))
        // 参数详情
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
                .background(DesignSystem.Colors.backgroundSecondary())
                .padding(DesignSystem.Spacing.medium)
        ) {
            FilterParameterRow("色温", "%.0fK".format(filter.parameters.temperature + 6500))
            FilterParameterRow("色调", "%.0f".format(filter.parameters.tint))
            FilterParameterRow("曝光", "%.2f EV".format(filter.parameters.exposure))
            FilterParameterRow("对比度", "%.2f".format(filter.parameters.contrast))
            FilterParameterRow("饱和度", "%.2f".format(filter.parameters.saturation))
            if (filter.parameters.isMonochrome) {
                FilterParameterRow("黑白", "开启")
            }
        }

        Spacer(Modifier.height(DesignSystem.Spacing.large))

        // 下载/移除按钮
        if (isDownloaded) {
            OutlinedButton(
                onClick = { scope.launch { manager.removeFilter(filter) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignSystem.Colors.error)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
                Text("移除下载", style = DesignSystem.Typography.headline)
            }
        } else {
            Button(
                onClick = { scope.launch { manager.downloadFilter(filter) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                colors = ButtonDefaults.buttonColors(containerColor = DesignSystem.Colors.primary)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
                Text("下载滤镜 (${filter.downloads})", style = DesignSystem.Typography.headline, color = Color.White)
            }
        }
        Spacer(Modifier.height(DesignSystem.Spacing.xLarge))
    }
}

@Composable
private fun FilterParameterRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        Text(value, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textPrimary())
    }
}

// MARK: - 地点 Tab

@Composable
private fun LocationTab(recommender: LocationRecommender, onLocationClick: (String) -> Unit) {
    val context = LocalContext.current
    val nearbyLocations by recommender.nearbyLocations.collectAsState()
    val currentLocation by recommender.currentLocation.collectAsState()
    val isSearching by recommender.isSearching.collectAsState()
    var selectedTag by remember { mutableStateOf<String?>(null) }
    val allTags = remember { recommender.getAllTags() }

    // 定位权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchLastLocation(context, recommender)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 定位状态栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignSystem.Spacing.medium, vertical = DesignSystem.Spacing.xxSmall)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
                .background(DesignSystem.Colors.backgroundSecondary())
                .padding(horizontal = DesignSystem.Spacing.small, vertical = DesignSystem.Spacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val locationGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val statusIcon = if (locationGranted) Icons.Filled.MyLocation else Icons.Filled.LocationOff
            val statusColor = if (locationGranted) DesignSystem.Colors.success else DesignSystem.Colors.error
            val statusText = when {
                currentLocation != null -> "已定位，显示附近拍摄点"
                locationGranted -> "正在获取位置..."
                else -> "定位权限未开启，显示全部拍摄点"
            }
            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
            Text(statusText, style = DesignSystem.Typography.footnote, color = DesignSystem.Colors.textSecondary(), modifier = Modifier.weight(1f))
            if (currentLocation != null) {
                IconButton(onClick = { fetchLastLocation(context, recommender) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.GpsFixed, contentDescription = "刷新定位", tint = DesignSystem.Colors.primary, modifier = Modifier.size(16.dp))
                }
            } else {
                TextButton(onClick = {
                    if (locationGranted) {
                        fetchLastLocation(context, recommender)
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }) {
                    Text("定位", style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.primary)
                }
            }
        }

        // 标签横滑
        LazyRow(
            contentPadding = PaddingValues(horizontal = DesignSystem.Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxSmall)
        ) {
            item {
                FilterCategoryChip(title = "全部", isSelected = selectedTag == null) { selectedTag = null }
            }
            items(allTags) { tag ->
                FilterCategoryChip(title = tag, isSelected = selectedTag == tag) { selectedTag = tag }
            }
        }

        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))

        // 地点列表
        val locations = when {
            isSearching -> nearbyLocations
            selectedTag != null -> recommender.getLocationsByTag(selectedTag!!)
            nearbyLocations.isNotEmpty() -> nearbyLocations
            else -> recommender.allLocations
        }

        if (locations.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignSystem.Spacing.xxLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(DesignSystem.Spacing.medium))
                Text("暂无拍摄点", style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textSecondary())
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                Text("该标签下暂无拍摄点推荐", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = DesignSystem.Spacing.xLarge)) {
                locations.forEach { location ->
                    LocationRow(
                        location = location,
                        distance = recommender.distanceToCurrent(location),
                        onClick = { onLocationClick(location.id) }
                    )
                    Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                }
            }
        }
    }
}

@Composable
private fun LocationRow(location: PhotoLocation, distance: Double?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Spacing.medium)
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
            .background(DesignSystem.Colors.backgroundSecondary())
            .clickable(onClick = onClick)
            .padding(DesignSystem.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
                .background(DesignSystem.Colors.backgroundTertiary()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = difficultyColor(location.difficulty), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(DesignSystem.Spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(location.name, style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textPrimary(), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Surface(shape = CircleShape, color = difficultyColor(location.difficulty).copy(alpha = 0.15f)) {
                    Text(
                        location.difficulty.displayName,
                        style = DesignSystem.Typography.caption2,
                        color = difficultyColor(location.difficulty),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(location.description, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(2.dp))
                Text(location.bestTime, style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.textTertiary(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (distance != null) {
                    Text(formatDistance(distance), style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.primary)
                }
            }
        }
    }
}

// MARK: - 地点详情

@Composable
private fun LocationDetailScreen(locationId: String, recommender: LocationRecommender) {
    val currentLocation by recommender.currentLocation.collectAsState()
    val location = remember(locationId) { recommender.allLocations.firstOrNull { it.id == locationId } }

    if (location == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("拍摄点不存在", style = DesignSystem.Typography.body, color = DesignSystem.Colors.textTertiary())
        }
        return
    }

    val distance = currentLocation?.let { recommender.distanceToCurrent(location) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DesignSystem.Spacing.medium)
    ) {
        Spacer(Modifier.height(DesignSystem.Spacing.small))
        // 预览区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DesignSystem.Colors.primary.copy(alpha = 0.3f),
                            DesignSystem.Colors.secondary.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
                Text(location.name, style = DesignSystem.Typography.title2, color = Color.White)
            }
        }

        Spacer(Modifier.height(DesignSystem.Spacing.large))

        // 基本信息
        Text(location.name, style = DesignSystem.Typography.title2, color = DesignSystem.Colors.textPrimary())
        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        Text(location.description, style = DesignSystem.Typography.body, color = DesignSystem.Colors.textSecondary())

        Spacer(Modifier.height(DesignSystem.Spacing.small))
        // 坐标
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "%.4f, %.4f".format(location.coordinate.latitude, location.coordinate.longitude),
                style = DesignSystem.Typography.caption1,
                color = DesignSystem.Colors.textTertiary()
            )
        }

        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        // 最佳拍摄时间
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = DesignSystem.Colors.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(location.bestTime, style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textSecondary())
        }

        Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
        // 难度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("拍摄难度: ", style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textSecondary())
            Text(location.difficulty.displayName, style = DesignSystem.Typography.callout.copy(fontWeight = FontWeight.SemiBold), color = difficultyColor(location.difficulty))
        }

        // 距离
        if (distance != null) {
            Spacer(Modifier.height(DesignSystem.Spacing.xxSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Straighten, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(formatDistanceFull(distance), style = DesignSystem.Typography.callout, color = DesignSystem.Colors.textSecondary())
            }
        }

        Spacer(Modifier.height(DesignSystem.Spacing.small))
        // 标签
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxxSmall)
        ) {
            items(location.tags) { tag ->
                Surface(shape = CircleShape, color = DesignSystem.Colors.primary.copy(alpha = 0.15f)) {
                    Text(
                        tag,
                        style = DesignSystem.Typography.caption2,
                        color = DesignSystem.Colors.primary,
                        modifier = Modifier.padding(horizontal = DesignSystem.Spacing.xxxSmall, vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(DesignSystem.Spacing.xLarge))
    }
}

// MARK: - 工具函数

/**
 * 图标键映射为 Material ImageVector（与模型中 iconName/symbolName 对应）
 */
private fun communityIcon(key: String): ImageVector = when (key) {
    "person" -> Icons.Filled.Person
    "landscape" -> Icons.Filled.Landscape
    "restaurant" -> Icons.Filled.Restaurant
    "location_city" -> Icons.Filled.LocationCity
    "nightlight" -> Icons.Filled.Nightlight
    "eco" -> Icons.Filled.Eco
    "account_balance" -> Icons.Filled.AccountBalance
    "bug_report" -> Icons.Filled.BugReport
    "contrast" -> Icons.Filled.Contrast
    "radio_button_unchecked" -> Icons.Filled.RadioButtonUnchecked
    "waves" -> Icons.Filled.Waves
    "center_focus_strong" -> Icons.Filled.CenterFocusStrong
    "pets" -> Icons.Filled.Pets
    "palette" -> Icons.Filled.Palette
    "light_mode" -> Icons.Filled.LightMode
    "apartment" -> Icons.Filled.Apartment
    "history" -> Icons.Filled.History
    "water_drop" -> Icons.Filled.WaterDrop
    "cloud" -> Icons.Filled.Cloud
    "movie" -> Icons.Filled.Movie
    "temple_buddhist" -> Icons.Filled.TempleBuddhist
    else -> Icons.Filled.PhotoCamera
}

/**
 * 难度对应颜色
 */
private fun difficultyColor(difficulty: PhotoDifficulty): Color = when (difficulty) {
    PhotoDifficulty.EASY -> DesignSystem.Colors.success
    PhotoDifficulty.MEDIUM -> DesignSystem.Colors.accent
    PhotoDifficulty.HARD -> DesignSystem.Colors.error
}

/**
 * 格式化距离（短格式，用于列表行）
 */
private fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters < 1000) {
        "%.0fm".format(distanceMeters)
    } else {
        "%.1fkm".format(distanceMeters / 1000)
    }
}

/**
 * 格式化距离（完整格式，用于详情页）
 */
private fun formatDistanceFull(distanceMeters: Double): String {
    return if (distanceMeters < 1000) {
        "距离: %.0f 米".format(distanceMeters)
    } else {
        "距离: %.1f 公里".format(distanceMeters / 1000)
    }
}

/**
 * 格式化日期（epoch 毫秒 -> yyyy/MM/dd）
 */
private fun formatDate(epochMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}

/**
 * 通过系统 LocationManager 获取最近一次定位，注入到推荐器
 */
private fun fetchLastLocation(context: Context, recommender: LocationRecommender) {
    try {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        // 优先使用 NETWORK 提供者（室内可用），其次 GPS
        val provider = locationManager.allProviders.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return
        val location = locationManager.getLastKnownLocation(provider)
        if (location != null) {
            recommender.setCurrentLocation(GeoCoordinate(location.latitude, location.longitude))
        } else {
            // 无最近定位，触发一次附近搜索（展示全部按名称排序）
            recommender.setCurrentLocation(null)
        }
    } catch (e: SecurityException) {
        // 权限被回收时安全降级
        recommender.setCurrentLocation(null)
    } catch (e: Exception) {
        recommender.setCurrentLocation(null)
    }
}
