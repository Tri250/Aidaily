package com.livecompose.livecapture.presentation.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.livecompose.livecapture.R
import com.livecompose.livecapture.core.design.Primary
import com.livecompose.livecapture.core.design.TitleTextStyle
import com.livecompose.livecapture.core.design.TrackingDotAligned
import com.livecompose.livecapture.core.design.TrackingDotColor
import com.livecompose.livecapture.core.design.GridLine
import com.livecompose.livecapture.presentation.Screen

@Composable
fun LiveComposeView(
    viewModel: LiveComposeViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val currentTipIndex by viewModel.currentTipIndex.collectAsStateWithLifecycle()
    val expandedFeatureCard by viewModel.expandedFeatureCard.collectAsStateWithLifecycle()
    val isDemoAnimating by viewModel.isDemoAnimating.collectAsStateWithLifecycle()
    val demoExampleIndex by viewModel.demoExampleIndex.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== Top Section: Brand Logo + Tagline =====
        BrandHeader()

        Spacer(modifier = Modifier.height(24.dp))

        // ===== Middle Section: Interactive Composition Demo =====
        CompositionDemoSection(
            isDemoAnimating = isDemoAnimating,
            demoExampleIndex = demoExampleIndex,
            onCycleExample = {
                viewModel.setDemoAnimating(true)
                viewModel.cycleDemoExample()
            },
            onAnimationComplete = { viewModel.setDemoAnimating(false) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ===== CTA Button: Navigate to Capture =====
        val captureButtonDesc = stringResource(R.string.compose_start_capture)
        Button(
            onClick = { navController?.navigate(Screen.Capture.route) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    contentDescription = captureButtonDesc
                },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = captureButtonDesc,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ===== Feature Cards (Expandable) =====
        FeatureCardsSection(
            expandedCard = expandedFeatureCard,
            onCardClick = { viewModel.setExpandedFeatureCard(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ===== Composition Tips Section =====
        CompositionTipsSection(
            tips = viewModel.compositionTips,
            currentTipIndex = currentTipIndex,
            onCycleTip = { viewModel.cycleTip() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ===== Footer: Model Info =====
        Text(
            text = stringResource(R.string.compose_offline_ai),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.compose_model_info),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BrandHeader() {
    val logoDesc = stringResource(R.string.a11y_compose_logo)

    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Primary, Color(0xFF1976D2))
                ),
                shape = CircleShape
            )
            .semantics { contentDescription = logoDesc },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(56.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.compose_brand_name),
        style = TitleTextStyle.copy(fontSize = 36.sp),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )

    Text(
        text = "LiveCapture",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        fontSize = 18.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.compose_subtitle),
        color = Primary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun CompositionDemoSection(
    isDemoAnimating: Boolean,
    demoExampleIndex: Int,
    onCycleExample: () -> Unit,
    onAnimationComplete: () -> Unit
) {
    val demoAreaDesc = stringResource(R.string.a11y_compose_demo_area)
    val demoTitleDesc = stringResource(R.string.compose_demo_title)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = demoTitleDesc,
            style = TitleTextStyle,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Demo preview box with grid overlay and tracking dot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = demoAreaDesc }
        ) {
            // Simulated photo background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A237E).copy(alpha = 0.3f),
                                Color(0xFF0D47A1).copy(alpha = 0.5f),
                                Color(0xFF01579B).copy(alpha = 0.3f)
                            )
                        )
                    )
            )

            // Grid overlay (三分法)
            DemoGridOverlay()

            // Animated tracking dot
            DemoTrackingDot(
                exampleIndex = demoExampleIndex,
                isAnimating = isDemoAnimating,
                onAnimationComplete = onAnimationComplete
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cycle button
        val buttonLabel = stringResource(R.string.compose_demo_button)
        OutlinedAccessibleButton(
            text = buttonLabel,
            onClick = onCycleExample
        )
    }
}

@Composable
private fun DemoGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.dp.toPx()

        // Vertical lines
        drawLine(
            color = GridLine,
            start = Offset(width / 3, 0f),
            end = Offset(width / 3, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = GridLine,
            start = Offset(width * 2 / 3, 0f),
            end = Offset(width * 2 / 3, height),
            strokeWidth = strokeWidth
        )
        // Horizontal lines
        drawLine(
            color = GridLine,
            start = Offset(0f, height / 3),
            end = Offset(width, height / 3),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = GridLine,
            start = Offset(0f, height * 2 / 3),
            end = Offset(width, height * 2 / 3),
            strokeWidth = strokeWidth
        )

        // Intersection dots
        val dotRadius = 4.dp.toPx()
        val thirdPoints = listOf(
            width / 3f to height / 3f,
            width / 3f to height * 2 / 3f,
            width * 2 / 3f to height / 3f,
            width * 2 / 3f to height * 2 / 3f
        )
        thirdPoints.forEach { (x, y) ->
            drawCircle(
                color = TrackingDotAligned.copy(alpha = 0.6f),
                radius = dotRadius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun DemoTrackingDot(
    exampleIndex: Int,
    isAnimating: Boolean,
    onAnimationComplete: () -> Unit
) {
    // Define target positions for different composition examples
    val targetPositions = remember {
        listOf(
            // Rule of thirds: upper-left intersection
            1f / 3f to 1f / 3f,
            // Rule of thirds: lower-right intersection
            2f / 3f to 2f / 3f,
            // Center composition
            0.5f to 0.5f,
            // Upper-right intersection
            2f / 3f to 1f / 3f
        )
    }

    val (targetX, targetY) = targetPositions[exampleIndex % targetPositions.size]

    var currentX by remember { mutableStateOf(targetX) }
    var currentY by remember { mutableStateOf(targetY) }

    val animatedX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "demoX"
    )
    val animatedY by animateFloatAsState(
        targetValue = targetY,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "demoY"
    )

    LaunchedEffect(animatedX, animatedY) {
        currentX = animatedX
        currentY = animatedY
    }

    // Detect when animation is effectively done
    LaunchedEffect(targetX, targetY) {
        kotlinx.coroutines.delay(1300)
        onAnimationComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "demoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "demoPulse"
    )

    val dotDesc = stringResource(R.string.compose_demo_tracking_dot)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = dotDesc }
    ) {
        val x = animatedX * size.width
        val y = animatedY * size.height
        val radius = 16.dp.toPx()

        // Pulse ring
        drawCircle(
            color = TrackingDotAligned.copy(alpha = 0.3f),
            radius = radius * pulseScale,
            center = Offset(x, y)
        )

        // Solid dot
        drawCircle(
            color = TrackingDotAligned,
            radius = radius * 0.5f,
            center = Offset(x, y)
        )

        // Cross when near grid intersection (simplified: always show for demo)
        val crossSize = 20.dp.toPx()
        drawLine(
            color = TrackingDotAligned,
            start = Offset(x - crossSize, y),
            end = Offset(x + crossSize, y),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = TrackingDotAligned,
            start = Offset(x, y - crossSize),
            end = Offset(x, y + crossSize),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun OutlinedAccessibleButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        shape = RoundedCornerShape(12.dp),
        color = Primary.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = Primary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FeatureCardsSection(
    expandedCard: Int,
    onCardClick: (Int) -> Unit
) {
    val sectionTitle = stringResource(R.string.compose_feature_ai_title)
    val features = listOf(
        FeatureData(
            icon = Icons.Default.Psychology,
            titleResId = R.string.compose_feature_ai_title,
            descResId = R.string.compose_feature_ai_desc,
            detailResId = R.string.compose_feature_ai_detail
        ),
        FeatureData(
            icon = Icons.Default.AutoFixHigh,
            titleResId = R.string.compose_feature_tracking_title,
            descResId = R.string.compose_feature_tracking_desc,
            detailResId = R.string.compose_feature_tracking_detail
        ),
        FeatureData(
            icon = Icons.Default.Timelapse,
            titleResId = R.string.compose_feature_auto_capture_title,
            descResId = R.string.compose_feature_auto_capture_desc,
            detailResId = R.string.compose_feature_auto_capture_detail
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        features.forEachIndexed { index, feature ->
            ExpandableFeatureCard(
                icon = feature.icon,
                title = stringResource(feature.titleResId),
                description = stringResource(feature.descResId),
                detail = stringResource(feature.detailResId),
                isExpanded = expandedCard == index,
                onClick = { onCardClick(index) }
            )
        }
    }
}

private data class FeatureData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val titleResId: Int,
    val descResId: Int,
    val detailResId: Int
)

@Composable
private fun ExpandableFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    detail: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val cardDesc = if (isExpanded) {
        stringResource(R.string.a11y_compose_feature_card_expanded, title)
    } else {
        stringResource(R.string.a11y_compose_feature_card_collapsed, title)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = cardDesc
                stateDescription = if (isExpanded) "已展开" else "已折叠"
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expandable detail section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = detail,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    )
}

@Composable
private fun CompositionTipsSection(
    tips: List<CompositionTip>,
    currentTipIndex: Int,
    onCycleTip: () -> Unit
) {
    val sectionTitle = stringResource(R.string.compose_tips_section_title)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle,
                style = TitleTextStyle,
                modifier = Modifier.semantics { heading() }
            )

            val cycleLabel = stringResource(R.string.compose_demo_button)
            TextButton(
                text = cycleLabel,
                onClick = onCycleTip
            )
        }

        // Tip cards
        tips.forEachIndexed { index, tip ->
            val tipTitle = stringResource(tip.titleResId)
            val tipDesc = stringResource(tip.descResId)
            val tipCardDesc = stringResource(R.string.a11y_compose_tip_card, tipTitle)
            val isHighlighted = index == currentTipIndex

            val cardColor by animateColorAsState(
                targetValue = if (isHighlighted) {
                    Primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                animationSpec = tween(300),
                label = "tipColor"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .semantics {
                        contentDescription = tipCardDesc
                    },
                shape = RoundedCornerShape(12.dp),
                color = cardColor
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tipTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isHighlighted) Primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tipDesc,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TextButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        shape = RoundedCornerShape(8.dp),
        color = Primary.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            color = Primary,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
