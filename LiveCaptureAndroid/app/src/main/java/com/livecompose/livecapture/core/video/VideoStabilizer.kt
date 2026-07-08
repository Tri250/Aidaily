package com.livecompose.livecapture.core.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.livecompose.livecapture.core.logger.AppLogger
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 电子防抖处理器 - 使用陀螺仪/旋转矢量传感器数据补偿设备抖动
 *
 * 对应 iOS 端 VideoStabilizer.swift，使用 Android 原生 SensorManager
 * 替代 CoreMotion，使用 Matrix + Canvas 替代 CIImage 仿射变换。
 *
 * ## 工作原理
 * 1. 启动时记录参考姿态（旋转矢量）
 * 2. 每帧获取当前设备姿态
 * 3. 计算当前姿态与参考姿态的差异（roll/pitch/yaw）
 * 4. 应用低通滤波平滑减少高频抖动
 * 5. 生成反向仿射变换补偿抖动（使用 Matrix）
 * 6. 对 Bitmap 应用变换和裁剪消除黑边
 *
 * ## 姿态映射
 * - roll（翻滚）→ 旋转补偿
 * - pitch（俯仰）→ Y 轴平移
 * - yaw（偏航）→ X 轴平移
 *
 * ## 平滑参数
 * - smoothingFactor: 0.8（低通滤波系数）
 * - maxRotation: ±5°（最大旋转补偿）
 * - maxTranslation: 图像尺寸的 5%（最大平移补偿）
 * - cropMargin: 10%（边缘裁剪比例，消除黑边）
 */
