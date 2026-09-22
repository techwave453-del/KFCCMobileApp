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
        BibleTranslation("kjv", "KJV", "King James Version", "English", true),
        BibleTranslation("niv", "NIV", "New International Version", "English", false),
        BibleTranslation("rsv", "RSV", "Revised Standard Version", "English", false)
    )

    private val books = listOf(
        BibleBook("genesis", "Genesis", "Gen", Testament.OLD, 50),
        BibleBook("exodus", "Exodus", "Ex", Testament.OLD, 40),
        BibleBook("leviticus", "Leviticus", "Lev", Testament.OLD, 27),
        BibleBook("numbers", "Numbers", "Num", Testament.OLD, 36),
        BibleBook("deuteronomy", "Deuteronomy", "Deut", Testament.OLD, 34),
        BibleBook("joshua", "Joshua", "Josh", Testament.OLD, 24),
        BibleBook("judges", "Judges", "Judg", Testament.OLD, 21),
        BibleBook("ruth", "Ruth", "Ruth", Testament.OLD, 4),
        BibleBook("1-samuel", "1 Samuel", "1 Sam", Testament.OLD, 31),
        BibleBook("2-samuel", "2 Samuel", "2 Sam", Testament.OLD, 24),
        BibleBook("1-kings", "1 Kings", "1 Kgs", Testament.OLD, 22),
        BibleBook("2-kings", "2 Kings", "2 Kgs", Testament.OLD, 25),
        BibleBook("1-chronicles", "1 Chronicles", "1 Chr", Testament.OLD, 29),
        BibleBook("2-chronicles", "2 Chronicles", "2 Chr", Testament.OLD, 36),
        BibleBook("ezra", "Ezra", "Ezra", Testament.OLD, 10),
        BibleBook("nehemiah", "Nehemiah", "Neh", Testament.OLD, 13),
        BibleBook("esther", "Esther", "Esth", Testament.OLD, 10),
        BibleBook("job", "Job", "Job", Testament.OLD, 42),
        BibleBook("psalms", "Psalms", "Ps", Testament.OLD, 150),
        BibleBook("proverbs", "Proverbs", "Prov", Testament.OLD, 31),
        BibleBook("ecclesiastes", "Ecclesiastes", "Eccl", Testament.OLD, 12),
        BibleBook("song-of-solomon", "Song of Solomon", "Song", Testament.OLD, 8),
        BibleBook("isaiah", "Isaiah", "Isa", Testament.OLD, 66),
        BibleBook("jeremiah", "Jeremiah", "Jer", Testament.OLD, 52),
        BibleBook("lamentations", "Lamentations", "Lam", Testament.OLD, 5),
        BibleBook("ezekiel", "Ezekiel", "Ezek", Testament.OLD, 48),
        BibleBook("daniel", "Daniel", "Dan", Testament.OLD, 12),
        BibleBook("hosea", "Hosea", "Hos", Testament.OLD, 14),
        BibleBook("joel", "Joel", "Joel", Testament.OLD, 3),
        BibleBook("amos", "Amos", "Amos", Testament.OLD, 9),
        BibleBook("obadiah", "Obadiah", "Obad", Testament.OLD, 1),
        BibleBook("jonah", "Jonah", "Jonah", Testament.OLD, 4),
        BibleBook("micah", "Micah", "Mic", Testament.OLD, 7),
        BibleBook("nahum", "Nahum", "Nah", Testament.OLD, 3),
        BibleBook("habakkuk", "Habakkuk", "Hab", Testament.OLD, 3),
        BibleBook("zephaniah", "Zephaniah", "Zeph", Testament.OLD, 3),
        BibleBook("haggai", "Haggai", "Hag", Testament.OLD, 2),
        BibleBook("zechariah", "Zechariah", "Zech", Testament.OLD, 14),
        BibleBook("malachi", "Malachi", "Mal", Testament.OLD, 4),
        BibleBook("matthew", "Matthew", "Matt", Testament.NEW, 28),
        BibleBook("mark", "Mark", "Mk", Testament.NEW, 16),
        BibleBook("luke", "Luke", "Lk", Testament.NEW, 24),
        BibleBook("john", "John", "Jn", Testament.NEW, 21),
        BibleBook("acts", "Acts", "Acts", Testament.NEW, 28),
        BibleBook("romans", "Romans", "Rom", Testament.NEW, 16),
        BibleBook("1-corinthians", "1 Corinthians", "1 Cor", Testament.NEW, 16),
        BibleBook("2-corinthians", "2 Corinthians", "2 Cor", Testament.NEW, 13),
        BibleBook("galatians", "Galatians", "Gal", Testament.NEW, 6),
        BibleBook("ephesians", "Ephesians", "Eph", Testament.NEW, 6),
        BibleBook("philippians", "Philippians", "Phil", Testament.NEW, 4),
        BibleBook("colossians", "Colossians", "Col", Testament.NEW, 4),
        BibleBook("1-thessalonians", "1 Thessalonians", "1 Thess", Testament.NEW, 5),
        BibleBook("2-thessalonians", "2 Thessalonians", "2 Thess", Testament.NEW, 3),
        BibleBook("1-timothy", "1 Timothy", "1 Tim", Testament.NEW, 6),
        BibleBook("2-timothy", "2 Timothy", "2 Tim", Testament.NEW, 4),
        BibleBook("titus", "Titus", "Tit", Testament.NEW, 3),
        BibleBook("philemon", "Philemon", "Phlm", Testament.NEW, 1),
        BibleBook("hebrews", "Hebrews", "Heb", Testament.NEW, 13),
        BibleBook("james", "James", "Jas", Testament.NEW, 5),
        BibleBook("1-peter", "1 Peter", "1 Pet", Testament.NEW, 5),
        BibleBook("2-peter", "2 Peter", "2 Pet", Testament.NEW, 3),
        BibleBook("1-john", "1 John", "1 Jn", Testament.NEW, 5),
        BibleBook("2-john", "2 John", "2 Jn", Testament.NEW, 1),
        BibleBook("3-john", "3 John", "3 Jn", Testament.NEW, 1),
        BibleBook("jude", "Jude", "Jude", Testament.NEW, 1),
        BibleBook("revelation", "Revelation", "Rev", Testament.NEW, 22)
    )

    private val sampleChapters = mapOf(
        "kjv:psalms:23" to BibleChapter(
            "psalms",
            23,
            listOf(
                BibleVerse(1, "The LORD is my shepherd; I shall not want."),
                BibleVerse(2, "He maketh me to lie down in green pastures: he leadeth me beside the still waters."),
                BibleVerse(3, "He restoreth my soul: he leadeth me in the paths of righteousness for his name's sake."),
                BibleVerse(4, "Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me."),
                BibleVerse(5, "Thou preparest a table before me in the presence of mine enemies: thou anointest my head with oil."),
                BibleVerse(6, "Surely goodness and mercy shall follow me all the days of my life: and I will dwell in the house of the LORD for ever.")
            )
        ),
        "kjv:john:3" to BibleChapter(
            "john",
            3,
            listOf(
                BibleVerse(16, "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.")
            )
        )
    )

    override fun getTranslations(): List<BibleTranslation> = translations

    override fun getBooks(translationId: String): List<BibleBook> = books

    override fun getChapter(
        translationId: String,
        bookId: String,
        chapterNumber: Int
    ): BibleChapter? = sampleChapters["$translationId:$bookId:$chapterNumber"]

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