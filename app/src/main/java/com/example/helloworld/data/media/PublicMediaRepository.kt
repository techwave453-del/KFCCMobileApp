package com.example.helloworld.data.media

import com.example.helloworld.data.KfccDataResult
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from

/** Public, read-only media data source backed directly by Supabase. */
class PublicMediaRepository {
    suspend fun loadPublished(): KfccDataResult<List<PublicMediaItem>> = try {
        val items = SupabaseProvider.client.from("media_items").select {
            filter { eq("published", true) }
        }.decodeList<PublicMediaItem>()
        KfccDataResult.Success(items)
    } catch (error: Exception) {
        KfccDataResult.Failure(error.message ?: "Unable to load published media.", error)
    }
}
