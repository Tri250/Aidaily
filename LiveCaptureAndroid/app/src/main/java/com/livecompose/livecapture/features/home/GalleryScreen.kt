package com.livecompose.livecapture.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.ui.design.DesignSystem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 图库界面
 * 对应 iOS 的 GalleryView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: HomeViewModel = viewModel()) {
    val records by viewModel.records.collectAsState()
    var selectedPhotoIndex by remember { mutableIntStateOf(-1) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

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
                itemsIndexed(records) { index, record ->
                    PhotoCard(
                        record = record,
                        thumbnail = viewModel.getThumbnail(record.id),
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
                                selectedPhotoIndex = index
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
            photo = viewModel.getFullPhoto(record.id),
            onDismiss = { selectedPhotoIndex = -1 },
            onDelete = {
                viewModel.deleteRecord(record.id)
                selectedPhotoIndex = -1
            }
        )
    }
}

@Composable
private fun PhotoCard(
    record: PhotoRecord,
    thumbnail: android.graphics.Bitmap?,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (isSelected) DesignSystem.Colors.primary else Color.White.copy(alpha = 0.7f),
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
    photo: android.graphics.Bitmap?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val dateFormat = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
            Text(dateFormat.format(Date(record.creationDate)))
        },
        text = {
            Column {
                if (photo != null) {
                    AsyncImage(
                        model = photo.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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