package com.livecompose.livecapture.core.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

// Camera2Interop 实验性 API 需要 @OptIn 注解
@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CameraManager"
        private const val TARGET_FRAME_RATE = 60
        private const val FALLBACK_FRAME_RATE = 30
        private const val EXECUTOR_TIMEOUT_SECONDS = 5L
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio

    // 设备支持的缩放范围（从 zoomState 读取，供 UI 动态显示按钮）
    private val _zoomRange = MutableStateFlow(1f..1f)
    val zoomRange: StateFlow<ClosedRange<Float>> = _zoomRange

    private val _isBackCamera = MutableStateFlow(true)
    val isBackCamera: StateFlow<Boolean> = _isBackCamera

    private val _isTorchEnabled = MutableStateFlow(false)
    val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled

    // 设备是否支持闪光灯（前置摄像头通常不支持）
    private val _hasTorchUnit = MutableStateFlow(false)
    val hasTorchUnit: StateFlow<Boolean> = _hasTorchUnit

    private val _exposureCompensation = MutableStateFlow(0)
    val exposureCompensation: StateFlow<Int> = _exposureCompensation

    // 设备支持的曝光补偿范围（供 UI 动态绑定 Slider）
    private val _exposureRange = MutableStateFlow(0..0)
    val exposureRange: StateFlow<IntRange> = _exposureRange

    // 相机绑定错误状态
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 相机就绪状态
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val captureExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // 防止 imageProxy 竞争: 标记是否正在处理帧
    private val isProcessingFrame = AtomicBoolean(false)

    private var onFrameAnalyzed: ((ImageProxy) -> Unit)? = null

    fun setOnFrameAnalyzed(callback: (ImageProxy) -> Unit) {
        onFrameAnalyzed = callback
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean = false
    ) {
        _errorMessage.value = null
        _isCameraReady.value = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView, useFrontCamera)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera", e)
                _errorMessage.value = "相机启动失败"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean
    ) {
        val provider = cameraProvider ?: run {
            _errorMessage.value = "CameraProvider 未初始化"
            return
        }

        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // 切换前检查目标相机是否存在，避免无前置摄像头设备绑定失败
        try {
            if (!provider.hasCamera(cameraSelector)) {
                _errorMessage.value = if (useFrontCamera) "设备无前置摄像头" else "设备无后置摄像头"
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "hasCamera check failed, proceeding anyway", e)
        }

        provider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // 尝试设置高帧率（通过 Camera2Interop），不支持时降级到默认
        val analysisBuilder = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)

        // 通过 Camera2Interop 尝试设置高帧率；60fps 不支持时降级 30fps，再不支持则用默认
        try {
            val extender = androidx.camera.camera2.interop.Camera2Interop.Extender(analysisBuilder)
            // 优先 60fps，失败则 30fps
            extender.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                android.util.Range(TARGET_FRAME_RATE, TARGET_FRAME_RATE)
            )
            Log.d(TAG, "Set target FPS range to [$TARGET_FRAME_RATE, $TARGET_FRAME_RATE]")
        } catch (e: Exception) {
            Log.w(TAG, "60fps not supported, trying 30fps fallback", e)
            try {
                val extender = androidx.camera.camera2.interop.Camera2Interop.Extender(analysisBuilder)
                extender.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    android.util.Range(FALLBACK_FRAME_RATE, FALLBACK_FRAME_RATE)
                )
                Log.d(TAG, "Set target FPS range to [$FALLBACK_FRAME_RATE, $FALLBACK_FRAME_RATE]")
            } catch (e2: Exception) {
                Log.w(TAG, "FPS range setting not supported, using device default", e2)
                // 不设置 FPS range，使用设备默认，相机仍可正常工作
            }
        }

        imageAnalysis = analysisBuilder.build().also { analysis ->
            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                // 防竞争: 仅当没有帧正在处理时才回调，否则直接丢弃
                if (isProcessingFrame.compareAndSet(false, true)) {
                    try {
                        onFrameAnalyzed?.invoke(imageProxy)
                        // 消费者负责关闭 imageProxy: 通过 onFrameProcessingComplete() 或直接 close()
                        // 此处不关闭，因为消费者可能异步使用 buffer
                    } catch (e: Exception) {
                        Log.e(TAG, "Frame analysis callback error", e)
                        isProcessingFrame.set(false)
                        imageProxy.close()  // 回调异常时关闭，避免泄漏
                    }
                } else {
                    // 帧被跳过，立即关闭 imageProxy 回收 buffer
                    imageProxy.close()
                }
            }
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setJpegQuality(95)
            .build()

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
                imageCapture
            )
            cameraControl = camera?.cameraControl
            _isBackCamera.value = !useFrontCamera
            _isCameraReady.value = true
            _errorMessage.value = null

            // 绑定成功后读取设备真实能力，反馈给 UI
            updateCameraCapabilities()

            Log.d(TAG, "Camera bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            _errorMessage.value = "相机绑定失败"
            _isCameraReady.value = false
        }
    }

    /**
     * 从 cameraInfo 读取设备真实能力：缩放范围、曝光范围、闪光灯支持
     * 确保 UI 显示与设备实际能力一致，避免假选中
     */
    private fun updateCameraCapabilities() {
        val cam = camera ?: return
        try {
            // 缩放范围
            val zoomState = cam.cameraInfo.zoomState.value
            if (zoomState != null) {
                _zoomRange.value = zoomState.minZoomRatio..zoomState.maxZoomRatio
                _zoomRatio.value = zoomState.zoomRatio
            }
            // 曝光补偿范围
            val exposureState = cam.cameraInfo.exposureState
            if (exposureState.isExposureCompensationSupported) {
                _exposureRange.value = exposureState.exposureCompensationRange.lower..exposureState.exposureCompensationRange.upper
            } else {
                _exposureRange.value = 0..0
            }
            // 闪光灯支持
            _hasTorchUnit.value = cam.cameraInfo.hasFlashUnit()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read camera capabilities", e)
        }
    }

    /**
     * 标记帧处理完成，允许下一帧进入处理
     * 注意：消费者在调用此方法前必须确保已关闭 imageProxy（或不再使用其 buffer），
     * 因为 imageProxy 的生命周期由消费者管理。
     */
    fun onFrameProcessingComplete() {
        isProcessingFrame.set(false)
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val current = _isBackCamera.value
        startCamera(lifecycleOwner, previewView, useFrontCamera = current)
    }

    fun setZoom(zoomRatio: Float) {
        // 使用设备真实支持的缩放范围钳制
        val range = _zoomRange.value
        val clamped = zoomRatio.coerceIn(range.start, range.endInclusive)
        cameraControl?.setZoomRatio(clamped)
        _zoomRatio.value = clamped
    }

    fun setTorchEnabled(enabled: Boolean) {
        // 无闪光灯单元时不更新状态，避免 UI 假显示
        if (!_hasTorchUnit.value) {
            Log.w(TAG, "Torch not available on this camera")
            return
        }
        val future = cameraControl?.enableTorch(enabled)
        future?.addListener({
            // 仅在成功时更新状态，失败时回滚
            try {
                future.get()
                _isTorchEnabled.value = enabled
            } catch (e: Exception) {
                Log.w(TAG, "enableTorch failed, state not updated", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleTorch() {
        setTorchEnabled(!_isTorchEnabled.value)
    }

    fun setExposureCompensation(value: Int) {
        // 使用设备真实支持的曝光范围校验
        val range = _exposureRange.value
        if (value !in range) {
            Log.w(TAG, "Exposure compensation $value out of range $range")
            return
        }
        cameraControl?.setExposureCompensationIndex(value)
        _exposureCompensation.value = value
    }

    fun focusAndMeter(point: androidx.compose.ui.geometry.Offset, previewWidth: Float, previewHeight: Float) {
        if (previewWidth <= 0f || previewHeight <= 0f) return
        val factory = androidx.camera.core.SurfaceOrientedMeteringPointFactory(
            previewWidth, previewHeight
        )
        val point2 = factory.createPoint(point.x, point.y)
        val action = androidx.camera.core.FocusMeteringAction.Builder(point2).build()
        cameraControl?.startFocusAndMetering(action)
    }

    fun capturePhoto(
        onSuccess: (ImageProxy) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("ImageCapture not initialized"))
            return
        }
        if (captureExecutor.isShutdown) {
            onError(IllegalStateException("Capture executor is shutdown"))
            return
        }

        capture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    onSuccess(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    onError(exception)
                }
            }
        )
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            onFrameAnalyzed = null
            isProcessingFrame.set(false)
            _isCameraReady.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
    }

    /**
     * shutdown 等待 Executor 终止完成，避免资源泄漏
     * 仅在 App 销毁时调用（LiveCaptureApp.onTerminate）
     */
    fun shutdown() {
        stopCamera()
        analysisExecutor.shutdown()
        captureExecutor.shutdown()
        try {
            if (!analysisExecutor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Log.w(TAG, "Analysis executor did not terminate in $EXECUTOR_TIMEOUT_SECONDS seconds")
            }
            if (!captureExecutor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Log.w(TAG, "Capture executor did not terminate in $EXECUTOR_TIMEOUT_SECONDS seconds")
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Executor shutdown interrupted", e)
            Thread.currentThread().interrupt()
        }
    }
}
