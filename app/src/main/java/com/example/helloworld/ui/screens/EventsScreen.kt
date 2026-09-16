package com.example.helloworld.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
                Text("Events", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(5.dp))
                Text("Stay connected with upcoming church programmes and gatherings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (events.isEmpty()) {
            item {
                Card(Modifier.padding(horizontal = 18.dp), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text("No published events yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("New church programmes will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(events.sortedBy { it.start_at }, key = { it.id }) { event -> EventCard(event) }
        }
    }
}

@Composable
private fun EventCard(event: Event) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Text(formatDate(event.start_at), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.weight(1f))
                if (event.featured) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("FEATURED", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            Column(Modifier.padding(17.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(event.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (event.location.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(6.dp)); Text(event.location, style = MaterialTheme.typography.bodyMedium) }
                }
                if (event.short_description.isNotBlank()) { Spacer(Modifier.height(9.dp)); Text(event.short_description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3) }
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(17.dp)) }, label = { Text(event.attendance_type.replace('_', ' ')) })
                    if (event.registration_url.isNotBlank()) AssistChip(onClick = {}, label = { Text("Register") })
                }
            }
        }
    }
}

private fun formatDate(value: String): String = try {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
} catch (_: Exception) { value.replace('T', ' ') }
