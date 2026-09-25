package com.example.helloworld.admin.media

import android.content.Context
import com.example.helloworld.data.SupabaseProvider
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.KfccOutboxRepository
import com.example.helloworld.data.offline.MediaItemEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
private data class MediaSyncPayload(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val published: Boolean? = null,
    val featured: Boolean? = null
)

/** Direct Supabase Media Center repository with local Room cache + outbox mutations. */
class MediaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val client = SupabaseProvider.client
    private val db = KfccDatabase.getInstance(appContext)
    private val outbox = KfccOutboxRepository(appContext, db)

    suspend fun load(): Result<List<AdminMediaItem>> = runCatching {
        val local = db.mediaItemDao().getAll()
        if (local.isNotEmpty()) {
            return@runCatching local.map(::toAdminItem)
        }
        client.from("media_items")
            .select()
            .decodeList<AdminMediaItem>()
            .sortedByDescending { it.created_at }
            .also { db.mediaItemDao().upsertAll(it.map(::toEntity)) }
    }

    suspend fun update(
        id: Long,
        title: String,
        description: String,
        category: String,
        published: Boolean? = null,
        featured: Boolean? = null
    ): Result<AdminMediaItem> = runCatching {
        val current = db.mediaItemDao().getAll().firstOrNull { it.id == id }
            ?: error("Media item $id is not available in the local cache.")
        val updated = current.copy(
            title = title,
            description = description,
            category = category,
            published = published ?: current.published,
            featured = featured ?: current.featured
        )
        db.mediaItemDao().upsertAll(listOf(updated))
        outbox.enqueue(
            entityType = "media_items",
            operationType = "UPDATE",
            entityId = id.toString(),
            payload = Json.encodeToString(
                MediaSyncPayload(title, description, category, published, featured)
            )
        )
        toAdminItem(updated)
    }

    suspend fun setFeatured(id: Long, featured: Boolean): Result<Unit> = runCatching {
        val current = db.mediaItemDao().getAll().firstOrNull { it.id == id }
            ?: error("Media item $id is not available in the local cache.")
        db.mediaItemDao().upsertAll(listOf(current.copy(featured = featured)))
        outbox.enqueue(
            entityType = "media_items",
            operationType = "UPDATE",
            entityId = id.toString(),
            payload = Json.encodeToString(MediaSyncPayload(featured = featured))
        )
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        db.mediaItemDao().deleteById(id)
        outbox.enqueue("media_items", "DELETE", id.toString(), "{}")
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
        val bucket = client.storage.from("media")
        bucket.upload(path, bytes) {
            upsert = true
            contentType = mimeType
        }
        val publicUrl = bucket.publicUrl(path)
        val item = mapOf(
            "title" to title,
            "description" to description,
            "category" to category,
            "type" to type,
            "url" to publicUrl,
            "storage_path" to path,
            "published" to true
        )
        client.from("media_items").insert(item).decodeSingle<AdminMediaItem>()
            .also { db.mediaItemDao().upsertAll(listOf(toEntity(it))) }
    }

    private fun toEntity(item: AdminMediaItem) = MediaItemEntity(
        id = item.id, legacyId = item.legacy_id, title = item.title, type = item.type,
        category = item.category, description = item.description, url = item.url,
        storagePath = item.storage_path, createdAt = item.created_at, published = item.published,
        thumbnailUrl = item.thumbnail_url, featured = item.featured
    )

    private fun toAdminItem(item: MediaItemEntity) = AdminMediaItem(
        id = item.id, title = item.title, type = item.type, category = item.category,
        description = item.description, url = item.url, storage_path = item.storagePath,
        featured = item.featured, published = item.published, thumbnail_url = item.thumbnailUrl,
        created_at = item.createdAt
    )
}
