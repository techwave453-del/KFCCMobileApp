package com.example.helloworld.data.offline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BibleTranslationEntity::class,
        BibleBookEntity::class,
        BibleVerseEntity::class,
        SiteContentEntity::class,
        MediaItemEntity::class,
        EventEntity::class,
        NotificationEntity::class,
        NotificationReadEntity::class,
        SyncOperationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class KfccDatabase : RoomDatabase() {
    abstract fun bibleTranslationDao(): BibleTranslationDao
    abstract fun bibleBookDao(): BibleBookDao
    abstract fun bibleVerseDao(): BibleVerseDao
    abstract fun siteContentDao(): SiteContentDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun eventDao(): EventDao
    abstract fun notificationDao(): NotificationDao
    abstract fun syncOperationDao(): SyncOperationDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS site_content_cache (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS media_items_cache (id INTEGER NOT NULL PRIMARY KEY, legacyId INTEGER, title TEXT NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, storagePath TEXT, createdAt TEXT NOT NULL, published INTEGER NOT NULL, thumbnailUrl TEXT, featured INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_items_cache_published ON media_items_cache(published)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_items_cache_createdAt ON media_items_cache(createdAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS events_cache (id INTEGER NOT NULL PRIMARY KEY, slug TEXT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, shortDescription TEXT NOT NULL, description TEXT NOT NULL, image TEXT NOT NULL, flyerUrl TEXT NOT NULL, startAt TEXT NOT NULL, endAt TEXT, allDay INTEGER NOT NULL, location TEXT NOT NULL, address TEXT NOT NULL, attendanceType TEXT NOT NULL, registrationUrl TEXT NOT NULL, contact TEXT NOT NULL, livestreamUrl TEXT NOT NULL, featured INTEGER NOT NULL, status TEXT NOT NULL, displayOrder INTEGER NOT NULL, createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_cache_status ON events_cache(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_cache_startAt ON events_cache(startAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_cache_displayOrder ON events_cache(displayOrder)")
                db.execSQL("CREATE TABLE IF NOT EXISTS notifications_cache (id TEXT NOT NULL PRIMARY KEY, userId TEXT, title TEXT NOT NULL, message TEXT NOT NULL, type TEXT NOT NULL, createdAt TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_cache_userId ON notifications_cache(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_cache_createdAt ON notifications_cache(createdAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS notification_reads_cache (notificationId TEXT NOT NULL, userId TEXT NOT NULL, readAt TEXT NOT NULL, PRIMARY KEY(notificationId, userId))")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_operations (operationId TEXT NOT NULL PRIMARY KEY, entityType TEXT NOT NULL, operationType TEXT NOT NULL, entityId TEXT, payload TEXT NOT NULL, createdAt INTEGER NOT NULL, attempts INTEGER NOT NULL, status TEXT NOT NULL, lastError TEXT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_operations_status ON sync_operations(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_operations_createdAt ON sync_operations(createdAt)")
            }
        }

        @Volatile private var INSTANCE: KfccDatabase? = null

        fun getInstance(context: Context): KfccDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KfccDatabase::class.java,
                    "kfcc_offline.db"
                )
                                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
