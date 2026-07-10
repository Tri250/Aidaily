package com.livecompose.livecapture.features.share

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Send
// 注意：图标统一通过上面的具名导入使用；Image 图标与 foundation.Image 同名，
// 此处显式导入图标，下方 composable 调用改用全限定名 androidx.compose.foundation.Image 以避免冲突。
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.sharecard.ShareCardGenerator
import com.livecompose.livecapture.core.sharecard.ShareCardMetadata
import com.livecompose.livecapture.core.sharecard.ShareCardStyle
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.elevatedShadow
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 分享卡片界面 ViewModel
 *
 * 使用 [MutableStateFlow] / [StateFlow] 管理状态：
 * - 当前选中的卡片样式
 * - 已生成的卡片 Bitmap（样式变化时重新生成）
 * - 生成中标志
 * - 各样式的预览缩略图
 */
class ShareCardViewModel : ViewModel() {

    private val _selectedStyle = MutableStateFlow(ShareCardStyle.Minimal)
    val selectedStyle: StateFlow<ShareCardStyle> = _selectedStyle.asStateFlow()

    private val _cardImage = MutableStateFlow<Bitmap?>(null)
    val cardImage: StateFlow<Bitmap?> = _cardImage.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _previews = MutableStateFlow<Map<ShareCardStyle, Bitmap>>(emptyMap())
    val previews: StateFlow<Map<ShareCardStyle, Bitmap>> = _previews.asStateFlow()

    /** 切换卡片样式 */
    fun selectStyle(style: ShareCardStyle) {
        _selectedStyle.value = style
    }

    /** 生成完整尺寸卡片（样式变化时调用） */
    fun generateCard(photo: Bitmap, metadata: ShareCardMetadata) {
        if (_isGenerating.value) return
        _isGenerating.value = true
        _cardImage.value = null
        viewModelScope.launch {
            val card = ShareCardGenerator.generateCard(photo, _selectedStyle.value, metadata)
            _cardImage.value = card
            _isGenerating.value = false
        }
    }

    /** 生成样式选择器所需的全部预览缩略图 */
    fun generatePreviews(photo: Bitmap) {
        viewModelScope.launch {
            val result = mutableMapOf<ShareCardStyle, Bitmap>()
            for (style in ShareCardStyle.all) {
                result[style] = ShareCardGenerator.generatePreview(photo, style)
            }
            _previews.value = result
        }
    }
}

