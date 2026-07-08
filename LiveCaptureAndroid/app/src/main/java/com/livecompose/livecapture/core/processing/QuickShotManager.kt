package com.livecompose.livecapture.core.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * 快速抓拍管理器
 * Quick Shot 模式：启动即拍，零延迟抓拍
 */
class QuickShotManager {

    companion object {
        private const val TAG = "QuickShotManager"
        private const val MAX_BUFFER_SIZE = 3
    }

    private val _isQuickShotMode = MutableStateFlow(false)
    val isQuickShotMode: StateFlow<Boolean> = _isQuickShotMode.asStateFlow()

    private val _burstCount = MutableStateFlow(0)
    val burstCount: StateFlow<Int> = _burstCount.asStateFlow()

    private val _lastCaptureTimeMs = MutableStateFlow(0L)
    val lastCaptureTimeMs: StateFlow<Long> = _lastCaptureTimeMs.asStateFlow()

    private val isCapturing = AtomicBoolean(false)
    private val frameBuffer = mutableListOf<ByteArray>()
    private val captureTimestamps = mutableListOf<Long>()

    /**
     * 启用/禁用 Quick Shot 模式
     */
    fun setQuickShotMode(enabled: Boolean) {
        _isQuickShotMode.value = enabled
        if (enabled) {
            frameBuffer.clear()
            captureTimestamps.clear()
        }
    }

    /**
     * 缓冲帧（持续缓冲最近 N 帧）
     * 当用户按下快门时，直接使用缓冲中的帧
     */
    fun bufferFrame(jpegData: ByteArray) {
        if (!_isQuickShotMode.value) return

        synchronized(frameBuffer) {
            frameBuffer.add(jpegData)
            if (frameBuffer.size > MAX_BUFFER_SIZE) {
                frameBuffer.removeFirst()
            }
        }
    }

    /**
     * 快速抓拍
     * 返回缓冲中最新的一帧 + 当前帧
     */
    fun quickCapture(currentFrame: ByteArray? = null): ByteArray? {
        if (!_isQuickShotMode.value) return currentFrame

        val startTime = System.currentTimeMillis()
        isCapturing.set(true)

        val result = synchronized(frameBuffer) {
            // 优先使用缓冲中的最新帧
            if (frameBuffer.isNotEmpty()) {
                frameBuffer.last()
            } else {
                currentFrame
            }
        }

        _lastCaptureTimeMs.value = System.currentTimeMillis() - startTime
        isCapturing.set(false)

        return result
    }

    /**
     * 连拍模式
     * 快速拍摄多张照片
     */
    fun startBurst() {
        _burstCount.value = 0
        isCapturing.set(true)
    }

    fun addBurstFrame(jpegData: ByteArray): ByteArray {
        synchronized(frameBuffer) {
            _burstCount.value = _burstCount.value + 1
            captureTimestamps.add(System.currentTimeMillis())
        }
        return jpegData
    }

    fun stopBurst(): List<ByteArray> {
        isCapturing.set(false)
        return synchronized(frameBuffer) {
            frameBuffer.toList()
        }
    }

    /**
     * 获取连拍帧率
     */
    fun getBurstFps(): Float {
        if (captureTimestamps.size < 2) return 0f
        val duration = captureTimestamps.last() - captureTimestamps.first()
        if (duration <= 0) return 0f
        return (captureTimestamps.size - 1).toFloat() / duration * 1000f
    }

    /**
     * 3:4 裁剪（复用 CameraManager 的逻辑）
     */
    fun cropToThreeByFour(jpegData: ByteArray): ByteArray? {
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
}
