package com.livecompose.livecapture.core.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RAW 拍摄管理器
 * 管理 Camera2 RAW sensor 拍摄流程
 * 与现有 CameraManager 协同工作
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object RawCaptureManager {

    /**
     * 检查设备是否支持 RAW 拍摄
     */
    fun isRawSupported(context: Context, cameraId: String = "0"): Boolean {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
        return try {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取 RAW 输出尺寸
     */
    fun getRawOutputSize(context: Context, cameraId: String = "0"): Size? {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
        return try {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)?.maxByOrNull { it.width * it.height }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取传感器的黑电平和白电平
     */
    fun getBlackWhiteLevels(context: Context, cameraId: String = "0"): Pair<Int, Int> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
        return try {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val blackLevel = characteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN)
                ?.let { pattern ->
                    maxOf(
                        pattern.getOffsetForIndex(0, 0),
                        pattern.getOffsetForIndex(1, 0),
                        pattern.getOffsetForIndex(0, 1),
                        pattern.getOffsetForIndex(1, 1)
                    )
                } ?: 0
            val whiteLevel = characteristics.get(android.hardware.camera2.CameraCharacteristics.Key("android.sensor.whiteLevel", Int::class.java)) ?: 1023
            Pair(blackLevel, whiteLevel)
        } catch (e: Exception) {
            Pair(0, 1023)
        }
    }

    /**
     * 获取传感器的色彩校正矩阵
     */
    fun getColorCorrectionMatrix(context: Context, cameraId: String = "0"): FloatArray? {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
        return try {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val ccm = characteristics.get(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM1)
            ccm?.let { matrix ->
                FloatArray(9) { i -> matrix.getElement(i % 3, i / 3).toFloat() }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 配置 RAW 拍摄请求的 Builder
     */
    fun configureRawRequest(builder: CaptureRequest.Builder): CaptureRequest.Builder {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF)
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, 100) // ISO 100
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 33_000_000L) // 33ms
        builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_OFF)
        builder.set(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_OFF)
        builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
        return builder
    }
}
