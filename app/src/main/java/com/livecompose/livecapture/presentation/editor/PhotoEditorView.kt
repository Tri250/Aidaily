package com.livecompose.livecapture.presentation.editor

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.livecompose.livecapture.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorView(
    photoPath: String,
    onBack: () -> Unit = {},
    viewModel: PhotoEditorViewModel = hiltViewModel()
) {
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val contrast by viewModel.contrast.collectAsStateWithLifecycle()
    val saturation by viewModel.saturation.collectAsStateWithLifecycle()
    val rotation by viewModel.rotation.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.editor_tab_crop),
        stringResource(R.string.editor_tab_filter),
        stringResource(R.string.editor_tab_adjust)
    )

    // Handle save success
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.clearSaveState()
            onBack()
        }
    }

    // Handle save error
    saveError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSaveState() },
            title = { Text(stringResource(R.string.editor_save_error)) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSaveState() }) {
                    Text(stringResource(R.string.editor_ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveEdits(photoPath) },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = stringResource(R.string.editor_save),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Tab-specific content area
                when (selectedTab) {
                    0 -> CropToolbar(viewModel = viewModel)
                    1 -> FilterToolbar(viewModel = viewModel)
                    2 -> AdjustToolbar(viewModel = viewModel)
                }

                // Tab bar
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            EditorImage(
                photoPath = photoPath,
                currentFilter = currentFilter,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                rotation = rotation,
                showCropOverlay = selectedTab == 0,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun EditorImage(
    photoPath: String,
    currentFilter: PhotoFilter,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotation: Int,
    showCropOverlay: Boolean,
    viewModel: PhotoEditorViewModel
) {
    val context = LocalContext.current
    val colorMatrix = remember(currentFilter, brightness, contrast, saturation) {
        val filterMatrix = currentFilter.toColorMatrix()
        val brightnessMatrix = buildBrightnessMatrix(brightness)
        val contrastMatrix = buildContrastMatrix(contrast)
        val saturationMatrix = buildSaturationMatrix(saturation)
        combineMatrices(filterMatrix, brightnessMatrix, contrastMatrix, saturationMatrix)
    }

    val imageData = remember(photoPath) {
        if (photoPath.startsWith("content://")) {
            Uri.parse(photoPath)
        } else {
            File(photoPath)
        }
    }

    val imageModel = remember(imageData) {
        ImageRequest.Builder(context)
            .data(imageData)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    val matrix = colorMatrix.values
                    this.colorMatrix = androidx.compose.ui.graphics.ColorMatrix(matrix)
                }
        )

        if (showCropOverlay) {
            CropOverlay(viewModel = viewModel)
        }
    }
}

@Composable
private fun CropOverlay(viewModel: PhotoEditorViewModel) {
    val cropRegion by viewModel.cropRegion.collectAsStateWithLifecycle()
    val cropAspectRatio by viewModel.cropAspectRatio.collectAsStateWithLifecycle()

    // Compute the crop rect based on aspect ratio constraint
    var computedCrop by remember(cropAspectRatio) {
        mutableStateOf(cropRegion)
    }

    // Reset crop region when aspect ratio changes
    LaunchedEffect(cropAspectRatio) {
        val initial = CropRegion()
        computedCrop = if (cropAspectRatio != null) {
            // Constrain to aspect ratio, centered
            val ratio = cropAspectRatio!!
            if (ratio >= 1f) {
                val height = 1f / ratio
                CropRegion(left = 0f, top = (1f - height) / 2f, right = 1f, bottom = (1f + height) / 2f)
            } else {
                val width = ratio
                CropRegion(left = (1f - width) / 2f, top = 0f, right = (1f + width) / 2f, bottom = 1f)
            }
        } else {
            initial
        }
        viewModel.updateCropRegion(computedCrop)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cropAspectRatio) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / size.width
                    val dy = dragAmount.y / size.height

                    val newCrop = computedCrop.let { crop ->
                        val newLeft = (crop.left + dx).coerceIn(0f, crop.right - 0.1f)
                        val newTop = (crop.top + dy).coerceIn(0f, crop.bottom - 0.1f)
                        crop.copy(left = newLeft, top = newTop)
                    }
                    computedCrop = newCrop
                    viewModel.updateCropRegion(newCrop)
                }
            }
    ) {
        val cropLeft = computedCrop.left * size.width
        val cropTop = computedCrop.top * size.height
        val cropRight = computedCrop.right * size.width
        val cropBottom = computedCrop.bottom * size.height
        val cropWidth = cropRight - cropLeft
        val cropHeight = cropBottom - cropTop

        // Dim outside crop area
        val dimColor = Color.Black.copy(alpha = 0.6f)

        // Top
        drawRect(dimColor, topLeft = Offset(0f, 0f), size = Size(size.width, cropTop))
        // Bottom
        drawRect(dimColor, topLeft = Offset(0f, cropBottom), size = Size(size.width, size.height - cropBottom))
        // Left
        drawRect(dimColor, topLeft = Offset(0f, cropTop), size = Size(cropLeft, cropHeight))
        // Right
        drawRect(dimColor, topLeft = Offset(cropRight, cropTop), size = Size(size.width - cropRight, cropHeight))

        // Crop border
        drawRect(
            color = Color.White,
            topLeft = Offset(cropLeft, cropTop),
            size = Size(cropWidth, cropHeight),
            style = Stroke(width = 2f)
        )

        // Grid lines (rule of thirds)
        val gridColor = Color.White.copy(alpha = 0.5f)
        for (i in 1..2) {
            val x = cropLeft + cropWidth * i / 3f
            val y = cropTop + cropHeight * i / 3f
            drawLine(gridColor, Offset(x, cropTop), Offset(x, cropBottom), strokeWidth = 1f)
            drawLine(gridColor, Offset(cropLeft, y), Offset(cropRight, y), strokeWidth = 1f)
        }

        // Corner handles
        val handleLength = 30f
        val handleWidth = 4f
        val handleColor = Color.White

        // Top-left
        drawLine(handleColor, Offset(cropLeft, cropTop), Offset(cropLeft + handleLength, cropTop), strokeWidth = handleWidth)
        drawLine(handleColor, Offset(cropLeft, cropTop), Offset(cropLeft, cropTop + handleLength), strokeWidth = handleWidth)
        // Top-right
        drawLine(handleColor, Offset(cropRight, cropTop), Offset(cropRight - handleLength, cropTop), strokeWidth = handleWidth)
        drawLine(handleColor, Offset(cropRight, cropTop), Offset(cropRight, cropTop + handleLength), strokeWidth = handleWidth)
        // Bottom-left
        drawLine(handleColor, Offset(cropLeft, cropBottom), Offset(cropLeft + handleLength, cropBottom), strokeWidth = handleWidth)
        drawLine(handleColor, Offset(cropLeft, cropBottom), Offset(cropLeft, cropBottom - handleLength), strokeWidth = handleWidth)
        // Bottom-right
        drawLine(handleColor, Offset(cropRight, cropBottom), Offset(cropRight - handleLength, cropBottom), strokeWidth = handleWidth)
        drawLine(handleColor, Offset(cropRight, cropBottom), Offset(cropRight, cropBottom - handleLength), strokeWidth = handleWidth)
    }
}

