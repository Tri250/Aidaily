package com.livecompose.livecapture.features.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

private val Context.dataStore by preferencesDataStore(name = "onboarding_prefs")

private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

/**
 * 引导页数据
 */
data class OnboardingPageData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val gradientColors: List<Color>
)

private val onboardingPages = listOf(
    OnboardingPageData(
        icon = Icons.Default.AutoAwesome,
        title = "AI 智能构图",
        description = "让 AI 为你找到最佳构图角度",
        gradientColors = listOf(Color(0xFF007AFF), Color(0xFF5856D6))
    ),
    OnboardingPageData(
        icon = Icons.Default.ColorLens,
        title = "LUT 色彩预设",
        description = "一键应用专业胶片色彩",
        gradientColors = listOf(Color(0xFFFF6B35), Color(0xFFFF2D55))
    ),
    OnboardingPageData(
        icon = Icons.Default.Brush,
        title = "相框水印",
        description = "为作品添加个性边框和水印",
        gradientColors = listOf(Color(0xFF34C759), Color(0xFF30D158))
    )
)

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
 * 启动引导页
 * 使用 HorizontalPager 实现 3 页引导
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 跳过按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            setOnboardingCompleted(context)
                            onComplete()
                        }
                    }
                ) {
                    Text(
                        "跳过",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
            }

            // 引导页内容
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pageData = onboardingPages[page]
                OnboardingPageContent(pageData = pageData)
            }

            // 底部指示器
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300)
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF007AFF)
                                else Color.White.copy(alpha = 0.3f)
                            )
                            .then(
                                if (isSelected) Modifier.size(width = width, height = 8.dp)
                                else Modifier.size(8.dp)
                            )
                    )
                }
            }

            // 开始使用按钮
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    scope.launch {
                        setOnboardingCompleted(context)
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                )
            ) {
                Text(
                    "开始使用",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(pageData: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标圆形背景
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    brush = ComposeBrush.linearGradient(
                        colors = pageData.gradientColors
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = pageData.icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 标题
        Text(
            text = pageData.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述
        Text(
            text = pageData.description,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

private suspend fun android.content.Context.edit(
    transform: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit
) {
    dataStore.edit(transform)
}