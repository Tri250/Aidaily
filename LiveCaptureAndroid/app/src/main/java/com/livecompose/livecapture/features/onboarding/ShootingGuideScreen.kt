package com.livecompose.livecapture.features.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PanHand
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.PanoramaHorizontal
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 拍摄技巧数据模型
 */
data class ShootingTip(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * 拍摄技巧分类
 */
data class ShootingCategory(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val tips: List<ShootingTip>
)

/**
 * 预设拍摄教程数据（与 iOS ShootingGuideData 对齐）
 */
object ShootingGuideData {
    val categories: List<ShootingCategory> = listOf(
        ShootingCategory(
            id = "composition",
            icon = Icons.Filled.GridOn,
            title = "构图技巧",
            tips = listOf(
                ShootingTip("rule_of_thirds", Icons.Filled.GridOn, "三分法",
                    "将画面横竖各分三等份，形成九宫格。把主体放在四条线的交叉点上，能让画面更加平衡和自然。这是最基础也最实用的构图法则，适用于几乎所有拍摄场景。"),
                ShootingTip("leading_lines", Icons.Filled.Timeline, "引导线",
                    "利用道路、栏杆、河流等自然线条，引导观众的视线进入画面，增强纵深感和层次感。引导线的终点最好是画面的主体，形成视觉焦点。"),
                ShootingTip("symmetry", Icons.Filled.Balance, "对称构图",
                    "利用建筑、水面倒影等对称元素，营造平衡、稳定、庄重的视觉效果。对称构图适合表现建筑的宏伟、水面的宁静等主题。"),
                ShootingTip("foreground_frame", Icons.Filled.CropSquare, "前景框架",
                    "利用门窗、树枝、拱门等作为前景框架，将主体框在其中，增加画面的层次感和深度，引导观众注意力聚焦到主体上。")
            )
        ),
        ShootingCategory(
            id = "lighting",
            icon = Icons.Filled.WbSunny,
            title = "光线运用",
            tips = listOf(
                ShootingTip("golden_hour", Icons.Filled.WbTwilight, "黄金时刻",
                    "日出后和日落前的一小时是摄影的黄金时刻。此时光线柔和温暖，色温偏暖，能拍出梦幻般的效果。尽量在这段时间拍摄人像和风光。"),
                ShootingTip("backlight", Icons.Filled.WbSunny, "逆光拍摄",
                    "让光源位于主体后方，可以拍出剪影效果或营造梦幻的光晕。逆光拍摄时注意使用点测光对准主体，适当增加曝光补偿。"),
                ShootingTip("side_light_portrait", Icons.Filled.Person, "侧光人像",
                    "侧光能在人物面部产生明暗对比，增强立体感和轮廓感。45度侧光是最经典的人像光线角度，可以很好地塑造面部特征。"),
                ShootingTip("soft_light", Icons.Filled.FilterDrama, "柔光技巧",
                    "阴天、树荫下或使用柔光设备，可以获得柔和的散射光。柔光适合拍摄人像，减少面部阴影，让皮肤看起来更加细腻光滑。")
            )
        ),
        ShootingCategory(
            id = "portrait",
            icon = Icons.Filled.Person,
            title = "人像摄影",
            tips = listOf(
                ShootingTip("angle_selection", Icons.Filled.Camera, "角度选择",
                    "俯拍显脸小，仰拍显腿长，平拍最自然。尝试不同角度找到最适合被摄者的角度。一般建议镜头略高于眼睛水平线，避免双下巴。"),
                ShootingTip("expression_guide", Icons.Filled.SentimentSatisfied, "表情引导",
                    "与模特保持沟通，用轻松的话题引导自然表情。抓拍比摆拍往往更自然。可以让模特看向别处、走动或做一些小动作，捕捉真实瞬间。"),
                ShootingTip("environmental_portrait", Icons.Filled.Apartment, "环境人像",
                    "将人物融入环境中，讲述一个故事。环境人像强调人物与场景的关系，背景元素应与人物形成呼应，而非单纯的背景虚化。"),
                ShootingTip("close_up", Icons.Filled.Visibility, "特写技巧",
                    "聚焦人物的眼睛，使用大光圈虚化背景。特写能够传达强烈的情感，注意眼部对焦要精准，稍微的失焦都会影响整体效果。")
            )
        ),
        ShootingCategory(
            id = "landscape",
            icon = Icons.Filled.Landscape,
            title = "风光摄影",
            tips = listOf(
                ShootingTip("wide_angle", Icons.Filled.Panorama, "广角运用",
                    "使用广角镜头可以拍出宏大的场景，增强透视感。广角适合拍摄壮丽的山川、建筑等，前景物体会显得更大，背景更远，营造空间感。"),
                ShootingTip("long_exposure", Icons.Filled.Waves, "长曝光",
                    "使用慢速快门（需三脚架），可以拍出丝绸般的流水、车轨等效果。拍摄水流时通常使用 1/4 秒到 2 秒的快门速度，配合 ND 滤镜使用。"),
                ShootingTip("hdr_scene", Icons.Filled.Layers, "HDR 场景",
                    "在明暗对比强烈的场景使用 HDR 模式，保留亮部和暗部细节。适合拍摄日出日落、逆光建筑等，避免高光过曝或暗部死黑。"),
                ShootingTip("panorama", Icons.Filled.PanoramaHorizontal, "全景拍摄",
                    "拍摄宽广场景时使用全景模式，保持手机平稳移动。全景拍摄时注意保持水平，避免画面中出现移动物体导致拼接错位。")
            )
        ),
        ShootingCategory(
            id = "mobile_photo",
            icon = Icons.Filled.Smartphone,
            title = "手机摄影",
            tips = listOf(
                ShootingTip("stability", Icons.Filled.PanHand, "稳定性",
                    "保持手机稳定是拍出清晰照片的关键。双手握持、肘部贴紧身体，或使用三脚架。拍摄时轻按快门，避免晃动。"),
                ShootingTip("clean_lens", Icons.Filled.AutoFixHigh, "清洁镜头",
                    "手机镜头容易被指纹和灰尘污染，拍照前用软布擦拭镜头。一个干净的镜头能让照片清晰度大幅提升，避免朦胧和光晕。"),
                ShootingTip("use_grid", Icons.Filled.GridOn, "使用网格",
                    "开启相机网格线，帮助构图和保持水平。网格线是三分法构图的视觉辅助，也能帮助你在拍摄建筑时保持垂直线条的平直。"),
                ShootingTip("avoid_digital_zoom", Icons.Filled.ZoomOut, "避免数码变焦",
                    "数码变焦会损失画质，尽量靠近拍摄对象或后期裁剪。如果需要放大，使用光学变焦或拍摄后裁剪，裁剪比数码变焦保留更多细节。")
            )
        )
    )
}

/**
 * 拍摄教程界面
 *
 * 可搜索、可折叠分类的拍摄技巧指南。对应 iOS 端 ShootingGuideView。
 *
 * @param onClose 关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShootingGuideScreen(onClose: () -> Unit) {
    var searchText by remember { mutableStateOf("") }
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }

    // 默认展开第一个分类
    LaunchedEffect(Unit) {
        if (expandedCategories.isEmpty()) {
            ShootingGuideData.categories.firstOrNull()?.let {
                expandedCategories = expandedCategories + it.id
            }
        }
    }

    val filteredCategories = remember(searchText) { filterCategories(searchText) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拍摄教程", color = DesignSystem.Colors.textPrimary()) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = DesignSystem.Colors.textPrimary())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DesignSystem.Colors.backgroundPrimary()
                )
            )
        },
        containerColor = DesignSystem.Colors.backgroundPrimary()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索框
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small, vertical = DesignSystem.Spacing.xxSmall),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DesignSystem.Colors.textTertiary()) },
                placeholder = { Text("搜索拍摄技巧", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary()) },
                singleLine = true,
                shape = RoundedCornerShape(DesignSystem.CornerRadius.large),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DesignSystem.Colors.backgroundSecondary(),
                    unfocusedContainerColor = DesignSystem.Colors.backgroundSecondary(),
                    focusedBorderColor = DesignSystem.Colors.primary,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            if (filteredCategories.isEmpty()) {
                EmptySearchView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = DesignSystem.Spacing.small,
                        vertical = DesignSystem.Spacing.xxSmall
                    ),
                    verticalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.medium)
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        CategorySection(
                            category = category,
                            isExpanded = category.id in expandedCategories,
                            onToggle = {
                                expandedCategories = if (category.id in expandedCategories) {
                                    expandedCategories - category.id
                                } else {
                                    expandedCategories + category.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/** 按搜索文本过滤分类与技巧 */
private fun filterCategories(searchText: String): List<ShootingCategory> {
    if (searchText.isBlank()) return ShootingGuideData.categories
    val query = searchText.trim()
    return ShootingGuideData.categories.mapNotNull { category ->
        val matching = category.tips.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }
        if (matching.isEmpty()) null
        else category.copy(tips = matching)
    }
}

