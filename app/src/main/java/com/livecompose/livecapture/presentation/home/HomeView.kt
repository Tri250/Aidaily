package com.livecompose.livecapture.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.livecompose.livecapture.R
import com.livecompose.livecapture.core.design.Primary
import com.livecompose.livecapture.core.design.TitleTextStyle
import com.livecompose.livecapture.core.design.*
import com.livecompose.livecapture.core.detection.CompositionResult
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.presentation.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController? = null,
    onSettingsClick: () -> Unit = {}
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    var selectedRecord by remember { mutableStateOf<PhotoRecord?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 日期分组逻辑放在 Composable 内部处理
    val groupedRecords = remember(records) {
        groupRecordsByDate(records)
    }

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
            .padding(Spacing.ExtraLarge)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.gallery_title),
                style = TitleTextStyle
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.capture_settings)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
                    Text(
                        text = stringResource(R.string.gallery_empty_title),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        fontSize = FontSize.TitleMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    Text(
                        text = stringResource(R.string.gallery_empty_desc),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        fontSize = FontSize.BodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.Huge))
                    Button(
                        onClick = { navController?.navigate(Screen.Capture.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(stringResource(R.string.gallery_go_capture), color = Color.White)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedRecords.forEach { (header, groupItems) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = Spacing.Small, top = Spacing.Large, bottom = Spacing.Small)
                        )
                    }
                    items(groupItems) { record ->
                        PhotoThumbnail(
                            record = record,
                            onClick = { selectedRecord = record }
                        )
                    }
                }
            }
        }
    }

    // Photo detail bottom sheet
    if (selectedRecord != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedRecord = null },
            sheetState = sheetState
        ) {
            PhotoDetailBottomSheet(
                record = selectedRecord!!,
                onDelete = {
                    viewModel.deleteRecord(selectedRecord!!)
                    selectedRecord = null
                }
            )
        }
    }
}

private fun groupRecordsByDate(records: List<PhotoRecord>): List<Pair<String, List<PhotoRecord>>> {
    if (records.isEmpty()) return emptyList()

    val now = Calendar.getInstance()
    val todayStart = now.clone() as Calendar
    todayStart.set(Calendar.HOUR_OF_DAY, 0)
    todayStart.set(Calendar.MINUTE, 0)
    todayStart.set(Calendar.SECOND, 0)
    todayStart.set(Calendar.MILLISECOND, 0)

    val yesterdayStart = todayStart.clone() as Calendar
    yesterdayStart.add(Calendar.DAY_OF_YEAR, -1)

    val weekStart = todayStart.clone() as Calendar
    val dayOfWeek = todayStart.get(Calendar.DAY_OF_WEEK)
    val daysBack = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    weekStart.add(Calendar.DAY_OF_YEAR, -daysBack)

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayNameFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    val map = LinkedHashMap<String, MutableList<PhotoRecord>>()

    records.sortedByDescending { it.timestamp }.forEach { record ->
        val recordCal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
        val header = when {
            recordCal.timeInMillis >= todayStart.timeInMillis -> "今天"
            recordCal.timeInMillis >= yesterdayStart.timeInMillis -> "昨天"
            recordCal.timeInMillis >= weekStart.timeInMillis -> dayNameFormat.format(recordCal.time)
            else -> dateFormat.format(recordCal.time)
        }
        map.getOrPut(header) { mutableListOf() }.add(record)
    }

    return map.toList()
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
            .clip(RoundedCornerShape(CornerRadius.Medium))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = thumbModel,
            contentDescription = stringResource(R.string.content_desc_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PhotoDetailBottomSheet(
    record: PhotoRecord,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.gallery_share_chooser)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ExtraLarge)
            .padding(bottom = Spacing.Massive)
    ) {
        AsyncImage(
            model = mainImageModel,
            contentDescription = stringResource(R.string.content_desc_photo_detail),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(CornerRadius.ExtraLarge))
        )

        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        // 场景类型彩色标签
        val sceneType = record.inferredSceneType()
        val sceneColor = sceneTypeColor(sceneType)
        Surface(
            color = sceneColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(CornerRadius.Medium),
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = sceneType.label,
                color = sceneColor,
                modifier = Modifier.padding(horizontal = Spacing.Large, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        // 美学评分进度条可视化（0-1 范围，绿色渐变）
        record.aestheticScore?.let { score ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.gallery_detail_score),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format("%.2f", score),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.Medium)
                    .clip(RoundedCornerShape(CornerRadius.Small))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(score.coerceIn(0f, 1f))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF81C784), Color(0xFF2E7D32))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
        }

        // EXIF 参数用图标+数值卡片横向排列
        ExifInfoCards(record = record)

        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        Text(
            text = "${dateFormat.format(Date(record.timestamp))} · ${record.width} x ${record.height}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        // 分享和删除按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            OutlinedButton(
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(record.filePath))
                        type = "image/jpeg"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.gallery_share))
            }
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.gallery_delete))
            }
        }
    }
}

private data class ExifItem(val icon: ImageVector, val label: String, val value: String)

@Composable
private fun ExifInfoCards(record: PhotoRecord) {
    val items = buildList {
        record.iso?.let { add(ExifItem(Icons.Default.Exposure, stringResource(R.string.gallery_detail_iso), it)) }
        record.shutterSpeed?.let { add(ExifItem(Icons.Default.Timer, stringResource(R.string.gallery_detail_shutter), it)) }
        record.aperture?.let { add(ExifItem(Icons.Default.Camera, stringResource(R.string.gallery_detail_aperture), it)) }
        record.focalLength?.let { add(ExifItem(Icons.Default.Straighten, stringResource(R.string.gallery_detail_focal), it)) }
    }

    if (items.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            items.forEach { item ->
                ExifCard(item = item, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExifCard(item: ExifItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.Large))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun PhotoRecord.inferredSceneType(): CompositionResult.SceneType {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 20..23 || hour in 0..5 -> CompositionResult.SceneType.NIGHT_SCENE
        width > height -> CompositionResult.SceneType.LANDSCAPE_NATURE
        else -> CompositionResult.SceneType.PORTRAIT_STANDING
    }
}

private fun sceneTypeColor(sceneType: CompositionResult.SceneType): Color = when (sceneType) {
    CompositionResult.SceneType.PORTRAIT_STANDING,
    CompositionResult.SceneType.PORTRAIT_SITTING -> Color(0xFFFF8A80)
    CompositionResult.SceneType.LANDSCAPE_SUNSET,
    CompositionResult.SceneType.LANDSCAPE_NATURE -> Color(0xFF69F0AE)
    CompositionResult.SceneType.NIGHT_SCENE -> Color(0xFFB388FF)
    CompositionResult.SceneType.FOOD_STYLING -> Color(0xFFFFD54F)
    CompositionResult.SceneType.PRODUCT_WHITE -> Color(0xFFE0E0E0)
    CompositionResult.SceneType.CITY_URBAN -> Color(0xFF40C4FF)
    CompositionResult.SceneType.GENERAL -> Color(0xFFB0BEC5)
}
