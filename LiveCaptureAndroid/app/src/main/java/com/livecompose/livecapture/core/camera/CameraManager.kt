package com.livecompose.livecapture.core.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Android 相机管理器 - 基于 Camera2 API
 * 对应 iOS 的 CameraManager
 */
class CameraManager(private val context: Context) {

    private val systemCameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null

    private val backgroundThread = HandlerThread("CameraBackground").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)

    private var cameraId: String = "0" // 0=back, 1=front
    private var cameraCharacteristics: CameraCharacteristics? = null

    private val _isSessionRunning = MutableStateFlow(false)
    val isSessionRunning: StateFlow<Boolean> = _isSessionRunning.asStateFlow()

    private val _lastPhotoSaved = MutableStateFlow(false)
    val lastPhotoSaved: StateFlow<Boolean> = _lastPhotoSaved.asStateFlow()

    private val _zoomState = MutableStateFlow(ZoomState())
    val zoomState: StateFlow<ZoomState> = _zoomState.asStateFlow()

    private val _zoomPresets = MutableStateFlow<List<ZoomPreset>>(emptyList())
    val zoomPresets: StateFlow<List<ZoomPreset>> = _zoomPresets.asStateFlow()

    private val _zoomRange = MutableStateFlow(1.0f..1.0f)
    val zoomRange: StateFlow<ClosedFloatingPointRange<Float>> = _zoomRange.asStateFlow()

    private val _availableLenses = MutableStateFlow<List<LensKind>>(emptyList())
    val availableLenses: StateFlow<List<LensKind>> = _availableLenses.asStateFlow()

    var isFrontCamera: Boolean = false
        private set

    var onSampleBuffer: ((Image) -> Unit)? = null
    var onPhotoDataReady: ((ByteArray) -> Unit)? = null

    var shouldBeRunning: Boolean = false

    private var captureRequestBuilder: CaptureRequest.Builder? = null

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * 打开相机并开始预览
     */
    @SuppressLint("MissingPermission")
    fun openCamera(cameraId: String = "0") {
        if (!hasCameraPermission()) return
        this.cameraId = cameraId
        this.isFrontCamera = cameraId == "1"
        try {
            systemCameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    cameraCharacteristics = systemCameraManager.getCameraCharacteristics(cameraId)
                    configureZoomCapabilities()
                    createCameraPreviewSession()
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                    cameraDevice = null
                }

                override fun onError(device: CameraDevice, error: Int) {
                    device.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: SecurityException) {
            // Permission denied
        }
    }

    fun closeCamera() {
        shouldBeRunning = false
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        _isSessionRunning.value = false
    }

    /**
     * 设置预览 Surface
     */
    fun setPreviewSurface(surface: Surface) {
        previewSurface = surface
        cameraDevice?.let { createCameraPreviewSession() }
    }

    /**
     * 创建预览会话
     */
    private fun createCameraPreviewSession() {
        val device = cameraDevice ?: return
        val surface = previewSurface ?: return

        // 设置 ImageReader 用于帧分析
        val characteristics = cameraCharacteristics ?: return
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as StreamConfigurationMap
        val previewSize = map.getOutputSizes(SurfaceTexture::class.java).firstOrNull() ?: Size(1920, 1080)

        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                onSampleBuffer?.invoke(image)
                image.close()
            }
        }, backgroundHandler)

        val targets = mutableListOf<Surface>()
        targets.add(surface)
        imageReader?.surface?.let { targets.add(it) }

        try {
            device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    requestBuilder.addTarget(surface)
                    imageReader?.surface?.let { requestBuilder.addTarget(it) }
                    requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    captureRequestBuilder = requestBuilder
                    session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                    _isSessionRunning.value = true
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, backgroundHandler)
        } catch (e: Exception) {
            // Handle error
        }
    }

    /**
     * 拍摄照片
     */
    fun capturePhoto() {
        val device = cameraDevice ?: return
        val characteristics = cameraCharacteristics ?: return
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as StreamConfigurationMap
        val largestSize = map.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

        val photoReader = ImageReader.newInstance(largestSize.width, largestSize.height, ImageFormat.JPEG, 1)
        photoReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()

            // 裁剪为 3:4
            val cropped = cropToThreeByFour(bytes)
            onPhotoDataReady?.invoke(cropped ?: bytes)
            _lastPhotoSaved.value = true
            reader.close()
        }, backgroundHandler)

        try {
            val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            requestBuilder.addTarget(photoReader.surface)
            requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90)
            captureSession?.capture(requestBuilder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            _lastPhotoSaved.value = false
        }
    }

    /**
     * 裁剪为 3:4 比例
     */
    private fun cropToThreeByFour(jpegData: ByteArray): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size) ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val desiredAspect = 3.0f / 4.0f
        val currentAspect = width.toFloat() / height.toFloat()

        var cropWidth = width
        var cropHeight = height
        var startX = 0
        var startY = 0

        if (currentAspect > desiredAspect) {
            cropWidth = (height * desiredAspect).toInt()
            startX = (width - cropWidth) / 2
        } else if (currentAspect < desiredAspect) {
            cropHeight = (width / desiredAspect).toInt()
            startY = (height - cropHeight) / 2
        }

        val cropped = Bitmap.createBitmap(bitmap, startX, startY, cropWidth, cropHeight)
        val output = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.JPEG, 95, output)
        bitmap.recycle()
        cropped.recycle()
        return output.toByteArray()
    }

    /**
     * 切换前后摄像头
     */
    fun toggleCameraPosition() {
        val newId = if (cameraId == "0") "1" else "0"
        closeCamera()
        openCamera(newId)
    }

    /**
     * 配置变焦能力
     */
    private fun configureZoomCapabilities() {
        val characteristics = cameraCharacteristics ?: return
        val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        val range = 1.0f..maxZoom.coerceAtMost(10.0f)

        val lenses = if (isFrontCamera) {
            listOf(LensKind.FRONT)
        } else {
            listOf(LensKind.ULTRA_WIDE, LensKind.WIDE, LensKind.TELEPHOTO)
        }

        val presets = buildZoomPresets(range, lenses)
        _zoomRange.value = range
        _availableLenses.value = lenses
        _zoomPresets.value = presets
        _zoomState.value = ZoomState(currentFactor = 1.0f, displayedFactor = 1.0f, focalLength = 24, activeLens = LensKind.WIDE)
    }

    private fun buildZoomPresets(range: ClosedFloatingPointRange<Float>, lenses: List<LensKind>): List<ZoomPreset> {
        val presets = mutableListOf<ZoomPreset>()
        if (lenses.contains(LensKind.ULTRA_WIDE)) {
            presets.add(ZoomPreset(LensKind.ULTRA_WIDE, 0.5f, 13, ZoomPreset.PresetStyle.SECONDARY))
        }
        val primary = if (lenses.contains(LensKind.FRONT)) LensKind.FRONT else LensKind.WIDE
        presets.add(ZoomPreset(primary, 1.0f, 24, ZoomPreset.PresetStyle.PRIMARY))
        if (range.contains(2.0f) && primary != LensKind.FRONT) {
            presets.add(ZoomPreset(primary, 2.0f, 48, ZoomPreset.PresetStyle.SECONDARY))
        }
        return presets.sortedBy { it.zoomFactor }
    }

    /**
     * 选择变焦预设
     */
    fun selectZoomPreset(preset: ZoomPreset) {
        applyZoomFactor(preset.zoomFactor)
    }

    /**
     * 交互式变焦更新
     */
    fun updateInteractiveZoom(factor: Float) {
        applyZoomFactor(factor)
    }

    /**
     * 完成交互式变焦
     */
    fun finalizeInteractiveZoom(factor: Float) {
        applyZoomFactor(factor)
    }

    private fun applyZoomFactor(factor: Float) {
        val clamped = factor.coerceIn(_zoomRange.value.start, _zoomRange.value.endInclusive)
        val requestBuilder = captureRequestBuilder ?: return
        requestBuilder.set(CaptureRequest.SCALER_CROP_REGION, computeZoomRect(clamped))
        try {
            captureSession?.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            // Ignore
        }
        val lens = when {
            isFrontCamera -> LensKind.FRONT
            clamped < 0.9f -> LensKind.ULTRA_WIDE
            clamped > 2.4f -> LensKind.TELEPHOTO
            else -> LensKind.WIDE
        }
        val focal = (clamped * 24).roundToInt()
        _zoomState.value = ZoomState(
            currentFactor = clamped,
            displayedFactor = (clamped * 100).roundToInt() / 100f,
            focalLength = focal,
            activeLens = lens,
            isContinuous = false
        )
    }

    private fun computeZoomRect(zoomFactor: Float): android.graphics.Rect {
        val characteristics = cameraCharacteristics ?: return android.graphics.Rect(0, 0, 100, 100)
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: android.graphics.Rect(0, 0, 100, 100)
        val centerX = sensorSize.centerX()
        val centerY = sensorSize.centerY()
        val width = (sensorSize.width() / zoomFactor).toInt()
        val height = (sensorSize.height() / zoomFactor).toInt()
        return android.graphics.Rect(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2)
    }

    fun destroy() {
        closeCamera()
        backgroundThread.quitSafely()
    }
}