package com.example.helloworld.ui.screens.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.bible.BibleChapter
import com.example.helloworld.data.bible.BibleBook
import com.example.helloworld.data.bible.BibleVerse
import com.example.helloworld.data.bible.KfccBibleRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleChapterScreen(
    bookId: String,
    chapterNumber: Int,
    onBack: () -> Unit
) {
    val repository = remember { KfccBibleRepository() }

    val books by produceState(initialValue = emptyList<BibleBook>(), repository) {
        value = runCatching { repository.getBooks("kjv") }.getOrDefault(emptyList())
    }

    val book = books.firstOrNull { it.id == bookId }

    val chapter by produceState<BibleChapter?>(initialValue = null, repository, bookId, chapterNumber) {
        value = runCatching {
            repository.getChapter(
                translationId = "kjv",
                bookId = bookId,
                chapterNumber = chapterNumber
            )
        }.getOrNull()
    }

    var selectedVerse by remember {
        mutableStateOf<BibleVerse?>(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book?.name ?: bookId,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Chapter $chapterNumber • KJV",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Bible"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share chapter"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (chapter == null) {
                ChapterNotLoaded(
                    bookName = book?.name ?: bookId,
                    chapterNumber = chapterNumber,
                    onBack = onBack
                )
            } else {
                BibleReader(
                    chapter = chapter,
                    onVerseClick = { verse ->
                        selectedVerse = verse
                    }
                )
            }

            selectedVerse?.let { verse ->
                VerseActionsPanel(
                    verse = verse,
                    bookName = book?.name ?: bookId,
                    chapterNumber = chapterNumber,
                    onDismiss = {
                        selectedVerse = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ChapterNotLoaded(
    bookName: String,
    chapterNumber: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Text(
            text = "$bookName $chapterNumber",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )

        Text(
            text = "This chapter could not be loaded.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Back to Bible")
        }
    }
}


@Composable

private fun BibleReader(

    chapter: BibleChapter,

    onVerseClick: (BibleVerse) -> Unit

) {

    LazyColumn(

        modifier = Modifier.fillMaxSize(),

        contentPadding = PaddingValues(

            start = 20.dp,

            top = 20.dp,

            end = 20.dp,

            bottom = 110.dp

        )

    ) {

        item {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(bottom = 18.dp),

                verticalAlignment = Alignment.CenterVertically

            ) {

                Surface(

                    modifier = Modifier.size(44.dp),

                    shape = CircleShape,

                    color = MaterialTheme.colorScheme.primaryContainer

                ) {

                    Box(

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            imageVector = Icons.Default.MenuBook,

                            contentDescription = null,

                            tint = MaterialTheme.colorScheme.primary,

                            modifier = Modifier.size(23.dp)

                        )

                    }

                }



                Column(

                    modifier = Modifier.padding(start = 12.dp)

                ) {

                    Text(

                        text = "Reading Scripture",

                        style = MaterialTheme.typography.labelMedium,

                        color = MaterialTheme.colorScheme.primary,

                        fontWeight = FontWeight.Bold

                    )



                    Text(

                        text = "${chapter.chapterNumber} • ${chapter.verses.size} verses",

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

            }



            ChapterHeader(chapter)



            Spacer(modifier = Modifier.height(22.dp))

        }



        items(

            items = chapter.verses,

            key = { it.number }

        ) { verse ->

            BibleVerseRow(

                verse = verse,

                onClick = {

                    onVerseClick(verse)

                }

            )

        }



        item {

            Spacer(modifier = Modifier.height(28.dp))

            ChapterEnd()

        }

    }

}

    @Composable
    private fun ChapterHeader(
        chapter: BibleChapter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "KING JAMES VERSION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Chapter ${chapter.chapterNumber}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp)
            )

            Text(
                text = "Holy Scripture",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 18.dp)
            )
        }
    }

    @Composable
    private fun BibleVerseRow(
        verse: BibleVerse,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 6.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = verse.number.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = verse.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.45
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
        }
    }

    @Composable
    private fun VerseActionsPanel(
        verse: BibleVerse,
        bookName: String,
        chapterNumber: Int,
        onDismiss: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f)
                )
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    )
                    .clickable(onClick = {}),
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = verse.number.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = "$bookName $chapterNumber:${verse.number}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "King James Version",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Text(
                                text = "×",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    Text(
                        text = verse.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35
                        ),
                        modifier = Modifier.padding(
                            top = 16.dp,
                            bottom = 18.dp
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VerseAction(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null
                                )
                            },
                            label = "Bookmark",
                            modifier = Modifier.weight(1f)
                        )

                        VerseAction(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            },
                            label = "Share",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun VerseAction(
        icon: @Composable () -> Unit,
        label: String,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 14.dp
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun ChapterEnd() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Text(
                text = "End of chapter",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
