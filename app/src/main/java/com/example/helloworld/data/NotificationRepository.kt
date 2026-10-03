package com.example.helloworld.data

import com.example.helloworld.data.offline.KfccContentRepository
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.NotificationReadEntity
import com.example.helloworld.data.bible.DailyScriptureRepository
import com.example.helloworld.data.bible.KfccBibleRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class NotificationReadRecord(
    @SerialName("notification_id") val notificationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("read_at") val readAt: String
)

class NotificationRepository {
    private val client = SupabaseProvider.client
    private val offline = KfccContentRepository(KfccDatabase.getInstance(KfccDataContext.appContext))
    private val dailyScriptureRepository = DailyScriptureRepository(KfccBibleRepository())

    fun observeNotifications(): Flow<List<AppNotification>> {
        val userId = client.auth.currentUserOrNull()?.id
        return offline.observeNotifications(userId)
    }

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        syncFromServer().getOrElse {
            val userId = client.auth.currentUserOrNull()?.id
            offline.getNotifications(userId)
        }
    }

    suspend fun syncFromServer(): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
        val rows = client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
            .filter { it.isEnabled && (it.userId == null || it.userId == userId) }
            .sortedByDescending { it.createdAt }

        val reads = client.from("notification_reads")
            .select(Columns.list("notification_id", "user_id", "read_at"))
            .decodeList<NotificationReadRecord>()
            .filter { it.userId == userId }
            .associateBy { it.notificationId }

        val resolved = rows.map { notification ->
            notification.copy(readAt = reads[notification.id]?.readAt)
        }

        val dailyScripture = getTodayScriptureNotification(userId)
        val all = if (dailyScripture != null) resolved + dailyScripture else resolved

        cache(resolved)
        all.sortedByDescending { it.createdAt }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
        if (notificationId.startsWith(DAILY_SCRIPTURE_NOTIFICATION_PREFIX)) {
            val date = notificationId.removePrefix(DAILY_SCRIPTURE_NOTIFICATION_PREFIX)
            applicationPreferences()
                .edit()
                .putBoolean(dailyScriptureReadKey(userId, date), true)
                .apply()
            return@runCatching Unit
        }
        val now = java.time.Instant.now().toString()
        client.from("notification_reads").upsert(
            NotificationReadRecord(
                notificationId = notificationId,
                userId = userId,
                readAt = now
            )
        )
        offlineCache().notificationDao().upsertRead(
            NotificationReadEntity(notificationId, userId, now)
        )
    }

    suspend fun getTodayScriptureNotificationForDelivery(): AppNotification? {
        return getTodayScriptureNotification(client.auth.currentUserOrNull()?.id)
    }

    private suspend fun getTodayScriptureNotification(userId: String?): AppNotification? {
        val translations = KfccBibleRepository().getTranslations()
        val translationId = translations.firstOrNull { it.id.equals("kjv", true) }?.id ?: "kjv"
        val selected = dailyScriptureRepository.getToday(translationId) ?: return null
        val chapter = dailyScriptureRepository.getPassage(translationId, selected) ?: return null
        val books = KfccBibleRepository().getBooks(translationId)
        val bookName = books.firstOrNull { it.id == selected.bookId }?.name ?: selected.bookId
        val verses = chapter.verses
            .filter { it.number in selected.verseStart..selected.verseEnd }
            .joinToString(" ") { "${it.number}. ${it.text}" }
        if (verses.isBlank()) return null

        val date = java.time.LocalDate.now().toString()
        val id = DAILY_SCRIPTURE_NOTIFICATION_PREFIX + date
        val readAt = if (
            userId != null &&
            applicationPreferences().getBoolean(dailyScriptureReadKey(userId, date), false)
        ) java.time.Instant.now().toString() else null
        val reference = selected.reference(bookName)
        val message = buildString {
            append(reference)
            append("\n\n")
            append(verses)
            if (selected.reflection.isNotBlank()) {
                append("\n\n")
                append(selected.reflection)
            }
        }.take(2000)

        return AppNotification(
            id = id,
            title = "Today's Scripture • ${selected.themeName}",
            message = message,
            type = "daily_scripture",
            createdAt = date + "T00:00:00Z",
            readAt = readAt
        )
    }

    private fun applicationPreferences() =
        KfccDataContext.appContext.getSharedPreferences(
            "kfcc_notification_delivery",
            android.content.Context.MODE_PRIVATE
        )

    private fun dailyScriptureReadKey(userId: String, date: String) =
        "daily_scripture_read_${userId}_$date"

    suspend fun markAllAsRead(notificationIds: List<String>): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
        val now = java.time.Instant.now().toString()
        val records = notificationIds.distinct().map {
            NotificationReadRecord(
                notificationId = it,
                userId = userId,
                readAt = now
            )
        }
        if (records.isNotEmpty()) {
            client.from("notification_reads").upsert(records)
            records.forEach {
                offlineCache().notificationDao().upsertRead(
                    NotificationReadEntity(it.notificationId, userId, it.readAt)
                )
            }
        }
    }

    suspend fun getPublicDefault(onInstall: Boolean): Result<AppNotification?> = runCatching {
        val rows = client.from("app_notifications")
            .select(Columns.list("id", "title", "message", "type", "created_at", "user_id", "is_enabled", "show_on_install", "show_on_sign_in", "updated_at"))
            .decodeList<AppNotification>()
        rows.firstOrNull {
            it.userId == null &&
                it.isEnabled &&
                if (onInstall) it.showOnInstall else it.showOnSignIn
        }
    }

    private suspend fun cache(rows: List<AppNotification>) {
        offlineCache().notificationDao().upsertAll(rows.map {
            com.example.helloworld.data.offline.NotificationEntity(
                it.id, it.userId, it.title, it.message, it.type, it.createdAt
            )
        })
    }

    private fun offlineCache() = KfccDatabase.getInstance(KfccDataContext.appContext)

    companion object {
        const val DAILY_SCRIPTURE_NOTIFICATION_PREFIX = "daily-scripture-"
    }
}
