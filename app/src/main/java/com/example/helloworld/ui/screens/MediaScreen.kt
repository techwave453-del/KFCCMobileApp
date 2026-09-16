package com.example.helloworld.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.MediaItem

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

        Text(
            text = "Media Center",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
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
    VideoDialog(
        title = item.title,
        onDismiss = onDismiss
    ) {
        if (youtubeVideoId(item.url) != null) YoutubePlayer(url = item.url)
        else ExoPlayerView(url = item.url)
    }
}

@Composable
private fun LivePlayerDialog(liveStream: LiveStream, onDismiss: () -> Unit) {
    VideoDialog(
        title = liveStream.title.ifBlank { "Live Worship Service" },
        onDismiss = onDismiss
    ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                // Keep the player at a true widescreen 16:9 ratio instead of
                // letting the dialog squeeze it into a small rectangular box.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    content()
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubePlayer(url: String) {
    val videoId = youtubeVideoId(url) ?: return

    // Give the WebView a real third-party HTTPS origin while keeping YouTube
    // as the iframe source. This preserves the Referer/context needed by
    // YouTube's embedded player without pretending the app is youtube.com.
    val embedBaseUrl = "https://kingdomfellowshipchristianchurch.onrender.com/"
    val iframeUrl =
        "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&enablejsapi=1&origin=https%3A%2F%2Fkingdomfellowshipchristianchurch.onrender.com"

    val html = """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                    background: #000;
                }
                iframe {
                    position: absolute;
                    inset: 0;
                    border: 0;
                    width: 100%;
                    height: 100%;
                    display: block;
                    background: #000;
                }
            </style>
        </head>
        <body>
            <iframe
                src="$iframeUrl"
                title="YouTube video player"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.BLACK)
                // YouTube's HTML5 video surface needs hardware acceleration.
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadsImagesAutomatically = true
                settings.allowContentAccess = true
                settings.allowFileAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL(embedBaseUrl, html, "text/html", "UTF-8", embedBaseUrl)
            }
        },
        update = { webView ->
            if (webView.url != embedBaseUrl) {
                webView.loadDataWithBaseURL(embedBaseUrl, html, "text/html", "UTF-8", embedBaseUrl)
            }
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
