package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.events.Event
import com.example.helloworld.ui.components.ChurchHero
import com.example.helloworld.ui.components.ChurchServiceCard
import com.example.helloworld.ui.theme.KFCCTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    info: ChurchInfo,
    events: List<Event> = emptyList(),
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onOpenChat: () -> Unit = {},
    onOpenMedia: () -> Unit = {},
    onOpenEvents: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { ChurchHero(title = info.title, subtitle = info.subtitle) }

        item {
            Column(Modifier.padding(16.dp)) {
                Text(info.aboutTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(info.aboutText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Card(
                onClick = onOpenChat,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("KFCC Community Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Connect, share and chat with the KFCC community.", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onOpenChat) { Text("Open") }
                }
            }
        }

        if (events.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upcoming Events", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenEvents) {
                        Icon(Icons.Default.Event, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("View all")
                    }
                }
            }
            items(events.sortedBy { it.start_at }.take(3)) { event ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(event.start_at.replace('T', ' '), style = MaterialTheme.typography.labelMedium)
                        if (event.short_description.isNotBlank()) Text(event.short_description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Card(
                onClick = onOpenMedia,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Watch KFCC Media", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Sermons, worship and church media.", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onOpenMedia) { Text("Open") }
                }
            }
        }

        item {
            Text("Join Us In Worship", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        }
        items(info.services) { service ->
            ChurchServiceCard(service = service, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() { KFCCTheme { HomeScreen(ChurchContent.default) } }
