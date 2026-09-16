package com.example.helloworld.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.MediaItem
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        if (liveStream.enabled && liveStream.url.isNotBlank()) {
            Button(
                onClick = { showLivePlayer = true },
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Icon(Icons.Default.LiveTv, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text("LIVE", fontWeight = FontWeight.ExtraBold)
                    Text(
                        liveStream.title.ifBlank { "Watch Live Worship Service" },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Text("Media Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (mediaItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No media items available.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(mediaItems, key = { it.id }) { item ->
                    MediaGridItem(item) { selectedVideo = item }
                }
            }
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
fun MediaGridItem(item: MediaItem, onVideoClick: () -> Unit) {
    val isVideo = item.type.equals("video", ignoreCase = true)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .then(if (isVideo) Modifier.clickable(onClick = onVideoClick) else Modifier),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(128.dp).fillMaxWidth()) {
                if (item.type.equals("image", ignoreCase = true)) {
                    AsyncImage(
                        model = item.url,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        tint = Color.White
                    )
                } else if (isVideo) {
                    val thumbnail = youtubeThumbnailUrl(item.url)
                    if (thumbnail != null) {
                        AsyncImage(
                            model = thumbnail,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(52.dp))
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.68f)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play ${item.title}",
                            modifier = Modifier.padding(10.dp).size(30.dp),
                            tint = Color.White
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(item.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, fontWeight = FontWeight.Bold)
                Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
            }
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
                // Manual initialize() requires automatic initialization to be disabled.
                playerView.enableAutomaticInitialization = false
                lifecycleOwner.lifecycle.addObserver(playerView)
                playerView.initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                }, true)
            }
        },
        update = { playerView ->
            if (!playerView.isAttachedToWindow) return@AndroidView
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerView(url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var playbackError by remember(url) { mutableStateOf<String?>(null) }
    val player = remember(url) {
        ExoPlayer.Builder(context)
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1000, 5000, 500, 1000).build())
            .build()
            .apply {
                addListener(object : androidx.media3.common.Player.Listener {
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
