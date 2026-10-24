package com.example.helloworld.ui.screens

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.helloworld.admin.media.AdminMediaItem
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from

/**
 * Resolves a published screen background from the media library. Supports both images and looping videos with instant poster fallback and smooth fade-in.
 */
@Composable
fun rememberScreenBackgroundUrl(screenKey: String, backgroundType: String = "screen"): String? {
    val context = LocalContext.current.applicationContext
    val cacheKey = backgroundType + "_" + screenKey
    val categoryPrefix = if (backgroundType == "hero") "hero_bg_" else "screen_bg_"
    var imageUrl by remember(cacheKey) {
        mutableStateOf(
            context.getSharedPreferences("screen_background_cache", Context.MODE_PRIVATE)
                .getString(cacheKey, null)
        )
    }
    LaunchedEffect(cacheKey) {
        val lookup = runCatching {
            val rows = SupabaseProvider.client.from("media_items")
                .select()
                .decodeList<AdminMediaItem>()
            rows.filter {
                it.category == categoryPrefix + screenKey &&
                    (it.type.equals("image", true) || it.type.equals("video", true)) &&
                    it.published &&
                    it.url.startsWith("http")
            }.maxByOrNull { it.created_at }?.url
        }
        lookup.onSuccess { url ->
            imageUrl = url
            val preferences = context.getSharedPreferences("screen_background_cache", Context.MODE_PRIVATE)
            if (url.isNullOrBlank()) {
                preferences.edit().remove(cacheKey).apply()
            } else {
                preferences.edit().putString(cacheKey, url).apply()
            }
        }
        imageUrl?.let { url ->
            if (!url.contains(".mp4", true) && !url.contains(".webm", true) && !url.contains(".mkv", true)) {
                context.imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build()
                )
            }
        }
    }
    return imageUrl
}

@Composable
fun ScreenBackgroundImage(
    screenKey: String,
    backgroundType: String = "screen",
    fallbackUrl: String? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    overlayAlpha: Float = 0.22f
) {
    val url = rememberScreenBackgroundUrl(screenKey, backgroundType) ?: fallbackUrl
    val isVideo = !url.isNullOrBlank() && (url.contains(".mp4", true) || url.contains(".webm", true) || url.contains(".mkv", true) || url.contains("video", true))

    if (!url.isNullOrBlank()) {
        Box(modifier = modifier) {
            if (isVideo) {
                val context = LocalContext.current
                var isPlaying by remember(url) { mutableStateOf(false) }

                // Always display high-resolution poster image instantly while video buffers
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=1200&q=80",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )

                val videoView = remember(url) {
                    VideoView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(Uri.parse(url))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.start()
                            isPlaying = true
                        }
                        start()
                    }
                }
                DisposableEffect(videoView) {
                    videoView.start()
                    onDispose {
                        videoView.stopPlayback()
                    }
                }
                AndroidView(
                    factory = { videoView },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isPlaying) 1f else 0f),
                    update = { view ->
                        view.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        if (!view.isPlaying) {
                            view.start()
                        } else {
                            isPlaying = true
                        }
                    }
                )
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            if (overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.02f),
                                    0.38f to Color.Black.copy(alpha = overlayAlpha * 0.22f),
                                    0.72f to Color.Black.copy(alpha = overlayAlpha * 0.68f),
                                    1.0f to Color.Black.copy(alpha = overlayAlpha.coerceIn(0f, 0.60f))
                                )
                            )
                        )
                )
            }
        }
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.background))
    }
}
