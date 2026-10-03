package com.example.helloworld.data.bible

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import java.time.LocalDate

data class DailyScripture(
    val themeName: String,
    val themeDescription: String,
    val bookId: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val situation: String,
    val reflection: String
) {
    fun reference(bookName: String): String {
        val verses = if (verseStart == verseEnd) "$verseStart" else "$verseStart-$verseEnd"
        return "$bookName $chapter:$verses"
    }
}

class DailyScriptureRepository(
    private val bibleRepository: BibleRepository = KfccBibleRepository()
) {
    suspend fun getToday(translationId: String, date: LocalDate = LocalDate.now()): DailyScripture? {
        val entries = runCatching {
            SupabaseProvider.client.from("daily_scriptures").select {
                filter {
                    eq("translation_id", translationId)
                    eq("is_active", true)
                }
                order("priority", Order.DESCENDING)
            }.decodeList<DailyScriptureRow>()
        }.getOrDefault(emptyList())

        if (entries.isEmpty()) return null

        val themes = runCatching {
            SupabaseProvider.client.from("daily_scripture_themes").select {
                filter { eq("is_active", true) }
                order("sort_order", Order.ASCENDING)
            }.decodeList<DailyScriptureThemeRow>()
        }.getOrDefault(emptyList()).associateBy { it.id }

        val eligible = entries
            .filter { themes.containsKey(it.theme_id) }
            .sortedWith(compareBy<DailyScriptureRow> { themes[it.theme_id]?.sort_order ?: Int.MAX_VALUE }.thenBy { it.id })

        if (eligible.isEmpty()) return null

        // Deterministic daily rotation: one stable Scripture per calendar day.
        // It changes tomorrow, but opening the app repeatedly today returns the same entry.
        val selected = eligible[Math.floorMod(date.toEpochDay().hashCode(), eligible.size)]
        val theme = themes[selected.theme_id] ?: return null

        return DailyScripture(
            themeName = theme.name,
            themeDescription = theme.description,
            bookId = selected.book_id,
            chapter = selected.chapter,
            verseStart = selected.verse_start,
            verseEnd = selected.verse_end,
            situation = selected.situation,
            reflection = selected.reflection
        )
    }

    suspend fun getPassage(
        translationId: String,
        scripture: DailyScripture
    ): BibleChapter? {
        return bibleRepository.getChapter(
            translationId = translationId,
            bookId = scripture.bookId,
            chapterNumber = scripture.chapter
        )
    }

    @Serializable
    private data class DailyScriptureRow(
        val id: String,
        val theme_id: String,
        val translation_id: String,
        val book_id: String,
        val chapter: Int,
        val verse_start: Int,
        val verse_end: Int,
        val situation: String = "",
        val reflection: String = ""
    )

    @Serializable
    private data class DailyScriptureThemeRow(
        val id: String,
        val name: String,
        val description: String = "",
        val sort_order: Int = 0
    )
}
