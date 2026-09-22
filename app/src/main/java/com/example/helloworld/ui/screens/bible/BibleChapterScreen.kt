package com.example.helloworld.ui.screens.bible

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.bible.BibleChapter
import com.example.helloworld.data.bible.KfccBibleRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleChapterScreen(
    bookId: String,
    chapterNumber: Int,
    onBack: () -> Unit
) {
    val repository = remember { KfccBibleRepository() }

    val books = remember {
        repository.getBooks("kjv")
    }

    val book = books.firstOrNull {
        it.id == bookId
    }

    val chapter = remember(bookId, chapterNumber) {
        repository.getChapter(
            translationId = "kjv",
            bookId = bookId,
            chapterNumber = chapterNumber
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${book?.name ?: bookId} $chapterNumber"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        if (chapter == null) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "This chapter is not loaded yet.",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "The Bible foundation is ready for the full Scripture dataset.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

        } else {

            BibleVerseList(
                chapter = chapter,
                innerPadding = innerPadding
            )
        }
    }
}

@Composable
private fun BibleVerseList(
    chapter: BibleChapter,
    innerPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        items(chapter.verses) { verse ->

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = verse.number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = verse.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark verse"
                    )
                }
            }
        }
    }
}