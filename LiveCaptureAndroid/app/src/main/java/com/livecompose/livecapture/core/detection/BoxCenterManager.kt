package com.livecompose.livecapture.core.detection

import android.graphics.PointF
import android.graphics.RectF
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 检测框中心点追踪管理器
 */
class BoxCenterManager {

    private val _baseCenterInView = MutableStateFlow<PointF?>(null)
    val baseCenterInView: StateFlow<PointF?> = _baseCenterInView.asStateFlow()

    private val _currentCenterInView = MutableStateFlow<PointF?>(null)
    val currentCenterInView: StateFlow<PointF?> = _currentCenterInView.asStateFlow()

    private var rawTrackingPosition: PointF? = null
    private var compositionRect: RectF = RectF()
    private var referenceAttitude: FloatArray? = null
    private val maxAngle = Math.PI / 6.0
    private var offsetSmoother = AdaptivePointSmoother(baseResponse = 0.20)
    private var currentZoomFactor = 1.0f
    private var isFrontCamera = false
    private var lastAngularVelocity = PointF(0f, 0f)
    private val velocityHistory = mutableListOf<PointF>()
    private val maxVelocityHistoryCount = 5
    private val magneticThreshold = 25.0f
    private val magneticStrength = 0.90f
    private val snapThreshold = 5.0f
    private var alignedStartTime: Long? = null
    private val lockDuration = 1000L // 1 second
    private var isLockedToCenter = false

    fun updateCompositionRect(rect: RectF) {
        compositionRect = rect
    }

    fun updateZoomFactor(factor: Float) {
        currentZoomFactor = max(0.5f, factor)
    }

    fun setFrontCamera(isFront: Boolean) {
        isFrontCamera = isFront
    }

    fun setBaseCenter(center: PointF?, attitude: FloatArray?) {
        _baseCenterInView.value = center
        _currentCenterInView.value = center
        referenceAttitude = attitude?.clone()
        offsetSmoother.reset()
        velocityHistory.clear()
        lastAngularVelocity = PointF(0f, 0f)
        resetLockState()
    }

    fun reset() {
        _baseCenterInView.value = null
        _currentCenterInView.value = null
        rawTrackingPosition = null
        referenceAttitude = null
        offsetSmoother.reset()
        currentZoomFactor = 1.0f
        velocityHistory.clear()
        lastAngularVelocity = PointF(0f, 0f)
        resetLockState()
    }

    private fun resetLockState() {
        alignedStartTime = null
        isLockedToCenter = false
    }

    fun updateCenter(motion: FloatArray?) {
        if (motion == null) return
        val reference = referenceAttitude ?: return

        val currentAttitude = motion
        val deltaPitch = currentAttitude[1] - reference[1] // pitch
        val deltaRoll = currentAttitude[2] - reference[2]   // roll

        val clampedPitch = max(-maxAngle, min(maxAngle, deltaPitch.toDouble()))
        val clampedRoll = max(-maxAngle, min(maxAngle, deltaRoll.toDouble()))

        val rollForOffset = clampedRoll
        val pitchForOffset = if (isFrontCamera) -clampedPitch else clampedPitch
        val offsetX = (rollForOffset / maxAngle).toFloat()
        val offsetY = (pitchForOffset / maxAngle).toFloat()

        val rotationRateY = (currentAttitude[1] - reference[1]).toFloat()
        val rotationRateX = (currentAttitude[2] - reference[2]).toFloat()
        val angularVelocity = PointF(rotationRateX, rotationRateY)
        updateVelocityHistory(angularVelocity)

        val speed = sqrt(angularVelocity.x.pow(2) + angularVelocity.y.pow(2))
        offsetSmoother.updateResponse(speed.toDouble())

        val smoothed = offsetSmoother.filter(offsetX.toDouble(), offsetY.toDouble())
        updateCenterWithOffset(smoothed.first.toFloat(), smoothed.second.toFloat())
        lastAngularVelocity = angularVelocity
    }

    private fun updateCenterWithOffset(offsetX: Float, offsetY: Float) {
        val base = _baseCenterInView.value ?: return
        if (compositionRect.isEmpty) return

        val screenCenterX = compositionRect.centerX()
        val screenCenterY = compositionRect.centerY()

        if (isLockedToCenter) {
            rawTrackingPosition = PointF(screenCenterX, screenCenterY)
            _currentCenterInView.value = PointF(screenCenterX, screenCenterY)
            return
        }

        val baseVectorX = base.x - screenCenterX
        val baseVectorY = base.y - screenCenterY
        val screenRadius = sqrt((compositionRect.width() / 2).pow(2) + (compositionRect.height() / 2).pow(2))
        val distanceToCenter = sqrt(baseVectorX.pow(2) + baseVectorY.pow(2))
        val normalizedDistance = distanceToCenter / screenRadius

        val baseGainX = compositionRect.width() * 0.55f
        val baseGainY = compositionRect.height() * 0.55f
        val distanceGain = 0.6f + normalizedDistance * 0.8f
        val zoomGain = 1.0f + (currentZoomFactor - 1.0f) * 0.35f

        val adaptiveGainX = baseGainX * distanceGain * zoomGain
        val adaptiveGainY = baseGainY * distanceGain * zoomGain

        val displacementX = offsetX * adaptiveGainX
        val displacementY = offsetY * adaptiveGainY

        val velocityCompensation = calculateVelocityCompensation()

        val rawTargetX = base.x + displacementX + velocityCompensation.first
        val rawTargetY = base.y + displacementY + velocityCompensation.second

        rawTrackingPosition = clampPoint(rawTargetX, rawTargetY)

        val snappedTarget = applyMagneticSnap(rawTargetX, rawTargetY)
        _currentCenterInView.value = clampPoint(snappedTarget.first, snappedTarget.second)
    }

