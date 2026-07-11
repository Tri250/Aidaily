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
fun UserAgreementView(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_agreement_title)) },
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
                text = stringResource(R.string.user_agreement_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.user_agreement_update_date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            HorizontalDivider()

            // Acceptance
            SectionTitle(stringResource(R.string.user_agreement_acceptance_title))
            BodyText(stringResource(R.string.user_agreement_acceptance_body))

            // Service Scope
            SectionTitle(stringResource(R.string.user_agreement_scope_title))
            BodyText(stringResource(R.string.user_agreement_scope_body))

            // User Obligations
            SectionTitle(stringResource(R.string.user_agreement_obligations_title))
            BodyText(stringResource(R.string.user_agreement_obligations_body))

            // IP Rights
            SectionTitle(stringResource(R.string.user_agreement_ip_title))
            BodyText(stringResource(R.string.user_agreement_ip_body))

            // Privacy
            SectionTitle(stringResource(R.string.user_agreement_privacy_title))
            BodyText(stringResource(R.string.user_agreement_privacy_body))

            // Liability
            SectionTitle(stringResource(R.string.user_agreement_liability_title))
            BodyText(stringResource(R.string.user_agreement_liability_body))

            // Termination
            SectionTitle(stringResource(R.string.user_agreement_termination_title))
            BodyText(stringResource(R.string.user_agreement_termination_body))

            // Dispute Resolution
            SectionTitle(stringResource(R.string.user_agreement_dispute_title))
            BodyText(stringResource(R.string.user_agreement_dispute_body))

            // Changes
            SectionTitle(stringResource(R.string.user_agreement_changes_title))
            BodyText(stringResource(R.string.user_agreement_changes_body))

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
