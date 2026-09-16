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
import androidx.compose.ui.graphics.vector.ImageVector
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

    val mediaItems by vm.items.collectAsState()
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
    var showUploadDialog by remember { mutableStateOf(false) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var selectedMime by remember { mutableStateOf("") }

    var editItem by remember { mutableStateOf<AdminMediaItem?>(null) }
    var deleteItem by remember { mutableStateOf<AdminMediaItem?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            vm.contentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        selectedUri = uri
        selectedName = vm.displayName(uri)
        selectedMime = vm.contentResolver().getType(uri).orEmpty()
        showUploadDialog = true
    }

    val visibleItems = mediaItems
        .filter { item ->
            val search = query.trim()

            val matchesQuery =
                search.isBlank() ||
                    item.title.contains(search, ignoreCase = true) ||
                    item.description.contains(search, ignoreCase = true) ||
                    item.category.contains(search, ignoreCase = true) ||
                    item.url.contains(search, ignoreCase = true)

            val matchesFilter =
                filter == "All" ||
                    item.type.equals(filter, ignoreCase = true) ||
                    (filter.equals("Video", ignoreCase = true) && item.isVideo)

            matchesQuery && matchesFilter
        }
        .let { list ->
            if (sortNewest) {
                list.sortedByDescending { it.created_at }
            } else {
                list.sortedBy { it.title.lowercase() }
            }
        }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Media Center",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "Manage images, videos, audio, documents and external media.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    IconButton(
                        onClick = vm::load,
                        enabled = !loading &&
                            !saving &&
                            !uploading &&
                            !deleting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        label = "Total",
                        value = mediaItems.size.toString(),
                        icon = Icons.Default.Collections,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        label = "Videos",
                        value = mediaItems.count { it.isVideo }.toString(),
                        icon = Icons.Default.VideoLibrary,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        label = "Featured",
                        value = mediaItems.count { it.featured }.toString(),
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = {
                                Text("Search media")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "All",
                                "Video",
                                "image",
                                "audio",
                                "document"
                            ).forEach { value ->

                                FilterChip(
                                    selected = filter.equals(
                                        value,
                                        ignoreCase = true
                                    ),
                                    onClick = {
                                        filter = value
                                    },
                                    label = {
                                        Text(
                                            if (value == "image") {
                                                "Images"
                                            } else {
                                                value.replaceFirstChar {
                                                    it.uppercase()
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sort newest first",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium
                            )

                            Switch(
                                checked = sortNewest,
                                onCheckedChange = {
                                    sortNewest = it
                                }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canUpload) {
                        Button(
                            onClick = {
                                picker.launch(
                                    arrayOf(
                                        "image/*",
                                        "video/*",
                                        "audio/*",
                                        "application/pdf"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null
                            )

                            Text(
                                text = "Upload file",
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                showUrlDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null
                            )

                            Text(
                                text = "Add URL",
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }

            error?.let { errorMessage ->
                item {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            message?.let { actionMessage ->
                item {
                    Text(
                        text = actionMessage,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            uploadMessage?.let { uploadStatus ->
                item {
                    Text(
                        text = uploadStatus,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Text(
                    text = "Media Library · ${visibleItems.size}",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (loading && mediaItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (!loading && visibleItems.isEmpty()) {
                item {
                    EmptyMedia(
                        canUpload = canUpload,
                        onUpload = {
                            picker.launch(
                                arrayOf(
                                    "image/*",
                                    "video/*",
                                    "audio/*",
                                    "application/pdf"
                                )
                            )
                        }
                    )
                }
            } else {
                items(
                    items = visibleItems,
                    key = { it.id }
                ) { mediaItem ->

                    MediaLibraryCard(
                        item = mediaItem,
                        canEdit = canEdit,
                        canDelete = canDelete,
                        busy = saving || deleting,

                        onEdit = {
                            editItem = mediaItem
                        },

                        onDelete = {
                            deleteItem = mediaItem
                        },

                        onFeatured = {
                            vm.setFeatured(
                                mediaItem,
                                !mediaItem.featured
                            )
                        },

                        onPublish = { published ->
                            vm.save(
                                mediaItem,
                                mediaItem.title,
                                mediaItem.description,
                                mediaItem.category,
                                published
                            ) {
                                // Save completed.
                            }
                        }
                    )
                }
            }
        }
    }

    if (showUrlDialog) {
        UrlDialog(
            saving = saving,
            onDismiss = {
                if (!saving) {
                    showUrlDialog = false
                }
            },
            onSave = { title, type, category, url, description, featured ->
                vm.addUrl(
                    title = title,
                    type = type,
                    category = category,
                    url = url,
                    description = description,
                    featured = featured
                ) {
                    showUrlDialog = false
                }
            }
        )
    }

    if (showUploadDialog && selectedUri != null) {
        UploadDialog(
            uploading = uploading,
            name = selectedName,
            mime = selectedMime,

            onDismiss = {
                if (!uploading) {
                    showUploadDialog = false
                    selectedUri = null
                }
            },

            onUpload = { title, description, category, type ->
                vm.upload(
                    selectedUri!!,
                    title,
                    description,
                    category,
                    type
                ) {
                    showUploadDialog = false
                    selectedUri = null
                }
            }
        )
    }

    editItem?.let { mediaItem ->
        EditDialog(
            item = mediaItem,
            saving = saving,

            onDismiss = {
                if (!saving) {
                    editItem = null
                }
            },

            onSave = { title, description, category, published ->
                vm.save(
                    mediaItem,
                    title,
                    description,
                    category,
                    published
                ) {
                    editItem = null
                }
            }
        )
    }

    deleteItem?.let { mediaItem ->
        AlertDialog(
            onDismissRequest = {
                if (!deleting) {
                    deleteItem = null
                }
            },

            title = {
                Text("Delete media?")
            },

            text = {
                Text(
                    "Permanently remove ${
                        mediaItem.title.ifBlank {
                            "this item"
                        }
                    }?"
                )
            },

            confirmButton = {
                Button(
                    onClick = {
                        vm.delete(mediaItem) {
                            deleteItem = null
                        }
                    },
                    enabled = !deleting
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {
                OutlinedButton(
                    onClick = {
                        deleteItem = null
                    },
                    enabled = !deleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EmptyMedia(
    canUpload: Boolean,
    onUpload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Collections,
            contentDescription = null
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "No media items found",
            style = MaterialTheme.typography.titleMedium
        )

        if (canUpload) {
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onUpload
            ) {
                Text("Upload media")
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
    onFeatured: () -> Unit,
    onPublish: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (item.isVideo) {
                            Icons.Default.VideoLibrary
                        } else {
                            when (item.type.lowercase()) {
                                "audio" -> Icons.Default.AudioFile
                                "document" -> Icons.Default.Description
                                else -> Icons.Default.Image
                            }
                        },
                    contentDescription = null
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = item.title.ifBlank {
                            item.url
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${item.type.ifBlank { "media" }} · ${item.category}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (item.featured) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text("FEATURED")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.url,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.published) {
                        "Published"
                    } else {
                        "Draft"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium
                )

                if (canEdit) {
                    Switch(
                        checked = item.published,
                        onCheckedChange = onPublish,
                        enabled = !busy
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (canEdit && item.isVideo) {
                    OutlinedButton(
                        onClick = onFeatured,
                        enabled = !busy
                    ) {
                        Text(
                            (if (item.featured) {
                                "Unfeature"
                            } else {
                                "Feature video"
                            })
                        )
                    }
                }

                if (canEdit) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !busy
                    ) {
                        Text("Edit")
                    }
                }

                if (canDelete) {
                    OutlinedButton(
                        onClick = onDelete,
                        enabled = !busy
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun UrlDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        String,
        String,
        Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("video") }
    var category by remember { mutableStateOf("general") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var featured by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Add media URL")
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text("Title")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = {
                        Text("Media URL")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = {
                        Text("Category")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Type: $type")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Featured video",
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = featured,
                        onCheckedChange = {
                            featured = it
                        },
                        enabled = type == "video"
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text("Description")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        type,
                        category,
                        url,
                        description,
                        featured
                    )
                },
                enabled = !saving &&
                    title.isNotBlank() &&
                    url.isNotBlank()
            ) {
                Text("Save")
            }
        },

        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !saving
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UploadDialog(
    uploading: Boolean,
    name: String,
    mime: String,
    onDismiss: () -> Unit,
    onUpload: (
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var title by remember(name) {
        mutableStateOf(
            name.substringBeforeLast('.')
                .ifBlank { "Media" }
        )
    }

    var description by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("general")
    }

    val type =
        if (mime.startsWith("video")) {
            "video"
        } else if (mime.startsWith("audio")) {
            "audio"
        } else if (mime == "application/pdf") {
            "document"
        } else {
            "image"
        }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Upload media")
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(name)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text("Title")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = {
                        Text("Category")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text("Description")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (uploading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    onUpload(
                        title,
                        description,
                        category,
                        type
                    )
                },
                enabled = !uploading &&
                    title.isNotBlank()
            ) {
                Text("Upload")
            }
        },

        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !uploading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditDialog(
    item: AdminMediaItem,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        String,
        String,
        String,
        Boolean
    ) -> Unit
) {
    var title by remember(item.id) {
        mutableStateOf(item.title)
    }

    var description by remember(item.id) {
        mutableStateOf(item.description)
    }

    var category by remember(item.id) {
        mutableStateOf(item.category)
    }

    var published by remember(item.id) {
        mutableStateOf(item.published)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Edit media")
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text("Title")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text("Description")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = {
                        Text("Category")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Published",
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = published,
                        onCheckedChange = {
                            published = it
                        },
                        enabled = !saving
                    )
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        description,
                        category,
                        published
                    )
                },
                enabled = !saving &&
                    title.isNotBlank()
            ) {
                Text("Save")
            }
        },

        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !saving
            ) {
                Text("Cancel")
            }
        }
    )
}
