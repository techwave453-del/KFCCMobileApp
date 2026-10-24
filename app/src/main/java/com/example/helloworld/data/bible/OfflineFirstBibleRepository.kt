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

    override suspend fun getVerseCount(translationId: String) = verses.count(translationId)

    override suspend fun getBooks(translationId: String): List<BibleBook> {
        val allBooks = books.getAll()
        return allBooks.map { b ->
            val name = if (translationId == "kam") getKambaBookName(b.id) else b.name
            BibleBook(b.id, name, b.abbreviation,
                if (b.testament == "NT") Testament.NEW else Testament.OLD, b.chapterCount)
        }
    }

    private fun getKambaBookName(bookId: String): String = when (bookId) {
        "GEN" -> "Mwambĩlĩlyo"
        "EXO" -> "Kuma"
        "LEV" -> "Alivai"
        "NUM" -> "Motalo"
        "DEU" -> "Kũtũngĩlĩlwa Kwa Mĩao"
        "JOS" -> "Yosua"
        "JDG" -> "Asili"
        "RUT" -> "Luthi"
        "1SA" -> "1 Samueli"
        "2SA" -> "2 Samueli"
        "1KI" -> "1 Asumbĩ"
        "2KI" -> "2 Asumbĩ"
        "1CH" -> "1 Mavinda"
        "2CH" -> "2 Mavinda"
        "EZR" -> "Esala"
        "NEH" -> "Neemia"
        "EST" -> "Esita"
        "JOB" -> "Yovu"
        "PSA" -> "Savuli"
        "PRO" -> "Nthimo"
        "ECC" -> "Mũtavan'ya"
        "SNG" -> "Wathi"
        "ISA" -> "Isaia"
        "JER" -> "Yelemai"
        "LAM" -> "Iĩo"
        "EZK" -> "Esekieli"
        "DAN" -> "Ndanieli"
        "HOS" -> "Osea"
        "JOL" -> "Yoeli"
        "AMO" -> "Amosi"
        "OBA" -> "Ovatia"
        "JON" -> "Yona"
        "MIC" -> "Mika"
        "NAM" -> "Naumu"
        "HAB" -> "Avakuki"
        "ZEP" -> "Nzevania"
        "HAG" -> "Akai"
        "ZEC" -> "Nzekalia"
        "MAL" -> "Malaki"
        "MAT" -> "Mathayo"
        "MRK" -> "Maliko"
        "LUK" -> "Luka"
        "JHN" -> "Yoana"
        "ACT" -> "Meko Ma Atũmwa"
        "ROM" -> "Alomi"
        "1CO" -> "1 Akolintho"
        "2CO" -> "2 Akolintho"
        "GAL" -> "Akalatia"
        "EPH" -> "Aeveso"
        "PHP" -> "Avilivi"
        "COL" -> "Akolosai"
        "1TH" -> "1 Athesalonika"
        "2TH" -> "2 Athesalonika"
        "1TI" -> "1 Timotheo"
        "2TI" -> "2 Timotheo"
        "TIT" -> "Tito"
        "PHM" -> "Vilemona"
        "HEB" -> "Aevelania"
        "JAS" -> "Yakobo"
        "1PE" -> "1 Vetelo"
        "2PE" -> "2 Vetelo"
        "1JN" -> "1 Yoana"
        "2JN" -> "2 Yoana"
        "3JN" -> "3 Yoana"
        "JUD" -> "Yuta"
        "REV" -> "Ũvuan'yo"
        else -> bookId
    }

    override suspend fun getChapter(translationId: String, bookId: String, chapterNumber: Int): BibleChapter? {
        var local = verses.getChapter(translationId, bookId, chapterNumber)
        if (local.isEmpty() && translationId == "gnb") {
            val kjvLocal = verses.getChapter("kjv", bookId, chapterNumber)
            if (kjvLocal.isNotEmpty()) {
                verses.upsertAll(kjvLocal.map { it.copy(translationId = translationId) })
                local = verses.getChapter(translationId, bookId, chapterNumber)
            }
        }
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
        var local = verses.search(translationId, normalized)
        if (local.isEmpty() && translationId == "gnb") {
            val kjvLocal = verses.search("kjv", normalized)
            if (kjvLocal.isNotEmpty()) {
                verses.upsertAll(kjvLocal.map { it.copy(translationId = translationId) })
                local = verses.search(translationId, normalized)
            }
        }
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
