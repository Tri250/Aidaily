package com.livecompose.livecapture.presentation.feedback

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livecompose.livecapture.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackView(
    onNavigateBack: () -> Unit = {},
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val successMsg = stringResource(R.string.feedback_success)

    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) {
            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
            viewModel.reset()
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.feedback_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Category
        var categoryExpanded by remember { mutableStateOf(false) }
        val categories = mapOf(
            "bug" to stringResource(R.string.feedback_category_bug),
            "suggestion" to stringResource(R.string.feedback_category_suggestion),
            "other" to stringResource(R.string.feedback_category_other)
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = categories[state.category] ?: state.category,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.feedback_category)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            viewModel.setCategory(key)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        OutlinedTextField(
            value = state.content,
            onValueChange = { viewModel.setContent(it) },
            label = { Text(stringResource(R.string.feedback_content)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            maxLines = 8
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email (optional)
        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.setEmail(it) },
            label = { Text(stringResource(R.string.feedback_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error message
        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submit button
        Button(
            onClick = { viewModel.submitFeedback() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting && state.content.isNotBlank()
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.settings_submit_feedback))
        }
    }
}
