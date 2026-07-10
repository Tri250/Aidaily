package com.livecompose.livecapture.core.motion

import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.util.TypedValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.lerp

@Singleton
class BoxCenterManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "BoxCenterManager"
        private const val ALIGNMENT_TOLERANCE_DP = 15f
        private const val MAGNETIC_SNAP_THRESHOLD_DP = 40f
        private const val LOCK_DURATION_MS = 800L
        private const val SNAP_LERP_FACTOR = 0.3f
    }

    private var referenceCenter: PointF? = null
    private var screenCenter: PointF = PointF(0.5f, 0.5f)
    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f
    private var isLocked = false
    private var lockTimestamp = 0L

    // 磁性吸附后的显示点（实际用于渲染）
    private var displayPoint: PointF? = null

    private val _trackPoint = MutableStateFlow<PointF?>(null)
    val trackPoint: StateFlow<PointF?> = _trackPoint

    private val _isAligned = MutableStateFlow(false)
    val isAligned: StateFlow<Boolean> = _isAligned

    private val _alignmentProgress = MutableStateFlow(0f)
    val alignmentProgress: StateFlow<Float> = _alignmentProgress

    // dp → px 换算
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    fun setScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        screenCenter = PointF(width / 2f, height / 2f)
    }

    fun updateScreenCenter(width: Float, height: Float) {
        setScreenSize(width, height)
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
        val offsetX = motionData.gyroY * 50f
        val offsetY = motionData.gyroX * 50f

        val ref = referenceCenter ?: return

        // 原始追踪点（不受吸附影响）
        val rawX = (ref.x + offsetX).coerceIn(0f, screenWidth)
        val rawY = (ref.y + offsetY).coerceIn(0f, screenHeight)
        val rawPoint = PointF(rawX, rawY)

        // 真正的磁性吸附: 当接近中心时，渐进式吸附到中心
        val snapThresholdPx = dpToPx(MAGNETIC_SNAP_THRESHOLD_DP)
        val dx = rawX - screenCenter.x
        val dy = rawY - screenCenter.y
        val distance = hypot(dx, dy)

        val finalPoint = if (distance < snapThresholdPx) {
            // LERP 插值: 越靠近中心，吸附力越强
            val snapStrength = 1f - (distance / snapThresholdPx)
            val snappedX = lerp(rawX, screenCenter.x, SNAP_LERP_FACTOR * snapStrength)
            val snappedY = lerp(rawY, screenCenter.y, SNAP_LERP_FACTOR * snapStrength)
            PointF(snappedX, snappedY)
        } else {
            rawPoint
        }

        displayPoint = finalPoint
        _trackPoint.value = finalPoint

        evaluateAlignment(finalPoint)
    }

    private fun evaluateAlignment(trackPoint: PointF) {
        val dx = abs(trackPoint.x - screenCenter.x)
        val dy = abs(trackPoint.y - screenCenter.y)
        val distance = hypot(dx, dy)

        val maxDistance = screenWidth * 0.5f
        val progress = 1f - (distance / maxDistance).coerceIn(0f, 1f)
        _alignmentProgress.value = progress

        val tolerancePx = dpToPx(ALIGNMENT_TOLERANCE_DP)
        val snapThresholdPx = dpToPx(MAGNETIC_SNAP_THRESHOLD_DP)
        val isWithinSnap = distance < snapThresholdPx

        if (isWithinSnap && !_isAligned.value) {
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
        displayPoint = null
        _trackPoint.value = null
        _isAligned.value = false
        _alignmentProgress.value = 0f
        isLocked = false
    }
}
