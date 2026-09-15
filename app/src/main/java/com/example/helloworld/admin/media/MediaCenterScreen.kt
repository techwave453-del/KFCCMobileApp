package com.example.helloworld.admin.media

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
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
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
    canEdit: Boolean = false,
    canDelete: Boolean = false,
    viewModel: MediaCenterViewModel = viewModel()
) {
    val items by viewModel.items.collectAsStateCompat()
    val loading by viewModel.loading.collectAsStateCompat()
    val savingId by viewModel.savingId.collectAsStateCompat()
    val error by viewModel.error.collectAsStateCompat()
    var editing by remember { mutableStateOf<AdminMediaItem?>(null) }
    var deleting by remember { mutableStateOf<AdminMediaItem?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Media Center", style = MaterialTheme.typography.headlineSmall)
                    Text("Manage church images, videos, audio and external media", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = viewModel::refresh, enabled = !loading) { Icon(Icons.Default.Refresh, "Refresh media") }
            }
            Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            if (loading && items.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Loading media library…")
                }
                return@Column
            }
            if (!loading && items.isEmpty() && error == null) Text("No media has been published yet.", style = MaterialTheme.typography.bodyLarge)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (item.isVideo) Icons.Default.VideoLibrary else Icons.Default.Image, null)
                            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    if (item.featured && item.isVideo) Icon(Icons.Default.Star, "Featured video", tint = MaterialTheme.colorScheme.primary)
                                }
                                Text("${item.type.ifBlank { "media" }} · ${item.category}", style = MaterialTheme.typography.labelMedium)
                                if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(if (item.published) "Published" else "Unpublished", style = MaterialTheme.typography.labelSmall, color = if (item.published) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                            if (canEdit) IconButton(onClick = { editing = item }) { Icon(Icons.Default.Edit, "Edit media") }
                            if (canDelete) IconButton(onClick = { deleting = item }, enabled = savingId != item.id) { Icon(Icons.Default.Delete, "Delete media") }
                        }
                    }
                }
            }
        }
    }

    editing?.let { item ->
        MediaEditDialog(item, saving = savingId == item.id, onDismiss = { editing = null }, onSave = { title, description, category, featured -> viewModel.update(item, title, description, category, featured); editing = null })
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete media?") },
            text = { Text("This will remove “${item.title}” from the church media library. This action cannot be undone.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(item); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MediaEditDialog(item: AdminMediaItem, saving: Boolean, onDismiss: () -> Unit, onSave: (String, String, String, Boolean) -> Unit) {
    var title by remember(item.id) { mutableStateOf(item.title) }
    var description by remember(item.id) { mutableStateOf(item.description) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var featured by remember(item.id) { mutableStateOf(item.featured && item.isVideo) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Edit media") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                if (item.isVideo) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured video")
                }
                Text("URL: ${item.url}", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        confirmButton = { Button(onClick = { onSave(title, description, category, featured) }, enabled = title.isNotBlank() && !saving) { if (saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat(): androidx.compose.runtime.State<T> =
    androidx.compose.runtime.collectAsState(this)
