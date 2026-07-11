package com.livecompose.livecapture.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.livecompose.livecapture.R
import com.livecompose.livecapture.core.design.Primary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleResId: Int,
    val descResId: Int
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.Psychology,
        titleResId = R.string.onboarding_page1_title,
        descResId = R.string.onboarding_page1_desc
    ),
    OnboardingPage(
        icon = Icons.Default.AutoFixHigh,
        titleResId = R.string.onboarding_page2_title,
        descResId = R.string.onboarding_page2_desc
    ),
    OnboardingPage(
        icon = Icons.Default.CameraAlt,
        titleResId = R.string.onboarding_page3_title,
        descResId = R.string.onboarding_page3_desc
    )
)

@Composable
fun OnboardingView(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val isLastPage = pagerState.currentPage == pages.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(3f)
        ) { page ->
            val pageData = pages[page]
            OnboardingPageContent(
                icon = pageData.icon,
                title = stringResource(pageData.titleResId),
                description = stringResource(pageData.descResId)
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Page indicator dots
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onComplete) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = {
                    if (isLastPage) {
                        viewModel.completeOnboarding()
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = if (isLastPage) stringResource(R.string.onboarding_start)
                    else stringResource(R.string.onboarding_next),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Icon area
        Surface(
            color = Primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.size(160.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
