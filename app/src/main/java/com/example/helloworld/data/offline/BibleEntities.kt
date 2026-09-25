package com.example.helloworld.data.offline

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "bible_translations")
data class BibleTranslationEntity(
    @androidx.room.PrimaryKey val id: String,
    val abbreviation: String,
    val name: String,
    val language: String,
    val isOffline: Boolean,
    val isEnabled: Boolean,
    val sortOrder: Int
)

@Entity(tableName = "bible_books", indices = [Index(value = ["bookOrder"])])
data class BibleBookEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val abbreviation: String,
    val testament: String,
    val bookOrder: Int,
    val chapterCount: Int
)

@Entity(
    tableName = "bible_verses",
    primaryKeys = ["translationId", "bookId", "chapter", "verse"],
    indices = [Index(value = ["translationId", "bookId", "chapter"])]
)
data class BibleVerseEntity(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)
