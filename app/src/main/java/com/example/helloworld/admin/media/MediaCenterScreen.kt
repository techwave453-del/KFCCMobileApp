package com.example.helloworld.admin.media

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private data class MediaCategoryOption(val value: String, val label: String)

private val MEDIA_CATEGORIES = listOf(
    MediaCategoryOption("videos", "Videos"),
    MediaCategoryOption("sermons", "Sermons"),
    MediaCategoryOption("worship", "Worship"),
    MediaCategoryOption("events", "Events"),
    MediaCategoryOption("gallery", "Gallery")
)

private fun categoryLabel(value: String): String =
    MEDIA_CATEGORIES.firstOrNull { it.value.equals(value, ignoreCase = true) }?.label
        ?: value.replaceFirstChar { it.uppercase() }


@Composable
fun MediaCenterScreen(
    modifier: Modifier = Modifier,
    canUpload: Boolean = false,
    canEdit: Boolean = false,
    canDelete: Boolean = false
) {
    val viewModel: MediaCenterViewModel = viewModel()
    val context = LocalContext.current
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
    var showUrlDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<AdminMediaItem?>(null) }
    var deletingItem by remember { mutableStateOf<AdminMediaItem?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val resolver = viewModel.contentResolver()
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers do not expose persistable permissions. The URI can
            // still be used during this activity while the provider grants access.
        }

        val name = viewModel.displayName(uri)
        val mime = resolver.getType(uri).orEmpty()
        selectedUri = uri
        selectedFileName = name
        selectedMimeType = mime
        showUploadDialog = true
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Media Center", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "${items.size} media item${if (items.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Manage church images, videos, audio and documents from one secure media library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canUpload) {
                            Button(
                                onClick = { picker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) },
                                enabled = !loading && !uploading && !saving && !deleting,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Upload")
                            }
                            OutlinedButton(
                                onClick = { showUrlDialog = true },
                                enabled = !loading && !uploading && !saving && !deleting,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add URL")
                            }
                        }
                        FilledTonalIconButton(
                            onClick = viewModel::load,
                            enabled = !loading && !uploading && !saving && !deleting
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp))
                            else Icon(Icons.Default.Refresh, contentDescription = "Refresh media")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            uploadMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }
            actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }
            error?.let { message ->
                AdminErrorMessage(message = message, modifier = Modifier.padding(bottom = 8.dp))
            }

            when {
                loading && items.isEmpty() -> CenterMessage("Loading media library…", true)
                error != null && items.isEmpty() -> AdminErrorMessage(
                    message = "Unable to load the media library\n${error ?: "Please refresh and try again."}",
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
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
                            MediaItemCard(
                                item = item,
                                canEdit = canEdit,
                                canDelete = canDelete,
                                saving = saving,
                                deleting = deleting,
                                context = context,
                                onEdit = { editingItem = item },
                                onDelete = { deletingItem = item },
                                onPublishedChange = { checked ->
                                    viewModel.save(item, item.title, item.description, item.category, checked) {}
                                },
                                onFeaturedChange = { viewModel.setFeatured(item, !item.featured) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUrlDialog) {
        AddMediaUrlDialog(
            saving = saving,
            onDismiss = { if (!saving) showUrlDialog = false },
            onAdd = { title, url, description, category, type ->
                viewModel.addUrl(title, url, description, category, type) { showUrlDialog = false }
            }
        )
    }

    if (showUploadDialog && selectedUri != null) {
        UploadMediaDialog(
            uploading = uploading,
            fileName = selectedFileName,
            mimeType = selectedMimeType,
            onDismiss = { if (!uploading) { showUploadDialog = false; selectedUri = null; selectedFileName = ""; selectedMimeType = "" } },
            onUpload = { title, description, category, type ->
                val uri = selectedUri
                if (uri != null) {
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
            confirmButton = { Button(onClick = { viewModel.delete(item) { deletingItem = null } }, enabled = !deleting) { if (deleting) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Delete") } },
            dismissButton = { OutlinedButton(onClick = { deletingItem = null }, enabled = !deleting) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MediaItemCard(
    item: AdminMediaItem,
    canEdit: Boolean,
    canDelete: Boolean,
    saving: Boolean,
    deleting: Boolean,
    context: android.content.Context,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPublishedChange: (Boolean) -> Unit,
    onFeaturedChange: () -> Unit
) {
    val icon = when {
        item.type.equals("video", true) -> Icons.Default.VideoLibrary
        item.type.equals("audio", true) -> Icons.Default.AudioFile
        item.type.equals("document", true) -> Icons.Default.Description
        else -> Icons.Default.Image
    }
    val typeLabel = item.type.ifBlank { "media" }.replaceFirstChar { it.uppercase() }
    val category = categoryLabel(item.category)
    val urlAvailable = item.url.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title.ifBlank { "Untitled media" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text(typeLabel) },
                            leadingIcon = { Icon(icon, contentDescription = null, Modifier.size(16.dp)) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(category) }
                        )
                    }
                }
                if (canEdit) {
                    IconButton(onClick = onEdit, enabled = !saving && !deleting) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit media")
                    }
                }
                if (canDelete) {
                    IconButton(onClick = onDelete, enabled = !saving && !deleting) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete media")
                    }
                }
            }

            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (urlAvailable) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        item.url,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Open")
                    }
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Media URL", item.url))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (item.published) "Published" else "Unpublished",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        if (item.published) "Visible in public media" else "Hidden from public media",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canEdit) {
                    Switch(
                        checked = item.published,
                        onCheckedChange = onPublishedChange,
                        enabled = !saving && !deleting
                    )
                }
            }

            if (item.isVideo) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (item.featured) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (item.featured) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (item.featured) "Featured video" else "Not featured",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                if (item.featured) "Shown as the featured media" else "Available as a regular video",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (canEdit) {
                            OutlinedButton(
                                onClick = onFeaturedChange,
                                enabled = !saving && !deleting
                            ) {
                                Text(if (item.featured) "Remove" else "Feature")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Operation failed",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText("Kanisa Admin Error", message)
                    )
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Copy error")
            }
        }
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
private fun EditMediaDialog(item: AdminMediaItem, saving: Boolean, onDismiss: () -> Unit, onSave: (String, String, String, Boolean) -> Unit) {
    var title by remember(item.id) { mutableStateOf(item.title) }
    var description by remember(item.id) { mutableStateOf(item.description) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var published by remember(item.id) { mutableStateOf(item.published) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit media") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Column {
            OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Category: ${categoryLabel(category)}") }
            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                MEDIA_CATEGORIES.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { category = option.value; categoryExpanded = false })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Published", modifier = Modifier.weight(1f)); Switch(checked = published, onCheckedChange = { published = it }, enabled = !saving) }
    } }, confirmButton = { Button(onClick = { onSave(title, description, category, published) }, enabled = !saving && title.isNotBlank()) { if (saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Save") } }, dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } })
}

@Composable
private fun UploadMediaDialog(uploading: Boolean, fileName: String, mimeType: String, onDismiss: () -> Unit, onUpload: (String, String, String, String) -> Unit) {
    var title by remember(fileName) { mutableStateOf(fileName.substringBeforeLast('.').ifBlank { "Media" }) }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("videos") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(if (mimeType.startsWith("video/")) "video" else if (mimeType.startsWith("audio/")) "audio" else if (mimeType == "application/pdf") "document" else "image") }
    var typeExpanded by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Upload media") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Selected: $fileName", style = MaterialTheme.typography.bodySmall)
        if (mimeType.isNotBlank()) Text(mimeType, style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Column {
            OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Category: ${categoryLabel(category)}") }
            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                MEDIA_CATEGORIES.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { category = option.value; categoryExpanded = false })
                }
            }
        }
        Column { OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: $type") }; DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) { listOf("image", "video", "audio", "document").forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { type = value; typeExpanded = false }) } } }
        if (uploading) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.height(20.dp)); Text("Uploading…", modifier = Modifier.padding(start = 10.dp)) }
    } }, confirmButton = { Button(onClick = { onUpload(title, description, category, type) }, enabled = !uploading && title.isNotBlank()) { Text("Upload") } }, dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !uploading) { Text("Cancel") } })
}

@Composable
private fun AddMediaUrlDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("videos") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("video") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Media URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add a YouTube, video, image, audio, PDF or other public media URL without uploading a file.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Media URL") }, placeholder = { Text("https://...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Column {
                    OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Category: ${categoryLabel(category)}") }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        MEDIA_CATEGORIES.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = { category = option.value; categoryExpanded = false })
                        }
                    }
                }
                Column {
                    OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: $type") }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("video", "image", "audio", "document").forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { type = value; typeExpanded = false })
                        }
                    }
                }
                if (saving) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(20.dp))
                    Text("Adding URL…", modifier = Modifier.padding(start = 10.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(title.ifBlank { "External media" }, url.trim(), description, category, type) }, enabled = !saving && url.trim().isNotBlank()) {
                if (saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Add URL")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}

private fun youtubeVideoId(url: String): String? {
    val patterns = listOf(
        Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/live/)([A-Za-z0-9_-]{11})"),
        Regex("youtube\\.com/watch\\?.*v=([A-Za-z0-9_-]{11})")
    )
    return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }
}

private fun youtubeThumbnailUrl(url: String): String? =
    youtubeVideoId(url)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