class VideoStabilizer(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "VideoStabilizer"
        /** 平滑系数（0-1），值越大平滑越强但响应越慢 */
        private const val SMOOTHING_FACTOR = 0.8f
        /** 最大旋转角度（弧度，约 ±5°） */
        private const val MAX_ROTATION_RAD = 5.0 * Math.PI / 180.0
        /** 最大平移比例（相对于图像尺寸） */
        private const val MAX_TRANSLATION_RATIO = 0.05f
        /** 裁剪边距比例（消除黑边） */
        private const val CROP_MARGIN = 0.10f
        /** 稳定阈值（0.5°） */
        private val STABLE_THRESHOLD_RAD = 0.5 * Math.PI / 180.0
    }

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    /** 参考姿态（录制开始时的设备姿态，弧度） */
    private var referenceRoll: Double = 0.0
    private var referencePitch: Double = 0.0
    private var referenceYaw: Double = 0.0
    private var hasReference: Boolean = false

    /** 当前平滑后的姿态差值（弧度） */
    private var smoothedRoll: Double = 0.0
    private var smoothedPitch: Double = 0.0
    private var smoothedYaw: Double = 0.0

    /** 当前原始姿态差值（用于稳定性判断） */
    @Volatile
    private var currentRoll: Double = 0.0
    @Volatile
    private var currentPitch: Double = 0.0
    @Volatile
    private var currentYaw: Double = 0.0

    /** 当前平滑后的变换矩阵 */
    private val smoothedMatrix = Matrix()
    private var lastMatrix = Matrix()

    private var isRunning = false

    /**
     * 启动防抖（开始采集传感器数据）
     */
    fun startStabilization() {
        if (isRunning || rotationSensor == null) {
            if (rotationSensor == null) {
                AppLogger.w(TAG, "设备不支持旋转矢量传感器，无法启用防抖")
            }
            return
        }
        sensorManager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        isRunning = true
        resetReference()
    }

    /**
     * 停止防抖
     */
    fun stopStabilization() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
        resetReference()
    }

    /**
     * 重置参考姿态（用于切换场景）
     */
    fun resetReference() {
        hasReference = false
        referenceRoll = 0.0
        referencePitch = 0.0
        referenceYaw = 0.0
        smoothedRoll = 0.0
        smoothedPitch = 0.0
        smoothedYaw = 0.0
        currentRoll = 0.0
        currentPitch = 0.0
        currentYaw = 0.0
        smoothedMatrix.reset()
        lastMatrix.reset()
    }

    /**
     * 对 Bitmap 应用防抖变换
     *
     * @param bitmap 输入帧
     * @return 防抖后的 Bitmap，失败返回原 Bitmap
     */
    fun stabilizeFrame(bitmap: Bitmap): Bitmap {
        if (!isRunning || !hasReference) {
            return bitmap
        }

        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap

        // 1. 限制旋转角度
        val clampedRoll = currentRoll.coerceIn(-MAX_ROTATION_RAD, MAX_ROTATION_RAD)
        val clampedPitch = currentPitch.coerceIn(-MAX_ROTATION_RAD, MAX_ROTATION_RAD)
        val clampedYaw = currentYaw.coerceIn(-MAX_ROTATION_RAD, MAX_ROTATION_RAD)

        // 2. 计算平移补偿
        val maxTranslationX = width * MAX_TRANSLATION_RATIO
        val maxTranslationY = height * MAX_TRANSLATION_RATIO

        val tx = (clampedYaw / MAX_ROTATION_RAD * maxTranslationX)
            .coerceIn(-maxTranslationX.toDouble(), maxTranslationX.toDouble()).toFloat()
        val ty = (clampedPitch / MAX_ROTATION_RAD * maxTranslationY)
            .coerceIn(-maxTranslationY.toDouble(), maxTranslationY.toDouble()).toFloat()

        // 3. 构建防抖变换矩阵（反向补偿）
        val targetMatrix = Matrix()
        val imageCenterX = width / 2f
        val imageCenterY = height / 2f

        // 移动到中心 → 反向旋转 → 反向平移 → 移回原点 → 放大（消除黑边）
        targetMatrix.postTranslate(-imageCenterX, -imageCenterY)
        targetMatrix.postRotate((-clampedRoll).toFloat())
        targetMatrix.postTranslate(-tx, -ty)
        targetMatrix.postTranslate(imageCenterX, imageCenterY)

        val scale = 1.0f + CROP_MARGIN * 2
        targetMatrix.postScale(scale, scale, imageCenterX, imageCenterY)

        // 4. 平滑变换矩阵（线性插值）
        smoothedMatrix.set(lerpMatrix(lastMatrix, targetMatrix, 1.0f - SMOOTHING_FACTOR))
        lastMatrix.set(smoothedMatrix)

        // 5. 应用变换到 Bitmap
        return applyMatrixToBitmap(bitmap, smoothedMatrix, width, height)
    }

    /**
     * 当前设备是否稳定（旋转幅度很小）
     */
    val isCurrentlyStable: Boolean
        get() {
            if (!hasReference) return true
            return abs(currentRoll) < STABLE_THRESHOLD_RAD &&
                abs(currentPitch) < STABLE_THRESHOLD_RAD &&
                abs(currentYaw) < STABLE_THRESHOLD_RAD
        }

    /**
     * 当前旋转幅度（弧度）
     */
    val currentRotationMagnitude: Double
        get() = sqrt(
            currentRoll * currentRoll +
                currentPitch * currentPitch +
                currentYaw * currentYaw
        )

    // MARK: - SensorEventListener

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_ROTATION_VECTOR
        ) return

        val values = event.values
        if (values.size < 4) return

        // 将旋转矢量转为旋转矩阵，再转为姿态角
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        // orientation[0]=azimuth(yaw), [1]=pitch, [2]=roll（弧度）
        val yaw = orientation[0].toDouble()
        val pitch = orientation[1].toDouble()
        val roll = orientation[2].toDouble()

        // 首次设置参考姿态
        if (!hasReference) {
            referenceRoll = roll
            referencePitch = pitch
            referenceYaw = yaw
            hasReference = true
            smoothedRoll = 0.0
            smoothedPitch = 0.0
            smoothedYaw = 0.0
            return
        }

        // 计算姿态差值
        val deltaRoll = normalizeAngle(roll - referenceRoll)
        val deltaPitch = normalizeAngle(pitch - referencePitch)
        val deltaYaw = normalizeAngle(yaw - referenceYaw)

        // 应用低通滤波平滑
        smoothedRoll = lowPassFilter(deltaRoll, smoothedRoll)
        smoothedPitch = lowPassFilter(deltaPitch, smoothedPitch)
        smoothedYaw = lowPassFilter(deltaYaw, smoothedYaw)

        currentRoll = smoothedRoll
        currentPitch = smoothedPitch
        currentYaw = smoothedYaw
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不处理
    }

    // MARK: - 私有方法

    /** 低通滤波器 */
    private fun lowPassFilter(current: Double, previous: Double): Double {
        return previous + SMOOTHING_FACTOR * (current - previous)
    }

    /** 角度归一化到 [-π, π] */
    private fun normalizeAngle(angle: Double): Double {
        var a = angle
        while (a > Math.PI) a -= 2 * Math.PI
        while (a < -Math.PI) a += 2 * Math.PI
        return a
    }

    /** 线性插值两个 Matrix */
    private fun lerpMatrix(from: Matrix, to: Matrix, t: Float): Matrix {
        val fromValues = FloatArray(9)
        val toValues = FloatArray(9)
        from.getValues(fromValues)
        to.getValues(toValues)

        val resultValues = FloatArray(9)
        for (i in 0 until 9) {
            resultValues[i] = fromValues[i] + (toValues[i] - fromValues[i]) * t
        }

        return Matrix().apply { setValues(resultValues) }
    }

    /** 对 Bitmap 应用变换矩阵，并裁剪回原始尺寸 */
    private fun applyMatrixToBitmap(source: Bitmap, matrix: Matrix, width: Int, height: Int): Bitmap {
        return try {
            // 创建放大后的图像，避免边缘黑边
            val scaledWidth = (width * (1.0f + CROP_MARGIN * 2)).toInt()
            val scaledHeight = (height * (1.0f + CROP_MARGIN * 2)).toInt()
            val output = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(source, matrix, paint)

            // 裁剪回原始尺寸（居中裁剪，去除黑边）
            val startX = ((scaledWidth - width) / 2).coerceAtLeast(0)
            val startY = ((scaledHeight - height) / 2).coerceAtLeast(0)
            Bitmap.createBitmap(output, startX, startY, width, height)
        } catch (e: Exception) {
            AppLogger.w(TAG, "防抖变换失败，返回原图", e)
            source
        }
    }
}
