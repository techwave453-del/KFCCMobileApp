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
import java.text.SimpleDateFormat
import java.util.Locale

private val mediaCategories = listOf(
    "general", "hero", "logo", "gallery", "services", "about", "events",
    "resources", "sermons", "youth", "worship", "documents", "other"
)

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
    var selectedFileName by remember { mutableStateOf("") }
    var selectedMimeType by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<AdminMediaItem?>(null) }
    var deletingItem by remember { mutableStateOf<AdminMediaItem?>(null) }
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf("all") }
    var sortNewest by remember { mutableStateOf(true) }

    val filteredItems = remember(items, search, typeFilter, categoryFilter, sortNewest) {
        val query = search.trim().lowercase(Locale.getDefault())
        items.filter { item ->
            val matchesQuery = query.isBlank() || listOf(item.title, item.description, item.category, item.type, item.url)
                .joinToString(" ").lowercase(Locale.getDefault()).contains(query)
            val matchesType = typeFilter == "all" || item.type.equals(typeFilter, ignoreCase = true)
            val matchesCategory = categoryFilter == "all" || item.category.equals(categoryFilter, ignoreCase = true)
            matchesQuery && matchesType && matchesCategory
        }.let { list ->
            if (sortNewest) list.sortedByDescending { it.created_at } else list.sortedBy { it.title.lowercase(Locale.getDefault()) }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = viewModel.contentResolver()
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { }
        selectedUri = uri
        selectedFileName = viewModel.displayName(uri)
        selectedMimeType = resolver.getType(uri).orEmpty()
        showUploadDialog = true
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Media Center", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Upload, organize and publish the church's photos, videos, audio and documents.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(
                        onClick = viewModel::load,
                        enabled = !loading && !uploading && !saving && !deleting
                    ) { Icon(Icons.Default.Refresh, contentDescription = "Refresh media") }
                    if (canUpload) {
                        FilledTonalButton(
                            onClick = { picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) },
                            enabled = !loading && !uploading && !saving && !deleting
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Upload")
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaStatCard("${items.size}", "Library items", Icons.Default.VideoLibrary, Modifier.weight(1f))
                    MediaStatCard("${items.count { it.type.equals("image", true) }}", "Images", Icons.Default.Image, Modifier.weight(1f))
                    MediaStatCard("${items.count { it.isVideo }}", "Videos", Icons.Default.PlayCircle, Modifier.weight(1f))
                    MediaStatCard("${items.count { it.published }}", "Published", Icons.Default.Public, Modifier.weight(1f))
                }
            }

            if (uploadMessage != null || actionMessage != null || error != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uploadMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Media Library", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Showing ${filteredItems.size} of ${items.size} media ${if (items.size == 1) "item" else "items"}.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Search media") },
                            placeholder = { Text("Title, category or description") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear search") } }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterMenu("Type", typeFilter, listOf("all", "image", "video", "audio", "document"), Modifier.weight(1f)) { typeFilter = it }
                            FilterMenu("Category", categoryFilter, listOf("all") + mediaCategories, Modifier.weight(1f)) { categoryFilter = it }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (sortNewest) "Newest first" else "Name A–Z", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { sortNewest = !sortNewest }) {
                                Icon(Icons.Default.Sort, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Sort")
                            }
                        }
                    }
                }
            }

            when {
                loading && items.isEmpty() -> item { CenterMessage("Loading media library…", true) }
                error != null && items.isEmpty() -> item { CenterMessage("Unable to load the media library\nPlease refresh and try again.") }
                filteredItems.isEmpty() -> item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.height(10.dp))
                            Text(if (items.isEmpty()) "Your media library is empty" else "No matching media", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (items.isEmpty()) "Upload your first photo, video, audio file or PDF."
                                else "Try another search or filter.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (canUpload && items.isEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) }) { Text("Upload media") }
                            }
                        }
                    }
                }
                else -> items(filteredItems, key = { it.id }) { item ->
                    MediaLibraryCard(
                        item = item,
                        canEdit = canEdit,
                        canDelete = canDelete,
                        busy = saving || deleting,
                        onEdit = { editingItem = item },
                        onDelete = { deletingItem = item },
                        onPublishChanged = { published -> viewModel.save(item, item.title, item.description, item.category, published) {} },
                        onFeaturedChanged = { viewModel.setFeatured(item, it) }
                    )
                }
            }
        }
    }

    if (showUploadDialog && selectedUri != null) {
        UploadMediaDialog(
            uploading = uploading,
            fileName = selectedFileName,
            mimeType = selectedMimeType,
            onDismiss = { if (!uploading) { showUploadDialog = false; selectedUri = null } },
            onUpload = { title, description, category, type ->
                selectedUri?.let { uri ->
                    viewModel.upload(uri, title, description, category, type) {
                        showUploadDialog = false
                        selectedUri = null
                        selectedFileName = ""
                        selectedMimeType = ""
                    }
                }
            }
        )
    }

    editingItem?.let { item ->
        EditMediaDialog(item, saving, { if (!saving) editingItem = null }) { title, description, category, published ->
            viewModel.save(item, title, description, category, published) { editingItem = null }
        }
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!deleting) deletingItem = null },
            title = { Text("Delete media?") },
            text = { Text("This will permanently remove “${item.title.ifBlank { "this media item" }}” from the Media Center. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = { viewModel.delete(item) { deletingItem = null } }, enabled = !deleting) {
                    if (deleting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Delete")
                }
            },
            dismissButton = { OutlinedButton(onClick = { deletingItem = null }, enabled = !deleting) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MediaStatCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FilterMenu(label: String, selected: String, options: List<String>, modifier: Modifier, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${if (selected == "all") "All" else selected}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option == "all") "All $label" else option.replaceFirstChar { it.uppercase() }) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun MediaLibraryCard(
    item: AdminMediaItem,
    canEdit: Boolean,
    canDelete: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPublishChanged: (Boolean) -> Unit,
    onFeaturedChanged: (Boolean) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                        Icon(mediaIcon(item), contentDescription = null, modifier = Modifier.size(30.dp))
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (item.isVideo && item.featured) {
                            Icon(Icons.Default.Star, contentDescription = "Featured video", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        "${item.type.ifBlank { "media" }} · ${item.category.ifBlank { "general" }} · ${formatMediaDate(item.created_at)}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canEdit) IconButton(onClick = onEdit, enabled = !busy) { Icon(Icons.Default.Edit, contentDescription = "Edit media") }
                if (canDelete) IconButton(onClick = onDelete, enabled = !busy) { Icon(Icons.Default.Delete, contentDescription = "Delete media") }
            }

            if (item.description.isNotBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Text(item.url, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.published) "Published" else "Draft", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                if (canEdit) Switch(checked = item.published, onCheckedChange = onPublishChanged, enabled = !busy)
            }

            if (item.isVideo) {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = if (item.featured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (item.featured) "Featured Video" else "Not featured", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    if (canEdit) {
                        OutlinedButton(onClick = { onFeaturedChanged(!item.featured) }, enabled = !busy) {
                            Text(if (item.featured) "Clear" else "Set Featured")
                        }
                    }
                }
            }
        }
    }
}