/**
 * 分享卡片预览与分享界面（对标 iOS ShareCardView）。
 *
 * 包含：卡片预览、样式选择器（横向滚动缩略图）、平台分享按钮
 * （微信好友 / 朋友圈 / 微博 / 小红书 / 保存图片 / 更多）。
 *
 * @param photo 原始照片
 * @param record 照片记录（用于渲染日期与 EXIF 参数），可为空
 * @param onClose 关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCardScreen(
    photo: Bitmap,
    record: PhotoRecord? = null,
    onClose: () -> Unit,
    viewModel: ShareCardViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val cardImage by viewModel.cardImage.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val previews by viewModel.previews.collectAsState()

    val shareManager = remember(context) { ShareManager(context) }
    val metadata = remember(record) { buildMetadata(record) }
    var showXhsAlert by remember { mutableStateOf(false) }

    // 样式变化（含首次进入）时重新生成卡片
    LaunchedEffect(selectedStyle) {
        viewModel.generateCard(photo, metadata)
    }
    // 进入时生成样式预览缩略图
    LaunchedEffect(photo) {
        viewModel.generatePreviews(photo)
    }

    // 分享卡片生成后反馈
    LaunchedEffect(cardImage, isGenerating) {
        if (cardImage != null && !isGenerating) {
            Toast.makeText(context, "分享卡片已生成", Toast.LENGTH_SHORT).show()
        }
    }

    fun showSnack(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    /** 当前用于分享的图片：卡片已生成则用卡片，否则用原图 */
    fun currentShareImage(): Bitmap = cardImage ?: photo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分享") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DesignSystem.Colors.backgroundPrimary(),
                    titleContentColor = DesignSystem.Colors.textPrimary(),
                    navigationIconContentColor = DesignSystem.Colors.textSecondary(),
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DesignSystem.Colors.backgroundPrimary(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            DragIndicator()

            CardPreview(
                cardImage = cardImage,
                isGenerating = isGenerating,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            )

            StylePickerSection(
                selectedStyle = selectedStyle,
                previews = previews,
                onSelect = viewModel::selectStyle,
            )

            Spacer(modifier = Modifier.height(20.dp))

            PlatformButtons(
                onWeChatSession = {
                    if (!shareManager.isWeChatInstalled()) {
                        showSnack("未安装微信")
                    } else {
                        shareManager.shareToWeChatWithSdk(
                            currentShareImage(),
                            SendMessageToWX.Req.WXSceneSession,
                        )
                        Toast.makeText(context, "已分享到微信", Toast.LENGTH_SHORT).show()
                        showSnack("已分享到微信好友")
                    }
                },
                onWeChatTimeline = {
                    if (!shareManager.isWeChatInstalled()) {
                        showSnack("未安装微信")
                    } else {
                        shareManager.shareToWeChatWithSdk(
                            currentShareImage(),
                            SendMessageToWX.Req.WXSceneTimeline,
                        )
                        Toast.makeText(context, "已分享到微信", Toast.LENGTH_SHORT).show()
                        showSnack("已分享到朋友圈")
                    }
                },
                onWeibo = {
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) { bitmapToShareUri(context, currentShareImage()) }
                        if (uri == null) {
                            showSnack("生成分享图片失败")
                            return@launch
                        }
                        if (!shareManager.isWeiboInstalled()) {
                            shareManager.shareImage(uri)
                            showSnack("未安装微博，已打开系统分享")
                        } else {
                            shareManager.shareToWeibo(uri)
                            showSnack("已分享到微博")
                        }
                    }
                },
                onXiaohongshu = {
                    if (!isXiaohongshuInstalled(context)) {
                        showXhsAlert = true
                    } else {
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) { bitmapToShareUri(context, currentShareImage()) }
                            if (uri == null) {
                                showSnack("生成分享图片失败")
                                return@launch
                            }
                            shareManager.shareImage(uri)
                            showSnack("已打开分享，请选择小红书")
                        }
                    }
                },
                onSave = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            saveToGallery(context, currentShareImage())
                        }
                        showSnack(if (ok) "已保存到相册" else "保存失败，请检查权限")
                    }
                },
                onMore = {
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) { bitmapToShareUri(context, currentShareImage()) }
                        if (uri == null) {
                            showSnack("生成分享图片失败")
                            return@launch
                        }
                        shareManager.shareImage(uri)
                    }
                },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showXhsAlert) {
        AlertDialog(
            onDismissRequest = { showXhsAlert = false },
            confirmButton = {
                TextButton(onClick = { showXhsAlert = false }) { Text("好的") }
            },
            title = { Text("提示") },
            text = { Text("未安装小红书 App，请先安装后再试。") },
        )
    }
}

// MARK: - 卡片预览

@Composable
private fun DragIndicator() {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .width(36.dp)
            .height(5.dp)
            .clip(DesignSystem.smallRoundedShape)
            .background(DesignSystem.Colors.gray3()),
    )
}

