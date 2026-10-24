package com.example.helloworld.ui.screens.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.example.helloworld.ui.screens.ScreenBackgroundImage
import android.content.Context
import android.content.Intent
import com.example.helloworld.data.bible.BibleChapter
import com.example.helloworld.data.bible.BibleBook
import com.example.helloworld.data.bible.BibleTranslation
import com.example.helloworld.data.bible.BibleVerse
import com.example.helloworld.data.bible.KfccBibleRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleChapterScreen(
    bookId: String,
    chapterNumber: Int,
    initialVerse: Int? = null,
    initialVerseExplanation: String? = null,
    onBack: () -> Unit
) {
    val repository = remember { KfccBibleRepository() }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kanisa_bible", Context.MODE_PRIVATE) }
    var currentTranslationId by remember { mutableStateOf(prefs.getString("translation_id", "kjv") ?: "kjv") }
    LaunchedEffect(bookId, chapterNumber) {
        prefs.edit().putString("continue_book", bookId).putInt("continue_chapter", chapterNumber).apply()
    }

    val translations by produceState(initialValue = emptyList<BibleTranslation>(), repository) {
        value = runCatching { repository.getTranslations() }.getOrDefault(emptyList())
    }
    var showTranslations by remember { mutableStateOf(false) }
    var showVerseTranslations by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var noteDraft by remember { mutableStateOf("") }
    var verseUiRefresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val books by produceState(initialValue = emptyList<BibleBook>(), repository, currentTranslationId) {
        value = runCatching { repository.getBooks(currentTranslationId) }.getOrDefault(emptyList())
    }

    val book = books.firstOrNull { it.id == bookId }

    val chapter by produceState<BibleChapter?>(initialValue = null, repository, bookId, chapterNumber, currentTranslationId) {
        value = runCatching {
            repository.getChapter(
                translationId = currentTranslationId,
                bookId = bookId,
                chapterNumber = chapterNumber
            )
        }.getOrNull()
    }

    var selectedVerse by remember {
        mutableStateOf<BibleVerse?>(null)
    }
    var comparedVerse by remember { mutableStateOf<Pair<BibleTranslation, BibleVerse>?>(null) }

    // References opened from games, chat, search, and notifications land on the
    // exact verse and automatically show the same centered verse-actions dialog
    // used when a reader taps a verse manually.
    LaunchedEffect(bookId, chapterNumber, initialVerse, chapter) {
        val requestedVerse = initialVerse
        val existingNumber = selectedVerse?.number
        selectedVerse = when {
            existingNumber != null && chapter?.verses?.any { it.number == existingNumber } == true ->
                chapter?.verses?.firstOrNull { it.number == existingNumber }
            requestedVerse != null && requestedVerse > 0 ->
                chapter?.verses?.firstOrNull { it.number == requestedVerse }
            else -> null
        }
    }

    if (showNoteEditor) {
        AlertDialog(
            onDismissRequest = { showNoteEditor = false },
            title = { Text("Verse note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedVerse?.let { verse -> Text("${book?.name ?: bookId} $chapterNumber:${verse.number}", color = MaterialTheme.colorScheme.primary) }
                    OutlinedTextField(value = noteDraft, onValueChange = { noteDraft = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8, label = { Text("Your notes") }, placeholder = { Text("Write your thoughts about this verse…") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedVerse?.let { verse ->
                        prefs.edit().putString(verseNoteKey(currentTranslationId, bookId, chapterNumber, verse.number), noteDraft).apply()
                        verseUiRefresh++
                    }
                    showNoteEditor = false
                }) { Text("Save note") }
            },
            dismissButton = { TextButton(onClick = { showNoteEditor = false }) { Text("Cancel") } }
        )
    }

    if (showVerseTranslations) {
        AlertDialog(
            onDismissRequest = { showVerseTranslations = false },
            title = { Text("Read verse in another version") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(translations.filter { it.id != currentTranslationId }, key = { it.id }) { translation ->
                        TextButton(onClick = {
                            val verseNumber = selectedVerse?.number
                            scope.launch {
                                val targetChapter = runCatching { repository.getChapter(translation.id, bookId, chapterNumber) }.getOrNull()
                                val translatedVerse = targetChapter?.verses?.firstOrNull { it.number == verseNumber }
                                if (translatedVerse != null) comparedVerse = translation to translatedVerse
                                showVerseTranslations = false
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(translation.name, fontWeight = FontWeight.SemiBold)
                                Text(translation.abbreviation, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVerseTranslations = false }) { Text("Close") } }
        )
    }

    if (showTranslations) {
        AlertDialog(
            onDismissRequest = { showTranslations = false },
            title = { Text("Bible translation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    translations.forEach { translation ->
                        TextButton(
                            onClick = {
                                prefs.edit().putString("translation_id", translation.id).apply()
                                currentTranslationId = translation.id
                                showTranslations = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("${translation.name} (${translation.abbreviation})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTranslations = false }) { Text("Close") } }
        )
    }

    Box(Modifier.fillMaxSize()) {
        ScreenBackgroundImage(
            screenKey = "bible",
            modifier = Modifier.fillMaxSize(),
            overlayAlpha = 0.24f
        )
        Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.42f)
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(
                                text = book?.name ?: bookId,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Chapter $chapterNumber • ${currentTranslationId.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.86f)
                            )
                        }
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
                    FilledTonalButton(
                        onClick = { showTranslations = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = currentTranslationId.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { shareText(context, "${book?.name ?: bookId} $chapterNumber — ${currentTranslationId.uppercase()}") }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share chapter"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            chapter?.let { loadedChapter ->
                BibleReader(
                    chapter = loadedChapter,
                    translationId = currentTranslationId,
                    initialVerse = initialVerse,
                    prefs = prefs,
                    uiRefresh = verseUiRefresh,
                    onVerseClick = { verse ->
                        selectedVerse = verse
                        comparedVerse = null
                    }
                )
            } ?: ChapterNotLoaded(
                bookName = book?.name ?: bookId,
                chapterNumber = chapterNumber,
                onBack = onBack
            )

            selectedVerse?.let { verse ->
                VerseActionsPanel(
                    verse = verse,
                    explanation = initialVerseExplanation,
                    bookName = book?.name ?: bookId,
                    chapterNumber = chapterNumber,
                    translationId = currentTranslationId,
                    onDismiss = { selectedVerse = null },
                    onBookmark = { toggleBookmark(prefs, bookId, chapterNumber, it.number) },
                    isBookmarked = { isBookmarked(prefs, bookId, chapterNumber, it.number) },
                    onShare = { shareText(context, (book?.name ?: bookId) + " " + chapterNumber + ":" + it.number + " — " + currentTranslationId.uppercase() + " — " + it.text) },
                    note = prefs.getString(verseNoteKey(currentTranslationId, bookId, chapterNumber, verse.number), "").orEmpty(),
                    highlightColor = prefs.getInt(verseHighlightKey(currentTranslationId, bookId, chapterNumber, verse.number), 0),
                    onEditNote = {
                        noteDraft = prefs.getString(verseNoteKey(currentTranslationId, bookId, chapterNumber, verse.number), "").orEmpty()
                        showNoteEditor = true
                    },
                    onTranslate = { showVerseTranslations = true },
                    comparison = comparedVerse,
                    onSwipeTranslation = { direction ->
                        if (translations.isNotEmpty()) {
                            val activeTranslationId = comparedVerse?.first?.id ?: currentTranslationId
                            val currentIndex = translations.indexOfFirst { it.id == activeTranslationId }
                            val safeIndex = if (currentIndex >= 0) currentIndex else 0
                            val nextIndex = (safeIndex + direction + translations.size) % translations.size
                            val targetTranslation = translations[nextIndex]
                            if (targetTranslation.id == currentTranslationId) {
                                comparedVerse = null
                            } else {
                                scope.launch {
                                    val targetChapter = runCatching {
                                        repository.getChapter(targetTranslation.id, bookId, chapterNumber)
                                    }.getOrNull()
                                    val translatedVerse = targetChapter?.verses?.firstOrNull { it.number == verse.number }
                                    if (translatedVerse != null) comparedVerse = targetTranslation to translatedVerse
                                }
                            }
                        }
                    },
                    onHighlight = { color ->
                        prefs.edit().putInt(verseHighlightKey(currentTranslationId, bookId, chapterNumber, verse.number), color).apply()
                        verseUiRefresh++
                    }
                )
            }
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
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
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
    translationId: String,
    initialVerse: Int? = null,
    prefs: android.content.SharedPreferences,
    uiRefresh: Int,
    onVerseClick: (BibleVerse) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(chapter.chapterNumber, initialVerse) {
        val verse = initialVerse
        if (verse != null && verse > 0) {
            listState.scrollToItem(verse.coerceAtMost(chapter.verses.size))
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),

        contentPadding = PaddingValues(

            start = 20.dp,

            top = 18.dp,

            end = 20.dp,

            bottom = 120.dp

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

                            imageVector = Icons.AutoMirrored.Filled.MenuBook,

                            contentDescription = null,

                            tint = MaterialTheme.colorScheme.primary,

                            modifier = Modifier.size(23.dp)

                        )

                    }

                }



                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
            }

            ChapterHeader(chapter, translationId)



            Spacer(modifier = Modifier.height(22.dp))

        }



        items(

            items = chapter.verses,

            key = { it.number }

        ) { verse ->

            BibleVerseRow(
                verse = verse,
                highlightColor = run {
                    uiRefresh
                    prefs.getInt(verseHighlightKey(translationId, chapter.bookId, chapter.chapterNumber, verse.number), 0)
                },
                hasNote = prefs.getString(verseNoteKey(translationId, chapter.bookId, chapter.chapterNumber, verse.number), "").orEmpty().isNotBlank(),
                onClick = { onVerseClick(verse) }
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
        chapter: BibleChapter,
        translationId: String
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.86f))
                .padding(22.dp)
        ) {
            Text(
                text = "HOLY SCRIPTURE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Chapter ${chapter.chapterNumber}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "${translationId.uppercase()} • ${chapter.verses.size} verses",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    @Composable
    private fun BibleVerseRow(
        verse: BibleVerse,
        highlightColor: Int,
        hasNote: Boolean,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
            color = if (highlightColor != 0) Color(highlightColor) else MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = verse.number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(30.dp).padding(top = 3.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = verse.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.08f,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.55f
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasNote) Text("Note saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }

    @Composable
    private fun VerseActionsPanel(
        verse: BibleVerse,
        explanation: String? = null,
        bookName: String,
        chapterNumber: Int,
        translationId: String,
        onDismiss: () -> Unit,
        onBookmark: (BibleVerse) -> Unit,
        isBookmarked: (BibleVerse) -> Boolean,
        onShare: (BibleVerse) -> Unit,
        note: String,
        highlightColor: Int,
        onEditNote: () -> Unit,
        onTranslate: () -> Unit,
        comparison: Pair<BibleTranslation, BibleVerse>?,
        onSwipeTranslation: (Int) -> Unit,
        onHighlight: (Int) -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.38f)
                )
                .clickable(onClick = onDismiss)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
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
                    modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())
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
                                text = translationId.uppercase(),
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
                        text = comparison?.second?.text ?: verse.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35
                        ),
                        modifier = Modifier
                            .padding(
                                top = 16.dp,
                                bottom = if (explanation.isNullOrBlank()) 8.dp else 12.dp
                            )
                            .pointerInput(translationId, comparison?.first?.id) {
                                var horizontalDrag = 0f
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        horizontalDrag += dragAmount
                                        change.consume()
                                    },
                                    onDragEnd = {
                                        if (horizontalDrag < -60f) onSwipeTranslation(1)
                                        else if (horizontalDrag > 60f) onSwipeTranslation(-1)
                                    }
                                )
                            }
                    )
                    Text(
                        text = "Viewing " + (comparison?.first?.abbreviation ?: translationId.uppercase()) + " • Swipe left/right to switch versions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (!explanation.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "Today's Reflection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                        )
                    }

                    comparison?.let { (translation, _) ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "Original in " + translationId.uppercase() + ":",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(
                            text = verse.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }

                    Text("Highlight verse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0xFFFFF176.toInt(), 0xFFA5D6A7.toInt(), 0xFF90CAF9.toInt(), 0xFFF48FB1.toInt(), 0xFFFFCC80.toInt(), 0xFFCE93D8.toInt()).forEach { colorValue ->
                            Surface(
                                modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onHighlight(colorValue) },
                                shape = CircleShape,
                                color = Color(colorValue),
                                border = BorderStroke(if (highlightColor == colorValue) 3.dp else 1.dp, if (highlightColor == colorValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {}
                        }
                        TextButton(onClick = { onHighlight(0) }) { Text("Clear") }
                    }
                    if (note.isNotBlank()) {
                        Text("Your note", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VerseAction(icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) }, label = "Note", modifier = Modifier.weight(1f), onClick = onEditNote)
                        VerseAction(icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) }, label = "Translate", modifier = Modifier.weight(1f), onClick = onTranslate)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VerseAction(
                            icon = {
                                Icon(
                                    imageVector = if (isBookmarked(verse)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null
                                )
                            },
                            label = if (isBookmarked(verse)) "Bookmarked" else "Bookmark",
                            modifier = Modifier.weight(1f),
                            onClick = { onBookmark(verse) }
                        )

                        VerseAction(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            },
                            label = "Share",
                            modifier = Modifier.weight(1f),
                            onClick = { onShare(verse) }
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
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = modifier.clickable(onClick = onClick),
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
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
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


private fun verseNoteKey(translationId: String, bookId: String, chapter: Int, verse: Int) = "verse_note:$translationId:$bookId:$chapter:$verse"
private fun verseHighlightKey(translationId: String, bookId: String, chapter: Int, verse: Int) = "verse_highlight:$translationId:$bookId:$chapter:$verse"

private fun bookmarkKey(bookId: String, chapter: Int, verse: Int) = "bookmark:$bookId:$chapter:$verse"

private fun isBookmarked(prefs: android.content.SharedPreferences, bookId: String, chapter: Int, verse: Int): Boolean =
    prefs.getBoolean(bookmarkKey(bookId, chapter, verse), false)

private fun toggleBookmark(prefs: android.content.SharedPreferences, bookId: String, chapter: Int, verse: Int) {
    val key = bookmarkKey(bookId, chapter, verse)
    prefs.edit().putBoolean(key, !prefs.getBoolean(key, false)).apply()
}

private fun shareText(context: android.content.Context, text: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, "Share Scripture"))
}