private fun mediaIcon(item: AdminMediaItem) = when {
    item.isVideo -> Icons.Default.PlayCircle
    item.type.equals("audio", true) -> Icons.Default.AudioFile
    item.type.equals("document", true) -> Icons.Default.PictureAsPdf
    else -> Icons.Default.Image
}

private fun formatMediaDate(value: String): String {
    if (value.isBlank()) return ""
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(value.substringBefore('.').removeSuffix("Z"))
        parsed?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it) } ?: value.take(10)
    } catch (_: Exception) { value.take(10) }
}

@Composable
private fun CenterMessage(message: String, loading: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (loading) CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EditMediaDialog(item: AdminMediaItem, saving: Boolean, onDismiss: () -> Unit, onSave: (String, String, String, Boolean) -> Unit) {
    var title by remember(item.id) { mutableStateOf(item.title) }
    var description by remember(item.id) { mutableStateOf(item.description) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var published by remember(item.id) { mutableStateOf(item.published) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit media") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(item.url, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Published", modifier = Modifier.weight(1f)); Switch(checked = published, onCheckedChange = { published = it }, enabled = !saving) }
        } },
        confirmButton = { Button(onClick = { onSave(title, description, category, published) }, enabled = !saving && title.isNotBlank()) { if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save Changes") } },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}

@Composable
private fun UploadMediaDialog(uploading: Boolean, fileName: String, mimeType: String, onDismiss: () -> Unit, onUpload: (String, String, String, String) -> Unit) {
    var title by remember(fileName) { mutableStateOf(fileName.substringBeforeLast('.').ifBlank { "Media" }) }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("general") }
    var type by remember { mutableStateOf(if (mimeType.startsWith("video/")) "video" else if (mimeType.startsWith("audio/")) "audio" else if (mimeType == "application/pdf") "document" else "image") }
    var typeExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload media") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Selected: $fileName", style = MaterialTheme.typography.bodySmall)
            if (mimeType.isNotBlank()) Text(mimeType, style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: $type") }
                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    listOf("image", "video", "audio", "document").forEach { value ->
                        DropdownMenuItem(text = { Text(value.replaceFirstChar { it.uppercase() }) }, onClick = { type = value; typeExpanded = false })
                    }
                }
            }
            if (uploading) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp)); Text("Uploading…", modifier = Modifier.padding(start = 10.dp)) }
        } },
        confirmButton = { Button(onClick = { onUpload(title, description, category, type) }, enabled = !uploading && title.isNotBlank()) { Text("Upload media") } },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !uploading) { Text("Cancel") } }
    )
}
