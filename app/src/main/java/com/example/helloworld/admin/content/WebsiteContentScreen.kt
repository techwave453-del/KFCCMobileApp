package com.example.helloworld.admin.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WebsiteContentScreen(
    modifier: Modifier = Modifier,
    viewModel: WebsiteContentViewModel = viewModel(factory = WebsiteContentViewModel.Factory(androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application))
) {
    val content by viewModel.content.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()

    if (loading && content.name.isBlank()) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Loading website content…")
        }
        return
    }

    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Website Content", style = MaterialTheme.typography.headlineSmall)
                    Text("Manage public pages, services, classes, contact information and theme.", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = viewModel::refresh, enabled = !loading && !saving) { Icon(Icons.Default.Refresh, "Refresh") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("General website information", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(content.phone, { viewModel.update(content.copy(phone = it)) }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                    OutlinedTextField(content.email, { viewModel.update(content.copy(email = it)) }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                }
            }
        }
        item { Text("Services & Events", style = MaterialTheme.typography.titleMedium) }
        itemsIndexed(content.services) { index, service ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        service.title,
                        { value -> val list = content.services.toMutableList(); list[index] = service.copy(title = value); viewModel.update(content.copy(services = list)) },
                        label = { Text("Service name") }, modifier = Modifier.fillMaxWidth(), enabled = !saving
                    )
                    OutlinedTextField(
                        service.time,
                        { value -> val list = content.services.toMutableList(); list[index] = service.copy(time = value); viewModel.update(content.copy(services = list)) },
                        label = { Text("Schedule") }, modifier = Modifier.fillMaxWidth(), enabled = !saving
                    )
                }
            }
        }
        item { Text("Theme", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(content.theme.mode, { viewModel.update(content.copy(theme = content.theme.copy(mode = it))) }, label = { Text("Appearance: light or dark") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                    OutlinedTextField(content.theme.accent, { viewModel.update(content.copy(theme = content.theme.copy(accent = it))) }, label = { Text("Accent color") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                }
            }
        }
        item {
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            if (saved) Text("Website content saved successfully.", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Button(onClick = viewModel::save, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                if (saving) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Save Website Content")
            }
        }
    }
}
