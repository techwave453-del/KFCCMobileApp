package com.example.helloworld.admin.media

import android.content.Context
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
private data class MediaUpdateRequest(
    val title: String,
    val description: String,
    val category: String,
    val published: Boolean? = null,
    val featured: Boolean? = null
)

/** Server-authoritative Media Center API client. */
class MediaRepository(context: Context) {
    private val client = SupabaseProvider.client

    suspend fun load(): Result<List<AdminMediaItem>> = runCatching {
        client.from("media_items")
            .select()
            .decodeList<AdminMediaItem>()
            .sortedByDescending { it.created_at }
    }

    suspend fun update(id: Long, title: String, description: String, category: String, published: Boolean? = null, featured: Boolean? = null): Result<AdminMediaItem> = runCatching {
        client.from("media_items")
            .update({
                set("title", title)
                set("description", description)
                set("category", category)
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

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        // We might want to delete from storage too if it's a hosted file, 
        // but for now just delete the DB record.
        client.from("media_items").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun upload(bytes: ByteArray, fileName: String, mimeType: String, title: String, description: String, category: String, type: String): Result<AdminMediaItem> = runCatching {
        val path = "${UUID.randomUUID()}_$fileName"
        val bucket = client.storage.from("media")
        
        bucket.upload(path, bytes) {
            upsert = true
        }
        
        val publicUrl = bucket.publicUrl(path)
        
        val item = mapOf(
            "title" to title,
            "description" to description,
            "category" to category,
            "type" to type,
            "url" to publicUrl,
            "published" to true
        )
        
        client.from("media_items").insert(item).decodeSingle<AdminMediaItem>()
    }
}
