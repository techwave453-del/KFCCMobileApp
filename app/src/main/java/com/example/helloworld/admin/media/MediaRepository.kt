package com.example.helloworld.admin.media

import android.content.Context
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.util.UUID

/** Direct Supabase Data API + Storage client for the Admin Media Center. */
class MediaRepository(context: Context) {
    private val client = SupabaseProvider.client
    private val bucket = client.storage.from("church-media")

    suspend fun load(): Result<List<AdminMediaItem>> = runCatching {
        client.from("media_items")
            .select()
            .decodeList<AdminMediaItem>()
            .sortedByDescending { it.created_at }
    }

    suspend fun update(
        id: Long,
        title: String,
        description: String,
        category: String,
        published: Boolean? = null,
        featured: Boolean? = null
    ): Result<AdminMediaItem> = runCatching {
        client.from("media_items")
            .update({
                set("title", title.trim())
                set("description", description.trim())
                set("category", category.trim())
                if (published != null) set("published", published)
                if (featured != null) set("featured", featured)
            }) {
                filter { eq("id", id) }
            }
            .decodeSingle<AdminMediaItem>()
    }

    suspend fun setFeatured(id: Long, featured: Boolean): Result<Unit> = runCatching {
        client.from("media_items")
            .update({ set("featured", featured) }) {
                filter { eq("id", id) }
            }
    }

    suspend fun delete(item: AdminMediaItem): Result<Unit> = runCatching {
        client.from("media_items").delete {
            filter { eq("id", item.id) }
        }

        item.storage_path?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { bucket.delete(path) }
        }
    }

    suspend fun upload(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        title: String,
        description: String,
        category: String,
        type: String
    ): Result<AdminMediaItem> = runCatching {
        val path = "${UUID.randomUUID()}_$fileName"

        bucket.upload(path, bytes) {
            upsert = false
            contentType = mimeType
        }

        val publicUrl = bucket.publicUrl(path)

        val item = mapOf(
            "title" to title.trim(),
            "description" to description.trim(),
            "category" to category.trim(),
            "type" to type,
            "url" to publicUrl,
            "storage_path" to path,
            "published" to true,
            "featured" to false
        )

        try {
            client.from("media_items")
                .insert(item)
                .decodeSingle<AdminMediaItem>()
        } catch (error: Exception) {
            runCatching { bucket.delete(path) }
            throw error
        }
    }
}
