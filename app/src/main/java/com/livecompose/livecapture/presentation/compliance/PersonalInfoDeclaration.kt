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
fun PersonalInfoDeclaration(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.personal_info_declaration_title)) },
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
                text = stringResource(R.string.personal_info_declaration_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.personal_info_declaration_update_date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            HorizontalDivider()

            // Intro
            BodyText(stringResource(R.string.personal_info_declaration_intro))

            // Table header
            DeclarationTableHeader()

            HorizontalDivider()

            // Camera
            DeclarationRow(
                infoType = stringResource(R.string.personal_info_camera_type),
                purpose = stringResource(R.string.personal_info_camera_purpose),
                storage = stringResource(R.string.personal_info_camera_storage),
                retention = stringResource(R.string.personal_info_camera_retention)
            )

            HorizontalDivider()

            // Photos
            DeclarationRow(
                infoType = stringResource(R.string.personal_info_photos_type),
                purpose = stringResource(R.string.personal_info_photos_purpose),
                storage = stringResource(R.string.personal_info_photos_storage),
                retention = stringResource(R.string.personal_info_photos_retention)
            )

            HorizontalDivider()

            // Sensors
            DeclarationRow(
                infoType = stringResource(R.string.personal_info_sensors_type),
                purpose = stringResource(R.string.personal_info_sensors_purpose),
                storage = stringResource(R.string.personal_info_sensors_storage),
                retention = stringResource(R.string.personal_info_sensors_retention)
            )

            HorizontalDivider()

            // Settings
            DeclarationRow(
                infoType = stringResource(R.string.personal_info_settings_type),
                purpose = stringResource(R.string.personal_info_settings_purpose),
                storage = stringResource(R.string.personal_info_settings_storage),
                retention = stringResource(R.string.personal_info_settings_retention)
            )

            HorizontalDivider()

            // Summary
            SectionTitle(stringResource(R.string.personal_info_summary_title))
            BodyText(stringResource(R.string.personal_info_summary_body))

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DeclarationTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.personal_info_header_type),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.personal_info_header_purpose),
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.personal_info_header_storage),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.personal_info_header_retention),
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DeclarationRow(
    infoType: String,
    purpose: String,
    storage: String,
    retention: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = infoType,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = purpose,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = storage,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = retention,
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodySmall
        )
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
