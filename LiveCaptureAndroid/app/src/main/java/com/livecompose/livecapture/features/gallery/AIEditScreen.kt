package com.livecompose.livecapture.features.gallery

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.core.editing.AIEditViewModel
import com.livecompose.livecapture.core.editing.ImageExpander
import com.livecompose.livecapture.core.editing.SkyReplacer
import com.livecompose.livecapture.core.editing.StyleTransfer
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIEditScreen(
    photoId: String,
    sourceBitmap: Bitmap?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { AppContainer.getInstance(context).aiEditViewModel }
    val scope = rememberCoroutineScope()

    val selectedTool by viewModel.selectedTool.collectAsState()
    val sourceImage by viewModel.sourceImage.collectAsState()
    val editedImage by viewModel.editedImage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedSkyType by viewModel.selectedSkyType.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val styleIntensity by viewModel.styleIntensity.collectAsState()
    val expandAmount by viewModel.expandAmount.collectAsState()
    val expandDirection by viewModel.expandDirection.collectAsState()

    LaunchedEffect(sourceBitmap) {
        viewModel.setSourceImage(sourceBitmap)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI 编辑",
                        style = DesignSystem.Typography.headline,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DesignSystem.Colors.minimalBackground
                )
            )
        },
        containerColor = DesignSystem.Colors.minimalBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // MARK: - 图片对比区域
            BeforeAfterImageSection(
                sourceImage = sourceImage,
                editedImage = editedImage,
                isProcessing = isProcessing,
                progress = progress
            )

            Spacer(modifier = Modifier.height(DesignSystem.Spacing.medium))

            // MARK: - 错误信息
            if (errorMessage != null) {
                ErrorMessageBanner(
                    message = errorMessage!!,
                    onDismiss = { viewModel.undo() }
                )
                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))
            }

            // MARK: - 工具选择器
            ToolSelectorChips(
                selectedTool = selectedTool,
                onToolSelected = { viewModel.selectTool(it) }
            )

            Spacer(modifier = Modifier.height(DesignSystem.Spacing.medium))

            // MARK: - 工具参数控制区
            ToolParameterSection(
                selectedTool = selectedTool,
                selectedSkyType = selectedSkyType,
                onSkyTypeSelected = { viewModel.setSkyType(it) },
                selectedStyle = selectedStyle,
                onStyleSelected = { viewModel.setStyle(it) },
                styleIntensity = styleIntensity,
                onStyleIntensityChanged = { viewModel.setStyleIntensity(it) },
                expandAmount = expandAmount,
                onExpandAmountChanged = { viewModel.setExpandAmount(it) },
                expandDirection = expandDirection,
                onExpandDirectionChanged = { viewModel.setExpandDirection(it) }
            )

            Spacer(modifier = Modifier.height(DesignSystem.Spacing.medium))

            // MARK: - 操作按钮
            ActionButtons(
                isProcessing = isProcessing,
                hasEditResult = editedImage != null,
                onApply = {
                    scope.launch {
                        viewModel.applyEdit()
                    }
                },
                onApplyAndContinue = {
                    viewModel.applyAndContinue()
                },
                onUndo = {
                    viewModel.undo()
                },
                onReset = {
                    viewModel.reset()
                }
            )

            Spacer(modifier = Modifier.height(DesignSystem.Spacing.large))
        }
    }
}

// MARK: - Before/After 图片对比

