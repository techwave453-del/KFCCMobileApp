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

    var currentQuickAction by remember { mutableIntStateOf(0) }

    val quickActions = remember {
        listOf(
            QuickAction("Services", "Worship times", Icons.Default.Church, "services", onOpenServices),
            QuickAction("Sermons", "Watch media", Icons.AutoMirrored.Filled.MenuBook, "sermons", onOpenSermons),
            QuickAction("Giving", "Tithes & Gift", Icons.Default.Favorite, "giving", onOpenGiving),
            QuickAction("Events", "What's on", Icons.Default.CalendarToday, "events", onOpenEvents)
        )
    }

    LaunchedEffect(Unit) {
        delay(300L)
        while (true) {
            delay(4000L)
            currentQuickAction = (currentQuickAction + 1) % quickActions.size
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
                        imageUrl = quickAccessImages[action.imageCategory]?.url
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
                FaithFeature("Bible Games", "Test your Bible knowledge", Icons.Default.SportsEsports, true, {}),
                FaithFeature("Prayer", "Build a life of prayer", Icons.Default.VolunteerActivism, true, {}),
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
                FaithFeatureCard(faithFeatures[page])
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
private fun FaithFeatureCard(feature: FaithFeature) {
    Card(
        onClick = feature.onClick,
        enabled = !feature.comingSoon,
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