@Composable
private fun CropToolbar(viewModel: PhotoEditorViewModel) {
    val cropAspectRatio by viewModel.cropAspectRatio.collectAsStateWithLifecycle()

    val aspectRatios = listOf(
        null to stringResource(R.string.editor_crop_free),
        1f to "1:1",
        3f / 4f to "3:4",
        4f / 3f to "4:3",
        9f / 16f to "9:16",
        16f / 9f to "16:9"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
    ) {
        // Aspect ratio chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            aspectRatios.forEach { (ratio, label) ->
                FilterChip(
                    selected = cropAspectRatio == ratio,
                    onClick = { viewModel.setCropAspectRatio(ratio) },
                    label = { Text(label, fontSize = 13.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rotate button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = { viewModel.rotate() },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.RotateRight,
                    contentDescription = stringResource(R.string.editor_crop_rotate),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.editor_crop_rotate), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FilterToolbar(viewModel: PhotoEditorViewModel) {
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PhotoFilter.entries.forEach { filter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (currentFilter == filter) 3.dp else 1.dp,
                            color = if (currentFilter == filter) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.applyFilter(filter) },
                    contentAlignment = Alignment.Center
                ) {
                    // Show a colored preview for the filter
                    val previewColor = when (filter) {
                        PhotoFilter.ORIGINAL -> Color(0xFF90CAF9)
                        PhotoFilter.BLACK_WHITE -> Color(0xFFBDBDBD)
                        PhotoFilter.WARM -> Color(0xFFFFCC80)
                        PhotoFilter.COOL -> Color(0xFF80DEEA)
                        PhotoFilter.VINTAGE -> Color(0xFFD7CCC8)
                        PhotoFilter.VIVID -> Color(0xFFA5D6A7)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(previewColor, RoundedCornerShape(6.dp))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(filter.nameResId),
                    fontSize = 11.sp,
                    color = if (currentFilter == filter) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (currentFilter == filter) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun AdjustToolbar(viewModel: PhotoEditorViewModel) {
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val contrast by viewModel.contrast.collectAsStateWithLifecycle()
    val saturation by viewModel.saturation.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdjustSlider(
            label = stringResource(R.string.editor_adjust_brightness),
            value = brightness,
            onValueChange = { viewModel.adjustBrightness(it) }
        )
        AdjustSlider(
            label = stringResource(R.string.editor_adjust_contrast),
            value = contrast,
            onValueChange = { viewModel.adjustContrast(it) }
        )
        AdjustSlider(
            label = stringResource(R.string.editor_adjust_saturation),
            value = saturation,
            onValueChange = { viewModel.adjustSaturation(it) }
        )
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = value.toInt().toString(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