    private fun applyMagneticSnap(x: Float, y: Float): Pair<Float, Float> {
        if (compositionRect.isEmpty) return x to y
        val centerX = compositionRect.centerX()
        val centerY = compositionRect.centerY()
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt(dx.pow(2) + dy.pow(2))

        if (distance < snapThreshold) return centerX to centerY

        if (distance < magneticThreshold) {
            val normalized = ((distance - snapThreshold) / (magneticThreshold - snapThreshold)).coerceIn(0f, 1f)
            val easeFactor = 1f - normalized.pow(0.5f)
            val attractionStrength = easeFactor * magneticStrength
            return (x - dx * attractionStrength) to (y - dy * attractionStrength)
        }
        return x to y
    }

    private fun calculateVelocityCompensation(): Pair<Float, Float> {
        if (velocityHistory.size < 3) return 0f to 0f
        val avgVelocity = velocityHistory.fold(PointF(0f, 0f)) { acc, v ->
            PointF(acc.x + v.x, acc.y + v.y)
        }
        val count = velocityHistory.size.toFloat()
        val normalizedVelocity = PointF(avgVelocity.x / count, avgVelocity.y / count)
        val compensationFactor = 0.04f * (1.0f / offsetSmoother.currentResponse.toFloat())
        return (normalizedVelocity.x * compensationFactor * compositionRect.width()) to
                (normalizedVelocity.y * compensationFactor * compositionRect.height())
    }

    private fun updateVelocityHistory(velocity: PointF) {
        velocityHistory.add(velocity)
        if (velocityHistory.size > maxVelocityHistoryCount) {
            velocityHistory.removeFirst()
        }
    }

    private fun clampPoint(x: Float, y: Float): PointF {
        return PointF(
            x.coerceIn(compositionRect.left, compositionRect.right),
            y.coerceIn(compositionRect.top, compositionRect.bottom)
        )
    }

    fun isAlignedWithCenter(tolerance: Float = 5.0f): Boolean {
        val rawPosition = rawTrackingPosition ?: return false.also { resetLockState() }
        if (compositionRect.isEmpty) return false

        val centerX = compositionRect.centerX()
        val centerY = compositionRect.centerY()
        val dx = rawPosition.x - centerX
        val dy = rawPosition.y - centerY
        val distance = sqrt(dx.pow(2) + dy.pow(2))
        val isAligned = distance <= tolerance

        if (isAligned) {
            val startTime = alignedStartTime
            if (startTime == null) {
                alignedStartTime = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - startTime >= lockDuration && !isLockedToCenter) {
                isLockedToCenter = true
            }
        } else {
            resetLockState()
        }
        return isAligned
    }

    fun distanceToCenter(): Float? {
        val rawPosition = rawTrackingPosition ?: return null
        if (compositionRect.isEmpty) return null
        val centerX = compositionRect.centerX()
        val centerY = compositionRect.centerY()
        val dx = rawPosition.x - centerX
        val dy = rawPosition.y - centerY
        return sqrt(dx.pow(2) + dy.pow(2))
    }
}

/**
 * 自适应点平滑器
 */
class AdaptivePointSmoother(private val baseResponse: Double) {
    var currentResponse: Double = baseResponse
        private set
    private var previousX: Double? = null
    private var previousY: Double? = null

    private val lowSpeedThreshold = 0.15
    private val highSpeedThreshold = 3.0
    private val minResponse = 0.12
    private val maxResponse = 0.22

    fun updateResponse(speed: Double) {
        currentResponse = when {
            speed < lowSpeedThreshold -> maxResponse
            speed > highSpeedThreshold -> minResponse
            else -> {
                val t = (speed - lowSpeedThreshold) / (highSpeedThreshold - lowSpeedThreshold)
                maxResponse - t * (maxResponse - minResponse)
            }
        }
    }

    fun filter(x: Double, y: Double): Pair<Double, Double> {
        val prevX = previousX
        val prevY = previousY
        if (prevX == null || prevY == null) {
            previousX = x
            previousY = y
            return x to y
        }
        val t = currentResponse
        val filteredX = prevX + t * (x - prevX)
        val filteredY = prevY + t * (y - prevY)
        previousX = filteredX
        previousY = filteredY
        return filteredX to filteredY
    }

    fun reset() {
        previousX = null
        previousY = null
        currentResponse = baseResponse
    }
}