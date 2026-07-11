package com.livecompose.livecapture.presentation.editor

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.graphics.ColorMatrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.storage.PhotoStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CropRegion(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    private val storageService: PhotoStorageService
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(PhotoFilter.ORIGINAL)
    val currentFilter: StateFlow<PhotoFilter> = _currentFilter.asStateFlow()

    private val _brightness = MutableStateFlow(0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _contrast = MutableStateFlow(0f)
    val contrast: StateFlow<Float> = _contrast.asStateFlow()

    private val _saturation = MutableStateFlow(0f)
    val saturation: StateFlow<Float> = _saturation.asStateFlow()

    private val _cropRegion = MutableStateFlow(CropRegion())
    val cropRegion: StateFlow<CropRegion> = _cropRegion.asStateFlow()

    private val _cropAspectRatio = MutableStateFlow<Float?>(null)
    val cropAspectRatio: StateFlow<Float?> = _cropAspectRatio.asStateFlow()

    private val _rotation = MutableStateFlow(0)
    val rotation: StateFlow<Int> = _rotation.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun applyFilter(filter: PhotoFilter) {
        _currentFilter.value = filter
    }

    fun adjustBrightness(value: Float) {
        _brightness.value = value
    }

    fun adjustContrast(value: Float) {
        _contrast.value = value
    }

    fun adjustSaturation(value: Float) {
        _saturation.value = value
    }

    fun setCropAspectRatio(ratio: Float?) {
        _cropAspectRatio.value = ratio
    }

    fun updateCropRegion(region: CropRegion) {
        _cropRegion.value = region
    }

    fun rotate() {
        _rotation.value = (_rotation.value + 90) % 360
    }

    fun resetEdits() {
        _currentFilter.value = PhotoFilter.ORIGINAL
        _brightness.value = 0f
        _contrast.value = 0f
        _saturation.value = 0f
        _cropRegion.value = CropRegion()
        _cropAspectRatio.value = null
        _rotation.value = 0
    }

    fun clearSaveState() {
        _saveSuccess.value = false
        _saveError.value = null
    }

    fun getCombinedColorMatrix(): ColorMatrix {
        val filterMatrix = _currentFilter.value.toColorMatrix()
        val brightnessMatrix = buildBrightnessMatrix(_brightness.value)
        val contrastMatrix = buildContrastMatrix(_contrast.value)
        val saturationMatrix = buildSaturationMatrix(_saturation.value)
        return combineMatrices(filterMatrix, brightnessMatrix, contrastMatrix, saturationMatrix)
    }

    fun getEditedBitmap(original: Bitmap): Bitmap {
        var bitmap = original

        // Apply rotation
        if (_rotation.value != 0) {
            val matrix = Matrix().apply { postRotate(_rotation.value.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) {
                bitmap.recycle()
            }
            bitmap = rotated
        }

        // Apply crop
        val crop = _cropRegion.value
        if (crop.left != 0f || crop.top != 0f || crop.right != 1f || crop.bottom != 1f) {
            val x = (crop.left * bitmap.width).toInt().coerceAtLeast(0)
            val y = (crop.top * bitmap.height).toInt().coerceAtLeast(0)
            val w = ((crop.right - crop.left) * bitmap.width).toInt().coerceAtLeast(1).coerceAtMost(bitmap.width - x)
            val h = ((crop.bottom - crop.top) * bitmap.height).toInt().coerceAtLeast(1).coerceAtMost(bitmap.height - y)
            val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
            if (cropped !== bitmap) {
                bitmap.recycle()
            }
            bitmap = cropped
        }

        return bitmap
    }

    fun saveEdits(photoPath: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            _saveSuccess.value = false
            try {
                withContext(Dispatchers.IO) {
                    val original = storageService.loadBitmapFromPath(photoPath)
                        ?: throw IllegalStateException("Failed to load photo")

                    try {
                        val edited = getEditedBitmap(original)

                        // Apply color filter
                        val colorMatrix = getCombinedColorMatrix()
                        val paint = android.graphics.Paint().apply {
                            val androidColorMatrix = android.graphics.ColorMatrix(colorMatrix.values)
                            colorFilter = android.graphics.ColorFilter(androidColorMatrix)
                        }

                        val filtered = Bitmap.createBitmap(edited.width, edited.height, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(filtered)
                        canvas.drawBitmap(edited, 0f, 0f, paint)
                        if (filtered !== edited) {
                            edited.recycle()
                        }

                        storageService.saveEditedPhoto(filtered)
                        filtered.recycle()
                    } finally {
                        original.recycle()
                    }
                }
                _saveSuccess.value = true
            } catch (e: Exception) {
                _saveError.value = e.message ?: "Save failed"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
