package com.livecompose.livecapture.core.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.MeteringRectangle
import android.graphics.SurfaceTexture
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
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Android 相机管理器 - 基于 Camera2 API
 */
class CameraManager(private val context: Context) {

    companion object {
        private const val TAG = "CameraManager"
    }

    private val systemCameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager

    private var cameraDevice: CameraDevice? = null
        @Synchronized set
        @Synchronized get
    private var captureSession: CameraCaptureSession? = null
        @Synchronized set
        @Synchronized get
    private var imageReader: ImageReader? = null
        @Synchronized set
        @Synchronized get
    private var previewSurface: Surface? = null

    private val backgroundThread = HandlerThread("CameraBackground").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)
    private var isDestroyed = false

    private var cameraId: String = "0" // 0=back, 1=front
    private var cameraCharacteristics: CameraCharacteristics? = null

    private val _isSessionRunning = MutableStateFlow(false)
    val isSessionRunning: StateFlow<Boolean> = _isSessionRunning.asStateFlow()

    private val _lastPhotoSaved = MutableStateFlow(false)
    val lastPhotoSaved: StateFlow<Boolean> = _lastPhotoSaved.asStateFlow()

    private val _cameraError = MutableStateFlow<CameraErrorType?>(null)
    val cameraError: StateFlow<CameraErrorType?> = _cameraError.asStateFlow()

    private val _isCameraOpened = MutableStateFlow(false)
    val isCameraOpened: StateFlow<Boolean> = _isCameraOpened.asStateFlow()

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

    // [v1.1.7] 专业相机管理器 - 手动控制参数应用到 CaptureRequest
    var proCameraManager: ProCameraManager? = null

    // 闪光灯模式
    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    // AE/AF 锁定
    private val _aeLocked = MutableStateFlow(false)
    val aeLocked: StateFlow<Boolean> = _aeLocked.asStateFlow()
    private val _afLocked = MutableStateFlow(false)
    val afLocked: StateFlow<Boolean> = _afLocked.asStateFlow()

    // 对焦状态
    private val _focusState = MutableStateFlow(FocusState.IDLE)
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    // 拍摄比例
    private val _aspectRatio = MutableStateFlow(AspectRatio.RATIO_3_4)
    val aspectRatio: StateFlow<AspectRatio> = _aspectRatio.asStateFlow()

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * 打开相机并开始预览
     */
    @SuppressLint("MissingPermission")
    fun openCamera(cameraId: String = "0") {
        if (!hasCameraPermission()) {
            _cameraError.value = CameraErrorType.PERMISSION_DENIED
            return
        }
        if (isDestroyed) return
        this.cameraId = cameraId
        this.isFrontCamera = cameraId == "1"
        _cameraError.value = null
        try {
            closeCamera()
            systemCameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    _isCameraOpened.value = true
                    _cameraError.value = null
                    shouldBeRunning = true
                    cameraCharacteristics = try {
                        systemCameraManager.getCameraCharacteristics(cameraId)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "获取相机特性失败", e)
                        null
                    }
                    configureZoomCapabilities()
                    createCameraPreviewSession()
                }

                override fun onDisconnected(device: CameraDevice) {
                    AppLogger.w(TAG, "相机断开连接")
                    _cameraError.value = CameraErrorType.CAMERA_DISCONNECTED
                    _isCameraOpened.value = false
                    device.close()
                    cameraDevice = null
                }

                override fun onError(device: CameraDevice, error: Int) {
                    AppLogger.e(TAG, "相机打开失败, 错误码: $error")
                    _cameraError.value = when (error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> CameraErrorType.CAMERA_IN_USE
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> CameraErrorType.CAMERA_DISCONNECTED
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> CameraErrorType.NO_CAMERA_HARDWARE
                        else -> CameraErrorType.UNKNOWN
                    }
                    _isCameraOpened.value = false
                    shouldBeRunning = false
                    device.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "相机权限不足", e)
            _cameraError.value = CameraErrorType.PERMISSION_DENIED
        } catch (e: Exception) {
            AppLogger.e(TAG, "相机打开异常", e)
            _cameraError.value = CameraErrorType.UNKNOWN
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
        _isCameraOpened.value = false
        _cameraError.value = null
    }

    /**
     * 重新打开相机（拍照后恢复使用）
     */
    fun reopenCamera() {
        if (isDestroyed) return
        shouldBeRunning = true
        if (cameraId.isNotEmpty()) {
            openCamera(cameraId)
        }
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

        // 关闭旧的 ImageReader 和 Session
        try {
            captureSession?.close()
            captureSession = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭旧会话失败", e)
        }

        // 设置 ImageReader 用于帧分析
        val characteristics = cameraCharacteristics ?: return
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as? StreamConfigurationMap ?: return

        // 选择最佳预览尺寸：匹配屏幕比例
        val previewSize = selectOptimalPreviewSize(map)

        try {
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
        } catch (e: Exception) {
            AppLogger.e(TAG, "创建 ImageReader 失败", e)
            return
        }
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                try {
                    onSampleBuffer?.invoke(image)
                } catch (e: Exception) {
                    AppLogger.w(TAG, "帧处理异常", e)
                } finally {
                    image.close()
                }
            }
        }, backgroundHandler)

        val targets = mutableListOf<Surface>()
        targets.add(surface)
        imageReader?.surface?.let { targets.add(it) }

        try {
            device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        requestBuilder.addTarget(surface)
                        imageReader?.surface?.let { requestBuilder.addTarget(it) }
                        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                        captureRequestBuilder = requestBuilder
                        // [v1.1.7] 应用专业模式手动控制参数（ISO、快门、白平衡、对焦等）
                        proCameraManager?.applyToCaptureRequest(requestBuilder, characteristics)
                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                        _isSessionRunning.value = true
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "预览请求创建失败", e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    AppLogger.e(TAG, "相机会话配置失败")
                    _isSessionRunning.value = false
                    _cameraError.value = CameraErrorType.SESSION_CONFIG_FAILED
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            AppLogger.e(TAG, "创建相机会话异常", e)
        }
    }

    /**
     * 选择最佳预览尺寸
     *
     * 适配国内品牌手机屏幕比例:
     * - 华为/荣耀: 19.5:9 ~ 20.5:9
     * - 小米: 20:9 ~ 20.5:9
     * - OPPO/vivo: 20:9
     *
     * 策略: 优先选择与屏幕比例最接近的预览尺寸，
     * 避免拉伸或裁剪导致的构图偏差
     */
    private fun selectOptimalPreviewSize(map: StreamConfigurationMap): Size {
        val availableSizes = map.getOutputSizes(SurfaceTexture::class.java)
        if (availableSizes.isNullOrEmpty()) return Size(1920, 1080)

        // 获取屏幕尺寸
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val screenAspect = screenWidth.toFloat() / screenHeight.toFloat()

        // 选择 16:9 或 4:3 中最接近屏幕比例的尺寸
        // 优先选择宽度 >= 1080 的尺寸以保证清晰度
        val sorted = availableSizes
            .filter { it.width >= 1080 }
            .sortedByDescending { it.width * it.height }

        if (sorted.isEmpty()) {
            val fallback = availableSizes.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)
            AppLogger.i(TAG, "预览尺寸(fallback): ${fallback.width}x${fallback.height}")
            return fallback
        }

        // 按与屏幕比例差距排序，选择最接近的
        val best = sorted.minByOrNull {
            val aspect = it.width.toFloat() / it.height.toFloat()
            kotlin.math.abs(aspect - screenAspect)
        } ?: sorted.first()

        AppLogger.i(TAG, "预览尺寸: ${best.width}x${best.height}, 屏幕比例: $screenAspect, 尺寸比例: ${best.width.toFloat()/best.height}")
        return best
    }

    /**
     * 拍摄照片
     */
    fun capturePhoto() {
        val device = cameraDevice
        if (device == null) {
            AppLogger.w(TAG, "拍照失败: cameraDevice 为 null")
            _lastPhotoSaved.value = false
            return
        }
        val characteristics = cameraCharacteristics
        if (characteristics == null) {
            AppLogger.w(TAG, "拍照失败: cameraCharacteristics 为 null")
            _lastPhotoSaved.value = false
            return
        }
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as? StreamConfigurationMap
        if (map == null) {
            AppLogger.w(TAG, "拍照失败: StreamConfigurationMap 为 null")
            _lastPhotoSaved.value = false
            return
        }
        val largestSize = map.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

        val session = captureSession
        if (session == null) {
            AppLogger.w(TAG, "拍照失败: captureSession 为 null，尝试重建预览会话")
            _lastPhotoSaved.value = false
            shouldBeRunning = true
            // 尝试重建会话后重试
            createCameraPreviewSession()
            // 延迟后重试
            backgroundHandler.postDelayed({
                val retrySession = captureSession
                if (retrySession == null) {
                    AppLogger.e(TAG, "拍照重试失败: captureSession 仍然为 null")
                    _lastPhotoSaved.value = false
                    return@postDelayed
                }
                doCapturePhoto(device, map, largestSize, retrySession)
            }, 500)
            return
        }

        doCapturePhoto(device, map, largestSize, session)
    }

    /**
     * 执行实际的拍照操作
     */
    private fun doCapturePhoto(
        device: CameraDevice,
        map: StreamConfigurationMap,
        photoSize: Size,
        session: CameraCaptureSession
    ) {
        val photoReader = ImageReader.newInstance(photoSize.width, photoSize.height, ImageFormat.JPEG, 1)
        photoReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            if (image == null) {
                reader.close()
                return@setOnImageAvailableListener
            }
            try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                // 裁剪为 3:4
                val cropped = cropToThreeByFour(bytes)
                onPhotoDataReady?.invoke(cropped ?: bytes)
                _lastPhotoSaved.value = true
            } catch (e: Exception) {
                AppLogger.e(TAG, "照片数据处理失败", e)
                _lastPhotoSaved.value = false
            } finally {
                image.close()
                reader.close()
            }
        }, backgroundHandler)

        try {
            val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            requestBuilder.addTarget(photoReader.surface)
            requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90)
            // [v1.1.7] 应用专业模式手动控制参数到拍照请求
            proCameraManager?.applyToCaptureRequest(requestBuilder, cameraCharacteristics!!)
            session.capture(requestBuilder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            AppLogger.e(TAG, "拍照请求失败", e)
            _lastPhotoSaved.value = false
            photoReader.close()
        }
    }

    /**
     * 裁剪为 3:4 比例
     */
    private fun cropToThreeByFour(jpegData: ByteArray): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size) ?: return null
        try {
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

            // 安全边界检查
            cropWidth = cropWidth.coerceAtMost(width - startX)
            cropHeight = cropHeight.coerceAtMost(height - startY)

            val cropped = Bitmap.createBitmap(bitmap, startX, startY, cropWidth, cropHeight)
            val output = ByteArrayOutputStream()
            cropped.compress(Bitmap.CompressFormat.JPEG, 95, output)
            cropped.recycle()
            return output.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * 切换前后摄像头
     */
    fun toggleCameraPosition() {
        if (isDestroyed) return
        val newCameraId = if (isFrontCamera) "0" else "1"
        AppLogger.d(TAG, "切换摄像头: $cameraId -> $newCameraId")
        openCamera(newCameraId)
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
        // [v1.1.7] 变焦时同步应用专业模式参数
        proCameraManager?.applyToCaptureRequest(requestBuilder, cameraCharacteristics!!)
        try {
            captureSession?.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            AppLogger.w(TAG, "应用变焦失败", e)
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

    // ====== 对焦 / 闪光灯 / AE-AF 锁 ======

    /**
     * 点按对焦/测光
     * @param x 归一化 X 坐标 (0..1)
     * @param y 归一化 Y 坐标 (0..1)
     * @param sensorSize 传感器尺寸
     */
    fun tapToFocus(x: Float, y: Float, sensorSize: Rect) {
        val requestBuilder = captureRequestBuilder
        if (requestBuilder == null) {
            AppLogger.w(TAG, "对焦失败: captureRequestBuilder 为 null")
            return
        }
        val session = captureSession
        if (session == null) {
            AppLogger.w(TAG, "对焦失败: captureSession 为 null")
            return
        }
        _focusState.value = FocusState.FOCUSING
        val meteringRect = normalizedToSensorRect(x, y, sensorSize)
        requestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRect))
        requestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(meteringRect))
        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
        requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        try {
            session.capture(requestBuilder.build(), null, backgroundHandler)
            requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
            session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
            _focusState.value = FocusState.FOCUSED
        } catch (e: Exception) {
            AppLogger.w(TAG, "对焦失败", e)
            _focusState.value = FocusState.FAILED
        }
    }

    /**
     * 归一化坐标转传感器区域
     */
    private fun normalizedToSensorRect(x: Float, y: Float, sensorSize: Rect): MeteringRectangle {
        val halfSide = 50
        val cx = (x * sensorSize.width()).toInt().coerceIn(halfSide, sensorSize.width() - halfSide)
        val cy = (y * sensorSize.height()).toInt().coerceIn(halfSide, sensorSize.height() - halfSide)
        return MeteringRectangle(cx - halfSide, cy - halfSide, halfSide * 2, halfSide * 2, MeteringRectangle.METERING_WEIGHT_MAX)
    }

    /**
     * 切换闪光灯模式
     */
    fun toggleFlashMode() {
        val modes = FlashMode.entries
        val nextIndex = (modes.indexOf(_flashMode.value) + 1) % modes.size
        _flashMode.value = modes[nextIndex]
        applyFlashMode()
    }

    /**
     * 设置闪光灯模式
     */
    fun setFlashMode(mode: FlashMode) {
        _flashMode.value = mode
        applyFlashMode()
    }

    private fun applyFlashMode() {
        val requestBuilder = captureRequestBuilder ?: return
        when (_flashMode.value) {
            FlashMode.OFF -> {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
            }
            FlashMode.AUTO -> {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH)
                requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
            }
            FlashMode.ON -> {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
            }
            FlashMode.TORCH -> {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
            }
        }
        try {
            captureSession?.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            AppLogger.w(TAG, "应用闪光模式失败", e)
        }
    }

    /**
     * 切换 AE 锁定
     */
    fun toggleAELock() {
        _aeLocked.value = !_aeLocked.value
        val requestBuilder = captureRequestBuilder ?: return
        requestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, _aeLocked.value)
        try {
            captureSession?.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            AppLogger.w(TAG, "AE锁定失败", e)
        }
    }

    /**
     * 切换 AF 锁定
     */
    fun toggleAFLock() {
        _afLocked.value = !_afLocked.value
        if (_afLocked.value) {
            val requestBuilder = captureRequestBuilder
            if (requestBuilder == null) {
                AppLogger.w(TAG, "AF锁定失败: captureRequestBuilder 为 null")
                _afLocked.value = false
                return
            }
            val session = captureSession
            if (session == null) {
                AppLogger.w(TAG, "AF锁定失败: captureSession 为 null")
                _afLocked.value = false
                return
            }
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
            requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            try {
                session.capture(requestBuilder.build(), null, backgroundHandler)
                requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
                session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
            } catch (e: Exception) {
                AppLogger.w(TAG, "AF锁定失败", e)
            }
        }
    }

    /**
     * 设置拍摄比例
     */
    fun setAspectRatio(ratio: AspectRatio) {
        _aspectRatio.value = ratio
    }

    /**
     * 获取传感器尺寸
     */
    fun getSensorRect(): Rect? {
        return cameraCharacteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    }

    fun destroy() {
        isDestroyed = true
        closeCamera()
        try {
            backgroundThread.quitSafely()
            backgroundThread.join(3000)
        } catch (e: Exception) {
            AppLogger.w(TAG, "后台线程关闭异常", e)
        }
    }
}