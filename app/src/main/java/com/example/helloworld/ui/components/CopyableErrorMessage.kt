package com.example.helloworld.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

/**
 * A reusable error surface for the whole app.
 *
 * The user-facing message can stay friendly while [technicalDetails] contains
 * the actual exception/backend message that is useful when reporting a bug.
 * The copy action never copies secrets automatically; callers should pass only
 * safe diagnostic information.
 */
@Composable
fun CopyableErrorMessage(
    message: String,
    technicalDetails: String? = null,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    onRetry: (() -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val copyText = buildString {
        append(title)
        append(": ")
        append(message)
        if (!technicalDetails.isNullOrBlank() && technicalDetails != message) {
            append("\n\nTechnical details:\n")
            append(technicalDetails)
        }
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            if (!technicalDetails.isNullOrBlank() && technicalDetails != message) {
                Text(
                    text = technicalDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(copyText))
                        copied = true
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy error")
                    Text(if (copied) "Copied" else "Copy error")
                }

                if (onRetry != null) {
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

/**
 * Converts an exception into a safe diagnostic string for CopyableErrorMessage.
 * Authentication tokens, passwords and authorization headers must not be passed
 * into this function or exposed to the UI.
 */
fun safeErrorDetails(error: Throwable?): String? =
    error?.message?.trim()?.takeIf { it.isNotBlank() }
