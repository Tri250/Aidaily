package com.livecompose.livecapture.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 图库界面 - 液态玻璃风格 2026 高端摄影体验
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: HomeViewModel = viewModel(),
    onPhotoClick: ((String) -> Unit)? = null
) {
    val records by viewModel.records.collectAsState()
    var selectedPhotoIndex by remember { mutableIntStateOf(-1) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    var filterRating by remember { mutableIntStateOf(0) }
    var filterFlaggedOnly by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var emptyStateVisible by remember { mutableStateOf(false) }

    LaunchedEffect(records.isEmpty()) {
        if (records.isEmpty()) {
            emptyStateVisible = true
        } else {
            emptyStateVisible = false
        }
    }

    val filteredRecords = remember(records, filterRating, filterFlaggedOnly) {
        records.filter { record ->
            val ratingMatch = filterRating == 0 || record.rating == filterRating
            val flagMatch = !filterFlaggedOnly || record.flag
            ratingMatch && flagMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.backgroundPrimary())
    ) {
        // 顶部栏 - 国潮质感渐变
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DesignSystem.Colors.gradientStart.copy(alpha = 0.08f),
                            DesignSystem.Colors.gradientEnd.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "图库",
                style = DesignSystem.Typography.largeTitle,
                color = DesignSystem.Colors.textPrimary()
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelectionMode) {
                IconButton(onClick = {
                    viewModel.deleteRecords(selectedIds.toList())
                    selectedIds = emptySet()
                    isSelectionMode = false
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = DesignSystem.Colors.error)
                }
                TextButton(onClick = {
                    isSelectionMode = false
                    selectedIds = emptySet()
                }) {
                    Text("取消", color = DesignSystem.Colors.textPrimary())
                }
            } else if (records.isNotEmpty()) {
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "筛选",
                            tint = if (filterRating > 0 || filterFlaggedOnly) DesignSystem.Colors.accentWarm
                            else DesignSystem.Colors.textSecondary()
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部照片") },
                            onClick = {
                                filterRating = 0
                                filterFlaggedOnly = false
                                showFilterMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("仅收藏") },
                            onClick = {
                                filterFlaggedOnly = !filterFlaggedOnly
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (filterFlaggedOnly) Icons.Default.Star else Icons.Default.StarOutline,
                                    null
                                )
                            }
                        )
                        HorizontalDivider()
                        Text(
                            "按评分筛选",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = DesignSystem.Colors.textTertiary()
                        )
                        (1..5).forEach { stars ->
                            DropdownMenuItem(
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        repeat(stars) {
                                            Icon(
                                                Icons.Default.Star, null,
                                                tint = DesignSystem.Colors.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    filterRating = if (filterRating == stars) 0 else stars
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (filterRating == stars) {
                                        Icon(Icons.Default.Check, null, tint = DesignSystem.Colors.accentWarm)
                                    }
                                }
                            )
                        }
                    }
                }
                Text(
                    "${records.size} 张",
                    style = DesignSystem.Typography.caption1,
                    color = DesignSystem.Colors.textTertiary()
                )
            }
        }

        if (records.isEmpty()) {
            // 空状态 - 液态玻璃卡片 + 入场动画
            AnimatedVisibility(
                visible = emptyStateVisible,
                enter = fadeIn(animationSpec = DesignSystem.Animation.entryFadeIn) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(300)
                        )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(32.dp)
                            .liquidGlass(cornerRadius = 24.dp, intensity = 0.08f)
                            .padding(40.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = DesignSystem.Colors.textTertiary()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "暂无照片",
                            style = DesignSystem.Typography.title2,
                            color = DesignSystem.Colors.textSecondary(),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "使用下方拍摄按钮开始创作",
                            style = DesignSystem.Typography.subheadline,
                            color = DesignSystem.Colors.textTertiary()
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
            ) {
                itemsIndexed(filteredRecords, key = { _, it -> it.id }) { index, record ->
                    StaggeredEntryItem(index = index) {
                        PhotoCard(
                            record = record,
                            viewModel = viewModel,
                            isSelected = record.id in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (record.id in selectedIds) {
                                        val new = selectedIds - record.id
                                        if (new.isEmpty()) isSelectionMode = false
                                        new
                                    } else selectedIds + record.id
                                } else {
                                    if (onPhotoClick != null) {
                                        onPhotoClick(record.id)
                                    } else {
                                        selectedPhotoIndex = records.indexOf(record)
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedIds = setOf(record.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 照片详情
    if (selectedPhotoIndex >= 0 && selectedPhotoIndex < records.size) {
        val record = records[selectedPhotoIndex]
        PhotoDetailDialog(
            record = record,
            viewModel = viewModel,
            onDismiss = { selectedPhotoIndex = -1 },
            onDelete = {
                viewModel.deleteRecord(record.id)
                selectedPhotoIndex = -1
            },
            onRatingChange = { rating -> viewModel.updateRating(record.id, rating) },
            onToggleFlag = { viewModel.toggleFlag(record.id) }
        )
    }
}

@Composable
private fun PhotoCard(
    record: PhotoRecord,
    viewModel: HomeViewModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var thumbnail by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(record.id) {
        thumbnail = viewModel.getThumbnail(record.id)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = DesignSystem.Animation.quick,
        label = "photoCardScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        if (thumbnail != null) {
            val thumb = thumbnail!!
            Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Shimmer 占位 - 使用 DesignSystem 颜色
            val gray2 = DesignSystem.Colors.gray2()
            val gray3 = DesignSystem.Colors.gray3()
            val shimmerColors = remember {
                listOf(gray2, gray3, gray2)
            }
            val transition = rememberInfiniteTransition()
            val translateAnim = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            val brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset.Zero,
                end = Offset(x = translateAnim.value, y = translateAnim.value)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = DesignSystem.Colors.textTertiary()
                )
            }
        }

        // 评分指示器
        if (record.rating > 0 && !isSelectionMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(
                        DesignSystem.Colors.minimalDarkOverlay,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(record.rating) {
                    Icon(
                        Icons.Default.Star, null,
                        tint = DesignSystem.Colors.accent,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // 收藏标记
        if (record.flag && !isSelectionMode) {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                tint = DesignSystem.Colors.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DesignSystem.Colors.minimalDarkOverlay)
            )
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (isSelected) DesignSystem.Colors.accentWarm
                else DesignSystem.Colors.minimalSecondaryLabel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
            )
        }
    }
}

@Composable
private fun PhotoDetailDialog(
    record: PhotoRecord,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onToggleFlag: () -> Unit
) {
    var photo by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(record.id) {
        photo = viewModel.getFullPhoto(record.id)
    }

    var currentRating by remember { mutableIntStateOf(record.rating) }
    var isFlagged by remember { mutableStateOf(record.flag) }

    val dateFormat = remember { SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DesignSystem.Colors.backgroundPrimary(),
        titleContentColor = DesignSystem.Colors.textPrimary(),
        textContentColor = DesignSystem.Colors.textSecondary(),
        icon = {
            Icon(Icons.Default.Info, null, tint = DesignSystem.Colors.primary)
        },
        title = {
            Text(dateFormat.format(Date(record.creationDate)))
        },
        text = {
            Column {
                if (photo != null) {
                    val p = photo!!
                    Image(
                        bitmap = p.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(DesignSystem.Colors.gray2()),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = DesignSystem.Colors.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 星标评分
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = {
                                currentRating = if (currentRating == star) 0 else star
                                onRatingChange(currentRating)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (star <= currentRating) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "$star 星",
                                tint = if (star <= currentRating) DesignSystem.Colors.accent
                                else DesignSystem.Colors.textTertiary(),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 收藏标记按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        isFlagged = !isFlagged
                        onToggleFlag()
                    }) {
                        Icon(
                            if (isFlagged) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isFlagged) DesignSystem.Colors.primary
                            else DesignSystem.Colors.textTertiary(),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isFlagged) "已收藏" else "收藏",
                            color = if (isFlagged) DesignSystem.Colors.primary
                            else DesignSystem.Colors.textSecondary()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // EXIF 信息
                record.iso?.let { ExifRow("ISO", "${it.toInt()}") }
                record.shutterSpeed?.let { ExifRow("快门", "1/${(1.0 / it).toInt()}s") }
                record.aperture?.let { ExifRow("光圈", "f/%.1f".format(it)) }
                record.imageWidth?.let { w ->
                    record.imageHeight?.let { h -> ExifRow("分辨率", "${w}×${h}") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("删除", color = DesignSystem.Colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = DesignSystem.Colors.textSecondary())
            }
        }
    )
}

@Composable
private fun ExifRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = DesignSystem.Typography.caption1,
            color = DesignSystem.Colors.textTertiary()
        )
        Text(
            value,
            style = DesignSystem.Typography.monoCaption,
            color = DesignSystem.Colors.textSecondary()
        )
    }
}

/**
 * 网格项交错入场动画 - 国潮质感动效
 * 从 0.9 缩放 + 渐显进入，每项间隔 30ms
 */
@Composable
private fun StaggeredEntryItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = DesignSystem.Animation.entryScaleIn,
        label = "staggeredScale_$index"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = DesignSystem.Animation.entryScaleIn,
        label = "staggeredAlpha_$index"
    )

    LaunchedEffect(Unit) {
        delay(index * 30L)
        visible = true
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .graphicsLayer(alpha = alpha)
    ) {
        content()
    }
}