package com.example.helloworld.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.delay

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

    var visibleQuickActions by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        visibleQuickActions = 0
        repeat(4) { index ->
            delay(if (index == 0) 250L else 140L)
            visibleQuickActions = index + 1
        }
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
                SectionHeader(title = "Quick Actions")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnimatedVisibility(
                            visible = visibleQuickActions >= 1,
                            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(350)) + scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(350)) + slideInVertically(animationSpec = androidx.compose.animation.core.tween(350), initialOffsetY = { it / 5 }),
                            modifier = Modifier.weight(1f)
                        ) {
                            QuickActionCard(
                                title = "Services",
                                description = "Worship times",
                                icon = Icons.Default.Church,
                                onClick = onOpenServices,
                                modifier = Modifier.fillMaxWidth(),
                                imageUrl = quickAccessImages["services"]?.url
                            )
                        }
                        AnimatedVisibility(
                            visible = visibleQuickActions >= 2,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                            modifier = Modifier.weight(1f)
                        ) {
                            QuickActionCard(
                                title = "Sermons",
                                description = "Watch media",
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                onClick = onOpenSermons,
                                modifier = Modifier.fillMaxWidth(),
                                imageUrl = quickAccessImages["sermons"]?.url
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnimatedVisibility(
                            visible = visibleQuickActions >= 3,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                            modifier = Modifier.weight(1f)
                        ) {
                            QuickActionCard(
                                title = "Giving",
                                description = "Tithes & Gift",
                                icon = Icons.Default.Favorite,
                                onClick = onOpenGiving,
                                modifier = Modifier.fillMaxWidth(),
                                imageUrl = quickAccessImages["giving"]?.url
                            )
                        }
                        AnimatedVisibility(
                            visible = visibleQuickActions >= 4,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                            modifier = Modifier.weight(1f)
                        ) {
                            QuickActionCard(
                                title = "Events",
                                description = "What's on",
                                icon = Icons.Default.CalendarToday,
                                onClick = onOpenEvents,
                                modifier = Modifier.fillMaxWidth(),
                                imageUrl = quickAccessImages["events"]?.url
                            )
                        }
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
            SectionHeader(title = "Grow in Faith")
            Spacer(Modifier.height(2.dp))

            val faithFeatures = listOf(
                FaithFeature("Daily Scripture", "Read and reflect", Icons.Default.MenuBook, false),
                FaithFeature("Bible Games", "Test your Bible knowledge", Icons.Default.SportsEsports, true),
                FaithFeature("Prayer", "Build a life of prayer", Icons.Default.VolunteerActivism, true),
                FaithFeature("Worship & Media", "Sermons and worship", Icons.Default.PlayCircle, false),
                FaithFeature("Fellowship", "Connect with the church", Icons.Default.Groups, false)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(faithFeatures) { feature ->
                    FaithFeatureCard(feature)
                }
            }
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


private data class FaithFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val comingSoon: Boolean
)

@Composable
private fun FaithFeatureCard(feature: FaithFeature) {
    Card(
        modifier = Modifier.width(190.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2
            )
            if (feature.comingSoon) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = "Coming soon",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
