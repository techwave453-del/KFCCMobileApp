package com.example.helloworld.data.bible

import android.content.Context
import android.text.Html
import com.example.helloworld.config.AppConfig
import com.example.helloworld.data.offline.BibleBookEntity
import com.example.helloworld.data.offline.BibleTranslationEntity
import com.example.helloworld.data.offline.BibleVerseEntity
import com.example.helloworld.data.offline.KfccDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ApiBibleVersion(val id: String, val name: String, val abbreviation: String, val language: String, val languageCode: String)

class ApiBibleRepository(context: Context) {
    private val endpoint = "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1/bible-passage"
    private val db = KfccDatabase.getInstance(context.applicationContext)
    private val ids = listOf("GEN","EXO","LEV","NUM","DEU","JOS","JDG","RUT","1SA","2SA","1KI","2KI","1CH","2CH","EZR","NEH","EST","JOB","PSA","PRO","ECC","SNG","ISA","JER","LAM","EZK","DAN","HOS","JOL","AMO","OBA","JON","MIC","NAM","HAB","ZEP","HAG","ZEC","MAL","MAT","MRK","LUK","JHN","ACT","ROM","1CO","2CO","GAL","EPH","PHP","COL","1TH","2TH","1TI","2TI","TIT","PHM","HEB","JAS","1PE","2PE","1JN","2JN","3JN","JUD","REV")
    private val names = listOf("Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians","Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation")

