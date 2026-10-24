package com.example.helloworld.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
data class BibleTranslation(
    val id: String,
    val name: String,
    val languageCode: String,
    val languageName: String,
    val abbreviation: String,
    val description: String = "",
    val licenseName: String,
    val licenseUrl: String? = null,
    val sourceName: String,
    val sourceUrl: String? = null,
    val isPublicDomain: Boolean,
    val isEnabled: Boolean,
    val sortOrder: Int
)

@Serializable
data class BibleBookRecord(
    val id: String,
    val name: String,
    val abbreviation: String,
    val testament: String,
    val bookOrder: Int,
    val chapterCount: Int
)

@Serializable
data class BibleVerseRecord(
    val id: Long,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

class BibleRepository {

    private val client
        get() = SupabaseProvider.client

    suspend fun getTranslations(): List<BibleTranslation> =
        client.from("bible_translations")
            .select {
                filter { eq("is_enabled", true) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList()

    suspend fun getBooks(): List<BibleBookRecord> =
        client.from("bible_books")
            .select {
                order("book_order", Order.ASCENDING)
            }
            .decodeList()

    suspend fun getChapter(
        translationId: String,
        bookId: String,
        chapter: Int
    ): List<BibleVerseRecord> =
        client.from("bible_verses")
            .select {
                filter {
                    eq("translation_id", translationId)
                    eq("book_id", bookId)
                    eq("chapter", chapter)
                }
                order("verse", Order.ASCENDING)
            }
            .decodeList()

    suspend fun getVerse(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ): BibleVerseRecord? =
        client.from("bible_verses")
            .select {
                filter {
                    eq("translation_id", translationId)
                    eq("book_id", bookId)
                    eq("chapter", chapter)
                    eq("verse", verse)
                }
            }
            .decodeList<BibleVerseRecord>()
            .firstOrNull()

    suspend fun search(
        translationId: String,
        query: String,
        limit: Int = 50
    ): List<BibleVerseRecord> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        return client.from("bible_verses")
            .select(columns = Columns.list(
                "id",
                "translation_id",
                "book_id",
                "chapter",
                "verse",
                "text"
            )) {
                filter {
                    eq("translation_id", translationId)
                    ilike("text", "%$trimmed%")
                }
                order("book_id", Order.ASCENDING)
                order("chapter", Order.ASCENDING)
                order("verse", Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList()
    }
}
