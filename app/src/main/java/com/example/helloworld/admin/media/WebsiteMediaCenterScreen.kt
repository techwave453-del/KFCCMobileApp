package com.example.helloworld.admin.media

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WebsiteMediaCenterScreen(
    modifier: Modifier = Modifier,
    canUpload: Boolean = false,
    canEdit: Boolean = false,
    canDelete: Boolean = false
) {
    val vm: MediaCenterViewModel = viewModel()
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    val saving by vm.saving.collectAsState()
    val uploading by vm.uploading.collectAsState()
    val deleting by vm.deleting.collectAsState()
    val error by vm.error.collectAsState()
    val message by vm.actionMessage.collectAsState()
    val uploadMessage by vm.uploadMessage.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var sortNewest by remember { mutableStateOf(true) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var selectedMime by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<AdminMediaItem?>(null) }
    var deleteItem by remember { mutableStateOf<AdminMediaItem?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try { vm.contentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        selectedUri = uri
        selectedName = vm.displayName(uri)
        selectedMime = vm.contentResolver().getType(uri).orEmpty()
        showUploadDialog = true
    }

    val visible = items.filter { item ->
        val q = query.trim()
        val matchesQuery = q.isBlank() || listOf(item.title, item.description, item.category, item.url).any { it.contains(q, true) }
        val matchesFilter = filter == "All" || item.type.equals(filter, true) || (filter == "Video" && item.isVideo)
        matchesQuery && matchesFilter
    }.let { list -> if (sortNewest) list.sortedByDescending { it.created_at } else list.sortedBy { it.title.lowercase() } }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Media Center", style = MaterialTheme.typography.headlineSmall)
                        Text("Manage images, videos, audio, documents and external media.", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = vm::load, enabled = !loading && !saving && !uploading && !deleting) { Icon(Icons.Default.Refresh, "Refresh") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("Total", items.size.toString(), Icons.Default.Collections, Modifier.weight(1f))
                    SummaryCard("Videos", items.count { it.isVideo }.toString(), Icons.Default.VideoLibrary, Modifier.weight(1f))
                    SummaryCard("Featured", items.count { it.featured }.toString(), Icons.Default.Star, Modifier.weight(1f))
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search media") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("All", "Video", "image", "audio", "document").forEach { value -> FilterChip(selected = filter.equals(value, true), onClick = { filter = value }, label = { Text(if (value == "image") "Images" else value.replaceFirstChar { it.uppercase() }) }) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sort newest first", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            Switch(sortNewest, { sortNewest = it })
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (canUpload) Button(onClick = { picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.UploadFile, null); Text("Upload file", Modifier.padding(start = 6.dp)) }
                    if (canUpload) OutlinedButton(onClick = { showUrlDialog = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Link, null); Text("Add URL", Modifier.padding(start = 6.dp)) }
                }
            }
            error?.let { err -> item { Text(err, color = MaterialTheme.colorScheme.error) } }
            message?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.primary) } }
            uploadMessage?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.primary) } }
            item {
                Text("Media Library · ${visible.size}", style = MaterialTheme.typography.titleMedium)
            }
            if (loading && items.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            else if (!loading && visible.isEmpty()) item { EmptyMedia(canUpload) { picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) } }
            else items(visible, key = { it.id }) { item ->
                MediaLibraryCard(item, canEdit, canDelete, saving || deleting, onEdit = { editItem = item }, onDelete = { deleteItem = item }, onFeatured = { vm.setFeatured(item, !item.featured) }, onPublish = { vm.save(item, item.title, item.description, item.category, it) })
            }
        }
    }

    if (showUrlDialog) UrlDialog(saving, { showUrlDialog = false }) { title, type, category, url, description, featured -> vm.addUrl(title, type, category, url, description, featured) { showUrlDialog = false } }
    if (showUploadDialog && selectedUri != null) UploadDialog(uploading, selectedName, selectedMime, { if (!uploading) { showUploadDialog = false; selectedUri = null } }) { title, description, category, type -> vm.upload(selectedUri!!, title, description, category, type) { showUploadDialog = false; selectedUri = null } }
    editItem?.let { item -> EditDialog(item, saving, { editItem = null }) { title, description, category, published -> vm.save(item, title, description, category, published) { editItem = null } } }
    deleteItem?.let { item -> AlertDialog(onDismissRequest = { if (!deleting) deleteItem = null }, title = { Text("Delete media?") }, text = { Text("Permanently remove ${item.title.ifBlank { "this item" }}?") }, confirmButton = { Button(onClick = { vm.delete(item) { deleteItem = null } }, enabled = !deleting) { Text("Delete") } }, dismissButton = { OutlinedButton(onClick = { deleteItem = null }, enabled = !deleting) { Text("Cancel") } }) }
}

