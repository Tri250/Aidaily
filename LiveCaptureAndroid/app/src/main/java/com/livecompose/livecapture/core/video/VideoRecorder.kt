package com.livecompose.livecapture.core.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * 视频录制器 - 使用 MediaCodec + MediaMuxer 实现高质量视频录制
 *
 * 对应 iOS 端 VideoRecorder.swift，使用 Android 原生 MediaCodec（硬件编码器）
 * 替代 AVAssetWriter，MediaMuxer 替代 MP4 容器写入。
 *
 * ## 核心功能
 * - 使用 MediaCodec 硬件编码（H.264/H.265）+ MediaMuxer MP4 容器
 * - 可选 AAC 音频编码（MediaCodec）
 * - 可选的实时滤镜处理
 * - 录制过程中可拍照
 * - 支持暂停/恢复录制
 *
 * ## 数据流
 * 相机帧（YUV/Image）→ encodeFrame(byte[], timestamp) → MediaCodec → MediaMuxer → 输出 MP4
 *
 * ## 线程安全
 * - 编码操作在 [Dispatchers.Default] 上执行
 * - StateFlow 状态更新线程安全
 */
open class VideoRecorder(private val context: Context) {

    companion object {
        private const val TAG = "VideoRecorder"
        private const val TIMEOUT_US = 10_000L
        private const val MIME_TYPE_VIDEO = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MIME_TYPE_AUDIO = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val AUDIO_SAMPLE_RATE = 44_100
        private const val AUDIO_BIT_RATE = 128_000
        private const val AUDIO_CHANNEL_COUNT = 2
    }

    // MARK: - 状态

    private val _recordingState = MutableStateFlow(VideoRecordingState())
    /** 录制状态（供 UI 绑定） */
    val recordingState: StateFlow<VideoRecordingState> = _recordingState.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    /** 预览图像（用于实时预览） */
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    // MARK: - 编码相关

    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false

    private var outputFilePath: String? = null
    /** 最近一次录制完成的输出文件路径（公开只读） */
    val lastRecordedPath: String? get() = outputFilePath
    private var startTimeUs: Long = -1L
    private val frameCounter = AtomicLong(0)
    private val bufferInfo = MediaCodec.BufferInfo()
    private val audioBufferInfo = MediaCodec.BufferInfo()

    @Volatile
    private var isPaused = false
    private var pausedDurationUs = 0L
    private var pauseStartTimeUs = 0L

    private var currentQuality: VideoQuality = VideoQuality.HD_1080P_30
    private var currentMode: VideoMode = VideoMode.NORMAL

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val encodeMutex = Mutex()

    // MARK: - 开始录制

    /**
     * 开始录制视频
     *
     * @param quality 视频质量
     * @param mode 录制模式
     * @param filterEnabled 是否启用滤镜
     * @throws VideoRecorderError 启动失败时抛出
     */
    @Throws(VideoRecorderError::class)
    open fun startRecording(
        quality: VideoQuality,
        mode: VideoMode,
        filterEnabled: Boolean = false
    ) {
        if (_recordingState.value.isRecording) {
            throw VideoRecorderError.WriterInWrongState
        }

        currentQuality = quality
        currentMode = mode

        // 1. 创建输出文件
        val outputDir = File(context.cacheDir, "videos").apply { mkdirs() }
        val fileName = "LiveCapture_${System.currentTimeMillis()}.mp4"
        val outputFile = File(outputDir, fileName)
        if (outputFile.exists()) outputFile.delete()
        outputFilePath = outputFile.absolutePath

        // 2. 创建 MediaMuxer
        mediaMuxer = try {
            MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            throw VideoRecorderError.CannotCreateWriter
        }

        // 3. 配置并启动视频编码器
        try {
            setupVideoEncoder(quality, mode)
        } catch (e: Exception) {
            AppLogger.e(TAG, "视频编码器配置失败", e)
            releaseResources()
            throw VideoRecorderError.CannotAddVideoInput
        }

        // 4. 配置音频编码器（如果需要）
        if (mode.requiresAudio) {
            try {
                setupAudioEncoder()
            } catch (e: Exception) {
                AppLogger.w(TAG, "音频编码器配置失败，将仅录制视频", e)
            }
        }

        // 5. 启动编码器
        videoEncoder?.start()
        audioEncoder?.start()

        // 6. 重置状态
        startTimeUs = -1L
        frameCounter.set(0)
        isPaused = false
        pausedDurationUs = 0L
        pauseStartTimeUs = 0L
        muxerStarted = false

        // 7. 更新状态
        _recordingState.value = VideoRecordingState(
            isRecording = true,
            mode = mode,
            quality = quality,
            stabilizationEnabled = true,
            filterEnabled = filterEnabled
        )
    }