@Composable
private fun BeforeAfterImageSection(
    sourceImage: Bitmap?,
    editedImage: Bitmap?,
    isProcessing: Boolean,
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(horizontal = DesignSystem.Spacing.small)
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
            .background(DesignSystem.Colors.gray2()),
        contentAlignment = Alignment.Center
    ) {
        val displayBitmap = editedImage ?: sourceImage
        if (displayBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = if (editedImage != null) "编辑后" else "编辑前",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // 处理中叠加层
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = DesignSystem.Colors.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))
                        Text(
                            "处理中… ${(progress * 100).toInt()}%",
                            style = DesignSystem.Typography.subheadline,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxxSmall))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .width(160.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = DesignSystem.Colors.primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = DesignSystem.Colors.textTertiary(),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))
                Text(
                    "选择工具开始 AI 编辑",
                    style = DesignSystem.Typography.subheadline,
                    color = DesignSystem.Colors.textTertiary()
                )
            }
        }

        // Before/After 标签
        if (editedImage != null && !isProcessing) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(DesignSystem.Spacing.xSmall)
            ) {
                Text(
                    "BEFORE",
                    style = DesignSystem.Typography.caption2,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(DesignSystem.CornerRadius.micro)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxxSmall))
                Text(
                    "AFTER",
                    style = DesignSystem.Typography.caption2,
                    color = DesignSystem.Colors.primary,
                    modifier = Modifier
                        .background(
                            DesignSystem.Colors.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(DesignSystem.CornerRadius.micro)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// MARK: - 错误信息

@Composable
private fun ErrorMessageBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Spacing.small)
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
            .background(DesignSystem.Colors.errorBg)
            .clickable(onClick = onDismiss)
            .padding(DesignSystem.Spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            style = DesignSystem.Typography.footnote,
            color = DesignSystem.Colors.error,
            modifier = Modifier.weight(1f)
        )
    }
}

// MARK: - 工具选择 Chips

@Composable
private fun ToolSelectorChips(
    selectedTool: AIEditViewModel.AIEditTool,
    onToolSelected: (AIEditViewModel.AIEditTool) -> Unit
) {
    val tools = AIEditViewModel.AIEditTool.entries
    val icons = listOf(
        Icons.Default.AutoFixHigh,       // REMOVE
        Icons.Default.InvertColors,      // SKY_REPLACE
        Icons.Default.Expand,            // EXPAND
        Icons.Default.Palette            // STYLE_TRANSFER
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxxSmall)
    ) {
        tools.forEachIndexed { index, tool ->
            val isSelected = tool == selectedTool
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) DesignSystem.Colors.primary
                else Color.White.copy(alpha = 0.08f),
                animationSpec = tween(200),
                label = "chipBg_$index"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White
                else DesignSystem.Colors.minimalSecondaryLabel,
                animationSpec = tween(200),
                label = "chipText_$index"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onToolSelected(tool) }
                    )
                    .padding(vertical = DesignSystem.Spacing.xSmall),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icons[index],
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    tool.displayName,
                    style = DesignSystem.Typography.caption2,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// MARK: - 工具参数区域

@Composable
private fun ToolParameterSection(
    selectedTool: AIEditViewModel.AIEditTool,
    selectedSkyType: SkyReplacer.SkyType,
    onSkyTypeSelected: (SkyReplacer.SkyType) -> Unit,
    selectedStyle: StyleTransfer.ArtStyle,
    onStyleSelected: (StyleTransfer.ArtStyle) -> Unit,
    styleIntensity: Float,
    onStyleIntensityChanged: (Float) -> Unit,
    expandAmount: Int,
    onExpandAmountChanged: (Int) -> Unit,
    expandDirection: ImageExpander.ExpansionDirection,
    onExpandDirectionChanged: (ImageExpander.ExpansionDirection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Spacing.small)
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
            .background(DesignSystem.Colors.backgroundSecondary())
            .padding(DesignSystem.Spacing.small)
    ) {
        when (selectedTool) {
            AIEditViewModel.AIEditTool.REMOVE -> {
                RemoveToolSection()
            }
            AIEditViewModel.AIEditTool.SKY_REPLACE -> {
                SkyReplaceSection(
                    selectedSkyType = selectedSkyType,
                    onSkyTypeSelected = onSkyTypeSelected
                )
            }
            AIEditViewModel.AIEditTool.EXPAND -> {
                ExpandSection(
                    expandAmount = expandAmount,
                    onExpandAmountChanged = onExpandAmountChanged,
                    expandDirection = expandDirection,
                    onExpandDirectionChanged = onExpandDirectionChanged
                )
            }
            AIEditViewModel.AIEditTool.STYLE_TRANSFER -> {
                StyleTransferSection(
                    selectedStyle = selectedStyle,
                    onStyleSelected = onStyleSelected,
                    styleIntensity = styleIntensity,
                    onStyleIntensityChanged = onStyleIntensityChanged
                )
            }
        }
    }
}