    private suspend fun request(payload: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val c = URL(endpoint).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"; c.connectTimeout = 20000; c.readTimeout = 45000; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("apikey", AppConfig.SUPABASE_PUBLISHABLE_KEY)
            c.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching { JSONObject(text) }.getOrNull()
                val message = error?.optString("error").orEmpty().ifBlank { "API.Bible request failed" }
                val upstreamStatus = error?.optInt("upstreamStatus", 0) ?: 0
                val upstreamMessage = error?.optString("upstreamMessage").orEmpty()
                val details = buildList {
                    if (upstreamStatus > 0) add("API.Bible HTTP $upstreamStatus")
                    if (upstreamMessage.isNotBlank() && upstreamMessage != "null") add(upstreamMessage)
                }.joinToString(": ")
                throw IllegalStateException(if (details.isBlank()) "$message (HTTP $status)" else "$message — $details")
            }
            JSONObject(text)
        } finally { c.disconnect() }
    }

    suspend fun listVersions(): List<ApiBibleVersion> {
        val response = request(JSONObject().put("action", "catalog").put("language", "all"))
        val data = response.optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { index ->
            val item = data.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            if (id.isBlank()) return@mapNotNull null
            val language = item.optJSONObject("language")
            ApiBibleVersion(
                id = id,
                name = item.optString("name", id),
                abbreviation = item.optString("abbreviation").ifBlank { id.take(8).uppercase() },
                language = language?.optString("name").orEmpty().ifBlank { "Unknown language" },
                languageCode = language?.optString("id").orEmpty().ifBlank { "und" }
            )
        }.distinctBy { it.id }.sortedBy { it.name.lowercase() }
    }

    suspend fun download(version: ApiBibleVersion, progress: (String) -> Unit) {
        val translationId = version.id.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
        val bookData = request(JSONObject().put("action", "books").put("bibleId", version.id)).optJSONArray("data")
            ?: throw IllegalStateException("API.Bible did not return a book list.")
        if (bookData.length() == 0) throw IllegalStateException("This edition has no accessible books.")
        val apiBooks = (0 until bookData.length()).mapNotNull { bookData.optJSONObject(it) }
        val mapped = mutableListOf<Triple<JSONObject, String, List<JSONObject>>>()
        val bookEntities = mutableListOf<BibleBookEntity>()
        var total = 0
        for (book in apiBooks) {
            val id = canonicalId(book.optString("id"), book.optString("name"), book.optInt("number", 0))
            if (id !in ids) continue
            val chapters = request(JSONObject().put("action", "chapters").put("bibleId", version.id).put("bookId", book.optString("id"))).optJSONArray("data") ?: continue
            val chapterList = (0 until chapters.length()).mapNotNull { chapters.optJSONObject(it) }
            if (chapterList.isEmpty()) continue
            mapped += Triple(book, id, chapterList)
            total += chapterList.size
            bookEntities += BibleBookEntity(id, names[ids.indexOf(id)], id, if (ids.indexOf(id) >= 39) "NT" else "OT", ids.indexOf(id) + 1, chapterList.size)
        }
        if (mapped.isEmpty()) throw IllegalStateException("Could not map API.Bible books to standard Bible books.")
        db.bibleBookDao().upsertAll(bookEntities)
        db.bibleTranslationDao().upsertAll(listOf(BibleTranslationEntity(translationId, version.abbreviation.take(20), version.name, version.language, false, false, 40)))
        val chaptersToDownload = mapped.flatMap { (book, bookId, chapters) ->
            chapters.mapNotNull { chapter ->
                val chapterId = chapter.optString("id")
                val chapterNo = chapter.optInt("number", chapterId.substringAfterLast('.').toIntOrNull() ?: 0)
                if (chapterId.isBlank() || chapterNo <= 0) null
                else Triple(book, bookId, chapterId to chapterNo)
            }
        }
        total = chaptersToDownload.size
        if (total == 0) throw IllegalStateException("No downloadable chapters were found.")

        // API.Bible requires one request per chapter. Keep a small bounded pool so
        // downloads run concurrently without overwhelming the provider or device.
        val concurrency = Semaphore(5)
        val completed = AtomicInteger(0)
        val downloadedVerses = coroutineScope {
            chaptersToDownload.map { (book, bookId, chapterInfo) ->
                async {
                    concurrency.withPermit {
                        val (chapterId, chapterNo) = chapterInfo
                        progress("Downloading " + book.optString("name", bookId) + " " + chapterNo + " (" + completed.get() + "/" + total + ")")
                        val response = request(JSONObject().put("action", "chapter").put("bibleId", version.id).put("chapterId", chapterId))
                        val html = response.optJSONObject("data")?.optString("content").orEmpty()
                        val verses = parseVerses(html, bookId, chapterNo, translationId)
                        val finished = completed.incrementAndGet()
                        progress("Downloaded " + finished + " of " + total + " chapters")
                        verses
                    }
                }
            }.awaitAll().flatten()
        }
        if (downloadedVerses.isNotEmpty()) {
            downloadedVerses.chunked(500).forEach { batch -> db.bibleVerseDao().upsertAll(batch) }
        }
        val count = db.bibleVerseDao().count(translationId)
        if (count == 0) throw IllegalStateException("No verse text was returned. This edition may not permit downloading through API.Bible.")
        db.bibleTranslationDao().upsertAll(listOf(BibleTranslationEntity(translationId, version.abbreviation.take(20), version.name, version.language, true, true, 40)))
    }

    private fun parseVerses(html: String, bookId: String, chapter: Int, translationId: String): List<BibleVerseEntity> {
        if (html.isBlank()) return emptyList()

        // API.Bible markup varies slightly between editions. Match verse markers in
        // the whole document rather than parsing every span independently (nested
        // spans caused the previous parser to miss valid verse text).
        val markerPatterns = listOf(
            Regex("""(?is)<span\b(?=[^>]*\bclass\s*=\s*["'][^"']*(?:^|\s)(?:v|verse-number|verse-num)(?:\s|$)[^"']*["'])[^>]*>\s*(\d{1,3})\s*</span>"""),
            Regex("""(?is)<span\b(?=[^>]*\bdata-number\s*=\s*["']?(\d{1,3})["']?)[^>]*>"""),
            Regex("""(?is)<span\b(?=[^>]*\bclass\s*=\s*["'][^"']*\bv\b[^"']*["'])[^>]*>\s*(\d{1,3})\s*</span>""")
        )
        val markers: List<Triple<Int, Int, Int>> = markerPatterns.firstNotNullOfOrNull { pattern ->
            pattern.findAll(html).mapNotNull { match ->
                val numberText = match.groups[1]?.value
                    ?: Regex("""(?i)\bdata-number\s*=\s*["']?(\d{1,3})""")
                        .find(match.value)?.groupValues?.getOrNull(1)
                val number = numberText?.toIntOrNull() ?: return@mapNotNull null
                if (number !in 1..200) return@mapNotNull null
                Triple(match.range.last + 1, number, match.range.first)
            }.toList().takeIf { it.isNotEmpty() }
        } ?: emptyList()

        if (markers.isNotEmpty()) {
            return markers.mapIndexedNotNull { index, marker ->
                val end = markers.getOrNull(index + 1)?.third ?: html.length
                val raw = html.substring(marker.first.coerceAtMost(html.length), end.coerceAtLeast(marker.first).coerceAtMost(html.length))
                val text = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
                    .replace(Regex("[\u00a0\\s]+"), " ")
                    .trim().trim(' ', '¶', '·')
                if (text.isBlank()) null else BibleVerseEntity(translationId, bookId, chapter, marker.second, text)
            }
        }

        // Do not store a whole chapter as verse 1: that would corrupt verse
        // addressing and search. Return empty so the caller can report the issue.
        return emptyList()
    }

    private fun canonicalId(apiId: String, name: String, number: Int): String {
        val id = apiId.uppercase().substringBefore('.')
        if (id in ids) return id
        val clean = name.lowercase().replace(Regex("[^a-z0-9]"), "")
        val index = names.indexOfFirst { it.lowercase().replace(Regex("[^a-z0-9]"), "") == clean }
        return if (index >= 0) ids[index] else ids.getOrNull(number - 1).orEmpty()
    }
}
