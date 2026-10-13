package com.example.helloworld.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.helloworld.ui.components.ModernEventCard
import com.example.helloworld.ui.components.QuickActionCard
import com.example.helloworld.ui.components.SectionHeader
import com.example.helloworld.ui.theme.KFCCTheme

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    info: ChurchInfo,
    mediaItems: List<MediaItem> = emptyList(),
    events: List<Event> = emptyList(),
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onOpenChat: () -> Unit = {},
    onOpenMedia: () -> Unit = {},
    onOpenBible: () -> Unit = {},
    onOpenBibleGames: () -> Unit = {},
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
        mediaItems
            .filter { it.type.equals("image", ignoreCase = true) && it.url.isNotBlank() }
    }

    var currentQuickAction by remember { mutableIntStateOf(0) }
    var showPrayerDialog by remember { mutableStateOf(false) }
    var prayerText by remember { mutableStateOf("") }
    var prayerSubmitted by remember { mutableStateOf(false) }

    // Keep the navigation callbacks current. A no-key remember would retain
    // the first callbacks captured when HomeScreen was initially composed.
    val quickActions = listOf(
            QuickAction("Services", "Worship times", Icons.Default.Church, "services", onOpenServices),
            QuickAction("Sermons", "Watch media", Icons.AutoMirrored.Filled.MenuBook, "sermons", onOpenSermons),
            QuickAction("Giving", "Tithes & Gift", Icons.Default.Favorite, "giving", onOpenGiving),
            QuickAction("Events", "What's on", Icons.Default.CalendarToday, "events", onOpenEvents)
    )

    LaunchedEffect(currentQuickAction, quickActions.size) {
        delay(8000L)
        currentQuickAction = (currentQuickAction + 1) % quickActions.size
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
                AnimatedContent(
                    targetState = currentQuickAction,
                    transitionSpec = {
                        (
                            fadeIn(androidx.compose.animation.core.tween(450)) +
                                scaleIn(
                                    initialScale = 0.94f,
                                    animationSpec = androidx.compose.animation.core.tween(450)
                                ) +
                                slideInVertically(
                                    animationSpec = androidx.compose.animation.core.tween(450),
                                    initialOffsetY = { it / 6 }
                                )
                        ).togetherWith(
                            fadeOut(androidx.compose.animation.core.tween(350)) +
                                scaleOut(
                                    targetScale = 0.96f,
                                    animationSpec = androidx.compose.animation.core.tween(350)
                                ) +
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 5 },
                                    animationSpec = androidx.compose.animation.core.tween(350)
                                )
                        )
                    },
                    label = "Quick action carousel",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) { index ->
                    val action = quickActions[index]
                    QuickActionCard(
                        title = action.title,
                        description = action.description,
                        icon = action.icon,
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth(),
                        imageUrl = quickActionImageUrl(
                            mediaItems = quickAccessImages,
                            category = action.imageCategory
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentQuickAction =
                                (currentQuickAction - 1 + quickActions.size) % quickActions.size
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous quick action")
                    }

                    Text(
                        text = "${currentQuickAction + 1} / ${quickActions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = {
                            currentQuickAction =
                                (currentQuickAction + 1) % quickActions.size
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next quick action")
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
                FaithFeature("Daily Scripture", "Read and reflect", Icons.Default.MenuBook, false, onOpenBible),
                FaithFeature("Bible Games", "Test your Bible knowledge", Icons.Default.SportsEsports, false, onOpenBibleGames),
                FaithFeature("Prayer", "Build a life of prayer", Icons.Default.VolunteerActivism, false, { showPrayerDialog = true }),
                FaithFeature("Worship & Media", "Sermons and worship", Icons.Default.PlayCircle, false, onOpenMedia),
                FaithFeature("Fellowship", "Connect with the church", Icons.Default.Groups, false, onOpenChat)
            )

            val faithPagerState = rememberPagerState(pageCount = { faithFeatures.size })

            LaunchedEffect(faithFeatures.size) {
                while (true) {
                    delay(3500L)
                    faithPagerState.animateScrollToPage((faithPagerState.currentPage + 1) % faithFeatures.size)
                }
            }

            HorizontalPager(
                state = faithPagerState,
                pageSize = PageSize.Fixed(190.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val feature = faithFeatures[page]
                FaithFeatureCard(
                    feature = feature,
                    imageUrl = quickActionImageUrl(quickAccessImages, feature.title, page)
                )
            }
        }

    }

    if (showPrayerDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPrayerDialog = false 
                prayerSubmitted = false
                prayerText = ""
            },
            title = { Text("Submit Prayer Request") },
            text = {
                Column {
                    if (prayerSubmitted) {
                        Text(
                            text = "🙏 Your prayer request has been received. Our pastoral prayer team will join you in faith.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text("Share your prayer request with our church community and prayer team. All requests are handled with care and confidentiality.")
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = prayerText,
                            onValueChange = { prayerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type your prayer request here...") },
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            },
            confirmButton = {
                if (prayerSubmitted) {
                    Button(onClick = {
                        showPrayerDialog = false
                        prayerSubmitted = false
                        prayerText = ""
                    }) {
                        Text("Close")
                    }
                } else {
                    Button(
                        onClick = {
                            if (prayerText.isNotBlank()) {
                                prayerSubmitted = true
                            }
                        },
                        enabled = prayerText.isNotBlank()
                    ) {
                        Text("Submit Prayer")
                    }
                }
            },
            dismissButton = {
                if (!prayerSubmitted) {
                    TextButton(onClick = { showPrayerDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KFCCTheme {
        HomeScreen(ChurchContent.default)
    }
}



private fun quickActionImageUrl(
    mediaItems: List<MediaItem>,
    category: String,
    index: Int = 0
): String {
    val lowerCategory = category.lowercase()
    val keywords = when {
        lowerCategory.contains("service") -> setOf("service", "services", "worship", "sanctuary", "sunday")
        lowerCategory.contains("sermon") || lowerCategory.contains("media") -> setOf("sermon", "sermons", "preaching", "message", "media", "watch")
        lowerCategory.contains("giving") || lowerCategory.contains("tithe") -> setOf("giving", "give", "tithe", "tithes", "offerings", "donation", "generosity")
        lowerCategory.contains("event") -> setOf("event", "events", "calendar", "gathering", "what's on")
        lowerCategory.contains("scripture") || lowerCategory.contains("bible") -> setOf("bible", "scripture", "word", "reading", "study", "psalms")
        lowerCategory.contains("game") -> setOf("game", "quiz", "youth", "fun", "kids")
        lowerCategory.contains("prayer") -> setOf("prayer", "pray", "hands", "altar", "intercession")
        lowerCategory.contains("fellowship") || lowerCategory.contains("chat") -> setOf("chat", "fellowship", "community", "connect", "group")
        else -> setOf(lowerCategory)
    }

    val matched = mediaItems.firstOrNull { item ->
        val cat = item.category.lowercase()
        val title = item.title.lowercase()
        val desc = item.description.lowercase()
        item.url.isNotBlank() && keywords.any { kw -> cat.contains(kw) || title.contains(kw) || desc.contains(kw) }
    }?.url

    if (!matched.isNullOrBlank()) return matched

    return when {
        lowerCategory.contains("service") -> "https://images.unsplash.com/photo-1438232992991-995b7058bbb3?auto=format&fit=crop&w=800&q=80"
        lowerCategory.contains("sermon") || lowerCategory.contains("media") -> "https://images.unsplash.com/photo-1519671482749-fd09be7ccebf?auto=format&fit=crop&w=800&q=80"
        lowerCategory.contains("giving") || lowerCategory.contains("tithe") -> "https://images.unsplash.com/photo-1532629345422-7515f3d16bb9?auto=format&fit=crop&w=800&q=80"
        lowerCategory.contains("event") -> "https://images.unsplash.com/photo-1511632765486-a01980e01a18?auto=format&fit=crop&w=800&q=80"
        lowerCategory.contains("scripture") || lowerCategory.contains("bible") -> "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80"
        lowerCategory.contains("game") -> "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=800&q=80"
        lowerCategory.contains("prayer") -> "https://images.unsplash.com/photo-1509099836639-18ba1795216d?auto=format&fit=crop&w=800&q=80"
        lowerCategory.contains("fellowship") || lowerCategory.contains("chat") -> "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=800&q=80"
        else -> "https://images.unsplash.com/photo-1438232992991-995b7058bbb3?auto=format&fit=crop&w=800&q=80"
    }
}

private data class QuickAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val imageCategory: String,
    val onClick: () -> Unit
)

private data class FaithFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val comingSoon: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun FaithFeatureCard(feature: FaithFeature, imageUrl: String? = null) {
    Card(
        onClick = feature.onClick,
        enabled = !feature.comingSoon,
        modifier = Modifier.width(190.dp).height(170.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (imageUrl != null) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = if (imageUrl != null) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = if (imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (imageUrl != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        minLines = 2,
                        maxLines = 2
                    )
                    if (feature.comingSoon) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (imageUrl != null) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "Coming soon",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (imageUrl != null) Color.White else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