// MARK: - 物体移除参数

@Composable
private fun RemoveToolSection() {
    Column {
        Text(
            "物体移除",
            style = DesignSystem.Typography.headline,
            color = DesignSystem.Colors.textPrimary()
        )
        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxxSmall))
        Text(
            "在图片上涂抹想要移除的物体区域",
            style = DesignSystem.Typography.subheadline,
            color = DesignSystem.Colors.textSecondary()
        )
        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
                .border(
                    DesignSystem.Stroke.widthStandard,
                    DesignSystem.Colors.minimalBorder,
                    RoundedCornerShape(DesignSystem.CornerRadius.medium)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "点击图片涂抹选择移除区域",
                style = DesignSystem.Typography.caption1,
                color = DesignSystem.Colors.textTertiary()
            )
        }
    }
}

// MARK: - 天空替换参数

@Composable
private fun SkyReplaceSection(
    selectedSkyType: SkyReplacer.SkyType,
    onSkyTypeSelected: (SkyReplacer.SkyType) -> Unit
) {
    Column {
        Text(
            "天空类型",
            style = DesignSystem.Typography.headline,
            color = DesignSystem.Colors.textPrimary()
        )
        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))

        val skyTypes = SkyReplacer.SkyType.entries
        val skyColors = listOf(
            Color(0xFF87CEEB),  // SUNNY
            Color(0xFFFF7043),  // SUNSET
            Color(0xFF1A237E),  // NIGHT
            Color(0xFF4A148C),  // STARRY
            Color(0xFF00C853),  // AURORA
            Color(0xFF455A64)   // DRAMATIC
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxxSmall)
        ) {
            skyTypes.forEachIndexed { index, skyType ->
                val isSelected = skyType == selectedSkyType
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) DesignSystem.Colors.primary
                    else Color.Transparent,
                    animationSpec = tween(200),
                    label = "skyBorder_$index"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
                        .border(
                            DesignSystem.Stroke.widthThick,
                            borderColor,
                            RoundedCornerShape(DesignSystem.CornerRadius.small)
                        )
                        .background(skyColors[index].copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSkyTypeSelected(skyType) }
                        )
                        .padding(vertical = DesignSystem.Spacing.xSmall),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(DesignSystem.CornerRadius.micro))
                            .background(skyColors[index])
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        skyType.displayName,
                        style = DesignSystem.Typography.caption2,
                        color = if (isSelected) DesignSystem.Colors.textPrimary()
                        else DesignSystem.Colors.textSecondary()
                    )
                }
            }
        }
    }
}

// MARK: - 风格迁移参数

