package com.livecompose.livecapture.core.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
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
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 视频编辑器 - 裁剪、变速、导出
 *
 * 对应 iOS 端 VideoEditor.swift，使用 Android 原生 MediaExtractor +
 * MediaMuxer（裁剪）/ MediaCodec（变速）替代 AVAssetExportSession。
 *
 * ## 核心功能
 * - [trimVideo] 裁剪视频时间范围
 * - [adjustSpeed] 调整视频播放速度
 * - [generateThumbnail] 生成视频缩略图
 * - [getVideoDuration] 获取视频时长
 */
class VideoEditor(private val context: Context) {

    companion object {
        private const val TAG = "VideoEditor"
        private const val TIMEOUT_US = 10_000L
    }

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** 处理进度（0-1） */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** 错误信息 */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 裁剪视频时间范围
     *
     * @param sourcePath 源视频路径
     * @param startTimeUs 开始时间（微秒）
     * @param endTimeUs 结束时间（微秒）
     * @param onComplete 完成回调，返回输出文件路径
     */
    fun trimVideo(
        sourcePath: String,
        startTimeUs: Long,
        endTimeUs: Long,
        onComplete: (String?) -> Unit
    ) {
        if (_isProcessing.value) {
            onComplete(null)
            return
        }
        _isProcessing.value = true
        _progress.value = 0f
        _errorMessage.value = null

        scope.launch {
            var extractor: MediaExtractor? = null
            var muxer: MediaMuxer? = null
            var outputPath: String? = null

            try {
                outputPath = generateOutputPath("_trimmed")
                extractor = MediaExtractor()
                extractor.setDataSource(sourcePath)

                muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val trackIndices = mutableListOf<Pair<Int, Int>>() // (extractorTrack, muxerTrack)
                var durationUs = endTimeUs - startTimeUs

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        extractor.selectTrack(i)
                        val muxerTrack = muxer.addTrack(format)
                        trackIndices.add(i to muxerTrack)
                    }
                }

                muxer.start()

                val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()
                val totalDurationUs = durationUs.coerceAtLeast(1L)

                for ((extractorTrack, muxerTrack) in trackIndices) {
                    extractor.unselectTrack(extractorTrack)
                    // 仅选中当前轨道（避免多轨道交错读取问题）
                    for (i in 0 until extractor.trackCount) {
                        if (i == extractorTrack) extractor.selectTrack(i)
                    }
                    extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                    var firstPresentationTimeUs = -1L
                    while (true) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) break

                        val sampleTime = extractor.sampleTime
                        if (sampleTime > endTimeUs) break
                        if (sampleTime < startTimeUs) {
                            extractor.advance()
                            continue
                        }

                        if (firstPresentationTimeUs < 0) {
                            firstPresentationTimeUs = sampleTime
                        }

                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.flags = extractor.sampleFlags
                        bufferInfo.presentationTimeUs = sampleTime - firstPresentationTimeUs

                        try {
                            muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "写入样本失败", e)
                        }

