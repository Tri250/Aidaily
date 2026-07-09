package com.livecompose.livecapture.core.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.location.Location
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import com.livecompose.livecapture.core.logger.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * 完整的 RAW/DNG 拍摄管理器
 * 使用 Camera2 RAW_SENSOR 格式创建 ImageReader
 * 使用 Android DngCreator 将 RAW 数据封装为 DNG 文件
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DngCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "DngCaptureManager"
    }

    private val systemCameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager

    private val dngThread = HandlerThread("DngCapture").apply { start() }
    private val dngHandler = Handler(dngThread.looper)
    private var isDestroyed = false

    private var rawImageReader: ImageReader? = null
    private var jpegImageReader: ImageReader? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private var currentCameraId: String = "0"
    private var cameraCharacteristics: CameraCharacteristics? = null

    var onDngSaved: ((String) -> Unit)? = null
    var onJpegSaved: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /**
     * 检查设备是否支持 RAW 拍摄
     */
    fun isRawSupported(cameraId: String = "0"): Boolean {
        return try {
            val characteristics = systemCameraManager.getCameraCharacteristics(cameraId)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取 RAW 传感器输出尺寸
     */
    fun getRawOutputSize(cameraId: String = "0"): Size? {
        return try {
            val characteristics = systemCameraManager.getCameraCharacteristics(cameraId)
            val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)?.maxByOrNull { it.width * it.height }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取 JPEG 输出尺寸
     */
    fun getJpegOutputSize(cameraId: String = "0"): Size? {
        return try {
            val characteristics = systemCameraManager.getCameraCharacteristics(cameraId)
            val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            configMap?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width * it.height }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 打开相机并准备 RAW 拍摄
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.CAMERA)
    fun openCamera(cameraId: String = "0") {
        if (isDestroyed) return
        currentCameraId = cameraId
        try {
            close()
            systemCameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    cameraCharacteristics = try {
                        systemCameraManager.getCameraCharacteristics(cameraId)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "获取相机特性失败", e)
                        null
                    }
                }

                override fun onDisconnected(device: CameraDevice) {
                    AppLogger.w(TAG, "相机断开连接")
                    device.close()
                    cameraDevice = null
                }

                override fun onError(device: CameraDevice, error: Int) {
                    AppLogger.e(TAG, "相机打开失败, 错误码: $error")
                    device.close()
                    cameraDevice = null
                    onError?.invoke("相机打开失败: $error")
                }
            }, dngHandler)
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "相机权限不足", e)
            onError?.invoke("相机权限不足")
        } catch (e: Exception) {
            AppLogger.e(TAG, "相机打开异常", e)
            onError?.invoke("相机打开异常: ${e.message}")
        }
    }

    /**
     * 拍摄 RAW + JPEG 照片
     */
    fun captureRaw(
        location: Location? = null,
        captureCallback: ((CameraCaptureSession.CaptureCallback) -> Unit)? = null
    ) {
        val device = cameraDevice ?: run {
            onError?.invoke("相机未打开")
            return
        }
        val characteristics = cameraCharacteristics ?: run {
            onError?.invoke("相机特性不可用")
            return
        }

        val configMap = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        ) as? StreamConfigurationMap ?: run {
            onError?.invoke("无法获取流配置")
            return
        }

        val rawSize = configMap.getOutputSizes(ImageFormat.RAW_SENSOR)
            .maxByOrNull { it.width * it.height } ?: run {
            onError?.invoke("设备不支持 RAW_SENSOR")
            return
        }

        val jpegSize = configMap.getOutputSizes(ImageFormat.JPEG)
            .maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

        // 创建 RAW ImageReader
        rawImageReader = ImageReader.newInstance(
            rawSize.width, rawSize.height, ImageFormat.RAW_SENSOR, 1
        )

        // 创建 JPEG ImageReader
        jpegImageReader = ImageReader.newInstance(
            jpegSize.width, jpegSize.height, ImageFormat.JPEG, 1
        )

        val rawSurface = rawImageReader?.surface ?: run {
            onError?.invoke("RAW ImageReader 创建失败")
            return
        }
        val jpegSurface = jpegImageReader?.surface ?: run {
            onError?.invoke("JPEG ImageReader 创建失败")
            return
        }

        val targets = listOf(
            OutputConfiguration(rawSurface),
            OutputConfiguration(jpegSurface)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                targets,
                Executors.newSingleThreadExecutor(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        triggerRawCapture(
                            device, characteristics, rawSurface, jpegSurface,
                            rawSize, jpegSize, location, captureCallback
                        )
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError?.invoke("会话配置失败")
                    }
                }
            )
            device.createCaptureSession(sessionConfig)
        } else {
            @Suppress("DEPRECATION")
            device.createCaptureSession(
                listOf(rawSurface, jpegSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        triggerRawCapture(
                            device, characteristics, rawSurface, jpegSurface,
                            rawSize, jpegSize, location, captureCallback
                        )
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError?.invoke("会话配置失败")
                    }
                },
                dngHandler
            )
        }
    }

    private fun triggerRawCapture(
        device: CameraDevice,
        characteristics: CameraCharacteristics,
        rawSurface: Surface,
        jpegSurface: Surface,
        rawSize: Size,
        jpegSize: Size,
        location: Location?,
        captureCallback: ((CameraCaptureSession.CaptureCallback) -> Unit)?
    ) {
        try {
            val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            requestBuilder.addTarget(rawSurface)
            requestBuilder.addTarget(jpegSurface)

            // 设置手动控制参数
            requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF)
            requestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_OFF)
            requestBuilder.set(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_OFF)
            requestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90)

            // 设置传感器参数
            requestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 100)
            requestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 33_000_000L) // 33ms

            // 设置位置信息
            location?.let {
                requestBuilder.set(CaptureRequest.JPEG_GPS_LOCATION, it)
            }

            val captureListener = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    processRawCapture(result, characteristics, rawSize, location)
                }
            }

            captureCallback?.invoke(captureListener)

            val session = captureSession
            if (session == null) {
                AppLogger.w(TAG, "RAW 拍摄失败: captureSession 为 null")
                onError?.invoke("RAW 拍摄失败: 相机会话未就绪")
                return
            }
            session.capture(requestBuilder.build(), captureListener, dngHandler)
        } catch (e: Exception) {
            onError?.invoke("触发 RAW 拍摄失败: ${e.message}")
        }
    }

    private fun processRawCapture(
        result: TotalCaptureResult,
        characteristics: CameraCharacteristics,
        rawSize: Size,
        location: Location?
    ) {
        val rawImage = rawImageReader?.acquireNextImage() ?: run {
            processJpegCapture()
            return
        }

        var outputStream: FileOutputStream? = null
        try {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateStr = dateFormat.format(Date(timestamp))

            // 确保 DCIM/Camera 目录存在
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val cameraDir = File(dcimDir, "Camera")
            if (!cameraDir.exists()) {
                cameraDir.mkdirs()
            }

            val dngFile = File(cameraDir, "RAW_${dateStr}.dng")

            // 使用 DngCreator 创建 DNG 文件
            val dngCreator = DngCreator(characteristics, result)

            // 写入 EXIF 信息
            writeExifInfo(dngCreator, result, characteristics, rawSize, location)

            // 写入 DNG 文件
            outputStream = FileOutputStream(dngFile)
            dngCreator.writeImage(outputStream, rawImage)
            dngCreator.close()

            onDngSaved?.invoke(dngFile.absolutePath)
        } catch (e: Exception) {
            AppLogger.e(TAG, "DNG 保存失败", e)
            onError?.invoke("DNG 保存失败: ${e.message}")
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            rawImage.close()
        }

        // 同时处理 JPEG
        processJpegCapture()
    }

    private fun processJpegCapture() {
        val jpegImage = jpegImageReader?.acquireNextImage() ?: return

        try {
            val buffer = jpegImage.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateStr = dateFormat.format(Date(timestamp))

            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val cameraDir = File(dcimDir, "Camera")
            if (!cameraDir.exists()) {
                cameraDir.mkdirs()
            }

            val jpegFile = File(cameraDir, "IMG_${dateStr}.jpg")
            jpegFile.writeBytes(bytes)

            onJpegSaved?.invoke(jpegFile.absolutePath)
        } catch (e: Exception) {
            onError?.invoke("JPEG 保存失败: ${e.message}")
        } finally {
            jpegImage.close()
        }
    }

    private fun writeExifInfo(
        dngCreator: DngCreator,
        result: TotalCaptureResult,
        characteristics: CameraCharacteristics,
        rawSize: Size,
        location: Location?
    ) {
        try {
            // ISO
            val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: 100
            dngCreator.setDescription("ISO: $iso")

            // 快门速度
            val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 33_000_000L
            val shutterSpeed = exposureTime.toDouble() / 1_000_000_000.0
            dngCreator.setDescription("Shutter: ${shutterSpeed}s")

            // 光圈
            val aperture = result.get(CaptureResult.LENS_APERTURE) ?: 1.8f
            dngCreator.setDescription("Aperture: f/$aperture")

            // 焦距
            val focalLength = result.get(CaptureResult.LENS_FOCAL_LENGTH) ?: 4.0f
            dngCreator.setDescription("Focal: ${focalLength}mm")

            // 传感器尺寸
            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val sensorWidth = sensorSize?.width ?: 0f
            val sensorHeight = sensorSize?.height ?: 0f

            // 方向
            val orientation = result.get(CaptureResult.JPEG_ORIENTATION) ?: 90
            dngCreator.setOrientation(orientation)

            // GPS 坐标
            location?.let {
                dngCreator.setLocation(it)
            }

            // 制造商和型号
            dngCreator.setDescription("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")

        } catch (e: Exception) {
            // EXIF 写入失败不影响主流程
        }
    }

    /**
     * 关闭所有资源
     */
    fun close() {
        try {
            captureSession?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭会话异常", e)
        }
        captureSession = null
        try {
            cameraDevice?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭相机异常", e)
        }
        cameraDevice = null
        try {
            rawImageReader?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭 RAW ImageReader 异常", e)
        }
        rawImageReader = null
        try {
            jpegImageReader?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭 JPEG ImageReader 异常", e)
        }
        jpegImageReader = null
    }

    fun destroy() {
        isDestroyed = true
        close()
        try {
            dngThread.quitSafely()
            dngThread.join(3000)
        } catch (e: Exception) {
            AppLogger.w(TAG, "DNG线程关闭异常", e)
        }
    }
}