@Composable
private fun StyleTransferSection(
    selectedStyle: StyleTransfer.ArtStyle,
    onStyleSelected: (StyleTransfer.ArtStyle) -> Unit,
    styleIntensity: Float,
    onStyleIntensityChanged: (Float) -> Unit
) {
    Column {
        Text(
            "艺术风格",
            style = DesignSystem.Typography.headline,
            color = DesignSystem.Colors.textPrimary()
        )
        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))

        val styles = StyleTransfer.ArtStyle.entries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxxSmall)
        ) {
            styles.forEach { style ->
                val isSelected = style == selectedStyle
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) DesignSystem.Colors.primary.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f),
                    animationSpec = tween(200),
                    label = "styleBg_${style.name}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
                        .background(bgColor)
                        .border(
                            DesignSystem.Stroke.widthStandard,
                            if (isSelected) DesignSystem.Colors.primary
                            else Color.Transparent,
                            RoundedCornerShape(DesignSystem.CornerRadius.small)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onStyleSelected(style) }
                        )
                        .padding(vertical = DesignSystem.Spacing.xxSmall),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        style.displayName,
                        style = DesignSystem.Typography.caption2,
                        color = if (isSelected) DesignSystem.Colors.primary
                        else DesignSystem.Colors.textSecondary(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DesignSystem.Spacing.small))

        // 风格强度滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "强度",
                style = DesignSystem.Typography.subheadline,
                color = DesignSystem.Colors.textSecondary()
            )
            Spacer(modifier = Modifier.width(DesignSystem.Spacing.xSmall))
            Slider(
                value = styleIntensity,
                onValueChange = onStyleIntensityChanged,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = DesignSystem.Colors.primary,
                    activeTrackColor = DesignSystem.Colors.primary,
                    inactiveTrackColor = DesignSystem.Colors.gray3()
                )
            )
            Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxxSmall))
            Text(
                "${(styleIntensity * 100).toInt()}%",
                style = DesignSystem.Typography.monoCaption,
                color = DesignSystem.Colors.textSecondary(),
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

// MARK: - 图像扩展参数

@Composable
private fun ExpandSection(
    expandAmount: Int,
    onExpandAmountChanged: (Int) -> Unit,
    expandDirection: ImageExpander.ExpansionDirection,
    onExpandDirectionChanged: (ImageExpander.ExpansionDirection) -> Unit
) {
    Column {
        Text(
            "扩展设置",
            style = DesignSystem.Typography.headline,
            color = DesignSystem.Colors.textPrimary()
        )
        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))

        // 扩展量滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "扩展量",
                style = DesignSystem.Typography.subheadline,
                color = DesignSystem.Colors.textSecondary()
            )
            Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxxSmall))
            Slider(
                value = expandAmount.toFloat(),
                onValueChange = { onExpandAmountChanged(it.toInt()) },
                valueRange = 10f..500f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = DesignSystem.Colors.primary,
                    activeTrackColor = DesignSystem.Colors.primary,
                    inactiveTrackColor = DesignSystem.Colors.gray3()
                )
            )
            Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxxSmall))
            Text(
                "${expandAmount}px",
                style = DesignSystem.Typography.monoCaption,
                color = DesignSystem.Colors.textSecondary(),
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))

        // 扩展方向选择
        Text(
            "方向",
            style = DesignSystem.Typography.subheadline,
            color = DesignSystem.Colors.textSecondary()
        )
        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxxSmall))

        val directions = ImageExpander.ExpansionDirection.entries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxxSmall)
        ) {
            directions.forEach { direction ->
                val isSelected = direction == expandDirection
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) DesignSystem.Colors.primary.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f),
                    animationSpec = tween(200),
                    label = "dirBg_${direction.name}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
                        .background(bgColor)
                        .border(
                            DesignSystem.Stroke.widthStandard,
                            if (isSelected) DesignSystem.Colors.primary
                            else Color.Transparent,
                            RoundedCornerShape(DesignSystem.CornerRadius.small)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onExpandDirectionChanged(direction) }
                        )
                        .padding(vertical = DesignSystem.Spacing.xxSmall),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        direction.displayName,
                        style = DesignSystem.Typography.caption2,
                        color = if (isSelected) DesignSystem.Colors.primary
                        else DesignSystem.Colors.textSecondary(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// MARK: - 操作按钮

@Composable
private fun ActionButtons(
    isProcessing: Boolean,
    hasEditResult: Boolean,
    onApply: () -> Unit,
    onApplyAndContinue: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = DesignSystem.Spacing.small)
    ) {
        // 应用按钮
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = DesignSystem.Colors.primary,
                disabledContainerColor = DesignSystem.Colors.primary.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxxSmall))
                Text("处理中…", style = DesignSystem.Typography.headline, color = Color.White)
            } else {
                Icon(
                    Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignSystem.Spacing.xxxSmall))
                Text("应用", style = DesignSystem.Typography.headline, color = Color.White)
            }
        }

        if (hasEditResult) {
            Spacer(modifier = Modifier.height(DesignSystem.Spacing.xSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xSmall)
            ) {
                // 继续编辑
                Button(
                    onClick = onApplyAndContinue,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignSystem.Colors.secondary
                    ),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
                ) {
                    Text("继续编辑", style = DesignSystem.Typography.callout, color = Color.White)
                }

                // 撤销
                Button(
                    onClick = onUndo,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DesignSystem.Colors.textSecondary()
                    ),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
                ) {
                    Text("撤销", style = DesignSystem.Typography.callout)
                }

                // 重置
                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DesignSystem.Colors.error
                    ),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
                ) {
                    Text("重置", style = DesignSystem.Typography.callout)
                }
            }
        }
    }
}
