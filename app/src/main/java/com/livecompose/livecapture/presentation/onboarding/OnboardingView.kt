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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.design.Primary
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
 * 引导页面列表
 */
private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.AutoAwesome,
        title = "智能构图",
        description = "AI实时分析取景画面\n提供专业级构图建议"
    ),
    OnboardingPage(
        icon = Icons.Default.Image,
        title = "智能场景识别",
        description = "自动识别9种场景\n并提供拍摄指导"
    ),
    OnboardingPage(
        icon = Icons.Default.CameraAlt,
        title = "一键拍摄",
        description = "对齐后自动拍摄\n轻松捕捉精彩瞬间"
    )
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
                .padding(24.dp),
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
                            text = "跳过",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 16.sp
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

                Spacer(modifier = Modifier.height(32.dp))

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
                            text = "开始使用",
                            fontSize = 18.sp,
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
                            text = "下一步",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
            .padding(horizontal = 16.dp),
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
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述文本
        Text(
            text = page.description,
            fontSize = 16.sp,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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