package com.livecompose.livecapture.core.video

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 视频录制视图模型
 *
 * 对应 iOS 端 VideoViewModel.swift，协调视频录制、防抖、滤镜等模块，
 * 管理录制状态机，为 UI 提供统一接口。
 *
 * ## 依赖模块
 * - [videoRecorder] 标准视频录制器
 * - [slowMotionRecorder] 慢动作录制器
 * - [timelapseRecorder] 延时摄影录制器
 * - [stabilizer] 电子防抖处理器
 * - [editor] 视频编辑器
 *
 * ## 模式切换
 * - 标准模式：使用 VideoRecorder
 * - 慢动作模式：使用 SlowMotionRecorder
 * - 延时摄影模式：使用 TimelapseRecorder（独立录制器）
 * - 电影模式：使用 VideoRecorder（含景深效果）
 */
class VideoViewModel(context: Context) : ViewModel() {

    companion object {
        private const val TAG = "VideoViewModel"
    }

    private val appContext = context.applicationContext

    /** 标准视频录制器 */
    val videoRecorder = VideoRecorder(appContext)
    /** 慢动作录制器 */
    val slowMotionRecorder = SlowMotionRecorder(appContext)
    /** 延时摄影录制器 */
    val timelapseRecorder = TimelapseRecorder(appContext, interval = 2.0)
    /** 电子防抖处理器 */
    val stabilizer = VideoStabilizer(appContext)
    /** 视频编辑器 */
    val editor = VideoEditor(appContext)

    // MARK: - UI 状态

    private val _recordingState = MutableStateFlow(VideoRecordingState())
    /** 录制状态 */
    val recordingState: StateFlow<VideoRecordingState> = _recordingState.asStateFlow()

    private val _selectedQuality = MutableStateFlow(VideoQuality.HD_1080P_30)
    /** 选中的视频质量 */
    val selectedQuality: StateFlow<VideoQuality> = _selectedQuality.asStateFlow()

    private val _selectedMode = MutableStateFlow(VideoMode.NORMAL)
    /** 选中的录制模式 */
    val selectedMode: StateFlow<VideoMode> = _selectedMode.asStateFlow()

    private val _stabilizationEnabled = MutableStateFlow(true)
    /** 是否启用防抖 */
    val stabilizationEnabled: StateFlow<Boolean> = _stabilizationEnabled.asStateFlow()

    private val _isSwitchingMode = MutableStateFlow(false)
    /** 是否正在切换模式 */
    val isSwitchingMode: StateFlow<Boolean> = _isSwitchingMode.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    /** 预览图像 */
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** 错误信息 */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 当前活跃的录制器（延时摄影除外） */
    private val activeRecorder: VideoRecorder?
        get() = when (_selectedMode.value) {
            VideoMode.NORMAL, VideoMode.CINEMATIC -> videoRecorder
            VideoMode.SLOW_MOTION -> slowMotionRecorder
            VideoMode.TIMELAPSE -> null
        }

    init {
        // 收集标准录制器状态
        viewModelScope.launch {
            videoRecorder.recordingState.collect { state ->
                if (_selectedMode.value == VideoMode.NORMAL || _selectedMode.value == VideoMode.CINEMATIC) {
                    _recordingState.value = state
                }
            }
        }
        viewModelScope.launch {
            videoRecorder.previewBitmap.collect { bitmap ->
                _previewBitmap.value = bitmap
            }
        }

        // 收集慢动作录制器状态
        viewModelScope.launch {
            slowMotionRecorder.recordingState.collect { state ->
                if (_selectedMode.value == VideoMode.SLOW_MOTION) {
                    _recordingState.value = state
                }
            }
        }
        viewModelScope.launch {
            slowMotionRecorder.previewBitmap.collect { bitmap ->
                _previewBitmap.value = bitmap
            }
        }

        // 收集延时摄影录制器状态
        viewModelScope.launch {
            timelapseRecorder.isRecording.collect { isRecording ->
                if (_selectedMode.value == VideoMode.TIMELAPSE) {
                    _recordingState.update { it.copy(isRecording = isRecording) }
                }
            }
        }
        viewModelScope.launch {
            timelapseRecorder.elapsedTime.collect { time ->
                if (_selectedMode.value == VideoMode.TIMELAPSE) {
                    _recordingState.update { it.copy(duration = time) }
                }
            }
        }
        viewModelScope.launch {
            timelapseRecorder.previewBitmap.collect { bitmap ->
                _previewBitmap.value = bitmap
            }
        }
    }

