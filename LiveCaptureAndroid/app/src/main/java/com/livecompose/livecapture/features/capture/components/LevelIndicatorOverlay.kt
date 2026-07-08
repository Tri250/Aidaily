package com.livecompose.livecapture.features.capture.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext

private const val LEVEL_THRESHOLD_DEGREES = 3.0f

/**
 * 水平仪叠加层
 * 利用加速度计检测设备倾斜角度，当水平偏差 < 3° 时线条变绿
 */
@Composable
fun LevelIndicatorOverlay(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current

    var targetRotation by remember { mutableFloatStateOf(0f) }
    var isLevel by remember { mutableStateOf(false) }

    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 200),
        label = "rotation"
    )

    val lineColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isLevel -> Color(0xFF00C853)
            else -> Color.White.copy(alpha = 0.6f)
        },
        label = "lineColor"
    )

    // 传感器监听
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return@DisposableEffect onDispose { }

        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values ?: return
                val x = event.values[0]
                val y = event.values[1]
                // 计算倾斜角度 (横屏时使用 x 轴，竖屏时使用 y 轴)
                targetRotation = Math.toDegrees(kotlin.math.atan2(x.toDouble(), y.toDouble())).toFloat()
                isLevel = kotlin.math.abs(targetRotation) < LEVEL_THRESHOLD_DEGREES
            }
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose { sensorManager.unregisterListener(listener) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (!enabled || lineColor.alpha == 0f) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2

        withTransform(
            transformBlock = {
                translate(left = centerX, top = centerY)
                rotate(degrees = -animatedRotation)
            },
            drawBlock = {
                // 主水平线
                drawLine(
                    color = lineColor,
                    start = Offset(-80f, 0f),
                    end = Offset(80f, 0f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // 左侧刻度线
                drawLine(
                    color = lineColor.copy(alpha = 0.7f),
                    start = Offset(-70f, 0f),
                    end = Offset(-70f, -8f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
                // 右侧刻度线
                drawLine(
                    color = lineColor.copy(alpha = 0.7f),
                    start = Offset(70f, 0f),
                    end = Offset(70f, -8f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )

                // 水平状态指示点
                if (isLevel) {
                    drawCircle(color = Color(0xFF00C853), radius = 4f, center = Offset(0f, 0f))
                }
            }
        )
    }
}
