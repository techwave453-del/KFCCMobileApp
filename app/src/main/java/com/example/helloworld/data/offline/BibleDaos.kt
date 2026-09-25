package com.example.helloworld.data.offline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleTranslationDao {
    @Query("SELECT * FROM bible_translations WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    fun observeEnabled(): Flow<List<BibleTranslationEntity>>
    @Query("SELECT * FROM bible_translations WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    suspend fun getEnabled(): List<BibleTranslationEntity>
    @Query("SELECT * FROM bible_translations WHERE id = :id LIMIT 1")
    suspend fun get(id: String): BibleTranslationEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BibleTranslationEntity>)
}

@Dao
interface BibleBookDao {
    @Query("SELECT * FROM bible_books ORDER BY bookOrder ASC")
    suspend fun getAll(): List<BibleBookEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BibleBookEntity>)
}

@Dao
interface BibleVerseDao {
    @Query("SELECT * FROM bible_verses WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter ORDER BY verse ASC")
    suspend fun getChapter(translationId: String, bookId: String, chapter: Int): List<BibleVerseEntity>

    @Query("SELECT * FROM bible_verses WHERE translationId = :translationId AND text LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY bookId ASC, chapter ASC, verse ASC LIMIT 50")
    suspend fun search(translationId: String, query: String): List<BibleVerseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BibleVerseEntity>)

    @Query("SELECT COUNT(*) FROM bible_verses WHERE translationId = :translationId")
    suspend fun count(translationId: String): Int
}
