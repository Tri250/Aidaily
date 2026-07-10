package com.livecompose.livecapture.features.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.livecompose.livecapture.ui.design.DesignSystem

private val Context.dataStore by preferencesDataStore(name = "onboarding_prefs")
private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

/**
 * 检查是否已完成引导
 */
suspend fun isOnboardingCompleted(context: android.content.Context): Boolean {
    return context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }.first()
}

/**
 * 标记引导已完成
 */
suspend fun setOnboardingCompleted(context: android.content.Context) {
    context.dataStore.edit { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] = true
    }
}

/**
 * 启动引导页 - 秒简相机风格
 *
 * 设计语言：
 * - 纯黑沉浸背景
 * - 青绿发光圆环装饰（参考秒简相机开屏）
 * - 大号标题 + 纤细副标题
 * - 右下角圆形主按钮
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 背景发光圆环装饰
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.35f, size.height * 0.35f)
            val baseRadius = size.minDimension * 0.55f

            // 外层大圆环
            drawArc(
                color = DesignSystem.Colors.primary.copy(alpha = glowAlpha * 0.55f),
                startAngle = ringRotation,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                size = androidx.compose.ui.geometry.Size(baseRadius * 2, baseRadius * 2),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // 中层圆环
            drawArc(
                color = DesignSystem.Colors.secondary.copy(alpha = glowAlpha * 0.40f),
                startAngle = ringRotation + 120f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(
                    center.x - baseRadius * 0.72f,
                    center.y - baseRadius * 0.72f
                ),
                size = androidx.compose.ui.geometry.Size(
                    baseRadius * 1.44f,
                    baseRadius * 1.44f
                ),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // 内层小圆环
            drawArc(
                color = DesignSystem.Colors.accent.copy(alpha = glowAlpha * 0.35f),
                startAngle = -ringRotation,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(
                    center.x - baseRadius * 0.42f,
                    center.y - baseRadius * 0.42f
                ),
                size = androidx.compose.ui.geometry.Size(
                    baseRadius * 0.84f,
                    baseRadius * 0.84f
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 品牌图标
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DesignSystem.Colors.primary.copy(alpha = 0.25f),
                                DesignSystem.Colors.primary.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = DesignSystem.Colors.primary.copy(alpha = 0.85f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DesignSystem.Colors.primary)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "精彩一拍即合",
                style = DesignSystem.Typography.title1,
                color = Color.White,
                fontSize = 36.sp,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "告别反复修图，\n按下快门就能得到满意的影像。",
                style = DesignSystem.Typography.callout,
                color = DesignSystem.Colors.minimalLabelSecondary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(56.dp))

            // 主按钮：右下角浮动
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            setOnboardingCompleted(context)
                            onComplete()
                        }
                    },
                    shape = CircleShape,
                    containerColor = DesignSystem.Colors.primary,
                    contentColor = Color.Black,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "开始",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
