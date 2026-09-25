package com.example.helloworld.data.offline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BibleTranslationEntity::class, BibleBookEntity::class, BibleVerseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KfccDatabase : RoomDatabase() {
    abstract fun bibleTranslationDao(): BibleTranslationDao
    abstract fun bibleBookDao(): BibleBookDao
    abstract fun bibleVerseDao(): BibleVerseDao

    companion object {
        @Volatile private var INSTANCE: KfccDatabase? = null
        fun getInstance(context: Context): KfccDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KfccDatabase::class.java,
                    "kfcc_offline.db"
                ).build().also { INSTANCE = it }
            }
    }
}
