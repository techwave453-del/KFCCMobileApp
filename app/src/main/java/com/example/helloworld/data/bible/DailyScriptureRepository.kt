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
        }.getOrDefault(emptyList())

        if (themes.isEmpty()) return null

        val today = date
        val eligibleEntries = entries.filter { entry ->
            entry.available_from?.let { LocalDate.parse(it) <= today } ?: true
        }.filter { entry ->
            entry.available_until?.let { LocalDate.parse(it) >= today } ?: true
        }

        val themeById = themes.associateBy { it.id }
        val availableThemes = themes.filter { theme ->
            eligibleEntries.any { it.theme_id == theme.id }
        }
        if (availableThemes.isEmpty()) return null

        val configuredThemeId = runCatching {
            SupabaseProvider.client.from("daily_scripture_settings").select {
                filter { eq("id", 1) }
            }.decodeList<DailyScriptureSettingsRow>().firstOrNull()?.selected_theme_id
        }.getOrNull()

        // An administrator can pin a theme. When no theme is pinned, select a
        // stable pseudo-random active theme for the calendar day so every user
        // sees the same theme throughout that day.
        val selectedTheme = configuredThemeId
            ?.let { themeById[it] }
            ?.takeIf { it in availableThemes }
            ?: availableThemes[Math.floorMod(date.toEpochDay().hashCode(), availableThemes.size)]

        val eligible = eligibleEntries
            .filter { it.theme_id == selectedTheme.id }
            .sortedBy { it.id }

        if (eligible.isEmpty()) return null

        // Keep the selected theme stable for the day, while rotating through
        // that theme's available passages when it has more than one.
        val selected = eligible[Math.floorMod(date.toEpochDay().hashCode(), eligible.size)]
        val theme = themeById[selected.theme_id] ?: return null

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
        val reflection: String = "",
        val available_from: String? = null,
        val available_until: String? = null
    )

    @Serializable
    private data class DailyScriptureSettingsRow(
        val id: Int,
        val selected_theme_id: String? = null
    )

    @Serializable
    private data class DailyScriptureThemeRow(
        val id: String,
        val name: String,
        val description: String = "",
        val sort_order: Int = 0
    )
}
