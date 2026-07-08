package com.livecompose.livecapture.core.video

import android.content.Context

/**
 * 慢动作视频录制器
 *
 * 对应 iOS 端 SlowMotionRecorder.swift，继承 [VideoRecorder]，
 * 以高帧率（120fps/240fps）录制，以 30fps 播放，实现 4x 或 8x 慢动作效果。
 *
 * ## 慢动作速度
 * - SPEED_4X: 录制 120fps，播放 30fps（4x 慢动作）
 * - SPEED_8X: 录制 240fps，播放 30fps（8x 慢动作）
 *
 * ## 实现原理
 * 1. 重写 [getVideoFrameRate] 配置高帧率录制
 * 2. 重写 [getVideoBitRate] 提高比特率以保持画质
 * 3. 通过帧率差异，播放时系统自动以较低帧率播放实现慢动作
 */
class SlowMotionRecorder(context: Context) : VideoRecorder(context) {

    /** 当前慢动作速度 */
    @Volatile
    private var speed: SlowMotionSpeed = SlowMotionSpeed.SPEED_4X

    /**
     * 开始慢动作录制
     *
     * @param quality 基础视频质量（用于分辨率）
     * @param speed 慢动作速度
     * @param filterEnabled 是否启用滤镜
     */
    @Throws(VideoRecorderError::class)
    fun startRecording(
        quality: VideoQuality,
        speed: SlowMotionSpeed,
        filterEnabled: Boolean = false
    ) {
        this.speed = speed
        startRecording(quality, VideoMode.SLOW_MOTION, filterEnabled)
    }

    /**
     * 慢动作录制比特率：高帧率需要更高比特率以保持画质
     */
    override fun getVideoBitRate(quality: VideoQuality, mode: VideoMode): Int {
        val baseBitRate = quality.bitRate
        return when (speed) {
            SlowMotionSpeed.SPEED_4X -> (baseBitRate * 2.5).toInt() // 120fps 需要约 2.5x 比特率
            SlowMotionSpeed.SPEED_8X -> baseBitRate * 4              // 240fps 需要约 4x 比特率
        }
    }

    /**
     * 慢动作录制帧率：使用高帧率录制
     */
    override fun getVideoFrameRate(quality: VideoQuality, mode: VideoMode): Int {
        return speed.recordFrameRate
    }
}
