package com.example.helloworld.data.bible

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

interface BibleRepository {
    suspend fun getTranslations(): List<BibleTranslation>
    suspend fun getBooks(translationId: String): List<BibleBook>
    suspend fun getChapter(
        translationId: String,
        bookId: String,
        chapterNumber: Int
    ): BibleChapter?
    suspend fun search(
        translationId: String,
        query: String
    ): List<BibleSearchResult>
}

@Serializable
private data class BibleTranslationRow(
    val id: String,
    val abbreviation: String,
    val name: String,
    val language_code: String,
    val language_name: String,
    val description: String = "",
    val is_enabled: Boolean = true
)

@Serializable
private data class BibleBookRow(
    val id: String,
    val name: String,
    val abbreviation: String,
    val testament: String,
    val book_order: Int,
    val chapter_count: Int
)

@Serializable
private data class BibleVerseRow(
    val book_id: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class BibleSearchResult(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

class KfccBibleRepository : BibleRepository {

    override suspend fun getTranslations(): List<BibleTranslation> {
        return SupabaseProvider.client
            .from("bible_translations")
            .select {
                filter { eq("is_enabled", true) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<BibleTranslationRow>()
            .map {
                BibleTranslation(
                    id = it.id,
                    abbreviation = it.abbreviation,
                    name = it.name,
                    language = it.language_name,
                    isOffline = false
                )
            }
    }

    override suspend fun getBooks(translationId: String): List<BibleBook> {
        return SupabaseProvider.client
            .from("bible_books")
            .select {
                order("book_order", Order.ASCENDING)
            }
            .decodeList<BibleBookRow>()
            .map {
                BibleBook(
                    id = it.id,
                    name = it.name,
                    abbreviation = it.abbreviation,
                    testament = if (it.testament == "NT") Testament.NEW else Testament.OLD,
                    chapterCount = it.chapter_count
                )
            }
    }

    override suspend fun getChapter(
        translationId: String,
        bookId: String,
        chapterNumber: Int
    ): BibleChapter? {
        val rows = SupabaseProvider.client
            .from("bible_verses")
            .select {
                filter {
                    eq("translation_id", translationId)
                    eq("book_id", bookId)
                    eq("chapter", chapterNumber)
                }
                order("verse", Order.ASCENDING)
            }
            .decodeList<BibleVerseRow>()

        if (rows.isEmpty()) return null

        return BibleChapter(
            bookId = bookId,
            chapterNumber = chapterNumber,
            verses = rows.map { BibleVerse(it.verse, it.text) }
        )
    }

    override suspend fun search(
        translationId: String,
        query: String
    ): List<BibleSearchResult> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()

        return SupabaseProvider.client
            .from("bible_verses")
            .select {
                filter {
                    eq("translation_id", translationId)
                    ilike("text", "%$normalized%")
                }
                order("book_id", Order.ASCENDING)
                order("chapter", Order.ASCENDING)
                order("verse", Order.ASCENDING)
                limit(50)
            }
            .decodeList<BibleVerseRow>()
            .map { row ->
                BibleSearchResult(
                    bookId = row.book_id,
                    bookName = row.book_id,
                    chapter = row.chapter,
                    verse = row.verse,
                    text = row.text
                )
            }
    }
}