                        _progress.value = (bufferInfo.presentationTimeUs.toFloat() / totalDurationUs).coerceIn(0f, 1f)
                        extractor.advance()
                    }

                    // 重置选择状态
                    for (i in 0 until extractor.trackCount) {
                        extractor.unselectTrack(i)
                    }
                }

                muxer.stop()
                _progress.value = 1f

                // 保存到媒体库
                outputPath?.let { path ->
                    saveToMediaStore(File(path))
                }

                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onComplete(outputPath)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "视频裁剪失败", e)
                _errorMessage.value = e.message ?: "裁剪失败"
                outputPath?.let { runCatching { File(it).delete() } }
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onComplete(null)
                }
            } finally {
                try { extractor?.release() } catch (_: Exception) {}
                try { muxer?.release() } catch (_: Exception) {}
            }
        }
    }

    /**
     * 调整视频速度
     *
     * @param sourcePath 源视频路径
     * @param speed 速度倍率（>1 加速，<1 减速）
     * @param onComplete 完成回调，返回输出文件路径
     */
    fun adjustSpeed(
        sourcePath: String,
        speed: Float,
        onComplete: (String?) -> Unit
    ) {
        if (_isProcessing.value || speed <= 0f) {
            onComplete(null)
            return
        }
        _isProcessing.value = true
        _progress.value = 0f
        _errorMessage.value = null

        scope.launch {
            var extractor: MediaExtractor? = null
            var muxer: MediaMuxer? = null
            var outputPath: String? = null

            try {
                outputPath = generateOutputPath("_speed${speed}")
                extractor = MediaExtractor()
                extractor.setDataSource(sourcePath)

                muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val trackMap = mutableMapOf<Int, Int>() // extractorTrack -> muxerTrack
                var videoTrack = -1
                var audioTrack = -1
                var sourceDurationUs = 0L

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/")) {
                        videoTrack = i
                        if (format.containsKey(MediaFormat.KEY_DURATION)) {
                            sourceDurationUs = format.getLong(MediaFormat.KEY_DURATION)
                        }
                        // 修改帧率以体现速度变化（MediaFormat 不能直接复制，需重新构造）
                        val width = if (format.containsKey(MediaFormat.KEY_WIDTH))
                            format.getInteger(MediaFormat.KEY_WIDTH) else 0
                        val height = if (format.containsKey(MediaFormat.KEY_HEIGHT))
                            format.getInteger(MediaFormat.KEY_HEIGHT) else 0
                        val newFormat = MediaFormat.createVideoFormat(mime, width, height)
                        // 复制常用键
                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            val originalFps = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                            newFormat.setInteger(
                                MediaFormat.KEY_FRAME_RATE,
                                (originalFps * speed).toInt().coerceAtLeast(1)
                            )
                        }
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            newFormat.setInteger(
                                MediaFormat.KEY_BIT_RATE,
                                format.getInteger(MediaFormat.KEY_BIT_RATE)
                            )
                        }
                        if (format.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) {
                            newFormat.setInteger(
                                MediaFormat.KEY_I_FRAME_INTERVAL,
                                format.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL)
                            )
                        }
                        trackMap[i] = muxer.addTrack(newFormat)
                    } else if (mime.startsWith("audio/")) {
                        audioTrack = i
                        trackMap[i] = muxer.addTrack(format)
                    }
                }

                if (videoTrack < 0) {
                    throw IllegalStateException("未找到视频轨道")
                }

                muxer.start()

                val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                // 处理每个轨道：调整时间戳实现变速
                for ((extractorTrack, muxerTrack) in trackMap) {
                    for (i in 0 until extractor.trackCount) {
                        extractor.unselectTrack(i)
                    }
                    extractor.selectTrack(extractorTrack)
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                    val isAudio = extractorTrack == audioTrack

                    while (true) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) break

                        val sampleTime = extractor.sampleTime
                        // 通过缩放时间戳实现变速
                        val newTimeUs = (sampleTime / speed).toLong()

                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.flags = if (isAudio) extractor.sampleFlags else extractor.sampleFlags
                        bufferInfo.presentationTimeUs = newTimeUs

                        try {
                            muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "写入样本失败", e)
                        }

                        if (!isAudio && sourceDurationUs > 0) {
                            _progress.value = (sampleTime.toFloat() / sourceDurationUs).coerceIn(0f, 1f)
                        }
                        extractor.advance()
                    }
                }

                muxer.stop()
                _progress.value = 1f

                outputPath?.let { path ->
                    saveToMediaStore(File(path))
                }

                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onComplete(outputPath)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "视频变速失败", e)
                _errorMessage.value = e.message ?: "变速处理失败"
                outputPath?.let { runCatching { File(it).delete() } }
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onComplete(null)
                }
            } finally {
                try { extractor?.release() } catch (_: Exception) {}
                try { muxer?.release() } catch (_: Exception) {}
            }
        }
    }

    /**
     * 获取视频时长（微秒）
     */
    fun getVideoDuration(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            (durationStr?.toLongOrNull() ?: 0L) * 1000L
        } catch (e: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * 生成视频缩略图
     *
     * @param path 视频路径
     * @param timeUs 截取时间点（微秒）
     * @return 缩略图 Bitmap，失败返回 null
     */
    fun generateThumbnail(path: String, timeUs: Long = 0L): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            // 获取原始尺寸，缩放到 480 宽
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val width = widthStr?.toIntOrNull() ?: 480
            val height = heightStr?.toIntOrNull() ?: 360
            val targetWidth = 480
            val targetHeight = (height * targetWidth.toFloat() / width).toInt().coerceAtLeast(1)

            retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )?.let { bitmap ->
                if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
                    Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                        .also { if (it != bitmap) bitmap.recycle() }
                } else {
                    bitmap
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "生成缩略图失败", e)
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * 保存到媒体库
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
     * 生成临时输出文件路径
     */
    private fun generateOutputPath(suffix: String): String {
        val outputDir = File(context.cacheDir, "videos").apply { mkdirs() }
        val fileName = "LiveCapture${suffix}_${System.currentTimeMillis()}.mp4"
        val file = File(outputDir, fileName)
        if (file.exists()) file.delete()
        return file.absolutePath
    }

    /**
     * 销毁编辑器
     */
    fun destroy() {
        scope.coroutineContext[Job]?.cancel()
    }
}
