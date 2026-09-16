package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.events.Event
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun EventsScreen(events: List<Event>, innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Events", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Upcoming church programmes and gatherings", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        if (events.isEmpty()) item { Text("No published events yet.", style = MaterialTheme.typography.bodyLarge) }
        items(events.sortedBy { it.start_at }) { event -> EventCard(event) }
    }
}

@Composable
private fun EventCard(event: Event) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (event.featured) Text("FEATURED", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("${formatDate(event.start_at)} · ${event.category}", style = MaterialTheme.typography.labelLarge)
            if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.bodyMedium)
            if (event.short_description.isNotBlank()) {
                Spacer(Modifier.height(8.dp)); Text(event.short_description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(event.attendance_type.replace('_', ' ')) })
                if (event.registration_url.isNotBlank()) AssistChip(onClick = {}, label = { Text("Register") })
            }
        }
    }
}

private fun formatDate(value: String): String = try {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
} catch (_: Exception) { value.replace('T', ' ') }
