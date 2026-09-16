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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Switch
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
    canUpload: Boolean = false,
    canEdit: Boolean = false,
    canDelete: Boolean = false
) {
    val viewModel: MediaCenterViewModel = viewModel()
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val uploading by viewModel.uploading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val deleting by viewModel.deleting.collectAsState()
    val error by viewModel.error.collectAsState()
    val uploadMessage by viewModel.uploadMessage.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<AdminMediaItem?>(null) }
    var deletingItem by remember { mutableStateOf<AdminMediaItem?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            showUploadDialog = true
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null)
                Text("Media Center", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(start = 12.dp))
                if (canUpload) {
                    Button(
                        onClick = { picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) },
                        enabled = !loading && !uploading && !saving && !deleting
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Text("Upload", modifier = Modifier.padding(start = 6.dp))
                    }
                    Spacer(Modifier.padding(horizontal = 4.dp))
                }
                IconButton(onClick = viewModel::load, enabled = !loading && !uploading && !saving && !deleting) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh media")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Manage church images, videos, audio and documents from one secure media library.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            uploadMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            actionMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            if (error != null && items.isNotEmpty()) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            when {
                loading && items.isEmpty() -> CenterMessage("Loading media library…", loading = true)
                error != null && items.isEmpty() -> CenterMessage("Unable to load the media library\n${error ?: "Please refresh and try again."}")
                items.isEmpty() -> {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.height(12.dp))
                        Text("No media items yet", style = MaterialTheme.typography.titleMedium)
                        if (canUpload) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) }) { Text("Upload the first media file") }
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(items, key = { it.id }) { item ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (item.isVideo) Icons.Default.VideoLibrary else Icons.Default.Image, contentDescription = null)
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(4.dp))
                                            Text("${item.type} · ${item.category}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (canEdit) {
                                            IconButton(onClick = { editingItem = item }, enabled = !saving && !deleting) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit media")
                                            }
                                        }
                                        if (canDelete) {
                                            IconButton(onClick = { deletingItem = item }, enabled = !saving && !deleting) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete media")
                                            }
                                        }
                                    }

                                    if (item.description.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (item.published) "Published" else "Unpublished", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                        if (canEdit) {
                                            Switch(
                                                checked = item.published,
                                                onCheckedChange = { checked ->
                                                    viewModel.save(item, item.title, item.description, item.category, checked) {}
                                                },
                                                enabled = !saving && !deleting
                                            )
                                        }
                                    }

                                    if (item.isVideo) {
                                        Spacer(Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = if (item.featured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(if (item.featured) "Featured video" else "Not featured", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f).padding(start = 6.dp))
                                            if (canEdit) {
                                                OutlinedButton(onClick = { viewModel.setFeatured(item, !item.featured) }, enabled = !saving && !deleting) {
                                                    Text(if (item.featured) "Unfeature" else "Feature video")
                                                }
                                            }
                                        }
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
            onDismiss = { if (!uploading) { showUploadDialog = false; selectedUri = null } },
            onUpload = { title, description, category, type ->
                viewModel.upload(selectedUri!!, title, description, category, type) {
                    showUploadDialog = false
                    selectedUri = null
                }
            }
        )
    }

    editingItem?.let { item ->
        EditMediaDialog(
            item = item,
            saving = saving,
            onDismiss = { if (!saving) editingItem = null },
            onSave = { title, description, category, published ->
                viewModel.save(item, title, description, category, published) { editingItem = null }
            }
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!deleting) deletingItem = null },
            title = { Text("Delete media?") },
            text = { Text("This will permanently remove “${item.title.ifBlank { "this media item" }}” from the Media Center. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.delete(item) { deletingItem = null } },
                    enabled = !deleting
                ) { if (deleting) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { deletingItem = null }, enabled = !deleting) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CenterMessage(message: String, loading: Boolean = false) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (loading) CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EditMediaDialog(
    item: AdminMediaItem,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var title by remember(item.id) { mutableStateOf(item.title) }
    var description by remember(item.id) { mutableStateOf(item.description) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var published by remember(item.id) { mutableStateOf(item.published) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit media") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Published", modifier = Modifier.weight(1f))
                    Switch(checked = published, onCheckedChange = { published = it }, enabled = !saving)
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(title, description, category, published) }, enabled = !saving && title.isNotBlank()) { if (saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Save") } },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
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
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Column {
                    OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: $type") }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("image", "video", "audio", "document").forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { type = value; typeExpanded = false })
                        }
                    }
                }
                if (uploading) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(20.dp))
                    Text("Uploading…", modifier = Modifier.padding(start = 10.dp))
                }
            }
        },
        confirmButton = { Button(onClick = { onUpload(title, description, category, type) }, enabled = !uploading && title.isNotBlank()) { Text("Upload") } },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !uploading) { Text("Cancel") } }
    )
}
