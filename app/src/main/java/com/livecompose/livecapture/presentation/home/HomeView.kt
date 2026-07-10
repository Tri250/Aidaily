package com.livecompose.livecapture.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.livecompose.livecapture.core.design.TitleTextStyle
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.storage.PhotoStorageService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeView() {
    val context = LocalContext.current
    val storageService = remember { PhotoStorageService(context) }
    var records by remember { mutableStateOf(storageService.getAllRecords()) }
    var selectedRecord by remember { mutableStateOf<PhotoRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "图库",
            style = TitleTextStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无照片\n开始拍摄你的第一张照片吧",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(records) { record ->
                    PhotoThumbnail(
                        record = record,
                        onClick = { selectedRecord = record }
                    )
                }
            }
        }
    }

    // Photo detail dialog
    selectedRecord?.let { record ->
        PhotoDetailDialog(
            record = record,
            onDismiss = { selectedRecord = null },
            onDelete = {
                storageService.deleteRecord(record)
                records = storageService.getAllRecords()
                selectedRecord = null
            }
        )
    }
}

@Composable
private fun PhotoThumbnail(
    record: PhotoRecord,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(record.thumbPath))
                .crossfade(true)
                .build(),
            contentDescription = "Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PhotoDetailDialog(
    record: PhotoRecord,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("照片详情") },
        text = {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(record.filePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Photo detail",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                DetailItem("时间", dateFormat.format(Date(record.timestamp)))
                DetailItem("尺寸", "${record.width} x ${record.height}")
                record.iso?.let { DetailItem("ISO", it) }
                record.shutterSpeed?.let { DetailItem("快门", it) }
                record.aperture?.let { DetailItem("光圈", it) }
                record.focalLength?.let { DetailItem("焦距", it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium
        )
    }
}
