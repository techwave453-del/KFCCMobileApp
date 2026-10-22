package com.example.helloworld.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

/**
 * A reusable error surface for the whole app.
 *
 * Technical exception strings from Supabase/Ktor can contain request URLs,
 * query parameters and authentication headers. Always sanitize them before
 * showing or copying diagnostics.
 */
@Composable
fun CopyableErrorMessage(
    message: String,
    technicalDetails: String? = null,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    onRetry: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var copyStatus by remember { mutableStateOf("Copy error") }
    var copied by remember { mutableStateOf(false) }

    val safeMessage = sanitizeErrorText(message)
    val safeTechnicalDetails = technicalDetails?.let(::sanitizeErrorText)

    val copyText = buildString {
        append(title)
        append(": ")
        append(safeMessage)
        if (!safeTechnicalDetails.isNullOrBlank() && safeTechnicalDetails != safeMessage) {
            append("\n\nTechnical details:\n")
            append(safeTechnicalDetails)
        }
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
            copyStatus = "Copy error"
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Card(
        modifier = modifier.fillMaxWidth().heightIn(max = screenHeight * 0.62f),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = safeMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                if (!safeTechnicalDetails.isNullOrBlank() && safeTechnicalDetails != safeMessage) {
                    Text(
                        text = safeTechnicalDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        val result = runCatching {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(title, copyText))
                        }
                        if (result.isSuccess) {
                            copyStatus = "Copied"
                            copied = true
                        } else {
                            copyStatus = "Copy failed — tap to retry"
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy error")
                    Text(if (copied) "Copied" else copyStatus)
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
 * Removes credentials and request metadata that must never leave the process
 * through an error surface or clipboard.
 */
fun sanitizeErrorText(raw: String): String {
    var value = raw.trim()

    value = value.replace(
        Regex("(?i)Authorization=\\[Bearer\\s+[^\\]]*\\]"),
        "Authorization=[REDACTED]"
    )
    value = value.replace(
        Regex("(?i)apikey=\\[[^\\]]*\\]"),
        "apikey=[REDACTED]"
    )
    value = value.replace(
        Regex("(?i)(password|passwd|access_token|refresh_token|api[_-]?key)\\s*[:=]\\s*[^,;\\s\\]]+"),
        "$1=[REDACTED]"
    )
    value = value.replace(
        Regex("https?://[^\\s,}\\]]+"),
        "[request URL redacted]"
    )
    value = value.replace(
        Regex("\\bBearer\\s+[A-Za-z0-9._~+/=-]+"),
        "Bearer [REDACTED]"
    )

    return value.ifBlank { "Unknown error." }
}

/**
 * Converts an exception into a safe diagnostic string for CopyableErrorMessage.
 */
fun safeErrorDetails(error: Throwable?): String? =
    error?.message?.let(::sanitizeErrorText)?.takeIf { it.isNotBlank() }
