package com.example.helloworld.data.bible

import android.content.Context
import com.example.helloworld.data.offline.BibleBookEntity
import com.example.helloworld.data.offline.BibleTranslationEntity
import com.example.helloworld.data.offline.BibleVerseEntity
import com.example.helloworld.data.offline.KfccDatabase
import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BibleOfflineSeeder {
    private const val ASSET_DIR = "bible/kjv"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfNeeded(context: Context) {
        val db = KfccDatabase.getInstance(context)
        if (db.bibleVerseDao().count("kjv") >= 31_102) return

        val assetFiles = context.assets.list(ASSET_DIR)
            ?.filter { it.endsWith(".json") }
            ?.sorted()
            .orEmpty()

        if (assetFiles.isEmpty()) return

        db.withTransaction {
            for (fileName in assetFiles) {
                val raw = context.assets.open("$ASSET_DIR/$fileName")
                    .bufferedReader()
                    .use { it.readText() }
                val part = json.decodeFromString<KjvAssetPart>(raw)

                if (part.part == 0) {
                    part.translation?.let {
                        db.bibleTranslationDao().upsertAll(
                            listOf(
                                BibleTranslationEntity(
                                    id = it.id,
                                    abbreviation = it.abbreviation,
                                    name = it.name,
                                    language = it.language_name,
                                    isOffline = true,
                                    isEnabled = true,
                                    sortOrder = it.sort_order
                                )
                            )
                        )
                    }
                    if (part.books.isNotEmpty()) {
                        db.bibleBookDao().upsertAll(
                            part.books.map {
                                BibleBookEntity(
                                    id = it.id,
                                    name = it.name,
                                    abbreviation = it.abbreviation,
                                    testament = it.testament,
                                    bookOrder = it.book_order,
                                    chapterCount = it.chapter_count
                                )
                            }
                        )
                    }
                }

                if (part.verses.isNotEmpty()) {
                    db.bibleVerseDao().upsertAll(
                        part.verses.map {
                            BibleVerseEntity(
                                translationId = part.translationId,
                                bookId = it.book_id,
                                chapter = it.chapter,
                                verse = it.verse,
                                text = it.text
                            )
                        }
                    )
                }
            }
        }
    }

    @Serializable
    private data class KjvAssetPart(
        val part: Int,
        val translationId: String,
        val translation: TranslationAsset? = null,
        val books: List<BookAsset> = emptyList(),
        val verses: List<VerseAsset> = emptyList()
    )

    @Serializable
    private data class TranslationAsset(
        val id: String,
        val abbreviation: String,
        val name: String,
        val language_name: String,
        val sort_order: Int = 0
    )

    @Serializable
    private data class BookAsset(
        val id: String,
        val name: String,
        val abbreviation: String,
        val testament: String,
        val book_order: Int,
        val chapter_count: Int
    )

    @Serializable
    private data class VerseAsset(
        val book_id: String,
        val chapter: Int,
        val verse: Int,
        val text: String
    )
}