    // MARK: - 开始录制

    /**
     * 开始录制视频
     */
    fun startRecording() {
        if (_recordingState.value.isRecording) return

        try {
            when (_selectedMode.value) {
                VideoMode.NORMAL, VideoMode.CINEMATIC -> {
                    videoRecorder.startRecording(
                        quality = _selectedQuality.value,
                        mode = _selectedMode.value
                    )
                    if (_stabilizationEnabled.value) {
                        stabilizer.startStabilization()
                    }
                }

                VideoMode.SLOW_MOTION -> {
                    slowMotionRecorder.startRecording(
                        quality = _selectedQuality.value,
                        speed = SlowMotionSpeed.SPEED_4X
                    )
                    if (_stabilizationEnabled.value) {
                        stabilizer.startStabilization()
                    }
                }

                VideoMode.TIMELAPSE -> {
                    timelapseRecorder.startRecording(
                        quality = _selectedQuality.value
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "录制启动失败", e)
            showError("录制启动失败: ${e.message}")
        }
    }

    // MARK: - 停止录制

    /**
     * 停止录制
     */
    fun stopRecording() {
        if (!_recordingState.value.isRecording) return

        when (_selectedMode.value) {
            VideoMode.NORMAL, VideoMode.CINEMATIC, VideoMode.SLOW_MOTION -> {
                val recorder = activeRecorder ?: return
                if (_stabilizationEnabled.value) {
                    stabilizer.stopStabilization()
                }
                recorder.stopRecording { _, error ->
                    if (error != null) {
                        showError("录制完成但保存失败: ${error.message}")
                    }
                }
            }

            VideoMode.TIMELAPSE -> {
                timelapseRecorder.stopRecording { path ->
                    AppLogger.i(TAG, "延时摄影完成: $path")
                }
            }
        }
    }

    // MARK: - 模式切换

    /**
     * 切换录制模式
     * @param mode 目标模式
     */
    fun switchMode(mode: VideoMode) {
        if (mode == _selectedMode.value || _recordingState.value.isRecording) return

        _isSwitchingMode.value = true
        _selectedMode.value = mode

        // 延时摄影模式不需要防抖
        if (mode == VideoMode.TIMELAPSE) {
            _stabilizationEnabled.value = false
        }

        viewModelScope.launch {
            // 短暂延迟模拟模式切换动画
            kotlinx.coroutines.delay(300)
            _isSwitchingMode.value = false
        }
    }

    // MARK: - 帧处理

    /**
     * 处理来自相机的视频帧（YUV 数据）
     *
     * @param yuvData YUV_420_888 字节数据
     * @param width 帧宽度
     * @param height 帧高度
     * @param timestampUs 时间戳（微秒）
     */
    fun processFrame(yuvData: ByteArray, width: Int, height: Int, timestampUs: Long) {
        if (!_recordingState.value.isRecording) return

        when (_selectedMode.value) {
            VideoMode.TIMELAPSE -> {
                // 延时摄影：将帧转为 Bitmap 传递给 TimelapseRecorder
                // 实际场景中由 CameraX ImageAnalysis 提供 Bitmap
            }
            else -> {
                activeRecorder?.encodeFrame(yuvData, width, height, timestampUs)
            }
        }
    }

    /**
     * 处理来自相机的视频帧（Bitmap）
     *
     * @param bitmap 当前帧 Bitmap
     */
    fun processFrame(bitmap: Bitmap) {
        if (!_recordingState.value.isRecording) return

        // 延时摄影模式：传递给 TimelapseRecorder
        if (_selectedMode.value == VideoMode.TIMELAPSE) {
            timelapseRecorder.captureFrame(bitmap)
        }
    }

    /**
     * 处理音频帧
     *
     * @param pcmData PCM 字节数据
     * @param timestampUs 时间戳（微秒）
     */
    fun processAudioFrame(pcmData: ByteArray, timestampUs: Long) {
        if (!_recordingState.value.isRecording || _selectedMode.value == VideoMode.TIMELAPSE) return
        activeRecorder?.encodeAudioFrame(pcmData, timestampUs)
    }

    // MARK: - 录制中拍照

    /**
     * 在录制过程中拍照
     *
     * @param yuvData 最近的 YUV 数据
     * @param width 帧宽
     * @param height 帧高
     * @return JPEG 字节数据
     */
    fun capturePhoto(yuvData: ByteArray, width: Int, height: Int): ByteArray? {
        if (!_recordingState.value.isRecording) return null
        return activeRecorder?.capturePhotoDuringRecording(yuvData, width, height)
    }

    // MARK: - 暂停/恢复

    /** 暂停录制 */
    fun pauseRecording() {
        if (_selectedMode.value == VideoMode.TIMELAPSE) return
        activeRecorder?.pauseRecording()
    }

    /** 恢复录制 */
    fun resumeRecording() {
        if (_selectedMode.value == VideoMode.TIMELAPSE) return
        activeRecorder?.resumeRecording()
    }

    // MARK: - 防抖控制

    /** 防抖是否可用（延时摄影不可用） */
    val canUseStabilization: Boolean
        get() = _selectedMode.value != VideoMode.TIMELAPSE

    /** 切换防抖开关 */
    fun toggleStabilization() {
        if (!canUseStabilization) return
        _stabilizationEnabled.value = !_stabilizationEnabled.value

        if (_recordingState.value.isRecording) {
            if (_stabilizationEnabled.value) {
                stabilizer.startStabilization()
            } else {
                stabilizer.stopStabilization()
            }
        }
    }

    // MARK: - 辅助方法

    /** 获取当前录制时长（格式化） */
    val formattedDuration: String
        get() = _recordingState.value.formattedDuration

    /** 获取当前文件大小（格式化） */
    val formattedFileSize: String
        get() = _recordingState.value.formattedFileSize

    /** 延时摄影加速比 */
    val timelapseSpeedupRatio: String
        get() {
            if (_selectedMode.value != VideoMode.TIMELAPSE) return ""
            return String.format("%.0fx", timelapseRecorder.speedupRatio)
        }

    /** 延时摄影预估时长 */
    val timelapseEstimatedDuration: String
        get() = timelapseRecorder.formattedEstimatedDuration

    /** 延时摄影已录制时间 */
    val timelapseElapsedTime: String
        get() = timelapseRecorder.formattedElapsedTime

    /** 延时摄影帧数 */
    val timelapseFrameCount: Int
        get() = timelapseRecorder.frameCount.value

    /** 是否正在录制延时摄影 */
    val isRecordingTimelapse: Boolean
        get() = _selectedMode.value == VideoMode.TIMELAPSE && _recordingState.value.isRecording

    // MARK: - 错误处理

    /** 显示错误提示 */
    private fun showError(message: String) {
        _errorMessage.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _errorMessage.value = null
        }
    }

    /** 清除错误信息 */
    fun clearError() {
        _errorMessage.value = null
    }

    // MARK: - 资源清理

    override fun onCleared() {
        super.onCleared()
        try {
            stabilizer.stopStabilization()
            videoRecorder.destroy()
            slowMotionRecorder.destroy()
            timelapseRecorder.destroy()
            editor.destroy()
        } catch (e: Exception) {
            AppLogger.w(TAG, "VideoViewModel 资源清理异常", e)
        }
    }
}
