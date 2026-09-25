package com.example.helloworld.data.offline

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.helloworld.admin.media.AdminMediaItem
import com.example.helloworld.data.AppNotification
import com.example.helloworld.data.EventItem
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.events.EventInput
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class MediaSyncPayload(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val published: Boolean? = null,
    val featured: Boolean? = null
)

class KfccContentSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val db = KfccDatabase.getInstance(appContext)
    private val json = Json { ignoreUnknownKeys = true }
    private val eventIdMap = mutableMapOf<Long, Long>()

    override suspend fun doWork(): Result = runCatching {
        processOutbox()
        syncSiteContent()
        syncMedia()
        syncEvents()
        syncNotifications()
        Result.success()
    }.getOrElse { Result.retry() }

    private suspend fun processOutbox() {
        val dao = db.syncOperationDao()
        dao.getPending().forEach { operation ->
            val attempts = operation.attempts + 1
            try {
                when (operation.entityType) {
                    "site_content" -> processSiteContent(operation)
                    "app_notifications" -> processNotification(operation)
                    "events" -> processEvent(operation)
                    "media_items" -> processMedia(operation)
                    else -> error("Unsupported sync entity: " + operation.entityType)
                }
                dao.delete(operation.operationId)
            } catch (e: Exception) {
                dao.updateStatus(
                    operation.operationId,
                    if (attempts >= 5) "failed" else "pending",
                    attempts,
                    e.message
                )
                if (attempts < 5) throw e
            }
        }
    }

    private suspend fun processSiteContent(operation: SyncOperationEntity) {
        val body = json.parseToJsonElement(operation.payload).jsonObject
        when (operation.operationType) {
            "UPSERT" -> SupabaseProvider.client.from("site_content").upsert(
                mapOf(
                    "key" to body.getValue("key").jsonPrimitive.content,
                    "value" to body.getValue("value").jsonPrimitive.content
                )
            )
            "DELETE" -> SupabaseProvider.client.from("site_content").delete {
                filter { eq("key", body.getValue("key").jsonPrimitive.content) }
            }
            else -> error("Unsupported site_content operation: " + operation.operationType)
        }
    }

    private suspend fun processNotification(operation: SyncOperationEntity) {
        val body = json.parseToJsonElement(operation.payload).jsonObject
        when (operation.operationType) {
            "INSERT" -> SupabaseProvider.client.from("app_notifications").insert(
                mapOf(
                    "id" to body.getValue("id").jsonPrimitive.content,
                    "title" to body.getValue("title").jsonPrimitive.content,
                    "message" to body.getValue("message").jsonPrimitive.content,
                    "type" to body.getValue("type").jsonPrimitive.content,
                    "created_at" to body.getValue("created_at").jsonPrimitive.content,
                    "user_id" to body["user_id"]?.jsonPrimitive?.content
                )
            )
            else -> error("Unsupported app_notifications operation: " + operation.operationType)
        }
    }

    private suspend fun processMedia(operation: SyncOperationEntity) {
        when (operation.operationType) {
            "UPDATE" -> {
                val id = operation.entityId?.toLongOrNull() ?: error("Missing media id")
                val payload = json.decodeFromString<MediaSyncPayload>(operation.payload)
                SupabaseProvider.client.from("media_items").update({
                    payload.title?.let { set("title", it) }
                    payload.description?.let { set("description", it) }
                    payload.category?.let { set("category", it) }
                    payload.published?.let { set("published", it) }
                    payload.featured?.let { set("featured", it) }
                }) {
                    filter { eq("id", id) }
                }
            }
            "DELETE" -> {
                val id = operation.entityId?.toLongOrNull() ?: error("Missing media id")
                SupabaseProvider.client.from("media_items").delete {
                    filter { eq("id", id) }
                }
            }
            else -> error("Unsupported media_items operation: " + operation.operationType)
        }
    }

    private suspend fun processEvent(operation: SyncOperationEntity) {
        val input = json.decodeFromString<EventInput>(operation.payload)
        when (operation.operationType) {
            "INSERT" -> {
                val created = SupabaseProvider.client.from("events")
                    .insert(input)
                    .decodeSingle<EventItem>()

                val localId = operation.entityId?.toLongOrNull()
                if (localId != null && localId < 0) {
                    db.eventDao().deleteById(localId)
                    db.eventDao().upsertAll(listOf(toEventEntity(created)))
                    eventIdMap[localId] = created.id
                    db.syncOperationDao().remapPendingEntityId(
                        entityType = "events",
                        oldEntityId = localId.toString(),
                        newEntityId = created.id.toString()
                    )
                }
            }
            "UPDATE" -> {
                val rawId = operation.entityId?.toLongOrNull() ?: error("Missing event id")
                val serverId = eventIdMap[rawId] ?: rawId
                SupabaseProvider.client.from("events").update(input) {
                    filter { eq("id", serverId) }
                }
            }
            "DELETE" -> {
                val rawId = operation.entityId?.toLongOrNull() ?: error("Missing event id")
                val serverId = eventIdMap[rawId] ?: rawId
                SupabaseProvider.client.from("events").delete {
                    filter { eq("id", serverId) }
                }
            }
            else -> error("Unsupported events operation: " + operation.operationType)
        }
    }

    private fun toEventEntity(x: EventItem) = EventEntity(
        id = x.id,
        slug = x.slug,
        title = x.title,
        category = x.category,
        shortDescription = x.shortDescription,
        description = x.description,
        image = x.image,
        flyerUrl = x.flyerUrl,
        startAt = x.startAt,
        endAt = x.endAt,
        allDay = x.allDay,
        location = x.location,
        address = x.address,
        attendanceType = x.attendanceType,
        registrationUrl = x.registrationUrl,
        contact = x.contact,
        livestreamUrl = x.livestreamUrl,
        featured = x.featured,
        status = x.status,
        displayOrder = x.displayOrder,
        createdAt = x.createdAt,
        updatedAt = x.updatedAt
    )

    private suspend fun syncSiteContent() {
        val rows = SupabaseProvider.client.from("site_content")
            .select(Columns.list("key", "value"))
            .decodeList<SiteContentRow>()
        db.siteContentDao().upsertAll(rows.map { SiteContentEntity(it.key, it.value) })
    }

    private suspend fun syncMedia() {
        val rows = SupabaseProvider.client.from("media_items")
            .select()
            .decodeList<AdminMediaItem>()
        // Keep negative local IDs while their queued mutations are still pending.
        // Server-backed rows are replaced by the authoritative snapshot.
        db.mediaItemDao().deleteServerBacked()
        db.mediaItemDao().upsertAll(rows.map {
            MediaItemEntity(
                id = it.id,
                legacyId = it.legacy_id,
                title = it.title,
                type = it.type,
                category = it.category,
                description = it.description,
                url = it.url,
                storagePath = it.storage_path,
                createdAt = it.created_at,
                published = it.published,
                thumbnailUrl = it.thumbnail_url,
                featured = it.featured
            )
        })
    }

    private suspend fun syncEvents() {
        val rows = SupabaseProvider.client.from("events").select().decodeList<EventItem>()
        // Keep negative optimistic IDs until their INSERT operation is reconciled.
        db.eventDao().deleteServerBacked()
        db.eventDao().upsertAll(rows.map {
            EventEntity(
                id = it.id, slug = it.slug, title = it.title, category = it.category,
                shortDescription = it.shortDescription, description = it.description, image = it.image,
                flyerUrl = it.flyerUrl, startAt = it.startAt, endAt = it.endAt, allDay = it.allDay,
                location = it.location, address = it.address, attendanceType = it.attendanceType,
                registrationUrl = it.registrationUrl, contact = it.contact, livestreamUrl = it.livestreamUrl,
                featured = it.featured, status = it.status, displayOrder = it.displayOrder,
                createdAt = it.createdAt, updatedAt = it.updatedAt
            )
        })
    }

    private suspend fun syncNotifications() {
        val rows = SupabaseProvider.client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
        db.notificationDao().upsertAll(rows.map {
            NotificationEntity(it.id, it.userId, it.title, it.message, it.type, it.createdAt)
        })
    }
}
