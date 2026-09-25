package com.example.helloworld.data.bible

import com.example.helloworld.data.SupabaseProvider
import com.example.helloworld.data.offline.BibleVerseEntity
import com.example.helloworld.data.offline.KfccDatabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

class OfflineFirstBibleRepository(private val database: KfccDatabase) : BibleRepository {
    private val translations = database.bibleTranslationDao()
    private val books = database.bibleBookDao()
    private val verses = database.bibleVerseDao()

    override suspend fun getTranslations() = translations.getEnabled().map {
        BibleTranslation(it.id, it.abbreviation, it.name, it.language, it.isOffline)
    }

    override suspend fun getBooks(translationId: String) = books.getAll().map {
        BibleBook(it.id, it.name, it.abbreviation,
            if (it.testament == "NT") Testament.NEW else Testament.OLD, it.chapterCount)
    }

    override suspend fun getChapter(translationId: String, bookId: String, chapterNumber: Int): BibleChapter? {
        val local = verses.getChapter(translationId, bookId, chapterNumber)
        if (local.isNotEmpty()) return BibleChapter(bookId, chapterNumber, local.map { BibleVerse(it.verse, it.text) })

        return runCatching {
            SupabaseProvider.client.from("bible_verses").select {
                filter {
                    eq("translation_id", translationId)
                    eq("book_id", bookId)
                    eq("chapter", chapterNumber)
                }
                order("verse", Order.ASCENDING)
            }.decodeList<BibleVerseRow>().takeIf { it.isNotEmpty() }?.let { rows ->
                verses.upsertAll(rows.map {
                    BibleVerseEntity(translationId, it.book_id, it.chapter, it.verse, it.text)
                })
                BibleChapter(bookId, chapterNumber, rows.map { BibleVerse(it.verse, it.text) })
            }
        }.getOrNull()
    }

    override suspend fun search(translationId: String, query: String): List<BibleSearchResult> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()
        val bookNames = books.getAll().associate { it.id to it.name }
        val local = verses.search(translationId, normalized)
        if (local.isNotEmpty()) return local.map {
            BibleSearchResult(it.bookId, bookNames[it.bookId] ?: it.bookId, it.chapter, it.verse, it.text)
        }

        return runCatching {
            SupabaseProvider.client.from("bible_verses").select {
                filter {
                    eq("translation_id", translationId)
                    ilike("text", "%$normalized%")
                }
                order("book_id", Order.ASCENDING)
                order("chapter", Order.ASCENDING)
                order("verse", Order.ASCENDING)
                limit(50)
            }.decodeList<BibleVerseRow>().map {
                BibleSearchResult(it.book_id, bookNames[it.book_id] ?: it.book_id, it.chapter, it.verse, it.text)
            }
        }.getOrDefault(emptyList())
    }

    fun observeTranslations(): Flow<List<BibleTranslation>> =
        translations.observeEnabled().map { list ->
            list.map { BibleTranslation(it.id, it.abbreviation, it.name, it.language, it.isOffline) }
        }

    @Serializable
    private data class BibleVerseRow(
        val book_id: String,
        val chapter: Int,
        val verse: Int,
        val text: String
    )
}
