package com.example.helloworld.admin.live

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.content.WebsiteContentViewModel

@Composable
fun LiveStreamingScreen(
    modifier: Modifier = Modifier,
    viewModel: WebsiteContentViewModel = viewModel(
        factory = WebsiteContentViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val content by viewModel.content.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val live = content.liveStream

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Live Streaming", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Manage the live-service settings directly in Supabase.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = viewModel::refresh, enabled = !loading && !saving) {
                Icon(Icons.Default.Refresh, "Refresh live-stream settings")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Broadcast status", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (live.enabled) "Live streaming is enabled" else "Live streaming is disabled",
                        Modifier.weight(1f)
                    )
                    Switch(
                        checked = live.enabled,
                        onCheckedChange = {
                            viewModel.update(
                                content.copy(liveStream = live.copy(enabled = it))
                            )
                        },
                        enabled = !saving
                    )
                }
            }
        }

        LiveField("Stream title", live.title, saving) {
            viewModel.update(content.copy(liveStream = live.copy(title = it)))
        }
        LiveField("YouTube / livestream URL", live.url, saving) {
            viewModel.update(content.copy(liveStream = live.copy(url = it)))
        }
        LiveField("Public description", live.description, saving, 4) {
            viewModel.update(content.copy(liveStream = live.copy(description = it)))
        }

        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (saved) Text("Live-stream settings saved successfully.", color = MaterialTheme.colorScheme.primary)

        Button(
            onClick = viewModel::saveLiveStream,
            enabled = !loading && !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(20.dp))
            else Text("Save Live Streaming")
        }
    }
}

@Composable
private fun LiveField(
    label: String,
    value: String,
    disabled: Boolean,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = !disabled,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth()
    )
}
