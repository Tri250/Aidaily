package com.livecompose.livecapture.core.video

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 音频采集器
 *
 * 为视频录制提供真实的音频采集（替代 iOS AVCaptureAudioDataOutput），通过 [AudioRecord]
 * 读取 PCM 数据并回调给 [VideoRecorder.encodeAudioFrame] 编码。
 *
 * ## 技术细节
 * - 采样率：44100 Hz（与 [VideoRecorder] 的 AAC 编码器一致）
 * - 声道：立体声（CHANNEL_IN_STEREO）
 * - 位深：16 位 PCM（ENCODING_PCM_16BIT）
 * - 缓冲区：[AudioRecord.getMinBufferSize] 的 2 倍，保证不丢帧
 *
 * ## 权限要求
 * 需要 Manifest 声明 `RECORD_AUDIO` 权限，并已通过运行时权限请求授予。
 * 调用方应在 [start] 前确认权限已授予。
 *
 * @param onPcmData PCM 数据回调（参数：数据、时间戳微秒）
 */
class AudioCapture(
    private val onPcmData: (ByteArray, Long) -> Unit
) {

    companion object {
        private const val TAG = "AudioCapture"
        private const val SAMPLE_RATE = 44_100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 读取缓冲区大小（字节） */
    private var bufferSize = 0

    /** 是否正在采集 */
    @Volatile
    private var isCapturing = false

    /** 录制起始时间戳（毫秒），用于计算 PTS */
    private var startMillis = 0L

    /**
     * 检查 AudioRecord 是否可用（权限已授予且硬件支持）
     *
     * @return true 表示可以初始化 AudioRecord
     */
    fun isAvailable(): Boolean {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBuf <= 0) return false
        // 尝试初始化验证（仅查询状态，不真正录制）
        return try {
            val ar = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBuf * 2
            )
            val ok = ar.state == AudioRecord.STATE_INITIALIZED
            ar.release()
            ok
        } catch (e: Exception) {
            AppLogger.w(TAG, "AudioRecord 不可用", e)
            false
        }
    }

    /**
     * 开始音频采集
     *
     * 在独立 IO 协程中循环读取 PCM 数据，通过 [onPcmData] 回调。
     * 时间戳基于 [System.nanoTime] 转换为微秒，与视频帧 PTS 对齐。
     */
    @SuppressLint("MissingPermission")
    fun start() {
        if (isCapturing) return
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize <= 0) {
            AppLogger.w(TAG, "AudioRecord 缓冲区大小无效: $bufferSize")
            return
        }
        bufferSize *= 2

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "AudioRecord 初始化失败", e)
            return
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            AppLogger.w(TAG, "AudioRecord 未初始化成功")
            audioRecord?.release()
            audioRecord = null
            return
        }

        isCapturing = true
        startMillis = System.currentTimeMillis()
        audioRecord?.startRecording()

        captureJob = scope.launch {
            // 每次读取 1/10 缓冲区的数据，约 23ms，保证时间分辨率
            val readSize = max(bufferSize / 10, 1024)
            val buffer = ByteArray(readSize)
            while (isActive && isCapturing) {
                val read = audioRecord?.read(buffer, 0, readSize) ?: -1
                if (read > 0) {
                    val ptsUs = (System.currentTimeMillis() - startMillis) * 1000L
                    onPcmData(buffer.copyOf(read), ptsUs)
                } else if (read < 0 && read != AudioRecord.ERROR_INVALID_OPERATION) {
                    // ERROR_INVALID_OPERATION 出现在暂停时，属正常；其他错误记录
                    AppLogger.w(TAG, "AudioRecord 读取异常: $read")
                }
            }
        }

        AppLogger.d(TAG, "音频采集已启动（采样率: $SAMPLE_RATE, 缓冲区: $bufferSize）")
    }

    /**
     * 停止音频采集并释放资源
     */
    fun stop() {
        isCapturing = false
        captureJob?.cancel()
        captureJob = null
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            AppLogger.w(TAG, "AudioRecord 停止异常", e)
        }
        audioRecord?.release()
        audioRecord = null
        AppLogger.d(TAG, "音频采集已停止")
    }

    /**
     * 释放所有资源
     */
    fun release() {
        stop()
        scope.cancel()
    }
}
