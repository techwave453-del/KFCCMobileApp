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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.bible.BibleBook
import com.example.helloworld.data.bible.KfccBibleRepository
import com.example.helloworld.data.bible.Testament
import android.content.Context
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleHomeScreen(
    onBack: () -> Unit,
    onOpenChapter: (String, Int) -> Unit,
    onOpenSearch: () -> Unit
) {
    val repository = remember { KfccBibleRepository() }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kanisa_bible", Context.MODE_PRIVATE) }
    val savedBook = prefs.getString("continue_book", "GEN") ?: "GEN"
    val savedChapter = prefs.getInt("continue_chapter", 1)
    var selectedTranslationId by remember { mutableStateOf(prefs.getString("translation_id", "kjv") ?: "kjv") }
    val books by produceState(initialValue = emptyList<BibleBook>(), repository) {
        value = runCatching { repository.getBooks(selectedTranslationId) }.getOrDefault(emptyList())
    }
    val translations by produceState(initialValue = emptyList<com.example.helloworld.data.bible.BibleTranslation>(), repository) {
        value = runCatching { repository.getTranslations() }.getOrDefault(emptyList())
    }
    val translationAvailability by produceState(initialValue = emptyMap<String, Int>(), repository, translations) {
        value = translations.associate { translation ->
            translation.id to runCatching { repository.getVerseCount(translation.id) }.getOrDefault(0)
        }
    }
    var selectedBook by remember { mutableStateOf<BibleBook?>(null) }
    var showTranslations by remember { mutableStateOf(false) }
    val selectedTranslation = translations.firstOrNull { it.id == selectedTranslationId }
    val dailyChapter by produceState<com.example.helloworld.data.bible.BibleChapter?>(initialValue = null, repository, selectedTranslationId) {
        val day = LocalDate.now().dayOfYear
        val chapterNumber = ((day - 1) % 150) + 1
        value = runCatching { repository.getChapter(selectedTranslationId, "psalms", chapterNumber) }.getOrNull()
    }

    if (showTranslations) {
        AlertDialog(
            onDismissRequest = { showTranslations = false },
            title = { Text("Bible translation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    translations.forEach { translation ->
                        val count = translationAvailability[translation.id] ?: 0
                        TextButton(
                            onClick = {
                                if (count > 0) {
                                    prefs.edit().putString("translation_id", translation.id).apply()\n                                    selectedTranslationId = translation.id
                                    showTranslations = false
                                }
                            },
                            enabled = count > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("${translation.name} (${translation.abbreviation})", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (count > 0) "${count} verses available" else "Verse data not available yet",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTranslations = false }) { Text("Close") } }
        )
    }

    selectedBook?.let { book ->
        BibleChapterPickerDialog(
            book = book,
            onDismiss = { selectedBook = null },
            onSelectChapter = { chapter ->
                selectedBook = null
                onOpenChapter(book.id, chapter)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bible",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = selectedTranslation?.name ?: selectedTranslationId.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTranslations = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Choose Bible translation"
                        )
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Scripture"
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
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item {
                BibleHero(translationAbbreviation = selectedTranslation?.abbreviation ?: selectedTranslationId.uppercase())
            }

            item {
                TodaysScriptureCard(
                    chapter = dailyChapter,
                    translationName = selectedTranslation?.name ?: selectedTranslationId.uppercase(),
                    onClick = {
                        dailyChapter?.let { onOpenChapter(it.bookId, it.chapterNumber) }
                    }
                )
            }

            item {
                ContinueReadingCard(
                    bookName = books.firstOrNull { it.id == savedBook }?.name ?: savedBook,
                    chapter = savedChapter,
                    onClick = {
                        onOpenChapter(savedBook, savedChapter)
                    }
                )
            }

            item {
                BibleSectionHeader(
                    title = "Browse Scripture",
                    subtitle = "Explore all 66 books of the Bible."
                )
            }

            item {
                TestamentHeader(
                    title = "Old Testament",
                    subtitle = "39 books"
                )
            }

            items(
                books.filter { it.testament == Testament.OLD },
                key = { it.id }
            ) { book ->
                BibleBookRow(
                    book = book,
                    onClick = { selectedBook = book }
                )
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))

                TestamentHeader(
                    title = "New Testament",
                    subtitle = "27 books"
                )
            }

            items(
                books.filter { it.testament == Testament.NEW },
                key = { it.id }
            ) { book ->
                BibleBookRow(
                    book = book,
                    onClick = { selectedBook = book }
                )
            }
        }
    }
}

@Composable
private fun BibleHero(translationAbbreviation: String) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(colors.primaryContainer, colors.secondaryContainer, colors.tertiaryContainer)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = colors.surface.copy(alpha = 0.72f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = colors.primary, modifier = Modifier.size(29.dp))
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "Holy Bible",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Read the Word. Grow in faith.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Discover Scripture, reflect on God’s promises, and continue your journey wherever you are.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onPrimaryContainer.copy(alpha = 0.82f),
                    modifier = Modifier.padding(top = 10.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BiblePill(translationAbbreviation)
                    BiblePill("66 Books")
                    BiblePill("Offline")
                }
            }
        }
    }
}

@Composable
private fun BiblePill(text: String) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun TodaysScriptureCard(
    chapter: com.example.helloworld.data.bible.BibleChapter?,
    translationName: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = colors.primary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("TODAY’S SCRIPTURE", style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                    Text(
                        if (chapter != null) "Psalm ${chapter.chapterNumber}" else "Daily reading",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                chapter?.verses?.firstOrNull()?.text?.let { "“$it”" } ?: "Scripture for today is not available in this translation.",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.3f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 18.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    chapter?.verses?.firstOrNull()?.let { "Psalm ${chapter.chapterNumber}:${it.number}" } ?: translationName,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text("Read chapter  ›", style = MaterialTheme.typography.labelLarge, color = colors.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    bookName: String,
    chapter: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("CONTINUE READING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("$bookName • Chapter $chapter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            }
            Text("Continue  ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BibleSectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun TestamentHeader(
    title: String,
    subtitle: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun BibleBookRow(
    book: BibleBook,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${book.chapterCount} chapters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BibleChapterPickerDialog(
    book: BibleBook,
    onDismiss: () -> Unit,
    onSelectChapter: (Int) -> Unit
) {
    val chapters = (1..book.chapterCount).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = book.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${book.chapterCount} chapters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters.chunked(5)) { rowChapters ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowChapters.forEach { chapter ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onSelectChapter(chapter)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chapter.toString(),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        repeat(5 - rowChapters.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}