package com.example.helloworld.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.events.Event
import com.example.helloworld.ui.components.ChurchHero
import com.example.helloworld.ui.components.ChurchServiceCard
import com.example.helloworld.ui.theme.KFCCTheme
import kotlin.math.absoluteValue

private data class QuickAccessItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val imageUrl: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    info: ChurchInfo,
    events: List<Event> = emptyList(),
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onQuickAccess: (String) -> Unit = {}
) {
    val fallbackImages = info.services.map { it.imageUrl }
    fun image(index: Int) = fallbackImages.getOrNull(index % fallbackImages.size).orEmpty()
    val quickAccess = listOf(
        QuickAccessItem("Services", "Worship, prayer and fellowship", Icons.Default.MenuBook, image(0)),
        QuickAccessItem("Sermons", "Watch and listen to messages", Icons.Default.PlayCircle, image(1)),
        QuickAccessItem("Giving", "Support the ministry", Icons.Default.VolunteerActivism, image(2)),
        QuickAccessItem("Events", "See what's coming up", Icons.Default.CalendarMonth, image(3))
    )
    val pagerState = rememberPagerState(pageCount = { quickAccess.size })

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { ChurchHero(title = info.title, subtitle = info.subtitle) }
        item {
            Column(Modifier.padding(top = 18.dp)) {
                Text("Quick Access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 28.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth().height(190.dp)
                ) { page ->
                    val item = quickAccess[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    val scale = 0.88f + (1f - pageOffset.coerceIn(0f, 1f)) * 0.12f
                    Card(
                        onClick = { onQuickAccess(item.title) },
                        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            if (item.imageUrl.isNotBlank()) {
                                AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(item.icon, contentDescription = null, modifier = Modifier.size(34.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(quickAccess.size) { index ->
                        Surface(
                            modifier = Modifier.padding(horizontal = 3.dp).size(if (index == pagerState.currentPage) 18.dp else 6.dp, 6.dp),
                            shape = RoundedCornerShape(50),
                            color = if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ) {}
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(16.dp)) {
                Text(info.aboutTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(info.aboutText, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (events.isNotEmpty()) {
            item { Text("Upcoming Events", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
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
        item { Text("Join Us In Worship", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
        items(info.services) { service -> ChurchServiceCard(service = service, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() { KFCCTheme { HomeScreen(ChurchContent.default) } }
