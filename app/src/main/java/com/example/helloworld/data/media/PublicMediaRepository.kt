package com.example.helloworld.data.media

import com.example.helloworld.data.KfccDataResult
import com.example.helloworld.data.SupabaseProvider

/**
 * Public, read-only media data source.
 *
 * This repository intentionally only reads published media. Administrative
 * mutations remain on the authenticated server API so the APK never receives
 * privileged database credentials.
 */
class PublicMediaRepository {
    suspend fun loadPublished(): KfccDataResult<List<PublicMediaItem>> {
        return try {
            val items = SupabaseProvider.client
                .from("media_items")
                .select()
                .decodeList<PublicMediaItem>()

            KfccDataResult.Success(items)
        } catch (error: Exception) {
            KfccDataResult.Failure(
                message = error.message ?: "Unable to load published media.",
                cause = error,
            )
        }
    }
}
