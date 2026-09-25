package com.example.helloworld.data.offline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteContentDao {
    @Query("SELECT * FROM site_content_cache ORDER BY key ASC")
    suspend fun getAll(): List<SiteContentEntity>

    @Query("SELECT * FROM site_content_cache ORDER BY key ASC")
    fun observeAll(): Flow<List<SiteContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SiteContentEntity>)
}

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items_cache WHERE published = 1 ORDER BY createdAt DESC")
    suspend fun getPublished(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items_cache WHERE published = 1 ORDER BY createdAt DESC")
    fun observePublished(): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items_cache")
    suspend fun clear()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events_cache WHERE status = 'published' ORDER BY displayOrder ASC, startAt ASC")
    suspend fun getPublished(): List<EventEntity>

    @Query("SELECT * FROM events_cache WHERE status = 'published' ORDER BY displayOrder ASC, startAt ASC")
    fun observePublished(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EventEntity>)

    @Query("DELETE FROM events_cache")
    suspend fun clear()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications_cache WHERE userId IS NULL OR userId = :userId ORDER BY createdAt DESC")
    suspend fun getForUser(userId: String?): List<NotificationEntity>

    @Query("SELECT * FROM notifications_cache WHERE userId IS NULL OR userId = :userId ORDER BY createdAt DESC")
    fun observeForUser(userId: String?): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRead(item: NotificationReadEntity)

    @Query("SELECT * FROM notification_reads_cache WHERE userId = :userId")
    suspend fun getReads(userId: String): List<NotificationReadEntity>
}
