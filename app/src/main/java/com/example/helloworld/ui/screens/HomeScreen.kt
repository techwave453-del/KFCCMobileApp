package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.events.Event
import com.example.helloworld.ui.components.ChurchHero
import com.example.helloworld.ui.components.ChurchServiceCard
import com.example.helloworld.ui.components.SectionHeader
import com.example.helloworld.ui.theme.KFCCTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(info: ChurchInfo, events: List<Event> = emptyList(), innerPadding: PaddingValues = PaddingValues(0.dp)) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { ChurchHero(title = info.title, subtitle = info.subtitle, churchName = info.churchName, imageUrl = info.services.firstOrNull()?.imageUrl) }
        item {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 24.dp)) {
                Text("Welcome to ${info.churchName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text(info.aboutText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
            }
        }
        if (events.isNotEmpty()) {
            item { SectionHeader("Upcoming Events", modifier = Modifier.padding(horizontal = 18.dp), action = "View all") }
            items(events.sortedBy { it.start_at }.take(3)) { event ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(event.start_at.replace('T', ' '), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        if (event.short_description.isNotBlank()) { Spacer(Modifier.height(5.dp)); Text(event.short_description, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
                    }
                }
            }
        }
        item { SectionHeader("Join Us In Worship", modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 4.dp), action = "All services") }
        items(info.services) { service -> ChurchServiceCard(service = service, modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() { KFCCTheme { HomeScreen(ChurchContent.default) } }
