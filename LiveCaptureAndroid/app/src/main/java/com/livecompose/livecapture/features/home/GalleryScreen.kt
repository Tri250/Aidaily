package com.livecompose.livecapture.features.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.ui.design.DesignSystem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 图库界面
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

    // 筛选状态
    var filterRating by remember { mutableIntStateOf(0) }
    var filterFlaggedOnly by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // 应用筛选
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red)
                }
                TextButton(onClick = {
                    isSelectionMode = false
                    selectedIds = emptySet()
                }) {
                    Text("取消", color = DesignSystem.Colors.textPrimary())
                }
            } else if (records.isNotEmpty()) {
                // 筛选按钮
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "筛选",
                            tint = if (filterRating > 0 || filterFlaggedOnly) DesignSystem.Colors.primary
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
                        Divider()
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
                                                Icons.Default.Star,
                                                null,
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
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = DesignSystem.Colors.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                Text(
                    "${records.size} 张照片",
                    style = DesignSystem.Typography.caption1,
                    color = DesignSystem.Colors.textTertiary()
                )
            }
        }

        if (records.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(60.dp))
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = DesignSystem.Colors.textTertiary()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("暂无照片", style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textSecondary())
                    Text("使用下方拍摄按钮开始创作", style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
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

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
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
            // Shimmer placeholder 效果
            val shimmerColors = remember {
                listOf(
                    Color(0xFF1F1F1F),
                    Color(0xFF292929),
                    Color(0xFF1F1F1F)
                )
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
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
            }
        }

        // 评分指示器
        if (record.rating > 0 && !isSelectionMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(DesignSystem.Colors.minimalDarkOverlay, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(record.rating) {
                    Icon(
                        Icons.Default.Star,
                        null,
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
                tint = if (isSelected) DesignSystem.Colors.primary else DesignSystem.Colors.minimalSecondaryLabel,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val dateFormat = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
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
                    // Shimmer placeholder for detail photo
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
                                tint = if (star <= currentRating) DesignSystem.Colors.accent else Color.Gray,
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
                            tint = if (isFlagged) DesignSystem.Colors.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isFlagged) "已收藏" else "收藏",
                            color = if (isFlagged) DesignSystem.Colors.primary else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                record.detectionMethod?.let { Text("检测方法: $it") }
                record.iso?.let { Text("ISO: ${it.toInt()}") }
                record.shutterSpeed?.let { Text("快门: 1/${(1.0 / it).toInt()}s") }
                record.aperture?.let { Text("光圈: f/%.1f".format(it)) }
                record.imageWidth?.let { w ->
                    record.imageHeight?.let { h -> Text("分辨率: ${w}×${h}") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("删除", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}