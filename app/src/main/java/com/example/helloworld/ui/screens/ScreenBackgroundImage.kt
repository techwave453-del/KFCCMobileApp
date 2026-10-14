package com.example.helloworld.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun rememberScreenBackgroundUrl(screenKey: String): String? {
    val context = LocalContext.current.applicationContext
    var imageUrl by remember(screenKey) {
        mutableStateOf(
            context.getSharedPreferences("screen_background_cache", android.content.Context.MODE_PRIVATE)
                .getString(screenKey, null)
        )
    }
    LaunchedEffect(screenKey) {
        runCatching {
            val rows = SupabaseProvider.client.from("media_items")
                .select()
                .decodeList<AdminMediaItem>()
            rows.filter {
                it.category == "screen_bg_" + screenKey &&
                    it.type.equals("image", ignoreCase = true) &&
                    it.published &&
                    it.url.startsWith("http")
            }.maxByOrNull { it.created_at }?.url
        }.getOrNull()?.let { url ->
            imageUrl = url
            context.getSharedPreferences("screen_background_cache", android.content.Context.MODE_PRIVATE)
                .edit().putString(screenKey, url).apply()
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
    fallbackUrl: String? = null,
    contentDescription: String? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Crop
) {
    val url = rememberScreenBackgroundUrl(screenKey) ?: fallbackUrl
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
