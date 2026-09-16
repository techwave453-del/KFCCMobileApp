package com.example.helloworld.admin.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MediaCenterScreen(
    modifier: Modifier = Modifier,
    canUpload: Boolean = false
) {
    val viewModel: MediaCenterViewModel = viewModel()
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val uploading by viewModel.uploading.collectAsState()
    val error by viewModel.error.collectAsState()
    val uploadMessage by viewModel.uploadMessage.collectAsState()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            showUploadDialog = true
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null)
                Text(
                    "Media Center",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                if (canUpload) {
                    Button(
                        onClick = {
                            picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf"))
                        },
                        enabled = !loading && !uploading
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Text("Upload", modifier = Modifier.padding(start = 6.dp))
                    }
                    Spacer(Modifier.padding(horizontal = 4.dp))
                }
                IconButton(onClick = viewModel::load, enabled = !loading && !uploading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh media")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Upload images, videos, audio or PDF documents. Files are stored by the church media service.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            if (uploadMessage != null) {
                Text(uploadMessage!!, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            if (error != null && items.isNotEmpty()) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            when {
                loading && items.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading media library…")
                    }
                }
                error != null && items.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.height(12.dp))
                        Text("Unable to load the media library", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(error ?: "Please refresh the page and try again.")
                    }
                }
                items.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.height(12.dp))
                        Text("No media items yet", style = MaterialTheme.typography.titleMedium)
                        if (canUpload) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) }) {
                                Text("Upload the first media file")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(items, key = { it.id }) { item ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (item.type == "video") Icons.Default.VideoLibrary else Icons.Default.Image,
                                        contentDescription = null
                                    )
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(
                                            item.title.ifBlank { item.url },
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(item.type, style = MaterialTheme.typography.bodySmall)
                                        if (item.type == "video") {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                if (item.featured) "Featured video" else "Not featured",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            if (item.published) "Published" else "Unpublished",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.published) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    if (item.type == "video") {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = if (item.featured) "Featured" else "Video",
                                            tint = if (item.featured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog && selectedUri != null) {
        UploadMediaDialog(
            uploading = uploading,
            onDismiss = {
                if (!uploading) {
                    showUploadDialog = false
                    selectedUri = null
                }
            },
            onUpload = { title, description, category, type ->
                viewModel.upload(selectedUri!!, title, description, category, type) {
                    showUploadDialog = false
                    selectedUri = null
                }
            }
        )
    }
}

@Composable
private fun UploadMediaDialog(
    uploading: Boolean,
    onDismiss: () -> Unit,
    onUpload: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("general") }
    var type by remember { mutableStateOf("image") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload media") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Type: $type")
                    }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("image", "video", "audio", "document").forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    type = value
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                if (uploading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.height(20.dp))
                        Text("Uploading…", modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpload(title, description, category, type) },
                enabled = !uploading
            ) { Text("Upload") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !uploading) { Text("Cancel") }
        }
    )
}
