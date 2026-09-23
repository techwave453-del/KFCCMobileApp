package com.example.helloworld.data.bible

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

@Serializable
private data class KjcAsset(
    val version: String,
    val name: String,
    val language: String,
    val license: String,
    val books: List<KjcBook>
)

@Serializable
private data class KjcBook(
    val book: String,
    val bookId: Int,
    val englishName: String,
    val testament: String,
    val chapters: List<KjcChapter>
)

@Serializable
private data class KjcChapter(
    val chapter: Int,
    val verses: List<KjcVerse>
)

@Serializable
private data class KjcVerse(
    val number: Int,
    val text: String
)

class KfccBibleRepository(
    private val context: Context
) : BibleRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val asset: KjcAsset by lazy {
        context.assets
            .open("bible/kjv.json")
            .bufferedReader()
            .use { reader ->
                json.decodeFromString<KjcAsset>(reader.readText())
            }
    }

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

    private val books: List<BibleBook> by lazy {
        asset.books.map { book ->
            BibleBook(
                id = bookId(book),
                name = book.englishName,
                abbreviation = book.book,
                testament = if (book.testament == "NT") {
                    Testament.NEW
                } else {
                    Testament.OLD
                },
                chapterCount = book.chapters.size
            )
        }
    }

    override fun getTranslations(): List<BibleTranslation> {
        return translations
    }

    override fun getBooks(translationId: String): List<BibleBook> {
        return if (translationId == "kjv") {
            books
        } else {
            emptyList()
        }
    }

    override fun getChapter(
        translationId: String,
        bookId: String,
        chapterNumber: Int
    ): BibleChapter? {
        if (translationId != "kjv") {
            return null
        }

        val book = asset.books.firstOrNull {
            bookId(it) == bookId
        } ?: return null

        val chapter = book.chapters.firstOrNull {
            it.chapter == chapterNumber
        } ?: return null

        return BibleChapter(
            bookId = bookId,
            chapterNumber = chapter.chapter,
            verses = chapter.verses.map {
                BibleVerse(
                    number = it.number,
                    text = it.text
                )
            }
        )
    }

    override fun search(
        translationId: String,
        query: String
    ): List<BibleSearchResult> {
        if (translationId != "kjv" || query.isBlank()) {
            return emptyList()
        }

        val normalized = query.trim().lowercase()

        return asset.books.flatMap { book ->
            val id = bookId(book)

            book.chapters.flatMap { chapter ->
                chapter.verses
                    .filter {
                        it.text.lowercase().contains(normalized)
                    }
                    .map { verse ->
                        BibleSearchResult(
                            bookId = id,
                            bookName = book.englishName,
                            chapter = chapter.chapter,
                            verse = verse.number,
                            text = verse.text
                        )
                    }
            }
        }
    }

    private fun bookId(book: KjcBook): String {
        return when (book.bookId) {
            1 -> "genesis"
            2 -> "exodus"
            3 -> "leviticus"
            4 -> "numbers"
            5 -> "deuteronomy"
            6 -> "joshua"
            7 -> "judges"
            8 -> "ruth"
            9 -> "1-samuel"
            10 -> "2-samuel"
            11 -> "1-kings"
            12 -> "2-kings"
            13 -> "1-chronicles"
            14 -> "2-chronicles"
            15 -> "ezra"
            16 -> "nehemiah"
            17 -> "esther"
            18 -> "job"
            19 -> "psalms"
            20 -> "proverbs"
            21 -> "ecclesiastes"
            22 -> "song-of-solomon"
            23 -> "isaiah"
            24 -> "jeremiah"
            25 -> "lamentations"
            26 -> "ezekiel"
            27 -> "daniel"
            28 -> "hosea"
            29 -> "joel"
            30 -> "amos"
            31 -> "obadiah"
            32 -> "jonah"
            33 -> "micah"
            34 -> "nahum"
            35 -> "habakkuk"
            36 -> "zephaniah"
            37 -> "haggai"
            38 -> "zechariah"
            39 -> "malachi"
            40 -> "matthew"
            41 -> "mark"
            42 -> "luke"
            43 -> "john"
            44 -> "acts"
            45 -> "romans"
            46 -> "1-corinthians"
            47 -> "2-corinthians"
            48 -> "galatians"
            49 -> "ephesians"
            50 -> "philippians"
            51 -> "colossians"
            52 -> "1-thessalonians"
            53 -> "2-thessalonians"
            54 -> "1-timothy"
            55 -> "2-timothy"
            56 -> "titus"
            57 -> "philemon"
            58 -> "hebrews"
            59 -> "james"
            60 -> "1-peter"
            61 -> "2-peter"
            62 -> "1-john"
            63 -> "2-john"
            64 -> "3-john"
            65 -> "jude"
            66 -> "revelation"
            else -> book.englishName
                .lowercase()
                .replace(" ", "-")
        }
    }
}
