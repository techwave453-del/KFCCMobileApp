package com.example.helloworld.data.offline

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.helloworld.data.EventItem
import com.example.helloworld.data.MediaItem
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.AppNotification
import com.example.helloworld.events.EventInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class KfccContentSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val db = KfccDatabase.getInstance(appContext)

    override suspend fun doWork(): Result = runCatching {
        processOutbox()
        syncSiteContent()
        syncMedia()
        syncEvents()
        syncNotifications()
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun processOutbox() {
        val dao = db.syncOperationDao()
        dao.getPending().forEach { operation ->
            val attempts = operation.attempts + 1
            try {
                when (operation.entityType) {
                    "site_content" -> processSiteContent(operation)
                    "app_notifications" -> processNotification(operation)
                    "events" -> processEvent(operation)
                    else -> error("Unsupported sync entity: " + operation.entityType)
                }
                dao.delete(operation.operationId)
            } catch (e: Exception) {
                dao.updateStatus(operation.operationId, if (attempts >= 5) "failed" else "pending", attempts, e.message)
                if (attempts < 5) throw e
            }
        }
    }

    private suspend fun processSiteContent(operation: SyncOperationEntity) {
        val body = json.parseToJsonElement(operation.payload).jsonObject
        when (operation.operationType) {
            "UPSERT" -> SupabaseProvider.client.from("site_content").upsert(mapOf("key" to body.getValue("key").jsonPrimitive.content, "value" to body.getValue("value").jsonPrimitive.content))
            "DELETE" -> SupabaseProvider.client.from("site_content").delete { filter { eq("key", body.getValue("key").jsonPrimitive.content) } }
            else -> error("Unsupported site_content operation: " + operation.operationType)
        }
    }

    private suspend fun processNotification(operation: SyncOperationEntity) {
        val body = json.parseToJsonElement(operation.payload).jsonObject
        when (operation.operationType) {
            "INSERT" -> SupabaseProvider.client.from("app_notifications").insert(mapOf("title" to body.getValue("title").jsonPrimitive.content, "message" to body.getValue("message").jsonPrimitive.content, "type" to body.getValue("type").jsonPrimitive.content))
            else -> error("Unsupported app_notifications operation: " + operation.operationType)
        }
    }

    private suspend fun processEvent(operation: SyncOperationEntity) {
        val input = json.decodeFromString<EventInput>(operation.payload)
        when (operation.operationType) {
            "INSERT" -> SupabaseProvider.client.from("events").insert(input)
            "UPDATE" -> SupabaseProvider.client.from("events").update(input) { filter { eq("id", operation.entityId?.toLongOrNull() ?: error("Missing event id")) } }
            "DELETE" -> SupabaseProvider.client.from("events").delete { filter { eq("id", operation.entityId?.toLongOrNull() ?: error("Missing event id")) } }
            else -> error("Unsupported events operation: " + operation.operationType)
        }
    }
    private suspend fun syncSiteContent() {
        val rows = SupabaseProvider.client
            .from("site_content")
            .select(Columns.list("key", "value"))
            .decodeList<SiteContentRow>()
        db.siteContentDao().upsertAll(rows.map { SiteContentEntity(it.key, it.value) })
    }

    private suspend fun syncMedia() {
        val rows = SupabaseProvider.client
            .from("media_items")
            .select { filter { eq("published", true) } }
            .decodeList<MediaItem>()
        db.mediaItemDao().clear()
        db.mediaItemDao().upsertAll(rows.map {
            MediaItemEntity(
                id = it.id,
                legacyId = null,
                title = it.title,
                type = it.type,
                category = it.category,
                description = it.description,
                url = it.url,
                storagePath = null,
                createdAt = it.createdAt,
                published = true,
                thumbnailUrl = null,
                featured = it.featured
            )
        })
    }

    private suspend fun syncEvents() {
        val rows = SupabaseProvider.client
            .from("events")
            .select()
            .decodeList<EventItem>()
        db.eventDao().clear()
        db.eventDao().upsertAll(rows.map {
            EventEntity(
                id = it.id,
                slug = it.slug,
                title = it.title,
                category = it.category,
                shortDescription = it.shortDescription,
                description = it.description,
                image = it.image,
                flyerUrl = it.flyerUrl,
                startAt = it.startAt,
                endAt = it.endAt,
                allDay = it.allDay,
                location = it.location,
                address = it.address,
                attendanceType = it.attendanceType,
                registrationUrl = it.registrationUrl,
                contact = it.contact,
                livestreamUrl = it.livestreamUrl,
                featured = it.featured,
                status = it.status,
                displayOrder = it.displayOrder,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        })
    }

    private suspend fun syncNotifications() {
        val rows = SupabaseProvider.client
            .from("app_notifications")
            .select()
            .decodeList<AppNotification>()
        db.notificationDao().upsertAll(rows.map {
            NotificationEntity(it.id, it.userId, it.title, it.message, it.type, it.createdAt)
        })
    }
}
