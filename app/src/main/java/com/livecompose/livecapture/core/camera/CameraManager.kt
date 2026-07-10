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

// #40: Camera2Interop 实验性 API 需要 @OptIn 注解
@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CameraManager"
        private const val TARGET_FRAME_RATE = 60
        private const val EXECUTOR_TIMEOUT_SECONDS = 5L
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio

    private val _isBackCamera = MutableStateFlow(true)
    val isBackCamera: StateFlow<Boolean> = _isBackCamera

    private val _isTorchEnabled = MutableStateFlow(false)
    val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled

    private val _exposureCompensation = MutableStateFlow(0)
    val exposureCompensation: StateFlow<Int> = _exposureCompensation

    // #17: 相机绑定错误状态
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // #55: 相机就绪状态
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
                // #17: 通知 UI 相机启动失败
                _errorMessage.value = "相机启动失败: ${e.message}"
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
        provider.unbindAll()

        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // 尝试设置 60fps（通过 Camera2Interop，兼容性降级）
        val analysisBuilder = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)

        // #40: 通过 Camera2Interop 尝试设置高帧率（已有 @OptIn 类注解）
        try {
            androidx.camera.camera2.interop.Camera2Interop.Extender(analysisBuilder).apply {
                setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    android.util.Range(TARGET_FRAME_RATE, TARGET_FRAME_RATE)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "60fps not supported on this device, using default frame rate")
        }

        imageAnalysis = analysisBuilder.build().also { analysis ->
            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                // 防竞争: 仅当没有帧正在处理时才回调，否则直接丢弃
                if (isProcessingFrame.compareAndSet(false, true)) {
                    onFrameAnalyzed?.invoke(imageProxy)
                }
                // 始终关闭 imageProxy，确保 buffer 回收
                imageProxy.close()
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

            Log.d(TAG, "Camera bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            // #17: 通知 UI 绑定失败
            _errorMessage.value = "相机绑定失败: ${e.message}"
            _isCameraReady.value = false
        }
    }

    /**
     * 标记帧处理完成，允许下一帧进入处理
     */
    fun onFrameProcessingComplete() {
        isProcessingFrame.set(false)
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val current = _isBackCamera.value
        startCamera(lifecycleOwner, previewView, useFrontCamera = current)
    }

    fun setZoom(zoomRatio: Float) {
        val clamped = zoomRatio.coerceIn(0.5f, 5.0f)
        cameraControl?.setZoomRatio(clamped)
        _zoomRatio.value = clamped
    }

    fun setTorchEnabled(enabled: Boolean) {
        cameraControl?.enableTorch(enabled)
        _isTorchEnabled.value = enabled
    }

    fun toggleTorch() {
        setTorchEnabled(!_isTorchEnabled.value)
    }

    fun setExposureCompensation(value: Int) {
        val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange
        if (range != null && value in range.lower..range.upper) {
            cameraControl?.setExposureCompensationIndex(value)
            _exposureCompensation.value = value
        }
    }

    fun focusAndMeter(point: androidx.compose.ui.geometry.Offset, previewWidth: Float, previewHeight: Float) {
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
     * #34: shutdown 等待 Executor 终止完成，避免资源泄漏
     * 注意：此方法仅在 App 销毁时调用，不在 ViewModel.onCleared 中调用
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
