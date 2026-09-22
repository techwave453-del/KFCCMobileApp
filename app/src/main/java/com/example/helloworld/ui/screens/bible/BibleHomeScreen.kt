package com.example.helloworld.ui.screens.bible

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.helloworld.data.bible.BibleBook
import com.example.helloworld.data.bible.KfccBibleRepository
import com.example.helloworld.data.bible.Testament

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleHomeScreen(
    onBack: () -> Unit,
    onOpenChapter: (String, Int) -> Unit
) {
    val repository = remember { KfccBibleRepository() }
    val books = remember { repository.getBooks("kjv") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bible") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Read, reflect and discover Scripture.",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "King James Version",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenChapter("psalms", 23)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Today's Scripture",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Text(
                            text = "Psalm 23",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Text(
                            text = "The LORD is my shepherd; I shall not want.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Browse Scripture",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Text(
                    text = "Old Testament",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(
                books.filter { it.testament == Testament.OLD }
            ) { book ->
                BibleBookRow(
                    book = book,
                    onClick = {
                        onOpenChapter(book.id, 1)
                    }
                )
            }

            item {
                Text(
                    text = "New Testament",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            items(
                books.filter { it.testament == Testament.NEW }
            ) { book ->
                BibleBookRow(
                    book = book,
                    onClick = {
                        onOpenChapter(book.id, 1)
                    }
                )
            }
        }
    }
}

@Composable
private fun BibleBookRow(
    book: BibleBook,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.padding(end = 14.dp)
            )

            Column {
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "${book.chapterCount} chapters",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}