@Composable
private fun CardPreview(
    cardImage: Bitmap?,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(DesignSystem.largeRoundedShape)
            .background(DesignSystem.Colors.backgroundSecondary())
            .elevatedShadow(),
    ) {
        if (cardImage != null) {
            androidx.compose.foundation.Image(
                bitmap = cardImage.asImageBitmap(),
                contentDescription = "分享卡片",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatioCard(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatioCard(),
                contentAlignment = Alignment.Center,
            ) {
                if (isGenerating) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "正在生成分享卡片...",
                            style = DesignSystem.Typography.caption1,
                            color = DesignSystem.Colors.textTertiary(),
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = DesignSystem.Colors.textTertiary(),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

/** 卡片统一 3:4 宽高比 */
private fun Modifier.aspectRatioCard(): Modifier =
    this.aspectRatio(3f / 4f)

// MARK: - 样式选择器

@Composable
private fun StylePickerSection(
    selectedStyle: ShareCardStyle,
    previews: Map<ShareCardStyle, Bitmap>,
    onSelect: (ShareCardStyle) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            "选择卡片风格",
            style = DesignSystem.Typography.headline,
            color = DesignSystem.Colors.textPrimary(),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ShareCardStyle.all) { style ->
                StyleButton(
                    style = style,
                    isSelected = selectedStyle.id == style.id,
                    preview = previews[style],
                    onClick = { onSelect(style) },
                )
            }
        }
    }
}

@Composable
private fun StyleButton(
    style: ShareCardStyle,
    isSelected: Boolean,
    preview: Bitmap?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val borderColor = if (isSelected) DesignSystem.Colors.primary else DesignSystem.Colors.gray3()
        val borderWidth = if (isSelected) 2.5.dp else 1.dp
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 96.dp)
                .clip(DesignSystem.smallRoundedShape)
                .background(DesignSystem.Colors.backgroundSecondary())
                .border(width = borderWidth, color = borderColor, shape = DesignSystem.smallRoundedShape),
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) {
                androidx.compose.foundation.Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = style.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = DesignSystem.Colors.textTertiary(),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            style.displayName,
            style = DesignSystem.Typography.caption1,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) DesignSystem.Colors.primary else DesignSystem.Colors.textSecondary(),
            maxLines = 1,
        )
    }
}

// MARK: - 平台分享按钮

@Composable
private fun PlatformButtons(
    onWeChatSession: () -> Unit,
    onWeChatTimeline: () -> Unit,
    onWeibo: () -> Unit,
    onXiaohongshu: () -> Unit,
    onSave: () -> Unit,
    onMore: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 第一行：微信好友 / 朋友圈 / 微博 / 小红书
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ShareButton(Icons.Default.Chat, WeChatGreen, "微信好友", onWeChatSession)
            ShareButton(Icons.Default.Send, WeChatGreen, "朋友圈", onWeChatTimeline)
            ShareButton(Icons.Default.Public, WeiboRed, "微博", onWeibo)
            ShareButton(Icons.Default.Favorite, XiaohongshuRed, "小红书", onXiaohongshu)
        }
        // 第二行：保存图片 / 更多
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ShareButton(Icons.Default.SaveAlt, DesignSystem.Colors.primary, "保存图片", onSave)
            ShareButton(Icons.Default.MoreHoriz, DesignSystem.Colors.gray4(), "更多", onMore)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowScope.ShareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            style = DesignSystem.Typography.caption2,
            color = DesignSystem.Colors.textSecondary(),
            maxLines = 1,
        )
    }
}

// MARK: - 颜色常量（与 iOS 品牌色对齐）

private val WeChatGreen = Color(0xFF2EBD4F)
private val WeiboRed = Color(0xFFE63838)
private val XiaohongshuRed = Color(0xFFF03838)

// MARK: - 分享辅助函数

/** 由 [PhotoRecord] 构建卡片元数据（含日期与 EXIF 参数） */
private fun buildMetadata(record: PhotoRecord?): ShareCardMetadata {
    if (record == null) return ShareCardMetadata()
    return ShareCardMetadata(
        title = "构妙 · LiveCompose",
        subtitle = null,
        date = record.creationDate,
        detectionMethod = record.detectionMethod,
        iso = record.iso,
        shutterSpeed = record.shutterSpeed,
        aperture = record.aperture,
        imageWidth = record.imageWidth,
        imageHeight = record.imageHeight,
    )
}

/** 将卡片 Bitmap 写入缓存文件并返回可分享的 FileProvider URI */
private fun bitmapToShareUri(context: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "share_card_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        AppLogger.e("ShareCard", "生成分享 URI 失败", e)
        null
    }
}

/** 保存卡片到系统相册 (DCIM/LiveCapture)，返回是否成功 */
private fun saveToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    val filename = "LiveCapture_${System.currentTimeMillis()}.jpg"
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/LiveCapture")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
    return try {
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    } catch (e: Exception) {
        AppLogger.e("ShareCard", "保存到相册失败", e)
        false
    }
}

/** 检查小红书是否安装 */
private fun isXiaohongshuInstalled(context: android.content.Context): Boolean = try {
    context.packageManager.getPackageInfo("com.xingin.xhs", 0)
    true
} catch (e: Exception) {
    false
}
