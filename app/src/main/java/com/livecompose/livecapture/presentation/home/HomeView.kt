package com.livecompose.livecapture.presentation.home

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.livecompose.livecapture.R
import com.livecompose.livecapture.core.accessibility.AccessibilityHelper
import com.livecompose.livecapture.core.design.TitleTextStyle
import com.livecompose.livecapture.core.storage.PhotoRecord
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeView(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedRecord by remember { mutableStateOf<PhotoRecord?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // #69: 每次页面可见时刷新图库（从拍摄页返回后自动刷新）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadRecords()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.tab_home),
            style = TitleTextStyle,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .semantics { heading() }
        )

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.home_empty_title),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.semantics { contentDescription = context.getString(R.string.a11y_photo_grid) }
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
        val context = LocalContext.current
        PhotoDetailDialog(
            record = record,
            onDismiss = { selectedRecord = null },
            onDelete = {
                viewModel.deleteRecord(record)
                selectedRecord = null
            },
            onShare = {
                viewModel.sharePhoto(record, context)
            },
            onShareToWechat = {
                viewModel.shareToWechat(record, context)
            },
            isWechatInstalled = viewModel.isWechatInstalled(context),
            onEdit = {
                val photoPath = viewModel.getPhotoForEditing(record)
                val encoded = URLEncoder.encode(photoPath, "UTF-8")
                navController?.navigate("photo_editor/$encoded")
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
    val context = LocalContext.current

    // 缩略图始终用本地文件路径
    val thumbModel = remember(record.thumbPath) {
        ImageRequest.Builder(context)
            .data(File(record.thumbPath))
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = AccessibilityHelper.contentDescriptionForPhoto(record)
                role = Role.Button
            }
    ) {
        AsyncImage(
            model = thumbModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PhotoDetailDialog(
    record: PhotoRecord,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onShareToWechat: () -> Unit,
    isWechatInstalled: Boolean,
    onEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // 主图加载: 支持 MediaStore Uri 和文件路径
    val mainImageModel = remember(record.filePath) {
        val data = if (record.filePath.startsWith("content://")) {
            Uri.parse(record.filePath)
        } else {
            File(record.filePath)
        }
        ImageRequest.Builder(context)
            .data(data)
            .crossfade(true)
            .build()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_photo_detail)) },
        text = {
            Column {
                AsyncImage(
                    model = mainImageModel,
                    contentDescription = stringResource(R.string.a11y_photo_detail_image),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                DetailItem(stringResource(R.string.home_detail_time), dateFormat.format(Date(record.timestamp)))
                DetailItem(stringResource(R.string.home_detail_size), stringResource(R.string.home_detail_size_format, record.width, record.height))
                record.iso?.let { DetailItem("ISO", it) }
                record.shutterSpeed?.let { DetailItem(stringResource(R.string.home_detail_shutter), it) }
                record.aperture?.let { DetailItem(stringResource(R.string.home_detail_aperture), it) }
                record.focalLength?.let { DetailItem(stringResource(R.string.home_detail_focal_length), it) }
                record.aestheticScore?.let { DetailItem(stringResource(R.string.home_detail_aesthetic_score), String.format("%.2f", it)) }
                record.cropRegion?.let { region ->
                    DetailItem(stringResource(R.string.home_detail_crop_region), stringResource(R.string.home_detail_crop_format, region.centerX, region.centerY))
                }
            }
        },
        confirmButton = {
            Row {
                // Edit button
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.home_edit),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.home_edit))
                }
                // Share button
                TextButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.share))
                }
                // WeChat share button (only if WeChat installed)
                if (isWechatInstalled) {
                    TextButton(onClick = onShareToWechat) {
                        Text(stringResource(R.string.share_to_wechat))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.home_close))
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.a11y_delete_photo)
                    role = Role.Button
                }
            ) {
                Text(stringResource(R.string.home_delete))
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
