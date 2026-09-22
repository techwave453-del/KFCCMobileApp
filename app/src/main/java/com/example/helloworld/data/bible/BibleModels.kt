package com.example.helloworld.data.bible

import kotlinx.serialization.Serializable

@Serializable
data class BibleTranslation(
    val id: String,
    val abbreviation: String,
    val name: String,
    val language: String,
    val isOffline: Boolean = false
)

@Serializable
data class BibleBook(
    val id: String,
    val name: String,
    val abbreviation: String,
    val testament: Testament,
    val chapterCount: Int
)

@Serializable
data class BibleChapter(
    val bookId: String,
    val chapterNumber: Int,
    val verses: List<BibleVerse>
)

@Serializable
data class BibleVerse(
    val number: Int,
    val text: String
)

enum class Testament {
    OLD,
    NEW
}

data class BibleReference(
    val bookName: String,
    val chapter: Int,
    val verse: Int? = null
) {
    override fun toString(): String {
        return if (verse != null) {
            "$bookName $chapter:$verse"
        } else {
            "$bookName $chapter"
        }
    }
}
