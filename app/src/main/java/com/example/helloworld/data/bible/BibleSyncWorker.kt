package com.example.helloworld.data.bible

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.helloworld.data.SupabaseProvider
import com.example.helloworld.data.offline.BibleBookEntity
import com.example.helloworld.data.offline.BibleTranslationEntity
import com.example.helloworld.data.offline.BibleVerseEntity
import com.example.helloworld.data.offline.KfccDatabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

class BibleSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private val db = KfccDatabase.getInstance(appContext)

    override suspend fun doWork(): Result = runCatching {
        syncTranslations()
        syncBooks()
        syncVerses()
        Result.success()
    }.getOrElse { Result.retry() }

    private suspend fun syncTranslations() {
        val rows = SupabaseProvider.client.from("bible_translations").select {
            filter { eq("is_enabled", true) }
            order("sort_order", Order.ASCENDING)
        }.decodeList<BibleTranslationRow>()

        db.bibleTranslationDao().upsertAll(rows.map {
            BibleTranslationEntity(
                id = it.id, abbreviation = it.abbreviation, name = it.name,
                language = it.language_name, isOffline = db.bibleVerseDao().count(it.id) > 0,
                isEnabled = it.is_enabled, sortOrder = it.sort_order
            )
        })
    }

    private suspend fun syncBooks() {
        val rows = SupabaseProvider.client.from("bible_books").select {
            order("book_order", Order.ASCENDING)
        }.decodeList<BibleBookRow>()
        db.bibleBookDao().upsertAll(rows.map {
            BibleBookEntity(it.id, it.name, it.abbreviation, it.testament, it.book_order, it.chapter_count)
        })
    }

    private suspend fun syncVerses() {
        val translationIds = db.bibleTranslationDao().getEnabled().map { it.id }
        for (translationId in translationIds) {
            var from = 0L
            val pageSize = 1000L
            var fetched: Int
            do {
                val rows = SupabaseProvider.client.from("bible_verses").select {
                    filter { eq("translation_id", translationId) }
                    order("book_id", Order.ASCENDING)
                    order("chapter", Order.ASCENDING)
                    order("verse", Order.ASCENDING)
                    range(from..(from + pageSize - 1))
                }.decodeList<BibleVerseRow>()
                fetched = rows.size
                if (rows.isNotEmpty()) db.bibleVerseDao().upsertAll(rows.map {
                    BibleVerseEntity(translationId, it.book_id, it.chapter, it.verse, it.text)
                })
                from += pageSize
            } while (fetched == pageSize.toInt())
        }
    }

    @Serializable private data class BibleTranslationRow(
        val id: String, val abbreviation: String, val name: String,
        val language_code: String, val language_name: String,
        val is_enabled: Boolean = true, val sort_order: Int = 0
    )
    @Serializable private data class BibleBookRow(
        val id: String, val name: String, val abbreviation: String,
        val testament: String, val book_order: Int, val chapter_count: Int
    )
    @Serializable private data class BibleVerseRow(
        val book_id: String, val chapter: Int, val verse: Int, val text: String
    )
}
