package com.livecompose.livecapture.core.motion

import android.graphics.PointF
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot

@Singleton
class BoxCenterManager @Inject constructor() {

    companion object {
        private const val TAG = "BoxCenterManager"
        private const val ALIGNMENT_TOLERANCE_DP = 15f
        private const val MAGNETIC_SNAP_THRESHOLD_DP = 20f
        private const val LOCK_DURATION_MS = 1000L
    }

    private var referenceCenter: PointF? = null
    private var screenCenter: PointF = PointF(0.5f, 0.5f)
    private var isLocked = false
    private var lockTimestamp = 0L

    private val _trackPoint = MutableStateFlow<PointF?>(null)
    val trackPoint: StateFlow<PointF?> = _trackPoint

    private val _isAligned = MutableStateFlow(false)
    val isAligned: StateFlow<Boolean> = _isAligned

    private val _alignmentProgress = MutableStateFlow(0f)
    val alignmentProgress: StateFlow<Float> = _alignmentProgress

    fun setScreenSize(width: Float, height: Float) {
        screenCenter = PointF(width / 2f, height / 2f)
    }

    fun updateFromDetection(
        bboxCenterX: Float,
        bboxCenterY: Float,
        motionData: MotionStabilityMonitor.MotionData
    ) {
        // 记录基准中心（首次检测结果）
        if (referenceCenter == null) {
            referenceCenter = PointF(bboxCenterX, bboxCenterY)
            Log.d(TAG, "Reference center set: ($bboxCenterX, $bboxCenterY)")
        }

        // 计算姿态变化带来的屏幕坐标偏移
        val offsetX = motionData.gyroY * 50f  // 绕Y轴旋转影响水平偏移
        val offsetY = motionData.gyroX * 50f  // 绕X轴旋转影响垂直偏移

        val ref = referenceCenter ?: return

        // 更新追踪点
        val newTrackX = (ref.x + offsetX).coerceIn(0f, screenCenter.x * 2f)
        val newTrackY = (ref.y + offsetY).coerceIn(0f, screenCenter.y * 2f)
        val newTrackPoint = PointF(newTrackX, newTrackY)

        _trackPoint.value = newTrackPoint

        // 对齐判断
        evaluateAlignment(newTrackPoint)
    }

    private fun evaluateAlignment(trackPoint: PointF) {
        val dx = abs(trackPoint.x - screenCenter.x)
        val dy = abs(trackPoint.y - screenCenter.y)
        val distance = hypot(dx, dy)

        // 计算对齐进度 (0.0 ~ 1.0)
        val progress = 1f - (distance / (screenCenter.x * 0.5f)).coerceIn(0f, 1f)
        _alignmentProgress.value = progress

        // 磁性吸附效果
        val isWithinSnap = distance < MAGNETIC_SNAP_THRESHOLD_DP

        if (isWithinSnap && !_isAligned.value) {
            // 进入对齐锁定
            if (!isLocked) {
                isLocked = true
                lockTimestamp = System.currentTimeMillis()
            }
            if (System.currentTimeMillis() - lockTimestamp >= LOCK_DURATION_MS) {
                _isAligned.value = true
            }
        } else if (!isWithinSnap) {
            isLocked = false
            _isAligned.value = false
        }
    }

    fun reset() {
        referenceCenter = null
        _trackPoint.value = null
        _isAligned.value = false
        _alignmentProgress.value = 0f
        isLocked = false
    }

    fun updateScreenCenter(width: Float, height: Float) {
        screenCenter = PointF(width / 2f, height / 2f)
    }
}
