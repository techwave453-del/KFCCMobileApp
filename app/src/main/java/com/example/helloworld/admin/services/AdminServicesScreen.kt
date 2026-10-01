package com.example.helloworld.admin.services
import com.example.helloworld.admin.AdminErrorMessage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.admin.content.WebsiteContentViewModel
import com.example.helloworld.data.ChurchService
import com.example.helloworld.admin.media.MediaCenterPicker

@Composable
fun AdminServicesScreen(
    modifier: Modifier = Modifier,
    viewModel: WebsiteContentViewModel,
    canEditServices: Boolean = true,
    canEditGiving: Boolean = true
) {
    val content by viewModel.content.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()
    var showEditor by remember { mutableStateOf<Int?>(null) }
    var givingUrl by remember(content.givingUrl) { mutableStateOf(content.givingUrl) }

    if (showEditor != null) {
        val index = showEditor!!
        val service = if (index == -1) ChurchService("", "", "") else content.services[index]

        ServiceEditor(
            service = service,
            onDismiss = { showEditor = null },
            onSave = { updated ->
                val newList = content.services.toMutableList()
                if (index == -1) newList.add(updated) else newList[index] = updated
                viewModel.update(content.copy(services = newList))
                showEditor = null
            }
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Services & Giving", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Manage worship services and the public giving link.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (canEditServices) {
                Button(onClick = { showEditor = -1 }, enabled = !saving) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Service")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        error?.let { message ->
            AdminErrorMessage(message = message, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (saved) {
            Text("Changes saved successfully.", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }

        if (canEditServices) {
            Text("Service Times", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (content.services.isEmpty()) {
                Text("No services defined.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    itemsIndexed(content.services) { index, service ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(service.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        service.time,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { showEditor = index },
                                    enabled = !saving
                                ) { Icon(Icons.Default.Edit, "Edit") }
                                IconButton(
                                    onClick = {
                                        val newList = content.services.toMutableList()
                                        newList.removeAt(index)
                                        viewModel.update(content.copy(services = newList))
                                    },
                                    enabled = !saving
                                ) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = viewModel::saveServices,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Save Service Times")
                }
            }
        }

        if (canEditGiving) {
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Giving", style = MaterialTheme.typography.titleLarge)
            Text(
                "Set the public online giving destination used by the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = givingUrl,
                onValueChange = { givingUrl = it },
                label = { Text("Giving URL") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveGivingUrl(givingUrl) },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Save Giving Link")
            }
        }
    }
}

@Composable
private fun ServiceEditor(
    service: ChurchService,
    onDismiss: () -> Unit,
    onSave: (ChurchService) -> Unit
) {
    var title by remember { mutableStateOf(service.title) }
    var time by remember { mutableStateOf(service.time) }
    var imageUrl by remember { mutableStateOf(service.imageUrl) }
    var showPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (service.title.isBlank()) "Add Service" else "Edit Service",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Service Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Service Time (e.g. Sundays at 10:00 AM)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Image URL") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { showPicker = true }) { Text("Pick") }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onSave(ChurchService(title.trim(), time.trim(), imageUrl.trim())) },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && time.isNotBlank()
        ) {
            Text("Confirm Details")
        }
    }

    if (showPicker) {
        MediaCenterPicker(
            title = "Select Service Image",
            allowedTypes = setOf("image"),
            onDismiss = { showPicker = false },
            onSelected = { url ->
                imageUrl = url
                showPicker = false
            }
        )
    }
}
