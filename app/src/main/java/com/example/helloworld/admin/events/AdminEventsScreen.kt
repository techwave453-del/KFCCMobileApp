package com.example.helloworld.admin.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.events.Event
import com.example.helloworld.events.EventInput
import com.example.helloworld.admin.media.MediaCenterPicker

@Composable
fun AdminEventsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminEventsViewModel
) {
    val events by viewModel.events.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()
    var editing by remember { mutableStateOf<Event?>(null) }
    var formOpen by remember { mutableStateOf(false) }

    if (formOpen) {
        EventEditor(
            existing = editing,
            saving = saving,
            onBack = { formOpen = false },
            onSave = { input -> viewModel.save(editing, input); formOpen = false },
            modifier = modifier
        )
        return
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Events Management", style = MaterialTheme.typography.headlineSmall)
                Text("Create, publish, feature and maintain church events.")
            }
            Button(onClick = { editing = null; formOpen = true }) { Text("Add event") }
        }
        Spacer(Modifier.height(12.dp))
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        if (saved) {
            Text("Event saved successfully.", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(events, key = { it.id }) { event ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (event.featured) AssistChip(onClick = {}, label = { Text("Featured") })
                        }
                        Text("${event.category} · ${event.status}", style = MaterialTheme.typography.labelMedium)
                        Text(event.start_at, style = MaterialTheme.typography.bodySmall)
                        if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = event; formOpen = true }) { Text("Edit") }
                            OutlinedButton(onClick = { viewModel.delete(event) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventEditor(
    existing: Event?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (EventInput) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "General") }
    var shortDescription by remember(existing) { mutableStateOf(existing?.short_description.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var image by remember(existing) { mutableStateOf(existing?.image.orEmpty()) }
    var flyer by remember(existing) { mutableStateOf(existing?.flyer_url.orEmpty()) }
    var startAt by remember(existing) { mutableStateOf(existing?.start_at.orEmpty()) }
    var endAt by remember(existing) { mutableStateOf(existing?.end_at.orEmpty()) }
    var location by remember(existing) { mutableStateOf(existing?.location.orEmpty()) }
    var address by remember(existing) { mutableStateOf(existing?.address.orEmpty()) }
    var attendance by remember(existing) { mutableStateOf(existing?.attendance_type ?: "in_person") }
    var registration by remember(existing) { mutableStateOf(existing?.registration_url.orEmpty()) }
    var contact by remember(existing) { mutableStateOf(existing?.contact.orEmpty()) }
    var livestream by remember(existing) { mutableStateOf(existing?.livestream_url.orEmpty()) }
    var featured by remember(existing) { mutableStateOf(existing?.featured ?: false) }
    var status by remember(existing) { mutableStateOf(existing?.status ?: "draft") }
    var displayOrder by remember(existing) { mutableStateOf((existing?.display_order ?: 0).toString()) }
    var pickerTarget by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (existing == null) "Create Event" else "Edit Event", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Cancel") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            item { Field("Title", title) { title = it } }
            item { Field("Category", category) { category = it } }
            item { Field("Short description", shortDescription) { shortDescription = it } }
            item { Field("Description", description, minLines = 3) { description = it } }
            item {
                MediaUrlField("Event image", image, "Choose image", onChoose = { pickerTarget = "image" }) { image = it }
            }
            item {
                MediaUrlField("Event flyer", flyer, "Choose flyer", onChoose = { pickerTarget = "flyer" }) { flyer = it }
            }
            item { Field("Start date/time", startAt) { startAt = it } }
            item { Field("End date/time", endAt) { endAt = it } }
            item { Field("Location", location) { location = it } }
            item { Field("Address", address) { address = it } }
            item { Field("Attendance type: in_person / online / hybrid", attendance) { attendance = it } }
            item { Field("Registration URL", registration) { registration = it } }
            item { Field("Contact", contact) { contact = it } }
            item { Field("Livestream URL", livestream) { livestream = it } }
            item { Field("Status: draft / published / archived", status) { status = it } }
            item { Field("Display order", displayOrder) { displayOrder = it } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Featured event")
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
            }
            item {
                Button(
                    onClick = {
                        if (title.isBlank() || startAt.isBlank()) return@Button
                        onSave(EventInput(title, category, shortDescription, description, image, flyer, startAt, endAt.ifBlank { null }, false, location, address, attendance, registration, contact, livestream, featured, status, displayOrder.toIntOrNull() ?: 0))
                    },
                    enabled = !saving && title.isNotBlank() && startAt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { if (saving) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Save event") }
            }
        }
    }

    if (pickerTarget != null) {
        MediaCenterPicker(
            title = if (pickerTarget == "image") "Choose event image" else "Choose event flyer",
            allowedTypes = setOf("image", "document"),
            onDismiss = { pickerTarget = null },
            onSelected = { url ->
                if (pickerTarget == "image") image = url else flyer = url
                pickerTarget = null
            }
        )
    }
}

@Composable
private fun MediaUrlField(
    label: String,
    value: String,
    buttonLabel: String,
    onChoose: () -> Unit,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Field(label + " URL", value, onChange = onChange)
        OutlinedButton(onClick = onChoose, modifier = Modifier.fillMaxWidth()) { Text(buttonLabel + " from Media Center") }
    }
}

@Composable
private fun Field(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, minLines = minLines, modifier = Modifier.fillMaxWidth())
}
