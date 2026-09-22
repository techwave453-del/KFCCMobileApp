package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.data.MediaItem
import com.example.helloworld.events.Event
import com.example.helloworld.ui.components.ChurchHero
import com.example.helloworld.ui.components.ChurchServiceCard
import com.example.helloworld.ui.components.ModernEventCard
import com.example.helloworld.ui.components.QuickActionCard
import com.example.helloworld.ui.components.SectionHeader
import com.example.helloworld.ui.theme.KFCCTheme

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@Composable
fun HomeScreen(
    info: ChurchInfo,
    mediaItems: List<MediaItem> = emptyList(),
    events: List<Event> = emptyList(),
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onOpenChat: () -> Unit = {},
    onOpenMedia: () -> Unit = {},
    onOpenEvents: () -> Unit = {},
    onOpenGiving: () -> Unit = {},
    onOpenSermons: () -> Unit = {},
    onOpenLive: () -> Unit = {},
    onOpenServices: () -> Unit = {},
) {
    val heroImages = remember(mediaItems) {
        mediaItems.filter { it.category.equals("hero", ignoreCase = true) && it.type.equals("image", ignoreCase = true) }
            .map { it.url }
    }

    val quickAccessImages = remember(mediaItems) {
        mediaItems.filter { it.type.equals("image", ignoreCase = true) }
            .associateBy { it.category.lowercase() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ChurchHero(
                title = info.title,
                subtitle = info.subtitle,
                backgroundImages = heroImages,
                isLive = info.liveStream.enabled,
                onLiveClick = onOpenLive
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = info.aboutTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = info.aboutText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "Quick Access")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionCard(
                            title = "Services",
                            description = "Worship times",
                            icon = Icons.Default.Church,
                            onClick = onOpenServices,
                            modifier = Modifier.weight(1f),
                            imageUrl = quickAccessImages["services"]?.url
                        )
                        QuickActionCard(
                            title = "Sermons",
                            description = "Watch media",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            onClick = onOpenSermons,
                            modifier = Modifier.weight(1f),
                            imageUrl = quickAccessImages["sermons"]?.url
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionCard(
                            title = "Giving",
                            description = "Tithes & Gift",
                            icon = Icons.Default.Favorite,
                            onClick = onOpenGiving,
                            modifier = Modifier.weight(1f),
                            imageUrl = quickAccessImages["giving"]?.url
                        )
                        QuickActionCard(
                            title = "Events",
                            description = "What's on",
                            icon = Icons.Default.CalendarToday,
                            onClick = onOpenEvents,
                            modifier = Modifier.weight(1f),
                            imageUrl = quickAccessImages["events"]?.url
                        )
                    }
                }
            }
        }

        if (events.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Upcoming Events",
                    actionText = "View all",
                    onActionClick = onOpenEvents
                )
                
                val sortedEvents = events.sortedBy { it.start_at }.take(5)
                val pagerState = rememberPagerState(pageCount = { sortedEvents.size })
                
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val event = sortedEvents[page]
                    ModernEventCard(
                        title = event.title,
                        dateString = event.start_at,
                        description = event.short_description,
                        onClick = onOpenEvents
                    )
                }
            }
        }

        item {
            SectionHeader(title = "Join Us In Worship")
        }
        
        items(info.services) { service ->
            ChurchServiceCard(
                service = service,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KFCCTheme {
        HomeScreen(ChurchContent.default)
    }
}
