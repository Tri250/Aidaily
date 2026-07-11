package com.livecompose.livecapture.presentation.compliance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.R

@Composable
fun PrivacyConsentDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onUserAgreementClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Do nothing - user must make a choice */ },
        title = {
            Text(
                text = stringResource(R.string.privacy_consent_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.privacy_consent_message),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                val privacyPolicyText = stringResource(R.string.privacy_policy_title)
                val userAgreementText = stringResource(R.string.user_agreement_title)
                val prefixText = stringResource(R.string.privacy_consent_detail_prefix)
                val suffixText = stringResource(R.string.privacy_consent_detail_suffix)
                val andText = stringResource(R.string.privacy_consent_and)

                val annotatedString = buildAnnotatedString {
                    append(prefixText)

                    pushStringAnnotation(tag = "privacy_policy", annotation = "privacy_policy")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(privacyPolicyText)
                    }
                    pop()

                    append(andText)

                    pushStringAnnotation(tag = "user_agreement", annotation = "user_agreement")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(userAgreementText)
                    }
                    pop()

                    append(suffixText)
                }

                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "privacy_policy",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let {
                            onPrivacyPolicyClick()
                        }

                        annotatedString.getStringAnnotations(
                            tag = "user_agreement",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let {
                            onUserAgreementClick()
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onAgree) {
                Text(stringResource(R.string.privacy_consent_agree))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDecline) {
                Text(stringResource(R.string.privacy_consent_decline))
            }
        }
    )
}
