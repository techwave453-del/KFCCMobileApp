package com.example.helloworld.admin.content

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WebsiteContentScreen(
    modifier: Modifier = Modifier,
    viewModel: WebsiteContentViewModel = viewModel(factory = WebsiteContentViewModel.Factory(LocalContext.current.applicationContext as android.app.Application))
) {
    val content by viewModel.content.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Website Content", style = MaterialTheme.typography.headlineSmall)
                Text("Manage public homepage content without changing protected Church Identity.", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = viewModel::refresh, enabled = !loading && !saving) { Icon(Icons.Default.Refresh, "Refresh website content") }
        }
        Text("Permission: Website Content editing. The server remains the final authorization authority.", style = MaterialTheme.typography.bodySmall)
        Field("Tagline", content.tagline, { viewModel.update(content.copy(tagline = it)) }, saving)
        Field("Homepage title", content.title, { viewModel.update(content.copy(title = it)) }, saving)
        Field("Homepage subtitle", content.subtitle, { viewModel.update(content.copy(subtitle = it)) }, saving, 2)
        Field("About heading", content.aboutTitle, { viewModel.update(content.copy(aboutTitle = it)) }, saving)
        Field("About text", content.aboutText, { viewModel.update(content.copy(aboutText = it)) }, saving, 5)
        Field("Phone", content.phone, { viewModel.update(content.copy(phone = it)) }, saving)
        Field("Email", content.email, { viewModel.update(content.copy(email = it)) }, saving)
        Text("Live Streaming", style = MaterialTheme.typography.titleMedium)
        Field("Live stream title", content.liveStream.title, { viewModel.update(content.copy(liveStream = content.liveStream.copy(title = it))) }, saving)
        Field("YouTube Live URL", content.liveStream.url, { viewModel.update(content.copy(liveStream = content.liveStream.copy(url = it))) }, saving)
        Field("Live description", content.liveStream.description, { viewModel.update(content.copy(liveStream = content.liveStream.copy(description = it))) }, saving, 3)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enable live stream", modifier = Modifier.weight(1f))
            Switch(checked = content.liveStream.enabled, onCheckedChange = { viewModel.update(content.copy(liveStream = content.liveStream.copy(enabled = it))) }, enabled = !saving)
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (saved) Text("Website Content saved successfully.", color = MaterialTheme.colorScheme.primary)
        Button(onClick = viewModel::save, enabled = !loading && !saving, modifier = Modifier.fillMaxWidth()) {
            if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Save Website Content")
        }
    }
}

@Composable
private fun Field(label: String, value: String, onValueChange: (String) -> Unit, disabled: Boolean, minLines: Int = 1) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, enabled = !disabled, minLines = minLines, modifier = Modifier.fillMaxWidth())
}
