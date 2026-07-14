package com.livecompose.livecapture.presentation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.livecompose.livecapture.R
import com.livecompose.livecapture.core.design.Primary
import com.livecompose.livecapture.core.design.TitleTextStyle
import com.livecompose.livecapture.core.design.*
import com.livecompose.livecapture.presentation.Screen

@Composable
fun LiveComposeView(
    navController: NavController? = null,
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Huge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.capture_settings)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Huge))

        // Brand Logo
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Primary, Color(0xFF1976D2))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = stringResource(R.string.content_desc_logo),
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.Huge))

        Text(
            text = stringResource(R.string.dashboard_title),
            style = TitleTextStyle.copy(fontSize = FontSize.DisplayLarge),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.dashboard_subtitle),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = FontSize.TitleLarge
        )

        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        Text(
            text = stringResource(R.string.dashboard_tagline),
            color = Primary,
            fontSize = FontSize.HeadlineMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(Spacing.Massive))

        // Quick Capture Entry
        Button(
            onClick = { navController?.navigate(Screen.Capture.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(CornerRadius.ExtraLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.dashboard_start_capture),
                fontSize = FontSize.TitleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(Spacing.Huge))

        // Stats Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            StatCard(
                label = stringResource(R.string.dashboard_stats_today),
                value = "0 ${stringResource(R.string.dashboard_stats_unit)}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.dashboard_stats_total),
                value = "0 ${stringResource(R.string.dashboard_stats_unit)}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.dashboard_stats_score),
                value = "0.00",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.Huge))

        // Recent Photos
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.dashboard_recent_photos),
                fontSize = FontSize.TitleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.Large))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Large)
            ) {
                items(5) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(CornerRadius.Large))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(Spacing.Massive)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Huge))

        // Model Status Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius.ExtraLarge),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.dashboard_model_status),
                    fontSize = FontSize.BodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.Medium))
                Text(
                    text = stringResource(R.string.dashboard_model_student),
                    fontSize = FontSize.TitleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text(
                    text = "${stringResource(R.string.dashboard_inference_speed)}: -- FPS",
                    fontSize = FontSize.BodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Huge))

        // Feature Cards
        FeatureCard(
            icon = Icons.Default.Psychology,
            title = stringResource(R.string.dashboard_feature_ai_title),
            description = stringResource(R.string.dashboard_feature_ai_desc)
        )

        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        FeatureCard(
            icon = Icons.Default.AutoFixHigh,
            title = stringResource(R.string.dashboard_feature_track_title),
            description = stringResource(R.string.dashboard_feature_track_desc)
        )

        Spacer(modifier = Modifier.height(Spacing.Huge))

        Text(
            text = stringResource(R.string.dashboard_offline),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = FontSize.BodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Text(
            text = stringResource(R.string.dashboard_model_info),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontSize = FontSize.BodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CornerRadius.ExtraLarge),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = FontSize.HeadlineMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = label,
                fontSize = FontSize.BodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.ExtraLarge),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
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
                    modifier = Modifier.size(Spacing.Huge)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.ExtraLarge))

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = FontSize.TitleMedium
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = FontSize.BodyMedium
                )
            }
        }
    }
}
