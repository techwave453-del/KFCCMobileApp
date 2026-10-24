package com.example.helloworld.data.bible

import android.content.Context
import android.text.Html
import com.example.helloworld.config.AppConfig
import com.example.helloworld.data.offline.BibleBookEntity
import com.example.helloworld.data.offline.BibleTranslationEntity
import com.example.helloworld.data.offline.BibleVerseEntity
import com.example.helloworld.data.offline.KfccDatabase
import kotlinx.coroutines.Dispatchers
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
        var done = 0
        for ((book, bookId, chapters) in mapped) {
            for (chapter in chapters) {
                val chapterId = chapter.optString("id")
                val chapterNo = chapter.optInt("number", chapterId.substringAfterLast('.').toIntOrNull() ?: 0)
                if (chapterId.isBlank() || chapterNo <= 0) continue
                progress("Downloading " + book.optString("name", bookId) + " " + chapterNo + " (" + (done + 1) + "/" + total + ")")
                val response = request(JSONObject().put("action", "chapter").put("bibleId", version.id).put("chapterId", chapterId))
                val html = response.optJSONObject("data")?.optString("content").orEmpty()
                val verses = parseVerses(html, bookId, chapterNo, translationId)
                if (verses.isNotEmpty()) db.bibleVerseDao().upsertAll(verses)
                done++
                progress("Downloaded " + done + " of " + total + " chapters")
            }
        }
        val count = db.bibleVerseDao().count(translationId)
        if (count == 0) throw IllegalStateException("No verse text was returned. This edition may not permit downloading through API.Bible.")
        db.bibleTranslationDao().upsertAll(listOf(BibleTranslationEntity(translationId, version.abbreviation.take(20), version.name, version.language, true, true, 40)))
    }

    private fun parseVerses(html: String, bookId: String, chapter: Int, translationId: String): List<BibleVerseEntity> {
        val spans = Regex("(?is)<span\\b([^>]*)>(.*?)</span>").findAll(html).toList()
        val starts = spans.mapIndexedNotNull { index, match ->
            val attrs = match.groupValues[1]
            if (!Regex("(?i)class=[\"'][^\"']*\\bv\\b[^\"']*[\"']").containsMatchIn(attrs)) return@mapIndexedNotNull null
            val n = Html.fromHtml(match.groupValues[2], Html.FROM_HTML_MODE_LEGACY).toString().trim().toIntOrNull() ?: return@mapIndexedNotNull null
            Triple(match.range.last + 1, n, index)
        }
        return starts.mapIndexedNotNull { i, start ->
            val end = starts.getOrNull(i + 1)?.let { spans[it.third].range.first } ?: html.length
            val raw = html.substring(start.first.coerceAtMost(html.length), end.coerceAtLeast(start.first).coerceAtMost(html.length))
            val text = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("[\\u00a0\\s]+"), " ").trim().trim(' ', '¶', '·')
            if (text.isBlank()) null else BibleVerseEntity(translationId, bookId, chapter, start.second, text)
        }
    }

    private fun canonicalId(apiId: String, name: String, number: Int): String {
        val id = apiId.uppercase().substringBefore('.')
        if (id in ids) return id
        val clean = name.lowercase().replace(Regex("[^a-z0-9]"), "")
        val index = names.indexOfFirst { it.lowercase().replace(Regex("[^a-z0-9]"), "") == clean }
        return if (index >= 0) ids[index] else ids.getOrNull(number - 1).orEmpty()
    }
}