    /**
     * 配置视频编码器
     */
    private fun setupVideoEncoder(quality: VideoQuality, mode: VideoMode) {
        val format = MediaFormat.createVideoFormat(
            MIME_TYPE_VIDEO,
            quality.dimensions.width,
            quality.dimensions.height
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, getVideoBitRate(quality, mode))
            setInteger(MediaFormat.KEY_FRAME_RATE, getVideoFrameRate(quality, mode))
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            // 启用 B 帧以提升压缩率（设备支持时）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setInteger("bframe-interval", 1)
            }
        }

        val codec = tryCreateEncoder(MIME_TYPE_VIDEO, format)
            ?: throw VideoRecorderError.CannotAddVideoInput
        videoEncoder = codec
    }

    /**
     * 配置音频编码器（AAC）
     */
    private fun setupAudioEncoder() {
        val format = MediaFormat.createAudioFormat(
            MIME_TYPE_AUDIO,
            AUDIO_SAMPLE_RATE,
            AUDIO_CHANNEL_COUNT
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }

        val codec = tryCreateEncoder(MIME_TYPE_AUDIO, format) ?: return
        audioEncoder = codec
    }

    /**
     * 尝试创建编码器（优先硬件编码器）
     */
    private fun getCodecName(info: MediaCodecInfo): String {
        return try {
            info.javaClass.getMethod("getName").invoke(info) as String
        } catch (_: Exception) {
            @Suppress("DEPRECATION")
            info.name
        }
    }

    @Suppress("DEPRECATION")
    private fun tryCreateEncoder(mimeType: String, format: MediaFormat): MediaCodec? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        // 优先选择硬件编码器
        val hardwareCodec = codecList.codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) } &&
                !getCodecName(info).contains("soft", ignoreCase = true)
        }
        val codecName = if (hardwareCodec != null) {
            getCodecName(hardwareCodec)
        } else {
            codecList.findEncoderForFormat(format) ?: return null
        }
        return try {
            MediaCodec.createByCodecName(codecName).apply { configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }
        } catch (e: Exception) {
            AppLogger.w(TAG, "创建编码器失败: $codecName", e)
            null
        }
    }

    /**
     * 获取视频比特率（可被子类重写，如慢动作）
     */
    protected open fun getVideoBitRate(quality: VideoQuality, mode: VideoMode): Int {
        return quality.bitRate
    }

    /**
     * 获取视频帧率（可被子类重写，如慢动作）
     */
    protected open fun getVideoFrameRate(quality: VideoQuality, mode: VideoMode): Int {
        return quality.frameRate
    }

    // MARK: - 编码视频帧

    /**
     * 编码一帧 YUV 数据（来自 CameraX ImageAnalysis）
     *
     * @param yuvData YUV_420_888 字节数据
     * @param width 帧宽度
     * @param height 帧高度
     * @param timestampUs 时间戳（微秒）
     */
    fun encodeFrame(yuvData: ByteArray, width: Int, height: Int, timestampUs: Long) {
        if (!_recordingState.value.isRecording || isPaused) return

        scope.launch {
            encodeMutex.withLock {
                try {
                    if (startTimeUs < 0) {
                        startTimeUs = timestampUs
                    }
                    val adjustedTimeUs = timestampUs - startTimeUs - pausedDurationUs
                    if (adjustedTimeUs < 0) return@withLock

                    val encoder = videoEncoder ?: return@withLock

                    // 输入
                    val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.let {
                            it.clear()
                            it.put(yuvData)
                            encoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                yuvData.size,
                                adjustedTimeUs,
                                0
                            )
                        }
                    }

                    // 输出
                    drainEncoder(encoder, isVideo = true)

                    // 更新预览与状态
                    frameCounter.incrementAndGet()
                    val duration = adjustedTimeUs / 1_000_000.0
                    _recordingState.value = _recordingState.value.copy(
                        duration = duration,
                        fileSize = estimateFileSize(duration)
                    )
                } catch (e: Exception) {
                    AppLogger.e(TAG, "视频帧编码失败", e)
                }
            }
        }
    }

    /**
     * 编码一帧 Bitmap（用于延时摄影或处理后的帧）
     */
    fun encodeBitmapFrame(bitmap: Bitmap, timestampUs: Long) {
        if (!_recordingState.value.isRecording || isPaused) return

        scope.launch {
            encodeMutex.withLock {
                try {
                    if (startTimeUs < 0) {
                        startTimeUs = timestampUs
                    }
                    val adjustedTimeUs = timestampUs - startTimeUs - pausedDurationUs
                    if (adjustedTimeUs < 0) return@withLock

                    val encoder = videoEncoder ?: return@withLock

                    val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.let {
                            it.clear()
                            // 将 Bitmap 转为 YUV（简化处理：直接使用 ARGB 输入，多数编码器不支持，
                            // 这里通过 YuvImage 转换以确保兼容性）
                            val yuv = bitmapToYuv420(bitmap)
                            it.put(yuv)
                            encoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                yuv.size,
                                adjustedTimeUs,
                                0
                            )
                        }
                    }
                    drainEncoder(encoder, isVideo = true)

                    frameCounter.incrementAndGet()
                    val duration = adjustedTimeUs / 1_000_000.0
                    _recordingState.value = _recordingState.value.copy(
                        duration = duration,
                        fileSize = estimateFileSize(duration)
                    )
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Bitmap 帧编码失败", e)
                }
            }
        }
    }

    /**
     * 编码音频帧（PCM）
     *
     * @param pcmData PCM 字节数据
     * @param timestampUs 时间戳（微秒）
     */
    fun encodeAudioFrame(pcmData: ByteArray, timestampUs: Long) {
        if (!_recordingState.value.isRecording || isPaused) return
        val audioEncoder = audioEncoder ?: return

        scope.launch {
            encodeMutex.withLock {
                try {
                    if (startTimeUs < 0) return@withLock
                    val adjustedTimeUs = timestampUs - startTimeUs - pausedDurationUs
                    if (adjustedTimeUs < 0) return@withLock

                    val inputIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = audioEncoder.getInputBuffer(inputIndex)
                        inputBuffer?.let {
                            it.clear()
                            it.put(pcmData)
                            audioEncoder.queueInputBuffer(
                                inputIndex, 0, pcmData.size, adjustedTimeUs, 0
                            )
                        }
                    }
                    drainEncoder(audioEncoder, isVideo = false)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "音频帧编码失败", e)
                }
            }
        }
    }

    /**
     * 从编码器提取输出数据并写入 MediaMuxer
     */
    private fun drainEncoder(encoder: MediaCodec, isVideo: Boolean) {
        val trackIndex = if (isVideo) videoTrackIndex else audioTrackIndex
        val info = if (isVideo) bufferInfo else audioBufferInfo

        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // 仅在第一次输出格式变化时启动 muxer
                    synchronized(this) {
                        val muxer = mediaMuxer
                        if (!muxerStarted && muxer != null) {
                            val newTrack = muxer.addTrack(encoder.outputFormat)
                            if (isVideo) {
                                videoTrackIndex = newTrack
                            } else {
                                audioTrackIndex = newTrack
                            }
                            // 当至少有一个轨道就绪时启动 muxer
                            val hasVideoTrack = videoTrackIndex >= 0
                            val needsAudio = currentMode.requiresAudio
                            val hasAudioTrack = audioTrackIndex >= 0
                            if (hasVideoTrack && (!needsAudio || hasAudioTrack)) {
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                    }
                }
                outputIndex >= 0 -> {
                    if (!muxerStarted) {
                        // 等待 muxer 启动
                        encoder.releaseOutputBuffer(outputIndex, false)
                        continue
                    }
                    val outputBuffer = encoder.getOutputBuffer(outputIndex)
                    if (info.size > 0 && outputBuffer != null) {
                        outputBuffer.position(info.offset)
                        outputBuffer.limit(info.offset + info.size)
                        if (trackIndex >= 0) {
                            try {
                                mediaMuxer?.writeSampleData(trackIndex, outputBuffer, info)
                            } catch (e: Exception) {
                                AppLogger.w(TAG, "写入样本数据失败", e)
                            }
                        }
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)

                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        return
                    }
                }
                else -> return
            }
        }
    }

    // MARK: - 停止录制

    /**
     * 停止录制
     *
     * @param onComplete 完成回调，返回输出文件路径或异常
     */
    fun stopRecording(onComplete: (String?, Exception?) -> Unit) {
        if (!_recordingState.value.isRecording) {
            onComplete(null, VideoRecorderError.WriterInWrongState)
            return
        }

        val recordedDuration = _recordingState.value.duration
        val filePath = outputFilePath

        scope.launch {
            try {
                encodeMutex.withLock {
                    // 发送 EOS 标记
                    sendEndOfStream(videoEncoder, isVideo = true)
                    sendEndOfStream(audioEncoder, isVideo = false)

                    // 排空剩余数据
                    videoEncoder?.let { drainEncoder(it, isVideo = true) }
                    audioEncoder?.let { drainEncoder(it, isVideo = false) }
                }

                // 停止 muxer
                synchronized(this) {
                    if (muxerStarted) {
                        mediaMuxer?.stop()
                    }
                }

                withContext(Dispatchers.Main) {
                    _recordingState.value = _recordingState.value.copy(
                        isRecording = false,
                        recordedDuration = recordedDuration
                    )
                }

                // 保存到媒体库
                filePath?.let { path ->
                    val savedUri = saveToMediaStore(File(path))
                    releaseResources()
                    withContext(Dispatchers.Main) {
                        onComplete(savedUri?.toString() ?: path, null)
                    }
                } ?: run {
                    releaseResources()
                    withContext(Dispatchers.Main) {
                        onComplete(null, VideoRecorderError.FileCreationFailed)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "停止录制失败", e)
                releaseResources()
                withContext(Dispatchers.Main) {
                    onComplete(null, e)
                }
            }
        }
    }

    /**
     * 发送结束流标记
     */
    private fun sendEndOfStream(encoder: MediaCodec?, isVideo: Boolean) {
        if (encoder == null) return
        try {
            val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
            if (inputIndex >= 0) {
                encoder.queueInputBuffer(
                    inputIndex, 0, 0,
                    System.nanoTime() / 1000,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "发送 EOS 失败", e)
        }
    }

    // MARK: - 暂停/恢复

    /**
     * 暂停录制
     */
    fun pauseRecording() {
        if (!_recordingState.value.isRecording || isPaused) return
        isPaused = true
        pauseStartTimeUs = System.nanoTime() / 1000
    }

    /**
     * 恢复录制
     */
    fun resumeRecording() {
        if (!_recordingState.value.isRecording || !isPaused) return
        isPaused = false
        pausedDurationUs += (System.nanoTime() / 1000) - pauseStartTimeUs
    }

    // MARK: - 录制中拍照

    /**
     * 在录制过程中拍照（从最近一帧 YUV 生成 JPEG）
     *
     * @param yuvData 最近的 YUV 数据
     * @param width 帧宽
     * @param height 帧高
     * @return JPEG 字节数据，失败返回 null
     */
    fun capturePhotoDuringRecording(
        yuvData: ByteArray,
        width: Int,
        height: Int
    ): ByteArray? {
        return try {
            val yuvImage = YuvImage(yuvData, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 92, out)
            out.toByteArray()
        } catch (e: Exception) {
            AppLogger.w(TAG, "录制中拍照失败", e)
            null
        }
    }

    // MARK: - 私有辅助方法

    /**
     * 预估文件大小
     */
    private fun estimateFileSize(duration: Double): Long {
        val videoBitRate = currentQuality.bitRate.toLong()
        val audioBitRate = if (currentMode.requiresAudio) AUDIO_BIT_RATE.toLong() else 0L
        val totalBitRate = videoBitRate + audioBitRate
        return (duration * totalBitRate / 8.0).toLong()
    }

    /**
     * 将 Bitmap 转换为 YUV_420_888（NV12）
     */
    private fun bitmapToYuv420(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val ySize = width * height
        val yuv = ByteArray(ySize + ySize / 2)
        var yIndex = 0
        var uvIndex = ySize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val argbPixel = argb[j * width + i]
                val r = (argbPixel shr 16) and 0xFF
                val g = (argbPixel shr 8) and 0xFF
                val b = argbPixel and 0xFF

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = y.toByte()
                if (j % 2 == 0 && i % 2 == 0 && uvIndex < yuv.size - 1) {
                    yuv[uvIndex++] = u.toByte()
                    yuv[uvIndex++] = v.toByte()
                }
            }
        }
        return yuv
    }

    /**
     * 保存视频到媒体库（MediaStore）
     */
    private fun saveToMediaStore(file: File): Uri? {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LiveCapture")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存到媒体库失败", e)
            null
        }
    }

    /**
     * 释放所有资源
     */
    @Synchronized
    private fun releaseResources() {
        try {
            videoEncoder?.let {
                try { it.stop() } catch (_: Exception) {}
                it.release()
            }
            audioEncoder?.let {
                try { it.stop() } catch (_: Exception) {}
                it.release()
            }
            if (muxerStarted) {
                try { mediaMuxer?.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "释放资源异常", e)
        } finally {
            videoEncoder = null
            audioEncoder = null
            mediaMuxer = null
            videoTrackIndex = -1
            audioTrackIndex = -1
            muxerStarted = false
            startTimeUs = -1L
            outputFilePath = null
            frameCounter.set(0)
        }
    }

    /**
     * 销毁录制器（清理协程）
     */
    fun destroy() {
        releaseResources()
        scope.coroutineContext[Job]?.cancel()
    }
}
