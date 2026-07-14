package com.livecompose.livecapture.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.R
import com.livecompose.livecapture.core.design.Primary
import com.livecompose.livecapture.core.design.*
import kotlinx.coroutines.launch

/**
 * 引导页数据类
 */
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * 引导页主视图
 *
 * @param onComplete 引导完成回调，点击"开始使用"按钮时触发
 * @param onSkip 跳过引导回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingView(
    onComplete: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val onboardingPages = listOf(
        OnboardingPage(
            icon = Icons.Default.AutoAwesome,
            title = stringResource(R.string.onboarding_title_compose),
            description = stringResource(R.string.onboarding_desc_compose)
        ),
        OnboardingPage(
            icon = Icons.Default.Image,
            title = stringResource(R.string.onboarding_title_scene),
            description = stringResource(R.string.onboarding_desc_scene)
        ),
        OnboardingPage(
            icon = Icons.Default.CameraAlt,
            title = stringResource(R.string.onboarding_title_capture),
            description = stringResource(R.string.onboarding_desc_capture)
        )
    )
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Huge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 跳过按钮（最后一页隐藏）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = FontSize.TitleMedium
                        )
                    }
                }
            }

            // 水平分页内容
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                OnboardingPageContent(page = onboardingPages[page])
            }

            // 底部区域：指示器和按钮
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 页面指示器
                PageIndicator(
                    pageCount = onboardingPages.size,
                    currentPage = pagerState.currentPage
                )

                Spacer(modifier = Modifier.height(Spacing.Massive))

                // 底部按钮
                if (isLastPage) {
                    // 最后一页显示"开始使用"按钮
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_start),
                            fontSize = FontSize.TitleLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                } else {
                    // 其他页显示"下一步"按钮
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_next),
                            fontSize = FontSize.TitleLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.Huge))
            }
        }
    }
}

/**
 * 单个引导页内容
 *
 * @param page 引导页数据
 */
@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部大图标
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = Primary.copy(alpha = 0.15f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 标题
        Text(
            text = page.title,
            fontSize = FontSize.DisplayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        // 描述文本
        Text(
            text = page.description,
            fontSize = FontSize.TitleMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

/**
 * 页面指示器组件
 *
 * @param pageCount 总页数
 * @param currentPage 当前页面索引
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            PageDot(
                isSelected = index == currentPage
            )
        }
    }
}

/**
 * 单个指示器点
 *
 * @param isSelected 是否选中
 */
@Composable
private fun PageDot(isSelected: Boolean) {
    val size by animateDpAsState(
        targetValue = if (isSelected) 10.dp else 8.dp,
        label = "dot_size"
    )

    val color by animateColorAsState(
        targetValue = if (isSelected) Primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
        label = "dot_color"
    )

    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = color
    ) {}
}
