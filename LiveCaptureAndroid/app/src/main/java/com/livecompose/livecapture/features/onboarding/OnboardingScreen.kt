package com.livecompose.livecapture.features.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Context
import com.livecompose.livecapture.ui.design.DesignSystem
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
        gradientColors = listOf(DesignSystem.Colors.primary, DesignSystem.Colors.secondary)
    ),
    OnboardingPageData(
        icon = Icons.Default.ColorLens,
        title = "LUT 色彩预设",
        description = "一键应用专业胶片色彩",
        gradientColors = listOf(DesignSystem.Colors.accent, DesignSystem.Colors.error)
    ),
    OnboardingPageData(
        icon = Icons.Default.Brush,
        title = "相框水印",
        description = "为作品添加个性边框和水印",
        gradientColors = listOf(DesignSystem.Colors.success, DesignSystem.Colors.info)
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
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 跳过按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.medium, vertical = DesignSystem.Spacing.small),
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
                        color = DesignSystem.Colors.minimalSecondaryLabel,
                        style = DesignSystem.Typography.callout
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
            Spacer(modifier = Modifier.height(DesignSystem.Spacing.large))
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xxSmall),
                modifier = Modifier.padding(bottom = DesignSystem.Spacing.small)
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = DesignSystem.Animation.iosSpringDp(0.35, 0.72f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) DesignSystem.Colors.primary
                                else DesignSystem.Colors.minimalBorder
                            )
                            .then(
                                if (isSelected) Modifier.size(width = width, height = 8.dp)
                                else Modifier.size(8.dp)
                            )
                    )
                }
            }

            // 开始使用按钮
            Spacer(modifier = Modifier.height(DesignSystem.Spacing.xLarge))
            Button(
                onClick = {
                    scope.launch {
                        setOnboardingCompleted(context)
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.xLarge)
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesignSystem.Colors.primary
                )
            ) {
                Text(
                    "开始使用",
                    style = DesignSystem.Typography.title3,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxLarge))
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

        Spacer(modifier = Modifier.height(DesignSystem.Spacing.xxLarge))

        // 标题
        Text(
            text = pageData.title,
            style = DesignSystem.Typography.title1,
            color = DesignSystem.Colors.minimalLabel,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(DesignSystem.Spacing.small))

        // 描述
        Text(
            text = pageData.description,
            style = DesignSystem.Typography.callout,
            color = DesignSystem.Colors.minimalSecondaryLabel,
            textAlign = TextAlign.Center
        )
    }
}

private suspend fun android.content.Context.edit(
    transform: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit
) {
    dataStore.edit(transform)
}