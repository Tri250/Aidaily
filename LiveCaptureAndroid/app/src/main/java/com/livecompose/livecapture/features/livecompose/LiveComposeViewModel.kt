package com.livecompose.livecapture.features.livecompose

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.lut.BuiltInPresets
import com.livecompose.livecapture.core.lut.LutPreset
import com.livecompose.livecapture.core.lut.LutProcessor
import com.livecompose.livecapture.core.storage.PhotoStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * LiveCompose 页面 ViewModel
 * 管理滤镜预览的状态和处理逻辑
 */
class LiveComposeViewModel(application: Application) : AndroidViewModel(application) {

    private val lutProcessor = LutProcessor()
    private val storage = PhotoStorageService(application.applicationContext)

    private val _selectedPreset = MutableStateFlow<LutPreset?>(null)
    val selectedPreset: StateFlow<LutPreset?> = _selectedPreset.asStateFlow()

    private val _intensity = MutableStateFlow(1f)
    val intensity: StateFlow<Float> = _intensity.asStateFlow()

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var demoBitmap: Bitmap? = null

    /**
     * 生成示例渐变图作为演示图片
     */
    fun generateDemoBitmap(context: Context) {
        if (demoBitmap != null) return
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                createDemoGradientBitmap(800, 600)
            }
            demoBitmap = bitmap
            // 默认显示原图
            _processedBitmap.value = bitmap
        }
    }

    /**
     * 选择预设
     */
    fun selectPreset(preset: LutPreset) {
        _selectedPreset.value = preset
        applyCurrentPreset()
    }

    /**
     * 更新强度
     */
    fun updateIntensity(value: Float) {
        _intensity.value = value
        applyCurrentPreset()
    }

    /**
     * 保存处理后的照片
     */
    fun saveProcessedPhoto(context: Context) {
        val bitmap = _processedBitmap.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                val data = stream.toByteArray()
                val presetName = _selectedPreset.value?.name ?: "原图"
                storage.savePhoto(data, detectionMethod = "LUT:$presetName")
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "照片已保存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 应用当前选中的预设
     */
    private fun applyCurrentPreset() {
        val source = demoBitmap ?: return
        val preset = _selectedPreset.value ?: return
        val intensityValue = _intensity.value

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = withContext(Dispatchers.Default) {
                    // 根据强度插值生成混合预设
                    val blendedPreset = blendPresetWithOriginal(preset, intensityValue)
                    lutProcessor.applyPreset(source, blendedPreset)
                }
                val oldBitmap = _processedBitmap.value
                _processedBitmap.value = result
                // 回收旧的处理结果（不回收 demoBitmap）
                if (oldBitmap != null && oldBitmap !== demoBitmap) {
                    oldBitmap.recycle()
                }
            } catch (e: Exception) {
                // 处理失败时保持当前图片
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * 将预设与原图混合，根据强度插值
     */
    private fun blendPresetWithOriginal(preset: LutPreset, intensity: Float): LutPreset {
        if (intensity >= 1f) return preset
        val original = BuiltInPresets.findById("original") ?: return preset
        return LutPreset(
            id = preset.id,
            name = preset.name,
            category = preset.category,
            description = preset.description,
            saturation = original.saturation + (preset.saturation - original.saturation) * intensity,
            contrast = original.contrast + (preset.contrast - original.contrast) * intensity,
            warmth = original.warmth + (preset.warmth - original.warmth) * intensity,
            tint = original.tint + (preset.tint - original.tint) * intensity,
            highlights = original.highlights + (preset.highlights - original.highlights) * intensity,
            shadows = original.shadows + (preset.shadows - original.shadows) * intensity,
            fade = original.fade + (preset.fade - original.fade) * intensity,
            grain = original.grain + (preset.grain - original.grain) * intensity,
            vignette = original.vignette + (preset.vignette - original.vignette) * intensity,
            sharpening = original.sharpening + (preset.sharpening - original.sharpening) * intensity,
            exposure = original.exposure + (preset.exposure - original.exposure) * intensity
        )
    }

    /**
     * 创建示例渐变图
     */
    private fun createDemoGradientBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 天空渐变
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height * 0.6f,
                Color.rgb(135, 206, 250),  // 天蓝
                Color.rgb(255, 183, 77),    // 日落橙
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.6f, skyPaint)

        // 地面渐变
        val groundPaint = Paint().apply {
            shader = LinearGradient(
                0f, height * 0.6f, 0f, height.toFloat(),
                Color.rgb(76, 153, 0),     // 草绿
                Color.rgb(34, 85, 0),       // 深绿
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, height * 0.6f, width.toFloat(), height.toFloat(), groundPaint)

        // 太阳
        val sunPaint = Paint().apply {
            color = Color.rgb(255, 220, 100)
            isAntiAlias = true
        }
        canvas.drawCircle(width * 0.7f, height * 0.25f, 60f, sunPaint)

        // 云彩（简单白色椭圆）
        val cloudPaint = Paint().apply {
            color = Color.argb(180, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawOval(
            android.graphics.RectF(width * 0.15f, height * 0.12f, width * 0.45f, height * 0.2f),
            cloudPaint
        )
        canvas.drawOval(
            android.graphics.RectF(width * 0.55f, height * 0.08f, width * 0.8f, height * 0.16f),
            cloudPaint
        )

        // 远山
        val mountainPaint = Paint().apply {
            color = Color.rgb(80, 100, 80)
            isAntiAlias = true
        }
        val mountainPath = android.graphics.Path().apply {
            moveTo(0f, height * 0.6f)
            lineTo(width * 0.15f, height * 0.4f)
            lineTo(width * 0.3f, height * 0.55f)
            lineTo(width * 0.45f, height * 0.38f)
            lineTo(width * 0.6f, height * 0.52f)
            lineTo(width * 0.75f, height * 0.42f)
            lineTo(width * 0.9f, height * 0.5f)
            lineTo(width.toFloat(), height * 0.45f)
            lineTo(width.toFloat(), height * 0.6f)
            close()
        }
        canvas.drawPath(mountainPath, mountainPaint)

        return bitmap
    }

    override fun onCleared() {
        super.onCleared()
        val currentDemo = demoBitmap
        val currentProcessed = _processedBitmap.value
        // 先回收处理后的bitmap（排除demoBitmap引用）
        if (currentProcessed != null && currentProcessed !== currentDemo) {
            currentProcessed.recycle()
        }
        // 再回收demoBitmap
        currentDemo?.recycle()
        demoBitmap = null
        _processedBitmap.value = null
    }
}
