package com.example.helloworld.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.helloworld.admin.media.AdminMediaItem
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from

/**
 * Resolves a published screen background from the media library. The URL is
 * persisted locally and Coil's disk cache stores image bytes for offline use.
 */
@Composable
fun rememberScreenBackgroundUrl(screenKey: String, backgroundType: String = "screen"): String? {
    val context = LocalContext.current.applicationContext
    val cacheKey = backgroundType + "_" + screenKey
    val categoryPrefix = if (backgroundType == "hero") "hero_bg_" else "screen_bg_"
    var imageUrl by remember(cacheKey) {
        mutableStateOf(
            context.getSharedPreferences("screen_background_cache", android.content.Context.MODE_PRIVATE)
                .getString(cacheKey, null)
        )
    }
    LaunchedEffect(cacheKey) {
        runCatching {
            val rows = SupabaseProvider.client.from("media_items")
                .select()
                .decodeList<AdminMediaItem>()
            rows.filter {
                it.category == categoryPrefix + screenKey &&
                    it.type.equals("image", ignoreCase = true) &&
                    it.published &&
                    it.url.startsWith("http")
            }.maxByOrNull { it.created_at }?.url
        }.getOrNull()?.let { url ->
            imageUrl = url
            context.getSharedPreferences("screen_background_cache", android.content.Context.MODE_PRIVATE)
                .edit().putString(cacheKey, url).apply()
        }
        imageUrl?.let { url ->
            context.imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
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
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Crop,
    overlayAlpha: Float = 0.22f
) {
    val url = rememberScreenBackgroundUrl(screenKey, backgroundType) ?: fallbackUrl
    if (!url.isNullOrBlank()) {
        Box(modifier = modifier) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentScale = contentScale
            )
            if (overlayAlpha > 0f) {
                Box(
                    modifier = androidx.compose.ui.Modifier
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
    }
}
