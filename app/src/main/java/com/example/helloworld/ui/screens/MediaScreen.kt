package com.example.helloworld.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.MediaItem
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun MediaScreen(
    mediaItems: List<MediaItem>,
    liveStream: LiveStream,
    innerPadding: PaddingValues
) {
    var selectedVideo by remember { mutableStateOf<MediaItem?>(null) }
    var showLivePlayer by remember { mutableStateOf(false) }

    val featuredMessage = remember(mediaItems) {
        mediaItems.firstOrNull { it.featured && it.type == "video" }
            ?: mediaItems.firstOrNull { it.type == "video" }
    }

    val latestMedia = remember(mediaItems) {
        mediaItems.filter { it.type == "video" }.take(10)
    }

    val galleryItems = remember(mediaItems) {
        mediaItems.filter { it.category.equals("gallery", ignoreCase = true) || it.type == "image" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 0.dp)
    ) {
        // 1. Media Hero
        item {
            MediaHeroSection()
        }

        // 2. Featured Message
        featuredMessage?.let { item ->
            item {
                FeaturedMessageSection(item) { selectedVideo = item }
            }
        }

        // 3. Live Now Banner (Styled like the design)
        if (liveStream.enabled) {
            item {
                LiveNowBanner(liveStream) { showLivePlayer = true }
            }
        }

        // 4. Explore Media
        item {
            ExploreMediaSection()
        }

        // 5. Latest Media
        item {
            SectionHeader(title = "Latest Media", subtitle = "See our newest content", onSeeAll = {})
        }
        items(latestMedia) { item ->
            LatestMediaItem(item) { selectedVideo = item }
        }

        // 6. Photo Gallery
        item {
            SectionHeader(title = "Photo Gallery", subtitle = "Moments from our church community", onSeeAll = {})
            PhotoGalleryRow(galleryItems)
        }

        // 7. Footer
        item {
            MediaFooter()
        }
    }

    selectedVideo?.let { item ->
        MediaPlayerDialog(item = item, onDismiss = { selectedVideo = null })
    }

    if (showLivePlayer) {
        LivePlayerDialog(liveStream = liveStream, onDismiss = { showLivePlayer = false })
    }
}

@Composable
private fun MediaHeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&w=1600&q=80",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "MEDIA",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Text(
                text = "Watch, listen, and experience Kingdom Fellowship wherever you are.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(Color(0xFFFFD700))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Jesus Changes Lives",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.W300,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun FeaturedMessageSection(item: MediaItem, onWatchNow: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "▶ FEATURED MESSAGE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Box(modifier = Modifier.height(200.dp).fillMaxWidth().clickable(onClick = onWatchNow)) {
                    AsyncImage(
                        model = youtubeThumbnailUrl(item.url),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(48.dp),
                            tint = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("1:02:15", color = Color.White, fontSize = 10.sp)
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Pastor John K. Mwangi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Sunday Service", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("September 7, 2026", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onWatchNow,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF000033))
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Watch Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveNowBanner(liveStream: LiveStream, onWatchLive: () -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1515162305285-0293e4767cc2?auto=format&fit=crop&w=800&q=80",
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f)))
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                    Spacer(Modifier.width(8.dp))
                    Text("LIVE NOW", color = Color.Red, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                Text("Join us for today's service", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onWatchLive,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.LiveTv, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Watch Live", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ExploreMediaSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
        Text("Explore Media", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Browse through our different media categories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryTile("Videos", Icons.Default.VideoLibrary, Color(0xFF1A237E), Modifier.weight(1f))
            CategoryTile("Sermons", Icons.Default.Mic, Color(0xFF4A148C), Modifier.weight(1f))
            CategoryTile("Worship", Icons.Default.MusicNote, Color(0xFF1B5E20), Modifier.weight(1f))
            CategoryTile("Events", Icons.Default.CalendarMonth, Color(0xFFE65100), Modifier.weight(1f))
            CategoryTile("Gallery", Icons.Default.Collections, Color(0xFF880E4F), Modifier.weight(1f))
        }
    }
}

@Composable
private fun CategoryTile(title: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.aspectRatio(0.85f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LatestMediaItem(item: MediaItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(110.dp, 66.dp).clip(RoundedCornerShape(8.dp))) {
            AsyncImage(
                model = youtubeThumbnailUrl(item.url),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.4f)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp).padding(2.dp), tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("48:22", color = Color.White, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Pastor John K. Mwangi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("September 3, 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        SuggestionChip(
            onClick = {},
            label = { Text(item.category.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.height(22.dp),
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun PhotoGalleryRow(items: List<MediaItem>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            AsyncImage(
                model = item.url,
                contentDescription = null,
                modifier = Modifier.size(140.dp, 90.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun MediaFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .height(200.dp)
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1438232992991-995b7058bbb3?auto=format&fit=crop&w=1200&q=80",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00001A).copy(alpha = 0.8f)))
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Be Part of Our Story", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("Subscribe to our channel and stay updated with our latest messages and events.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Subscribe Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onSeeAll) {
            Text("See All", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MediaPlayerDialog(item: MediaItem, onDismiss: () -> Unit) {
    VideoDialog(title = item.title, onDismiss = onDismiss) {
        if (youtubeVideoId(item.url) != null) YoutubePlayer(url = item.url)
        else ExoPlayerView(url = item.url)
    }
}

@Composable
private fun LivePlayerDialog(liveStream: LiveStream, onDismiss: () -> Unit) {
    VideoDialog(title = liveStream.title.ifBlank { "Live Worship Service" }, onDismiss = onDismiss) {
        if (youtubeVideoId(liveStream.url) != null) YoutubePlayer(url = liveStream.url)
        else ExoPlayerView(url = liveStream.url)
    }
}

@Composable
private fun VideoDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) { content() }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun YoutubePlayer(url: String) {
    val videoId = youtubeVideoId(url) ?: return
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            YouTubePlayerView(context).also { playerView ->
                playerView.enableAutomaticInitialization = false
                lifecycleOwner.lifecycle.addObserver(playerView)
                playerView.initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                }, true)
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerView(url: String) {
    val context = LocalContext.current
    var playbackError by remember(url) { mutableStateOf<String?>(null) }
    val player = remember(url) {
        ExoPlayer.Builder(context)
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1000, 5000, 500, 1000).build())
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        playbackError = error.errorCodeName
                    }
                })
                setMediaItem(PlayerMediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) { onDispose { player.release() } }

    Column(modifier = Modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            factory = { PlayerView(it).apply {
                this.player = player
                useController = true
                controllerShowTimeoutMs = 2500
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            } },
            update = { it.player = player }
        )
        playbackError?.let {
            Text(
                text = "Unable to play this video ($it).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun youtubeVideoId(url: String): String? {
    val patterns = listOf(
        Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/live/)([A-Za-z0-9_-]{11})"),
        Regex("youtube\\.com/watch\\?.*v=([A-Za-z0-9_-]{11})")
    )
    return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }
}

private fun youtubeThumbnailUrl(url: String): String? =
    youtubeVideoId(url)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
