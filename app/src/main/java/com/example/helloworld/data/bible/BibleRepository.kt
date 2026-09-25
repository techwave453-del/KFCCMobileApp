package com.example.helloworld.data.bible

import com.example.helloworld.data.KfccDataContext
import com.example.helloworld.data.offline.KfccDatabase

interface BibleRepository {
    suspend fun getTranslations(): List<BibleTranslation>
    suspend fun getBooks(translationId: String): List<BibleBook>
    suspend fun getChapter(translationId: String, bookId: String, chapterNumber: Int): BibleChapter?
    suspend fun search(translationId: String, query: String): List<BibleSearchResult>
}

data class BibleSearchResult(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

class KfccBibleRepository : BibleRepository {
    private val delegate: OfflineFirstBibleRepository
        get() = OfflineFirstBibleRepository(KfccDatabase.getInstance(KfccDataContext.appContext))

    override suspend fun getTranslations() = delegate.getTranslations()
    override suspend fun getBooks(translationId: String) = delegate.getBooks(translationId)
    override suspend fun getChapter(translationId: String, bookId: String, chapterNumber: Int) =
        delegate.getChapter(translationId, bookId, chapterNumber)
    override suspend fun search(translationId: String, query: String) =
        delegate.search(translationId, query)
}
