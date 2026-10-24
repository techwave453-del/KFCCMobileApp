package com.example.helloworld.admin.backgrounds

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.helloworld.admin.media.MediaCenterViewModel

private data class BackgroundTarget(val key: String, val label: String)
private val targets = listOf(
    BackgroundTarget("bible_games", "Bible Games"),
    BackgroundTarget("bible_chapter", "Bible Chapters & Verses")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenBackgroundsScreen(modifier: Modifier = Modifier, canUpload: Boolean = false, canEdit: Boolean = false) {
    val vm: MediaCenterViewModel = viewModel()
    val context = LocalContext.current
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    val uploading by vm.uploading.collectAsState()
    val error by vm.error.collectAsState()
    val message by vm.actionMessage.collectAsState()
    val uploadMessage by vm.uploadMessage.collectAsState()
    var targetKey by remember { mutableStateOf("bible_games") }
    var backgroundKind by remember { mutableStateOf("screen") }
    var expanded by remember { mutableStateOf(false) }
    var kindExpanded by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showWhiteBackgroundConfirm by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
            selectedUri = uri
            selectedName = vm.displayName(uri)
            showUploadDialog = true
        }
    }
    val backgrounds = items.filter {
        (it.category.startsWith("screen_bg_") || it.category.startsWith("hero_bg_")) &&
            (it.type.equals("image", true) || it.type.equals("video", true)) && it.published
    }.sortedByDescending { it.created_at }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Screen Backgrounds", style = MaterialTheme.typography.headlineSmall)
                        Text("Manage image and looping video backgrounds for Bible games and chapters.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = kindExpanded, onExpandedChange = { kindExpanded = it }) {
                    OutlinedTextField(
                        value = if (backgroundKind == "screen") "Full-screen background" else "Hero / card background",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Background type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(kindExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = kindExpanded, onDismissRequest = { kindExpanded = false }) {
                        DropdownMenuItem(text = { Text("Full-screen background") }, onClick = { backgroundKind = "screen"; kindExpanded = false })
                        DropdownMenuItem(text = { Text("Hero / card background") }, onClick = { backgroundKind = "hero"; kindExpanded = false })
                    }
                }
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = targets.firstOrNull { it.key == targetKey }?.label ?: "Bible Games",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Screen") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        targets.forEach { target ->
                            DropdownMenuItem(text = { Text(target.label) }, onClick = { targetKey = target.key; expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canUpload) Button(onClick = { picker.launch(arrayOf("image/png", "image/jpeg", "image/webp", "video/mp4", "video/webm", "video/mkv")) }, enabled = !loading && !uploading, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (uploading) "Uploading…" else "Upload image or video")
                    }
                    IconButton(onClick = vm::refresh, enabled = !loading && !uploading) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp)) else Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
                if (canEdit && backgroundKind == "screen") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showWhiteBackgroundConfirm = true },
                        enabled = !loading && !uploading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove custom background (use default)")
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        uploadMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        Text("Published backgrounds", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(backgrounds.size) { index ->
                val item = backgrounds[index]
                val isHero = item.category.startsWith("hero_bg_")
                val prefix = if (isHero) "hero_bg_" else "screen_bg_"
                val screenKey = item.category.removePrefix(prefix)
                val isVideo = item.type.equals("video", true) || item.url.contains(".mp4", true)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            if (isVideo) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            } else {
                                AsyncImage(model = item.url, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(targets.firstOrNull { it.key == screenKey }?.label ?: screenKey, style = MaterialTheme.typography.bodyMedium)
                            Text(if (isVideo) "Looping video background" else if (isHero) "Hero / card background" else "Full-screen background", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (canEdit) TextButton(onClick = {
                            vm.save(item, item.title, item.description, item.category, false) {}
                        }) { Text("Unpublish") }
                    }
                }
            }
            if (backgrounds.isEmpty()) item { Text("No custom backgrounds yet. Choose Bible Games or Bible Chapters and upload an image or video.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    if (showWhiteBackgroundConfirm) AlertDialog(
        onDismissRequest = { showWhiteBackgroundConfirm = false },
        title = { Text("Remove custom background?") },
        text = { Text("Remove the published background for " + (targets.firstOrNull { it.key == targetKey }?.label ?: targetKey) + "? The screen will return to its default theme background.") },
        confirmButton = {
            TextButton(onClick = {
                val toUnpublish = items.filter { it.category == "screen_bg_" + targetKey && it.published }
                showWhiteBackgroundConfirm = false
                if (toUnpublish.isEmpty()) {
                    vm.refresh()
                } else {
                    fun unpublishNext(index: Int) {
                        if (index >= toUnpublish.size) {
                            vm.refresh()
                        } else {
                            val item = toUnpublish[index]
                            vm.save(item, item.title, item.description, item.category, false) {
                                unpublishNext(index + 1)
                            }
                        }
                    }
                    unpublishNext(0)
                }
            }) { Text("Remove background") }
        },
        dismissButton = { TextButton(onClick = { showWhiteBackgroundConfirm = false }) { Text("Cancel") } }
    )

    if (showUploadDialog) AlertDialog(
        onDismissRequest = { showUploadDialog = false },
        title = { Text(if (backgroundKind == "screen") "Upload full-screen background" else "Upload hero / card background") },
        text = { Text("Assign " + selectedName + " to " + (targets.firstOrNull { it.key == targetKey }?.label ?: targetKey) + " as a " + (if (backgroundKind == "screen") "full-screen background" else "hero / card background") + "?") },
        confirmButton = {
            TextButton(enabled = selectedUri != null && !uploading, onClick = {
                val uri = selectedUri ?: return@TextButton
                val name = selectedName.substringBeforeLast('.', selectedName)
                val screen = targetKey
                val prefix = if (backgroundKind == "screen") "screen_bg_" else "hero_bg_"
                val type = if (selectedName.contains(".mp4", true) || selectedName.contains(".webm", true) || selectedName.contains(".mkv", true)) "video" else "image"
                vm.upload(uri, name, "screen=" + screen + ";placement=" + backgroundKind, prefix + screen, type) {
                    showUploadDialog = false
                    selectedUri = null
                }
            }) { Text("Upload and publish") }
        },
        dismissButton = { TextButton(onClick = { showUploadDialog = false }) { Text("Cancel") } }
    )
}
