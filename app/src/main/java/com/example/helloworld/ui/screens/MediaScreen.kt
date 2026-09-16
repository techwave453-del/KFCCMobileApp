package com.example.helloworld.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.MediaItem

@Composable
fun MediaScreen(mediaItems: List<MediaItem>, liveStream: LiveStream, innerPadding: PaddingValues) {
    var selectedVideo by remember { mutableStateOf<MediaItem?>(null) }
    var showLivePlayer by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Text("Media", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("Watch messages, worship and church moments.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (liveStream.enabled && liveStream.url.isNotBlank()) {
            Card(
                onClick = { showLivePlayer = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))
            ) {
                Box(Modifier.fillMaxWidth().height(175.dp)) {
                    val liveThumbnail = youtubeThumbnailUrl(liveStream.url)
                    if (liveThumbnail != null) {
                        AsyncImage(liveThumbnail, liveStream.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(Color(0xFF0B1730)))
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .05f), Color.Black.copy(alpha = .82f)))))
                    Surface(Modifier.align(Alignment.TopStart).padding(14.dp), shape = RoundedCornerShape(50), color = Color(0xFFE53935)) {
                        Text("LIVE NOW", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(liveStream.title.ifBlank { "Live Worship Service" }, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Tap to watch live", color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodySmall)
                    }
                    Surface(Modifier.align(Alignment.Center), shape = RoundedCornerShape(50), color = Color.White.copy(alpha = .96f)) {
                        Icon(Icons.Default.PlayArrow, "Play live", Modifier.padding(13.dp).size(30.dp), tint = Color.Black)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        Text("Latest Media", Modifier.padding(horizontal = 18.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))

        if (mediaItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No media items available.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(mediaItems, key = { it.id }) { item -> MediaGridItem(item) { selectedVideo = item } }
            }
        }
    }

    selectedVideo?.let { MediaPlayerDialog(it) { selectedVideo = null } }
    if (showLivePlayer) LivePlayerDialog(liveStream) { showLivePlayer = false }
}

@Composable
fun MediaGridItem(item: MediaItem, onVideoClick: () -> Unit) {
    val isVideo = item.type.equals("video", true)
    Card(
        modifier = Modifier.fillMaxWidth().height(205.dp).then(if (isVideo) Modifier.clickable(onClick = onVideoClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(138.dp)) {
                when {
                    item.type.equals("image", true) -> AsyncImage(item.url, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    isVideo -> {
                        val thumbnail = youtubeThumbnailUrl(item.url)
                        if (thumbnail != null) AsyncImage(thumbnail, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, Modifier.size(48.dp)) }
                        Surface(Modifier.align(Alignment.Center), shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .72f)) { Icon(Icons.Default.PlayArrow, "Play ${item.title}", Modifier.padding(10.dp).size(28.dp), tint = Color.White) }
                    }
                    else -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null) }
                }
                if (item.type.equals("image", true)) Icon(Icons.Default.Image, null, Modifier.align(Alignment.BottomEnd).padding(9.dp), tint = Color.White)
            }
            Column(Modifier.padding(11.dp)) {
                Text(item.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            }
        }
    }
}

@Composable private fun MediaPlayerDialog(item: MediaItem, onDismiss: () -> Unit) = AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }, title = { Text(item.title) }, text = { if (youtubeVideoId(item.url) != null) YoutubePlayer(item.url) else ExoPlayerView(item.url) })

@Composable private fun LivePlayerDialog(liveStream: LiveStream, onDismiss: () -> Unit) = AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }, title = { Text(liveStream.title.ifBlank { "Live Worship Service" }) }, text = { if (youtubeVideoId(liveStream.url) != null) YoutubePlayer(liveStream.url) else ExoPlayerView(liveStream.url) })

@SuppressLint("SetJavaScriptEnabled")
@Composable private fun YoutubePlayer(url: String) {
    val videoId = youtubeVideoId(url) ?: return
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    youtubeEmbedHtml(videoId),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://www.youtube.com",
                youtubeEmbedHtml(videoId),
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

@Composable private fun ExoPlayerView(url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                setPlayer(player)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        update = { playerView ->
            playerView.setPlayer(player)
        }
    )
}

private fun youtubeVideoId(url: String): String? = listOf(Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/live/)([A-Za-z0-9_-]{11})"), Regex("youtube\\.com/watch\\?.*v=([A-Za-z0-9_-]{11})")).firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }
private fun youtubeThumbnailUrl(url: String): String? = youtubeVideoId(url)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
private fun youtubeEmbedHtml(videoId: String): String = """<!doctype html><html><body style='margin:0;background:#000'><iframe width='100%' height='100%' src='https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1' title='KFCC video' frameborder='0' allow='autoplay; encrypted-media; picture-in-picture; fullscreen' allowfullscreen></iframe></body></html>"""