@Composable
private fun CategorySection(
    category: ShootingCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        // 分类标题
        Surface(
            color = DesignSystem.Colors.backgroundSecondary(),
            shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
            onClick = onToggle
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignSystem.Spacing.xSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = DesignSystem.Colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(DesignSystem.Spacing.xSmall))
                Text(
                    text = category.title,
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.textPrimary()
                )
                Spacer(Modifier.width(DesignSystem.Spacing.xxSmall))
                Text(
                    text = "${category.tips.count()} 条",
                    style = DesignSystem.Typography.caption1,
                    color = DesignSystem.Colors.textTertiary()
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = DesignSystem.Colors.textTertiary(),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayerRotation(isExpanded)
                )
            }
        }

        // 展开的提示列表
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = DesignSystem.Spacing.xxSmall),
                verticalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxSmall)
            ) {
                category.tips.forEachIndexed { index, tip ->
                    TipRow(tip = tip, isLast = index == category.tips.lastIndex)
                }
            }
        }
    }
}

/** 旋转箭头指示展开/收起状态 */
private fun Modifier.graphicsLayerRotation(expanded: Boolean): Modifier =
    this.graphicsLayer { rotationZ = if (expanded) 180f else 0f }

@Composable
private fun TipRow(tip: ShootingTip, isLast: Boolean) {
    Surface(
        color = DesignSystem.Colors.backgroundSecondary().copy(alpha = 0.5f),
        shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(DesignSystem.Spacing.xSmall),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = DesignSystem.Colors.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(DesignSystem.CornerRadius.small)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tip.icon,
                        contentDescription = null,
                        tint = DesignSystem.Colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(DesignSystem.Spacing.xSmall))
                Column {
                    Text(
                        text = tip.title,
                        style = DesignSystem.Typography.headline,
                        color = DesignSystem.Colors.textPrimary()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tip.description,
                        style = DesignSystem.Typography.subheadline,
                        color = DesignSystem.Colors.textSecondary(),
                        lineHeight = 21.sp
                    )
                }
            }
            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 48.dp),
                    thickness = DesignSystem.Stroke.widthThin,
                    color = DesignSystem.Stroke.subtle()
                )
            }
        }
    }
}

@Composable
private fun EmptySearchView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.medium)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = DesignSystem.Colors.textTertiary(),
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "未找到相关技巧",
            style = DesignSystem.Typography.title3,
            color = DesignSystem.Colors.textSecondary()
        )
        Text(
            text = "尝试其他搜索关键词",
            style = DesignSystem.Typography.subheadline,
            color = DesignSystem.Colors.textTertiary()
        )
    }
}
