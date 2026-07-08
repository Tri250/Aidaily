package com.livecompose.livecapture.core.video

import android.content.Context
import android.graphics.Bitmap
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * 延时摄影录制器
 *
 * 对应 iOS 端 TimelapseRecorder.swift，以固定时间间隔捕获帧，
 * 合成标准帧率视频，实现时间加速效果。
 *
 * ## 工作原理
 * 1. 创建 [VideoRecorder] 准备输出视频
 * 2. 启动协程定时器，按固定间隔触发帧捕获
 * 3. 每次定时器触发时，从相机获取当前帧（Bitmap）
 * 4. 写入到 VideoRecorder
 * 5. 停止时完成写入，生成最终视频
 *
 * ## 参数配置
 * - interval: 帧捕获间隔（默认 2.0 秒）
 * - outputFrameRate: 输出帧率（默认 30fps）
 * - 默认加速比：30fps × 2s = 60x 速度提升
 *
 * ## 使用示例
 * - 2s 间隔，30fps 输出 → 60x 加速
 * - 1s 间隔，30fps 输出 → 30x 加速
 * - 5s 间隔，30fps 输出 → 150x 加速
 */
class TimelapseRecorder(
    context: Context,
    /** 帧捕获间隔（秒） */
    private val interval: Double = 2.0
) {

    companion object {
        private const val TAG = "TimelapseRecorder"
        /** 输出帧率 */
        private const val OUTPUT_FRAME_RATE = 30
    }

    private val appContext = context.applicationContext
    private val videoRecorder = VideoRecorder(appContext)

    private val _isRecording = MutableStateFlow(false)
    /** 是否正在录制 */
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0.0)
    /** 已录制时间（现实时间，秒） */
    val elapsedTime: StateFlow<Double> = _elapsedTime.asStateFlow()

    private val _estimatedDuration = MutableStateFlow(0.0)
    /** 预估输出视频时长（秒） */
    val estimatedDuration: StateFlow<Double> = _estimatedDuration.asStateFlow()

    private val _frameCount = MutableStateFlow(0)
    /** 已捕获帧数 */
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    /** 预览图像 */
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var captureJob: Job? = null
    private var startTimestampMs: Long = 0L
    @Volatile
    private var latestBitmap: Bitmap? = null

    /**
     * 开始延时摄影录制
     *
     * @param quality 视频质量（决定输出分辨率）
     * @param filterEnabled 是否启用滤镜
     */
    @Throws(VideoRecorderError::class)
    fun startRecording(
        quality: VideoQuality = VideoQuality.HD_1080P_30,
        filterEnabled: Boolean = false
    ) {
        if (_isRecording.value) return

        videoRecorder.startRecording(quality, VideoMode.TIMELAPSE, filterEnabled)
        startTimestampMs = System.currentTimeMillis()
        _frameCount.value = 0
        _estimatedDuration.value = 0.0
        _elapsedTime.value = 0.0
        _isRecording.value = true

        // 启动定时器（协程）
        captureJob = scope.launch {
            // 立即捕获第一帧（等待外部帧到达）
            delay((interval * 1000).toLong())
            while (_isRecording.value) {
                onTimerTick()
                delay((interval * 1000).toLong())
            }
        }
    }

    /**
     * 向录制器提供当前帧（由外部调用方传递）
     *
     * @param bitmap 当前帧的 Bitmap
     */
    fun captureFrame(bitmap: Bitmap) {
        if (!_isRecording.value) return
        latestBitmap = bitmap
        _previewBitmap.value = bitmap
    }

    /**
     * 定时器触发时的处理
     */
    private suspend fun onTimerTick() {
        mutex.withLock {
            if (!_isRecording.value) return@withLock

            // 更新已录制时间
            val elapsed = (System.currentTimeMillis() - startTimestampMs) / 1000.0
            _elapsedTime.value = elapsed

            val bitmap = latestBitmap ?: return@withLock

            // 写入帧（时间戳基于帧索引）
            val currentCount = _frameCount.value
            val presentationTimeUs = (currentCount.toLong() * 1_000_000L / OUTPUT_FRAME_RATE)
            videoRecorder.encodeBitmapFrame(bitmap, presentationTimeUs)

            _frameCount.value = currentCount + 1
            _estimatedDuration.value = (currentCount + 1).toDouble() / OUTPUT_FRAME_RATE
        }
    }

    /**
     * 停止录制
     *
     * @param onComplete 完成回调，返回输出文件路径
     */
    fun stopRecording(onComplete: (String?) -> Unit) {
        if (!_isRecording.value) {
            onComplete(null)
            return
        }

        captureJob?.cancel()
        captureJob = null
        _isRecording.value = false

        videoRecorder.stopRecording { path, error ->
            if (error != null) {
                AppLogger.e(TAG, "延时摄影停止失败", error)
            }
            onComplete(path)
        }
    }

    /**
     * 加速比
     */
    val speedupRatio: Double
        get() {
            if (_frameCount.value <= 0) return 0.0
            val playbackTime = _frameCount.value.toDouble() / OUTPUT_FRAME_RATE
            if (playbackTime <= 0) return 0.0
            return _elapsedTime.value / playbackTime
        }

    /** 格式化时长显示 */
    val formattedElapsedTime: String
        get() {
            val totalSeconds = _elapsedTime.value.toInt()
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    /** 格式化预估时长 */
    val formattedEstimatedDuration: String
        get() {
            val totalSeconds = _estimatedDuration.value.toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    /**
     * 销毁录制器
     */
    fun destroy() {
        if (_isRecording.value) {
            stopRecording {}
        }
        videoRecorder.destroy()
        scope.coroutineContext[Job]?.cancel()
    }
}
