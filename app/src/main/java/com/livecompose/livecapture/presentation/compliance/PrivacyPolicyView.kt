package com.livecompose.livecapture.presentation.compliance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyView(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_policy_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.privacy_policy_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.privacy_policy_update_date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            HorizontalDivider()

            // Introduction
            SectionTitle(stringResource(R.string.privacy_policy_intro_title))
            BodyText(stringResource(R.string.privacy_policy_intro_body))

            // Camera
            SectionTitle(stringResource(R.string.privacy_policy_camera_title))
            BodyText(stringResource(R.string.privacy_policy_camera_body))

            // Photo Storage
            SectionTitle(stringResource(R.string.privacy_policy_storage_title))
            BodyText(stringResource(R.string.privacy_policy_storage_body))

            // Sensors
            SectionTitle(stringResource(R.string.privacy_policy_sensors_title))
            BodyText(stringResource(R.string.privacy_policy_sensors_body))

            // AI Inference
            SectionTitle(stringResource(R.string.privacy_policy_ai_title))
            BodyText(stringResource(R.string.privacy_policy_ai_body))

            // No Network
            SectionTitle(stringResource(R.string.privacy_policy_network_title))
            BodyText(stringResource(R.string.privacy_policy_network_body))

            // No Third-party SDK
            SectionTitle(stringResource(R.string.privacy_policy_third_party_title))
            BodyText(stringResource(R.string.privacy_policy_third_party_body))

            // Data Retention
            SectionTitle(stringResource(R.string.privacy_policy_retention_title))
            BodyText(stringResource(R.string.privacy_policy_retention_body))

            // User Rights
            SectionTitle(stringResource(R.string.privacy_policy_rights_title))
            BodyText(stringResource(R.string.privacy_policy_rights_body))

            // Changes
            SectionTitle(stringResource(R.string.privacy_policy_changes_title))
            BodyText(stringResource(R.string.privacy_policy_changes_body))

            // Contact
            SectionTitle(stringResource(R.string.privacy_policy_contact_title))
            BodyText(stringResource(R.string.privacy_policy_contact_body))

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
    )
}