@Composable private fun SummaryCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) { Card(modifier) { Column(Modifier.padding(12.dp)) { Icon(icon, null); Spacer(Modifier.height(5.dp)); Text(value, style = MaterialTheme.typography.titleLarge); Text(label, style = MaterialTheme.typography.labelSmall) } } }

@Composable private fun EmptyMedia(canUpload: Boolean, onUpload: () -> Unit) { Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Collections, null); Spacer(Modifier.height(10.dp)); Text("No media items found", style = MaterialTheme.typography.titleMedium); if (canUpload) { Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onUpload) { Text("Upload media") } } } }

@Composable private fun MediaLibraryCard(item: AdminMediaItem, canEdit: Boolean, canDelete: Boolean, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onFeatured: () -> Unit, onPublish: (Boolean) -> Unit) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (item.isVideo) Icons.Default.VideoLibrary else when (item.type.lowercase()) { "audio" -> Icons.Default.AudioFile; "document" -> Icons.Default.Description; else -> Icons.Default.Image }, null); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("${item.type.ifBlank { "media" }} · ${item.category}", style = MaterialTheme.typography.labelSmall) }; if (item.featured) AssistChip(onClick = {}, enabled = false, label = { Text("FEATURED") }, leadingIcon = { Icon(Icons.Default.Star, null) }) } ; if (item.description.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }; Spacer(Modifier.height(8.dp)); Text(item.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text(if (item.published) "Published" else "Draft", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium); if (canEdit) Switch(item.published, onPublish, enabled = !busy) }; Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { if (canEdit && item.isVideo) OutlinedButton(onClick = onFeatured, enabled = !busy) { Text(if (item.featured) "Unfeature" else "Feature video") }; if (canEdit) OutlinedButton(onClick = onEdit, enabled = !busy) { Text("Edit") }; if (canDelete) OutlinedButton(onClick = onDelete, enabled = !busy) { Text("Delete") } } } } }

@Composable private fun UrlDialog(saving: Boolean, onDismiss: () -> Unit, onSave: (String, String, String, String, String, Boolean) -> Unit) { var title by remember { mutableStateOf("") }; var type by remember { mutableStateOf("video") }; var category by remember { mutableStateOf("general") }; var url by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var featured by remember { mutableStateOf(false) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Add media URL") }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(url, { url = it }, label = { Text("Media URL") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Text("Type: $type"); Row(verticalAlignment = Alignment.CenterVertically) { Text("Featured video", Modifier.weight(1f)); Switch(featured, { featured = it }, enabled = type == "video") }; OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { Button(onClick = { onSave(title, type, category, url, description, featured) }, enabled = !saving && title.isNotBlank() && url.isNotBlank()) { Text("Save") } }, dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }) }

@Composable private fun UploadDialog(uploading: Boolean, name: String, mime: String, onDismiss: () -> Unit, onUpload: (String, String, String, String) -> Unit) { var title by remember(name) { mutableStateOf(name.substringBeforeLast('.').ifBlank { "Media" }) }; var description by remember { mutableStateOf("") }; var category by remember { mutableStateOf("general") }; val type = if (mime.startsWith("video")) "video" else if (mime.startsWith("audio")) "audio" else if (mime == "application/pdf") "document" else "image"; AlertDialog(onDismissRequest = onDismiss, title = { Text("Upload media") }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { Text(name); OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()); if (uploading) LinearProgressIndicator(Modifier.fillMaxWidth()) } }, confirmButton = { Button(onClick = { onUpload(title, description, category, type) }, enabled = !uploading && title.isNotBlank()) { Text("Upload") } }, dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !uploading) { Text("Cancel") } }) }

@Composable private fun EditDialog(item: AdminMediaItem, saving: Boolean, onDismiss: () -> Unit, onSave: (String, String, String, Boolean) -> Unit) { var title by remember(item.id) { mutableStateOf(item.title) }; var description by remember(item.id) { mutableStateOf(item.description) }; var category by remember(item.id) { mutableStateOf(item.category) }; var published by remember(item.id) { mutableStateOf(item.published) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit media") }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Row(verticalAlignment = Alignment.CenterVertically) { Text("Published", Modifier.weight(1f)); Switch(published, { published = it }, enabled = !saving) } } }, confirmButton = { Button(onClick = { onSave(title, description, category, published) }, enabled = !saving && title.isNotBlank()) { Text("Save") } }, dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }) }
