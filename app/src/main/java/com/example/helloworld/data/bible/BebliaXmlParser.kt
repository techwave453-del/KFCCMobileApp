package com.example.helloworld.data.bible

import android.content.Context
import android.util.Xml
import com.example.helloworld.data.SupabaseProvider
import com.example.helloworld.data.offline.BibleBookEntity
import com.example.helloworld.data.offline.BibleTranslationEntity
import com.example.helloworld.data.offline.BibleVerseEntity
import com.example.helloworld.data.offline.KfccDatabase
import io.github.jan.supabase.postgrest.from
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object BebliaXmlParser {
    private val standardBookIds = listOf(
        "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO", "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL",
        "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV"
    )

    suspend fun parseAndStore(
        context: Context,
        inputStream: InputStream,
        translationId: String,
        abbreviation: String,
        name: String,
        language: String
    ) {
        val db = KfccDatabase.getInstance(context)
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(inputStream, null)
        }

        db.bibleTranslationDao().upsertAll(
            listOf(
                BibleTranslationEntity(
                    id = translationId,
                    abbreviation = abbreviation,
                    name = name,
                    language = language,
                    isOffline = true,
                    isEnabled = true,
                    sortOrder = 30
                )
            )
        )

        var eventType = parser.eventType
        var currentBookIndex = 0
        var currentBookId = "GEN"
        var currentChapter = 1
        var currentVerse = 1
        val chaptersMap = mutableMapOf<String, Int>()
        val booksList = mutableListOf<BibleBookEntity>()
        val versesList = mutableListOf<BibleVerseEntity>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name.lowercase()
                    if (tagName == "book" || tagName.contains("book") || tagName == "biblebook") {
                        val bookName = parser.getAttributeValue(null, "bname") ?: parser.getAttributeValue(null, "name")
                        val bnum = parser.getAttributeValue(null, "number")?.toIntOrNull() 
                            ?: parser.getAttributeValue(null, "bnumber")?.toIntOrNull() 
                            ?: (currentBookIndex + 1)
                        currentBookIndex = bnum
                        currentBookId = if (bnum in 1..standardBookIds.size) standardBookIds[bnum - 1] else guessBookId(bookName ?: "", bnum)
                    } else if (tagName == "chapter" || tagName.contains("chap")) {
                        currentChapter = parser.getAttributeValue(null, "number")?.toIntOrNull() 
                            ?: parser.getAttributeValue(null, "cnumber")?.toIntOrNull() 
                            ?: currentChapter
                    } else if (tagName == "verse" || tagName.contains("vers")) {
                        currentVerse = parser.getAttributeValue(null, "number")?.toIntOrNull() 
                            ?: parser.getAttributeValue(null, "vnumber")?.toIntOrNull() 
                            ?: currentVerse
                        val text = parser.nextText().trim()
                        if (text.isNotBlank() && currentBookId.isNotBlank()) {
                            versesList.add(
                                BibleVerseEntity(
                                    translationId = translationId,
                                    bookId = currentBookId,
                                    chapter = currentChapter,
                                    verse = currentVerse,
                                    text = text
                                )
                            )
                            val maxC = chaptersMap[currentBookId] ?: currentChapter
                            if (currentChapter > maxC) chaptersMap[currentBookId] = currentChapter
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        chaptersMap.forEach { (bId, maxChap) ->
            val bName = getBookName(bId)
            booksList.add(
                BibleBookEntity(
                    id = bId,
                    name = bName,
                    abbreviation = bId,
                    testament = if (isNewTestament(bId)) "NT" else "OT",
                    bookOrder = getBookOrder(bId),
                    chapterCount = maxChap
                )
            )
        }

        if (booksList.isNotEmpty()) {
            db.bibleBookDao().upsertAll(booksList)
        }
        if (versesList.isNotEmpty()) {
            db.bibleVerseDao().upsertAll(versesList)
            runCatching {
                SupabaseProvider.client.from("bible_translations").upsert(
                    mapOf(
                        "id" to translationId,
                        "abbreviation" to abbreviation,
                        "name" to name,
                        "language_code" to language.lowercase(),
                        "language_name" to language,
                        "is_enabled" to true,
                        "sort_order" to 30
                    )
                )
                versesList.chunked(500).forEach { chunk ->
                    SupabaseProvider.client.from("bible_verses").upsert(
                        chunk.map {
                            mapOf(
                                "translation_id" to it.translationId,
                                "book_id" to it.bookId,
                                "chapter" to it.chapter,
                                "verse" to it.verse,
                                "text" to it.text
                            )
                        }
                    )
                }
            }
        }
    }

    private fun guessBookId(name: String, order: Int): String {
        val clean = name.trim().lowercase()
        return when {
            clean.contains("gen") || clean.contains("mwambidianili") || clean.contains("mwambidian") -> "GEN"
            clean.contains("exo") || clean.contains("kuma") -> "EXO"
            clean.contains("lev") || clean.contains("lita") -> "LEV"
            clean.contains("num") || clean.contains("mgalo") -> "NUM"
            clean.contains("deut") || clean.contains("kumbukumbu") -> "DEU"
            clean.contains("matt") || clean.contains("matayo") -> "MAT"
            clean.contains("mark") || clean.contains("mako") -> "MRK"
            clean.contains("luc") || clean.contains("luke") || clean.contains("luka") -> "LUK"
            clean.contains("john") || clean.contains("yohana") -> "JHN"
            clean.contains("acts") || clean.contains("matendo") -> "ACT"
            clean.contains("psalm") || clean.contains("zaburi") -> "PSA"
            else -> {
                if (order in standardBookIds.indices) standardBookIds[order - 1] else "BK$order"
            }
        }
    }

    private fun getBookName(bookId: String): String = when (bookId) {
        "GEN" -> "Genesis"
        "EXO" -> "Exodus"
        "LEV" -> "Leviticus"
        "NUM" -> "Numbers"
        "DEU" -> "Deuteronomy"
        "JOS" -> "Joshua"
        "JDG" -> "Judges"
        "RUT" -> "Ruth"
        "1SA" -> "1 Samuel"
        "2SA" -> "2 Samuel"
        "1KI" -> "1 Kings"
        "2KI" -> "2 Kings"
        "1CH" -> "1 Chronicles"
        "2CH" -> "2 Chronicles"
        "EZR" -> "Ezra"
        "NEH" -> "Nehemiah"
        "EST" -> "Esther"
        "JOB" -> "Job"
        "PSA" -> "Psalms"
        "PRO" -> "Proverbs"
        "ECC" -> "Ecclesiastes"
        "SNG" -> "Song of Solomon"
        "ISA" -> "Isaiah"
        "JER" -> "Jeremiah"
        "LAM" -> "Lamentations"
        "EZK" -> "Ezekiel"
        "DAN" -> "Daniel"
        "HOS" -> "Hosea"
        "JOL" -> "Joel"
        "AMO" -> "Amos"
        "OBA" -> "Obadiah"
        "JON" -> "Jonah"
        "MIC" -> "Micah"
        "NAM" -> "Nahum"
        "HAB" -> "Habakkuk"
        "ZEP" -> "Zephaniah"
        "HAG" -> "Haggai"
        "ZEC" -> "Zechariah"
        "MAL" -> "Malachi"
        "MAT" -> "Matthew"
        "MRK" -> "Mark"
        "LUK" -> "Luke"
        "JHN" -> "John"
        "ACT" -> "Acts"
        "ROM" -> "Romans"
        "1CO" -> "1 Corinthians"
        "2CO" -> "2 Corinthians"
        "GAL" -> "Galatians"
        "EPH" -> "Ephesians"
        "PHP" -> "Philippians"
        "COL" -> "Colossians"
        "1TH" -> "1 Thessalonians"
        "2TH" -> "2 Thessalonians"
        "1TI" -> "1 Timothy"
        "2TI" -> "2 Timothy"
        "TIT" -> "Titus"
        "PHM" -> "Philemon"
        "HEB" -> "Hebrews"
        "JAS" -> "James"
        "1PE" -> "1 Peter"
        "2PE" -> "2 Peter"
        "1JN" -> "1 John"
        "2JN" -> "2 John"
        "3JN" -> "3 John"
        "JUD" -> "Jude"
        "REV" -> "Revelation"
        else -> bookId
    }

    private fun getBookOrder(bookId: String): Int {
        return (standardBookIds.indexOf(bookId) + 1).let { if (it > 0) it else 99 }
    }

    private fun isNewTestament(bookId: String): Boolean {
        val nt = listOf("MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV")
        return bookId.uppercase() in nt
    }
}

