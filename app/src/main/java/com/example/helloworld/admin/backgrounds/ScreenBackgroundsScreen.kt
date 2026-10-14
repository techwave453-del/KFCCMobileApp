package com.example.helloworld.admin.backgrounds

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
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
    BackgroundTarget("home", "Home"),
    BackgroundTarget("bible", "Bible"),
    BackgroundTarget("chat", "Chat"),
    BackgroundTarget("notifications", "Notifications"),
    BackgroundTarget("profile", "Profile")
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
    var expanded by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
            selectedUri = uri
            selectedName = vm.displayName(uri)
            showUploadDialog = true
        }
    }
    val backgrounds = items.filter { it.category.startsWith("screen_bg_") && it.type.equals("image", true) && it.published }
        .sortedByDescending { it.created_at }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Screen Backgrounds", style = MaterialTheme.typography.headlineSmall)
                        Text("Upload images and assign them to supported screens.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = targets.first { it.key == targetKey }.label,
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
                    if (canUpload) Button(onClick = { picker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }, enabled = !loading && !uploading, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (uploading) "Uploading…" else "Upload image")
                    }
                    IconButton(onClick = vm::refresh, enabled = !loading && !uploading) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp)) else Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
                Text("PNG, JPG and WebP are supported. Published backgrounds are cached on devices after download for offline display.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                val screenKey = item.category.removePrefix("screen_bg_")
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = item.url, contentDescription = item.title, modifier = Modifier.size(72.dp), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(targets.firstOrNull { it.key == screenKey }?.label ?: screenKey, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (canEdit) TextButton(onClick = {
                            vm.save(item, item.title, item.description, item.category, false) {}
                        }) { Text("Unpublish") }
                    }
                }
            }
            if (backgrounds.isEmpty()) item { Text("No backgrounds yet. Upload your WordArt PNG to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    if (showUploadDialog) AlertDialog(
        onDismissRequest = { showUploadDialog = false },
        title = { Text("Upload screen background") },
        text = { Text("Assign " + selectedName + " to " + targets.first { it.key == targetKey }.label + "?") },
        confirmButton = {
            TextButton(enabled = selectedUri != null && !uploading, onClick = {
                val uri = selectedUri ?: return@TextButton
                val name = selectedName.substringBeforeLast('.', selectedName)
                val screen = targetKey
                vm.upload(uri, name, "screen=" + screen, "screen_bg_" + screen, "image") {
                    showUploadDialog = false
                    selectedUri = null
                }
            }) { Text("Upload and publish") }
        },
        dismissButton = { TextButton(onClick = { showUploadDialog = false }) { Text("Cancel") } }
    )
}
