package com.example.helloworld.data.bible

interface BibleRepository {
    fun getTranslations(): List<BibleTranslation>

    fun getBooks(translationId: String): List<BibleBook>

    fun getChapter(
        translationId: String,
        bookId: String,
        chapterNumber: Int
    ): BibleChapter?

    fun search(
        translationId: String,
        query: String
    ): List<BibleSearchResult>
}

data class BibleSearchResult(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

class KfccBibleRepository : BibleRepository {

    private val translations = listOf(
        BibleTranslation(
            id = "kjv",
            abbreviation = "KJV",
            name = "King James Version",
            language = "English",
            isOffline = true
        ),
        BibleTranslation(
            id = "niv",
            abbreviation = "NIV",
            name = "New International Version",
            language = "English",
            isOffline = false
        ),
        BibleTranslation(
            id = "rsv",
            abbreviation = "RSV",
            name = "Revised Standard Version",
            language = "English",
            isOffline = false
        )
    )

    private val books = listOf(
        BibleBook("genesis", "Genesis", "Gen", Testament.OLD, 50),
        BibleBook("exodus", "Exodus", "Ex", Testament.OLD, 40),
        BibleBook("psalms", "Psalms", "Ps", Testament.OLD, 150),
        BibleBook("proverbs", "Proverbs", "Prov", Testament.OLD, 31),
        BibleBook("isaiah", "Isaiah", "Isa", Testament.OLD, 66),
        BibleBook("matthew", "Matthew", "Matt", Testament.NEW, 28),
        BibleBook("mark", "Mark", "Mk", Testament.NEW, 16),
        BibleBook("luke", "Luke", "Lk", Testament.NEW, 24),
        BibleBook("john", "John", "Jn", Testament.NEW, 21),
        BibleBook("acts", "Acts", "Acts", Testament.NEW, 28),
        BibleBook("romans", "Romans", "Rom", Testament.NEW, 16)
    )

    private val sampleChapters = mapOf(
        "kjv:psalms:23" to BibleChapter(
            bookId = "psalms",
            chapterNumber = 23,
            verses = listOf(
                BibleVerse(1, "The LORD is my shepherd; I shall not want."),
                BibleVerse(2, "He maketh me to lie down in green pastures: he leadeth me beside the still waters."),
                BibleVerse(3, "He restoreth my soul: he leadeth me in the paths of righteousness for his name's sake."),
                BibleVerse(4, "Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me."),
                BibleVerse(5, "Thou preparest a table before me in the presence of mine enemies: thou anointest my head with oil."),
                BibleVerse(6, "Surely goodness and mercy shall follow me all the days of my life: and I will dwell in the house of the LORD for ever.")
            )
        ),
        "kjv:john:3" to BibleChapter(
            bookId = "john",
            chapterNumber = 3,
            verses = listOf(
                BibleVerse(16, "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.")
            )
        )
    )

    override fun getTranslations(): List<BibleTranslation> {
        return translations
    }

    override fun getBooks(translationId: String): List<BibleBook> {
        return books
    }

    override fun getChapter(
        translationId: String,
        bookId: String,
        chapterNumber: Int
    ): BibleChapter? {
        return sampleChapters["$translationId:$bookId:$chapterNumber"]
    }

    override fun search(
        translationId: String,
        query: String
    ): List<BibleSearchResult> {
        if (query.isBlank()) return emptyList()

        val normalized = query.trim().lowercase()

        return sampleChapters
            .filter { it.key.startsWith("$translationId:") }
            .flatMap { entry ->
                val chapter = entry.value
                val book = books.firstOrNull { it.id == chapter.bookId }
                    ?: return@flatMap emptyList()

                chapter.verses
                    .filter { it.text.lowercase().contains(normalized) }
                    .map { verse ->
                        BibleSearchResult(
                            bookId = book.id,
                            bookName = book.name,
                            chapter = chapter.chapterNumber,
                            verse = verse.number,
                            text = verse.text
                        )
                    }
            }
    }
}
